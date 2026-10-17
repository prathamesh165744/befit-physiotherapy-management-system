// One place that configures axios for the whole app. It is imported once from main.jsx.
import axios from 'axios';

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Attach the login token to every request.
// The branchId parameter is kept for compatibility, but the SERVER decides which branch a receptionist may see
// (from their account); only an ADMIN's choice of branch is honoured.
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  const branchId = localStorage.getItem('branchId');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  if (branchId && config.method === 'get') {
    config.params = { branchId, ...config.params };
  }
  return config;
});

// ONLY a 401 (missing / expired / invalid token) sends the user back to login.
// 403, 404, 409, 5xx and network errors are shown by the page that made the request.
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || '';
    if (error.response?.status === 401 && !url.includes('/api/auth/')) {
      localStorage.clear();
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login?expired=1');
      }
    }
    return Promise.reject(error);
  }
);

/** A readable message for any axios error (also works when the error body is a Blob, e.g. a failed file download). */
export async function errorMessage(err, fallback = 'Something went wrong. Please try again.') {
  if (!err?.response) return 'Cannot reach the server. Please check your connection and try again.';
  let data = err.response.data;
  if (typeof Blob !== 'undefined' && data instanceof Blob) {
    try { data = JSON.parse(await data.text()); } catch { data = null; }
  }
  if (data && typeof data === 'object' && data.message) return data.message;
  if (typeof data === 'string' && data.trim() && data.length < 200) return data;
  switch (err.response.status) {
    case 403: return 'You do not have permission to do that.';
    case 404: return 'The requested item was not found.';
    default: return fallback;
  }
}

/** Sorts an error into a kind the UI can react to differently. */
export function classifyError(err) {
  if (!err?.response) return 'network';
  const s = err.response.status;
  if (s === 401) return 'unauthorized';
  if (s === 403) return 'forbidden';
  if (s === 404) return 'notfound';
  if (s >= 500) return 'server';
  return 'other';
}

/** An admin works in one chosen branch (or ALL = view only). Creating records needs a specific one. */
export function branchForCreate() {
  const b = localStorage.getItem('branchId');
  return localStorage.getItem('role') === 'ADMIN' && b && b !== 'ALL' ? b : undefined;
}

/** Save a Blob as a file in the browser. */
export function saveBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export function filenameFromDisposition(header, fallback) {
  if (!header) return fallback;
  const star = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (star) { try { return decodeURIComponent(star[1]); } catch { /* ignore */ } }
  const plain = /filename="?([^";]+)"?/i.exec(header);
  return plain ? plain[1] : fallback;
}

export default axios;
