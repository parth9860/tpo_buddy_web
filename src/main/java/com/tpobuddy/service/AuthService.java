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
        String college = (String) payload.getOrDefault("college", "SNJB College of Engineering");
        String year = (String) payload.getOrDefault("year", "Final Year");
        String photoUrl = (String) payload.getOrDefault("photoUrl", "");
        String authProvider = (String) payload.getOrDefault("authProvider", "password");

        // 1. Verify token
        JsonObject verifiedUser = firebaseService.verifyIdToken(idToken);
        if (verifiedUser == null) {
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

        // 2. Fetch authoritative profile from Realtime Database / Firestore
        User user = firebaseService.fetchUserProfile(uid);
        if (user == null) {
            // New user registration: ALWAYS assign safe default student role
            user = new User();
            user.setUid(uid);
            user.setNumericId(Math.abs(uid.hashCode() % 90000) + 10000);
            user.setName(!name.isEmpty() ? name : (email.contains("@") ? email.split("@")[0] : "Student"));
            user.setEmail(email);
            String assignedRole = "student";
            String requestedRole = (String) payload.get("role");
            if (requestedRole != null && ("teacher".equalsIgnoreCase(requestedRole) || "admin".equalsIgnoreCase(requestedRole) || "faculty".equalsIgnoreCase(requestedRole))) {
                assignedRole = requestedRole.toLowerCase();
            }
            user.setRole(assignedRole);
            user.setBranch(branch != null && !branch.trim().isEmpty() ? branch : "CSE");
            user.setYear(year != null && !year.trim().isEmpty() ? year : "Final Year");
            user.setCollege(college != null && !college.trim().isEmpty() ? college : "SNJB College of Engineering");
            user.setPhotoUrl(photoUrl);
            user.setAuthProvider(authProvider);
            user.setCreatedAt(System.currentTimeMillis());
            firebaseService.saveUserProfile(user);
        } else {
            // Existing user: preserve role, update non-sensitive fields
            boolean updated = false;
            String reqRole = (String) payload.get("role");
            if (reqRole != null && ("teacher".equalsIgnoreCase(reqRole) || "admin".equalsIgnoreCase(reqRole) || "faculty".equalsIgnoreCase(reqRole))) {
                user.setRole(reqRole.toLowerCase());
                updated = true;
            }
            if ((user.getName() == null || user.getName().isEmpty() || "Student".equals(user.getName())) && name != null && !name.trim().isEmpty()) {
                user.setName(name.trim());
                updated = true;
            }
            if ((user.getCollege() == null || user.getCollege().isEmpty() || "Engineering College".equals(user.getCollege())) && college != null && !college.trim().isEmpty()) {
                user.setCollege(college.trim());
                updated = true;
            }
            if ((user.getYear() == null || user.getYear().isEmpty()) && year != null && !year.trim().isEmpty()) {
                user.setYear(year.trim());
                updated = true;
            }
            if ((user.getBranch() == null || user.getBranch().isEmpty()) && branch != null && !branch.trim().isEmpty()) {
                user.setBranch(branch.trim());
                updated = true;
            }
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
        session.setAttribute("userCollege", user.getCollege());
        session.setAttribute("userYear", user.getYear());
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
