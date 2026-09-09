(function () {
  // Add another page here, then open it with /query/<page-code>/.
  // Only phone and qrCode vary; all pages share the existing query business logic.
  var defaultCode = "default";
  var pageCodePattern = /^[a-z0-9][a-z0-9_-]{0,31}$/;
  var pages = {
    default: {
      phone: "13027616171",
      qrCode: "/static/img/contact-qr.png",
      showOnlineService: false
    },
    "1": {
      phone: "13027616171",
      qrCode: "/static/img/contact-qr.png",
      showOnlineService: false
    }
  };

  function normalizeCode(value) {
    return String(value || "").trim().toLowerCase();
  }

  function publicAssetUrl(value) {
    var path = String(value || "");
    if (/^\/(files|profile)\//.test(path) && /^\/query\//.test(window.location.pathname)) {
      return "/query" + path;
    }
    return path;
  }

  function codeFromLocation() {
    var params = new URLSearchParams(window.location.search || "");
    var queryCode = normalizeCode(params.get("page"));
    if (queryCode) return queryCode;

    var parts = window.location.pathname.split("/").filter(Boolean);
    var queryIndex = parts.indexOf("query");
    if (queryIndex !== -1 && parts[queryIndex + 1]) {
      return normalizeCode(parts[queryIndex + 1]);
    }
    if (parts.length === 1 && parts[0] !== "index1.html") {
      return normalizeCode(parts[0]);
    }
    return defaultCode;
  }

  var requestedCode = codeFromLocation();
  var known = Object.prototype.hasOwnProperty.call(pages, requestedCode) || pageCodePattern.test(requestedCode);
  var code = known ? requestedCode : defaultCode;
  var selected = pages[code] || pages[defaultCode];

  window.__FREE_QUERY_PAGE__ = {
    code: code,
    requestedCode: requestedCode,
    known: known,
    phone: selected.phone,
    qrCode: selected.qrCode,
    showOnlineService: selected.showOnlineService === true
  };
  document.documentElement.setAttribute("data-query-page", code);

  if (known && code !== defaultCode && window.fetch) {
    window.__FREE_QUERY_PAGE_READY__ = fetch(
      "/h5/page/config?code=" + encodeURIComponent(code),
      { cache: "no-store", credentials: "same-origin" }
    )
      .then(function (response) {
        if (!response.ok) throw new Error("page config request failed");
        return response.json();
      })
      .then(function (payload) {
        var data = payload && payload.data;
        if (!data || !Object.prototype.hasOwnProperty.call(data, "phone") || !data.qr_code) {
          return window.__FREE_QUERY_PAGE__;
        }
        window.__FREE_QUERY_PAGE__.phone = String(data.phone || "").trim();
        window.__FREE_QUERY_PAGE__.qrCode = publicAssetUrl(data.qr_code);
        window.__FREE_QUERY_PAGE__.showOnlineService = data.show_online_service === true;
        return window.__FREE_QUERY_PAGE__;
      })
      .catch(function () {
        return window.__FREE_QUERY_PAGE__;
      });
  } else {
    window.__FREE_QUERY_PAGE_READY__ = Promise.resolve(window.__FREE_QUERY_PAGE__);
  }
})();
