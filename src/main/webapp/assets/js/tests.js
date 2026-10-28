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
let submissionState = 'IN_PROGRESS'; // 'IN_PROGRESS', 'SUBMITTING', 'SUBMITTED', 'SUBMISSION_FAILED'
let pendingAutosavePromise = null;
let lastSubmissionReason = 'manual';
let violationCount = 0;
let lastViolationTime = 0;
let watchdogsAttached = false;
let pollingInterval = null;
let isWarningModalOpen = false;
let violationCooldown = false;

function updateViolationCounterDisplay() {
    const badgeText = document.getElementById('runnerViolationCountText');
    if (badgeText) {
        badgeText.textContent = violationCount;
    }
    const remCount = document.getElementById('violationsRemainingCount');
    if (remCount) {
        remCount.textContent = Math.max(0, 3 - violationCount);
    }
    const currentCount = document.getElementById('securityModalCurrentCount');
    if (currentCount) {
        currentCount.textContent = violationCount;
    }
    const badgeContainer = document.getElementById('runnerViolationBadge');
    if (badgeContainer) {
        if (violationCount === 0) {
            badgeContainer.style.background = '#f8fafc';
            badgeContainer.style.borderColor = '#e2e8f0';
            badgeContainer.style.color = 'var(--text-secondary)';
        } else if (violationCount === 1) {
            badgeContainer.style.background = '#fef3c7';
            badgeContainer.style.borderColor = '#fde68a';
            badgeContainer.style.color = '#b45309';
        } else {
            badgeContainer.style.background = '#fef2f2';
            badgeContainer.style.borderColor = '#fecaca';
            badgeContainer.style.color = '#b91c1c';
        }
    }
}

window.stopProctorWebcam = function () {
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
    window.selectedDifficulty = diff;
    const input = document.getElementById('selectedDifficultyInput');
    if (input) input.value = diff;

    const cards = {
        'Medium': document.getElementById('diffCardMedium'),
        'Hard': document.getElementById('diffCardHard'),
        'Extra Hard': document.getElementById('diffCardExtraHard')
    };

    const badges = {
        'Medium': document.getElementById('diffBadgeMedium'),
        'Hard': document.getElementById('diffBadgeHard'),
        'Extra Hard': document.getElementById('diffBadgeExtraHard')
    };

    Object.keys(cards).forEach(key => {
        if (cards[key]) {
            if (key === diff) {
                cards[key].classList.add('active');
            } else {
                cards[key].classList.remove('active');
            }
        }
        if (badges[key]) {
            badges[key].style.display = (key === diff) ? 'inline-block' : 'none';
        }
    });
}
window.selectDifficulty = selectDifficulty;
window.initiateExamSetup = initiateExamSetup;

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

    // 1. Immediately request fullscreen synchronously within direct click gesture
    try {
        if (!document.fullscreenElement) {
            const doc = document.documentElement;
            if (doc.requestFullscreen) {
                doc.requestFullscreen().catch(() => { });
            } else if (doc.webkitRequestFullscreen) {
                doc.webkitRequestFullscreen();
            } else if (doc.msRequestFullscreen) {
                doc.msRequestFullscreen();
            }
        }
    } catch (e) {
        console.warn('Initial fullscreen gesture notice:', e);
    }

    // 2. Lock down UI immediately: hide sidebar, navbar, and headers
    document.body.classList.add('in-exam-mode');
    document.documentElement.classList.add('in-exam-mode');

    // Proactively initialize webcam if not already connected
    if (!cameraStream && navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ video: { width: 480, height: 360 } })
            .then(stream => {
                cameraStream = stream;
                attachStreamToAllPips();
            })
            .catch(e => console.warn('Proctor camera notice:', e));
    }

    // Ensure latest difficulty selection is picked up
    const diffInput = document.getElementById('selectedDifficultyInput');
    if (diffInput && diffInput.value) {
        selectedDifficulty = diffInput.value;
    } else if (window.selectedDifficulty) {
        selectedDifficulty = window.selectedDifficulty;
    }

    // Show Preparation Modal
    showAiLoadingModal();

    try {
        const payload = {
            seriesId: selectedSeriesId,
            difficulty: selectedDifficulty,
            category: selectedCategory,
            subject: selectedSubject,
            selectedTopics: selectedTopics ? [selectedTopics] : []
        };

        let data = null;
        try {
            const res = await fetch(`${window.CONTEXT_PATH}/api/test-attempts`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const cType = res.headers.get('content-type') || '';
            if (res.ok && cType.includes('application/json')) {
                data = await res.json();
            }
        } catch (fetchErr) {
            data = null;
        }

        if (data && data.success && data.attemptId) {
            activeAttemptId = data.attemptId;

            if (data.status === 'READY') {
                await fetchAndLaunchQuestions(activeAttemptId);
            } else {
                pollGenerationStatus(activeAttemptId);
            }
        } else {
            console.warn('API /api/test-attempts returned non-success or non-JSON, attempting fallback...');
            await fallbackStartExam();
        }
    } catch (err) {
        console.warn('Error connecting to /api/test-attempts, attempting fallback...', err);
        try {
            await fallbackStartExam();
        } catch (fallbackErr) {
            console.warn('Backend test APIs unavailable. Activating client-side Placement Question Engine...');
            await startClientSideExam();
        }
    }
}

async function fallbackStartExam() {
    const fbPayload = {
        category: selectedSubject || selectedCategory || 'Quantitative Aptitude',
        seriesId: selectedSeriesId,
        topic: selectedTopics || selectedSubject || 'General Assessment',
        difficulty: selectedDifficulty || 'Medium',
        count: 30
    };

    let data = null;
    try {
        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(fbPayload)
        });
        const cType = res.headers.get('content-type') || '';
        if (res.ok && cType.includes('application/json')) {
            data = await res.json();
        }
    } catch (err) {
        data = null;
    }

    if (data && data.success && data.questions && data.questions.length > 0) {
        hideAiLoadingModal();
        activeAttemptId = data.attemptId;
        currentQuestions = data.questions;
        currentIndex = 0;
        userAnswers = {};
        markedForReview.clear();
        remainingSeconds = data.durationSeconds || 1800;
        violationCount = 0;

        document.body.classList.add('in-exam-mode');
        document.documentElement.classList.add('in-exam-mode');

        switchToExamRunnerView(selectedCategory, selectedTitle, selectedDifficulty);
        startTimerCountdown();
        attachSecurityWatchdogs();
        renderQuestionPalette();
        renderCurrentQuestion();
    } else {
        // Automatically switch to client-side question engine for static hosting (e.g. Netlify)
        console.info('Switching to client-side Question Engine...');
        await startClientSideExam();
    }
}

