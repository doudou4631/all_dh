(function () {
  var PROFILE_ACCOUNT_KEY = 'profile_user_account';
  var PROFILE_TOKEN_KEY = 'profile_user_token';
  var PROFILE_POINTS_KEY = 'profile_user_points';

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

  function toPath(url) {
    if (window.MobileRuntimeConfig && typeof window.MobileRuntimeConfig.toPath === 'function') {
      return window.MobileRuntimeConfig.toPath(url);
    }
    return url;
  }

  function getProfileAccount() {
    try {
      return (localStorage.getItem(PROFILE_ACCOUNT_KEY) || '').trim();
    } catch (e) {
      return '';
    }
  }

  function getProfileToken() {
    try {
      return (localStorage.getItem(PROFILE_TOKEN_KEY) || '').trim();
    } catch (e) {
      return '';
    }
  }

  function getBatchLoginRedirectUrl() {
    var redirectTarget = toPath(resolveHref('/batch/'));
    var redirect = encodeURIComponent(redirectTarget || '/batch/');
    return resolveHref('/profile/?redirect=' + redirect);
  }

  function ensureBatchLogin() {
    if (getProfileAccount() && getProfileToken()) return true;
    window.location.replace(getBatchLoginRedirectUrl());
    return false;
  }

  if (!ensureBatchLogin()) return;
  var phonesEl = document.getElementById('phones');
  var countEl = document.getElementById('count');
  var submitBtn = document.getElementById('submit');
  var resultsEl = document.getElementById('results');
  var detailModal = document.getElementById('batch-detail-modal');
  var detailPhoneEl = document.getElementById('batch-detail-phone');
  var detailListEl = document.getElementById('batch-detail-list');

  var currentTaskId = '';
  var currentResults = [];
  var currentSummary = null;
  var batchToastTimer = null;

  function notify(message) {
    var text = String(message || '').trim();
    if (!text) return;
    if (window.AppBottomNav && typeof window.AppBottomNav.notify === 'function') {
      window.AppBottomNav.notify(text);
      return;
    }
    var toast = document.getElementById('batch-inline-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'batch-inline-toast';
      toast.style.cssText =
        'position:fixed;left:50%;bottom:96px;transform:translateX(-50%);max-width:82vw;padding:10px 14px;border-radius:10px;background:rgba(28,28,30,.92);color:#fff;font-size:13px;line-height:1.4;text-align:center;z-index:2300;';
      document.body.appendChild(toast);
    }
    toast.textContent = text;
    toast.hidden = false;
    if (batchToastTimer) clearTimeout(batchToastTimer);
    batchToastTimer = setTimeout(function () {
      toast.hidden = true;
    }, 1800);
  }

  function parsePhones(text) {
    return text
      .split(/[\r\n,，;；\s]+/)
      .map(function (s) {
        return s.trim();
      })
      .filter(function (s) {
        return /^\d{7,15}$/.test(s);
      })
      .slice(0, 20);
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }


  function getPlatformDisplayName(name) {
    if (!name) return '未知平台';
    if (name === '360' || name === '360手机卫士') return '360手机卫士';
    if (name.indexOf('360') >= 0 && name.indexOf('卫士') < 0) return '360手机卫士';
    return name;
  }
  function parseMarkedInfo(platformResult) {
    if (platformResult && typeof platformResult.error === 'string' && platformResult.error.trim()) {
      return { marked: false, markType: '' };
    }
    var firstResult =
      platformResult &&
      platformResult.data &&
      platformResult.data.platformResults &&
      platformResult.data.platformResults[0];
    if (!firstResult) {
      return { marked: false, markType: '' };
    }
    var platformName = getPlatformDisplayName((platformResult && (platformResult.platformName || platformResult.platform)) || '');
    var isMobileHighFreq = platformName === '移动高频';
    var status = String(firstResult.status || '').trim();
    if (!status) {
      return { marked: false, markType: '' };
    }
    if (status.indexOf('yes-') === 0) {
      var markType = status.slice(4).trim();
      if (!markType || markType.indexOf('泰迪未来标记已取消') >= 0 || markType.indexOf('同步时间') >= 0) {
        return { marked: false, markType: '' };
      }
      if (isMobileHighFreq && markType === '普通标记') {
        return { marked: true, markType: '高频拦截' };
      }
      return { marked: true, markType: markType };
    }
    if (status === 'yes') {
      if (isMobileHighFreq) {
        return { marked: true, markType: '高频拦截' };
      }
      return { marked: true, markType: '普通标记' };
    }
    if (status.indexOf('no') === 0) {
      return { marked: false, markType: '' };
    }
    return { marked: false, markType: '' };
  }

  function getMarkedItems(results) {
    if (!Array.isArray(results) || !results.length) return [];
    var list = [];
    results.forEach(function (r) {
      var parsed = parseMarkedInfo(r);
      if (!parsed.marked) return;
      list.push({
        platform: getPlatformDisplayName((r && (r.platformName || r.platform)) || ''),
        markType: parsed.markType || '普通标记'
      });
    });
    return list;
  }


  function mapBatchResultItem(item) {
    var code = Number(item && item.code);
    var success = code === 0 || code === 200;
    var data = item && item.data;
    var platformResults = data && Array.isArray(data.results) ? data.results : [];
    return {
      phone: String((item && item.phone) || ''),
      error: !success,
      errorMessage: success ? '' : String((item && item.message) || '查询失败').trim() || '查询失败',
      markedItems: success ? getMarkedItems(platformResults) : [],
      failedEntries: Number(item && item.failedEntries) || 0
    };
  }

  function normalizeBatchResults(resultRows, fallbackPhones) {
    if (Array.isArray(resultRows) && resultRows.length) {
      return resultRows.map(mapBatchResultItem);
    }
    return (fallbackPhones || []).map(function (phone) {
      return {
        phone: phone,
        error: true,
        errorMessage: '查询失败',
        markedItems: [],
        failedEntries: 0
      };
    });
  }


  function buildResultItem(item, index) {
    var phone = item.phone;
    var tags = '';
    var badgeClass = 'batch-result-badge--clean';
    var badgeText = '无标记';

    if (item.error) {
      tags = item.errorMessage || '查询失败，请稍后重试';
      badgeClass = 'batch-result-badge--fail';
      badgeText = '查询失败';
    } else if (item.markedItems.length) {
      tags = item.markedItems
        .map(function (m) {
          return m.platform;
        })
        .join(' · ');
      badgeClass = 'batch-result-badge--marked';
      badgeText = '有标记';
    } else {
      tags = '未发现平台标记';
    }

    return (
      '<button type="button" class="batch-result-row" data-index="' +
      index +
      '">' +
      '<div class="batch-result-main">' +
      '<p class="batch-result-phone">' +
      escapeHtml(phone) +
      '</p>' +
      '<p class="batch-result-tags' +
      (item.error || !item.markedItems.length ? ' is-muted' : '') +
      '">' +
      escapeHtml(tags) +
      '</p>' +
      '</div>' +
      '<span class="batch-result-badge ' +
      badgeClass +
      '">' +
      badgeText +
      '</span>' +
      '<span class="batch-result-arrow">›</span>' +
      '</button>'
    );
  }

  function renderTaskCard(done, current, total) {
    var percent = total ? Math.round((current / total) * 100) : 0;
    var taskIdText = currentTaskId || (done ? '—' : '生成中');
    var html =
      '<div class="batch-task-card">' +
      '<div class="batch-task-row"><span>任务ID</span><span>' +
      escapeHtml(taskIdText) +
      '</span></div>' +
      '<div class="batch-task-row"><span>状态</span><span class="batch-status ' +
      (done ? 'batch-status--done' : 'batch-status--running') +
      '">' +
      (done ? '已完成' : '查询中') +
      '</span></div>' +
      '<div class="batch-task-row"><span>进度</span><span>' +
      current +
      '/' +
      total +
      '</span></div>' +
      '<div class="batch-progress"><div class="batch-progress-bar" style="width:' +
      percent +
      '%\"></div></div>';
    html += '</div>';
    return html;
  }

  function renderResultsPanel() {
    var count = currentResults.length;
    var listHtml = currentResults
      .map(function (item, index) {
        return buildResultItem(item, index);
      })
      .join('');
    return (
      '<div class="batch-results-panel">' +
      '<div class="batch-results-head">' +
      '<h2>查询结果 (' +
      count +
      '条)</h2>' +
      '<button type="button" class="batch-csv-btn" id="batch-csv-btn">下载CSV</button>' +
      '</div>' +
      '<div class="batch-results-list">' +
      listHtml +
      '</div>' +
      '</div>'
    );
  }

  function renderProgress(current, total, done) {
    var html = renderTaskCard(done, current, total);
    if (done) {
      html += renderResultsPanel();
    }
    resultsEl.innerHTML = html;
    if (done) {
      bindCsvDownload();
      bindResultRows();
    }
  }

  function openDetailModal(item) {
    if (!detailModal || !item) return;

    detailPhoneEl.textContent = item.phone;

    if (item.error) {
      detailListEl.innerHTML =
        '<p class=\"batch-detail-empty\">' + escapeHtml(item.errorMessage || '查询失败，请稍后重试') + '</p>';
    } else if (!item.markedItems.length) {
      detailListEl.innerHTML = '<p class="batch-detail-empty">该号码未发现被标记平台</p>';
    } else {
      detailListEl.innerHTML = item.markedItems
        .map(function (m) {
          return (
            '<div class="batch-detail-item">' +
            '<div class="batch-detail-item-main">' +
            '<p class="batch-detail-platform">' +
            escapeHtml(m.platform) +
            '</p>' +
            '<p class="batch-detail-type">' +
            escapeHtml(m.markType) +
            '</p>' +
            '</div>' +
            '<span class="batch-result-badge batch-result-badge--marked">有标记</span>' +
            '</div>'
          );
        })
        .join('');
    }

    detailModal.hidden = false;
    document.body.classList.add('batch-detail-open');
  }

  function closeDetailModal() {
    if (!detailModal) return;
    detailModal.hidden = true;
    document.body.classList.remove('batch-detail-open');
  }

  function bindDetailModal() {
    if (!detailModal) return;
    document.getElementById('batch-detail-mask').addEventListener('click', closeDetailModal);
    document.getElementById('batch-detail-close').addEventListener('click', closeDetailModal);
  }

  function bindResultRows() {
    var rows = resultsEl.querySelectorAll('.batch-result-row[data-index]');
    rows.forEach(function (row) {
      row.addEventListener('click', function () {
        var index = Number(row.getAttribute('data-index'));
        var item = currentResults[index];
        if (!item) return;
        openDetailModal(item);
      });
    });
  }

  function bindCsvDownload() {
    var btn = document.getElementById('batch-csv-btn');
    if (!btn) return;
    btn.addEventListener('click', function () {
      if (!currentResults.length) return;
      var lines = ['号码,标记状态,平台,标记类型'];
      currentResults.forEach(function (item) {
        if (item.error) {
          lines.push([item.phone, '查询失败', '', item.errorMessage || '查询失败'].map(csvCell).join(','));
          return;
        }
        if (!item.markedItems.length) {
          lines.push([item.phone, '无标记', '', ''].map(csvCell).join(','));
          return;
        }
        item.markedItems.forEach(function (m, i) {
          lines.push(
            [i === 0 ? item.phone : '', i === 0 ? '有标记' : '', m.platform, m.markType].map(csvCell).join(',')
          );
        });
      });
      var blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8' });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      var taskIdForFile = currentTaskId ? currentTaskId.replace('#', '') : String(Date.now());
      a.href = url;
      a.download = '批量查询结果_' + taskIdForFile + '.csv';
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  function csvCell(value) {
    var s = String(value || '');
    if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0) {
      return '"' + s.replace(/"/g, '""') + '"';
    }
    return s;
  }

  function updateCount() {
    countEl.textContent = '已输入 ' + parsePhones(phonesEl.value).length + ' 个号码';
  }

  function extractNumbers() {
    var text = phonesEl.value || '';
    var matches = text.match(/\d{7,15}/g) || [];
    var seen = {};
    var list = [];
    matches.forEach(function (num) {
      if (!seen[num]) {
        seen[num] = true;
        list.push(num);
      }
    });
    phonesEl.value = list.slice(0, 20).join('\n');
    updateCount();
    if (!list.length) {
      notify('未识别到有效号码');
    }
  }

  function pasteNumbers() {
    if (navigator.clipboard && navigator.clipboard.readText) {
      navigator.clipboard
        .readText()
        .then(function (text) {
          var current = phonesEl.value.replace(/\s+$/, '');
          phonesEl.value = current ? current + '\n' + text : text;
          updateCount();
        })
        .catch(function () {
          phonesEl.focus();
          notify('无法读取剪贴板，请长按输入框手动粘贴');
        });
      return;
    }
    phonesEl.focus();
    notify('请长按输入框手动粘贴号码');
  }

  function clearList() {
    phonesEl.value = '';
    updateCount();
  }
  function syncRemainingPoints(remainingPoints) {
    var num = Number(remainingPoints);
    if (isNaN(num)) return;
    var account = getProfileAccount();
    if (!account) return;
    try {
      var mapRaw = localStorage.getItem(PROFILE_POINTS_KEY);
      var map = mapRaw ? JSON.parse(mapRaw) : {};
      map[account] = Math.max(0, Math.round(num));
      localStorage.setItem(PROFILE_POINTS_KEY, JSON.stringify(map));
    } catch (e) {}
  }

  function clearTokenAndRedirect(message) {
    try {
      localStorage.removeItem(PROFILE_TOKEN_KEY);
    } catch (e) {}
    notify(message || '登录已失效，请重新登录');
    setTimeout(function () {
      window.location.href = getBatchLoginRedirectUrl();
    }, 800);
  }

  function applyBatchResponse(data, phones) {
    currentTaskId = String((data && data.taskId) || '').trim();
    currentSummary = {
      total: Number(data && data.total) || phones.length,
      platformCount: Number(data && data.platformCount) || 0,
      successCount: Number(data && data.successCount) || 0,
      failedCount: Number(data && data.failedCount) || 0,
      totalChargePoints: Number(data && data.totalChargePoints) || 0,
      refundedPoints: Number(data && data.refundedPoints) || 0,
      actualCostPoints: Number(data && data.actualCostPoints) || 0,
      remainingPoints: Number(data && data.remainingPoints)
    };
    currentResults = normalizeBatchResults(data && data.results, phones);
    if (window.QueryRecords && typeof window.QueryRecords.addBatchRecords === 'function') {
      window.QueryRecords.addBatchRecords(currentResults);
    }
    renderProgress(currentResults.length, phones.length, true);
    if (window.QueryStats && typeof window.QueryStats.recordQueryForCurrentUser === 'function') {
      window.QueryStats.recordQueryForCurrentUser(currentResults.length || phones.length);
    }
    if (!isNaN(currentSummary.remainingPoints)) {
      syncRemainingPoints(currentSummary.remainingPoints);
    }
  }

  phonesEl.addEventListener('input', updateCount);

  document.getElementById('btn-extract').addEventListener('click', extractNumbers);
  document.getElementById('btn-paste').addEventListener('click', pasteNumbers);
  document.getElementById('btn-clear').addEventListener('click', clearList);

  submitBtn.addEventListener('click', function () {
    var list = parsePhones(phonesEl.value);
    if (!list.length) {
      notify('请输入至少一个有效号码');
      return;
    }
    if (!window.BiaojiApiBridge || typeof window.BiaojiApiBridge.batchQuery !== 'function') {
      notify('批量查询服务未加载，请刷新后重试');
      return;
    }
    var token = getProfileToken();
    if (!token) {
      clearTokenAndRedirect('请先登录后再批量查询');
      return;
    }

    submitBtn.disabled = true;
    currentTaskId = '';
    currentResults = [];
    currentSummary = null;

    renderProgress(0, list.length, false);

    window.BiaojiApiBridge.batchQuery(list, token, function (err, data) {
      submitBtn.disabled = false;
      if (err) {
        var msg = String(err || '').trim();
        if (msg.indexOf('登录') >= 0 || msg.indexOf('失效') >= 0 || msg.indexOf('401') >= 0) {
          clearTokenAndRedirect(msg || '登录已失效，请重新登录');
          return;
        }
        notify(msg || '批量查询失败');
        return;
      }
      applyBatchResponse(data || {}, list);
    });
  });

  updateCount();
  bindDetailModal();
})();
