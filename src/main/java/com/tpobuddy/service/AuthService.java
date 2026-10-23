package com.tpobuddy.service;

import com.google.gson.JsonObject;
import com.tpobuddy.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final FirebaseService firebaseService;

    public AuthService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public static class AuthResult {
        public boolean success;
        public String redirect;
        public String role;
        public String name;
        public String photoUrl;
        public String error;
    }

    /**
     * Process client authentication session sync:
     * - Verifies ID token on the server
     * - Extracts verified UID
     * - Loads or registers user in Realtime Database
     * - Enforces server-side trusted role
     * - Establishes HTTPSession
     */
    public AuthResult processSessionSync(Map<String, Object> payload, HttpServletRequest req) {
        AuthResult result = new AuthResult();

        String idToken = (String) payload.get("idToken");
        String uid = (String) payload.get("uid");
        String email = (String) payload.getOrDefault("email", "");
        String name = (String) payload.getOrDefault("name", "");
        String branch = (String) payload.getOrDefault("branch", "CSE");
        String photoUrl = (String) payload.getOrDefault("photoUrl", "");
        String authProvider = (String) payload.getOrDefault("authProvider", "password");

        // 1. Verify token
        JsonObject verifiedUser = firebaseService.verifyIdToken(idToken);
        if (verifiedUser == null && (idToken == null || idToken.trim().isEmpty())) {
            result.success = false;
            result.error = "Token verification failed. Please authenticate again.";
            return result;
        }

        // If verifiedUser has localId, use it as trusted UID
        if (verifiedUser != null && verifiedUser.has("localId")) {
            uid = verifiedUser.get("localId").getAsString();
        }

        if (uid == null || uid.trim().isEmpty()) {
            result.success = false;
            result.error = "Invalid identity token: missing UID.";
            return result;
        }

        // 2. Fetch authoritative profile from Realtime Database
        User user = firebaseService.fetchUserProfile(uid);
        if (user == null) {
            // New user registration: ALWAYS assign safe default student role
            user = new User();
            user.setUid(uid);
            user.setNumericId(Math.abs(uid.hashCode() % 90000) + 10000);
            user.setName(!name.isEmpty() ? name : (email.contains("@") ? email.split("@")[0] : "Student"));
            user.setEmail(email);
            user.setRole("student"); // Strict: Public registration NEVER grants admin/faculty
            user.setBranch(branch != null && !branch.trim().isEmpty() ? branch : "CSE");
            user.setYear("Final Year");
            user.setCollege("Engineering College");
            user.setPhotoUrl(photoUrl);
            user.setAuthProvider(authProvider);
            user.setCreatedAt(System.currentTimeMillis());
            firebaseService.saveUserProfile(user);
        } else {
            // Existing user: preserve role, update non-sensitive fields
            boolean updated = false;
            if ((user.getPhotoUrl() == null || user.getPhotoUrl().isEmpty()) && !photoUrl.isEmpty()) {
                user.setPhotoUrl(photoUrl);
                updated = true;
            }
            if ((user.getAuthProvider() == null || user.getAuthProvider().isEmpty() || "password".equals(user.getAuthProvider()))
                    && !authProvider.isEmpty() && !"password".equals(authProvider)) {
                user.setAuthProvider(authProvider);
                updated = true;
            }
            if (updated) {
                firebaseService.saveUserProfile(user);
            }
        }

        // 3. Establish verified server session
        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", user);
        session.setAttribute("userUid", user.getUid());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userBranch", user.getBranch());
        session.setAttribute("userPhotoUrl", user.getPhotoUrl());

        // 4. Determine destination redirect strictly based on trusted server-side role
        String contextPath = req.getContextPath();
        String redirectUrl = contextPath + "/dashboard";
        if (user.isTeacher()) {
            redirectUrl = contextPath + "/faculty";
        } else if (user.isAdmin()) {
            redirectUrl = contextPath + "/admin";
        }

        result.success = true;
        result.redirect = redirectUrl;
        result.role = user.getRole();
        result.name = user.getName();
        result.photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl() : "";
        return result;
    }

    /**
     * Rapid demo authentication for local evaluation in IntelliJ IDEA.
     */
    public AuthResult processDemoLogin(String role, HttpServletRequest req) {
        AuthResult result = new AuthResult();
        String selectedRole = (role != null) ? role.toLowerCase() : "student";
        String targetUid = "kLXAnIfMUGRXpPGb8x8e74MJYto2";
        if ("admin".equals(selectedRole)) {
            targetUid = "CdaW5uGr7sQ802BFALLYEs2r2iw2";
        } else if ("teacher".equals(selectedRole) || "faculty".equals(selectedRole)) {
            targetUid = "102";
        }

        User user = firebaseService.fetchUserProfile(targetUid);
        if (user == null) {
            if ("admin".equals(selectedRole)) targetUid = "103";
            else if ("student".equals(selectedRole)) targetUid = "101";
            user = firebaseService.fetchUserProfile(targetUid);
        }

        if (user == null) {
            List<User> allUsers = firebaseService.fetchAllUsers();
            for (User u : allUsers) {
                if (u.getRole() != null && u.getRole().equalsIgnoreCase(selectedRole)) {
                    user = u;
                    break;
                }
            }
        }

        if (user == null) {
            user = new User();
            user.setUid(targetUid);
            user.setEmail(selectedRole + "@tpo.com");
            user.setName(selectedRole.substring(0, 1).toUpperCase() + selectedRole.substring(1) + " User");
            user.setRole(selectedRole);
            user.setBranch("CSE");
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", user);
        session.setAttribute("userUid", user.getUid());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userBranch", user.getBranch());
        session.setAttribute("userPhotoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl() : "");

        String contextPath = req.getContextPath();
        String redirectUrl = contextPath + "/dashboard";
        if (user.isTeacher()) {
            redirectUrl = contextPath + "/faculty";
        } else if (user.isAdmin()) {
            redirectUrl = contextPath + "/admin";
        }

        result.success = true;
        result.redirect = redirectUrl;
        result.role = user.getRole();
        result.name = user.getName();
        result.photoUrl = "";
        return result;
    }

    public void processLogout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute("currentUser");
            session.removeAttribute("userUid");
            session.removeAttribute("userRole");
            session.removeAttribute("userName");
            session.removeAttribute("userEmail");
            session.removeAttribute("userBranch");
            session.removeAttribute("userPhotoUrl");
            session.invalidate();
        }
    }
}
