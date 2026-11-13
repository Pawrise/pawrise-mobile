"""Icônes Lucide (https://lucide.dev, licence ISC) pour iOS et Android, générées depuis le même SVG.

Les icônes de la maquette Figma ont été exportées avec leurs traits vectorisés, ce qui déforme les tracés.
On les remplace par leur équivalent Lucide, au trait de la maquette (1,9 sur la grille 24).

Usage (depuis la racine du dépôt, sur la branche iOS ou Android) :
  npm pack lucide-static@0.544.0 && tar xzf lucide-static-0.544.0.tgz
  python3 scripts/lucide_icons.py package/icons --ios ios       # LucidePaths.swift + assets SVG
  python3 scripts/lucide_icons.py package/icons --android android  # LucidePaths.kt
"""
import argparse
import re
import xml.etree.ElementTree as ET
from pathlib import Path

LUCIDE_VERSION = "0.544.0"

# nom Pawrise → icône Lucide (une seule table pour les deux plateformes)
ICONS = {
    "map": "map",
    "chat": "message-square",
    "settings": "settings",
    "doc": "file-text",
    "heart": "heart",
    "pulse": "heart-pulse",
    "activity": "activity",
    "moon": "moon",
    "thermometer": "thermometer",
    "battery": "battery",
    "alert": "triangle-alert",
    "alertCircle": "circle-alert",
    "swap": "arrow-left-right",
    "close": "x",
    "check": "check",
    "signal": "signal",
    "shield": "shield",
    "send": "send",
    "paw": "paw-print",
    "user": "user",
    "lock": "lock",
    "chevronLeft": "chevron-left",
    "chevronRight": "chevron-right",
    "trash": "trash-2",
    "logout": "log-out",
    "plus": "plus",
    "search": "search",
    "mail": "mail",
    "smartphone": "smartphone",
    "eye": "eye",
    "eyeOff": "eye-off",
}

# assets iOS (catalogue Onboarding.xcassets) : nom → (icône, taille en pt, trait sur la grille 24)
IOS_ASSETS = {
    "paw": ("paw", 40, 1.9),
    "signal": ("signal", 40, 1.9),
    "heart": ("heart", 40, 1.9),
    "alert": ("alert", 40, 1.9),
    "map": ("map", 40, 1.9),
    "back": ("chevronLeft", 20, 1.9),
    "loginback": ("chevronLeft", 18, 1.9),
    "learnback": ("chevronLeft", 20, 2.4),
    "close": ("close", 20, 1.9),
    "check": ("check", 28, 1.9),
    "rowchevron": ("chevronRight", 16, 1.9),
    "signal28": ("signal", 28, 1.9),
}
COLORS = {"rowchevron": "#5a7082", "close": "#5a7082", "learnback": "#123044", "check": "#0e7c74"}
ACTION = "#0a5c78"
NS = "{http://www.w3.org/2000/svg}"
HEADER = f"Lucide {LUCIDE_VERSION} (ISC, https://lucide.dev), généré par scripts/lucide_icons.py : ne pas modifier à la main."


def lucide_paths(icons: Path, name: str) -> list[str]:
    """Tracés d'une icône, chaque forme SVG convertie en `d` (les moteurs natifs ne lisent que des tracés)."""
    out = []
    for el in ET.parse(icons / f"{name}.svg").getroot():
        tag, a = el.tag.replace(NS, ""), el.attrib
        f = lambda k, d=0.0: float(a.get(k, d))
        if tag == "path":
            out.append(a["d"])
        elif tag == "circle":
            cx, cy, r = f("cx"), f("cy"), f("r")
            out.append(f"M{cx - r:g} {cy:g}a{r:g} {r:g} 0 1 0 {2 * r:g} 0a{r:g} {r:g} 0 1 0 {-2 * r:g} 0")
        elif tag == "line":
            out.append(f"M{f('x1'):g} {f('y1'):g}L{f('x2'):g} {f('y2'):g}")
        elif tag in ("polyline", "polygon"):
            pts = re.split(r"[\s,]+", a["points"].strip())
            d = "M" + " L".join(f"{pts[i]} {pts[i + 1]}" for i in range(0, len(pts), 2))
            out.append(d + ("Z" if tag == "polygon" else ""))
        elif tag == "rect":
            x, y, w, h = f("x"), f("y"), f("width"), f("height")
            rx = f("rx", a.get("ry", 0))
            if rx:
                iw, ih = w - 2 * rx, h - 2 * rx
                out.append(
                    f"M{x + rx:g} {y:g}h{iw:g}a{rx:g} {rx:g} 0 0 1 {rx:g} {rx:g}v{ih:g}a{rx:g} {rx:g} 0 0 1 {-rx:g} {rx:g}"
                    f"h{-iw:g}a{rx:g} {rx:g} 0 0 1 {-rx:g} {-rx:g}v{-ih:g}a{rx:g} {rx:g} 0 0 1 {rx:g} {-rx:g}Z"
                )
            else:
                out.append(f"M{x:g} {y:g}h{w:g}v{h:g}h{-w:g}Z")
        else:
            raise SystemExit(f"{name}.svg : forme non gérée <{tag}>")
    return out


