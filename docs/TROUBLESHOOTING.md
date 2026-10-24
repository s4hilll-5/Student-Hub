# Troubleshooting

## Backend won't start

| Message | Fix |
|---|---|
| `JWT_SECRET is not set` / `must be Base64-encoded` / `at least 32 bytes` | Put a value generated with `openssl rand -base64 64` (or the PowerShell command in `backend/.env.example`) into `backend/.env`. |
| `Could not resolve placeholder 'DB_PASSWORD'` or `'JWT_SECRET'` | `backend/.env` wasn't found. Run the app **from the `backend` folder** (`cd backend; mvn spring-boot:run`). In IntelliJ, set the run configuration's *Working directory* to the `backend` folder, or define the variables under *Environment variables*. |
| `Access denied for user 'studenthub_app'@'localhost'` | The password in `.env` doesn't match `01_create_database.sql`, or the script wasn't run. Re-run it (after editing the password) as root. |
| `Unknown database 'studenthub'` | Run `database/01_create_database.sql`. |
| `Public Key Retrieval is not allowed` | Keep `allowPublicKeyRetrieval=true` in `DB_URL` for local MySQL 8. |
| `Communications link failure` | MySQL isn't running, or host/port are wrong. On Windows: *Services* → *MySQL80* → Start. |
| `Schema-validation: missing table` / `wrong column type` | The database schema doesn't match the entities. Use a fresh database and let Flyway create it; never create tables by hand. If you changed an entity, add a new Flyway migration. |
| `Validate failed: Migrations have failed validation` | A migration file was edited after it ran. Restore it and add a new `V2__...` file instead. On a disposable local database: `DROP DATABASE studenthub;` then re-run the setup script. |
| `Check constraint ... is violated` on startup data | MySQL older than 8.0.16 ignores CHECKs; newer versions enforce them. Upgrade to MySQL 8.0.16+. |
| `Port 8080 was already in use` | Stop the other process, or set `PORT=8081` in `.env` and `VITE_DEV_BACKEND_URL=http://localhost:8081` in `frontend/.env.local`. |
| `release version 21 not supported` | Maven is using an older JDK. Check `mvn -v`; set `JAVA_HOME` to JDK 21 (or switch `pom.xml` to 17). |

## Signing in

| Symptom | Fix |
|---|---|
| Signed out after every page reload | The refresh cookie isn't being stored. Locally, `COOKIE_SECURE` must be `false` and you must use the Vite URL (`http://localhost:5173`), not `127.0.0.1` on one side and `localhost` on the other. In production, the frontend and API must be same-site (see DEPLOYMENT.md). |
| `403 Security check failed` on refresh/logout | The `XSRF-TOKEN` cookie is missing or doesn't match. Clear the site's cookies and sign in again. If you call the API from another tool, send `X-XSRF-TOKEN` with the cookie value (Postman collection does this). |
| `429 Too many attempts` | The auth rate limit was hit. Wait for the `Retry-After` seconds, or set `RATE_LIMIT_ENABLED=false` locally. |
| Reset email never arrives | With `MAIL_ENABLED=false` the link is printed in the backend console. With SMTP: Gmail needs an *App password* (2-step verification on), port 587, STARTTLS. Check the backend log for `Could not send password reset email`. |
| "Your session has ended" right after changing the password in another tab | Expected: changing or resetting the password signs out every other session. |

## Frontend

| Symptom | Fix |
|---|---|
| `npm install` fails or Vite says Node is too old | Install Node 22 LTS (Vite 8 needs 20.19+). `node -v` to check. |
| Every API call fails with "Can't reach the server" | The backend isn't running on `VITE_DEV_BACKEND_URL` (default `http://localhost:8080`). |
| CORS errors in the console | You set `VITE_API_BASE_URL` to an absolute URL. Leave it as `/api` and let the dev proxy (or production rewrite) forward requests. If you really need cross-origin calls, add the origin to `CORS_ALLOWED_ORIGINS`. |
| Blank page after deploying to Vercel | Check `vercel.json` is in the `frontend` root and the SPA rewrite is present. |
| Dates are a day off | Set the right timezone in *Settings*. "Today" on the server follows the profile timezone. |

## Data

| Symptom | Fix |
|---|---|
| "You can't record attendance for a future date" for today's class | The profile timezone is behind your actual timezone. Fix it in *Settings*. |
| A class or study session won't save: "This overlaps with ..." | Times overlap another class on that weekday (or another session on that date). Back-to-back times are allowed. |
| Deleted a subject and its attendance disappeared | By design: attendance records and timetable slots belong to the subject. Assignments, exams, notes and study sessions are kept. |

## Tests

| Symptom | Fix |
|---|---|
| Backend tests fail to download dependencies | They need Maven Central (`repo.maven.apache.org`). Behind a corporate proxy, configure `~/.m2/settings.xml`. |
| `mvn test` uses your MySQL | It shouldn't: tests activate the `test` profile (H2). Make sure `src/test/resources/application-test.yml` exists. |
