import { h } from './dom.js';
import { t } from './i18n.js';
import { icon } from './icons.js';
import { avatar } from './avatars.js';

/**
 * Drop-down menu under the avatar in the top bar. Closes on Escape, on a click outside it, or after choosing.
 * items: [{ icon, label, href } | { icon, label, onSelect, danger }]
 */
let openMenu = null;

export function closeAccountMenu() {
    if (!openMenu) return;
    openMenu.menu.remove();
    openMenu.button.setAttribute('aria-expanded', 'false');
    document.removeEventListener('pointerdown', openMenu.outside, true);
    document.removeEventListener('keydown', openMenu.keys);
    openMenu = null;
}

export function toggleAccountMenu(button, user, items) {
    if (openMenu) {
        closeAccountMenu();
        return;
    }
    const entries = items.map((item) => {
        const content = [icon(item.icon, { size: 18 }), h('span', {}, item.label)];
        const el = item.href
            ? h('a', { class: 'menu-item', role: 'menuitem', href: item.href }, content)
            : h('button', { type: 'button', class: `menu-item ${item.danger ? 'danger' : ''}`, role: 'menuitem' }, content);
        el.addEventListener('click', () => {
            closeAccountMenu();
            item.onSelect?.();
        });
        return el;
    });

    const menu = h('div', { class: 'account-menu', role: 'menu', 'aria-label': t('menu.label') },
        h('div', { class: 'menu-header' },
            avatar(user, 44),
            h('div', { class: 'stack', style: 'gap: 0; min-width: 0' },
                h('strong', { class: 'truncate' }, user?.username ?? ''),
                h('span', { class: 'muted small truncate' }, user?.email ?? ''))),
        h('div', { class: 'menu-items' }, entries));

    const outside = (event) => {
        if (!menu.contains(event.target) && !button.contains(event.target)) closeAccountMenu();
    };
    const keys = (event) => {
        if (event.key === 'Escape') {
            closeAccountMenu();
            button.focus();
        }
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            const index = entries.indexOf(document.activeElement);
            const next = event.key === 'ArrowDown' ? index + 1 : index - 1;
            entries[(next + entries.length) % entries.length].focus();
        }
    };
    document.body.append(menu);
    document.addEventListener('pointerdown', outside, true);
    document.addEventListener('keydown', keys);
    button.setAttribute('aria-expanded', 'true');
    openMenu = { menu, button, outside, keys };
    entries[0]?.focus();
}
