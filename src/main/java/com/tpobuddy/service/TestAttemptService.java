package com.tpobuddy.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tpobuddy.model.*;
import com.tpobuddy.util.Config;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
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
            if (existing != null && !"submitted".equalsIgnoreCase(existing.getStatus())
                    && !"auto_submitted_security".equalsIgnoreCase(existing.getStatus())
                    && !"auto_submitted_timeout".equalsIgnoreCase(existing.getStatus())) {
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

        // Step 3 & 4: Generate 50 candidates & validate
        List<Question> candidatePool = aiGenerationService.generateCandidatePool(series, effDifficulty, customTopics, 50);

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

    public TestAttempt getAttempt(String attemptId, String studentUid) {
        TestAttempt attempt = attemptRegistry.get(attemptId);
        if (attempt != null && attempt.getUserId().equals(studentUid)) {
            return attempt;
        }
        return null;
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

        resp.addProperty("success", true);
        resp.addProperty("violationCount", attempt.getViolationCount());
        resp.addProperty("mustAutoSubmit", attempt.getViolationCount() >= 3);
        return resp;
    }

    /**
     * Authoritative submission and scoring of the test attempt.
     */
    public synchronized TestEvaluationService.EvaluationPayload submitAttempt(String attemptId, String studentUid,
                                                                              Map<Integer, Integer> finalAnswers,
                                                                              String reason) {
        // Check if already evaluated and cached (Idempotency)
        if (cachedResults.containsKey(attemptId)) {
            return cachedResults.get(attemptId);
        }

        TestAttempt attempt = attemptRegistry.get(attemptId);
        if (attempt == null || !attempt.getUserId().equals(studentUid)) {
            return null;
        }

        List<Question> questions = protectedQuestions.get(attemptId);
        TestSeries series = testSeriesService.getSeriesById(attempt.getSeriesId());

        long now = System.currentTimeMillis();
        String explicitReason = (reason != null && !reason.trim().isEmpty()) ? reason : "manual";
        String status = "submitted";

        if ("automatic-security-submission".equalsIgnoreCase(explicitReason) || attempt.getViolationCount() >= 3) {
            status = "auto_submitted_security";
        } else if ("timeout".equalsIgnoreCase(explicitReason) || (attempt.getDeadline() > 0 && now > attempt.getDeadline() + 10000)) {
            status = "auto_submitted_timeout";
        }

        int riskScore = Math.min(100, (attempt.getFocusLostCount() * 15) + (attempt.getFullscreenExitCount() * 25));
        if ("auto_submitted_security".equals(status)) riskScore = 100;
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
        attempt.setSectionBreakdownJson(result.getSectionBreakdownJson());
        attempt.setWeakTopicsJson(gson.toJson(result.getWeakTopics()));
        attempt.setRecommendedPractice(gson.toJson(result.getRecommendedRevisionTopics()));

        // Cache the evaluation payload
        cachedResults.put(attemptId, payload);

        // Persist attempt & result to Firebase
        try {
            firebaseService.saveTestAttempt(studentUid, attempt);
            persistTestResult(result);
        } catch (Exception e) {
            System.err.println("Firebase result persistence note: " + e.getMessage());
        }

        // Clear active pointer for student so they can take new tests
        studentActiveAttempt.remove(studentUid);

        return payload;
    }

    private void persistTestResult(TestResult result) {
        if (result == null || result.getStudentUid() == null) return;
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("resultId", result.getResultId());
            obj.addProperty("attemptId", result.getAttemptId());
            obj.addProperty("studentUid", result.getStudentUid());
            obj.addProperty("studentName", result.getStudentName());
            obj.addProperty("category", result.getCategory());
            obj.addProperty("seriesId", result.getSeriesId());
            obj.addProperty("seriesName", result.getSeriesName());
            obj.addProperty("subject", result.getSubject());
            obj.addProperty("difficulty", result.getDifficulty());
            obj.addProperty("score", result.getScore());
            obj.addProperty("accuracy", result.getAccuracy());
            obj.addProperty("correctCount", result.getCorrectCount());
            obj.addProperty("incorrectCount", result.getIncorrectCount());
            obj.addProperty("unansweredCount", result.getUnansweredCount());
            obj.addProperty("totalQuestions", result.getTotalQuestions());
            obj.addProperty("passed", result.isPassed());
            obj.addProperty("sectionBreakdownJson", result.getSectionBreakdownJson());
            obj.addProperty("topicWisePerformanceJson", result.getTopicWisePerformanceJson());
            obj.addProperty("weakTopicsJson", gson.toJson(result.getWeakTopics()));
            obj.addProperty("completedAt", result.getCompletedAt());

            String userResultUrl = Config.getFirebaseDatabaseUrl() + "/test_results/" + result.getStudentUid() + "/" + result.getResultId() + ".json";
            String globalResultUrl = Config.getFirebaseDatabaseUrl() + "/all_test_results/" + result.getResultId() + ".json";
            firebaseService.executeHttpRequest(userResultUrl, "PUT", obj.toString());
            firebaseService.executeHttpRequest(globalResultUrl, "PUT", obj.toString());
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
        return "submitted".equalsIgnoreCase(s) || "auto_submitted_security".equalsIgnoreCase(s) || "auto_submitted_timeout".equalsIgnoreCase(s);
    }
}
