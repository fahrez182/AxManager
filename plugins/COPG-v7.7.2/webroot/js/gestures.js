/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   gestures.js — Drag-to-dismiss for every bottom-sheet
   Grab the handle/header and pull down: past the threshold the sheet
   closes, otherwise it snaps back. Works for the theme/language sheets
   and the dynamically-injected device/package/confirm/sort/picker sheets.
   window.Gestures.init()  (call after all sheets exist — see boot.js)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const THRESHOLD = 90;     // px pulled down to dismiss
  const VELOCITY  = 0.5;    // px/ms flick to dismiss regardless of distance

  function overlay() { return document.getElementById('sheetOverlay'); }

  function closeSheet(sheet) {
    sheet.style.transition = '';
    sheet.style.transform = '';
    sheet.classList.remove('open');
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  function snapBack(sheet) {
    sheet.style.transition = 'transform var(--dur-med) var(--spring)';
    sheet.style.transform = '';
    setTimeout(() => { sheet.style.transition = ''; }, 300);
  }

  function wire(sheet) {
    if (sheet.dataset.dragWired) return;
    sheet.dataset.dragWired = '1';

    let startY = 0, lastY = 0, startT = 0, dy = 0, dragging = false;

    const grabZone = t => !!(t.closest('.sheet-handle') || t.closest('.sheet-header'));

    sheet.addEventListener('touchstart', e => {
      if (e.touches.length !== 1 || !grabZone(e.target)) { dragging = false; return; }
      dragging = true;
      startY = lastY = e.touches[0].clientY;
      startT = Date.now();
      dy = 0;
      sheet.style.transition = 'none';
    }, { passive: true });

    sheet.addEventListener('touchmove', e => {
      if (!dragging) return;
      lastY = e.touches[0].clientY;
      dy = lastY - startY;
      if (dy > 0) {
        e.preventDefault();
        sheet.style.transform = `translateY(${dy}px)`;
      }
    }, { passive: false });

    sheet.addEventListener('touchend', () => {
      if (!dragging) return;
      dragging = false;
      const v = dy / Math.max(1, Date.now() - startT);
      if (dy > THRESHOLD || v > VELOCITY) closeSheet(sheet);
      else snapBack(sheet);
    });
  }

  function init() {
    document.querySelectorAll('.bottom-sheet').forEach(wire);
  }

  w.Gestures = { init };
})(window);
