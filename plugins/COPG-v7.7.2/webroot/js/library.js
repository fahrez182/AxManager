/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   library.js — Library page: devices & packages from COPG.json
   Real data via COPG.* (js/copg-data.js); CRUD modals in js/modals.js;
   icons in js/icons.js; app picker in js/apppicker.js.
   Adds: icons, installed badges, sort + filter, swipe between tabs.
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */

/* current search / sort / filter per panel */
const LibQuery = {
  devices: '', packages: '',
  devSort: 'default',  // default | name | apps | brand
  pkgSort: 'default',  // default | name | type | installed
  pkgFilter: 'all',    // all | installed | blocked | cpu_only
  devBrand: 'all',     // 'all' | lowercased brand — Devices-tab brand filter
};

const installedSet = () => (window.COPG && COPG.installedSet) ? COPG.installedSet : new Set();

/* ─── Multi-select via long-press (shared by Packages + Devices) ─────────────
   The list is NOT re-rendered when selection changes — cards are mutated in
   place (add/remove the checkbox, toggle .is-selected). Re-rendering would
   re-run Icons.load on every card → visible icon flash. makeSelectCtl builds
   one controller per list; wireSelectable binds a card to it. */
let lpSuppress = false;                    // swallow the click that ends a long-press
const SVG_CHECK = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>`;
const SVG_XMARK = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`;

function makeSelectCtl(o) {
  // o: { list:()=>el, rows:()=>arr, uid, canSelect, open, onDelete, barId, keys:{delTitle,delMsg} }
  const sel = new Map();
  let mode = false, bar = null;
  const ctl = {
    get mode() { return mode; },
    canSelect: o.canSelect,
    open: o.open,
    ensureBar() {
      if (bar) return bar;
      const page = $('#pageLibrary'); if (!page) return null;
      page.insertAdjacentHTML('beforeend', `
        <div class="pkg-selbar" id="${o.barId}" hidden role="toolbar" aria-label="Selection actions">
          <button class="pkg-selbar__x" data-x aria-label="Cancel">${SVG_XMARK}</button>
          <span class="pkg-selbar__count" data-count>0</span>
          <button class="pkg-selbar__all" data-all type="button" data-i18n="sel_all">Select all</button>
          <div class="pkg-selbar__spacer"></div>
          ${o.onEdit ? '<button class="pkg-selbar__act" data-edit type="button" data-i18n="sel_edit">Edit together</button>' : ''}
          <button class="pkg-selbar__act pkg-selbar__act--danger" data-del type="button" data-i18n="sel_delete">Delete</button>
        </div>`);
      bar = $('#' + o.barId);
      bar.querySelector('[data-x]').addEventListener('click', () => ctl.exit());
      bar.querySelector('[data-all]').addEventListener('click', () => ctl.all());
      bar.querySelector('[data-del]').addEventListener('click', () => ctl.del());
      const eb = bar.querySelector('[data-edit]');
      if (eb) eb.addEventListener('click', () => ctl.edit());
      return bar;
    },
    enter(it) {
      mode = true; sel.clear();
      if (it && o.canSelect(it)) sel.set(o.uid(it), it);
      if (window.haptic) haptic(18);
      ctl.applyDOM(); ctl.updateBar();
    },
    exit() { if (!mode) return; mode = false; sel.clear(); ctl.applyDOM(); ctl.updateBar(); },
    toggle(it, card) {
      const u = o.uid(it);
      if (sel.has(u)) { sel.delete(u); card.classList.remove('is-selected'); }
      else { sel.set(u, it); card.classList.add('is-selected'); }
      if (window.haptic) haptic(8);
      if (mode && sel.size === 0) { ctl.exit(); return; }   // deselect all ⇒ leave mode
      ctl.updateBar();
    },
    all() {
      const rows = o.rows().filter(o.canSelect);
      const allSel = rows.length && rows.every(r => sel.has(o.uid(r)));
      sel.clear();
      if (!allSel) rows.forEach(r => sel.set(o.uid(r), r));
      ctl.applyDOM(); ctl.updateBar();
    },
    // Mutate the currently-rendered cards in place — no re-render, so icons stay put.
    applyDOM() {
      const list = o.list(); if (!list) return;
      list.classList.toggle('selecting', mode);
      [...list.children].forEach(card => {
        const it = card._selItem; if (!it) return;
        let cb = card.querySelector(':scope > .pkg-check');
        if (mode && o.canSelect(it)) {
          if (!cb) { cb = document.createElement('span'); cb.className = 'pkg-check'; cb.innerHTML = SVG_CHECK; card.insertBefore(cb, card.firstChild); }
          card.classList.toggle('is-selected', sel.has(o.uid(it)));
        } else {
          if (cb) cb.remove();
          card.classList.remove('is-selected');
        }
      });
    },
    updateBar() {
      const b = ctl.ensureBar(); if (!b) return;
      b.hidden = !mode;
      const n = sel.size;
      const c = b.querySelector('[data-count]'); if (c) c.textContent = I18N.t('sel_count').replace('%d', n);
      const d = b.querySelector('[data-del]'); if (d) d.disabled = n === 0;
      const ed = b.querySelector('[data-edit]'); if (ed) ed.disabled = n === 0;
    },
    edit() {
      const items = [...sel.values()];
      if (items.length && o.onEdit) o.onEdit(items);
    },
    del() {
      const items = [...sel.values()]; if (!items.length) return;
      Modals.confirmDelete(
        I18N.t(o.keys.delTitle),
        I18N.t(o.keys.delMsg).replace('%d', items.length),
        async () => { mode = false; sel.clear(); ctl.updateBar(); await o.onDelete(items); }
      );
    },
  };
  return ctl;
}

