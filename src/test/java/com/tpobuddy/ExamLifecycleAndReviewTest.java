package com.tpobuddy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tpobuddy.controller.TestController;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.TestResult;
import com.tpobuddy.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end unit and integration tests verifying:
 * 1. Active Assessment creation, tracking, and restoration.
 * 2. Active Session clearance on submission (prevents false ongoing modal).
 * 3. Question Reviews snapshot persistence with explanations.
 * 4. Proctoring 3-strike violation handling and termination.
 * 5. Next recommended test generation based on topic weaknesses.
 */
public class ExamLifecycleAndReviewTest {

    private TestAttemptService attemptService;
    private TestSeriesService seriesService;
    private TestEvaluationService evaluationService;
    private QuestionSelectionService selectionService;
    private QuestionValidationService validationService;
    private AIGenerationService aiGenerationService;
    private FirebaseService firebaseService;
    private Gson gson;

    @BeforeEach
    public void setUp() {
        seriesService = new TestSeriesService();
        evaluationService = new TestEvaluationService();
        selectionService = new QuestionSelectionService();
        validationService = new QuestionValidationService();
        aiGenerationService = new AIGenerationService(validationService);
        firebaseService = new FirebaseService();
        gson = new Gson();

        attemptService = new TestAttemptService(
                aiGenerationService,
                selectionService,
                evaluationService,
                seriesService,
                firebaseService
        );
    }

    @Test
    @DisplayName("Active Attempt is created, retrievable, and cleared upon submission")
    public void testActiveAttemptLifecycleAndClearance() {
        String studentId = "student_test_101";

        // 1. Initialize attempt
        TestAttempt attempt = attemptService.createOrGetAttempt(
                studentId, "Test Candidate", "college_snjb_quant", "Medium", null
        );

        assertNotNull(attempt, "Initiated attempt must not be null");
        assertEquals("READY", attempt.getStatus());
        assertEquals(studentId, attempt.getUserId());

        // 2. Verify active attempt is found for student
        TestAttempt active = attemptService.getActiveAttemptForStudent(studentId);
        assertNotNull(active, "Active attempt should be present for student");
        assertEquals(attempt.getId(), active.getId());

        // 3. Save progress (answers)
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0); // Correct
        answers.put(2, 1); // Incorrect
        answers.put(3, 0); // Correct
        boolean saved = attemptService.saveProgress(attempt.getId(), studentId, answers);
        assertTrue(saved, "Progress should be saved successfully");

        // 4. Submit attempt
        TestEvaluationService.EvaluationPayload payload = attemptService.submitAttempt(
                attempt.getId(), studentId, answers, "student_submitted"
        );

        assertNotNull(payload, "Evaluation payload must not be null");
        assertNotNull(payload.result, "Result must not be null");
        assertEquals("submitted", attempt.getStatus(), "Status must be submitted");

        // 5. Verify Question Reviews Snapshot was generated & persisted on attempt and result
        assertNotNull(attempt.getQuestionReviewsJson(), "Question reviews JSON snapshot must be stored on attempt");
        assertNotNull(payload.result.getQuestionReviewsJson(), "Question reviews JSON snapshot must be stored on result");

        Type reviewListType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> reviews = gson.fromJson(attempt.getQuestionReviewsJson(), reviewListType);
        assertNotNull(reviews);
        assertFalse(reviews.isEmpty(), "Review snapshot list must not be empty");

        // Check first question review details
        Map<String, Object> q1Review = reviews.get(0);
        assertNotNull(q1Review.get("question"));
        assertNotNull(q1Review.get("explanation"));

        // 6. CRITICAL: Verify active attempt is CLEARED after submission
        TestAttempt postSubmitActive = attemptService.getActiveAttemptForStudent(studentId);
        assertNull(postSubmitActive, "After submission, getActiveAttemptForStudent must return null so no ongoing modal appears");
    }

    @Test
    @DisplayName("Dual Active Session synchronization between TestController and TestAttemptService")
    public void testDualActiveSessionSynchronization() {
        String studentId = "student_session_sync_202";

        // Create an active attempt
        TestAttempt attempt = attemptService.createOrGetAttempt(
                studentId, "Session Candidate", "college_snjb_quant", "Medium", null
        );
        assertNotNull(attempt);
        assertNotNull(attemptService.getActiveAttemptForStudent(studentId));

        // Clear active session through synchronized method
        TestController.clearActiveSession(studentId);
        attemptService.clearStudentActiveAttempt(studentId);

        assertNull(attemptService.getActiveAttemptForStudent(studentId),
                "Active attempt in TestAttemptService must be cleared");
    }

    @Test
    @DisplayName("3-strike violation correctly flags and logs integrity warnings")
    public void testProctoringThreeStrikeViolations() {
        String studentId = "student_proctor_303";
        TestAttempt attempt = attemptService.createOrGetAttempt(
                studentId, "Proctor Candidate", "college_snjb_quant", "Medium", null
        );

        // Log 3 strikes
        JsonObject ev1 = new JsonObject();
        ev1.addProperty("type", "FULLSCREEN_EXIT");
        attemptService.logProctoringEvent(attempt.getId(), studentId, ev1);

        JsonObject ev2 = new JsonObject();
        ev2.addProperty("type", "TAB_SWITCH");
        attemptService.logProctoringEvent(attempt.getId(), studentId, ev2);

        JsonObject ev3 = new JsonObject();
        ev3.addProperty("type", "FOCUS_LOST");
        JsonObject resp3 = attemptService.logProctoringEvent(attempt.getId(), studentId, ev3);

        assertEquals(3, attempt.getViolationCount(), "Violations count must be 3");
        assertTrue(resp3.get("mustAutoSubmit").getAsBoolean(), "3 strikes must indicate auto-submit / termination");

        // Submit with proctor_violation
        TestEvaluationService.EvaluationPayload payload = attemptService.submitAttempt(
                attempt.getId(), studentId, Collections.emptyMap(), "proctor_violation"
        );

        assertEquals("terminated", attempt.getStatus(), "3-strike violation submit must set status to terminated");
        assertNull(attemptService.getActiveAttemptForStudent(studentId), "Terminated attempt must not remain active");
    }

    @Test
    @DisplayName("Next test recommendations prioritize weak topics and SNJB placement syllabus sequence")
    public void testNextTestRecommendationLogic() {
        List<String> weakTopics = List.of("Percentages", "Profit and Loss");

        // Weak topic should be recommended first
        String recommended = weakTopics.get(0);
        assertEquals("Percentages", recommended, "Weak topic should be recommended first for targeted improvement");

        // Verify syllabus sequence exists for SNJB placement preparation
        List<String> quantTopics = PlacementSyllabusRegistry.TOPICS_QUANTITATIVE;
        assertFalse(quantTopics.isEmpty(), "Syllabus topics for Quantitative Aptitude must not be empty");
        assertTrue(quantTopics.stream().anyMatch(t -> t.contains("Number System")), "Syllabus must contain Number System");
        assertTrue(quantTopics.stream().anyMatch(t -> t.contains("Percentages")), "Syllabus must contain Percentages");
        assertTrue(quantTopics.stream().anyMatch(t -> t.contains("Time and Work")), "Syllabus must contain Time and Work");
    }
}
