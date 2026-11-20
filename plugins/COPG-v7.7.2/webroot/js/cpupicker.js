/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   cpupicker.js — "Choose a CPU profile" bottom-sheet
   Parallel to devicepicker.js but for CPU model profiles (COPG.getCpuModels()):
   search + tappable rows (chip avatar + SoC name + key), current selection marked.
   The model list is the shipped CPU/manifest.json (device) or the built-in fallback
   (preview). Selecting a row calls back with {key, name}.
   window.CpuPicker.open(currentKey, cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const CHIP_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="7" y="7" width="10" height="10" rx="1.5"/><path d="M9 2v3M15 2v3M9 19v3M15 19v3M2 9h3M2 15h3M19 9h3M19 15h3"/></svg>';
  const CHECK_SVG = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
  const MISS_SVG  = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>';
  const TRASH_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="cpuPickerSheet" role="dialog" aria-modal="true" aria-label="CPU profiles">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="cpupicker_title">CPU Profiles</span>
        <button class="sheet-header__close" id="cpClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-searchrow">
        <div class="picker-search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="search" id="cpSearch" autocomplete="off" data-i18n-attr="placeholder:cpupicker_search" placeholder="Search CPUs…" />
        </div>
        <button type="button" class="picker-add" id="cpImportBtn" data-i18n-attr="aria-label:cpu_import_btn" aria-label="Import custom CPU">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
      <div class="picker-import__panel" id="cpImportPanel" hidden>
        <div class="picker-import__title" data-i18n="cpu_import_btn">Import custom CPU…</div>
        <label class="field"><span class="field__label" data-i18n="cpu_import_name_lbl">Profile name</span>
        <input type="text" id="cpImportName" class="picker-import__name" autocomplete="off" data-i18n-attr="placeholder:cpu_import_name_ph" placeholder="e.g. My SD8 Elite" /></label>
        <div class="picker-import__filerow">
          <button type="button" class="picker-import__filebtn" id="cpImportFile" data-i18n="cpu_import_choose">Choose file…</button>
          <span class="picker-import__fname" id="cpImportFileName" data-i18n="cpu_import_nofile">No file selected</span>
        </div>
        <div class="picker-import__err" id="cpImportErr" hidden></div>
        <button type="button" class="picker-import__save" id="cpImportSave" data-i18n="cpu_import_save">Save profile</button>
      </div>
      <input type="file" id="cpFileInput" hidden />
      <div class="picker-body">
        <div class="picker-list" id="cpList"></div>
        <div class="picker-empty" id="cpEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8.5" y1="11" x2="13.5" y2="11"/></svg>
          <span id="cpEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $c = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('cpuPickerSheet');

  let cb = null;
  let rowData = [];            // [{el, search}]
  let query = '';
  let selectedKey = null;

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $c('#cpClose').addEventListener('click', close);

  function showEmpty(msg) { $c('#cpEmptyMsg').textContent = msg; $c('#cpEmpty').hidden = false; }
  function hideEmpty() { $c('#cpEmpty').hidden = true; }

  async function render() {
    const list = $c('#cpList');
    list.innerHTML = '';
    rowData = [];
    const models = (w.COPG && COPG.getCpuModels) ? await COPG.getCpuModels() : [];
    if (!models.length) { showEmpty(I18N.t('cpupicker_empty')); return; }
    // Which profiles actually have a cpuinfo file on disk (null = couldn't check → all ok).
    const fileKeys = (w.COPG && COPG.cpuFileKeys) ? await COPG.cpuFileKeys() : null;
    hideEmpty();
    const frag = document.createDocumentFragment();
    models.forEach(m => {
      // Highlight the active model; with no explicit key the default is Snapdragon (sd8elite).
      const isSel = (m.key === selectedKey) || (!selectedKey && m.key === 'sd8elite');
      const missing = !!(fileKeys && !fileKeys.has(m.key));   // manifest entry, no file
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'app-row dev-row';
      row.dataset.key = m.key;
      row.classList.toggle('is-selected', isSel);
      row.classList.toggle('app-row--missing', missing);
      row.innerHTML =
        `<span class="app-row__icon dev-row__avatar">${CHIP_SVG}</span>` +
        `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span></span>` +
        (missing ? `<span class="app-row__warn" data-i18n-attr="aria-label:cpu_missing" aria-label="File missing">${MISS_SVG}</span>` : '') +
        `<span class="app-row__del" role="button" tabindex="0" data-i18n-attr="aria-label:profile_delete" aria-label="Delete profile">${TRASH_SVG}</span>` +
        `<span class="dev-row__check">${CHECK_SVG}</span>`;
      row.querySelector('.app-row__label').textContent = m.name;
      row.querySelector('.app-row__pkg').textContent = missing ? (m.key + ' · ' + I18N.t('cpu_missing')) : m.key;
      row.addEventListener('click', () => {
        // Don't block — the file might arrive via sync/import — but warn loudly so a
        // missing profile can't silently become a no-op spoof.
        if (missing && typeof showToast === 'function') showToast(I18N.t('cpu_missing_warn'));
        const fn = cb; cb = null;
        close();
        if (fn) fn({ key: m.key, name: m.name });
      });
      const del = row.querySelector('.app-row__del');
      if (del) del.addEventListener('click', e => {
        e.preventDefault(); e.stopPropagation();          // don't select the row
        if (!(w.Modals && Modals.confirmDelete)) return;
        Modals.confirmDelete(I18N.t('cpu_del_title'),
          I18N.t('cpu_del_msg').replace('%s', m.name), async () => {
            try { await COPG.deleteCpuModel(m.key); if (typeof showToast === 'function') showToast(I18N.t('cpu_del_done')); }
            catch (err) { if (typeof showToast === 'function') showToast(I18N.t('cpu_del_err')); }
            render();
          });
      });
      frag.appendChild(row);
      rowData.push({ el: row, search: (m.name + ' ' + m.key).toLowerCase() });
    });
    list.appendChild(frag);
    applyView();
  }

  function applyView() {
    let visible = 0;
    rowData.forEach(r => {
      const show = !query || r.search.includes(query);
      r.el.style.display = show ? '' : 'none';
      if (show) visible++;
    });
    if (rowData.length && visible === 0) showEmpty(I18N.t('picker_no_results'));
    else hideEmpty();
  }

  $c('#cpSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  /* ── Import custom cpuinfo ──────────────────────────────────────────────
     Pick a file (SAF, no accept — Android mis-types extensionless files),
     name it, save → CPU/cpuinfo_<key> + manifest. Auto-selects the result. */
  let pendingText = null;
  const IMPORT_ERR = {
    name_required: 'cpu_import_err_name', bad_name: 'cpu_import_err_name',
    empty: 'cpu_import_err_empty', too_large: 'cpu_import_err_large',
    not_cpuinfo: 'cpu_import_err_format', exists: 'cpu_import_err_exists',
  };
  function importErr(msg) { const e = $c('#cpImportErr'); e.textContent = msg; e.hidden = false; }
  function hideImportErr() { $c('#cpImportErr').hidden = true; }
  function resetImport() {
    pendingText = null;
    $c('#cpImportName').value = '';
    $c('#cpImportFileName').textContent = I18N.t('cpu_import_nofile');
    hideImportErr();
  }
  $c('#cpImportBtn').addEventListener('click', () => {
    const panel = $c('#cpImportPanel');
    const willOpen = panel.hidden;
    panel.hidden = !willOpen;
    if (willOpen) resetImport();
  });
  $c('#cpImportFile').addEventListener('click', () => $c('#cpFileInput').click());
  $c('#cpFileInput').addEventListener('change', e => {
    const f = e.target.files && e.target.files[0];
    e.target.value = '';                                  // allow re-pick of the same file
    if (!f) return;
    const r = new FileReader();
    r.onload = () => {
      pendingText = String(r.result || '');
      $c('#cpImportFileName').textContent = f.name || I18N.t('cpu_import_file_ok');
      const nameEl = $c('#cpImportName');
      if (!nameEl.value.trim())                           // prefill name from filename
        nameEl.value = (f.name || '').replace(/\.[^.]+$/, '').replace(/^cpuinfo[_-]?/i, '') || (f.name || '');
      hideImportErr();
    };
    r.onerror = () => importErr(I18N.t('cpu_import_err_read'));
    r.readAsText(f);
  });
  $c('#cpImportSave').addEventListener('click', async () => {
    const name = $c('#cpImportName').value.trim();
    if (!name)         { importErr(I18N.t('cpu_import_err_name')); return; }
    if (!pendingText)  { importErr(I18N.t('cpu_import_err_nofile')); return; }
    const btn = $c('#cpImportSave'); btn.disabled = true;
    try {
      const res = await COPG.importCpuModel(name, pendingText);
      $c('#cpImportPanel').hidden = true; resetImport();
      const fn = cb; cb = null;
      close();
      if (fn) fn({ key: res.key, name: res.name });       // auto-select the new profile
      if (typeof showToast === 'function') showToast(I18N.t('cpu_import_done'));
    } catch (err) {
      importErr(I18N.t(IMPORT_ERR[String(err && err.message)] || 'cpu_import_err_generic'));
    } finally { btn.disabled = false; }
  });

  function open(currentKey, callback) {
    cb = callback || null;
    selectedKey = currentKey || null;
    query = '';
    const inp = $c('#cpSearch'); if (inp) inp.value = '';
    $c('#cpImportPanel').hidden = true; resetImport();
    overlay()?.classList.add('visible');
    const s = sheet();
    // Clear any residual inline transform left by a drag-to-dismiss gesture.
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    render();
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.CpuPicker = { open };
})(window);
