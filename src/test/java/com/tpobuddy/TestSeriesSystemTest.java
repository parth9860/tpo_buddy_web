package com.tpobuddy;

import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.TestResult;
import com.tpobuddy.model.TestSeries;
import com.tpobuddy.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Automated Test Suite for the AI-Powered Test Series System.
 * Tests:
 * 1. College vs Platform Series Separation
 * 2. SNJB 4 Sections & 12 Computer Science Subjects
 * 3. Difficulty Level Validation (Medium, Hard, Extra Hard)
 * 4. Question Validation & Answer-Leak Screening
 * 5. 50 Candidate -> 30 Selected Balancing & Randomization
 * 6. Student Isolation & Independent Attempts
 * 7. Server-Side Grading, Negative Marking & Weak Topic Detection
 * 8. Reading Comprehension & Output Prediction
 */
public class TestSeriesSystemTest {

    private TestSeriesService testSeriesService;
    private QuestionValidationService validationService;
    private QuestionSelectionService selectionService;
    private TestEvaluationService evaluationService;

    @BeforeEach
    public void setUp() {
        testSeriesService = new TestSeriesService();
        validationService = new QuestionValidationService();
        selectionService = new QuestionSelectionService();
        evaluationService = new TestEvaluationService();
    }

    // =========================================================================
    // 1. College vs Platform Series Separation
    // =========================================================================

    @Test
    @DisplayName("1. College and Platform series must be strictly isolated")
    public void testCollegeAndPlatformSeparation() {
        List<TestSeries> collegeSeries = testSeriesService.getSeriesByCategory(TestSeries.CATEGORY_COLLEGE);
        List<TestSeries> platformSeries = testSeriesService.getSeriesByCategory(TestSeries.CATEGORY_PLATFORM);

        assertNotNull(collegeSeries, "College series list must not be null");
        assertNotNull(platformSeries, "Platform series list must not be null");
        assertFalse(collegeSeries.isEmpty(), "College series list must not be empty");
        assertFalse(platformSeries.isEmpty(), "Platform series list must not be empty");

        // Verify college series contain ONLY category COLLEGE
        for (TestSeries ts : collegeSeries) {
            assertEquals(TestSeries.CATEGORY_COLLEGE, ts.getCategory(),
                    "College series " + ts.getSeriesId() + " must have category COLLEGE");
        }

        // Verify platform series contain ONLY category PLATFORM
        for (TestSeries ts : platformSeries) {
            assertEquals(TestSeries.CATEGORY_PLATFORM, ts.getCategory(),
                    "Platform series " + ts.getSeriesId() + " must have category PLATFORM");
        }

        // Verify zero overlap between IDs
        Set<String> collegeIds = new HashSet<>();
        for (TestSeries ts : collegeSeries) collegeIds.add(ts.getSeriesId());

        for (TestSeries ts : platformSeries) {
            assertFalse(collegeIds.contains(ts.getSeriesId()),
                    "Platform series ID must not collide with college series: " + ts.getSeriesId());
        }
    }

    // =========================================================================
    // 2. SNJB Placement Preparation 4 Sections & 12 CS Subjects
    // =========================================================================

