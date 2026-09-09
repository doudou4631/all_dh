(function () {
  var DEFAULT_CONTACT = {
    phone: "13027616171",
    qrCode: "/static/img/contact-qr.png"
  };

  function getContact() {
    var page = window.__FREE_QUERY_PAGE__ || {};
    var hasPhone = Object.prototype.hasOwnProperty.call(page, "phone");
    return {
      phone: hasPhone ? String(page.phone || "").trim() : DEFAULT_CONTACT.phone,
      qrCode: String(page.qrCode || DEFAULT_CONTACT.qrCode)
    };
  }

  function escapeHtml(value) {
    var chars = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };
    return String(value).replace(/[&<>"']/g, function (char) {
      return chars[char];
    });
  }

  function isDetailsPage() {
    var hash = location.hash || "";
    return /pages\/details\/index/i.test(hash) || !!document.querySelector(".details-page");
  }

  function getMarkCount() {
    var cards = document.querySelectorAll(".platform-card");
    if (cards.length) return cards.length;
    var all = document.querySelectorAll("*");
    for (var i = 0; i < all.length; i++) {
      var v = all[i].__vue__;
      if (!v) continue;
      var qr = v.queryResult || (v.$data && v.$data.queryResult);
      if (qr && Array.isArray(qr.item)) return qr.item.length;
    }
    return 0;
  }

  function ensureHeaderExtras() {
    var header = document.querySelector(".details-header");
    if (!header) return;

    if (!header.querySelector(".details-header__tip")) {
      var tip = document.createElement("div");
      tip.className = "details-header__tip";
      tip.textContent = "针对号码:";
      header.insertBefore(tip, header.firstChild);
    }

    var count = getMarkCount();
    var summary = header.querySelector(".details-header__summary");
    if (!summary) {
      summary = document.createElement("div");
      summary.className = "details-header__summary";
      header.appendChild(summary);
    }
    if (document.querySelector(".details-empty") && count === 0) {
      summary.textContent = "未查询到平台标记";
    } else {
      summary.innerHTML =
        '被 <span class="details-header__summary-num">' +
        count +
        "</span> 个平台标记";
    }
  }

  function showWxModal() {
    var contact = getContact();
    var phone = escapeHtml(contact.phone);
    var qrCode = escapeHtml(contact.qrCode);
    var host = document.getElementById("phone-frame") || document.body;
    var modal = document.querySelector(".wx-qr-modal");
    if (!modal) {
      modal = document.createElement("div");
      modal.className = "wx-qr-modal";
      modal.innerHTML =
        '<div class="wx-qr-modal__mask" data-close="1"></div>' +
        '<div class="wx-qr-modal__panel" role="dialog">' +
        '<img class="wx-qr-modal__qr" alt="客服微信" src="' + qrCode + '">' +
        '<div class="wx-qr-modal__tip">长按识别二维码添加客服微信</div>' +
        '<div class="wx-qr-modal__tel">或拨打 <a href="tel:' +
        phone +
        '">' +
        phone +
        "</a></div>" +
        "</div>";
      modal.addEventListener("click", function (e) {
        if (e.target.getAttribute("data-close") === "1") hideWxModal();
      });
    }
    if (modal.parentNode !== host) host.appendChild(modal);
    // 清除可能残留的 fixed 定位内联样式，确保铺满 phone-frame
    modal.style.cssText = "";
    modal.classList.add("wx-qr-modal--show");
    document.body.style.overflow = "hidden";
  }

  function hideWxModal() {
    var modal = document.querySelector(".wx-qr-modal");
    if (modal) modal.classList.remove("wx-qr-modal--show");
    document.body.style.overflow = "";
  }

  function ensureOnlineServiceButton() {
    var enabled = window.__FREE_QUERY_PAGE__ && window.__FREE_QUERY_PAGE__.showOnlineService === true;
    document.body.classList.toggle("online-service-enabled", enabled);
    var fallback = document.querySelector(".online-service-float-ball");
    if (!enabled) {
      if (fallback) fallback.remove();
      return;
    }
    if (document.querySelector(".float-chat-ball")) {
      if (fallback) fallback.remove();
      return;
    }
    if (!fallback) {
      fallback = document.createElement("button");
      fallback.type = "button";
      fallback.className = "online-service-float-ball";
      fallback.innerHTML = '<span class="online-service-float-ball__icon">&#58966;</span><span class="online-service-float-ball__label">客服</span>';
      fallback.addEventListener("click", function () {
        if (typeof window.MyChat === "function") window.MyChat("showPanel");
      });
      document.body.appendChild(fallback);
    }
  }

  function ensureFloat() {
    var header = document.querySelector(".details-header");
    if (!header) return;
    var contact = getContact();
    var floatBox = document.querySelector(".details-float");
    if (!floatBox) {
      floatBox = document.createElement("div");
      floatBox.className = "details-float";
      floatBox.innerHTML =
        (contact.phone ? '<div class="details-float__btn" data-act="tel">' +
        '<span class="details-float__icon details-float__icon--tel"></span>' +
        "<span>服务电话</span></div>" : "") +
        '<div class="details-float__btn" data-act="wx">' +
        '<span class="details-float__icon details-float__icon--wx"></span>' +
        "<span>客服微信</span></div>";
      floatBox.addEventListener("click", function (e) {
        var btn = e.target.closest("[data-act]");
        if (!btn) return;
        if (btn.getAttribute("data-act") === "tel") {
          if (contact.phone) window.location.href = "tel:" + contact.phone;
        } else {
          showWxModal();
        }
      });
    }
    if (floatBox.parentNode !== header) header.appendChild(floatBox);
  }

  function ensureBottom() {
    if (document.querySelector(".result-bottom")) return;
    var contact = getContact();
    var phone = escapeHtml(contact.phone);
    var qrCode = escapeHtml(contact.qrCode);
    var content =
      document.querySelector(".details-page__content") ||
      document.querySelector(".details-page");
    if (!content) return;

    var box = document.createElement("div");
    box.className = "result-bottom";
    box.innerHTML =
      '<button type="button" class="result-bottom__back">返回重新查询</button>' +
      (phone ? '<div class="result-bottom__contact">联系我们: ' + phone + "</div>" : "") +
      '<div class="result-bottom__qr"><img alt="qr" src="' + qrCode + '"></div>';
    box.querySelector(".result-bottom__back").addEventListener("click", function () {
      if (window.history.length > 1) window.history.back();
      else location.hash = "#/";
    });
    content.appendChild(box);
  }

  function cleanup() {
    document.body.classList.remove("result-skin-on");
    hideWxModal();
    var modal = document.querySelector(".wx-qr-modal");
    if (modal) modal.remove();
    var f = document.querySelector(".details-float");
    if (f) f.remove();
    var b = document.querySelector(".result-bottom");
    if (b) b.remove();
  }

  function applySkin() {
    ensureOnlineServiceButton();
    if (!isDetailsPage()) {
      cleanup();
      return;
    }
    document.body.classList.add("result-skin-on");
    ensureHeaderExtras();
    ensureFloat();
    ensureBottom();
  }

  var configReady = window.__FREE_QUERY_PAGE_READY__ || Promise.resolve();
  configReady.finally(function () {
    setInterval(applySkin, 400);
    window.addEventListener("hashchange", function () {
      setTimeout(applySkin, 60);
      setTimeout(applySkin, 400);
    });
    document.addEventListener("DOMContentLoaded", applySkin);
    setTimeout(applySkin, 100);
  });
})();
