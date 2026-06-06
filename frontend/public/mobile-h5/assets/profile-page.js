(function () {
  var STORAGE_KEY = 'profile_user_account';
  var PASSWORD_KEY = 'profile_password_map';
  var QUERY_RECORDS_KEY = 'profile_query_records';
  var RECHARGE_RECORDS_KEY = 'profile_recharge_records';
  var POINTS_KEY = 'profile_user_points';
  var TOKEN_KEY = 'profile_user_token';
  var DEFAULT_POINTS = 100;
  var profileToastTimer = null;

  var DEMO_ACCOUNTS = {
    admin: '123456',
    test: 'test123'
  };

  function readJson(key, fallback) {
    try {
      var raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (e) {
      return fallback;
    }
  }

  function writeJson(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {}
  }

  function getAccount() {
    try {
      return localStorage.getItem(STORAGE_KEY) || '';
    } catch (e) {
      return '';
    }
  }
  function getToken() {
    try {
      return (localStorage.getItem(TOKEN_KEY) || '').trim();
    } catch (e) {
      return '';
    }
  }

  function setAccount(account) {
    try {
      if (account) localStorage.setItem(STORAGE_KEY, account);
      else localStorage.removeItem(STORAGE_KEY);
    } catch (e) {}
  }
  function setToken(token) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token);
      else localStorage.removeItem(TOKEN_KEY);
    } catch (e) {}
  }

  function clearLogin() {
    setAccount('');
    setToken('');
  }

  function getPasswordMap() {
    return readJson(PASSWORD_KEY, {});
  }

  function getAccountPassword(account) {
    var map = getPasswordMap();
    if (map[account]) return map[account];
    if (Object.prototype.hasOwnProperty.call(DEMO_ACCOUNTS, account)) {
      return DEMO_ACCOUNTS[account];
    }
    return '';
  }

  function setAccountPassword(account, password) {
    var map = getPasswordMap();
    map[account] = password;
    writeJson(PASSWORD_KEY, map);
  }

  function maskAccount(account) {
    if (!account) return account;
    if (account.length <= 2) return account;
    if (account.length <= 4) return account.slice(0, 1) + '***';
    return account.slice(0, 2) + '***' + account.slice(-2);
  }

  function formatPoints(value) {
    var num = Number(value);
    if (isNaN(num)) num = 0;
    return String(Math.round(num));
  }

  function getPoints() {
    var account = getAccount();
    if (!account || !getToken()) return 0;
    var map = readJson(POINTS_KEY, {});
    if (Object.prototype.hasOwnProperty.call(map, account)) {
      return map[account];
    }
    return DEFAULT_POINTS;
  }

  function setPoints(value) {
    var account = getAccount();
    if (!account) return;
    var map = readJson(POINTS_KEY, {});
    map[account] = value;
    writeJson(POINTS_KEY, map);
  }

  function getTodayQueryCount() {
    if (window.QueryStats) {
      return window.QueryStats.getTodayQueryCount(getAccount());
    }
    return 0;
  }

  function renderUserHeader() {
    var titleEl = document.getElementById('profile-title');
    var descEl = document.getElementById('profile-desc');
    var pointsValueEl = document.getElementById('profile-points-value');
    var todayQueryEl = document.getElementById('profile-today-query-value');
    var rechargeTextEl = document.getElementById('profile-recharge-text');
    var rechargeLabelEl = document.getElementById('profile-recharge-label');
    if (!titleEl || !descEl) return;

    var account = getAccount();
    var loggedIn = !!(account && getToken());
    if (loggedIn) {
      titleEl.textContent = maskAccount(account);
      descEl.textContent = '欢迎使用批量查询服务';
      if (pointsValueEl) {
        pointsValueEl.textContent = formatPoints(getPoints());
        pointsValueEl.classList.remove('is-muted');
      }
      if (todayQueryEl) {
        todayQueryEl.textContent = String(getTodayQueryCount());
        todayQueryEl.classList.remove('is-muted');
      }
      if (rechargeTextEl) {
        rechargeTextEl.textContent = '充值';
        rechargeTextEl.classList.add('profile-stat-action');
      }
      if (rechargeLabelEl) rechargeLabelEl.textContent = '联系客服';
    } else {
      titleEl.textContent = '登录/注册';
      descEl.textContent = '取消号码标记，提升号码接听效率~';
      if (pointsValueEl) {
        pointsValueEl.textContent = '--';
        pointsValueEl.classList.add('is-muted');
      }
      if (todayQueryEl) {
        todayQueryEl.textContent = '--';
        todayQueryEl.classList.add('is-muted');
      }
      if (rechargeTextEl) {
        rechargeTextEl.textContent = '登录';
        rechargeTextEl.classList.add('profile-stat-action');
      }
      if (rechargeLabelEl) rechargeLabelEl.textContent = '立即登录';
    }
  }

  function isLoggedIn() {
    return !!(getAccount() && getToken());
  }

  function getAppBase() {
    if (window.MobileRuntimeConfig && typeof window.MobileRuntimeConfig.getAppBase === 'function') {
      return window.MobileRuntimeConfig.getAppBase();
    }
    var path = window.location.pathname || '/';
    if (path === '/mobile-h5' || path.indexOf('/mobile-h5/') === 0) {
      return '/mobile-h5';
    }
    return '';
  }

  function resolveHref(href) {
    if (window.MobileRuntimeConfig && typeof window.MobileRuntimeConfig.resolveHref === 'function') {
      return window.MobileRuntimeConfig.resolveHref(href);
    }
    if (!href || href.charAt(0) !== '/') return href;
    var base = getAppBase();
    if (!base) return href;
    if (href === '/') return base + '/';
    if (href.indexOf(base + '/') === 0) return href;
    return base + href;
  }

  function isAbsoluteUrl(url) {
    var text = String(url || '').toLowerCase();
    return text.indexOf('http://') === 0 || text.indexOf('https://') === 0;
  }

  function rewriteStaticLinks() {
    var mapping = {
      'about.html': '/profile/about.html',
      'agreement.html': '/profile/agreement.html',
      'privacy.html': '/profile/privacy.html',
      'index.html': '/profile/?v=3'
    };
    var links = document.querySelectorAll('a[href]');
    links.forEach(function (link) {
      var href = String(link.getAttribute('href') || '').trim();
      if (!href || !Object.prototype.hasOwnProperty.call(mapping, href)) return;
      link.setAttribute('href', resolveHref(mapping[href]));
    });
  }

  function getPostLoginRedirectPath() {
    try {
      var params = new URLSearchParams(window.location.search || '');
      var raw = (params.get('redirect') || '').trim();
      if (!raw) return '';
      if (raw.charAt(0) !== '/') return '';
      if (raw.indexOf('//') === 0) return '';
      if (raw.indexOf('/profile') === 0 || raw.indexOf('/mobile-h5/profile') === 0) return '';
      return raw;
    } catch (e) {
      return '';
    }
  }

  function redirectAfterLoginIfNeeded() {
    var path = getPostLoginRedirectPath();
    if (!path) return false;
    window.location.href = resolveHref(path);
    return true;
  }

  function notify(message) {
    var text = String(message || '').trim();
    if (!text) return;
    if (window.AppBottomNav && typeof window.AppBottomNav.notify === 'function') {
      window.AppBottomNav.notify(text);
      return;
    }
    var toast = document.getElementById('profile-inline-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'profile-inline-toast';
      toast.style.cssText =
        'position:fixed;left:50%;bottom:96px;transform:translateX(-50%);max-width:82vw;padding:10px 14px;border-radius:10px;background:rgba(28,28,30,.92);color:#fff;font-size:13px;line-height:1.4;text-align:center;z-index:2300;';
      document.body.appendChild(toast);
    }
    toast.textContent = text;
    toast.hidden = false;
    if (profileToastTimer) clearTimeout(profileToastTimer);
    profileToastTimer = setTimeout(function () {
      toast.hidden = true;
    }, 1800);
  }

  function confirmAction(message, onConfirm, onCancel) {
    if (window.AppBottomNav && typeof window.AppBottomNav.confirmDialog === 'function') {
      window.AppBottomNav.confirmDialog(message, onConfirm, onCancel);
      return;
    }
    notify(message || '请确认操作');
    if (typeof onCancel === 'function') onCancel();
  }

  function requireLogin(tip) {
    if (isLoggedIn()) return true;
    notify(tip || '请先登录');
    openLoginModal();
    return false;
  }

  function openLoginModal() {
    var modal = document.getElementById('profile-login-modal');
    if (!modal) return;
    modal.hidden = false;
    document.body.classList.add('profile-modal-open');
    var accountInput = document.getElementById('profile-account');
    if (accountInput) accountInput.focus();
  }

  function closeLoginModal() {
    var modal = document.getElementById('profile-login-modal');
    if (!modal) return;
    modal.hidden = true;
    document.body.classList.remove('profile-modal-open');
  }

  function openPasswordModal() {
    var modal = document.getElementById('profile-password-modal');
    if (!modal) return;
    modal.hidden = false;
    document.body.classList.add('profile-modal-open');
  }

  function closePasswordModal() {
    var modal = document.getElementById('profile-password-modal');
    if (!modal) return;
    modal.hidden = true;
    document.body.classList.remove('profile-modal-open');
    var ids = ['profile-old-password', 'profile-new-password', 'profile-confirm-password'];
    ids.forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.value = '';
    });
  }
  function validateLoginForm(account, password) {
    if (!account || !password) {
      return '请输入账号和密码';
    }
    if (account.length < 2) {
      return '账号至少 2 个字符';
    }
    if (password.length < 5) {
      return '密码至少 5 位';
    }
    return null;
  }

  function requestRemoteLogin(account, password, callback) {
    if (!window.BiaojiApiBridge || typeof window.BiaojiApiBridge.requests !== 'function') {
      callback('登录服务未加载，请刷新页面后重试');
      return;
    }
    var payload = {
      account: account,
      password: password
    };
    if (/^\d{11}$/.test(account)) {
      payload.phone = account;
    }
    var done = false;
    function finish(err, data) {
      if (done) return;
      done = true;
      callback(err, data);
    }
    window.BiaojiApiBridge.requests({
      url: 'server/freeQuery/login',
      data: payload,
      success: function (resp) {
        var code = Number(resp && resp.code);
        if (code === 0 || code === 200) {
          finish(null, (resp && resp.data) || {});
          return;
        }
        finish((resp && resp.msg) || '登录失败');
      },
      fail: function (err) {
        finish((err && err.msg) || '网络错误，请重试');
      }
    });
  }

  function applyRemoteLoginResult(account, password, data) {
    var token = String((data && data.token) || '').trim();
    if (!token) {
      return false;
    }
    var loginAccount = String(
      (data && (data.account || data.userName || data.phone || data.nickName)) || account || ''
    ).trim();
    if (!loginAccount) loginAccount = account;
    setAccount(loginAccount);
    setToken(token);

    var points = Number(data && data.points);
    if (!isNaN(points)) {
      setPoints(Math.max(0, points));
    }

    if (String(password || '').trim()) {
      setAccountPassword(loginAccount, password);
    }
    return true;
  }

  function getQueryRecords() {
    if (window.QueryRecords && typeof window.QueryRecords.getRecords === 'function') {
      return window.QueryRecords.getRecords(getAccount());
    }
    var list = readJson(QUERY_RECORDS_KEY, []);
    return Array.isArray(list) ? list : [];
  }

  function getRechargeRecords() {
    var list = readJson(RECHARGE_RECORDS_KEY, null);
    if (list && list.length) return list;
    return [
      { title: '批量查询套餐', amount: '¥79.00', remark: '100 次查询', time: '2026-05-28 16:30' },
      { title: '体验包', amount: '¥19.90', remark: '20 次查询', time: '2026-05-20 09:12' }
    ];
  }

  function bindLoginModal() {
    var modal = document.getElementById('profile-login-modal');
    if (!modal) return;

    document.getElementById('profile-user-btn').addEventListener('click', function () {
      if (isLoggedIn()) {
        confirmAction('是否退出登录？', function () {
          clearLogin();
          if (window.UserAuth && typeof window.UserAuth.logout === 'function') {
            window.UserAuth.logout();
          }
          renderUserHeader();
          var homeHref = resolveHref('/');
          try {
            window.location.replace(homeHref);
          } catch (e) {
            window.location.href = homeHref;
          }
        });
        return;
      }
      openLoginModal();
    });

    document.getElementById('profile-login-mask').addEventListener('click', closeLoginModal);
    document.getElementById('profile-login-close').addEventListener('click', closeLoginModal);

    var accountInput = document.getElementById('profile-account');
    var passwordInput = document.getElementById('profile-password');
    var submitBtn = document.getElementById('profile-login-submit');

    submitBtn.addEventListener('click', function () {
      if (submitBtn.disabled) return;
      var account = (accountInput.value || '').trim();
      var password = passwordInput.value || '';
      var err = validateLoginForm(account, password);
      if (err) {
        notify(err);
        return;
      }
      var originalText = submitBtn.textContent;
      submitBtn.disabled = true;
      submitBtn.textContent = '登录中...';
      requestRemoteLogin(account, password, function (loginErr, loginData) {
        submitBtn.disabled = false;
        submitBtn.textContent = originalText;
        if (loginErr) {
          notify(loginErr);
          return;
        }
        if (!applyRemoteLoginResult(account, password, loginData)) {
          notify('登录失败，请稍后重试');
          return;
        }
        renderUserHeader();
        closeLoginModal();
        accountInput.value = '';
        passwordInput.value = '';
        redirectAfterLoginIfNeeded();
      });
    });

    passwordInput.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') {
        document.getElementById('profile-login-submit').click();
      }
    });
  }

  function bindPasswordModal() {
    var modal = document.getElementById('profile-password-modal');
    if (!modal) return;

    document.getElementById('profile-password-mask').addEventListener('click', closePasswordModal);
    document.getElementById('profile-password-close').addEventListener('click', closePasswordModal);

    document.getElementById('profile-password-submit').addEventListener('click', function () {
      var account = getAccount();
      if (!account) {
        notify('请先登录');
        closePasswordModal();
        openLoginModal();
        return;
      }

      var oldPwd = document.getElementById('profile-old-password').value || '';
      var newPwd = document.getElementById('profile-new-password').value || '';
      var confirmPwd = document.getElementById('profile-confirm-password').value || '';

      if (!oldPwd || !newPwd || !confirmPwd) {
        notify('请填写完整密码信息');
        return;
      }
      if (getAccountPassword(account) !== oldPwd) {
        notify('原密码错误');
        return;
      }
      if (newPwd.length < 6) {
        notify('新密码至少 6 位');
        return;
      }
      if (newPwd !== confirmPwd) {
        notify('两次输入的新密码不一致');
        return;
      }

      setAccountPassword(account, newPwd);
      closePasswordModal();
      notify('密码修改成功，请使用新密码登录');
    });
  }

  function bindStatActions() {
    var pointsBtn = document.getElementById('stat-points');
    if (pointsBtn) {
      pointsBtn.addEventListener('click', function () {
        if (!requireLogin()) return;
        window.location.href = resolveHref('/profile/recharge-records.html');
      });
    }

    var todayQueryBtn = document.getElementById('stat-today-query');
    if (todayQueryBtn) {
      todayQueryBtn.addEventListener('click', function () {
        if (!requireLogin()) return;
        window.location.href = resolveHref('/profile/query-records.html?v=3');
      });
    }

    var rechargeStatBtn = document.getElementById('stat-recharge');
    if (rechargeStatBtn) {
      rechargeStatBtn.addEventListener('click', function () {
        if (!isLoggedIn()) {
          openLoginModal();
          return;
        }
        if (window.AppBottomNav) window.AppBottomNav.openWechatModal();
      });
    }
  }

  function bindMenuActions() {
    var batchBtn = document.getElementById('btn-batch');
    if (batchBtn) {
      batchBtn.addEventListener('click', function () {
        if (!requireLogin()) return;
        window.location.href = resolveHref('/batch/');
      });
    }

    var queryBtn = document.getElementById('btn-query-records');
    if (queryBtn) {
      queryBtn.addEventListener('click', function () {
        if (!requireLogin()) return;
        window.location.href = resolveHref('/profile/query-records.html?v=3');
      });
    }

    var rechargeBtn = document.getElementById('btn-recharge-records');
    if (rechargeBtn) {
      rechargeBtn.addEventListener('click', function () {
        if (!requireLogin()) return;
        window.location.href = resolveHref('/profile/recharge-records.html');
      });
    }

    var pwdBtn = document.getElementById('btn-change-password');
    if (pwdBtn) {
      pwdBtn.addEventListener('click', function () {
        if (!requireLogin()) return;
        openPasswordModal();
      });
    }

    var serviceBtn = document.getElementById('btn-service');
    if (serviceBtn) {
      serviceBtn.addEventListener('click', function () {
        if (window.AppBottomNav) window.AppBottomNav.openWechatModal();
      });
    }

    var inviteBtn = document.getElementById('btn-invite');
    if (inviteBtn) {
      inviteBtn.addEventListener('click', function () {
        var text = '取消号码标记，提升号码接听效率~';
        var sharePath = resolveHref('/');
        var shareUrl = isAbsoluteUrl(sharePath) ? sharePath : (location.origin + sharePath);
        if (navigator.share) {
          navigator.share({ title: '取消号码标记', text: text, url: shareUrl }).catch(function () {});
          return;
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard
            .writeText(shareUrl)
            .then(function () {
              notify('邀请链接已复制，快去分享吧');
            })
            .catch(function () {
              notify('请复制链接分享给好友：' + shareUrl);
            });
          return;
        }
        notify('请复制链接分享给好友：' + shareUrl);
      });
    }
  }

  window.ProfilePage = {
    getAccount: getAccount,
    getToken: getToken,
    isLoggedIn: isLoggedIn,
    requireLogin: requireLogin,
    openLoginModal: openLoginModal,
    renderUserHeader: renderUserHeader,
    getQueryRecords: getQueryRecords,
    getRechargeRecords: getRechargeRecords,
    getPoints: getPoints,
    setPoints: setPoints,
    getTodayQueryCount: getTodayQueryCount,
    clearLogin: clearLogin
  };
  rewriteStaticLinks();

  renderUserHeader();
  if (isLoggedIn() && redirectAfterLoginIfNeeded()) return;
  bindLoginModal();
  bindPasswordModal();
  bindStatActions();
  bindMenuActions();
})();
