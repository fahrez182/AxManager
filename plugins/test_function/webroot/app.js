// Test Function WebUI: one card per part of AxManager's WebUI API.
//
// Commands reach a shell as sh -c "<command>", inside double quotes, so an outer shell expands
// $VARS before the command runs. Where a variable must be expanded by the command itself (one
// set by the env option, $!, loop variables), it is written \$ here. No double quotes in commands.

const PLUGIN_ID = "test_function";
const hasKsu = typeof window.ksu !== "undefined";
const hasAxeron = typeof window.Axeron !== "undefined";
let pluginDir = null;

// ---------- helpers ----------

let callbackCounter = 0;

/** ksu.exec(command, options, callback) as a promise of {errno, stdout, stderr}. */
function execAsync(command, options) {
  return new Promise((resolve, reject) => {
    const name = `tf_exec_${Date.now()}_${callbackCounter++}`;
    window[name] = (errno, stdout, stderr) => {
      delete window[name];
      resolve({ errno, stdout, stderr });
    };
    try {
      ksu.exec(command, JSON.stringify(options || {}), name);
    } catch (e) {
      delete window[name];
      reject(e);
    }
  });
}

/** Minimal emitter, the shape ksu.spawn calls into. */
function emitter() {
  const listeners = {};
  return {
    on(event, fn) { (listeners[event] ||= []).push(fn); },
    emit(event, ...args) { (listeners[event] || []).forEach((fn) => fn(...args)); },
  };
}

function spawnDirectChild() {
  const child = emitter();
  child.stdout = emitter();
  child.stderr = emitter();
  return child;
}

/** ksu.spawn called directly, without the kernelsu.js helper. */
function spawnDirect(command, args, options) {
  const name = `tf_spawn_${Date.now()}_${callbackCounter++}`;
  const child = spawnDirectChild();
  child.on("exit", () => delete window[name]);
  window[name] = child;
  ksu.spawn(command, JSON.stringify(args || []), JSON.stringify(options || {}), name);
  return child;
}

/** Axeron.exec(command, options, callback) as a promise of {errno, stdout, stderr}. */
function axeronExecAsync(command, options) {
  return new Promise((resolve, reject) => {
    const name = `tf_axexec_${Date.now()}_${callbackCounter++}`;
    window[name] = (errno, stdout, stderr) => {
      delete window[name];
      resolve({ errno, stdout, stderr });
    };
    try {
      Axeron.exec(command, JSON.stringify(options || {}), name);
    } catch (e) {
      delete window[name];
      reject(e);
    }
  });
}

// ---------- loading and freeze meter ----------
// Everything here moves from a timer on the page's JS thread. A bridge call that blocks JS
// stops the spinner, the busy bar and the meter together, which is the point: the freeze
// becomes visible and measurable instead of being hidden by a CSS animation.

const FREEZE_LIMIT_MS = 200;
const TICK_MS = 40;
const SPIN = ["◐", "◓", "◑", "◒"];
let running = 0;
let ticker = null;
const meters = new Set();

function tick() {
  const now = performance.now();
  document.getElementById("busy").style.left = `${((now / 6) % 130) - 30}%`;
  const spin = SPIN[Math.floor(now / 120) % SPIN.length];
  document.querySelectorAll(".status.run").forEach((s) => (s.dataset.spin = spin));
  for (const m of meters) {
    m.max = Math.max(m.max, now - m.last - TICK_MS);
    m.last = now;
  }
}

// Timers of a hidden page are throttled: that gap is not a freeze, so start counting afresh.
document.addEventListener("visibilitychange", () => {
  const now = performance.now();
  for (const m of meters) m.last = now;
});

/** Starts measuring the longest time the page's JS was stuck; stop() returns it in ms. */
function startMeter() {
  const m = { last: performance.now(), max: 0 };
  meters.add(m);
  running++;
  document.getElementById("busy").hidden = false;
  if (ticker === null) ticker = setInterval(tick, TICK_MS);
  return () => {
    // The gap up to now counts too: a call that blocked until it returned ends here.
    if (!document.hidden) m.max = Math.max(m.max, performance.now() - m.last - TICK_MS);
    meters.delete(m);
    running--;
    if (running === 0) {
      clearInterval(ticker);
      ticker = null;
      document.getElementById("busy").hidden = true;
    }
    return Math.max(0, Math.round(m.max));
  };
}

