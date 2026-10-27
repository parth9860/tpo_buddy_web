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
