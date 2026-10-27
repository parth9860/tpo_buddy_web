/**
 * TPO Buddy — Authentication Client Module
 * Manages Firebase Web SDK auth and Java Servlet session synchronization.
 */

// Show toast notification
function showToast(message, type = 'success') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <span style="font-size: 1.25rem;">${type === 'success' ? '✓' : '⚠'}</span>
        <div>${message}</div>
    `;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(40px)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Toggle password visibility
function togglePasswordVisibility(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    if (input.type === 'password') {
        input.type = 'text';
        btn.textContent = 'Hide';
    } else {
        input.type = 'password';
        btn.textContent = 'Show';
    }
}

// Promise timeout wrapper to prevent hanging operations
function withTimeout(promise, ms, errorMsg = 'Operation timed out') {
    return Promise.race([
        promise,
        new Promise((_, reject) => setTimeout(() => reject(new Error(errorMsg)), ms))
    ]);
}

// Basic HTML escaping helper for safe inline display
function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, m => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[m]);
}

// Sync verified Firebase ID Token with Java Servlet session
// Returns a structured result object so callers can reliably manage UI state.
async function syncSessionWithServer(user, role, branch, name, photoUrl, authProvider, college, year) {
    const result = { success: false, redirect: null, error: null };

    if (!user) {
        result.error = 'Authentication session is missing. Please sign in again.';
        return result;
    }

    try {
        const idToken = await user.getIdToken();
        const payload = {
            idToken: idToken,
            uid: user.uid,
            email: user.email || '',
            name: name || user.displayName || (user.email ? user.email.split('@')[0] : 'Student'),
            role: role || 'student',
            branch: branch || 'CSE',
            college: college || "SNJB's Late Sau KBJ College of Engineering",
            year: year || 'Final Year',
            photoUrl: photoUrl || user.photoURL || '',
            authProvider: authProvider || 'password'
        };

        // Request timeout to prevent hanging network calls
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 15000);
        let data = null;
        try {
            const res = await fetch(window.CONTEXT_PATH + '/api/auth/session', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
                signal: controller.signal
            });
            clearTimeout(timeoutId);
            if (!res.ok) {
                let errorMsg = 'Unable to verify your identity with the server. Please try again.';
                try {
                    const errData = await res.json();
                    if (errData && errData.error) errorMsg = errData.error;
                } catch (jsonErr) { /* ignore malformed error bodies */ }
                result.error = errorMsg;
                return result;
            }
            data = await res.json();
        } finally {
            clearTimeout(timeoutId);
        }

        if (data && data.success) {
            result.success = true;
            result.redirect = data.redirect;
            result.role = data.role;
            result.name = data.name;
            result.photoUrl = data.photoUrl;
        } else {
            result.error = (data && data.error) || 'Session creation failed on server.';
        }
    } catch (err) {
        console.error('Server session sync error:', err);
        result.error = 'Unable to synchronize server session. Please try again.';
    }

    return result;
}

// Reset button states
function resetSocialButtons() {
    setSocialButtonsLoading('', false);
}

// Set loading state for social login buttons
function setSocialButtonsLoading(activeProvider, isLoading) {
    const buttons = {
        google: document.getElementById('btnGoogle'),
        facebook: document.getElementById('btnFacebook')
    };

    const submitBtn = document.getElementById('btnSubmit');
    if (submitBtn) {
        if (isLoading) {
            submitBtn.dataset.originalText = submitBtn.textContent;
            submitBtn.disabled = true;
        } else if (submitBtn.dataset.originalText) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = `<span class="btn-label">${submitBtn.dataset.originalText}</span>`;
        }
    }

    Object.keys(buttons).forEach(key => {
        const btn = buttons[key];
        if (!btn) return;
        btn.disabled = isLoading;
        if (isLoading && key === activeProvider) {
            btn.dataset.originalHtml = btn.dataset.originalHtml || btn.innerHTML;
            const label = key.charAt(0).toUpperCase() + key.slice(1);
            btn.innerHTML = `
                <div class="social-spinner"></div>
                <span class="btn-text">Connecting to ${label}...</span>
            `;
        } else if (!isLoading) {
            if (btn.dataset.originalHtml) {
                btn.innerHTML = btn.dataset.originalHtml;
            }
        }
    });
}

// Controlled loading state for the Sign In / Create Account buttons
function setLoginButtonLoading(isLoading, loadingText) {
    const submitBtn = document.getElementById('btnSubmit');
    if (!submitBtn) return;
    if (isLoading) {
        submitBtn.dataset.originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = `
            <span class="btn-spinner" aria-hidden="true"></span>
            <span class="btn-label">${loadingText}</span>
        `;
    } else {
        submitBtn.disabled = false;
        submitBtn.innerHTML = `<span class="btn-label">${submitBtn.dataset.originalText || 'Sign In'}</span>`;
    }
}

function resetLoginButton() {
    setLoginButtonLoading(false);
}

// Calculate hash code for unique institutional numeric ID
function computeNumericId(str) {
    let hash = 0;
    if (!str || str.length === 0) return Math.floor(Math.random() * 89999) + 10000;
    for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) - hash) + str.charCodeAt(i);
        hash |= 0;
    }
    return Math.abs(hash % 90000) + 10000;
}

// Global state for pending account linking
window.pendingSocialCredential = null;
window.pendingSocialEmail = '';
window.pendingSocialProvider = '';

// Handle Social Sign-In for Google and Facebook
async function handleSocialLogin(providerType) {
    if (typeof firebase === 'undefined' || !firebase.auth) {
        showToast('Firebase Authentication is not available. Please check internet connection.', 'error');
        return;
    }

    setSocialButtonsLoading(providerType, true);

    try {
        let provider;
        let providerName;

        if (providerType === 'google') {
            provider = new firebase.auth.GoogleAuthProvider();
            provider.addScope('profile');
            provider.addScope('email');
            provider.setCustomParameters({ prompt: 'select_account' });
            providerName = 'Google';
        } else if (providerType === 'facebook') {
            provider = new firebase.auth.FacebookAuthProvider();
            provider.addScope('email');
            provider.addScope('public_profile');
            providerName = 'Facebook';
        } else {
            throw new Error('Unsupported authentication provider: ' + providerType);
        }

        // Authenticate with Firebase via popup
        const result = await auth.signInWithPopup(provider);
        const user = result.user;
        if (!user) {
            throw new Error('No user credentials returned from provider.');
        }

        await processAuthenticatedUser(user, providerName.toLowerCase());

    } catch (error) {
        handleSocialAuthError(error, providerType);
    } finally {
        // Always release the loading state, even when sign-in fails or the session sync is redirected.
        resetSocialButtons();
    }
}

// Process authenticated user after social sign-in or account linking
async function processAuthenticatedUser(user, authProviderName) {
    const providerLabel = authProviderName ? (authProviderName.charAt(0).toUpperCase() + authProviderName.slice(1)) : 'Provider';
    showToast(`Signed in with ${providerLabel}! Synchronizing session...`, 'success');

    // Immediately synchronize authenticated session with Spring Boot backend
    // The backend securely verifies the Firebase ID token and retrieves authoritative role & profile from Realtime Database
    const sessionResult = await syncSessionWithServer(user, 'student', 'CSE', user.displayName || '', user.photoURL || '', authProviderName);

    if (sessionResult && sessionResult.success && sessionResult.redirect) {
        window.location.href = sessionResult.redirect;
        return;
    }

    handleSocialAuthError(new Error((sessionResult && sessionResult.error) || 'Session synchronization failed. Please try again.'), 'social');
}

// Handle and format social authentication errors
function handleSocialAuthError(error, providerType) {
    console.error(`Social auth error [${providerType}]:`, error);
    let msg = 'Authentication failed. Please try again.';

    if (!error) {
        showToast(msg, 'error');
        return;
    }

    const providerLabel = providerType ? (providerType.charAt(0).toUpperCase() + providerType.slice(1)) : 'Social';

    switch (error.code) {
        case 'auth/popup-closed-by-user':
            msg = `Sign-in cancelled. The ${providerLabel} sign-in window was closed.`;
            break;
        case 'auth/cancelled-popup-request':
            msg = 'Previous sign-in request was cancelled. Please try again.';
            break;
        case 'auth/popup-blocked':
            msg = 'Sign-in popup was blocked by your browser. Please allow popups for this site and try again.';
            break;
        case 'auth/account-exists-with-different-credential':
            // Secure account linking workflow
            window.pendingSocialCredential = error.credential;
            window.pendingSocialEmail = error.email || '';
            window.pendingSocialProvider = providerLabel;
            promptAccountLinking(error, providerLabel);
            return;
        case 'auth/operation-not-allowed':
            msg = `${providerLabel} sign-in is not enabled in Firebase Authentication console. Please enable it under Firebase Console > Authentication > Sign-in method.`;
            break;
        case 'auth/unauthorized-domain':
            msg = `Current domain (${window.location.hostname}) is not authorized in Firebase Console. Add it in Firebase Console > Authentication > Settings > Authorized Domains.`;
            break;
        case 'auth/network-request-failed':
            msg = 'Network connection failed. Please check your internet connectivity and try again.';
            break;
        case 'auth/user-disabled':
            msg = 'This user account has been disabled by an administrator.';
            break;
        default:
            if (error.message) {
                msg = error.message;
            }
            break;
    }

    showToast(msg, 'error');
}

// Guide the user through a secure account-linking flow
async function promptAccountLinking(error, newProviderLabel) {
    const email = error.email || '';
    const modal = document.getElementById('accountLinkingModal');
    const msgEl = document.getElementById('accountLinkingMsg');
    const passGroup = document.getElementById('linkingPasswordGroup');

    if (!modal) {
        showToast(`An account with ${email} already exists. Please sign in with your original method to link accounts.`, 'error');
        return;
    }

    try {
        const methods = await auth.fetchSignInMethodsForEmail(email);
        const hasPassword = methods.includes('password');

        if (hasPassword) {
            msgEl.innerHTML = `An account already exists for <strong>${email}</strong> using Email and Password. Enter your password below to securely link your <strong>${newProviderLabel}</strong> account.`;
            if (passGroup) passGroup.style.display = 'block';
        } else {
            const originalMethod = methods[0] || 'your existing provider';
            msgEl.innerHTML = `An account already exists for <strong>${email}</strong> using <strong>${originalMethod}</strong>. Please sign in using your original method, and your accounts will be linked.`;
            if (passGroup) passGroup.style.display = 'none';
        }

        modal.style.display = 'flex';
    } catch (fetchErr) {
        console.warn('Fetch sign-in methods warning:', fetchErr);
        msgEl.innerHTML = `An account already exists for <strong>${email}</strong>. Please authenticate with your existing password to link your ${newProviderLabel} account.`;
        if (passGroup) passGroup.style.display = 'block';
        modal.style.display = 'flex';
    }
}

function closeAccountLinkingModal() {
    const modal = document.getElementById('accountLinkingModal');
    if (modal) modal.style.display = 'none';
    window.pendingSocialCredential = null;
    window.pendingSocialEmail = '';
    window.pendingSocialProvider = '';
    resetSocialButtons();
}

async function handleAccountLinkingSubmit(e) {
    e.preventDefault();
    const passwordInput = document.getElementById('linkPassword');
    const password = passwordInput ? passwordInput.value : '';
    const email = window.pendingSocialEmail;
    const pendingCred = window.pendingSocialCredential;
    const submitBtn = document.getElementById('btnConfirmLinking');

    if (!password) {
        showToast('Please enter your account password to verify account ownership.', 'error');
        return;
    }

    if (!email || !pendingCred) {
        showToast('Account linking session expired. Please try signing in again.', 'error');
        closeAccountLinkingModal();
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Linking accounts...';

    try {
        // Authenticate with the existing email and password
        const userCred = await auth.signInWithEmailAndPassword(email, password);
        const user = userCred.user;

        // Securely link the new social credential to the authenticated user
        await user.linkWithCredential(pendingCred);
        showToast(`Successfully linked ${window.pendingSocialProvider || 'social'} account!`, 'success');

        closeAccountLinkingModal();
        await processAuthenticatedUser(user, (window.pendingSocialProvider || 'social').toLowerCase());
    } catch (linkErr) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Verify & Link';
        let errMsg = 'Failed to link accounts. Please verify your password.';
        if (linkErr.code === 'auth/wrong-password' || linkErr.code === 'auth/invalid-credential') {
            errMsg = 'Incorrect password for existing account.';
        } else if (linkErr.code === 'auth/credential-already-in-use') {
            errMsg = 'This social account is already linked to a different profile.';
        }
        showToast(errMsg, 'error');
    }
}

// Handle Student & User Login (Option 1: Email and Password)
async function handleLogin(e) {
    if (e && e.preventDefault) {
        e.preventDefault();
    }

    const emailInput = document.getElementById('email');
    const passInput = document.getElementById('password');
    const email = emailInput ? emailInput.value.trim() : '';
    const password = passInput ? passInput.value : '';
    const errorBox = document.getElementById('loginError');

    const showError = (msg) => {
        if (errorBox) {
            errorBox.textContent = msg;
            errorBox.style.display = 'flex';
        }
    };

    // Client-side validations
    if (!email || !password) {
        showError('Please enter both email address and password.');
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showError('Please enter a valid email address format (e.g., student@college.edu).');
        return;
    }

    if (password.length < 6) {
        showError('Password must be at least 6 characters long.');
        return;
    }

    if (typeof auth === 'undefined' || !auth) {
        setLoginButtonLoading(false);
        showError('Authentication service is temporarily unavailable. Please try again in a moment.');
        showToast('Authentication service is temporarily unavailable. Please try again.', 'error');
        return;
    }

    setLoginButtonLoading(true, 'Verifying credentials...');

    let redirectUrl = null;
    // Safety watchdog: never leave the button stuck in a loading state
    const watchdogId = setTimeout(() => {
        resetLoginButton();
        const timeoutMsg = 'Login request timed out. Please check your connection and try again.';
        showError(timeoutMsg);
        showToast(timeoutMsg, 'error');
    }, 60000);

    try {
        const cred = await auth.signInWithEmailAndPassword(email, password);
        const user = cred ? cred.user : null;

        if (!user) {
            throw new Error('Authentication failed. No user returned from provider.');
        }

        showToast('Authentication verified! Synchronizing session...', 'success');
        // Synchronize directly with Spring Boot backend (Fast, <500ms)
        const sessionResult = await syncSessionWithServer(user, null, null, user.displayName || '', user.photoURL || '', 'password');

        if (sessionResult && sessionResult.success && sessionResult.redirect) {
            redirectUrl = sessionResult.redirect;
        } else if (sessionResult && sessionResult.error) {
            showError(sessionResult.error);
        }
    } catch (error) {
        handleLoginError(error);
    } finally {
        clearTimeout(watchdogId);
        // ALWAYS restore the button state unless a successful redirect is being performed.
        if (!redirectUrl) {
            resetLoginButton();
        }
    }

    if (redirectUrl) {
        window.location.href = redirectUrl;
    }
}

// Map Firebase authentication errors to user-friendly messages
function handleLoginError(error) {
    let msg = 'Incorrect email or password. Please verify your credentials and try again.';

    if (error) {
        switch (error.code) {
            case 'auth/invalid-credential':
            case 'auth/wrong-password':
            case 'auth/user-not-found':
            case 'auth/invalid-email':
                msg = 'Invalid email or password. Please verify your credentials and try again.';
                break;
            case 'auth/too-many-requests':
                msg = 'Access to this account has been temporarily disabled due to multiple failed login attempts. Please reset your password or try again later.';
                break;
            case 'auth/user-disabled':
                msg = 'This user account has been disabled by an administrator. Please contact your campus placement officer.';
                break;
            case 'auth/network-request-failed':
                msg = 'Network connection failed. Please verify your internet connection and try again.';
                break;
            case 'auth/expired-id-token':
                msg = 'Your session has expired. Please sign in again to continue.';
                break;
            case 'auth/operation-not-allowed':
                msg = 'Email and password sign-in is not enabled. Please contact support.';
                break;
            case 'auth/unauthorized-domain':
                msg = 'This domain is not authorized for authentication. Please contact the administrator.';
                break;
            default:
                if (error.message) {
                    msg = error.message;
                }
                break;
        }
    }

    const errorBox = document.getElementById('loginError');
    if (errorBox) {
        errorBox.textContent = msg;
        errorBox.style.display = 'flex';
    }
    showToast(msg, 'error');
}

// Handle Student Registration
async function handleRegister(e) {
    if (e && e.preventDefault) e.preventDefault();

    const nameInput = document.getElementById('name');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const branchInput = document.getElementById('branch');
    const yearInput = document.getElementById('year');
    const collegeInput = document.getElementById('college');
    const submitBtn = document.getElementById('btnSubmit');
    const errorBox = document.getElementById('registerError');

    const showError = (msg, isHtml = false) => {
        if (errorBox) {
            if (isHtml) {
                errorBox.innerHTML = msg;
            } else {
                errorBox.textContent = msg;
            }
            errorBox.style.display = 'block';
        }
        if (!isHtml) {
            showToast(msg, 'error');
        }
    };

    const clearError = () => {
        if (errorBox) {
            errorBox.innerHTML = '';
            errorBox.style.display = 'none';
        }
    };

    clearError();

    const name = nameInput ? nameInput.value.trim() : '';
    const email = emailInput ? emailInput.value.trim() : '';
    const password = passwordInput ? passwordInput.value : '';
    const confirmPassword = confirmPasswordInput ? confirmPasswordInput.value : '';
    const branch = branchInput ? branchInput.value : 'CSE';
    const year = yearInput ? yearInput.value : 'Final Year';
    const college = collegeInput ? collegeInput.value.trim() : "SNJB's Late Sau KBJ College of Engineering";

    // 1. Strict Form Validation
    if (!name) {
        showError('Please enter your full name.');
        if (nameInput) nameInput.focus();
        return;
    }
    if (name.length < 2) {
        showError('Full name must be at least 2 characters long.');
        if (nameInput) nameInput.focus();
        return;
    }

    if (!email) {
        showError('Please enter your college email address.');
        if (emailInput) emailInput.focus();
        return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showError('Please enter a valid email format (e.g., student@college.edu).');
        if (emailInput) emailInput.focus();
        return;
    }

    if (!password) {
        showError('Please enter a secure password.');
        if (passwordInput) passwordInput.focus();
        return;
    }
    if (password.length < 6) {
        showError('Password must be at least 6 characters long.');
        if (passwordInput) passwordInput.focus();
        return;
    }

    if (confirmPasswordInput && password !== confirmPassword) {
        showError('Passwords do not match. Please re-enter to confirm.');
        if (confirmPasswordInput) confirmPasswordInput.focus();
        return;
    }

    if (typeof auth === 'undefined' || !auth) {
        showError('Authentication service is temporarily unavailable. Please refresh the page.');
        return;
    }

    // 2. Prevent Duplicate Submissions & Display Loading Spinner
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `
            <span class="btn-spinner" aria-hidden="true" style="display:inline-block; vertical-align:middle; width:16px; height:16px; border:2px solid rgba(255,255,255,0.3); border-top-color:#fff; border-radius:50%; animation:spin 0.8s linear infinite; margin-right:8px;"></span>
            <span class="btn-label">Creating account...</span>
        `;
    }

    // Safety watchdog timer: never leave the submit button permanently stuck
    const watchdogId = setTimeout(() => {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<span class="btn-label">Create Student Account</span>';
        }
        showError('Registration request timed out. If your account was created, please try signing in.');
    }, 25000);

    try {
        // 3. Create Account in Firebase Authentication
        const cred = await auth.createUserWithEmailAndPassword(email, password);
        const user = cred.user;
        const uid = user.uid;
        const numericId = Math.floor(Math.random() * 89999) + 10000;

        // Try updating display name on Firebase Auth user object
        try {
            if (user.updateProfile) {
                await user.updateProfile({ displayName: name });
            }
        } catch (ignored) {}

        // 4. Construct Authoritative Student Profile
        const userProfile = {
            uid: uid,
            numericId: numericId,
            name: name,
            displayName: name,
            email: email,
            college: college || "SNJB's Late Sau KBJ College of Engineering",
            branch: branch || 'CSE',
            year: year || 'Final Year',
            skills: 'Java, SQL',
            interests: 'Software Development',
            role: 'student',
            authProvider: 'password',
            createdAt: (typeof firebase !== 'undefined' && firebase.database && firebase.database.ServerValue)
                ? firebase.database.ServerValue.TIMESTAMP
                : Date.now()
        };

        // 5. Write to Realtime Database with timeout protection
        if (typeof database !== 'undefined' && database) {
            try {
                await withTimeout(database.ref('users/' + uid).set(userProfile), 5000, 'Database write timeout');
            } catch (dbErr) {
                console.warn('Realtime Database write note (will synchronize via server):', dbErr);
            }

            // Seed initial student_metrics node
            const initialMetrics = {
                javaSkill: 0,
                dsaSkill: 0,
                sqlSkill: 0,
                aptitudeSkill: 0,
                technicalScore: 0,
                interviewScore: 0,
                skillCompletionScore: 20,
                resumeScore: 0,
                testsCompleted: 0,
                streakDays: 1,
                desiredRole: 'Software Engineer',
                targetSalary: '₹12.0 LPA',
                preferredLocation: 'Bangalore / Remote',
                updatedAt: (typeof firebase.database !== 'undefined' && firebase.database.ServerValue)
                    ? firebase.database.ServerValue.TIMESTAMP
                    : Date.now()
            };
            try {
                await withTimeout(database.ref('student_metrics/' + uid).set(initialMetrics), 4000, 'Metrics seed timeout');
            } catch (mErr) {
                console.warn('Metrics seed note:', mErr);
            }
        }

        // 6. Write to Cloud Firestore with fail-safe timeout (never blocks registration if unprovisioned)
        if (typeof firestore !== 'undefined' && firestore) {
            try {
                const fsProfile = {
                    uid: uid,
                    numericId: numericId,
                    name: name,
                    displayName: name,
                    email: email,
                    role: 'student',
                    branch: branch || 'CSE',
                    year: year || 'Final Year',
                    college: college || "SNJB's Late Sau KBJ College of Engineering",
                    authProvider: 'password',
                    createdAt: (typeof firebase.firestore !== 'undefined' && firebase.firestore.FieldValue)
                        ? firebase.firestore.FieldValue.serverTimestamp()
                        : new Date().toISOString()
                };
                await withTimeout(firestore.collection('users').doc(uid).set(fsProfile, { merge: true }), 3000, 'Firestore timeout');
            } catch (fsErr) {
                console.warn('Firestore user profile seed note (handled via server fallback):', fsErr);
            }
        }

        // 7. Synchronize Session with Trusted Spring Boot Backend
        const sessionResult = await syncSessionWithServer(user, 'student', branch, name, '', 'password', college, year);

        clearTimeout(watchdogId);

        if (sessionResult && sessionResult.success && sessionResult.redirect) {
            showToast('Registration successful! Launching student dashboard...', 'success');
            window.location.href = sessionResult.redirect;
            return;
        }

        throw new Error((sessionResult && sessionResult.error) || 'Registration succeeded, but server session could not be established.');

    } catch (error) {
        clearTimeout(watchdogId);
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<span class="btn-label">Create Student Account</span>';
        }

        let msg = 'Registration failed. Please check your details and try again.';
        let isHtml = false;

        if (error.code === 'auth/email-already-in-use') {
            msg = `An account with <strong>${escapeHtml(email)}</strong> already exists. ` +
                  `<a href="${window.CONTEXT_PATH || ''}/login.jsp?email=${encodeURIComponent(email)}" style="font-weight: 600; text-decoration: underline; margin-left: 6px; color: #2563eb;">Sign In Instead &rarr;</a>`;
            isHtml = true;
            showToast('An account with this email address already exists.', 'error');
        } else if (error.code === 'auth/weak-password') {
            msg = 'Password is too weak. Please use at least 6 characters with a combination of letters and numbers.';
            showToast(msg, 'error');
        } else if (error.code === 'auth/invalid-email') {
            msg = 'The email address format is invalid. Please check for errors.';
            showToast(msg, 'error');
        } else if (error.code === 'auth/network-request-failed') {
            msg = 'Network connection failed. Please check your internet connection and try again.';
            showToast(msg, 'error');
        } else if (error.code === 'auth/operation-not-allowed') {
            msg = 'Email/password registration is not enabled. Please contact your campus placement officer.';
            showToast(msg, 'error');
        } else if (error.code === 'auth/too-many-requests') {
            msg = 'Too many registration requests. Please wait a few moments before trying again.';
            showToast(msg, 'error');
        } else if (error.message) {
            msg = error.message;
            showToast(msg, 'error');
        }

        showError(msg, isHtml);
    }
}

// Handle Forgot Password
async function handleForgotPassword(e) {
    e.preventDefault();
    const email = document.getElementById('email').value.trim();
    const submitBtn = document.getElementById('btnSubmit');

    if (!email) {
        showToast('Please enter your registered email address.', 'error');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Sending reset link...';

    try {
        await auth.sendPasswordResetEmail(email);
        showToast('Password reset link sent! Check your email inbox.', 'success');
        submitBtn.textContent = 'Reset Link Sent';
        setTimeout(() => {
            window.location.href = window.CONTEXT_PATH + '/login.jsp';
        }, 3000);
    } catch (error) {
        // Prevent email enumeration by still displaying success/generic message
        showToast('If an account exists for this email, a password reset link has been sent.', 'success');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Send Reset Link';
    }
}

// Handle Logout with confirmation, webcam teardown, and session invalidation
async function handleLogout() {
    if (!window.confirm('Are you sure you want to log out of TPO Buddy? Any active test session will be concluded.')) {
        return;
    }

    try {
        // Stop any active proctoring webcam stream safely
        if (typeof window.stopProctorWebcam === 'function') {
            window.stopProctorWebcam();
        }
    } catch (e) {
        console.warn('Webcam teardown notice:', e);
    }

    try {
        if (typeof auth !== 'undefined' && auth.signOut) {
            await auth.signOut();
        }
    } catch (ignored) {}

    // Invalidate server session & redirect to login page with clear toast signal
    window.location.href = (window.CONTEXT_PATH || '') + '/logout';
}

// Toggle mobile navigation drawer
function toggleMobileSidebar() {
    const sidebar = document.querySelector('.app-sidebar');
    if (sidebar) {
        sidebar.classList.toggle('open');
    }
}

// Handle notification query params on page load
document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('logout') === 'true') {
        showToast('You have been successfully logged out.', 'success');
        // Clean URL to prevent re-triggering toast on refresh
        window.history.replaceState({}, document.title, window.location.pathname);
    } else if (params.get('error') === 'unauthorized') {
        showToast('Please sign in to access protected placement features.', 'error');
        window.history.replaceState({}, document.title, window.location.pathname);
    } else if (params.get('error') === 'forbidden') {
        showToast('Access denied: your account role does not permit this action.', 'error');
        window.history.replaceState({}, document.title, window.location.pathname);
    } else if (params.get('error') === 'session_expired') {
        showToast('Your session has expired. Please sign in again.', 'error');
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});

