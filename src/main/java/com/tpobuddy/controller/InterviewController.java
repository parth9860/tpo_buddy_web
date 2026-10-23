package com.tpobuddy.controller;

import com.tpobuddy.model.InterviewSession;
import com.tpobuddy.model.User;
import com.tpobuddy.service.InterviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring MVC / REST Controller for AI Mock Interview sessions and Evaluation Reports.
 * Replaces and extends CareerPulse Android InterviewActivity & MockInterviewReportActivity.
 */
@Controller
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @GetMapping({"/interview", "/interview.jsp"})
    public String viewInterview(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp";
        }

        InterviewSession latestReport = interviewService.getLatestReport(user.getUid());
        List<InterviewSession> history = interviewService.getUserHistory(user.getUid());

        model.addAttribute("user", user);
        model.addAttribute("latestReport", latestReport);
        model.addAttribute("history", history);
        return "interview";
    }

    @PostMapping(value = "/api/interview/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startInterview(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String uid = (user != null) ? user.getUid() : "guest_student";
        String name = (user != null) ? user.getName() : "Student";

        String role = payload.getOrDefault("role", "Full Stack Developer");
        InterviewSession interviewSession = interviewService.startSession(uid, name, role);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", interviewSession.getSessionId());
        response.put("jobRole", interviewSession.getJobRole());
        response.put("totalQuestions", interviewSession.getQuestions().size());
        response.put("firstQuestion", interviewSession.getQuestions().isEmpty() ? null : interviewSession.getQuestions().get(0));

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/interview/answer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        int questionId = ((Number) payload.getOrDefault("questionId", 1)).intValue();
        String answer = (String) payload.getOrDefault("answer", "");

        Map<String, Object> evalResult = interviewService.evaluateAnswer(sessionId, questionId, answer);
        if (Boolean.TRUE.equals(evalResult.get("success"))) {
            InterviewSession interviewSession = interviewService.getSession(sessionId);
            int nextIdx = (int) evalResult.get("nextQuestionIndex");
            if (interviewSession != null && nextIdx < interviewSession.getQuestions().size()) {
                evalResult.put("nextQuestion", interviewSession.getQuestions().get(nextIdx));
            }
        }

        return ResponseEntity.ok(evalResult);
    }

    @PostMapping(value = "/api/interview/finish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finishInterview(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        InterviewSession finishedSession = interviewService.finishSession(sessionId);

        Map<String, Object> response = new HashMap<>();
        if (finishedSession == null) {
            response.put("success", false);
            response.put("error", "Session not found");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("report", finishedSession);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/api/interview/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLatestReport(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String uid = (user != null) ? user.getUid() : "guest_student";

        InterviewSession latest = interviewService.getLatestReport(uid);
        Map<String, Object> response = new HashMap<>();
        response.put("success", latest != null);
        response.put("report", latest);
        return ResponseEntity.ok(response);
    }
}
