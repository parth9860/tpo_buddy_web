package com.tpobuddy.controller;

import com.tpobuddy.model.Job;
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
 * Controller managing Placement Jobs, Campus Drives, and Applications.
 */
@Controller
public class JobController {

    private final FirebaseService firebaseService;

    public JobController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping({"/jobs", "/jobs.jsp"})
    public String viewJobs(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp";
        }

        List<Job> jobs = firebaseService.fetchJobs(user.getUid());
        model.addAttribute("user", user);
        model.addAttribute("jobs", jobs);
        return "jobs";
    }

    @GetMapping(value = "/api/jobs/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listJobs(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String uid = (user != null) ? user.getUid() : "guest";

        List<Job> jobs = firebaseService.fetchJobs(uid);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("jobs", jobs);
        response.put("total", jobs.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/api/jobs/match", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> matchJobs(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String uid = (user != null) ? user.getUid() : "guest";

        List<Job> jobs = firebaseService.fetchJobs(uid);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("matches", jobs);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/jobs/apply", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyJob(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Unauthorized");
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(err);
        }

        String jobId = (String) body.get("jobId");
        if (jobId == null || jobId.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Missing jobId");
            return ResponseEntity.badRequest().body(err);
        }

        boolean success = firebaseService.applyForJob(user.getUid(), jobId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Application submitted successfully to Firebase!" : "Failed to submit application.");
        return ResponseEntity.ok(response);
    }
}
