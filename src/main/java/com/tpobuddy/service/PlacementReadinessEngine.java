package com.tpobuddy.service;

import com.tpobuddy.model.StudentMetrics;
import java.io.Serializable;

/**
 * Port of Android app's PlacementReadinessEngine.
 * Computes placement readiness (0 - 100%) and categorizes student's placement standing.
 */
public class PlacementReadinessEngine {

    public static class ReadinessResult implements Serializable {
        private static final long serialVersionUID = 1L;

        public final int overallScore;
        public final String status;
        public final String aiSummary;
        public final int aptitudeScore;
        public final int dsaScore;
        public final int technicalScore;
        public final int interviewScore;
        public final int resumeScore;
        public final int skillCompletionScore;
        public final int consistencyScore;

        public ReadinessResult(int overallScore, String status, String aiSummary,
                               int aptitudeScore, int dsaScore, int technicalScore,
                               int interviewScore, int resumeScore, int skillCompletionScore,
                               int consistencyScore) {
            this.overallScore = Math.min(100, Math.max(0, overallScore));
            this.status = status;
            this.aiSummary = aiSummary;
            this.aptitudeScore = aptitudeScore;
            this.dsaScore = dsaScore;
            this.technicalScore = technicalScore;
            this.interviewScore = interviewScore;
            this.resumeScore = resumeScore;
            this.skillCompletionScore = skillCompletionScore;
            this.consistencyScore = consistencyScore;
        }
    }

    public static ReadinessResult calculate(StudentMetrics m) {
        if (m == null) {
            return new ReadinessResult(0, "Foundation Stage", "Complete your initial assessment tests to calculate placement readiness.", 0, 0, 0, 0, 0, 0, 0);
        }

        int resume = m.getResumeScore();
        int aptitude = m.getAptitudeSkill();
        int technical = m.getTechnicalScore() > 0 ? m.getTechnicalScore() : Math.round((m.getJavaSkill() + m.getSqlSkill()) / 2f);
        int dsa = m.getDsaSkill();
        int interview = m.getInterviewScore();
        int skillComp = m.getSkillCompletionScore();
        int streak = Math.min(100, m.getStreakDays() * 10);

        // Weighted placement calculation
        // Aptitude: 25%, Technical/Java/SQL: 25%, DSA: 20%, Resume: 15%, Interview: 15%
        double totalWeighted = 0;
        double totalWeight = 0;

        if (aptitude > 0) { totalWeighted += aptitude * 0.25; totalWeight += 0.25; }
        if (technical > 0) { totalWeighted += technical * 0.25; totalWeight += 0.25; }
        if (dsa > 0) { totalWeighted += dsa * 0.20; totalWeight += 0.20; }
        if (resume > 0) { totalWeighted += resume * 0.15; totalWeight += 0.15; }
        if (interview > 0) { totalWeighted += interview * 0.15; totalWeight += 0.15; }

        int overall = totalWeight > 0 ? (int) Math.round(totalWeighted / totalWeight) : 0;
        if (overall == 0 && (resume > 0 || technical > 0 || aptitude > 0)) {
            overall = Math.max(resume, Math.max(technical, aptitude));
        }

        String status;
        String summary;

        if (overall >= 80) {
            status = "Placement Ready (Top Tier)";
            summary = "Excellent preparation profile. You are highly competitive for Tier-1 product and consulting drives.";
        } else if (overall >= 65) {
            status = "Accelerated Stage";
            summary = "Solid foundational profile. Focus on improving your weaker areas to cross the 80% mark for premium offers.";
        } else if (overall >= 40) {
            status = "Developing Competency";
            summary = "Active progress. Increase mock assessments and update your technical resume to qualify for top shortlist criteria.";
        } else {
            status = "Foundation Stage";
            summary = "Preparation journey started. Take your first section assessments and upload a resume to unlock readiness insights.";
        }

        return new ReadinessResult(overall, status, summary, aptitude, dsa, technical, interview, resume, skillComp, streak);
    }
}
