package com.tpobuddy;

import com.google.gson.JsonObject;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.TestSeries;
import com.tpobuddy.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Authoritative Automated Test Suite verifying:
 * 1. Strict Topic Isolation for the 6 SNJB Foundational topics.
 * 2. Multi-topic isolation (Group 1: Number System, HCF & LCM, Divisibility Rules;
 *    Group 2: Percentages, Profit and Loss, Discounts).
 * 3. Prevention of unrelated topic contamination (Zero Technical/Verbal/Logical in Quant tests).
 * 4. Duplicate question prevention within an attempt.
 * 5. Fullscreen exit detection, deduplication, and 3-strike consequence.
 * 6. Idempotent submission and race condition resilience.
 * 7. Student attempt isolation and immutability after submission.
 */
public class TopicIsolationAndExamIntegrityTest {

    private TestAttemptService attemptService;
    private TestSeriesService seriesService;
    private TestEvaluationService evaluationService;
    private QuestionSelectionService selectionService;
    private QuestionValidationService validationService;
    private AIGenerationService aiGenerationService;
    private FirebaseService firebaseService;

    @BeforeEach
    public void setUp() {
        seriesService = new TestSeriesService();
        evaluationService = new TestEvaluationService();
        selectionService = new QuestionSelectionService();
        validationService = new QuestionValidationService();
        aiGenerationService = new AIGenerationService(validationService);
        firebaseService = new FirebaseService();

        attemptService = new TestAttemptService(
                aiGenerationService,
                selectionService,
                evaluationService,
                seriesService,
                firebaseService
        );
    }

    // =========================================================================
    // 1. STRICT SINGLE TOPIC ACCURACY TESTS (SNJB PRIORITY 6 TOPICS)
    // =========================================================================

    @Test
    @DisplayName("1.1 Number System test contains ONLY Number System questions")
    public void testNumberSystemStrictTopicIsolation() {
        List<String> topics = List.of("Number System");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_ns_1", "Student One", "college_snjb_quant", "Medium", topics
        );

