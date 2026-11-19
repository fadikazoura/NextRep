import { h, toast } from '../dom.js';
import { t } from '../i18n.js';
import { icon } from '../icons.js';
import { api, ApiError } from '../api.js';

/** A bottom sheet (native <dialog>): focus stays inside it and Escape closes it. */
function sheet(title, body) {
    const close = h('button', { type: 'button', class: 'icon-btn', 'aria-label': t('guide.close') }, icon('close'));
    const dialog = h('dialog', { class: 'sheet', 'aria-label': title },
        h('div', { class: 'row' }, h('h2', {}, title), h('span', { class: 'spacer' }), close),
        body);
    close.addEventListener('click', () => dialog.close());
    dialog.addEventListener('click', (event) => { if (event.target === dialog) dialog.close(); });
    dialog.addEventListener('close', () => dialog.remove());
    document.body.append(dialog);
    dialog.showModal();
    return dialog;
}

/* ---------- Rate us ---------- */

const SNOOZE_KEY = 'nextrep.ratePromptSnoozedUntil';
const WORKOUTS_KEY = 'nextrep.completedWorkouts';
const SNOOZE_DAYS = 14;

function readNumber(key) {
    try {
        return Number(localStorage.getItem(key)) || 0;
    } catch {
        return 0;
    }
}

function writeNumber(key, value) {
    try {
        localStorage.setItem(key, String(value));
    } catch { /* ignore */ }
}

/**
 * Asks for a rating at a happy moment: right after the user finishes a workout (from the 2nd one on),
 * only if they never rated and didn't press "Not now" in the last two weeks.
 */
export async function maybePromptRating() {
    const finished = readNumber(WORKOUTS_KEY) + 1;
    writeNumber(WORKOUTS_KEY, finished);
    if (finished < 2 || Date.now() < readNumber(SNOOZE_KEY)) return;
    try {
        await api('/api/users/me/rating');
        return; // already rated: never nag again
    } catch (error) {
        if (!(error instanceof ApiError && error.status === 404)) return;
    }
    setTimeout(() => openRating({ prompt: true }), 1600); // let the "workout complete" screen land first
}

/** Centered pop-up with five big stars; after choosing, a comment box slides in. */
export async function openRating({ prompt = false } = {}) {
    let previous = null;
    try {
        previous = await api('/api/users/me/rating');
    } catch (error) {
        if (!(error instanceof ApiError && error.status === 404)) throw error;
    }

    let stars = previous?.stars ?? 0;
    const label = h('p', { class: 'rating-label' });
    const buttons = [1, 2, 3, 4, 5].map((value) => {
        const button = h('button', {
            type: 'button', class: 'star-btn', role: 'radio', style: `--i:${value}`,
            'aria-label': t('feedback.starsLabel', { count: value }),
        }, icon('star', { size: 44 }));
        button.addEventListener('mouseenter', () => paint(value));
        button.addEventListener('click', () => choose(value));
        return button;
    });
    const starRow = h('div', { class: 'stars', role: 'radiogroup', 'aria-label': t('feedback.rateTitle') }, buttons);
    starRow.addEventListener('mouseleave', () => paint(stars));

    const comment = h('textarea', { rows: 3, maxlength: 1000, placeholder: t('feedback.commentPlaceholder') }, previous?.comment ?? '');
    const submit = h('button', { type: 'submit', class: 'btn btn-primary btn-block' }, t('feedback.send'));
    const error = h('p', { class: 'form-error', hidden: true });
    const details = h('div', { class: 'rate-details stack', hidden: !stars }, comment, error, submit);
    const notNow = h('button', { type: 'button', class: 'link-btn' }, t('feedback.notNow'));

    const form = h('form', { class: 'rate-card stack' },
        h('div', { class: 'rate-badge' }, icon('star', { size: 30 })),
        h('h2', { class: 'rate-title' }, t('feedback.loveTitle')),
        h('p', { class: 'muted center rate-sub' }, previous ? t('feedback.ratedBefore') : t('feedback.rateIntro')),
        starRow, label, details, notNow);

    /** preview = stars lit while hovering; the chosen value is kept separately */
    function paint(preview) {
        buttons.forEach((b, i) => {
            b.classList.toggle('on', i < preview);
            b.setAttribute('aria-checked', String(i + 1 === stars));
        });
        label.textContent = preview ? t(`feedback.star${preview}`) : t('feedback.tapStars');
    }

    function choose(value) {
        stars = value;
        buttons.forEach((b, i) => {
            b.classList.remove('pop');
            if (i < value) {
                void b.offsetWidth; // restart the animation
                b.classList.add('pop');
            }
        });
        paint(value);
        if (details.hidden) {
            details.hidden = false;
            comment.focus({ preventScroll: true });
        }
    }

    const dialog = h('dialog', { class: 'popup', 'aria-label': t('feedback.rateTitle') }, form);
    const close = () => {
        dialog.classList.add('closing');
        setTimeout(() => dialog.close(), 180);
    };
    notNow.addEventListener('click', () => {
        if (prompt) writeNumber(SNOOZE_KEY, Date.now() + SNOOZE_DAYS * 86400000);
        close();
    });
    dialog.addEventListener('cancel', (event) => { // Escape key: animate out too
        event.preventDefault();
        notNow.click();
    });
    dialog.addEventListener('click', (event) => { if (event.target === dialog) notNow.click(); });
    dialog.addEventListener('close', () => dialog.remove());

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!stars) return;
        submit.disabled = true;
        try {
            await api('/api/users/me/rating', { method: 'PUT', body: { stars, comment: comment.value } });
            form.replaceChildren(
                h('div', { class: 'rate-badge rate-done' }, icon('check', { size: 30 })),
                h('h2', { class: 'rate-title' }, t('feedback.thanksTitle')),
                h('p', { class: 'muted center' }, t('feedback.thanksRating')));
            setTimeout(close, 1600);
        } catch {
            error.hidden = false;
            error.textContent = t('errors.generic');
            submit.disabled = false;
        }
    });

    paint(stars);
    document.body.append(dialog);
    dialog.showModal();
}

