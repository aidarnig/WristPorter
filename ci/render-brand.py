#!/usr/bin/env python3
"""Reproducibly export PNG/ICO assets from canonical vector SVG designs."""
from pathlib import Path
from io import BytesIO
from PIL import Image
import cairosvg

ASSETS = Path(__file__).resolve().parents[1] / "docs" / "assets"

def raster(source, destination, width, height):
    data = cairosvg.svg2png(url=str(ASSETS / source), output_width=width, output_height=height)
    image = Image.open(BytesIO(data)).convert("RGBA")
    image.save(ASSETS / destination, optimize=True)
    return image

def main():
    for theme in ("light", "dark"):
        image = raster(f"logo-{theme}.svg", f"logo-{theme}.png", 1380, 372)
        # The rightmost glyph of "WristPorter" was previously clipped by a
        # too-narrow viewBox. Keep at least 24 transparent pixels as padding.
        opaque_bounds = image.getchannel("A").getbbox()
        if opaque_bounds is None or opaque_bounds[2] > image.width - 24:
            raise RuntimeError(f"Wordmark clipped or too close to right edge: {theme}, bounds={opaque_bounds}")
    mark = raster("watch-icon.svg", "watch-icon.png", 1024, 1024)
    mark.save(ASSETS / "wristporter.ico", format="ICO", sizes=[
        (16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    # Explorer uses different icon resolutions depending on DPI and view.
    with Image.open(ASSETS / "wristporter.ico") as icons:
        present = icons.ico.sizes()
        required = {(16, 16), (32, 32), (48, 48), (256, 256)}
        if not required.issubset(present):
            raise RuntimeError(f"Windows ICO lacks required sizes: {required - present}")
    raster("social-preview.svg", "social-preview.png", 1280, 640)
    raster("ui-preview-ru.svg", "ui-preview-ru.png", 1280, 720)
    raster("ui-preview-en.svg", "ui-preview-en.png", 1280, 720)

if __name__ == "__main__":
    main()
