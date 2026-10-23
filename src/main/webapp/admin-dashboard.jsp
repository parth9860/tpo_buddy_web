<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    User user = (User) request.getAttribute("user");
    Map<String, Integer> stats = (Map<String, Integer>) request.getAttribute("stats");
    List<User> usersList = (List<User>) request.getAttribute("usersList");

    int userCount = (stats != null && stats.containsKey("userCount")) ? stats.get("userCount") : 0;
    int jobCount = (stats != null && stats.containsKey("jobCount")) ? stats.get("jobCount") : 0;
    int appCount = (stats != null && stats.containsKey("applicationCount")) ? stats.get("applicationCount") : 0;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Administrator Console — TPO Buddy</title>
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
                    <h1 class="page-title">Platform Administrator Console</h1>
                    <p class="page-subtitle">Platform health, live Firebase synchronization metrics, and user management.</p>
                </div>
                <div style="display: flex; gap: 0.75rem; align-items: center;">
                    <div class="badge badge-danger" style="font-size: 0.85rem; padding: 0.5rem 1rem;">
                        Root Administrator Access
                    </div>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- Stats Overview -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-label">Registered Accounts</div>
                    <div class="metric-value"><%= userCount %></div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Realtime Database `users` node</div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Active Job Drives</div>
                    <div class="metric-value"><%= jobCount %></div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Realtime Database `jobs` node</div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Submitted Applications</div>
                    <div class="metric-value"><%= appCount %></div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Realtime Database `applications` node</div>
                </div>
            </div>

            <!-- User Management -->
            <div class="data-table-card">
                <div class="data-table-header">
                    <h3 style="font-size: 1.15rem;">User Accounts & Role Permissions</h3>
                    <span style="font-size: 0.85rem; color: var(--text-muted);">Strict server-side role enforcement</span>
                </div>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Branch</th>
                                <th>Academic Year</th>
                                <th>Role</th>
                                <th>Security Scope</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (usersList != null && !usersList.isEmpty()) {
                                for (User u : usersList) { %>
                                <tr>
                                    <td style="font-weight: 600; color: var(--text-primary);"><%= u.getName() %></td>
                                    <td><%= u.getEmail() %></td>
                                    <td><span class="badge badge-primary"><%= u.getBranch() %></span></td>
                                    <td><%= u.getYear() %></td>
                                    <td>
                                        <span class="badge <%= u.isAdmin() ? "badge-danger" : (u.isTeacher() ? "badge-warning" : "badge-success") %>">
                                            <%= u.getRole().toUpperCase() %>
                                        </span>
                                    </td>
                                    <td style="font-size: 0.825rem; color: var(--text-muted);">
                                        <%= u.isAdmin() ? "FULL_PLATFORM_READ_WRITE" : (u.isTeacher() ? "TPO_DEPARTMENT_DRIVES" : "STUDENT_SELF_RECORDS") %>
                                    </td>
                                </tr>
                            <%  }
                            } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</body>
</html>
