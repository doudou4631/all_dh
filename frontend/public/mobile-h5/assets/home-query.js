/**
 * 首页「查询」跳转结果页（兼容 uni-app hash 路由）
 */
(function () {
  function getPhone() {
    var selectors = [
      '.search-input .uni-input-input',
      '.search-input input',
      '.search-section input',
      'input[type="number"]',
      'input'
    ];
    for (var i = 0; i < selectors.length; i++) {
      var el = document.querySelector(selectors[i]);
      if (el && String(el.value || '').trim()) {
        return String(el.value).trim();
      }
    }
    return '';
  }

  function toast(msg) {
    if (window.AppBottomNav && typeof window.AppBottomNav.notify === 'function') {
      window.AppBottomNav.notify(msg);
      return;
    }
    if (typeof uni !== 'undefined' && uni.showToast) {
      uni.showToast({ title: msg, icon: 'none' });
      return;
    }
    var toast = document.getElementById('home-inline-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'home-inline-toast';
      toast.style.cssText =
        'position:fixed;left:50%;bottom:96px;transform:translateX(-50%);max-width:82vw;padding:10px 14px;border-radius:10px;background:rgba(28,28,30,.92);color:#fff;font-size:13px;line-height:1.4;text-align:center;z-index:2300;';
      document.body.appendChild(toast);
    }
    toast.textContent = String(msg || '');
    clearTimeout(toast._timer);
    toast.hidden = false;
    toast._timer = setTimeout(function () {
      toast.hidden = true;
    }, 1800);
  }
  function resolveHref(href) {
    if (window.MobileRuntimeConfig && typeof window.MobileRuntimeConfig.resolveHref === 'function') {
      return window.MobileRuntimeConfig.resolveHref(href);
    }
    return href;
  }

  function goResult(phone) {
    var url = resolveHref('/result/?phone=' + encodeURIComponent(phone));
    window.location.assign(url);
  }

  function onQueryClick(e) {
    var btn = e.target.closest('.search-btn');
    if (!btn) return;

    var phone = getPhone();
    if (!phone) {
      e.preventDefault();
      e.stopPropagation();
      toast('请输入手机号码');
      return;
    }
    if (!/^\d{7,15}$/.test(phone)) {
      e.preventDefault();
      e.stopPropagation();
      toast('请输入正确的号码');
      return;
    }

    e.preventDefault();
    e.stopPropagation();
    goResult(phone);
  }

  document.addEventListener('click', onQueryClick, true);
  document.addEventListener('touchend', onQueryClick, true);
})();
