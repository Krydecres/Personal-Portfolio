import { useEffect } from 'react';
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext.jsx';
import RequireAuth from './auth/RequireAuth.jsx';
import Layout from './components/Layout.jsx';
import { useProfile } from './lib/useProfile.js';
import Account from './pages/Account.jsx';
import AuthCallback from './pages/AuthCallback.jsx';
import Home from './pages/Home.jsx';
import { PrivacyPolicy, TermsOfService } from './pages/Legal.jsx';
import Login from './pages/Login.jsx';
import NotFound from './pages/NotFound.jsx';
import PostDetail from './pages/PostDetail.jsx';
import Posts from './pages/Posts.jsx';
import Register from './pages/Register.jsx';
import AdminLayout from './pages/admin/AdminLayout.jsx';
import ExperienceEditor from './pages/admin/ExperienceEditor.jsx';
import PostEditor from './pages/admin/PostEditor.jsx';
import PostsAdmin from './pages/admin/PostsAdmin.jsx';
import ProfileEditor from './pages/admin/ProfileEditor.jsx';
import ProjectEditor from './pages/admin/ProjectEditor.jsx';

/** Resets scroll and the document title on navigation. */
function RouteEffects() {
  const { pathname } = useLocation();
  const { profile } = useProfile();
  useEffect(() => {
    window.scrollTo(0, 0);
    if (!pathname.startsWith('/posts/')) document.title = profile?.fullName || 'Portfolio';
  }, [pathname, profile?.fullName]);
  return null;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <RouteEffects />
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Home />} />
            <Route path="posts" element={<Posts />} />
            <Route path="posts/:slug" element={<PostDetail />} />
            <Route path="login" element={<Login />} />
            <Route path="register" element={<Register />} />
            <Route path="auth/callback" element={<AuthCallback />} />
            <Route path="privacy" element={<PrivacyPolicy />} />
            <Route path="terms" element={<TermsOfService />} />
            <Route
              path="account"
              element={
                <RequireAuth>
                  <Account />
                </RequireAuth>
              }
            />
            <Route
              path="admin"
              element={
                <RequireAuth admin>
                  <AdminLayout />
                </RequireAuth>
              }
            >
              <Route index element={<ProfileEditor />} />
              <Route path="experience" element={<ExperienceEditor />} />
              <Route path="projects" element={<ProjectEditor />} />
              <Route path="posts" element={<PostsAdmin />} />
              <Route path="posts/new" element={<PostEditor />} />
              <Route path="posts/:id" element={<PostEditor />} />
            </Route>
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
