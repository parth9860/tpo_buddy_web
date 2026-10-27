/**
 * TPO Buddy - Test Result & Answer Review Engine
 * Handles post-examination performance breakdown, question reviews, filtering, and solutions display.
 */

let rawReviewsData = [];
let currentFilter = 'ALL';

document.addEventListener('DOMContentLoaded', () => {
    initReviewsData();
    renderReviewQuestions(rawReviewsData);
});

function initReviewsData() {
    try {
        const jsonEl = document.getElementById('reviewsDataJson');
        if (jsonEl) {
            const rawText = jsonEl.textContent ? jsonEl.textContent.trim() : '[]';
            rawReviewsData = JSON.parse(rawText || '[]');
        }
    } catch (e) {
        console.warn('Failed parsing reviews JSON:', e);
        rawReviewsData = [];
    }

    const allCountEl = document.getElementById('countFilterAll');
    if (allCountEl) {
        allCountEl.textContent = rawReviewsData.length;
    }
}

function scrollToReviewSection() {
    const sec = document.getElementById('answersReviewSection');
    if (sec) {
        sec.scrollIntoView({ behavior: 'smooth' });
    }
}

function filterReviewQuestions(filter, btn) {
    currentFilter = filter;
    document.querySelectorAll('#reviewFilterButtons .filter-chip-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');

    const filtered = rawReviewsData.filter(q => {
        if (filter === 'ALL') return true;
        if (filter === 'CORRECT') return q.isCorrect === true;
        if (filter === 'INCORRECT') return q.isCorrect === false && (q.selectedOption >= 0 || q.userAnswer >= 0);
        if (filter === 'UNANSWERED') {
            return (q.selectedOption === undefined || q.selectedOption < 0 || q.selectedOption === null) &&
                   (q.userAnswer === undefined || q.userAnswer < 0 || q.userAnswer === null);
        }
        return true;
    });

    renderReviewQuestions(filtered);
}

function renderReviewQuestions(questionsList) {
    const container = document.getElementById('reviewQuestionsContainer');
    if (!container) return;

    const allCountEl = document.getElementById('countFilterAll');
    if (allCountEl) allCountEl.textContent = rawReviewsData.length;

    if (!questionsList || questionsList.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 3rem; color: var(--text-muted); background: #f8fafc; border-radius: var(--radius-sm); border: 1px dashed var(--border-subtle);">
                No questions match the selected filter (${escapeHtml(currentFilter)}).
            </div>
        `;
        return;
    }

    container.innerHTML = '';
    const optLetters = ['A', 'B', 'C', 'D'];

    questionsList.forEach((q, idx) => {
        const card = document.createElement('div');
        const isCorrect = q.isCorrect === true;
        const userAns = (q.selectedOption !== undefined && q.selectedOption !== null) ? q.selectedOption :
                       ((q.userAnswer !== undefined && q.userAnswer !== null) ? q.userAnswer : -1);
        const isAnswered = userAns >= 0;
        const statusCls = isCorrect ? 'correct' : (isAnswered ? 'incorrect' : 'unanswered');

        card.className = `review-question-card ${statusCls}`;

        const corrOpt = (q.correctOption !== undefined && q.correctOption !== null) ? q.correctOption :
                        ((q.correctAnswer !== undefined && q.correctAnswer !== null) ? q.correctAnswer : 0);

        let passageHtml = '';
        if (q.passageOrCode && q.passageOrCode.trim().length > 0) {
            passageHtml = `
                <div style="background: #0f172a; color: #f8fafc; border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1rem; overflow-x: auto; font-family: 'Consolas', monospace; font-size: 0.85rem; line-height: 1.5;">
                    <span style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; font-weight: 700; display: block; margin-bottom: 0.35rem;">Reference Code / Context:</span>
                    <pre style="margin: 0;"><code>${escapeHtml(q.passageOrCode)}</code></pre>
                </div>
            `;
        }

        // Options list
        const options = [q.optionA, q.optionB, q.optionC, q.optionD];
        let optionsHtml = '';
        options.forEach((optText, optIdx) => {
            if (!optText && optText !== '') return;
            let optCls = 'option-review-item';
            let optIcon = '';

            const isUserChoice = (userAns === optIdx);
            const isCorrectChoice = (corrOpt === optIdx);

            if (isCorrectChoice && isUserChoice) {
                optCls += ' is-user-selected-right';
                optIcon = '<span style="color: #16a34a; font-weight: 700;">✓ Your Answer (Correct)</span>';
            } else if (isCorrectChoice) {
                optCls += ' is-correct-target';
                optIcon = '<span style="color: #16a34a; font-weight: 700;">✓ Correct Key</span>';
            } else if (isUserChoice) {
                optCls += ' is-user-selected-wrong';
                optIcon = '<span style="color: #dc2626; font-weight: 700;">✗ Your Answer (Incorrect)</span>';
            }

            optionsHtml += `
                <div class="${optCls}">
                    <div style="display: flex; align-items: center; gap: 0.75rem;">
                        <span style="font-weight: 700; width: 22px; height: 22px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.06); font-size: 0.75rem;">${optLetters[optIdx]}</span>
                        <span>${escapeHtml(optText)}</span>
                    </div>
                    <div>${optIcon}</div>
                </div>
            `;
        });

        const sectionBadge = q.section ? `<span class="badge badge-primary" style="font-size: 0.7rem;">${escapeHtml(q.section)}</span>` : '';
        const topicBadge = q.topic ? `<span class="badge badge-outline" style="font-size: 0.7rem;">${escapeHtml(q.topic)}</span>` : '';
        const diffBadge = q.difficulty ? `<span class="badge badge-outline" style="font-size: 0.7rem;">${escapeHtml(q.difficulty)}</span>` : '';
        const statusBadgeText = isCorrect ? '✓ Correct (+2 Marks)' : (isAnswered ? '✗ Incorrect (0 Marks)' : '— Unanswered (0 Marks)');
        const statusBadgeClass = isCorrect ? 'badge-success' : (isAnswered ? 'badge-danger' : 'badge-outline');

        const explanationText = q.explanation ? String(q.explanation) : 'Verified correct answer by assessment engine.';
        const explanationFormatted = escapeHtml(explanationText).replace(/\n/g, '<br/>');

        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; flex-wrap: wrap; gap: 0.5rem;">
                <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <span style="font-weight: 800; font-size: 1rem; color: var(--text-primary);">Question ${idx + 1}</span>
                    ${sectionBadge}
                    ${topicBadge}
                    ${diffBadge}
                </div>
                <span class="badge ${statusBadgeClass}">
                    ${statusBadgeText}
                </span>
            </div>

            <div style="font-weight: 600; font-size: 1rem; line-height: 1.5; color: var(--text-primary); margin-bottom: 1rem;">
                ${escapeHtml(q.question)}
            </div>

            ${passageHtml}

            <div style="margin-bottom: 1rem;">
                ${optionsHtml}
            </div>

            <div class="explanation-box">
                <div style="display: flex; align-items: center; gap: 0.35rem; color: var(--primary); font-weight: 700; margin-bottom: 0.35rem;">
                    <span>💡</span> Conceptual Solution &amp; Explanation:
                </div>
                <div style="line-height: 1.6; color: var(--text-secondary);">
                    ${explanationFormatted}
                </div>
            </div>
        `;

        container.appendChild(card);
    });
}

function escapeHtml(text) {
    if (!text && text !== 0) return '';
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

window.scrollToReviewSection = scrollToReviewSection;
window.filterReviewQuestions = filterReviewQuestions;
