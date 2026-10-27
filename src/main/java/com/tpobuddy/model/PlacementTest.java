package com.tpobuddy.model;

import java.io.Serializable;

/**
 * Model representing a configured Placement Test within the Company Placement Test Series.
 */
public class PlacementTest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String description;
    private String category; // Quantitative Aptitude, Logical Reasoning, Verbal Ability, Technical and Coding, Comprehensive Mock
    private String subCategory; // Specific topic (e.g. Number System, Percentages, Java OOP)
    private String difficulty = "Medium"; // Easy, Medium, Hard
    private int questionCount = 25;
    private int durationMinutes = 30;
    private int totalMarks = 50;
    private int marksPerQuestion = 2;
    private int passPercentage = 60;
    private String targetBranch = "ALL";
    private String targetYear = "ALL";
    private int allowedAttempts = 0; // 0 = unlimited
    private String status = "PUBLISHED"; // PUBLISHED, DRAFT, ARCHIVED
    private boolean negativeMarking = false;
    private double negativeMarks = 0.5;
    private boolean randomizeQuestions = true;
    private String instructions = "Standard timed assessment with integrity monitoring. 3 security violations will trigger auto-submission.";
    private long createdAt = System.currentTimeMillis();

    public PlacementTest() {
    }

    public PlacementTest(String id, String title, String description, String category, String subCategory,
                         String difficulty, int questionCount, int durationMinutes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.subCategory = subCategory;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.durationMinutes = durationMinutes;
        this.totalMarks = questionCount * 2;
        this.marksPerQuestion = 2;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }

    public int getMarksPerQuestion() { return marksPerQuestion; }
    public void setMarksPerQuestion(int marksPerQuestion) { this.marksPerQuestion = marksPerQuestion; }

    public int getPassPercentage() { return passPercentage; }
    public void setPassPercentage(int passPercentage) { this.passPercentage = passPercentage; }

    public String getTargetBranch() { return targetBranch; }
    public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }

    public String getTargetYear() { return targetYear; }
    public void setTargetYear(String targetYear) { this.targetYear = targetYear; }

    public int getAllowedAttempts() { return allowedAttempts; }
    public void setAllowedAttempts(int allowedAttempts) { this.allowedAttempts = allowedAttempts; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isNegativeMarking() { return negativeMarking; }
    public void setNegativeMarking(boolean negativeMarking) { this.negativeMarking = negativeMarking; }

    public double getNegativeMarks() { return negativeMarks; }
    public void setNegativeMarks(double negativeMarks) { this.negativeMarks = negativeMarks; }

    public boolean isRandomizeQuestions() { return randomizeQuestions; }
    public void setRandomizeQuestions(boolean randomizeQuestions) { this.randomizeQuestions = randomizeQuestions; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
