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

/**
 * Calls the NextRep API. Adds the JWT, sends/receives JSON, and turns error responses into ApiError.
 * A 401 on an authenticated call means the token expired: the session is cleared and the app returns to login.
 */
export async function api(path, { method = 'GET', body, authenticated = true } = {}) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (authenticated && session.token) headers.Authorization = `Bearer ${session.token}`;

    const wakeUpHint = setTimeout(() => toast(t('errors.wakingUp'), 0), WAKE_UP_HINT_AFTER_MS);
    let response;
    try {
        response = await fetch(path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        });
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
    if (response.status === 204) return null;

    const isJson = (response.headers.get('content-type') || '').includes('json');
    const data = isJson ? await response.json() : null;
    if (!response.ok) throw new ApiError(response.status, data);
    return data;
}
