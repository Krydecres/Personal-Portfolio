import { useState } from 'react';
import { applyTokenResponse } from '../api/client.js';
import { accountApi } from '../api/endpoints.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { Notice, TextInput } from '../components/Form.jsx';
import PasswordChecklist, { passwordOk } from '../components/PasswordChecklist.jsx';

function ProfileForm() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ displayName: user.displayName, avatarUrl: user.avatarUrl || '' });
  const [msg, setMsg] = useState(null);
  const [errors, setErrors] = useState({});

  const submit = async (e) => {
    e.preventDefault();
    setMsg(null);
    setErrors({});
    try {
      setUser(await accountApi.update(form));
      setMsg({ kind: 'success', text: 'Đã lưu thông tin tài khoản.' });
    } catch (err) {
      setErrors(err.fieldErrors);
      setMsg({ kind: 'error', text: err.message });
    }
  };

  return (
    <form className="card-form" onSubmit={submit}>
      <h2>Thông tin</h2>
      <p className="muted">Email: {user.email}</p>
      <Notice kind={msg?.kind}>{msg?.text}</Notice>
      <TextInput
        label="Tên hiển thị"
        required
        maxLength={80}
        value={form.displayName}
        onChange={(e) => setForm({ ...form, displayName: e.target.value })}
        error={errors.displayName}
      />
      <TextInput
        label="Ảnh đại diện (URL https)"
        type="url"
        value={form.avatarUrl}
        onChange={(e) => setForm({ ...form, avatarUrl: e.target.value })}
        error={errors.avatarUrl}
      />
      <button className="btn">Lưu thông tin</button>
    </form>
  );
}

function PasswordForm() {
  const { user } = useAuth();
  const empty = { currentPassword: '', newPassword: '', confirmPassword: '' };
  const [form, setForm] = useState(empty);
  const [msg, setMsg] = useState(null);
  const [errors, setErrors] = useState({});
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const mismatch = form.confirmPassword && form.confirmPassword !== form.newPassword;

  const submit = async (e) => {
    e.preventDefault();
    setMsg(null);
    setErrors({});
    try {
      applyTokenResponse(await accountApi.changePassword(form));
      setForm(empty);
      setMsg({ kind: 'success', text: 'Đã đổi mật khẩu. Các thiết bị khác đã bị đăng xuất.' });
    } catch (err) {
      setErrors(err.fieldErrors);
      setMsg({ kind: 'error', text: err.message });
    }
  };

  return (
    <form className="card-form" onSubmit={submit}>
      <h2>{user.hasPassword ? 'Đổi mật khẩu' : 'Đặt mật khẩu'}</h2>
      {!user.hasPassword && (
        <p className="muted">Bạn đang đăng nhập bằng Google. Đặt mật khẩu để có thể đăng nhập bằng email.</p>
      )}
      <Notice kind={msg?.kind}>{msg?.text}</Notice>
      {user.hasPassword && (
        <TextInput
          label="Mật khẩu hiện tại"
          type="password"
          autoComplete="current-password"
          value={form.currentPassword}
          onChange={set('currentPassword')}
        />
      )}
      <TextInput
        label="Mật khẩu mới"
        type="password"
        autoComplete="new-password"
        value={form.newPassword}
        onChange={set('newPassword')}
        error={errors.newPassword}
      />
      <PasswordChecklist password={form.newPassword} />
      <TextInput
        label="Nhập lại mật khẩu mới"
        type="password"
        autoComplete="new-password"
        value={form.confirmPassword}
        onChange={set('confirmPassword')}
        error={mismatch ? 'Mật khẩu nhập lại không khớp' : null}
      />
      <button
        className="btn"
        disabled={!passwordOk(form.newPassword) || mismatch || !form.confirmPassword || (user.hasPassword && !form.currentPassword)}
      >
        {user.hasPassword ? 'Đổi mật khẩu' : 'Đặt mật khẩu'}
      </button>
    </form>
  );
}

export default function Account() {
  return (
    <section className="page-narrow">
      <h1 className="page-title">Tài khoản của tôi</h1>
      <ProfileForm />
      <PasswordForm />
    </section>
  );
}
