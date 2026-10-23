<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<footer class="footer" style="background: #ffffff; border-top: 1px solid var(--border-subtle); padding: 3rem 0; margin-top: 4rem; box-shadow: 0 -2px 10px rgba(15, 23, 42, 0.02);">
    <div class="container" style="display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 2rem;">
        <div style="display: flex; align-items: center; gap: 0.75rem;">
            <div class="logo-orb">TB</div>
            <div>
                <div style="font-family: var(--font-heading); font-size: 1.15rem; font-weight: 700; color: var(--text-primary);">TPO Buddy</div>
                <div style="font-size: 0.825rem; color: var(--text-muted);">AI-Based Placement Management System</div>
            </div>
        </div>

        <div style="display: flex; gap: 2rem; font-size: 0.9rem;">
            <a href="${pageContext.request.contextPath}/#home">Home</a>
            <a href="${pageContext.request.contextPath}/#features">Features</a>
            <a href="${pageContext.request.contextPath}/#how-it-works">How It Works</a>
            <a href="${pageContext.request.contextPath}/login.jsp">Portal Login</a>
        </div>

        <div style="font-size: 0.85rem; color: var(--text-muted);">
            &copy; 2026 TPO Buddy Platform. Synchronized with Firebase. All rights reserved.
        </div>
    </div>
</footer>
