#!/usr/bin/env python3
"""Upload a Zenith UI build to Google Play, and read back where a release stands.

Needs a Play service account key (Play Console -> Users and permissions -> invite the service
account, then Google Cloud -> service account -> JSON key). Point PLAY_KEY at it, or leave it at
the default path below. The key is not in this repo and must never be committed.

    python tools/play_publish.py status
    python tools/play_publish.py upload --track internal
    python tools/play_publish.py upload --track internal --notes "What changed"
    python tools/play_publish.py upload --track production --rollout 0.1

Nothing is sent until the summary is printed and confirmed, unless --yes is passed.
"""
from __future__ import annotations

import argparse
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

PACKAGE = "com.gabriel.zenith_Launcher"
DEFAULT_KEY = os.path.expanduser("~/.config/zenith-play/service-account.json")
DEFAULT_AAB = "app/build/outputs/bundle/release/app-release.aab"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def service(key_path: str):
    if not os.path.exists(key_path):
        sys.exit(
            f"No Play key at {key_path}.\n"
            "Create a service account, download its JSON key, invite its email address in\n"
            "Play Console -> Users and permissions, and save the key there (or set PLAY_KEY)."
        )
    creds = service_account.Credentials.from_service_account_file(key_path, scopes=[SCOPE])
    return build("androidpublisher", "v3", credentials=creds, cache_discovery=False)


def show_status(api) -> None:
    edit = api.edits().insert(body={}, packageName=PACKAGE).execute()["id"]
    try:
        tracks = api.edits().tracks().list(editId=edit, packageName=PACKAGE).execute()
        for track in tracks.get("tracks", []):
            print(f"\n{track['track']}")
            for release in track.get("releases", []):
                versions = ", ".join(str(v) for v in release.get("versionCodes", [])) or "-"
                status = release.get("status", "?")
                fraction = release.get("userFraction")
                rollout = f", {fraction:.0%} rollout" if fraction else ""
                print(f"  {release.get('name', versions)}: {status}{rollout} (versionCode {versions})")
    finally:
        # A read-only edit is thrown away, so nothing can be left half-changed in the Console.
        api.edits().delete(editId=edit, packageName=PACKAGE).execute()


def upload(api, aab: str, track: str, notes: str | None, rollout: float | None, assume_yes: bool) -> None:
    if not os.path.exists(aab):
        sys.exit(f"No bundle at {aab}. Build one with: ./gradlew :app:bundleRelease")

    print(f"About to upload {aab} ({os.path.getsize(aab) / 1e6:.1f} MB) to the {track} track of {PACKAGE}.")
    if rollout is not None:
        print(f"Staged rollout to {rollout:.0%} of users.")
    if not assume_yes and input("Type yes to send it: ").strip().lower() != "yes":
        sys.exit("Nothing was uploaded.")

    edit = api.edits().insert(body={}, packageName=PACKAGE).execute()["id"]
    media = MediaFileUpload(aab, mimetype="application/octet-stream", resumable=True)
    bundle = api.edits().bundles().upload(editId=edit, packageName=PACKAGE, media_body=media).execute()
    version_code = bundle["versionCode"]
    print(f"Uploaded versionCode {version_code}.")

    release: dict = {"versionCodes": [str(version_code)]}
    if rollout is not None:
        release["status"], release["userFraction"] = "inProgress", rollout
    else:
        release["status"] = "completed"
    if notes:
        release["releaseNotes"] = [{"language": "en-GB", "text": notes}]

    api.edits().tracks().update(
        editId=edit, packageName=PACKAGE, track=track, body={"releases": [release]}
    ).execute()
    api.edits().commit(editId=edit, packageName=PACKAGE).execute()
    print(f"Done: versionCode {version_code} is on the {track} track.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", choices=["status", "upload"])
    parser.add_argument("--aab", default=DEFAULT_AAB)
    parser.add_argument("--track", default="internal", choices=["internal", "alpha", "beta", "production"])
    parser.add_argument("--notes", help="Release notes shown to testers or users")
    parser.add_argument("--rollout", type=float, help="Staged rollout fraction, e.g. 0.1 for 10%%")
    parser.add_argument("--key", default=os.environ.get("PLAY_KEY", DEFAULT_KEY))
    parser.add_argument("--yes", action="store_true", help="Skip the confirmation prompt")
    args = parser.parse_args()

    api = service(args.key)
    if args.command == "status":
        show_status(api)
    else:
        upload(api, args.aab, args.track, args.notes, args.rollout, args.yes)


if __name__ == "__main__":
    main()