/**
 * Resolves after the browser has drawn a frame, so "running" is on screen before a call.
 * A hidden page draws no frames, so it also resolves after 100 ms rather than waiting forever.
 */
const nextPaint = () => new Promise((r) => {
  const timer = setTimeout(r, 100);
  requestAnimationFrame(() => setTimeout(() => { clearTimeout(timer); r(); }, 0));
});

/** The distinct [FAIL] lines of a script's output: the command itself can still exit 0. */
function failedChecks(stdout) {
  return [...new Set(stdout.split("\n").filter((l) => l.startsWith("[FAIL]")))];
}

/** Puts the failed checks first, so a "fail" says why without scrolling. */
function withFailures(text, stdout) {
  const failed = failedChecks(stdout);
  if (failed.length === 0) return text;
  return `Failed checks (the command itself exited normally):\n${failed.join("\n")}\n\n${text}`;
}

function fmt(result) {
  return `errno: ${result.errno}\n--- stdout ---\n${result.stdout || ""}` +
    (result.stderr ? `\n--- stderr ---\n${result.stderr}` : "");
}

async function getPluginDir() {
  if (pluginDir) return pluginDir;
  const r = await execAsync("echo $AXERONDIR");
  pluginDir = `${r.stdout.trim()}/plugins/${PLUGIN_ID}`;
  return pluginDir;
}

// ---------- tests ----------
// run(log) returns true (pass), false (fail) or undefined (informational).

