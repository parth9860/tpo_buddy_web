<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.User" %>
<%@ page import="com.tpobuddy.model.InterviewSession" %>
<%@ page import="java.util.List" %>
<%
    User currentUser = (User) session.getAttribute("currentUser");
    String userName = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : "Candidate";
    InterviewSession latestReport = (InterviewSession) request.getAttribute("latestReport");
    List<InterviewSession> history = (List<InterviewSession>) request.getAttribute("history");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>AI Mock Interview - TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <style>
        .interview-grid {
            display: grid;
            grid-template-columns: 1fr 340px;
            gap: 1.5rem;
        }
        @media (max-width: 1024px) {
            .interview-grid { grid-template-columns: 1fr; }
        }
        .webcam-box {
            position: relative;
            width: 100%;
            height: 220px;
            background: #0f172a;
            border-radius: var(--radius-md);
            overflow: hidden;
            display: flex;
            align-items: center;
            justify-content: center;
            border: 2px solid rgba(99, 102, 241, 0.3);
        }
        .webcam-box video {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transform: scaleX(-1);
        }
        .ai-indicator-badge {
            position: absolute;
            top: 10px;
            left: 10px;
            background: rgba(15, 23, 42, 0.75);
            backdrop-filter: blur(4px);
            padding: 4px 10px;
            border-radius: 999px;
            font-size: 0.75rem;
            color: #38bdf8;
            display: flex;
            align-items: center;
            gap: 6px;
            font-weight: 600;
        }
        .live-pulse {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #ef4444;
            animation: pulse-ring 1.5s infinite;
        }
        @keyframes pulse-ring {
            0% { transform: scale(0.95); opacity: 1; }
            50% { transform: scale(1.3); opacity: 0.5; }
            100% { transform: scale(0.95); opacity: 1; }
        }
    </style>
