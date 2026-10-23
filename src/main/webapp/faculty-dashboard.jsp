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
    </script>
</body>
</html>