// Bind one card to its select controller: long-press to enter, tap to toggle/open.
function wireSelectable(ctl, card, item) {
  card._selItem = item;
  let t = null, x = 0, y = 0;
  const clr = () => { if (t) { clearTimeout(t); t = null; } };
  card.addEventListener('pointerdown', e => {
    if (ctl.mode) return;                        // in select mode a press is just a tap-toggle
    if (e.button != null && e.button !== 0) return;
    if (!ctl.canSelect(item)) return;            // non-selectable (e.g. This Device) can't start select
    x = e.clientX; y = e.clientY; clr();
    t = setTimeout(() => {
      t = null;
      lpSuppress = true; setTimeout(() => { lpSuppress = false; }, 700);
      ctl.enter(item);
    }, 600);
  });
  card.addEventListener('pointermove', e => {
    if (t && (Math.abs(e.clientX - x) > 8 || Math.abs(e.clientY - y) > 8)) clr();
  });
  card.addEventListener('pointerup', clr);
  card.addEventListener('pointercancel', clr);
  card.addEventListener('click', e => {
    if (lpSuppress) return;                       // the click that ended the long-press
    if (ctl.mode) { if (ctl.canSelect(item)) ctl.toggle(item, card); return; }
    spawnRipple(e, card); ctl.open(item);
  });
}

const PkgSel = makeSelectCtl({
  list: () => $('#packageList'),
  rows: () => lastPkgRows,
  uid: p => `${p.clean}\u0001${p.userScope}\u0001${p.deviceKey}`,
  canSelect: () => true,
  open: pkg => Modals.openPackage(pkg),
  onEdit: items => Modals.openPackageBatch(items),
  onDelete: async items => {
    items.forEach(it => COPG.deletePackage(it.clean, it.type, it.deviceKey, it.userScope));
    await persistAndRefresh(I18N.t('sel_toast_deleted').replace('%d', items.length));
  },
  barId: 'pkgSelBar',
  keys: { delTitle: 'sel_del_title', delMsg: 'sel_del_msg' },
});

const DevSel = makeSelectCtl({
  list: () => $('#deviceList'),
  rows: () => lastDevRows,
  uid: d => d.key,
  canSelect: d => !d.identity && !d.random,       // "This Device" + "Random" are built-in → never selectable/deletable
  open: dev => dev.random ? Modals.info({ title: I18N.t('dev_random'), message: I18N.t('dev_random_info') }) : Modals.openDevice(dev.key),
  onDelete: async items => {
    items.forEach(d => COPG.deleteDevice(d.key));
    await persistAndRefresh(I18N.t('sel_toast_deleted_dev').replace('%d', items.length));
  },
  barId: 'devSelBar',
  keys: { delTitle: 'sel_del_title_dev', delMsg: 'sel_del_msg_dev' },
});

let lastPkgRows = [], lastDevRows = [];

/* ─── Inner tab switch ─── */
function switchLibTab(tabKey) {
  if (tabKey === State.activeLibTab) return;
  PkgSel.exit(); DevSel.exit();                   // leaving a tab cancels its selection
  State.activeLibTab = tabKey;
  $$('.inner-tab').forEach(t => t.classList.toggle('active', t.dataset.libTab === tabKey));
  $$('.lib-panel').forEach(p => {
    const isActive = p.id === (tabKey === 'devices' ? 'libDevices' : 'libPackages');
    p.classList.toggle('active', isActive);
  });
}

$$('.inner-tab').forEach(tab => {
  tab.addEventListener('click', e => { spawnRipple(e, tab); switchLibTab(tab.dataset.libTab); });
});

