<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.Job" %>
<%@ page import="com.tpobuddy.model.User" %>
<%@ page import="java.util.List" %>
<%
    User user = (User) request.getAttribute("user");
    List<Job> jobs = (List<Job>) request.getAttribute("jobs");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Placement Jobs & Drives — TPO Buddy</title>
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
                    <h1 class="page-title">Campus Placement Drives & Jobs</h1>
                    <p class="page-subtitle">Verified institutional recruitment drives from Firebase Realtime Database.</p>
                </div>
                <div>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- Search & Filters -->
            <div class="glass-card" style="padding: 1.25rem 1.5rem; margin-bottom: 2rem; display: flex; gap: 1rem; align-items: center; flex-wrap: wrap;">
                <div style="flex: 1; min-width: 250px;">
                    <input type="text" id="jobSearch" class="form-control" placeholder="Search by role, company name, or technology..." oninput="filterJobs(this.value)">
                </div>
                <div style="display: flex; gap: 0.5rem;">
                    <button class="btn btn-secondary btn-sm" onclick="filterJobs('')">All</button>
                    <button class="btn btn-secondary btn-sm" onclick="filterJobs('Java')">Java</button>
                    <button class="btn btn-secondary btn-sm" onclick="filterJobs('Cloud')">Cloud</button>
                    <button class="btn btn-secondary btn-sm" onclick="filterJobs('SQL')">SQL</button>
                </div>
            </div>

            <!-- Job Cards Grid -->
            <div class="jobs-grid">
                <% if (jobs != null && !jobs.isEmpty()) {
                    for (Job j : jobs) { %>
                    <div class="job-card">
                        <div>
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem;">
                                <div>
                                    <div class="badge badge-primary" style="margin-bottom: 0.5rem;"><%= j.getCompany() %></div>
                                    <h3 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.25rem;"><%= j.getTitle() %></h3>
                                    <div style="font-size: 0.85rem; color: var(--text-muted);">📍 <%= j.getLocation() %></div>
                                </div>
                                <span class="badge badge-success"><%= j.getPackageInfo() %></span>
                            </div>

                            <div style="margin-bottom: 1.25rem;">
                                <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase; margin-bottom: 0.25rem;">Eligibility Criteria</div>
                                <div style="font-size: 0.9rem; color: var(--text-secondary); font-weight: 500;"><%= j.getEligibility() %></div>
                            </div>

                            <div style="margin-bottom: 1.5rem;">
                                <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase; margin-bottom: 0.5rem;">Required Skills</div>
                                <div style="display: flex; flex-wrap: wrap; gap: 0.4rem;">
                                    <% if (j.getSkills() != null && !j.getSkills().trim().isEmpty()) {
                                        for (String skill : j.getSkills().split(",")) { %>
                                            <span class="badge" style="background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe;"><%= skill.trim() %></span>
                                    <%  }
                                       } else { %>
                                            <span class="badge" style="background: #f1f5f9; color: #64748b;">Not Specified</span>
                                    <% } %>
                                </div>
                            </div>
                        </div>

                        <div style="border-top: 1px solid var(--border-subtle); padding-top: 1.25rem; display: flex; justify-content: space-between; align-items: center;">
                            <div style="font-size: 0.8rem; color: var(--text-muted);">
                                Deadline: <strong style="color: var(--text-secondary);"><%= j.getDeadline() %></strong>
                            </div>
                            <div>
                                <% if (j.isApplied()) { %>
                                    <button class="btn btn-secondary btn-sm" disabled>✓ Applied</button>
                                <% } else { %>
                                    <button class="btn btn-primary btn-sm" onclick="applyJob('<%= j.getId() %>', this)">Apply Now</button>
                                <% } %>
                            </div>
                        </div>
                    </div>
                <%  }
                } else { %>
                    <div class="glass-card" style="grid-column: 1 / -1; text-align: center; padding: 4rem 2rem; color: var(--text-muted);">
                        No recruitment opportunities available in this category.
                    </div>
                <% } %>
            </div>
        </main>
    </div>
</body>
</html>
