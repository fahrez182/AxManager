#!/system/bin/sh
# COPG per-app proxy daemon (M2). Runs as ROOT (from service.sh at boot; also `reload`ed on WebUI save).
#
# Reads the flat assignment file the WebUI writes:
#     /data/adb/COPG/proxy/active.conf   — one line per proxied app:
#         <pkg> <type> <host> <port> <user> <pass> [u=<id>|u=all]
#     type = socks5 (http later); user/pass "-" when none. Blank lines / '#' comments ignored.
#     Optional 7th field scopes it to Android users (clones/work-profile/second-space):
#       omitted or u=all -> every user the app is installed for (default; no clone leaks)
#       u=<id>           -> only that user (u=0 = owner only). Mirrors the Zygisk u=<id> spoof tag.
#
# For each DISTINCT proxy (host:port:user:pass) it starts one hev-socks5-tproxy on its own port,
# then hands tproxy.sh one "group" per proxy: <mark>,<port>,<uid,uid,...>. Apps sharing a proxy
# share a hev; different proxies stay isolated. uid is stable, so this is applied once (boot +
# on-change), no foreground polling.
#
#   copg_proxy.sh start | stop | reload | status | watch
set -u

DIR="$(cd "$(dirname "$0")" && pwd)"
MODDIR="/data/adb/modules/COPG"
LIC="$MODDIR/copg_lic"                     # licensing helper (verify + LOAD-BEARING hev launch)
# Encrypted hev binary (ciphertext under K_GPU). There is NO plaintext hev on disk — copg_lic
# recovers K_GPU from a valid, device-bound license and decrypt+exec's it in an anonymous memfd.
ABI="$(getprop ro.product.cpu.abi 2>/dev/null)"
ENC="$DIR/../bin/$ABI/hev_tproxy.enc"
[ -f "$ENC" ] || ENC="$DIR/../bin/arm64-v8a/hev_tproxy.enc"
TPROXY="$DIR/tproxy.sh"
RUN=/data/adb/COPG/proxy
CONF="$RUN/active.conf"
LOG="$RUN/daemon.log"
mkdir -p "$RUN"; chmod 0700 "$RUN" 2>/dev/null   # root-only; holds active.conf (0600)

log() { echo "[$(date '+%H:%M:%S')] $*" >> "$LOG"; }

# LOAD-BEARING PRO GATE. hev is shipped ENCRYPTED; the ONLY way to run it is `copg_lic runhev`,
# which verifies the license, recovers K_GPU, and decrypt+exec's hev in RAM. No license → no K_GPU →
# hev never materializes → routing cannot start. Same uncrackable model as the paid v7878 dexes:
# editing THIS shell script, hand-writing active.conf, or patching a boolean all unlock nothing,
# because the plaintext proxy binary does not exist without a valid, device-bound license.
# Returns 0 (started, pid in $2) or 1 (no license / decrypt failed).
launch_hev() {  # <cfg> <pidfile>
    [ -x "$LIC" ] || { log "copg_lic missing ($LIC)"; return 1; }
    [ -f "$ENC" ] || { log "encrypted hev missing ($ENC)"; return 1; }
    "$LIC" runhev "$ENC" "$1" "$2" >/dev/null 2>>"$LOG"
}

uid_of() {
    u=$(stat -c %u "/data/data/$1" 2>/dev/null)
    [ -n "$u" ] && [ "$u" != 0 ] && { echo "$u"; return; }
    dumpsys package "$1" 2>/dev/null | grep -m1 -oE 'userId=[0-9]+' | cut -d= -f2
}

