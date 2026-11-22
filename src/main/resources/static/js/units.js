import { api, ApiError } from './api.js';
import { t } from './i18n.js';

/**
 * The server always stores kilograms; the user's profile says how weights should be shown (KG or LB).
 * Every weight on screen goes through here, and every weight typed in is converted back to kg before it is sent.
 */
const LB_PER_KG = 2.20462;
let unit = null; // null = not loaded yet this session

export async function loadUnit() {
    if (unit) return unit;
    try {
        setUnit((await api('/api/users/me/profile')).weightUnit);
    } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 404) return 'KG'; // don't cache a failure
        unit = 'KG';
    }
    return unit;
}

export function setUnit(value) {
    unit = value === 'LB' ? 'LB' : 'KG';
}

export function resetUnit() {
    unit = null;
}

export const isLb = () => unit === 'LB';

/** "kg" / "lb" (or the Hebrew word), ready to put after a number. */
export const unitLabel = () => t(`enum.${isLb() ? 'LB' : 'KG'}`);

/** Kilograms from the server → number in the user's unit (kg keeps 2 decimals, lb rounds to 0.1). */
export function toDisplay(kg) {
    const n = Number(kg);
    return isLb() ? Math.round(n * LB_PER_KG * 10) / 10 : Math.round(n * 100) / 100;
}

/** A weight as text without the unit, e.g. "102.5". */
export const formatWeight = (kg) => String(toDisplay(kg));

/** A weight as text with the unit, e.g. "225 lb". */
export const weightText = (kg) => `${formatWeight(kg)} ${unitLabel()}`;

/** Big totals (volume): whole numbers with thousands separators. */
export const volumeText = (kg) => `${Math.round(toDisplay(kg)).toLocaleString('en-US')} ${unitLabel()}`;

/** What the user typed (in their unit) → kilograms for the API. */
export function fromInput(value) {
    const n = Number(value);
    return isLb() ? Math.round((n / LB_PER_KG) * 100) / 100 : n;
}

/** Prefill value for a number input; '' when there is nothing to show. */
export const inputValue = (kg) => (kg == null || kg === '' ? '' : formatWeight(kg));

/** Input step that matches real plates: 0.25 kg or 0.5 lb. */
export const weightStep = () => (isLb() ? 0.5 : 0.25);
