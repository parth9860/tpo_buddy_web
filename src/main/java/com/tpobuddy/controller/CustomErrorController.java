package com.tpobuddy.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Authoritative Custom Error Controller for TPO Buddy.
 * Replaces Spring Boot default Whitelabel Error Page with:
 * 1. Clean JSON error payloads for all /api/** and AJAX requests.
 * 2. Premium branded JSP error page for student browser navigation.
 * 3. Comprehensive server logging with full stack trace without leaking internals to clients.
 */
@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageObj = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exceptionObj = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object uriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusObj instanceof Integer) {
            statusCode = (Integer) statusObj;
        }

        String requestUri = uriObj != null ? uriObj.toString() : request.getRequestURI();
        String errorMessage = messageObj != null ? messageObj.toString() : null;

        if (exceptionObj instanceof Throwable) {
            Throwable ex = (Throwable) exceptionObj;
            logger.error("[SYSTEM-ERROR] URI: {} | Status: {} | Exception: {}", requestUri, statusCode, ex.getMessage(), ex);
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = ex.getMessage();
            }
        } else {
            logger.warn("[SYSTEM-ERROR] URI: {} | Status: {} | Message: {}", requestUri, statusCode, errorMessage);
        }

        // Determine if request expects JSON
        String acceptHeader = request.getHeader("Accept");
        boolean isApi = (requestUri != null && requestUri.startsWith("/api/")) ||
                (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));

        if (isApi) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("status", statusCode);
            resp.put("error", statusCode == 404 ? "Endpoint not found" :
                    (statusCode == 403 ? "Forbidden" : "Internal server error"));
            return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(resp);
        }

        model.addAttribute("statusCode", statusCode);
        if (statusCode == 404) {
            model.addAttribute("errorTitle", "Assessment or Page Not Found");
            model.addAttribute("errorMessage", "The requested test, attempt, or resource does not exist.");
        } else if (statusCode == 403) {
            model.addAttribute("errorTitle", "Access Denied");
            model.addAttribute("errorMessage", "You are not authorized to view this test attempt or result.");
        } else {
            model.addAttribute("errorTitle", "Unexpected Server Error");
            model.addAttribute("errorMessage", "A server error occurred while processing your request. Any completed test results have been safely recorded.");
        }

        return "error";
    }
}
