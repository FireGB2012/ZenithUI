# Zenith and a server

Asked for on 2026-10-04: "set my app up for connection to a server" and a prompt for the AI that
looks after the server. Until now Zenith had no internet permission and no networking code, and
said so in its listing and its privacy policy.

## Decided (by Gabriel, 2026-10-04)

| Question | Answer |
| --- | --- |
| What the server is for | Real Aura codes, cloud backup, accounts |
| Where it runs | Gabriel's own server; another AI with a terminal there installs it |
| Signing in | Username and password, **and** Google |
| Who gets cloud backup | Zenith Plus, the same rule as backing up to a file |
| How a backup is kept | As it is, readable on the server, behind the account's sign-in. No backup password. The hidden-apps PIN is never uploaded |

Decided by Claude, said in the plan Gabriel approved: a code pays the friend 150 and its owner 150,
for the owner's first ten friends; both need an account; the server is one Python file that the
other AI installs unchanged; an account can be deleted in the app and on a page the server serves
(Google Play requires both).

## The server (`server/`)

`zenith_server.py`: one file, standard library only, SQLite, listening on loopback behind a web
server that does HTTPS. `test_zenith_server.py` (run `python -m unittest` in `server/`) starts the
real thing and talks to it over HTTP. `make_prompt.py` writes `PROMPT-FOR-SERVER-AI.md`, the
message for the other AI, with the program inside it and its checksum.

Everything is JSON under `/v1`. A refusal is `{"error": "<word>", "message": "<sentence>"}`.
A signed-in request carries `Authorization: Bearer <token>`.

