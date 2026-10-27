package com.tpobuddy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model representing a Test Series entity in the AI-Powered Test Series System.
 * Supports distinct categories: COLLEGE and PLATFORM.
 */
public class TestSeries implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CATEGORY_COLLEGE = "COLLEGE";
    public static final String CATEGORY_PLATFORM = "PLATFORM";

    // Difficulty constants
    public static final String DIFFICULTY_MEDIUM = "Medium";
    public static final String DIFFICULTY_HARD = "Hard";
    public static final String DIFFICULTY_EXTRA_HARD = "Extra Hard";

    private String seriesId;
    private String name;
    private String category; // COLLEGE or PLATFORM
    private String seriesType; // SNJB_PLACEMENT, BASIC_CS, COLLEGE_SPECIFIC, PRACTICE_TOPIC, CS_MIXED, etc.
    private String description;
    private String subject; // Main subject or "Multi-Section Assessment"
    private List<String> syllabusTopics = new ArrayList<>();
    private Map<String, Integer> sectionWeights = new HashMap<>(); // Section name -> target count
    private List<String> supportedDifficultyLevels = new ArrayList<>();
    private int questionCount = 30; // Standardized: exactly 30 questions
    private int candidatePoolSize = 50; // Standardized: exactly 50 candidates generated
    private int durationMinutes = 30;
    private boolean active = true;
    private String createdBy = "SYSTEM";
    private long updatedAt = System.currentTimeMillis();
    private String icon = "📝";
    private int totalMarks = 60; // 30 questions * 2 marks
    private int marksPerQuestion = 2;
    private boolean negativeMarking = false;
    private double negativeMarks = 0.5;

    public TestSeries() {
        this.supportedDifficultyLevels.add(DIFFICULTY_MEDIUM);
        this.supportedDifficultyLevels.add(DIFFICULTY_HARD);
        this.supportedDifficultyLevels.add(DIFFICULTY_EXTRA_HARD);
    }

    public TestSeries(String seriesId, String name, String category, String seriesType,
                      String description, String subject, String icon, int durationMinutes) {
        this();
        this.seriesId = seriesId;
        this.name = name;
        this.category = category;
        this.seriesType = seriesType;
        this.description = description;
        this.subject = subject;
        this.icon = icon;
        this.durationMinutes = durationMinutes;
        this.totalMarks = this.questionCount * this.marksPerQuestion;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeriesType() {
        return seriesType;
    }

    public void setSeriesType(String seriesType) {
        this.seriesType = seriesType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getSyllabusTopics() {
        return syllabusTopics;
    }

    public void setSyllabusTopics(List<String> syllabusTopics) {
        this.syllabusTopics = syllabusTopics;
    }

    public Map<String, Integer> getSectionWeights() {
        return sectionWeights;
    }

    public void setSectionWeights(Map<String, Integer> sectionWeights) {
        this.sectionWeights = sectionWeights;
    }

    public List<String> getSupportedDifficultyLevels() {
        return supportedDifficultyLevels;
    }

    public void setSupportedDifficultyLevels(List<String> supportedDifficultyLevels) {
        this.supportedDifficultyLevels = supportedDifficultyLevels;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
        this.totalMarks = questionCount * this.marksPerQuestion;
    }

    public int getCandidatePoolSize() {
        return candidatePoolSize;
    }

    public void setCandidatePoolSize(int candidatePoolSize) {
        this.candidatePoolSize = candidatePoolSize;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getMarksPerQuestion() {
        return marksPerQuestion;
    }

    public void setMarksPerQuestion(int marksPerQuestion) {
        this.marksPerQuestion = marksPerQuestion;
        this.totalMarks = this.questionCount * marksPerQuestion;
    }

    public boolean isNegativeMarking() {
        return negativeMarking;
    }

    public void setNegativeMarking(boolean negativeMarking) {
        this.negativeMarking = negativeMarking;
    }

    public double getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(double negativeMarks) {
        this.negativeMarks = negativeMarks;
    }
}
