#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""本地静态站点 + API 反向代理，便于离线打开原 H5 页面。"""

from __future__ import annotations

import gzip
import json
import ipaddress
import mimetypes
import os
import re
import sys
import threading
import time
import urllib.request
import uuid
from http.cookies import SimpleCookie
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

ROOT = Path(__file__).resolve().parent
PORT = int(os.environ.get("PORT", "18765"))
BACKEND_UPSTREAM = os.environ.get("BACKEND_UPSTREAM", "http://127.0.0.1:8080").rstrip("/")
INTERNAL_KEY = os.environ.get(
    "FREE_QUERY_INTERNAL_KEY",
    "13115afeaf374a43a8bb2b53238d5bf5ee2392c196e04c3d928040d5ff4d4992",
)
MAX_REQUEST_BYTES = 16 * 1024
RESULT_TTL_SECONDS = 10 * 60
RESULT_CACHE: dict[str, tuple[float, dict]] = {}
RESULT_CACHE_LOCK = threading.Lock()
PAGE_CODE_PATTERN = re.compile(r"^[a-z0-9][a-z0-9_-]{0,31}$")

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        self._visitor_id_to_set: str | None = None
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def log_message(self, fmt: str, *args) -> None:
        try:
            sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), fmt % args))
            sys.stderr.flush()
        except (OSError, ValueError):
            pass

    def end_headers(self) -> None:
        # 避免浏览器强缓存本地调试资源
        request_path = urlparse(self.path).path
        if request_path.startswith(("/static/", "/cdn/")):
            self.send_header("Cache-Control", "public, max-age=3600, stale-while-revalidate=86400")
        else:
            self.send_header("Cache-Control", "no-store")
        self.send_header(
            "Content-Security-Policy",
            "default-src 'self'; connect-src 'self'; img-src 'self' data: blob:; "
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
            "style-src 'self' 'unsafe-inline'; font-src 'self' data:; "
            "frame-src 'none'; object-src 'none'; base-uri 'self'",
        )
        if self._visitor_id_to_set:
            self.send_header(
                "Set-Cookie",
                f"fq_visitor_id={self._visitor_id_to_set}; Path=/; Max-Age=31536000; SameSite=Lax",
            )
        origin = self.headers.get("Origin")
        if origin and self._is_same_origin():
            self.send_header("Access-Control-Allow-Origin", origin)
            self.send_header("Vary", "Origin")
        super().end_headers()

    def do_OPTIONS(self) -> None:
        if not self._is_same_origin():
            self.send_error(403, "Forbidden")
            return
        self.send_response(204)
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/health":
            self._write_json({"status": "ok", "service": "free-query"})
            return
        if path.startswith(("/files/", "/profile/", "/query/files/", "/query/profile/")):
            self._proxy_backend_asset(path)
            return
        if path == "/h5/advert/getList":
            self._ensure_visitor_id()
            self._write_api_success({"top": [], "bottom": [], "pop_up_content": "", "pop_up_day": 0})
            return
        if path == "/h5/mark/policy":
            try:
                self._write_api_success(self._query_policy_backend())
            except Exception:  # noqa: BLE001
                self._write_api_success({"result_delay_ms": 6000})
            return
        if path == "/h5/page/config":
            page_code = str(parse_qs(urlparse(self.path).query).get("code", ["1"])[-1]).strip().lower()
            if not PAGE_CODE_PATTERN.fullmatch(page_code):
                self._write_api_error("查询页面不存在", status=404)
                return
            try:
                self._write_api_success(self._query_page_config_backend(page_code))
            except Exception:  # noqa: BLE001
                self._write_api_success({
                    "page_code": page_code,
                    "phone": "13027616171",
                    "qr_code": "/static/img/contact-qr.png",
                })
            return
        if path.startswith("/h5/"):
            self._write_api_error("接口已由本项目接管")
            return
        if self._serve_local_or_fallback():
            return
        self.send_error(404, "Not Found")

    def do_POST(self) -> None:
        if not self._is_same_origin():
            self._write_api_error("禁止跨站调用查询接口", status=403)
            return
        if int(self.headers.get("Content-Length", "0") or "0") > MAX_REQUEST_BYTES:
            self._write_api_error("请求内容过大", status=413)
            return
        path = urlparse(self.path).path
        if path == "/h5/security/challenge":
            self._handle_security_challenge()
            return
        if path == "/h5/mark/query":
            self._handle_mark_query()
            return
        if path == "/h5/mark/getInfo":
            self._handle_mark_get_info()
            return
        if path.startswith("/h5/"):
            self._write_api_error("该接口未开放")
            return
        self.send_error(405, "Method Not Allowed")

    def do_PUT(self) -> None:
        self.send_error(405, "Method Not Allowed")

    def do_DELETE(self) -> None:
        self.send_error(405, "Method Not Allowed")

    def _read_payload(self) -> dict:
        length = int(self.headers.get("Content-Length", "0") or "0")
        raw = self.rfile.read(length) if length > 0 else b""
        payload: dict = {}
        if raw:
            text = raw.decode("utf-8", errors="replace")
            try:
                value = json.loads(text)
                if isinstance(value, dict):
                    payload.update(value)
            except json.JSONDecodeError:
                payload.update({key: values[-1] for key, values in parse_qs(text).items()})
        payload.update({key: values[-1] for key, values in parse_qs(urlparse(self.path).query).items()})
        return payload

    def _handle_mark_query(self) -> None:
        payload = self._read_payload()
        number = str(payload.get("number") or payload.get("phoneNumber") or "").strip()
        if not number:
            self._write_api_error("请输入查询号码")
            return
        visitor_id = self._ensure_visitor_id()
        fingerprint = self.headers.get("X-Device-Fingerprint", "")
        query_token = self.headers.get("X-Free-Query-Token", "")
        if not self._is_valid_fingerprint(fingerprint) or not query_token:
            self._write_api_error("安全验证已失效，请刷新页面后重试")
            return
        try:
            result = self._query_backend(number, visitor_id, fingerprint, query_token)
        except Exception as exc:  # noqa: BLE001
            self._write_api_error(str(exc))
            return

        result_id = uuid.uuid4().hex
        with RESULT_CACHE_LOCK:
            self._cleanup_result_cache()
            RESULT_CACHE[result_id] = (time.time(), self._adapt_query_result(number, result))
        self._write_api_success({"id": result_id})

    def _handle_security_challenge(self) -> None:
        payload = self._read_payload()
        fingerprint = str(payload.get("fingerprint") or "").lower()
        if not self._is_valid_fingerprint(fingerprint):
            self._write_api_error("无法识别查询设备，请刷新页面后重试")
            return
        visitor_id = self._ensure_visitor_id()
        try:
            result = self._query_challenge_backend(visitor_id, fingerprint)
        except Exception as exc:  # noqa: BLE001
            self._write_api_error(str(exc))
            return
        self._write_api_success(result)

    def _handle_mark_get_info(self) -> None:
        payload = self._read_payload()
        result_id = str(payload.get("id") or "").strip()
        with RESULT_CACHE_LOCK:
            self._cleanup_result_cache()
            cached = RESULT_CACHE.get(result_id)
        if cached is None:
            self._write_api_error("查询结果已失效，请重新查询")
            return
        self._write_api_success(cached[1])

    def _query_backend(self, number: str, visitor_id: str, fingerprint: str, query_token: str) -> dict:
        body = json.dumps({"phoneNumber": number}, ensure_ascii=False).encode("utf-8")
        client_ip = self._client_ip()
        request = urllib.request.Request(
            BACKEND_UPSTREAM + "/server/freeQuery/single",
            data=body,
            headers={
                "Content-Type": "application/json",
                "Accept": "application/json",
                "X-Visitor-Id": visitor_id,
                "X-Free-Query-Key": INTERNAL_KEY,
                "X-Free-Query-Client-IP": client_ip,
                "X-Device-Fingerprint": fingerprint,
                "X-Free-Query-Token": query_token,
                "User-Agent": self.headers.get("User-Agent", "free-query-client"),
            },
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=90) as response:
            value = json.loads(response.read().decode("utf-8"))
        if int(value.get("code", 500)) != 200:
            raise RuntimeError(value.get("msg") or "号码查询失败")
        data = value.get("data") or {}
        if data.get("success") is False:
            raise RuntimeError(data.get("message") or "号码查询失败")
        return data

    def _query_challenge_backend(self, visitor_id: str, fingerprint: str) -> dict:
        body = json.dumps({"fingerprint": fingerprint}).encode("utf-8")
        request = urllib.request.Request(
            BACKEND_UPSTREAM + "/server/freeQuery/challenge",
            data=body,
            headers={
                "Content-Type": "application/json",
                "Accept": "application/json",
                "X-Free-Query-Key": INTERNAL_KEY,
                "X-Free-Query-Client-IP": self._client_ip(),
                "X-Visitor-Id": visitor_id,
                "User-Agent": self.headers.get("User-Agent", "free-query-client"),
            },
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=10) as response:
            value = json.loads(response.read().decode("utf-8"))
        if int(value.get("code", 500)) != 200:
            raise RuntimeError(value.get("msg") or "安全验证失败")
        return value.get("data") or {}

    def _query_policy_backend(self) -> dict:
        request = urllib.request.Request(
            BACKEND_UPSTREAM + "/server/freeQuery/policy",
            headers={"Accept": "application/json", "X-Free-Query-Key": INTERNAL_KEY},
            method="GET",
        )
        with urllib.request.urlopen(request, timeout=5) as response:
            value = json.loads(response.read().decode("utf-8"))
        if int(value.get("code", 500)) != 200:
            raise RuntimeError("无法读取查询延时配置")
        data = value.get("data") or {}
        return {"result_delay_ms": max(0, int(data.get("resultDelayMs") or 0))}

    def _query_page_config_backend(self, page_code: str) -> dict:
        url = BACKEND_UPSTREAM + "/server/freeQuery/page-config?code=" + page_code
        request = urllib.request.Request(
            url,
            headers={"Accept": "application/json", "X-Free-Query-Key": INTERNAL_KEY},
            method="GET",
        )
        with urllib.request.urlopen(request, timeout=5) as response:
            value = json.loads(response.read().decode("utf-8"))
        if int(value.get("code", 500)) != 200:
            raise RuntimeError("无法读取查询页面配置")
        data = value.get("data") or {}
        return {
            "page_code": str(data.get("pageCode") or page_code),
            "phone": "" if data.get("phone") is None else str(data.get("phone")),
            "qr_code": str(data.get("qrCode") or "/static/img/contact-qr.png"),
            "show_online_service": bool(data.get("showOnlineService")),
        }

    def _client_ip(self) -> str:
        peer = self.client_address[0]
        try:
            peer_ip = ipaddress.ip_address(peer)
        except ValueError:
            return "unknown"
        if peer_ip.is_loopback:
            forwarded = self.headers.get("X-Real-IP", "")
            try:
                return ipaddress.ip_address(forwarded.strip()).compressed
            except ValueError:
                pass
        return peer_ip.compressed

    def _is_same_origin(self) -> bool:
        origin = self.headers.get("Origin")
        if not origin:
            return True
        parsed = urlparse(origin)
        host = self.headers.get("Host", "").lower()
        return parsed.scheme in {"http", "https"} and parsed.netloc.lower() == host

    def _ensure_visitor_id(self) -> str:
        cookie = SimpleCookie()
        try:
            cookie.load(self.headers.get("Cookie", ""))
        except ValueError:
            cookie = SimpleCookie()
        morsel = cookie.get("fq_visitor_id")
        visitor_id = morsel.value if morsel else ""
        if (not 8 <= len(visitor_id) <= 64
                or not visitor_id.replace("-", "").replace("_", "").isalnum()):
            visitor_id = uuid.uuid4().hex
            self._visitor_id_to_set = visitor_id
        return visitor_id

    def _is_valid_fingerprint(self, value: str) -> bool:
        return len(value) == 64 and all(char in "0123456789abcdef" for char in value)

    def _adapt_query_result(self, number: str, data: dict) -> dict:
        code_map = {
            "泰迪熊": "tdx", "360": "sll", "腾讯": "tx", "百度": "bd",
            "联通管家": "lt", "联通": "lt", "移动高频": "ydhigh",
            "电话邦": "dhb", "搜狗": "sghmt", "小米手机": "xm", "小米": "xm",
        }
        marked_items = []
        for item in data.get("results") or []:
            if not item.get("marked"):
                continue
            name = str(item.get("platformName") or "未知平台")
            icon_name = str(item.get("platformIcon") or "")
            icon_code = icon_name[:-4] if icon_name.endswith(".png") else ""
            if icon_code not in {"tdx", "sll", "tx", "bd", "lt", "ydhigh", "dhb", "sghmt", "xm", "114"}:
                icon_code = code_map.get(name, "114")
            result_text = str(item.get("resultText") or "存在标记")
            if icon_code == "ydhigh" and result_text.strip().lower() == "yes":
                result_text = "高频拦截-拨打部分电话会有拦截"
            elif result_text.lower().startswith("yes-"):
                result_text = result_text[4:]
            marked_items.append({
                "platform": icon_code,
                "platform_name": name,
                "content": result_text,
                "sales_price": 0,
                "clear_time": "",
                "clear_explain": "",
                "is_disabled": 1,
            })
        return {
            "number": number,
            "is_mark": 1 if marked_items else 0,
            "title": f"发现 {len(marked_items)} 个平台标记" if marked_items else "",
            "item": marked_items,
            "fail_title": "",
            "fail_explain": "",
            "promotion_link": "",
        }

    def _cleanup_result_cache(self) -> None:
        expires_before = time.time() - RESULT_TTL_SECONDS
        expired = [key for key, value in RESULT_CACHE.items() if value[0] < expires_before]
        for key in expired:
            RESULT_CACHE.pop(key, None)

    def _write_api_success(self, data: dict) -> None:
        self._write_json({"code": "0000", "msg": "操作成功", "data": data})

    def _write_api_error(self, message: str, status: int = 200) -> None:
        self._write_json({"code": "5000", "msg": message, "data": None}, status=status)

    def _write_json(self, payload: dict, status: int = 200) -> None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _proxy_backend_asset(self, path: str) -> None:
        try:
            upstream_path = path.removeprefix("/query") if path.startswith("/query/") else path
            request = urllib.request.Request(
                f"{BACKEND_UPSTREAM}{upstream_path}",
                headers={"Accept": self.headers.get("Accept", "*/*")},
            )
            with urllib.request.urlopen(request, timeout=15) as response:
                data = response.read()
                self.send_response(response.status)
                self.send_header(
                    "Content-Type",
                    response.headers.get("Content-Type", "application/octet-stream"),
                )
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
        except Exception:  # noqa: BLE001
            self.send_error(404, "Not Found")

    def _local_file(self) -> Path | None:
        path = urlparse(self.path).path
        if path in ("", "/", "/index1.html", "/query", "/query/"):
            return ROOT / "index1.html"
        parts = [part for part in path.split("/") if part]
        is_page_path = (
            len(parts) == 1 and PAGE_CODE_PATTERN.fullmatch(parts[0])
        ) or (
            len(parts) == 2 and parts[0] == "query" and PAGE_CODE_PATTERN.fullmatch(parts[1])
        )
        if is_page_path:
            return ROOT / "index1.html"
        if not (path.startswith("/static/") or path.startswith("/cdn/")):
            return None
        # 去掉开头 /
        rel = path.lstrip("/").replace("\\", "/")
        candidate = (ROOT / rel).resolve()
        try:
            candidate.relative_to(ROOT)
        except ValueError:
            return None
        if candidate.is_file():
            return candidate
        return None

    def _serve_local_or_fallback(self) -> bool:
        local = self._local_file()
        if local is None:
            return False
        ctype = mimetypes.guess_type(str(local))[0] or "application/octet-stream"
        data = local.read_bytes()
        accepts_gzip = "gzip" in self.headers.get("Accept-Encoding", "").lower()
        is_text_asset = ctype.startswith("text/") or ctype in {
            "application/javascript",
            "application/json",
        }
        use_gzip = accepts_gzip and is_text_asset and len(data) >= 1024
        if use_gzip:
            data = gzip.compress(data, compresslevel=6)
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        if use_gzip:
            self.send_header("Content-Encoding", "gzip")
            self.send_header("Vary", "Accept-Encoding")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)
        return True


def main() -> None:
    # Windows 下快速复用端口
    class ReusableServer(ThreadingHTTPServer):
        allow_reuse_address = True

    # 补齐部分 MIME
    mimetypes.add_type("application/javascript", ".js")
    mimetypes.add_type("text/css", ".css")
    mimetypes.add_type("font/woff2", ".woff2")
    mimetypes.add_type("application/json", ".json")

    server = ReusableServer(("127.0.0.1", PORT), Handler)
    url = f"http://127.0.0.1:{PORT}/#/"
    print("=" * 60)
    print("号码标记查询平台 - 本地镜像已启动")
    print(f"请用浏览器打开: {url}")
    print("按 Ctrl+C 停止服务")
    print("=" * 60)
    try:
        # 尝试自动打开浏览器
        if sys.platform.startswith("win"):
            os.startfile(url)  # type: ignore[attr-defined]
    except Exception:
        pass
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n已停止")
        server.server_close()


if __name__ == "__main__":
    main()
