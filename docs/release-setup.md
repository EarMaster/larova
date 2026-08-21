# Release setup

One-time setup for signing and publishing: the upload key, the four GitHub secrets
`release.yml` needs, the Play Console side, and the branch protection on `main`.

Nothing here is needed to work on the app. It is needed before the first release can leave the
machine, and it is worth doing early because two of the steps have waiting time in them (a Play
Console account review, and DNS for the privacy policy URL).

---

## 1. The upload key

**What this key is.** With Play App Signing — which is mandatory for new apps — Google holds the
actual *app signing key* and re-signs every release. What you generate here is the **upload key**,
which only proves to Play that a build came from you. That distinction is worth understanding
before you worry about losing it: a lost upload key is recoverable through a Play Console support
request, while a lost app signing key under the old model was terminal. It is still a blocker
until Google resets it, so back it up.

Run this **in your own Git Bash window**, not through Claude Code's `!` prefix — `read -s` needs a
real terminal to hide what you type, and the whole point is that the password never lands in
scrollback or in an agent transcript.

```bash
cd /d/Users/nicow/Tools/larova
KEYTOOL="/c/Program Files/Java/jdk-23/bin/keytool.exe"

# Prompts twice, echoes nothing. Nothing is written to shell history because the value
# lives in an environment variable rather than on a command line.
read -rsp "New keystore password: " KS_PW; echo
read -rsp "Repeat: "               KS_PW2; echo
[ "$KS_PW" = "$KS_PW2" ] && echo "match" || echo "MISMATCH — stop and start over"

"$KEYTOOL" -genkeypair \
  -alias larova-upload \
  -keyalg RSA -keysize 4096 \
  -validity 10950 \
  -storetype PKCS12 \
  -keystore larova-upload.p12 \
  -dname "CN=Larova, O=Larova, C=DE" \
  -storepass:env KS_PW \
  -keypass:env KS_PW
```

Why these values:

- **`-validity 10950`** is 30 years. Play requires the key to stay valid well past 2033; an
  expiring upload key is a problem you cannot fix by editing anything, so overshoot.
- **`-keysize 4096`** because this key is generated once and lives for the life of the app.
- **`-storetype PKCS12`** is keytool's modern default. JKS still works but prints a migration
  warning on every use. Note this means the file is `.p12`, not `.jks` — the extension is
  cosmetic to `release.yml`, which decodes the secret to a path of its own choosing, but the
  Gradle `signingConfig` written in M0 may need `storeType = "PKCS12"` if the build JDK's default
  ever differs.
- **`-dname`** ends up in the certificate and is visible in the Play Console's signing tab.
  It is not user-facing; change it if you would rather it named you personally.

**Back it up before doing anything else.** The keystore file *and* the password, in two places
that are not this laptop — a password manager entry with the file attached is the usual answer.
`.gitignore` covers `*.p12`, `*.jks` and `*.b64` so none of it can reach the repo by accident, but
that also means nothing in git will ever remind you it existed.

**The keystore does not live in the repo**, so a local release build needs `KEYSTORE_PATH`
pointing at wherever it is kept. CI is unaffected: `release.yml` reconstructs the file from
`KEYSTORE_BASE64` into the runner's workspace, and never reads a path from here. Whatever the M0
Gradle `signingConfig` ends up looking like, it must treat a missing keystore as "build unsigned"
rather than "fail" — otherwise a plain `assembleDebug` on a fresh clone stops working for anyone
who does not hold the key, which is everyone but you.

To confirm the password and alias are right without waiting for a release to fail:

```bash
"$KEYTOOL" -list -keystore /path/to/larova-upload.p12 -alias larova-upload
```

## 2. The four GitHub secrets

`release.yml` reads these to sign the release build. Same terminal session as above, so `$KS_PW`
is still set:

```bash
base64 -w 0 larova-upload.p12 > larova-upload.b64

gh secret set KEYSTORE_BASE64 < larova-upload.b64
printf '%s' "$KS_PW"        | gh secret set KEYSTORE_PASSWORD
printf '%s' "larova-upload" | gh secret set KEY_ALIAS
printf '%s' "$KS_PW"        | gh secret set KEY_PASSWORD

rm larova-upload.b64        # the .p12 stays; only the transport copy goes
unset KS_PW KS_PW2
gh secret list
```

