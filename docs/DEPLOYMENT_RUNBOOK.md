# Escobar.Club — Deployment Runbook

Written assuming you're a beginner at server administration. Every command below is one you can copy-paste
directly. Where a command is risky, it's explicitly called out **before** it, not after.

---

## 1. Connecting to the VPS with your personal key

From PowerShell on your laptop:

```powershell
ssh -i "$env:USERPROFILE\.ssh\escobar_personal" root@200.141.14.221
```

(Replace `escobar_personal` with whatever filename you actually saved your `rishi-personal-escobar` private
key as.)

You'll land at a prompt like:
```
root@srv1874041:~#
```
That means you're now typing commands **on the server**, not your laptop. Everything from here until you
type `exit` runs on the VPS.

## 2. Checking Docker

```bash
docker ps
```
Shows all 4 running containers (`mysql`, `backend`, `frontend`, `caddy`). Healthy output looks like this —
every container should say `Up` (and `mysql` should say `(healthy)`):
```
NAME             IMAGE            STATUS
app-backend-1    app-backend      Up X minutes
app-caddy-1      caddy:2-alpine   Up X minutes
app-frontend-1   app-frontend     Up X minutes
app-mysql-1      mysql:8.0        Up X minutes (healthy)
```

If a container is missing or restarting repeatedly, check its logs (safe, read-only):
```bash
docker logs app-backend-1 --tail 100
docker logs app-caddy-1 --tail 100
```

## 3. Checking the application itself

From your own laptop (not the VPS), these confirm the site is actually reachable from the outside:

```powershell
curl.exe -s -o NUL -w "site: %{http_code}`n" https://escobar.club/
curl.exe -s -o NUL -w "api: %{http_code}`n" https://escobar.club/api/campaigns
curl.exe -s -o NUL -w "login: %{http_code}`n" https://escobar.club/login
```
All three should print `200`. Anything else means something's wrong — check `docker ps` and `docker logs`
on the server next.

## 4. How GitHub Actions deploys (the normal path)

You don't need to do anything for this — it's automatic. Any push to the `main` branch on GitHub triggers
`.github/workflows/deploy.yml`, which SSHes into the VPS as the `deploy` user and runs the same deploy
script described below. You can watch it run at:
`https://github.com/rishirathodmegavath-maker/Escobar.club/actions`

## 5. Manual deploy (if CI fails or you need to deploy without pushing new code)

This is the exact script CI runs, done by hand as the `deploy` user (never as `root` directly — `deploy`
has intentionally limited permissions, which is the whole point of it existing):

```bash
sudo -u deploy -H bash -c '
set -e
cd /opt/escobar-club/app
git fetch origin main
git reset --hard origin/main
docker compose -f docker-compose.prod.yml up --build -d --force-recreate
docker image prune -f
'
```

**What each line does:**
- `git fetch` + `git reset --hard origin/main` — makes the server's copy of the code exactly match GitHub's
  `main` branch, discarding any local changes on the server (there shouldn't be any — the server is not
  where you edit code).
- `docker compose ... up --build -d --force-recreate` — rebuilds the Docker images from the current code
  and restarts every container with the new build. This takes a few minutes.
- `docker image prune -f` — deletes old, now-unused Docker images to free disk space. Safe; does not touch
  running containers or volumes.

**This actually redeploys production.** Only run it when you mean to.

## 6. Safely restarting the application (no code changes)

If containers are just misbehaving and you don't need to deploy new code:

```bash
sudo -u deploy docker compose -f /opt/escobar-club/app/docker-compose.prod.yml restart
```

This restarts all 4 containers in place, using whatever image/config they already have — much faster than
a full redeploy, and doesn't touch the database's data (the volume persists independently of the
container).

## 7. Commands that are safe to run any time

```bash
docker ps                                                    # see what's running
docker logs <container-name> --tail 100                      # read recent logs
docker compose -f /opt/escobar-club/app/docker-compose.prod.yml config --quiet   # validate config, no changes
df -h /                                                       # check disk space
docker system df                                              # check Docker's disk usage
sudo -u deploy git -C /opt/escobar-club/app log -1            # see what commit is currently deployed
cat /root/.ssh/authorized_keys                                # see what SSH keys can log in as root
cat /home/deploy/.ssh/authorized_keys                         # see what SSH keys can log in as deploy
```
None of these change anything — read freely.

## 8. Commands that should NOT be run without understanding them first

- **`docker compose down`** — stops and *removes* all containers (not just stops them). Named volumes
  (the database) survive this, but it takes the whole site offline until you bring it back up, and it's
  easy to forget a step on the way back up. Prefer `restart` (§6) for routine issues.
- **`docker system prune -a`** (note the `-a`) — removes *all* unused images, not just dangling ones,
  which can force a full rebuild-from-scratch on the next deploy (slow, and briefly downloads everything
  again). The deploy script's `docker image prune -f` (no `-a`) is the safe version already built in.
- **`git reset --hard` run as `root` or with the wrong working directory** — if run outside
  `/opt/escobar-club/app`, or as the wrong user, this can discard uncommitted changes somewhere you didn't
  intend. Always `cd` and confirm `pwd` first, or use the exact script in §5.
- **Anything involving `rm -rf`** on the server — there is no undo. If you think you need to delete
  something, move it aside first (`mv` to a `.bak` name) instead, and only truly delete once you're certain.
- **Editing `/root/.ssh/authorized_keys` or `/home/deploy/.ssh/authorized_keys` by hand** — always `cat`
  the file first, and append (`>>`) rather than overwrite (`>`) unless you specifically mean to replace
  everything in it. Removing your *own* currently-connected key locks you out of future connections (your
  current session stays alive, but you can't reconnect) — see `SSH_KEY_INVENTORY.md`.
- **Changing `.env` and expecting it to take effect immediately** — it won't, until something recreates the
  containers. After editing `.env` by hand, run the `--force-recreate` command from §5 (or at minimum
  `docker compose ... up -d --force-recreate` without the git steps) or your change is silently ignored.
- **`usermod`/`chown` on `deploy` or the `/opt/escobar-club` directories** — this project already hit a
  real bug here (see `ERRORS_AND_INCIDENTS.md` #10, a UID collision). Don't touch user/group ownership in
  this area without understanding exactly which UID owns which host directory first.
