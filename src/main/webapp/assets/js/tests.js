/**
 * TPO Buddy — Secure Assessment & Online Examination System
 * Features:
 * - Pre-test instruction & camera verification
 * - Mandatory Fullscreen enforcement prior to test launch
 * - Three-Strike Security Rule (3 permitted violations per attempt)
 * - Centralized violation counter shared across Fullscreen exit and Tab-switch / Focus-loss
 * - Intelligent incident debouncing (prevents double-counting from visibilitychange + blur)
 * - In-app exit & browser navigation lock during active attempts
 * - Confirmation dialog before manual submission
 * - Automatic submission on 3rd security violation
 * - Attempt persistence across page refreshes via server-side session sync
 * - Progressive answer autosave and risk evaluation
 */

let activeAttemptId = null;
let currentQuestions = [];
let currentIndex = 0;
let userAnswers = {};
let markedForReview = new Set();
let timerInterval = null;
let remainingSeconds = 1500; // 25 minutes default
let cameraStream = null;
let cameraVerified = false;
let selectedCategory = 'Technical';
let selectedCount = 25;
let isSubmitting = false;
let violationCount = 0;
let lastViolationIncidentTime = 0;
let watchdogsAttached = false;
let pendingResumeData = null;

// Expose camera teardown globally for auth logout integration
window.stopProctorWebcam = function() {
    if (cameraStream) {
        try {
            cameraStream.getTracks().forEach(track => track.stop());
        } catch (e) {
            console.warn('Error stopping camera tracks:', e);
        }
        cameraStream = null;
        cameraVerified = false;
    }
};

// -------------------------------------------------------------
// Pre-flight Secure Test Instructions & Webcam Verification
// -------------------------------------------------------------

function initiateExamSetup(category, count = 25) {
    selectedCategory = category;
    selectedCount = count;

    const modalTitle = document.getElementById('setupModalTitle');
    if (modalTitle) {
        modalTitle.textContent = `Secure ${category === 'all' ? 'Comprehensive' : category} Assessment Instructions`;
    }
    const catDetail = document.getElementById('setupCategoryDetail');
    if (catDetail) {
        catDetail.textContent = `${category === 'all' ? 'Comprehensive Mock' : category} • ${count} Questions`;
    }

    // Reset consent & preview state
    const consent = document.getElementById('examConsentCheckbox');
    if (consent) consent.checked = false;
    const startBtn = document.getElementById('btnStartExamConfirmed');
    if (startBtn) {
        startBtn.disabled = true;
        startBtn.textContent = 'Start Secure Test →';
    }

    // Reset camera preview display
    const preview = document.getElementById('setupWebcamPreview');
    const placeholder = document.getElementById('setupWebcamPlaceholder');
    const statusDot = document.getElementById('setupStatusDot');
    const statusText = document.getElementById('setupStatusText');

    if (preview) {
        preview.style.display = 'none';
        preview.srcObject = null;
    }
    if (placeholder) {
        placeholder.style.display = 'block';
        placeholder.textContent = '📷 Camera Not Connected';
    }
    if (statusDot) statusDot.style.background = '#94a3b8';
    if (statusText) {
        statusText.textContent = 'Webcam initialization required';
        statusText.style.color = 'var(--text-secondary)';
    }

    // Open instructions modal
    const modal = document.getElementById('examSetupModal');
    if (modal) modal.classList.add('active');
}

function cancelExamSetup() {
    const modal = document.getElementById('examSetupModal');
    if (modal) modal.classList.remove('active');
    window.stopProctorWebcam();
}

