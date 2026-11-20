/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   boot.js — Startup orchestration (runs last)
   Loads COPG.json / list.json, reads real device/system info via the
   bridge, paints the UI, then asynchronously folds in the installed-app
   set (for "installed" badges + filter) once it resolves.
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function () {
  function repaintLibrary() {
    if (typeof renderDevices === 'function')       renderDevices();
    if (typeof renderPackages === 'function')      renderPackages();
    if (typeof updateStats === 'function')         updateStats();
    if (typeof updatePreviewBanner === 'function') updatePreviewBanner();
  }

  function applySystemInfo(info) {
    if (!info) return;
    const si = State.sysInfo;
    si.androidVer = info.androidVer || si.androidVer;
    si.abi        = info.abi        || si.abi;
    si.gpu        = info.gpu        || si.gpu;
    si.kernel     = info.kernel     || si.kernel;
    si.zygisk     = info.zygiskLabel || si.zygisk;
    si.rootVersionLabel = info.rootVersionLabel || si.rootVersionLabel;
    si.rootVersion      = info.rootVersion || si.rootVersion;
    if (info.version)     { si.version = info.version; }
    if (info.versionCode) { si.versionCode = info.versionCode; }
    if (info.author)      { si.author = info.author; }
    if (info.description) { si.description = info.description; }
    if (info.rootEnv)     { State.rootEnv = info.rootEnv; }
    State.rootSecondary = info.secondary || [];
    if (typeof info.moduleActive === 'boolean') State.moduleActive = info.moduleActive;
    if (info.moduleStatus) State.moduleStatus = info.moduleStatus;

    if (typeof populateSysInfo === 'function') populateSysInfo();
    if (typeof populateAbout === 'function')   populateAbout();
    if (typeof updateHeroStatus === 'function') updateHeroStatus();
    if (typeof updateHeroVersion === 'function') updateHeroVersion();
  }

  async function boot() {
    const log = (m, t) => { if (window.logEvent) logEvent(m, t); };
    log('Boot: ' + (window.COPG && COPG.hasBridge() ? 'bridge available' : 'no bridge (preview)'), 'info');
    try {
      if (window.COPG && typeof COPG.loadAll === 'function') await COPG.loadAll();
      log('Config loaded: ' + COPG.listDevices().length + ' devices, ' + COPG.listPackages().length + ' packages.', 'success');
    } catch (err) {
      console.error('COPG.loadAll failed:', err);
    }
    repaintLibrary();
    if (window.I18N && typeof I18N.apply === 'function') I18N.apply();

    // Real system/device info (bridge only; preview keeps placeholders).
    if (window.COPG && typeof COPG.getSystemInfo === 'function') {
      COPG.getSystemInfo().then(info => {
        applySystemInfo(info);
        if (info) log(`System: Android ${info.androidVer} · ${info.rootVersionLabel} · ${info.zygiskLabel} · kernel ${info.kernel}`, 'info');
      }).catch(() => {});
    }

    // Installed-app set → repaint Library so "installed" badges/filter appear. Then pull the system
    // desktop names for clone entries (incl. the number suffix, e.g. "DevCheck 1") and repaint again.
    if (window.COPG && typeof COPG.getInstalledPackages === 'function') {
      COPG.getInstalledPackages().then(set => {
        repaintLibrary(); log('Installed apps indexed: ' + (set ? set.size : 0) + '.', 'info');
        if (typeof COPG.loadCloneNames === 'function')
          COPG.loadCloneNames().then(() => repaintLibrary()).catch(() => {});
      }).catch(() => {});
    }

    // Make any bottom-sheet (incl. injected modals) drag-to-dismiss.
    if (window.Gestures && typeof Gestures.init === 'function') Gestures.init();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
