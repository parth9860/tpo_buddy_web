```css
/* =====================================
   TPO BUDDY ADMIN — BLUE & WHITE THEME
   ===================================== */

:root {
    --adm-bg: #f4f7fc;
    --adm-panel: #ffffff;
    --adm-panel-hover: #f1f6ff;
    --adm-border: #e2e8f0;
    --adm-text: #172b4d;
    --adm-muted: #64748b;
    --adm-primary: #2563eb;
    --adm-green: #059669;
    --adm-orange: #d97706;
    --adm-red: #dc2626;
}

body {
    background: #f4f7fc !important;
    color: #172b4d !important;
}

.bg-grid-overlay {
    display: none !important;
}

.app-shell {
    display: flex;
    align-items: stretch;
    min-height: 100vh;
    background: #f4f7fc;
}

/* Sidebar */

.admin-sidebar {
    width: 255px;
    min-width: 255px;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #ffffff;
    border-right: 1px solid #e2e8f0;
    color: #172b4d;
    position: relative;
    z-index: 10;
}

.admin-brand {
    display: flex;
    align-items: center;
    gap: 12px;
    min-height: 95px;
    padding: 20px 23px;
    border-bottom: 1px solid #edf1f7;
    text-decoration: none;
    color: #172b4d;
    font-size: 21px;
    font-weight: 850;
    letter-spacing: -0.8px;
}

.admin-brand-logo {
    display: grid;
    place-items: center;
    width: 40px;
    height: 40px;
    border-radius: 13px;
    color: #ffffff;
    background: linear-gradient(135deg, #2563eb, #0ea5e9);
    font-size: 14px;
    letter-spacing: 0;
    box-shadow: 0 5px 12px rgba(37, 99, 235, .2);
}

.admin-brand-blue {
    color: #0284c7;
}

.admin-nav-label {
    padding: 23px 22px 10px;
    color: #94a3b8;
    font-size: 10px;
    font-weight: 800;
    letter-spacing: 1.3px;
}

.admin-nav {
    display: flex;
    flex-direction: column;
    gap: 5px;
    padding: 0 12px;
}

.admin-nav-link {
    display: flex;
    align-items: center;
    gap: 12px;
    min-height: 46px;
    padding: 11px 13px;
    border-radius: 9px;
    color: #475569;
    text-decoration: none;
    font-size: 12px;
    font-weight: 650;
    transition: background .2s, color .2s;
}

.admin-nav-link:hover {
    color: #1d4ed8;
    background: #eff6ff;
}

.admin-nav-link.active {
    color: #1d4ed8;
    background: #eaf2ff;
    box-shadow: inset 3px 0 #2563eb;
    font-weight: 800;
}

.nav-icon {
    display: grid;
    place-items: center;
    width: 24px;
    flex-shrink: 0;
    font-size: 18px;
}

.admin-nav-divider {
    height: 1px;
    margin: 12px 8px 0;
    background: #e9eef5;
}

.admin-sidebar-bottom {
    margin-top: auto;
    padding: 16px;
    border-top: 1px solid #e9eef5;
}

.admin-sidebar-user {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 15px;
}

.admin-sidebar-avatar {
    display: grid;
    place-items: center;
    width: 38px;
    height: 38px;
    flex-shrink: 0;
    border-radius: 50%;
    background: #dbeafe;
    color: #1d4ed8;
    font-weight: 800;
}

.admin-sidebar-user-info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.admin-sidebar-user-info strong {
    overflow: hidden;
    color: #172b4d;
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.admin-sidebar-user-info span {
    color: #64748b;
    font-size: 10px;
}

.admin-sidebar-logout {
    width: 100%;
    padding: 11px;
    border: 1px solid #fecaca;
    border-radius: 9px;
    background: #fff7f7;
    color: #dc2626;
    font-size: 12px;
    font-weight: 750;
    cursor: pointer;
}

.admin-sidebar-logout:hover {
    background: #fee2e2;
}

/* Main content */

.admin-main,
.app-main {
    flex: 1;
    min-width: 0;
    padding: 30px;
    background: #f4f7fc;
}

.admin-eyebrow {
    color: #2563eb;
}

.admin-title {
    color: #172b4d;
}

.admin-description,
.admin-section-subtitle,
.admin-stat-note {
    color: #64748b;
}

.admin-profile,
.admin-btn,
.admin-stat-card,
.admin-feature-card,
.admin-table-card,
.admin-test-card {
    background: #ffffff;
    border-color: #e2e8f0;
    box-shadow: 0 2px 8px rgba(15, 23, 42, .025);
}

.admin-profile-name,
.admin-feature-title,
.admin-section-title,
.admin-table-title,
.admin-test-title {
    color: #172b4d;
}

.admin-profile-label {
    color: #64748b;
}

.admin-btn {
    color: #334155;
}

.admin-btn:hover {
    background: #eff6ff;
    color: #1d4ed8;
}

.admin-btn-primary {
    background: #2563eb;
    border-color: #2563eb;
    color: #ffffff;
}

.admin-btn-primary:hover {
    background: #1d4ed8;
    color: #ffffff;
}

.admin-stat-label {
    color: #64748b;
}

.admin-stat-value {
    color: #172b4d;
}

.admin-stat-icon,
.admin-feature-icon {
    background: #eff6ff;
}

.admin-banner {
    background: #eff6ff;
    border-color: #bfdbfe;
}

.admin-banner strong {
    color: #1e40af;
}

.admin-banner p,
.admin-feature-description,
.admin-test-description {
    color: #64748b;
}

.admin-test-card {
    background: linear-gradient(135deg, #ffffff, #f0f6ff);
    border-color: #bfdbfe;
}

.admin-test-pill {
    color: #1d4ed8;
    background: #eff6ff;
    border-color: #bfdbfe;
}

.admin-topic {
    background: #ffffff;
    border-color: #e2e8f0;
}

.admin-topic-name {
    color: #172b4d;
}

.admin-table th {
    color: #64748b;
    background: #f8fafc;
}

.admin-table td {
    color: #334155;
    border-color: #edf1f7;
}

.admin-table tbody tr:hover {
    background: #f8fbff;
}

.admin-user-name {
    color: #172b4d;
}

.admin-search {
    background: #ffffff;
    color: #172b4d;
    border-color: #cbd5e1;
}

.admin-search::placeholder {
    color: #94a3b8;
}

.admin-footer {
    color: #64748b;
    border-color: #e2e8f0;
}

@media (max-width: 800px) {
    .admin-sidebar {
        width: 215px;
        min-width: 215px;
    }

    .admin-main,
    .app-main {
        padding: 20px;
    }
}

@media (max-width: 600px) {
    .admin-sidebar {
        width: 68px;
        min-width: 68px;
    }

    .admin-brand {
        justify-content: center;
        padding: 12px 5px;
    }

    .admin-brand > span:not(.admin-brand-logo),
    .admin-nav-label,
    .admin-nav-link > span:not(.nav-icon),
    .admin-sidebar-bottom .admin-sidebar-user-info,
    .admin-sidebar-logout {
        display: none;
    }

    .admin-nav {
        padding: 0 8px;
    }

    .admin-nav-link {
        justify-content: center;
        padding: 12px 4px;
    }

    .admin-nav-link.active {
        box-shadow: inset 0 -3px #2563eb;
    }

    .admin-sidebar-bottom {
        padding: 10px 5px;
    }

    .admin-sidebar-user {
        justify-content: center;
    }

    .admin-main,
    .app-main {
        padding: 15px;
    }
}
```