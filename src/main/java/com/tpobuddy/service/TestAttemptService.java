package com.tpobuddy.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tpobuddy.controller.TestController;
import com.tpobuddy.model.*;
import com.tpobuddy.util.Config;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Service orchestrating the complete lifecycle of AI-generated Test Attempts.
 * Enforces:
 * - Independent student attempts with unique randomized question sets
 * - Double-click and duplicate attempt prevention (Idempotency)
 * - 50 candidate generation -> 30 question selection -> discarding unused 20
 * - Absolute protection of answer keys (server-side only)
 * - Authoritative evaluation and Firebase persistence
 * - Periodic cleanup of expired/temporary attempts
 */
@Service
public class TestAttemptService {

    private final AIGenerationService aiGenerationService;
    private final QuestionSelectionService questionSelectionService;
    private final TestEvaluationService evaluationService;
    private final TestSeriesService testSeriesService;
    private final FirebaseService firebaseService;
    private final Gson gson = new Gson();

    // In-memory active attempts map: attemptId -> TestAttempt
    private final Map<String, TestAttempt> attemptRegistry = new ConcurrentHashMap<>();

    // Student active attempt pointer: studentUid -> attemptId
    private final Map<String, String> studentActiveAttempt = new ConcurrentHashMap<>();

    // Protected server-only questions: attemptId -> List<Question> (contains answers & explanations)
    private final Map<String, List<Question>> protectedQuestions = new ConcurrentHashMap<>();

    // Protected server-only submitted results: attemptId -> EvaluationPayload
    private final Map<String, TestEvaluationService.EvaluationPayload> cachedResults = new ConcurrentHashMap<>();

    public TestAttemptService(AIGenerationService aiGenerationService,
                              QuestionSelectionService questionSelectionService,
                              TestEvaluationService evaluationService,
                              TestSeriesService testSeriesService,
                              FirebaseService firebaseService) {
        this.aiGenerationService = aiGenerationService;
        this.questionSelectionService = questionSelectionService;
        this.evaluationService = evaluationService;
        this.testSeriesService = testSeriesService;
        this.firebaseService = firebaseService;
    }

