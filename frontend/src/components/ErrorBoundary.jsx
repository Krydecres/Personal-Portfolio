import { Component } from 'react';
import { Link } from 'react-router-dom';

/**
 * Catches render errors in one page so the header stays usable and the user sees a message
 * instead of a blank screen. Resets when the route changes (key = pathname in Layout).
 */
export default class ErrorBoundary extends Component {
  state = { error: null };

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('Page crashed:', error, info.componentStack);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <section className="page-narrow">
        <h1 className="page-title">Trang này gặp lỗi</h1>
        <p className="muted">Hãy tải lại trang. Nếu vẫn lỗi, quay về trang chủ và thử lại.</p>
        <p>
          <button type="button" className="btn" onClick={() => window.location.reload()}>
            Tải lại trang
          </button>{' '}
          <Link to="/">Về trang chủ</Link>
        </p>
      </section>
    );
  }
}
