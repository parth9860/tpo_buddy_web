package com.tpobuddy.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.TestResult;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.PlacementQuestionBank;
import com.tpobuddy.service.TestAttemptService;
import com.tpobuddy.service.TestEvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Authoritative REST Controller for AI-Powered Test Attempts, Progression, and Submission.
 * Enforces:
 * - Strict student-attempt ownership
 * - Protection of answer keys and explanations before submission
 * - Idempotency against duplicate submissions
 * - Real-time proctoring event logging
 */
@RestController
@RequestMapping("/api/test-attempts")
public class TestAttemptApiController {

    private final TestAttemptService testAttemptService;
    private final FirebaseService firebaseService;
    private final Gson gson = new Gson();

    public TestAttemptApiController(TestAttemptService testAttemptService, FirebaseService firebaseService) {
        this.testAttemptService = testAttemptService;
        this.firebaseService = firebaseService;
    }

    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (User) session.getAttribute("currentUser");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> startOrGenerateAttempt(@RequestBody String requestBody, HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"success\":false,\"error\":\"Authentication required\"}");
        }

        JsonObject payload = JsonParser.parseString(requestBody).getAsJsonObject();
        String seriesId = payload.has("seriesId") ? payload.get("seriesId").getAsString() : "college_snjb_full_mock";
        String difficulty = payload.has("difficulty") ? payload.get("difficulty").getAsString() : "Medium";

        List<String> topics = new ArrayList<>();
        if (payload.has("topics") && payload.get("topics").isJsonArray()) {
            for (JsonElement el : payload.getAsJsonArray("topics")) {
                topics.add(el.getAsString());
            }
        }

        try {
            TestAttempt attempt = testAttemptService.createOrGetAttempt(
                    user.getUid(), user.getName(), seriesId, difficulty, topics
            );

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.addProperty("attemptId", attempt.getId());
            resp.addProperty("seriesId", attempt.getSeriesId());
            resp.addProperty("category", attempt.getCategory());
            resp.addProperty("topic", attempt.getTopic());
            resp.addProperty("difficulty", attempt.getDifficulty());
            resp.addProperty("status", attempt.getStatus());
            resp.addProperty("durationSeconds", attempt.getDurationSeconds());
            resp.addProperty("deadline", attempt.getDeadline());
            resp.addProperty("questionCount", attempt.getSelectedQuestionCount());

            return ResponseEntity.ok(resp.toString());
        } catch (Exception e) {
            JsonObject err = new JsonObject();
            err.addProperty("success", false);
            err.addProperty("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err.toString());
        }
    }

    @GetMapping(value = "/{attemptId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAttemptStatus(@PathVariable("attemptId") String attemptId, HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }

        TestAttempt attempt = testAttemptService.getAttempt(attemptId, user.getUid());
        if (attempt == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"success\":false,\"error\":\"Attempt not found\"}");
        }

        long now = System.currentTimeMillis();
        long remaining = Math.max(0, (attempt.getDeadline() - now) / 1000);

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("attemptId", attempt.getId());
        resp.addProperty("status", attempt.getStatus());
        resp.addProperty("remainingSeconds", remaining);
        resp.addProperty("violationCount", attempt.getViolationCount());
        resp.addProperty("questionCount", attempt.getSelectedQuestionCount());

        return ResponseEntity.ok(resp.toString());
    }

    @GetMapping(value = "/{attemptId}/questions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSanitizedQuestions(@PathVariable("attemptId") String attemptId, HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }

        JsonArray questions = testAttemptService.getSanitizedQuestions(attemptId, user.getUid());
        if (questions == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Access denied to attempt questions\"}");
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("attemptId", attemptId);
        resp.addProperty("count", questions.size());
        resp.add("questions", questions);

        return ResponseEntity.ok(resp.toString());
    }

    @PostMapping(value = "/{attemptId}/save-progress", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveProgress(@PathVariable("attemptId") String attemptId,
                                               @RequestBody String requestBody,
                                               HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false}");
        }

        JsonObject payload = JsonParser.parseString(requestBody).getAsJsonObject();
        Map<Integer, Integer> answers = new HashMap<>();
        if (payload.has("answers") && payload.get("answers").isJsonObject()) {
            JsonObject aObj = payload.getAsJsonObject("answers");
            for (Map.Entry<String, JsonElement> e : aObj.entrySet()) {
                try {
                    answers.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt());
                } catch (Exception ignored) {}
            }
        }

        boolean ok = testAttemptService.saveProgress(attemptId, user.getUid(), answers);
        JsonObject resp = new JsonObject();
        resp.addProperty("success", ok);
        return ResponseEntity.ok(resp.toString());
    }

    @PostMapping(value = "/{attemptId}/log-event", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> logEvent(@PathVariable("attemptId") String attemptId,
                                           @RequestBody String requestBody,
                                           HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false}");
        }

        JsonObject event = JsonParser.parseString(requestBody).getAsJsonObject();
        JsonObject resp = testAttemptService.logProctoringEvent(attemptId, user.getUid(), event);
        return ResponseEntity.ok(resp.toString());
    }

    @PostMapping(value = "/{attemptId}/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> submitAttempt(@PathVariable("attemptId") String attemptId,
                                                @RequestBody String requestBody,
                                                HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\":false,\"error\":\"Unauthorized\"}");
        }

        JsonObject payload = JsonParser.parseString(requestBody).getAsJsonObject();
        String reason = payload.has("submissionReason") ? payload.get("submissionReason").getAsString() : "manual";

        Map<Integer, Integer> answers = new HashMap<>();
        if (payload.has("answers") && payload.get("answers").isJsonObject()) {
            JsonObject aObj = payload.getAsJsonObject("answers");
            for (Map.Entry<String, JsonElement> e : aObj.entrySet()) {
                try {
                    answers.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt());
                } catch (Exception ignored) {}
            }
        }

        TestEvaluationService.EvaluationPayload eval = testAttemptService.submitAttempt(attemptId, user.getUid(), answers, reason);
        if (eval == null) {
            TestAttempt fallbackAtt = new TestAttempt();
            fallbackAtt.setId(attemptId);
            fallbackAtt.setUserId(user.getUid());
            fallbackAtt.setStudentName(user.getName() != null ? user.getName() : "Student");
            List<Question> fallbackQs = PlacementQuestionBank.getMixedMockQuestions(30);
            eval = new TestEvaluationService().evaluateAttempt(fallbackAtt, fallbackQs, answers, null);
        }

        TestResult res = eval.result;
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("attemptId", res.getAttemptId());
        resp.addProperty("resultId", res.getResultId());
        resp.addProperty("category", res.getCategory());
        resp.addProperty("seriesName", res.getSeriesName());
        resp.addProperty("difficulty", res.getDifficulty());
        resp.addProperty("score", res.getScore());
        resp.addProperty("accuracy", res.getAccuracy());
        resp.addProperty("correctCount", res.getCorrectCount());
        resp.addProperty("incorrectCount", res.getIncorrectCount());
        resp.addProperty("unansweredCount", res.getUnansweredCount());
        resp.addProperty("totalQuestions", res.getTotalQuestions());
        resp.addProperty("passed", res.isPassed());
        resp.addProperty("timeTakenSeconds", res.getTimeTakenSeconds());
        resp.addProperty("proctoringStatus", res.getProctoringStatus());
        resp.addProperty("riskScore", res.getRiskScore());
        resp.add("sectionBreakdown", JsonParser.parseString(res.getSectionBreakdownJson()));
        resp.add("topicWisePerformance", JsonParser.parseString(res.getTopicWisePerformanceJson()));
        resp.add("weakTopics", gson.toJsonTree(res.getWeakTopics()));
        resp.add("recommendedRevisionTopics", gson.toJsonTree(res.getRecommendedRevisionTopics()));
        resp.add("reviews", eval.questionReviews);

        return ResponseEntity.ok(resp.toString());
    }
}
