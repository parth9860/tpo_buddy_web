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
  return content
    .replace(/<%@\s*page[^%>]*%>/g, '')
    .replace(/\$\{pageContext\.request\.contextPath\}/g, '')
    .replace(/<%= currentUser[^%]*%>/g, '')
    .replace(/<%= currentPath[^%]*%>/g, '')
    .replace(/<%= userName %>/g, 'Student')
    .replace(/<%= userRole %>/g, 'STUDENT')
    .replace(/<%= userBranch %>/g, 'CSE')
    .replace(/<%= initial %>/g, 'S')
    .replace(/<%[\s\S]*?%>/g, '');
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
      if (expr.includes('user != null ? user.getName()') || expr.includes('userName') || expr.includes('user.getName()')) return 'Student';
      if (expr.includes('user != null ? user.getBranch()')) return 'CSE';
      if (expr.includes('user != null ? user.getYear()')) return 'Final Year';
      if (expr.includes('user != null ? user.getCollege()')) return "SNJB's Late Sau KBJ College of Engineering";
      if (expr.includes('overallScore') || expr.includes('readinessVal')) return '82';
      if (expr.includes('strokeOffset')) return '61.07';
      if (expr.includes('atsScore') || expr.includes('resumeScore')) return '82';
      if (expr.includes('status') || expr.includes('readiness.status')) return 'Placement Ready';
      if (expr.includes('aiWelcome')) return 'Welcome to TPO Buddy! Explore active placement drives, optimize your resume ATS, and practice AI mock interviews.';
      if (expr.includes('aiSummary') || expr.includes('readiness.aiSummary')) return 'Complete practice tests in DSA and Quantitative Aptitude to elevate your readiness for campus placement drives.';
      if (expr.includes('javaSkill')) return '75';
      if (expr.includes('dsaSkill')) return '70';
      if (expr.includes('sqlSkill')) return '80';
      if (expr.includes('aptitudeSkill')) return '78';
      if (expr.includes('desiredRole')) return 'Software Development Engineer';
      if (expr.includes('targetSalary')) return '₹6 - 12 LPA';
      if (expr.includes('preferredLocation')) return 'Pune / Mumbai';
      if (expr.includes('jobs != null ? jobs.size()') || expr.includes('jobs.size()')) return '3';
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
