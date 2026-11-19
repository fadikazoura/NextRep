import { h, toast } from './dom.js';
import { t } from './i18n.js';
import { icon } from './icons.js';

const STORAGE_KEY = 'nextrep.restSeconds';
const MIN = 15;
const MAX = 600;
let audio = null;

function savedDuration() {
    try {
        const value = Number(localStorage.getItem(STORAGE_KEY));
        return value >= MIN && value <= MAX ? value : 90;
    } catch {
        return 90;
    }
}

function saveDuration(seconds) {
    try {
        localStorage.setItem(STORAGE_KEY, String(seconds));
    } catch { /* ignore */ }
}

/** Two short beeps. The AudioContext is created on a tap (browsers block sound that starts without one). */
function beep() {
    if (!audio) return;
    const now = audio.currentTime;
    for (const offset of [0, 0.22]) {
        const osc = audio.createOscillator();
        const gain = audio.createGain();
        osc.frequency.value = 880;
        gain.gain.setValueAtTime(0.0001, now + offset);
        gain.gain.exponentialRampToValueAtTime(0.25, now + offset + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + offset + 0.16);
        osc.connect(gain).connect(audio.destination);
        osc.start(now + offset);
        osc.stop(now + offset + 0.18);
    }
}

/**
 * Countdown shown above the tab bar after every logged set.
 * It counts toward a fixed end time (not "seconds left"), so it stays correct even if the phone throttles timers.
 */
export function createRestTimer() {
    let duration = savedDuration();
    let endsAt = 0;
    let interval = null;

    const time = h('strong', { class: 'rest-time', 'aria-live': 'off' });
    const label = h('span', { class: 'muted small' }, t('rest.label'));
    const bar = h('div', { class: 'rest-bar', role: 'timer', hidden: true });

    const adjust = (delta) => {
        duration = Math.min(MAX, Math.max(MIN, duration + delta));
        saveDuration(duration);
        endsAt = Math.max(Date.now() + 1000, endsAt + delta * 1000);
        render();
    };

    const stop = () => {
        clearInterval(interval);
        interval = null;
        bar.hidden = true;
        bar.classList.remove('rest-done');
    };

    const render = () => {
        if (!bar.isConnected && interval) {
            stop();
            return;
        }
        const left = Math.max(0, Math.ceil((endsAt - Date.now()) / 1000));
        time.textContent = `${Math.floor(left / 60)}:${String(left % 60).padStart(2, '0')}`;
        bar.style.setProperty('--progress', String(Math.min(1, left / duration)));
        if (left === 0 && interval) finish();
    };

    const finish = () => {
        clearInterval(interval);
        interval = null;
        bar.classList.add('rest-done');
        label.textContent = t('rest.go');
        navigator.vibrate?.([200, 100, 200]);
        beep();
        toast(t('rest.done'));
        setTimeout(() => { if (!interval) stop(); }, 4000);
    };

    bar.append(
        h('div', { class: 'rest-info' }, icon('timer', { size: 20 }), h('div', { class: 'stack', style: 'gap: 0' }, label, time)),
        h('div', { class: 'rest-actions' },
            h('button', { type: 'button', class: 'icon-btn', 'aria-label': t('rest.less'), onclick: () => adjust(-15) }, '−15'),
            h('button', { type: 'button', class: 'icon-btn', 'aria-label': t('rest.more'), onclick: () => adjust(15) }, '+15'),
            h('button', { type: 'button', class: 'icon-btn', 'aria-label': t('rest.skip'), onclick: stop }, icon('close', { size: 18 }))));

    return {
        el: bar,
        start() {
            try {
                audio ??= new (window.AudioContext || window.webkitAudioContext)();
                if (audio.state === 'suspended') audio.resume();
            } catch { /* no sound available */ }
            label.textContent = t('rest.label');
            bar.classList.remove('rest-done');
            endsAt = Date.now() + duration * 1000;
            bar.hidden = false;
            clearInterval(interval);
            interval = setInterval(render, 250);
            render();
        },
        stop,
    };
}
