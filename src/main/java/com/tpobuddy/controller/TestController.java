package com.tpobuddy.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tpobuddy.model.PlacementTest;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.StudentMetrics;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.TestResult;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.PlacementQuestionBank;
import com.tpobuddy.service.PlacementTestService;
import com.tpobuddy.service.TestEvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller managing the Company Placement Preparation Test Series for students.
 * Features:
 * - Standardized Sectional, Topic-wise, and Full Mock Placement Tests
 * - Sub-millisecond question retrieval
 * - Anti-cheating fullscreen and tab-switch monitoring
 * - Session state resilience across page reloads
 * - Secure server-side grading and step-by-step solution explanations
 * - Real-time synchronization with Firebase student profiles
 */
@Controller
public class TestController {

    private final FirebaseService firebaseService;
    private final PlacementTestService placementTestService;
    private final com.tpobuddy.service.TestSeriesService testSeriesService;
    private final com.tpobuddy.service.TestAttemptService testAttemptService;

    // Active session tracking: key = userId, value = ActiveExamSession
    private static final Map<String, ActiveExamSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    public static void clearActiveSession(String userId) {
        if (userId != null) {
            ACTIVE_SESSIONS.remove(userId);
        }
    }

    public TestController(FirebaseService firebaseService,
                          PlacementTestService placementTestService,
                          com.tpobuddy.service.TestSeriesService testSeriesService,
                          com.tpobuddy.service.TestAttemptService testAttemptService) {
        this.firebaseService = firebaseService;
        this.placementTestService = placementTestService;
        this.testSeriesService = testSeriesService;
        this.testAttemptService = testAttemptService;
    }

    public static class ActiveExamSession {
        public String attemptId;
        public String userId;
        public String category;
        public String topic;
        public String testId;
        public List<Integer> assignedQuestionIds = new ArrayList<>();
        public Map<Integer, Integer> savedAnswers = new ConcurrentHashMap<>();
        public Set<Integer> markedForReview = Collections.newSetFromMap(new ConcurrentHashMap<>());
        public List<JsonObject> monitoringEvents = Collections.synchronizedList(new ArrayList<>());
        public int violationCount = 0;
        public long startTime;
        public int durationSeconds;
        public long deadline;
        public boolean submitted = false;
        public int finalScore = 0;
        public int correctCount = 0;
        public int riskScore = 0;
        public String riskLevel = "LOW";
        public String status = "in_progress";
        public String submissionReason = "manual";
        public long submissionTime = 0;
        public int negativeMarking = 0; // 0 = false, 1 = true
        public double negativeMarks = 0.5;
    }

    // =========================================================================
    // View Endpoint
    // =========================================================================

    @GetMapping({"/tests", "/tests.jsp"})
    public String viewTests(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp?error=unauthorized";
        }

        List<TestAttempt> attempts = firebaseService.fetchTestAttempts(user.getUid());
        List<PlacementTest> availableTests = placementTestService.getAllTests();
        List<com.tpobuddy.model.TestSeries> collegeSeries = testSeriesService.getSeriesByCategory(com.tpobuddy.model.TestSeries.CATEGORY_COLLEGE);
        List<com.tpobuddy.model.TestSeries> platformSeries = testSeriesService.getSeriesByCategory(com.tpobuddy.model.TestSeries.CATEGORY_PLATFORM);

        model.addAttribute("user", user);
        model.addAttribute("attempts", attempts != null ? attempts : Collections.emptyList());
        model.addAttribute("availableTests", availableTests);
        model.addAttribute("collegeSeries", collegeSeries);
        model.addAttribute("platformSeries", platformSeries);
        model.addAttribute("categories", PlacementQuestionBank.getCategoriesMetadata());
        model.addAttribute("totalQuestionBankSize", PlacementQuestionBank.getTotalQuestionCount());