        assertNotNull(attempt);
        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions, "Questions list must not be null");
        assertFalse(questions.isEmpty(), "Questions list must not be empty");

        for (Question q : questions) {
            assertTrue(q.getTopic().equalsIgnoreCase("Number System") ||
                       q.getTopic().toLowerCase().contains("number system"),
                    "Question topic must be Number System, but got: " + q.getTopic());
            assertEquals("Quantitative Aptitude", q.getCategory());
        }
    }

    @Test
    @DisplayName("1.2 HCF and LCM test contains ONLY HCF and LCM questions")
    public void testHcfLcmStrictTopicIsolation() {
        List<String> topics = List.of("HCF and LCM");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_hcf_2", "Student Two", "college_snjb_quant", "Medium", topics
        );

        assertNotNull(attempt);
        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        for (Question q : questions) {
            assertTrue(q.getTopic().equalsIgnoreCase("HCF and LCM") ||
                       q.getTopic().toLowerCase().contains("hcf") ||
                       q.getTopic().toLowerCase().contains("lcm"),
                    "Question topic must be HCF and LCM, but got: " + q.getTopic());
        }
    }

    @Test
    @DisplayName("1.3 Divisibility Rules test contains ONLY Divisibility questions")
    public void testDivisibilityStrictTopicIsolation() {
        List<String> topics = List.of("Divisibility Rules");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_div_3", "Student Three", "college_snjb_quant", "Medium", topics
        );

        assertNotNull(attempt);
        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        for (Question q : questions) {
            assertTrue(q.getTopic().equalsIgnoreCase("Divisibility Rules") ||
                       q.getTopic().toLowerCase().contains("divisib"),
                    "Question topic must be Divisibility Rules, but got: " + q.getTopic());
        }
    }

    @Test
    @DisplayName("1.4 Percentages test contains ONLY Percentage questions")
    public void testPercentagesStrictTopicIsolation() {
        List<String> topics = List.of("Percentages");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_pct_4", "Student Four", "college_snjb_quant", "Medium", topics
        );

        assertNotNull(attempt);
        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        for (Question q : questions) {
            assertTrue(q.getTopic().equalsIgnoreCase("Percentages") ||
                       q.getTopic().toLowerCase().contains("percent"),
                    "Question topic must be Percentages, but got: " + q.getTopic());
        }
    }

    @Test
    @DisplayName("1.5 Profit and Loss test contains ONLY Profit and Loss questions")
    public void testProfitLossStrictTopicIsolation() {
        List<String> topics = List.of("Profit and Loss");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_pl_5", "Student Five", "college_snjb_quant", "Medium", topics
        );

        assertNotNull(attempt);
        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        for (Question q : questions) {
            assertTrue(q.getTopic().equalsIgnoreCase("Profit and Loss") ||
                       q.getTopic().toLowerCase().contains("profit") ||
                       q.getTopic().toLowerCase().contains("loss"),
                    "Question topic must be Profit and Loss, but got: " + q.getTopic());
        }
    }

    @Test
    @DisplayName("1.6 Discounts test contains ONLY Discount questions")
    public void testDiscountsStrictTopicIsolation() {
        List<String> topics = List.of("Discounts");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_disc_6", "Student Six", "college_snjb_quant", "Medium", topics
        );

        assertNotNull(attempt);
        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        for (Question q : questions) {
            assertTrue(q.getTopic().equalsIgnoreCase("Discounts") ||
                       q.getTopic().toLowerCase().contains("discount"),
                    "Question topic must be Discounts, but got: " + q.getTopic());
        }
    }

    // =========================================================================
    // 2. COMPOUND TOPIC GROUP TESTS & ZERO UNRELATED CONTAMINATION
    // =========================================================================

    @Test
    @DisplayName("2.1 Test for Number System, HCF and LCM, and Divisibility Rules contains ONLY those 3 topics")
    public void testCompoundGroupOneIsolation() {
        List<String> allowed = List.of("Number System", "HCF and LCM", "Divisibility Rules");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_grp1", "Group 1 Candidate", "college_snjb_quant", "Medium", allowed
        );

        assertNotNull(attempt);
        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        for (Question q : questions) {
            boolean matches = PlacementQuestionBank.isQuestionInTopics(q, allowed);
            assertTrue(matches, "Question must belong strictly to Group 1 topics, but was: " + q.getTopic());

            // Zero Percentages, Profit and Loss, Discounts, Verbal, or Tech
            String top = q.getTopic().toLowerCase();
            assertFalse(top.contains("percent"), "Group 1 must not contain Percentages");
            assertFalse(top.contains("profit"), "Group 1 must not contain Profit and Loss");
            assertFalse(top.contains("discount"), "Group 1 must not contain Discounts");
            assertFalse(top.contains("java"), "Group 1 must not contain Java");
            assertFalse(top.contains("grammar"), "Group 1 must not contain Verbal");
        }
    }

    @Test
    @DisplayName("2.2 Test for Percentages, Profit and Loss, and Discounts contains ONLY those 3 topics")
    public void testCompoundGroupTwoIsolation() {
        List<String> allowed = List.of("Percentages", "Profit and Loss", "Discounts");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_grp2", "Group 2 Candidate", "college_snjb_quant", "Medium", allowed
        );

        assertNotNull(attempt);
        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        for (Question q : questions) {
            boolean matches = PlacementQuestionBank.isQuestionInTopics(q, allowed);
            assertTrue(matches, "Question must belong strictly to Group 2 topics, but was: " + q.getTopic());

            // Zero Number System, HCF, Divisibility, Verbal, or Tech
            String top = q.getTopic().toLowerCase();
            assertFalse(top.contains("number system"), "Group 2 must not contain Number System");
            assertFalse(top.contains("hcf"), "Group 2 must not contain HCF");
            assertFalse(top.contains("lcm"), "Group 2 must not contain LCM");
            assertFalse(top.contains("divisib"), "Group 2 must not contain Divisibility");
            assertFalse(top.contains("python"), "Group 2 must not contain Python");
        }
    }

    @Test
    @DisplayName("2.3 Zero duplicate questions within the same attempt")
    public void testZeroDuplicateQuestionsInAttempt() {
        List<String> allowed = List.of("Percentages", "Profit and Loss", "Discounts");
        TestAttempt attempt = attemptService.createOrGetAttempt(
                "student_dup_test", "Deduplication Candidate", "college_snjb_quant", "Medium", allowed
        );

        List<Question> questions = attemptService.getAttemptQuestions(attempt.getId());
        assertNotNull(questions);

        Set<Integer> seenIds = new HashSet<>();
        Set<String> seenTexts = new HashSet<>();

        for (Question q : questions) {
            assertFalse(seenIds.contains(q.getId()), "Duplicate question ID detected: " + q.getId());
            seenIds.add(q.getId());

            String normalizedText = q.getQuestion().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            assertFalse(seenTexts.contains(normalizedText), "Duplicate question text detected: " + q.getQuestion());
            seenTexts.add(normalizedText);
        }
    }

    // =========================================================================
    // 3. FULLSCREEN EXIT, 3-STRIKE VIOLATION & CONCURRENCY TESTS
    // =========================================================================

    @Test
    @DisplayName("3.1 Exiting fullscreen triggers violation logging and increments strike count")
    public void testFullscreenExitViolationHandling() {
        String studentId = "student_fs_violator";
        TestAttempt attempt = attemptService.createOrGetAttempt(
                studentId, "FS Candidate", "college_snjb_quant", "Medium", null
        );

        assertEquals(0, attempt.getViolationCount());
        assertEquals(0, attempt.getFullscreenExitCount());

        JsonObject fsEvent = new JsonObject();
        fsEvent.addProperty("type", "FULLSCREEN_EXIT");
        fsEvent.addProperty("message", "Fullscreen exit detected");

        JsonObject resp = attemptService.logProctoringEvent(attempt.getId(), studentId, fsEvent);
        assertTrue(resp.get("success").getAsBoolean());
        assertEquals(1, attempt.getViolationCount());
        assertEquals(1, attempt.getFullscreenExitCount());
        assertFalse(resp.get("mustAutoSubmit").getAsBoolean(), "Strike 1 should not trigger auto-submit");
    }

    @Test
    @DisplayName("3.2 3 Strikes trigger automatic exam termination and score penalty")
    public void testThreeStrikesConsequence() {
        String studentId = "student_three_strikes";
        TestAttempt attempt = attemptService.createOrGetAttempt(
                studentId, "Strike Candidate", "college_snjb_quant", "Medium", null
        );

        // Strike 1
        JsonObject e1 = new JsonObject();
        e1.addProperty("type", "FULLSCREEN_EXIT");
        attemptService.logProctoringEvent(attempt.getId(), studentId, e1);

        // Strike 2
        JsonObject e2 = new JsonObject();
        e2.addProperty("type", "TAB_SWITCH");
        attemptService.logProctoringEvent(attempt.getId(), studentId, e2);

        // Strike 3
        JsonObject e3 = new JsonObject();
        e3.addProperty("type", "FULLSCREEN_EXIT");
        JsonObject r3 = attemptService.logProctoringEvent(attempt.getId(), studentId, e3);

        assertEquals(3, attempt.getViolationCount());
        assertTrue(r3.get("mustAutoSubmit").getAsBoolean(), "Third violation must flag for automatic submission");

        // Submit with reason proctor_violation
        TestEvaluationService.EvaluationPayload payload = attemptService.submitAttempt(
                attempt.getId(), studentId, Collections.emptyMap(), "proctor_violation"
        );

        assertNotNull(payload);
        assertEquals("terminated", attempt.getStatus());
        assertEquals(100, attempt.getRiskScore());
        assertEquals("HIGH", attempt.getRiskLevel());
        assertEquals("FLAGGED", attempt.getProctoringStatus());
    }

    @Test
    @DisplayName("3.3 Idempotent submission: repeated submit calls return identical cached result")
    public void testIdempotentSubmission() {
        String studentId = "student_idempotent";
        TestAttempt attempt = attemptService.createOrGetAttempt(
                studentId, "Idempotent Candidate", "college_snjb_quant", "Medium", null
        );

        Map<Integer, Integer> answers = Map.of(101, 1, 102, 0);

        // First submission
        TestEvaluationService.EvaluationPayload first = attemptService.submitAttempt(
                attempt.getId(), studentId, answers, "student_submitted"
        );
        assertNotNull(first);
        String firstResultId = first.result.getResultId();
        int firstScore = first.result.getScore();

        // Repeated submission of the SAME attempt (e.g. double click or retry)
        TestEvaluationService.EvaluationPayload second = attemptService.submitAttempt(
                attempt.getId(), studentId, answers, "student_submitted"
        );
        assertNotNull(second);
        assertSame(first, second, "Repeated submission must return cached evaluation payload");
        assertEquals(firstResultId, second.result.getResultId());
        assertEquals(firstScore, second.result.getScore());
    }

    @Test
    @DisplayName("3.4 Student attempt isolation: students cannot access or submit another student's attempt")
    public void testStudentAttemptOwnershipIsolation() {
        String studentA = "student_alpha";
        String studentB = "student_beta";

        TestAttempt attemptA = attemptService.createOrGetAttempt(
                studentA, "Student Alpha", "college_snjb_quant", "Medium", null
        );

        // Student B attempts to fetch Student A's attempt
        TestAttempt foreignAttempt = attemptService.getAttempt(attemptA.getId(), studentB);
        assertNull(foreignAttempt, "Student B must not be able to retrieve Student A's attempt");

        // Student B attempts to get questions for Student A's attempt
        assertNull(attemptService.getSanitizedQuestions(attemptA.getId(), studentB),
                "Student B must be denied sanitized questions of Student A");

        // Student B attempts to save progress on Student A's attempt
        boolean progressHacked = attemptService.saveProgress(attemptA.getId(), studentB, Map.of(1, 0));
        assertFalse(progressHacked, "Student B must not be able to save progress on Student A's attempt");
    }

    @Test
    @DisplayName("3.5 Submitted attempts are immutable and cannot receive answers or violations")
    public void testSubmittedAttemptImmutability() {
        String studentId = "student_immutable";
        TestAttempt attempt = attemptService.createOrGetAttempt(
                studentId, "Immutable Candidate", "college_snjb_quant", "Medium", null
        );

        attemptService.submitAttempt(attempt.getId(), studentId, Collections.emptyMap(), "manual");
        assertEquals("submitted", attempt.getStatus());

        // Subsequent save-progress must be rejected
        boolean savedPostSubmit = attemptService.saveProgress(attempt.getId(), studentId, Map.of(101, 2));
        assertFalse(savedPostSubmit, "Answers cannot be saved to an already submitted attempt");

        // Subsequent proctoring events must be rejected
        JsonObject ev = new JsonObject();
        ev.addProperty("type", "FULLSCREEN_EXIT");
        JsonObject evResp = attemptService.logProctoringEvent(attempt.getId(), studentId, ev);
        assertFalse(evResp.get("success").getAsBoolean(), "Events after submission must be ignored");
    }
}
