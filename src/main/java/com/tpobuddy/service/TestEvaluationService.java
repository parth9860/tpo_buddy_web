package com.tpobuddy.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.TestResult;
import com.tpobuddy.model.TestSeries;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service performing authoritative server-side test evaluation,
 * sectional breakdown, topic performance analysis, and targeted revision recommendations.
 * Student-supplied scores and answer keys are never trusted.
 */
@Service
public class TestEvaluationService {

    private final Gson gson = new Gson();

    public static class EvaluationPayload {
        public final TestResult result;
        public final JsonArray questionReviews;

        public EvaluationPayload(TestResult result, JsonArray questionReviews) {
            this.result = result;
            this.questionReviews = questionReviews;
        }
    }

    /**
     * Evaluates a completed test attempt against authoritative server-side questions and answers.
     */
    public EvaluationPayload evaluateAttempt(TestAttempt attempt, List<Question> authoritativeQuestions,
                                             Map<Integer, Integer> submittedAnswers, TestSeries series) {
        long now = System.currentTimeMillis();
        String resultId = "res_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        int totalQuestions = (authoritativeQuestions != null) ? authoritativeQuestions.size() : 30;
        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;
        double marksPerQ = (series != null && series.getMarksPerQuestion() > 0) ? series.getMarksPerQuestion() : 2.0;
        double positiveMarks = 0.0;
        double negativeMarksDeducted = 0.0;
        double totalMarks = totalQuestions * marksPerQ;

        boolean applyNegative = (series != null && series.isNegativeMarking());
        double negativeDeduction = (series != null) ? series.getNegativeMarks() : 0.5;

        // Analytics maps
        Map<String, int[]> sectionStats = new LinkedHashMap<>(); // section -> [correct, total]
        Map<String, int[]> topicStats = new LinkedHashMap<>();   // topic -> [correct, total]

        JsonArray reviews = new JsonArray();

        if (authoritativeQuestions != null) {
            for (Question q : authoritativeQuestions) {
                int qId = q.getId();
                Integer selectedOpt = (submittedAnswers != null) ? submittedAnswers.get(qId) : null;
                boolean isAnswered = (selectedOpt != null && selectedOpt >= 0 && selectedOpt <= 3);
                boolean isCorrect = (isAnswered && selectedOpt == q.getCorrectOption());

                String sec = (q.getSection() != null && !q.getSection().isEmpty()) ? q.getSection() :
                        (q.getCategory() != null ? q.getCategory() : "General");
                String top = (q.getTopic() != null && !q.getTopic().isEmpty()) ? q.getTopic() : "General";

                sectionStats.computeIfAbsent(sec, k -> new int[2])[1]++;
                topicStats.computeIfAbsent(top, k -> new int[2])[1]++;

                if (isCorrect) {
                    correctCount++;
                    double qMark = (q.getMarks() > 1 ? q.getMarks() : marksPerQ);
                    positiveMarks += qMark;
                    sectionStats.get(sec)[0]++;
                    topicStats.get(top)[0]++;
                } else if (isAnswered) {
                    incorrectCount++;
                    if (applyNegative) {
                        negativeMarksDeducted += negativeDeduction;
                    }
                } else {
                    unansweredCount++;
                }

                // Create review object for post-submission review
                JsonObject r = new JsonObject();
                r.addProperty("questionId", qId);
                r.addProperty("question", q.getQuestion());
                r.addProperty("section", sec);
                r.addProperty("topic", top);
                r.addProperty("difficulty", q.getDifficulty());
                r.addProperty("questionType", q.getQuestionType());
                r.addProperty("passageOrCode", q.getPassageOrCode());
                r.addProperty("optionA", q.getOptionA());
                r.addProperty("optionB", q.getOptionB());
                r.addProperty("optionC", q.getOptionC());
                r.addProperty("optionD", q.getOptionD());
                r.addProperty("selectedOption", (selectedOpt != null) ? selectedOpt : -1);
                r.addProperty("correctOption", q.getCorrectOption());
                r.addProperty("isCorrect", isCorrect);
                r.addProperty("isAnswered", isAnswered);
                r.addProperty("explanation", q.getExplanation());
                reviews.add(r);
            }
        }

        double marksEarned = Math.max(0.0, positiveMarks - negativeMarksDeducted);
        int scorePercent = totalQuestions > 0 ? Math.round((correctCount * 100.0f) / totalQuestions) : 0;
        int answeredTotal = correctCount + incorrectCount;
        double accuracy = (answeredTotal > 0) ? Math.round((correctCount * 100.0) / answeredTotal) : 0.0;
        boolean passed = scorePercent >= 60;

        // Build section breakdown JSON
        JsonObject secJson = new JsonObject();
        for (Map.Entry<String, int[]> e : sectionStats.entrySet()) {
            JsonObject sObj = new JsonObject();
            sObj.addProperty("correct", e.getValue()[0]);
            sObj.addProperty("total", e.getValue()[1]);
            int secPct = (e.getValue()[1] > 0) ? Math.round((e.getValue()[0] * 100.0f) / e.getValue()[1]) : 0;
            sObj.addProperty("scorePercent", secPct);
            secJson.add(e.getKey(), sObj);
        }

        // Build topic breakdown JSON & identify weak topics
        JsonObject topJson = new JsonObject();
        List<String> weakTopics = new ArrayList<>();
        List<String> revisionTopics = new ArrayList<>();

        for (Map.Entry<String, int[]> e : topicStats.entrySet()) {
            String topicName = e.getKey();
            int c = e.getValue()[0];
            int t = e.getValue()[1];
            double topAccuracy = (t > 0) ? (c * 100.0 / t) : 0.0;

            JsonObject tObj = new JsonObject();
            tObj.addProperty("correct", c);
            tObj.addProperty("total", t);
            tObj.addProperty("accuracy", Math.round(topAccuracy));
            topJson.add(topicName, tObj);

            if (topAccuracy < 60.0) {
                weakTopics.add(topicName);
                revisionTopics.add("Focused Revision: " + topicName + " (" + Math.round(topAccuracy) + "% accuracy)");
            }
        }

        if (weakTopics.isEmpty() && scorePercent < 75) {
            revisionTopics.add("Speed and accuracy drill on high-frequency questions");
        } else if (scorePercent >= 80) {
            revisionTopics.add("Ready for Advanced / Extra Hard mock assessments");
        }

        long timeTaken = (attempt != null && attempt.getStartTime() > 0) ?
                Math.max(1, (now - attempt.getStartTime()) / 1000) : 0L;

        // Create TestResult entity
        TestResult result = new TestResult();
        result.setResultId(resultId);
        result.setAttemptId(attempt != null ? attempt.getId() : "att_unknown");
        result.setStudentUid(attempt != null ? attempt.getUserId() : "");
        result.setStudentName(attempt != null ? attempt.getStudentName() : "Student");
        result.setCategory(attempt != null ? attempt.getCategory() : TestSeries.CATEGORY_COLLEGE);
        result.setSeriesId(attempt != null ? attempt.getSeriesId() : (series != null ? series.getSeriesId() : ""));
        result.setSeriesName(series != null ? series.getName() : "Placement Assessment");
        result.setSubject(series != null ? series.getSubject() : "Core Placement");
        result.setDifficulty(attempt != null ? attempt.getDifficulty() : "Medium");
        result.setScore(scorePercent);
        result.setMarksEarned(marksEarned);
        result.setTotalMarks(totalMarks);
        result.setAccuracy(accuracy);
        result.setCorrectCount(correctCount);
        result.setIncorrectCount(incorrectCount);
        result.setUnansweredCount(unansweredCount);
        result.setTotalQuestions(totalQuestions);
        result.setPassed(passed);
        result.setSectionBreakdownJson(secJson.toString());
        result.setTopicWisePerformanceJson(topJson.toString());
        result.setWeakTopics(weakTopics);
        result.setRecommendedRevisionTopics(revisionTopics);
        result.setTimeTakenSeconds(timeTaken);
        result.setProctoringStatus(attempt != null ? attempt.getProctoringStatus() : "VALIDATED");
        result.setRiskScore(attempt != null ? attempt.getRiskScore() : 0);
        result.setCompletedAt(now);

        return new EvaluationPayload(result, reviews);
    }
}
