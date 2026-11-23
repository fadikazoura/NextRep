import { h, toast } from '../dom.js';
import { icon } from '../icons.js';
import { t, enumLabel } from '../i18n.js';
import { api, ApiError } from '../api.js';
import { exerciseName } from './exercise-guide.js';
import { createRestTimer } from '../rest-timer.js';
import { celebratePr, estimateOneRepMax } from '../celebrate.js';
import { invalidateMuscleVolume } from './muscle-map.js';
import { maybePromptRating } from './feedback.js';
import { loadInsights, celebrateNewAchievements } from '../insights.js';
import { openPlateCalculator } from '../plate-calculator.js';
import { loadUnit, unitLabel, weightText, volumeText, fromInput, inputValue, weightStep } from '../units.js';

const BASE = '/api/users/me/workouts';

/** GET that turns "404 = nothing there" into null instead of an error. */
async function getOrNull(path) {
    try {
        return await api(path);
    } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
    }
}

function requestedDayId() {
    const query = location.hash.split('?')[1] ?? '';
    const day = Number(new URLSearchParams(query).get('day'));
    return Number.isInteger(day) && day > 0 ? day : null;
}

export async function workoutView() {
    await loadUnit();
    let workout = await getOrNull(`${BASE}/current`);
    const dayId = requestedDayId();
    let notice = null;

    if (!workout && dayId) {
        workout = await api(BASE, { method: 'POST', body: { planDayId: dayId } });
        history.replaceState(null, '', '#/workout'); // a reload must not start a second workout
    } else if (workout && dayId && workout.planDayId !== dayId) {
        notice = t('workout.alreadyRunning');
    }

    return workout ? activeWorkout(workout, notice) : pickWorkout();
}

/* ---------- no workout yet: choose a plan day or a free workout ---------- */

async function pickWorkout() {
    const plan = await getOrNull('/api/users/me/plan');
    const formError = h('p', { class: 'form-error', role: 'alert', hidden: true });

    const start = async (planDayId, button) => {
        button.disabled = true;
        try {
            await api(BASE, { method: 'POST', body: planDayId ? { planDayId } : {} });
            window.dispatchEvent(new HashChangeEvent('hashchange')); // re-render this route
        } catch (error) {
            if (error instanceof ApiError && error.status === 401) return;
            button.disabled = false;
            formError.hidden = false;
            formError.textContent = t('errors.generic');
        }
    };

    const today = new Date().toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase();

    return h('section', { class: 'stack' },
        h('div', { class: 'stack', style: 'gap: 4px' },
            h('h1', {}, t('workout.pickTitle')),
            h('p', { class: 'muted' }, t('workout.pickHint'))),
        plan ? plan.days.map((day) => h('div', { class: `card row ${day.dayOfWeek === today ? 'card-today' : ''}` },
            h('div', { class: 'stack', style: 'gap: 2px' },
                h('h3', {}, t(`day.${day.dayOfWeek}`), day.dayOfWeek === today ? h('span', { class: 'badge badge-accent today-badge' }, t('workout.today')) : null),
                h('span', { class: 'muted small' }, `${day.name} · ${t('workout.exerciseCount', { count: day.exercises.length })}`)),
            h('span', { class: 'spacer' }),
            h('button', { class: 'btn btn-primary btn-sm', type: 'button', onclick: (e) => start(day.id, e.currentTarget) },
                t('plan.startDay'))))
            : h('p', { class: 'muted' }, t('workout.noPlan'), ' ', h('a', { href: '#/plan' }, t('workout.toPlan'))),
        h('button', { class: 'btn btn-block', type: 'button', onclick: (e) => start(null, e.currentTarget) },
            t('workout.free')),
        formError);
}

/* ---------- workout in progress ---------- */

