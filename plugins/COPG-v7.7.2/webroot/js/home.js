/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   home.js — Hero status, stats, system info, console
   (About now lives on the Settings page — see settings.js)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */

/* ─── Hero status badge ─── */
// Real module status → badge colour + label. active=green, degraded/reboot/removing=amber,
// notloaded/zygisk_off=red, disabled=grey. Driven by the native boot heartbeat (see getSystemInfo).
const HERO_STATUS = {
  active:     { cls: 'active',   key: 'status_active' },
  degraded:   { cls: 'warn',     key: 'status_degraded' },
  reboot:     { cls: 'warn',     key: 'status_reboot' },
  removing:   { cls: 'warn',     key: 'status_removing' },
  notloaded:  { cls: 'danger',   key: 'status_notloaded' },
  zygisk_off: { cls: 'danger',   key: 'status_zygisk_off' },
  disabled:   { cls: 'neutral',  key: 'status_disabled' },
};
function updateHeroStatus() {
  const badge  = $('#heroBadge');
  if (!badge) return;
  const st = HERO_STATUS[State.moduleStatus] || HERO_STATUS.active;
  badge.className = `badge badge--${st.cls}`;
  const label = I18N.t(st.key);
  badge.innerHTML = `<span class="badge__dot"></span>${label}`;
  const envBadge = $('#envBadge');
  if (envBadge) {
    const extra = (State.rootSecondary && State.rootSecondary.length) ? ` +${State.rootSecondary.length}` : '';
    envBadge.textContent = State.rootEnv + extra;
    envBadge.title = (State.rootSecondary && State.rootSecondary.length)
      ? `${I18N.t('sys_root')}: ${State.rootEnv} · ${State.rootSecondary.map(r => r.name).join(', ')}`
      : `${I18N.t('sys_root')}: ${State.rootEnv}`;
  }
}

/* ─── Hero version badge next to the COPG logo (from module.prop) ─── */
function updateHeroVersion() {
  const v = State.sysInfo.version;
  const el = document.getElementById('heroVer');
  if (el && v) el.textContent = 'v' + v;
}

/* ─── Quick Stats ─── */
function updateStats() {
  const devCount = (typeof COPG !== 'undefined') ? COPG.listDevices().length : 0;
  const pkgCount = (typeof COPG !== 'undefined') ? COPG.listPackages().length : 0;
  const devEl = $('#statDevices');
  const pkgEl = $('#statPackages');
  if (devEl) devEl.textContent = devCount;
  if (pkgEl) pkgEl.textContent = pkgCount;
}

/* ─── System Info ─── */
function populateSysInfo() {
  const si = State.sysInfo;
  const map = {
    androidVer: si.androidVer,
    abiVal:     si.abi,
    zygiskVal:  si.zygisk,
    rootVerVal: si.rootVersionLabel || State.rootEnv || '—',
    gpuVal:     si.gpu,
    kernelVal:  si.kernel,
  };
  Object.entries(map).forEach(([id, val]) => {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  });
}

/* Console moved to Settings → Developer ("Open Console" row); handler lives in settings.js. */

/* ─── Entrance animation ─── */
function runEntranceAnim() {
  const items = [
    document.querySelector('.hero'),
    document.querySelector('.stats-grid'),
    document.querySelector('.sys-card'),
    document.querySelector('.console-card'),
  ].filter(Boolean);

  items.forEach((elem, i) => {
    elem.style.opacity = '0';
    elem.style.transform = 'translateY(16px)';
    elem.style.transition = 'opacity 400ms ease, transform 400ms cubic-bezier(0.34,1.56,0.64,1)';
    setTimeout(() => {
      elem.style.opacity = '';
      elem.style.transform = '';
      setTimeout(() => { elem.style.opacity=''; elem.style.transform=''; elem.style.transition=''; }, 450);
    }, 80 + i * 70);
  });
}

/* ─── Stat cards → open Library on the matching tab ─── */
$$('.stat-card[data-card]').forEach(card => {
  card.setAttribute('role', 'button');
  card.setAttribute('tabindex', '0');
  const open = () => {
    navigateTo('library');
    if (typeof switchLibTab === 'function') switchLibTab(card.dataset.card);
  };
  card.addEventListener('click', e => { spawnRipple(e, card); open(); });
  card.addEventListener('keydown', e => {
    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); open(); }
  });
});

/* ─── Init ─── */
updateHeroStatus();
updateStats();
populateSysInfo();
runEntranceAnim();
