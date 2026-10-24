import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import AppLayout from './components/layout/AppLayout';
import ProtectedRoute from './components/routing/ProtectedRoute';
import PublicOnlyRoute from './components/routing/PublicOnlyRoute';
import LoadingSpinner from './components/ui/LoadingSpinner';

// Pages are split into separate chunks so the landing page doesn't download chart code.
const Landing = lazy(() => import('./pages/Landing'));
const Login = lazy(() => import('./pages/auth/Login'));
const Register = lazy(() => import('./pages/auth/Register'));
const ForgotPassword = lazy(() => import('./pages/auth/ForgotPassword'));
const ResetPassword = lazy(() => import('./pages/auth/ResetPassword'));
const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));
const AssignmentsPage = lazy(() => import('./pages/assignments/AssignmentsPage'));
const AttendancePage = lazy(() => import('./pages/attendance/AttendancePage'));
const SubjectDetailPage = lazy(() => import('./pages/attendance/SubjectDetailPage'));
const TimetablePage = lazy(() => import('./pages/timetable/TimetablePage'));
const ExamsPage = lazy(() => import('./pages/exams/ExamsPage'));
const NotesPage = lazy(() => import('./pages/notes/NotesPage'));
const AnalyticsPage = lazy(() => import('./pages/analytics/AnalyticsPage'));
const SettingsPage = lazy(() => import('./pages/settings/SettingsPage'));
const AdminPage = lazy(() => import('./pages/admin/AdminPage'));
const NotFound = lazy(() => import('./pages/NotFound'));

function PageFallback() {
  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <LoadingSpinner label="Loading page" />
    </div>
  );
}

export default function App() {
  return (
    <>
      <Suspense fallback={<PageFallback />}>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
          </Route>
          <Route path="/reset-password" element={<ResetPassword />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/assignments" element={<AssignmentsPage />} />
              <Route path="/attendance" element={<AttendancePage />} />
              <Route path="/attendance/:subjectId" element={<SubjectDetailPage />} />
              <Route path="/timetable" element={<TimetablePage />} />
              <Route path="/exams" element={<ExamsPage />} />
              <Route path="/notes" element={<NotesPage />} />
              <Route path="/analytics" element={<AnalyticsPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route
                path="/admin"
                element={
                  <ProtectedRoute roles={['ADMIN']}>
                    <AdminPage />
                  </ProtectedRoute>
                }
              />
            </Route>
          </Route>

          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
      <Toaster
        position="top-right"
        toastOptions={{
          className: '!bg-surface !text-ink !border !border-line !shadow-lg !text-sm',
          duration: 3500,
        }}
      />
    </>
  );
}
