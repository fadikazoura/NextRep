import { h } from './dom.js';
import { icon } from './icons.js';
import { t } from './i18n.js';
import { isLb, unitLabel, toDisplay, fromInput } from './units.js';

/**
 * "What do I put on the bar?" — given a target weight, shows the plates for each side of a barbell.
 * Works in the user's unit (real kg plates or real lb plates), and when the exact weight can't be loaded
 * it shows the closest weight that can, so the user can log what is actually on the bar.
 */
const SETS = {
    KG: {
        bars: [20, 15, 10],
        // Competition (IWF) colors, so the picture looks like a real gym.
        plates: [[25, '#d62828'], [20, '#1d4ed8'], [15, '#eab308'], [10, '#16a34a'], [5, '#f1f5f9'], [2.5, '#1f2937'], [1.25, '#9ca3af']],
    },
    LB: {
        bars: [45, 35, 15],
        plates: [[45, '#1d4ed8'], [35, '#eab308'], [25, '#16a34a'], [10, '#f1f5f9'], [5, '#d62828'], [2.5, '#9ca3af']],
    },
};

/** Greedy works for these plate sizes: biggest plates first, never above the target. Exported for tests. */
export function platesPerSide(target, bar, plateSizes) {
    let left = Math.round(((target - bar) / 2) * 100) / 100;
    const result = [];
    for (const size of plateSizes) {
        while (left + 1e-9 >= size) {
            result.push(size);
            left = Math.round((left - size) * 100) / 100;
        }
    }
    return result;
}

export function openPlateCalculator({ weightKg, onUse }) {
    const set = SETS[isLb() ? 'LB' : 'KG'];
    const colorOf = new Map(set.plates);
    const sizes = set.plates.map(([size]) => size);
    let bar = set.bars[0];

    const target = h('input', {
        type: 'number', inputmode: 'decimal', min: 0, step: isLb() ? 5 : 2.5,
        value: weightKg ? toDisplay(weightKg) : bar * 3, 'aria-label': t('plates.target'),
    });
    const barChips = h('div', { class: 'chips', role: 'group', 'aria-label': t('plates.bar') });
    const visual = h('div', { class: 'plate-bar', 'aria-hidden': 'true' });
    const list = h('p', { class: 'plate-list' });
    const note = h('p', { class: 'muted small' });
    const use = h('button', { class: 'btn btn-primary btn-block', type: 'button' });
    let achieved = 0;

    const draw = () => {
        barChips.replaceChildren(...set.bars.map((b) => h('button', {
            type: 'button', class: 'chip chip-sm', 'aria-pressed': String(b === bar),
            onclick: () => { bar = b; draw(); },
        }, `${b} ${unitLabel()}`)));

        const wanted = Number(target.value) || 0;
        const plates = wanted > bar ? platesPerSide(wanted, bar, sizes) : [];
        achieved = Math.round((bar + 2 * plates.reduce((a, b) => a + b, 0)) * 100) / 100;

        const max = sizes[0];
        visual.replaceChildren(
            h('span', { class: 'plate-sleeve' }),
            ...plates.map((size, i) => h('span', {
                class: 'plate',
                style: `--h: ${38 + 62 * (size / max)}%; --c: ${colorOf.get(size)}; --i: ${i}`,
            })),
            h('span', { class: 'plate-collar' }));

        list.textContent = plates.length
            ? `${t('plates.perSide')}: ${groupPlates(plates)}`
            : t('plates.justBar');
        note.textContent = wanted > 0 && Math.abs(achieved - wanted) > 0.01
            ? t('plates.closest', { weight: `${achieved} ${unitLabel()}` }) : '';
        use.textContent = t('plates.use', { weight: `${achieved} ${unitLabel()}` });
    };
    target.addEventListener('input', draw);

    const dialog = h('dialog', { class: 'popup', 'aria-label': t('plates.title') });
    const close = () => {
        dialog.classList.add('closing');
        setTimeout(() => dialog.close(), 180);
    };
    use.addEventListener('click', () => {
        onUse?.(fromInput(achieved));
        close();
    });
    dialog.addEventListener('cancel', (event) => { event.preventDefault(); close(); });
    dialog.addEventListener('click', (event) => { if (event.target === dialog) close(); });
    dialog.addEventListener('close', () => dialog.remove());

    dialog.append(h('div', { class: 'rate-card plate-card stack' },
        h('div', { class: 'rate-badge plate-badge' }, icon('barbell', { size: 30 })),
        h('h2', { class: 'rate-title plate-title' }, t('plates.title')),
        h('label', { class: 'plate-target' }, h('span', { class: 'muted small' }, t('plates.target')), target,
            h('span', { class: 'muted' }, unitLabel())),
        h('div', { class: 'stack', style: 'gap: 4px; width: 100%' }, h('span', { class: 'muted small' }, t('plates.bar')), barChips),
        visual,
        list,
        note,
        use,
        h('button', { class: 'link-btn', type: 'button', onclick: close }, t('plates.close'))));

    draw();
    document.body.append(dialog);
    dialog.showModal();
    target.select();
}

/** [20, 20, 5] -> "2 × 20 + 5" */
function groupPlates(plates) {
    const counts = new Map();
    for (const p of plates) counts.set(p, (counts.get(p) ?? 0) + 1);
    return [...counts].map(([size, n]) => (n > 1 ? `${n} × ${size}` : `${size}`)).join(' + ');
}
