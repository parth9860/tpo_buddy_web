package com.tpobuddy.controller;

import com.tpobuddy.model.StudentMetrics;
import com.tpobuddy.model.TestAttempt;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.PlacementReadinessEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller displaying detailed student progress and performance trends.
 */
@Controller
public class ProgressController {

    private final FirebaseService firebaseService;

    public ProgressController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping({"/progress", "/progress.jsp"})
    public String viewProgress(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp";
        }

        String uid = user.getUid();
        StudentMetrics metrics = firebaseService.fetchStudentMetrics(uid);
        PlacementReadinessEngine.ReadinessResult readiness = PlacementReadinessEngine.calculate(metrics);
        List<TestAttempt> testHistory = firebaseService.fetchTestAttempts(uid);

        model.addAttribute("user", user);
        model.addAttribute("metrics", metrics);
        model.addAttribute("readiness", readiness);
        model.addAttribute("testHistory", testHistory);

        return "progress";
    }
}
