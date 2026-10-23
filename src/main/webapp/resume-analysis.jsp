<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.ResumeData" %>
<%@ page import="com.tpobuddy.model.User" %>
<%
    ResumeData resume = (ResumeData) request.getAttribute("resume");
    User user = (User) request.getAttribute("user");
    String successMsg = (String) request.getAttribute("successMessage");
    String errorMsg = (String) request.getAttribute("errorMessage");
    int atsScore = (resume != null) ? resume.getAtsScore() : 0;
    int grammarScore = (resume != null) ? resume.getGrammarScore() : 0;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>AI Resume Analysis — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
</head>
<body>
    <div class="bg-grid-overlay"></div>

    <div class="app-shell">
        <jsp:include page="/WEB-INF/fragments/sidebar.jsp" />

        <main class="app-main">
            <div class="page-header">
                <div>
                    <h1 class="page-title">AI Resume Analysis & ATS Scanner</h1>
                    <p class="page-subtitle">Instant PDF keyword audit, structural evaluation, and placement skill gap detection.</p>
                </div>
                <div>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <% if (successMsg != null) { %>
                <div class="toast toast-success" style="position: static; margin-bottom: 1.5rem; animation: none;">
                    <span>✓</span>
                    <div><%= successMsg %></div>
                </div>
            <% } %>

            <% if (errorMsg != null) { %>
                <div class="toast toast-error" style="position: static; margin-bottom: 1.5rem; animation: none;">
                    <span>⚠</span>
                    <div><%= errorMsg %></div>
                </div>
            <% } %>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 2rem;">
                <!-- Upload & Text Card -->
                <div class="glass-card" style="padding: 2rem;">
                    <h3 style="margin-bottom: 1rem; font-size: 1.3rem;">Upload Resume (PDF / Text)</h3>
                    <p style="font-size: 0.9rem; color: var(--text-muted); margin-bottom: 1.5rem;">
                        Our backend extracts text using Apache PDFBox, checks for ATS keyword matches, and syncs directly with your Firebase profile.
                    </p>

                    <form action="${pageContext.request.contextPath}/resume" method="post" enctype="multipart/form-data">
                        <div class="form-group">
                            <label class="form-label">Upload Resume PDF</label>
                            <div style="border: 2px dashed #93c5fd; border-radius: var(--radius-md); padding: 2rem; text-align: center; background: #f8fafc; transition: all 0.2s ease;">
                                <div style="font-size: 2.2rem; margin-bottom: 0.75rem;">📁</div>
                                <div style="font-size: 0.95rem; font-weight: 600; color: var(--text-primary); margin-bottom: 0.25rem;">Choose a PDF file to upload</div>
                                <div style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 1rem;">PDF files up to 10MB supported</div>
                                <input type="file" name="resumeFile" id="resumeFile" accept="application/pdf" style="display: none;" onchange="updateFileLabel(this)">
                                <button type="button" class="btn btn-secondary btn-sm" onclick="document.getElementById('resumeFile').click()">Browse Files</button>
                                <div id="fileSelectedLabel" style="margin-top: 0.75rem; font-size: 0.85rem; color: var(--secondary); font-weight: 500;"></div>
                            </div>
                        </div>

                        <div class="form-group" style="margin-top: 1.5rem;">
                            <label for="resumeText" class="form-label">Or Paste Resume Text</label>
                            <textarea id="resumeText" name="resumeText" class="form-control" rows="8" placeholder="Paste full resume text here (Education, Experience, Projects, Skills)..."><%= (resume != null && resume.getResumeText() != null) ? resume.getResumeText() : "" %></textarea>
                        </div>

                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                            <div class="form-group">
                                <label for="experience" class="form-label">Internship / Experience Summary</label>
                                <input type="text" id="experience" name="experience" class="form-control" placeholder="Software Engineering Intern (6 mos)" value="<%= (resume != null && resume.getExperience() != null) ? resume.getExperience() : "" %>">
                            </div>
                            <div class="form-group">
                                <label for="projects" class="form-label">Key Projects Highlight</label>
                                <input type="text" id="projects" name="projects" class="form-control" placeholder="TPO Buddy AI, E-Commerce" value="<%= (resume != null && resume.getProjects() != null) ? resume.getProjects() : "" %>">
                            </div>
                        </div>

                        <button type="submit" class="btn btn-primary" style="width: 100%; padding: 0.85rem; margin-top: 0.5rem;">
                            <span>Start ATS Resume Analysis</span>
                            <span>→</span>
                        </button>
                    </form>
                </div>

                <!-- Analysis Results Card -->
                <div class="glass-card" style="padding: 2rem;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                        <h3 style="font-size: 1.3rem;">ATS Analysis Report</h3>
                        <span class="badge <%= atsScore >= 80 ? "badge-success" : (atsScore > 0 ? "badge-warning" : "badge-primary") %>">
                            <%= atsScore >= 80 ? "ATS COMPLIANT" : (atsScore > 0 ? "NEEDS OPTIMIZATION" : "PENDING UPLOAD") %>
                        </span>
                    </div>

                    <% if (resume != null && atsScore > 0) { %>
                        <!-- Score Row -->
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem;">
                            <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1.25rem; text-align: center;">
                                <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Overall ATS Match</div>
                                <div style="font-family: var(--font-heading); font-size: 2.75rem; font-weight: 800; color: var(--primary);"><%= atsScore %>%</div>
                                <div style="font-size: 0.8rem; color: var(--text-muted);">Pass threshold: 75%</div>
                            </div>
                            <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1.25rem; text-align: center;">
                                <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Grammar & Syntax</div>
                                <div style="font-family: var(--font-heading); font-size: 2.75rem; font-weight: 800; color: var(--accent-emerald);"><%= grammarScore %>%</div>
                                <div style="font-size: 0.8rem; color: var(--text-muted);">Structural readability</div>
                            </div>
                        </div>

                        <!-- Detected Skills -->
                        <div style="margin-bottom: 1.5rem;">
                            <div style="font-size: 0.85rem; font-weight: 600; text-transform: uppercase; color: var(--text-muted); margin-bottom: 0.75rem;">Detected Skills</div>
                            <div style="display: flex; flex-wrap: wrap; gap: 0.5rem;">
                                <% if (resume.getDetectedSkills() != null && !resume.getDetectedSkills().isEmpty()) {
                                    for (String s : resume.getDetectedSkills()) { %>
                                        <span class="badge badge-success"><%= s %></span>
                                <%  }
                                } else { %>
                                    <span style="font-size: 0.85rem; color: var(--text-muted);">No key skills detected.</span>
                                <% } %>
                            </div>
                        </div>

                        <!-- Missing High-Impact Skills -->
                        <div style="margin-bottom: 1.5rem;">
                            <div style="font-size: 0.85rem; font-weight: 600; text-transform: uppercase; color: var(--text-muted); margin-bottom: 0.75rem;">Recommended Missing Skills</div>
                            <div style="display: flex; flex-wrap: wrap; gap: 0.5rem;">
                                <% if (resume.getMissingSkills() != null && !resume.getMissingSkills().isEmpty()) {
                                    for (String s : resume.getMissingSkills()) { %>
                                        <span class="badge badge-warning">⚠ <%= s %></span>
                                <%  }
                                } else { %>
                                    <span class="badge badge-success">✓ Comprehensive skill coverage</span>
                                <% } %>
                            </div>
                        </div>

                        <!-- Section Reviews -->
                        <div style="margin-bottom: 1.5rem; background: #f8fafc; padding: 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                            <div style="font-weight: 600; color: var(--text-primary); font-size: 0.9rem; margin-bottom: 0.25rem;">Experience Review:</div>
                            <div style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 0.75rem;"><%= resume.getExperienceReview() %></div>
                            <div style="font-weight: 600; color: var(--text-primary); font-size: 0.9rem; margin-bottom: 0.25rem;">Projects Review:</div>
                            <div style="font-size: 0.85rem; color: var(--text-secondary);"><%= resume.getProjectsReview() %></div>
                        </div>

                        <!-- Improvement Suggestions -->
                        <div>
                            <div style="font-size: 0.85rem; font-weight: 600; text-transform: uppercase; color: var(--text-muted); margin-bottom: 0.75rem;">Actionable ATS Recommendations</div>
                            <ul style="list-style: none; display: flex; flex-direction: column; gap: 0.5rem;">
                                <% if (resume.getSuggestions() != null) {
                                    for (String sug : resume.getSuggestions()) { %>
                                        <li style="font-size: 0.85rem; color: var(--text-secondary); display: flex; align-items: flex-start; gap: 0.5rem;">
                                            <span style="color: var(--secondary);">•</span>
                                            <span><%= sug %></span>
                                        </li>
                                <%  }
                                } %>
                            </ul>
                        </div>
                    <% } else { %>
                        <div style="text-align: center; padding: 4rem 2rem; color: var(--text-muted);">
                            <div style="font-size: 3rem; margin-bottom: 1rem; opacity: 0.5;">📄</div>
                            <h4 style="color: var(--text-primary); margin-bottom: 0.5rem;">No Resume Analyzed Yet</h4>
                            <p style="font-size: 0.9rem;">Upload your resume PDF or paste your experience details on the left to generate your verified ATS scorecard and skill audit.</p>
                        </div>
                    <% } %>
                </div>
            </div>
        </main>
    </div>

    <script>
        function updateFileLabel(input) {
            const label = document.getElementById('fileSelectedLabel');
            if (input.files && input.files[0]) {
                label.textContent = 'Selected: ' + input.files[0].name;
            }
        }
    </script>
</body>
</html>
