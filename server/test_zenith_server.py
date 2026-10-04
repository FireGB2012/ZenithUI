"""
Tests for zenith_server.py. Run from this folder:  python -m unittest -v

They start the real server on a spare port with a database in a temporary folder and talk to it
over HTTP, the way the app does. Google's part is tried with keys made for the purpose
(test_keys.py), handed to the server in place of Google's own.
"""

import base64
import hashlib
import json
import os
import shutil
import sqlite3
import tempfile
import threading
import time
import unittest
import urllib.request

import test_keys as keys
import zenith_server as zenith

CLIENT_ID = "test-client.apps.googleusercontent.com"


def b64(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def sign(message, n=keys.N, d=keys.D):
    """RSASSA-PKCS1-v1_5 with SHA-256, the way Google signs: built here so the server's check has something real to open."""
    size = (n.bit_length() + 7) // 8
    tail = bytes.fromhex("3031300d060960864801650304020105000420") + hashlib.sha256(message).digest()
    block = b"\x00\x01" + b"\xff" * (size - len(tail) - 3) + b"\x00" + tail
    return pow(int.from_bytes(block, "big"), d, n).to_bytes(size, "big")


def token(claims=None, header=None, n=keys.N, d=keys.D, **changes):
    head = {"alg": "RS256", "kid": "test-key", "typ": "JWT"}
    head.update(header or {})
    body = {"iss": "https://accounts.google.com", "aud": CLIENT_ID, "sub": "1234567890", "iat": int(time.time()), "exp": int(time.time()) + 3600}
    body.update(claims or {})
    body.update(changes)
    body = {name: value for name, value in body.items() if value is not None}
    signed = b64(json.dumps(head).encode()) + "." + b64(json.dumps(body).encode())
    return signed + "." + b64(sign(signed.encode("ascii"), n, d))


def jwks(n=keys.N, kid="test-key"):
    return {"keys": [{"kty": "RSA", "alg": "RS256", "use": "sig", "kid": kid,
                      "n": b64(n.to_bytes((n.bit_length() + 7) // 8, "big")), "e": b64((65537).to_bytes(3, "big"))}]}


class SignatureTest(unittest.TestCase):

    def test_a_signature_made_by_openssl_is_accepted(self):
        self.assertTrue(zenith.rsa_sha256_verify(keys.N, keys.E, keys.MESSAGE, keys.OPENSSL_SIGNATURE))

    def test_our_own_signer_agrees_with_openssl(self):
        self.assertEqual(keys.OPENSSL_SIGNATURE, sign(keys.MESSAGE))

    def test_a_changed_message_a_changed_signature_and_another_key_are_refused(self):
        self.assertFalse(zenith.rsa_sha256_verify(keys.N, keys.E, keys.MESSAGE + b" ", keys.OPENSSL_SIGNATURE))
        flipped = bytearray(keys.OPENSSL_SIGNATURE)
        flipped[40] ^= 1
        self.assertFalse(zenith.rsa_sha256_verify(keys.N, keys.E, keys.MESSAGE, bytes(flipped)))
        self.assertFalse(zenith.rsa_sha256_verify(keys.OTHER_N, keys.E, keys.MESSAGE, keys.OPENSSL_SIGNATURE))

    def test_a_signature_of_the_wrong_length_or_too_large_is_refused(self):
        self.assertFalse(zenith.rsa_sha256_verify(keys.N, keys.E, keys.MESSAGE, keys.OPENSSL_SIGNATURE[1:]))
        self.assertFalse(zenith.rsa_sha256_verify(keys.N, keys.E, keys.MESSAGE, b"\x00" + keys.OPENSSL_SIGNATURE))
        self.assertFalse(zenith.rsa_sha256_verify(keys.N, keys.E, keys.MESSAGE, b""))
        too_large = (keys.N + 5).to_bytes(256, "big")
        self.assertFalse(zenith.rsa_sha256_verify(keys.N, keys.E, keys.MESSAGE, too_large))

    def test_padding_that_only_looks_right_is_refused(self):
        # The old forgery: a short run of FF, the digest, then rubbish to fill the block. A check
        # that parses the block and stops after the digest lets it through; this one must not.
        size = 256
        tail = bytes.fromhex("3031300d060960864801650304020105000420") + hashlib.sha256(keys.MESSAGE).digest()
        short = b"\x00\x01" + b"\xff" * 8 + b"\x00" + tail
        block = short + b"\x55" * (size - len(short))
        forged = pow(int.from_bytes(block, "big"), keys.D, keys.N).to_bytes(size, "big")
        self.assertFalse(zenith.rsa_sha256_verify(keys.N, keys.E, keys.MESSAGE, forged))


class GoogleVerifierTest(unittest.TestCase):

    def verifier(self, **options):
        self.fetches = 0

        def fetch():
            self.fetches += 1
            return options.get("certs", jwks())

        return zenith.GoogleVerifier(CLIENT_ID, fetch=fetch, clock=options.get("clock", time.time))

    def refused(self, bad, said):
        with self.assertRaises(ValueError) as raised:
            self.verifier().verify(bad)
        self.assertEqual(said, str(raised.exception))

    def test_a_token_google_signed_for_this_app_is_accepted(self):
        claims = self.verifier().verify(token(sub="42"))
        self.assertEqual("42", claims["sub"])
        self.assertEqual("42", self.verifier().verify(token(sub="42", iss="accounts.google.com"))["sub"])

    def test_a_token_signed_by_anyone_else_is_refused(self):
        self.refused(token(n=keys.OTHER_N, d=keys.OTHER_D), "bad signature")

    def test_a_token_changed_after_signing_is_refused(self):
        head, body, signature = token(sub="42").split(".")
        other = b64(json.dumps({"iss": "https://accounts.google.com", "aud": CLIENT_ID, "sub": "43", "exp": int(time.time()) + 3600}).encode())
        self.refused(head + "." + other + "." + signature, "bad signature")

    def test_a_token_may_not_choose_how_it_is_checked(self):
        unsigned = b64(json.dumps({"alg": "none", "kid": "test-key"}).encode()) + "." + token().split(".")[1] + "."
        self.refused(unsigned, "wrong algorithm")
        self.refused(token(header={"alg": "HS256"}), "wrong algorithm")
        self.refused(token(header={"alg": "RS512"}), "wrong algorithm")

    def test_a_token_for_another_app_or_from_another_issuer_is_refused(self):
        self.refused(token(aud="someone-else.apps.googleusercontent.com"), "meant for another app")
        self.refused(token(aud=[CLIENT_ID, "x"]), "meant for another app")
        self.refused(token(iss="https://accounts.example.com"), "wrong issuer")

    def test_a_token_out_of_date_or_from_the_future_is_refused(self):
        self.refused(token(exp=int(time.time()) - 120), "out of date")
        self.refused(token(exp=None), "out of date")
        self.refused(token(iat=int(time.time()) + 3600), "from the future")

    def test_a_token_with_nobody_in_it_is_refused(self):
        self.refused(token(sub=None), "no subject")
        self.refused(token(sub=""), "no subject")
        self.refused(token(sub=12345), "no subject")

    def test_things_that_are_not_tokens_are_refused(self):
        for bad in (None, 12, "", "a.b", "a.b.c.d", "!!.!!.!!", "e30.e30.e30", "W10.W10.AAAA", "x" * 9000 + ".a.b"):
            with self.assertRaises(ValueError, msg=repr(bad)[:40]):
                self.verifier().verify(bad)

    def test_a_key_google_does_not_have_is_refused_without_asking_again_at_once(self):
        verifier = self.verifier()
        for _ in range(5):
            with self.assertRaises(ValueError) as raised:
                verifier.verify(token(header={"kid": "made-up"}))
            self.assertEqual("unknown key", str(raised.exception))
        self.assertEqual(1, self.fetches)

    def test_new_keys_are_fetched_when_google_changes_them(self):
        moment = [1_000_000.0]
        current = {"certs": jwks(kid="old")}
        fetches = []

        def fetch():
            fetches.append(1)
            return current["certs"]

        verifier = zenith.GoogleVerifier(CLIENT_ID, fetch=fetch, clock=lambda: moment[0])
        fresh = lambda kid: token(header={"kid": kid}, iat=int(moment[0]), exp=int(moment[0]) + 3600)
        verifier.verify(fresh("old"))
        current["certs"] = jwks(kid="new")
        moment[0] += 30
        with self.assertRaises(ValueError):
            verifier.verify(fresh("new"))  # Asked less than a minute ago: not again yet.
        moment[0] += 61
        self.assertEqual("1234567890", verifier.verify(fresh("new"))["sub"])
        self.assertEqual(2, len(fetches))


class LimiterTest(unittest.TestCase):

    def test_so_many_calls_in_so_many_seconds_and_no_more(self):
        moment = [0.0]
        limiter = zenith.Limiter(clock=lambda: moment[0])
        self.assertEqual([True, True, True, False], [limiter.allow("k", 3, 60) for _ in range(4)])
        self.assertTrue(limiter.allow("other", 3, 60))
        moment[0] = 61
        self.assertTrue(limiter.allow("k", 3, 60))


class PasswordTest(unittest.TestCase):

    def test_a_password_is_kept_as_a_salted_hash_and_checks_out(self):
        first, second = zenith.hash_password("correct horse"), zenith.hash_password("correct horse")
        self.assertNotEqual(first, second)
        self.assertNotIn("correct", first)
        self.assertTrue(zenith.check_password("correct horse", first))
        self.assertFalse(zenith.check_password("correct horsf", first))
        self.assertFalse(zenith.check_password("correct horse", "rubbish"))
        self.assertFalse(zenith.check_password("correct horse", ""))


class ApiTest(unittest.TestCase):
    """Each test gets a server of its own, with nothing in it."""

    google = True

    def setUp(self):
        self.folder = tempfile.mkdtemp(prefix="zenith-test-")
        verifier = zenith.GoogleVerifier(CLIENT_ID, fetch=lambda: jwks()) if self.google else None
        self.server = zenith.Server(("127.0.0.1", 0), os.path.join(self.folder, "test.db"), google=verifier)
        self.server.RequestHandlerClass.log_message = lambda *args: None
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.base = "http://127.0.0.1:%d" % self.server.server_address[1]

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        shutil.rmtree(self.folder, ignore_errors=True)

    def call(self, method, path, body=None, token=None, raw=None, headers=None):
        if headers is None:
            return zenith.call(self.base, method, path, body, token, raw)[:2]
        request = urllib.request.Request(self.base + path, method=method, headers=headers,
                                         data=json.dumps(body).encode() if body is not None else None)
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                return response.status, json.loads(response.read() or b"null")
        except urllib.error.HTTPError as error:
            return error.code, json.loads(error.read() or b"null")

    def unlimited(self):
        self.server.limiter.allow = lambda key, limit, seconds: True

    def account(self, username="gabriel", password="password-1", code=None):
        status, made = self.call("POST", "/v1/account", {"username": username, "password": password, "code": code})
        self.assertEqual(201, status, made)
        return made["token"], made["account"]

    def database(self):
        return sqlite3.connect(os.path.join(self.folder, "test.db"))

    # ---- accounts

    def test_a_new_account_starts_signed_in_with_a_code_of_its_own(self):
        token_, account = self.account()
        self.assertEqual("gabriel", account["username"])
        self.assertFalse(account["google"])
        self.assertRegex(account["code"], zenith.CODE_RE)
        self.assertEqual((0, 10, False, None), (account["referrals"], account["referralCap"], account["redeemed"], account["backupAt"]))
        self.assertEqual((200, {"account": account}), self.call("GET", "/v1/me", token=token_))

    def test_the_code_the_app_already_shows_is_kept_if_nobody_has_it(self):
        self.assertEqual("WTR4RT", self.account("one", code="WTR4RT")[1]["code"])
        # Taken, or not a code at all: a fresh one is made.
        self.assertNotEqual("WTR4RT", self.account("two", code="WTR4RT")[1]["code"])
        self.assertRegex(self.account("three", code="AEIOU1")[1]["code"], zenith.CODE_RE)
        self.assertRegex(self.account("four", code=12)[1]["code"], zenith.CODE_RE)

    def test_usernames_and_passwords_have_to_be_reasonable(self):
        self.unlimited()
        for username in ("ab", "Gabriel", "has space", "x" * 21, "émile", "", None, 5):
            status, said = self.call("POST", "/v1/account", {"username": username, "password": "password-1"})
            self.assertEqual((400, "bad_username"), (status, said["error"]), username)
        for password in ("short", "", None, "x" * 201, 12345678):
            status, said = self.call("POST", "/v1/account", {"username": "fine_name", "password": password})
            self.assertEqual((400, "bad_password"), (status, said["error"]), password)

    def test_a_username_can_be_taken_only_once(self):
        self.account()
        status, said = self.call("POST", "/v1/account", {"username": "gabriel", "password": "another-password"})
        self.assertEqual((409, "taken"), (status, said["error"]))

    def test_the_password_is_not_kept(self):
        self.account(password="very-secret-words")
        dump = "\n".join(self.database().iterdump())
        self.assertNotIn("very-secret-words", dump)
        self.assertIn("scrypt$", dump)

    def test_signing_in_takes_the_right_password_and_nothing_else(self):
        self.account()
        status, signed = self.call("POST", "/v1/session", {"username": "gabriel", "password": "password-1"})
        self.assertEqual((200, "gabriel"), (status, signed["account"]["username"]))
        self.assertEqual(200, self.call("GET", "/v1/me", token=signed["token"])[0])
        self.assertEqual(200, self.call("POST", "/v1/session", {"username": "  Gabriel ", "password": "password-1"})[0])
        wrong_password = self.call("POST", "/v1/session", {"username": "gabriel", "password": "password-2"})
        nobody = self.call("POST", "/v1/session", {"username": "nobody", "password": "password-1"})
        # The two cannot be told apart, so the answer does not say which names exist.
        self.assertEqual(wrong_password, nobody)
        self.assertEqual(401, nobody[0])
        self.assertEqual(400, self.call("POST", "/v1/session", {"username": "gabriel"})[0])

    def test_a_google_account_cannot_be_signed_in_to_with_a_password(self):
        self.call("POST", "/v1/session/google", {"idToken": token()})
        self.database().execute("UPDATE accounts SET username='viagoogle'").connection.commit()
        self.assertEqual(401, self.call("POST", "/v1/session", {"username": "viagoogle", "password": ""})[0])
        self.assertEqual(401, self.call("POST", "/v1/session", {"username": "viagoogle", "password": "password-1"})[0])

    def test_signing_out_ends_that_sign_in_and_no_other(self):
        first, _ = self.account()
        second = self.call("POST", "/v1/session", {"username": "gabriel", "password": "password-1"})[1]["token"]
        self.assertEqual(204, self.call("DELETE", "/v1/session", token=first)[0])
        self.assertEqual(401, self.call("GET", "/v1/me", token=first)[0])
        self.assertEqual(200, self.call("GET", "/v1/me", token=second)[0])

    def test_nothing_gets_in_without_a_token_that_is_known(self):
        self.account()
        for method, path in (("GET", "/v1/me"), ("DELETE", "/v1/session"), ("DELETE", "/v1/account"), ("GET", "/v1/backup"),
                             ("PUT", "/v1/backup"), ("DELETE", "/v1/backup"), ("POST", "/v1/aura/redeem")):
            self.assertEqual(401, self.call(method, path)[0], path)
            self.assertEqual(401, self.call(method, path, token="made-up-token")[0], path)
            self.assertEqual(401, self.call(method, path, headers={"Authorization": "Basic Z2FicmllbDpwYXNzd29yZC0x"})[0], path)

    def test_tokens_are_kept_only_as_hashes(self):
        token_, _ = self.account()
        self.assertNotIn(token_, "\n".join(self.database().iterdump()))

    def test_a_sign_in_left_unused_for_long_enough_ends(self):
        token_, _ = self.account()
        long_ago = zenith.now_ms() - (zenith.SESSION_IDLE_SECONDS + 60) * 1000
        self.database().execute("UPDATE sessions SET last_used=?", (long_ago,)).connection.commit()
        self.assertEqual(401, self.call("GET", "/v1/me", token=token_)[0])

    def test_the_oldest_sign_ins_give_way_to_new_ones(self):
        self.unlimited()
        first, _ = self.account()
        self.database().execute("UPDATE sessions SET last_used=last_used-100000").connection.commit()
        for _ in range(zenith.MAX_SESSIONS_PER_ACCOUNT):
            self.assertEqual(200, self.call("POST", "/v1/session", {"username": "gabriel", "password": "password-1"})[0])
        self.assertEqual(401, self.call("GET", "/v1/me", token=first)[0])
        self.assertEqual(zenith.MAX_SESSIONS_PER_ACCOUNT, self.database().execute("SELECT COUNT(*) FROM sessions").fetchone()[0])

    def test_deleting_an_account_needs_its_password_and_takes_everything_with_it(self):
        token_, _ = self.account()
        self.call("PUT", "/v1/backup", token=token_, raw=b'{"format":"zenith-ui-backup"}')
        self.assertEqual(401, self.call("DELETE", "/v1/account", token=token_)[0])
        self.assertEqual(401, self.call("DELETE", "/v1/account", {"password": "wrong-one"}, token=token_)[0])
        self.assertEqual(200, self.call("GET", "/v1/me", token=token_)[0])
        self.assertEqual(204, self.call("DELETE", "/v1/account", {"password": "password-1"}, token=token_)[0])
        self.assertEqual(401, self.call("GET", "/v1/me", token=token_)[0])
        db = self.database()
        for table in ("accounts", "sessions", "backups"):
            self.assertEqual(0, db.execute("SELECT COUNT(*) FROM " + table).fetchone()[0], table)
        # The name is free again.
        self.account()

    # ---- Google

    def test_a_google_sign_in_makes_an_account_once_and_finds_it_after(self):
        status, first = self.call("POST", "/v1/session/google", {"idToken": token(sub="g-1"), "code": "WTR4RT"})
        self.assertEqual((200, True, None, True, "WTR4RT"),
                         (status, first["created"], first["account"]["username"], first["account"]["google"], first["account"]["code"]))
        status, again = self.call("POST", "/v1/session/google", {"idToken": token(sub="g-1"), "code": "BCDFGH"})
        self.assertEqual((200, False, "WTR4RT"), (status, again["created"], again["account"]["code"]))
        self.assertEqual(200, self.call("GET", "/v1/me", token=again["token"])[0])
        self.assertTrue(self.call("POST", "/v1/session/google", {"idToken": token(sub="g-2")})[1]["created"])

    def test_signing_in_to_a_google_account_that_exists_is_not_counted_as_making_one(self):
        self.call("POST", "/v1/session/google", {"idToken": token(sub="g-1")})
        answers = [self.call("POST", "/v1/session/google", {"idToken": token(sub="g-1")})[0] for _ in range(3)]
        self.assertEqual([200] * 3, answers)
        # New ones are: five an hour from one address, as with usernames. (Ten calls in all here,
        # which is as many sign-ins as one address may make in a minute.)
        answers = [self.call("POST", "/v1/session/google", {"idToken": token(sub="new-%d" % index)})[0] for index in range(6)]
        self.assertEqual([200] * 4 + [429] * 2, answers)

    def test_nothing_of_google_s_is_kept_but_the_number(self):
        self.call("POST", "/v1/session/google", {"idToken": token(sub="g-1", email="someone@gmail.com", name="Some One")})
        dump = "\n".join(self.database().iterdump())
        self.assertIn("g-1", dump)
        self.assertNotIn("someone@gmail.com", dump)
        self.assertNotIn("Some One", dump)

    def test_a_token_google_did_not_sign_gets_nowhere(self):
        for bad in (token(n=keys.OTHER_N, d=keys.OTHER_D), token(aud="other"), token(exp=1), "nonsense", None, 7):
            status, said = self.call("POST", "/v1/session/google", {"idToken": bad})
            self.assertEqual((401, "wrong"), (status, said["error"]), str(bad)[:30])
        self.assertEqual(0, self.database().execute("SELECT COUNT(*) FROM accounts").fetchone()[0])

    def test_the_delete_page_can_sign_in_without_making_an_account(self):
        status, said = self.call("POST", "/v1/session/google", {"idToken": token(sub="g-9"), "create": False})
        self.assertEqual((404, "no_account"), (status, said["error"]))
        self.assertEqual(0, self.database().execute("SELECT COUNT(*) FROM accounts").fetchone()[0])

    def test_a_google_account_is_deleted_without_a_password(self):
        signed = self.call("POST", "/v1/session/google", {"idToken": token(sub="g-1")})[1]
        self.assertEqual(204, self.call("DELETE", "/v1/account", token=signed["token"])[0])
        self.assertEqual(0, self.database().execute("SELECT COUNT(*) FROM accounts").fetchone()[0])

    # ---- backups

    def test_a_backup_comes_back_exactly_as_it_went_and_the_newest_replaces_the_last(self):
        token_, _ = self.account()
        first = '{"format":"zenith-ui-backup","version":1,"settings":{"userName":"Gabriel \\u00e6"},"layout":[]}'.encode()
        status, saved = self.call("PUT", "/v1/backup", token=token_, raw=first)
        self.assertEqual(200, status)
        self.assertEqual(saved["savedAt"], self.call("GET", "/v1/me", token=token_)[1]["account"]["backupAt"])
        status, body, headers = zenith.call(self.base, "GET", "/v1/backup", token=token_)
        self.assertEqual((200, json.loads(first), str(saved["savedAt"])), (status, body, headers["X-Zenith-Saved-At"]))
        with urllib.request.urlopen(urllib.request.Request(self.base + "/v1/backup", headers={"Authorization": "Bearer " + token_})) as raw:
            self.assertEqual(first, raw.read())
        second = b'{"format":"zenith-ui-backup","version":1,"layout":[1]}'
        self.call("PUT", "/v1/backup", token=token_, raw=second)
        self.assertEqual(json.loads(second), self.call("GET", "/v1/backup", token=token_)[1])
        self.assertEqual(1, self.database().execute("SELECT COUNT(*) FROM backups").fetchone()[0])

    def test_one_account_never_sees_another_s_backup(self):
        mine, _ = self.account("mine")
        theirs, _ = self.account("theirs")
        self.call("PUT", "/v1/backup", token=mine, raw=b'{"format":"zenith-ui-backup","who":"mine"}')
        self.assertEqual(404, self.call("GET", "/v1/backup", token=theirs)[0])
        self.assertEqual(204, self.call("DELETE", "/v1/backup", token=theirs)[0])
        self.assertEqual("mine", self.call("GET", "/v1/backup", token=mine)[1]["who"])

    def test_a_backup_can_be_deleted(self):
        token_, _ = self.account()
        self.call("PUT", "/v1/backup", token=token_, raw=b'{"format":"zenith-ui-backup"}')
        self.assertEqual(204, self.call("DELETE", "/v1/backup", token=token_)[0])
        self.assertEqual(404, self.call("GET", "/v1/backup", token=token_)[0])
        self.assertIsNone(self.call("GET", "/v1/me", token=token_)[1]["account"]["backupAt"])

    def test_only_a_zenith_backup_of_a_sensible_size_is_taken(self):
        token_, _ = self.account()
        for bad, error in ((b"not json", "bad_json"), (b"[1,2]", "not_backup"), (b'{"format":"other"}', "not_backup"), (b"\xff\xfe", "bad_json")):
            status, said = self.call("PUT", "/v1/backup", token=token_, raw=bad)
            self.assertEqual((400, error), (status, said["error"]), bad)
        huge = b'{"format":"zenith-ui-backup","pad":"' + b"x" * zenith.MAX_BACKUP_BODY + b'"}'
        self.assertEqual(413, self.call("PUT", "/v1/backup", token=token_, raw=huge)[0])
        self.assertEqual(404, self.call("GET", "/v1/backup", token=token_)[0])

    # ---- codes

    def test_a_friend_s_code_counts_once_for_each_friend(self):
        self.unlimited()
        owner, account = self.account("owner", code="WTR4RT")
        for name in ("friend_one", "friend_two"):
            friend, _ = self.account(name)
            status, said = self.call("POST", "/v1/aura/redeem", {"code": " wtr-4rt "}, token=friend)
            self.assertEqual((200, True, True), (status, said["redeemed"], said["account"]["redeemed"]))
            status, said = self.call("POST", "/v1/aura/redeem", {"code": "WTR4RT"}, token=friend)
            self.assertEqual((409, "already"), (status, said["error"]))
        self.assertEqual(2, self.call("GET", "/v1/me", token=owner)[1]["account"]["referrals"])

    def test_a_code_has_to_be_someone_else_s_and_real(self):
        mine, account = self.account("mine", code="WTR4RT")
        for code, status, error in ((account["code"], 422, "own_code"), ("BCDFGH", 404, "unknown_code"), ("AEIOU1", 400, "bad_code"),
                                    ("", 400, "bad_code"), (None, 400, "bad_code"), ("WTR4RTX", 400, "bad_code")):
            got, said = self.call("POST", "/v1/aura/redeem", {"code": code}, token=mine)
            self.assertEqual((status, error), (got, said["error"]), code)
        self.assertFalse(self.call("GET", "/v1/me", token=mine)[1]["account"]["redeemed"])

    def test_a_code_pays_for_so_many_friends_and_no_more(self):
        self.unlimited()
        self.server.max_referrals = 2
        owner, _ = self.account("owner", code="WTR4RT")
        for index in range(3):
            friend, _ = self.account("friend_%d" % index)
            self.assertEqual(200, self.call("POST", "/v1/aura/redeem", {"code": "WTR4RT"}, token=friend)[0])
        account = self.call("GET", "/v1/me", token=owner)[1]["account"]
        self.assertEqual((2, 2), (account["referrals"], account["referralCap"]))

    def test_a_code_that_was_counted_stays_counted_when_the_friend_leaves(self):
        owner, _ = self.account("owner", code="WTR4RT")
        friend, _ = self.account("friend")
        self.call("POST", "/v1/aura/redeem", {"code": "WTR4RT"}, token=friend)
        self.assertEqual(204, self.call("DELETE", "/v1/account", {"password": "password-1"}, token=friend)[0])
        self.assertEqual(1, self.call("GET", "/v1/me", token=owner)[1]["account"]["referrals"])
        # And the row left behind names nobody who has gone.
        self.assertEqual([(None,)], self.database().execute("SELECT redeemer_id FROM redemptions").fetchall())

    def test_a_friend_who_used_a_code_cannot_use_another_after_its_owner_leaves(self):
        owner, _ = self.account("owner", code="WTR4RT")
        other, _ = self.account("other", code="BCDFGH")
        friend, _ = self.account("friend")
        self.call("POST", "/v1/aura/redeem", {"code": "WTR4RT"}, token=friend)
        self.call("DELETE", "/v1/account", {"password": "password-1"}, token=owner)
        self.assertEqual(409, self.call("POST", "/v1/aura/redeem", {"code": "BCDFGH"}, token=friend)[0])
        self.assertTrue(self.call("GET", "/v1/me", token=friend)[1]["account"]["redeemed"])

    # ---- the edges

    def test_requests_are_only_taken_at_a_measured_pace(self):
        self.account()
        answers = [self.call("POST", "/v1/session", {"username": "gabriel", "password": "wrong-password"})[0] for _ in range(12)]
        self.assertEqual([401] * 10 + [429] * 2, answers)
        # Even the right password has to wait now.
        self.assertEqual(429, self.call("POST", "/v1/session", {"username": "gabriel", "password": "password-1"})[0])

    def test_accounts_cannot_be_made_by_the_hundred(self):
        answers = [self.call("POST", "/v1/account", {"username": "name_%d" % index, "password": "password-1"})[0] for index in range(7)]
        self.assertEqual([201] * 5 + [429] * 2, answers)

    def test_the_caller_is_who_our_own_web_server_says_and_not_who_the_caller_says(self):
        self.account()
        # Ten wrong tries from one address, each claiming to be someone new in front of the real one.
        for index in range(10):
            self.call("POST", "/v1/session", {"username": "user_%d" % index, "password": "wrong-password"},
                      headers={"Content-Type": "application/json", "X-Forwarded-For": "10.9.9.%d, 203.0.113.7" % index})
        status, _ = self.call("POST", "/v1/session", {"username": "gabriel", "password": "password-1"},
                              headers={"Content-Type": "application/json", "X-Forwarded-For": "1.2.3.4, 203.0.113.7"})
        self.assertEqual(429, status)
        # Someone else, behind the same web server, is not held up by them.
        status, _ = self.call("POST", "/v1/session", {"username": "gabriel", "password": "password-1"},
                              headers={"Content-Type": "application/json", "X-Forwarded-For": "203.0.113.8"})
        self.assertEqual(200, status)

    def test_a_caller_s_word_for_its_address_is_ignored_when_there_is_no_web_server_in_front(self):
        self.server.trust_proxy = False
        self.account()
        for index in range(10):
            self.call("POST", "/v1/session", {"username": "gabriel", "password": "wrong-password"},
                      headers={"Content-Type": "application/json", "X-Forwarded-For": "10.0.0.%d" % index})
        status, _ = self.call("POST", "/v1/session", {"username": "gabriel", "password": "password-1"},
                              headers={"Content-Type": "application/json", "X-Forwarded-For": "10.0.0.99"})
        self.assertEqual(429, status)

    def test_what_is_not_there_says_so_in_json(self):
        self.assertEqual((404, "no_such"), (lambda r: (r[0], r[1]["error"]))(self.call("GET", "/v1/nothing")))
        self.assertEqual((404, "no_such"), (lambda r: (r[0], r[1]["error"]))(self.call("GET", "/../etc/passwd")))
        self.assertEqual((405, "no_such"), (lambda r: (r[0], r[1]["error"]))(self.call("GET", "/v1/account")))
        self.assertEqual(405, self.call("POST", "/v1/health", {})[0])

    def test_bodies_have_to_be_json_objects_of_a_sensible_size(self):
        for raw, status in ((b"{not json", 400), (b"[]", 400), (b'"text"', 400), (b"x" * (zenith.MAX_JSON_BODY + 1), 413)):
            self.assertEqual(status, self.call("POST", "/v1/session", raw=raw)[0], raw[:20])

    def test_an_answer_gets_through_even_when_the_request_was_never_read(self):
        # Refused for the pace alone, before the body is looked at, with a body big enough that
        # leaving it unread would end the connection in a reset on its way back.
        self.server.limiter.allow = lambda key, limit, seconds: False
        big = b'{"pad":"' + b"x" * 300_000 + b'"}'
        for _ in range(5):
            status, said = self.call("PUT", "/v1/backup", token="whatever", raw=big)
            self.assertEqual((429, "slow_down"), (status, said["error"]))

    def test_health_says_what_is_switched_on(self):
        self.assertEqual((200, {"ok": True, "name": "zenith", "version": 1, "google": True}), self.call("GET", "/v1/health"))

    def test_the_pages_are_served_and_locked_down(self):
        with urllib.request.urlopen(self.base + "/") as page:
            self.assertIn("Zenith UI", page.read().decode())
        with urllib.request.urlopen(self.base + "/delete-account") as page:
            text, policy = page.read().decode(), page.headers["Content-Security-Policy"]
        nonce = policy.split("'nonce-")[1].split("'")[0]
        self.assertIn('<script nonce="%s">' % nonce, text)
        self.assertIn("default-src 'none'", policy)
        self.assertIn("frame-ancestors 'none'", policy)
        self.assertIn(CLIENT_ID, text)
        # A new one every time, or it is not a nonce.
        with urllib.request.urlopen(self.base + "/delete-account") as page:
            self.assertNotIn(nonce, page.headers["Content-Security-Policy"])


class WithoutGoogleTest(ApiTest):
    """The same server with no Google client ID: everything else must work, and Google must say why it does not."""

    google = False

    def test_google_sign_in_says_it_is_not_set_up(self):
        status, said = self.call("POST", "/v1/session/google", {"idToken": token()})
        self.assertEqual((503, "no_google"), (status, said["error"]))
        self.assertFalse(self.call("GET", "/v1/health")[1]["google"])
        with urllib.request.urlopen(self.base + "/delete-account") as page:
            self.assertNotIn("accounts.google.com/gsi", page.read().decode())

    # These need Google; the rest of ApiTest runs here unchanged.
    test_a_google_sign_in_makes_an_account_once_and_finds_it_after = None
    test_signing_in_to_a_google_account_that_exists_is_not_counted_as_making_one = None
    test_nothing_of_google_s_is_kept_but_the_number = None
    test_a_token_google_did_not_sign_gets_nowhere = None
    test_the_delete_page_can_sign_in_without_making_an_account = None
    test_a_google_account_is_deleted_without_a_password = None
    test_a_google_account_cannot_be_signed_in_to_with_a_password = None
    test_health_says_what_is_switched_on = None
    test_the_pages_are_served_and_locked_down = None


if __name__ == "__main__":
    unittest.main()
