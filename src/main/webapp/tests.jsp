<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.TestAttempt" %>
<%@ page import="com.tpobuddy.model.PlacementTest" %>
<%@ page import="com.tpobuddy.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%
    User user = (User) request.getAttribute("user");
    List<TestAttempt> attempts = (List<TestAttempt>) request.getAttribute("attempts");
    List<PlacementTest> availableTests = (List<PlacementTest>) request.getAttribute("availableTests");
    Integer totalQuestionBankSize = (Integer) request.getAttribute("totalQuestionBankSize");
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Company Placement Test Series — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <script src="${pageContext.request.contextPath}/assets/js/tests.js"></script>
    <style>
        .test-filter-tab {
            padding: 0.6rem 1.25rem;
            border-radius: var(--radius-sm);
            font-size: 0.875rem;
            font-weight: 600;
            cursor: pointer;
            border: 1px solid var(--border-subtle);
            background: #ffffff;
            color: var(--text-secondary);
            transition: all 0.2s ease;
        }
        .test-filter-tab:hover {
            background: #f1f5f9;
            color: var(--text-primary);
        }
        .test-filter-tab.active {
            background: var(--primary);
            color: #ffffff;
            border-color: var(--primary);
            box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
        }

        .exam-option-card {
            background: #ffffff;
            border: 2px solid var(--border-subtle);
            border-radius: var(--radius-sm);
            padding: 1rem 1.25rem;
            display: flex;
            align-items: center;
            gap: 1rem;
            cursor: pointer;
            transition: all 0.2s ease;
            margin-bottom: 0.75rem;
        }
        .exam-option-card:hover {
            border-color: #93c5fd;
            background: #f8fafc;
        }
        .exam-option-card.selected {
            border-color: var(--primary);
            background: #eff6ff;
            box-shadow: 0 0 0 1px var(--primary);
        }
        .exam-option-badge {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: #f1f5f9;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 0.85rem;
            color: var(--text-secondary);
            flex-shrink: 0;
            transition: all 0.2s;
        }
        .exam-option-badge.active {
            background: var(--primary);
            color: #ffffff;
        }
        .exam-option-text {
            font-size: 0.95rem;
            color: var(--text-primary);
            line-height: 1.4;
        }

        .palette-grid {
            display: grid;
            grid-template-columns: repeat(5, 1fr);
            gap: 0.5rem;
            max-height: 320px;
            overflow-y: auto;
            padding-right: 0.25rem;
        }
        .palette-btn {
            aspect-ratio: 1;
            border: 1px solid var(--border-subtle);
            background: #f8fafc;
            border-radius: var(--radius-sm);
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--text-secondary);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.15s;
        }
        .palette-btn:hover {
            border-color: var(--primary);
        }
        .palette-btn.current {
            border: 2px solid var(--primary);
            box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
        }
        .palette-btn.answered {
            background: #10b981;
            color: #ffffff;
            border-color: #059669;
        }
        .palette-btn.marked {
            background: #f59e0b;
            color: #ffffff;
            border-color: #d97706;
        }
        .palette-btn.answered.marked {
            background: linear-gradient(135deg, #10b981 50%, #f59e0b 50%);
            color: #ffffff;
        }

        .review-item-card {
            background: #ffffff;
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-sm);
            padding: 1.25rem;
            margin-bottom: 1rem;
        }
        .review-item-card.correct {
            border-left: 4px solid #10b981;
        }
        .review-item-card.incorrect {
            border-left: 4px solid #dc2626;
        }
    </style>
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="app-shell">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="app-main">
            <!-- Header -->
            <div class="page-header">
                <div>
                    <h1 class="page-title">Company Placement Preparation Test Series</h1>
                    <p class="page-subtitle">
                        Comprehensive campus recruitment assessments covering Quantitative Aptitude, Logical Reasoning, Verbal Ability, Technical Coding, and Company Mock Exams.
                    </p>
                </div>
                <div style="display: flex; gap: 0.75rem; align-items: center;">
                    <span class="badge badge-success" style="font-size: 0.85rem; padding: 0.45rem 0.85rem;">
                        ⚡ Sub-Millisecond Instant Load
                    </span>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- ============================================================= -->
            <!-- Test Selection Screen -->
            <!-- ============================================================= -->
            <div id="testCategoryPicker">
                <!-- Priority Topics Quick Access Banner -->
                <div class="glass-card" style="padding: 1.5rem; margin-bottom: 2rem; border: 2px solid #93c5fd; background: linear-gradient(135deg, rgba(239, 246, 255, 0.8) 0%, rgba(240, 253, 250, 0.8) 100%);">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; gap: 0.5rem;">
                        <div>
                            <div style="display: flex; align-items: center; gap: 0.5rem;">
                                <span style="font-size: 1.35rem;">🎯</span>
                                <h3 style="font-size: 1.15rem; font-weight: 800; color: var(--text-primary); margin: 0;">
                                    Placement Foundational Priority Topics (Quick 10-Q Practice)
                                </h3>
                                <span class="badge badge-primary">High Priority</span>
                            </div>
                            <p style="font-size: 0.85rem; color: var(--text-secondary); margin: 0.25rem 0 0 0;">
                                Initial placement preparation core syllabus: Master these 6 foundational aptitude modules first.
                            </p>
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 0.85rem;">
                        <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('Quantitative Aptitude', 10, 'topic_num_system', 'Number System')" style="background: #ffffff; text-align: left; padding: 0.75rem 1rem; display: block;">
                            <div style="font-weight: 700; color: var(--text-primary);">1. Number System</div>
                            <div style="font-size: 0.75rem; color: var(--text-muted);">Remainders, unit digits</div>
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('Quantitative Aptitude', 10, 'topic_hcf_lcm', 'HCF and LCM')" style="background: #ffffff; text-align: left; padding: 0.75rem 1rem; display: block;">
                            <div style="font-weight: 700; color: var(--text-primary);">2. HCF and LCM</div>
                            <div style="font-size: 0.75rem; color: var(--text-muted);">Co-primes, bells chiming</div>
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('Quantitative Aptitude', 10, 'topic_divisibility', 'Divisibility Rules')" style="background: #ffffff; text-align: left; padding: 0.75rem 1rem; display: block;">
                            <div style="font-weight: 700; color: var(--text-primary);">3. Divisibility Rules</div>
                            <div style="font-size: 0.75rem; color: var(--text-muted);">Divisibility by 7, 8, 9, 11</div>
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('Quantitative Aptitude', 10, 'topic_percentages', 'Percentages')" style="background: #ffffff; text-align: left; padding: 0.75rem 1rem; display: block;">
                            <div style="font-weight: 700; color: var(--text-primary);">4. Percentages</div>
                            <div style="font-size: 0.75rem; color: var(--text-muted);">Successive change, price</div>
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('Quantitative Aptitude', 10, 'topic_profit_loss', 'Profit and Loss')" style="background: #ffffff; text-align: left; padding: 0.75rem 1rem; display: block;">
                            <div style="font-weight: 700; color: var(--text-primary);">5. Profit and Loss</div>
                            <div style="font-size: 0.75rem; color: var(--text-muted);">False weights, margins</div>
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('Quantitative Aptitude', 10, 'topic_discounts', 'Discounts')" style="background: #ffffff; text-align: left; padding: 0.75rem 1rem; display: block;">
                            <div style="font-weight: 700; color: var(--text-primary);">6. Discounts</div>
                            <div style="font-size: 0.75rem; color: var(--text-muted);">Successive discounts, offers</div>
                        </button>
                    </div>
                </div>

                <!-- Section Filter Tabs -->
                <div style="display: flex; gap: 0.75rem; margin-bottom: 1.5rem; flex-wrap: wrap;">
                    <button type="button" class="test-filter-tab active" onclick="filterTestsByTab('all', this)">
                        All Assessments
                    </button>
                    <button type="button" class="test-filter-tab" onclick="filterTestsByTab('Quantitative Aptitude', this)">
                        📐 Quantitative Aptitude
                    </button>
                    <button type="button" class="test-filter-tab" onclick="filterTestsByTab('Logical Reasoning', this)">
                        🧩 Logical Reasoning
                    </button>
                    <button type="button" class="test-filter-tab" onclick="filterTestsByTab('Verbal Ability', this)">
                        📖 Verbal Ability
                    </button>
                    <button type="button" class="test-filter-tab" onclick="filterTestsByTab('Technical and Coding', this)">
                        💻 Technical & Coding
                    </button>
                    <button type="button" class="test-filter-tab" onclick="filterTestsByTab('Company Mock', this)">
                        🏢 Company Mocks
                    </button>
                </div>

                <!-- Tests Grid -->
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.5rem; margin-bottom: 2.5rem;" id="testsCardsGrid">
                    <% if (availableTests != null) {
                        for (PlacementTest t : availableTests) {
                            String badgeColor = "badge-primary";
                            if ("Quantitative Aptitude".equalsIgnoreCase(t.getCategory())) badgeColor = "badge-primary";
                            else if ("Logical Reasoning".equalsIgnoreCase(t.getCategory())) badgeColor = "badge-secondary";
                            else if ("Verbal Ability".equalsIgnoreCase(t.getCategory())) badgeColor = "badge-success";
                            else if ("Technical and Coding".equalsIgnoreCase(t.getCategory())) badgeColor = "badge-warning";
                    %>
                        <div class="card-3d glass-card test-card-item" data-category="<%= t.getCategory() %>" style="padding: 1.75rem; display: flex; flex-direction: column; justify-content: space-between;">
                            <div>
                                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                                    <span class="badge <%= badgeColor %>" style="font-size: 0.75rem;"><%= t.getCategory() %></span>
                                    <span class="badge badge-outline" style="font-size: 0.75rem;"><%= t.getDifficulty() %></span>
                                </div>
                                <h3 style="font-size: 1.2rem; margin-bottom: 0.4rem; color: var(--text-primary);"><%= t.getTitle() %></h3>
                                <p style="font-size: 0.85rem; color: var(--text-muted); line-height: 1.4; margin-bottom: 1.25rem;">
                                    <%= t.getDescription() %>
                                </p>
                                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; margin-bottom: 1.5rem; font-size: 0.8rem; color: var(--text-secondary); background: #f8fafc; padding: 0.75rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                                    <div>📝 <strong><%= t.getQuestionCount() %></strong> Questions</div>
                                    <div>⏱️ <strong><%= t.getDurationMinutes() %></strong> Minutes</div>
                                    <div>🎯 <strong>+<%= t.getMarksPerQuestion() %></strong> Marks/Q</div>
                                    <div><%= t.isNegativeMarking() ? "⚠️ -" + t.getNegativeMarks() + " Negative" : "✓ No Negative" %></div>
                                </div>
                            </div>
                            <button type="button" class="btn btn-primary" onclick="initiateExamSetup('<%= t.getCategory() %>', <%= t.getQuestionCount() %>, '<%= t.getId() %>', '<%= t.getSubCategory() %>')">
                                <span>Start Test</span>
                                <span>→</span>
                            </button>
                        </div>
                    <%  }
                    } %>
                </div>

                <!-- Test History Table -->
                <div class="data-table-card">
                    <div class="data-table-header">
                        <h3 style="font-size: 1.15rem;">Your Completed Assessment History</h3>
                        <span style="font-size: 0.85rem; color: var(--text-muted);">Synced from Firebase Realtime Database</span>
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
                                    for (TestAttempt a : attempts) { %>
                                    <tr>
                                        <td style="font-weight: 600; color: var(--text-primary);"><%= a.getCategory() %></td>
                                        <td><%= a.getTopic() != null ? a.getTopic() : "General" %></td>
                                        <td style="font-weight: 700; color: <%= a.isPassed() ? "var(--accent-emerald)" : "var(--accent-amber)" %>;">
                                            <%= a.getScore() %>%
                                        </td>
                                        <td><%= a.getCorrectAnswers() %> / <%= a.getTotalQuestions() %></td>
                                        <td>
                                            <span class="badge <%= "HIGH".equalsIgnoreCase(a.getRiskLevel()) ? "badge-danger" : ("MODERATE".equalsIgnoreCase(a.getRiskLevel()) ? "badge-warning" : "badge-success") %>">
                                                <%= a.getRiskLevel() != null ? a.getRiskLevel() : "LOW" %>
                                            </span>
                                        </td>
                                        <td>
                                            <span class="badge <%= a.isPassed() ? "badge-success" : "badge-warning" %>">
                                                <%= a.isPassed() ? "PASSED" : "NEEDS PRACTICE" %>
                                            </span>
                                        </td>
                                        <td><%= sdf.format(new Date(a.getSavedAt())) %></td>
                                    </tr>
                                <%  }
                                } else { %>
                                    <tr>
                                        <td colspan="7" style="text-align: center; padding: 3rem; color: var(--text-muted);">
                                            No test attempts recorded yet. Choose any placement test above to start your practice.
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- ============================================================= -->
            <!-- Active Test Runner (Initially Hidden) -->
            <!-- ============================================================= -->
            <div id="testRunner" class="test-runner glass-card" style="display: none; padding: 2rem;">
                <!-- Runner Header -->
                <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1.25rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
                    <div>
                        <span id="runnerCategoryBadge" class="badge badge-primary" style="margin-bottom: 0.25rem;">Quantitative Aptitude</span>
                        <h2 id="runnerQuestionNumber" style="font-size: 1.4rem; margin: 0.25rem 0 0 0;">Question 1 of 25</h2>
                    </div>

                    <div style="display: flex; align-items: center; gap: 1rem;">
                        <div style="display: flex; align-items: center; gap: 0.5rem; background: #f8fafc; padding: 0.5rem 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                            <span style="font-size: 1.25rem;">⏱️</span>
                            <div>
                                <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase;">Remaining</div>
                                <div id="runnerTimerDisplay" style="font-family: var(--font-heading); font-size: 1.35rem; font-weight: 800; color: var(--text-primary); line-height: 1;">30:00</div>
                            </div>
                        </div>

                        <button type="button" id="btnSubmitExam" class="btn btn-primary" onclick="confirmAndSubmitExam()" style="background: #10b981; border-color: #059669;">
                            ✓ Submit Test
                        </button>
                    </div>
                </div>

                <!-- Runner Body Grid: Question Statement + Palette -->
                <div style="display: grid; grid-template-columns: 1fr 280px; gap: 2rem;">
                    <!-- Question Area -->
                    <div>
                        <div style="display: flex; gap: 0.5rem; align-items: center; margin-bottom: 1rem;">
                            <span id="runnerDifficultyBadge" class="badge badge-success">Medium</span>
                            <span id="runnerMarksBadge" class="badge badge-outline">+2 Marks</span>
                        </div>

                        <div id="runnerQuestionStatement" style="font-size: 1.15rem; font-weight: 600; color: var(--text-primary); line-height: 1.6; margin-bottom: 1.5rem;">
                            Loading question...
                        </div>

                        <!-- 4 Options -->
                        <div id="runnerOptionsList"></div>

                        <!-- Controls -->
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem; pt: 1rem; border-top: 1px solid var(--border-subtle);">
                            <button type="button" id="btnPrevQuestion" class="btn btn-outline" onclick="prevQuestion()" disabled>
                                ← Previous
                            </button>

                            <div style="display: flex; gap: 0.5rem;">
                                <button type="button" id="btnMarkReview" class="btn btn-outline btn-sm" onclick="toggleMarkForReview()">
                                    ☆ Mark for Review
                                </button>
                                <button type="button" class="btn btn-outline btn-sm" onclick="clearCurrentResponse()">
                                    Clear Response
                                </button>
                            </div>

                            <button type="button" id="btnNextQuestion" class="btn btn-primary" onclick="nextQuestion()">
                                Next Question →
                            </button>
                        </div>
                    </div>

                    <!-- Right Column: Question Palette & Pip Camera -->
                    <div>
                        <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1.25rem;">
                            <h4 style="font-size: 0.95rem; margin-bottom: 1rem; color: var(--text-primary);">Question Navigation</h4>

                            <!-- Palette Legend -->
                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; font-size: 0.75rem; margin-bottom: 1rem;">
                                <div><span style="color: #10b981; font-weight: 700;">●</span> Answered (<span id="paletteCountAnswered">0</span>)</div>
                                <div><span style="color: #94a3b8; font-weight: 700;">●</span> Unanswered (<span id="paletteCountUnanswered">25</span>)</div>
                                <div><span style="color: #f59e0b; font-weight: 700;">●</span> Marked (<span id="paletteCountMarked">0</span>)</div>
                            </div>

                            <!-- Palette Grid -->
                            <div id="runnerQuestionPalette" class="palette-grid"></div>

                            <!-- Pip Camera -->
                            <div style="margin-top: 1.25rem; border-top: 1px solid var(--border-subtle); padding-top: 1rem;">
                                <video id="examWebcamPip" autoplay muted playsinline style="width: 100%; height: 120px; object-fit: cover; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); display: none; background: #000000;"></video>
                                <div style="font-size: 0.75rem; color: var(--text-muted); text-align: center; margin-top: 0.25rem;">
                                    🛡️ Proctoring Active
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ============================================================= -->
            <!-- Result & Solutions Card (Initially Hidden) -->
            <!-- ============================================================= -->
            <div id="testResultCard" class="test-runner glass-card" style="display: none; padding: 2.5rem;">
                <div style="text-align: center; margin-bottom: 2rem;">
                    <span id="resultStatusBadge" class="badge badge-success" style="font-size: 0.85rem; padding: 0.4rem 1rem; margin-bottom: 1rem;">PASSED</span>
                    <h2 style="font-size: 2rem; margin-bottom: 0.5rem;">Assessment Completed!</h2>
                    <p style="color: var(--text-muted);">Your performance has been scored and synchronized to your Firebase student profile.</p>

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
                            <div id="resultRiskDisplay" style="font-family: var(--font-heading); font-size: 1.35rem; font-weight: 700; color: #16a34a; margin-top: 0.5rem;">LOW RISK</div>
                        </div>
                    </div>
                </div>

                <div style="border-top: 1px solid var(--border-subtle); padding-top: 1.5rem; margin-bottom: 2rem;">
                    <h3 style="font-size: 1.2rem; margin-bottom: 1rem;">Question Explanations & Step-by-Step Solutions</h3>
                    <div id="resultReviewList"></div>
                </div>

                <div style="text-align: center;">
                    <a href="${pageContext.request.contextPath}/tests" class="btn btn-primary">Done & Return to Assessments</a>
                </div>
            </div>

            <!-- ============================================================= -->
            <!-- Modals: Setup, Security Violation, Resume -->
            <!-- ============================================================= -->

            <!-- Instructions & Consent Modal -->
            <div id="examSetupModal" class="modal-overlay">
                <div class="modal-card" style="max-width: 540px;">
                    <div class="modal-header">
                        <div>
                            <h3 class="modal-title" id="setupModalTitle">Assessment Setup & Rules</h3>
                            <div style="font-size: 0.8rem; color: var(--text-muted);" id="setupCategoryDetail">Quantitative Aptitude • 25 Questions</div>
                        </div>
                        <button type="button" class="btn-close" onclick="cancelExamSetup()">✕</button>
                    </div>
                    <div class="modal-body">
                        <ul style="font-size: 0.85rem; color: var(--text-secondary); line-height: 1.6; margin-bottom: 1rem; padding-left: 1.2rem;">
                            <li>The assessment runs in <strong>mandatory fullscreen mode</strong>.</li>
                            <li>Exiting fullscreen or switching browser tabs triggers an <strong>integrity warning</strong>.</li>
                            <li>A maximum of <strong>3 violations</strong> are permitted. On the 3rd violation, the test is automatically submitted.</li>
                            <li>Countdown timer continues even if you reload the page.</li>
                        </ul>

                        <!-- Optional Webcam Preview -->
                        <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1.25rem;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                                <div style="display: flex; align-items: center; gap: 0.5rem;">
                                    <span id="setupStatusDot" style="width: 10px; height: 10px; border-radius: 50%; background: #94a3b8; display: inline-block;"></span>
                                    <span style="font-size: 0.8rem; font-weight: 600;" id="setupStatusText">Webcam check (optional)</span>
                                </div>
                                <button type="button" class="btn btn-outline btn-sm" onclick="requestWebcamVerification()">Check Camera</button>
                            </div>
                            <video id="setupWebcamPreview" autoplay muted playsinline style="width: 100%; height: 140px; object-fit: cover; border-radius: var(--radius-sm); display: none;"></video>
                            <div id="setupWebcamPlaceholder" style="text-align: center; padding: 1rem; color: var(--text-muted); font-size: 0.85rem;">
                                📷 Camera Not Connected
                            </div>
                        </div>

                        <!-- Consent Checkbox -->
                        <label style="display: flex; align-items: center; gap: 0.75rem; cursor: pointer; font-size: 0.85rem; color: var(--text-primary);">
                            <input type="checkbox" id="examConsentCheckbox" onchange="handleConsentChange(this)">
                            <span>I agree to the test integrity guidelines and acknowledge fullscreen proctoring.</span>
                        </label>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline" onclick="cancelExamSetup()">Cancel</button>
                        <button type="button" id="btnStartExamConfirmed" class="btn btn-primary" onclick="startExamInFullscreen()" disabled>
                            Start Placement Test →
                        </button>
                    </div>
                </div>
            </div>

            <!-- Security Warning Modal -->
            <div id="securityWarningModal" class="security-modal-overlay">
                <div class="security-modal-card">
                    <div class="security-modal-header">
                        <span style="font-size: 1.75rem;">🚨</span>
                        <div>
                            <h3 class="security-modal-title" id="securityModalTitle">Security Warning 1/3</h3>
                            <div style="font-size: 0.8rem; color: #b91c1c; font-weight: 600;">Integrity Violation Detected</div>
                        </div>
                    </div>
                    <div class="security-modal-body">
                        <p style="font-size: 0.95rem; color: #991b1b; margin-bottom: 0.75rem;">
                            Exiting fullscreen or switching windows is prohibited during examination.
                        </p>
                        <div style="font-size: 0.85rem; color: var(--text-muted);">
                            Violations remaining before auto-submission: <strong id="violationsRemainingCount" style="color: #dc2626; font-size: 1.1rem;">2</strong>
                        </div>
                    </div>
                    <div class="security-modal-footer">
                        <button type="button" class="btn btn-primary btn-sm" onclick="acknowledgeWarningAndReturnFullscreen()" style="background: #dc2626; border-color: #b91c1c;">
                            Return to Fullscreen & Continue Test →
                        </button>
                    </div>
                </div>
            </div>

            <!-- Session Resume Modal -->
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
                            You have an active examination session in progress. In accordance with test integrity rules, you must return to fullscreen to resume.
                        </p>
                        <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1rem; font-size: 0.85rem;">
                            <div>Assessment: <strong id="resumeCategory">Quantitative Aptitude</strong></div>
                            <div>Remaining: <strong id="resumeTimeRemaining" style="color: var(--primary);">--:--</strong></div>
                            <div>Violations: <strong id="resumeViolationCount" style="color: #dc2626;">0 / 3</strong></div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary" onclick="resumeActiveExamInFullscreen()">
                            Enter Fullscreen & Resume Assessment →
                        </button>
                    </div>
                </div>
            </div>

        </main>
    </div>

    <script>
        function filterTestsByTab(category, btn) {
            document.querySelectorAll('.test-filter-tab').forEach(b => b.classList.remove('active'));
            if (btn) btn.classList.add('active');

            const cards = document.querySelectorAll('.test-card-item');
            cards.forEach(card => {
                if (category === 'all') {
                    card.style.display = 'flex';
                } else {
                    const cardCat = card.getAttribute('data-category');
                    if (cardCat && cardCat.toLowerCase().includes(category.toLowerCase())) {
                        card.style.display = 'flex';
                    } else {
                        card.style.display = 'none';
                    }
                }
            });
        }
    </script>
</body>
</html>
