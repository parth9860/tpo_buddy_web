package com.tpobuddy.controller;

import com.tpobuddy.model.*;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.PersonalizedAiEngine;
import com.tpobuddy.service.PlacementReadinessEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller serving the Student Dashboard.
 * Fast parallel data retrieval with Placement Readiness scoring and AI recommendations.
 */
@Controller
public class DashboardController {

    private final FirebaseService firebaseService;

    public DashboardController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping({"/dashboard", "/student-dashboard.jsp"})
    public String dashboard(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (user == null) {
            return "redirect:/login.jsp";
        }

        if (user.isTeacher()) {
            return "redirect:/faculty";
        } else if (user.isAdmin()) {
            return "redirect:/admin";
        }

        String uid = user.getUid();

        // High performance: Execute all required Firebase operations concurrently
        CompletableFuture<StudentMetrics> metricsFuture = firebaseService.fetchStudentMetricsAsync(uid);
        CompletableFuture<List<TestAttempt>> testsFuture = firebaseService.fetchTestAttemptsAsync(uid);
        CompletableFuture<List<Job>> jobsFuture = firebaseService.fetchJobsAsync(uid);
        CompletableFuture<ResumeData> resumeFuture = firebaseService.fetchResumeDataAsync(uid);

        StudentMetrics metrics;
        try {
            metrics = metricsFuture.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            metrics = new StudentMetrics();
        }

        List<TestAttempt> recentTests;
        try {
            recentTests = testsFuture.get(1500, TimeUnit.MILLISECONDS);
            if (recentTests != null && recentTests.size() > 5) {
                recentTests = recentTests.subList(0, 5);
            }
        } catch (Exception e) {
            recentTests = new ArrayList<>();
        }

        List<Job> jobs;
        try {
            jobs = jobsFuture.get(1500, TimeUnit.MILLISECONDS);
            if (jobs != null && jobs.size() > 3) {
                jobs = jobs.subList(0, 3);
            }
        } catch (Exception e) {
            jobs = new ArrayList<>();
        }

        ResumeData resume;
        try {
            resume = resumeFuture.get(1500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            resume = new ResumeData();
            resume.setUserId(uid);
        }

        // Placement Readiness Calculation
        PlacementReadinessEngine.ReadinessResult readiness = PlacementReadinessEngine.calculate(metrics);
        metrics.setReadinessScore(readiness.overallScore);

        // AI Welcome Banner
        String weakTopic = null;
        if (metrics.getDsaSkill() > 0 && metrics.getDsaSkill() < 70) weakTopic = "DSA & Algorithms";
        else if (metrics.getSqlSkill() > 0 && metrics.getSqlSkill() < 70) weakTopic = "Database & SQL";
        else if (metrics.getAptitudeSkill() > 0 && metrics.getAptitudeSkill() < 70) weakTopic = "Quantitative Aptitude";

        String aiWelcome = PersonalizedAiEngine.generateWelcomeMessage(user.getName(), readiness.overallScore, weakTopic, metrics.getStreakDays());

        model.addAttribute("user", user);
        model.addAttribute("metrics", metrics);
        model.addAttribute("readiness", readiness);
        model.addAttribute("aiWelcome", aiWelcome);
        model.addAttribute("recentTests", recentTests);
        model.addAttribute("jobs", jobs);
        model.addAttribute("resume", resume);

        return "student-dashboard";
    }

    @org.springframework.web.bind.annotation.ResponseBody
    @GetMapping(value = "/api/student/metrics", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> getLiveStudentMetrics(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String uid = (user != null) ? user.getUid() : "guest";

        StudentMetrics metrics = firebaseService.fetchStudentMetrics(uid);
        ResumeData resume = firebaseService.fetchResumeData(uid);
        PlacementReadinessEngine.ReadinessResult readiness = PlacementReadinessEngine.calculate(metrics);
        List<Job> jobs = firebaseService.fetchJobs(uid);

        int resumeScore = (resume != null && resume.getAtsScore() > 0) ? resume.getAtsScore() : metrics.getResumeScore();

        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("success", true);
        res.put("readinessScore", readiness.overallScore);
        res.put("status", readiness.status);
        res.put("testsCompleted", metrics.getTestsCompleted());
        res.put("resumeScore", resumeScore);
        res.put("interviewScore", metrics.getInterviewScore());
        res.put("javaSkill", metrics.getJavaSkill());
        res.put("dsaSkill", metrics.getDsaSkill());
        res.put("sqlSkill", metrics.getSqlSkill());
        res.put("aptitudeSkill", metrics.getAptitudeSkill());
        res.put("streakDays", metrics.getStreakDays());
        res.put("eligibleDrives", jobs != null ? jobs.size() : 0);
        res.put("desiredRole", metrics.getDesiredRole());
        res.put("targetSalary", metrics.getTargetSalary());

        // Radar data points derived live from Firebase metrics
        java.util.Map<String, Integer> radar = new java.util.HashMap<>();
        radar.put("coding", metrics.getDsaSkill() > 0 ? metrics.getDsaSkill() : metrics.getJavaSkill());
        radar.put("aptitude", metrics.getAptitudeSkill());
        radar.put("systemDesign", metrics.getSqlSkill());
        radar.put("resume", resumeScore);
        radar.put("interview", metrics.getInterviewScore());
        res.put("radar", radar);

        return org.springframework.http.ResponseEntity.ok(res);
    }
}
