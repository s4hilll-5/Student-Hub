# Security

## Token storage strategy and trade-offs

StudentHub uses **an in-memory access token plus an HttpOnly refresh cookie**.

| | Access token | Refresh token |
|---|---|---|
| Format | Signed JWT (HMAC-SHA; the suggested 64-byte secret gives HS512, the 32-byte minimum gives HS256) | 256-bit random value (opaque) |
| Lifetime | 15 minutes | 7 days, rotated on every use |
| Where the browser keeps it | A JavaScript variable (`api/tokenStore.js`), never `localStorage` | `studenthub_refresh` cookie: `HttpOnly; Secure; SameSite=Strict; Path=/api/auth` |
| Where the server keeps it | Nowhere (stateless) | SHA-256 hash in `refresh_tokens` |
| Sent as | `Authorization: Bearer ...` | Automatically by the browser, only to `/api/auth/*` |

Alternatives considered:

| Strategy | XSS impact | CSRF exposure | Verdict |
|---|---|---|---|
| Access token in `localStorage` | Any injected script can read and exfiltrate the token and use it from anywhere until it expires | None (header-based) | Simple, but one XSS bug leaks long-lived credentials |
| Everything in cookies | Script can't read tokens, but can still make requests while the page is open | Every endpoint becomes CSRF-exposed and needs CSRF tokens | Workable, more moving parts on every request |
| **Memory + HttpOnly refresh cookie (chosen)** | Script can't read the refresh token. An attacker who gets script execution can only use the short-lived access token while the tab is open | Only the two cookie endpoints, which are CSRF-protected | Best balance for a SPA |

Residual risk, stated plainly: XSS is still serious. Malicious script running in the page can call the API as the user while the page is open. React escapes output by default and the app never renders raw HTML (`dangerouslySetInnerHTML` is not used anywhere). Add a strict Content-Security-Policy at the host serving the frontend for defence in depth.

### Refresh token rotation and theft detection

Every `POST /api/auth/refresh` revokes the presented token and issues a new one. If a token that was already rotated is presented again, it was probably copied and replayed, so **all** of that user's refresh tokens are revoked (`REUSE_DETECTED`) and they must sign in again. A 20-second grace window treats two tabs refreshing at the same moment as normal rather than as theft.

### Making old tokens stop working

- Each user has a `token_version` embedded in their JWTs (`ver` claim). Changing or resetting the password, or an admin disabling the account, increments it, so **every existing access token fails immediately**, not just after 15 minutes.
- The JWT filter loads the user on each request (a primary-key lookup) and rejects disabled accounts and stale versions.
- Password change and reset also revoke all refresh tokens. Changing the password from Settings issues a fresh session for the current tab.

## CSRF

CSRF needs a credential the browser attaches automatically. Header tokens are never attached automatically, so CSRF tokens are not needed for the bearer-authenticated API. They *are* needed for the endpoints that read the refresh cookie, and only there:

- `CookieCsrfTokenRepository` issues an `XSRF-TOKEN` cookie (readable by JavaScript, `SameSite=Strict`).
- `POST /api/auth/refresh` and `POST /api/auth/logout` require the `X-XSRF-TOKEN` header to match (double-submit). Another site can't read the cookie, so it can't forge the header. The SPA calls `GET /api/auth/csrf` once to obtain it.
- The refresh cookie is also `SameSite=Strict`, so browsers don't send it on cross-site requests at all. Two independent layers.
- Login and register require `Content-Type: application/json`, which a cross-site HTML form can't send without a CORS preflight that the CORS policy refuses.

This is configured in `SecurityConfig` with `requireCsrfProtectionMatcher`. CSRF protection is not disabled globally.

## Authorization and data ownership