/* ─── Empty-state helper ─── */
function libEmpty(key) {
  const div = document.createElement('div');
  div.className = 'lib-empty';
  div.textContent = I18N.t(key);
  return div;
}

/* ─── small inline SVG helpers ─── */
const SVG_EDIT  = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>`;
const SVG_TRASH = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>`;
const SVG_PHONE = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="5" y="2" width="14" height="20" rx="2"/><circle cx="12" cy="17" r="1" fill="currentColor"/></svg>`;
const SVG_LAUNCH = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="6 4 20 12 6 20 6 4"/></svg>`;
const SVG_CLEAR = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2"/><path d="M9.5 11l5 5m0-5l-5 5"/></svg>`;
const SVG_SORT  = `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h12M3 12h9M3 18h6"/><path d="M17 8V5m0 0 2.5 2.5M17 5l-2.5 2.5"/><path d="M17 16v3m0 0 2.5-2.5M17 19l-2.5-2.5"/></svg>`;
const SVG_SHARE = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.6" y1="13.5" x2="15.4" y2="17.5"/><line x1="15.4" y1="6.5" x2="8.6" y2="10.5"/></svg>`;

const chip = (cls, text) => { const s = document.createElement('span'); s.className = 'chip ' + cls; s.textContent = text; return s; };

/* ─── Sort / filter definitions ─── */
const SORT_OPTS = {
  devices:  [['default', 'sort_default'], ['name', 'sort_name'], ['name_desc', 'sort_name_za'], ['apps', 'sort_apps'], ['brand', 'sort_brand']],
  packages: [['default', 'sort_default'], ['name', 'sort_name'], ['name_desc', 'sort_name_za'], ['type', 'sort_type'], ['installed', 'sort_installed']],
};

function sortDevices(arr) {
  const a = [...arr];
  // 'default' keeps keyOrder; the others sort by value. Either way the built-in buckets
  // ("This Device" then "Random") are pinned to the top afterwards.
  if (LibQuery.devSort === 'name')       a.sort((x, y) => x.name.toLowerCase().localeCompare(y.name.toLowerCase()));
  else if (LibQuery.devSort === 'name_desc') a.sort((x, y) => y.name.toLowerCase().localeCompare(x.name.toLowerCase()));
  else if (LibQuery.devSort === 'apps')  a.sort((x, y) => y.gameCount - x.gameCount);
  else if (LibQuery.devSort === 'brand') a.sort((x, y) => (x.brand || '~').localeCompare(y.brand || '~'));
  // Pin built-in buckets on top for every sort (This Device = 0, Random = 1, rest = 2).
  // Array.sort is stable, so non-built-ins keep their just-computed relative order.
  const rank = d => d.identity ? 0 : (d.random ? 1 : 2);
  a.sort((x, y) => rank(x) - rank(y));
  return a;
}
function sortPackages(arr) {
  const set = installedSet();
  const a = [...arr];
  if (LibQuery.pkgSort === 'default') return a;
  // 'name'/'name_desc' sort by DISPLAY name (x.name), not the package id.
  if (LibQuery.pkgSort === 'name')   a.sort((x, y) => x.name.toLowerCase().localeCompare(y.name.toLowerCase()));
  else if (LibQuery.pkgSort === 'name_desc') a.sort((x, y) => y.name.toLowerCase().localeCompare(x.name.toLowerCase()));
  else if (LibQuery.pkgSort === 'type')   a.sort((x, y) => x.type.localeCompare(y.type));
  else if (LibQuery.pkgSort === 'installed') a.sort((x, y) => (set.has(y.clean) ? 1 : 0) - (set.has(x.clean) ? 1 : 0));
  return a;
}
function pkgMatchesFilter(p) {
  const set = installedSet();
  switch (LibQuery.pkgFilter) {
    case 'installed': return set.has(p.clean);
    case 'blocked':   return p.blocked;        // :blocked tag (force real CPU)
    case 'cpu_only':  return p.with_cpu;       // CPU spoof on (cpu=<key> / with_cpu)
    default:          return true;
  }
}

