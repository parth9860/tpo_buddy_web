package com.tpobuddy.controller;

import com.tpobuddy.model.StudentMetrics;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for viewing and updating Student Profile & Career Preferences.
 */
@Controller
public class ProfileController {

    private final FirebaseService firebaseService;

    public ProfileController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping({"/profile", "/profile.jsp"})
    public String viewProfile(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp";
        }

        User freshUser = firebaseService.fetchUserProfile(user.getUid());
        if (freshUser != null) {
            user = freshUser;
            session.setAttribute("currentUser", user);
        }

        StudentMetrics metrics = firebaseService.fetchStudentMetrics(user.getUid());

        model.addAttribute("user", user);
        model.addAttribute("metrics", metrics);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "college", required = false) String college,
            @RequestParam(value = "desiredRole", required = false) String desiredRole,
            @RequestParam(value = "targetSalary", required = false) String targetSalary,
            @RequestParam(value = "preferredLocation", required = false) String preferredLocation,
            HttpServletRequest request,
            Model model) {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp";
        }

        if (name != null && !name.trim().isEmpty()) {
            user.setName(name.trim());
        }
        if (college != null && !college.trim().isEmpty()) {
            user.setCollege(college.trim());
        }

        firebaseService.saveUserProfile(user);

        Map<String, Object> updates = new HashMap<>();
        if (desiredRole != null) updates.put("desiredRole", desiredRole.trim());
        if (targetSalary != null) updates.put("targetSalary", targetSalary.trim());
        if (preferredLocation != null) updates.put("preferredLocation", preferredLocation.trim());
        firebaseService.updateStudentMetrics(user.getUid(), updates);

        session.setAttribute("currentUser", user);
        session.setAttribute("userName", user.getName());

        model.addAttribute("successMessage", "Profile preferences successfully synced to Firebase!");
        return viewProfile(request, model);
    }
}
