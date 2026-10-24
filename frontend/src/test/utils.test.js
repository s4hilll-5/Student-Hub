import { describe, expect, it } from 'vitest';
import { attendanceVerdict, calculateAttendance, formatPercent } from '../utils/attendance';
import { calendarDaysUntil, describeDue, fromLocalInputValue, startOfWeek, toISODate, toLocalInputValue } from '../utils/date';
import { getErrorMessage, getFieldErrors } from '../utils/errors';
import { parseTags } from '../pages/notes/NoteEditorModal';

describe('calculateAttendance (mirrors the backend formula)', () => {
  it('handles zero classes without dividing by zero', () => {
    const stats = calculateAttendance(0, 0, 75);
    expect(stats.percentage).toBeNull();
    expect(stats.meetsTarget).toBe(true);
    expect(stats.classesNeededToReachTarget).toBe(0);
  });

  it('works out classes needed and classes that can be missed', () => {
    expect(calculateAttendance(2, 3, 75).classesNeededToReachTarget).toBe(1); // 3/4
    expect(calculateAttendance(20, 40, 75).classesNeededToReachTarget).toBe(40); // 60/80
    expect(calculateAttendance(33, 40, 75).classesYouCanMiss).toBe(4); // 33/44
    expect(calculateAttendance(30, 40, 75).classesYouCanMiss).toBe(0);
  });

  it('rounds the percentage to two decimals', () => {
    expect(calculateAttendance(2, 3, 75).percentage).toBe(66.67);
  });

  it('reports a 100% target as unreachable after an absence', () => {
    const stats = calculateAttendance(9, 10, 100);
    expect(stats.targetReachable).toBe(false);
    expect(stats.classesNeededToReachTarget).toBeNull();
  });
});

describe('attendanceVerdict', () => {
  it('describes each situation in plain language', () => {
    expect(attendanceVerdict({ totalCounted: 0 }).text).toBe('No classes recorded yet');
    expect(attendanceVerdict(calculateAttendance(2, 3, 75)).text).toBe('Attend the next 1 class to reach 75%');
    expect(attendanceVerdict(calculateAttendance(33, 40, 75)).text).toBe('You can miss 4 classes and stay on target');
    expect(attendanceVerdict(calculateAttendance(30, 40, 75)).tone).toBe('warning');
    expect(attendanceVerdict(calculateAttendance(9, 10, 100)).text).toBe("100% can't be reached any more");
  });

  it('formats percentages and dashes for missing values', () => {
    expect(formatPercent(null)).toBe('—');
    expect(formatPercent(81.25, 2)).toBe('81.25%');
    expect(formatPercent(80, 1)).toBe('80%');
  });
});

describe('date helpers', () => {
  it('round-trips datetime-local values through UTC instants', () => {
    const iso = fromLocalInputValue('2026-10-20T18:30');
    expect(toLocalInputValue(iso)).toBe('2026-10-20T18:30');
  });

  it('starts weeks on Monday', () => {
    expect(toISODate(startOfWeek(new Date(2026, 9, 18)))).toBe('2026-10-12'); // Sunday -> Monday before
    expect(toISODate(startOfWeek(new Date(2026, 9, 12)))).toBe('2026-10-12');
  });

  it('describes deadlines relative to now', () => {
    const now = new Date(2026, 9, 16, 10, 0);
    expect(describeDue(new Date(2026, 9, 14, 9, 0), now)).toBe('Overdue by 2 days');
    expect(describeDue(new Date(2026, 9, 26, 9, 0), now)).toBe('Due in 10 days');
    expect(describeDue(new Date(2026, 9, 17, 9, 0), now)).toMatch(/^Due tomorrow at/);
    expect(calendarDaysUntil(new Date(2026, 9, 19, 23, 0), now)).toBe(3);
  });
});

describe('error helpers', () => {
  it('prefers the API message and maps field errors', () => {
    const error = {
      response: {
        status: 400,
        data: { message: 'Some fields need attention.', fieldErrors: [{ field: 'title', message: 'Enter a title' }] },
      },
    };
    expect(getErrorMessage(error)).toBe('Some fields need attention.');
    expect(getFieldErrors(error)).toEqual({ title: 'Enter a title' });
  });

  it('explains network failures', () => {
    expect(getErrorMessage({ request: {} })).toMatch(/Can't reach the server/);
    expect(getErrorMessage({ response: { status: 500, data: {} } })).toMatch(/server had a problem/);
  });
});

describe('parseTags', () => {
  it('trims, lower-cases and de-duplicates', () => {
    expect(parseTags(' Exam Prep, sql ,  SQL, , joins ')).toEqual(['exam prep', 'sql', 'joins']);
  });
});
