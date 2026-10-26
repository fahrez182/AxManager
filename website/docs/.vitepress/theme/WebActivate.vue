<script setup lang="ts">
// <WebActivate /> in markdown: starts AxManager from the browser over WebUSB,
// the same command as "Start by connecting to a computer", without installing adb.
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from "vue";
import type { Adb } from "@yume-chan/adb";
import type { AdbDaemonWebUsbDevice } from "@yume-chan/adb-daemon-webusb";

const PACKAGE = "frb.axeron.manager";
const SERVER_NAME = "axeron_server";
const KILL_SERVER = "adb kill-server";

type State = "idle" | "connecting" | "connected" | "activating" | "done" | "failed";

const supported = ref(true);
const state = ref<State>("idle");
const deviceName = ref("");
const lines = ref<string[]>([]);
const logEl = ref<HTMLElement>();
// Another program (adb.exe, usually from Android Studio) holds the device.
const deviceBusy = ref(false);
const copied = ref(false);

const adb = shallowRef<Adb>();
// Kept so Try again can reopen it without the picker.
let selected: AdbDaemonWebUsbDevice | undefined;

const busy = computed(() => state.value === "connecting" || state.value === "activating");
const connected = computed(() => adb.value !== undefined && state.value !== "idle" && state.value !== "connecting");

// Reconnects after the USB link drops while authorizing, before giving up.
const MAX_ATTEMPTS = 3;
// How long a dropped device gets to come back.
const REAPPEAR_MS = 15000;
// Silence from the starter before the log says it is still waiting.
const QUIET_MS = 15000;
// How long the first command of a new session may take before it counts as dead.
const PROBE_MS = 10000;
// How long adbd gets to say it is restarting on TCP, or to drop the USB link doing it.
const TCP_SWITCH_MS = 5000;
const TCP_ACTIVE_MARK = "__AX_TCP_ACTIVE=";

function log(line: string) {
  lines.value.push(line);
  nextTick(() => logEl.value?.scrollTo({ top: logEl.value.scrollHeight }));
}

// Chrome reports a USB link that went away mid-transfer as a NetworkError ("A transfer error
// has occurred"), sometimes before any disconnect event. Many phones restart USB debugging
// right after Allow is tapped, so it is common during authorization.
function isUsbDrop(e: unknown): boolean {
  return e instanceof Error && (e.name === "NetworkError" || /transfer error/i.test(e.message));
}

// The session is gone and a reconnect may bring it back: the link dropped, or adbd restarted
// and asked to authenticate again. That AUTH packet (0x48545541, "AUTH") arriving after the
// handshake is what the library reports as "Unknown command: 48545541".
function isSessionLost(e: unknown): boolean {
  return isUsbDrop(e) ||
    (e instanceof Error && (/Unknown command: 48545541/.test(e.message) || /USB connection lost/.test(e.message)));
}

// Polls for the device with [serial] until it is back (a dropped device returns as a new
// USBDevice, which Chrome still lets us open without the picker).
async function reappear(serial: string): Promise<AdbDaemonWebUsbDevice | undefined> {
  const { AdbDaemonWebUsbDeviceManager } = await import("@yume-chan/adb-daemon-webusb");
  const deadline = Date.now() + REAPPEAR_MS;
  while (Date.now() < deadline) {
    await new Promise((resolve) => setTimeout(resolve, 500));
    const devices = await AdbDaemonWebUsbDeviceManager.BROWSER!.getDevices();
    const found = devices.find((d) => d.serial === serial);
    if (found) return found;
  }
  return undefined;
}

onMounted(() => {
  supported.value = typeof navigator !== "undefined" && "usb" in navigator;
});

onBeforeUnmount(() => {
  adb.value?.close();
});

async function connect() {
  state.value = "connecting";
  lines.value = [];
  deviceBusy.value = false;
  try {
    const { AdbDaemonWebUsbDeviceManager } = await import("@yume-chan/adb-daemon-webusb");
    const manager = AdbDaemonWebUsbDeviceManager.BROWSER!;
    const device = await manager.requestDevice();
    if (!device) {
      log("No device selected.");
      state.value = "idle";
      return;
    }
    log(`Selected ${device.name || "device"} (${device.serial})`);
    selected = device;
    await open(device);
  } catch (e) {
    log(`Connect failed: ${e}`);
    state.value = "failed";
  }
}

