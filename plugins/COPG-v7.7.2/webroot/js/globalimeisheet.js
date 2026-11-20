/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   globalimeisheet.js — "Global IMEI" bottom-sheet (Settings → Global Hooks)
   Device-wide IMEI: an explicit enable toggle + slot-0/slot-1 values (value is kept
   in global.json even when disabled). PRO → locked on the free tier. NO resident-hook
   risk gate: the hook runs in com.android.phone, a separate process an app can't scan,
   so it's app-invisible (unlike GPU/mock). Device-wide caveat is in glob_imei_desc. Drives
   COPG.{loadGlobalConfig,getGlobalConfig,saveGlobalConfig,genImei,restartTelephony,
   licenseStatus}. Self-managed open/close (mirrors gaidsheet.js). window.GlobalImeiSheet.open(onDone).
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const LOCK_SVG = '<svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--confirm" id="globImeiSheet" role="dialog" aria-modal="true" aria-label="Global IMEI">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="glob_imei">Global IMEI</span>
        <span class="trow__pro" id="giPro" hidden>${LOCK_SVG}<span data-i18n="lic_pro_tag">PRO</span></span>
        <button class="sheet-header__close" id="giClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="sheet-body">
        <p class="confirm-msg" data-i18n="glob_imei_desc"></p>
        <div class="setting-row setting-row--toggle gi-enable">
          <span class="setting-row__label" data-i18n="glob_imei_enable">Enable global IMEI</span>
          <label class="toggle" aria-label="Enable global IMEI">
            <input type="checkbox" id="giEnable"/>
            <span class="toggle__track"><span class="toggle__thumb"></span></span>
          </label>
        </div>
        <div class="gi-fields" id="giFields">
          <label class="field"><span class="field__head"><span class="field__label" data-i18n="glob_imei_s0">IMEI · SIM 1</span><span class="field__gen" id="giGen1" role="button" tabindex="0" data-i18n="serial_gen">Generate</span></span>
            <input class="field__input field__input--mono" id="giImei1" type="text" inputmode="numeric" maxlength="15" placeholder="356938035643809" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
          <label class="field"><span class="field__head"><span class="field__label" data-i18n="glob_imei_s1">IMEI · SIM 2 (optional)</span><span class="field__gen" id="giGen2" role="button" tabindex="0" data-i18n="serial_gen">Generate</span></span>
            <input class="field__input field__input--mono" id="giImei2" type="text" inputmode="numeric" maxlength="15" placeholder="352000000000004" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
        </div>
        <div class="picker-import__err" id="giErr" hidden></div>
        <div class="form-buttons">
          <button type="button" class="btn btn--ghost" id="giCancel" data-i18n="btn_cancel">Cancel</button>
          <button type="button" class="btn btn--primary" id="giApply" data-i18n="btn_save">Save</button>
        </div>
      </div>
    </div>`);

  const $c = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('globImeiSheet');

  let onDone = null, licensed = false, licPending = false, openSeq = 0;

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  function showErr(m) { const e = $c('#giErr'); e.textContent = m; e.hidden = false; }
  function hideErr() { $c('#giErr').hidden = true; }
  function syncFields() { $c('#giFields').classList.toggle('is-off', !$c('#giEnable').checked); }

  $c('#giClose').addEventListener('click', close);
  $c('#giCancel').addEventListener('click', close);
  $c('#giGen1').addEventListener('click', () => { $c('#giImei1').value = w.COPG ? COPG.genImei() : ''; hideErr(); });
  $c('#giGen2').addEventListener('click', () => { $c('#giImei2').value = w.COPG ? COPG.genImei() : ''; hideErr(); });
  [$c('#giImei1'), $c('#giImei2')].forEach(inp =>
    inp.addEventListener('input', () => { inp.value = inp.value.replace(/\D/g, '').slice(0, 15); hideErr(); }));

  // The hook lives in com.android.phone (a SEPARATE process), NOT in any game/app — so an app's
  // anti-cheat can't scan it (different UID, SELinux-isolated) and the IMEI arrives over the binder
  // as a clean value. No resident-hook risk gate here (unlike GPU/mock). Only gate = PRO license.
  // The device-wide caveat lives in the sheet's static description (glob_imei_desc).
  $c('#giEnable').addEventListener('change', () => {
    const on = $c('#giEnable').checked;
    // While the license check is still in flight, allow the toggle optimistically — the background
    // resolve reverts it if unlicensed. Once resolved, block immediately (free tier can't enable).
    if (on && !licensed && !licPending) { $c('#giEnable').checked = false; showToast(I18N.t('lic_pro_required')); }
    syncFields();
  });

  $c('#giApply').addEventListener('click', async () => {
    const enabled = $c('#giEnable').checked;
    const v0 = ($c('#giImei1').value || '').replace(/\D/g, '');
    const v1 = ($c('#giImei2').value || '').replace(/\D/g, '');
    if (enabled && (!v0 || v0.length !== 15)) { showErr(I18N.t('glob_imei_len')); return; }
    if (v1 && v1.length !== 15) { showErr(I18N.t('glob_imei_len')); return; }
    if (enabled && !licensed) { showToast(I18N.t('lic_pro_required')); return; }
    hideErr();
    const btn = $c('#giApply'); btn.disabled = true;
    try {
      await COPG.saveGlobalConfig({ enabled, imei: v0, imei2: v1 });
      // Apply live: bounce the persistent telephony process so the hook reinstalls (no reboot).
      let live = false;
      try { live = (await COPG.restartTelephony()).ok; } catch (_) {}
      showToast(enabled ? (live ? I18N.t('glob_applied') : I18N.t('glob_saved'))
                        : I18N.t('glob_disabled'));
      close();
      if (typeof onDone === 'function') onDone(globalStateLabel(enabled, v0));
    } catch (e) { showErr(String(e)); }
    finally { btn.disabled = false; }
  });

  function globalStateLabel(enabled, imei) {
    if (!enabled) return I18N.t('glob_off');
    return imei ? (imei.slice(0, 6) + '…') : I18N.t('glob_on');
  }

  function open(done) {
    onDone = done || null; hideErr();
    const seq = ++openSeq;                       // guards against a stale async patching a later open
    const onDevice = !!(w.COPG && COPG.hasBridge && COPG.hasBridge());

    // 1) INSTANT paint from the synchronous boot cache — no awaits before the sheet shows. The slow
    //    bits (disk re-read + license via app_process, ~1.4s) used to block the reveal → open lag.
    const cfg = (w.COPG && COPG.getGlobalConfig && COPG.getGlobalConfig()) || { enabled: false, imei: '', imei2: '' };
    $c('#giEnable').checked = !!cfg.enabled;
    $c('#giImei1').value = cfg.imei || '';
    $c('#giImei2').value = cfg.imei2 || '';
    licensed = !onDevice;                         // preview: unlocked demo. device: unknown until checked
    licPending = onDevice;                         // don't hard-block the toggle during the check
    $c('#giPro').hidden = licensed;
    syncFields();

    // 2) Show immediately.
    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); requestAnimationFrame(() => s.classList.add('open')); }

    if (!onDevice) return;

    // 3) Background refresh: disk config + license. Patch only if THIS open is still current and the
    //    user hasn't edited the field yet (compare to the value we painted → clobber-safe).
    (async () => {
      try {
        const disk = await COPG.loadGlobalConfig();
        if (seq === openSeq && disk) {
          if ($c('#giImei1').value === (cfg.imei  || '')) $c('#giImei1').value = disk.imei  || '';
          if ($c('#giImei2').value === (cfg.imei2 || '')) $c('#giImei2').value = disk.imei2 || '';
          if ($c('#giEnable').checked === !!cfg.enabled)  $c('#giEnable').checked = !!disk.enabled;
          syncFields();
        }
      } catch (_) {}
      try {
        const st = await COPG.licenseStatus();
        if (seq === openSeq) {
          licensed = !!(st && (st.state === 'VALID' || st.state === 'GRACE'));
          licPending = false;
          $c('#giPro').hidden = licensed;
          // Free tier flipped enable on during the pending window → revert it now.
          if (!licensed && $c('#giEnable').checked && !cfg.enabled) {
            $c('#giEnable').checked = false; showToast(I18N.t('lic_pro_required')); syncFields();
          }
        }
      } catch (_) { if (seq === openSeq) licPending = false; }
    })();
  }

  w.GlobalImeiSheet = { open };
})(window);
