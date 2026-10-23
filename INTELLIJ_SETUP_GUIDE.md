# 🚀 TPO Buddy - IntelliJ IDEA Complete Setup & Execution Guide

Welcome to the **TPO Buddy Web Platform** — a modernized full-stack web application converted from the CareerPulse / TPO Buddy Android app using **Spring Boot 3.2.5**, **React 18**, **JSP + HTML5**, and **Embedded Tomcat 10**.

---

## 📌 Project Overview
- **Workspace Location**: `D:\tpo_intellag`
- **Backend Framework**: Spring Boot 3.2.5 with Java 21 / 25
- **Embedded Web Server**: Apache Tomcat 10 (Port **`8085`** to prevent Docker/WSL conflicts)
- **Frontend Architecture**:
  - **JSP + HTML5 Views**: Server-rendered dashboards, tests, resume analysis, and profile management with Vanilla CSS glassmorphic aesthetics.
  - **React 18 Embedded Components**: Interactive AI Mock Interview Studio with camera/speech proctoring and real-time scorecards.
  - **React 18 Single Page Application (SPA)**: Modern dark-themed dashboard at `/react-portal` and standalone in `client/` with Vite.
- **AI Placement Engine**:
  - AI Mock Interview Studio & Scoring Rubric (`/interview`)
  - Apache PDFBox Resume ATS Parser & Skill Matcher (`/resume`)
  - Career Copilot Placement Chatbot (`/ai-buddy`)
  - Anti-Cheating Proctored Assessment Tests (`/tests`)
  - Campus Job Board with 1-Click Apply (`/jobs`)

---

## 🛠️ How to Open and Run in IntelliJ IDEA

### Step 1: Open the Project in IntelliJ IDEA
1. Launch **IntelliJ IDEA**.
2. Click **File** ➔ **Open...**
3. Select the folder: `D:\tpo_intellag` and click **OK**.
4. When prompted with *"Trust Project"*, select **Trust Project**.
5. IntelliJ IDEA will automatically recognize:
   - `pom.xml` as the root Maven project.
   - Spring Boot and Web module facets (`tpo-buddy.iml`).
   - Pre-configured Run Configurations in `.idea/runConfigurations/`.

---

### Step 2: Configure Project SDK (Java 21 or Java 25)
1. In IntelliJ IDEA, go to **File** ➔ **Project Structure** (`Ctrl + Alt + Shift + S`).
2. Under **Project Settings** ➔ **Project**:
   - **SDK**: Select **Java 21** or **Java 25** (e.g., Android Studio JBR `C:\Program Files\Android\Android Studio\jbr` or Temurin/Oracle JDK 21+).
   - **Language Level**: Set to `21 - Pattern matching, record patterns, virtual threads`.
3. Click **Apply** and **OK**.

---

### Step 3: Run the Application (3 Easy Options)

#### Option A: One-Click Run Configuration (Recommended)
1. In the top-right toolbar of IntelliJ IDEA, look at the **Run Configurations** dropdown.
2. Select **`TpoBuddyApplication`**.
3. Click the green **▶ Run** button (or press `Shift + F10`).
4. Embedded Tomcat will start on port `8085`.

#### Option B: Maven Tool Window
1. On the right-hand sidebar of IntelliJ IDEA, open the **Maven** tab.
2. Expand **`tpo-buddy`** ➔ **Plugins** ➔ **`spring-boot`**.
3. Double-click **`spring-boot:run`**.

#### Option C: Windows Batch Script
1. In `D:\tpo_intellag`, double-click **`START_SPRING_BOOT.bat`** (or `run.bat`).
2. The script auto-detects your Java runtime and launches the application.

---

### Step 4: Running the React Client (Optional / Standalone)
You have **both** embedded React inside the server AND a standalone React Vite client:

#### 1. Embedded React (No Node.js command needed):
- Simply visit: **http://localhost:8085/react-portal** or **http://localhost:8085/react-app**
- Fully renders the React 18 Single Page Application served directly by Spring Boot!

#### 2. Standalone React Vite Dev Server (Hot Reloading):
1. In IntelliJ IDEA, open the **Terminal** (`Alt + F12`).
2. Run:
   ```powershell
   cd client
   npm run dev
   ```
