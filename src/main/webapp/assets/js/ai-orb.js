/**
 * TPO Buddy — 3D Visuals & Animation Interactivity
 * Powers 3D tilt, mouse parallax, and animated metric counters.
 */

document.addEventListener('DOMContentLoaded', () => {
    init3DTilt();
    initAnimatedCounters();
});

// Interactive 3D tilt on cards and AI Orb
function init3DTilt() {
    const tiltCards = document.querySelectorAll('.card-3d');
    tiltCards.forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            const centerX = rect.width / 2;
            const centerY = rect.height / 2;
            const rotateX = ((y - centerY) / centerY) * -10;
            const rotateY = ((x - centerX) / centerX) * 10;

            card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-6px)`;
        });

        card.addEventListener('mouseleave', () => {
            card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0)';
        });
    });

    // Parallax tracking on the 3D Orb Scene
    const orbContainer = document.querySelector('.orb-container');
    const orbScene = document.querySelector('.orb-scene');
    if (orbScene && orbContainer) {
        orbScene.addEventListener('mousemove', (e) => {
            const rect = orbScene.getBoundingClientRect();
            const x = e.clientX - rect.left - rect.width / 2;
            const y = e.clientY - rect.top - rect.height / 2;
            orbContainer.style.transform = `rotateY(${x * 0.15}deg) rotateX(${-y * 0.15}deg)`;
        });

        orbScene.addEventListener('mouseleave', () => {
            orbContainer.style.transform = 'rotateY(0deg) rotateX(10deg)';
        });
    }
}

// Smooth animated counters settling on actual Firebase values
function initAnimatedCounters() {
    const counters = document.querySelectorAll('.animated-counter');
    counters.forEach(counter => {
        const target = parseInt(counter.getAttribute('data-target') || counter.textContent, 10);
        if (isNaN(target)) return;

        let current = 0;
        const duration = 1200; // ms
        const stepTime = 20;
        const totalSteps = duration / stepTime;
        const increment = target / totalSteps;

        const timer = setInterval(() => {
            current += increment;
            if (current >= target) {
                counter.textContent = target;
                clearInterval(timer);
            } else {
                counter.textContent = Math.round(current);
            }
        }, stepTime);
    });
}
