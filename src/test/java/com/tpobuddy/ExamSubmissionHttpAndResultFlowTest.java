package com.tpobuddy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tpobuddy.controller.CustomErrorController;
import com.tpobuddy.controller.TestAttemptApiController;
import com.tpobuddy.controller.TestController;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.User;
import com.tpobuddy.service.*;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end Controller & Flow Test Suite verifying:
 * 1. Successful manual submission via TestAttemptApiController.
 * 2. Successful automatic submission on 3 proctoring violations.
 * 3. Correct score calculation and question evaluation.
 * 4. Idempotency on repeated submit requests.
 * 5. Unauthorized submission rejection (HTTP 403).
 * 6. Unauthenticated submission rejection (HTTP 401).
 * 7. Empty answers and unanswered questions scoring.
 * 8. Result page navigation and model generation (TestController.viewTestResult).
 * 9. CustomErrorController JSON and JSP fallback rendering.
 * 10. Prevention of Script injection and NPEs in weak topic parsing.
 */
public class ExamSubmissionHttpAndResultFlowTest {

    private TestAttemptService attemptService;
    private TestSeriesService seriesService;
    private TestEvaluationService evaluationService;
    private QuestionSelectionService selectionService;
    private QuestionValidationService validationService;
    private AIGenerationService aiGenerationService;
    private FirebaseService firebaseService;

    private TestAttemptApiController apiController;
    private TestController testController;
    private CustomErrorController errorController;

    private User student1;
    private User student2;

    @BeforeEach
    public void setUp() {
        seriesService = new TestSeriesService();
        evaluationService = new TestEvaluationService();
        selectionService = new QuestionSelectionService();
        validationService = new QuestionValidationService();
        aiGenerationService = new AIGenerationService(validationService) {
            @Override
            public List<Question> generateCandidatePool(com.tpobuddy.model.TestSeries series, String difficulty, List<String> customTopics, int targetPoolSize) {
                List<Question> pool = new ArrayList<>();
                for (int i = 1; i <= 30; i++) {
                    Question q = new Question();
                    q.setId(i + 2000);
                    q.setCategory("Quantitative Aptitude");
                    q.setSection("Quantitative Aptitude");
                    q.setTopic("Number System");
                    q.setDifficulty("Medium");
                    q.setQuestion("Quantitative Question #" + i);
                    q.setOptionA("Opt A");
                    q.setOptionB("Opt B");
                    q.setOptionC("Opt C");
                    q.setOptionD("Opt D");
                    q.setCorrectOption(1); // Option B is correct
                    q.setExplanation("Step-by-step solution for #" + i);
                    pool.add(q);
                }
                return pool;
            }
        };
        firebaseService = new FirebaseService();

        attemptService = new TestAttemptService(
                aiGenerationService,
                selectionService,
                evaluationService,
                seriesService,
                firebaseService
        );

        apiController = new TestAttemptApiController(attemptService, firebaseService);
        PlacementTestService placementTestService = new PlacementTestService(firebaseService);
        testController = new TestController(firebaseService, placementTestService, seriesService, attemptService);
        errorController = new CustomErrorController();

        student1 = new User("user_student_1", 101, "Pooja Patil", "student1@snjb.org", "student", "CSE", "BE", "SNJB", System.currentTimeMillis());
        student2 = new User("user_student_2", 102, "Rohit Sharma", "student2@snjb.org", "student", "IT", "BE", "SNJB", System.currentTimeMillis());
    }

