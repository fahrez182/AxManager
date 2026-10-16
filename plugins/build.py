#!/usr/bin/env python3
"""Packs each plugin folder here into an installable zip in out/.

    python plugins/build.py                 # every plugin
    python plugins/build.py test_function   # just one
    python plugins/build.py --abort         # also adds an abort-test file (Test Function aborts)

Text files are written with LF line endings whatever the checkout uses: a CRLF shell script
breaks on Android. module.prop is checked with the installer's own rules first.
"""

import argparse
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "out"
ID_RULE = re.compile(r"^[a-zA-Z][a-zA-Z0-9._-]+$")
BINARY = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".ico", ".so", ".zip", ".dex", ".jar"}


def read_prop(path: Path) -> dict:
    props = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def check_prop(folder: Path, props: dict) -> None:
    problems = []
    if not ID_RULE.match(props.get("id", "")):
        problems.append(f"id {props.get('id')!r} does not match {ID_RULE.pattern}")
    if props.get("id") != folder.name:
        problems.append(f"id {props.get('id')!r} differs from the folder name {folder.name!r}")
    if not props.get("axeronPlugin", "").isdigit():
        problems.append("axeronPlugin must be a number")
    if not props.get("versionCode", "").isdigit():
        problems.append("versionCode must be an integer")
    if problems:
        sys.exit(f"{folder.name}/module.prop: " + "; ".join(problems))


def pack(folder: Path, abort_test: bool) -> Path:
    props = read_prop(folder / "module.prop")
    check_prop(folder, props)
    suffix = "-abort" if abort_test else ""
    target = OUT / f"{props['id']}-{props.get('version', 'v0')}{suffix}.zip"
    OUT.mkdir(exist_ok=True)

    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as zf:
        for file in sorted(p for p in folder.rglob("*") if p.is_file()):
            name = file.relative_to(folder).as_posix()
            data = file.read_bytes()
            if file.suffix.lower() not in BINARY:
                data = data.replace(b"\r\n", b"\n")
            info = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (0o755 if file.suffix in {".sh", ""} else 0o644) << 16
            zf.writestr(info, data)
        if abort_test:
            zf.writestr("abort-test", b"")
    return target


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("plugins", nargs="*", help="plugin folders to pack (default: all)")
    parser.add_argument("--abort", action="store_true", help="add an abort-test file to the zip")
    args = parser.parse_args()

    folders = [ROOT / p for p in args.plugins] if args.plugins else \
        sorted(p.parent for p in ROOT.glob("*/module.prop"))
    if not folders:
        sys.exit("no plugin folders found")
    for folder in folders:
        if not (folder / "module.prop").is_file():
            sys.exit(f"{folder.name}: no module.prop")
        print(f"{folder.name} -> {pack(folder, args.abort).relative_to(ROOT.parent)}")


if __name__ == "__main__":
    main()
