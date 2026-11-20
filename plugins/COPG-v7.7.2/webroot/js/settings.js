/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   settings.js — Developer toggles + About card (module info)
   (theme/lang in theme.js)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
const debugToggle = $('#debugToggle');

if (debugToggle) {
  // Restore persisted state into BOTH the checkbox and State (so the Console's
  // debug gate is correct from first paint, not only after a manual toggle).
  try { debugToggle.checked = localStorage.getItem('copg-debug') === '1'; } catch(_) {}
  State.debugLogs = debugToggle.checked;
  debugToggle.addEventListener('change', () => {
    State.debugLogs = debugToggle.checked;
    showToast(I18N.t(debugToggle.checked ? 'toast_debug_on' : 'toast_debug_off'));
    try { localStorage.setItem('copg-debug', debugToggle.checked ? '1' : '0'); } catch(_) {}
    // Always-visible confirmation (console.warn is ungated by the debug gate).
    try {
      console.warn(debugToggle.checked
        ? '[debug] Debug Logs ON — UI activity is now logged here.'
        : '[debug] Debug Logs OFF.');
    } catch(_) {}
  });
}

/* ─── Fullscreen (immersive) toggle ───
   ON = fullscreen (status bar hidden), OFF = status bar shown. Default ON (like
   ReZygisk's WebUI). Persisted as `copg-fullscreen`; applied via the KSU bridge
   (`COPG.setFullscreen` → `ksu.fullScreen`) both now (parse time) and on change.
   No-op in browser preview (no bridge). */
const fsToggle = $('#fullscreenToggle');

if (fsToggle) {
  let on = true; // default fullscreen ON
  try { const v = localStorage.getItem('copg-fullscreen'); if (v !== null) on = v === '1'; } catch(_) {}
  fsToggle.checked = on;
  State.fullscreen = on;
  try { COPG.setFullscreen(on); } catch(_) {}   // apply persisted pref at startup
  fsToggle.addEventListener('change', () => {
    State.fullscreen = fsToggle.checked;
    try { COPG.setFullscreen(fsToggle.checked); } catch(_) {}
    try { localStorage.setItem('copg-fullscreen', fsToggle.checked ? '1' : '0'); } catch(_) {}
    showToast(I18N.t(fsToggle.checked ? 'toast_fullscreen_on' : 'toast_fullscreen_off'));
  });
}

/* ─── Toggle fix + always-on report ───
   The switch (`label.toggle`) is a small tap target on the row's right edge, so
   tapping the row's icon/label did nothing — that's why toggles felt dead. Make
   the WHOLE `.setting-row--toggle` flip its checkbox (taps that land on the
   switch are left to the native checkbox so we don't double-toggle).
   `console.warn` is captured by the in-app Console UNGATED (visible even with
   Debug Logs off), so these lines are an always-on debugging trail. */
function toggleReport(msg) { try { console.warn('[toggle] ' + msg); } catch (_) {} }

$$('.setting-row--toggle').forEach(row => {
  row.addEventListener('click', e => {
    if (e.target.closest('label.toggle')) return;   // tap on the switch → native checkbox handles it
    const cb = row.querySelector('input[type="checkbox"]');
    if (!cb) return;
    cb.checked = !cb.checked;                        // tap on the row text → flip it ourselves
    cb.dispatchEvent(new Event('change', { bubbles: true }));
  });
});

// One ungated line per actual toggle change (visible even with Debug Logs off).
document.addEventListener('change', e => {
  const cb = e.target;
  if (cb && cb.matches && cb.matches('.toggle input[type="checkbox"]'))
    toggleReport((cb.id || '?') + ' = ' + cb.checked);
}, true);

/* ─── About card expand/collapse ─── */
const aboutCard = $('#aboutCard');
aboutCard?.addEventListener('click', () => aboutCard.classList.toggle('expanded'));