async function activeWorkout(workout, notice) {
    const [recommendations, records, catalog] = await Promise.all([
        workout.planDayId ? getOrNull(`/api/users/me/plan/days/${workout.planDayId}/recommendations`) : null,
        api('/api/users/me/records').catch(() => []),
        api('/api/exercises').catch(() => []), // to know which exercises use a barbell (plate calculator)
    ]);
    const barbell = new Set(catalog.filter((ex) => ex.equipment === 'BARBELL').map((ex) => ex.id));

    // Best results so far, per exercise, so a new personal record can be celebrated the moment it's logged.
    const bests = new Map(records.map((r) => [r.exerciseId, {
        heaviest: Number(r.heaviest?.weightKg ?? 0),
        e1rm: Number(r.estimatedOneRepMaxKg ?? 0),
    }]));
    const restTimer = createRestTimer();

    const root = h('section', { class: 'stack' });
    const cards = h('div', { class: 'stack' });
    const shownExercises = new Set();

    // Header with a live clock of the total workout time.
    const clockTime = h('span', {});
    const clock = h('span', { class: 'workout-clock with-icon' }, icon('timer', { size: 18 }), clockTime);
    const tick = () => {
        if (!root.isConnected && root.dataset.mounted) return clearInterval(timer);
        root.dataset.mounted = root.isConnected ? '1' : '';
        clockTime.textContent = formatDuration(Date.now() - Date.parse(workout.startedAt));
    };
    const timer = setInterval(tick, 1000);
    tick();

    const addCard = (exerciseId, name, recommendation) => {
        if (shownExercises.has(exerciseId)) return;
        shownExercises.add(exerciseId);
        cards.append(exerciseCard({
            workout, exerciseId, name, recommendation, barbell: barbell.has(exerciseId),
            onLogged: (set) => {
                restTimer.start();
                checkForRecord(set, name, bests);
            },
        }));
    };

    for (const rec of recommendations?.exercises ?? []) addCard(rec.exerciseId, rec.exerciseName, rec);
    for (const set of workout.sets) addCard(set.exerciseId, set.exerciseName, null);

    // Native append() would print null as the text "null", so empty parts are filtered out.
    root.append(...[
        h('div', { class: 'stack', style: 'gap: 4px' },
            h('h1', {}, workout.planDayName ?? t('workout.freeTitle')),
            clock),
        notice ? h('p', { class: 'form-error', role: 'status' }, notice) : null,
        cards,
        addExercisePicker((exercise) => addCard(exercise.id, exercise.name, null)),
        finishActions(workout, root, () => { clearInterval(timer); restTimer.stop(); }),
        restTimer.el,
    ].filter(Boolean));
    return root;
}

function checkForRecord(set, name, bests) {
    const weight = Number(set.weightKg);
    const e1rm = estimateOneRepMax(weight, set.reps);
    const best = bests.get(set.exerciseId);
    if (!best) {
        bests.set(set.exerciseId, { heaviest: weight, e1rm }); // first time ever: that's a baseline, not a PR
        return;
    }
    if (weight > best.heaviest) {
        celebratePr(name, t('pr.heaviest', { weight: weightText(weight), reps: set.reps }));
    } else if (e1rm > best.e1rm + 0.01) {
        celebratePr(name, t('pr.e1rm', { e1rm: weightText(e1rm) }));
    }
    best.heaviest = Math.max(best.heaviest, weight);
    best.e1rm = Math.max(best.e1rm, e1rm);
}

