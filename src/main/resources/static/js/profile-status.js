import { api, ApiError } from './api.js';
import { resetUnit } from './units.js';

/**
 * Remembers whether the logged-in user already has a profile, so we ask the server once per session.
 * New users must set up a profile before they can use the plan, workout and progress screens.
 */
let cached = null;

export async function hasProfile() {
    if (cached !== null) return cached;
    try {
        await api('/api/users/me/profile');
        cached = true;
    } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 404) throw error;
        cached = false;
    }
    return cached;
}

export function markProfileSaved() {
    cached = true;
}

export function resetProfileStatus() {
    cached = null;
    resetUnit();
}
