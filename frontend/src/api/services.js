import { api, ensureCsrfCookie } from './client';

/** Drops undefined, null and empty-string params so they don't reach the query string. */
function clean(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

const data = (promise) => promise.then((response) => response.data);

export const authApi = {
  register: (payload) => data(api.post('/auth/register', payload, { skipAuthRefresh: true })),
  login: (payload) => data(api.post('/auth/login', payload, { skipAuthRefresh: true })),
  logout: async () => {
    await ensureCsrfCookie();
    return api.post('/auth/logout', null, { skipAuthRefresh: true });
  },
  forgotPassword: (email) => data(api.post('/auth/forgot-password', { email }, { skipAuthRefresh: true })),
  resetPassword: (token, newPassword) =>
    data(api.post('/auth/reset-password', { token, newPassword }, { skipAuthRefresh: true })),
};

export const usersApi = {
  me: () => data(api.get('/users/me')),
  update: (payload) => data(api.put('/users/me', payload)),
  changePassword: (payload) => data(api.put('/users/me/password', payload)),
  deleteAccount: (payload) => api.delete('/users/me', { data: payload }),
};

export const assignmentsApi = {
  list: (params) => data(api.get('/assignments', { params: clean(params) })),
  get: (id) => data(api.get(`/assignments/${id}`)),
  create: (payload) => data(api.post('/assignments', payload)),
  update: (id, payload) => data(api.put(`/assignments/${id}`, payload)),
  setStatus: (id, status) => data(api.patch(`/assignments/${id}/status`, { status })),
  remove: (id) => api.delete(`/assignments/${id}`),
};

export const subjectsApi = {
  list: (params) => data(api.get('/subjects', { params: clean(params) })),
  get: (id) => data(api.get(`/subjects/${id}`)),
  create: (payload) => data(api.post('/subjects', payload)),
  update: (id, payload) => data(api.put(`/subjects/${id}`, payload)),
  remove: (id) => api.delete(`/subjects/${id}`),
  attendance: (id, params) => data(api.get(`/subjects/${id}/attendance`, { params: clean(params) })),
  recordAttendance: (id, payload) => data(api.post(`/subjects/${id}/attendance`, payload)),
};

export const attendanceApi = {
  update: (id, payload) => data(api.put(`/attendance/${id}`, payload)),
  remove: (id) => api.delete(`/attendance/${id}`),
};

export const timetableApi = {
  list: (day) => data(api.get('/timetable', { params: clean({ day }) })),
  create: (payload) => data(api.post('/timetable', payload)),
  update: (id, payload) => data(api.put(`/timetable/${id}`, payload)),
  remove: (id) => api.delete(`/timetable/${id}`),
};

export const studySessionsApi = {
  list: (params) => data(api.get('/study-sessions', { params: clean(params) })),
  create: (payload) => data(api.post('/study-sessions', payload)),
  update: (id, payload) => data(api.put(`/study-sessions/${id}`, payload)),
  setCompletion: (id, payload) => data(api.patch(`/study-sessions/${id}/completion`, payload)),
  remove: (id) => api.delete(`/study-sessions/${id}`),
};

export const examsApi = {
  list: (params) => data(api.get('/exams', { params: clean(params) })),
  get: (id) => data(api.get(`/exams/${id}`)),
  create: (payload) => data(api.post('/exams', payload)),
  update: (id, payload) => data(api.put(`/exams/${id}`, payload)),
  remove: (id) => api.delete(`/exams/${id}`),
};

export const notesApi = {
  list: (params) => data(api.get('/notes', { params: clean(params) })),
  tags: () => data(api.get('/notes/tags')),
  get: (id) => data(api.get(`/notes/${id}`)),
  create: (payload) => data(api.post('/notes', payload)),
  update: (id, payload) => data(api.put(`/notes/${id}`, payload)),
  setPinned: (id, pinned) => data(api.patch(`/notes/${id}/pin`, { pinned })),
  remove: (id) => api.delete(`/notes/${id}`),
};

export const dashboardApi = {
  summary: () => data(api.get('/dashboard/summary')),
  analytics: (weeks = 8) => data(api.get('/dashboard/analytics', { params: { weeks } })),
};

export const adminApi = {
  users: (params) => data(api.get('/admin/users', { params: clean(params) })),
  stats: () => data(api.get('/admin/stats')),
  setEnabled: (id, enabled) => data(api.patch(`/admin/users/${id}/status`, { enabled })),
};
