export const PRIORITIES = [
  { value: 'LOW', label: 'Low', tone: 'neutral' },
  { value: 'MEDIUM', label: 'Medium', tone: 'warning' },
  { value: 'HIGH', label: 'High', tone: 'danger' },
];

export const ASSIGNMENT_STATUSES = [
  { value: 'PENDING', label: 'To do', tone: 'neutral' },
  { value: 'IN_PROGRESS', label: 'In progress', tone: 'brand' },
  { value: 'COMPLETED', label: 'Completed', tone: 'success' },
];

export const ATTENDANCE_STATUSES = [
  { value: 'PRESENT', label: 'Present', tone: 'success' },
  { value: 'ABSENT', label: 'Absent', tone: 'danger' },
  { value: 'CANCELLED', label: 'Cancelled', tone: 'neutral' },
];

export const EXAM_TYPES = [
  { value: 'QUIZ', label: 'Quiz' },
  { value: 'UNIT_TEST', label: 'Unit test' },
  { value: 'MIDTERM', label: 'Midterm' },
  { value: 'FINAL', label: 'Final exam' },
  { value: 'PRACTICAL', label: 'Practical' },
  { value: 'VIVA', label: 'Viva' },
  { value: 'OTHER', label: 'Other' },
];

export const PREPARATION_STATUSES = [
  { value: 'NOT_STARTED', label: 'Not started', tone: 'danger' },
  { value: 'IN_PROGRESS', label: 'Studying', tone: 'warning' },
  { value: 'REVISING', label: 'Revising', tone: 'brand' },
  { value: 'READY', label: 'Ready', tone: 'success' },
];

/** Subject colours, chosen to stay readable as small chips in both themes. */
export const SUBJECT_COLORS = [
  '#6366F1', '#8B5CF6', '#EC4899', '#EF4444', '#F59E0B', '#10B981', '#0EA5E9', '#64748B',
];

export function labelOf(list, value) {
  return list.find((item) => item.value === value)?.label ?? value;
}

export function toneOf(list, value) {
  return list.find((item) => item.value === value)?.tone ?? 'neutral';
}
