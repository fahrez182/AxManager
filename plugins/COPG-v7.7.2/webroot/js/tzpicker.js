/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   tzpicker.js — "Choose a timezone" bottom-sheet
   Parallel to cpupicker.js but for per-app timezone (the tz=<IANA zone> tag,
   COPG.getTzZones()): search + tappable rows, plus a leading "Real (off)" row and
   an "add custom" panel (type any IANA id → COPG.importTzZone → TZ/manifest.json).
   Selecting a row calls back with {key, name} — key '' = off (real device zone).
   window.TzPicker.open(currentKey, cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const CLOCK_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>';
  const OFF_SVG   = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/><line x1="4" y1="4" x2="20" y2="20"/></svg>';
  const CHECK_SVG = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
  const TRASH_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="tzPickerSheet" role="dialog" aria-modal="true" aria-label="Timezones">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="tzpicker_title">Timezones</span>
        <button class="sheet-header__close" id="tzClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-searchrow">
        <div class="picker-search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="search" id="tzSearch" autocomplete="off" data-i18n-attr="placeholder:tzpicker_search" placeholder="Search timezones…" />
        </div>
        <button type="button" class="picker-add" id="tzAddBtn" data-i18n-attr="aria-label:tz_add_btn" aria-label="Add custom timezone">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
      <div class="picker-import__panel" id="tzAddPanel" hidden>
        <div class="picker-import__title" data-i18n="tz_add_btn">Add custom timezone</div>
        <label class="field"><span class="field__label" data-i18n="tz_add_lbl">IANA timezone ID</span>
        <input type="text" id="tzAddId" class="picker-import__name" autocomplete="off" autocapitalize="none" spellcheck="false" data-i18n-attr="placeholder:tz_add_ph" placeholder="e.g. America/Toronto" /></label>
        <div class="picker-import__err" id="tzAddErr" hidden></div>
        <button type="button" class="picker-import__save" id="tzAddSave" data-i18n="tz_add_save">Add timezone</button>
      </div>
      <div class="picker-body">
        <div class="picker-list" id="tzList"></div>
        <div class="picker-empty" id="tzEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8.5" y1="11" x2="13.5" y2="11"/></svg>
          <span id="tzEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $t = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('tzPickerSheet');

  let cb = null;
  let rowData = [];
  let query = '';
  let selectedKey = null;

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $t('#tzClose').addEventListener('click', close);

  function showEmpty(msg) { $t('#tzEmptyMsg').textContent = msg; $t('#tzEmpty').hidden = false; }
  function hideEmpty() { $t('#tzEmpty').hidden = true; }

  function addRow(frag, key, name, deletable, isOffRow) {
    const isSel = (key === (selectedKey || ''));
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'app-row dev-row';
    row.dataset.key = key;
    row.classList.toggle('is-selected', isSel);
    row.innerHTML =
      `<span class="app-row__icon dev-row__avatar">${isOffRow ? OFF_SVG : CLOCK_SVG}</span>` +
      `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span></span>` +
      (deletable ? `<span class="app-row__del" role="button" tabindex="0" data-i18n-attr="aria-label:profile_delete" aria-label="Delete">${TRASH_SVG}</span>` : '') +
      `<span class="dev-row__check">${CHECK_SVG}</span>`;
    row.querySelector('.app-row__label').textContent =
      (isOffRow ? '' : (COPG.flagForTz(key) ? COPG.flagForTz(key) + ' ' : '')) + name;
    row.querySelector('.app-row__pkg').textContent = isOffRow ? '' : key;
    row.addEventListener('click', () => {
      const fn = cb; cb = null;
      close();
      if (fn) fn({ key, name });
    });
    if (deletable) {
      const del = row.querySelector('.app-row__del');
      if (del) del.addEventListener('click', e => {
        e.preventDefault(); e.stopPropagation();
        if (!(w.Modals && Modals.confirmDelete)) return;
        Modals.confirmDelete(I18N.t('tz_del_title'),
          I18N.t('tz_del_msg').replace('%s', name), async () => {
            try { await COPG.deleteTzZone(key); if (typeof showToast === 'function') showToast(I18N.t('tz_del_done')); }
            catch (err) { if (typeof showToast === 'function') showToast(I18N.t('tz_del_err')); }
            render();
          });
      });
    }
    frag.appendChild(row);
    rowData.push({ el: row, search: (name + ' ' + key).toLowerCase(), off: !!isOffRow });
  }

  async function render() {
    const list = $t('#tzList');
    list.innerHTML = '';
    rowData = [];
    const zones = (w.COPG && COPG.getTzZones) ? await COPG.getTzZones() : [];
    const builtin = new Set(['UTC', 'America/New_York', 'America/Chicago', 'America/Denver',
      'America/Los_Angeles', 'America/Sao_Paulo', 'Europe/London', 'Europe/Paris', 'Europe/Berlin',
      'Europe/Moscow', 'Europe/Istanbul', 'Africa/Cairo', 'Africa/Lagos', 'Asia/Dubai', 'Asia/Tehran',
      'Asia/Karachi', 'Asia/Kolkata', 'Asia/Dhaka', 'Asia/Bangkok', 'Asia/Shanghai', 'Asia/Singapore',
      'Asia/Tokyo', 'Asia/Seoul', 'Australia/Sydney', 'Pacific/Auckland']);
    hideEmpty();
    const frag = document.createDocumentFragment();
    addRow(frag, '', I18N.t('tz_real'), false, true);           // "Real (device timezone)" = off
    zones.forEach(z => addRow(frag, z.key, z.name, !builtin.has(z.key), false));
    list.appendChild(frag);
    applyView();
  }

  function applyView() {
    let visible = 0;
    rowData.forEach(r => {
      const show = r.off || !query || r.search.includes(query);   // keep the off row always visible
      r.el.style.display = show ? '' : 'none';
      if (show && !r.off) visible++;
    });
    if (rowData.length > 1 && visible === 0) showEmpty(I18N.t('picker_no_results'));
    else hideEmpty();
  }

  $t('#tzSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  /* ── Add custom IANA zone ── */
  const ADD_ERR = { empty: 'tz_add_err_empty', bad_id: 'tz_add_err_bad', exists: 'tz_add_err_exists' };
  function addErr(msg) { const e = $t('#tzAddErr'); e.textContent = msg; e.hidden = false; }
  function resetAdd() { $t('#tzAddId').value = ''; $t('#tzAddErr').hidden = true; }
  $t('#tzAddBtn').addEventListener('click', () => {
    const panel = $t('#tzAddPanel');
    const willOpen = panel.hidden;
    panel.hidden = !willOpen;
    if (willOpen) { resetAdd(); const i = $t('#tzAddId'); if (i) i.focus(); }
  });
  $t('#tzAddSave').addEventListener('click', async () => {
    const id = $t('#tzAddId').value.trim();
    const btn = $t('#tzAddSave'); btn.disabled = true;
    try {
      const res = await COPG.importTzZone(id);
      $t('#tzAddPanel').hidden = true; resetAdd();
      const fn = cb; cb = null;
      close();
      if (fn) fn({ key: res.key, name: res.name });              // auto-select the new zone
      if (typeof showToast === 'function') showToast(I18N.t('tz_add_done'));
    } catch (err) {
      addErr(I18N.t(ADD_ERR[String(err && err.message)] || 'tz_add_err_generic'));
    } finally { btn.disabled = false; }
  });
  $t('#tzAddId').addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); $t('#tzAddSave').click(); } });

  function open(currentKey, callback) {
    cb = callback || null;
    selectedKey = currentKey || '';
    query = '';
    const inp = $t('#tzSearch'); if (inp) inp.value = '';
    $t('#tzAddPanel').hidden = true; resetAdd();
    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    render();
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.TzPicker = { open };
})(window);
