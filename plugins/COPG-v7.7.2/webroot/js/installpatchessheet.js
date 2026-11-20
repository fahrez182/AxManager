/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   installpatchessheet.js — "Install Patches" bottom-sheet (Settings → Global Hooks)
   Groups the four package-manager install-time patches (they all hook the installer inside
   system_server, so they belong together, not as loose top-level toggles):
     sig       — disable APK signature verification (install modified / re-signed APKs)
     downgrade — allow installing a lower version over a higher one
     verifier  — disable the install verification agent (Play Protect scan)
     arsc      — allow a compressed resources.arsc (lift the Android-11+ install limit)
   FREE. Device-wide. Runs under the "System-Server Spoofs" master switch → when that's off the
   rows are inert with a one-line note (enable master + reboot first). sig & verifier lower a real
   install-security boundary → uncheck-then-confirm warn on enable; downgrade & arsc apply directly.
   Instant-apply per toggle (no Save): writes global.json.corepatch via COPG.saveGlobalConfig, and
   the resident CorePatchHook picks up the live file within ~1.5s (no reboot once armed).
   Idea/technique ports of Lucky Patcher's PMPatch (vova7878) + CorePatch. Self-managed open/close,
   mirrors globalimeisheet.js. window.InstallPatchesSheet.open(onDone).
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  // key → { label i18n, one-line sub i18n, warn-on-enable? }
  const PATCHES = [
    { key: 'sig',       label: 'glob_cp_sig',       sub: 'glob_cp_sig_sub',       warn: true  },
    { key: 'prevsig',   label: 'glob_cp_prevsig',   sub: 'glob_cp_prevsig_sub',   warn: false },
    { key: 'downgrade', label: 'glob_cp_downgrade', sub: 'glob_cp_downgrade_sub', warn: false },
    { key: 'verifier',  label: 'glob_cp_verifier',  sub: 'glob_cp_verifier_sub',  warn: false },
    { key: 'arsc',      label: 'glob_cp_arsc',      sub: 'glob_cp_arsc_sub',      warn: false },
    { key: 'hiddenapi', label: 'glob_cp_hiddenapi', sub: 'glob_cp_hiddenapi_sub', warn: false },
    { key: 'block',     label: 'glob_cp_block',     sub: 'glob_cp_block_sub',     warn: false },
  ];

  const rows = PATCHES.map(p => `
        <div class="setting-row setting-row--toggle ip-row" data-key="${p.key}">
          <span class="ip-row__text">
            <span class="setting-row__label" data-i18n="${p.label}"></span>
            <span class="ip-row__sub" data-i18n="${p.sub}"></span>
          </span>
          <label class="toggle" aria-label="${p.key}">
            <input type="checkbox" id="ip_${p.key}"/>
            <span class="toggle__track"><span class="toggle__thumb"></span></span>
          </label>
        </div>`).join('');

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--confirm" id="installPatchSheet" role="dialog" aria-modal="true" aria-label="Install Patches">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="glob_installpatch">Install Patches</span>
        <button class="sheet-header__close" id="ipClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="sheet-body">
        <p class="confirm-msg" data-i18n="glob_installpatch_desc"></p>
        <div class="ip-note" id="ipMasterNote" hidden>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 9v4M12 17h.01"/><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/></svg>
          <span data-i18n="glob_installpatch_master"></span>
        </div>
        <div class="ip-rows" id="ipRows">${rows}</div>
      </div>
    </div>`);

  const $c = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('installPatchSheet');

  let onDone = null, syncing = false;

  function cpCfg() {
    try { const c = w.COPG && COPG.getGlobalConfig && COPG.getGlobalConfig(); return (c && c.corepatch) || {}; }
    catch (_) { return {}; }
  }
  function masterOn() { try { return !!(w.COPG && COPG.getGlobalConfig && COPG.getGlobalConfig().sysserver); } catch (_) { return false; } }
  // ARMED = master on AND a real reboot has happened since it was toggled (these patches hook the
  // installer in system_server, which only arms at boot). Rows stay inert until an actual reboot, so
  // enabling the master doesn't make them look live before the restart.
  function armed() { try { return !!(w.COPG && COPG.sysserverArmed && COPG.sysserverArmed()); } catch (_) { return masterOn(); } }

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
    if (typeof onDone === 'function') onDone();
  }

  function paint() {
    const co = cpCfg(), on = masterOn(), live = armed();
    syncing = true;
    PATCHES.forEach(p => { const cb = $c('#ip_' + p.key); if (cb) { cb.checked = !!co[p.key]; cb.disabled = !live; } });
    syncing = false;
    // One note, two messages: master OFF → "enable it + reboot"; master ON but not yet rebooted →
    // "reboot once to apply". Shown whenever not armed; hidden only once a real reboot armed the layer.
    const note = $c('#ipMasterNote'); const nspan = note && note.querySelector('span');
    if (nspan) nspan.textContent = I18N.t(on ? 'glob_installpatch_pending' : 'glob_installpatch_master');
    if (note) note.hidden = live;
    $c('#ipRows').classList.toggle('is-locked', !live);
  }

  async function apply(key, on) {
    const merged = Object.assign({}, cpCfg(), { [key]: on });
    try { await COPG.saveGlobalConfig({ corepatch: merged }); showToast(I18N.t(on ? 'toast_cp_on' : 'toast_cp_off')); }
    catch (e) { showToast(String(e)); paint(); }
  }

  function wire(p) {
    const cb = $c('#ip_' + p.key);
    if (!cb) return;
    cb.addEventListener('change', () => {
      if (syncing) return;
      if (!(w.COPG && COPG.hasBridge && COPG.hasBridge())) { showToast(I18N.t('set_gaid_preview')); syncing = true; cb.checked = false; syncing = false; return; }
      if (cb.checked && !masterOn()) {   // master off → block + guide to the master switch
        syncing = true; cb.checked = false; syncing = false;
        Modals.confirm({ title: I18N.t('sysserver_needed_title'), message: I18N.t('sysserver_needed_msg'), okLabel: I18N.t('sysserver_warn_ok'), infoOnly: true });
        return;
      }
      if (cb.checked && !armed()) {      // master on but not rebooted yet → not live until restart
        syncing = true; cb.checked = false; syncing = false;
        Modals.confirm({ title: I18N.t('sysserver_pending_title'), message: I18N.t('sysserver_pending_msg'), okLabel: I18N.t('sysserver_warn_ok'), infoOnly: true });
        return;
      }
      if (cb.checked && p.warn) {         // sig / verifier → uncheck-then-confirm
        syncing = true; cb.checked = false; syncing = false;
        Modals.confirm({
          title:   I18N.t('cp_warn_title'),
          message: I18N.t('cp_warn_msg'),
          okLabel: I18N.t('cp_warn_ok'),
          onOk: () => { syncing = true; cb.checked = true; syncing = false; apply(p.key, true); },
        });
      } else {
        apply(p.key, cb.checked);
      }
    });
  }

  $c('#ipClose').addEventListener('click', close);
  PATCHES.forEach(wire);

  function open(done) {
    onDone = done || null;
    const onDevice = !!(w.COPG && COPG.hasBridge && COPG.hasBridge());
    paint();                                        // instant paint from the boot cache
    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); requestAnimationFrame(() => s.classList.add('open')); }
    if (!onDevice) return;
    // Background: authoritative live state (live file + global.json), then repaint if unchanged by the user.
    (async () => { try { await COPG.loadGlobalConfig(); paint(); } catch (_) {} })();
  }

  // How many patches are on → the Settings row hint. "Off" / "2 on" / "Reboot needed" (master off + intent set).
  function hintLabel() {
    const co = cpCfg(); const n = PATCHES.filter(p => co[p.key]).length;
    if (!n) return I18N.t('glob_off');
    if (!armed()) return I18N.t('glob_installpatch_reboot');   // master off OR on-but-pending-reboot
    return I18N.t('glob_installpatch_count').replace('{n}', n);
  }

  w.InstallPatchesSheet = { open, hintLabel };
})(window);
