package com.tpobuddy.controller;

import com.tpobuddy.model.ResumeData;
import com.tpobuddy.model.StudentMetrics;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.PersonalizedAiEngine;
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
import java.util.Map;

/**
 * Controller for AI Placement Assistant (AI Buddy chatbot).
 */
@Controller
public class AiChatController {

    private final FirebaseService firebaseService;

    public AiChatController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping({"/ai-buddy", "/ai-buddy.jsp"})
    public String viewAiBuddy(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp";
        }

        StudentMetrics metrics = firebaseService.fetchStudentMetrics(user.getUid());
        model.addAttribute("user", user);
        model.addAttribute("metrics", metrics);
        return "ai-buddy";
    }

    @PostMapping(value = "/api/ai/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Unauthorized");
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(err);
        }

        String userQuery = (String) body.getOrDefault("message", "");

        StudentMetrics metrics = firebaseService.fetchStudentMetrics(user.getUid());
        ResumeData resume = firebaseService.fetchResumeData(user.getUid());

        String aiReply = PersonalizedAiEngine.generateChatResponse(userQuery, user, metrics, resume);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("reply", aiReply);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
