/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   apppicker.js — "Pick from installed apps" bottom-sheet
   Simple list: search + tappable rows (icon + label + package). Apps come from
   COPG.listInstalledApps() (ksu.listPackages → pm fallback) with real labels
   filled in afterwards via COPG.enrichApps() so the list shows immediately.
   Selecting a row calls back with {pkg,label}. Shares #sheetOverlay.
   window.AppPicker.open(cb)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--picker" id="appPickerSheet" role="dialog" aria-modal="true" aria-label="Installed apps">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="picker_title">Installed Apps</span>
        <button class="sheet-header__close" id="apClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="picker-search">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input type="search" id="apSearch" autocomplete="off" data-i18n-attr="placeholder:picker_search" placeholder="Search apps…" />
      </div>
      <div class="picker-controls">
        <div class="pk-filters" id="apFilters">
          <button type="button" class="pk-chip" data-filter="all" data-i18n="filter_all">All</button>
          <button type="button" class="pk-chip selected" data-filter="user" data-i18n="filter_user">User</button>
          <button type="button" class="pk-chip" data-filter="system" data-i18n="filter_system">System</button>
        </div>
        <button type="button" class="pk-sort" id="apSortBtn" aria-label="Sort A→Z / Z→A">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 5v14"/><path d="M4 8l3-3 3 3"/><path d="M14 7h6M14 12h4M14 17h2"/></svg>
          <span id="apSortLabel" data-i18n="sort_name">Name (A→Z)</span>
        </button>
      </div>
      <div class="picker-userbar" id="apUserBar" hidden>
        <span class="picker-userbar__label" data-i18n="picker_user_for">Apps for</span>
        <div class="pk-drawer" id="apUserDrawer">
          <button type="button" class="pk-drawer__btn" id="apUserBtn" aria-haspopup="listbox" aria-expanded="false">
            <span id="apUserBtnText" data-i18n="pkg_user_all">All users</span>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          <div class="pk-drawer__menu" id="apUserMenu" role="listbox" hidden></div>
        </div>
      </div>
      <div class="picker-body">
        <div class="picker-list" id="apList"></div>
        <div class="picker-empty" id="apEmpty" hidden>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8.5" y1="11" x2="13.5" y2="11"/></svg>
          <span id="apEmptyMsg"></span>
        </div>
      </div>
    </div>`);

  const $a = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('appPickerSheet');

  let cb = null;
  let loaded = false;
  let rowData = [];            // [{el, label, search, system, user, scope}]
  let query = '';
  let filter = 'user';         // 'all' | 'user' | 'system' (default: user apps) — app TYPE
  let sortDir = 'az';          // 'az' | 'za'
  let userFilter = '';         // '' = all Android users | '0' owner | '10'/'999'/… a clone
  let usersList = [];          // [{id,name}] from COPG.getInstalledUsers()
  let cloneNames = {};         // "pkg@u<id>" -> per-clone desktop label ("设备模拟检测 1") from the probe

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  $a('#apClose').addEventListener('click', close);

  /* Centered empty/no-results state — toggled WITHOUT touching the row list, so
     the picker never collapses or reloads. */
  function showEmpty(msg) { $a('#apEmptyMsg').textContent = msg; $a('#apEmpty').hidden = false; }
  function hideEmpty() { $a('#apEmpty').hidden = true; }

  /* Clean-package names already present in the config (any device profile). Used to
     tint picker rows green so the user sees at a glance which apps are already added. */
  function addedSet() {
    // Keyed by "clean|scope" so a package added for ONE user only greens THAT user's row,
    // not the owner / other-user rows of the same package.
    try { return new Set((w.COPG && COPG.listPackages ? COPG.listPackages() : [])
      .map(p => p.clean + '|' + (p.userScope || ''))); }
    catch (e) { return new Set(); }
  }
  /* Toggle the "already added" green state on every row against the current config
     (re-evaluated on each open, so a just-added package shows up next time). */
  function markAdded() {
    const added = addedSet();
    rowData.forEach(r => r.el.classList.toggle('app-row--added',
      added.has(r.el.dataset.pkg + '|' + (r.el.dataset.scope || ''))));
  }

  /* Humanize a package with no launcher label (system components like
     com.android.health.connect.backuprestore have no android:label → loadLabel returns the
     package). Strip the common vendor prefix, title-case the remaining segments. The raw
     package still shows on the row's second line, so nothing is lost. */
  function prettyPkg(pkg) {
    let s = String(pkg || '');
    s = s.replace(/^com\.google\.android\./, '').replace(/^com\.android\./, '').replace(/^com\./, '');
    const words = s.split(/[._]/).filter(Boolean);
    if (!words.length) return pkg;
    return words.map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
  }

  function render(apps) {
    const list = $a('#apList');
    list.innerHTML = '';
    rowData = [];
    if (!apps.length) {
      showEmpty(I18N.t(COPG.hasBridge() ? 'picker_empty' : 'picker_preview'));
      return;
    }
    hideEmpty();
    const frag = document.createDocumentFragment();
    // Per Android-user expansion: an app installed for the owner shows as a NORMAL row
    // (scope '' = all users); an app that also (or only) exists in a clone / work-profile
    // user gets an extra row per user, badged "User N" and returning that user's scope.
    // On a device with no clones every app has just its owner row → identical to before.
    const variantsFor = app => {
      const uids = (w.COPG && COPG.usersForPackage) ? COPG.usersForPackage(app.pkg) : [];
      // Owner scope: when the device has ANY clone / secondary user, scope the owner variant to
      // u=0 (owner-ONLY) so a spoof picked for the owner does NOT leak to the clones. On a plain
      // single-user phone keep '' (owner == all users there) so no stray u= tag is written —
      // back-compat, no behaviour change. This is the fix for "spoofing the owner also spoofs the
      // clone": without it the owner row emitted '' (= all users) and every clone inherited it.
      const ownerScope = usersList.some(u => u.id !== 0) ? '0' : '';
      if (!uids.length) return [{ user: 0, scope: ownerScope }];  // preview / unknown → normal row
      const out = [];
      if (uids.indexOf(0) !== -1) out.push({ user: 0, scope: ownerScope });
      uids.filter(u => u !== 0).forEach(u => out.push({ user: u, scope: String(u) }));
      return out.length ? out : [{ user: uids[0], scope: String(uids[0]) }];  // clone-only app
    };
    // Badge text for a clone user = the user ID ("User 900"). Many ROMs name every clone profile the
    // SAME ("应用分身" / "App clone"), so the profile name can't tell clones apart — the id always can.
    const userName = id => I18N.t('tag_user').replace('%s', id);
    apps.forEach(app => {
      const base = (app.label && app.label !== app.pkg) ? app.label : prettyPkg(app.pkg);
      variantsFor(app).forEach(v => {
        // Prefer the per-clone DESKTOP label ("设备模拟检测 1/2", launcher label via the probe) so a
        // clone row shows its real distinct name; fall back to the base app label.
        const label = (v.user > 0 && cloneNames[app.pkg + '@u' + v.user]) || base;
        const row = document.createElement('button');
        row.type = 'button';
        row.className = 'app-row';
        row.dataset.pkg = app.pkg;
        row.dataset.user = String(v.user);
        row.dataset.scope = v.scope;
        const badge = v.user !== 0 ? `<span class="app-row__user"></span>` : '';
        row.innerHTML =
          `<span class="app-row__icon" data-icon></span>` +
          `<span class="app-row__text"><span class="app-row__label"></span><span class="app-row__pkg"></span>${badge}</span>`;
        row.querySelector('.app-row__label').textContent = label;
        row.querySelector('.app-row__pkg').textContent = app.pkg;
        // Badge = the clone/profile name, disambiguated by id when repeated (textContent: untrusted).
        if (v.user !== 0) { const b = row.querySelector('.app-row__user'); if (b) b.textContent = userName(v.user); }
        if (w.Icons) Icons.load(row.querySelector('[data-icon]'), app.pkg, label);
        // Click handled by ONE delegated listener on #apList (see below) — a per-row listener
        // ×hundreds is a real build cost + memory on the weaker KsuWebUI WebView.
        frag.appendChild(row);
        rowData.push({ el: row, label, search: (label + ' ' + app.pkg + ' ' + (v.user ? userName(v.user) : '')).toLowerCase(),
                       system: !!app.system, user: v.user, scope: v.scope });
      });
    });
    list.appendChild(frag);
    sortRows();
    applyView();
    markAdded();
  }

  function matchesFilter(r) {
    // Android-user scope axis first ('' = all users): a specific user shows only its rows.
    if (userFilter !== '' && String(r.user) !== userFilter) return false;
    if (filter === 'all')    return true;
    if (filter === 'system') return r.system;
    return !r.system;                 // 'user' (app TYPE)
  }

  /* Build the user-scope drawer (کشو) from the detected Android users; only meaningful when
     the device has a clone / work-profile / dual-app user beyond the owner. Hidden otherwise. */
  function buildUserBar() {
    const bar = $a('#apUserBar'), menu = $a('#apUserMenu');
    if (!bar || !menu) return;
    const hasClones = usersList.some(u => u.id !== 0);
    bar.hidden = !hasClones;
    if (!hasClones) { userFilter = ''; return; }
    const opts = [{ v: '', label: I18N.t('pkg_user_all') }]
      .concat(usersList.map(u => ({
        v: String(u.id),
        label: u.id === 0 ? I18N.t('pkg_user_owner') : (u.id + (u.name ? ' · ' + u.name : '')),
      })));
    menu.innerHTML = opts.map(o =>
      `<button type="button" class="pk-drawer__opt${o.v === userFilter ? ' selected' : ''}" data-user="${o.v}">${o.label.replace(/</g, '&lt;')}</button>`).join('');
    setUserFilter(userFilter);   // sync the button label
  }
  function setUserFilter(v) {
    userFilter = v || '';
    const btnText = $a('#apUserBtnText');
    if (btnText) {
      const o = usersList.find(u => String(u.id) === userFilter);
      btnText.textContent = userFilter === '' ? I18N.t('pkg_user_all')
        : (userFilter === '0' ? I18N.t('pkg_user_owner') : (userFilter + (o && o.name ? ' · ' + o.name : '')));
    }
    const menu = $a('#apUserMenu');
    if (menu) menu.querySelectorAll('.pk-drawer__opt').forEach(b => b.classList.toggle('selected', b.dataset.user === userFilter));
  }
  function toggleDrawer(openIt) {
    const dr = $a('#apUserDrawer'), menu = $a('#apUserMenu'), btn = $a('#apUserBtn');
    if (!dr || !menu) return;
    const show = openIt == null ? menu.hidden : openIt;
    menu.hidden = !show;
    dr.classList.toggle('open', show);
    if (btn) btn.setAttribute('aria-expanded', String(show));
  }

  /* Search + filter — show/hide rows in place, never reload. */
  function applyView() {
    let visible = 0;
    rowData.forEach(r => {
      const show = (!query || r.search.includes(query)) && matchesFilter(r);
      r.el.style.display = show ? '' : 'none';
      if (show) visible++;
    });
    if (rowData.length && visible === 0) showEmpty(I18N.t('picker_no_results'));
    else hideEmpty();
  }

  /* Re-order the DOM rows (and rowData) by label, honouring sortDir. */
  function sortRows() {
    const dir = sortDir === 'za' ? -1 : 1;
    rowData.sort((a, b) => dir * a.label.toLowerCase().localeCompare(b.label.toLowerCase()));
    const list = $a('#apList');
    rowData.forEach(r => list.appendChild(r.el));
  }

  function updateSortLabel() {
    const span = $a('#apSortLabel');
    if (!span) return;
    const key = sortDir === 'az' ? 'sort_name' : 'sort_name_za';
    span.dataset.i18n = key;          // keep data-i18n in sync so I18N.apply() is correct
    span.textContent = I18N.t(key);
  }

  function setFilter(f) {
    filter = f;
    const fz = $a('#apFilters');
    if (fz) fz.querySelectorAll('.pk-chip').forEach(c => c.classList.toggle('selected', c.dataset.filter === f));
  }

  async function loadList() {
    const list = $a('#apList');
    hideEmpty();
    list.innerHTML = '<div class="picker-loading"><span class="spin"></span></div>';
    if (window.logEvent) logEvent('App picker: loading installed apps…', 'info');
    let apps = [];
    try {
      apps = (w.COPG && COPG.listInstalledApps) ? await COPG.listInstalledApps() : [];
    } catch (err) {
      if (window.logEvent) logEvent('App picker: load failed — ' + err, 'error');
    }
    // Android users must be known BEFORE render so per-user rows + names are built (getInstalledPackages,
    // called inside listInstalledApps above, already populated installedByUser / usersForPackage).
    try { usersList = (w.COPG && COPG.getInstalledUsers) ? await COPG.getInstalledUsers() : [{ id: 0, name: 'Owner' }]; }
    catch (_) { usersList = [{ id: 0, name: 'Owner' }]; }
    buildUserBar();
    // Paint NOW with the base labels (list.json + prettyPkg fallback) so the list appears instantly
    // instead of after the multi-second enrich await. Real labels + per-clone names are fetched in the
    // BACKGROUND and patched into the existing rows in place (enrichInBackground → patchLabels), then a
    // single re-sort/re-filter settles it — no blank spinner wait. (The one cost: the User/System filter
    // + A→Z order may settle once when enrich lands; the list is usable the whole time.)
    render(apps);
    loaded = true;
    if (window.logEvent) logEvent('App picker: ' + apps.length + ' apps shown; enriching…', apps.length ? 'success' : 'warn');
    enrichInBackground(apps);
  }

  /* Real labels + per-clone desktop names, fetched AFTER the first paint. Patched into the already-
     rendered rows in place — no blank wait, no full re-render (which would flash + reset scroll). */
  async function enrichInBackground(apps) {
    if (!apps.length) return;
    let changed = false;
    if (w.COPG && COPG.enrichApps) {
      try {
        const data = await COPG.enrichApps(apps.map(a => a.pkg));
        const labels = (data && data.labels) || {};
        const sys = (data && data.system) || null;
        apps.forEach(a => {
          if (labels[a.pkg] && labels[a.pkg] !== a.label) { a.label = labels[a.pkg]; changed = true; }
          if (sys) a.system = sys.has(a.pkg);
        });
        if (sys) changed = true;   // system flags may have arrived → re-filter
      } catch (err) { if (window.logEvent) logEvent('App picker: enrich failed — ' + err, 'warn'); }
    }
    if (w.COPG && COPG.probeLabelsForUser && COPG.usersForPackage) {
      const byUser = new Map();
      apps.forEach(a => (COPG.usersForPackage(a.pkg) || []).forEach(u => {
        if (u > 0 && !cloneNames[a.pkg + '@u' + u]) {
          if (!byUser.has(u)) byUser.set(u, new Set());
          byUser.get(u).add(a.pkg);
        }
      }));
      for (const [u, set] of byUser) {
        try {
          const o = await COPG.probeLabelsForUser([...set], u);
          for (const p of Object.keys(o)) if (o[p]) { cloneNames[p + '@u' + u] = o[p]; changed = true; }
        } catch (_) {}
      }
    }
    if (changed) patchLabels(apps);
    if (window.logEvent) logEvent('App picker: enrich done.', 'info');
  }

  /* Update label + system flag on the already-rendered rows in place, then re-sort/re-filter once. */
  function patchLabels(apps) {
    const byPkg = {};
    apps.forEach(a => { byPkg[a.pkg] = a; });
    rowData.forEach(r => {
      const a = byPkg[r.el.dataset.pkg];
      if (!a) return;
      const base = (a.label && a.label !== a.pkg) ? a.label : prettyPkg(a.pkg);
      const label = (r.user > 0 && cloneNames[a.pkg + '@u' + r.user]) || base;
      const lblEl = r.el.querySelector('.app-row__label');
      if (lblEl && lblEl.textContent !== label) {
        lblEl.textContent = label;
        r.label = label;
        r.search = (label + ' ' + a.pkg + ' ' + (r.user ? 'user ' + r.user : '')).toLowerCase();
      }
      r.system = !!a.system;
    });
    sortRows(); applyView(); markAdded();
  }

  /* ONE delegated row click for the whole list (replaces a per-row listener). */
  $a('#apList').addEventListener('click', e => {
    const row = e.target.closest('.app-row'); if (!row) return;
    const fn = cb; cb = null;
    const pkg = row.dataset.pkg;
    const lbl = row.querySelector('.app-row__label')?.textContent || pkg;
    close();
    if (fn) fn({ pkg, label: lbl, userScope: row.dataset.scope || '' });
  });

  $a('#apSearch').addEventListener('input', e => { query = e.target.value.trim().toLowerCase(); applyView(); });

  /* Filter chips (All / User / System) — instant, preserves search + scroll. */
  $a('#apFilters').addEventListener('click', e => {
    const chip = e.target.closest('.pk-chip');
    if (!chip) return;
    setFilter(chip.dataset.filter);
    applyView();
  });

  /* Sort toggle (Name A→Z ⇄ Z→A). */
  $a('#apSortBtn').addEventListener('click', () => {
    sortDir = sortDir === 'az' ? 'za' : 'az';
    updateSortLabel();
    sortRows();
    applyView();
  });

  /* User-scope drawer (کشو): open/close + pick a user → filter rows in place. */
  $a('#apUserBtn').addEventListener('click', e => { e.stopPropagation(); toggleDrawer(); });
  $a('#apUserMenu').addEventListener('click', e => {
    const opt = e.target.closest('.pk-drawer__opt'); if (!opt) return;
    setUserFilter(opt.dataset.user); toggleDrawer(false); applyView();
  });
  document.addEventListener('click', e => {           // close the drawer on an outside tap
    const dr = $a('#apUserDrawer'); if (dr && !dr.contains(e.target)) toggleDrawer(false);
  });

  function open(callback) {
    cb = callback || null;
    query = '';
    setFilter('user');                 // default app-TYPE filter = user apps
    userFilter = '';                   // default Android-user scope = all users
    toggleDrawer(false);
    sortDir = 'az'; updateSortLabel();
    const s = sheet();
    const inp = $a('#apSearch'); if (inp) inp.value = '';
    overlay()?.classList.add('visible');
    // Clear any residual inline transform/transition left by a drag-to-dismiss
    // gesture. An inline `transform:translateY(Npx)` BEATS the `.bottom-sheet.open`
    // stylesheet rule, so without this the sheet would stay off-screen and could
    // never visually reopen after the first drag (no error, no log → "nothing happens").
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    requestAnimationFrame(() => s && s.classList.add('open'));
    if (window.logEvent) logEvent('App picker opened.', 'info');
    if (!loaded) loadList();
    else { buildUserBar(); sortRows(); applyView(); markAdded(); }    // reuse cached rows (no reload); refresh drawer + added-state
  }

  w.AppPicker = { open };
})(window);
