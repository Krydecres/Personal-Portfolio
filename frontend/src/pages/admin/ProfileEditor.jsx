import { useEffect, useState } from 'react';
import { profileApi } from '../../api/endpoints.js';
import { Notice, TextArea, TextInput } from '../../components/Form.jsx';
import Markdown from '../../components/Markdown.jsx';
import { splitList } from '../../lib/format.js';
import { setProfile, useProfile } from '../../lib/useProfile.js';

const FIELDS = [
  ['fullName', 'Họ và tên', { required: true, maxLength: 120 }],
  ['headline', 'Câu giới thiệu ngắn', { maxLength: 200, placeholder: 'Backend developer yêu thích hệ thống phân tán' }],
  ['availability', 'Trạng thái', { maxLength: 160, placeholder: 'Đang tìm cơ hội thực tập' }],
  ['location', 'Nơi sống', { maxLength: 120 }],
  ['contactEmail', 'Email liên hệ', { type: 'email', maxLength: 190 }],
  ['avatarUrl', 'Ảnh chân dung (URL https)', { type: 'url' }],
  ['githubUrl', 'GitHub', { type: 'url', placeholder: 'https://github.com/…' }],
  ['linkedinUrl', 'LinkedIn', { type: 'url' }],
  ['websiteUrl', 'Website', { type: 'url' }],
  ['resumeUrl', 'Link CV', { type: 'url' }],
];

export default function ProfileEditor() {
  const { profile } = useProfile();
  const [form, setForm] = useState(null);
  const [errors, setErrors] = useState({});
  const [msg, setMsg] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (profile && !form) {
      const f = {};
      FIELDS.forEach(([k]) => (f[k] = profile[k] || ''));
      f.bio = profile.bio || '';
      f.skills = profile.skills.join(', ');
      setForm(f);
    }
  }, [profile, form]);

  if (!form) return <p className="muted">Đang tải…</p>;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setMsg(null);
    setErrors({});
    try {
      const saved = await profileApi.update({ ...form, skills: splitList(form.skills) });
      setProfile(saved);
      setMsg({ kind: 'success', text: 'Đã lưu hồ sơ.' });
    } catch (err) {
      setErrors(err.fieldErrors);
      setMsg({ kind: 'error', text: err.message });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="editor" onSubmit={submit}>
      <Notice kind={msg?.kind}>{msg?.text}</Notice>
      <div className="grid-2">
        {FIELDS.map(([key, label, props]) => (
          <TextInput
            key={key}
            label={label}
            {...props}
            value={form[key]}
            onChange={(e) => setForm({ ...form, [key]: e.target.value })}
            error={errors[key]}
          />
        ))}
      </div>
      <TextInput
        label="Kỹ năng"
        hint="Phân tách bằng dấu phẩy, ví dụ: Java, Spring Boot, React"
        value={form.skills}
        onChange={(e) => setForm({ ...form, skills: e.target.value })}
        error={errors.skills}
      />
      <div className="split">
        <TextArea
          label="Giới thiệu (Markdown)"
          rows={12}
          maxLength={10000}
          value={form.bio}
          onChange={(e) => setForm({ ...form, bio: e.target.value })}
          error={errors.bio}
        />
        <div className="preview" aria-label="Xem trước">
          <p className="field-hint">Xem trước</p>
          <Markdown>{form.bio}</Markdown>
        </div>
      </div>
      <div className="form-actions">
        <button className="btn" disabled={busy}>
          {busy ? 'Đang lưu…' : 'Lưu hồ sơ'}
        </button>
      </div>
    </form>
  );
}
