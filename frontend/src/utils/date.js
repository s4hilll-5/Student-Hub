/**
 * Date helpers. The API sends instants as ISO strings in UTC ("2026-10-20T12:30:00Z"),
 * calendar dates as "YYYY-MM-DD" and clock times as "HH:mm" or "HH:mm:ss".
 * Everything is displayed in the browser's local timezone.
 */

export const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export const DAY_LABELS = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
};

const pad = (n) => String(n).padStart(2, '0');

/** "YYYY-MM-DD" for a Date in local time. */
export function toISODate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function todayISO() {
  return toISODate(new Date());
}

/** Parses "YYYY-MM-DD" as a local date (new Date("2026-10-16") would be UTC midnight). */
export function parseISODate(value) {
  const [y, m, d] = value.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function addDays(date, days) {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

/** Monday of the week containing the date. */
export function startOfWeek(date) {
  const copy = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const offset = (copy.getDay() + 6) % 7; // Monday = 0
  copy.setDate(copy.getDate() - offset);
  return copy;
}

export function dayKeyOf(date) {
  return DAYS[(date.getDay() + 6) % 7];
}

/** Value for <input type="datetime-local"> from an ISO instant. */
export function toLocalInputValue(isoInstant) {
  if (!isoInstant) return '';
  const d = new Date(isoInstant);
  return `${toISODate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** ISO instant (UTC) from a datetime-local value, which is in the user's local time. */
export function fromLocalInputValue(value) {
  return value ? new Date(value).toISOString() : null;
}

/** "09:00:00" or "09:00" -> "09:00" (for <input type="time">). */
export function toTimeInput(value) {
  return value ? value.slice(0, 5) : '';
}

export function formatTime(value) {
  if (!value) return '';
  const [h, m] = value.split(':').map(Number);
  const d = new Date();
  d.setHours(h, m, 0, 0);
  return d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
}

export function formatTimeRange(start, end) {
  return `${formatTime(start)} – ${formatTime(end)}`;
}

export function formatDate(value, options = { day: 'numeric', month: 'short', year: 'numeric' }) {
  if (!value) return '';
  const date = typeof value === 'string' && value.length === 10 ? parseISODate(value) : new Date(value);
  return date.toLocaleDateString(undefined, options);
}

export function formatDateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString(undefined, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export function minutesBetween(start, end) {
  const [sh, sm] = start.split(':').map(Number);
  const [eh, em] = end.split(':').map(Number);
  return eh * 60 + em - (sh * 60 + sm);
}

export function formatMinutes(total) {
  if (!total) return '0 min';
  const h = Math.floor(total / 60);
  const m = total % 60;
  if (!h) return `${m} min`;
  return m ? `${h} h ${m} min` : `${h} h`;
}

const DAY_MS = 24 * 60 * 60 * 1000;

/** Whole calendar days from today to the date (negative if past). */
export function calendarDaysUntil(value, now = new Date()) {
  const target = new Date(value);
  const a = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const b = new Date(target.getFullYear(), target.getMonth(), target.getDate());
  return Math.round((b - a) / DAY_MS);
}

/**
 * Human wording for a deadline: "Due today at 5:00 PM", "Due tomorrow", "Due in 4 days",
 * "Overdue by 2 days".
 */
export function describeDue(value, now = new Date()) {
  const due = new Date(value);
  const days = calendarDaysUntil(due, now);
  const time = due.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
  if (due < now) {
    if (days === 0) return `Was due today at ${time}`;
    const ago = Math.abs(days);
    return `Overdue by ${ago} ${ago === 1 ? 'day' : 'days'}`;
  }
  if (days === 0) return `Due today at ${time}`;
  if (days === 1) return `Due tomorrow at ${time}`;
  if (days < 7) return `Due ${due.toLocaleDateString(undefined, { weekday: 'long' })} at ${time}`;
  return `Due in ${days} days`;
}

/** Countdown for exams: { days, hours, label }. */
export function countdown(value, now = new Date()) {
  const diff = new Date(value) - now;
  if (diff <= 0) return { days: 0, hours: 0, label: 'Started' };
  const days = Math.floor(diff / DAY_MS);
  const hours = Math.floor((diff % DAY_MS) / (60 * 60 * 1000));
  const calendarDays = calendarDaysUntil(value, now);
  let label;
  if (calendarDays === 0) label = hours > 0 ? `Today, in ${hours} h` : 'Within the hour';
  else if (calendarDays === 1) label = 'Tomorrow';
  else label = `In ${calendarDays} days`;
  return { days, hours, label };
}

export function greetingFor(date = new Date()) {
  const h = date.getHours();
  if (h < 5) return 'Working late';
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  return 'Good evening';
}
