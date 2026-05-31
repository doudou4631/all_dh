/**
 * 首页「查询」跳转结果页（兼容 uni-app hash 路由）
 */
(function () {
  var BASE_PATH = '/free-query-ui';
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
    if (typeof uni !== 'undefined' && uni.showToast) {
      uni.showToast({ title: msg, icon: 'none' });
      return;
    }
    alert(msg);
  }

  function goResult(phone) {
    var url =
      window.location.origin +
      BASE_PATH +
      '/result/?phone=' +
      encodeURIComponent(phone);
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
