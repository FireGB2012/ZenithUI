"""Puts one rendered option into the ZenithUI app as its launcher icon.

    python3 install_icon.py OPTION_DIR REPO_DIR
    python3 install_icon.py /mnt/project-files/zenith-app-icon/2-rising-z ~/ZenithUI

Writes the adaptive icon layers as lossless WebP in drawable-nodpi (background, foreground,
and monochrome for themed icons), the adaptive icon XML for ic_launcher and ic_launcher_round,
and play/icon-512.png. Since dfe20d4 (4.0) the release line has no density PNGs: minSdk 29
phones all use the adaptive icon in mipmap-anydpi.
"""
import glob
import os
import sys

from PIL import Image

ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_layer_background" />
    <foreground android:drawable="@drawable/ic_launcher_layer_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_layer_monochrome" />
</adaptive-icon>
"""


def clean_png(src, dst, mode):
    """Re-save without any metadata chunks, in the mode the app expects."""
    im = Image.open(src).convert(mode)
    im.save(dst, optimize=True)


def webp(src, dst, mode):
    """Lossless WebP, pixel for pixel the PNG, like the layers it replaces."""
    im = Image.open(src).convert(mode)
    im.save(dst, "WEBP", lossless=True, quality=100, method=6, exact=True)
    back = Image.open(dst).convert(mode)
    assert back.tobytes() == im.tobytes(), dst


def main():
    opt, repo = sys.argv[1], sys.argv[2]
    res = os.path.join(repo, "app", "src", "main", "res")
    lay = os.path.join(opt, "android-layers")
    nodpi = os.path.join(res, "drawable-nodpi")
    webp(os.path.join(lay, "ic_launcher_layer_background.png"),
         os.path.join(nodpi, "ic_launcher_layer_background.webp"), "RGB")
    for name in ("foreground", "monochrome"):
        webp(os.path.join(lay, f"ic_launcher_layer_{name}.png"),
             os.path.join(nodpi, f"ic_launcher_layer_{name}.webp"), "RGBA")
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        with open(os.path.join(res, "mipmap-anydpi", name), "w") as f:
            f.write(ADAPTIVE_XML)
    clean_png(glob.glob(os.path.join(opt, "*-play-store-512.png"))[0], os.path.join(repo, "play", "icon-512.png"), "RGBA")
    print("installed", os.path.basename(opt.rstrip("/")), "into", repo)


if __name__ == "__main__":
    main()