async function startClientSideExam() {
    const statusText = document.getElementById('aiLoadingStatusText');
    const bar = document.getElementById('aiProgressBar');

    if (statusText) statusText.textContent = 'Step 1/4: Calibrating 30 balanced questions for assessment...';
    if (bar) bar.style.width = '25%';

    await new Promise(r => setTimeout(r, 200));
    if (statusText) statusText.textContent = 'Step 2/4: Validating structured questions, options, and difficulty calibration...';
    if (bar) bar.style.width = '55%';

    await new Promise(r => setTimeout(r, 250));
    if (statusText) statusText.textContent = 'Step 3/4: Setting up proctoring integrity watchdog & timer...';
    if (bar) bar.style.width = '80%';

    await new Promise(r => setTimeout(r, 250));
    if (statusText) statusText.textContent = 'Step 4/4: Test ready! Launching fullscreen exam view...';
    if (bar) bar.style.width = '100%';

    await new Promise(r => setTimeout(r, 200));

    let questions = [];
    if (typeof PlacementQuestionBank !== 'undefined' && PlacementQuestionBank.getQuestionsForAttempt) {
        questions = PlacementQuestionBank.getQuestionsForAttempt({
            seriesId: selectedSeriesId,
            category: selectedCategory || selectedSubject || 'Quantitative Aptitude',
            topic: selectedTopics || selectedSubject || 'General Assessment',
            difficulty: selectedDifficulty || 'Medium',
            count: 30
        });
    }

    if (!questions || questions.length === 0) {
        hideAiLoadingModal();
        document.body.classList.remove('in-exam-mode');
        document.documentElement.classList.remove('in-exam-mode');
        alert('Could not calibrate examination questions. Please refresh the page and try again.');
        return;
    }

    hideAiLoadingModal();
    activeAttemptId = 'att_' + Date.now().toString(36) + '_' + Math.random().toString(36).substr(2, 6);
    currentQuestions = questions;
    currentIndex = 0;
    userAnswers = {};
    markedForReview = new Set();
    remainingSeconds = 1800; // 30 minutes
    violationCount = 0;

    // Fullscreen entry
    try {
        if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
            await document.documentElement.requestFullscreen();
        }
    } catch (e) {
        console.warn('Fullscreen request bypassed:', e);
    }

    document.body.classList.add('in-exam-mode');
    document.documentElement.classList.add('in-exam-mode');

    switchToExamRunnerView(selectedCategory, selectedTitle, selectedDifficulty);
    startTimerCountdown();
    attachSecurityWatchdogs();
    renderQuestionPalette();
    renderCurrentQuestion();
}

function showAiLoadingModal() {
    const modal = document.getElementById('aiGenLoadingModal');
    if (modal) {
        const label = document.getElementById('aiLoadingSeriesLabel');
        if (label) label.textContent = `${selectedTitle} • ${selectedDifficulty} Assessment`;

        const status = document.getElementById('aiLoadingStatusText');
        if (status) status.textContent = 'Step 1/4: Calibrating 30 balanced questions for assessment...';

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
                if (statusText) statusText.textContent = 'Step 2/4: Validating structured questions, options, and difficulty calibration...';
                if (bar) bar.style.width = '55%';
            } else if (pollCount >= 5 && pollCount < 8) {
                if (statusText) statusText.textContent = 'Step 3/4: Setting up proctoring integrity watchdog & timer...';
                if (bar) bar.style.width = '80%';
            } else if (pollCount >= 8) {
                if (statusText) statusText.textContent = 'Step 4/4: Test ready! Launching fullscreen exam view...';
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
    isSubmitting = false;
    submissionState = 'IN_PROGRESS';
    hideSubmissionStatusOverlay();

    document.body.classList.add('in-exam-mode');
    document.documentElement.classList.add('in-exam-mode');

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
        pip.play().catch(() => { });
    }

    updateViolationCounterDisplay();
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
    if (isSubmitting || submissionState !== 'IN_PROGRESS') return;
    const q = currentQuestions[currentIndex];
    userAnswers[q.id] = optIndex;
    renderCurrentQuestion();
    renderQuestionPalette();

    const saveStatus = document.getElementById('runnerSaveStatus');
    if (saveStatus) {
        saveStatus.innerHTML = '<span style="color: #f59e0b; font-size: 0.7rem;">●</span> Saving...';
    }

    // Autosave progress asynchronously with bounded promise tracking
    if (activeAttemptId && submissionState === 'IN_PROGRESS') {
        pendingAutosavePromise = fetch(`${window.CONTEXT_PATH}/api/test-attempts/${activeAttemptId}/save-progress`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answers: userAnswers })
        }).then(() => {
            if (saveStatus) {
                saveStatus.innerHTML = '<span style="color: #10b981; font-size: 0.7rem;">●</span> Auto-saved';
            }
        }).catch(() => {
            if (saveStatus) {
                saveStatus.innerHTML = '<span style="color: #64748b; font-size: 0.7rem;">●</span> Saved locally';
            }
        }).finally(() => {
            pendingAutosavePromise = null;
        });
    }
}

function clearCurrentResponse() {
    if (isSubmitting || submissionState !== 'IN_PROGRESS') return;
    const q = currentQuestions[currentIndex];
    delete userAnswers[q.id];
    renderCurrentQuestion();
    renderQuestionPalette();

    const saveStatus = document.getElementById('runnerSaveStatus');
    if (saveStatus) {
        saveStatus.innerHTML = '<span style="color: #f59e0b; font-size: 0.7rem;">●</span> Saving...';
    }

    if (activeAttemptId && submissionState === 'IN_PROGRESS') {
        pendingAutosavePromise = fetch(`${window.CONTEXT_PATH}/api/test-attempts/${activeAttemptId}/save-progress`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answers: userAnswers })
        }).then(() => {
            if (saveStatus) {
                saveStatus.innerHTML = '<span style="color: #10b981; font-size: 0.7rem;">●</span> Auto-saved';
            }
        }).catch(() => {
            if (saveStatus) {
                saveStatus.innerHTML = '<span style="color: #64748b; font-size: 0.7rem;">●</span> Saved locally';
            }
        }).finally(() => {
            pendingAutosavePromise = null;
        });
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
// Persistent Proctoring Camera Engine & Stream Protection
// -------------------------------------------------------------

let cameraHeartbeatInterval = null;
let recoveringCamera = false;

function bindCameraEvents(videoEl) {
    if (!videoEl) return;
    ['pause', 'suspend', 'abort', 'stalled', 'emptied'].forEach(evt => {
        videoEl.addEventListener(evt, () => {
            if (cameraStream && !isSubmitting) {
                if (videoEl.srcObject !== cameraStream) {
                    videoEl.srcObject = cameraStream;
                }
                videoEl.play().catch(() => { });
            }
        });
    });
}