async function requestProctorCamera() {
    const btn = document.getElementById('btnRequestCamera');
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Connecting Camera...';
    }

    const statusDot = document.getElementById('setupStatusDot');
    const statusText = document.getElementById('setupStatusText');
    const preview = document.getElementById('setupWebcamPreview');
    const placeholder = document.getElementById('setupWebcamPlaceholder');

    try {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            throw new Error('Webcam API is not supported in this browser.');
        }

        const stream = await navigator.mediaDevices.getUserMedia({
            video: {
                width: { ideal: 640 },
                height: { ideal: 480 },
                facingMode: 'user'
            },
            audio: false
        });

        const videoTracks = stream.getVideoTracks();
        if (!videoTracks || videoTracks.length === 0) {
            throw new Error('No active video track detected on camera stream.');
        }

        cameraStream = stream;
        cameraVerified = true;

        if (preview) {
            preview.srcObject = stream;
            preview.style.display = 'block';
        }
        if (placeholder) placeholder.style.display = 'none';

        if (statusDot) statusDot.style.background = '#16a34a';
        if (statusText) {
            statusText.textContent = `Webcam Verified (${videoTracks[0].label || 'Active'})`;
            statusText.style.color = '#16a34a';
        }

        if (btn) {
            btn.textContent = '✓ Camera Connected';
            btn.classList.remove('btn-secondary');
            btn.classList.add('btn-primary');
        }

        validateStartButton();
    } catch (err) {
        console.error('Camera access error:', err);
        cameraVerified = false;
        if (statusDot) statusDot.style.background = '#dc2626';
        if (statusText) {
            statusText.textContent = 'Camera Access Denied or Device Unavailable';
            statusText.style.color = '#dc2626';
        }
        if (placeholder) {
            placeholder.textContent = '⚠️ Camera permission denied or not detected. Please enable camera access in your browser bar.';
        }
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Retry Camera Setup';
        }
        validateStartButton();
    }
}

function onConsentToggled() {
    validateStartButton();
}

function validateStartButton() {
    const consent = document.getElementById('examConsentCheckbox');
    const startBtn = document.getElementById('btnStartExamConfirmed');
    if (startBtn) {
        startBtn.disabled = !(consent && consent.checked && cameraVerified);
    }
}

// -------------------------------------------------------------
// Launch Secure Exam Session (Fullscreen Check & Three-Strike Initialization)
// -------------------------------------------------------------

async function launchExamSession() {
    const startBtn = document.getElementById('btnStartExamConfirmed');
    if (startBtn) {
        startBtn.disabled = true;
        startBtn.textContent = 'Entering Fullscreen Mode...';
    }

    try {
        // Step 1: Request browser fullscreen mode
        let fullscreenActive = false;
        if (document.documentElement.requestFullscreen) {
            try {
                await document.documentElement.requestFullscreen();
                fullscreenActive = !!document.fullscreenElement;
            } catch (fsErr) {
                console.warn('Fullscreen request rejected or denied:', fsErr);
                fullscreenActive = false;
            }
        }

        // Enforce: Test starts ONLY after fullscreen is successfully activated
        if (!fullscreenActive && !document.fullscreenElement) {
            alert('Fullscreen mode is mandatory to begin this secure examination.\n\nPlease allow fullscreen permissions in your browser and click "Start Secure Test" again.');
            if (startBtn) {
                startBtn.disabled = false;
                startBtn.textContent = 'Start Secure Test →';
            }
            return;
        }

        if (startBtn) startBtn.textContent = 'Authorizing Test Session...';

        // Step 2: Request server to start exam attempt
        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                category: selectedCategory,
                count: selectedCount
            })
        });

        const data = await res.json();
        if (!data.success) {
            throw new Error(data.error || 'Failed to start exam session.');
        }

        activeAttemptId = data.attemptId;
        currentQuestions = data.questions || [];
        currentIndex = 0;
        userAnswers = {};
        markedForReview.clear();
        violationCount = data.violationCount || 0;
        remainingSeconds = data.durationSeconds || (currentQuestions.length * 60);
        isSubmitting = false;

        // Step 3: Activate distraction-free exam mode & hide in-app navigation
        document.body.classList.add('test-in-progress');
        lockNavigationHistory();

        // Close setup modal
        const setupModal = document.getElementById('examSetupModal');
        if (setupModal) setupModal.classList.remove('active');

        // Switch to test runner view
        document.getElementById('testCategoryPicker').style.display = 'none';
        document.getElementById('testResultCard').style.display = 'none';
        document.getElementById('testRunnerContainer').style.display = 'block';

        // Update UI indicators
        updateViolationBadgeUI();
        updateFullscreenBadgeUI(true);

        // Connect active webcam to exam PIP
        const activePip = document.getElementById('activeProctorVideo');
        if (activePip && cameraStream) {
            activePip.srcObject = cameraStream;
        }

        // Build Question Palette, start timer & attach security watchdogs
        buildQuestionPalette();
        attachProctoringWatchdogs();
        startTimer();

        // Render first question
        renderQuestion();
    } catch (err) {
        alert(err.message || 'Unable to launch examination. Please try again.');
        if (startBtn) {
            startBtn.disabled = false;
            startBtn.textContent = 'Start Secure Test →';
        }
    }
}