</head>
<body class="dashboard-body">
    <div class="dashboard-layout">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="main-content">
            <header class="content-header">
                <div>
                    <h1 class="page-title">AI Mock Interview Studio</h1>
                    <p class="page-subtitle">Practice role-tailored technical & behavioral interviews with instant AI feedback & scoring</p>
                </div>
                <div style="display: flex; gap: 0.75rem;">
                    <a href="${pageContext.request.contextPath}/react-portal" class="btn btn-secondary btn-sm" style="display: flex; align-items: center; gap: 0.35rem;">
                        <span>&#9883;</span> React Modern View
                    </a>
                </div>
            </header>

            <!-- React Interview Studio Container -->
            <div id="react-interview-root"></div>

            <!-- Historical Reports Section -->
            <div class="glass-card" style="margin-top: 2rem; padding: 1.5rem;">
                <h3 style="font-size: 1.15rem; margin-bottom: 1rem; display: flex; align-items: center; gap: 0.5rem;">
                    <span>&#128221;</span> Recent Interview Evaluations
                </h3>
                <% if (history == null || history.isEmpty()) { %>
                    <div style="text-align: center; padding: 2rem; color: var(--text-muted); font-size: 0.9rem;">
                        No past interview attempts yet. Start your first session above!
                    </div>
                <% } else { %>
                    <div style="overflow-x: auto;">
                        <table style="width: 100%; border-collapse: collapse; font-size: 0.875rem;">
                            <thead>
                                <tr style="border-bottom: 2px solid var(--border-subtle); text-align: left; color: var(--text-muted);">
                                    <th style="padding: 0.75rem;">Role</th>
                                    <th style="padding: 0.75rem;">Overall Score</th>
                                    <th style="padding: 0.75rem;">Technical</th>
                                    <th style="padding: 0.75rem;">Communication</th>
                                    <th style="padding: 0.75rem;">Summary</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (InterviewSession item : history) { %>
                                    <tr style="border-bottom: 1px solid var(--border-subtle);">
                                        <td style="padding: 0.75rem; font-weight: 600;"><%= item.getJobRole() %></td>
                                        <td style="padding: 0.75rem;">
                                            <span style="font-weight: 700; color: <%= item.getOverallScore() >= 70 ? "#10b981" : (item.getOverallScore() >= 50 ? "#f59e0b" : "#ef4444") %>;">
                                                <%= item.getOverallScore() %>%
                                            </span>
                                        </td>
                                        <td style="padding: 0.75rem;"><%= item.getTechScore() %>%</td>
                                        <td style="padding: 0.75rem;"><%= item.getCommScore() %>%</td>
                                        <td style="padding: 0.75rem; color: var(--text-secondary); max-width: 320px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"><%= item.getSummary() %></td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                <% } %>
            </div>
        </main>
    </div>

    <!-- Embedded React 18 AI Mock Interview Component -->
    <script type="text/babel">
        const { useState, useEffect, useRef } = React;

        function AiInterviewApp() {
            const [selectedRole, setSelectedRole] = useState("Full Stack Developer");
            const [sessionState, setSessionState] = useState("IDLE"); // IDLE, ACTIVE, REPORT
            const [sessionId, setSessionId] = useState(null);
            const [currentQuestion, setCurrentQuestion] = useState(null);
            const [totalQuestions, setTotalQuestions] = useState(5);
            const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
            const [userAnswer, setUserAnswer] = useState("");
            const [isEvaluating, setIsEvaluating] = useState(false);
            const [evalFeedback, setEvalFeedback] = useState(null);
            const [finalReport, setFinalReport] = useState(null);
            const [timerSeconds, setTimerSeconds] = useState(120);
            const [webcamEnabled, setWebcamEnabled] = useState(false);
            const [isListening, setIsListening] = useState(false);

            const videoRef = useRef(null);
            const timerRef = useRef(null);

            // Timer countdown
            useEffect(() => {
                if (sessionState === "ACTIVE" && timerSeconds > 0) {
                    timerRef.current = setInterval(() => {
                        setTimerSeconds(prev => prev - 1);
                    }, 1000);
                } else if (timerSeconds === 0 && sessionState === "ACTIVE") {
                    handleAnswerSubmit();
                }
                return () => clearInterval(timerRef.current);
            }, [sessionState, timerSeconds]);

            // Speech Synthesis for asking questions aloud
            const speakQuestion = (text) => {
                if ('speechSynthesis' in window) {
                    window.speechSynthesis.cancel();
                    const utterance = new SpeechSynthesisUtterance(text);
                    utterance.rate = 1.0;
                    utterance.pitch = 1.0;
                    window.speechSynthesis.speak(utterance);
                }
            };

            // Toggle Camera
            const toggleWebcam = async () => {
                if (!webcamEnabled) {
                    try {
                        const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
                        if (videoRef.current) {
                            videoRef.current.srcObject = stream;
                        }
                        setWebcamEnabled(true);
                    } catch (err) {
                        alert("Camera permission not granted. Running in avatar mode.");
                    }
                } else {
                    if (videoRef.current && videoRef.current.srcObject) {
                        videoRef.current.srcObject.getTracks().forEach(track => track.stop());
                    }
                    setWebcamEnabled(false);
                }
            };

            // Voice Dictation
            const toggleVoiceRecognition = () => {
                const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
                if (!SpeechRecognition) {
                    alert("Speech recognition not supported in this browser. Please type your answer.");
                    return;
                }
                if (isListening) {
                    setIsListening(false);
                    return;
                }
                const recognition = new SpeechRecognition();
                recognition.continuous = true;
                recognition.interimResults = true;
                recognition.onstart = () => setIsListening(true);
                recognition.onresult = (e) => {
                    let transcript = '';
                    for (let i = e.resultIndex; i < e.results.length; ++i) {
                        transcript += e.results[i][0].transcript;
                    }
                    setUserAnswer(prev => prev + " " + transcript);
                };
                recognition.onend = () => setIsListening(false);
                recognition.start();
            };

            const startInterview = async () => {
                try {
                    const res = await fetch(window.CONTEXT_PATH + '/api/interview/start', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ role: selectedRole })
                    });
                    const data = await res.json();
                    if (data.success) {
                        setSessionId(data.sessionId);
                        setCurrentQuestion(data.firstQuestion);
                        setTotalQuestions(data.totalQuestions || 5);
                        setCurrentQuestionIndex(0);
                        setSessionState("ACTIVE");
                        setTimerSeconds(120);
                        setUserAnswer("");
                        setEvalFeedback(null);
                        speakQuestion(data.firstQuestion.questionText);
                    }
                } catch (e) {
                    console.error("Failed to start session", e);
                }
            };

            const handleAnswerSubmit = async () => {
                if (!sessionId || isEvaluating) return;
                setIsEvaluating(true);
                try {
                    const res = await fetch(window.CONTEXT_PATH + '/api/interview/answer', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: sessionId,
                            questionId: currentQuestion ? currentQuestion.id : 1,
                            answer: userAnswer
                        })
                    });
                    const data = await res.json();
                    if (data.success) {
                        setEvalFeedback({
                            score: data.score,
                            feedback: data.feedback
                        });

                        setTimeout(() => {
                            if (data.isCompleted) {
                                finishInterview();
                            } else if (data.nextQuestion) {
                                setCurrentQuestion(data.nextQuestion);
                                setCurrentQuestionIndex(data.nextQuestionIndex);
                                setUserAnswer("");
                                setEvalFeedback(null);
                                setTimerSeconds(120);
                                speakQuestion(data.nextQuestion.questionText);
                            }
                            setIsEvaluating(false);
                        }, 2500);
                    } else {
                        setIsEvaluating(false);
                    }
                } catch (e) {
                    setIsEvaluating(false);
                    console.error("Answer submission failed", e);
                }
            };

            const finishInterview = async () => {
                try {
                    const res = await fetch(window.CONTEXT_PATH + '/api/interview/finish', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ sessionId: sessionId })
                    });
                    const data = await res.json();
                    if (data.success) {
                        setFinalReport(data.report);
                        setSessionState("REPORT");
                        if (window.confetti) {
                            window.confetti({ particleCount: 120, spread: 70, origin: { y: 0.6 } });
                        }
                    }
                } catch (e) {
                    console.error("Finish interview failed", e);
                }
            };

            if (sessionState === "IDLE") {
                return (
                    <div className="glass-card" style={{ padding: '2.5rem', textAlign: 'center' }}>
                        <div style={{ width: '64px', height: '64px', borderRadius: '50%', background: 'linear-gradient(135deg, #4f46e5, #06b6d4)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem', color: 'white', fontSize: '1.75rem', boxShadow: '0 10px 25px -5px rgba(79, 70, 229, 0.4)' }}>
                            &#127897;
                        </div>
                        <h2 style={{ fontSize: '1.75rem', marginBottom: '0.75rem' }}>Select Target Placement Role</h2>
                        <p style={{ color: 'var(--text-secondary)', maxWidth: '540px', margin: '0 auto 2rem', fontSize: '0.95rem' }}>
                            Our CareerPulse AI Interviewer simulates authentic company technical and behavioral rounds with webcam proctoring, speech synthesis, and deep rubric analysis.
                        </p>

                        <div style={{ display: 'flex', justifyContent: 'center', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '2.5rem' }}>
                            {["Full Stack Developer", "Frontend React Developer", "Java Backend Developer", "Data Analyst & AI"].map(role => (
                                <button
                                    key={role}
                                    type="button"
                                    onClick={() => setSelectedRole(role)}
                                    className={`btn ${selectedRole === role ? 'btn-primary' : 'btn-secondary'}`}
                                    style={{ padding: '0.75rem 1.25rem', borderRadius: '10px', fontWeight: 600 }}
                                >
                                    {role}
                                </button>
                            ))}
                        </div>

                        <button
                            type="button"
                            onClick={startInterview}
                            className="btn btn-primary"
                            style={{ padding: '0.85rem 2.5rem', fontSize: '1.05rem', borderRadius: '12px', boxShadow: '0 10px 20px -3px rgba(79, 70, 229, 0.35)' }}
                        >
                            Start AI Interview Session &#8594;
                        </button>
                    </div>
                );
            }

            if (sessionState === "ACTIVE") {
                return (
                    <div className="interview-grid">
                        {/* Main Interaction Area */}
                        <div className="glass-card" style={{ padding: '2rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <span style={{ background: '#e0e7ff', color: '#4338ca', padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 700 }}>
                                        {currentQuestion ? currentQuestion.category : 'Technical'}
                                    </span>
                                    <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                                        Question {currentQuestionIndex + 1} of {totalQuestions}
                                    </span>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, color: timerSeconds < 30 ? '#ef4444' : '#4f46e5' }}>
                                    <span>&#9201;</span>
                                    <span>{Math.floor(timerSeconds / 60)}:{('0' + (timerSeconds % 60)).slice(-2)}</span>
                                </div>
                            </div>

                            <div style={{ background: 'rgba(99, 102, 241, 0.05)', border: '1px solid rgba(99, 102, 241, 0.2)', padding: '1.5rem', borderRadius: '12px', marginBottom: '1.5rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem' }}>
                                    <h3 style={{ fontSize: '1.2rem', lineHeight: 1.5, margin: 0 }}>
                                        {currentQuestion ? currentQuestion.questionText : "Loading question..."}
                                    </h3>
                                    <button
                                        type="button"
                                        onClick={() => currentQuestion && speakQuestion(currentQuestion.questionText)}
                                        title="Re-read question aloud"
                                        style={{ background: '#eef2ff', border: 'none', borderRadius: '50%', width: '36px', height: '36px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1rem', color: '#4f46e5' }}
                                    >
                                        &#128266;
                                    </button>
                                </div>
                            </div>

                            <div style={{ marginBottom: '1.25rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                    <label style={{ fontSize: '0.875rem', fontWeight: 600 }}>Your Response (Type or Speak)</label>
                                    <button
                                        type="button"
                                        onClick={toggleVoiceRecognition}
                                        style={{ background: isListening ? '#fee2e2' : '#f1f5f9', border: '1px solid var(--border-subtle)', borderRadius: '6px', padding: '4px 10px', fontSize: '0.8rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: isListening ? '#991b1b' : 'inherit' }}
                                    >
                                        <span>&#127908;</span> {isListening ? 'Listening...' : 'Voice Dictation'}
                                    </button>
                                </div>
                                <textarea
                                    rows="5"
                                    value={userAnswer}
                                    onChange={(e) => setUserAnswer(e.target.value)}
                                    placeholder="Explain your thought process, architecture, key trade-offs, and examples..."
                                    style={{ width: '100%', padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-subtle)', background: 'var(--surface)', fontSize: '0.95rem', resize: 'vertical' }}
                                />
                            </div>

                            {evalFeedback && (
                                <div style={{ padding: '1rem', background: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: '8px', marginBottom: '1.25rem', color: '#065f46', fontSize: '0.9rem' }}>
                                    <strong>AI Score: {evalFeedback.score}/10</strong> &bull; {evalFeedback.feedback}
                                </div>
                            )}

                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                                <button
                                    type="button"
                                    onClick={handleAnswerSubmit}
                                    disabled={isEvaluating}
                                    className="btn btn-primary"
                                    style={{ padding: '0.75rem 1.75rem', borderRadius: '8px' }}
                                >
                                    {isEvaluating ? 'Evaluating...' : (currentQuestionIndex + 1 === totalQuestions ? 'Submit & Finalize &#8594;' : 'Submit Answer &#8594;')}
                                </button>
                            </div>
                        </div>

                        {/* Webcam & AI Copilot Sidebar */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <div className="glass-card" style={{ padding: '1rem' }}>
                                <div className="webcam-box">
                                    <div className="ai-indicator-badge">
                                        <div className="live-pulse"></div>
                                        <span>PROCTORING ON</span>
                                    </div>
                                    <video ref={videoRef} autoPlay playsInline muted style={{ display: webcamEnabled ? 'block' : 'none' }}></video>
                                    {!webcamEnabled && (
                                        <div style={{ textAlign: 'center', color: '#94a3b8' }}>
                                            <div style={{ fontSize: '2.5rem', marginBottom: '0.25rem' }}>&#128104;&#8205;&#128187;</div>
                                            <div style={{ fontSize: '0.75rem' }}>Camera Off (Avatar Active)</div>
                                        </div>
                                    )}
                                </div>
                                <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'center' }}>
                                    <button
                                        type="button"
                                        onClick={toggleWebcam}
                                        className="btn btn-secondary btn-sm"
                                        style={{ width: '100%', fontSize: '0.8rem' }}
                                    >
                                        {webcamEnabled ? 'Disable Camera' : 'Enable Camera'}
                                    </button>
                                </div>
                            </div>

                            <div className="glass-card" style={{ padding: '1.25rem' }}>
                                <h4 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Target Role</h4>
                                <div style={{ fontWeight: 700, fontSize: '1.05rem', color: 'var(--primary)' }}>{selectedRole}</div>
                                <div style={{ marginTop: '1rem', borderTop: '1px solid var(--border-subtle)', paddingTop: '0.75rem', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                                    &#128161; <strong>Tip:</strong> Use the STAR method (Situation, Task, Action, Result) to maximize your score.
                                </div>
                            </div>
                        </div>
                    </div>
                );
            }

            if (sessionState === "REPORT" && finalReport) {
                return (
                    <div className="glass-card animate-fade-in-up" style={{ padding: '2.5rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1.25rem', marginBottom: '1.5rem' }}>
                            <div>
                                <span style={{ background: '#ecfdf5', color: '#047857', padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 700 }}>
                                    INTERVIEW COMPLETED
                                </span>
                                <h2 style={{ fontSize: '1.75rem', marginTop: '0.5rem' }}>Placement Readiness Evaluation Report</h2>
                                <p style={{ color: 'var(--text-secondary)', margin: 0 }}>Target Role: <strong>{finalReport.jobRole}</strong></p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setSessionState("IDLE")}
                                className="btn btn-primary btn-sm"
                            >
                                Start New Session
                            </button>
                        </div>

                        {/* Top Score Cards */}
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
                            <div style={{ background: 'rgba(99, 102, 241, 0.08)', padding: '1.5rem', borderRadius: '12px', textAlign: 'center', border: '1px solid rgba(99, 102, 241, 0.2)' }}>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>OVERALL READINESS</div>
                                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: finalReport.overallScore >= 70 ? '#10b981' : '#f59e0b' }}>
                                    {finalReport.overallScore}%
                                </div>
                            </div>
                            <div style={{ background: 'rgba(16, 185, 129, 0.08)', padding: '1.5rem', borderRadius: '12px', textAlign: 'center', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>TECHNICAL COMPETENCE</div>
                                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#10b981' }}>
                                    {finalReport.techScore}%
                                </div>
                            </div>
                            <div style={{ background: 'rgba(59, 130, 246, 0.08)', padding: '1.5rem', borderRadius: '12px', textAlign: 'center', border: '1px solid rgba(59, 130, 246, 0.2)' }}>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>COMMUNICATION & STAR</div>
                                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#3b82f6' }}>
                                    {finalReport.commScore}%
                                </div>
                            </div>
                        </div>

                        <div style={{ background: 'var(--surface-hover)', padding: '1.25rem', borderRadius: '10px', marginBottom: '1.75rem', lineHeight: 1.6 }}>
                            <strong>AI Placement Summary: </strong> {finalReport.summary}
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
                            <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', padding: '1.25rem', borderRadius: '10px' }}>
                                <h4 style={{ color: '#166534', margin: '0 0 0.75rem 0' }}>&#9989; Key Strengths</h4>
                                <ul style={{ margin: 0, paddingLeft: '1.25rem', color: '#14532d', fontSize: '0.9rem', lineHeight: 1.6 }}>
                                    {finalReport.strengths && finalReport.strengths.map((s, idx) => (
                                        <li key={idx}>{s}</li>
                                    ))}
                                </ul>
                            </div>
                            <div style={{ background: '#fffbeb', border: '1px solid #fde68a', padding: '1.25rem', borderRadius: '10px' }}>
                                <h4 style={{ color: '#92400e', margin: '0 0 0.75rem 0' }}>&#9888; Actionable Next Steps</h4>
                                <ul style={{ margin: 0, paddingLeft: '1.25rem', color: '#78350f', fontSize: '0.9rem', lineHeight: 1.6 }}>
                                    {finalReport.recommendations && finalReport.recommendations.map((r, idx) => (
                                        <li key={idx}>{r}</li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                    </div>
                );
            }

            return null;
        }

        const rootEl = document.getElementById('react-interview-root');
        if (rootEl) {
            const root = ReactDOM.createRoot(rootEl);
            root.render(<AiInterviewApp />);
        }
    </script>
</body>
</html>
