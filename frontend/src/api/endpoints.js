import { http } from './client.js';

export const authApi = {
  providers: () => http.get('/api/auth/providers'),
  login: (email, password) => http.post('/api/auth/login', { email, password }),
  register: (body) => http.post('/api/auth/register', body),
  logout: () => http.post('/api/auth/logout'),
};

export const accountApi = {
  me: () => http.get('/api/account/me'),
  update: (body) => http.put('/api/account/me', body),
  changePassword: (body) => http.post('/api/account/password', body),
};

export const profileApi = {
  get: () => http.get('/api/profile'),
  update: (body) => http.put('/api/admin/profile', body),
  addExperience: (body) => http.post('/api/admin/profile/experiences', body),
  updateExperience: (id, body) => http.put(`/api/admin/profile/experiences/${id}`, body),
  deleteExperience: (id) => http.del(`/api/admin/profile/experiences/${id}`),
  addProject: (body) => http.post('/api/admin/profile/projects', body),
  updateProject: (id, body) => http.put(`/api/admin/profile/projects/${id}`, body),
  deleteProject: (id) => http.del(`/api/admin/profile/projects/${id}`),
};

const q = (params) => {
  const s = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') s.set(k, v);
  });
  const str = s.toString();
  return str ? `?${str}` : '';
};

export const postApi = {
  list: ({ page = 0, size = 10, tag } = {}) => http.get(`/api/posts${q({ page, size, tag })}`),
  tags: () => http.get('/api/posts/tags'),
  get: (slug) => http.get(`/api/posts/${encodeURIComponent(slug)}`),
  comments: (slug) => http.get(`/api/posts/${encodeURIComponent(slug)}/comments`),
  addComment: (slug, content) => http.post(`/api/posts/${encodeURIComponent(slug)}/comments`, { content }),
  deleteComment: (id) => http.del(`/api/comments/${id}`),
  adminList: ({ page = 0, size = 20 } = {}) => http.get(`/api/admin/posts${q({ page, size })}`),
  adminGet: (id) => http.get(`/api/admin/posts/${id}`),
  create: (body) => http.post('/api/admin/posts', body),
  update: (id, body) => http.put(`/api/admin/posts/${id}`, body),
  remove: (id) => http.del(`/api/admin/posts/${id}`),
};
