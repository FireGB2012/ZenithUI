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
  the app behaves as it did.
- `data/server/`: `ZenithServer` (the routes above over `HttpURLConnection`, `org.json`),
  `AccountStore` (the token and the account, in a prefs file left out of Android's own backups),
  and the pure rules with tests: what a refusal means, what a code pays.
- Settings gets an **Account** page: create, sign in, Continue with Google (Credential Manager),
  sign out, delete.
- **Backup & reset** gets "Back up to your account" and "Restore from your account" (Plus).
- **Aura**: with an account a code is checked by the server; the owner is paid when the app next
  asks how many friends used it.

## What changes for privacy

The internet permission is new. The listing and `docs/index.html` stop saying "no account, no
server, nothing leaves your phone" and say instead: nothing leaves it unless you make an account,
and then exactly what is listed above. Play's Data safety form has to be filled in again by
Gabriel, and the delete page's address goes in the listing.

## Not done here

Linking a Google sign-in to an existing password account; resetting a forgotten password (there
is no email address to send anything to); backing up by itself on a schedule; checking a Plus
purchase on the server.