/* ─── Render Devices ─── */
function renderDevices() {
  const list = $('#deviceList');
  if (!list) return;
  const devices = COPG.listDevices();
  const cnt = $('#devTabCount');
  if (cnt) cnt.textContent = devices.length;

  const q = LibQuery.devices.trim().toLowerCase();
  let rows = q
    ? devices.filter(d => (d.name + ' ' + d.model + ' ' + d.brand).toLowerCase().includes(q))
    : devices;
  // Brand filter: keep only the chosen brand. This Device + Random are brandless built-ins and
  // always stay (hiding the user's own identity profile would be bad UX).
  if (LibQuery.devBrand !== 'all')
    rows = rows.filter(d => d.identity || d.random || (d.brand || '').toLowerCase() === LibQuery.devBrand);
  rows = sortDevices(rows);

  lastDevRows = rows;
  list.innerHTML = '';
  if (!rows.length) { list.appendChild(libEmpty('empty_devices')); return; }

  rows.forEach((dev, i) => {
    const card = document.createElement('div');
    card.className = 'device-card';
    card.style.animationDelay = `${Math.min(i * 45, 400)}ms`;
    // Built-in buckets: "This Device" (identity, editable, not deletable) and "Random"
    // (not a real device → not editable/deletable/shareable; edit opens an info popup).
    const builtin = dev.identity || dev.random;
    const DICE = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="8" cy="8" r="1.3" fill="currentColor"/><circle cx="16" cy="16" r="1.3" fill="currentColor"/><circle cx="16" cy="8" r="1.3" fill="currentColor"/><circle cx="8" cy="16" r="1.3" fill="currentColor"/></svg>';
    const delBtn = builtin ? '' : `<button class="card-btn card-btn--delete" aria-label="${I18N.t('act_delete')}">${SVG_TRASH}</button>`;
    const shareBtn = dev.random ? '' : `<button class="card-btn card-btn--share" aria-label="${I18N.t('act_share')}">${SVG_SHARE}</button>`;
    card.innerHTML = `
      <div class="device-card__avatar">${dev.random ? DICE : SVG_PHONE}</div>
      <div class="device-card__info">
        <div class="device-card__name"></div>
        <div class="device-card__meta"></div>
      </div>
      <div class="card-actions">
        ${shareBtn}
        <button class="card-btn card-btn--edit" aria-label="${I18N.t('act_edit')}">${SVG_EDIT}</button>
        ${delBtn}
      </div>`;
    card.querySelector('.device-card__name').textContent = dev.name;
    card.querySelector('.device-card__meta').textContent = dev.random
      ? `${I18N.t('dev_random_sub')} · ${dev.gameCount} ${I18N.t('dev_apps')}`
      : dev.identity
        ? `${I18N.t('dev_this_device_sub')} · ${dev.gameCount} ${I18N.t('dev_apps')}`
        : `${I18N.t('dev_model')}: ${dev.model} · ${dev.gameCount} ${I18N.t('dev_apps')}`;

    card.querySelector('.card-btn--edit').addEventListener('click', e => {
      e.stopPropagation(); spawnRipple(e, card);
      if (dev.random) Modals.info({ title: I18N.t('dev_random'), message: I18N.t('dev_random_info') });
      else Modals.openDevice(dev.key);
    });
    const shareEl = card.querySelector('.card-btn--share');
    if (shareEl) shareEl.addEventListener('click', e => {
      e.stopPropagation(); spawnRipple(e, card); Modals.openDeviceShare('export', dev.key);
    });
    if (!builtin) {
      card.querySelector('.card-btn--delete').addEventListener('click', e => {
        e.stopPropagation();
        Modals.confirmDelete(
          I18N.t('confirm_del_device_title'),
          `${dev.name} — ${dev.gameCount} ${I18N.t('dev_apps')}`,
          async () => { COPG.deleteDevice(dev.key); await persistAndRefresh(I18N.t('toast_device_deleted')); }
        );
      });
    }
    wireSelectable(DevSel, card, dev);   // long-press to multi-select (This Device excluded)
    list.appendChild(card);
  });
  if (DevSel.mode) DevSel.applyDOM();     // re-apply checkboxes/selection after a re-render
}

