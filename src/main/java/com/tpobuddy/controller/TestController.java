package com.tpobuddy.controller;

import com.google.gson.*;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.TestQuestionBank;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring MVC / REST Controller managing Tests, Assessments, Mock Examinations,
 * Proctoring Attempt Lifecycle, Three-Strike Security Violations, Answer scoring,
 * Anti-cheating Risk Scoring, and Firebase Attempt synchronization.
 */
@Controller
public class TestController {

    private final FirebaseService firebaseService;

    public TestController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public static class ActiveExamSession {
        public String attemptId;
        public String userId;
        public String studentName;
        public String category;
        public String topic;
        public long startTime;
        public int durationSeconds;
        public long deadline;
        public List<Integer> assignedQuestionIds = new ArrayList<>();
        public Map<Integer, Integer> savedAnswers = new HashMap<>();
        public List<JsonObject> monitoringEvents = new ArrayList<>();
        public int violationCount = 0;
        public String status = "in_progress";
        public String submissionReason = null;
        public long submissionTime = 0;
        public int currentQuestionIndex = 0;
        public boolean submitted = false;
        public int finalScore = 0;
        public int correctCount = 0;
        public int riskScore = 0;
        public String riskLevel = "LOW";
    }

    private static final Map<String, ActiveExamSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    @GetMapping({"/tests", "/tests.jsp"})
    public String viewTests(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp?error=unauthorized";
        }

