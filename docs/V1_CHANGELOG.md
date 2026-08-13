# Escobar.Club — V1 Changelog

**Purpose:** a complete record of what was built and changed, why, and where — so the project can be
understood and recovered without depending on any AI assistant's memory.

**How to read this document:**
- ✅ **Verified** — I have direct, first-hand knowledge of this (I made the change, watched it deploy, or
  read the exact diff while writing this document).
- 📋 **From commit history** — confirmed to exist via `git log`/`git show`, but I did not personally do the
  work or witness it happen, so the "why" is my best reading of the commit message and diff, not lived
  context.
- ⚠️ **Not in git** — a server-side or account-level change with no corresponding commit. Git alone will
  not show you this; it's recorded here specifically so it isn't lost.

This document was generated 2026-08-13 by auditing the actual git history, configuration files, and this
project's session record. Anything I could not verify is explicitly marked as such rather than guessed.

---

## Part 1 — Core application (pre-dates the security/deployment work below)

The platform is a Spring Boot 3 (Java 21) + React/TypeScript (Vite) creator–business partnership app. Full
architecture is in the repo's own [`README.md`](../README.md) and [`PRODUCTION_ARCHITECTURE.md`](PRODUCTION_ARCHITECTURE.md).
Below is a 📋 summary of the feature-building history from `git log`, for completeness — the detailed,
first-hand part of this changelog is Part 2.

| Commit | Date | What |
|---|---|---|
| `a7e5073` | 2026-07-13 | Initial commit — creator-business partnership platform |
| `6699632` | 2026-07-14 | Converted to campaign-based applications; added payout calculation module |
| `f2ce629` | 2026-07-14 | Reskinned frontend to a dark-sidebar design system |
| `007c0b8` | 2026-07-14 | Added pluggable object storage abstraction (local disk / Cloudflare R2) |
| `75316c2` | 2026-07-14 | Fixed prod MySQL connection (`allowPublicKeyRetrieval`) |
| `2e641d3`, `0d80c41` | 2026-07-14 | Early deploy attempt on Cloudflare Pages + Railway (superseded — see Part 2, project now runs on Hostinger VPS + GitHub Actions) |
| `f9e72d8`, `db70363` | 2026-07-30 | Removed application-approval step; added V1 requirements + profile uploads |
| `634c9ca` | 2026-08-03 | Made auth production-ready; reworked campaign lifecycle |
| `0adac1d` | 2026-08-07 | Added login lockout, 2FA (TOTP), passwordless OTP login |
| `cd59e18` | 2026-08-07 | Added Admin Panel V1 — business/campaign approval, creator KYC review |
| `f691124` | 2026-08-07 | Gated content submission/payouts behind admin-verified KYC |
| `21a380e` | 2026-08-07 | Added Signal mark brand identity + splash screen |
| `64bdc09`, `ca2a81e`, `745500c` | 2026-08-08 | Docker port binding hardening; full UI/UX redesign; real 404 page |
| `71ac0eb`, `b8e7cf1`, `35fdb4b` | 2026-08-13 | Public creator-profile access fix; server-side draft autosave; Discover page redesign |

**⚠️ Not verifiable from git alone:** the exact business logic *why* behind each of these (e.g. why payouts
were deferred, why KYC gating was added when it was) lives in commit messages only — I did not witness this
work firsthand. If you need that context later, `git show <hash>` on any commit above has the full diff and
message.

---

## Part 2 — Security & production-deployment work (✅ verified, first-hand)

This is the arc of work covered in detail by this documentation set. Every item below was either directly
performed and verified by an assistant working in this project's session, or is a direct git commit
inspected while writing this document.

### 2.1 Production infrastructure stood up

