<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Derive social sign-in capability directly from verified server configuration
    String firebaseApiKey = com.tpobuddy.util.Config.getFirebaseApiKey();
    boolean firebaseAuthConfigured = firebaseApiKey != null && !firebaseApiKey.trim().isEmpty();
    String firebaseAppId = com.tpobuddy.util.Config.getFirebaseAppId();
    boolean fbFacebookConfigured = firebaseAuthConfigured && (firebaseAppId != null && !firebaseAppId.trim().isEmpty());
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Create Student Account — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
</head>
<body style="display: flex; flex-direction: column; min-height: 100vh;">
    <div class="bg-grid-overlay"></div>

    <jsp:include page="/WEB-INF/fragments/navbar.jsp" />

    <main style="flex: 1; display: flex; align-items: center; justify-content: center; padding: 2.5rem 1.25rem;">
        <div class="glass-card animate-fade-in-up" style="width: 100%; max-width: 520px; padding: 2.5rem 2rem;">
            <div style="text-align: center; margin-bottom: 1.75rem;">
                <div class="logo-orb" style="width: 44px; height: 44px; margin: 0 auto 1rem; font-size: 1.1rem;">TB</div>
                <h2 style="font-size: 1.75rem; margin-bottom: 0.35rem; font-weight: 700;">Student Registration</h2>
                <p style="font-size: 0.9rem; margin: 0; color: var(--text-secondary);">Create your synchronized account on TPO Buddy</p>
            </div>

            <!-- Inline validation & registration error message box -->
            <div id="registerError" class="alert alert-error" role="alert" style="display: none; margin-bottom: 1.25rem; font-size: 0.875rem;"></div>

            <form id="registerForm" onsubmit="handleRegister(event)" novalidate>
                <!-- Full Name -->
                <div class="form-group" style="margin-bottom: 1rem;">
                    <label for="name" class="form-label" style="display: flex; justify-content: space-between;">
                        <span>Full Name <span style="color: var(--danger, #ef4444);">*</span></span>
                        <span id="nameError" class="field-error-hint" style="color: #ef4444; font-size: 0.75rem; display: none;"></span>
                    </label>
                    <input type="text" id="name" name="name" class="form-control" placeholder="Alex Johnson" required autocomplete="name" />
                </div>

                <!-- College Email Address -->
                <div class="form-group" style="margin-bottom: 1rem;">
                    <label for="email" class="form-label" style="display: flex; justify-content: space-between;">
                        <span>College Email Address <span style="color: var(--danger, #ef4444);">*</span></span>
                        <span id="emailError" class="field-error-hint" style="color: #ef4444; font-size: 0.75rem; display: none;"></span>
                    </label>
                    <input type="email" id="email" name="email" class="form-control" placeholder="alex.johnson@college.edu" required autocomplete="email" inputmode="email" />
                </div>

                <!-- Passwords (Password + Confirm Password) -->
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="password" class="form-label">
                            Password <span style="color: var(--danger, #ef4444);">*</span>
                        </label>
                        <div class="password-wrap">
                            <input type="password" id="password" name="password" class="form-control" placeholder="Min. 6 chars" minlength="6" required autocomplete="new-password" />
                            <button type="button" class="password-toggle" onclick="togglePasswordVisibility('password', this)" aria-label="Show password" aria-pressed="false">Show</button>
                        </div>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="confirmPassword" class="form-label">
                            Confirm <span style="color: var(--danger, #ef4444);">*</span>
                        </label>
                        <div class="password-wrap">
                            <input type="password" id="confirmPassword" name="confirmPassword" class="form-control" placeholder="Re-enter password" minlength="6" required autocomplete="new-password" />
                            <button type="button" class="password-toggle" onclick="togglePasswordVisibility('confirmPassword', this)" aria-label="Show password" aria-pressed="false">Show</button>
                        </div>
                    </div>
                </div>

                <!-- Department & Academic Year -->
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="branch" class="form-label">Department / Branch</label>
                        <select id="branch" name="branch" class="form-control">
                            <option value="CSE" selected>Computer Science (CSE)</option>
                            <option value="IT">Information Tech (IT)</option>
                            <option value="AI_DS">AI & Data Science (AIDS)</option>
                            <option value="ECE">Electronics (ECE)</option>
                            <option value="ME">Mechanical (ME)</option>
                            <option value="CE">Civil Eng (CE)</option>
                        </select>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="year" class="form-label">Academic Year</label>
                        <select id="year" name="year" class="form-control">
                            <option value="Final Year" selected>Final Year (4th)</option>
                            <option value="Third Year">Third Year (3rd)</option>
                            <option value="Second Year">Second Year (2nd)</option>
                            <option value="First Year">First Year (1st)</option>
                        </select>
                    </div>
                </div>

                <!-- Institution / College Name -->
                <div class="form-group" style="margin-bottom: 1.25rem;">
                    <label for="college" class="form-label">Institution / College Name</label>
                    <input type="text" id="college" name="college" class="form-control" placeholder="SNJB's Late Sau KBJ College of Engineering" value="SNJB's Late Sau KBJ College of Engineering" />
                </div>

                <!-- Submit Button -->
                <button type="submit" id="btnSubmit" class="btn btn-primary" style="width: 100%; padding: 0.85rem; font-weight: 600; font-size: 0.95rem; border-radius: var(--radius-sm);">
                    <span class="btn-label">Create Student Account</span>
                </button>
            </form>

            <!-- Clean Divider & Social Providers (Google & Facebook) -->
            <% if (firebaseAuthConfigured) { %>
                <div class="social-divider" aria-hidden="true" style="margin: 1.5rem 0 1rem;">
                    <span>OR REGISTER WITH</span>
                </div>

                <div class="social-auth-buttons">
                    <!-- Continue with Google -->
                    <button type="button" id="btnGoogle" class="social-btn social-btn-google" onclick="handleSocialLogin('google')" title="Register with Google account" aria-label="Continue with Google">
                        <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                            <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"/>
                            <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.33 24 12 24z"/>
                            <path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 9.99 0 12s.45 3.82 1.25 5.42l4.03-3.15z"/>
                            <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/>
                        </svg>
                        <span class="btn-text">Continue with Google</span>
                    </button>

                    <!-- Continue with Facebook -->
                    <% if (fbFacebookConfigured) { %>
                    <button type="button" id="btnFacebook" class="social-btn social-btn-facebook" onclick="handleSocialLogin('facebook')" title="Register with Facebook account" aria-label="Continue with Facebook">
                        <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                            <path fill="#1877F2" d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                        </svg>
                        <span class="btn-text">Continue with Facebook</span>
                    </button>
                    <% } %>
                </div>
            <% } %>

            <!-- Sign In Link -->
            <div style="text-align: center; margin-top: 1.5rem; font-size: 0.875rem; color: var(--text-secondary);">
                Already have an account?
                <a href="${pageContext.request.contextPath}/login.jsp" style="font-weight: 600; color: var(--primary);">Sign In</a>
            </div>

            <!-- Security & Placement Guidelines Disclaimer -->
            <div style="text-align: center; margin-top: 1rem; font-size: 0.75rem; color: var(--text-muted); line-height: 1.4;">
                Accounts are assigned the standard student placement role. Credentials and data are secured with Firebase Authentication and institutional integrity policies.
            </div>
        </div>
    </main>

    <jsp:include page="/WEB-INF/fragments/footer.jsp" />
</body>
</html>