/* ---------- Contact us ---------- */

export function openContact() {
    const topics = ['QUESTION', 'BUG', 'IDEA', 'OTHER'];
    let topic = 'QUESTION';
    const chips = h('div', { class: 'chips', role: 'radiogroup', 'aria-label': t('feedback.topic') },
        topics.map((value) => h('button', {
            type: 'button', class: 'chip chip-sm', 'aria-pressed': String(value === topic),
            onclick: (event) => {
                topic = value;
                chips.querySelectorAll('.chip').forEach((c) => c.setAttribute('aria-pressed', String(c === event.currentTarget)));
            },
        }, t(`feedback.topic.${value}`))));
    const message = h('textarea', { rows: 5, minlength: 10, maxlength: 2000, required: true, placeholder: t('feedback.messagePlaceholder') });
    const counter = h('span', { class: 'muted small' }, '0/2000');
    message.addEventListener('input', () => { counter.textContent = `${message.value.length}/2000`; });
    const submit = h('button', { type: 'submit', class: 'btn btn-primary btn-block' }, icon('message', { size: 18 }), t('feedback.send'));
    const error = h('p', { class: 'form-error', role: 'alert', hidden: true });
    const form = h('form', { class: 'stack', novalidate: true },
        h('p', { class: 'muted' }, t('feedback.contactIntro')),
        h('div', { class: 'field' }, h('label', {}, t('feedback.topic')), chips),
        h('div', { class: 'field' }, h('label', {}, t('feedback.message')), message, counter),
        error, submit);

    const dialog = sheet(t('feedback.contactTitle'), form);
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        error.hidden = true;
        if (message.value.trim().length < 10) {
            error.hidden = false;
            error.textContent = t('feedback.tooShort');
            return;
        }
        submit.disabled = true;
        try {
            await api('/api/users/me/contact', { method: 'POST', body: { topic, message: message.value } });
            dialog.close();
            toast(t('feedback.thanksMessage'));
        } catch (e) {
            error.hidden = false;
            error.textContent = e instanceof ApiError && e.status === 429 ? t('feedback.tooMany') : t('errors.generic');
            submit.disabled = false;
        }
    });
}
