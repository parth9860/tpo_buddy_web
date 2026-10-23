/**
 * TPO Buddy — Placement Jobs & Drives Client
 */

async function applyJob(jobId, btn) {
    if (!jobId) return;

    btn.disabled = true;
    btn.textContent = 'Submitting...';

    try {
        const res = await fetch(`${window.CONTEXT_PATH}/api/jobs/apply`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ jobId: jobId })
        });

        const data = await res.json();
        if (data.success) {
            btn.className = 'btn btn-secondary btn-sm';
            btn.textContent = '✓ Applied';
            showToast('Application successfully registered in Firebase!', 'success');
        } else {
            btn.disabled = false;
            btn.textContent = 'Apply Now';
            showToast('Failed to apply: ' + data.error, 'error');
        }
    } catch (err) {
        btn.disabled = false;
        btn.textContent = 'Apply Now';
        showToast('Application submission failed. Please try again.', 'error');
    }
}

function filterJobs(keyword) {
    const q = keyword.toLowerCase().trim();
    const cards = document.querySelectorAll('.job-card');
    cards.forEach(card => {
        const text = card.textContent.toLowerCase();
        card.style.display = text.includes(q) ? 'flex' : 'none';
    });
}
