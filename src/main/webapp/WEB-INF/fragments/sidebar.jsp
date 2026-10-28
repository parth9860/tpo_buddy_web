<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.tpobuddy.model.User" %>
<%
    User currentUser = (User) session.getAttribute("currentUser");
    String currentPath = request.getServletPath();
    String userName = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : "Student";
    String userRole = (currentUser != null && currentUser.getRole() != null) ? currentUser.getRole().toUpperCase() : "STUDENT";
    String userBranch = (currentUser != null && currentUser.getBranch() != null) ? currentUser.getBranch() : "CSE";
    String initial = (userName.length() > 0) ? userName.substring(0, 1).toUpperCase() : "S";
%>
<aside class="app-sidebar collapsed" id="appSidebar" aria-label="Main Navigation">
    <div class="sidebar-header">
        <a href="${pageContext.request.contextPath}/dashboard" class="sidebar-brand" title="TPO Buddy">
            <div class="logo-orb">TB</div>
            <span class="sidebar-brand-text">TPO<span style="color: var(--secondary);">Buddy</span></span>
        </a>
        <button type="button" class="sidebar-toggle-btn" id="sidebarToggleBtn" aria-label="Toggle navigation menu" title="Expand menu (⋮)" aria-expanded="false">
            <span class="toggle-dots">⋮</span>
        </button>
        <button type="button" class="sidebar-close-btn" id="sidebarCloseBtn" aria-label="Close menu" title="Close menu" style="display: none;">✕</button>
    </div>

    <ul class="sidebar-nav">
        <li>
            <a href="${pageContext.request.contextPath}/dashboard" class="nav-item-link <%= currentPath.contains("dashboard") && !currentPath.contains("faculty") && !currentPath.contains("admin") ? "active" : "" %>" id="nav-dashboard" title="Dashboard">
                <span class="nav-icon">&#128202;</span>
                <span class="nav-item-text">Dashboard</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/tests" class="nav-item-link <%= currentPath.contains("tests") ? "active" : "" %>" id="nav-tests" title="AI Test Series">
                <span class="nav-icon">&#127919;</span>
                <span class="nav-item-text">AI Test Series</span>
                <span class="badge nav-item-badge" style="font-size: 0.65rem; background: #2563eb; color: #fff; padding: 2px 7px; border-radius: 9999px; font-weight: 700;">Live</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/interview" class="nav-item-link <%= currentPath.contains("interview") ? "active" : "" %>" id="nav-interview" title="AI Mock Interview">
                <span class="nav-icon">&#127897;</span>
                <span class="nav-item-text">AI Mock Interview</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/resume" class="nav-item-link <%= currentPath.contains("resume") ? "active" : "" %>" id="nav-resume" title="AI Resume ATS">
                <span class="nav-icon">&#128196;</span>
                <span class="nav-item-text">AI Resume ATS</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/ai-buddy" class="nav-item-link <%= currentPath.contains("ai-buddy") ? "active" : "" %>" id="nav-ai-buddy" title="Career Copilot AI">
                <span class="nav-icon">&#129302;</span>
                <span class="nav-item-text">Career Copilot AI</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/jobs" class="nav-item-link <%= currentPath.contains("jobs") ? "active" : "" %>" id="nav-jobs" title="Placement Jobs">
                <span class="nav-icon">&#128188;</span>
                <span class="nav-item-text">Placement Jobs</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/progress" class="nav-item-link <%= currentPath.contains("progress") ? "active" : "" %>" id="nav-progress" title="My Progress">
                <span class="nav-icon">&#128640;</span>
                <span class="nav-item-text">My Progress</span>
            </a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/profile" class="nav-item-link <%= currentPath.contains("profile") ? "active" : "" %>" id="nav-profile" title="Profile">
                <span class="nav-icon">&#128100;</span>
                <span class="nav-item-text">Profile</span>
            </a>
        </li>

        <li style="margin-top: 0.75rem; border-top: 1px solid var(--border-subtle); padding-top: 0.5rem; display: none;" id="nav-faculty-item">
            <a href="${pageContext.request.contextPath}/faculty" class="nav-item-link <%= currentPath.contains("faculty") ? "active" : "" %>" id="nav-faculty" title="Faculty / TPO Portal">
                <span class="nav-icon">&#127891;</span>
                <span class="nav-item-text">Faculty Portal</span>
            </a>
        </li>

        <li style="display: none;" id="nav-admin-item">
            <a href="${pageContext.request.contextPath}/admin" class="nav-item-link <%= currentPath.contains("admin") ? "active" : "" %>" id="nav-admin" title="Admin Console">
                <span class="nav-icon">&#128737;</span>
                <span class="nav-item-text">Admin Console</span>
            </a>
        </li>

        <li style="margin-top: auto; padding-top: 1rem;">
            <a href="javascript:void(0)" onclick="handleLogout()" class="nav-item-link" style="color: var(--accent-rose);" title="Sign Out">
                <span class="nav-icon">&#128682;</span>
                <span class="nav-item-text">Logout</span>
            </a>
        </li>
    </ul>

    <div class="sidebar-footer">
        <div class="user-snippet">
            <div class="user-avatar" id="sidebarUserAvatar">
                <% if (currentUser != null && currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isEmpty()) { %>
                    <img src="<%= currentUser.getPhotoUrl() %>" alt="<%= userName %>" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;" onerror="this.style.display='none'; if(this.nextElementSibling) this.nextElementSibling.style.display='inline';">
                    <span style="display: none;"><%= initial %></span>
                <% } else { %>
                    <%= initial %>
                <% } %>
            </div>
            <div class="user-details">
                <div class="user-name" id="sidebarUserName"><%= userName %></div>
                <div class="user-role-badge">
                    <span id="sidebarUserRole"><%= userRole %></span>
                    <span>&bull;</span>
                    <span id="sidebarUserBranch"><%= userBranch %></span>
                </div>
            </div>
            <a href="javascript:void(0)" onclick="handleLogout()" class="user-exit-btn" title="Sign Out" style="background: #fee2e2; border: 1px solid #fecaca; border-radius: var(--radius-sm); padding: 0.4rem 0.6rem; cursor: pointer; color: #991b1b; font-size: 0.85rem; font-weight: 600; display: flex; align-items: center; gap: 0.25rem; text-decoration: none;" aria-label="Sign out">
                <span>&#128682;</span>
                <span style="font-size: 0.75rem;">Exit</span>
            </a>
        </div>
    </div>
