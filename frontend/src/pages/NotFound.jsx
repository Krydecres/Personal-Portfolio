import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <section className="page-narrow">
      <h1 className="page-title">Không tìm thấy trang</h1>
      <p className="muted">Đường dẫn này không tồn tại hoặc đã bị gỡ.</p>
      <Link className="btn" to="/">
        Về trang chủ
      </Link>
    </section>
  );
}