/* ─── Render Packages ─── */
function renderPackages() {
  const list = $('#packageList');
  if (!list) return;
  const pkgs = COPG.listPackages();
  const cnt = $('#pkgTabCount');
  if (cnt) cnt.textContent = pkgs.length;

  const set = installedSet();
  const q = LibQuery.packages.trim().toLowerCase();
  let rows = pkgs.filter(pkgMatchesFilter);
  if (q) rows = rows.filter(p => (p.name + ' ' + p.clean).toLowerCase().includes(q));
  rows = sortPackages(rows);

  lastPkgRows = rows;
  list.innerHTML = '';
  if (!rows.length) { list.appendChild(libEmpty('empty_packages')); return; }

  rows.forEach((pkg, i) => {
    const card = document.createElement('div');
    card.className = 'package-card';
    card.style.animationDelay = `${Math.min(i * 30, 400)}ms`;
    const installed = set.has(pkg.clean);
    // Launch button (installed only): force-stops first so the spoof re-applies on a
    // fresh Zygisk specialize, then opens the app.
    const launchBtn = installed
      ? `<button class="card-btn card-btn--launch" aria-label="${I18N.t('act_launch')}">${SVG_LAUNCH}</button>`
      : '';
    // Clear-data (installed only): wipes the app's stored identity anchors (SSAID, cached IDs,
    // install-attribution) so the next launch reads the spoofed device fresh. Destructive → confirm-gated.
    const clearBtn = installed
      ? `<button class="card-btn card-btn--clear" aria-label="${I18N.t('act_cleardata')}">${SVG_CLEAR}</button>`
      : '';
    card.innerHTML = `
      <div class="package-card__icon" data-icon></div>
      <div class="package-card__info">
        <div class="package-card__device"></div>
        <div class="package-card__name"></div>
        <div class="package-card__pkg"></div>
        <div class="package-card__chips"></div>
      </div>
      <div class="card-actions">
        ${launchBtn}
        ${clearBtn}
        <button class="card-btn card-btn--edit" aria-label="${I18N.t('act_edit')}">${SVG_EDIT}</button>
        <button class="card-btn card-btn--delete" aria-label="${I18N.t('act_delete')}">${SVG_TRASH}</button>
      </div>`;
    card.querySelector('.package-card__name').textContent = pkg.name;
    card.querySelector('.package-card__pkg').textContent = pkg.clean;

    // Dim text ABOVE the name (same slot/style as the device model): the profile
    // this package is attached to ("This Device" for the identity profile).
    const devEl = card.querySelector('.package-card__device');
    const aboveText = pkg.deviceName || '';
    if (aboveText) devEl.textContent = aboveText;
    else devEl.remove();

    // chips: tags + installed (type now shown above the name, not as a chip)
    const chips = card.querySelector('.package-card__chips');
    // Per-user scope badge (u=<id>): only when scoped to a specific user (blank = all users,
    // no badge). Owner = "Owner"; a clone/work-profile user shows its own name (the system
    // app-clone name, e.g. "Clone 1", via COPG.userNameOf) so clones are distinguishable, falling
    // back to "User <id>". Leads the row so a same-package-different-user pair stands out.
    if (pkg.userScope !== '' && pkg.userScope != null)
      chips.appendChild(chip('chip--user', pkg.userScope === '0'
        ? I18N.t('tag_user_owner')
        : I18N.t('tag_user').replace('%s', pkg.userScope)));   // clone → user id ("User 900"), the only reliable distinguisher
    if (pkg.with_cpu)    chips.appendChild(chip('chip--withcpu', pkg.cpuModel ? COPG.cpuModelName(pkg.cpuModel) : I18N.t('tag_withcpu')));
    if (pkg.blocked)     chips.appendChild(chip('chip--blocked', I18N.t('tag_block')));
    if (pkg.cow)         chips.appendChild(chip('chip--cow', I18N.t('tag_cow')));
    if (pkg.gpu)         chips.appendChild(chip('chip--gpu', pkg.gpuModel ? COPG.gpuModelName(pkg.gpuModel) : I18N.t('tag_gpu')));
    if (pkg.aid)         chips.appendChild(chip('chip--aid', I18N.t('tag_aid')));
    if (pkg.mock)        chips.appendChild(chip('chip--mock', I18N.t('tag_mock')));
    if (pkg.sim) {
      const n0 = pkg.simS0 ? COPG.carrierModelName(pkg.simS0) : '';
      const n1 = pkg.simS1 ? COPG.carrierModelName(pkg.simS1) : '';
      let label;
      if (pkg.sim === 'phantom')               label = (n0 || I18N.t('tag_sim')) + ' +SIM';  // Add-SIM
      else if (n0 && n1 && pkg.simS0 === pkg.simS1) label = n0;            // both same
      else if (n0 && n1)                       label = n0 + ' / ' + n1;    // different
      else if (n0)                             label = n0 + ' · SIM1';
      else if (n1)                             label = n1 + ' · SIM2';
      else                                     label = I18N.t('tag_sim');
      const resident = pkg.sim === 'aggressive' || pkg.sim === 'phantom';  // both keep a hook mapped
      chips.appendChild(chip(resident ? 'chip--simx' : 'chip--sim',
                             label + (resident ? ' ⚡' : '')));
    }
    if (pkg.proxy)      chips.appendChild(chip('chip--proxy', COPG.proxyModelName(pkg.proxy) || I18N.t('tag_proxy')));
    // tweak tags — muted shared style so they read as a group, not as loud as spoof chips
    if (pkg.dnd)         chips.appendChild(chip('chip--tweak', I18N.t('tag_dnd')));
    if (pkg.dab)         chips.appendChild(chip('chip--tweak', I18N.t('tag_dab')));
    if (pkg.kso)         chips.appendChild(chip('chip--tweak', I18N.t('tag_kso')));
    if (pkg.nolog)       chips.appendChild(chip('chip--tweak', I18N.t('tag_nolog')));
    if (installed) chips.appendChild(chip('chip--installed', I18N.t('badge_installed')));

    // lazy icon
    if (window.Icons) Icons.load(card.querySelector('[data-icon]'), pkg.clean, pkg.name);

    if (installed) {
      card.querySelector('.card-btn--launch').addEventListener('click', async e => {
        e.stopPropagation(); spawnRipple(e, card);
        if (window.haptic) haptic(15);
        showToast(I18N.t('toast_relaunching').replace('%s', pkg.name));
        await COPG.relaunchPackage(pkg.clean, pkg.userScope);
      });
      // Clear data — destructive, so a danger confirm that names the app + warns. On OK it wipes
      // the app's data then relaunches it fresh (so the active spoof re-applies to a clean install).
      card.querySelector('.card-btn--clear').addEventListener('click', e => {
        e.stopPropagation();
        Modals.confirm({
          title: I18N.t('cleardata_title'),
          message: I18N.t('cleardata_msg').replace('%s', pkg.name),
          okLabel: I18N.t('cleardata_ok'),
          danger: true,
          onOk: async () => {
            showToast(I18N.t('toast_cleared').replace('%s', pkg.name));
            await COPG.clearPackageData(pkg.clean, pkg.userScope);
          },
        });
      });
    }
    card.querySelector('.card-btn--edit').addEventListener('click', e => {
      e.stopPropagation(); spawnRipple(e, card); Modals.openPackage(pkg);
    });
    card.querySelector('.card-btn--delete').addEventListener('click', e => {
      e.stopPropagation();
      Modals.confirmDelete(
        I18N.t('confirm_del_package_title'),
        `${pkg.name}\n${pkg.clean}`,
        async () => { COPG.deletePackage(pkg.clean, pkg.type, pkg.deviceKey, pkg.userScope); await persistAndRefresh(I18N.t('toast_package_deleted')); }
      );
    });
    wireSelectable(PkgSel, card, pkg);   // long-press to multi-select
    list.appendChild(card);
  });
  if (PkgSel.mode) PkgSel.applyDOM();     // re-apply checkboxes/selection after a re-render
}