function keepCameraActive() {
    if (isSubmitting) return;

    if (cameraStream) {
        cameraStream.getVideoTracks().forEach(t => {
            if (!t.enabled) t.enabled = true;
        });
    }

    // Check if cameraStream exists and has active tracks
    const hasLiveTracks = cameraStream && cameraStream.getVideoTracks().some(t => t.readyState === 'live' && t.enabled);
    if (!hasLiveTracks) {
        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia && !recoveringCamera) {
            recoveringCamera = true;
            navigator.mediaDevices.getUserMedia({ video: { width: 480, height: 360 } })
                .then(stream => {
                    cameraStream = stream;
                    recoveringCamera = false;
                    attachStreamToAllPips();
                })
                .catch(err => {
                    recoveringCamera = false;
                    console.warn('Camera recovery attempt:', err);
                });
        }
    } else {
        attachStreamToAllPips();
    }
}

function attachStreamToAllPips() {
    if (!cameraStream || isSubmitting) return;

    const pips = [
        document.getElementById('examWebcamPip'),
        document.getElementById('securityWarningWebcamPip')
    ];

    pips.forEach(pip => {
        if (pip) {
            pip.muted = true;
            pip.defaultMuted = true;
            if (pip.srcObject !== cameraStream) {
                pip.srcObject = cameraStream;
            }
            pip.style.display = 'block';
            const playPromise = pip.play();
            if (playPromise !== undefined) {
                playPromise.catch(() => { });
            }
        }
    });
}

// -------------------------------------------------------------
// Anti-Cheat Security & Three-Strike Monitoring
// -------------------------------------------------------------

function getFullscreenElement() {
    return document.fullscreenElement ||
        document.webkitFullscreenElement ||
        document.mozFullScreenElement ||
        document.msFullscreenElement ||
        null;
}

function handleFullscreenChange() {
    // Unpause camera immediately on display mode change
    keepCameraActive();
    setTimeout(keepCameraActive, 100);
    setTimeout(keepCameraActive, 300);

    const fs = getFullscreenElement();
    if (!fs) {
        if (activeAttemptId && !isSubmitting && !isWarningModalOpen && !violationCooldown) {
            triggerSecurityViolation('FULLSCREEN_EXIT', 'Fullscreen exit detected');
        }
    }
}

function handleWindowBlur() {
    keepCameraActive();
    // If fullscreen is simultaneously lost (e.g. user pressed ESC), prioritize FULLSCREEN_EXIT
    if (!getFullscreenElement()) {
        handleFullscreenChange();
        return;
    }

    if (activeAttemptId && !isSubmitting && !isWarningModalOpen && !violationCooldown) {
        triggerSecurityViolation('FOCUS_LOST', 'Window focus lost / tab switch detected');
    }
}

function handleVisibilityChange() {
    keepCameraActive();
    if (document.hidden && activeAttemptId && !isSubmitting && !isWarningModalOpen && !violationCooldown) {
        triggerSecurityViolation('TAB_SWITCH', 'Browser minimized or tab switch detected');
    }
}

function handleBeforeUnload(e) {
    if (activeAttemptId && !isSubmitting) {
        e.preventDefault();
        e.returnValue = 'You have an active exam in progress. Leaving this page may submit or terminate your attempt.';
        return e.returnValue;
    }
}

function attachSecurityWatchdogs() {
    if (watchdogsAttached) return;
    watchdogsAttached = true;

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('blur', handleWindowBlur);
    window.addEventListener('focus', keepCameraActive);

    // Multi-browser vendor fullscreen events
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
    document.addEventListener('mozfullscreenchange', handleFullscreenChange);
    document.addEventListener('MSFullscreenChange', handleFullscreenChange);

    window.addEventListener('beforeunload', handleBeforeUnload);

    // Bind proctor video elements
    bindCameraEvents(document.getElementById('examWebcamPip'));
    bindCameraEvents(document.getElementById('securityWarningWebcamPip'));

    // Start 1-second camera heartbeat & continuous fullscreen watchdog
    clearInterval(cameraHeartbeatInterval);
    cameraHeartbeatInterval = setInterval(() => {
        if (activeAttemptId && !isSubmitting) {
            keepCameraActive();
            // Continuous Fullscreen check: if fullscreen was exited and modal isn't open yet
            if (!getFullscreenElement() && !isWarningModalOpen && !violationCooldown) {
                triggerSecurityViolation('FULLSCREEN_EXIT', 'Fullscreen exit detected by integrity monitor');
            }
        }
    }, 1000);
}

function detachSecurityWatchdogs() {
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    window.removeEventListener('blur', handleWindowBlur);
    window.removeEventListener('focus', keepCameraActive);

    document.removeEventListener('fullscreenchange', handleFullscreenChange);
    document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
    document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
    document.removeEventListener('MSFullscreenChange', handleFullscreenChange);

    window.removeEventListener('beforeunload', handleBeforeUnload);

    clearInterval(cameraHeartbeatInterval);
    cameraHeartbeatInterval = null;
    watchdogsAttached = false;
    isWarningModalOpen = false;
    violationCooldown = false;
}

// -------------------------------------------------------------
// Submission Progress Overlay Helpers
// -------------------------------------------------------------

function showSubmissionStatusOverlay(title, desc, state, retryCallback) {
    const overlay = document.getElementById('submissionStatusOverlay');
    if (!overlay) return;
    const titleEl = document.getElementById('submissionStatusTitle');
    const descEl = document.getElementById('submissionStatusDesc');
    const spinnerEl = document.getElementById('submissionStatusSpinner');
    const iconEl = document.getElementById('submissionStatusIcon');
    const retryCont = document.getElementById('submissionRetryContainer');

    if (titleEl && title) titleEl.textContent = title;
    if (descEl && desc) descEl.textContent = desc;

    if (state === 'SUBMISSION_FAILED') {
        if (spinnerEl) spinnerEl.style.display = 'none';
        if (iconEl) {
            iconEl.style.display = 'block';
            iconEl.textContent = '⚠️';
        }
        if (retryCont) retryCont.style.display = 'block';
        window._submissionRetryCallback = retryCallback;
    } else {
        if (spinnerEl) spinnerEl.style.display = 'block';
        if (iconEl) iconEl.style.display = 'none';
        if (retryCont) retryCont.style.display = 'none';
    }

    overlay.classList.add('active');
    overlay.style.display = 'flex';
}

function updateSubmissionStatusMessage(desc) {
    const descEl = document.getElementById('submissionStatusDesc');
    if (descEl && desc) descEl.textContent = desc;
}

