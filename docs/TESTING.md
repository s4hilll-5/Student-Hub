# Testing

## Backend (JUnit 5, Mockito, Spring Boot Test)

```powershell
cd backend
mvn test
```

Tests run against an in-memory H2 database in MySQL compatibility mode (profile `test`, `src/test/resources/application-test.yml`), so no MySQL server is needed. They use their own test-only JWT secret, BCrypt strength 4 (fast) and rate limiting off.

| Test class | Kind | Covers |
|---|---|---|
| `service/AttendanceCalculatorTest` | Unit | Zero classes, cancelled classes excluded, HALF_UP rounding, exact target comparison (74.996% is below 75%), classes needed, classes you can miss, unreachable 100% target, **brute-force cross-check of both formulas over every combination up to 30/30 for six targets**, negative counts and invalid targets rejected |
| `service/AssignmentServiceTest` | Mockito | Owner taken from the authenticated user, another user's subject rejected, another user's assignment is 404 and never deleted, `completedAt` set and cleared, overdue rules, update keeps status |
| `service/AuthServiceTest` | Mockito | Duplicate email (case-insensitive) is 409, emails normalised, only the BCrypt hash stored, unknown email in forgot-password does nothing observable, only the token hash stored, expired reset token rejected, a successful reset bumps the token version and revokes sessions |
| `security/JwtServiceTest` | Unit | Round trip, tampered token, token signed with another key, expired token, missing or weak secret prevents startup |
| `security/FixedWindowRateLimiterTest` | Unit | Blocks after the limit, separate clients, resets with the window |
| `controller/AuthIntegrationTest` | MockMvc, full context | Register sets an HttpOnly/SameSite=Strict cookie and never returns the password, duplicate email 409, per-field validation errors, identical 401 for wrong password and unknown email, protected endpoints need a valid token, refresh needs the CSRF header and rotates the cookie, **replaying a rotated refresh token revokes every session**, logout revokes, forgot-password answers identically, full reset flow (single use, old access token stops working, old password rejected), account deletion requires password and confirmation |
| `controller/OwnershipIntegrationTest` | MockMvc | Another student's assignment can't be read, updated, status-changed or deleted (404), a `userId` in the body is ignored, another student's subject can't be referenced or used for attendance, attendance records are owned through the subject, notes/exams/timetable are scoped, students get 403 on admin endpoints |
| `controller/ValidationIntegrationTest` | MockMvc | Missing fields listed, unknown enums and malformed JSON are 400, **invalid attendance counts** (attended > held, negative, target > 100), duplicate subject names 409, attendance in the future 400, timetable end before start 400, overlaps 409 but back-to-back allowed, study sessions can't clash with classes, sorting restricted to known fields, no stack traces, non-JSON body 415 |
| `controller/DashboardIntegrationTest` | MockMvc | A new account gets zeros and nulls, not errors; counts and **overdue assignments**; prior classes combined with records and cancelled ignored (81.25%), below-target filter, dashboard overall attendance |
| `repository/RepositoryTest` | `@DataJpaTest` | Overdue count query, search is owner-scoped and treats `%` literally, grouped attendance counts, overlap query, deleting a subject cascades attendance and unlinks assignments |

## Frontend (Vitest, React Testing Library)

```powershell
cd frontend
npm test
```

| File | Covers |
|---|---|
| `src/test/utils.test.js` | Client attendance maths and plain-language verdicts, date helpers (UTC round trip, Monday weeks, due wording), API error and field-error mapping, tag parsing |
| `src/test/components.test.jsx` | `ProtectedRoute` (anonymous redirect, loading state, signed-in, admin-only), `FormInput` accessibility (label, `aria-invalid`, described by the error), `ConfirmDialog` (confirm, cancel, Escape), `DataTable` empty state, `AttendanceMeter` |
| `src/test/login.test.jsx` | Login validation, successful sign-in navigates to the dashboard, server error message shown |

## Manual API testing with Postman

1. Start MySQL and the backend (`COOKIE_SECURE=false` in `backend/.env` for local HTTP).
2. In Postman: *Import* → `docs/postman/StudentHub.postman_collection.json`.
3. Check the collection variable `baseUrl` is `http://localhost:8080/api`; change `email` if you like.
4. Run folder **1. Auth**: *CSRF cookie*, then *Register* (or *Login* if the email already exists). The test script saves `accessToken` into the collection; Postman's cookie jar keeps the refresh and XSRF cookies.
5. Run folders 2 to 9 in order (or use *Run collection*). Each request has assertions on the status code, and the create requests store ids for the following ones.
6. Negative checks are included: wrong password (401), no token (401), invalid bodies (400), attended > held (400), future attendance (400), overlapping class (409), another id (404).
7. To test the reset flow with `MAIL_ENABLED=false`, run *Forgot password*, copy the token from the link printed in the backend console into *Reset password*, and send it.
8. To check access control between users, register a second email, then try `GET /api/assignments/{id}` with an id from the first user: expect 404.

## Manual UI checklist

- Register → lands on the dashboard with zeros and empty states (no errors).
- Add subjects (try attended > held: blocked), log Present/Absent/Cancelled from the Attendance page, open a subject's history, edit and delete a record; check the percentage and the "can miss / attend next" sentence update.
- Add an assignment due yesterday: it shows as overdue (red row, dashboard count). Mark it complete from the list and from the dashboard.
- Add classes, try an overlapping one (error message names the clash), plan a study session during a class (blocked), mark a past session done.
- Add exams; check the countdown, change preparation status inline, view the calendar.
- Create notes with tags, pin one, filter by tag and search.
- Progress page: switch 4/8/12 weeks, toggle each chart's table view.
- Settings: change timezone, goal and target; change password (other sessions end); toggle the theme; delete the account.
- Resize to phone width: navigation drawer, no sideways page scrolling.
