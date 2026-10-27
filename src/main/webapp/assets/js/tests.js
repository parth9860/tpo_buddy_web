/**
 * TPO Buddy — Company Placement Preparation Test Series Engine
 * Full-featured interactive examination system with:
 * - Pre-test instruction & camera verification
 * - Mandatory Fullscreen enforcement
 * - Three-Strike Security Rule (tab switch & fullscreen exit detection)
 * - Persistent countdown timer across page reloads
 * - Dynamic Question Palette (Answered, Unanswered, Marked for Review)
 * - Auto-submission on timer expiry or 3rd security strike
 * - Instant server grading with detailed step-by-step solution review
 */

let activeAttemptId = null;
let currentQuestions = [];
let currentIndex = 0;
let userAnswers = {};
let markedForReview = new Set();
let timerInterval = null;
let remainingSeconds = 1800; // 30 mins default
let cameraStream = null;
let selectedCategory = 'Quantitative Aptitude';
let selectedTopic = null;
let selectedTestId = null;
let selectedCount = 25;
let isSubmitting = false;
let violationCount = 0;
let lastViolationTime = 0;
let watchdogsAttached = false;

window.stopProctorWebcam = function() {
    if (cameraStream) {
        try {
            cameraStream.getTracks().forEach(t => t.stop());
        } catch (e) {
            console.warn('Error stopping camera:', e);
        }
        cameraStream = null;
    }
};

// -------------------------------------------------------------
// Pre-flight Instructions & Camera Check
// -------------------------------------------------------------

function initiateExamSetup(category, count = 25, testId = null, topic = null) {
    selectedCategory = category;
    selectedCount = count;
    selectedTestId = testId;
    selectedTopic = topic;

    const modalTitle = document.getElementById('setupModalTitle');
    if (modalTitle) {
        modalTitle.textContent = `${topic ? topic : category} — Assessment Setup`;
    }
    const catDetail = document.getElementById('setupCategoryDetail');
    if (catDetail) {
        catDetail.textContent = `${category} • ${topic ? topic + ' • ' : ''}${count} Questions • ${count <= 10 ? '15' : '30'} Minutes`;
    }

    const consent = document.getElementById('examConsentCheckbox');
    if (consent) consent.checked = false;

    const startBtn = document.getElementById('btnStartExamConfirmed');
    if (startBtn) {
        startBtn.disabled = true;
        startBtn.textContent = 'Start Placement Test →';
    }

    const preview = document.getElementById('setupWebcamPreview');
    const placeholder = document.getElementById('setupWebcamPlaceholder');
    const statusDot = document.getElementById('setupStatusDot');
    const statusText = document.getElementById('setupStatusText');

    if (preview) { preview.style.display = 'none'; preview.srcObject = null; }
    if (placeholder) { placeholder.style.display = 'block'; placeholder.textContent = '📷 Camera Not Connected'; }
    if (statusDot) statusDot.style.background = '#94a3b8';
    if (statusText) {
        statusText.textContent = 'Optional webcam verification for proctored tests';
        statusText.style.color = 'var(--text-secondary)';
    }

    const modal = document.getElementById('examSetupModal');
    if (modal) modal.classList.add('active');
}

function cancelExamSetup() {
    const modal = document.getElementById('examSetupModal');
    if (modal) modal.classList.remove('active');
    window.stopProctorWebcam();
}

async function requestWebcamVerification() {
    const statusText = document.getElementById('setupStatusText');
    const statusDot = document.getElementById('setupStatusDot');
    const preview = document.getElementById('setupWebcamPreview');
    const placeholder = document.getElementById('setupWebcamPlaceholder');

    try {
        if (statusText) statusText.textContent = 'Requesting camera access...';
        const stream = await navigator.mediaDevices.getUserMedia({ video: { width: 480, height: 360 } });
        cameraStream = stream;

        if (preview) {
            preview.srcObject = stream;
            preview.style.display = 'block';
            await preview.play();
        }
        if (placeholder) placeholder.style.display = 'none';
        if (statusDot) statusDot.style.background = '#10b981';
        if (statusText) {
            statusText.textContent = '✓ Camera validated successfully';
            statusText.style.color = '#047857';
        }
    } catch (err) {
        if (statusDot) statusDot.style.background = '#f59e0b';
        if (statusText) {
            statusText.textContent = 'Camera unavailable (proceeding in standard browser mode)';
            statusText.style.color = '#b45309';
        }
    }
}

