import { NavLink, Outlet } from 'react-router-dom';

export default function AdminLayout() {
  return (
    <section className="wrap admin">
      <h1 className="page-title">Quản lý portfolio</h1>
      <nav className="admin-tabs" aria-label="Quản lý">
        <NavLink to="/admin" end>
          Hồ sơ
        </NavLink>
        <NavLink to="/admin/experience">Kinh nghiệm</NavLink>
        <NavLink to="/admin/projects">Dự án</NavLink>
        <NavLink to="/admin/posts">Bài viết</NavLink>
      </nav>
      <div className="admin-body">
        <Outlet />
      </div>
    </section>
  );
}
