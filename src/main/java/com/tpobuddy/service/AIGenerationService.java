package com.tpobuddy.service;

import com.google.gson.*;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestSeries;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enterprise AI Question Generation Service using Google Gemini AI.
 * Generates structured 50-candidate question pools for College & Platform assessments.
 * Features:
 * - Resilient batching to produce 50 comprehensive candidate questions
 * - Strict JSON schema enforcement
 * - Difficulty complexity tuning (Medium, Hard, Extra Hard)
 * - Exponential backoff retries with HTTP timeout protection
 * - Automatic fallback to verified placement repository on AI unavailability
 * - Absolute protection of API credentials (never logged or exposed)
 */
@Service
public class AIGenerationService {

    private final QuestionValidationService validationService;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private final AtomicInteger generatedIdCounter = new AtomicInteger(900000);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-flash-lite-latest}")
    private String geminiModel;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/}")
    private String geminiApiUrl;

    public AIGenerationService(QuestionValidationService validationService) {
        this.validationService = validationService;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Generates a pool of candidate questions (target: 50) for the specified series and difficulty.
     */
    public List<Question> generateCandidatePool(TestSeries series, String difficulty, List<String> customTopics, int targetPoolSize) {
        if (targetPoolSize <= 0) targetPoolSize = 50;
        String effDifficulty = (difficulty != null && !difficulty.trim().isEmpty()) ? difficulty : "Medium";

        String apiKey = getEffectiveApiKey();
        List<Question> candidatePool = new ArrayList<>();

        // If Gemini API is configured, attempt AI generation in 2 resilient batches
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                int batch1Size = (int) Math.ceil(targetPoolSize / 2.0);
                int batch2Size = targetPoolSize - batch1Size;

                List<Question> batch1 = generateBatchWithGemini(series, effDifficulty, customTopics, batch1Size, 1, apiKey);
                if (batch1 != null) candidatePool.addAll(batch1);

                List<Question> batch2 = generateBatchWithGemini(series, effDifficulty, customTopics, batch2Size, 2, apiKey);
                if (batch2 != null) candidatePool.addAll(batch2);

                // Run strict validation and deduplication
                candidatePool = validationService.filterValidPool(candidatePool);
                System.out.println("✨ AI Question Generator produced " + candidatePool.size() + " validated candidates.");
            } catch (Exception e) {
                System.err.println("Notice: AI generation encountered an issue (" + e.getClass().getSimpleName() + "). Engaging verified repository fallback.");
            }
        }

        // If pool is under targetPoolSize (e.g. API unconfigured, rate-limited, or partial failure),
        // seamlessly top up using verified placement repository questions
        if (candidatePool.size() < targetPoolSize) {
            int needed = targetPoolSize - candidatePool.size();
            List<Question> fallbackQuestions = fetchFallbackQuestions(series, effDifficulty, customTopics, needed, candidatePool);
            candidatePool.addAll(fallbackQuestions);
        }

        return candidatePool;
    }

    private List<Question> generateBatchWithGemini(TestSeries series, String difficulty, List<String> topics,
                                                   int count, int batchNum, String apiKey) throws Exception {
        String prompt = buildGenerationPrompt(series, difficulty, topics, count, batchNum);
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
        genConfig.addProperty("temperature", 0.4);
        genConfig.addProperty("topP", 0.95);
        genConfig.addProperty("maxOutputTokens", 8192);
        genConfig.addProperty("responseMimeType", "application/json");
        payload.add("generationConfig", genConfig);

        String rawResponse = executeHttpRequestWithRetry(endpoint, payload.toString(), 2);
        if (rawResponse == null || rawResponse.isEmpty()) {
            return Collections.emptyList();
        }

        return parseGeminiResponse(rawResponse, series, difficulty);
    }

    private String buildGenerationPrompt(TestSeries series, String difficulty, List<String> topics, int count, int batchNum) {
        String seriesName = (series != null) ? series.getName() : "Placement Assessment";
        String subject = (series != null) ? series.getSubject() : "Core Placement Topics";
        String category = (series != null) ? series.getCategory() : "COLLEGE";

        List<String> topicList = (topics != null && !topics.isEmpty())
                ? topics
                : (series != null && series.getSyllabusTopics() != null ? series.getSyllabusTopics() : List.of("General Assessment"));

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert technical assessment creator for engineering college campus placements.\n");
        sb.append("Generate exactly ").append(count).append(" high-quality, non-duplicate multiple choice questions.\n");
        sb.append("Batch: ").append(batchNum).append("\n");
        sb.append("Assessment Category: ").append(category).append("\n");
        sb.append("Series Name: ").append(seriesName).append("\n");
        sb.append("Subject/Domain: ").append(subject).append("\n");
        sb.append("Difficulty Level: ").append(difficulty).append("\n");

        sb.append("\nDifficulty Complexity Guidelines:\n");
        switch (difficulty.toLowerCase()) {
            case "hard" -> sb.append("- HARD: Involve multi-step logical derivations, moderate algorithmic complexity, tricky pointer/operator precedence, and applied technical concepts. Require analytical reasoning.\n");
            case "extra hard", "extrahard" -> sb.append("- EXTRA HARD: Advanced technical reasoning, edge cases, subtle concurrency/memory gotchas, complex combinatorial logic, and multi-concept problem solving. Distractors must be deeply plausible.\n");
            default -> sb.append("- MEDIUM: Core standard campus placement questions, fundamental concepts, and straightforward mathematical/programming applications.\n");
        }

        sb.append("\nCover the following syllabus topics:\n");
        for (String t : topicList) {
            sb.append("- ").append(t).append("\n");
        }

        sb.append("\nRules for generation:\n");
        sb.append("1. Exactly 4 options for each question (optionA, optionB, optionC, optionD).\n");
        sb.append("2. Exactly one unambiguously correct answer (correctOption: 0 for A, 1 for B, 2 for C, 3 for D).\n");
        sb.append("3. For Reading Comprehension, provide the complete reading passage in 'passageOrCode'.\n");
        sb.append("4. For Output Prediction questions, provide the exact clean code snippet in 'passageOrCode' or within 'question'.\n");
        sb.append("5. Provide a detailed, step-by-step solution derivation in 'explanation'.\n");
        sb.append("6. NEVER reveal or embed the correct answer in the question statement.\n");
        sb.append("7. Return ONLY a valid JSON array of question objects without markdown wrapping.\n");

        sb.append("\nJSON Schema:\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"question\": \"Question statement...\",\n");
        sb.append("    \"section\": \"Section Name (e.g. Quantitative Aptitude / Logical Reasoning / Verbal Ability / Technical and Coding)\",\n");
        sb.append("    \"topic\": \"Specific Topic Name\",\n");
        sb.append("    \"difficulty\": \"").append(difficulty).append("\",\n");
        sb.append("    \"questionType\": \"MCQ | OUTPUT_PREDICTION | READING_COMPREHENSION\",\n");
        sb.append("    \"passageOrCode\": \"Optional passage or code block\",\n");
        sb.append("    \"optionA\": \"First choice\",\n");
        sb.append("    \"optionB\": \"Second choice\",\n");
        sb.append("    \"optionC\": \"Third choice\",\n");
        sb.append("    \"optionD\": \"Fourth choice\",\n");
        sb.append("    \"correctOption\": 0,\n");
        sb.append("    \"explanation\": \"Detailed step-by-step explanation...\"\n");
        sb.append("  }\n");
        sb.append("]\n");

        return sb.toString();
    }

    private List<Question> parseGeminiResponse(String responseJson, TestSeries series, String difficulty) {
        List<Question> list = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
            if (!root.has("candidates") || root.getAsJsonArray("candidates").isEmpty()) {
                return list;
            }
            JsonObject cand = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
            JsonObject content = cand.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts.isEmpty()) return list;

            String text = parts.get(0).getAsJsonObject().get("text").getAsString().trim();
            // Clean markdown code blocks if present
            if (text.startsWith("```json")) text = text.substring(7);
            if (text.startsWith("```")) text = text.substring(3);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
            text = text.trim();

            JsonElement parsed = JsonParser.parseString(text);
            JsonArray arr = parsed.isJsonArray() ? parsed.getAsJsonArray() :
                    (parsed.isJsonObject() && parsed.getAsJsonObject().has("questions") ?
                            parsed.getAsJsonObject().getAsJsonArray("questions") : null);

            if (arr != null) {
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        Question q = new Question();
                        q.setId(generatedIdCounter.incrementAndGet());
                        q.setQuestion(getString(obj, "question", ""));
                        q.setCategory(series != null ? series.getName() : "Placement Series");
                        q.setSection(getString(obj, "section", (series != null ? series.getSubject() : "General")));
                        q.setTopic(getString(obj, "topic", "General Assessment"));
                        q.setDifficulty(getString(obj, "difficulty", difficulty));
                        q.setQuestionType(getString(obj, "questionType", "MCQ"));
                        q.setPassageOrCode(getString(obj, "passageOrCode", ""));
                        q.setOptionA(getString(obj, "optionA", ""));
                        q.setOptionB(getString(obj, "optionB", ""));
                        q.setOptionC(getString(obj, "optionC", ""));
                        q.setOptionD(getString(obj, "optionD", ""));
                        q.setCorrectOption(getInt(obj, "correctOption", 0));
                        q.setExplanation(getString(obj, "explanation", ""));
                        q.setMarks(2);
                        list.add(q);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini JSON output: " + e.getMessage());
        }
        return list;
    }

    private List<Question> fetchFallbackQuestions(TestSeries series, String difficulty, List<String> customTopics,
                                                  int countNeeded, List<Question> existingPool) {
        Set<String> existingNormalizedQuestions = new HashSet<>();
        for (Question q : existingPool) {
            if (q.getQuestion() != null) {
                existingNormalizedQuestions.add(q.getQuestion().trim().toLowerCase());
            }
        }

        List<Question> bank = PlacementQuestionBank.getAllQuestions();
        Collections.shuffle(bank);

        List<Question> matching = new ArrayList<>();
        // Priority 1: match difficulty and subject
        for (Question q : bank) {
            if (matching.size() >= countNeeded) break;
            if (!existingNormalizedQuestions.contains(q.getQuestion().trim().toLowerCase())) {
                boolean diffMatch = q.getDifficulty() != null && q.getDifficulty().equalsIgnoreCase(difficulty);
                if (diffMatch) {
                    Question clone = cloneQuestion(q);
                    matching.add(clone);
                    existingNormalizedQuestions.add(q.getQuestion().trim().toLowerCase());
                }
            }
        }

        // Priority 2: fill remaining regardless of difficulty tag to guarantee candidate pool
        if (matching.size() < countNeeded) {
            for (Question q : bank) {
                if (matching.size() >= countNeeded) break;
                if (!existingNormalizedQuestions.contains(q.getQuestion().trim().toLowerCase())) {
                    Question clone = cloneQuestion(q);
                    clone.setDifficulty(difficulty);
                    matching.add(clone);
                    existingNormalizedQuestions.add(q.getQuestion().trim().toLowerCase());
                }
            }
        }

        return matching;
    }

    private Question cloneQuestion(Question src) {
        Question q = new Question();
        q.setId(generatedIdCounter.incrementAndGet());
        q.setCategory(src.getCategory());
        q.setSection(src.getSection() != null && !src.getSection().isEmpty() ? src.getSection() : src.getCategory());
        q.setTopic(src.getTopic());
        q.setDifficulty(src.getDifficulty());
        q.setQuestionType(src.getQuestionType());
        q.setPassageOrCode(src.getPassageOrCode());
        q.setQuestion(src.getQuestion());
        q.setOptionA(src.getOptionA());
        q.setOptionB(src.getOptionB());
        q.setOptionC(src.getOptionC());
        q.setOptionD(src.getOptionD());
        q.setCorrectOption(src.getCorrectOption());
        q.setExplanation(src.getExplanation());
        q.setMarks(src.getMarks() > 0 ? src.getMarks() : 2);
        return q;
    }

    private String executeHttpRequestWithRetry(String urlStr, String jsonBody, int maxRetries) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlStr))
                        .timeout(Duration.ofSeconds(25))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response.body();
                } else if (status == 429) {
                    // Rate limited: backoff and retry
                    long sleepMs = (long) Math.pow(2, attempt) * 1500;
                    Thread.sleep(sleepMs);
                } else {
                    System.err.println("Gemini HTTP " + status + " on attempt " + attempt);
                }
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    System.err.println("Gemini request failed after " + maxRetries + " attempts: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private String getEffectiveApiKey() {
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.contains("YOUR_")) {
            return geminiApiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }
        return Config.get("gemini.api.key", "");
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
