/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   gaidsheet.js — "Advertising ID" bottom-sheet (Settings → Privacy)
   View the current global Google advertising id, set a custom one, generate a
   random one, or restore the device's real id (backed up once on first change).
   Drives COPG.{getAdvertisingId,setAdvertisingId,genAdvertisingId,
   getOriginalAdvertisingId,restoreAdvertisingId}. Self-managed open/close
   (mirrors carrierpicker.js). window.GaidSheet.open(onDone).
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const COPY_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>';

  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--confirm" id="gaidSheet" role="dialog" aria-modal="true" aria-label="Advertising ID">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="set_gaid">Advertising ID</span>
        <button class="sheet-header__close" id="gsClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="sheet-body">
        <p class="confirm-msg" data-i18n="gaid_desc"></p>
        <div class="gaid-current">
          <span class="gaid-current__lbl" data-i18n="set_gaid_current">Current</span>
          <code class="gaid-current__val" id="gsCur">—</code>
          <button type="button" class="gaid-copy" id="gsCopy" aria-label="Copy">${COPY_SVG}</button>
        </div>
        <label class="field">
          <span class="field__label" data-i18n="gaid_input_lbl">New advertising ID</span>
          <input type="text" id="gsInput" class="field__input field__input--mono" autocomplete="off" spellcheck="false" maxlength="36" data-i18n-attr="placeholder:gaid_input_ph" placeholder="xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx" />
        </label>
        <div class="picker-import__err" id="gsErr" hidden></div>
        <div class="gaid-quick">
          <button type="button" class="btn btn--ghost" id="gsGen" data-i18n="gaid_gen">Random</button>
          <button type="button" class="btn btn--ghost" id="gsRestore" data-i18n="gaid_restore" hidden>Restore original</button>
        </div>
        <div class="form-buttons">
          <button type="button" class="btn btn--ghost" id="gsCancel" data-i18n="btn_cancel">Cancel</button>
          <button type="button" class="btn btn--primary" id="gsApply" data-i18n="gaid_apply">Apply</button>
        </div>
      </div>
    </div>`);

  const $c = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');
  const sheet = () => document.getElementById('gaidSheet');
  const UUID_RE = /^[0-9a-fA-F-]{36}$/;

  let onDone = null;
  let original = null;   // the backed-up real id (or null)

  function close() {
    const s = sheet();
    if (s) { s.classList.remove('open'); s.style.transition = ''; s.style.transform = ''; }
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }

  function showErr(msg) { const e = $c('#gsErr'); e.textContent = msg; e.hidden = false; }
  function hideErr() { $c('#gsErr').hidden = true; }

  $c('#gsClose').addEventListener('click', close);
  $c('#gsCancel').addEventListener('click', close);

  $c('#gsGen').addEventListener('click', () => {
    $c('#gsInput').value = w.COPG ? COPG.genAdvertisingId() : '';
    hideErr();
  });

  $c('#gsRestore').addEventListener('click', () => {
    if (original) { $c('#gsInput').value = original; hideErr(); }
  });

  $c('#gsCopy').addEventListener('click', async () => {
    const v = $c('#gsCur').textContent.trim();
    if (!v || v === '—') return;
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) await navigator.clipboard.writeText(v);
      else {
        const ta = document.createElement('textarea');
        ta.value = v; ta.style.position = 'fixed'; ta.style.opacity = '0';
        document.body.appendChild(ta); ta.select();
        try { document.execCommand('copy'); } finally { ta.remove(); }
      }
      if (typeof showToast === 'function') showToast(I18N.t('gaid_copied'));
    } catch (_) {}
  });

  $c('#gsApply').addEventListener('click', async () => {
    const val = $c('#gsInput').value.trim();
    if (!UUID_RE.test(val)) { showErr(I18N.t('gaid_invalid')); return; }
    hideErr();
    const btn = $c('#gsApply'); btn.disabled = true;
    try {
      const nid = await COPG.setAdvertisingId(val);
      if (nid) {
        // "restored" toast when the applied id is the backed-up original, else generic set.
        if (original && nid === original && typeof showToast === 'function') showToast(I18N.t('gaid_restored'));
        else if (typeof showToast === 'function') showToast(I18N.t('set_gaid_done'));
        close();
        if (typeof onDone === 'function') onDone(nid);
      } else if (typeof showToast === 'function') showToast(I18N.t('set_gaid_fail'));
    } catch (_) {
      if (typeof showToast === 'function') showToast(I18N.t('set_gaid_fail'));
    } finally { btn.disabled = false; }
  });

  async function open(done) {
    onDone = done || null;
    original = null;
    hideErr();
    const curEl = $c('#gsCur'), inp = $c('#gsInput'), restore = $c('#gsRestore');
    curEl.textContent = '…';
    inp.value = '';
    restore.hidden = true;

    if (!w.COPG || !COPG.hasBridge()) {
      curEl.textContent = I18N.t('set_gaid_preview');
    } else {
      let cur = null;
      try { cur = await COPG.getAdvertisingId(); } catch (_) {}
      curEl.textContent = cur || I18N.t('set_gaid_none');
      inp.value = cur || '';
      try { original = await COPG.getOriginalAdvertisingId(); } catch (_) {}
      // Offer restore only when we have a real backup that differs from what's live now.
      restore.hidden = !(original && UUID_RE.test(original) && original !== cur);
    }

    overlay()?.classList.add('visible');
    const s = sheet();
    if (s) { s.style.transition = ''; s.style.transform = ''; s.classList.remove('open'); }
    requestAnimationFrame(() => s && s.classList.add('open'));
  }

  w.GaidSheet = { open };
})(window);
