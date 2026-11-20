/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   uapicker.js — "Choose a WebView User-Agent profile" bottom-sheet
   Parallel to cpupicker.js/gpupicker.js but for UA profiles (COPG.getUaProfiles()):
   search + tappable rows (name + UA preview), current selection marked, per-row delete,
   and a "+" add panel (name + UA string → UA/manifest.json). Selecting calls back {key,name}.
   window.UaPicker.open(currentKey, cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const GLOBE_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18"/></svg>';
  const CHECK_SVG = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
  const TRASH_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="uaPickerSheet" role="dialog" aria-modal="true" aria-label="UA profiles">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="uapicker_title">User-Agent Profiles</span>
        <button class="sheet-header__close" id="uaClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-searchrow">
        <div class="picker-search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="search" id="uaSearch" autocomplete="off" data-i18n-attr="placeholder:uapicker_search" placeholder="Search UAs…" />
        </div>
        <button type="button" class="picker-add" id="uaAddBtn" data-i18n-attr="aria-label:ua_add_btn" aria-label="Add custom UA">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
      <div class="picker-import__panel" id="uaAddPanel" hidden>
        <div class="picker-import__title" data-i18n="ua_add_btn">Add custom UA…</div>
        <label class="field"><span class="field__label" data-i18n="ua_add_name_lbl">Profile name</span>
        <input type="text" id="uaAddName" class="picker-import__name" autocomplete="off" data-i18n-attr="placeholder:ua_add_name_ph" placeholder="e.g. My iPad" /></label>
        <label class="field"><span class="field__label" data-i18n="ua_add_ua_lbl">User-Agent string</span>
        <input type="text" id="uaAddUa" class="picker-import__name" spellcheck="false" autocapitalize="none" autocomplete="off" data-i18n-attr="placeholder:ua_add_ua_ph" placeholder="Mozilla/5.0 (…)" /></label>
        <div class="picker-import__err" id="uaAddErr" hidden></div>
        <button type="button" class="picker-import__save" id="uaAddSave" data-i18n="ua_add_save">Save profile</button>
      </div>
      <div class="picker-body">
        <div class="picker-list" id="uaList"></div>
        <div class="picker-empty" id="uaEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8.5" y1="11" x2="13.5" y2="11"/></svg>
          <span id="uaEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $u = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('uaPickerSheet');

  let cb = null, rowData = [], query = '', selectedKey = null;

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $u('#uaClose').addEventListener('click', close);

  function showEmpty(msg) { $u('#uaEmptyMsg').textContent = msg; $u('#uaEmpty').hidden = false; }
  function hideEmpty() { $u('#uaEmpty').hidden = true; }

  async function render() {
    const list = $u('#uaList');
    list.innerHTML = ''; rowData = [];
    const profs = (w.COPG && COPG.getUaProfiles) ? await COPG.getUaProfiles() : [];
    if (!profs.length) { showEmpty(I18N.t('uapicker_empty')); return; }
    hideEmpty();
    const frag = document.createDocumentFragment();
    profs.forEach(p => {
      const isSel = p.key === selectedKey;
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'app-row dev-row';
      row.dataset.key = p.key;
      row.classList.toggle('is-selected', isSel);
      row.innerHTML =
        `<span class="app-row__icon dev-row__avatar">${GLOBE_SVG}</span>` +
        `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span></span>` +
        `<span class="app-row__del" role="button" tabindex="0" data-i18n-attr="aria-label:profile_delete" aria-label="Delete profile">${TRASH_SVG}</span>` +
        `<span class="dev-row__check">${CHECK_SVG}</span>`;
      row.querySelector('.app-row__label').textContent = p.name;
      row.querySelector('.app-row__pkg').textContent = p.ua;   // full UA as the sub-line (wraps/ellipsis via CSS)
      row.addEventListener('click', () => { const fn = cb; cb = null; close(); if (fn) fn({ key: p.key, name: p.name }); });
      const del = row.querySelector('.app-row__del');
      if (del) del.addEventListener('click', e => {
        e.preventDefault(); e.stopPropagation();
        if (!(w.Modals && Modals.confirmDelete)) return;
        Modals.confirmDelete(I18N.t('ua_del_title'),
          I18N.t('ua_del_msg').replace('%s', p.name), async () => {
            try { await COPG.deleteUaProfile(p.key); if (typeof showToast === 'function') showToast(I18N.t('ua_del_done')); }
            catch (err) { if (typeof showToast === 'function') showToast(I18N.t('ua_del_err')); }
            render();
          });
      });
      frag.appendChild(row);
      rowData.push({ el: row, search: (p.name + ' ' + p.key + ' ' + p.ua).toLowerCase() });
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
  $u('#uaSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  function addErr(msg) { const e = $u('#uaAddErr'); e.textContent = msg; e.hidden = false; }
  function resetAdd() { $u('#uaAddName').value = ''; $u('#uaAddUa').value = ''; $u('#uaAddErr').hidden = true; }
  $u('#uaAddBtn').addEventListener('click', () => {
    const panel = $u('#uaAddPanel'); const willOpen = panel.hidden;
    panel.hidden = !willOpen; if (willOpen) resetAdd();
  });
  $u('#uaAddSave').addEventListener('click', async () => {
    const name = $u('#uaAddName').value.trim(), ua = $u('#uaAddUa').value.trim();
    if (!name) { addErr(I18N.t('ua_add_err_name')); return; }
    if (!ua)   { addErr(I18N.t('ua_add_err_ua')); return; }
    const btn = $u('#uaAddSave'); btn.disabled = true;
    try {
      const key = await COPG.importUaProfile({ name, ua });
      $u('#uaAddPanel').hidden = true; resetAdd();
      const fn = cb; cb = null; close();
      if (fn) fn({ key, name });
      if (typeof showToast === 'function') showToast(I18N.t('ua_add_done'));
    } catch (err) { addErr(I18N.t('ua_add_err_ua')); }
    finally { btn.disabled = false; }
  });

  function open(currentKey, callback) {
    cb = callback || null;
    selectedKey = currentKey || null;
    query = '';
    const inp = $u('#uaSearch'); if (inp) inp.value = '';
    $u('#uaAddPanel').hidden = true; resetAdd();
    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    render();
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.UaPicker = { open };
})(window);