const tests = [
  {
    id: "module-info",
    title: "ksu.moduleInfo()",
    desc: "The plugin's module.prop fields and state, plus moduleDir. Synchronous.",
    blocking: true,
    async run(log) {
      const info = JSON.parse(ksu.moduleInfo());
      log(JSON.stringify(info, null, 2));
      return info.id === PLUGIN_ID;
    },
  },
  {
    id: "exec-sync",
    title: "ksu.exec(command)",
    desc: "Synchronous by design: returns stdout (+ stderr) and blocks the page meanwhile. With sleep 1, expect a ~1000 ms freeze here.",
    blocking: true,
    async run(log) {
      const out = ksu.exec("id; echo marker=$AXERON_PLUGIN; echo to-stderr >&2; sleep 1");
      log(out);
      return out.includes(`marker=${PLUGIN_ID}`) && out.includes("to-stderr");
    },
  },
  {
    id: "exec-options",
    title: "ksu.exec(command, options, callback)",
    desc: "Async with cwd and env options; the plugin's system/bin comes first in PATH. Also sleeps 1s: the page must not freeze.",
    async run(log) {
      const dir = await getPluginDir();
      const r = await execAsync(
        "pwd; echo greeting=\\$TF_GREETING; echo path0=\\${PATH%%:*}; axtest ping; sleep 1",
        { cwd: dir, env: { TF_GREETING: "hello" } },
      );
      log(fmt(r));
      return r.errno === 0 &&
        r.stdout.includes(dir) &&
        r.stdout.includes("greeting=hello") &&
        r.stdout.includes(`path0=${dir}/system/bin`);
    },
  },
  {
    id: "exec-quoting",
    title: "Quoting: $ expanded too early",
    desc: "Without \\$, a variable set by the env option is expanded by the outer shell (empty).",
    async run(log) {
      const r = await execAsync("echo early=[$TF_X] late=[\\$TF_X]", { env: { TF_X: "set" } });
      log(fmt(r));
      return r.stdout.includes("early=[]") && r.stdout.includes("late=[set]");
    },
  },
  {
    id: "spawn-direct",
    title: "ksu.spawn(...) streaming",
    desc: "stdout/stderr 'data' events while it runs, then 'exit' and, for a non-zero code, 'error'.",
    run(log) {
      return new Promise((resolve) => {
        const lines = [];
        const push = (s) => { lines.push(s); log(lines.join("\n")); };
        const child = spawnDirect(
          "for i in 1 2 3; do echo line \\$i; sleep 1; done; echo oops >&2; exit 3",
          [],
          {},
        );
        let sawError = false;
        child.stdout.on("data", (d) => push(`[stdout] ${d.trim()}`));
        child.stderr.on("data", (d) => push(`[stderr] ${d.trim()}`));
        // 'error' arrives before 'exit', so it is seen even though 'exit' drops the child.
        child.on("error", (e) => { sawError = e.exitCode === 3; push(`[error] exitCode=${e.exitCode}`); });
        child.on("exit", (code) => {
          push(`[exit] ${code}`);
          if (!sawError) push("(no 'error' event before 'exit')");
          resolve(code === 3 && sawError);
        });
      });
    },
  },
  {
    id: "kernelsu-module",
    title: "kernelsu.js helper (ax://kernelsu.js/)",
    desc: "Dynamic import of the bundled module, then exec(), spawn(cmd, args) and spawn(cmd, options), moduleInfo(), listPackages() and getPackagesInfo().",
    async run(log) {
      let mod;
      try {
        mod = await import("ax://kernelsu.js/");
      } catch (e) {
        log(`import failed: ${e}\n(the module can still be bundled into your page)`);
        return false;
      }
      const r = await mod.exec("echo from-module", {});
      let text = `exec(): ${JSON.stringify(r)}`;
      // Resolves to what a spawn() call printed, or to its error.
      const spawned = (...callArgs) => new Promise((resolve) => {
        let out = "";
        const c = mod.spawn(...callArgs);
        c.stdout.on("data", (d) => (out += d));
        c.on("error", (e) => resolve(`error: ${e.message || e}`));
        c.on("exit", (code) => resolve(`exit ${code}: ${out.trim()}`));
      });
      const withArgs = await spawned("echo", ["args-ok"]);
      const withOptions = await spawned("pwd", { cwd: "/system" });
      const withBoth = await spawned("echo", ["both-ok"], { cwd: "/" });
      text += `\nspawn(cmd, [args]): ${withArgs}` +
        `\nspawn(cmd, options): ${withOptions}` +
        `\nspawn(cmd, [args], options): ${withBoth}`;
      // Parsed for the caller: arrays, not JSON strings.
      const info = JSON.parse(mod.moduleInfo());
      const users = mod.listPackages("user");
      const pkgs = mod.getPackagesInfo(users.slice(0, 2));
      const single = mod.getPackagesInfo(JSON.stringify(users.slice(0, 1)));
      text += `\nmoduleInfo().id: ${info.id}` +
        `\nlistPackages("user"): ${users.length} apps` +
        `\ngetPackagesInfo([2 apps]): ${pkgs.map((p) => p.appLabel).join(", ")}` +
        `\ngetPackagesInfo(JSON string): ${single.map((p) => p.appLabel).join(", ")}` +
        `\nexports: ${["enableEdgeToEdge", "exit"].map((n) => `${n}=${typeof mod[n]}`).join(", ")}` +
        `\nksu.exit: ${typeof ksu.exit}`;
      log(text);
      return r.errno === 0 && r.stdout.includes("from-module") &&
        withArgs.includes("args-ok") && withOptions.includes("/system") && withBoth.includes("both-ok") &&
        info.id === PLUGIN_ID && Array.isArray(users) && users.length > 0 &&
        pkgs.length === 2 && pkgs.every((p) => p.appLabel) && single.length === 1;
    },
  },
  {
    id: "axeron-exec",
    title: "Axeron.exec(command, options)",
    desc: "Synchronous: returns {errno, stdout, stderr} as JSON and blocks the page meanwhile. With sleep 1, expect a ~1000 ms freeze here.",
    blocking: true,
    async run(log) {
      if (!hasAxeron) { log("window.Axeron is missing"); return false; }
      const r = JSON.parse(Axeron.exec("echo axeron-ok; id -u; sleep 1", JSON.stringify({})));
      log(fmt(r));
      return r.errno === 0 && r.stdout.includes("axeron-ok");
    },
  },
  {
    id: "axeron-exec-async",
    title: "Axeron.exec(command, options, callback)",
    desc: "Callback variant: returns at once and calls back with (errno, stdout, stderr). Sleeps 1s: the page must not freeze.",
    async run(log) {
      if (!hasAxeron) { log("window.Axeron is missing"); return false; }
      const r = await axeronExecAsync("echo axeron-async-ok; sleep 1", {});
      log(fmt(r));
      return r.errno === 0 && r.stdout.includes("axeron-async-ok");
    },
  },
  {
    id: "axeron-module",
    title: "axeron.js helper (ax://axeron.js/)",
    desc: "Dynamic import of the bundled module, then its async exec(). Sleeps 1s: the page must not freeze.",
    async run(log) {
      try {
        const mod = await import("ax://axeron.js/");
        const r = await mod.exec("echo axeron-module-ok; sleep 1");
        log(fmt(r));
        return r.stdout.includes("axeron-module-ok");
      } catch (e) {
        log(`import failed: ${e}`);
        return false;
      }
    },
  },
  {
    id: "ui",
    title: "ksu.toast / ksu.fullScreen / Axeron.toast",
    desc: "Shows two toasts and hides the system bars for 3 seconds.",
    async run(log) {
      ksu.toast("ksu.toast works");
      if (hasAxeron) setTimeout(() => Axeron.toast("Axeron.toast works"), 1500);
      ksu.fullScreen(true);
      await new Promise((r) => setTimeout(r, 3000));
      ksu.fullScreen(false);
      log("Did you see both toasts and the bars disappear?");
    },
  },
  {
    id: "packages",
    title: "Package APIs and app icons",
    desc: "listPackages / getPackagesInfo / getPackagesIcons (all synchronous), ax://package.icon/ and ksu://icon/.",
    blocking: true,
    async run(log, card) {
      const user = JSON.parse(ksu.listPackages("user"));
      const system = JSON.parse(ksu.listPackages("system"));
      const all = JSON.parse(ksu.listPackages("all"));
      const sample = user.slice(0, 3);
      const info = JSON.parse(ksu.getPackagesInfo(JSON.stringify(sample)));
      const icons = JSON.parse(ksu.getPackagesIcons(JSON.stringify(sample), 96));
      log(
        `user: ${user.length}, system: ${system.length}, all: ${all.length}\n` +
        `getPackagesInfo:\n${JSON.stringify(info, null, 2)}`,
      );
      const box = card.querySelector(".icons");
      box.innerHTML = "";
      sample.forEach((pkg, i) => {
        const add = (src, label) => {
          const f = document.createElement("figure");
          f.innerHTML = `<img alt=""><figcaption>${label}</figcaption>`;
          f.querySelector("img").src = src;
          box.appendChild(f);
        };
        if (icons[i] && icons[i].icon) add(icons[i].icon, "base64");
        add(`ax://package.icon/${pkg}`, "ax://");
        add(`ksu://icon/${pkg}`, "ksu://");
      });
      const unlabeled = info.filter((p) => !p.appLabel || p.appLabel === "null");
      if (unlabeled.length) log(`${card.querySelector("pre").textContent}\n\nno label: ${unlabeled.map((p) => p.packageName).join(", ")}`);
      return all.length === user.length + system.length && info.every((p) => !p.error) && unlabeled.length === 0;
    },
    extra: '<div class="icons"></div>',
  },
  {
    id: "packages-loop",
    title: "getPackagesInfo, one call per app",
    desc: "How app-list pages often use it: one synchronous call per package, for every user app. Each call must be cheap (the list is fetched once per page), and every app needs a label.",
    blocking: true,
    async run(log) {
      const user = JSON.parse(ksu.listPackages("user"));
      const start = performance.now();
      const infos = user.map((pkg) => JSON.parse(ksu.getPackagesInfo(JSON.stringify([pkg])))[0]);
      const ms = Math.round(performance.now() - start);
      const unlabeled = infos.filter((p) => p.error || !p.appLabel || p.appLabel === "null");
      const perCall = user.length ? (ms / user.length).toFixed(1) : "0";
      log(
        `${user.length} calls in ${ms} ms (${perCall} ms per call)\n` +
        `no label: ${unlabeled.length ? unlabeled.map((p) => p.packageName).join(", ") : "none"}\n\n` +
        infos.slice(0, 10).map((p) => `${p.appLabel}  (${p.packageName})`).join("\n"),
      );
      return unlabeled.length === 0;
    },
  },
  {
    id: "files",
    title: "ksu.writeFile / readFile / listFile / copyFile / moveFile / removeFile",
    desc: "Synchronous file API, run as the server: writes a file in run/, reads it back, copies, moves, lists and removes it.",
    blocking: true,
    async run(log) {
      const dir = `${await getPluginDir()}/run`;
      const text = "line 1\nquote ' and $HOME stay literal\nno newline at the end";
      const lines = [];
      const step = (name, value) => { lines.push(`${name}: ${JSON.stringify(value)}`); return value; };
      const wrote = step("writeFile", ksu.writeFile(`${dir}/tf-file.txt`, text));
      const same = step("readFile matches", ksu.readFile(`${dir}/tf-file.txt`) === text);
      const copied = step("copyFile", ksu.copyFile(`${dir}/tf-file.txt`, `${dir}/tf-copy.txt`));
      const moved = step("moveFile", ksu.moveFile(`${dir}/tf-copy.txt`, `${dir}/tf-moved.txt`));
      const listed = step("listFile", JSON.parse(ksu.listFile(dir)).filter((n) => n.startsWith("tf-")).sort());
      const removed = step("removeFile", ksu.removeFile(`${dir}/tf-file.txt`) && ksu.removeFile(`${dir}/tf-moved.txt`));
      const gone = step("listFile after", JSON.parse(ksu.listFile(dir)).filter((n) => n.startsWith("tf-")));
      step("readFile of a missing file", ksu.readFile(`${dir}/tf-missing.txt`));
      log(lines.join("\n"));
      return wrote && same && copied && moved && removed &&
        listed.join() === "tf-file.txt,tf-moved.txt" && gone.length === 0;
    },
  },
  {
    id: "file-output-stream",
    title: "ksu.fileOutputStream()",
    desc: "open / write (base64) / writeByte / flush / close, then open(path, true) to append. close() returns once the server has the whole file.",
    blocking: true,
    async run(log) {
      const path = `${await getPluginDir()}/run/tf-stream.txt`;
      const fos = ksu.fileOutputStream();
      const lines = [];
      const step = (name, value) => { lines.push(`${name}: ${JSON.stringify(value)}`); return value; };
      const id = step("open", fos.open(path));
      const wrote = step("write", fos.write(id, btoa("chunk-1;"))) &&
        step("write", fos.write(id, btoa("chunk-2;"))) &&
        step("writeByte '!'", fos.writeByte(id, 33)) &&
        step("flush", fos.flush(id));
      const closed = step("close", fos.close(id));
      step("close again", fos.close(id));
      const appendId = step("open(path, true)", fos.open(path, true));
      const appended = step("write", fos.write(appendId, btoa("+appended"))) && step("close", fos.close(appendId));
      const content = step("readFile", ksu.readFile(path));
      ksu.removeFile(path);
      log(lines.join("\n"));
      return id !== "" && wrote && closed && appended && content === "chunk-1;chunk-2;!+appended";
    },
  },
  {
    id: "insets",
    title: "ksu.enableInsets / kernelsu.js enableEdgeToEdge",
    desc: "ksu.enableInsets(false) keeps the page clear of the system bars for 3 seconds, then the helper's enableEdgeToEdge() draws under them again.",
    async run(log) {
      const mod = await import("ax://kernelsu.js/");
      ksu.enableInsets(false);
      await new Promise((r) => setTimeout(r, 3000));
      mod.enableEdgeToEdge();
      log("Did the page move off the status and navigation bars, then back under them?");
    },
  },
  {
    id: "exit",
    title: "ksu.exit() / kernelsu.js exit()",
    desc: "Closes this WebUI through the helper's exit(), which calls ksu.exit(). It should leave recents too.",
    async run(log) {
      const mod = await import("ax://kernelsu.js/");
      log("Closing in 1 second…");
      await new Promise((r) => setTimeout(r, 1000));
      mod.exit();
    },
  },
  {
    id: "shortcut",
    title: "ksu.createShortcut()",
    desc: "Asks the launcher to pin a home-screen shortcut to this WebUI (banner icon). Returns at once.",
    async run(log) {
      const ok = ksu.createShortcut();
      log(`createShortcut(): ${ok}\nDid the launcher ask to add the shortcut (or say it was updated)?`);
      return ok;
    },
  },
  {
    id: "marker",
    title: "Process marker and lifecycle",
    desc: "Starts two daemons from the WebUI, one in its own session (setsid) and one plain background job, and lists every process carrying this plugin's marker. Both must outlive the command that started them. Disable the plugin and re-ignite: all of them must be gone.",
    async run(log) {
      const dir = await getPluginDir();
      await execAsync(
        `setsid sh -c 'sleep 86400' </dev/null >/dev/null 2>&1 & echo \\$! > ${dir}/run/webui-daemon.pid`,
        { cwd: dir },
      );
      // No setsid: it stays in the exec's session, which must not be killed once exec returns.
      await execAsync(
        `sh -c 'sleep 86400' </dev/null >/dev/null 2>&1 & echo \\$! > ${dir}/run/webui-bg.pid`,
        { cwd: dir },
      );
      const r = await execAsync(
        `cat /proc/self/environ | tr '\\0' '\\n' | grep '^AXERON_PLUGIN='; echo; ` +
        `for n in webui-daemon webui-bg; do p=\\$(cat run/\\$n.pid); ` +
        `[ -d /proc/\\$p ] && echo \\$n=alive || echo \\$n=gone; done; echo; axtest procs`,
        { cwd: dir },
      );
      log(fmt(r));
      return r.stdout.includes(`AXERON_PLUGIN=${PLUGIN_ID}`) &&
        r.stdout.includes("webui-daemon=alive") &&
        r.stdout.includes("webui-bg=alive");
    },
  },
  {
    id: "action",
    title: "Run action.sh",
    desc: "The same diagnostics as the Action button, run from the WebUI.",
    async run(log) {
      const dir = await getPluginDir();
      const r = await execAsync("sh ./action.sh", { cwd: dir });
      log(withFailures(fmt(r), r.stdout));
      return r.errno === 0 && failedChecks(r.stdout).length === 0;
    },
  },
  {
    id: "reports",
    title: "Reports and logs",
    desc: "What customize.sh, post-fs-data.sh and service.sh wrote, and the igniter's log file.",
    async run(log) {
      const dir = await getPluginDir();
      const r = await execAsync(
        `for f in reports/*.txt; do echo; echo ===== \\$f; cat \\$f; done; ` +
        `echo; echo ===== logs; ls -l $AXERONDIR/logs; ` +
        `echo; echo ===== ${PLUGIN_ID}.post-fs-data.log; cat $AXERONDIR/logs/${PLUGIN_ID}.post-fs-data.log`,
        { cwd: dir },
      );
      log(withFailures(fmt(r), r.stdout));
      return failedChecks(r.stdout).length === 0;
    },
  },
  {
    id: "slow",
    title: "Toggle slow post-fs-data.sh",
    desc: "Creates or removes the slow-post-fs-data flag: post-fs-data.sh then sleeps 50s, past the 40s limit. Disable + enable the plugin and re-ignite to try it.",
    async run(log) {
      const dir = await getPluginDir();
      const r = await execAsync(
        "if [ -f slow-post-fs-data ]; then rm slow-post-fs-data; echo slow mode OFF; " +
        "else touch slow-post-fs-data; echo slow mode ON; fi",
        { cwd: dir },
      );
      log(fmt(r));
    },
  },
  {
    id: "css",
    title: "/internal/insets.css and /internal/colors.css",
    desc: "CSS variables AxManager serves to every WebUI.",
    async run(log, card) {
      const css = getComputedStyle(document.documentElement);
      const names = [
        "--safe-area-inset-top", "--safe-area-inset-bottom",
        "--window-inset-top", "--window-inset-bottom",
        "--primary", "--onPrimary", "--secondaryContainer", "--surface", "--background", "--error",
      ];
      log(names.map((n) => `${n}: ${css.getPropertyValue(n).trim() || "<unset>"}`).join("\n"));
      const box = card.querySelector(".swatches");
      box.innerHTML = "";
      ["primary", "secondary", "tertiary", "primaryContainer", "surfaceContainer", "error"].forEach((c) => {
        const s = document.createElement("span");
        s.title = c;
        s.style.background = `var(--${c})`;
        box.appendChild(s);
      });
      return css.getPropertyValue("--primary").trim() !== "";
    },
    extra: '<div class="swatches"></div>',
  },
];

