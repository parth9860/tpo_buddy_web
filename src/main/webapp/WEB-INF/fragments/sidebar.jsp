<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.User" %>
<%
    User currentUser = (User) session.getAttribute("currentUser");
    String currentPath = request.getServletPath();
    String userName = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : "Student";
    String userRole = (currentUser != null && currentUser.getRole() != null) ? currentUser.getRole().toUpperCase() : "STUDENT";
    String userBranch = (currentUser != null && currentUser.getBranch() != null) ? currentUser.getBranch() : "CSE";
    String initial = (userName.length() > 0) ? userName.substring(0, 1).toUpperCase() : "S";
%>
<aside class="app-sidebar">
    <div class="sidebar-header">
        <a href="${pageContext.request.contextPath}/dashboard" class="sidebar-brand">
            <div class="logo-orb">TB</div>
            <span>TPO<span style="color: var(--secondary);">Buddy</span></span>
        </a>
    </div>

    <ul class="sidebar-nav">
        <li>
            <a href="${pageContext.request.contextPath}/dashboard" class="nav-item-link <%= currentPath.contains("dashboard") && !currentPath.contains("faculty") && !currentPath.contains("admin") ? "active" : "" %>">
                <span class="nav-icon">&#128202;</span>
                <span>Dashboard</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/tests" class="nav-item-link <%= currentPath.contains("tests") ? "active" : "" %>">
                <span class="nav-icon">&#128221;</span>
                <span>Placement Tests</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/interview" class="nav-item-link <%= currentPath.contains("interview") ? "active" : "" %>">
                <span class="nav-icon">&#127897;</span>
                <span>AI Mock Interview</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/resume" class="nav-item-link <%= currentPath.contains("resume") ? "active" : "" %>">
                <span class="nav-icon">&#128196;</span>
                <span>AI Resume ATS</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/ai-buddy" class="nav-item-link <%= currentPath.contains("ai-buddy") ? "active" : "" %>">
                <span class="nav-icon">&#129302;</span>
                <span>Career Copilot AI</span>
            </a>
        </li>

        <li>
            <a href="${pageContext.request.contextPath}/jobs" class="nav-item-link <%= currentPath.contains("jobs") ? "active" : "" %>">
                <span class="nav-icon">&#128188;</span>
                <span>Placement Jobs</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/progress" class="nav-item-link <%= currentPath.contains("progress") ? "active" : "" %>">
                <span class="nav-icon">&#128640;</span>
                <span>My Progress</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/react-portal" class="nav-item-link <%= currentPath.contains("react-portal") ? "active" : "" %>" style="border: 1px dashed rgba(99, 102, 241, 0.4); border-radius: 8px; margin: 0.25rem 0; background: rgba(99, 102, 241, 0.05);">
                <span class="nav-icon">&#9883;</span>
                <span style="font-weight: 600; color: #4f46e5;">React Modern Hub</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/profile" class="nav-item-link <%= currentPath.contains("profile") ? "active" : "" %>">
                <span class="nav-icon">&#128100;</span>
                <span>Profile</span>
            </a>
        </li>

        <% if (currentUser != null && (currentUser.isTeacher() || currentUser.isAdmin())) { %>
        <li style="margin-top: 0.75rem; border-top: 1px solid var(--border-subtle); padding-top: 0.5rem;">
            <a href="${pageContext.request.contextPath}/faculty" class="nav-item-link <%= currentPath.contains("faculty") ? "active" : "" %>">
                <span class="nav-icon">&#127891;</span>
                <span>Faculty / TPO Portal</span>
            </a>
        </li>
        <% } %>

        <% if (currentUser != null && currentUser.isAdmin()) { %>
        <li>
            <a href="${pageContext.request.contextPath}/admin" class="nav-item-link <%= currentPath.contains("admin") ? "active" : "" %>">
                <span class="nav-icon">&#128737;</span>
                <span>Admin Console</span>
            </a>
        </li>
        <% } %>

        <li style="margin-top: auto; padding-top: 1rem;">
            <a href="${pageContext.request.contextPath}/logout" class="nav-item-link" style="color: var(--accent-rose);">
                <span class="nav-icon">&#128682;</span>
                <span>Logout</span>
            </a>
        </li>
    </ul>

    <div class="sidebar-footer">
        <div class="user-snippet">
            <div class="user-avatar" style="overflow: hidden; display: flex; align-items: center; justify-content: center;">
                <% if (currentUser != null && currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isEmpty()) { %>
                    <img src="<%= currentUser.getPhotoUrl() %>" alt="<%= userName %>" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;" onerror="this.style.display='none'; if(this.nextElementSibling) this.nextElementSibling.style.display='inline';">
                    <span style="display: none;"><%= initial %></span>
                <% } else { %>
                    <%= initial %>
                <% } %>
            </div>
            <div class="user-details">
                <div class="user-name"><%= userName %></div>
                <div class="user-role-badge">
                    <span><%= userRole %></span>
                    <span>&bull;</span>
                    <span><%= userBranch %></span>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/logout" title="Sign Out" style="background: #fee2e2; border: 1px solid #fecaca; border-radius: var(--radius-sm); padding: 0.4rem 0.6rem; cursor: pointer; color: #991b1b; font-size: 0.85rem; font-weight: 600; display: flex; align-items: center; gap: 0.25rem; text-decoration: none;" aria-label="Sign out">
                <span>&#128682;</span>
                <span style="font-size: 0.75rem;">Exit</span>
            </a>
        </div>
    </div>
</aside>
