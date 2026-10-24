/**
 * The access token lives only in memory (a module variable), never in localStorage.
 * An XSS bug therefore cannot read a long-lived credential from storage, and closing
 * the tab forgets it. Sessions survive reloads through the HttpOnly refresh cookie,
 * which JavaScript cannot read at all.
 */
let accessToken = null;

export const tokenStore = {
  get: () => accessToken,
  set: (token) => {
    accessToken = token;
  },
  clear: () => {
    accessToken = null;
  },
};
