import { h, toast } from '../dom.js';
import { icon } from '../icons.js';
import { t, enumLabel } from '../i18n.js';
import { api, ApiError } from '../api.js';
import { lineChart } from '../chart.js';
import { loadInsights, celebrateNewAchievements, badgeIcon } from '../insights.js';
import { loadUnit, unitLabel, toDisplay, formatWeight, weightText, volumeText, fromInput, inputValue, isLb } from '../units.js';

// Every metric is a weight in kg from the server; the chart shows it in the user's unit.
const METRICS = [
    { key: 'estimatedOneRepMaxKg', label: 'progress.metric.e1rm', withUnit: true },
    { key: 'topWeightKg', label: 'progress.metric.top', withUnit: true },
    { key: 'volumeKg', label: 'progress.metric.volume', withUnit: false },
];

export async function progressView() {
    await loadUnit();
    const [records, bodyWeights, history, insights] = await Promise.all([
        api('/api/users/me/records'),
        api('/api/users/me/body-weight'),
        api('/api/users/me/workouts?size=5'),
        loadInsights().catch(() => null),
    ]);
    if (insights) celebrateNewAchievements(insights);

    return h('section', { class: 'stack' },
        h('h1', {}, t('progress.title')),
        recentWorkouts(history),
        achievementsSection(insights),
        h('h2', {}, t('progress.records')),
        records.length
            ? h('div', { class: 'stack' }, sortByStrength(records).map(recordCard))
            : h('div', { class: 'card muted' }, t('progress.noRecords'), ' ',
                h('a', { href: '#/workout' }, t('progress.startOne'))),
        h('h2', {}, t('progress.bodyWeight')),
        bodyWeightCard(bodyWeights));
}

/* ---------- recent workouts ---------- */

function recentWorkouts(firstPage) {
    if (!firstPage.content.length) return null;
    let page = firstPage.page;
    const list = h('ul', { class: 'exercise-list history-list' });
    const more = h('button', { class: 'btn btn-sm btn-ghost btn-block', type: 'button' }, t('progress.showMore'));

    // Every row opens the workout's sets; rows are real buttons for keyboard and screen-reader users.
    const row = (w) => h('li', {},
        h('button', { class: 'history-row', type: 'button', 'aria-label': `${t('progress.openWorkout')}: ${formatDate(w.startedAt)}`,
            onclick: () => openWorkoutDetail(w.id) },
            h('div', {},
                h('div', { class: 'ex-name' }, w.planDayName ?? t('workout.freeTitle')),
                h('div', { class: 'muted small' }, formatDate(w.startedAt))),
            h('span', { class: 'spacer' }),
            h('div', { class: 'ex-scheme' }, `${w.setCount} ${t('workout.sets').toLowerCase()} · ${volumeText(w.totalVolumeKg)}`),
            icon('chevron', { size: 16 })));
    const showMoreIfAny = (p) => { more.hidden = p.page + 1 >= p.totalPages; };

    list.append(...firstPage.content.map(row));
    showMoreIfAny(firstPage);
    more.addEventListener('click', async () => {
        more.disabled = true;
        try {
            const next = await api(`/api/users/me/workouts?size=${firstPage.size}&page=${page + 1}`);
            page = next.page;
            list.append(...next.content.map(row));
            showMoreIfAny(next);
        } catch (e) {
            if (!(e instanceof ApiError && e.status === 401)) toast(t('errors.generic'));
        } finally {
            more.disabled = false;
        }
    });

    return h('div', { class: 'card stack', style: 'gap: 8px' },
        h('div', { class: 'row' },
            h('h3', {}, t('progress.recent')),
            h('span', { class: 'spacer' }),
            h('span', { class: 'muted small' }, t('progress.totalWorkouts', { count: firstPage.totalElements }))),
        list,
        more);
}

