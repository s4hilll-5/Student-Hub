import { useId, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarCheck, CalendarDays, ChartColumn, ClipboardList, GraduationCap, Minus, NotebookPen, Plus } from 'lucide-react';
import Logo from '../components/layout/Logo';
import ThemeToggle from '../components/ui/ThemeToggle';
import { useAuth } from '../context/AuthContext';
import { calculateAttendance } from '../utils/attendance';
import AttendanceMeter from './attendance/AttendanceMeter';

function Stepper({ label, value, onChange, min = 0, max = 200 }) {
  const id = useId();
  return (
    <div>
      <label htmlFor={id} className="text-[13px] text-muted">
        {label}
      </label>
      <div className="mt-1 flex items-center rounded-[10px] border border-line bg-surface">
        <button
          type="button"
          className="grid h-10 w-10 place-items-center text-muted hover:text-ink disabled:opacity-40"
          onClick={() => onChange(Math.max(min, value - 1))}
          disabled={value <= min}
          aria-label={`Decrease ${label.toLowerCase()}`}
        >
          <Minus size={16} />
        </button>
        <input
          id={id}
          type="number"
          inputMode="numeric"
          min={min}
          max={max}
          value={value}
          onChange={(e) => {
            const next = Number.parseInt(e.target.value, 10);
            onChange(Number.isNaN(next) ? min : Math.max(min, Math.min(max, next)));
          }}
          className="h-10 w-full min-w-0 bg-transparent text-center font-display text-lg font-semibold tabular outline-none"
        />
        <button
          type="button"
          className="grid h-10 w-10 place-items-center text-muted hover:text-ink disabled:opacity-40"
          onClick={() => onChange(Math.min(max, value + 1))}
          disabled={value >= max}
          aria-label={`Increase ${label.toLowerCase()}`}
        >
          <Plus size={16} />
        </button>
      </div>
    </div>
  );
}

/** Live demo of the attendance maths, computed in the browser with the same formula as the API. */
function AttendanceDemo() {
  const [held, setHeld] = useState(40);
  const [attended, setAttended] = useState(29);
  const [target, setTarget] = useState(75);
  const targetId = useId();
  const stats = useMemo(() => calculateAttendance(Math.min(attended, held), held, target), [attended, held, target]);

  return (
    <div className="rounded-2xl border border-line bg-surface p-5 shadow-[0_24px_60px_-28px_rgba(79,70,229,0.35)] sm:p-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-muted">Try it</p>
          <p className="font-display text-lg font-semibold">Data Structures</p>
        </div>
        <span className="rounded-md bg-brand-soft px-2 py-1 text-xs font-medium text-brand">CS-201</span>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3">
        <Stepper label="Classes held" value={held} onChange={(v) => { setHeld(v); if (attended > v) setAttended(v); }} />
        <Stepper label="Classes attended" value={attended} onChange={(v) => setAttended(Math.min(v, held))} max={held} />
      </div>

      <div className="mt-4">
        <div className="flex items-center justify-between">
          <label htmlFor={targetId} className="text-[13px] text-muted">
            Attendance target
          </label>
          <span className="text-sm font-medium tabular">{target}%</span>
        </div>
        <input
          id={targetId}
          type="range"
          min={50}
          max={100}
          step={5}
          value={target}
          onChange={(e) => setTarget(Number(e.target.value))}
          className="mt-2 w-full accent-[var(--app-brand)]"
        />
      </div>

      <AttendanceMeter stats={stats} className="mt-5 border-t border-line pt-5" />
    </div>
  );
}

const MODULES = [
  { icon: ClipboardList, title: 'Assignments', text: 'Due dates, priorities and status, with overdue work flagged before it piles up.' },
  { icon: CalendarCheck, title: 'Attendance', text: 'Per-subject percentages, cancelled classes left out, and how many you can still miss.' },
  { icon: CalendarDays, title: 'Timetable and study plan', text: 'Your weekly classes, plus study sessions that never clash with them.' },
  { icon: GraduationCap, title: 'Exams', text: 'Countdowns, syllabus notes and how prepared you are for each paper.' },
  { icon: NotebookPen, title: 'Notes', text: 'Searchable notes tagged by subject, with the important ones pinned.' },
  { icon: ChartColumn, title: 'Progress', text: 'Weekly study hours against your goal, completion trends and attendance by subject.' },
];

export default function Landing() {
  const { isAuthenticated } = useAuth();
  return (
    <div className="min-h-screen bg-bg">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5 sm:px-6 lg:px-8">
        <Logo />
        <nav className="flex items-center gap-1 sm:gap-2" aria-label="Account">
          <ThemeToggle />
          {isAuthenticated ? (
            <Link to="/dashboard" className="rounded-[10px] bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-strong dark:text-[#14132a]">
              Open dashboard
            </Link>
          ) : (
            <>
              <Link to="/login" className="rounded-[10px] px-3 py-2 text-sm font-medium text-muted hover:text-ink">
                Sign in
              </Link>
              <Link to="/register" className="rounded-[10px] bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-strong dark:text-[#14132a]">
                Create account
              </Link>
            </>
          )}
        </nav>
      </header>

      <main>
        <section className="mx-auto grid max-w-6xl items-center gap-12 px-4 pb-20 pt-10 sm:px-6 lg:grid-cols-[1.1fr_1fr] lg:px-8 lg:pt-16">
          <div>
            <h1 className="max-w-xl text-[40px] font-semibold leading-[1.05] sm:text-[56px]">
              Know exactly how many classes you can miss.
            </h1>
            <p className="mt-5 max-w-lg text-lg text-muted">
              StudentHub keeps your attendance, assignments, exams and study time in one place, and turns your own
              records into clear next steps for the week.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to={isAuthenticated ? '/dashboard' : '/register'}
                className="inline-flex h-11 items-center rounded-[10px] bg-brand px-5 text-[15px] font-medium text-white hover:bg-brand-strong dark:text-[#14132a]"
              >
                {isAuthenticated ? 'Open dashboard' : 'Create your free account'}
              </Link>
              {!isAuthenticated && (
                <Link
                  to="/login"
                  className="inline-flex h-11 items-center rounded-[10px] border border-line bg-surface px-5 text-[15px] font-medium hover:bg-subtle"
                >
                  Sign in
                </Link>
              )}
            </div>
          </div>
          <AttendanceDemo />
        </section>

        <section className="border-t border-line bg-surface">
          <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:px-8">
            <h2 className="max-w-lg text-[28px] font-semibold leading-tight">Everything a semester throws at you, in one place</h2>
            <dl className="mt-10 grid gap-x-10 gap-y-8 sm:grid-cols-2 lg:grid-cols-3">
              {MODULES.map(({ icon: Icon, title, text }) => (
                <div key={title} className="flex gap-4">
                  <Icon size={22} className="mt-0.5 shrink-0 text-brand" aria-hidden="true" />
                  <div>
                    <dt className="font-semibold">{title}</dt>
                    <dd className="mt-1 text-[15px] text-muted">{text}</dd>
                  </div>
                </div>
              ))}
            </dl>
          </div>
        </section>
      </main>

      <footer className="mx-auto flex max-w-6xl flex-col gap-2 px-4 py-8 text-sm text-muted sm:flex-row sm:justify-between sm:px-6 lg:px-8">
        <p>StudentHub, a student productivity platform.</p>
        <p>Built with React, Spring Boot and MySQL.</p>
      </footer>
    </div>
  );
}