        return "tests";
    }

    // =========================================================================
    // REST APIs for Test Engine
    // =========================================================================

    @GetMapping(value = "/api/tests/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getCategories() {
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("categories", new Gson().toJsonTree(PlacementQuestionBank.getCategoriesMetadata()));
        resp.addProperty("totalQuestionCount", PlacementQuestionBank.getTotalQuestionCount());
        return ResponseEntity.ok(resp.toString());
    }

    @GetMapping(value = "/api/tests/active-session", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getActiveSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"hasActiveSession\":false}");
        }

        // 1. Check authoritative TestAttemptService first
        TestAttempt aiAttempt = testAttemptService.getActiveAttemptForStudent(user.getUid());
        if (aiAttempt != null) {
            long now = System.currentTimeMillis();
            long remaining = Math.max(0, (aiAttempt.getDeadline() - now) / 1000);
            if (remaining > 0) {
                JsonArray sanitizedQuestions = testAttemptService.getSanitizedQuestions(aiAttempt.getId(), user.getUid());
                if (sanitizedQuestions != null && sanitizedQuestions.size() > 0) {
                    JsonObject resp = new JsonObject();
                    resp.addProperty("hasActiveSession", true);
                    resp.addProperty("attemptId", aiAttempt.getId());
                    resp.addProperty("category", aiAttempt.getCategory());
                    resp.addProperty("topic", aiAttempt.getTopic());
                    resp.addProperty("testTitle", aiAttempt.getTestTitle() != null ? aiAttempt.getTestTitle() : aiAttempt.getTopic());
                    resp.addProperty("testId", aiAttempt.getSeriesId() != null ? aiAttempt.getSeriesId() : aiAttempt.getTestId());
                    resp.addProperty("difficulty", aiAttempt.getDifficulty());
                    resp.addProperty("remainingSeconds", remaining);
                    resp.addProperty("violationCount", aiAttempt.getViolationCount());
                    resp.addProperty("questionCount", sanitizedQuestions.size());
                    if (aiAttempt.getSavedAnswersJson() != null && !aiAttempt.getSavedAnswersJson().isEmpty()) {
                        try {
                            resp.add("savedAnswers", JsonParser.parseString(aiAttempt.getSavedAnswersJson()));
                        } catch (Exception ignored) {
                            resp.add("savedAnswers", new JsonObject());
                        }
                    } else {
                        resp.add("savedAnswers", new JsonObject());
                    }
                    resp.add("questions", sanitizedQuestions);
                    return ResponseEntity.ok(resp.toString());
                }
            }
        }

        // 2. Check legacy ACTIVE_SESSIONS map
        ActiveExamSession examSession = ACTIVE_SESSIONS.get(user.getUid());
        if (examSession == null || examSession.submitted) {
            ACTIVE_SESSIONS.remove(user.getUid());
            return ResponseEntity.ok("{\"hasActiveSession\":false}");
        }

        long now = System.currentTimeMillis();
        long remaining = Math.max(0, (examSession.deadline - now) / 1000);
        if (remaining <= 0) {
            // Auto submit if expired and clear
            JsonObject autoSubmit = new JsonObject();
            autoSubmit.addProperty("attemptId", examSession.attemptId);
            autoSubmit.addProperty("submissionReason", "timeout");
            ACTIVE_SESSIONS.remove(user.getUid());
            return submitExam(autoSubmit.toString(), request);
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("hasActiveSession", true);
        resp.addProperty("attemptId", examSession.attemptId);
        resp.addProperty("category", examSession.category);
        resp.addProperty("topic", examSession.topic);
        resp.addProperty("testTitle", examSession.topic);
        resp.addProperty("testId", examSession.testId);
        resp.addProperty("remainingSeconds", remaining);
        resp.addProperty("violationCount", examSession.violationCount);
        resp.addProperty("questionCount", examSession.assignedQuestionIds.size());
        resp.add("savedAnswers", new Gson().toJsonTree(examSession.savedAnswers));

        // Return sanitized questions without answers
        JsonArray qArr = new JsonArray();
        for (int qId : examSession.assignedQuestionIds) {
            Question q = PlacementQuestionBank.getQuestionById(qId);
            if (q != null) {
                JsonObject qObj = new JsonObject();
                qObj.addProperty("id", q.getId());
                qObj.addProperty("category", q.getCategory());
                qObj.addProperty("topic", q.getTopic());
                qObj.addProperty("difficulty", q.getDifficulty());
                qObj.addProperty("question", q.getQuestion());
                qObj.addProperty("optionA", q.getOptionA());
                qObj.addProperty("optionB", q.getOptionB());
                qObj.addProperty("optionC", q.getOptionC());
                qObj.addProperty("optionD", q.getOptionD());
                qObj.addProperty("marks", q.getMarks());
                qArr.add(qObj);
            }
        }
        resp.add("questions", qArr);

        return ResponseEntity.ok(resp.toString());
    }

    @PostMapping(value = "/api/tests/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> startTest(@RequestBody String body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false,\"error\":\"Authentication required\"}");
        }

        JsonObject payload = JsonParser.parseString(body).getAsJsonObject();
        String category = payload.has("category") ? payload.get("category").getAsString() : "Quantitative Aptitude";
        String topic = payload.has("topic") ? payload.get("topic").getAsString() : null;
        String testId = payload.has("testId") ? payload.get("testId").getAsString() : null;
        String difficulty = payload.has("difficulty") ? payload.get("difficulty").getAsString() : "Medium";
        
        int count = 30;
        if (payload.has("count")) {
            try {
                count = payload.get("count").getAsInt();
            } catch (Exception e) {
                try {
                    count = Integer.parseInt(payload.get("count").getAsString());
                } catch (Exception ignored) {
                    count = 30;
                }
            }
        }

        // Map seriesId aliases if passed as category
        if ("college_snjb_quant".equalsIgnoreCase(category)) {
            category = "Quantitative Aptitude";
        } else if ("college_snjb_logical".equalsIgnoreCase(category)) {
            category = "Logical Reasoning";
        } else if ("college_snjb_verbal".equalsIgnoreCase(category)) {
            category = "Verbal Ability";
        } else if ("college_snjb_tech".equalsIgnoreCase(category)) {
            category = "Technical and Coding";
        } else if ("college_snjb_full_mock".equalsIgnoreCase(category)) {
            category = "all";
        }

        PlacementTest pTest = (testId != null) ? placementTestService.getTestById(testId) : null;
        if (pTest != null) {
            category = pTest.getCategory();
            if (pTest.getSubCategory() != null && !"Comprehensive".equalsIgnoreCase(pTest.getSubCategory()) && !"Full Mock".equalsIgnoreCase(pTest.getSubCategory())) {
                topic = pTest.getSubCategory();
            }
            count = pTest.getQuestionCount();
        }

        List<Question> questions;
        if (topic != null && !topic.trim().isEmpty() && !"all".equalsIgnoreCase(topic) && !"comprehensive".equalsIgnoreCase(topic)) {
            questions = PlacementQuestionBank.getQuestionsForTopic(topic, count);
        } else if ("all".equalsIgnoreCase(category) || "Comprehensive Mock".equalsIgnoreCase(category) || "Company Mock".equalsIgnoreCase(category)) {
            questions = PlacementQuestionBank.getMixedMockQuestions(count);
        } else {
            questions = PlacementQuestionBank.getQuestionsForCategory(category, count);
        }

        if (questions == null || questions.isEmpty()) {
            if (topic != null && !topic.trim().isEmpty() && !"all".equalsIgnoreCase(topic)) {
                // Keep strictly within category rather than mixing unrelated sections
                questions = PlacementQuestionBank.getQuestionsForCategory(category, count);
            } else {
                questions = PlacementQuestionBank.getMixedMockQuestions(count);
            }
        }

        String attemptId = "att_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long now = System.currentTimeMillis();
        int durationMins = (pTest != null) ? pTest.getDurationMinutes() : (count <= 10 ? 15 : 30);
        int durationSecs = durationMins * 60;
        long deadline = now + (durationSecs * 1000L);

        ActiveExamSession examSession = new ActiveExamSession();
        examSession.attemptId = attemptId;
        examSession.userId = user.getUid();
        examSession.category = category;
        examSession.topic = (topic != null) ? topic : "General Assessment";
        examSession.testId = (testId != null) ? testId : "test_" + category.toLowerCase().replace(" ", "_");
        examSession.startTime = now;
        examSession.durationSeconds = durationSecs;
        examSession.deadline = deadline;
        if (pTest != null && pTest.isNegativeMarking()) {
            examSession.negativeMarking = 1;
            examSession.negativeMarks = pTest.getNegativeMarks();
        }

        JsonArray sanitizedQuestions = new JsonArray();
        for (Question q : questions) {
            examSession.assignedQuestionIds.add(q.getId());

            JsonObject qObj = new JsonObject();
            qObj.addProperty("id", q.getId());
            qObj.addProperty("category", q.getCategory());
            qObj.addProperty("topic", q.getTopic());
            qObj.addProperty("difficulty", q.getDifficulty());
            qObj.addProperty("question", q.getQuestion());
            qObj.addProperty("optionA", q.getOptionA());
            qObj.addProperty("optionB", q.getOptionB());
            qObj.addProperty("optionC", q.getOptionC());
            qObj.addProperty("optionD", q.getOptionD());
            qObj.addProperty("marks", q.getMarks());
            sanitizedQuestions.add(qObj);
        }

        ACTIVE_SESSIONS.put(user.getUid(), examSession);

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("attemptId", attemptId);
        resp.addProperty("category", examSession.category);
        resp.addProperty("topic", examSession.topic);
        resp.addProperty("durationSeconds", durationSecs);
        resp.addProperty("deadline", deadline);
        resp.addProperty("questionCount", sanitizedQuestions.size());
        resp.add("questions", sanitizedQuestions);

        return ResponseEntity.ok(resp.toString());
    }

    @PostMapping(value = "/api/tests/save-progress", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> saveProgress(@RequestBody String body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false}");
        }

        ActiveExamSession examSession = ACTIVE_SESSIONS.get(user.getUid());
        if (examSession == null || examSession.submitted) {
            return ResponseEntity.ok("{\"success\":false,\"message\":\"No active session\"}");
        }

        JsonObject payload = JsonParser.parseString(body).getAsJsonObject();
        if (payload.has("answers") && payload.get("answers").isJsonObject()) {
            JsonObject answers = payload.getAsJsonObject("answers");
            for (Map.Entry<String, JsonElement> e : answers.entrySet()) {
                try {
                    int qId = Integer.parseInt(e.getKey());
                    int opt = e.getValue().getAsInt();
                    examSession.savedAnswers.put(qId, opt);
                } catch (Exception ignored) {}
            }
        }

        return ResponseEntity.ok("{\"success\":true}");
    }

    @PostMapping(value = "/api/tests/log-event", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> logEvent(@RequestBody String body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false}");
        }

        ActiveExamSession examSession = ACTIVE_SESSIONS.get(user.getUid());
        if (examSession == null || examSession.submitted) {
            return ResponseEntity.ok("{\"success\":false}");
        }

        JsonObject payload = JsonParser.parseString(body).getAsJsonObject();
        examSession.monitoringEvents.add(payload);

        String type = payload.has("type") ? payload.get("type").getAsString() : "";
        if ("TAB_SWITCH".equalsIgnoreCase(type) || "FOCUS_LOST".equalsIgnoreCase(type) || "FULLSCREEN_EXIT".equalsIgnoreCase(type)) {
            examSession.violationCount++;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("violationCount", examSession.violationCount);
        resp.addProperty("mustAutoSubmit", examSession.violationCount >= 3);

        return ResponseEntity.ok(resp.toString());
    }

    @PostMapping(value = "/api/tests/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> submitExam(@RequestBody String body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }

        JsonObject submission = JsonParser.parseString(body).getAsJsonObject();
        ActiveExamSession examSession = ACTIVE_SESSIONS.get(user.getUid());

        String attemptId = (submission.has("attemptId") && !submission.get("attemptId").getAsString().isEmpty())
                ? submission.get("attemptId").getAsString()
                : (examSession != null ? examSession.attemptId : null);

        String explicitReason = submission.has("submissionReason") ? submission.get("submissionReason").getAsString() : "manual";

        Map<Integer, Integer> finalAnswers = new HashMap<>();
        if (examSession != null && examSession.savedAnswers != null) {
            finalAnswers.putAll(examSession.savedAnswers);
        }
        if (submission.has("answers") && submission.get("answers").isJsonObject()) {
            JsonObject ansObj = submission.getAsJsonObject("answers");
            for (Map.Entry<String, JsonElement> e : ansObj.entrySet()) {
                try {
                    finalAnswers.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt());
                } catch (Exception ignored) {}
            }
        }

        TestEvaluationService.EvaluationPayload eval;
        try {
            eval = testAttemptService.submitAttempt(attemptId, user.getUid(), finalAnswers, explicitReason);
        } catch (SecurityException se) {
            System.err.println("Security exception in submitExam: " + se.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"" + se.getMessage() + "\"}");
        } catch (Exception e) {
            System.err.println("Error in submitExam delegation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"success\":false,\"error\":\"Internal submission error\"}");
        }

        ACTIVE_SESSIONS.remove(user.getUid());
        testAttemptService.clearStudentActiveAttempt(user.getUid());

        TestResult res = (eval != null) ? eval.result : null;
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("attemptId", (res != null && res.getAttemptId() != null) ? res.getAttemptId() : attemptId);
        result.addProperty("resultId", (res != null && res.getResultId() != null) ? res.getResultId() : ("res_" + attemptId));
        result.addProperty("score", res != null ? res.getScore() : 0);
        result.addProperty("correctCount", res != null ? res.getCorrectCount() : 0);
        result.addProperty("incorrectCount", res != null ? res.getIncorrectCount() : 0);
        result.addProperty("unansweredCount", res != null ? res.getUnansweredCount() : 0);
        result.addProperty("totalQuestions", res != null ? res.getTotalQuestions() : 30);
        result.addProperty("passed", res != null && res.isPassed());
        result.addProperty("riskScore", res != null ? res.getRiskScore() : 0);
        result.addProperty("riskLevel", (res != null && res.getRiskLevel() != null) ? res.getRiskLevel() : "LOW");
        result.addProperty("proctoringStatus", (res != null && res.getProctoringStatus() != null) ? res.getProctoringStatus() : "VALIDATED");
        result.addProperty("examStatus", (res != null && res.isPassed()) ? "passed" : "submitted");
        result.add("reviews", (eval != null && eval.questionReviews != null) ? eval.questionReviews : new JsonArray());

        return ResponseEntity.ok(result.toString());
    }

    @GetMapping({"/test-result", "/tests/result/{attemptId}"})
    public String viewTestResult(@PathVariable(value = "attemptId", required = false) String pathAttemptId,
                                 @RequestParam(value = "attemptId", required = false) String paramAttemptId,
                                 HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp?error=unauthorized";
        }

        String attemptId = (pathAttemptId != null && !pathAttemptId.trim().isEmpty())
                ? pathAttemptId.trim()
                : (paramAttemptId != null ? paramAttemptId.trim() : null);

        System.out.printf("[RESULT-REQUEST] Student: %s | AttemptId: %s%n", user.getUid(), attemptId);

        TestAttempt targetAttempt = null;

        // 1. Instant check in memory / cached results (0ms)
        if (attemptId != null) {
            targetAttempt = testAttemptService.getAttempt(attemptId, user.getUid());
        }

        try {
            // 2. Direct single-attempt query in Firebase (~100ms)
            if (targetAttempt == null && attemptId != null) {
                try {
                    targetAttempt = firebaseService.fetchTestAttemptById(attemptId, user.getUid());
                } catch (Exception fEx) {
                    System.err.println("Notice: Firebase direct fetch returned error: " + fEx.getMessage());
                }
            }

            // 3. Fallback: Query student's attempt list
            if (targetAttempt == null) {
                try {
                    List<TestAttempt> userAttempts = firebaseService.fetchTestAttempts(user.getUid());
                    if (userAttempts != null && !userAttempts.isEmpty()) {
                        if (attemptId != null) {
                            for (TestAttempt a : userAttempts) {
                                if (attemptId.equals(a.getId()) || (a.getId() != null && a.getId().contains(attemptId))) {
                                    targetAttempt = a;
                                    break;
                                }
                            }
                        }
                        if (targetAttempt == null) {
                            targetAttempt = userAttempts.get(0);
                        }
                    }
                } catch (Exception fListEx) {
                    System.err.println("Notice: Firebase attempt list fetch returned error: " + fListEx.getMessage());
                }
            }

            // 4. Missing attempt verification
            if (targetAttempt == null) {
                System.err.printf("[RESULT-NOT-FOUND] No attempt found for ID: %s and student: %s%n", attemptId, user.getUid());
                return "redirect:/tests?error=attempt_not_found";
            }

            // 5. Ownership verification
            if (targetAttempt.getUserId() != null && !targetAttempt.getUserId().trim().isEmpty()) {
                if (!targetAttempt.getUserId().equals(user.getUid()) && !user.isAdmin() && !user.isTeacher()) {
                    System.err.printf("[RESULT-FORBIDDEN] Unauthorized attempt access: user %s requested attempt owned by %s%n",
                            user.getUid(), targetAttempt.getUserId());
                    return "redirect:/tests?error=unauthorized_attempt";
                }
            }

            model.addAttribute("user", user);
            model.addAttribute("attempt", targetAttempt);

            // Parse section breakdown safely
            Map<String, Map<String, Object>> sectionBreakdown = new LinkedHashMap<>();
            try {
                if (targetAttempt.getSectionBreakdownJson() != null && !targetAttempt.getSectionBreakdownJson().isEmpty()) {
                    JsonElement parsed = JsonParser.parseString(targetAttempt.getSectionBreakdownJson());
                    if (parsed != null && parsed.isJsonObject()) {
                        JsonObject secObj = parsed.getAsJsonObject();
                        for (String key : secObj.keySet()) {
                            JsonElement valEl = secObj.get(key);
                            if (valEl != null && valEl.isJsonObject()) {
                                JsonObject s = valEl.getAsJsonObject();
                                Map<String, Object> map = new HashMap<>();
                                map.put("correct", (s.has("correct") && !s.get("correct").isJsonNull()) ? s.get("correct").getAsInt() : 0);
                                map.put("total", (s.has("total") && !s.get("total").isJsonNull()) ? s.get("total").getAsInt() : 0);
                                map.put("scorePercent", (s.has("scorePercent") && !s.get("scorePercent").isJsonNull()) ? s.get("scorePercent").getAsInt() : 0);
                                sectionBreakdown.put(key, map);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            model.addAttribute("sectionBreakdown", sectionBreakdown);

            // Parse weak topics safely
            List<String> weakTopics = new ArrayList<>();
            try {
                if (targetAttempt.getWeakTopicsJson() != null && !targetAttempt.getWeakTopicsJson().isEmpty()) {
                    JsonElement parsed = JsonParser.parseString(targetAttempt.getWeakTopicsJson());
                    if (parsed != null && parsed.isJsonArray()) {
                        for (JsonElement el : parsed.getAsJsonArray()) {
                            if (el != null && !el.isJsonNull()) {
                                String wStr = el.getAsString();
                                if (wStr != null && !wStr.trim().isEmpty()) {
                                    weakTopics.add(wStr.trim());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            model.addAttribute("weakTopics", weakTopics);

            // Parse question reviews safely
            JsonArray reviewsArray = new JsonArray();
            try {
                if (targetAttempt.getQuestionReviewsJson() != null && !targetAttempt.getQuestionReviewsJson().trim().isEmpty() && !targetAttempt.getQuestionReviewsJson().equals("[]")) {
                    JsonElement parsed = JsonParser.parseString(targetAttempt.getQuestionReviewsJson());
                    if (parsed != null && parsed.isJsonArray()) {
                        reviewsArray = parsed.getAsJsonArray();
                    }
                }
            } catch (Exception ignored) {}

            // Fallback reconstruction if reviewsArray is empty
            if (reviewsArray.isEmpty() && targetAttempt.getSavedAnswersJson() != null) {
                try {
                    JsonObject answers = JsonParser.parseString(targetAttempt.getSavedAnswersJson()).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : answers.entrySet()) {
                        int qId = Integer.parseInt(e.getKey());
                        int userAns = e.getValue().getAsInt();
                        Question q = PlacementQuestionBank.getQuestionById(qId);
                        if (q != null) {
                            JsonObject r = new JsonObject();
                            r.addProperty("questionId", qId);
                            r.addProperty("question", q.getQuestion());
                            r.addProperty("section", q.getCategory());
                            r.addProperty("topic", q.getTopic());
                            r.addProperty("difficulty", q.getDifficulty());
                            r.addProperty("optionA", q.getOptionA());
                            r.addProperty("optionB", q.getOptionB());
                            r.addProperty("optionC", q.getOptionC());
                            r.addProperty("optionD", q.getOptionD());
                            r.addProperty("selectedOption", userAns);
                            r.addProperty("correctOption", q.getCorrectOption());
                            r.addProperty("isCorrect", userAns == q.getCorrectOption());
                            r.addProperty("isAnswered", userAns >= 0);
                            r.addProperty("explanation", q.getExplanation());
                            reviewsArray.add(r);
                        }
                    }
                } catch (Exception ignored) {}
            }
            String safeReviewsJson = reviewsArray.toString().replace("</script>", "<\\/script>");
            model.addAttribute("reviewsJson", safeReviewsJson);
            model.addAttribute("reviewsCount", reviewsArray.size());

            // Determine recommended next test
            String recommendedSeriesId = "college_snjb_quant";
            String recommendedTestTitle = "Quantitative Aptitude - Number Systems";
            String recommendedSubject = "Quantitative Aptitude";

            if (!weakTopics.isEmpty() && weakTopics.get(0) != null) {
                String firstWeak = weakTopics.get(0).trim();
                recommendedTestTitle = "Focused Practice: " + firstWeak;
                String lower = firstWeak.toLowerCase();
                if (lower.contains("code") || lower.contains("java") || lower.contains("program")) {
                    recommendedSeriesId = "college_snjb_tech";
                    recommendedSubject = "Technical and Coding";
                } else if (lower.contains("logic") || lower.contains("puzzle") || lower.contains("relation")) {
                    recommendedSeriesId = "college_snjb_logical";
                    recommendedSubject = "Logical Reasoning";
                } else if (lower.contains("verbal") || lower.contains("grammar") || lower.contains("vocab")) {
                    recommendedSeriesId = "college_snjb_verbal";
                    recommendedSubject = "Verbal Ability";
                } else {
                    recommendedSeriesId = "college_snjb_quant";
                    recommendedSubject = "Quantitative Aptitude";
                }
            } else {
                // Advance in sequence
                String curCat = targetAttempt.getCategory() != null ? targetAttempt.getCategory() : "";
                if (curCat.contains("Quantitative")) {
                    recommendedSeriesId = "college_snjb_logical";
                    recommendedTestTitle = "SNJB Logical Reasoning Placement Test";
                    recommendedSubject = "Logical Reasoning";
                } else if (curCat.contains("Logical")) {
                    recommendedSeriesId = "college_snjb_verbal";
                    recommendedTestTitle = "SNJB Verbal Ability Placement Test";
                    recommendedSubject = "Verbal Ability";
                } else if (curCat.contains("Verbal")) {
                    recommendedSeriesId = "college_snjb_tech";
                    recommendedTestTitle = "SNJB Technical & Coding Placement Test";
                    recommendedSubject = "Technical and Coding";
                } else {
                    recommendedSeriesId = "college_snjb_full_mock";
                    recommendedTestTitle = "SNJB Comprehensive Full Placement Mock";
                    recommendedSubject = "Full Mock";
                }
            }

            model.addAttribute("recommendedSeriesId", recommendedSeriesId);
            model.addAttribute("recommendedTestTitle", recommendedTestTitle);
            model.addAttribute("recommendedSubject", recommendedSubject);

            return "test-result";
        } catch (Exception ex) {
            System.err.println("Unhandled error preparing test result view: " + ex.getMessage());
            ex.printStackTrace();
            model.addAttribute("errorMessage", "Unable to display test results at this time. Please try refreshing or checking your progress dashboard.");
            return "redirect:/dashboard?error=result_view_error";
        }
    }

    @GetMapping(value = "/api/test-results", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getTestResults(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }
        List<TestAttempt> attempts = firebaseService.fetchTestAttempts(user.getUid());
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("results", new Gson().toJsonTree(attempts));
        return ResponseEntity.ok(resp.toString());
    }

    @GetMapping(value = "/api/test-results/{resultId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getTestResultById(@PathVariable("resultId") String resultId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }
        List<TestAttempt> attempts = firebaseService.fetchTestAttempts(user.getUid());
        for (TestAttempt att : attempts) {
            if (resultId.equals(att.getId()) || (att.getId() != null && att.getId().contains(resultId))) {
                JsonObject resp = new JsonObject();
                resp.addProperty("success", true);
                resp.add("result", new Gson().toJsonTree(att));
                return ResponseEntity.ok(resp.toString());
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"success\":false,\"error\":\"Result not found\"}");
    }
}