// -------------------------------------------------------------
// Centralized Security Violation Engine (Three-Strike Rule)
// -------------------------------------------------------------

async function recordSecurityViolation(type, details) {
    if (!activeAttemptId || isSubmitting) return;

    const now = Date.now();
    // Debounce duplicate events (e.g. visibilitychange + window blur within 2.5s)
    if (now - lastViolationIncidentTime < 2500) {
        return;
    }
    lastViolationIncidentTime = now;

    violationCount++;
    updateViolationBadgeUI();

    try {
        // Send violation telemetry to backend
        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/log-event`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                attemptId: activeAttemptId,
                type: type,
                details: `${details} (Violation #${violationCount})`,
                violationCount: violationCount
            })
        });

        const data = await res.json();
        if (data && data.violationCount !== undefined) {
            violationCount = data.violationCount;
            updateViolationBadgeUI();
        }

        // If backend already auto-submitted on 3rd violation
        if (data && data.autoSubmitted) {
            handleAutoSubmissionDueToViolations(data);
            return;
        }
    } catch (e) {
        console.warn('Telemetry log error:', e);
    }

    // Enforce Three-Strike warnings in the UI
    if (violationCount === 1) {
        showSecurityWarningModal(
            'Security Warning 1/3',
            'First Security Violation Recorded',
            'Security Warning 1/3: Please remain in fullscreen and avoid switching tabs.',
            'Exiting fullscreen or switching browser tabs is recorded as an exam integrity violation. You have 2 permitted warnings remaining before your assessment is automatically submitted.',
            2,
            false
        );
    } else if (violationCount === 2) {
        showSecurityWarningModal(
            'Security Warning 2/3 (FINAL WARNING)',
            'Critical Security Alert',
            'Security Warning 2/3: This is your final warning before automatic submission.',
            'FINAL WARNING: Any further fullscreen exit or tab switch will automatically terminate your exam and submit your current score immediately.',
            1,
            false
        );
    } else if (violationCount >= 3) {
        // Third strike: immediate auto-submission
        triggerAutoSubmitForViolations();
    }
}

function showSecurityWarningModal(title, subtitle, message, detail, remaining, isFinal) {
    const modal = document.getElementById('securityWarningModal');
    if (!modal) return;

    const titleEl = document.getElementById('securityModalTitle');
    const subEl = document.getElementById('securityModalSubtitle');
    const msgEl = document.getElementById('securityModalMessage');
    const detailEl = document.getElementById('securityModalDetail');
    const remainingEl = document.getElementById('violationsRemainingCount');
    const btnReturn = document.getElementById('btnReturnFullscreen');

    if (titleEl) titleEl.textContent = title;
    if (subEl) subEl.textContent = subtitle;
    if (msgEl) msgEl.textContent = message;
    if (detailEl) detailEl.textContent = detail;
    if (remainingEl) remainingEl.textContent = remaining;

    if (btnReturn) {
        if (isFinal) {
            btnReturn.disabled = true;
            btnReturn.textContent = 'Submitting Assessment Automatically...';
        } else {
            btnReturn.disabled = false;
            btnReturn.textContent = 'Return to Fullscreen & Continue Test →';
        }
    }

    modal.classList.add('active');
}

async function acknowledgeWarningAndReturnFullscreen() {
    const modal = document.getElementById('securityWarningModal');
    if (modal) modal.classList.remove('active');

    // Attempt to return to fullscreen
    if (document.documentElement.requestFullscreen) {
        try {
            await document.documentElement.requestFullscreen();
            updateFullscreenBadgeUI(true);
        } catch (e) {
            console.warn('Fullscreen re-entry rejected:', e);
            updateFullscreenBadgeUI(false);
        }
    }
}

function triggerAutoSubmitForViolations() {
    if (isSubmitting) return;
    isSubmitting = true;
    clearInterval(timerInterval);

    showSecurityWarningModal(
        'Security Violation Limit Reached (3/3)',
        'Assessment Terminated Automatically',
        'Security violation limit reached. Your test is being submitted automatically.',
        'You have exceeded the 3 permitted security violations for this examination. Your current answers are being saved and evaluated.',
        0,
        true
    );

    setTimeout(() => {
        executeFinalSubmit('auto_submitted_security', 'automatic-security-submission');
    }, 1800);
}

