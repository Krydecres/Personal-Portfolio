import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { applyTokenResponse, clearSession, onSessionChange, refreshSession } from '../api/client.js';
import { authApi } from '../api/endpoints.js';

const AuthContext = createContext(null);

/** Tabs tell each other about sign-out so a stale tab does not keep showing private pages. */
const channel = typeof BroadcastChannel !== 'undefined' ? new BroadcastChannel('portfolio-auth') : null;

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const off = onSessionChange(setUser);
    // Restore the session from the HttpOnly refresh cookie (if any) on page load.
    refreshSession().finally(() => setReady(true));
    const onMessage = (e) => {
      if (e.data === 'logout') clearSession();
      if (e.data === 'login') refreshSession();
    };
    channel?.addEventListener('message', onMessage);
    return () => {
      off();
      channel?.removeEventListener('message', onMessage);
    };
  }, []);

  const login = useCallback(async (email, password) => {
    const user = applyTokenResponse(await authApi.login(email, password));
    channel?.postMessage('login');
    return user;
  }, []);

  const register = useCallback(async (body) => {
    const user = applyTokenResponse(await authApi.register(body));
    channel?.postMessage('login');
    return user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      clearSession();
      channel?.postMessage('logout');
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      ready,
      isAdmin: user?.role === 'ADMIN',
      login,
      register,
      logout,
      setUser,
      refresh: refreshSession,
    }),
    [user, ready, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
