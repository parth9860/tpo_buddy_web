package com.tpobuddy.model;

import java.io.Serializable;

/**
 * Job Opportunity Model matching Firebase Realtime Database `jobs` and Firestore `jobs`.
 */
public class Job implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String company;
    private String location;
    private String packageInfo; // e.g. "₹18.5 LPA"
    private String eligibility;
    private String description;
    private String skills;
    private String deadline;
    private String status = "ACTIVE";
    private boolean applied = false;

    public Job() {
    }

    public Job(String id, String title, String company, String location, String packageInfo, String eligibility, String description, String skills, String deadline, String status) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.packageInfo = packageInfo;
        this.eligibility = eligibility;
        this.description = description;
        this.skills = skills;
        this.deadline = deadline;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(String packageInfo) {
        this.packageInfo = packageInfo;
    }

    public String getEligibility() {
        return eligibility;
    }

    public void setEligibility(String eligibility) {
        this.eligibility = eligibility;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }
}
