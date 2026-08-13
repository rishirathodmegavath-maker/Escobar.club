# Escobar.Club — Backup & Disaster Recovery

**Scenario this document answers:** *if your laptop completely dies tomorrow, what exactly do you need to
recover Escobar.club?*

**⚠️ Important gap, stated plainly:** as of this document's writing, **there is no automated backup of the
database or uploaded media beyond MySQL's binlog** (which has a 30-day retention window and was only ever
used once, reactively, for an emergency recovery — see `ERRORS_AND_INCIDENTS.md`). A destination strategy
("local dumps + offsite object storage") was chosen but the specific provider was never picked, and nothing
has been automated yet. **This is the single biggest real risk in the current setup** — if the VPS disk
fails, is compromised, or Hostinger has an incident, the binlog dies with everything else and there is
currently no independent copy of the database or uploaded files anywhere.

---

## What you need, grouped by where it actually lives

### 1. Recoverable from Git / GitHub (no special backup needed)

- All application source code (backend + frontend)
- All Docker configuration (`Dockerfile`s, `docker-compose.yml`, `docker-compose.prod.yml`)
- The Caddy reverse-proxy config (`Caddyfile`)
- The GitHub Actions deploy workflow (`.github/workflows/deploy.yml`)
- Full database **schema** (Flyway migrations in `backend/src/main/resources/db/migration/`) — this
  recreates an *empty* database with the correct structure, not your actual data
- This documentation set itself

**As long as you still have GitHub access, none of the above can be lost.**

### 2. Recoverable from backups (you need to actually make these — see below)

- **Database contents** (users, campaigns, content, KYC records, payouts — everything that isn't schema)
- **Uploaded media** — public (`/opt/escobar-club/media`) and private KYC documents
  (`/opt/escobar-club/media-private`)
- **The production `.env` file** — not in git by design, exists only on the VPS
- **SSH private keys** — see `SSH_KEY_INVENTORY.md`, cannot be regenerated from anywhere if lost

### 3. Must be regenerated (not "recovered" — these get new values, not restored old ones)

- Any SSH key whose private half is lost with no backup (see `SSH_KEY_INVENTORY.md` for the exact
  procedure)
- API tokens/keys, if their account access is also lost (Apify, ZeptoMail, etc.) — otherwise these can
  just be re-copied from the provider's dashboard if you still have account access, no "regeneration"
  needed
- TLS certificates — **not something you manage at all**: Caddy issues and renews these automatically via
  Let's Encrypt as long as DNS points at the server and ports 80/443 are open. A fresh VPS with the same
  domain pointed at it will just get new certificates automatically on first boot.

### 4. Cannot be recovered at all if lost with no backup (the genuinely irreversible category)

- **SSH private keys** — cryptographically impossible to reconstruct, explained in full in
  `SSH_KEY_INVENTORY.md`. Not catastrophic on its own (you regenerate and re-authorize a new key), but if
  it's your *only* access path and you have no fallback, you're locked out until you find another way in
  (e.g. Hostinger's browser console).
- **Database contents, if the VPS is lost with no backup and the binlog is also gone** — genuinely gone.
  All user accounts, campaigns, KYC verifications, payout history — unrecoverable.
- **Uploaded media, if the VPS is lost with no backup** — same as above. This already happened once, at
  small scale, during the incident described in `ERRORS_AND_INCIDENTS.md` (a handful of files lost even
  though the *database rows* pointing to them were recovered via binlog).

---

## Recovery checklist by component

| Component | Recoverable from | Action needed |
|---|---|---|
| GitHub repository | GitHub itself | None, as long as you have GitHub account access |
| Domain / DNS | Registrar (unknown — see `ACCOUNTS_AND_ACCESS.md`) | Re-point DNS at a new VPS IP if the server itself is rebuilt elsewhere |
| VPS / Hostinger | Hostinger account | Provision a new VPS if the current one is unrecoverable; DNS then needs re-pointing |
| SSH access | Password-manager backup of `rishi-personal-escobar`, or Hostinger's browser console as a fallback | See `SSH_KEY_INVENTORY.md` |
| SSH private-key backups | You, proactively, in a password manager | **Not yet confirmed done — do this now if you haven't** |
| GitHub Actions secrets (`DEPLOY_SSH_KEY`) | GitHub itself, once you're back into the repo | Re-add if a new CI key is ever generated |
| Application `.env` | **No backup currently exists** | Reconstruct from `SECRETS_INVENTORY.md`'s list of variable names + each service's own dashboard (you'd need to re-pull or regenerate each value) |
| Database | **No independent backup currently exists** — only the 30-day MySQL binlog, which lives on the same disk as everything else | See the gap noted at the top of this document |
| Uploaded media (public) | **No backup currently exists** | Same gap |
| Uploaded media (private/KYC) | **No backup currently exists** | Same gap, and more sensitive — this is PII |
| Docker configuration | Git | None needed |
| External API credentials (Apify, ZeptoMail, Google) | Each service's own dashboard, if you still have account access to that service | Re-copy the value, no true "backup" needed as long as those accounts themselves are safe |

---

## What to actually set up (recommended next steps, not yet done)

1. **Back up your SSH private keys today** — this costs nothing and closes the single most fragile gap.
   See the exact guidance in `SSH_KEY_INVENTORY.md`.
2. **Back up the production `.env` file** — copy it into your password manager as a secure note/attachment.
   It changes rarely; even a manual "copy it whenever you touch it" habit is far better than nothing.
3. **Automate database + media backups.** The destination decision was: local dumps on the VPS *plus*
   offsite object storage (a specific provider — Cloudflare R2, Backblaze B2, etc. — was never chosen).
   Concretely, this needs:
   - A scheduled `mysqldump` (or equivalent) of the database, written to a directory outside the app's own
     Docker volumes.
   - A way to sync uploaded media (`/opt/escobar-club/media` and `/opt/escobar-club/media-private`) offsite
     too — these are not covered by a database dump at all.
   - Both pushed somewhere that survives total VPS loss — a bucket you control, in a different provider
     than Hostinger.
   - A retention policy (e.g. keep the last 7–30 daily backups) so this doesn't grow unbounded.
   - This is real, not-yet-done work — treat it as the top priority once documentation is finished.
