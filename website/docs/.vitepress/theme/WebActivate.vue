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

function log(line: string) {
  lines.value.push(line);
  nextTick(() => logEl.value?.scrollTo({ top: logEl.value.scrollHeight }));
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
    log(`Selected ${device.name} (${device.serial})`);
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

async function open(device: AdbDaemonWebUsbDevice) {
  try {
    const { AdbDaemonWebUsbDevice } = await import("@yume-chan/adb-daemon-webusb");
    const { Adb, AdbDaemonTransport } = await import("@yume-chan/adb");
    const { default: AdbWebCredentialStore } = await import("@yume-chan/adb-credential-web");

    let connection;
    try {
      connection = await device.connect();
    } catch (e) {
      if (e instanceof AdbDaemonWebUsbDevice.DeviceBusyError) {
        log(`The device is in use by another program, usually adb from Android Studio. Close Android Studio and other ADB tools, run \`${KILL_SERVER}\` on this computer, then try again.`);
        deviceBusy.value = true;
      } else {
        log(`Can't open the device: ${e}`);
      }
      state.value = "failed";
      return;
    }

    log("Waiting for you to allow USB debugging on the device...");
    const transport = await AdbDaemonTransport.authenticate({
      serial: device.serial,
      connection,
      credentialStore: new AdbWebCredentialStore("AxManager Web"),
    });
    adb.value = new Adb(transport);
    deviceName.value = device.name;
    log(`Connected to ${device.name}`);
    state.value = "connected";

    transport.disconnected.then(() => {
      if (adb.value?.transport !== transport) return;
      adb.value = undefined;
      log("Device disconnected.");
      if (state.value !== "done") state.value = "idle";
    });
  } catch (e) {
    log(`Connect failed: ${e}`);
    adb.value?.close();
    adb.value = undefined;
    state.value = "failed";
  }
}

async function disconnect() {
  const current = adb.value;
  adb.value = undefined;
  await current?.close();
  deviceName.value = "";
  state.value = "idle";
}

async function activate() {
  if (!adb.value) return;
  state.value = "activating";
  const shell = adb.value.subprocess.noneProtocol;
  try {
    // Same path the app shows under View Command: <apk dir>/lib/<abi>/libaxeron.so
    const pmPath = (await shell.spawnWaitText(`pm path ${PACKAGE}`)).trim();
    log(`$ pm path ${PACKAGE}`);
    log(pmPath || "(nothing)");
    const apk = pmPath.split("\n")[0]?.replace(/^package:/, "").trim();
    if (!apk) {
      log("AxManager is not installed on this device. Install it first, then try again.");
      state.value = "failed";
      return;
    }
    const apkDir = apk.substring(0, apk.lastIndexOf("/"));
    const starter = (await shell.spawnWaitText(`ls ${apkDir}/lib/*/libaxeron.so`)).trim().split("\n")[0];
    if (!starter || !starter.endsWith("libaxeron.so")) {
      log(`Can't find libaxeron.so under ${apkDir}/lib. Try reinstalling AxManager.`);
      state.value = "failed";
      return;
    }

    log(`$ ${starter}`);
    const process = await shell.spawn(`${starter} 2>&1; echo "__AX_EXIT=$?"`);
    const reader = process.output.getReader();
    const decoder = new TextDecoder();
    let pending = "";
    let exitCode: number | undefined;
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      pending += decoder.decode(value, { stream: true });
      const parts = pending.split("\n");
      pending = parts.pop()!;
      for (const line of parts) {
        const exit = line.match(/^__AX_EXIT=(\d+)/);
        if (exit) exitCode = Number(exit[1]);
        else log(line);
      }
    }
    if (pending) log(pending);

    if (exitCode !== 0) {
      log(`Starter exited with ${exitCode ?? "no exit code"}.`);
      state.value = "failed";
      return;
    }

    // The starter exits once it forked the server; check that the server stayed up.
    await new Promise((resolve) => setTimeout(resolve, 2000));
    const pid = (await shell.spawnWaitText(`pidof ${SERVER_NAME}`)).trim();
    if (pid) {
      log(`${SERVER_NAME} is running (pid ${pid}).`);
      state.value = "done";
    } else {
      log(`${SERVER_NAME} is not running. Open AxManager and check the activation log.`);
      state.value = "failed";
    }
  } catch (e) {
    log(`Activate failed: ${e}`);
    state.value = "failed";
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
          <button class="ax-activate-btn brand" :disabled="busy" @click="activate">Activate</button>
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
