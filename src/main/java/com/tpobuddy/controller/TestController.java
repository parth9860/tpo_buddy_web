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
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.PlacementQuestionBank;
import com.tpobuddy.service.PlacementTestService;
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

    // Active session tracking: key = userId, value = ActiveExamSession
    private static final Map<String, ActiveExamSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    public TestController(FirebaseService firebaseService,
                          PlacementTestService placementTestService,
                          com.tpobuddy.service.TestSeriesService testSeriesService) {
        this.firebaseService = firebaseService;
        this.placementTestService = placementTestService;
        this.testSeriesService = testSeriesService;
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

        ActiveExamSession examSession = ACTIVE_SESSIONS.get(user.getUid());
        if (examSession == null || examSession.submitted) {
            return ResponseEntity.ok("{\"hasActiveSession\":false}");
        }

        long now = System.currentTimeMillis();
        long remaining = Math.max(0, (examSession.deadline - now) / 1000);
        if (remaining <= 0) {
            // Auto submit if expired
            JsonObject autoSubmit = new JsonObject();
            autoSubmit.addProperty("attemptId", examSession.attemptId);
            autoSubmit.addProperty("submissionReason", "timeout");
            return submitExam(autoSubmit.toString(), request);
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("hasActiveSession", true);
        resp.addProperty("attemptId", examSession.attemptId);
        resp.addProperty("category", examSession.category);
        resp.addProperty("topic", examSession.topic);
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
            questions = PlacementQuestionBank.getMixedMockQuestions(count);
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

        String attemptId = (examSession != null) ? examSession.attemptId :
                (submission.has("attemptId") ? submission.get("attemptId").getAsString() : "att_" + System.currentTimeMillis());

        String category = (examSession != null) ? examSession.category :
                (submission.has("category") ? submission.get("category").getAsString() : "Quantitative Aptitude");
        String topic = (examSession != null) ? examSession.topic : "Placement Practice";

        Map<Integer, Integer> finalAnswers = new HashMap<>();
        if (examSession != null) {
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

        long now = System.currentTimeMillis();
        String explicitReason = submission.has("submissionReason") ? submission.get("submissionReason").getAsString() : "manual";
        String status = "submitted";

        if ("automatic-security-submission".equalsIgnoreCase(explicitReason) || (examSession != null && examSession.violationCount >= 3)) {
            status = "auto_submitted_security";
        } else if ("timeout".equalsIgnoreCase(explicitReason) || (examSession != null && examSession.deadline > 0 && now > examSession.deadline + 15000)) {
            status = "auto_submitted_timeout";
        }

        int focusLostCount = 0;
        int fullscreenExitCount = 0;
        if (examSession != null) {
            for (JsonObject evt : examSession.monitoringEvents) {
                String type = evt.has("type") ? evt.get("type").getAsString() : "";
                if ("FOCUS_LOST".equalsIgnoreCase(type) || "TAB_SWITCH".equalsIgnoreCase(type)) focusLostCount++;
                if ("FULLSCREEN_EXIT".equalsIgnoreCase(type)) fullscreenExitCount++;
            }
        }

        int riskScore = Math.min(100, (focusLostCount * 15) + (fullscreenExitCount * 25));
        if ("auto_submitted_security".equals(status)) riskScore = 100;
        String riskLevel = (riskScore <= 15) ? "LOW" : (riskScore <= 40 ? "MODERATE" : "HIGH");
        String proctoringStatus = (riskScore > 40) ? "FLAGGED" : "VALIDATED";

        List<Integer> questionIdsToScore = (examSession != null && !examSession.assignedQuestionIds.isEmpty())
                ? examSession.assignedQuestionIds
                : new ArrayList<>(finalAnswers.keySet());

        int totalQuestions = Math.max(1, questionIdsToScore.size());
        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;
        JsonArray reviews = new JsonArray();

        for (int qId : questionIdsToScore) {
            Question q = PlacementQuestionBank.getQuestionById(qId);
            if (q != null) {
                Integer userAns = finalAnswers.get(qId);
                boolean isAnswered = (userAns != null && userAns >= 0);
                boolean isCorrect = (isAnswered && userAns == q.getCorrectOption());

                if (isCorrect) {
                    correctCount++;
                } else if (isAnswered) {
                    incorrectCount++;
                } else {
                    unansweredCount++;
                }

                JsonObject r = new JsonObject();
                r.addProperty("questionId", qId);
                r.addProperty("question", q.getQuestion());
                r.addProperty("optionA", q.getOptionA());
                r.addProperty("optionB", q.getOptionB());
                r.addProperty("optionC", q.getOptionC());
                r.addProperty("optionD", q.getOptionD());
                r.addProperty("selectedOption", (userAns != null) ? userAns : -1);
                r.addProperty("correctOption", q.getCorrectOption());
                r.addProperty("isCorrect", isCorrect);
                r.addProperty("explanation", q.getExplanation());
                reviews.add(r);
            }
        }

        int scorePercent = Math.round((correctCount * 100.0f) / totalQuestions);
        boolean passed = scorePercent >= 60;

        // Persist attempt to Firebase Realtime Database
        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId);
        attempt.setTestId(category);
        attempt.setUserId(user.getUid());
        attempt.setStudentName(user.getName());
        attempt.setCategory(category);
        attempt.setTopic(topic);
        attempt.setScore(scorePercent);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setCorrectAnswers(correctCount);
        attempt.setSavedAt(now);
        attempt.setSubmissionTime(now);
        attempt.setPassed(passed);
        attempt.setStartTime(examSession != null ? examSession.startTime : now);
        attempt.setDurationSeconds(examSession != null ? examSession.durationSeconds : 1800);
        attempt.setDeadline(examSession != null ? examSession.deadline : now);
        attempt.setStatus(status);
        attempt.setSubmissionReason(explicitReason);
        attempt.setViolationCount(examSession != null ? examSession.violationCount : 0);
        attempt.setRiskScore(riskScore);
        attempt.setRiskLevel(riskLevel);
        attempt.setFocusLostCount(focusLostCount);
        attempt.setFullscreenExitCount(fullscreenExitCount);
        attempt.setProctoringStatus(proctoringStatus);

        firebaseService.saveTestAttempt(user.getUid(), attempt);

        // Update student metrics in Firebase
        try {
            Map<String, Object> metricUpdates = new HashMap<>();
            StudentMetrics metrics = firebaseService.fetchStudentMetrics(user.getUid());
            int currentTests = (metrics != null) ? metrics.getTestsCompleted() : 0;
            metricUpdates.put("testsCompleted", currentTests + 1);
            if (category.toLowerCase().contains("quant") || category.toLowerCase().contains("aptitude")) {
                int cur = (metrics != null) ? metrics.getAptitudeSkill() : 0;
                metricUpdates.put("aptitudeSkill", Math.max(cur, scorePercent));
            } else if (category.toLowerCase().contains("tech") || category.toLowerCase().contains("code")) {
                int cur = (metrics != null) ? metrics.getJavaSkill() : 0;
                metricUpdates.put("javaSkill", Math.max(cur, scorePercent));
            }
            firebaseService.updateStudentMetrics(user.getUid(), metricUpdates);
        } catch (Exception ignored) {}

        // Clear active session
        ACTIVE_SESSIONS.remove(user.getUid());

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("attemptId", attemptId);
        result.addProperty("score", scorePercent);
        result.addProperty("correctCount", correctCount);
        result.addProperty("incorrectCount", incorrectCount);
        result.addProperty("unansweredCount", unansweredCount);
        result.addProperty("totalQuestions", totalQuestions);
        result.addProperty("passed", passed);
        result.addProperty("riskScore", riskScore);
        result.addProperty("riskLevel", riskLevel);
        result.addProperty("proctoringStatus", proctoringStatus);
        result.addProperty("examStatus", status);
        result.add("reviews", reviews);

        return ResponseEntity.ok(result.toString());
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
