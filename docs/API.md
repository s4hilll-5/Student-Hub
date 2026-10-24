# API reference

Base URL: `http://localhost:8080/api` (through the Vite dev server: `http://localhost:5173/api`).

All request and response bodies are JSON. Instants are ISO-8601 UTC strings (`2026-10-20T13:00:00Z`), dates are `YYYY-MM-DD`, times are `HH:mm`.

## Authentication

Except for `/api/auth/*` and `/actuator/health`, every endpoint requires:

```
Authorization: Bearer <accessToken>
```

`/api/auth/refresh` and `/api/auth/logout` use the `studenthub_refresh` cookie instead, and need the CSRF header `X-XSRF-TOKEN: <value of the XSRF-TOKEN cookie>` (get the cookie from `GET /api/auth/csrf`).

## Errors

Every error has the same shape:

```json
{
  "timestamp": "2026-10-16T10:42:11.120Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Some fields need attention.",
  "path": "/api/assignments",
  "fieldErrors": [
    { "field": "title", "message": "Enter a title" },
    { "field": "dueAt", "message": "Choose a due date and time" }
  ]
}
```

| Status | When |
|---|---|
| 200 / 201 / 202 / 204 | Success (201 includes a `Location` header; 204 has no body) |
| 400 | Validation failure, malformed JSON, invalid enum or sort field, business rule (future attendance date, foreign `subjectId`, wrong current password) |
| 401 | Missing, invalid or expired token; wrong credentials; dead refresh cookie |
| 403 | Wrong role (`/api/admin/**`), missing/invalid CSRF token on refresh/logout |
| 404 | Record doesn't exist **or belongs to another user** |
| 405 / 415 | Wrong method / body not JSON |
| 409 | Duplicate email, duplicate subject name/code, overlapping class or study session |
| 429 | Rate limit exceeded (`Retry-After` header) |
| 500 | Unexpected server error (details are only logged) |

## Pagination

List endpoints marked *paged* accept `page` (0-based), `size` (max 100) and `sort=field,asc|desc`, and return:

```json
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 23, "totalPages": 3, "first": true, "last": false }
```

Sorting by a field that isn't listed below is a 400.

---

## Auth: `/api/auth`

| Method | Path | Body | Success | Notes |
|---|---|---|---|---|
| POST | `/register` | `{ fullName, email, password, timezone? }` | 201 `AuthResponse` + refresh cookie | 409 if the email exists |
| POST | `/login` | `{ email, password }` | 200 `AuthResponse` + refresh cookie | 401 `Invalid email or password.` |
| POST | `/refresh` | none (cookie + `X-XSRF-TOKEN`) | 200 `AuthResponse` + rotated cookie | 401 and cookie cleared if invalid |
| POST | `/logout` | none (cookie + `X-XSRF-TOKEN`) | 204, cookie cleared | |
| GET | `/csrf` | | 204 + `XSRF-TOKEN` cookie | |
| POST | `/forgot-password` | `{ email }` | 202 `{ message }` | Same response whether or not the email exists |
| POST | `/reset-password` | `{ token, newPassword }` | 200 `{ message }` | 400 if invalid/expired/used |

`AuthResponse`:

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 7, "fullName": "Asha Rao", "email": "asha@example.com", "role": "STUDENT",
    "college": null, "course": null, "semester": null, "timezone": "Asia/Kolkata",
    "attendanceTarget": 75.00, "weeklyStudyGoalHours": 10, "createdAt": "2026-10-16T09:00:00Z"
  }
}
```

Password rule: at least 8 characters, with a letter and a number, at most 72 bytes.

## Profile: `/api/users/me`

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/api/users/me` | | 200 `UserResponse` |
| PUT | `/api/users/me` | `{ fullName, college?, course?, semester? (1-12), timezone, attendanceTarget (1-100), weeklyStudyGoalHours (1-112) }` | 200 `UserResponse` |
| PUT | `/api/users/me/password` | `{ currentPassword, newPassword }` | 200 `AuthResponse` (new session; other devices signed out) |
| DELETE | `/api/users/me` | `{ password, confirmation: "DELETE" }` | 204 (all data deleted) |

## Assignments: `/api/assignments`