/* ─── Persist + refresh both lists + banner ─── */
async function persistAndRefresh(successMsg) {
  try {
    if (window.logEvent) logEvent('Saving COPG.json / list.json…', 'info');
    const res = await COPG.save();
    renderDevices();
    renderPackages();
    if (typeof updateStats === 'function') updateStats();
    updatePreviewBanner();
    if (res && res.preview) {
      showToast(I18N.t('toast_preview_only'));
      if (window.logEvent) logEvent('Preview mode — not written to device.', 'warn');
    } else {
      if (successMsg) showToast(successMsg);
      if (window.logEvent) logEvent('Saved to /data/adb/modules/COPG (' + COPG.listDevices().length + ' devices, ' + COPG.listPackages().length + ' packages).', 'success');
    }
  } catch (err) {
    showToast(I18N.t('toast_save_failed'));
    console.error('save failed:', err);
  }
}

/* ─── Preview banner (no bridge) ─── */
function updatePreviewBanner() {
  const banner = $('#previewBanner');
  if (!banner) return;
  const show = !COPG.hasBridge() && COPG.previewUnsaved;
  banner.classList.toggle('visible', show);
}

/* ─── Sort sheet (built dynamically, reused for devices & packages) ─── */
document.body.insertAdjacentHTML('beforeend', `
  <div class="bottom-sheet" id="sortSheet" role="dialog" aria-modal="true" aria-label="Sort">
    <div class="sheet-handle"></div>
    <div class="sheet-header">
      <span class="sheet-header__title" data-i18n="sort_title">Sort by</span>
      <button class="sheet-header__close" id="sortClose" aria-label="Close">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </div>
    <div class="sheet-body" id="sortSheetBody"></div>
  </div>`);
$('#sortClose')?.addEventListener('click', () => closeAllSheets());

/* ─── Brand filter sheet (devices tab; built dynamically in openBrandSheet) ─── */
document.body.insertAdjacentHTML('beforeend', `
  <div class="bottom-sheet" id="brandSheet" role="dialog" aria-modal="true" aria-label="Brand">
    <div class="sheet-handle"></div>
    <div class="sheet-header">
      <span class="sheet-header__title" data-i18n="brand_title">Filter by brand</span>
      <button class="sheet-header__close" id="brandClose" aria-label="Close">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </div>
    <div class="sheet-body" id="brandSheetBody"></div>
  </div>`);
