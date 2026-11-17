import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { postApi } from '../api/endpoints.js';
import { useAuth } from '../auth/AuthContext.jsx';
import Avatar from '../components/Avatar.jsx';
import { Notice } from '../components/Form.jsx';
import Markdown from '../components/Markdown.jsx';
import { formatDate, safeUrl } from '../lib/format.js';

function Comments({ slug }) {
  const { user } = useAuth();
  const location = useLocation();
  const [comments, setComments] = useState(null);
  const [text, setText] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    postApi.comments(slug).then(setComments).catch(() => setComments([]));
  }, [slug, user?.id]);

  const submit = async (e) => {
    e.preventDefault();
    if (!text.trim()) return;
    setBusy(true);
    setError(null);
    try {
      const created = await postApi.addComment(slug, text.trim());
      setComments((list) => [...(list || []), created]);
      setText('');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id) => {
    if (!window.confirm('Xoá bình luận này?')) return;
    try {
      await postApi.deleteComment(id);
      setComments((list) => list.filter((c) => c.id !== id));
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <section className="comments" aria-labelledby="comments-title">
      <h2 id="comments-title">Bình luận{comments?.length ? ` (${comments.length})` : ''}</h2>
      {comments?.length === 0 && <p className="muted">Chưa có bình luận. Hãy là người đầu tiên.</p>}
      <ul className="comment-list">
        {comments?.map((c) => (
          <li key={c.id}>
            <Avatar name={c.author.displayName} url={c.author.avatarUrl} size={34} />
            <div>
              <p className="comment-head">
                <strong>{c.author.displayName}</strong>
                <time className="muted small" dateTime={c.createdAt}>
                  {formatDate(c.createdAt)}
                </time>
                {c.canDelete && (
                  <button type="button" className="link-button small" onClick={() => remove(c.id)}>
                    Xoá
                  </button>
                )}
              </p>
              {/* rendered as plain text: React escapes it, so HTML in comments is never executed */}
              <p className="comment-text">{c.content}</p>
            </div>
          </li>
        ))}
      </ul>
      <Notice>{error}</Notice>
      {user ? (
        <form className="comment-form" onSubmit={submit}>
          <label htmlFor="comment" className="sr-only">
            Viết bình luận
          </label>
          <textarea
            id="comment"
            rows={3}
            maxLength={2000}
            placeholder="Viết bình luận…"
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
          <button className="btn" disabled={busy || !text.trim()}>
            {busy ? 'Đang gửi…' : 'Gửi bình luận'}
          </button>
        </form>
      ) : (
        <p className="muted">
          <Link to="/login" state={{ from: location.pathname }}>
            Đăng nhập
          </Link>{' '}
          để bình luận.
        </p>
      )}
    </section>
  );
}

export default function PostDetail() {
  const { slug } = useParams();
  const { isAdmin } = useAuth();
  const [post, setPost] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    setPost(null);
    setError(null);
    postApi
      .get(slug)
      .then((p) => {
        setPost(p);
        document.title = p.title;
      })
      .catch((e) => setError(e.status === 404 ? 'Không tìm thấy bài viết này.' : e.message));
  }, [slug]);

  if (error)
    return (
      <section className="page-narrow">
        <p className="page-status">{error}</p>
        <Link to="/posts">Xem các bài viết khác</Link>
      </section>
    );
  if (!post) return <p className="page-status">Đang tải…</p>;

  return (
    <article className="article">
      <header className="article-head">
        <p className="muted">
          <Link to="/posts">Bài viết</Link>
          {post.tags.map((t) => (
            <span key={t}>
              {' / '}
              <Link to={`/posts?tag=${encodeURIComponent(t)}`}>{t}</Link>
            </span>
          ))}
        </p>
        <h1>{post.title}</h1>
        {post.summary && <p className="article-summary">{post.summary}</p>}
        <p className="article-meta">
          <Avatar name={post.author.displayName} url={post.author.avatarUrl} size={28} />
          <span>{post.author.displayName}</span>
          <time dateTime={post.publishedAt}>{formatDate(post.publishedAt)}</time>
          <span>{post.readingMinutes} phút đọc</span>
          {isAdmin && (
            <Link className="edit-link" to={`/admin/posts/${post.id}`}>
              Sửa bài
            </Link>
          )}
        </p>
      </header>
      {safeUrl(post.coverImageUrl) && (
        <img className="article-cover" src={safeUrl(post.coverImageUrl)} alt="" referrerPolicy="no-referrer" />
      )}
      <Markdown className="prose prose-article">{post.content}</Markdown>
      <Comments slug={post.slug} />
    </article>
  );
}
