(function () {
  var WECHAT_QR = '/assets/icons/customer-wechat.png';
  var SERVICE_PHONE = '13027616171';
  var PROFILE_ACCOUNT_KEY = 'profile_user_account';

  var ICONS = {
    home:
      '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path class="nav-icon-stroke" d="M4 10.5L12 4l8 6.5V19a1.5 1.5 0 0 1-1.5 1.5H15v-6H9v6H5.5A1.5 1.5 0 0 1 4 19v-8.5Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
    search:
      '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle class="nav-icon-stroke" cx="11" cy="11" r="6.5" stroke="currentColor" stroke-width="1.8"/><path class="nav-icon-stroke" d="M16 16l4.5 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
    batch:
      '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><rect class="nav-icon-stroke" x="4" y="5" width="16" height="14" rx="1.5" stroke="currentColor" stroke-width="1.8"/><path class="nav-icon-stroke" d="M8 9h8M8 12h8M8 15h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
    service:
      '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path class="nav-icon-stroke" d="M5 10.5V9a7 7 0 0 1 14 0v1.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path class="nav-icon-stroke" d="M6 10.5h-.5A2.5 2.5 0 0 0 3 13v1.5A2.5 2.5 0 0 0 5.5 17H6m12-6.5h.5A2.5 2.5 0 0 1 21 13v1.5a2.5 2.5 0 0 1-2.5 2.5H18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><rect class="nav-icon-stroke" x="5" y="10.5" width="14" height="8" rx="2" stroke="currentColor" stroke-width="1.8"/></svg>',
    profile:
      '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle class="nav-icon-stroke" cx="12" cy="8" r="3.5" stroke="currentColor" stroke-width="1.8"/><path class="nav-icon-stroke" d="M5.5 19.5c1.2-3 3.4-4.5 6.5-4.5s5.3 1.5 6.5 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>'
  };

  var NAV_ITEMS = [
    { id: 'home', label: '首页', href: '/' },
    { id: 'free-query', label: '免费查询', href: '/?tab=query' },
    { id: 'batch', label: '批量查询', href: '/batch/' },
    { id: 'contact', label: '联系客服', action: 'contact' },
    { id: 'profile', label: '个人中心', href: '/profile/' }
  ];
  var toastTimer = null;

  function getAppBase() {
    var path = location.pathname || '/';
    if (path === '/mobile-h5' || path.indexOf('/mobile-h5/') === 0) {
      return '/mobile-h5';
    }
    return '';
  }

  function stripAppBase(path) {
    var base = getAppBase();
    if (base && path.indexOf(base) === 0) {
      var stripped = path.slice(base.length);
      return stripped || '/';
    }
    return path;
  }

  function getProfileAccount() {
    try {
      return (localStorage.getItem(PROFILE_ACCOUNT_KEY) || '').trim();
    } catch (e) {
      return '';
    }
  }

  function isBatchLoginReady() {
    return !!getProfileAccount();
  }

  function getBatchLoginRedirectHref() {
    return resolveHref('/profile/');
  }

  function getActiveId() {
    var path = stripAppBase(location.pathname.replace(/\/+$/, '') || '/');
    if (path === '/batch' || path.indexOf('/batch/') === 0) return 'batch';
    if (path === '/profile' || path.indexOf('/profile/') === 0) return 'profile';
    if (path.indexOf('/result') === 0) return 'free-query';
    if (location.search.indexOf('tab=query') >= 0) return 'free-query';
    if (location.hash.indexOf('search') >= 0) return 'free-query';
    return 'home';
  }

  function resolveHref(href) {
    if (!href || href.charAt(0) !== '/') return href;
    var base = getAppBase();
    if (!base) return href;
    if (href === '/') return base + '/';
    if (href.indexOf(base + '/') === 0) return href;
    return base + href;
  }

  function scrollToSearch() {
    var el =
      document.querySelector('.search-section[data-v-41c99d40]') ||
      document.querySelector('.search-section') ||
      document.querySelector('.search-box');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      var input =
        document.querySelector('.search-input .uni-input-input') ||
        document.querySelector('.search-input input');
      if (input) setTimeout(function () { input.focus(); }, 350);
    }
  }

  function openWechatModal() {
    var modal = document.getElementById('bn-wechat-modal');
    if (modal) {
      modal.hidden = false;
      document.body.classList.add('bn-modal-open');
    }
  }

  function closeWechatModal() {
    var modal = document.getElementById('bn-wechat-modal');
    if (modal) {
      modal.hidden = true;
      document.body.classList.remove('bn-modal-open');
    }
  }

  function ensureUiStyle() {
    if (document.getElementById('bn-ui-style')) return;
    var style = document.createElement('style');
    style.id = 'bn-ui-style';
    style.textContent =
      '.bn-toast{position:fixed;left:50%;bottom:96px;transform:translate(-50%,8px);background:rgba(28,28,30,.92);color:#fff;padding:10px 14px;border-radius:10px;font-size:13px;line-height:1.4;max-width:82vw;text-align:center;box-shadow:0 8px 24px rgba(0,0,0,.22);z-index:2300;opacity:0;pointer-events:none;transition:opacity .2s ease,transform .2s ease;}' +
      '.bn-toast.is-show{opacity:1;transform:translate(-50%,0);}' +
      '.bn-confirm-open{overflow:hidden;}' +
      '.bn-confirm-modal{position:fixed;inset:0;z-index:2400;display:flex;align-items:center;justify-content:center;padding:20px;box-sizing:border-box;}' +
      '.bn-confirm-mask{position:absolute;inset:0;background:rgba(0,0,0,.35);}' +
      '.bn-confirm-panel{position:relative;width:min(320px,90vw);background:#fff;border-radius:14px;padding:18px 16px 14px;box-shadow:0 18px 48px rgba(0,0,0,.24);}' +
      '.bn-confirm-message{margin:0;color:#1f2d3d;font-size:15px;line-height:1.6;text-align:center;word-break:break-word;}' +
      '.bn-confirm-actions{margin-top:14px;display:flex;gap:10px;}' +
      '.bn-confirm-btn{flex:1;height:38px;border-radius:10px;border:1px solid #d9e0ee;background:#fff;color:#334155;font-size:14px;cursor:pointer;}' +
      '.bn-confirm-btn--ok{border-color:#1f6bff;background:#1f6bff;color:#fff;}';
    document.head.appendChild(style);
  }

  function notify(message) {
    var text = String(message || '').trim();
    if (!text) return;
    ensureUiStyle();
    var toast = document.getElementById('bn-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'bn-toast';
      toast.className = 'bn-toast';
      document.body.appendChild(toast);
    }
    toast.textContent = text;
    toast.classList.add('is-show');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      toast.classList.remove('is-show');
    }, 1800);
  }

  function ensureConfirmModal() {
    var modal = document.getElementById('bn-confirm-modal');
    if (modal) return modal;
    ensureUiStyle();
    modal = document.createElement('div');
    modal.id = 'bn-confirm-modal';
    modal.className = 'bn-confirm-modal';
    modal.hidden = true;
    modal.innerHTML =
      '<div class="bn-confirm-mask" id="bn-confirm-mask"></div>' +
      '<div class="bn-confirm-panel" role="dialog" aria-modal="true" aria-label="提示">' +
      '<p class="bn-confirm-message" id="bn-confirm-message"></p>' +
      '<div class="bn-confirm-actions">' +
      '<button type="button" class="bn-confirm-btn bn-confirm-btn--cancel" id="bn-confirm-cancel">取消</button>' +
      '<button type="button" class="bn-confirm-btn bn-confirm-btn--ok" id="bn-confirm-ok">确定</button>' +
      '</div></div>';
    document.body.appendChild(modal);
    return modal;
  }

  function confirmDialog(message, onConfirm, onCancel) {
    var modal = ensureConfirmModal();
    var msgEl = document.getElementById('bn-confirm-message');
    var mask = document.getElementById('bn-confirm-mask');
    var cancelBtn = document.getElementById('bn-confirm-cancel');
    var okBtn = document.getElementById('bn-confirm-ok');

    if (msgEl) msgEl.textContent = String(message || '请确认操作');

    modal.hidden = false;
    document.body.classList.add('bn-confirm-open');

    function cleanup() {
      if (mask) mask.removeEventListener('click', onCancelClick);
      if (cancelBtn) cancelBtn.removeEventListener('click', onCancelClick);
      if (okBtn) okBtn.removeEventListener('click', onConfirmClick);
      modal.removeEventListener('click', onModalClick);
      document.removeEventListener('keydown', onKeydown);
    }

    function close(result) {
      modal.hidden = true;
      document.body.classList.remove('bn-confirm-open');
      cleanup();
      if (result) {
        if (typeof onConfirm === 'function') onConfirm();
      } else if (typeof onCancel === 'function') {
        onCancel();
      }
    }

    function onCancelClick(e) {
      if (e) e.preventDefault();
      close(false);
    }

    function onConfirmClick(e) {
      if (e) e.preventDefault();
      close(true);
    }

    function onModalClick(e) {
      if (e.target === modal) close(false);
    }

    function onKeydown(e) {
      if (e.key === 'Escape') close(false);
    }

    if (mask) mask.addEventListener('click', onCancelClick);
    if (cancelBtn) cancelBtn.addEventListener('click', onCancelClick);
    if (okBtn) okBtn.addEventListener('click', onConfirmClick);
    modal.addEventListener('click', onModalClick);
    document.addEventListener('keydown', onKeydown);
    if (okBtn) okBtn.focus();
  }

  function buildModal() {
    if (document.getElementById('bn-wechat-modal')) return;
    var wrap = document.createElement('div');
    wrap.id = 'bn-wechat-modal';
    wrap.className = 'bn-wechat-modal';
    wrap.hidden = true;
    wrap.innerHTML =
      '<div class="bn-wechat-modal-mask" id="bn-wechat-modal-mask"></div>' +
      '<div class="bn-wechat-modal-panel" role="dialog" aria-label="联系客服">' +
      '<img class="bn-wechat-modal-qr" src="' +
      WECHAT_QR +
      '" alt="客服微信二维码" />' +
      '<p class="bn-wechat-modal-tip">长按识别二维码添加客服微信</p>' +
      '<p class="bn-wechat-modal-tip" style="margin-top:8px">或拨打 <a href="tel:' +
      SERVICE_PHONE +
      '">' +
      SERVICE_PHONE +
      '</a></p>' +
      '</div>';
    document.body.appendChild(wrap);
    document.getElementById('bn-wechat-modal-mask').addEventListener('click', closeWechatModal);
    wrap.addEventListener('click', function (e) {
      if (e.target === wrap) closeWechatModal();
    });
  }

  function buildNav() {
    if (document.querySelector('.app-bottom-nav')) return;

    var active = getActiveId();
    var nav = document.createElement('nav');
    nav.className = 'app-bottom-nav';
    nav.setAttribute('aria-label', '底部导航');

    NAV_ITEMS.forEach(function (item) {
      var isActive = item.id === active;
      var iconKey =
        item.id === 'free-query' ? 'search' : item.id === 'contact' ? 'service' : item.id;
      var inner =
        '<span class="nav-icon">' +
        (ICONS[iconKey] || ICONS.home) +
        '</span><span class="nav-label">' +
        item.label +
        '</span>';

      if (item.action === 'contact') {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'app-bottom-nav-item' + (isActive ? ' is-active' : '');
        btn.innerHTML = inner;
        btn.addEventListener('click', openWechatModal);
        nav.appendChild(btn);
        return;
      }

      var link = document.createElement('a');
      link.className = 'app-bottom-nav-item' + (isActive ? ' is-active' : '');
      link.href = resolveHref(item.href);
      link.innerHTML = inner;

      if (item.id === 'free-query') {
        link.addEventListener('click', function (e) {
          var currentPath = stripAppBase(location.pathname.replace(/\/+$/, '') || '/');
          var onHome =
            (currentPath === '/' || currentPath === '/index.html') &&
            location.search.indexOf('tab=query') < 0;
          if (onHome) {
            e.preventDefault();
            scrollToSearch();
            nav.querySelectorAll('.app-bottom-nav-item').forEach(function (el) {
              el.classList.remove('is-active');
            });
            link.classList.add('is-active');
          }
        });
      }

      if (item.id === 'batch') {
        link.addEventListener('click', function (e) {
          if (isBatchLoginReady()) return;
          e.preventDefault();
          window.location.href = getBatchLoginRedirectHref();
        });
      }

      nav.appendChild(link);
    });

    document.body.appendChild(nav);
    document.body.classList.add('has-bottom-nav');
  }

  function initQueryTab() {
    if (location.search.indexOf('tab=query') < 0) return;
    var tryScroll = function () {
      scrollToSearch();
    };
    if (document.readyState === 'complete') {
      setTimeout(tryScroll, 500);
    } else {
      window.addEventListener('load', function () {
        setTimeout(tryScroll, 500);
      });
    }
  }

  buildModal();
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', buildNav);
  } else {
    buildNav();
  }
  initQueryTab();

  window.AppBottomNav = {
    openWechatModal: openWechatModal,
    closeWechatModal: closeWechatModal,
    notify: notify,
    confirmDialog: confirmDialog
  };
})();
