<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.*" %>
<%@ page import="com.tpobuddy.service.PlacementReadinessEngine.ReadinessResult" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%
    User user = (User) request.getAttribute("user");
    StudentMetrics metrics = (StudentMetrics) request.getAttribute("metrics");
    ReadinessResult readiness = (ReadinessResult) request.getAttribute("readiness");
    List<TestAttempt> testHistory = (List<TestAttempt>) request.getAttribute("testHistory");
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>My Progress & Analytics — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="app-shell">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="app-main">
            <!-- Header -->
            <div class="page-header">
                <div>
                    <h1 class="page-title">Performance Analytics & Placement Progress</h1>
                    <p class="page-subtitle">Historical test trajectory, topic competencies, and readiness trend analysis.</p>
                </div>
                <div>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- Readiness Breakdown Banner -->
            <div class="glass-card" style="padding: 2rem; margin-bottom: 2rem;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                    <div>
                        <span class="badge badge-primary"><%= readiness != null ? readiness.status : "Foundation Stage" %></span>
                        <h2 style="font-size: 1.6rem; margin-top: 0.5rem;">Comprehensive Placement Readiness Profile</h2>
                    </div>
                    <div style="text-align: right;">
                        <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Composite Score</div>
                        <div style="font-family: var(--font-heading); font-size: 2.75rem; font-weight: 800; color: var(--secondary);"><%= readiness != null ? readiness.overallScore : 0 %>%</div>
                    </div>
                </div>

                <!-- Competency Breakdown Bars -->
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 2rem;">
                    <div>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 0.4rem; font-size: 0.875rem;">
                            <span style="color: var(--text-primary); font-weight: 500;">Technical & Core Java</span>
                            <span style="color: var(--secondary); font-weight: 700;"><%= metrics != null ? metrics.getJavaSkill() : 0 %>%</span>
                        </div>
                        <div class="metric-bar-bg" style="margin-bottom: 1.25rem;">
                            <div class="metric-bar-fill" style="width: <%= metrics != null ? metrics.getJavaSkill() : 0 %>%;"></div>
                        </div>

                        <div style="display: flex; justify-content: space-between; margin-bottom: 0.4rem; font-size: 0.875rem;">
                            <span style="color: var(--text-primary); font-weight: 500;">Data Structures & Algorithms</span>
                            <span style="color: var(--secondary); font-weight: 700;"><%= metrics != null ? metrics.getDsaSkill() : 0 %>%</span>
                        </div>
                        <div class="metric-bar-bg" style="margin-bottom: 1.25rem;">
                            <div class="metric-bar-fill" style="width: <%= metrics != null ? metrics.getDsaSkill() : 0 %>%;"></div>
                        </div>
                    </div>

                    <div>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 0.4rem; font-size: 0.875rem;">
                            <span style="color: var(--text-primary); font-weight: 500;">Database Systems & SQL</span>
                            <span style="color: var(--secondary); font-weight: 700;"><%= metrics != null ? metrics.getSqlSkill() : 0 %>%</span>
                        </div>
                        <div class="metric-bar-bg" style="margin-bottom: 1.25rem;">
                            <div class="metric-bar-fill" style="width: <%= metrics != null ? metrics.getSqlSkill() : 0 %>%;"></div>
                        </div>

                        <div style="display: flex; justify-content: space-between; margin-bottom: 0.4rem; font-size: 0.875rem;">
                            <span style="color: var(--text-primary); font-weight: 500;">Quantitative Aptitude</span>
                            <span style="color: var(--secondary); font-weight: 700;"><%= metrics != null ? metrics.getAptitudeSkill() : 0 %>%</span>
                        </div>
                        <div class="metric-bar-bg" style="margin-bottom: 1.25rem;">
                            <div class="metric-bar-fill" style="width: <%= metrics != null ? metrics.getAptitudeSkill() : 0 %>%;"></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Historical Performance Table -->
            <div class="data-table-card">
                <div class="data-table-header">
                    <h3 style="font-size: 1.15rem;">All Completed Assessments</h3>
                    <span style="font-size: 0.85rem; color: var(--text-muted);"><%= testHistory != null ? testHistory.size() : 0 %> Recorded Attempts</span>
                </div>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Category</th>
                                <th>Topic</th>
                                <th>Score</th>
                                <th>Correct Answers</th>
                                <th>Result Status</th>
                                <th>Timestamp</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (testHistory != null && !testHistory.isEmpty()) {
                                for (TestAttempt t : testHistory) { %>
                                <tr>
                                    <td style="font-weight: 600; color: var(--text-primary);"><%= t.getCategory() %></td>
                                    <td><%= t.getTopic() %></td>
                                    <td style="font-weight: 700; color: <%= t.isPassed() ? "var(--accent-emerald)" : "var(--accent-amber)" %>;"><%= t.getScore() %>%</td>
                                    <td><%= t.getCorrectAnswers() %> / <%= t.getTotalQuestions() %></td>
                                    <td>
                                        <span class="badge <%= t.isPassed() ? "badge-success" : "badge-warning" %>">
                                            <%= t.isPassed() ? "PASSED (≥ 60%)" : "NEEDS PRACTICE" %>
                                        </span>
                                    </td>
                                    <td><%= sdf.format(new Date(t.getSavedAt())) %></td>
                                </tr>
                            <%  }
                            } else { %>
                                <tr>
                                    <td colspan="6" style="text-align: center; padding: 3rem; color: var(--text-muted);">
                                        Insufficient assessment history. Take more section tests in the Tests tab to generate your trend analysis.
                                        <div style="margin-top: 1rem;">
                                            <a href="${pageContext.request.contextPath}/tests" class="btn btn-primary btn-sm">Take a Practice Test</a>
                                        </div>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</body>
</html>