</aside>
<div class="sidebar-backdrop" id="sidebarBackdrop"></div>
<div class="mobile-header-bar">
    <button type="button" class="mobile-nav-toggle-btn" id="mobileMenuBtn" aria-label="Open navigation menu">
        <span>☰</span>
    </button>
    <a href="${pageContext.request.contextPath}/dashboard" class="mobile-brand">
        <div class="logo-orb" style="width: 32px; height: 32px; font-size: 0.9rem;">TB</div>
        <span style="font-weight: 700; color: var(--text-primary);">TPO<span style="color: var(--secondary);">Buddy</span></span>
    </a>
    <button type="button" class="btn btn-outline btn-sm" onclick="handleLogout()" style="padding: 0.25rem 0.6rem; font-size: 0.8rem;">Exit</button>
</div>

<script>
    (function initAnimatedSidebar() {
        const sidebar = document.getElementById('appSidebar');
        const toggleBtn = document.getElementById('sidebarToggleBtn');
        const closeBtn = document.getElementById('sidebarCloseBtn');
        const backdrop = document.getElementById('sidebarBackdrop');
        const mobileMenuBtn = document.getElementById('mobileMenuBtn');

        // Active route highlight
        const path = window.location.pathname.toLowerCase();
        const navMap = {
            'dashboard': 'nav-dashboard',
            'tests': 'nav-tests',
            'interview': 'nav-interview',
            'resume': 'nav-resume',
            'ai-buddy': 'nav-ai-buddy',
            'jobs': 'nav-jobs',
            'progress': 'nav-progress',
            'profile': 'nav-profile',
            'faculty': 'nav-faculty',
            'admin': 'nav-admin'
        };
        for (const [key, id] of Object.entries(navMap)) {
            if (path.includes(key)) {
                const el = document.getElementById(id);
                if (el) el.classList.add('active');
            }
        }

        // Sidebar state handling: Start collapsed as compact rail with three-dot toggle
        if (sidebar) {
            sidebar.classList.add('collapsed');
            document.body.classList.add('sidebar-collapsed');
        }

        function toggleSidebar() {
            if (!sidebar) return;
            const isCollapsed = sidebar.classList.contains('collapsed');
            if (isCollapsed) {
                sidebar.classList.remove('collapsed');
                sidebar.classList.add('expanded');
                document.body.classList.remove('sidebar-collapsed');
                if (toggleBtn) {
                    toggleBtn.innerHTML = '<span class="toggle-dots">◀</span>';
                    toggleBtn.setAttribute('title', 'Collapse menu');
                    toggleBtn.setAttribute('aria-expanded', 'true');
                }
            } else {
                sidebar.classList.add('collapsed');
                sidebar.classList.remove('expanded');
                document.body.classList.add('sidebar-collapsed');
                if (toggleBtn) {
                    toggleBtn.innerHTML = '<span class="toggle-dots">⋮</span>';
                    toggleBtn.setAttribute('title', 'Expand menu (⋮)');
                    toggleBtn.setAttribute('aria-expanded', 'false');
                }
            }
        }

        function openMobileSidebar() {
            if (sidebar) sidebar.classList.add('mobile-open');
            if (backdrop) backdrop.classList.add('active');
            document.body.style.overflow = 'hidden';
        }

        function closeMobileSidebar() {
            if (sidebar) sidebar.classList.remove('mobile-open');
            if (backdrop) backdrop.classList.remove('active');
            document.body.style.overflow = '';
        }

        window.openMobileSidebar = openMobileSidebar;
        window.closeMobileSidebar = closeMobileSidebar;

        if (toggleBtn) {
            toggleBtn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                toggleSidebar();
            });
        }

        if (closeBtn) {
            closeBtn.addEventListener('click', (e) => {
                e.preventDefault();
                closeMobileSidebar();
            });
        }

        if (backdrop) {
            backdrop.addEventListener('click', closeMobileSidebar);
        }

        if (mobileMenuBtn) {
            mobileMenuBtn.addEventListener('click', (e) => {
                e.preventDefault();
                openMobileSidebar();
            });
        }

        // Close mobile drawer on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && sidebar && sidebar.classList.contains('mobile-open')) {
                closeMobileSidebar();
            }
        });

        // When user selects a feature: smoothly collapse to rail, navigate
        const navLinks = document.querySelectorAll('.app-sidebar .nav-item-link');
        navLinks.forEach(link => {
            link.addEventListener('click', function(e) {
                const href = this.getAttribute('href');
                if (!href || href.startsWith('javascript:')) return;
                
                // If on mobile: close drawer
                if (window.innerWidth <= 768) {
                    closeMobileSidebar();
                } else if (sidebar && !sidebar.classList.contains('collapsed')) {
                    // Automatically collapse back into compact rail state upon destination selection
                    sidebar.classList.add('collapsed');
                    sidebar.classList.remove('expanded');
                    document.body.classList.add('sidebar-collapsed');
                    if (toggleBtn) {
                        toggleBtn.innerHTML = '<span class="toggle-dots">⋮</span>';
                        toggleBtn.setAttribute('title', 'Expand menu (⋮)');
                        toggleBtn.setAttribute('aria-expanded', 'false');
                    }
                }
            });
        });

        // User profile synchronization & role visibility
        function updateSidebarUser(u) {
            if (!u) return;
            const name = u.displayName || u.name || (u.email ? u.email.split('@')[0] : 'Student');
            const role = (u.role || 'STUDENT').toUpperCase();
            const branch = u.branch || 'CSE';
            const photoUrl = u.photoURL || u.photoUrl;

            const nameEl = document.getElementById('sidebarUserName');
            const roleEl = document.getElementById('sidebarUserRole');
            const branchEl = document.getElementById('sidebarUserBranch');
            const avatarEl = document.getElementById('sidebarUserAvatar');

            if (nameEl) nameEl.textContent = name;
            if (roleEl) roleEl.textContent = role;
            if (branchEl) branchEl.textContent = branch;

            if (avatarEl) {
                if (photoUrl) {
                    avatarEl.innerHTML = '<img src="' + photoUrl + '" style="width:100%;height:100%;border-radius:50%;object-fit:cover;" onerror="this.parentElement.textContent=\'' + name.charAt(0).toUpperCase() + '\'" />';
                } else {
                    avatarEl.textContent = name.charAt(0).toUpperCase();
                }
            }

            // Role based links
            const isFaculty = role === 'TEACHER' || role === 'FACULTY' || role === 'TPO' || role === 'ADMIN';
            const isAdmin = role === 'ADMIN';

            const facItem = document.getElementById('nav-faculty-item');
            const adminItem = document.getElementById('nav-admin-item');
            if (facItem) facItem.style.display = isFaculty ? 'block' : 'none';
            if (adminItem) adminItem.style.display = isAdmin ? 'block' : 'none';
        }

        try {
            const cached = localStorage.getItem('currentUser');
            if (cached) updateSidebarUser(JSON.parse(cached));
        } catch(e){}

        if (typeof firebase !== 'undefined' && firebase.auth) {
            firebase.auth().onAuthStateChanged(function(user) {
                if (user) {
                    let userProfile = { uid: user.uid, email: user.email, name: user.displayName, photoUrl: user.photoURL };
                    try {
                        const local = localStorage.getItem('currentUser');
                        if (local) userProfile = { ...userProfile, ...JSON.parse(local) };
                    } catch(e){}
                    updateSidebarUser(userProfile);

                    // Fetch authoritative role from Realtime Database if available
                    if (typeof database !== 'undefined' && database) {
                        database.ref('users/' + user.uid).once('value').then(snap => {
                            if (snap.exists()) {
                                const val = snap.val();
                                updateSidebarUser(val);
                                try { localStorage.setItem('currentUser', JSON.stringify({ ...userProfile, ...val })); } catch(e){}
                            }
                        }).catch(() => {});
                    }
                }
            });
        }
    })();
</script>
