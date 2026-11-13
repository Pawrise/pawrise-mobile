"""Génère les icônes Lucide (https://lucide.dev, licence ISC) pour iOS et Android à partir du même SVG.

Usage :
  npm pack lucide-static@0.544.0 && tar xzf lucide-static-0.544.0.tgz
  python3 scripts/lucide_icons.py package/icons --ios <dossier .xcassets/…> --android <dossier res/drawable>

Le trait est réglé sur la maquette : icône de 40 pt, trait de 3,17 pt (1,9 sur la grille 24 de Lucide).
"""
import argparse
import re
import xml.etree.ElementTree as ET
from pathlib import Path

# nom de l'asset Pawrise → nom de l'icône Lucide
ICONS = {
    "paw": "paw-print",
    "signal": "signal",
    "heart": "heart",
    "alert": "triangle-alert",
    "map": "map",
}
SIZE = 40
STROKE_WIDTH = 1.9
COLOR = "#0a5c78"
NS = "{http://www.w3.org/2000/svg}"


def shapes_to_paths(svg: Path) -> list[str]:
    """Convertit chaque forme SVG en tracé (VectorDrawable ne connaît que <path>)."""
    out = []
    for el in ET.parse(svg).getroot():
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
            x, y, w, h, rx = f("x"), f("y"), f("width"), f("height"), f("rx", a.get("ry", 0))
            if rx:
                out.append(
                    f"M{x + rx:g} {y:g}h{w - 2 * rx:g}a{rx:g} {rx:g} 0 0 1 {rx:g} {rx:g}v{h - 2 * rx:g}"
                    f"a{rx:g} {rx:g} 0 0 1 {-rx:g} {rx:g}h{-(w - 2 * rx):g}a{rx:g} {rx:g} 0 0 1 {-rx:g} {-rx:g}"
                    f"v{-(h - 2 * rx):g}a{rx:g} {rx:g} 0 0 1 {rx:g} {-rx:g}Z"
                )
            else:
                out.append(f"M{x:g} {y:g}h{w:g}v{h:g}h{-w:g}Z")
        else:
            raise SystemExit(f"{svg.name} : forme non gérée <{tag}>")
    return out


def ios_svg(paths: list[str], source: str) -> str:
    body = "\n".join(f'  <path d="{d}"/>' for d in paths)
    return (
        f"<!-- Lucide « {source} » (ISC), généré par scripts/lucide_icons.py -->\n"
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{SIZE}" height="{SIZE}" viewBox="0 0 24 24" fill="none" '
        f'stroke="{COLOR}" stroke-width="{STROKE_WIDTH}" stroke-linecap="round" stroke-linejoin="round">\n{body}\n</svg>\n'
    )


def android_vector(paths: list[str], source: str) -> str:
    body = "\n".join(
        f'    <path android:pathData="{d}" android:strokeColor="{COLOR}" android:strokeWidth="{STROKE_WIDTH}"\n'
        f'        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000" />'
        for d in paths
    )
    return (
        f"<!-- Lucide « {source} » (ISC), généré par scripts/lucide_icons.py -->\n"
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{SIZE}dp" android:height="{SIZE}dp" android:viewportWidth="24" android:viewportHeight="24">\n'
        f"{body}\n</vector>\n"
    )


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("icons", type=Path, help="dossier icons/ de lucide-static")
    p.add_argument("--ios", type=Path, help="dossier qui contient les <nom>.imageset")
    p.add_argument("--android", type=Path, help="dossier res/drawable")
    args = p.parse_args()
    for name, source in ICONS.items():
        paths = shapes_to_paths(args.icons / f"{source}.svg")
        if args.ios:
            imageset = args.ios / f"{name}.imageset"
            imageset.mkdir(parents=True, exist_ok=True)
            for old in imageset.glob("*.svg"):
                old.unlink()
            (imageset / f"{name}.svg").write_text(ios_svg(paths, source))
            (imageset / "Contents.json").write_text(
                '{"images":[{"filename":"%s.svg","idiom":"universal"}],"info":{"author":"xcode","version":1},'
                '"properties":{"preserves-vector-representation":true,"template-rendering-intent":"original"}}\n' % name
            )
        if args.android:
            args.android.mkdir(parents=True, exist_ok=True)
            (args.android / f"ic_learn_{name}.xml").write_text(android_vector(paths, source))
        print(name, "←", source)


if __name__ == "__main__":
    main()
