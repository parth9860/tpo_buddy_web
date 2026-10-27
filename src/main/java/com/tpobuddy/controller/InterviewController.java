package com.tpobuddy.controller;

import com.tpobuddy.model.InterviewSession;
import com.tpobuddy.model.InterviewSession.InterviewQuestion;
import com.tpobuddy.model.User;
import com.tpobuddy.service.InterviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring MVC and REST Controller for AI Mock Interview sessions, evaluations, and reports.
 * Enforces strict student authentication, session isolation, and resilient error handling.
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
    public ResponseEntity<Map<String, Object>> startInterview(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Authentication required. Please log in to start a mock interview.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        String role = (payload != null && payload.get("role") != null) ? String.valueOf(payload.get("role")) : "Full Stack Developer";
        String difficulty = (payload != null && payload.get("difficulty") != null) ? String.valueOf(payload.get("difficulty")) : "Medium";
        int questionCount = 5;
        if (payload != null && payload.containsKey("questionCount")) {
            try {
                questionCount = ((Number) payload.get("questionCount")).intValue();
            } catch (Exception ignored) {}
        }

        InterviewSession interviewSession = interviewService.startSession(
                user.getUid(),
                user.getName(),
                role,
                difficulty,
                questionCount
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", interviewSession.getSessionId());
        response.put("jobRole", interviewSession.getJobRole());
        response.put("difficulty", interviewSession.getDifficulty());
        response.put("totalQuestions", interviewSession.getQuestions().size());
        response.put("durationMinutes", interviewSession.getDurationMinutes());
        response.put("firstQuestion", interviewSession.getQuestions().isEmpty() ? null : interviewSession.getQuestions().get(0));

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/api/interview/active-session", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getActiveSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Authentication required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        InterviewSession active = interviewService.getActiveSession(user.getUid());
        Map<String, Object> response = new HashMap<>();
        if (active == null) {
            response.put("success", true);
            response.put("hasActiveSession", false);
            return ResponseEntity.ok(response);
        }

        int currIdx = active.getCurrentQuestionIndex();
        InterviewQuestion currQ = (currIdx < active.getQuestions().size()) ? active.getQuestions().get(currIdx) : null;

        response.put("success", true);
        response.put("hasActiveSession", true);
        response.put("sessionId", active.getSessionId());
        response.put("jobRole", active.getJobRole());
        response.put("difficulty", active.getDifficulty());
        response.put("totalQuestions", active.getQuestions().size());
        response.put("currentQuestionIndex", currIdx);
        response.put("currentQuestion", currQ);
        response.put("durationMinutes", active.getDurationMinutes());
        response.put("startTime", active.getStartTime());

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/interview/answer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Authentication required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        if (payload == null || !payload.containsKey("sessionId") || !payload.containsKey("questionId")) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Missing sessionId or questionId in submission.");
            return ResponseEntity.badRequest().body(err);
        }

        String sessionId = String.valueOf(payload.get("sessionId"));
        int questionId;
        try {
            questionId = ((Number) payload.get("questionId")).intValue();
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Invalid questionId format.");
            return ResponseEntity.badRequest().body(err);
        }

        String answer = (payload.get("answer") != null) ? String.valueOf(payload.get("answer")) : "";

        Map<String, Object> evalResult = interviewService.evaluateAnswer(user.getUid(), sessionId, questionId, answer);
        if (!Boolean.TRUE.equals(evalResult.get("success"))) {
            if ("Unauthorized access to this interview session.".equals(evalResult.get("error"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(evalResult);
            }
            return ResponseEntity.badRequest().body(evalResult);
        }

        InterviewSession interviewSession = interviewService.getSession(sessionId);
        int nextIdx = (int) evalResult.get("nextQuestionIndex");
        if (interviewSession != null && nextIdx < interviewSession.getQuestions().size()) {
            evalResult.put("nextQuestion", interviewSession.getQuestions().get(nextIdx));
        }

        return ResponseEntity.ok(evalResult);
    }

    @PostMapping(value = "/api/interview/finish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finishInterview(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Authentication required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        if (payload == null || !payload.containsKey("sessionId")) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Missing sessionId in finish request.");
            return ResponseEntity.badRequest().body(err);
        }

        String sessionId = String.valueOf(payload.get("sessionId"));
        InterviewSession finishedSession = interviewService.finishSession(user.getUid(), sessionId);

        Map<String, Object> response = new HashMap<>();
        if (finishedSession == null) {
            response.put("success", false);
            response.put("error", "Session not found or unauthorized.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
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
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Authentication required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        InterviewSession latest = interviewService.getLatestReport(user.getUid());
        Map<String, Object> response = new HashMap<>();
        response.put("success", latest != null);
        response.put("report", latest);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/api/interview/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserHistory(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Authentication required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        List<InterviewSession> history = interviewService.getUserHistory(user.getUid());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("history", history);
        return ResponseEntity.ok(response);
    }
}