async function retry() {
  if (!selected) return connect();
  state.value = "connecting";
  deviceBusy.value = false;
  log("Trying again...");
  await open(selected);
}

async function copyKillServer() {
  try {
    await navigator.clipboard.writeText(KILL_SERVER);
    copied.value = true;
    setTimeout(() => (copied.value = false), 2000);
  } catch {
    log(`Can't copy, type it yourself: ${KILL_SERVER}`);
  }
}

async function open(device: AdbDaemonWebUsbDevice, attempt = 1) {
  let candidate: Adb | undefined;
  try {
    const { AdbDaemonWebUsbDevice } = await import("@yume-chan/adb-daemon-webusb");
    const { Adb, AdbDaemonTransport } = await import("@yume-chan/adb");
    const { default: AdbWebCredentialStore } = await import("@yume-chan/adb-credential-web");

    let connection;
    try {
      connection = await device.connect();
    } catch (e) {
      if (!(e instanceof AdbDaemonWebUsbDevice.DeviceBusyError)) throw e;
      // Any NetworkError while claiming reads as busy, including our own claim from a failed
      // attempt on this page. Release it and claim once more before blaming another program.
      await device.raw.close().catch(() => {});
      try {
        connection = await device.connect();
      } catch (again) {
        if (!(again instanceof AdbDaemonWebUsbDevice.DeviceBusyError)) throw again;
        log(`Another program holds this phone's ADB interface: an adb server (Android Studio, scrcpy, a phone suite such as Mi PC Suite), or this page open in another tab. Close it, or run \`${KILL_SERVER}\` on this computer, then try again.`);
        deviceBusy.value = true;
        state.value = "failed";
        return;
      }
    }

    log("Waiting for you to allow USB debugging on the device...");
    const transport = await AdbDaemonTransport.authenticate({
      serial: device.serial,
      connection,
      credentialStore: new AdbWebCredentialStore("AxManager Web"),
    });
    candidate = new Adb(transport);
    // Some phones reset adbd once more after saving the key, without the USB link going away:
    // the session then dies on its first command. Find out now rather than at Activate.
    await probe(candidate);

    adb.value = candidate;
    deviceName.value = device.name || device.serial;
    log(`Connected to ${deviceName.value}`);
    state.value = "connected";

    transport.disconnected.then(() => {
      if (adb.value?.transport !== transport) return;
      adb.value = undefined;
      log("Device disconnected.");
      if (state.value !== "done" && state.value !== "activating") state.value = "idle";
    });
  } catch (e) {
    await candidate?.close().catch(() => {});
    if (adb.value === candidate) adb.value = undefined;
    // Let go of the interface, or the next attempt (or Try again) finds it claimed.
    await device.raw.close().catch(() => {});
    if (isSessionLost(e) && attempt < MAX_ATTEMPTS) {
      log(`The connection to the phone was reset (${e}). Phones often restart USB debugging right after Allow; waiting for it to come back...`);
      const again = await reappear(device.serial);
      if (again) {
        selected = again;
        log(`Reconnecting (attempt ${attempt + 1} of ${MAX_ATTEMPTS})...`);
        return open(again, attempt + 1);
      }
      log("The device didn't come back. Unplug and replug the cable, keep the phone unlocked, then press Connect again.");
    } else if (isSessionLost(e)) {
      log(`The connection keeps getting reset (${e}). Try another cable or port, keep both screens on, then press Connect again.`);
    } else {
      log(`Connect failed: ${e}`);
    }
    state.value = "failed";
  }
}

// One harmless command through the new session; throws if the phone reset it meanwhile.
async function probe(device: Adb) {
  const reset = device.disconnected.then(() => {
    throw new Error("USB connection lost");
  });
  const timeout = new Promise<never>((_, reject) =>
    setTimeout(() => reject(new Error("USB connection lost: the phone stopped answering")), PROBE_MS),
  );
  await Promise.race([device.subprocess.noneProtocol.spawnWaitText("echo ok"), reset, timeout]);
}

