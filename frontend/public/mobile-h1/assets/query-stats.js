(function () {
  var ACCOUNT_KEY = 'profile_user_account';
  var TODAY_QUERY_KEY = 'profile_today_query';

  function pad(n) {
    return n < 10 ? '0' + n : String(n);
  }

  function getTodayDateStr() {
    var d = new Date();
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
  }

  function getAccount() {
    try {
      return localStorage.getItem(ACCOUNT_KEY) || '';
    } catch (e) {
      return '';
    }
  }

  function readMap() {
    try {
      var raw = localStorage.getItem(TODAY_QUERY_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch (e) {
      return {};
    }
  }

  function writeMap(map) {
    try {
      localStorage.setItem(TODAY_QUERY_KEY, JSON.stringify(map));
    } catch (e) {}
  }

  function getTodayQueryCount(account) {
    account = account || getAccount();
    if (!account) return 0;

    var today = getTodayDateStr();
    var map = readMap();
    var item = map[account];

    if (!item || item.date !== today) return 0;
    return Number(item.count) || 0;
  }

  function recordQuery(account, count) {
    account = account || getAccount();
    if (!account) return 0;

    var n = Number(count);
    if (isNaN(n) || n <= 0) n = 1;

    var today = getTodayDateStr();
    var map = readMap();
    var item = map[account];

    if (!item || item.date !== today) {
      item = { date: today, count: 0 };
    }

    item.count = (Number(item.count) || 0) + n;
    map[account] = item;
    writeMap(map);
    return item.count;
  }

  function recordQueryForCurrentUser(count) {
    return recordQuery(getAccount(), count);
  }

  window.QueryStats = {
    getAccount: getAccount,
    getTodayDateStr: getTodayDateStr,
    getTodayQueryCount: getTodayQueryCount,
    recordQuery: recordQuery,
    recordQueryForCurrentUser: recordQueryForCurrentUser
  };
})();
