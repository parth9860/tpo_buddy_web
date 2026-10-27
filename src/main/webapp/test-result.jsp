<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%
    User user = (User) request.getAttribute("user");
    TestAttempt attempt = (TestAttempt) request.getAttribute("attempt");
    Map<String, Map<String, Object>> sectionBreakdown = (Map<String, Map<String, Object>>) request.getAttribute("sectionBreakdown");
    List<String> weakTopics = (List<String>) request.getAttribute("weakTopics");
    String reviewsJson = (String) request.getAttribute("reviewsJson");
    Integer reviewsCount = (Integer) request.getAttribute("reviewsCount");
    String recommendedSeriesId = (String) request.getAttribute("recommendedSeriesId");
    String recommendedTestTitle = (String) request.getAttribute("recommendedTestTitle");
    String recommendedSubject = (String) request.getAttribute("recommendedSubject");
    if (recommendedSeriesId == null || recommendedSeriesId.trim().isEmpty()) recommendedSeriesId = "college_snjb_quant";
    if (recommendedTestTitle == null || recommendedTestTitle.trim().isEmpty()) recommendedTestTitle = "Quantitative Aptitude - Number Systems";
    if (recommendedSubject == null || recommendedSubject.trim().isEmpty()) recommendedSubject = "Quantitative Aptitude";

    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");
    long displayTimestamp = (attempt != null && attempt.getSavedAt() > 0) ? attempt.getSavedAt() :
                           ((attempt != null && attempt.getSubmissionTime() > 0) ? attempt.getSubmissionTime() : System.currentTimeMillis());
    String formattedDate = sdf.format(new Date(displayTimestamp));

    int score = (attempt != null) ? attempt.getScore() : 0;
    boolean passed = (attempt != null) && attempt.isPassed();
    int totalQuestions = (attempt != null && attempt.getTotalQuestions() > 0) ? attempt.getTotalQuestions() : 30;
    int correctCount = (attempt != null) ? attempt.getCorrectAnswers() : 0;
    int incorrectCount = (attempt != null) ? attempt.getIncorrectAnswers() : 0;
    int unansweredCount = (attempt != null) ? attempt.getUnansweredCount() : Math.max(0, totalQuestions - (correctCount + incorrectCount));
    int attemptedCount = correctCount + incorrectCount;
    double accuracy = (attempt != null && attempt.getAccuracyPercentage() > 0) ? attempt.getAccuracyPercentage() :
            (attemptedCount > 0 ? Math.round((correctCount * 100.0) / attemptedCount) : 0);

    long timeTakenSecs = (attempt != null) ? attempt.getTimeTakenSeconds() : 0;
    long mins = timeTakenSecs / 60;
    long secs = timeTakenSecs % 60;
    String timeTakenStr = mins + "m " + secs + "s";

    int violations = (attempt != null) ? attempt.getViolationCount() : 0;
    String riskLevel = (attempt != null && attempt.getRiskLevel() != null) ? attempt.getRiskLevel() : (violations >= 2 ? "MODERATE" : "LOW");
    String attemptStatus = (attempt != null && attempt.getStatus() != null) ? attempt.getStatus() : "submitted";
    String title = (attempt != null && attempt.getTestTitle() != null && !attempt.getTestTitle().isEmpty()) ? attempt.getTestTitle() :
            ((attempt != null && attempt.getTopic() != null) ? attempt.getTopic() : "Assessment");
    String category = (attempt != null && attempt.getCategory() != null) ? attempt.getCategory() : "COLLEGE";
    String difficulty = (attempt != null && attempt.getDifficulty() != null) ? attempt.getDifficulty() : "Medium";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><%= title %> — Performance Analysis &amp; Answer Review | TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <style>
        .result-stat-card {
            background: #ffffff;
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-md);
            padding: 1.25rem 1.5rem;
            box-shadow: 0 1px 3px rgba(0,0,0,0.04);
            display: flex;
            flex-direction: column;
            justify-content: center;
        }
        .result-stat-val {
            font-family: var(--font-heading);
            font-size: 2rem;
            font-weight: 800;
            line-height: 1.1;
            margin-top: 0.25rem;
        }
        .result-stat-label {
            font-size: 0.75rem;
            color: var(--text-muted);
            text-transform: uppercase;
            font-weight: 700;
            letter-spacing: 0.04em;
        }
        .filter-chip-btn {
            border: 1px solid var(--border-subtle);
            background: #ffffff;
            color: var(--text-secondary);
            padding: 0.45rem 1rem;
            border-radius: 9999px;
            font-size: 0.85rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .filter-chip-btn:hover {
            border-color: var(--primary);
            color: var(--primary);
        }
        .filter-chip-btn.active {
            background: var(--primary);
            border-color: var(--primary);
            color: #ffffff;
            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
        }
        .review-question-card {
            background: #ffffff;
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-md);
            padding: 1.5rem;
            margin-bottom: 1.25rem;
            box-shadow: 0 1px 3px rgba(0,0,0,0.03);
            transition: border-color 0.2s ease;
        }
        .review-question-card.correct {
            border-left: 5px solid #10b981;
        }
        .review-question-card.incorrect {
            border-left: 5px solid #ef4444;
        }
        .review-question-card.unanswered {
            border-left: 5px solid #94a3b8;
        }
        .option-review-item {
            padding: 0.75rem 1rem;
            border-radius: var(--radius-sm);
            border: 1px solid var(--border-subtle);
            margin-bottom: 0.5rem;
            font-size: 0.9rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            transition: all 0.15s ease;
        }
        .option-review-item.is-correct-target {
            background: #f0fdf4;
            border-color: #86efac;
            color: #166534;
            font-weight: 600;
        }
        .option-review-item.is-user-selected-wrong {
            background: #fef2f2;
            border-color: #fca5a5;
            color: #991b1b;
            font-weight: 600;
        }
        .option-review-item.is-user-selected-right {
            background: #f0fdf4;
            border-color: #4ade80;
            color: #15803d;
            font-weight: 700;
        }
        .explanation-box {
            background: #f8fafc;
            border-left: 4px solid var(--primary);
            border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
            padding: 1rem 1.25rem;
            margin-top: 1rem;
            font-size: 0.875rem;
            line-height: 1.6;
            color: var(--text-primary);
        }
    </style>
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="app-shell">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="app-main">
            <!-- Breadcrumbs & Nav -->
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.75rem;">
                <div style="display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; color: var(--text-muted);">
                    <a href="${pageContext.request.contextPath}/tests" style="color: var(--primary); text-decoration: none; font-weight: 600;">← Back to AI Test Series</a>
                    <span>/</span>
                    <span>Assessment Evaluation &amp; Solutions</span>
                </div>
                <div style="display: flex; gap: 0.5rem;">
                    <a href="${pageContext.request.contextPath}/tests" class="btn btn-outline btn-sm">All Tests</a>
                    <a href="${pageContext.request.contextPath}/progress" class="btn btn-outline btn-sm">My Progress</a>
                </div>
            </div>

            <!-- Header Hero Banner -->
            <div class="glass-card" style="padding: 2rem; margin-bottom: 2rem; position: relative; overflow: hidden;">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1.5rem;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.65rem; margin-bottom: 0.5rem; flex-wrap: wrap;">
                            <span class="badge badge-primary"><%= category %></span>
                            <span class="badge badge-outline"><%= difficulty %></span>
                            <span class="badge <%= passed ? "badge-success" : "badge-warning" %>" style="font-weight: 700;">
                                <%= passed ? "✓ PASSED (≥ 60%)" : "⚠️ NEEDS PRACTICE (< 60%)" %>
                            </span>
                            <% if ("auto_submitted_security".equalsIgnoreCase(attemptStatus) || "terminated".equalsIgnoreCase(attemptStatus)) { %>
                                <span class="badge badge-danger">Terminated (Security Limit)</span>
                            <% } else if ("auto_submitted_timeout".equalsIgnoreCase(attemptStatus) || "auto_submitted".equalsIgnoreCase(attemptStatus)) { %>
                                <span class="badge badge-warning">Time Expired</span>
                            <% } %>
                        </div>
                        <h1 style="font-size: 1.85rem; font-weight: 800; margin: 0 0 0.5rem 0; color: var(--text-primary);">
                            <%= title %>
                        </h1>
                        <p style="color: var(--text-muted); font-size: 0.875rem; margin: 0;">
                            Submitted on <strong><%= formattedDate %></strong> • Candidate: <strong><%= user != null ? user.getName() : "Student" %></strong> • Attempt ID: <code style="font-size: 0.75rem; background: #f1f5f9; padding: 2px 6px; border-radius: 4px;"><%= attempt != null ? attempt.getId() : "" %></code>
                        </p>
                    </div>

                    <div style="display: flex; align-items: center; gap: 0.75rem;">
                        <button type="button" class="btn btn-primary" onclick="scrollToReviewSection()" style="display: flex; align-items: center; gap: 0.4rem;">
                            <span>🔍</span> Review All Answers
                        </button>
                    </div>
                </div>
            </div>

            <!-- Key Statistics Grid -->
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1.25rem; margin-bottom: 2rem;">
                <!-- Final Score -->
                <div class="result-stat-card">
                    <div class="result-stat-label">Final Score</div>
                    <div class="result-stat-val" style="color: <%= passed ? "var(--accent-emerald)" : "var(--accent-amber)" %>;">
                        <%= score %>%
                    </div>
                    <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.35rem;">
                        Raw Score: <strong><%= (correctCount * 2) %></strong> / <%= (totalQuestions * 2) %> Marks
                    </div>
                </div>

                <!-- Accuracy -->
                <div class="result-stat-card">
                    <div class="result-stat-label">Accuracy Rate</div>
                    <div class="result-stat-val" style="color: #2563eb;">
                        <%= Math.round(accuracy) %>%
                    </div>
                    <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.35rem;">
                        <%= correctCount %> correct of <%= attemptedCount %> attempted
                    </div>
                </div>

                <!-- Questions Overview -->
                <div class="result-stat-card">
                    <div class="result-stat-label">Correct / Total</div>
                    <div class="result-stat-val" style="color: var(--text-primary);">
                        <%= correctCount %> <span style="font-size: 1.1rem; color: var(--text-muted); font-weight: 600;">/ <%= totalQuestions %></span>
                    </div>
                    <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.35rem;">
                        <span style="color: #10b981; font-weight: 600;"><%= correctCount %> Correct</span> • 
                        <span style="color: #ef4444; font-weight: 600;"><%= incorrectCount %> Wrong</span> • 
                        <span style="color: #64748b; font-weight: 600;"><%= unansweredCount %> Skipped</span>
                    </div>
                </div>

                <!-- Time Taken -->
                <div class="result-stat-card">
                    <div class="result-stat-label">Time Taken</div>
                    <div class="result-stat-val" style="color: var(--text-primary);">
                        <%= timeTakenStr %>
                    </div>
                    <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.35rem;">
                        Allocated: 30 minutes
                    </div>
                </div>

                <!-- Integrity Proctoring -->
                <div class="result-stat-card">
                    <div class="result-stat-label">Proctoring Status</div>
                    <div class="result-stat-val" style="font-size: 1.35rem; color: <%= "HIGH".equalsIgnoreCase(riskLevel) ? "#dc2626" : ("MODERATE".equalsIgnoreCase(riskLevel) ? "#d97706" : "#16a34a") %>;">
                        <%= riskLevel %> RISK
                    </div>
                    <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.35rem;">
                        Violations Recorded: <strong><%= violations %> / 3</strong>
                    </div>
                </div>
            </div>

            <!-- Two-Column Breakdown: SNJB Sections + Diagnostic Strengths & Weaknesses -->
            <div style="display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 2rem; margin-bottom: 2.5rem;">
                <!-- Section-wise Performance -->
                <div class="glass-card" style="padding: 1.75rem;">
                    <h3 style="font-size: 1.15rem; margin: 0 0 0.5rem 0; display: flex; align-items: center; gap: 0.5rem;">
                        <span>📊</span> Section-Wise Performance (SNJB Syllabus)
                    </h3>
                    <p style="font-size: 0.85rem; color: var(--text-muted); margin: 0 0 1.25rem 0;">
                        Performance distribution across Quantitative, Logical, Verbal, and Technical domains.
                    </p>

                    <% if (sectionBreakdown != null && !sectionBreakdown.isEmpty()) { %>
                        <div style="display: flex; flex-direction: column; gap: 1rem;">
                            <% for (Map.Entry<String, Map<String, Object>> e : sectionBreakdown.entrySet()) {
                                String secName = e.getKey();
                                Map<String, Object> valMap = e.getValue();
                                int c = 0, tot = 0, pct = 0;
                                if (valMap != null) {
                                    Object cObj = valMap.get("correct");
                                    Object tObj = valMap.get("total");
                                    Object pObj = valMap.get("scorePercent");
                                    if (cObj instanceof Number) c = ((Number) cObj).intValue();
                                    if (tObj instanceof Number) tot = ((Number) tObj).intValue();
                                    if (pObj instanceof Number) pct = ((Number) pObj).intValue();
                                }
                            %>
                                <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem;">
                                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.4rem;">
                                        <span style="font-weight: 700; font-size: 0.9rem; color: var(--text-primary);"><%= secName %></span>
                                        <span style="font-size: 0.85rem; font-weight: 700; color: <%= pct >= 60 ? "#16a34a" : "#d97706" %>;">
                                            <%= c %> / <%= tot %> (<%= pct %>%)
                                        </span>
                                    </div>
                                    <div style="height: 7px; background: #e2e8f0; border-radius: 9999px; overflow: hidden;">
                                        <div style="width: <%= pct %>%; height: 100%; background: <%= pct >= 60 ? "#10b981" : (pct >= 40 ? "#f59e0b" : "#ef4444") %>; border-radius: 9999px;"></div>
                                    </div>
                                </div>
                            <% } %>
                        </div>
                    <% } else { %>
                        <div style="background: #f8fafc; padding: 1.25rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); text-align: center; color: var(--text-muted); font-size: 0.85rem;">
                            Overall Assessment: <strong><%= correctCount %> of <%= totalQuestions %> Questions Correct (<%= score %>%)</strong>
                        </div>
                    <% } %>
                </div>

                <!-- Diagnostic Strengths & Weaknesses -->
                <div class="glass-card" style="padding: 1.75rem; display: flex; flex-direction: column; justify-content: space-between;">
                    <div>
                        <h3 style="font-size: 1.15rem; margin: 0 0 0.5rem 0; display: flex; align-items: center; gap: 0.5rem;">
                            <span>🩺</span> Diagnostic Performance Insights
                        </h3>
                        <p style="font-size: 0.85rem; color: var(--text-muted); margin: 0 0 1.25rem 0;">
                            Identified concept areas requiring attention prior to upcoming recruitment drives.
                        </p>

                        <!-- Weak Topics -->
                        <div style="margin-bottom: 1.25rem;">
                            <div style="font-weight: 700; font-size: 0.85rem; color: #b91c1c; text-transform: uppercase; margin-bottom: 0.5rem; display: flex; align-items: center; gap: 0.35rem;">
                                <span>⚠️</span> Areas for Targeted Practice
                            </div>
                            <% if (weakTopics != null && !weakTopics.isEmpty()) { %>
                                <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                                    <% for (String w : weakTopics) { %>
                                        <span class="badge" style="background: #fee2e2; color: #991b1b; border: 1px solid #fca5a5; font-size: 0.8rem; padding: 0.35rem 0.65rem;">
                                            <%= w %>
                                        </span>
                                    <% } %>
                                </div>
                            <% } else if (score >= 80) { %>
                                <div style="background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: var(--radius-sm); padding: 0.75rem; color: #166534; font-size: 0.85rem;">
                                    ✓ Outstanding performance! No major concept gaps detected. Ready for advanced company mocks.
                                </div>
                            <% } else { %>
                                <div style="font-size: 0.85rem; color: var(--text-muted);">
                                    Practice speed and accuracy drills to raise your overall score above 75%.
                                </div>
                            <% } %>
                        </div>

                        <!-- Recommendations Card -->
                        <div style="background: #eff6ff; border: 1px solid #bfdbfe; border-radius: var(--radius-sm); padding: 1rem;">
                            <div style="font-weight: 700; font-size: 0.85rem; color: #1e40af; margin-bottom: 0.25rem;">
                                Next Step Recommendation:
                            </div>
                            <div style="font-size: 0.85rem; color: #1e3a8a; line-height: 1.5;">
                                Practice <strong><%= recommendedTestTitle %></strong> next to reinforce your foundational accuracy.
                            </div>
                        </div>
                    </div>

                    <!-- Actions -->
                    <div style="display: flex; gap: 0.75rem; margin-top: 1.5rem; flex-wrap: wrap;">
                        <a href="${pageContext.request.contextPath}/tests?preselect=<%= recommendedSeriesId %>" class="btn btn-primary btn-sm" style="flex: 1; text-align: center;">
                            Start Recommended Test →
                        </a>
                        <a href="${pageContext.request.contextPath}/tests" class="btn btn-outline btn-sm">
                            Browse All Tests
                        </a>
                    </div>
                </div>
            </div>

            <!-- ============================================================= -->
            <!-- Question-by-Question Solution Review Section -->
            <!-- ============================================================= -->
            <div id="answersReviewSection" class="glass-card" style="padding: 2rem; margin-bottom: 2.5rem;">
                <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1.25rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
                    <div>
                        <h2 style="font-size: 1.45rem; font-weight: 800; margin: 0 0 0.25rem 0; color: var(--text-primary);">
                            Question-by-Question Detailed Solutions
                        </h2>
                        <p style="font-size: 0.85rem; color: var(--text-muted); margin: 0;">
                            Inspect every problem statement, your response, correct keys, and conceptual step-by-step solutions.
                        </p>
                    </div>

                    <!-- Filter Controls -->
                    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;" id="reviewFilterButtons">
                        <button type="button" class="filter-chip-btn active" onclick="filterReviewQuestions('ALL', this)">
                            All Questions (<span id="countFilterAll">0</span>)
                        </button>
                        <button type="button" class="filter-chip-btn" onclick="filterReviewQuestions('CORRECT', this)">
                            ✓ Correct (<span id="countFilterCorrect"><%= correctCount %></span>)
                        </button>
                        <button type="button" class="filter-chip-btn" onclick="filterReviewQuestions('INCORRECT', this)">
                            ✗ Incorrect (<span id="countFilterIncorrect"><%= incorrectCount %></span>)
                        </button>
                        <button type="button" class="filter-chip-btn" onclick="filterReviewQuestions('UNANSWERED', this)">
                            — Unanswered (<span id="countFilterUnanswered"><%= unansweredCount %></span>)
                        </button>
                    </div>
                </div>

                <!-- Questions Container -->
                <div id="reviewQuestionsContainer">
                    <div style="text-align: center; padding: 3rem; color: var(--text-muted);">
                        Loading question explanations and step-by-step solutions...
                    </div>
                </div>

                <!-- Bottom Actions Navigation Flow -->
                <div style="margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid var(--border-subtle); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
                    <div>
                        <button type="button" class="btn btn-outline" onclick="window.scrollTo({ top: 0, behavior: 'smooth' })">
                            ↑ Back to Summary
                        </button>
                    </div>
                    <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
                        <a href="${pageContext.request.contextPath}/tests?preselect=<%= attempt != null ? attempt.getSeriesId() : "college_snjb_full_mock" %>" class="btn btn-outline">
                            🔄 Retake This Test (New Questions)
                        </a>
                        <a href="${pageContext.request.contextPath}/tests?preselect=<%= recommendedSeriesId %>" class="btn btn-primary" style="display: flex; align-items: center; gap: 0.35rem;">
                            <span>Next Recommended Test:</span> <strong><%= recommendedSubject %> →</strong>
                        </a>
                    </div>
                </div>
            </div>

        </main>
    </div>

    <script type="application/json" id="reviewsDataJson"><%= (reviewsJson != null && !reviewsJson.trim().isEmpty()) ? reviewsJson : "[]" %></script>
    <script src="${pageContext.request.contextPath}/assets/js/test-result.js?v=<%= System.currentTimeMillis() %>"></script>
</body>
</html>
