import { h, toast } from '../dom.js';
import { icon } from '../icons.js';
import { t, enumLabel } from '../i18n.js';
import { api, ApiError } from '../api.js';
import { lineChart } from '../chart.js';
import { loadUnit, unitLabel, toDisplay, formatWeight, weightText, volumeText, fromInput, inputValue, isLb } from '../units.js';

// Every metric is a weight in kg from the server; the chart shows it in the user's unit.
const METRICS = [
    { key: 'estimatedOneRepMaxKg', label: 'progress.metric.e1rm', withUnit: true },
    { key: 'topWeightKg', label: 'progress.metric.top', withUnit: true },
    { key: 'volumeKg', label: 'progress.metric.volume', withUnit: false },
];

export async function progressView() {
    await loadUnit();
    const [records, bodyWeights, history] = await Promise.all([
        api('/api/users/me/records'),
        api('/api/users/me/body-weight'),
        api('/api/users/me/workouts?size=5'),
    ]);

    return h('section', { class: 'stack' },
        h('h1', {}, t('progress.title')),
        recentWorkouts(history),
        h('h2', {}, t('progress.records')),
        records.length
            ? h('div', { class: 'stack' }, sortByStrength(records).map(recordCard))
            : h('div', { class: 'card muted' }, t('progress.noRecords'), ' ',
                h('a', { href: '#/workout' }, t('progress.startOne'))),
        h('h2', {}, t('progress.bodyWeight')),
        bodyWeightCard(bodyWeights));
}

/* ---------- recent workouts ---------- */

function recentWorkouts(page) {
    if (!page.content.length) return null;
    return h('div', { class: 'card stack', style: 'gap: 8px' },
        h('div', { class: 'row' },
            h('h3', {}, t('progress.recent')),
            h('span', { class: 'spacer' }),
            h('span', { class: 'muted small' }, t('progress.totalWorkouts', { count: page.totalElements }))),
        h('ul', { class: 'exercise-list' }, page.content.map((w) => h('li', {},
            h('div', {},
                h('div', { class: 'ex-name' }, w.planDayName ?? t('workout.freeTitle')),
                h('div', { class: 'muted small' }, formatDate(w.startedAt))),
            h('div', { class: 'ex-scheme' }, `${w.setCount} ${t('workout.sets').toLowerCase()} · ${volumeText(w.totalVolumeKg)}`)))),
    );
}

/* ---------- personal records ---------- */

function sortByStrength(records) {
    return [...records].sort((a, b) => Number(b.estimatedOneRepMaxKg ?? 0) - Number(a.estimatedOneRepMaxKg ?? 0));
}

function recordCard(record) {
    const chartBox = h('div', { class: 'stack', style: 'gap: 8px', hidden: true });
    let loaded = null;
    let metric = METRICS[0];

    const draw = () => {
        const points = loaded.points
            .filter((p) => p[metric.key] != null)
            .map((p) => ({ x: p.date, y: toDisplay(p[metric.key]) }));
        const tabs = h('div', { class: 'chips' }, METRICS.map((m) => h('button', {
            type: 'button', class: 'chip chip-sm', 'aria-pressed': String(m === metric),
            onclick: () => { metric = m; draw(); },
        }, t(m.label))));
        chartBox.replaceChildren(tabs,
            points.length > 1 ? lineChart(points, { unit: metric.withUnit ? ` ${unitLabel()}` : '', label: `${record.exerciseName} — ${t(metric.label)}` })
                : h('p', { class: 'muted small' }, t('progress.needMore')));
    };

    const toggle = h('button', { class: 'btn btn-sm btn-ghost', type: 'button', 'aria-expanded': 'false' }, icon('chart', { size: 18 }), t('progress.chart'));
    toggle.addEventListener('click', async () => {
        const open = chartBox.hidden;
        chartBox.hidden = !open;
        toggle.setAttribute('aria-expanded', String(open));
        if (open && !loaded) {
            toggle.disabled = true;
            try {
                loaded = await api(`/api/users/me/exercises/${record.exerciseId}/progress`);
                draw();
            } catch (e) {
                chartBox.hidden = true;
                if (!(e instanceof ApiError && e.status === 401)) toast(t('errors.generic'));
            } finally {
                toggle.disabled = false;
            }
        }
    });

    const line = (label, set) => set
        ? h('div', { class: 'pr-line' }, h('span', { class: 'muted small' }, label),
            h('span', { class: 'pr-value' }, `${weightText(set.weightKg)} × ${set.reps}`))
        : null;

    return h('article', { class: 'card stack', style: 'gap: 10px' },
        h('div', { class: 'row' },
            h('div', { class: 'stack', style: 'gap: 2px' },
                h('h3', {}, record.exerciseName),
                h('span', { class: 'muted small' }, enumLabel(record.muscleGroup))),
            h('span', { class: 'spacer' }),
            record.estimatedOneRepMaxKg != null
                ? h('div', { class: 'pr-big' }, h('strong', {}, formatWeight(record.estimatedOneRepMaxKg)),
                    h('span', { class: 'muted small' }, t('progress.e1rmShort', { unit: unitLabel() })))
                : null),
        h('div', { class: 'grid-2' }, line(t('progress.heaviest'), record.heaviest), line(t('progress.mostReps'), record.mostReps)),
        toggle,
        chartBox);
}

