package com.tpobuddy;

import com.tpobuddy.controller.InterviewController;
import com.tpobuddy.model.InterviewSession;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.InterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end Unit and Integration Test Suite for the AI Mock Interview Feature:
 * 1. Session start with role, difficulty, and question count customization.
 * 2. Resilience and fallback question generation.
 * 3. Answer evaluation, score calculation, feedback, and question progression.
 * 4. Idempotency on repeated answer submission.
 * 5. Session isolation and cross-user authorization enforcement.
 * 6. Report generation, category scoring, and status transition.
 * 7. Active session discovery and seamless recovery on page reload.
 * 8. Controller HTTP 401 unauthenticated access rejection.
 * 9. Controller HTTP 403 cross-student session tampering prevention.
 * 10. JSP view navigation and model population.
 */
public class InterviewSystemTest {

    private FirebaseService firebaseService;
    private InterviewService interviewService;
    private InterviewController interviewController;

    private User student1;
    private User student2;

    @BeforeEach
    public void setUp() {
        firebaseService = new FirebaseService();
        interviewService = new InterviewService(firebaseService);
        interviewController = new InterviewController(interviewService);

        student1 = new User();
        student1.setUid("student_uid_101");
        student1.setName("Alice Sharma");
        student1.setEmail("alice@college.edu");
        student1.setRole("student");

        student2 = new User();
        student2.setUid("student_uid_202");
        student2.setName("Bob Verma");
        student2.setEmail("bob@college.edu");
        student2.setRole("student");
    }

    @Test
    @DisplayName("1. Starting interview session initializes role, difficulty, question count, and questions")
    public void testStartSession_InitializesCorrectly() {
        InterviewSession session = interviewService.startSession(
                student1.getUid(),
                student1.getName(),
                "Full Stack Developer",
                "Hard",
                5
        );

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals(student1.getUid(), session.getUserId());
        assertEquals("Full Stack Developer", session.getJobRole());
        assertEquals("Hard", session.getDifficulty());
        assertEquals(5, session.getQuestionCount());
        assertEquals("IN_PROGRESS", session.getStatus());
        assertEquals(0, session.getCurrentQuestionIndex());
        assertFalse(session.getQuestions().isEmpty());
        assertEquals(5, session.getQuestions().size());

        InterviewSession.InterviewQuestion firstQ = session.getQuestions().get(0);
        assertNotNull(firstQ.getQuestionText());
        assertFalse(firstQ.getQuestionText().isEmpty());
        assertNotNull(firstQ.getCategory());
    }

    @Test
    @DisplayName("2. Unconfigured or unreachable Gemini cleanly uses calibrated fallback questions")
    public void testStartSession_FallbackQuestionsAvailable() {
        InterviewSession session = interviewService.startSession(
                student1.getUid(),
                student1.getName(),
                "Java Backend Developer",
                "Medium",
                3
        );

        assertNotNull(session);
        assertEquals(3, session.getQuestions().size());
        for (InterviewSession.InterviewQuestion q : session.getQuestions()) {
            assertNotNull(q.getQuestionText());
            assertNotNull(q.getExpectedConcepts());
        }
    }

    @Test
    @DisplayName("3. Evaluating candidate answer computes score, feedback, strengths, and advances index")
    public void testEvaluateAnswer_ScoresAndAdvances() {
        InterviewSession session = interviewService.startSession(
                student1.getUid(),
                student1.getName(),
                "Frontend React Developer",
                "Medium",
                3
        );

        String answer = "The Virtual DOM is an in-memory representation of real DOM elements. React uses a diffing algorithm during reconciliation to compute minimal DOM updates, batching changes with Fiber for high performance.";
        Map<String, Object> result = interviewService.evaluateAnswer(student1.getUid(), session.getSessionId(), 1, answer);

        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("score"));
        assertTrue((Integer) result.get("score") >= 0 && (Integer) result.get("score") <= 10);
        assertNotNull(result.get("feedback"));
        assertEquals(1, result.get("nextQuestionIndex"));
        assertFalse((Boolean) result.get("isCompleted"));