function exerciseCard({ workout, exerciseId, name, recommendation, barbell, onLogged }) {
    const mySets = () => workout.sets.filter((s) => s.exerciseId === exerciseId);
    const lastSet = mySets().at(-1);

    const weight = h('input', {
        type: 'number', inputmode: 'decimal', min: 0, max: 2200, step: weightStep(), placeholder: unitLabel(),
        'aria-label': t('workout.weight', { unit: unitLabel() }),
        value: inputValue(lastSet?.weightKg ?? recommendation?.weightKg),
    });
    const reps = h('input', {
        type: 'number', inputmode: 'numeric', min: 0, max: 100, step: 1, placeholder: t('workout.reps'),
        'aria-label': t('workout.reps'),
        value: lastSet?.reps ?? recommendation?.repMax ?? '',
    });
    const error = h('p', { class: 'field-error', hidden: true });
    const setList = h('ol', { class: 'set-list' });
    const counter = h('span', { class: 'muted small' });
    const logButton = h('button', { class: 'btn btn-primary', type: 'submit' }, t('workout.logSet'));

    const renderSets = () => {
        const sets = mySets();
        counter.textContent = recommendation
            ? t('workout.setsDone', { done: sets.length, total: recommendation.sets })
            : t('workout.setsLogged', { count: sets.length });
        setList.replaceChildren(...sets.map((set) => h('li', {},
            h('span', { class: 'set-num' }, `#${set.setNumber}`),
            h('span', { class: 'set-value' }, `${weightText(set.weightKg)} × ${set.reps}`),
            h('button', {
                class: 'icon-btn icon-btn-sm', type: 'button', 'aria-label': t('workout.deleteSet'),
                onclick: () => removeSet(set),
            }, icon('close', { size: 16 })))));
    };

    const removeSet = async (set) => {
        try {
            await api(`${BASE}/${workout.id}/sets/${set.id}`, { method: 'DELETE' });
            workout.sets = workout.sets.filter((s) => s.id !== set.id);
            renderSets();
        } catch (e) {
            if (!(e instanceof ApiError && e.status === 401)) toast(t('errors.generic'));
        }
    };

    const form = h('form', { class: 'set-form', novalidate: true }, weight, h('span', { class: 'muted' }, '×'), reps, logButton);
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        error.hidden = true;
        if (weight.value === '' || reps.value === '') {
            error.hidden = false;
            error.textContent = t('workout.fillBoth');
            return;
        }
        logButton.disabled = true;
        try {
            const set = await api(`${BASE}/${workout.id}/sets`, {
                method: 'POST',
                body: { exerciseId, weightKg: fromInput(weight.value), reps: Number(reps.value) },
            });
            workout.sets.push(set);
            renderSets();
            onLogged(set);
        } catch (e) {
            if (e instanceof ApiError && e.status === 401) return;
            error.hidden = false;
            error.textContent = e instanceof ApiError && e.status === 400
                ? Object.values(e.fieldErrors)[0] ?? e.message
                : t('errors.generic');
        } finally {
            logButton.disabled = false;
        }
    });

    const plates = barbell ? h('button', {
        class: 'icon-btn icon-btn-sm plate-open', type: 'button', 'aria-label': t('plates.open'), title: t('plates.open'),
        onclick: () => openPlateCalculator({
            weightKg: weight.value === '' ? null : fromInput(weight.value),
            onUse: (kg) => { weight.value = inputValue(kg); reps.focus(); },
        }),
    }, icon('barbell', { size: 18 })) : null;

    renderSets();
    return h('article', { class: 'card stack', style: 'gap: 10px' },
        h('div', { class: 'row' }, h('h3', {}, exerciseName(exerciseId, name, 'ex-title')), h('span', { class: 'spacer' }), counter, plates),
        recommendation ? recommendationLine(recommendation) : null,
        setList,
        form,
        error);
}

function recommendationLine(rec) {
    const range = rec.repMin === rec.repMax ? `${rec.repMin}` : `${rec.repMin}–${rec.repMax}`;
    const target = rec.weightKg != null
        ? `${rec.sets} × ${range} @ ${weightText(rec.weightKg)}`
        : `${rec.sets} × ${range}`;
    return h('div', { class: 'rec' },
        h('span', { class: `badge rec-${rec.action.toLowerCase()}` }, t(`action.${rec.action}`)),
        h('span', { class: 'rec-target' }, target),
        h('p', { class: 'muted small rec-reason' },
            rec.action === 'START' ? t('workout.startHint') : rec.reason));
}

function addExercisePicker(onPick) {
    const wrapper = h('div', { class: 'stack', style: 'gap: 8px' });
    const open = h('button', { class: 'btn btn-block btn-ghost', type: 'button' }, icon('plus', { size: 18 }), t('workout.addExercise'));
    open.addEventListener('click', async () => {
        open.disabled = true;
        try {
            const exercises = await api('/api/exercises');
            const select = h('select', { 'aria-label': t('workout.addExercise') },
                h('option', { value: '' }, t('workout.chooseExercise')),
                groupByMuscle(exercises).map(([group, list]) => h('optgroup', { label: enumLabel(group) },
                    list.map((ex) => h('option', { value: ex.id }, ex.name)))));
            select.addEventListener('change', () => {
                const exercise = exercises.find((ex) => ex.id === Number(select.value));
                if (exercise) onPick(exercise);
                wrapper.replaceChildren(open);
                open.disabled = false;
            });
            wrapper.replaceChildren(select);
            select.focus();
        } catch {
            open.disabled = false;
            toast(t('errors.generic'));
        }
    });
    wrapper.append(open);
    return wrapper;
}

