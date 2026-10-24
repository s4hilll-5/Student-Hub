import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Loads data with loading/error state and a reload function.
 *   const { data, loading, error, reload } = useApi(() => notesApi.list(params), [params]);
 * Responses from superseded requests are ignored, so fast filter changes never show stale data.
 */
export function useApi(fetcher, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const requestId = useRef(0);
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  const load = useCallback(async ({ silent = false } = {}) => {
    const id = ++requestId.current;
    setState((prev) => ({ ...prev, loading: !silent || prev.data === null, error: null }));
    try {
      const data = await fetcherRef.current();
      if (id === requestId.current) setState({ data, loading: false, error: null });
      return data;
    } catch (error) {
      if (id === requestId.current) setState((prev) => ({ ...prev, loading: false, error }));
      return undefined;
    }
  }, []);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  const setData = useCallback((updater) => {
    setState((prev) => ({ ...prev, data: typeof updater === 'function' ? updater(prev.data) : updater }));
  }, []);

  return {
    data: state.data,
    loading: state.loading,
    error: state.error,
    reload: () => load({ silent: true }),
    setData,
  };
}