    @Test
    @DisplayName("2. SNJB Placement series must include all 4 sections and 12 Core CS subjects")
    public void testSnjbSectionsAndCoreCsSubjects() {
        // Verify 4 SNJB Sections exist in syllabus registry
        List<String> quantTopics = PlacementSyllabusRegistry.TOPICS_QUANTITATIVE;
        List<String> logicalTopics = PlacementSyllabusRegistry.TOPICS_LOGICAL;
        List<String> verbalTopics = PlacementSyllabusRegistry.TOPICS_VERBAL;
        List<String> techTopics = PlacementSyllabusRegistry.TOPICS_TECHNICAL;

        assertFalse(quantTopics.isEmpty(), "Quantitative Aptitude syllabus must be structured");
        assertFalse(logicalTopics.isEmpty(), "Logical Reasoning syllabus must be structured");
        assertFalse(verbalTopics.isEmpty(), "Verbal Ability syllabus must be structured");
        assertFalse(techTopics.isEmpty(), "Technical syllabus must be structured");

        // Verify SNJB Mock Test Series weights
        TestSeries snjbMock = testSeriesService.getSeriesById("college_snjb_full_mock");
        assertNotNull(snjbMock, "SNJB Full Mock series must exist");
        assertEquals(30, snjbMock.getQuestionCount(), "SNJB Mock must specify exactly 30 questions");
        assertEquals(50, snjbMock.getCandidatePoolSize(), "SNJB Mock candidate pool must be 50");

        Map<String, Integer> weights = snjbMock.getSectionWeights();
        assertEquals(8, weights.get(PlacementSyllabusRegistry.SECTION_QUANTITATIVE));
        assertEquals(8, weights.get(PlacementSyllabusRegistry.SECTION_LOGICAL));
        assertEquals(7, weights.get(PlacementSyllabusRegistry.SECTION_VERBAL));
        assertEquals(7, weights.get(PlacementSyllabusRegistry.SECTION_TECHNICAL));

        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(30, totalWeight, "Sum of section weights must equal 30");

        // Verify 12 Basic CS Subjects exist
        List<String> csSubjects = PlacementSyllabusRegistry.BASIC_CS_SUBJECTS;
        assertEquals(12, csSubjects.size(), "There must be exactly 12 Basic CS subjects");
        assertTrue(csSubjects.contains("Programming Fundamentals"));
        assertTrue(csSubjects.contains("Object-Oriented Programming"));
        assertTrue(csSubjects.contains("Data Structures and Algorithms"));
        assertTrue(csSubjects.contains("DBMS and SQL"));
        assertTrue(csSubjects.contains("Operating Systems"));
        assertTrue(csSubjects.contains("Computer Networks"));
        assertTrue(csSubjects.contains("Software Engineering"));
        assertTrue(csSubjects.contains("Computer Organization and Architecture"));
        assertTrue(csSubjects.contains("Java Programming"));
        assertTrue(csSubjects.contains("Python Programming"));
        assertTrue(csSubjects.contains("C Programming"));
        assertTrue(csSubjects.contains("Problem Solving and Output Prediction"));
    }

    // =========================================================================
    // 3. Difficulty Levels Calibration
    // =========================================================================

    @Test
    @DisplayName("3. All test series must support Medium, Hard, and Extra Hard difficulty levels")
    public void testDifficultyLevels() {
        List<TestSeries> all = testSeriesService.getAllSeries();
        for (TestSeries ts : all) {
            List<String> diffs = ts.getSupportedDifficultyLevels();
            assertTrue(diffs.contains(TestSeries.DIFFICULTY_MEDIUM), "Must support Medium");
            assertTrue(diffs.contains(TestSeries.DIFFICULTY_HARD), "Must support Hard");
            assertTrue(diffs.contains(TestSeries.DIFFICULTY_EXTRA_HARD), "Must support Extra Hard");
        }

        // Verify Question Bank filtering for Extra Hard
        List<Question> extraHardQs = PlacementQuestionBank.getQuestionsByFilter(null, null, "Extra Hard", 5);
        assertNotNull(extraHardQs);
        assertFalse(extraHardQs.isEmpty(), "Question bank must contain Extra Hard questions");
        for (Question q : extraHardQs) {
            assertEquals("Extra Hard", q.getDifficulty());
        }
    }

    // =========================================================================
    // 4. Candidate Question Validation & Answer-Leak Screening
    // =========================================================================

