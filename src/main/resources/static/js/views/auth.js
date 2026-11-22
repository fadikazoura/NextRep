import { h } from '../dom.js';
import { icon } from '../icons.js';
import { t } from '../i18n.js';
import { api, ApiError, session } from '../api.js';
import { hasProfile, resetProfileStatus } from '../profile-status.js';
import { clearCurrentUser } from '../current-user.js';
import { phoneField } from '../phone-field.js';

export function loginView({ notice } = {}) {
    const login = h('input', { id: 'login', type: 'text', autocomplete: 'username', autocapitalize: 'none', required: true });
    const password = passwordInput('current-password');
    return authPage({
        title: t('auth.loginTitle'),
        submitLabel: t('auth.login'),
        notice,
        fields: [
            field('login', t('auth.emailOrUsername'), login),
            field('password', t('auth.password'), passwordField(password)),
            h('a', { class: 'small forgot-link', href: '#/forgot-password' }, t('reset.forgotLink')),
        ],
        footer: h('p', { class: 'muted small center' }, `${t('auth.noAccount')} `, h('a', { href: '#/register' }, t('auth.register'))),
        onSubmit: () => logIn(login.value.trim(), password.value),
    });
}

export function registerView() {
    const username = h('input', {
        id: 'username', type: 'text', autocomplete: 'username', autocapitalize: 'none', spellcheck: 'false',
        minlength: 3, maxlength: 20, pattern: '[A-Za-z0-9._]{3,20}', required: true,
    });
    const email = h('input', { id: 'email', type: 'email', autocomplete: 'email', required: true });
    const phone = phoneField('phone', null);
    const password = passwordInput('new-password');
    return authPage({
        title: t('auth.registerTitle'),
        submitLabel: t('auth.register'),
        fields: [
            field('username', t('account.username'), username, t('account.usernameHint')),
            field('email', t('auth.email'), email),
            field('phone', t('account.phoneOptional'), phone.el),
            field('password', t('auth.password'), passwordField(password), t('auth.passwordHint')),
        ],
        footer: h('p', { class: 'muted small center' }, `${t('auth.haveAccount')} `, h('a', { href: '#/login' }, t('auth.login'))),
        onSubmit: async () => {
            await api('/api/auth/register', {
                method: 'POST',
                authenticated: false,
                body: {
                    username: username.value.trim(),
                    email: email.value.trim(),
                    phone: phone.value() || null,
                    password: password.value,
                },
            });
            await logIn(email.value.trim(), password.value);
        },
    });
}

async function logIn(login, password) {
    const { accessToken } = await api('/api/auth/login', { method: 'POST', body: { login, password }, authenticated: false });
    session.save(accessToken);
    resetProfileStatus();
    clearCurrentUser();
    location.hash = (await hasProfile()) ? '#/' : '#/profile';
}

/* ---------- building blocks ---------- */

/** A labelled input with a hint and an error line; the error is found by the input's id. */
export function field(id, label, control, hint) {
    return h('div', { class: 'field', 'data-field': id },
        h('label', { for: id }, label),
        control,
        hint ? h('p', { class: 'muted small' }, hint) : null,
        h('p', { class: 'field-error', hidden: true }));
}

function passwordInput(autocomplete) {
    return h('input', { id: 'password', type: 'password', autocomplete, minlength: 8, required: true });
}

/** Wraps the password input with an eye button that toggles between hidden and visible text. */
export function passwordField(input) {
    const toggle = h('button', {
        type: 'button', class: 'password-toggle', 'aria-pressed': 'false',
        'aria-controls': input.id, 'aria-label': t('auth.showPassword'),
    }, icon('eye'));
    toggle.addEventListener('click', () => {
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        toggle.replaceChildren(icon(show ? 'eyeOff' : 'eye'));
        toggle.setAttribute('aria-pressed', String(show));
        toggle.setAttribute('aria-label', show ? t('auth.hidePassword') : t('auth.showPassword'));
        input.focus();
    });
    return h('div', { class: 'password-wrap' }, input, toggle);
}

/** Shows server errors next to their fields ({ errors: { username: '...' } }), or as one message. */
export function showFormErrors(form, formError, error) {
    const fields = error instanceof ApiError ? error.fieldErrors : {};
    let shownNextToField = false;
    form.querySelectorAll('[data-field]').forEach((wrapper) => {
        const message = fields[wrapper.dataset.field];
        const line = wrapper.querySelector('.field-error');
        line.hidden = !message;
        line.textContent = message ?? '';
        if (message) shownNextToField = true;
    });
    formError.hidden = shownNextToField;
    formError.textContent = shownNextToField ? '' : messageFor(error);
}

export function clearFormErrors(form, formError) {
    form.querySelectorAll('.field-error').forEach((line) => { line.hidden = true; });
    formError.hidden = true;
}

export function authPage({ title, subtitle, submitLabel, notice, fields, footer, onSubmit }) {
    const formError = h('p', { class: 'form-error', role: 'alert', hidden: true });
    const submit = h('button', { class: 'btn btn-primary btn-block', type: 'submit' }, submitLabel);
    const form = h('form', { class: 'card stack', novalidate: true },
        h('h2', {}, title),
        subtitle ? h('p', { class: 'muted small' }, subtitle) : null,
        notice ? h('p', { class: 'form-error' }, notice) : null,
        fields,
        formError,
        submit,
        footer);

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFormErrors(form, formError);
        submit.disabled = true;
        try {
            await onSubmit();
        } catch (error) {
            showFormErrors(form, formError, error);
        } finally {
            submit.disabled = false;
        }
    });

    return h('section', { class: 'stack' },
        h('div', { class: 'hero stack' },
            h('div', { class: 'brand' }, 'Next', h('span', {}, 'Rep')),
            h('p', { class: 'muted' }, t('app.tagline'))),
        form);
}

function messageFor(error) {
    if (!(error instanceof ApiError)) return t('errors.generic');
    if (error.status === 0) return t('errors.network');
    if (error.status === 401) return t('errors.invalidCredentials');
    if (error.status === 409) return t('errors.emailTaken');
    return error.message || t('errors.generic');
}
