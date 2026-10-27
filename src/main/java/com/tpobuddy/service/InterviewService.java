package com.tpobuddy.service;

import com.google.gson.*;
import com.tpobuddy.model.InterviewSession;
import com.tpobuddy.model.InterviewSession.InterviewQuestion;
import com.tpobuddy.model.ResumeData;
import com.tpobuddy.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise AI Mock Interview Service using Google Gemini AI and Firebase.
 * Provides dynamic question generation, real-time natural language answer evaluation,
 * and comprehensive placement readiness report scoring.
 */
@Service
public class InterviewService {

    private final FirebaseService firebaseService;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    @Value("${gemini.api.key:}")
    private String geminiApiKey = "";

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel = "gemini-2.5-flash";

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/}")
    private String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final Map<String, InterviewSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, List<InterviewSession>> userSessionHistory = new ConcurrentHashMap<>();

    private static final Map<String, Map<String, List<InterviewQuestion>>> FALLBACK_QUESTION_BANK = new HashMap<>();

    static {
        // Full Stack Developer
        Map<String, List<InterviewQuestion>> fsMap = new HashMap<>();
        List<InterviewQuestion> fsMed = new ArrayList<>();
        fsMed.add(new InterviewQuestion(1, "Can you explain how a client request flows in a modern Spring Boot + React web application from browser to database?", "Technical", "react, http, controller, service, repository, database, json, rest"));
        fsMed.add(new InterviewQuestion(2, "How do you handle state management in React, and when would you choose Context API or Redux over local component state?", "Technical", "state, props, context, redux, re-render, centralized store, complex state"));
        fsMed.add(new InterviewQuestion(3, "Describe how you secure REST endpoints in a Spring Boot application using Spring Security or JWT tokens.", "Technical", "filter, token, jwt, authentication, authorization, bearer, security chain, roles"));
        fsMed.add(new InterviewQuestion(4, "Tell me about a challenging bug you encountered in a project, how you diagnosed the root cause, and how you resolved it.", "Behavioral", "debugging, log, root cause, reproduction, test, resolution, reflection"));
        fsMed.add(new InterviewQuestion(5, "How do you optimize database queries and prevent N+1 query problems in ORM frameworks like Hibernate/JPA?", "System Design", "join fetch, indexing, n+1, batch size, eager, lazy, execution plan"));
        fsMap.put("Medium", fsMed);

        List<InterviewQuestion> fsHard = new ArrayList<>();
        fsHard.add(new InterviewQuestion(1, "How would you design a distributed caching layer using Redis for high-concurrency microservices, and how do you handle cache invalidation?", "System Design", "redis, cache invalidation, cache aside, write-through, ttl, distributed, concurrency"));
        fsHard.add(new InterviewQuestion(2, "Explain event-driven architecture with Apache Kafka or RabbitMQ. How do you guarantee message ordering and idempotency?", "System Design", "kafka, partition, consumer group, idempotency, ack, dead letter queue, ordering"));
        fsHard.add(new InterviewQuestion(3, "Compare optimistic vs pessimistic locking in high-throughput database transactions. In what scenarios does each shine?", "Technical", "version, lock, collision, transaction, isolation, deadlocks, performance"));
        fsHard.add(new InterviewQuestion(4, "Describe a time when you had conflicting technical architectural opinions with a team member. How did you reach consensus?", "Behavioral", "consensus, data-driven, prototype, respectful, trade-offs, alignment"));
        fsHard.add(new InterviewQuestion(5, "How do you architect a zero-downtime deployment strategy with containerized microservices and blue-green deployments?", "System Design", "blue-green, canary, kubernetes, health check, rollback, load balancer"));
        fsMap.put("Hard", fsHard);
        fsMap.put("Extra Hard", fsHard);
        FALLBACK_QUESTION_BANK.put("Full Stack Developer", fsMap);

        // Frontend React Developer
        Map<String, List<InterviewQuestion>> feMap = new HashMap<>();
        List<InterviewQuestion> feMed = new ArrayList<>();
        feMed.add(new InterviewQuestion(1, "What is the Virtual DOM in React, and how does reconciliation work when a state change occurs?", "Technical", "virtual dom, diffing, reconciliation, re-render, batching, fiber, performance"));
        feMed.add(new InterviewQuestion(2, "Explain the rules and practical use cases of React Hooks like useEffect, useMemo, and useCallback.", "Technical", "dependency array, memoization, lifecycle, side effect, reference equality"));
        feMed.add(new InterviewQuestion(3, "How do you ensure responsive design and accessibility (a11y) across different devices and screen sizes?", "Technical", "media queries, flexbox, grid, aria, semantic html, mobile-first, contrast"));
        feMed.add(new InterviewQuestion(4, "How do you handle client-side form validation and state without causing unnecessary re-renders?", "Technical", "controlled, uncontrolled, debounce, validation, performance, custom hook"));
        feMed.add(new InterviewQuestion(5, "Describe a time when you received constructive feedback on your UI design or code review. How did you adapt?", "Behavioral", "feedback, collaboration, open mind, improvement, empathy, team"));
        feMap.put("Medium", feMed);
        feMap.put("Hard", feMed);
        feMap.put("Extra Hard", feMed);
        FALLBACK_QUESTION_BANK.put("Frontend React Developer", feMap);

        // Java Backend Developer
        Map<String, List<InterviewQuestion>> beMap = new HashMap<>();
        List<InterviewQuestion> beMed = new ArrayList<>();
        beMed.add(new InterviewQuestion(1, "Explain the difference between HashMap and ConcurrentHashMap in Java. How does thread safety work internally?", "Technical", "hashmap, concurrenthashmap, bucket, synchronization, lock striping, cas, race condition"));
        beMed.add(new InterviewQuestion(2, "What are the core differences between Spring Boot's @Component, @Service, and @Repository annotations?", "Technical", "stereotypes, dependency injection, exception translation, component scan, spring context"));
        beMed.add(new InterviewQuestion(3, "How does garbage collection work in the JVM, and what steps do you take when diagnosing an OutOfMemoryError?", "Technical", "heap, stack, young generation, old generation, mark and sweep, heap dump, memory leak"));
        beMed.add(new InterviewQuestion(4, "Describe the ACID properties in database transactions and how Spring's @Transactional annotation manages them.", "Technical", "atomicity, consistency, isolation, durability, rollback, propagation, commit"));
        beMed.add(new InterviewQuestion(5, "Where do you see yourself in 3 years as a backend software engineer?", "HR", "growth, technical mastery, ownership, leadership, impact, learning"));
        beMap.put("Medium", beMed);
        beMap.put("Hard", beMed);
        beMap.put("Extra Hard", beMed);
        FALLBACK_QUESTION_BANK.put("Java Backend Developer", beMap);

        // Data Analyst & AI
        Map<String, List<InterviewQuestion>> daMap = new HashMap<>();
        List<InterviewQuestion> daMed = new ArrayList<>();
        daMed.add(new InterviewQuestion(1, "Explain the difference between INNER JOIN, LEFT JOIN, and FULL OUTER JOIN in SQL with an example.", "Technical", "inner join, left join, outer join, null, match, primary key, foreign key"));
        daMed.add(new InterviewQuestion(2, "How do you handle missing values and outliers in a dataset prior to running statistical models?", "Technical", "imputation, mean, median, iqr, standard deviation, drop, domain knowledge"));
        daMed.add(new InterviewQuestion(3, "What is the difference between Precision, Recall, and F1-score? When would you prioritize Recall over Precision?", "Technical", "true positive, false positive, false negative, trade-off, fraud detection, medical diagnosis"));
        daMed.add(new InterviewQuestion(4, "How would you explain a complex data metric or model prediction to a non-technical recruiter or stakeholder?", "Behavioral", "simplification, analogy, business value, visual, clarity, active listening"));
        daMed.add(new InterviewQuestion(5, "Describe how you have used Python libraries like Pandas or NumPy for exploratory data analysis.", "Technical", "dataframe, groupby, aggregation, visualization, slicing, cleaning"));
        daMap.put("Medium", daMed);
        daMap.put("Hard", daMed);
        daMap.put("Extra Hard", daMed);
        FALLBACK_QUESTION_BANK.put("Data Analyst & AI", daMap);
    }

    public InterviewService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Starts a new AI Mock Interview session.
     * Personalizes questions using Gemini AI based on role, difficulty, questionCount, and resume data.
     */
    public InterviewSession startSession(String userId, String studentName, String role, String difficulty, int questionCount) {
        String effRole = (role != null && !role.trim().isEmpty()) ? role.trim() : "Full Stack Developer";
        String effDifficulty = (difficulty != null && !difficulty.trim().isEmpty()) ? difficulty.trim() : "Medium";
        int effCount = (questionCount > 0 && questionCount <= 10) ? questionCount : 5;

        InterviewSession session = new InterviewSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStudentName((studentName != null && !studentName.trim().isEmpty()) ? studentName : "Student");
        session.setJobRole(effRole);
        session.setDifficulty(effDifficulty);
        session.setQuestionCount(effCount);
        session.setDurationMinutes(effCount * 3);

        // Fetch resume data if available for tailoring
        ResumeData resume = null;
        try {
            if (userId != null && !userId.equals("guest_student")) {
                resume = firebaseService.fetchResumeData(userId);
            }
        } catch (Exception ignored) {}

        List<InterviewQuestion> questions = null;
        String apiKey = getEffectiveApiKey();

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                questions = generateQuestionsWithGemini(effRole, effDifficulty, effCount, session.getStudentName(), resume, apiKey);
                if (questions != null && !questions.isEmpty()) {
                    System.out.println("✨ Generated " + questions.size() + " Gemini AI questions for role: " + effRole + " [" + effDifficulty + "]");
                }
            } catch (Exception e) {
                System.err.println("Notice: Gemini question generation unavailable (" + e.getMessage() + "). Using calibrated question bank.");
            }
        }

        // Fallback to calibrated questions if AI unavailable or partial
        if (questions == null || questions.isEmpty()) {
            questions = getFallbackQuestions(effRole, effDifficulty, effCount);
        }

        session.setQuestions(questions);
        session.setCurrentQuestionIndex(0);
        activeSessions.put(session.getSessionId(), session);
        return session;
    }

    public InterviewSession startSession(String userId, String studentName, String role) {
        return startSession(userId, studentName, role, "Medium", 5);
    }

    public InterviewSession getSession(String sessionId) {
        if (sessionId == null) return null;
        return activeSessions.get(sessionId);
    }

    /**
     * Checks if the student has an active in-progress session.
     */
    public InterviewSession getActiveSession(String userId) {
        if (userId == null) return null;
        for (InterviewSession s : activeSessions.values()) {
            if (userId.equals(s.getUserId()) && "IN_PROGRESS".equalsIgnoreCase(s.getStatus())) {
                return s;
            }
        }
        return null;
    }

    /**
     * Evaluates a candidate's answer using Gemini AI or structured rubric fallback.
     */
    public Map<String, Object> evaluateAnswer(String userId, String sessionId, int questionId, String userAnswer) {
        Map<String, Object> result = new HashMap<>();
        InterviewSession session = activeSessions.get(sessionId);

        if (session == null) {
            result.put("success", false);
            result.put("error", "Interview session not found or expired.");
            return result;
        }

        if (userId != null && session.getUserId() != null && !session.getUserId().equals("guest_student") && !session.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("error", "Unauthorized access to this interview session.");
            return result;
        }

        InterviewQuestion currentQ = null;
        for (InterviewQuestion q : session.getQuestions()) {
            if (q.getId() == questionId) {
                currentQ = q;
                break;
            }
        }

        if (currentQ == null) {
            result.put("success", false);
            result.put("error", "Question #" + questionId + " not found in session.");
            return result;
        }

        // Idempotency check: if already evaluated, return cached result
        if (currentQ.getUserAnswer() != null && !currentQ.getUserAnswer().isEmpty() && currentQ.getAiFeedback() != null) {
            result.put("success", true);
            result.put("score", currentQ.getScore());
            result.put("feedback", currentQ.getAiFeedback());
            result.put("strengths", currentQ.getStrengths());
            result.put("areasToImprove", currentQ.getAreasToImprove());
            result.put("nextQuestionIndex", session.getCurrentQuestionIndex());
            result.put("isCompleted", session.getCurrentQuestionIndex() >= session.getQuestions().size());
            return result;
        }

        currentQ.setUserAnswer((userAnswer != null) ? userAnswer.trim() : "");

        int score = 0;
        String feedback = "";
        List<String> strengths = new ArrayList<>();
        List<String> areasToImprove = new ArrayList<>();
        int techScore = 0;
        int commScore = 0;

        String apiKey = getEffectiveApiKey();
        boolean evaluatedByAi = false;

        if (apiKey != null && !apiKey.isEmpty() && currentQ.getUserAnswer().length() >= 5) {
            try {
                JsonObject evalJson = evaluateWithGemini(currentQ.getQuestionText(), currentQ.getCategory(),
                        currentQ.getExpectedConcepts(), currentQ.getUserAnswer(), session.getJobRole(), session.getDifficulty(), apiKey);

                if (evalJson != null) {
                    score = Math.max(0, Math.min(10, getInt(evalJson, "score", 7)));
                    feedback = getString(evalJson, "feedback", "");
                    techScore = Math.max(0, Math.min(10, getInt(evalJson, "technicalAccuracy", score)));
                    commScore = Math.max(0, Math.min(10, getInt(evalJson, "communicationClarity", score)));

                    if (evalJson.has("strengths") && evalJson.get("strengths").isJsonArray()) {
                        for (JsonElement e : evalJson.getAsJsonArray("strengths")) {
                            strengths.add(e.getAsString());
                        }
                    }
                    if (evalJson.has("areasToImprove") && evalJson.get("areasToImprove").isJsonArray()) {
                        for (JsonElement e : evalJson.getAsJsonArray("areasToImprove")) {
                            areasToImprove.add(e.getAsString());
                        }
                    }
                    evaluatedByAi = true;
                }
            } catch (Exception e) {
                System.err.println("Notice: Gemini answer evaluation error (" + e.getMessage() + "). Applying rubric evaluation.");
            }
        }

        if (!evaluatedByAi) {
            // Heuristic Rubric Fallback
            int[] rubric = evaluateWithRubric(currentQ, currentQ.getUserAnswer());
            score = rubric[0];
            techScore = rubric[1];
            commScore = rubric[2];
            feedback = generateRubricFeedback(currentQ, score);
            strengths.add("Addressed the primary question prompt directly.");
            if (score >= 7) {
                strengths.add("Demonstrated core concept familiarity and structured reasoning.");
            }
            if (currentQ.getExpectedConcepts() != null && !currentQ.getExpectedConcepts().isEmpty()) {
                areasToImprove.add("Incorporate key concepts: " + currentQ.getExpectedConcepts());
            }
            areasToImprove.add("Elaborate using the STAR method (Situation, Task, Action, Result) with measurable outcomes.");
        }

        currentQ.setScore(score);
        currentQ.setTechnicalAccuracy(techScore);
        currentQ.setCommunicationClarity(commScore);
        currentQ.setAiFeedback(feedback);
        currentQ.setStrengths(strengths);
        currentQ.setAreasToImprove(areasToImprove);

        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);

        result.put("success", true);
        result.put("score", score);
        result.put("feedback", feedback);
        result.put("strengths", strengths);
        result.put("areasToImprove", areasToImprove);
        result.put("nextQuestionIndex", session.getCurrentQuestionIndex());
        result.put("isCompleted", session.getCurrentQuestionIndex() >= session.getQuestions().size());

        return result;
    }

    public Map<String, Object> evaluateAnswer(String sessionId, int questionId, String userAnswer) {
        return evaluateAnswer(null, sessionId, questionId, userAnswer);
    }

    /**
     * Concludes the interview session, synthesizes full evaluation report via Gemini AI,
     * and persists results to Firebase Realtime Database and Cloud Firestore.
     */
    public InterviewSession finishSession(String userId, String sessionId) {
        InterviewSession session = activeSessions.get(sessionId);
        if (session == null) return null;

        if (userId != null && session.getUserId() != null && !session.getUserId().equals("guest_student") && !session.getUserId().equals(userId)) {
            return null; // unauthorized
        }

        // Idempotent: return completed session if already finished
        if ("COMPLETED".equalsIgnoreCase(session.getStatus())) {
            return session;
        }

        session.setEndTime(System.currentTimeMillis());
        session.setStatus("COMPLETED");

        int totalScore = 0;
        int techScoreSum = 0;
        int techCount = 0;
        int commScoreSum = 0;
        int commCount = 0;

        for (InterviewQuestion q : session.getQuestions()) {
            totalScore += (q.getScore() * 10);
            if ("Behavioral".equalsIgnoreCase(q.getCategory()) || "HR".equalsIgnoreCase(q.getCategory())) {
                commScoreSum += (q.getScore() * 10);
                commCount++;
            } else {
                techScoreSum += (q.getScore() * 10);
                techCount++;
            }
        }

        int totalQ = session.getQuestions().size();
        int overall = (totalQ > 0) ? (totalScore / totalQ) : 0;
        int techScore = (techCount > 0) ? (techScoreSum / techCount) : overall;
        int commScore = (commCount > 0) ? (commScoreSum / commCount) : overall;

        session.setOverallScore(overall);
        session.setTechScore(techScore);
        session.setCommScore(commScore);

        String apiKey = getEffectiveApiKey();
        boolean reportGeneratedByAi = false;

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                JsonObject aiReport = generateFinalReportWithGemini(session, apiKey);
                if (aiReport != null) {
                    session.setSummary(getString(aiReport, "summary", "Candidate completed the interview successfully."));

                    List<String> strengths = new ArrayList<>();
                    if (aiReport.has("strengths") && aiReport.get("strengths").isJsonArray()) {
                        for (JsonElement el : aiReport.getAsJsonArray("strengths")) strengths.add(el.getAsString());
                    }
                    if (!strengths.isEmpty()) session.setStrengths(strengths);

                    List<String> weaknesses = new ArrayList<>();
                    if (aiReport.has("weaknesses") && aiReport.get("weaknesses").isJsonArray()) {
                        for (JsonElement el : aiReport.getAsJsonArray("weaknesses")) weaknesses.add(el.getAsString());
                    }
                    if (!weaknesses.isEmpty()) session.setWeaknesses(weaknesses);

                    List<String> recommendations = new ArrayList<>();
                    if (aiReport.has("recommendations") && aiReport.get("recommendations").isJsonArray()) {
                        for (JsonElement el : aiReport.getAsJsonArray("recommendations")) recommendations.add(el.getAsString());
                    }
                    if (!recommendations.isEmpty()) session.setRecommendations(recommendations);

                    reportGeneratedByAi = true;
                }
            } catch (Exception e) {
                System.err.println("Notice: Gemini final report synthesis error (" + e.getMessage() + "). Using rule synthesis.");
            }
        }

        if (!reportGeneratedByAi) {
            synthesizeRuleReport(session, overall, techScore, commScore);
        }

        // Persist to Firebase Realtime Database & Firestore
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", session.getSessionId());
            data.put("userId", session.getUserId());
            data.put("studentName", session.getStudentName());
            data.put("jobRole", session.getJobRole());
            data.put("difficulty", session.getDifficulty());
            data.put("totalScore", session.getOverallScore());
            data.put("overallScore", session.getOverallScore());
            data.put("techScore", session.getTechScore());
            data.put("commScore", session.getCommScore());
            data.put("summary", session.getSummary());
            data.put("strengths", session.getStrengths());
            data.put("weaknesses", session.getWeaknesses());
            data.put("recommendations", session.getRecommendations());
            data.put("startTime", session.getStartTime());
            data.put("endTime", session.getEndTime());
            data.put("timestamp", session.getEndTime());
            data.put("status", "COMPLETED");

            List<Map<String, Object>> qList = new ArrayList<>();
            for (InterviewQuestion q : session.getQuestions()) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("id", q.getId());
                qMap.put("questionText", q.getQuestionText());
                qMap.put("category", q.getCategory());
                qMap.put("userAnswer", q.getUserAnswer());
                qMap.put("aiFeedback", q.getAiFeedback());
                qMap.put("score", q.getScore());
                qList.add(qMap);
            }
            data.put("questions", qList);

            firebaseService.saveMockInterview(session.getUserId(), session.getSessionId(), data);

            // Update student placement metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("interviewScore", session.getOverallScore());
            firebaseService.updateStudentMetrics(session.getUserId(), metrics);
        } catch (Exception e) {
            System.err.println("Notice: Error syncing interview report to Firebase: " + e.getMessage());
        }

        // Cache in user history
        userSessionHistory.computeIfAbsent(session.getUserId(), k -> new ArrayList<>()).removeIf(s -> s.getSessionId().equals(session.getSessionId()));
        userSessionHistory.get(session.getUserId()).add(0, session);

        return session;
    }

    public InterviewSession finishSession(String sessionId) {
        return finishSession(null, sessionId);
    }

    public List<InterviewSession> getUserHistory(String userId) {
        if (userId == null) return Collections.emptyList();
        List<InterviewSession> memoryList = userSessionHistory.getOrDefault(userId, Collections.emptyList());
        if (!memoryList.isEmpty()) {
            return memoryList;
        }

        // Fetch live from Firebase
        List<Map<String, Object>> liveList = firebaseService.fetchMockInterviews(userId);
        List<InterviewSession> result = new ArrayList<>();
        for (Map<String, Object> item : liveList) {
            InterviewSession s = new InterviewSession();
            s.setSessionId((String) item.get("id"));
            s.setUserId((String) item.get("userId"));
            s.setJobRole((String) item.get("jobRole"));
            s.setDifficulty((String) item.getOrDefault("difficulty", "Medium"));
            s.setOverallScore(((Number) item.getOrDefault("overallScore", item.getOrDefault("totalScore", 0))).intValue());
            s.setTechScore(((Number) item.getOrDefault("techScore", 0)).intValue());
            s.setCommScore(((Number) item.getOrDefault("commScore", 0)).intValue());
            s.setSummary((String) item.getOrDefault("summary", ""));
            s.setStatus("COMPLETED");
            result.add(s);
        }

        if (!result.isEmpty()) {
            userSessionHistory.put(userId, result);
        }
        return result;
    }

    public InterviewSession getLatestReport(String userId) {
        List<InterviewSession> history = getUserHistory(userId);
        if (history != null && !history.isEmpty()) {
            return history.get(0);
        }
        return null;
    }

    // =========================================================================
    // Gemini AI Prompting & Request Execution
    // =========================================================================

    private List<InterviewQuestion> generateQuestionsWithGemini(String role, String difficulty, int count,
                                                                 String studentName, ResumeData resume, String apiKey) throws Exception {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an elite Lead Technical Interviewer and Engineering Hiring Manager at a top tech company.\n");
        prompt.append("Generate exactly ").append(count).append(" authentic mock interview questions for the target job role: '").append(role).append("'.\n");
        prompt.append("Difficulty level: ").append(difficulty).append(".\n");

        if (resume != null && resume.getResumeText() != null && !resume.getResumeText().trim().isEmpty()) {
            prompt.append("Candidate background summary:\n");
            if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
                prompt.append("Skills: ").append(String.join(", ", resume.getSkills())).append(".\n");
            }
            if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
                prompt.append("Projects: ").append(String.join("; ", resume.getProjects())).append(".\n");
            }
            prompt.append("Incorporate candidate skills and project architecture context naturally where appropriate.\n");
        }

        prompt.append("Question requirements:\n");
        prompt.append("- Mix Technical (coding/APIs/architecture), System Design/Scenarios, Behavioral (STAR framework), and HR/Motivation.\n");
        prompt.append("- Format: JSON array of objects with keys:\n");
        prompt.append("  'id' (number 1 to ").append(count).append("),\n");
        prompt.append("  'questionText' (string question statement),\n");
        prompt.append("  'category' (one of 'Technical', 'System Design', 'Behavioral', 'HR'),\n");
        prompt.append("  'expectedConcepts' (comma-separated list of expected keywords or key points).\n");
        prompt.append("Output STRICT valid JSON ONLY. No markdown wrappers.");

        String rawResponse = executeGeminiContentGeneration(prompt.toString(), apiKey);
        if (rawResponse == null || rawResponse.isEmpty()) return Collections.emptyList();

        return parseQuestionsFromJson(rawResponse, count);
    }

    private JsonObject evaluateWithGemini(String questionText, String category, String expectedConcepts,
                                          String candidateAnswer, String role, String difficulty, String apiKey) throws Exception {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert AI Technical Interviewer evaluating a candidate's answer for role: '").append(role).append("', difficulty: ").append(difficulty).append(".\n\n");
        prompt.append("Question (Category: ").append(category).append("):\n").append(questionText).append("\n\n");
        prompt.append("Expected Core Concepts: ").append(expectedConcepts).append("\n\n");
        prompt.append("Candidate's Submitted Answer:\n\"").append(candidateAnswer).append("\"\n\n");
        prompt.append("Evaluate this answer thoroughly. Return a strict JSON object with fields:\n");
        prompt.append("{\n");
        prompt.append("  \"score\": <integer from 0 to 10>,\n");
        prompt.append("  \"technicalAccuracy\": <integer from 0 to 10>,\n");
        prompt.append("  \"communicationClarity\": <integer from 0 to 10>,\n");
        prompt.append("  \"feedback\": \"<constructive, professional 2-3 sentence review analyzing depth, precision, and trade-offs>\",\n");
        prompt.append("  \"strengths\": [\"<key positive point 1>\", \"<key positive point 2>\"],\n");
        prompt.append("  \"areasToImprove\": [\"<concrete improvement point 1>\", \"<concrete improvement point 2>\"]\n");
        prompt.append("}\n");
        prompt.append("STRICT JSON ONLY. No markdown block.");

        String rawResponse = executeGeminiContentGeneration(prompt.toString(), apiKey);
        if (rawResponse == null || rawResponse.isEmpty()) return null;

        String cleanJson = cleanMarkdown(rawResponse);
        JsonElement el = JsonParser.parseString(cleanJson);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private JsonObject generateFinalReportWithGemini(InterviewSession session, String apiKey) throws Exception {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an Executive Campus Placement Director synthesizing the Final Placement Readiness Evaluation Report for student: ")
                .append(session.getStudentName()).append(" targeting role: '").append(session.getJobRole())
                .append("' at difficulty: ").append(session.getDifficulty()).append(".\n\n");

        prompt.append("Transcript of Questions and Candidate Responses:\n");
        for (InterviewQuestion q : session.getQuestions()) {
            prompt.append("Q").append(q.getId()).append(" [").append(q.getCategory()).append("]: ").append(q.getQuestionText()).append("\n");
            prompt.append("Answer: ").append(q.getUserAnswer() != null ? q.getUserAnswer() : "No response").append("\n");
            prompt.append("Score: ").append(q.getScore()).append("/10. AI Feedback: ").append(q.getAiFeedback()).append("\n\n");
        }

        prompt.append("Based on the overall interview performance, formulate an executive evaluation report in strict JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"<Comprehensive 2-4 sentence assessment of candidate readiness, placement tier recommendation, and demeanor>\",\n");
        prompt.append("  \"strengths\": [\"<demonstrated strength 1>\", \"<demonstrated strength 2>\", \"<demonstrated strength 3>\"],\n");
        prompt.append("  \"weaknesses\": [\"<identified gap 1>\", \"<identified gap 2>\"],\n");
        prompt.append("  \"recommendations\": [\"<actionable preparation step 1>\", \"<actionable preparation step 2>\", \"<actionable preparation step 3>\"]\n");
        prompt.append("}\n");
        prompt.append("STRICT JSON ONLY.");

        String rawResponse = executeGeminiContentGeneration(prompt.toString(), apiKey);
        if (rawResponse == null || rawResponse.isEmpty()) return null;

        String cleanJson = cleanMarkdown(rawResponse);
        JsonElement el = JsonParser.parseString(cleanJson);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private String executeGeminiContentGeneration(String prompt, String apiKey) {
        String endpoint = geminiApiUrl + geminiModel + ":generateContent?key=" + apiKey;

        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        payload.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.35);
        genConfig.addProperty("topP", 0.95);
        genConfig.addProperty("maxOutputTokens", 4096);
        genConfig.addProperty("responseMimeType", "application/json");
        payload.add("generationConfig", genConfig);

        return executeHttpRequestWithRetry(endpoint, payload.toString(), 1);
    }

    private String executeHttpRequestWithRetry(String urlStr, String jsonBody, int maxRetries) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlStr))
                        .timeout(Duration.ofSeconds(12))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response.body();
                } else if (status == 429) {
                    // Backoff on rate limit
                    long sleepMs = (long) Math.pow(2, attempt) * 1200;
                    Thread.sleep(sleepMs);
                } else {
                    System.err.println("Gemini HTTP error " + status + " on attempt " + attempt);
                }
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    System.err.println("Gemini HTTP call failed after retries: " + e.getMessage());
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Helpers, Parsing & Heuristics
    // =========================================================================

    private List<InterviewQuestion> parseQuestionsFromJson(String rawResponse, int targetCount) {
        List<InterviewQuestion> list = new ArrayList<>();
        try {
            String text = cleanMarkdown(rawResponse);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            if (root.has("candidates") && !root.getAsJsonArray("candidates").isEmpty()) {
                JsonObject cand = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
                text = cand.getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
                text = cleanMarkdown(text);
            }

            JsonElement parsed = JsonParser.parseString(text);
            JsonArray arr = parsed.isJsonArray() ? parsed.getAsJsonArray() :
                    (parsed.isJsonObject() && parsed.getAsJsonObject().has("questions") ?
                            parsed.getAsJsonObject().getAsJsonArray("questions") : null);

            if (arr != null) {
                int idCounter = 1;
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        String qText = getString(obj, "questionText", getString(obj, "question", ""));
                        String cat = getString(obj, "category", "Technical");
                        String concepts = getString(obj, "expectedConcepts", getString(obj, "concepts", ""));

                        if (!qText.trim().isEmpty()) {
                            list.add(new InterviewQuestion(idCounter++, qText, cat, concepts));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini generated questions: " + e.getMessage());
        }
        return list;
    }

    private String cleanMarkdown(String text) {
        if (text == null) return "";
        text = text.trim();
        if (text.startsWith("```json")) text = text.substring(7);
        if (text.startsWith("```")) text = text.substring(3);
        if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
        return text.trim();
    }

    private List<InterviewQuestion> getFallbackQuestions(String role, String difficulty, int count) {
        Map<String, List<InterviewQuestion>> roleBank = FALLBACK_QUESTION_BANK.getOrDefault(role, FALLBACK_QUESTION_BANK.get("Full Stack Developer"));
        List<InterviewQuestion> bank = roleBank.getOrDefault(difficulty, roleBank.get("Medium"));
        if (bank == null || bank.isEmpty()) {
            bank = FALLBACK_QUESTION_BANK.get("Full Stack Developer").get("Medium");
        }

        List<InterviewQuestion> selected = new ArrayList<>();
        int id = 1;
        for (int i = 0; i < Math.min(count, bank.size()); i++) {
            InterviewQuestion src = bank.get(i);
            selected.add(new InterviewQuestion(id++, src.getQuestionText(), src.getCategory(), src.getExpectedConcepts()));
        }
        return selected;
    }

    private int[] evaluateWithRubric(InterviewQuestion q, String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return new int[]{1, 1, 1};
        }

        String lowerAnswer = answer.toLowerCase();
        int matches = 0;
        if (q.getExpectedConcepts() != null && !q.getExpectedConcepts().isEmpty()) {
            String[] concepts = q.getExpectedConcepts().toLowerCase().split(",\\s*");
            for (String kw : concepts) {
                if (!kw.trim().isEmpty() && lowerAnswer.contains(kw.trim())) {
                    matches++;
                }
            }
        }

        int wordCount = answer.trim().split("\\s+").length;
        int score = 4;
        int tech = 4;
        int comm = 5;

        if (matches >= 4 && wordCount >= 35) {
            score = 9;
            tech = 9;
            comm = 9;
        } else if (matches >= 2 && wordCount >= 20) {
            score = 7;
            tech = 7;
            comm = 7;
        } else if (wordCount >= 15) {
            score = 5;
            tech = 5;
            comm = 6;
        } else {
            score = 3;
            tech = 3;
            comm = 4;
        }

        return new int[]{score, tech, comm};
    }

    private String generateRubricFeedback(InterviewQuestion q, int score) {
        if (score >= 8) {
            return "Excellent answer! You demonstrated solid technical depth, articulated core concepts clearly, and covered key architectural nuances.";
        } else if (score >= 6) {
            return "Good response. You addressed the fundamental points well. To achieve an outstanding score, include concrete project examples or discuss performance trade-offs.";
        } else if (score >= 4) {
            return "Fair attempt. Your answer is on the right track, but could benefit from explaining key underlying mechanisms: " + q.getExpectedConcepts() + ".";
        } else {
            return "Brief answer. In an interview, elaborate on your thought process using the STAR method (Situation, Task, Action, Result).";
        }
    }

    private void synthesizeRuleReport(InterviewSession session, int overall, int techScore, int commScore) {
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (techScore >= 70) {
            strengths.add("Strong foundational knowledge in " + session.getJobRole() + " core concepts and design patterns.");
        } else {
            weaknesses.add("Review core technical definitions and architectural trade-offs for " + session.getJobRole() + ".");
            recommendations.add("Spend 30 minutes daily reviewing system design principles and standard interview questions.");
        }

        if (commScore >= 70) {
            strengths.add("Articulate and structured communication using professional terminology.");
        } else {
            weaknesses.add("Practice answering with the STAR method (Situation, Task, Action, Result) to avoid brief answers.");
            recommendations.add("Record yourself speaking your technical explanations aloud to build cadence and confidence.");
        }

        strengths.add("Maintained active engagement and answered all assigned assessment prompts.");
        weaknesses.add("Could provide more real-world project context or quantifiable metrics in behavioral answers.");
        recommendations.add("Tailor examples directly to candidate experiences from your uploaded resume projects.");

        session.setStrengths(strengths);
        session.setWeaknesses(weaknesses);
        session.setRecommendations(recommendations);

        if (overall >= 75) {
            session.setSummary("Candidate demonstrates high placement readiness for " + session.getJobRole() + " [" + session.getDifficulty() + "]. Highly recommended for on-campus company technical rounds.");
        } else if (overall >= 60) {
            session.setSummary("Candidate shows solid potential with moderate placement readiness. Needs targeted practice on system design and technical elaboration.");
        } else {
            session.setSummary("Candidate requires further conceptual preparation before attending tier-1 recruitment drives. Review recommended practice topics.");
        }
    }

    private String getEffectiveApiKey() {
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.startsWith("${") && !geminiApiKey.contains("YOUR_")) {
            return geminiApiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty() && !envKey.startsWith("${") && !envKey.contains("YOUR_")) {
            return envKey.trim();
        }
        String configKey = Config.get("gemini.api.key", "");
        if (configKey != null && !configKey.trim().isEmpty() && !configKey.startsWith("${") && !configKey.contains("YOUR_")) {
            return configKey.trim();
        }
        try {
            java.io.File dotEnv = new java.io.File(".env");
            if (dotEnv.exists()) {
                try (java.util.Scanner sc = new java.util.Scanner(dotEnv)) {
                    while (sc.hasNextLine()) {
                        String line = sc.nextLine().trim();
                        if (line.startsWith("GEMINI_API_KEY=") && !line.startsWith("#")) {
                            String k = line.substring("GEMINI_API_KEY=".length()).trim();
                            if (!k.isEmpty()) return k;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String getString(JsonObject obj, String key, String def) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return def;
    }

    private int getInt(JsonObject obj, String key, int def) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsInt();
            } catch (Exception ignored) {}
        }
        return def;
    }
}
