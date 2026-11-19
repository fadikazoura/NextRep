import { h, toast } from '../dom.js';
import { t, enumLabel } from '../i18n.js';
import { api, ApiError } from '../api.js';
import { markProfileSaved } from '../profile-status.js';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

const SELECTS = {
    sex: ['MALE', 'FEMALE'],
    experienceLevel: ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'],
    goal: ['STRENGTH', 'HYPERTROPHY', 'ENDURANCE'],
    equipment: ['FULL_GYM', 'DUMBBELLS_ONLY', 'HOME'],
    weightUnit: ['KG', 'LB'],
};

const DEFAULTS = {
    birthDate: '', sex: 'MALE', heightCm: '', experienceLevel: 'BEGINNER', goal: 'HYPERTROPHY',
    sessionMinutes: 60, equipment: 'FULL_GYM', weightUnit: 'KG', trainingDays: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
    injuries: '',
};

/** The profile is optional until the first save: a 404 means "new user", so we show an empty form. */
async function loadProfile() {
    try {
        return { profile: await api('/api/users/me/profile'), isNew: false };
    } catch (error) {
        if (error instanceof ApiError && error.status === 404) return { profile: DEFAULTS, isNew: true };
        throw error;
    }
}

export async function profileView({ notice } = {}) {
    const { profile, isNew } = await loadProfile();
    const errors = {};
    const errorFor = (name) => (errors[name] = h('p', { class: 'field-error', hidden: true }));

    const inputs = {
        birthDate: h('input', { id: 'birthDate', type: 'date', required: true, value: profile.birthDate ?? '',
            max: new Date().toISOString().slice(0, 10) }),
        heightCm: h('input', { id: 'heightCm', type: 'number', inputmode: 'numeric', min: 100, max: 250,
            required: true, value: profile.heightCm ?? '' }),
        sessionMinutes: h('input', { id: 'sessionMinutes', type: 'number', inputmode: 'numeric', min: 20, max: 180,
            step: 5, required: true, value: profile.sessionMinutes ?? '' }),
        injuries: h('textarea', { id: 'injuries', rows: 2, maxlength: 500, placeholder: t('profile.injuriesHint') },
            profile.injuries ?? ''),
    };
    for (const [name, options] of Object.entries(SELECTS)) {
        inputs[name] = h('select', { id: name },
            options.map((value) => h('option', { value, selected: value === profile[name] }, enumLabel(value))));
    }

    const selectedDays = new Set(profile.trainingDays ?? []);
    const dayChips = h('div', { class: 'chips', role: 'group', 'aria-labelledby': 'days-label' },
        DAYS.map((day) => {
            const chip = h('button', { type: 'button', class: 'chip', 'aria-pressed': String(selectedDays.has(day)) },
                t(`day.short.${day}`));
            chip.addEventListener('click', () => {
                selectedDays.has(day) ? selectedDays.delete(day) : selectedDays.add(day);
                chip.setAttribute('aria-pressed', String(selectedDays.has(day)));
            });
            return chip;
        }));

    const field = (name, control) => h('div', { class: 'field' },
        h('label', { for: name }, t(`profile.${name}`)), control, errorFor(name));

    const formError = h('p', { class: 'form-error', role: 'alert', hidden: true });
    const submit = h('button', { class: 'btn btn-primary btn-block', type: 'submit' },
        isNew ? t('profile.create') : t('common.save'));

    const showErrors = (error) => {
        const fields = error instanceof ApiError ? error.fieldErrors : {};
        for (const [name, el] of Object.entries(errors)) {
            el.hidden = !fields[name];
            el.textContent = fields[name] ?? '';
        }
        formError.hidden = Object.keys(fields).length > 0;
        formError.textContent = error instanceof ApiError && error.status === 0 ? t('errors.network')
            : (error instanceof ApiError && error.status === 400 ? error.message : t('errors.generic'));
    };

    const form = h('form', { class: 'card stack', novalidate: true },
        h('div', { class: 'grid-2' }, field('birthDate', inputs.birthDate), field('sex', inputs.sex)),
        h('div', { class: 'grid-2' }, field('heightCm', inputs.heightCm), field('weightUnit', inputs.weightUnit)),
        field('experienceLevel', inputs.experienceLevel),
        field('goal', inputs.goal),
        h('div', { class: 'grid-2' }, field('equipment', inputs.equipment), field('sessionMinutes', inputs.sessionMinutes)),
        h('div', { class: 'field' }, h('label', { id: 'days-label' }, t('profile.trainingDays')), dayChips, errorFor('trainingDays')),
        field('injuries', inputs.injuries),
        formError,
        submit);

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        formError.hidden = true;
        submit.disabled = true;
        try {
            await api('/api/users/me/profile', {
                method: 'PUT',
                body: {
                    birthDate: inputs.birthDate.value || null,
                    sex: inputs.sex.value,
                    heightCm: inputs.heightCm.value ? Number(inputs.heightCm.value) : null,
                    experienceLevel: inputs.experienceLevel.value,
                    goal: inputs.goal.value,
                    sessionMinutes: inputs.sessionMinutes.value ? Number(inputs.sessionMinutes.value) : null,
                    equipment: inputs.equipment.value,
                    weightUnit: inputs.weightUnit.value,
                    trainingDays: DAYS.filter((day) => selectedDays.has(day)),
                    injuries: inputs.injuries.value.trim() || null,
                },
            });
            Object.values(errors).forEach((el) => { el.hidden = true; });
            markProfileSaved();
            toast(t('profile.saved'));
            if (isNew) location.hash = '#/plan';
        } catch (error) {
            if (error instanceof ApiError && error.status === 401) return;
            showErrors(error);
        } finally {
            submit.disabled = false;
        }
    });

    return h('section', { class: 'stack' },
        h('div', { class: 'stack', style: 'gap: 4px' },
            isNew ? h('span', { class: 'badge badge-accent', style: 'align-self: flex-start' }, t('profile.setupStep')) : null,
            h('h1', {}, isNew ? t('profile.setupTitle') : t('profile.title')),
            h('p', { class: 'muted' }, isNew ? t('profile.newHint') : t('profile.hint'))),
        notice && isNew ? h('p', { class: 'form-error', role: 'status' }, notice) : null,
        form);
}