    /**
     * Initiates generation and setup of an independent test attempt.
     */
    public synchronized TestAttempt createOrGetAttempt(String studentUid, String studentName,
                                                       String seriesId, String difficulty,
                                                       List<String> customTopics) {
        if (studentUid == null || studentUid.trim().isEmpty()) {
            throw new IllegalArgumentException("Student authentication is required.");
        }

        String effDifficulty = (difficulty != null && !difficulty.trim().isEmpty()) ? difficulty : "Medium";
        TestSeries series = testSeriesService.getSeriesById(seriesId);
        if (series == null) {
            series = testSeriesService.getSeriesById("college_snjb_full_mock");
        }

        // Idempotency: Check if student already has an active, unsubmitted attempt
        String existingAttemptId = studentActiveAttempt.get(studentUid);
        if (existingAttemptId != null) {
            TestAttempt existing = attemptRegistry.get(existingAttemptId);
            if (existing != null && !isSubmitted(existing)) {
                long now = System.currentTimeMillis();
                if (existing.getDeadline() > 0 && now < existing.getDeadline()) {
                    // Active valid attempt exists; return idempotently only if series and difficulty match
                    if (series != null && series.getSeriesId().equalsIgnoreCase(existing.getSeriesId())
                            && effDifficulty.equalsIgnoreCase(existing.getDifficulty())) {
                        return existing;
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        String attemptId = "att_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        int durationSecs = (series != null ? series.getDurationMinutes() : 30) * 60;
        long deadline = now + (durationSecs * 1000L);
        long expiresAt = deadline + (15 * 60 * 1000L); // 15 mins grace period before cleanup

        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId);
        attempt.setUserId(studentUid);
        attempt.setStudentName(studentName != null ? studentName : "Student");
        attempt.setSeriesId(series != null ? series.getSeriesId() : seriesId);
        attempt.setCategory(series != null ? series.getCategory() : TestSeries.CATEGORY_COLLEGE);
        attempt.setTopic(series != null ? series.getSubject() : "Placement Practice");
        attempt.setTestTitle(series != null ? series.getName() : "Assessment");
        attempt.setDifficulty(effDifficulty);
        attempt.setStatus("GENERATING");
        attempt.setGenerationStartedAt(now);
        attempt.setStartTime(now);
        attempt.setDurationSeconds(durationSecs);
        attempt.setDeadline(deadline);
        attempt.setExpiresAt(expiresAt);
        attempt.setSelectedQuestionCount(QuestionSelectionService.TARGET_QUESTION_COUNT);

        attemptRegistry.put(attemptId, attempt);
        studentActiveAttempt.put(studentUid, attemptId);

        String effTopic = (customTopics != null && !customTopics.isEmpty()) ? String.join(", ", customTopics) : (series != null ? series.getSubject() : "Placement Practice");
        String qSetId = "qs_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        List<Question> candidatePool = new ArrayList<>();

        // 1. Check Firebase for an existing valid question set matching exact series, topic and difficulty
        JsonObject existingSet = firebaseService.fetchReusableSnjbQuestionSet(seriesId, effTopic, effDifficulty);
        if (existingSet != null && existingSet.has("questions") && existingSet.get("questions").isJsonArray()) {
            JsonArray qArr = existingSet.getAsJsonArray("questions");
            if (qArr.size() >= 30) {
                for (JsonElement qEl : qArr) {
                    if (qEl.isJsonObject()) {
                        candidatePool.add(gson.fromJson(qEl, Question.class));
                    }
                }
            }
        }

        // 2. If no suitable question set exists, dynamically generate with Gemini AI
        if (candidatePool.size() < 30) {
            candidatePool = aiGenerationService.generateCandidatePool(series, effDifficulty, customTopics, 50);

            // Persist the new validated question set into Firebase snjbQuestionSets for subsequent reuse
            if (!candidatePool.isEmpty()) {
                JsonObject setJson = new JsonObject();
                setJson.addProperty("setId", qSetId);
                setJson.addProperty("seriesId", series != null ? series.getSeriesId() : seriesId);
                setJson.addProperty("topic", effTopic);
                setJson.addProperty("difficulty", effDifficulty);
                setJson.addProperty("questionCount", candidatePool.size());
                setJson.addProperty("createdAt", now);
                setJson.addProperty("expiresAt", now + (24 * 3600 * 1000L)); // 24h validity
                setJson.addProperty("status", "ACTIVE");
                setJson.addProperty("model", "gemini-2.5-flash");
                setJson.add("questions", gson.toJsonTree(candidatePool));
                try {
                    firebaseService.saveSnjbQuestionSet(qSetId, setJson);
                } catch (Exception e) {
                    System.err.println("Notice: could not persist question set to Firebase: " + e.getMessage());
                }
            }
        }

        // Step 5: Select exactly 30 questions
        List<Question> selected30 = questionSelectionService.selectQuestionsForAttempt(candidatePool, series, effDifficulty);

        // Discard remaining 20 candidates (candidatePool is local and goes out of scope here)
        protectedQuestions.put(attemptId, selected30);

        attempt.setGeneratedAt(System.currentTimeMillis());
        attempt.setStatus("READY");

        // Sync initial attempt record to Firebase
        try {
            firebaseService.saveTestAttempt(studentUid, attempt);
        } catch (Exception e) {
            System.err.println("Notice: Could not sync initial attempt metadata to Firebase: " + e.getMessage());
        }

        return attempt;
    }

    /**
     * Retrieves sanitized questions for an active attempt (STRICT: NO ANSWERS EXPOSED).
     */
    public JsonArray getSanitizedQuestions(String attemptId, String studentUid) {
        TestAttempt attempt = attemptRegistry.get(attemptId);
        if (attempt == null || !attempt.getUserId().equals(studentUid)) {
            return null;
        }

        List<Question> questions = protectedQuestions.get(attemptId);
        if (questions == null || questions.isEmpty()) {
            return new JsonArray();
        }

        // Transition from READY to IN_PROGRESS upon question retrieval
        if ("READY".equalsIgnoreCase(attempt.getStatus())) {
            attempt.setStatus("in_progress");
        }

        JsonArray arr = new JsonArray();
        for (Question q : questions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", q.getId());
            obj.addProperty("displayOrder", q.getDisplayOrder());
            obj.addProperty("question", q.getQuestion());
            obj.addProperty("section", q.getSection());
            obj.addProperty("topic", q.getTopic());
            obj.addProperty("difficulty", q.getDifficulty());
            obj.addProperty("questionType", q.getQuestionType());
            obj.addProperty("passageOrCode", q.getPassageOrCode());
            obj.addProperty("optionA", q.getOptionA());
            obj.addProperty("optionB", q.getOptionB());
            obj.addProperty("optionC", q.getOptionC());
            obj.addProperty("optionD", q.getOptionD());
            obj.addProperty("marks", q.getMarks() > 0 ? q.getMarks() : 2);
            // CRITICAL: DO NOT ADD correctOption or explanation!
            arr.add(obj);
        }
        return arr;
    }

    public List<Question> getAttemptQuestions(String attemptId) {
        return protectedQuestions.get(attemptId);
    }

    public TestAttempt getAttempt(String attemptId, String studentUid) {
        if (attemptId == null) return null;
        TestAttempt attempt = attemptRegistry.get(attemptId);
        if (attempt != null && (attempt.getUserId() == null || studentUid == null || attempt.getUserId().equals(studentUid))) {
            return attempt;
        }
        if (cachedResults.containsKey(attemptId)) {
            TestEvaluationService.EvaluationPayload payload = cachedResults.get(attemptId);
            if (payload != null && payload.attempt != null) {
                TestAttempt att = payload.attempt;
                if (att.getUserId() == null || studentUid == null || att.getUserId().equals(studentUid)) {
                    return att;
                }
            }
        }
        return null;
    }

    public synchronized TestAttempt getActiveAttemptForStudent(String studentUid) {
        if (studentUid == null || studentUid.trim().isEmpty()) return null;
        String attemptId = studentActiveAttempt.get(studentUid);
        if (attemptId == null) return null;

        TestAttempt attempt = attemptRegistry.get(attemptId);
        if (attempt == null) {
            studentActiveAttempt.remove(studentUid);
            return null;
        }

        if (isSubmitted(attempt)) {
            studentActiveAttempt.remove(studentUid);
            return null;
        }

        long now = System.currentTimeMillis();
        if (attempt.getDeadline() > 0 && now > attempt.getDeadline()) {
            // Auto submit expired attempt
            Map<Integer, Integer> saved = new HashMap<>();
            if (attempt.getSavedAnswersJson() != null && !attempt.getSavedAnswersJson().isEmpty()) {
                try {
                    JsonObject ansObj = JsonParser.parseString(attempt.getSavedAnswersJson()).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : ansObj.entrySet()) {
                        saved.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt());
                    }
                } catch (Exception ignored) {}
            }
            submitAttempt(attemptId, studentUid, saved, "timeout");
            studentActiveAttempt.remove(studentUid);
            return null;
        }

        return attempt;
    }

    public synchronized void clearStudentActiveAttempt(String studentUid) {
        if (studentUid != null) {
            studentActiveAttempt.remove(studentUid);
        }
    }

    public synchronized boolean saveProgress(String attemptId, String studentUid, Map<Integer, Integer> answers) {
        TestAttempt attempt = getAttempt(attemptId, studentUid);
        if (attempt == null || isSubmitted(attempt)) {
            return false;
        }
        if (answers != null) {
            attempt.setSavedAnswersJson(gson.toJson(answers));
        }
        return true;
    }

    public synchronized JsonObject logProctoringEvent(String attemptId, String studentUid, JsonObject event) {
        JsonObject resp = new JsonObject();
        TestAttempt attempt = getAttempt(attemptId, studentUid);
        if (attempt == null || isSubmitted(attempt)) {
            resp.addProperty("success", false);
            return resp;
        }

        String type = event.has("type") ? event.get("type").getAsString() : "";
        if ("TAB_SWITCH".equalsIgnoreCase(type) || "FOCUS_LOST".equalsIgnoreCase(type) || "FULLSCREEN_EXIT".equalsIgnoreCase(type)) {
            attempt.setViolationCount(attempt.getViolationCount() + 1);
            if ("FOCUS_LOST".equalsIgnoreCase(type) || "TAB_SWITCH".equalsIgnoreCase(type)) {
                attempt.setFocusLostCount(attempt.getFocusLostCount() + 1);
            }
            if ("FULLSCREEN_EXIT".equalsIgnoreCase(type)) {
                attempt.setFullscreenExitCount(attempt.getFullscreenExitCount() + 1);
            }
        }

        // Persist event to Firebase snjbSecurityEvents node
        try {
            firebaseService.saveSnjbSecurityEvent(attemptId, event);
        } catch (Exception ignored) {}

        resp.addProperty("success", true);
        resp.addProperty("violationCount", attempt.getViolationCount());
        resp.addProperty("mustAutoSubmit", attempt.getViolationCount() >= 3);
        return resp;
    }

    /**
     * Authoritative submission and scoring of the test attempt (Centralized & Idempotent).
     */
    public synchronized TestEvaluationService.EvaluationPayload submitAttempt(String attemptId, String studentUid,
                                                                              Map<Integer, Integer> finalAnswers,
                                                                              String reason) {
        long tStart = System.currentTimeMillis();
        System.out.printf("[SUBMISSION-START] Attempt: %s | Student: %s | Reason: %s%n", attemptId, studentUid, reason);

        // Check if already evaluated and cached (Idempotency)
        if (attemptId != null && cachedResults.containsKey(attemptId)) {
            System.out.printf("[IDEMPOTENT-SUBMISSION] Attempt %s already finalized, returning cached result.%n", attemptId);
            return cachedResults.get(attemptId);
        }

        TestAttempt attempt = (attemptId != null) ? attemptRegistry.get(attemptId) : null;

        // Security check: Verify that student owns the attempt
        if (attempt != null && attempt.getUserId() != null && studentUid != null && !attempt.getUserId().equals(studentUid)) {
            System.err.printf("[SECURITY-VIOLATION] Attempt %s belongs to %s, unauthorized submit by %s%n",
                    attemptId, attempt.getUserId(), studentUid);
            throw new SecurityException("Unauthorized: Attempt does not belong to user " + studentUid);
        }

        // Idempotency: Return existing evaluation if attempt is already completed
        if (attempt != null && isSubmitted(attempt)) {
            TestEvaluationService.EvaluationPayload cached = cachedResults.get(attemptId);
            if (cached != null) {
                System.out.printf("[IDEMPOTENT-SUBMISSION] Attempt %s status is %s, returning cached evaluation.%n", attemptId, attempt.getStatus());
                return cached;
            }
        }

        if (attempt == null) {
            // Reconstruct attempt from active session or fallback parameters
            attempt = new TestAttempt();
            attempt.setId(attemptId != null ? attemptId : "att_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            attempt.setUserId(studentUid);
            attempt.setStudentName("Student");
            attempt.setSeriesId("college_snjb_full_mock");
            attempt.setCategory(TestSeries.CATEGORY_COLLEGE);
            attempt.setTopic("Quantitative Aptitude");
            attempt.setDifficulty("Medium");
            attempt.setStartTime(System.currentTimeMillis() - 1800000L);
            attempt.setDurationSeconds(1800);
            attempt.setDeadline(System.currentTimeMillis());
            attempt.setSelectedQuestionCount(30);
            attemptRegistry.put(attempt.getId(), attempt);
        }

        long tState = System.currentTimeMillis();

        List<Question> questions = protectedQuestions.get(attemptId);
        if (questions == null || questions.isEmpty()) {
            questions = new ArrayList<>();
            if (finalAnswers != null && !finalAnswers.isEmpty()) {
                for (Integer qId : finalAnswers.keySet()) {
                    Question q = PlacementQuestionBank.getQuestionById(qId);
                    if (q != null) questions.add(q);
                }
            }
            if (questions.isEmpty()) {
                questions = PlacementQuestionBank.getMixedMockQuestions(30);
            }
            protectedQuestions.put(attempt.getId(), questions);
        }

        TestSeries series = testSeriesService.getSeriesById(attempt.getSeriesId());
        if (series == null) {
            series = testSeriesService.getSeriesById("college_snjb_full_mock");
        }

        long now = System.currentTimeMillis();
        String explicitReason = (reason != null && !reason.trim().isEmpty()) ? reason : "manual";
        String status = "submitted";

        if ("proctor_violation".equalsIgnoreCase(explicitReason) || "automatic-security-submission".equalsIgnoreCase(explicitReason) || attempt.getViolationCount() >= 3) {
            status = "terminated";
        } else if ("timeout".equalsIgnoreCase(explicitReason) || (attempt.getDeadline() > 0 && now > attempt.getDeadline() + 10000)) {
            status = "auto_submitted";
        }

        int riskScore = Math.min(100, (attempt.getFocusLostCount() * 15) + (attempt.getFullscreenExitCount() * 25));
        if ("terminated".equals(status) || "auto_submitted_security".equals(status)) riskScore = 100;
        String riskLevel = (riskScore <= 15) ? "LOW" : (riskScore <= 40 ? "MODERATE" : "HIGH");
        String proctoringStatus = (riskScore > 40) ? "FLAGGED" : "VALIDATED";

        attempt.setRiskScore(riskScore);
        attempt.setRiskLevel(riskLevel);
        attempt.setProctoringStatus(proctoringStatus);
        attempt.setStatus(status);
        attempt.setSubmissionReason(explicitReason);
        attempt.setSubmissionTime(now);

        // Perform authoritative server-side grading
        TestEvaluationService.EvaluationPayload payload = evaluationService.evaluateAttempt(attempt, questions, finalAnswers, series);
        TestResult result = payload.result;

        attempt.setScore(result.getScore());
        attempt.setCorrectAnswers(result.getCorrectCount());
        attempt.setIncorrectAnswers(result.getIncorrectCount());
        attempt.setUnansweredCount(result.getUnansweredCount());
        attempt.setTotalQuestions(result.getTotalQuestions());
        attempt.setMarksEarned(result.getMarksEarned());
        attempt.setMaxMarks(result.getTotalMarks());
        attempt.setAccuracyPercentage(result.getAccuracy());
        attempt.setPassed(result.isPassed());
        attempt.setTimeTakenSeconds(result.getTimeTakenSeconds());
        attempt.setSavedAt(now);
        attempt.setSectionBreakdownJson(result.getSectionBreakdownJson());
        attempt.setWeakTopicsJson(gson.toJson(result.getWeakTopics()));
        attempt.setRecommendedPractice(gson.toJson(result.getRecommendedRevisionTopics()));
        attempt.setQuestionReviewsJson(payload.questionReviews.toString());
        result.setQuestionReviewsJson(payload.questionReviews.toString());

        long tGraded = System.currentTimeMillis();

        // Cache the evaluation payload for instant retrieval
        payload.attempt = attempt;
        cachedResults.put(attempt.getId(), payload);
        attemptRegistry.put(attempt.getId(), attempt);
        if (attemptId != null && !attemptId.equals(attempt.getId())) {
            cachedResults.put(attemptId, payload);
            attemptRegistry.put(attemptId, attempt);
        }

        // Persist attempt & result to Firebase (fast primary RTDB writes with async secondary mirrors)
        final TestAttempt finalAttempt = attempt;
        CompletableFuture<Boolean> attemptSaveFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return firebaseService.saveTestAttempt(studentUid, finalAttempt);
            } catch (Exception e) {
                return false;
            }
        });