type TcpOutcome = "switched" | "unchanged" | "unconfirmed";

// Like the app's OTG activation: ADB goes to TCP only when AxManager's TCP mode is on (its
// provider answers adb's shell), on the port set there. Not raced against the USB link like
// activate's steps: the link dropping here is adbd restarting, not a failure.
async function applyTcpMode(device: Adb): Promise<TcpOutcome> {
  const text = await device.subprocess.noneProtocol.spawnWaitText(
    `content call --uri content://${PACKAGE}.server --method getTcpConfig 2>&1; ` +
      `echo ${TCP_ACTIVE_MARK}$(getprop service.adb.tcp.port)`,
  );
  const mode = text.match(/tcp_mode=(true|false)/)?.[1];
  const port = Number(text.match(/tcp_port=(\d+)/)?.[1]) || undefined;
  // service.adb.tcp.port is "" or "0" while adbd is on USB.
  const activePort = Number(text.match(new RegExp(`${TCP_ACTIVE_MARK}(\\d+)`))?.[1]) || undefined;
  log(`TCP mode ${mode ?? "?"}, configured port ${port ?? "?"}, ADB on TCP port ${activePort ?? "none"}`);

  if (mode === undefined) {
    log("Couldn't read AxManager's TCP mode. ADB stays on USB.");
    return "unchanged";
  }
  if (mode !== "true") {
    log("TCP mode is off in AxManager. ADB stays on USB.");
    return "unchanged";
  }
  if (port === undefined || port < 1024 || port > 65535) {
    log(`AxManager's TCP port isn't valid (${port}). ADB stays on USB.`);
    return "unchanged";
  }
  if (activePort === port) {
    log(`ADB already listens on TCP port ${port}.`);
    return "unchanged";
  }

  log(activePort !== undefined
    ? `ADB is on TCP port ${activePort}, AxManager wants ${port}: switching it to ${port}...`
    : `TCP mode is on: switching ADB to TCP port ${port}...`);
  // setPort waits for adbd to close the socket, which a restarting adbd may never do over USB.
  const outcome = await Promise.race([
    device.tcpip.setPort(port).then(
      () => "switched",
      (e) => (/restarting/i.test(String(e?.message ?? e)) ? "switched" : `error: ${e?.message ?? e}`),
    ),
    device.disconnected.then(() => "dropped"),
    new Promise<string>((resolve) => setTimeout(() => resolve("timeout"), TCP_SWITCH_MS)),
  ]);
  if (outcome === "switched" || outcome === "dropped") {
    log(`ADB is restarting on TCP port ${port}.`);
    return "switched";
  }
  if (outcome === "timeout") {
    log(`Couldn't confirm the switch to TCP port ${port}. Going on over this connection.`);
    return "unconfirmed";
  }
  log(`Couldn't switch ADB to TCP port ${port} (${outcome.slice("error: ".length)}). ADB stays on USB.`);
  return "unchanged";
}

// Before the starter, as the app does: with TCP mode on, adbd moves to its TCP port first, so its
// restart happens before there is a server to take down. The browser can't reach adbd over TCP,
// but adbd keeps serving USB in TCP mode: the page reconnects over USB and starts the server from
// there. Returns the session to start it on, or undefined when the phone didn't come back.
async function switchToTcpFirst(device: Adb): Promise<Adb | undefined> {
  let outcome: TcpOutcome;
  try {
    outcome = await applyTcpMode(device);
  } catch (e) {
    log(`Couldn't apply AxManager's TCP mode (${e}). ADB stays on USB.`);
    return device;
  }
  if (outcome !== "switched") return device;

  // Let the old session finish dropping, or the old USB device is still listed and gets reopened.
  await Promise.race([
    device.disconnected,
    new Promise((resolve) => setTimeout(resolve, TCP_SWITCH_MS)),
  ]);
  await device.close().catch(() => {});
  if (adb.value === device) adb.value = undefined;

  log("Waiting for the phone to come back over USB...");
  const again = selected && await reappear(selected.serial);
  if (!again) {
    log("The phone didn't come back over USB. ADB now listens on TCP: start AxManager from its TCP Debugging card, or replug the cable and press Connect again.");
    return undefined;
  }
  selected = again;
  state.value = "connecting";
  await open(again);
  if (!adb.value) return undefined;
  state.value = "activating";
  return adb.value;
}

