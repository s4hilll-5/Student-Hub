import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { FullPageLoader } from '../ui/LoadingSpinner';

/**
 * Guards app pages. While the session is being restored it shows a loader (so a reload
 * doesn't bounce a signed-in user to /login); anonymous users go to /login and come back
 * afterwards. `roles` restricts a route, e.g. roles={['ADMIN']}.
 *
 * This is a usability guard only: the API enforces every permission on the server.
 */
export default function ProtectedRoute({ roles, children }) {
  const { status, user } = useAuth();
  const location = useLocation();

  if (status === 'loading') return <FullPageLoader />;
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  if (roles && !roles.includes(user?.role)) {
    return <Navigate to="/dashboard" replace />;
  }
  return children ?? <Outlet />;
}