def swift_table(table: dict[str, list[str]]) -> str:
    lines = [f"// {HEADER}", "", "/// Tracés des icônes Lucide (grille 24 × 24, au trait).", "enum LucidePaths {"]
    for name, paths in table.items():
        items = ", ".join(f'"{d}"' for d in paths)
        lines.append(f"    /// Lucide « {ICONS[name]} »")
        lines.append(f"    static let {name}: [String] = [{items}]")
    return "\n".join(lines) + "\n}\n"


def kotlin_table(table: dict[str, list[str]]) -> str:
    lines = [
        f"// {HEADER}",
        "package com.pawrise.core.designsystem.component",
        "",
        "/** Tracés des icônes Lucide (grille 24 × 24, au trait). */",
        "@Suppress(\"MaxLineLength\")",
        "object LucidePaths {",
    ]
    for name, paths in table.items():
        items = ", ".join(f'"{d}"' for d in paths)
        lines.append(f"    /** Lucide « {ICONS[name]} » */")
        lines.append(f"    val {name[0].upper() + name[1:]}: List<String> = listOf({items})")
    return "\n".join(lines) + "\n}\n"


def svg(paths: list[str], size: int, stroke: float, color: str, source: str) -> str:
    body = "\n".join(f'  <path d="{d}"/>' for d in paths)
    return (
        f"<!-- Lucide « {source} » {LUCIDE_VERSION} (ISC), généré par scripts/lucide_icons.py -->\n"
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" '
        f'stroke="{color}" stroke-width="{stroke}" stroke-linecap="round" stroke-linejoin="round">\n{body}\n</svg>\n'
    )


def photo_plus(plus: list[str]) -> str:
    """Pastille « Choisir une photo » (maquette A08) : disque 44 pt et « + » blanc de 16 pt, trait 2,6."""
    scale = 16 / 14  # le « plus » de Lucide couvre 14 unités (5 → 19) : on le met à 16 pt
    offset = 22 - 12 * scale
    body = "\n".join(f'    <path d="{d}"/>' for d in plus)
    return (
        f"<!-- Pastille photo : disque + Lucide « plus » {LUCIDE_VERSION} (ISC), généré par scripts/lucide_icons.py -->\n"
        f'<svg xmlns="http://www.w3.org/2000/svg" width="44" height="44" viewBox="0 0 44 44">\n'
        f'  <circle cx="22" cy="22" r="22" fill="{ACTION}"/>\n'
        f'  <g transform="translate({offset:.6f} {offset:.6f}) scale({scale:.6f})" fill="none" stroke="#ffffff" '
        f'stroke-width="{2.6 / scale:g}" stroke-linecap="round" stroke-linejoin="round">\n{body}\n  </g>\n</svg>\n'
    )


def write_imageset(folder: Path, name: str, content: str) -> None:
    imageset = folder / f"{name}.imageset"
    imageset.mkdir(parents=True, exist_ok=True)
    for old in imageset.glob("*.svg"):
        old.unlink()
    (imageset / f"{name}.svg").write_text(content)
    (imageset / "Contents.json").write_text(
        '{"images":[{"filename":"%s.svg","idiom":"universal"}],"info":{"author":"xcode","version":1},'
        '"properties":{"preserves-vector-representation":true,"template-rendering-intent":"original"}}\n' % name
    )


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("icons", type=Path, help="dossier icons/ de lucide-static")
    p.add_argument("--ios", type=Path, help="dossier ios/ du dépôt")
    p.add_argument("--android", type=Path, help="dossier android/ du dépôt")
    args = p.parse_args()
    table = {name: lucide_paths(args.icons, source) for name, source in ICONS.items()}
    if args.ios:
        (args.ios / "Pawrise/DesignSystem/LucidePaths.swift").write_text(swift_table(table))
        assets = args.ios / "Pawrise/Resources/Onboarding.xcassets/Onboarding"
        for asset, (icon, size, stroke) in IOS_ASSETS.items():
            write_imageset(assets, asset, svg(table[icon], size, stroke, COLORS.get(asset, ACTION), ICONS[icon]))
        write_imageset(assets, "photoplus", photo_plus(table["plus"]))
    if args.android:
        out = args.android / "core/designsystem/src/main/java/com/pawrise/core/designsystem/component/LucidePaths.kt"
        out.write_text(kotlin_table(table))
    print(f"{len(table)} icônes Lucide {LUCIDE_VERSION}")


if __name__ == "__main__":
    main()
