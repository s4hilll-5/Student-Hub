# Verification status

This file states exactly what was checked while the project was built, how, and what still needs to be run on a machine with full internet access. Nothing here claims more than was actually executed.

Build environment: Ubuntu 24.04, OpenJDK 21.0.12, Maven 3.9.11, Node 22.22, MySQL 8.0.46, Chromium (Playwright 1.56). That environment **could not reach Maven Central**, so Spring Boot's dependencies could not be downloaded.

## Verified

| What | How | Result |
|---|---|---|
| Flyway migration `V1__create_schema.sql` | Applied to a real MySQL 8.0.46 server | All 11 tables created |
| Database constraints and cascades | 18 SQL checks on MySQL 8.0.46: case-insensitive unique email, every CHECK constraint (targets, semester, attended ≤ held, negative counts, time ranges, minutes), ENUM rejection, per-user unique subject names, subject delete cascades attendance and timetable but sets assignments to NULL, account delete removes all of a user's data and leaves other users untouched | 18/18 pass |
| Least-privilege DB user | Ran `database/01_create_database.sql`, then applied the migration **as `studenthub_app`**; tried to create another database as that user | Schema created; other database refused (`ERROR 1044`) |
| `LIKE` escaping used by search | Ran the generated pattern with `ESCAPE '!'` on MySQL | `50%` matches literally; `!` and `_` escaped correctly |
| Attendance maths (`AttendanceCalculator`) | Compiled and run with plain `javac`/`java` (it has no Spring dependencies): 16 cases plus a brute-force comparison over 10,086 combinations | All pass |
| Backend internal consistency | Every backend and test source compiled with `javac` without the Spring jars, then compiler errors were filtered to those *not* caused by the missing libraries. This catches wrong method names, record fields, constructor arguments and types between the project's own classes | No internal errors left |
| Frontend production build | `npm run build` (Vite 8) | Builds cleanly |
| Frontend unit and component tests | `npm test` (Vitest 5, Testing Library) | 23/23 pass |
| Frontend in a real browser | Playwright + Chromium against the production build, with `/api` answered by an in-memory mock that returns the same JSON shapes as the backend DTOs. 28 checks: landing demo, login/register validation, API error display, protected-route redirect, sign-in flow, dashboard "Done", overdue highlighting, assignment create and search, quick attendance logging, tag filter, chart table views, dark theme, no horizontal scrolling at 390 px on every page, no JavaScript errors | 28/28 pass; screenshots in `docs/screenshots/` |
| Colour palette for charts | dataviz palette validator (colour-vision deficiency separation, contrast) in light and dark themes | Pass |

## Not yet verified (run these first)

| What | Command | Why it wasn't run |
|---|---|---|
| Backend compiles against Spring Boot 3.5.16 | `cd backend; mvn -B verify` | Maven Central was blocked |
| Backend test suite (JUnit, Mockito, MockMvc, `@DataJpaTest`: 63 test methods, 71 cases with parameterised rows) | `mvn test` | Same |
| Hibernate `ddl-auto=validate` accepts the Flyway schema | Start the backend against MySQL once | Same |
| End-to-end frontend ↔ real backend | Start both and follow the manual checklist in TESTING.md, or run the Postman collection | Same |
| Docker images and `docker-compose.yml` | `docker compose up --build` | No Docker daemon was available |
| Deployment to Render/Railway/Vercel | See DEPLOYMENT.md | Not deployed |
| SMTP email delivery | `MAIL_ENABLED=true` with real credentials | No SMTP account |

The included GitHub Actions workflow (`.github/workflows/ci.yml`) runs `mvn verify`, applies the migration to a MySQL service container, and runs the frontend tests and build. Pushing the repository to GitHub is the quickest way to run the unverified backend steps.

If `mvn test` reports a failure, paste the first error. Because the backend's internal references are already consistent, any remaining issue is most likely a small library API detail and quick to fix.
