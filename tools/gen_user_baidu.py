# -*- coding: utf-8 -*-
"""Generate user Baidu page from Xiaomi template (UTF-8 Chinese)."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "frontend/src/views/server/mark/user/xiaomi.vue"
TARGET = ROOT / "frontend/src/views/server/mark/user/baidu.vue"

XIAOMI_TAB = zh(0x5C0F, 0x7C73, 0x20, 0x2D, 0x20)
BAIDU_TAB = zh(0x767E, 0x5EA6, 0x20, 0x2D, 0x20)
XIAOMI_TITLE = "xiaomo" + zh(0x6807, 0x8BB0, 0x63D0, 0x4EA4)
BAIDU_TITLE = zh(0x767E, 0x5EA6, 0x6807, 0x8BB0, 0x63D0, 0x4EA4)
XIAOMI_NAME = zh(0x5C0F, 0x7C73, 0x624B, 0x673A)
BAIDU_NAME = zh(0x767E, 0x5EA6)
XIAOMI_MATCH = zh(0x5C0F, 0x7C73)

REPLACEMENTS = [
    ("mark-user-xiaomi-page", "mark-user-baidu-page"),
    ("xiaomi-page-card", "baidu-page-card"),
    ("xiaomi-content-shell", "baidu-content-shell"),
    ("xiaomi-submit-panel", "baidu-submit-panel"),
    ("xiaomi-submit-section", "baidu-submit-section"),
    ("xiaomi-submit-title", "baidu-submit-title"),
    ("xiaomi-submit-textarea", "baidu-submit-textarea"),
    ("xiaomi-submit-stats__deduct", "baidu-submit-stats__deduct"),
    ("xiaomi-submit-stats", "baidu-submit-stats"),
    ("xiaomi-submit-actions", "baidu-submit-actions"),
    ("xiaomi-submit-buttons", "baidu-submit-buttons"),
    ("xiaomi-action-btn--extract", "baidu-action-btn--extract"),
    ("xiaomi-action-btn", "baidu-action-btn"),
    ("xiaomi-result-block", "baidu-result-block"),
    ("xiaomi-result-head", "baidu-result-head"),
    ("xiaomi-result-table", "baidu-result-table"),
    ("xiaomi-result-empty", "baidu-result-empty"),
    (XIAOMI_TAB, BAIDU_TAB),
    (XIAOMI_TITLE, BAIDU_TITLE),
    ("MarkUserXiaomi", "MarkUserBaidu"),
    ("markXiaomiPlatform", "markBaiduPlatform"),
    ("XIAOMI_PLATFORM_CODE", "BAIDU_PLATFORM_CODE"),
    (f"platformName = ref('{XIAOMI_NAME}')", f"platformName = ref('{BAIDU_NAME}')"),
    (f".includes('{XIAOMI_MATCH}')", f".includes('{BAIDU_NAME}')"),
    ("xiaomi-record-", "baidu-record-"),
]


def main() -> None:
    content = SOURCE.read_text(encoding="utf-8")
    for old, new in REPLACEMENTS:
        content = content.replace(old, new)
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_text(content, encoding="utf-8")
    print(f"Wrote {TARGET}")


if __name__ == "__main__":
    main()
