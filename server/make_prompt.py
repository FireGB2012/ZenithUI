"""
Writes PROMPT-FOR-SERVER-AI.md: the message to send to the AI that looks after the server, with
zenith_server.py inside it, exactly as it is here. Run it again whenever the server changes:

    python make_prompt.py
"""

import hashlib
import io
import os

HERE = os.path.dirname(os.path.abspath(__file__))

TEMPLATE = '''You are setting up a small backend on this server for **Zenith UI**, an Android home screen app that I (the owner of this server) make. Another AI wrote the server program below and tested it against the app. Your job is to install it **exactly as it is**, put HTTPS in front of it, check that it works, and give me a short report which I will hand back to that AI.

Please do not rewrite, reformat or "improve" the program: the app is built against it byte for byte. If you think something has to change, tell me instead of changing it.

## What you are installing

- One Python file, standard library only (Python 3.9 or newer). No pip, no virtualenv, no Docker needed.
- It keeps everything in one SQLite file.
- It listens on `127.0.0.1:8787` only. A web server in front of it does the HTTPS.
- It does four things for the app: accounts (username and password, or Google), one backup per account, referral codes, and Looks (home screen designs people share with each other by a short code).

## Before you touch anything

1. Look at what is here: the OS and version, `python3 --version`, what is already listening on ports 80 and 443, which web server (if any) is installed, and what the firewall allows. If this server already hosts other sites, they must keep working: add to what is there, do not replace it.
2. A **hostname** is needed, with a DNS record pointing at this server (for example `zenith.example.com`). Android apps only talk to HTTPS with a certificate that phones trust already, so: no plain HTTP, no self-signed certificate, no bare IP address. If I have not told you a hostname, ask me. If I have no domain at all, tell me the cheapest ways to get one (a domain I buy, or a free dynamic DNS name such as DuckDNS that works with Let's Encrypt) and wait for my answer.

## Install

Adapt paths and commands to this system, but keep the shape.

3. Make a system user with no login, and two folders:
   - user `zenith`
   - `/opt/zenith` for the program (owned by root, readable by `zenith`)
   - `/var/lib/zenith` for the database (owned by `zenith`, mode `700`)
4. Save the program at the end of this message as `/opt/zenith/zenith_server.py`. Then check it arrived whole:
   ```
   sha256sum /opt/zenith/zenith_server.py
   ```
   It must print `@SHA256@`. If it does not, the copy was damaged (line endings, a missing last line, trailing spaces): fix the copy. Do not go on with a file that does not match. The file ends with one newline after `sys.exit(main(sys.argv[1:]))`.
5. Try it, as the `zenith` user:
   ```
   sudo -u zenith python3 /opt/zenith/zenith_server.py --selftest
   ```
   Every line must start with `PASS` and the last line must be `selftest: all passed`.
6. Settings go in `/etc/zenith.env` (owned by root, mode `640`, group `zenith`):
   ```
   ZENITH_DB=/var/lib/zenith/zenith.db
   ZENITH_HOST=127.0.0.1
   ZENITH_PORT=8787
   ZENITH_GOOGLE_CLIENT_ID=
   ZENITH_PRIVACY_URL=
   ```
   If port 8787 is taken, pick another free one and use it everywhere below. Leave `ZENITH_GOOGLE_CLIENT_ID` empty for now (see "Later").
7. Run it as a service. With systemd, `/etc/systemd/system/zenith.service`:
   ```
   [Unit]
   Description=Zenith UI server
   After=network-online.target
   Wants=network-online.target

   [Service]
   User=zenith
   Group=zenith
   EnvironmentFile=/etc/zenith.env
   ExecStart=/usr/bin/python3 /opt/zenith/zenith_server.py
   Restart=on-failure
   RestartSec=2
   NoNewPrivileges=true
   PrivateTmp=true
   ProtectSystem=strict
   ProtectHome=true
   ReadWritePaths=/var/lib/zenith
   CapabilityBoundingSet=
   RestrictAddressFamilies=AF_INET AF_INET6 AF_UNIX
   MemoryMax=300M

   [Install]
   WantedBy=multi-user.target
   ```
   Then `systemctl daemon-reload && systemctl enable --now zenith`, and check:
   ```
   curl -s http://127.0.0.1:8787/v1/health
   ```
   It should answer `{"ok":true,"name":"zenith","version":1,"google":false}`.

## HTTPS in front of it

8. If no web server is installed, install **Caddy** and give it this (it gets and renews the certificate by itself):
   ```
   zenith.example.com {
       encode gzip
       request_body {
           max_size 1MB
       }
       reverse_proxy 127.0.0.1:8787
   }
   ```
   If nginx, Apache, Traefik or Caddy is already serving other sites, add one more site for this hostname instead, with a certificate (certbot, or whatever is already used here). For nginx the site needs:
   ```
   client_max_body_size 1m;
   location / {
       proxy_pass http://127.0.0.1:8787;
       proxy_set_header Host $host;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
   }
   ```
   Whichever web server it is, these four things must be true:
   - plain HTTP redirects to HTTPS, and the certificate renews by itself;
   - the web server adds the caller's address to the `X-Forwarded-For` header (the program limits how fast one address may try passwords, and reads the **last** address in that header);
   - request bodies up to 1 MB are let through;
   - request bodies are not written to any log.
9. The firewall lets 80 and 443 in. Port 8787 must **not** be reachable from outside: the program believes `X-Forwarded-For` only because nothing but the local web server can reach it. Check with `ss -ltnp | grep 8787` that it is bound to `127.0.0.1` only.

## Check it from outside

10. Run this against the real address:
    ```
    sudo -u zenith python3 /opt/zenith/zenith_server.py --check https://zenith.example.com
    ```
    It makes two throwaway accounts, tries everything the app does, and deletes them again. Every line must say `PASS` and the last line must be `check of https://... : all passed`.
    Also open `https://zenith.example.com/` and `https://zenith.example.com/delete-account` and confirm both pages load.

## Keep it safe

11. Copy the database somewhere once a day and keep two weeks of copies. A systemd timer or cron job running something like this is enough:
    ```
    sqlite3 /var/lib/zenith/zenith.db ".backup '/var/backups/zenith/zenith-$(date +%F).db'"
    find /var/backups/zenith -name 'zenith-*.db' -mtime +14 -delete
    ```
    (If the `sqlite3` command is not installed: `python3 -c "import sqlite3,sys; s=sqlite3.connect(sys.argv[1]); d=sqlite3.connect(sys.argv[2]); s.backup(d)" SOURCE DESTINATION` does the same.) The backup folder must be readable by root only.
12. Do not open any other port, do not expose the database file, and do not delete or move `/var/lib/zenith/zenith.db` once the service has run: it holds people's accounts.

## Later

- **Google sign-in.** When I give you a Google "Web application" client ID (it ends in `.apps.googleusercontent.com`), put it in `/etc/zenith.env` as `ZENITH_GOOGLE_CLIENT_ID=...`, run `systemctl restart zenith`, and check that `/v1/health` now says `"google":true`. The server then needs to be able to reach `https://www.googleapis.com` going out.
- **Updating.** I may send a newer `zenith_server.py` with a new checksum. Replace the file, check the checksum, run `--selftest`, `systemctl restart zenith`, run `--check` again. The database stays where it is.
- **Featured Looks.** The app lists a few Looks for everybody to try, and only ones I have chosen. When I send you a code such as `LOOK-BCD234`, run `sudo -u zenith env ZENITH_DB=/var/lib/zenith/zenith.db python3 /opt/zenith/zenith_server.py --feature LOOK-BCD234` (and `--unfeature` the same way to take one off). Never feature a Look I did not name.
- **Looking at it.** `journalctl -u zenith` shows one line for each request (method, path, status) and never a password, a token or a request body.

## What I need back from you

When it is all working, send me this, filled in. I will pass it on as it is.

```
ZENITH SERVER REPORT
Server address:        https://...
Health answer:         (what `curl -s https://HOSTNAME/v1/health` printed)
Check result:          (the last line that --check printed)
Program checksum:      (what sha256sum printed)
OS and Python:         ...
Web server and cert:   (which web server; where the certificate comes from; renews by itself: yes/no)
Service:               (active (running)? starts on boot?)
Database and backups:  (where the database is; where the daily copies go; how many are kept)
Port 8787 from outside: (closed - and how you checked)
Google sign-in:        off, waiting for the client ID
Done differently:      (anything you had to change from these instructions, and why)
For the owner to know: (anything else: costs, renewals, things to keep an eye on)
```

If you get stuck, or anything above does not fit this server, stop and tell me what you found rather than guessing.

## The program

Save everything between the two marker lines as `/opt/zenith/zenith_server.py`. The marker lines themselves are not part of the file.

-----BEGIN zenith_server.py-----
@PROGRAM@-----END zenith_server.py-----
'''


def main():
    program = io.open(os.path.join(HERE, "zenith_server.py"), encoding="utf-8", newline="").read()
    assert "\r" not in program, "zenith_server.py must have plain line endings"
    assert program.endswith("\n") and not program.endswith("\n\n")
    assert "zenith_server.py-----" not in program, "the program must not contain the marker lines"
    checksum = hashlib.sha256(program.encode("utf-8")).hexdigest()
    text = TEMPLATE.replace("@SHA256@", checksum).replace("@PROGRAM@", program)
    io.open(os.path.join(HERE, "PROMPT-FOR-SERVER-AI.md"), "w", encoding="utf-8", newline="\n").write(text)
    print("PROMPT-FOR-SERVER-AI.md: %d lines, program checksum %s" % (text.count("\n"), checksum))


if __name__ == "__main__":
    main()
