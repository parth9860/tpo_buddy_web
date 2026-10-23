<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Register Student Account — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
</head>
<body style="display: flex; flex-direction: column; min-height: 100vh;">
    <div class="bg-grid-overlay"></div>

    <jsp:include page="/WEB-INF/fragments/navbar.jsp" />

    <main style="flex: 1; display: flex; align-items: center; justify-content: center; padding: 3rem 1.5rem;">
        <div class="glass-card animate-fade-in-up" style="width: 100%; max-width: 500px; padding: 2.5rem 2rem;">
            <div style="text-align: center; margin-bottom: 2rem;">
                <div class="logo-orb" style="width: 44px; height: 44px; margin: 0 auto 1rem; font-size: 1.1rem;">TB</div>
                <h2 style="font-size: 1.75rem; margin-bottom: 0.5rem;">Student Registration</h2>
                <p style="font-size: 0.9rem; margin: 0;">Create your account synchronized with TPO Buddy</p>
            </div>

            <form id="registerForm" onsubmit="handleRegister(event)">
                <div class="form-group">
                    <label for="name" class="form-label">Full Name</label>
                    <input type="text" id="name" class="form-control" placeholder="Alex Johnson" required autocomplete="name">
                </div>

                <div class="form-group">
                    <label for="email" class="form-label">College Email Address</label>
                    <input type="email" id="email" class="form-control" placeholder="alex.johnson@college.edu" required autocomplete="email">
                </div>

                <div class="form-group">
                    <label for="password" class="form-label">Password</label>
                    <div style="position: relative;">
                        <input type="password" id="password" class="form-control" placeholder="Minimum 6 characters" minlength="6" required style="padding-right: 4rem;">
                        <button type="button" onclick="togglePasswordVisibility('password', this)" style="position: absolute; right: 0.75rem; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--text-muted); font-size: 0.8rem; cursor: pointer;">Show</button>
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                    <div class="form-group">
                        <label for="branch" class="form-label">Department / Branch</label>
                        <select id="branch" class="form-control">
                            <option value="CSE" selected>Computer Science (CSE)</option>
                            <option value="IT">Information Tech (IT)</option>
                            <option value="ECE">Electronics (ECE)</option>
                            <option value="ME">Mechanical (ME)</option>
                            <option value="CE">Civil Eng (CE)</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label for="year" class="form-label">Academic Year</label>
                        <select id="year" class="form-control">
                            <option value="Final Year" selected>Final Year (4th)</option>
                            <option value="Third Year">Third Year (3rd)</option>
                            <option value="Second Year">Second Year (2nd)</option>
                            <option value="First Year">First Year (1st)</option>
                        </select>
                    </div>
                </div>

                <div class="form-group">
                    <label for="college" class="form-label">Institution / College Name</label>
                    <input type="text" id="college" class="form-control" placeholder="National Institute of Technology" value="National Institute of Technology">
                </div>

                <button type="submit" id="btnSubmit" class="btn btn-primary" style="width: 100%; padding: 0.85rem; margin-top: 0.5rem;">
                    Create Student Account
                </button>
            </form>

            <!-- Clean Divider: OR CONTINUE WITH -->
            <div class="social-divider" aria-hidden="true">
                <span>OR CONTINUE WITH</span>
            </div>

            <!-- Social Authentication Buttons -->
            <div class="social-auth-buttons">
                <!-- Continue with Google -->
                <button type="button" id="btnGoogle" class="social-btn social-btn-google" onclick="handleSocialLogin('google')" title="Sign up with your Google account" aria-label="Continue with Google">
                    <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                        <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"/>
                        <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.33 24 12 24z"/>
                        <path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 9.99 0 12s.45 3.82 1.25 5.42l4.03-3.15z"/>
                        <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/>
                    </svg>
                    <span class="btn-text">Continue with Google</span>
                </button>

                <!-- Continue with Facebook -->
                <button type="button" id="btnFacebook" class="social-btn social-btn-facebook" onclick="handleSocialLogin('facebook')" title="Sign up with your Facebook account" aria-label="Continue with Facebook">
                    <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                        <path fill="#1877F2" d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                    </svg>
                    <span class="btn-text">Continue with Facebook</span>
                </button>

                <!-- Continue with Apple -->
                <button type="button" id="btnApple" class="social-btn social-btn-apple" onclick="handleSocialLogin('apple')" title="Sign up with your Apple ID" aria-label="Continue with Apple">
                    <svg class="social-icon social-icon-apple" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                        <path fill="#000000" d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M15.97 6.42c.62-.75 1.04-1.8 0.92-2.85-.9.04-2 .6-2.65 1.35-.58.66-1.08 1.73-.95 2.76 1.01.08 2.06-.51 2.68-1.26z"/>
                    </svg>
                    <span class="btn-text">Continue with Apple</span>
                </button>
            </div>

            <div style="text-align: center; margin-top: 1.75rem; font-size: 0.875rem; color: var(--text-secondary);">
                Already have an account?
                <a href="${pageContext.request.contextPath}/login.jsp" style="font-weight: 600;">Sign In</a>
            </div>
        </div>
    </main>

    <jsp:include page="/WEB-INF/fragments/footer.jsp" />
</body>
</html>
