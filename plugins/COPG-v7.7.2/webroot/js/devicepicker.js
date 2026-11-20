/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   devicepicker.js — "Choose a device profile" bottom-sheet
   Parallel to apppicker.js but for device profiles (COPG.listDevices()):
   search + A→Z/Z→A sort + tappable rows (device avatar + name + brand·model),
   with the current selection marked. Shares #sheetOverlay and the .picker-* /
   .app-row styling. Selecting a row calls back with {pkgKey, name}.
   window.DevicePicker.open(currentPkgKey, cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const PHONE_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="2" width="12" height="20" rx="2.5"/><line x1="10" y1="18" x2="14" y2="18"/></svg>';
  // dice = the "Random" bucket (random device each launch)
  const DICE_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="8" cy="8" r="1.3" fill="currentColor"/><circle cx="16" cy="16" r="1.3" fill="currentColor"/><circle cx="16" cy="8" r="1.3" fill="currentColor"/><circle cx="8" cy="16" r="1.3" fill="currentColor"/></svg>';
  const CHECK_SVG = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="devPickerSheet" role="dialog" aria-modal="true" aria-label="Device profiles">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="devpicker_title">Device Profiles</span>
        <button class="sheet-header__close" id="dpClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-search">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input type="search" id="dpSearch" autocomplete="off" data-i18n-attr="placeholder:devpicker_search" placeholder="Search devices…" />
      </div>
      <div class="picker-controls">
        <div class="pk-spacer"></div>
        <button type="button" class="pk-sort" id="dpSortBtn" aria-label="Sort A→Z / Z→A">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 5v14"/><path d="M4 8l3-3 3 3"/><path d="M14 7h6M14 12h4M14 17h2"/></svg>
          <span id="dpSortLabel" data-i18n="sort_name">Name (A→Z)</span>
        </button>
      </div>
      <div class="picker-body">
        <div class="picker-list" id="dpList"></div>
        <div class="picker-empty" id="dpEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8.5" y1="11" x2="13.5" y2="11"/></svg>
          <span id="dpEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $d = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('devPickerSheet');

  let cb = null;
  let rowData = [];            // [{el, name, search}]
  let query = '';
  let sortDir = 'az';         // 'az' | 'za'
  let selectedKey = null;

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $d('#dpClose').addEventListener('click', close);

  function showEmpty(msg) { $d('#dpEmptyMsg').textContent = msg; $d('#dpEmpty').hidden = false; }
  function hideEmpty() { $d('#dpEmpty').hidden = true; }

  function render() {
    const list = $d('#dpList');
    list.innerHTML = '';
    rowData = [];
    const devs = (w.COPG && COPG.listDevices) ? COPG.listDevices() : [];
    if (!devs.length) {
      showEmpty(I18N.t('devpicker_empty'));
      return;
    }
    hideEmpty();
    const frag = document.createDocumentFragment();
    devs.forEach(d => {
      const sub = [d.brand, d.model].filter(Boolean).join(' · ');
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'app-row dev-row';
      row.dataset.key = d.pkgKey;
      row.classList.toggle('is-selected', d.pkgKey === selectedKey);
      row.innerHTML =
        `<span class="app-row__icon dev-row__avatar">${d.random ? DICE_SVG : PHONE_SVG}</span>` +
        `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span></span>` +
        `<span class="dev-row__check">${CHECK_SVG}</span>`;
      row.querySelector('.app-row__label').textContent = d.name;
      row.querySelector('.app-row__pkg').textContent = sub;
      row.addEventListener('click', () => {
        const fn = cb; cb = null;
        close();
        if (fn) fn({ pkgKey: d.pkgKey, name: d.name });
      });
      frag.appendChild(row);
      // rank pins the built-in buckets (This Device = 0, Random = 1) to the top; rest = 2.
      rowData.push({ el: row, name: d.name, search: (d.name + ' ' + sub).toLowerCase(),
                     rank: d.identity ? 0 : (d.random ? 1 : 2) });
    });
    list.appendChild(frag);
    sortRows();
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

  function sortRows() {
    const dir = sortDir === 'za' ? -1 : 1;
    rowData.sort((a, b) => dir * a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
    rowData.sort((a, b) => a.rank - b.rank);   // pin This Device + Random on top (stable → keeps A→Z within rest)
    const list = $d('#dpList');
    rowData.forEach(r => list.appendChild(r.el));
  }

  function updateSortLabel() {
    const span = $d('#dpSortLabel');
    if (!span) return;
    const key = sortDir === 'az' ? 'sort_name' : 'sort_name_za';
    span.dataset.i18n = key;
    span.textContent = I18N.t(key);
  }

  $d('#dpSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  $d('#dpSortBtn').addEventListener('click', () => {
    sortDir = sortDir === 'az' ? 'za' : 'az';
    updateSortLabel();
    sortRows();
    applyView();
  });

  function open(currentKey, callback) {
    cb = callback || null;
    selectedKey = currentKey || null;
    query = '';
    sortDir = 'az'; updateSortLabel();
    const inp = $d('#dpSearch'); if (inp) inp.value = '';
    overlay()?.classList.add('visible');
    const s = sheet();
    // Clear any residual inline transform left by a drag-to-dismiss gesture
    // (an inline transform beats the .open stylesheet rule — see apppicker.js).
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    render();
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.DevicePicker = { open };
})(window);