function handleConsentChange(checkbox) {
    const startBtn = document.getElementById('btnStartExamConfirmed');
    if (startBtn) {
        startBtn.disabled = !checkbox.checked;
    }
}

// -------------------------------------------------------------
// Launch Assessment in Fullscreen
// -------------------------------------------------------------

async function startExamInFullscreen() {
    const modal = document.getElementById('examSetupModal');
    if (modal) modal.classList.remove('active');

    try {
        if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
            await document.documentElement.requestFullscreen();
        }
    } catch (e) {
        console.warn('Fullscreen request bypassed:', e);
    }

    // Launch test on backend
    try {
        const payload = {
            category: selectedCategory,
            topic: selectedTopic,
            testId: selectedTestId,
            count: selectedCount
        };

        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (data.success && data.questions && data.questions.length > 0) {
            activeAttemptId = data.attemptId;
            currentQuestions = data.questions;
            currentIndex = 0;
            userAnswers = {};
            markedForReview = new Set();
            violationCount = 0;
            remainingSeconds = data.durationSeconds || (currentQuestions.length <= 10 ? 900 : 1800);

            switchToExamRunnerView(data.category, data.topic);
            startTimerCountdown();
            attachSecurityWatchdogs();
            renderQuestionPalette();
            renderCurrentQuestion();
        } else {
            alert('Failed to initialize test questions: ' + (data.error || 'Unknown error'));
        }
    } catch (err) {
        alert('Network error launching assessment test.');
    }
}

function switchToExamRunnerView(category, topic) {
    const picker = document.getElementById('testCategoryPicker');
    const runner = document.getElementById('testRunner');
    const resultCard = document.getElementById('testResultCard');

    if (picker) picker.style.display = 'none';
    if (resultCard) resultCard.style.display = 'none';
    if (runner) runner.style.display = 'block';

    const catEl = document.getElementById('runnerCategoryBadge');
    if (catEl) catEl.textContent = topic ? `${category} • ${topic}` : category;

    // Pip camera
    const pip = document.getElementById('examWebcamPip');
    if (pip && cameraStream) {
        pip.srcObject = cameraStream;
        pip.style.display = 'block';
        pip.play().catch(() => {});
    }
}

// -------------------------------------------------------------
// Question Rendering & Navigation
// -------------------------------------------------------------

function renderCurrentQuestion() {
    if (!currentQuestions || currentQuestions.length === 0) return;

    const q = currentQuestions[currentIndex];
    const total = currentQuestions.length;

    // Header updates
    const numEl = document.getElementById('runnerQuestionNumber');
    if (numEl) numEl.textContent = `Question ${currentIndex + 1} of ${total}`;

    const diffEl = document.getElementById('runnerDifficultyBadge');
    if (diffEl) {
        diffEl.textContent = q.difficulty || 'Medium';
        diffEl.className = 'badge ' + (q.difficulty === 'Easy' ? 'badge-success' : (q.difficulty === 'Hard' ? 'badge-danger' : 'badge-primary'));
    }

    const marksEl = document.getElementById('runnerMarksBadge');
    if (marksEl) marksEl.textContent = `+${q.marks || 2} Marks`;

    // Statement
    const statementEl = document.getElementById('runnerQuestionStatement');
    if (statementEl) {
        statementEl.textContent = q.question;
    }

    // Options
    const optionsContainer = document.getElementById('runnerOptionsList');
    if (optionsContainer) {
        optionsContainer.innerHTML = '';
        const options = [
            { key: 0, letter: 'A', text: q.optionA },
            { key: 1, letter: 'B', text: q.optionB },
            { key: 2, letter: 'C', text: q.optionC },
            { key: 3, letter: 'D', text: q.optionD }
        ];

        const selected = userAnswers[q.id];

        options.forEach(opt => {
            const isSelected = (selected !== undefined && selected === opt.key);
            const optDiv = document.createElement('div');
            optDiv.className = `exam-option-card ${isSelected ? 'selected' : ''}`;
            optDiv.onclick = () => selectOption(opt.key);

            optDiv.innerHTML = `
                <div class="exam-option-badge ${isSelected ? 'active' : ''}">${opt.letter}</div>
                <div class="exam-option-text">${escapeHtml(opt.text)}</div>
            `;
            optionsContainer.appendChild(optDiv);
        });
    }

    // Prev / Next button state
    const prevBtn = document.getElementById('btnPrevQuestion');
    const nextBtn = document.getElementById('btnNextQuestion');
    if (prevBtn) prevBtn.disabled = (currentIndex === 0);
    if (nextBtn) {
        if (currentIndex === total - 1) {
            nextBtn.textContent = 'Review All →';
        } else {
            nextBtn.textContent = 'Next Question →';
        }
    }

    // Mark review toggle state
    const markBtn = document.getElementById('btnMarkReview');
    if (markBtn) {
        if (markedForReview.has(q.id)) {
            markBtn.innerHTML = '★ Marked for Review';
            markBtn.style.background = '#fef3c7';
            markBtn.style.color = '#b45309';
        } else {
            markBtn.innerHTML = '☆ Mark for Review';
            markBtn.style.background = 'transparent';
            markBtn.style.color = 'var(--text-secondary)';
        }
    }

    updatePaletteHighlight();
}

