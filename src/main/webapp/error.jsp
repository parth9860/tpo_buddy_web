<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.User" %>
<%
    Integer statusCode = (Integer) request.getAttribute("statusCode");
    if (statusCode == null) statusCode = 500;
    String errorTitle = (String) request.getAttribute("errorTitle");
    if (errorTitle == null) {
        if (statusCode == 404) errorTitle = "Page or Resource Not Found";
        else if (statusCode == 403) errorTitle = "Access Forbidden";
        else errorTitle = "Unexpected Server Error";
    }
    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage == null || errorMessage.trim().isEmpty()) {
        if (statusCode == 404) {
            errorMessage = "The assessment or page you are trying to access does not exist or has been moved.";
        } else if (statusCode == 403) {
            errorMessage = "You do not have permission to access this resource or examination attempt.";
        } else {
            errorMessage = "A server processing error occurred. If you were taking an exam, your responses have been preserved.";
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><%= statusCode %> — <%= errorTitle %> | TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <style>
        .error-container {
            min-height: 80vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 2rem;
        }
        .error-card {
            background: #ffffff;
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-lg);
            padding: 3rem 2.5rem;
            max-width: 560px;
            width: 100%;
            text-align: center;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);
        }
        .error-code {
            font-family: var(--font-heading);
            font-size: 3.5rem;
            font-weight: 800;
            color: var(--primary);
            line-height: 1;
            margin-bottom: 0.5rem;
        }
        .error-title {
            font-size: 1.35rem;
            font-weight: 700;
            color: var(--text-primary);
            margin-bottom: 1rem;
        }
        .error-desc {
            font-size: 0.95rem;
            color: var(--text-secondary);
            line-height: 1.6;
            margin-bottom: 2rem;
        }
        .error-actions {
            display: flex;
            gap: 1rem;
            justify-content: center;
            flex-wrap: wrap;
        }
    </style>
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="error-container">
        <div class="error-card">
            <div style="font-size: 3rem; margin-bottom: 1rem;">
                <%= statusCode == 404 ? "🔍" : (statusCode == 403 ? "🔒" : "⚠️") %>
            </div>
            <div class="error-code"><%= statusCode %></div>
            <h1 class="error-title"><%= errorTitle %></h1>
            <p class="error-desc"><%= errorMessage %></p>

            <div class="error-actions">
                <a href="${pageContext.request.contextPath}/tests" class="btn btn-primary">
                    🎯 AI Test Series
                </a>
                <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-outline">
                    📊 Student Dashboard
                </a>
                <a href="${pageContext.request.contextPath}/progress" class="btn btn-outline">
                    🚀 My Progress
                </a>
            </div>

            <div style="margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid var(--border-subtle); font-size: 0.8rem; color: var(--text-muted);">
                TPO Buddy Placement Portal &bull; SNJB Engineering Placement Cell
            </div>
        </div>
    </div>
</body>
</html>
