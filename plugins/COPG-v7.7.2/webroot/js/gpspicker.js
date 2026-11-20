/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   gpspicker.js — "Choose a place" bottom-sheet (GPS coordinate spoof)
   Parallel to tzpicker.js but for the per-app gps=<lat>,<lng> tag. Rows come from the bundled
   place DB (COPG.getPlaces() → webroot/cities.json, [{name,country,lat,lng}]); selecting one calls
   back with {lat, lng, name}. A leading "Real (off)" row clears the spoof (lat/lng ''). An
   "add custom" panel takes a literal lat/lng (+ optional label) for a place not in the list — the
   escape hatch, since the tag stores coords, not the name.
   window.GpsPicker.open(currentLat, currentLng, cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const PIN_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 12-9 12s-9-5-9-12a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>';
  const OFF_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 12-9 12s-9-5-9-12a9 9 0 0 1 18 0z"/><line x1="4" y1="4" x2="20" y2="20"/></svg>';
  const CHECK_SVG = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="gpsPickerSheet" role="dialog" aria-modal="true" aria-label="Places">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="gpspicker_title">Location</span>
        <button class="sheet-header__close" id="gpsClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-searchrow">
        <div class="picker-search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="search" id="gpsSearch" autocomplete="off" data-i18n-attr="placeholder:gpspicker_search" placeholder="Search country or city…" />
        </div>
        <button type="button" class="picker-add" id="gpsAddBtn" data-i18n-attr="aria-label:gps_add_btn" aria-label="Add custom coordinates">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
      <div class="picker-import__panel" id="gpsAddPanel" hidden>
        <div class="picker-import__title" data-i18n="gps_add_btn">Add custom coordinates</div>
        <label class="field"><span class="field__label" data-i18n="gps_add_lat">Latitude</span>
        <input type="text" id="gpsAddLat" class="picker-import__name" inputmode="decimal" autocomplete="off" spellcheck="false" placeholder="e.g. 35.6762" /></label>
        <label class="field"><span class="field__label" data-i18n="gps_add_lng">Longitude</span>
        <input type="text" id="gpsAddLng" class="picker-import__name" inputmode="decimal" autocomplete="off" spellcheck="false" placeholder="e.g. 139.6503" /></label>
        <label class="field"><span class="field__label" data-i18n="gps_add_label">Label (optional)</span>
        <input type="text" id="gpsAddLabel" class="picker-import__name" autocomplete="off" spellcheck="false" data-i18n-attr="placeholder:gps_add_label_ph" placeholder="e.g. Home" /></label>
        <div class="picker-import__err" id="gpsAddErr" hidden></div>
        <button type="button" class="picker-import__save" id="gpsAddSave" data-i18n="gps_add_save">Use coordinates</button>
      </div>
      <div class="picker-body">
        <div class="picker-list" id="gpsList"></div>
        <div class="picker-empty" id="gpsEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8.5" y1="11" x2="13.5" y2="11"/></svg>
          <span id="gpsEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $g = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('gpsPickerSheet');

  let cb = null;
  let rowData = [];
  let query = '';
  let selLat = '', selLng = '';

  // Coord equality at ~4dp (~11 m) — how a saved tag matches a bundled place for the selected tick.
  const eq = (a, b) => a !== '' && b !== '' && Math.abs(+a - +b) < 1e-4;

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $g('#gpsClose').addEventListener('click', close);

  function showEmpty(msg) { $g('#gpsEmptyMsg').textContent = msg; $g('#gpsEmpty').hidden = false; }
  function hideEmpty() { $g('#gpsEmpty').hidden = true; }

  function pick(lat, lng, name) {
    const fn = cb; cb = null;
    close();
    if (fn) fn({ lat, lng, name });
  }

  function addRow(frag, lat, lng, name, sub, isOffRow) {
    const isSel = isOffRow ? (selLat === '' && selLng === '') : (eq(lat, selLat) && eq(lng, selLng));
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'app-row dev-row';
    row.classList.toggle('is-selected', isSel);
    row.innerHTML =
      `<span class="app-row__icon dev-row__avatar">${isOffRow ? OFF_SVG : PIN_SVG}</span>` +
      `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span></span>` +
      `<span class="dev-row__check">${CHECK_SVG}</span>`;
    row.querySelector('.app-row__label').textContent =
      (COPG.flagForCountryName(sub) ? COPG.flagForCountryName(sub) + ' ' : '') + name;
    row.querySelector('.app-row__pkg').textContent = sub || '';
    row.addEventListener('click', () => pick(isOffRow ? '' : String(lat), isOffRow ? '' : String(lng), isOffRow ? '' : name));
    frag.appendChild(row);
    rowData.push({ el: row, search: ((name || '') + ' ' + (sub || '')).toLowerCase(), off: !!isOffRow });
  }

  async function render() {
    const list = $g('#gpsList');
    list.innerHTML = '';
    rowData = [];
    const places = (w.COPG && COPG.getPlaces) ? await COPG.getPlaces() : [];
    hideEmpty();
    const frag = document.createDocumentFragment();
    addRow(frag, '', '', I18N.t('gps_real'), '', true);          // "Real (device location)" = off
    places.forEach(p => addRow(frag, p.lat, p.lng, p.name, p.country || '', false));
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

  $g('#gpsSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  /* ── Add custom coordinates ── */
  function addErr(msg) { const e = $g('#gpsAddErr'); e.textContent = msg; e.hidden = false; }
  function resetAdd() { $g('#gpsAddLat').value = ''; $g('#gpsAddLng').value = ''; $g('#gpsAddLabel').value = ''; $g('#gpsAddErr').hidden = true; }
  $g('#gpsAddBtn').addEventListener('click', () => {
    const panel = $g('#gpsAddPanel');
    const willOpen = panel.hidden;
    panel.hidden = !willOpen;
    if (willOpen) { resetAdd(); const i = $g('#gpsAddLat'); if (i) i.focus(); }
  });
  $g('#gpsAddSave').addEventListener('click', () => {
    const la = $g('#gpsAddLat').value.trim(), lo = $g('#gpsAddLng').value.trim();
    if (la === '' || lo === '' || isNaN(+la) || isNaN(+lo)) { addErr(I18N.t('gps_add_err_num')); return; }
    if (+la < -90 || +la > 90 || +lo < -180 || +lo > 180) { addErr(I18N.t('gps_add_err_range')); return; }
    const label = $g('#gpsAddLabel').value.trim() || (la + ', ' + lo);
    $g('#gpsAddPanel').hidden = true; resetAdd();
    pick(la, lo, label);
  });
  ['#gpsAddLat', '#gpsAddLng', '#gpsAddLabel'].forEach(sel =>
    $g(sel).addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); $g('#gpsAddSave').click(); } }));

  function open(currentLat, currentLng, callback) {
    cb = callback || null;
    selLat = currentLat || '';
    selLng = currentLng || '';
    query = '';
    const inp = $g('#gpsSearch'); if (inp) inp.value = '';
    $g('#gpsAddPanel').hidden = true; resetAdd();
    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    render();
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.GpsPicker = { open };
})(window);
