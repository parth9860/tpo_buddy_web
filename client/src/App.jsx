import React, { useState, useEffect } from 'react';

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [jobs, setJobs] = useState([]);
  const [loadingJobs, setLoadingJobs] = useState(false);
  const [appliedJobs, setAppliedJobs] = useState(new Set());

  // AI Interview State
  const [interviewRole, setInterviewRole] = useState('Full Stack Developer');
  const [interviewState, setInterviewState] = useState('IDLE'); // IDLE, RUNNING, REPORT
  const [interviewSessionId, setInterviewSessionId] = useState(null);
  const [currentQ, setCurrentQ] = useState(null);
  const [qIndex, setQIndex] = useState(0);
  const [answerText, setAnswerText] = useState('');
  const [evalResult, setEvalResult] = useState(null);
  const [interviewReport, setInterviewReport] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Chatbot State
  const [chatMessages, setChatMessages] = useState([
    { sender: 'ai', text: 'Hello! I am your AI Placement Advisor. How can I assist you with your campus interview prep today?' }
  ]);
  const [chatInput, setChatInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);

  const [metrics, setMetrics] = useState({
    readinessScore: 0,
    testsCompleted: 0,
    resumeScore: 0,
    interviewScore: 0,
    status: 'Foundation Stage',
    dsaSkill: 0,
    javaSkill: 0,
    sqlSkill: 0,
    aptitudeSkill: 0,
    eligibleDrives: 0,
    radar: { coding: 0, aptitude: 0, systemDesign: 0, resume: 0, interview: 0 }
  });

  useEffect(() => {
    fetchJobs();
    fetchMetrics();
  }, []);

  const fetchJobs = async () => {
    setLoadingJobs(true);
    try {
      const res = await fetch('/api/jobs/list');
      const data = await res.json();
      if (data && data.jobs) {
        setJobs(data.jobs);
      } else {
        setJobs([]);
      }
    } catch (e) {
      console.warn('Live jobs fetch from Firebase:', e);
      setJobs([]);
    } finally {
      setLoadingJobs(false);
    }
  };

  const fetchMetrics = async () => {
    try {
      const res = await fetch('/api/student/metrics');
      const data = await res.json();
      if (data && data.success) {
        setMetrics(data);
      }
    } catch (e) {
      console.warn('Live metrics fetch note:', e);
    }
  };

  const handleApply = (jobId) => {
    setAppliedJobs(prev => new Set([...prev, jobId]));
  };

  const sendChatMessage = async (e) => {
    if (e) e.preventDefault();
    if (!chatInput.trim() || chatLoading) return;

    const query = chatInput;
    setChatMessages(prev => [...prev, { sender: 'user', text: query }]);
    setChatInput('');
    setChatLoading(true);

    try {
      const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: query })
      });
      const data = await res.json();
      setChatMessages(prev => [...prev, { sender: 'ai', text: data.reply || 'Practice your fundamentals and explain projects using the STAR method!' }]);
    } catch (err) {
      setChatMessages(prev => [...prev, { sender: 'ai', text: 'To master technical rounds, focus on Data Structures, REST APIs, and clean code principles.' }]);
    } finally {
      setChatLoading(false);
    }
  };

  const startMockInterview = async () => {
    try {
      const res = await fetch('/api/interview/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role: interviewRole })
      });
      const data = await res.json();
      if (data.success) {
        setInterviewSessionId(data.sessionId);
        setCurrentQ(data.firstQuestion);
        setQIndex(0);
        setInterviewState('RUNNING');
        setAnswerText('');
        setEvalResult(null);
      }
    } catch (err) {
      alert('Could not start session. Ensure Spring Boot is running on port 8085.');
    }
  };

  const submitInterviewAnswer = async () => {
    if (!interviewSessionId || isSubmitting) return;
    setIsSubmitting(true);
    try {
      const res = await fetch('/api/interview/answer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sessionId: interviewSessionId,
          questionId: currentQ ? currentQ.id : 1,
          answer: answerText
        })
      });
      const data = await res.json();
      if (data.success) {
        setEvalResult({ score: data.score, feedback: data.feedback });
        setTimeout(async () => {
          if (data.isCompleted) {
            const finishRes = await fetch('/api/interview/finish', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ sessionId: interviewSessionId })
            });
            const finishData = await finishRes.json();
            setInterviewReport(finishData.report);
            setInterviewState('REPORT');
          } else if (data.nextQuestion) {
            setCurrentQ(data.nextQuestion);
            setQIndex(data.nextQuestionIndex);
            setAnswerText('');
            setEvalResult(null);
          }
          setIsSubmitting(false);
        }, 2000);
      }
    } catch (err) {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#090d16' }}>
      {/* Sidebar Navigation */}
      <aside style={{ width: '260px', background: '#0b1120', borderRight: '1px solid rgba(255,255,255,0.08)', padding: '1.5rem 1rem', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '2.5rem', paddingLeft: '0.5rem' }}>
          <div style={{ width: '38px', height: '38px', borderRadius: '10px', background: 'linear-gradient(135deg, #6366f1, #a855f7)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: '800', color: '#fff' }}>
            TB
          </div>
          <div>
            <div style={{ fontWeight: '700', fontSize: '1.1rem', color: '#fff' }}>TPO Buddy</div>
            <div style={{ fontSize: '0.75rem', color: '#818cf8', fontWeight: '600' }}>React 18 SPA</div>
          </div>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', flex: 1 }}>
          {[
            { id: 'dashboard', label: 'Dashboard & Radar', icon: '📊' },
            { id: 'interview', label: 'AI Mock Interview', icon: '🎙️' },
            { id: 'chat', label: 'Career Copilot AI', icon: '🤖' },
            { id: 'jobs', label: 'Placement Jobs', icon: '💼' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                width: '100%',
                padding: '0.75rem 1rem',
                borderRadius: '8px',
                border: activeTab === tab.id ? '1px solid rgba(99, 102, 241, 0.4)' : 'none',
                background: activeTab === tab.id ? 'linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(168, 85, 247, 0.2))' : 'transparent',
                color: activeTab === tab.id ? '#ffffff' : '#94a3b8',
                fontWeight: activeTab === tab.id ? 600 : 500,
                textAlign: 'left'
              }}
            >
              <span>{tab.icon}</span>
              <span>{tab.label}</span>
            </button>
          ))}
        </nav>

        <div style={{ borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '1rem' }}>
          <a
            href="http://localhost:8085/dashboard"
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#94a3b8', textDecoration: 'none', fontSize: '0.85rem', padding: '0.5rem' }}
          >
            <span>←</span> Switch to JSP Portal
          </a>
        </div>
      </aside>

      {/* Main Content */}
      <main style={{ flex: 1, padding: '2rem', maxWidth: '1300px', margin: '0 auto', width: '100%' }}>
        {/* Header */}
        <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: '800', margin: 0, color: '#f8fafc' }}>
              {activeTab === 'dashboard' && 'Placement Readiness Dashboard'}
              {activeTab === 'interview' && 'AI Mock Interview Studio'}
              {activeTab === 'chat' && 'Career Copilot Placement Assistant'}
              {activeTab === 'jobs' && 'Active Campus Recruitment Drives'}
            </h1>
            <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginTop: '0.25rem' }}>
              Synchronized with Spring Boot 3 & Embedded Tomcat (Port 8085)
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', padding: '6px 14px', borderRadius: '999px', color: '#34d399', fontSize: '0.8rem', fontWeight: 600 }}>
            <span>●</span> API Connected
          </div>
        </header>

        {/* Dashboard Tab */}
        {activeTab === 'dashboard' && (
          <div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
              <div className="glass-card" style={{ padding: '1.5rem' }}>
                <div style={{ fontSize: '0.8rem', color: '#94a3b8', fontWeight: 600 }}>OVERALL READINESS</div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#38bdf8', marginTop: '0.5rem' }}>{metrics.readinessScore}%</div>
                <div style={{ fontSize: '0.8rem', color: '#34d399', marginTop: '0.25rem' }}>{metrics.status || 'Live Readiness'}</div>
              </div>
              <div className="glass-card" style={{ padding: '1.5rem' }}>
                <div style={{ fontSize: '0.8rem', color: '#94a3b8', fontWeight: 600 }}>APTITUDE & TESTS</div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#a855f7', marginTop: '0.5rem' }}>{metrics.aptitudeSkill}%</div>
                <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: '0.25rem' }}>{metrics.testsCompleted} assessments completed</div>
              </div>
              <div className="glass-card" style={{ padding: '1.5rem' }}>
                <div style={{ fontSize: '0.8rem', color: '#94a3b8', fontWeight: 600 }}>RESUME ATS SCORE</div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#10b981', marginTop: '0.5rem' }}>{metrics.resumeScore}%</div>
                <div style={{ fontSize: '0.8rem', color: '#34d399', marginTop: '0.25rem' }}>Apache PDFBox verified</div>
              </div>
              <div className="glass-card" style={{ padding: '1.5rem' }}>
                <div style={{ fontSize: '0.8rem', color: '#94a3b8', fontWeight: 600 }}>MOCK INTERVIEW</div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#f59e0b', marginTop: '0.5rem' }}>{metrics.interviewScore}%</div>
                <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: '0.25rem' }}>Live Firebase Evaluation</div>
              </div>
            </div>

            {/* Quick launch grid */}
            <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: '1.5rem' }}>
              <div className="glass-card" style={{ padding: '1.5rem' }}>
                <h3 style={{ margin: '0 0 1rem 0', fontSize: '1.15rem' }}>Preparation Radar</h3>
                <div style={{ display: 'flex', justifyContent: 'center' }}>
                  <svg width="280" height="240" viewBox="0 0 280 240">
                    <polygon points="140,25 235,90 200,195 80,195 45,90" fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="1" />
                    <polygon points="140,55 205,105 180,175 100,175 75,105" fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="1" />
                    <polygon
                      points={`${140 + ((metrics.radar?.coding ?? metrics.dsaSkill ?? 0) / 100 * 80) * Math.cos(-Math.PI / 2)},${115 + ((metrics.radar?.coding ?? metrics.dsaSkill ?? 0) / 100 * 80) * Math.sin(-Math.PI / 2)} ${140 + ((metrics.radar?.aptitude ?? metrics.aptitudeSkill ?? 0) / 100 * 80) * Math.cos(-Math.PI / 2 + 2 * Math.PI / 5)},${115 + ((metrics.radar?.aptitude ?? metrics.aptitudeSkill ?? 0) / 100 * 80) * Math.sin(-Math.PI / 2 + 2 * Math.PI / 5)} ${140 + ((metrics.radar?.systemDesign ?? metrics.sqlSkill ?? 0) / 100 * 80) * Math.cos(-Math.PI / 2 + 4 * Math.PI / 5)},${115 + ((metrics.radar?.systemDesign ?? metrics.sqlSkill ?? 0) / 100 * 80) * Math.sin(-Math.PI / 2 + 4 * Math.PI / 5)} ${140 + ((metrics.radar?.resume ?? metrics.resumeScore ?? 0) / 100 * 80) * Math.cos(-Math.PI / 2 + 6 * Math.PI / 5)},${115 + ((metrics.radar?.resume ?? metrics.resumeScore ?? 0) / 100 * 80) * Math.sin(-Math.PI / 2 + 6 * Math.PI / 5)} ${140 + ((metrics.radar?.interview ?? metrics.interviewScore ?? 0) / 100 * 80) * Math.cos(-Math.PI / 2 + 8 * Math.PI / 5)},${115 + ((metrics.radar?.interview ?? metrics.interviewScore ?? 0) / 100 * 80) * Math.sin(-Math.PI / 2 + 8 * Math.PI / 5)}`}
                      fill="rgba(99, 102, 241, 0.3)"
                      stroke="#818cf8"
                      strokeWidth="2"
                    />
                    <text x="140" y="16" fill="#94a3b8" fontSize="11" textAnchor="middle">Coding ({metrics.radar?.coding ?? metrics.dsaSkill ?? 0}%)</text>
                    <text x="245" y="95" fill="#94a3b8" fontSize="11">Aptitude ({metrics.radar?.aptitude ?? metrics.aptitudeSkill ?? 0}%)</text>
                    <text x="210" y="215" fill="#94a3b8" fontSize="11">System Design ({metrics.radar?.systemDesign ?? metrics.sqlSkill ?? 0}%)</text>
                    <text x="40" y="215" fill="#94a3b8" fontSize="11">Resume ({metrics.radar?.resume ?? metrics.resumeScore ?? 0}%)</text>
                    <text x="5" y="95" fill="#94a3b8" fontSize="11">Interview ({metrics.radar?.interview ?? metrics.interviewScore ?? 0}%)</text>
                  </svg>
                </div>
              </div>

              <div className="glass-card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.15rem' }}>Quick Actions</h3>
                <button
                  onClick={() => setActiveTab('interview')}
                  className="btn-primary"
                  style={{ padding: '0.85rem', width: '100%', textAlign: 'left' }}
                >
                  🎙️ <strong>Launch AI Mock Interview</strong>
                  <div style={{ fontSize: '0.75rem', opacity: 0.8 }}>Practice technical questions with instant grading</div>
                </button>
                <button
                  onClick={() => setActiveTab('chat')}
                  className="btn-secondary"
                  style={{ padding: '0.85rem', width: '100%', textAlign: 'left' }}
                >
                  🤖 <strong>Ask Career Copilot AI</strong>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Get placement strategy & interview tips</div>
                </button>
                <button
                  onClick={() => setActiveTab('jobs')}
                  className="btn-secondary"
                  style={{ padding: '0.85rem', width: '100%', textAlign: 'left' }}
                >
                  💼 <strong>Browse Campus Drives</strong>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Apply to verified on-campus opportunities</div>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* AI Mock Interview Tab */}
        {activeTab === 'interview' && (
          <div className="glass-card" style={{ padding: '2rem' }}>
            {interviewState === 'IDLE' && (
              <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
                <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🎙️</div>
                <h2 style={{ fontSize: '1.6rem', marginBottom: '0.5rem' }}>Choose Target Interview Track</h2>
                <p style={{ color: '#94a3b8', maxWidth: '500px', margin: '0 auto 1.5rem' }}>
                  Select the engineering role you are preparing for. The AI will evaluate both your technical depth and communication clarity.
                </p>

                <div style={{ display: 'flex', justifyContent: 'center', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '2rem' }}>
                  {['Full Stack Developer', 'Frontend React Developer', 'Java Backend Developer', 'Data Analyst & AI'].map(role => (
                    <button
                      key={role}
                      onClick={() => setInterviewRole(role)}
                      className={interviewRole === role ? 'btn-primary' : 'btn-secondary'}
                      style={{ padding: '0.65rem 1.25rem', borderRadius: '8px' }}
                    >
                      {role}
                    </button>
                  ))}
                </div>

                <button onClick={startMockInterview} className="btn-primary" style={{ padding: '0.85rem 2.5rem', fontSize: '1.05rem', borderRadius: '10px' }}>
                  Begin Mock Interview →
                </button>
              </div>
            )}

            {interviewState === 'RUNNING' && (
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                  <span style={{ background: '#3b82f6', color: '#fff', padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 600 }}>
                    {currentQ?.category || 'Technical'}
                  </span>
                  <span style={{ color: '#94a3b8', fontSize: '0.9rem' }}>
                    Question {qIndex + 1} of 5
                  </span>
                </div>

                <div style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '10px', padding: '1.5rem', marginBottom: '1.5rem' }}>
                  <h3 style={{ margin: 0, fontSize: '1.25rem', lineHeight: 1.5 }}>
                    {currentQ?.questionText}
                  </h3>
                </div>

                <div style={{ marginBottom: '1.5rem' }}>
                  <label style={{ display: 'block', fontSize: '0.85rem', color: '#94a3b8', marginBottom: '0.5rem', fontWeight: 600 }}>
                    Your Explanation / Answer
                  </label>
                  <textarea
                    rows={6}
                    value={answerText}
                    onChange={(e) => setAnswerText(e.target.value)}
                    placeholder="Provide your technical explanation, architecture, edge cases, and practical examples..."
                    style={{ width: '100%', padding: '1rem', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(0,0,0,0.3)', color: '#fff', fontSize: '0.95rem' }}
                  />
                </div>

                {evalResult && (
                  <div style={{ padding: '1rem', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: '8px', marginBottom: '1.5rem', color: '#34d399' }}>
                    <strong>Score: {evalResult.score}/10</strong> • {evalResult.feedback}
                  </div>
                )}

                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <button onClick={submitInterviewAnswer} disabled={isSubmitting} className="btn-primary" style={{ padding: '0.75rem 2rem' }}>
                    {isSubmitting ? 'Evaluating with AI...' : 'Submit Answer →'}
                  </button>
                </div>
              </div>
            )}

            {interviewState === 'REPORT' && interviewReport && (
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.08)', paddingBottom: '1rem' }}>
                  <div>
                    <h2 style={{ margin: 0, fontSize: '1.5rem' }}>Placement Readiness Report</h2>
                    <div style={{ color: '#818cf8', fontSize: '0.9rem', marginTop: '0.25rem' }}>Role: {interviewReport.jobRole}</div>
                  </div>
                  <button onClick={() => setInterviewState('IDLE')} className="btn-secondary">
                    Start Another Track
                  </button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem', marginBottom: '2rem' }}>
                  <div style={{ background: 'rgba(99, 102, 241, 0.1)', padding: '1.5rem', borderRadius: '10px', textAlign: 'center', border: '1px solid rgba(99, 102, 241, 0.3)' }}>
                    <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>OVERALL SCORE</div>
                    <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#818cf8' }}>{interviewReport.overallScore}%</div>
                  </div>
                  <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '1.5rem', borderRadius: '10px', textAlign: 'center', border: '1px solid rgba(16, 185, 129, 0.3)' }}>
                    <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>TECHNICAL DEPTH</div>
                    <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#34d399' }}>{interviewReport.techScore}%</div>
                  </div>
                  <div style={{ background: 'rgba(59, 130, 246, 0.1)', padding: '1.5rem', borderRadius: '10px', textAlign: 'center', border: '1px solid rgba(59, 130, 246, 0.3)' }}>
                    <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>COMMUNICATION</div>
                    <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#60a5fa' }}>{interviewReport.commScore}%</div>
                  </div>
                </div>

                <div style={{ background: 'rgba(255,255,255,0.03)', padding: '1.25rem', borderRadius: '8px', lineHeight: 1.6, marginBottom: '1.5rem' }}>
                  <strong>Evaluation Summary: </strong> {interviewReport.summary}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Career Copilot AI Chat */}
        {activeTab === 'chat' && (
          <div className="glass-card" style={{ display: 'flex', flexDirection: 'column', height: '620px', padding: '1.5rem' }}>
            <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '1rem', paddingRight: '0.5rem' }}>
              {chatMessages.map((msg, i) => (
                <div
                  key={i}
                  style={{
                    alignSelf: msg.sender === 'user' ? 'flex-end' : 'flex-start',
                    maxWidth: '75%',
                    padding: '0.85rem 1.25rem',
                    borderRadius: '12px',
                    background: msg.sender === 'user' ? '#6366f1' : 'rgba(255,255,255,0.08)',
                    color: '#fff',
                    fontSize: '0.9rem',
                    lineHeight: 1.5
                  }}
                >
                  {msg.text}
                </div>
              ))}
              {chatLoading && (
                <div style={{ alignSelf: 'flex-start', color: '#94a3b8', fontStyle: 'italic', fontSize: '0.85rem' }}>
                  Copilot is thinking...
                </div>
              )}
            </div>

            <form onSubmit={sendChatMessage} style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <input
                type="text"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                placeholder="Ask anything about placement prep, resumes, or interview questions..."
                style={{ flex: 1, padding: '0.85rem', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(0,0,0,0.3)', color: '#fff' }}
              />
              <button type="submit" disabled={chatLoading} className="btn-primary" style={{ padding: '0.85rem 1.5rem' }}>
                Send
              </button>
            </form>
          </div>
        )}

        {/* Jobs Tab */}
        {activeTab === 'jobs' && (
          <div>
            {loadingJobs ? (
              <div style={{ textAlign: 'center', padding: '3rem', color: '#94a3b8' }}>Loading campus recruitment drives...</div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
                {jobs.map(job => (
                  <div key={job.id} className="glass-card" style={{ padding: '1.5rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <h3 style={{ margin: 0, fontSize: '1.15rem' }}>{job.title}</h3>
                      <span style={{ background: '#3b82f6', color: '#fff', fontSize: '0.75rem', padding: '3px 8px', borderRadius: '6px' }}>
                        {job.packageInfo || (job.packageLpa ? job.packageLpa + ' LPA' : 'Competitive')}
                      </span>
                    </div>
                    <div style={{ color: '#818cf8', fontWeight: 600, margin: '0.35rem 0' }}>{job.company}</div>
                    <p style={{ fontSize: '0.85rem', color: '#94a3b8', margin: '0.75rem 0' }}>{job.description}</p>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem', borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '0.75rem' }}>
                      <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                        {job.eligibility || (job.minCgpa ? 'Min CGPA: ' + job.minCgpa : 'All Branches')}
                      </span>
                      <button
                        onClick={() => handleApply(job.id)}
                        disabled={appliedJobs.has(job.id) || job.applied}
                        className={appliedJobs.has(job.id) || job.applied ? 'btn-secondary' : 'btn-primary'}
                        style={{ fontSize: '0.8rem', padding: '5px 12px' }}
                      >
                        {appliedJobs.has(job.id) || job.applied ? 'Applied ✓' : 'Apply Now'}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
