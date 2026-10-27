import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');
const webappDir = path.resolve(rootDir, 'src/main/webapp');
const distDir = path.resolve(rootDir, 'dist');

console.log('🚀 Building full TPO Buddy static website for Netlify...');
console.log('Source webapp:', webappDir);
console.log('Target dist:  ', distDir);

// 1. Prepare dist directory
if (fs.existsSync(distDir)) {
  fs.rmSync(distDir, { recursive: true, force: true });
}
fs.mkdirSync(distDir, { recursive: true });

// 2. Copy assets (CSS, JS, images)
const srcAssets = path.resolve(webappDir, 'assets');
const destAssets = path.resolve(distDir, 'assets');
if (fs.existsSync(srcAssets)) {
  fs.cpSync(srcAssets, destAssets, { recursive: true });
  console.log('✓ Copied assets to dist/assets');
}

// Read fragments
const headFrag = fs.readFileSync(path.resolve(webappDir, 'WEB-INF/fragments/head.jsp'), 'utf-8');
const navbarFrag = fs.readFileSync(path.resolve(webappDir, 'WEB-INF/fragments/navbar.jsp'), 'utf-8');
const footerFrag = fs.readFileSync(path.resolve(webappDir, 'WEB-INF/fragments/footer.jsp'), 'utf-8');
const sidebarFrag = fs.readFileSync(path.resolve(webappDir, 'WEB-INF/fragments/sidebar.jsp'), 'utf-8');

// Fragment pre-processing
function cleanHead(content) {
  return content
    .replace(/<%@\s*page[^%>]*%>/g, '')
    .replace(/\$\{pageContext\.request\.contextPath\}/g, '')
    .replace(/<%= Config\.getFirebaseApiKey\(\) %>/g, 'AIzaSyDZoYdhWMj76s3Q8Oujohus2kSmkNTyU0Y')
    .replace(/<%= Config\.get\("firebase\.authDomain"[^)]*\) %>/g, 'tpobuddy.firebaseapp.com')
    .replace(/<%= Config\.getFirebaseDatabaseUrl\(\) %>/g, 'https://tpobuddy-default-rtdb.firebaseio.com')
    .replace(/<%= Config\.getFirebaseProjectId\(\) %>/g, 'tpobuddy')
    .replace(/<%= Config\.getFirebaseStorageBucket\(\) %>/g, 'tpobuddy.firebasestorage.app')
    .replace(/<%= Config\.get\("firebase\.messagingSenderId"[^)]*\) %>/g, '811364970356')
    .replace(/<%= Config\.getFirebaseAppId\(\) %>/g, '1:811364970356:web:09bc29db4bff3a07cb4621')
    .replace(/<%= Config\.getFirebaseMeasurementId\(\) %>/g, 'G-K3KP8NTCSF')
    .replace(/window\.CONTEXT_PATH\s*=\s*'[^']*';/, "window.CONTEXT_PATH = '';");
}

