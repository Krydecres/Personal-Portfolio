import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { postApi } from '../api/endpoints.js';
import { formatDate } from '../lib/format.js';

export default function Posts() {
  const [params, setParams] = useSearchParams();
  const tag = params.get('tag') || '';
  const page = Math.max(0, Number(params.get('page') || 0) || 0);
  const [data, setData] = useState(null);
  const [tags, setTags] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    postApi.tags().then(setTags).catch(() => setTags([]));
  }, []);

  useEffect(() => {
    setData(null);
    postApi
      .list({ page, size: 10, tag })
      .then(setData)
      .catch((e) => setError(e.message));
  }, [page, tag]);

  const go = (next) => {
    const p = new URLSearchParams(params);
    Object.entries(next).forEach(([k, v]) => (v === '' || v === 0 ? p.delete(k) : p.set(k, v)));
    setParams(p);
  };

  return (
    <section className="wrap page">
      <h1 className="page-title">Bài viết</h1>
      {tags.length > 0 && (
        <div className="tag-filter" role="group" aria-label="Lọc theo thẻ">
          <button type="button" aria-pressed={!tag} onClick={() => go({ tag: '', page: 0 })}>
            Tất cả
          </button>
          {tags.map((t) => (
            <button key={t} type="button" aria-pressed={tag === t} onClick={() => go({ tag: t, page: 0 })}>
              {t}
            </button>
          ))}
        </div>
      )}

      {error && <p className="page-status">{error}</p>}
      {!data && !error && <p className="page-status">Đang tải…</p>}
      {data && data.content.length === 0 && (
        <p className="page-status">{tag ? `Chưa có bài nào gắn thẻ “${tag}”.` : 'Chưa có bài viết nào.'}</p>
      )}
      {data && data.content.length > 0 && (
        <>
          <ul className="post-list post-list-full">
            {data.content.map((p) => (
              <li key={p.id}>
                <time dateTime={p.publishedAt}>{formatDate(p.publishedAt)}</time>
                <div>
                  <Link to={`/posts/${p.slug}`} className="post-title">
                    {p.title}
                  </Link>
                  {p.summary && <p className="muted">{p.summary}</p>}
                  <p className="post-meta">
                    <span>{p.readingMinutes} phút đọc</span>
                    {p.tags.length > 0 && <span>Thẻ: {p.tags.join(', ')}</span>}
                  </p>
                </div>
              </li>
            ))}
          </ul>
          {data.totalPages > 1 && (
            <nav className="pager" aria-label="Phân trang">
              <button type="button" className="btn btn-ghost" disabled={page === 0} onClick={() => go({ page: page - 1 })}>
                Trang trước
              </button>
              <span className="muted">
                Trang {page + 1}/{data.totalPages}
              </span>
              <button
                type="button"
                className="btn btn-ghost"
                disabled={page + 1 >= data.totalPages}
                onClick={() => go({ page: page + 1 })}
              >
                Trang sau
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  );
}
