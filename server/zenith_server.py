#!/usr/bin/env python3
"""
Zenith UI server: accounts, one backup per account, Aura codes, and shared Looks.

One file, standard library only (Python 3.9 or newer). It listens on the loopback address and is
meant to sit behind a web server that speaks HTTPS for it (Caddy, nginx). Everything it keeps is
in one SQLite file.

    python3 zenith_server.py                 run the server
    python3 zenith_server.py --selftest      start a throwaway copy and try every route
    python3 zenith_server.py --check URL     try a running server from outside, over its real address
    python3 zenith_server.py --feature CODE  show a shared Look in the app's Featured list
    python3 zenith_server.py --unfeature CODE  take it off again

Settings come from the environment:

    ZENITH_DB                 where the database file is            (./zenith.db)
    ZENITH_HOST               address to listen on                  (127.0.0.1)
    ZENITH_PORT               port to listen on                     (8787)
    ZENITH_GOOGLE_CLIENT_ID   the "Web application" OAuth client ID; Google sign-in is off without it
    ZENITH_TRUST_PROXY        1 to take the caller's address from X-Forwarded-For   (1 on loopback)
    ZENITH_MAX_REFERRALS      how many friends a code pays out for  (10)
    ZENITH_PRIVACY_URL        link shown on the front page          (none)

What is stored: for a password account, the username and a salted scrypt hash of the password; for
a Google account, Google's id number for that person and nothing else from Google; the account's
Aura code; who redeemed a code (so it counts once); the last backup the app sent, exactly as it
was sent; and each Look the account shared, under its code, until the account is deleted. Which
Looks are featured is set only from this command line, so nothing anybody uploads is shown to
anybody else unless the person running the server chose it. Passwords, tokens and request bodies
are never written to the log.
"""

import base64
import collections
import hashlib
import hmac
import json
import os
import re
import secrets
import shutil
import sqlite3
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

API_VERSION = 1

# The app's own alphabet for Aura codes (Aura.ALPHABET): six characters, no vowels.
CODE_ALPHABET = "BCDFGHJKLMNPQRSTVWXZ23456789"
CODE_RE = re.compile("^[" + CODE_ALPHABET + "]{6}$")
USERNAME_RE = re.compile(r"^[a-z0-9_]{3,20}$")
PASSWORD_MIN, PASSWORD_MAX = 8, 200

MAX_JSON_BODY = 16 * 1024
MAX_BACKUP_BODY = 512 * 1024
# A Look is a home screen's settings and layout, without anything personal: a backup's size at most.
MAX_LOOK_BODY = 256 * 1024
# The newest this many of an account's shared Looks are kept; older ones go, unless featured.
MAX_LOOKS_PER_ACCOUNT = 20
MAX_FEATURED = 30
# How much of a request nobody asked to read is still read before answering; see Handler.drain.
MAX_DRAIN = 2 * 1024 * 1024

SESSION_IDLE_SECONDS = 400 * 24 * 3600
MAX_SESSIONS_PER_ACCOUNT = 20

GOOGLE_CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs"
GOOGLE_ISSUERS = ("accounts.google.com", "https://accounts.google.com")


def now_ms():
    return int(time.time() * 1000)


# ----------------------------------------------------------------------------- passwords

SCRYPT_N, SCRYPT_R, SCRYPT_P = 2 ** 15, 8, 1

# One hash takes about 32 MB for a tenth of a second. On a small server a burst of sign-ins all
# hashing at once would run it out of memory, so only two are ever worked on at a time.
_HASHING = threading.BoundedSemaphore(2)


def hash_password(password):
    salt = secrets.token_bytes(16)
    with _HASHING:
        digest = hashlib.scrypt(password.encode("utf-8"), salt=salt, n=SCRYPT_N, r=SCRYPT_R, p=SCRYPT_P,
                                maxmem=64 * 1024 * 1024, dklen=32)
    return "scrypt$%d$%d$%d$%s$%s" % (SCRYPT_N, SCRYPT_R, SCRYPT_P, salt.hex(), digest.hex())


