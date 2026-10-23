package com.tpobuddy.controller;

import com.tpobuddy.model.Job;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Faculty and TPO Officer Dashboard,
 * Student monitoring audit, and Examination Proctoring Review.
 */
@Controller
public class FacultyController {

    private final FirebaseService firebaseService;

    public FacultyController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping({"/faculty", "/faculty-dashboard.jsp"})
    public String facultyDashboard(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null || (!user.isTeacher() && !user.isAdmin())) {
            return "redirect:/dashboard?error=forbidden";
        }

        List<User> students = firebaseService.fetchAllUsers();
        List<Job> drives = firebaseService.fetchJobs(null);
        List<TestAttempt> proctoringAttempts = firebaseService.fetchAllTestAttempts();

        model.addAttribute("user", user);
        model.addAttribute("students", students);
        model.addAttribute("drives", drives);
        model.addAttribute("proctoringAttempts", proctoringAttempts);
        return "faculty-dashboard";
    }

    @PostMapping(value = "/api/faculty/review-attempt", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reviewAttempt(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null || (!user.isTeacher() && !user.isAdmin())) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Faculty authorization required");
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body(err);
        }

        String attemptId = (String) body.get("attemptId");
        String userId = (String) body.get("userId");
        String status = (String) body.getOrDefault("status", "REVIEWED");
        String notes = (String) body.getOrDefault("notes", "");

        if (attemptId == null || attemptId.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Missing attemptId");
            return ResponseEntity.badRequest().body(err);
        }

        boolean ok = firebaseService.updateTestAttemptReview(attemptId, userId, status, notes);

        Map<String, Object> res = new HashMap<>();
        res.put("success", ok);
        res.put("status", status);
        return ResponseEntity.ok(res);
    }
}
