import { h, skeleton, toast } from './dom.js';
import { icon } from './icons.js';
import { getLang, setLang, t } from './i18n.js';
import { ApiError, session } from './api.js';
import { loginView, registerView } from './views/auth.js';
import { homeView } from './views/home.js';
import { planView } from './views/plan.js';
import { profileView } from './views/profile.js';
import { workoutView } from './views/workout.js';
import { progressView } from './views/progress.js';
import { hasProfile, resetProfileStatus } from './profile-status.js';
import { settingsView } from './views/settings.js';
import { avatar } from './avatars.js';
import { cachedUser, clearCurrentUser, currentUser } from './current-user.js';
import { closeAccountMenu, toggleAccountMenu } from './account-menu.js';
import { openContact, openRating } from './views/feedback.js';

/** Hash routes: #/path. Public routes work without logging in. */
const ROUTES = {
    '/login': { view: loginView, public: true },
    '/register': { view: registerView, public: true },
    '/': { view: homeView },
    '/plan': { view: planView, needsProfile: true },
    '/workout': { view: workoutView, needsProfile: true },
    '/progress': { view: progressView, needsProfile: true },
    '/profile': { view: profileView },
    '/settings': { view: settingsView },
};

const TABS = [
    { path: '/', icon: 'home', key: 'nav.home' },
    { path: '/plan', icon: 'calendar', key: 'nav.plan' },
    { path: '/workout', icon: 'dumbbell', key: 'nav.workout' },
    { path: '/progress', icon: 'chart', key: 'nav.progress' },
    { path: '/profile', icon: 'user', key: 'nav.profile' },
];

const main = document.getElementById('view');
let pendingNotice = null;

/** The route part of the hash, without query params: "#/workout?day=3" -> "/workout". */
function currentPath() {
    return location.hash.replace(/^#/, '').split('?')[0] || '/';
}

async function render() {
    const path = currentPath();
    const route = ROUTES[path];

    if (!route) {
        main.replaceChildren(h('p', { class: 'muted center' }, t('errors.notFound')));
        return;
    }
    if (!route.public && !session.loggedIn) {
        location.hash = '#/login';
        return;
    }
    if (route.public && session.loggedIn) {
        location.hash = '#/';
        return;
    }

    renderChrome(path);
    main.replaceChildren(skeleton());
    try {
        const notice = pendingNotice;
        pendingNotice = null;
        if (route.needsProfile && !(await hasProfile())) {
            pendingNotice = t('profile.required');
            location.hash = '#/profile';
            return;
        }
        const view = await route.view({ notice });
        if (currentPath() === path) main.replaceChildren(view);
    } catch (error) {
        if (error instanceof ApiError && error.status === 401) return; // session-expired handler redirects
        main.replaceChildren(h('p', { class: 'form-error' },
            error instanceof ApiError && error.status === 0 ? t('errors.network') : t('errors.generic')));
    }
    main.focus({ preventScroll: true });
}

function renderChrome(path) {
    const theme = document.documentElement.getAttribute('data-theme');
    const themeButton = document.getElementById('theme-toggle');
    themeButton.replaceChildren(icon(theme === 'dark' ? 'sun' : 'moon'));
    themeButton.setAttribute('aria-label', theme === 'dark' ? t('theme.toLight') : t('theme.toDark'));

    // Logged-in users change the language in Settings; the login and sign-up pages keep a quick toggle.
    const langButton = document.getElementById('lang-toggle');
    langButton.hidden = session.loggedIn;
    langButton.textContent = t('lang.switch');
    langButton.setAttribute('aria-label', t('lang.switchLabel'));

    renderAccountButton();

    // Rate us / Contact us live in the top bar, for logged-in users.
    const rate = document.getElementById('rate-button');
    rate.hidden = !session.loggedIn;
    rate.replaceChildren(icon('star', { size: 18 }));
    rate.setAttribute('aria-label', t('menu.rate'));
    rate.title = t('menu.rate');

    const contact = document.getElementById('contact-button');
    contact.hidden = !session.loggedIn;
    contact.replaceChildren(h('span', {}, t('menu.contact')));

    const tabbar = document.getElementById('tabbar');
    tabbar.hidden = !session.loggedIn;
    tabbar.replaceChildren(...TABS.map((tab) => h('a', {
            href: `#${tab.path}`,
            'aria-current': tab.path === path ? 'page' : null,
        },
        h('span', { class: 'tab-icon', 'aria-hidden': 'true' }, icon(tab.icon, { size: 22 })),
        h('span', {}, t(tab.key)))));
}

/** The avatar in the top bar; the user is loaded once and then kept in memory. */
function renderAccountButton() {
    const button = document.getElementById('account-button');
    button.hidden = !session.loggedIn;
    button.setAttribute('aria-label', t('menu.label'));
    if (!session.loggedIn) return;
    const user = cachedUser();
    button.replaceChildren(avatar(user, 36));
    if (!user) currentUser().catch(() => { /* a failed request is handled where it happens */ });
}

function logOut() {
    session.clear();
    resetProfileStatus();
    clearCurrentUser();
    location.hash = '#/login';
}

function comingSoon() {
    return h('section', { class: 'card stack' },
        h('p', {}, t('common.comingSoon')),
        h('a', { class: 'btn', href: '#/' }, t('common.back')));
}

document.getElementById('theme-toggle').addEventListener('click', () => {
    const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    try {
        localStorage.setItem('nextrep.theme', next);
    } catch { /* ignore */ }
    renderChrome(currentPath());
});

document.getElementById('lang-toggle').addEventListener('click', () => {
    setLang(getLang() === 'en' ? 'he' : 'en');
});

document.getElementById('account-button').addEventListener('click', (event) => {
    toggleAccountMenu(event.currentTarget, cachedUser(), [
        { icon: 'user', label: t('menu.trainingProfile'), href: '#/profile' },
        { icon: 'settings', label: t('menu.settings'), href: '#/settings' },
        { icon: 'logout', label: t('auth.logout'), onSelect: logOut, danger: true },
    ]);
});

document.getElementById('rate-button').addEventListener('click', () => {
    openRating().catch(() => toast(t('errors.generic')));
});
document.getElementById('contact-button').addEventListener('click', openContact);

window.addEventListener('userchange', renderAccountButton);

window.addEventListener('session-expired', () => {
    resetProfileStatus();
    clearCurrentUser();
    pendingNotice = t('auth.sessionExpired');
    location.hash = '#/login';
    toast(t('auth.sessionExpired'));
});

window.addEventListener('langchange', render);
window.addEventListener('hashchange', () => {
    closeAccountMenu();
    render();
});
render();

// Installable app (PWA): the service worker keeps the app shell available offline.
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js').catch(() => { /* the app works without it */ });
    });
}
