package com.tpobuddy.model;

import java.io.Serializable;

/**
 * Test Attempt Record with full examination lifecycle, proctoring,
 * anti-cheating risk detection, and audit review fields.
 */
public class TestAttempt implements Serializable {
    private static final long serialVersionUID = 2L;

    private String id;
    private String userId;
    private String studentName;
    private String category;
    private String topic;
    private int score; // 0-100 (%)
    private int totalQuestions;
    private int correctAnswers;
    private long savedAt;
    private boolean passed;

    // Proctoring & Lifecycle Fields
    private String testId;
    private long startTime;
    private int durationSeconds;
    private long deadline;
    private long submissionTime;
    private String status; // "in_progress", "submitted", "auto_submitted_security", "auto_submitted_timeout"
    private String submissionReason; // "manual", "automatic-security-submission", "timeout"
    private int violationCount; // 0 to 3 security warnings
    private String savedAnswersJson; // JSON representation of questionId -> selectedOption
    private int currentQuestionIndex;

    // Risk Detection & Monitoring
    private int riskScore; // 0-100
    private String riskLevel; // "LOW", "MODERATE", "HIGH"
    private int focusLostCount;
    private int fullscreenExitCount;
    private int cameraDisconnectCount;
    private int copyPasteCount;
    private String monitoringEventsJson; // JSON array of logged audit events
    private String proctoringStatus; // "VALIDATED", "SUSPICIOUS", "FLAGGED", "REVIEWED"
    private String reviewNotes;

    public TestAttempt() {
        this.status = "submitted";
        this.submissionReason = "manual";
        this.violationCount = 0;
        this.riskScore = 0;
        this.riskLevel = "LOW";
        this.proctoringStatus = "VALIDATED";
    }

    public TestAttempt(String id, String userId, String category, String topic, int score,
                       int totalQuestions, int correctAnswers, long savedAt, boolean passed) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.topic = topic;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.savedAt = savedAt;
        this.passed = passed;
        this.status = "SUBMITTED";
        this.riskScore = 0;
        this.riskLevel = "LOW";
        this.proctoringStatus = "VALIDATED";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public long getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(long savedAt) {
        this.savedAt = savedAt;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getFocusLostCount() {
        return focusLostCount;
    }

    public void setFocusLostCount(int focusLostCount) {
        this.focusLostCount = focusLostCount;
    }

    public int getFullscreenExitCount() {
        return fullscreenExitCount;
    }

    public void setFullscreenExitCount(int fullscreenExitCount) {
        this.fullscreenExitCount = fullscreenExitCount;
    }

    public int getCameraDisconnectCount() {
        return cameraDisconnectCount;
    }

    public void setCameraDisconnectCount(int cameraDisconnectCount) {
        this.cameraDisconnectCount = cameraDisconnectCount;
    }

    public int getCopyPasteCount() {
        return copyPasteCount;
    }

    public void setCopyPasteCount(int copyPasteCount) {
        this.copyPasteCount = copyPasteCount;
    }

    public String getMonitoringEventsJson() {
        return monitoringEventsJson;
    }

    public void setMonitoringEventsJson(String monitoringEventsJson) {
        this.monitoringEventsJson = monitoringEventsJson;
    }

    public String getProctoringStatus() {
        return proctoringStatus;
    }

    public void setProctoringStatus(String proctoringStatus) {
        this.proctoringStatus = proctoringStatus;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public String getTestId() {
        return testId != null ? testId : (category != null ? category : id);
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public long getSubmissionTime() {
        return submissionTime > 0 ? submissionTime : savedAt;
    }

    public void setSubmissionTime(long submissionTime) {
        this.submissionTime = submissionTime;
    }

    public String getSubmissionReason() {
        return submissionReason != null ? submissionReason : "manual";
    }

    public void setSubmissionReason(String submissionReason) {
        this.submissionReason = submissionReason;
    }

    public int getViolationCount() {
        return violationCount;
    }

    public void setViolationCount(int violationCount) {
        this.violationCount = violationCount;
    }

    public String getSavedAnswersJson() {
        return savedAnswersJson != null ? savedAnswersJson : "{}";
    }

    public void setSavedAnswersJson(String savedAnswersJson) {
        this.savedAnswersJson = savedAnswersJson;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }
}