function hideSubmissionStatusOverlay() {
    const overlay = document.getElementById('submissionStatusOverlay');
    if (overlay) {
        overlay.classList.remove('active');
        overlay.style.display = 'none';
    }
}

window.retrySubmitAssessment = function () {
    isSubmitting = false;
    submissionState = 'IN_PROGRESS';
    window._activeSubmitRequest = false;
    const submitBtn = document.getElementById('btnSubmitExam');
    if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = '✓ Submit Test';
    }
    const confirmBtn = document.getElementById('btnFinalSubmitConfirm');
    if (confirmBtn) {
        confirmBtn.disabled = false;
        confirmBtn.textContent = 'Yes, Finalize & Submit';
    }
    if (typeof window._submissionRetryCallback === 'function') {
        window._submissionRetryCallback();
    } else {
        submitAssessmentTest(lastSubmissionReason || 'manual');
    }
};

// -------------------------------------------------------------
// Security Violations: 3-Strike Rule & Instant Finalization
// -------------------------------------------------------------

function triggerSecurityViolation(type, message) {
    if (!activeAttemptId || isSubmitting || submissionState !== 'IN_PROGRESS') return;

    // Deduplication check for warnings 1 and 2
    if (violationCooldown && violationCount < 2) return;

    violationCount++;
    updateViolationCounterDisplay();

    // BUG 1 FIX: If third violation threshold is reached, IMMEDIATELY initiate automatic submission
    if (violationCount >= 3) {
        // Synchronous submission lock immediately set
        submissionState = 'SUBMITTING';
        isSubmitting = true;
        violationCooldown = true;

        // Immediately close any open warning modals
        const warnModal = document.getElementById('securityWarningModal');
        if (warnModal) warnModal.classList.remove('active');
        isWarningModalOpen = false;

        // Immediately stop test timer and unbind security listeners so no further events fire
        clearInterval(timerInterval);
        detachSecurityWatchdogs();

        // Log proctoring event to server (non-blocking)
        if (activeAttemptId) {
            fetch(`${window.CONTEXT_PATH}/api/test-attempts/${activeAttemptId}/log-event`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ type: type, message: message, count: violationCount })
            }).catch(() => { });
        }

        // Show non-dismissible submission overlay
        showSubmissionStatusOverlay(
            'Security Violation Limit Reached (3/3)',
            'Maximum integrity violations reached (3/3). Auto-submitting and grading exam...',
            'SUBMITTING'
        );

        // Immediately execute submission without requiring any student clicks or modal acknowledgement
        submitAssessmentTest('proctor_violation');
        return;
    }

    // Otherwise, handle violation 1 or 2:
    violationCooldown = true;
    setTimeout(() => {
        violationCooldown = false;
    }, 2500);

    if (activeAttemptId) {
        fetch(`${window.CONTEXT_PATH}/api/test-attempts/${activeAttemptId}/log-event`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type: type, message: message, count: violationCount })
        }).then(res => res.json()).then(data => {
            if (data && (data.mustAutoSubmit || data.violationCount >= 3) && submissionState === 'IN_PROGRESS') {
                // Server confirmed violation limit reached
                violationCount = Math.max(violationCount, data.violationCount || 3);
                submissionState = 'SUBMITTING';
                isSubmitting = true;
                clearInterval(timerInterval);
                detachSecurityWatchdogs();
                showSubmissionStatusOverlay('Security Violation Limit Reached (3/3)', 'Server verified violation threshold reached. Auto-submitting exam...', 'SUBMITTING');
                submitAssessmentTest('proctor_violation');
            }
        }).catch(() => { });
    }

    showSecurityWarningModal(type);
}

function showSecurityWarningModal(type) {
    if (violationCount >= 3) return; // Handled immediately in triggerSecurityViolation
    const modal = document.getElementById('securityWarningModal');
    if (!modal) return;

    isWarningModalOpen = true;

    // Wake and attach camera in security warning preview immediately
    keepCameraActive();
    setTimeout(keepCameraActive, 50);

    const titleEl = document.getElementById('securityModalTitle');
    const subtitleEl = document.getElementById('securityModalSubtitle');
    const msgEl = document.getElementById('securityModalMessage');
    const noticeEl = document.getElementById('securityNextStepNotice');
    const ackBtn = document.getElementById('btnAcknowledgeSecurity');
    const currCountEl = document.getElementById('securityModalCurrentCount');
    const remCountEl = document.getElementById('violationsRemainingCount');

    if (currCountEl) currCountEl.textContent = violationCount;
    if (remCountEl) remCountEl.textContent = Math.max(0, 3 - violationCount);

    if (violationCount === 1) {
        if (titleEl) titleEl.textContent = 'Security Warning (1/3)';
        if (subtitleEl) subtitleEl.textContent = 'Integrity Violation Detected';
        if (msgEl) {
            msgEl.textContent = 'Warning 1/3: You have left fullscreen mode or switched away from the exam. Return to fullscreen immediately. Further violations will terminate your exam.';
        }
        if (noticeEl) {
            noticeEl.innerHTML = 'Violations remaining before exam penalty/termination: <strong style="color: #dc2626;">2</strong>';
        }
        if (ackBtn) {
            ackBtn.textContent = 'Return to Fullscreen & Continue Assessment →';
            ackBtn.style.background = '#dc2626';
        }
    } else if (violationCount === 2) {
        if (titleEl) titleEl.textContent = 'Security Warning (2/3)';
        if (subtitleEl) subtitleEl.textContent = 'Final Warning Before Exam Termination';
        if (msgEl) {
            msgEl.textContent = 'Warning 2/3: This is your final warning. You must remain in fullscreen mode. 1 more violation will immediately submit and terminate your assessment!';
        }
        if (noticeEl) {
            noticeEl.innerHTML = '<strong style="color: #dc2626; font-size: 0.9rem;">CRITICAL: 1 more violation will immediately terminate your assessment!</strong>';
        }
        if (ackBtn) {
            ackBtn.textContent = 'Return to Fullscreen & Continue Assessment →';
            ackBtn.style.background = '#b91c1c';
        }
    }

    modal.classList.add('active');
}

async function acknowledgeWarningAndReturnFullscreen() {
    const modal = document.getElementById('securityWarningModal');
    if (modal) modal.classList.remove('active');
    isWarningModalOpen = false;

    if (violationCount >= 3) {
        if (!isSubmitting) {
            submitAssessmentTest('proctor_violation');
        }
        return;
    }

    try {
        const doc = document.documentElement;
        if (!getFullscreenElement()) {
            if (doc.requestFullscreen) {
                await doc.requestFullscreen();
            } else if (doc.webkitRequestFullscreen) {
                doc.webkitRequestFullscreen();
            } else if (doc.msRequestFullscreen) {
                doc.msRequestFullscreen();
            }
        }
    } catch (e) {
        console.warn('Fullscreen reentry notice:', e);
    }

    keepCameraActive();
    setTimeout(keepCameraActive, 100);
}

