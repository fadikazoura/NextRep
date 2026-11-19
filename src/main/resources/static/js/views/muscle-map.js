import { h, spinner } from '../dom.js';
import { t, enumLabel } from '../i18n.js';
import { api, ApiError } from '../api.js';
import { icon } from '../icons.js';
import { exerciseName } from './exercise-guide.js';

/*
 * Interactive body map: tap a muscle to see the exercises that train it.
 * Muscles are colored by how many sets you did for them in the last 7 days (a heat map).
 * The figure is drawn here as SVG paths for the LEFT half of the body; the right half is a mirror copy.
 */
const NS = 'http://www.w3.org/2000/svg';

const SILHOUETTE = [
    'M100 11c10 0 17 9 17 21s-7 22-17 22-17-10-17-22 7-21 17-21z', // head
    'M91 50h18v16H91z', // neck
    'M64 66q36-10 72 0l9 14q4 46-9 94l-6 34q-30 12-60 0l-6-34q-13-48-9-94z', // torso
    'M62 70q-14 6-17 26l-5 52q-4 34-2 64h12q4-30 8-62l8-40z', // left arm
    'M138 70q14 6 17 26l5 52q4 34 2 64h-12q-4-30-8-62l-8-40z', // right arm
    'M38 214q-4 14 2 22 8 2 10-6l-2-16z', // left hand
    'M162 214q4 14-2 22-8 2-10-6l2-16z', // right hand
    'M70 200q14 10 29 8l-3 98q-2 50-6 92h-12q-8-44-10-92-4-50 2-106z', // left leg
    'M130 200q-14 10-29 8l3 98q2 50 6 92h12q8-44 10-92 4-50-2-106z', // right leg
    'M78 398h14l2 12H72z', // left foot
    'M122 398h-14l-2 12h22z', // right foot
];

// Left-side muscle shapes per view; `center` shapes sit on the middle line and are not mirrored.
const FRONT = {
    SHOULDERS: ['M63 69q-13 5-16 24 6 8 13 4 3-13 11-21z'],
    CHEST: ['M73 77q13-6 26-2v33q-13 6-26 0-9-10-7-21z'],
    BICEPS: ['M49 101q-6 18-4 40 6 6 12 0 4-20 2-40z'],
    CORE: ['M71 117q7-4 11 0-2 44 2 74-10-4-14-20-4-30 1-54z'],
    QUADS: ['M73 212q12-5 24 1 2 40-5 80-10 6-16 0-10-42-3-81z'],
    CALVES: ['M79 314q-6 28 0 66h8q4-38 0-66z'],
};
const FRONT_CENTER = { CORE: ['M85 113h30q3 38-1 77-14 9-28 0-4-39-1-77z'] };

const BACK = {
    SHOULDERS: ['M63 69q-13 5-16 24 6 8 13 4 3-13 11-21z'],
    BACK: ['M70 82q15 1 28 11v56q-12 9-22 1-13-30-6-68z'],
    TRICEPS: ['M48 99q-6 19-2 43 7 6 13-2 3-21 1-41z'],
    GLUTES: ['M72 202q14-5 27 1v33q-13 9-25 1-9-16-2-35z'],
    HAMSTRINGS: ['M74 246q11-5 22 1 1 27-4 49-9 5-16 0-6-25-2-50z'],
    CALVES: ['M77 312q-9 24-3 50 8 10 16 0 4-26-2-50z'],
};
const BACK_CENTER = {
    BACK: ['M86 58h28l11 14q-25 12-50 0z', 'M85 153h30q3 24-1 45H86q-4-21-1-45z'],
};

const volumeCache = { data: null, at: 0 };
const exerciseCache = new Map();

function svgEl(tag, attrs = {}) {
    const el = document.createElementNS(NS, tag);
    for (const [key, value] of Object.entries(attrs)) el.setAttribute(key, String(value));
    return el;
}

function heatLevel(sets) {
    if (!sets) return 0;
    if (sets < 4) return 1;
    if (sets < 10) return 2;
    return 3;
}

