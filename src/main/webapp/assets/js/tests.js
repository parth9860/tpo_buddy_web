/**
 * TPO Buddy — AI-Powered Test Series Engine
 * Features:
 * - Dynamic Gemini AI candidate question generation (50 candidate pool -> 30 selected questions)
 * - Difficulty calibration (Medium, Hard, Extra Hard)
 * - Structured syllabus topic balance & passage / code snippet support
 * - Proctoring: Mandatory Fullscreen enforcement, webcam pip, 3-strike tab switch detection
 * - State resilience & persistent timer across page reloads
 * - Dynamic 30-Question Palette (Answered, Unanswered, Marked for Review)
 * - Server-side authoritative grading & topic-wise performance analytics
 * - Weak topic identification & targeted revision recommendations
 */

let activeAttemptId = null;
let currentQuestions = [];
let currentIndex = 0;
let userAnswers = {};
let markedForReview = new Set();
let timerInterval = null;
let remainingSeconds = 1800; // 30 mins standard
let cameraStream = null;
let selectedSeriesId = 'college_snjb_full_mock';
let selectedTitle = 'SNJB Placement Preparation Assessment';
let selectedCategory = 'COLLEGE';
let selectedSubject = 'Multi-Section Assessment';
let selectedTopics = null;
let selectedDifficulty = 'Medium';
let isSubmitting = false;
let violationCount = 0;
let lastViolationTime = 0;
let watchdogsAttached = false;
let pollingInterval = null;

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
// Pre-flight Instructions, Difficulty Selection & Camera Check
// -------------------------------------------------------------

