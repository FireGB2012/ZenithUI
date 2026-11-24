# Zenith's app icon

The launcher icon is drawn by these scripts, in the same liquid glass as the Zenith Plus icon
(`glass.py` is that renderer). Why it looks the way it does: `specs/2026-11-24-new-app-icon-design.md`.

```
pip install numpy scipy pillow
python3 tools/app-icon/make_zenith_icon.py /tmp/zenith-icon rising-z      # one option, ~30 s
python3 tools/app-icon/make_zenith_icon.py /tmp/zenith-icon               # all four and the sheets
python3 tools/app-icon/install_icon.py /tmp/zenith-icon/2-rising-z .      # put one into the app
S=900 python3 tools/app-icon/make_zenith_icon.py /tmp/zenith-icon         # quick, softer draft
```

- The canvas is Android's 108 dp adaptive layer; `T` is the 72 dp part a mask shows. Symbols stay
  inside the 66 dp circle Android promises every mask keeps (0.458 T from the middle), so a Samsung
  squircle, a Pixel circle or any other shape never cuts them.
- Per option it writes `<key>-squircle-1024.png`, `<key>-circle-1024.png`, `<key>-play-store-512.png`
  and `android-layers/` (background, foreground with alpha, monochrome for themed icons; 432 px,
  108 dp at xxxhdpi). It lays the saved foreground over the saved background and prints how far
  that is from the render, so a bad split shows at once.
- `install_icon.py` writes the three layers into `drawable-nodpi` as lossless WebP (checked pixel
  for pixel), the adaptive icon XML with `<monochrome>`, and `play/icon-512.png` (32-bit).
  The feature graphic is not touched by it.
- The options are `OPTIONS` in `make_zenith_icon.py`: a symbol (`glass_z`, `rising_z`, the planet in
  `PLANET`) on a colour set from `looks.py` (pastel is Zenith's old gradient, `brand_bg.png`;
  midnight, aurora, frost). Any symbol can go on any colour set: add an entry.
- The home screen sheet needs a Zenith home screen shot in `HOME_SHOT`; without one it is skipped.
