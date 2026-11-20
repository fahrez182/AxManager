/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   icons.js — App icon loader (lazy, local-file based)
   Every icon resolves to the SAME local file icons/<pkg>.png; only what FILLS it
   differs, and neither path makes a per-icon bridge call:
     • installed apps  → rendered from the on-device APK by the root userprobe
       (COPG.probeRenderIcons → app_process, any Android user incl. clones,
       offline). Replaces the old owner-only ksu://icon scheme.
     • non-installed    → downloaded from Play/APKPure/F-Droid by ONE detached
       shell (COPG.batchFetchIcons) — their APK isn't on device to render.
     • anything missing → inline <svg> fallback (data: URIs are CSP-blocked, so a
       data:image/svg fallback would render broken — inline DOM is immune)
   Rows whose file isn't there yet show the SVG, register a watcher, and enqueue
   for the right filler; retries upgrade SVG→real icon as files land (the probe
   render is synchronous, so its retry fires immediately). Fully offline once
   files exist; covers user-added + clone-user packages; no UI jank.
   window.Icons.load(el, pkg, label)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  /* ── deterministic colour + initial fallback ── */
  const PALETTE = [
    ['#818CF8', '#A78BFA'], ['#34D399', '#10B981'], ['#67E8F9', '#22D3EE'],
    ['#F472B6', '#EC4899'], ['#FBBF24', '#F59E0B'], ['#F87171', '#EF4444'],
    ['#A3E635', '#84CC16'], ['#60A5FA', '#3B82F6'],
  ];
  function hash(str) {
    let h = 0;
    for (let i = 0; i < str.length; i++) { h = (h << 5) - h + str.charCodeAt(i); h |= 0; }
    return Math.abs(h);
  }
  /* Letter = first char of the DISPLAYED NAME, so it always matches the visible title. When a real
     label is resolved later (enrichApps → the probe), Icons.setLabel repaints this with the real
     name (com.whatsapp → "WhatsApp" → "W"); a label-less system overlay keeps its package title
     (android.overlay.common → "A", matching the row). Empty label → last package segment. */
  function initialOf(label, pkg) {
    const s = (label || '').trim() || (pkg || '').split('.').filter(Boolean).pop() || '?';
    const ch = s.replace(/[^\p{L}\p{N}]/u, '').charAt(0) || s.charAt(0) || '?';
    return ch.toUpperCase();
  }
  function escapeXml(s) {
    return String(s).replace(/[<>&]/g, c => (c === '<' ? '&lt;' : c === '>' ? '&gt;' : '&amp;'));
  }
  /* Raw <svg> markup for INLINE DOM injection (NOT a data: URI — CSP-safe). */
  function genSVGMarkup(pkg, label) {
    const [c1, c2] = PALETTE[hash(pkg || label || '?') % PALETTE.length];
    const ch = escapeXml(initialOf(label, pkg));
    const id = 'g' + (hash(pkg || label || '?') % 100000);
    return (
      `<svg class="app-icon-img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40">` +
      `<defs><linearGradient id="${id}" x1="0" y1="0" x2="1" y2="1">` +
      `<stop offset="0" stop-color="${c1}"/><stop offset="1" stop-color="${c2}"/></linearGradient></defs>` +
      `<rect width="40" height="40" rx="11" fill="url(#${id})"/>` +
      `<text x="20" y="21" font-family="system-ui,sans-serif" font-size="19" font-weight="700" ` +
      `fill="#0A0A12" fill-opacity="0.85" text-anchor="middle" dominant-baseline="central">${ch}</text></svg>`
    );
  }

  function safeName(pkg) { return String(pkg).replace(/[^A-Za-z0-9._-]/g, '_'); }
  // Set on refresh() so subsequent local-file <img> bypass the WebView image
  // cache (a deleted file may otherwise serve a stale cached hit).
  let BUST = 0;
  function localUrl(pkg, bust) {
    const v = bust || BUST;
    return 'icons/' + safeName(pkg) + '.png' + (v ? ('?v=' + v) : '');
  }

  /* Inline <svg> fallback — CSP-safe, always renders. */
  function paintSVG(el, pkg, label) { el.innerHTML = genSVGMarkup(pkg, label); }

  /* ── Batch download coordination ──
     Non-installed rows whose local file isn't there yet register as "watchers"
     and enqueue their pkg. A debounce fires ONE COPG.batchFetchIcons for the
     whole set; timed retries then re-attempt the local file as the worker fills
     them (no extra bridge polling). ── */
  const WATCH = new Map();        // el -> { pkg, label, installed } — keyed by ELEMENT so every clone row (same pkg) is retried, not just the last
  const PENDING = new Set();      // NON-installed pkgs → scrape (COPG.batchFetchIcons)
  const PENDING_PROBE = new Set();// INSTALLED pkgs → root probe render (COPG.probeRenderIcons)
  let batchTimer = null;
  const RETRY_AT = [4000, 10000, 22000, 45000];  // ms after a SCRAPE batch fires (network, slower)

  function bridgeReady() {
    return !!(w.COPG && COPG.hasBridge && COPG.hasBridge());
  }
  function enqueueBatch(pkg, installed) {
    if (!bridgeReady()) return;
    (installed ? PENDING_PROBE : PENDING).add(pkg);
    clearTimeout(batchTimer);
    batchTimer = setTimeout(flushBatch, 400);
  }
  async function flushBatch() {
    const scrape = [...PENDING]; PENDING.clear();
    const probe = [...PENDING_PROBE]; PENDING_PROBE.clear();
    if (!bridgeReady()) return;
    // NON-installed → download from Play/APKPure/F-Droid (fire-and-forget, slow).
    if (scrape.length && COPG.batchFetchIcons) { COPG.batchFetchIcons(scrape); RETRY_AT.forEach(t => setTimeout(retryWatchers, t)); }
    // INSTALLED → render the on-device APK icon as root (synchronous; any user, offline).
    if (probe.length && COPG.probeRenderIcons) {
      try { await COPG.probeRenderIcons(probe); } catch (_) {}
      retryWatchers();                            // files are ready the moment the render returns
    }
  }
  function retryWatchers() {
    if (!WATCH.size) return;
    const bust = Date.now();
    WATCH.forEach((r, el) => tryLocal(el, r.pkg, r.label, bust, r.installed));
  }

  /* Attempt the local file; success paints it (+clears watcher), failure shows
     the SVG fallback, registers a watcher, and enqueues the pkg for the batch.
     WATCH is keyed by the ELEMENT, not the pkg — the same package appears on
     several rows at once (one per Android user/clone), so a pkg-keyed map would
     retry only the last row and leave every other clone row on the letter tile. */
  function tryLocal(el, pkg, label, bust, installed) {
    const img = new Image();
    img.alt = '';
    img.decoding = 'async';
    img.className = 'app-icon-img';
    img.style.opacity = '0';
    img.onload = () => {
      img.style.opacity = '1';
      el.innerHTML = ''; el.appendChild(img);
      WATCH.delete(el);
    };
    img.onerror = () => {
      if (!el.firstChild) paintSVG(el, pkg, label);
      else if (!el.querySelector('svg')) paintSVG(el, pkg, label);
      WATCH.set(el, { pkg, label, installed: !!installed });
      enqueueBatch(pkg, installed);
    };
    img.src = localUrl(pkg, bust);
  }

  async function resolve(el, pkg, label) {
    // Both installed and non-installed resolve to the SAME local file icons/<pkg>.png; only the
    // SOURCE that FILLS it differs — installed apps are rendered from the on-device APK by the root
    // userprobe (any user, offline, replaces the owner-only ksu://icon), non-installed are scraped
    // from Play/APKPure/F-Droid. tryLocal's onerror routes the miss to the right filler.
    let installed = false;
    try {
      const set = (w.COPG && COPG.getInstalledPackages) ? await COPG.getInstalledPackages() : null;
      installed = !!(set && set.has(pkg));
    } catch (_) {}
    tryLocal(el, pkg, label, undefined, installed);
  }

  /* Shared lazy observer */
  let observer = null;
  function ensureObserver() {
    if (observer) return observer;
    observer = new IntersectionObserver(entries => {
      entries.forEach(e => {
        if (!e.isIntersecting) return;
        const el = e.target;
        observer.unobserve(el);
        resolve(el, el.dataset.pkg, el.dataset.label || '');
      });
    }, { rootMargin: '200px', threshold: 0.01 });
    return observer;
  }

  function load(el, pkg, label) {
    if (!el) return;
    el.dataset.pkg = pkg;
    el.dataset.label = label || '';
    // instant inline-SVG placeholder so layout never flashes empty
    if (!el.firstChild) paintSVG(el, pkg, label);
    ensureObserver().observe(el);
  }

  /* Wipe cached files + in-memory state so every icon re-downloads fresh.
     Caller repaints the library afterwards (re-runs Icons.load on new rows). */
  async function refresh() {
    WATCH.clear(); PENDING.clear(); clearTimeout(batchTimer);
    BUST = Date.now();   // cache-bust all subsequent local-file loads
    if (w.COPG && COPG.clearIconCache) { try { await COPG.clearIconCache(); } catch (_) {} }
  }

  /* A real display name arrived later (enrichApps → probe labels): update the stored label and,
     if this icon is still the letter placeholder (no real icon rendered/downloaded yet), repaint
     it so the letter follows the real name. A real <img> icon is left untouched. */
  function setLabel(el, label) {
    if (!el || !label) return;
    const pkg = el.dataset.pkg || '';
    el.dataset.label = label;
    if (el.querySelector('svg')) paintSVG(el, pkg, label);
  }

  w.Icons = { load, setLabel, genSVGMarkup, refresh };
})(window);
