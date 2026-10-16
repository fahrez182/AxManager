# Plugins

Plugins kept with the AxManager source. Each folder is a plugin; `build.py` packs it into an
installable zip.

| Plugin | What it is |
|---|---|
| [`test_function`](test_function) | **Test Function**: exercises the whole plugin environment and WebUI API, and reports PASS/FAIL |

## Build

```bash
py plugins/build.py                  # every plugin -> plugins/out/<id>-<version>.zip
py plugins/build.py test_function    # one plugin
py plugins/build.py --abort          # Test Function variant whose customize.sh calls abort
```

(`python3` instead of `py` on Linux/macOS.) The script checks `module.prop` with the installer's
rules (id pattern, numeric `versionCode` and `axeronPlugin`, id = folder name) and writes text
files with LF line endings, so a Windows checkout still produces working scripts.
`.gitattributes` keeps them LF in git as well.

Install the zip with **+** on AxManager's Plugin page, then re-ignite.

## Test Function

`id=test_function`, `axeronPlugin=19100`. Every stage writes what it saw to
`$AXERONDIR/plugins/test_function/reports/<stage>.txt` (and to its own output), with
`[PASS]`/`[FAIL]` checks and `[INFO]` values.

| File | What it tests |
|---|---|
| `customize.sh` | Installer variables (`MODPATH`, `ZIPFILE`, `ARCH`, `API`, `IS64BIT`, …), server env, `AXERON_PLUGIN` at install, `grep_prop`, reading `ZIPFILE` by hand, picking a file per `ARCH`, `set_perm` (chown 0:0 is best effort, chmod always applied, context only when given), `set_perm_recursive`, a process started at install (must be stopped by the next re-ignite), `abort` (with `--abort`) |
| `post-fs-data.sh` | Marker, stdin `/dev/null`, output to `logs/test_function.post-fs-data.log`; touch `slow-post-fs-data` in the plugin folder to see the 40s limit |
| `service.sh` | `ASH_STANDALONE=1`, marker, `system.prop` applied, `axtest` on `PATH`; starts a heartbeat daemon in its own session and a child with a cleared env (no marker, found through the daemon's session) |
| `system.prop` | `debug.axeron.test_function=loaded` |
| `system/bin/axtest` | Linked into `$AXERONXBIN`: `axtest ping \| env \| marker \| procs` from QuickShell or anywhere |
| `action.sh` | Full diagnostics: env, working dir and `PATH` order, boot stages, which processes are alive and marked, server settings (stop-with-server, Shizuku intercept), and every `[FAIL]` from all reports |
| `uninstall.sh` | Runs on removal; writes `logs/test_function.uninstalled`, leaves a `sleep` running that AxManager must stop |
| `webroot/` | Every `ksu` and `Axeron` method (sync and callback), streaming `spawn` and its event order, the `kernelsu.js`/`axeron.js` helpers (`spawn` with args and/or options), quoting, app icons, insets/colors CSS, a daemon started from the WebUI, reports and logs. Each test shows how long it froze the page: synchronous calls are expected to, callback ones must not |
| `banner.png` | `banner=` in `module.prop` |
| `README.md` | Deleted by the installer after `customize.sh` (checked there) |
| `extras/` | Read straight from `ZIPFILE` in `customize.sh`, then removed from the plugin folder |

### Suggested run

1. Install the zip. Read the installer output (customize report).
2. Re-ignite (flame button, or Home → power → flame).
3. Tap **Action**: everything should be PASS; the install-time daemon should be gone, the heartbeat
   daemon alive and marked, the clean-env child alive without a marker.
4. Open the WebUI, **Run all**. Then try the toast/full-screen card by hand.
5. In the WebUI, run *Process marker and lifecycle*. Disable the plugin and re-ignite: `axtest procs`
   in QuickShell should list nothing for `test_function` (the heartbeat, its clean-env child and the
   WebUI daemon are all stopped).
6. Turn on **Settings → Stop plugins with the server**, enable the plugin, re-ignite, then stop the
   server: after a restart, the old daemons must be gone.
7. Uninstall and re-ignite: `logs/test_function.uninstall.log` and `logs/test_function.uninstalled`
   exist, and the `sleep 3600` it left is gone.
8. `py plugins/build.py --abort`, install that zip: the install fails with the abort message and no
   `test_function` folder is left behind.
