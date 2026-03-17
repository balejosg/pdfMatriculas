#!/usr/bin/env python3
"""Create sanitized PNG backgrounds from the provided templates.

We generate a 2-page *generic* background:
  - Page 1 background from the FP template (page 1).
  - Page 2 background from another template that has a larger academic area
    (default: BACH template, page 2).

Sanitization strategy:
  - Rasterize pages to PNG.
  - Remove checkbox/checkmark icons (small images).
  - Redact known sample/text-heavy blocks (study title, codes, academic tables,
    example names/dates, producer lines).

Outputs:
  assets/backgrounds/bg_page1.png
  assets/backgrounds/bg_page2.png
"""

from __future__ import annotations

import argparse
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path

import pdfplumber
from PIL import Image, ImageDraw


@dataclass(frozen=True)
class Rect:
    # Coordinates in PDF points, origin at TOP-LEFT (pdfplumber style).
    x0: float
    top: float
    x1: float
    bottom: float


def _pt_to_px(v: float, dpi: int) -> int:
    return int(round(v * dpi / 72.0))


def _rect_to_px(r: Rect, dpi: int) -> tuple[int, int, int, int]:
    return (
        _pt_to_px(r.x0, dpi),
        _pt_to_px(r.top, dpi),
        _pt_to_px(r.x1, dpi),
        _pt_to_px(r.bottom, dpi),
    )


def _expand(r: Rect, m: float) -> Rect:
    return Rect(r.x0 - m, r.top - m, r.x1 + m, r.bottom + m)


def _blank(draw: ImageDraw.ImageDraw, r: Rect, dpi: int) -> None:
    x0, y0, x1, y1 = _rect_to_px(r, dpi)
    draw.rectangle([x0, y0, x1, y1], fill=(255, 255, 255))


def _render_pdf_to_pngs(
    pdf_path: Path, *, dpi: int, out_prefix: Path, first: int, last: int
) -> list[Path]:
    out_prefix.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        "pdftoppm",
        "-png",
        "-r",
        str(dpi),
        "-f",
        str(first),
        "-l",
        str(last),
        str(pdf_path),
        str(out_prefix),
    ]
    subprocess.run(cmd, check=True)
    # pdftoppm writes <prefix>-1.png, <prefix>-2.png, ...
    out_files = sorted(out_prefix.parent.glob(out_prefix.name + "-*.png"))
    return out_files


def _small_image_rects(page) -> list[Rect]:
    rects: list[Rect] = []
    for im in page.images:
        w = float(im.get("width", 0) or 0)
        h = float(im.get("height", 0) or 0)
        # Keep the header/banner image.
        if w > 200 or h > 30:
            continue
        rects.append(
            Rect(
                float(im["x0"]), float(im["top"]), float(im["x1"]), float(im["bottom"])
            )
        )
    return rects


def sanitize_templates(
    template_page1: Path, template_page2: Path, *, dpi: int, out_dir: Path
) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmpdir = Path(tmp)

        png_page1 = _render_pdf_to_pngs(
            template_page1, dpi=dpi, out_prefix=tmpdir / "p1", first=1, last=1
        )
        png_page2 = _render_pdf_to_pngs(
            template_page2, dpi=dpi, out_prefix=tmpdir / "p2", first=2, last=2
        )
        if len(png_page1) != 1:
            raise RuntimeError(
                f"Expected 1 rendered PNG for page1, got {len(png_page1)}"
            )
        if len(png_page2) != 1:
            raise RuntimeError(
                f"Expected 1 rendered PNG for page2, got {len(png_page2)}"
            )

        with (
            pdfplumber.open(str(template_page1)) as pdf1,
            pdfplumber.open(str(template_page2)) as pdf2,
        ):
            page1 = pdf1.pages[0]
            page2 = pdf2.pages[1]

            # Manual redactions (top-based coords).
            manual_p1 = [
                # Top line: "ejemplar... Cod..."
                Rect(0, 0, 595.3, 25),
                # Study title lines (template contains a concrete example)
                Rect(0, 74, 595.3, 106),
                # Course value area
                Rect(270, 104, 360, 126),
            ]

            manual_p2 = [
                Rect(0, 0, 595.3, 25),
                # Academic table/text block (varies by ESO/BACH/FP). We'll replace it.
                # Keep the signature boxes/labels area lower on the page.
                Rect(0, 170, 595.3, 420),
                # Bottom producer line.
                Rect(0, 810, 595.3, 842),
            ]

            # Page 1
            img = Image.open(png_page1[0]).convert("RGB")
            draw = ImageDraw.Draw(img)
            for r in _small_image_rects(page1):
                _blank(draw, _expand(r, 1.2), dpi)
            for r in manual_p1:
                _blank(draw, r, dpi)
            img.save(out_dir / "bg_page1.png", format="PNG", optimize=True)

            # Page 2
            img = Image.open(png_page2[0]).convert("RGB")
            draw = ImageDraw.Draw(img)
            for r in _small_image_rects(page2):
                _blank(draw, _expand(r, 1.2), dpi)
            for r in manual_p2:
                _blank(draw, r, dpi)
            img.save(out_dir / "bg_page2.png", format="PNG", optimize=True)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument(
        "--template-page1", type=Path, default=Path("modelomatricula_FP.pdf")
    )
    ap.add_argument(
        "--template-page2", type=Path, default=Path("modelomatricula_bach.pdf")
    )
    ap.add_argument("--dpi", type=int, default=300)
    ap.add_argument("--out-dir", type=Path, default=Path("assets/backgrounds"))
    args = ap.parse_args()

    sanitize_templates(
        args.template_page1, args.template_page2, dpi=args.dpi, out_dir=args.out_dir
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
