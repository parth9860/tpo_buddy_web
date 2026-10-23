/**
 * TPO Buddy — AI Buddy Chat Client
 */

document.addEventListener('DOMContentLoaded', () => {
    const chatForm = document.getElementById('chatForm');
    const chatInput = document.getElementById('chatInput');
    const messagesBox = document.getElementById('chatMessages');

    if (!chatForm || !chatInput || !messagesBox) return;

    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const text = chatInput.value.trim();
        if (!text) return;

        // Append user bubble
        appendMessage('user', text);
        chatInput.value = '';

        // Append typing indicator
        const typingId = appendTypingIndicator();
        messagesBox.scrollTop = messagesBox.scrollHeight;

        try {
            const res = await fetch(window.CONTEXT_PATH + '/api/ai/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: text })
            });

            const data = await res.json();
            removeTypingIndicator(typingId);

            if (data.success && data.reply) {
                appendMessage('assistant', data.reply);
            } else {
                appendMessage('assistant', 'Sorry, I encountered an issue analyzing your query. Please try again.');
            }
        } catch (err) {
            removeTypingIndicator(typingId);
            appendMessage('assistant', 'Unable to reach AI Buddy. Please verify your connection.');
        }

        messagesBox.scrollTop = messagesBox.scrollHeight;
    });

    window.sendQuickPrompt = function(promptText) {
        chatInput.value = promptText;
        chatForm.dispatchEvent(new Event('submit'));
    };
});

function appendMessage(sender, text) {
    const box = document.getElementById('chatMessages');
    const bubble = document.createElement('div');
    bubble.className = `message-bubble message-${sender}`;

    // Simple markdown formatting: bold **text**, linebreaks
    let formatted = escapeHtml(text)
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n/g, '<br/>');

    bubble.innerHTML = formatted;
    box.appendChild(bubble);
}

function appendTypingIndicator() {
    const box = document.getElementById('chatMessages');
    const id = 'typing-' + Date.now();
    const bubble = document.createElement('div');
    bubble.id = id;
    bubble.className = 'message-bubble message-assistant';
    bubble.innerHTML = '<span style="opacity: 0.7;">AI Buddy is analyzing your records...</span>';
    box.appendChild(bubble);
    return id;
}

function removeTypingIndicator(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

function escapeHtml(str) {
    return str.replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;');
}
