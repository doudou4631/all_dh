# -*- coding: utf-8 -*-
"""Generate user 360 page from Xiaomi template (UTF-8 Chinese)."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "frontend/src/views/server/mark/user/xiaomi.vue"
TARGET = ROOT / "frontend/src/views/server/mark/user/qihu360.vue"

XIAOMI_TAB = zh(0x5C0F, 0x7C73, 0x20, 0x2D, 0x20)
XIAOMI_TITLE = "xiaomo" + zh(0x6807, 0x8BB0, 0x63D0, 0x4EA4)
QIHU360_TITLE = "360" + zh(0x6807, 0x8BB0, 0x63D0, 0x4EA4)
XIAOMI_NAME = zh(0x5C0F, 0x7C73, 0x624B, 0x673A)
XIAOMI_MATCH = zh(0x5C0F, 0x7C73)
SUBMIT_TAB_SUFFIX = zh(0x63D0, 0x4EA4, 0x53F7, 0x7801)
RECORD_TAB_SUFFIX = zh(0x4EFB, 0x52A1, 0x8BB0, 0x5F55)
SUBMIT_LABEL_EXPR = f"${{platformName.value}} - {SUBMIT_TAB_SUFFIX}"
RECORD_LABEL_EXPR = f"${{platformName.value}} - {RECORD_TAB_SUFFIX}"

CLASS_REPLACEMENTS = [
    ("mark-user-xiaomi-page", "mark-user-qihu360-page"),
    ("xiaomi-page-card", "qihu360-page-card"),
    ("xiaomi-content-shell", "qihu360-content-shell"),
    ("xiaomi-submit-panel", "qihu360-submit-panel"),
    ("xiaomi-submit-section", "qihu360-submit-section"),
    ("xiaomi-submit-title", "qihu360-submit-title"),
    ("xiaomi-submit-textarea", "qihu360-submit-textarea"),
    ("xiaomi-submit-stats__deduct", "qihu360-submit-stats__deduct"),
    ("xiaomi-submit-stats", "qihu360-submit-stats"),
    ("xiaomi-submit-actions", "qihu360-submit-actions"),
    ("xiaomi-submit-buttons", "qihu360-submit-buttons"),
    ("xiaomi-action-btn--extract", "qihu360-action-btn--extract"),
    ("xiaomi-action-btn", "qihu360-action-btn"),
    ("xiaomi-result-block", "qihu360-result-block"),
    ("xiaomi-result-head", "qihu360-result-head"),
    ("xiaomi-result-table", "qihu360-result-table"),
    ("xiaomi-result-empty", "qihu360-result-empty"),
    (XIAOMI_TITLE, QIHU360_TITLE),
    ("MarkUserXiaomi", "MarkUserQihu360"),
    ("xiaomi-record-", "qihu360-record-"),
    ("import { XIAOMI_PLATFORM_CODE } from '@/utils/markXiaomiPlatform'\n", ""),
    (f"platformName = ref('{XIAOMI_NAME}')", "platformName = ref(resolve360PlatformNameFallback(activePlatformCode.value))"),
    (f".includes('{XIAOMI_MATCH}')", ".includes('360')"),
    ("XIAOMI_PLATFORM_CODE", "activePlatformCode.value"),
]


def inject_route_platform_logic(content: str) -> str:
    marker = "const { proxy } = getCurrentInstance()\n\nconst activeTab"
    if marker not in content:
        raise RuntimeError("Unable to locate injection marker in xiaomi template")
    injection = f"""const {{ proxy }} = getCurrentInstance()
const route = useRoute()

function resolveRoutePlatformCode() {{
  const queryCode = String(route.query?.platformCode || '').trim().toLowerCase()
  if (queryCode) return resolve360PlatformCodeForPage(queryCode)
  const path = String(route.path || '').toLowerCase()
  if (path.includes('qihufirst')) return 'qihu_first'
  if (path.includes('qihusecond')) return 'qihu_second'
  if (path.includes('sanliuling')) return 'sanliuling'
  return 'sanliuling'
}}

const activePlatformCode = ref(resolveRoutePlatformCode())
const submitTabLabel = computed(() => `{SUBMIT_LABEL_EXPR}`)
const recordTabLabel = computed(() => `{RECORD_LABEL_EXPR}`)

const activeTab"""
    content = content.replace(marker, injection, 1)

    import_line = "import { computed, ref, reactive, watch, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'\n"
    content = content.replace(
        import_line,
        import_line + "import { useRoute } from 'vue-router'\n",
        1,
    )

    phone_extract = "import { extractPhoneNumbersPreservingOrder, formatExtractedPhones } from '@/utils/markPhoneExtract'\n"
    content = content.replace(
        phone_extract,
        phone_extract
        + "import {\n"
        + "  is360PlatformNameMatch,\n"
        + "  normalize360PlatformCode,\n"
        + "  resolve360PlatformCodeForPage,\n"
        + "  resolve360PlatformNameFallback\n"
        + "} from '@/utils/markQihu360Platform'\n",
        1,
    )

    content = content.replace(
        f"String(item.platformCode || '').toLowerCase() === activePlatformCode.value)\n      || list.find((item) => String(item.platformName || '').includes('360'))",
        "String(item.platformCode || '').toLowerCase() === activePlatformCode.value)\n      || list.find((item) => is360PlatformNameMatch(item))",
        1,
    )

    old_mount = """onMounted(() => {
  loadPlatformInfo()
  syncRecordTabData()
})"""
    new_mount = """watch(
  () => route.query?.platformCode,
  (code) => {
    const nextCode = resolve360PlatformCodeForPage(code || activePlatformCode.value)
    if (nextCode === activePlatformCode.value) return
    activePlatformCode.value = nextCode
    queryParams.platformCode = nextCode
    platformName.value = resolve360PlatformNameFallback(nextCode)
    loadPlatformInfo()
    syncRecordTabData()
  }
)

onMounted(() => {
  activePlatformCode.value = resolveRoutePlatformCode()
  queryParams.platformCode = activePlatformCode.value
  platformName.value = resolve360PlatformNameFallback(activePlatformCode.value)
  loadPlatformInfo()
  syncRecordTabData()
})"""
    content = content.replace(old_mount, new_mount, 1)
    return content


def main() -> None:
    content = SOURCE.read_text(encoding="utf-8")
    for old, new in CLASS_REPLACEMENTS:
        content = content.replace(old, new)
    content = content.replace(
        f'<el-tab-pane label="{XIAOMI_TAB}{SUBMIT_TAB_SUFFIX}" name="submit">',
        '<el-tab-pane :label="submitTabLabel" name="submit">',
    )
    content = content.replace(
        f'<el-tab-pane label="{XIAOMI_TAB}{RECORD_TAB_SUFFIX}" name="record">',
        '<el-tab-pane :label="recordTabLabel" name="record">',
    )
    content = inject_route_platform_logic(content)
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_text(content, encoding="utf-8")
    print(f"Wrote {TARGET}")


if __name__ == "__main__":
    main()