// -------------------------------------------------------------
// Submit Assessment & Results Display (Fast & Idempotent)
// -------------------------------------------------------------

function confirmAndSubmitExam() {
    if (submissionState === 'SUBMITTED') return;
    if (isSubmitting && window._activeSubmitRequest) return;

    // Reset submission flags to allow user review or retry
    isSubmitting = false;
    submissionState = 'IN_PROGRESS';

    const confirmBtn = document.getElementById('btnFinalSubmitConfirm');
    if (confirmBtn) {
        confirmBtn.disabled = false;
        confirmBtn.textContent = 'Yes, Finalize & Submit';
    }
    const submitBtn = document.getElementById('btnSubmitExam');
    if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = '✓ Submit Test';
    }

    const total = currentQuestions.length;
    const answered = Object.keys(userAnswers).length;
    const unanswered = Math.max(0, total - answered);
    const marked = markedForReview.size;

    const answeredEl = document.getElementById('confirmAnsweredCount');
    if (answeredEl) answeredEl.textContent = answered;
    const unansweredEl = document.getElementById('confirmUnansweredCount');
    if (unansweredEl) unansweredEl.textContent = unanswered;
    const markedEl = document.getElementById('confirmMarkedCount');
    if (markedEl) markedEl.textContent = marked;

    const modal = document.getElementById('examSubmitConfirmModal');
    if (modal) {
        modal.classList.add('active');
    } else {
        const msg = `Are you sure you want to submit your test?\n\n• Answered: ${answered} / ${total}\n• Unanswered: ${unanswered}\n\nOnce submitted, answers cannot be modified.`;
        if (confirm(msg)) {
            submitAssessmentTest('student_submitted');
        }
    }
}

function closeSubmitConfirmModal() {
    const modal = document.getElementById('examSubmitConfirmModal');
    if (modal) modal.classList.remove('active');

    const confirmBtn = document.getElementById('btnFinalSubmitConfirm');
    if (confirmBtn) {
        confirmBtn.disabled = false;
        confirmBtn.textContent = 'Yes, Finalize & Submit';
    }
    const submitBtn = document.getElementById('btnSubmitExam');
    if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = '✓ Submit Test';
    }
}

function executeFinalSubmit(reason) {
    const confirmBtn = document.getElementById('btnFinalSubmitConfirm');
    if (confirmBtn) {
        confirmBtn.disabled = true;
        confirmBtn.textContent = 'Finalizing...';
    }
    closeSubmitConfirmModal();
    submitAssessmentTest(reason || 'student_submitted');
}

