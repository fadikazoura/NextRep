/**
 * NextRep's own line icons (24×24, drawn with strokes), so they look the same on every phone —
 * unlike emoji, which every OS draws differently. Color follows the text color (currentColor).
 */
const NS = 'http://www.w3.org/2000/svg';

const ICONS = {
    home: ['M3 10.5 12 3l9 7.5', 'M5 9.5V21h14V9.5', 'M10 21v-6h4v6'],
    calendar: ['M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z', 'M3 10h18', 'M8 3v4', 'M16 3v4'],
    dumbbell: ['M2.5 9.5v5', 'M6 6.5v11', 'M18 6.5v11', 'M21.5 9.5v5', 'M6 12h12'],
    chart: ['M3 17l6-6 4 4 8-8', 'M15 7h6v6'],
    user: ['M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M4 21c0-4 3.6-7 8-7s8 3 8 7'],
    sun: ['M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M12 2v2', 'M12 20v2', 'M4.9 4.9l1.4 1.4', 'M17.7 17.7l1.4 1.4',
        'M2 12h2', 'M20 12h2', 'M4.9 19.1l1.4-1.4', 'M17.7 6.3l1.4-1.4'],
    moon: ['M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z'],
    logout: ['M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4', 'M16 17l5-5-5-5', 'M21 12H9'],
    info: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M12 16v-5', 'M12 8h.01'],
    play: ['M7 4.5l12 7.5-12 7.5z'],
    check: ['M4 12.5l5 5L20 6.5'],
    close: ['M6 6l12 12', 'M18 6L6 18'],
    plus: ['M12 5v14', 'M5 12h14'],
    minus: ['M5 12h14'],
    warning: ['M12 3.5 2.5 20h19z', 'M12 10v4', 'M12 17h.01'],
    eye: ['M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z'],
    eyeOff: ['M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z', 'M3 3l18 18'],
    timer: ['M12 21a8 8 0 1 0 0-16 8 8 0 0 0 0 16z', 'M12 9v4l2.5 2.5', 'M9 2h6'],
    trophy: ['M8 4h8v5a4 4 0 0 1-8 0z', 'M8 6H5a3 3 0 0 0 3 4', 'M16 6h3a3 3 0 0 1-3 4', 'M12 13v5', 'M8 21h8', 'M9.5 18h5'],
    flame: ['M12 22c4 0 7-2.8 7-7 0-3.5-2.2-5.5-3.7-8.5-.9 1.8-2 2.8-3.3 3C12 6.5 10.5 4 8.5 2c-.6 4-4.5 6.6-4.5 12.2C4 18.6 7.5 22 12 22z'],
    sparkles: ['M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z', 'M19 3v4', 'M17 5h4'],
    chevron: ['M9 6l6 6-6 6'],
    trash: ['M4 7h16', 'M9 7V4h6v3', 'M6 7l1 13h10l1-13'],
    bolt: ['M13 2 4 14h7l-1 8 9-12h-7z'],
};

export function icon(name, { size = 20, label } = {}) {
    const svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('width', size);
    svg.setAttribute('height', size);
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '2');
    svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round');
    svg.setAttribute('class', `icon icon-${name}`);
    if (label) {
        svg.setAttribute('role', 'img');
        svg.setAttribute('aria-label', label);
    } else {
        svg.setAttribute('aria-hidden', 'true');
    }
    for (const d of ICONS[name] ?? []) {
        const path = document.createElementNS(NS, 'path');
        path.setAttribute('d', d);
        svg.append(path);
    }
    return svg;
}
