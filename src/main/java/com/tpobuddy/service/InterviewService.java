package com.tpobuddy.service;

import com.tpobuddy.model.InterviewSession;
import com.tpobuddy.model.InterviewSession.InterviewQuestion;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing AI Mock Interview question bank generation,
 * natural language answer evaluation, and placement readiness report scoring.
 */
@Service
public class InterviewService {

    private final FirebaseService firebaseService;

    public InterviewService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    private final Map<String, InterviewSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, List<InterviewSession>> userSessionHistory = new ConcurrentHashMap<>();

    private static final Map<String, List<InterviewQuestion>> ROLE_QUESTION_BANK = new HashMap<>();

    static {
        // Full Stack Developer
        List<InterviewQuestion> fullstack = new ArrayList<>();
        fullstack.add(new InterviewQuestion(1, "Can you explain how a client request flows in a modern Spring Boot + React web application from browser to database?", "Technical", "react, http, controller, service, repository, database, json, rest"));
        fullstack.add(new InterviewQuestion(2, "How do you handle state management in React, and when would you choose Context API or Redux over local component state?", "Technical", "state, props, context, redux, re-render, centralized store, complex state"));
        fullstack.add(new InterviewQuestion(3, "Describe how you secure REST endpoints in a Spring Boot application using Spring Security or JWT tokens.", "Technical", "filter, token, jwt, authentication, authorization, bearer, security chain, roles"));
        fullstack.add(new InterviewQuestion(4, "Tell me about a challenging bug you encountered in a project, how you diagnosed the root cause, and how you resolved it.", "Behavioral", "debugging, log, root cause, reproduction, test, resolution, reflection"));
        fullstack.add(new InterviewQuestion(5, "How do you optimize database queries and prevent N+1 query problems in ORM frameworks like Hibernate/JPA?", "System Design", "join fetch, indexing, n+1, batch size, eager, lazy, execution plan"));
        ROLE_QUESTION_BANK.put("Full Stack Developer", fullstack);

        // Frontend React Developer
        List<InterviewQuestion> frontend = new ArrayList<>();
        frontend.add(new InterviewQuestion(1, "What is the Virtual DOM in React, and how does reconciliation work when a state change occurs?", "Technical", "virtual dom, diffing, reconciliation, re-render, batching, fiber, performance"));
        frontend.add(new InterviewQuestion(2, "Explain the rules and practical use cases of React Hooks like useEffect, useMemo, and useCallback.", "Technical", "dependency array, memoization, lifecycle, side effect, reference equality"));
        frontend.add(new InterviewQuestion(3, "How do you ensure responsive design and accessibility (a11y) across different devices and screen sizes?", "Technical", "media queries, flexbox, grid, aria, semantic html, mobile-first, contrast"));
        frontend.add(new InterviewQuestion(4, "How do you handle client-side form validation and state without causing unnecessary re-renders?", "Technical", "controlled, uncontrolled, debounce, validation, performance, custom hook"));
        frontend.add(new InterviewQuestion(5, "Describe a time when you received constructive feedback on your UI design or code review. How did you adapt?", "Behavioral", "feedback, collaboration, open mind, improvement, empathy, team"));
        ROLE_QUESTION_BANK.put("Frontend React Developer", frontend);

        // Java Backend Developer
        List<InterviewQuestion> javaDev = new ArrayList<>();
        javaDev.add(new InterviewQuestion(1, "Explain the difference between HashMap and ConcurrentHashMap in Java. How does thread safety work internally?", "Technical", "hashmap, concurrenthashmap, bucket, synchronization, lock striping, cas, race condition"));
        javaDev.add(new InterviewQuestion(2, "What are the core differences between Spring Boot's @Component, @Service, and @Repository annotations?", "Technical", "stereotypes, dependency injection, exception translation, component scan, spring context"));
        javaDev.add(new InterviewQuestion(3, "How does garbage collection work in the JVM, and what steps do you take when diagnosing an OutOfMemoryError?", "Technical", "heap, stack, young generation, old generation, mark and sweep, heap dump, memory leak"));
        javaDev.add(new InterviewQuestion(4, "Describe the ACID properties in database transactions and how Spring's @Transactional annotation manages them.", "Technical", "atomicity, consistency, isolation, durability, rollback, propagation, commit"));
        javaDev.add(new InterviewQuestion(5, "Where do you see yourself in 3 years as a software engineer?", "HR", "growth, technical mastery, ownership, leadership, impact, learning"));
        ROLE_QUESTION_BANK.put("Java Backend Developer", javaDev);

        // Data Analyst & AI
        List<InterviewQuestion> dataAi = new ArrayList<>();
        dataAi.add(new InterviewQuestion(1, "Explain the difference between INNER JOIN, LEFT JOIN, and FULL OUTER JOIN in SQL with an example.", "Technical", "inner join, left join, outer join, null, match, primary key, foreign key"));
        dataAi.add(new InterviewQuestion(2, "How do you handle missing values and outliers in a dataset prior to running statistical models?", "Technical", "imputation, mean, median, iqr, standard deviation, drop, domain knowledge"));
        dataAi.add(new InterviewQuestion(3, "What is the difference between Precision, Recall, and F1-score? When would you prioritize Recall over Precision?", "Technical", "true positive, false positive, false negative, trade-off, fraud detection, medical diagnosis"));
        dataAi.add(new InterviewQuestion(4, "How would you explain a complex data metric or model prediction to a non-technical recruiter or stakeholder?", "Behavioral", "simplification, analogy, business value, visual, clarity, active listening"));
        dataAi.add(new InterviewQuestion(5, "Describe how you have used Python libraries like Pandas or NumPy for exploratory data analysis.", "Technical", "dataframe, groupby, aggregation, visualization, slicing, cleaning"));
        ROLE_QUESTION_BANK.put("Data Analyst & AI", dataAi);
    }

