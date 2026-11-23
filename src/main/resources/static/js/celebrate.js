import { h } from './dom.js';
import { t } from './i18n.js';
import { icon } from './icons.js';

const COLORS = ['var(--accent)', '#ffffff', '#ffd23f', '#4ade80', '#38bdf8'];

/** Estimated one-rep max (Epley), the same formula the server uses. A single rep is the 1RM itself. */
export function estimateOneRepMax(weightKg, reps) {
    if (reps <= 0) return 0;
    return reps === 1 ? weightKg : weightKg * (30 + reps) / 30;
}

/** Full-screen "New PR!" burst with confetti. Disappears by itself; a tap closes it sooner. */
export function celebratePr(exerciseName, detail) {
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    const confetti = reduceMotion ? [] : Array.from({ length: 36 }, () => h('span', {
        class: 'confetti',
        style: `--x:${Math.round(Math.random() * 100)}vw;--d:${(0.9 + Math.random() * 1.2).toFixed(2)}s;`
            + `--r:${Math.round(Math.random() * 720 - 360)}deg;--delay:${(Math.random() * 0.25).toFixed(2)}s;`
            + `background:${COLORS[Math.floor(Math.random() * COLORS.length)]}`,
    }));

    const overlay = h('div', { class: 'pr-burst', role: 'alert', onclick: () => overlay.remove() },
        confetti,
        h('div', { class: 'pr-card' },
            h('div', { class: 'pr-trophy' }, icon('trophy', { size: 44 })),
            h('div', { class: 'pr-title' }, t('pr.title')),
            h('div', { class: 'pr-exercise' }, exerciseName),
            h('div', { class: 'muted small' }, detail)));

    document.body.append(overlay);
    navigator.vibrate?.([80, 60, 80, 60, 160]);
    setTimeout(() => overlay.remove(), 2800);
}

/** "Achievement unlocked!" — the same burst as a PR, with the badge in the middle. */
export function celebrateAchievement(badgeEl, name, description) {
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    const confetti = reduceMotion ? [] : Array.from({ length: 30 }, () => h('span', {
        class: 'confetti',
        style: `--x:${Math.round(Math.random() * 100)}vw;--d:${(0.9 + Math.random() * 1.2).toFixed(2)}s;`
            + `--r:${Math.round(Math.random() * 720 - 360)}deg;--delay:${(Math.random() * 0.25).toFixed(2)}s;`
            + `background:${COLORS[Math.floor(Math.random() * COLORS.length)]}`,
    }));
    const overlay = h('div', { class: 'pr-burst badge-burst', role: 'alert', onclick: () => overlay.remove() },
        confetti,
        h('div', { class: 'pr-card' },
            h('div', { class: 'badge-burst-icon' }, badgeEl),
            h('div', { class: 'pr-title' }, t('badge.unlocked')),
            h('div', { class: 'pr-exercise' }, name),
            h('div', { class: 'muted small' }, description)));
    document.body.append(overlay);
    navigator.vibrate?.([60, 40, 120]);
    setTimeout(() => overlay.remove(), 3000);
}
