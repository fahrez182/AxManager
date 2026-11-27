#!/system/bin/sh
# G9 — Force rare bucket for all Google packages
# Source: Appzuku, MrCarb0n
# Ensures every com.google.* package sits in "rare" standby bucket
# so Android almost never wakes it up.
MODULE_ID="G9_gms_rare_bucket_forever"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

apply() {
    vopl_apply_or_skip G9_GMS_RARE_BUCKET_FOREVER || return 0
    vopl_need_api 28 || return 0
    local count=0
    for pkg in $(pm list packages 2>/dev/null | grep -oE 'com\.google\.[a-zA-Z0-9._]+'); do
        vopl_run "$MODULE_ID: $pkg" \
            am set-standby-bucket "$pkg" rare
        count=$((count + 1))
    done
    vopl_log INFO "$MODULE_ID: $count Google packages set to rare"
}

revert() {
    vopl_need_api 28 || return 0
    for pkg in $(pm list packages 2>/dev/null | grep -oE 'com\.google\.[a-zA-Z0-9._]+'); do
        vopl_run "revert $MODULE_ID" \
            am set-standby-bucket "$pkg" active
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
