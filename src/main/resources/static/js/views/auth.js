import { h } from '../dom.js';
import { t } from '../i18n.js';
import { api, ApiError, session } from '../api.js';
import { hasProfile, resetProfileStatus } from '../profile-status.js';

export function loginView({ notice } = {}) {
    return authForm({
        title: t('auth.loginTitle'),
        submitLabel: t('auth.login'),
        switchText: t('auth.noAccount'),
        switchLabel: t('auth.register'),
        switchHref: '#/register',
        notice,
        onSubmit: (credentials) => logIn(credentials),
    });
}

export function registerView() {
    return authForm({
        title: t('auth.registerTitle'),
        submitLabel: t('auth.register'),
        switchText: t('auth.haveAccount'),
        switchLabel: t('auth.login'),
        switchHref: '#/login',
        passwordHint: t('auth.passwordHint'),
        onSubmit: async (credentials) => {
            await api('/api/auth/register', { method: 'POST', body: credentials, authenticated: false });
            await logIn(credentials);
        },
    });
}

async function logIn(credentials) {
    const { accessToken } = await api('/api/auth/login', { method: 'POST', body: credentials, authenticated: false });
    session.save(accessToken);
    resetProfileStatus();
    location.hash = (await hasProfile()) ? '#/' : '#/profile';
}

function authForm({ title, submitLabel, switchText, switchLabel, switchHref, passwordHint, notice, onSubmit }) {
    const email = h('input', { id: 'email', type: 'email', autocomplete: 'email', required: true });
    const password = h('input', {
        id: 'password',
        type: 'password',
        autocomplete: switchHref === '#/login' ? 'new-password' : 'current-password',
        minlength: 8,
        required: true,
    });
    const emailError = h('p', { class: 'field-error', hidden: true });
    const passwordError = h('p', { class: 'field-error', hidden: true });
    const formError = h('p', { class: 'form-error', role: 'alert', hidden: true });
    const submit = h('button', { class: 'btn btn-primary btn-block', type: 'submit' }, submitLabel);

    const showErrors = (error) => {
        const fields = error instanceof ApiError ? error.fieldErrors : {};
        emailError.hidden = !fields.email;
        emailError.textContent = fields.email ?? '';
        passwordError.hidden = !fields.password;
        passwordError.textContent = fields.password ?? '';
        if (Object.keys(fields).length === 0) {
            formError.hidden = false;
            formError.textContent = messageFor(error);
        }
    };

    const form = h('form', {
            class: 'card stack',
            novalidate: true,
            onsubmit: async (event) => {
                event.preventDefault();
                formError.hidden = true;
                emailError.hidden = true;
                passwordError.hidden = true;
                submit.disabled = true;
                try {
                    await onSubmit({ email: email.value.trim(), password: password.value });
                } catch (error) {
                    showErrors(error);
                } finally {
                    submit.disabled = false;
                }
            },
        },
        h('h2', {}, title),
        notice ? h('p', { class: 'form-error' }, notice) : null,
        h('div', { class: 'field' }, h('label', { for: 'email' }, t('auth.email')), email, emailError),
        h('div', { class: 'field' },
            h('label', { for: 'password' }, t('auth.password')),
            passwordField(password),
            passwordHint ? h('p', { class: 'muted small' }, passwordHint) : null,
            passwordError),
        formError,
        submit,
        h('p', { class: 'muted small center' }, `${switchText} `, h('a', { href: switchHref }, switchLabel)),
    );

    return h('section', { class: 'stack' },
        h('div', { class: 'hero stack' },
            h('div', { class: 'brand' }, 'Next', h('span', {}, 'Rep')),
            h('p', { class: 'muted' }, t('app.tagline'))),
        form);
}

/** Wraps the password input with an eye button that toggles between hidden and visible text. */
function passwordField(input) {
    const toggle = h('button', { type: 'button', class: 'password-toggle', 'aria-pressed': 'false',
        'aria-controls': input.id, 'aria-label': t('auth.showPassword') }, '👁');
    toggle.addEventListener('click', () => {
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        toggle.textContent = show ? '🙈' : '👁';
        toggle.setAttribute('aria-pressed', String(show));
        toggle.setAttribute('aria-label', show ? t('auth.hidePassword') : t('auth.showPassword'));
        input.focus();
    });
    return h('div', { class: 'password-wrap' }, input, toggle);
}

function messageFor(error) {
    if (!(error instanceof ApiError)) return t('errors.generic');
    if (error.status === 0) return t('errors.network');
    if (error.status === 401) return t('errors.invalidCredentials');
    if (error.status === 409) return t('errors.emailTaken');
    return error.message || t('errors.generic');
}
