import { LoaderCircle } from 'lucide-react';
import { cx } from './cx';

const VARIANTS = {
  primary: 'bg-brand text-white hover:bg-brand-strong dark:text-[#14132a] disabled:hover:bg-brand',
  secondary: 'border border-line bg-surface text-ink hover:bg-subtle',
  ghost: 'text-muted hover:bg-subtle hover:text-ink',
  danger: 'bg-rose-600 text-white hover:bg-rose-700 disabled:hover:bg-rose-600',
  'danger-ghost': 'text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-500/10',
};

const SIZES = {
  sm: 'h-8 gap-1.5 px-3 text-[13px]',
  md: 'h-10 gap-2 px-4 text-sm',
  icon: 'h-9 w-9 justify-center',
  'icon-sm': 'h-8 w-8 justify-center',
};

export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  icon: Icon,
  type = 'button',
  className,
  children,
  disabled,
  ...props
}) {
  const iconSize = size === 'sm' || size === 'icon-sm' ? 15 : 17;
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cx(
        'inline-flex shrink-0 items-center rounded-[10px] font-medium transition-colors',
        'disabled:cursor-not-allowed disabled:opacity-60',
        VARIANTS[variant],
        SIZES[size],
        className,
      )}
      {...props}
    >
      {loading ? (
        <LoaderCircle size={iconSize} className="animate-spin" aria-hidden="true" />
      ) : (
        Icon && <Icon size={iconSize} aria-hidden="true" />
      )}
      {children}
    </button>
  );
}
