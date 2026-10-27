<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.User" %>
<%
    User currentUser = (User) session.getAttribute("currentUser");
    String userName = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : "Student Candidate";
    String userRole = (currentUser != null && currentUser.getRole() != null) ? currentUser.getRole().toUpperCase() : "STUDENT";
    String userBranch = (currentUser != null && currentUser.getBranch() != null) ? currentUser.getBranch() : "CSE";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>TPO Buddy - React Modern Portal</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <style>
        :root {
            --react-primary: #6366f1;
            --react-bg: #0f172a;
            --react-card: rgba(30, 41, 59, 0.7);
            --react-border: rgba(255, 255, 255, 0.08);
            --react-text: #f8fafc;
            --react-muted: #94a3b8;
        }
        body.react-theme {
            background-color: var(--react-bg);
            color: var(--react-text);
            font-family: 'Inter', -apple-system, sans-serif;
            margin: 0;
            padding: 0;
            min-height: 100vh;
        }
        .react-app-container {
            display: flex;
            min-height: 100vh;
        }
        .react-sidebar {
            width: 260px;
            background: #090d16;
            border-right: 1px solid var(--react-border);
            padding: 1.5rem 1rem;
            display: flex;
            flex-direction: column;
        }
        .react-main {
            flex: 1;
            padding: 2rem;
            overflow-y: auto;
            max-width: 1400px;
            margin: 0 auto;
            width: 100%;
        }
        .nav-btn {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            width: 100%;
            padding: 0.75rem 1rem;
            border-radius: 8px;
            background: transparent;
            border: none;
            color: var(--react-muted);
            font-size: 0.9rem;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s ease;
            text-align: left;
        }
        .nav-btn:hover {
            background: rgba(255, 255, 255, 0.05);
            color: #fff;
        }
        .nav-btn.active {
            background: linear-gradient(135deg, rgba(99, 102, 241, 0.25), rgba(168, 85, 247, 0.25));
            border: 1px solid rgba(99, 102, 241, 0.4);
            color: #fff;
            font-weight: 600;
        }
        .react-card {
            background: var(--react-card);
            backdrop-filter: blur(12px);
            border: 1px solid var(--react-border);
            border-radius: 14px;
            padding: 1.5rem;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3);
        }
        .radar-poly {
            fill: rgba(99, 102, 241, 0.25);
            stroke: #818cf8;
            stroke-width: 2;
        }
    </style>
