<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>TPO Buddy — AI-Based Placement Management System</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <jsp:include page="/WEB-INF/fragments/navbar.jsp" />

    <!-- Hero Section -->
    <section id="home" style="padding: 5rem 0 6rem; position: relative;">
        <div class="container" style="display: grid; grid-template-columns: 1.15fr 0.85fr; align-items: center; gap: 3.5rem;">
            <div class="animate-fade-in-up">
                <div class="badge badge-primary" style="margin-bottom: 1.5rem;">
                    <span>⚡ AI-Driven Placement Intelligence</span>
                </div>
                <h1 style="margin-bottom: 1.5rem; line-height: 1.15;">
                    Your Career. <br/>
                    Your Skills. <br/>
                    <span style="background: var(--gradient-primary); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">Your Future.</span>
                </h1>
                <p style="font-size: 1.15rem; max-width: 540px; margin-bottom: 2.5rem; line-height: 1.7;">
                    TPO Buddy is an AI-powered placement management platform that helps students analyze their resumes, improve technical skills, practice assessments, and discover career opportunities.
                </p>
                <div style="display: flex; gap: 1.25rem; flex-wrap: wrap;">
                    <a href="${pageContext.request.contextPath}/register.jsp" class="btn btn-primary btn-lg">
                        <span>Get Started</span>
                        <span>→</span>
                    </a>
                    <a href="#features" class="btn btn-secondary btn-lg">Explore Features</a>
                </div>
            </div>

            <!-- 3D AI Assistant Orb Visual -->
            <div class="animate-fade-in-up stagger-2" style="position: relative;">
                <div class="orb-scene">
                    <div class="orb-halo"></div>
                    <div class="orb-container">
                        <div class="orb-ring orb-ring-1"></div>
                        <div class="orb-ring orb-ring-2"></div>
                        <div class="orb-ring orb-ring-3"></div>
                        <div class="orb-core"></div>
                    </div>
                </div>

                <!-- Floating 3D Elements -->
                <div class="glass-card animate-float" style="position: absolute; top: 10%; right: -5%; padding: 0.85rem 1.25rem; border-color: #93c5fd; display: flex; align-items: center; gap: 0.75rem; box-shadow: var(--shadow-md);">
                    <span style="font-size: 1.35rem;">📄</span>
                    <div>
                        <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">ATS Score</div>
                        <div style="font-size: 1.1rem; font-weight: 700; color: var(--primary);">85% Verified</div>
                    </div>
                </div>

                <div class="glass-card animate-float" style="position: absolute; bottom: 8%; left: -5%; padding: 0.85rem 1.25rem; border-color: #93c5fd; display: flex; align-items: center; gap: 0.75rem; animation-delay: -2s; box-shadow: var(--shadow-md);">
                    <span style="font-size: 1.35rem;">🎯</span>
                    <div>
                        <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Placement Target</div>
                        <div style="font-size: 1.1rem; font-weight: 700; color: var(--text-primary);">Software Engineer</div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Features Section with 3D Tilt Cards -->
    <section id="features" style="padding: 5rem 0; border-top: 1px solid var(--border-subtle);">
        <div class="container">
            <div style="text-align: center; max-width: 650px; margin: 0 auto 4rem;">
                <div class="badge badge-primary" style="margin-bottom: 1rem;">Core Capabilities</div>
                <h2>Intelligent Placement Engineering</h2>
                <p>Designed to provide deep analytics, synchronized assessment logic, and end-to-end recruitment tracking.</p>
            </div>

            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 2rem;">
                <!-- Feature 1 -->
                <div class="card-3d glass-card">
                    <div style="width: 52px; height: 52px; border-radius: var(--radius-sm); background: #eff6ff; border: 1px solid #bfdbfe; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin-bottom: 1.5rem;">
                        📄
                    </div>
                    <h3 style="margin-bottom: 0.75rem; font-size: 1.35rem;">AI Resume Analysis</h3>
                    <p>Upload PDF resumes for automated ATS scoring, keyword detection, section audits, and real-time skill-gap recommendations.</p>
                </div>

                <!-- Feature 2 -->
                <div class="card-3d glass-card">
                    <div style="width: 52px; height: 52px; border-radius: var(--radius-sm); background: #e0f2fe; border: 1px solid #bae6fd; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin-bottom: 1.5rem;">
                        🤖
                    </div>
                    <h3 style="margin-bottom: 0.75rem; font-size: 1.35rem;">AI Placement Assistant</h3>
                    <p>Personalized AI Buddy chatbot trained on student academic records, weaker topics, and high-frequency company interview questions.</p>
                </div>

                <!-- Feature 3 -->
                <div class="card-3d glass-card">
                    <div style="width: 52px; height: 52px; border-radius: var(--radius-sm); background: #eff6ff; border: 1px solid #bfdbfe; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin-bottom: 1.5rem;">
                        📝
                    </div>
                    <h3 style="margin-bottom: 0.75rem; font-size: 1.35rem;">Technical & Aptitude Tests</h3>
                    <p>Timed 25-question assessments spanning Java, DSA, SQL, and Quantitative Aptitude with instant server-validated scoring.</p>
                </div>

                <!-- Feature 4 -->
                <div class="card-3d glass-card">
                    <div style="width: 52px; height: 52px; border-radius: var(--radius-sm); background: #ecfdf5; border: 1px solid #a7f3d0; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin-bottom: 1.5rem;">
                        💼
                    </div>
                    <h3 style="margin-bottom: 0.75rem; font-size: 1.35rem;">Job Matching & Drives</h3>
                    <p>Real-time campus recruitment opportunities with branch and CGPA eligibility matching, deadline alerts, and 1-click applications.</p>
                </div>

                <!-- Feature 5 -->
                <div class="card-3d glass-card">
                    <div style="width: 52px; height: 52px; border-radius: var(--radius-sm); background: #fffbeb; border: 1px solid #fde68a; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin-bottom: 1.5rem;">
                        📈
                    </div>
                    <h3 style="margin-bottom: 0.75rem; font-size: 1.35rem;">Performance Tracking</h3>
                    <p>Multi-dimensional readiness tracking factoring in assessment accuracy, streak consistency, and resume caliber directly from Firebase.</p>
                </div>

                <!-- Feature 6 -->
                <div class="card-3d glass-card">
                    <div style="width: 52px; height: 52px; border-radius: var(--radius-sm); background: #eff6ff; border: 1px solid #bfdbfe; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin-bottom: 1.5rem;">
                        🎓
                    </div>
                    <h3 style="margin-bottom: 0.75rem; font-size: 1.35rem;">Placement Management</h3>
                    <p>Institutional coordination with role-based authorization for Students, Faculty/TPO officers, and Administrators.</p>
                </div>
            </div>
        </div>
    </section>

    <!-- How It Works Section -->
    <section id="how-it-works" style="padding: 5rem 0; border-top: 1px solid var(--border-subtle); background: linear-gradient(180deg, #f8fafc 0%, #eff6ff 100%);">
        <div class="container">
            <div style="text-align: center; max-width: 600px; margin: 0 auto 4rem;">
                <div class="badge badge-primary" style="margin-bottom: 1rem;">Process</div>
                <h2>How It Works</h2>
                <p>A streamlined three-step workflow to accelerate campus placement readiness.</p>
            </div>

            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 2rem;">
                <div class="glass-card" style="text-align: center; padding: 2.5rem 2rem;">
                    <div style="width: 48px; height: 48px; border-radius: 50%; background: var(--gradient-primary); color: #fff; font-weight: 700; font-size: 1.25rem; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.5rem;">1</div>
                    <h3 style="font-size: 1.25rem; margin-bottom: 0.75rem;">Create Your Profile</h3>
                    <p style="font-size: 0.95rem;">Register with your college branch, academic year, and career aspirations, instantly syncing with Firebase.</p>
                </div>

                <div class="glass-card" style="text-align: center; padding: 2.5rem 2rem;">
                    <div style="width: 48px; height: 48px; border-radius: 50%; background: var(--gradient-primary); color: #fff; font-weight: 700; font-size: 1.25rem; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.5rem;">2</div>
                    <h3 style="font-size: 1.25rem; margin-bottom: 0.75rem;">Analyze & Prepare with AI</h3>
                    <p style="font-size: 0.95rem;">Upload your resume for instant ATS auditing, complete 25-question section tests, and chat with AI Buddy.</p>
                </div>

                <div class="glass-card" style="text-align: center; padding: 2.5rem 2rem;">
                    <div style="width: 48px; height: 48px; border-radius: 50%; background: var(--gradient-primary); color: #fff; font-weight: 700; font-size: 1.25rem; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.5rem;">3</div>
                    <h3 style="font-size: 1.25rem; margin-bottom: 0.75rem;">Track & Discover Opportunities</h3>
                    <p style="font-size: 0.95rem;">Monitor your readiness metrics, level up weaker topics, and apply to matched placement opportunities.</p>
                </div>
            </div>
        </div>
    </section>

    <!-- About Section -->
    <section id="about" style="padding: 5rem 0; border-top: 1px solid var(--border-subtle);">
        <div class="container" style="max-width: 850px; text-align: center;">
            <div class="badge badge-primary" style="margin-bottom: 1rem;">Platform Architecture</div>
            <h2>Enterprise Unified Placement System</h2>
            <p style="font-size: 1.1rem; line-height: 1.8; margin-bottom: 2rem;">
                Built on Java EE Servlets and JavaServer Pages with direct Firebase Cloud synchronization. Accounts, assessment records, resumes, and placement jobs are 100% interoperable between the Android mobile application and the web platform.
            </p>
            <a href="${pageContext.request.contextPath}/register.jsp" class="btn btn-primary btn-lg">Launch TPO Buddy</a>
        </div>
    </section>

    <jsp:include page="/WEB-INF/fragments/footer.jsp" />
</body>
</html>
