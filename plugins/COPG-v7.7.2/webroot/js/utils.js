/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   utils.js — DOM helpers, ripple, toast
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
const $ = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

/* ── Ripple ── */
function spawnRipple(e, targetEl) {
  const container = $('#rippleContainer');
  if (!container) return;
  const rect = targetEl.getBoundingClientRect();
  const x = (e.clientX || rect.left + rect.width / 2) - rect.left;
  const y = (e.clientY || rect.top + rect.height / 2) - rect.top;
  const size = Math.max(rect.width, rect.height) * 2.2;
  const r = document.createElement('div');
  r.className = 'ripple';
  const cr = container.getBoundingClientRect();
  r.style.cssText = `width:${size}px;height:${size}px;left:${rect.left-cr.left+x-size/2}px;top:${rect.top-cr.top+y-size/2}px;`;
  container.appendChild(r);
  r.addEventListener('animationend', () => r.remove());
}

/* ── Haptics ── */
/* Light vibration for tactile feedback. No-op where unsupported (most
   desktop browsers); KSU WebUI / Android Chrome honour navigator.vibrate. */
function haptic(ms) {
  try { if (navigator.vibrate) navigator.vibrate(ms || 10); } catch (_) {}
}

/* General ripple delegation */
document.addEventListener('click', e => {
  const t = e.target.closest('.glass-card,.device-card,.package-card,.setting-row,.sheet-option,.inner-tab');
  if (t) spawnRipple(e, t);
}, { passive: true });

/* Global light haptic on any primary tappable surface */
document.addEventListener('pointerdown', e => {
  if (e.target.closest('button, .nav-item, .device-card, .package-card, .setting-row, .sheet-option, .inner-tab, .fchip, .lib-sort, .add-pill, .seg__opt, .toggle, .app-row, .card-btn')) {
    haptic(8);
  }
}, { passive: true });

/* ── Dev log helper ──
   Safe everywhere: forwards to the in-app Console (js/console.js) when present,
   else falls back to native console. Use logEvent('msg','success'|'warn'|...).
   The Console gates info-level lines behind Settings → "Debug Logs". */
function logEvent(msg, type) {
  try {
    if (window.Log && Log[type === 'warn' ? 'warn' : (type || 'info')]) {
      Log[type === 'warn' ? 'warn' : (type || 'info')](msg);
      return;
    }
  } catch (_) {}
  try { (type === 'error' ? console.error : type === 'warn' ? console.warn : console.log)(msg); } catch (_) {}
}

/* ── Toast ── */
let _toastTimer = null;
function showToast(msg) {
  let t = document.querySelector('.toast');
  if (!t) { t = document.createElement('div'); t.className = 'toast'; document.body.appendChild(t); }
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => t.classList.remove('show'), 2200);
}
