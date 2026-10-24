import { subjectsApi } from '../api/services';
import { useApi } from './useApi';

/** The student's subjects (with attendance stats), for dropdowns and filters. */
export function useSubjects() {
  const { data, loading, error, reload } = useApi(() => subjectsApi.list(), []);
  return { subjects: data ?? [], loading, error, reload };
}

export function subjectOptions(subjects, { includeNone = true, noneLabel = 'No subject' } = {}) {
  const options = subjects.map((s) => ({ value: String(s.id), label: s.code ? `${s.name} (${s.code})` : s.name }));
  return includeNone ? [{ value: '', label: noneLabel }, ...options] : options;
}
