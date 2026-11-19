import { api, apiBlob } from './api.js';

/**
 * The logged-in user (username, email, avatar), loaded once and shared by the header and the settings page.
 * Changes are announced with a "userchange" event so every place that shows the avatar updates together.
 */
let user = null;
let loading = null;
let photo = { url: null, forUserId: null };

export async function currentUser({ refresh = false } = {}) {
    if (user && !refresh) return user;
    loading ??= api('/api/users/me').finally(() => { loading = null; });
    setCurrentUser(await loading);
    return user;
}

export function cachedUser() {
    return user;
}

export function setCurrentUser(next, { photoChanged = false } = {}) {
    if (photoChanged || next?.avatar?.kind !== 'PHOTO' || next?.id !== photo.forUserId) dropPhoto();
    user = next;
    window.dispatchEvent(new Event('userchange'));
}

export function clearCurrentUser() {
    user = null;
    dropPhoto();
}

/** Object URL of the uploaded picture (fetched with the token, then cached in memory). */
export async function photoUrl() {
    if (!user || user.avatar?.kind !== 'PHOTO') return null;
    if (photo.url && photo.forUserId === user.id) return photo.url;
    const blob = await apiBlob('/api/users/me/avatar/photo');
    if (!blob) return null;
    dropPhoto();
    photo = { url: URL.createObjectURL(blob), forUserId: user.id };
    return photo.url;
}

function dropPhoto() {
    if (photo.url) URL.revokeObjectURL(photo.url);
    photo = { url: null, forUserId: null };
}
