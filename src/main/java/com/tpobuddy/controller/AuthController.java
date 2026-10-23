package com.tpobuddy.controller;

import com.tpobuddy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC / REST Controller for client authentication sessions,
 * Firebase token verification, and logout.
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Synchronize verified client Firebase ID Token with server-side HTTPSession.
     * Prevents duplicate logins and derives identity strictly from the verified token.
     */
    @PostMapping(value = "/api/auth/session", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> syncSession(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        AuthService.AuthResult result = authService.processSessionSync(payload, request);

        Map<String, Object> response = new HashMap<>();
        if (!result.success) {
            response.put("success", false);
            response.put("error", result.error != null ? result.error : "Authentication failed");
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(response);
        }

        response.put("success", true);
        response.put("redirect", result.redirect);
        response.put("role", result.role);
        response.put("name", result.name);
        response.put("photoUrl", result.photoUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * One-click demo login for rapid testing in IntelliJ IDEA without external setup.
     */
    @RequestMapping(value = "/api/auth/demo", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> demoLogin(@RequestParam(value = "role", defaultValue = "student") String role,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) throws IOException {
        AuthService.AuthResult result = authService.processDemoLogin(role, request);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("redirect", result.redirect);
        body.put("role", result.role);
        body.put("name", result.name);

        // If accessed directly via browser GET link, redirect immediately
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            response.sendRedirect(result.redirect);
            return null;
        }

        return ResponseEntity.ok(body);
    }

    /**
     * Handles logout for both AJAX and browser navigation.
     */
    @RequestMapping(value = {"/logout", "/api/auth/logout"}, method = {RequestMethod.GET, RequestMethod.POST})
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authService.processLogout(request);

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":true,\"redirect\":\"" + request.getContextPath() + "/login.jsp?logout=true\"}");
        } else {
            response.sendRedirect(request.getContextPath() + "/login.jsp?logout=true");
        }
    }
}
