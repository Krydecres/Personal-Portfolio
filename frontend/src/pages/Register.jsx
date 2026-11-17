import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { Notice, TextInput } from '../components/Form.jsx';
import GoogleButton from '../components/GoogleButton.jsx';
import PasswordChecklist, { passwordOk } from '../components/PasswordChecklist.jsx';
import { safeReturnPath, useProviders } from './Login.jsx';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const providers = useProviders();
  const [form, setForm] = useState({ email: '', displayName: '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const mismatch = form.confirmPassword.length > 0 && form.confirmPassword !== form.password;
  const canSubmit =
    form.email && form.displayName.trim().length >= 2 && passwordOk(form.password) && !mismatch && form.confirmPassword;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setErrors({});
    try {
      await register({ ...form, email: form.email.trim(), displayName: form.displayName.trim() });
      navigate(safeReturnPath(location.state?.from), { replace: true });
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="auth">
      <h1 className="page-title">Tạo tài khoản</h1>
      {providers.google && (
        <>
          <GoogleButton label="Đăng ký với Google" />
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
          autoComplete="email"
          required
          maxLength={190}
          value={form.email}
          onChange={set('email')}
          error={errors.email}
        />
        <TextInput
          label="Tên hiển thị"
          autoComplete="name"
          required
          maxLength={80}
          value={form.displayName}
          onChange={set('displayName')}
          error={errors.displayName}
        />
        <TextInput
          label="Mật khẩu"
          type="password"
          autoComplete="new-password"
          required
          value={form.password}
          onChange={set('password')}
          error={errors.password}
        />
        <PasswordChecklist password={form.password} labels={providers.passwordRules} />
        <TextInput
          label="Nhập lại mật khẩu"
          type="password"
          autoComplete="new-password"
          required
          value={form.confirmPassword}
          onChange={set('confirmPassword')}
          error={mismatch ? 'Mật khẩu nhập lại không khớp' : errors.confirmPassword}
        />
        <button className="btn btn-block" disabled={busy || !canSubmit}>
          {busy ? 'Đang tạo tài khoản…' : 'Tạo tài khoản'}
        </button>
      </form>
      <p className="muted">
        Đã có tài khoản? <Link to="/login" state={location.state}>Đăng nhập</Link>
      </p>
    </section>
  );
}