function handleAutoSubmissionDueToViolations(data) {
    if (isSubmitting) return;
    isSubmitting = true;
    clearInterval(timerInterval);

    showSecurityWarningModal(
        'Security Violation Limit Reached (3/3)',
        'Assessment Terminated Automatically',
        'Security violation limit reached. Your test is being submitted automatically.',
        'The maximum permitted 3 violations have been reached. Your assessment has concluded.',
        0,
        true
    );

    setTimeout(() => {
        closeSecurityWarningModal();
        window.stopProctorWebcam();
        document.body.classList.remove('test-in-progress');
        if (document.fullscreenElement && document.exitFullscreen) {
            document.exitFullscreen().catch(() => {});
        }
        displayResults(data);
    }, 1800);
}

function closeSecurityWarningModal() {
    const modal = document.getElementById('securityWarningModal');
    if (modal) modal.classList.remove('active');
}

function updateViolationBadgeUI() {
    const badge = document.getElementById('violationCounterIndicator');
    if (!badge) return;

    badge.textContent = `Security Violations: ${Math.min(3, violationCount)}/3`;
    badge.className = 'violation-badge';
    if (violationCount === 0) {
        badge.classList.add('violation-0');
    } else if (violationCount === 1) {
        badge.classList.add('violation-1');
    } else if (violationCount === 2) {
        badge.classList.add('violation-2');
    } else {
        badge.classList.add('violation-3');
    }
}

function updateFullscreenBadgeUI(isActive) {
    const badge = document.getElementById('fullscreenStatusIndicator');
    if (!badge) return;

    if (isActive) {
        badge.className = 'fullscreen-indicator active';
        badge.innerHTML = '⛶ Fullscreen Active';
    } else {
        badge.className = 'fullscreen-indicator inactive';
        badge.innerHTML = '⚠️ Fullscreen Exited';
    }
}

// -------------------------------------------------------------
// Security Watchdogs (Fullscreen, Tab-Switch, Visibility, Clipboard)
// -------------------------------------------------------------

function attachProctoringWatchdogs() {
    if (watchdogsAttached) return;
    watchdogsAttached = true;

    // 1. Fullscreen Exit Watchdog
    document.addEventListener('fullscreenchange', () => {
        if (!activeAttemptId || isSubmitting) return;

        if (!document.fullscreenElement) {
            updateFullscreenBadgeUI(false);
            recordSecurityViolation('FULLSCREEN_EXIT', 'Candidate exited fullscreen mode.');
            showSecurityAlert('Security Warning: Fullscreen mode exited. Please return to fullscreen immediately.');
        } else {
            updateFullscreenBadgeUI(true);
        }
    });

    // 2. Tab-Switch & Page Visibility Watchdog
    document.addEventListener('visibilitychange', () => {
        if (!activeAttemptId || isSubmitting) return;

        if (document.hidden) {
            recordSecurityViolation('TAB_SWITCH', 'Candidate switched browser tab or minimized window.');
            showSecurityAlert('Security Warning: Tab switch detected. Avoid leaving this assessment tab.');
        }
    });

    // 3. Window Blur Watchdog (debounced together with visibilitychange)
    window.addEventListener('blur', () => {
        if (!activeAttemptId || isSubmitting) return;

        recordSecurityViolation('FOCUS_LOST', 'Window focus lost to external application.');
    });

    // 4. Camera Track Continuity Watchdog
    if (cameraStream) {
        const track = cameraStream.getVideoTracks()[0];
        if (track) {
            track.onended = () => {
                handleCameraInterruption('Webcam stream was ended or device disconnected.');
            };
        }
    }

    const cameraCheckInterval = setInterval(() => {
        if (!activeAttemptId || isSubmitting) {
            clearInterval(cameraCheckInterval);
            return;
        }
        if (!cameraStream) {
            handleCameraInterruption('Webcam feed unavailable.');
            return;
        }
        const track = cameraStream.getVideoTracks()[0];
        if (!track || track.readyState !== 'live') {
            handleCameraInterruption('Webcam video track interrupted.');
        }
    }, 4000);

    // 5. Clipboard & Right-Click Restrictions
    window.addEventListener('copy', (e) => {
        if (!activeAttemptId || isSubmitting) return;
        e.preventDefault();
        showSecurityAlert('Copying test content is strictly prohibited.');
    });

    window.addEventListener('paste', (e) => {
        if (!activeAttemptId || isSubmitting) return;
        e.preventDefault();
        showSecurityAlert('Pasting content into the test is prohibited.');
    });

    window.addEventListener('cut', (e) => {
        if (!activeAttemptId || isSubmitting) return;
        e.preventDefault();
    });

    window.addEventListener('contextmenu', (e) => {
        if (!activeAttemptId || isSubmitting) return;
        e.preventDefault();
    });
}