3. Open **http://localhost:5173/**. Requests to `/api/**` are automatically proxied to Spring Boot on port `8085`.

---

## 🌐 Application URL Directory

| Feature / Page | URL Path | Description |
|---|---|---|
| **Home Landing Page** | `http://localhost:8085/` | Public portal with hero, role navigation, and feature showcase |
| **Login Portal** | `http://localhost:8085/login.jsp` | Sign in with 1-click Demo buttons, Email/Password, or Google |
| **Student Dashboard** | `http://localhost:8085/dashboard` | Placement readiness radar, test stats, upcoming drives |
| **🎙️ AI Mock Interview** | `http://localhost:8085/interview` | Role-based technical & HR interview with audio & live score report |
| **⚛️ React Modern Portal** | `http://localhost:8085/react-portal` | Pure React 18 SPA with interactive dashboard, radar & chat |
| **🤖 Career Copilot AI** | `http://localhost:8085/ai-buddy` | AI placement assistant with interview suggestions and advice |
| **📄 AI Resume ATS** | `http://localhost:8085/resume` | PDF upload, Apache PDFBox text extraction & ATS score analysis |
| **✅ Practice Tests** | `http://localhost:8085/tests` | Proctored 25-question timed assessments (Aptitude, Tech, Coding) |
| **💼 Placement Job Board** | `http://localhost:8085/jobs` | Campus drives, eligibility verification, and 1-click applications |
| **🚀 Skill Progress** | `http://localhost:8085/progress` | Learning roadmaps, test history graphs, and category breakdowns |
| **👨‍🏫 Faculty / TPO Portal** | `http://localhost:8085/faculty` | Student verification, test attempt review, batch statistics |
| **🛡️ Admin Console** | `http://localhost:8085/admin` | Post drives, add questions, broadcast notices, system settings |
| **🩺 Actuator Health** | `http://localhost:8085/actuator/health` | Embedded Tomcat server health and JVM metrics |

---

## ⚡ Instant One-Click Demo Credentials
On `http://localhost:8085/login.jsp`, click any of the **Quick Demo Login** buttons:
- **👨‍🎓 Student**: `student@tpobuddy.edu` (Full student dashboard, tests, interview, ATS)
- **👨‍🏫 Faculty / TPO**: `faculty@tpobuddy.edu` (Faculty assessment analytics & approvals)
- **🛡️ Admin**: `admin@tpobuddy.edu` (Full administrative console, drive creation, question bank)

Direct browser demo links:
- `http://localhost:8085/api/auth/demo?role=student`
- `http://localhost:8085/api/auth/demo?role=faculty`
- `http://localhost:8085/api/auth/demo?role=admin`

---

## 📱 Android App to Web Architecture Mapping

| Android Activity (`com.tpo.careerpulse`) | Web Implementation in `d:\tpo_intellag` |
|---|---|
| `LoginActivity` / `RegisterActivity` | `login.jsp`, `register.jsp`, `AuthController.java` |
| `DashboardActivity` | `student-dashboard.jsp`, `DashboardController.java` |
| `InterviewActivity` & `MockInterviewReportActivity` | `interview.jsp`, `InterviewController.java`, `InterviewService.java` |
| `AIChatActivity` | `ai-buddy.jsp`, `AiChatController.java`, `PersonalizedAiEngine.java` |
| `TestDashboardActivity` / `AptitudeActivity` | `tests.jsp`, `TestController.java`, `TestQuestionBank.java` |
| `JobMatchActivity` | `jobs.jsp`, `JobController.java`, `AdminJobMatcher.java` |
| `ResumeActivity` | `resume-analysis.jsp`, `ResumeController.java`, Apache PDFBox |
| `StudyPlanActivity` | `progress.jsp`, `ProgressController.java` |
| `TeacherActivity` | `faculty-dashboard.jsp`, `FacultyController.java` |
| `AdminActivity` | `admin-dashboard.jsp`, `AdminController.java` |
| `NotificationsActivity` | `NotificationController.java` (`/api/notifications`), navbar alerts |
| Web Admin Panel (`admin-panel`) | `react-portal.jsp` and standalone `client/` Vite SPA |