/* ---------- body weight ---------- */

function bodyWeightCard(entries) {
    const sorted = [...entries].sort((a, b) => a.recordedOn.localeCompare(b.recordedOn));
    const card = h('div', { class: 'card stack' });

    const render = () => {
        const latest = sorted.at(-1);
        const first = sorted[0];
        const change = latest && first && latest !== first ? Number(latest.weightKg) - Number(first.weightKg) : null;

        const input = h('input', {
            type: 'number', inputmode: 'decimal', min: isLb() ? 55 : 25, max: isLb() ? 880 : 400, step: 0.1,
            placeholder: unitLabel(), 'aria-label': t('progress.todayWeight', { unit: unitLabel() }),
            value: inputValue(latest?.weightKg),
        });
        const error = h('p', { class: 'field-error', hidden: true });
        const save = h('button', { class: 'btn btn-primary', type: 'submit' }, t('common.save'));
        const form = h('form', { class: 'set-form set-form-2', novalidate: true }, input, save);
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            error.hidden = true;
            if (input.value === '') return;
            save.disabled = true;
            try {
                const saved = await api('/api/users/me/body-weight', { method: 'PUT', body: { weightKg: fromInput(input.value) } });
                const index = sorted.findIndex((e) => e.recordedOn === saved.recordedOn);
                if (index >= 0) sorted[index] = saved; else sorted.push(saved); // PUT = one entry per day (upsert)
                sorted.sort((a, b) => a.recordedOn.localeCompare(b.recordedOn));
                toast(t('progress.weightSaved'));
                render();
            } catch (e) {
                if (e instanceof ApiError && e.status === 401) return;
                save.disabled = false;
                error.hidden = false;
                error.textContent = e instanceof ApiError && e.status === 400
                    ? Object.values(e.fieldErrors)[0] ?? e.message : t('errors.generic');
            }
        });

        // Native replaceChildren() would print null as the text "null", so empty parts are filtered out.
        card.replaceChildren(...[
            latest
                ? h('div', { class: 'row' },
                    h('div', { class: 'pr-big pr-big-start' }, h('strong', {}, weightText(latest.weightKg)),
                        h('span', { class: 'muted small' }, formatDate(latest.recordedOn))),
                    h('span', { class: 'spacer' }),
                    change !== null ? h('span', { class: `badge ${change <= 0 ? 'badge-accent' : ''}` },
                        `${change > 0 ? '+' : ''}${weightText(change)}`) : null)
                : h('p', { class: 'muted' }, t('progress.noWeight')),
            sorted.length > 1
                ? lineChart(sorted.map((e) => ({ x: e.recordedOn, y: toDisplay(e.weightKg) })),
                    { unit: ` ${unitLabel()}`, label: t('progress.bodyWeight') })
                : null,
            h('label', { class: 'small muted' }, t('progress.todayWeight', { unit: unitLabel() })),
            form,
            error,
        ].filter(Boolean));
    };

    render();
    return card;
}

/* ---------- helpers ---------- */

function formatDate(iso) {
    return new Date(iso).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' });
}
