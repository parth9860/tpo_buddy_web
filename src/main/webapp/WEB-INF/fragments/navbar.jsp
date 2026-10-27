<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<header class="navbar">
    <div class="container nav-wrapper">
        <a href="${pageContext.request.contextPath}/" class="nav-logo">
            <div class="logo-orb">TB</div>
            <span>TPO<span style="color: var(--secondary);">Buddy</span></span>
        </a>

        <ul class="nav-links">
            <li><a href="${pageContext.request.contextPath}/#home" class="nav-link">Home</a></li>
            <li><a href="${pageContext.request.contextPath}/#features" class="nav-link">Features</a></li>
            <li><a href="${pageContext.request.contextPath}/interview" class="nav-link">AI Interview</a></li>
            <li><a href="${pageContext.request.contextPath}/#about" class="nav-link">About</a></li>
        </ul>

        <div class="nav-actions">
            <a href="${pageContext.request.contextPath}/login.jsp" class="btn btn-secondary btn-sm">Login</a>
            <a href="${pageContext.request.contextPath}/register.jsp" class="btn btn-primary btn-sm">Get Started</a>
        </div>
    </div>
</header>
