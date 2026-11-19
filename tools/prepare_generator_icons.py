"""Bake a 40/44-size wooden tray into each existing 384px generator icon.

Original gen_*.png remain untouched. Run with Python + Pillow after adding source icons.
The application maps gen_* to the generated *_tray resource, including drag/warehouse UI.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "android-assets/drawable"


def make_tray(source: Path) -> Path:
    canvas = Image.new("RGBA", (384, 384))
    shadow = Image.new("RGBA", canvas.size)
    ImageDraw.Draw(shadow).rounded_rectangle((23, 30, 361, 374), radius=30, fill=(62, 50, 38, 85))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(7)))
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((18, 18, 366, 366), radius=28, fill="#E8D2B0", outline="#A9703C", width=17)
    draw.rounded_rectangle((39, 39, 345, 345), radius=13, outline="#F7E6C8", width=4)
    # Irregular, quiet grain in the margin rather than a stripe texture over the icon.
    draw.arc((36, 75, 118, 238), 95, 265, fill=(169, 112, 60, 55), width=3)
    draw.arc((280, 130, 350, 312), -80, 82, fill=(169, 112, 60, 55), width=3)
    original = Image.open(source).convert("RGBA")
    cropped = original.crop(original.getbbox())
    cropped.thumbnail((272, 278), Image.Resampling.LANCZOS)
    x, y = (384 - cropped.width) // 2, (384 - cropped.height) // 2 - 7
    mask = Image.new("RGBA", canvas.size)
    mask.paste((62, 50, 38, 80), (x + 1, y + 9), cropped.getchannel("A"))
    canvas.alpha_composite(mask.filter(ImageFilter.GaussianBlur(5)))
    canvas.alpha_composite(cropped, (x, y))
    target = source.with_stem(source.stem + "_tray")
    canvas.save(target, optimize=True)
    return target


if __name__ == "__main__":
    sources = sorted(p for p in DRAWABLE.glob("gen_*.png") if not p.stem.endswith("_tray"))
    for source in sources:
        print(make_tray(source).relative_to(ROOT))
    print(f"Prepared {len(sources)} generator icons; source icons preserved.")
