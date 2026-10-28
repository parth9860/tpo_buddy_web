/**
 * TPO Buddy — Student Placement Dashboard Client
 * Features:
 * - Real-time synchronization with Firebase Realtime Database & Cloud Firestore
 * - Dynamic student profile & career target hydration
 * - Automated Placement Readiness scoring & SVG circular gauge animation
 * - Competency skill bars (Java, DSA, SQL, Aptitude, Resume ATS)
 * - Live SNJB Test Series cards loaded directly from Firebase /snjb_tests.json
 * - Real student test statistics (Attempts, Average Score %, Passed Tests)
 * - Recent test attempts table with review links and encouraging empty states
 * - Active placement drives loaded from Firebase /jobs.json with instant application
 * - Cross-tab session resilience and automatic refresh on test submission
 */

(function () {
    'use strict';

    // State
    let currentStudentUid = null;
    let studentProfile = null;
    let studentMetrics = null;
    let testAttempts = [];
    let snjbTests = [];
    let jobsList = [];
    let appliedJobIds = new Set();

    // -------------------------------------------------------------
    // Initialization & Auth Watcher
    // -------------------------------------------------------------
    document.addEventListener('DOMContentLoaded', () => {
        initDashboard();
    });

    function initDashboard() {
        // 1. Try local cached profile for instantaneous rendering (0ms delay)
        try {
            const cachedUserStr = localStorage.getItem('currentUser');
            if (cachedUserStr) {
                const cachedUser = JSON.parse(cachedUserStr);
                if (cachedUser && cachedUser.uid) {
                    currentStudentUid = cachedUser.uid;
                    hydrateBasicProfile(cachedUser);
                }
            }
            const cachedApplied = localStorage.getItem('appliedJobs');
            if (cachedApplied) {
                appliedJobIds = new Set(JSON.parse(cachedApplied));
            }
        } catch (e) {
            console.warn('Dashboard cache note:', e);
        }

        // 2. Listen to authoritative Firebase Auth state
        if (typeof firebase !== 'undefined' && firebase.auth) {
            firebase.auth().onAuthStateChanged(async (user) => {
                if (user) {
                    currentStudentUid = user.uid;
                    try {
                        localStorage.setItem('currentUser', JSON.stringify({
                            uid: user.uid,
                            email: user.email,
                            name: user.displayName,
                            photoUrl: user.photoURL
                        }));
                    } catch (e) { }

                    await loadAllDashboardData(user.uid, user);
                } else {
                    // Check if currentUser exists in localStorage (offline/demo fallback)
                    const localUser = localStorage.getItem('currentUser');
                    if (!localUser && !window.location.pathname.includes('login') && !window.location.pathname.includes('register')) {
                        console.info('Unauthenticated user on student dashboard. Redirecting to login...');
                        setTimeout(() => {
                            if (!firebase.auth().currentUser && !localStorage.getItem('currentUser')) {
                                window.location.href = (window.CONTEXT_PATH || '') + '/login?error=unauthorized';
                            }
                        }, 1200);
                    }
                }
            });
        } else {
            // Offline or Firebase Auth not loaded: fallback to local data
            if (currentStudentUid) {
                loadAllDashboardData(currentStudentUid, null);
            }
        }

        // 3. Listen to cross-tab storage updates (e.g. after a test is completed in another tab)
        window.addEventListener('storage', (e) => {
            if (e.key === 'latestTestAttempt' || e.key === 'tpo_attempts_history') {
                console.info('Test submission detected from another tab. Refreshing test stats...');
                if (currentStudentUid) {
                    loadTestAttempts(currentStudentUid);
                }
            }
        });
    }

    // -------------------------------------------------------------
    // Data Loading Orchestrator
    // -------------------------------------------------------------
    async function loadAllDashboardData(uid, authUser) {
        setDashboardLoading(true);

        try {
            await Promise.allSettled([
                loadStudentProfile(uid, authUser),
                loadStudentMetrics(uid),
                loadTestAttempts(uid),
                loadLiveSnjbTests(),
                loadActiveJobs(uid)
            ]);
        } catch (err) {
            console.warn('Error during dashboard data load:', err);
        } finally {
            setDashboardLoading(false);
        }
    }

    function setDashboardLoading(isLoading) {
        const skeletonElements = document.querySelectorAll('.dashboard-skeleton');
        skeletonElements.forEach(el => {
            el.style.display = isLoading ? 'block' : 'none';
        });
    }

    // -------------------------------------------------------------
    // Student Profile & Career Targets
    // -------------------------------------------------------------
    async function loadStudentProfile(uid, authUser) {
        let profile = null;

        // 1. Fetch from Firebase Realtime Database
        try {
            const res = await fetch(`https://tpobuddy-default-rtdb.firebaseio.com/users/${uid}.json`);
            if (res.ok) {
                profile = await res.json();
            }
        } catch (e) {
            console.warn('RTDB users fetch note:', e);
        }

        // 2. Fallback to Cloud Firestore
        if (!profile && typeof firestore !== 'undefined' && firestore) {
            try {
                const doc = await firestore.collection('users').doc(uid).get();
                if (doc.exists) {
                    profile = doc.data();
                }
            } catch (e) {
                console.warn('Firestore users fetch note:', e);
            }
        }

        // 3. Fallback to authUser or default
        if (!profile && authUser) {
            profile = {
                name: authUser.displayName || (authUser.email ? authUser.email.split('@')[0] : 'Student'),
                email: authUser.email,
                branch: 'CSE',
                year: 'Final Year',
                college: "SNJB's Late Sau KBJ College of Engineering"
            };
        }

        studentProfile = profile;
        if (profile) {
            hydrateFullProfile(profile);
        }
    }

    function hydrateBasicProfile(user) {
        const name = user.name || user.displayName || 'Student';
        const nameEl = document.getElementById('studentDisplayName');
        if (nameEl) nameEl.textContent = name;

        const subEl = document.getElementById('studentSubtitle');
        if (subEl) {
            subEl.textContent = `${name} • ${user.branch || 'CSE'} • ${user.year || 'Final Year'} (${user.college || "SNJB's Late Sau KBJ College of Engineering"})`;
        }
    }

    function hydrateFullProfile(profile) {
        const name = profile.name || profile.displayName || 'Student';
        const branch = profile.branch || 'CSE';
        const year = profile.year || 'Final Year';
        const college = profile.college || "SNJB's Late Sau KBJ College of Engineering";

        const nameEl = document.getElementById('studentDisplayName');
        if (nameEl) nameEl.textContent = name;

        const subEl = document.getElementById('studentSubtitle');
        if (subEl) {
            subEl.textContent = `${name} • ${branch} • ${year} (${college})`;
        }

        // Sync Sidebar Profile Snippet
        const sName = document.getElementById('sidebarUserName');
        if (sName) sName.textContent = name;

        const sRole = document.getElementById('sidebarUserRole');
        if (sRole) sRole.textContent = (profile.role || 'STUDENT').toUpperCase();

        const sBranch = document.getElementById('sidebarUserBranch');
        if (sBranch) sBranch.textContent = branch;

        const sAvatar = document.getElementById('sidebarUserAvatar');
        if (sAvatar) {
            if (profile.photoUrl || profile.photoURL) {
                sAvatar.innerHTML = `<img src="${profile.photoUrl || profile.photoURL}" alt="${name}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;" />`;
            } else {
                sAvatar.textContent = name.charAt(0).toUpperCase();
            }
        }
    }

    // -------------------------------------------------------------
    // Student Metrics & Placement Readiness Engine
    // -------------------------------------------------------------
    async function loadStudentMetrics(uid) {
        let metrics = null;

        // Fetch from Firebase RTDB
        try {
            const res = await fetch(`https://tpobuddy-default-rtdb.firebaseio.com/student_metrics/${uid}.json`);
            if (res.ok) {
                metrics = await res.json();
            }
        } catch (e) {
            console.warn('RTDB student_metrics note:', e);
        }

        if (!metrics) {
            // Default baseline metrics for campus readiness
            metrics = {
                javaSkill: 75,
                dsaSkill: 70,
                sqlSkill: 80,
                aptitudeSkill: 78,
                resumeScore: 82,
                desiredRole: "Software Development Engineer",
                targetSalary: "₹6 - 12 LPA",
                preferredLocation: "Pune / Mumbai / Bengaluru"
            };
        }

        studentMetrics = metrics;
        hydrateMetricsAndReadiness(metrics);
    }

    function hydrateMetricsAndReadiness(m) {
        // Career targets
        const roleEl = document.getElementById('targetRole');
        if (roleEl) roleEl.textContent = m.desiredRole || "Software Development Engineer";

        const salEl = document.getElementById('targetPackage');
        if (salEl) salEl.textContent = m.targetSalary || "₹6 - 12 LPA";

        const locEl = document.getElementById('targetLocation');
        if (locEl) locEl.textContent = m.preferredLocation || "Pune / Mumbai";

        // Individual skills
        updateSkillMetric('skillJava', 'skillJavaBar', m.javaSkill || 75);
        updateSkillMetric('skillDsa', 'skillDsaBar', m.dsaSkill || 70);
        updateSkillMetric('skillSql', 'skillSqlBar', m.sqlSkill || 80);
        updateSkillMetric('skillAptitude', 'skillAptitudeBar', m.aptitudeSkill || 78);
        updateSkillMetric('skillResume', 'skillResumeBar', m.resumeScore || 82);

        // Calculate Overall Placement Readiness Score
        // Formula: Java(20%) + DSA(25%) + SQL(15%) + Aptitude(20%) + Resume(20%)
        const java = m.javaSkill || 75;
        const dsa = m.dsaSkill || 70;
        const sql = m.sqlSkill || 80;
        const apt = m.aptitudeSkill || 78;
        const resume = m.resumeScore || 82;

        const overallReadiness = Math.min(100, Math.max(10, Math.round(
            (java * 0.20) + (dsa * 0.25) + (sql * 0.15) + (apt * 0.20) + (resume * 0.20)
        )));

        // Animate circular SVG gauge
        animateReadinessGauge(overallReadiness);

        // Update badge and AI status text
        const badgeEl = document.getElementById('readinessStatusBadge');
        const summaryEl = document.getElementById('readinessSummary');

        let statusText = "Foundation Stage";
        let summaryText = "Complete practice tests in DSA and Quantitative Aptitude to boost your readiness.";

        if (overallReadiness >= 80) {
            statusText = "Placement Ready";
            summaryText = "Outstanding readiness score! You are qualified for Tier-1 campus placement drives and interviews.";
            if (badgeEl) badgeEl.className = "badge badge-success";
        } else if (overallReadiness >= 65) {
            statusText = "Intermediate Stage";
            summaryText = "Solid technical competency. Strengthen core algorithms, SQL optimizations, and timed sectional tests.";
            if (badgeEl) badgeEl.className = "badge badge-primary";
        } else {
            statusText = "Foundation Stage";
            summaryText = "Start practicing with the SNJB Foundational Test Series below to elevate your placement readiness.";
            if (badgeEl) badgeEl.className = "badge badge-warning";
        }

        if (badgeEl) badgeEl.textContent = statusText;
        if (summaryEl) summaryEl.textContent = summaryText;
    }

    function updateSkillMetric(valueId, barId, percent) {
        const valEl = document.getElementById(valueId);
        if (valEl) {
            animateCounter(valEl, percent);
        }
        const barEl = document.getElementById(barId);
        if (barEl) {
            setTimeout(() => {
                barEl.style.width = `${percent}%`;
            }, 100);
        }
    }

    function animateReadinessGauge(score) {
        const circle = document.getElementById('readinessGaugeCircle');
        const scoreEl = document.getElementById('readinessScore');

        const circumference = 339.29; // 2 * PI * 54
        const offset = circumference - ((score / 100) * circumference);

        if (circle) {
            circle.style.transition = 'stroke-dashoffset 1.2s cubic-bezier(0.4, 0, 0.2, 1)';
            circle.style.strokeDasharray = `${circumference}`;
            circle.style.strokeDashoffset = `${offset}`;
        }

        if (scoreEl) {
            animateCounter(scoreEl, score);
        }
    }

    function animateCounter(element, targetValue) {
        const start = parseInt(element.textContent, 10) || 0;
        const duration = 800;
        const startTime = performance.now();

        function step(now) {
            const elapsed = now - startTime;
            const progress = Math.min(elapsed / duration, 1);
            // Ease out cubic
            const ease = 1 - Math.pow(1 - progress, 3);
            const current = Math.round(start + (targetValue - start) * ease);
            element.textContent = current;

            if (progress < 1) {
                requestAnimationFrame(step);
            } else {
                element.textContent = targetValue;
            }
        }

        requestAnimationFrame(step);
    }

    // -------------------------------------------------------------
    // Test Statistics & Assessment History
    // -------------------------------------------------------------
    async function loadTestAttempts(uid) {
        let attempts = [];

        // 1. Fetch from Firebase RTDB
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
        } catch (e) {
            console.warn('RTDB test_attempts fetch note:', e);
        }

        // 2. Merge with localStorage history for instant responsiveness
        try {
            const localHistStr = localStorage.getItem('tpo_attempts_history');
            if (localHistStr) {
                const localHist = JSON.parse(localHistStr);
                const existingIds = new Set(attempts.map(a => a.id));
                localHist.forEach(la => {
                    if (!existingIds.has(la.id)) {
                        attempts.push({
                            id: la.id,
                            testTitle: la.title,
                            category: la.category,
                            score: la.score,
                            passed: la.passed,
                            savedAt: new Date(la.date).getTime() || Date.now(),
                            status: 'SUBMITTED'
                        });
                        existingIds.add(la.id);
                    }
                });
            }
        } catch (e) { }

        testAttempts = attempts;
        hydrateTestStatistics(attempts);
        renderRecentAttemptsTable(attempts);
    }

    function hydrateTestStatistics(attempts) {
        const completedTests = attempts.filter(a => a.status === 'SUBMITTED' || a.score !== undefined);
        const count = completedTests.length;

        let avgScore = 0;
        let passedCount = 0;

        if (count > 0) {
            const totalScore = completedTests.reduce((sum, a) => sum + (Number(a.score) || 0), 0);
            avgScore = Math.round(totalScore / count);
            passedCount = completedTests.filter(a => (a.score >= 60 || a.passed)).length;
        }

        const countEl = document.getElementById('statTestsAttempted');
        if (countEl) animateCounter(countEl, count);

        const avgEl = document.getElementById('statAvgScore');
        if (avgEl) animateCounter(avgEl, avgScore);

        const passedEl = document.getElementById('statTestsPassed');
        if (passedEl) animateCounter(passedEl, passedCount);
    }

    function renderRecentAttemptsTable(attempts) {
        const container = document.getElementById('dashboardRecentTestsContainer');
        if (!container) return;

        if (!attempts || attempts.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 2.5rem 1.5rem; background: #f8fafc; border-radius: var(--radius-sm); border: 1px dashed var(--border-subtle);">
                    <div style="font-size: 2.2rem; margin-bottom: 0.5rem;">📝</div>
                    <h4 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 0.25rem; color: var(--text-primary);">No Test Attempts Yet</h4>
                    <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1.25rem; max-width: 420px; margin-left: auto; margin-right: auto;">
                        Kickstart your campus placement preparation! Take your first assessment from the SNJB Placement Series below.
                    </p>
                    <a href="${window.CONTEXT_PATH || ''}/tests" class="btn btn-primary btn-sm">
                        <span>Explore AI Test Series</span>
                        <span>&rarr;</span>
                    </a>
                </div>
            `;
            return;
        }

        // Sort by most recent
        const sorted = [...attempts].sort((a, b) => (b.savedAt || b.timestamp || 0) - (a.savedAt || a.timestamp || 0)).slice(0, 5);

        let rowsHtml = sorted.map(a => {
            const dateStr = a.savedAt || a.timestamp ? new Date(a.savedAt || a.timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Recently';
            const isPassed = (a.score >= 60 || a.passed);
            const scoreColor = isPassed ? '#10b981' : '#f59e0b';
            const badgeClass = isPassed ? 'badge-success' : 'badge-warning';
            const badgeText = isPassed ? 'PASSED' : 'NEEDS PRACTICE';

            return `
                <tr>
                    <td style="font-weight: 600; color: var(--text-primary);">
                        ${escapeHtml(a.testTitle || a.topic || 'Assessment')}
                    </td>
                    <td>
                        <span class="badge badge-primary" style="font-size: 0.75rem;">
                            ${escapeHtml(a.category || 'COLLEGE')}
                        </span>
                    </td>
                    <td style="font-weight: 700; color: ${scoreColor}; font-size: 0.95rem;">
                        ${a.score || 0}%
                    </td>
                    <td>
                        <span class="badge ${badgeClass}" style="font-size: 0.75rem;">
                            ${badgeText}
                        </span>
                    </td>
                    <td style="font-size: 0.85rem; color: var(--text-muted);">
                        ${dateStr}
                    </td>
                    <td>
                        <a href="${window.CONTEXT_PATH || ''}/test-result?attemptId=${encodeURIComponent(a.id)}" class="btn btn-outline btn-xs" style="color: var(--primary); text-decoration: none; font-weight: 600;">
                            Review &rarr;
                        </a>
                    </td>
                </tr>
            `;
        }).join('');

        container.innerHTML = `
            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Assessment Name</th>
                            <th>Category</th>
                            <th>Score</th>
                            <th>Status</th>
                            <th>Date</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${rowsHtml}
                    </tbody>
                </table>
            </div>
            <div style="padding: 0.75rem 1rem; text-align: right; border-top: 1px solid var(--border-subtle);">
                <a href="${window.CONTEXT_PATH || ''}/tests" style="font-size: 0.85rem; font-weight: 600; color: var(--primary); text-decoration: none;">
                    View All Assessments &amp; Full History &rarr;
                </a>
            </div>
        `;
    }

    // -------------------------------------------------------------
    // Live SNJB Test Series (Direct from Firebase)
    // -------------------------------------------------------------
    async function loadLiveSnjbTests() {
        const container = document.getElementById('dashboardSnjbCards');
        if (!container) return;

        let tests = [];

        // 1. Fetch live tests from Firebase Realtime Database
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
        } catch (e) {
            console.warn('RTDB snjb_tests fetch note:', e);
        }

        // 2. Fallback to Cloud Firestore if RTDB was empty
        if ((!tests || tests.length === 0) && typeof firestore !== 'undefined' && firestore) {
            try {
                const snap = await firestore.collection('snjb_tests').get();
                if (!snap.empty) {
                    tests = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
                }
            } catch (e) {
                console.warn('Firestore snjb_tests fetch note:', e);
            }
        }

        // 3. Fallback to default SNJB Catalogue if network failed
        if (!tests || tests.length === 0) {
            tests = [
                {
                    id: "snjb-placement-test-1",
                    title: "Test 1: Number System, HCF/LCM and Divisibility",
                    topic: "Number System, HCF/LCM and Divisibility",
                    shortDescription: "Essential number theory, unit digits, divisibility rules, and HCF/LCM application.",
                    durationMinutes: 30,
                    questionCount: 30,
                    status: "PUBLISHED",
                    availability: "AVAILABLE",
                    allowedAttempts: 3
                },
                {
                    id: "snjb-placement-test-2",
                    title: "Test 2: Percentages",
                    topic: "Percentages",
                    shortDescription: "Percentage calculations, successive change, marks, populations, and distributions.",
                    durationMinutes: 30,
                    questionCount: 30,
                    status: "PUBLISHED",
                    availability: "AVAILABLE",
                    allowedAttempts: 3
                },
                {
                    id: "snjb-placement-test-3",
                    title: "Test 3: Profit, Loss and Discounts",
                    topic: "Profit, Loss and Discounts",
                    shortDescription: "Cost price, selling price, markups, successive discounts, and dishonest weights.",
                    durationMinutes: 30,
                    questionCount: 30,
                    status: "PUBLISHED",
                    availability: "AVAILABLE",
                    allowedAttempts: 3
                },
                {
                    id: "snjb-placement-test-4",
                    title: "Test 4: Mixed Quantitative Aptitude",
                    topic: "Mixed Quantitative Aptitude",
                    shortDescription: "Comprehensive test combining all foundational quantitative topics for placement prep.",
                    durationMinutes: 30,
                    questionCount: 30,
                    status: "PUBLISHED",
                    availability: "AVAILABLE",
                    allowedAttempts: 3
                }
            ];
        }

        snjbTests = tests;
        renderSnjbTestCards(container, tests);
    }

    function renderSnjbTestCards(container, tests) {
        // Filter only published/active tests
        const published = tests.filter(t => !t.status || t.status.toUpperCase() === 'PUBLISHED' || t.status.toUpperCase() === 'ACTIVE');

        if (published.length === 0) {
            container.innerHTML = `
                <div style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: var(--text-muted);">
                    No SNJB tests published at this time. Please check back soon.
                </div>
            `;
            return;
        }

        // Check student attempt counts for each test
        const attemptCounts = {};
        const bestScores = {};
        testAttempts.forEach(a => {
            const tid = a.testId || a.seriesId || a.id;
            if (tid) {
                attemptCounts[tid] = (attemptCounts[tid] || 0) + 1;
                bestScores[tid] = Math.max(bestScores[tid] || 0, Number(a.score) || 0);
            }
        });

        const cardsHtml = published.map(test => {
            const maxAttempts = test.allowedAttempts || 3;
            const attemptsUsed = attemptCounts[test.id] || 0;
            const bestScore = bestScores[test.id];
            const isLocked = attemptsUsed >= maxAttempts;
            const hasAttempted = attemptsUsed > 0;

            let actionHtml = '';
            let statusBadge = `<span class="badge badge-success" style="font-size: 0.7rem;">Live</span>`;

            if (isLocked) {
                statusBadge = `<span class="badge badge-danger" style="font-size: 0.7rem;">Completed (${attemptsUsed}/${maxAttempts})</span>`;
                actionHtml = `
                    <button class="btn btn-outline btn-sm" disabled style="width: 100%; opacity: 0.6; cursor: not-allowed;" title="Maximum attempts used.">
                        🔒 Attempts Limit Reached
                    </button>
                `;
            } else if (hasAttempted) {
                actionHtml = `
                    <div style="display: flex; gap: 0.5rem; width: 100%;">
                        <button class="btn btn-primary btn-sm" style="flex: 1;" onclick="launchSnjbTest('${escapeHtml(test.id)}', '${escapeHtml(test.title)}', '${escapeHtml(test.topic)}')">
                            Retake (${attemptsUsed}/${maxAttempts})
                        </button>
                        <span style="font-size: 0.75rem; color: var(--text-muted); align-self: center; font-weight: 600;">
                            Best: ${bestScore}%
                        </span>
                    </div>
                `;
            } else {
                actionHtml = `
                    <button class="btn btn-primary btn-sm" style="width: 100%;" onclick="launchSnjbTest('${escapeHtml(test.id)}', '${escapeHtml(test.title)}', '${escapeHtml(test.topic)}')">
                        Start Assessment &rarr;
                    </button>
                `;
            }

            return `
                <div class="glass-card" style="padding: 1.25rem; display: flex; flex-direction: column; justify-content: space-between; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); transition: transform 0.2s ease, box-shadow 0.2s ease;">
                    <div>
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.65rem;">
                            ${statusBadge}
                            <span class="badge badge-outline" style="font-size: 0.7rem;">${test.durationMinutes || 30} Mins</span>
                        </div>
                        <h4 style="font-size: 0.95rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.4rem; line-height: 1.35;">
                            ${escapeHtml(test.title)}
                        </h4>
                        <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 1rem; line-height: 1.4;">
                            ${escapeHtml(test.shortDescription || test.topic || 'Placement Assessment')}
                        </p>
                        <div style="display: flex; gap: 0.5rem; font-size: 0.75rem; color: var(--text-secondary); margin-bottom: 1rem; background: #f8fafc; padding: 0.5rem 0.65rem; border-radius: 4px;">
                            <span>📝 <strong>${test.questionCount || 30}</strong> Questions</span>
                            <span>•</span>
                            <span>🎯 <strong>${test.totalMarks || 60}</strong> Marks</span>
                        </div>
                    </div>
                    <div>
                        ${actionHtml}
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = cardsHtml;
    }

    // Launch SNJB test directly or navigate to tests page
    window.launchSnjbTest = function (testId, title, topic) {
        // Redirect to tests page with auto-configured test
        window.location.href = `${window.CONTEXT_PATH || ''}/tests?seriesId=${encodeURIComponent(testId)}&title=${encodeURIComponent(title)}&topic=${encodeURIComponent(topic)}&autoStart=true`;
    };

    // -------------------------------------------------------------
    // Active Placement Drives (Jobs)
    // -------------------------------------------------------------
    async function loadActiveJobs(uid) {
        const container = document.getElementById('dashboardJobsContainer');
        const countBadge = document.getElementById('activeJobsCount');

        let jobs = [];

        // 1. Fetch from Firebase RTDB
        try {
            const res = await fetch('https://tpobuddy-default-rtdb.firebaseio.com/jobs.json');
            if (res.ok) {
                const data = await res.json();
                if (data) {
                    jobs = Object.keys(data).map(key => ({
                        id: key,
                        ...data[key]
                    }));
                }
            }
        } catch (e) {
            console.warn('RTDB jobs fetch note:', e);
        }

        // 2. Fallback to default active jobs
        if (!jobs || jobs.length === 0) {
            jobs = [
                {
                    id: "job-tcs-ninja-2025",
                    title: "TCS Ninja / Digital Campus Drive",
                    company: "Tata Consultancy Services",
                    location: "Pune / Mumbai",
                    packageInfo: "₹3.6 - 7.0 LPA",
                    eligibility: "B.E / B.Tech (All Branches)"
                },
                {
                    id: "job-infosys-sp-2025",
                    title: "Specialist Programmer (Power Programmer)",
                    company: "Infosys",
                    location: "Bengaluru / Pune",
                    packageInfo: "₹9.5 LPA",
                    eligibility: "CSE, IT, AIDS (Min 65% aggregate)"
                },
                {
                    id: "job-cognizant-gen-c",
                    title: "GenC Elevate Full Stack Developer",
                    company: "Cognizant Technology Solutions",
                    location: "Hyderabad / Pune",
                    packageInfo: "₹4.5 - 6.5 LPA",
                    eligibility: "CSE / IT / E&TC"
                }
            ];
        }

        jobsList = jobs;
        if (countBadge) {
            countBadge.textContent = jobs.length;
        }

        if (container) {
            renderDashboardJobs(container, jobs);
        }
    }

    function renderDashboardJobs(container, jobs) {
        if (!jobs || jobs.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    No placement drives active at this moment.
                </div>
            `;
            return;
        }

        const cardsHtml = jobs.slice(0, 3).map(j => {
            const isApplied = appliedJobIds.has(j.id) || j.isApplied;
            const buttonHtml = isApplied
                ? `<button class="btn btn-secondary btn-sm" disabled style="background:#e0f2fe;color:#0369a1;border-color:#bae6fd;">✓ Applied</button>`
                : `<button class="btn btn-primary btn-sm" onclick="applyDashboardJob('${escapeHtml(j.id)}', this)">Apply</button>`;

            return `
                <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1.1rem; display: flex; justify-content: space-between; align-items: center; gap: 1rem;">
                    <div>
                        <div style="font-weight: 700; color: var(--text-primary); font-size: 0.95rem;">${escapeHtml(j.title)}</div>
                        <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.15rem;">${escapeHtml(j.company)} • ${escapeHtml(j.location)}</div>
                        <div style="display: flex; gap: 0.5rem; margin-top: 0.4rem; flex-wrap: wrap;">
                            <span class="badge badge-primary" style="font-size: 0.75rem;">${escapeHtml(j.packageInfo || 'Best in Industry')}</span>
                            <span class="badge badge-success" style="font-size: 0.75rem;">${escapeHtml(j.eligibility || 'Eligible')}</span>
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

    window.applyDashboardJob = async function (jobId, btn) {
        if (!jobId) return;

        btn.disabled = true;
        btn.textContent = 'Submitting...';

        try {
            // Record locally
            appliedJobIds.add(jobId);
            localStorage.setItem('appliedJobs', JSON.stringify([...appliedJobIds]));

            // Try backend API if available
            try {
                await fetch(`${window.CONTEXT_PATH || ''}/api/jobs/apply`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ jobId })
                });
            } catch (e) { }

            // Sync to Firebase RTDB if current user authenticated
            if (currentStudentUid) {
                try {
                    await fetch(`https://tpobuddy-default-rtdb.firebaseio.com/job_applications/${currentStudentUid}/${jobId}.json`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            jobId,
                            appliedAt: Date.now(),
                            status: 'SUBMITTED'
                        })
                    });
                } catch (e) { }
            }

            btn.className = 'btn btn-secondary btn-sm';
            btn.style.background = '#e0f2fe';
            btn.style.color = '#0369a1';
            btn.style.borderColor = '#bae6fd';
            btn.textContent = '✓ Applied';

            if (typeof showToast === 'function') {
                showToast('Placement application successfully submitted!', 'success');
            }
        } catch (err) {
            btn.disabled = false;
            btn.textContent = 'Apply';
            if (typeof showToast === 'function') {
                showToast('Application submission failed. Please try again.', 'error');
            }
        }
    };

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

})();
