<%@ page import="com.tpobuddy.util.Config" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="TPO Buddy - Modern AI-Powered Placement Management System for campus recruitment, resume analysis, mock tests, and skill enhancement.">
<meta name="theme-color" content="#2563eb">

<!-- Fonts -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Outfit:wght@400;500;600;700;800&display=swap" rel="stylesheet">

<!-- Core Stylesheets -->
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/animations.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard.css">

<!-- React 18 & ReactDOM & Babel Standalone -->
<script crossorigin src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
<script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
<script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>

<!-- Lucide Icons -->
<script src="https://unpkg.com/lucide@latest"></script>

<!-- Canvas Confetti -->
<script src="https://cdn.jsdelivr.net/npm/canvas-confetti@1.9.4/dist/confetti.browser.min.js"></script>

<!-- Firebase Compat Web SDK (v10.8.0) -->
<script src="https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js"></script>
<script src="https://www.gstatic.com/firebasejs/10.8.0/firebase-auth-compat.js"></script>
<script src="https://www.gstatic.com/firebasejs/10.8.0/firebase-database-compat.js"></script>
<script src="https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore-compat.js"></script>
<script src="https://www.gstatic.com/firebasejs/10.8.0/firebase-analytics-compat.js"></script>

<!-- Global Context & Dynamic Firebase Config (Server-Injected) -->
<script>
    window.CONTEXT_PATH = '${pageContext.request.contextPath}';
    window.FIREBASE_CONFIG = {
        apiKey: "<%= Config.getFirebaseApiKey() %>",
        authDomain: "<%= Config.get("firebase.authDomain", "tpobuddy.firebaseapp.com") %>",
        databaseURL: "<%= Config.getFirebaseDatabaseUrl() %>",
        projectId: "<%= Config.getFirebaseProjectId() %>",
        storageBucket: "<%= Config.getFirebaseStorageBucket() %>",
        messagingSenderId: "<%= Config.get("firebase.messagingSenderId", "811364970356") %>",
        appId: "<%= Config.getFirebaseAppId() %>",
        measurementId: "<%= Config.getFirebaseMeasurementId() %>"
    };
</script>
<script src="${pageContext.request.contextPath}/assets/js/firebase-config.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/auth.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/question-bank.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/ai-orb.js"></script>
