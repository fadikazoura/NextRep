import { h, spinner, toast } from './dom.js';
import { getLang, setLang, t } from './i18n.js';
import { ApiError, session } from './api.js';
import { loginView, registerView } from './views/auth.js';
import { homeView } from './views/home.js';
import { planView } from './views/plan.js';
import { profileView } from './views/profile.js';
import { hasProfile, resetProfileStatus } from './profile-status.js';

/** Hash routes: #/path. Public routes work without logging in. */
const ROUTES = {
    '/login': { view: loginView, public: true },
    '/register': { view: registerView, public: true },
    '/': { view: homeView },
    '/plan': { view: planView, needsProfile: true },
    '/workout': { view: comingSoon, needsProfile: true },
    '/progress': { view: comingSoon, needsProfile: true },
    '/profile': { view: profileView },
};

const TABS = [
    { path: '/', icon: '⌂', key: 'nav.home' },
    { path: '/plan', icon: '📅', key: 'nav.plan' },
    { path: '/workout', icon: '🏋️', key: 'nav.workout' },
    { path: '/progress', icon: '📈', key: 'nav.progress' },
    { path: '/profile', icon: '👤', key: 'nav.profile' },
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
    main.replaceChildren(spinner());
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
    themeButton.textContent = theme === 'dark' ? '☀' : '☾';
    themeButton.setAttribute('aria-label', theme === 'dark' ? t('theme.toLight') : t('theme.toDark'));

    const langButton = document.getElementById('lang-toggle');
    langButton.textContent = t('lang.switch');
    langButton.setAttribute('aria-label', t('lang.switchLabel'));

    const logout = document.getElementById('logout');
    logout.hidden = !session.loggedIn;
    logout.textContent = t('auth.logout');

    const tabbar = document.getElementById('tabbar');
    tabbar.hidden = !session.loggedIn;
    tabbar.replaceChildren(...TABS.map((tab) => h('a', {
            href: `#${tab.path}`,
            'aria-current': tab.path === path ? 'page' : null,
        },
        h('span', { class: 'tab-icon', 'aria-hidden': 'true' }, tab.icon),
        h('span', {}, t(tab.key)))));
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

document.getElementById('logout').addEventListener('click', () => {
    session.clear();
    resetProfileStatus();
    location.hash = '#/login';
});

window.addEventListener('session-expired', () => {
    resetProfileStatus();
    pendingNotice = t('auth.sessionExpired');
    location.hash = '#/login';
    toast(t('auth.sessionExpired'));
});

window.addEventListener('langchange', render);
window.addEventListener('hashchange', render);
render();
