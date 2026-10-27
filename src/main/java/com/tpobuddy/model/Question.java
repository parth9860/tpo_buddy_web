package com.tpobuddy.model;

import java.io.Serializable;

/**
 * Question Model matching Firebase `questions` node and test requirements.
 */
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String category;
    private String topic;
    private String difficulty; // "EASY", "MEDIUM", "HARD"
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private int correctOption; // 0=A, 1=B, 2=C, 3=D or 1,2,3,4
    private String explanation;
    private int marks = 1;

    // AI Test Series Extended Fields
    private String questionType = "MCQ"; // "MCQ", "OUTPUT_PREDICTION", "READING_COMPREHENSION", "CODING_PROBLEM"
    private String passageOrCode = ""; // Passage text for RC or Code block for Output Prediction
    private String section = ""; // Section in multi-section exam
    private int displayOrder = 0;

    public Question() {
    }

    public Question(int id, String category, String topic, String difficulty, String question,
                    String optionA, String optionB, String optionC, String optionD,
                    int correctOption, String explanation) {
        this.id = id;
        this.category = category;
        this.topic = topic;
        this.difficulty = difficulty;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.marks = 1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public int getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(int correctOption) {
        this.correctOption = correctOption;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }

    public String getQuestionType() {
        return questionType != null ? questionType : "MCQ";
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getPassageOrCode() {
        return passageOrCode != null ? passageOrCode : "";
    }

    public void setPassageOrCode(String passageOrCode) {
        this.passageOrCode = passageOrCode;
    }

    public String getSection() {
        return section != null ? section : "";
    }

    public void setSection(String section) {
        this.section = section;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
