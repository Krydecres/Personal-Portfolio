import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';

/**
 * Client-side route guard. This only decides what to render: the real enforcement is on the server,
 * which rejects every admin call without a valid ADMIN token.
 */
export default function RequireAuth({ children, admin = false }) {
  const { user, ready, isAdmin } = useAuth();
  const location = useLocation();

  if (!ready) return <p className="page-status">Đang kiểm tra phiên đăng nhập…</p>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (admin && !isAdmin) {
    return (
      <section className="page-narrow">
        <h1 className="page-title">Không có quyền truy cập</h1>
        <p className="muted">Trang này chỉ dành cho chủ portfolio.</p>
      </section>
    );
  }
  return children;
}
