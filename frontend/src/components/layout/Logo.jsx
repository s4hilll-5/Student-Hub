import { Link } from 'react-router-dom';

export default function Logo({ to = '/', className = '' }) {
  return (
    <Link to={to} className={`inline-flex items-center gap-2.5 ${className}`} aria-label="StudentHub home">
      <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand text-white dark:text-[#14132a]" aria-hidden="true">
        <svg viewBox="0 0 32 32" className="h-5 w-5">
          <path d="M8 22V10h3.4v4.6h5.2V10H20v12h-3.4v-4.4h-5.2V22z" fill="currentColor" />
          <rect x="21.6" y="18.6" width="3.4" height="3.4" rx="1" fill="currentColor" opacity="0.6" />
        </svg>
      </span>
      <span className="font-display text-lg font-semibold tracking-tight text-ink">StudentHub</span>
    </Link>
  );
}