function handleCameraInterruption(details) {
    const badge = document.getElementById('examProctorBadge');
    if (badge) {
        badge.classList.add('disconnected');
        badge.innerHTML = '<span class="proctor-dot"></span> Camera Disconnected';
    }
    const pipStatus = document.getElementById('proctorPipStatus');
    if (pipStatus) {
        pipStatus.classList.add('disconnected');
        pipStatus.innerHTML = '<span class="proctor-dot"></span> Offline';
    }

    recordSecurityViolation('CAMERA_DISCONNECTED', details);
    showSecurityAlert('Webcam Warning: Video stream interrupted. Please check your camera.');
}

function showSecurityAlert(msg) {
    const banner = document.getElementById('examSecurityAlert');
    const msgEl = document.getElementById('securityAlertMsg');
    if (banner && msgEl) {
        msgEl.textContent = msg;
        banner.style.display = 'flex';
        clearTimeout(banner._dismissTimer);
        banner._dismissTimer = setTimeout(() => {
            banner.style.display = 'none';
        }, 5000);
    }
}

function dismissSecurityAlert() {
    const banner = document.getElementById('examSecurityAlert');
    if (banner) banner.style.display = 'none';
}

// -------------------------------------------------------------
// In-App Navigation Guards & Tab Closure Prevention
// -------------------------------------------------------------

function lockNavigationHistory() {
    // Push dummy history entry to trap browser back button
    window.history.pushState(null, null, window.location.href);

    window.addEventListener('popstate', (e) => {
        if (activeAttemptId && !isSubmitting) {
            window.history.pushState(null, null, window.location.href);
            showSecurityAlert('Navigation is locked during an active examination. Please use "Submit Assessment" to conclude your test.');
        }
    });

    window.addEventListener('beforeunload', (e) => {
        if (activeAttemptId && !isSubmitting) {
            e.preventDefault();
            e.returnValue = 'An examination is active. Navigating away or closing this tab may count as an integrity violation.';
            return e.returnValue;
        }
    });
}

// -------------------------------------------------------------
// Active Session Recovery (Prevents Counter Reset on Page Refresh)
// -------------------------------------------------------------

