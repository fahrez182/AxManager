/* ───────────────────────────────────────────────────────────────────────────
   status.js — Hook Status bottom-sheet  (window.HookStatus)

   Device-wide "served spoofs" board. Reads /data/adb/COPG/served.jsonl (written by
   the Zygisk module's flushServed via the root companion) and shows, per capability,
   the most recent fake VALUE COPG actually served + which app + when. Proof the hooks
   are alive across the device.

   CRACK-SAFE: the log carries ONLY spoofed values (user data). It never contains
   license / PRO / engine / key state, and a locked/unconfigured feature writes
   nothing — so this board can't be used as an unlock oracle. See the memory
   [[no-license-internals-in-ui]]. No pass/fail flags here, on purpose.

   Presentation only — FS work is in copg-data.js (getServed/clearServed).
   Opened from Settings → Hook Status.  window.HookStatus.open()
─────────────────────────────────────────────────────────────────────────── */
(function (w) {
  'use strict';

  /* capability key → short Latin label (technical terms stay Latin across languages). */
  const LABELS = {
    device: 'Build / Model', serial: 'Serial', cpu: 'CPU', cow: 'Props (COW)',
    tz: 'Timezone', rr: 'Refresh rate', vpn: 'VPN hide', hidedev: 'Dev-opts hide',
    aid: 'ANDROID_ID', gpu: 'GPU', sensor: 'Sensors', webfp: 'Canvas FP',
    webua: 'User-Agent', mock: 'Mock-Loc hide', gps: 'GPS', cell: 'Cell tower',
    gaid: 'GAID', appset: 'App Set ID', drm: 'DRM', imei: 'IMEI', imsi: 'IMSI',
    iccid: 'ICCID', sim: 'SIM / Carrier', uptime: 'Uptime',
  };
  const ORDER = Object.keys(LABELS);

  const SVG_X =
    `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`;
  const SVG_REFRESH =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-2.64-6.36"/><polyline points="21 3 21 8 16 8"/></svg>`;
  const SVG_TRASH =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>`;

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--status" id="statusSheet" role="dialog" aria-modal="true" aria-label="Hook Status">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="status_title">Hook Status</span>
        <button class="sheet-header__close" id="statusClose" aria-label="Close">${SVG_X}</button>
      </div>
      <div class="sheet-body">
        <div class="syshook" id="sysHook" hidden>
          <div class="syshook__head">
            <span class="syshook__title" data-i18n="sys_title">System hooks</span>
            <span class="syshook__module" id="sysModule"></span>
          </div>
          <div class="syshook__list" id="sysHookList"></div>
        </div>
        <p class="status-intro" data-i18n="status_intro">The spoof values COPG has served across the device recently. Launch an app to see its spoofs appear here.</p>
        <div class="status-actions">
          <button type="button" class="status-btn" id="statusRefreshBtn"><span class="status-btn__ic">${SVG_REFRESH}</span><span data-i18n="status_refresh">Refresh</span></button>
          <button type="button" class="status-btn status-btn--ghost" id="statusClearBtn"><span class="status-btn__ic">${SVG_TRASH}</span><span data-i18n="status_clear">Clear</span></button>
        </div>
        <div class="status-list" id="statusList"></div>
      </div>
    </div>`);

  const $s = sel => document.querySelector(sel);
  const log = (m, t) => { if (w.logEvent) logEvent(m, t || 'info'); };

  function ago(sec) {
    const d = Math.max(0, Math.floor(Date.now() / 1000) - (sec || 0));
    if (d < 60)    return I18N.t('status_now');
    if (d < 3600)  return Math.floor(d / 60) + I18N.t('status_m');
    if (d < 86400) return Math.floor(d / 3600) + I18N.t('status_h');
    return Math.floor(d / 86400) + I18N.t('status_d');
  }

  function appName(pkg) {
    try { return (COPG.nameFor && COPG.nameFor(pkg, '')) || pkg; } catch (_) { return pkg; }
  }

  function renderRows(events, sysInfo) {
    // Read-path consistency for the two spoofs that have an additive system_server layer: aid (in-app
    // forge covers only Settings.Secure.getString; SsaidHook makes query/call/shell match) and hidedev
    // (in-app forge covers only the Settings.Global/Secure getInt cache; HidedevHook makes the direct
    // provider read match). Both layers are live only when the System-Server Spoofs master is active, so
    // the same aggregate flag drives both chips (not per-hook license state → crack-safe).
    const ssaidActive = !!(sysInfo && sysInfo.sysserver === 'active');
    // group globally by capability → most recent event + count of distinct apps served
    const byCap = {};
    events.forEach(e => {
      const c = byCap[e.k] || (byCap[e.k] = { last: e, apps: new Set() });
      c.apps.add(e.p);
      if ((e.t || 0) >= (c.last.t || 0)) c.last = e;
    });
    const listEl = $s('#statusList');
    listEl.innerHTML = '';
    const caps = ORDER.filter(k => byCap[k]);
    if (!caps.length) {
      listEl.innerHTML = `<div class="status-empty">${I18N.t('status_empty')}</div>`;
      return;
    }
    caps.forEach(k => {
      const c = byCap[k];
      const row = document.createElement('div');
      row.className = 'status-row';
      const val = String(c.last.v || '');
      const vShort = val.length > 24 ? val.slice(0, 22) + '…' : val;
      const nApps = c.apps.size;
      const app = appName(c.last.p);
      row.innerHTML = `
        <span class="status-row__dot"></span>
        <span class="status-row__cap"></span>
        <span class="status-row__val"></span>
        <span class="status-row__meta"></span>`;
      row.querySelector('.status-row__cap').textContent = LABELS[k] || k;
      row.querySelector('.status-row__val').textContent = vShort;
      const metaText = app + (nApps > 1 ? ' +' + (nApps - 1) : '') + ' · ' + ago(c.last.t);
      const metaEl = row.querySelector('.status-row__meta');
      if (k === 'aid' || k === 'hidedev') {
        // consistency badge: all paths (system_server hook live) vs app only (in-app forge alone)
        const chip = document.createElement('span');
        chip.className = 'status-row__cons ' + (ssaidActive ? 'is-ok' : 'is-weak');
        chip.textContent = I18N.t(ssaidActive ? 'status_aid_allpaths' : 'status_aid_apponly');
        metaEl.appendChild(chip);
        metaEl.appendChild(document.createTextNode(' · ' + metaText));
      } else {
        metaEl.textContent = metaText;
      }
      listEl.appendChild(row);
    });
  }

  // ── System-server-spoofs health: ONE aggregate status + module-loaded. Crack-safe (no per-hook). ──
  const SYS_STATE = {
    active: 'sys_active', off: 'sys_off', crashed: 'sys_crashed',
    tripped: 'sys_tripped', reboot: 'sys_mod_reboot', stale: 'sys_stale',
  };
  // css chip class per aggregate state
  const SYS_CLASS = { active: 'active', off: 'off', crashed: 'crashed', tripped: 'tripped', reboot: 'reboot', stale: 'tripped' };
  function renderSysHooks(info) {
    const panel = $s('#sysHook'); if (!panel) return;
    if (!info || info.loaded === 'preview' || !info.sysserver) { panel.hidden = true; return; }
    panel.hidden = false;
    // module-loaded chip (overall "is COPG running this boot")
    const modEl = $s('#sysModule');
    const modKey = info.loaded === 'loaded' ? 'sys_mod_loaded'
                 : info.loaded === 'reboot' ? 'sys_mod_reboot' : 'sys_mod_stale';
    modEl.textContent = I18N.t(modKey);
    modEl.className = 'syshook__module is-' + (info.loaded === 'loaded' ? 'loaded' : info.loaded === 'reboot' ? 'reboot' : 'stale');
    // single aggregate row
    const listEl = $s('#sysHookList');
    listEl.innerHTML = '';
    const row = document.createElement('div');
    row.className = 'syshook__row';
    row.innerHTML = `<span class="syshook__name" data-i18n="glob_sysserver">System-Server Spoofs</span><span class="syshook__chip is-${SYS_CLASS[info.sysserver] || 'off'}"></span>`;
    row.querySelector('.syshook__name').textContent = I18N.t('glob_sysserver');
    row.querySelector('.syshook__chip').textContent = I18N.t(SYS_STATE[info.sysserver] || 'sys_off');
    listEl.appendChild(row);
  }

  async function refresh() {
    const listEl = $s('#statusList');
    listEl.innerHTML = `<div class="status-loading">${I18N.t('status_loading')}</div>`;
    let info = null;
    try { info = await COPG.getSystemHooks(); } catch (_) {}
    renderSysHooks(info);
    let events = [];
    try { events = await COPG.getServed(); } catch (e) { log('status load: ' + e, 'warn'); }
    renderRows(events, info);
  }

  async function clearAll() {
    try { await COPG.clearServed(); } catch (_) {}
    showToast(I18N.t('status_cleared'));
    refresh();
  }

  $s('#statusClose').addEventListener('click', () => closeSheet('statusSheet'));
  $s('#statusRefreshBtn').addEventListener('click', e => { spawnRipple(e, e.currentTarget); refresh(); });
  $s('#statusClearBtn').addEventListener('click', e => { spawnRipple(e, e.currentTarget); clearAll(); });

  function open() { openSheet('statusSheet'); refresh(); log('Hook Status opened.', 'info'); }
  w.HookStatus = { open };
})(window);