function initiateExamSetup(seriesOrCat, titleOrCount, categoryOrTestId, subjectOrTopic, defaultTopic = null) {
    if (typeof titleOrCount === 'string') {
        // New API: initiateExamSetup(seriesId, title, category, subject, topics)
        selectedSeriesId = seriesOrCat || 'college_snjb_full_mock';
        selectedTitle = titleOrCount || 'Assessment';
        selectedCategory = categoryOrTestId || 'COLLEGE';
        selectedSubject = subjectOrTopic || 'General';
        selectedTopics = defaultTopic;
    } else {
        // Backward compatibility: initiateExamSetup(category, count, testId, topic)
        selectedCategory = seriesOrCat || 'COLLEGE';
        selectedSeriesId = categoryOrTestId || 'college_snjb_quant';
        selectedTitle = defaultTopic || subjectOrTopic || selectedCategory;
        selectedSubject = selectedCategory;
        selectedTopics = defaultTopic || subjectOrTopic;
    }

    selectedDifficulty = 'Medium';
    selectDifficulty('Medium');

    const modalTitle = document.getElementById('setupModalTitle');
    if (modalTitle) {
        modalTitle.textContent = `${selectedTitle} — Assessment Setup`;
    }
    const catDetail = document.getElementById('setupCategoryDetail');
    if (catDetail) {
        catDetail.textContent = `${selectedCategory} • ${selectedSubject} • 30 Questions • 30 Minutes`;
    }

    const consent = document.getElementById('examConsentCheckbox');
    if (consent) consent.checked = false;

    const startBtn = document.getElementById('btnStartExamConfirmed');
    if (startBtn) {
        startBtn.disabled = true;
        startBtn.textContent = 'Generate & Start AI Test →';
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

function selectDifficulty(diff) {
    selectedDifficulty = diff;
    const cards = {
        'Medium': document.getElementById('diffCardMedium'),
        'Hard': document.getElementById('diffCardHard'),
        'Extra Hard': document.getElementById('diffCardExtraHard')
    };

    Object.keys(cards).forEach(key => {
        if (cards[key]) {
            if (key === diff) {
                cards[key].classList.add('active');
            } else {
                cards[key].classList.remove('active');
            }
        }
    });
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
// Launch Assessment in Fullscreen with AI Question Generation
// -------------------------------------------------------------

async function startExamInFullscreen() {
    const setupModal = document.getElementById('examSetupModal');
    if (setupModal) setupModal.classList.remove('active');

    // Show AI Generation Loading Modal
    showAiLoadingModal();

    try {
        const payload = {
            seriesId: selectedSeriesId,
            difficulty: selectedDifficulty,
            category: selectedCategory,
            subject: selectedSubject,
            selectedTopics: selectedTopics ? [selectedTopics] : []
        };

        const res = await fetch(`${window.CONTEXT_PATH}/api/test-attempts`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (data.success && data.attemptId) {
            activeAttemptId = data.attemptId;

            if (data.status === 'READY') {
                await fetchAndLaunchQuestions(activeAttemptId);
            } else {
                // Poll attempt generation status
                pollGenerationStatus(activeAttemptId);
            }
        } else {
            hideAiLoadingModal();
            alert('Failed to initiate AI test generation: ' + (data.message || data.error || 'Server error'));
        }
    } catch (err) {
        hideAiLoadingModal();
        alert('Network error connecting to test series engine.');
    }
}

function showAiLoadingModal() {
    const modal = document.getElementById('aiGenLoadingModal');
    if (modal) {
        const label = document.getElementById('aiLoadingSeriesLabel');
        if (label) label.textContent = `${selectedTitle} • ${selectedDifficulty} Difficulty`;

        const status = document.getElementById('aiLoadingStatusText');
        if (status) status.textContent = 'Step 1/4: Contacting Gemini AI to generate 50 candidate questions...';

        const bar = document.getElementById('aiProgressBar');
        if (bar) bar.style.width = '25%';

        modal.classList.add('active');
    }
}

function hideAiLoadingModal() {
    const modal = document.getElementById('aiGenLoadingModal');
    if (modal) modal.classList.remove('active');
    clearInterval(pollingInterval);
}

function pollGenerationStatus(attemptId) {
    let pollCount = 0;
    const maxPolls = 60; // 60 * 1.5s = 90s max

    clearInterval(pollingInterval);
    pollingInterval = setInterval(async () => {
        pollCount++;
        try {
            const res = await fetch(`${window.CONTEXT_PATH}/api/test-attempts/${attemptId}/status`);
            const data = await res.json();

            const statusText = document.getElementById('aiLoadingStatusText');
            const bar = document.getElementById('aiProgressBar');

            if (pollCount > 2 && pollCount < 5) {
                if (statusText) statusText.textContent = 'Step 2/4: Validating structured JSON, non-empty options, and correctness...';
                if (bar) bar.style.width = '55%';
            } else if (pollCount >= 5 && pollCount < 8) {
                if (statusText) statusText.textContent = 'Step 3/4: Selecting 30 balanced questions mapped to syllabus topic weights...';
                if (bar) bar.style.width = '80%';
            } else if (pollCount >= 8) {
                if (statusText) statusText.textContent = 'Step 4/4: Isolating secure attempt and encrypting server-side answer keys...';
                if (bar) bar.style.width = '95%';
            }

            if (data.status === 'READY') {
                clearInterval(pollingInterval);
                if (bar) bar.style.width = '100%';
                await fetchAndLaunchQuestions(attemptId);
            } else if (data.status === 'FAILED') {
                clearInterval(pollingInterval);
                hideAiLoadingModal();
                alert('Question generation failed: ' + (data.errorMessage || 'Unknown error. Please retry.'));
            } else if (pollCount >= maxPolls) {
                clearInterval(pollingInterval);
                hideAiLoadingModal();
                alert('Generation request timed out. Please try again or choose standard practice.');
            }
        } catch (e) {
            console.warn('Error polling status:', e);
        }
    }, 1500);
}

async function fetchAndLaunchQuestions(attemptId) {
    try {
        const res = await fetch(`${window.CONTEXT_PATH}/api/test-attempts/${attemptId}/questions`);
        const data = await res.json();

        if (data.success && data.questions && data.questions.length > 0) {
            hideAiLoadingModal();

            // Request fullscreen
            try {
                if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
                    await document.documentElement.requestFullscreen();
                }
            } catch (e) {
                console.warn('Fullscreen request bypassed:', e);
            }

            activeAttemptId = attemptId;
            currentQuestions = data.questions;
            currentIndex = 0;
            userAnswers = {};
            markedForReview = new Set();
            violationCount = 0;
            remainingSeconds = (data.durationMinutes ? data.durationMinutes * 60 : 1800);

            switchToExamRunnerView(selectedCategory, selectedTitle, selectedDifficulty);
            startTimerCountdown();
            attachSecurityWatchdogs();
            renderQuestionPalette();
            renderCurrentQuestion();
        } else {
            hideAiLoadingModal();
            alert('Failed to retrieve test questions from secure session.');
        }
    } catch (e) {
        hideAiLoadingModal();
        alert('Network error retrieving examination questions.');
    }
}

function switchToExamRunnerView(category, title, difficulty) {
    const picker = document.getElementById('testCategoryPicker');
    const runner = document.getElementById('testRunner');
    const resultCard = document.getElementById('testResultCard');

    if (picker) picker.style.display = 'none';
    if (resultCard) resultCard.style.display = 'none';
    if (runner) runner.style.display = 'block';

    const catEl = document.getElementById('runnerCategoryBadge');
    if (catEl) catEl.textContent = category || 'COLLEGE';

    const diffEl = document.getElementById('runnerDifficultyBadge');
    if (diffEl) {
        diffEl.textContent = difficulty || 'Medium';
        diffEl.className = 'badge ' + (difficulty === 'Hard' ? 'badge-warning' : (difficulty === 'Extra Hard' ? 'badge-danger' : 'badge-primary'));
    }

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
        diffEl.textContent = q.difficulty || selectedDifficulty || 'Medium';
        diffEl.className = 'badge ' + (q.difficulty === 'Hard' ? 'badge-warning' : (q.difficulty === 'Extra Hard' ? 'badge-danger' : 'badge-primary'));
    }

    const typeBadge = document.getElementById('runnerQuestionTypeBadge');
    if (typeBadge) {
        if (q.questionType === 'OUTPUT_PREDICTION') {
            typeBadge.textContent = '💻 Code Output';
            typeBadge.className = 'badge badge-warning';
            typeBadge.style.display = 'inline-block';
        } else if (q.questionType === 'READING_COMPREHENSION') {
            typeBadge.textContent = '📖 Comprehension';
            typeBadge.className = 'badge badge-secondary';
            typeBadge.style.display = 'inline-block';
        } else {
            typeBadge.style.display = 'none';
        }
    }

    const marksEl = document.getElementById('runnerMarksBadge');
    if (marksEl) marksEl.textContent = `+${q.marks || 2} Marks`;

    const sectionEl = document.getElementById('runnerSectionIndicator');
    if (sectionEl) {
        sectionEl.textContent = `Section: ${q.section || selectedSubject || selectedCategory}`;
    }

    // Statement
    const statementEl = document.getElementById('runnerQuestionStatement');
    if (statementEl) {
        statementEl.textContent = q.question;
    }

    // Passage or Code Snippet Block
    const passageContainer = document.getElementById('runnerPassageCodeContainer');
    if (passageContainer) {
        if (q.passageOrCode && q.passageOrCode.trim().length > 0) {
            passageContainer.style.display = 'block';
            if (q.questionType === 'READING_COMPREHENSION') {
                passageContainer.innerHTML = `
                    <div class="reading-passage-box">
                        <strong style="display: block; margin-bottom: 0.5rem; color: var(--primary);">📖 Reading Comprehension Passage:</strong>
                        ${escapeHtml(q.passageOrCode).replace(/\\n/g, '<br/>')}
                    </div>
                `;
            } else {
                passageContainer.innerHTML = `
                    <div class="passage-code-box">
                        <span class="passage-code-badge">Code Snippet / Program</span>
                        <pre><code>${escapeHtml(q.passageOrCode)}</code></pre>
                    </div>
                `;
            }
        } else {
            passageContainer.style.display = 'none';
            passageContainer.innerHTML = '';
        }
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
            nextBtn.textContent = 'Review & Submit →';
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
    if (activeAttemptId) {
        fetch(`${window.CONTEXT_PATH}/api/test-attempts/${activeAttemptId}/save-progress`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answers: userAnswers })
        }).catch(() => {});
    }
}

