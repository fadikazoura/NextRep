import { h } from '../dom.js';
import { t, enumLabel } from '../i18n.js';
import { api, ApiError } from '../api.js';

async function loadPlan() {
    try {
        return await api('/api/users/me/plan');
    } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
    }
}

export async function planView() {
    const plan = await loadPlan();
    const root = h('section', { class: 'stack' });
    const formError = h('p', { class: 'form-error', role: 'alert', hidden: true });

    const generate = async (useAi, button) => {
        const buttons = root.querySelectorAll('button[data-generate]');
        buttons.forEach((b) => { b.disabled = true; });
        const label = button.textContent;
        button.textContent = useAi ? t('plan.generatingAi') : t('plan.generating');
        formError.hidden = true;
        try {
            const created = await api(`/api/users/me/plan/generate?ai=${useAi}`, { method: 'POST' });
            root.replaceChildren(...content(created));
        } catch (error) {
            if (error instanceof ApiError && error.status === 401) return;
            formError.hidden = false;
            formError.replaceChildren(...generateError(error));
            buttons.forEach((b) => { b.disabled = false; });
            button.textContent = label;
        }
    };

    const actions = (hasPlan) => h('div', { class: 'stack', style: 'gap: 8px' },
        h('button', { class: 'btn btn-primary btn-block', type: 'button', 'data-generate': true,
            onclick: (e) => generate(true, e.currentTarget) }, `✨ ${hasPlan ? t('plan.regenerateAi') : t('plan.generateAi')}`),
        h('button', { class: 'btn btn-block', type: 'button', 'data-generate': true,
            onclick: (e) => generate(false, e.currentTarget) }, t('plan.generateDefault')),
        hasPlan ? h('p', { class: 'muted small center' }, t('plan.replaceWarning')) : null,
        formError);

    const content = (current) => current
        ? [planHeader(current), ...current.days.map(dayCard), actions(true)]
        : [h('h1', {}, t('plan.title')),
            h('div', { class: 'card stack' }, h('p', {}, t('plan.empty')), actions(false))];

    root.replaceChildren(...content(plan));
    return root;
}

function generateError(error) {
    if (error instanceof ApiError && error.status === 404) {
        return [t('plan.needProfile'), ' ', h('a', { href: '#/profile' }, t('plan.goToProfile'))];
    }
    if (error instanceof ApiError && error.status === 0) return [t('errors.network')];
    return [t('errors.generic')];
}

function planHeader(plan) {
    return h('div', { class: 'stack', style: 'gap: 6px' },
        h('div', { class: 'row' },
            h('h1', {}, plan.name),
            h('span', { class: `badge ${plan.source === 'AI' ? 'badge-accent' : ''}` },
                plan.source === 'AI' ? t('plan.sourceAi') : t('plan.sourceDefault'))),
        h('p', { class: 'muted small' }, t('plan.daysPerWeek', { count: plan.days.length })));
}

function dayCard(day) {
    return h('article', { class: 'card stack', style: 'gap: 10px' },
        h('div', { class: 'row' },
            h('h3', {}, t(`day.${day.dayOfWeek}`)),
            h('span', { class: 'muted small' }, day.name),
            h('span', { class: 'spacer' }),
            h('a', { class: 'btn btn-sm', href: `#/workout?day=${day.id}` }, t('plan.startDay'))),
        h('ol', { class: 'exercise-list' },
            day.exercises.map((ex) => h('li', {},
                h('div', {},
                    h('div', { class: 'ex-name' }, ex.name),
                    h('div', { class: 'muted small' }, enumLabel(ex.muscleGroup))),
                h('div', { class: 'ex-scheme' },
                    ex.repMin === ex.repMax ? `${ex.sets} × ${ex.repMin}` : `${ex.sets} × ${ex.repMin}–${ex.repMax}`)))));
}