$('#brandClose')?.addEventListener('click', () => closeAllSheets());

function openSortSheet(ctx) {
  if (!SORT_OPTS[ctx]) return;          // ignore stray callers (e.g. other sheets' sort buttons)
  const body = $('#sortSheetBody');
  const cur = ctx === 'devices' ? LibQuery.devSort : LibQuery.pkgSort;
  body.innerHTML = '';
  SORT_OPTS[ctx].forEach(([val, key]) => {
    const row = document.createElement('div');
    row.className = 'sheet-option' + (val === cur ? ' selected' : '');
    row.innerHTML =
      `<div class="sheet-option__icon">${SVG_SORT}</div>` +
      `<span class="sheet-option__label">${I18N.t(key)}</span>` +
      `<svg class="sheet-option__check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>`;
    row.addEventListener('click', () => {
      if (ctx === 'devices') LibQuery.devSort = val; else LibQuery.pkgSort = val;
      updateSortLabels();
      ctx === 'devices' ? renderDevices() : renderPackages();
      closeAllSheets();
    });
    body.appendChild(row);
  });
  openSheet('sortSheet');
}

function updateSortLabels() {
  const dl = document.querySelector('#libDevices .lib-sort__label');
  const pl = document.querySelector('#libPackages .lib-sort__label');
  const find = (ctx, val) => (SORT_OPTS[ctx].find(o => o[0] === val) || [])[1];
  if (dl) dl.textContent = I18N.t(find('devices', LibQuery.devSort));
  if (pl) pl.textContent = I18N.t(find('packages', LibQuery.pkgSort));
  updateBrandLabel();
}

// Pretty brand: capitalize a purely-lowercase brand (google→Google); leave stylized/mixed-case
// brands as the data has them (OPPO, OnePlus, HONOR).
function prettyBrand(b) {
  b = String(b || '').trim();
  return /^[a-z0-9 ]+$/.test(b) ? b.charAt(0).toUpperCase() + b.slice(1) : b;
}

function updateBrandLabel() {
  const span = document.querySelector('#libDevBrand .lib-brand__label');
  if (!span) return;
  if (LibQuery.devBrand === 'all') { span.dataset.i18n = 'brand_all'; span.textContent = I18N.t('brand_all'); }
  else { delete span.dataset.i18n; span.textContent = prettyBrand(LibQuery.devBrand); }
  const btn = document.getElementById('libDevBrand');
  if (btn) btn.classList.toggle('is-active', LibQuery.devBrand !== 'all');
}

// Brand filter sheet for the Devices tab: "All brands" + each brand present, with a device count.
function openBrandSheet() {
  const body = $('#brandSheetBody');
  if (!body) return;
  const counts = new Map();                         // brandKey -> {label, n}
  COPG.listDevices().forEach(d => {
    if (d.identity || d.random || !d.brand) return;
    const key = d.brand.toLowerCase();
    const e = counts.get(key) || { label: prettyBrand(d.brand), n: 0 };
    e.n++; counts.set(key, e);
  });
  const total = [...counts.values()].reduce((s, e) => s + e.n, 0);
  const brands = [...counts.entries()].sort((a, b) =>
    a[1].label.toLowerCase().localeCompare(b[1].label.toLowerCase()));
  body.innerHTML = '';
  const mk = (key, label, n) => {
    const row = document.createElement('div');
    row.className = 'sheet-option' + (key === LibQuery.devBrand ? ' selected' : '');
    row.innerHTML =
      `<div class="sheet-option__icon"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg></div>` +
      `<span class="sheet-option__label"></span>` +
      `<span class="sheet-option__count">${n}</span>` +
      `<svg class="sheet-option__check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>`;
    row.querySelector('.sheet-option__label').textContent = label;
    row.addEventListener('click', () => {
      LibQuery.devBrand = key;
      updateBrandLabel();
      renderDevices();
      closeAllSheets();
    });
    body.appendChild(row);
  };
  mk('all', I18N.t('brand_all'), total);
  brands.forEach(([key, e]) => mk(key, e.label, e.n));
  openSheet('brandSheet');
}

