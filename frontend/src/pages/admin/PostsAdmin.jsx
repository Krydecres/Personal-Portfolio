import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { postApi } from '../../api/endpoints.js';
import { Notice } from '../../components/Form.jsx';
import { formatDate } from '../../lib/format.js';

export default function PostsAdmin() {
  const [data, setData] = useState(null);
  const [msg, setMsg] = useState(null);

  const load = () =>
    postApi
      .adminList({ size: 50 })
      .then(setData)
      .catch((e) => setMsg({ kind: 'error', text: e.message }));

  useEffect(() => {
    load();
  }, []);

  const remove = async (p) => {
    if (!window.confirm(`Xoá bài “${p.title}”? Bình luận của bài cũng bị xoá.`)) return;
    try {
      await postApi.remove(p.id);
      setMsg({ kind: 'success', text: 'Đã xoá bài viết.' });
      load();
    } catch (e) {
      setMsg({ kind: 'error', text: e.message });
    }
  };

  return (
    <div>
      <Notice kind={msg?.kind}>{msg?.text}</Notice>
      <Link className="btn" to="/admin/posts/new">
        Viết bài mới
      </Link>
      {!data && <p className="muted">Đang tải…</p>}
      {data?.content.length === 0 && <p className="muted">Chưa có bài viết. Bắt đầu với bài đầu tiên.</p>}
      <ul className="admin-list">
        {data?.content.map((p) => (
          <li key={p.id}>
            <div>
              <strong>{p.title}</strong>
              <span className="muted small">
                <span className={`badge badge-${p.status.toLowerCase()}`}>
                  {p.status === 'PUBLISHED' ? 'Đã đăng' : 'Bản nháp'}
                </span>{' '}
                Sửa lần cuối {formatDate(p.updatedAt)}
              </span>
            </div>
            <div className="row-actions">
              {p.status === 'PUBLISHED' && (
                <Link className="link-button" to={`/posts/${p.slug}`}>
                  Xem
                </Link>
              )}
              <Link className="link-button" to={`/admin/posts/${p.id}`}>
                Sửa
              </Link>
              <button type="button" className="link-button danger" onClick={() => remove(p)}>
                Xoá
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