/* ─── Populate About rows (module info — real data from module.prop) ─── */
function populateAbout() {
  const si = State.sysInfo;
  const ver = si.version || '';
  const set = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
  set('aboutVersion',     ver ? 'v' + ver : '—');
  set('aboutVersion2',    ver || '—');
  set('aboutVersionCode', si.versionCode || '—');
  set('aboutAuthor',      si.author || '—');
  // Description stays the localized about_desc (data-i18n) — not module.prop's short text.
}
populateAbout();

/* ─── Advertising ID (GAID) — FREE global spoof (root rewrite of GMS adid_key) ─── */
async function refreshGaid() {
  const hint = $('#gaidHint');
  if (!hint) return;
  if (!COPG.hasBridge()) { hint.textContent = I18N.t('set_gaid_preview'); return; }
  try {
    const id = await COPG.getAdvertisingId();
    hint.textContent = id ? id.slice(0, 8) + '…' : I18N.t('set_gaid_none');
  } catch (_) { hint.textContent = '—'; }
}
const gaidRow = $('#gaidRow');
if (gaidRow) {
  gaidRow.addEventListener('click', () => {
    if (!COPG.hasBridge()) { showToast(I18N.t('set_gaid_preview')); return; }
    // Full sheet: view current, set custom, generate random, or restore the real id.
    if (window.GaidSheet) GaidSheet.open(() => refreshGaid());
  });
  refreshGaid();
}

/* ─── Hook Status: device-wide "served spoofs" board ─── */
const statusRow = $('#statusRow');
if (statusRow) {
  statusRow.addEventListener('click', () => { if (window.HookStatus) HookStatus.open(); });
}
// Integrations — the module-helpers dashboard (NoMount, zygisk, root manager).
const helpersRow = $('#helpersRow');
if (helpersRow) {
  const openHelpers = () => { if (window.Helpers) Helpers.open(); };
  helpersRow.addEventListener('click', openHelpers);
  helpersRow.addEventListener('keydown', e => {
    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); openHelpers(); }
  });
}
// Open Console — moved off Home into Settings → Developer (debug tool, not a primary action).
const consoleRow = $('#consoleRow');
if (consoleRow) {
  const openC = () => { if (window.Console && typeof Console.open === 'function') Console.open(); };
  consoleRow.addEventListener('click', openC);
  consoleRow.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); openC(); } });
}