    @Test
    @DisplayName("4. Validation engine must reject invalid options, leaked answers, and duplicates")
    public void testQuestionValidation() {
        // Valid Question: Question(id, category, topic, difficulty, question, optA, optB, optC, optD, correct, explanation)
        Question valid = new Question(
                101, "Quantitative Aptitude", "Number System", "Medium",
                "What is the remainder when 7^84 is divided by 342?",
                "1", "7", "49", "341", 0, "Remainder theorem calculation: 7^3 = 343 = 342*1 + 1."
        );
        assertTrue(validationService.validateQuestion(valid).isValid(), "Valid question must pass validation");

        // Invalid: Empty option
        Question invalidOption = new Question(
                102, "Logical Reasoning", "Puzzles", "Medium",
                "Sample puzzle question for placement prep?",
                "", "Opt B", "Opt C", "Opt D", 0, "Valid explanation."
        );
        assertFalse(validationService.validateQuestion(invalidOption).isValid(), "Question with empty option must fail");

        // Invalid: Out-of-bounds answer index
        Question outOfBounds = new Question(
                103, "Logical Reasoning", "Puzzles", "Medium",
                "Sample logical question with invalid answer key?",
                "A", "B", "C", "D", 5, "Valid explanation."
        );
        assertFalse(validationService.validateQuestion(outOfBounds).isValid(), "Question with invalid answer index must fail");

        // Invalid: Leaked answer in question text
        Question leaked = new Question(
                104, "Verbal Ability", "Grammar", "Medium",
                "Which of the following is correct? Answer is (B)",
                "A", "B", "C", "D", 1, "Explanation provided."
        );
        assertFalse(validationService.validateQuestion(leaked).isValid(), "Question with leaked answer in text must fail");

        // Duplicate filtering
        List<Question> pool = new ArrayList<>();
        pool.add(valid);
        Question duplicate = new Question(
                105, "Quantitative Aptitude", "Number System", "Medium",
                "what is the remainder when 7^84 is divided by 342?", // case-insensitive duplicate
                "1", "7", "49", "341", 0, "Remainder calculation."
        );
        pool.add(duplicate);

        List<Question> deduplicated = validationService.filterValidPool(pool);
        assertEquals(1, deduplicated.size(), "Deduplication must eliminate near-identical questions");
    }

    // =========================================================================
    // 5. 50 Candidate Pool -> 30 Selected Questions with Section Weights
    // =========================================================================

    @Test
    @DisplayName("5. Selection service must choose exactly 30 questions respecting section weights")
    public void testQuestionSelectionFromCandidatePool() {
        // Generate a 50 candidate pool across 4 sections
        List<Question> candidates = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            String sec;
            if (i <= 15) sec = PlacementSyllabusRegistry.SECTION_QUANTITATIVE;
            else if (i <= 28) sec = PlacementSyllabusRegistry.SECTION_LOGICAL;
            else if (i <= 39) sec = PlacementSyllabusRegistry.SECTION_VERBAL;
            else sec = PlacementSyllabusRegistry.SECTION_TECHNICAL;

            Question q = new Question(
                    i, sec, "Topic " + (i % 5), "Medium",
                    "Question statement number " + i + " on syllabus topics.",
                    "Option A " + i, "Option B " + i, "Option C " + i, "Option D " + i,
                    i % 4, "Explanation for question " + i
            );
            q.setSection(sec);
            candidates.add(q);
        }
        assertEquals(50, candidates.size());

        TestSeries snjbMock = testSeriesService.getSeriesById("college_snjb_full_mock");
        List<Question> selected = selectionService.selectQuestionsForAttempt(candidates, snjbMock, "Medium");

        assertNotNull(selected);
        assertEquals(30, selected.size(), "Selection must yield exactly 30 questions");

        // Count per section
        int quantCount = 0, logicalCount = 0, verbalCount = 0, techCount = 0;
        for (Question q : selected) {
            if (PlacementSyllabusRegistry.SECTION_QUANTITATIVE.equals(q.getSection())) quantCount++;
            else if (PlacementSyllabusRegistry.SECTION_LOGICAL.equals(q.getSection())) logicalCount++;
            else if (PlacementSyllabusRegistry.SECTION_VERBAL.equals(q.getSection())) verbalCount++;
            else if (PlacementSyllabusRegistry.SECTION_TECHNICAL.equals(q.getSection())) techCount++;
        }