| Route | Does | Answers |
| --- | --- | --- |
| `GET /v1/health` | says it is there | `{ok, name, version, google}` |
| `POST /v1/account` `{username, password, code?}` | makes a password account, signed in | 201 `{token, account}`; 400 `bad_username` `bad_password`; 409 `taken` |
| `POST /v1/session` `{username, password}` | signs in | 200 `{token, account}`; 401 `wrong` |
| `POST /v1/session/google` `{idToken, code?, create?}` | signs in with Google, making the account the first time | 200 `{token, created, account}`; 401 `wrong`; 404 `no_account`; 503 `no_google` |
| `DELETE /v1/session` | signs this phone out | 204 |
| `GET /v1/me` | the account as it stands | `{account}` |
| `DELETE /v1/account` `{password}` | deletes it and all that is kept with it (no password for a Google account) | 204; 401 `wrong` |
| `PUT /v1/backup` (the backup's own JSON) | keeps it, in place of the last | `{savedAt}`; 400 `not_backup`; 413 |
| `GET /v1/backup` | the backup, as it was sent, with `X-Zenith-Saved-At` | 404 `no_backup` |
| `DELETE /v1/backup` | forgets it | 204 |
| `POST /v1/aura/redeem` `{code}` | uses a friend's code, once per account | `{redeemed, account}`; 400 `bad_code`; 404 `unknown_code`; 409 `already`; 422 `own_code` |

`account` is `{username, google, code, referrals, referralCap, redeemed, backupAt}`. `username` is
null for a Google account. `referrals` is how many friends used the code, never more than the cap.
Any route can answer 401 `signed_out`, 429 `slow_down`, 503 `busy`.

What it keeps: username and a salted scrypt hash; or Google's id number for the person and nothing
else of Google's; the Aura code; one row per code used; the last backup. Tokens are kept as hashes.

## The app

- `server.properties` (tracked, nothing secret): the server's address and the Google web client
  ID. Both empty until the server exists; empty means every server feature stays out of sight and
  the app behaves as it did. `-PzenithServerUrl=…` and `-PzenithGoogleClientId=…` override them for
  one build.
- `data/server/`: `ZenithServer` (the routes above over `HttpURLConnection`, `org.json`; a reply is
  a yes, a no with the server's word and sentence, or "out of reach"), `AccountStore` (the token
  and the account, in a prefs file left out of Android's own backups) and `AccountRules`.
- Settings gets an **Account** page: create, sign in, Continue with Google (Credential Manager),
  sign out, delete.
- **Backup & reset** gets "Back up to your account" and "Restore from your account" (Plus).
- **Aura**: with an account a code is checked by the server; the owner is paid when the app next
  asks how many friends used it.

### Built (2026-10-04), and what was decided on the way

- **Aura stays on the phone.** The server counts; it holds no balance. What a code has earned is
  worked out on each phone from the server's count: `referralsPaid` in the settings remembers, for
  each account's code, what the count was when this phone last paid, and `AccountRules.settle` pays
  the difference. So it travels with the points in a backup, two accounts on one phone never pay
  for each other, and a new account that is given a deleted account's code starts from its own
  nothing. A phone signed in to an account that has earned from ten friends is paid for the ten,
  whichever phone it is: each phone's points are its own.
- **A code used on another phone** pays this phone once too (`referredBy` becomes
  `Aura.USED_ON_ACCOUNT`, which no code can look like). It is also what puts things right if the
  phone died between the server counting a code and the points being written down.
- **Each request is seen through** by the view model even when the page that asked for it is
  closed, and they take turns (a mutex), so an account the server made is never lost on the way
  back and replies are taken in the order they were given.
- **The copy sent to the account** leaves out the hidden-apps PIN, the trial date and the purchase
  (`AccountRules.forAccount`); restoring keeps the phone's own. It does hold the list of hidden
  apps, as a backup file does: that is the "as it is" Gabriel chose.
- **The app asks how the account stands** when the Account or Backup page opens and, at most every
  ten minutes, on the way back to the home screen; after a failure, a minute later. Signed out, it
  never calls.
- **Signed out in a build with a server**, the Aura page shows one card in place of "Your code" and
  "Redeem": a code is not real until the server knows whose it is.
- **Only HTTPS**, except that a debug build may talk plain HTTP to the PC it runs beside
  (`10.0.2.2`, `localhost`), for testing against `python server/zenith_server.py`.
- `googleid` is pinned to 1.2.0: 1.2.1 was built with a newer Kotlin than this project compiles
  with.
- Requests are sent in one piece, not streamed: Java throws away the answer to a streamed request
  that is refused as not signed in.

Tests: `AccountRulesTest`, `ZenithServerTest` (the app's client against the real server program,
started on a spare port; skipped where there is no Python), `SettingsCatalogTest` (nothing of
accounts is found in a build with no server). Walked on the API 29 emulator against the real
server on the PC: create, wrong password, sign out and in, a friend's use of the code arriving,
redeeming a friend's (and an unknown one, and one's own), backup, restore, delete, server down, the
light theme, the shrunk release build. **Not checked: Continue with Google itself**, which needs
the real client IDs and a phone with a Google account.

## The server, as installed (2026-10-04)

`https://zenithlabz.duckdns.org`, in `server.properties` since the report came back. The program
is the one in `server/`, unchanged (same sha256). It runs on a desktop PC in Gabriel's home, behind
his router: Caddy with a Let's Encrypt certificate in front, a free DuckDNS name kept up to date by
a cron job, the router's ports 80 and 443 forwarded by UPnP, a copy of the database every night
with fourteen kept. Google sign-in is off there until it is given the Web client ID; the app has
none either, so it shows no Google button.

Checked from this PC on 2026-10-04: the health route and the delete page answer from the internet
(asked from France and Singapore); the app on the API 29 emulator made an account there over
HTTPS, backed up, restored, signed out and in, and deleted the account, which left nothing behind.

Two things that come with a server at home, both told to Gabriel:

- **Inside his own home network the name does not answer**: the router does not send traffic for
  its own outside address back in. A phone on his Wi-Fi cannot reach the server by name; on mobile
  data, and everywhere else, it can. (The emulator test went round this with a hosts entry.)
- **It is up only while that PC is on and online**, and the name gives away the home's internet
  address to anyone who looks it up. The name can be pointed at a hosted server later without a
  new build of the app, as long as the DuckDNS name is kept.

## What changes for privacy

The internet permission is new. The listing and `docs/index.html` stop saying "no account, no
server, nothing leaves your phone" and say instead: nothing leaves it unless you make an account,
and then exactly what is listed above. Play's Data safety form has to be filled in again by
Gabriel, and the delete page's address goes in the listing. The answers are in `play/listing.md`;
the order of doing it is in `RELEASING.md` ("Turning the server on"). The hosted policy is a copy
in another repository and has to be replaced by hand.

Worth doing on the server, and not done by the prompt: a limit on how long its logs are kept (they
hold an IP address, a time and a path for each request). The policy says only that they rotate.

## Not done here

Linking a Google sign-in to an existing password account; resetting a forgotten password (there
is no email address to send anything to); backing up by itself on a schedule; checking a Plus
purchase on the server.
