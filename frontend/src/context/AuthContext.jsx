import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { onSessionExpired, refreshSession } from '../api/client';
import { authApi } from '../api/services';
import { tokenStore } from '../api/tokenStore';

const AuthContext = createContext(null);

/**
 * status: 'loading' while restoring a session on page load, then 'authenticated' or 'anonymous'.
 * On load we try POST /auth/refresh: if the HttpOnly refresh cookie is still valid, the
 * user stays signed in across reloads without the access token ever touching storage.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let active = true;
    refreshSession()
      .then((session) => {
        if (!active) return;
        setUser(session.user);
        setStatus('authenticated');
      })
      .catch(() => {
        if (!active) return;
        tokenStore.clear();
        setStatus('anonymous');
      });

    onSessionExpired(() => {
      setUser(null);
      setStatus('anonymous');
      toast.error('Your session ended. Sign in again.', { id: 'session-expired' });
    });
    return () => {
      active = false;
    };
  }, []);

  const startSession = useCallback((session) => {
    tokenStore.set(session.accessToken);
    setUser(session.user);
    setStatus('authenticated');
    return session.user;
  }, []);

  const login = useCallback(async (email, password) => startSession(await authApi.login({ email, password })), [startSession]);

  const register = useCallback(async (payload) => startSession(await authApi.register(payload)), [startSession]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // Even if the server can't be reached, forget the session locally.
    }
    tokenStore.clear();
    setUser(null);
    setStatus('anonymous');
  }, []);

  /** Clears local state after the account has been deleted (the server already cleared the cookie). */
  const forgetSession = useCallback(() => {
    tokenStore.clear();
    setUser(null);
    setStatus('anonymous');
  }, []);

  const value = useMemo(
    () => ({
      user,
      status,
      isAuthenticated: status === 'authenticated',
      isAdmin: user?.role === 'ADMIN',
      login,
      register,
      logout,
      startSession,
      forgetSession,
      setUser,
    }),
    [user, status, login, register, logout, startSession, forgetSession],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