        List<TestAttempt> attempts = firebaseService.fetchTestAttempts(user.getUid());
        model.addAttribute("user", user);
        model.addAttribute("attempts", attempts);
        return "tests";
    }

    @GetMapping(value = "/api/tests/questions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getQuestions(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "count", defaultValue = "25") int count) {

        List<Question> questions = TestQuestionBank.getQuestions(category, count);
        JsonArray arr = new JsonArray();
        for (Question q : questions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", q.getId());
            obj.addProperty("category", q.getCategory());
            obj.addProperty("topic", q.getTopic());
            obj.addProperty("difficulty", q.getDifficulty());
            obj.addProperty("question", q.getQuestion());
            obj.addProperty("optionA", q.getOptionA());
            obj.addProperty("optionB", q.getOptionB());
            obj.addProperty("optionC", q.getOptionC());
            obj.addProperty("optionD", q.getOptionD());
            arr.add(obj);
        }
        return arr.toString();
    }

    @GetMapping(value = "/api/tests/active-session", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getActiveSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("{\"success\":false,\"error\":\"Authentication required\"}");
        }

        long now = System.currentTimeMillis();
        ActiveExamSession active = null;

        for (ActiveExamSession s : ACTIVE_SESSIONS.values()) {
            if (s.userId.equals(user.getUid()) && !s.submitted && now < s.deadline) {
                active = s;
                break;
            }
        }

        if (active == null) {
            List<TestAttempt> userAttempts = firebaseService.fetchTestAttempts(user.getUid());
            if (userAttempts != null && !userAttempts.isEmpty()) {
                TestAttempt latest = userAttempts.get(0);
                if ("in_progress".equalsIgnoreCase(latest.getStatus()) && latest.getDeadline() > now) {
                    active = new ActiveExamSession();
                    active.attemptId = latest.getId();
                    active.userId = latest.getUserId();
                    active.studentName = latest.getStudentName();
                    active.category = latest.getCategory();
                    active.topic = latest.getTopic();
                    active.startTime = latest.getStartTime();
                    active.durationSeconds = latest.getDurationSeconds();
                    active.deadline = latest.getDeadline();
                    active.violationCount = latest.getViolationCount();
                    active.status = "in_progress";
                    active.currentQuestionIndex = latest.getCurrentQuestionIndex();

                    List<Question> qList = TestQuestionBank.getQuestions(latest.getCategory(), latest.getTotalQuestions());
                    for (Question q : qList) {
                        active.assignedQuestionIds.add(q.getId());
                    }

                    try {
                        if (latest.getSavedAnswersJson() != null && !latest.getSavedAnswersJson().isEmpty()) {
                            JsonObject obj = JsonParser.parseString(latest.getSavedAnswersJson()).getAsJsonObject();
                            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                                active.savedAnswers.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsInt());
                            }
                        }
                    } catch (Exception ignored) {}

                    ACTIVE_SESSIONS.put(active.attemptId, active);
                }
            }
        }

        JsonObject response = new JsonObject();
        if (active != null) {
            response.addProperty("success", true);
            response.addProperty("hasActiveSession", true);
            response.addProperty("attemptId", active.attemptId);
            response.addProperty("category", active.category);
            response.addProperty("durationSeconds", active.durationSeconds);
            int remaining = Math.max(0, (int) ((active.deadline - now) / 1000L));
            response.addProperty("remainingSeconds", remaining);
            response.addProperty("deadline", active.deadline);
            response.addProperty("violationCount", active.violationCount);
            response.addProperty("currentQuestionIndex", active.currentQuestionIndex);
            response.addProperty("status", active.status);

            JsonArray sanitizedQuestions = new JsonArray();
            for (int qId : active.assignedQuestionIds) {
                Question q = TestQuestionBank.getQuestionById(qId);
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
                    sanitizedQuestions.add(qObj);
                }
            }
            response.add("questions", sanitizedQuestions);

            JsonObject savedAnsJson = new JsonObject();
            for (Map.Entry<Integer, Integer> e : active.savedAnswers.entrySet()) {
                savedAnsJson.addProperty(String.valueOf(e.getKey()), e.getValue());
            }
            response.add("savedAnswers", savedAnsJson);
        } else {
            response.addProperty("success", true);
            response.addProperty("hasActiveSession", false);
        }

        return ResponseEntity.ok(response.toString());
    }

    @PostMapping(value = "/api/tests/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> startExam(HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("{\"success\":false,\"error\":\"Authentication required\"}");
        }

        long now = System.currentTimeMillis();

        for (ActiveExamSession s : ACTIVE_SESSIONS.values()) {
            if (s.userId.equals(user.getUid()) && !s.submitted && now < s.deadline) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("attemptId", s.attemptId);
                response.addProperty("resumed", true);
                response.addProperty("durationSeconds", s.durationSeconds);
                response.addProperty("remainingSeconds", Math.max(0, (int) ((s.deadline - now) / 1000L)));
                response.addProperty("deadline", s.deadline);
                response.addProperty("category", s.category);
                response.addProperty("violationCount", s.violationCount);
                response.addProperty("questionCount", s.assignedQuestionIds.size());

                JsonArray sanitizedQuestions = new JsonArray();
                for (int qId : s.assignedQuestionIds) {
                    Question q = TestQuestionBank.getQuestionById(qId);
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
                        sanitizedQuestions.add(qObj);
                    }
                }
                response.add("questions", sanitizedQuestions);

                JsonObject savedAnsJson = new JsonObject();
                for (Map.Entry<Integer, Integer> e : s.savedAnswers.entrySet()) {
                    savedAnsJson.addProperty(String.valueOf(e.getKey()), e.getValue());
                }
                response.add("savedAnswers", savedAnsJson);

                return ResponseEntity.ok(response.toString());
            }
        }

        JsonObject body = parseRequestBody(request);
        String category = (body != null && body.has("category")) ? body.get("category").getAsString() : "Technical";
        int count = 25;
        if (body != null && body.has("count")) {
            try { count = body.get("count").getAsInt(); } catch (Exception ignored) {}
        }

        int durationSeconds = Math.min(1500, Math.max(300, count * 60));
        long deadline = now + (durationSeconds * 1000L);
        String attemptId = UUID.randomUUID().toString();

        List<Question> questions = TestQuestionBank.getQuestions(category, count);
        ActiveExamSession examSession = new ActiveExamSession();
        examSession.attemptId = attemptId;
        examSession.userId = user.getUid();
        examSession.studentName = user.getName();
        examSession.category = category;
        examSession.topic = "Comprehensive Assessment";
        examSession.startTime = now;
        examSession.durationSeconds = durationSeconds;
        examSession.deadline = deadline;
        examSession.violationCount = 0;
        examSession.status = "in_progress";
        examSession.currentQuestionIndex = 0;

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
            sanitizedQuestions.add(qObj);
        }

        ACTIVE_SESSIONS.put(attemptId, examSession);

        TestAttempt initialAttempt = new TestAttempt();
        initialAttempt.setId(attemptId);
        initialAttempt.setTestId(category);
        initialAttempt.setUserId(user.getUid());
        initialAttempt.setStudentName(user.getName());
        initialAttempt.setCategory(category);
        initialAttempt.setTopic("Comprehensive Assessment");
        initialAttempt.setStartTime(now);
        initialAttempt.setSavedAt(now);
        initialAttempt.setDurationSeconds(durationSeconds);
        initialAttempt.setDeadline(deadline);
        initialAttempt.setStatus("in_progress");
        initialAttempt.setSubmissionReason("in_progress");
        initialAttempt.setViolationCount(0);
        initialAttempt.setTotalQuestions(sanitizedQuestions.size());
        firebaseService.saveTestAttempt(user.getUid(), initialAttempt);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("attemptId", attemptId);
        response.addProperty("durationSeconds", durationSeconds);
        response.addProperty("remainingSeconds", durationSeconds);
        response.addProperty("deadline", deadline);
        response.addProperty("category", category);
        response.addProperty("violationCount", 0);
        response.addProperty("questionCount", sanitizedQuestions.size());
        response.add("questions", sanitizedQuestions);

        return ResponseEntity.ok(response.toString());
    }

    @PostMapping(value = "/api/tests/log-event", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> logEvent(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }

        JsonObject body = parseRequestBody(request);
        if (body == null || !body.has("attemptId")) {
            return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Missing attemptId\"}");
        }

        String attemptId = body.get("attemptId").getAsString();
        ActiveExamSession examSession = ACTIVE_SESSIONS.get(attemptId);
        if (examSession == null || !examSession.userId.equals(user.getUid())) {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN)
                    .body("{\"success\":false,\"error\":\"Exam attempt not found or unauthorized\"}");
        }

        String type = body.has("type") ? body.get("type").getAsString() : "UNKNOWN";
        String details = body.has("details") ? body.get("details").getAsString() : "";
        long now = System.currentTimeMillis();

        JsonObject event = new JsonObject();
        event.addProperty("type", type);
        event.addProperty("details", details);
        event.addProperty("timestamp", now);
        examSession.monitoringEvents.add(event);

        boolean isSecurityViolation = "FULLSCREEN_EXIT".equalsIgnoreCase(type)
                || "FOCUS_LOST".equalsIgnoreCase(type)
                || "TAB_SWITCH".equalsIgnoreCase(type)
                || "WINDOW_BLUR".equalsIgnoreCase(type);

        if (isSecurityViolation && !examSession.submitted) {
            if (body.has("violationCount")) {
                int clientCount = body.get("violationCount").getAsInt();
                examSession.violationCount = Math.max(examSession.violationCount + 1, clientCount);
            } else {
                examSession.violationCount++;
            }

            // Three-Strike rule: auto-submits on the 3rd violation
            if (examSession.violationCount >= 3) {
                examSession.submitted = true;
                examSession.status = "auto_submitted_security";
                examSession.submissionReason = "automatic-security-submission";
                examSession.submissionTime = now;

                int totalQuestions = !examSession.assignedQuestionIds.isEmpty() ? examSession.assignedQuestionIds.size() : 25;
                int correctCount = 0;
                JsonArray reviews = new JsonArray();

                for (int qId : examSession.assignedQuestionIds) {
                    Question q = TestQuestionBank.getQuestionById(qId);
                    if (q != null) {
                        Integer userAns = examSession.savedAnswers.get(qId);
                        boolean isCorrect = (userAns != null && userAns == q.getCorrectOption());
                        if (isCorrect) correctCount++;

                        JsonObject r = new JsonObject();
                        r.addProperty("questionId", qId);
                        r.addProperty("question", q.getQuestion());
                        r.addProperty("selectedOption", (userAns != null) ? userAns : -1);
                        r.addProperty("correctOption", q.getCorrectOption());
                        r.addProperty("isCorrect", isCorrect);
                        r.addProperty("explanation", q.getExplanation());
                        reviews.add(r);
                    }
                }

                int scorePercent = Math.round((correctCount * 100.0f) / totalQuestions);
                boolean passed = scorePercent >= 60;
                examSession.finalScore = scorePercent;
                examSession.correctCount = correctCount;
                examSession.riskScore = 100;
                examSession.riskLevel = "HIGH";

                TestAttempt attempt = new TestAttempt();
                attempt.setId(examSession.attemptId);
                attempt.setTestId(examSession.category);
                attempt.setUserId(user.getUid());
                attempt.setStudentName(user.getName());
                attempt.setCategory(examSession.category);
                attempt.setTopic(examSession.topic);
                attempt.setScore(scorePercent);
                attempt.setTotalQuestions(totalQuestions);
                attempt.setCorrectAnswers(correctCount);
                attempt.setSavedAt(now);
                attempt.setPassed(passed);
                attempt.setStartTime(examSession.startTime);
                attempt.setDurationSeconds(examSession.durationSeconds);
                attempt.setDeadline(examSession.deadline);
                attempt.setSubmissionTime(now);
                attempt.setStatus("auto_submitted_security");
                attempt.setSubmissionReason("automatic-security-submission");
                attempt.setViolationCount(examSession.violationCount);
                attempt.setRiskScore(100);
                attempt.setRiskLevel("HIGH");
                attempt.setMonitoringEventsJson(new Gson().toJson(examSession.monitoringEvents));
                attempt.setSavedAnswersJson(new Gson().toJson(examSession.savedAnswers));
                attempt.setProctoringStatus("FLAGGED");
                attempt.setReviewNotes("Automatically submitted after reaching 3 security violations limit.");

                firebaseService.saveTestAttempt(user.getUid(), attempt);

                JsonObject autoRes = new JsonObject();
                autoRes.addProperty("success", true);
                autoRes.addProperty("recorded", true);
                autoRes.addProperty("violationCount", examSession.violationCount);
                autoRes.addProperty("autoSubmitted", true);
                autoRes.addProperty("submissionReason", "automatic-security-submission");
                autoRes.addProperty("score", scorePercent);
                autoRes.addProperty("correctCount", correctCount);
                autoRes.addProperty("totalQuestions", totalQuestions);
                autoRes.addProperty("passed", passed);
                autoRes.addProperty("riskScore", 100);
                autoRes.addProperty("riskLevel", "HIGH");
                autoRes.add("reviews", reviews);

                return ResponseEntity.ok(autoRes.toString());
            } else {
                TestAttempt progressAttempt = new TestAttempt();
                progressAttempt.setId(examSession.attemptId);
                progressAttempt.setTestId(examSession.category);
                progressAttempt.setUserId(user.getUid());
                progressAttempt.setStudentName(user.getName());
                progressAttempt.setCategory(examSession.category);
                progressAttempt.setTopic(examSession.topic);
                progressAttempt.setStartTime(examSession.startTime);
                progressAttempt.setSavedAt(now);
                progressAttempt.setDurationSeconds(examSession.durationSeconds);
                progressAttempt.setDeadline(examSession.deadline);
                progressAttempt.setStatus("in_progress");
                progressAttempt.setSubmissionReason("in_progress");
                progressAttempt.setViolationCount(examSession.violationCount);
                progressAttempt.setTotalQuestions(examSession.assignedQuestionIds.size());
                progressAttempt.setMonitoringEventsJson(new Gson().toJson(examSession.monitoringEvents));
                progressAttempt.setSavedAnswersJson(new Gson().toJson(examSession.savedAnswers));
                firebaseService.saveTestAttempt(user.getUid(), progressAttempt);
            }
        }

        JsonObject res = new JsonObject();
        res.addProperty("success", true);
        res.addProperty("recorded", true);
        res.addProperty("violationCount", examSession.violationCount);
        res.addProperty("autoSubmitted", false);
        return ResponseEntity.ok(res.toString());
    }

    @PostMapping(value = "/api/tests/save-progress", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> saveProgress(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }

        JsonObject body = parseRequestBody(request);
        if (body == null || !body.has("attemptId")) {
            return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Invalid request\"}");
        }

        String attemptId = body.get("attemptId").getAsString();
        ActiveExamSession examSession = ACTIVE_SESSIONS.get(attemptId);
        if (examSession != null && examSession.userId.equals(user.getUid()) && !examSession.submitted) {
            if (body.has("answers") && body.get("answers").isJsonObject()) {
                JsonObject answers = body.getAsJsonObject("answers");
                for (Map.Entry<String, JsonElement> entry : answers.entrySet()) {
                    try {
                        int qId = Integer.parseInt(entry.getKey());
                        int ans = entry.getValue().getAsInt();
                        examSession.savedAnswers.put(qId, ans);
                    } catch (Exception ignored) {}
                }
            }
            if (body.has("currentQuestionIndex")) {
                try {
                    examSession.currentQuestionIndex = body.get("currentQuestionIndex").getAsInt();
                } catch (Exception ignored) {}
            }
        }

        JsonObject res = new JsonObject();
        res.addProperty("success", true);
        res.addProperty("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(res.toString());
    }

    @PostMapping(value = "/api/tests/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> submitTest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }

        JsonObject submission = parseRequestBody(request);
        if (submission == null) {
            return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Invalid payload\"}");
        }

        String attemptId = submission.has("attemptId") ? submission.get("attemptId").getAsString() : null;
        ActiveExamSession examSession = (attemptId != null) ? ACTIVE_SESSIONS.get(attemptId) : null;

        if (examSession != null && !examSession.userId.equals(user.getUid())) {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN)
                    .body("{\"success\":false,\"error\":\"Exam attempt ownership mismatch\"}");
        }

        if (examSession != null && examSession.submitted) {
            JsonObject existingResult = new JsonObject();
            existingResult.addProperty("success", true);
            existingResult.addProperty("score", examSession.finalScore);
            existingResult.addProperty("correctCount", examSession.correctCount);
            existingResult.addProperty("totalQuestions", examSession.assignedQuestionIds.size());
            existingResult.addProperty("passed", examSession.finalScore >= 60);
            existingResult.addProperty("riskScore", examSession.riskScore);
            existingResult.addProperty("riskLevel", examSession.riskLevel);
            existingResult.addProperty("status", examSession.status);
            existingResult.addProperty("submissionReason", examSession.submissionReason);
            existingResult.addProperty("alreadySubmitted", true);
            return ResponseEntity.ok(existingResult.toString());
        }

        String category = (examSession != null) ? examSession.category :
                (submission.has("category") ? submission.get("category").getAsString() : "Technical");
        String topic = (examSession != null) ? examSession.topic : "Assessment";

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

        List<JsonObject> allEvents = new ArrayList<>();
        if (examSession != null) {
            allEvents.addAll(examSession.monitoringEvents);
        }
        if (submission.has("monitoringEvents") && submission.get("monitoringEvents").isJsonArray()) {
            for (JsonElement el : submission.getAsJsonArray("monitoringEvents")) {
                if (el.isJsonObject()) {
                    allEvents.add(el.getAsJsonObject());
                }
            }
        }

        long now = System.currentTimeMillis();
        String explicitReason = submission.has("submissionReason") ? submission.get("submissionReason").getAsString() : null;
        String status = "submitted";
        String submissionReason = "manual";

        if ("automatic-security-submission".equalsIgnoreCase(explicitReason) || (examSession != null && examSession.violationCount >= 3)) {
            status = "auto_submitted_security";
            submissionReason = "automatic-security-submission";
        } else if ("timeout".equalsIgnoreCase(explicitReason) || (examSession != null && examSession.deadline > 0 && now > examSession.deadline + 15000)) {
            status = "auto_submitted_timeout";
            submissionReason = "timeout";
        }

        int focusLostCount = 0;
        int fullscreenExitCount = 0;
        int cameraDisconnectCount = 0;
        int copyPasteCount = 0;

        for (JsonObject evt : allEvents) {
            String type = evt.has("type") ? evt.get("type").getAsString() : "";
            if ("FOCUS_LOST".equalsIgnoreCase(type) || "TAB_SWITCH".equalsIgnoreCase(type)) {
                focusLostCount++;
            } else if ("FULLSCREEN_EXIT".equalsIgnoreCase(type)) {
                fullscreenExitCount++;
            } else if ("CAMERA_DISCONNECTED".equalsIgnoreCase(type) || "CAMERA_ERROR".equalsIgnoreCase(type)) {
                cameraDisconnectCount++;
            } else if ("CLIPBOARD_ATTEMPT".equalsIgnoreCase(type) || "COPY_PASTE".equalsIgnoreCase(type)) {
                copyPasteCount++;
            }
        }

        int riskScore = 0;
        riskScore += Math.min(25, focusLostCount * 5);
        riskScore += Math.min(30, fullscreenExitCount * 15);
        riskScore += Math.min(40, cameraDisconnectCount * 25);
        riskScore += Math.min(20, copyPasteCount * 10);
        if ("auto_submitted_security".equals(status)) {
            riskScore = 100;
        }
        riskScore = Math.min(100, riskScore);

        String riskLevel = (riskScore <= 15) ? "LOW" : (riskScore <= 40 ? "MODERATE" : "HIGH");
        String proctoringStatus = (riskScore > 40) ? "FLAGGED" : "VALIDATED";

        int totalQuestions = (examSession != null && !examSession.assignedQuestionIds.isEmpty())
                ? examSession.assignedQuestionIds.size() : Math.max(1, finalAnswers.size());
        int correctCount = 0;
        JsonArray reviews = new JsonArray();

        List<Integer> questionIdsToScore = (examSession != null && !examSession.assignedQuestionIds.isEmpty())
                ? examSession.assignedQuestionIds
                : new ArrayList<>(finalAnswers.keySet());

        for (int qId : questionIdsToScore) {
            Question q = TestQuestionBank.getQuestionById(qId);
            if (q != null) {
                Integer userAns = finalAnswers.get(qId);
                boolean isCorrect = (userAns != null && userAns == q.getCorrectOption());
                if (isCorrect) correctCount++;

                JsonObject r = new JsonObject();
                r.addProperty("questionId", qId);
                r.addProperty("question", q.getQuestion());
                r.addProperty("selectedOption", (userAns != null) ? userAns : -1);
                r.addProperty("correctOption", q.getCorrectOption());
                r.addProperty("isCorrect", isCorrect);
                r.addProperty("explanation", q.getExplanation());
                reviews.add(r);
            }
        }

        int scorePercent = Math.round((correctCount * 100.0f) / totalQuestions);
        boolean passed = scorePercent >= 60;

        if (examSession != null) {
            examSession.submitted = true;
            examSession.finalScore = scorePercent;
            examSession.correctCount = correctCount;
            examSession.riskScore = riskScore;
            examSession.riskLevel = riskLevel;
            examSession.status = status;
            examSession.submissionReason = submissionReason;
            examSession.submissionTime = now;
        }

        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId != null ? attemptId : UUID.randomUUID().toString());
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
        attempt.setDurationSeconds(examSession != null ? examSession.durationSeconds : 1500);
        attempt.setDeadline(examSession != null ? examSession.deadline : now);
        attempt.setStatus(status);
        attempt.setSubmissionReason(submissionReason);
        attempt.setViolationCount(examSession != null ? examSession.violationCount : 0);
        attempt.setRiskScore(riskScore);
        attempt.setRiskLevel(riskLevel);
        attempt.setFocusLostCount(focusLostCount);
        attempt.setFullscreenExitCount(fullscreenExitCount);
        attempt.setCameraDisconnectCount(cameraDisconnectCount);
        attempt.setCopyPasteCount(copyPasteCount);
        attempt.setMonitoringEventsJson(new Gson().toJson(allEvents));
        attempt.setSavedAnswersJson(new Gson().toJson(finalAnswers));
        attempt.setProctoringStatus(proctoringStatus);

        firebaseService.saveTestAttempt(user.getUid(), attempt);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("score", scorePercent);
        result.addProperty("correctCount", correctCount);
        result.addProperty("totalQuestions", totalQuestions);
        result.addProperty("passed", passed);
        result.addProperty("riskScore", riskScore);
        result.addProperty("riskLevel", riskLevel);
        result.addProperty("proctoringStatus", proctoringStatus);
        result.addProperty("examStatus", status);
        result.add("reviews", reviews);

        return ResponseEntity.ok(result.toString());
    }

    private JsonObject parseRequestBody(HttpServletRequest req) {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            if (sb.length() == 0) return null;
            return JsonParser.parseString(sb.toString()).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
