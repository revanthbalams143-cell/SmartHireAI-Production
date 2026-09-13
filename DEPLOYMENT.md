# Deployment Guide

## Option 1: Docker Compose
1. Configure `.env` from `.env.example`.
2. Run `docker compose up --build -d`.
3. Verify:
   - Frontend: `http://localhost:5173`
   - Backend health via API endpoints.

## Option 2: Backend JAR + Static Hosting
1. Build backend JAR with `mvn clean package`.
2. Deploy JAR to VM/container with Java 21.
3. Host frontend assets on Nginx/Apache/S3 static hosting.
4. Set `SMART_HIRE_API_BASE` globally if backend host differs.

## Production Considerations
- Restrict CORS to trusted domains.
- Store secrets in environment variables.
- Use managed PostgreSQL and backups.
- Enable HTTPS via reverse proxy.
- Monitor logs and JVM memory.

## Troubleshooting: `interviews` Table Schema Drift
If the backend logs `column i1_0.experience_level does not exist` (or a
similar "column does not exist" error for another `interviews` column),
your database was created before that field existed on `Interview.java`.
`spring.jpa.hibernate.ddl-auto=update` cannot add a NOT NULL column to a
table that already has rows, so it is skipped automatically. Run the
one-time, idempotent, data-preserving repair script once against your
existing database, then restart the backend:

```
psql -U postgres -d smarthire -f smarthire-backend/db/repair_interviews_schema.sql
```

See the comments at the top of that script for full details.



## Production hardening
- `APP_JWT_SECRET` is mandatory and must be at least 32 characters.
- Admin bootstrap is disabled by default. Only enable it with explicit secret values.
- Production recording persistence uses `RECORDING_STORAGE_PROVIDER=s3` and AWS/S3-compatible credentials.
- The frontend proxies `/api/` and `/_ai/object-detection/` so browsers do not need internal service URLs.


## Production hardening applied

- Production JWT secret is environment-only and must be at least 32 characters.
- Admin bootstrap is disabled by default; production must explicitly configure credentials only when bootstrap is required.
- Candidate-specific interview/assessment endpoints enforce authenticated user ownership, with ADMIN as the explicit override.
- Analytics PDF download is restricted to the candidate owner or recruiter/admin roles.
- Browser live AI monitoring uses the authenticated Spring Boot facade instead of direct localhost AI service calls.
- AI usage dashboard counts instrumented backend AI operations recorded in `ai_usage_events`.
- System Health is calculated from the database, configured AI health endpoints, and Gemini configuration rather than a hard-coded percentage.
- Production Render configuration deploys the five Python AI services as private services, a Spring Boot backend, the frontend proxy, and PostgreSQL.
- Production recording storage can use the included S3-compatible provider with `RECORDING_STORAGE_PROVIDER=s3`.
