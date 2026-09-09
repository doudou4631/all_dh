(function () {
  // 拦截跳转到支付/订单相关页面
  var blocked = [
    "/pages/payment/",
    "/pages/order/",
    "pages/payment/",
    "pages/order/",
    "payment/guide",
    "payment/payloading",
    "order/index",
    "order/detail",
    "order/difference",
    "order/processing",
  ];

  function isBlocked(url) {
    if (!url) return false;
    var s = String(url);
    for (var i = 0; i < blocked.length; i++) {
      if (s.indexOf(blocked[i]) !== -1) return true;
    }
    return false;
  }

  function guard() {
    var hash = location.hash || "";
    if (isBlocked(hash)) {
      location.replace("/#/");
    }
  }

  // 覆盖常见跳转
  function wrapHistory(fnName) {
    var raw = history[fnName];
    if (typeof raw !== "function") return;
    history[fnName] = function (state, title, url) {
      if (isBlocked(url) || isBlocked(location.hash)) {
        return raw.call(history, state, title, "/#/");
      }
      return raw.apply(history, arguments);
    };
  }
  wrapHistory("pushState");
  wrapHistory("replaceState");

  window.addEventListener("hashchange", guard);
  document.addEventListener("DOMContentLoaded", guard);
  setInterval(guard, 800);
})();
