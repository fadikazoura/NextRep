import { h, toast } from '../dom.js';
import { getLang, setLang, t } from '../i18n.js';
import { icon } from '../icons.js';
import { api, apiBlob, apiUpload, session } from '../api.js';
import { avatar, presetAvatar, PRESETS } from '../avatars.js';
import { currentUser, setCurrentUser, clearCurrentUser } from '../current-user.js';
import { resetProfileStatus } from '../profile-status.js';
import { phoneField } from '../phone-field.js';
import { clearFormErrors, passwordField, showFormErrors } from './auth.js';

/**
 * Account settings — who you are and your privacy. Training details (goal, days, equipment)
 * stay on the Profile tab, so the two never mix.
 */
export async function settingsView() {
    const user = await currentUser({ refresh: true });
    return h('section', { class: 'stack' },
        h('div', { class: 'stack', style: 'gap: 4px' },
            h('h1', {}, t('settings.title')),
            h('p', { class: 'muted' }, t('settings.subtitle'), ' ',
                h('a', { href: '#/profile' }, t('settings.trainingLink')))),
        languageSection(),
        avatarSection(user),
        accountSection(user),
        securitySection(),
        privacySection());
}

function section(iconName, title, ...children) {
    return h('section', { class: 'card stack settings-card' },
        h('h2', { class: 'with-icon settings-title' }, icon(iconName, { size: 20 }), title),
        ...children);
}

function formParts(submitLabel, { danger = false } = {}) {
    const formError = h('p', { class: 'form-error', role: 'alert', hidden: true });
    const submit = h('button', { class: `btn ${danger ? 'btn-danger-solid' : 'btn-primary'}`, type: 'submit' }, submitLabel);
    return { formError, submit };
}

function field(id, label, control, hint) {
    return h('div', { class: 'field', 'data-field': id },
        h('label', { for: id }, label),
        control,
        hint ? h('p', { class: 'muted small' }, hint) : null,
        h('p', { class: 'field-error', hidden: true }));
}

/** Runs a form's async action with a busy button and server errors shown next to the fields. */
function handleSubmit(form, formError, submit, action) {
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFormErrors(form, formError);
        submit.disabled = true;
        try {
            await action();
        } catch (error) {
            showFormErrors(form, formError, error);
        } finally {
            submit.disabled = false;
        }
    });
}

/* ---------- language ---------- */

function languageSection() {
    const options = [['en', 'English', 'EN'], ['he', 'עברית', 'עב']];
    const group = h('div', { class: 'segmented', role: 'radiogroup', 'aria-label': t('settings.language') },
        options.map(([code, name, short]) => h('button', {
            type: 'button', role: 'radio', class: 'segment', 'aria-checked': String(getLang() === code), lang: code,
            onclick: () => { if (getLang() !== code) setLang(code); }, // the whole app re-renders in the new language
        }, h('span', { class: 'segment-badge' }, short), name)));
    return section('globe', t('settings.language'), h('p', { class: 'muted small' }, t('settings.languageHint')), group);
}

/* ---------- avatar ---------- */

