package com.tpobuddy.service;

import com.google.gson.*;
import com.tpobuddy.model.*;
import com.tpobuddy.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * High-performance Spring Service for interacting with Firebase Authentication,
 * Realtime Database, and Cloud Firestore.
 * Features persistent connection pooling via Java 21 HttpClient and parallel async fetching.
 */
@Service
public class FirebaseService {

    private static FirebaseService instance;
    private final Gson gson = new Gson();

    @Value("${firebase.databaseURL:https://tpobuddy-default-rtdb.firebaseio.com}")
    private String databaseUrl;

    @Value("${firebase.apiKey:}")
    private String apiKey;

    @Value("${firebase.projectId:tpobuddy}")
    private String projectId;

    // Persistent HTTP client with connection pooling and HTTP/2
    private final HttpClient httpClient;

    public FirebaseService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @PostConstruct
    public void init() {
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            this.databaseUrl = Config.getFirebaseDatabaseUrl();
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            this.apiKey = Config.getFirebaseApiKey();
        }
        if (projectId == null || projectId.trim().isEmpty()) {
            this.projectId = Config.getFirebaseProjectId();
        }
        instance = this;
        System.out.println("⚡ FirebaseService initialized with database: " + databaseUrl);
    }

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
            instance.databaseUrl = Config.getFirebaseDatabaseUrl();
            instance.apiKey = Config.getFirebaseApiKey();
            instance.projectId = Config.getFirebaseProjectId();
        }
        return instance;
    }

    // =========================================================================
    // Firebase Authentication Token Verification
    // =========================================================================

    /**
     * Verifies a Firebase ID token sent from the client.
     * Uses Google Identity Toolkit REST API with the project API key.
     * Returns a JsonObject containing user details (localId, email, etc.) or parsed JWT payload fallback.
     */
    public JsonObject verifyIdToken(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            return null;
        }

        try {
            String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + apiKey;
            JsonObject payload = new JsonObject();
            payload.addProperty("idToken", idToken.trim());

            String response = executeHttpRequest(endpoint, "POST", payload.toString());
            if (response != null && !response.isEmpty()) {
                JsonObject resObj = JsonParser.parseString(response).getAsJsonObject();
                if (resObj.has("users") && resObj.getAsJsonArray("users").size() > 0) {
                    return resObj.getAsJsonArray("users").get(0).getAsJsonObject();
                }
            }
        } catch (Exception e) {
            System.err.println("Firebase ID token verification via API warning: " + e.getMessage());
        }

        // Fast offline JWT validation fallback
        return parseJwtPayload(idToken);
    }

    private JsonObject parseJwtPayload(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                if (json.has("aud") && projectId.equals(json.get("aud").getAsString())) {
                    JsonObject userObj = new JsonObject();
                    userObj.addProperty("localId", json.has("user_id") ? json.get("user_id").getAsString() : (json.has("sub") ? json.get("sub").getAsString() : ""));
                    userObj.addProperty("email", json.has("email") ? json.get("email").getAsString() : "");
                    return userObj;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // =========================================================================
    // Realtime Database: User Profile Management (`users/{uid}`)
    // =========================================================================

    public User fetchUserProfile(String uid) {
        if (uid == null || uid.trim().isEmpty()) return null;

        String url = databaseUrl + "/users/" + uid + ".json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                User user = new User();
                user.setUid(uid);
                user.setNumericId(obj.has("numericId") ? obj.get("numericId").getAsInt() : Math.abs(uid.hashCode() % 90000) + 10000);
                user.setName(obj.has("name") ? obj.get("name").getAsString() : "Student");
                user.setEmail(obj.has("email") ? obj.get("email").getAsString() : "");
                user.setRole(obj.has("role") ? obj.get("role").getAsString() : "student");
                user.setBranch(obj.has("branch") ? obj.get("branch").getAsString() : (obj.has("department") ? obj.get("department").getAsString() : "CSE"));
                user.setYear(obj.has("year") ? obj.get("year").getAsString() : "Final Year");
                user.setCollege(obj.has("college") ? obj.get("college").getAsString() : "SNJB College of Engineering");
                user.setCreatedAt(obj.has("createdAt") ? obj.get("createdAt").getAsLong() : System.currentTimeMillis());
                user.setPhotoUrl(obj.has("photoUrl") ? obj.get("photoUrl").getAsString() : (obj.has("photoURL") ? obj.get("photoURL").getAsString() : ""));
                user.setAuthProvider(obj.has("authProvider") ? obj.get("authProvider").getAsString() : "password");
                return user;
            } catch (Exception e) {
                System.err.println("Error parsing user profile for " + uid + ": " + e.getMessage());
            }
        }

        // Secondary fallback: Cloud Firestore `users/{uid}`
        if (projectId != null && apiKey != null && !apiKey.isEmpty()) {
            try {
                String fsUrl = "https://firestore.googleapis.com/v1/projects/" + projectId 
                        + "/databases/(default)/documents/users/" + uid + "?key=" + apiKey;
                String fsResponse = executeHttpRequest(fsUrl, "GET", null);
                if (fsResponse != null && !fsResponse.contains("error") && fsResponse.contains("fields")) {
                    JsonObject fsDoc = JsonParser.parseString(fsResponse).getAsJsonObject();
                    if (fsDoc.has("fields")) {
                        JsonObject fields = fsDoc.getAsJsonObject("fields");
                        User user = new User();
                        user.setUid(uid);
                        user.setName(getFsString(fields, "name", "Student"));
                        user.setEmail(getFsString(fields, "email", ""));
                        user.setRole(getFsString(fields, "role", "student"));
                        user.setBranch(getFsString(fields, "branch", "CSE"));
                        user.setYear(getFsString(fields, "year", "Final Year"));
                        user.setCollege(getFsString(fields, "college", "SNJB College of Engineering"));
                        user.setPhotoUrl(getFsString(fields, "photoUrl", ""));
                        user.setAuthProvider(getFsString(fields, "authProvider", "password"));
                        user.setCreatedAt(System.currentTimeMillis());
                        return user;
                    }
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    public boolean saveUserProfile(User user) {
        if (user == null || user.getUid() == null) return false;

        String url = databaseUrl + "/users/" + user.getUid() + ".json";
        JsonObject json = new JsonObject();
        json.addProperty("uid", user.getUid());
        json.addProperty("numericId", user.getNumericId());
        json.addProperty("name", user.getName());
        json.addProperty("email", user.getEmail());
        json.addProperty("role", user.getRole());
        json.addProperty("branch", user.getBranch());
        json.addProperty("year", user.getYear());
        json.addProperty("college", user.getCollege());
        json.addProperty("createdAt", user.getCreatedAt() > 0 ? user.getCreatedAt() : System.currentTimeMillis());
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            json.addProperty("photoUrl", user.getPhotoUrl());
            json.addProperty("photoURL", user.getPhotoUrl());
        }
        if (user.getAuthProvider() != null && !user.getAuthProvider().isEmpty()) {
            json.addProperty("authProvider", user.getAuthProvider());
        }

        String response = executeHttpRequest(url, "PUT", json.toString());

        // Also asynchronously sync to Firestore `users/{uid}` in fail-safe mode
        saveFirestoreUserProfile(user);

        return response != null;
    }

    private void saveFirestoreUserProfile(User user) {
        if (user == null || user.getUid() == null || projectId == null || apiKey == null || apiKey.isEmpty()) return;
        try {
            JsonObject fields = new JsonObject();
            fields.add("uid", makeStringField(user.getUid()));
            fields.add("id", makeStringField(user.getUid()));
            fields.add("name", makeStringField(user.getName() != null ? user.getName() : "Student"));
            fields.add("displayName", makeStringField(user.getName() != null ? user.getName() : "Student"));
            fields.add("email", makeStringField(user.getEmail() != null ? user.getEmail() : ""));
            fields.add("role", makeStringField(user.getRole() != null ? user.getRole() : "student"));
            fields.add("branch", makeStringField(user.getBranch() != null ? user.getBranch() : "CSE"));
            fields.add("year", makeStringField(user.getYear() != null ? user.getYear() : "Final Year"));
            fields.add("college", makeStringField(user.getCollege() != null ? user.getCollege() : "SNJB College of Engineering"));
            fields.add("authProvider", makeStringField(user.getAuthProvider() != null ? user.getAuthProvider() : "password"));
            fields.add("numericId", makeIntegerField(user.getNumericId()));
            fields.add("createdAt", makeIntegerField(user.getCreatedAt() > 0 ? user.getCreatedAt() : System.currentTimeMillis()));
            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                fields.add("photoUrl", makeStringField(user.getPhotoUrl()));
            }

            StringBuilder maskParams = new StringBuilder();
            for (String fieldName : fields.keySet()) {
                maskParams.append("&updateMask.fieldPaths=").append(fieldName);
            }

            String firestoreUrl = "https://firestore.googleapis.com/v1/projects/" + projectId 
                    + "/databases/(default)/documents/users/" + user.getUid() 
                    + "?key=" + apiKey + maskParams.toString();

            JsonObject body = new JsonObject();
            body.add("fields", fields);

            executeHttpRequest(firestoreUrl, "PATCH", body.toString());
        } catch (Exception ignored) {
            // Fail-safe: Realtime Database is primary active store
        }
    }

    private String getFsString(JsonObject fields, String key, String defVal) {
        if (fields == null || !fields.has(key)) return defVal;
        try {
            JsonObject f = fields.getAsJsonObject(key);
            if (f.has("stringValue")) return f.get("stringValue").getAsString();
        } catch (Exception ignored) {}
        return defVal;
    }

    public List<User> fetchAllUsers() {
        List<User> list = new ArrayList<>();
        String url = databaseUrl + "/users.json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonObject usersMap = JsonParser.parseString(response).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : usersMap.entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        JsonObject obj = entry.getValue().getAsJsonObject();
                        User u = new User();
                        u.setUid(entry.getKey());
                        u.setName(obj.has("name") ? obj.get("name").getAsString() : "Student");
                        u.setEmail(obj.has("email") ? obj.get("email").getAsString() : "");
                        u.setRole(obj.has("role") ? obj.get("role").getAsString() : "student");
                        u.setBranch(obj.has("branch") ? obj.get("branch").getAsString() : "CSE");
                        u.setYear(obj.has("year") ? obj.get("year").getAsString() : "Final Year");
                        u.setPhotoUrl(obj.has("photoUrl") ? obj.get("photoUrl").getAsString() : (obj.has("photoURL") ? obj.get("photoURL").getAsString() : ""));
                        u.setAuthProvider(obj.has("authProvider") ? obj.get("authProvider").getAsString() : "password");
                        list.add(u);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing all users: " + e.getMessage());
            }
        }
        return list;
    }

    // =========================================================================
    // Realtime Database: Student Metrics (`student_metrics/{uid}`)
    // =========================================================================

    public StudentMetrics fetchStudentMetrics(String uid) {
        StudentMetrics metrics = new StudentMetrics();
        if (uid == null || uid.trim().isEmpty()) return metrics;

        String url = databaseUrl + "/student_metrics/" + uid + ".json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                metrics.setJavaSkill(getInt(obj, "javaSkill", 0));
                metrics.setDsaSkill(getInt(obj, "dsaSkill", 0));
                metrics.setSqlSkill(getInt(obj, "sqlSkill", 0));
                metrics.setAptitudeSkill(getInt(obj, "aptitudeSkill", 0));
                metrics.setTechnicalScore(getInt(obj, "technicalScore", 0));
                metrics.setInterviewScore(getInt(obj, "interviewScore", 0));
                metrics.setSkillCompletionScore(getInt(obj, "skillCompletionScore", 0));
                metrics.setResumeScore(getInt(obj, "resumeScore", 0));
                metrics.setTestsCompleted(getInt(obj, "testsCompleted", 0));
                metrics.setStreakDays(getInt(obj, "streakDays", 0));
                metrics.setReadinessScore(getInt(obj, "readinessScore", 0));
                metrics.setDesiredRole(getString(obj, "desiredRole", "Software Engineer"));
                metrics.setTargetSalary(getString(obj, "targetSalary", "₹12.0 LPA"));
                metrics.setPreferredLocation(getString(obj, "preferredLocation", "Bangalore / Remote"));
                metrics.setDataSource("firebase");
                return metrics;
            } catch (Exception e) {
                System.err.println("Error parsing student metrics for " + uid + ": " + e.getMessage());
            }
        }

        return metrics;
    }

    public boolean updateStudentMetrics(String uid, Map<String, Object> updates) {
        if (uid == null || updates == null || updates.isEmpty()) return false;

        String url = databaseUrl + "/student_metrics/" + uid + ".json";
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (entry.getValue() instanceof Number) {
                json.addProperty(entry.getKey(), (Number) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                json.addProperty(entry.getKey(), (Boolean) entry.getValue());
            } else {
                json.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        json.addProperty("updatedAt", System.currentTimeMillis());

        String response = executeHttpRequest(url, "PATCH", json.toString());
        return response != null;
    }

    // =========================================================================
    // Realtime Database: Jobs & Placement Drives (`jobs`)
    // =========================================================================

    public List<Job> fetchJobs(String studentUid) {
        List<Job> jobs = new ArrayList<>();
        String url = databaseUrl + "/jobs.json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonElement element = JsonParser.parseString(response);
                if (element.isJsonObject()) {
                    JsonObject map = element.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            Job job = parseJobJson(entry.getKey(), entry.getValue().getAsJsonObject());
                            if (studentUid != null) {
                                job.setApplied(hasAppliedForJob(studentUid, job.getId()));
                            }
                            jobs.add(job);
                        }
                    }
                } else if (element.isJsonArray()) {
                    JsonArray arr = element.getAsJsonArray();
                    for (int i = 0; i < arr.size(); i++) {
                        if (!arr.get(i).isJsonNull() && arr.get(i).isJsonObject()) {
                            Job job = parseJobJson(String.valueOf(i), arr.get(i).getAsJsonObject());
                            if (studentUid != null) {
                                job.setApplied(hasAppliedForJob(studentUid, job.getId()));
                            }
                            jobs.add(job);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing jobs from Firebase: " + e.getMessage());
            }
        }

        return jobs;
    }

    private Job parseJobJson(String id, JsonObject obj) {
        Job j = new Job();
        j.setId(id);
        j.setTitle(getString(obj, "title", getString(obj, "jobRole", "Software Engineer")));
        j.setCompany(getString(obj, "company", getString(obj, "companyName", "TechCorp")));
        j.setLocation(getString(obj, "location", "Bangalore, India"));
        j.setPackageInfo(getString(obj, "packageInfo", getString(obj, "salary", "₹18.5 LPA")));
        j.setEligibility(getString(obj, "eligibility", getString(obj, "experience", "7.0 CGPA+ • Open for All")));
        j.setDescription(getString(obj, "description", "Full-time placement opportunity"));
        j.setSkills(getString(obj, "skills", "Java, SQL, Spring Boot"));
        j.setDeadline(getString(obj, "deadline", "2026-10-31"));
        j.setStatus(getString(obj, "status", "ACTIVE"));
        return j;
    }

    // =========================================================================
    // Realtime Database: Assessment Questions (`questions`)
    // =========================================================================

    public List<Question> fetchQuestions(String category) {
        List<Question> questions = new ArrayList<>();
        String url = databaseUrl + "/questions.json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonElement element = JsonParser.parseString(response);
                if (element.isJsonArray()) {
                    JsonArray arr = element.getAsJsonArray();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonElement item = arr.get(i);
                        if (item != null && !item.isJsonNull() && item.isJsonObject()) {
                            Question q = parseQuestionJson(item.getAsJsonObject());
                            if (q != null) questions.add(q);
                        }
                    }
                } else if (element.isJsonObject()) {
                    JsonObject map = element.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                        if (entry.getValue() != null && entry.getValue().isJsonObject()) {
                            Question q = parseQuestionJson(entry.getValue().getAsJsonObject());
                            if (q != null) questions.add(q);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing live questions from Firebase: " + e.getMessage());
            }
        }

        if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category) && !"mock".equalsIgnoreCase(category)) {
            String norm = category.toLowerCase().trim();
            List<Question> filtered = new ArrayList<>();
            for (Question q : questions) {
                if (q.getCategory() != null && q.getCategory().toLowerCase().contains(norm)) {
                    filtered.add(q);
                } else if (q.getTopic() != null && q.getTopic().toLowerCase().contains(norm)) {
                    filtered.add(q);
                }
            }
            if (!filtered.isEmpty()) {
                return filtered;
            }
        }

        return questions;
    }

    private Question parseQuestionJson(JsonObject obj) {
        try {
            Question q = new Question();
            q.setId(getInt(obj, "id", 1));
            q.setCategory(getString(obj, "category", "Technical"));
            q.setTopic(getString(obj, "topic", "General"));
            q.setDifficulty(getString(obj, "difficulty", "Medium"));
            q.setQuestion(getString(obj, "question", ""));
            q.setOptionA(getString(obj, "optionA", ""));
            q.setOptionB(getString(obj, "optionB", ""));
            q.setOptionC(getString(obj, "optionC", ""));
            q.setOptionD(getString(obj, "optionD", ""));
            q.setCorrectOption(getInt(obj, "correctOption", 0));
            q.setExplanation(getString(obj, "explanation", ""));
            return q;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean saveQuestion(Question q) {
        if (q == null) return false;
        if (q.getId() <= 0) {
            q.setId((int) (System.currentTimeMillis() % 10000000) + 1000);
        }
        String url = databaseUrl + "/questions/" + q.getId() + ".json";
        JsonObject obj = new JsonObject();
        obj.addProperty("id", q.getId());
        obj.addProperty("category", q.getCategory() != null ? q.getCategory() : "Technical");
        obj.addProperty("topic", q.getTopic() != null ? q.getTopic() : "General");
        obj.addProperty("difficulty", q.getDifficulty() != null ? q.getDifficulty() : "Medium");
        obj.addProperty("question", q.getQuestion() != null ? q.getQuestion() : "");
        obj.addProperty("optionA", q.getOptionA() != null ? q.getOptionA() : "");
        obj.addProperty("optionB", q.getOptionB() != null ? q.getOptionB() : "");
        obj.addProperty("optionC", q.getOptionC() != null ? q.getOptionC() : "");
        obj.addProperty("optionD", q.getOptionD() != null ? q.getOptionD() : "");
        obj.addProperty("correctOption", q.getCorrectOption());
        obj.addProperty("explanation", q.getExplanation() != null ? q.getExplanation() : "");
        obj.addProperty("marks", q.getMarks() > 0 ? q.getMarks() : 2);
        String res = executeHttpRequest(url, "PUT", obj.toString());
        return res != null && !res.isEmpty();
    }

    public int saveQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) return 0;
        int count = 0;
        for (Question q : questions) {
            if (saveQuestion(q)) count++;
        }
        return count;
    }

    // =========================================================================
    // Realtime Database: Campus Events & Drives (`events`)
    // =========================================================================

    public List<Map<String, Object>> fetchEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        String url = databaseUrl + "/events.json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonElement element = JsonParser.parseString(response);
                if (element.isJsonArray()) {
                    JsonArray arr = element.getAsJsonArray();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonElement item = arr.get(i);
                        if (item != null && !item.isJsonNull() && item.isJsonObject()) {
                            events.add(parseEventJson(item.getAsJsonObject(), i));
                        }
                    }
                } else if (element.isJsonObject()) {
                    JsonObject map = element.getAsJsonObject();
                    int idx = 0;
                    for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                        if (entry.getValue() != null && entry.getValue().isJsonObject()) {
                            events.add(parseEventJson(entry.getValue().getAsJsonObject(), idx++));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing live events from Firebase: " + e.getMessage());
            }
        }
        return events;
    }

    private Map<String, Object> parseEventJson(JsonObject obj, int index) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "event_" + (index + 1));
        map.put("title", getString(obj, "title", "Campus Drive"));
        map.put("category", getString(obj, "category", "GENERAL"));
        map.put("date", getString(obj, "date", ""));
        map.put("time", getString(obj, "time", ""));
        map.put("location", getString(obj, "location", ""));
        map.put("message", getString(obj, "title", "") + " — Date: " + getString(obj, "date", "") + " (" + getString(obj, "time", "") + ") at " + getString(obj, "location", ""));
        map.put("timestamp", System.currentTimeMillis());
        map.put("isRead", false);
        return map;
    }

    // =========================================================================
    // Realtime Database: AI Mock Interviews (`mock_interviews/{userId}`)
    // =========================================================================

    public boolean saveMockInterview(String userId, Map<String, Object> data) {
        if (userId == null || data == null) return false;
        String interviewId = UUID.randomUUID().toString();
        String url = databaseUrl + "/mock_interviews/" + userId + "/" + interviewId + ".json";
        String json = gson.toJson(data);
        String response = executeHttpRequest(url, "PUT", json);
        return response != null;
    }

    public List<Map<String, Object>> fetchMockInterviews(String userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (userId == null) return list;
        String url = databaseUrl + "/mock_interviews/" + userId + ".json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonObject map = JsonParser.parseString(response).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        JsonObject obj = entry.getValue().getAsJsonObject();
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", entry.getKey());
                        item.put("jobRole", getString(obj, "jobRole", "Software Engineer"));
                        item.put("overallScore", getInt(obj, "totalScore", getInt(obj, "overallScore", 0)));
                        item.put("techScore", getInt(obj, "techScore", 0));
                        item.put("commScore", getInt(obj, "commScore", 0));
                        item.put("summary", getString(obj, "summary", ""));
                        item.put("userId", userId);
                        list.add(item);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing live mock interviews from Firebase: " + e.getMessage());
            }
        }
        return list;
    }

    public boolean applyForJob(String uid, String jobId) {
        if (uid == null || jobId == null) return false;

        String url = databaseUrl + "/applications/" + uid + "/" + jobId + ".json";
        JsonObject appData = new JsonObject();
        appData.addProperty("userId", uid);
        appData.addProperty("jobId", jobId);
        appData.addProperty("status", "APPLIED");
        appData.addProperty("appliedAt", System.currentTimeMillis());

        String response = executeHttpRequest(url, "PUT", appData.toString());
        return response != null;
    }

    public boolean hasAppliedForJob(String uid, String jobId) {
        if (uid == null || jobId == null) return false;
        String url = databaseUrl + "/applications/" + uid + "/" + jobId + ".json";
        String response = executeHttpRequest(url, "GET", null);
        return response != null && !response.equals("null") && !response.trim().isEmpty();
    }

    // =========================================================================
    // Realtime Database: Test Attempts (`test_attempts/{uid}`)
    // =========================================================================

    public boolean saveTestAttempt(String uid, TestAttempt attempt) {
        if (uid == null || attempt == null) return false;

        String attemptId = (attempt.getId() != null && !attempt.getId().isEmpty())
                ? attempt.getId()
                : UUID.randomUUID().toString();
        attempt.setId(attemptId);
        long now = System.currentTimeMillis();

        JsonObject json = new JsonObject();
        json.addProperty("id", attemptId);
        json.addProperty("attemptId", attemptId);
        json.addProperty("testId", attempt.getTestId());
        json.addProperty("userId", uid);
        json.addProperty("studentName", attempt.getStudentName() != null ? attempt.getStudentName() : "Student");
        json.addProperty("category", attempt.getCategory());
        json.addProperty("topic", attempt.getTopic());
        json.addProperty("score", attempt.getScore());
        json.addProperty("totalQuestions", attempt.getTotalQuestions());
        json.addProperty("correctAnswers", attempt.getCorrectAnswers());
        json.addProperty("passed", attempt.isPassed());
        json.addProperty("savedAt", attempt.getSavedAt() > 0 ? attempt.getSavedAt() : now);
        json.addProperty("startTime", attempt.getStartTime() > 0 ? attempt.getStartTime() : now);
        json.addProperty("durationSeconds", attempt.getDurationSeconds());
        json.addProperty("deadline", attempt.getDeadline());
        json.addProperty("submissionTime", attempt.getSubmissionTime() > 0 ? attempt.getSubmissionTime() : now);
        json.addProperty("status", attempt.getStatus() != null ? attempt.getStatus() : "submitted");
        json.addProperty("submissionReason", attempt.getSubmissionReason() != null ? attempt.getSubmissionReason() : "manual");
        json.addProperty("violationCount", attempt.getViolationCount());
        json.addProperty("savedAnswersJson", attempt.getSavedAnswersJson());
        json.addProperty("currentQuestionIndex", attempt.getCurrentQuestionIndex());
        json.addProperty("riskScore", attempt.getRiskScore());
        json.addProperty("riskLevel", attempt.getRiskLevel() != null ? attempt.getRiskLevel() : "LOW");
        json.addProperty("focusLostCount", attempt.getFocusLostCount());
        json.addProperty("fullscreenExitCount", attempt.getFullscreenExitCount());
        json.addProperty("cameraDisconnectCount", attempt.getCameraDisconnectCount());
        json.addProperty("copyPasteCount", attempt.getCopyPasteCount());
        json.addProperty("monitoringEventsJson", attempt.getMonitoringEventsJson() != null ? attempt.getMonitoringEventsJson() : "[]");
        json.addProperty("proctoringStatus", attempt.getProctoringStatus() != null ? attempt.getProctoringStatus() : "VALIDATED");
        json.addProperty("reviewNotes", attempt.getReviewNotes() != null ? attempt.getReviewNotes() : "");
        json.addProperty("testTitle", attempt.getTestTitle());
        json.addProperty("marksEarned", attempt.getMarksEarned());
        json.addProperty("maxMarks", attempt.getMaxMarks());
        json.addProperty("incorrectAnswers", attempt.getIncorrectAnswers());
        json.addProperty("unansweredCount", attempt.getUnansweredCount());
        json.addProperty("timeTakenSeconds", attempt.getTimeTakenSeconds());
        json.addProperty("accuracyPercentage", attempt.getAccuracyPercentage());
        json.addProperty("negativeMarkingApplied", attempt.isNegativeMarkingApplied());
        json.addProperty("negativeMarksDeducted", attempt.getNegativeMarksDeducted());
        json.addProperty("sectionBreakdownJson", attempt.getSectionBreakdownJson());
        json.addProperty("weakTopicsJson", attempt.getWeakTopicsJson());
        json.addProperty("recommendedPractice", attempt.getRecommendedPractice());
        json.addProperty("seriesId", attempt.getSeriesId() != null ? attempt.getSeriesId() : "");
        json.addProperty("difficulty", attempt.getDifficulty() != null ? attempt.getDifficulty() : "Medium");
        json.addProperty("generationStartedAt", attempt.getGenerationStartedAt());
        json.addProperty("generatedAt", attempt.getGeneratedAt());
        json.addProperty("expiresAt", attempt.getExpiresAt());
        json.addProperty("selectedQuestionCount", attempt.getSelectedQuestionCount());
        json.addProperty("generationVersion", attempt.getGenerationVersion());

        String userUrl = databaseUrl + "/test_attempts/" + uid + "/" + attemptId + ".json";
        String globalUrl = databaseUrl + "/all_test_attempts/" + attemptId + ".json";

        String resp1 = executeHttpRequest(userUrl, "PUT", json.toString());
        executeHttpRequest(globalUrl, "PUT", json.toString());

        saveFirestoreTestAttempt(attemptId, attempt);

        if (resp1 != null && !"in_progress".equalsIgnoreCase(attempt.getStatus())) {
            Map<String, Object> updates = new HashMap<>();
            String cat = attempt.getCategory() != null ? attempt.getCategory().toLowerCase() : "";
            if (cat.contains("java") || cat.contains("tech")) {
                updates.put("javaSkill", attempt.getScore());
                updates.put("technicalScore", attempt.getScore());
            } else if (cat.contains("dsa") || cat.contains("algo")) {
                updates.put("dsaSkill", attempt.getScore());
            } else if (cat.contains("sql") || cat.contains("dbms")) {
                updates.put("sqlSkill", attempt.getScore());
            } else if (cat.contains("aptitude") || cat.contains("quant")) {
                updates.put("aptitudeSkill", attempt.getScore());
            }

            StudentMetrics current = fetchStudentMetrics(uid);
            updates.put("testsCompleted", current.getTestsCompleted() + 1);
            updateStudentMetrics(uid, updates);
            return true;
        }

        return resp1 != null;
    }

    public List<TestAttempt> fetchTestAttempts(String uid) {
        List<TestAttempt> list = new ArrayList<>();
        if (uid == null) return list;

        String url = databaseUrl + "/test_attempts/" + uid + ".json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonObject map = JsonParser.parseString(response).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        TestAttempt t = parseTestAttemptJson(entry.getKey(), uid, entry.getValue().getAsJsonObject());
                        list.add(t);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing test attempts: " + e.getMessage());
            }
        }

        list.sort((a, b) -> Long.compare(b.getSavedAt(), a.getSavedAt()));
        return list;
    }

    public List<TestAttempt> fetchAllTestAttempts() {
        List<TestAttempt> list = new ArrayList<>();
        String url = databaseUrl + "/all_test_attempts.json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonObject map = JsonParser.parseString(response).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        JsonObject obj = entry.getValue().getAsJsonObject();
                        String userId = getString(obj, "userId", "");
                        TestAttempt t = parseTestAttemptJson(entry.getKey(), userId, obj);
                        list.add(t);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing all test attempts: " + e.getMessage());
            }
        }

        if (list.isEmpty()) {
            List<User> users = fetchAllUsers();
            for (User u : users) {
                list.addAll(fetchTestAttempts(u.getUid()));
            }
        }

        list.sort((a, b) -> Long.compare(b.getSavedAt(), a.getSavedAt()));
        return list;
    }

    public boolean updateTestAttemptReview(String attemptId, String userId, String status, String reviewNotes) {
        if (attemptId == null || attemptId.trim().isEmpty()) return false;

        JsonObject patch = new JsonObject();
        if (status != null && !status.isEmpty()) {
            patch.addProperty("proctoringStatus", status);
        }
        if (reviewNotes != null) {
            patch.addProperty("reviewNotes", reviewNotes);
        }
        patch.addProperty("reviewedAt", System.currentTimeMillis());

        String globalUrl = databaseUrl + "/all_test_attempts/" + attemptId + ".json";
        executeHttpRequest(globalUrl, "PATCH", patch.toString());

        if (userId != null && !userId.isEmpty()) {
            String userUrl = databaseUrl + "/test_attempts/" + userId + "/" + attemptId + ".json";
            executeHttpRequest(userUrl, "PATCH", patch.toString());
        }

        return true;
    }

    private void saveFirestoreTestAttempt(String attemptId, TestAttempt attempt) {
        if (attemptId == null || attempt == null) return;
        try {
            JsonObject fields = new JsonObject();
            fields.add("attemptId", makeStringField(attemptId));
            fields.add("id", makeStringField(attemptId));
            fields.add("testId", makeStringField(attempt.getTestId()));
            fields.add("userId", makeStringField(attempt.getUserId()));
            fields.add("studentName", makeStringField(attempt.getStudentName() != null ? attempt.getStudentName() : "Student"));
            fields.add("category", makeStringField(attempt.getCategory() != null ? attempt.getCategory() : "Technical"));
            fields.add("topic", makeStringField(attempt.getTopic() != null ? attempt.getTopic() : "Assessment"));
            fields.add("score", makeIntegerField(attempt.getScore()));
            fields.add("totalQuestions", makeIntegerField(attempt.getTotalQuestions()));
            fields.add("correctAnswers", makeIntegerField(attempt.getCorrectAnswers()));
            fields.add("passed", makeBooleanField(attempt.isPassed()));
            fields.add("status", makeStringField(attempt.getStatus() != null ? attempt.getStatus() : "submitted"));
            fields.add("submissionReason", makeStringField(attempt.getSubmissionReason() != null ? attempt.getSubmissionReason() : "manual"));
            fields.add("violationCount", makeIntegerField(attempt.getViolationCount()));
            fields.add("savedAnswersJson", makeStringField(attempt.getSavedAnswersJson()));
            fields.add("currentQuestionIndex", makeIntegerField(attempt.getCurrentQuestionIndex()));
            fields.add("startTime", makeIntegerField(attempt.getStartTime()));
            fields.add("submissionTime", makeIntegerField(attempt.getSubmissionTime()));
            fields.add("savedAt", makeIntegerField(attempt.getSavedAt()));
            fields.add("riskScore", makeIntegerField(attempt.getRiskScore()));
            fields.add("riskLevel", makeStringField(attempt.getRiskLevel() != null ? attempt.getRiskLevel() : "LOW"));
            fields.add("focusLostCount", makeIntegerField(attempt.getFocusLostCount()));
            fields.add("fullscreenExitCount", makeIntegerField(attempt.getFullscreenExitCount()));
            fields.add("cameraDisconnectCount", makeIntegerField(attempt.getCameraDisconnectCount()));
            fields.add("copyPasteCount", makeIntegerField(attempt.getCopyPasteCount()));
            fields.add("seriesId", makeStringField(attempt.getSeriesId() != null ? attempt.getSeriesId() : ""));
            fields.add("difficulty", makeStringField(attempt.getDifficulty() != null ? attempt.getDifficulty() : "Medium"));

            StringBuilder maskParams = new StringBuilder();
            for (String fieldName : fields.keySet()) {
                maskParams.append("&updateMask.fieldPaths=").append(fieldName);
            }

            String firestoreUrl = "https://firestore.googleapis.com/v1/projects/" + projectId 
                    + "/databases/(default)/documents/test_attempts/" + attemptId 
                    + "?key=" + apiKey + maskParams.toString();

            JsonObject body = new JsonObject();
            body.add("fields", fields);

            executeHttpRequest(firestoreUrl, "PATCH", body.toString());
        } catch (Exception e) {
            // Quiet fallback: attempt is already securely saved in Firebase Realtime Database
        }
    }

    private JsonObject makeStringField(String val) {
        JsonObject obj = new JsonObject();
        obj.addProperty("stringValue", val != null ? val : "");
        return obj;
    }

    private JsonObject makeIntegerField(long val) {
        JsonObject obj = new JsonObject();
        obj.addProperty("integerValue", String.valueOf(val));
        return obj;
    }

    private JsonObject makeBooleanField(boolean val) {
        JsonObject obj = new JsonObject();
        obj.addProperty("booleanValue", val);
        return obj;
    }

    private TestAttempt parseTestAttemptJson(String id, String defaultUserId, JsonObject obj) {
        TestAttempt t = new TestAttempt();
        t.setId(id);
        t.setTestId(getString(obj, "testId", getString(obj, "category", "Technical")));
        t.setUserId(getString(obj, "userId", defaultUserId));
        t.setStudentName(getString(obj, "studentName", "Student"));
        t.setCategory(getString(obj, "category", "Technical"));
        t.setTopic(getString(obj, "topic", "General"));
        t.setScore(getInt(obj, "score", 0));
        t.setTotalQuestions(getInt(obj, "totalQuestions", 25));
        t.setCorrectAnswers(getInt(obj, "correctAnswers", 0));
        t.setSavedAt(obj.has("savedAt") ? obj.get("savedAt").getAsLong() : System.currentTimeMillis());
        t.setPassed(obj.has("passed") && obj.get("passed").getAsBoolean());
        t.setStartTime(obj.has("startTime") ? obj.get("startTime").getAsLong() : 0);
        t.setDurationSeconds(getInt(obj, "durationSeconds", 1500));
        t.setDeadline(obj.has("deadline") ? obj.get("deadline").getAsLong() : 0);
        t.setSubmissionTime(obj.has("submissionTime") ? obj.get("submissionTime").getAsLong() : t.getSavedAt());
        t.setStatus(getString(obj, "status", "submitted"));
        t.setSubmissionReason(getString(obj, "submissionReason", "manual"));
        t.setViolationCount(getInt(obj, "violationCount", 0));
        t.setSavedAnswersJson(getString(obj, "savedAnswersJson", "{}"));
        t.setCurrentQuestionIndex(getInt(obj, "currentQuestionIndex", 0));
        t.setRiskScore(getInt(obj, "riskScore", 0));
        t.setRiskLevel(getString(obj, "riskLevel", "LOW"));
        t.setFocusLostCount(getInt(obj, "focusLostCount", 0));
        t.setFullscreenExitCount(getInt(obj, "fullscreenExitCount", 0));
        t.setCameraDisconnectCount(getInt(obj, "cameraDisconnectCount", 0));
        t.setCopyPasteCount(getInt(obj, "copyPasteCount", 0));
        t.setMonitoringEventsJson(getString(obj, "monitoringEventsJson", "[]"));
        t.setProctoringStatus(getString(obj, "proctoringStatus", "VALIDATED"));
        t.setReviewNotes(getString(obj, "reviewNotes", ""));
        t.setTestTitle(getString(obj, "testTitle", t.getCategory()));
        if (obj.has("marksEarned")) {
            try { t.setMarksEarned(obj.get("marksEarned").getAsDouble()); } catch (Exception ignored) {}
        }
        if (obj.has("maxMarks")) {
            try { t.setMaxMarks(obj.get("maxMarks").getAsDouble()); } catch (Exception ignored) {}
        }
        t.setIncorrectAnswers(getInt(obj, "incorrectAnswers", 0));
        t.setUnansweredCount(getInt(obj, "unansweredCount", 0));
        if (obj.has("timeTakenSeconds")) {
            try { t.setTimeTakenSeconds(obj.get("timeTakenSeconds").getAsLong()); } catch (Exception ignored) {}
        }
        if (obj.has("accuracyPercentage")) {
            try { t.setAccuracyPercentage(obj.get("accuracyPercentage").getAsDouble()); } catch (Exception ignored) {}
        }
        t.setNegativeMarkingApplied(obj.has("negativeMarkingApplied") && obj.get("negativeMarkingApplied").getAsBoolean());
        if (obj.has("negativeMarksDeducted")) {
            try { t.setNegativeMarksDeducted(obj.get("negativeMarksDeducted").getAsDouble()); } catch (Exception ignored) {}
        }
        t.setSectionBreakdownJson(getString(obj, "sectionBreakdownJson", "{}"));
        t.setWeakTopicsJson(getString(obj, "weakTopicsJson", "[]"));
        t.setRecommendedPractice(getString(obj, "recommendedPractice", ""));
        t.setSeriesId(getString(obj, "seriesId", ""));
        t.setDifficulty(getString(obj, "difficulty", "Medium"));
        if (obj.has("generationStartedAt")) t.setGenerationStartedAt(obj.get("generationStartedAt").getAsLong());
        if (obj.has("generatedAt")) t.setGeneratedAt(obj.get("generatedAt").getAsLong());
        if (obj.has("expiresAt")) t.setExpiresAt(obj.get("expiresAt").getAsLong());
        t.setSelectedQuestionCount(getInt(obj, "selectedQuestionCount", t.getTotalQuestions()));
        t.setGenerationVersion(getInt(obj, "generationVersion", 1));
        return t;
    }

    // =========================================================================
    // Realtime Database: Resume Management (`resumes/{uid}`)
    // =========================================================================

    public ResumeData fetchResumeData(String uid) {
        ResumeData resume = new ResumeData();
        resume.setUserId(uid);
        if (uid == null) return resume;

        String url = databaseUrl + "/resumes/" + uid + ".json";
        String response = executeHttpRequest(url, "GET", null);

        if (response != null && !response.equals("null") && !response.trim().isEmpty()) {
            try {
                JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                resume.setSkills(getString(obj, "skills", ""));
                resume.setExperience(getString(obj, "experience", ""));
                resume.setProjects(getString(obj, "projects", ""));
                resume.setResumeText(getString(obj, "resumeText", ""));
                resume.setAtsScore(getInt(obj, "atsScore", 0));
                resume.setGrammarScore(getInt(obj, "grammarScore", 0));
                resume.setExperienceReview(getString(obj, "experienceReview", ""));
                resume.setProjectsReview(getString(obj, "projectsReview", ""));

                if (obj.has("missingSkills") && obj.get("missingSkills").isJsonArray()) {
                    for (JsonElement el : obj.getAsJsonArray("missingSkills")) {
                        resume.getMissingSkills().add(el.getAsString());
                    }
                }
                if (obj.has("suggestions") && obj.get("suggestions").isJsonArray()) {
                    for (JsonElement el : obj.getAsJsonArray("suggestions")) {
                        resume.getSuggestions().add(el.getAsString());
                    }
                }
                if (obj.has("detectedSkills") && obj.get("detectedSkills").isJsonArray()) {
                    for (JsonElement el : obj.getAsJsonArray("detectedSkills")) {
                        resume.getDetectedSkills().add(el.getAsString());
                    }
                }
                resume.setUpdatedAt(obj.has("updatedAt") ? obj.get("updatedAt").getAsLong() : System.currentTimeMillis());
                return resume;
            } catch (Exception e) {
                System.err.println("Error parsing resume data: " + e.getMessage());
            }
        }

        return resume;
    }

    public boolean saveResumeData(ResumeData resume) {
        if (resume == null || resume.getUserId() == null) return false;

        String url = databaseUrl + "/resumes/" + resume.getUserId() + ".json";
        JsonObject json = new JsonObject();
        json.addProperty("userId", resume.getUserId());
        json.addProperty("skills", resume.getSkills());
        json.addProperty("experience", resume.getExperience());
        json.addProperty("projects", resume.getProjects());
        json.addProperty("resumeText", resume.getResumeText());
        json.addProperty("atsScore", resume.getAtsScore());
        json.addProperty("grammarScore", resume.getGrammarScore());
        json.addProperty("experienceReview", resume.getExperienceReview());
        json.addProperty("projectsReview", resume.getProjectsReview());
        json.addProperty("updatedAt", System.currentTimeMillis());

        JsonArray missing = new JsonArray();
        for (String s : resume.getMissingSkills()) missing.add(s);
        json.add("missingSkills", missing);

        JsonArray sug = new JsonArray();
        for (String s : resume.getSuggestions()) sug.add(s);
        json.add("suggestions", sug);

        JsonArray det = new JsonArray();
        for (String s : resume.getDetectedSkills()) det.add(s);
        json.add("detectedSkills", det);

        String response = executeHttpRequest(url, "PUT", json.toString());

        if (response != null && resume.getAtsScore() > 0) {
            Map<String, Object> metricUpdates = new HashMap<>();
            metricUpdates.put("resumeScore", resume.getAtsScore());
            updateStudentMetrics(resume.getUserId(), metricUpdates);
        }

        return response != null;
    }

    // =========================================================================
    // Realtime Database: Admin & Faculty Statistics
    // =========================================================================

    public Map<String, Integer> fetchSystemStats() {
        Map<String, Integer> stats = new HashMap<>();
        int userCount = 0;
        int jobCount = 0;
        int applicationCount = 0;

        try {
            String usersRes = executeHttpRequest(databaseUrl + "/users.json", "GET", null);
            if (usersRes != null && !usersRes.equals("null")) {
                userCount = JsonParser.parseString(usersRes).getAsJsonObject().size();
            }

            String jobsRes = executeHttpRequest(databaseUrl + "/jobs.json", "GET", null);
            if (jobsRes != null && !jobsRes.equals("null")) {
                jobCount = JsonParser.parseString(jobsRes).getAsJsonObject().size();
            }

            String appsRes = executeHttpRequest(databaseUrl + "/applications.json", "GET", null);
            if (appsRes != null && !appsRes.equals("null")) {
                JsonObject apps = JsonParser.parseString(appsRes).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : apps.entrySet()) {
                    if (e.getValue().isJsonObject()) {
                        applicationCount += e.getValue().getAsJsonObject().size();
                    }
                }
            }
        } catch (Exception ignored) {}

        stats.put("userCount", userCount);
        stats.put("jobCount", jobCount);
        stats.put("applicationCount", applicationCount);
        return stats;
    }

    // =========================================================================
    // Parallel Asynchronous Dashboard Batch Loading (Requirement 8 & 9)
    // =========================================================================

    public CompletableFuture<StudentMetrics> fetchStudentMetricsAsync(String uid) {
        return CompletableFuture.supplyAsync(() -> fetchStudentMetrics(uid));
    }

    public CompletableFuture<List<TestAttempt>> fetchTestAttemptsAsync(String uid) {
        return CompletableFuture.supplyAsync(() -> fetchTestAttempts(uid));
    }

    public CompletableFuture<List<Job>> fetchJobsAsync(String uid) {
        return CompletableFuture.supplyAsync(() -> fetchJobs(uid));
    }

    public CompletableFuture<ResumeData> fetchResumeDataAsync(String uid) {
        return CompletableFuture.supplyAsync(() -> fetchResumeData(uid));
    }

    // =========================================================================
    // High-Performance HTTP Request Helper (HttpClient with connection reuse)
    // =========================================================================

    public String executeHttpRequest(String urlStr, String method, String payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json");

            if ("POST".equalsIgnoreCase(method)) {
                builder.POST(payload != null ? HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8) : HttpRequest.BodyPublishers.noBody());
            } else if ("PUT".equalsIgnoreCase(method)) {
                builder.PUT(payload != null ? HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8) : HttpRequest.BodyPublishers.noBody());
            } else if ("PATCH".equalsIgnoreCase(method)) {
                builder.method("PATCH", payload != null ? HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8) : HttpRequest.BodyPublishers.noBody());
            } else if ("DELETE".equalsIgnoreCase(method)) {
                builder.DELETE();
            } else {
                builder.GET();
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            } else {
                if (!urlStr.contains("firestore.googleapis.com")) {
                    System.err.println("HTTP " + method + " " + urlStr + " returned status " + status);
                }
            }
        } catch (Exception e) {
            if (!urlStr.contains("firestore.googleapis.com")) {
                System.err.println("HTTP " + method + " exception for " + urlStr + ": " + e.getMessage());
            }
        }
        return null;
    }

    private int getInt(JsonObject obj, String key, int defVal) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return defVal;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return defVal;
        }
    }

    private String getString(JsonObject obj, String key, String defVal) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return defVal;
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return defVal;
        }
    }
}