| Commit | What | Files |
|---|---|---|
| `31f175e` | Production Docker Compose + Caddy config for Hostinger deployment | `docker-compose.prod.yml`, `Caddyfile` |
| `65128f4` | Caddy switched to `escobar.club` with automatic TLS (Let's Encrypt via Caddy) | `Caddyfile` |
| `d698a2e` | GitHub Actions deploy pipeline added | `.github/workflows/deploy.yml` |
| `5294bf0` | Caddy force-restarted on every deploy so Caddyfile edits actually take effect | `.github/workflows/deploy.yml` |
| `08f85e5` | `docker compose up --build -d --force-recreate` on every deploy (plain `up -d` was silently skipping container recreation on config/`.env`-only changes) | `.github/workflows/deploy.yml` |
| `6091de9` | Raised nginx `proxy_read_timeout` on `/api/` above the Apify sync budget (Apify calls can take up to ~2 min) | `frontend/nginx.conf` |

**Status: deployed and live** as of 2026-08-13 (confirmed via public HTTP health checks at time of writing).

### 2.2 Security audit and fixes

A full 3-part security audit (backend, frontend, infra) was run against the live production site with
explicit authorization for active testing. Findings were report-only until the operator reviewed and
approved fixes. What follows is what was actually fixed and deployed.

**Critical: file-upload content-type bypass** — `POST /api/media/upload` trusted the client-supplied
`Content-Type` header, so an HTML file could be uploaded labeled `image/png` and served back as real
`text/html` from the app's own origin (live-proven during the audit — a stored-XSS vector). Fixed by adding
`UploadValidator` (magic-byte verification), applied to both `LocalStorageService` and `S3StorageService`.
**Commit:** `d58beff`. **Status: deployed.**

**Critical: SSH password authentication effectively still enabled** — ⚠️ **not in git**, server-side only.
`/etc/ssh/sshd_config.d/50-cloud-init.conf` (`PasswordAuthentication yes`) was sorting alphabetically before
`60-cloudimg-settings.conf` (`PasswordAuthentication no`), and sshd uses first-match-wins — so password auth
was silently active despite apparent intent to disable it, while under active brute-force traffic. Fixed by
editing `/etc/ssh/sshd_config` directly (line 130) to `PermitRootLogin prohibit-password`, and confirming the
conflicting drop-in no longer wins. Verified key-based login still worked, and password auth was rejected,
immediately after restart. **Status: applied and confirmed working**, but ⚠️ **I have no way to re-verify
this is still true right now** — I lost SSH access to the server on 2026-08-13 (see §2.4). Whoever manages
the server next should re-check `sshd -T | grep -i permitrootlogin` periodically.

**High: refresh token readable by JavaScript** — both access and refresh JWTs lived in `localStorage`, so
any XSS meant persistent (7-day) account takeover, not a short-lived one. Refresh token moved to an
httpOnly, `Secure`, `SameSite=Strict` cookie scoped to `/api/auth`; access token stays in `localStorage`
for the existing `Authorization: Bearer` header pattern. **Commit:** `d58beff`. **Status: deployed.**

**High: missing security headers** — added CSP (no `unsafe-inline`, the one inline script in `index.html`
is allowlisted by exact SHA-256 hash instead), HSTS, `X-Frame-Options: DENY`, `Referrer-Policy` at the Caddy
layer, which fronts both the SPA and the API. **Commit:** `d58beff` (Caddyfile changes). **Status: deployed.**

**Self-discovered during verification — Secure cookie flag missing despite real HTTPS:** `frontend/nginx.conf`
had `proxy_set_header X-Forwarded-Proto $scheme;`, where nginx's own `$scheme` is always `http` (its
internal connection from Caddy is plain HTTP over the Docker network) — silently overwriting the correct
header Caddy had already set, which meant the backend's `isSecure()` check always saw `http` and never set
the cookie's `Secure` flag in production. **Commit:** `4371cbf`. **Status: deployed.**

### 2.3 CI/CD deploy-key ownership migration (2026-08-13)

Full context and current key state: [`SSH_KEY_INVENTORY.md`](SSH_KEY_INVENTORY.md). Summary of what changed
and why:

1. GitHub Actions originally deployed by SSHing in **as `root`**, using a private key that had been
   generated by an AI assistant working in this project (not the operator). ⚠️ Not in git — server-side
   `authorized_keys` state.
2. Operator generated their own personal key (`rishi-personal-escobar`) for their own `root` access, added
   ⚠️ (server-side, not in git).
3. A dedicated non-root `deploy` system user was created on the VPS: member of the `docker` group only
   (no `sudo`), owns `/opt/escobar-club/app`. ⚠️ Not in git — `useradd`/`chown` run directly on the server.
   *(One self-caught issue during this step: `useradd` initially auto-assigned `deploy` the same UID (1000)
   as the backend container's internal `appuser`, which would have given the `deploy` SSH account direct
   filesystem access to uploaded media by UID coincidence. Caught during verification and fixed by moving
   `deploy` to UID/GID 1500 before anything depended on the original UID.)*
4. `.github/workflows/deploy.yml` changed to connect as `deploy` instead of `root`. **Commit:** `9e1ef67`.
5. First CI key the operator generated was passphrase-protected, which `appleboy/ssh-action` cannot use
   non-interactively (it aborts before ever presenting a credential — shows as `Connection closed by
   authenticating user deploy ... [preauth]` in the server's auth log, with *no* failed-auth entry, which is
   the diagnostic signature of this specific failure mode). Replaced with a passphrase-less key
   (`github-actions-escobar-ci`, informally "`escobar_ci_nopass`"). ⚠️ Key material itself is never in git —
   only the workflow's `username:` field changed, already covered by commit `9e1ef67`.
6. Verified end-to-end with commit `f6538b9` (an intentionally empty commit, used only to trigger a real
   CI run without touching any files) — GitHub Actions run succeeded, deployed commit confirmed live on the
   server.
