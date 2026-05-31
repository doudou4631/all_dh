(function () {
  var STORAGE_KEY = 'biaoji_user_session';

  function readSession() {
    try {
      var raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      var data = JSON.parse(raw);
      if (!data || !data.token || !data.phone) return null;
      return data;
    } catch (e) {
      return null;
    }
  }

  function writeSession(data) {
    if (!data) {
      localStorage.removeItem(STORAGE_KEY);
      return;
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  }

  function maskPhone(phone) {
    var s = String(phone || '');
    if (s.length === 11) return s.slice(0, 3) + '****' + s.slice(7);
    if (s.length >= 7) return s.slice(0, 3) + '****' + s.slice(-2);
    return s;
  }

  window.UserAuth = {
    getSession: readSession,
    isLoggedIn: function () {
      return !!readSession();
    },
    canUseBatch: function () {
      var s = readSession();
      return !!(s && s.batchEnabled && (s.balance > 0 || s.balance === -1));
    },
    maskPhone: maskPhone,
    saveSession: function (data) {
      writeSession(data);
    },
    logout: function () {
      writeSession(null);
    },
    refreshAccount: function (callback) {
      var session = readSession();
      if (!session) {
        callback && callback(null);
        return;
      }
      if (!window.BiaojiApiBridge || !window.BiaojiApiBridge.getAccountInfo) {
        callback && callback(session);
        return;
      }
      window.BiaojiApiBridge.getAccountInfo(session.token, function (err, info) {
        if (err || !info) {
          callback && callback(session);
          return;
        }
        var next = {
          token: session.token,
          phone: info.phone || session.phone,
          batchEnabled: !!info.batchEnabled,
          balance: typeof info.balance === 'number' ? info.balance : 0,
          nickname: info.nickname || maskPhone(info.phone || session.phone)
        };
        writeSession(next);
        callback && callback(next);
      });
    }
  };
})();
