# -*- coding: utf-8 -*-
"""
Scan frontend source for encoding corruption and regenerate known broken files.

Root cause: editing Vue/JS with Chinese on Windows using non-UTF-8 tools corrupts files
(???? placeholders or invalid UTF-8 bytes).

Usage:
  python tools/fix_frontend_encoding.py          # scan only
  python tools/fix_frontend_encoding.py --fix    # regenerate known files
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FRONTEND_SRC = ROOT / 'frontend' / 'src'
SCAN_EXT = {'.vue', '.js', '.ts', '.tsx'}

REGENERATORS = [
    ROOT / 'tools' / 'gen_mark_process_status.py',
    ROOT / 'tools' / 'gen_process_workbench.py',
    ROOT / 'backend' / 'sql' / 'fix_tencent_vue_encoding.py',
    ROOT / 'backend' / 'sql' / 'generate_user_order_detail_vue.py',
    ROOT / 'tools' / 'gen_user_xiaomi.py',
    ROOT / 'tools' / 'gen_agent_process_detail.py',
    ROOT / 'tools' / 'gen_agent_past_date_range_picker.py',
]


def scan_file(path: Path) -> list[str]:
    issues: list[str] = []
    raw = path.read_bytes()
    try:
        text = raw.decode('utf-8')
    except UnicodeDecodeError as exc:
        issues.append(f'invalid UTF-8: {exc}')
        return issues
    if '????' in text:
        issues.append('contains ???? placeholder text')
    q = text.count('?')
    cn = len(re.findall(r'[\u4e00-\u9fff]', text))
    if q >= 20 and cn < 5 and path.suffix == '.vue':
        issues.append(f'suspicious question marks (q={q}, chinese={cn})')
    return issues


def scan_all() -> dict[Path, list[str]]:
    result: dict[Path, list[str]] = {}
    for path in FRONTEND_SRC.rglob('*'):
        if path.suffix not in SCAN_EXT or not path.is_file():
            continue
        issues = scan_file(path)
        if issues:
            result[path] = issues
    return result


def run_regenerators() -> None:
    for script in REGENERATORS:
        if not script.exists():
            print('skip missing', script.relative_to(ROOT))
            continue
        print('run', script.relative_to(ROOT))
        subprocess.run([sys.executable, str(script)], check=True, cwd=str(ROOT))


def print_report(title: str, report: dict[Path, list[str]]) -> None:
    if not report:
        print(title, '- none')
        return
    print(title)
    for path, issues in sorted(report.items()):
        print(path.relative_to(ROOT))
        for issue in issues:
            print('  -', issue)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--fix', action='store_true', help='regenerate known UTF-8 source files')
    args = parser.parse_args()

    print_report('=== encoding issues ===', scan_all())

    if not args.fix:
        if scan_all():
            print('\nRun: python tools/fix_frontend_encoding.py --fix')
            return 1
        return 0

    run_regenerators()
    remaining = scan_all()
    print_report('\n=== remaining issues after fix ===', remaining)
    if remaining:
        return 1
    print('\nAll scanned frontend files are valid UTF-8.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
