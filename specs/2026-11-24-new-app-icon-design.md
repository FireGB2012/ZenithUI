# The new Zenith icon: Rising Z

- Date: 2026-11-24
- Branch: `claude/project-thread-bnu4hy`, on top of 118 / 4.0.2 (`claude/project-thread-9raxr5`)
- Status: built. Gabriel: "Yo can u try to make a new icon for zenith". Four options were drawn;
  he picked **2 Rising Z**.

## What was wrong with the old icon

- **A tile inside the tile.** The foreground layer carried its own rounded glass tile, rim and all,
  on top of the gradient. Android cuts an adaptive icon to the phone's own shape (Samsung's
  squircle, Pixel's circle), so every phone showed that tile sitting inside its icon shape.
- **No monochrome layer**, so themed icons (Android 13 and later, One UI's themed icons) had no
  flat Zenith to draw.
- **No big source.** The symbol was a photoreal render whose largest copy was the 512 px Play icon.

## The options

All four are in the Zenith Plus icon's glass (clear glass, rainbow at the rims, light from the top
left, a soft shadow straight down and a little right), drawn by `tools/app-icon/`. The option
sheets and home screen previews are in the project files, `zenith-app-icon/`.

1. Glass Z: Zenith's old pastel gradient, a plain glass Z.
2. **Rising Z** (picked): the midnight gradient; a glass Z whose top right corner is an arrow
   going up. It is the old icon's rising arrow, drawn as the Z itself.
3. Planet: a glass ball with a tilted ring on the Aurora theme's teal.
4. Frost Planet: the planet on white frost.

## What changed

- `drawable-nodpi/ic_launcher_layer_background.webp`: the midnight gradient, full bleed, so it
  fills whatever shape the phone cuts.
- `drawable-nodpi/ic_launcher_layer_foreground.webp`: the glass Z with its own shadow and light on
  a clear layer. The Z stays well inside the 66 dp circle Android promises every mask keeps.
- `drawable-nodpi/ic_launcher_layer_monochrome.webp` (new): the Z as a flat white silhouette;
  `<monochrome>` is added to both `mipmap-anydpi` adaptive icons.
- All three are 432 px (108 dp at xxxhdpi) and lossless WebP, as the layers have been since 4.0.
  There are still no density PNGs: on minSdk 29 every phone uses the adaptive icon.
- `play/icon-512.png` (now 32-bit, as `play/listing.md` asks) and
  `play/feature-graphic-1024x500.png` carry the new icon. The feature graphic is the old one with
  only the icon swapped: its background was refitted where the old tile and its shadow were.
  `tools/play_publish.py` does not send graphics, so both go up by hand in Play Console.
- `tools/app-icon/`: the renderer and `install_icon.py`, to re-render, tweak or swap the icon.

## Checked

- aapt2 from build-tools 37 against android-37: the whole `res` folder compiles, and the icon links
  into an APK with its background, foreground and monochrome in place.
- The saved foreground laid over the saved background matches the render (largest difference 0.06,
  mean 0.0014, on a 0 to 1 scale, at the Z's anti-aliased edge).
- Not run: the Gradle build and the unit tests, since only icon resources and Play graphics changed.
  Not seen on his A55 yet.
