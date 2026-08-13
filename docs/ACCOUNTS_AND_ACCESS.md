# Escobar.Club — Accounts & Access Inventory

**No secret values are in this document.** This lists every external service the project actually depends
on (found by auditing the code, not guessed), what it's for, and how to get back into it. Actual passwords/
keys belong in your password manager — see [`SECRETS_INVENTORY.md`](SECRETS_INVENTORY.md) for what those
secrets are (still no values) and [`BACKUP_AND_DISASTER_RECOVERY.md`](BACKUP_AND_DISASTER_RECOVERY.md) for
the backup strategy.

**Verification note:** everything below was found by directly auditing `.env.example`, `docker-compose.prod.yml`,
`backend/pom.xml`, `frontend/package.json`, and the `Caddyfile`. Where I know an account identifier (e.g. an
email address) because it appeared directly in this project's session, I've included it. Where I do not
know something (e.g. domain registrar), it's marked **UNKNOWN — fill in yourself**.

---

## 1. GitHub

- **Purpose:** source control for the entire codebase, and CI/CD via GitHub Actions (`.github/workflows/deploy.yml`).
- **Repository:** `rishirathodmegavath-maker/Escobar.club`
- **Account:** GitHub username `rishirathodmegavath-maker`
- **Where credentials are stored:** your GitHub account password + 2FA, wherever you keep those (password
  manager, recommended). Not something this project stores anywhere.
- **How to reset/recover access:** GitHub's own account-recovery flow at github.com. If you use 2FA (you
  should), make sure you also have GitHub's recovery codes saved — losing both your 2FA device and recovery
  codes on a GitHub account with no recovery email/phone set up is a real "permanently locked out" scenario.
- **Dashboard:** https://github.com/rishirathodmegavath-maker/Escobar.club
- **What it's used for:** hosting the repo, and the one GitHub Actions secret, `DEPLOY_SSH_KEY`, that lets
  the deploy pipeline SSH into the VPS as the `deploy` user.

## 2. Hostinger (VPS hosting)

- **Purpose:** hosts the production server itself.
- **Server:** `srv1874041`, IP `200.141.14.221`
- **Account:** **UNKNOWN — fill in yourself.** I never touched Hostinger's own control panel (hPanel) —
  only SSH into the VPS it provisions. The hPanel login is a *separate* credential from SSH access, and
  you need it for things SSH can't do: rebooting from a stuck state, reinstalling the OS, viewing the VPS's
  console/VNC if SSH itself ever breaks, and managing billing.
- **Where credentials are stored:** your Hostinger account login, wherever you keep it (password manager).
- **How to reset/recover access:** Hostinger's own account recovery at hostinger.com, tied to whatever
  email/phone you signed up with.
- **Dashboard:** https://hpanel.hostinger.com (VPS management)
- **What it's used for:** the VPS that runs everything — MySQL, the backend, the frontend, Caddy, all in
  Docker containers. See [`PRODUCTION_ARCHITECTURE.md`](PRODUCTION_ARCHITECTURE.md).

## 3. Domain / DNS — escobar.club

- **Purpose:** the production domain, pointed at the Hostinger VPS.
- **Registrar / DNS provider:** **UNKNOWN — fill in yourself.** I never configured DNS in this project's
  session — by the time I was working on this server, `escobar.club` and `www.escobar.club` already
  resolved to `200.141.14.221` (confirmed via Caddy's config and live HTTPS checks). Find out who your
  registrar is (check your email for the original domain purchase, or run `whois escobar.club`) and record
  it here.
- **Where credentials are stored:** wherever you registered the domain — password manager.
- **How to reset/recover access:** the registrar's own account recovery.
- **What it's used for:** two DNS A (or equivalent) records pointing `escobar.club` and `www.escobar.club`
  at the VPS IP. Caddy (running on the VPS) handles TLS certificate issuance automatically via Let's
  Encrypt — **you do not manage TLS certificates directly**, Caddy renews them itself as long as DNS keeps
  pointing here and ports 80/443 stay open.

## 4. Google Cloud Console (OAuth)

- **Purpose:** "Sign in with Google" — creator/business login via Google OAuth.
- **What's configured:** an OAuth 2.0 Client ID, used as both `GOOGLE_CLIENT_ID` (backend, verifies the ID
  token) and `VITE_GOOGLE_CLIENT_ID` (frontend, same value — Vite only exposes browser env vars prefixed
  `VITE_`).