function finishActions(workout, root, stopTimer) {
    const error = h('p', { class: 'form-error', role: 'alert', hidden: true });
    let discardArmed = false;

    const finish = h('button', { class: 'btn btn-primary btn-block', type: 'button' }, icon('check', { size: 18 }), t('workout.finish'));
    finish.addEventListener('click', async () => {
        finish.disabled = true;
        error.hidden = true;
        try {
            const done = await api(`${BASE}/${workout.id}/complete`, { method: 'POST' });
            stopTimer();
            invalidateMuscleVolume();
            root.replaceChildren(summary(done));
            // A new badge gets the stage; the rating question can wait for another workout.
            const newBadges = await loadInsights().then(celebrateNewAchievements).catch(() => 0);
            if (!newBadges) maybePromptRating();
        } catch (e) {
            if (e instanceof ApiError && e.status === 401) return;
            finish.disabled = false;
            error.hidden = false;
            error.textContent = e instanceof ApiError && e.status === 409 ? t('workout.emptyError') : t('errors.generic');
        }
    });

    // Two taps instead of a blocking confirm() dialog: the first tap arms, the second discards.
    const discard = h('button', { class: 'btn btn-block btn-ghost btn-danger', type: 'button' }, t('workout.discard'));
    discard.addEventListener('click', async () => {
        if (!discardArmed) {
            discardArmed = true;
            discard.textContent = t('workout.discardConfirm');
            setTimeout(() => { discardArmed = false; discard.textContent = t('workout.discard'); }, 4000);
            return;
        }
        discard.disabled = true;
        try {
            await api(`${BASE}/${workout.id}`, { method: 'DELETE' });
            stopTimer();
            toast(t('workout.discarded'));
            location.hash = '#/';
        } catch (e) {
            discard.disabled = false;
            if (!(e instanceof ApiError && e.status === 401)) toast(t('errors.generic'));
        }
    });

    return h('div', { class: 'stack', style: 'gap: 8px' }, error, finish, discard);
}

function summary(workout) {
    const sets = workout.sets;
    const volume = sets.reduce((sum, s) => sum + Number(s.weightKg) * s.reps, 0);
    const exercises = new Set(sets.map((s) => s.exerciseId)).size;
    const minutes = Math.max(1, Math.round((Date.parse(workout.completedAt) - Date.parse(workout.startedAt)) / 60000));
    const stat = (value, label) => h('div', { class: 'card stat' }, h('strong', {}, value), h('span', { class: 'muted small' }, label));

    return h('div', { class: 'stack' },
        h('div', { class: 'center stack', style: 'gap: 4px' },
            h('div', { class: 'done-badge' }, icon('trophy', { size: 40 })),
            h('h1', {}, t('workout.doneTitle')),
            h('p', { class: 'muted' }, t('workout.doneHint'))),
        h('div', { class: 'grid-2' },
            stat(`${minutes} ${t('workout.min')}`, t('workout.duration')),
            stat(String(sets.length), t('workout.sets')),
            stat(String(exercises), t('workout.exercises')),
            stat(volumeText(volume), t('workout.volume'))),
        h('a', { class: 'btn btn-primary btn-block', href: '#/progress' }, t('workout.seeProgress')),
        h('a', { class: 'btn btn-block', href: '#/' }, t('nav.home')));
}

/* ---------- helpers ---------- */

function groupByMuscle(exercises) {
    const groups = new Map();
    for (const ex of exercises) {
        if (!groups.has(ex.muscleGroup)) groups.set(ex.muscleGroup, []);
        groups.get(ex.muscleGroup).push(ex);
    }
    return [...groups.entries()];
}

function formatDuration(ms) {
    const total = Math.max(0, Math.floor(ms / 1000));
    const hours = Math.floor(total / 3600);
    const minutes = String(Math.floor((total % 3600) / 60)).padStart(2, '0');
    const seconds = String(total % 60).padStart(2, '0');
    return hours ? `${hours}:${minutes}:${seconds}` : `${minutes}:${seconds}`;
}
