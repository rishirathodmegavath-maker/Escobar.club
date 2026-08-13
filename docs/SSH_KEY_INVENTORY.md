# Escobar.Club — SSH Key Inventory

**No private key material is in this document, ever.** This tracks what each key is *for*, not the keys
themselves.

**⚠️ IMPORTANT — read this before trusting anything below:** the state described here is what was
**directly observed and verified** during this project's session, as of **2026-08-13, ~09:53 server time**
(when the last key removal was confirmed) and **~09:45** (when the current CI key was confirmed as the sole
entry in `deploy`'s `authorized_keys`). Since then:
- The assistant that made these changes **lost all SSH access to the server** (by design, at the operator's
  request) and cannot re-verify this state.
- At least one manual deploy has happened since (operator ran the deploy script themselves as `deploy`).

**Before relying on this table for anything important, re-verify it yourself:**
```bash
# As root (using your personal key):
cat /root/.ssh/authorized_keys
# As deploy (from root, or SSH directly if you have deploy's key):
cat /home/deploy/.ssh/authorized_keys
```

---

## Key inventory

| Filename (local, on whoever holds it) | Comment on the key | Authenticates to | Status | Used by GitHub Actions? |
|---|---|---|---|---|
| `escobar_personal` (or similar, on the operator's laptop) | `rishi-personal-escobar` | `root@200.141.14.221` | **✅ Active** — confirmed working by the operator | No |
| `escobar_ci_nopass` (private key lives only in the GitHub Actions secret `DEPLOY_SSH_KEY`) | `github-actions-escobar-ci` | `deploy@200.141.14.221` | **✅ Active** — confirmed by 2 successful CI runs; 1 later run failed for an undiagnosed reason (see `ERRORS_AND_INCIDENTS.md`) but the key itself was not implicated | **Yes — this is the only key GitHub Actions uses** |
| (first CI key generated, before `_nopass`) | `github-actions-escobar-deploy` | *was* `deploy@200.141.14.221` | **🗑️ Retired / replaced** — was passphrase-protected, which `appleboy/ssh-action` cannot use non-interactively. Removed from `deploy`'s `authorized_keys` when the passphrase-less key was added (the operator's instruction was "replace," not "add alongside") | No — never worked in CI |
| `escobar_deploy` (existed only in the AI assistant's working environment, never the operator's) | `claude-deploy-escobar` | *was* `root@200.141.14.221` | **🗑️ Retired** — removed from `root`'s `authorized_keys` on 2026-08-13 at the operator's explicit request, specifically to end the AI assistant's direct server access. Confirmed no longer authenticating (`Permission denied (publickey)`) immediately after removal. | No |
| `escobar_ci_deploy2` (existed only in the AI assistant's working environment) | `github-actions-escobar-deploy-2` | *was* `root@200.141.14.221` | **🗑️ Retired** — an earlier, never-successfully-used attempt at a dedicated CI key. Removed in the same cleanup as `claude-deploy-escobar`. | No — the `DEPLOY_SSH_KEY` secret never reliably matched this key; this mismatch was the original cause of early CI deploy failures. |

## Fingerprints

**⚠️ Not recorded.** Public key fingerprints were never printed to this conversation (only each key's
*comment*, e.g. `rishi-personal-escobar`, was used to identify keys in `authorized_keys` listings — the
actual base64 key material was deliberately never displayed). If you want fingerprints on file for future
verification, run this yourself on the server for each `authorized_keys` file:
```bash
ssh-keygen -lf /root/.ssh/authorized_keys
ssh-keygen -lf /home/deploy/.ssh/authorized_keys
```

## Where each active private key should live

- **`rishi-personal-escobar`** (your personal root access): on your own machine, `~/.ssh/`, and backed up
  per the strategy in the next section. This is the key you use for all manual server administration.
- **`escobar_ci_nopass` / `github-actions-escobar-ci`** (CI's key): the private half should exist in
  **exactly two places** — (1) wherever you generated it, ideally moved into your password manager's secure
  file storage and then deleted from disk, and (2) the GitHub Actions secret `DEPLOY_SSH_KEY`. It does not
  need to be passphrase-protected (CI can't type a passphrase), which is a deliberate, accepted trade-off —
  its blast radius is already limited by the `deploy` account having no `sudo` and owning only
  `/opt/escobar-club/app`.

## Recovery procedure if your laptop is lost

See [`COMPLETE_RECOVERY_RUNBOOK.md`](COMPLETE_RECOVERY_RUNBOOK.md) for the full walkthrough. In short:

1. **If you have a backup of `rishi-personal-escobar`'s private key** (see backup strategy below): restore
   it to `~/.ssh/` on your new machine, `chmod 600` it, and you're back in as root immediately.
2. **If you do NOT have a backup of it:** the private key is gone permanently — see the next section for
   why, and what to do.
3. **The CI key (`escobar_ci_nopass`) does not need laptop recovery at all** if you also backed it up
   separately, since GitHub Actions holds its own copy in the `DEPLOY_SSH_KEY` secret regardless of what
   happens to your laptop. CI will keep working even through total laptop loss, as long as you don't also
   lose GitHub account access.

---

## Why a lost private key cannot be recreated

An SSH key pair is asymmetric: the public key (safe to share, safe to put in `authorized_keys` or even in
this document) is mathematically derived from the private key, but **not the reverse**. There is no
operation that reconstructs a private key from its public key — this is the entire point of asymmetric
cryptography, not a limitation of any particular tool. If the private key file is gone and there is no
backup of it, it is **gone permanently**, full stop.

**What this means concretely:** if `rishi-personal-escobar`'s private key is lost with no backup, you
cannot "recover" root access with that key — you must **replace** it:

1. Generate a **new** key pair on your new machine: `ssh-keygen -t ed25519 -C "rishi-personal-escobar-2"`.
2. Get the new **public** key onto the server. This requires *some* existing way in — either:
   - You still have another working key (e.g. if the CI key's private half is safely backed up and you're
     comfortable temporarily using server access via that path to add your new key — though it's cleaner to
     avoid mixing CI and personal access even temporarily), or
   - Hostinger's own **browser-based VPS console** (via hPanel — this does not need SSH at all, it's a
     virtual terminal to the machine), which is exactly the scenario this access method exists for.
3. Add the new public key to `/root/.ssh/authorized_keys` (append, don't overwrite, in case anything else
   is relying on the current state).
4. Confirm the new key works, *then* remove the old (now-unusable, since its private half is lost) key's
   line from `authorized_keys` — it's harmless to leave a public key you can never authenticate with, but
   removing it keeps the file accurate.

## Safe backup strategy for your SSH private keys

Per your own instruction: **do not put private keys in this documentation, this repo, or plain text files
in general.** Use one of:

- **A password manager with secure file/note attachment support** (1Password, Bitwarden, etc.) — attach the
  private key file itself (not pasted as text, which can mangle line breaks and corrupt the key) as a
  secure attachment. This is the recommended approach — it's encrypted at rest, syncs across your devices,
  and survives a lost laptop by design.
- **An encrypted backup volume** (e.g. VeraCrypt container, or your OS's full-disk encryption plus a backup
  to encrypted external storage) — acceptable, but a password manager is simpler and more reliably
  recoverable from a *different* new machine.
- **Do not** email it to yourself, put it in an unencrypted cloud drive, or store it as a plain `.txt`/`.md`
  file anywhere — including outside this repo.

Back up:
1. `rishi-personal-escobar` (your personal root key) — the one that matters most, since it's your only path
   to full server control if CI/GitHub access is also somehow lost.
2. `escobar_ci_nopass` (the CI key) — lower urgency, since GitHub already holds a working copy in
   `DEPLOY_SSH_KEY`, but worth having so you're not fully dependent on GitHub's secret storage either.
