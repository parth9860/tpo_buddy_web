package com.tpobuddy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing an AI Mock Interview Session and Evaluation Report.
 * Synchronized with CareerPulse Android InterviewActivity & MockInterviewReportActivity.
 */
public class InterviewSession implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class InterviewQuestion implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private String questionText;
        private String category; // "Technical", "Behavioral", "System Design", "HR"
        private String expectedConcepts;
        private String userAnswer;
        private String aiFeedback;
        private int score; // 0 to 10

        public InterviewQuestion() {}

        public InterviewQuestion(int id, String questionText, String category, String expectedConcepts) {
            this.id = id;
            this.questionText = questionText;
            this.category = category;
            this.expectedConcepts = expectedConcepts;
            this.score = 0;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getExpectedConcepts() { return expectedConcepts; }
        public void setExpectedConcepts(String expectedConcepts) { this.expectedConcepts = expectedConcepts; }

        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

        public String getAiFeedback() { return aiFeedback; }
        public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    private String sessionId;
    private String userId;
    private String studentName;
    private String jobRole;
    private long startTime;
    private long endTime;
    private String status; // "IN_PROGRESS", "COMPLETED"
    private int currentQuestionIndex;
    private List<InterviewQuestion> questions = new ArrayList<>();

    // Evaluation Report fields
    private int overallScore;
    private int techScore;
    private int commScore;
    private String summary;
    private List<String> strengths = new ArrayList<>();
    private List<String> weaknesses = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    public InterviewSession() {
        this.startTime = System.currentTimeMillis();
        this.status = "IN_PROGRESS";
        this.currentQuestionIndex = 0;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getJobRole() { return jobRole; }
    public void setJobRole(String jobRole) { this.jobRole = jobRole; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }

    public List<InterviewQuestion> getQuestions() { return questions; }
    public void setQuestions(List<InterviewQuestion> questions) { this.questions = questions; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public int getTechScore() { return techScore; }
    public void setTechScore(int techScore) { this.techScore = techScore; }

    public int getCommScore() { return commScore; }
    public void setCommScore(int commScore) { this.commScore = commScore; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getWeaknesses() { return weaknesses; }
    public void setWeaknesses(List<String> weaknesses) { this.weaknesses = weaknesses; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
