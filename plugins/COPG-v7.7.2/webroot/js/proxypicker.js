/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   proxypicker.js — "Choose a proxy" bottom-sheet
   Parallel to carrierpicker.js but for per-app proxy profiles (COPG.getProxyModels()):
   search + tappable rows (globe avatar + proxy name + host:port), current selection marked,
   per-row edit/delete, and a + add-panel to append a custom proxy (name/host/port/user/pass →
   PROXY/manifest.json). Proxies are user-specific, so there are no built-ins — the list starts
   empty and the user adds their own. Selecting a row calls back with {key, name}.
   window.ProxyPicker.open(currentKey, cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const NET_SVG  = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18"/></svg>';
  const CHECK_SVG = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
  const TRASH_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';
  const EDIT_SVG  = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="proxyPickerSheet" role="dialog" aria-modal="true" aria-label="Proxies">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="proxypicker_title">Proxies</span>
        <button class="sheet-header__close" id="pxClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-searchrow">
        <div class="picker-search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="search" id="pxSearch" autocomplete="off" data-i18n-attr="placeholder:proxypicker_search" placeholder="Search proxies…" />
        </div>
        <button type="button" class="picker-add" id="pxAddBtn" data-i18n-attr="aria-label:proxy_add_btn" aria-label="Add proxy">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
      <div class="picker-import__panel" id="pxAddPanel" hidden>
        <div class="picker-import__title" data-i18n="proxy_add_title">Add proxy…</div>
        <label class="field"><span class="field__label" data-i18n="proxy_add_name_lbl">Name</span>
        <input type="text" id="pxAddName" class="field__input" autocomplete="off" data-i18n-attr="placeholder:proxy_add_name_ph" placeholder="e.g. US Residential" /></label>
        <label class="field"><span class="field__label" data-i18n="proxy_add_type_lbl">Type</span>
        <div class="seg" id="pxAddType" data-type="socks5">
          <button type="button" class="seg__opt active" data-type="socks5">SOCKS5</button>
          <button type="button" class="seg__opt" data-type="http">HTTP</button>
          <button type="button" class="seg__opt" data-type="socks4">SOCKS4</button>
        </div></label>
        <div class="sim-add__grid">
          <label class="field" style="grid-column:span 2"><span class="field__label" data-i18n="proxy_add_host_lbl">Host / IP</span>
          <input type="text" id="pxAddHost" class="field__input field__input--mono" autocomplete="off" data-i18n-attr="placeholder:proxy_add_host_ph" placeholder="1.2.3.4" /></label>
          <label class="field"><span class="field__label" data-i18n="proxy_add_port_lbl">Port</span>
          <input type="text" id="pxAddPort" class="field__input field__input--mono" inputmode="numeric" maxlength="5" autocomplete="off" data-i18n-attr="placeholder:proxy_add_port_ph" placeholder="1080" /></label>
        </div>
        <div class="sim-add__grid">
          <label class="field"><span class="field__label" data-i18n="proxy_add_user_lbl">Username</span>
          <input type="text" id="pxAddUser" class="field__input" autocomplete="off" data-i18n-attr="placeholder:proxy_add_opt_ph" placeholder="(optional)" /></label>
          <label class="field"><span class="field__label" data-i18n="proxy_add_pass_lbl">Password</span>
          <input type="text" id="pxAddPass" class="field__input" autocomplete="off" data-i18n-attr="placeholder:proxy_add_opt_ph" placeholder="(optional)" /></label>
        </div>
        <div class="picker-import__hint" data-i18n="proxy_add_hint">SOCKS5 only for now. The app routes through this proxy at kernel level — no VPN, no interface for the app to see. Leave user/password empty for an open proxy.</div>
        <div class="picker-import__err" id="pxAddErr" hidden></div>
        <button type="button" class="picker-import__save" id="pxAddSave" data-i18n="proxy_add_save">Save proxy</button>
      </div>
      <div class="picker-body">
        <div class="picker-list" id="pxList"></div>
        <div class="picker-empty" id="pxEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M3 12h18"/></svg>
          <span id="pxEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $c = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('proxyPickerSheet');

  let cb = null, rowData = [], query = '', selectedKey = null, editKey = null;

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $c('#pxClose').addEventListener('click', close);

  function showEmpty(msg) { $c('#pxEmptyMsg').textContent = msg; $c('#pxEmpty').hidden = false; }
  function hideEmpty() { $c('#pxEmpty').hidden = true; }

  async function render() {
    const list = $c('#pxList');
    list.innerHTML = '';
    rowData = [];
    const models = (w.COPG && COPG.getProxyModels) ? await COPG.getProxyModels() : [];
    const frag = document.createDocumentFragment();
    // "None" — clears the proxy (app goes direct). Always first.
    {
      const none = document.createElement('button');
      none.type = 'button';
      none.className = 'app-row dev-row sim-row--none';
      none.dataset.key = '';
      none.classList.toggle('is-selected', !selectedKey);
      none.innerHTML =
        `<span class="app-row__icon dev-row__avatar">${NET_SVG}</span>` +
        `<span class="app-row__text"><span class="app-row__label"></span></span>` +
        `<span class="dev-row__check">${CHECK_SVG}</span>`;
      none.querySelector('.app-row__label').textContent = I18N.t('proxy_none');
      none.addEventListener('click', () => { const fn = cb; cb = null; close(); if (fn) fn({ key: '', name: '' }); });
      frag.appendChild(none);
      rowData.push({ el: none, search: '' });
    }
    if (!models.length) { list.appendChild(frag); showEmpty(I18N.t('proxypicker_empty')); return; }
    hideEmpty();
    models.forEach(m => {
      const isSel = (m.key === selectedKey);
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'app-row dev-row';
      row.dataset.key = m.key;
      row.classList.toggle('is-selected', isSel);
      row.innerHTML =
        `<span class="app-row__icon dev-row__avatar">${NET_SVG}</span>` +
        `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span></span>` +
        `<span class="app-row__edit" role="button" tabindex="0" data-i18n-attr="aria-label:profile_edit" aria-label="Edit profile">${EDIT_SVG}</span>` +
        `<span class="app-row__del" role="button" tabindex="0" data-i18n-attr="aria-label:profile_delete" aria-label="Delete profile">${TRASH_SVG}</span>` +
        `<span class="dev-row__check">${CHECK_SVG}</span>`;
      row.querySelector('.app-row__label').textContent = m.name;
      row.querySelector('.app-row__pkg').textContent =
        `${(m.type || 'socks5').toUpperCase()} · ${m.host}:${m.port}${m.user ? ' · ' + m.user : ''}`;
      row.addEventListener('click', () => { const fn = cb; cb = null; close(); if (fn) fn({ key: m.key, name: m.name }); });
      const edit = row.querySelector('.app-row__edit');
      if (edit) edit.addEventListener('click', e => { e.preventDefault(); e.stopPropagation(); openEdit(m); });
      const del = row.querySelector('.app-row__del');
      if (del) del.addEventListener('click', e => {
        e.preventDefault(); e.stopPropagation();
        if (!(w.Modals && Modals.confirmDelete)) return;
        Modals.confirmDelete(I18N.t('proxy_del_title'),
          I18N.t('proxy_del_msg').replace('%s', m.name), async () => {
            try { await COPG.deleteProxyModel(m.key); if (typeof showToast === 'function') showToast(I18N.t('proxy_del_done')); }
            catch (err) { if (typeof showToast === 'function') showToast(I18N.t('proxy_del_err')); }
            render();
          });
      });
      frag.appendChild(row);
      rowData.push({ el: row, search: (m.name + ' ' + m.key + ' ' + m.host + ' ' + m.port).toLowerCase() });
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

  $c('#pxSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  const ADD_ERR = {
    name_required: 'proxy_add_err_name', bad_name: 'proxy_add_err_name',
    bad_host: 'proxy_add_err_host', bad_port: 'proxy_add_err_port', exists: 'proxy_add_err_exists',
  };
  function addErr(msg) { const e = $c('#pxAddErr'); e.textContent = msg; e.hidden = false; }
  function hideAddErr() { $c('#pxAddErr').hidden = true; }
  // Type segmented control (in-app, styled) — replaces the native <select> that rendered as
  // the OS webview picker. Value lives in the container's data-type; each button toggles .active.
  function setType(t) {
    const seg = $c('#pxAddType'); if (!seg) return;
    t = ['socks5', 'http', 'socks4'].indexOf(String(t || '').toLowerCase()) >= 0
      ? String(t).toLowerCase() : 'socks5';
    seg.dataset.type = t;
    seg.querySelectorAll('.seg__opt').forEach(b => b.classList.toggle('active', b.dataset.type === t));
  }
  $c('#pxAddType').querySelectorAll('.seg__opt').forEach(b =>
    b.addEventListener('click', () => setType(b.dataset.type)));
  function resetAdd() {
    $c('#pxAddName').value = ''; $c('#pxAddHost').value = ''; $c('#pxAddPort').value = ''; setType('socks5');
    $c('#pxAddUser').value = ''; $c('#pxAddPass').value = ''; hideAddErr();
  }
  function setPanelMode() {
    const title = $c('#pxAddPanel .picker-import__title');
    if (title) title.textContent = I18N.t(editKey ? 'proxy_edit_title' : 'proxy_add_title');
    const save = $c('#pxAddSave');
    if (save) save.textContent = I18N.t(editKey ? 'proxy_edit_save' : 'proxy_add_save');
  }
  function openAdd() { editKey = null; resetAdd(); setPanelMode(); $c('#pxAddPanel').hidden = false; }
  function openEdit(m) {
    editKey = m.key; hideAddErr();
    $c('#pxAddName').value = m.name || ''; setType(m.type || 'socks5'); $c('#pxAddHost').value = m.host || '';
    $c('#pxAddPort').value = m.port || ''; $c('#pxAddUser').value = m.user || ''; $c('#pxAddPass').value = m.pass || '';
    setPanelMode();
    $c('#pxAddPanel').hidden = false;
    $c('#pxAddPanel').scrollIntoView({ block: 'nearest' });
  }
  $c('#pxAddBtn').addEventListener('click', () => {
    const panel = $c('#pxAddPanel');
    if (panel.hidden) openAdd();
    else { panel.hidden = true; editKey = null; }
  });
  $c('#pxAddSave').addEventListener('click', async () => {
    const o = {
      name: $c('#pxAddName').value.trim(), type: $c('#pxAddType').dataset.type || 'socks5', host: $c('#pxAddHost').value.trim(),
      port: $c('#pxAddPort').value.trim(), user: $c('#pxAddUser').value.trim(), pass: $c('#pxAddPass').value.trim(),
    };
    const btn = $c('#pxAddSave'); btn.disabled = true;
    try {
      if (editKey) {
        const wasSel = (editKey === selectedKey);
        const res = await COPG.updateProxyModel(editKey, o);
        editKey = null; $c('#pxAddPanel').hidden = true; resetAdd(); setPanelMode(); render();
        if (wasSel && cb) cb({ key: res.key, name: res.name });
        if (typeof showToast === 'function') showToast(I18N.t('proxy_edit_done'));
      } else {
        const res = await COPG.importProxyModel(o);
        $c('#pxAddPanel').hidden = true; resetAdd();
        const fn = cb; cb = null; close();
        if (fn) fn({ key: res.key, name: res.name });
        if (typeof showToast === 'function') showToast(I18N.t('proxy_add_done'));
      }
    } catch (err) {
      addErr(I18N.t(ADD_ERR[String(err && err.message)] || 'proxy_add_err_generic'));
    } finally { btn.disabled = false; }
  });

  function open(currentKey, callback) {
    cb = callback || null;
    selectedKey = currentKey || null;
    query = ''; editKey = null;
    const inp = $c('#pxSearch'); if (inp) inp.value = '';
    $c('#pxAddPanel').hidden = true; resetAdd(); setPanelMode();
    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    render();
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.ProxyPicker = { open };
})(window);
