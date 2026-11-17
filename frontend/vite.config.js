import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * In development every /api call goes through the Vite proxy, so the browser sees one origin
 * (http://localhost:5173) and the refresh cookie stays first-party.
 *
 * `vite preview` serves the production build with the same security headers you should set on the
 * real web server (nginx, Caddy...). The CSP is not applied to `vite dev` because the React
 * fast-refresh runtime needs an inline script.
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backend = env.VITE_BACKEND_ORIGIN || 'http://localhost:8080';

  const commonHeaders = {
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'strict-origin-when-cross-origin',
    'X-Frame-Options': 'DENY',
    'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()',
  };

  const csp = [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com",
    "img-src 'self' https: data:",
    "connect-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
  ].join('; ');

  const proxy = { '/api': { target: backend, changeOrigin: true } };

  return {
    plugins: [react()],
    server: { port: 5173, strictPort: true, headers: commonHeaders, proxy },
    preview: {
      port: 5173,
      strictPort: true,
      headers: { ...commonHeaders, 'Content-Security-Policy': csp },
      proxy,
    },
    build: { sourcemap: false },
  };
});