async function disconnect() {
  const current = adb.value;
  adb.value = undefined;
  await current?.close();
  deviceName.value = "";
  state.value = "idle";
}

async function activate(retried = false) {
  if (!adb.value) return;
  state.value = "activating";
  // Once is enough: a retry reconnects after the starter's session was lost, ADB is already set.
  const device = retried ? adb.value : await switchToTcpFirst(adb.value);
  if (!device) {
    state.value = "failed";
    return;
  }
  const shell = device.subprocess.noneProtocol;
  // A dropped USB link ends the shell's output without an exit code, or leaves a read waiting
  // forever; racing every step against it says what happened instead.
  let lost = false;
  const disconnected = device.disconnected.then(() => {
    lost = true;
    throw new Error("USB connection lost");
  });
  disconnected.catch(() => {});
  const step = <T,>(work: Promise<T>) => Promise.race([work, disconnected]);
  let quiet: ReturnType<typeof setInterval> | undefined;
  try {
    // Same path the app shows under View Command: <apk dir>/lib/<abi>/libaxeron.so
    log(`$ pm path ${PACKAGE}`);
    const pmPath = (await step(shell.spawnWaitText(`pm path ${PACKAGE}`))).trim();
    log(pmPath || "(nothing)");
    const apk = pmPath.split("\n")[0]?.replace(/^package:/, "").trim();
    if (!apk) {
      log("AxManager is not installed on this device. Install it first, then try again.");
      state.value = "failed";
      return;
    }
    const apkDir = apk.substring(0, apk.lastIndexOf("/"));
    const starter = (await step(shell.spawnWaitText(`ls ${apkDir}/lib/*/libaxeron.so`))).trim().split("\n")[0];
    if (!starter || !starter.endsWith("libaxeron.so")) {
      log(`Can't find libaxeron.so under ${apkDir}/lib. Try reinstalling AxManager.`);
      state.value = "failed";
      return;
    }

    log(`$ ${starter}`);
    const process = await step(shell.spawn(`${starter} 2>&1; echo "__AX_EXIT=$?"`));
    const reader = process.output.getReader();
    const decoder = new TextDecoder();
    let pending = "";
    let exitCode: number | undefined;
    let lastOutput = Date.now();
    quiet = setInterval(() => {
      const seconds = Math.round((Date.now() - lastOutput) / 1000);
      if (seconds >= QUIET_MS / 1000) log(`(no output from the starter for ${seconds} s, still waiting)`);
    }, QUIET_MS);
    while (true) {
      const { done, value } = await step(reader.read());
      if (done) break;
      lastOutput = Date.now();
      pending += decoder.decode(value, { stream: true });
      const parts = pending.split("\n");
      pending = parts.pop()!;
      for (const raw of parts) {
        // Older adbd runs the command on a PTY, which ends lines with \r\n.
        const line = raw.replace(/\r$/, "");
        const exit = line.match(/^__AX_EXIT=(\d+)/);
        if (exit) exitCode = Number(exit[1]);
        else log(line);
      }
    }
    clearInterval(quiet);
    if (pending) log(pending.replace(/\r$/, ""));

    if (exitCode !== 0) {
      log(exitCode === undefined
        ? "The starter's output ended without an exit code."
        : `Starter exited with ${exitCode}.`);
      state.value = "failed";
      return;
    }

    // The starter exits once it forked the server; check that the server stayed up.
    await new Promise((resolve) => setTimeout(resolve, 2000));
    log(`$ pidof ${SERVER_NAME}`);
    const pid = (await step(shell.spawnWaitText(`pidof ${SERVER_NAME}`))).trim();
    if (pid) {
      log(`${SERVER_NAME} is running (pid ${pid}).`);
      state.value = "done";
    } else {
      log(`${SERVER_NAME} is not running. Open AxManager and check the activation log.`);
      state.value = "failed";
    }
  } catch (e) {
    clearInterval(quiet);
    if ((lost || isSessionLost(e)) && !retried && selected) {
      // Running the starter again is safe: it kills an old server before starting a new one.
      log(`The connection to the phone was reset during activation (${e}). Reconnecting to run it again...`);
      await device.close().catch(() => {});
      if (adb.value === device) adb.value = undefined;
      state.value = "connecting";
      await open(selected);
      if (adb.value) return activate(true);
      return;
    }
    if (lost || isSessionLost(e)) {
      log(`The connection to the phone was reset during activation again (${e}). The server may have started anyway: open AxManager to check, or press Connect and Activate once more.`);
    } else {
      log(`Activate failed: ${e}`);
    }
    state.value = "failed";
  } finally {
    clearInterval(quiet);
  }
}
</script>

