/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   gpupicker.js — "Choose a GPU profile" bottom-sheet  (REFERENCE branch only)
   Parallel to cpupicker.js but for GPU profiles (COPG.getGpuModels()):
   search + tappable rows (chip avatar + GPU name + key), current selection marked.
   The list is GPU/manifest.json (device) or the built-in fallback (preview). The
   custom-add panel takes 4 fields (name, GL/Vulkan renderer, vendor, vendorID hex)
   instead of a file — a GPU profile is just strings, vendorID is config-driven.
   Selecting a row calls back with {key, name}.
   window.GpuPicker.open(currentKey, cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const GPU_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="8" cy="12" r="2.5"/><path d="M14 10h5M14 14h5"/></svg>';
  const CHECK_SVG = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
  const TRASH_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';
  const EDIT_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="gpuPickerSheet" role="dialog" aria-modal="true" aria-label="GPU profiles">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="gpupicker_title">GPU Profiles</span>
        <button class="sheet-header__close" id="gpClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-searchrow">
        <div class="picker-search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="search" id="gpSearch" autocomplete="off" data-i18n-attr="placeholder:gpupicker_search" placeholder="Search GPUs…" />
        </div>
        <button type="button" class="picker-add" id="gpAddBtn" data-i18n-attr="aria-label:gpu_add_btn" aria-label="Add custom GPU">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
      <div class="picker-import__panel" id="gpAddPanel" hidden>
        <div class="picker-import__title" data-i18n="gpu_add_btn">Add custom GPU…</div>
        <label class="field"><span class="field__label" data-i18n="gpu_add_name_lbl">Profile name</span>
        <input type="text" id="gpAddName"     class="field__input" autocomplete="off" data-i18n-attr="placeholder:gpu_add_name_ph"     placeholder="e.g. Adreno 750" /></label>
        <label class="field"><span class="field__label" data-i18n="gpu_add_renderer_lbl">Renderer</span>
        <input type="text" id="gpAddRenderer" class="field__input" autocomplete="off" data-i18n-attr="placeholder:gpu_add_renderer_ph" placeholder="e.g. Adreno (TM) 750" /></label>
        <label class="field"><span class="field__label" data-i18n="gpu_add_vendor_lbl">Vendor</span>
        <input type="text" id="gpAddVendor"   class="field__input" autocomplete="off" data-i18n-attr="placeholder:gpu_add_vendor_ph"   placeholder="e.g. Qualcomm" /></label>
        <label class="field"><span class="field__label" data-i18n="gpu_add_vendorid_lbl">Vulkan vendor ID</span>
        <input type="text" id="gpAddVendorId" class="field__input field__input--mono" autocomplete="off" data-i18n-attr="placeholder:gpu_add_vendorid_ph" placeholder="e.g. 0x5143" /></label>
        <div class="adv-group" id="gpAdvGroup">
          <button type="button" class="adv-toggle" id="gpAdvToggle" aria-expanded="false">
            <span class="field__label" data-i18n="gpu_adv_title">Advanced spoofs</span>
            <span class="field__opt" data-i18n="opt_optional">(optional)</span>
            <svg class="adv-caret" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          <div class="adv-body" id="gpAdvBody" hidden>
            <label class="field"><span class="field__label" data-i18n="gpu_add_glversion_lbl">GL version</span>
            <input type="text" id="gpAddGlVersion" class="field__input field__input--mono" autocomplete="off" data-i18n-attr="placeholder:gpu_add_glversion_ph" placeholder="e.g. OpenGL ES 3.2 V@0762.0" /></label>
            <label class="field"><span class="field__label" data-i18n="gpu_add_glsl_lbl">GLSL version</span>
            <input type="text" id="gpAddGlsl" class="field__input field__input--mono" autocomplete="off" data-i18n-attr="placeholder:gpu_add_glsl_ph" placeholder="e.g. OpenGL ES GLSL ES 3.20" /></label>
            <label class="field"><span class="field__label" data-i18n="gpu_add_egl_lbl">EGL version</span>
            <input type="text" id="gpAddEgl" class="field__input field__input--mono" autocomplete="off" data-i18n-attr="placeholder:gpu_add_egl_ph" placeholder="e.g. 1.5 Android META-EGL" /></label>
            <label class="trow" id="gpAddExtRow">
              <span class="trow__label"><span data-i18n="gpu_add_ext_lbl">Fake GL extensions</span><span class="field__opt field__opt--warn" data-i18n="gpu_add_ext_tag">risky</span></span>
              <span class="toggle"><input type="checkbox" id="gpAddExtToggle"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
            <label class="field" id="gpAddExtField" hidden><span class="field__label" data-i18n="gpu_add_ext_list_lbl">Extensions to add (space-separated)</span>
            <textarea id="gpAddExtensions" class="field__input field__input--mono" rows="2" autocomplete="off" data-i18n-attr="placeholder:gpu_add_ext_ph" placeholder="GL_EXT_geometry_shader GL_OES_texture_view"></textarea></label>
            <div class="picker-import__subhead" data-i18n="gpu_add_vk_adv">Vulkan</div>
            <label class="field"><span class="field__label" data-i18n="gpu_add_deviceid_lbl">Vulkan device ID</span>
            <input type="text" id="gpAddDeviceId" class="field__input field__input--mono" autocomplete="off" data-i18n-attr="placeholder:gpu_add_deviceid_ph" placeholder="e.g. 0x43050a01" /></label>
            <label class="field"><span class="field__label" data-i18n="gpu_add_api_lbl">API version</span>
            <input type="text" id="gpAddApiVersion" class="field__input field__input--mono" autocomplete="off" data-i18n-attr="placeholder:gpu_add_api_ph" placeholder="e.g. 1.3.0" /></label>
            <label class="field"><span class="field__label" data-i18n="gpu_add_driver_lbl">Driver version</span>
            <input type="text" id="gpAddDriverVersion" class="field__input field__input--mono" autocomplete="off" data-i18n-attr="placeholder:gpu_add_driver_ph" placeholder="e.g. 512.762.0" /></label>
            <label class="field"><span class="field__label" data-i18n="gpu_add_vram_lbl">VRAM (MB)</span>
            <input type="number" id="gpAddVram" class="field__input field__input--mono" autocomplete="off" min="0" data-i18n-attr="placeholder:gpu_add_vram_ph" placeholder="e.g. 12288" /></label>
            <label class="trow" id="gpAddFeatRow">
              <span class="trow__label"><span data-i18n="gpu_add_feat_lbl">Force all Vulkan features</span><span class="field__opt field__opt--warn" data-i18n="gpu_add_ext_tag">risky</span></span>
              <span class="toggle"><input type="checkbox" id="gpAddFeatToggle"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          </div>
        </div>
        <div class="picker-import__err" id="gpAddErr" hidden></div>
        <button type="button" class="picker-import__save" id="gpAddSave" data-i18n="gpu_add_save">Save profile</button>
      </div>
      <div class="picker-body">
        <div class="picker-list" id="gpList"></div>
        <div class="picker-empty" id="gpEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8.5" y1="11" x2="13.5" y2="11"/></svg>
          <span id="gpEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $g = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('gpuPickerSheet');

  let cb = null;
  let rowData = [];
  let query = '';
  let selectedKey = null;
  let editKey = null;      // non-null → the add panel is editing this profile

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $g('#gpClose').addEventListener('click', close);

  function showEmpty(msg) { $g('#gpEmptyMsg').textContent = msg; $g('#gpEmpty').hidden = false; }
  function hideEmpty() { $g('#gpEmpty').hidden = true; }

  async function render() {
    const list = $g('#gpList');
    list.innerHTML = '';
    rowData = [];
    const models = (w.COPG && COPG.getGpuModels) ? await COPG.getGpuModels() : [];
    if (!models.length) { showEmpty(I18N.t('gpupicker_empty')); return; }
    hideEmpty();
    const frag = document.createDocumentFragment();
    models.forEach(m => {
      // Highlight the active model; with no explicit key the default is adreno830.
      const isSel = (m.key === selectedKey) || (!selectedKey && m.key === 'adreno830');
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'app-row dev-row';
      row.dataset.key = m.key;
      row.classList.toggle('is-selected', isSel);
      row.innerHTML =
        `<span class="app-row__icon dev-row__avatar">${GPU_SVG}</span>` +
        `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span></span>` +
        `<span class="app-row__edit" role="button" tabindex="0" data-i18n-attr="aria-label:profile_edit" aria-label="Edit profile">${EDIT_SVG}</span>` +
        `<span class="app-row__del" role="button" tabindex="0" data-i18n-attr="aria-label:profile_delete" aria-label="Delete profile">${TRASH_SVG}</span>` +
        `<span class="dev-row__check">${CHECK_SVG}</span>`;
      row.querySelector('.app-row__label').textContent = m.name;
      row.querySelector('.app-row__pkg').textContent = m.renderer || m.key;
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
        e.preventDefault(); e.stopPropagation();          // don't select the row
        if (!(w.Modals && Modals.confirmDelete)) return;
        Modals.confirmDelete(I18N.t('gpu_del_title'),
          I18N.t('gpu_del_msg').replace('%s', m.name), async () => {
            try { await COPG.deleteGpuModel(m.key); if (typeof showToast === 'function') showToast(I18N.t('gpu_del_done')); }
            catch (err) { if (typeof showToast === 'function') showToast(I18N.t('gpu_del_err')); }
            render();
          });
      });
      frag.appendChild(row);
      rowData.push({ el: row, search: (m.name + ' ' + m.key + ' ' + (m.renderer || '')).toLowerCase() });
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

  $g('#gpSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  /* ── Add custom GPU profile (name + renderer + vendor + vendorID) ─────────── */
  const ADD_ERR = {
    name_required: 'gpu_add_err_name', bad_name: 'gpu_add_err_name',
    renderer_required: 'gpu_add_err_renderer', bad_vendorid: 'gpu_add_err_vendorid',
    bad_deviceid: 'gpu_add_err_deviceid',
    exists: 'gpu_add_err_exists',
    not_found: 'gpu_edit_err_gone',
  };
  function addErr(msg) { const e = $g('#gpAddErr'); e.textContent = msg; e.hidden = false; }
  function hideAddErr() { $g('#gpAddErr').hidden = true; }

  // GL version is PRE-FILLED with the profile's stored value, or the device's real
  // GL_VERSION (SurfaceFlinger) as the baseline, so the user edits from a real
  // string. Left at the default OR cleared == spoof OFF (we send '' so native
  // keeps the real value). realGl is the device baseline captured here.
  let realGl = '';
  async function prefillGl(existing) {
    realGl = '';
    try { const si = w.COPG && COPG.getSystemInfo ? await COPG.getSystemInfo() : null;
          realGl = (si && si.gpuVersion) || ''; } catch (_) {}
    const f = $g('#gpAddGlVersion');
    if (f) f.value = existing || realGl;   // stored value in edit; real baseline otherwise
  }

  function resetAdd() {
    $g('#gpAddName').value = '';
    $g('#gpAddRenderer').value = '';
    $g('#gpAddVendor').value = '';
    $g('#gpAddVendorId').value = '';
    $g('#gpAddGlVersion').value = '';
    $g('#gpAddDeviceId').value = '';
    $g('#gpAddExtToggle').checked = false;
    $g('#gpAddExtField').hidden = true;
    $g('#gpAddExtensions').value = '';
    $g('#gpAddApiVersion').value = '';
    $g('#gpAddDriverVersion').value = '';
    $g('#gpAddVram').value = '';
    $g('#gpAddFeatToggle').checked = false;
    $g('#gpAddGlsl').value = '';
    $g('#gpAddEgl').value = '';
    hideAddErr();
  }

  // packed uint32 Vulkan apiVersion → "major.minor.patch" for the edit field.
  // apiVersion masks major to 7 bits (top 3 are the variant).
  function unpackApi(n) {
    n = Number(n) || 0; if (!n) return '';
    return `${(n >>> 22) & 0x7f}.${(n >>> 12) & 0x3ff}.${n & 0xfff}`;
  }
  // driverVersion uses the 10-bit VK_VERSION major (no variant) — e.g. Qualcomm
  // "512.762.0" needs major>127, so DON'T mask to 7 bits like apiVersion.
  function unpackDriver(n) {
    n = Number(n) || 0; if (!n) return '';
    return `${n >>> 22}.${(n >>> 12) & 0x3ff}.${n & 0xfff}`;
  }

  // GL_EXTENSIONS is RISKY (an appended extension the driver lacks can crash a
  // game). Enabling the toggle requires an explicit warning-confirm — same gate
  // pattern as the package modal's GPU risk switch (uncheck first, re-check on OK).
  $g('#gpAddExtToggle').addEventListener('change', () => {
    const tog = $g('#gpAddExtToggle');
    if (tog.checked) {
      tog.checked = false;                       // require explicit acceptance
      const enable = () => { tog.checked = true; $g('#gpAddExtField').hidden = false;
                             $g('#gpAddExtensions').focus(); };
      if (w.Modals && Modals.confirm) {
        Modals.confirm({
          title: I18N.t('gpu_ext_warn_title'),
          message: I18N.t('gpu_ext_warn_msg'),
          okLabel: I18N.t('gpu_ext_warn_ok'),
          danger: true,
          onOk: enable,
        });
      } else { enable(); }
    } else {
      $g('#gpAddExtField').hidden = true;        // off → keep the text, just stop sending it
    }
  });

  // "Force all Vulkan features" — same warn gate. Enabling every VkPhysicalDevice
  // feature bit can crash a game that uses one the real driver lacks.
  $g('#gpAddFeatToggle').addEventListener('change', () => {
    const tog = $g('#gpAddFeatToggle');
    if (!tog.checked) return;
    tog.checked = false;                          // require explicit acceptance
    const enable = () => { tog.checked = true; };
    if (w.Modals && Modals.confirm) {
      Modals.confirm({
        title: I18N.t('gpu_feat_warn_title'),
        message: I18N.t('gpu_feat_warn_msg'),
        okLabel: I18N.t('gpu_ext_warn_ok'),
        danger: true,
        onOk: enable,
      });
    } else { enable(); }
  });

  // Collapsible "Advanced spoofs" section (same pattern as the device modal's
  // Advanced build props) — keeps the add/edit panel short by default.
  function setGpAdv(open) {
    const g = $g('#gpAdvGroup'), b = $g('#gpAdvBody'), t = $g('#gpAdvToggle');
    if (!g || !b) return;
    g.classList.toggle('is-open', !!open);
    b.hidden = !open;
    if (t) t.setAttribute('aria-expanded', open ? 'true' : 'false');
  }
  $g('#gpAdvToggle').addEventListener('click', () => setGpAdv($g('#gpAdvBody').hidden));
  // true when a profile carries any advanced spoof → auto-open the section on edit.
  function hasAdv(m) {
    return !!(m && (m.gl_version || m.glsl_version || m.egl_version || m.extensions ||
                    m.device_id || m.vk_api_version || m.vk_driver_version || m.vram_mb ||
                    (m.vk_features && String(m.vk_features) !== '0x0' && Number(m.vk_features) !== 0)));
  }

  // Panel title + save-button label track add vs edit mode. Set directly (not via
  // data-i18n) because the panel is toggled open between I18N.apply() runs.
  function setPanelMode() {
    const title = $g('#gpAddPanel .picker-import__title');
    if (title) title.textContent = I18N.t(editKey ? 'gpu_edit_title' : 'gpu_add_btn');
    const save = $g('#gpAddSave');
    if (save) save.textContent = I18N.t(editKey ? 'gpu_edit_save' : 'gpu_add_save');
  }

  function openAdd() {
    editKey = null;
    resetAdd();
    setPanelMode();
    setGpAdv(false);              // collapsed for a fresh add
    prefillGl();
    $g('#gpAddPanel').hidden = false;
  }

  function openEdit(m) {
    editKey = m.key;
    hideAddErr();
    $g('#gpAddName').value     = m.name || '';
    $g('#gpAddRenderer').value = m.renderer || '';
    $g('#gpAddVendor').value   = m.vendor || '';
    $g('#gpAddVendorId').value = m.vendor_id || '';
    $g('#gpAddDeviceId').value = m.device_id || '';
    const hasExt = !!(m.extensions && String(m.extensions).trim());
    $g('#gpAddExtToggle').checked = hasExt;        // JS-set → no change event → no warning
    $g('#gpAddExtField').hidden = !hasExt;
    $g('#gpAddExtensions').value = m.extensions || '';
    $g('#gpAddApiVersion').value = unpackApi(m.vk_api_version);
    $g('#gpAddDriverVersion').value = unpackDriver(m.vk_driver_version);
    $g('#gpAddVram').value = m.vram_mb ? String(m.vram_mb) : '';
    $g('#gpAddFeatToggle').checked = !!(m.vk_features && String(m.vk_features) !== '0x0' && Number(m.vk_features) !== 0);
    $g('#gpAddGlsl').value = m.glsl_version || '';
    $g('#gpAddEgl').value = m.egl_version || '';
    setPanelMode();
    setGpAdv(hasAdv(m));                     // auto-open if the profile uses any advanced spoof
    prefillGl(m.gl_version || '');          // stored gl_version, else real baseline
    $g('#gpAddPanel').hidden = false;
    $g('#gpAddPanel').scrollIntoView({ block: 'nearest' });
  }

  $g('#gpAddBtn').addEventListener('click', () => {
    const panel = $g('#gpAddPanel');
    if (panel.hidden) openAdd();
    else { panel.hidden = true; editKey = null; }
  });

  $g('#gpAddSave').addEventListener('click', async () => {
    const name = $g('#gpAddName').value.trim();
    if (!name) { addErr(I18N.t('gpu_add_err_name')); return; }
    const btn = $g('#gpAddSave'); btn.disabled = true;
    try {
      // GL version off when left at default OR cleared: empty or == the real
      // baseline → '' (native keeps the real GL_VERSION). Only an edited value spoofs.
      const glVal = $g('#gpAddGlVersion').value.trim();
      const glOut = (!glVal || glVal === realGl.trim()) ? '' : glVal;
      const fields = {
        name,
        renderer:  $g('#gpAddRenderer').value,
        vendor:    $g('#gpAddVendor').value,
        vendorId:  $g('#gpAddVendorId').value,
        glVersion: glOut,
        deviceId:  $g('#gpAddDeviceId').value,
        // risky opt-ins only sent when their toggle is ON (off → real kept)
        extensions:    $g('#gpAddExtToggle').checked ? $g('#gpAddExtensions').value : '',
        apiVersion:    $g('#gpAddApiVersion').value,
        driverVersion: $g('#gpAddDriverVersion').value,
        vramMb:        $g('#gpAddVram').value,
        vkFeatures:    $g('#gpAddFeatToggle').checked,
        glslVersion:   $g('#gpAddGlsl').value,
        eglVersion:    $g('#gpAddEgl').value,
      };
      const wasEdit = !!editKey;
      const res = wasEdit ? await COPG.updateGpuModel(editKey, fields)
                          : await COPG.importGpuModel(fields);
      editKey = null;
      $g('#gpAddPanel').hidden = true; resetAdd();
      if (wasEdit) {
        // stay in the picker; just refresh the edited row
        render();
        if (typeof showToast === 'function') showToast(I18N.t('gpu_edit_done'));
      } else {
        const fn = cb; cb = null;
        close();
        if (fn) fn({ key: res.key, name: res.name });     // auto-select the new profile
        if (typeof showToast === 'function') showToast(I18N.t('gpu_add_done'));
      }
    } catch (err) {
      addErr(I18N.t(ADD_ERR[String(err && err.message)] || 'gpu_add_err_generic'));
    } finally { btn.disabled = false; }
  });

  function open(currentKey, callback) {
    cb = callback || null;
    selectedKey = currentKey || null;
    query = '';
    const inp = $g('#gpSearch'); if (inp) inp.value = '';
    editKey = null;
    $g('#gpAddPanel').hidden = true; resetAdd();
    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    render();
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.GpuPicker = { open };
})(window);