function selectOption(optIndex) {
    const q = currentQuestions[currentIndex];
    userAnswers[q.id] = optIndex;
    renderCurrentQuestion();
    renderQuestionPalette();

    // Autosave progress asynchronously
    fetch(`${window.CONTEXT_PATH}/api/tests/save-progress`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers: userAnswers })
    }).catch(() => {});
}

function clearCurrentResponse() {
    const q = currentQuestions[currentIndex];
    delete userAnswers[q.id];
    renderCurrentQuestion();
    renderQuestionPalette();
}

function toggleMarkForReview() {
    const q = currentQuestions[currentIndex];
    if (markedForReview.has(q.id)) {
        markedForReview.delete(q.id);
    } else {
        markedForReview.add(q.id);
    }
    renderCurrentQuestion();
    renderQuestionPalette();
}

function navigateToQuestion(idx) {
    if (idx >= 0 && idx < currentQuestions.length) {
        currentIndex = idx;
        renderCurrentQuestion();
    }
}

function prevQuestion() {
    if (currentIndex > 0) {
        currentIndex--;
        renderCurrentQuestion();
    }
}

function nextQuestion() {
    if (currentIndex < currentQuestions.length - 1) {
        currentIndex++;
        renderCurrentQuestion();
    }
}

// -------------------------------------------------------------
// Question Palette
// -------------------------------------------------------------

function renderQuestionPalette() {
    const palette = document.getElementById('runnerQuestionPalette');
    if (!palette) return;

    palette.innerHTML = '';
    currentQuestions.forEach((q, idx) => {
        const isAnswered = (userAnswers[q.id] !== undefined);
        const isMarked = markedForReview.has(q.id);
        const isCurrent = (idx === currentIndex);

        let cls = 'palette-btn';
        if (isCurrent) cls += ' current';
        if (isAnswered) cls += ' answered';
        if (isMarked) cls += ' marked';

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = cls;
        btn.textContent = (idx + 1);
        btn.onclick = () => navigateToQuestion(idx);

        palette.appendChild(btn);
    });

    updatePaletteCounts();
}

function updatePaletteHighlight() {
    const palette = document.getElementById('runnerQuestionPalette');
    if (!palette) return;
    const btns = palette.querySelectorAll('.palette-btn');
    btns.forEach((btn, idx) => {
        if (idx === currentIndex) {
            btn.classList.add('current');
        } else {
            btn.classList.remove('current');
        }
    });
}

function updatePaletteCounts() {
    let answered = Object.keys(userAnswers).length;
    let marked = markedForReview.size;
    let total = currentQuestions.length;
    let unanswered = total - answered;

    const answeredEl = document.getElementById('paletteCountAnswered');
    if (answeredEl) answeredEl.textContent = answered;
    const unansweredEl = document.getElementById('paletteCountUnanswered');
    if (unansweredEl) unansweredEl.textContent = unanswered;
    const markedEl = document.getElementById('paletteCountMarked');
    if (markedEl) markedEl.textContent = marked;
}

// -------------------------------------------------------------
// Timer & Periodic Autosave
// -------------------------------------------------------------

function startTimerCountdown() {
    clearInterval(timerInterval);
    updateTimerDisplay();

    timerInterval = setInterval(() => {
        remainingSeconds--;
        updateTimerDisplay();

        if (remainingSeconds <= 0) {
            clearInterval(timerInterval);
            submitAssessmentTest('timeout');
        }
    }, 1000);
}

function updateTimerDisplay() {
    const timerEl = document.getElementById('runnerTimerDisplay');
    if (!timerEl) return;

    const mins = Math.floor(Math.max(0, remainingSeconds) / 60);
    const secs = Math.max(0, remainingSeconds) % 60;
    timerEl.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;

    if (remainingSeconds < 300) {
        timerEl.style.color = '#dc2626';
    } else {
        timerEl.style.color = 'var(--text-primary)';
    }
}

