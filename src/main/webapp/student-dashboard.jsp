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
    String aiWelcome = (String) request.getAttribute("aiWelcome");
    List<TestAttempt> recentTests = (List<TestAttempt>) request.getAttribute("recentTests");
    List<Job> jobs = (List<Job>) request.getAttribute("jobs");
    ResumeData resume = (ResumeData) request.getAttribute("resume");

    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
    int readinessVal = (readiness != null) ? readiness.overallScore : 0;
    // Calculate circular stroke offset (circumference = 2 * PI * 54 = 339.29)
    double circumference = 339.29;
    double strokeOffset = circumference - (readinessVal / 100.0 * circumference);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Student Dashboard — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <script src="${pageContext.request.contextPath}/assets/js/jobs.js"></script>
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="app-shell">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="app-main">
            <!-- Header -->
            <div class="page-header">
                <div>
                    <h1 class="page-title">Placement Dashboard</h1>
                    <p class="page-subtitle">
                        <%= user != null ? user.getName() : "Student" %> • <%= user != null ? user.getBranch() : "CSE" %> • <%= user != null ? user.getYear() : "Final Year" %> (<%= user != null ? user.getCollege() : "College" %>)
                    </p>
                </div>
                <div style="display: flex; gap: 0.75rem; align-items: center;">
                    <a href="${pageContext.request.contextPath}/tests" class="btn btn-secondary btn-sm">📝 Practice Assessment</a>
                    <a href="${pageContext.request.contextPath}/resume" class="btn btn-primary btn-sm">📄 Update Resume</a>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- AI Welcome Banner -->
            <div class="ai-banner animate-fade-in-up">
                <div style="display: flex; align-items: center; gap: 1rem;">
                    <div class="logo-orb" style="flex-shrink: 0; width: 42px; height: 42px;">🤖</div>
                    <p class="ai-banner-text">
                        <%= aiWelcome != null ? aiWelcome : "Welcome to TPO Buddy! Complete practice tests to increase your placement readiness score." %>
                    </p>
                </div>
                <a href="${pageContext.request.contextPath}/ai-buddy" class="btn btn-secondary btn-sm" style="flex-shrink: 0;">Chat with AI Buddy →</a>
            </div>

            <!-- Readiness & Target Banner -->
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin-bottom: 2rem;">
                <!-- Readiness Gauge Card -->
                <div class="glass-card" style="display: flex; align-items: center; gap: 2rem; padding: 2rem;">
                    <div style="position: relative; width: 130px; height: 130px; flex-shrink: 0;">
                        <svg width="130" height="130" viewBox="0 0 130 130">
                            <circle cx="65" cy="65" r="54" fill="none" stroke="rgba(37, 99, 235, 0.12)" stroke-width="10"></circle>
                            <circle class="progress-ring-circle" cx="65" cy="65" r="54" fill="none" stroke="url(#primaryGradient)" stroke-width="10" stroke-dasharray="339.29" stroke-dashoffset="<%= String.format("%.2f", strokeOffset) %>" stroke-linecap="round"></circle>
                            <defs>
                                <linearGradient id="primaryGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                                    <stop offset="0%" stop-color="#2563eb" />
                                    <stop offset="100%" stop-color="#0284c7" />
                                </linearGradient>
                            </defs>
                        </svg>
                        <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center;">
                            <span class="animated-counter" style="font-family: var(--font-heading); font-size: 2.1rem; font-weight: 700; color: var(--text-primary); line-height: 1;"><%= readinessVal %></span>
                            <span style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Percent</span>
                        </div>
                    </div>
                    <div>
                        <div class="badge badge-primary" style="margin-bottom: 0.5rem;"><%= readiness != null ? readiness.status : "Foundation Stage" %></div>
                        <h3 style="font-size: 1.35rem; margin-bottom: 0.5rem;">Placement Readiness</h3>
                        <p style="font-size: 0.875rem; margin: 0; color: var(--text-secondary);">
                            <%= readiness != null ? readiness.aiSummary : "Complete tests and upload a resume to calculate readiness." %>
                        </p>
                    </div>
                </div>

                <!-- Career Targets Card -->
                <div class="glass-card" style="padding: 2rem; display: flex; flex-direction: column; justify-content: space-between;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <h3 style="font-size: 1.25rem;">Target Career Profile</h3>
                        <a href="${pageContext.request.contextPath}/profile" style="font-size: 0.85rem; color: var(--primary);">Edit Targets</a>
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem;">
                        <div style="background: #f8fafc; padding: 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                            <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Desired Role</div>
                            <div style="font-weight: 600; color: var(--text-primary); margin-top: 0.25rem;"><%= (metrics != null && metrics.getDesiredRole() != null) ? metrics.getDesiredRole() : "Not Set" %></div>
                        </div>
                        <div style="background: #f8fafc; padding: 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                            <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Target Package</div>
                            <div style="font-weight: 600; color: var(--primary); margin-top: 0.25rem;"><%= (metrics != null && metrics.getTargetSalary() != null) ? metrics.getTargetSalary() : "Not Set" %></div>
                        </div>
                        <div style="background: #f8fafc; padding: 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                            <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Location</div>
                            <div style="font-weight: 600; color: var(--text-primary); margin-top: 0.25rem;"><%= (metrics != null && metrics.getPreferredLocation() != null) ? metrics.getPreferredLocation() : "Not Set" %></div>
                        </div>
                    </div>
                    <div style="margin-top: 1rem; font-size: 0.8rem; color: var(--text-muted);">
                        ⚡ Synchronized in real-time with Firebase Realtime Database
                    </div>
                </div>
            </div>

            <!-- Metrics Grid -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-label">Technical / Java</div>
                    <div class="metric-value"><span class="animated-counter"><%= metrics != null ? metrics.getJavaSkill() : 0 %></span>%</div>
                    <div class="metric-bar-bg">
                        <div class="metric-bar-fill" style="width: <%= metrics != null ? metrics.getJavaSkill() : 0 %>%;"></div>
                    </div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">DSA & Algorithms</div>
                    <div class="metric-value"><span class="animated-counter"><%= metrics != null ? metrics.getDsaSkill() : 0 %></span>%</div>
                    <div class="metric-bar-bg">
                        <div class="metric-bar-fill" style="width: <%= metrics != null ? metrics.getDsaSkill() : 0 %>%;"></div>
                    </div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Database / SQL</div>
                    <div class="metric-value"><span class="animated-counter"><%= metrics != null ? metrics.getSqlSkill() : 0 %></span>%</div>
                    <div class="metric-bar-bg">
                        <div class="metric-bar-fill" style="width: <%= metrics != null ? metrics.getSqlSkill() : 0 %>%;"></div>
                    </div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Quantitative Aptitude</div>
                    <div class="metric-value"><span class="animated-counter"><%= metrics != null ? metrics.getAptitudeSkill() : 0 %></span>%</div>
                    <div class="metric-bar-bg">
                        <div class="metric-bar-fill" style="width: <%= metrics != null ? metrics.getAptitudeSkill() : 0 %>%;"></div>
                    </div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Resume ATS Score</div>
                    <div class="metric-value"><span class="animated-counter"><%= resume != null && resume.getAtsScore() > 0 ? resume.getAtsScore() : (metrics != null ? metrics.getResumeScore() : 0) %></span>%</div>
                    <div class="metric-bar-bg">
                        <div class="metric-bar-fill" style="width: <%= resume != null && resume.getAtsScore() > 0 ? resume.getAtsScore() : (metrics != null ? metrics.getResumeScore() : 0) %>%;"></div>
                    </div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Completed Tests</div>
                    <div class="metric-value"><span class="animated-counter"><%= metrics != null ? metrics.getTestsCompleted() : 0 %></span></div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.5rem;">
                        🔥 Consistency Streak: <%= metrics != null ? metrics.getStreakDays() : 0 %> Days
                    </div>
                </div>
            </div>

            <!-- Two-Column Sections: Recent Tests + Top Jobs -->
            <div style="display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 2rem;">
                <!-- Recent Test Attempts -->
                <div class="data-table-card">
                    <div class="data-table-header">
                        <h3 style="font-size: 1.15rem;">Recent Test Attempts</h3>
                        <a href="${pageContext.request.contextPath}/tests" style="font-size: 0.85rem; color: var(--secondary);">View All Tests</a>
                    </div>
                    <div class="table-responsive">
                        <table class="table">
                            <thead>
                                <tr>
                                    <th>Assessment</th>
                                    <th>Topic</th>
                                    <th>Score</th>
                                    <th>Status</th>
                                    <th>Date</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (recentTests != null && !recentTests.isEmpty()) {
                                    for (TestAttempt t : recentTests) { %>
                                    <tr>
                                        <td style="font-weight: 600; color: var(--text-primary);"><%= t.getCategory() %></td>
                                        <td><%= t.getTopic() %></td>
                                        <td style="font-weight: 700; color: <%= t.isPassed() ? "var(--accent-emerald)" : "var(--accent-amber)" %>;"><%= t.getScore() %>%</td>
                                        <td>
                                            <span class="badge <%= t.isPassed() ? "badge-success" : "badge-warning" %>">
                                                <%= t.isPassed() ? "PASSED" : "NEEDS PRACTICE" %>
                                            </span>
                                        </td>
                                        <td><%= sdf.format(new Date(t.getSavedAt())) %></td>
                                    </tr>
                                <%  }
                                } else { %>
                                    <tr>
                                        <td colspan="5" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                                            No assessment tests recorded yet.
                                            <div style="margin-top: 0.75rem;">
                                                <a href="${pageContext.request.contextPath}/tests" class="btn btn-primary btn-sm">Take First Assessment</a>
                                            </div>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Recommended Job Drives -->
                <div class="data-table-card">
                    <div class="data-table-header">
                        <h3 style="font-size: 1.15rem;">Active Placement Drives</h3>
                        <a href="${pageContext.request.contextPath}/jobs" style="font-size: 0.85rem; color: var(--secondary);">All Jobs</a>
                    </div>
                    <div style="padding: 1.25rem 1.5rem; display: flex; flex-direction: column; gap: 1rem;">
                        <% if (jobs != null && !jobs.isEmpty()) {
                            for (Job j : jobs) { %>
                            <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1.1rem; display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <div style="font-weight: 700; color: var(--text-primary); font-size: 0.95rem;"><%= j.getTitle() %></div>
                                    <div style="font-size: 0.85rem; color: var(--text-muted);"><%= j.getCompany() %> • <%= j.getLocation() %></div>
                                    <div style="display: flex; gap: 0.5rem; margin-top: 0.4rem;">
                                        <span class="badge badge-primary"><%= j.getPackageInfo() %></span>
                                        <span class="badge badge-success"><%= j.getEligibility() %></span>
                                    </div>
                                </div>
                                <div>
                                    <% if (j.isApplied()) { %>
                                        <button class="btn btn-secondary btn-sm" disabled>✓ Applied</button>
                                    <% } else { %>
                                        <button class="btn btn-primary btn-sm" onclick="applyJob('<%= j.getId() %>', this)">Apply</button>
                                    <% } %>
                                </div>
                            </div>
                        <%  }
                        } else { %>
                            <div style="text-align: center; padding: 2rem; color: var(--text-muted);">
                                No placement drives listed at this moment.
                            </div>
                        <% } %>
                    </div>
                </div>
            </div>
        </main>
    </div>
</body>
</html>
