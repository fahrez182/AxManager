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
    desc: "Dynamic import of the bundled module, then exec(), spawn(cmd, args) and spawn(cmd, options).",
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
      log(text);
      return r.errno === 0 && r.stdout.includes("from-module") &&
        withArgs.includes("args-ok") && withOptions.includes("/system") && withBoth.includes("both-ok");
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
      const all = JSON.parse(ksu.listAllPackages());
      const sample = user.slice(0, 3);
      const info = JSON.parse(ksu.getPackagesInfo(JSON.stringify(sample)));
      const icons = JSON.parse(ksu.getPackagesIcons(JSON.stringify(sample), 96));
      log(
        `user: ${user.length}, system: ${system.length}, all: ${all.length}\n` +
        `listUserPackages: ${JSON.parse(ksu.listUserPackages()).length}, ` +
        `listSystemPackages: ${JSON.parse(ksu.listSystemPackages()).length}\n` +
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
      return all.length === user.length + system.length && info.every((p) => !p.error);
    },
    extra: '<div class="icons"></div>',
  },
  {
    id: "marker",
    title: "Process marker and lifecycle",
    desc: "Starts a daemon from the WebUI and lists every process carrying this plugin's marker. Disable the plugin and re-ignite: all of them must be gone.",
    async run(log) {
      const dir = await getPluginDir();
      await execAsync(
        `setsid sh -c 'sleep 86400' </dev/null >/dev/null 2>&1 & echo \\$! > ${dir}/run/webui-daemon.pid`,
        { cwd: dir },
      );
      const r = await execAsync(
        `cat /proc/self/environ | tr '\\0' '\\n' | grep '^AXERON_PLUGIN='; echo; axtest procs`,
        { cwd: dir },
      );
      log(fmt(r));
      return r.stdout.includes(`AXERON_PLUGIN=${PLUGIN_ID}`);
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
  // The UI test hides the bars and the slow toggle changes state: both stay manual.
  for (const t of tests) {
    if (t.id === "ui" || t.id === "slow") continue;
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
