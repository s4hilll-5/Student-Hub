import { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { cx } from './cx';

const SIZES = { sm: 'sm:max-w-md', md: 'sm:max-w-lg', lg: 'sm:max-w-2xl' };
const FOCUSABLE = 'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Accessible dialog: focus moves inside on open, Tab is trapped, Escape and the backdrop
 * close it, focus returns to the trigger on close. Slides up as a sheet on small screens.
 */
export default function Modal({ open, onClose, title, description, children, size = 'md', dismissible = true }) {
  const titleId = useId();
  const descriptionId = useId();
  const panelRef = useRef(null);
  const contentRef = useRef(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!open) return undefined;
    const previouslyFocused = document.activeElement;
    const panel = panelRef.current;
    // Prefer an explicit [data-autofocus], then the first field in the body, then anything.
    const first =
      panel?.querySelector('[data-autofocus]') ||
      contentRef.current?.querySelector(FOCUSABLE) ||
      panel?.querySelector(FOCUSABLE);
    first?.focus();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    function onKeyDown(event) {
      if (event.key === 'Escape' && dismissible) {
        event.stopPropagation();
        onCloseRef.current?.();
      }
      if (event.key === 'Tab' && panel) {
        const items = [...panel.querySelectorAll(FOCUSABLE)];
        if (!items.length) return;
        const firstItem = items[0];
        const lastItem = items[items.length - 1];
        if (event.shiftKey && document.activeElement === firstItem) {
          event.preventDefault();
          lastItem.focus();
        } else if (!event.shiftKey && document.activeElement === lastItem) {
          event.preventDefault();
          firstItem.focus();
        }
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
      if (previouslyFocused instanceof HTMLElement) previouslyFocused.focus();
    };
  }, [open, dismissible]);

  if (!open) return null;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-6">
      <div
        className="absolute inset-0 bg-[#0f0e19]/55 backdrop-blur-[2px]"
        onClick={() => dismissible && onClose?.()}
        aria-hidden="true"
      />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        className={cx(
          'relative flex max-h-[92vh] w-full flex-col overflow-hidden rounded-t-2xl border border-line bg-surface shadow-2xl shadow-black/20 sm:rounded-2xl',
          SIZES[size],
        )}
      >
        <div className="flex items-start justify-between gap-4 px-5 pt-5 sm:px-6">
          <div>
            <h2 id={titleId} className="text-lg font-semibold">
              {title}
            </h2>
            {description && (
              <p id={descriptionId} className="mt-1 text-sm text-muted">
                {description}
              </p>
            )}
          </div>
          {dismissible && (
            <button
              type="button"
              onClick={onClose}
              className="-mr-1.5 rounded-lg p-1.5 text-muted transition hover:bg-subtle hover:text-ink"
              aria-label="Close"
            >
              <X size={18} />
            </button>
          )}
        </div>
        <div ref={contentRef} className="overflow-y-auto">
          {children}
        </div>
      </div>
    </div>,
    document.body,
  );
}

export function ModalBody({ children, className }) {
  return <div className={cx('space-y-4 px-5 py-4 sm:px-6', className)}>{children}</div>;
}

export function ModalFooter({ children }) {
  return (
    <div className="sticky bottom-0 flex flex-col-reverse gap-2 border-t border-line bg-surface px-5 py-3.5 sm:flex-row sm:justify-end sm:px-6">
      {children}
    </div>
  );
}
