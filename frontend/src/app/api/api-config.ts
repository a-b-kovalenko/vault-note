const LOCAL_API_BASE_URL = 'http://localhost:8080';
const LOCAL_HOSTNAMES = new Set(['localhost', '127.0.0.1', '::1', '[::1]']);

function resolveApiBaseUrl(): string {
  if (typeof window === 'undefined' || LOCAL_HOSTNAMES.has(window.location.hostname)) {
    return LOCAL_API_BASE_URL;
  }

  return window.location.origin;
}

export const API_BASE_URL = resolveApiBaseUrl();
