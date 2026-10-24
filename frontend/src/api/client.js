import axios from 'axios';
import { tokenStore } from './tokenStore';

const SAFE_METHODS = ['get', 'head', 'options'];

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

export function readCookie(name) {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.split('; ').find((row) => row.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.split('=')[1]) : null;
}

/** Makes sure the XSRF-TOKEN cookie exists before a cookie-authenticated call. */
export async function ensureCsrfCookie() {
  if (!readCookie('XSRF-TOKEN')) {
    await api.get('/auth/csrf', { skipAuthRefresh: true });
  }
}

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const method = (config.method || 'get').toLowerCase();
  if (!SAFE_METHODS.includes(method)) {
    // Double-submit CSRF token. The server only checks it on /auth/refresh and /auth/logout.
    const xsrf = readCookie('XSRF-TOKEN');
    if (xsrf) config.headers['X-XSRF-TOKEN'] = xsrf;
  }
  return config;
});

let refreshPromise = null;
let sessionExpiredHandler = () => {};

export function onSessionExpired(handler) {
  sessionExpiredHandler = handler;
}

/**
 * Exchanges the refresh cookie for a new access token. Concurrent callers share one
 * request, so several 401s at once (or React StrictMode's double effects) trigger a
 * single refresh rather than racing the token rotation.
 */
export function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      await ensureCsrfCookie();
      const { data } = await api.post('/auth/refresh', null, { skipAuthRefresh: true });
      tokenStore.set(data.accessToken);
      return data;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    if (status === 401 && original && !original._retried && !original.skipAuthRefresh && tokenStore.get()) {
      original._retried = true;
      try {
        await refreshSession();
        original.headers.Authorization = `Bearer ${tokenStore.get()}`;
        return api(original);
      } catch {
        tokenStore.clear();
        sessionExpiredHandler();
      }
    }
    return Promise.reject(error);
  },
);
