import { h, spinner } from '../dom.js';
import { t } from '../i18n.js';
import { api } from '../api.js';
import { hasProfile } from '../profile-status.js';

const TILES = [
    { href: '#/plan', icon: '📅', key: 'home.plan' },
    { href: '#/workout', icon: '🏋️', key: 'home.workout' },
    { href: '#/progress', icon: '📈', key: 'home.progress' },
    { href: '#/profile', icon: '👤', key: 'home.profile' },
];

export async function homeView() {
    const [me, profileReady] = await Promise.all([api('/api/users/me'), hasProfile()]);
    return h('section', { class: 'stack' },
        h('div', { class: 'stack', style: 'gap: 4px' },
            h('h1', {}, t('home.hello', { email: me.email.split('@')[0] })),
            h('p', { class: 'muted' }, t('home.subtitle'))),
        profileReady ? null : h('a', { class: 'card card-link card-cta stack', href: '#/profile', style: 'gap: 6px' },
            h('h3', {}, `👤 ${t('home.setupTitle')}`),
            h('p', { class: 'small' }, t('home.setupText')),
            h('span', { class: 'btn btn-primary btn-block' }, t('home.setupButton'))),
        h('div', { class: 'grid-2' },
            TILES.map((tile) => h('a', { class: 'card card-link stack', href: tile.href, style: 'gap: 8px' },
                h('span', { class: 'tile-icon', 'aria-hidden': 'true' }, tile.icon),
                h('h3', {}, t(tile.key))))),
        h('p', { class: 'muted small' }, t('app.pitch')));
}

export function loadingView() {
    return spinner();
}