- **Account:** **UNKNOWN — fill in yourself** (whichever Google account created the OAuth Client ID).
- **Where credentials are stored:** the Client ID itself is not secret (it's sent to the browser) but is
  still tracked as a deployment value — see `.env` on the VPS. The Google account that owns the Cloud
  project is a separate, more sensitive credential — password manager.
- **How to reset/recover access:** Google Account recovery at accounts.google.com, or Google Cloud Console
  IAM if the project has multiple owners.
- **Dashboard:** https://console.cloud.google.com/apis/credentials
- **What it's used for:** letting users log in with their Google account instead of a password. Based on
  the codebase (`google-api-client` backend dependency, ID-token verification pattern), only a Client ID is
  needed — I found no evidence of a separate Google **Client Secret** being used, which is normal for this
  flow (ID-token verification doesn't need one). If Google Console ever shows one for this project, that
  would be an additional secret to track.

## 5. ZeptoMail (Zoho) — transactional email

- **Purpose:** currently the **active** email provider (`EMAIL_PROVIDER=zeptomail` in production) — sends
  password-reset links, email verification, and any other transactional email.
- **Account:** Zoho/ZeptoMail account — **exact login UNKNOWN to me**, but this project's email sending
  domain was confirmed to be India-region (`api.zeptomail.in`, not the `.com` default — this distinction
  matters, see `ERRORS_AND_INCIDENTS.md`).
- **Where credentials are stored:** `ZEPTOMAIL_API_KEY` in the production `.env` on the VPS.
- **How to reset/recover access:** Zoho account recovery at zoho.com, or regenerate the Mail Agent API key
  from the ZeptoMail dashboard if you still have account access but lost the key itself.
- **Dashboard:** https://www.zoho.com/zeptomail (region-specific — confirm you're on the `.in` dashboard
  for this account specifically, not `.com`)
- **What it's used for:** all transactional email in production.

## 6. Resend — transactional email (configured, not currently active)

- **Purpose:** an alternative email provider the code supports (`EMAIL_PROVIDER=resend`), but production is
  currently set to `zeptomail` instead.
- **Account:** **UNKNOWN — fill in yourself**, if one was ever created. `.env.example` implies this may
  never have been set up for real (sandbox sender `onboarding@resend.dev` is the documented default).
- **Where credentials are stored:** `RESEND_API_KEY` env var, present in config but not necessarily set to
  a real value in production right now.
- **Dashboard:** https://resend.com
- **What it's used for:** nothing right now, in production. Kept as a fallback/alternative provider in the
  code.

## 7. Apify — Instagram metrics scraper

- **Purpose:** unofficial Instagram scraper used to sync post metrics (likes/comments/views) for published
  content. Not Meta's official Graph API — same ToS caveat that implies.
- **Account:** **UNKNOWN — fill in yourself.**
- **Where credentials are stored:** `APIFY_API_TOKEN` in production `.env`.
- **How to reset/recover access:** Apify account recovery at apify.com, or regenerate the token from the
  Integrations page if you still have account access.
- **Dashboard:** https://console.apify.com/account/integrations
- **What it's used for:** `POST /api/content/{id}/metrics/sync` calls out to Apify's `instagram-scraper`
  actor to fetch current like/comment/view counts for a creator's published post.

## 8. Database (MySQL) — self-hosted, not a separate service

- **Purpose:** the application's primary datastore.
- **Where it runs:** **not** a managed cloud database — it's a `mysql:8.0` Docker container running on the
  same Hostinger VPS as everything else (see `docker-compose.prod.yml`). There is no separate "database
  provider" account to lose access to; losing the VPS means losing the database, which is exactly why
  backups matter (`BACKUP_AND_DISASTER_RECOVERY.md`).
- **Where credentials are stored:** `DB_USERNAME` / `DB_PASSWORD` / `DB_ROOT_PASSWORD` in production `.env`.
- **What it's used for:** everything — users, campaigns, content, KYC records, payouts, sessions.

## 9. S3-compatible object storage — code exists, not currently active

- **Purpose:** the codebase has a generic S3-compatible storage backend (`S3StorageService`) as a drop-in
  alternative to local disk storage, built during earlier exploration.
- **Current status:** **not in use.** `STORAGE_PROVIDER=local` in production; all `S3_*` env vars are
  empty placeholders in `.env.example`. No real bucket or account currently exists for this, as far as I
  found in the audit.
- **If activated later:** would need a real S3-compatible bucket (Cloudflare R2, Backblaze B2, AWS S3,
  etc.) and its own account/credentials — not yet a real dependency of this project.

## 10. Admin account (application-level, not infrastructure)

- **Purpose:** the platform's own built-in `ADMIN` role, for approving businesses/campaigns and reviewing
  creator KYC inside the app itself. This is **not** an external service — it's a row in your own database.
- **Account:** `admin@escobar.club`, seeded by Flyway migration `V25__admin_seed_user.sql`. The seeded
  password hash is a random, syntactically-valid placeholder that cannot be used to log in as-is — the
  account must be (and, per this session's record, was) claimed via the app's own "Forgot password" flow
  before first real use.
- **Where credentials are stored:** whatever password you set via that flow — password manager. This
  project/document does not know or store it.
- **How to reset/recover access:** the app's own "Forgot password" flow, which sends a reset link via
  whichever email provider is active (ZeptoMail currently) to `admin@escobar.club`. If that inbox is also
  lost, recovery would require direct database access (updating `users.password_hash` for that row) — see
  the deployment runbook for how to get a shell on the database.
