/**
 * TPO Buddy — Firebase Client Configuration
 * Strictly synchronized with the TPO Buddy / CareerPulse Android application.
 * Dynamic credentials injected at runtime via backend JSP / environment or window.FIREBASE_CONFIG.
 */

const firebaseConfig = (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.apiKey) ? window.FIREBASE_CONFIG : {
    apiKey: (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.apiKey) || "",
    authDomain: (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.authDomain) || "tpobuddy.firebaseapp.com",
    databaseURL: (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.databaseURL) || "https://tpobuddy-default-rtdb.firebaseio.com",
    projectId: (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.projectId) || "tpobuddy",
    storageBucket: (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.storageBucket) || "tpobuddy.firebasestorage.app",
    messagingSenderId: (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.messagingSenderId) || "811364970356",
    appId: (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.appId) || "1:811364970356:web:09bc29db4bff3a07cb4621",
    measurementId: (window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.measurementId) || "G-K3KP8NTCSF"
};

// Initialize Firebase if not already initialized
if (typeof firebase !== 'undefined' && !firebase.apps.length) {
    if (firebaseConfig.apiKey) {
        try {
            firebase.initializeApp(firebaseConfig);
            console.log("🔥 Firebase connected to project: " + firebaseConfig.projectId);
        } catch (e) {
            console.warn("Firebase initialization note:", e);
        }
    } else {
        console.warn("⚠️ Firebase API Key is not configured. Please define firebase.apiKey in application.properties or environment variables.");
    }
}

const auth = (typeof firebase !== 'undefined' && firebase.apps.length) ? firebase.auth() : null;
const database = (typeof firebase !== 'undefined' && firebase.apps.length) ? firebase.database() : null;
const firestore = (typeof firebase !== 'undefined' && firebase.apps.length) ? firebase.firestore() : null;
let analytics = null;
if (typeof firebase !== 'undefined' && firebase.apps.length && typeof firebase.analytics === 'function') {
    try {
        analytics = firebase.analytics();
    } catch (e) {
        console.warn("Firebase analytics note:", e);
    }
}
