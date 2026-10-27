package com.tpobuddy.controller;

import com.tpobuddy.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for public marketing and authentication entry views.
 */
@Controller
public class PageController {

    @GetMapping({"/", "/index.jsp"})
    public String index() {
        return "index";
    }

    @GetMapping({"/login", "/login.jsp"})
    public String login(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user != null) {
            if (user.isTeacher()) return "redirect:/faculty";
            if (user.isAdmin()) return "redirect:/admin";
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping({"/register", "/register.jsp"})
    public String register(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @GetMapping({"/forgot-password", "/forgot-password.jsp"})
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping({"/react-portal", "/react-portal.jsp", "/portal"})
    public String reactPortal(HttpServletRequest request) {
        return "redirect:/dashboard";
    }

    @GetMapping("/react-app")
    public String reactApp() {
        return "forward:/react-dist/index.html";
    }
}