// -------------------------------------------------------------
// Anti-Cheat Security & Three-Strike Monitoring
// -------------------------------------------------------------

function attachSecurityWatchdogs() {
    if (watchdogsAttached) return;
    watchdogsAttached = true;

    document.addEventListener('visibilitychange', () => {
        if (document.hidden && activeAttemptId && !isSubmitting) {
            triggerSecurityViolation('TAB_SWITCH', 'Tab switch or window minimized detected');
        }
    });

    window.addEventListener('blur', () => {
        if (activeAttemptId && !isSubmitting) {
            triggerSecurityViolation('FOCUS_LOST', 'Window focus lost');
        }
    });

    document.addEventListener('fullscreenchange', () => {
        if (!document.fullscreenElement && activeAttemptId && !isSubmitting) {
            triggerSecurityViolation('FULLSCREEN_EXIT', 'Fullscreen exit detected');
        }
    });
}

function triggerSecurityViolation(type, message) {
    const now = Date.now();
    if (now - lastViolationTime < 2500) return; // Debounce rapid events
    lastViolationTime = now;

    violationCount++;

    // Log to backend
    fetch(`${window.CONTEXT_PATH}/api/tests/log-event`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: type, message: message })
    }).catch(() => {});

    if (violationCount >= 3) {
        submitAssessmentTest('automatic-security-submission');
    } else {
        showSecurityWarningModal();
    }
}

function showSecurityWarningModal() {
    const modal = document.getElementById('securityWarningModal');
    if (!modal) return;

    const title = document.getElementById('securityModalTitle');
    if (title) title.textContent = `Security Warning ${violationCount}/3`;

    const rem = document.getElementById('violationsRemainingCount');
    if (rem) rem.textContent = (3 - violationCount);

    modal.classList.add('active');
}

async function acknowledgeWarningAndReturnFullscreen() {
    const modal = document.getElementById('securityWarningModal');
    if (modal) modal.classList.remove('active');

    try {
        if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
            await document.documentElement.requestFullscreen();
        }
    } catch (e) {}
}

// -------------------------------------------------------------
// Submit Assessment & Results Display
// -------------------------------------------------------------

function confirmAndSubmitExam() {
    const total = currentQuestions.length;
    const answered = Object.keys(userAnswers).length;
    const unanswered = total - answered;

    const msg = `Are you sure you want to submit your test?\n\n• Answered: ${answered} / ${total}\n• Unanswered: ${unanswered}\n\nOnce submitted, answers cannot be modified.`;
    if (confirm(msg)) {
        submitAssessmentTest('manual');
    }
}

async function submitAssessmentTest(reason = 'manual') {
    if (isSubmitting) return;
    isSubmitting = true;
    clearInterval(timerInterval);

    // Close any open modals
    const warnModal = document.getElementById('securityWarningModal');
    if (warnModal) warnModal.classList.remove('active');

    // Exit fullscreen
    if (document.fullscreenElement && document.exitFullscreen) {
        try { await document.exitFullscreen(); } catch (e) {}
    }
    window.stopProctorWebcam();

    const submitBtn = document.getElementById('btnSubmitExam');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Submitting & Grading...';
    }

    try {
        const payload = {
            attemptId: activeAttemptId,
            answers: userAnswers,
            submissionReason: reason
        };

        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/submit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (data.success) {
            renderAssessmentResult(data);
        } else {
            alert('Submission grading error: ' + (data.error || 'Server error'));
        }
    } catch (err) {
        alert('Network error submitting test. Your answers have been preserved.');
    } finally {
        activeAttemptId = null;
        isSubmitting = false;
    }
}

