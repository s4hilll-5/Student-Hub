import { NavLink } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { cx } from '../ui/cx';
import Logo from './Logo';
import { ADMIN_NAV, NAV_ITEMS, SECONDARY_NAV } from './navItems';

function NavItem({ item, onNavigate }) {
  const Icon = item.icon;
  return (
    <NavLink
      to={item.to}
      onClick={onNavigate}
      className={({ isActive }) =>
        cx(
          'relative flex items-center gap-3 rounded-lg px-3 py-2 text-[15px] font-medium transition-colors',
          isActive ? 'bg-brand-soft text-brand' : 'text-muted hover:bg-subtle hover:text-ink',
        )
      }
    >
      <Icon size={18} aria-hidden="true" />
      {item.label}
    </NavLink>
  );
}

/** Navigation content, shared by the desktop sidebar and the mobile drawer. */
export function SidebarContent({ onNavigate }) {
  const { user, isAdmin, logout } = useAuth();
  const secondary = isAdmin ? [...SECONDARY_NAV, ADMIN_NAV] : SECONDARY_NAV;
  const initials = (user?.fullName || '?')
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');

  return (
    <div className="flex h-full flex-col">
      <div className="px-5 pb-6 pt-5">
        <Logo to="/dashboard" />
      </div>
      <nav className="flex-1 space-y-1 px-3" aria-label="Main">
        {NAV_ITEMS.map((item) => (
          <NavItem key={item.to} item={item} onNavigate={onNavigate} />
        ))}
        <div className="my-4 border-t border-line" />
        {secondary.map((item) => (
          <NavItem key={item.to} item={item} onNavigate={onNavigate} />
        ))}
      </nav>
      <div className="m-3 flex items-center gap-3 rounded-xl border border-line p-3">
        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-subtle text-sm font-semibold text-ink" aria-hidden="true">
          {initials}
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">{user?.fullName}</p>
          <p className="truncate text-xs text-muted">{user?.email}</p>
        </div>
        <button
          type="button"
          onClick={logout}
          className="rounded-lg p-2 text-muted transition hover:bg-subtle hover:text-ink"
          aria-label="Sign out"
          title="Sign out"
        >
          <LogOut size={17} />
        </button>
      </div>
    </div>
  );
}

export default function Sidebar() {
  return (
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-line bg-surface lg:block">
      <SidebarContent />
    </aside>
  );
}
