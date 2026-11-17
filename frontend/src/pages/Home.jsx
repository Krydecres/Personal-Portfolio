import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { postApi } from '../api/endpoints.js';
import { useAuth } from '../auth/AuthContext.jsx';
import Avatar from '../components/Avatar.jsx';
import Markdown from '../components/Markdown.jsx';
import { formatDate, formatRange, safeUrl } from '../lib/format.js';
import { useProfile } from '../lib/useProfile.js';

function Hero({ profile }) {
  const words = (profile.fullName || '').split(/\s+/).filter(Boolean);
  const links = [
    ['GitHub', profile.githubUrl],
    ['LinkedIn', profile.linkedinUrl],
    ['Website', profile.websiteUrl],
    ['CV', profile.resumeUrl],
  ].filter(([, url]) => safeUrl(url));

  return (
    <section className="hero wrap" aria-labelledby="hero-name">
      <div className="hero-text">
        <h1 id="hero-name" className="hero-name">
          {words.map((w, i) => (
            <span key={i}>
              <span className="hero-word" style={{ '--i': i }}>
                {w}
              </span>
              {i < words.length - 1 && ' '}
            </span>
          ))}
        </h1>
        {profile.headline && <p className="hero-headline">{profile.headline}</p>}
        <div className="hero-facts">
          {profile.availability && <span className="status-pill">{profile.availability}</span>}
          {profile.location && <span className="muted">{profile.location}</span>}
        </div>
        {(links.length > 0 || profile.contactEmail) && (
          <ul className="hero-links">
            {links.map(([label, url]) => (
              <li key={label}>
                <a href={safeUrl(url)} target="_blank" rel="noopener noreferrer">
                  {label}
                </a>
              </li>
            ))}
            {profile.contactEmail && (
              <li>
                <a href={`mailto:${profile.contactEmail}`}>{profile.contactEmail}</a>
              </li>
            )}
          </ul>
        )}
      </div>
      <div className="hero-portrait">
        <div className="portrait-block" aria-hidden="true" />
        {safeUrl(profile.avatarUrl) ? (
          <img src={safeUrl(profile.avatarUrl)} alt={`Ảnh của ${profile.fullName}`} referrerPolicy="no-referrer" />
        ) : (
          <div className="portrait-empty">
            <Avatar name={profile.fullName} size={120} />
          </div>
        )}
      </div>
    </section>
  );
}

function Section({ id, title, children, action }) {
  return (
    <section className="section wrap" aria-labelledby={id}>
      <div className="section-head">
        <h2 id={id}>{title}</h2>
        {action}
      </div>
      <div className="section-body">{children}</div>
    </section>
  );
}

export default function Home() {
  const { profile, loading, error } = useProfile();
  const { isAdmin } = useAuth();
  const [posts, setPosts] = useState(null);

  useEffect(() => {
    postApi
      .list({ size: 3 })
      .then((r) => setPosts(r.content))
      .catch(() => setPosts([]));
  }, []);

  if (loading) return <p className="page-status">Đang tải…</p>;
  if (error || !profile)
    return (
      <p className="page-status">Không tải được hồ sơ. Kiểm tra backend đang chạy ở cổng 8080 rồi tải lại trang.</p>
    );

  const editLink = (to) =>
    isAdmin ? (
      <Link className="edit-link" to={to}>
        Sửa
      </Link>
    ) : null;

  return (
    <>
      <Hero profile={profile} />

      <Section id="about" title="Giới thiệu" action={editLink('/admin')}>
        <Markdown className="prose prose-lead">{profile.bio}</Markdown>
        {profile.skills.length > 0 && (
          <ul className="skills" aria-label="Kỹ năng">
            {profile.skills.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ul>
        )}
      </Section>

      {(profile.experiences.length > 0 || isAdmin) && (
        <Section id="experience" title="Kinh nghiệm" action={editLink('/admin/experience')}>
          {profile.experiences.length === 0 ? (
            <p className="muted">Chưa có kinh nghiệm nào. Thêm ở trang quản lý.</p>
          ) : (
            <ol className="timeline">
              {profile.experiences.map((e) => (
                <li key={e.id}>
                  <p className="timeline-when">{formatRange(e.startDate, e.endDate)}</p>
                  <div>
                    <h3>
                      {e.role} <span className="muted">tại {e.organization}</span>
                    </h3>
                    {e.location && <p className="muted small">{e.location}</p>}
                    {e.description && <Markdown>{e.description}</Markdown>}
                  </div>
                </li>
              ))}
            </ol>
          )}
        </Section>
      )}

      {(profile.projects.length > 0 || isAdmin) && (
        <Section id="projects" title="Dự án" action={editLink('/admin/projects')}>
          {profile.projects.length === 0 ? (
            <p className="muted">Chưa có dự án nào. Thêm ở trang quản lý.</p>
          ) : (
            <div className="projects">
              {profile.projects.map((p) => (
                <article key={p.id} className={`project${p.featured ? ' project-featured' : ''}`}>
                  {safeUrl(p.imageUrl) && (
                    <img src={safeUrl(p.imageUrl)} alt="" loading="lazy" referrerPolicy="no-referrer" />
                  )}
                  <div className="project-body">
                    <h3>{p.name}</h3>
                    {p.summary && <p>{p.summary}</p>}
                    {p.techStack.length > 0 && <p className="tech">{p.techStack.join(', ')}</p>}
                    <p className="project-links">
                      {safeUrl(p.projectUrl) && (
                        <a href={safeUrl(p.projectUrl)} target="_blank" rel="noopener noreferrer">
                          Xem dự án
                        </a>
                      )}
                      {safeUrl(p.repoUrl) && (
                        <a href={safeUrl(p.repoUrl)} target="_blank" rel="noopener noreferrer">
                          Mã nguồn
                        </a>
                      )}
                    </p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </Section>
      )}

      <Section
        id="writing"
        title="Bài viết mới"
        action={
          <Link className="edit-link" to="/posts">
            Tất cả bài viết
          </Link>
        }
      >
        {posts === null ? (
          <p className="muted">Đang tải…</p>
        ) : posts.length === 0 ? (
          <p className="muted">
            Chưa có bài viết nào{isAdmin ? '. ' : '.'}
            {isAdmin && <Link to="/admin/posts/new">Viết bài đầu tiên</Link>}
          </p>
        ) : (
          <ul className="post-list">
            {posts.map((p) => (
              <li key={p.id}>
                <time dateTime={p.publishedAt}>{formatDate(p.publishedAt)}</time>
                <div>
                  <Link to={`/posts/${p.slug}`} className="post-title">
                    {p.title}
                  </Link>
                  {p.summary && <p className="muted">{p.summary}</p>}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Section>
    </>
  );
}