        InterviewSession.InterviewQuestion q1 = session.getQuestions().get(0);
        assertEquals(answer, q1.getUserAnswer());
        assertEquals(result.get("score"), q1.getScore());
        assertEquals(result.get("feedback"), q1.getAiFeedback());
        assertNotNull(q1.getStrengths());
        assertNotNull(q1.getAreasToImprove());
    }

    @Test
    @DisplayName("4. Idempotency: submitting answer to same question twice returns cached evaluation")
    public void testEvaluateAnswer_Idempotency() {
        InterviewSession session = interviewService.startSession(
                student1.getUid(),
                student1.getName(),
                "Data Analyst & AI",
                "Medium",
                3
        );

        String answer = "Inner join returns matching records, left join returns all rows from left table, outer join returns all rows from both.";
        Map<String, Object> firstEval = interviewService.evaluateAnswer(student1.getUid(), session.getSessionId(), 1, answer);
        int initialNextIndex = (Integer) firstEval.get("nextQuestionIndex");

        // Submit duplicate request
        Map<String, Object> secondEval = interviewService.evaluateAnswer(student1.getUid(), session.getSessionId(), 1, answer);

        assertTrue((Boolean) secondEval.get("success"));
        assertEquals(firstEval.get("score"), secondEval.get("score"));
        assertEquals(firstEval.get("feedback"), secondEval.get("feedback"));
        assertEquals(initialNextIndex, secondEval.get("nextQuestionIndex"));
        assertEquals(1, session.getCurrentQuestionIndex());
    }

    @Test
    @DisplayName("5. Session isolation: student B cannot evaluate or modify student A's interview")
    public void testSessionIsolation_PreventsCrossUserAccess() {
        InterviewSession session = interviewService.startSession(
                student1.getUid(),
                student1.getName(),
                "Full Stack Developer",
                "Medium",
                3
        );

        Map<String, Object> result = interviewService.evaluateAnswer(
                student2.getUid(), // Student 2 attempting to submit answer to Student 1's session
                session.getSessionId(),
                1,
                "Unauthorized candidate answer"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(((String) result.get("error")).contains("Unauthorized access"));
        assertEquals(0, session.getCurrentQuestionIndex());
    }

    @Test
    @DisplayName("6. Finishing session calculates overall, technical, and communication scores and marks completed")
    public void testFinishSession_ProducesCompleteReport() {
        InterviewSession session = interviewService.startSession(
                student1.getUid(),
                student1.getName(),
                "Java Backend Developer",
                "Hard",
                2
        );

        // Answer Q1
        interviewService.evaluateAnswer(student1.getUid(), session.getSessionId(), 1,
                "HashMap is not thread-safe. ConcurrentHashMap uses CAS operations and synchronized buckets or lock striping.");

        // Answer Q2
        interviewService.evaluateAnswer(student1.getUid(), session.getSessionId(), 2,
                "Component is generic, Service marks business domain, and Repository enables automatic persistence exception translation.");

        InterviewSession finished = interviewService.finishSession(student1.getUid(), session.getSessionId());

        assertNotNull(finished);
        assertEquals("COMPLETED", finished.getStatus());
        assertTrue(finished.getOverallScore() >= 0 && finished.getOverallScore() <= 100);
        assertTrue(finished.getTechScore() >= 0 && finished.getTechScore() <= 100);
        assertTrue(finished.getCommScore() >= 0 && finished.getCommScore() <= 100);
        assertNotNull(finished.getSummary());
        assertFalse(finished.getSummary().isEmpty());
        assertNotNull(finished.getStrengths());
        assertNotNull(finished.getRecommendations());
    }

    @Test
    @DisplayName("7. Active session discovery allows candidate to resume ongoing interview on page reload")
    public void testActiveSessionRecovery() {
        InterviewSession session = interviewService.startSession(
                student1.getUid(),
                student1.getName(),
                "Full Stack Developer",
                "Medium",
                5
        );

        InterviewSession active = interviewService.getActiveSession(student1.getUid());
        assertNotNull(active);
        assertEquals(session.getSessionId(), active.getSessionId());
        assertEquals("IN_PROGRESS", active.getStatus());

        // Once finished, no active session should be returned
        interviewService.finishSession(student1.getUid(), session.getSessionId());
        InterviewSession postFinishActive = interviewService.getActiveSession(student1.getUid());
        assertNull(postFinishActive);
    }

    @Test
    @DisplayName("8. Controller enforces authentication on API endpoints (HTTP 401)")
    public void testController_UnauthenticatedAccessRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No user in session

        Map<String, Object> payload = new HashMap<>();
        payload.put("role", "Full Stack Developer");

        ResponseEntity<Map<String, Object>> startRes = interviewController.startInterview(payload, request);
        assertEquals(HttpStatus.UNAUTHORIZED, startRes.getStatusCode());

        ResponseEntity<Map<String, Object>> activeRes = interviewController.getActiveSession(request);
        assertEquals(HttpStatus.UNAUTHORIZED, activeRes.getStatusCode());

        payload.put("sessionId", "fake_session_123");
        payload.put("questionId", 1);
        payload.put("answer", "Hello");
        ResponseEntity<Map<String, Object>> answerRes = interviewController.submitAnswer(payload, request);
        assertEquals(HttpStatus.UNAUTHORIZED, answerRes.getStatusCode());

        ResponseEntity<Map<String, Object>> finishRes = interviewController.finishInterview(payload, request);
        assertEquals(HttpStatus.UNAUTHORIZED, finishRes.getStatusCode());
    }

    @Test
    @DisplayName("9. Controller enforces authorization and blocks cross-student tampering (HTTP 403)")
    public void testController_CrossStudentTamperingBlocked() {
        InterviewSession session = interviewService.startSession(
                student1.getUid(),
                student1.getName(),
                "Full Stack Developer",
                "Medium",
                3
        );

        // Student 2 session
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute("currentUser", student2);
        req.setSession(httpSession);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", session.getSessionId());
        payload.put("questionId", 1);
        payload.put("answer", "Tampered answer by Bob");

        ResponseEntity<Map<String, Object>> response = interviewController.submitAnswer(payload, req);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("10. JSP view navigation directs authenticated student to interview view with model attributes")
    public void testViewInterview_AuthenticatedStudent_PopulatesModel() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute("currentUser", student1);
        req.setSession(httpSession);

        Model model = new ExtendedModelMap();
        String viewName = interviewController.viewInterview(req, model);

        assertEquals("interview", viewName);
        assertTrue(model.containsAttribute("user"));
        assertTrue(model.containsAttribute("latestReport"));
        assertTrue(model.containsAttribute("history"));
    }
}
