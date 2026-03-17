#!/usr/bin/env python3
"""Extract cascaded select option model from the web form HTML.

This script is intentionally read-only: it never submits the form.

Usage:
  python3 tools/extract_model.py --html path/to/form.html --out data/model.json

Optionally, you can pass --url to fetch HTML.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any
from urllib.request import Request, urlopen

from lxml import html


def _read_url(url: str) -> str:
    req = Request(
        url,
        headers={
            "User-Agent": "pdfMatriculas-model-extractor/1.0",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        },
    )
    with urlopen(req, timeout=60) as resp:
        raw = resp.read()
        # Most pages are ISO-8859-1.
        for enc in ("utf-8", "iso-8859-1", "cp1252"):
            try:
                return raw.decode(enc)
            except UnicodeDecodeError:
                pass
        return raw.decode("utf-8", errors="replace")


def _extract_options(sel_el) -> list[dict[str, str]]:
    out: list[dict[str, str]] = []
    for opt in sel_el.xpath(".//option"):
        v = opt.get("value") or ""
        t = (opt.text or "").strip() or v
        out.append({"v": v, "t": t})
    return out


def _get_select_options(doc, select_id: str) -> list[dict[str, str]]:
    try:
        sel = doc.get_element_by_id(select_id)
    except KeyError:
        return []
    return _extract_options(sel)


def _as_int_keys(d: dict[int, Any]) -> dict[str, Any]:
    return {str(k): v for k, v in sorted(d.items(), key=lambda kv: kv[0])}


def build_model(html_text: str) -> dict[str, Any]:
    doc = html.fromstring(html_text)

    selectors: dict[str, Any] = {}
    selectors["txtEstudios"] = _get_select_options(doc, "txtEstudios")
    selectors["txtESO_Cursos"] = _get_select_options(doc, "txtESO_Cursos")
    selectors["txtBACH_Cursos"] = _get_select_options(doc, "txtBACH_Cursos")
    selectors["txtCICLOS_Cursos"] = _get_select_options(doc, "txtCICLOS_Cursos")
    selectors["txtCICLOS_Grados"] = _get_select_options(doc, "txtCICLOS_Grados")
    selectors["txtSexo"] = _get_select_options(doc, "txtSexo")
    selectors["matriculaCompleta"] = _get_select_options(doc, "matriculaCompleta")
    selectors["txtDobleMatricula"] = _get_select_options(doc, "txtDobleMatricula")
    selectors["txtTipoDoc"] = _get_select_options(doc, "txtTipoDoc")
    selectors["txtTipoDocTutor1"] = _get_select_options(doc, "txtTipoDocTutor1")
    selectors["txtTipoDocTutor2"] = _get_select_options(doc, "txtTipoDocTutor2")

    # ESO programs by course.
    selectors["txtESO_Programas"] = {
        "1": _get_select_options(doc, "txtESO_C1_Programa"),
        "2": _get_select_options(doc, "txtESO_C2_Programa"),
        "3": _get_select_options(doc, "txtESO_C3_Programa"),
        "4": _get_select_options(doc, "txtESO_C4_Programa"),
    }

    # ESO course 4 itineraries by program (P1..P3 use the same items).
    selectors["txtESO_C4_Itinerarios"] = {
        "1": _get_select_options(doc, "txtESO_C4_P1_Itinerarios"),
        "2": _get_select_options(doc, "txtESO_C4_P2_Itinerarios"),
        "3": _get_select_options(doc, "txtESO_C4_P3_Itinerarios"),
    }

    selectors["txtBACH_Itinerarios"] = {
        "1": _get_select_options(doc, "txtBACH_C1_P0_Itinerarios"),
        "2": _get_select_options(doc, "txtBACH_C2_P0_Itinerarios"),
    }

    # Build per-scenario option lists.
    all_ids = [el.get("id") for el in doc.xpath("//*[@id]")]

    scenarios: dict[str, Any] = {}

    # ESO/BACH (main + child) option containers.
    opt_cont = [
        i
        for i in all_ids
        if i
        and i.endswith("_Opt")
        and (
            i.startswith("ESO_")
            or i.startswith("BACH_")
            or i.startswith("child_ESO_")
            or i.startswith("child_BACH_")
        )
    ]

    for opt_id in opt_cont:
        base = opt_id[: -len("_Opt")]
        data = {"opt": {}, "com": {}, "tron": {}}

        # optativas
        div = doc.get_element_by_id(opt_id)
        for sel in div.xpath(".//select"):
            cls = sel.get("class") or ""
            m = re.search(r"\b(child_)?opcion_(\d+)\b", cls)
            if not m:
                continue
            slot = int(m.group(2))
            data["opt"][slot] = _extract_options(sel)

        # comunes
        com_id = base + "_OptComunes"
        try:
            div = doc.get_element_by_id(com_id)
        except KeyError:
            div = None
        if div is not None:
            for sel in div.xpath(".//select"):
                cls = sel.get("class") or ""
                m = re.search(r"\b(child_)?selectOptComunes(\d+)\b", cls)
                if not m:
                    continue
                slot = int(m.group(2))
                data["com"][slot] = _extract_options(sel)

        # troncales
        tron_id = base + "_OptTroncal"
        try:
            div = doc.get_element_by_id(tron_id)
        except KeyError:
            div = None
        if div is not None:
            for sel in div.xpath(".//select"):
                cls = sel.get("class") or ""
                m = re.search(r"\b(child_)?selectTroncal(\d+)\b", cls)
                if not m:
                    continue
                slot = int(m.group(2))
                data["tron"][slot] = _extract_options(sel)

        scenarios[base] = {
            "opt": _as_int_keys(data["opt"]),
            "com": _as_int_keys(data["com"]),
            "tron": _as_int_keys(data["tron"]),
        }

    # CICLOS containers (main + child)
    for prefix in ("", "child_"):
        for cid in all_ids:
            if not cid:
                continue
            if not cid.startswith(prefix + "CICLOS_C"):
                continue
            if (
                cid.endswith("_Opt")
                or cid.endswith("_OptComunes")
                or cid.endswith("_OptTroncal")
            ):
                continue
            if not re.match(
                rf"^{prefix}CICLOS_C\d+_Grado_(Medio_(Pres|Dist)|Superior|Basico)$", cid
            ):
                continue
            div = doc.get_element_by_id(cid)
            data = {"opt": {}, "com": {}, "tron": {}}
            for sel in div.xpath(".//select"):
                cls = sel.get("class") or ""
                if "opcion_ciclos" in cls:
                    data["opt"][1] = _extract_options(sel)
            scenarios[cid] = {
                "opt": _as_int_keys(data["opt"]),
                "com": _as_int_keys(data["com"]),
                "tron": _as_int_keys(data["tron"]),
            }

    out = {
        "meta": {
            "source": "form.html",
            "version": 1,
        },
        "selectors": selectors,
        "scenarios": scenarios,
    }
    return out


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    src = ap.add_mutually_exclusive_group(required=True)
    src.add_argument("--html", type=Path, help="Path to HTML file")
    src.add_argument("--url", type=str, help="URL to fetch HTML from")
    ap.add_argument("--out", type=Path, required=True, help="Output JSON path")
    args = ap.parse_args(argv)

    if args.url:
        html_text = _read_url(args.url)
    else:
        html_text = args.html.read_text(encoding="utf-8", errors="replace")

    model = build_model(html_text)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(
        json.dumps(model, ensure_ascii=True, indent=2) + "\n", encoding="utf-8"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
