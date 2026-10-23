package com.tpobuddy.model;

import java.io.Serializable;

/**
 * Metric data model corresponding to Realtime Database `student_metrics/{uid}`
 * and Firestore `profiles/{uid}` in the Android application.
 */
public class StudentMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    private int javaSkill;
    private int dsaSkill;
    private int sqlSkill;
    private int aptitudeSkill;
    private int technicalScore;
    private int interviewScore;
    private int skillCompletionScore;
    private int resumeScore;
    private int testsCompleted;
    private int streakDays;
    private int readinessScore;

    private String desiredRole = "Software Engineer";
    private String targetSalary = "₹12.0 LPA";
    private String preferredLocation = "Bangalore / Remote";
    private String dataSource = "firebase";

    public StudentMetrics() {
    }

    public int getJavaSkill() {
        return javaSkill;
    }

    public void setJavaSkill(int javaSkill) {
        this.javaSkill = javaSkill;
    }

    public int getDsaSkill() {
        return dsaSkill;
    }

    public void setDsaSkill(int dsaSkill) {
        this.dsaSkill = dsaSkill;
    }

    public int getSqlSkill() {
        return sqlSkill;
    }

    public void setSqlSkill(int sqlSkill) {
        this.sqlSkill = sqlSkill;
    }

    public int getAptitudeSkill() {
        return aptitudeSkill;
    }

    public void setAptitudeSkill(int aptitudeSkill) {
        this.aptitudeSkill = aptitudeSkill;
    }

    public int getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(int technicalScore) {
        this.technicalScore = technicalScore;
    }

    public int getInterviewScore() {
        return interviewScore;
    }

    public void setInterviewScore(int interviewScore) {
        this.interviewScore = interviewScore;
    }

    public int getSkillCompletionScore() {
        return skillCompletionScore;
    }

    public void setSkillCompletionScore(int skillCompletionScore) {
        this.skillCompletionScore = skillCompletionScore;
    }

    public int getResumeScore() {
        return resumeScore;
    }

    public void setResumeScore(int resumeScore) {
        this.resumeScore = resumeScore;
    }

    public int getTestsCompleted() {
        return testsCompleted;
    }

    public void setTestsCompleted(int testsCompleted) {
        this.testsCompleted = testsCompleted;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }

    public int getReadinessScore() {
        return readinessScore;
    }

    public void setReadinessScore(int readinessScore) {
        this.readinessScore = readinessScore;
    }

    public String getDesiredRole() {
        return desiredRole != null ? desiredRole : "Software Engineer";
    }

    public void setDesiredRole(String desiredRole) {
        this.desiredRole = desiredRole;
    }

    public String getTargetSalary() {
        return targetSalary != null ? targetSalary : "₹12.0 LPA";
    }

    public void setTargetSalary(String targetSalary) {
        this.targetSalary = targetSalary;
    }

    public String getPreferredLocation() {
        return preferredLocation != null ? preferredLocation : "Bangalore / Remote";
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
}
