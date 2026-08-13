# Escobar.Club — Complete Recovery After Laptop Loss

**Read this if:** your laptop is lost, stolen, or destroyed; Windows was reinstalled; or you're setting up
on a brand new machine and need to regain full control of Escobar.Club. This assumes you remember nothing
and are starting from a blank Windows machine with nothing installed.

**Before you start, be honest with yourself about one thing:** this runbook can only get you as far back as
your backups actually go. If you never backed up your SSH private key (see step 4), you will not be able to
"recover" it — you'll be generating a new one and re-authorizing it, which this guide also covers. If you
never set up a password manager, start there, today, before anything else — most of this recovery is
impossible without one.

---

## Step 0 — What you need before starting

- A working internet connection and a Windows machine.
- Access to your password manager (if you use one — if you don't, and this is your only copy of any
  credential, that is the actual emergency to resolve first).
- Access to whatever email account you used to sign up for GitHub, Hostinger, and the domain registrar —
  needed for any account-recovery flow.

## Step 1 — Install the tools you'll need

1. **Git for Windows** (includes Git Bash, which gives you a proper `ssh` client): https://git-scm.com/download/win
2. **A code editor**, if you'll be doing development again, not just administration: https://code.visualstudio.com
3. **Docker Desktop**, only if you plan to run the project locally (not required just to manage production):
   https://www.docker.com/products/docker-desktop

You do not need to install Java, Maven, or Node.js unless you intend to build/run the project locally
outside Docker — the production server runs everything in containers.

## Step 2 — Regain access to GitHub

1. Go to https://github.com/login and sign in, or use GitHub's account-recovery flow if you've lost your
   password/2FA device too (this can take longer if you also lost your 2FA recovery codes — GitHub will
   walk you through their identity-verification process).
2. Confirm you can see the repository: `https://github.com/rishirathodmegavath-maker/Escobar.club`
3. Clone it to your new machine:
   ```powershell
   git clone https://github.com/rishirathodmegavath-maker/Escobar.club.git
   cd Escobar.club
   ```
   **This alone recovers 100% of the application source code, Docker configuration, and this
   documentation.** Nothing else in this step is needed to get the code back.

## Step 3 — Regain access to your password manager

If you use one (1Password, Bitwarden, etc.), install its app/browser extension and sign in — this is where
your SSH key backup (if you made one, per `SSH_KEY_INVENTORY.md`) and every service password from
`SECRETS_INVENTORY.md`/`ACCOUNTS_AND_ACCESS.md` should already live.

If you don't have one yet: **set one up now**, before continuing. The rest of this document assumes you
have somewhere secure to put credentials as you recover/rotate them.

## Step 4 — Restore SSH access to the server

**Do you have a backed-up copy of your `rishi-personal-escobar` private key** (in your password manager, as
per `SSH_KEY_INVENTORY.md`)?

### If yes:
1. Retrieve it from your password manager and save it to `C:\Users\<you>\.ssh\escobar_personal` (create the
   `.ssh` folder if it doesn't exist).
2. Test it:
   ```powershell
   ssh -i "$env:USERPROFILE\.ssh\escobar_personal" root@200.141.14.221
   ```
3. If you land at `root@srv1874041:~#`, **you're fully back in.** Skip to Step 6.

### If no (the private key is genuinely lost):
An SSH private key cannot be reconstructed from anything else — not from its public key, not from the
server, not from anywhere. You need to generate a new key and get it authorized. This requires *some*
existing way into the server:

**Option A — Hostinger's browser-based VPS console (no SSH needed at all):**
1. Log into Hostinger's hPanel: https://hpanel.hostinger.com (recover this account first if needed — see
   `ACCOUNTS_AND_ACCESS.md`).
2. Find your VPS and open its browser-based terminal/console (Hostinger provides this specifically for
   situations where SSH access is unavailable).
3. From that console, you're already logged in as root — proceed to add a new key (below) without needing
   any existing SSH credential at all.

**Option B — if the GitHub Actions `deploy` key still works and you're willing to use it temporarily:**
This gets you onto the server as the low-privilege `deploy` account, not root, so you'd still need to `su`
or otherwise escalate — generally messier than Option A. Prefer Option A if it's available to you.

**Once you have a root shell (via either option), add your new key:**
1. On your new laptop, generate a fresh key pair:
   ```powershell
   ssh-keygen -t ed25519 -C "rishi-personal-escobar-2" -f "$env:USERPROFILE\.ssh\escobar_personal"
   ```
   (Press Enter through the passphrase prompts if you want no passphrase — for your *personal* key, a
   passphrase is fine and recommended, unlike the CI key which specifically must not have one.)
2. Display the **public** key so you can copy it:
   ```powershell
   type "$env:USERPROFILE\.ssh\escobar_personal.pub"
   ```