async function checkForActiveExamSession() {
    try {
        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/active-session`);
        const data = await res.json();

        if (data && data.success && data.hasActiveSession) {
            if (data.status === 'in_progress' && data.remainingSeconds > 0) {
                pendingResumeData = data;
                const catEl = document.getElementById('resumeCategory');
                if (catEl) catEl.textContent = data.category || 'Technical';

                const mins = Math.max(0, Math.floor(data.remainingSeconds / 60));
                const secs = Math.max(0, data.remainingSeconds % 60);
                const timeEl = document.getElementById('resumeTimeRemaining');
                if (timeEl) timeEl.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

                const violEl = document.getElementById('resumeViolationCount');
                if (violEl) violEl.textContent = `${data.violationCount || 0} / 3`;

                const resumeModal = document.getElementById('sessionResumeModal');
                if (resumeModal) resumeModal.classList.add('active');
            } else if (data.status === 'auto_submitted_security') {
                showSecurityAlert('Your previous assessment was terminated and automatically submitted after reaching 3 security violations.');
            }
        }
    } catch (e) {
        console.warn('Error checking active session:', e);
    }
}

async function resumeActiveExamInFullscreen() {
    if (!pendingResumeData) return;

    // Must enter fullscreen to resume
    let fsSuccess = false;
    if (document.documentElement.requestFullscreen) {
        try {
            await document.documentElement.requestFullscreen();
            fsSuccess = !!document.fullscreenElement;
        } catch (e) {
            fsSuccess = false;
        }
    }

    if (!fsSuccess && !document.fullscreenElement) {
        alert('Fullscreen mode is required to resume your assessment.\n\nPlease allow fullscreen permissions in your browser.');
        return;
    }

    const data = pendingResumeData;
    activeAttemptId = data.attemptId;
    selectedCategory = data.category || 'Technical';
    currentQuestions = data.questions || [];
    currentIndex = data.currentQuestionIndex || 0;
    userAnswers = {};

    if (data.savedAnswers) {
        for (const [k, v] of Object.entries(data.savedAnswers)) {
            userAnswers[parseInt(k)] = v;
        }
    }

    // Preserve violation count across refresh (never resets to zero on reload)
    violationCount = data.violationCount || 0;
    remainingSeconds = data.remainingSeconds || 1500;
    isSubmitting = false;

    const resumeModal = document.getElementById('sessionResumeModal');
    if (resumeModal) resumeModal.classList.remove('active');

    document.getElementById('testCategoryPicker').style.display = 'none';
    document.getElementById('testResultCard').style.display = 'none';
    document.getElementById('testRunnerContainer').style.display = 'block';

    document.body.classList.add('test-in-progress');
    lockNavigationHistory();

    updateViolationBadgeUI();
    updateFullscreenBadgeUI(true);

    buildQuestionPalette();
    attachProctoringWatchdogs();
    startTimer();
    renderQuestion();
}

// -------------------------------------------------------------
// Question Palette & Navigation
// -------------------------------------------------------------

function buildQuestionPalette() {
    const grid = document.getElementById('paletteGrid');
    if (!grid) return;
    grid.innerHTML = '';

    currentQuestions.forEach((q, idx) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'palette-btn' + (idx === currentIndex ? ' current' : '');
        btn.id = `palette-btn-${idx}`;
        btn.textContent = (idx + 1);
        btn.onclick = () => goToQuestion(idx);
        grid.appendChild(btn);
    });

    updatePaletteStyles();
}

function updatePaletteStyles() {
    let answeredCount = 0;

    currentQuestions.forEach((q, idx) => {
        const btn = document.getElementById(`palette-btn-${idx}`);
        if (!btn) return;

        const isAnswered = (userAnswers[q.id] !== undefined);
        const isMarked = markedForReview.has(idx);

        if (isAnswered) answeredCount++;

        btn.className = 'palette-btn';
        if (idx === currentIndex) {
            btn.classList.add('current');
        }

        if (isAnswered && isMarked) {
            btn.classList.add('answered-marked');
        } else if (isAnswered) {
            btn.classList.add('answered');
        } else if (isMarked) {
            btn.classList.add('marked');
        }
    });

    const countEl = document.getElementById('paletteAnsweredCount');
    if (countEl) countEl.textContent = answeredCount;
}

function goToQuestion(idx) {
    if (idx >= 0 && idx < currentQuestions.length) {
        currentIndex = idx;
        renderQuestion();
    }
}

function nextQuestion() {
    if (currentIndex < currentQuestions.length - 1) {
        currentIndex++;
        renderQuestion();
    }
}

function prevQuestion() {
    if (currentIndex > 0) {
        currentIndex--;
        renderQuestion();
    }
}

function toggleMarkReview() {
    if (markedForReview.has(currentIndex)) {
        markedForReview.delete(currentIndex);
    } else {
        markedForReview.add(currentIndex);
    }
    renderQuestion();
}

function clearCurrentAnswer() {
    const q = currentQuestions[currentIndex];
    if (q && userAnswers[q.id] !== undefined) {
        delete userAnswers[q.id];
        saveProgressToServer();
        renderQuestion();
    }
}

function renderQuestion() {
    if (!currentQuestions.length) return;
    const q = currentQuestions[currentIndex];

    document.getElementById('qCurrentNum').textContent = (currentIndex + 1);
    document.getElementById('qTotalNum').textContent = currentQuestions.length;
    document.getElementById('qCategoryBadge').textContent = `${q.category} • ${q.difficulty}`;
    document.getElementById('qTopicBadge').textContent = `TOPIC: ${q.topic || 'General'}`;
    document.getElementById('qText').textContent = q.question;

    const reviewBadge = document.getElementById('qReviewBadge');
    if (reviewBadge) {
        reviewBadge.style.display = markedForReview.has(currentIndex) ? 'inline-block' : 'none';
    }

    const btnMark = document.getElementById('btnMarkReview');
    if (btnMark) {
        btnMark.textContent = markedForReview.has(currentIndex) ? '✓ Marked for Review' : '🔖 Mark for Review';
    }

    const optContainer = document.getElementById('optionsContainer');
    optContainer.innerHTML = '';

    const options = [
        { key: 0, letter: 'A', text: q.optionA },
        { key: 1, letter: 'B', text: q.optionB },
        { key: 2, letter: 'C', text: q.optionC },
        { key: 3, letter: 'D', text: q.optionD }
    ];

    options.forEach(opt => {
        const tile = document.createElement('div');
        tile.className = 'option-tile' + (userAnswers[q.id] === opt.key ? ' selected' : '');
        tile.innerHTML = `
            <div class="option-letter">${opt.letter}</div>
            <div style="font-size: 0.95rem; color: var(--text-primary); line-height: 1.4;">${opt.text}</div>
        `;
        tile.onclick = () => selectOption(q.id, opt.key);
        optContainer.appendChild(tile);
    });

    const percent = ((currentIndex + 1) / currentQuestions.length) * 100;
    document.getElementById('testProgressBar').style.width = percent + '%';

    document.getElementById('btnPrev').disabled = (currentIndex === 0);
    document.getElementById('btnNext').disabled = (currentIndex === currentQuestions.length - 1);

    updatePaletteStyles();
}

function selectOption(qId, optKey) {
    if (isSubmitting) return;
    userAnswers[qId] = optKey;
    saveProgressToServer();
    renderQuestion();
}

// -------------------------------------------------------------
// Autosave Engine
// -------------------------------------------------------------

let saveDebounceTimer = null;
function saveProgressToServer() {
    const indicator = document.getElementById('saveStatusIndicator');
    if (indicator) {
        indicator.innerHTML = '⏳ Saving...';
        indicator.style.color = '#b45309';
    }

    clearTimeout(saveDebounceTimer);
    saveDebounceTimer = setTimeout(async () => {
        if (!activeAttemptId || isSubmitting) return;
        try {
            await fetch(`${window.CONTEXT_PATH}/api/tests/save-progress`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    attemptId: activeAttemptId,
                    answers: userAnswers,
                    currentQuestionIndex: currentIndex
                })
            });
            if (indicator) {
                indicator.innerHTML = '● Progress Saved';
                indicator.style.color = '#16a34a';
            }
        } catch (e) {
            if (indicator) {
                indicator.innerHTML = '⚠️ Offline Sync Delay';
                indicator.style.color = '#dc2626';
            }
        }
    }, 500);
}

// -------------------------------------------------------------
// Examination Timer
// -------------------------------------------------------------

function startTimer() {
    clearInterval(timerInterval);
    updateTimerDisplay();
    timerInterval = setInterval(() => {
        remainingSeconds--;
        updateTimerDisplay();
        if (remainingSeconds <= 0) {
            clearInterval(timerInterval);
            alert('Examination time limit has expired! Submitting your answers now.');
            executeFinalSubmit('auto_submitted_timeout', 'timeout');
        }
    }, 1000);
}

function updateTimerDisplay() {
    const mins = Math.max(0, Math.floor(remainingSeconds / 60));
    const secs = Math.max(0, remainingSeconds % 60);
    const formatted = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
    const el = document.getElementById('timerDisplay');
    if (el) el.textContent = formatted;
}

// -------------------------------------------------------------
// Submission & Results Handling
// -------------------------------------------------------------

function showSubmitConfirmModal() {
    let answered = 0;
    currentQuestions.forEach(q => {
        if (userAnswers[q.id] !== undefined) answered++;
    });
    const marked = markedForReview.size;
    const unanswered = currentQuestions.length - answered;

    document.getElementById('modalSummaryAnswered').textContent = answered;
    document.getElementById('modalSummaryMarked').textContent = marked;
    document.getElementById('modalSummaryUnanswered').textContent = unanswered;

    document.getElementById('submitConfirmModal').classList.add('active');
}

function closeSubmitConfirmModal() {
    document.getElementById('submitConfirmModal').classList.remove('active');
}

async function executeFinalSubmit(status = 'submitted', reason = 'manual') {
    if (isSubmitting && reason === 'manual') return;
    isSubmitting = true;

    clearInterval(timerInterval);
    closeSubmitConfirmModal();
    closeSecurityWarningModal();

    const submitBtn = document.getElementById('btnFinalSubmit');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Submitting Assessment...';
    }

    // Teardown camera & distraction-free lock
    window.stopProctorWebcam();
    document.body.classList.remove('test-in-progress');

    // Exit fullscreen if active
    if (document.fullscreenElement && document.exitFullscreen) {
        try { await document.exitFullscreen(); } catch (ignored) {}
    }

    try {
        const payload = {
            attemptId: activeAttemptId,
            category: selectedCategory,
            answers: userAnswers,
            status: status,
            submissionReason: reason
        };

        const res = await fetch(`${window.CONTEXT_PATH}/api/tests/submit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!data.success && !data.alreadySubmitted) {
            throw new Error(data.error || 'Server error during submission');
        }

        displayResults(data);
    } catch (err) {
        console.error('Submission error:', err);
        // Fallback display in case of network issue
        displayResults({
            score: 0,
            correctCount: 0,
            totalQuestions: currentQuestions.length || 25,
            passed: false,
            riskLevel: (violationCount >= 3) ? 'HIGH' : 'MODERATE',
            riskScore: (violationCount >= 3) ? 100 : 50,
            reviews: []
        });
    }
}