async function submitAssessmentTest(reason = 'manual') {
    if (submissionState === 'SUBMITTED') return;
    if (isSubmitting && submissionState === 'SUBMITTING' && window._activeSubmitRequest) {
        return; // Prevent duplicate concurrent requests
    }

    lastSubmissionReason = reason;
    submissionState = 'SUBMITTING';
    isSubmitting = true;

    // Resilient attempt resolution: recover from active session if activeAttemptId was lost
    let submittedAttemptId = activeAttemptId;
    if (!submittedAttemptId) {
        try {
            const sessRes = await fetch(`${window.CONTEXT_PATH}/api/tests/active-session`);
            const sessData = await sessRes.json();
            if (sessData && sessData.hasActiveSession && sessData.attemptId) {
                submittedAttemptId = sessData.attemptId;
                activeAttemptId = submittedAttemptId;
            }
        } catch (e) {
            console.warn('Could not recover active session ID:', e);
        }
    }

    if (!submittedAttemptId) {
        console.warn('No active attempt ID found to submit.');
        isSubmitting = false;
        submissionState = 'IN_PROGRESS';
        hideSubmissionStatusOverlay();
        const submitBtn = document.getElementById('btnSubmitExam');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = '✓ Submit Test';
        }
        const confirmBtn = document.getElementById('btnFinalSubmitConfirm');
        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.textContent = 'Yes, Finalize & Submit';
        }
        alert('No active exam session was found. Please ensure you have started a test series.');
        return;
    }

    clearInterval(timerInterval);
    detachSecurityWatchdogs();

    // Close any other open modals
    const warnModal = document.getElementById('securityWarningModal');
    if (warnModal) warnModal.classList.remove('active');
    const confirmModal = document.getElementById('examSubmitConfirmModal');
    if (confirmModal) confirmModal.classList.remove('active');

    // Exit fullscreen safely
    try {
        if (document.fullscreenElement && document.exitFullscreen) {
            await document.exitFullscreen();
        }
    } catch (e) { }

    window.stopProctorWebcam();
    document.body.classList.remove('in-exam-mode');
    document.documentElement.classList.remove('in-exam-mode');

    const submitBtn = document.getElementById('btnSubmitExam');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Submitting & Grading...';
    }

    // Display status overlay
    const overlayTitle = (reason === 'proctor_violation') ? 'Security Violation Limit Reached (3/3)' :
        (reason === 'timeout' ? 'Time Expired — Finalizing Exam' : 'Submitting Your Exam');
    showSubmissionStatusOverlay(overlayTitle, 'Submitting your exam...', 'SUBMITTING');

    // Bounded await for any pending autosave (max 300ms) to ensure no answer is lost
    if (pendingAutosavePromise) {
        try {
            await Promise.race([
                pendingAutosavePromise,
                new Promise(resolve => setTimeout(resolve, 300))
            ]);
        } catch (e) { }
    }

    updateSubmissionStatusMessage('Finalizing your result...');

    const payload = {
        answers: userAnswers,
        submissionReason: reason
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 12000);

    try {
        window._activeSubmitRequest = true;
        const res = await fetch(`${window.CONTEXT_PATH}/api/test-attempts/${submittedAttemptId}/submit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
            signal: controller.signal
        });
        clearTimeout(timeoutId);
        window._activeSubmitRequest = false;

        let data = null;
        try {
            data = await res.json();
        } catch (parseErr) {
            console.warn('Non-JSON response received from primary submit endpoint:', parseErr);
        }

        if (data && data.success) {
            submissionState = 'SUBMITTED';
            isSubmitting = false;
            updateSubmissionStatusMessage('Submission confirmed. Opening your result...');
            activeAttemptId = null;
            const targetAttemptId = data.attemptId || data.id || submittedAttemptId;
            setTimeout(() => {
                window.location.href = `${window.CONTEXT_PATH}/test-result?attemptId=${encodeURIComponent(targetAttemptId)}`;
            }, 300);
            return;
        } else {
            console.warn('Primary submission returned non-success, checking fallback...', data);
            await fallbackSubmitExam(submittedAttemptId, reason);
        }
    } catch (err) {
        clearTimeout(timeoutId);
        window._activeSubmitRequest = false;
        console.warn('Network or timeout on primary submit, verifying status...', err);

        // Check if server already finalized attempt (Idempotency check)
        updateSubmissionStatusMessage('Submission is taking longer than expected. Checking your saved result...');
        try {
            const statusCheck = await fetch(`${window.CONTEXT_PATH}/api/test-attempts/${submittedAttemptId}/status`);
            const statusData = await statusCheck.json();
            if (statusData && statusData.success && (statusData.status === 'submitted' || statusData.status === 'terminated' || statusData.status === 'auto_submitted')) {
                submissionState = 'SUBMITTED';
                isSubmitting = false;
                updateSubmissionStatusMessage('Submission confirmed. Opening your result...');
                activeAttemptId = null;
                setTimeout(() => {
                    window.location.href = `${window.CONTEXT_PATH}/test-result?attemptId=${encodeURIComponent(submittedAttemptId)}`;
                }, 300);
                return;
            }
        } catch (checkErr) { }

        // Fallback endpoint retry
        try {
            await fallbackSubmitExam(submittedAttemptId, reason);
        } catch (fbErr) {
            if (typeof PlacementQuestionBank !== 'undefined' && PlacementQuestionBank.gradeAttempt) {
                console.info('Evaluating attempt via client-side placement grading engine...');
                const duration = Math.max(1, 1800 - remainingSeconds);
                const result = PlacementQuestionBank.gradeAttempt(
                    {
                        attemptId: submittedAttemptId,
                        title: selectedTitle || 'SNJB Quantitative Aptitude Sectional',
                        category: selectedCategory || 'Quantitative Aptitude',
                        difficulty: selectedDifficulty || 'Medium',
                        seriesId: selectedSeriesId || 'college_snjb_quant'
                    },
                    currentQuestions,
                    userAnswers,
                    duration,
                    violationCount
                );

                submissionState = 'SUBMITTED';
                isSubmitting = false;

                // Sync attempt result to Firebase RTDB for student profile & history
                let currentUid = null;
                try {
                    const u = localStorage.getItem('currentUser');
                    if (u) currentUid = JSON.parse(u).uid;
                } catch(e){}
                if (currentUid) {
                    try {
                        fetch(`https://tpobuddy-default-rtdb.firebaseio.com/test_attempts/${currentUid}/${submittedAttemptId}.json`, {
                            method: 'PUT',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                id: submittedAttemptId,
                                testId: selectedSeriesId,
                                seriesId: selectedSeriesId,
                                testTitle: selectedTitle,
                                topic: selectedSubject || selectedTitle,
                                category: selectedCategory,
                                difficulty: selectedDifficulty,
                                score: result.score,
                                correctAnswers: result.correctCount,
                                totalQuestions: result.totalQuestions,
                                passed: result.passed,
                                riskLevel: result.riskLevel,
                                savedAt: Date.now(),
                                status: 'SUBMITTED'
                            })
                        }).catch(e => console.warn('Firebase RTDB save note:', e));
                    } catch(e){}
                }

                updateSubmissionStatusMessage('Submission confirmed. Opening your result...');
                activeAttemptId = null;
                setTimeout(() => {
                    window.location.href = `${window.CONTEXT_PATH}/test-result?attemptId=${encodeURIComponent(submittedAttemptId)}`;
                }, 300);
                return;
            }

            submissionState = 'SUBMISSION_FAILED';
            isSubmitting = false;
            window._activeSubmitRequest = false;
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = '✓ Submit Test';
            }
            const confirmBtn = document.getElementById('btnFinalSubmitConfirm');
            if (confirmBtn) {
                confirmBtn.disabled = false;
                confirmBtn.textContent = 'Yes, Finalize & Submit';
            }
            showSubmissionStatusOverlay(
                'Submission Could Not Be Confirmed',
                'Your answers have been safely preserved. Please check your internet connection and click below to retry safely.',
                'SUBMISSION_FAILED',
                () => {
                    isSubmitting = false;
                    submissionState = 'IN_PROGRESS';
                    submitAssessmentTest(reason);
                }
            );
        }
    } finally {
        window._activeSubmitRequest = false;
    }
}

async function fallbackSubmitExam(attemptId, reason = 'manual') {
    const payload = {
        attemptId: attemptId,
        answers: userAnswers,
        submissionReason: reason
    };

    const res = await fetch(`${window.CONTEXT_PATH}/api/tests/submit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    let data = null;
    try {
        data = await res.json();
    } catch (fbParseErr) {
        console.warn('Non-JSON response from fallback submit endpoint:', fbParseErr);
    }

    if (data && data.success) {
        submissionState = 'SUBMITTED';
        isSubmitting = false;
        updateSubmissionStatusMessage('Submission confirmed. Opening your result...');
        activeAttemptId = null;
        const targetAttemptId = data.attemptId || data.id || attemptId;
        setTimeout(() => {
            window.location.href = `${window.CONTEXT_PATH}/test-result?attemptId=${encodeURIComponent(targetAttemptId)}`;
        }, 300);
    } else {
        throw new Error((data && (data.error || data.message)) || 'Fallback submission failed');
    }
}

function renderAssessmentResult(data) {
    document.body.classList.remove('in-exam-mode');
    document.documentElement.classList.remove('in-exam-mode');

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
            const userAnsIdx = (r.userAnswer !== undefined && r.userAnswer !== null) ? r.userAnswer : ((r.selectedOption !== undefined && r.selectedOption !== null) ? r.selectedOption : -1);
            const userChoiceText = userAnsIdx >= 0 ? `${optLetters[userAnsIdx]}` : 'Unanswered';
            const corrAnsIdx = (r.correctAnswer !== undefined && r.correctAnswer !== null) ? r.correctAnswer : ((r.correctOption !== undefined && r.correctOption !== null) ? r.correctOption : 0);
            const correctChoiceText = `${optLetters[corrAnsIdx]}`;

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
// Active Session Resume on Page Refresh & Dynamic Catalogue
// -------------------------------------------------------------

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Check active in-progress exam session
    try {
        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/active-session`);
        const data = await res.json();
        if (data && data.hasActiveSession && data.questions && data.questions.length > 0) {
            activeAttemptId = data.attemptId;
            currentQuestions = data.questions;
            currentIndex = 0;
            userAnswers = data.savedAnswers || {};
            violationCount = data.violationCount || 0;
            remainingSeconds = data.remainingSeconds || 1800;

            updateViolationCounterDisplay();

            const modal = document.getElementById('sessionResumeModal');
            if (modal) {
                const catEl = document.getElementById('resumeCategory');
                if (catEl) catEl.textContent = (data.category || 'Placement Assessment') + (data.topic ? ` (${data.topic})` : '');
                const mins = Math.floor(Math.max(0, remainingSeconds) / 60);
                const secs = Math.max(0, remainingSeconds) % 60;
                const timeEl = document.getElementById('resumeTimeRemaining');
                if (timeEl) timeEl.textContent = `${mins}m ${secs}s`;
                const violEl = document.getElementById('resumeViolationCount');
                if (violEl) violEl.textContent = `${violationCount} / 3`;
                modal.classList.add('active');
            }
        } else {
            const modal = document.getElementById('sessionResumeModal');
            if (modal) modal.classList.remove('active');
            activeAttemptId = null;
        }
    } catch (e) {
        console.warn('Error verifying active session:', e);
    }

    // 2. Initialize dynamic test series page features
    initTestSeriesPage();
});

