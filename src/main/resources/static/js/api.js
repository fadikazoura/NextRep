import { t } from './i18n.js';
import { hideToast, toast } from './dom.js';

const TOKEN_KEY = 'nextrep.token';
/** Free hosting sleeps when idle; if a request is slow, tell the user why. */
const WAKE_UP_HINT_AFTER_MS = 4000;

/**
 * The token lives in localStorage so a refresh keeps you logged in.
 * Trade-off: any script on the page could read it, which is why the UI never inserts HTML from data (see dom.js).
 */
export const session = {
    get token() {
        try {
            return localStorage.getItem(TOKEN_KEY);
        } catch {
            return null;
        }
    },
    save(token) {
        try {
            localStorage.setItem(TOKEN_KEY, token);
        } catch { /* ignore */ }
    },
    clear() {
        try {
            localStorage.removeItem(TOKEN_KEY);
        } catch { /* ignore */ }
    },
    get loggedIn() {
        return Boolean(this.token);
    },
};

/** An error answered by the API (RFC 9457 ProblemDetail), or status 0 for network failures. */
export class ApiError extends Error {
    constructor(status, problem) {
        super(problem?.detail || problem?.title || `HTTP ${status}`);
        this.status = status;
        this.title = problem?.title;
        this.fieldErrors = problem?.errors ?? {};
    }
}

/** Sends a request with the JWT, shows the "server waking up" hint, and handles an expired session. */
async function send(path, { method = 'GET', headers = {}, body, authenticated = true }) {
    const allHeaders = { ...headers };
    if (authenticated && session.token) allHeaders.Authorization = `Bearer ${session.token}`;

    const wakeUpHint = setTimeout(() => toast(t('errors.wakingUp'), 0), WAKE_UP_HINT_AFTER_MS);
    let response;
    try {
        response = await fetch(path, { method, headers: allHeaders, body });
    } catch {
        throw new ApiError(0, { title: t('errors.network') });
    } finally {
        clearTimeout(wakeUpHint);
        hideToast();
    }

    if (response.status === 401 && authenticated) {
        session.clear();
        window.dispatchEvent(new Event('session-expired'));
    }
    return response;
}

async function readJson(response) {
    if (response.status === 204) return null;
    const isJson = (response.headers.get('content-type') || '').includes('json');
    const data = isJson ? await response.json() : null;
    if (!response.ok) throw new ApiError(response.status, data);
    return data;
}

/**
 * Calls the NextRep API. Adds the JWT, sends/receives JSON, and turns error responses into ApiError.
 * A 401 on an authenticated call means the token expired: the session is cleared and the app returns to login.
 */
export async function api(path, { method = 'GET', body, authenticated = true } = {}) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const response = await send(path, {
        method, headers, authenticated, body: body === undefined ? undefined : JSON.stringify(body),
    });
    return readJson(response);
}

/** Uploads a file as multipart/form-data (the browser sets the Content-Type with its boundary). */
export async function apiUpload(path, file, fieldName = 'file') {
    const form = new FormData();
    form.append(fieldName, file);
    return readJson(await send(path, { method: 'POST', headers: { Accept: 'application/json' }, body: form }));
}

/** Downloads binary data (a picture, an export); null when it does not exist. */
export async function apiBlob(path) {
    const response = await send(path, {});
    if (response.status === 404) return null;
    if (!response.ok) throw new ApiError(response.status, null);
    return response.blob();
}
