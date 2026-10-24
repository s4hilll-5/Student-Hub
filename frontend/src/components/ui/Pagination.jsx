import { ChevronLeft, ChevronRight } from 'lucide-react';
import Button from './Button';

/** page is zero-based, matching the API. */
export default function Pagination({ page, size, totalElements, totalPages, onPageChange }) {
  if (!totalElements) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return (
    <nav className="flex items-center justify-between gap-3 border-t border-line px-5 py-3" aria-label="Pagination">
      <p className="text-[13px] text-muted tabular">
        {from}–{to} of {totalElements}
      </p>
      <div className="flex items-center gap-1">
        <Button
          variant="ghost"
          size="icon-sm"
          icon={ChevronLeft}
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
          aria-label="Previous page"
        />
        <span className="px-2 text-[13px] text-muted tabular">
          Page {page + 1} of {Math.max(totalPages, 1)}
        </span>
        <Button
          variant="ghost"
          size="icon-sm"
          icon={ChevronRight}
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
          aria-label="Next page"
        />
      </div>
    </nav>
  );
}
