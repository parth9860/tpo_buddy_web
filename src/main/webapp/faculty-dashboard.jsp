<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.*" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%
    User user = (User) request.getAttribute("user");
    List<User> students = (List<User>) request.getAttribute("students");
    List<Job> drives = (List<Job>) request.getAttribute("drives");
    List<TestAttempt> proctoringAttempts = (List<TestAttempt>) request.getAttribute("proctoringAttempts");
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Faculty / TPO Portal — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="app-shell">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="app-main">
            <!-- Header -->
            <div class="page-header">
                <div>
                    <h1 class="page-title">Faculty & TPO Management Portal</h1>
                    <p class="page-subtitle">
                        Welcome Prof. <%= user != null ? user.getName() : "Faculty" %> • <%= user != null ? user.getBranch() : "CSE" %> Department
                    </p>
                </div>
                <div style="display: flex; gap: 0.75rem; align-items: center;">
                    <div class="badge badge-primary" style="font-size: 0.85rem; padding: 0.5rem 1rem;">
                        Institutional TPO Access
                    </div>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- Stats Overview -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-label">Enrolled Students</div>
                    <div class="metric-value"><%= students != null ? students.size() : 0 %></div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Synced from Firebase users</div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Proctored Exam Attempts</div>
                    <div class="metric-value"><%= proctoringAttempts != null ? proctoringAttempts.size() : 0 %></div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Online examination records</div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Active Drives</div>
                    <div class="metric-value"><%= drives != null ? drives.size() : 0 %></div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Verified recruitment listings</div>
                </div>

                <div class="metric-card">
                    <div class="metric-label">Department Scope</div>
                    <div class="metric-value" style="font-size: 1.6rem;"><%= user != null ? user.getBranch() : "CSE" %></div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Academic Placement Division</div>
                </div>
            </div>

            <!-- ================================================================= -->
            <!-- SNJB College — Main Placement Test Series Management & Question Bank -->
            <!-- ================================================================= -->
            <div class="data-table-card" id="snjb-management" style="border: 2px solid #bfdbfe; background: #ffffff;">
                <div class="data-table-header" style="flex-wrap: wrap; gap: 1rem; align-items: center; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1.25rem;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem;">
                            <span style="font-size: 1.25rem;">🏛️</span>
                            <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--text-primary); margin: 0;">
                                SNJB College – Main Placement Test Series Management
                            </h3>
                            <span class="badge badge-primary">TPO Controller</span>
                        </div>
                        <p style="font-size: 0.85rem; color: var(--text-muted); margin: 0;">
                            Configure campus placement tests, manage 4 main series and 55 subsections, preview Gemini questions, and export student performance analytics.
                        </p>
                    </div>

                    <div style="display: flex; gap: 0.65rem; flex-wrap: wrap;">
                        <button type="button" class="btn btn-sm" onclick="openGeminiGenerationModal()" style="background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%); color: #ffffff; border: none; font-weight: 600; box-shadow: 0 2px 6px rgba(124, 58, 237, 0.25);">
                            ✨ Generate with Gemini AI
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="openQuestionBankModal()">
                            📚 Question Bank & CSV Import
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="downloadSnjbResultsCsv()" style="border-color: #10b981; color: #047857;">
                            📥 Export Results (CSV)
                        </button>
                        <button type="button" class="btn btn-primary btn-sm" onclick="openCreateTestModal()">
                            + Create New Test
                        </button>
                    </div>
                </div>

                <!-- Live Series Analytics Stats -->
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem; padding: 1.25rem; background: #f8fafc; border-bottom: 1px solid var(--border-subtle);">
                    <div style="background: #ffffff; padding: 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                        <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Active Series Tests</div>
                        <div style="font-size: 1.5rem; font-weight: 800; color: var(--primary);" id="adminStatTestsCount">55 Topics</div>
                        <div style="font-size: 0.75rem; color: var(--text-muted);" id="adminStatCategoriesCount">4 Main Series</div>
                    </div>
                    <div style="background: #ffffff; padding: 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                        <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Student Participation</div>
                        <div style="font-size: 1.5rem; font-weight: 800; color: var(--text-primary);" id="adminStatAttemptsCount">0 Attempts</div>
                        <div style="font-size: 0.75rem; color: var(--text-muted);">Recorded across series</div>
                    </div>
                    <div style="background: #ffffff; padding: 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                        <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Overall Batch Average</div>
                        <div style="font-size: 1.5rem; font-weight: 800; color: #0284c7;" id="adminStatBatchAvg">0%</div>
                        <div style="font-size: 0.75rem; color: #16a34a;" id="adminStatBatchPassRate">0% Pass Rate</div>
                    </div>
                    <div style="background: #ffffff; padding: 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); grid-column: span 1;">
                        <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Student Weak Topics Diagnosis</div>
                        <div id="adminStatWeakTopics" style="font-size: 0.85rem; font-weight: 600; color: #92400e; margin-top: 0.25rem;">
                            Aggregating student performance telemetry...
                        </div>
                    </div>
                </div>

                <!-- Test Definitions Table -->
                <div class="table-responsive">
                    <table class="table" id="adminTestsTable">
                        <thead>
                            <tr>
                                <th>Test Title & Category</th>
                                <th>Difficulty</th>
                                <th>Questions / Marks</th>
                                <th>Negative Marking</th>
                                <th>Duration</th>
                                <th>Attempts & Target</th>
                                <th>Status</th>
                                <th style="text-align: right;">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="adminTestsTableBody">
                            <tr>
                                <td colspan="8" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                                    ⏳ Loading SNJB Placement Tests...
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Online Examination Proctoring & Integrity Review -->
            <div class="data-table-card">
                <div class="data-table-header">
                    <div>
                        <h3 style="font-size: 1.15rem;">Online Examination Proctoring & Integrity Audit</h3>
                        <p style="font-size: 0.85rem; color: var(--text-muted); margin: 0.25rem 0 0 0;">
                            Server-evaluated exam sessions with webcam presence monitoring, focus-loss tracking, and anti-cheating risk scoring.
                        </p>
                    </div>
                    <span class="badge badge-secondary"><%= proctoringAttempts != null ? proctoringAttempts.size() : 0 %> Recorded Attempts</span>
                </div>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Student Name</th>
                                <th>Assessment & Topic</th>
                                <th>Score</th>
                                <th>Risk Level</th>
                                <th>Violations Summary</th>
                                <th>Proctoring Status</th>
                                <th>Timestamp</th>
                                <th>Review</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (proctoringAttempts != null && !proctoringAttempts.isEmpty()) {
                                for (TestAttempt pa : proctoringAttempts) { 
                                    String rLevel = pa.getRiskLevel() != null ? pa.getRiskLevel() : "LOW";
                                    String pStatus = pa.getProctoringStatus() != null ? pa.getProctoringStatus() : "VALIDATED";
                                    String safeJson = (pa.getMonitoringEventsJson() != null && !pa.getMonitoringEventsJson().isEmpty())
                                            ? pa.getMonitoringEventsJson().replace("\"", "&quot;").replace("'", "&#39;")
                                            : "[]";
                                    String safeNotes = (pa.getReviewNotes() != null) ? pa.getReviewNotes().replace("\"", "&quot;") : "";
                            %>
                                <tr>
                                    <td>
                                        <div style="font-weight: 600; color: var(--text-primary);">
                                            <%= (pa.getStudentName() != null && !pa.getStudentName().isEmpty()) ? pa.getStudentName() : pa.getUserId() %>
                                        </div>
                                        <div style="font-size: 0.75rem; color: var(--text-muted); font-family: monospace;">
                                            <%= pa.getId() != null && pa.getId().length() > 8 ? pa.getId().substring(0, 8) + "..." : pa.getId() %>
                                        </div>
                                    </td>
                                    <td>
                                        <div style="font-weight: 500;"><%= pa.getCategory() %></div>
                                        <div style="font-size: 0.8rem; color: var(--text-muted);"><%= pa.getTopic() %></div>
                                    </td>
                                    <td>
                                        <div style="font-weight: 700; color: <%= pa.isPassed() ? "var(--accent-emerald)" : "var(--accent-amber)" %>;">
                                            <%= pa.getScore() %>%
                                        </div>
                                        <div style="font-size: 0.75rem; color: var(--text-muted);">
                                            <%= pa.getCorrectAnswers() %>/<%= pa.getTotalQuestions() %>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge <%= "HIGH".equals(rLevel) ? "badge-risk-high" : ("MODERATE".equals(rLevel) ? "badge-risk-moderate" : "badge-risk-low") %>">
                                            <%= rLevel %> (<%= pa.getRiskScore() %>/100)
                                        </span>
                                    </td>
                                    <td style="font-size: 0.8rem; color: var(--text-secondary); line-height: 1.4;">
                                        <div>🪟 Focus lost: <strong><%= pa.getFocusLostCount() %></strong></div>
                                        <div>🖥 Fullscreen exit: <strong><%= pa.getFullscreenExitCount() %></strong></div>
                                        <div>📷 Camera disconnects: <strong><%= pa.getCameraDisconnectCount() %></strong></div>
                                    </td>
                                    <td>
                                        <span class="badge <%= "FLAGGED".equals(pStatus) ? "badge-warning" : ("REVIEWED".equals(pStatus) ? "badge-success" : "badge-primary") %>" id="status-badge-<%= pa.getId() %>">
                                            <%= pStatus %>
                                        </span>
                                    </td>
                                    <td style="font-size: 0.85rem; color: var(--text-muted); white-space: nowrap;">
                                        <%= sdf.format(new Date(pa.getSavedAt())) %>
                                    </td>
                                    <td>
                                        <button type="button" class="btn btn-outline btn-sm"
                                                onclick="openFacultyReviewModal('<%= pa.getId() %>', '<%= pa.getUserId() %>', '<%= (pa.getStudentName() != null ? pa.getStudentName() : pa.getUserId()).replace("'", "\\'") %>', '<%= pa.getCategory() %>', <%= pa.getScore() %>, '<%= rLevel %>', <%= pa.getRiskScore() %>, <%= pa.getFocusLostCount() %>, <%= pa.getFullscreenExitCount() %>, <%= pa.getCameraDisconnectCount() %>, <%= pa.getCopyPasteCount() %>, '<%= pStatus %>', '<%= safeNotes %>', '<%= safeJson %>')">
                                            Audit Log
                                        </button>
                                    </td>
                                </tr>
                            <%  }
                            } else { %>
                                <tr>
                                    <td colspan="8" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                                        No online examination attempts recorded yet.
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Student Directory Table -->
            <div class="data-table-card">
                <div class="data-table-header">
                    <h3 style="font-size: 1.15rem;">Registered Students Directory</h3>
                    <span style="font-size: 0.85rem; color: var(--text-muted);">Realtime Database `users`</span>
                </div>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Student Name</th>
                                <th>Email</th>
                                <th>Branch</th>
                                <th>Academic Year</th>
                                <th>Role</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (students != null && !students.isEmpty()) {
                                for (User s : students) { %>
                                <tr>
                                    <td style="font-weight: 600; color: var(--text-primary);"><%= s.getName() %></td>
                                    <td><%= s.getEmail() %></td>
                                    <td><span class="badge badge-primary"><%= s.getBranch() %></span></td>
                                    <td><%= s.getYear() %></td>
                                    <td><span class="badge <%= s.isTeacher() ? "badge-warning" : "badge-success" %>"><%= s.getRole().toUpperCase() %></span></td>
                                </tr>
                            <%  }
                            } else { %>
                                <tr>
                                    <td colspan="5" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">No student records found.</td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Placement Drives Management -->
            <div class="data-table-card">
                <div class="data-table-header">
                    <h3 style="font-size: 1.15rem;">Current Campus Recruitment Drives</h3>
                </div>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Company</th>
                                <th>Job Title</th>
                                <th>Package</th>
                                <th>Eligibility Criteria</th>
                                <th>Deadline</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (drives != null && !drives.isEmpty()) {
                                for (Job j : drives) { %>
                                <tr>
                                    <td style="font-weight: 600; color: var(--text-primary);"><%= j.getCompany() %></td>
                                    <td><%= j.getTitle() %></td>
                                    <td style="font-weight: 700; color: var(--secondary);"><%= j.getPackageInfo() %></td>
                                    <td><%= j.getEligibility() %></td>
                                    <td><%= j.getDeadline() %></td>
                                    <td><span class="badge badge-success"><%= j.getStatus() %></span></td>
                                </tr>
                            <%  }
                            } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>

    <!-- SNJB Placement Test Configuration Modal -->
    <div id="snjbTestModal" class="modal-overlay">
        <div class="modal-card" style="max-width: 800px;">
            <div class="modal-header">
                <div>
                    <h3 class="modal-title" id="testModalTitle">Create SNJB Placement Test</h3>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Configure test parameters, questions, grading & eligibility</div>
                </div>
                <button type="button" class="modal-close-btn" onclick="closeTestModal()">✕</button>
            </div>

            <form id="snjbTestForm" onsubmit="handleSaveTest(event)">
                <div class="modal-body">
                    <input type="hidden" id="editTestId" value="">

                    <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                        <div>
                            <label class="form-label" style="font-size: 0.85rem; font-weight: 700;">Test Title *</label>
                            <input type="text" id="editTestTitle" class="form-control" required placeholder="e.g. SNJB - Advanced Java & OOP Mastery" style="font-size: 0.9rem;">
                        </div>
                        <div>
                            <label class="form-label" style="font-size: 0.85rem; font-weight: 700;">Status</label>
                            <select id="editTestStatus" class="form-control" style="font-size: 0.9rem;">
                                <option value="PUBLISHED">PUBLISHED (Active)</option>
                                <option value="DRAFT">DRAFT (Draft Mode)</option>
                                <option value="ARCHIVED">ARCHIVED (Inactive)</option>
                            </select>
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                        <div>
                            <label class="form-label" style="font-size: 0.85rem; font-weight: 700;">Category *</label>
                            <select id="editTestCategory" class="form-control" required style="font-size: 0.85rem;">
                                <option value="Quantitative Aptitude">Quantitative Aptitude</option>
                                <option value="Logical Reasoning">Logical Reasoning</option>
                                <option value="Verbal Ability">Verbal Ability</option>
                                <option value="Programming and Coding">Programming and Coding</option>
                                <option value="Core Computer Science">Core Computer Science</option>
                                <option value="Technical Interview Preparation">Technical Interview Preparation</option>
                                <option value="Company-Specific Mock Tests">Company-Specific Mock Tests</option>
                                <option value="Full-Length Mock Tests">Full-Length Mock Tests</option>
                            </select>
                        </div>
                        <div>
                            <label class="form-label" style="font-size: 0.85rem; font-weight: 700;">Sub-Category / Topic</label>
                            <input type="text" id="editTestSubCategory" class="form-control" placeholder="e.g. Java OOP, TCS Pattern" style="font-size: 0.85rem;">
                        </div>
                        <div>
                            <label class="form-label" style="font-size: 0.85rem; font-weight: 700;">Difficulty Level</label>
                            <select id="editTestDifficulty" class="form-control" style="font-size: 0.85rem;">
                                <option value="Easy">Easy (Foundation)</option>
                                <option value="Medium" selected>Medium (Standard Campus)</option>
                                <option value="Hard">Hard (Advanced / Product)</option>
                            </select>
                        </div>
                    </div>

                    <div class="form-group" style="margin-bottom: 1rem;">
                        <label class="form-label" style="font-size: 0.85rem; font-weight: 700;">Test Description</label>
                        <textarea id="editTestDescription" class="form-control" rows="2" placeholder="Brief outline of syllabus covered and target outcomes..." style="font-size: 0.85rem;"></textarea>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1rem;">
                        <div>
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Question Count</label>
                            <input type="number" id="editTestQuestionCount" class="form-control" value="25" min="1" max="100" style="font-size: 0.85rem;" onchange="updateTotalMarksCalc()">
                        </div>
                        <div>
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Marks per Q</label>
                            <input type="number" id="editTestMarksPerQ" class="form-control" value="2" min="1" max="10" style="font-size: 0.85rem;" onchange="updateTotalMarksCalc()">
                        </div>
                        <div>
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Total Marks</label>
                            <input type="number" id="editTestTotalMarks" class="form-control" value="50" min="1" style="font-size: 0.85rem;">
                        </div>
                        <div>
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Duration (Mins)</label>
                            <input type="number" id="editTestDuration" class="form-control" value="25" min="5" max="180" style="font-size: 0.85rem;">
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-bottom: 1rem;">
                        <div>
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Passing %</label>
                            <input type="number" id="editTestPassingPct" class="form-control" value="60" min="20" max="100" style="font-size: 0.85rem;">
                        </div>
                        <div>
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Allowed Attempts</label>
                            <input type="number" id="editTestAllowedAttempts" class="form-control" value="0" min="0" max="10" title="0 = Unlimited attempts" style="font-size: 0.85rem;">
                            <span style="font-size: 0.7rem; color: var(--text-muted);">0 = Unlimited attempts</span>
                        </div>
                        <div>
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Target Branch</label>
                            <select id="editTestTargetBranch" class="form-control" style="font-size: 0.85rem;">
                                <option value="ALL">All Branches</option>
                                <option value="Computer Engineering">Computer Engineering</option>
                                <option value="Information Technology">Information Technology</option>
                                <option value="Mechanical Engineering">Mechanical Engineering</option>
                                <option value="Civil Engineering">Civil Engineering</option>
                                <option value="E&TC Engineering">E&TC Engineering</option>
                                <option value="AI & Data Science">AI & Data Science</option>
                            </select>
                        </div>
                    </div>

                    <!-- Negative Marking & Randomization Checks -->
                    <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1rem; display: flex; flex-direction: column; gap: 0.75rem;">
                        <div style="display: flex; align-items: center; gap: 1.5rem;">
                            <label style="display: flex; align-items: center; gap: 0.5rem; font-size: 0.85rem; font-weight: 600; cursor: pointer;">
                                <input type="checkbox" id="editTestNegativeMarking" onchange="toggleNegativeMarkingInput()">
                                Enable Negative Marking
                            </label>

                            <div id="negativeMarksField" style="display: none; align-items: center; gap: 0.5rem;">
                                <span style="font-size: 0.8rem; color: #dc2626; font-weight: 600;">Deduct:</span>
                                <input type="number" id="editTestNegativeDeduction" class="form-control" value="0.5" step="0.25" min="0.25" max="5" style="width: 80px; padding: 0.25rem 0.5rem; font-size: 0.85rem;">
                                <span style="font-size: 0.8rem; color: var(--text-muted);">marks per wrong answer</span>
                            </div>
                        </div>

                        <div style="display: flex; align-items: center; gap: 1.5rem;">
                            <label style="display: flex; align-items: center; gap: 0.5rem; font-size: 0.85rem; font-weight: 600; cursor: pointer;">
                                <input type="checkbox" id="editTestRandomize" checked>
                                Randomize Question Order for Each Candidate
                            </label>
                        </div>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <label class="form-label" style="font-size: 0.85rem; font-weight: 700;">Instructions for Students</label>
                        <textarea id="editTestInstructions" class="form-control" rows="2" placeholder="e.g. Ensure stable connection and webcam permissions before launching..." style="font-size: 0.85rem;"></textarea>
                    </div>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary btn-sm" onclick="closeTestModal()">Cancel</button>
                    <button type="submit" id="btnSaveTestSubmit" class="btn btn-primary btn-sm">Save Test Configuration</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Question Bank & CSV Import Modal -->
    <div id="snjbQuestionBankModal" class="modal-overlay">
        <div class="modal-card" style="max-width: 850px; max-height: 90vh; display: flex; flex-direction: column;">
            <div class="modal-header">
                <div>
                    <h3 class="modal-title">SNJB Question Bank & Bulk CSV Ingestion</h3>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Add verified MCQs with explanations or bulk import questions via CSV</div>
                </div>
                <button type="button" class="modal-close-btn" onclick="closeQuestionBankModal()">✕</button>
            </div>

            <!-- Tab Buttons -->
            <div style="display: flex; border-bottom: 1px solid var(--border-subtle); padding: 0 1.5rem; background: #f8fafc; gap: 0.5rem; flex-wrap: wrap;">
                <button type="button" id="tabBtnAddSingle" class="btn btn-sm" onclick="switchQBankTab('single')" style="border-radius: 0; border-bottom: 2px solid var(--primary); font-weight: 700; background: none; color: var(--primary);">
                    ➕ Add Single Question
                </button>
                <button type="button" id="tabBtnImportCsv" class="btn btn-sm" onclick="switchQBankTab('csv')" style="border-radius: 0; font-weight: 600; background: none; color: var(--text-secondary);">
                    📂 Bulk CSV Import
                </button>
                <button type="button" id="tabBtnBrowseBank" class="btn btn-sm" onclick="switchQBankTab('browse')" style="border-radius: 0; font-weight: 600; background: none; color: var(--text-secondary);">
                    🔍 Browse Repository (<span id="qBankTotalCount">0</span>)
                </button>
                <button type="button" id="tabBtnGeminiGen" class="btn btn-sm" onclick="switchQBankTab('gemini')" style="border-radius: 0; font-weight: 700; background: none; color: #7c3aed;">
                    🤖 Generate with Gemini AI
                </button>
            </div>

            <div class="modal-body" style="overflow-y: auto; flex: 1;">
                <!-- Tab 1: Single Question Form -->
                <div id="qBankSinglePane">
                    <form id="singleQuestionForm" onsubmit="handleSaveSingleQuestion(event)">
                        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Category *</label>
                                <select id="qCategory" class="form-control" required style="font-size: 0.85rem;">
                                    <option value="Quantitative Aptitude">Quantitative Aptitude</option>
                                    <option value="Logical Reasoning">Logical Reasoning</option>
                                    <option value="Verbal Ability">Verbal Ability</option>
                                    <option value="Programming and Coding" selected>Programming and Coding</option>
                                    <option value="Core Computer Science">Core Computer Science</option>
                                    <option value="Technical Interview Preparation">Technical Interview Preparation</option>
                                    <option value="Company-Specific Mock Tests">Company-Specific Mock Tests</option>
                                    <option value="Full-Length Mock Tests">Full-Length Mock Tests</option>
                                </select>
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Topic *</label>
                                <input type="text" id="qTopic" class="form-control" required placeholder="e.g. Java Polymorphism" style="font-size: 0.85rem;">
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Difficulty Level</label>
                                <select id="qDifficulty" class="form-control" style="font-size: 0.85rem;">
                                    <option value="Easy">Easy</option>
                                    <option value="Medium" selected>Medium</option>
                                    <option value="Hard">Hard</option>
                                </select>
                            </div>
                        </div>

                        <div class="form-group" style="margin-bottom: 1rem;">
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Question Statement *</label>
                            <textarea id="qText" class="form-control" rows="3" required placeholder="Enter clear, concise question statement..." style="font-size: 0.9rem;"></textarea>
                        </div>

                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 1rem;">
                            <div>
                                <label class="form-label" style="font-size: 0.8rem;">Option A *</label>
                                <input type="text" id="qOptionA" class="form-control" required placeholder="Answer option A" style="font-size: 0.85rem;">
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem;">Option B *</label>
                                <input type="text" id="qOptionB" class="form-control" required placeholder="Answer option B" style="font-size: 0.85rem;">
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem;">Option C *</label>
                                <input type="text" id="qOptionC" class="form-control" required placeholder="Answer option C" style="font-size: 0.85rem;">
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem;">Option D *</label>
                                <input type="text" id="qOptionD" class="form-control" required placeholder="Answer option D" style="font-size: 0.85rem;">
                            </div>
                        </div>

                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700; color: #16a34a;">Verified Correct Answer *</label>
                                <select id="qCorrectOption" class="form-control" required style="font-size: 0.85rem; border-color: #86efac; background: #f0fdf4;">
                                    <option value="0">Option A</option>
                                    <option value="1">Option B</option>
                                    <option value="2">Option C</option>
                                    <option value="3">Option D</option>
                                </select>
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Marks Assigned</label>
                                <input type="number" id="qMarks" class="form-control" value="2" min="1" max="10" style="font-size: 0.85rem;">
                            </div>
                        </div>

                        <div class="form-group" style="margin-bottom: 1rem;">
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Verified Explanation & Solution</label>
                            <textarea id="qExplanation" class="form-control" rows="2" placeholder="Explain why the answer is correct to assist candidate learning..." style="font-size: 0.85rem;"></textarea>
                        </div>

                        <div style="text-align: right;">
                            <button type="submit" id="btnSaveQuestion" class="btn btn-primary btn-sm">Add Question to Repository</button>
                        </div>
                    </form>
                </div>

                <!-- Tab 2: Bulk CSV Ingestion -->
                <div id="qBankCsvPane" style="display: none;">
                    <div style="background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: var(--radius-sm); padding: 1.25rem; margin-bottom: 1.5rem;">
                        <h4 style="font-size: 0.95rem; font-weight: 700; color: #166534; margin-bottom: 0.5rem;">CSV Ingestion Format Guidelines:</h4>
                        <p style="font-size: 0.85rem; color: #15803d; line-height: 1.5; margin-bottom: 0.75rem;">
                            The CSV file must contain the following header columns (case-insensitive):<br>
                            <code>category, topic, difficulty, question, optionA, optionB, optionC, optionD, correctOption, marks, explanation</code>
                        </p>
                        <p style="font-size: 0.8rem; color: #166534; margin-bottom: 1rem;">
                            • <code>correctOption</code> can be specified as index (<code>0, 1, 2, 3</code>) or letter (<code>A, B, C, D</code>).<br>
                            • Questions will be validated on the server for non-empty options and safe encoding.
                        </p>
                        <button type="button" class="btn btn-outline btn-sm" onclick="downloadCsvTemplate()" style="background: #ffffff; border-color: #16a34a; color: #166534;">
                            📄 Download Sample CSV Template
                        </button>
                    </div>

                    <form id="csvUploadForm" onsubmit="handleUploadCsv(event)">
                        <div class="form-group" style="margin-bottom: 1.25rem;">
                            <label class="form-label" style="font-size: 0.85rem; font-weight: 700;">Select Validated CSV File</label>
                            <input type="file" id="csvFileInput" class="form-control" accept=".csv" required style="padding: 0.5rem; font-size: 0.85rem;">
                        </div>

                        <div id="csvUploadResult" style="display: none; padding: 1rem; border-radius: var(--radius-sm); margin-bottom: 1rem; font-size: 0.85rem;"></div>

                        <button type="submit" id="btnUploadCsv" class="btn btn-primary btn-sm">
                            Upload and Validate Questions
                        </button>
                    </form>
                </div>

                <!-- Tab 3: Browse Repository -->
                <div id="qBankBrowsePane" style="display: none;">
                    <div style="display: flex; gap: 0.75rem; margin-bottom: 1rem;">
                        <input type="text" id="qBankSearchInput" class="form-control" placeholder="Search question repository by keyword or topic..." oninput="filterQBankTable()" style="font-size: 0.85rem;">
                    </div>
                    <div class="table-responsive" style="max-height: 380px; overflow-y: auto;">
                        <table class="table" style="font-size: 0.85rem;">
                            <thead>
                                <tr>
                                    <th>Question Statement</th>
                                    <th>Category & Topic</th>
                                    <th>Difficulty</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody id="qBankBrowseTableBody">
                                <tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 2rem;">Loading question repository...</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Tab 4: Gemini AI Question Generation & Review Workflow -->
                <div id="qBankGeminiPane" style="display: none;">
                    <!-- Status / Health check banner -->
                    <div id="geminiStatusBanner" style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1.25rem; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 0.75rem;">
                        <div style="display: flex; align-items: center; gap: 0.75rem;">
                            <div style="width: 36px; height: 36px; border-radius: 8px; background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%); display: flex; align-items: center; justify-content: center; color: #ffffff; font-size: 1.1rem;">
                                🤖
                            </div>
                            <div>
                                <div style="font-weight: 700; font-size: 0.9rem; color: var(--text-primary);" id="geminiStatusTitle">Connecting to Google Gemini API...</div>
                                <div style="font-size: 0.75rem; color: var(--text-muted);" id="geminiStatusSubtitle">Configured Model: Inspecting...</div>
                            </div>
                        </div>
                        <div id="geminiStatusBadge">
                            <span class="badge" style="background: #e2e8f0; color: #475569;">Verifying API Key...</span>
                        </div>
                    </div>

                    <!-- Gemini Generation Form -->
                    <form id="geminiGenerateForm" onsubmit="adminRunGeminiGeneration(event)">
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Section / Category *</label>
                                <select id="geminiGenSection" class="form-control" required style="font-size: 0.85rem;" onchange="onGeminiSectionChange()">
                                    <option value="Quantitative Aptitude" selected>Quantitative Aptitude (Phase 1 & 2)</option>
                                    <option value="Logical Reasoning">Logical Reasoning (Phase 2)</option>
                                    <option value="Verbal Ability">Verbal Ability (Phase 3)</option>
                                    <option value="Technical and Coding">Technical and Coding (Phase 3)</option>
                                </select>
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Difficulty Level *</label>
                                <select id="geminiGenDifficulty" class="form-control" style="font-size: 0.85rem;">
                                    <option value="Easy">Easy (Foundation Concepts)</option>
                                    <option value="Medium" selected>Medium (Standard Campus Placement)</option>
                                    <option value="Hard">Hard (Advanced / Product Level)</option>
                                </select>
                            </div>
                        </div>

                        <!-- Phase 1 Quick Topic Selectors -->
                        <div id="geminiPhase1TopicChips" style="margin-bottom: 1rem;">
                            <label class="form-label" style="font-size: 0.75rem; font-weight: 700; color: var(--primary); text-transform: uppercase;">
                                ⭐ Phase 1 Priority Topics (Click to Auto-fill):
                            </label>
                            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                                <button type="button" class="btn btn-outline btn-sm" onclick="setGeminiTopic('Number System', 'Quantitative Aptitude')" style="font-size: 0.75rem; padding: 0.25rem 0.6rem;">
                                    🔢 Number System
                                </button>
                                <button type="button" class="btn btn-outline btn-sm" onclick="setGeminiTopic('HCF and LCM', 'Quantitative Aptitude')" style="font-size: 0.75rem; padding: 0.25rem 0.6rem;">
                                    ➗ HCF & LCM
                                </button>
                                <button type="button" class="btn btn-outline btn-sm" onclick="setGeminiTopic('Divisibility Rules', 'Quantitative Aptitude')" style="font-size: 0.75rem; padding: 0.25rem 0.6rem;">
                                    📐 Divisibility Rules
                                </button>
                                <button type="button" class="btn btn-outline btn-sm" onclick="setGeminiTopic('Percentages', 'Quantitative Aptitude')" style="font-size: 0.75rem; padding: 0.25rem 0.6rem;">
                                    📊 Percentages
                                </button>
                                <button type="button" class="btn btn-outline btn-sm" onclick="setGeminiTopic('Profit and Loss', 'Quantitative Aptitude')" style="font-size: 0.75rem; padding: 0.25rem 0.6rem;">
                                    💹 Profit and Loss
                                </button>
                                <button type="button" class="btn btn-outline btn-sm" onclick="setGeminiTopic('Discounts', 'Quantitative Aptitude')" style="font-size: 0.75rem; padding: 0.25rem 0.6rem;">
                                    🏷️ Discounts
                                </button>
                            </div>
                        </div>

                        <div style="display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 1rem; margin-bottom: 1.25rem;">
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Topic / Syllabus Unit *</label>
                                <input type="text" id="geminiGenTopic" class="form-control" required placeholder="e.g. Number System, Percentages..." value="Number System" style="font-size: 0.85rem;">
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Question Type</label>
                                <select id="geminiGenType" class="form-control" style="font-size: 0.85rem;" onchange="onGeminiTypeChange()">
                                    <option value="MCQ" selected>MCQ (4 Options)</option>
                                    <option value="CODING">Coding Problem</option>
                                </select>
                            </div>
                            <div>
                                <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Question Count</label>
                                <select id="geminiGenCount" class="form-control" style="font-size: 0.85rem;">
                                    <option value="5">5 Questions</option>
                                    <option value="10" selected>10 Questions</option>
                                    <option value="15">15 Questions</option>
                                    <option value="20">20 Questions</option>
                                    <option value="25">25 Questions</option>
                                    <option value="30">30 Questions (Full Test)</option>
                                </select>
                            </div>
                        </div>

                        <div id="geminiLanguageRow" style="display: none; margin-bottom: 1.25rem;">
                            <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Programming Language / Tech Stack</label>
                            <select id="geminiGenLanguage" class="form-control" style="font-size: 0.85rem; max-width: 300px;">
                                <option value="Java" selected>Java (JDK 17+)</option>
                                <option value="Python">Python 3</option>
                                <option value="C++">C++ (Standard)</option>
                                <option value="SQL">SQL (ANSI / MySQL)</option>
                                <option value="General">Language-Agnostic Algorithm</option>
                            </select>
                        </div>

                        <div style="display: flex; justify-content: space-between; align-items: center; background: #faf5ff; border: 1px solid #e9d5ff; border-radius: var(--radius-sm); padding: 0.85rem 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.75rem;">
                            <div style="font-size: 0.8rem; color: #6b21a8;">
                                <strong>💡 Quality & Verification Engine:</strong> Every question is dynamically checked for distinct choices, validated arithmetic answers, and step-by-step explanations before preview.
                            </div>
                            <button type="submit" id="btnRunGeminiGen" class="btn btn-sm" style="background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%); color: #ffffff; border: none; font-weight: 700; white-space: nowrap; padding: 0.5rem 1.25rem;">
                                ✨ Generate with Gemini AI
                            </button>
                        </div>
                    </form>

                    <!-- Loading Indicator -->
                    <div id="geminiGenLoading" style="display: none; text-align: center; padding: 2.5rem 1.5rem; background: #ffffff; border: 1px dashed #cbd5e1; border-radius: var(--radius-sm); margin-bottom: 1.5rem;">
                        <div class="spinner" style="margin: 0 auto 1rem; width: 40px; height: 40px; border-width: 3px; border-top-color: #7c3aed;"></div>
                        <h4 style="font-size: 1rem; font-weight: 700; color: #1e293b; margin-bottom: 0.35rem;">Synthesizing Questions with Google Gemini AI...</h4>
                        <p style="font-size: 0.85rem; color: #64748b; margin-bottom: 0.5rem;" id="geminiLoadingMsg">
                            Enforcing topic alignment, verifying step-by-step arithmetic derivations, and ensuring unique answer options...
                        </p>
                        <div style="font-size: 0.75rem; color: #94a3b8;">Elapsed Time: <span id="geminiElapsedTimer">0s</span></div>
                    </div>

                    <!-- Generation Error Banner -->
                    <div id="geminiErrorBanner" style="display: none; background: #fee2e2; border: 1px solid #fca5a5; border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1.5rem; color: #991b1b; font-size: 0.85rem;">
                        <div style="font-weight: 700; margin-bottom: 0.25rem;" id="geminiErrorTitle">⚠️ Generation Failed</div>
                        <div id="geminiErrorMessage">Unable to complete question generation.</div>
                    </div>

                    <!-- Preview Container -->
                    <div id="geminiPreviewContainer" style="display: none;">
                        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.75rem; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: var(--radius-sm); padding: 0.85rem 1rem; margin-bottom: 1.25rem;">
                            <div>
                                <h4 style="font-size: 0.95rem; font-weight: 700; color: #166534; margin: 0;">
                                    📝 Generated Questions Preview (<span id="geminiPreviewCount">0</span> Questions)
                                </h4>
                                <div style="font-size: 0.8rem; color: #15803d; margin-top: 0.15rem;">
                                    Inspect, refine phrasing, verify correct answers, or discard individual questions before publishing to the live question bank.
                                </div>
                            </div>
                            <div style="display: flex; gap: 0.5rem; align-items: center;">
                                <button type="button" class="btn btn-outline btn-sm" onclick="discardAllPreviewQuestions()" style="border-color: #f87171; color: #b91c1c;">
                                    🗑️ Discard All
                                </button>
                                <button type="button" id="btnPublishApproved" class="btn btn-sm" onclick="publishApprovedGeminiQuestions()" style="background: #16a34a; color: #ffffff; border: none; font-weight: 700; padding: 0.5rem 1rem;">
                                    💾 Save Approved Questions to Question Bank
                                </button>
                            </div>
                        </div>

                        <!-- Published Success Banner -->
                        <div id="geminiPublishSuccessBanner" style="display: none; background: #dcfce7; border: 1px solid #86efac; border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1.25rem; color: #15803d; font-size: 0.85rem;">
                            <div style="font-weight: 700; margin-bottom: 0.25rem;">✓ Questions Successfully Published!</div>
                            <div id="geminiPublishSuccessMsg">Approved questions have been verified and saved to the live Firebase question bank. Students can now practice with these questions.</div>
                        </div>

                        <!-- Question List Cards -->
                        <div id="geminiQuestionCardsList"></div>
                    </div>
                </div>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-secondary btn-sm" onclick="closeQuestionBankModal()">Close Window</button>
            </div>
        </div>
    </div>

    <!-- Faculty Proctoring Review & Timeline Modal -->
    <div id="facultyReviewModal" class="modal-overlay">
        <div class="modal-card" style="max-width: 720px;">
            <div class="modal-header">
                <div>
                    <h3 class="modal-title" id="revStudentTitle">Examination Integrity Review</h3>
                    <div style="font-size: 0.8rem; color: var(--text-muted);" id="revAttemptMeta">Attempt ID: -</div>
                </div>
                <button type="button" class="modal-close-btn" onclick="closeFacultyReviewModal()">✕</button>
            </div>

            <div class="modal-body">
                <!-- Metrics Summary Cards -->
                <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.75rem; margin-bottom: 1.5rem;">
                    <div style="background: #f8fafc; border: 1px solid var(--border-subtle); padding: 0.75rem; border-radius: var(--radius-sm); text-align: center;">
                        <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase;">Score</div>
                        <div id="revScoreVal" style="font-size: 1.3rem; font-weight: 800; color: var(--primary);">0%</div>
                    </div>
                    <div style="background: #f8fafc; border: 1px solid var(--border-subtle); padding: 0.75rem; border-radius: var(--radius-sm); text-align: center;">
                        <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase;">Risk Score</div>
                        <div id="revRiskVal" style="font-size: 1.3rem; font-weight: 800;">0/100</div>
                    </div>
                    <div style="background: #f8fafc; border: 1px solid var(--border-subtle); padding: 0.75rem; border-radius: var(--radius-sm); text-align: center;">
                        <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase;">Focus Losses</div>
                        <div id="revFocusVal" style="font-size: 1.3rem; font-weight: 800; color: #b45309;">0</div>
                    </div>
                    <div style="background: #f8fafc; border: 1px solid var(--border-subtle); padding: 0.75rem; border-radius: var(--radius-sm); text-align: center;">
                        <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase;">Cam Drops</div>
                        <div id="revCamVal" style="font-size: 1.3rem; font-weight: 800; color: #dc2626;">0</div>
                    </div>
                </div>

                <!-- Event Timeline -->
                <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 0.75rem; color: var(--text-primary);">
                    Chronological Integrity Audit Log
                </h4>
                <div id="revTimelineContainer" style="max-height: 220px; overflow-y: auto; background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1.5rem; font-size: 0.85rem;">
                    <!-- Rendered by JS -->
                </div>

                <!-- Decision Form -->
                <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 0.75rem; color: var(--text-primary);">
                    Faculty Review Decision & Notes
                </h4>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                    <div>
                        <label class="form-label" style="font-size: 0.8rem;">Review Status</label>
                        <select id="revStatusSelect" class="form-control" style="font-size: 0.85rem;">
                            <option value="VALIDATED">VALIDATED — Satisfactory Integrity</option>
                            <option value="FLAGGED">FLAGGED — Suspected Anomaly (Require Interview)</option>
                            <option value="REVIEWED">REVIEWED — Cleared After Faculty Inspection</option>
                        </select>
                    </div>
                </div>

                <div class="form-group" style="margin-bottom: 0;">
                    <label class="form-label" style="font-size: 0.8rem;">Faculty Review Comments / Reasoning</label>
                    <textarea id="revNotesText" class="form-control" rows="3" placeholder="Enter notes explaining decision (e.g. verified genuine hardware drop, approved score)..." style="font-size: 0.85rem;"></textarea>
                </div>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-secondary btn-sm" onclick="closeFacultyReviewModal()">Cancel</button>
                <button type="button" id="btnSaveReview" class="btn btn-primary btn-sm" onclick="submitFacultyReview()">Save Decision & Update Firebase</button>
            </div>
        </div>
    </div>

    <!-- Script to manage faculty modal interaction -->
    <script>
        let currentReviewAttemptId = null;
        let currentReviewUserId = null;

        function openFacultyReviewModal(attemptId, userId, studentName, category, score, riskLevel, riskScore, focusLost, fsExit, camDrop, copyPaste, status, notes, eventsJson) {
            currentReviewAttemptId = attemptId;
            currentReviewUserId = userId;

            document.getElementById('revStudentTitle').textContent = `Integrity Audit: ${studentName}`;
            document.getElementById('revAttemptMeta').textContent = `${category} • Attempt ID: ${attemptId}`;
            document.getElementById('revScoreVal').textContent = `${score}%`;
            
            const riskValEl = document.getElementById('revRiskVal');
            riskValEl.textContent = `${riskScore}/100`;
            if (riskLevel === 'HIGH') {
                riskValEl.style.color = '#991b1b';
            } else if (riskLevel === 'MODERATE') {
                riskValEl.style.color = '#b45309';
            } else {
                riskValEl.style.color = '#166534';
            }

            document.getElementById('revFocusVal').textContent = focusLost;
            document.getElementById('revCamVal').textContent = camDrop;

            document.getElementById('revStatusSelect').value = status || 'VALIDATED';
            document.getElementById('revNotesText').value = notes || '';

            // Render Timeline
            const timelineEl = document.getElementById('revTimelineContainer');
            timelineEl.innerHTML = '';

            let events = [];
            try {
                events = (typeof eventsJson === 'string') ? JSON.parse(eventsJson) : eventsJson;
            } catch (e) {
                events = [];
            }

            if (!events || events.length === 0) {
                timelineEl.innerHTML = '<div style="color: var(--text-muted); text-align: center; padding: 1.5rem;">No suspicious events or interruptions recorded during this assessment. Clean session.</div>';
            } else {
                events.forEach(ev => {
                    const row = document.createElement('div');
                    row.style.padding = '0.5rem 0';
                    row.style.borderBottom = '1px solid #e2e8f0';
                    row.style.display = 'flex';
                    row.style.alignItems = 'flex-start';
                    row.style.justifyContent = 'space-between';
                    row.style.gap = '1rem';

                    const timeStr = ev.timestamp ? new Date(ev.timestamp).toLocaleTimeString() : 'N/A';
                    let typeBadge = '<span class="badge badge-warning">FOCUS</span>';
                    if (ev.type === 'CAMERA_DISCONNECTED') {
                        typeBadge = '<span class="badge badge-danger">CAMERA</span>';
                    } else if (ev.type === 'FULLSCREEN_EXIT') {
                        typeBadge = '<span class="badge badge-warning">FULLSCREEN</span>';
                    } else if (ev.type === 'CLIPBOARD_ATTEMPT') {
                        typeBadge = '<span class="badge badge-primary">CLIPBOARD</span>';
                    }

                    row.innerHTML = `
                        <div style="flex: 1;">
                            <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.2rem;">
                                ${typeBadge}
                                <strong style="color: var(--text-primary); font-size: 0.85rem;">${ev.type}</strong>
                            </div>
                            <div style="color: var(--text-muted); font-size: 0.8rem;">${ev.details || 'Integrity signal'}</div>
                        </div>
                        <div style="color: var(--text-muted); font-size: 0.75rem; white-space: nowrap;">${timeStr}</div>
                    `;
                    timelineEl.appendChild(row);
                });
            }

            document.getElementById('facultyReviewModal').classList.add('active');
        }

        function closeFacultyReviewModal() {
            document.getElementById('facultyReviewModal').classList.remove('active');
        }

        async function submitFacultyReview() {
            if (!currentReviewAttemptId) return;

            const btn = document.getElementById('btnSaveReview');
            btn.disabled = true;
            btn.textContent = 'Saving decision...';

            const status = document.getElementById('revStatusSelect').value;
            const notes = document.getElementById('revNotesText').value.trim();

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/faculty/review-attempt`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        attemptId: currentReviewAttemptId,
                        userId: currentReviewUserId,
                        status: status,
                        notes: notes
                    })
                });

                const data = await res.json();
                if (data.success) {
                    showToast('Review decision recorded and synchronized to Firebase!', 'success');
                    const badge = document.getElementById(`status-badge-${currentReviewAttemptId}`);
                    if (badge) {
                        badge.textContent = status;
                        badge.className = `badge ${status === 'FLAGGED' ? 'badge-warning' : (status === 'REVIEWED' ? 'badge-success' : 'badge-primary')}`;
                    }
                    closeFacultyReviewModal();
                } else {
                    showToast(data.error || 'Failed to save review.', 'error');
                }
            } catch (err) {
                showToast('Network error while saving review decision.', 'error');
            } finally {
                btn.disabled = false;
                btn.textContent = 'Save Decision & Update Firebase';
            }
        }

        // =============================================================
        // SNJB Placement Test Series: Admin & Faculty Controller Script
        // =============================================================

        let adminCachedTests = [];
        let adminCachedQuestions = [];

        async function loadAdminSnjbTests() {
            const tbody = document.getElementById('adminTestsTableBody');
            if (!tbody) return;

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/tests`);
                const data = await res.json();

                if (data && data.success) {
                    adminCachedTests = data.tests || [];
                    renderAdminTestsTable();
                } else {
                    tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: #dc2626; padding: 2rem;">Failed to load tests: ${data.error || 'Server error'}</td></tr>`;
                }
            } catch (err) {
                tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: #dc2626; padding: 2rem;">Network error loading test management data.</td></tr>`;
            }
        }

        function renderAdminTestsTable() {
            const tbody = document.getElementById('adminTestsTableBody');
            if (!tbody) return;

            if (!adminCachedTests || adminCachedTests.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="8" style="text-align: center; padding: 3rem; color: var(--text-muted);">
                            No tests configured yet. Click "+ Create New Test" to add the first placement test.
                        </td>
                    </tr>
                `;
                return;
            }

            tbody.innerHTML = '';
            adminCachedTests.forEach(t => {
                const tr = document.createElement('tr');

                // Difficulty badge
                let diffBadgeClass = 'badge-success';
                if (t.difficulty === 'Medium') diffBadgeClass = 'badge-warning';
                if (t.difficulty === 'Hard') diffBadgeClass = 'badge-danger';

                // Status badge
                let statusBadgeClass = 'badge-success';
                if (t.status === 'DRAFT') statusBadgeClass = 'badge-warning';
                if (t.status === 'ARCHIVED') statusBadgeClass = 'badge-secondary';

                // Negative marking text
                const negText = t.negativeMarking
                    ? `<span style="color: #dc2626; font-weight: 600;">⚠️ -${t.negativeMarks} Marks</span>`
                    : '<span style="color: #16a34a;">No Negative</span>';

                // Target
                const targetText = `${t.targetBranch || 'ALL'} (${t.targetYear || 'ALL'})`;
                const attemptsText = (t.allowedAttempts === 0) ? 'Unlimited' : `${t.allowedAttempts} max`;

                tr.innerHTML = `
                    <td>
                        <div style="font-weight: 700; color: var(--text-primary); font-size: 0.95rem;">${t.title}</div>
                        <div style="font-size: 0.8rem; color: var(--text-muted); line-height: 1.4; max-width: 260px;">${t.description || 'Campus placement assessment'}</div>
                    </td>
                    <td>
                        <div style="font-weight: 600; font-size: 0.85rem;">${t.category}</div>
                        <div style="margin-top: 0.25rem;">
                            <span class="badge ${diffBadgeClass}" style="font-size: 0.75rem;">${t.difficulty}</span>
                            ${t.subCategory ? `<span style="font-size: 0.75rem; color: var(--text-muted); margin-left: 0.35rem;">${t.subCategory}</span>` : ''}
                        </div>
                    </td>
                    <td>
                        <div style="font-weight: 600;">${t.questionCount} Questions</div>
                        <div style="font-size: 0.8rem; color: var(--text-muted);">${t.totalMarks} Marks (+${t.marksPerQuestion}/q)</div>
                    </td>
                    <td style="font-size: 0.85rem;">${negText}</td>
                    <td style="font-size: 0.85rem; font-weight: 600;">${t.durationMinutes} Mins</td>
                    <td style="font-size: 0.8rem; color: var(--text-secondary);">
                        <div>Attempts: <strong>${attemptsText}</strong></div>
                        <div style="color: var(--text-muted);">${targetText}</div>
                    </td>
                    <td>
                        <span class="badge ${statusBadgeClass}">${t.status}</span>
                    </td>
                    <td style="text-align: right; white-space: nowrap;">
                        <button type="button" class="btn btn-outline btn-sm" onclick="editTest('${t.id}')" title="Edit Parameters">
                            ✏️ Edit
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="duplicateTest('${t.id}')" title="Duplicate Test">
                            📋 Clone
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="toggleTestStatus('${t.id}', '${t.status}')" title="Change Publication Status">
                            ${t.status === 'PUBLISHED' ? '⏸️ Unpublish' : '▶️ Publish'}
                        </button>
                        <button type="button" class="btn btn-outline btn-sm" onclick="deleteTest('${t.id}')" style="color: #dc2626; border-color: #fca5a5;" title="Delete Test">
                            🗑️
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }

        async function loadSnjbAdminAnalytics() {
            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/analytics`);
                const data = await res.json();

                if (data && data.success) {
                    const testsCountEl = document.getElementById('adminStatTestsCount');
                    if (testsCountEl) testsCountEl.textContent = `${data.totalTests || 8} Tests`;

                    const catCountEl = document.getElementById('adminStatCategoriesCount');
                    if (catCountEl) catCountEl.textContent = `${data.totalCategories || 8} Core Categories`;

                    const attemptsCountEl = document.getElementById('adminStatAttemptsCount');
                    if (attemptsCountEl) attemptsCountEl.textContent = `${data.totalAttempts || 0} Attempts`;

                    const batchAvgEl = document.getElementById('adminStatBatchAvg');
                    if (batchAvgEl) batchAvgEl.textContent = `${data.averageScore || 0}%`;

                    const passRateEl = document.getElementById('adminStatBatchPassRate');
                    if (passRateEl) passRateEl.textContent = `${data.passRate || 0}% Pass Rate`;

                    const weakEl = document.getElementById('adminStatWeakTopics');
                    if (weakEl) {
                        if (data.weakTopics && data.weakTopics.length > 0) {
                            const tags = data.weakTopics.map(w => `<span class="badge badge-warning" style="margin-right: 0.35rem; margin-top: 0.25rem;">⚠️ ${w}</span>`).join('');
                            weakEl.innerHTML = tags;
                        } else {
                            weakEl.innerHTML = '<span style="color: #16a34a; font-weight: 600;">✓ Solid cohort performance across tested topics</span>';
                        }
                    }
                }
            } catch (err) {
                console.warn('Error loading SNJB analytics:', err);
            }
        }

        function openCreateTestModal() {
            document.getElementById('editTestId').value = '';
            document.getElementById('testModalTitle').textContent = 'Create New SNJB Placement Test';
            document.getElementById('editTestTitle').value = '';
            document.getElementById('editTestDescription').value = '';
            document.getElementById('editTestCategory').value = 'Quantitative Aptitude';
            document.getElementById('editTestSubCategory').value = '';
            document.getElementById('editTestDifficulty').value = 'Medium';
            document.getElementById('editTestQuestionCount').value = '25';
            document.getElementById('editTestMarksPerQ').value = '2';
            document.getElementById('editTestTotalMarks').value = '50';
            document.getElementById('editTestDuration').value = '25';
            document.getElementById('editTestPassingPct').value = '60';
            document.getElementById('editTestAllowedAttempts').value = '0';
            document.getElementById('editTestTargetBranch').value = 'ALL';
            document.getElementById('editTestTargetYear').value = 'ALL';
            document.getElementById('editTestStatus').value = 'PUBLISHED';
            document.getElementById('editTestNegativeMarking').checked = false;
            document.getElementById('negativeMarksField').style.display = 'none';
            document.getElementById('editTestNegativeDeduction').value = '0.5';
            document.getElementById('editTestRandomize').checked = true;
            document.getElementById('editTestInstructions').value = 'Please ensure you are in a quiet environment with a working webcam. Three security violations will result in automatic submission.';

            document.getElementById('snjbTestModal').classList.add('active');
        }

        function editTest(testId) {
            const test = adminCachedTests.find(t => t.id === testId);
            if (!test) return;

            document.getElementById('editTestId').value = test.id;
            document.getElementById('testModalTitle').textContent = `Edit Test: ${test.title}`;
            document.getElementById('editTestTitle').value = test.title || '';
            document.getElementById('editTestDescription').value = test.description || '';
            document.getElementById('editTestCategory').value = test.category || 'Quantitative Aptitude';
            document.getElementById('editTestSubCategory').value = test.subCategory || '';
            document.getElementById('editTestDifficulty').value = test.difficulty || 'Medium';
            document.getElementById('editTestQuestionCount').value = test.questionCount || 25;
            document.getElementById('editTestMarksPerQ').value = test.marksPerQuestion || 2;
            document.getElementById('editTestTotalMarks').value = test.totalMarks || 50;
            document.getElementById('editTestDuration').value = test.durationMinutes || 25;
            document.getElementById('editTestPassingPct').value = test.passingPercentage || 60;
            document.getElementById('editTestAllowedAttempts').value = test.allowedAttempts || 0;
            document.getElementById('editTestTargetBranch').value = test.targetBranch || 'ALL';
            document.getElementById('editTestTargetYear').value = test.targetYear || 'ALL';
            document.getElementById('editTestStatus').value = test.status || 'PUBLISHED';
            document.getElementById('editTestNegativeMarking').checked = !!test.negativeMarking;
            document.getElementById('negativeMarksField').style.display = test.negativeMarking ? 'flex' : 'none';
            document.getElementById('editTestNegativeDeduction').value = test.negativeMarks || 0.5;
            document.getElementById('editTestRandomize').checked = (test.randomizeQuestions !== false);
            document.getElementById('editTestInstructions').value = test.instructions || '';

            document.getElementById('snjbTestModal').classList.add('active');
        }

        function closeTestModal() {
            document.getElementById('snjbTestModal').classList.remove('active');
        }

        function updateTotalMarksCalc() {
            const count = parseInt(document.getElementById('editTestQuestionCount').value) || 25;
            const marksPerQ = parseInt(document.getElementById('editTestMarksPerQ').value) || 2;
            document.getElementById('editTestTotalMarks').value = count * marksPerQ;
        }

        function toggleNegativeMarkingInput() {
            const checked = document.getElementById('editTestNegativeMarking').checked;
            document.getElementById('negativeMarksField').style.display = checked ? 'flex' : 'none';
        }

        async function handleSaveTest(e) {
            e.preventDefault();
            const submitBtn = document.getElementById('btnSaveTestSubmit');
            submitBtn.disabled = true;
            submitBtn.textContent = 'Saving Test Configuration...';

            const payload = {
                id: document.getElementById('editTestId').value.trim(),
                title: document.getElementById('editTestTitle').value.trim(),
                description: document.getElementById('editTestDescription').value.trim(),
                category: document.getElementById('editTestCategory').value,
                subCategory: document.getElementById('editTestSubCategory').value.trim(),
                difficulty: document.getElementById('editTestDifficulty').value,
                questionCount: parseInt(document.getElementById('editTestQuestionCount').value) || 25,
                marksPerQuestion: parseInt(document.getElementById('editTestMarksPerQ').value) || 2,
                totalMarks: parseInt(document.getElementById('editTestTotalMarks').value) || 50,
                durationMinutes: parseInt(document.getElementById('editTestDuration').value) || 25,
                passingPercentage: parseInt(document.getElementById('editTestPassingPct').value) || 60,
                allowedAttempts: parseInt(document.getElementById('editTestAllowedAttempts').value) || 0,
                targetBranch: document.getElementById('editTestTargetBranch').value,
                targetYear: document.getElementById('editTestTargetYear').value,
                status: document.getElementById('editTestStatus').value,
                negativeMarking: document.getElementById('editTestNegativeMarking').checked,
                negativeMarks: parseFloat(document.getElementById('editTestNegativeDeduction').value) || 0.5,
                randomizeQuestions: document.getElementById('editTestRandomize').checked,
                instructions: document.getElementById('editTestInstructions').value.trim()
            };

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/tests/save`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const data = await res.json();
                if (data.success) {
                    showToast('Test configuration saved and synchronized to Firebase!', 'success');
                    closeTestModal();
                    loadAdminSnjbTests();
                    loadSnjbAdminAnalytics();
                } else {
                    showToast(data.error || 'Failed to save test.', 'error');
                }
            } catch (err) {
                showToast('Network error saving test configuration.', 'error');
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Save Test Configuration';
            }
        }

        async function duplicateTest(testId) {
            if (!confirm('Clone this test and create a duplicate draft copy?')) return;

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/tests/${testId}/duplicate`, {
                    method: 'POST'
                });
                const data = await res.json();
                if (data.success) {
                    showToast('Test duplicated successfully!', 'success');
                    loadAdminSnjbTests();
                } else {
                    showToast(data.error || 'Failed to duplicate test.', 'error');
                }
            } catch (e) {
                showToast('Network error duplicating test.', 'error');
            }
        }

        async function toggleTestStatus(testId, currentStatus) {
            const newStatus = (currentStatus === 'PUBLISHED') ? 'DRAFT' : 'PUBLISHED';
            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/tests/${testId}/status`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status: newStatus })
                });
                const data = await res.json();
                if (data.success) {
                    showToast(`Test status updated to ${newStatus}`, 'success');
                    loadAdminSnjbTests();
                } else {
                    showToast(data.error || 'Failed to update status.', 'error');
                }
            } catch (e) {
                showToast('Network error updating status.', 'error');
            }
        }

        async function deleteTest(testId) {
            if (!confirm('Are you sure you want to delete this test from the SNJB Placement Series? This cannot be undone.')) return;

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/tests/${testId}`, {
                    method: 'DELETE'
                });
                const data = await res.json();
                if (data.success) {
                    showToast('Test deleted successfully.', 'success');
                    loadAdminSnjbTests();
                    loadSnjbAdminAnalytics();
                } else {
                    showToast(data.error || 'Failed to delete test.', 'error');
                }
            } catch (e) {
                showToast('Network error deleting test.', 'error');
            }
        }

        // =============================================================
        // Question Bank & CSV Import
        // =============================================================

        function openQuestionBankModal() {
            document.getElementById('snjbQuestionBankModal').classList.add('active');
            switchQBankTab('single');
            loadQuestionBankQuestions();
        }

        function openGeminiGenerationModal(defaultTopic, defaultCategory) {
            document.getElementById('snjbQuestionBankModal').classList.add('active');
            switchQBankTab('gemini');
            if (defaultCategory) {
                const sec = document.getElementById('geminiGenSection');
                if (sec) sec.value = defaultCategory;
            }
            if (defaultTopic) {
                const top = document.getElementById('geminiGenTopic');
                if (top) top.value = defaultTopic;
            }
            onGeminiSectionChange();
            checkGeminiStatus();
        }

        function closeQuestionBankModal() {
            document.getElementById('snjbQuestionBankModal').classList.remove('active');
        }

        function switchQBankTab(tab) {
            document.getElementById('qBankSinglePane').style.display = (tab === 'single') ? 'block' : 'none';
            document.getElementById('qBankCsvPane').style.display = (tab === 'csv') ? 'block' : 'none';
            document.getElementById('qBankBrowsePane').style.display = (tab === 'browse') ? 'block' : 'none';
            const geminiPane = document.getElementById('qBankGeminiPane');
            if (geminiPane) geminiPane.style.display = (tab === 'gemini') ? 'block' : 'none';

            const btnSingle = document.getElementById('tabBtnAddSingle');
            const btnCsv = document.getElementById('tabBtnImportCsv');
            const btnBrowse = document.getElementById('tabBtnBrowseBank');
            const btnGemini = document.getElementById('tabBtnGeminiGen');

            if (btnSingle) {
                btnSingle.style.borderBottom = (tab === 'single') ? '2px solid var(--primary)' : 'none';
                btnSingle.style.color = (tab === 'single') ? 'var(--primary)' : 'var(--text-secondary)';
            }

            if (btnCsv) {
                btnCsv.style.borderBottom = (tab === 'csv') ? '2px solid var(--primary)' : 'none';
                btnCsv.style.color = (tab === 'csv') ? 'var(--primary)' : 'var(--text-secondary)';
            }

            if (btnBrowse) {
                btnBrowse.style.borderBottom = (tab === 'browse') ? '2px solid var(--primary)' : 'none';
                btnBrowse.style.color = (tab === 'browse') ? 'var(--primary)' : 'var(--text-secondary)';
            }

            if (btnGemini) {
                btnGemini.style.borderBottom = (tab === 'gemini') ? '2px solid #7c3aed' : 'none';
                btnGemini.style.color = (tab === 'gemini') ? '#7c3aed' : 'var(--text-secondary)';
            }

            if (tab === 'gemini') {
                checkGeminiStatus();
            }
        }

        async function handleSaveSingleQuestion(e) {
            e.preventDefault();
            const btn = document.getElementById('btnSaveQuestion');
            btn.disabled = true;
            btn.textContent = 'Saving Question...';

            const payload = {
                category: document.getElementById('qCategory').value,
                topic: document.getElementById('qTopic').value.trim(),
                difficulty: document.getElementById('qDifficulty').value,
                question: document.getElementById('qText').value.trim(),
                optionA: document.getElementById('qOptionA').value.trim(),
                optionB: document.getElementById('qOptionB').value.trim(),
                optionC: document.getElementById('qOptionC').value.trim(),
                optionD: document.getElementById('qOptionD').value.trim(),
                correctOption: parseInt(document.getElementById('qCorrectOption').value) || 0,
                marks: parseInt(document.getElementById('qMarks').value) || 2,
                explanation: document.getElementById('qExplanation').value.trim()
            };

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/questions/save`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const data = await res.json();
                if (data.success) {
                    showToast('Question saved to repository successfully!', 'success');
                    document.getElementById('singleQuestionForm').reset();
                    loadQuestionBankQuestions();
                } else {
                    showToast(data.error || 'Failed to save question.', 'error');
                }
            } catch (err) {
                showToast('Network error saving question.', 'error');
            } finally {
                btn.disabled = false;
                btn.textContent = 'Add Question to Repository';
            }
        }

        function downloadCsvTemplate() {
            const csvContent = "category,topic,difficulty,question,optionA,optionB,optionC,optionD,correctOption,marks,explanation\n" +
                "Programming and Coding,Java OOP,Medium,\"Which keyword prevents a Java method from being overridden in subclasses?\",abstract,static,final,synchronized,C,2,\"The final keyword applied to a method prevents method overriding in subclasses.\"\n" +
                "Core Computer Science,DBMS Normalization,Medium,\"Which normal form eliminates partial functional dependency on composite candidate keys?\",1NF,2NF,3NF,BCNF,B,2,\"Second Normal Form (2NF) mandates 1NF and no non-prime attribute is partially dependent on any candidate key.\"\n";

            const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.setAttribute('download', 'snjb_questions_template.csv');
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        async function handleUploadCsv(e) {
            e.preventDefault();
            const fileInput = document.getElementById('csvFileInput');
            if (!fileInput.files || !fileInput.files[0]) {
                alert('Please select a CSV file first.');
                return;
            }

            const btn = document.getElementById('btnUploadCsv');
            btn.disabled = true;
            btn.textContent = 'Validating and Ingesting CSV...';

            const resultBox = document.getElementById('csvUploadResult');
            resultBox.style.display = 'none';

            const formData = new FormData();
            formData.append('file', fileInput.files[0]);

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/questions/import-csv`, {
                    method: 'POST',
                    body: formData
                });
                const data = await res.json();

                resultBox.style.display = 'block';
                if (data.success) {
                    resultBox.style.background = '#dcfce7';
                    resultBox.style.color = '#15803d';
                    resultBox.style.border = '1px solid #86efac';
                    resultBox.innerHTML = `<strong>✓ Ingestion Successful!</strong> Successfully imported and verified ${data.importedCount} questions into the repository.`;
                    fileInput.value = '';
                    loadQuestionBankQuestions();
                    loadAdminSnjbTests();
                } else {
                    resultBox.style.background = '#fee2e2';
                    resultBox.style.color = '#991b1b';
                    resultBox.style.border = '1px solid #fca5a5';
                    resultBox.innerHTML = `<strong>⚠️ Ingestion Error:</strong> ${data.error || 'Failed to process CSV file.'}`;
                }
            } catch (err) {
                resultBox.style.display = 'block';
                resultBox.style.background = '#fee2e2';
                resultBox.style.color = '#991b1b';
                resultBox.style.border = '1px solid #fca5a5';
                resultBox.innerHTML = '<strong>⚠️ Network Error:</strong> Could not transmit CSV file to server.';
            } finally {
                btn.disabled = false;
                btn.textContent = 'Upload and Validate Questions';
            }
        }

        async function loadQuestionBankQuestions() {
            const tbody = document.getElementById('qBankBrowseTableBody');
            const totalEl = document.getElementById('qBankTotalCount');
            if (!tbody) return;

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/snjb/admin/questions`);
                const data = await res.json();
                if (data && data.success) {
                    adminCachedQuestions = data.questions || [];
                    if (totalEl) totalEl.textContent = adminCachedQuestions.length;
                    renderQBankTable(adminCachedQuestions);
                }
            } catch (e) {
                console.warn('Error loading questions:', e);
            }
        }

        function renderQBankTable(questions) {
            const tbody = document.getElementById('qBankBrowseTableBody');
            if (!tbody) return;

            if (!questions || questions.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 2rem;">No questions found in repository matching criteria.</td></tr>';
                return;
            }

            tbody.innerHTML = '';
            questions.forEach((q, idx) => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>
                        <div style="font-weight: 600; color: var(--text-primary); line-height: 1.4;">${q.question}</div>
                        <div style="font-size: 0.75rem; color: #16a34a; margin-top: 0.25rem;">
                            ✓ Correct: Option ${String.fromCharCode(65 + q.correctOption)}
                        </div>
                    </td>
                    <td>
                        <div>${q.category}</div>
                        <div style="font-size: 0.75rem; color: var(--text-muted);">${q.topic || 'General'}</div>
                    </td>
                    <td><span class="badge ${q.difficulty === 'Hard' ? 'badge-danger' : (q.difficulty === 'Easy' ? 'badge-success' : 'badge-warning')}">${q.difficulty}</span></td>
                    <td>
                        <button type="button" class="btn btn-outline btn-sm" onclick="alert('Verified solution: ' + ${JSON.stringify(q.explanation || 'Verified correct answer.')})">
                            💡 Solution
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }

        function filterQBankTable() {
            const query = (document.getElementById('qBankSearchInput').value || '').toLowerCase().trim();
            if (!query) {
                renderQBankTable(adminCachedQuestions);
                return;
            }

            const filtered = adminCachedQuestions.filter(q => {
                return (q.question || '').toLowerCase().includes(query) ||
                    (q.topic || '').toLowerCase().includes(query) ||
                    (q.category || '').toLowerCase().includes(query);
            });
            renderQBankTable(filtered);
        }

        // =============================================================
        // Google Gemini AI Question Generation & Publishing Engine
        // =============================================================

        let geminiPreviewQuestions = [];
        let geminiTimerInterval = null;
        let geminiStartTime = 0;

        function setGeminiTopic(topic, section) {
            const topicInput = document.getElementById('geminiGenTopic');
            const secSelect = document.getElementById('geminiGenSection');
            if (topicInput) topicInput.value = topic;
            if (secSelect && section) secSelect.value = section;
            onGeminiSectionChange();
        }

        function onGeminiSectionChange() {
            const sec = document.getElementById('geminiGenSection').value;
            const chips = document.getElementById('geminiPhase1TopicChips');
            const langRow = document.getElementById('geminiLanguageRow');
            if (chips) {
                chips.style.display = (sec === 'Quantitative Aptitude') ? 'block' : 'none';
            }
            if (langRow) {
                langRow.style.display = (sec === 'Technical and Coding') ? 'block' : 'none';
            }
        }

        function onGeminiTypeChange() {
            const type = document.getElementById('geminiGenType').value;
            const langRow = document.getElementById('geminiLanguageRow');
            if (langRow && type === 'CODING') {
                langRow.style.display = 'block';
            }
        }

        async function checkGeminiStatus() {
            const titleEl = document.getElementById('geminiStatusTitle');
            const subtitleEl = document.getElementById('geminiStatusSubtitle');
            const badgeEl = document.getElementById('geminiStatusBadge');
            if (!titleEl) return;

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/admin/questions/gemini/status`);
                if (res.ok) {
                    const data = await res.json();
                    if (data && data.configured) {
                        titleEl.textContent = '✓ Google Gemini AI Engine Active & Connected';
                        titleEl.style.color = '#15803d';
                        subtitleEl.textContent = `Model: ${data.model || 'gemini-1.5-flash'} | ${data.activeTrackerDuplicatesDetected || 0} duplicates intercepted | Key: ${data.maskedKey || 'Configured'}`;
                        badgeEl.innerHTML = '<span class="badge" style="background: #dcfce7; color: #166534; font-weight: 700;">API Ready</span>';
                    } else {
                        titleEl.textContent = '⚠️ Google Gemini API Key Missing';
                        titleEl.style.color = '#b45309';
                        subtitleEl.textContent = 'Set GEMINI_API_KEY environment variable. Student assessments will serve verified Firebase question bank.';
                        badgeEl.innerHTML = '<span class="badge" style="background: #fef3c7; color: #92400e; font-weight: 700;">Key Not Set</span>';
                    }
                }
            } catch (err) {
                console.warn('Error checking Gemini API status:', err);
            }
        }

        async function adminRunGeminiGeneration(e) {
            e.preventDefault();
            const submitBtn = document.getElementById('btnRunGeminiGen');
            const loadingBox = document.getElementById('geminiGenLoading');
            const errorBox = document.getElementById('geminiErrorBanner');
            const previewBox = document.getElementById('geminiPreviewContainer');
            const successBox = document.getElementById('geminiPublishSuccessBanner');
            const timerEl = document.getElementById('geminiElapsedTimer');

            if (errorBox) errorBox.style.display = 'none';
            if (previewBox) previewBox.style.display = 'none';
            if (successBox) successBox.style.display = 'none';
            if (loadingBox) loadingBox.style.display = 'block';

            submitBtn.disabled = true;
            submitBtn.textContent = 'Contacting Gemini AI...';

            geminiStartTime = Date.now();
            if (geminiTimerInterval) clearInterval(geminiTimerInterval);
            geminiTimerInterval = setInterval(() => {
                const secs = Math.floor((Date.now() - geminiStartTime) / 1000);
                if (timerEl) timerEl.textContent = `${secs}s`;
            }, 1000);

            const payload = {
                section: document.getElementById('geminiGenSection').value,
                topic: document.getElementById('geminiGenTopic').value.trim(),
                difficulty: document.getElementById('geminiGenDifficulty').value,
                count: parseInt(document.getElementById('geminiGenCount').value) || 10,
                questionType: document.getElementById('geminiGenType').value,
                language: document.getElementById('geminiGenLanguage') ? document.getElementById('geminiGenLanguage').value : 'Java'
            };

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/admin/questions/generate`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const data = await res.json();

                if (loadingBox) loadingBox.style.display = 'none';
                clearInterval(geminiTimerInterval);

                if (data && data.success && data.questions && data.questions.length > 0) {
                    geminiPreviewQuestions = data.questions;
                    renderGeminiPreviewCards();
                    if (previewBox) {
                        previewBox.style.display = 'block';
                        previewBox.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    }
                } else {
                    if (errorBox) {
                        errorBox.style.display = 'block';
                        document.getElementById('geminiErrorTitle').textContent = '⚠️ Question Generation Alert';
                        document.getElementById('geminiErrorMessage').textContent = data.error || 'Failed to generate questions. Check API key configuration or try again.';
                    }
                }
            } catch (err) {
                if (loadingBox) loadingBox.style.display = 'none';
                clearInterval(geminiTimerInterval);
                if (errorBox) {
                    errorBox.style.display = 'block';
                    document.getElementById('geminiErrorTitle').textContent = '⚠️ Network Error';
                    document.getElementById('geminiErrorMessage').textContent = 'Could not communicate with the question generation service.';
                }
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = '✨ Generate with Gemini AI';
            }
        }

        function renderGeminiPreviewCards() {
            const listContainer = document.getElementById('geminiQuestionCardsList');
            const countEl = document.getElementById('geminiPreviewCount');
            if (!listContainer) return;

            if (countEl) countEl.textContent = geminiPreviewQuestions.length;
            listContainer.innerHTML = '';

            if (geminiPreviewQuestions.length === 0) {
                listContainer.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 2rem;">No questions remaining in preview. You can run generation again above.</div>';
                return;
            }

            geminiPreviewQuestions.forEach((q, idx) => {
                const card = document.createElement('div');
                card.className = 'gemini-preview-item';
                card.id = `previewCard_${idx}`;

                const isMcq = (q.questionType !== 'CODING');
                const diffBadgeClass = q.difficulty === 'Hard' ? 'badge-danger' : (q.difficulty === 'Easy' ? 'badge-success' : 'badge-warning');

                let optionsHtml = '';
                if (isMcq && q.options && q.options.length >= 4) {
                    optionsHtml = `
                        <div style="font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); margin-bottom: 0.4rem;">
                            Answer Options (Select the verified correct radio choice):
                        </div>
                        <div class="gemini-opt-grid">
                            ${q.options.map((opt, optIdx) => `
                                <div class="gemini-opt-box ${q.correctOption === optIdx ? 'correct' : ''}" id="optBox_${idx}_${optIdx}">
                                    <input type="radio" name="previewCorrect_${idx}" id="radio_${idx}_${optIdx}" ${q.correctOption === optIdx ? 'checked' : ''} onchange="updatePreviewCorrectOpt(${idx}, ${optIdx})" style="cursor: pointer;">
                                    <label for="radio_${idx}_${optIdx}" style="font-weight: 700; margin: 0 0.35rem 0 0; cursor: pointer; color: ${q.correctOption === optIdx ? '#166534' : 'var(--text-primary)'};">
                                        ${String.fromCharCode(65 + optIdx)}:
                                    </label>
                                    <input type="text" class="form-control" value="${escapeHtml(opt)}" onchange="updatePreviewOptionText(${idx}, ${optIdx}, this.value)" style="font-size: 0.85rem; padding: 0.25rem 0.5rem; flex: 1;">
                                </div>
                            `).join('')}
                        </div>
                    `;
                } else if (q.codingProblem) {
                    const cp = q.codingProblem;
                    optionsHtml = `
                        <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.75rem; margin: 0.75rem 0; font-size: 0.85rem;">
                            <div style="font-weight: 700; color: var(--text-primary); margin-bottom: 0.25rem;">Problem Title: ${escapeHtml(cp.title || '')}</div>
                            <div style="color: var(--text-secondary); margin-bottom: 0.5rem;">${escapeHtml(cp.problemStatement || '')}</div>
                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; font-size: 0.8rem; color: #475569;">
                                <div><strong>Input Format:</strong> ${escapeHtml(cp.inputFormat || '')}</div>
                                <div><strong>Output Format:</strong> ${escapeHtml(cp.outputFormat || '')}</div>
                            </div>
                        </div>
                    `;
                }

                card.innerHTML = `
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; flex-wrap: wrap; gap: 0.5rem;">
                        <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                            <span style="font-weight: 800; font-size: 0.95rem; color: var(--primary);">#${idx + 1}</span>
                            <span class="badge ${diffBadgeClass}">${q.difficulty || 'Medium'}</span>
                            <span class="badge badge-outline" style="font-size: 0.75rem;">${q.topic || 'General'}</span>
                            <span class="gemini-validation-badge valid">
                                ✓ ${q.validationNotes || 'Verified Quality'}
                            </span>
                        </div>
                        <div style="display: flex; gap: 0.5rem;">
                            <button type="button" class="btn btn-outline btn-sm" onclick="discardPreviewQuestion(${idx})" style="border-color: #fca5a5; color: #dc2626; padding: 0.2rem 0.5rem; font-size: 0.75rem;">
                                🗑️ Discard
                            </button>
                        </div>
                    </div>

                    <div style="margin-bottom: 0.75rem;">
                        <label class="form-label" style="font-size: 0.8rem; font-weight: 700;">Question Statement (Editable)</label>
                        <textarea class="form-control" rows="2" onchange="updatePreviewQuestionText(${idx}, this.value)" style="font-size: 0.85rem; font-family: inherit;">${escapeHtml(q.question || q.questionText || '')}</textarea>
                    </div>

                    ${optionsHtml}

                    <div style="margin-top: 0.75rem;">
                        <label class="form-label" style="font-size: 0.8rem; font-weight: 700; color: #0369a1;">Verified Explanation & Step-by-Step Derivation</label>
                        <textarea class="form-control" rows="2" onchange="updatePreviewExplanation(${idx}, this.value)" style="font-size: 0.85rem; background: #f0f9ff; border-color: #bae6fd;">${escapeHtml(q.explanation || '')}</textarea>
                    </div>
                `;
                listContainer.appendChild(card);
            });
        }

        function escapeHtml(str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        function updatePreviewQuestionText(idx, val) {
            if (geminiPreviewQuestions[idx]) {
                geminiPreviewQuestions[idx].question = val;
                geminiPreviewQuestions[idx].questionText = val;
            }
        }

        function updatePreviewOptionText(idx, optIdx, val) {
            if (geminiPreviewQuestions[idx] && geminiPreviewQuestions[idx].options) {
                geminiPreviewQuestions[idx].options[optIdx] = val;
            }
        }

        function updatePreviewCorrectOpt(idx, correctIdx) {
            if (geminiPreviewQuestions[idx]) {
                geminiPreviewQuestions[idx].correctOption = correctIdx;
                for (let o = 0; o < 4; o++) {
                    const box = document.getElementById(`optBox_${idx}_${o}`);
                    if (box) {
                        if (o === correctIdx) {
                            box.classList.add('correct');
                        } else {
                            box.classList.remove('correct');
                        }
                    }
                }
            }
        }

        function updatePreviewExplanation(idx, val) {
            if (geminiPreviewQuestions[idx]) {
                geminiPreviewQuestions[idx].explanation = val;
            }
        }

        function discardPreviewQuestion(idx) {
            geminiPreviewQuestions.splice(idx, 1);
            renderGeminiPreviewCards();
        }

        function discardAllPreviewQuestions() {
            if (confirm('Discard all previewed questions?')) {
                geminiPreviewQuestions = [];
                const previewBox = document.getElementById('geminiPreviewContainer');
                if (previewBox) previewBox.style.display = 'none';
            }
        }

        async function publishApprovedGeminiQuestions() {
            if (!geminiPreviewQuestions || geminiPreviewQuestions.length === 0) {
                alert('No questions in preview to publish.');
                return;
            }

            // Sanity validation: ensure all questions have statements and options
            for (let i = 0; i < geminiPreviewQuestions.length; i++) {
                const q = geminiPreviewQuestions[i];
                const text = q.question || q.questionText;
                if (!text || text.trim().length === 0) {
                    alert(`Question #${i + 1} has an empty question statement. Please fix or discard it.`);
                    return;
                }
                if (q.questionType !== 'CODING' && q.options) {
                    if (q.options.length < 4 || q.options.some(o => !o || o.trim().length === 0)) {
                        alert(`Question #${i + 1} has incomplete options. All 4 choices must be non-empty.`);
                        return;
                    }
                }
            }

            const publishBtn = document.getElementById('btnPublishApproved');
            publishBtn.disabled = true;
            publishBtn.textContent = 'Saving to Question Bank...';

            try {
                const res = await fetch(`${window.CONTEXT_PATH}/api/admin/questions/publish`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ questions: geminiPreviewQuestions })
                });

                const data = await res.json();
                if (data && data.success) {
                    const successBanner = document.getElementById('geminiPublishSuccessBanner');
                    const successMsg = document.getElementById('geminiPublishSuccessMsg');
                    if (successBanner) {
                        successBanner.style.display = 'block';
                        successMsg.textContent = `✓ Successfully verified and saved ${data.publishedCount || geminiPreviewQuestions.length} questions into the live question bank! Students taking practice tests will now benefit from these questions.`;
                        successBanner.scrollIntoView({ behavior: 'smooth' });
                    }
                    geminiPreviewQuestions = [];
                    setTimeout(() => {
                        const previewBox = document.getElementById('geminiPreviewContainer');
                        if (previewBox) previewBox.style.display = 'none';
                        loadQuestionBankQuestions();
                        loadAdminSnjbTests();
                    }, 2500);
                } else {
                    alert(data.error || 'Failed to publish questions.');
                }
            } catch (err) {
                alert('Network error while saving approved questions.');
            } finally {
                publishBtn.disabled = false;
                publishBtn.textContent = '💾 Save Approved Questions to Question Bank';
            }
        }

        function downloadSnjbResultsCsv() {
            window.location.href = `${window.CONTEXT_PATH}/api/snjb/admin/export-results`;
        }

        // Initialize SNJB Test Management on Load
        document.addEventListener('DOMContentLoaded', () => {
            loadAdminSnjbTests();
            loadSnjbAdminAnalytics();

            if (window.location.hash === '#gemini-gen' || sessionStorage.getItem('openGeminiGen') === 'true') {
                sessionStorage.removeItem('openGeminiGen');
                setTimeout(() => openGeminiGenerationModal(), 200);
            }
        });
    </script>
</body>
</html>