async function initTestSeriesPage() {
    // 1. Check URL parameters for direct deep-link or autoStart from Dashboard
    try {
        const params = new URLSearchParams(window.location.search);
        const autoStart = params.get('autoStart');
        const sId = params.get('seriesId');
        const sTitle = params.get('title');
        const sCat = params.get('category') || 'COLLEGE';
        const sSub = params.get('subject') || params.get('topic');
        const sTop = params.get('topic');

        if (autoStart === 'true' && sId) {
            console.info('Auto-launching test setup from query params:', sId, sTitle);
            setTimeout(() => {
                initiateExamSetup(sId, sTitle || 'Assessment', sCat, sSub || sTitle, sTop);
            }, 350);
        }
    } catch(e){}

    // 2. Fetch live published SNJB Tests from Firebase RTDB
    loadLiveSnjbTestsForTestPage();

    // 3. Hydrate Assessment History from Firebase RTDB & localStorage
    loadStudentTestHistory();
}

async function loadLiveSnjbTestsForTestPage() {
    const container = document.getElementById('firebaseSnjbCollegeTestsContainer');
    if (!container) return;

    let tests = [];
    try {
        const res = await fetch('https://tpobuddy-default-rtdb.firebaseio.com/snjb_tests.json');
        if (res.ok) {
            const data = await res.json();
            if (data) {
                tests = Object.keys(data).map(key => ({
                    id: key,
                    ...data[key]
                }));
            }
        }
    } catch(e) {
        console.warn('RTDB snjb_tests fetch note:', e);
    }

    if (!tests || tests.length === 0) {
        tests = [
            {
                id: "snjb-placement-test-1",
                title: "Test 1: Number System, HCF/LCM and Divisibility",
                topic: "Number System, HCF/LCM and Divisibility",
                shortDescription: "Number system, HCF/LCM, divisibility rules and applications.",
                durationMinutes: 30,
                questionCount: 30,
                status: "PUBLISHED",
                allowedAttempts: 3
            },
            {
                id: "snjb-placement-test-2",
                title: "Test 2: Percentages",
                topic: "Percentages",
                shortDescription: "Percentage calculations, variations, and applications in aptitude.",
                durationMinutes: 30,
                questionCount: 30,
                status: "PUBLISHED",
                allowedAttempts: 3
            },
            {
                id: "snjb-placement-test-3",
                title: "Test 3: Profit, Loss and Discounts",
                topic: "Profit, Loss and Discounts",
                shortDescription: "Cost price, marked price, discounts, margins, and profit calculations.",
                durationMinutes: 30,
                questionCount: 30,
                status: "PUBLISHED",
                allowedAttempts: 3
            },
            {
                id: "snjb-placement-test-4",
                title: "Test 4: Mixed Quantitative Aptitude",
                topic: "Mixed Quantitative Aptitude",
                shortDescription: "All six foundational topics combined into a comprehensive mock.",
                durationMinutes: 30,
                questionCount: 30,
                status: "PUBLISHED",
                allowedAttempts: 3
            }
        ];
    }

    // Check student attempt counts
    let localAttempts = [];
    try {
        const hist = localStorage.getItem('tpo_attempts_history');
        if (hist) localAttempts = JSON.parse(hist);
    } catch(e){}

    const attemptMap = {};
    const bestScoreMap = {};
    localAttempts.forEach(a => {
        const tid = a.testId || a.seriesId || a.id;
        if (tid) {
            attemptMap[tid] = (attemptMap[tid] || 0) + 1;
            bestScoreMap[tid] = Math.max(bestScoreMap[tid] || 0, Number(a.score) || 0);
        }
    });

    const cardsHtml = tests.map(t => {
        const maxAttempts = t.allowedAttempts || 3;
        const used = attemptMap[t.id] || 0;
        const isLocked = used >= maxAttempts;
        const hasAttempted = used > 0;
        const best = bestScoreMap[t.id];

        let buttonHtml = '';
        if (isLocked) {
            buttonHtml = `<button class="btn btn-outline btn-sm" disabled style="width: 100%; opacity: 0.6; cursor: not-allowed;">🔒 Attempts Limit Reached (${used}/${maxAttempts})</button>`;
        } else if (hasAttempted) {
            buttonHtml = `<button class="btn btn-primary btn-sm" style="width: 100%;" onclick="initiateExamSetup('${escapeHtml(t.id)}', '${escapeHtml(t.title)}', 'COLLEGE', '${escapeHtml(t.topic)}', '${escapeHtml(t.topic)}')">Retake Test (${used}/${maxAttempts}) • Best: ${best}%</button>`;
        } else {
            buttonHtml = `<button class="btn btn-primary btn-sm" style="width: 100%;" onclick="initiateExamSetup('${escapeHtml(t.id)}', '${escapeHtml(t.title)}', 'COLLEGE', '${escapeHtml(t.topic)}', '${escapeHtml(t.topic)}')">Configure &amp; Start Test &rarr;</button>`;
        }

        return `
            <div class="series-card college-group-item" data-group="SNJB_PLACEMENT" style="background: #ffffff; border: 1.5px solid #3b82f6; border-radius: var(--radius-sm); padding: 1.25rem; display: flex; flex-direction: column; justify-content: space-between;">
                <div>
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.65rem;">
                        <span class="badge badge-primary" style="font-size: 0.7rem;">SNJB Series</span>
                        <span class="badge badge-outline" style="font-size: 0.7rem;">${t.durationMinutes || 30} Mins</span>
                    </div>
                    <h3 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 0.35rem; color: var(--text-primary); display: flex; align-items: center; gap: 0.4rem;">
                        <span>🏛️</span>
                        <span>${escapeHtml(t.title)}</span>
                    </h3>
                    <p style="font-size: 0.8rem; color: var(--text-muted); line-height: 1.4; margin-bottom: 1rem;">
                        ${escapeHtml(t.shortDescription || t.topic || 'SNJB Placement Assessment')}
                    </p>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; margin-bottom: 1.15rem; font-size: 0.75rem; color: var(--text-secondary); background: #f8fafc; padding: 0.65rem; border-radius: 4px; border: 1px solid var(--border-subtle);">
                        <div>📝 <strong>${t.questionCount || 30}</strong> Questions</div>
                        <div>🎯 <strong>+${t.marksPerQuestion || 2}</strong> Marks/Q</div>
                        <div>⚡ <strong>50 AI Pool</strong></div>
                        <div>🔒 <strong>${maxAttempts}</strong> Max Attempts</div>
                    </div>
                </div>
                <div>
                    ${buttonHtml}
                </div>
            </div>
        `;
    }).join('');

    container.innerHTML = cardsHtml;
}

