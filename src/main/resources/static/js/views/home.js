import { h } from '../dom.js';
import { t, getLang } from '../i18n.js';
import { api, ApiError } from '../api.js';
import { hasProfile } from '../profile-status.js';
import { icon } from '../icons.js';
import { muscleMapCard } from './muscle-map.js';
import { currentUser } from '../current-user.js';
import { loadUnit, volumeText } from '../units.js';
import { session } from '../api.js';
import { resetProfileStatus } from '../profile-status.js';
import { clearCurrentUser } from '../current-user.js';

const DAY_NAMES = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']; // JS getDay() order

async function getOrNull(path) {
    try {
        return await api(path);
    } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
    }
}

/** Home answers one question first: "what should I do right now?" — then shows how the week is going. */
export async function homeView() {
    const [me, profileReady] = await Promise.all([currentUser(), hasProfile()]);
    const name = me.username;

    const header = h('div', { class: 'stack', style: 'gap: 2px' },
        h('p', { class: 'muted small eyebrow' }, todayLabel()),
        h('h1', {}, t('home.hello', { email: name })));

    if (!profileReady) {
        return h('section', { class: 'stack' }, header, setupCard());
    }

    const [plan, current, history] = await Promise.all([
        getOrNull('/api/users/me/plan'),
        getOrNull('/api/users/me/workouts/current'),
        api('/api/users/me/workouts?size=30'),
        loadUnit(), // so the volume below is shown in kg or lb
    ]);

    const weekStart = startOfWeek(new Date());
    const thisWeek = history.content.filter((w) => new Date(w.startedAt) >= weekStart);
    const volume = thisWeek.reduce((sum, w) => sum + Number(w.totalVolumeKg), 0);

    const main = h('section', { class: 'stack' },
        header,
        me.demo ? demoBanner() : null,
        current ? inProgressCard(current) : todayCard(plan),
        weekCard(plan, thisWeek, weekStart),
        h('div', { class: 'grid-2' },
            stat(icon('bolt', { size: 18 }), volumeText(volume), t('home.weekVolume')),
            stat(icon('flame', { size: 18 }), String(history.totalElements), t('home.totalWorkouts'))),
        h('div', { class: 'grid-2' },
            quickLink('#/progress', 'chart', t('home.progress')),
            quickLink('#/profile', 'user', t('home.profile'))));

    // On wide screens the muscle map sits beside the dashboard; on phones it comes after it.
    return h('div', { class: 'home-layout' }, main, h('aside', { class: 'home-aside' }, muscleMapCard()));
}

/** Shown inside a demo account: it is temporary, so invite the visitor to make a real one. */
function demoBanner() {
    const create = h('button', { class: 'btn btn-sm btn-primary', type: 'button' }, t('demo.create'));
    create.addEventListener('click', () => {
        session.clear();
        resetProfileStatus();
        clearCurrentUser();
        location.hash = '#/register';
    });
    return h('div', { class: 'card demo-banner' },
        icon('sparkles', { size: 20 }),
        h('span', { class: 'small' }, t('demo.banner')),
        create);
}

function setupCard() {
    return h('a', { class: 'card card-link card-cta stack', href: '#/profile', style: 'gap: 6px' },
        h('h3', { class: 'with-icon' }, icon('user', { size: 20 }), t('home.setupTitle')),
        h('p', { class: 'small' }, t('home.setupText')),
        h('span', { class: 'btn btn-primary btn-block' }, t('home.setupButton')));
}

function inProgressCard(workout) {
    const minutes = Math.max(1, Math.round((Date.now() - Date.parse(workout.startedAt)) / 60000));
    return h('div', { class: 'card card-hero stack', style: 'gap: 8px' },
        h('span', { class: 'eyebrow accent with-icon' }, h('span', { class: 'live-dot' }), t('home.inProgress')),
        h('h2', { class: 'hero-title' }, workout.planDayName ?? t('workout.freeTitle')),
        h('p', { class: 'muted small' }, t('home.inProgressInfo', { minutes, sets: workout.sets.length })),
        h('a', { class: 'btn btn-primary btn-block', href: '#/workout' }, icon('play', { size: 18 }), t('home.continue')));
}

function todayCard(plan) {
    if (!plan) {
        return h('div', { class: 'card card-hero stack', style: 'gap: 8px' },
            h('span', { class: 'eyebrow accent' }, t('home.noPlanEyebrow')),
            h('h2', { class: 'hero-title' }, t('home.noPlanTitle')),
            h('p', { class: 'muted small' }, t('home.noPlanText')),
            h('a', { class: 'btn btn-primary btn-block', href: '#/plan' }, icon('sparkles', { size: 18 }), t('home.buildPlan')));
    }

    const today = DAY_NAMES[new Date().getDay()];
    const day = plan.days.find((d) => d.dayOfWeek === today);
    if (day) {
        return h('div', { class: 'card card-hero stack', style: 'gap: 10px' },
            h('span', { class: 'eyebrow accent' }, t('home.todayEyebrow')),
            h('h2', { class: 'hero-title' }, day.name),
            h('p', { class: 'muted small' },
                `${t('workout.exerciseCount', { count: day.exercises.length })} · ${day.exercises.slice(0, 3).map((e) => e.name).join(', ')}${day.exercises.length > 3 ? '…' : ''}`),
            h('a', { class: 'btn btn-primary btn-block', href: `#/workout?day=${day.id}` }, icon('play', { size: 18 }), t('home.startToday')));
    }

    const next = nextTrainingDay(plan);
    return h('div', { class: 'card card-hero stack', style: 'gap: 8px' },
        h('span', { class: 'eyebrow' }, t('home.restEyebrow')),
        h('h2', { class: 'hero-title' }, t('home.restTitle')),
        next ? h('p', { class: 'muted small' }, t('home.nextDay', { day: t(`day.${next.dayOfWeek}`), name: next.name })) : null,
        h('a', { class: 'btn btn-block', href: '#/workout' }, t('home.trainAnyway')));
}