        assertEquals(8, quantCount, "Must select 8 Quantitative questions");
        assertEquals(8, logicalCount, "Must select 8 Logical questions");
        assertEquals(7, verbalCount, "Must select 7 Verbal questions");
        assertEquals(7, techCount, "Must select 7 Technical questions");
    }

    // =========================================================================
    // 6. Independent Student Attempts & Randomization
    // =========================================================================

    @Test
    @DisplayName("6. Two students starting the same test must receive independently randomized attempts")
    public void testStudentIndependenceAndRandomization() {
        List<Question> candidates = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            candidates.add(new Question(
                    i, "Quantitative Aptitude", "Algebra", "Medium",
                    "Algebra Statement number " + i + " with distinct formulation.",
                    "Option A", "Option B", "Option C", "Option D",
                    0, "Explanation for question " + i
            ));
        }

        TestSeries series = new TestSeries();
        series.setQuestionCount(30);

        List<Question> attemptA = selectionService.selectQuestionsForAttempt(candidates, series, "Medium");
        List<Question> attemptB = selectionService.selectQuestionsForAttempt(candidates, series, "Medium");

        assertEquals(30, attemptA.size());
        assertEquals(30, attemptB.size());

        // Question order should not be identical due to SecureRandom shuffling
        List<Integer> idsA = attemptA.stream().map(Question::getId).toList();
        List<Integer> idsB = attemptB.stream().map(Question::getId).toList();

        assertNotEquals(idsA, idsB, "Two student attempts should have independently randomized ordering");
    }

    // =========================================================================
    // 7. Server-Side Evaluation, Negative Marking & Weak Topics Detection
    // =========================================================================

    @Test
    @DisplayName("7. Authoritative evaluation must calculate accuracy, apply negative marks, and detect weak topics")
    public void testAuthoritativeGradingAndWeakTopics() {
        List<Question> questions = new ArrayList<>();
        // 5 questions on Number System, 5 questions on Java Programming
        for (int i = 1; i <= 10; i++) {
            String sec = i <= 5 ? "Quantitative Aptitude" : "Technical and Coding";
            String top = i <= 5 ? "Number System" : "Java Programming";
            Question q = new Question(
                    i, sec, top, "Medium",
                    "Question statement " + i + " verifying fundamentals.",
                    "A", "B", "C", "D",
                    0, "Answer is A because calculation verified."
            );
            questions.add(q);
        }

        TestSeries series = new TestSeries();
        series.setNegativeMarking(true);
        series.setNegativeMarks(0.5);
        series.setMarksPerQuestion(2);

        TestAttempt attempt = new TestAttempt();
        attempt.setId("test_attempt_123");
        attempt.setUserId("student_456");

        // Answers:
        // Student answers Q1-Q5 (Number System) all WRONG (chose option 1 instead of 0)
        // Student answers Q6-Q10 (Java) all CORRECT (chose option 0)
        Map<Integer, Integer> answers = new HashMap<>();
        for (int i = 1; i <= 5; i++) answers.put(i, 1); // 5 incorrect
        for (int i = 6; i <= 10; i++) answers.put(i, 0); // 5 correct

        TestEvaluationService.EvaluationPayload payload = evaluationService.evaluateAttempt(attempt, questions, answers, series);
        assertNotNull(payload);
        TestResult result = payload.result;
        assertNotNull(result);

        assertEquals(5, result.getCorrectCount());
        assertEquals(5, result.getIncorrectCount());
        assertEquals(0, result.getUnansweredCount());

        // Marks calculation: 5 * 2 = 10; Deductions: 5 * 0.5 = 2.5; Final = 7.5
        assertEquals(7.5, result.getMarksEarned(), 0.01);

        // Accuracy: 5 / 10 = 50.0%
        assertEquals(50.0, result.getAccuracy(), 0.01);
        assertFalse(result.isPassed(), "50% score should not pass (passing threshold >= 60%)");

        // Weak topic detection: Number System accuracy is 0.0%, so it MUST be flagged as weak
        assertTrue(result.getWeakTopics().contains("Number System"),
                "Number System should be identified as a weak topic");
        assertFalse(result.getWeakTopics().contains("Java Programming"),
                "Java Programming had 100% accuracy and should not be weak");

        // Recommended practice should be provided
        assertFalse(result.getRecommendedRevisionTopics().isEmpty(),
                "Targeted recommendations should be generated for weak topics");
    }

    // =========================================================================
    // 8. Reading Comprehension & Output Prediction Formatting
    // =========================================================================

    @Test
    @DisplayName("8. Reading comprehension and output prediction questions must preserve formatted passages")
    public void testReadingComprehensionAndOutputPrediction() {
        Question rc = new Question(
                301, "Verbal Ability", "Reading Comprehension", "Hard",
                "What is the primary message of the author in paragraph 2?",
                "Option 1", "Option 2", "Option 3", "Option 4", 2,
                "Explanation based on the text: primary message is accelerating digital transformation."
        );
        rc.setQuestionType("READING_COMPREHENSION");
        rc.setPassageOrCode("In an era of rapid technological acceleration, artificial intelligence has emerged as a transformative paradigm across industries.");

        assertTrue(validationService.validateQuestion(rc).isValid());
        assertEquals("READING_COMPREHENSION", rc.getQuestionType());
        assertNotNull(rc.getPassageOrCode());
        assertTrue(rc.getPassageOrCode().contains("artificial intelligence"));
    }
}