3. In your root shell on the server (via the Hostinger console), append it — do not overwrite the file:
   ```bash
   echo "PASTE_THE_PUBLIC_KEY_LINE_HERE" >> /root/.ssh/authorized_keys
   ```
4. From your laptop, confirm the new key works:
   ```powershell
   ssh -i "$env:USERPROFILE\.ssh\escobar_personal" root@200.141.14.221
   ```
5. **Immediately back up this new private key to your password manager** — this is exactly the step that
   was missed last time.
6. Optional cleanup: once confirmed working, you can remove the old (now-permanently-unusable) key's line
   from `/root/.ssh/authorized_keys` — it's harmless to leave, but tidier to remove.

## Step 5 — Regain access to GitHub Actions secrets

You already have GitHub access from Step 2. If the CI deploy key (`DEPLOY_SSH_KEY`) also needs restoring
(only necessary if it's also lost — check `SSH_KEY_INVENTORY.md` for whether you have a backup of
`escobar_ci_nopass`):

- **If you have a backup of it:** go to the repo → Settings → Secrets and variables → Actions →
  `DEPLOY_SSH_KEY` → paste it back in.
- **If you don't:** generate a fresh passphrase-less key, add its public half to the `deploy` user's
  `authorized_keys` on the server (same append pattern as Step 4, but to
  `/home/deploy/.ssh/authorized_keys` instead of root's), then update the GitHub secret with the new
  private key.

## Step 6 — Confirm production is actually healthy

From your laptop:
```powershell
curl.exe -s -o NUL -w "site: %{http_code}`n" https://escobar.club/
curl.exe -s -o NUL -w "api: %{http_code}`n" https://escobar.club/api/campaigns
```
Both should say `200`. If they don't, see `DEPLOYMENT_RUNBOOK.md` for how to check container status and
logs — the application itself likely survived even a total laptop loss, since it runs independently on the
VPS.

## Step 7 — Restore your `.env` and application secrets, if the VPS itself was also lost

This is a much bigger scenario than "lost my laptop" — it means the server itself is gone, not just your
access to it. If that's what happened:

1. Provision a new VPS (Hostinger, or wherever you choose).
2. Point DNS at its new IP (needs your domain registrar access — see `ACCOUNTS_AND_ACCESS.md`).
3. Clone the repo onto it, matching the path convention (`/opt/escobar-club/app`) so the existing deploy
   scripts work without editing.
4. Reconstruct `.env` — see `SECRETS_INVENTORY.md` for the full list of every variable name needed. If you
   backed up `.env` itself (recommended in `BACKUP_AND_DISASTER_RECOVERY.md`), restore it directly. If not,
   you'll need to re-pull or regenerate each value from its respective service dashboard (Google Cloud,
   ZeptoMail, Apify — all listed in `ACCOUNTS_AND_ACCESS.md`).
5. Re-create the `deploy` user and SSH key setup from scratch — the exact commands used originally are
   documented in `V1_CHANGELOG.md` §2.3, with the UID-collision pitfall called out so you don't repeat it.
6. **Database and uploaded media:** restore from your offsite backup, if one exists by the time this
   happens (see `BACKUP_AND_DISASTER_RECOVERY.md` — as of this document's writing, that backup did not yet
   exist, which is the most urgent gap to close). Without a backup, this data is genuinely gone — the
   application would start from an empty database (Flyway will recreate the schema automatically on first
   boot, but with no data in it).
7. Deploy: push to `main`, or run the manual deploy script from `DEPLOYMENT_RUNBOOK.md`.

## Step 8 — Regain access to external service dashboards

Work through `ACCOUNTS_AND_ACCESS.md` top to bottom — GitHub, Hostinger, domain registrar, Google Cloud,
ZeptoMail, Apify. Each has its own account-recovery flow; none of them are recoverable "through" this
project, only through that service's own login/recovery process.

## Step 9 — Regain access to the application's own admin panel

Once the site is confirmed live (Step 6), go to `https://escobar.club/login` and use "Forgot password" for
`admin@escobar.club`. This sends a reset link via whichever email provider is active (ZeptoMail, currently)
— so this step depends on email delivery actually working, which depends on `ZEPTOMAIL_API_KEY` being
correctly restored in `.env` if you're also in the "VPS was lost" scenario from Step 7.

---

## You're done when...

- [ ] You can SSH into the server with a key you control and have backed up
- [ ] GitHub Actions can deploy successfully (test with a trivial commit or `workflow_dispatch`)
- [ ] `https://escobar.club` loads and the API responds
- [ ] You can log into the admin panel
- [ ] Every credential in `SECRETS_INVENTORY.md` and `ACCOUNTS_AND_ACCESS.md` has a current, working copy
  in your password manager — not just "it works right now," but specifically backed up somewhere that
  survives *this* happening again
