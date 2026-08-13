# Escobar.Club — Errors & Incidents Log

Every incident below is marked with my confidence in it: **✅ Verified** (I directly witnessed and can cite
exact evidence — commands run, output seen) or **📋 From session record** (recorded earlier in this
project's ongoing session, before the point I can cite raw tool output from — I'm confident this happened
as described, but can't re-paste the original evidence).

---

## 1. Accidental production test-data deletion ("Bot 2" incident)

- **Date:** 📋 prior to 2026-08-13 (exact date not preserved in the record available to me)
- **Symptom:** a QA background agent, instructed to "delete every test account/campaign/content/
  application/KYC record/draft you created," interpreted this far too broadly and deleted **all**
  pre-existing production data down to just the admin account — including a real user's business + creator
  account pair with real campaign, content, KYC, and payout data.
- **Cause:** overly broad agent instructions combined with insufficient scoping/guardrails on what a QA
  agent was allowed to delete in a production database.
- **Impact:** near-total data loss across all tables except `users` (admin only survived).
- **Resolution:** recovered via MySQL binlog decoding (`binlog_format=ROW`, `binlog_row_image=FULL` was
  already enabled) — `mysqlbinlog -vv --base64-output=DECODE-ROWS` to reconstruct the lost rows. All
  **database rows** were recovered. **Uploaded media files were not** — binlogs only record row data, not
  file bytes, so the business's logo, the creator's profile picture, the KYC document photo, and a content
  image were permanently lost. The affected user was informed and accepted the loss of those specific
  files.
- **Current status:** ✅ resolved (data recovered), but the underlying gap (no independent backup beyond
  the binlog) is still open — see `BACKUP_AND_DISASTER_RECOVERY.md`.
- **Lessons learned:** this incident is the direct reason every subsequent AI-agent task in this project
  was given much stricter rules — no destructive database operations without explicit human review, and a
  strong bias toward "report findings, let a human decide" over "fix it automatically." That posture is
  reflected throughout the rest of this changelog.

## 2. MySQL binlog recovery (the mechanism used to resolve #1)

- **Date:** 📋 same incident as #1
- **What happened:** `mysqlbinlog` is **not included** in the official `mysql:8.0` Docker image — had to
  install `mysql-server-core-8.0` (not just `mysql-client`, which only provides `mysql-client-core-8.0`
  without the binlog tool) directly on the host to get the binary needed to decode the binlogs.
- **Current status:** ✅ resolved, and the technique is documented here so it doesn't need to be
  re-discovered under pressure next time.
- **Note for the future:** the binlog is a 30-day rolling window, not a real backup. It's what saved this
  project once — it should not be relied on as the *only* recovery mechanism going forward.

## 3. SSH password authentication silently enabled despite apparent intent to disable it

- **Date:** ✅ 2026-08-13 (this project's session)
- **Symptom:** identified during a security audit as a Critical finding — SSH password authentication
  appeared to be disabled in config but was actually still active, under active brute-force traffic from
  the public internet.
- **Cause:** `/etc/ssh/sshd_config.d/50-cloud-init.conf` set `PasswordAuthentication yes`, and it sorted
  alphabetically *before* `60-cloudimg-settings.conf` which set `PasswordAuthentication no`. sshd's
  `Include` directive uses first-match-wins semantics across drop-in files processed in alphabetical order
  — so the `50-` file's `yes` won, silently overriding the apparent intent of the `60-` file.
  **General lesson:** never assume a later-processed config file wins in sshd's `Include` chain — verify
  the *effective* config, not just what you last edited.
- **Impact:** password-based brute-force login attempts against `root` were possible, despite the operator
  presumably believing this was already disabled.
- **Resolution:** edited `/etc/ssh/sshd_config` directly (not a new drop-in, to avoid ambiguity about which
  file would win) and set `PermitRootLogin prohibit-password` at line 130. Verified with `sshd -t` (syntax
  check) then `sshd -T` (effective config) *before* restarting the service — then confirmed both that
  key-based login still worked and that password auth was now actually rejected, immediately after restart.
- **Current status:** ✅ fixed and verified at the time. ⚠️ Cannot be re-verified now — the assistant that
  fixed this lost SSH access shortly after (see incident #9). Re-check periodically: `sshd -T | grep -i permitrootlogin`.

## 4. Critical file-upload content-type bypass (stored XSS)

- **Date:** ✅ 2026-08-13
- **Symptom:** `POST /api/media/upload` only checked the client-supplied `Content-Type` header. An HTML
  file uploaded and labeled `image/png` was served back as real `text/html` from the app's own origin —
  live-proven during the audit with an actual proof-of-concept file.
- **Cause:** no server-side verification of actual file content, only trust in an attacker-controlled
  request header.
- **Impact:** a classic stored-XSS-via-upload vector — any authenticated user could have hosted
  attacker-controlled HTML/JS on the app's own trusted origin.
- **Resolution:** added `UploadValidator` (magic-byte signature verification for PNG/JPEG/GIF/WEBP/PDF/
  MP4/MOV/WEBM), applied to both storage backends. Content-type and file extension are now always derived
  from the verified bytes, never from client input. Commit `d58beff`.
- **Current status:** ✅ deployed, verified via the exact PoC pattern both locally and in production
  (malicious file correctly rejected with `415`, real image correctly accepted with `201`).

## 5. Refresh token stored in `localStorage` (persistent XSS-to-account-takeover)

- **Date:** ✅ 2026-08-13
- **Symptom/cause:** both access and refresh JWTs lived in `localStorage`, readable by any JavaScript
  running on the page — meaning any XSS (including #4 above, before it was fixed) meant a full 7-day
  account takeover, not just a short-lived one.
- **Impact:** high — combined with the upload vulnerability, this was a realistic full-compromise chain.
- **Resolution:** refresh token moved to an httpOnly, `Secure`, `SameSite=Strict` cookie scoped to
  `/api/auth`. Access token stays in `localStorage` (short-lived, lower risk, needed for the existing
  `Authorization: Bearer` pattern). Commit `d58beff`.
- **Current status:** ✅ deployed, verified end-to-end (login → refresh → logout) both locally and in
  production.

## 6. Secure cookie flag missing in production despite real HTTPS

- **Date:** ✅ 2026-08-13 — self-discovered during verification of #5, not reported by an external audit
- **Symptom:** after deploying the httpOnly-cookie fix, the `Set-Cookie` header had no `Secure` attribute
  even though the site is genuinely served over HTTPS.
- **Cause:** `frontend/nginx.conf` had `proxy_set_header X-Forwarded-Proto $scheme;` — nginx's own `$scheme`
  variable reflects its *internal* connection (plain HTTP, from Caddy over the Docker network), not the
  original client's scheme. This silently overwrote the correct header Caddy had already set upstream, so
  the backend's `isSecure()` check always saw `http`.
- **Impact:** the refresh-token cookie would have been sent over plain HTTP if it were ever intercepted at
  a network layer that stripped HTTPS — undermining part of the point of fix #5.
- **Resolution:** changed to `proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;` (pass through
  the upstream value instead of clobbering it). Commit `4371cbf`.
- **Current status:** ✅ fixed and verified live.
- **Lesson:** in any multi-hop reverse-proxy chain, every hop must be checked individually for header
  passthrough — a fix at the outermost layer (Caddy) doesn't help if a layer behind it (nginx) undoes it.

## 7. ZeptoMail emails not sending (wrong regional API host)

- **Date:** ✅ 2026-08-13
- **Symptom:** after wiring up a real ZeptoMail API key, test emails still weren't arriving; direct `curl`
  testing against the API returned `401 Invalid API Token`.
- **Cause:** the backend had `https://api.zeptomail.com` hardcoded — the global/US default. This
  particular account is provisioned in ZeptoMail's **India** region (`api.zeptomail.in`). The key was
  valid; the host was wrong for that specific account.
- **Impact:** all transactional email (password reset, verification) silently failed to send.
- **Resolution:** made the API URL configurable (`ZEPTOMAIL_API_URL`, defaulting to `.in` for this
  project), instead of hardcoded. Confirmed working with a real end-to-end test email.
- **Current status:** ✅ resolved, live.
- **Lesson:** never assume a `.com` default is correct for a regional API provider — always confirm the
  account's actual dashboard/region before wiring up.

## 8. Deploy pipeline not picking up config-only changes

- **Date:** 📋 prior to the detailed session record I can cite directly, but confirmed still relevant and
  addressed again during this session
- **Symptom:** two separate occasions where a deploy appeared to succeed but the running containers still
  reflected old config — once after a `Caddyfile`-only edit, once after a `.env`-only change (the backend
  container kept old environment variable values despite a rebuild).
- **Cause:** `docker compose up -d` (without `--force-recreate`) doesn't reliably detect that a
  bind-mounted file's *content* changed, or that env values changed, if the image itself didn't change —
  its change-detection isn't guaranteed to catch every case that actually requires a container restart.
- **Resolution:** the deploy script now always runs `docker compose -f docker-compose.prod.yml up --build
  -d --force-recreate` (plus `docker image prune -f` to avoid unbounded image accumulation), instead of
  trying to predict when Compose's own change-detection will or won't catch a needed restart.
- **Current status:** ✅ resolved — this is now the permanent, standing deploy behavior (see
  `.github/workflows/deploy.yml`).

## 9. GitHub Actions deploy failures during the CI-key ownership migration

- **Date:** ✅ 2026-08-13
- **Symptom:** multiple distinct failures while moving the CI deploy key from an AI-assistant-controlled
  root key to an operator-controlled `deploy`-user key.
- **Sub-incident A — stale/mismatched key:** the very first attempt to switch usernames failed because the
  `DEPLOY_SSH_KEY` GitHub secret didn't actually match either key present on the server — traced by testing
  both known keys' authentication directly and finding both worked *manually* but CI still failed,
  pointing at the secret itself being wrong/stale.
- **Sub-incident B — passphrase-protected key:** the operator's first self-generated CI key had a
  passphrase. `appleboy/ssh-action` (a Go SSH client) cannot supply a passphrase interactively, so it
  aborted *before* ever presenting a credential to the server. Diagnostic signature: the server's auth log
  showed `Connection closed by authenticating user deploy ... [preauth]` with **no** corresponding "Failed
  password"/"Failed publickey" line — the tell that the client gave up locally rather than being rejected
  by the server. Resolved by generating a new, passphrase-less key dedicated to CI.
- **Sub-incident C — unexplained failure on the portfolio-validation deploy:** after two confirmed-working
  CI runs, a third run (for commit `2c44e05`) failed at the same "Deploy over SSH" step for a reason that
  was **never diagnosed** — by this point the assistant working on this project had already lost SSH access
  (per the operator's explicit, intentional instruction) and couldn't inspect the server directly. GitHub's
  job-log API also requires repo-admin rights that weren't available. **The operator resolved it by
  deploying that commit manually** (`sudo -u deploy` running the same script CI would run) and confirmed
  production was healthy, but explicitly declined further investigation ("don't run another deployment").
- **Current status:** ⚠️ **partially unresolved.** Sub-incidents A and B are fully understood and fixed.
  **Sub-incident C's root cause is unknown.** The pipeline *had* worked twice before it, and manual deploys
  using the identical script continue to work, so the deploy mechanism itself isn't obviously broken — but
  something about the automated GitHub-Actions-initiated run specifically failed without explanation.
  **Recommendation:** the next time this pipeline is used, watch it closely, and if it fails again, get the
  actual GitHub Actions log via the web UI (requires no special permissions beyond normal repo-owner
  access) rather than relying on API access, which is what blocked diagnosis this time.

## 10. Deploy-user UID collision with the container's internal user

- **Date:** ✅ 2026-08-13
- **Symptom:** caught during verification, before it caused any real problem — `useradd` auto-assigned the
  new `deploy` system account UID/GID **1000**, and the backend Docker container's internal `appuser` (set
  up in the `Dockerfile` with no explicit UID pin) also happened to get UID 1000 in its own image build.
  Since Docker bind-mounts preserve host UID numbers, this meant the `deploy` SSH account would have had
  direct filesystem read/write access to uploaded media purely by UID coincidence — not because of any
  intended permission grant.
- **Cause:** neither `useradd` (host) nor the Dockerfile's `useradd` (image) pinned an explicit UID, so
  both independently landed on "first available," which happened to collide.
- **Impact:** would have been a minor but real over-permissioning of the CI deploy account, not a data
  breach in itself, but sloppy relative to the "least privilege" goal of creating a dedicated deploy user in
  the first place.
- **Resolution:** moved `deploy` to UID/GID 1500 (`usermod -u` / `groupmod -g`), then re-applied ownership
  of `/opt/escobar-club/app` to the corrected UID. Verified `/opt/escobar-club/media` reverted to showing
  as an unnamed UID 1000 (no longer resolving to any host account by name), and that `deploy` could still
  do everything it actually needs (`docker compose config` succeeded as a dry-run check).
- **Current status:** ✅ resolved.

## 11. KYC documents served with zero authentication

- **Date:** ✅ 2026-08-13
- **Symptom/cause:** KYC PAN card documents went through the same generic upload/serving path as public
  assets (avatars, logos, content media) — a Spring static resource handler mapped to `/media/**`, marked
  `permitAll` in `SecurityConfig`, with nginx passing the path straight through with no auth check at any
  layer.
- **Impact:** anyone who obtained a document's URL — via a screenshot, browser history, referrer header, a
  shared link, or a compromised business account — could fetch the raw PAN card photo with zero
  authentication. Filenames were random UUIDs (not brute-forceable), but that only protects against
  guessing, not against a leaked URL.
- **Resolution:** full details in `V1_CHANGELOG.md` §2.5 — private storage directory, authenticated
  streaming endpoint, ownership/relationship-based authorization, frontend blob-fetch rendering, and an
  automatic migration for any already-uploaded document. Commit `937139f`.
- **Current status:** ✅ deployed and verified live (new endpoint requires auth; checked all existing
  records, none needed migration).

## 12. Portfolio-link URL scheme injection (`javascript:` stored XSS)

- **Date:** ✅ 2026-08-13
- **Symptom/cause:** `portfolioLinks` accepted any non-blank string; the frontend's `z.string().url()`
  check gave a false sense of safety since the underlying WHATWG URL parser accepts `javascript:`, `data:`,
  and `vbscript:` as syntactically valid URLs. Confirmed directly by testing `new URL("javascript:alert(1)")`
  — it parses successfully. A malicious link would be stored and rendered as a raw `<a href>` in
  `CreatorProfileInline`, shown to any business reviewing that creator's content.
- **Impact:** real, clickable stored XSS — not just a theoretical data-URI curiosity.
- **Resolution:** backend `@Pattern` requiring `http://`/`https://`, matching frontend validation, 7 new
  tests covering valid and malicious inputs. Checked all existing production records — none affected.
  Commit `2c44e05`.
- **Current status:** ✅ deployed (manually, per incident #9 sub-incident C — the automated CI run for this
  same commit failed for an unrelated, undiagnosed reason).

## 13. Production load test — saturation at 75 concurrent users

- **Date:** ✅ 2026-08-13
- **Symptom:** a staged k6 load test (read-only public endpoints only) was clean at 10/25/50 concurrent
  users (0% errors, RPS scaling roughly linearly). At 75 concurrent users, error rate jumped to 31%, 300
  requests hit a full 10-second timeout, and — critically — **throughput dropped** (21 req/s, down from
  97.5 req/s at 50 users) rather than gradually degrading. That RPS-drops-under-more-load pattern is the
  signature of a saturated queue, not gradual slowdown.
- **Cause:** **not confirmed** — no server-side metrics access (CPU/RAM/connection-pool/logs) was available
  at the time of the test. The most likely explanation, based on reading `application.yml`, is that no
  explicit HikariCP connection-pool size is configured, meaning the app runs on Spring Boot's default
  10-connection pool while every tested endpoint is DB-bound — but this is inference from config, not a
  measured fact.
- **Impact:** the site fully recovered on its own within seconds of load stopping — no crash, no lasting
  damage, confirmed via immediate post-test health checks. Impact was contained to the duration of the
  test itself.
- **Resolution:** none applied — the test was explicitly report-only, no code or config was changed as a
  result.
- **Current status:** ⚠️ **open.** Real concurrent-user capacity is untested above 50 confirmed-stable /
  75 confirmed-failing. If real traffic could approach that range, this needs real investigation (start
  with checking/tuning `spring.datasource.hikari.maximum-pool-size`) before it becomes a real outage rather
  than a test result.

---

## Summary of what's still open

- Sub-incident C of #9 (unexplained CI failure) — root cause unknown
- #13 (load-test capacity ceiling) — root cause suspected, not confirmed, not fixed
- The standing backup gap referenced throughout — see `BACKUP_AND_DISASTER_RECOVERY.md`