`base64 -w 0` keeps it on one line. Multi-line would still decode, but a single line removes any
question about how the secret gets expanded in the workflow.

Piping rather than `--body "$KS_PW"` keeps the password out of the process list and out of
`~/.bash_history`.

Verify with `gh secret list` — four entries, and note that GitHub cannot show you the values
again. If you ever need to confirm the keystore password is right, test it locally with
`keytool -list -keystore larova-upload.p12` rather than by watching a release fail.

## 3. Google Play

None of this can be automated from here; it needs a browser and, for the first step, a wait.

1. **Play Console developer account** — one-off registration fee, and identity verification that
   can take a few days. Nothing else on this list can complete before it does.
2. **Create the app**, package name **`app.larova`**. This cannot be changed afterwards: a
   different package name is a different app, with no shared update path or installed base.
3. **Privacy policy URL** — `https://larova.app/privacy` must actually resolve when a reviewer
   opens it. This is the one store requirement Larova's offline architecture cannot satisfy by
   itself, so it needs the domain pointed somewhere and a page served. It does not weaken the
   no-internet claim: the app never fetches it, the Play Store links to it.
4. **Data safety form** — no data collected, no data shared. Answerable honestly, which is
   unusual enough to be worth saying in the listing text.
5. **Target audience 18+**, category Tools or Lifestyle. Not Medical, not Health & Fitness. See
   `concept.md` §2.3 for why: declaring a child audience triggers the whole Families policy.
6. **Service account for automated upload** — in Google Cloud, create a service account, grant it
   access in the Play Console under Users and permissions (Release manager is enough), download
   the JSON key, then:

   ```bash
   gh secret set SERVICE_ACCOUNT_JSON < ~/Downloads/play-service-account.json
   rm ~/Downloads/play-service-account.json
   ```

   Until this exists, `google-play.yml` fails at the upload step. `release.yml` itself still
   works — it tags, builds and cuts the GitHub Release regardless, so you are not blocked from
   releasing, only from publishing to Play.
7. **First upload must be manual.** Play will not accept an API upload for a track that has never
   had a release; upload the first AAB by hand, then the workflow takes over.

## 4. Branch protection

Applied to `main` already, matching the sibling project:

| Setting | Value |
|---|---|
| Pull request required | yes, 0 approvals |
| Enforced for admins | **yes** — no direct pushes, including yours |
| Required checks | `Build`, `Unit Tests`, `Lint`, `Analyze (Kotlin)` |
| Force pushes / deletions | blocked |

This changes how a release is promoted: `develop` reaches `main` through a pull request that you
open and merge, and merging it is what triggers `release.yml`. An agent should never open that
PR — see the branching model in `AGENTS.md`.

**The required checks currently pass by being skipped.** Every job in `ci.yml` and `codeql.yml` is
gated on a root `./gradlew` that does not exist yet, and GitHub counts a skipped job as a
satisfied requirement. If a PR ever sits forever on a pending check instead, that is the thing to
look at first: a job that is skipped reports, while a *workflow* that never runs at all (a path
filter, or a workflow-level `if`) reports nothing and blocks the merge indefinitely.

To relax or remove it:

```bash
# see the current state
gh api repos/EarMaster/larova/branches/main/protection

# let admins bypass, keeping everything else
gh api -X PATCH repos/EarMaster/larova/branches/main/protection/enforce_admins  # DELETE to remove

# remove protection entirely
gh api -X DELETE repos/EarMaster/larova/branches/main/protection
```

## 5. What is done and what is waiting

| Step | State |
|---|---|
| Branch protection on `main` | done |
| `.gitignore` covers keystores, `.p12`, base64 and service-account JSON | done |
| Upload keystore generated and backed up | done, 2026-08-21 |
| `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | done, 2026-08-21 |
| Play Console account, app, listing, service account | **yours, §3** — start the account first, it has a wait |
| `SERVICE_ACCOUNT_JSON` | after the Play service account exists |
| Privacy policy at `https://larova.app/privacy` | needs DNS and a page; blocks store review, not development |
| Gradle project, so any of the CI jobs actually run | M0, `implementation-plan.md` |