def check_password(password, stored):
    try:
        scheme, n, r, p, salt, digest = stored.split("$")
        if scheme != "scrypt":
            return False
        with _HASHING:
            got = hashlib.scrypt(password.encode("utf-8"), salt=bytes.fromhex(salt), n=int(n), r=int(r), p=int(p),
                                 maxmem=64 * 1024 * 1024, dklen=len(digest) // 2)
        return hmac.compare_digest(got, bytes.fromhex(digest))
    except (ValueError, TypeError):
        return False


# A hash to spend the same time on when the username does not exist, so that a wrong name and a
# wrong password cannot be told apart by how long the answer takes.
_DUMMY_HASH = hash_password(secrets.token_hex(16))


# ----------------------------------------------------------------------------- Google sign-in

def _b64url(text):
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


# DER prefix of a SHA-256 DigestInfo (RFC 8017, section 9.2).
_SHA256_PREFIX = bytes.fromhex("3031300d060960864801650304020105000420")


def rsa_sha256_verify(n, e, message, signature):
    """
    RSASSA-PKCS1-v1_5 with SHA-256. The whole padded block is rebuilt and compared, byte for byte,
    with what the signature opens to: nothing is parsed out of the signature, so there is nothing
    for a forged one to slip past.
    """
    size = (n.bit_length() + 7) // 8
    if len(signature) != size or size < len(_SHA256_PREFIX) + 32 + 11:
        return False
    number = int.from_bytes(signature, "big")
    if number >= n:
        return False
    opened = pow(number, e, n).to_bytes(size, "big")
    tail = _SHA256_PREFIX + hashlib.sha256(message).digest()
    expected = b"\x00\x01" + b"\xff" * (size - len(tail) - 3) + b"\x00" + tail
    return hmac.compare_digest(opened, expected)


class GoogleVerifier:
    """Checks a Google ID token: signed by Google, meant for this app, and not out of date."""

    def __init__(self, client_id, fetch=None, clock=time.time):
        self.client_id = client_id
        self._fetch = fetch or self._fetch_certs
        self._clock = clock
        self._keys = {}
        self._fetched = 0.0
        self._lock = threading.Lock()

    @staticmethod
    def _fetch_certs():
        with urllib.request.urlopen(GOOGLE_CERTS_URL, timeout=10) as response:
            return json.loads(response.read().decode("utf-8"))

    def _key(self, kid):
        with self._lock:
            # Google changes its keys every few days. An unknown key id means "look again", but
            # not more than once a minute, whatever is being thrown at this.
            stale = self._clock() - self._fetched
            if (kid not in self._keys and stale > 60) or stale > 6 * 3600:
                fresh = {}
                for key in self._fetch().get("keys", []):
                    if key.get("kty") == "RSA" and key.get("alg", "RS256") == "RS256" and key.get("use", "sig") == "sig":
                        fresh[key["kid"]] = (int.from_bytes(_b64url(key["n"]), "big"), int.from_bytes(_b64url(key["e"]), "big"))
                self._keys = fresh
                self._fetched = self._clock()
            return self._keys.get(kid)

    def verify(self, token):
        """Returns the token's claims, or raises ValueError saying what is wrong with it."""
        if not isinstance(token, str) or token.count(".") != 2 or len(token) > 8192:
            raise ValueError("not a token")
        head64, body64, sig64 = token.split(".")
        try:
            header = json.loads(_b64url(head64))
            claims = json.loads(_b64url(body64))
            signature = _b64url(sig64)
        except (ValueError, TypeError):
            raise ValueError("not a token")
        if not isinstance(header, dict) or not isinstance(claims, dict):
            raise ValueError("not a token")
        # Only the one algorithm, and only Google's own keys: a token cannot choose how it is checked.
        if header.get("alg") != "RS256":
            raise ValueError("wrong algorithm")
        key = self._key(header.get("kid"))
        if key is None:
            raise ValueError("unknown key")
        if not rsa_sha256_verify(key[0], key[1], (head64 + "." + body64).encode("ascii"), signature):
            raise ValueError("bad signature")
        if claims.get("iss") not in GOOGLE_ISSUERS:
            raise ValueError("wrong issuer")
        if claims.get("aud") != self.client_id:
            raise ValueError("meant for another app")
        current = self._clock()
        if not isinstance(claims.get("exp"), (int, float)) or claims["exp"] < current - 60:
            raise ValueError("out of date")
        if isinstance(claims.get("iat"), (int, float)) and claims["iat"] > current + 300:
            raise ValueError("from the future")
        if not isinstance(claims.get("sub"), str) or not claims["sub"]:
            raise ValueError("no subject")
        return claims


# ----------------------------------------------------------------------------- rate limits

class Limiter:
    """So many calls for a key in so many seconds. Kept in memory: a restart forgets, which is fine."""

    def __init__(self, clock=time.time):
        self._clock = clock
        self._calls = collections.defaultdict(collections.deque)
        self._lock = threading.Lock()
        self._swept = clock()

    def allow(self, key, limit, seconds):
        current = self._clock()
        with self._lock:
            if current - self._swept > 600:
                for stale in [k for k, calls in self._calls.items() if not calls or current - calls[-1] > 86400]:
                    del self._calls[stale]
                self._swept = current
            calls = self._calls[(key, seconds)]
            while calls and current - calls[0] > seconds:
                calls.popleft()
            if len(calls) >= limit:
                return False
            calls.append(current)
            return True


# ----------------------------------------------------------------------------- the database

SCHEMA = """
CREATE TABLE IF NOT EXISTS accounts (
    id          INTEGER PRIMARY KEY,
    username    TEXT UNIQUE,
    pass        TEXT,
    google_sub  TEXT UNIQUE,
    code        TEXT NOT NULL UNIQUE,
    created     INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS sessions (
    token_hash  TEXT PRIMARY KEY,
    account_id  INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    created     INTEGER NOT NULL,
    last_used   INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS sessions_account ON sessions(account_id);
CREATE TABLE IF NOT EXISTS backups (
    account_id  INTEGER PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    body        BLOB NOT NULL,
    saved_at    INTEGER NOT NULL
);
-- One row for each time a code was used. When either account goes, its side of the row is
-- emptied and the row stays: a code that was counted stays counted, and holds nobody's name.
CREATE TABLE IF NOT EXISTS redemptions (
    id           INTEGER PRIMARY KEY,
    redeemer_id  INTEGER UNIQUE REFERENCES accounts(id) ON DELETE SET NULL,
    owner_id     INTEGER REFERENCES accounts(id) ON DELETE SET NULL,
    at           INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS redemptions_owner ON redemptions(owner_id);
-- A Look somebody shared: whose it is, what it is called and what it is. It goes with its account.
CREATE TABLE IF NOT EXISTS looks (
    code        TEXT PRIMARY KEY,
    account_id  INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    body        BLOB NOT NULL,
    featured    INTEGER NOT NULL DEFAULT 0,
    created     INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS looks_account ON looks(account_id);
"""


class Store:
    def __init__(self, path):
        self.path = path
        self._write = threading.Lock()
        db = self._connect()
        try:
            db.execute("PRAGMA journal_mode=WAL")
            db.executescript(SCHEMA)
        finally:
            db.close()

    def _connect(self):
        db = sqlite3.connect(self.path, timeout=10, isolation_level=None)
        db.execute("PRAGMA foreign_keys=ON")
        db.row_factory = sqlite3.Row
        return db

    def read(self, sql, args=()):
        db = self._connect()
        try:
            return db.execute(sql, args).fetchall()
        finally:
            db.close()

    def write(self, work):
        """Runs work(db) as one change: all of it happens, or none of it."""
        with self._write:
            db = self._connect()
            try:
                db.execute("BEGIN IMMEDIATE")
                try:
                    result = work(db)
                except BaseException:
                    db.execute("ROLLBACK")
                    raise
                db.execute("COMMIT")
                return result
            finally:
                db.close()


class Refused(Exception):
    """An answer that is not "yes": the HTTP status, a short word for the app, and a sentence for a person."""

    def __init__(self, status, error, message):
        super().__init__(message)
        self.status, self.error, self.message = status, error, message


def new_code(db, wanted=None):
    """The account's Aura code: the one the app already shows, if nobody has it, or a fresh one."""
    if wanted and CODE_RE.match(wanted) and not db.execute("SELECT 1 FROM accounts WHERE code=?", (wanted,)).fetchone():
        return wanted
    for _ in range(100):
        code = "".join(secrets.choice(CODE_ALPHABET) for _ in range(6))
        if not db.execute("SELECT 1 FROM accounts WHERE code=?", (code,)).fetchone():
            return code
    raise Refused(503, "busy", "Could not make a code. Try again.")


def new_look_code(db):
    """A code for a shared Look: six of the same characters as an Aura code, unique among Looks."""
    for _ in range(100):
        code = "".join(secrets.choice(CODE_ALPHABET) for _ in range(6))
        if not db.execute("SELECT 1 FROM looks WHERE code=?", (code,)).fetchone():
            return code
    raise Refused(503, "busy", "Could not make a code. Try again.")


def look_code(text):
    """The six characters of a Look's code, however it was typed: LOOK-ABC234, abc 234, ABC234."""
    if not isinstance(text, str):
        return ""
    cleaned = "".join(ch for ch in text.upper() if ch.isalnum())
    if cleaned.startswith("LOOK"):
        cleaned = cleaned[4:]
    return cleaned if CODE_RE.match(cleaned) else ""


def new_session(db, account_id):
    token = secrets.token_urlsafe(32)
    moment = now_ms()
    db.execute("INSERT INTO sessions(token_hash, account_id, created, last_used) VALUES (?,?,?,?)",
               (hashlib.sha256(token.encode("ascii")).hexdigest(), account_id, moment, moment))
    # A person signs in on a handful of phones, not on hundreds: the oldest sign-ins give way.
    db.execute("DELETE FROM sessions WHERE account_id=? AND token_hash NOT IN "
               "(SELECT token_hash FROM sessions WHERE account_id=? ORDER BY last_used DESC LIMIT ?)",
               (account_id, account_id, MAX_SESSIONS_PER_ACCOUNT))
    return token


# ----------------------------------------------------------------------------- the web pages

FRONT_PAGE = """<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><title>Zenith UI</title>
<style>body{font:16px/1.5 system-ui,sans-serif;max-width:34rem;margin:3rem auto;padding:0 1rem;color:#16181d}a{color:#5b4bd6}</style>
<h1>Zenith UI</h1>
<p>This is the server the Zenith UI home screen app talks to when you make an account: it keeps
your account, your Aura code, and your backup if you save one.</p>
<p><a href="/delete-account">Delete my account and everything kept with it</a></p>
%(privacy)s
</html>"""

DELETE_PAGE = """<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><title>Delete your Zenith UI account</title>
<style>body{font:16px/1.5 system-ui,sans-serif;max-width:34rem;margin:3rem auto;padding:0 1rem;color:#16181d}
input,button{font:inherit;padding:.6rem .8rem;margin:.25rem 0;width:100%%;box-sizing:border-box;border-radius:.6rem;border:1px solid #b9bcc6}
button{background:#b3261e;color:#fff;border:0;cursor:pointer}#said{margin-top:1rem;font-weight:600}</style>
<h1>Delete your Zenith UI account</h1>
<p>This deletes the account, its Aura code and its backup from this server, at once and for good.
What is on your phone is not touched. You can also do this in the app: Settings, Account, Delete account.</p>
<form id="form">
<input id="username" autocomplete="username" placeholder="Username" required>
<input id="password" type="password" autocomplete="current-password" placeholder="Password" required>
<button>Delete my account</button>
</form>
%(google)s
<p id="said" role="status"></p>
<script nonce="%(nonce)s">
const said = document.getElementById('said');
async function call(method, path, body, token) {
  const headers = {'Content-Type': 'application/json'};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const response = await fetch(path, {method, headers, body: body ? JSON.stringify(body) : undefined});
  const text = await response.text();
  let answer = {};
  try { answer = text ? JSON.parse(text) : {}; } catch (e) {}
  if (!response.ok) throw new Error(answer.message || 'That did not work.');
  return answer;
}
document.getElementById('form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const password = document.getElementById('password').value;
  try {
    const signedIn = await call('POST', '/v1/session', {username: document.getElementById('username').value, password});
    await call('DELETE', '/v1/account', {password}, signedIn.token);
    said.textContent = 'Your account has been deleted.';
    event.target.reset();
  } catch (error) { said.textContent = error.message; }
});
window.zenithGoogle = async (answer) => {
  try {
    const signedIn = await call('POST', '/v1/session/google', {idToken: answer.credential, create: false});
    await call('DELETE', '/v1/account', null, signedIn.token);
    said.textContent = 'Your account has been deleted.';
  } catch (error) { said.textContent = error.message; }
};
</script>
</html>"""

GOOGLE_BUTTON = """<p>Made your account with Google? Sign in here and it is deleted straight away.</p>
<script src="https://accounts.google.com/gsi/client" async nonce="%(nonce)s"></script>
<div id="g_id_onload" data-client_id="%(client)s" data-callback="zenithGoogle" data-auto_prompt="false"></div>
<div class="g_id_signin" data-type="standard" data-text="signin_with"></div>"""


# ----------------------------------------------------------------------------- the server

class Handler(BaseHTTPRequestHandler):
    server_version = "Zenith/%d" % API_VERSION
    timeout = 30

    # ---- plumbing

    def log_message(self, format, *args):
        # The request line and the status, and never a body, a header or a query string.
        sys.stderr.write("%s %s\n" % (self.address_string(), format % args))

    def log_request(self, code="-", size="-"):
        self.log_message('"%s %s" %s', self.command, self.path.split("?")[0], code)

    def caller(self):
        address = self.client_address[0]
        if self.server.trust_proxy and address in ("127.0.0.1", "::1"):
            forwarded = self.headers.get("X-Forwarded-For")
            if forwarded:
                # The last one is what our own web server saw; anything before it the caller wrote.
                return forwarded.split(",")[-1].strip() or address
        return address

    def drain(self):
        """
        Reads whatever the caller sent that nothing has read. A request can be refused before its
        body is looked at (too many tries, not signed in); answering and closing with the body
        still unread ends the connection in a reset, and the caller never sees the answer.
        """
        if self.drained:
            return
        self.drained = True
        length = self.headers.get("Content-Length") or ""
        left = min(int(length), MAX_DRAIN) if length.isdigit() else 0
        while left > 0:
            chunk = self.rfile.read(min(65536, left))
            if not chunk:
                break
            left -= len(chunk)

    def send(self, status, body=b"", content_type="application/json; charset=utf-8", headers=()):
        self.drain()
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")
        for name, value in headers:
            self.send_header(name, value)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def answer(self, status, value):
        self.send(status, json.dumps(value, separators=(",", ":")).encode("utf-8"))

    def body(self, limit):
        length = self.headers.get("Content-Length")
        if length is None:
            if self.headers.get("Transfer-Encoding"):
                raise Refused(411, "length", "Say how long the request is.")
            # Nothing was sent. What to make of that is for whoever asked for the body.
            return b""
        if not length.isdigit():
            raise Refused(400, "length", "Say how long the request is.")
        if int(length) > limit:
            raise Refused(413, "too_big", "That is too big.")
        self.drained = True
        return self.rfile.read(int(length))

    def json_body(self):
        raw = self.body(MAX_JSON_BODY)
        try:
            value = json.loads(raw.decode("utf-8")) if raw else {}
        except (ValueError, UnicodeDecodeError):
            raise Refused(400, "bad_json", "That is not JSON.")
        if not isinstance(value, dict):
            raise Refused(400, "bad_json", "That is not a JSON object.")
        return value

    def limit(self, what, limit, seconds, key=None):
        if not self.server.limiter.allow((what, key or self.caller()), limit, seconds):
            raise Refused(429, "slow_down", "Too many tries. Wait a little and try again.")

    def account(self):
        """The account the request's token belongs to."""
        header = self.headers.get("Authorization", "")
        if not header.startswith("Bearer "):
            raise Refused(401, "signed_out", "Sign in first.")
        token_hash = hashlib.sha256(header[7:].strip().encode("utf-8", "replace")).hexdigest()
        rows = self.server.store.read(
            "SELECT a.*, s.last_used AS session_used FROM sessions s JOIN accounts a ON a.id=s.account_id WHERE s.token_hash=?",
            (token_hash,))
        if not rows or now_ms() - rows[0]["session_used"] > SESSION_IDLE_SECONDS * 1000:
            raise Refused(401, "signed_out", "Sign in again.")
        # Noted once a day at most: reading must not turn every request into a write.
        if now_ms() - rows[0]["session_used"] > 24 * 3600 * 1000:
            self.server.store.write(lambda db: db.execute("UPDATE sessions SET last_used=? WHERE token_hash=?", (now_ms(), token_hash)))
        self.token_hash = token_hash
        return rows[0]

    def describe(self, account_id):
        store = self.server.store
        account = store.read("SELECT * FROM accounts WHERE id=?", (account_id,))[0]
        referrals = store.read("SELECT COUNT(*) AS n FROM redemptions WHERE owner_id=?", (account_id,))[0]["n"]
        redeemed = store.read("SELECT 1 FROM redemptions WHERE redeemer_id=?", (account_id,))
        backup = store.read("SELECT saved_at FROM backups WHERE account_id=?", (account_id,))
        return {
            "username": account["username"],
            "google": account["google_sub"] is not None,
            "code": account["code"],
            "referrals": min(referrals, self.server.max_referrals),
            "referralCap": self.server.max_referrals,
            "redeemed": bool(redeemed),
            "backupAt": backup[0]["saved_at"] if backup else None,
        }

    # ---- routing

    def route(self):
        path = self.path.split("?")[0].rstrip("/") or "/"
        self.drained = False
        try:
            self.limit("any", 240, 60)
            handler = ROUTES.get((self.command, path))
            if handler is None:
                known = {p for (_, p) in ROUTES}
                raise Refused(405 if path in known else 404, "no_such", "There is nothing here.")
            handler(self)
        except Refused as refused:
            self.answer(refused.status, {"error": refused.error, "message": refused.message})
        except sqlite3.Error:
            self.answer(503, {"error": "busy", "message": "The server is busy. Try again."})
        except (ConnectionError, TimeoutError):
            pass
        except Exception as error:  # A bug must not take the server down, or say more than this.
            sys.stderr.write("error: %s: %s\n" % (type(error).__name__, error))
            self.answer(500, {"error": "server", "message": "Something went wrong on the server."})

    do_GET = do_POST = do_PUT = do_DELETE = do_HEAD = route

    # ---- pages

    def front_page(self):
        privacy = self.server.privacy_url
        link = '<p><a href="%s">Privacy policy</a></p>' % privacy if privacy else ""
        self.send(200, (FRONT_PAGE % {"privacy": link}).encode("utf-8"), "text/html; charset=utf-8",
                  [("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'")])

    def delete_page(self):
        nonce = secrets.token_urlsafe(16)
        client = self.server.google.client_id if self.server.google else None
        google = GOOGLE_BUTTON % {"nonce": nonce, "client": client} if client else ""
        policy = ("default-src 'none'; style-src 'unsafe-inline' https://accounts.google.com; connect-src 'self' https://accounts.google.com; "
                  "frame-src https://accounts.google.com; img-src 'self' data: https://*.gstatic.com; "
                  "script-src 'nonce-%s' https://accounts.google.com; base-uri 'none'; form-action 'none'; frame-ancestors 'none'" % nonce)
        self.send(200, (DELETE_PAGE % {"nonce": nonce, "google": google}).encode("utf-8"), "text/html; charset=utf-8",
                  [("Content-Security-Policy", policy)])

    # ---- the API

    def health(self):
        self.answer(200, {"ok": True, "name": "zenith", "version": API_VERSION, "google": self.server.google is not None})

    def create_account(self):
        self.limit("signup", 5, 3600)
        self.limit("signup-day", 20, 86400)
        asked = self.json_body()
        username, password = asked.get("username"), asked.get("password")
        if not isinstance(username, str) or not USERNAME_RE.match(username):
            raise Refused(400, "bad_username", "A username is 3 to 20 of: small letters, numbers and _.")
        if not isinstance(password, str) or not PASSWORD_MIN <= len(password) <= PASSWORD_MAX:
            raise Refused(400, "bad_password", "A password is at least %d characters." % PASSWORD_MIN)
        hashed = hash_password(password)

        def work(db):
            if db.execute("SELECT 1 FROM accounts WHERE username=?", (username,)).fetchone():
                raise Refused(409, "taken", "That username is taken.")
            code = new_code(db, asked.get("code") if isinstance(asked.get("code"), str) else None)
            account_id = db.execute("INSERT INTO accounts(username, pass, code, created) VALUES (?,?,?,?)",
                                    (username, hashed, code, now_ms())).lastrowid
            return account_id, new_session(db, account_id)

        account_id, token = self.server.store.write(work)
        self.answer(201, {"token": token, "account": self.describe(account_id)})

    def sign_in(self):
        self.limit("signin", 10, 60)
        asked = self.json_body()
        username, password = asked.get("username"), asked.get("password")
        if not isinstance(username, str) or not isinstance(password, str) or len(password) > PASSWORD_MAX:
            raise Refused(400, "bad_request", "Give a username and a password.")
        username = username.strip().lower()
        self.limit("signin-name", 10, 900, key=username)
        rows = self.server.store.read("SELECT id, pass FROM accounts WHERE username=?", (username,))
        stored = rows[0]["pass"] if rows and rows[0]["pass"] else None
        # The same work whether or not there is such an account.
        right = check_password(password, stored or _DUMMY_HASH) and stored is not None
        if not right:
            raise Refused(401, "wrong", "Wrong username or password.")
        account_id = rows[0]["id"]
        token = self.server.store.write(lambda db: new_session(db, account_id))
        self.answer(200, {"token": token, "account": self.describe(account_id)})

    def sign_in_google(self):
        self.limit("signin", 10, 60)
        if self.server.google is None:
            raise Refused(503, "no_google", "Google sign-in is not set up on this server.")
        asked = self.json_body()
        try:
            claims = self.server.google.verify(asked.get("idToken"))
        except ValueError:
            raise Refused(401, "wrong", "Google did not vouch for that sign-in.")
        except (urllib.error.URLError, OSError):
            raise Refused(503, "busy", "Could not reach Google. Try again.")
        subject = claims["sub"]
        may_create = asked.get("create", True) is not False
        if may_create and not self.server.store.read("SELECT 1 FROM accounts WHERE google_sub=?", (subject,)):
            self.limit("signup", 5, 3600)
            self.limit("signup-day", 20, 86400)

        def work(db):
            row = db.execute("SELECT id FROM accounts WHERE google_sub=?", (subject,)).fetchone()
            created = row is None
            if created:
                if not may_create:
                    raise Refused(404, "no_account", "There is no Zenith account for that Google account.")
                code = new_code(db, asked.get("code") if isinstance(asked.get("code"), str) else None)
                account_id = db.execute("INSERT INTO accounts(google_sub, code, created) VALUES (?,?,?)",
                                        (subject, code, now_ms())).lastrowid
            else:
                account_id = row["id"]
            return account_id, new_session(db, account_id), created

        account_id, token, created = self.server.store.write(work)
        self.answer(200, {"token": token, "created": created, "account": self.describe(account_id)})

    def sign_out(self):
        self.account()
        self.server.store.write(lambda db: db.execute("DELETE FROM sessions WHERE token_hash=?", (self.token_hash,)))
        self.send(204)

    def me(self):
        self.answer(200, {"account": self.describe(self.account()["id"])})

    def delete_account(self):
        account = self.account()
        self.limit("delete", 10, 3600, key=str(account["id"]))
        if account["pass"] is not None:
            # A phone left unlocked on a table must not be enough to lose an account.
            asked = self.json_body()
            if not isinstance(asked.get("password"), str) or not check_password(asked["password"], account["pass"]):
                raise Refused(401, "wrong", "Wrong password.")
        self.server.store.write(lambda db: db.execute("DELETE FROM accounts WHERE id=?", (account["id"],)))
        self.send(204)

    def put_backup(self):
        account = self.account()
        self.limit("backup", 60, 3600, key=str(account["id"]))
        raw = self.body(MAX_BACKUP_BODY)
        try:
            value = json.loads(raw.decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            raise Refused(400, "bad_json", "A backup is JSON.")
        if not isinstance(value, dict) or value.get("format") != "zenith-ui-backup":
            raise Refused(400, "not_backup", "That is not a Zenith backup.")
        saved = now_ms()
        self.server.store.write(lambda db: db.execute(
            "INSERT INTO backups(account_id, body, saved_at) VALUES (?,?,?) "
            "ON CONFLICT(account_id) DO UPDATE SET body=excluded.body, saved_at=excluded.saved_at",
            (account["id"], raw, saved)))
        self.answer(200, {"savedAt": saved})

    def get_backup(self):
        account = self.account()
        rows = self.server.store.read("SELECT body, saved_at FROM backups WHERE account_id=?", (account["id"],))
        if not rows:
            raise Refused(404, "no_backup", "There is no backup in this account.")
        self.send(200, bytes(rows[0]["body"]), headers=[("X-Zenith-Saved-At", str(rows[0]["saved_at"]))])

    def delete_backup(self):
        account = self.account()
        self.server.store.write(lambda db: db.execute("DELETE FROM backups WHERE account_id=?", (account["id"],)))
        self.send(204)

    def redeem(self):
        account = self.account()
        self.limit("redeem", 10, 3600, key=str(account["id"]))
        asked = self.json_body()
        code = asked.get("code")
        code = "".join(ch for ch in code.upper() if ch.isalnum()) if isinstance(code, str) else ""
        if not CODE_RE.match(code):
            raise Refused(400, "bad_code", "That is not a Zenith code.")

        def work(db):
            if db.execute("SELECT 1 FROM redemptions WHERE redeemer_id=?", (account["id"],)).fetchone():
                raise Refused(409, "already", "You have already used a code.")
            owner = db.execute("SELECT id FROM accounts WHERE code=?", (code,)).fetchone()
            if owner is None:
                raise Refused(404, "unknown_code", "Nobody has that code.")
            if owner["id"] == account["id"]:
                raise Refused(422, "own_code", "That is your own code.")
            db.execute("INSERT INTO redemptions(redeemer_id, owner_id, at) VALUES (?,?,?)", (account["id"], owner["id"], now_ms()))

        self.server.store.write(work)
        self.answer(200, {"redeemed": True, "account": self.describe(account["id"])})

    # ---- Looks

    def query(self, name):
        values = urllib.parse.parse_qs(urllib.parse.urlsplit(self.path).query).get(name)
        return values[0] if values else None

    def share_look(self):
        account = self.account()
        self.limit("look-share", 30, 3600, key=str(account["id"]))
        raw = self.body(MAX_LOOK_BODY)
        try:
            value = json.loads(raw.decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            raise Refused(400, "bad_json", "A Look is JSON.")
        if not isinstance(value, dict) or value.get("format") != "zenith-look":
            raise Refused(400, "not_look", "That is not a Zenith Look.")
        name = value.get("name")
        name = name.strip()[:40] if isinstance(name, str) and name.strip() else "A Look"

        def work(db):
            code = new_look_code(db)
            db.execute("INSERT INTO looks(code, account_id, name, body, created) VALUES (?,?,?,?,?)",
                       (code, account["id"], name, raw, now_ms()))
            # An account keeps its newest Looks; the oldest give way, except the featured ones.
            # Two shared in the same millisecond are told apart by the order they came in, so the
            # one just shared is never the one to go.
            db.execute("DELETE FROM looks WHERE account_id=? AND featured=0 AND code NOT IN "
                       "(SELECT code FROM looks WHERE account_id=? ORDER BY created DESC, rowid DESC LIMIT ?)",
                       (account["id"], account["id"], MAX_LOOKS_PER_ACCOUNT))
            return code

        self.answer(201, {"code": self.server.store.write(work)})

    def get_look(self):
        # Anybody may open a Look by its code: that is what sharing one is for. Not by guessing.
        self.limit("look-get", 60, 60)
        code = look_code(self.query("code"))
        if not code:
            raise Refused(400, "bad_code", "That is not a Look's code.")
        rows = self.server.store.read("SELECT body FROM looks WHERE code=?", (code,))
        if not rows:
            raise Refused(404, "no_look", "Nobody has shared a Look with that code.")
        self.send(200, bytes(rows[0]["body"]))

    def featured_looks(self):
        self.limit("look-get", 60, 60)
        rows = self.server.store.read("SELECT code, name FROM looks WHERE featured=1 ORDER BY created DESC, rowid DESC LIMIT ?", (MAX_FEATURED,))
        self.answer(200, {"looks": [{"code": row["code"], "name": row["name"]} for row in rows]})

    def delete_look(self):
        account = self.account()
        code = look_code(self.query("code"))
        gone = self.server.store.write(lambda db: db.execute(
            "DELETE FROM looks WHERE code=? AND account_id=?", (code, account["id"])).rowcount)
        if not gone:
            raise Refused(404, "no_look", "This account has shared no Look with that code.")
        self.send(204)


ROUTES = {
    ("GET", "/"): Handler.front_page,
    ("GET", "/delete-account"): Handler.delete_page,
    ("GET", "/v1/health"): Handler.health,
    ("POST", "/v1/account"): Handler.create_account,
    ("DELETE", "/v1/account"): Handler.delete_account,
    ("POST", "/v1/session"): Handler.sign_in,
    ("POST", "/v1/session/google"): Handler.sign_in_google,
    ("DELETE", "/v1/session"): Handler.sign_out,
    ("GET", "/v1/me"): Handler.me,
    ("PUT", "/v1/backup"): Handler.put_backup,
    ("GET", "/v1/backup"): Handler.get_backup,
    ("DELETE", "/v1/backup"): Handler.delete_backup,
    ("POST", "/v1/aura/redeem"): Handler.redeem,
    ("POST", "/v1/looks"): Handler.share_look,
    ("GET", "/v1/look"): Handler.get_look,
    ("DELETE", "/v1/look"): Handler.delete_look,
    ("GET", "/v1/looks/featured"): Handler.featured_looks,
}


class Server(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True

    def __init__(self, address, db_path, google_client_id=None, trust_proxy=None, max_referrals=10, privacy_url=None, google=None):
        super().__init__(address, Handler)
        self.store = Store(db_path)
        self.limiter = Limiter()
        self.google = google or (GoogleVerifier(google_client_id) if google_client_id else None)
        self.trust_proxy = address[0] in ("127.0.0.1", "::1", "localhost") if trust_proxy is None else trust_proxy
        self.max_referrals = max_referrals
        self.privacy_url = privacy_url


def from_environment():
    host = os.environ.get("ZENITH_HOST", "127.0.0.1")
    trust = os.environ.get("ZENITH_TRUST_PROXY")
    return Server(
        (host, int(os.environ.get("ZENITH_PORT", "8787"))),
        os.environ.get("ZENITH_DB", "zenith.db"),
        google_client_id=os.environ.get("ZENITH_GOOGLE_CLIENT_ID") or None,
        trust_proxy=None if trust is None else trust == "1",
        max_referrals=int(os.environ.get("ZENITH_MAX_REFERRALS", "10")),
        privacy_url=os.environ.get("ZENITH_PRIVACY_URL") or None,
    )


# ----------------------------------------------------------------------------- trying it out

def call(base, method, path, body=None, token=None, raw=None):
    """One request, for the checks below. Returns (status, parsed JSON or bytes, headers)."""
    data = raw if raw is not None else (json.dumps(body).encode("utf-8") if body is not None else None)
    request = urllib.request.Request(base.rstrip("/") + path, data=data, method=method)
    request.add_header("User-Agent", "zenith-check/%d" % API_VERSION)
    if data is not None:
        request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            status, payload, headers = response.status, response.read(), response.headers
    except urllib.error.HTTPError as error:
        status, payload, headers = error.code, error.read(), error.headers
    if "application/json" in (headers.get("Content-Type") or "") and payload:
        try:
            return status, json.loads(payload.decode("utf-8")), headers
        except ValueError:
            pass
    return status, payload, headers


def check(base):
    """
    Tries a server from the outside, the way the app does, and tidies up after itself: the two
    throwaway accounts it makes are deleted again. Returns the number of things that went wrong.
    """
    failures = 0

    def expect(what, got, wanted):
        nonlocal failures
        fine = got == wanted
        failures += 0 if fine else 1
        print("%s  %s%s" % ("PASS" if fine else "FAIL", what, "" if fine else "  (got %r, wanted %r)" % (got, wanted)))

    tag = secrets.token_hex(4)
    first, second, password = "check_a_" + tag, "check_b_" + tag, "check-" + secrets.token_hex(8)

    status, health, _ = call(base, "GET", "/v1/health")
    expect("the server answers", (status, isinstance(health, dict) and health.get("ok")), (200, True))
    if status != 200:
        return failures + 1
    print("      Google sign-in: %s" % ("set up" if health.get("google") else "not set up (ZENITH_GOOGLE_CLIENT_ID is empty)"))

    status, made, _ = call(base, "POST", "/v1/account", {"username": first, "password": password})
    expect("an account can be made", status, 201)
    if status != 201:
        return failures + 1
    token, code = made["token"], made["account"]["code"]
    expect("the same name cannot be taken twice", call(base, "POST", "/v1/account", {"username": first, "password": password})[0], 409)
    expect("a wrong password is refused", call(base, "POST", "/v1/session", {"username": first, "password": password + "x"})[0], 401)
    status, signed, _ = call(base, "POST", "/v1/session", {"username": first, "password": password})
    expect("signing in works", status, 200)
    expect("no token, no entry", call(base, "GET", "/v1/me")[0], 401)
    status, me, _ = call(base, "GET", "/v1/me", token=token)
    expect("the account can be read", (status, me.get("account", {}).get("username") if isinstance(me, dict) else None), (200, first))

    backup = json.dumps({"format": "zenith-ui-backup", "version": 1, "settings": {}, "layout": []}).encode("utf-8")
    expect("there is no backup to begin with", call(base, "GET", "/v1/backup", token=token)[0], 404)
    expect("something that is not a backup is refused", call(base, "PUT", "/v1/backup", token=token, raw=b'{"format":"other"}')[0], 400)
    expect("a backup can be saved", call(base, "PUT", "/v1/backup", token=token, raw=backup)[0], 200)
    status, body, headers = call(base, "GET", "/v1/backup", token=token)
    expect("and comes back as it went", (status, body if isinstance(body, dict) else None), (200, json.loads(backup)))
    expect("with the time it was saved", (headers.get("X-Zenith-Saved-At") or "").isdigit(), True)

    status, other, _ = call(base, "POST", "/v1/account", {"username": second, "password": password})
    expect("a second account can be made", status, 201)
    if status == 201:
        friend = other["token"]
        expect("a code that nobody has is refused", call(base, "POST", "/v1/aura/redeem", {"code": "ZZZZZZ"}, token=friend)[0] in (404, 400), True)
        expect("your own code is refused", call(base, "POST", "/v1/aura/redeem", {"code": other["account"]["code"]}, token=friend)[0], 422)
        expect("a friend's code is taken", call(base, "POST", "/v1/aura/redeem", {"code": code}, token=friend)[0], 200)
        expect("but only once", call(base, "POST", "/v1/aura/redeem", {"code": code}, token=friend)[0], 409)
        status, me, _ = call(base, "GET", "/v1/me", token=token)
        expect("and the owner of the code is told", me.get("account", {}).get("referrals") if isinstance(me, dict) else None, 1)
        expect("the second account can be deleted", call(base, "DELETE", "/v1/account", {"password": password}, token=friend)[0], 204)

    look = {"format": "zenith-look", "version": 1, "name": "Check", "settings": {}, "layout": []}
    expect("something that is not a Look is not shared", call(base, "POST", "/v1/looks", token=token, raw=b'{"format":"other"}')[0], 400)
    expect("a Look cannot be shared without an account", call(base, "POST", "/v1/looks", raw=json.dumps(look).encode("utf-8"))[0], 401)
    status, shared, _ = call(base, "POST", "/v1/looks", token=token, raw=json.dumps(look).encode("utf-8"))
    expect("a Look can be shared", status, 201)
    look_code_given = shared.get("code") if isinstance(shared, dict) else None
    status, opened, _ = call(base, "GET", "/v1/look?code=LOOK-" + (look_code_given or ""))
    expect("and opened by its code, with no account", (status, opened if isinstance(opened, dict) else None), (200, look))
    expect("a code nobody has opens nothing", call(base, "GET", "/v1/look?code=ZZZZZZ")[0], 404)
    status, featured, _ = call(base, "GET", "/v1/looks/featured")
    expect("the featured Looks can be listed", (status, isinstance(featured, dict) and isinstance(featured.get("looks"), list)), (200, True))
    expect("a shared Look can be taken back", call(base, "DELETE", "/v1/look?code=" + (look_code_given or ""), token=token)[0], 204)
    expect("and is gone", call(base, "GET", "/v1/look?code=" + (look_code_given or ""))[0], 404)

    expect("signing out ends that sign-in", call(base, "DELETE", "/v1/session", token=signed.get("token") if isinstance(signed, dict) else None)[0], 204)
    expect("deleting needs the password", call(base, "DELETE", "/v1/account", {"password": "wrong-" + password}, token=token)[0], 401)
    expect("the account can be deleted", call(base, "DELETE", "/v1/account", {"password": password}, token=token)[0], 204)
    expect("and is gone", call(base, "GET", "/v1/me", token=token)[0], 401)
    expect("the page for deleting an account is there", call(base, "GET", "/delete-account")[0], 200)
    return failures


def selftest():
    folder = tempfile.mkdtemp(prefix="zenith-selftest-")
    server = Server(("127.0.0.1", 0), os.path.join(folder, "selftest.db"))
    # The checks make accounts faster than any one person would.
    server.limiter.allow = lambda key, limit, seconds: True
    threading.Thread(target=server.serve_forever, daemon=True).start()
    try:
        return check("http://127.0.0.1:%d" % server.server_address[1])
    finally:
        server.shutdown()
        server.server_close()
        shutil.rmtree(folder, ignore_errors=True)


def main(arguments):
    if arguments[:1] == ["--selftest"]:
        failures = selftest()
        print("selftest: %s" % ("all passed" if failures == 0 else "%d FAILED" % failures))
        return 1 if failures else 0
    if arguments[:1] == ["--check"] and len(arguments) == 2:
        failures = check(arguments[1])
        print("check of %s: %s" % (arguments[1], "all passed" if failures == 0 else "%d FAILED" % failures))
        return 1 if failures else 0
    if arguments[:1] in (["--feature"], ["--unfeature"]) and len(arguments) == 2:
        code = look_code(arguments[1])
        store = Store(os.environ.get("ZENITH_DB", "zenith.db"))
        changed = store.write(lambda db: db.execute(
            "UPDATE looks SET featured=? WHERE code=?", (1 if arguments[0] == "--feature" else 0, code)).rowcount)
        print("%s %s" % ("featured" if arguments[0] == "--feature" else "unfeatured", code) if changed else "no Look has the code %r" % arguments[1])
        return 0 if changed else 1
    if arguments:
        print(__doc__)
        return 2
    server = from_environment()
    sys.stderr.write("zenith: listening on %s:%d, database %s, Google sign-in %s\n" % (
        server.server_address[0], server.server_address[1], server.store.path, "on" if server.google else "off"))
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
