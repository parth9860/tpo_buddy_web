package com.tpobuddy.dto;

import java.io.Serializable;

public class AuthSessionResponse implements Serializable {
    private boolean success;
    private String redirect;
    private String role;
    private String name;
    private String photoUrl;
    private String error;

    public AuthSessionResponse() {}

    public AuthSessionResponse(boolean success, String redirect, String role, String name, String photoUrl) {
        this.success = success;
        this.redirect = redirect;
        this.role = role;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getRedirect() { return redirect; }
    public void setRedirect(String redirect) { this.redirect = redirect; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
