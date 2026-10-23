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

// Sync verified Firebase ID Token with Java Servlet session
async function syncSessionWithServer(user, role, branch, name, photoUrl, authProvider) {
    try {
        const idToken = await user.getIdToken();
        const payload = {
            idToken: idToken,
            uid: user.uid,
            email: user.email || '',
            name: name || user.displayName || (user.email ? user.email.split('@')[0] : 'Student'),
            role: role || 'student',
            branch: branch || 'CSE',
            photoUrl: photoUrl || user.photoURL || '',
            authProvider: authProvider || 'password'
        };

        const res = await fetch(window.CONTEXT_PATH + '/api/auth/session', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (data.success) {
            window.location.href = data.redirect;
        } else {
            showToast(data.error || 'Session creation failed on server.', 'error');
            resetSocialButtons();
        }
    } catch (err) {
        console.error('Server session sync error:', err);
        showToast('Unable to synchronize server session.', 'error');
        resetSocialButtons();
    }
}

// Reset button states
function resetSocialButtons() {
    setSocialButtonsLoading('', false);
    const submitBtn = document.getElementById('btnSubmit');
    if (submitBtn) {
        submitBtn.disabled = false;
        if (submitBtn.dataset.originalText) {
            submitBtn.textContent = submitBtn.dataset.originalText;
        }
    }
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
            submitBtn.textContent = submitBtn.dataset.originalText;
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
        resetSocialButtons();
        handleSocialAuthError(error, providerType);
    }
}

// Process authenticated user after social sign-in or account linking
async function processAuthenticatedUser(user, authProviderName) {
    const providerLabel = authProviderName ? (authProviderName.charAt(0).toUpperCase() + authProviderName.slice(1)) : 'Provider';
    showToast(`Signed in with ${providerLabel}! Synchronizing session...`, 'success');

    // Immediately synchronize authenticated session with Spring Boot backend
    // The backend securely verifies the Firebase ID token and retrieves authoritative role & profile from Realtime Database
    await syncSessionWithServer(user, 'student', 'CSE', user.displayName || '', user.photoURL || '', authProviderName);
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
    e.preventDefault();
    const emailInput = document.getElementById('email');
    const passInput = document.getElementById('password');
    const email = emailInput ? emailInput.value.trim() : '';
    const password = passInput ? passInput.value : '';
    const submitBtn = document.getElementById('btnSubmit');

    // Client-side validations
    if (!email || !password) {
        showToast('Please enter both email address and password.', 'error');
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showToast('Please enter a valid email address format (e.g., student@college.edu).', 'error');
        return;
    }

    if (password.length < 6) {
        showToast('Password must be at least 6 characters long.', 'error');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.dataset.originalText = submitBtn.textContent;
    submitBtn.textContent = 'Verifying credentials...';

    try {
        const cred = await auth.signInWithEmailAndPassword(email, password);
        const user = cred.user;

        showToast('Authentication verified! Synchronizing session...', 'success');
        // Synchronize directly with Spring Boot backend (Fast, <500ms)
        await syncSessionWithServer(user, null, null, user.displayName || '', user.photoURL || '', 'password');
    } catch (error) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Sign In';
        let msg = 'Incorrect email or password.';
        if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password' || error.code === 'auth/invalid-credential') {
            msg = 'Invalid email or password. Please verify your credentials and try again.';
        } else if (error.code === 'auth/too-many-requests') {
            msg = 'Access to this account has been temporarily disabled due to multiple failed login attempts. Please reset your password or try again later.';
        } else if (error.code === 'auth/invalid-email') {
            msg = 'The email address format is invalid. Please check and retry.';
        } else if (error.code === 'auth/user-disabled') {
            msg = 'This user account has been disabled by an administrator. Please contact your campus placement officer.';
        } else if (error.code === 'auth/network-request-failed') {
            msg = 'Network connection failed. Please verify your internet connection.';
        }
        showToast(msg, 'error');
    }
}

// Handle Student Registration
async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const branch = document.getElementById('branch').value;
    const year = document.getElementById('year').value;
    const college = document.getElementById('college').value.trim();
    const submitBtn = document.getElementById('btnSubmit');

    if (!name || !email || !password) {
        showToast('Please fill in all required fields.', 'error');
        return;
    }

    if (password.length < 6) {
        showToast('Password must be at least 6 characters long.', 'error');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating account...';

    try {
        const cred = await auth.createUserWithEmailAndPassword(email, password);
        const user = cred.user;
        const uid = user.uid;
        const numericId = Math.abs(uid.hashCode ? uid.hashCode() : (Math.floor(Math.random() * 89999) + 10000));

        // Create Realtime Database user node matching Android app
        const userProfile = {
            uid: uid,
            numericId: numericId,
            name: name,
            email: email,
            college: college || 'National Institute of Technology',
            branch: branch || 'CSE',
            year: year || 'Final Year',
            skills: 'Java, SQL',
            interests: 'Software Development',
            role: 'student',
            createdAt: firebase.database.ServerValue.TIMESTAMP
        };

        await database.ref('users/' + uid).set(userProfile);

        // Also write to Cloud Firestore `users/{uid}`
        try {
            if (typeof firestore !== 'undefined' && firestore) {
                const fsProfile = Object.assign({}, userProfile, {
                    displayName: name,
                    authProvider: 'password',
                    createdAt: (typeof firebase.firestore !== 'undefined' && firebase.firestore.FieldValue)
                        ? firebase.firestore.FieldValue.serverTimestamp()
                        : new Date().toISOString()
                });
                await firestore.collection('users').doc(uid).set(fsProfile, { merge: true });
            }
        } catch (fsErr) {
            console.warn('Firestore user seed warning:', fsErr);
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
            updatedAt: firebase.database.ServerValue.TIMESTAMP
        };
        await database.ref('student_metrics/' + uid).set(initialMetrics);

        showToast('Registration successful! Launching dashboard...', 'success');
        await syncSessionWithServer(user, 'student', branch, name, '', 'password');
    } catch (error) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create Student Account';
        let msg = 'Registration failed. Please try again.';
        if (error.code === 'auth/email-already-in-use') {
            msg = 'An account with this email address already exists.';
        } else if (error.code === 'auth/weak-password') {
            msg = 'Password is too weak. Must be at least 6 characters.';
        }
        showToast(msg, 'error');
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

