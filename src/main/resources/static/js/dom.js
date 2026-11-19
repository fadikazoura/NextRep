/**
 * Tiny DOM builder. Text is always inserted as text nodes (never as HTML),
 * so data coming from the API cannot inject markup or scripts (XSS-safe).
 *
 *   h('button', { class: 'btn', onclick: save }, 'Save')
 */
export function h(tag, props = {}, ...children) {
    const el = document.createElement(tag);
    for (const [key, value] of Object.entries(props ?? {})) {
        if (value === undefined || value === null || value === false) continue;
        if (key.startsWith('on') && typeof value === 'function') {
            el.addEventListener(key.slice(2).toLowerCase(), value);
        } else if (key === 'class') {
            el.className = value;
        } else if (key === 'text') {
            el.textContent = value;
        } else if (value === true) {
            el.setAttribute(key, '');
        } else {
            el.setAttribute(key, String(value));
        }
    }
    for (const child of children.flat(Infinity)) {
        if (child === null || child === undefined || child === false) continue;
        el.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
    return el;
}

export function spinner() {
    return h('div', { class: 'spinner', role: 'progressbar', 'aria-label': 'Loading' });
}

let toastTimer;

export function toast(message, ms = 3500) {
    const el = document.getElementById('toast');
    el.textContent = message;
    el.hidden = false;
    clearTimeout(toastTimer);
    if (ms > 0) {
        toastTimer = setTimeout(() => { el.hidden = true; }, ms);
    }
}

export function hideToast() {
    clearTimeout(toastTimer);
    document.getElementById('toast').hidden = true;
}
