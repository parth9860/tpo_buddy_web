package com.tpobuddy.model;

import java.io.Serializable;

/**
 * User model representing the authenticated Firebase user profile
 * Compatible with Android's UserModel / User and Realtime Database `users/{uid}`.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uid;
    private int numericId;
    private String name;
    private String email;
    private String role; // "student", "teacher", "admin"
    private String branch; // department
    private String year;
    private String college;
    private long createdAt;
    private String photoUrl;
    private String authProvider; // "password", "google", "facebook", "apple"

    public User() {
    }

    public User(String uid, int numericId, String name, String email, String role, String branch, String year, String college, long createdAt) {
        this.uid = uid;
        this.numericId = numericId;
        this.name = name;
        this.email = email;
        this.role = role != null ? role.trim().toLowerCase() : "student";
        this.branch = branch;
        this.year = year;
        this.college = college;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getNumericId() {
        return numericId;
    }

    public void setNumericId(int numericId) {
        this.numericId = numericId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role.trim().toLowerCase() : "student";
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getAuthProvider() {
        return authProvider != null ? authProvider : "password";
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public boolean isStudent() {
        return "student".equalsIgnoreCase(role);
    }

    public boolean isTeacher() {
        return "teacher".equalsIgnoreCase(role) || "faculty".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