function avatarSection(user) {
    const preview = h('div', { class: 'avatar-preview' });
    const grid = h('div', { class: 'preset-grid', role: 'radiogroup', 'aria-label': t('settings.presets') });
    const fileInput = h('input', { type: 'file', accept: 'image/png,image/jpeg,image/webp', hidden: true });
    const error = h('p', { class: 'form-error', role: 'alert', hidden: true });

    const render = (current) => {
        preview.replaceChildren(avatar(current, 88));
        grid.replaceChildren(...PRESETS.map((preset) => {
            const selected = current.avatar.kind === 'PRESET' && current.avatar.preset === preset.name;
            return h('button', {
                type: 'button', class: 'preset-option', role: 'radio', 'aria-checked': String(selected),
                'aria-label': preset.name, onclick: () => run(() => api('/api/users/me/avatar', { method: 'PUT', body: { preset: preset.name } })),
            }, presetAvatar(preset.name, 48));
        }));
    };

    const run = async (request, options) => {
        error.hidden = true;
        try {
            const updated = await request();
            setCurrentUser(updated, options);
            render(updated);
            toast(t('settings.avatarSaved'));
        } catch (e) {
            error.hidden = false;
            error.textContent = e.message || t('errors.generic');
        }
    };

    fileInput.addEventListener('change', async () => {
        const file = fileInput.files[0];
        fileInput.value = '';
        if (!file) return;
        try {
            const small = await resizeImage(file, 256);
            await run(() => apiUpload('/api/users/me/avatar/photo', small), { photoChanged: true });
        } catch {
            error.hidden = false;
            error.textContent = t('settings.photoError');
        }
    });

    render(user);
    return section('user', t('settings.avatar'),
        h('div', { class: 'row', style: 'gap: 16px' },
            preview,
            h('div', { class: 'stack', style: 'gap: 8px; flex: 1; min-width: 180px' },
                h('button', { type: 'button', class: 'btn', onclick: () => fileInput.click() },
                    icon('camera', { size: 18 }), t('settings.uploadPhoto')),
                h('button', { type: 'button', class: 'btn btn-ghost',
                    onclick: () => run(() => api('/api/users/me/avatar', { method: 'DELETE' })) }, t('settings.useInitials')))),
        h('p', { class: 'muted small' }, t('settings.orPick')),
        grid,
        fileInput,
        error);
}

/**
 * Shrinks the photo in the browser before uploading: a square 256×256 crop from the middle, as WebP
 * (JPEG where WebP isn't supported). A 5 MB phone photo becomes ~20 KB, so it fits easily in the database.
 */
function resizeImage(file, size) {
    return new Promise((resolve, reject) => {
        const img = new Image();
        const url = URL.createObjectURL(file);
        img.onload = () => {
            URL.revokeObjectURL(url);
            const side = Math.min(img.width, img.height);
            const canvas = document.createElement('canvas');
            canvas.width = size;
            canvas.height = size;
            canvas.getContext('2d').drawImage(img, (img.width - side) / 2, (img.height - side) / 2, side, side, 0, 0, size, size);
            canvas.toBlob((blob) => {
                if (blob && blob.type === 'image/webp') return resolve(blob);
                canvas.toBlob((jpeg) => (jpeg ? resolve(jpeg) : reject(new Error('encode'))), 'image/jpeg', 0.85);
            }, 'image/webp', 0.85);
        };
        img.onerror = () => {
            URL.revokeObjectURL(url);
            reject(new Error('not an image'));
        };
        img.src = url;
    });
}

/* ---------- account ---------- */

function accountSection(user) {
    const username = h('input', {
        id: 'username', type: 'text', value: user.username, autocomplete: 'username', autocapitalize: 'none',
        minlength: 3, maxlength: 20, required: true,
    });
    const email = h('input', { id: 'email', type: 'email', value: user.email, disabled: true });
    const phone = phoneField('phone', user.phone);
    const { formError, submit } = formParts(t('common.save'));
    const form = h('form', { class: 'stack', novalidate: true },
        field('username', t('account.username'), username, t('account.usernameHint')),
        field('email', t('auth.email'), email, t('settings.emailLocked')),
        field('phone', t('account.phoneOptional'), phone.el),
        formError,
        submit);

    handleSubmit(form, formError, submit, async () => {
        const updated = await api('/api/users/me/account', {
            method: 'PUT',
            body: { username: username.value.trim(), phone: phone.value() },
        });
        setCurrentUser(updated);
        toast(t('settings.saved'));
    });
    return section('mail', t('settings.account'), form);
}

/* ---------- security ---------- */