# Expand a package + optional user scope into the FULL kernel uids to route — one per Android
# user the app is installed for. An app's uid = userId*100000 + appId, so the owner-only appId
# (what uid_of returns) misses clones/work-profile/second-space. Users are read from /data/user/*
# (filesystem, boot-safe — no pm dependency). scope: "u=<id>" one user, else all installed users.
uids_for() {  # <pkg> <scope>
    _pkg=$1; _scope=${2:-}
    _appid=$(uid_of "$_pkg"); [ -n "$_appid" ] || return
    _appid=$(( _appid % 100000 ))
    case "$_scope" in u=all|all|'') _want="" ;; u=*) _want=${_scope#u=} ;; *) _want="" ;; esac
    _out=""
    for _d in /data/user/*/; do
        _u=$(basename "$_d")
        case "$_u" in ''|*[!0-9]*) continue ;; esac        # numeric user dirs only
        [ -n "$_want" ] && [ "$_u" != "$_want" ] && continue
        [ -d "/data/user/$_u/$_pkg" ] || continue          # installed for this user?
        _out="$_out $(( _u * 100000 + _appid ))"
    done
    # fallback to the base appId only for the all-users case (enumeration came up empty, e.g. very
    # early boot); an explicit u=<id> that doesn't resolve stays empty so we don't proxy the wrong user.
    [ -n "$_out" ] || [ -n "$_want" ] || _out=" $_appid"
    echo $_out
}

kill_hevs() { for p in "$RUN"/hev_*.pid; do [ -f "$p" ] && { kill "$(cat "$p")" 2>/dev/null; rm -f "$p"; }; done; }

stop() {
    sh "$TPROXY" teardown >/dev/null 2>&1
    kill_hevs
    log "stopped"
}

start() {
    [ -f "$CONF" ] || { log "no active.conf — nothing to start"; return 0; }
    stop                                  # teardown-first (idempotent; clears a crashed prior run)

    # group apps by proxy signature (host|port|user|pass). Emit groups for tproxy.sh.
    sigs=""; groups=""; n=0
    while read -r pkg type host port user pass rest; do
        case "$pkg" in ''|'#'*) continue;; esac
        case "$type" in socks5|http|socks4) ;; *) log "skip $pkg: unsupported type '$type'"; continue;; esac
        uids=$(uids_for "$pkg" "$rest"); [ -n "$uids" ] || { log "skip $pkg: uid unresolved (not installed?)"; continue; }
        sig="$type|$host|$port|$user|$pass"
        # find existing group index for this proxy sig
        gi=""; i=1
        for s in $sigs; do [ "$s" = "$sig" ] && { gi=$i; break; }; i=$((i+1)); done
        if [ -z "$gi" ]; then
            n=$((n+1)); gi=$n; sigs="$sigs $sig"
            hport=$((1088 + n - 1)); mark=$n
            # hev config for this proxy → its own transparent port
            cfg="$RUN/hev_$n.yml"
            {
                echo "main:"; echo "  workers: 1"
                echo "socks5:"; echo "  type: $type"; echo "  port: $port"; echo "  address: '$host'"; echo "  udp: 'udp'"
                [ "$user" != "-" ] && [ -n "$user" ] && { echo "  username: '$user'"; echo "  password: '$pass'"; }
                echo "tcp:"; echo "  port: $hport"; echo "  address: '::'"
                echo "udp:"; echo "  port: $hport"; echo "  address: '::'"
            } > "$cfg"
            # LOAD-BEARING launch: copg_lic verifies the license, recovers K_GPU, and decrypt+exec's
            # hev in an anonymous memfd. Fails (no pid) with no valid license → routing never applies.
            if ! launch_hev "$cfg" "$RUN/hev_$n.pid"; then
                log "hev launch REFUSED for proxy #$n (no valid PRO license) — aborting, no routing"
                stop; return 0
            fi
            eval "GRP_$gi=\"$mark,$hport\""
            log "proxy #$n $host:$port -> hev port $hport (pid $(cat "$RUN/hev_$n.pid" 2>/dev/null))"
        fi
        for u in $uids; do eval "GRP_$gi=\"\$GRP_$gi,$u\""; done
        log "$pkg -> proxy #$gi uids:$uids"
    done < "$CONF"

    [ "$n" -gt 0 ] || { log "no valid proxied apps"; return 0; }
    sleep 1
    # collect the group strings + verify each hev came up
    args=""; j=1
    while [ "$j" -le "$n" ]; do
        eval "g=\$GRP_$j"; args="$args $g"
        kill -0 "$(cat "$RUN/hev_$j.pid" 2>/dev/null)" 2>/dev/null || { log "hev #$j died:"; tail -3 "$RUN/hev_$j.log" >> "$LOG" 2>/dev/null; }
        j=$((j+1))
    done
    # shellcheck disable=SC2086
    sh "$TPROXY" setup $args >> "$LOG" 2>&1 && log "routing up ($n proxy group(s)):$args" || { log "routing FAILED — tearing down (fail-closed)"; stop; }
}

status() {
    echo "hev instances:"; for p in "$RUN"/hev_*.pid; do [ -f "$p" ] && echo "  $(basename "$p" .pid) pid $(cat "$p") $(kill -0 "$(cat "$p")" 2>/dev/null && echo up || echo DEAD)"; done
    echo "COPG mangle chains: $(iptables -t mangle -S 2>/dev/null | grep -c COPG_PROXY)"
    echo "ip rules -> table 2024: $(ip rule 2>/dev/null | grep -c 2024)"
}

# watch active.conf for changes (WebUI rewrites it on save); reload on mtime change.
watch() {
    start
    last=$(stat -c %Y "$CONF" 2>/dev/null || echo 0)
    while :; do
        sleep 3
        cur=$(stat -c %Y "$CONF" 2>/dev/null || echo 0)
        [ "$cur" != "$last" ] && { last=$cur; log "active.conf changed — reloading"; start; }
    done
}

case "${1:-}" in
    start)  start ;;
    stop)   stop ;;
    reload) start ;;
    status) status ;;
    watch)  watch ;;
    *) echo "usage: $0 start|stop|reload|status|watch"; exit 2 ;;
esac
