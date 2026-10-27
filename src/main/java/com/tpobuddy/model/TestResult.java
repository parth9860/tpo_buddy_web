package com.tpobuddy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Persisted test result and performance analytics entity.
 * Stored in `testResults` collection and synchronized with Firebase profiles.
 */
public class TestResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String resultId;
    private String attemptId;
    private String studentUid;
    private String studentName;
    private String category; // COLLEGE or PLATFORM
    private String seriesId;
    private String seriesName;
    private String subject;
    private String difficulty; // Medium, Hard, Extra Hard
    private int score; // 0-100%
    private double marksEarned;
    private double totalMarks;
    private double accuracy; // percentage of answered questions that were correct
    private int correctCount;
    private int incorrectCount;
    private int unansweredCount;
    private int totalQuestions;
    private boolean passed;
    private String sectionBreakdownJson = "{}";
    private String topicWisePerformanceJson = "{}";
    private List<String> weakTopics = new ArrayList<>();
    private List<String> recommendedRevisionTopics = new ArrayList<>();
    private long timeTakenSeconds;
    private String proctoringStatus = "VALIDATED";
    private int riskScore = 0;
    private long completedAt = System.currentTimeMillis();

    public TestResult() {
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public String getStudentUid() {
        return studentUid;
    }

    public void setStudentUid(String studentUid) {
        this.studentUid = studentUid;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public double getMarksEarned() {
        return marksEarned;
    }

    public void setMarksEarned(double marksEarned) {
        this.marksEarned = marksEarned;
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getIncorrectCount() {
        return incorrectCount;
    }

    public void setIncorrectCount(int incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    public int getUnansweredCount() {
        return unansweredCount;
    }

    public void setUnansweredCount(int unansweredCount) {
        this.unansweredCount = unansweredCount;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getSectionBreakdownJson() {
        return sectionBreakdownJson;
    }

    public void setSectionBreakdownJson(String sectionBreakdownJson) {
        this.sectionBreakdownJson = sectionBreakdownJson;
    }

    public String getTopicWisePerformanceJson() {
        return topicWisePerformanceJson;
    }

    public void setTopicWisePerformanceJson(String topicWisePerformanceJson) {
        this.topicWisePerformanceJson = topicWisePerformanceJson;
    }

    public List<String> getWeakTopics() {
        return weakTopics;
    }

    public void setWeakTopics(List<String> weakTopics) {
        this.weakTopics = weakTopics;
    }

    public List<String> getRecommendedRevisionTopics() {
        return recommendedRevisionTopics;
    }

    public void setRecommendedRevisionTopics(List<String> recommendedRevisionTopics) {
        this.recommendedRevisionTopics = recommendedRevisionTopics;
    }

    public long getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public void setTimeTakenSeconds(long timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public String getProctoringStatus() {
        return proctoringStatus;
    }

    public void setProctoringStatus(String proctoringStatus) {
        this.proctoringStatus = proctoringStatus;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }
}
