package com.tpobuddy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Resume Data model matching Firestore `resumes/{uid}` and Realtime Database `resumes/{uid}`.
 */
public class ResumeData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String skills;
    private String experience;
    private String projects;
    private String resumeText;
    private int atsScore;
    private int grammarScore;
    private String experienceReview;
    private String projectsReview;
    private List<String> detectedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private long updatedAt;

    public ResumeData() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getProjects() {
        return projects;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public int getAtsScore() {
        return atsScore;
    }

    public void setAtsScore(int atsScore) {
        this.atsScore = atsScore;
    }

    public int getGrammarScore() {
        return grammarScore;
    }

    public void setGrammarScore(int grammarScore) {
        this.grammarScore = grammarScore;
    }

    public String getExperienceReview() {
        return experienceReview;
    }

    public void setExperienceReview(String experienceReview) {
        this.experienceReview = experienceReview;
    }

    public String getProjectsReview() {
        return projectsReview;
    }

    public void setProjectsReview(String projectsReview) {
        this.projectsReview = projectsReview;
    }

    public List<String> getDetectedSkills() {
        return detectedSkills;
    }

    public void setDetectedSkills(List<String> detectedSkills) {
        this.detectedSkills = detectedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
