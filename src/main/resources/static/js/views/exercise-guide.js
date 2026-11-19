import { h, spinner } from '../dom.js';
import { icon } from '../icons.js';
import { t, enumLabel, getLang } from '../i18n.js';
import { api } from '../api.js';

const cache = new Map();

/** A tappable exercise name that opens the how-to guide. */
export function exerciseName(exerciseId, name, className = 'ex-name') {
    return h('button', {
        type: 'button', class: `ex-link ${className}`, 'aria-haspopup': 'dialog',
        onclick: () => openGuide(exerciseId, name),
    }, name, h('span', { class: 'ex-info', 'aria-hidden': 'true' }, icon('info', { size: 16 })));
}

/** Opens a bottom sheet (native <dialog>) with steps, the common mistake and a video link. */
export async function openGuide(exerciseId, name) {
    const body = h('div', { class: 'stack' }, spinner());
    const close = h('button', { type: 'button', class: 'icon-btn', 'aria-label': t('guide.close') }, icon('close'));
    const dialog = h('dialog', { class: 'sheet', 'aria-label': name },
        h('div', { class: 'row' }, h('h2', {}, name), h('span', { class: 'spacer' }), close),
        body);

    close.addEventListener('click', () => dialog.close());
    dialog.addEventListener('click', (event) => { if (event.target === dialog) dialog.close(); }); // tap outside
    dialog.addEventListener('close', () => dialog.remove());
    document.body.append(dialog);
    dialog.showModal();

    try {
        if (!cache.has(exerciseId)) cache.set(exerciseId, await api(`/api/exercises/${exerciseId}/guide`));
        const guide = cache.get(exerciseId);
        const text = getLang() === 'he' && guide.he.steps.length ? guide.he : guide.en;

        body.replaceChildren(
            h('div', { class: 'row', style: 'gap: 8px' },
                h('span', { class: 'badge' }, enumLabel(guide.muscleGroup)),
                h('span', { class: 'badge' }, enumLabel(guide.equipment))),
            h('h3', {}, t('guide.howTo')),
            h('ol', { class: 'guide-steps' }, text.steps.map((step) => h('li', {}, step))),
            text.commonMistake
                ? h('div', { class: 'guide-mistake' }, h('strong', { class: 'with-icon' }, icon('warning', { size: 18 }), t('guide.mistake')), h('p', {}, text.commonMistake))
                : null,
            h('a', { class: 'btn btn-primary btn-block', href: guide.videoSearchUrl, target: '_blank', rel: 'noopener noreferrer' },
                icon('play', { size: 18 }), t('guide.watch')));
    } catch {
        body.replaceChildren(h('p', { class: 'form-error' }, t('errors.generic')));
    }
}