| Method | Path | Body / query | Success |
|---|---|---|---|
| GET | `/api/assignments` | *paged*; `search`, `subjectId`, `priority` (LOW/MEDIUM/HIGH), `status` (PENDING/IN_PROGRESS/COMPLETED), `overdue` (true/false), `dueFrom`, `dueTo` (instants). Sort: `dueAt` (default asc), `createdAt`, `updatedAt`, `title`, `priority`, `status` | 200 page |
| GET | `/api/assignments/{id}` | | 200 |
| POST | `/api/assignments` | `{ title, description?, subjectId?, dueAt, priority, status? }` | 201 |
| PUT | `/api/assignments/{id}` | same as POST (`status` null keeps the current one) | 200 |
| PATCH | `/api/assignments/{id}/status` | `{ status }` | 200 |
| DELETE | `/api/assignments/{id}` | | 204 |

Response:

```json
{
  "id": 12, "title": "Linked list implementation", "description": "With unit tests",
  "subject": { "id": 3, "name": "Data Structures", "code": "CS-301", "color": "#6366F1" },
  "dueAt": "2026-10-17T14:30:00Z", "priority": "HIGH", "status": "PENDING",
  "overdue": false, "completedAt": null,
  "createdAt": "2026-10-10T08:00:00Z", "updatedAt": "2026-10-10T08:00:00Z"
}
```

`overdue` = due before now and not completed. `completedAt` is set when the status becomes COMPLETED and cleared if it changes back.

## Subjects and attendance

| Method | Path | Body / query | Success |
|---|---|---|---|
| GET | `/api/subjects` | `belowTarget=true` to list only subjects under target | 200 list |
| GET | `/api/subjects/{id}` | | 200 |
| POST | `/api/subjects` | `{ name, code?, color? (#RRGGBB), targetPercentage? (default: profile target), priorAttended?, priorTotal? }` | 201 |
| PUT | `/api/subjects/{id}` | same | 200 |
| DELETE | `/api/subjects/{id}` | deletes its attendance records and timetable slots; unlinks assignments, exams, notes and study sessions | 204 |
| GET | `/api/subjects/{id}/attendance` | *paged* (default 20, newest first); `from`, `to` dates. Sort: `classDate`, `createdAt`, `status` | 200 page |
| POST | `/api/subjects/{id}/attendance` | `{ classDate, status: PRESENT/ABSENT/CANCELLED, notes? }` | 201 |
| PUT | `/api/attendance/{id}` | same | 200 |
| DELETE | `/api/attendance/{id}` | | 204 |

`classDate` can't be in the future (in the student's own timezone). `priorAttended` can't exceed `priorTotal`.

Subject response with calculated attendance:

```json
{
  "id": 3, "name": "Microprocessors", "code": "MP-301", "color": "#6366F1",
  "targetPercentage": 75.00, "priorAttended": 10, "priorTotal": 12,
  "attendance": {
    "present": 13, "absent": 3, "cancelled": 1, "totalCounted": 16,
    "percentage": 81.25, "target": 75.00, "meetsTarget": true,
    "classesNeededToReachTarget": 0, "targetReachable": true, "classesYouCanMiss": 1
  }
}
```

Formulas (cancelled classes excluded): `percentage = present / (present + absent) × 100`, rounded to 2 decimals; classes needed = `ceil((T·n − 100·p) / (100 − T))`; classes you can miss = `floor((100·p − T·n) / T)`. With no counted classes the percentage is `null`. A 100% target becomes unreachable (`targetReachable: false`, `classesNeededToReachTarget: null`) after any absence.

## Timetable: `/api/timetable`

| Method | Path | Body / query | Success |
|---|---|---|---|
| GET | `/api/timetable` | `day=MONDAY` (optional) | 200 list, Monday first |
| POST | `/api/timetable` | `{ subjectId, dayOfWeek, startTime, endTime, location? }` | 201 (409 if it overlaps another class that day) |
| PUT | `/api/timetable/{id}` | same | 200 |
| DELETE | `/api/timetable/{id}` | | 204 |

Back-to-back classes (one ends at 10:00, the next starts at 10:00) don't overlap.

## Study sessions: `/api/study-sessions`

| Method | Path | Body / query | Success |
|---|---|---|---|
| GET | `/api/study-sessions` | `from`, `to` dates (default: the current Monday-Sunday week; max 366 days) | 200 list |
| POST | `/api/study-sessions` | `{ title, subjectId?, sessionDate, startTime, endTime, notes? }` | 201 (409 if it overlaps another session or a class) |
| PUT | `/api/study-sessions/{id}` | same | 200 |
| PATCH | `/api/study-sessions/{id}/completion` | `{ completed: true, actualMinutes? (1-1440, default: planned) }` or `{ completed: false }` | 200 (400 for a future session) |
| DELETE | `/api/study-sessions/{id}` | | 204 |

## Exams: `/api/exams`

