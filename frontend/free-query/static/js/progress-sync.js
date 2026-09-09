(function () {
  var fingerprintPromise = null;

  function canvasSignature() {
    try {
      var canvas = document.createElement("canvas");
      var context = canvas.getContext("2d");
      context.textBaseline = "top";
      context.font = "14px Arial";
      context.fillStyle = "#2374d8";
      context.fillRect(3, 3, 80, 20);
      context.fillStyle = "#f2b705";
      context.fillText("free-query-2026", 6, 6);
      return canvas.toDataURL();
    } catch (_error) {
      return "canvas-unavailable";
    }
  }

  function webglSignature() {
    try {
      var canvas = document.createElement("canvas");
      var gl = canvas.getContext("webgl") || canvas.getContext("experimental-webgl");
      if (!gl) return "webgl-unavailable";
      var extension = gl.getExtension("WEBGL_debug_renderer_info");
      if (!extension) return String(gl.getParameter(gl.VERSION) || "webgl");
      return [
        gl.getParameter(extension.UNMASKED_VENDOR_WEBGL),
        gl.getParameter(extension.UNMASKED_RENDERER_WEBGL)
      ].join("|");
    } catch (_error) {
      return "webgl-unavailable";
    }
  }

  function fallbackHash(value) {
    var hash = 2166136261;
    for (var index = 0; index < value.length; index++) {
      hash ^= value.charCodeAt(index);
      hash = Math.imul(hash, 16777619);
    }
    var block = (hash >>> 0).toString(16).padStart(8, "0");
    return block.repeat(8);
  }

  function sha256(value) {
    if (!window.crypto || !window.crypto.subtle || typeof TextEncoder === "undefined") {
      return Promise.resolve(fallbackHash(value));
    }
    return window.crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)).then(function (buffer) {
      return Array.prototype.map.call(new Uint8Array(buffer), function (item) {
        return item.toString(16).padStart(2, "0");
      }).join("");
    });
  }

  function getFingerprint() {
    if (fingerprintPromise) return fingerprintPromise;
    var screenSize = window.screen
      ? [Math.min(screen.width, screen.height), Math.max(screen.width, screen.height), screen.colorDepth].join("x")
      : "screen-unavailable";
    var values = [
      navigator.userAgent || "",
      navigator.platform || "",
      navigator.language || "",
      (navigator.languages || []).join(","),
      Intl.DateTimeFormat().resolvedOptions().timeZone || "",
      screenSize,
      navigator.hardwareConcurrency || 0,
      navigator.deviceMemory || 0,
      navigator.maxTouchPoints || 0,
      canvasSignature(),
      webglSignature()
    ];
    fingerprintPromise = sha256(values.join("||"));
    return fingerprintPromise;
  }

  function installQueryTokenInterceptor() {
    if (!window.XMLHttpRequest || window.XMLHttpRequest.prototype.__freeQuerySecurityInstalled) return;
    var prototype = window.XMLHttpRequest.prototype;
    var originalOpen = prototype.open;
    var originalSend = prototype.send;

    prototype.open = function (method, url) {
      this.__freeQueryUrl = String(url || "");
      return originalOpen.apply(this, arguments);
    };

    prototype.send = function (body) {
      var xhr = this;
      if (xhr.__freeQueryUrl.indexOf("/h5/mark/query") === -1) {
        return originalSend.call(xhr, body);
      }
      getFingerprint()
        .then(function (fingerprint) {
          return fetch("/h5/security/challenge", {
            method: "POST",
            cache: "no-store",
            credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fingerprint: fingerprint })
          }).then(function (response) { return response.json(); }).then(function (result) {
            if (!result || result.code !== "0000" || !result.data || !result.data.token) {
              throw new Error("challenge-failed");
            }
            xhr.setRequestHeader("X-Device-Fingerprint", fingerprint);
            xhr.setRequestHeader("X-Free-Query-Token", result.data.token);
          });
        })
        .catch(function () {
          xhr.setRequestHeader("X-Device-Fingerprint", "security-challenge-failed");
        })
        .then(function () { originalSend.call(xhr, body); });
    };
    prototype.__freeQuerySecurityInstalled = true;
  }

  window.__getFreeQueryFingerprint = getFingerprint;
  installQueryTokenInterceptor();

  var queue = window.webpackJsonp = window.webpackJsonp || [];
  var originalPush = queue.push.bind(queue);

  queue.push = function (payload) {
    var modules = payload && payload[1];
    if (modules && modules["9098"]) {
      var originalModule = modules["9098"];
      modules["9098"] = function (module, exports, require) {
        originalModule(module, exports, require);
        var component = exports.a;
        var methods = component && (component.methods || (component.options && component.options.methods));
        if (!methods || typeof methods.openModal !== "function") return;

        methods.openModal = function () {
          var vm = this;
          var startedAt = Date.now();
          var delayMs = null;

          vm.__progressStartedAt = startedAt;
          vm.__progressDelayMs = null;
          vm.__progressCompletionPending = false;
          if (vm.__progressFinishTimer) {
            clearTimeout(vm.__progressFinishTimer);
            vm.__progressFinishTimer = null;
          }

          document.body.classList.add("no-scroll");
          vm.loadingProgressModal = true;
          vm.percentage = 0;
          if (vm.loadingTrim) clearInterval(vm.loadingTrim);

          fetch("/h5/mark/policy?t=" + Date.now(), { cache: "no-store" })
            .then(function (response) { return response.json(); })
            .then(function (result) {
              var configured = Number(result && result.data && result.data.result_delay_ms);
              delayMs = Number.isFinite(configured) && configured >= 0 ? configured : 6000;
              vm.__progressDelayMs = delayMs;
              if (vm.__progressCompletionPending) finishWhenReady(vm);
            })
            .catch(function () {
              delayMs = 6000;
              vm.__progressDelayMs = delayMs;
              if (vm.__progressCompletionPending) finishWhenReady(vm);
            });

          vm.loadingTrim = setInterval(function () {
            if (delayMs === null) return;
            var percentage = delayMs === 0
              ? 99
              : Math.floor((Date.now() - startedAt) * 100 / delayMs);
            vm.percentage = Math.min(99, Math.max(vm.percentage, percentage));
            if (vm.percentage >= 99) {
              clearInterval(vm.loadingTrim);
              vm.loadingTrim = null;
            }
          }, 100);
        };

        function finishWhenReady(vm) {
          if (!vm.__progressCompletionPending || vm.__progressDelayMs === null) return;
          if (vm.__progressFinishTimer) clearTimeout(vm.__progressFinishTimer);

          var elapsed = Date.now() - vm.__progressStartedAt;
          var remaining = Math.max(0, vm.__progressDelayMs - elapsed);
          vm.__progressFinishTimer = setTimeout(function () {
            vm.__progressFinishTimer = null;
            vm.__progressCompletionPending = false;
            if (vm.loadingTrim) {
              clearInterval(vm.loadingTrim);
              vm.loadingTrim = null;
            }
            vm.percentage = 100;
            setTimeout(function () {
              document.body.classList.remove("no-scroll");
              vm.loadingProgressModal = false;
              vm.$emit("completed");
            }, 500);
          }, remaining);
        }

        methods.clearTrim = function () {
          this.__progressCompletionPending = true;
          finishWhenReady(this);
        };

        var originalCloseModal = methods.closeModal;
        methods.closeModal = function () {
          // A successful response calls clearTrim() and closeModal() back-to-back.
          // Keep the popup visible until the configured progress duration is complete.
          if (this.__progressCompletionPending) return;
          if (this.__progressFinishTimer) {
            clearTimeout(this.__progressFinishTimer);
            this.__progressFinishTimer = null;
          }
          return originalCloseModal.apply(this, arguments);
        };
      };
    }
    return originalPush(payload);
  };
})();