<template>
  <div class="ax-activate">
    <p v-if="!supported" class="ax-activate-warn">
      This browser can't use USB devices (WebUSB). Open this page in Chrome or Edge on a computer, or in Chrome on Android.
    </p>
    <template v-else>
      <div class="ax-activate-status">
        <span class="ax-activate-dot" :data-state="state" />
        <span v-if="state === 'idle'">Not connected</span>
        <span v-else-if="state === 'connecting'">Connecting...</span>
        <span v-else-if="state === 'activating'">Activating on {{ deviceName }}...</span>
        <span v-else-if="state === 'done'">AxManager started on {{ deviceName }}</span>
        <span v-else-if="state === 'failed'">Failed, see the log</span>
        <span v-else>Connected to {{ deviceName }}</span>
      </div>

      <div class="ax-activate-actions">
        <button v-if="!connected" class="ax-activate-btn brand" :disabled="busy" @click="connect">
          Connect
        </button>
        <template v-else>
          <button class="ax-activate-btn brand" :disabled="busy" @click="activate()">Activate</button>
          <button class="ax-activate-btn alt" :disabled="busy" @click="disconnect">Disconnect</button>
        </template>
      </div>

      <div v-if="deviceBusy && !connected" class="ax-activate-busy">
        <code>{{ KILL_SERVER }}</code>
        <button class="ax-activate-btn alt" @click="copyKillServer">{{ copied ? "Copied" : "Copy" }}</button>
        <button class="ax-activate-btn brand" :disabled="busy" @click="retry">Try again</button>
      </div>

      <pre v-if="lines.length" ref="logEl" class="ax-activate-log">{{ lines.join("\n") }}</pre>
    </template>
  </div>
</template>

<style scoped>
.ax-activate {
  margin: 16px 0;
  padding: 20px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  background: var(--vp-c-bg-soft);
}

.ax-activate-warn {
  margin: 0;
  color: var(--vp-c-warning-1);
}

.ax-activate-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.ax-activate-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--vp-c-text-3);
}
.ax-activate-dot[data-state="connected"],
.ax-activate-dot[data-state="connecting"],
.ax-activate-dot[data-state="activating"] {
  background: var(--vp-c-brand-1);
}
.ax-activate-dot[data-state="done"] {
  background: var(--vp-c-green-1, #3a9d5d);
}
.ax-activate-dot[data-state="failed"] {
  background: var(--vp-c-danger-1);
}

.ax-activate-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.ax-activate-btn {
  padding: 0 20px;
  line-height: 38px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  transition: background-color 0.25s, opacity 0.25s;
}
.ax-activate-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ax-activate-btn.brand {
  background: var(--vp-button-brand-bg);
  color: var(--vp-button-brand-text);
}
.ax-activate-btn.brand:not(:disabled):hover {
  background: var(--vp-button-brand-hover-bg);
}
.ax-activate-btn.alt {
  background: var(--vp-button-alt-bg);
  color: var(--vp-button-alt-text);
}
.ax-activate-btn.alt:not(:disabled):hover {
  background: var(--vp-button-alt-hover-bg);
}

.ax-activate-busy {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.ax-activate-log {
  margin: 16px 0 0;
  padding: 12px 14px;
  max-height: 320px;
  overflow: auto;
  border-radius: 8px;
  background: var(--vp-code-block-bg);
  font-family: var(--vp-font-family-mono);
  font-size: 12.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
