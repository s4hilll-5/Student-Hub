import { Link } from 'react-router-dom';
import Logo from '../components/layout/Logo';

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col bg-bg px-4 sm:px-6">
      <div className="py-5">
        <Logo />
      </div>
      <div className="mx-auto flex max-w-md flex-1 flex-col items-start justify-center pb-24">
        <p className="font-display text-[64px] font-semibold leading-none text-brand">404</p>
        <h1 className="mt-4 text-2xl font-semibold">This page doesn't exist</h1>
        <p className="mt-2 text-muted">The link may be old, or the address has a typo.</p>
        <Link
          to="/dashboard"
          className="mt-6 inline-flex h-10 items-center rounded-[10px] bg-brand px-4 text-sm font-medium text-white hover:bg-brand-strong dark:text-[#14132a]"
        >
          Go to your dashboard
        </Link>
      </div>
    </div>
  );
}
