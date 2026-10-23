<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Reset Password — TPO Buddy</title>
    <jsp:include page="/WEB-INF/fragments/head.jsp" />
</head>
<body style="display: flex; flex-direction: column; min-height: 100vh;">
    <div class="bg-grid-overlay"></div>

    <jsp:include page="/WEB-INF/fragments/navbar.jsp" />

    <main style="flex: 1; display: flex; align-items: center; justify-content: center; padding: 3rem 1.5rem;">
        <div class="glass-card animate-fade-in-up" style="width: 100%; max-width: 440px; padding: 2.5rem 2rem;">
            <div style="text-align: center; margin-bottom: 2rem;">
                <div class="logo-orb" style="width: 44px; height: 44px; margin: 0 auto 1rem; font-size: 1.1rem;">TB</div>
                <h2 style="font-size: 1.75rem; margin-bottom: 0.5rem;">Reset Password</h2>
                <p style="font-size: 0.9rem; margin: 0;">Enter your account email to receive a secure Firebase reset link</p>
            </div>

            <form id="forgotForm" onsubmit="handleForgotPassword(event)">
                <div class="form-group">
                    <label for="email" class="form-label">Registered Email</label>
                    <input type="email" id="email" class="form-control" placeholder="name@college.edu" required autocomplete="email">
                </div>

                <button type="submit" id="btnSubmit" class="btn btn-primary" style="width: 100%; padding: 0.85rem; margin-top: 0.5rem;">
                    Send Reset Link
                </button>
            </form>

            <div style="text-align: center; margin-top: 2rem; font-size: 0.875rem; color: var(--text-secondary);">
                Remembered your password?
                <a href="${pageContext.request.contextPath}/login.jsp" style="font-weight: 600;">Back to Sign In</a>
            </div>
        </div>
    </main>

    <jsp:include page="/WEB-INF/fragments/footer.jsp" />
</body>
</html>
