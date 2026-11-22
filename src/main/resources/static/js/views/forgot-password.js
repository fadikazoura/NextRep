import { h, toast } from '../dom.js';
import { t } from '../i18n.js';
import { api } from '../api.js';
import { authPage, field, passwordField } from './auth.js';

const RESEND_AFTER_SECONDS = 30;

/**
 * "Forgot password" in two steps on one page: 1) the email, 2) the 6-digit code from the email + a new password.
 * The server never says whether the email has an account, so the message after step 1 is always the same.
 */
export function forgotPasswordView() {
    const container = h('div', {});
    container.append(emailStep(container));
    return container;
}

function emailStep(container, initialEmail = '') {
    const email = h('input', { id: 'email', type: 'email', autocomplete: 'email', required: true, value: initialEmail });
    return authPage({
        title: t('reset.title'),
        subtitle: t('reset.emailHint'),
        submitLabel: t('reset.sendCode'),
        fields: [field('email', t('auth.email'), email)],
        footer: backToLogin(),
        onSubmit: async () => {
            await api('/api/auth/password-reset', { method: 'POST', authenticated: false, body: { email: email.value.trim() } });
            container.replaceChildren(codeStep(container, email.value.trim()));
        },
    });
}

function codeStep(container, email) {
    const code = h('input', {
        id: 'code', class: 'code-input', type: 'text', inputmode: 'numeric', autocomplete: 'one-time-code',
        maxlength: 6, pattern: '\\d{6}', placeholder: '••••••', required: true,
    });
    code.addEventListener('input', () => { code.value = code.value.replace(/\D/g, '').slice(0, 6); });
    const password = h('input', { id: 'newPassword', type: 'password', autocomplete: 'new-password', minlength: 8, required: true });

    const resend = h('button', { type: 'button', class: 'link-btn' });
    let wait = RESEND_AFTER_SECONDS;
    const tick = () => {
        resend.disabled = wait > 0;
        resend.textContent = wait > 0 ? t('reset.resendIn', { seconds: wait }) : t('reset.resend');
        if (wait-- > 0) setTimeout(tick, 1000);
    };
    resend.addEventListener('click', async () => {
        resend.disabled = true;
        try {
            await api('/api/auth/password-reset', { method: 'POST', authenticated: false, body: { email } });
            toast(t('reset.codeResent'));
            wait = RESEND_AFTER_SECONDS;
            tick();
        } catch {
            resend.disabled = false;
            toast(t('errors.generic'));
        }
    });
    tick();

    const page = authPage({
        title: t('reset.codeTitle'),
        subtitle: t('reset.codeSent', { email }),
        submitLabel: t('reset.setPassword'),
        fields: [
            field('code', t('reset.code'), code),
            field('newPassword', t('settings.newPassword'), passwordField(password), t('auth.passwordHint')),
            h('div', { class: 'row', style: 'justify-content: space-between' },
                resend,
                h('button', { type: 'button', class: 'link-btn', onclick: () => container.replaceChildren(emailStep(container, email)) },
                    t('reset.otherEmail'))),
        ],
        footer: backToLogin(),
        onSubmit: async () => {
            await api('/api/auth/password-reset/confirm', {
                method: 'POST',
                authenticated: false,
                body: { email, code: code.value, newPassword: password.value },
            });
            toast(t('reset.done'));
            location.hash = '#/login';
        },
    });
    setTimeout(() => code.focus(), 0);
    return page;
}

function backToLogin() {
    return h('p', { class: 'muted small center' }, h('a', { href: '#/login' }, t('reset.backToLogin')));
}
