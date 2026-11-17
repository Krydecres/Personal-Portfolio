import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { postApi } from '../../api/endpoints.js';
import { Notice, TextArea, TextInput } from '../../components/Form.jsx';
import Markdown from '../../components/Markdown.jsx';
import { splitList } from '../../lib/format.js';

const EMPTY = { title: '', slug: '', summary: '', content: '', coverImageUrl: '', tags: '', status: 'DRAFT' };

export default function PostEditor() {
  const { id } = useParams();
  const isNew = id === undefined;
  const navigate = useNavigate();
  const [form, setForm] = useState(isNew ? EMPTY : null);
  const [tab, setTab] = useState('write');
  const [errors, setErrors] = useState({});
  const [msg, setMsg] = useState(null);
  const [busy, setBusy] = useState(false);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    if (isNew) return;
    postApi
      .adminGet(id)
      .then((p) =>
        setForm({
          title: p.title,
          slug: p.slug,
          summary: p.summary || '',
          content: p.content,
          coverImageUrl: p.coverImageUrl || '',
          tags: p.tags.join(', '),
          status: p.status,
        }),
      )
      .catch((e) => setMsg({ kind: 'error', text: e.message }));
  }, [id, isNew]);

  useEffect(() => {
    if (!dirty) return undefined;
    const warn = (e) => {
      e.preventDefault();
      e.returnValue = '';
    };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [dirty]);

  if (!form) return msg ? <Notice kind="error">{msg.text}</Notice> : <p className="muted">Đang tải…</p>;

  const bind = (k) => ({
    value: form[k],
    onChange: (e) => {
      setForm({ ...form, [k]: e.target.value });
      setDirty(true);
    },
    error: errors[k],
  });

  const save = async (status) => {
    setBusy(true);
    setErrors({});
    setMsg(null);
    try {
      const body = { ...form, status, tags: splitList(form.tags), slug: form.slug.trim() };
      const saved = isNew ? await postApi.create(body) : await postApi.update(id, body);
      setDirty(false);
      setForm({ ...form, slug: saved.slug, status: saved.status });
      setMsg({ kind: 'success', text: status === 'PUBLISHED' ? 'Đã đăng bài.' : 'Đã lưu bản nháp.' });
      if (isNew) navigate(`/admin/posts/${saved.id}`, { replace: true });
    } catch (e) {
      setErrors(e.fieldErrors);
      setMsg({ kind: 'error', text: e.message });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="editor" onSubmit={(e) => e.preventDefault()}>
      <p>
        <Link to="/admin/posts">Danh sách bài viết</Link>
      </p>
      <Notice kind={msg?.kind}>{msg?.text}</Notice>
      <TextInput label="Tiêu đề" required maxLength={200} {...bind('title')} />
      <div className="grid-2">
        <TextInput
          label="Đường dẫn (slug)"
          hint="Để trống để tạo tự động từ tiêu đề"
          maxLength={160}
          pattern="[a-z0-9]+(-[a-z0-9]+)*"
          {...bind('slug')}
        />
        <TextInput label="Thẻ" hint="Phân tách bằng dấu phẩy" {...bind('tags')} />
        <TextInput label="Ảnh bìa (URL https)" type="url" {...bind('coverImageUrl')} />
        <TextInput label="Tóm tắt" maxLength={500} {...bind('summary')} />
      </div>

      <div className="tabs" role="tablist" aria-label="Nội dung">
        <button type="button" role="tab" aria-selected={tab === 'write'} onClick={() => setTab('write')}>
          Viết
        </button>
        <button type="button" role="tab" aria-selected={tab === 'preview'} onClick={() => setTab('preview')}>
          Xem trước
        </button>
      </div>
      {tab === 'write' ? (
        <TextArea
          label="Nội dung (Markdown)"
          rows={20}
          className="mono"
          maxLength={200000}
          {...bind('content')}
        />
      ) : (
        <div className="preview preview-article">
          <Markdown className="prose prose-article">{form.content || '_Chưa có nội dung._'}</Markdown>
        </div>
      )}

      <div className="form-actions sticky-actions">
        <span className="muted small">
          Trạng thái: {form.status === 'PUBLISHED' ? 'Đã đăng' : 'Bản nháp'}
          {dirty && ' (có thay đổi chưa lưu)'}
        </span>
        <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => save('DRAFT')}>
          {form.status === 'PUBLISHED' ? 'Chuyển về nháp' : 'Lưu nháp'}
        </button>
        <button type="button" className="btn" disabled={busy || !form.title || !form.content} onClick={() => save('PUBLISHED')}>
          {form.status === 'PUBLISHED' ? 'Cập nhật bài' : 'Đăng bài'}
        </button>
      </div>
    </form>
  );
}
