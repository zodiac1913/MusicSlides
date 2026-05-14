# LyricSlides

A desktop app that finds song lyrics, builds presentation slides with accessible text on customizable backgrounds, and exports to PowerPoint. Built for worship teams but works for anyone.

## Download & Install

Grab the latest release from the [Releases page](../../releases).

| Platform | File | Instructions |
|----------|------|--------------|
| **Linux** | `LyricSlides_x.x.x_amd64.deb` | `sudo dpkg -i LyricSlides*.deb` |
| **Windows** | `LyricSlides-windows.zip` | Unzip anywhere, run `LyricSlides.exe` inside the folder |
| **macOS** | `LyricSlides-macos.zip` | Unzip, drag `LyricSlides.app` to Applications |

### First-run notes

- **Windows:** SmartScreen may say "Windows protected your PC." Click **More info** → **Run anyway**. This only happens once.
- **macOS:** You may see "can't be opened because it is from an unidentified developer." Right-click the app → **Open** → **Open**. Only needed the first time.

No Java installation required — the runtime is bundled.

## Features

- **Smart Song Search** — Type a song title, get autocomplete suggestions from Genius and Hymnary.org. Works for gospel, worship, hymns, and mainstream.
- **CCLI Support** — Enter or look up CCLI Song #. Stored locally so it auto-fills next time. Displayed on title and closing slides (bottom-right corner).
- **Lyrics Editor** — View, verify, and edit lyrics before generating slides. Live slide breakdown preview.
- **Background Selection** — Procedural built-ins, import your own images, or import LibreOffice Impress templates (.otp files).
- **Smart Slide Generation** — Splits lyrics on verse breaks, max 8 lines per slide (balanced splitting). Single-line hymns auto-grouped into 4-line stanzas.
- **Title & Closing Slides** — Auto-generated with song name, artist, and CCLI legal reference.
- **Accessibility** — WCAG AA contrast checking (4.5:1 minimum), auto text color adjustment, text shadows.
- **Export** — PowerPoint (PPTX) or individual PNG images at 1920×1080. Filename: `Artist-Title-CCLI.pptx`.

## Workflow

1. **Search** — Type a song title in the search box, pick from suggestions (or paste lyrics manually)
2. **Edit** — Review lyrics, see slide breakdown, make edits
3. **Background** — Pick a background (built-in, imported image, or LibreOffice template)
4. **Generate** — Builds title slide + lyrics slides + closing slide with CCLI
5. **Preview & Export** — Browse slides, export as PPTX or PNGs

## Data Storage

Everything is stored locally in `~/.lyricslides/`:
- `ccli_database.json` — Remembered CCLI numbers (auto-fills on future searches)
- `custom_backgrounds.json` — Your imported background references
- `imported_backgrounds/` — Extracted images from .otp templates

## Tech Stack

- **Kotlin** + Compose Multiplatform for Desktop
- **Ktor** for HTTP (lyrics fetching)
- **Apache POI** for PPTX generation
- **Java AWT** for slide rendering and accessibility checks

## Building from Source

```bash
./gradlew run          # Run the app
./gradlew build        # Compile + test
./gradlew packageDeb   # Linux .deb
./gradlew createDistributable  # Native app bundle
```

Requires JDK 17+.

## License

Open source. Use it, share it, modify it.