export function muscleMapCard() {
    const card = h('div', { class: 'card stack muscle-card', style: 'gap: 12px' });
    let side = 'front';
    let selected = null;
    let volume = {};

    const figureBox = h('div', { class: 'muscle-figure' });
    let body3d = null; // set when the 3D figure is running; otherwise the flat SVG figure is used
    const hint = h('p', { class: 'muted small center' }, t('muscles.hint'));
    const details = h('div', { class: 'stack muscle-details', style: 'gap: 8px' }, hint);

    const sideButtons = ['front', 'back'].map((value) => h('button', {
        type: 'button', class: 'chip chip-sm', 'aria-pressed': String(value === side),
        onclick: () => flipTo(value),
    }, t(`muscles.${value}`)));

    const drawFigure = () => {
        const svg = svgEl('svg', { viewBox: '0 0 200 420', class: 'body-svg', role: 'group', 'aria-label': t('muscles.title') });
        const base = svgEl('g', { class: 'body-base' });
        SILHOUETTE.forEach((d) => base.append(svgEl('path', { d })));
        svg.append(base);

        const shapes = side === 'front' ? FRONT : BACK;
        const center = side === 'front' ? FRONT_CENTER : BACK_CENTER;
        const left = svgEl('g');
        const right = svgEl('g', { transform: 'translate(200 0) scale(-1 1)' });
        const middle = svgEl('g');

        const addMuscle = (parent, group, d) => {
            const path = svgEl('path', {
                d, class: 'muscle', 'data-muscle': group, 'data-level': heatLevel(volume[group]),
                tabindex: 0, role: 'button', 'aria-label': enumLabel(group),
                'aria-pressed': String(group === selected),
            });
            path.addEventListener('click', () => select(group));
            path.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    select(group);
                }
            });
            parent.append(path);
        };
        for (const [group, paths] of Object.entries(shapes)) {
            paths.forEach((d) => { addMuscle(left, group, d); addMuscle(right, group, d); });
        }
        for (const [group, paths] of Object.entries(center)) paths.forEach((d) => addMuscle(middle, group, d));
        svg.append(left, right, middle);
        if (side === 'front') {
            // decorative "six-pack" lines on top of the abs; clicks pass through to the muscle below
            svg.append(svgEl('path', { d: 'M100 116v72M88 138h24M88 162h24', class: 'muscle-detail' }));
        }
        figureBox.replaceChildren(svg);
    };

    const flipTo = (value) => {
        if (value === side) return;
        side = value;
        sideButtons.forEach((b, i) => b.setAttribute('aria-pressed', String(['front', 'back'][i] === side)));
        if (body3d) {
            body3d.face(side);
            return;
        }
        figureBox.classList.add('flipping');
        setTimeout(() => {
            drawFigure();
            figureBox.classList.remove('flipping');
        }, 180);
    };

    const select = async (group) => {
        selected = group;
        body3d?.setSelected(group);
        figureBox.querySelectorAll('.muscle').forEach((p) =>
            p.setAttribute('aria-pressed', String(p.dataset.muscle === group)));
        details.replaceChildren(spinner());
        try {
            if (!exerciseCache.has(group)) exerciseCache.set(group, await api(`/api/exercises?muscleGroup=${group}`));
            if (selected !== group) return;
            const exercises = exerciseCache.get(group);
            const sets = volume[group] ?? 0;
            details.replaceChildren(
                h('div', { class: 'row' },
                    h('h3', {}, enumLabel(group)),
                    h('span', { class: 'spacer' }),
                    h('span', { class: `badge ${sets ? 'badge-accent' : ''}` }, t('muscles.setsThisWeek', { sets }))),
                h('ul', { class: 'exercise-list' }, exercises.map((ex) => h('li', {},
                    exerciseName(ex.id, ex.name),
                    h('span', { class: 'muted small' }, enumLabel(ex.equipment))))));
        } catch {
            details.replaceChildren(h('p', { class: 'form-error' }, t('errors.generic')));
        }
    };

    card.append(
        h('div', { class: 'row' },
            h('h3', { class: 'with-icon' }, icon('bolt', { size: 18 }), t('muscles.title')),
            h('span', { class: 'spacer' }),
            h('div', { class: 'chips' }, sideButtons)),
        figureBox,
        h('div', { class: 'heat-legend muted small' },
            t('muscles.legend'),
            [0, 1, 2, 3].map((level) => h('span', { class: 'heat-swatch', 'data-level': level }))),
        details);

    drawFigure();
    const volumeReady = loadVolume().then((data) => {
        volume = data;
        if (body3d) body3d.setHeat(volume); else drawFigure();
    }).catch(() => { /* the map still works without the heat colors */ });

    // Upgrade to the 3D figure when the device supports WebGL. three.js is loaded only now (lazy), so the
    // rest of the app never pays for it; if anything fails, the SVG figure simply stays.
    upgradeTo3d(figureBox, select).then((instance) => {
        if (!instance) return;
        body3d = instance;
        hint.textContent = t('muscles.hint3d');
        volumeReady.then(() => body3d.setHeat(volume));
        if (selected) body3d.setSelected(selected);
    });
    return card;
}

async function upgradeTo3d(figureBox, onSelect) {
    try {
        const { createBody3d, webglAvailable } = await import('../body3d.js');
        if (!webglAvailable()) return null;
        const sex = await profileSex();
        const holder = h('div', { class: 'body3d' });
        const instance = createBody3d(holder, { sex, onSelect });
        figureBox.replaceChildren(holder);
        figureBox.classList.add('is-3d');
        return instance;
    } catch {
        return null;
    }
}

async function profileSex() {
    try {
        return (await api('/api/users/me/profile')).sex;
    } catch (error) {
        if (error instanceof ApiError && error.status === 404) return 'MALE';
        throw error;
    }
}

/** Sets per muscle in the last 7 days; cached for a minute so moving between pages doesn't refetch. */
async function loadVolume() {
    if (volumeCache.data && Date.now() - volumeCache.at < 60000) return volumeCache.data;
    const rows = await api('/api/users/me/muscles/volume?days=7');
    volumeCache.data = Object.fromEntries(rows.map((r) => [r.muscleGroup, r.sets]));
    volumeCache.at = Date.now();
    return volumeCache.data;
}

/** Called after a workout is finished, so the heat map shows the new sets right away. */
export function invalidateMuscleVolume() {
    volumeCache.data = null;
}
