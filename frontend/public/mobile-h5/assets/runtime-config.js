(function () {
  var DEFAULT_API_BASE = 'https://biaoji.aleo1314.vip/prod-api/';
  var DEFAULT_PAGE_CODE = 'mobile-h5';
  var PAGE_CODE_PATTERN = /^[a-z0-9-]{2,32}$/;

  var state = {
    requestedPage: '',
    ready: false,
    loading: false,
    callbacks: [],
    config: {
      requestedPage: DEFAULT_PAGE_CODE,
      pageCode: DEFAULT_PAGE_CODE,
      pageName: '手机页H5',
      servicePhone: '13027616171',
      wechatQrUrl: '/mobile-h5/assets/icons/customer-wechat.png',
      navHomeUrl: '/',
      navQueryUrl: '/?tab=query',
      navBatchUrl: '/batch/',
      navProfileUrl: '/profile/',
      resultBackUrl: '/',
      entryUrl: '/mobile-h5/?page=' + DEFAULT_PAGE_CODE
    }
  };

  function trim(value) {
    return String(value || '').trim();
  }

  function normalizePageCode(value) {
    var code = trim(value).toLowerCase();
    if (!code) return '';
    if (!PAGE_CODE_PATTERN.test(code)) return '';
    return code;
  }

  function getAppBase() {
    var path = window.location.pathname || '/';
    if (path === '/mobile-h5' || path.indexOf('/mobile-h5/') === 0) {
      return '/mobile-h5';
    }
    if (path === '/mobile-h1' || path.indexOf('/mobile-h1/') === 0) {
      return '/mobile-h1';
    }
    return '';
  }

  function getRequestedPageFromUrl() {
    try {
      var params = new URLSearchParams(window.location.search || '');
      return normalizePageCode(params.get('page'));
    } catch (e) {
      return '';
    }
  }

  function getApiBase() {
    if (window.BiaojiApiBridge && window.BiaojiApiBridge.API_BASE) {
      return String(window.BiaojiApiBridge.API_BASE);
    }
    return DEFAULT_API_BASE;
  }

  function splitUrl(url) {
    var source = String(url || '');
    var hash = '';
    var query = '';
    var hashIndex = source.indexOf('#');
    if (hashIndex >= 0) {
      hash = source.slice(hashIndex);
      source = source.slice(0, hashIndex);
    }
    var queryIndex = source.indexOf('?');
    if (queryIndex >= 0) {
      query = source.slice(queryIndex + 1);
      source = source.slice(0, queryIndex);
    }
    return {
      path: source || '',
      query: query,
      hash: hash
    };
  }

  function isExternalHref(href) {
    var text = trim(href).toLowerCase();
    if (!text) return false;
    return (
      text.indexOf('http://') === 0 ||
      text.indexOf('https://') === 0 ||
      text.indexOf('//') === 0 ||
      text.indexOf('mailto:') === 0 ||
      text.indexOf('tel:') === 0 ||
      text.indexOf('javascript:') === 0 ||
      text.indexOf('data:') === 0
    );
  }

  function resolveHref(href, options) {
    var raw = trim(href);
    if (!raw) return raw;
    if (raw.charAt(0) === '#') return raw;
    if (isExternalHref(raw)) return raw;

    var parts = splitUrl(raw);
    if (!parts.path || parts.path.charAt(0) !== '/') {
      return raw;
    }

    var appBase = getAppBase();
    var path = parts.path;
    var finalPath = path;
    if (appBase) {
      if (path === appBase || path.indexOf(appBase + '/') === 0) {
        finalPath = path;
      } else {
        finalPath = path === '/' ? appBase + '/' : appBase + path;
      }
    }

    var resolvedOptions = options || {};
    var addPage = resolvedOptions.addPage !== false;
    var pageCode = normalizePageCode(resolvedOptions.pageCode || state.config.pageCode || DEFAULT_PAGE_CODE);
    var query = parts.query;
    if (addPage && pageCode) {
      var params = new URLSearchParams(query || '');
      if (!params.has('page')) {
        params.set('page', pageCode);
      }
      query = params.toString();
    }

    return finalPath + (query ? '?' + query : '') + parts.hash;
  }

  function toPath(url) {
    var text = trim(url);
    if (!text) return '';
    if (!isExternalHref(text)) return text;
    try {
      var u = new URL(text, window.location.origin);
      return u.pathname + (u.search || '') + (u.hash || '');
    } catch (e) {
      return text;
    }
  }

  function buildDefaultEntry(pageCode) {
    var code = normalizePageCode(pageCode) || DEFAULT_PAGE_CODE;
    return '/mobile-h5/?page=' + encodeURIComponent(code);
  }

  function mergeConfig(remoteData) {
    var data = remoteData || {};
    var pageCode = normalizePageCode(data.pageCode) || DEFAULT_PAGE_CODE;
    var requestedPage = normalizePageCode(data.requestedPage) || state.requestedPage || DEFAULT_PAGE_CODE;
    var merged = {
      requestedPage: requestedPage,
      pageCode: pageCode,
      pageName: trim(data.pageName) || state.config.pageName,
      servicePhone: trim(data.servicePhone) || state.config.servicePhone,
      wechatQrUrl: trim(data.wechatQrUrl) || state.config.wechatQrUrl,
      navHomeUrl: trim(data.navHomeUrl) || state.config.navHomeUrl,
      navQueryUrl: trim(data.navQueryUrl) || state.config.navQueryUrl,
      navBatchUrl: trim(data.navBatchUrl) || state.config.navBatchUrl,
      navProfileUrl: trim(data.navProfileUrl) || state.config.navProfileUrl,
      resultBackUrl: trim(data.resultBackUrl) || state.config.resultBackUrl,
      entryUrl: trim(data.entryUrl) || buildDefaultEntry(pageCode)
    };
    return merged;
  }

  function flushCallbacks() {
    var list = state.callbacks.slice();
    state.callbacks.length = 0;
    list.forEach(function (cb) {
      try {
        cb(getConfig());
      } catch (e) {}
    });
  }

  function requestPublicConfig(pageCode, callback) {
    var xhr = new XMLHttpRequest();
    var apiBase = getApiBase();
    var path = apiBase + 'server/mobilePageConfig/public/current';
    var query = pageCode ? ('?page=' + encodeURIComponent(pageCode)) : '';
    xhr.open('GET', path + query, true);
    xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    xhr.onreadystatechange = function () {
      if (xhr.readyState !== 4) return;
      if (xhr.status < 200 || xhr.status >= 300) {
        callback(new Error('request_failed'));
        return;
      }
      try {
        var response = JSON.parse(xhr.responseText || '{}');
        var code = Number(response && response.code);
        if (code !== 0 && code !== 200) {
          callback(new Error((response && response.msg) || 'request_failed'));
          return;
        }
        callback(null, (response && response.data) || {});
      } catch (e) {
        callback(e);
      }
    };
    xhr.onerror = function () {
      callback(new Error('network_error'));
    };
    xhr.send(null);
  }

  function ensureLoaded() {
    if (state.loading || state.ready) return;
    state.loading = true;
    requestPublicConfig(state.requestedPage, function (err, data) {
      state.loading = false;
      if (!err && data) {
        state.config = mergeConfig(data);
      } else {
        state.config = mergeConfig({});
      }
      state.ready = true;
      flushCallbacks();
    });
  }

  function getConfig() {
    return {
      requestedPage: state.config.requestedPage,
      pageCode: state.config.pageCode,
      pageName: state.config.pageName,
      servicePhone: state.config.servicePhone,
      wechatQrUrl: state.config.wechatQrUrl,
      navHomeUrl: state.config.navHomeUrl,
      navQueryUrl: state.config.navQueryUrl,
      navBatchUrl: state.config.navBatchUrl,
      navProfileUrl: state.config.navProfileUrl,
      resultBackUrl: state.config.resultBackUrl,
      entryUrl: state.config.entryUrl
    };
  }

  function ready(callback) {
    if (typeof callback !== 'function') return;
    if (state.ready) {
      callback(getConfig());
      return;
    }
    state.callbacks.push(callback);
  }

  state.requestedPage = getRequestedPageFromUrl();
  if (!state.requestedPage) {
    state.requestedPage = DEFAULT_PAGE_CODE;
  }

  window.MobileRuntimeConfig = {
    getConfig: getConfig,
    getPageCode: function () {
      return state.config.pageCode || DEFAULT_PAGE_CODE;
    },
    getRequestedPage: function () {
      return state.requestedPage || DEFAULT_PAGE_CODE;
    },
    getAppBase: getAppBase,
    resolveHref: resolveHref,
    toPath: toPath,
    isReady: function () {
      return state.ready;
    },
    ready: ready
  };

  ensureLoaded();
})();
