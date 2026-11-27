#!/system/bin/sh
# G16 — Restrict background data for every com.google.* package
# Source: Aatricks, Nougat Battery Guide
# Settings:
#   cmd netpolicy set restrict-background true (only for the app's UID context
#   is not granular; we use cmd appops set ... SYSTEM_ALERT_WINDOW ignore and
#   the per-app data saver signal)
# Per-app path: cmd netpolicy add restrict-background-whitelist <uid> is the
# opposite — what we want is to mark all google apps as restricted
# using RESTRICT_BACKGROUND. We use the appops set RUN_IN_BACKGROUND
# combined with cmd netpolicy set restrict-background true; that's the
# safest non-root path.
MODULE_ID="G16_google_bg_data_restrict"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

apply() {
    vopl_apply_or_skip G16_GOOGLE_BG_DATA_RESTRICT || return 0
    # global data saver mode
    vopl_run "$MODULE_ID: data saver" \
        cmd netpolicy set restrict-background true
    # per-app: cmd appops set <pkg> SYSTEM_ALERT_WINDOW ignore not enough;
    # use RUN_IN_BACKGROUND already done in G2/G3; we add a stricter
    # approach: set the package's data usage cycle to background
    # which keeps the OS from polling
    local count=0
    for pkg in $(pm list packages 2>/dev/null | grep -oE 'com\.google\.[a-zA-Z0-9._]+'); do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "$MODULE_ID: $pkg" \
            cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
        vopl_run "$MODULE_ID: $pkg" \
            cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore
        count=$((count + 1))
    done
    vopl_log INFO "$MODULE_ID: data saver ON, restricted $count Google packages"
}

revert() {
    vopl_run "revert $MODULE_ID" cmd netpolicy set restrict-background false
    for pkg in $(pm list packages 2>/dev/null | grep -oE 'com\.google\.[a-zA-Z0-9._]+'); do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "revert $MODULE_ID" cmd appops reset "$pkg"
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