/* ─── Global Hooks — device-wide IMEI: collapsed row opens GlobalImeiSheet ─── */
(function initGlobalHooks() {
  const row = $('#globImeiRow'), hint = $('#globImeiHint');
  if (!row) return;
  async function refresh() {
    try {
      let cfg = COPG.getGlobalConfig && COPG.getGlobalConfig();
      if ((!cfg || (!cfg.enabled && !cfg.imei)) && COPG.loadGlobalConfig) cfg = await COPG.loadGlobalConfig();
      if (!hint) return;
      hint.textContent = (cfg && cfg.enabled && cfg.imei) ? (cfg.imei.slice(0, 6) + '…') : I18N.t('glob_off');
    } catch (_) { if (hint) hint.textContent = '—'; }
  }
  row.addEventListener('click', () => {
    if (window.GlobalImeiSheet) GlobalImeiSheet.open(() => refresh());
  });
  refresh();

  // system_server master state (global.json sysserver) — FLAG_SECURE runs in system_server, so it's
  // gated by the master too; grey + block it when the master is off, same as the modal rows.
  function ssOn() { try { return !!(COPG.getGlobalConfig && COPG.getGlobalConfig().sysserver); } catch (_) { return false; } }
  // ARMED = master on AND a real reboot has happened since the toggle (system_server hooks only
  // install at boot). Dependents grey on this, NOT on ssOn(), so flipping the master on leaves them
  // greyed/"reboot to apply" until the device actually restarts — the requested UX.
  function ssArmed() { try { return !!(COPG.sysserverArmed && COPG.sysserverArmed()); } catch (_) { return ssOn(); } }
  // Add a persistent ⟳ reboot marker to a Settings row's label (idempotent).
  function markReboot(row) {
    if (!row || row.querySelector('.setting-row__reboot')) return;
    const m = document.createElement('span');
    m.className = 'setting-row__reboot'; m.textContent = ' ⟳';
    m.style.cssText = 'color:var(--warn,#F59E0B);font-size:12px;opacity:.85';
    try { m.title = I18N.t('sysserver_reboot_mark'); } catch (_) {}
    const lbl = row.querySelector('.setting-row__label'); if (lbl) lbl.appendChild(m);
  }

  /* ── System-Server state chips — same visual language as the Hook Status board ──
     The master row carries a live state chip (Reboot to activate → Active); each dependent shows a
     "Reboot to activate" chip while the master is on-but-not-yet-rebooted. Reuses the board's labels
     (sys_mod_reboot / sys_active) and the .state-chip styles (css/settings.css) so the two agree. */
  function ensureChip(rowId, beforeSel) {
    const row = document.getElementById(rowId); if (!row) return null;
    let chip = row.querySelector('.state-chip');
    if (!chip) {
      chip = document.createElement('span'); chip.className = 'state-chip'; chip.hidden = true;
      const ref = beforeSel ? row.querySelector(beforeSel) : null;
      if (ref) row.insertBefore(chip, ref); else row.appendChild(chip);
    }
    return chip;
  }
  function setChip(chip, show, cls, key) {
    if (!chip) return;
    chip.hidden = !show;
    chip.className = 'state-chip' + (cls ? ' ' + cls : '');
    if (show) { try { chip.textContent = I18N.t(key); } catch (_) { chip.textContent = ''; } }
  }
  function paintSsState() {
    const on = ssOn(), armed = ssArmed();
    // master row: chip only when ON (the toggle itself reads OFF otherwise) — pending vs armed
    const mc = ensureChip('sysServerRow', '.toggle');
    setChip(mc, on, armed ? 'is-active' : 'is-reboot', armed ? 'sys_active' : 'sys_mod_reboot');
    // dependent toggle (FLAG_SECURE): "Reboot to activate" chip while pending; greyed until armed
    const fc = ensureChip('flagSecureRow', '.toggle');
    setChip(fc, on && !armed, 'is-reboot', 'sys_mod_reboot');
    const fr = document.getElementById('flagSecureRow'); if (fr) fr.style.opacity = armed ? '' : '.5';
    // install-patches row: tint its hint accent while pending (it already shows "Reboot needed")
    const ih = document.getElementById('installPatchHint');
    if (ih) ih.classList.toggle('is-reboot-hint', on && !armed);
  }

  /* Disable FLAG_SECURE — device-wide (global.json "flagsecure"). Runs in system_server → gated by
     the master "System-Server Spoofs" switch: greyed + blocked (turn master on + reboot first) when
     the master is off. Once the master is on + booted, enabling pops the screenshot/DRM confirm and
     the resident hook picks up the live file within ~1.5s. */
  const fsCb = $('#flagSecureToggle');
  const fsRow = $('#flagSecureRow');
  if (fsCb) {
    let syncing = false;
    async function syncFs() {
      try {
        let cfg = (COPG.hasBridge() && COPG.loadGlobalConfig)
          ? await COPG.loadGlobalConfig()
          : (COPG.getGlobalConfig && COPG.getGlobalConfig());
        syncing = true; fsCb.checked = !!(cfg && cfg.flagsecure); syncing = false;
        paintSsState();   // grey + "Reboot to activate" chip (replaces the bare ⟳ marker)
      } catch (_) {}
    }
    async function saveFs(on) {
      try { await COPG.saveGlobalConfig({ flagsecure: on }); showToast(I18N.t(on ? 'toast_flagsecure_on' : 'toast_flagsecure_off')); }
      catch (e) { showToast(String(e)); await syncFs(); }
    }
    fsCb.addEventListener('change', () => {
      if (syncing) return;
      if (!COPG.hasBridge()) { showToast(I18N.t('set_gaid_preview')); syncing = true; fsCb.checked = false; syncing = false; return; }
      if (fsCb.checked && !ssOn()) {   // master off → block + guide to the master switch
        syncing = true; fsCb.checked = false; syncing = false;
        Modals.confirm({ title: I18N.t('sysserver_needed_title'), message: I18N.t('sysserver_needed_msg'), okLabel: I18N.t('sysserver_warn_ok'), infoOnly: true });
        return;
      }
      if (fsCb.checked && !ssArmed()) {   // master on but not rebooted yet → not live until restart
        syncing = true; fsCb.checked = false; syncing = false;
        Modals.confirm({ title: I18N.t('sysserver_pending_title'), message: I18N.t('sysserver_pending_msg'), okLabel: I18N.t('sysserver_warn_ok'), infoOnly: true });
        return;
      }
      if (fsCb.checked) {
        // Uncheck-then-confirm (dismissing the heads-up leaves it OFF), same idiom as the risk gates.
        syncing = true; fsCb.checked = false; syncing = false;
        Modals.confirm({
          title:   I18N.t('flagsecure_warn_title'),
          message: I18N.t('flagsecure_warn_msg'),
          okLabel: I18N.t('flagsecure_warn_ok'),
          onOk: () => { syncing = true; fsCb.checked = true; syncing = false; saveFs(true); },
        });
      } else {
        saveFs(false);
      }
    });
    syncFs();
  }

  /* No Storage Restrict — device-wide on/off (global.json "storage"). Benign (only lifts the SAF
     folder-picker block in ExternalStorageProvider), so no risk gate: plain toggle → save. The
     StorageHook re-reads the live file within ~1.5s (no reboot). */
  const stCb = $('#storageToggle');
  if (stCb) {
    let stSyncing = false;
    async function syncSt() {
      try {
        let cfg = (COPG.hasBridge() && COPG.loadGlobalConfig)
          ? await COPG.loadGlobalConfig()
          : (COPG.getGlobalConfig && COPG.getGlobalConfig());
        stSyncing = true; stCb.checked = !!(cfg && cfg.storage); stSyncing = false;
      } catch (_) {}
    }
    stCb.addEventListener('change', async () => {
      if (stSyncing) return;
      if (!COPG.hasBridge()) { showToast(I18N.t('set_gaid_preview')); stSyncing = true; stCb.checked = false; stSyncing = false; return; }
      const on = stCb.checked;
      try { await COPG.saveGlobalConfig({ storage: on }); showToast(I18N.t(on ? 'toast_storage_on' : 'toast_storage_off')); }
      catch (e) { showToast(String(e)); await syncSt(); }
    });
    syncSt();
  }

  /* Concurrent Mic — device-wide (global.json "micshare"). Lets two apps use the mic at once (e.g. a
     game's voice chat + a screen recorder). Applied by a hook in audioserver (injected at boot by the
     module); the WebUI just flips the live file, which the hook polls (~1.5s, no reboot). Plain toggle,
     no risk gate (out-of-process, not app-detectable; benign for the user). */
  const mcCb = $('#micShareToggle');
  if (mcCb) {
    let mcSyncing = false;
    async function syncMc() {
      try {
        let cfg = (COPG.hasBridge() && COPG.loadGlobalConfig)
          ? await COPG.loadGlobalConfig()
          : (COPG.getGlobalConfig && COPG.getGlobalConfig());
        mcSyncing = true; mcCb.checked = !!(cfg && cfg.micshare); mcSyncing = false;
      } catch (_) {}
    }
    mcCb.addEventListener('change', async () => {
      if (mcSyncing) return;
      if (!COPG.hasBridge()) { showToast(I18N.t('set_gaid_preview')); mcSyncing = true; mcCb.checked = false; mcSyncing = false; return; }
      const on = mcCb.checked;
      try { await COPG.saveGlobalConfig({ micshare: on }); showToast(I18N.t(on ? 'toast_micshare_on' : 'toast_micshare_off')); }
      catch (e) { showToast(String(e)); await syncMc(); }
    });
    syncMc();
  }

  /* Install Patches — one grouped row → opens the sheet (installpatchessheet.js) holding the four
     package-manager install-time patches (sig / downgrade / verifier / arsc). They all hook the
     installer inside system_server, so they live together, gated by the "System-Server Spoofs"
     master; the sheet handles the master-gate note + the sig/verifier warn. Row hint shows how many
     are on. */
  const ipRow = $('#installPatchRow'), ipHint = $('#installPatchHint');
  async function refreshIp() {
    try {
      // Always pull the persisted config first. The in-memory default carries a truthy-but-empty
      // corepatch object, so the old "load only if !cfg.corepatch" guard never refreshed — the row
      // showed "Off" on a fresh open even when patches were on. Mirror syncSs(): await the load.
      if (COPG.hasBridge && COPG.hasBridge() && COPG.loadGlobalConfig) await COPG.loadGlobalConfig();
      if (ipHint && window.InstallPatchesSheet) ipHint.textContent = InstallPatchesSheet.hintLabel();
      paintSsState();   // tint the hint accent while the master is pending a reboot
    } catch (_) { if (ipHint) ipHint.textContent = '—'; }
  }
  if (ipRow) {
    ipRow.addEventListener('click', () => { if (window.InstallPatchesSheet) InstallPatchesSheet.open(() => refreshIp()); });
    refreshIp();
  }

  /* System-Server Spoofs — MASTER stability switch (global.json "sysserver"), default OFF. When off,
     the module arms NONE of the system_server hooks (VPN-hide, per-app locale, FLAG_SECURE, Play
     install-source, PairIP block) at boot, so a fresh/updated install can never bootloop. Because
     those hooks only install at boot, toggling this REQUIRES A REBOOT to take effect — the confirm
     says so. This is the stability gate; individual system_server spoofs still have their own
     boot-crash guards underneath. */
  const ssCb = $('#sysServerToggle');
  if (ssCb) {
    let ssSyncing = false;
    async function syncSs() {
      try {
        let cfg = (COPG.hasBridge() && COPG.loadGlobalConfig)
          ? await COPG.loadGlobalConfig()
          : (COPG.getGlobalConfig && COPG.getGlobalConfig());
        ssSyncing = true; ssCb.checked = !!(cfg && cfg.sysserver); ssSyncing = false;
        paintSsState();
      } catch (_) {}
    }
    async function saveSs(on) {
      try {
        await COPG.saveGlobalConfig({ sysserver: on }); showToast(I18N.t('toast_sysserver_reboot'));
        // Repaint the state chips: turning the master ON flips its chip to "Reboot to activate" and
        // leaves the dependents greyed with their own pending chip until a real reboot arms the layer.
        paintSsState();
      } catch (e) { showToast(String(e)); await syncSs(); }
    }
    ssCb.addEventListener('change', () => {
      if (ssSyncing) return;
      if (!COPG.hasBridge()) { showToast(I18N.t('set_gaid_preview')); ssSyncing = true; ssCb.checked = false; ssSyncing = false; return; }
      const on = ssCb.checked;
      // Both directions need a reboot to apply (the hooks install/skip only at boot); confirm either way.
      ssSyncing = true; ssCb.checked = !on; ssSyncing = false;   // revert until confirmed
      Modals.confirm({
        title:   I18N.t('sysserver_warn_title'),
        message: I18N.t(on ? 'sysserver_warn_on_msg' : 'sysserver_warn_off_msg'),
        okLabel: I18N.t('sysserver_warn_ok'),
        onOk: () => { ssSyncing = true; ssCb.checked = on; ssSyncing = false; saveSs(on); },
      });
    });
    syncSs();
  }

  // Repaint the state chips whenever the Device tab is opened — keeps them correct after a language
  // change (labels re-translate) or any config drift, without polling.
  document.querySelectorAll('.nav-item[data-page="device"]').forEach(n =>
    n.addEventListener('click', () => setTimeout(paintSsState, 0)));
})();
