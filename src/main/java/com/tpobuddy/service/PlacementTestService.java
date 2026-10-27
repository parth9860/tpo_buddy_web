package com.tpobuddy.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tpobuddy.model.PlacementTest;
import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestAttempt;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service managing Company Placement Tests, administrative test creation,
 * duplication, CSV ingestion, and student performance analytics.
 */
@Service
public class PlacementTestService {

    private final FirebaseService firebaseService;
    private final Map<String, PlacementTest> testRegistry = new ConcurrentHashMap<>();

    public PlacementTestService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
        initializeDefaultPlacementTests();
    }

    private void initializeDefaultPlacementTests() {
        // Section 1: Quantitative Aptitude
        registerTest(new PlacementTest("test_quant", "Quantitative Aptitude Placement Test",
                "Comprehensive arithmetic and numerical reasoning covering 21 core campus topics.",
                "Quantitative Aptitude", "Comprehensive", "Medium", 25, 30));

        // Section 2: Logical Reasoning
        registerTest(new PlacementTest("test_logical", "Logical Reasoning Assessment",
                "Deductive, analytical, coding-decoding, and pattern recognition assessments for placements.",
                "Logical Reasoning", "Comprehensive", "Medium", 25, 30));

        // Section 3: Verbal Ability
        registerTest(new PlacementTest("test_verbal", "Verbal Ability & English Communication",
                "Grammar, error spotting, sentence improvement, synonyms, and comprehension.",
                "Verbal Ability", "Comprehensive", "Easy", 25, 25));

        // Section 4: Technical & Coding
        registerTest(new PlacementTest("test_tech", "Technical & Coding Assessment",
                "Java OOP, C, Python basics, Data Structures, DBMS, SQL, and Operating Systems.",
                "Technical and Coding", "Core CS", "Medium", 25, 30));

        // Priority Topic 1: Number System
        registerTest(new PlacementTest("topic_num_system", "Number System Special Practice",
                "Unit digits, remainders, factors, and divisibility patterns.",
                "Quantitative Aptitude", "Number System", "Medium", 10, 15));

        // Priority Topic 2: HCF & LCM
        registerTest(new PlacementTest("topic_hcf_lcm", "HCF & LCM Practice Test",
                "Co-primes, bells chiming, and algebraic factorization methods.",
                "Quantitative Aptitude", "HCF and LCM", "Medium", 10, 15));

        // Priority Topic 3: Divisibility Rules
        registerTest(new PlacementTest("topic_divisibility", "Divisibility Rules & Remainders",
                "Divisibility rules for 7, 8, 9, 11, and composite divisors.",
                "Quantitative Aptitude", "Divisibility Rules", "Medium", 10, 15));

        // Priority Topic 4: Percentages
        registerTest(new PlacementTest("topic_percentages", "Percentages & Applications",
                "Successive percentage changes, population models, and expenditure balancing.",
                "Quantitative Aptitude", "Percentages", "Medium", 10, 15));

        // Priority Topic 5: Profit & Loss
        registerTest(new PlacementTest("topic_profit_loss", "Profit, Loss & Dishonest Dealers",
                "False weights, cost price ratios, marked prices, and overall profit.",
                "Quantitative Aptitude", "Profit and Loss", "Medium", 10, 15));

        // Priority Topic 6: Discounts
        registerTest(new PlacementTest("topic_discounts", "Discounts & Successive Rebates",
                "Single equivalent discount, buy-get-free offers, and margin calculations.",
                "Quantitative Aptitude", "Discounts", "Easy", 10, 15));

        // Full Placement Mock
        PlacementTest fullMock = new PlacementTest("test_full_mock", "Full Placement Mock Examination",
                "Full-length placement simulator mixing Quantitative Aptitude, Logical Reasoning, Verbal, and Technical.",
                "Comprehensive Mock", "Full Mock", "Hard", 25, 35);
        fullMock.setNegativeMarking(true);
        fullMock.setNegativeMarks(0.5);
        registerTest(fullMock);

        // Company Mocks
        registerTest(new PlacementTest("company_tcs", "TCS NQT National Placement Simulator",
                "TCS NQT cognitive and technical assessment pattern simulator.",
                "Company Mock", "TCS", "Medium", 25, 30));

        registerTest(new PlacementTest("company_infosys", "Infosys Campus Assessment Simulator",
                "Infosys test pattern with reasoning puzzles, pseudocode, and mathematical abilities.",
                "Company Mock", "Infosys", "Medium", 25, 30));

        registerTest(new PlacementTest("company_wipro", "Wipro Elite NLTH Assessment",
                "Wipro National Level Talent Hunt test pattern simulator.",
                "Company Mock", "Wipro", "Medium", 25, 30));

        registerTest(new PlacementTest("company_accenture", "Accenture Cognitive & Technical Test",
                "Accenture critical thinking, abstract reasoning, and coding fundamentals.",
                "Company Mock", "Accenture", "Medium", 25, 30));
    }

    public void registerTest(PlacementTest test) {
        if (test != null && test.getId() != null) {
            testRegistry.put(test.getId(), test);
        }
    }

    public List<PlacementTest> getAllTests() {
        return new ArrayList<>(testRegistry.values());
    }

    public PlacementTest getTestById(String testId) {
        return testRegistry.get(testId);
    }

    public boolean saveTest(PlacementTest test) {
        if (test == null) return false;
        if (test.getId() == null || test.getId().trim().isEmpty()) {
            test.setId("test_" + System.currentTimeMillis());
        }
        testRegistry.put(test.getId(), test);
        return true;
    }

    public PlacementTest duplicateTest(String testId) {
        PlacementTest orig = testRegistry.get(testId);
        if (orig == null) return null;

        PlacementTest clone = new PlacementTest();
        clone.setId("test_copy_" + System.currentTimeMillis());
        clone.setTitle("Copy of " + orig.getTitle());
        clone.setDescription(orig.getDescription());
        clone.setCategory(orig.getCategory());
        clone.setSubCategory(orig.getSubCategory());
        clone.setDifficulty(orig.getDifficulty());
        clone.setQuestionCount(orig.getQuestionCount());
        clone.setDurationMinutes(orig.getDurationMinutes());
        clone.setTotalMarks(orig.getTotalMarks());
        clone.setMarksPerQuestion(orig.getMarksPerQuestion());
        clone.setPassPercentage(orig.getPassPercentage());
        clone.setTargetBranch(orig.getTargetBranch());
        clone.setTargetYear(orig.getTargetYear());
        clone.setAllowedAttempts(orig.getAllowedAttempts());
        clone.setStatus("DRAFT");
        clone.setNegativeMarking(orig.isNegativeMarking());
        clone.setNegativeMarks(orig.getNegativeMarks());
        clone.setRandomizeQuestions(orig.isRandomizeQuestions());
        clone.setInstructions(orig.getInstructions());

        testRegistry.put(clone.getId(), clone);
        return clone;
    }

    public boolean updateTestStatus(String testId, String newStatus) {
        PlacementTest t = testRegistry.get(testId);
        if (t != null) {
            t.setStatus(newStatus);
            return true;
        }
        return false;
    }

    public boolean deleteTest(String testId) {
        return testRegistry.remove(testId) != null;
    }

    // =========================================================================
    // Analytics & Weak Topics Calculation
    // =========================================================================

    public Map<String, Object> calculateSeriesAnalytics() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<PlacementTest> tests = getAllTests();
        List<TestAttempt> attempts = firebaseService.fetchAllTestAttempts();

        map.put("totalTests", tests.size());

        Set<String> categories = tests.stream().map(PlacementTest::getCategory).collect(Collectors.toSet());
        map.put("totalCategories", categories.size());

        int totalAttempts = (attempts != null) ? attempts.size() : 0;
        map.put("totalAttempts", totalAttempts);

        if (totalAttempts > 0) {
            double avgScore = attempts.stream().mapToInt(TestAttempt::getScore).average().orElse(0.0);
            long passedCount = attempts.stream().filter(TestAttempt::isPassed).count();
            double passRate = (passedCount * 100.0) / totalAttempts;

            map.put("averageScore", (int) Math.round(avgScore));
            map.put("passRate", (int) Math.round(passRate));

            // Find weak topics (topics with average score < 60%)
            Map<String, List<Integer>> topicScores = new HashMap<>();
            for (TestAttempt a : attempts) {
                String top = a.getTopic() != null ? a.getTopic() : a.getCategory();
                if (top != null) {
                    topicScores.computeIfAbsent(top, k -> new ArrayList<>()).add(a.getScore());
                }
            }

            List<String> weakTopics = new ArrayList<>();
            for (Map.Entry<String, List<Integer>> entry : topicScores.entrySet()) {
                double avg = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(100.0);
                if (avg < 60.0) {
                    weakTopics.add(entry.getKey() + " (" + (int) Math.round(avg) + "% avg)");
                }
            }

            if (weakTopics.isEmpty()) {
                weakTopics.add("Percentages (Foundational)");
                weakTopics.add("HCF & LCM");
            }
            map.put("weakTopics", weakTopics);
        } else {
            map.put("averageScore", 0);
            map.put("passRate", 0);
            map.put("weakTopics", Arrays.asList("Number System", "Percentages", "HCF & LCM"));
        }

        return map;
    }

    // =========================================================================
    // CSV Ingestion & Export
    // =========================================================================

    public int importQuestionsFromCsv(InputStream inputStream) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] parts = parseCsvLine(line);
                if (parts.length >= 10) {
                    Question q = new Question();
                    q.setCategory(parts[0].trim());
                    q.setTopic(parts[1].trim());
                    q.setDifficulty(parts[2].trim());
                    q.setQuestion(parts[3].trim());
                    q.setOptionA(parts[4].trim());
                    q.setOptionB(parts[5].trim());
                    q.setOptionC(parts[6].trim());
                    q.setOptionD(parts[7].trim());

                    String corr = parts[8].trim().toUpperCase();
                    int cOpt = 0;
                    if (corr.equals("B") || corr.equals("1")) cOpt = 1;
                    else if (corr.equals("C") || corr.equals("2")) cOpt = 2;
                    else if (corr.equals("D") || corr.equals("3")) cOpt = 3;
                    q.setCorrectOption(cOpt);

                    if (parts.length > 9) {
                        try { q.setMarks(Integer.parseInt(parts[9].trim())); } catch (Exception ignored) {}
                    }
                    if (parts.length > 10) {
                        q.setExplanation(parts[10].trim());
                    }

                    PlacementQuestionBank.addQuestion(q);
                    count++;
                }
            }
        } catch (Exception e) {
            System.err.println("CSV import error: " + e.getMessage());
        }
        return count;
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    public String generateAttemptsCsv() {
        List<TestAttempt> attempts = firebaseService.fetchAllTestAttempts();
        StringBuilder csv = new StringBuilder();
        csv.append("Attempt ID,Student UID,Student Name,Assessment Category,Topic,Score (%),Correct Answers,Total Questions,Passed,Risk Level,Proctoring Status,Status,Timestamp\n");

        if (attempts != null) {
            for (TestAttempt a : attempts) {
                csv.append(escapeCsv(a.getId())).append(",")
                        .append(escapeCsv(a.getUserId())).append(",")
                        .append(escapeCsv(a.getStudentName() != null ? a.getStudentName() : "Student")).append(",")
                        .append(escapeCsv(a.getCategory())).append(",")
                        .append(escapeCsv(a.getTopic())).append(",")
                        .append(a.getScore()).append(",")
                        .append(a.getCorrectAnswers()).append(",")
                        .append(a.getTotalQuestions()).append(",")
                        .append(a.isPassed() ? "YES" : "NO").append(",")
                        .append(escapeCsv(a.getRiskLevel() != null ? a.getRiskLevel() : "LOW")).append(",")
                        .append(escapeCsv(a.getProctoringStatus() != null ? a.getProctoringStatus() : "VALIDATED")).append(",")
                        .append(escapeCsv(a.getStatus())).append(",")
                        .append(new Date(a.getSavedAt())).append("\n");
            }
        }
        return csv.toString();
    }

    private String escapeCsv(String str) {
        if (str == null) return "\"\"";
        return "\"" + str.replace("\"", "\"\"") + "\"";
    }
}
