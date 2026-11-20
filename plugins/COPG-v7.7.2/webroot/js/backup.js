/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   backup.js — Backup & Restore bottom-sheet
   Opened from the cloud button in the Library header (#libBackupBtn).
     • Export  → COPG.backupConfig()  (device: writes a timestamped pair to
                 /sdcard/Download/COPG; browser preview: blob downloads).
     • Import  → device: lists backups via COPG.listBackups(), tap to restore;
                 browser preview (or "from file"): a <input type=file> read
                 through COPG.restoreFromText().
   All data work lives in js/copg-data.js; this file is presentation only.
   Built on the shared .bottom-sheet / #sheetOverlay machinery (openSheet/
   closeAllSheets from theme.js); drag-to-dismiss is wired by Gestures.init().
   Designed to grow: add a new .backup-action row + handler for future tools.
   window.Backup.open()
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  /* cloud + up arrow (export) / cloud + down arrow (import) */
  const SVG_CLOUD_UP =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">` +
    `<path d="M7 18h9a4 4 0 0 0 .5-7.97 6 6 0 0 0-11.3-1.2A3.5 3.5 0 0 0 6 18"/>` +
    `<line x1="12" y1="16.6" x2="12" y2="10.4"/><polyline points="9.5 12.9 12 10.2 14.5 12.9"/></svg>`;
  const SVG_CLOUD_DOWN =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">` +
    `<path d="M7 18h9a4 4 0 0 0 .5-7.97 6 6 0 0 0-11.3-1.2A3.5 3.5 0 0 0 6 18"/>` +
    `<line x1="12" y1="10.4" x2="12" y2="16.6"/><polyline points="9.5 14.1 12 16.8 14.5 14.1"/></svg>`;
  const SVG_CHEV =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 6 15 12 9 18"/></svg>`;
  /* down caret for the Import toggle — its menu opens downward, so closed = ▼,
     open = ▲ (CSS rotates it 180° while the list is showing). */
  const SVG_CARET_DOWN =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>`;
  /* circular sync arrows (Sync from GitHub) — spins via .is-busy while pulling */
  const SVG_SYNC =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><polyline points="21 4 21 9.5 15.5 9.5"/><polyline points="3 20 3 14.5 8.5 14.5"/><path d="M5 9a7.5 7.5 0 0 1 12.4-2.8L21 9.5M3 14.5l3.6 3.3A7.5 7.5 0 0 0 19 15"/></svg>`;
  const SVG_ARCHIVE =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="4" rx="1"/><path d="M5 8v11a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8"/><line x1="10" y1="12" x2="14" y2="12"/></svg>`;
  const SVG_FOLDER =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>`;
  const SVG_RESTORE =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 3-6.7"/><polyline points="3 4 3 9 8 9"/></svg>`;
  const SVG_X =
    `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`;
  const SVG_TRASH =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>`;
  /* config = device/grid · names = tag+text — used in the import type chooser */
  const SVG_CONFIG =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2" fill="currentColor" stroke="none"/></svg>`;
  const SVG_NAMES =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M20.6 13.4 13.4 20.6a2 2 0 0 1-2.8 0l-6.2-6.2a2 2 0 0 1-.6-1.4V5a2 2 0 0 1 2-2h7a2 2 0 0 1 1.4.6l6.4 6.4a2 2 0 0 1 0 2.4z"/><circle cx="8" cy="8" r="1.3" fill="currentColor" stroke="none"/></svg>`;
  /* image thumbnail + small refresh arc — "re-download app icons" (spins via .is-busy) */
  const SVG_REFRESH_ICONS =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="6" width="12" height="12" rx="2"/><circle cx="6.8" cy="9.8" r="1.2"/><path d="M3 14.5 6.5 11l3 3 2-2 2.5 2.5"/><path d="M20.5 5.5a4.5 4.5 0 1 0 .4 4"/><polyline points="21 2 21 5.5 17.5 5.5"/></svg>`;

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--backup" id="backupSheet" role="dialog" aria-modal="true" aria-label="Backup &amp; Restore">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="backup_title">Backup &amp; Restore</span>
        <button class="sheet-header__close" id="backupClose" aria-label="Close">${SVG_X}</button>
      </div>
      <div class="sheet-body">
        <p class="backup-intro" data-i18n="backup_intro">Save a copy of your devices &amp; packages, or restore a previous backup.</p>

        <button type="button" class="backup-action" id="backupSyncBtn">
          <span class="backup-action__icon backup-action__icon--sync">${SVG_SYNC}</span>
          <span class="backup-action__text">
            <span class="backup-action__title" data-i18n="backup_sync">Sync from GitHub</span>
            <span class="backup-action__sub" data-i18n="backup_sync_sub">Fetch the latest config from the repo</span>
          </span>
          <span class="backup-action__chev">${SVG_CHEV}</span>
        </button>

        <button type="button" class="backup-action" id="backupRefreshIconsBtn">
          <span class="backup-action__icon backup-action__icon--sync">${SVG_REFRESH_ICONS}</span>
          <span class="backup-action__text">
            <span class="backup-action__title" data-i18n="set_refresh_icons">Refresh App Icons</span>
            <span class="backup-action__sub" data-i18n="set_refresh_icons_sub">Re-download package icons</span>
          </span>
          <span class="backup-action__chev">${SVG_CHEV}</span>
        </button>

        <button type="button" class="backup-action" id="backupExportBtn">
          <span class="backup-action__icon backup-action__icon--up">${SVG_CLOUD_UP}</span>
          <span class="backup-action__text">
            <span class="backup-action__title" data-i18n="backup_export">Export backup</span>
            <span class="backup-action__sub" data-i18n="backup_export_sub">Save current data to Downloads</span>
          </span>
          <span class="backup-action__chev">${SVG_CHEV}</span>
        </button>

        <button type="button" class="backup-action" id="backupImportBtn">
          <span class="backup-action__icon backup-action__icon--down">${SVG_CLOUD_DOWN}</span>
          <span class="backup-action__text">
            <span class="backup-action__title" data-i18n="backup_import">Import &amp; restore</span>
            <span class="backup-action__sub" data-i18n="backup_import_sub">Restore data from a backup</span>
          </span>
          <span class="backup-action__chev backup-action__chev--toggle">${SVG_CARET_DOWN}</span>
        </button>

        <div class="backup-list" id="backupList" hidden></div>
      </div>
    </div>
    <input type="file" id="backupFileInput" hidden />`);

  const $b      = s => document.querySelector(s);
  const sheet   = () => $b('#backupSheet');
  const overlay = () => $b('#sheetOverlay');
  const log = (m, t) => { if (w.logEvent) logEvent(m, t || 'info'); };

  /* Re-render the Library without re-saving (restore already persisted). */
  function repaintLibrary() {
    try {
      if (typeof renderDevices === 'function') renderDevices();
      if (typeof renderPackages === 'function') renderPackages();
      if (typeof updateStats === 'function') updateStats();
      if (typeof updatePreviewBanner === 'function') updatePreviewBanner();
    } catch (e) { console.warn('library repaint:', e); }
  }

  /* "20260612-143052" → "2026-06-12 14:30" */
  function prettyTs(ts) {
    const m = String(ts || '').match(/^(\d{4})(\d{2})(\d{2})-(\d{2})(\d{2})(\d{2})$/);
    return m ? `${m[1]}-${m[2]}-${m[3]} ${m[4]}:${m[5]}` : (ts || '');
  }

  function blobDownload(name, data) {
    try {
      const url = URL.createObjectURL(new Blob([data], { type: 'application/json' }));
      const a = document.createElement('a');
      a.href = url; a.download = name;
      document.body.appendChild(a); a.click(); a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1500);
    } catch (e) { console.warn('blob download:', e); }
  }

  /* ─── Export ─── */
  async function doExport() {
    log('Backup: exporting current config…', 'info');
    try {
      const res = await COPG.backupConfig();
      const names = res.files.map(f => f.name).join(', ');
      if (res.preview) { res.files.forEach(f => blobDownload(f.name, f.data)); log('Backup downloaded in browser: ' + names, 'success'); }
      else             { log('Backup written to ' + res.dir + ': ' + names, 'success'); }
      showToast(I18N.t('toast_backup_ok'));
    } catch (err) {
      showToast(I18N.t('toast_backup_fail'));
      log('Backup failed — ' + err, 'error');
    }
  }

  /* ─── Import ─── */
  async function doImport() {
    if (!COPG.hasBridge()) { $b('#backupFileInput').click(); return; }   // preview → file picker
    const listEl = $b('#backupList');
    const open = !listEl.hidden;
    if (open) { listEl.hidden = true; $b('#backupImportBtn').classList.remove('is-open'); return; }   // tapping again collapses
    listEl.hidden = false;
    $b('#backupImportBtn').classList.add('is-open');                      // caret ▼ → ▲
    listEl.innerHTML = '<div class="backup-loading"><span class="spin"></span></div>';
    let entries = [];
    try { entries = await COPG.listBackups(); }
    catch (err) { log('Backup: listing failed — ' + err, 'warn'); }
    renderBackupList(entries);
  }

  /* ─── Sync from GitHub ─── */
  async function doSync() {
    const btn = $b('#backupSyncBtn');
    if (btn.classList.contains('is-busy')) return;                 // guard double-tap
    if (!COPG.hasBridge()) { showToast(I18N.t('toast_sync_preview')); log('Sync needs the device bridge — unavailable in preview.', 'warn'); return; }
    btn.classList.add('is-busy');
    log('Sync: pulling COPG.json + list.json from GitHub…', 'info');
    try {
      const res = await COPG.syncFromGitHub();
      repaintLibrary();
      showToast(I18N.t('toast_sync_ok'));
      log('Synced ' + ((res.synced || []).join(' + ') || '—') + ' from GitHub.', 'success');
      closeAllSheets();
    } catch (err) {
      showToast(I18N.t('toast_sync_fail'));
      log('Sync failed — ' + err, 'error');
    } finally {
      btn.classList.remove('is-busy');
    }
  }

  /* ─── Refresh app icons ───
     Wipe the webroot/icons cache + in-memory state, then repaint the Library so
     every package icon re-downloads fresh (covers a game changing its icon). */
  async function doRefreshIcons() {
    const btn = $b('#backupRefreshIconsBtn');
    if (btn.classList.contains('is-busy')) return;                 // guard double-tap
    if (!COPG.hasBridge()) { showToast(I18N.t('toast_icons_refresh_preview')); log('Icon refresh needs the device bridge — unavailable in preview.', 'warn'); return; }
    btn.classList.add('is-busy');
    showToast(I18N.t('toast_icons_refreshing'));
    log('Refresh: wiping cached icons + re-downloading…', 'info');
    try {
      if (w.Icons && Icons.refresh) await Icons.refresh();
      repaintLibrary();                                            // re-runs Icons.load on new rows
      showToast(I18N.t('toast_icons_refreshed'));
      log('App icons refreshed.', 'success');
    } catch (err) {
      showToast(I18N.t('toast_restore_fail'));
      log('Icon refresh failed — ' + err, 'error');
    } finally {
      btn.classList.remove('is-busy');
    }
  }

  function renderBackupList(entries) {
    const listEl = $b('#backupList');
    listEl.innerHTML = '';
    if (!entries.length) {
      const empty = document.createElement('div');
      empty.className = 'backup-empty';
      empty.textContent = I18N.t('backup_empty');
      listEl.appendChild(empty);
    } else {
      entries.forEach(entry => listEl.appendChild(buildEntryRow(entry)));
    }
    listEl.appendChild(buildFileRow());            // always offer "import from file…"
  }

  function buildEntryRow(entry) {
    const title = entry.ts ? prettyTs(entry.ts) : (entry.file || '');
    const parts = [];
    if (entry.configFile) parts.push('COPG.json');
    if (entry.listFile)   parts.push('list.json');
    if (entry.file)       parts.push(entry.file);

    /* row = restore (whole-row tap) + a separate trash button. They're kept as
       siblings in a wrapper — a <button> inside a <button> is invalid HTML. */
    const wrap = document.createElement('div');
    wrap.className = 'backup-row-wrap';

    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'backup-row';
    row.innerHTML =
      `<span class="backup-row__icon">${SVG_ARCHIVE}</span>` +
      `<span class="backup-row__text"><span class="backup-row__title"></span><span class="backup-row__sub"></span></span>` +
      `<span class="backup-row__go">${SVG_RESTORE}</span>`;
    row.querySelector('.backup-row__title').textContent = title;
    row.querySelector('.backup-row__sub').textContent   = parts.join(' · ');
    row.addEventListener('click', () => confirmRestore(entry, title));

    const del = document.createElement('button');
    del.type = 'button';
    del.className = 'backup-row__del';
    del.setAttribute('aria-label', I18N.t('backup_delete'));
    del.innerHTML = SVG_TRASH;
    del.addEventListener('click', e => { e.stopPropagation(); confirmDelete(entry, title); });

    wrap.appendChild(row);
    wrap.appendChild(del);
    return wrap;
  }

  function confirmDelete(entry, title) {
    const run = async () => {
      try {
        const res = await COPG.deleteBackup(entry);
        log('Deleted backup ' + title + ' (' + ((res.removed || []).join(', ') || '—') + ')', 'success');
        showToast(I18N.t('toast_backup_deleted'));
        let entries = [];
        try { entries = await COPG.listBackups(); } catch (err) { log('Backup: re-listing failed — ' + err, 'warn'); }
        renderBackupList(entries);                          // repaint list in place
      } catch (err) {
        showToast(I18N.t('toast_restore_fail'));
        log('Delete backup failed — ' + err, 'error');
      }
    };
    if (w.Modals && Modals.confirmDelete) {
      Modals.confirmDelete(I18N.t('backup_delete_title'), I18N.t('backup_delete_msg') + '\n' + title, run);
    } else run();
  }

  function buildFileRow() {
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'backup-row backup-row--browse';
    row.innerHTML =
      `<span class="backup-row__icon">${SVG_FOLDER}</span>` +
      `<span class="backup-row__text"><span class="backup-row__title"></span></span>`;
    row.querySelector('.backup-row__title').textContent = I18N.t('backup_from_file');
    row.addEventListener('click', () => $b('#backupFileInput').click());
    return row;
  }

  function confirmRestore(entry, title) {
    const run = async () => {
      try {
        const res = await COPG.restoreBackup(entry);
        repaintLibrary();
        showToast(I18N.t(res.preview ? 'toast_preview_only' : 'toast_restore_ok'));
        log('Restored ' + ((res.restored || []).join(' + ') || '—') + ' from ' + title, 'success');
        closeAllSheets();
      } catch (err) {
        showToast(I18N.t('toast_restore_fail'));
        log('Restore failed — ' + err, 'error');
      }
    };
    if (w.Modals && Modals.confirm) {
      Modals.confirm({
        title:   I18N.t('backup_confirm_title'),
        message: I18N.t('backup_confirm_msg') + '\n' + title,
        okLabel: I18N.t('backup_restore'),
        onOk:    run,
      });
    } else run();
  }

  /* File <input> path (browser preview + "import from file…").
     Any *.json works — we classify by CONTENT, not the filename, then let the
     user confirm/override the type before it overwrites anything. */
  $b('#backupFileInput').addEventListener('change', e => {
    const file = e.target.files && e.target.files[0];
    e.target.value = '';                            // allow re-picking the same file
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result || '');
      let info;
      try { info = COPG.inspectBackup(text); }       // throws on bad JSON / non-object
      catch (err) { showToast(I18N.t('toast_invalid_json')); log('Import: not a valid JSON file — ' + err, 'error'); return; }
      promptKind(text, file.name, info);             // user picks config / names (detected pre-marked)
    };
    reader.onerror = () => { showToast(I18N.t('toast_restore_fail')); log('File read failed.', 'error'); };
    reader.readAsText(file);
  });

  /* Show the "what kind of file is this?" chooser in the list region. The
     detected type is pre-badged; either choice restores with that exact kind so
     a wrong auto-guess can never silently overwrite the other file. */
  function promptKind(text, fileName, info) {
    const listEl = $b('#backupList');
    listEl.hidden = false;
    $b('#backupImportBtn').classList.add('is-open');                      // caret ▲ while chooser shows
    listEl.innerHTML = '';
    const head = document.createElement('div');
    head.className = 'backup-kind-head';
    head.textContent = I18N.t('backup_kind_title');
    listEl.appendChild(head);
    const fileTag = document.createElement('div');
    fileTag.className = 'backup-kind-file';
    fileTag.textContent = fileName;
    listEl.appendChild(fileTag);
    listEl.appendChild(buildKindRow(text, fileName, 'config', info.kind === 'config'));
    listEl.appendChild(buildKindRow(text, fileName, 'list',   info.kind === 'list'));
    try { listEl.scrollIntoView({ block: 'nearest', behavior: 'smooth' }); } catch (_) {}
  }

  function buildKindRow(text, fileName, kind, detected) {
    const isCfg = kind === 'config';
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'backup-row backup-kind-row';
    row.innerHTML =
      `<span class="backup-row__icon">${isCfg ? SVG_CONFIG : SVG_NAMES}</span>` +
      `<span class="backup-row__text"><span class="backup-row__title"></span><span class="backup-row__sub"></span></span>` +
      (detected ? `<span class="backup-kind-badge"></span>` : `<span class="backup-row__go">${SVG_CHEV}</span>`);
    row.querySelector('.backup-row__title').textContent = I18N.t(isCfg ? 'backup_kind_config' : 'backup_kind_list');
    row.querySelector('.backup-row__sub').textContent   = I18N.t(isCfg ? 'backup_kind_config_sub' : 'backup_kind_list_sub');
    if (detected) row.querySelector('.backup-kind-badge').textContent = I18N.t('backup_kind_detected');
    row.addEventListener('click', () => doRestoreFromText(text, fileName, kind));
    return row;
  }

  async function doRestoreFromText(text, fileName, kind) {
    try {
      const res = await COPG.restoreFromText(text, kind);
      repaintLibrary();
      showToast(I18N.t(res.preview ? 'toast_preview_only' : 'toast_restore_ok'));
      log('Restored ' + kind + ' from file ' + fileName, 'success');
      closeAllSheets();
    } catch (err) {
      showToast(I18N.t('toast_restore_fail'));
      log('Restore from file failed — ' + err, 'error');
    }
  }

  $b('#backupClose').addEventListener('click', () => closeAllSheets());
  $b('#backupExportBtn').addEventListener('click', e => { spawnRipple(e, e.currentTarget); doExport(); });
  $b('#backupImportBtn').addEventListener('click', e => { spawnRipple(e, e.currentTarget); doImport(); });
  $b('#backupSyncBtn').addEventListener('click',   e => { spawnRipple(e, e.currentTarget); doSync(); });
  $b('#backupRefreshIconsBtn').addEventListener('click', e => { spawnRipple(e, e.currentTarget); doRefreshIcons(); });
  $b('#libBackupBtn')?.addEventListener('click', e => { spawnRipple(e, e.currentTarget); open(); });

  function open() {
    const listEl = $b('#backupList');
    if (listEl) { listEl.hidden = true; listEl.innerHTML = ''; }   // reset to the two actions
    $b('#backupImportBtn')?.classList.remove('is-open');           // caret back to ▼
    openSheet('backupSheet');
    log('Backup & Restore opened.', 'info');
  }

  w.Backup = { open };
})(window);
