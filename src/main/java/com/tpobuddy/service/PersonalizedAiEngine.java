package com.tpobuddy.service;

import com.tpobuddy.model.ResumeData;
import com.tpobuddy.model.StudentMetrics;
import com.tpobuddy.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of Android app's PersonalizedAiEngine.
 * Formulates personalized, context-aware AI responses and recommendations based on live student data.
 */
public class PersonalizedAiEngine {

    public static String generateWelcomeMessage(String name, int readinessScore, String weakTopic, int streakDays) {
        String studentName = (name != null && !name.trim().isEmpty()) ? name : "Student";
        if (weakTopic != null && !weakTopic.isEmpty()) {
            return "Welcome back, " + studentName + "! Your overall readiness is at " + readinessScore + "%. Focus on strengthening " + weakTopic + " today to maximize shortlist probability.";
        }
        if (readinessScore >= 80) {
            return "Outstanding progress, " + studentName + "! You are at " + readinessScore + "% placement readiness with an active " + streakDays + "-day consistency streak. Ready for top-tier drives!";
        }
        if (readinessScore > 0) {
            return "Good day, " + studentName + "! You have achieved " + readinessScore + "% of your placement readiness target. Complete today's practice tests to level up!";
        }
        return "Welcome to TPO Buddy, " + studentName + "! Take a quick 10-minute assessment test and upload your resume to generate your personalized AI placement roadmap.";
    }

