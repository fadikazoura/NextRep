import { h } from './dom.js';
import { icon } from './icons.js';
import { t, getLang } from './i18n.js';
import { api } from './api.js';
import { currentUser } from './current-user.js';
import { celebrateAchievement } from './celebrate.js';

/** Icon and color tier of every badge the server knows (codes from the Achievement enum). */
export const BADGES = {
    FIRST_WORKOUT: { icon: 'rocket', tier: 'bronze' },
    WORKOUTS_10: { icon: 'medal', tier: 'bronze' },
    WORKOUTS_25: { icon: 'medal', tier: 'silver' },
    WORKOUTS_50: { icon: 'trophy', tier: 'gold' },
    WORKOUTS_100: { icon: 'crown', tier: 'gold' },
    PERFECT_WEEK: { icon: 'target', tier: 'silver' },
    STREAK_4: { icon: 'flame', tier: 'silver' },
    STREAK_12: { icon: 'flame', tier: 'gold' },
    SETS_500: { icon: 'dumbbell', tier: 'silver' },
    CLUB_100: { icon: 'barbell', tier: 'gold' },
    BIG_SESSION: { icon: 'bolt', tier: 'silver' },
};

/**
 * Streak, weekly totals and badges. The browser sends its time zone and the first day of the week,
 * so "this week" means the same thing on the server as on the screen (Sunday-first in Hebrew).
 */
export function loadInsights() {
    let tz = 'UTC';
    try {
        tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    } catch { /* very old browser: UTC */ }
    const firstDay = getLang() === 'he' ? 'SUNDAY' : 'MONDAY';
    return api(`/api/users/me/insights?tz=${encodeURIComponent(tz)}&firstDay=${firstDay}`);
}

/** A round badge icon, gold/silver/bronze when earned and grey while locked. */
export function badgeIcon(code, unlocked, size = 26) {
    const badge = BADGES[code] ?? { icon: 'medal', tier: 'bronze' };
    return h('span', { class: `badge-icon tier-${badge.tier} ${unlocked ? 'is-unlocked' : 'is-locked'}`, 'aria-hidden': 'true' },
        icon(badge.icon, { size }));
}

/**
 * Celebrates badges earned since the last visit and returns how many there were. The codes already seen are remembered per user in
 * localStorage; the very first time nothing is celebrated (otherwise an existing user, or the demo account,
 * would get a burst of every old badge at once).
 */
export async function celebrateNewAchievements(insights) {
    const user = await currentUser().catch(() => null);
    if (!user) return 0;
    const key = `nextrep.badgesSeen.${user.id}`;
    const unlocked = insights.achievements.filter((a) => a.unlocked).map((a) => a.code);
    let seen = null;
    try {
        seen = JSON.parse(localStorage.getItem(key) ?? 'null');
    } catch { /* ignore */ }
    try {
        localStorage.setItem(key, JSON.stringify(unlocked));
    } catch { /* ignore */ }
    if (!Array.isArray(seen)) return 0;

    const fresh = unlocked.filter((code) => !seen.includes(code));
    fresh.slice(0, 3).forEach((code, i) => setTimeout(() => celebrateAchievement(
        badgeIcon(code, true, 44), t(`badge.${code}`), t(`badge.${code}.desc`)), 600 + i * 3200));
    return fresh.length;
}