    public InterviewSession startSession(String userId, String studentName, String role) {
        InterviewSession session = new InterviewSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStudentName(studentName);
        session.setJobRole((role != null && !role.trim().isEmpty()) ? role : "Full Stack Developer");

        List<InterviewQuestion> questions = ROLE_QUESTION_BANK.getOrDefault(session.getJobRole(), ROLE_QUESTION_BANK.get("Full Stack Developer"));
        List<InterviewQuestion> sessionQuestions = new ArrayList<>();
        for (InterviewQuestion q : questions) {
            sessionQuestions.add(new InterviewQuestion(q.getId(), q.getQuestionText(), q.getCategory(), q.getExpectedConcepts()));
        }
        session.setQuestions(sessionQuestions);
        activeSessions.put(session.getSessionId(), session);
        return session;
    }

    public InterviewSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public Map<String, Object> evaluateAnswer(String sessionId, int questionId, String userAnswer) {
        InterviewSession session = activeSessions.get(sessionId);
        Map<String, Object> result = new HashMap<>();

        if (session == null) {
            result.put("success", false);
            result.put("error", "Session not found");
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
            result.put("error", "Question not found in session");
            return result;
        }

        currentQ.setUserAnswer(userAnswer);

        // Evaluation heuristic based on concept coverage, length, and structure
        int score = 0;
        StringBuilder feedback = new StringBuilder();

        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            score = 0;
            feedback.append("No response provided. In a live interview, attempt to structure your thoughts even if unsure.");
        } else {
            String lowerAnswer = userAnswer.toLowerCase();
            String[] expected = currentQ.getExpectedConcepts().toLowerCase().split(",\\s*");
            int matches = 0;
            for (String kw : expected) {
                if (lowerAnswer.contains(kw.trim())) {
                    matches++;
                }
            }

            int wordCount = userAnswer.trim().split("\\s+").length;

            if (matches >= 4 && wordCount >= 30) {
                score = 9;
                feedback.append("Excellent answer! You demonstrated solid depth, articulated core concepts clearly, and covered key architectural nuances.");
            } else if (matches >= 2 && wordCount >= 20) {
                score = 7;
                feedback.append("Good answer. You hit the key points. To elevate your rating to top tier, include a concrete production example or discuss trade-offs.");
            } else if (wordCount >= 15) {
                score = 5;
                feedback.append("Fair attempt. Your answer is on the right track, but misses several fundamental keywords: ").append(currentQ.getExpectedConcepts()).append(".");
            } else {
                score = 3;
                feedback.append("Brief answer. Elaborate with the STAR method (Situation, Task, Action, Result) or discuss the underlying mechanism in more depth.");
            }
        }

