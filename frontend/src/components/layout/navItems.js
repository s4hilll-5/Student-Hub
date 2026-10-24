import {
  CalendarCheck,
  CalendarDays,
  ChartColumn,
  ClipboardList,
  GraduationCap,
  LayoutDashboard,
  NotebookPen,
  Settings,
  ShieldCheck,
} from 'lucide-react';

export const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/assignments', label: 'Assignments', icon: ClipboardList },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
  { to: '/timetable', label: 'Timetable', icon: CalendarDays },
  { to: '/exams', label: 'Exams', icon: GraduationCap },
  { to: '/notes', label: 'Notes', icon: NotebookPen },
  { to: '/analytics', label: 'Progress', icon: ChartColumn },
];

export const SECONDARY_NAV = [{ to: '/settings', label: 'Settings', icon: Settings }];

export const ADMIN_NAV = { to: '/admin', label: 'Admin', icon: ShieldCheck };