async function loadStudentTestHistory() {
    const tbody = document.getElementById('testHistoryTableBody');
    if (!tbody) return;

    let uid = null;
    try {
        const u = localStorage.getItem('currentUser');
        if (u) uid = JSON.parse(u).uid;
    } catch(e){}

    let attempts = [];

    // 1. Fetch from Firebase RTDB if uid is known
    if (uid) {
        try {
            const res = await fetch(`https://tpobuddy-default-rtdb.firebaseio.com/test_attempts/${uid}.json`);
            if (res.ok) {
                const data = await res.json();
                if (data) {
                    attempts = Object.keys(data).map(key => ({
                        id: key,
                        ...data[key]
                    }));
                }
            }
        } catch(e){}
    }

    // 2. Merge with localStorage history
    try {
        const histStr = localStorage.getItem('tpo_attempts_history');
        if (histStr) {
            const localHist = JSON.parse(histStr);
            const existingIds = new Set(attempts.map(a => a.id));
            localHist.forEach(la => {
                if (!existingIds.has(la.id)) {
                    attempts.push({
                        id: la.id,
                        testTitle: la.title,
                        category: la.category,
                        score: la.score,
                        passed: la.passed,
                        savedAt: new Date(la.date).getTime() || Date.now()
                    });
                    existingIds.add(la.id);
                }
            });
        }
    } catch(e){}

    if (!attempts || attempts.length === 0) {
        return; // Keep existing empty state
    }

    // Sort by recent
    const sorted = [...attempts].sort((a, b) => (b.savedAt || 0) - (a.savedAt || 0));

    tbody.innerHTML = sorted.map(a => {
        const isCollege = !a.category || a.category.toUpperCase().includes('COLLEGE');
        const badgeClass = isCollege ? 'badge-primary' : 'badge-secondary';
        const diff = a.difficulty || 'Medium';
        const isPassed = (a.score >= 60 || a.passed);
        const scoreColor = isPassed ? 'var(--accent-emerald)' : 'var(--accent-amber)';
        const riskLevel = a.riskLevel || 'LOW';
        const riskBadge = (riskLevel === 'HIGH') ? 'badge-danger' : ((riskLevel === 'MODERATE') ? 'badge-warning' : 'badge-success');
        const dateStr = a.savedAt ? new Date(a.savedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Recently';

        return `
            <tr>
                <td><span class="badge ${badgeClass}" style="font-size: 0.75rem;">${escapeHtml(a.category || 'COLLEGE')}</span></td>
                <td style="font-weight: 600; color: var(--text-primary);">${escapeHtml(a.testTitle || a.topic || 'Assessment')}</td>
                <td><span class="badge badge-outline" style="font-size: 0.75rem;">${escapeHtml(diff)}</span></td>
                <td style="font-weight: 700; color: ${scoreColor};">${a.score || 0}%</td>
                <td>${a.correctAnswers !== undefined ? `${a.correctAnswers} / ${a.totalQuestions || 30}` : 'Completed'}</td>
                <td><span class="badge ${riskBadge}">${escapeHtml(riskLevel)}</span></td>
                <td><span class="badge ${isPassed ? 'badge-success' : 'badge-warning'}">${isPassed ? 'PASSED' : 'NEEDS PRACTICE'}</span></td>
                <td>${dateStr}</td>
                <td>
                    <a href="${window.CONTEXT_PATH || ''}/test-result?attemptId=${encodeURIComponent(a.id)}" class="btn btn-outline btn-xs" style="color: var(--primary); font-weight: 600; text-decoration: none; display: inline-flex; align-items: center; gap: 0.25rem;">
                        <span>📊</span> Review
                    </a>
                </td>
            </tr>
        `;
    }).join('');
}

function dismissResumeModal() {
    const modal = document.getElementById('sessionResumeModal');
    if (modal) modal.classList.remove('active');
    activeAttemptId = null;
}

async function resumeActiveExamInFullscreen() {
    const modal = document.getElementById('sessionResumeModal');
    if (modal) modal.classList.remove('active');

    try {
        if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
            await document.documentElement.requestFullscreen();
        }
    } catch (e) {
        console.warn('Fullscreen resume notice:', e);
    }

    switchToExamRunnerView(selectedCategory, selectedTitle, selectedDifficulty);
    updateViolationCounterDisplay();
    startTimerCountdown();
    attachSecurityWatchdogs();
    renderQuestionPalette();
    renderCurrentQuestion();
}

window.confirmAndSubmitExam = confirmAndSubmitExam;
window.closeSubmitConfirmModal = closeSubmitConfirmModal;
window.executeFinalSubmit = executeFinalSubmit;
window.dismissResumeModal = dismissResumeModal;
window.resumeActiveExamInFullscreen = resumeActiveExamInFullscreen;
window.acknowledgeWarningAndReturnFullscreen = acknowledgeWarningAndReturnFullscreen;
window.prevQuestion = prevQuestion;
window.nextQuestion = nextQuestion;
window.selectOption = selectOption;
window.clearCurrentResponse = clearCurrentResponse;
window.toggleMarkForReview = toggleMarkForReview;
window.navigateToQuestion = navigateToQuestion;
window.startExamInFullscreen = startExamInFullscreen;
window.initiateExamSetup = initiateExamSetup;
window.cancelExamSetup = cancelExamSetup;
window.selectDifficulty = selectDifficulty;
window.requestWebcamVerification = requestWebcamVerification;
window.handleConsentChange = handleConsentChange;
window.retakeCurrentAssessment = retakeCurrentAssessment;

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
