/* ───────────────────────────────────────────────────────────────────────────
   helpers.js — Integrations / Module Helpers dashboard  (window.Helpers)

   A read-only board of the modules COPG actually collaborates with — NOT a module
   manager and NOT a full module list. It lists only COPG's own helpers/collabs, gives
   each a type badge + live status, and a detail sheet that credits the project (repo link).
   Data-driven: adding a future helper (another kernel/KSU/LSPosed collab) is one CATALOG
   entry, no new UI.

   CRACK-SAFE: shows module PRESENCE + the capability it powers only — never license / PRO /
   gate / key state. Module-presence can't be used as an unlock oracle. See
   [[no-license-internals-in-ui]].

   Presentation only — detection (NoMount probe, zygisk/root) is in copg-data.js / State.sysInfo.
   Opened from Settings → Integrations.  window.Helpers.open()
─────────────────────────────────────────────────────────────────────────── */
(function (w) {
  'use strict';

  const SVG_X =
    `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`;
  const SVG_PLUG =
    `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 2v6M15 2v6M7 8h10v3a5 5 0 0 1-10 0z"/><path d="M12 16v6"/></svg>`;

  /* status rank for sort: Active, then Update-needed, then Installed, then Not installed. */
  const RANK = { active: 0, outdated: 1, installed: 2, absent: 3 };

  /* Only COPG's real collabs. Each: id, name (string or fn), type, roleKey (i18n),
     detect() → {status:'active'|'installed'|'absent', version}. */
  const CATALOG = [
    {
      id: 'nomount', name: 'NoMount', type: 'metamodule',
      roleKey: 'hlp_nomount_role',
      detect: () => (COPG.detectNoMount ? COPG.detectNoMount() : { status: 'absent', version: '' }),
    },
    {
      id: 'zygisk', name: () => sysZygisk(), type: 'zygisk',
      roleKey: 'hlp_zygisk_role',
      detect: async () => {
        const d = COPG.detectZygisk ? await COPG.detectZygisk()
                : { status: isReal(State.sysInfo.zygisk) ? 'active' : 'absent', version: '' };
        if (!d.version) d.version = sysZygiskVer();   // fall back to the sysInfo label's version
        return d;
      },
    },
    {
      id: 'root', name: () => sysRootName(), type: 'root',
      roleKey: 'hlp_root_role',
      detect: async () => ({
        status: (isReal(State.sysInfo.rootVersionLabel) || isReal(State.rootEnv)) ? 'active' : 'absent',
        version: sysRootVer(),
      }),
    },
  ];

  const isReal = v => !!v && v !== '—';
  // sysInfo.zygisk is the zygiskLabel = "Zygisk Next (v1.5.0 (…))" — variant name with the
  // version in a trailing paren group. Split them so the row NAME stays clean and the version
  // shows in the detail only.
  const zygiskLabel = () => (isReal(State.sysInfo.zygisk) ? State.sysInfo.zygisk : '');
  const sysZygisk = () => { const l = zygiskLabel(); return l ? l.replace(/\s*\(.*\)\s*$/, '').trim() : 'Zygisk'; };
  const sysZygiskVer = () => { const m = zygiskLabel().match(/\((.+)\)\s*$/); return m ? m[1] : ''; };
  // rootVersionLabel = "KernelSU 3.2.4(32473)" — name + version; keep the name for the row,
  // the rest for the detail version line.
  const sysRootName = () => {
    const l = State.sysInfo.rootVersionLabel;
    return isReal(l) ? l.split(' ')[0] : (isReal(State.rootEnv) ? State.rootEnv : 'Root');
  };
  const sysRootVer = () => {
    const l = State.sysInfo.rootVersionLabel;
    return isReal(l) ? l.split(' ').slice(1).join(' ') : '';
  };
  const nameOf = h => (typeof h.name === 'function' ? h.name() : h.name);

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--helpers" id="helpersSheet" role="dialog" aria-modal="true" aria-label="Integrations">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="hlp_title">Integrations</span>
        <button class="sheet-header__close" id="helpersClose" aria-label="Close">${SVG_X}</button>
      </div>
      <div class="sheet-body">
        <p class="hlp-intro" data-i18n="hlp_intro">Modules that work with COPG. Status is read live from your device.</p>
        <div class="hlp-list" id="helpersList"></div>
      </div>
    </div>`);

  const $h = s => document.querySelector(s);

  function pill(status) {
    const key = status === 'active' ? 'hlp_active'
              : status === 'outdated' ? 'hlp_outdated'
              : status === 'installed' ? 'hlp_installed' : 'hlp_absent';
    const span = document.createElement('span');
    span.className = 'hlp-pill hlp-pill--' + status;
    span.setAttribute('data-i18n', key);
    span.textContent = I18N.t(key);
    return span;
  }

  function typeBadge(type) {
    const span = document.createElement('span');
    span.className = 'hlp-type hlp-type--' + type;
    const key = 'hlp_type_' + type;
    span.setAttribute('data-i18n', key);
    span.textContent = I18N.t(key);
    return span;
  }

  // Static info card — NOT a button. Everything (version, role, status) is shown inline, so
  // there's no detail sheet to open; only the name is a link, crediting the helper's repo.
  function renderRow(h, det) {
    const row = document.createElement('div');
    row.className = 'hlp-row hlp-row--static';

    const main = document.createElement('div');
    main.className = 'hlp-row__main';

    const top = document.createElement('div');
    top.className = 'hlp-row__top';
    const name = document.createElement('span');
    name.className = 'hlp-row__name';
    name.textContent = nameOf(h);                       // textContent — no injection
    top.appendChild(name);
    // short version only — just the leading dotted number, so a long build string can't crowd
    // the name ("1.5.0-843-5217106-release" → "1.5.0", "21 (595)" → "21", "3.2.5" → "3.2.5").
    const ver = (String(det.version || '').match(/\d+(?:\.\d+)*/) || [''])[0];
    if (ver) {
      const v = document.createElement('span');
      v.className = 'hlp-row__ver';
      v.textContent = ver;
      top.appendChild(v);
    }
    top.appendChild(typeBadge(h.type));
    main.appendChild(top);

    const role = document.createElement('div');
    role.className = 'hlp-row__role';
    role.setAttribute('data-i18n', h.roleKey);
    role.textContent = I18N.t(h.roleKey);
    main.appendChild(role);

    row.appendChild(main);
    row.appendChild(pill(det.status));

    // Static by default; a helper that needs ACTION (NoMount driver too old → COPG fell back
    // to bind-mount) becomes tappable with a plain-language explanation + what to do.
    if (det.status === 'outdated') {
      row.classList.remove('hlp-row--static');
      row.classList.add('hlp-row--action');
      row.setAttribute('role', 'button');
      row.setAttribute('tabindex', '0');
      const explain = () => Modals.info({ title: nameOf(h), message: I18N.t('hlp_nomount_outdated') });
      row.addEventListener('click', e => { spawnRipple(e, row); explain(); });
      row.addEventListener('keydown', e => {
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); explain(); }
      });
    }
    return row;
  }

  async function render() {
    const list = $h('#helpersList');
    if (!list) return;
    // Resolve all detections (NoMount is async via the bridge; the rest are sync-ish).
    const dets = await Promise.all(CATALOG.map(h => Promise.resolve(h.detect())
      .catch(() => ({ status: 'absent', version: '' }))));
    const rows = CATALOG.map((h, i) => ({ h, det: dets[i] }))
      .sort((a, b) => (RANK[a.det.status] - RANK[b.det.status]));
    list.textContent = '';
    rows.forEach(({ h, det }) => list.appendChild(renderRow(h, det)));
  }

  function open() {
    render();                                           // async repaint; sheet opens immediately
    Modals.closeAll && Modals.closeAll();
    const overlay = document.getElementById('sheetOverlay');
    overlay && overlay.classList.add('visible');
    const sheet = $h('#helpersSheet');
    if (sheet) { sheet.style.transition = ''; sheet.style.transform = ''; requestAnimationFrame(() => sheet.classList.add('open')); }
  }
  function close() {
    const sheet = $h('#helpersSheet');
    sheet && sheet.classList.remove('open');
    if (![...document.querySelectorAll('.bottom-sheet.open')].length)
      document.getElementById('sheetOverlay')?.classList.remove('visible');
  }

  $h('#helpersClose')?.addEventListener('click', close);

  w.Helpers = { open, close };
})(window);
