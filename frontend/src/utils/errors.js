/** Turns an Axios error into a sentence a student can act on. */
export function getErrorMessage(error, fallback = 'Something went wrong. Try again.') {
  if (!error) return fallback;
  if (error.response) {
    const { status, data } = error.response;
    if (data && typeof data.message === 'string' && data.message) return data.message;
    if (status === 401) return 'Sign in to continue.';
    if (status === 403) return "You don't have permission to do that.";
    if (status === 404) return "That item doesn't exist any more.";
    if (status === 429) return 'Too many attempts. Wait a minute and try again.';
    if (status >= 500) return 'The server had a problem. Try again in a moment.';
    return fallback;
  }
  if (error.code === 'ECONNABORTED') return 'The server took too long to answer. Try again.';
  if (error.request) return "Can't reach the server. Check your connection and try again.";
  return error.message || fallback;
}

/** { fieldName: message } from the API's fieldErrors array, for showing errors next to inputs. */
export function getFieldErrors(error) {
  const list = error?.response?.data?.fieldErrors;
  if (!Array.isArray(list)) return {};
  return list.reduce((acc, item) => {
    if (item?.field && !acc[item.field]) acc[item.field] = item.message;
    return acc;
  }, {});
}
