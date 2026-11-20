/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   carrierpicker.js — "Choose a carrier" bottom-sheet
   Parallel to cpupicker.js / gpupicker.js but for SIM/carrier profiles
   (COPG.getCarrierModels()): search + tappable rows (SIM avatar + carrier name +
   MCC/MNC · ISO), current selection marked, per-row delete, and a + add-panel to
   append a custom carrier (name/MCC/MNC/ISO → SIM/manifest.json). The list is the
   shipped SIM/manifest.json (device) or the built-in fallback (preview). Selecting a
   row calls back with {key, name}.
   window.CarrierPicker.open(currentKey, cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const SIM_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2h8l4 4v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"/><rect x="8" y="12" width="8" height="6" rx="1"/><path d="M11 12v6M8 15h8"/></svg>';
  const CHECK_SVG = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
  const TRASH_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';
  const EDIT_SVG  = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="simPickerSheet" role="dialog" aria-modal="true" aria-label="Carriers">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="simpicker_title">Carriers</span>
        <button class="sheet-header__close" id="smClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-searchrow">
        <div class="picker-search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="search" id="smSearch" autocomplete="off" data-i18n-attr="placeholder:simpicker_search" placeholder="Search carriers…" />
        </div>
        <button type="button" class="picker-add" id="smAddBtn" data-i18n-attr="aria-label:sim_add_btn" aria-label="Add custom carrier">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
      <div class="picker-import__panel" id="smAddPanel" hidden>
        <div class="picker-import__title" data-i18n="sim_add_title">Add custom carrier…</div>
        <label class="field"><span class="field__label" data-i18n="sim_add_name_lbl">Carrier name</span>
        <input type="text" id="smAddName" class="field__input" autocomplete="off" data-i18n-attr="placeholder:sim_add_name_ph" placeholder="e.g. T-Mobile" /></label>
        <div class="sim-add__grid">
          <label class="field"><span class="field__label" data-i18n="sim_add_mcc_lbl">MCC</span>
          <input type="text" id="smAddMcc" class="field__input field__input--mono" inputmode="numeric" maxlength="3" autocomplete="off" data-i18n-attr="placeholder:sim_add_mcc_ph" placeholder="310" /></label>
          <label class="field"><span class="field__label" data-i18n="sim_add_mnc_lbl">MNC</span>
          <input type="text" id="smAddMnc" class="field__input field__input--mono" inputmode="numeric" maxlength="3" autocomplete="off" data-i18n-attr="placeholder:sim_add_mnc_ph" placeholder="260" /></label>
          <label class="field"><span class="field__label" data-i18n="sim_add_iso_lbl">Country</span>
          <input type="text" id="smAddIso" class="field__input field__input--mono" maxlength="2" autocomplete="off" data-i18n-attr="placeholder:sim_add_iso_ph" placeholder="us" /></label>
        </div>
        <div class="picker-import__hint" data-i18n="sim_add_iso_hint">Country is the 2-letter ISO code (us, gb, de…). MCC/MNC identify the network.</div>
        <div class="picker-import__err" id="smAddErr" hidden></div>
        <button type="button" class="picker-import__save" id="smAddSave" data-i18n="sim_add_save">Save carrier</button>
      </div>
      <div class="picker-body">
        <div class="picker-list" id="smList"></div>
        <div class="picker-empty" id="smEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8.5" y1="11" x2="13.5" y2="11"/></svg>
          <span id="smEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $c = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('simPickerSheet');

  let cb = null;
  let rowData = [];
  let query = '';
  let selectedKey = null;
  let editKey = null;            // non-null → the add panel is editing this carrier

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $c('#smClose').addEventListener('click', close);

  function showEmpty(msg) { $c('#smEmptyMsg').textContent = msg; $c('#smEmpty').hidden = false; }
  function hideEmpty() { $c('#smEmpty').hidden = true; }

  async function render() {
    const list = $c('#smList');
    list.innerHTML = '';
    rowData = [];
    const models = (w.COPG && COPG.getCarrierModels) ? await COPG.getCarrierModels() : [];
    if (!models.length) { showEmpty(I18N.t('simpicker_empty')); return; }
    hideEmpty();
    const frag = document.createDocumentFragment();
    // "None (real)" — clears the slot (leave this SIM real). Always first, not searchable-away.
    {
      const none = document.createElement('button');
      none.type = 'button';
      none.className = 'app-row dev-row sim-row--none';
      none.dataset.key = '';
      none.classList.toggle('is-selected', !selectedKey);
      none.innerHTML =
        `<span class="app-row__icon dev-row__avatar">${SIM_SVG}</span>` +
        `<span class="app-row__text"><span class="app-row__label"></span></span>` +
        `<span class="dev-row__check">${CHECK_SVG}</span>`;
      none.querySelector('.app-row__label').textContent = I18N.t('pkg_sim_none');
      none.addEventListener('click', () => { const fn = cb; cb = null; close(); if (fn) fn({ key: '', name: '' }); });
      frag.appendChild(none);
      rowData.push({ el: none, search: '' });        // always visible (empty search string)
    }
    models.forEach(m => {
      const isSel = (m.key === selectedKey);
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'app-row dev-row';
      row.dataset.key = m.key;
      row.classList.toggle('is-selected', isSel);
      row.innerHTML =
        `<span class="app-row__icon dev-row__avatar">${SIM_SVG}</span>` +
        `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span></span>` +
        `<span class="app-row__edit" role="button" tabindex="0" data-i18n-attr="aria-label:profile_edit" aria-label="Edit profile">${EDIT_SVG}</span>` +
        `<span class="app-row__del" role="button" tabindex="0" data-i18n-attr="aria-label:profile_delete" aria-label="Delete profile">${TRASH_SVG}</span>` +
        `<span class="dev-row__check">${CHECK_SVG}</span>`;
      row.querySelector('.app-row__label').textContent =
        (COPG.flagEmoji(m.iso) ? COPG.flagEmoji(m.iso) + ' ' : '') + m.name;
      row.querySelector('.app-row__pkg').textContent =
        `${m.mcc || ''}${m.mnc ? '/' + m.mnc : ''}${m.iso ? ' · ' + String(m.iso).toUpperCase() : ''}`;
      row.addEventListener('click', () => {
        const fn = cb; cb = null;
        close();
        if (fn) fn({ key: m.key, name: m.name });
      });
      const edit = row.querySelector('.app-row__edit');
      if (edit) edit.addEventListener('click', e => {
        e.preventDefault(); e.stopPropagation();          // don't select the row
        openEdit(m);
      });
      const del = row.querySelector('.app-row__del');
      if (del) del.addEventListener('click', e => {
        e.preventDefault(); e.stopPropagation();
        if (!(w.Modals && Modals.confirmDelete)) return;
        Modals.confirmDelete(I18N.t('sim_del_title'),
          I18N.t('sim_del_msg').replace('%s', m.name), async () => {
            try { await COPG.deleteCarrierModel(m.key); if (typeof showToast === 'function') showToast(I18N.t('sim_del_done')); }
            catch (err) { if (typeof showToast === 'function') showToast(I18N.t('sim_del_err')); }
            render();
          });
      });
      frag.appendChild(row);
      rowData.push({ el: row, search: (m.name + ' ' + m.key + ' ' + (m.mcc || '') + ' ' + (m.mnc || '') + ' ' + (m.iso || '')).toLowerCase() });
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

  $c('#smSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  /* ── Add custom carrier ─────────────────────────────────────────────────
     4 fields (name/MCC/MNC/ISO) → COPG.importCarrierModel → SIM/manifest.json.
     Auto-selects the result. */
  const ADD_ERR = {
    name_required: 'sim_add_err_name', bad_name: 'sim_add_err_name',
    bad_mcc: 'sim_add_err_mcc', bad_mnc: 'sim_add_err_mnc',
    bad_iso: 'sim_add_err_iso', exists: 'sim_add_err_exists',
  };
  function addErr(msg) { const e = $c('#smAddErr'); e.textContent = msg; e.hidden = false; }
  function hideAddErr() { $c('#smAddErr').hidden = true; }
  function resetAdd() {
    $c('#smAddName').value = '';
    $c('#smAddMcc').value = '';
    $c('#smAddMnc').value = '';
    $c('#smAddIso').value = '';
    hideAddErr();
  }
  // Panel title + save-button label track add vs edit mode. Set directly (not via
  // data-i18n) because the panel is toggled open between I18N.apply() runs.
  function setPanelMode() {
    const title = $c('#smAddPanel .picker-import__title');
    if (title) title.textContent = I18N.t(editKey ? 'sim_edit_title' : 'sim_add_title');
    const save = $c('#smAddSave');
    if (save) save.textContent = I18N.t(editKey ? 'sim_edit_save' : 'sim_add_save');
  }
  function openAdd() {
    editKey = null;
    resetAdd();
    setPanelMode();
    $c('#smAddPanel').hidden = false;
  }
  function openEdit(m) {
    editKey = m.key;
    hideAddErr();
    $c('#smAddName').value = m.name || '';
    $c('#smAddMcc').value  = m.mcc || '';
    $c('#smAddMnc').value  = m.mnc || '';
    $c('#smAddIso').value  = m.iso || '';
    setPanelMode();
    $c('#smAddPanel').hidden = false;
    $c('#smAddPanel').scrollIntoView({ block: 'nearest' });
  }
  $c('#smAddBtn').addEventListener('click', () => {
    const panel = $c('#smAddPanel');
    if (panel.hidden) openAdd();
    else { panel.hidden = true; editKey = null; }
  });
  $c('#smAddSave').addEventListener('click', async () => {
    const o = {
      name: $c('#smAddName').value.trim(),
      mcc:  $c('#smAddMcc').value.trim(),
      mnc:  $c('#smAddMnc').value.trim(),
      iso:  $c('#smAddIso').value.trim(),
    };
    const btn = $c('#smAddSave'); btn.disabled = true;
    try {
      if (editKey) {
        const wasSel = (editKey === selectedKey);
        const res = await COPG.updateCarrierModel(editKey, o);
        editKey = null;
        $c('#smAddPanel').hidden = true; resetAdd();
        setPanelMode();
        render();                                           // reflect the edit; stay in picker
        if (wasSel && cb) cb({ key: res.key, name: res.name });  // refresh the modal's carrier text
        if (typeof showToast === 'function') showToast(I18N.t('sim_edit_done'));
      } else {
        const res = await COPG.importCarrierModel(o);
        $c('#smAddPanel').hidden = true; resetAdd();
        const fn = cb; cb = null;
        close();
        if (fn) fn({ key: res.key, name: res.name });       // auto-select the new carrier
        if (typeof showToast === 'function') showToast(I18N.t('sim_add_done'));
      }
    } catch (err) {
      addErr(I18N.t(ADD_ERR[String(err && err.message)] || 'sim_add_err_generic'));
    } finally { btn.disabled = false; }
  });

  function open(currentKey, callback) {
    cb = callback || null;
    selectedKey = currentKey || null;
    query = '';
    editKey = null;
    const inp = $c('#smSearch'); if (inp) inp.value = '';
    $c('#smAddPanel').hidden = true; resetAdd(); setPanelMode();
    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    render();
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.CarrierPicker = { open };
})(window);
