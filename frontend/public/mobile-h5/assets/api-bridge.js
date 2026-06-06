(function () {
  var API_BASE = 'https://biaoji.aleo1314.vip/prod-api/';

  var PLATFORM_ID_MAP = {
    泰迪熊: 0,
    腾讯: 1,
    电话邦: 2,
    百度: 3,
    '360': 4,
    移动高频: 5,
    联通管家: 6,
    搜狗: 7,
    小米手机: 8
  };

  var APPEAL_TEXT =
    '泰迪熊：普通标记申诉后6小时生效（30天内二次处理会驳回）；提示10个工作日要暂停拨号；腾讯管家：审核1-3个工作日（期间暂停外呼）；360卫士：三个月内只能处理一次';
  var FREE_QUERY_DEVICE_ID_STORAGE_KEY = 'free_query_device_id';
  var PROFILE_TOKEN_KEY = 'profile_user_token';

  function createRandomDeviceId() {
    var byCrypto = '';
    try {
      if (typeof crypto !== 'undefined' && crypto && typeof crypto.randomUUID === 'function') {
        byCrypto = crypto.randomUUID();
      }
    } catch (error) {
      byCrypto = '';
    }
    if (byCrypto) {
      return 'fq_' + byCrypto.replace(/-/g, '');
    }
    return 'fq_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 12);
  }

  function getOrCreateDeviceId() {
    if (typeof window === 'undefined') {
      return createRandomDeviceId();
    }
    try {
      var exists = window.localStorage.getItem(FREE_QUERY_DEVICE_ID_STORAGE_KEY);
      if (exists && exists.trim()) {
        return exists.trim();
      }
      var created = createRandomDeviceId();
      window.localStorage.setItem(FREE_QUERY_DEVICE_ID_STORAGE_KEY, created);
      return created;
    } catch (error) {
      return createRandomDeviceId();
    }
  }

  function buildSingleQueryPayload(phone) {
    return {
      phone: String(phone || '').trim(),
      deviceId: getOrCreateDeviceId()
    };
  }

  function getStoredFreeToken() {
    if (typeof window === 'undefined') {
      return '';
    }
    try {
      return String(window.localStorage.getItem(PROFILE_TOKEN_KEY) || '').trim();
    } catch (error) {
      return '';
    }
  }

  function normalizeBatchPhones(phones) {
    if (!Array.isArray(phones)) return [];
    var seen = {};
    var list = [];
    phones.forEach(function (item) {
      var phone = String(item || '').replace(/\D/g, '').trim();
      if (!/^\d{7,15}$/.test(phone)) return;
      if (seen[phone]) return;
      seen[phone] = true;
      list.push(phone);
    });
    return list.slice(0, 20);
  }

  function parseStatus(item) {
    if (item && typeof item.error === 'string' && item.error.trim()) {
      return { status: '查询失败', process: 0 };
    }
    var pr =
      item &&
      item.data &&
      item.data.platformResults &&
      item.data.platformResults[0];
    if (!pr) {
      return { status: '查询失败', process: 0 };
    }
    var platformName = String((item && (item.platformName || item.platform)) || '').trim();
    var st = String(pr.status || '');
    if (st.indexOf('yes-') === 0) {
      var msg = st.slice(4).trim();
      if (msg.indexOf('泰迪未来标记已取消') >= 0 || msg.indexOf('同步时间') >= 0) {
        return { status: '无标记', process: 0 };
      }
      if (platformName === '移动高频' && (!msg || msg === '普通标记')) {
        return { status: '高频拦截', process: 1 };
      }
      return { status: msg || '有标记', process: 1 };
    }
    if (st === 'yes') {
      if (platformName === '移动高频') {
        return { status: '高频拦截', process: 1 };
      }
      return { status: '有标记', process: 1 };
    }
    if (st.indexOf('no') === 0 || st === 'no') {
      return { status: '无标记', process: 0 };
    }
    return { status: '查询失败', process: 0 };
  }

  function transformSearchResponse(resp) {
    if (resp.code === 42901 || resp.code === 42902 || resp.code === 42903) {
      return { code: 1, msg: resp.msg || '查询失败' };
    }
    if (resp.code !== 200 && resp.code !== 0) {
      return { code: 1, msg: resp.msg || '查询失败' };
    }
    var raw = (resp.data && resp.data.results) || [];
    var results = raw.map(function (item) {
      var parsed = parseStatus(item);
      var name = item.platformName || '';
      return {
        id: Object.prototype.hasOwnProperty.call(PLATFORM_ID_MAP, name)
          ? PLATFORM_ID_MAP[name]
          : 99,
        platform: name,
        status: parsed.status,
        process: parsed.process
      };
    });
    return {
      code: 0,
      data: {
        results: results,
        expire_time: null,
        payment: {
          paymentMethods: [],
          customerwx: '18537174371',
          appid: ''
        }
      }
    };
  }

  function requestJson(url, method, data, success, fail, headers) {
    var xhr = new XMLHttpRequest();
    xhr.open(method, url, true);
    xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    if (headers) {
      Object.keys(headers).forEach(function (key) {
        xhr.setRequestHeader(key, headers[key]);
      });
    }
    xhr.onload = function () {
      var body = {};
      try {
        body = JSON.parse(xhr.responseText || '{}');
      } catch (e) {
        fail && fail(e);
        return;
      }
      if (xhr.status >= 200 && xhr.status < 300) {
        success && success(body);
      } else {
        fail && fail(body);
      }
    };
    xhr.onerror = function () {
      fail && fail(new Error('network error'));
    };
    xhr.send(method === 'GET' ? null : JSON.stringify(data || {}));
  }

  window.BiaojiApiBridge = {
    API_BASE: API_BASE,
    queryPhone: function (phone, callback) {
      var authToken = getStoredFreeToken();
      var headers = authToken ? { 'X-Free-Token': authToken } : null;
      requestJson(
        API_BASE + 'server/freeQuery/single',
        'POST',
        buildSingleQueryPayload(phone),
        function (resp) {
          if (resp.code === 42901 || resp.code === 42902 || resp.code === 42903) {
            callback(resp.msg || '查询次数已达上限');
            return;
          }
          if (resp.code !== 200 && resp.code !== 0) {
            callback(resp.msg || '查询失败');
            return;
          }
          var transformed = transformSearchResponse(resp);
          if (transformed.code !== 0) {
            callback(transformed.msg || '查询失败');
            return;
          }
          callback(null, transformed.data);
        },
        function (err) {
          callback((err && err.msg) || '网络错误，请重试');
        },
        headers
      );
    },
    queryRecords: function (token, callback) {
      var cb = callback;
      var inputToken = token;
      if (typeof token === 'function') {
        cb = token;
        inputToken = '';
      }
      var authToken = String(inputToken || getStoredFreeToken() || '').trim();
      if (!authToken) {
        cb && cb('请先登录后查看查询记录', []);
        return;
      }

      requestJson(
        API_BASE + 'server/freeQuery/records',
        'GET',
        null,
        function (resp) {
          var code = Number(resp && resp.code);
          if (code === 0 || code === 200) {
            var data = (resp && resp.data) || [];
            cb && cb(null, Array.isArray(data) ? data : []);
            return;
          }
          cb && cb((resp && resp.msg) || '获取查询记录失败', []);
        },
        function (err) {
          cb && cb((err && err.msg) || '网络错误，请重试', []);
        },
        {
          'X-Free-Token': authToken
        }
      );
    },
    requests: function (opts, apiBase, fallbackRequest) {
      var url = opts.url || '';
      var data = opts.data || {};
      var success = opts.success;
      var fail = opts.fail;
      var base = apiBase || API_BASE;

      if (url === 'user/getinfo') {
        requestJson(
          base + 'server/freeQuery/quota',
          'GET',
          null,
          function () {
            success &&
              success({
                code: 0,
                data: {
                  name: '取消号码标记',
                  appealDescription: APPEAL_TEXT
                }
              });
          },
          function () {
            success &&
              success({
                code: 0,
                data: {
                  name: '取消号码标记',
                  appealDescription: APPEAL_TEXT
                }
              });
          }
        );
        return;
      }

      if (url === 'user/search') {
        requestJson(
          base + 'server/freeQuery/single',
          'POST',
          buildSingleQueryPayload(data.phone),
          function (resp) {
            success && success(transformSearchResponse(resp));
          },
          function (err) {
            var msg =
              (err && err.msg) || '查询失败，请重试';
            success && success({ code: 1, msg: msg });
            fail && fail(err);
          }
        );
        return;
      }

      if (fallbackRequest) {
        fallbackRequest({
          url: base + url,
          method: 'POST',
          data: data,
          header: { 'Content-Type': 'application/json' },
          success: function (res) {
            success && success(res.data);
          },
          fail: fail
        });
        return;
      }

      requestJson(
        base + url,
        'POST',
        data,
        function (resp) {
          success && success(resp);
        },
        fail
      );
    },
    batchQuery: function (phones, token, callback) {
      var authToken = String(token || getStoredFreeToken() || '').trim();
      if (!authToken) {
        callback && callback('请先登录后再批量查询');
        return;
      }

      var normalizedPhones = normalizeBatchPhones(phones);
      if (!normalizedPhones.length) {
        callback && callback('请输入至少一个有效号码');
        return;
      }

      requestJson(
        API_BASE + 'server/apiServer/asyncBatchOpt',
        'POST',
        {
          phones: normalizedPhones,
          token: authToken,
          deviceId: getOrCreateDeviceId()
        },
        function (resp) {
          var code = Number(resp && resp.code);
          if (code === 0 || code === 200) {
            callback && callback(null, (resp && resp.data) || {});
            return;
          }
          callback && callback((resp && resp.msg) || '批量查询失败', (resp && resp.data) || null);
        },
        function (err) {
          callback && callback((err && err.msg) || '网络错误，请重试');
        },
        {
          'X-Free-Token': authToken
        }
      );
    }
  };
})();
