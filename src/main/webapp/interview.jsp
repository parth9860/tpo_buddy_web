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
        .option-chip {
            padding: 0.65rem 1.15rem;
            border-radius: 10px;
            border: 1px solid var(--border-subtle);
            background: var(--surface);
            font-size: 0.875rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .option-chip:hover {
            border-color: var(--primary);
            background: var(--surface-hover);
        }
        .option-chip.active {
            background: var(--primary);
            color: #ffffff;
            border-color: var(--primary);
            box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35);
        }
        .eval-tag {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 4px 10px;
            border-radius: 6px;
            font-size: 0.8rem;
            font-weight: 600;
        }
        .eval-tag-strength {
            background: #dcfce7;
            color: #15803d;
            border: 1px solid #bbf7d0;
        }
        .eval-tag-improve {
            background: #fef3c7;
            color: #b45309;
            border: 1px solid #fde68a;
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
                    <p class="page-subtitle">Practice role-tailored technical & behavioral interviews powered by Google Gemini AI with live rubric evaluation</p>
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
                                    <th style="padding: 0.75rem;">Difficulty</th>
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
                                            <span style="font-size: 0.75rem; padding: 2px 8px; border-radius: 4px; background: #e0e7ff; color: #4338ca; font-weight: 600;">
                                                <%= item.getDifficulty() != null ? item.getDifficulty() : "Medium" %>
                                            </span>
                                        </td>
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

        const AVAILABLE_ROLES = [
            "Full Stack Developer",
            "Frontend React Developer",
            "Java Backend Developer",
            "Data Analyst & AI",
            "DevOps & Cloud Engineer"
        ];

        const DIFFICULTIES = [
            { id: "Medium", label: "Medium", desc: "Standard Campus Recruitment" },
            { id: "Hard", label: "Hard", desc: "Tier-1 Product Companies" },
            { id: "Extra Hard", label: "Extra Hard", desc: "FAANG & Core Systems" }
        ];

        const QUESTION_COUNTS = [
            { count: 3, label: "3 Questions (~10 mins)", tag: "Quick Drill" },
            { count: 5, label: "5 Questions (~20 mins)", tag: "Standard Round" },
            { count: 8, label: "8 Questions (~35 mins)", tag: "Comprehensive" }
        ];

        function AiInterviewApp() {
            const [selectedRole, setSelectedRole] = useState("Full Stack Developer");
            const [selectedDifficulty, setSelectedDifficulty] = useState("Medium");
            const [selectedCount, setSelectedCount] = useState(5);

            const [sessionState, setSessionState] = useState("IDLE"); // IDLE, ACTIVE, REPORT
            const [sessionId, setSessionId] = useState(null);
            const [currentQuestion, setCurrentQuestion] = useState(null);
            const [totalQuestions, setTotalQuestions] = useState(5);
            const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);

            const [userAnswer, setUserAnswer] = useState("");
            const [isStarting, setIsStarting] = useState(false);
            const [isEvaluating, setIsEvaluating] = useState(false);
            const [isFinishing, setIsFinishing] = useState(false);

            const [evalFeedback, setEvalFeedback] = useState(null);
            const [finalReport, setFinalReport] = useState(null);
            const [timerSeconds, setTimerSeconds] = useState(150);
            const [webcamEnabled, setWebcamEnabled] = useState(false);
            const [isListening, setIsListening] = useState(false);
            const [autoAdvanceTimer, setAutoAdvanceTimer] = useState(null);
            const [resumeAlert, setResumeAlert] = useState(null);

            const videoRef = useRef(null);
            const timerRef = useRef(null);

            const getCtx = () => (typeof window.CONTEXT_PATH === 'string' ? window.CONTEXT_PATH : '');

            // Active Session Check on Mount to gracefully recover on page refresh
            useEffect(() => {
                const checkActiveSession = async () => {
                    try {
                        const res = await fetch(getCtx() + '/api/interview/active-session');
                        if (res.ok) {
                            const data = await res.json();
                            if (data && data.success && data.hasActiveSession) {
                                setSessionId(data.sessionId);
                                setSelectedRole(data.jobRole || "Full Stack Developer");
                                setSelectedDifficulty(data.difficulty || "Medium");
                                setTotalQuestions(data.totalQuestions || 5);
                                setCurrentQuestionIndex(data.currentQuestionIndex || 0);
                                setCurrentQuestion(data.currentQuestion);
                                setSessionState("ACTIVE");
                                setTimerSeconds(150);
                                setResumeAlert("Resumed ongoing interview session.");
                                setTimeout(() => setResumeAlert(null), 5000);
                            }
                        }
                    } catch (e) {
                        console.log("No active session to resume.");
                    }
                };
                checkActiveSession();
            }, []);

            // Timer countdown
            useEffect(() => {
                if (sessionState === "ACTIVE" && timerSeconds > 0 && !evalFeedback) {
                    timerRef.current = setInterval(() => {
                        setTimerSeconds(prev => prev - 1);
                    }, 1000);
                } else if (timerSeconds === 0 && sessionState === "ACTIVE" && !evalFeedback && !isEvaluating) {
                    handleAnswerSubmit();
                }
                return () => clearInterval(timerRef.current);
            }, [sessionState, timerSeconds, evalFeedback, isEvaluating]);

            // Clean up webcam stream on unmount
            useEffect(() => {
                return () => {
                    if (videoRef.current && videoRef.current.srcObject) {
                        videoRef.current.srcObject.getTracks().forEach(t => t.stop());
                    }
                    if (autoAdvanceTimer) clearTimeout(autoAdvanceTimer);
                };
            }, [autoAdvanceTimer]);

            // Speech Synthesis
            const speakQuestion = (text) => {
                if ('speechSynthesis' in window && text) {
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
                    alert("Speech recognition is not supported in this browser. Please type your answer.");
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
                    setUserAnswer(prev => (prev ? prev + " " : "") + transcript);
                };
                recognition.onerror = () => setIsListening(false);
                recognition.onend = () => setIsListening(false);
                recognition.start();
            };

            const startInterview = async () => {
                if (isStarting) return;
                setIsStarting(true);
                try {
                    const res = await fetch(getCtx() + '/api/interview/start', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            role: selectedRole,
                            difficulty: selectedDifficulty,
                            questionCount: selectedCount
                        })
                    });
                    const data = await res.json();
                    if (data && data.success) {
                        setSessionId(data.sessionId);
                        setCurrentQuestion(data.firstQuestion);
                        setTotalQuestions(data.totalQuestions || selectedCount);
                        setCurrentQuestionIndex(0);
                        setSessionState("ACTIVE");
                        setTimerSeconds(150);
                        setUserAnswer("");
                        setEvalFeedback(null);
                        if (data.firstQuestion && data.firstQuestion.questionText) {
                            speakQuestion(data.firstQuestion.questionText);
                        }
                    } else {
                        alert((data && (data.error || data.message)) || "Failed to start interview. Please log in.");
                    }
                } catch (e) {
                    console.error("Failed to start session", e);
                    alert("Network error starting interview. Please check your connection.");
                } finally {
                    setIsStarting(false);
                }
            };

            const handleAnswerSubmit = async () => {
                if (!sessionId || isEvaluating) return;
                setIsEvaluating(true);
                try {
                    const res = await fetch(getCtx() + '/api/interview/answer', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: sessionId,
                            questionId: currentQuestion ? currentQuestion.id : (currentQuestionIndex + 1),
                            answer: userAnswer
                        })
                    });
                    const data = await res.json();
                    if (data && data.success) {
                        setEvalFeedback({
                            score: data.score,
                            feedback: data.feedback,
                            strengths: data.strengths || [],
                            areasToImprove: data.areasToImprove || [],
                            isCompleted: data.isCompleted,
                            nextQuestion: data.nextQuestion,
                            nextQuestionIndex: data.nextQuestionIndex
                        });

                        // Auto advance after review window
                        const timerId = setTimeout(() => {
                            advanceToNextQuestion(data);
                        }, 4000);
                        setAutoAdvanceTimer(timerId);
                    } else {
                        alert((data && (data.error || data.message)) || "Evaluation failed. Please try submitting again.");
                    }
                } catch (e) {
                    console.error("Answer submission failed", e);
                    alert("Network error evaluating answer. Please retry.");
                } finally {
                    setIsEvaluating(false);
                }
            };

            const advanceToNextQuestion = async (data) => {
                if (autoAdvanceTimer) clearTimeout(autoAdvanceTimer);
                if (data.isCompleted) {
                    await finishInterview();
                } else if (data.nextQuestion) {
                    setCurrentQuestion(data.nextQuestion);
                    setCurrentQuestionIndex(data.nextQuestionIndex);
                    setUserAnswer("");
                    setEvalFeedback(null);
                    setTimerSeconds(150);
                    speakQuestion(data.nextQuestion.questionText);
                }
            };

            const finishInterview = async () => {
                if (isFinishing || !sessionId) return;
                setIsFinishing(true);
                try {
                    const res = await fetch(getCtx() + '/api/interview/finish', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ sessionId: sessionId })
                    });
                    const data = await res.json();
                    if (data && data.success) {
                        setFinalReport(data.report);
                        setSessionState("REPORT");
                        if (window.confetti) {
                            window.confetti({ particleCount: 120, spread: 70, origin: { y: 0.6 } });
                        }
                    } else {
                        alert("Could not load final report: " + ((data && (data.error || data.message)) || "Unknown error"));
                    }
                } catch (e) {
                    console.error("Finish interview failed", e);
                    alert("Network error generating final report.");
                } finally {
                    setIsFinishing(false);
                }
            };

            // RENDER: IDLE SETUP STATE
            if (sessionState === "IDLE") {
                return (
                    <div className="glass-card" style={{ padding: '2.5rem', textAlign: 'center' }}>
                        <div style={{ width: '68px', height: '68px', borderRadius: '50%', background: 'linear-gradient(135deg, #4f46e5, #06b6d4)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem', color: 'white', fontSize: '1.85rem', boxShadow: '0 10px 25px -5px rgba(79, 70, 229, 0.4)' }}>
                            &#127897;
                        </div>
                        <h2 style={{ fontSize: '1.75rem', marginBottom: '0.75rem' }}>AI Placement Mock Interview Studio</h2>
                        <p style={{ color: 'var(--text-secondary)', maxWidth: '580px', margin: '0 auto 2rem', fontSize: '0.95rem' }}>
                            Simulate authentic company technical, system design, and HR rounds powered by Google Gemini AI with real-time feedback, live camera practice mirror, and comprehensive placement scoring.
                        </p>

                        {/* Step 1: Select Job Role */}
                        <div style={{ marginBottom: '2rem' }}>
                            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.75rem' }}>
                                1. Target Placement Role
                            </label>
                            <div style={{ display: 'flex', justifyContent: 'center', gap: '0.65rem', flexWrap: 'wrap' }}>
                                {AVAILABLE_ROLES.map(role => (
                                    <button
                                        key={role}
                                        type="button"
                                        onClick={() => setSelectedRole(role)}
                                        className={`option-chip ${selectedRole === role ? 'active' : ''}`}
                                    >
                                        {role}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Step 2: Select Difficulty Level */}
                        <div style={{ marginBottom: '2rem' }}>
                            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.75rem' }}>
                                2. Interview Difficulty Level
                            </label>
                            <div style={{ display: 'flex', justifyContent: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
                                {DIFFICULTIES.map(d => (
                                    <button
                                        key={d.id}
                                        type="button"
                                        onClick={() => setSelectedDifficulty(d.id)}
                                        className={`option-chip ${selectedDifficulty === d.id ? 'active' : ''}`}
                                        style={{ minWidth: '170px', textAlign: 'left', padding: '0.75rem 1rem' }}
                                    >
                                        <div style={{ fontWeight: 700 }}>{d.label}</div>
                                        <div style={{ fontSize: '0.75rem', opacity: 0.85, marginTop: '2px' }}>{d.desc}</div>
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Step 3: Question Count / Duration */}
                        <div style={{ marginBottom: '2.5rem' }}>
                            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.75rem' }}>
                                3. Assessment Length
                            </label>
                            <div style={{ display: 'flex', justifyContent: 'center', gap: '0.65rem', flexWrap: 'wrap' }}>
                                {QUESTION_COUNTS.map(qc => (
                                    <button
                                        key={qc.count}
                                        type="button"
                                        onClick={() => setSelectedCount(qc.count)}
                                        className={`option-chip ${selectedCount === qc.count ? 'active' : ''}`}
                                    >
                                        {qc.label} &bull; <span style={{ opacity: 0.85 }}>{qc.tag}</span>
                                    </button>
                                ))}
                            </div>
                        </div>

                        <button
                            type="button"
                            onClick={startInterview}
                            disabled={isStarting}
                            className="btn btn-primary"
                            style={{ padding: '0.9rem 3rem', fontSize: '1.05rem', borderRadius: '12px', boxShadow: '0 10px 20px -3px rgba(79, 70, 229, 0.35)', fontWeight: 700 }}
                        >
                            {isStarting ? 'Generating Gemini AI Questions...' : 'Start AI Interview Session \u2192'}
                        </button>
                    </div>
                );
            }

            // RENDER: ACTIVE INTERVIEW STUDIO
            if (sessionState === "ACTIVE") {
                return (
                    <div>
                        {resumeAlert && (
                            <div style={{ background: '#e0e7ff', color: '#4338ca', padding: '0.75rem 1.25rem', borderRadius: '8px', marginBottom: '1rem', fontWeight: 600, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span>&#9432; {resumeAlert}</span>
                                <button type="button" onClick={() => setResumeAlert(null)} style={{ background: 'transparent', border: 'none', cursor: 'pointer', fontWeight: 700 }}>&times;</button>
                            </div>
                        )}

                        <div className="interview-grid">
                            {/* Main Interaction Area */}
                            <div className="glass-card" style={{ padding: '2rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', flexWrap: 'wrap' }}>
                                        <span style={{ background: '#e0e7ff', color: '#4338ca', padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 700 }}>
                                            {currentQuestion ? currentQuestion.category : 'Technical'}
                                        </span>
                                        <span style={{ background: '#f1f5f9', color: '#475569', padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 600 }}>
                                            {selectedRole} &bull; {selectedDifficulty}
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

                                {/* Question Statement */}
                                <div style={{ background: 'rgba(99, 102, 241, 0.05)', border: '1px solid rgba(99, 102, 241, 0.2)', padding: '1.5rem', borderRadius: '12px', marginBottom: '1.5rem' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem' }}>
                                        <h3 style={{ fontSize: '1.2rem', lineHeight: 1.5, margin: 0 }}>
                                            {currentQuestion ? currentQuestion.questionText : "Loading Gemini AI question..."}
                                        </h3>
                                        <button
                                            type="button"
                                            onClick={() => currentQuestion && speakQuestion(currentQuestion.questionText)}
                                            title="Read question aloud"
                                            style={{ background: '#eef2ff', border: 'none', borderRadius: '50%', width: '36px', height: '36px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1rem', color: '#4f46e5' }}
                                        >
                                            &#128266;
                                        </button>
                                    </div>
                                </div>

                                {/* Candidate Answer Area */}
                                <div style={{ marginBottom: '1.25rem' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                        <label style={{ fontSize: '0.875rem', fontWeight: 600 }}>Your Response (Type or Dictate)</label>
                                        <button
                                            type="button"
                                            onClick={toggleVoiceRecognition}
                                            style={{ background: isListening ? '#fee2e2' : '#f1f5f9', border: '1px solid var(--border-subtle)', borderRadius: '6px', padding: '4px 10px', fontSize: '0.8rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: isListening ? '#991b1b' : 'inherit', fontWeight: 600 }}
                                        >
                                            <span>&#127908;</span> {isListening ? 'Listening...' : 'Voice Dictation'}
                                        </button>
                                    </div>
                                    <textarea
                                        rows="6"
                                        value={userAnswer}
                                        onChange={(e) => setUserAnswer(e.target.value)}
                                        disabled={isEvaluating || evalFeedback !== null}
                                        placeholder="Articulate your thought process, architectural rationale, code syntax, and real-world examples..."
                                        style={{ width: '100%', padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-subtle)', background: 'var(--surface)', fontSize: '0.95rem', resize: 'vertical', lineHeight: 1.5 }}
                                    />
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.35rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                        <span>Word count: {userAnswer.trim() ? userAnswer.trim().split(/\s+/).length : 0} words</span>
                                        <span>Tip: Include edge cases and trade-offs</span>
                                    </div>
                                </div>

                                {/* Live Evaluation Feedback Banner */}
                                {evalFeedback && (
                                    <div className="animate-fade-in-up" style={{ padding: '1.25rem', background: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: '10px', marginBottom: '1.25rem', color: '#065f46' }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                            <div style={{ fontWeight: 800, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                <span>&#9989;</span>
                                                <span>AI Evaluation Score: {evalFeedback.score}/10</span>
                                            </div>
                                            <button
                                                type="button"
                                                onClick={() => advanceToNextQuestion(evalFeedback)}
                                                className="btn btn-primary btn-sm"
                                                style={{ fontSize: '0.8rem', padding: '4px 12px' }}
                                            >
                                                Next Question \u2192
                                            </button>
                                        </div>
                                        <p style={{ margin: '0 0 0.75rem 0', fontSize: '0.9rem', lineHeight: 1.5 }}>
                                            {evalFeedback.feedback}
                                        </p>
                                        {evalFeedback.strengths && evalFeedback.strengths.length > 0 && (
                                            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '0.35rem' }}>
                                                {evalFeedback.strengths.map((str, idx) => (
                                                    <span key={idx} className="eval-tag eval-tag-strength">&#10003; {str}</span>
                                                ))}
                                            </div>
                                        )}
                                        {evalFeedback.areasToImprove && evalFeedback.areasToImprove.length > 0 && (
                                            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                                                {evalFeedback.areasToImprove.map((gap, idx) => (
                                                    <span key={idx} className="eval-tag eval-tag-improve">&#9888; {gap}</span>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Action Buttons */}
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
                                    <button
                                        type="button"
                                        onClick={finishInterview}
                                        disabled={isFinishing || isEvaluating}
                                        className="btn btn-secondary btn-sm"
                                        style={{ fontSize: '0.8rem', color: '#ef4444' }}
                                    >
                                        {isFinishing ? 'Concluding...' : 'Finish Early & View Report'}
                                    </button>

                                    <button
                                        type="button"
                                        onClick={handleAnswerSubmit}
                                        disabled={isEvaluating || evalFeedback !== null}
                                        className="btn btn-primary"
                                        style={{ padding: '0.75rem 2rem', borderRadius: '8px', fontWeight: 700 }}
                                    >
                                        {isEvaluating ? 'Evaluating with Gemini AI...' : (currentQuestionIndex + 1 === totalQuestions ? 'Submit & Finalize Interview \u2192' : 'Submit Answer \u2192')}
                                    </button>
                                </div>
                            </div>

                            {/* Sidebar: Webcam, Role, Tips */}
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                <div className="glass-card" style={{ padding: '1rem' }}>
                                    <div className="webcam-box">
                                        <div className="ai-indicator-badge">
                                            <div className="live-pulse"></div>
                                            <span>AI PROCTORING ACTIVE</span>
                                        </div>
                                        <video ref={videoRef} autoPlay playsInline muted style={{ display: webcamEnabled ? 'block' : 'none' }}></video>
                                        {!webcamEnabled && (
                                            <div style={{ textAlign: 'center', color: '#94a3b8' }}>
                                                <div style={{ fontSize: '2.5rem', marginBottom: '0.25rem' }}>&#128104;&#8205;&#128187;</div>
                                                <div style={{ fontSize: '0.75rem', fontWeight: 600 }}>Webcam Off (Avatar Mode)</div>
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
                                    <h4 style={{ fontSize: '0.8rem', marginBottom: '0.35rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Target Role & Tier</h4>
                                    <div style={{ fontWeight: 700, fontSize: '1.05rem', color: 'var(--primary)' }}>{selectedRole}</div>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '2px' }}>Level: <strong>{selectedDifficulty}</strong></div>

                                    <div style={{ marginTop: '1rem', borderTop: '1px solid var(--border-subtle)', paddingTop: '0.75rem', fontSize: '0.8rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                                        &#128161; <strong>Interview Scoring Tip:</strong> Structure behavioral answers using the <strong>STAR</strong> method (Situation, Task, Action, Result) and state quantifiable outcomes.
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                );
            }

            // RENDER: FINAL PLACEMENT EVALUATION REPORT
            if (sessionState === "REPORT" && finalReport) {
                return (
                    <div className="glass-card animate-fade-in-up" style={{ padding: '2.5rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1.5rem', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
                            <div>
                                <span style={{ background: '#ecfdf5', color: '#047857', padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 700 }}>
                                    &#10003; INTERVIEW EVALUATION COMPLETED
                                </span>
                                <h2 style={{ fontSize: '1.85rem', marginTop: '0.5rem', marginBottom: '0.25rem' }}>Placement Readiness Evaluation Report</h2>
                                <p style={{ color: 'var(--text-secondary)', margin: 0, fontSize: '0.95rem' }}>
                                    Candidate: <strong>{finalReport.studentName || 'Candidate'}</strong> &bull; Target Role: <strong>{finalReport.jobRole}</strong> ({finalReport.difficulty || selectedDifficulty})
                                </p>
                            </div>
                            <div style={{ display: 'flex', gap: '0.75rem' }}>
                                <button
                                    type="button"
                                    onClick={() => window.print()}
                                    className="btn btn-secondary btn-sm"
                                >
                                    &#128438; Print / Save PDF
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setSessionState("IDLE")}
                                    className="btn btn-primary btn-sm"
                                >
                                    Start New Session \u2192
                                </button>
                            </div>
                        </div>

                        {/* Top Score Cards */}
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
                            <div style={{ background: 'rgba(99, 102, 241, 0.08)', padding: '1.5rem', borderRadius: '12px', textAlign: 'center', border: '1px solid rgba(99, 102, 241, 0.2)' }}>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase' }}>OVERALL PLACEMENT READINESS</div>
                                <div style={{ fontSize: '2.75rem', fontWeight: 800, color: finalReport.overallScore >= 70 ? '#10b981' : (finalReport.overallScore >= 50 ? '#f59e0b' : '#ef4444'), margin: '0.5rem 0' }}>
                                    {finalReport.overallScore}%
                                </div>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                                    {finalReport.overallScore >= 75 ? 'Tier-1 Candidate' : (finalReport.overallScore >= 60 ? 'Tier-2 Candidate' : 'Preparation Required')}
                                </div>
                            </div>
                            <div style={{ background: 'rgba(16, 185, 129, 0.08)', padding: '1.5rem', borderRadius: '12px', textAlign: 'center', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase' }}>TECHNICAL PROFICIENCY</div>
                                <div style={{ fontSize: '2.75rem', fontWeight: 800, color: '#10b981', margin: '0.5rem 0' }}>
                                    {finalReport.techScore}%
                                </div>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Code Syntax, Architecture & Concepts</div>
                            </div>
                            <div style={{ background: 'rgba(59, 130, 246, 0.08)', padding: '1.5rem', borderRadius: '12px', textAlign: 'center', border: '1px solid rgba(59, 130, 246, 0.2)' }}>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase' }}>COMMUNICATION & STAR</div>
                                <div style={{ fontSize: '2.75rem', fontWeight: 800, color: '#3b82f6', margin: '0.5rem 0' }}>
                                    {finalReport.commScore}%
                                </div>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Clarity, Tone & Articulation</div>
                            </div>
                        </div>

                        {/* Executive Placement Summary */}
                        <div style={{ background: 'var(--surface-hover)', padding: '1.5rem', borderRadius: '12px', marginBottom: '2rem', lineHeight: 1.6, border: '1px solid var(--border-subtle)' }}>
                            <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '1rem', color: 'var(--primary)' }}>&#127891; AI Executive Evaluation Summary</h4>
                            <p style={{ margin: 0, fontSize: '0.95rem', color: 'var(--text-primary)' }}>{finalReport.summary}</p>
                        </div>

                        {/* Strengths & Recommendations */}
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '2.5rem' }}>
                            <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', padding: '1.5rem', borderRadius: '12px' }}>
                                <h4 style={{ color: '#166534', margin: '0 0 0.75rem 0', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <span>&#9989;</span> Candidate Key Strengths
                                </h4>
                                <ul style={{ margin: 0, paddingLeft: '1.25rem', color: '#14532d', fontSize: '0.9rem', lineHeight: 1.6 }}>
                                    {finalReport.strengths && finalReport.strengths.map((s, idx) => (
                                        <li key={idx} style={{ marginBottom: '0.35rem' }}>{s}</li>
                                    ))}
                                </ul>
                            </div>
                            <div style={{ background: '#fffbeb', border: '1px solid #fde68a', padding: '1.5rem', borderRadius: '12px' }}>
                                <h4 style={{ color: '#92400e', margin: '0 0 0.75rem 0', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <span>&#9888;</span> Actionable Study & Revision Steps
                                </h4>
                                <ul style={{ margin: 0, paddingLeft: '1.25rem', color: '#78350f', fontSize: '0.9rem', lineHeight: 1.6 }}>
                                    {finalReport.recommendations && finalReport.recommendations.map((r, idx) => (
                                        <li key={idx} style={{ marginBottom: '0.35rem' }}>{r}</li>
                                    ))}
                                </ul>
                            </div>
                        </div>

                        {/* Question by Question Detailed Breakdown */}
                        {finalReport.questions && finalReport.questions.length > 0 && (
                            <div>
                                <h3 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>&#128221; Question-by-Question AI Analysis</h3>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                    {finalReport.questions.map((q, idx) => (
                                        <div key={idx} style={{ border: '1px solid var(--border-subtle)', borderRadius: '10px', padding: '1.25rem', background: 'var(--surface)' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                    <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>Q{q.id}.</span>
                                                    <span style={{ background: '#e0e7ff', color: '#4338ca', padding: '2px 8px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 600 }}>{q.category}</span>
                                                </div>
                                                <span style={{ fontWeight: 700, color: (q.score * 10) >= 70 ? '#10b981' : '#f59e0b' }}>
                                                    Score: {q.score}/10
                                                </span>
                                            </div>
                                            <p style={{ fontWeight: 600, fontSize: '0.95rem', margin: '0 0 0.5rem 0' }}>{q.questionText}</p>
                                            <div style={{ background: 'rgba(0,0,0,0.03)', padding: '0.75rem', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
                                                <strong>Your Answer:</strong> {q.userAnswer ? q.userAnswer : '<No answer recorded>'}
                                            </div>
                                            {q.aiFeedback && (
                                                <div style={{ fontSize: '0.85rem', color: '#047857', background: '#f0fdf4', padding: '0.5rem 0.75rem', borderRadius: '6px' }}>
                                                    <strong>AI Feedback:</strong> {q.aiFeedback}
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
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
