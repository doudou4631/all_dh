#!/usr/bin/env python3
"""Download remote images and patch project to use local /assets/icons/ paths."""
import hashlib
import os
import re
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICONS = os.path.join(ROOT, "assets", "icons")
os.makedirs(ICONS, exist_ok=True)

# url -> local filename (stable names for known assets)
NAMED = {
    "https://biaoji.aleo1314.vip/wechat/customer-wechat2.png": "customer-wechat.png",
    "https://biaoji.aleo1314.vip/free-query-icons/teddy.png": "teddy.png",
    "https://biaoji.aleo1314.vip/free-query-icons/tencent.png": "tencent.png",
    "https://biaoji.aleo1314.vip/free-query-icons/360.png": "360.png",
    "https://biaoji.aleo1314.vip/free-query-icons/baidu.ico": "baidu.ico",
    "https://biaoji.aleo1314.vip/free-query-icons/sogou.ico": "sogou.ico",
    "https://biaoji.aleo1314.vip/free-query-icons/mobile.png": "mobile.png",
    "https://biaoji.aleo1314.vip/free-query-icons/unicom.svg": "unicom.svg",
    "https://biaoji.aleo1314.vip/free-query-icons/dianhuabang.ico": "dianhuabang.ico",
    "https://biaoji.aleo1314.vip/free-query-icons/xiaomi.jpeg": "xiaomi.jpeg",
    "https://so.360tres.com/d/inn/733e59e6/360Logo_36x36.png": "360-home.png",
    "https://www.baidu.com/favicon.ico": "baidu-home.ico",
    "https://img.teddymobile.cn/www/images/news/logo.png": "teddy-home.png",
    "https://www.chinaunicom.com/favicon.ico": "unicom-home.ico",
    "https://img.onlinedown.net/download/202212/android/logo/8ad4e75c5d947e0ce7ab2749c420e325.png": "tencent-home.png",
    "https://dss0.bdstatic.com/-0U0bnSm1A5BphGlnYG/tam-ogel/-618389961_1825120068_121_121.png": "mobile-home.png",
    "https://www.dianhua.cn/favicon.ico": "dianhuabang-home.ico",
    "https://www.sogou.com/images/logo/new/favicon.ico?nv=1&v=3": "sogou-home.ico",
    "https://www.sogou.com/images/logo/new/favicon.ico": "sogou-home.ico",
}

IMAGE_EXT = (".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".svg")


def local_path(filename):
    return "/assets/icons/" + filename


def url_to_name(url):
    if url in NAMED:
        return NAMED[url]
    base = url.split("?")[0].rstrip("/")
    name = os.path.basename(urllib.parse.unquote(base))
    if not name or "." not in name:
        h = hashlib.md5(url.encode()).hexdigest()[:10]
        name = "img-" + h + ".png"
    name = re.sub(r"[^\w.\-]", "_", name)
    return name


def collect_urls():
    urls = set(NAMED.keys())
    for dirpath, _, files in os.walk(ROOT):
        if "node_modules" in dirpath or ".git" in dirpath:
            continue
        for fn in files:
            if not fn.endswith((".js", ".html", ".css")):
                continue
            path = os.path.join(dirpath, fn)
            try:
                text = open(path, encoding="utf-8", errors="ignore").read()
            except OSError:
                continue
            for u in re.findall(r"https?://[^\s\"'\\)]+", text):
                u = u.rstrip("\\")
                low = u.lower()
                if any(low.split("?")[0].endswith(ext) for ext in IMAGE_EXT):
                    urls.add(u)
                elif "favicon" in low or "/logo" in low or "wechat" in low:
                    urls.add(u)
    return sorted(urls)


def download(url, filename):
    dest = os.path.join(ICONS, filename)
    if os.path.exists(dest) and os.path.getsize(dest) > 0:
        print("skip", filename)
        return dest
    print("get ", url, "->", filename)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    last_err = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=90) as resp:
                data = resp.read()
            with open(dest, "wb") as f:
                f.write(data)
            print("saved", filename, len(data))
            return dest
        except Exception as e:
            last_err = e
            print("retry", attempt + 1, filename, e)
    print("FAIL", filename, last_err)
    return None


def patch_files(url_map):
    for dirpath, _, files in os.walk(ROOT):
        if "scripts" in dirpath:
            continue
        for fn in files:
            if not fn.endswith((".js", ".html", ".css")):
                continue
            path = os.path.join(dirpath, fn)
            try:
                text = open(path, encoding="utf-8", errors="ignore").read()
            except OSError:
                continue
            orig = text
            for url, local in sorted(url_map.items(), key=lambda x: -len(x[0])):
                text = text.replace(url, local)
            if text != orig:
                open(path, "w", encoding="utf-8").write(text)
                print("patched", os.path.relpath(path, ROOT))


def main():
    urls = collect_urls()
    url_map = {}
    for url in urls:
        name = url_to_name(url)
        if download(url, name) is None:
            continue
        url_map[url] = local_path(name)
        # also map without query string
        base = url.split("?")[0]
        if base != url:
            url_map[base] = local_path(name)
    patch_files(url_map)
    print("done", len(url_map), "mappings")


if __name__ == "__main__":
    main()