function clearCurrentResponse() {
    const q = currentQuestions[currentIndex];
    delete userAnswers[q.id];
    renderCurrentQuestion();
    renderQuestionPalette();

    if (activeAttemptId) {
        fetch(`${window.CONTEXT_PATH}/api/test-attempts/${activeAttemptId}/save-progress`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answers: userAnswers })
        }).catch(() => {});
    }
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
    } else {
        confirmAndSubmitExam();
    }
}

// -------------------------------------------------------------
// Question Palette (30 Questions)
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
    if (now - lastViolationTime < 2500) return; // Debounce
    lastViolationTime = now;

    violationCount++;

    if (activeAttemptId) {
        fetch(`${window.CONTEXT_PATH}/api/test-attempts/${activeAttemptId}/log-event`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type: type, message: message })
        }).catch(() => {});
    }

    if (violationCount >= 3) {
        submitAssessmentTest('proctor_violation');
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
            answers: userAnswers,
            submissionReason: reason
        };

        const res = await fetch(`${window.CONTEXT_PATH}/api/test-attempts/${activeAttemptId}/submit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (data.success) {
            renderAssessmentResult(data);
        } else {
            alert('Submission grading error: ' + (data.message || data.error || 'Server error'));
        }
    } catch (err) {
        alert('Network error submitting test. Your answers have been preserved.');
    } finally {
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

    // Score & Accuracy
    const scoreEl = document.getElementById('resultScorePercent');
    if (scoreEl) scoreEl.textContent = `${data.score}%`;

    const accEl = document.getElementById('resultAccuracyPercent');
    if (accEl) accEl.textContent = `${Math.round(data.accuracy || 0)}%`;

    // Counts
    const fracEl = document.getElementById('resultCorrectFraction');
    if (fracEl) fracEl.textContent = `${data.correctCount} / ${data.totalQuestions || 30}`;

    // Risk Display
    const riskEl = document.getElementById('resultRiskDisplay');
    if (riskEl) {
        const riskLevel = data.riskLevel || (violationCount >= 2 ? 'MODERATE' : 'LOW');
        riskEl.textContent = `${riskLevel} RISK`;
        riskEl.style.color = (riskLevel === 'HIGH' ? '#dc2626' : (riskLevel === 'MODERATE' ? '#d97706' : '#16a34a'));
    }

    // Section Breakdown Grid
    const sectionContainer = document.getElementById('resultSectionBreakdownContainer');
    const sectionGrid = document.getElementById('resultSectionBreakdownGrid');
    if (sectionContainer && sectionGrid) {
        if (data.sectionBreakdown && Object.keys(data.sectionBreakdown).length > 0) {
            sectionGrid.innerHTML = '';
            sectionContainer.style.display = 'block';

            Object.entries(data.sectionBreakdown).forEach(([secName, secData]) => {
                const total = secData.total || 0;
                const correct = secData.correct || 0;
                const pct = total > 0 ? Math.round((correct / total) * 100) : 0;

                const card = document.createElement('div');
                card.style.background = '#f8fafc';
                card.style.padding = '1rem';
                card.style.borderRadius = 'var(--radius-sm)';
                card.style.border = '1px solid var(--border-subtle)';
                card.innerHTML = `
                    <div style="font-weight: 700; color: var(--text-primary); font-size: 0.9rem; margin-bottom: 0.25rem;">${escapeHtml(secName)}</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.5rem;">${correct} of ${total} Correct (${pct}%)</div>
                    <div style="height: 6px; background: #e2e8f0; border-radius: 9999px; overflow: hidden;">
                        <div style="width: ${pct}%; height: 100%; background: ${pct >= 60 ? '#10b981' : '#f59e0b'};"></div>
                    </div>
                `;
                sectionGrid.appendChild(card);
            });
        } else {
            sectionContainer.style.display = 'none';
        }
    }

    // Weak Topics
    const weakContainer = document.getElementById('resultWeakTopicsContainer');
    const weakList = document.getElementById('resultWeakTopicsList');
    if (weakContainer && weakList) {
        if (data.weakTopics && data.weakTopics.length > 0) {
            weakContainer.style.display = 'block';
            weakList.innerHTML = '';
            data.weakTopics.forEach(topic => {
                const chip = document.createElement('div');
                chip.className = 'weak-topic-chip';
                chip.innerHTML = `<span>⚠️</span><span>${escapeHtml(topic)}</span>`;
                weakList.appendChild(chip);
            });
        } else {
            weakContainer.style.display = 'none';
        }
    }

    // Render Solutions Review
    const reviewList = document.getElementById('resultReviewList');
    if (reviewList && data.reviews) {
        reviewList.innerHTML = '';
        data.reviews.forEach((r, idx) => {
            const card = document.createElement('div');
            card.className = `review-item-card ${r.isCorrect ? 'correct' : 'incorrect'}`;

            const optLetters = ['A', 'B', 'C', 'D'];
            const userChoiceText = r.userAnswer >= 0 ? `${optLetters[r.userAnswer]}` : 'Unanswered';
            const correctChoiceText = `${optLetters[r.correctAnswer]}`;

            let passageHtml = '';
            if (r.passageOrCode && r.passageOrCode.trim().length > 0) {
                passageHtml = `
                    <div class="passage-code-box" style="margin-bottom: 1rem;">
                        <span class="passage-code-badge">Reference Code / Passage</span>
                        <pre><code>${escapeHtml(r.passageOrCode)}</code></pre>
                    </div>
                `;
            }

            card.innerHTML = `
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
                    <div>
                        <span style="font-weight: 700; font-size: 0.95rem; color: var(--text-primary);">Question ${idx + 1}</span>
                        ${r.section ? `<span class="badge badge-outline" style="font-size: 0.75rem; margin-left: 0.5rem;">${escapeHtml(r.section)}</span>` : ''}
                        ${r.topic ? `<span class="badge badge-outline" style="font-size: 0.75rem; margin-left: 0.25rem;">${escapeHtml(r.topic)}</span>` : ''}
                    </div>
                    <span class="badge ${r.isCorrect ? 'badge-success' : 'badge-danger'}">
                        ${r.isCorrect ? '✓ Correct (+2)' : (r.userAnswer >= 0 ? '✗ Incorrect (0)' : '— Unanswered (0)')}
                    </span>
                </div>
                <div style="font-weight: 600; font-size: 0.95rem; margin-bottom: 1rem; color: var(--text-primary); line-height: 1.5;">
                    ${escapeHtml(r.question)}
                </div>
                ${passageHtml}
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
                    ${escapeHtml(r.explanation || 'Verified correct answer by assessment engine.').replace(/\\n/g, '<br/>')}
                </div>
            `;
            reviewList.appendChild(card);
        });
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function retakeCurrentAssessment() {
    initiateExamSetup(selectedSeriesId, selectedTitle, selectedCategory, selectedSubject, selectedTopics);
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

    switchToExamRunnerView(selectedCategory, selectedTitle, selectedDifficulty);
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
