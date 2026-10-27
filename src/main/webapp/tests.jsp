<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.TestAttempt" %>
<%@ page import="com.tpobuddy.model.TestSeries" %>
<%@ page import="com.tpobuddy.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%
    User user = (User) request.getAttribute("user");
    List<TestAttempt> attempts = (List<TestAttempt>) request.getAttribute("attempts");
    List<TestSeries> collegeSeries = (List<TestSeries>) request.getAttribute("collegeSeries");
    List<TestSeries> platformSeries = (List<TestSeries>) request.getAttribute("platformSeries");
    Integer totalQuestionBankSize = (Integer) request.getAttribute("totalQuestionBankSize");
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>AI Test Series — College & Platform Preparation — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
    <script src="${pageContext.request.contextPath}/assets/js/tests.js?v=<%= System.currentTimeMillis() %>"></script>
    <style>
        /* Top Category Navigation Tabs */
        .category-nav-bar {
            display: flex;
            gap: 1rem;
            margin-bottom: 2rem;
            border-bottom: 2px solid var(--border-subtle);
            padding-bottom: 0.75rem;
        }
        .category-tab-btn {
            background: transparent;
            border: none;
            padding: 0.85rem 1.75rem;
            font-size: 1.05rem;
            font-weight: 700;
            color: var(--text-secondary);
            cursor: pointer;
            border-radius: var(--radius-sm);
            display: flex;
            align-items: center;
            gap: 0.65rem;
            transition: all 0.2s ease;
            position: relative;
        }
        .category-tab-btn:hover {
            color: var(--primary);
            background: rgba(37, 99, 235, 0.05);
        }
        .category-tab-btn.active {
            color: var(--primary);
            background: #eff6ff;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
        }
        .category-tab-btn.active::after {
            content: '';
            position: absolute;
            bottom: -0.85rem;
            left: 0;
            right: 0;
            height: 3px;
            background: var(--primary);
            border-radius: 3px 3px 0 0;
        }

        /* Sub-Filter Pills */
        .sub-filter-pill {
            padding: 0.5rem 1.15rem;
            border-radius: 9999px;
            font-size: 0.85rem;
            font-weight: 600;
            cursor: pointer;
            border: 1px solid var(--border-subtle);
            background: #ffffff;
            color: var(--text-secondary);
            transition: all 0.2s ease;
        }
        .sub-filter-pill:hover {
            background: #f1f5f9;
            color: var(--text-primary);
        }
        .sub-filter-pill.active {
            background: var(--primary);
            color: #ffffff;
            border-color: var(--primary);
            box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
        }

        /* Series Card */
        .series-card {
            background: #ffffff;
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-md);
            padding: 1.5rem;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: all 0.25s ease;
            position: relative;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        }
        .series-card:hover {
            transform: translateY(-3px);
            border-color: #93c5fd;
            box-shadow: 0 10px 25px -5px rgba(37, 99, 235, 0.12), 0 8px 10px -6px rgba(37, 99, 235, 0.05);
        }
        .series-card-featured {
            border: 2px solid #3b82f6;
            background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 100%);
        }

        /* Difficulty Selector Cards */
        .difficulty-options-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 0.75rem;
            margin: 1rem 0 1.25rem 0;
        }
        .difficulty-card {
            border: 2px solid var(--border-subtle);
            border-radius: var(--radius-sm);
            padding: 0.85rem;
            cursor: pointer;
            text-align: center;
            background: #ffffff;
            transition: all 0.2s ease;
        }
        .difficulty-card:hover {
            border-color: #93c5fd;
            background: #f8fafc;
        }
        .difficulty-card.active {
            border-color: var(--primary);
            background: #eff6ff;
            box-shadow: 0 0 0 1px var(--primary);
        }
        .difficulty-title {
            font-weight: 700;
            font-size: 0.9rem;
            margin-bottom: 0.25rem;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.35rem;
        }
        .difficulty-desc {
            font-size: 0.725rem;
            color: var(--text-muted);
            line-height: 1.3;
        }

        /* AI Generation Modal */
        .ai-loading-card {
            background: #ffffff;
            border-radius: var(--radius-md);
            padding: 2.5rem 2rem;
            max-width: 520px;
            width: 90%;
            text-align: center;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
            position: relative;
        }
        .ai-pulse-orb {
            width: 72px;
            height: 72px;
            margin: 0 auto 1.5rem auto;
            border-radius: 50%;
            background: linear-gradient(135deg, #6366f1 0%, #3b82f6 50%, #10b981 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 2rem;
            box-shadow: 0 0 25px rgba(99, 102, 241, 0.6);
            animation: pulse-glow 2s infinite ease-in-out;
        }
        @keyframes pulse-glow {
            0%, 100% { transform: scale(1); box-shadow: 0 0 20px rgba(99, 102, 241, 0.5); }
            50% { transform: scale(1.08); box-shadow: 0 0 35px rgba(59, 130, 246, 0.8); }
        }
        .ai-progress-bar-container {
            width: 100%;
            height: 8px;
            background: #e2e8f0;
            border-radius: 9999px;
            overflow: hidden;
            margin: 1.5rem 0;
        }
        .ai-progress-bar-fill {
            height: 100%;
            width: 30%;
            background: linear-gradient(90deg, #6366f1, #3b82f6, #10b981);
            border-radius: 9999px;
            transition: width 0.5s ease;
            animation: shimmer-load 2s infinite linear;
            background-size: 200% 100%;
        }
        @keyframes shimmer-load {
            0% { background-position: 100% 0; }
            100% { background-position: -100% 0; }
        }

        /* Passage or Code Snippet Block */
        .passage-code-box {
            background: #0f172a;
            color: #e2e8f0;
            border-radius: var(--radius-sm);
            padding: 1.25rem;
            margin: 1rem 0 1.5rem 0;
            font-family: 'Fira Code', 'Consolas', monospace;
            font-size: 0.9rem;
            line-height: 1.6;
            overflow-x: auto;
            border: 1px solid #1e293b;
            position: relative;
        }
        .passage-code-badge {
            position: absolute;
            top: 0.5rem;
            right: 0.75rem;
            background: rgba(255, 255, 255, 0.1);
            color: #94a3b8;
            font-size: 0.7rem;
            font-weight: 600;
            padding: 0.2rem 0.5rem;
            border-radius: 4px;
            text-transform: uppercase;
        }
        .reading-passage-box {
            background: #f8fafc;
            color: #1e293b;
            border-left: 4px solid var(--primary);
            border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
            padding: 1.25rem;
            margin: 1rem 0 1.5rem 0;
            font-size: 0.925rem;
            line-height: 1.7;
            font-style: italic;
        }

        /* Exam Option Cards */
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
            width: 34px;
            height: 34px;
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

        /* Palette Grid */
        .palette-grid {
            display: grid;
            grid-template-columns: repeat(5, 1fr);
            gap: 0.5rem;
            max-height: 300px;
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

        /* Analytics & Results */
        .review-item-card {
            background: #ffffff;
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-sm);
            padding: 1.25rem;
            margin-bottom: 1.25rem;
        }
        .review-item-card.correct {
            border-left: 4px solid #10b981;
        }
        .review-item-card.incorrect {
            border-left: 4px solid #dc2626;
        }
        .weak-topic-chip {
            background: #fee2e2;
            color: #991b1b;
            border: 1px solid #fecaca;
            padding: 0.35rem 0.75rem;
            border-radius: 9999px;
            font-size: 0.8rem;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 0.35rem;
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
                    <h1 class="page-title">🎯 AI Test Series — College & Platform Preparation</h1>
                    <p class="page-subtitle">
                        Campus recruitment assessments with dynamic AI question generation (50 candidate pool → 30 selected questions), difficulty calibration, proctored testing, and performance analytics.
                    </p>
                </div>
                <div style="display: flex; gap: 0.75rem; align-items: center;">
                    <span class="badge badge-primary" style="font-size: 0.85rem; padding: 0.45rem 0.85rem;">
                        ⚡ Gemini AI Integrated
                    </span>
                    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()">🚪 Logout</button>
                </div>
            </div>

            <!-- ============================================================= -->
            <!-- Test Series Explorer Screen -->
            <!-- ============================================================= -->
            <div id="testCategoryPicker">
                <!-- Main Category Switcher (College vs Platform) -->
                <div class="category-nav-bar">
                    <button type="button" class="category-tab-btn active" id="tabCollegeSeriesBtn" onclick="switchMainCategory('COLLEGE')">
                        <span>🏛️</span>
                        <span>College Test Series</span>
                    </button>
                    <button type="button" class="category-tab-btn" id="tabPlatformSeriesBtn" onclick="switchMainCategory('PLATFORM')">
                        <span>⚡</span>
                        <span>Platform Test Series</span>
                    </button>
                </div>

                <!-- ------------------------------------------------------------- -->
                <!-- A. COLLEGE TEST SERIES SECTION -->
                <!-- ------------------------------------------------------------- -->
                <div id="collegeSeriesContainer">
                    <div style="margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
                        <div>
                            <h2 style="font-size: 1.35rem; font-weight: 800; margin: 0; color: var(--text-primary);">
                                College Assessments & SNJB Placement Preparation
                            </h2>
                            <p style="font-size: 0.875rem; color: var(--text-muted); margin: 0.25rem 0 0 0;">
                                Curated to prepare students for college examinations, departmental assessments, and SNJB placement preparation.
                            </p>
                        </div>

                        <!-- College Sub-Filters -->
                        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                            <button type="button" class="sub-filter-pill active" onclick="filterCollegeGroup('ALL', this)">All College</button>
                            <button type="button" class="sub-filter-pill" onclick="filterCollegeGroup('SNJB_PLACEMENT', this)">Series 1: SNJB Placement</button>
                            <button type="button" class="sub-filter-pill" onclick="filterCollegeGroup('BASIC_CS', this)">Series 2: Basic CS</button>
                            <button type="button" class="sub-filter-pill" onclick="filterCollegeGroup('COLLEGE_SPECIFIC', this)">Series 3: College Specific</button>
                        </div>
                    </div>

                    <!-- Series 1: SNJB Placement Prep Highlight Banner -->
                    <div class="glass-card college-group-item" data-group="SNJB_PLACEMENT" style="padding: 1.75rem; margin-bottom: 2rem; border: 2px solid #3b82f6; background: linear-gradient(135deg, rgba(239, 246, 255, 0.95) 0%, rgba(240, 253, 250, 0.95) 100%);">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.25rem; flex-wrap: wrap; gap: 1rem;">
                            <div>
                                <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.35rem;">
                                    <span style="font-size: 1.5rem;">🎯</span>
                                    <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--text-primary); margin: 0;">
                                        Series 1: SNJB Placement Preparation Assessment (Full Mock)
                                    </h3>
                                    <span class="badge badge-primary">Standardized 4-Section Mock</span>
                                </div>
                                <p style="font-size: 0.875rem; color: var(--text-secondary); margin: 0; max-width: 750px;">
                                    Comprehensive placement assessment covering all 4 core sections: Quantitative Aptitude (8Q), Logical Reasoning (8Q), Verbal Ability (7Q), and Technical & Coding (7Q).
                                </p>
                            </div>
                            <button type="button" class="btn btn-primary" onclick="initiateExamSetup('college_snjb_full_mock', 'SNJB Placement Preparation Assessment', 'COLLEGE', 'Multi-Section Assessment', null)">
                                <span>Start 4-Section Mock (30 Qs)</span>
                                <span>→</span>
                            </button>
                        </div>

                        <!-- 4 Major Sectional Direct Cards -->
                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 1rem;">
                            <div style="background: #ffffff; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; display: flex; flex-direction: column; justify-content: space-between;">
                                <div>
                                    <div style="font-weight: 700; color: var(--text-primary); font-size: 0.95rem; margin-bottom: 0.25rem;">📐 1. Quantitative Aptitude</div>
                                    <div style="font-size: 0.775rem; color: var(--text-muted); margin-bottom: 0.85rem;">Number system, percentages, profit & loss, algebra, series</div>
                                </div>
                                <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('college_snjb_quant', 'SNJB Quantitative Aptitude Sectional', 'COLLEGE', 'Quantitative Aptitude', null)">
                                    Take Sectional →
                                </button>
                            </div>

                            <div style="background: #ffffff; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; display: flex; flex-direction: column; justify-content: space-between;">
                                <div>
                                    <div style="font-weight: 700; color: var(--text-primary); font-size: 0.95rem; margin-bottom: 0.25rem;">🧩 2. Logical Reasoning</div>
                                    <div style="font-size: 0.775rem; color: var(--text-muted); margin-bottom: 0.85rem;">Series, coding-decoding, blood relations, seating arrangements</div>
                                </div>
                                <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('college_snjb_logical', 'SNJB Logical Reasoning Sectional', 'COLLEGE', 'Logical Reasoning', null)">
                                    Take Sectional →
                                </button>
                            </div>

                            <div style="background: #ffffff; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; display: flex; flex-direction: column; justify-content: space-between;">
                                <div>
                                    <div style="font-weight: 700; color: var(--text-primary); font-size: 0.95rem; margin-bottom: 0.25rem;">📖 3. Verbal Ability</div>
                                    <div style="font-size: 0.775rem; color: var(--text-muted); margin-bottom: 0.85rem;">Grammar, comprehension passages, idioms, para jumbles</div>
                                </div>
                                <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('college_snjb_verbal', 'SNJB Verbal Ability Sectional', 'COLLEGE', 'Verbal Ability', null)">
                                    Take Sectional →
                                </button>
                            </div>

                            <div style="background: #ffffff; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 1rem; display: flex; flex-direction: column; justify-content: space-between;">
                                <div>
                                    <div style="font-weight: 700; color: var(--text-primary); font-size: 0.95rem; margin-bottom: 0.25rem;">💻 4. Technical & Coding</div>
                                    <div style="font-size: 0.775rem; color: var(--text-muted); margin-bottom: 0.85rem;">C, Java, Python, output prediction, DSA, DBMS, OS, Networks</div>
                                </div>
                                <button type="button" class="btn btn-outline btn-sm" onclick="initiateExamSetup('college_snjb_tech', 'SNJB Technical and Coding Sectional', 'COLLEGE', 'Technical and Coding', null)">
                                    Take Sectional →
                                </button>
                            </div>
                        </div>

                        <!-- Priority 6 Foundational Modules -->
                        <div style="margin-top: 1.25rem; pt: 1rem; border-top: 1px dashed #bfdbfe;">
                            <div style="font-size: 0.8rem; font-weight: 700; color: #1e40af; margin-bottom: 0.5rem;">
                                ⚡ Quick 10-Question Foundational Priority Drills:
                            </div>
                            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                                <button type="button" class="btn btn-outline btn-sm" style="background:#fff; font-size:0.775rem;" onclick="initiateExamSetup('college_snjb_quant', 'Quantitative Aptitude', 'COLLEGE', 'Quantitative Aptitude', 'Number system, HCF/LCM, divisibility')">1. Number System & HCF</button>
                                <button type="button" class="btn btn-outline btn-sm" style="background:#fff; font-size:0.775rem;" onclick="initiateExamSetup('college_snjb_quant', 'Quantitative Aptitude', 'COLLEGE', 'Quantitative Aptitude', 'Percentages, profit and loss, discounts')">2. Percentages & Profit/Loss</button>
                                <button type="button" class="btn btn-outline btn-sm" style="background:#fff; font-size:0.775rem;" onclick="initiateExamSetup('college_snjb_quant', 'Quantitative Aptitude', 'COLLEGE', 'Quantitative Aptitude', 'Ratio, proportion, averages, mixtures and alligation')">3. Ratio & Averages</button>
                                <button type="button" class="btn btn-outline btn-sm" style="background:#fff; font-size:0.775rem;" onclick="initiateExamSetup('college_snjb_quant', 'Quantitative Aptitude', 'COLLEGE', 'Quantitative Aptitude', 'Time and work, pipes and cisterns')">4. Time and Work</button>
                                <button type="button" class="btn btn-outline btn-sm" style="background:#fff; font-size:0.775rem;" onclick="initiateExamSetup('college_snjb_quant', 'Quantitative Aptitude', 'COLLEGE', 'Quantitative Aptitude', 'Time, speed and distance, trains, boats and streams')">5. Speed & Distance</button>
                                <button type="button" class="btn btn-outline btn-sm" style="background:#fff; font-size:0.775rem;" onclick="initiateExamSetup('college_snjb_quant', 'Quantitative Aptitude', 'COLLEGE', 'Quantitative Aptitude', 'Permutations, combinations, probability')">6. Probability & PnC</button>
                            </div>
                        </div>
                    </div>

                    <!-- Series 2: Basic Computer Science (12 Core Subjects) & Series 3: College Specific Grid -->
                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.5rem; margin-bottom: 2.5rem;" id="collegeSeriesGrid">
                        <% if (collegeSeries != null) {
                            for (TestSeries ts : collegeSeries) {
                                // Skip snjb full mock since it's already rendered in the top banner
                                if ("college_snjb_full_mock".equals(ts.getSeriesId()) ||
                                    "college_snjb_quant".equals(ts.getSeriesId()) ||
                                    "college_snjb_logical".equals(ts.getSeriesId()) ||
                                    "college_snjb_verbal".equals(ts.getSeriesId()) ||
                                    "college_snjb_tech".equals(ts.getSeriesId())) {
                                    continue;
                                }
                                String badgeType = "BASIC_CS".equals(ts.getSeriesType()) ? "Series 2: Basic CS" : "Series 3: College Specific";
                        %>
                            <div class="series-card college-group-item" data-group="<%= ts.getSeriesType() %>">
                                <div>
                                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                                        <span class="badge badge-primary" style="font-size: 0.75rem;"><%= badgeType %></span>
                                        <span class="badge badge-outline" style="font-size: 0.75rem;"><%= ts.getDurationMinutes() %> Mins</span>
                                    </div>
                                    <h3 style="font-size: 1.15rem; margin-bottom: 0.4rem; color: var(--text-primary); display: flex; align-items: center; gap: 0.5rem;">
                                        <span><%= ts.getIcon() != null ? ts.getIcon() : "📝" %></span>
                                        <span><%= ts.getName() %></span>
                                    </h3>
                                    <p style="font-size: 0.85rem; color: var(--text-muted); line-height: 1.4; margin-bottom: 1.25rem;">
                                        <%= ts.getDescription() %>
                                    </p>
                                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; margin-bottom: 1.25rem; font-size: 0.8rem; color: var(--text-secondary); background: #f8fafc; padding: 0.75rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                                        <div>📝 <strong><%= ts.getQuestionCount() %></strong> Questions</div>
                                        <div>🎯 <strong>+<%= ts.getMarksPerQuestion() %></strong> Marks/Q</div>
                                        <div>⚡ <strong>50 AI Pool</strong> Candidates</div>
                                        <div>✓ 3 Difficulty Levels</div>
                                    </div>
                                </div>
                                <button type="button" class="btn btn-outline" style="width: 100%; font-weight: 600;" onclick="initiateExamSetup('<%= ts.getSeriesId() %>', '<%= ts.getName() %>', '<%= ts.getCategory() %>', '<%= ts.getSubject() %>', null)">
                                    Configure & Start Test →
                                </button>
                            </div>
                        <%  }
                        } %>
                    </div>
                </div>

                <!-- ------------------------------------------------------------- -->
                <!-- B. PLATFORM TEST SERIES SECTION (Initially Hidden) -->
                <!-- ------------------------------------------------------------- -->
                <div id="platformSeriesContainer" style="display: none;">
                    <div style="margin-bottom: 1.5rem;">
                        <h2 style="font-size: 1.35rem; font-weight: 800; margin: 0; color: var(--text-primary);">
                            Platform Practice Tracks (Self-Paced Readiness)
                        </h2>
                        <p style="font-size: 0.875rem; color: var(--text-muted); margin: 0.25rem 0 0 0;">
                            Independent skill tracks designed to build technical depth, consistency, and problem-solving performance independently of college tests.
                        </p>
                    </div>

                    <!-- Platform Grid (13 Tracks) -->
                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.5rem; margin-bottom: 2.5rem;">
                        <% if (platformSeries != null) {
                            for (TestSeries ps : platformSeries) {
                        %>
                            <div class="series-card">
                                <div>
                                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                                        <span class="badge badge-secondary" style="font-size: 0.75rem;">Platform Practice Track</span>
                                        <span class="badge badge-outline" style="font-size: 0.75rem;"><%= ps.getDurationMinutes() %> Mins</span>
                                    </div>
                                    <h3 style="font-size: 1.15rem; margin-bottom: 0.4rem; color: var(--text-primary); display: flex; align-items: center; gap: 0.5rem;">
                                        <span><%= ps.getIcon() != null ? ps.getIcon() : "⚡" %></span>
                                        <span><%= ps.getName() %></span>
                                    </h3>
                                    <p style="font-size: 0.85rem; color: var(--text-muted); line-height: 1.4; margin-bottom: 1.25rem;">
                                        <%= ps.getDescription() %>
                                    </p>
                                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; margin-bottom: 1.25rem; font-size: 0.8rem; color: var(--text-secondary); background: #f8fafc; padding: 0.75rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                                        <div>📝 <strong><%= ps.getQuestionCount() %></strong> Questions</div>
                                        <div>🎯 <strong>+<%= ps.getMarksPerQuestion() %></strong> Marks/Q</div>
                                        <div>⚡ <strong>50 AI Pool</strong> Candidates</div>
                                        <div>✓ 3 Difficulty Levels</div>
                                    </div>
                                </div>
                                <button type="button" class="btn btn-outline" style="width: 100%; font-weight: 600;" onclick="initiateExamSetup('<%= ps.getSeriesId() %>', '<%= ps.getName() %>', '<%= ps.getCategory() %>', '<%= ps.getSubject() %>', null)">
                                    Configure & Start Track →
                                </button>
                            </div>
                        <%  }
                        } %>
                    </div>
                </div>

                <!-- ------------------------------------------------------------- -->
                <!-- Test History Table -->
                <!-- ------------------------------------------------------------- -->
                <div class="data-table-card">
                    <div class="data-table-header">
                        <h3 style="font-size: 1.15rem;">Your Completed Assessment History</h3>
                        <span style="font-size: 0.85rem; color: var(--text-muted);">Synced from Firebase Realtime Database & Firestore</span>
                    </div>
                    <div class="table-responsive">
                        <table class="table">
                            <thead>
                                <tr>
                                    <th>Category</th>
                                    <th>Series / Subject</th>
                                    <th>Difficulty</th>
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
                                        String catBadge = "COLLEGE".equalsIgnoreCase(a.getCategory()) ? "badge-primary" : "badge-secondary";
                                        String diff = a.getDifficulty() != null && !a.getDifficulty().isEmpty() ? a.getDifficulty() : "Medium";
                                %>
                                    <tr>
                                        <td>
                                            <span class="badge <%= catBadge %>" style="font-size: 0.75rem;">
                                                <%= a.getCategory() != null ? a.getCategory() : "COLLEGE" %>
                                            </span>
                                        </td>
                                        <td style="font-weight: 600; color: var(--text-primary);">
                                            <%= a.getTopic() != null ? a.getTopic() : (a.getTestTitle() != null ? a.getTestTitle() : "General") %>
                                        </td>
                                        <td>
                                            <span class="badge badge-outline" style="font-size: 0.75rem;"><%= diff %></span>
                                        </td>
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
                                        <td colspan="8" style="text-align: center; padding: 3rem; color: var(--text-muted);">
                                            No test attempts recorded yet. Choose any test above to start your practice.
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
                        <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem;">
                            <span id="runnerCategoryBadge" class="badge badge-primary">COLLEGE</span>
                            <span id="runnerDifficultyBadge" class="badge badge-outline">Medium</span>
                            <span id="runnerQuestionTypeBadge" class="badge badge-warning" style="display: none;">Code Output</span>
                        </div>
                        <h2 id="runnerQuestionNumber" style="font-size: 1.35rem; margin: 0.25rem 0 0 0;">Question 1 of 30</h2>
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
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
                            <span id="runnerSectionIndicator" style="font-size: 0.85rem; font-weight: 700; color: var(--primary);">Section: Quantitative Aptitude</span>
                            <span id="runnerMarksBadge" class="badge badge-outline">+2 Marks</span>
                        </div>

                        <!-- Statement -->
                        <div id="runnerQuestionStatement" style="font-size: 1.15rem; font-weight: 600; color: var(--text-primary); line-height: 1.6; margin-bottom: 1rem;">
                            Loading question...
                        </div>

                        <!-- Optional Passage or Code Block Container -->
                        <div id="runnerPassageCodeContainer" style="display: none;"></div>

                        <!-- 4 Options -->
                        <div id="runnerOptionsList"></div>

                        <!-- Controls -->
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem; padding-top: 1rem; border-top: 1px solid var(--border-subtle);">
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
                                <div><span style="color: #94a3b8; font-weight: 700;">●</span> Unanswered (<span id="paletteCountUnanswered">30</span>)</div>
                                <div><span style="color: #f59e0b; font-weight: 700;">●</span> Marked (<span id="paletteCountMarked">0</span>)</div>
                            </div>

                            <!-- Palette Grid (30 Questions) -->
                            <div id="runnerQuestionPalette" class="palette-grid"></div>

                            <!-- Pip Camera -->
                            <div style="margin-top: 1.25rem; border-top: 1px solid var(--border-subtle); padding-top: 1rem;">
                                <video id="examWebcamPip" autoplay muted playsinline style="width: 100%; height: 110px; object-fit: cover; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); display: none; background: #000000;"></video>
                                <div style="font-size: 0.75rem; color: var(--text-muted); text-align: center; margin-top: 0.25rem;">
                                    🛡️ Proctoring Active
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ============================================================= -->
            <!-- Result & Analytics Card (Initially Hidden) -->
            <!-- ============================================================= -->
            <div id="testResultCard" class="test-runner glass-card" style="display: none; padding: 2.5rem;">
                <div style="text-align: center; margin-bottom: 2rem;">
                    <span id="resultStatusBadge" class="badge badge-success" style="font-size: 0.85rem; padding: 0.4rem 1rem; margin-bottom: 1rem;">PASSED</span>
                    <h2 style="font-size: 2rem; margin-bottom: 0.5rem;">Assessment Completed!</h2>
                    <p style="color: var(--text-muted);">Your performance has been scored server-side and synchronized to your Firebase student profile.</p>

                    <div style="display: flex; justify-content: center; gap: 1.25rem; margin-top: 1.5rem; flex-wrap: wrap;">
                        <div style="background: #f8fafc; padding: 1.25rem 1.75rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); min-width: 140px;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Score</div>
                            <div id="resultScorePercent" style="font-family: var(--font-heading); font-size: 2.25rem; font-weight: 800; color: var(--primary);">0%</div>
                        </div>
                        <div style="background: #f8fafc; padding: 1.25rem 1.75rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); min-width: 140px;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Accuracy</div>
                            <div id="resultAccuracyPercent" style="font-family: var(--font-heading); font-size: 2.25rem; font-weight: 800; color: #10b981;">0%</div>
                        </div>
                        <div style="background: #f8fafc; padding: 1.25rem 1.75rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); min-width: 140px;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Correct Answers</div>
                            <div id="resultCorrectFraction" style="font-family: var(--font-heading); font-size: 2.25rem; font-weight: 800; color: var(--text-primary);">0 / 30</div>
                        </div>
                        <div style="background: #f8fafc; padding: 1.25rem 1.75rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); min-width: 140px;">
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Integrity Status</div>
                            <div id="resultRiskDisplay" style="font-family: var(--font-heading); font-size: 1.25rem; font-weight: 700; color: #16a34a; margin-top: 0.5rem;">LOW RISK</div>
                        </div>
                    </div>
                </div>

                <!-- Section-wise Performance Breakdown -->
                <div id="resultSectionBreakdownContainer" style="display: none; border-top: 1px solid var(--border-subtle); padding-top: 1.5rem; margin-bottom: 1.5rem;">
                    <h3 style="font-size: 1.15rem; margin-bottom: 1rem;">Section-Wise Performance</h3>
                    <div id="resultSectionBreakdownGrid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem;"></div>
                </div>

                <!-- Weak Topics and Recommendations -->
                <div id="resultWeakTopicsContainer" style="display: none; background: #fff1f2; border: 1px solid #fecdd3; border-radius: var(--radius-sm); padding: 1.25rem; margin-bottom: 1.5rem;">
                    <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
                        <span style="font-size: 1.25rem;">🩺</span>
                        <h4 style="margin: 0; font-size: 1rem; color: #9f1239;">Weak Topics Identified (Targeted Revision Recommended)</h4>
                    </div>
                    <p style="font-size: 0.85rem; color: #881337; margin: 0 0 0.75rem 0;">
                        The system detected low accuracy in these specific topics. We recommend practicing targeted revision tests to strengthen these concepts:
                    </p>
                    <div id="resultWeakTopicsList" style="display: flex; gap: 0.5rem; flex-wrap: wrap;"></div>
                </div>

                <!-- Step-by-Step Question Review & Solutions -->
                <div style="border-top: 1px solid var(--border-subtle); padding-top: 1.5rem; margin-bottom: 2rem;">
                    <h3 style="font-size: 1.2rem; margin-bottom: 1rem;">Question Explanations & Step-by-Step Solutions</h3>
                    <div id="resultReviewList"></div>
                </div>

                <div style="text-align: center; display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap;">
                    <button type="button" class="btn btn-outline" onclick="retakeCurrentAssessment()">🔄 Retake Test (New Questions)</button>
                    <a href="${pageContext.request.contextPath}/tests" class="btn btn-primary">Done & Return to Test Series</a>
                </div>
            </div>

            <!-- ============================================================= -->
            <!-- Modals: Setup, AI Loading, Security Violation, Resume -->
            <!-- ============================================================= -->

            <!-- Instructions & Difficulty Selector Modal -->
            <div id="examSetupModal" class="modal-overlay">
                <div class="modal-card" style="max-width: 580px;">
                    <div class="modal-header">
                        <div>
                            <h3 class="modal-title" id="setupModalTitle">Assessment Setup & Rules</h3>
                            <div style="font-size: 0.8rem; color: var(--text-muted);" id="setupCategoryDetail">College Test Series • 30 Questions • 30 Minutes</div>
                        </div>
                        <button type="button" class="btn-close" onclick="cancelExamSetup()">✕</button>
                    </div>
                    <div class="modal-body">
                        <!-- Difficulty Selection Section -->
                        <div style="margin-bottom: 1rem;">
                            <label style="font-weight: 700; font-size: 0.875rem; color: var(--text-primary); display: block; margin-bottom: 0.25rem;">
                                Select Difficulty Level:
                            </label>
                            <input type="hidden" id="selectedDifficultyInput" value="Medium">
                            <div class="difficulty-options-grid">
                                <div class="difficulty-card active" id="diffCardMedium" onclick="selectDifficulty('Medium')" style="cursor: pointer; user-select: none;">
                                    <div class="difficulty-title" style="color: #2563eb; display: flex; align-items: center; justify-content: space-between;">
                                        <div style="display: flex; align-items: center; gap: 0.35rem;">
                                            <span>📘</span>
                                            <span>Medium</span>
                                        </div>
                                        <span id="diffBadgeMedium" style="font-size: 0.68rem; font-weight: 700; background: #2563eb; color: #fff; padding: 2px 7px; border-radius: 9999px;">✓ Active</span>
                                    </div>
                                    <div class="difficulty-desc">Standard placement patterns, core fundamentals, and standard applications.</div>
                                </div>
                                <div class="difficulty-card" id="diffCardHard" onclick="selectDifficulty('Hard')" style="cursor: pointer; user-select: none;">
                                    <div class="difficulty-title" style="color: #d97706; display: flex; align-items: center; justify-content: space-between;">
                                        <div style="display: flex; align-items: center; gap: 0.35rem;">
                                            <span>📙</span>
                                            <span>Hard</span>
                                        </div>
                                        <span id="diffBadgeHard" style="font-size: 0.68rem; font-weight: 700; background: #d97706; color: #fff; padding: 2px 7px; border-radius: 9999px; display: none;">✓ Active</span>
                                    </div>
                                    <div class="difficulty-desc">Multistep reasoning, moderate algorithmic depth, and applied technical concepts.</div>
                                </div>
                                <div class="difficulty-card" id="diffCardExtraHard" onclick="selectDifficulty('Extra Hard')" style="cursor: pointer; user-select: none;">
                                    <div class="difficulty-title" style="color: #dc2626; display: flex; align-items: center; justify-content: space-between;">
                                        <div style="display: flex; align-items: center; gap: 0.35rem;">
                                            <span>📕</span>
                                            <span>Extra Hard</span>
                                        </div>
                                        <span id="diffBadgeExtraHard" style="font-size: 0.68rem; font-weight: 700; background: #dc2626; color: #fff; padding: 2px 7px; border-radius: 9999px; display: none;">✓ Active</span>
                                    </div>
                                    <div class="difficulty-desc">Advanced reasoning, edge cases, complex problem-solving, and tricky output prediction.</div>
                                </div>
                            </div>
                        </div>

                        <!-- Proctoring Rules -->
                        <ul style="font-size: 0.85rem; color: var(--text-secondary); line-height: 1.6; margin-bottom: 1rem; padding-left: 1.2rem;">
                            <li>Personalized test generated with <strong>Gemini AI</strong> (30 balanced questions).</li>
                            <li>The assessment runs in <strong>mandatory fullscreen mode</strong>.</li>
                            <li>Exiting fullscreen or switching browser tabs triggers an <strong>integrity warning</strong> (Max 3 strikes).</li>
                            <li>Countdown timer continues even if you reload the page.</li>
                        </ul>

                        <!-- Optional Webcam Preview -->
                        <div style="background: #f8fafc; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 0.85rem; margin-bottom: 1rem;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                                <div style="display: flex; align-items: center; gap: 0.5rem;">
                                    <span id="setupStatusDot" style="width: 10px; height: 10px; border-radius: 50%; background: #94a3b8; display: inline-block;"></span>
                                    <span style="font-size: 0.8rem; font-weight: 600;" id="setupStatusText">Webcam check (optional)</span>
                                </div>
                                <button type="button" class="btn btn-outline btn-sm" onclick="requestWebcamVerification()">Check Camera</button>
                            </div>
                            <video id="setupWebcamPreview" autoplay muted playsinline style="width: 100%; height: 120px; object-fit: cover; border-radius: var(--radius-sm); display: none;"></video>
                            <div id="setupWebcamPlaceholder" style="text-align: center; padding: 0.75rem; color: var(--text-muted); font-size: 0.85rem;">
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
                            Generate & Start AI Test →
                        </button>
                    </div>
                </div>
            </div>

            <!-- AI Question Generation Loading Modal -->
            <div id="aiGenLoadingModal" class="modal-overlay">
                <div class="ai-loading-card">
                    <div class="ai-pulse-orb">⚡</div>
                    <h3 style="font-size: 1.4rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.5rem;">
                        Preparing Your Personalized Test
                    </h3>
                    <div style="font-size: 0.875rem; color: var(--text-secondary); margin-bottom: 1.25rem;" id="aiLoadingSeriesLabel">
                        Generating questions with Google Gemini AI...
                    </div>

                    <div class="ai-progress-bar-container">
                        <div class="ai-progress-bar-fill" id="aiProgressBar"></div>
                    </div>

                    <div id="aiLoadingStatusText" style="font-size: 0.85rem; font-weight: 600; color: var(--primary); min-height: 24px;">
                        Contacting Gemini AI Engine...
                    </div>
                    <p style="font-size: 0.75rem; color: var(--text-muted); margin-top: 1rem;">
                        Generating 50 candidate questions, strictly validating schema, and selecting 30 balanced questions based on topic weights.
                    </p>
                </div>
            </div>

            <!-- Security Warning Modal (Three-Strike Anti-Cheat) -->
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
        function switchMainCategory(category) {
            const collegeBtn = document.getElementById('tabCollegeSeriesBtn');
            const platformBtn = document.getElementById('tabPlatformSeriesBtn');
            const collegeCont = document.getElementById('collegeSeriesContainer');
            const platformCont = document.getElementById('platformSeriesContainer');

            if (category === 'COLLEGE') {
                collegeBtn.classList.add('active');
                platformBtn.classList.remove('active');
                collegeCont.style.display = 'block';
                platformCont.style.display = 'none';
            } else {
                platformBtn.classList.add('active');
                collegeBtn.classList.remove('active');
                platformCont.style.display = 'block';
                collegeCont.style.display = 'none';
            }
        }

        function filterCollegeGroup(group, btn) {
            document.querySelectorAll('.sub-filter-pill').forEach(b => b.classList.remove('active'));
            if (btn) btn.classList.add('active');

            const items = document.querySelectorAll('.college-group-item');
            items.forEach(item => {
                if (group === 'ALL') {
                    item.style.display = (item.tagName.toLowerCase() === 'div' && item.classList.contains('series-card')) ? 'flex' : 'block';
                } else {
                    const itemGroup = item.getAttribute('data-group');
                    if (itemGroup === group) {
                        item.style.display = (item.tagName.toLowerCase() === 'div' && item.classList.contains('series-card')) ? 'flex' : 'block';
                    } else {
                        item.style.display = 'none';
                    }
                }
            });
        }

        window.selectDifficulty = function(diff) {
            window.selectedDifficulty = diff;
            var hiddenInput = document.getElementById('selectedDifficultyInput');
            if (hiddenInput) hiddenInput.value = diff;

            var cards = {
                'Medium': document.getElementById('diffCardMedium'),
                'Hard': document.getElementById('diffCardHard'),
                'Extra Hard': document.getElementById('diffCardExtraHard')
            };

            var badges = {
                'Medium': document.getElementById('diffBadgeMedium'),
                'Hard': document.getElementById('diffBadgeHard'),
                'Extra Hard': document.getElementById('diffBadgeExtraHard')
            };

            Object.keys(cards).forEach(function(key) {
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
        };
    </script>
</body>
</html>
