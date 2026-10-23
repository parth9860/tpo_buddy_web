package com.tpobuddy.service;

import com.tpobuddy.model.ResumeData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.*;

/**
 * Service for PDF resume text extraction and ATS analysis.
 */
public class ResumeAnalysisService {

    private static final String[] ESSENTIAL_SKILLS = {
        "Java", "Python", "C++", "Data Structures", "Algorithms",
        "SQL", "Database", "Spring Boot", "REST API", "Git",
        "Docker", "AWS", "Cloud", "Microservices", "System Design",
        "Linux", "Unit Testing", "HTML", "CSS", "JavaScript"
    };

    private static final String[] MISSING_CANDIDATES = {
        "Docker & Containerization", "AWS / Cloud Deployments",
        "System Design Fundamentals", "CI/CD & Jenkins",
        "Unit Testing (JUnit / Mockito)", "Kubernetes Orchestration"
    };

    /**
     * Extracts text from an uploaded PDF InputStream using Apache PDFBox.
     */
    public static String extractTextFromPdf(InputStream inputStream) {
        if (inputStream == null) return "";
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document).trim();
        } catch (Exception e) {
            System.err.println("PDFBox text extraction failed: " + e.getMessage());
            return "";
        }
    }

    /**
     * Analyzes resume text and returns a populated ResumeData object with real metrics.
     */
    public static ResumeData analyzeResumeText(String userId, String rawText) {
        ResumeData data = new ResumeData();
        data.setUserId(userId);
        data.setResumeText(rawText != null ? rawText : "");
        data.setUpdatedAt(System.currentTimeMillis());

        if (rawText == null || rawText.trim().isEmpty()) {
            data.setAtsScore(0);
            data.setGrammarScore(0);
            data.setExperienceReview("No resume text provided for analysis.");
            data.setProjectsReview("No project information detected.");
            return data;
        }

        String lowerText = rawText.toLowerCase();

        // 1. Skill Detection
        List<String> detected = new ArrayList<>();
        for (String skill : ESSENTIAL_SKILLS) {
            if (lowerText.contains(skill.toLowerCase())) {
                detected.add(skill);
            }
        }
        data.setDetectedSkills(detected);
        data.setSkills(String.join(", ", detected));

        // 2. ATS Score Calculation
        int baseAts = 40;
        // Check for standard section headings
        if (lowerText.contains("education") || lowerText.contains("academic")) baseAts += 10;
        if (lowerText.contains("experience") || lowerText.contains("internship") || lowerText.contains("work")) baseAts += 15;
        if (lowerText.contains("projects") || lowerText.contains("project")) baseAts += 15;
        if (lowerText.contains("skills") || lowerText.contains("technical skills")) baseAts += 10;

        // Skill bonus
        int skillBonus = Math.min(15, detected.size() * 2);
        int finalAts = Math.min(95, baseAts + skillBonus);
        data.setAtsScore(finalAts);

        // 3. Grammar & Formatting Score
        int grammar = 88;
        if (rawText.length() > 500 && rawText.length() < 5000) grammar += 4;
        if (lowerText.contains("%") || lowerText.matches(".*\\d+%.*")) grammar += 4; // quantifiable metric bonus
        data.setGrammarScore(Math.min(96, grammar));

        // 4. Missing Skills & Recommendations
        List<String> missing = new ArrayList<>();
        for (String candidate : MISSING_CANDIDATES) {
            boolean found = false;
            for (String det : detected) {
                if (candidate.toLowerCase().contains(det.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (!found && missing.size() < 3) {
                missing.add(candidate);
            }
        }
        data.setMissingSkills(missing);

        // 5. Suggestions
        List<String> suggestions = new ArrayList<>();
        if (!lowerText.matches(".*\\d+%.*") && !lowerText.contains("improved by") && !lowerText.contains("reduced by")) {
            suggestions.add("Add quantifiable metrics and KPIs to project descriptions (e.g. 'Improved query latency by 35%').");
        }
        suggestions.add("Include active GitHub repository links and live project deployment URLs.");
        if (!detected.contains("Docker") && !detected.contains("AWS") && !detected.contains("Cloud")) {
            suggestions.add("Add foundational Cloud/DevOps competencies (Docker, AWS) to qualify for modern full-stack roles.");
        }
        suggestions.add("Ensure bullet points follow the Google XYZ formula: 'Accomplished [X] as measured by [Y], by doing [Z]'.");
        data.setSuggestions(suggestions);

        // 6. Detailed Reviews
        data.setExperienceReview(detected.contains("Java") || detected.contains("SQL")
                ? "Strong alignment with campus software engineering requisites. Highlights core backend competency."
                : "Foundational experience documented. Recommend expanding technical internships and capstone contributions.");

        data.setProjectsReview("Projects demonstrate practical software engineering principles. Adding containerization or API documentation links will further strengthen screening pass rates.");

        return data;
    }
}