    private MockHttpServletRequest createAuthRequest(User user) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user);
        request.setSession(session);
        return request;
    }

    // 1. Successful manual submission via API
    @Test
    @DisplayName("1. Manual submission evaluates answers, grades authoritative score, and returns redirectUrl")
    public void testSuccessfulManualSubmission() {
        TestAttempt attempt = attemptService.createOrGetAttempt(
                student1.getUid(), student1.getName(), "college_snjb_quant", "Medium", Collections.singletonList("Number System")
        );
        assertNotNull(attempt);

        List<Question> qs = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(qs);
        assertFalse(qs.isEmpty());

        // Answer 15 correct (out of 30)
        JsonObject answersObj = new JsonObject();
        for (int i = 0; i < 15; i++) {
            Question q = qs.get(i);
            answersObj.addProperty(String.valueOf(q.getId()), q.getCorrectOption());
        }

        JsonObject payload = new JsonObject();
        payload.add("answers", answersObj);
        payload.addProperty("submissionReason", "student_submitted");

        MockHttpServletRequest request = createAuthRequest(student1);
        ResponseEntity<String> response = apiController.submitAttempt(attempt.getId(), payload.toString(), request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonObject respJson = JsonParser.parseString(response.getBody()).getAsJsonObject();
        assertTrue(respJson.get("success").getAsBoolean());
        assertEquals(attempt.getId(), respJson.get("attemptId").getAsString());
        assertEquals(15, respJson.get("correctCount").getAsInt());
        assertEquals(15, respJson.get("unansweredCount").getAsInt());
        assertEquals(50, respJson.get("score").getAsInt()); // 15/30 = 50%
        assertTrue(respJson.has("redirectUrl"));
        assertTrue(respJson.get("redirectUrl").getAsString().contains(attempt.getId()));
    }

    // 2. Successful automatic submission on 3 proctoring violations
    @Test
    @DisplayName("2. Proctoring 3-strike violations terminate exam and submit with risk score 100")
    public void testAutomaticSubmissionOnThreeViolations() {
        TestAttempt attempt = attemptService.createOrGetAttempt(
                student1.getUid(), student1.getName(), "college_snjb_quant", "Medium", null
        );

        // Record 3 violations
        for (int i = 1; i <= 3; i++) {
            JsonObject event = new JsonObject();
            event.addProperty("type", "TAB_SWITCH");
            attemptService.logProctoringEvent(attempt.getId(), student1.getUid(), event);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("submissionReason", "proctor_violation");
        payload.add("answers", new JsonObject());

        MockHttpServletRequest request = createAuthRequest(student1);
        ResponseEntity<String> response = apiController.submitAttempt(attempt.getId(), payload.toString(), request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonObject respJson = JsonParser.parseString(response.getBody()).getAsJsonObject();
        assertTrue(respJson.get("success").getAsBoolean());
        assertEquals("terminated", attempt.getStatus());
        assertEquals(100, attempt.getRiskScore());
        assertEquals("HIGH", attempt.getRiskLevel());
    }

    // 3. Repeated / Duplicate Submissions (Idempotency)
    @Test
    @DisplayName("3. Duplicate clicks and repeated submissions return identical cached result without duplicate records")
    public void testIdempotentRepeatedSubmissions() {
        TestAttempt attempt = attemptService.createOrGetAttempt(
                student1.getUid(), student1.getName(), "college_snjb_quant", "Medium", null
        );

        JsonObject payload = new JsonObject();
        payload.addProperty("submissionReason", "manual");
        payload.add("answers", new JsonObject());

        MockHttpServletRequest request = createAuthRequest(student1);

        // Submit 1
        ResponseEntity<String> res1 = apiController.submitAttempt(attempt.getId(), payload.toString(), request);
        assertEquals(HttpStatus.OK, res1.getStatusCode());
        JsonObject json1 = JsonParser.parseString(res1.getBody()).getAsJsonObject();

        // Submit 2 (Immediate double click)
        ResponseEntity<String> res2 = apiController.submitAttempt(attempt.getId(), payload.toString(), request);
        assertEquals(HttpStatus.OK, res2.getStatusCode());
        JsonObject json2 = JsonParser.parseString(res2.getBody()).getAsJsonObject();

        assertEquals(json1.get("resultId").getAsString(), json2.get("resultId").getAsString());
        assertEquals(json1.get("score").getAsInt(), json2.get("score").getAsInt());
    }

    // 4. Unauthorized student submitting another student's attempt
    @Test
    @DisplayName("4. Attempt ownership verification: student2 cannot submit student1's attempt")
    public void testUnauthorizedSubmissionRejection() {
        TestAttempt attempt = attemptService.createOrGetAttempt(
                student1.getUid(), student1.getName(), "college_snjb_quant", "Medium", null
        );

        JsonObject payload = new JsonObject();
        payload.addProperty("submissionReason", "manual");
        payload.add("answers", new JsonObject());

        // student2 attempts to submit student1's exam
        MockHttpServletRequest request = createAuthRequest(student2);
        ResponseEntity<String> response = apiController.submitAttempt(attempt.getId(), payload.toString(), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        JsonObject respJson = JsonParser.parseString(response.getBody()).getAsJsonObject();
        assertFalse(respJson.get("success").getAsBoolean());
    }

    // 5. Unauthenticated request rejection
    @Test
    @DisplayName("5. Unauthenticated submit request returns HTTP 401 Unauthorized")
    public void testUnauthenticatedSubmissionRejection() {
        MockHttpServletRequest unauthRequest = new MockHttpServletRequest(); // No session
        ResponseEntity<String> response = apiController.submitAttempt("att_fake_id", "{}", unauthRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // 6. View Test Result Page rendering
    @Test
    @DisplayName("6. Result page view resolves 'test-result' with populated model attributes")
    public void testViewTestResultPageFlow() {
        TestAttempt attempt = attemptService.createOrGetAttempt(
                student1.getUid(), student1.getName(), "college_snjb_quant", "Medium", null
        );

        List<Question> qs = attemptService.getAttemptQuestions(attempt.getId());
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(qs.get(0).getId(), qs.get(0).getCorrectOption());

        // Submit
        attemptService.submitAttempt(attempt.getId(), student1.getUid(), answers, "manual");

        // View result
        MockHttpServletRequest request = createAuthRequest(student1);
        Model model = new ExtendedModelMap();

        String viewName = testController.viewTestResult(null, attempt.getId(), request, model);
        assertEquals("test-result", viewName);

        assertNotNull(model.getAttribute("user"));
        assertNotNull(model.getAttribute("attempt"));
        assertNotNull(model.getAttribute("sectionBreakdown"));
        assertNotNull(model.getAttribute("weakTopics"));
        assertNotNull(model.getAttribute("reviewsJson"));
        assertNotNull(model.getAttribute("recommendedSeriesId"));
        assertNotNull(model.getAttribute("recommendedTestTitle"));
    }

    // 7. Result page unauthorized access
    @Test
    @DisplayName("7. Unauthorized student viewing another student's result redirects to unauthorized")
    public void testViewTestResultUnauthorizedAccess() {
        TestAttempt attempt = attemptService.createOrGetAttempt(
                student1.getUid(), student1.getName(), "college_snjb_quant", "Medium", null
        );
        attemptService.submitAttempt(attempt.getId(), student1.getUid(), Collections.emptyMap(), "manual");

        MockHttpServletRequest request = createAuthRequest(student2); // Student 2 viewing Student 1
        Model model = new ExtendedModelMap();

        String viewName = testController.viewTestResult(null, attempt.getId(), request, model);
        assertTrue(viewName.startsWith("redirect:/tests?error=unauthorized_attempt"));
    }

    // 8. CustomErrorController API JSON response
    @Test
    @DisplayName("8. CustomErrorController returns clean JSON for /api/ requests")
    public void testCustomErrorControllerForApi() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/test-attempts/att_123/submit");
        req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        req.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Evaluator failure");

        Model model = new ExtendedModelMap();
        Object result = errorController.handleError(req, model);

        assertTrue(result instanceof ResponseEntity<?>);
        ResponseEntity<?> entity = (ResponseEntity<?>) result;
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
        assertTrue(entity.getBody().toString().contains("Internal server error"));
    }

    // 9. CustomErrorController Browser View response
    @Test
    @DisplayName("9. CustomErrorController returns 'error' view with friendly message for browser requests")
    public void testCustomErrorControllerForBrowser() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/test-result");
        req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);

        Model model = new ExtendedModelMap();
        Object result = errorController.handleError(req, model);

        assertEquals("error", result);
        assertEquals(500, model.getAttribute("statusCode"));
        assertEquals("Unexpected Server Error", model.getAttribute("errorTitle"));
        assertNotNull(model.getAttribute("errorMessage"));
    }

    // 10. Reviews JSON escapes </script> tags
    @Test
    @DisplayName("10. Script tags in question reviews are escaped preventing XSS and HTML break")
    public void testReviewsJsonScriptEscaping() {
        TestAttempt attempt = attemptService.createOrGetAttempt(
                student1.getUid(), student1.getName(), "college_snjb_quant", "Medium", null
        );
        attempt.setQuestionReviewsJson("[{\"question\":\"What is </script><script>alert(1)</script>?\"}]");
        attemptService.submitAttempt(attempt.getId(), student1.getUid(), Collections.emptyMap(), "manual");

        MockHttpServletRequest request = createAuthRequest(student1);
        Model model = new ExtendedModelMap();

        String view = testController.viewTestResult(null, attempt.getId(), request, model);
        assertEquals("test-result", view);

        String reviewsJson = (String) model.getAttribute("reviewsJson");
        assertNotNull(reviewsJson);
        assertFalse(reviewsJson.contains("</script>"));
    }
}