// ---------- page ----------

function render() {
  const main = document.getElementById("tests");
  for (const t of tests) {
    const card = document.createElement("section");
    card.className = "card";
    card.innerHTML =
      `<header><h2></h2><span class="status"></span><button>Run</button></header>` +
      `<p class="desc muted"></p>${t.extra || ""}<pre></pre>`;
    card.querySelector("h2").textContent = t.title;
    card.querySelector(".desc").textContent = t.desc;
    card.querySelector("button").onclick = () => runTest(t, card);
    t.card = card;
    main.appendChild(card);
  }
}

async function runTest(t, card) {
  const status = card.querySelector(".status");
  const button = card.querySelector("button");
  const pre = card.querySelector("pre");
  const log = (text) => (pre.textContent = text);
  if (button.disabled) return undefined;
  button.disabled = true;
  status.className = "status run";
  status.textContent = "running";
  pre.textContent = "";

  const stopMeter = startMeter();
  await nextPaint(); // "running" and the spinner are on screen before the call starts
  let ok;
  try {
    if (!hasKsu) throw new Error("window.ksu is missing: open this page from AxManager");
    ok = await t.run(log, card);
  } catch (e) {
    log(`${pre.textContent}\n\nthrew: ${e && e.stack ? e.stack : e}`);
    ok = false;
  }
  const frozeMs = stopMeter();
  button.disabled = false;

  // A non-blocking call that froze the page is a problem even when its output is right.
  const froze = frozeMs > FREEZE_LIMIT_MS;
  let verdict = ok === undefined ? "done" : ok ? "pass" : "fail";
  let kind = ok === false ? "fail" : "pass";
  if (froze && !t.blocking && ok !== false) {
    verdict = "froze";
    kind = "warn";
  }
  status.className = `status ${kind}`;
  status.textContent = `${verdict} · ${frozeMs} ms`;
  log(`${pre.textContent}\n\n[page] longest freeze: ${frozeMs} ms` +
    (t.blocking ? " (synchronous call, a freeze is expected)" : froze ? " — blocked the page" : ""));
  return ok;
}

document.getElementById("run-all").onclick = async (event) => {
  const button = event.currentTarget;
  button.disabled = true;
  // The UI, insets and shortcut tests need eyes, exit closes the page, the slow toggle changes
  // state: all stay manual.
  for (const t of tests) {
    if (["ui", "insets", "exit", "shortcut", "slow"].includes(t.id)) continue;
    await runTest(t, t.card);
  }
  button.disabled = false;
};

document.getElementById("clear-all").onclick = () => {
  for (const t of tests) {
    t.card.querySelector("pre").textContent = "";
    t.card.querySelector(".status").className = "status";
    t.card.querySelector(".status").textContent = "";
  }
};

document.getElementById("bridge").textContent =
  `ksu: ${hasKsu ? "available" : "missing"} · Axeron: ${hasAxeron ? "available" : "missing"} · ${location.href}`;

render();
