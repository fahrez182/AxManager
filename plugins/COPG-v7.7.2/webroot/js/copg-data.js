/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   copg-data.js — COPG.json / list.json data layer
   (bridge + parse + model + persistence; no DOM)

   COPG.json shape (shared with zygisk/binaries) — UNIFIED model, no cpu_spoof:
     "PACKAGES_THIS_DEVICE":  [ "com.x:cpu=sd8elite", "com.y:blocked" ] // built-in identity profile
     "PACKAGES_THIS_DEVICE_DEVICE": { }                                 // empty Build = no spoof (tags only)
     "PACKAGES_<KEY>":        [ "com.x:cpu=<key>", "com.y:blocked" ]    // a device's game list
     "PACKAGES_<KEY>_DEVICE": { BRAND, DEVICE, MANUFACTURER, MODEL,
                                FINGERPRINT, PRODUCT, SERIAL?, ANDROID_VERSION?, SDK_INT?,
                                // ANDROID_ID / GAID are NO LONGER profile fields — the :aid / :gaid tag
                                // derives a distinct id per package (or an aid=/gaid= literal pins one)
                                // EMPTY string on any field = that field is NOT spoofed (native skip-empty)
                                // optional extra Build fields (JNI + COW), all strings:
                                BOARD?, HARDWARE?, DISPLAY?, ID?, BOOTLOADER?, TAGS?, TYPE?,
                                SECURITY_PATCH?, INCREMENTAL?, CODENAME?, SOC_MANUFACTURER?, SOC_MODEL? }
   Insertion order of keys is meaningful and preserved on save (keyOrder).
   Package tags are colon suffixes: pkg:cpu=<key> (CPU spoof + model), pkg:blocked (force real CPU),
   pkg:cow, pkg:aid, pkg:gpu=<key>, + tweak tags. Legacy cpu_spoof.{blacklist,cpu_only_packages}
   are auto-migrated into PACKAGES_THIS_DEVICE on load (migrateLegacyCpu) then dropped.
   Logic ported from the previous WebUI (old.js) for full parity.
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {

  const MODULE_DIR = '/data/adb/modules/COPG';
  const CONFIG_PATH = `${MODULE_DIR}/COPG.json`;
  const LIST_PATH   = `${MODULE_DIR}/list.json`;
  // Live ':vpns' target list read by the system_server VPN hook (no-reboot). system_data_file.
  const VPNS_TARGETS_PATH = '/data/system/copg_vpns.txt';
  // Live 'lang=' target map (one "package<TAB>bcp47" per line) read by the system_server locale hook
  // (no-reboot). system_data_file so system_server can read it.
  const LANG_TARGETS_PATH = '/data/system/copg_lang.txt';
  // Live 'wallpaper'/'wallpaper=' target map (one "package<TAB>id" per line) read by the system_server
  // wallpaper-id hook (no-reboot). system_data_file so system_server can read it.
  const WALLPAPER_TARGETS_PATH = '/data/system/copg_wallpaper.txt';
  // Live 'account'/'account=' target map (one "package<TAB>email" per line; bare = HIDE) read by the
  // system_server account hook (no-reboot). system_data_file so system_server can read it.
  const ACCOUNT_TARGETS_PATH = '/data/system/copg_account.txt';
  // Live 'aid'/'aid=' target map (one "package<TAB>literal" per line; bare = derive) read by the
  // system_server SsaidHook (SettingsProvider.getSsaidSettingLocked) so the query/call/shell read
  // paths match the in-app getString forge, no reboot. system_data_file so system_server can read it.
  const SSAID_TARGETS_PATH = '/data/system/copg_ssaid.txt';
  // Live ':playsrc' / ':pairip' target lists (package names, one per line) read by the system_server
  // PlaysrcHook every ~1.5s. system_data_file so system_server can read them.
  const PLAYSRC_TARGETS_PATH = '/data/system/copg_playsrc.txt';
  const PAIRIP_TARGETS_PATH  = '/data/system/copg_pairip.txt';
  // Live ':tensorfeat' target list (system_server TensorfeatHook re-reads it every ~1.5s).
  const TENSORFEAT_TARGETS_PATH = '/data/system/copg_tensorfeat.txt';
  // Live ':hidedev' target list (system_server HidedevHook re-reads it every ~1.5s — the system-side
  // half of hidedev that covers the direct SettingsProvider read path).
  const HIDEDEV_TARGETS_PATH = '/data/system/copg_hidedev.txt';
  const BACKUP_DIR  = '/sdcard/Download/COPG';
  const LOG_DIR     = '/sdcard/Download/COPG/LOGS';
  // GitHub "Sync" — same source the module's update_config.sh pulls from
  // (AlirezaParsi/COPG, JSON branch). Downloaded straight to stdout, parsed +
  // applied + saved through the normal path; downloader fallback curl → wget →
  // busybox wget (each a single, simple command — no compound shell).
  // COPG.json / list.json live under module/ in the repo; branch stays JSON (this
  // repo is PR'd into JSON), so only the path carries the module/ prefix.
  const SYNC_BASE       = 'https://raw.githubusercontent.com/AlirezaParsi/COPG/refs/heads/JSON/module';
  const SYNC_CONFIG_URL = `${SYNC_BASE}/COPG.json`;
  const SYNC_LIST_URL   = `${SYNC_BASE}/list.json`;
  // CPU profile library — synced MANIFEST-DRIVEN (no hardcoded filenames): pull the
  // manifest, then each CPU/cpuinfo_<key> it lists, so adding a profile to the repo
  // needs ZERO WebUI change. GPU manifest is best-effort (only exists once GPU is
  // productized to the JSON branch; 404 → silently skipped).
  const SYNC_CPU_MANIFEST_URL = `${SYNC_BASE}/CPU/manifest.json`;
  const SYNC_CPU_FILE_BASE    = `${SYNC_BASE}/CPU/`;            // + cpuinfo_<key>
  const SYNC_GPU_MANIFEST_URL = `${SYNC_BASE}/GPU/manifest.json`;

  /* ─── KernelSU exec bridge (Promise wrapper, from old.js) ─── */
  function execCommand(command) {
    return new Promise((resolve, reject) => {
      const cb = `exec_cb_${Date.now()}_${Math.floor(Math.random() * 1e6)}`;
      w[cb] = (errno, stdout, stderr) => {
        delete w[cb];
        if (errno === 0) resolve(stdout || '');
        else reject(stderr || `Command failed (code ${errno})`);
      };
      try {
        if (typeof ksu !== 'undefined' && ksu.exec) ksu.exec(command, '{}', cb);
        else { delete w[cb]; reject('KSU API not available'); }
      } catch (e) { delete w[cb]; reject(String(e)); }
    });
  }

  /* Streaming variant of execCommand for BIG-output commands (installed-package enumerate,
     the userprobe dump, `pm list packages`). ksu.exec BUFFERS the whole stdout and fires one
     callback on process exit → the WebView blocks until the command finishes and can hit the
     large-stdout timeout. ksu.spawn (MMRL WebUI-X) STREAMS stdout in chunks, so we accumulate
     without the single-giant-callback block or the buffer wall. Resolves the full stdout, same
     contract as execCommand. FEATURE-DETECTED: classic KsuWebUI has no ksu.spawn → and any spawn
     hiccup → it transparently falls back to execCommand, so the exec path is never lost. */
  let _spawnProbed = false;
  function spawnCommand(command) {
    if (!_spawnProbed) {
      _spawnProbed = true;
      try { console.warn('[bridge] ksu.spawn ' + (typeof ksu !== 'undefined' && typeof ksu.spawn === 'function'
        ? 'AVAILABLE → streaming big calls' : 'absent → exec fallback')); } catch (_) {}
    }
    if (typeof ksu === 'undefined' || typeof ksu.spawn !== 'function') return execCommand(command);
    return new Promise((resolve, reject) => {
      let out = '', err = '', settled = false;
      const chunk = d => (d == null ? '' : (typeof d === 'string' ? d : (d.data !== undefined ? d.data : String(d))));
      const fallback = () => { if (!settled) { settled = true; execCommand(command).then(resolve, reject); } };
      try {
        const child = ksu.spawn('sh', ['-c', command]);
        if (!child || !child.stdout || !child.stdout.on) return fallback();   // unexpected shape → exec
        child.stdout.on('data', d => { out += chunk(d); });
        if (child.stderr && child.stderr.on) child.stderr.on('data', d => { err += chunk(d); });
        child.on('exit', code => {
          if (settled) return; settled = true;
          if (code === 0 || code === undefined || code === null) resolve(out);
          else reject(err || `Command failed (code ${code})`);
        });
        child.on('error', () => fallback());     // spawn itself broke → exec
      } catch (e) { fallback(); }
    });
  }

  const hasBridge = () => (typeof ksu !== 'undefined' && !!ksu.exec);

  /* shell-single-quote escape: ' → '\'' */
  const shq = s => String(s).replace(/'/g, "'\\''");

  /* Run ONE command in the GLOBAL mount namespace via `su -M` (mount-master). KernelSU / SukiSU /
     ReSukiSU place su subprocesses in an ISOLATED mount namespace by default, in which per-user
     reads like `pm list packages --user <id>` see an empty view and return nothing (while the
     same command in Termux root — global ns — works). -M runs just this query in the global ns,
     with no effect on any other module command. Used only for the per-user package fan-out. */
  const execMaster = cmd => execCommand("su -M -c '" + shq(cmd) + "'");

  /* ─── Tag helpers (verbatim parity with old.js) ─── */
  const clean   = pkg => pkg.split(':')[0];
  const tagsOf  = pkg => pkg.split(':').slice(1);
  const join    = (name, tags) => tags.length ? `${name}:${tags.join(':')}` : name;
  function addTag(pkg, tag) {
    const t = tagsOf(pkg); if (!t.includes(tag)) t.push(tag);
    return join(clean(pkg), t);
  }
  function removeTag(pkg, tag) {
    return join(clean(pkg), tagsOf(pkg).filter(x => x !== tag));
  }
  const hasTag = (pkg, tag) => tagsOf(pkg).includes(tag);

  /* ─── Per-user scope value-tag (u=<id>) ───
     Scopes a package entry to ONE Android user (uid/100000): owner = 0, work profile /
     ROM clone / dual-app ≥ 10 (HyperOS dual-apps = 999). '' (no tag) = ALL users, the
     back-compat default. Lets the SAME package carry a different spoof per user — the
     native match (spoof_module.cpp) prefers the entry whose u= equals uid/100000, else
     an unscoped one. '@'-suffix idPackage() mirrors the native per-user derive so a
     clone's derived android_id / serial / GAID / App Set ID / IMEI differ from the owner's. */
  const userScopeOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('u=') === 0);
    return t ? t.slice(2) : '';                      // '' = all users
  };
  function setUserScope(pkg, id) {
    const t = tagsOf(pkg).filter(x => x.indexOf('u=') !== 0);   // drop any existing u=*
    const s = String(id == null ? '' : id).trim();
    if (s !== '' && /^[0-9]+$/.test(s)) t.push('u=' + s);       // '' → all users (no tag)
    return join(clean(pkg), t);
  }
  /* Byte-identical to the native id_package: only a non-owner user (>0) gets the "@u<id>"
     suffix, so owner + every unscoped entry derive exactly as before. Feed this to the
     derive*() ports so the modal ⓘ previews the value the app will actually read. */
  function idPackage(cleanName, userScope) {
    const n = parseInt(userScope, 10);
    return (Number.isFinite(n) && n > 0) ? (cleanName + '@u' + n) : cleanName;
  }

  /* ─── Country flag emoji (DISPLAY ONLY) ───
     Flags are prepended to picker names for readability. They are NEVER written into the
     stored tag values / manifests that the native side parses (carrier iso, locale tag,
     IANA zone, coords) — pure UI sugar, so nothing native sees an emoji. flagEmoji() maps
     an ISO 3166-1 alpha-2 code to its regional-indicator pair (US → 🇺🇸). */
  function flagEmoji(cc) {
    const c = String(cc || '').trim().toUpperCase();
    if (!/^[A-Z]{2}$/.test(c)) return '';
    return String.fromCodePoint(0x1F1E6 + c.charCodeAt(0) - 65, 0x1F1E6 + c.charCodeAt(1) - 65);
  }
  // BCP-47 tag → region flag: the last 2-letter subtag is the region (en-US→US, es-MX→MX,
  // zh-Hans-CN→CN). '' when the tag carries no region (e.g. bare "en").
  function flagForLocale(tag) {
    const parts = String(tag || '').split('-');
    for (let i = parts.length - 1; i >= 1; i--) {
      if (/^[A-Za-z]{2}$/.test(parts[i])) return flagEmoji(parts[i]);
    }
    return '';
  }
  // IANA zone → flag, via a map of the built-in zones (custom zones → '', no flag). UTC = none.
  const TZ_CC = {
    'UTC': '', 'America/New_York': 'US', 'America/Chicago': 'US', 'America/Denver': 'US',
    'America/Los_Angeles': 'US', 'America/Sao_Paulo': 'BR', 'Europe/London': 'GB',
    'Europe/Lisbon': 'PT', 'Europe/Paris': 'FR', 'Europe/Berlin': 'DE', 'Europe/Moscow': 'RU',
    'Europe/Istanbul': 'TR',
    'Africa/Cairo': 'EG', 'Africa/Lagos': 'NG', 'Asia/Dubai': 'AE', 'Asia/Tehran': 'IR',
    'Asia/Karachi': 'PK', 'Asia/Kolkata': 'IN', 'Asia/Dhaka': 'BD', 'Asia/Bangkok': 'TH',
    'Asia/Shanghai': 'CN', 'Asia/Singapore': 'SG', 'Asia/Tokyo': 'JP', 'Asia/Seoul': 'KR',
    'Australia/Sydney': 'AU', 'Pacific/Auckland': 'NZ',
  };
  function flagForTz(zone) { return flagEmoji(TZ_CC[zone] || ''); }
  // Country full name → flag (the cities.json `country` field is a name, not an ISO code).
  const COUNTRY_CC = {
    'Afghanistan':'AF','Algeria':'DZ','Argentina':'AR','Armenia':'AM','Australia':'AU','Austria':'AT',
    'Azerbaijan':'AZ','Bahrain':'BH','Bangladesh':'BD','Belgium':'BE','Bolivia':'BO','Brazil':'BR',
    'Cambodia':'KH','Canada':'CA','Chile':'CL','China':'CN','Colombia':'CO','Cuba':'CU','Czechia':'CZ',
    'Denmark':'DK','Ecuador':'EC','Egypt':'EG','Ethiopia':'ET','Finland':'FI','France':'FR','Georgia':'GE',
    'Germany':'DE','Ghana':'GH','Greece':'GR','Guatemala':'GT','Hungary':'HU','Iceland':'IS','India':'IN',
    'Indonesia':'ID','Iran':'IR','Iraq':'IQ','Ireland':'IE','Israel':'IL','Italy':'IT','Japan':'JP',
    'Jordan':'JO','Kazakhstan':'KZ','Kenya':'KE','Kuwait':'KW','Lebanon':'LB','Malaysia':'MY','Mexico':'MX',
    'Mongolia':'MN','Morocco':'MA','Nepal':'NP','Netherlands':'NL','New Zealand':'NZ','Nigeria':'NG',
    'Norway':'NO','Oman':'OM','Pakistan':'PK','Panama':'PA','Peru':'PE','Philippines':'PH','Poland':'PL',
    'Portugal':'PT','Qatar':'QA','Romania':'RO','Russia':'RU','Saudi Arabia':'SA','Senegal':'SN',
    'Singapore':'SG','South Africa':'ZA','South Korea':'KR','Spain':'ES','Sri Lanka':'LK','Sweden':'SE',
    'Switzerland':'CH','Taiwan':'TW','Thailand':'TH','Tunisia':'TN','Turkey':'TR','Ukraine':'UA',
    'United Arab Emirates':'AE','United Kingdom':'GB','United States':'US','Uruguay':'UY','Uzbekistan':'UZ',
    'Venezuela':'VE','Vietnam':'VN',
  };
  function flagForCountryName(name) { return flagEmoji(COUNTRY_CC[String(name || '').trim()] || ''); }

  /* ─── CPU model value-tag (cpu=<key>) ───
     For a with_cpu app, this picks WHICH cpuinfo profile the native companion mounts
     (CPU/cpuinfo_<key>). It's a colon value-tag carrying a value after '=' (unlike the
     boolean tags); empty/absent → the legacy default file (Snapdragon 8 Elite). */
  const cpuModelOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('cpu=') === 0);
    return t ? t.slice(4) : '';
  };
  function setCpuModel(pkg, key) {
    const t = tagsOf(pkg).filter(x => x.indexOf('cpu=') !== 0);   // drop any existing cpu=*
    if (key) t.push('cpu=' + key);
    return join(clean(pkg), t);
  }

  /* ─── GPU model value-tag (gpu=<key>) — REFERENCE branch only ───
     Opt-in, DEFAULT OFF: the gpu=<key> tag arms the RESIDENT GL/Vulkan inline hooks
     (detectable, never implicit — unlike cpu=, nothing else turns it on) and picks
     WHICH profile from GPU/manifest.json the native side spoofs to. No tag = no hook
     = zero residency. Same colon value-tag shape as cpu=. */
  const gpuModelOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('gpu=') === 0);
    return t ? t.slice(4) : '';
  };
  function setGpuModel(pkg, key) {
    const t = tagsOf(pkg).filter(x => x.indexOf('gpu=') !== 0);   // drop any existing gpu=*
    if (key) t.push('gpu=' + key);
    return join(clean(pkg), t);
  }

  /* ─── Timezone value-tag (tz=<IANA zone>) ───
     Per-app timezone. FREE + stealth: native does Java TimeZone.setDefault + user.timezone
     + ICU default (zero residency, survives DLCLOSE) AND a COW forge of persist.sys.timezone
     for native localtime/tzset. Value is an IANA id ("America/New_York") — '/' not ':' so it
     survives the colon tag split. Same colon value-tag shape as cpu=/gpu=; '' = off (real zone). */
  const timezoneOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('tz=') === 0);
    return t ? t.slice(3) : '';
  };
  function setTimezone(pkg, zone) {
    const t = tagsOf(pkg).filter(x => x.indexOf('tz=') !== 0);   // drop any existing tz=*
    if (zone) t.push('tz=' + zone);
    return join(clean(pkg), t);
  }

  /* ─── DPI value-tag (dpi=<n>) — controller tweak ───
     Per-app screen density. Consumed ONLY by unified_controller (like dab/dnd/kso, NOT
     Zygisk): when the app becomes active the controller backs up the real density and runs
     `wm density <n>`, HOLDS it while the app is alive (foreground or background) and restores
     it (prior override / `wm density reset`) only when the app is closed/killed — no re-apply
     on app switches. FREE, zero residency (no in-app hook). Plain integer 120..640; '' = off. */
  const dpiOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('dpi=') === 0);
    return t ? t.slice(4) : '';
  };
  function setDpi(pkg, n) {
    const t = tagsOf(pkg).filter(x => x.indexOf('dpi=') !== 0);   // drop any existing dpi=*
    const v = parseInt(n, 10);
    if (v >= 120 && v <= 640) t.push('dpi=' + v);
    return join(clean(pkg), t);
  }

  /* ─── Locale value-tag (lang=<BCP-47>) ───
     Per-app language/region. FREE + stealth, applied SYSTEM-SIDE: the system_server hook
     (PackageConfigPersister) injects the locale into the app's Configuration at process start, so
     the app reads/renders the fake locale with zero residency in its own process. Value is a BCP-47
     tag ("en-US", "pt-BR", "zh-Hans-CN") — '-' not ':' so it survives the colon tag split. Same
     colon value-tag shape as tz=; '' = off (real locale). */
  const localeOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('lang=') === 0);
    return t ? t.slice(5) : '';
  };
  function setLocale(pkg, loc) {
    const t = tagsOf(pkg).filter(x => x.indexOf('lang=') !== 0);  // drop any existing lang=*
    if (loc) t.push('lang=' + loc);
    return join(clean(pkg), t);
  }

  /* ─── Wallpaper-id tag (wallpaper / wallpaper=<id>) ───
     Per-app WallpaperManager.getWallpaperId spoof. FREE + stealth, applied SYSTEM-SIDE: the
     system_server hook (WallpaperManagerService.getWallpaperIdForUser) forges the returned int for the
     tagged app's binder calls, with zero residency in the app. The id is a device-persistent monotonic
     counter (survives data-clear/reinstall) → anti-fraud SDKs (Forter/DoorDash) use it as a device
     LINKAGE signal. Bare 'wallpaper' → default -1 ("no wallpaper", mainly for the verify test);
     'wallpaper=<id>' pins a plausible per-persona integer (breaks linkage WITHOUT the -1 bot-farm tell).
     hasWallpaper = tagged at all; wallpaperOf = the id ('' = bare → -1). */
  const hasWallpaper = pkg => tagsOf(pkg).some(x => x === 'wallpaper' || x.indexOf('wallpaper=') === 0);
  const wallpaperOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('wallpaper=') === 0);
    return t ? t.slice(10) : '';
  };
  function setWallpaper(pkg, on, id) {
    const t = tagsOf(pkg).filter(x => x !== 'wallpaper' && x.indexOf('wallpaper=') !== 0);
    if (on) { const v = (id == null ? '' : String(id).trim()); t.push(v ? 'wallpaper=' + v : 'wallpaper'); }
    return join(clean(pkg), t);
  }

  /* ─── Account-list tag (account / account=<email>) ───
     Per-app Google-account spoof. FREE + stealth, applied SYSTEM-SIDE: the system_server hook
     (AccountManagerService.getAccountsAsUser) overrides the returned account list for the tagged app's
     binder calls, zero residency in the app. Anti-fraud SDKs (Forter/DoorDash) enumerate the account
     list + per-account id as a device-LINKAGE signal (survives data-clear/reinstall). Bare 'account'
     HIDES (empty list); 'account=<email>' returns one forged com.google account with that email (a
     distinct per-persona identity). hasAccount = tagged at all; accountEmailOf = the email ('' = hide).
     NOTE: the gms-process read path (apps that read accounts via Google Play Services) is covered by a
     separate hook that reads the SAME tag. */
  const hasAccount = pkg => tagsOf(pkg).some(x => x === 'account' || x.indexOf('account=') === 0);
  const accountEmailOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('account=') === 0);
    return t ? t.slice(8) : '';
  };
  function setAccount(pkg, on, email) {
    const t = tagsOf(pkg).filter(x => x !== 'account' && x.indexOf('account=') !== 0);
    if (on) { const v = (email == null ? '' : String(email).trim()); t.push(v ? 'account=' + v : 'account'); }
    return join(clean(pkg), t);
  }

  /* ─── WebView User-Agent value-tag (ua=<key>) — PRO, RESIDENT ───
     Like gpu=<key>: the tag holds a short profile KEY, and the actual UA string lives in
     UA/manifest.json (parallel to GPU/manifest.json), so COPG.json stays per-app-clean and the UA is
     reusable across apps. uaKeyOf returns the key ('' = off); the native resolves key → UA. */
  const uaKeyOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('ua=') === 0);
    return t ? t.slice(3) : '';
  };
  function setUa(pkg, key) {
    const t = tagsOf(pkg).filter(x => x.indexOf('ua=') !== 0);   // drop any existing ua=*
    if (key) t.push('ua=' + key);
    return join(clean(pkg), t);
  }

  /* ─── Uptime value-tag (uptime=<sec>) — PRO, RESIDENT ───
     Extra uptime ADDED to the real device uptime: the app reads real_uptime + <sec>. Because the
     real boot clock is monotonic and survives an app close, the fake uptime never resets to the base
     on relaunch — it keeps growing and only drops on a real reboot. Resident inline hooks
     (clock_gettime/sysinfo) → detectable → PRO + risk-gated, never anti-cheat. uptimeOf returns the
     SECONDS (0 = off). (parseInt tolerates a legacy '@…' anchor suffix from older configs.) */
  const uptimeOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('uptime=') === 0);
    if (!t) return 0;
    const v = parseInt(t.slice(7), 10);   // parseInt stops at any legacy '@' → the seconds part
    return isNaN(v) || v < 0 ? 0 : v;
  };
  function setUptime(pkg, seconds) {
    const t = tagsOf(pkg).filter(x => x.indexOf('uptime=') !== 0);   // drop any existing uptime=*
    if (seconds > 0) t.push('uptime=' + seconds);
    return join(clean(pkg), t);
  }

  /* ─── boot_id value-tag (bootid / bootid=rnd / bootid=<uuid>) — PRO ───
     Per-app /proc/sys/kernel/random/boot_id (a per-boot UUID anti-fraud SDKs read as a boot-session
     fingerprint). Three states: bare 'bootid' = a STABLE per-package-derived UUID (same across
     launches AND real reboots); 'bootid=rnd' = CHURN (a fresh UUID each cold launch = a fake reboot);
     'bootid=<uuid>' = a pinned literal. The backend is chosen natively: NoMount VFS redirect where the
     driver is present (stealth, no residency), else an inline open/openat hook (resident → detectable
     → risk-gated). bootidOf returns {on, churn, literal} ('' literal = derived/churn). */
  const bootidOf = pkg => {
    const val = tagsOf(pkg).find(x => x.indexOf('bootid=') === 0);
    if (val) {
      const v = val.slice(7);
      return v === 'rnd' ? { on: true, churn: true, literal: '' }
                         : { on: true, churn: false, literal: v };
    }
    return hasTag(pkg, 'bootid') ? { on: true, churn: false, literal: '' }
                                 : { on: false, churn: false, literal: '' };
  };
  function setBootid(pkg, on, churn, literal) {
    const t = tagsOf(pkg).filter(x => x !== 'bootid' && x.indexOf('bootid=') !== 0);
    if (on) {
      const lit = String(literal || '').trim().toLowerCase();
      if (/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(lit)) t.push('bootid=' + lit);
      else if (churn)                                                                 t.push('bootid=rnd');
      else                                                                            t.push('bootid');
    }
    return join(clean(pkg), t);
  }

  /* ─── WiFi MAC value-tag (mac / mac=rnd / mac=<12hex>) — PRO, RESIDENT ───
     Per-app WiFi MAC (/sys/class/net/wlanN/address, the path anti-fraud SDKs + Java
     NetworkInterface.getHardwareAddress read). Three states: bare 'mac' = STABLE per-package
     derive; 'mac=rnd' = CHURN (fresh each cold launch); 'mac=<12hex>' = pinned literal. The literal
     is 12 hex WITHOUT separators — a colon MAC would be shredded by the colon tag split. Native
     inline open/openat redirect (resident → detectable → risk-gated). macOf returns {on, churn,
     literal} where literal is the 12-hex form ('' = derived/churn). */
  const macOf = pkg => {
    const val = tagsOf(pkg).find(x => x.indexOf('mac=') === 0);
    if (val) {
      const v = val.slice(4);
      return v === 'rnd' ? { on: true, churn: true, literal: '' }
                         : { on: true, churn: false, literal: v };
    }
    return hasTag(pkg, 'mac') ? { on: true, churn: false, literal: '' }
                              : { on: false, churn: false, literal: '' };
  };
  function setMac(pkg, on, churn, literal) {
    const t = tagsOf(pkg).filter(x => x !== 'mac' && x.indexOf('mac=') !== 0);
    if (on) {
      const hex = String(literal || '').replace(/[^0-9a-fA-F]/g, '').toLowerCase();   // strip ':' / '-'
      if (/^[0-9a-f]{12}$/.test(hex)) t.push('mac=' + hex);
      else if (churn)                 t.push('mac=rnd');
      else                            t.push('mac');
    }
    return join(clean(pkg), t);
  }

  /* ─── Bluetooth MAC value-tag (btmac / btmac=rnd / btmac=<12hex>) — PRO, STEALTH ───
     Per-app Bluetooth MAC (Settings.Secure.getString("bluetooth_address"), THE app-visible read;
     BluetoothAdapter.getAddress() is masked 02:… since A6). Same three states as the WiFi mac tag.
     Cache-forge in postAppSpecialize then DLCLOSE → zero residency (not risk-gated). The 'mac'/'mac='
     filters use indexOf===0 so 'btmac'/'btmac=' never cross-match them, and vice-versa. */
  const btmacOf = pkg => {
    const val = tagsOf(pkg).find(x => x.indexOf('btmac=') === 0);
    if (val) {
      const v = val.slice(6);
      return v === 'rnd' ? { on: true, churn: true, literal: '' }
                         : { on: true, churn: false, literal: v };
    }
    return hasTag(pkg, 'btmac') ? { on: true, churn: false, literal: '' }
                                : { on: false, churn: false, literal: '' };
  };
  function setBtmac(pkg, on, churn, literal) {
    const t = tagsOf(pkg).filter(x => x !== 'btmac' && x.indexOf('btmac=') !== 0);
    if (on) {
      const hex = String(literal || '').replace(/[^0-9a-fA-F]/g, '').toLowerCase();   // strip ':' / '-'
      if (/^[0-9a-f]{12}$/.test(hex)) t.push('btmac=' + hex);
      else if (churn)                 t.push('btmac=rnd');
      else                            t.push('btmac');
    }
    return join(clean(pkg), t);
  }

  /* ─── Refresh-rate value-tag (rr=<hz>) — FREE, RESIDENT ───
     Spoofed display refresh rate. Resident v7878 DisplayManagerGlobal.getDisplayInfo hook
     (there is NO stealth path — the value is a fresh binder copy each read) → detectable →
     risk-gated. refreshOf returns the Hz (0 = off). */
  const refreshOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('rr=') === 0);
    if (!t) return 0;
    const v = parseFloat(t.slice(3));
    return isNaN(v) || v <= 0 ? 0 : v;
  };
  function setRefreshRate(pkg, hz) {
    const t = tagsOf(pkg).filter(x => x.indexOf('rr=') !== 0);   // drop any existing rr=*
    if (hz > 0) t.push('rr=' + hz);
    return join(clean(pkg), t);
  }

  /* ─── GPS coordinate value-tag (gps=<lat>,<lng>) — PRO, RESIDENT ───
     Per-app fake GPS position. The native v7878 GpsHook overwrites every delivered
     android.location.Location's coordinates with <lat>,<lng> (decimal degrees) — there is NO stealth
     path (location is binder-backed), so it's resident → detectable → risk-gated, never anti-cheat.
     Value is "<lat>,<lng>" — a comma, not a colon, so it survives the colon tag split (same trick as
     tz='s '/'). gpsOf returns {lat,lng} strings or null (off). Coords only enter via the place picker,
     which resolves a country/city name to lat/lng; the tag stores the coords, not the name. */
  const gpsOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('gps=') === 0);
    if (!t) return null;
    const parts = t.slice(4).split(',');
    if (parts.length < 2) return null;
    const lat = parts[0].trim(), lng = parts[1].trim();
    if (lat === '' || lng === '' || isNaN(+lat) || isNaN(+lng)) return null;
    return { lat, lng };
  };
  function setGps(pkg, lat, lng) {
    const t = tagsOf(pkg).filter(x => x.indexOf('gps=') !== 0);   // drop any existing gps=*
    const la = String(lat).trim(), lo = String(lng).trim();
    if (la !== '' && lo !== '' && !isNaN(+la) && !isNaN(+lo) &&
        +la >= -90 && +la <= 90 && +lo >= -180 && +lo <= 180) {
      t.push('gps=' + la + ',' + lo);
    }
    return join(clean(pkg), t);
  }

  /* ─── Cell-tower value-tag (cell=<lac>,<ci>) — PRO, RESIDENT ───
     Per-app fake serving cell. The native v7878 CellHook rewrites every reported CellInfo identity's
     area code (LAC/TAC) + cell id (CID/CI/NCI) to <lac>,<ci> (plain decimals) — resident (cell info is
     binder-backed, no stealth path) → risk-gated, never anti-cheat. INDEPENDENT of gps= (China risk-
     control cross-checks GPS↔cell; this is the cell half, GPS untouched). Comma survives the colon
     split. Format: cell=<lac>,<ci>[,<mcc>,<mnc>] — MCC (3 digits) / MNC (2-3 digits) optional (a
     reader of getAllCellInfo() / CellIdentity sees the fake MCC/MNC too). cellOf returns
     {lac,ci,mcc,mnc} (mcc/mnc '' when absent) or null (off). */
  const cellOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('cell=') === 0);
    if (!t) return null;
    const parts = t.slice(5).split(',');
    if (parts.length < 2) return null;
    const lac = parts[0].trim(), ci = parts[1].trim();
    if (!/^\d+$/.test(lac) || !/^\d+$/.test(ci)) return null;
    let mcc = (parts[2] || '').trim(), mnc = (parts[3] || '').trim();
    if (!/^\d{3}$/.test(mcc)) mcc = '';
    if (!/^\d{2,3}$/.test(mnc)) mnc = '';
    return { lac, ci, mcc, mnc };
  };
  function setCell(pkg, lac, ci, mcc, mnc) {
    const t = tagsOf(pkg).filter(x => x.indexOf('cell=') !== 0);   // drop any existing cell=*
    const la = String(lac).trim(), c = String(ci).trim();
    const mc = String(mcc || '').trim(), mn = String(mnc || '').trim();
    if (/^\d+$/.test(la) && /^\d+$/.test(c)) {
      let v = la + ',' + c;
      // append MCC/MNC only when valid; MNC needs MCC (so the tail is well-formed)
      if (/^\d{3}$/.test(mc)) { v += ',' + mc; if (/^\d{2,3}$/.test(mn)) v += ',' + mn; }
      t.push('cell=' + v);
    }
    return join(clean(pkg), t);
  }

  /* Bundled place database for the GPS picker (webroot/cities.json — [{name,country,lat,lng}]).
     A WebUI-only display convenience: the picker resolves a place NAME → lat/lng, but the tag stores
     coords only, so this file never ships to the native side. Same-origin fetch works in both browser
     preview and the on-device WebUI (it sits beside index.html). Cached after the first load; a fetch
     failure yields [] so the picker still offers Real (off) + the custom lat/lng entry. */
  let placesCache = null;
  async function getPlaces() {
    if (placesCache) return placesCache;
    try {
      const res = await fetch('cities.json', { cache: 'force-cache' });
      const arr = await res.json();
      placesCache = Array.isArray(arr) ? arr : [];
    } catch (e) { console.warn('[gps] cities.json load failed:', e); placesCache = []; }
    return placesCache;
  }
  // Nearest bundled place to a lat/lng, for showing a name on the modal chip when coords are set
  // (equirectangular distance is fine at city scale). Returns null if the DB is empty/not loaded.
  function nearestPlace(lat, lng) {
    if (!placesCache || !placesCache.length) return null;
    const la = +lat, lo = +lng;
    if (isNaN(la) || isNaN(lo)) return null;
    let best = null, bestD = Infinity;
    for (const p of placesCache) {
      const dLat = p.lat - la, dLng = (p.lng - lo) * Math.cos(la * Math.PI / 180);
      const d = dLat * dLat + dLng * dLng;
      if (d < bestD) { bestD = d; best = p; }
    }
    // ~0.15° (~15 km) tolerance → close enough to call it that place, else null (show raw coords).
    return bestD <= 0.0225 ? best : null;
  }

  /* ─── ANDROID_ID literal value-tag (aid=<hex>) ───
     Bare 'aid' derives a per-app id from the package. 'aid=<hex>' pins an EXACT 16-hex
     ANDROID_ID verbatim — so a second phone can be made to match the first, or an app's id
     re-rolled by changing the literal. */
  const aidLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('aid=') === 0);
    return t ? t.slice(4) : '';
  };

  /* ─── Per-app SERIAL value-tag (serial=<value>) ───
     Bare 'serial' derives a per-app Build.SERIAL from the package (overriding the device
     profile's SERIAL). 'serial=<value>' pins an exact serial verbatim. 'serial' (exact) never
     matches the 'serial=' prefix. Same colon value-tag shape as aid=. */
  const serialLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('serial=') === 0);
    return t ? t.slice(7) : '';
  };

  /* ─── Canvas-fp seed value-tag (webfp=<seed>) ───
     Bare 'webfp' derives a per-app-stable canvas seed from the package. 'webfp=<seed>' pins an
     EXACT seed (hex, e.g. 8f3a1c) so the user can re-roll to a new fingerprint (Randomize) or set
     a specific one. 'webfp' (exact) never matches the 'webfp=' prefix. Same value-tag shape as aid=. */
  const webfpSeedOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('webfp=') === 0);
    return t ? t.slice(6) : '';
  };

  /* ─── GAID literal value-tag (gaid=<uuid>) ───
     Bare 'gaid' derives a per-app UUID from the package. 'gaid=<uuid>' pins an EXACT
     advertising id verbatim. 'gaid' (exact) never matches the 'gaid=' prefix. Same colon
     value-tag shape as aid=. */
  const gaidLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('gaid=') === 0);
    return t ? t.slice(5) : '';
  };

  /* ─── App Set ID literal value-tag (appset=<uuid>) ───
     Bare 'appset' derives a per-app UUID from the package; 'appset=<uuid>' pins one. Same
     shape as gaid=. 'appset' (exact) never matches the 'appset=' prefix. */
  const appsetLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('appset=') === 0);
    return t ? t.slice(7) : '';
  };

  // DRM (Widevine) value-tags: drm=<level> pins L1/L2/L3 (bare :drm = L1);
  // drmid=<hex> pins deviceUniqueId; drmsys=<int> pins systemId. NB drm= must NOT
  // match drmid=/drmsys= — exclude those prefixes.
  const drmLevelOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('drm=') === 0);
    return t ? t.slice(4) : '';
  };
  const drmIdLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('drmid=') === 0);
    return t ? t.slice(6) : '';
  };
  const drmSysLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('drmsys=') === 0);
    return t ? t.slice(7) : '';
  };
  // IMEI value-tags: imei=<15> pins slot 0, imei2=<15> pins slot 1 (dual-SIM); bare :imei
  // derives both per-app. NB imei= must NOT match imei2= — exclude that prefix.
  const imeiLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('imei=') === 0);
    return t ? t.slice(5) : '';
  };
  const imei2LiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('imei2=') === 0);
    return t ? t.slice(6) : '';
  };
  // IMSI / ICCID value-tags: imsi=/iccid= pin slot 0, imsi2=/iccid2= pin slot 1 (dual-SIM); bare
  // :imsi / :iccid derive per-app+slot. Same resident v7878 TelephonyManager hook as IMEI.
  // NB imsi= must NOT match imsi2= — exclude that prefix (same trap as imei=/imei2=).
  const imsiLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('imsi=') === 0);
    return t ? t.slice(5) : '';
  };
  const imsi2LiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('imsi2=') === 0);
    return t ? t.slice(6) : '';
  };
  const iccidLiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('iccid=') === 0);
    return t ? t.slice(6) : '';
  };
  const iccid2LiteralOf = pkg => {
    const t = tagsOf(pkg).find(x => x.indexOf('iccid2=') === 0);
    return t ? t.slice(7) : '';
  };

  /* ─── SIM / carrier value-tags (sim=<key> = SAFE, simx=<key> = AGGRESSIVE) ───
     Two PRO modes. sim= forges the gsm.* carrier props via COW (stealth, both modes
     do this natively); simx= ALSO injects the resident ISub-proxy dex (detectable,
     never anti-cheat). One tag per package — mutually exclusive (a Safe|Aggressive
     segment in the modal). carrierOf returns {mode:'safe'|'aggressive'|'', key}. */
  // A package may carry a DIFFERENT carrier per SIM slot: sim=A@0 (SIM1) + sim=B@1 (SIM2).
  // A bare sim=<key> (no @) = both slots same. Returns { mode, s0, s1 } where s0/s1 are
  // carrier keys ('' = that slot left real). mode = 'aggressive' if any simx tag present.
  // PHANTOM mode (simfake=<key>) = ADD a SIM to an empty slot (vs sim=/simx= which RENAME a real
  // SIM). One carrier key, no @slot. Resident/detectable like aggressive. Wins over sim=/simx=.
  const carrierOf = pkg => {
    const tags = tagsOf(pkg);
    let anyAgg = false, anySim = false, phantom = false, s0 = '', s1 = '', pk = '';
    const parse = v => { const at = v.lastIndexOf('@'); if (at >= 0) { const s = parseInt(v.slice(at + 1), 10); return { key: v.slice(0, at), slot: (s === 0 || s === 1) ? s : -1 }; } return { key: v, slot: -1 }; };
    tags.forEach(t => {
      if (t.indexOf('simfake=') === 0) { phantom = true; pk = t.slice(8); return; }
      let agg = false, val = null;
      if (t.indexOf('simx=') === 0) { agg = true; val = t.slice(5); }
      else if (t.indexOf('sim=') === 0) { val = t.slice(4); }
      else return;
      anySim = true; if (agg) anyAgg = true;
      const p = parse(val);
      if (p.slot === -1) { s0 = p.key; s1 = p.key; }   // bare = both
      else if (p.slot === 0) s0 = p.key;
      else if (p.slot === 1) s1 = p.key;
    });
    if (phantom) return { mode: 'phantom', s0: pk, s1: '' };   // phantom overrides rename tags
    return { mode: anyAgg ? 'aggressive' : (anySim ? 'safe' : ''), s0, s1 };
  };
  function setCarrierModel(pkg, mode, s0, s1) {
    const t = tagsOf(pkg).filter(x => x.indexOf('sim=') !== 0 && x.indexOf('simx=') !== 0 && x.indexOf('simfake=') !== 0);
    if (mode === 'phantom') {
      if (s0) t.push('simfake=' + s0);                 // add-a-SIM (single carrier key, no slot)
    } else if (mode) {
      const pfx = mode === 'aggressive' ? 'simx=' : 'sim=';
      if (s0 && s1 && s0 === s1) t.push(pfx + s0);      // both same → bare shorthand
      else {
        if (s0) t.push(pfx + s0 + '@0');
        if (s1) t.push(pfx + s1 + '@1');
      }
    }
    return join(clean(pkg), t);
  }

  /* ─── Per-app ANDROID_ID derive (BYTE-FOR-BYTE parity with deriveAndroidId() in
     spoof_module.cpp) ───
     NO seed: the native side hashes the app's package alone (FNV-1a 64 → splitmix64 →
     16 hex) so each app gets a distinct, stable id. Mirrored here ONLY to PREVIEW the
     value the app will read in the package modal's ⓘ — MUST match the C++ exactly
     (verified). Package names are ASCII, so charCodeAt & 0xff == the C++ byte. */
  function deriveAndroidId(pkg) {
    const MASK = (1n << 64n) - 1n;
    let h = 14695981039346656037n;                 // FNV-1a 64 offset basis
    const s = clean(String(pkg || ''));
    if (!s) return '';
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    h = (h + 0x9E3779B97F4A7C15n) & MASK;           // splitmix64 finalizer
    h = ((h ^ (h >> 30n)) * 0xBF58476D1CE4E5B9n) & MASK;
    h = ((h ^ (h >> 27n)) * 0x94D049BB133111EBn) & MASK;
    h = (h ^ (h >> 31n)) & MASK;
    return h.toString(16).padStart(16, '0');
  }

  /* ─── Per-app GAID derive (BYTE-FOR-BYTE parity with deriveGaid() in spoof_module.cpp) ───
     NO seed: native hashes package+":gaid" (FNV-1a 64 → two splitmix64 pulls → 128-bit
     RFC-4122 v4 UUID) so each app gets a distinct, stable advertising id (differs from the
     same package's ANDROID_ID via the ":gaid" salt). Preview only; MUST match the C++. */
  function deriveGaid(pkg) {
    const MASK = (1n << 64n) - 1n;
    let h = 14695981039346656037n;                 // FNV-1a 64 offset basis
    const base = clean(String(pkg || ''));
    if (!base) return '';
    const s = base + ':gaid';
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    const sm = () => {                             // splitmix64 pull, mutates h like the C++ ref
      h = (h + 0x9E3779B97F4A7C15n) & MASK;
      let z = h;
      z = ((z ^ (z >> 30n)) * 0xBF58476D1CE4E5B9n) & MASK;
      z = ((z ^ (z >> 27n)) * 0x94D049BB133111EBn) & MASK;
      return (z ^ (z >> 31n)) & MASK;
    };
    const a = sm(), b = sm();
    const by = new Array(16);
    for (let i = 0; i < 8; i++) {
      by[i]     = Number((a >> BigInt(8 * (7 - i))) & 0xffn);
      by[8 + i] = Number((b >> BigInt(8 * (7 - i))) & 0xffn);
    }
    by[6] = (by[6] & 0x0f) | 0x40;                 // RFC-4122 version 4
    by[8] = (by[8] & 0x3f) | 0x80;                 // variant 10xx
    const x = by.map(v => v.toString(16).padStart(2, '0'));
    return `${x[0]}${x[1]}${x[2]}${x[3]}-${x[4]}${x[5]}-${x[6]}${x[7]}-${x[8]}${x[9]}-${x[10]}${x[11]}${x[12]}${x[13]}${x[14]}${x[15]}`;
  }

  /* ─── Per-app boot_id derive (BYTE-FOR-BYTE parity with deriveBootId() in spoof_module.cpp) ───
     FNV-1a 64 of id_package → XOR the ":bootid" salt constant → two splitmix64 mixes (hi, lo) →
     RFC-4122 v4 UUID via the SAME bit layout as formatUuid() (NOT deriveGaid's byte-array form).
     Preview only — the ⓘ shows the exact UUID a STABLE bootid will read; churn (bootid=rnd) and a
     pinned literal aren't derived. MUST match the C++ exactly. */
  function deriveBootId(idPackage) {
    const MASK = (1n << 64n) - 1n;
    const s = clean(String(idPackage || ''));
    if (!s) return '';
    let h = 14695981039346656037n;                     // FNV-1a 64 offset basis
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    h = (h ^ 0x626F6F7469645F00n) & MASK;              // ":bootid" salt (matches the C++ constant)
    const mix = x => {                                 // splitmix64 body — NO +GOLDEN here (hi path)
      x = ((x ^ (x >> 30n)) * 0xBF58476D1CE4E5B9n) & MASK;
      x = ((x ^ (x >> 27n)) * 0x94D049BB133111EBn) & MASK;
      return (x ^ (x >> 31n)) & MASK;
    };
    let hi = mix(h);
    let lo = mix((h + 0x9E3779B97F4A7C15n) & MASK);    // lo path adds GOLDEN first
    hi = (hi & 0xFFFFFFFFFFFF0FFFn) | 0x0000000000004000n;   // version 4
    lo = (lo & 0x3FFFFFFFFFFFFFFFn) | 0x8000000000000000n;   // variant 10xx
    const hx = (v, n) => v.toString(16).padStart(n, '0');
    return hx((hi >> 32n) & 0xFFFFFFFFn, 8) + '-' + hx((hi >> 16n) & 0xFFFFn, 4) + '-' +
           hx(hi & 0xFFFFn, 4) + '-' + hx((lo >> 48n) & 0xFFFFn, 4) + '-' + hx(lo & 0xFFFFFFFFFFFFn, 12);
  }

  /* ─── Per-app WiFi MAC derive (BYTE-FOR-BYTE parity with deriveMac() in spoof_module.cpp) ───
     FNV-1a 64 of id_package → XOR the ":mac" salt → one splitmix64 mix → little-endian bytes 0..5,
     octet0 locally-administered (|0x02) + unicast (&0xFE) → "xx:xx:xx:xx:xx:xx". Preview only — the
     ⓘ shows the exact MAC a STABLE mac will read; churn + pinned literal aren't derived. */
  function deriveMac(idPackage) {
    const MASK = (1n << 64n) - 1n;
    const s = clean(String(idPackage || ''));
    if (!s) return '';
    let h = 14695981039346656037n;
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    h = (h ^ 0x3A6D61635F000000n) & MASK;                 // ":mac" salt (matches the C++ constant)
    h = ((h ^ (h >> 30n)) * 0xBF58476D1CE4E5B9n) & MASK;
    h = ((h ^ (h >> 27n)) * 0x94D049BB133111EBn) & MASK;
    h = (h ^ (h >> 31n)) & MASK;
    const b = [];
    for (let i = 0; i < 6; i++) b.push(Number((h >> BigInt(8 * i)) & 0xffn));
    b[0] = (b[0] & 0xFE) | 0x02;
    return b.map(x => x.toString(16).padStart(2, '0')).join(':');
  }

  /* ─── Per-app Bluetooth MAC derive (BYTE-FOR-BYTE parity with deriveBtMac() in spoof_module.cpp) ───
     Same splitmix64 shape as deriveMac but ":bt" salt (so BT ≠ WiFi) and UPPERCASE output with
     octet0 unicast (&0xFE, no LA bit) → "FF:DD:1D:22:60:44". Preview only (stable btmac). */
  function deriveBtmac(idPackage) {
    const MASK = (1n << 64n) - 1n;
    const s = clean(String(idPackage || ''));
    if (!s) return '';
    let h = 14695981039346656037n;
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    h = (h ^ 0x3A62745F00000000n) & MASK;                 // ":bt" salt (matches the C++ constant)
    h = ((h ^ (h >> 30n)) * 0xBF58476D1CE4E5B9n) & MASK;
    h = ((h ^ (h >> 27n)) * 0x94D049BB133111EBn) & MASK;
    h = (h ^ (h >> 31n)) & MASK;
    const b = [];
    for (let i = 0; i < 6; i++) b.push(Number((h >> BigInt(8 * i)) & 0xffn));
    b[0] = b[0] & 0xFE;                                    // unicast (no LA bit)
    return b.map(x => x.toString(16).padStart(2, '0').toUpperCase()).join(':');
  }

  /* ─── Per-app App Set ID derive (BYTE-FOR-BYTE parity with deriveAppSetId() in
     spoof_module.cpp) ─── identical algorithm to deriveGaid but salted ":appset" so an app's
     App Set ID differs from its GAID. Preview only; MUST match the C++. */
  function deriveAppSetId(pkg) {
    const MASK = (1n << 64n) - 1n;
    let h = 14695981039346656037n;
    const base = clean(String(pkg || ''));
    if (!base) return '';
    const s = base + ':appset';
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    const sm = () => {
      h = (h + 0x9E3779B97F4A7C15n) & MASK;
      let z = h;
      z = ((z ^ (z >> 30n)) * 0xBF58476D1CE4E5B9n) & MASK;
      z = ((z ^ (z >> 27n)) * 0x94D049BB133111EBn) & MASK;
      return (z ^ (z >> 31n)) & MASK;
    };
    const a = sm(), b = sm();
    const by = new Array(16);
    for (let i = 0; i < 8; i++) {
      by[i]     = Number((a >> BigInt(8 * (7 - i))) & 0xffn);
      by[8 + i] = Number((b >> BigInt(8 * (7 - i))) & 0xffn);
    }
    by[6] = (by[6] & 0x0f) | 0x40;
    by[8] = (by[8] & 0x3f) | 0x80;
    const x = by.map(v => v.toString(16).padStart(2, '0'));
    return `${x[0]}${x[1]}${x[2]}${x[3]}-${x[4]}${x[5]}-${x[6]}${x[7]}-${x[8]}${x[9]}-${x[10]}${x[11]}${x[12]}${x[13]}${x[14]}${x[15]}`;
  }

  /* ─── Per-app Widevine deviceUniqueId derive (parity with deriveDrmDeviceId() in
     spoof_module.cpp) ─── package+":drmid" → FNV-1a 64 → 4 splitmix64 pulls → 32 bytes
     → 64 hex. Preview only; MUST match the C++. */
  function deriveDrmDeviceId(pkg) {
    const MASK = (1n << 64n) - 1n;
    let h = 14695981039346656037n;
    const base = clean(String(pkg || ''));
    if (!base) return '';
    const s = base + ':drmid';
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    const sm = () => {
      h = (h + 0x9E3779B97F4A7C15n) & MASK;
      let z = h;
      z = ((z ^ (z >> 30n)) * 0xBF58476D1CE4E5B9n) & MASK;
      z = ((z ^ (z >> 27n)) * 0x94D049BB133111EBn) & MASK;
      return (z ^ (z >> 31n)) & MASK;
    };
    let out = '';
    for (let p = 0; p < 4; p++) {
      const v = sm();
      for (let i = 0; i < 8; i++) out += Number((v >> BigInt(8 * (7 - i))) & 0xffn).toString(16).padStart(2, '0');
    }
    return out;
  }

  /* ─── Per-app Widevine systemId derive (parity with deriveDrmSystemId() in
     spoof_module.cpp) ─── package+":drmsys" → FNV-1a 64 → (h % 90000)+10000. */
  function deriveDrmSystemId(pkg) {
    const MASK = (1n << 64n) - 1n;
    let h = 14695981039346656037n;
    const base = clean(String(pkg || ''));
    if (!base) return '';
    const s = base + ':drmsys';
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    return String((h % 90000n) + 10000n);
  }

  // Luhn check digit for a numeric string (rightmost payload digit doubled).
  function luhnCheckDigit(payload) {
    let sum = 0, dbl = true;
    for (let i = payload.length - 1; i >= 0; i--) {
      let d = payload.charCodeAt(i) - 48;
      if (dbl) { d *= 2; if (d > 9) d -= 9; }
      sum += d; dbl = !dbl;
    }
    return (10 - (sum % 10)) % 10;
  }

  /* ─── Per-app IMEI derive (parity with deriveImei() in spoof_module.cpp) ───
     package+":imei" (slot 0) / +":imei1" (slot 1) → FNV-1a 64 → (h % 10^14) 14-digit
     payload + Luhn check = 15 digits, always Luhn-valid. Slot 1 = the dual-SIM IMEI2.
     Preview only; MUST match the C++. */
  function deriveImei(pkg, slot) {
    const MASK = (1n << 64n) - 1n;
    let h = 14695981039346656037n;
    const base = clean(String(pkg || ''));
    if (!base) return '';
    const s = base + (slot === 1 ? ':imei1' : ':imei');
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    const p14 = (h % 100000000000000n).toString().padStart(14, '0');
    return p14 + String(luhnCheckDigit(p14));
  }

  /* ─── Per-app + per-slot IMSI derive (parity with deriveImsi() in spoof_module.cpp) ───
     package+":imsi" (slot 0) / +":imsi1" (slot 1) → FNV-1a 64 → (h % 10^15) → 15 digits. Preview. */
  function deriveImsi(pkg, slot) {
    const MASK = (1n << 64n) - 1n;
    let h = 14695981039346656037n;
    const base = clean(String(pkg || ''));
    if (!base) return '';
    const s = base + (slot === 1 ? ':imsi1' : ':imsi');
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    return (h % 1000000000000000n).toString().padStart(15, '0');
  }

  /* ─── Per-app + per-slot ICCID derive (parity with deriveIccid() in spoof_module.cpp) ───
     package+":iccid" (slot 0) / +":iccid1" (slot 1) → FNV-1a 64 → "89" + (h % 10^16) 16-digit
     payload + Luhn check = 19 digits, always Luhn-valid. Preview only; MUST match the C++. */
  function deriveIccid(pkg, slot) {
    const MASK = (1n << 64n) - 1n;
    let h = 14695981039346656037n;
    const base = clean(String(pkg || ''));
    if (!base) return '';
    const s = base + (slot === 1 ? ':iccid1' : ':iccid');
    for (let i = 0; i < s.length; i++) { h ^= BigInt(s.charCodeAt(i) & 0xff); h = (h * 1099511628211n) & MASK; }
    const body = '89' + (h % 10000000000000000n).toString().padStart(16, '0');
    return body + String(luhnCheckDigit(body));
  }

  /* ─── CPU model library (CPU/cpuinfo_<key> + CPU/manifest.json) ───
     The picker's display names come from the shipped manifest; on device we cat the
     real file (so user-added custom profiles appear), and fall back to this built-in
     list (also the preview list, since the browser can't read the module dir). Keep
     BUILTIN in sync with module/CPU/manifest.json. */
  const CPU_DIR = MODULE_DIR + '/CPU';
  const BUILTIN_CPU_MODELS = [
    { key: 'sd8elite',   name: 'Qualcomm Snapdragon 8 Elite' },
    { key: '9000',       name: 'HiSilicon Kirin 9000' },
    { key: '9000s',      name: 'HiSilicon Kirin 9000S' },
    { key: '9020',       name: 'HiSilicon Kirin 9020' },
    { key: '9020a',      name: 'HiSilicon Kirin 9020A' },
    { key: '9030pro',    name: 'HiSilicon Kirin 9030 Pro' },
    { key: 'xuanjie_o1', name: 'Xiaomi Xring O1' },
    { key: 'xuanjie_o3', name: 'Xiaomi Xring O3' },
  ];
  let cpuModelsCache = null;
  async function getCpuModels() {
    if (cpuModelsCache) return cpuModelsCache;
    if (!hasBridge()) { cpuModelsCache = BUILTIN_CPU_MODELS.slice(); return cpuModelsCache; }
    try {
      const obj = JSON.parse(await execCommand(`cat ${CPU_DIR}/manifest.json`));
      const models = (obj && Array.isArray(obj.models))
        ? obj.models.filter(m => m && m.key && m.name) : [];
      cpuModelsCache = models.length ? models : BUILTIN_CPU_MODELS.slice();
    } catch (e) { cpuModelsCache = BUILTIN_CPU_MODELS.slice(); }
    return cpuModelsCache;
  }
  /* sync display-name lookup off the loaded cache (BUILTIN fallback before load) */
  function cpuModelName(key) {
    if (!key) return '';
    const m = (cpuModelsCache || BUILTIN_CPU_MODELS).find(x => x.key === key);
    return m ? m.name : key;
  }
  /* Which CPU profiles actually have a CPU/cpuinfo_<key> file on disk. The picker
     uses this to flag a manifest entry whose file is MISSING (deleted via a file
     manager, or a failed sync) — picking it would make the native side mount nothing.
     Device only (one `ls`); preview returns null = "can't check" → treat all present. */
  async function cpuFileKeys() {
    if (!hasBridge()) return null;
    try {
      const out = await execCommand(`ls -1 ${CPU_DIR} 2>/dev/null`);
      const keys = new Set();
      String(out).split('\n').forEach(line => {
        const m = line.trim().match(/^cpuinfo_(.+)$/);
        if (m) keys.add(m[1]);
      });
      return keys;
    } catch (e) { return null; }
  }

  /* key from a user-given name, sanitized to [a-z0-9_] — matches the companion's
     path-traversal guard (CPU/cpuinfo_<key>). */
  function cpuKeyFromName(name) {
    return String(name || '').trim().toLowerCase()
      .replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 40);
  }
  /* Import a user-supplied cpuinfo file as a reusable CPU profile: writes it to
     CPU/cpuinfo_<key> (0444 + system_file context so the bind-mount reads like a
     real system file) and appends {key,name} to CPU/manifest.json. Throws a short
     code on validation failure (mapped to a message in the picker UI). */
  async function importCpuModel(name, text) {
    name = String(name || '').trim();
    const body = String(text || '');
    if (!name)                 throw new Error('name_required');
    if (!body.trim())          throw new Error('empty');
    if (body.length > 262144)  throw new Error('too_large');
    if (!/(^|\n)\s*processor\s*:/i.test(body) && !/Hardware\s*:/i.test(body))
      throw new Error('not_cpuinfo');
    const key = cpuKeyFromName(name);
    if (!key)                  throw new Error('bad_name');
    const models = (await getCpuModels()).slice();
    if (models.some(m => m.key === key)) throw new Error('exists');
    const entry = { key, name };
    if (!hasBridge()) {                       // preview: can't persist; this session only
      models.push(entry); cpuModelsCache = models; previewUnsaved = true;
      return { ...entry, preview: true };
    }
    const file = `${CPU_DIR}/cpuinfo_${key}`;
    await execCommand(`mkdir -p ${CPU_DIR}`);
    await execCommand(`echo '${b64(body)}' | base64 -d > ${file}`);
    try {
      await execCommand(`chmod 0444 ${file}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${file}`);
    } catch (e) { console.warn('cpu import chmod/chcon:', e); }
    const next = { models: models.concat([entry]) };
    const mf = `${CPU_DIR}/manifest.json`;
    await execCommand(`echo '${b64(JSON.stringify(next, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try {
      await execCommand(`chmod 0644 ${mf}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${mf}`);
    } catch (e) { console.warn('cpu manifest chmod/chcon:', e); }
    cpuModelsCache = next.models;
    return entry;
  }
  /* Delete a CPU profile: removes CPU/cpuinfo_<key> + its manifest entry. Any profile
     is deletable (incl. the default — a file manager could remove it anyway); the
     native side is fail-safe if a package's cpu=<key> stops resolving (it just mounts
     nothing / the legacy default if present, no crash). */
  async function deleteCpuModel(key) {
    if (!key) return;
    const models = (await getCpuModels()).filter(m => m.key !== key);
    if (!hasBridge()) { cpuModelsCache = models; previewUnsaved = true; return; }
    try { await execCommand(`rm -f ${CPU_DIR}/cpuinfo_${key}`); } catch (e) { console.warn('cpu del rm:', e); }
    const mf = `${CPU_DIR}/manifest.json`;
    await execCommand(`echo '${b64(JSON.stringify({ models }, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try { await execCommand(`chmod 0644 ${mf}`); await execCommand(`chcon u:object_r:system_file:s0 ${mf}`); } catch (e) {}
    cpuModelsCache = models;
  }

  /* ─── WebView UA profile library (UA/manifest.json) ───
     Flat array [{key,name,ua}] read by BOTH the native side (resolves ua=<key> → ua) and this picker,
     same pattern as GPU/manifest.json. Keep BUILTIN in sync with module/UA/manifest.json. */
  const UA_DIR = MODULE_DIR + '/UA';
  const BUILTIN_UA_PROFILES = [
    { key: 'ipad_safari',    name: 'iPad Pro — Safari',    ua: 'Mozilla/5.0 (iPad; CPU OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1' },
    { key: 'iphone_safari',  name: 'iPhone 15 — Safari',   ua: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1' },
    { key: 'pixel_chrome',   name: 'Pixel 8 — Chrome',     ua: 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36' },
    { key: 'desktop_chrome', name: 'Desktop — Chrome/Win', ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36' },
  ];
  let uaProfilesCache = null;
  async function getUaProfiles() {
    if (uaProfilesCache) return uaProfilesCache;
    if (!hasBridge()) { uaProfilesCache = BUILTIN_UA_PROFILES.slice(); return uaProfilesCache; }
    try {
      const arr = JSON.parse(await execCommand(`cat ${UA_DIR}/manifest.json`));
      const ps = Array.isArray(arr) ? arr.filter(p => p && p.key && p.name && p.ua) : [];
      uaProfilesCache = ps.length ? ps : BUILTIN_UA_PROFILES.slice();
    } catch (e) { uaProfilesCache = BUILTIN_UA_PROFILES.slice(); }
    return uaProfilesCache;
  }
  function uaProfileName(key) {
    if (!key) return '';
    const p = (uaProfilesCache || BUILTIN_UA_PROFILES).find(x => x.key === key);
    return p ? p.name : key;
  }
  function uaKeyFromName(name) {
    return String(name || '').trim().toLowerCase()
      .replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 40);
  }
  /* Add/update a custom UA profile {name, ua} → appends to UA/manifest.json. Mirrors importGpuModel. */
  async function importUaProfile(o) {
    const name = String(o && o.name || '').trim();
    const ua   = String(o && o.ua   || '').trim();
    if (!name || !ua) throw new Error('name + UA required');
    const key = uaKeyFromName(name);
    const list = (await getUaProfiles()).slice().filter(p => p.key !== key);
    list.push({ key, name, ua });
    if (hasBridge()) {
      const js = shq(JSON.stringify(list, null, 2));
      await execCommand(`mkdir -p ${UA_DIR}; echo '${js}' > ${UA_DIR}/manifest.json; chmod 644 ${UA_DIR}/manifest.json; chcon u:object_r:system_file:s0 ${UA_DIR}/manifest.json 2>/dev/null`);
    }
    uaProfilesCache = list;
    return key;
  }
  async function deleteUaProfile(key) {
    const list = (await getUaProfiles()).slice().filter(p => p.key !== key);
    if (hasBridge()) {
      const js = shq(JSON.stringify(list, null, 2));
      await execCommand(`echo '${js}' > ${UA_DIR}/manifest.json; chmod 644 ${UA_DIR}/manifest.json; chcon u:object_r:system_file:s0 ${UA_DIR}/manifest.json 2>/dev/null`);
    }
    uaProfilesCache = list;
  }

  /* ─── GPU model library (GPU/manifest.json) — REFERENCE branch only ───
     Unlike CPU (manifest is display-only; native uses <key> as a filename), a GPU
     profile is JUST strings — renderer / vendor / Vulkan vendorID — so the SAME flat
     array file is read by the native side AND this picker. Format is a flat array
     [{key,name,renderer,vendor,vendor_id}] (NOT the CPU {models:[…]} shape). Keep
     BUILTIN in sync with module/GPU/manifest.json. */
  const GPU_DIR = MODULE_DIR + '/GPU';
  const BUILTIN_GPU_MODELS = [
    { key: 'adreno830', name: 'Adreno 830 (Snapdragon 8 Elite)',      renderer: 'Adreno (TM) 830', vendor: 'Qualcomm', vendor_id: '0x5143' },
    { key: 'adreno840', name: 'Adreno 840 (Snapdragon 8 Elite Gen 5)', renderer: 'Adreno (TM) 840', vendor: 'Qualcomm', vendor_id: '0x5143' },
  ];
  let gpuModelsCache = null;
  async function getGpuModels() {
    if (gpuModelsCache) return gpuModelsCache;
    if (!hasBridge()) { gpuModelsCache = BUILTIN_GPU_MODELS.slice(); return gpuModelsCache; }
    try {
      const arr = JSON.parse(await execCommand(`cat ${GPU_DIR}/manifest.json`));
      const models = Array.isArray(arr) ? arr.filter(m => m && m.key && m.name) : [];
      gpuModelsCache = models.length ? models : BUILTIN_GPU_MODELS.slice();
    } catch (e) { gpuModelsCache = BUILTIN_GPU_MODELS.slice(); }
    return gpuModelsCache;
  }
  function gpuModelName(key) {
    if (!key) return '';
    const m = (gpuModelsCache || BUILTIN_GPU_MODELS).find(x => x.key === key);
    return m ? m.name : key;
  }
  function gpuKeyFromName(name) {
    return String(name || '').trim().toLowerCase()
      .replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 40);
  }
  /* Normalise a GL_EXTENSIONS input (space/newline/comma separated) to a clean
     single-spaced list of valid extension tokens ([A-Za-z0-9_], e.g.
     GL_EXT_geometry_shader). Drops junk tokens; '' when nothing valid. These are
     APPENDED to the driver's real list natively (never replace it). */
  function normExtensions(s) {
    return String(s || '').split(/[\s,]+/)
      .map(t => t.trim()).filter(t => /^[A-Za-z0-9_]+$/.test(t))
      .join(' ');
  }
  /* Pack an apiVersion input into the uint32 Vulkan encoding. Accepts "1.3.0"
     (major<<22 | minor<<12 | patch) or a raw number / 0x hex. 0 when blank. */
  function packApiVersion(s) {
    s = String(s || '').trim();
    if (!s) return 0;
    const m = s.match(/^(\d+)\.(\d+)(?:\.(\d+))?$/);
    if (m) return (((+m[1]) << 22) | ((+m[2]) << 12) | (+(m[3] || 0))) >>> 0;
    const n = parseInt(s, s.slice(0, 2).toLowerCase() === '0x' ? 16 : 10);
    return Number.isFinite(n) && n > 0 ? (n >>> 0) : 0;
  }
  // 55 VkPhysicalDeviceFeatures bits → the "force all features" mask.
  const VK_FEATURES_ALL = '0x7fffffffffffff';

  /* Validate + build a GPU manifest entry from a fields object (shared by
     import/update). Throws a short code on bad input. Optional fields are omitted
     when blank so native keeps the real value (empty = no-op). Advanced Vulkan
     (apiVersion/driverVersion/features/VRAM) are all optional + default-off. */
  function buildGpuEntry(o) {
    const name      = String(o.name || '').trim();
    const renderer  = String(o.renderer || '').trim();
    const vendor    = String(o.vendor || '').trim();
    const vendorId  = String(o.vendorId || '').trim();
    const glVersion = String(o.glVersion || '').trim();
    const deviceId  = String(o.deviceId || '').trim();
    const extensions = normExtensions(o.extensions);
    if (!name)                          throw new Error('name_required');
    if (!renderer)                      throw new Error('renderer_required');
    if (!/^0x[0-9a-fA-F]{1,8}$/.test(vendorId)) throw new Error('bad_vendorid');
    if (deviceId && !/^0x[0-9a-fA-F]{1,8}$/.test(deviceId)) throw new Error('bad_deviceid');
    const key = gpuKeyFromName(name);
    if (!key)                           throw new Error('bad_name');
    const entry = { key, name, renderer, vendor: vendor || 'Qualcomm', vendor_id: vendorId.toLowerCase() };
    if (glVersion)  entry.gl_version = glVersion;         // optional: faked GL_VERSION
    if (deviceId)   entry.device_id  = deviceId.toLowerCase();  // optional: faked Vulkan deviceID
    if (extensions) entry.extensions = extensions;       // optional/risky: appended GL extensions
    const api = packApiVersion(o.apiVersion);
    if (api) entry.vk_api_version = api;                  // optional: Vulkan apiVersion
    const drv = packApiVersion(o.driverVersion);   // "x.y.z" (major<<22|minor<<12|patch) or raw
    if (drv) entry.vk_driver_version = drv;         // optional: Vulkan driverVersion
    if (o.vkFeatures) entry.vk_features = VK_FEATURES_ALL;  // optional/risky: force all 55 features
    const vram = parseInt(String(o.vramMb || '').trim(), 10);
    if (Number.isFinite(vram) && vram > 0) entry.vram_mb = vram;  // optional: reported VRAM (MB)
    const glsl = String(o.glslVersion || '').trim();
    if (glsl) entry.glsl_version = glsl;                  // optional: GL_SHADING_LANGUAGE_VERSION
    const egl = String(o.eglVersion || '').trim();
    if (egl) entry.egl_version = egl;                     // optional: EGL_VERSION
    return { key, entry };
  }

  /* Add a custom GPU profile from a fields object → appends to GPU/manifest.json.
     vendorID is config-driven so Mali (0x13B5) / Imagination (0x1010) etc. need no
     native change. Throws a short code on validation failure ('exists' on dup key). */
  async function importGpuModel(o) {
    const { key, entry } = buildGpuEntry(o || {});
    const models = (await getGpuModels()).slice();
    if (models.some(m => m.key === key)) throw new Error('exists');
    if (!hasBridge()) {                       // preview: session-only
      models.push(entry); gpuModelsCache = models; previewUnsaved = true;
      return { ...entry, preview: true };
    }
    const next = models.concat([entry]);
    const mf = `${GPU_DIR}/manifest.json`;
    await execCommand(`mkdir -p ${GPU_DIR}`);
    await execCommand(`echo '${b64(JSON.stringify(next, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try {
      await execCommand(`chmod 0644 ${mf}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${mf}`);
    } catch (e) { console.warn('gpu manifest chmod/chcon:', e); }
    gpuModelsCache = next;
    return entry;
  }
  /* Edit an existing GPU profile in place from a fields object. The KEY is immutable
     (renaming would orphan every package carrying gpu=<key>) — only the display/spoof
     fields change; the entry keeps its manifest position. Throws 'not_found' if the
     key is gone. Works on built-ins too (rewrites the shipped manifest). */
  async function updateGpuModel(key, o) {
    key = String(key || '').trim();
    if (!key)                           throw new Error('not_found');
    const built = buildGpuEntry(o || {});
    const entry = { ...built.entry, key };     // keep the ORIGINAL key (immutable)
    const models = (await getGpuModels()).slice();
    const idx = models.findIndex(m => m.key === key);
    if (idx < 0)                        throw new Error('not_found');
    models[idx] = entry;                       // replace in place (keeps key + order)
    if (!hasBridge()) {                        // preview: session-only
      gpuModelsCache = models; previewUnsaved = true;
      return { ...entry, preview: true };
    }
    const mf = `${GPU_DIR}/manifest.json`;
    await execCommand(`mkdir -p ${GPU_DIR}`);
    await execCommand(`echo '${b64(JSON.stringify(models, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try {
      await execCommand(`chmod 0644 ${mf}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${mf}`);
    } catch (e) { console.warn('gpu manifest chmod/chcon:', e); }
    gpuModelsCache = models;
    return entry;
  }
  /* Delete a GPU profile (manifest entry; no data file). Any profile is deletable;
     native is fail-safe — a gpu=<key> that no longer resolves just no-ops (hook not
     armed). If the modal's default pick (adreno830) is gone, a new GPU toggle simply
     has nothing to fall back to until the user picks/adds one. */
  async function deleteGpuModel(key) {
    if (!key) return;
    const models = (await getGpuModels()).filter(m => m.key !== key);
    if (!hasBridge()) { gpuModelsCache = models; previewUnsaved = true; return; }
    const mf = `${GPU_DIR}/manifest.json`;
    await execCommand(`echo '${b64(JSON.stringify(models, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try { await execCommand(`chmod 0644 ${mf}`); await execCommand(`chcon u:object_r:system_file:s0 ${mf}`); } catch (e) {}
    gpuModelsCache = models;
  }

  /* ─── Timezone library (TZ/manifest.json holds user-added CUSTOM zones) ───
     Built-in common IANA zones (inline) + any custom zones the user typed (persisted in
     TZ/manifest.json on device; preview keeps them for the session). A "zone" here is just
     the IANA id — no native file (unlike CPU): the tz=<id> tag carries the value and the
     native side applies it directly (TimeZone.setDefault + persist.sys.timezone COW). */
  const TZ_DIR = MODULE_DIR + '/TZ';
  const BUILTIN_TZ = [
    'UTC', 'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
    'America/Sao_Paulo', 'Europe/London', 'Europe/Lisbon', 'Europe/Paris', 'Europe/Berlin', 'Europe/Moscow',
    'Europe/Istanbul', 'Africa/Cairo', 'Africa/Lagos', 'Asia/Dubai', 'Asia/Tehran',
    'Asia/Karachi', 'Asia/Kolkata', 'Asia/Dhaka', 'Asia/Bangkok', 'Asia/Shanghai',
    'Asia/Singapore', 'Asia/Tokyo', 'Asia/Seoul', 'Australia/Sydney', 'Pacific/Auckland',
  ].map(z => ({ key: z, name: z }));
  const isBuiltinTz = key => BUILTIN_TZ.some(z => z.key === key);
  // Valid IANA-ish id: "Area/Location" (letters/digits/_/+/-, '/'-separated) or a bare word
  // like UTC. Must stay colon-free so it survives the tag split.
  function validTz(id) {
    id = String(id || '').trim();
    return id.length > 0 && id.length <= 64 && id.indexOf(':') < 0 &&
           /^[A-Za-z][A-Za-z0-9_+-]*(\/[A-Za-z0-9_+-]+)*$/.test(id);
  }
  let tzZonesCache = null;
  async function getTzZones() {
    if (tzZonesCache) return tzZonesCache;
    if (!hasBridge()) { tzZonesCache = BUILTIN_TZ.slice(); return tzZonesCache; }
    try {
      const obj = JSON.parse(await execCommand(`cat ${TZ_DIR}/manifest.json`));
      const custom = (obj && Array.isArray(obj.zones))
        ? obj.zones.filter(z => z && z.key).map(z => ({ key: z.key, name: z.name || z.key })) : [];
      const seen = new Set(BUILTIN_TZ.map(z => z.key));
      tzZonesCache = BUILTIN_TZ.concat(custom.filter(z => !seen.has(z.key)));
    } catch (e) { tzZonesCache = BUILTIN_TZ.slice(); }
    return tzZonesCache;
  }
  function tzName(key) {
    if (!key) return '';
    const z = (tzZonesCache || BUILTIN_TZ).find(x => x.key === key);
    return z ? z.name : key;
  }
  // Add a custom IANA zone → appended to TZ/manifest.json (only the custom set is persisted;
  // built-ins are inline). Throws a short code (empty/bad_id/exists) mapped to a UI message.
  async function importTzZone(id) {
    id = String(id || '').trim();
    if (!id)          throw new Error('empty');
    if (!validTz(id)) throw new Error('bad_id');
    const zones = await getTzZones();
    if (zones.some(z => z.key === id)) throw new Error('exists');
    const entry = { key: id, name: id };
    const customs = zones.filter(z => !isBuiltinTz(z.key)).concat([entry]);
    if (!hasBridge()) { tzZonesCache = zones.concat([entry]); previewUnsaved = true; return { ...entry, preview: true }; }
    await execCommand(`mkdir -p ${TZ_DIR}`);
    const mf = `${TZ_DIR}/manifest.json`;
    await execCommand(`echo '${b64(JSON.stringify({ zones: customs }, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try { await execCommand(`chmod 0644 ${mf}`); await execCommand(`chcon u:object_r:system_file:s0 ${mf}`); } catch (e) {}
    tzZonesCache = zones.concat([entry]);
    return entry;
  }
  // Delete a CUSTOM zone (built-ins are not deletable).
  async function deleteTzZone(key) {
    if (!key || isBuiltinTz(key)) return;
    const zones = (await getTzZones()).filter(z => z.key !== key);
    if (!hasBridge()) { tzZonesCache = zones; previewUnsaved = true; return; }
    const customs = zones.filter(z => !isBuiltinTz(z.key));
    const mf = `${TZ_DIR}/manifest.json`;
    await execCommand(`echo '${b64(JSON.stringify({ zones: customs }, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try { await execCommand(`chmod 0644 ${mf}`); await execCommand(`chcon u:object_r:system_file:s0 ${mf}`); } catch (e) {}
    tzZonesCache = zones;
  }

  /* ─── Locale library (LANG/manifest.json holds user-added CUSTOM locales) ───
     Built-in common BCP-47 locales (inline, with friendly names) + any custom tags the user typed
     (persisted in LANG/manifest.json on device; preview keeps them for the session). Like tz there
     is no native file — the lang=<tag> tag carries the value and the system_server hook applies it. */
  const LANG_DIR = MODULE_DIR + '/LANG';
  const BUILTIN_LANG = [
    ['en-US', 'English (United States)'], ['en-GB', 'English (United Kingdom)'],
    ['es-ES', 'Spanish (Spain)'], ['es-MX', 'Spanish (Mexico)'],
    ['pt-BR', 'Portuguese (Brazil)'], ['pt-PT', 'Portuguese (Portugal)'],
    ['fr-FR', 'French (France)'], ['de-DE', 'German (Germany)'], ['it-IT', 'Italian (Italy)'],
    ['nl-NL', 'Dutch (Netherlands)'], ['ru-RU', 'Russian (Russia)'], ['pl-PL', 'Polish (Poland)'],
    ['tr-TR', 'Turkish (Turkey)'], ['ar-SA', 'Arabic (Saudi Arabia)'], ['ar-EG', 'Arabic (Egypt)'],
    ['fa-IR', 'Persian (Iran)'], ['hi-IN', 'Hindi (India)'], ['id-ID', 'Indonesian (Indonesia)'],
    ['th-TH', 'Thai (Thailand)'], ['vi-VN', 'Vietnamese (Vietnam)'], ['ja-JP', 'Japanese (Japan)'],
    ['ko-KR', 'Korean (South Korea)'], ['zh-Hans-CN', 'Chinese (Simplified, China)'],
    ['zh-Hant-TW', 'Chinese (Traditional, Taiwan)'], ['zh-Hant-HK', 'Chinese (Traditional, Hong Kong)'],
  ].map(([key, name]) => ({ key, name }));
  const isBuiltinLang = key => BUILTIN_LANG.some(z => z.key === key);
  // Valid BCP-47-ish tag: a 2-3 letter language subtag + optional '-'-separated subtags (script/
  // region/variant, alnum). Must stay colon-free so it survives the tag split.
  function validLang(id) {
    id = String(id || '').trim();
    return id.length > 0 && id.length <= 35 && id.indexOf(':') < 0 &&
           /^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$/.test(id);
  }
  let langLocalesCache = null;
  async function getLangLocales() {
    if (langLocalesCache) return langLocalesCache;
    if (!hasBridge()) { langLocalesCache = BUILTIN_LANG.slice(); return langLocalesCache; }
    try {
      const obj = JSON.parse(await execCommand(`cat ${LANG_DIR}/manifest.json`));
      const custom = (obj && Array.isArray(obj.locales))
        ? obj.locales.filter(z => z && z.key).map(z => ({ key: z.key, name: z.name || z.key })) : [];
      const seen = new Set(BUILTIN_LANG.map(z => z.key));
      langLocalesCache = BUILTIN_LANG.concat(custom.filter(z => !seen.has(z.key)));
    } catch (e) { langLocalesCache = BUILTIN_LANG.slice(); }
    return langLocalesCache;
  }
  function langName(key) {
    if (!key) return '';
    const z = (langLocalesCache || BUILTIN_LANG).find(x => x.key === key);
    return z ? z.name : key;
  }
  async function importLangLocale(id) {
    id = String(id || '').trim();
    if (!id)            throw new Error('empty');
    if (!validLang(id)) throw new Error('bad_id');
    const locs = await getLangLocales();
    if (locs.some(z => z.key === id)) throw new Error('exists');
    const entry = { key: id, name: id };
    const customs = locs.filter(z => !isBuiltinLang(z.key)).concat([entry]);
    if (!hasBridge()) { langLocalesCache = locs.concat([entry]); previewUnsaved = true; return { ...entry, preview: true }; }
    await execCommand(`mkdir -p ${LANG_DIR}`);
    const mf = `${LANG_DIR}/manifest.json`;
    await execCommand(`echo '${b64(JSON.stringify({ locales: customs }, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try { await execCommand(`chmod 0644 ${mf}`); await execCommand(`chcon u:object_r:system_file:s0 ${mf}`); } catch (e) {}
    langLocalesCache = locs.concat([entry]);
    return entry;
  }
  async function deleteLangLocale(key) {
    if (!key || isBuiltinLang(key)) return;
    const locs = (await getLangLocales()).filter(z => z.key !== key);
    if (!hasBridge()) { langLocalesCache = locs; previewUnsaved = true; return; }
    const customs = locs.filter(z => !isBuiltinLang(z.key));
    const mf = `${LANG_DIR}/manifest.json`;
    await execCommand(`echo '${b64(JSON.stringify({ locales: customs }, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try { await execCommand(`chmod 0644 ${mf}`); await execCommand(`chcon u:object_r:system_file:s0 ${mf}`); } catch (e) {}
    langLocalesCache = locs;
  }

  /* ─── SIM / carrier library (SIM/manifest.json) — PRO ───
     Like GPU: a flat array [{key,name,mcc,mnc,iso}] read by BOTH the native side and
     this picker. Keep BUILTIN in sync with module/SIM/manifest.json. Custom carriers
     append to the manifest (reusable across apps). carrier_id is never stored (a fake
     name + real/placeholder id is a detectable mismatch — see CarrierHook.java). */
  const SIM_DIR = MODULE_DIR + '/SIM';
  const BUILTIN_CARRIERS = [
    { key: 'tmobile_us',      name: 'T-Mobile',       mcc: '310', mnc: '260', iso: 'us' },
    { key: 'att_us',          name: 'AT&T',           mcc: '310', mnc: '410', iso: 'us' },
    { key: 'verizon_us',      name: 'Verizon',        mcc: '311', mnc: '480', iso: 'us' },
    { key: 'vodafone_uk',     name: 'Vodafone',       mcc: '234', mnc: '15',  iso: 'gb' },
    { key: 'ee_uk',           name: 'EE',             mcc: '234', mnc: '30',  iso: 'gb' },
    { key: 'orange_fr',       name: 'Orange',         mcc: '208', mnc: '01',  iso: 'fr' },
    { key: 'telekom_de',      name: 'Telekom',        mcc: '262', mnc: '01',  iso: 'de' },
    { key: 'airtel_in',       name: 'Airtel',         mcc: '404', mnc: '45',  iso: 'in' },
    { key: 'jio_in',          name: 'Jio',            mcc: '405', mnc: '840', iso: 'in' },
    { key: 'docomo_jp',       name: 'NTT Docomo',     mcc: '440', mnc: '10',  iso: 'jp' },
    { key: 'chinamobile',     name: 'China Mobile',   mcc: '460', mnc: '00',  iso: 'cn' },
    { key: 'turkcell_tr',     name: 'Turkcell',       mcc: '286', mnc: '01',  iso: 'tr' },
    { key: 'uscellular_us',   name: 'U.S. Cellular',  mcc: '311', mnc: '580', iso: 'us' },
    { key: 'rogers_ca',       name: 'Rogers',         mcc: '302', mnc: '720', iso: 'ca' },
    { key: 'bell_ca',         name: 'Bell',           mcc: '302', mnc: '610', iso: 'ca' },
    { key: 'telus_ca',        name: 'Telus',          mcc: '302', mnc: '220', iso: 'ca' },
    { key: 'telcel_mx',       name: 'Telcel',         mcc: '334', mnc: '020', iso: 'mx' },
    { key: 'movistar_mx',     name: 'Movistar',       mcc: '334', mnc: '030', iso: 'mx' },
    { key: 'att_mx',          name: 'AT&T Mexico',    mcc: '334', mnc: '050', iso: 'mx' },
    { key: 'vivo_br',         name: 'Vivo',           mcc: '724', mnc: '06',  iso: 'br' },
    { key: 'claro_br',        name: 'Claro',          mcc: '724', mnc: '05',  iso: 'br' },
    { key: 'tim_br',          name: 'TIM',            mcc: '724', mnc: '04',  iso: 'br' },
    { key: 'claro_ar',        name: 'Claro',          mcc: '722', mnc: '310', iso: 'ar' },
    { key: 'movistar_ar',     name: 'Movistar',       mcc: '722', mnc: '07',  iso: 'ar' },
    { key: 'personal_ar',     name: 'Personal',       mcc: '722', mnc: '34',  iso: 'ar' },
    { key: 'claro_co',        name: 'Claro',          mcc: '732', mnc: '101', iso: 'co' },
    { key: 'movistar_co',     name: 'Movistar',       mcc: '732', mnc: '123', iso: 'co' },
    { key: 'tigo_co',         name: 'Tigo',           mcc: '732', mnc: '103', iso: 'co' },
    { key: 'entel_cl',        name: 'Entel',          mcc: '730', mnc: '01',  iso: 'cl' },
    { key: 'movistar_cl',     name: 'Movistar',       mcc: '730', mnc: '02',  iso: 'cl' },
    { key: 'claro_cl',        name: 'Claro',          mcc: '730', mnc: '03',  iso: 'cl' },
    { key: 'claro_pe',        name: 'Claro',          mcc: '716', mnc: '10',  iso: 'pe' },
    { key: 'movistar_pe',     name: 'Movistar',       mcc: '716', mnc: '06',  iso: 'pe' },
    { key: 'o2_uk',           name: 'O2',             mcc: '234', mnc: '10',  iso: 'gb' },
    { key: 'three_uk',        name: 'Three',          mcc: '234', mnc: '20',  iso: 'gb' },
    { key: 'sfr_fr',          name: 'SFR',            mcc: '208', mnc: '10',  iso: 'fr' },
    { key: 'bouygues_fr',     name: 'Bouygues',       mcc: '208', mnc: '20',  iso: 'fr' },
    { key: 'free_fr',         name: 'Free',           mcc: '208', mnc: '15',  iso: 'fr' },
    { key: 'vodafone_de',     name: 'Vodafone',       mcc: '262', mnc: '02',  iso: 'de' },
    { key: 'o2_de',           name: 'O2',             mcc: '262', mnc: '03',  iso: 'de' },
    { key: 'movistar_es',     name: 'Movistar',       mcc: '214', mnc: '07',  iso: 'es' },
    { key: 'vodafone_es',     name: 'Vodafone',       mcc: '214', mnc: '01',  iso: 'es' },
    { key: 'orange_es',       name: 'Orange',         mcc: '214', mnc: '03',  iso: 'es' },
    { key: 'tim_it',          name: 'TIM',            mcc: '222', mnc: '01',  iso: 'it' },
    { key: 'vodafone_it',     name: 'Vodafone',       mcc: '222', mnc: '10',  iso: 'it' },
    { key: 'windtre_it',      name: 'WindTre',        mcc: '222', mnc: '88',  iso: 'it' },
    { key: 'kpn_nl',          name: 'KPN',            mcc: '204', mnc: '08',  iso: 'nl' },
    { key: 'vodafone_nl',     name: 'Vodafone',       mcc: '204', mnc: '04',  iso: 'nl' },
    { key: 'tmobile_nl',      name: 'Odido',          mcc: '204', mnc: '16',  iso: 'nl' },
    { key: 'orange_pl',       name: 'Orange',         mcc: '260', mnc: '03',  iso: 'pl' },
    { key: 'play_pl',         name: 'Play',           mcc: '260', mnc: '06',  iso: 'pl' },
    { key: 'plus_pl',         name: 'Plus',           mcc: '260', mnc: '01',  iso: 'pl' },
    { key: 'mts_ru',          name: 'MTS',            mcc: '250', mnc: '01',  iso: 'ru' },
    { key: 'beeline_ru',      name: 'Beeline',        mcc: '250', mnc: '99',  iso: 'ru' },
    { key: 'megafon_ru',      name: 'MegaFon',        mcc: '250', mnc: '02',  iso: 'ru' },
    { key: 'telia_se',        name: 'Telia',          mcc: '240', mnc: '01',  iso: 'se' },
    { key: 'tele2_se',        name: 'Tele2',          mcc: '240', mnc: '07',  iso: 'se' },
    { key: 'swisscom_ch',     name: 'Swisscom',       mcc: '228', mnc: '01',  iso: 'ch' },
    { key: 'proximus_be',     name: 'Proximus',       mcc: '206', mnc: '01',  iso: 'be' },
    { key: 'meo_pt',          name: 'MEO',            mcc: '268', mnc: '06',  iso: 'pt' },
    { key: 'vodafone_pt',     name: 'Vodafone',       mcc: '268', mnc: '01',  iso: 'pt' },
    { key: 'a1_at',           name: 'A1',             mcc: '232', mnc: '01',  iso: 'at' },
    { key: 'vodafone_ie',     name: 'Vodafone',       mcc: '272', mnc: '01',  iso: 'ie' },
    { key: 'telenor_no',      name: 'Telenor',        mcc: '242', mnc: '01',  iso: 'no' },
    { key: 'tdc_dk',          name: 'TDC',            mcc: '238', mnc: '01',  iso: 'dk' },
    { key: 'elisa_fi',        name: 'Elisa',          mcc: '244', mnc: '05',  iso: 'fi' },
    { key: 'cosmote_gr',      name: 'Cosmote',        mcc: '202', mnc: '01',  iso: 'gr' },
    { key: 'tmobile_cz',      name: 'T-Mobile',       mcc: '230', mnc: '01',  iso: 'cz' },
    { key: 'orange_ro',       name: 'Orange',         mcc: '226', mnc: '10',  iso: 'ro' },
    { key: 'kyivstar_ua',     name: 'Kyivstar',       mcc: '255', mnc: '03',  iso: 'ua' },
    { key: 'telekom_hu',      name: 'Telekom',        mcc: '216', mnc: '30',  iso: 'hu' },
    { key: 'irancell_ir',     name: 'Irancell',       mcc: '432', mnc: '35',  iso: 'ir' },
    { key: 'mci_ir',          name: 'Hamrah-e Avval', mcc: '432', mnc: '11',  iso: 'ir' },
    { key: 'rightel_ir',      name: 'RighTel',        mcc: '432', mnc: '20',  iso: 'ir' },
    { key: 'etisalat_ae',     name: 'Etisalat',       mcc: '424', mnc: '02',  iso: 'ae' },
    { key: 'du_ae',           name: 'du',             mcc: '424', mnc: '03',  iso: 'ae' },
    { key: 'stc_sa',          name: 'STC',            mcc: '420', mnc: '01',  iso: 'sa' },
    { key: 'mobily_sa',       name: 'Mobily',         mcc: '420', mnc: '03',  iso: 'sa' },
    { key: 'zain_sa',         name: 'Zain',           mcc: '420', mnc: '04',  iso: 'sa' },
    { key: 'cellcom_il',      name: 'Cellcom',        mcc: '425', mnc: '02',  iso: 'il' },
    { key: 'partner_il',      name: 'Partner',        mcc: '425', mnc: '01',  iso: 'il' },
    { key: 'ooredoo_qa',      name: 'Ooredoo',        mcc: '427', mnc: '01',  iso: 'qa' },
    { key: 'zain_kw',         name: 'Zain',           mcc: '419', mnc: '02',  iso: 'kw' },
    { key: 'zain_iq',         name: 'Zain',           mcc: '418', mnc: '20',  iso: 'iq' },
    { key: 'zain_jo',         name: 'Zain',           mcc: '416', mnc: '01',  iso: 'jo' },
    { key: 'vodafone_eg',     name: 'Vodafone',       mcc: '602', mnc: '02',  iso: 'eg' },
    { key: 'orange_eg',       name: 'Orange',         mcc: '602', mnc: '01',  iso: 'eg' },
    { key: 'etisalat_eg',     name: 'Etisalat',       mcc: '602', mnc: '03',  iso: 'eg' },
    { key: 'vodacom_za',      name: 'Vodacom',        mcc: '655', mnc: '01',  iso: 'za' },
    { key: 'mtn_za',          name: 'MTN',            mcc: '655', mnc: '10',  iso: 'za' },
    { key: 'mtn_ng',          name: 'MTN',            mcc: '621', mnc: '30',  iso: 'ng' },
    { key: 'airtel_ng',       name: 'Airtel',         mcc: '621', mnc: '20',  iso: 'ng' },
    { key: 'glo_ng',          name: 'Glo',            mcc: '621', mnc: '50',  iso: 'ng' },
    { key: 'safaricom_ke',    name: 'Safaricom',      mcc: '639', mnc: '02',  iso: 'ke' },
    { key: 'iam_ma',          name: 'Maroc Telecom',  mcc: '604', mnc: '01',  iso: 'ma' },
    { key: 'djezzy_dz',       name: 'Djezzy',         mcc: '603', mnc: '02',  iso: 'dz' },
    { key: 'mtn_gh',          name: 'MTN',            mcc: '620', mnc: '01',  iso: 'gh' },
    { key: 'vodacom_tz',      name: 'Vodacom',        mcc: '640', mnc: '04',  iso: 'tz' },
    { key: 'ethio_et',        name: 'Ethio Telecom',  mcc: '636', mnc: '01',  iso: 'et' },
    { key: 'vi_in',           name: 'Vi',             mcc: '404', mnc: '11',  iso: 'in' },
    { key: 'unicom_cn',       name: 'China Unicom',   mcc: '460', mnc: '01',  iso: 'cn' },
    { key: 'telecom_cn',      name: 'China Telecom',  mcc: '460', mnc: '11',  iso: 'cn' },
    { key: 'au_jp',           name: 'au (KDDI)',      mcc: '440', mnc: '50',  iso: 'jp' },
    { key: 'softbank_jp',     name: 'SoftBank',       mcc: '440', mnc: '20',  iso: 'jp' },
    { key: 'skt_kr',          name: 'SK Telecom',     mcc: '450', mnc: '05',  iso: 'kr' },
    { key: 'kt_kr',           name: 'KT',             mcc: '450', mnc: '08',  iso: 'kr' },
    { key: 'lguplus_kr',      name: 'LG U+',          mcc: '450', mnc: '06',  iso: 'kr' },
    { key: 'telkomsel_id',    name: 'Telkomsel',      mcc: '510', mnc: '10',  iso: 'id' },
    { key: 'indosat_id',      name: 'Indosat',        mcc: '510', mnc: '01',  iso: 'id' },
    { key: 'xl_id',           name: 'XL',             mcc: '510', mnc: '11',  iso: 'id' },
    { key: 'jazz_pk',         name: 'Jazz',           mcc: '410', mnc: '01',  iso: 'pk' },
    { key: 'telenor_pk',      name: 'Telenor',        mcc: '410', mnc: '06',  iso: 'pk' },
    { key: 'zong_pk',         name: 'Zong',           mcc: '410', mnc: '04',  iso: 'pk' },
    { key: 'grameenphone_bd', name: 'Grameenphone',   mcc: '470', mnc: '01',  iso: 'bd' },
    { key: 'robi_bd',         name: 'Robi',           mcc: '470', mnc: '02',  iso: 'bd' },
    { key: 'ais_th',          name: 'AIS',            mcc: '520', mnc: '03',  iso: 'th' },
    { key: 'truemove_th',     name: 'TrueMove H',     mcc: '520', mnc: '00',  iso: 'th' },
    { key: 'dtac_th',         name: 'dtac',           mcc: '520', mnc: '18',  iso: 'th' },
    { key: 'viettel_vn',      name: 'Viettel',        mcc: '452', mnc: '04',  iso: 'vn' },
    { key: 'vinaphone_vn',    name: 'Vinaphone',      mcc: '452', mnc: '02',  iso: 'vn' },
    { key: 'mobifone_vn',     name: 'Mobifone',       mcc: '452', mnc: '01',  iso: 'vn' },
    { key: 'globe_ph',        name: 'Globe',          mcc: '515', mnc: '02',  iso: 'ph' },
    { key: 'smart_ph',        name: 'Smart',          mcc: '515', mnc: '03',  iso: 'ph' },
    { key: 'maxis_my',        name: 'Maxis',          mcc: '502', mnc: '12',  iso: 'my' },
    { key: 'celcom_my',       name: 'Celcom',         mcc: '502', mnc: '13',  iso: 'my' },
    { key: 'digi_my',         name: 'Digi',           mcc: '502', mnc: '16',  iso: 'my' },
    { key: 'singtel_sg',      name: 'Singtel',        mcc: '525', mnc: '01',  iso: 'sg' },
    { key: 'starhub_sg',      name: 'StarHub',        mcc: '525', mnc: '05',  iso: 'sg' },
    { key: 'csl_hk',          name: 'CSL',            mcc: '454', mnc: '00',  iso: 'hk' },
    { key: 'three_hk',        name: 'Three',          mcc: '454', mnc: '03',  iso: 'hk' },
    { key: 'chunghwa_tw',     name: 'Chunghwa',       mcc: '466', mnc: '92',  iso: 'tw' },
    { key: 'twm_tw',          name: 'Taiwan Mobile',  mcc: '466', mnc: '97',  iso: 'tw' },
    { key: 'dialog_lk',       name: 'Dialog',         mcc: '413', mnc: '02',  iso: 'lk' },
    { key: 'ncell_np',        name: 'Ncell',          mcc: '429', mnc: '02',  iso: 'np' },
    { key: 'kcell_kz',        name: 'Kcell',          mcc: '401', mnc: '02',  iso: 'kz' },
    { key: 'telstra_au',      name: 'Telstra',        mcc: '505', mnc: '01',  iso: 'au' },
    { key: 'optus_au',        name: 'Optus',          mcc: '505', mnc: '02',  iso: 'au' },
    { key: 'vodafone_au',     name: 'Vodafone',       mcc: '505', mnc: '03',  iso: 'au' },
    { key: 'spark_nz',        name: 'Spark',          mcc: '530', mnc: '05',  iso: 'nz' },
    { key: 'vodafone_nz',     name: 'One NZ',         mcc: '530', mnc: '01',  iso: 'nz' },
  ];
  let carrierModelsCache = null;
  async function getCarrierModels() {
    if (carrierModelsCache) return carrierModelsCache;
    if (!hasBridge()) { carrierModelsCache = BUILTIN_CARRIERS.slice(); return carrierModelsCache; }
    try {
      const arr = JSON.parse(await execCommand(`cat ${SIM_DIR}/manifest.json`));
      const models = Array.isArray(arr) ? arr.filter(m => m && m.key && m.name) : [];
      carrierModelsCache = models.length ? models : BUILTIN_CARRIERS.slice();
    } catch (e) { carrierModelsCache = BUILTIN_CARRIERS.slice(); }
    return carrierModelsCache;
  }
  function carrierModelName(key) {
    if (!key) return '';
    const m = (carrierModelsCache || BUILTIN_CARRIERS).find(x => x.key === key);
    return m ? m.name : key;
  }
  function carrierKeyFromName(name) {
    return String(name || '').trim().toLowerCase()
      .replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 40);
  }
  /* Validate + build a carrier manifest entry. name required; MCC = 3 digits, MNC =
     2-3 digits, ISO = 2 letters. Throws a short code on bad input (mapped in the picker). */
  function buildCarrierEntry(o) {
    const name = String(o.name || '').trim();
    const mcc  = String(o.mcc || '').trim();
    const mnc  = String(o.mnc || '').trim();
    const iso  = String(o.iso || '').trim().toLowerCase();
    if (!name)                    throw new Error('name_required');
    if (!/^\d{3}$/.test(mcc))     throw new Error('bad_mcc');
    if (!/^\d{2,3}$/.test(mnc))   throw new Error('bad_mnc');
    if (!/^[a-z]{2}$/.test(iso))  throw new Error('bad_iso');
    const key = carrierKeyFromName(name + '_' + mcc + mnc);
    if (!key)                     throw new Error('bad_name');
    return { key, entry: { key, name, mcc, mnc, iso } };
  }
  /* Add a custom carrier → appends to SIM/manifest.json. Throws 'exists' on dup key. */
  async function importCarrierModel(o) {
    const { key, entry } = buildCarrierEntry(o || {});
    const models = (await getCarrierModels()).slice();
    if (models.some(m => m.key === key)) throw new Error('exists');
    if (!hasBridge()) {                       // preview: session-only
      models.push(entry); carrierModelsCache = models; previewUnsaved = true;
      return { ...entry, preview: true };
    }
    const next = models.concat([entry]);
    const mf = `${SIM_DIR}/manifest.json`;
    await execCommand(`mkdir -p ${SIM_DIR}`);
    await execCommand(`echo '${b64(JSON.stringify(next, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try {
      await execCommand(`chmod 0644 ${mf}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${mf}`);
    } catch (e) { console.warn('sim manifest chmod/chcon:', e); }
    carrierModelsCache = next;
    return entry;
  }
  /* Edit an existing carrier in place. KEY is immutable (renaming would orphan every
     package carrying sim=<key>) — only the display/spoof fields change. Throws
     'not_found' if the key is gone. Mirrors updateGpuModel. */
  async function updateCarrierModel(key, o) {
    key = String(key || '').trim();
    if (!key)                           throw new Error('not_found');
    const built = buildCarrierEntry(o || {});
    const entry = { ...built.entry, key };     // keep the ORIGINAL key (immutable)
    const models = (await getCarrierModels()).slice();
    const idx = models.findIndex(m => m.key === key);
    if (idx < 0)                        throw new Error('not_found');
    models[idx] = entry;
    if (!hasBridge()) { carrierModelsCache = models; previewUnsaved = true; return { ...entry, preview: true }; }
    const mf = `${SIM_DIR}/manifest.json`;
    await execCommand(`mkdir -p ${SIM_DIR}`);
    await execCommand(`echo '${b64(JSON.stringify(models, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try {
      await execCommand(`chmod 0644 ${mf}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${mf}`);
    } catch (e) { console.warn('sim manifest chmod/chcon:', e); }
    carrierModelsCache = models;
    return entry;
  }
  /* Delete a carrier profile. Native is fail-safe — a sim=<key> that no longer resolves
     just no-ops (no forge / no inject). */
  async function deleteCarrierModel(key) {
    if (!key) return;
    const models = (await getCarrierModels()).filter(m => m.key !== key);
    if (!hasBridge()) { carrierModelsCache = models; previewUnsaved = true; return; }
    const mf = `${SIM_DIR}/manifest.json`;
    await execCommand(`echo '${b64(JSON.stringify(models, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try { await execCommand(`chmod 0644 ${mf}`); await execCommand(`chcon u:object_r:system_file:s0 ${mf}`); } catch (e) {}
    carrierModelsCache = models;
  }

  /* ─── Per-app PROXY (proxy=<key>) — PRO, kernel TPROXY via hev-socks5-tproxy ───
     A proxy profile = {key,name,type,host,port,user,pass} in PROXY/manifest.json (like SIM). The
     `proxy=<key>` tag routes just that app through the remote proxy. The root daemon (copg_proxy.sh)
     reads a FLAT file this layer writes on save — /data/adb/COPG/proxy/active.conf, one line per
     proxied app: "<pkg> <type> <host> <port> <user> <pass>" (user/pass '-' = none). Only SOCKS5 for
     now (hev is SOCKS5-upstream). Proxies are user-specific → no built-ins. */
  const PROXY_DIR = MODULE_DIR + '/PROXY';
  const PROXY_RUN = '/data/adb/COPG/proxy';           // daemon runtime dir (active.conf lives here)
  let proxyModelsCache = null;
  function proxyKeyFromName(name) {
    const k = String(name || '').trim().toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
    return k ? k.slice(0, 40) : '';
  }
  async function getProxyModels() {
    if (proxyModelsCache) return proxyModelsCache;
    if (!hasBridge()) { proxyModelsCache = []; return proxyModelsCache; }
    try {
      const arr = JSON.parse(await execCommand(`cat ${PROXY_DIR}/manifest.json`));
      proxyModelsCache = Array.isArray(arr) ? arr.filter(m => m && m.key && m.host) : [];
    } catch (e) { proxyModelsCache = []; }
    return proxyModelsCache;
  }
  function proxyModelName(key) {
    if (!key) return '';
    const m = (proxyModelsCache || []).find(x => x.key === key);
    return m ? m.name : key;
  }
  const proxyOf = pkg => { for (const t of tagsOf(pkg)) if (t.indexOf('proxy=') === 0) return t.slice(6); return ''; };
  function setProxy(pkg, key) {
    const t = tagsOf(pkg).filter(x => x.indexOf('proxy=') !== 0);
    if (key) t.push('proxy=' + key);
    return join(clean(pkg), t);
  }
  function buildProxyEntry(o) {
    const name = String(o.name || '').trim();
    let type = String(o.type || 'socks5').trim().toLowerCase();
    if (type !== 'http' && type !== 'socks4') type = 'socks5';   // socks5 | http | socks4 (hev upstream)
    const host = String(o.host || '').trim();
    const port = String(o.port || '').trim();
    const user = String(o.user || '').trim();
    const pass = String(o.pass || '').trim();
    if (!name)                                     throw new Error('name_required');
    if (!host || /\s/.test(host))                  throw new Error('bad_host');
    if (!/^\d{1,5}$/.test(port) || +port < 1 || +port > 65535) throw new Error('bad_port');
    const key = proxyKeyFromName(name);
    if (!key)                                      throw new Error('bad_name');
    return { key, entry: { key, name, type, host, port, user, pass } };
  }
  async function writeManifestProxy(models) {
    const mf = `${PROXY_DIR}/manifest.json`;
    await execCommand(`mkdir -p ${PROXY_DIR}`);
    await execCommand(`echo '${b64(JSON.stringify(models, null, 2) + '\n')}' | base64 -d > ${mf}`);
    try { await execCommand(`chmod 0644 ${mf}`); await execCommand(`chcon u:object_r:system_file:s0 ${mf}`); } catch (e) { console.warn('proxy manifest chmod/chcon:', e); }
  }
  async function importProxyModel(o) {
    const { key, entry } = buildProxyEntry(o || {});
    const models = (await getProxyModels()).slice();
    if (models.some(m => m.key === key)) throw new Error('exists');
    if (!hasBridge()) { models.push(entry); proxyModelsCache = models; previewUnsaved = true; return { ...entry, preview: true }; }
    const next = models.concat([entry]);
    await writeManifestProxy(next);
    proxyModelsCache = next;
    return entry;
  }
  async function updateProxyModel(key, o) {
    key = String(key || '').trim();
    if (!key) throw new Error('not_found');
    const built = buildProxyEntry(o || {});
    const entry = { ...built.entry, key };             // key immutable (renaming orphans proxy=<key>)
    const models = (await getProxyModels()).slice();
    const idx = models.findIndex(m => m.key === key);
    if (idx < 0) throw new Error('not_found');
    models[idx] = entry;
    if (!hasBridge()) { proxyModelsCache = models; previewUnsaved = true; return { ...entry, preview: true }; }
    await writeManifestProxy(models);
    proxyModelsCache = models;
    return entry;
  }
  async function deleteProxyModel(key) {
    if (!key) return;
    const models = (await getProxyModels()).filter(m => m.key !== key);
    if (!hasBridge()) { proxyModelsCache = models; previewUnsaved = true; return; }
    await writeManifestProxy(models);
    proxyModelsCache = models;
  }
  /* Write the daemon's flat active.conf from every proxy-tagged package + touch it so the watch
     loop reloads within ~3s (no reboot). Called from save() on device. PRO — the caller only
     reaches here when licensed; on the free tier the modal never sets a proxy= tag. */
  async function writeActiveConf() {
    if (!hasBridge()) return;
    const models = await getProxyModels();
    const byKey = {}; models.forEach(m => { byKey[m.key] = m; });
    const lines = [];
    for (const p of listPackages()) {
      if (!p.proxy) continue;
      const m = byKey[p.proxy]; if (!m) continue;      // key gone → skip (daemon no-ops anyway)
      const u = (m.user && m.user.length) ? m.user : '-';
      const pw = (m.pass && m.pass.length) ? m.pass : '-';
      lines.push([p.clean, m.type || 'socks5', m.host, m.port, u, pw].join(' '));
    }
    const body = lines.join('\n') + '\n';
    try {
      await execCommand(`mkdir -p ${PROXY_RUN}; chmod 0700 ${PROXY_RUN} 2>/dev/null; echo '${b64(body)}' | base64 -d > ${PROXY_RUN}/active.conf; chmod 0600 ${PROXY_RUN}/active.conf`);
    } catch (e) { console.warn('writeActiveConf:', e); }
  }

  /* ─── Android ↔ SDK mapping (from old.js) ─── */
  const androidToSdk = {
    '10': 29, '10.0': 29, '11': 30, '11.0': 30, '12': 31, '12.0': 31,
    '12L': 32, '12.1': 32, '13': 33, '13.0': 33, '14': 34, '14.0': 34,
    '15': 35, '15.0': 35, '16': 36, '16.0': 36, '17': 37, '17.0': 37,
  };
  const sdkToAndroid = (() => {
    const m = {};
    for (const [a, s] of Object.entries(androidToSdk)) m[s] = a.split('.')[0];
    return m;
  })();
  function sdkFromAndroid(v) {
    const c = String(v).replace(/[^0-9.L]/g, '');
    if (androidToSdk[c] != null) return androidToSdk[c];
    const main = c.split('.')[0];
    if (androidToSdk[main] != null) return androidToSdk[main];
    if (c.endsWith('L')) { const w2 = c.slice(0, -1); if (androidToSdk[w2] != null) return androidToSdk[w2]; }
    return null;
  }
  function androidFromSdk(v) {
    const c = String(v).replace(/[^0-9]/g, '');
    if (sdkToAndroid[c] != null) return sdkToAndroid[c];
    for (const [a, s] of Object.entries(androidToSdk)) if (String(s) === c) return a.split('.')[0];
    return null;
  }

  /* ─── Internal state ─── */
  let config   = {};   // parsed COPG.json
  let keyOrder = [];   // key insertion order
  let names    = {};   // list.json: cleanPkg (or "pkg@u<id>" per-clone) -> custom display name
  let userAppNames = null;   // Map<idPackage, system desktop label incl. clone suffix> (probe launcher label)
  let previewUnsaved = false;   // browser-preview edits not persisted

  /* ─── Device key derivation ─── */
  const deviceKeyFromName = name => `PACKAGES_${name.trim().toUpperCase().replace(/ /g, '_')}_DEVICE`;
  const pkgKeyOf = deviceKey => deviceKey.replace(/_DEVICE$/, '');

  /* ─── Load ─── */
  async function readFilePreview(rel) {
    // Browser preview: fetch the JSON shipped alongside the WebUI.
    try {
      const res = await fetch(rel, { cache: 'no-store' });
      if (res.ok) return await res.text();
    } catch (_) { /* fall through */ }
    // file:// fallback via XHR
    try {
      return await new Promise((resolve, reject) => {
        const x = new XMLHttpRequest();
        x.open('GET', rel, true);
        x.onreadystatechange = () => {
          if (x.readyState === 4) {
            if (x.status === 200 || x.status === 0) resolve(x.responseText || '');
            else reject(`HTTP ${x.status}`);
          }
        };
        x.send();
      });
    } catch (_) { return ''; }
  }

  async function loadAll() {
    let cfgText = '', listText = '';
    if (hasBridge()) {
      try { cfgText  = await execCommand(`cat ${CONFIG_PATH}`); } catch (_) {}
      try { listText = await execCommand(`cat ${LIST_PATH}`);   } catch (_) {}
    } else {
      // webroot/ is module/webroot/; the JSON lives one level up at module/COPG.json
      cfgText  = await readFilePreview('../COPG.json');
      listText = await readFilePreview('../list.json');
    }
    try { config = cfgText ? JSON.parse(cfgText) : {}; }
    catch (_) { config = {}; }
    try { names = listText ? JSON.parse(listText) : {}; }
    catch (_) { names = {}; }
    keyOrder = Object.keys(config);
    ensureThisDevice();      // built-in identity profile (auto-created)
    ensureRandom();          // built-in "Random" bucket (auto-created, right after This Device)
    migrateLegacyCpu();      // fold any legacy cpu_only/blacklist → This Device + tags, drop cpu_spoof
    try { await loadGlobalConfig(); } catch (_) {}   // device-wide hooks (global.json)
    return { ok: true };
  }

  // ── global.json — device-wide (NOT per-app) hooks. Currently the system-wide IMEI that
  //    hooks com.android.phone. Kept in its OWN file so COPG.json stays per-app-clean. Shape:
  //    { "imei": "<15 digits>", "imei2": "<slot-1, optional>" }. Empty = keep the real IMEI.
  const GLOBAL_PATH = MODULE_DIR + '/global.json';
  const FLAGSECURE_LIVE = '/data/system/copg_flagsecure.txt';
  const STORAGE_LIVE = '/data/system/copg_storage.txt';
  const COREPATCH_LIVE = '/data/system/copg_corepatch.txt';
  const MICSHARE_LIVE = '/data/system/copg_micshare';   // audioserver mic-share hook polls this ("1"/"0")
  function emptyCorepatch() { return { sig: false, downgrade: false, verifier: false, arsc: false, prevsig: false, hiddenapi: false, block: false }; }
  const CP_KEYS = ['sig', 'downgrade', 'verifier', 'arsc', 'prevsig', 'hiddenapi', 'block'];  // "SDVAPHB" bit order
  // Live kernel boot_id — a fresh UUID each boot, CONSTANT within a boot. Used to tell whether an
  // ACTUAL reboot has happened since the user toggled the system_server master (see sysserverArmed).
  // Same signal the Hook Status board reads (getSystemHooks). Cached — it never changes mid-boot.
  let _bootId = null;
  async function bootId() {
    if (_bootId != null) return _bootId;
    if (!hasBridge()) { _bootId = ''; return _bootId; }
    try { _bootId = ((await execCommand('cat /proc/sys/kernel/random/boot_id 2>/dev/null')) || '').trim(); }
    catch (_) { _bootId = ''; }
    return _bootId;
  }
  // REAL system_server liveness snapshot, from the native heartbeat /data/system/copg_ss_hb (epoch,
  // rewritten every 30s ONLY while the sysserver spoof layer is armed + running). This is strictly
  // better than the boot_id guess for "is it actually working": true = armed + alive, false = armed
  // intent but NOT running (dead/crashed/SDK-unsupported), null = no heartbeat info (older .so →
  // callers fall back to the boot_id reboot-since-toggle heuristic). Snapshot is a boolean (not a
  // decaying timestamp), refreshed on every loadGlobalConfig / getSystemHooks.
  const SS_HB_STALE_SEC = 120;   // 4× the 30s native cadence
  let _ssAlive = null;
  async function readSsAlive() {
    if (!hasBridge()) { _ssAlive = null; return _ssAlive; }
    try {
      const ts = parseInt(((await execCommand('cat /data/system/copg_ss_hb 2>/dev/null')) || '').trim(), 10);
      _ssAlive = (!isNaN(ts) && ts > 0) ? ((Math.floor(Date.now() / 1000) - ts) < SS_HB_STALE_SEC) : null;
    } catch (_) { _ssAlive = null; }
    return _ssAlive;
  }
  let globalConfig = { enabled: false, imei: '', imei2: '', flagsecure: false, storage: false, corepatch: emptyCorepatch(), micshare: false, sysserver: false, ss_boot: '' };
  function getGlobalConfig() { return globalConfig; }
  /* "Armed" = the system_server spoof layer is ACTUALLY running this boot — the gate every
     sysserver-backed control (the master chip, FLAG_SECURE, Install Patches, the per-app rows) uses
     to decide greyed-vs-live. Authoritative source is the native heartbeat (_ssAlive): true=alive,
     false=armed-intent-but-not-running (reboot pending, crashed, or SDK-unsupported). Only when the
     device predates the heartbeat (_ssAlive null) do we fall back to the boot_id "rebooted since the
     toggle" heuristic. This is why it's better than boot_id alone: boot_id can't tell a successful
     arm from a failed one; the heartbeat can. */
  function sysserverArmed() {
    const c = globalConfig;
    if (!c || !c.sysserver) return false;      // master off → nothing armed
    if (!hasBridge()) return true;             // preview has no reboot concept → reflect intent
    if (_ssAlive !== null) return _ssAlive;    // REAL liveness (current .so) — authoritative
    if (!c.ss_boot) return true;               // fallback, legacy config w/o ss_boot → assume rebooted
    return !!_bootId && c.ss_boot !== _bootId;  // fallback (older .so): reboot-since-toggle heuristic
  }
  function sysserverPending() { return !!(globalConfig && globalConfig.sysserver) && !sysserverArmed(); }
  async function loadGlobalConfig() {
    let txt = '';
    if (hasBridge()) { try { txt = await execCommand(`cat ${GLOBAL_PATH}`); } catch (_) {} }
    else            { txt = await readFilePreview('../global.json'); }
    let o = {};
    try { o = txt ? JSON.parse(txt) : {}; } catch (_) { o = {}; }
    const imei = (o && o.imei) || '';
    // Explicit flag; old files without it → on iff an IMEI is present (back-compat).
    const enabled = (o && typeof o.imei_enabled === 'boolean') ? o.imei_enabled : !!imei;
    // global.json holds the persisted INTENT (native defaults every one of these OFF when the key is
    // absent). The live toggle file (/data/system/copg_*.txt) is the ACTUAL armed state, which a
    // boot-guard trip can pull to OFF for a feature whose intent is ON. The live file may ONLY
    // DOWNGRADE — it must never show a feature ON that global.json has OFF. A stale live '1' (left
    // from a prior enable, after the intent was cleared without a reboot to reseed the file) was
    // making the WebUI show phantom-ON for FLAG_SECURE / No-Storage. So honour a live '0' against a
    // true intent (reflect a trip); ignore a live '1' over a false intent (the bug).
    let flagsecure = !!(o && o.flagsecure);
    let storage = !!(o && o.storage);
    let micshare = !!(o && o.micshare);
    if (hasBridge()) {
      try {
        const live = (await execCommand(`cat ${FLAGSECURE_LIVE} 2>/dev/null`) || '').trim();
        if (flagsecure && live === '0') flagsecure = false;
      } catch (_) {}
      try {
        const sl = (await execCommand(`cat ${STORAGE_LIVE} 2>/dev/null`) || '').trim();
        if (storage && sl === '0') storage = false;
      } catch (_) {}
      try {
        const ml = (await execCommand(`cat ${MICSHARE_LIVE} 2>/dev/null`) || '').trim();
        if (micshare && ml === '0') micshare = false;
      } catch (_) {}
    }
    // CorePatch (install-time PM patches): persisted intent in global.json.corepatch{sig,downgrade,
    // verifier,arsc}; the live file copg_corepatch.txt ("SDVA" bitline) is authoritative for the
    // ACTUAL armed state (incl. a boot-guard trip), same model as flagsecure/storage.
    const co = (o && typeof o.corepatch === 'object' && o.corepatch) ? o.corepatch : {};
    let corepatch = {}; CP_KEYS.forEach(k => corepatch[k] = !!co[k]);
    if (hasBridge()) {
      try {
        const cl = (await execCommand(`cat ${COREPATCH_LIVE} 2>/dev/null`) || '').trim();
        // Per-bit, the live bitline can only DOWNGRADE (a boot-guard trip turns a patch off); it must
        // never enable a bit global.json has off — same stale-'1' phantom-on guard as above.
        if (cl.length >= CP_KEYS.length) CP_KEYS.forEach((k, i) => { if (corepatch[k] && cl[i] === '0') corepatch[k] = false; });
      } catch (_) {}
    }
    // System_server-spoofs master switch: persisted intent only (native reads it at BOOT — no live
    // file, since enabling requires a reboot). Default OFF (absent → false) so nothing arms out of the box.
    const sysserver = !!(o && o.sysserver);
    // ss_boot: the boot_id stamped when the master was last toggled (WebUI-only marker; native ignores
    // it). Prime the live boot_id now so sysserverArmed() is correct from first paint.
    const ss_boot = (o && typeof o.ss_boot === 'string') ? o.ss_boot : '';
    if (hasBridge()) { try { await bootId(); } catch (_) {} try { await readSsAlive(); } catch (_) {} }
    globalConfig = { enabled, imei, imei2: (o && o.imei2) || '', flagsecure, storage, corepatch, micshare, sysserver, ss_boot };
    return globalConfig;
  }
  // Merge-preserving: overlay only the keys `cfg` provides so an IMEI-only save never drops
  // flagsecure and vice-versa (both live in the same global.json).
  async function saveGlobalConfig(cfg) {
    cfg = cfg || {};
    // Stamp the current boot_id whenever the master is (re)set, so dependents can tell a later reboot
    // apart from this same-boot toggle. A non-sysserver save (flagsecure/storage/…) preserves it.
    const ss_boot = ('sysserver' in cfg) ? await bootId() : (globalConfig.ss_boot || '');
    globalConfig = {
      enabled:    ('enabled'    in cfg) ? !!cfg.enabled            : globalConfig.enabled,
      imei:       ('imei'       in cfg) ? String(cfg.imei || '').trim()  : globalConfig.imei,
      imei2:      ('imei2'      in cfg) ? String(cfg.imei2 || '').trim() : globalConfig.imei2,
      flagsecure: ('flagsecure' in cfg) ? !!cfg.flagsecure         : globalConfig.flagsecure,
      storage:    ('storage'    in cfg) ? !!cfg.storage            : globalConfig.storage,
      micshare:   ('micshare'   in cfg) ? !!cfg.micshare           : globalConfig.micshare,
      corepatch:  ('corepatch'  in cfg) ? (() => { const o = {}; CP_KEYS.forEach(k => o[k] = !!(cfg.corepatch && cfg.corepatch[k])); return o; })()
                                        : (globalConfig.corepatch || emptyCorepatch()),
      sysserver:  ('sysserver'  in cfg) ? !!cfg.sysserver          : globalConfig.sysserver,
      ss_boot:    ss_boot,
    };
    if (!hasBridge()) { previewUnsaved = true; return { saved: false, preview: true }; }
    // Native reads imei_enabled + imei/imei2 + flagsecure + storage + corepatch + sysserver; values kept in-file even when off.
    const body = JSON.stringify(
      { imei_enabled: globalConfig.enabled, imei: globalConfig.imei, imei2: globalConfig.imei2,
        flagsecure: globalConfig.flagsecure, storage: globalConfig.storage,
        corepatch: globalConfig.corepatch, micshare: globalConfig.micshare, sysserver: globalConfig.sysserver,
        ss_boot: globalConfig.ss_boot },
      null, 2) + '\n';
    await execCommand(`echo '${b64(body)}' | base64 -d > ${GLOBAL_PATH}`);
    try {
      await execCommand(`chmod 0644 ${GLOBAL_PATH}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${GLOBAL_PATH}`);
      // The resident FlagSecureHook in system_server re-reads the live file every ~1.5s, so writing
      // it here flips the feature with NO reboot (system_server never re-forks). Best-effort; if
      // /data/system isn't writable from the WebUI domain the boot-time seed still applies it.
      await execCommand(`echo ${globalConfig.flagsecure ? '1' : '0'} > ${FLAGSECURE_LIVE} 2>/dev/null; chmod 0644 ${FLAGSECURE_LIVE} 2>/dev/null; true`);
      // Same live-toggle for No-Storage-Restrict: StorageHook (resident in com.android.externalstorage,
      // injected passthrough at boot) re-reads copg_storage.txt every ~1.5s → flips with no reboot.
      await execCommand(`echo ${globalConfig.storage ? '1' : '0'} > ${STORAGE_LIVE} 2>/dev/null; chmod 0644 ${STORAGE_LIVE} 2>/dev/null; true`);
      // Concurrent Mic: the audioserver mic-share hook polls this file → flips live with no reboot.
      await execCommand(`echo ${globalConfig.micshare ? '1' : '0'} > ${MICSHARE_LIVE} 2>/dev/null; chmod 0644 ${MICSHARE_LIVE} 2>/dev/null; true`);
      // Same live-toggle for CorePatch: CorePatchHook (resident in system_server) re-reads the "SDVA"
      // bitline every ~1.5s → the 4 patches flip with no reboot.
      { const c = globalConfig.corepatch || emptyCorepatch();
        const bits = CP_KEYS.map(k => c[k] ? '1' : '0').join('');
        await execCommand(`echo ${bits} > ${COREPATCH_LIVE} 2>/dev/null; chmod 0644 ${COREPATCH_LIVE} 2>/dev/null; true`); }
      // reloadIfNeeded() in the .so is gated on COPG.json's mtime, so a global.json-only edit
      // wouldn't reach the next fork without bumping it. (com.android.phone is persistent →
      // the user still reboots / kills it to re-run the hook, but this makes NEW forks correct.)
      await execCommand(`touch ${CONFIG_PATH}`);
    } catch (_) {}
    return { saved: true };
  }
  // Random Luhn-valid 15-digit IMEI (14 random digits + check digit) for the Generate button.
  function genImei() {
    let p = '';
    for (let i = 0; i < 14; i++) p += Math.floor(Math.random() * 10);
    return p + String(luhnCheckDigit(p));
  }
  // Re-specialize the (persistent) telephony process so a global.json change applies WITHOUT a
  // reboot: it respawns from zygote → spoof_module re-runs → the global IMEI hook reinstalls.
  async function restartTelephony() {
    if (!hasBridge()) return { ok: false, preview: true };
    try { await execCommand('killall com.android.phone 2>/dev/null; true'); return { ok: true }; }
    catch (e) { return { ok: false, error: String(e) }; }
  }

  /* ─── Selectors ─── */
  // Display name for a package, per (package, user-scope). A clone (userScope>0) gets its OWN name
  // key (idPackage → "pkg@u<id>"), so renaming one clone never renames the others; owner / all-users
  // use the base key. Order: custom (list.json, per-clone) → the system desktop name incl. the clone
  // number suffix (userAppNames, from the probe's launcher label) → base list.json name → package.
  const nameFor = (c, userScope) => names[idPackage(c, userScope)]
      || (userAppNames && userAppNames.get(idPackage(c, userScope)))
      || names[c] || c;

  function listDevices() {
    const out = [];
    for (const key of keyOrder) {
      if (key.endsWith('_DEVICE') && config[key]) {
        const props   = config[key];
        const pkgKey  = pkgKeyOf(key);
        const games   = Array.isArray(config[pkgKey]) ? config[pkgKey] : [];
        const identity = key === THIS_DEVICE_DKEY;
        const random   = key === RANDOM_DKEY;
        out.push({
          key, pkgKey, identity, random,
          name:  deviceLabel(key, props),
          model: (identity || random) ? '' : (props.MODEL || 'Unknown'),
          brand: (identity || random) ? '' : (props.BRAND || ''),
          props,
          gameCount: games.length,
        });
      }
    }
    return out;
  }

  /* Flat package list across all device profiles (incl. "This Device"), deduped by
     clean name, in keyOrder. Every package is a device package now. */
  function listPackages() {
    const out = [];
    const seen = new Set();
    const push = (raw, type, deviceKey, deviceName) => {
      const c = clean(raw);
      const us = userScopeOf(raw);            // '' = all users
      // De-dup per (package, user-scope) — the SAME package may legitimately appear on
      // two device profiles with different u= scopes (one per user), and BOTH must show.
      const dedupe = c + '|u=' + us;
      if (seen.has(dedupe)) return;
      seen.add(dedupe);
      out.push({
        clean: c, raw, type, deviceKey: deviceKey || null,
        deviceName: deviceName || null,
        userScope: us,                        // '' all users | '0' owner | '10'/'999'/… a clone
        tags: tagsOf(raw),
        with_cpu: hasTag(raw, 'with_cpu') || !!cpuModelOf(raw),   // cpu=<key> alone = CPU spoof on
        blocked: hasTag(raw, 'blocked'),
        cow: hasTag(raw, 'cow'),
        aid: hasTag(raw, 'aid') || !!aidLiteralOf(raw),   // bare aid (derive) OR aid=<hex> (literal)
        aidLiteral: aidLiteralOf(raw),                    // exact 16-hex id, '' = derive from seed
        serial: hasTag(raw, 'serial') || !!serialLiteralOf(raw),  // bare serial (derive) OR serial=<value> (literal); per-app Build.SERIAL, FREE
        serialLiteral: serialLiteralOf(raw),              // exact serial, '' = derive from package
        gaid: hasTag(raw, 'gaid') || !!gaidLiteralOf(raw),  // bare gaid (derive) OR gaid=<uuid> (literal); PRO resident
        gaidLiteral: gaidLiteralOf(raw),                    // exact UUID, '' = derive from seed
        appset: hasTag(raw, 'appset') || !!appsetLiteralOf(raw),  // bare appset (derive) OR appset=<uuid> (literal); PRO resident
        appsetLiteral: appsetLiteralOf(raw),                // exact UUID, '' = derive from package
        hideDev: hasTag(raw, 'hidedev'),   // hide USB-debugging + dev-options (stealth cache-forge, PRO)
        randid: hasTag(raw, 'randid'),     // churn the per-app derived IDs on every launch (PRO, stealth); device-random is the "Random" bucket
        cpuModel: cpuModelOf(raw),
        gpu: !!gpuModelOf(raw),          // reference: gpu=<key> present → GPU spoof on
        gpuModel: gpuModelOf(raw),
        timezone: timezoneOf(raw),       // tz=<IANA zone> per-app timezone (FREE, stealth), '' = off
        locale: localeOf(raw),           // lang=<BCP-47> per-app locale (FREE, stealth, system-side), '' = off
        wallpaper: hasWallpaper(raw),    // wallpaper[=<id>] per-app wallpaper-id spoof (PRO, stealth, system-side)
        wallpaperId: wallpaperOf(raw),   // the pinned id ('' = bare tag → -1)
        account: hasAccount(raw),        // account[=<email>] per-app google-account spoof (PRO, stealth, system-side)
        accountEmail: accountEmailOf(raw), // forged email ('' = bare tag → hide / empty list)
        webua: uaKeyOf(raw),                // ua=<key> per-app WebView User-Agent (PRO, resident), '' = off

        mock: hasTag(raw, 'mock'),       // resident LSPlant Location.isMock() → false (PRO)
        webfp: hasTag(raw, 'webfp') || !!webfpSeedOf(raw),  // bare webfp (derive) OR webfp=<seed> (pinned); PRO resident
        sensor: hasTag(raw, 'sensor'),   // per-app sensor-roster spoof (PRO, resident v7878)
        getprop: hasTag(raw, 'getprop'), // child `getprop` returns the spoofed props (PRO, resident libc execve hook)
        webfpSeed: webfpSeedOf(raw),     // exact canvas seed (hex), '' = derive from package
        gps: !!gpsOf(raw),               // gps=<lat>,<lng> resident v7878 Location coordinate overwrite (PRO)
        gpsLat: gpsOf(raw) ? gpsOf(raw).lat : '',   // fake latitude, '' = off
        gpsLng: gpsOf(raw) ? gpsOf(raw).lng : '',   // fake longitude, '' = off
        cell: !!cellOf(raw),             // cell=<lac>,<ci>[,<mcc>,<mnc>] resident v7878 cell overwrite (PRO)
        cellLac: cellOf(raw) ? cellOf(raw).lac : '',   // fake LAC/TAC (decimal), '' = off
        cellCi: cellOf(raw) ? cellOf(raw).ci : '',     // fake CID/CI/NCI (decimal), '' = off
        cellMcc: cellOf(raw) ? cellOf(raw).mcc : '',   // fake MCC (3 digits), '' = keep real
        cellMnc: cellOf(raw) ? cellOf(raw).mnc : '',   // fake MNC (2-3 digits), '' = keep real
        uptime: uptimeOf(raw) > 0,       // uptime=<sec> resident clock_gettime/sysinfo hook (PRO)
        uptimeSeconds: uptimeOf(raw),    // fake uptime in seconds, 0 = off
        bootid: bootidOf(raw).on,            // bootid/bootid=rnd/bootid=<uuid> per-app boot_id spoof (PRO)
        bootidChurn: bootidOf(raw).churn,    // true = fresh UUID each launch (bootid=rnd, "fake reboot")
        bootidLiteral: bootidOf(raw).literal,// pinned UUID, '' = derived (stable) or churn
        mac: macOf(raw).on,                  // mac/mac=rnd/mac=<12hex> per-app WiFi MAC spoof (PRO)
        macChurn: macOf(raw).churn,          // true = fresh MAC each launch (mac=rnd)
        macLiteral: macOf(raw).literal,      // pinned 12-hex MAC, '' = derived (stable) or churn
        btmac: btmacOf(raw).on,              // btmac/btmac=rnd/btmac=<12hex> per-app Bluetooth MAC spoof (PRO, stealth)
        btmacChurn: btmacOf(raw).churn,      // true = fresh BT MAC each launch (btmac=rnd)
        btmacLiteral: btmacOf(raw).literal,  // pinned 12-hex BT MAC, '' = derived (stable) or churn
        rr: refreshOf(raw) > 0,          // rr=<hz> resident DisplayManagerGlobal.getDisplayInfo hook (FREE)
        refreshHz: refreshOf(raw),       // spoofed display refresh rate in Hz, 0 = off
        drm: hasTag(raw, 'drm') || !!drmLevelOf(raw) || !!drmIdLiteralOf(raw) || !!drmSysLiteralOf(raw),   // resident LSPlant MediaDrm identity spoof (PRO)
        drmLevel: drmLevelOf(raw),       // drm=<level> pinned securityLevel L1/L2/L3, '' = default L1 (bare :drm)
        drmId: drmIdLiteralOf(raw),      // drmid=<hex> literal deviceUniqueId, '' = derive from package
        drmSys: drmSysLiteralOf(raw),    // drmsys=<int> literal systemId, '' = derive from package
        imei: hasTag(raw, 'imei') || !!imeiLiteralOf(raw) || !!imei2LiteralOf(raw),   // resident LSPlant TelephonyManager IMEI spoof (PRO)
        imeiLiteral: imeiLiteralOf(raw), // imei=<15> literal slot 0, '' = derive from package
        imei2Literal: imei2LiteralOf(raw), // imei2=<15> literal slot 1 (dual-SIM), '' = derive
        imsi: hasTag(raw, 'imsi') || !!imsiLiteralOf(raw) || !!imsi2LiteralOf(raw),   // resident v7878 getSubscriberId spoof (PRO)
        imsiLiteral: imsiLiteralOf(raw),   // imsi=<14-15> literal slot 0, '' = derive
        imsi2Literal: imsi2LiteralOf(raw), // imsi2=<14-15> literal slot 1 (dual-SIM), '' = derive
        iccid: hasTag(raw, 'iccid') || !!iccidLiteralOf(raw) || !!iccid2LiteralOf(raw), // resident v7878 getSimSerialNumber spoof (PRO)
        iccidLiteral: iccidLiteralOf(raw),   // iccid=<19-20> literal slot 0, '' = derive
        iccid2Literal: iccid2LiteralOf(raw), // iccid2=<19-20> literal slot 1 (dual-SIM), '' = derive
        vpns: hasTag(raw, 'vpns'),       // main "Hide VPN" = system_server VPN-hide (FREE, pairip-safe, per-uid; reboot to apply)
        vpnAgg: hasTag(raw, 'vpn') || hasTag(raw, 'vpnn'),   // nested "Aggressive" = resident in-app VPN-hide (FREE, risk-gated)
        playsrc: hasTag(raw, 'playsrc'), // main "Fake Play install source" = system_server install-source spoof (PRO, stealth-from-app; reboot to apply)
        pairip: hasTag(raw, 'pairip'),   // nested "Block PairIP license check" = system_server LicenseActivity block (PRO, stealth-from-app)
        tensorfeat: hasTag(raw, 'tensorfeat'), // "Tensor feature targets" = system_server PMS.hasSystemFeature spoof (PRO, stealth-from-app)
        sim: carrierOf(raw).mode,        // '' | 'safe' | 'aggressive' | 'phantom'
        simS0: carrierOf(raw).s0,        // SIM1 carrier key ('' = real)
        simS1: carrierOf(raw).s1,        // SIM2 carrier key ('' = real)
        proxy: proxyOf(raw),             // proxy=<key> per-app SOCKS5 proxy (PRO, kernel TPROXY), '' = off
        dnd: hasTag(raw, 'dnd'),
        dab: hasTag(raw, 'dab'),
        kso: hasTag(raw, 'kso'),
        nolog: hasTag(raw, 'nolog'),
        dpi: dpiOf(raw),                 // dpi=<n> per-app screen density (controller tweak)
        name: nameFor(c, us),
      });
    };
    for (const key of keyOrder) {
      if (key.startsWith('PACKAGES_') && !key.endsWith('_DEVICE') && Array.isArray(config[key])) {
        const dk = `${key}_DEVICE`;
        const dn = deviceLabel(dk, config[dk]);
        config[key].forEach(p => push(p, 'device', key, dn));
      }
    }
    return out;
  }

  /* Find which list a clean package currently lives in (for duplicate detection). */
  function locate(cleanName, ignoreClean, userScope) {
    // Duplicate detection is per (package, user-scope): the same package on another device
    // for a DIFFERENT user is not a clash (that's the whole point of per-user spoof), so only
    // report an entry with the SAME scope. userScope undefined = match any (legacy callers).
    if (ignoreClean === cleanName) return '';
    const us = userScope === undefined ? null : String(userScope);
    for (const key of keyOrder) {
      if (key.startsWith('PACKAGES_') && !key.endsWith('_DEVICE') && Array.isArray(config[key])) {
        if (config[key].some(p => clean(p) === cleanName && (us === null || userScopeOf(p) === us)))
          return `device "${deviceLabel(`${key}_DEVICE`, config[`${key}_DEVICE`])}"`;
      }
    }
    return '';
  }

  function deviceNameExists(name, model, ignoreKey) {
    for (const [key, val] of Object.entries(config)) {
      if (key.endsWith('_DEVICE') && key !== ignoreKey) {
        if (val.DEVICE === name)  return { field: 'name' };
        if (model && val.MODEL === model) return { field: 'model' };
      }
    }
    return null;
  }

  /* ─── "This Device" identity profile ─────────────────────────────────────
     A built-in device profile that is SPARSE — it carries only the fields the user
     has filled in (empty fields are omitted to keep the JSON small). With no fields
     it spoofs nothing (native skips empty/missing); fill one → it spoofs just that.
     The editor reads each field as `d.FIELD || ''`, so missing keys = empty inputs.
     Its display name is special-cased by key, independent of the DEVICE field.

     KEY NAME: 'PACKAGES_THISDEVICE' — deliberately NOT 'PACKAGES_THIS_DEVICE': the
     latter ends in '_DEVICE', which collides with the pkglist/profile suffix
     convention (would be misread as a device-profile key by webui AND native AND
     the controller). 'THISDEVICE' (no underscore) pairs cleanly: pkglist
     PACKAGES_THISDEVICE + profile PACKAGES_THISDEVICE_DEVICE. */
  const THIS_DEVICE_PKEY = 'PACKAGES_THISDEVICE';
  const THIS_DEVICE_DKEY = 'PACKAGES_THISDEVICE_DEVICE';
  const OLD_TD_PKEY = 'PACKAGES_THIS_DEVICE';        // pre-rename (collision-prone) keys
  const OLD_TD_DKEY = 'PACKAGES_THIS_DEVICE_DEVICE';
  function isThisDeviceKey(k) { return k === THIS_DEVICE_PKEY || k === THIS_DEVICE_DKEY; }

  /* ─── "Random" pseudo-device bucket ──────────────────────────────────────
     A second built-in bucket (same shape as This Device): a package assigned to it
     gets a RANDOM coherent device profile picked fresh on each launch (native draws
     from the other real profiles in the config). The profile object stays EMPTY — it's
     a marker; native keys off the key name PACKAGES_RANDOM. Not a real device → not
     editable/deletable/shareable; its Build fields are never read. Device-randomness
     lives in THIS bucket; ID-randomness is the orthogonal `randid` tag (works on any
     bucket). KEY NAME 'PACKAGES_RANDOM' (no underscore before a _DEVICE-like suffix),
     exactly like THISDEVICE, so it never trips the endsWith('_DEVICE') profile detection. */
  const RANDOM_PKEY = 'PACKAGES_RANDOM';
  const RANDOM_DKEY = 'PACKAGES_RANDOM_DEVICE';
  function isRandomKey(k) { return k === RANDOM_PKEY || k === RANDOM_DKEY; }
  function deviceLabel(dkey, props) {
    if (dkey === THIS_DEVICE_DKEY)
      return (w.I18N && w.I18N.t) ? w.I18N.t('dev_this_device') : 'This Device';
    if (dkey === RANDOM_DKEY)
      return (w.I18N && w.I18N.t) ? w.I18N.t('dev_random') : 'Random';
    // Display name = the friendly NAME (every bundled profile carries one; DEVICE stays the real
    // codename for fingerprint consistency). If a profile has no NAME, fall back to the KEY (which is
    // the name the device was added under), NEVER to DEVICE — DEVICE is a codename, not a label.
    // NAME is a WebUI-only display hint; native ignores it.
    if (props && props.NAME) return props.NAME;
    return dkey.replace('PACKAGES_', '').replace(/_DEVICE$/, '').replace(/_/g, ' ');
  }
  function ensureThisDevice() {
    // migrate the old collision-prone keys → THISDEVICE (preserving packages + order)
    const renameKey = (oldK, newK) => {
      if (config[oldK] === undefined) return;
      if (config[newK] === undefined) {
        config[newK] = config[oldK];
        const i = keyOrder.indexOf(oldK);
        if (i !== -1) keyOrder[i] = newK; else keyOrder.push(newK);
      }
      delete config[oldK];
      keyOrder = keyOrder.filter(k => k !== oldK);
    };
    renameKey(OLD_TD_PKEY, THIS_DEVICE_PKEY);
    renameKey(OLD_TD_DKEY, THIS_DEVICE_DKEY);

    if (!Array.isArray(config[THIS_DEVICE_PKEY])) config[THIS_DEVICE_PKEY] = [];
    // profile is a plain object; strip empty-string fields so the JSON stays minimal
    // (the editor reads each field as `d.FIELD || ''`, so missing = empty input).
    let prof = config[THIS_DEVICE_DKEY];
    if (!prof || typeof prof !== 'object' || Array.isArray(prof)) prof = {};
    for (const k of Object.keys(prof)) if (typeof prof[k] === 'string' && !prof[k].trim()) delete prof[k];
    config[THIS_DEVICE_DKEY] = prof;
    if (!keyOrder.includes(THIS_DEVICE_PKEY) || !keyOrder.includes(THIS_DEVICE_DKEY)) {
      keyOrder = keyOrder.filter(k => k !== THIS_DEVICE_PKEY && k !== THIS_DEVICE_DKEY);
      keyOrder.unshift(THIS_DEVICE_PKEY, THIS_DEVICE_DKEY);   // identity profile first
    }
  }
  /* Built-in "Random" bucket — always present so it's selectable/discoverable in the
     device picker. Empty profile object = marker only (native randomizes by key name).
     Placed right after This Device in keyOrder (both built-in buckets lead the list). */
  function ensureRandom() {
    if (!Array.isArray(config[RANDOM_PKEY])) config[RANDOM_PKEY] = [];
    if (!config[RANDOM_DKEY] || typeof config[RANDOM_DKEY] !== 'object' || Array.isArray(config[RANDOM_DKEY]))
      config[RANDOM_DKEY] = {};
    if (!keyOrder.includes(RANDOM_PKEY) || !keyOrder.includes(RANDOM_DKEY)) {
      keyOrder = keyOrder.filter(k => k !== RANDOM_PKEY && k !== RANDOM_DKEY);
      // after This Device (indices 0,1 once ensureThisDevice has run) → positions 2,3
      const at = keyOrder.includes(THIS_DEVICE_DKEY) ? keyOrder.indexOf(THIS_DEVICE_DKEY) + 1 : 0;
      keyOrder.splice(at, 0, RANDOM_PKEY, RANDOM_DKEY);
    }
  }
  /* One-time fold of the legacy global sets into the unified model:
       cpu_spoof.cpu_only_packages → "This Device" pkg with cpu=<key>
       cpu_spoof.blacklist         → "This Device" pkg with :blocked
     then the sets are emptied. Runs on load; persists on the next save. Native
     still parses the old sets too, so behaviour is identical before/after a save. */
  function migrateLegacyCpu() {
    const cpu = config.cpu_spoof; if (!cpu) return;
    const bl = Array.isArray(cpu.blacklist) ? cpu.blacklist : [];
    const co = Array.isArray(cpu.cpu_only_packages) ? cpu.cpu_only_packages : [];
    if (bl.length || co.length) {
      ensureThisDevice();
      const arr = config[THIS_DEVICE_PKEY];
      const managed = new Set();
      for (const k of keyOrder)
        if (k.startsWith('PACKAGES_') && !k.endsWith('_DEVICE') && Array.isArray(config[k]))
          config[k].forEach(p => managed.add(clean(p)));
      const add = raw => { const c = clean(raw); if (managed.has(c)) return; managed.add(c); arr.push(raw); };
      // cpu_only must keep mounting → ensure a cpu tag (default sd8elite if bare)
      co.forEach(p => { let s = p; if (!cpuModelOf(s) && !hasTag(s, 'with_cpu')) s = setCpuModel(s, 'sd8elite'); add(s); });
      bl.forEach(p => add(addTag(clean(p), 'blocked')));
    }
    delete config.cpu_spoof;                       // unified model: the global set is retired
    keyOrder = keyOrder.filter(k => k !== 'cpu_spoof');
  }

  /* Write the custom-props map into the profile's PROPS (trimmed, blanks dropped).
     form.props undefined → caller doesn't manage props, keep any hand-added PROPS. */
  function applyProps(data, props) {
    if (props === undefined || props === null) return;
    const out = {};
    Object.keys(props).forEach(k => {
      const kk = (k || '').trim(), vv = (props[k] || '').trim();
      if (kk && vv) out[kk] = vv;
    });
    if (Object.keys(out).length) data.PROPS = out; else delete data.PROPS;
  }

  // Build.PRODUCT (ro.product.name) is its own value, e.g. Samsung "e3qxxx" vs model "SM-S928B".
  // Blank → the fingerprint's product segment (brand/PRODUCT/device:...), else the model.
  function productOf(form) {
    const typed = (form.product || '').trim();
    if (typed) return typed;
    const seg = (form.fingerprint || '').trim().split('/');
    return (seg.length >= 3 && seg[1].trim()) || (form.model || '').trim();
  }

  /* form: {name, brand, model, product, manufacturer, fingerprint, android, sdk, adv, props}
     editingKey: existing _DEVICE key when editing, else null. */
  function upsertDevice(form, editingKey) {
    // Identity ("This Device") profile: fixed key, SPARSE (only non-empty fields kept),
    // DEVICE never set (so it stays a no-spoof profile). Fill a field → it spoofs only
    // that one. No 'Unknown'/generated fallbacks, no rename. Merge over the existing
    // object so unmanaged keys (e.g. a hand-added PROPS) survive.
    if (editingKey === THIS_DEVICE_DKEY) {
      const prev = (config[THIS_DEVICE_DKEY] && typeof config[THIS_DEVICE_DKEY] === 'object')
        ? config[THIS_DEVICE_DKEY] : {};
      const data = { ...prev };
      const set = (k, v) => { const t = (v || '').trim(); if (t) data[k] = t; else delete data[k]; };
      set('BRAND', form.brand); set('MANUFACTURER', form.manufacturer);
      set('MODEL', form.model); set('PRODUCT', productOf(form));
      set('FINGERPRINT', form.fingerprint);
      delete data.DEVICE;                                // locked: identity never fakes Build.DEVICE
      set('SERIAL', form.serial);
      delete data.ANDROID_ID; delete data.GAID;    // seeds removed — per-app tag derives from package
      set('ANDROID_VERSION', form.android); set('SDK_INT', form.sdk);
      if (form.adv) Object.keys(form.adv).forEach(k => set(k, form.adv[k]));
      applyProps(data, form.props);
      config[THIS_DEVICE_DKEY] = data;
      return { deviceKey: THIS_DEVICE_DKEY, packageKey: THIS_DEVICE_PKEY };
    }
    const name  = form.name.trim();
    const model = (form.model || '').trim() || 'Unknown';
    const brand = (form.brand || '').trim() || 'Unknown';
    const deviceKey = deviceKeyFromName(name);
    const packageKey = pkgKeyOf(deviceKey);

    // Merge over the existing object on edit (not a full rebuild) so keys the
    // form doesn't manage — e.g. a hand-added PROPS:{} object — survive. Spread
    // keeps original key order; managed keys are overwritten in place, blanked
    // optional keys are deleted.
    const prev = (editingKey && config[editingKey] && typeof config[editingKey] === 'object')
      ? config[editingKey] : {};
    const data = { ...prev };
    data.NAME = name;                                   // display label (WebUI only; native ignores)
    data.BRAND = brand;
    data.MANUFACTURER = (form.manufacturer || '').trim() || 'Unknown';
    data.MODEL = model;
    data.FINGERPRINT = (form.fingerprint || '').trim()
      || `${brand}/${model}/${model}:14/UP1A.231005.007/20230101:user/release-keys`;
    // Build.DEVICE must be the real ro.product.device CODENAME, not the display name. An explicit
    // DEVICE from the form wins (manual override); otherwise take it from the fingerprint's device
    // segment (brand/product/DEVICE:ver/...), falling back to the typed name only with no fingerprint.
    // NOTE: a DEVICE that differs from the fingerprint's device segment is itself a mismatch tell.
    data.DEVICE = (form.device || '').trim() || (function () {
      let dev = name;
      const seg = data.FINGERPRINT.split('/');
      if (seg.length >= 3) { const c = seg[2].split(':')[0].trim(); if (c) dev = c; }
      return dev;
    })();
    data.PRODUCT = productOf(form) || model;
    // Optional fields: set when present, delete when blank (so clearing a field
    // in the form actually removes it from the profile rather than going stale).
    const setOpt = (key, val) => { const v = (val || '').trim(); if (v) data[key] = v; else delete data[key]; };
    setOpt('SERIAL', form.serial);
    delete data.ANDROID_ID; delete data.GAID;      // seeds removed — per-app tag derives from package (or aid=/gaid= literal)
    setOpt('ANDROID_VERSION', form.android);
    setOpt('SDK_INT', form.sdk);
    // Optional extra Build.* / Build$VERSION.* fields (BOARD, HARDWARE, DISPLAY, ID,
    // BOOTLOADER, TAGS, TYPE, SECURITY_PATCH, INCREMENTAL, CODENAME, SOC_MANUFACTURER,
    // SOC_MODEL). spoof_module sets each via JNI and mirrors it into props for COW.
    // See EXTRA_BUILD_FIELDS in spoof_module.cpp.
    if (form.adv) Object.keys(form.adv).forEach(k => setOpt(k, form.adv[k]));
    // Custom system props (e.g. ril.product_code, ro.omc.build.version) — COW-forged for a
    // `cow`-tagged app. Managed by the device modal's props editor; merges over any existing PROPS.
    applyProps(data, form.props);

    if (editingKey && editingKey !== deviceKey) {
      // renamed: migrate key positions + package array
      const oldPkgKey = pkgKeyOf(editingKey);
      const di = keyOrder.indexOf(editingKey);
      const pi = keyOrder.indexOf(oldPkgKey);
      if (di !== -1) keyOrder[di] = deviceKey;
      if (pi !== -1) keyOrder[pi] = packageKey;
      if (config[oldPkgKey]) { config[packageKey] = config[oldPkgKey]; delete config[oldPkgKey]; }
      delete config[editingKey];
    } else if (!editingKey) {
      keyOrder.push(packageKey, deviceKey);
    }
    if (!Array.isArray(config[packageKey])) config[packageKey] = [];
    config[deviceKey] = data;
    return { deviceKey, packageKey };
  }

  function deleteDevice(deviceKey) {
    if (deviceKey === THIS_DEVICE_DKEY || deviceKey === RANDOM_DKEY) return { removedCount: 0, blocked: true }; // built-in buckets
    const packageKey = pkgKeyOf(deviceKey);
    const removed = Array.isArray(config[packageKey]) ? [...config[packageKey]] : [];
    // drop the device's games from list.json
    removed.forEach(p => { const c = clean(p); if (names[c]) delete names[c]; });
    delete config[packageKey];
    delete config[deviceKey];
    keyOrder = keyOrder.filter(k => k !== packageKey && k !== deviceKey);
    return { removedCount: removed.length };
  }

  /* ─── Device share (portable export/import code) ───
     A device profile is just its PACKAGES_<KEY>_DEVICE object. exportDevice wraps it
     with a magic + version so a pasted code is self-describing; importDevice rebuilds
     the profile DIRECTLY (no form) + creates the paired empty package list. The
     attached packages/games are NOT shared — they're the importer's own installed
     apps, meaningless on another phone. Zero native involvement (WebUI-only). */
  const DEV_CODE_MAGIC = 'COPG-DEV';
  const DEV_CODE_VER   = 1;
  // Every string field a device profile may legitimately carry (mirrors the device
  // modal + EXTRA_BUILD_FIELDS in spoof_module.cpp). Anything outside this set is
  // dropped on import so a hostile code can't inject arbitrary keys into the config.
  const DEVICE_STR_FIELDS = [
    'BRAND', 'DEVICE', 'MANUFACTURER', 'MODEL', 'FINGERPRINT', 'PRODUCT',
    'SERIAL', 'ANDROID_VERSION', 'SDK_INT',
    'BOARD', 'HARDWARE', 'DISPLAY', 'ID', 'BOOTLOADER', 'TAGS', 'TYPE',
    'SECURITY_PATCH', 'INCREMENTAL', 'CODENAME', 'SOC_MANUFACTURER', 'SOC_MODEL', 'RADIO',
  ];
  // copy only known string fields (+ PROPS if a string map) from a raw profile
  function pickDeviceFields(prof) {
    const out = {};
    DEVICE_STR_FIELDS.forEach(k => { if (typeof prof[k] === 'string' && prof[k].trim()) out[k] = prof[k].trim(); });
    if (prof.PROPS && typeof prof.PROPS === 'object' && !Array.isArray(prof.PROPS)) {
      const p = {};
      Object.keys(prof.PROPS).forEach(k => { if (typeof prof.PROPS[k] === 'string') p[k] = prof.PROPS[k]; });
      if (Object.keys(p).length) out.PROPS = p;
    }
    return out;
  }

  // how many packages a device's list holds (for the share sheet's "include packages" row)
  function devicePackageCount(deviceKey) {
    const arr = config[pkgKeyOf(deviceKey)];
    return Array.isArray(arr) ? arr.length : 0;
  }

  function exportDevice(deviceKey, includePackages) {
    const prof = config[deviceKey];
    if (!prof || typeof prof !== 'object') return null;
    const name = deviceKey === THIS_DEVICE_DKEY
      ? (prof.MODEL || prof.DEVICE || 'My Device')
      : (prof.DEVICE || deviceLabel(deviceKey, prof));
    const device = pickDeviceFields(prof);
    const payload = { m: DEV_CODE_MAGIC, v: DEV_CODE_VER, name, device };
    if (includePackages) {
      const arr = config[pkgKeyOf(deviceKey)];
      if (Array.isArray(arr) && arr.length) {
        payload.packages = arr.slice();                 // package strings incl. their colon tags
        const labels = {};                              // + their list.json display names
        arr.forEach(p => { const c = clean(p); if (names[c]) labels[c] = names[c]; });
        if (Object.keys(labels).length) payload.names = labels;
      }
    }
    // nothing to share (sparse identity + no packages) → let the caller warn
    if (!Object.keys(device).length && !payload.packages) return null;
    return `${DEV_CODE_MAGIC}${DEV_CODE_VER}:${b64(JSON.stringify(payload))}`;
  }

  // every PACKAGES_<KEY> list key (arrays in the config, incl. This Device)
  function pkgListKeys() { return keyOrder.filter(k => Array.isArray(config[k])); }
  // which existing device list (if any) already owns this clean package
  function findPackageOwner(cleanPkg) {
    for (const k of pkgListKeys()) if (config[k].some(p => clean(p) === cleanPkg)) return k;
    return null;
  }

  // importDevice(code, opts?) — opts.conflictMode: 'move' | 'keep' | undefined.
  // A package may live on only ONE device (else Zygisk can't tell which profile to
  // spoof). If imported packages already exist on another device and no conflictMode
  // is given, this returns {needsConflict:true, conflicts:[…]} WITHOUT mutating, so
  // the UI can ask. 'move' = take them here (remove from old); 'keep' = leave them on
  // the old device (skip on the new one).
  function importDevice(code, opts) {
    opts = opts || {};
    const raw = String(code || '').trim();
    if (!raw) return { ok: false, error: 'empty' };
    // tolerate the human prefix ("COPG-DEV1:") OR a bare base64 blob
    const m = raw.match(/^COPG-DEV\d*:([\s\S]+)$/);
    const payloadB64 = (m ? m[1] : raw).replace(/\s+/g, '');
    let obj;
    try { obj = JSON.parse(unb64(payloadB64)); }
    catch (_) { return { ok: false, error: 'bad' }; }
    if (!obj || obj.m !== DEV_CODE_MAGIC || !obj.device || typeof obj.device !== 'object')
      return { ok: false, error: 'bad' };

    const data = pickDeviceFields(obj.device);

    // optional shared packages: keep strings + tags, drop junk, dedupe by clean base
    let pkgs = [];
    if (Array.isArray(obj.packages)) {
      const seen = new Set();
      pkgs = obj.packages
        .filter(p => typeof p === 'string').map(p => p.trim())
        .filter(p => { const c = clean(p); if (!c || seen.has(c)) return false; seen.add(c); return true; });
    }
    // valid = at least one identity field OR at least one package (a sparse
    // "This Device"-style profile that only carries package tags is allowed).
    const hasIdentity = !!(data.BRAND || data.MODEL || data.FINGERPRINT);
    if (!hasIdentity && !pkgs.length) return { ok: false, error: 'bad' };

    // cross-device duplicate check (a package on two devices breaks Zygisk lookup)
    const conflicts = [];
    for (const p of pkgs) {
      const c = clean(p), owner = findPackageOwner(c);
      if (owner) {
        const dk = owner + '_DEVICE';
        conflicts.push({ pkg: p, clean: c, ownerKey: owner, ownerName: deviceLabel(dk, config[dk]) });
      }
    }
    if (conflicts.length && !opts.conflictMode)
      return { ok: false, needsConflict: true, conflicts, conflictCount: conflicts.length, total: pkgs.length };

    // resolve conflicts
    let moved = 0, kept = 0;
    if (conflicts.length) {
      const conflSet = new Set(conflicts.map(c => c.clean));
      if (opts.conflictMode === 'keep') {
        pkgs = pkgs.filter(p => !conflSet.has(clean(p)));   // leave dupes on the old device
        kept = conflicts.length;
      } else {                                              // 'move' → strip from old lists
        conflicts.forEach(cf => {
          const arr = config[cf.ownerKey];
          if (Array.isArray(arr)) config[cf.ownerKey] = arr.filter(p => clean(p) !== cf.clean);
        });
        moved = conflicts.length;
      }
    }
    // after 'keep' filtering there may be nothing left to import
    if (!hasIdentity && !pkgs.length) return { ok: false, error: 'empty' };

    // name from the payload, uniquified against existing devices (never overwrite)
    const base = (obj.name || data.DEVICE || data.MODEL || 'Imported Device').trim();
    let name = base, n = 2;
    while (config[deviceKeyFromName(name)]) name = `${base} (${n++})`;
    // only give a real identity profile a Build.DEVICE; a packages-only import
    // stays sparse (no identity spoof), just like the built-in This Device.
    if (hasIdentity && !data.DEVICE) data.DEVICE = name;

    const deviceKey  = deviceKeyFromName(name);
    const packageKey = pkgKeyOf(deviceKey);
    keyOrder.push(packageKey, deviceKey);
    config[packageKey] = pkgs;
    config[deviceKey]  = data;
    // merge shared display names into list.json for the imported packages
    if (obj.names && typeof obj.names === 'object') {
      pkgs.forEach(p => { const c = clean(p); const l = obj.names[c];
        if (typeof l === 'string' && l.trim()) names[c] = l.trim(); });
    }
    return { ok: true, deviceKey, name, packageCount: pkgs.length, moved, kept };
  }

  /* ─── Package mutations ─── */
  /* form: { pkg (clean), name, deviceKey (PACKAGES_… list key), with_cpu, cpuModel,
            blocked, cow, aid, gpu, gpuModel, mock, drm, drmLevel, drmId, drmSys, imei, imeiLiteral, imei2Literal, sim, simS0, simS1, dnd, dab, kso, nolog }
     editing: { clean, deviceKey } | null
     Unified model: every package lives in a PACKAGES_<KEY> device list (incl. the
     built-in "This Device"). All spoofs are colon tags on the package string. */
  function upsertPackage(form, editing) {
    const cleanName = clean(form.pkg.trim());
    const displayName = (form.name || '').trim() || cleanName;
    const newPkgKey = form.deviceKey ? pkgKeyOf(form.deviceKey) : null;

    // list.json: rename-aware update, keyed per (package, user-scope) via idPackage — so renaming
    // ONE clone (userScope>0 → "pkg@u<id>") never touches the other clones or the base entry.
    const nameKey = idPackage(cleanName, form.userScope);
    if (editing && editing.clean) {
      const oldKey = idPackage(editing.clean, editing.userScope);
      if (oldKey !== nameKey && names[oldKey]) delete names[oldKey];
    }
    names[nameKey] = displayName;

    // CPU spoof = a single cpu=<key> tag (enables the mount + picks the profile).
    // 'blocked' (unmount → force real CPU) is the opposite, so they're mutually
    // exclusive — with_cpu wins. cow/aid/gpu + tweak tags are independent.
    let finalPkg = cleanName;
    if (form.with_cpu)     finalPkg = setCpuModel(finalPkg, form.cpuModel || 'sd8elite');
    else if (form.blocked) finalPkg = addTag(finalPkg, 'blocked');
    if (form.cow)          finalPkg = addTag(finalPkg, 'cow');
    if (form.aid) {
      const lit = (form.aidLiteral || '').trim().toLowerCase();   // exact 16-hex → literal, else derive
      finalPkg = /^[0-9a-f]{16}$/.test(lit) ? addTag(finalPkg, 'aid=' + lit) : addTag(finalPkg, 'aid');
    }
    // Per-app SERIAL (FREE). Overrides the device profile's SERIAL; the modal greys this off when
    // the chosen profile already sets a SERIAL, so normally only one place sets it. A literal is
    // sanitized to serial-shaped [0-9A-Z]; empty → bare 'serial' (native derives from the package).
    if (form.serial) {
      const lit = (form.serialLiteral || '').trim().toUpperCase().replace(/[^0-9A-Z]/g, '').slice(0, 24);
      finalPkg = lit ? addTag(finalPkg, 'serial=' + lit) : addTag(finalPkg, 'serial');
    }
    if (form.gaid) {
      const lit = (form.gaidLiteral || '').trim().toLowerCase();  // exact UUID → literal, else seed-derive
      finalPkg = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(lit)
        ? addTag(finalPkg, 'gaid=' + lit) : addTag(finalPkg, 'gaid');
    }
    if (form.appset) {
      const lit = (form.appsetLiteral || '').trim().toLowerCase();  // exact UUID → literal, else package-derive
      finalPkg = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(lit)
        ? addTag(finalPkg, 'appset=' + lit) : addTag(finalPkg, 'appset');
    }
    if (form.hideDev)      finalPkg = addTag(finalPkg, 'hidedev');
    if (form.randid)       finalPkg = addTag(finalPkg, 'randid');   // fresh derived IDs each launch (PRO, stealth)
    if (form.timezone)     finalPkg = setTimezone(finalPkg, form.timezone);   // tz=<zone> (FREE, stealth)
    if (form.locale)       finalPkg = setLocale(finalPkg, form.locale);       // lang=<BCP-47> (FREE, stealth, system-side)
    if (form.wallpaper)    finalPkg = setWallpaper(finalPkg, true, form.wallpaperId); // wallpaper[=<id>] (PRO, stealth, system-side)
    if (form.account)      finalPkg = setAccount(finalPkg, true, form.accountEmail);  // account[=<email>] (PRO, stealth, system-side)
    if (form.webua)        finalPkg = setUa(finalPkg, form.webua);            // ua=<key> WebView User-Agent profile (PRO, resident)
    if (form.gpu)          finalPkg = setGpuModel(finalPkg, form.gpuModel || 'adreno830');
    if (form.mock)         finalPkg = addTag(finalPkg, 'mock');
    if (form.sensor)       finalPkg = addTag(finalPkg, 'sensor');     // sensor-roster spoof (PRO, resident)
    if (form.getprop)      finalPkg = addTag(finalPkg, 'getprop');    // getprop-child consistency (PRO, resident libc execve hook)
    if (form.webfp) {                                                 // WebView canvas fp spoof (PRO, resident)
      const seed = (form.webfpSeed || '').trim().toLowerCase().replace(/[^0-9a-fx]/g, '').slice(0, 10);
      finalPkg = seed ? addTag(finalPkg, 'webfp=' + seed) : addTag(finalPkg, 'webfp');  // pinned seed, else derive
    }
    // GPS (PRO, resident): gps=<lat>,<lng> overwrites delivered Location coords. setGps validates range.
    if (form.gps && form.gpsLat !== '' && form.gpsLng !== '') finalPkg = setGps(finalPkg, form.gpsLat, form.gpsLng);
    // Cell tower (PRO, resident): cell=<lac>,<ci> overwrites the reported CellInfo identity. Independent of GPS.
    if (form.cell && form.cellLac !== '' && form.cellCi !== '') finalPkg = setCell(finalPkg, form.cellLac, form.cellCi, form.cellMcc, form.cellMnc);
    // Uptime (PRO, resident): uptime=<sec> is ADDED to the real device uptime (survives app close).
    if (form.uptime && form.uptimeSeconds > 0) finalPkg = setUptime(finalPkg, form.uptimeSeconds);
    // boot_id (PRO): stealth NoMount redirect where present, else resident inline hook. setBootid
    // validates a pinned literal (falls back to churn/stable on a malformed UUID).
    finalPkg = setBootid(finalPkg, !!form.bootid, !!form.bootidChurn, form.bootidLiteral);
    // WiFi MAC (PRO): resident inline /sys/.../address redirect. setMac strips any ':'/'-' from the
    // literal (the tag stores 12 hex; a colon MAC would be shredded by the colon tag split).
    finalPkg = setMac(finalPkg, !!form.mac, !!form.macChurn, form.macLiteral);
    finalPkg = setBtmac(finalPkg, !!form.btmac, !!form.btmacChurn, form.btmacLiteral);
    // Refresh rate (FREE, resident): rr=<hz> spoofs the reported display refresh rate.
    if (form.rr && form.refreshHz > 0) finalPkg = setRefreshRate(finalPkg, form.refreshHz);
    if (form.drm) {
      // securityLevel: L2/L3 → pin via drm=<level>; L1 (or blank) → bare :drm (the default L1 path).
      const lvl = (form.drmLevel || '').toUpperCase();
      if (lvl === 'L2' || lvl === 'L3') finalPkg = addTag(finalPkg, 'drm=' + lvl);
      else                              finalPkg = addTag(finalPkg, 'drm');   // resident MediaDrm identity spoof (PRO): L1
      const idLit  = (form.drmId  || '').trim().toLowerCase();   // even-length hex → pin deviceUniqueId, else derive
      const sysLit = (form.drmSys || '').trim();                 // digits → pin systemId, else derive
      if (/^[0-9a-f]+$/.test(idLit) && idLit.length % 2 === 0) finalPkg = addTag(finalPkg, 'drmid=' + idLit);
      if (/^[0-9]+$/.test(sysLit))                             finalPkg = addTag(finalPkg, 'drmsys=' + sysLit);
    }
    if (form.imei) {
      finalPkg = addTag(finalPkg, 'imei');                     // resident TelephonyManager IMEI spoof (PRO)
      const imeiLit  = (form.imeiLiteral  || '').trim();       // slot 0: exactly 15 digits → pin, else derive
      const imei2Lit = (form.imei2Literal || '').trim();       // slot 1 (dual-SIM): same
      if (/^[0-9]{15}$/.test(imeiLit))  finalPkg = addTag(finalPkg, 'imei=' + imeiLit);
      if (/^[0-9]{15}$/.test(imei2Lit)) finalPkg = addTag(finalPkg, 'imei2=' + imei2Lit);
    }
    if (form.imsi) {
      finalPkg = addTag(finalPkg, 'imsi');                     // resident TelephonyManager.getSubscriberId spoof (PRO)
      const imsiLit  = (form.imsiLiteral  || '').trim();       // slot 0: 14-15 digits → pin, else derive
      const imsi2Lit = (form.imsi2Literal || '').trim();       // slot 1 (dual-SIM): same
      if (/^[0-9]{14,15}$/.test(imsiLit))  finalPkg = addTag(finalPkg, 'imsi=' + imsiLit);
      if (/^[0-9]{14,15}$/.test(imsi2Lit)) finalPkg = addTag(finalPkg, 'imsi2=' + imsi2Lit);
    }
    if (form.iccid) {
      finalPkg = addTag(finalPkg, 'iccid');                    // resident TelephonyManager.getSimSerialNumber spoof (PRO)
      const iccidLit  = (form.iccidLiteral  || '').trim();     // slot 0: 19-20 digits → pin, else derive
      const iccid2Lit = (form.iccid2Literal || '').trim();     // slot 1 (dual-SIM): same
      if (/^[0-9]{19,20}$/.test(iccidLit))  finalPkg = addTag(finalPkg, 'iccid=' + iccidLit);
      if (/^[0-9]{19,20}$/.test(iccid2Lit)) finalPkg = addTag(finalPkg, 'iccid2=' + iccid2Lit);
    }
    if (form.vpns)         finalPkg = addTag(finalPkg, 'vpns');  // main Hide VPN → system_server (pairip-safe, per-uid; reboot to apply)
    if (form.vpnAgg)       finalPkg = addTag(finalPkg, 'vpnn');  // aggressive → full resident in-app hook (ART + native getifaddrs)
    if (form.playsrc)      finalPkg = addTag(finalPkg, 'playsrc'); // Fake Play install source → system_server ComputerEngine hook (PRO, stealth-from-app)
    if (form.pairip)       finalPkg = addTag(finalPkg, 'pairip');  // Block PairIP license check → system_server ActivityStarter hook (PRO, stealth-from-app, independent)
    if (form.tensorfeat)   finalPkg = addTag(finalPkg, 'tensorfeat'); // Tensor feature targets → system_server PMS.hasSystemFeature hook (PRO, stealth-from-app)
    if (form.sim && (form.simS0 || form.simS1)) finalPkg = setCarrierModel(finalPkg, form.sim, form.simS0, form.simS1);
    if (form.proxy) finalPkg = setProxy(finalPkg, form.proxy);   // proxy=<key> per-app SOCKS5 (PRO)
    if (form.dnd)          finalPkg = addTag(finalPkg, 'dnd');
    if (form.dab)          finalPkg = addTag(finalPkg, 'dab');
    if (form.kso)          finalPkg = addTag(finalPkg, 'kso');
    if (form.nolog)        finalPkg = addTag(finalPkg, 'nolog');
    finalPkg = setDpi(finalPkg, form.dpi);   // dpi=<n> per-app screen density (clamped 120..640, '' = off)
    finalPkg = setUserScope(finalPkg, form.userScope);   // u=<id> per-user scope ('' = all users)

    const oldClean  = editing ? editing.clean : null;
    const oldUser   = editing ? (editing.userScope || '') : '';
    const newUser   = userScopeOf(finalPkg);
    const oldPkgKey = editing && editing.deviceKey ? pkgKeyOf(editing.deviceKey) : null;
    // Identity is (clean package, user-scope) — NOT clean alone — so editing/replacing one
    // user's entry never touches another user's entry for the same package on the same list.
    const sameEntry = (p, cn, us) => clean(p) === cn && userScopeOf(p) === us;

    // original index in its profile (to keep order on an in-place edit)
    let origPos = -1;
    if (oldPkgKey && Array.isArray(config[oldPkgKey]))
      origPos = config[oldPkgKey].findIndex(p => sameEntry(p, oldClean, oldUser));

    // remove from old profile (moved away, or to re-insert in place)
    if (oldPkgKey && Array.isArray(config[oldPkgKey])) {
      const oi = config[oldPkgKey].findIndex(p => sameEntry(p, oldClean, oldUser));
      if (oi !== -1) config[oldPkgKey].splice(oi, 1);
    }

    if (newPkgKey) {
      if (!Array.isArray(config[newPkgKey])) {
        config[newPkgKey] = [];
        if (!keyOrder.includes(newPkgKey)) {
          const di = keyOrder.indexOf(newPkgKey + '_DEVICE');
          if (di !== -1) keyOrder.splice(di, 0, newPkgKey); else keyOrder.push(newPkgKey);
        }
      }
      const arr = config[newPkgKey];
      const ex = arr.findIndex(p => sameEntry(p, cleanName, newUser));
      if (ex !== -1) arr.splice(ex, 1);
      if (oldPkgKey === newPkgKey && origPos !== -1) arr.splice(origPos, 0, finalPkg);
      else arr.push(finalPkg);
    }
    return { clean: cleanName, userScope: newUser };
  }

  function deletePackage(cleanName, _type, deviceKey, userScope) {
    const us = userScope == null ? null : String(userScope);
    const pkgKey = deviceKey ? pkgKeyOf(deviceKey) : null;
    if (pkgKey && Array.isArray(config[pkgKey])) {
      // Remove the entry for THIS user-scope only (null userScope = legacy caller → first
      // match by clean, back-compat). Other users' entries for the same package survive.
      const i = config[pkgKey].findIndex(p =>
        clean(p) === cleanName && (us === null || userScopeOf(p) === us));
      if (i !== -1) config[pkgKey].splice(i, 1);
    }
    // Drop this entry's per-(package,user) display name if no other entry with the SAME scope
    // remains; also drop the base name if the package is gone from every profile/user entirely.
    const nameKey = idPackage(cleanName, us == null ? '' : us);
    const remaining = listPackages();
    if (names[nameKey] && nameKey !== cleanName &&
        !remaining.some(p => p.clean === cleanName && idPackage(cleanName, p.userScope) === nameKey))
      delete names[nameKey];
    if (names[cleanName] && !remaining.some(p => p.clean === cleanName)) delete names[cleanName];
  }

  /* ─── Serialize + persist ─── */
  function serializeConfig() {
    const ordered = {};
    for (const k of keyOrder) if (config[k] !== undefined) ordered[k] = config[k];
    for (const k of Object.keys(config)) if (!keyOrder.includes(k)) { keyOrder.push(k); ordered[k] = config[k]; }
    return JSON.stringify(ordered, null, 2);
  }

  // Packages tagged ':vpns' across all profiles → the live target list the system_server VPN hook
  // re-reads every ~1.5s, so enabling ':vpns' on an app applies WITHOUT a reboot. De-duped clean names.
  function vpnsTargets() {
    const set = new Set();
    for (const key of keyOrder) {
      if (key.startsWith('PACKAGES_') && !key.endsWith('_DEVICE') && Array.isArray(config[key]))
        for (const entry of config[key]) if (hasTag(entry, 'vpns')) set.add(clean(entry));
    }
    return [...set];
  }

  // Packages tagged with `tag` across all profiles → package-name list. Shared by the ':playsrc' /
  // ':pairip' live-file writers (the system_server PlaysrcHook re-reads them every ~1.5s, so toggling
  // OFF disarms — and toggling a SECOND app ON works — with NO reboot; the FIRST enable still needs a
  // reboot because the load-bearing dex is only decrypted + armed at boot).
  function tagTargets(tag) {
    const set = new Set();
    for (const key of keyOrder) {
      if (key.startsWith('PACKAGES_') && !key.endsWith('_DEVICE') && Array.isArray(config[key]))
        for (const entry of config[key]) if (hasTag(entry, tag)) set.add(clean(entry));
    }
    return [...set];
  }

  // Packages tagged 'lang=<bcp47>' across all profiles → the live "package<TAB>locale" map the
  // system_server locale hook re-reads every ~1.5s, so enabling lang= applies WITHOUT a reboot.
  function langTargets() {
    const out = [];
    const seen = new Set();
    for (const key of keyOrder) {
      if (key.startsWith('PACKAGES_') && !key.endsWith('_DEVICE') && Array.isArray(config[key]))
        for (const entry of config[key]) {
          const loc = localeOf(entry);
          if (!loc) continue;
          const p = clean(entry);
          if (seen.has(p)) continue;
          seen.add(p);
          out.push(p + '\t' + loc);
        }
    }
    return out;
  }

  // Packages tagged 'wallpaper'/'wallpaper=<id>' across all profiles → the live "package<TAB>id" map the
  // system_server wallpaper hook re-reads every ~1.5s, so enabling wallpaper applies WITHOUT a reboot. A
  // bare 'wallpaper' tag emits "package<TAB>" (empty id → the hook forges -1).
  function wallpaperTargets() {
    const out = [];
    const seen = new Set();
    for (const key of keyOrder) {
      if (key.startsWith('PACKAGES_') && !key.endsWith('_DEVICE') && Array.isArray(config[key]))
        for (const entry of config[key]) {
          if (!hasWallpaper(entry)) continue;
          const p = clean(entry);
          if (seen.has(p)) continue;
          seen.add(p);
          out.push(p + '\t' + wallpaperOf(entry));
        }
    }
    return out;
  }

  // Packages tagged 'account'/'account=<email>' across all profiles → the live "package<TAB>email" map
  // the system_server account hook re-reads every ~1.5s (no reboot). A bare 'account' tag emits
  // "package<TAB>" (empty email → the hook HIDES / returns an empty account list).
  function accountTargets() {
    const out = [];
    const seen = new Set();
    for (const key of keyOrder) {
      if (key.startsWith('PACKAGES_') && !key.endsWith('_DEVICE') && Array.isArray(config[key]))
        for (const entry of config[key]) {
          if (!hasAccount(entry)) continue;
          const p = clean(entry);
          if (seen.has(p)) continue;
          seen.add(p);
          out.push(p + '\t' + accountEmailOf(entry));
        }
    }
    return out;
  }

  // Packages tagged 'aid'/'aid=<hex>' across all profiles → the live "package<TAB>literal" map the
  // system_server SsaidHook re-reads every ~1.5s (no reboot). A bare 'aid' emits "package<TAB>" (empty
  // literal → the hook derives the id from the package, byte-identical to the in-app forge).
  function ssaidTargets() {
    const out = [];
    const seen = new Set();
    for (const key of keyOrder) {
      if (key.startsWith('PACKAGES_') && !key.endsWith('_DEVICE') && Array.isArray(config[key]))
        for (const entry of config[key]) {
          if (!hasTag(entry, 'aid') && !aidLiteralOf(entry)) continue;
          const p = clean(entry);
          if (seen.has(p)) continue;
          seen.add(p);
          out.push(p + '\t' + aidLiteralOf(entry));
        }
    }
    return out;
  }

  async function save() {
    const cfgStr  = serializeConfig();
    const listStr = JSON.stringify(names, null, 2);
    if (!hasBridge()) { previewUnsaved = true; return { saved: false, preview: true }; }
    await execCommand(`echo '${shq(cfgStr)}' > ${CONFIG_PATH}`);
    await execCommand(`echo '${shq(listStr)}' > ${LIST_PATH}`);
    try {
      await execCommand(`chmod 644 ${CONFIG_PATH} ${LIST_PATH}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${CONFIG_PATH}`);
      await execCommand(`chcon u:object_r:system_file:s0 ${LIST_PATH}`);
    } catch (e) { console.warn('post-write chmod/chcon:', e); }
    // Push the live ':vpns' target list (package names, one per line) so the system_server VPN hook
    // applies with NO reboot. Must be system_data_file (not system_file) so system_server can read it.
    try {
      const pkgs = vpnsTargets();
      const body = pkgs.length ? pkgs.join('\n') + '\n' : '';
      await execCommand(`echo '${b64(body)}' | base64 -d > ${VPNS_TARGETS_PATH}`);
      await execCommand(`chmod 0644 ${VPNS_TARGETS_PATH}`);
      await execCommand(`chcon u:object_r:system_data_file:s0 ${VPNS_TARGETS_PATH} 2>/dev/null || true`);
      // The hook re-reads the file within ~1.5s, but an app caches its network view and only re-queries
      // getNetworkCapabilities on a FRESH process — so force-stop the :vpns apps. Reopening them then
      // reads the hidden value, no reboot (this is why a reboot "fixed" it — it restarted the app).
    } catch (e) { console.warn('vpns targets write:', e); }
    // Push the live 'lang=' target map (package<TAB>bcp47 per line) so the system_server locale hook
    // applies with NO reboot. system_data_file so system_server can read it. An app only re-reads its
    // locale on a FRESH process, so relaunch a lang= app to see the change (this is what a reboot did).
    try {
      const lines = langTargets();
      const body = lines.length ? lines.join('\n') + '\n' : '';
      await execCommand(`echo '${b64(body)}' | base64 -d > ${LANG_TARGETS_PATH}`);
      await execCommand(`chmod 0644 ${LANG_TARGETS_PATH}`);
      await execCommand(`chcon u:object_r:system_data_file:s0 ${LANG_TARGETS_PATH} 2>/dev/null || true`);
    } catch (e) { console.warn('lang targets write:', e); }
    // Push the live 'wallpaper' target map (package<TAB>id per line) so the system_server wallpaper hook
    // applies with NO reboot. system_data_file so system_server can read it. An app only re-reads the
    // wallpaper id on a FRESH process, so relaunch a wallpaper-tagged app to see the change.
    try {
      const lines = wallpaperTargets();
      const body = lines.length ? lines.join('\n') + '\n' : '';
      await execCommand(`echo '${b64(body)}' | base64 -d > ${WALLPAPER_TARGETS_PATH}`);
      await execCommand(`chmod 0644 ${WALLPAPER_TARGETS_PATH}`);
      await execCommand(`chcon u:object_r:system_data_file:s0 ${WALLPAPER_TARGETS_PATH} 2>/dev/null || true`);
    } catch (e) { console.warn('wallpaper targets write:', e); }
    // Push the live 'account' target map (package<TAB>email per line; bare = hide) so the system_server
    // account hook applies with NO reboot. system_data_file so system_server can read it. An app only
    // re-reads its accounts on a FRESH process, so relaunch an account-tagged app to see the change.
    try {
      const lines = accountTargets();
      const body = lines.length ? lines.join('\n') + '\n' : '';
      await execCommand(`echo '${b64(body)}' | base64 -d > ${ACCOUNT_TARGETS_PATH}`);
      await execCommand(`chmod 0644 ${ACCOUNT_TARGETS_PATH}`);
      await execCommand(`chcon u:object_r:system_data_file:s0 ${ACCOUNT_TARGETS_PATH} 2>/dev/null || true`);
    } catch (e) { console.warn('account targets write:', e); }
    // Push the live 'aid' target map (package<TAB>literal per line; bare = derive) so the system_server
    // ANDROID_ID hook applies with NO reboot — the query/call/shell read paths then match the in-app
    // getString forge. system_data_file so system_server can read it. The id is cached in-process, so
    // relaunch an aid-tagged app (fresh process) to pick up a change.
    try {
      const lines = ssaidTargets();
      const body = lines.length ? lines.join('\n') + '\n' : '';
      await execCommand(`echo '${b64(body)}' | base64 -d > ${SSAID_TARGETS_PATH}`);
      await execCommand(`chmod 0644 ${SSAID_TARGETS_PATH}`);
      await execCommand(`chcon u:object_r:system_data_file:s0 ${SSAID_TARGETS_PATH} 2>/dev/null || true`);
    } catch (e) { console.warn('ssaid targets write:', e); }
    // Push the live ':playsrc' + ':pairip' target lists so the system_server PlaysrcHook applies a
    // toggle-OFF (disarm) or a second toggle-ON with NO reboot. The hook re-reads within ~1.5s but an
    // app only re-queries its install source / launches LicenseActivity on a FRESH process, so relaunch
    // the target app to see the change. (The FIRST enable still needs a reboot — the load-bearing dex is
    // only decrypted + the hook armed at boot; writing the file for an app whose hook never armed does
    // nothing, which also keeps the free tier gated.) system_data_file so system_server can read them.
    try {
      for (const [tag, path] of [['playsrc', PLAYSRC_TARGETS_PATH], ['pairip', PAIRIP_TARGETS_PATH], ['tensorfeat', TENSORFEAT_TARGETS_PATH], ['hidedev', HIDEDEV_TARGETS_PATH]]) {
        const pkgs = tagTargets(tag);
        const body = pkgs.length ? pkgs.join('\n') + '\n' : '';
        await execCommand(`echo '${b64(body)}' | base64 -d > ${path}`);
        await execCommand(`chmod 0644 ${path}`);
        await execCommand(`chcon u:object_r:system_data_file:s0 ${path} 2>/dev/null || true`);
      }
    } catch (e) { console.warn('playsrc/pairip targets write:', e); }
    // Push the live per-app proxy assignment (proxy/active.conf) so the root proxy daemon
    // (copg_proxy.sh watch) reloads within ~3s — routes each proxy= app through its remote SOCKS5.
    try { await writeActiveConf(); } catch (e) { console.warn('proxy active.conf write:', e); }
    return { saved: true };
  }

  function exportText() { return { config: serializeConfig(), list: JSON.stringify(names, null, 2) }; }

  /* ─── Backup / Restore ─────────────────────────────────────────────────
     Export the live config to BACKUP_DIR as a timestamped pair
     (COPG-<ts>.json + list-<ts>.json); import reads a chosen backup back in,
     validates the JSON, then re-saves through save() so the same chmod/chcon
     path runs and the in-memory model stays the single source of truth.
     In browser preview there's no filesystem: backupConfig() returns the data
     so the UI can offer a blob download, listBackups() is empty, and restore
     is driven by a file <input> via restoreFromText(). Designed so new export
     targets (e.g. extra files) only need a new entry in currentSnapshot(). ─── */

  function stamp() {
    const d = new Date(), p = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}` +
           `-${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`;
  }

  function isPlainObject(o) { return !!o && typeof o === 'object' && !Array.isArray(o); }

  // Decide whether a parsed JSON object is a COPG.json (config) or a list.json
  // (display names) WITHOUT trusting the filename. Signals, strongest first:
  //   • any PACKAGES_* key or a cpu_spoof key → config (unambiguous shape)
  //   • every value is a string               → list   (flat pkg→name map)
  //   • any non-string (array/object) value   → config (config-shaped)
  //   • empty {}                              → list, but ambiguous (let the
  //     user confirm — an empty file could legitimately be either one).
  function detectKind(obj) {
    if (!isPlainObject(obj)) return { kind: 'config', ambiguous: false };
    const keys = Object.keys(obj);
    if (keys.some(k => /^PACKAGES_/i.test(k) || k === 'cpu_spoof'))
      return { kind: 'config', ambiguous: false };
    if (keys.length === 0) return { kind: 'list', ambiguous: true };
    if (Object.values(obj).every(v => typeof v === 'string'))
      return { kind: 'list', ambiguous: false };
    return { kind: 'config', ambiguous: false };
  }

  // Parse + classify a backup file's text. Throws on bad JSON / non-object so
  // the caller can surface an "invalid file" message. The import-from-file flow
  // uses this to show the detected type AND offer the user an override.
  function inspectBackup(text) {
    const obj = JSON.parse(text);                  // throws on malformed JSON
    if (!isPlainObject(obj)) throw new Error('Not a JSON object');
    const d = detectKind(obj);
    return { obj, kind: d.kind, ambiguous: d.ambiguous };
  }

  function applyConfigObject(obj) {
    config = isPlainObject(obj) ? obj : {};
    keyOrder = Object.keys(config);
  }
  function applyNamesObject(obj) { names = isPlainObject(obj) ? obj : {}; }

  // Snapshot of what export would write — the one place that lists backup files.
  function currentSnapshot() {
    const ts = stamp();
    return {
      ts,
      files: [
        { name: `COPG-${ts}.json`, kind: 'config', data: serializeConfig() },
        { name: `list-${ts}.json`, kind: 'list',   data: JSON.stringify(names, null, 2) },
      ],
    };
  }

  async function backupConfig() {
    const snap = currentSnapshot();
    if (!hasBridge()) return { preview: true, ts: snap.ts, files: snap.files };  // UI → blob download
    await execCommand(`mkdir -p ${BACKUP_DIR}`);
    const written = [];
    for (const f of snap.files) {
      const path = `${BACKUP_DIR}/${f.name}`;
      await execCommand(`echo '${shq(f.data)}' > ${path}`);
      written.push({ name: f.name, path, kind: f.kind });
    }
    return { preview: false, ts: snap.ts, dir: BACKUP_DIR, files: written };
  }

  // Device-only: list *.json in BACKUP_DIR, grouped into restorable sets.
  // Timestamped COPG-/list- files pair up by their <ts>; any other *.json file
  // is its own single-file entry (kind auto-detected at restore time).
  async function listBackups() {
    if (!hasBridge()) return [];
    let out = '';
    try { out = await execCommand(`ls -1 ${BACKUP_DIR}`); } catch (_) { return []; }
    const files = String(out).split('\n').map(s => s.trim()).filter(s => /\.json$/i.test(s));
    const sets = new Map();
    files.forEach(name => {
      const m = name.match(/^(COPG|list)-(\d{8}-\d{6})\.json$/i);
      const key = m ? m[2] : name;
      if (!sets.has(key)) sets.set(key, { ts: m ? m[2] : null, configFile: null, listFile: null, file: null });
      const set = sets.get(key);
      if (m && /^COPG$/i.test(m[1]))      set.configFile = name;
      else if (m && /^list$/i.test(m[1])) set.listFile = name;
      else                                set.file = name;
    });
    return [...sets.values()].sort((a, b) => {
      if (a.ts && b.ts) return b.ts.localeCompare(a.ts);   // newest timestamp first
      if (a.ts) return -1;
      if (b.ts) return 1;
      return (b.file || '').localeCompare(a.file || '');
    });
  }

  async function readApplyFile(name, forcedKind) {
    const text = await execCommand(`cat ${BACKUP_DIR}/${name}`);
    const obj  = JSON.parse(text);                       // throws on malformed → caller catches
    const kind = forcedKind || detectKind(obj).kind;
    if (kind === 'config') applyConfigObject(obj); else applyNamesObject(obj);
    return kind;
  }

  // Restore an entry from listBackups(); re-saves through save().
  async function restoreBackup(entry) {
    if (!hasBridge()) return { saved: false, preview: true, restored: [] };
    const restored = [];
    if (entry.configFile || entry.listFile) {
      if (entry.configFile) restored.push(await readApplyFile(entry.configFile, 'config'));
      if (entry.listFile)   restored.push(await readApplyFile(entry.listFile, 'list'));
    } else if (entry.file) {
      restored.push(await readApplyFile(entry.file, null));
    }
    const res = await save();
    return { ...res, restored };
  }

  // Device-only: delete a backup entry's file(s) from BACKUP_DIR. A timestamped
  // pair removes both COPG-/list- files; a single-file entry removes just it.
  // Filenames come from listBackups() (an `ls` of BACKUP_DIR, *.json only), so
  // they're the same trusted set the restore path already `cat`s.
  async function deleteBackup(entry) {
    if (!hasBridge()) return { deleted: false, preview: true, removed: [] };
    const removed = [];
    const rm = async name => { if (name) { await execCommand(`rm -f ${BACKUP_DIR}/${name}`); removed.push(name); } };
    if (entry.configFile) await rm(entry.configFile);
    if (entry.listFile)   await rm(entry.listFile);
    if (entry.file)       await rm(entry.file);
    return { deleted: true, preview: false, removed };
  }

  // Restore from raw text (file <input> / pasted JSON). Auto-detects the kind
  // unless the caller forces it ('config' | 'list') — the import UI passes the
  // user's chosen type so a wrong guess can never silently overwrite the other
  // file.
  async function restoreFromText(text, forcedKind) {
    const obj = JSON.parse(text);                        // throws on malformed
    if (!isPlainObject(obj)) throw new Error('Not a JSON object');
    const kind = forcedKind || detectKind(obj).kind;
    if (kind === 'config') applyConfigObject(obj); else applyNamesObject(obj);
    const res = await save();
    return { ...res, restored: [kind] };
  }

  // Download one URL to stdout and parse it as JSON. Tries curl, then wget,
  // then busybox wget — each a SINGLE simple command (no pipes/compound), per
  // the bridge constraint. A 404/HTML body fails JSON.parse → next downloader.
  async function fetchRemoteJson(url) {
    const cmds = [
      `curl -fsSL ${url}`,        // -f: fail on HTTP error, -L: follow redirects
      `wget -qO- ${url}`,
      `busybox wget -qO- ${url}`,
    ];
    let lastErr = null;
    for (const cmd of cmds) {
      try {
        const out = await execCommand(cmd);
        const obj = JSON.parse(String(out).trim());   // throws on empty/non-JSON
        if (!isPlainObject(obj)) throw new Error('remote payload is not a JSON object');
        return obj;
      } catch (e) { lastErr = e; }                     // fall through to next downloader
    }
    throw lastErr || new Error('no downloader available');
  }
  // Raw-text fetch (for cpuinfo files / manifests that aren't a plain object).
  async function fetchRemoteText(url) {
    const cmds = [`curl -fsSL ${url}`, `wget -qO- ${url}`, `busybox wget -qO- ${url}`];
    let lastErr = null;
    for (const cmd of cmds) {
      try { const t = String(await execCommand(cmd)); if (!t.trim()) throw new Error('empty'); return t; }
      catch (e) { lastErr = e; }
    }
    throw lastErr || new Error('no downloader available');
  }
  // Write a synced CPU profile file (bind-mount-safe: 0444 + system_file context,
  // like customize.sh / importCpuModel). base64 to dodge shell quoting.
  async function writeSyncedFile(path, body, mode) {
    await execCommand(`echo '${b64(body)}' | base64 -d > ${path}`);
    try { await execCommand(`chmod ${mode} ${path}`); await execCommand(`chcon u:object_r:system_file:s0 ${path}`); }
    catch (e) { console.warn('synced file chmod/chcon:', e); }
  }

  // Save the in-app console buffer to a timestamped .txt under LOG_DIR
  // (mirrors old.js's saveLogToFile, minus its existence-probing — the seconds
  // in stamp() make collisions effectively impossible, so no compound `ls`).
  // Browser preview has no FS → return the text for a blob download.
  async function saveLog(text) {
    const name = `COPG-LOG-${stamp()}.txt`;
    if (!hasBridge()) return { preview: true, name, data: text };
    await execCommand(`mkdir -p ${LOG_DIR}`);
    const path = `${LOG_DIR}/${name}`;
    await execCommand(`echo '${shq(text)}' > ${path}`);
    try { await execCommand(`chmod 644 ${path}`); } catch (e) { console.warn('log chmod:', e); }
    return { preview: false, name, path };
  }

  // "Sync from GitHub": pull the latest config set from the repo and adopt it.
  // COPG.json + list.json are the CORE (fetched first — a failure here throws and the
  // caller shows a fail toast). The CPU profile library + GPU manifest are extra and
  // BEST-EFFORT (a missing file never fails the core sync). CPU is manifest-driven:
  // we pull CPU/manifest.json then every cpuinfo_<key> it names, so new profiles in
  // the repo sync with no WebUI change.
  async function syncFromGitHub() {
    if (!hasBridge()) return { saved: false, preview: true, synced: [] };
    const cfg = await fetchRemoteJson(SYNC_CONFIG_URL);   // core: throws → fail toast
    const lst = await fetchRemoteJson(SYNC_LIST_URL);
    const synced = ['config', 'list'];

    // CPU profile library (manifest-driven, best-effort)
    try {
      const cpuTxt = await fetchRemoteText(SYNC_CPU_MANIFEST_URL);
      const models = (JSON.parse(cpuTxt).models || []).filter(m => m && m.key);
      await execCommand(`mkdir -p ${CPU_DIR}`);
      let got = 0;
      for (const m of models) {
        try {
          const body = await fetchRemoteText(`${SYNC_CPU_FILE_BASE}cpuinfo_${m.key}`);
          if (body.trim()) { await writeSyncedFile(`${CPU_DIR}/cpuinfo_${m.key}`, body, '0444'); got++; }
        } catch (e) { console.warn(`sync cpu ${m.key}:`, e); }
      }
      await writeSyncedFile(`${CPU_DIR}/manifest.json`, cpuTxt, '0644');
      cpuModelsCache = models;
      if (got) synced.push(`cpu(${got})`);
    } catch (e) { console.warn('sync CPU library:', e); }

    // GPU manifest (reference; only on JSON once GPU is productized → 404 = skip)
    try {
      const gpuTxt = await fetchRemoteText(SYNC_GPU_MANIFEST_URL);
      const gpuMf = JSON.parse(gpuTxt);
      if (Array.isArray(gpuMf)) {
        await execCommand(`mkdir -p ${GPU_DIR}`);
        await writeSyncedFile(`${GPU_DIR}/manifest.json`, gpuTxt, '0644');
        gpuModelsCache = gpuMf.filter(m => m && m.key && m.name);
        synced.push('gpu');
      }
    } catch (e) { /* GPU not on the JSON branch yet — silent skip */ }

    applyConfigObject(cfg);
    applyNamesObject(lst);
    const res = await save();
    return { ...res, synced };
  }

  /* ─── System info (real device data) ───────────────────────────────────
     Built from SIMPLE single-purpose bridge calls only — the same kind of
     plain command (`cat`, `getprop`, `uname`) that loadAll() proves works on
     this device. NO compound `;`/`$(...)`/`exit 0` scripts: some ksu.exec
     bridges choke on those (that's why the System tab stayed on "—" while the
     plain `cat` config load succeeded). Each value is read independently, so
     one failing call never blanks the rest.
       • getprop                  → Android/SDK + ABI list (ro.product.cpu.abilist)
       • uname -r                 → kernel
       • cat module.prop          → COPG version/code/author/description
       • cat <zygisk>/module.prop → Zygisk variant name (+ disable file)
       • ROOT BINARIES ARE NOT ON $PATH under ksu.exec — call them by absolute
         path: /data/adb/ksud, /data/adb/apd, /data/adb/magisk/magisk.        */
  let sysCache = null;

  // Absolute paths — root binaries are NOT on $PATH inside the ksu.exec shell.
  // `args` is a list: the binary is run once per arg and the outputs are joined
  // before parsing, because the version NAME and version CODE often come from
  // DIFFERENT subcommands:
  //   • KernelSU: `ksud -V` → name (e.g. 3.2.4); `ksud debug version` → code (32473)
  //   • Magisk:   `magisk -c` → name (e.g. 27.0); `magisk -V` → code (27000)
  const ROOT_BINS = {
    APatch:   { bin: '/data/adb/apd',            args: ['-V'] },
    KernelSU: { bin: '/data/adb/ksud',           args: ['-V', 'debug version'] },
    Magisk:   { bin: '/data/adb/magisk/magisk',  args: ['-c', '-V'] },
  };

  function cleanVer(s) { return (s || '').replace(/^(ksud?|kernelsu|apd?|magisk|version)[:\s]*/i, '').trim(); }

  /* Kernel release + architecture, e.g. "5.10.404R aarch64" (uname -r + uname -m). */
  function fullKernel(release, arch) {
    const r = (release || '').trim(), a = (arch || '').trim();
    return a ? `${r} ${a}`.trim() : r;
  }

  /* Parse SurfaceFlinger's "GLES: <vendor>, <renderer>, <version>" line into its
     three GL strings. vendor = up to the 1st comma, renderer = up to the 2nd,
     version = the rest (renderer never has a comma; the version's git-hash tail
     might, so only the first two commas split). Null when the line is absent. */
  function parseGles(line) {
    const i = (line || '').indexOf('GLES:');
    if (i < 0) return null;
    const rest = line.slice(i + 5).trim();
    const c1 = rest.indexOf(',');
    if (c1 < 0) return null;
    const vendor = rest.slice(0, c1).trim();
    const after = rest.slice(c1 + 1).trim();
    const c2 = after.indexOf(',');
    const renderer = (c2 < 0 ? after : after.slice(0, c2)).trim();
    const version = c2 < 0 ? '' : after.slice(c2 + 1).trim();
    return { vendor, renderer, version };
  }

  /* Normalise a root-tool version string to "x.y.z(code)" when both are present.
     Input is the joined output of the tool's version subcommands (see ROOT_BINS),
     so it may contain the name and code in any order/separator; we extract the
     semver and the first standalone 4+ digit code and surface whatever we read. */
  function normRootVer(raw) {
    const s = (raw || '').replace(/\s+/g, ' ').trim();
    if (!s) return '';
    // 1) the semantic version, e.g. 3.2.4 (or v1.0.5)
    const sm = s.match(/v?(\d+\.\d+(?:\.\d+)?)/);
    const ver = sm ? sm[1] : '';
    // 2) the version CODE = any standalone run of 4+ digits that ISN'T part of
    //    the semver. Remove the semver first, then grab the first long number —
    //    this catches every separator (paren/colon/dot/dash/space): 3.2.4(32473),
    //    32473:3.2.4, v3.2.4-32473, 3.2.4.32473 …
    const rest = sm ? s.replace(sm[0], ' ') : s;
    const cm = rest.match(/(\d{4,})/);
    const code = cm ? cm[1] : '';
    if (ver && code) return `${ver}(${code})`;
    if (ver)  return ver;
    if (code) return code;
    return cleanVer(s);
  }

  // one prop out of a `getprop` dump: lines look like `[key]: [value]`
  function propOf(dump, key) {
    const m = (dump || '').match(new RegExp('\\[' + key.replace(/\./g, '\\.') + '\\]:\\s*\\[([^\\]]*)\\]'));
    return m ? m[1].trim() : '';
  }
  // one field out of a module.prop / build.prop style `key=value` dump
  function fieldOf(dump, key) {
    const m = (dump || '').match(new RegExp('^' + key + '=(.*)$', 'm'));
    return m ? m[1].trim() : '';
  }

  // best-effort single command → text ('' on any failure), never throws.
  // A non-zero exit here is EXPECTED, not an error: `code 127` just means that
  // root binary isn't installed (e.g. apd/magisk on a KSU device), `code 1`
  // means an optional file is absent (a `disable` flag, or a not-installed
  // zygisk variant). So these are logged at info level (gated behind Debug
  // Logs), never as warnings/errors — they're normal probing, not failures.
  async function tryCmd(cmd) {
    try { return (await execCommand(cmd)) || ''; }
    catch (e) { try { console.log(`sysinfo: "${cmd}" → ${e}`); } catch (_) {} return ''; }
  }

  /* Detect a root solution by its absolute binary; returns its version
     (name+code, normalised) or null when the binary isn't present. Runs each
     configured subcommand and joins the outputs so normRootVer sees both the
     version name and the version code. A missing binary yields no output. */
  async function detectRoot(name) {
    const { bin, args } = ROOT_BINS[name];
    let combined = '';
    for (const a of args) {
      const out = (await tryCmd(`${bin} ${a} 2>/dev/null`)).trim();
      if (out) combined += ' ' + out;
    }
    combined = combined.trim();
    if (!combined) return null;               // binary absent / no output
    return { name, version: normRootVer(combined) };
  }

  async function readZygisk(dir, label) {
    const prop = await tryCmd(`cat ${dir}/module.prop 2>/dev/null`);
    if (!prop.trim()) return null;                       // dir/module.prop absent
    const off = (await tryCmd(`ls ${dir}/disable 2>/dev/null`)).trim() !== '';
    // Proper NAME from module.prop (authoritative — e.g. "BreZygisk" on the shared `rezygisk`
    // dir, not the hardcoded fallback), VERSION from the variant's own daemon controller when
    // present: Zygisk Next → `zygiskd status` (version_local), ReZygisk/forks → `zygisk-ptracer64
    // info` ("The ReZygisk Tracer <ver>") or `zygiskd64 version`. Unknown/future variants keep the
    // module.prop version.
    const name = fieldOf(prop, 'name') || label;
    let version = cleanVer(fieldOf(prop, 'version'));
    for (const c of [`${dir}/bin/zygiskd status`, `${dir}/bin/zygisk-ptracer64 info`, `${dir}/bin/zygisk-ptracer32 info`, `${dir}/bin/zygiskd64 version`, `${dir}/bin/zygiskd version`]) {
      const out = (await tryCmd(`${c} 2>/dev/null`)).trim();
      if (!out) continue;
      const m = out.match(/version_local:\s*(\S+)/) || out.match(/[0-9]+\.[0-9][0-9A-Za-z.\-]*/);
      if (m) { version = cleanVer(m[1] || m[0]); break; }
    }
    return { variant: name, version, on: !off };
  }

  async function getSystemInfo() {
    if (sysCache) return sysCache;
    if (!hasBridge()) return null;                       // preview: keep placeholders

    // Fire the independent reads together; each is a plain, single command.
    // GPU: glGetString needs a live GL context (a Java/EGL call we can't make
    // from shell), but SurfaceFlinger already holds one and prints the strings —
    // grep the single "GLES: <vendor>, <renderer>, <version>" line. Match the
    // COLON ("GLES:") so we skip SF's other "------RE GLES (Ganesh)------" header
    // line (no colon) — grepping bare "GLES" caught that header first → parse
    // failed → "—". grep also keeps the huge dump from tripping the ksu.exec
    // large-stdout timeout.
    const [props, kernel, arch, modProp, apd, ksud, magisk, zNext, reZy, gles, suv] = await Promise.all([
      tryCmd('getprop'),
      tryCmd('uname -r'),
      tryCmd('uname -m'),
      tryCmd('cat /data/adb/modules/COPG/module.prop 2>/dev/null'),
      detectRoot('APatch'),
      detectRoot('KernelSU'),
      detectRoot('Magisk'),
      readZygisk('/data/adb/modules/zygisksu', 'Zygisk Next'),
      readZygisk('/data/adb/modules/rezygisk', 'ReZygisk'),
      tryCmd('dumpsys SurfaceFlinger 2>/dev/null | grep -m1 GLES:'),
      // `su -v` reports the CURRENTLY-RUNNING su provider, immune to leftover
      // /data/adb/{apd,ksud,magisk} binaries from a previous root (the bug: a stale
      // binary still execs → detectRoot sees it → wrong primary). Suffixes are distinct:
      // :MAGISKSU / :KernelSU / :APatch (verified in each project's su source).
      tryCmd('su -v 2>/dev/null'),
    ]);
    const gl = parseGles(gles);

    const android = propOf(props, 'ro.build.version.release');
    const sdk     = propOf(props, 'ro.build.version.sdk');
    // ABI: prefer the full abilist (what the user asked for), first entry shown.
    const abilist = propOf(props, 'ro.product.cpu.abilist');
    const abi     = abilist || propOf(props, 'ro.product.cpu.abi');

    // Root: the ACTIVE manager is whichever provides the running `su` (su -v suffix),
    // not whichever leftover binary happens to still exec. Pick that as primary so a
    // root switch (e.g. KSU → Magisk) with stale /data/adb/apd or ksud around no longer
    // shows the wrong manager. Fall back to priority APatch → KernelSU → Magisk (mirrors
    // customize.sh) only when su -v is unavailable (restricted PATH / no su on-device).
    const detected = [apd, ksud, magisk].filter(Boolean);
    const suProvider = /:MAGISKSU\b/.test(suv) ? 'Magisk'
                     : /:KernelSU\b/.test(suv) ? 'KernelSU'
                     : /:APatch\b/i.test(suv)  ? 'APatch'
                     : null;
    const active = suProvider ? detected.find(r => r.name === suProvider) : null;
    // Primary = the active root (even if detectRoot couldn't read its version, keep the
    // name from su -v), else the priority fallback. Everything else is a leftover/secondary.
    const primary = active
                  || (suProvider ? { name: suProvider, version: '' } : null)
                  || detected[0]
                  || null;
    const roots = primary ? [primary, ...detected.filter(r => r.name !== primary.name)] : detected;
    const secondary = primary ? roots.filter(r => r.name !== primary.name) : [];

    // Zygisk: prefer an active (enabled) variant over a disabled one. Name only.
    const zygCandidates = [zNext, reZy].filter(Boolean);
    let zygisk = { variant: 'Not installed', version: '', on: false };
    if (zygCandidates.length) zygisk = zygCandidates.find(z => z.on) || zygCandidates[0];
    else if (primary && primary.name === 'Magisk') zygisk = { variant: 'Magisk Zygisk', version: '', on: true };

    // ── REAL module status (not just "the folder exists"). Reads, each a simple single command
    // (no compound `;`/`$()` — some ksu.exec bridges choke on those): the manager's disable/remove
    // flags, the native boot heartbeat (copg_hb: the boot_id the .so stamped this boot, or the
    // REBOOT sentinel customize.sh writes on install), the live kernel boot_id to compare against,
    // and the system_server-hook boot-crash counters. Status:
    //   active     — heartbeat boot_id == current boot → the .so loaded + ran THIS boot
    //   degraded   — active, but a hook self-disabled after boot crashes (bootguard tripped)
    //   reboot     — freshly installed (REBOOT sentinel), needs a reboot to load
    //   notloaded  — present + enabled + Zygisk on, but no current-boot heartbeat (bad preload / crash)
    //   disabled   — manager toggle off (disable file)
    //   removing   — marked for removal on next reboot (remove file)
    //   zygisk_off — no active Zygisk variant to load the module
    let moduleStatus = 'active';
    const bridge = hasBridge();
    if (bridge) {
      const [disRaw, remRaw, hbRaw, bootRaw, bgRaw] = await Promise.all([
        tryCmd('ls /data/adb/modules/COPG/disable 2>/dev/null'),
        tryCmd('ls /data/adb/modules/COPG/remove 2>/dev/null'),
        tryCmd('cat /data/system/copg_hb 2>/dev/null'),
        tryCmd('cat /proc/sys/kernel/random/boot_id 2>/dev/null'),
        tryCmd('cat /data/system/copg_fs_bootguard /data/system/copg_vpn_bootguard /data/system/copg_lang_bootguard /data/system/copg_playsrc_bootguard 2>/dev/null'),
      ]);
      const hb   = (hbRaw || '').trim();
      const live = (bootRaw || '').trim();
      const bgMax = Math.max(0, ...String(bgRaw || '').split(/\s+/).map(x => parseInt(x, 10) || 0));
      if      (remRaw.trim() !== '')            moduleStatus = 'removing';
      else if (disRaw.trim() !== '')            moduleStatus = 'disabled';
      else if (!(zygisk && zygisk.on))          moduleStatus = 'zygisk_off';
      else if (hb === 'REBOOT')                 moduleStatus = 'reboot';
      else if (hb && live && hb === live)       moduleStatus = (bgMax >= 3 ? 'degraded' : 'active');
      else if (hb && live)                      moduleStatus = 'notloaded';   // present but ≠ live boot → ran a prior boot, not this one = crashed/failed to load
      else                                      moduleStatus = 'active';       // heartbeat absent (older .so without the feature, or unreadable) → benefit of the doubt, no false red
    }
    const moduleOff = moduleStatus !== 'active' && moduleStatus !== 'degraded';

    sysCache = {
      androidVer: android ? `${android}${sdk ? ` (API ${sdk})` : ''}` : '—',
      sdk,
      abi: abi || '—',
      fingerprint: propOf(props, 'ro.build.fingerprint'),
      kernel: fullKernel(kernel, arch) || '—',
      // real GPU (what the gpu=<key> spoof fakes): renderer is the model shown,
      // vendor/version kept for future rows / copy-into-profile.
      gpu: (gl && gl.renderer) || '—',
      gpuVendor: gl ? gl.vendor : '',
      gpuVersion: gl ? gl.version : '',
      roots, primary, secondary,
      rootEnv: primary ? primary.name : 'Unknown',
      rootVersion: primary ? primary.version : '',
      // e.g. "KernelSU 3.2.4(32473)" — the root solution + its version
      rootVersionLabel: primary ? `${primary.name}${primary.version ? ' ' + primary.version : ''}` : '—',
      zygisk,
      // "Zygisk Next (v1.2.2)" — variant name + version when known
      zygiskLabel: zygisk.version
        ? `${zygisk.variant} (${/^v/i.test(zygisk.version) ? zygisk.version : 'v' + zygisk.version})`
        : zygisk.variant,
      version: cleanVer(fieldOf(modProp, 'version')),
      versionCode: fieldOf(modProp, 'versionCode'),
      author: fieldOf(modProp, 'author'),
      description: fieldOf(modProp, 'description'),
      moduleActive: !moduleOff,
      moduleStatus,
    };
    return sysCache;
  }

  /* ─── Installed packages (Set of package names) ─────────────────────────
     The host's package API differs by environment, so every call below is
     async-tolerant (await Promise.resolve — MMRL WebUI-X interfaces are
     Promise-based) and we try, in order: the KernelSU `ksu` API, the WebUI-X
     `$packageManager`, then a `pm` shell fallback. A diagnostic line is logged
     so the in-app Console shows exactly which interface answered. */
  let installedCache = null;
  let installedByUser = null;   // Map<userId(int), Set<clean>> — owner + other users (pm --user)
  let usersCache = null;        // [{id:int, name:string}] from the probe / pm list users
  let probeCache = null;        // {users:[{id,name,type}], byUser:Map<int,Set<clean>>} | false (failed)

  /* PRIMARY user + per-user-package source: a tiny root Java helper (src/userprobe →
     /data/adb/modules/COPG/userprobe.dex) run via app_process. It talks straight to the
     IUserManager + IPackageManager binders as root, so it needs no `pm` shell and is immune to
     the three things that broke the shell path: `pm list users` transaction-blocks (Motorola),
     the --user/--profile split (it returns each user's TYPE), and the KSU/SukiSU mount-namespace
     isolation that makes `pm list packages --user` return empty in the WebUI's ksu.exec. One call
     yields every user (with type) + every user's packages. Falls back to the shell path on any
     failure (dex missing, app_process error, {"error":…}). Cached; device-only. */
  async function runProbe() {
    if (probeCache !== null) return probeCache;
    probeCache = false;
    if (!hasBridge()) return false;
    try {
      const raw = await spawnCommand('CLASSPATH=/data/adb/modules/COPG/userprobe.dex app_process /system/bin com.copg.userprobe.Main');
      const o = JSON.parse(String(raw || '').trim());
      if (!o || o.error || !Array.isArray(o.users) || !o.users.length) {
        if (o && o.error) dlog('userprobe error: ' + o.error, 'warn');
        return false;
      }
      const users = o.users.map(u => ({
        id: u.id,
        name: (u.name && u.name !== 'null') ? u.name : (u.id === 0 ? 'Owner' : 'User ' + u.id),
        type: u.type || '',
      })).sort((a, b) => a.id - b.id);
      const byUser = new Map();
      if (o.packages) for (const k of Object.keys(o.packages))
        byUser.set(parseInt(k, 10), new Set(o.packages[k]));
      const system = new Set(Array.isArray(o.system) ? o.system : []);
      probeCache = { users, byUser, system };
      dlog('userprobe: users=[' + users.map(u => u.id).join(',') + '] ' +
        [...byUser].map(([k, v]) => 'u' + k + '=' + v.size).join(' '), 'success');
      return probeCache;
    } catch (e) { dlog('userprobe failed (→ shell fallback): ' + e, 'warn'); return false; }
  }

  /* Android users on the device (owner + work-profile / ROM-clone / dual-app spaces),
     for the package modal's user-scope selector and the picker's per-user fan-out.
     Owner (0) is always present. Device-only (needs root pm); preview → just owner. Cached.

     Three sources, first that yields real users wins — because some ROMs (verified on
     Motorola / Android 16) BLOCK `pm list users` even for root with
     "Failure calling service package: Failed transaction (2147483646)", and their clone
     spaces are PROFILE users in the 900+ range. `dumpsys user` and the on-disk
     /data/system/users dirs still expose them. All three carry the same
     `UserInfo{<id>:<name>:...}` shape, so one parser covers pm + dumpsys. */
  async function getInstalledUsers() {
    if (usersCache) return usersCache;
    // PRIMARY: the root binder probe (returns users + types, ns-immune). Shell path is fallback.
    const probed = await runProbe();
    if (probed && probed.users.length) { usersCache = probed.users; return usersCache; }
    let users = [{ id: 0, name: 'Owner' }];
    if (hasBridge()) {
      const parseUserInfo = (out) => {
        const map = new Map();
        const re = /UserInfo\{(\d+):([^:}]*):/g;
        let m;
        while ((m = re.exec(out || '')) !== null) {
          const id = parseInt(m[1], 10);
          let name = (m[2] || '').trim();
          if (!name || name === 'null') name = id === 0 ? 'Owner' : ('User ' + id);
          if (!map.has(id)) map.set(id, { id, name });
        }
        return [...map.values()];
      };
      let found = [];
      // 1) pm list users — fastest, but transaction-blocked on some ROMs.
      try { found = parseUserInfo(await execCommand('pm list users')); }
      catch (e) { dlog('pm list users failed: ' + e, 'warn'); }
      // 2) dumpsys user — works where pm is blocked (single pipe keeps stdout small; the
      //    regex ignores the non-UserInfo{ lines grep lets through). Owner alone → retry.
      if (found.length < 2) {
        try {
          const alt = parseUserInfo(await execCommand('dumpsys user | grep -i UserInfo'));
          if (alt.length > found.length) found = alt;
        } catch (e) { dlog('dumpsys user failed: ' + e, 'warn'); }
      }
      // 3) /data/system/users/*.xml dirs — last resort; numeric names = user ids.
      if (found.length < 2) {
        try {
          const map = new Map(found.map(u => [u.id, u]));
          (await execCommand('ls /data/system/users') || '').split(/\s+/).forEach(t => {
            if (/^\d+$/.test(t)) {
              const id = parseInt(t, 10);
              if (!map.has(id)) map.set(id, { id, name: id === 0 ? 'Owner' : ('User ' + id) });
            }
          });
          if (map.size > found.length) found = [...map.values()];
        } catch (e) { dlog('ls /data/system/users failed: ' + e, 'warn'); }
      }
      if (found.length) users = found.sort((a, b) => a.id - b.id);
    }
    usersCache = users;
    return users;
  }

  /* Module interface via the SANITIZED module id (MMRL WebUI-X).
     https://mmrl.dev/guide/webuix/sanitized-ids — the host injects the module's
     interface under `$<sanitized id>`, where every char outside [a-zA-Z0-9_] in
     the module id is replaced with `_`. For module "COPG" that is `$COPG`. */
  const MODULE_ID = 'COPG';
  const SANITIZED_ID = MODULE_ID.replace(/[^a-zA-Z0-9_]/g, '_');
  function moduleInterface() { return w['$' + SANITIZED_ID] || w.$COPG || w.$copg || null; }

  /* Android ApplicationInfo flags (https://mmrl.dev/guide/webuix/application-info-flags) */
  const APP_FLAG_SYSTEM = 0x00000001;
  const APP_FLAG_UPDATED_SYSTEM_APP = 0x00000080;
  function flagsAreSystem(flags) {
    const f = Number(flags) || 0;
    return (f & APP_FLAG_SYSTEM) !== 0 || (f & APP_FLAG_UPDATED_SYSTEM_APP) !== 0;
  }

  const dlog = (m, t) => { try { if (w.logEvent) w.logEvent(m, t || 'info'); } catch (_) {} };

  /* Normalise whatever a package source returns into a clean string[] of
     package names. Handles: a JS array (already parsed), a `{packages:[…]}`
     object, a JSON-string array (ksu.listPackages returns a JSON STRING — see
     kernelsu-alt: it does `JSON.parse(ksu.listPackages(type))`), and a newline
     list from `pm list packages` whose lines look like `package:com.foo`. */
  function normalizePkgList(raw) {
    let arr = [];
    if (Array.isArray(raw)) arr = raw;
    else if (raw && typeof raw === 'object' && Array.isArray(raw.packages)) arr = raw.packages;
    else if (typeof raw === 'string') {
      const s = raw.trim();
      if (s.startsWith('[')) { try { arr = JSON.parse(s); } catch (_) { arr = s.split('\n'); } }
      else arr = s.split('\n');               // `pm list packages` lines
    }
    return arr
      .map(p => (typeof p === 'string' ? p : (p && (p.packageName || p.package)) || ''))
      .map(s => s.replace(/^package:/, '').trim())   // strip `pm` prefix
      .filter(Boolean);
  }

  /* Diagnostic: which package interfaces are present? Logged once. */
  let apisLogged = false;
  function logApis() {
    if (apisLogged) return; apisLogged = true;
    dlog('APIs: ksu=' + (typeof ksu) +
         ' $packageManager=' + (typeof (w.$packageManager)) +
         ' $' + SANITIZED_ID + '=' + (typeof moduleInterface()) +
         ' bridge=' + hasBridge());
  }

  async function getInstalledPackages() {
    if (installedCache) return installedCache;
    logApis();
    // Package source: the root binder probe (runProbe) is PRIMARY — ONE call yields every user's
    // packages, manager-agnostic + ns-immune — fully retiring the KSU listPackages/getPackagesInfo
    // API. The only fallback is the manager-agnostic `pm` shell (owner list + per-user fan-out).
    installedByUser = new Map();
    installedByUser.set(0, new Set());
    const union = new Set();

    let probed = false;
    {
      const p = await runProbe();
      if (p && p.byUser.size) {
        for (const [id, set] of p.byUser) {
          if (!installedByUser.has(id)) installedByUser.set(id, new Set());
          set.forEach(x => { installedByUser.get(id).add(x); union.add(x); });
        }
        probed = true;
      }
    }

    // ── FALLBACK (probe unavailable): owner package list via the manager-agnostic `pm` shell.
    if (!probed && hasBridge()) {
      try {
        const arr = normalizePkgList(await spawnCommand('pm list packages'));
        arr.forEach(x => { installedByUser.get(0).add(x); union.add(x); });
        dlog('Installed packages (owner): ' + arr.length + ' via pm list packages', arr.length ? 'success' : 'warn');
      } catch (e) { dlog('pm list packages failed: ' + e, 'warn'); }
    }

    // ── FALLBACK when the host's API is owner-only: per non-owner user, `pm list packages
    // --user <id>` then, if that yields nothing, the user's app-data dir. Host-agnostic (root).
    // Skipped when the KSU path already found clones. Covers CLONE / work-profile / dual-app
    // users alike — matched by id, no per-type filter, so ROM clone profiles (900+) are included.
    if (!probed && hasBridge()) {
      try {
        const users = await getInstalledUsers();
        // A clone-user data path segment (com.x.y) — used to sieve `ls /data/user/<id>` output.
        const isPkgName = t => /^[a-zA-Z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$/.test(t);
        for (const u of users) {
          if (u.id === 0) continue;
          let list = [], src = '';
          // ROOT CAUSE (both Motorola clones + Xiaomi users 10-15): the KSU/SukiSU su subprocess
          // runs in an ISOLATED mount namespace, so `pm list packages --user <id>` returns EMPTY
          // even though /data/user/<id> is populated. Run the query via `su -M` (global ns) — the
          // fix that makes it match Termux root. Try both flags: CLONE-type profiles (Motorola
          // MyUI / OnePlus) need `--profile`; MANAGED_PROFILE (Xiaomi dual-apps) uses `--user`.
          for (const flag of ['--user', '--profile']) {
            try {
              list = normalizePkgList(await execMaster('pm list packages ' + flag + ' ' + u.id));
              if (list.length) { src = 'pm ' + flag + ' -M'; break; }
            } catch (e) { dlog('pm list packages ' + flag + ' ' + u.id + ' (-M) failed: ' + e, 'warn'); }
          }
          // Fallback where `su -M` is unavailable or the package service is blocked entirely: the
          // per-user app-data dir's subdirs ARE the packages installed for that user (root reads it).
          if (!list.length) {
            try {
              list = ((await execCommand('ls /data/user/' + u.id)) || '').split(/\s+/).filter(isPkgName);
              if (list.length) src = '/data/user dir';
            } catch (e) { dlog('ls /data/user/' + u.id + ' failed: ' + e, 'warn'); }
          }
          if (list.length) {
            installedByUser.set(u.id, new Set(list));
            list.forEach(p => union.add(p));
            dlog('User ' + u.id + ' (' + u.name + '): ' + list.length + ' packages (' + src + ')', 'info');
          }
        }
        try { console.warn('[COPG user-detect] fallback used, users=[' +
          [...installedByUser.keys()].sort((a, b) => a - b).join(',') + ']'); } catch (_) {}
      } catch (_) { /* getInstalledUsers already logs */ }
    }
    installedCache = union;
    return installedCache;
  }

  /* Which Android users a package is installed for (owner + clones), from installedByUser.
     Returns [ids]; empty if the maps aren't built yet. Used by the picker to hint a
     clone-only app and by the modal to default the user-scope selector sensibly. */
  function usersForPackage(cleanName) {
    if (!installedByUser) return [];
    const ids = [];
    for (const [id, set] of installedByUser) if (set.has(cleanName)) ids.push(id);
    return ids.sort((a, b) => a - b);
  }

  /* SYNC lookup of a user's own name (the system app-clone / profile name, e.g. "Clone 1", from
     UserInfo.name) for a clone id — for the library chip. Reads the cache the probe already filled
     (getInstalledPackages/getInstalledUsers, both run at boot). '' if unknown → caller falls back. */
  function userNameOf(id) {
    const n = parseInt(id, 10);
    if (!Number.isFinite(n) || n <= 0) return '';
    const u = (usersCache && usersCache.find(x => x.id === n)) ||
              (probeCache && probeCache.users && probeCache.users.find(x => x.id === n));
    return (u && u.name) ? u.name : '';
  }

  /* Fetch the SYSTEM desktop label (incl. the vendor clone-number suffix, e.g. "DevCheck 1") for
     every CONFIGURED clone entry (userScope>0), via the probe's per-user launcher label, into the
     userAppNames cache that nameFor falls back to. So a clone row shows its real desktop name with
     no manual renaming — where the ROM exposes it (some ROMs keep the suffix launcher-only, then a
     manual per-clone rename is the recourse). Cached per idPackage; call at load, then repaint. */
  const USERPROBE_DEX_ = '/data/adb/modules/COPG/userprobe.dex';
  async function loadCloneNames() {
    userAppNames = userAppNames || new Map();
    if (!hasBridge()) return userAppNames;
    const byUser = new Map();
    for (const p of listPackages()) {
      const uid = parseInt(p.userScope, 10);
      if (!Number.isFinite(uid) || uid <= 0) continue;
      if (userAppNames.has(idPackage(p.clean, p.userScope))) continue;        // already cached
      if (!/^[A-Za-z0-9._]+$/.test(p.clean)) continue;
      if (!byUser.has(uid)) byUser.set(uid, new Set());
      byUser.get(uid).add(p.clean);
    }
    for (const [uid, set] of byUser) {
      try {
        const raw = await spawnCommand('CLASSPATH=' + USERPROBE_DEX_ +
          ' app_process /system/bin com.copg.userprobe.Main labels ' + uid + ' ' + shq([...set].join(',')));
        let o = {}; try { o = JSON.parse(String(raw || '').trim()); } catch (_) {}
        for (const pk of Object.keys(o)) if (o[pk]) userAppNames.set(idPackage(pk, String(uid)), o[pk]);
      } catch (e) { dlog('loadCloneNames u' + uid + ' failed: ' + e, 'warn'); }
    }
    return userAppNames;
  }

  async function isInstalled(cleanName) { return (await getInstalledPackages()).has(cleanName); }

  /* Set of SYSTEM package names — fallback via `pm -s` when app flags aren't
     available from getPackagesInfo / getApplicationInfo. Cached. */
  let systemCache = null;
  async function getSystemPackages() {
    if (systemCache) return systemCache;
    // PRIMARY: the probe already flagged system apps (ApplicationInfo.FLAG_SYSTEM) in one pass.
    const p = await runProbe();
    if (p && p.system && p.system.size) { systemCache = p.system; return systemCache; }
    let arr = [];
    if (hasBridge()) {
      // fallback: plain command (no pipe) — normalizePkgList strips the `package:` prefix
      try { arr = normalizePkgList(await spawnCommand('pm list packages -s')); } catch (_) {}
    }
    systemCache = new Set(arr);
    return systemCache;
  }

  /* Best-effort single app label via the root userprobe (retires the KSU/$packageManager path). */
  async function getAppLabel(pkg) {
    try { const o = await probeLabels([pkg]); if (o[pkg]) return o[pkg]; } catch (_) { /* fall through */ }
    return null;
  }

  /* FAST installed-app list for the picker: [{pkg, label, system}] sorted A→Z.
     Only package names are awaited here (cheap), with list.json / package-name
     labels, so the picker shows immediately. Real labels + system flags are
     filled afterwards via enrichApps() (background). */
  async function listInstalledApps() {
    const set = await getInstalledPackages();
    // Classify system apps NOW (the probe already returned the FLAG_SYSTEM set in the same pass, so
    // getSystemPackages is instant here). Doing it up front makes the picker's User/System filter
    // correct on the FIRST paint — otherwise every row starts system:false and the default "User"
    // filter shows ALL apps for a frame, then shrinks when enrichApps fills the set (the flash).
    let sys = new Set();
    try { sys = await getSystemPackages(); } catch (_) {}
    return [...set]
      .map(pkg => ({ pkg, label: names[clean(pkg)] || pkg, system: sys.has(pkg) }))
      .sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase()));
  }

  /* Background enrichment for the picker: real app label + isSystem flag — now entirely from the
     root userprobe (retires ksu.getPackagesInfo / $packageManager.getApplicationInfo). Labels come
     from the probe's `labels` mode (each APK's real label, any user); the system set from the
     probe's FLAG_SYSTEM pass via getSystemPackages (which prefers the probe, falls back to pm -s).
     Returns { labels:{pkg->label}, system:Set }. Runs AFTER the list is on screen. */
  async function enrichApps(pkgs) {
    const labels = {};
    try { Object.assign(labels, await probeLabels(pkgs)); } catch (e) { dlog('enrich probeLabels failed: ' + e, 'warn'); }
    let system = new Set();
    try { system = await getSystemPackages(); } catch (_) {}
    dlog('enrich: probe → ' + Object.keys(labels).length + ' labels, ' + system.size + ' system', 'info');
    return { labels, system };
  }

  /* ─── Icon URLs (NOT base64) ───
     KsuWebUI's CSP blocks `data:` <img> but ALLOWS remote https <img>, so we let
     the WebView download the icon itself (off the bridge = no UI jank). We only
     use a bridge curl to SCRAPE the Play/F-Droid page for the icon URL (small
     stdout) — then hand the raw URL to an <img src>. The big base64 transfer +
     34KB localStorage write that lagged the UI are gone. */

  /* Scrape the Play page → direct play-lh icon URL (=s128). null on miss. */
  async function fetchPlayIconUrl(pkg) {
    if (!hasBridge()) return null;
    const p = shq(pkg);
    try {
      const found = await execCommand(
        `curl -sL --compressed --max-time 10 'https://play.google.com/store/apps/details?id=${p}&hl=en' 2>/dev/null | grep -o 'https://play-lh.googleusercontent.com/[^"\\\\=]*' | head -1`
      );
      const url = (found || '').trim().split('\n')[0];
      if (!url || !url.startsWith('https://play-lh')) return null;
      return `${url}=s128`;
    } catch (_) { return null; }
  }

  /* F-Droid icon URL fallback (open repo, no Play presence needed). */
  async function fetchFdroidIconUrl(pkg) {
    if (!hasBridge()) return null;
    const p = shq(pkg);
    try {
      const found = await execCommand(
        `curl -sL --compressed --max-time 10 'https://f-droid.org/en/packages/${p}/' 2>/dev/null | grep -o 'https://f-droid.org/repo/[^"]*\\.\\(png\\|webp\\)' | head -1`
      );
      const url = (found || '').trim().split('\n')[0];
      if (!url || !url.startsWith('https://f-droid.org/repo/')) return null;
      return url;
    } catch (_) { return null; }
  }

  /* Unified best-effort icon URL: Play first, then F-Droid. */
  async function fetchAppIconUrl(pkg) {
    return (await fetchPlayIconUrl(pkg)) || (await fetchFdroidIconUrl(pkg)) || null;
  }

  /* ─── Batch icon fetch to local files (device only) ───
     The lag comes from ONE `ksu.exec` (ksud fork) per icon. Instead fire a SINGLE
     detached background shell that downloads ALL missing icons to
     webroot/icons/<pkg>.png, then exits — JS just renders local <img src> (off
     the bridge). Returns fast (the worker is nohup-detached); the UI never waits.
     Files inherit the icons dir's system_file context (+ explicit chmod/chcon),
     so the WebView can read them and they survive offline. */
  const ICON_DIR_ABS = '/data/adb/modules/COPG/webroot/icons';
  const ICON_SCRIPT = '/data/adb/modules/COPG/icons_fetch.sh';
  // utf8-safe base64 of an ASCII script (avoids all shell-quoting of the body)
  function b64(s) { try { return btoa(unescape(encodeURIComponent(s))); } catch (_) { return btoa(s); } }
  // inverse of b64 — utf8-safe base64 decode (used by importDevice)
  function unb64(s) { try { return decodeURIComponent(escape(atob(s))); } catch (_) { return atob(s); } }

  async function batchFetchIcons(pkgs) {
    if (!hasBridge() || !pkgs || !pkgs.length) return false;
    // package names are [A-Za-z0-9._]; filter keeps the shell for-loop injection-safe
    const list = [...new Set(pkgs)].filter(p => /^[A-Za-z0-9._]+$/.test(p));
    if (!list.length) return false;
    const body = [
      '#!/system/bin/sh',
      'ICONS="' + ICON_DIR_ABS + '"',
      'UA="Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile"',
      'mkdir -p "$ICONS"',
      'chcon u:object_r:system_file:s0 "$ICONS" 2>/dev/null',
      'for p in ' + list.join(' ') + '; do',
      '  f="$ICONS/$p.png"',
      '  [ -s "$f" ] && continue',
      // 1) Google Play — square play-lh icon (best quality when present)
      '  u=$(curl -sL --compressed --max-time 10 "https://play.google.com/store/apps/details?id=$p&hl=en" 2>/dev/null | grep -o "https://play-lh.googleusercontent.com/[^\\"=]*" | head -1)',
      '  [ -n "$u" ] && curl -sL --compressed --max-time 10 "${u}=s128" -o "$f" 2>/dev/null',
      // 2) APKPure — package-keyed (-/<pkg> resolves the slug); the page\'s first
      //    app-icon-img is the real square icon. Needs a browser UA (else 403).
      '  if [ ! -s "$f" ]; then',
      '    a=$(curl -sL --compressed --max-time 12 -A "$UA" "https://apkpure.net/-/$p" 2>/dev/null | grep -o "app-icon-img\\"[^>]*src=\\"https://image.winudf.com/[^\\"]*\\"" | head -1 | sed "s/.*src=\\"//; s/\\"$//; s/&amp;/\\&/g")',
      '    [ -n "$a" ] && curl -sL --compressed --max-time 12 -A "$UA" "$a" -o "$f" 2>/dev/null',
      '  fi',
      // 3) F-Droid — open-source apps not on Play
      '  if [ ! -s "$f" ]; then',
      '    v=$(curl -sL --compressed --max-time 10 "https://f-droid.org/en/packages/$p/" 2>/dev/null | grep -o "https://f-droid.org/repo/[^\\"]*\\.\\(png\\|webp\\)" | head -1)',
      '    [ -n "$v" ] && curl -sL --compressed --max-time 10 "$v" -o "$f" 2>/dev/null',
      '  fi',
      '  if [ -s "$f" ]; then chmod 644 "$f"; chcon u:object_r:system_file:s0 "$f" 2>/dev/null; else rm -f "$f"; fi',
      'done',
      'touch "$ICONS/.done"',
      '',
    ].join('\n');
    try {
      await execCommand(`echo '${b64(body)}' | base64 -d > ${ICON_SCRIPT}`);
      // launch detached: the worker runs in the background, this returns at once
      await execCommand(`sh -c 'nohup sh ${ICON_SCRIPT} >/dev/null 2>&1 &'`);
      return true;
    } catch (_) { return false; }
  }

  /* ─── Render INSTALLED-app icons locally via the root userprobe (device only) ───
     app_process → com.copg.userprobe.Main icons <dir> <userId> <csv> renders each app's REAL
     icon (adaptive-icon aware) to webroot/icons/<pkg>.png — for ANY Android user, offline, no
     ksu:// scheme (so it also covers clone-only apps the owner-scoped ksu://icon can't). Grouped
     by a user that has each package (owner preferred). NON-installed apps are NOT handled here
     (their APK isn't on device) — those stay on batchFetchIcons. Returns {pkg:{label,ok}}. */
  const USERPROBE_DEX = '/data/adb/modules/COPG/userprobe.dex';
  async function probeRenderIcons(pkgs) {
    if (!hasBridge() || !pkgs || !pkgs.length) return {};
    const list = [...new Set(pkgs)].filter(p => /^[A-Za-z0-9._]+$/.test(p));
    if (!list.length) return {};
    // group each pkg under a user that has it installed (prefer owner 0)
    const byUser = new Map();
    for (const p of list) {
      let uid = 0;
      if (installedByUser && !(installedByUser.get(0) && installedByUser.get(0).has(p)))
        for (const [id, set] of installedByUser) if (set.has(p)) { uid = id; break; }
      if (!byUser.has(uid)) byUser.set(uid, []);
      byUser.get(uid).push(p);
    }
    try { await execCommand('mkdir -p ' + shq(ICON_DIR_ABS)); } catch (_) {}
    const out = {};
    for (const [uid, grp] of byUser) {
      try {
        const raw = await execCommand(
          'CLASSPATH=' + USERPROBE_DEX + ' app_process /system/bin com.copg.userprobe.Main icons ' +
          shq(ICON_DIR_ABS) + ' ' + uid + ' ' + shq(grp.join(',')));
        try { Object.assign(out, JSON.parse(String(raw || '').trim())); } catch (_) {}
        // make the freshly-written PNGs WebView-readable (system_file context, like the scraper)
        await execCommand("sh -c 'cd " + shq(ICON_DIR_ABS) +
          " 2>/dev/null && chmod 644 *.png 2>/dev/null; chcon u:object_r:system_file:s0 *.png 2>/dev/null; true'");
      } catch (e) { dlog('probeRenderIcons u' + uid + ' failed: ' + e, 'warn'); }
    }
    return out;
  }

  /* Resolve app display labels via the root userprobe `labels` mode (each APK's real label, any
     user) → {pkg:label}. Grouped by a user that has each package. Powers enrichApps / getAppLabel,
     retiring ksu.getPackagesInfo. Non-installed apps aren't resolvable here (no on-device APK). */
  const _labelCache = new Map();          // pkg -> resolved label (so reopening the picker is instant)
  // Launcher/desktop labels for a set of packages AS SEEN IN ONE specific user (userId), via the
  // probe's `labels` mode — for user>0 the probe returns the launcher label incl. the vendor clone
  // suffix ("设备模拟检测 1"). Used by the app picker to name each clone row distinctly. → {pkg:label}.
  async function probeLabelsForUser(pkgs, userId) {
    if (!hasBridge() || !pkgs || !pkgs.length) return {};
    const list = [...new Set(pkgs)].filter(p => /^[A-Za-z0-9._]+$/.test(p));
    if (!list.length) return {};
    try {
      const raw = await spawnCommand('CLASSPATH=' + USERPROBE_DEX +
        ' app_process /system/bin com.copg.userprobe.Main labels ' + (parseInt(userId, 10) || 0) + ' ' + shq(list.join(',')));
      return JSON.parse(String(raw || '').trim()) || {};
    } catch (e) { dlog('probeLabelsForUser u' + userId + ' failed: ' + e, 'warn'); return {}; }
  }

  async function probeLabels(pkgs) {
    if (!hasBridge() || !pkgs || !pkgs.length) return {};
    const all = [...new Set(pkgs)].filter(p => /^[A-Za-z0-9._]+$/.test(p));
    if (!all.length) return {};
    // serve cached labels immediately; only query the ones we haven't resolved yet
    const out = {};
    const list = [];
    for (const p of all) { if (_labelCache.has(p)) out[p] = _labelCache.get(p); else list.push(p); }
    if (!list.length) return out;
    const byUser = new Map();
    for (const p of list) {
      let uid = 0;
      if (installedByUser && !(installedByUser.get(0) && installedByUser.get(0).has(p)))
        for (const [id, set] of installedByUser) if (set.has(p)) { uid = id; break; }
      if (!byUser.has(uid)) byUser.set(uid, []);
      byUser.get(uid).push(p);
    }
    for (const [uid, grp] of byUser) {
      try {
        const raw = await spawnCommand(
          'CLASSPATH=' + USERPROBE_DEX + ' app_process /system/bin com.copg.userprobe.Main labels ' +
          uid + ' ' + shq(grp.join(',')));
        let o = {}; try { o = JSON.parse(String(raw || '').trim()); } catch (_) {}
        for (const k of Object.keys(o)) { out[k] = o[k]; _labelCache.set(k, o[k]); }
      } catch (e) { dlog('probeLabels u' + uid + ' failed: ' + e, 'warn'); }
    }
    return out;
  }

  /* Wipe every cached icon file so they re-download fresh (e.g. a game changed
     its icon). The UI then re-renders + re-batches the missing ones. Device only. */
  async function clearIconCache() {
    if (!hasBridge()) return false;
    try { await execCommand(`rm -f ${ICON_DIR_ABS}/*.png ${ICON_DIR_ABS}/.done`); return true; }
    catch (_) { return false; }
  }

  /* ─── Public API ─── */
  /* Immersive fullscreen toggle — KernelSU / ReZygisk WebUI bridge (`ksu.fullScreen`).
     true = fullscreen (status bar hidden), false = status bar shown. No-op when the
     bridge is absent (browser preview) or the host doesn't expose `fullScreen`. The
     page already pads `env(safe-area-inset-top)` (base.css) so content stays clear of
     the notch/clock while immersive. */
  function setFullscreen(on) {
    try {
      if (typeof ksu !== 'undefined' && typeof ksu.fullScreen === 'function') ksu.fullScreen(!!on);
    } catch (e) { dlog('fullScreen bridge failed: ' + e, 'warn'); }
  }

  /* Open a URL via an Android VIEW intent so it lands in the registered app (e.g. a
     t.me link → Telegram) instead of the in-WebView browser. Falls back to window.open
     in preview. */
  async function openExternal(url) {
    if (!url) return false;
    // Prefer the app's OWN scheme so it opens in the app, not a browser. A plain
    // https://t.me/<name> link gets routed to Chrome unless Telegram has verified the
    // domain; tg://resolve?domain=<name> is Telegram-exclusive → opens it directly.
    let target = url, m;
    if ((m = /^https?:\/\/t\.me\/([A-Za-z0-9_]{3,})\/?$/.exec(url))) target = 'tg://resolve?domain=' + m[1];
    if (!hasBridge()) { try { window.open(url, '_blank'); } catch (_) {} return true; }
    try { await execCommand(`am start -a android.intent.action.VIEW -d ${shq(target)}`); return true; }
    catch (e) { dlog('openExternal failed: ' + e, 'warn'); return false; }
  }

  /* Fire an Android SHARE sheet with plain text (the device/enrollment code) → the user picks
     Telegram and it's sent straight to the seller. Bypasses the clipboard entirely, which is
     blocked in some KsuWebUI builds (the "can't copy my code" bug). Browser preview → Web Share
     if available. */
  async function shareText(text) {
    if (!text) return false;
    if (!hasBridge()) { try { if (navigator.share) navigator.share({ text: String(text) }); } catch (_) {} return true; }
    try {
      await execCommand(`am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT ${shq(String(text))}`);
      return true;
    } catch (e) { dlog('shareText failed: ' + e, 'warn'); return false; }
  }

  /* Write the device/enrollment code to Download/COPG/copg-device-code.txt so a user with a broken
     clipboard can just attach the .txt file. Returns the path (or null). Device-only. */
  async function saveDeviceCode(code) {
    if (!hasBridge() || !code) return null;
    const path = `${BACKUP_DIR}/copg-device-code.txt`;
    try {
      await execCommand(`mkdir -p ${BACKUP_DIR}`);
      await execCommand(`echo '${shq(String(code))}' > ${path}`);
      await execCommand(`chmod 0644 ${path}`);
      return path;
    } catch (e) { dlog('saveDeviceCode failed: ' + e, 'warn'); return null; }
  }

  /* Force-stop an installed package, then launch it — so the next process start goes
     through Zygisk specialize again and re-applies the current spoof. Root shell; two
     single-purpose commands (per the bridge rule). No-op in browser preview. */
  async function relaunchPackage(pkg, userScope) {
    if (!hasBridge()) return false;
    const p = clean(pkg);
    // Target the RIGHT Android user: a per-user-scoped entry (u=<id>) must relaunch that user's
    // instance, not the owner's. '' / non-numeric = all-users → owner (user 0, no --user flag).
    const uid = (/^[0-9]+$/.test(String(userScope || ''))) ? String(userScope) : '';
    const uflag = uid ? ` --user ${uid}` : '';
    try {
      await execCommand(`am force-stop${uflag} ${shq(p)}`);
      // Launch via the RESOLVED launcher activity + `am start`, NOT `monkey`. `monkey` enables
      // accelerometer_rotation (auto-rotate) as a side effect, so every relaunch was silently turning
      // the user's auto-rotate back ON — verified on-device: `monkey … LAUNCHER` flips the setting
      // 0→1, `am start -n <component>` does not (and `am start` was already used for the per-user path
      // because monkey --user is unreliable across ROMs too). monkey stays only as a last-resort
      // fallback when the launcher component can't be resolved.
      let comp = '';
      try {
        const r = await execCommand(`cmd package resolve-activity --brief${uflag} ${shq(p)}`);
        comp = (String(r).trim().split('\n').pop() || '').trim();
      } catch (_) { /* fall back to monkey below */ }
      if (comp && comp.indexOf(p + '/') === 0) await execCommand(`am start${uflag} -n ${shq(comp)}`);
      else await execCommand(`monkey${uflag} -p ${shq(p)} -c android.intent.category.LAUNCHER 1`);
      dlog('relaunched ' + p + (uid ? ' (user ' + uid + ')' : ''), 'info');
      return true;
    } catch (e) { dlog('relaunch failed: ' + e, 'warn'); return false; }
  }

  /* ─── Global "served spoofs" status ───
     The Zygisk module appends one JSONL event per spoof VALUE it actually applies to a fork
     (spoof_module.flushServed via the root companion) to /data/adb/COPG/served.jsonl. Each line is
     {t:epoch, p:pkg, k:capability, v:value}. VALUES ONLY — no license/PRO/engine state ever, so the
     file can't be a crack oracle (a locked/unconfigured feature writes nothing). The Status board
     tails this and shows, per capability, the most recent fake value + app + when. Device-only. */
  const SERVED_LOG = '/data/adb/COPG/served.jsonl';
  async function getServed() {
    if (!hasBridge()) return [];
    let text = '';
    try { text = await execCommand(`cat ${SERVED_LOG} 2>/dev/null`); } catch (_) { return []; }
    const out = [];
    text.split('\n').forEach(l => {
      l = l.trim(); if (!l) return;
      try { const o = JSON.parse(l); if (o && o.k) out.push(o); } catch (_) { /* skip partial line */ }
    });
    return out;
  }
  async function clearServed() {
    if (!hasBridge()) return false;
    try { await execCommand(`rm -f ${SERVED_LOG}`); return true; } catch (_) { return false; }
  }

  /* System-server-spoofs health — ONE aggregate status (not a per-hook list), plus whether the module
     loaded this boot. Read from signals the native side already writes: the boot heartbeat (copg_hb =
     this boot's boot_id) and EVERY system_server hook's boot-crash guard counter (0 healthy, 1-2
     crashed-but-recovered, >=3 tripped/disabled-to-recover). ONE batched root read.

     CRACK-SAFE (see [[no-license-internals-in-ui]]): the status is AGGREGATE — it says the system_server
     layer is up/healthy after reboot, NEVER which feature is on or working. The per-hook FREE/PRO
     breakdown is deliberately NOT surfaced (a licensed-only per-hook active/crashed flag would be an
     unlock oracle). The master switch (sysserver) is user config, not license, so folding the PRO
     bootguards into the aggregate leaks nothing. No license / engine / key state is read here. */
  const BOOTGUARD_MAX = 3;
  const SS_HB_PERIOD = 30;   // seconds — must match spoof_module.cpp SS_HB_PERIOD (copg_ss_hb cadence)
  const SS_BOOTGUARDS = ['fs_bootguard', 'vpn_bootguard', 'lang_bootguard', 'playsrc_bootguard',
                         'tensorfeat_bootguard', 'corepatch_bootguard'];
  async function getSystemHooks() {
    if (!hasBridge()) return { loaded: 'preview' };
    // SIMPLE single-purpose reads only — one plain `cat` per file, fired together with Promise.all.
    // The old compound `B=…; echo; cat; for f in …; done` script returned EMPTY on some ksu.exec
    // bridges (the same gotcha getSystemInfo documents) → hb parsed blank → the board wrongly showed
    // "Not loading" even with the hooks live this boot. Each read is independent; one failure never
    // blanks the rest.
    const read = async p => { try { return ((await execCommand(`cat ${p} 2>/dev/null`)) || '').trim(); } catch (_) { return ''; } };
    let hb = '', boot = '', sshb = '', guards = [];
    try {
      [hb, boot, sshb, ...guards] = await Promise.all([
        read('/data/system/copg_hb'),
        read('/proc/sys/kernel/random/boot_id'),
        read('/data/system/copg_ss_hb'),
        ...SS_BOOTGUARDS.map(g => read('/data/system/copg_' + g)),
      ]);
    } catch (_) { return { loaded: 'unknown' }; }
    const bg = v => { const n = parseInt(v, 10); return isNaN(n) || n < 0 ? 0 : n; };

    if (_bootId == null && boot) _bootId = boot;   // same live boot_id sysserverArmed/Pending compare against
    const loaded = !hb ? 'stale' : (hb === 'REBOOT' ? 'reboot' : (hb === boot ? 'loaded' : 'stale'));
    const maxBg = Math.max(0, ...guards.map(bg));
    const master = !!(globalConfig && globalConfig.sysserver);
    // REAL liveness: the .so's heartbeat thread rewrites copg_ss_hb (epoch) every SS_HB_PERIOD=30s,
    // but ONLY once the system_server spoof layer actually armed this boot. So a FRESH stamp = the
    // layer is resident and running RIGHT NOW (not merely "loaded at boot"). Allow 4× the period for
    // a missed write + WebView latency before calling it dead. BACK-COMPAT: an older .so that predates
    // the heartbeat writes no copg_ss_hb → fall back to the boot-stamp (copg_hb == boot_id) so the
    // board still works before the new build ships.
    const ssTs = parseInt(sshb, 10);
    const ssPresent = !isNaN(ssTs) && ssTs > 0;
    // Refresh the SHARED liveness snapshot the toggle-greys read (null when absent → they fall back to
    // boot_id), so the board and every sysserver button agree and a board refresh updates the greys.
    _ssAlive = ssPresent ? ((Math.floor(Date.now() / 1000) - ssTs) < (SS_HB_PERIOD * 4)) : null;
    const ssAlive = ssPresent ? _ssAlive : (loaded === 'loaded');   // board: boot-stamp fallback on old .so

    // Aggregate system_server-spoofs state — driven by the real heartbeat, not just the boot stamp.
    let sysserver;
    if (!master)                    sysserver = 'off';          // master switch off
    else if (sysserverPending())    sysserver = 'reboot';       // enabled this boot, not yet rebooted to arm
    else if (loaded === 'reboot')   sysserver = 'reboot';       // fresh install sentinel, needs the boot to arm
    else if (!ssAlive)              sysserver = 'stale';         // armed intent but heartbeat dead → NOT running
    else if (maxBg >= BOOTGUARD_MAX) sysserver = 'tripped';     // a hook bootlooped → auto-disabled to recover
    else if (maxBg >= 1)            sysserver = 'crashed';      // a hook crashed at least once, recovered
    else                            sysserver = 'active';       // armed + heartbeat fresh = alive right now
    return { loaded, sysserver, ssAlive };
  }

  /* Wipe an app's stored data (pm clear). Resets the app's per-install identity anchors —
     SSAID (settings_ssaid.xml row), cached GAID/App-Set-ID, install-attribution prefs — so
     the next launch re-reads the SPOOFED device as a brand-new install (the "new device"
     workflow: device/SIM spoof + a clean data dir). Does NOT relaunch — the row's separate
     launch button handles that (through a fresh Zygisk specialize). Destructive: wipes logins,
     progress, everything under the app's data dir. Root; caller confirms first. Preview no-op. */
  async function clearPackageData(pkg, userScope) {
    if (!hasBridge()) return false;
    const p = clean(pkg);
    // Wipe the RIGHT user's data dir: a u=<id>-scoped entry clears that user's instance, not the
    // owner's. '' / non-numeric = all-users → owner (user 0). `pm clear --user N` targets the clone.
    const uid = (/^[0-9]+$/.test(String(userScope || ''))) ? String(userScope) : '';
    const uflag = uid ? ` --user ${uid}` : '';
    try {
      await execCommand(`pm clear${uflag} ${shq(p)}`);  // wipes data + stops the app (for that user)
      dlog('cleared data for ' + p + (uid ? ' (user ' + uid + ')' : ''), 'info');
      return true;
    } catch (e) { dlog('clear-data failed: ' + e, 'warn'); return false; }
  }

  // ── Global Advertising ID (GAID) spoof — FREE, root-only, zero residency ──
  // GMS stores the device advertising ID in adid_settings.xml (key adid_key). Rewriting
  // it + restarting GMS makes EVERY app read the new ID (it's a device-wide GMS value).
  // The phenotype .pb holds only flags, not the ID (verified on-device), so the prefs are
  // authoritative. No hook, no residency — just a root file edit. (Per-app GAID = paid, later.)
  const GAID_XML = '/data/data/com.google.android.gms/shared_prefs/adid_settings.xml';
  // The very first real GAID we ever overwrite is stashed here (once) so "Restore original"
  // can always hand the device back its true advertising id. Lives under the module dir
  // (root-only, survives GMS clears). Never overwritten after it's created.
  const GAID_ORIG = MODULE_DIR + '/.gaid_original';
  const UUID_RE = /^[0-9a-fA-F-]{36}$/;
  const isUuid = s => UUID_RE.test(String(s || '').trim());

  function genAdvertisingId() {
    if (w.crypto && crypto.randomUUID) return crypto.randomUUID();
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0; return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
  }

  async function getAdvertisingId() {
    if (!hasBridge()) return null;
    try {
      const out = await execCommand(`cat ${GAID_XML} 2>/dev/null`);
      const m = (out || '').match(/name="adid_key">([^<]*)</);
      return m ? m[1].trim() : null;
    } catch (e) { dlog('getAdvertisingId: ' + e, 'warn'); return null; }
  }

  // The stashed original id, or null if we never backed one up yet.
  async function getOriginalAdvertisingId() {
    if (!hasBridge()) return null;
    try {
      const out = (await execCommand(`cat ${GAID_ORIG} 2>/dev/null`) || '').trim();
      return isUuid(out) ? out : null;
    } catch (e) { dlog('getOriginalAdvertisingId: ' + e, 'warn'); return null; }
  }

  // Write a new GAID into GMS' prefs, restore the SELinux context (sed -i drops the MLS
  // categories c512,c768 → GMS can't read it), then restart GMS so it re-reads. Backs up
  // the true id ONCE before the first overwrite. Returns the new id, or null if unavailable
  // (file missing = user must open Google → Ads once).
  async function setAdvertisingId(uuid) {
    if (!hasBridge()) { previewUnsaved = true; return uuid || genAdvertisingId(); }
    const id = isUuid(uuid) ? String(uuid).trim() : genAdvertisingId();
    try {
      const exists = (await execCommand(`test -f ${GAID_XML} && echo y || echo n`) || '').trim();
      if (exists !== 'y') { dlog('adid_settings.xml missing — open Google → Ads once', 'warn'); return null; }
      // One-time backup of the real id, before we ever change it.
      const haveBackup = (await execCommand(`test -f ${GAID_ORIG} && echo y || echo n`) || '').trim();
      if (haveBackup !== 'y') {
        const cur = await getAdvertisingId();
        if (isUuid(cur)) {
          await execCommand(`echo '${shq(cur)}' > ${GAID_ORIG}`);
          await execCommand(`chmod 600 ${GAID_ORIG} 2>/dev/null || true`);
          dlog('backed up original advertising id', 'info');
        }
      }
      await execCommand(`sed -i 's#<string name="adid_key">[^<]*</string>#<string name="adid_key">${id}</string>#' ${GAID_XML}`);
      await execCommand(`restorecon ${GAID_XML} 2>/dev/null || true`);
      await execCommand(`am force-stop com.google.android.gms 2>/dev/null || true`);
      dlog('advertising id set to ' + id, 'info');
      return id;
    } catch (e) { dlog('setAdvertisingId: ' + e, 'warn'); return null; }
  }

  // Put the device's true advertising id back (from the one-time backup). Returns the
  // restored id, or null if there's no backup / no bridge.
  async function restoreAdvertisingId() {
    if (!hasBridge()) return null;
    const orig = await getOriginalAdvertisingId();
    if (!isUuid(orig)) { dlog('restoreAdvertisingId: no backup', 'warn'); return null; }
    return setAdvertisingId(orig);
  }

  // ── Licensing (licensing branch): drive the copg_lic root helper via ksu.exec ──
  const LIC_BIN = MODULE_DIR + '/copg_lic';

  async function licenseStatus() {
    if (!hasBridge()) return { state: 'PREVIEW' };
    try {
      const out = await execCommand(LIC_BIN + ' status 2>/dev/null');
      const get = k => { const m = (out || '').match(new RegExp('^' + k + '=(.*)$', 'm')); return m ? m[1].trim() : ''; };
      return { state: get('state') || 'FREE', buyer: get('buyer'), tier: get('tier'), expiry: get('expiry') };
    } catch (e) { return { state: 'FREE', error: String(e) }; }
  }

  async function getEnrollmentCode() {
    if (!hasBridge()) return null;
    const out = await execCommand(LIC_BIN + ' enroll 2>/dev/null');
    const line = (out || '').split('\n').map(s => s.trim()).find(l => l.startsWith('COPG1-'));
    return line || null;
  }

  async function importLicense(b64) {
    if (!hasBridge()) return { ok: false, preview: true };
    const clean = String(b64 || '').replace(/[^A-Za-z0-9+/=]/g, '');   // strip whitespace/newlines
    const dir = MODULE_DIR + '/license';
    await execCommand('mkdir -p ' + dir);
    await execCommand("echo '" + clean + "' | base64 -d > " + dir + '/license.bin');
    await execCommand('chmod 644 ' + dir + '/license.bin 2>/dev/null; chcon u:object_r:system_file:s0 ' + dir + '/license.bin 2>/dev/null || true');
    // Mirror to the persistent backup (/data/adb/copg) so it survives a module update OR a full
    // uninstall+reinstall — customize.sh restores from here. Device-locked + signed, so safe to keep.
    await execCommand('mkdir -p /data/adb/COPG 2>/dev/null; cp -f ' + dir + '/license.bin /data/adb/COPG/license.bin 2>/dev/null; chmod 600 /data/adb/COPG/license.bin 2>/dev/null; chcon u:object_r:system_file:s0 /data/adb/COPG/license.bin 2>/dev/null || true');
    const st = await licenseStatus();
    return { ok: st.state === 'VALID' || st.state === 'GRACE', status: st };
  }

  /* NoMount (maxsteeel/nomount) presence — the optional kernel helper COPG uses as the
     stealth /proc/cpuinfo redirect backend. 'active' = binary present AND the LKM answers
     (`nm version` exit 0); 'installed' = metamodule dir present but the kernel lacks
     CONFIG_NOMOUNT (binary can't talk to the LKM); 'absent' = not installed. Preview = absent.
     Presence only — never license/gate state ([[no-license-internals-in-ui]]). */
  const NM_BIN = '/data/adb/modules/nomount/bin/nm';
  async function detectNoMount() {
    if (!hasBridge()) return { status: 'absent', version: '' };
    try {
      const out = (await execCommand(`${NM_BIN} version 2>/dev/null`) || '').trim();
      if (out) {
        // LKM answers. Version = driver protocol from `nm version` (e.g. "22"), the same
        // kernel-level number COPG's native backend gates on (NM_MIN_VERSION=22, NoMount 2.2.0).
        // Older drivers (<=21 / 2.1.x) had the kernel freeze/panic bug → COPG falls back to the
        // bind-mount backend, and the card flags them 'outdated' (update needed).
        const ver = (out.match(/[0-9][0-9A-Za-z.\-]*/) || [''])[0];
        return { status: (parseInt(ver, 10) || 0) >= 22 ? 'active' : 'outdated', version: ver };
      }
    } catch (e) { /* nonzero exit = LKM not answering; fall through to the dir check */ }
    try {
      const dir = (await execCommand('[ -d /data/adb/modules/nomount ] && echo y || echo n')).trim();
      return { status: dir === 'y' ? 'installed' : 'absent', version: '' };
    } catch (e) { console.warn('detectNoMount:', e); return { status: 'absent', version: '' }; }
  }

  /* Zygisk REAL active status via the variant's own controller bin (not just dir presence).
     Zygisk Next: `zygiskd status` (client query to the running daemon) prints version_local +
     zygote_state_N lines; a line ending ",3" = Zygisk injected into that zygote → actively
     working. No controller output but the module is present/disabled → 'installed'. Falls back
     to dir + `disable` for ReZygisk / a down daemon. Preview = absent. */
  async function detectZygisk() {
    if (!hasBridge()) return { status: 'absent', version: '' };
    try {
      const out = (await execCommand('/data/adb/modules/zygisksu/bin/zygiskd status 2>/dev/null') || '').trim();
      if (/zygote_state_\d+:/.test(out)) {
        const ver = (out.match(/version_local:\s*(\S+)/) || ['', ''])[1];
        const injecting = /zygote_state_\d+:[^\n]*,3\b/.test(out);     // ",3" = injected into zygote
        return { status: injecting ? 'active' : 'installed', version: ver };
      }
    } catch (e) { /* not Zygisk Next, or daemon down → fall through */ }
    // ReZygisk / BReZygisk (module id `rezygisk`; BReZygisk shares the dir). Its ptrace daemon CLI
    // `zygisk-ptracer64 info` prints "The ReZygisk Tracer <ver>" + "Daemon process PID: <pid>"
    // (pid>0 = daemon running). The listening socket under /data/adb/rezygisk is the fallback.
    try {
      if ((await execCommand('[ -f /data/adb/modules/rezygisk/module.prop ] && echo y || echo n')).trim() === 'y') {
        const off = (await execCommand('[ -f /data/adb/modules/rezygisk/disable ] && echo y || echo n')).trim() === 'y';
        let info = '';
        for (const b of ['zygisk-ptracer64', 'zygisk-ptracer32']) {
          info = (await execCommand(`/data/adb/modules/rezygisk/bin/${b} info 2>/dev/null`) || '').trim();
          if (info) break;
        }
        // "The ReZygisk Tracer <ver>" / "The BReZygisk Tracer <ver>" → match the fork-agnostic
        // "Tracer <ver>", then fall back to any version number in the banner.
        const ver = (info.match(/Tracer\s+(\S+)/) || [])[1] || (info.match(/\d+\.\d[0-9A-Za-z.\-]*/) || [''])[0];
        const pid = parseInt((info.match(/Daemon process PID:\s*(\d+)/) || ['', '0'])[1], 10);
        const sock = (await execCommand('{ [ -S /data/adb/rezygisk/cp64.sock ] || [ -S /data/adb/rezygisk/cp32.sock ]; } && echo y || echo n')).trim() === 'y';
        const running = pid > 0 || sock;   // ptracer PID is authoritative; socket is the fallback
        return { status: (!off && running) ? 'active' : 'installed', version: ver };
      }
    } catch (e) { console.warn('detectZygisk rezygisk:', e); }
    // Zygisk Next present but the status query failed (disabled / daemon down) → installed.
    try {
      if ((await execCommand('[ -f /data/adb/modules/zygisksu/module.prop ] && echo y || echo n')).trim() === 'y')
        return { status: 'installed', version: '' };
    } catch (e) { console.warn('detectZygisk zn:', e); }
    return { status: 'absent', version: '' };
  }

  w.COPG = {
    MODULE_DIR, CONFIG_PATH, LIST_PATH, BACKUP_DIR,
    // licensing
    licenseStatus, getEnrollmentCode, importLicense,
    execCommand, hasBridge,
    loadAll,
    // selectors
    listDevices, listPackages, nameFor, locate, deviceNameExists,
    // mutations
    upsertDevice, deleteDevice, upsertPackage, deletePackage,
    // device share (portable export/import code)
    exportDevice, importDevice, devicePackageCount,
    // persistence
    save, exportText,
    // global hooks (device-wide) — global.json (system-wide IMEI)
    loadGlobalConfig, saveGlobalConfig, getGlobalConfig, sysserverArmed, sysserverPending, genImei, restartTelephony,
    // backup / restore
    backupConfig, listBackups, restoreBackup, deleteBackup, restoreFromText, inspectBackup, syncFromGitHub, saveLog,
    // system / device-environment info
    getSystemInfo, setFullscreen, relaunchPackage, clearPackageData, getServed, clearServed, getSystemHooks, openExternal, shareText, saveDeviceCode,
    // integrations / helper-module detection
    detectNoMount, detectZygisk,
    // global advertising-id (GAID) spoof — free
    getAdvertisingId, setAdvertisingId, genAdvertisingId, getOriginalAdvertisingId, restoreAdvertisingId,
    // installed apps + icons (KSU API with pm fallback)
    getInstalledPackages, getSystemPackages, isInstalled, getAppLabel, listInstalledApps, enrichApps,
    getInstalledUsers, usersForPackage, userNameOf, loadCloneNames, probeLabelsForUser,
    fetchPlayIconUrl, fetchFdroidIconUrl, fetchAppIconUrl, batchFetchIcons, probeRenderIcons, clearIconCache,
    // helpers exposed for modals/UI
    clean, tagsOf, hasTag, deviceKeyFromName, pkgKeyOf,
    userScopeOf, setUserScope, idPackage,
    flagEmoji, flagForLocale, flagForTz, flagForCountryName,
    deviceLabel, isThisDeviceKey, THIS_DEVICE_PKEY, THIS_DEVICE_DKEY,
    isRandomKey, RANDOM_PKEY, RANDOM_DKEY,
    sdkFromAndroid, androidFromSdk, deriveAndroidId, deriveGaid, deriveAppSetId, deriveDrmDeviceId, deriveDrmSystemId, deriveImei, deriveImsi, deriveIccid, deriveBootId, deriveMac, deriveBtmac,
    cpuModelOf, setCpuModel, getCpuModels, cpuModelName, importCpuModel, cpuFileKeys, deleteCpuModel,
    gpuModelOf, setGpuModel, getGpuModels, gpuModelName, importGpuModel, updateGpuModel, deleteGpuModel,
    timezoneOf, setTimezone, getTzZones, tzName, importTzZone, deleteTzZone,
    localeOf, setLocale, getLangLocales, langName, importLangLocale, deleteLangLocale,
    hasWallpaper, wallpaperOf, setWallpaper,
    hasAccount, accountEmailOf, setAccount,
    uaKeyOf, setUa, getUaProfiles, uaProfileName, importUaProfile, deleteUaProfile,
    uptimeOf, setUptime, bootidOf, setBootid, macOf, setMac, btmacOf, setBtmac,
    gpsOf, setGps, getPlaces, nearestPlace, cellOf, setCell,
    dpiOf, setDpi,
    carrierOf, setCarrierModel, getCarrierModels, carrierModelName, buildCarrierEntry, importCarrierModel, updateCarrierModel, deleteCarrierModel,
    proxyOf, setProxy, getProxyModels, proxyModelName, buildProxyEntry, importProxyModel, updateProxyModel, deleteProxyModel, writeActiveConf,
    // flags / raw access
    get config()    { return config; },
    get keyOrder()  { return keyOrder; },
    get names()     { return names; },
    get previewUnsaved() { return previewUnsaved; },
    set previewUnsaved(v) { previewUnsaved = v; },
    get installedSet()   { return installedCache || new Set(); },
  };

})(window);
