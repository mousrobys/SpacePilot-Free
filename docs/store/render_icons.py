"""Renders the SpacePilot Free SVG icon to PNG sizes for RuStore."""
import os
import re
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
SVG_PATH = os.path.join(ROOT, "icon-512.svg")
MASTER = os.path.join(ROOT, "icon-512.png")
OUT_DIR = os.path.abspath(os.path.join(ROOT, "..", "..", "для rustore"))

SIZES = {
    "icon-512.png": 512,
    "icon-256.png": 256,
    "icon-192.png": 192,
    "icon-144.png": 144,
    "icon-96.png": 96,
    "icon-72.png": 72,
    "icon-48.png": 48,
}


def main():
    master = Image.open(MASTER).convert("RGB")
    print("master:", master.size)
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, size in SIZES.items():
        resized = master.resize((size, size), Image.LANCZOS)
        path = os.path.join(OUT_DIR, name)
        resized.save(path, optimize=True)
        print("wrote", path, resized.size)


if __name__ == "__main__":
    main()
