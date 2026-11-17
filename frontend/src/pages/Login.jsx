import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/endpoints.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { Notice, TextInput } from '../components/Form.jsx';
import GoogleButton from '../components/GoogleButton.jsx';

const OAUTH_ERRORS = {
  OAUTH_FAILED: 'Đăng nhập Google không thành công. Hãy thử lại.',
  GOOGLE_EMAIL_UNVERIFIED: 'Email Google của bạn chưa được xác minh.',
  FORBIDDEN: 'Tài khoản đã bị vô hiệu hoá.',
};

/** Only same-app paths are accepted as "return to" targets (no open redirect). */
export const safeReturnPath = (p) => (typeof p === 'string' && p.startsWith('/') && !p.startsWith('//') ? p : '/');

export function useProviders() {
  const [providers, setProviders] = useState({ google: false, passwordRules: [] });
  useEffect(() => {
    authApi.providers().then(setProviders).catch(() => {});
  }, []);
  return providers;
}

export default function Login() {
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const providers = useProviders();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(OAUTH_ERRORS[params.get('error')] || null);
  const [busy, setBusy] = useState(false);
  const from = safeReturnPath(location.state?.from);

  useEffect(() => {
    if (user) navigate(from, { replace: true });
  }, [user, from, navigate]);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(form.email.trim(), form.password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message);
      setForm((f) => ({ ...f, password: '' }));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="auth">
      <h1 className="page-title">Đăng nhập</h1>
      {providers.google && (
        <>
          <GoogleButton />
          <p className="divider">
            <span>hoặc dùng email</span>
          </p>
        </>
      )}
      <form onSubmit={submit} noValidate>
        <Notice>{error}</Notice>
        <TextInput
          label="Email"
          type="email"
          autoComplete="username"
          required
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
        />
        <TextInput
          label="Mật khẩu"
          type="password"
          autoComplete="current-password"
          required
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        <button className="btn btn-block" disabled={busy || !form.email || !form.password}>
          {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </button>
      </form>
      <p className="muted">
        Chưa có tài khoản? <Link to="/register" state={location.state}>Tạo tài khoản</Link>
      </p>
    </section>
  );
}
