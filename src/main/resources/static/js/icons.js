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
    kettlebell: ['M12 21c-3.9 0-6.5-2.3-6.5-5.6S8.1 9.8 12 9.8s6.5 2.3 6.5 5.6S15.9 21 12 21z', 'M7.6 11.2V8a4.4 4.4 0 0 1 8.8 0v3.2'],
    barbell: ['M2 12h20', 'M5 8v8', 'M8 6v12', 'M16 6v12', 'M19 8v8'],
    medal: ['M12 22a6 6 0 1 0 0-12 6 6 0 0 0 0 12z', 'M8.5 11 6 2h4l2 5 2-5h4l-2.5 9', 'M12 14v4'],
    heart: ['M12 20s-7.5-4.5-9-9.5C2 6.5 5 4 7.8 4c1.8 0 3.2 1 4.2 2.4C13 5 14.4 4 16.2 4 19 4 22 6.5 21 10.5 19.5 15.5 12 20 12 20z'],
    target: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10z', 'M12 13a1 1 0 1 0 0-2 1 1 0 0 0 0 2z'],
    crown: ['M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5z'],
    rocket: ['M12 2c3 2.5 4.5 6 4.5 10l-2 4h-5l-2-4C7.5 8 9 4.5 12 2z', 'M9.5 16 7 20h3', 'M14.5 16l2.5 4h-3', 'M12 9.5h.01'],
    settings: ['M4 6h10', 'M18 6h2', 'M16 4v4', 'M4 12h4', 'M12 12h8', 'M10 10v4', 'M4 18h12', 'M18 16v4'],
    shield: ['M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z'],
    download: ['M12 3v12', 'M7 10l5 5 5-5', 'M4 21h16'],
    camera: ['M4 8h3l2-3h6l2 3h3v12H4z', 'M12 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8z'],
    key: ['M14.5 13.5a5 5 0 1 0-4-4L3 17v4h4v-2h2v-2h2l1.5-1.5', 'M16.5 7.5h.01'],
    mail: ['M3 6h18v12H3z', 'M3 7l9 6 9-6'],
    globe: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M3 12h18', 'M12 3c2.5 2.6 3.8 5.6 3.8 9s-1.3 6.4-3.8 9c-2.5-2.6-3.8-5.6-3.8-9S9.5 5.6 12 3z'],
    star: ['M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z'],
    message: ['M4 5h16v11H9l-5 4z'],
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
