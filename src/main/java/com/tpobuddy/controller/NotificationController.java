package com.tpobuddy.controller;

import com.tpobuddy.service.FirebaseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * REST Controller for Campus Placement Alerts and Notifications.
 * Sourced exclusively from live Firebase Realtime Database events (`events.json`).
 */
@Controller
public class NotificationController {

    private final FirebaseService firebaseService;

    public NotificationController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping(value = "/api/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotifications() {
        List<Map<String, Object>> events = firebaseService.fetchEvents();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("notifications", events);
        response.put("unreadCount", events.size());

        return ResponseEntity.ok(response);
    }
}
