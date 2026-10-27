package com.tpobuddy.service;

import com.tpobuddy.model.Question;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service enforcing rigorous validation on AI-generated and candidate question pools.
 * Validates:
 * - Non-empty question and options
 * - Distinct, plausible distractors
 * - Correct option index bounds (0 to 3)
 * - Required explanations
 * - Mandatory passages for Reading Comprehension
 * - Mandatory code snippets for Output Prediction
 * - Prevention of leaked answers embedded in question text
 * - Intra-pool duplicate detection via normalized similarity
 */
@Service
public class QuestionValidationService {

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * Validates a single question against all structural and semantic integrity rules.
     */
    public ValidationResult validateQuestion(Question q) {
        List<String> errors = new ArrayList<>();
        if (q == null) {
            errors.add("Question object is null");
            return new ValidationResult(false, errors);
        }

        // 1. Question text validation
        String qText = q.getQuestion();
        if (qText == null || qText.trim().length() < 10) {
            errors.add("Question text is empty or too short (minimum 10 characters required)");
        }

        // 2. Options validation (A, B, C, D must all be non-empty and unique)
        String optA = q.getOptionA();
        String optB = q.getOptionB();
        String optC = q.getOptionC();
        String optD = q.getOptionD();

        if (optA == null || optA.trim().isEmpty()) errors.add("Option A is empty");
        if (optB == null || optB.trim().isEmpty()) errors.add("Option B is empty");
        if (optC == null || optC.trim().isEmpty()) errors.add("Option C is empty");
        if (optD == null || optD.trim().isEmpty()) errors.add("Option D is empty");

        if (optA != null && optB != null && optC != null && optD != null) {
            Set<String> uniqueOptions = new HashSet<>();
            uniqueOptions.add(optA.trim().toLowerCase());
            uniqueOptions.add(optB.trim().toLowerCase());
            uniqueOptions.add(optC.trim().toLowerCase());
            uniqueOptions.add(optD.trim().toLowerCase());
            if (uniqueOptions.size() < 4) {
                errors.add("Duplicate options detected in question choices");
            }
        }

        // 3. Correct answer validation (0, 1, 2, 3)
        int correct = q.getCorrectOption();
        if (correct < 0 || correct > 3) {
            errors.add("Correct option index (" + correct + ") out of range [0-3]");
        }

        // 4. Explanation validation
        if (q.getExplanation() == null || q.getExplanation().trim().length() < 5) {
            errors.add("Explanation is missing or insufficient for candidate review");
        }

        // 5. Special question type validation
        String type = q.getQuestionType();
        if ("READING_COMPREHENSION".equalsIgnoreCase(type)) {
            if (q.getPassageOrCode() == null || q.getPassageOrCode().trim().length() < 40) {
                errors.add("Reading comprehension question is missing the required passage");
            }
        } else if ("OUTPUT_PREDICTION".equalsIgnoreCase(type)) {
            if (q.getPassageOrCode() == null || q.getPassageOrCode().trim().length() < 10) {
                // If code is inside question text, that's acceptable; otherwise verify passageOrCode
                if (!qText.contains(";") && !qText.contains("{") && !qText.contains("print")) {
                    errors.add("Output prediction question is missing executable code snippet");
                }
            }
        }

        // 6. Security: Verify correct answer is not trivially leaked in the question text
        if (qText != null && correct >= 0 && correct <= 3) {
            String normalizedQuestion = qText.toLowerCase();
            char correctChar = (char) ('a' + correct);
            String leakedLetter1 = "answer is (" + correctChar + ")";
            String leakedLetter2 = "answer is " + correctChar;
            String leakedLetter3 = "correct option is " + correctChar;

            if (normalizedQuestion.contains("answer is (")
                    || normalizedQuestion.contains("correct option is")
                    || normalizedQuestion.contains("answer:")
                    || normalizedQuestion.contains(leakedLetter1)
                    || normalizedQuestion.contains(leakedLetter2)
                    || normalizedQuestion.contains(leakedLetter3)) {
                errors.add("Question text appears to leak the correct answer");
            }

            String correctText = getOptionText(q, correct);
            if (correctText != null && !correctText.trim().isEmpty()) {
                String answerLower = correctText.trim().toLowerCase();
                if (answerLower.length() > 2 && (normalizedQuestion.contains("answer is " + answerLower)
                        || normalizedQuestion.contains("answer: " + answerLower))) {
                    errors.add("Question text appears to leak the correct answer");
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates an entire candidate question pool and removes duplicates / malformed questions.
     * Returns a list of strictly valid, unique questions.
     */
    public List<Question> filterValidPool(List<Question> rawPool) {
        if (rawPool == null || rawPool.isEmpty()) {
            return Collections.emptyList();
        }

        List<Question> validList = new ArrayList<>();
        Set<String> seenNormalizedQuestions = new HashSet<>();

        for (Question q : rawPool) {
            ValidationResult vr = validateQuestion(q);
            if (vr.isValid()) {
                String normalized = normalizeForDeduplication(q.getQuestion());
                if (!seenNormalizedQuestions.contains(normalized)) {
                    seenNormalizedQuestions.add(normalized);
                    validList.add(q);
                }
            }
        }

        return validList;
    }

    private String getOptionText(Question q, int index) {
        return switch (index) {
            case 0 -> q.getOptionA();
            case 1 -> q.getOptionB();
            case 2 -> q.getOptionC();
            case 3 -> q.getOptionD();
            default -> null;
        };
    }

    private String normalizeForDeduplication(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
}