        // Run secondary mirrors asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                firebaseService.saveSnjbTestResult(result);
                persistTestResult(result);
            } catch (Exception ignored) {}
        });

        // Synchronously wait for primary attempt save up to 1500ms; proceed without blocking if network is slow
        try {
            attemptSaveFuture.get(1500, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}

        long tPersist = System.currentTimeMillis();

        // Clear active pointer for student so they can take new tests
        studentActiveAttempt.remove(studentUid);
        TestController.clearActiveSession(studentUid);

        System.out.printf("[SUBMISSION-CONFIRMED] Attempt: %s | Student: %s | StateCheck: %dms | Grading: %dms | Persistence: %dms | Total: %dms | Status: %s | Score: %d%%%n",
                attempt.getId(), studentUid, (tState - tStart), (tGraded - tState), (tPersist - tGraded), (tPersist - tStart), status, attempt.getScore());

        return payload;
    }

    public synchronized TestEvaluationService.EvaluationPayload submitExam(String attemptId, String studentUid,
                                                                           Map<Integer, Integer> finalAnswers,
                                                                           String reason) {
        return submitAttempt(attemptId, studentUid, finalAnswers, reason);
    }

    private void persistTestResult(TestResult result) {
        if (result == null || result.getStudentUid() == null) return;
        try {
            JsonObject obj = firebaseService.serializeTestResult(result);
            String userResultUrl = Config.getFirebaseDatabaseUrl() + "/test_results/" + result.getStudentUid() + "/" + result.getResultId() + ".json";
            String globalResultUrl = Config.getFirebaseDatabaseUrl() + "/all_test_results/" + result.getResultId() + ".json";

            // Primary user result write
            firebaseService.executeHttpRequest(userResultUrl, "PUT", obj.toString());

            // Global result write asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    firebaseService.executeHttpRequest(globalResultUrl, "PUT", obj.toString());
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    /**
     * Periodic cleanup for expired or abandoned attempts (runs every 10 minutes).
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupExpiredAttempts() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, TestAttempt> entry : attemptRegistry.entrySet()) {
            TestAttempt att = entry.getValue();
            if (att.getExpiresAt() > 0 && now > att.getExpiresAt()) {
                toRemove.add(entry.getKey());
            }
        }

        for (String id : toRemove) {
            TestAttempt removed = attemptRegistry.remove(id);
            protectedQuestions.remove(id);
            if (removed != null && removed.getUserId() != null) {
                studentActiveAttempt.remove(removed.getUserId(), id);
            }
        }
    }

    private boolean isSubmitted(TestAttempt attempt) {
        String s = attempt.getStatus();
        return "submitted".equalsIgnoreCase(s) 
            || "auto_submitted".equalsIgnoreCase(s) 
            || "auto_submitted_security".equalsIgnoreCase(s) 
            || "auto_submitted_timeout".equalsIgnoreCase(s)
            || "terminated".equalsIgnoreCase(s)
            || "cancelled".equalsIgnoreCase(s);
    }
}