| Method | Path | Body / query | Success |
|---|---|---|---|
| GET | `/api/exams` | *paged* (default 20); `upcoming` (true/false), `subjectId`, `type`, `from`, `to` (instants). Sort: `examAt` (default asc, nearest first), `title`, `createdAt`, `preparationStatus` | 200 page |
| GET | `/api/exams/{id}` | | 200 |
| POST | `/api/exams` | `{ title, subjectId?, examAt, durationMinutes? (5-600), examType, preparationStatus?, location?, syllabusNotes? }` | 201 |
| PUT | `/api/exams/{id}` | same | 200 |
| DELETE | `/api/exams/{id}` | | 204 |

`examType`: QUIZ, UNIT_TEST, MIDTERM, FINAL, PRACTICAL, VIVA, OTHER. `preparationStatus`: NOT_STARTED, IN_PROGRESS, REVISING, READY. Responses include `upcoming: true|false`.

## Notes: `/api/notes`

| Method | Path | Body / query | Success |
|---|---|---|---|
| GET | `/api/notes` | *paged* (default 12); `search`, `subjectId`, `tag`, `pinned`. Pinned notes always come first; sort: `updatedAt` (default desc), `createdAt`, `title` | 200 page |
| GET | `/api/notes/tags` | | 200 `["exam prep", "theory"]` |
| GET | `/api/notes/{id}` | | 200 |
| POST | `/api/notes` | `{ title, content (≤ 50,000 chars), subjectId?, tags? (≤ 10, each ≤ 30 chars), pinned? }` | 201 |
| PUT | `/api/notes/{id}` | same | 200 |
| PATCH | `/api/notes/{id}/pin` | `{ pinned }` | 200 |
| DELETE | `/api/notes/{id}` | | 204 |

Tags are trimmed, lower-cased and de-duplicated.

## Dashboard: `/api/dashboard`

`GET /api/dashboard/summary` returns `firstName`, `today`, `timezone`, `assignments` (`total`, `pending`, `inProgress`, `completed`, `overdue`), `attendance` (`overallPercentage`, `target`, `present`, `totalCounted`, `subjects`, `subjectsBelowTarget`), `study` (`weekStart`, `weekEnd`, `minutesThisWeek`, `hoursThisWeek`, `goalHours`, `goalProgressPercent`, `completedSessions`, `plannedSessions`), `todaysClasses`, `todaysStudySessions`, `upcomingExams` (next 5), `upcomingDeadlines` (next 5 open assignments).

`GET /api/dashboard/analytics?weeks=8` (1-26) returns `assignments` (`completionRate` or null, `byStatus`, `weeklyTrend` of `{ weekStart, created, completed }`), `subjectAttendance`, `weeklyStudy` of `{ weekStart, hours, minutes, completedSessions, plannedSessions, goalHours }`, `studyGoal` and `upcomingDeadlines` (assignments and exams in the next 14 days). Weekly series always contain every week in the range, zero-filled.

## Admin: `/api/admin` (ADMIN role)

| Method | Path | Body / query | Success |
|---|---|---|---|
| GET | `/api/admin/users` | *paged*; `search` (name or email). Sort: `createdAt` (default desc), `fullName`, `email` | 200 page of `{ id, fullName, email, role, enabled, college, createdAt }` |
| GET | `/api/admin/stats` | | 200 platform totals |
| PATCH | `/api/admin/users/{id}/status` | `{ enabled }` | 200 (disabling signs the user out everywhere; admins can't disable themselves) |

## Health

`GET /actuator/health` → `{"status":"UP"}` (no details exposed).

## Trying it from PowerShell

```powershell
$base = "http://localhost:8080/api"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$auth = Invoke-RestMethod "$base/auth/register" -Method Post -WebSession $session -ContentType "application/json" `
  -Body (@{ fullName = "Asha Rao"; email = "asha@example.com"; password = "secret123"; timezone = "Asia/Kolkata" } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($auth.accessToken)" }

Invoke-RestMethod "$base/subjects" -Method Post -Headers $headers -ContentType "application/json" `
  -Body (@{ name = "Data Structures"; code = "CS-301"; targetPercentage = 75 } | ConvertTo-Json)
Invoke-RestMethod "$base/dashboard/summary" -Headers $headers
```

With curl (macOS/Linux/Git Bash):

```bash
TOKEN=$(curl -s -c cookies.txt -H 'Content-Type: application/json' \
  -d '{"email":"asha@example.com","password":"secret123"}' http://localhost:8080/api/auth/login | jq -r .accessToken)
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/assignments?overdue=true"
```