/* ─── Controls (sort pill + package filter chips), injected per panel ─── */
function buildControls() {
  const sortPill = ctx => `
    <button class="lib-sort" data-sort-ctx="${ctx}" type="button">
      ${SVG_SORT}<span class="lib-sort__label"></span>
    </button>`;

  // Wrap the search row + sort/filter controls in one sticky header so BOTH stay
  // pinned while the list scrolls (the wrapper carries the solid background).
  const wrapHead = (panelSel, ctx, filtersHTML = '') => {
    const search = document.querySelector(`${panelSel} .lib-searchrow`);
    if (!search || document.querySelector(`${panelSel} .lib-controls`)) return;
    search.insertAdjacentHTML('afterend', `<div class="lib-controls">${sortPill(ctx)}${filtersHTML}</div>`);
    const controls = search.nextElementSibling;
    const head = document.createElement('div');
    head.className = 'lib-stickyhead';
    search.parentNode.insertBefore(head, search);
    head.appendChild(search);
    head.appendChild(controls);
  };
  // Devices tab: a brand-filter pill sitting right beside the sort pill ("next to the default").
  const devBrandPill = `
    <button class="lib-sort lib-brand" id="libDevBrand" type="button">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
      <span class="lib-brand__label" data-i18n="brand_all">All brands</span>
    </button>`;
  wrapHead('#libDevices', 'devices', devBrandPill);
  wrapHead('#libPackages', 'packages', `
        <div class="lib-filters">
          <button class="fchip active" data-filter="all" data-i18n="filter_all">All</button>
          <button class="fchip" data-filter="installed" data-i18n="filter_installed">Installed</button>
          <button class="fchip" data-filter="blocked" data-i18n="filter_blocked">Blocked</button>
          <button class="fchip" data-filter="cpu_only" data-i18n="filter_cpu_only">CPU</button>
        </div>`);

  // scope to the Library panels only — the App Picker has its own .lib-sort (#apSort)
  $$('#pageLibrary .lib-sort[data-sort-ctx]').forEach(btn => btn.addEventListener('click', e => {
    spawnRipple(e, btn); openSortSheet(btn.dataset.sortCtx);
  }));
  $('#libDevBrand')?.addEventListener('click', e => { spawnRipple(e, e.currentTarget); openBrandSheet(); });
  $$('#libPackages .fchip').forEach(btn => btn.addEventListener('click', e => {
    spawnRipple(e, btn);
    LibQuery.pkgFilter = btn.dataset.filter;
    $$('#libPackages .fchip').forEach(b => b.classList.toggle('active', b === btn));
    renderPackages();
  }));
  updateSortLabels();
}

/* ─── Swipe between Devices / Packages ─── */
function wireSwipe() {
  const area = document.querySelector('#pageLibrary .scroll-area');
  if (!area) return;
  let x0 = 0, y0 = 0, t0 = 0, tracking = false;
  area.addEventListener('touchstart', e => {
    if (e.touches.length !== 1) { tracking = false; return; }
    // don't hijack horizontal scrolls of the filter/sort row or the search row
    if (e.target.closest('.lib-controls') || e.target.closest('.lib-searchrow')) { tracking = false; return; }
    x0 = e.touches[0].clientX; y0 = e.touches[0].clientY; t0 = Date.now(); tracking = true;
  }, { passive: true });
  area.addEventListener('touchend', e => {
    if (!tracking) return;
    tracking = false;
    const t = e.changedTouches[0];
    const dx = t.clientX - x0, dy = t.clientY - y0, dt = Date.now() - t0;
    if (dt > 600) return;
    // require a deliberate, dominantly-horizontal swipe (bigger threshold so
    // it won't fire by accident while scrolling the filter chips)
    if (Math.abs(dx) < 80 || Math.abs(dx) < Math.abs(dy) * 2) return;
    if (dx < 0 && State.activeLibTab === 'devices') switchLibTab('packages');
    else if (dx > 0 && State.activeLibTab === 'packages') switchLibTab('devices');
  }, { passive: true });
}

/* ─── Add buttons ─── */
$('#addDeviceBtn')?.addEventListener('click', e => { spawnRipple(e, e.currentTarget); Modals.openDevice(null); });
$('#importDeviceBtn')?.addEventListener('click', e => { spawnRipple(e, e.currentTarget); Modals.openDeviceShare('import'); });
$('#addPackageBtn')?.addEventListener('click', e => { spawnRipple(e, e.currentTarget); Modals.openPackage(null); });

/* ─── Search inputs ─── */
$('#deviceSearch')?.addEventListener('input', e => { LibQuery.devices = e.target.value; renderDevices(); });
$('#packageSearch')?.addEventListener('input', e => { LibQuery.packages = e.target.value; renderPackages(); });

/* expose refresh so modals can call after saving */
window.LibRefresh = persistAndRefresh;

/* initial setup + paint */
buildControls();
wireSwipe();
renderDevices();
renderPackages();
updatePreviewBanner();
