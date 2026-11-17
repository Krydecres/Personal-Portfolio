/**
 * Thin fetch wrapper.
 *
 * Security model
 * - The access token (JWT, 15 min) lives only in memory: never in localStorage/sessionStorage, so an
 *   XSS payload cannot simply read it from storage, and it disappears when the tab closes.
 * - The refresh token lives in an HttpOnly cookie that JavaScript cannot see. It is only sent to
 *   /api/auth/* and only on same-site requests (SameSite=Strict).
 * - On a 401 we refresh once (single-flight, so parallel requests share one refresh) and retry.
 */

const AUTH_PREFIX = '/api/auth/';

let accessToken = null;
let refreshTimer = null;
let refreshInFlight = null;
const listeners = new Set();

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Lỗi ${status}`);
    this.status = status;
    this.code = body?.code;
    this.fieldErrors = body?.fieldErrors || {};
  }
}

/** Subscribe to session changes: fn(user | null). */
export function onSessionChange(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

function emit(user) {
  listeners.forEach((fn) => fn(user));
}

/** Store a TokenResponse from login/register/refresh and schedule a silent refresh before expiry. */
export function applyTokenResponse(data) {
  accessToken = data.accessToken;
  clearTimeout(refreshTimer);
  const ms = Math.max(10, data.expiresIn - 60) * 1000;
  refreshTimer = setTimeout(() => {
    refreshSession().catch(() => {});
  }, ms);
  emit(data.user);
  return data.user;
}

export function clearSession() {
  accessToken = null;
  clearTimeout(refreshTimer);
  emit(null);
}

async function rawFetch(path, { method = 'GET', body, signal } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (accessToken && !path.startsWith(AUTH_PREFIX)) headers.Authorization = `Bearer ${accessToken}`;
  return fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: 'same-origin',
    signal,
  });
}

async function parse(res) {
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}

/**
 * Exchange the refresh cookie for a new access token. Resolves to the user, or null when there is
 * no valid session. Concurrent callers share one request.
 */
export function refreshSession() {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      for (let attempt = 0; attempt < 2; attempt++) {
        const res = await rawFetch(`${AUTH_PREFIX}refresh`, { method: 'POST' });
        if (res.ok) return applyTokenResponse(await res.json());
        // Another tab may have rotated the cookie a moment ago: the browser now holds the new one.
        if (res.status === 401 && attempt === 0) {
          await new Promise((r) => setTimeout(r, 400));
          continue;
        }
        break;
      }
      clearSession();
      return null;
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

export async function api(path, options = {}) {
  let res = await rawFetch(path, options);
  if (res.status === 401 && accessToken && !path.startsWith(AUTH_PREFIX)) {
    const user = await refreshSession();
    if (user) res = await rawFetch(path, options);
  }
  return parse(res);
}

export const http = {
  get: (path, opts) => api(path, { ...opts, method: 'GET' }),
  post: (path, body, opts) => api(path, { ...opts, method: 'POST', body }),
  put: (path, body, opts) => api(path, { ...opts, method: 'PUT', body }),
  del: (path, opts) => api(path, { ...opts, method: 'DELETE' }),
};

/**
 * Backend origin for full-page navigations (Google sign-in cannot go through fetch).
 * Dev: the API runs on another port. Production build: Spring Boot serves this app, so same origin ('').
 */
export const BACKEND_ORIGIN =
  import.meta.env.VITE_BACKEND_ORIGIN ?? (import.meta.env.DEV ? 'http://localhost:8080' : '');
