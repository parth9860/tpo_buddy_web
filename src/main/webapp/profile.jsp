<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.User" %>
<%@ page import="com.tpobuddy.model.StudentMetrics" %>
<%
    User user = (User) request.getAttribute("user");
    StudentMetrics metrics = (StudentMetrics) request.getAttribute("metrics");
    String successMsg = (String) request.getAttribute("successMessage");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>My Profile & Preferences — TPO Buddy</title>
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
                    <h1 class="page-title">Student Profile & Career Settings</h1>
                    <p class="page-subtitle">Manage personal information and recruitment targets synced with Firebase.</p>
                </div>
                <div>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <% if (successMsg != null) { %>
                <div class="toast toast-success" style="position: static; margin-bottom: 1.5rem; animation: none;">
                    <span>✓</span>
                    <div><%= successMsg %></div>
                </div>
            <% } %>

            <% if ("true".equals(request.getParameter("completeProfile"))) { %>
                <div class="toast toast-success" style="position: static; margin-bottom: 1.5rem; animation: none; border-left: 4px solid var(--primary); background: #eff6ff;">
                    <span style="font-size: 1.25rem;">ℹ</span>
                    <div><strong>Welcome to TPO Buddy!</strong> Please verify and complete your academic department and placement preferences below.</div>
                </div>
            <% } %>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 2rem;">
                <!-- Profile Form -->
                <div class="glass-card" style="padding: 2rem;">
                    <h3 style="margin-bottom: 1.5rem; font-size: 1.3rem;">Personal & Academic Information</h3>

                    <form action="${pageContext.request.contextPath}/profile" method="post">
                        <div class="form-group">
                            <label for="name" class="form-label">Full Name</label>
                            <input type="text" id="name" name="name" class="form-control" value="<%= user != null ? user.getName() : "" %>" required>
                        </div>

                        <div class="form-group">
                            <label class="form-label">College Email (Immutable)</label>
                            <input type="email" class="form-control" value="<%= user != null ? user.getEmail() : "" %>" disabled style="opacity: 0.6; cursor: not-allowed;">
                        </div>

                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                            <div class="form-group">
                                <label class="form-label">Department / Branch</label>
                                <input type="text" class="form-control" value="<%= user != null ? user.getBranch() : "CSE" %>" disabled style="opacity: 0.6; cursor: not-allowed;">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Academic Year</label>
                                <input type="text" class="form-control" value="<%= user != null ? user.getYear() : "Final Year" %>" disabled style="opacity: 0.6; cursor: not-allowed;">
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="college" class="form-label">Institution / College Name</label>
                            <input type="text" id="college" name="college" class="form-control" value="<%= user != null ? user.getCollege() : "" %>">
                        </div>

                        <div style="border-top: 1px solid var(--border-subtle); padding-top: 1.5rem; margin-top: 1.5rem;">
                            <h4 style="font-size: 1.15rem; margin-bottom: 1rem;">Placement Preferences</h4>

                            <div class="form-group">
                                <label for="desiredRole" class="form-label">Desired Placement Role</label>
                                <input type="text" id="desiredRole" name="desiredRole" class="form-control" placeholder="e.g. Java Backend Developer" value="<%= metrics != null ? metrics.getDesiredRole() : "Software Engineer" %>">
                            </div>

                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                                <div class="form-group">
                                    <label for="targetSalary" class="form-label">Target Package</label>
                                    <input type="text" id="targetSalary" name="targetSalary" class="form-control" placeholder="e.g. ₹14.0 LPA" value="<%= metrics != null ? metrics.getTargetSalary() : "₹12.0 LPA" %>">
                                </div>
                                <div class="form-group">
                                    <label for="preferredLocation" class="form-label">Preferred Location</label>
                                    <input type="text" id="preferredLocation" name="preferredLocation" class="form-control" placeholder="e.g. Bangalore / Remote" value="<%= metrics != null ? metrics.getPreferredLocation() : "Bangalore / Remote" %>">
                                </div>
                            </div>
                        </div>

                        <button type="submit" class="btn btn-primary" style="width: 100%; padding: 0.85rem; margin-top: 1rem;">
                            Save Profile Changes
                        </button>
                    </form>
                </div>

                <!-- Account Security & Firebase Info Card -->
                <div class="glass-card" style="padding: 2rem; display: flex; flex-direction: column; justify-content: space-between;">
                    <div>
                        <h3 style="margin-bottom: 1.5rem; font-size: 1.3rem;">Firebase Account Details</h3>

                        <% if (user != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) { %>
                        <div style="text-align: center; margin-bottom: 1.5rem;">
                            <img src="<%= user.getPhotoUrl() %>" alt="<%= user.getName() %>" style="width: 80px; height: 80px; border-radius: 50%; object-fit: cover; border: 3px solid #bfdbfe; box-shadow: var(--shadow-sm);">
                        </div>
                        <% } %>

                        <div style="background: #f8fafc; padding: 1.25rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); margin-bottom: 1.5rem;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Firebase User ID (UID)</div>
                            <div style="font-family: monospace; font-size: 0.95rem; color: var(--primary); margin-top: 0.25rem; word-break: break-all;">
                                <%= user != null ? user.getUid() : "N/A" %>
                            </div>
                        </div>

                        <div style="background: #f8fafc; padding: 1.25rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); margin-bottom: 1.5rem;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Institutional Numeric ID</div>
                            <div style="font-family: var(--font-heading); font-size: 1.5rem; font-weight: 700; color: var(--text-primary); margin-top: 0.25rem;">
                                <%= user != null ? user.getNumericId() : "N/A" %>
                            </div>
                        </div>

                        <div style="background: #f8fafc; padding: 1.25rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); margin-bottom: 1.5rem;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Account Role & Access Level</div>
                            <div style="margin-top: 0.5rem;">
                                <span class="badge badge-primary"><%= user != null ? user.getRole().toUpperCase() : "STUDENT" %></span>
                            </div>
                        </div>

                        <div style="background: #f8fafc; padding: 1.25rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); margin-bottom: 1.5rem;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Authentication Provider</div>
                            <div style="margin-top: 0.5rem; font-size: 0.95rem; font-weight: 600;">
                                <%
                                    String provider = user != null ? user.getAuthProvider() : "password";
                                    if ("google".equalsIgnoreCase(provider)) {
                                %>
                                    <span style="color: #4285F4;">🌐 Google Sign-In</span>
                                <% } else if ("facebook".equalsIgnoreCase(provider)) { %>
                                    <span style="color: #1877F2;">🌐 Facebook Login</span>
                                <% } else if ("apple".equalsIgnoreCase(provider)) { %>
                                    <span style="color: #000000;">🍎 Apple Sign-In</span>
                                <% } else { %>
                                    <span style="color: var(--text-secondary);">🔑 Email & Password</span>
                                <% } %>
                            </div>
                        </div>
                    </div>

                    <div style="border-top: 1px solid var(--border-subtle); padding-top: 1.5rem;">
                        <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()" style="width: 100%;">
                            Sign Out of TPO Buddy
                        </button>
                    </div>
                </div>
            </div>
        </main>
    </div>
</body>
</html>