</head>
<body class="react-theme">
    <div id="react-portal-root"></div>

    <script type="text/babel">
        const { useState, useEffect } = React;

        const initialUserData = {
            name: "<%= userName %>",
            role: "<%= userRole %>",
            branch: "<%= userBranch %>"
        };

        function ReactPortal() {
            const [currentTab, setCurrentTab] = useState("dashboard");
            const [jobs, setJobs] = useState([]);
            const [loadingJobs, setLoadingJobs] = useState(false);
            const [metrics, setMetrics] = useState({
                readinessScore: 0,
                status: 'Foundation Stage',
                testsCompleted: 0,
                resumeScore: 0,
                interviewScore: 0,
                aptitudeSkill: 0,
                dsaSkill: 0,
                javaSkill: 0,
                sqlSkill: 0,
                eligibleDrives: 0,
                radar: { coding: 0, aptitude: 0, systemDesign: 0, resume: 0, interview: 0 }
            });
            const [chatMessages, setChatMessages] = useState([
                { role: "assistant", text: "Hello <%= userName %>! I am your TPO Buddy Career Copilot. Ask me about resume optimization, company interview questions, or your placement readiness score." }
            ]);
            const [chatInput, setChatInput] = useState("");
            const [sendingChat, setSendingChat] = useState(false);

            useEffect(() => {
                fetchJobs();
                fetchMetrics();
            }, []);

            const fetchMetrics = async () => {
                try {
                    const res = await fetch(window.CONTEXT_PATH + '/api/student/metrics');
                    const data = await res.json();
                    if (data && data.success) {
                        setMetrics(data);
                    }
                } catch (e) {
                    console.error("Live metrics error", e);
                }
            };

            const fetchJobs = async () => {
                setLoadingJobs(true);
                try {
                    const res = await fetch(window.CONTEXT_PATH + '/api/jobs/list');
                    const data = await res.json();
                    if (data && data.jobs) {
                        setJobs(data.jobs);
                    }
                } catch (e) {
                    console.error("Jobs error", e);
                } finally {
                    setLoadingJobs(false);
                }
            };

            const sendChatMessage = async (e) => {
                if (e) e.preventDefault();
                if (!chatInput.trim() || sendingChat) return;

                const userMsg = { role: "user", text: chatInput };
                setChatMessages(prev => [...prev, userMsg]);
                const messageToSend = chatInput;
                setChatInput("");
                setSendingChat(true);

                try {
                    const res = await fetch(window.CONTEXT_PATH + '/api/ai/chat', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ message: messageToSend })
                    });
                    const data = await res.json();
                    setChatMessages(prev => [...prev, {
                        role: "assistant",
                        text: data.reply || "I am analyzing placement records. Practice mock tests to elevate your readiness!"
                    }]);
                } catch (err) {
                    setChatMessages(prev => [...prev, {
                        role: "assistant",
                        text: "Error contacting Career Copilot. Please check connection."
                    }]);
                } finally {
                    setSendingChat(false);
                }
            };

            return (
                <div className="react-app-container">
                    {/* Modern Dark Sidebar */}
                    <aside className="react-sidebar">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '2rem', padding: '0 0.5rem' }}>
                            <div style={{ width: '38px', height: '38px', borderRadius: '10px', background: 'linear-gradient(135deg, #6366f1, #a855f7)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: '800', color: '#fff' }}>
                                TB
                            </div>
                            <div>
                                <div style={{ fontWeight: '700', fontSize: '1.05rem', color: '#fff' }}>TPO Buddy</div>
                                <div style={{ fontSize: '0.75rem', color: '#818cf8', fontWeight: '600' }}>React 18 SPA Portal</div>
                            </div>
                        </div>

                        <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', flex: 1 }}>
                            <button
                                className={`nav-btn ${currentTab === 'dashboard' ? 'active' : ''}`}
                                onClick={() => setCurrentTab('dashboard')}
                            >
                                <span>&#128202;</span> Dashboard & Radar
                            </button>
                            <button
                                className={`nav-btn ${currentTab === 'ai-interview' ? 'active' : ''}`}
                                onClick={() => setCurrentTab('ai-interview')}
                            >
                                <span>&#127897;</span> Mock Interview
                            </button>
                            <button
                                className={`nav-btn ${currentTab === 'copilot' ? 'active' : ''}`}
                                onClick={() => setCurrentTab('copilot')}
                            >
                                <span>&#129302;</span> Career Copilot AI
                            </button>
                            <button
                                className={`nav-btn ${currentTab === 'jobs' ? 'active' : ''}`}
                                onClick={() => setCurrentTab('jobs')}
                            >
                                <span>&#128188;</span> Campus Job Board
                            </button>
                            <button
                                className={`nav-btn ${currentTab === 'resume' ? 'active' : ''}`}
                                onClick={() => setCurrentTab('resume')}
                            >
                                <span>&#128196;</span> ATS Resume Check
                            </button>
                            <a
                                href={window.CONTEXT_PATH + '/tests'}
                                className="nav-btn"
                                style={{ textDecoration: 'none' }}
                            >
                                <span>&#128221;</span> Placement Tests
                            </a>
                        </nav>

                        <div style={{ borderTop: '1px solid var(--react-border)', paddingTop: '1rem', marginTop: '1rem' }}>
                            <a
                                href={window.CONTEXT_PATH + '/dashboard'}
                                style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#94a3b8', textDecoration: 'none', fontSize: '0.85rem', padding: '0.5rem' }}
                            >
                                <span>&#8592;</span> Return to JSP Views
                            </a>
                        </div>
                    </aside>

                    {/* Main Content Area */}
                    <main className="react-main">
                        {/* Top Navbar */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
                            <div>
                                <h1 style={{ fontSize: '1.75rem', fontWeight: '700', margin: '0 0 0.25rem 0' }}>
                                    {currentTab === 'dashboard' && 'Placement Readiness & Radar'}
                                    {currentTab === 'ai-interview' && 'AI Mock Interview Simulator'}
                                    {currentTab === 'copilot' && 'AI Placement Advisor & Chat'}
                                    {currentTab === 'jobs' && 'Active Campus Drives'}
                                    {currentTab === 'resume' && 'Automated ATS Resume Analyzer'}
                                </h1>
                                <p style={{ margin: 0, color: 'var(--react-muted)', fontSize: '0.9rem' }}>
                                    Welcome back, <strong>{initialUserData.name}</strong> ({initialUserData.branch})
                                </p>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                <span style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.3)', color: '#34d399', padding: '6px 12px', borderRadius: '999px', fontSize: '0.8rem', fontWeight: '600' }}>
                                    &#9679; Spring Boot 3 API Connected
                                </span>
                            </div>
                        </div>

                        {/* Tab 1: Dashboard */}
                        {currentTab === 'dashboard' && (
                            <div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
                                    <div className="react-card">
                                        <div style={{ fontSize: '0.8rem', color: 'var(--react-muted)', fontWeight: '600' }}>READINESS SCORE</div>
                                        <div style={{ fontSize: '2.25rem', fontWeight: '800', color: '#38bdf8', marginTop: '0.5rem' }}>{metrics.readinessScore}%</div>
                                        <div style={{ fontSize: '0.8rem', color: '#34d399', marginTop: '0.25rem' }}>&#8593; {metrics.status}</div>
                                    </div>
                                    <div className="react-card">
                                        <div style={{ fontSize: '0.8rem', color: 'var(--react-muted)', fontWeight: '600' }}>TESTS COMPLETED</div>
                                        <div style={{ fontSize: '2.25rem', fontWeight: '800', color: '#a855f7', marginTop: '0.5rem' }}>{metrics.testsCompleted}</div>
                                        <div style={{ fontSize: '0.8rem', color: 'var(--react-muted)', marginTop: '0.25rem' }}>Live Firebase assessments</div>
                                    </div>
                                    <div className="react-card">
                                        <div style={{ fontSize: '0.8rem', color: 'var(--react-muted)', fontWeight: '600' }}>ATS RESUME SCORE</div>
                                        <div style={{ fontSize: '2.25rem', fontWeight: '800', color: '#10b981', marginTop: '0.5rem' }}>{metrics.resumeScore}%</div>
                                        <div style={{ fontSize: '0.8rem', color: '#34d399', marginTop: '0.25rem' }}>PDFBox Verified</div>
                                    </div>
                                    <div className="react-card">
                                        <div style={{ fontSize: '0.8rem', color: 'var(--react-muted)', fontWeight: '600' }}>ELIGIBLE DRIVES</div>
                                        <div style={{ fontSize: '2.25rem', fontWeight: '800', color: '#f59e0b', marginTop: '0.5rem' }}>{jobs.length}</div>
                                        <div style={{ fontSize: '0.8rem', color: 'var(--react-muted)', marginTop: '0.25rem' }}>Realtime Database Listings</div>
                                    </div>
                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '1.5rem' }}>
                                    {/* SVG Radar Matrix */}
                                    <div className="react-card">
                                        <h3 style={{ fontSize: '1.1rem', margin: '0 0 1rem 0' }}>Student Competence Radar</h3>
                                        <div style={{ display: 'flex', justifyContent: 'center' }}>
                                            <svg width="280" height="260" viewBox="0 0 280 260">
                                                {/* Background Webs */}
                                                <polygon points="140,30 235,95 200,205 80,205 45,95" fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="1" />
                                                <polygon points="140,65 205,115 180,185 100,185 75,115" fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="1" />
                                                {/* Student Radar Polygon */}
                                                <polygon
                                                    points={`${140 + ((metrics.radar?.coding ?? metrics.dsaSkill ?? 0) / 100 * 85) * Math.cos(-Math.PI / 2)},${125 + ((metrics.radar?.coding ?? metrics.dsaSkill ?? 0) / 100 * 85) * Math.sin(-Math.PI / 2)} ${140 + ((metrics.radar?.aptitude ?? metrics.aptitudeSkill ?? 0) / 100 * 85) * Math.cos(-Math.PI / 2 + 2 * Math.PI / 5)},${125 + ((metrics.radar?.aptitude ?? metrics.aptitudeSkill ?? 0) / 100 * 85) * Math.sin(-Math.PI / 2 + 2 * Math.PI / 5)} ${140 + ((metrics.radar?.systemDesign ?? metrics.sqlSkill ?? 0) / 100 * 85) * Math.cos(-Math.PI / 2 + 4 * Math.PI / 5)},${125 + ((metrics.radar?.systemDesign ?? metrics.sqlSkill ?? 0) / 100 * 85) * Math.sin(-Math.PI / 2 + 4 * Math.PI / 5)} ${140 + ((metrics.radar?.resume ?? metrics.resumeScore ?? 0) / 100 * 85) * Math.cos(-Math.PI / 2 + 6 * Math.PI / 5)},${125 + ((metrics.radar?.resume ?? metrics.resumeScore ?? 0) / 100 * 85) * Math.sin(-Math.PI / 2 + 6 * Math.PI / 5)} ${140 + ((metrics.radar?.interview ?? metrics.interviewScore ?? 0) / 100 * 85) * Math.cos(-Math.PI / 2 + 8 * Math.PI / 5)},${125 + ((metrics.radar?.interview ?? metrics.interviewScore ?? 0) / 100 * 85) * Math.sin(-Math.PI / 2 + 8 * Math.PI / 5)}`}
                                                    className="radar-poly"
                                                />
                                                {/* Labels */}
                                                <text x="140" y="20" fill="#94a3b8" fontSize="11" textAnchor="middle">DSA & Coding ({metrics.radar?.coding ?? metrics.dsaSkill ?? 0}%)</text>
                                                <text x="245" y="100" fill="#94a3b8" fontSize="11">Aptitude ({metrics.radar?.aptitude ?? metrics.aptitudeSkill ?? 0}%)</text>
                                                <text x="210" y="225" fill="#94a3b8" fontSize="11">System Design ({metrics.radar?.systemDesign ?? metrics.sqlSkill ?? 0}%)</text>
                                                <text x="40" y="225" fill="#94a3b8" fontSize="11">Resume ATS ({metrics.radar?.resume ?? metrics.resumeScore ?? 0}%)</text>
                                                <text x="5" y="100" fill="#94a3b8" fontSize="11">Interview ({metrics.radar?.interview ?? metrics.interviewScore ?? 0}%)</text>
                                            </svg>
                                        </div>
                                    </div>

                                    {/* Quick Actions */}
                                    <div className="react-card">
                                        <h3 style={{ fontSize: '1.1rem', margin: '0 0 1rem 0' }}>Accelerate Preparation</h3>
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                                            <a
                                                href={window.CONTEXT_PATH + '/tests'}
                                                className="btn btn-primary"
                                                style={{ padding: '0.85rem', width: '100%', textAlign: 'left', borderRadius: '8px', textDecoration: 'none', display: 'block', background: 'linear-gradient(135deg, #4f46e5, #06b6d4)' }}
                                            >
                                                &#128221; <strong>Placement Test Series</strong>
                                                <div style={{ fontSize: '0.75rem', opacity: 0.9 }}>Topic practice (Quant, Logic, Verbal, Tech) & company mocks</div>
                                            </a>
                                            <button
                                                className="btn btn-primary"
                                                onClick={() => setCurrentTab('ai-interview')}
                                                style={{ padding: '0.85rem', width: '100%', textAlign: 'left', borderRadius: '8px' }}
                                            >
                                                &#127897; <strong>Launch AI Mock Interview</strong>
                                                <div style={{ fontSize: '0.75rem', opacity: 0.8 }}>Practice technical & behavioral questions</div>
                                            </button>
                                            <a
                                                href={window.CONTEXT_PATH + '/jobs'}
                                                className="btn btn-secondary"
                                                style={{ padding: '0.85rem', width: '100%', textAlign: 'left', borderRadius: '8px', textDecoration: 'none', display: 'block' }}
                                            >
                                                &#128188; <strong>Explore Placement Drives</strong>
                                                <div style={{ fontSize: '0.75rem', color: 'var(--react-muted)' }}>View active campus opportunities & apply</div>
                                            </a>
                                            <button
                                                className="btn btn-secondary"
                                                onClick={() => setCurrentTab('copilot')}
                                                style={{ padding: '0.85rem', width: '100%', textAlign: 'left', borderRadius: '8px' }}
                                            >
                                                &#129302; <strong>Chat with Career Copilot</strong>
                                                <div style={{ fontSize: '0.75rem', color: 'var(--react-muted)' }}>Personalized advice & interview tips</div>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Tab 2: AI Mock Interview (Redirects or embeds) */}
                        {currentTab === 'ai-interview' && (
                            <div className="react-card" style={{ textAlign: 'center', padding: '3rem' }}>
                                <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>&#127897;</div>
                                <h2 style={{ margin: '0 0 0.5rem 0' }}>Interactive AI Mock Interview Studio</h2>
                                <p style={{ color: 'var(--react-muted)', maxWidth: '520px', margin: '0 auto 1.5rem' }}>
                                    Launch the proctored interview studio with webcam preview, audio speech playback, and rubric scoring.
                                </p>
                                <a href={window.CONTEXT_PATH + '/interview'} className="btn btn-primary" style={{ padding: '0.85rem 2rem', textDecoration: 'none', borderRadius: '8px', fontWeight: '600' }}>
                                    Open Mock Interview Studio &#8594;
                                </a>
                            </div>
                        )}

                        {/* Tab 3: Career Copilot AI */}
                        {currentTab === 'copilot' && (
                            <div className="react-card" style={{ display: 'flex', flexDirection: 'column', height: '600px' }}>
                                <div style={{ flex: 1, overflowY: 'auto', paddingRight: '0.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                    {chatMessages.map((msg, idx) => (
                                        <div
                                            key={idx}
                                            style={{
                                                alignSelf: msg.role === 'user' ? 'flex-end' : 'flex-start',
                                                maxWidth: '75%',
                                                padding: '0.85rem 1.25rem',
                                                borderRadius: '12px',
                                                background: msg.role === 'user' ? '#6366f1' : 'rgba(255, 255, 255, 0.08)',
                                                color: '#fff',
                                                fontSize: '0.9rem',
                                                lineHeight: 1.5
                                            }}
                                        >
                                            {msg.text}
                                        </div>
                                    ))}
                                    {sendingChat && (
                                        <div style={{ alignSelf: 'flex-start', color: 'var(--react-muted)', fontSize: '0.85rem', fontStyle: 'italic' }}>
                                            Career Copilot is typing...
                                        </div>
                                    )}
                                </div>

                                <form onSubmit={sendChatMessage} style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem' }}>
                                    <input
                                        type="text"
                                        value={chatInput}
                                        onChange={(e) => setChatInput(e.target.value)}
                                        placeholder="Ask a placement question (e.g., 'How to explain Spring Security in an interview?')..."
                                        style={{ flex: 1, padding: '0.85rem', borderRadius: '8px', border: '1px solid var(--react-border)', background: 'rgba(0,0,0,0.3)', color: '#fff' }}
                                    />
                                    <button type="submit" className="btn btn-primary" disabled={sendingChat} style={{ padding: '0.85rem 1.5rem', borderRadius: '8px' }}>
                                        Send
                                    </button>
                                </form>
                            </div>
                        )}

                        {/* Tab 4: Jobs */}
                        {currentTab === 'jobs' && (
                            <div>
                                {loadingJobs ? (
                                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--react-muted)' }}>Loading placement drives...</div>
                                ) : (
                                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
                                        {jobs.length === 0 ? (
                                            <div className="react-card" style={{ gridColumn: '1/-1', textAlign: 'center', padding: '3rem' }}>
                                                <h3>No Active Campus Drives Found</h3>
                                                <p style={{ color: 'var(--react-muted)' }}>All live recruitment opportunities from Firebase will appear here as soon as TPO coordinators publish them.</p>
                                                <a href={window.CONTEXT_PATH + '/jobs'} className="btn btn-primary btn-sm" style={{ textDecoration: 'none' }}>
                                                    View All Jobs on JSP Portal
                                                </a>
                                            </div>
                                        ) : (
                                            jobs.map(job => (
                                                <div key={job.id} className="react-card">
                                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                                        <h3 style={{ margin: 0, fontSize: '1.15rem' }}>{job.title}</h3>
                                                        <span style={{ background: '#3b82f6', color: '#fff', fontSize: '0.75rem', padding: '3px 8px', borderRadius: '6px' }}>
                                                            {job.packageInfo || (job.packageLpa ? job.packageLpa + ' LPA' : 'Competitive')}
                                                        </span>
                                                    </div>
                                                    <div style={{ color: '#818cf8', fontWeight: '600', margin: '0.35rem 0' }}>{job.company}</div>
                                                    <p style={{ fontSize: '0.85rem', color: 'var(--react-muted)', margin: '0.75rem 0' }}>{job.description}</p>
                                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem', borderTop: '1px solid var(--react-border)', paddingTop: '0.75rem' }}>
                                                        <span style={{ fontSize: '0.8rem', color: 'var(--react-muted)' }}>
                                                            {job.eligibility || (job.minCgpa ? 'Min CGPA: ' + job.minCgpa : 'All Branches')}
                                                        </span>
                                                        <a href={window.CONTEXT_PATH + '/jobs'} className="btn btn-primary btn-sm" style={{ textDecoration: 'none', fontSize: '0.8rem', padding: '4px 10px' }}>
                                                            Apply Now
                                                        </a>
                                                    </div>
                                                </div>
                                            ))
                                        )}
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Tab 5: Resume */}
                        {currentTab === 'resume' && (
                            <div className="react-card" style={{ textAlign: 'center', padding: '3rem' }}>
                                <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>&#128196;</div>
                                <h2 style={{ margin: '0 0 0.5rem 0' }}>Apache PDFBox AI Resume Analyzer</h2>
                                <p style={{ color: 'var(--react-muted)', maxWidth: '520px', margin: '0 auto 1.5rem' }}>
                                    Upload your PDF resume to extract skills, calculate your ATS score, and receive actionable keyword improvement suggestions.
                                </p>
                                <a href={window.CONTEXT_PATH + '/resume'} className="btn btn-primary" style={{ padding: '0.85rem 2rem', textDecoration: 'none', borderRadius: '8px', fontWeight: '600' }}>
                                    Launch Resume Analyzer &#8594;
                                </a>
                            </div>
                        )}
                    </main>
                </div>
            );
        }

        const portalRoot = document.getElementById('react-portal-root');
        if (portalRoot) {
            ReactDOM.createRoot(portalRoot).render(<ReactPortal />);
        }
    </script>
</body>
</html>
