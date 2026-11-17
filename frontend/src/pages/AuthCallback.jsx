import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

/**
 * Landing page after Google sign-in. The backend has already put the refresh token in the HttpOnly
 * cookie; nothing sensitive is in this URL. We just exchange the cookie for an access token.
 */
export default function AuthCallback() {
  const { refresh } = useAuth();
  const navigate = useNavigate();
  const done = useRef(false);

  useEffect(() => {
    if (done.current) return;
    done.current = true;
    refresh().then((user) => navigate(user ? '/' : '/login?error=OAUTH_FAILED', { replace: true }));
  }, [refresh, navigate]);

  return <p className="page-status">Đang hoàn tất đăng nhập…</p>;
}
