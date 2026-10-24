# Deployment

The one rule that shapes everything: **serve the frontend and `/api` from the same site.** The refresh token is a `SameSite=Strict` cookie, and browsers only send it when the page and the API share a registrable domain (or the same origin). Two ways to get that:

- a reverse proxy in front of both (the provided Nginx config, or any host that can route `/api`), or
- Vercel rewrites from the frontend domain to the backend (`frontend/vercel.json`), so the browser only ever talks to the frontend domain.

## Option A: Docker Compose (any VPS)

```bash
cp .env.example .env        # fill DB_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET
docker compose up -d --build
```

This starts MySQL 8.4, the Spring Boot API (Flyway creates the tables on first start) and Nginx serving the React build on port 3000, with `/api` proxied to the backend. Put HTTPS in front (Caddy, Traefik, or Nginx with Let's Encrypt), then set `COOKIE_SECURE=true` in `.env` and update `CORS_ALLOWED_ORIGINS` / `FRONTEND_URL` in `docker-compose.yml` to the public URL.

Production builds without Docker:

```bash
cd backend && mvn -B package -DskipTests      # -> backend/target/studenthub-backend.jar
java -jar backend/target/studenthub-backend.jar --spring.profiles.active=prod
cd frontend && npm ci && npm run build        # -> frontend/dist (static files)
```

## Option B: managed services (free tiers are enough for a portfolio)

### 1. Database

Create a MySQL 8 database on Railway, Aiven or a similar provider. Note the host, port, database name, user and password. Use a dedicated user for the app (see `database/01_create_database.sql`). Enable TLS if offered and add `sslMode=REQUIRED` to the JDBC URL.

### 2. Backend on Render or Railway

- Deploy from the GitHub repo with root directory `backend`. Either use the provided `Dockerfile`, or a Java build with build command `mvn -B package -DskipTests` and start command `java -jar target/studenthub-backend.jar`.
- Environment variables:

```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED&serverTimezone=UTC&characterEncoding=UTF-8
DB_USERNAME=<user>
DB_PASSWORD=<password>
JWT_SECRET=<openssl rand -base64 64>
COOKIE_SECURE=true
CORS_ALLOWED_ORIGINS=https://<your-frontend>.vercel.app
FRONTEND_URL=https://<your-frontend>.vercel.app
FORWARD_HEADERS_STRATEGY=framework
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<address>
MAIL_PASSWORD=<app password>
MAIL_FROM=StudentHub <address>
```

- Health check path: `/actuator/health`.
- Hosts usually inject `PORT`; `application.yml` honours it.

### 3. Frontend on Vercel

- Import the repo, root directory `frontend`, framework preset Vite (build `npm run build`, output `dist`).
- Edit `frontend/vercel.json` and replace `YOUR-BACKEND-HOST` with the backend's host, for example `studenthub-api.onrender.com`:

```json
{ "source": "/api/:path*", "destination": "https://studenthub-api.onrender.com/api/:path*" }
```

- Leave `VITE_API_BASE_URL` unset (it defaults to `/api`), so the browser calls the Vercel domain and Vercel forwards to the backend. Cookies are then first-party.
- The second rewrite sends every client-side route (`/dashboard`, `/notes`, ...) to `index.html`.

Rate limiting note: behind Vercel's rewrite, the backend sees requests from Vercel's network. `FORWARD_HEADERS_STRATEGY=framework` makes Spring use the `X-Forwarded-For` client address.

### 4. First admin (optional)

Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` (12+ characters) on the backend once, deploy, then remove `ADMIN_PASSWORD` from the environment.

## Production checklist

- [ ] HTTPS on every public URL, `COOKIE_SECURE=true`.
- [ ] Unique `JWT_SECRET` per environment, kept in the host's secret store.
- [ ] `CORS_ALLOWED_ORIGINS` and `FRONTEND_URL` match the real frontend URL exactly (scheme + host, no trailing slash).
- [ ] Same-site setup for `/api` (proxy or rewrite).
- [ ] SMTP configured (`MAIL_ENABLED=true`). The console fallback prints reset links and must not be used in production.
- [ ] Database backups enabled. The app user has no global privileges.
- [ ] `SPRING_PROFILES_ACTIVE=prod`.
- [ ] Only one backend instance, or shared rate-limit storage (see SECURITY.md).
- [ ] Check `/actuator/health` and sign up, sign in and refresh the page once after every deploy.

## Version notes

- Spring Boot **3.5.x** is used because the brief asks for 3.x. 3.5 is the last 3.x line and its open-source support window closed around mid-2026 (3.5.16, June 2026, was the latest patch when this was written). For a long-lived deployment, plan an upgrade to Spring Boot 4.x, which means reviewing the Spring Security 7 and Jackson 3 changes.
- Java 21 LTS is configured. Java 17 works too: change `<java.version>` in `pom.xml` and the Docker base images.
- The frontend needs Node 20.19+ (Vite 8). Node 22 LTS is recommended.