    public static String generateChatResponse(String query, User user, StudentMetrics metrics, ResumeData resume) {
        if (query == null || query.trim().isEmpty()) {
            return "Hello! I am TPO Buddy, your AI Placement Assistant. How can I help you prepare for upcoming campus recruitment drives today?";
        }

        String q = query.toLowerCase().trim();
        String name = user != null && user.getName() != null ? user.getName() : "Student";
        String branch = user != null && user.getBranch() != null ? user.getBranch() : "Computer Science";

        int readiness = metrics != null ? metrics.getReadinessScore() : 0;
        int dsa = metrics != null ? metrics.getDsaSkill() : 0;
        int java = metrics != null ? metrics.getJavaSkill() : 0;
        int sql = metrics != null ? metrics.getSqlSkill() : 0;
        int aptitude = metrics != null ? metrics.getAptitudeSkill() : 0;
        int atsScore = resume != null ? resume.getAtsScore() : (metrics != null ? metrics.getResumeScore() : 0);

        if (q.contains("study") || q.contains("today") || q.contains("routine") || q.contains("plan")) {
            if (dsa > 0 && dsa < 70) {
                return "Hi " + name + "! Looking at your live performance records, your DSA proficiency is at " + dsa + "%. Here is your high-priority study plan for today:\n\n" +
                        "1. **DSA Core** (45 mins) — Sliding Window, Two-Pointer technique & Binary Search\n" +
                        "2. **Java / OOP Principles** (30 mins) — Polymorphism, Abstract classes, Interface contracts\n" +
                        "3. **Speed Assessment** (20 mins) — Take 1 Quantitative Aptitude test in the Tests tab\n" +
                        "4. **System Architecture** (25 mins) — Review database normalization & indexing.";
            } else if (sql > 0 && sql < 70) {
                return "Hi " + name + "! Your database proficiency is currently at " + sql + "%. Let's target SQL improvement today:\n\n" +
                        "1. **SQL Joins & Aggregations** (40 mins) — LEFT vs FULL OUTER JOIN, GROUP BY with HAVING clause\n" +
                        "2. **Database Performance** (25 mins) — Clustered vs Non-clustered indexing and B-Trees\n" +
                        "3. **Mock Assessment** (20 mins) — Attempt a Technical test from the dashboard.";
            } else {
                return "Hi " + name + "! Your technical metrics are well-balanced (" + java + "% Java, " + dsa + "% DSA). Focus on interview sharpness today:\n\n" +
                        "1. **Quantitative Speed Practice** (30 mins) — Time, Speed & Distance, Probability\n" +
                        "2. **High-Frequency Technical Questions** (30 mins) — Microservices vs Monolith, ACID properties\n" +
                        "3. **Resume Review** (15 mins) — Add quantifiable project impact metrics.";
            }
        }

        if (q.contains("dsa") || q.contains("data structure") || q.contains("algorithm")) {
            if (dsa > 0) {
                return name + ", your live DSA accuracy in Firebase is currently **" + dsa + "%**. To break into Tier-1 product cutoffs (typically 75%+), practice medium-difficulty problems on Heaps, Dynamic Programming, and Graph Traversals (BFS/DFS).";
            } else {
                return name + ", you haven't taken a DSA test yet! Head over to the **Tests & Assessments** section and complete the 25-question Technical test to baseline your performance.";
            }
        }

        if (q.contains("java") || q.contains("interview questions") || q.contains("questions")) {
            return "Here are 4 critical Java placement questions frequently asked by top recruiters:\n\n" +
                    "1. **HashMap Internal Working:** How does Java 8+ handle hash collisions using Balanced Trees (TreeNode) once bin count exceeds TREEIFY_THRESHOLD (8)?\n" +
                    "2. **Garbage Collection:** What are the generational regions (Eden, Survivor, Tenured/Old), and when does Major GC trigger?\n" +
                    "3. **Concurrency:** Compare `ReentrantLock` vs `synchronized` block in terms of fairness, timed waiting, and interruptibility.\n" +
                    "4. **Polymorphism:** Can static or private methods be overridden? Why does method hiding happen with static methods?";
        }

        if (q.contains("resume") || q.contains("ats") || q.contains("cv")) {
            if (atsScore > 0) {
                return "Your latest resume scored **" + atsScore + "% ATS compatibility**! " +
                        (atsScore >= 80 ? "This is in the optimal range for campus placement ATS screening." :
                                "To elevate your score above 80%, ensure you include key enterprise keywords like Docker, AWS, Microservices, and Git, along with measurable project metrics.");
            } else {
                return "You haven't uploaded an ATS resume yet! Navigate to **AI Resume Analysis** to upload your PDF or paste your resume text for instant ATS scoring and missing skill identification.";
            }
        }

        if (q.contains("readiness") || q.contains("profile") || q.contains("score") || q.contains("status")) {
            return "📊 **Live Placement Profile Analysis for " + name + " (" + branch + ")**:\n\n" +
                    "• **Overall Placement Readiness:** " + (readiness > 0 ? readiness + "%" : "Initial assessment required") + "\n" +
                    "• **Resume ATS Score:** " + (atsScore > 0 ? atsScore + "%" : "Resume not uploaded") + "\n" +
                    "• **Technical / Java Score:** " + (java > 0 ? java + "%" : "Pending") + "\n" +
                    "• **Data Structures & Algorithms:** " + (dsa > 0 ? dsa + "%" : "Pending") + "\n" +
                    "• **SQL & Database Score:** " + (sql > 0 ? sql + "%" : "Pending") + "\n" +
                    "• **Quantitative Aptitude:** " + (aptitude > 0 ? aptitude + "%" : "Pending") + "\n\n" +
                    "Keep up consistent practice to stay ahead in company shortlists!";
        }

        if (q.contains("job") || q.contains("drive") || q.contains("company") || q.contains("eligibility")) {
            return "There are active recruitment drives from **Google Cloud Systems**, **Microsoft AI Labs**, **Amazon Web Services**, and **TCS Digital** in the Jobs tab. Check out your customized match score and apply with 1 click!";
        }

        // Generic placement assistant reply
        return "As your AI Placement Buddy, I can assist you with:\n\n" +
                "• **Personalized Daily Study Plans** (Type: *'What should I study today?'*)\n" +
                "• **Live Readiness & Skill Analysis** (Type: *'Analyze my placement profile'*)\n" +
                "• **High-Frequency Interview Questions** (Type: *'Give me Java interview questions'*)\n" +
                "• **Resume ATS & Skill Gap Review** (Type: *'How can I improve my resume?'*)\n" +
                "• **Company Drive Recommendations** (Type: *'Show available jobs'*)\n\n" +
                "What would you like to focus on right now?";
    }
}
