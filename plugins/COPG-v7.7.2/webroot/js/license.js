/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   license.js — PRO licensing UI (licensing branch only)
   Settings → License: one collapsible card (status chip → Get License · tap-to-copy code ·
   Import by paste or file), plus the gold PRO badge in the hero corner.
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function () {
  const t = (k, f) => (window.I18N ? I18N.t(k) : (f || k));
  const $ = (id) => document.getElementById(id);
  const CONTACT = 'https://t.me/theaosp';
  const bridge = () => window.COPG && COPG.hasBridge();

  function chipFor(state) {
    switch (state) {
      case 'VALID':   return t('lic_chip_active', 'PRO · active');
      case 'GRACE':   return t('lic_chip_grace', 'PRO · verify online');
      case 'EXPIRED': return t('lic_chip_expired', 'Expired · renew');
      case 'PREVIEW': return t('lic_chip_preview', 'Preview');
      default:        return t('lic_chip_free', 'Free tier');   // FREE / MISMATCH / REVOKED
    }
  }

  // Full-sentence state line in the card body — makes Expired/Revoked unmistakable.
  function stateText(state) {
    switch (state) {
      case 'VALID':   return t('lic_status_active',  'PRO active — all features unlocked');
      case 'GRACE':   return t('lic_status_grace',   'PRO active — verify online soon');
      case 'EXPIRED': return t('lic_status_expired', 'License expired — renew to keep PRO');
      case 'REVOKED': return t('lic_status_revoked', 'License revoked');
      case 'PREVIEW': return t('lic_status_preview', 'Preview — install the module to use PRO');
      default:        return t('lic_status_free',    'Free tier — PRO features locked');
    }
  }

  // Like stateText, but for an active license show the remaining days (or "lifetime").
  // copg_lic prints expiry= only for VALID; 0 = perpetual.
  function stateLine(st) {
    if (st.state !== 'VALID') return stateText(st.state);
    const exp = parseInt(st.expiry, 10);
    if (!exp) return t('lic_status_active_life', 'PRO active — lifetime');
    const days = Math.max(0, Math.ceil((exp * 1000 - Date.now()) / 86400000));
    return t('lic_status_days', 'PRO active — %d days left').replace('%d', days);
  }

  async function refresh() {
    const chip = $('licStatusChip');
    if (!chip || !window.COPG) return;
    chip.textContent = '…';
    let st = { state: 'FREE' };
    try { st = await COPG.licenseStatus(); } catch (e) {}
    window._licState = st;
    chip.textContent = chipFor(st.state);
    chip.dataset.state = st.state;
    const line = $('licStateLine'), lineTxt = $('licStateTxt');
    if (line) line.dataset.state = st.state;
    if (lineTxt) lineTxt.textContent = stateLine(st);
    const pro = $('heroPro');
    if (pro) {
      pro.dataset.state = st.state;          // hero badge lights up when unlocked
      const unlocked = st.state === 'VALID' || st.state === 'GRACE';
      const txt = pro.querySelector('.hero__pro-txt');
      if (txt) txt.textContent = unlocked ? 'PRO' : 'FREE';
    }
  }

  function setOpen(open) {
    const card = $('licenseCard'), head = $('licHeadRow');
    if (!card) return;
    card.classList.toggle('is-open', open);
    if (head) head.setAttribute('aria-expanded', open ? 'true' : 'false');
  }
  function toggle() { setOpen(!$('licenseCard').classList.contains('is-open')); }

  // Open the license card + scroll it into view (hero PRO badge).
  function reveal() {
    if (typeof navigateTo === 'function') navigateTo('settings');
    setOpen(true);
    setTimeout(() => $('licenseCard') &&
      $('licenseCard').scrollIntoView({ behavior: 'smooth', block: 'center' }), 120);
  }

  async function copyCode() {
    const code = $('licCodeText') ? $('licCodeText').textContent : '';
    const box = $('licCodeField');
    if (!code) return;
    try { await navigator.clipboard.writeText(code); } catch (e) {}
    if (box) { box.classList.add('copied'); setTimeout(() => box.classList.remove('copied'), 1600); }
    if (typeof haptic === 'function') haptic(10);
    showToast(t('lic_copied', 'Copied to clipboard'));
  }

  const currentCode = () => ($('licCodeText') ? $('licCodeText').textContent.trim() : '');

  // Share the code straight to Telegram (Android share sheet) — no clipboard needed.
  async function shareCode() {
    const c = currentCode(); if (!c) return;
    if (typeof haptic === 'function') haptic(10);
    if (window.COPG && COPG.shareText) await COPG.shareText(c);
  }

  // Write the code to Download/COPG/copg-device-code.txt so it can be sent as a file.
  async function saveCode() {
    const c = currentCode(); if (!c) return;
    const p = window.COPG && COPG.saveDeviceCode ? await COPG.saveDeviceCode(c) : null;
    showToast(p ? t('lic_saved_file', 'Saved to %s').replace('%s', p)
                : t('lic_save_fail', 'Could not save the file'));
  }

  // Toggle an inline QR of the code (scannable off the screen; no clipboard, no file).
  function toggleQr() {
    const c = currentCode();
    const panel = $('licQrPanel'), holder = $('licQrHolder'), btn = $('licQrBtn');
    if (!c || !panel || !holder) return;
    if (!panel.hidden) { panel.hidden = true; holder.innerHTML = ''; btn && btn.setAttribute('aria-pressed', 'false'); return; }
    try {
      const qr = window.qrcode(0, 'M'); qr.addData(c); qr.make();
      holder.innerHTML = qr.createSvgTag({ cellSize: 5, margin: 2, scalable: true });
    } catch (e) { holder.textContent = c; }   // fallback: show the text
    panel.hidden = false;
    if (btn) btn.setAttribute('aria-pressed', 'true');
  }

  async function getLicense() {
    if (!bridge()) { showToast(t('lic_device_only', 'Device only')); return; }
    const btn = $('licGetRow');
    if (btn) btn.disabled = true;
    showToast(t('lic_reading', 'Reading device…'));
    let code = null;
    try { code = await COPG.getEnrollmentCode(); } catch (e) {}
    if (btn) btn.disabled = false;
    if (!code) {
      Modals.info({ title: t('lic_get', 'Get License'), message: t('lic_read_fail', 'Could not read device fingerprint.') });
      return;
    }
    if ($('licCodeText')) $('licCodeText').textContent = code;
    if ($('licCodeWrap')) $('licCodeWrap').hidden = false;
    if ($('licShareHint')) $('licShareHint').hidden = false;   // surface Share/Save/QR fallbacks
    const qp = $('licQrPanel'); if (qp) qp.hidden = true;
    const qh = $('licQrHolder'); if (qh) qh.innerHTML = '';
    const qb = $('licQrBtn'); if (qb) qb.setAttribute('aria-pressed', 'false');
    try { await navigator.clipboard.writeText(code); } catch (e) {}   // keep auto-copy (best effort)
    showToast(t('lic_copied', 'Copied to clipboard'));
  }

  // Shared apply: `b64` is the base64 of a license.bin.
  async function applyImport(b64) {
    showToast(t('lic_importing', 'Importing…'));
    try {
      const res = await COPG.importLicense(b64);
      await refresh();
      if (res.ok) {
        setOpen(true);
        const f = $('licImportField'); if (f) f.value = '';
        showToast(t('lic_import_ok_short', 'PRO unlocked ✓'));
      }
      Modals.info({
        // Never surface WHY it failed (mismatch vs expired vs corrupt) — that is an oracle
        // that tells a cracker whether the signature verified. One generic message for all.
        title: t('lic_import', 'Import License'),
        message: res.ok
          ? t('lic_import_ok', 'License valid — PRO unlocked. Relaunch spoofed apps to apply.')
          : t('lic_import_bad', 'This license isn’t valid.'),
      });
    } catch (e) {
      Modals.info({ title: t('lic_import', 'Import License'), message: String(e) });
    }
  }

  // Import from the pasted text (the base64 the seller sends).
  function importFromPaste() {
    if (!bridge()) { showToast(t('lic_device_only', 'Device only')); return; }
    const f = $('licImportField');
    const txt = (f && f.value || '').replace(/\s+/g, '');
    if (!txt) { showToast(t('lic_paste_empty', 'Paste your license first')); return; }
    applyImport(txt);
  }

  // Import from a file — accepts either the raw .bin or the .b64.txt.
  function importFromFile() {
    if (!bridge()) { showToast(t('lic_device_only', 'Device only')); return; }
    const inp = document.createElement('input');
    inp.type = 'file';                       // no accept — Android SAF mistypes .bin
    inp.onchange = async () => {
      const file = inp.files && inp.files[0];
      if (!file) return;
      try {
        const buf = new Uint8Array(await file.arrayBuffer());
        let b64;
        if (buf[0] === 0x43 && buf[1] === 0x50 && buf[2] === 0x47 && buf[3] === 0x4C) { // "CPGL" magic → binary .bin
          let bin = '';
          for (let i = 0; i < buf.length; i++) bin += String.fromCharCode(buf[i]);
          b64 = btoa(bin);
        } else {                              // text file → already base64
          b64 = new TextDecoder().decode(buf).replace(/\s+/g, '');
        }
        applyImport(b64);
      } catch (e) {
        Modals.info({ title: t('lic_import', 'Import License'), message: String(e) });
      }
    };
    inp.click();
  }

  function openContact() {
    if (window.COPG && COPG.openExternal) COPG.openExternal(CONTACT);
    else window.open(CONTACT, '_blank');
  }

  function init() {
    $('licHeadRow')?.addEventListener('click', toggle);
    $('licHeadRow')?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggle(); }
    });
    $('licGetRow')?.addEventListener('click', getLicense);
    $('licCodeField')?.addEventListener('click', copyCode);     // tap the whole box to copy
    $('licShareBtn')?.addEventListener('click', shareCode);     // clipboard-free: share to Telegram
    $('licSaveBtn')?.addEventListener('click', saveCode);       // clipboard-free: save as .txt
    $('licQrBtn')?.addEventListener('click', toggleQr);         // clipboard-free: show a QR to scan
    $('licImportRow')?.addEventListener('click', importFromPaste);
    $('licImportFile')?.addEventListener('click', importFromFile);
    $('licContactLink')?.addEventListener('click', openContact);
    $('heroPro')?.addEventListener('click', reveal);
    refresh();
  }

  window.License = { refresh, init, reveal };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
