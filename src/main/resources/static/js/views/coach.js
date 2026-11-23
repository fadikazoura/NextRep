import { h } from '../dom.js';
import { icon } from '../icons.js';
import { t, getLang } from '../i18n.js';
import { api, ApiError } from '../api.js';

/**
 * "Ask your coach": a chat window with the AI coach, who answers using the athlete's own data (the server adds it).
 * The conversation lives only in memory, in this tab: nothing is stored on the server, and logging out clears it.
 */
const MAX_HISTORY = 8; // turns sent along with a question, so follow-ups have context
let messages = []; // { fromUser, text }
let remaining = null;
let busy = false;

export function resetCoach() {
    messages = [];
    remaining = null;
}

export function openCoach() {
    const list = h('div', { class: 'coach-messages', 'aria-live': 'polite' });
    const input = h('textarea', {
        class: 'coach-input', rows: 1, maxlength: 500, placeholder: t('coach.placeholder'), 'aria-label': t('coach.placeholder'),
    });
    const send = h('button', { class: 'btn btn-primary coach-send', type: 'submit', 'aria-label': t('coach.send') },
        icon('chevron', { size: 20 }));
    const left = h('p', { class: 'muted tiny coach-left' });
    const suggestions = h('div', { class: 'coach-suggestions' });

    const dialog = h('dialog', { class: 'coach-dialog', 'aria-label': t('coach.title') });
    const close = () => {
        dialog.classList.add('closing');
        setTimeout(() => dialog.close(), 180);
    };

    const bubble = (message) => h('div', { class: `coach-msg ${message.fromUser ? 'from-user' : 'from-coach'} ${message.error ? 'is-error' : ''}` },
        message.text);
    const scrollDown = () => { list.scrollTop = list.scrollHeight; };
    const updateLeft = () => {
        left.textContent = remaining == null ? t('coach.note') : `${t('coach.left', { count: remaining })} · ${t('coach.note')}`;
    };
    const render = () => {
        list.replaceChildren(
            h('div', { class: 'coach-msg from-coach' }, t('coach.hello')),
            ...messages.map(bubble));
        suggestions.hidden = messages.length > 0;
        updateLeft();
        scrollDown();
    };

    const ask = async (question) => {
        question = question.trim();
        if (!question || busy) return;
        busy = true;
        send.disabled = true;
        const history = messages.filter((m) => !m.error).slice(-MAX_HISTORY).map(({ fromUser, text }) => ({ fromUser, text }));
        messages.push({ fromUser: true, text: question });
        input.value = '';
        autoSize();
        render();
        const typing = h('div', { class: 'coach-msg from-coach coach-typing', 'aria-label': t('coach.thinking') },
            h('span'), h('span'), h('span'));
        list.append(typing);
        scrollDown();

        try {
            const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
            const response = await api(`/api/users/me/coach?tz=${encodeURIComponent(tz)}`, {
                method: 'POST', body: { question, history, language: getLang() },
            });
            remaining = response.remainingToday;
            typing.remove();
            const message = { fromUser: false, text: response.answer };
            messages.push(message);
            const el = bubble({ ...message, text: '' });
            list.append(el);
            await typeOut(el, response.answer, scrollDown);
            updateLeft();
        } catch (error) {
            typing.remove();
            if (error instanceof ApiError && error.status === 401) return;
            messages.pop(); // the question got no answer: put it back in the box so it can be sent again
            render();
            input.value = question;
            const text = error instanceof ApiError && error.status === 429 ? t('coach.limit')
                : error instanceof ApiError && error.status === 0 ? t('errors.network') : t('coach.unavailable');
            list.append(bubble({ fromUser: false, text, error: true }));
            scrollDown();
        } finally {
            busy = false;
            send.disabled = false;
            input.focus();
        }
    };

    const autoSize = () => {
        input.style.height = 'auto';
        input.style.height = `${Math.min(input.scrollHeight, 120)}px`;
    };
    input.addEventListener('input', autoSize);
    input.addEventListener('keydown', (event) => { // Enter sends, Shift+Enter makes a new line
        if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            ask(input.value);
        }
    });

    suggestions.append(...['today', 'stuck', 'short', 'volume'].map((key) =>
        h('button', { class: 'chip chip-sm', type: 'button', onclick: () => ask(t(`coach.suggest.${key}`)) },
            t(`coach.suggest.${key}`))));

    const form = h('form', { class: 'coach-form' }, input, send);
    form.addEventListener('submit', (event) => {
        event.preventDefault();
        ask(input.value);
    });

    dialog.append(h('div', { class: 'coach-panel' },
        h('div', { class: 'coach-head' },
            h('span', { class: 'coach-avatar', 'aria-hidden': 'true' }, icon('sparkles', { size: 20 })),
            h('div', { class: 'stack', style: 'gap: 0' },
                h('strong', {}, t('coach.title')),
                h('span', { class: 'muted tiny' }, t('coach.subtitle'))),
            h('span', { class: 'spacer' }),
            h('button', { class: 'icon-btn', type: 'button', 'aria-label': t('plates.close'), onclick: close }, icon('close', { size: 18 }))),
        list,
        suggestions,
        form,
        left));

    dialog.addEventListener('cancel', (event) => { event.preventDefault(); close(); });
    dialog.addEventListener('click', (event) => { if (event.target === dialog) close(); });
    dialog.addEventListener('close', () => dialog.remove());
    document.body.append(dialog);
    render();
    dialog.showModal();
    input.focus();
}

/** Reveals the answer a few characters at a time, like someone typing (instantly with reduced motion). */
async function typeOut(el, text, onProgress) {
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
        el.textContent = text;
        onProgress();
        return;
    }
    const step = Math.max(2, Math.ceil(text.length / 120)); // long answers finish in about two seconds
    for (let i = step; i < text.length + step; i += step) {
        el.textContent = text.slice(0, i);
        onProgress();
        await new Promise((resolve) => setTimeout(resolve, 16));
        if (!el.isConnected) return;
    }
}
