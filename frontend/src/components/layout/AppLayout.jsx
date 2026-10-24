import { useCallback, useState } from 'react';
import { Outlet } from 'react-router-dom';
import MobileNav from './MobileNav';
import Navbar from './Navbar';
import Sidebar from './Sidebar';

export default function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  const closeMenu = useCallback(() => setMenuOpen(false), []);
  return (
    <div className="min-h-screen bg-bg">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-surface focus:px-3 focus:py-2"
      >
        Skip to content
      </a>
      <Sidebar />
      <MobileNav open={menuOpen} onClose={closeMenu} />
      <div className="lg:pl-64">
        <Navbar onOpenMenu={() => setMenuOpen(true)} />
        <main id="main" className="mx-auto max-w-6xl px-4 pb-16 pt-6 sm:px-6 lg:px-8 lg:pt-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
