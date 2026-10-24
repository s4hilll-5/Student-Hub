import { useLocation } from 'react-router-dom';
import { Menu } from 'lucide-react';
import ThemeToggle from '../ui/ThemeToggle';
import Logo from './Logo';
import { ADMIN_NAV, NAV_ITEMS, SECONDARY_NAV } from './navItems';

function currentSection(pathname) {
  const all = [...NAV_ITEMS, ...SECONDARY_NAV, ADMIN_NAV];
  return all.find((item) => pathname === item.to || pathname.startsWith(`${item.to}/`))?.label ?? '';
}

/** Top bar: menu button + logo on mobile, current section name on desktop, theme switch. */
export default function Navbar({ onOpenMenu }) {
  const { pathname } = useLocation();
  return (
    <header className="sticky top-0 z-20 border-b border-line bg-bg/85 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-6xl items-center gap-3 px-4 sm:px-6 lg:px-8">
        <button
          type="button"
          onClick={onOpenMenu}
          className="-ml-1.5 rounded-lg p-2 text-muted hover:bg-subtle hover:text-ink lg:hidden"
          aria-label="Open navigation"
        >
          <Menu size={20} />
        </button>
        <div className="lg:hidden">
          <Logo to="/dashboard" />
        </div>
        <p className="hidden text-sm text-muted lg:block">{currentSection(pathname)}</p>
        <div className="ml-auto flex items-center gap-1">
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