- The user id comes **only** from the verified JWT (`@AuthenticationPrincipal AppUserPrincipal`). No request DTO has a `userId` field, and unknown JSON fields (like an injected `"userId"`) are ignored. This is tested.
- Every repository lookup is owner-scoped: `findByIdAndUserId`, `findByIdAndSubjectUserId`, `ownedBy(userId)` in Specifications.
- Referencing another user's subject in a request body is a 400 (`subjectId`).
- Another user's record answers **404**, the same as a non-existent id, so ids can't be probed.
- Roles: `/api/admin/**` requires `ROLE_ADMIN` in the URL rules **and** `@PreAuthorize("hasRole('ADMIN')")` on the controller. Students get 403. Public registration only creates students; the first admin comes from `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Admins see account metadata and totals, never students' academic content.

## Passwords

- BCrypt, strength 12 (`BCRYPT_STRENGTH`).
- Policy: at least 8 characters, a letter and a digit, at most 72 bytes (BCrypt ignores anything longer, so longer passwords are rejected rather than silently truncated).
- Login failures always return `401 "Invalid email or password."`, for unknown email, wrong password and disabled account alike. Spring's `DaoAuthenticationProvider` also hashes a dummy password for unknown emails so response time doesn't reveal accounts.

## Password reset and account enumeration

- `POST /api/auth/forgot-password` always returns 202 with the same message.
- For a real account: previous reset tokens are deleted, a new 256-bit token is generated, only its SHA-256 hash is stored, it expires in 30 minutes, and it can be used once.
- With SMTP enabled the email is sent asynchronously, so response time doesn't depend on whether the account exists.
- A successful reset bumps the token version and revokes every refresh token.
- With `MAIL_ENABLED=false` the link is printed to the backend log, **for development only**. Anyone with log access could reset passwords, so production must use SMTP.

## Rate limiting

`RateLimitFilter`, per client IP, fixed window:

| Endpoint | Limit |
|---|---|
| `POST /api/auth/login` | 10 per minute |
| `POST /api/auth/register` | 5 per 10 minutes |
| `POST /api/auth/forgot-password` | 5 per 15 minutes |
| `POST /api/auth/reset-password` | 10 per 15 minutes |
| `POST /api/auth/refresh` | 60 per minute |

Over the limit: `429` with `Retry-After`. Counters are in memory, which is fine for one instance. With several instances, move them to Redis (for example Bucket4j) or the API gateway. Behind a proxy, set `FORWARD_HEADERS_STRATEGY=framework` so the real client IP is used.

## Other measures

- **Secrets** come from environment variables or a git-ignored `backend/.env`. There is no default JWT secret: the app refuses to start without one of at least 256 bits.
- **CORS** allows only `CORS_ALLOWED_ORIGINS` (wildcards are dropped) with credentials, limited methods and headers.
- **SQL injection.** All database access goes through JPA with bound parameters. Search input is escaped for `LIKE` wildcards.
- **Errors.** `GlobalExceptionHandler` returns a fixed JSON shape. Stack traces, SQL and library messages are logged server-side only (`server.error.include-stacktrace=never`).
- **Response headers.** `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` on API responses, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` (Spring Security defaults plus explicit CSP). The Nginx config adds equivalents for the frontend.
- **Validation.** Every request body is a record with Bean Validation. Business rules (future attendance dates in the student's timezone, overlaps, subject ownership) are enforced in services. The database repeats the critical rules as CHECK constraints.
- **Least-privilege database user.** `database/01_create_database.sql` grants the app user rights on the `studenthub` schema only.
- **Cleanup.** Expired refresh and reset tokens are deleted nightly (`TokenCleanupJob`).

## Production checklist

- [ ] HTTPS everywhere. `COOKIE_SECURE=true` (the default; the `prod` profile forces it).
- [ ] A fresh `JWT_SECRET` per environment, stored in the host's secret manager.
- [ ] `CORS_ALLOWED_ORIGINS` and `FRONTEND_URL` set to the real frontend URL.
- [ ] Frontend and API served from the **same site** (reverse proxy or Vercel rewrite) so the `SameSite=Strict` refresh cookie is sent. See DEPLOYMENT.md.
- [ ] `MAIL_ENABLED=true` with a real SMTP account.
- [ ] `FORWARD_HEADERS_STRATEGY=framework` behind a load balancer.
- [ ] Database user without global privileges, TLS to the database if it's remote, backups enabled.
- [ ] A Content-Security-Policy on the frontend host.
- [ ] Shared rate limiting if you run more than one backend instance.