function weekCard(plan, thisWeek, weekStart) {
    const planned = new Set(plan?.days.map((d) => d.dayOfWeek) ?? []);
    const doneDays = new Set(thisWeek.map((w) => DAY_NAMES[new Date(w.startedAt).getDay()]));
    const todayIndex = Math.floor((startOfDay(new Date()) - weekStart) / 86400000);

    const days = Array.from({ length: 7 }, (_, i) => {
        const date = new Date(weekStart);
        date.setDate(weekStart.getDate() + i);
        const name = DAY_NAMES[date.getDay()];
        const state = doneDays.has(name) ? 'done' : planned.has(name) ? (i < todayIndex ? 'missed' : 'planned') : 'rest';
        return h('div', { class: `week-day ${state} ${i === todayIndex ? 'today' : ''}`, title: t(`day.${name}`) },
            h('span', { class: 'week-dot' }, state === 'done' ? icon('check', { size: 14 }) : null),
            h('span', { class: 'week-label' }, t(`day.short.${name}`)));
    });

    const goal = planned.size;
    const done = thisWeek.length;
    const left = Math.max(0, goal - done);
    return h('div', { class: 'card stack', style: 'gap: 14px' },
        h('div', { class: 'row week-head' },
            h('div', { class: 'stack', style: 'gap: 2px' },
                h('h3', {}, t('home.thisWeek')),
                h('span', { class: 'muted small' }, goal
                    ? t('home.weekProgress', { done, goal })
                    : t('home.weekCount', { done })),
                goal ? h('span', { class: `small week-hint ${left ? '' : 'week-hint-done'}` },
                    left ? t('home.weekLeft', { left }) : t('home.weekGoalHit')) : null),
            h('span', { class: 'spacer' }),
            goal ? progressRing(Math.min(1, done / goal)) : null),
        h('div', { class: 'week-strip' }, days));
}

/**
 * Circular progress for the weekly goal: the ring fills and the number counts up when the card appears.
 * Drawn with one SVG circle and stroke-dashoffset, so it is crisp at any size and needs no library.
 */
function progressRing(fraction) {
    const SIZE = 76;
    const STROKE = 8;
    const r = (SIZE - STROKE) / 2;
    const length = 2 * Math.PI * r;
    const pct = Math.round(fraction * 100);
    const NS = 'http://www.w3.org/2000/svg';
    const circle = (cls) => {
        const c = document.createElementNS(NS, 'circle');
        c.setAttribute('class', cls);
        c.setAttribute('cx', SIZE / 2);
        c.setAttribute('cy', SIZE / 2);
        c.setAttribute('r', r);
        c.setAttribute('stroke-width', STROKE);
        return c;
    };
    const svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('viewBox', `0 0 ${SIZE} ${SIZE}`);
    svg.setAttribute('width', SIZE);
    svg.setAttribute('height', SIZE);
    svg.setAttribute('aria-hidden', 'true');
    const bar = circle('ring-bar');
    bar.style.strokeDasharray = `${length}`;
    bar.style.strokeDashoffset = `${length}`; // empty; filled below so the transition runs
    svg.append(circle('ring-track'), bar);

    const number = h('strong', { class: 'ring-num' }, '0%');
    const ring = h('div', {
        class: `progress-ring ${pct >= 100 ? 'ring-full' : ''}`,
        role: 'img', 'aria-label': t('home.weekRingLabel', { pct }),
    }, svg, number);

    const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const show = () => {
        bar.style.strokeDashoffset = `${length * (1 - fraction)}`;
        if (reduced || pct === 0) {
            number.textContent = `${pct}%`;
            return;
        }
        const start = performance.now();
        const DURATION = 900;
        const step = (now) => {
            const p = Math.min(1, (now - start) / DURATION);
            const eased = 1 - (1 - p) ** 3;
            number.textContent = `${Math.round(pct * eased)}%`;
            if (p < 1) requestAnimationFrame(step);
        };
        requestAnimationFrame(step);
    };
    // Start once the ring is on screen (the view is built before it is attached).
    requestAnimationFrame(() => requestAnimationFrame(show));
    return ring;
}

function stat(iconEl, value, label) {
    return h('div', { class: 'card stat stat-left' },
        h('span', { class: 'muted small with-icon' }, iconEl, label),
        h('strong', { class: 'num' }, value));
}

function quickLink(href, iconName, label) {
    return h('a', { class: 'card card-link quick-link', href },
        icon(iconName, { size: 20 }), h('span', {}, label), h('span', { class: 'spacer' }), icon('chevron', { size: 18 }));
}

/* ---------- dates ---------- */

/** Hebrew users get a Sunday–Saturday week (as in Israel); English users get Monday–Sunday. */
function startOfWeek(date) {
    const firstDay = getLang() === 'he' ? 0 : 1;
    const start = startOfDay(date);
    const diff = (start.getDay() - firstDay + 7) % 7;
    start.setDate(start.getDate() - diff);
    return start;
}

function startOfDay(date) {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
}

function nextTrainingDay(plan) {
    const todayIndex = new Date().getDay();
    for (let offset = 1; offset <= 7; offset++) {
        const name = DAY_NAMES[(todayIndex + offset) % 7];
        const day = plan.days.find((d) => d.dayOfWeek === name);
        if (day) return day;
    }
    return null;
}

function todayLabel() {
    return new Date().toLocaleDateString(getLang() === 'he' ? 'he-IL' : 'en-US', { weekday: 'long', day: 'numeric', month: 'long' });
}

