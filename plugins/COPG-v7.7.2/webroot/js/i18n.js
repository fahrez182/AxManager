/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   i18n.js — minimal translation engine (no fetch; file:// safe)

   Add a language: create js/lang/<code>.js that calls
     I18N.register('<code>', { _meta:{name, flag, dir}, key:'value', ... });
   then add one <script> tag for it in index.html. It auto-appears
   in the Language sheet and translates every [data-i18n] element.
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {
  const reg   = {};   // code -> dict
  const order = [];   // registration order (for the language list)
  let   cur   = 'en';

  const I18N = {
    register(code, dict) {
      if (!reg[code]) order.push(code);
      reg[code] = dict;
      return I18N;
    },
    has(code)  { return !!reg[code]; },
    get current() { return cur; },
    meta(code) { return (reg[code] && reg[code]._meta) || {}; },

    /* All registered languages, in load order — used to build the sheet */
    languages() {
      return order.map(code => ({
        code,
        name: I18N.meta(code).name || code,
        flag: I18N.meta(code).flag || '',
        dir:  I18N.meta(code).dir  || 'ltr',
      }));
    },

    /* Lookup with English fallback, then the raw key */
    t(key) {
      const d = reg[cur];
      if (d && key in d) return d[key];
      if (reg.en && key in reg.en) return reg.en[key];
      return key;
    },

    setLang(code) { if (reg[code]) cur = code; return cur; },

    /* Translate the DOM: textContent for [data-i18n],
       attributes for [data-i18n-attr="placeholder:key, aria-label:key2"] */
    apply(root) {
      root = root || document;
      const dir  = I18N.meta(cur).dir || 'ltr';
      const html = document.documentElement;
      html.setAttribute('lang', cur);
      html.setAttribute('dir', dir);
      if (document.body) document.body.setAttribute('dir', dir);

      root.querySelectorAll('[data-i18n]').forEach(el => {
        el.textContent = I18N.t(el.getAttribute('data-i18n'));
      });
      root.querySelectorAll('[data-i18n-attr]').forEach(el => {
        el.getAttribute('data-i18n-attr').split(',').forEach(pair => {
          const i = pair.indexOf(':');
          if (i < 0) return;
          const attr = pair.slice(0, i).trim();
          const key  = pair.slice(i + 1).trim();
          if (attr && key) el.setAttribute(attr, I18N.t(key));
        });
      });
    },
  };

  w.I18N = I18N;
})(window);
