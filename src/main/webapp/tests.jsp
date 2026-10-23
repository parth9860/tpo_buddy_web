<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.TestAttempt" %>
<%@ page import="com.tpobuddy.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%
    User user = (User) request.getAttribute("user");
    List<TestAttempt> attempts = (List<TestAttempt>) request.getAttribute("attempts");
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Tests & Assessments — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <script src="${pageContext.request.contextPath}/assets/js/tests.js"></script>
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="app-shell">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="app-main">
            <!-- Header -->
            <div class="page-header">
                <div>
                    <h1 class="page-title">Placement Assessments & Practice Tests</h1>
                    <p class="page-subtitle">Standardized 25-question campus assessments with server-enforced scoring, webcam proctoring, and risk detection.</p>
                </div>
                <div>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- Test Category Picker -->
            <div id="testCategoryPicker">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; margin-bottom: 2.5rem;">
                    <!-- Technical -->
                    <div class="card-3d glass-card" style="padding: 2rem; display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div style="font-size: 2rem; margin-bottom: 1rem;">💻</div>
                            <h3 style="font-size: 1.25rem; margin-bottom: 0.5rem;">Technical Assessment</h3>
                            <p style="font-size: 0.875rem; color: var(--text-muted); margin-bottom: 1.5rem;">
                                Core topics: Java OOP pillars, DBMS & SQL Queries, Data Structures, Algorithms, and Complexity.
                            </p>
                        </div>
                        <button type="button" class="btn btn-primary" onclick="initiateExamSetup('Technical', 25)">
                            <span>Start 25-Q Test</span>
                            <span>→</span>
                        </button>
                    </div>

                    <!-- Quantitative Aptitude -->
                    <div class="card-3d glass-card" style="padding: 2rem; display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div style="font-size: 2rem; margin-bottom: 1rem;">📐</div>
                            <h3 style="font-size: 1.25rem; margin-bottom: 0.5rem;">Quantitative Aptitude</h3>
                            <p style="font-size: 0.875rem; color: var(--text-muted); margin-bottom: 1.5rem;">
                                Numerical problem solving: Time & Work, Speed & Distance, Profit & Loss, Percentages, and Probability.
                            </p>
                        </div>
                        <button type="button" class="btn btn-secondary" onclick="initiateExamSetup('Aptitude', 25)">
                            <span>Start 25-Q Test</span>
                            <span>→</span>
                        </button>
                    </div>

                    <!-- Logical Reasoning -->
                    <div class="card-3d glass-card" style="padding: 2rem; display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div style="font-size: 2rem; margin-bottom: 1rem;">🧩</div>
                            <h3 style="font-size: 1.25rem; margin-bottom: 0.5rem;">Logical Reasoning</h3>
                            <p style="font-size: 0.875rem; color: var(--text-muted); margin-bottom: 1.5rem;">
                                Analytical reasoning: Blood relations, Number & Letter series, Syllogisms, and Direction sense.
                            </p>
                        </div>
                        <button type="button" class="btn btn-secondary" onclick="initiateExamSetup('Logical', 25)">
                            <span>Start 25-Q Test</span>
                            <span>→</span>
                        </button>
                    </div>

                    <!-- Comprehensive Mock -->
                    <div class="card-3d glass-card" style="padding: 2rem; display: flex; flex-direction: column; justify-content: space-between; border-color: #93c5fd;">
                        <div>
                            <div style="font-size: 2rem; margin-bottom: 1rem;">🏆</div>
                            <div class="badge badge-primary" style="margin-bottom: 0.5rem;">Recommended</div>
                            <h3 style="font-size: 1.25rem; margin-bottom: 0.5rem;">Full Mock Assessment</h3>
                            <p style="font-size: 0.875rem; color: var(--text-muted); margin-bottom: 1.5rem;">
                                Full placement simulator mixing Technical, Aptitude, and Logic into a 25-question timed examination.
                            </p>
                        </div>
                        <button type="button" class="btn btn-primary" onclick="initiateExamSetup('all', 25)">
                            <span>Launch Mock Exam</span>
                            <span>→</span>
                        </button>
                    </div>
                </div>

                <!-- Test History Table -->
                <div class="data-table-card">
                    <div class="data-table-header">
                        <h3 style="font-size: 1.15rem;">Your Assessment History</h3>
                        <span style="font-size: 0.85rem; color: var(--text-muted);">Synced from Firebase test_attempts</span>
                    </div>
                    <div class="table-responsive">
                        <table class="table">
                            <thead>
                                <tr>
                                    <th>Assessment Category</th>
                                    <th>Topic</th>
                                    <th>Score</th>
                                    <th>Correct</th>
                                    <th>Integrity Risk</th>
                                    <th>Status</th>
                                    <th>Date Completed</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (attempts != null && !attempts.isEmpty()) {
                                    for (TestAttempt a : attempts) { 
                                        String rLevel = a.getRiskLevel() != null ? a.getRiskLevel() : "LOW";
                                %>
                                    <tr>
                                        <td style="font-weight: 600; color: var(--text-primary);"><%= a.getCategory() %></td>
                                        <td><%= a.getTopic() %></td>
                                        <td style="font-weight: 700; color: <%= a.isPassed() ? "var(--accent-emerald)" : "var(--accent-amber)" %>;"><%= a.getScore() %>%</td>
                                        <td><%= a.getCorrectAnswers() %> / <%= a.getTotalQuestions() %></td>
                                        <td>
                                            <span class="badge <%= "HIGH".equals(rLevel) ? "badge-risk-high" : ("MODERATE".equals(rLevel) ? "badge-risk-moderate" : "badge-risk-low") %>">
                                                <%= rLevel %> RISK (<%= a.getRiskScore() %>/100)
                                            </span>
                                        </td>
                                        <td>
                                            <span class="badge <%= a.isPassed() ? "badge-success" : "badge-warning" %>">
                                                <%= a.isPassed() ? "PASSED (≥ 60%)" : "NEEDS PRACTICE" %>
                                            </span>
                                        </td>
                                        <td><%= sdf.format(new Date(a.getSavedAt())) %></td>
                                    </tr>
                                <%  }
                                } else { %>
                                    <tr>
                                        <td colspan="7" style="text-align: center; padding: 3rem; color: var(--text-muted);">
                                            No completed test attempts yet. Select any test category above to take your first assessment.
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- Dedicated Secure Test Instructions & Setup Modal -->
            <div id="examSetupModal" class="modal-overlay">
                <div class="modal-card">
                    <div class="modal-header">
                        <div style="display: flex; align-items: center; gap: 0.75rem;">
                            <span style="font-size: 1.5rem;">🛡️</span>
                            <div>
                                <h3 class="modal-title" id="setupModalTitle">Secure Online Examination Instructions</h3>
                                <div style="font-size: 0.8rem; color: var(--text-muted);">Proctoring Verification & Security Policy</div>
                            </div>
                        </div>
                        <button type="button" class="modal-close-btn" onclick="cancelExamSetup()">✕</button>
                    </div>

                    <div class="modal-body">
                        <!-- Exam Parameters Overview -->
                        <div style="background: #eff6ff; border: 1px solid #bfdbfe; border-radius: var(--radius-sm); padding: 1rem 1.25rem; margin-bottom: 1.25rem; display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <div style="font-size: 0.75rem; text-transform: uppercase; color: var(--secondary); font-weight: 700;">Assessment</div>
                                <div id="setupCategoryDetail" style="font-weight: 700; color: var(--text-primary); font-size: 1.05rem;">Technical Assessment • 25 Questions</div>
                            </div>
                            <div style="text-align: right;">
                                <div style="font-size: 0.75rem; text-transform: uppercase; color: var(--secondary); font-weight: 700;">Security Policy</div>
                                <div style="font-weight: 700; color: #dc2626; font-size: 1.05rem;">3 Violations Allowed</div>
                            </div>
                        </div>

                        <!-- Dedicated Security Examination Rules -->
                        <div style="background: #fffbeb; border: 1px solid #fde68a; border-radius: var(--radius-sm); padding: 1.15rem; margin-bottom: 1.25rem;">
                            <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 0.5rem; color: #92400e;">Mandatory Examination Security Guidelines:</h4>
                            <ul style="font-size: 0.85rem; color: #78350f; line-height: 1.6; padding-left: 1.25rem; margin: 0;">
                                <li><strong>Fullscreen Enforcement:</strong> The test must be taken in fullscreen mode. Exiting fullscreen mode will count as a security violation.</li>
                                <li><strong>Tab & Window Switching:</strong> Switching browser tabs, minimizing the browser, or focusing on background windows will count as a security violation.</li>
                                <li><strong>Three-Strike Policy:</strong> The student has <strong>three permitted security violations per test attempt</strong>.</li>
                                <li><strong>Automatic Submission:</strong> On the third violation, the test will be <strong>automatically submitted and ended immediately</strong>.</li>
                                <li><strong>System Preparation:</strong> Close all unnecessary background applications, chat apps, and browser tabs before starting.</li>
                                <li><strong>Irreversible:</strong> The test cannot be resumed after automatic submission.</li>
                            </ul>
                        </div>

                        <!-- Camera Setup & Live Preview -->
                        <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 0.75rem; color: var(--text-primary);">Webcam Setup & Device Verification:</h4>
                        <div style="display: grid; grid-template-columns: 180px 1fr; gap: 1.25rem; align-items: center; background: #f8fafc; border: 1px solid var(--border-subtle); padding: 1.25rem; border-radius: var(--radius-sm); margin-bottom: 1.25rem;">
                            <div style="width: 180px; height: 130px; background: #0f172a; border-radius: var(--radius-sm); overflow: hidden; display: flex; align-items: center; justify-content: center; position: relative;">
                                <video id="setupWebcamPreview" autoplay muted playsinline style="width: 100%; height: 100%; object-fit: cover; transform: scaleX(-1); display: none;"></video>
                                <div id="setupWebcamPlaceholder" style="color: #94a3b8; font-size: 0.8rem; text-align: center; padding: 1rem;">
                                    📷 Camera Not Connected
                                </div>
                            </div>
                            <div>
                                <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
                                    <span id="setupStatusDot" style="width: 10px; height: 10px; border-radius: 50%; background: #94a3b8; display: inline-block;"></span>
                                    <span id="setupStatusText" style="font-weight: 600; font-size: 0.85rem; color: var(--text-secondary);">Webcam initialization required</span>
                                </div>
                                <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.75rem;">
                                    Click below to test your camera. TPO Buddy does not record or transmit raw video; only presence events are logged.
                                </p>
                                <button type="button" id="btnRequestCamera" class="btn btn-secondary btn-sm" onclick="requestProctorCamera()">
                                    📷 Allow & Test Camera
                                </button>
                            </div>
                        </div>

                        <!-- Consent Checkbox -->
                        <label style="display: flex; align-items: flex-start; gap: 0.75rem; cursor: pointer; font-size: 0.85rem; color: var(--text-primary);">
                            <input type="checkbox" id="examConsentCheckbox" onchange="onConsentToggled()" style="margin-top: 0.2rem;">
                            <span>I understand that this test runs exclusively in fullscreen mode with tab-switch monitoring, and that reaching three security violations will immediately auto-submit my exam.</span>
                        </label>
                    </div>

                    <div class="modal-footer">
                        <button type="button" id="btnCancelExamSetup" class="btn btn-secondary btn-sm" onclick="cancelExamSetup()">Cancel</button>
                        <button type="button" id="btnStartExamConfirmed" class="btn btn-primary btn-sm" onclick="launchExamSession()" disabled>
                            Start Secure Test →
                        </button>
                    </div>
                </div>
            </div>

            <!-- Active Test Runner (Distraction-Free Exam Interface) -->
            <div id="testRunnerContainer" class="test-runner" style="display: none; max-width: 1100px;">
                <!-- Exam Top Bar -->
                <div class="test-header-bar">
                    <div style="display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap;">
                        <span id="qCategoryBadge" class="badge badge-primary">Technical</span>
                        <span style="font-size: 0.95rem; color: var(--text-muted);">
                            Question <strong id="qCurrentNum" style="color: var(--text-primary); font-weight: 700;">1</strong> of <span id="qTotalNum">25</span>
                        </span>
                        <!-- Fullscreen Status Indicator -->
                        <span id="fullscreenStatusIndicator" class="fullscreen-indicator active">
                            ⛶ Fullscreen Active
                        </span>
                        <!-- Security Violation Counter (0/3, 1/3, 2/3) -->
                        <span id="violationCounterIndicator" class="violation-badge violation-0">
                            Security Violations: 0/3
                        </span>
                        <!-- Autosave Indicator -->
                        <span id="saveStatusIndicator" style="font-size: 0.8rem; color: #16a34a; font-weight: 500; display: inline-flex; align-items: center; gap: 0.35rem;">
                            ● Progress Saved
                        </span>
                    </div>

                    <div style="display: flex; align-items: center; gap: 0.75rem;">
                        <span id="examProctorBadge" class="proctor-status-indicator">
                            <span class="proctor-dot"></span> Camera Active
                        </span>
                        <div class="timer-pill">
                            ⏱ <span id="timerDisplay">25:00</span>
                        </div>
                    </div>
                </div>

                <!-- Exam Integrity Alert Banner (Revealed on warnings) -->
                <div id="examSecurityAlert" class="exam-alert-banner" style="display: none;">
                    <span style="font-size: 1.25rem;">⚠️</span>
                    <div style="flex: 1;">
                        <strong id="securityAlertTitle">Security Warning:</strong>
                        <span id="securityAlertMsg">Please remain in fullscreen and avoid switching tabs.</span>
                    </div>
                    <button type="button" onclick="dismissSecurityAlert()" style="background: none; border: none; color: #991b1b; font-weight: 700; cursor: pointer;">✕</button>
                </div>

                <!-- Progress Bar -->
                <div class="metric-bar-bg" style="margin-bottom: 1.5rem; height: 8px;">
                    <div id="testProgressBar" class="metric-bar-fill" style="width: 4%;"></div>
                </div>

                <!-- 2-Column Exam Layout Grid -->
                <div class="exam-layout-grid">
                    <!-- Left: Question & Answers -->
                    <div>
                        <div class="question-box">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                                <span id="qTopicBadge" style="font-size: 0.8rem; color: var(--text-muted); font-weight: 600;">TOPIC: Core Java</span>
                                <span id="qReviewBadge" class="badge badge-warning" style="display: none;">Marked for Review</span>
                            </div>

                            <div class="question-text" id="qText">Loading question...</div>
                            <div id="optionsContainer"></div>

                            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem; border-top: 1px solid var(--border-subtle); padding-top: 1.5rem; flex-wrap: wrap; gap: 0.75rem;">
                                <div style="display: flex; gap: 0.5rem;">
                                    <button type="button" id="btnPrev" class="btn btn-secondary btn-sm" onclick="prevQuestion()" disabled>← Previous</button>
                                    <button type="button" id="btnClearAnswer" class="btn btn-outline btn-sm" onclick="clearCurrentAnswer()">Clear Choice</button>
                                </div>

                                <div style="display: flex; gap: 0.5rem;">
                                    <button type="button" id="btnMarkReview" class="btn btn-outline btn-sm" onclick="toggleMarkReview()" style="color: #b45309; border-color: #fcd34d;">
                                        🔖 Mark for Review
                                    </button>
                                    <button type="button" id="btnNext" class="btn btn-secondary btn-sm" onclick="nextQuestion()">Next →</button>
                                    <button type="button" id="btnSubmitTestTrigger" class="btn btn-primary btn-sm" onclick="showSubmitConfirmModal()">
                                        Submit Assessment
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Right: Proctoring PIP & Question Palette -->
                    <div>
                        <!-- Proctoring PIP Feed -->
                        <div class="proctor-pip-card">
                            <div class="proctor-pip-header">
                                <span>Candidate Webcam</span>
                                <span id="proctorPipStatus" class="proctor-status-indicator">
                                    <span class="proctor-dot"></span> Live
                                </span>
                            </div>
                            <div class="proctor-video-wrap">
                                <video id="activeProctorVideo" class="proctor-video-feed" autoplay muted playsinline></video>
                            </div>
                        </div>

                        <!-- Question Palette -->
                        <div class="palette-card">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <h4 style="font-size: 0.95rem; font-weight: 700; margin: 0;">Question Palette</h4>
                                <span style="font-size: 0.8rem; color: var(--text-muted);"><span id="paletteAnsweredCount">0</span>/25 Answered</span>
                            </div>

                            <div id="paletteGrid" class="palette-grid">
                                <!-- Generated by JS for 25 questions -->
                            </div>

                            <div class="palette-legend">
                                <div class="legend-item">
                                    <span class="legend-swatch" style="background: #2563eb;"></span>
                                    <span>Answered</span>
                                </div>
                                <div class="legend-item">
                                    <span class="legend-swatch" style="background: #fef3c7; border-color: #fcd34d;"></span>
                                    <span>Marked Review</span>
                                </div>
                                <div class="legend-item">
                                    <span class="legend-swatch" style="background: #7c3aed;"></span>
                                    <span>Ans & Marked</span>
                                </div>
                                <div class="legend-item">
                                    <span class="legend-swatch" style="background: #f8fafc;"></span>
                                    <span>Not Visited</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Submit Confirmation Modal -->
            <div id="submitConfirmModal" class="modal-overlay">
                <div class="modal-card" style="max-width: 480px;">
                    <div class="modal-header">
                        <h3 class="modal-title">Confirm Assessment Submission</h3>
                        <button type="button" class="modal-close-btn" onclick="closeSubmitConfirmModal()">✕</button>
                    </div>
                    <div class="modal-body">
                        <div style="font-size: 1rem; font-weight: 600; color: var(--text-primary); margin-bottom: 0.75rem;">
                            Are you sure you want to submit your test? You cannot change your answers after submission.
                        </div>
                        <p style="color: var(--text-muted); font-size: 0.85rem; margin-bottom: 1.5rem;">
                            Your responses will be graded securely on the server and permanently recorded to your student profile.
                        </p>
                        <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1.5rem; display: grid; grid-template-columns: 1fr 1fr 1fr; text-align: center;">
                            <div>
                                <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Answered</div>
                                <div id="modalSummaryAnswered" style="font-size: 1.5rem; font-weight: 700; color: var(--primary);">0</div>
                            </div>
                            <div>
                                <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Marked</div>
                                <div id="modalSummaryMarked" style="font-size: 1.5rem; font-weight: 700; color: #b45309;">0</div>
                            </div>
                            <div>
                                <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Unanswered</div>
                                <div id="modalSummaryUnanswered" style="font-size: 1.5rem; font-weight: 700; color: var(--text-muted);">25</div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary btn-sm" onclick="closeSubmitConfirmModal()">Return to Exam</button>
                        <button type="button" id="btnFinalSubmit" class="btn btn-primary btn-sm" onclick="executeFinalSubmit()">Confirm & Submit Test</button>
                    </div>
                </div>
            </div>

            <!-- Centralized Security Warning Modal (Three-Strike Warning Dialog) -->
            <div id="securityWarningModal" class="security-modal-overlay">
                <div class="security-modal-card">
                    <div class="security-modal-header">
                        <span style="font-size: 1.75rem;">🚨</span>
                        <div>
                            <h3 class="security-modal-title" id="securityModalTitle">Security Warning 1/3</h3>
                            <div style="font-size: 0.8rem; color: #b91c1c; font-weight: 600;" id="securityModalSubtitle">Integrity Violation Detected</div>
                        </div>
                    </div>
                    <div class="security-modal-body">
                        <div id="securityModalMessage" style="font-size: 1.05rem; font-weight: 600; color: #991b1b; margin-bottom: 0.75rem;">
                            Security Warning 1/3: Please remain in fullscreen and avoid switching tabs.
                        </div>
                        <div id="securityModalDetail" style="font-size: 0.875rem; color: var(--text-secondary); line-height: 1.5; background: #fff5f5; border: 1px solid #fed7d7; padding: 0.85rem; border-radius: var(--radius-sm); margin-bottom: 1rem;">
                            Exiting fullscreen or switching windows is prohibited. You are allowed a maximum of 3 security violations. On the third violation, your assessment will be automatically submitted and closed.
                        </div>
                        <div style="font-size: 0.85rem; color: var(--text-muted);">
                            Violations remaining before auto-submission: <strong id="violationsRemainingCount" style="color: #dc2626; font-size: 1rem;">2</strong>
                        </div>
                    </div>
                    <div class="security-modal-footer">
                        <button type="button" id="btnReturnFullscreen" class="btn btn-primary btn-sm" onclick="acknowledgeWarningAndReturnFullscreen()" style="background: #dc2626; border-color: #b91c1c;">
                            Return to Fullscreen & Continue Test →
                        </button>
                    </div>
                </div>
            </div>

            <!-- Active Test Session Resume Modal (Handles page reload without resetting violation counter) -->
            <div id="sessionResumeModal" class="modal-overlay">
                <div class="modal-card" style="max-width: 520px; border: 2px solid var(--primary);">
                    <div class="modal-header" style="background: #eff6ff;">
                        <div style="display: flex; align-items: center; gap: 0.75rem;">
                            <span style="font-size: 1.5rem;">⏱️</span>
                            <h3 class="modal-title" style="color: var(--primary);">Ongoing Assessment In Progress</h3>
                        </div>
                    </div>
                    <div class="modal-body">
                        <p style="font-size: 0.95rem; color: var(--text-primary); margin-bottom: 1rem;">
                            You have an active examination session in progress. In accordance with exam integrity rules, tests must run in fullscreen mode.
                        </p>
                        <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1.25rem;">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem; font-size: 0.9rem;">
                                <span style="color: var(--text-muted);">Assessment:</span>
                                <strong id="resumeCategory">Technical</strong>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem; font-size: 0.9rem;">
                                <span style="color: var(--text-muted);">Time Remaining:</span>
                                <strong id="resumeTimeRemaining" style="color: var(--primary);">--:--</strong>
                            </div>
                            <div style="display: flex; justify-content: space-between; font-size: 0.9rem;">
                                <span style="color: var(--text-muted);">Security Violations:</span>
                                <strong id="resumeViolationCount" style="color: #dc2626;">0 / 3</strong>
                            </div>
                        </div>
                        <p style="font-size: 0.85rem; color: var(--text-muted);">
                            Note: Page refreshes do not reset your security violation counter or exam timer.
                        </p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary" onclick="resumeActiveExamInFullscreen()">
                            Enter Fullscreen & Resume Assessment →
                        </button>
                    </div>
                </div>
            </div>

            <!-- Result Card (Initially Hidden) -->
            <div id="testResultCard" class="test-runner glass-card" style="display: none; padding: 2.5rem;">
                <div style="text-align: center; margin-bottom: 2rem;">
                    <span id="resultStatusBadge" class="badge badge-success" style="font-size: 0.85rem; padding: 0.4rem 1rem; margin-bottom: 1rem;">PASSED</span>
                    <h2 style="font-size: 2rem; margin-bottom: 0.5rem;">Assessment Completed!</h2>
                    <p style="color: var(--text-muted);">Your performance has been validated on the server and synchronized to your Firebase profile.</p>

                    <div style="display: flex; justify-content: center; gap: 1.5rem; margin-top: 1.5rem; flex-wrap: wrap;">
                        <div style="background: #f8fafc; padding: 1.25rem 2rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); min-width: 160px;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Your Score</div>
                            <div id="resultScorePercent" style="font-family: var(--font-heading); font-size: 2.5rem; font-weight: 800; color: var(--primary);">0%</div>
                        </div>
                        <div style="background: #f8fafc; padding: 1.25rem 2rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); min-width: 160px;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Correct Answers</div>
                            <div id="resultCorrectFraction" style="font-family: var(--font-heading); font-size: 2.5rem; font-weight: 800; color: var(--text-primary);">0 / 25</div>
                        </div>
                        <div style="background: #f8fafc; padding: 1.25rem 2rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); min-width: 160px;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Integrity Status</div>
                            <div id="resultRiskDisplay" style="font-family: var(--font-heading); font-size: 1.5rem; font-weight: 700; color: #166534; margin-top: 0.5rem;">LOW RISK</div>
                        </div>
                    </div>
                </div>

                <div style="border-top: 1px solid var(--border-subtle); padding-top: 1.5rem; margin-bottom: 2rem;">
                    <h3 style="font-size: 1.2rem; margin-bottom: 1rem;">Question Explanations & Review</h3>
                    <div id="resultReviewList"></div>
                </div>

                <div style="text-align: center;">
                    <a href="${pageContext.request.contextPath}/tests" class="btn btn-primary">Done & Return to Assessments</a>
                </div>
            </div>
        </main>
    </div>
</body>
</html>
