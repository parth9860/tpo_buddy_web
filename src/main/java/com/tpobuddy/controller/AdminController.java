package com.tpobuddy.controller;

import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * Controller for Administrator Dashboard.
 */
@Controller
public class AdminController {

    private final FirebaseService firebaseService;

    public AdminController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping({"/admin", "/admin-dashboard.jsp"})
    public String adminDashboard(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null || !user.isAdmin()) {
            return "redirect:/dashboard?error=forbidden";
        }

        Map<String, Integer> stats = firebaseService.fetchSystemStats();
        List<User> usersList = firebaseService.fetchAllUsers();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("usersList", usersList);

        return "admin-dashboard";
    }
}