/** A past workout, set by set, grouped by exercise — with a shortcut to do the same plan day again. */
async function openWorkoutDetail(id) {
    let workout;
    try {
        workout = await api(`/api/users/me/workouts/${id}`);
    } catch (e) {
        if (!(e instanceof ApiError && e.status === 401)) toast(t('errors.generic'));
        return;
    }
    const byExercise = new Map();
    for (const set of workout.sets) {
        if (!byExercise.has(set.exerciseId)) byExercise.set(set.exerciseId, { name: set.exerciseName, sets: [] });
        byExercise.get(set.exerciseId).sets.push(set);
    }
    const volume = workout.sets.reduce((sum, s) => sum + Number(s.weightKg) * s.reps, 0);
    const minutes = workout.completedAt
        ? Math.max(1, Math.round((Date.parse(workout.completedAt) - Date.parse(workout.startedAt)) / 60000)) : null;

    const dialog = h('dialog', { class: 'popup', 'aria-label': workout.planDayName ?? t('workout.freeTitle') });
    const close = () => {
        dialog.classList.add('closing');
        setTimeout(() => dialog.close(), 180);
    };
    const stat = (value, label) => h('div', { class: 'detail-stat' }, h('strong', { class: 'num' }, value),
        h('span', { class: 'muted small' }, label));

    dialog.append(h('div', { class: 'rate-card detail-card stack' },
        h('div', { class: 'stack', style: 'gap: 2px' },
            h('h2', { class: 'detail-title' }, workout.planDayName ?? t('workout.freeTitle')),
            h('span', { class: 'muted small' }, formatDate(workout.startedAt))),
        h('div', { class: 'detail-stats' },
            minutes ? stat(`${minutes} ${t('workout.min')}`, t('workout.duration')) : null,
            stat(String(workout.sets.length), t('workout.sets')),
            stat(volumeText(volume), t('workout.volume'))),
        h('div', { class: 'detail-list' }, [...byExercise.values()].map((exercise) => h('div', { class: 'detail-ex' },
            h('strong', {}, exercise.name),
            h('div', { class: 'set-chips' }, exercise.sets.map((s) => h('span', { class: 'set-chip' },
                `${weightText(s.weightKg)} × ${s.reps}`)))))),
        workout.planDayId ? h('button', {
            class: 'btn btn-primary btn-block', type: 'button',
            onclick: () => { close(); location.hash = `#/workout?day=${workout.planDayId}`; },
        }, icon('play', { size: 18 }), t('progress.repeat')) : null,
        h('button', { class: 'link-btn', type: 'button', onclick: close }, t('plates.close'))));

    dialog.addEventListener('cancel', (event) => { event.preventDefault(); close(); });
    dialog.addEventListener('click', (event) => { if (event.target === dialog) close(); });
    dialog.addEventListener('close', () => dialog.remove());
    document.body.append(dialog);
    dialog.showModal();
}

/* ---------- achievements ---------- */

function achievementsSection(insights) {
    if (!insights) return null;
    const all = insights.achievements;
    const earned = all.filter((a) => a.unlocked).length;
    return h('section', { class: 'stack', style: 'gap: 10px' },
        h('div', { class: 'row' },
            h('h2', {}, t('badge.title')),
            h('span', { class: 'spacer' }),
            h('span', { class: 'badge badge-accent' }, `${earned}/${all.length}`)),
        h('div', { class: 'badge-grid' }, all.map((a) => h('div', {
            class: `badge-tile ${a.unlocked ? '' : 'locked'}`, title: t(`badge.${a.code}.desc`),
        },
            badgeIcon(a.code, a.unlocked),
            h('strong', { class: 'small' }, t(`badge.${a.code}`)),
            h('span', { class: 'muted tiny' }, a.unlocked ? formatDay(a.unlockedOn) : t(`badge.${a.code}.desc`)),
            a.unlocked ? null : h('div', { class: 'badge-progress', role: 'progressbar',
                'aria-valuemin': 0, 'aria-valuemax': 100, 'aria-valuenow': Math.round(a.progress * 100) },
            h('span', { style: `width: ${Math.round(a.progress * 100)}%` }))))));
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

/** A date without a time ("2026-11-20"): read at noon so no time zone can move it to another day. */
function formatDay(isoDate) {
    return new Date(`${isoDate}T12:00:00`).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatDate(iso) {
    return new Date(iso).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' });
}