function securitySection() {
    const current = h('input', { id: 'currentPassword', type: 'password', autocomplete: 'current-password', required: true });
    const next = h('input', { id: 'newPassword', type: 'password', autocomplete: 'new-password', minlength: 8, required: true });
    const { formError, submit } = formParts(t('settings.changePassword'));
    const form = h('form', { class: 'stack', novalidate: true },
        field('currentPassword', t('settings.currentPassword'), passwordField(current)),
        field('newPassword', t('settings.newPassword'), passwordField(next), t('auth.passwordHint')),
        formError,
        submit);

    handleSubmit(form, formError, submit, async () => {
        const { accessToken } = await api('/api/users/me/password', {
            method: 'PUT',
            body: { currentPassword: current.value, newPassword: next.value },
        });
        session.save(accessToken); // this device stays logged in with the new token
        current.value = '';
        next.value = '';
        toast(t('settings.passwordChanged'));
    });

    const everywhere = confirmButton(t('settings.logoutEverywhere'), t('settings.tapAgain'), async () => {
        await api('/api/users/me/sessions/revoke', { method: 'POST' });
        signOutLocally(t('settings.loggedOutEverywhere'));
    });

    return section('key', t('settings.security'), form,
        h('hr', { class: 'divider' }),
        h('p', { class: 'muted small' }, t('settings.logoutEverywhereHint')),
        everywhere);
}

/* ---------- privacy & data ---------- */

function privacySection() {
    const download = h('button', { type: 'button', class: 'btn' }, icon('download', { size: 18 }), t('settings.download'));
    download.addEventListener('click', async () => {
        download.disabled = true;
        try {
            const blob = await apiBlob('/api/users/me/export');
            const url = URL.createObjectURL(blob);
            const link = h('a', { href: url, download: `nextrep-data-${new Date().toISOString().slice(0, 10)}.json` });
            document.body.append(link);
            link.click();
            link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        } catch {
            toast(t('errors.generic'));
        } finally {
            download.disabled = false;
        }
    });

    // Deleting the account needs the password again, typed in a form that opens on purpose.
    const password = h('input', { id: 'password', type: 'password', autocomplete: 'current-password', required: true });
    const { formError, submit } = formParts(t('settings.deleteForever'), { danger: true });
    const deleteForm = h('form', { class: 'stack danger-zone', novalidate: true, hidden: true },
        h('p', { class: 'small' }, t('settings.deleteWarning')),
        field('password', t('settings.confirmPassword'), passwordField(password)),
        formError,
        submit);
    handleSubmit(deleteForm, formError, submit, async () => {
        await api('/api/users/me', { method: 'DELETE', body: { password: password.value } });
        signOutLocally(t('settings.deleted'));
    });
    const openDelete = h('button', { type: 'button', class: 'btn btn-ghost btn-danger' }, icon('trash', { size: 18 }), t('settings.deleteAccount'));
    openDelete.addEventListener('click', () => {
        deleteForm.hidden = !deleteForm.hidden;
        if (!deleteForm.hidden) password.focus();
    });

    return section('shield', t('settings.privacy'),
        h('p', { class: 'muted small' }, t('settings.downloadHint')),
        download,
        h('hr', { class: 'divider' }),
        openDelete,
        deleteForm);
}

/* ---------- helpers ---------- */

/** A button that needs two taps (no blocking confirm() dialog). */
function confirmButton(label, confirmLabel, action) {
    const button = h('button', { type: 'button', class: 'btn btn-ghost btn-danger' }, label);
    let armed = false;
    button.addEventListener('click', async () => {
        if (!armed) {
            armed = true;
            button.textContent = confirmLabel;
            setTimeout(() => { armed = false; button.textContent = label; }, 4000);
            return;
        }
        button.disabled = true;
        try {
            await action();
        } catch {
            button.disabled = false;
            toast(t('errors.generic'));
        }
    });
    return button;
}

function signOutLocally(message) {
    session.clear();
    resetProfileStatus();
    clearCurrentUser();
    location.hash = '#/login';
    toast(message);
}
