package com.tpobuddy.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tpobuddy.model.PlacementTest;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.User;
import com.tpobuddy.service.PlacementQuestionBank;
import com.tpobuddy.service.PlacementTestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Controller exposing secured administrative management APIs for the
 * Company Placement Preparation Test Series (SNJB Placement Series).
 * Restricts access to authorized Faculty and Administrators only.
 */
@RestController
@RequestMapping("/api/snjb/admin")
public class SnjbAdminController {

    private final PlacementTestService placementTestService;

    public SnjbAdminController(PlacementTestService placementTestService) {
        this.placementTestService = placementTestService;
    }

    private boolean isAuthorizedAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return false;
        String role = user.getRole();
        return role != null && ("teacher".equalsIgnoreCase(role) || "faculty".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role));
    }

    @GetMapping(value = "/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listTests(HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        List<PlacementTest> tests = placementTestService.getAllTests();
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("tests", new Gson().toJsonTree(tests));
        return ResponseEntity.ok(resp.toString());
    }

    @GetMapping(value = "/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAnalytics(HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        Map<String, Object> analytics = placementTestService.calculateSeriesAnalytics();
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        for (Map.Entry<String, Object> e : analytics.entrySet()) {
            if (e.getValue() instanceof Number) {
                resp.addProperty(e.getKey(), (Number) e.getValue());
            } else if (e.getValue() instanceof List) {
                resp.add(e.getKey(), new Gson().toJsonTree(e.getValue()));
            } else {
                resp.addProperty(e.getKey(), String.valueOf(e.getValue()));
            }
        }
        return ResponseEntity.ok(resp.toString());
    }

    @PostMapping(value = "/tests/save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveTest(@RequestBody String body, HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        try {
            PlacementTest test = new Gson().fromJson(body, PlacementTest.class);
            if (test == null || test.getTitle() == null || test.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Test title is required\"}");
            }

            boolean ok = placementTestService.saveTest(test);
            return ResponseEntity.ok("{\"success\":" + ok + ",\"testId\":\"" + test.getId() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping(value = "/tests/{testId}/duplicate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> duplicateTest(@PathVariable("testId") String testId, HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        PlacementTest cloned = placementTestService.duplicateTest(testId);
        if (cloned != null) {
            return ResponseEntity.ok("{\"success\":true,\"clonedId\":\"" + cloned.getId() + "\"}");
        }
        return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Original test not found\"}");
    }

    @PostMapping(value = "/tests/{testId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateTestStatus(@PathVariable("testId") String testId,
                                                   @RequestBody String body,
                                                   HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            String status = obj.has("status") ? obj.get("status").getAsString() : "PUBLISHED";
            boolean ok = placementTestService.updateTestStatus(testId, status);
            return ResponseEntity.ok("{\"success\":" + ok + ",\"status\":\"" + status + "\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Invalid status payload\"}");
        }
    }

    @DeleteMapping(value = "/tests/{testId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteTest(@PathVariable("testId") String testId, HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        boolean ok = placementTestService.deleteTest(testId);
        return ResponseEntity.ok("{\"success\":" + ok + "}");
    }

    @GetMapping(value = "/questions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listQuestions(HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        List<Question> questions = PlacementQuestionBank.getAllQuestions();
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("questions", new Gson().toJsonTree(questions));
        return ResponseEntity.ok(resp.toString());
    }

    @PostMapping(value = "/questions/save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveQuestion(@RequestBody String body, HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        try {
            Question q = new Gson().fromJson(body, Question.class);
            if (q == null || q.getQuestion() == null || q.getQuestion().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Question statement is required\"}");
            }
            PlacementQuestionBank.addQuestion(q);
            return ResponseEntity.ok("{\"success\":true,\"id\":" + q.getId() + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping(value = "/questions/import-csv", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> importCsvQuestions(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Empty or missing CSV file\"}");
        }

        try (InputStream is = file.getInputStream()) {
            int imported = placementTestService.importQuestionsFromCsv(is);
            return ResponseEntity.ok("{\"success\":true,\"importedCount\":" + imported + ",\"totalQuestionCount\":" + PlacementQuestionBank.getTotalQuestionCount() + "}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping(value = "/questions/import-csv-text", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> importCsvText(@RequestBody String body, HttpServletRequest request) {
        if (!isAuthorizedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\":false,\"error\":\"Unauthorized access\"}");
        }

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String csv = json.has("csvContent") ? json.get("csvContent").getAsString() : "";
            if (csv.isEmpty()) {
                return ResponseEntity.badRequest().body("{\"success\":false,\"error\":\"Empty CSV content\"}");
            }
            int imported = placementTestService.importQuestionsFromCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
            return ResponseEntity.ok("{\"success\":true,\"importedCount\":" + imported + ",\"totalQuestionCount\":" + PlacementQuestionBank.getTotalQuestionCount() + "}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/export-results")
    public void exportResults(HttpServletRequest request, HttpServletResponse response) {
        if (!isAuthorizedAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            String csvData = placementTestService.generateAttemptsCsv();
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"snjb_placement_test_results.csv\"");
            response.getOutputStream().write(csvData.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
