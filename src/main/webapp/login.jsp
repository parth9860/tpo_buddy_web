<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Sign In — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
</head>
<body style="display: flex; flex-direction: column; min-height: 100vh;">
    <div class="bg-grid-overlay"></div>

    <jsp:include page="/WEB-INF/fragments/navbar.jsp" />

    <main style="flex: 1; display: flex; align-items: center; justify-content: center; padding: 3rem 1.5rem;">
        <div class="glass-card animate-fade-in-up" style="width: 100%; max-width: 440px; padding: 2.5rem 2rem;">
            <div style="text-align: center; margin-bottom: 2rem;">
                <div class="logo-orb" style="width: 44px; height: 44px; margin: 0 auto 1rem; font-size: 1.1rem;">TB</div>
                <h2 style="font-size: 1.75rem; margin-bottom: 0.5rem;">Welcome Back</h2>
                <p style="font-size: 0.9rem; margin: 0;">Sign in to your synchronized TPO Buddy account</p>
            </div>

            <form id="loginForm" onsubmit="handleLogin(event)">
                <div class="form-group">
                    <label for="email" class="form-label">Email Address</label>
                    <input type="email" id="email" class="form-control" placeholder="name@college.edu" required autocomplete="email">
                </div>

                <div class="form-group">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                        <label for="password" class="form-label" style="margin: 0;">Password</label>
                        <a href="${pageContext.request.contextPath}/forgot-password.jsp" style="font-size: 0.8rem;">Forgot Password?</a>
                    </div>
                    <div style="position: relative;">
                        <input type="password" id="password" class="form-control" placeholder="••••••••" required style="padding-right: 4rem;">
                        <button type="button" onclick="togglePasswordVisibility('password', this)" style="position: absolute; right: 0.75rem; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--text-muted); font-size: 0.8rem; cursor: pointer;">Show</button>
                    </div>
                </div>

                <button type="submit" id="btnSubmit" class="btn btn-primary" style="width: 100%; padding: 0.85rem; margin-top: 0.5rem;">
                    Sign In
                </button>
            </form>

            <!-- Clean Divider: OR CONTINUE WITH -->
            <div class="social-divider" aria-hidden="true">
                <span>OR CONTINUE WITH</span>
            </div>

            <!-- Social Authentication Buttons (Google & Facebook) -->
            <div class="social-auth-buttons">
                <!-- Option 2: Continue with Google -->
                <button type="button" id="btnGoogle" class="social-btn social-btn-google" onclick="handleSocialLogin('google')" title="Sign in with your Google account" aria-label="Continue with Google">
                    <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                        <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"/>
                        <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.33 24 12 24z"/>
                        <path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 9.99 0 12s.45 3.82 1.25 5.42l4.03-3.15z"/>
                        <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/>
                    </svg>
                    <span class="btn-text">Continue with Google</span>
                </button>

                <!-- Option 3: Continue with Facebook -->
                <button type="button" id="btnFacebook" class="social-btn social-btn-facebook" onclick="handleSocialLogin('facebook')" title="Sign in with your Facebook account" aria-label="Continue with Facebook">
                    <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                        <path fill="#1877F2" d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                    </svg>
                    <span class="btn-text">Continue with Facebook</span>
                </button>
            </div>

            <!-- Live Firebase Verified Accounts for Testing -->
            <div style="margin-top: 1.5rem; padding: 1rem; background: rgba(99, 102, 241, 0.08); border: 1px dashed rgba(99, 102, 241, 0.35); border-radius: 12px; text-align: center;">
                <div style="font-size: 0.8rem; font-weight: 700; color: var(--primary); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.5rem; display: flex; align-items: center; justify-content: center; gap: 0.35rem;">
                    <span>&#9889;</span> Live Firebase Accounts (1-Click Sign In)
                </div>
                <div style="display: flex; gap: 0.5rem; justify-content: center; flex-wrap: wrap;">
                    <button type="button" onclick="fillAndSubmitAccount('student@tpo.com', 'password123')" class="btn btn-sm" style="background: #3b82f6; color: white; padding: 0.4rem 0.75rem; font-size: 0.8rem; border-radius: 6px; border: none; cursor: pointer; font-weight: 600;">Student (Alex)</button>
                    <button type="button" onclick="fillAndSubmitAccount('teacher@tpo.com', 'password123')" class="btn btn-sm" style="background: #10b981; color: white; padding: 0.4rem 0.75rem; font-size: 0.8rem; border-radius: 6px; border: none; cursor: pointer; font-weight: 600;">Faculty (Prof. Smith)</button>
                    <button type="button" onclick="fillAndSubmitAccount('admin@tpo.com', 'admin123')" class="btn btn-sm" style="background: #8b5cf6; color: white; padding: 0.4rem 0.75rem; font-size: 0.8rem; border-radius: 6px; border: none; cursor: pointer; font-weight: 600;">Admin Console</button>
                </div>
            </div>

            <script>
                function fillAndSubmitAccount(email, pass) {
                    const emailInput = document.getElementById('email');
                    const passInput = document.getElementById('password');
                    if (emailInput && passInput) {
                        emailInput.value = email;
                        passInput.value = pass;
                        const form = document.getElementById('loginForm');
                        if (form) {
                            form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
                        }
                    }
                }
            </script>

            <div style="text-align: center; margin-top: 1.5rem; font-size: 0.875rem; color: var(--text-secondary);">
                Don't have an account?
                <a href="${pageContext.request.contextPath}/register.jsp" style="font-weight: 600;">Create Account</a>
            </div>
        </div>

        <!-- Secure Account Linking Modal -->
        <div id="accountLinkingModal" class="modal-overlay" style="display: none; position: fixed; inset: 0; background: rgba(15, 23, 42, 0.65); backdrop-filter: blur(4px); z-index: 1000; align-items: center; justify-content: center; padding: 1.5rem;">
            <div class="glass-card animate-fade-in-up" style="background: #ffffff; width: 100%; max-width: 460px; padding: 2rem; border-radius: var(--radius-md); box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);">
                <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem;">
                    <div style="width: 40px; height: 40px; border-radius: 50%; background: #eff6ff; display: flex; align-items: center; justify-content: center; font-size: 1.25rem;">🔗</div>
                    <div>
                        <h3 style="margin: 0; font-size: 1.2rem; color: var(--text-primary);">Link Your Account</h3>
                        <div style="font-size: 0.8rem; color: var(--text-muted);">Secure Provider Synchronization</div>
                    </div>
                </div>
                <p id="accountLinkingMsg" style="font-size: 0.875rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 1.25rem;">
                    An account already exists with this email address. Please authenticate using your original sign-in method to link them securely.
                </p>
                <form id="accountLinkingForm" onsubmit="handleAccountLinkingSubmit(event)">
                    <div class="form-group" id="linkingPasswordGroup">
                        <label for="linkPassword" class="form-label">Existing Account Password</label>
                        <input type="password" id="linkPassword" class="form-control" placeholder="••••••••" autocomplete="current-password">
                    </div>
                    <div style="display: flex; gap: 0.75rem; margin-top: 1.5rem;">
                        <button type="button" class="btn btn-secondary btn-sm" onclick="closeAccountLinkingModal()" style="flex: 1;">Cancel</button>
                        <button type="submit" id="btnConfirmLinking" class="btn btn-primary btn-sm" style="flex: 1;">Verify & Link</button>
                    </div>
                </form>
            </div>
        </div>
    </main>

    <jsp:include page="/WEB-INF/fragments/footer.jsp" />
</body>
</html>
