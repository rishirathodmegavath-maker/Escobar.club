# Escobar.Club — Secrets Inventory

**No secret values are in this document.** Every actual value lives in exactly two places:
`/opt/escobar-club/app/.env` on the VPS (root-readable only, `600` permissions), and the GitHub Actions
secret `DEPLOY_SSH_KEY`. Nowhere else — confirmed via `.gitignore` and full git history audit that no
secret file has ever been committed to this repository.

**Recommended: keep a second copy of every value below in a password manager**, organized to mirror this
document, so a lost/corrupted VPS doesn't mean re-discovering every credential from scratch. See
[`BACKUP_AND_DISASTER_RECOVERY.md`](BACKUP_AND_DISASTER_RECOVERY.md).

---

| Secret | Service | Purpose | Currently stored | Used by | Rotation procedure | What breaks if rotated |
|---|---|---|---|---|---|---|
| `JWT_SECRET` | Application (self) | Signs/verifies access + refresh JWTs | `.env` on VPS | Backend, on every authenticated request | Generate a new long random string, update `.env`, redeploy | **Every logged-in user is signed out immediately** — access tokens become unverifiable. Not data-destructive, just disruptive. Users just log back in. |
| `TWO_FACTOR_SECRET_KEY` | Application (self) | Encryption key for TOTP secrets stored at rest | `.env` on VPS | Backend, 2FA verification | Requires a data migration, not just an env swap — see warning below | **Breaks 2FA for every user who has it enabled** — their stored encrypted TOTP secret becomes undecryptable, locking them out of login entirely until an admin disables 2FA on their account directly in the DB. **Do not rotate this casually.** |
| `DB_PASSWORD` | MySQL (self-hosted) | App's DB connection | `.env` on VPS | Backend → MySQL | Update both the MySQL user's actual password (`ALTER USER ... IDENTIFIED BY ...` inside the DB) and `.env`, then redeploy the backend — they must change together or the backend can't connect | Backend can't reach the database until both sides are updated to match |
| `DB_ROOT_PASSWORD` | MySQL (self-hosted) | MySQL root/admin password, used by the container's own healthcheck | `.env` on VPS | The `mysql` container's init + healthcheck | Same caveat as above — must match what's actually set inside the running MySQL instance, not just the `.env` file | Healthcheck fails, Docker Compose may treat the DB as unhealthy and refuse to bring up dependent services |
| `GOOGLE_CLIENT_ID` | Google Cloud (OAuth) | Verifies "Sign in with Google" ID tokens | `.env` on VPS + baked into the frontend build (`VITE_GOOGLE_CLIENT_ID`) | Backend (verification) + frontend (initiates the OAuth flow) | Not secret in the traditional sense (it's sent to every browser), but rotating it requires a Google Cloud Console change *and* a frontend rebuild/redeploy, since Vite bakes it into the built JS at build time, not read at runtime | Google login stops working until both the new ID is set in `.env` **and** the frontend is rebuilt |
| `ZEPTOMAIL_API_KEY` | ZeptoMail (Zoho) | Sends all transactional email in production | `.env` on VPS | Backend, `EMAIL_PROVIDER=zeptomail` | Regenerate the Mail Agent API key in ZeptoMail's dashboard, update `.env`, redeploy | Password reset, email verification, and any other transactional email silently stop sending (backend would need to be checked for how it handles a rejected send — not verified in this audit) |
| `ZEPTOMAIL_API_URL` | ZeptoMail (Zoho) | Region-specific API host | `.env` on VPS | Backend | Not really a "rotation" — this is config, not a secret. But **get this wrong and email breaks silently** — this project already hit exactly this bug once (defaulted to the global `.com` host when the account is actually on `.in`). See `ERRORS_AND_INCIDENTS.md`. | Email sends start failing with 401 errors if this doesn't match the account's actual region |
| `RESEND_API_KEY` | Resend | Alternative email provider, not currently active | `.env` on VPS (may be unset/placeholder) | Backend, only if `EMAIL_PROVIDER=resend` | Regenerate at resend.com if a real value ever gets set | Nothing currently, since it's not the active provider |
| `APIFY_API_TOKEN` | Apify | Instagram metrics scraping | `.env` on VPS | Backend, on-demand metrics sync | Regenerate at console.apify.com, update `.env`, redeploy | Metrics sync (`POST /api/content/{id}/metrics/sync`) starts failing; nothing else in the app depends on it |
| `DEPLOY_SSH_KEY` | GitHub Actions | Private key CI uses to SSH into the VPS as `deploy` | GitHub repo Settings → Secrets and variables → Actions | `.github/workflows/deploy.yml` | See `SSH_KEY_INVENTORY.md` — generate a new key pair, add the public half to `deploy`'s `authorized_keys` on the server, then update this GitHub secret with the new private half | Automated deploys stop working (`git push` to `main` no longer deploys) until fixed; manual deploy via `sudo -u deploy` on the server still works as a fallback |
| Personal VPS SSH key (`rishi-personal-escobar`) | Hostinger VPS | Your own root access | Your own machine, `~/.ssh/` — **back this up**, see `SSH_KEY_INVENTORY.md` | You, for all manual server administration | Generate a new key, add its public half to `/root/.ssh/authorized_keys` while you still have access via *some* working credential, remove the old one | You lose the ability to manage the server directly (containers, `.env`, logs, everything) until restored — CI deploys would still keep working independently, since they use a completely separate key/user |
| Hostinger account password | Hostinger | hPanel login — billing, VPS console, reinstall/reboot controls | Your password manager | You, for infrastructure-level control outside SSH | Hostinger's own account settings | You lose the "outside SSH" safety net — e.g. if SSH itself somehow breaks, hPanel's browser console is often the only way back in |
| GitHub account password + 2FA | GitHub | Owns the repo and the `DEPLOY_SSH_KEY` secret | Your password manager | You, for all repo/CI management | GitHub's own account recovery | You lose control of the source code and the ability to change CI secrets — this is one of the more critical credentials in the whole stack |
| Domain registrar login | *(registrar unknown — see `ACCOUNTS_AND_ACCESS.md`)* | Controls DNS for escobar.club | Your password manager | You, for DNS changes | Registrar's own recovery flow | If DNS ever needs to change (new server IP, etc.) you'd be stuck until recovered — but day-to-day operation doesn't touch this at all |
| `admin@escobar.club` application password | Application (self) | Logs into the platform's own built-in admin panel | Your password manager (this is a normal login password, not an API key) | You, via the app's login page | The app's own "Forgot password" flow | Loses admin-panel access (approvals, KYC review) until reset via email, or via direct DB access as a last resort |

---

## Rotation notes worth remembering

- **`.env` changes alone don't redeploy anything.** The deploy pipeline does `git reset --hard` + rebuild —
  it does not read a fresh `.env` unless the containers are recreated. This project's deploy script already
  runs `--force-recreate` on every deploy specifically because of an earlier bug where `.env`-only changes
  didn't reach a running backend (see `ERRORS_AND_INCIDENTS.md`). If you hand-edit `.env` on the server
  *without* triggering a deploy, restart manually: `docker compose -f docker-compose.prod.yml up -d --force-recreate`.
- **`TWO_FACTOR_SECRET_KEY` is the one to be genuinely careful with.** Unlike the others, rotating it isn't
  just disruptive, it's destructive to existing users' 2FA setup with no automatic recovery path.
- **Frontend-baked values (`VITE_GOOGLE_CLIENT_ID`) need a rebuild, not just an env change**, since Vite
  inlines them into the built JavaScript at build time.