function renderAssessmentResult(data) {
    const runner = document.getElementById('testRunner');
    const resultCard = document.getElementById('testResultCard');

    if (runner) runner.style.display = 'none';
    if (resultCard) resultCard.style.display = 'block';

    // Status Badge
    const badge = document.getElementById('resultStatusBadge');
    if (badge) {
        badge.textContent = data.passed ? 'PASSED (≥ 60%)' : 'NEEDS PRACTICE';
        badge.className = 'badge ' + (data.passed ? 'badge-success' : 'badge-warning');
    }

    // Score
    const scoreEl = document.getElementById('resultScorePercent');
    if (scoreEl) scoreEl.textContent = `${data.score}%`;

    // Counts
    const fracEl = document.getElementById('resultCorrectFraction');
    if (fracEl) fracEl.textContent = `${data.correctCount} / ${data.totalQuestions}`;

    // Risk
    const riskEl = document.getElementById('resultRiskDisplay');
    if (riskEl) {
        riskEl.textContent = `${data.riskLevel || 'LOW'} RISK (${data.proctoringStatus || 'VALIDATED'})`;
        riskEl.style.color = (data.riskLevel === 'HIGH' ? '#dc2626' : (data.riskLevel === 'MODERATE' ? '#d97706' : '#16a34a'));
    }

    // Render Solutions Review
    const reviewList = document.getElementById('resultReviewList');
    if (reviewList && data.reviews) {
        reviewList.innerHTML = '';
        data.reviews.forEach((r, idx) => {
            const card = document.createElement('div');
            card.className = `review-item-card ${r.isCorrect ? 'correct' : 'incorrect'}`;

            const optLetters = ['A', 'B', 'C', 'D'];
            const userChoiceText = r.selectedOption >= 0 ? `${optLetters[r.selectedOption]}` : 'Unanswered';
            const correctChoiceText = `${optLetters[r.correctOption]}`;

            card.innerHTML = `
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
                    <span style="font-weight: 700; font-size: 0.95rem; color: var(--text-primary);">Question ${idx + 1}</span>
                    <span class="badge ${r.isCorrect ? 'badge-success' : 'badge-danger'}">
                        ${r.isCorrect ? '✓ Correct (+2)' : (r.selectedOption >= 0 ? '✗ Incorrect (0)' : '— Unanswered (0)')}
                    </span>
                </div>
                <div style="font-weight: 600; font-size: 0.95rem; margin-bottom: 1rem; color: var(--text-primary); line-height: 1.5;">
                    ${escapeHtml(r.question)}
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; font-size: 0.85rem; margin-bottom: 1rem;">
                    <div style="background: #ffffff; padding: 0.65rem 0.85rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                        Your Answer: <strong style="color: ${r.isCorrect ? '#16a34a' : '#dc2626'};">${userChoiceText}</strong>
                    </div>
                    <div style="background: #ffffff; padding: 0.65rem 0.85rem; border-radius: var(--radius-sm); border: 1px solid #86efac;">
                        Correct Answer: <strong style="color: #16a34a;">Option ${correctChoiceText}</strong>
                    </div>
                </div>
                <div style="background: #f8fafc; border-left: 3px solid var(--primary); padding: 0.85rem 1rem; font-size: 0.85rem; color: var(--text-secondary); line-height: 1.5; border-radius: 0 var(--radius-sm) var(--radius-sm) 0;">
                    <strong style="color: var(--primary); display: block; margin-bottom: 0.25rem;">Step-by-Step Solution & Concept:</strong>
                    ${escapeHtml(r.explanation || 'Solution calculation verified by examination engine.').replace(/\n/g, '<br/>')}
                </div>
            `;
            reviewList.appendChild(card);
        });
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// -------------------------------------------------------------
// Active Session Resume on Page Refresh
// -------------------------------------------------------------

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/active-session`);
        const data = await res.json();
        if (data && data.hasActiveSession && data.questions) {
            activeAttemptId = data.attemptId;
            currentQuestions = data.questions;
            currentIndex = 0;
            userAnswers = data.savedAnswers || {};
            violationCount = data.violationCount || 0;
            remainingSeconds = data.remainingSeconds;

            const modal = document.getElementById('sessionResumeModal');
            if (modal) {
                document.getElementById('resumeCategory').textContent = data.category + (data.topic ? ` (${data.topic})` : '');
                const mins = Math.floor(remainingSeconds / 60);
                const secs = remainingSeconds % 60;
                document.getElementById('resumeTimeRemaining').textContent = `${mins}m ${secs}s`;
                document.getElementById('resumeViolationCount').textContent = `${violationCount} / 3`;
                modal.classList.add('active');
            }
        }
    } catch (e) {}
});

async function resumeActiveExamInFullscreen() {
    const modal = document.getElementById('sessionResumeModal');
    if (modal) modal.classList.remove('active');

    try {
        if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
            await document.documentElement.requestFullscreen();
        }
    } catch (e) {}

    switchToExamRunnerView(selectedCategory, selectedTopic);
    startTimerCountdown();
    attachSecurityWatchdogs();
    renderQuestionPalette();
    renderCurrentQuestion();
}

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
