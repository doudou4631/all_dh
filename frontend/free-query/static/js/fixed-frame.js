(function () {
  var W = 453;

  function unlockHeight(el) {
    if (!el || !el.style) return;
    el.style.setProperty("height", "auto", "important");
    el.style.setProperty("max-height", "none", "important");
    el.style.setProperty("min-height", "0", "important");
    el.style.setProperty("overflow-y", "visible", "important");
    el.style.setProperty("transform", "none", "important");
  }

  function lockWidth(el) {
    if (!el || !el.style) return;
    var desktop = window.innerWidth >= W + 1;
    if (desktop) {
      el.style.setProperty("width", W + "px", "important");
      el.style.setProperty("min-width", W + "px", "important");
      el.style.setProperty("max-width", W + "px", "important");
    } else {
      el.style.setProperty("width", "100%", "important");
      el.style.setProperty("max-width", W + "px", "important");
      el.style.setProperty("min-width", "0", "important");
    }
    unlockHeight(el);
  }

  function fitFrame() {
    var frame = document.getElementById("phone-frame");
    var app = document.getElementById("app");
    if (!frame) return;

    lockWidth(frame);
    lockWidth(app);
    frame.style.setProperty("margin-left", "auto", "important");
    frame.style.setProperty("margin-right", "auto", "important");
    frame.style.setProperty("transform", "none", "important");

    var uni = frame.querySelector("uni-app");
    lockWidth(uni);

    var mains = frame.querySelectorAll(
      ".main, .uni-page-body, uni-page-body, .uni-page-wrapper, .details-page, .details-page__content, .details-header, .main-scroll, .content-section"
    );
    for (var i = 0; i < mains.length; i++) {
      lockWidth(mains[i]);
    }

    document.documentElement.style.setProperty("overflow-y", "auto", "important");
    document.body.style.setProperty("overflow-y", "auto", "important");
    document.body.style.setProperty("height", "auto", "important");
  }

  window.addEventListener("resize", fitFrame);
  document.addEventListener("DOMContentLoaded", fitFrame);
  setTimeout(fitFrame, 0);
  setTimeout(fitFrame, 100);
  setTimeout(fitFrame, 500);
  setInterval(fitFrame, 600);

  var obs = new MutationObserver(function () {
    fitFrame();
  });
  document.addEventListener("DOMContentLoaded", function () {
    var frame = document.getElementById("phone-frame");
    if (frame) obs.observe(frame, { childList: true, subtree: true, attributes: true });
  });
})();
