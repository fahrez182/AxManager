/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   theme.js — Theme & language management + bottom sheets
   (translations live in js/i18n.js + js/lang/*.js)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */

/* ─── Theme ─── */
function applyTheme(theme) {
  State.theme = theme;
  const html = document.documentElement;
  if (theme === 'auto') {
    html.setAttribute('data-theme', window.matchMedia('(prefers-color-scheme:dark)').matches ? 'amoled' : 'light');
  } else {
    html.setAttribute('data-theme', theme);
  }
  // Keep the status-bar tint (theme-color meta) in sync with the resolved base
  // colour, so the system bar matches instead of showing a black band.
  try {
    const base = getComputedStyle(html).getPropertyValue('--bg-base').trim();
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta && base) meta.setAttribute('content', base);
  } catch(_) {}
  // Update sheet options
  $$('.sheet-option[data-theme-option]').forEach(o => o.classList.toggle('selected', o.dataset.themeOption === theme));
  // Update hint on settings row (localized)
  const hint = $('#themeRowHint');
  if (hint) hint.textContent = I18N.t('theme_' + theme);
  try { localStorage.setItem('copg-theme', theme); } catch(_) {}
}

/* ─── Language ─── */
function applyLanguage(lang) {
  State.language = I18N.setLang(lang);
  I18N.apply();                       // translates [data-i18n], sets <html lang/dir>

  // Reflect selection + hint
  $$('.sheet-option[data-lang]').forEach(o => o.classList.toggle('selected', o.dataset.lang === State.language));
  const langHint = $('#langRowHint');
  if (langHint) langHint.textContent = I18N.meta(State.language).name || State.language;
  // Theme hint is language-dependent too
  const themeHint = $('#themeRowHint');
  if (themeHint) themeHint.textContent = I18N.t('theme_' + State.theme);

  if (typeof updateHeroStatus === 'function') updateHeroStatus();
  // Re-render anything built dynamically with I18N.t() at render time —
  // I18N.apply() only touches static [data-i18n] nodes, so library cards,
  // the sort-pill labels and filter chips would otherwise keep the old language.
  if (typeof updateSortLabels === 'function') updateSortLabels();
  if (typeof renderDevices === 'function')    renderDevices();
  if (typeof renderPackages === 'function')   renderPackages();
  try { localStorage.setItem('copg-lang', State.language); } catch(_) {}
}

/* ─── Build the language sheet from registered languages ─── */
function buildLangSheet() {
  const body = $('#langSheet .sheet-body');
  if (!body) return;
  body.innerHTML = '';
  I18N.languages().forEach(l => {
    const row = document.createElement('div');
    row.className = 'sheet-option';
    row.dataset.lang = l.code;
    row.innerHTML =
      `<div class="sheet-option__icon" style="font-size:1.2rem;background:transparent;border:none;">${l.flag}</div>` +
      `<span class="sheet-option__label">${l.name}</span>` +
      `<svg class="sheet-option__check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>`;
    row.addEventListener('click', () => { applyLanguage(l.code); closeAllSheets(); });
    body.appendChild(row);
  });
}

/* ─── Bottom sheet helpers ─── */
// Clear any inline transform/transition left by a drag-to-dismiss gesture — it
// would otherwise override `.bottom-sheet.open` and keep the sheet off-screen.
function clearSheetDrag(el) { if (el) { el.style.transition = ''; el.style.transform = ''; } }

function openSheet(sheetId) {
  const overlay = $('#sheetOverlay');
  const sheet   = $(`#${sheetId}`);
  if (!overlay || !sheet) return;
  overlay.classList.add('visible');
  clearSheetDrag(sheet);
  requestAnimationFrame(() => sheet.classList.add('open'));
}

function closeSheet(sheetId) {
  const overlay = $('#sheetOverlay');
  const sheet   = $(`#${sheetId}`);
  if (!sheet) return;
  sheet.classList.remove('open');
  clearSheetDrag(sheet);
  if (!$$('.bottom-sheet.open').length) overlay?.classList.remove('visible');
}

function closeAllSheets() {
  $$('.bottom-sheet').forEach(s => { s.classList.remove('open'); clearSheetDrag(s); });
  $('#sheetOverlay')?.classList.remove('visible');
}

/* ─── Wire up settings rows that open sheets ─── */
$('#themeSettingRow')?.addEventListener('click', () => openSheet('themeSheet'));
$('#langSettingRow')?.addEventListener('click',  () => openSheet('langSheet'));

/* ─── Sheet close buttons ─── */
$$('.sheet-header__close').forEach(btn => {
  btn.addEventListener('click', () => closeAllSheets());
});
$('#sheetOverlay')?.addEventListener('click', closeAllSheets);

/* ─── Theme sheet options ─── */
$$('.sheet-option[data-theme-option]').forEach(opt => {
  opt.addEventListener('click', () => { applyTheme(opt.dataset.themeOption); closeAllSheets(); });
});

/* ─── Auto system theme listener ─── */
window.matchMedia('(prefers-color-scheme:dark)').addEventListener('change', () => {
  if (State.theme === 'auto') applyTheme('auto');
});

/* ─── Init from saved prefs ─── */
const _savedTheme = (() => { try { return localStorage.getItem('copg-theme') || 'dark'; } catch(_){ return 'dark'; }})();
const _savedLang  = (() => { try { return localStorage.getItem('copg-lang')  || 'en';   } catch(_){ return 'en';   }})();
buildLangSheet();
applyTheme(_savedTheme);
applyLanguage(I18N.has(_savedLang) ? _savedLang : 'en');
