/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   nav.js — Bottom navigation & page transitions
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
const Pages = {
  home:     $('#pageHome'),
  library:  $('#pageLibrary'),
  device:   $('#pageDevice'),
  settings: $('#pageSettings'),
};

function navigateTo(pageKey) {
  if (pageKey === State.activePage) return;
  const prev = Pages[State.activePage];
  const next = Pages[pageKey];
  if (!prev || !next) return;

  prev.classList.remove('active');
  prev.classList.add('exit-up');
  setTimeout(() => prev.classList.remove('exit-up'), 350);

  requestAnimationFrame(() => next.classList.add('active'));

  $$('.nav-item').forEach(item => {
    const hit = item.dataset.page === pageKey;
    item.classList.toggle('active', hit);
    item.setAttribute('aria-current', hit ? 'page' : 'false');
  });

  State.activePage = pageKey;
  const sa = next.querySelector('.scroll-area');
  if (sa) sa.scrollTop = 0;
}

$$('.nav-item').forEach(item => {
  item.addEventListener('click', e => { spawnRipple(e, item); navigateTo(item.dataset.page); });
});

/* Scroll shadow on home */
const _homeScroll = Pages.home?.querySelector('.scroll-area');
if (_homeScroll) {
  _homeScroll.addEventListener('scroll', () => {}, { passive: true });
}
