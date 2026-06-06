(function () {
  var ACCOUNT_KEY = 'profile_user_account';
  var RECORDS_MAP_KEY = 'profile_query_records_map';
  var LEGACY_RECORDS_KEY = 'profile_query_records';
  var MAX_RECORDS_PER_ACCOUNT = 120;

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
      return (localStorage.getItem(ACCOUNT_KEY) || '').trim();
    } catch (e) {
      return '';
    }
  }

  function pad(n) {
    return n < 10 ? '0' + n : String(n);
  }

  function nowText() {
    var d = new Date();
    return (
      d.getFullYear() +
      '-' +
      pad(d.getMonth() + 1) +
      '-' +
      pad(d.getDate()) +
      ' ' +
      pad(d.getHours()) +
      ':' +
      pad(d.getMinutes())
    );
  }

  function normalizeType(type) {
    return String(type || '').trim() === '批量查询' ? '批量查询' : '单号查询';
  }

  function normalizeMarked(marked) {
    var n = Number(marked);
    if (isNaN(n) || n < 0) return 0;
    return Math.round(n);
  }
  function normalizePhone(phone) {
    var raw = String(phone == null ? '' : phone).trim();
    if (!raw) return '';
    var digits = raw.replace(/\D/g, '');
    if (/^\d{7,15}$/.test(digits)) return digits;
    return raw;
  }

  function normalizeMarkedPlatforms(value) {
    var list = [];
    if (Array.isArray(value)) {
      list = value;
    } else if (typeof value === 'string') {
      list = value.split(/[、,，;；|·]+/);
    } else if (value != null) {
      list = [value];
    }
    var seen = {};
    return list
      .map(function (item) {
        return String(item == null ? '' : item).trim();
      })
      .filter(function (name) {
        if (!name) return false;
        if (seen[name]) return false;
        seen[name] = true;
        return true;
      });
  }

  function normalizeRecord(record) {
    if (!record) return null;
    var phone = normalizePhone(record.phone);
    if (!phone) return null;
    var markedPlatforms = normalizeMarkedPlatforms(
      record.markedPlatforms != null ? record.markedPlatforms : record.platforms
    );
    var marked = normalizeMarked(record.marked);
    if (!marked && markedPlatforms.length) {
      marked = markedPlatforms.length;
    }
    return {
      phone: phone,
      type: normalizeType(record.type),
      marked: marked,
      markedPlatforms: markedPlatforms,
      time: String(record.time || '').trim() || nowText()
    };
  }

  function readMap() {
    var map = readJson(RECORDS_MAP_KEY, {});
    return map && typeof map === 'object' ? map : {};
  }

  function writeMap(map) {
    writeJson(RECORDS_MAP_KEY, map);
  }

  function migrateLegacyRecords(account) {
    if (!account) return;
    var legacy = readJson(LEGACY_RECORDS_KEY, []);
    if (!Array.isArray(legacy) || !legacy.length) return;

    var map = readMap();
    if (Array.isArray(map[account]) && map[account].length) return;

    var normalized = legacy.map(normalizeRecord).filter(Boolean);
    if (!normalized.length) return;

    map[account] = normalized.slice(0, MAX_RECORDS_PER_ACCOUNT);
    writeMap(map);
    try {
      localStorage.removeItem(LEGACY_RECORDS_KEY);
    } catch (e) {}
  }

  function getRecords(account) {
    account = String(account || getAccount() || '').trim();
    if (!account) return [];

    migrateLegacyRecords(account);
    var map = readMap();
    var list = Array.isArray(map[account]) ? map[account] : [];
    return list.map(normalizeRecord).filter(Boolean);
  }

  function addRecords(records, account) {
    account = String(account || getAccount() || '').trim();
    if (!account) return [];
    if (!Array.isArray(records) || !records.length) return getRecords(account);

    var items = records.map(normalizeRecord).filter(Boolean);
    if (!items.length) return getRecords(account);

    var map = readMap();
    var oldList = Array.isArray(map[account]) ? map[account] : [];
    var merged = items.concat(oldList).slice(0, MAX_RECORDS_PER_ACCOUNT);
    map[account] = merged;
    writeMap(map);
    return merged;
  }

  function addRecord(record, account) {
    return addRecords([record], account);
  }

  function addSingleRecord(phone, marked, markedPlatforms, account) {
    var targetAccount = account;
    var platforms = markedPlatforms;
    if (arguments.length === 3 && !Array.isArray(markedPlatforms)) {
      targetAccount = markedPlatforms;
      platforms = [];
    }
    return addRecord(
      {
        phone: phone,
        type: '单号查询',
        marked: marked,
        markedPlatforms: platforms,
        time: nowText()
      },
      targetAccount
    );
  }

  function addBatchRecords(results, account) {
    if (!Array.isArray(results) || !results.length) {
      return getRecords(account);
    }
    var time = nowText();
    var records = results.map(function (row) {
      var marked = 0;
      var markedPlatforms = [];
      if (Array.isArray(row && row.markedItems)) {
        marked = row.markedItems.length;
        markedPlatforms = row.markedItems.map(function (item) {
          return item && item.platform;
        });
      } else if (typeof row === 'object' && row) {
        marked = normalizeMarked(row.marked);
        markedPlatforms = normalizeMarkedPlatforms(
          row.markedPlatforms != null ? row.markedPlatforms : row.platforms
        );
      }
      return {
        phone: row && row.phone,
        type: '批量查询',
        marked: marked,
        markedPlatforms: markedPlatforms,
        time: time
      };
    });
    return addRecords(records, account);
  }

  window.QueryRecords = {
    getAccount: getAccount,
    getRecords: getRecords,
    addRecord: addRecord,
    addSingleRecord: addSingleRecord,
    addBatchRecords: addBatchRecords
  };
})();