        currentQ.setScore(score);
        currentQ.setAiFeedback(feedback.toString());

        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);

        result.put("success", true);
        result.put("score", score);
        result.put("feedback", feedback.toString());
        result.put("nextQuestionIndex", session.getCurrentQuestionIndex());
        result.put("isCompleted", session.getCurrentQuestionIndex() >= session.getQuestions().size());

        return result;
    }

    public InterviewSession finishSession(String sessionId) {
        InterviewSession session = activeSessions.get(sessionId);
        if (session == null) return null;

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

        // Generate qualitative evaluation
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (techScore >= 70) {
            strengths.add("Strong foundational knowledge in " + session.getJobRole() + " core concepts.");
        } else {
            weaknesses.add("Review core technical definitions and architectural patterns for " + session.getJobRole() + ".");
            recommendations.add("Spend 30 minutes daily reviewing system design principles and standard interview questions.");
        }

        if (commScore >= 70) {
            strengths.add("Articulate and structured communication using professional terminology.");
        } else {
            weaknesses.add("Practice answering with the STAR method (Situation, Task, Action, Result) to avoid brief answers.");
            recommendations.add("Record yourself speaking your technical explanations aloud to build cadence.");
        }

        strengths.add("Maintained active engagement and answered all assigned assessment prompts.");
        weaknesses.add("Could provide more real-world project context or quantifiable metrics in behavioral questions.");
        recommendations.add("Tailor examples directly to candidate experiences from your uploaded resume.");

        session.setStrengths(strengths);
        session.setWeaknesses(weaknesses);
        session.setRecommendations(recommendations);

        if (overall >= 75) {
            session.setSummary("Candidate demonstrates high placement readiness for " + session.getJobRole() + ". Recommended for on-campus company technical rounds.");
        } else if (overall >= 60) {
            session.setSummary("Candidate shows solid potential with moderate placement readiness. Needs targeted practice on system design and technical elaboration.");
        } else {
            session.setSummary("Candidate requires further conceptual preparation before attending tier-1 recruitment drives. Review recommended practice topics.");
        }

        // Save into live Firebase Realtime Database
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", session.getSessionId());
            data.put("userId", session.getUserId());
            data.put("studentName", session.getStudentName());
            data.put("jobRole", session.getJobRole());
            data.put("totalScore", overall);
            data.put("overallScore", overall);
            data.put("techScore", techScore);
            data.put("commScore", commScore);
            data.put("summary", session.getSummary());
            data.put("strengths", session.getStrengths());
            data.put("weaknesses", session.getWeaknesses());
            data.put("recommendations", session.getRecommendations());
            data.put("timestamp", System.currentTimeMillis());

            firebaseService.saveMockInterview(session.getUserId(), data);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("interviewScore", overall);
            firebaseService.updateStudentMetrics(session.getUserId(), metrics);
        } catch (Exception e) {
            System.err.println("Note: error syncing mock interview to Firebase: " + e.getMessage());
        }

        // Save into local memory cache
        userSessionHistory.computeIfAbsent(session.getUserId(), k -> new ArrayList<>()).add(0, session);

        return session;
    }

    public List<InterviewSession> getUserHistory(String userId) {
        List<InterviewSession> memoryList = userSessionHistory.getOrDefault(userId, Collections.emptyList());
        if (!memoryList.isEmpty()) {
            return memoryList;
        }

        // Fetch live from Firebase Realtime Database
        List<Map<String, Object>> liveList = firebaseService.fetchMockInterviews(userId);
        List<InterviewSession> result = new ArrayList<>();
        for (Map<String, Object> item : liveList) {
            InterviewSession s = new InterviewSession();
            s.setSessionId((String) item.get("id"));
            s.setUserId((String) item.get("userId"));
            s.setJobRole((String) item.get("jobRole"));
            s.setOverallScore(((Number) item.getOrDefault("overallScore", 0)).intValue());
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
}