function displayResults(data) {
    document.getElementById('testRunnerContainer').style.display = 'none';
    const card = document.getElementById('testResultCard');
    card.style.display = 'block';

    document.getElementById('resultScorePercent').textContent = (data.score !== undefined ? data.score : 0) + '%';
    document.getElementById('resultCorrectFraction').textContent = `${data.correctCount || 0} / ${data.totalQuestions || 25}`;

    const riskLevel = data.riskLevel || 'LOW';
    const riskScore = data.riskScore !== undefined ? data.riskScore : 0;
    const riskEl = document.getElementById('resultRiskDisplay');
    if (riskEl) {
        riskEl.textContent = `${riskLevel} (${riskScore}/100)`;
        if (riskLevel === 'HIGH') {
            riskEl.style.color = '#991b1b';
        } else if (riskLevel === 'MODERATE') {
            riskEl.style.color = '#b45309';
        } else {
            riskEl.style.color = '#166534';
        }
    }

    const badge = document.getElementById('resultStatusBadge');
    if (data.passed) {
        badge.className = 'badge badge-success';
        badge.textContent = 'PASSED (≥ 60%)';
    } else {
        badge.className = 'badge badge-warning';
        badge.textContent = 'NEEDS IMPROVEMENT';
    }

    const reviewList = document.getElementById('resultReviewList');
    if (reviewList) {
        reviewList.innerHTML = '';
        if (data.reviews && data.reviews.length) {
            data.reviews.forEach((r, idx) => {
                const item = document.createElement('div');
                item.style.padding = '1.25rem 0';
                item.style.borderBottom = '1px solid var(--border-subtle)';
                item.innerHTML = `
                    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.5rem;">
                        <span style="font-weight: 600; color: var(--text-primary); font-size: 0.95rem;">Q${idx + 1}: ${r.question}</span>
                        <span class="badge ${r.isCorrect ? 'badge-success' : 'badge-danger'}">${r.isCorrect ? 'Correct (+4)' : 'Incorrect'}</span>
                    </div>
                    <div style="font-size: 0.85rem; color: var(--text-muted); line-height: 1.5; background: #f8fafc; padding: 0.75rem 1rem; border-radius: var(--radius-sm); border-left: 3px solid ${r.isCorrect ? '#16a34a' : '#2563eb'};">
                        <strong>Solution:</strong> ${r.explanation || 'Verified correct answer.'}
                    </div>
                `;
                reviewList.appendChild(item);
            });
        }
    }
}

// -------------------------------------------------------------
// Initialization on Page Load
// -------------------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {
    // Check if user has an existing active session that was interrupted
    checkForActiveExamSession();
});

window.addEventListener('beforeunload', () => {
    window.stopProctorWebcam();
});
