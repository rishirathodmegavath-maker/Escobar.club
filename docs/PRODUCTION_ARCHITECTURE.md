# Escobar.Club — Production Architecture

## Request flow

```
User's browser
     │
     ▼
Domain (escobar.club / www.escobar.club)
     │  DNS → 200.141.14.221  (⚠️ registrar unknown — see ACCOUNTS_AND_ACCESS.md)
     ▼
┌─────────────────────────────── Hostinger VPS (srv1874041) ───────────────────────────────┐
│                                                                                             │
│   Caddy (container: app-caddy-1)                                                          │
│   - Terminates TLS (automatic via Let's Encrypt — Caddy manages this itself)               │
│   - Listens on :80 and :443                                                                │
│   - Adds security headers: HSTS, CSP, X-Frame-Options, Referrer-Policy                     │
│   - Reverse-proxies everything to the frontend container                                   │
│         │                                                                                  │
│         ▼                                                                                  │
│   Frontend (container: app-frontend-1) — nginx serving the built React SPA                 │
│   - Serves static files (index.html, JS, CSS) for everything else                          │
│   - location ^~ /api/  → proxied to backend:8080/api/                                      │
│   - location ^~ /media/ → proxied to backend:8080/media/  (PUBLIC files only — see below)   │
│         │                                                                                   │
│         ▼                                                                                   │
│   Backend (container: app-backend-1) — Spring Boot 3 (Java 21), listens on :8080 internally │
│   - REST API under /api/**                                                                  │
│   - Serves public media under /media/** (Spring static resource handler)                    │
│   - KYC documents are NOT under /media/** — served only via an authenticated                │
│     GET /api/kyc/{creatorId}/document endpoint, reading from a directory never              │
│     registered with any static handler                                                       │
│         │                                                                                    │
│         ▼                                                                                    │
│   MySQL (container: app-mysql-1) — MySQL 8, internal only, not exposed outside the           │
│   Docker network                                                                              │
│                                                                                                │
│   All 4 containers defined in docker-compose.prod.yml, on the same Docker Compose network    │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

## Volumes (what's persistent vs. ephemeral)

| Host path | Mounted into | Contents | Owned by (host UID) |
|---|---|---|---|
| `mysql_data` (named Docker volume) | `mysql` container, `/var/lib/mysql` | The actual database | Docker-managed |
| `/opt/escobar-club/app` | *(not a container mount — this is the git working tree the deploy script operates on)* | Application source + `.env` | `deploy` user (UID 1500) |
| `/opt/escobar-club/media` | `backend` container, `/app/uploads` | **Public** uploaded media (avatars, logos, campaign content) — served at `/media/**`, no auth | UID 1000 (matches the container's internal `appuser`, coincidentally — not the host `deploy` user) |
| `/opt/escobar-club/media-private` | `backend` container, `/app/uploads-private` | **Private** KYC documents — never served by any static route, only through the authenticated `/api/kyc/{id}/document` endpoint | UID 1000 (same reasoning as above) |
| `caddy_data` / `caddy_config` (named Docker volumes) | `caddy` container | TLS certificates + Caddy's internal state | Docker-managed |

**Container restarts do not lose data** — everything that matters is on the bind mounts or named volumes
above, not inside the container's own writable layer. **VPS loss loses all of it**, since none of it is
backed up offsite yet (see `BACKUP_AND_DISASTER_RECOVERY.md`).

## The deploy user

- System account `deploy`, UID/GID **1500**, home `/home/deploy`.
- Member of the `docker` group only — **not** `sudo`. This lets it run `docker compose` commands without
  root, but is not true least-privilege: Docker-group membership is functionally root-equivalent on the
  host if deliberately abused (e.g. `docker run -v /:/host ...`). It's a real improvement over a direct
  root login (no root shell credential to leak), not a hard security boundary against a malicious actor who
  already has this account.
- Owns `/opt/escobar-club/app` (the git working tree deploys operate on).
- Its `authorized_keys` should contain exactly one key — see `SSH_KEY_INVENTORY.md`.

## GitHub Actions

- Workflow: `.github/workflows/deploy.yml`
- Trigger: any push to `main`, or manual `workflow_dispatch`
- What it does: SSHes into the VPS as `deploy` (using the `DEPLOY_SSH_KEY` secret), runs
  `git fetch && git reset --hard origin/main`, then
  `docker compose -f docker-compose.prod.yml up --build -d --force-recreate`, then `docker image prune -f`.
- This is the **only** automated path from "code merged to `main`" to "live in production." There is no
  staging environment and no manual approval gate. **Confirmed:** `backend/Dockerfile` builds with
  `mvn -B clean package -DskipTests` — the deploy pipeline does **not** run the backend test suite. A
  broken test would not block a deploy; tests are a pre-merge discipline (run manually/in a separate CI
  step before pushing to `main`), not a deploy-time gate.

## External services this architecture depends on

See `ACCOUNTS_AND_ACCESS.md` for the full inventory. Summary of what talks to what:

- **Backend → Google** (OAuth ID token verification) — for "Sign in with Google"
- **Backend → ZeptoMail** — all transactional email
- **Backend → Apify** — on-demand Instagram metrics scraping
- **GitHub Actions → VPS** — the deploy pipeline (SSH)
- **User's browser → Google** — the OAuth flow itself also talks to Google directly (hence
  `https://accounts.google.com` being explicitly allowlisted in Caddy's CSP)

## What's explicitly NOT part of this architecture (yet)

- No CDN in front of Caddy — Caddy is the direct public entry point.
- No load balancer / no horizontal scaling — a single VPS runs everything.
- No staging environment — `main` branch is production.
- No offsite backup automation (see `BACKUP_AND_DISASTER_RECOVERY.md`).
- No rate limiting on auth endpoints (identified as an open item, not yet built).
- `fail2ban` is **not installed** on the VPS (confirmed absent during this session's audit) — SSH brute
  force is currently only mitigated by key-only auth (`PermitRootLogin prohibit-password`), not by
  connection-rate blocking.
