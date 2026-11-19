import { h } from './dom.js';
import { icon } from './icons.js';
import { photoUrl } from './current-user.js';

/** Built-in avatars (must match the server's list). Each is an icon on a colored circle. */
export const PRESETS = [
    { name: 'dumbbell', color: '#c6ff3d', ink: '#0c0e11' },
    { name: 'kettlebell', color: '#38bdf8', ink: '#06202e' },
    { name: 'barbell', color: '#f472b6', ink: '#2e0619' },
    { name: 'flame', color: '#fb923c', ink: '#2e1404' },
    { name: 'bolt', color: '#facc15', ink: '#2b2302' },
    { name: 'trophy', color: '#a78bfa', ink: '#170d33' },
    { name: 'medal', color: '#4ade80', ink: '#05230f' },
    { name: 'heart', color: '#f87171', ink: '#2e0707' },
    { name: 'target', color: '#22d3ee', ink: '#03252b' },
    { name: 'timer', color: '#e2e8f0', ink: '#111418' },
    { name: 'crown', color: '#fbbf24', ink: '#2b1d02' },
    { name: 'rocket', color: '#818cf8', ink: '#0f1133' },
];

const INITIAL_COLORS = ['#c6ff3d', '#38bdf8', '#f472b6', '#fb923c', '#a78bfa', '#4ade80', '#facc15', '#22d3ee'];

/** The same username always gets the same color. */
function colorFor(text) {
    let hash = 0;
    for (const ch of text) hash = (hash * 31 + ch.codePointAt(0)) >>> 0;
    return INITIAL_COLORS[hash % INITIAL_COLORS.length];
}

export function presetAvatar(name, size) {
    const preset = PRESETS.find((p) => p.name === name) ?? PRESETS[0];
    return h('span', { class: 'avatar', style: `--size:${size}px;background:${preset.color};color:${preset.ink}` },
        icon(preset.name, { size: Math.round(size * 0.55) }));
}

/** Avatar of a user: uploaded picture, preset or initials. The picture loads after, without blocking. */
export function avatar(user, size = 36) {
    const name = user?.username ?? '?';
    if (user?.avatar?.kind === 'PRESET') return presetAvatar(user.avatar.preset, size);

    const initials = h('span', {
        class: 'avatar avatar-initials',
        style: `--size:${size}px;background:${colorFor(name)};font-size:${Math.round(size * 0.42)}px`,
    }, name.charAt(0).toUpperCase());

    if (user?.avatar?.kind === 'PHOTO') {
        photoUrl().then((url) => {
            if (!url) return;
            initials.replaceChildren(h('img', { src: url, alt: '' }));
            initials.classList.add('avatar-photo');
        }).catch(() => { /* keep the initials */ });
    }
    return initials;
}
