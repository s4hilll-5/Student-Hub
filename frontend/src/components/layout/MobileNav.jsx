import { useEffect } from 'react';
import { X } from 'lucide-react';
import { SidebarContent } from './Sidebar';

/** Slide-in navigation drawer for screens narrower than the desktop sidebar breakpoint. */
export default function MobileNav({ open, onClose }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (event) => event.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = overflow;
    };
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation">
      <div className="absolute inset-0 bg-[#0f0e19]/50" onClick={onClose} aria-hidden="true" />
      <div className="absolute inset-y-0 left-0 w-[min(18rem,85vw)] border-r border-line bg-surface shadow-xl">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-3 top-5 rounded-lg p-1.5 text-muted hover:bg-subtle hover:text-ink"
          aria-label="Close navigation"
        >
          <X size={18} />
        </button>
        <SidebarContent onNavigate={onClose} />
      </div>
    </div>
  );
}
