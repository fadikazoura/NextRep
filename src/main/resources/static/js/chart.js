/**
 * Minimal dependency-free line chart drawn as SVG.
 * points: [{ x: 'YYYY-MM-DD', y: number }] sorted by date. Colors come from CSS variables, so it follows the theme.
 */
const NS = 'http://www.w3.org/2000/svg';
const W = 320;
const H = 160;
const PAD = { top: 12, right: 12, bottom: 22, left: 40 };

function svg(tag, attrs = {}, text) {
    const el = document.createElementNS(NS, tag);
    for (const [key, value] of Object.entries(attrs)) el.setAttribute(key, String(value));
    if (text !== undefined) el.textContent = text;
    return el;
}

export function lineChart(points, { unit = '', label = '' } = {}) {
    const root = svg('svg', { viewBox: `0 0 ${W} ${H}`, class: 'chart', role: 'img', 'aria-label': label });
    if (points.length === 0) return root;

    const times = points.map((p) => Date.parse(p.x));
    const values = points.map((p) => p.y);
    let min = Math.min(...values);
    let max = Math.max(...values);
    if (min === max) { min -= 1; max += 1; }
    const margin = (max - min) * 0.1;
    min -= margin;
    max += margin;

    const t0 = Math.min(...times);
    const t1 = Math.max(...times);
    const xOf = (t) => (t1 === t0
        ? PAD.left + (W - PAD.left - PAD.right) / 2
        : PAD.left + ((t - t0) / (t1 - t0)) * (W - PAD.left - PAD.right));
    const yOf = (v) => PAD.top + (1 - (v - min) / (max - min)) * (H - PAD.top - PAD.bottom);

    // grid lines + y labels (min / middle / max)
    for (const v of [min + margin, (min + max) / 2, max - margin]) {
        root.append(svg('line', { x1: PAD.left, x2: W - PAD.right, y1: yOf(v), y2: yOf(v), class: 'chart-grid' }));
        root.append(svg('text', { x: PAD.left - 6, y: yOf(v) + 4, 'text-anchor': 'end', class: 'chart-label' },
            `${round(v)}${unit}`));
    }

    // x labels: first and last date
    const dateLabel = (iso) => new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
    root.append(svg('text', { x: PAD.left, y: H - 4, class: 'chart-label' }, dateLabel(points[0].x)));
    if (points.length > 1) {
        root.append(svg('text', { x: W - PAD.right, y: H - 4, 'text-anchor': 'end', class: 'chart-label' },
            dateLabel(points.at(-1).x)));
    }

    const coords = points.map((p, i) => [xOf(times[i]), yOf(p.y)]);
    if (coords.length > 1) {
        const line = coords.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(' ');
        const area = `${coords[0][0].toFixed(1)},${H - PAD.bottom} ${line} ${coords.at(-1)[0].toFixed(1)},${H - PAD.bottom}`;
        root.append(svg('polygon', { points: area, class: 'chart-area' }));
        root.append(svg('polyline', { points: line, class: 'chart-line' }));
    }
    coords.forEach(([x, y], i) => {
        const dot = svg('circle', { cx: x, cy: y, r: 3.5, class: 'chart-dot' });
        dot.append(svg('title', {}, `${points[i].x}: ${round(points[i].y)}${unit}`));
        root.append(dot);
    });
    return root;
}

function round(value) {
    return Math.abs(value) >= 100 ? Math.round(value) : Math.round(value * 10) / 10;
}
