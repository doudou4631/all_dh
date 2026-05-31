(function () {
  var BASE_PATH = '/free-query-ui';
  var WECHAT_QR = BASE_PATH + '/assets/icons/customer-wechat.png';
  var SERVICE_PHONE = '13027616171';

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
    { id: 'home', label: '首页', href: BASE_PATH + '/' },
    { id: 'free-query', label: '免费查询', href: BASE_PATH + '/?tab=query' },
    { id: 'batch', label: '批量查询', href: BASE_PATH + '/batch/' },
    { id: 'contact', label: '联系客服', action: 'contact' },
    { id: 'profile', label: '个人中心', href: BASE_PATH + '/profile/' }
  ];

  function normalizePath(path) {
    if (path.indexOf(BASE_PATH) === 0) {
      var sliced = path.slice(BASE_PATH.length);
      return sliced || '/';
    }
    return path;
  }

  function getActiveId() {
    var path = normalizePath(location.pathname.replace(/\/+$/, '') || '/');
    if (path === '/batch' || path.indexOf('/batch/') === 0) return 'batch';
    if (path === '/profile' || path.indexOf('/profile/') === 0) return 'profile';
    if (path.indexOf('/result') === 0) return 'free-query';
    if (location.search.indexOf('tab=query') >= 0) return 'free-query';
    if (location.hash.indexOf('search') >= 0) return 'free-query';
    return 'home';
  }

  function rootPrefix() {
    return '/';
  }

  function resolveHref(href) {
    return href;
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
          var normalizedPath = normalizePath(location.pathname.replace(/\/+$/, '') || '/');
          var onHome =
            (normalizedPath === '/' || normalizedPath === '/index.html') &&
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
    closeWechatModal: closeWechatModal
  };
})();
