/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   console.js — in-app developer / debug console
   Opened from the Home "Open Console" card. Provides:
     • live capture of console.log/info/warn/error + window errors/rejections
       (so module/WebUI failures surface here automatically)
     • a shell command runner through the KSU bridge (COPG.execCommand) — root,
       device-only; a no-op note in browser preview
     • a live logcat tail filtered to the module's tag (COPGModule)
     • clear / copy
   Exposes window.Console (open + log helpers) and window.Log.
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const MAX = 800;                 // cap buffered + rendered lines
  const buffer = [];               // { t, type, msg }
  let logcatOn = false, logcatBusy = false, logcatTimer = null;
  let greeted = false;

  /* ─── Inject the console sheet (same bottom-sheet family as modals) ─── */
  document.body.insertAdjacentHTML('beforeend', `
    <div class="bottom-sheet sheet--console" id="consoleSheet" role="dialog" aria-modal="true" aria-label="Console">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" data-i18n="console_title">Console</span>
        <button class="sheet-header__close" id="consoleClose" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="console-toolbar">
        <button type="button" class="con-btn" id="conLogcat">
          <span class="con-dot"></span><span data-i18n="console_logcat">Logcat</span>
        </button>
        <button type="button" class="con-btn" id="conClear" data-i18n="console_clear">Clear</button>
        <button type="button" class="con-btn" id="conCopy" data-i18n="console_copy">Copy</button>
        <button type="button" class="con-btn" id="conSave">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
          <span data-i18n="console_save">Save</span>
        </button>
      </div>
      <div class="console-output" id="consoleOutput"></div>
      <form class="console-input" id="consoleForm" autocomplete="off">
        <span class="console-input__prompt">$</span>
        <input type="text" id="consoleCmd" spellcheck="false" autocapitalize="off" autocorrect="off"
               data-i18n-attr="placeholder:console_cmd_ph" placeholder="Type a shell command…" />
        <button type="submit" class="con-run" aria-label="Run">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polygon points="5 3 19 12 5 21 5 3"/></svg>
        </button>
      </form>
    </div>`);

  const byId = id => document.getElementById(id);
  const overlay = () => byId('sheetOverlay');
  const sheet   = () => byId('consoleSheet');
  const out     = () => byId('consoleOutput');
  const isOpen  = () => sheet() && sheet().classList.contains('open');

  /* ─── Log buffer + rendering ─── */
  function fmt(a) {
    if (typeof a === 'string') return a;
    if (a instanceof Error) return a.message;
    try { return JSON.stringify(a); } catch (_) { return String(a); }
  }
  function stamp() {
    const d = new Date(), p = n => ('0' + n).slice(-2);
    return p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
  }
  function paint(e) {
    const o = out(); if (!o) return;
    const atBottom = o.scrollHeight - o.scrollTop - o.clientHeight < 48;
    const div = document.createElement('div');
    div.className = 'con-line con-' + e.type;
    div.innerHTML = '<span class="con-ts"></span><span class="con-msg"></span>';
    div.querySelector('.con-ts').textContent = e.t;
    div.querySelector('.con-msg').textContent = e.msg;
    o.appendChild(div);
    while (o.childNodes.length > MAX) o.removeChild(o.firstChild);
    if (atBottom) o.scrollTop = o.scrollHeight;
  }
  function add(msg, type) {
    const e = { t: stamp(), type: type || 'info', msg: String(msg) };
    buffer.push(e);
    if (buffer.length > MAX) buffer.shift();
    if (isOpen()) paint(e);
  }
  function renderAll() {
    const o = out(); if (!o) return;
    o.innerHTML = '';
    buffer.forEach(paint);
    o.scrollTop = o.scrollHeight;
  }

  // Info-level logs are gated by the Settings → "Debug Logs" toggle; warnings
  // and errors always show (you always want to see failures).
  const debugOn = () => !!(w.State && State.debugLogs);
  function addGated(msg, type) { if (debugOn()) add(msg, type || 'info'); }

  const Log = {
    info:    m => addGated(m, 'info'),
    success: m => addGated(m, 'success'),
    warn:    m => add(m, 'warning'),
    error:   m => add(m, 'error'),
    log:     m => addGated(m, 'info'),
  };

  /* ─── Capture native console + uncaught errors ─── */
  ['log', 'info', 'warn', 'error'].forEach(k => {
    const orig = console[k] ? console[k].bind(console) : function () {};
    console[k] = (...args) => {
      orig(...args);
      try {
        const msg = args.map(fmt).join(' ');
        if (k === 'error')      add(msg, 'error');       // always
        else if (k === 'warn')  add(msg, 'warning');     // always
        else                    addGated(msg, 'info');   // gated by Debug Logs
      } catch (_) {}
    };
  });
  w.addEventListener('error', ev => {
    try { add(`Error: ${ev.message} (${String(ev.filename || '').split('/').pop()}:${ev.lineno})`, 'error'); } catch (_) {}
  });
  w.addEventListener('unhandledrejection', ev => {
    try { const r = ev.reason; add('Unhandled rejection: ' + ((r && r.message) || r), 'error'); } catch (_) {}
  });

  /* ─── Shell command runner (root via bridge; device-only) ─── */
  /* ksu.exec resolves only when the spawned process EXITS, buffering all its
     stdout first. A command that never terminates (bare `logcat`, `top`,
     `tail -f`, `cat /dev/…`) therefore never resolves: its output balloons and
     the bridge — and the whole device — hangs. Guard the runner:
       • bare `logcat` (no -d) → rewrite to `logcat -d` (dump current buffer & exit)
       • other known infinite streamers → refuse with a hint instead of hanging. */
  function fixStreaming(cmd) {
    // `-d`/`-c`/`-g`/`-S`/`--help` make logcat exit; otherwise it streams.
    if (/\blogcat\b/.test(cmd) && !/\blogcat\b[^|;&]*\s-d\b/.test(cmd) && !/-[cgS]\b|--help/.test(cmd))
      return cmd.replace(/\blogcat\b/, 'logcat -d');
    return cmd;
  }
  function isInfinite(cmd) {
    return /(^|[;&|]\s*)(top|htop)\b(?![^|;&]*\s-n\b)/.test(cmd)          // top without -n
        || /\btail\s+-[fF]\b/.test(cmd)                                  // tail -f
        || /\bgetevent\b(?![^|;&]*-c\b)/.test(cmd)                       // getevent without -c
        || /\bcat\b[^|;&]*(\/dev\/|\/proc\/kmsg)/.test(cmd);             // cat of a stream
  }

  async function runCmd(cmd) {
    cmd = (cmd || '').trim();
    if (!cmd) return;
    add('$ ' + cmd, 'cmd');
    if (isInfinite(cmd)) {
      add('That command never exits and would hang the bridge. Add a count / `-d` / file redirect so it terminates.', 'warning');
      return;
    }
    const run = fixStreaming(cmd);
    if (run !== cmd) add('↪ ran as: ' + run + '   (bare logcat streams forever — added -d)', 'warning');
    if (!w.COPG || !COPG.hasBridge()) { add('No bridge — shell commands run on-device only.', 'warning'); return; }
    try {
      const o = await COPG.execCommand(run);
      add(o && o.trim() ? o.replace(/\s+$/, '') : '(no output)', 'out');
    } catch (err) { add(String(err), 'error'); }
  }

  /* ─── Logcat tail (module tag) ─── */
  function setLogcatUI() {
    const btn = byId('conLogcat');
    if (btn) btn.classList.toggle('active', logcatOn);
  }
  async function pollLogcat() {
    if (!logcatOn || logcatBusy) return;
    logcatBusy = true;
    try {
      const o = await COPG.execCommand('logcat -d -s COPGModule 2>/dev/null; logcat -c 2>/dev/null; true');
      if (o) o.split('\n').forEach(line => {
        line = line.trim();
        if (!line || line.indexOf('--------- beginning') === 0) return;
        const t = /\sE\s|\bERROR\b/.test(line) ? 'error' : (/\sW\s|\bWARN\b/.test(line) ? 'warning' : 'info');
        add(line, t);
      });
    } catch (err) { add('logcat: ' + err, 'error'); }
    finally { logcatBusy = false; }
  }
  function startLogcat() {
    if (logcatOn) return;
    if (!w.COPG || !COPG.hasBridge()) { add('No bridge — logcat runs on-device only.', 'warning'); return; }
    logcatOn = true; setLogcatUI();
    add('● logcat started — tag: COPGModule (open the target app to see hooks)', 'success');
    COPG.execCommand('logcat -c').catch(() => {});
    logcatTimer = setInterval(pollLogcat, 1500);
  }
  function stopLogcat() {
    if (!logcatOn) return;
    logcatOn = false; setLogcatUI();
    if (logcatTimer) { clearInterval(logcatTimer); logcatTimer = null; }
    add('● logcat stopped', 'warning');
  }

  /* ─── Save log to file ─── */
  // Aligned columns: "HH:MM:SS  TYPE     message" + a small header. Device →
  // COPG.saveLog() writes it under Download/COPG/LOGS; preview → blob download.
  function buildLogText() {
    const head = '# COPG console log — ' + new Date().toISOString() +
                 (w.COPG && COPG.hasBridge() ? '' : ' (preview)');
    const lines = buffer.map(e => `${e.t}  ${(e.type || 'info').toUpperCase().padEnd(7)} ${e.msg}`);
    return head + '\n' + '─'.repeat(40) + '\n' + lines.join('\n') + '\n';
  }
  function blobDownload(name, data) {
    try {
      const url = URL.createObjectURL(new Blob([data], { type: 'text/plain' }));
      const a = document.createElement('a');
      a.href = url; a.download = name;
      document.body.appendChild(a); a.click(); a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1500);
    } catch (e) { console.warn('log download:', e); }
  }
  const toast = key => { if (w.showToast && w.I18N) showToast(I18N.t(key)); };
  async function saveLog() {
    if (!buffer.length) { toast('console_save_empty'); return; }
    if (!w.COPG || !COPG.saveLog) { toast('console_save_fail'); return; }
    const text = buildLogText();
    try {
      const res = await COPG.saveLog(text);
      if (res.preview) { blobDownload(res.name, res.data); toast('console_saved_dl'); add('Log downloaded: ' + res.name, 'success'); }
      else             { toast('console_saved'); add('Log saved: ' + res.path, 'success'); }
    } catch (err) { toast('console_save_fail'); add('Save log failed — ' + err, 'error'); }
  }

  /* ─── Open / close ─── */
  function open() {
    if (!greeted) {
      add('COPG console ready' + (w.COPG && COPG.hasBridge() ? '' : ' (preview — no bridge)'), 'success');
      if (!debugOn()) add('Tip: turn on “Debug Logs” in Settings to see UI activity here.', 'info');
      greeted = true;
    }
    overlay() && overlay().classList.add('visible');
    requestAnimationFrame(() => sheet() && sheet().classList.add('open'));
    renderAll();
  }
  function close() {
    stopLogcat();
    sheet() && sheet().classList.remove('open');
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay() && overlay().classList.remove('visible');
  }

  /* ─── Wire UI ─── */
  byId('consoleClose').addEventListener('click', close);
  byId('conLogcat').addEventListener('click', () => (logcatOn ? stopLogcat() : startLogcat()));
  byId('conClear').addEventListener('click', () => {
    buffer.length = 0; renderAll();
    if (w.showToast && w.I18N) showToast(I18N.t('console_cleared'));
  });
  byId('conCopy').addEventListener('click', async () => {
    const text = buffer.map(e => `${e.t} ${e.msg}`).join('\n');
    let ok = false;
    try { if (navigator.clipboard) { await navigator.clipboard.writeText(text); ok = true; } } catch (_) {}
    if (w.showToast && w.I18N) showToast(I18N.t(ok ? 'console_copied' : 'console_copy_fail'));
  });
  byId('conSave').addEventListener('click', saveLog);
  byId('consoleForm').addEventListener('submit', e => {
    e.preventDefault();
    const input = byId('consoleCmd');
    const cmd = input.value;
    input.value = '';
    runCmd(cmd);
  });
  // keep the command input above the on-screen keyboard
  byId('consoleCmd').addEventListener('focus', () => {
    setTimeout(() => byId('consoleCmd').scrollIntoView({ block: 'nearest' }), 250);
  });

  w.Console = { open, close, log: Log.log, info: Log.info, success: Log.success, warn: Log.warn, error: Log.error, run: runCmd };
  w.Log = Log;
})(window);