function cleanNavbar(content) {
  return content
    .replace(/<%@\s*page[^%>]*%>/g, '')
    .replace(/\$\{pageContext\.request\.contextPath\}/g, '')
    .replace(/href=["'](\/?)login\.jsp["']/g, 'href="$1login"')
    .replace(/href=["'](\/?)register\.jsp["']/g, 'href="$1register"');
}

function cleanFooter(content) {
  return content
    .replace(/<%@\s*page[^%>]*%>/g, '')
    .replace(/\$\{pageContext\.request\.contextPath\}/g, '');
}

function cleanSidebar(content) {
  // Static version with client-side profile synchronization
  return `
<aside class="app-sidebar">
    <div class="sidebar-header">
        <a href="/dashboard" class="sidebar-brand">
            <div class="logo-orb">TB</div>
            <span>TPO<span style="color: var(--secondary);">Buddy</span></span>
        </a>
    </div>

    <ul class="sidebar-nav">
        <li>
            <a href="/dashboard" class="nav-item-link" id="nav-dashboard">
                <span class="nav-icon">&#128202;</span>
                <span>Dashboard</span>
            </a>
        </li>
        <li>
            <a href="/tests" class="nav-item-link" id="nav-tests" style="display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center; gap: 0.65rem;">
                    <span class="nav-icon">&#127919;</span>
                    <span>AI Test Series</span>
                </div>
                <span class="badge" style="font-size: 0.65rem; background: #2563eb; color: #fff; padding: 2px 7px; border-radius: 9999px; font-weight: 700;">Live</span>
            </a>
        </li>
        <li>
            <a href="/interview" class="nav-item-link" id="nav-interview">
                <span class="nav-icon">&#127897;</span>
                <span>AI Mock Interview</span>
            </a>
        </li>
        <li>
            <a href="/resume" class="nav-item-link" id="nav-resume">
                <span class="nav-icon">&#128196;</span>
                <span>AI Resume ATS</span>
            </a>
        </li>
        <li>
            <a href="/ai-buddy" class="nav-item-link" id="nav-ai-buddy">
                <span class="nav-icon">&#129302;</span>
                <span>Career Copilot AI</span>
            </a>
        </li>
        <li>
            <a href="/jobs" class="nav-item-link" id="nav-jobs">
                <span class="nav-icon">&#128188;</span>
                <span>Placement Jobs</span>
            </a>
        </li>
        <li>
            <a href="/progress" class="nav-item-link" id="nav-progress">
                <span class="nav-icon">&#128640;</span>
                <span>My Progress</span>
            </a>
        </li>
        <li>
            <a href="/profile" class="nav-item-link" id="nav-profile">
                <span class="nav-icon">&#128100;</span>
                <span>Profile</span>
            </a>
        </li>
        <li style="margin-top: auto; padding-top: 1rem;">
            <a href="javascript:void(0)" onclick="handleLogout()" class="nav-item-link" style="color: var(--accent-rose);">
                <span class="nav-icon">&#128682;</span>
                <span>Logout</span>
            </a>
        </li>
    </ul>

    <div class="sidebar-footer">
        <div class="user-snippet">
            <div class="user-avatar" id="sidebarUserAvatar">S</div>
            <div class="user-details">
                <div class="user-name" id="sidebarUserName">Student</div>
                <div class="user-role-badge">
                    <span id="sidebarUserRole">STUDENT</span>
                    <span>&bull;</span>
                    <span id="sidebarUserBranch">CSE</span>
                </div>
            </div>
            <a href="javascript:void(0)" onclick="handleLogout()" title="Sign Out" style="background: #fee2e2; border: 1px solid #fecaca; border-radius: var(--radius-sm); padding: 0.4rem 0.6rem; cursor: pointer; color: #991b1b; font-size: 0.85rem; font-weight: 600; display: flex; align-items: center; gap: 0.25rem; text-decoration: none;" aria-label="Sign out">
                <span>&#128682;</span>
                <span style="font-size: 0.75rem;">Exit</span>
            </a>
        </div>
    </div>
</aside>
<script>
    (function() {
        const path = window.location.pathname;
        const navMap = {
            'dashboard': 'nav-dashboard',
            'tests': 'nav-tests',
            'interview': 'nav-interview',
            'resume': 'nav-resume',
            'ai-buddy': 'nav-ai-buddy',
            'jobs': 'nav-jobs',
            'progress': 'nav-progress',
            'profile': 'nav-profile'
        };
        for (const [key, id] of Object.entries(navMap)) {
            if (path.includes(key)) {
                const el = document.getElementById(id);
                if (el) el.classList.add('active');
            }
        }
        function updateProfile(u) {
            if (!u) return;
            const name = u.displayName || u.name || (u.email ? u.email.split('@')[0] : 'Student');
            const nameEl = document.getElementById('sidebarUserName');
            const avatarEl = document.getElementById('sidebarUserAvatar');
            if (nameEl) nameEl.textContent = name;
            if (avatarEl) {
                if (u.photoURL || u.photoUrl) {
                    avatarEl.innerHTML = '<img src="' + (u.photoURL || u.photoUrl) + '" style="width:100%;height:100%;border-radius:50%;object-fit:cover;" />';
                } else {
                    avatarEl.textContent = name.charAt(0).toUpperCase();
                }
            }
        }
        try {
            const cached = localStorage.getItem('currentUser');
            if (cached) updateProfile(JSON.parse(cached));
        } catch(e){}
        if (typeof firebase !== 'undefined' && firebase.auth) {
            firebase.auth().onAuthStateChanged(function(user) {
                if (user) {
                    updateProfile(user);
                    try { localStorage.setItem('currentUser', JSON.stringify({ uid: user.uid, email: user.email, name: user.displayName, photoUrl: user.photoURL })); } catch(e){}
                }
            });
        }
    })();
</script>`;
}

const preparedHead = cleanHead(headFrag);
const preparedNavbar = cleanNavbar(navbarFrag);
const preparedFooter = cleanFooter(footerFrag);
const preparedSidebar = cleanSidebar(sidebarFrag);

// Helper to compile any JSP to HTML
function compileJspToHtml(jspContent, options = {}) {
  let html = jspContent;

  // Insert fragment includes
  html = html.replace(/<jsp:include\s+page=["']\/WEB-INF\/fragments\/head\.jsp["']\s*\/?>/g, preparedHead);
  html = html.replace(/<jsp:include\s+page=["']\/WEB-INF\/fragments\/navbar\.jsp["']\s*\/?>/g, preparedNavbar);
  html = html.replace(/<jsp:include\s+page=["']\/WEB-INF\/fragments\/footer\.jsp["']\s*\/?>/g, preparedFooter);
  html = html.replace(/<jsp:include\s+page=["']\/WEB-INF\/fragments\/sidebar\.jsp["']\s*\/?>/g, preparedSidebar);

  // Strip page directives
  html = html.replace(/<%@\s*page[^%>]*%>/g, '');

  // Strip scriptlet blocks (e.g. imports, server variable declarations)
  html = html.replace(/<%[\s\S]*?%>/g, (match) => {
    // If it's an expression <%= ... %>
    if (match.startsWith('<%=')) {
      const expr = match.slice(3, -2).trim();
      if (expr.includes('System.currentTimeMillis()')) return String(Date.now());
      if (expr.includes('userName') || expr.includes('user.getName()')) return 'Student';
      if (expr.includes('overallScore') || expr.includes('atsScore')) return '85';
      if (expr.includes('status') || expr.includes('aiWelcome')) return 'Ready for Placements';
      return '';
    }
    return '';
  });

  // Replace context path
  html = html.replace(/\$\{pageContext\.request\.contextPath\}/g, '');

  // Normalize JSP links to clean URLs for static hosting
  html = html.replace(/href=["'](\/?)index\.jsp["']/g, 'href="$1"');
  html = html.replace(/href=["'](\/?)student-dashboard\.jsp["']/g, 'href="$1dashboard"');
  html = html.replace(/href=["'](\/?)resume-analysis\.jsp["']/g, 'href="$1resume"');
  html = html.replace(/href=["'](\/?)faculty-dashboard\.jsp["']/g, 'href="$1faculty"');
  html = html.replace(/href=["'](\/?)admin-dashboard\.jsp["']/g, 'href="$1admin"');
  html = html.replace(/href=["'](\/?)([a-zA-Z0-9_-]+)\.jsp["']/g, 'href="$1$2"');

  return html;
}

// 3. Compile all pages
const pages = [
  { src: 'index.jsp', dest: 'index.html' },
  { src: 'login.jsp', dest: 'login.html' },
  { src: 'register.jsp', dest: 'register.html' },
  { src: 'forgot-password.jsp', dest: 'forgot-password.html' },
  { src: 'student-dashboard.jsp', dest: 'student-dashboard.html' },
  { src: 'student-dashboard.jsp', dest: 'dashboard.html' },
  { src: 'interview.jsp', dest: 'interview.html' },
  { src: 'tests.jsp', dest: 'tests.html' },
  { src: 'test-result.jsp', dest: 'test-result.html' },
  { src: 'resume-analysis.jsp', dest: 'resume-analysis.html' },
  { src: 'resume-analysis.jsp', dest: 'resume.html' },
  { src: 'jobs.jsp', dest: 'jobs.html' },
  { src: 'ai-buddy.jsp', dest: 'ai-buddy.html' },
  { src: 'progress.jsp', dest: 'progress.html' },
  { src: 'profile.jsp', dest: 'profile.html' },
  { src: 'faculty-dashboard.jsp', dest: 'faculty.html' },
  { src: 'admin-dashboard.jsp', dest: 'admin.html' }
];

for (const p of pages) {
  const srcFile = path.resolve(webappDir, p.src);
  if (fs.existsSync(srcFile)) {
    const raw = fs.readFileSync(srcFile, 'utf-8');
    const compiled = compileJspToHtml(raw);
    const destFile = path.resolve(distDir, p.dest);
    fs.writeFileSync(destFile, compiled, 'utf-8');
    console.log(`✓ Compiled ${p.src} -> dist/${p.dest}`);
  }
}

// Also compile react-portal if client exists
const clientDist = path.resolve(rootDir, 'client/dist');
if (fs.existsSync(clientDist)) {
  fs.cpSync(clientDist, path.resolve(distDir, 'portal'), { recursive: true });
  fs.copyFileSync(path.resolve(clientDist, 'index.html'), path.resolve(distDir, 'portal.html'));
  console.log('✓ Copied React Portal to dist/portal.html and dist/portal/');
}

// 4. Generate Netlify _redirects file for clean URL handling
const redirects = `
# Canonical clean URLs
/index.jsp         /index.html            200
/login.jsp         /login.html            200
/register.jsp      /register.html         200
/forgot-password.jsp /forgot-password.html 200
/student-dashboard.jsp /dashboard.html     200
/interview.jsp     /interview.html        200
/tests.jsp         /tests.html            200
/test-result.jsp   /test-result.html      200
/resume-analysis.jsp /resume.html         200
/jobs.jsp          /jobs.html             200
/ai-buddy.jsp      /ai-buddy.html         200
/progress.jsp      /progress.html         200
/profile.jsp       /profile.html          200

# Route mappings without extension
/login             /login.html            200
/register          /register.html         200
/forgot-password   /forgot-password.html  200
/dashboard         /dashboard.html        200
/interview         /interview.html        200
/tests             /tests.html            200
/test-result       /test-result.html      200
/resume            /resume.html           200
/jobs              /jobs.html             200
/ai-buddy          /ai-buddy.html         200
/progress          /progress.html         200
/profile           /profile.html          200
/faculty           /faculty.html          200
/admin             /admin.html            200
/portal            /portal.html           200
/react-portal      /portal.html           200

# Default SPA fallback to landing page
/*                 /index.html            200
`;

fs.writeFileSync(path.resolve(distDir, '_redirects'), redirects.trim(), 'utf-8');
console.log('✓ Created dist/_redirects with full navigation rules');

console.log('🎉 Real TPO Buddy static website successfully compiled to dist/');
