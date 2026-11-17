import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { useProfile } from '../lib/useProfile.js';
import Avatar from './Avatar.jsx';
import ErrorBoundary from './ErrorBoundary.jsx';

function AccountMenu() {
  const { user, isAdmin, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => setOpen(false), [location.pathname]);
  useEffect(() => {
    if (!open) return undefined;
    const close = (e) => {
      if (e.key === 'Escape' || (e.type === 'mousedown' && !ref.current?.contains(e.target))) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', close);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', close);
    };
  }, [open]);

  const onLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <div className="account" ref={ref}>
      <button
        type="button"
        className="account-trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        <Avatar name={user.displayName} url={user.avatarUrl} size={30} />
        <span className="account-name">{user.displayName}</span>
      </button>
      {open && (
        <div className="account-menu" role="menu">
          {isAdmin && (
            <Link role="menuitem" to="/admin">
              Quản lý portfolio
            </Link>
          )}
          <Link role="menuitem" to="/account">
            Tài khoản của tôi
          </Link>
          <button role="menuitem" type="button" onClick={onLogout}>
            Đăng xuất
          </button>
        </div>
      )}
    </div>
  );
}

export default function Layout() {
  const { user, ready } = useAuth();
  const location = useLocation();
  const { profile } = useProfile();
  const name = profile?.fullName || 'Portfolio';

  return (
    <div className="shell">
      <a className="skip-link" href="#main">
        Bỏ qua đến nội dung
      </a>
      <header className="site-header">
        <div className="wrap header-inner">
          <Link to="/" className="wordmark">
            <span className="wordmark-block" aria-hidden="true" />
            {name}
          </Link>
          <nav className="site-nav" aria-label="Chính">
            <NavLink to="/" end>
              Giới thiệu
            </NavLink>
            <NavLink to="/posts">Bài viết</NavLink>
            {ready &&
              (user ? (
                <AccountMenu />
              ) : (
                <Link to="/login" className="btn btn-small">
                  Đăng nhập
                </Link>
              ))}
          </nav>
        </div>
      </header>
      <main id="main" className="site-main">
        <ErrorBoundary key={location.pathname}>
          <Outlet />
        </ErrorBoundary>
      </main>
      <footer className="site-footer">
        <div className="wrap footer-inner">
          <span>
            © {new Date().getFullYear()} {name}
          </span>
          <span className="footer-links">
            <Link to="/privacy" className="muted">
              Quyền riêng tư
            </Link>
            <Link to="/terms" className="muted">
              Điều khoản
            </Link>
            {ready && !user && (
              <Link to="/login" className="muted">
                Đăng nhập để bình luận
              </Link>
            )}
          </span>
        </div>
      </footer>
    </div>
  );
}
