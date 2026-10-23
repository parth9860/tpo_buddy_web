<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.User" %>
<%@ page import="com.tpobuddy.model.StudentMetrics" %>
<%
    User user = (User) request.getAttribute("user");
    StudentMetrics metrics = (StudentMetrics) request.getAttribute("metrics");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>AI Buddy — Placement Assistant — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <script src="${pageContext.request.contextPath}/assets/js/chat.js"></script>
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="app-shell">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="app-main">
            <div class="page-header" style="margin-bottom: 1.5rem;">
                <div>
                    <h1 class="page-title">AI Placement Assistant</h1>
                    <p class="page-subtitle">Personalized intelligence trained on your live placement metrics, test scores, and target roles.</p>
                </div>
                <div>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- Quick Suggestions Bar -->
            <div style="display: flex; gap: 0.75rem; flex-wrap: wrap; margin-bottom: 1.25rem;">
                <button type="button" class="btn btn-secondary btn-sm" onclick="sendQuickPrompt('What should I study today?')">
                    📅 Daily Study Plan
                </button>
                <button type="button" class="btn btn-secondary btn-sm" onclick="sendQuickPrompt('Analyze my placement profile')">
                    📊 Analyze Profile Readiness
                </button>
                <button type="button" class="btn btn-secondary btn-sm" onclick="sendQuickPrompt('Give me Java interview questions')">
                    💡 High-Frequency Interview Questions
                </button>
                <button type="button" class="btn btn-secondary btn-sm" onclick="sendQuickPrompt('How can I improve my resume?')">
                    📄 Resume Optimization Advice
                </button>
                <button type="button" class="btn btn-secondary btn-sm" onclick="sendQuickPrompt('Show available jobs')">
                    💼 Matched Placement Drives
                </button>
            </div>

            <!-- Chat Window -->
            <div class="chat-window">
                <!-- Chat Header with Mini 3D Orb -->
                <div class="chat-header">
                    <div style="position: relative; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center;">
                        <div class="logo-orb" style="width: 36px; height: 36px; font-size: 1rem;">🤖</div>
                    </div>
                    <div>
                        <div style="font-weight: 700; color: var(--text-primary); font-size: 1.05rem;">AI Buddy Placement Mentor</div>
                        <div style="font-size: 0.8rem; color: var(--accent-emerald); display: flex; align-items: center; gap: 0.35rem;">
                            <span style="width: 7px; height: 7px; border-radius: 50%; background: var(--accent-emerald); display: inline-block;"></span>
                            <span>Live context: <%= user != null ? user.getName() : "Student" %> (<%= user != null ? user.getBranch() : "CSE" %>)</span>
                        </div>
                    </div>
                </div>

                <!-- Chat Messages Scroll Area -->
                <div class="chat-messages" id="chatMessages">
                    <!-- Initial Welcome Message from Assistant -->
                    <div class="message-bubble message-assistant">
                        Hello <strong><%= user != null ? user.getName() : "there" %></strong>! I'm your personalized AI Placement Assistant. I am directly connected to your Firebase test results, resume ATS score, and target career goals.
                        <br/><br/>
                        How would you like to prepare for campus recruitment today? You can ask me for a customized study plan, practice questions, or resume feedback!
                    </div>
                </div>

                <!-- Chat Input Form -->
                <form id="chatForm" class="chat-input-bar">
                    <input type="text" id="chatInput" class="form-control" placeholder="Ask AI Buddy anything about tests, study plans, or placement drives..." autocomplete="off">
                    <button type="submit" class="btn btn-primary" style="padding: 0.8rem 1.5rem;">
                        <span>Send</span>
                        <span>➤</span>
                    </button>
                </form>
            </div>
        </main>
    </div>
</body>
</html>
