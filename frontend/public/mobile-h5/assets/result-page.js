(function () {
  var SERVICE_PHONE = '13027616171';
  var ICON_BASE = '/assets/icons/';

  var PLATFORM_ICONS = {
    泰迪熊: ICON_BASE + 'teddy.png',
    腾讯: ICON_BASE + 'tencent.png',
    '360': ICON_BASE + '360.png',
    '360手机卫士': ICON_BASE + '360.png',
    百度: ICON_BASE + 'baidu.ico',
    搜狗: ICON_BASE + 'sogou.ico',
    移动高频: ICON_BASE + 'mobile.png',
    联通: ICON_BASE + 'unicom.svg',
    联通管家: ICON_BASE + 'unicom.svg',
    电话邦: ICON_BASE + 'dianhuabang.ico',
    小米手机: '/assets/icons/xiaomi.png?v=2'
  };

  function getQueryPhone() {
    var params = new URLSearchParams(window.location.search);
    return (params.get('phone') || '').trim();
  }
  function getRuntimeConfig() {
    if (window.MobileRuntimeConfig && typeof window.MobileRuntimeConfig.getConfig === 'function') {
      return window.MobileRuntimeConfig.getConfig() || {};
    }
    return {};
  }

  function getServicePhone() {
    var cfg = getRuntimeConfig();
    return String(cfg.servicePhone || SERVICE_PHONE);
  }

  function getWechatQr() {
    var cfg = getRuntimeConfig();
    return String(cfg.wechatQrUrl || '/assets/icons/customer-wechat.png');
  }

  function getResultBackUrl() {
    var cfg = getRuntimeConfig();
    return String(cfg.resultBackUrl || '/');
  }

  function resolveHref(href) {
    if (window.MobileRuntimeConfig && typeof window.MobileRuntimeConfig.resolveHref === 'function') {
      return window.MobileRuntimeConfig.resolveHref(href);
    }
    return href;
  }

  function applyRuntimeUiConfig() {
    var phone = getServicePhone();
    var btnCall = document.getElementById('btn-call');
    if (btnCall) btnCall.href = 'tel:' + phone;

    var contactLink = document.querySelector('.result-contact a');
    if (contactLink) {
      contactLink.href = 'tel:' + phone;
      contactLink.textContent = phone;
    }

    var qrPath = getWechatQr();
    var footerQr = document.getElementById('wechat-qr');
    if (footerQr) footerQr.src = qrPath;

    var modalQr = document.getElementById('wechat-modal-qr');
    if (modalQr) {
      modalQr.dataset.src = qrPath;
      if (!modalQr.src || modalQr.src === window.location.href) {
        modalQr.src = qrPath;
      }
    }
  }

  function isMarked(item) {
    if (!item) return false;
    if (item.process === 1) return true;
    var s = item.status || '';
    if (s === '无标记' || s === '未标记' || s === '查询失败' || s === '未开放' || s === '-') {
      return false;
    }
    return s !== '无标记';
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function getPlatformIcon(name) {
    if (PLATFORM_ICONS[name]) return PLATFORM_ICONS[name];
    if (name.indexOf('360') >= 0) return PLATFORM_ICONS['360'];
    if (name.indexOf('泰迪') >= 0) return PLATFORM_ICONS['泰迪熊'];
    if (name.indexOf('腾讯') >= 0) return PLATFORM_ICONS['腾讯'];
    if (name.indexOf('百度') >= 0) return PLATFORM_ICONS['百度'];
    if (name.indexOf('搜狗') >= 0) return PLATFORM_ICONS['搜狗'];
    if (name.indexOf('移动') >= 0) return PLATFORM_ICONS['移动高频'];
    if (name.indexOf('联通') >= 0) return PLATFORM_ICONS['联通管家'];
    if (name.indexOf('电话邦') >= 0) return PLATFORM_ICONS['电话邦'];
    if (name.indexOf('小米') >= 0) return PLATFORM_ICONS['小米手机'];
    return ICON_BASE + '360.png';
  }

  function renderCard(item) {
    var name = item.platform || '未知平台';
    var title = name.indexOf('标记') >= 0 ? '[' + name + ']' : '[' + name + ' 标记]';
    var msg = item.status && item.status !== '有标记' ? item.status : '普通标记';
    var icon = getPlatformIcon(name);
    var iconClass =
      'result-card-icon' + (name.indexOf('小米') >= 0 ? ' result-card-icon--xiaomi' : '');
    return (
      '<article class="result-card">' +
      '<div class="result-card-head">' +
      '<img class="' +
      iconClass +
      '" src="' +
      escapeHtml(icon) +
      '" alt="' +
      escapeHtml(name) +
      '" loading="lazy" decoding="async" />' +
      '<h3 class="result-card-title">' +
      escapeHtml(title) +
      '</h3></div>' +
      '<div class="result-card-row">' +
      '<span class="result-card-label">标记名称：</span>' +
      '<span class="result-card-value">' +
      escapeHtml(msg) +
      '</span></div></article>'
    );
  }

  function showError(msg) {
    document.getElementById('loading').hidden = true;
    document.getElementById('cards').hidden = true;
    document.getElementById('empty').hidden = true;
    var el = document.getElementById('error');
    el.textContent = msg;
    el.hidden = false;
  }

  function runQuery(phone) {
    applyRuntimeUiConfig();
    document.getElementById('hero-phone').textContent = phone;

    if (!phone) {
      showError('未提供手机号码');
      return;
    }

    if (!window.BiaojiApiBridge) {
      showError('接口未加载，请刷新页面');
      return;
    }

    window.BiaojiApiBridge.queryPhone(phone, function (err, data) {
      if (!err && window.QueryStats) window.QueryStats.recordQueryForCurrentUser(1);
      document.getElementById('loading').hidden = true;

      if (err) {
        showError(err);
        return;
      }

      var all = data.results || [];
      var marked = all.filter(isMarked);
      var markedPlatforms = marked
        .map(function (item) {
          return String((item && item.platform) || '').trim();
        })
        .filter(function (name, index, arr) {
          return !!name && arr.indexOf(name) === index;
        });
      if (window.QueryRecords && typeof window.QueryRecords.addSingleRecord === 'function') {
        window.QueryRecords.addSingleRecord(phone, marked.length, markedPlatforms);
      }

      document.getElementById('hero-count').textContent = String(marked.length);

      if (marked.length === 0) {
        document.getElementById('empty').hidden = false;
        return;
      }

      var html = marked.map(renderCard).join('');
      var cards = document.getElementById('cards');
      cards.innerHTML = html;
      cards.hidden = false;
    });
  }

  document.getElementById('btn-back').addEventListener('click', function () {
    window.location.assign(resolveHref(getResultBackUrl()));
  });

  document.getElementById('btn-wechat').addEventListener('click', function () {
    openWechatModal();
  });

  var wechatModal = document.getElementById('wechat-modal');
  var wechatModalMask = document.getElementById('wechat-modal-mask');

  function openWechatModal() {
    var qr = document.getElementById('wechat-modal-qr');
    if (qr && !qr.src && qr.dataset.src) qr.src = qr.dataset.src;
    wechatModal.hidden = false;
    document.body.classList.add('wechat-modal-open');
  }

  function closeWechatModal() {
    wechatModal.hidden = true;
    document.body.classList.remove('wechat-modal-open');
  }

  wechatModalMask.addEventListener('click', closeWechatModal);
  wechatModal.addEventListener('click', function (e) {
    if (e.target === wechatModal) closeWechatModal();
  });

  if (window.MobileRuntimeConfig && typeof window.MobileRuntimeConfig.ready === 'function') {
    window.MobileRuntimeConfig.ready(function () {
      applyRuntimeUiConfig();
    });
  }

  runQuery(getQueryPhone());
})();
