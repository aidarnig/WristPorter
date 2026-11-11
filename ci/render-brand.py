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
    raster("logo-light.svg", "logo-light.png", 1210, 372)
    raster("logo-dark.svg", "logo-dark.png", 1210, 372)
    mark = raster("watch-icon.svg", "watch-icon.png", 1024, 1024)
    mark.save(ASSETS / "wristporter.ico", format="ICO", sizes=[
        (16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    raster("social-preview.svg", "social-preview.png", 1280, 640)
    raster("ui-preview-ru.svg", "ui-preview-ru.png", 1280, 720)
    raster("ui-preview-en.svg", "ui-preview-en.png", 1280, 720)

if __name__ == "__main__":
    main()
