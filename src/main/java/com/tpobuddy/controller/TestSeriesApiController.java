package com.tpobuddy.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tpobuddy.model.TestSeries;
import com.tpobuddy.model.User;
import com.tpobuddy.service.TestSeriesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing endpoints for Test Series catalogue discovery and management.
 * Strictly separates College Test Series from Platform Test Series.
 */
@RestController
@RequestMapping("/api/test-series")
public class TestSeriesApiController {

    private final TestSeriesService testSeriesService;
    private final Gson gson = new Gson();

    public TestSeriesApiController(TestSeriesService testSeriesService) {
        this.testSeriesService = testSeriesService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTestSeries(@RequestParam(value = "category", required = false) String category) {
        List<TestSeries> list;
        if (category != null && !category.trim().isEmpty()) {
            list = testSeriesService.getSeriesByCategory(category.trim());
        } else {
            list = testSeriesService.getAllSeries();
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("category", category != null ? category.toUpperCase() : "ALL");
        resp.addProperty("count", list.size());
        resp.add("series", gson.toJsonTree(list));

        return ResponseEntity.ok(resp.toString());
    }

    @GetMapping(value = "/{seriesId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSeriesById(@PathVariable("seriesId") String seriesId) {
        TestSeries series = testSeriesService.getSeriesById(seriesId);
        if (series == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"success\":false,\"error\":\"Test Series not found\"}");
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("series", gson.toJsonTree(series));

        return ResponseEntity.ok(resp.toString());
    }

    @PutMapping(value = "/{seriesId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> toggleSeriesStatus(@PathVariable("seriesId") String seriesId,
                                                     @RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null || (!user.isTeacher() && !user.isAdmin())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"success\":false,\"error\":\"Faculty or Administrator permission required\"}");
        }

        boolean active = body.containsKey("active") && Boolean.parseBoolean(String.valueOf(body.get("active")));
        boolean ok = testSeriesService.updateSeriesStatus(seriesId, active, user);

        JsonObject resp = new JsonObject();
        resp.addProperty("success", ok);
        resp.addProperty("seriesId", seriesId);
        resp.addProperty("active", active);

        return ResponseEntity.ok(resp.toString());
    }
}