7. Old keys (`claude-deploy-escobar`, `github-actions-escobar-deploy-2`) removed from `root`'s
   `authorized_keys` once the new path was proven working. ⚠️ Server-side only, not in git.

**Status: deployed and working**, confirmed via two successful GitHub Actions runs (`f6538b9`, `937139f`)
before I lost server access. A third run (for `2c44e05`) failed for a reason never diagnosed — see
[`ERRORS_AND_INCIDENTS.md`](ERRORS_AND_INCIDENTS.md) — and the operator deployed that commit manually
instead. **Recommendation: re-run the CI pipeline soon on some low-risk change to confirm it's still
reliable**, since its last real outcome was an unexplained failure.

### 2.4 Upload size limit increased

`app.storage.max-file-size-bytes` (20MB) was too small for creator-uploaded video content. Raised to 200MB
across all three layers that must agree (nginx `client_max_body_size`, Spring `multipart.max-file-size`,
the app's own storage check), plus nginx proxy timeouts extended to 300s so a 200MB upload on a slow
connection doesn't get killed mid-transfer. **Commit:** `9c7f933`. **Status: deployed.**

### 2.5 KYC document privacy fix (2026-08-13)

KYC PAN card documents were stored under the same public `/media/**` path as avatars/logos/content media,
served with **zero authentication** (Spring `permitAll` + plain nginx passthrough). Anyone who obtained a
document's URL could fetch it directly.

Fixed by: a dedicated private storage directory never registered with any static resource handler; a new
authenticated `GET /api/kyc/{creatorId}/document` endpoint gated to the creator themself, an admin, or a
business with an existing content relationship to that creator; the frontend now fetches documents as
authenticated blobs (neither `<img src>` nor `<a href>` can carry an `Authorization` header). A startup
migration moves any already-uploaded document off the old public path automatically — checked, and no real
production KYC record needed it (all existing profiles had empty/already-handled document fields at deploy
time). **Commit:** `937139f`. **Requires:** `/opt/escobar-club/media-private` to exist on the host, owned
`1000:1000` (created manually 2026-08-13, ⚠️ not in git). **Status: deployed and confirmed live** (the new
endpoint responds `401` without auth, `200` health checks pass).

### 2.6 Portfolio-link URL validation fix (2026-08-13)

`CreatorProfileUpdateRequest.portfolioLinks` accepted any non-blank string up to 500 characters. The
frontend's `z.string().url()` check gave a false sense of safety — the WHATWG URL parser it's built on
accepts `javascript:`, `data:`, and `vbscript:` as syntactically valid URLs. A malicious link would be
stored and rendered as a raw `<a href>` in `CreatorProfileInline` (shown to any business reviewing that
creator's content) — a real, clickable stored-XSS vector.

Fixed with a backend `@Pattern(regexp = "^https?://.+")` (the actual security boundary — Bean Validation
runs before the request reaches the service/DB layer), matching frontend regex for UX, and a new test suite
(`CreatorControllerTest`, 7 tests) covering both valid and malicious inputs. Checked all 5 existing creator
profiles via the public API — none had portfolio links stored, so no data migration was needed.
**Commit:** `2c44e05`. **Status: deployed** (operator confirmed manually; the automated CI run for this
commit failed for an undiagnosed reason — see §2.3 and `ERRORS_AND_INCIDENTS.md`).

### 2.7 Production load test (2026-08-13)

A staged k6 load test against read-only public endpoints only (no accounts/data created). 10, 25, and 50
concurrent users were clean (0% errors). **75 concurrent users failed hard** — 31% error rate, request
throughput actually *dropped* rather than plateaued, 300 requests hit a full 10-second timeout. Site
recovered fully and immediately once load stopped; no lasting damage. Test halted there per plan (did not
attempt 100). Suspected — **not confirmed** (no server-side metrics access) — cause: Spring Boot's default
HikariCP connection pool (10 connections), uncustomized in `application.yml`, combined with every tested
endpoint being DB-bound. **No code or config changed as a result of this test.** Full data in
[`ERRORS_AND_INCIDENTS.md`](ERRORS_AND_INCIDENTS.md).

---

## Currently open / not yet done

As of this document's writing, the following were identified but **not yet implemented**:

- **Backup strategy** — no automated database or media backup exists beyond MySQL's binlog (30-day
  retention, used once for an emergency recovery — see `ERRORS_AND_INCIDENTS.md`). Destination decision
  ("local + offsite object storage") was made but the specific provider was never chosen before this
  documentation task began.
- **Auth endpoint rate limiting** — not implemented; not discussed in detail beyond being on the list.
- **Dependency/CVE review** — not yet performed.
- **Final consolidated security audit** — not yet performed (this would be a re-audit after all the above
  fixes, to confirm nothing regressed and nothing was missed).
- **HikariCP/Tomcat pool tuning** — suspected bottleneck from the load test, not yet investigated or changed.
