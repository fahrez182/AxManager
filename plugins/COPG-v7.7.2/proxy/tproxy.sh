#!/system/bin/sh
# COPG per-app transparent proxy — kernel TPROXY routing (MULTI-PROXY).
#
# Routes ONLY tagged apps' TCP/UDP through local hev-socks5-tproxy helpers, each relaying to a
# REMOTE SOCKS5. No tun, no interface for an app to enumerate (the stealth win over VpnService).
# Different apps can use DIFFERENT proxies: each proxy = one hev on its own port + its own fwmark;
# OUTPUT marks each app's uid with its proxy's mark, and PREROUTING keys on that (mark-surviving-
# the-loop is verified on-device) to TPROXY it to the matching hev port.
#
# Security-sensitive: a wrong rule breaks device networking or leaks the real IP. Ported from
# v2rayNG RootTproxyManager + its on-device debugging notes; verified on KernelSU/A16.
#
# Usage:  tproxy.sh probe
#         tproxy.sh setup  <mark,port,uid[,uid...]>  [group...]     (teardown-first, then install)
#         tproxy.sh teardown
#
# A "group" is one proxy: MARK,PORT,UID1,UID2,...  e.g.  1,1088,10249,10250   2,1089,10300
# The hev helpers must already be listening on each PORT (the daemon starts them). Marks must be
# small (1,2,3,... — Android encodes netId in the HIGH fwmark bits, and the ip rule below MUST sit
# at priority < 10000, before netd's own rules, or netd routes the packet out before we capture it).

set -u

IPT=iptables; IP6=ip6tables; IP=ip
command -v iptables >/dev/null 2>&1 || IPT=/system/bin/iptables
command -v ip6tables >/dev/null 2>&1 || IP6=/system/bin/ip6tables
command -v ip >/dev/null 2>&1 || IP=/system/bin/ip

TABLE=2024                # dedicated route table (local default dev lo) — shared by all marks
PRIORITY=1000             # ip rule priority — MUST be < 10000 (before Android netd's rules)
OUT=COPG_PROXY_OUT
PRE=COPG_PROXY_PRE
V6REJ=COPG_PROXY_V6

CIDR4="0.0.0.0/8 10.0.0.0/8 100.64.0.0/10 127.0.0.0/8 169.254.0.0/16 172.16.0.0/12 192.168.0.0/16 224.0.0.0/4 240.0.0.0/4 255.255.255.255/32"
CIDR6="::1/128 fc00::/7 fe80::/10 ff00::/8"

# ───────────────────────── capability probe ─────────────────────────
probe() {
    C=COPG_PROBE
    for cmd in "$IPT" "$IP6"; do
        fam=4; [ "$cmd" = "$IP6" ] && fam=6
        $cmd -t mangle -N $C 2>/dev/null; $cmd -t mangle -F $C 2>/dev/null
        $cmd -t mangle -A $C -p tcp -j TPROXY --on-port 1088 --tproxy-mark 1 2>/dev/null && echo "CAP_TPROXY$fam=1" || echo "CAP_TPROXY$fam=0"
        $cmd -t mangle -F $C 2>/dev/null
        $cmd -t mangle -A $C -m conntrack --ctdir REPLY -j RETURN 2>/dev/null && echo "CAP_CTDIR$fam=1" || echo "CAP_CTDIR$fam=0"
        $cmd -t mangle -F $C 2>/dev/null
        $cmd -t mangle -A $C -m addrtype --dst-type LOCAL -j RETURN 2>/dev/null && echo "CAP_ADDRTYPE$fam=1" || echo "CAP_ADDRTYPE$fam=0"
        $cmd -t mangle -F $C 2>/dev/null; $cmd -t mangle -X $C 2>/dev/null
    done
}

guard_src() {  # OUTPUT: exempt hev's spoofed non-local-source replies
    if [ "$1" = 4 ]; then [ "$CTDIR4" = 1 ] && { echo "-m conntrack --ctdir REPLY"; return; }; [ "$ADDR4" = 1 ] && { echo "-m addrtype ! --src-type LOCAL"; return; }
    else [ "$CTDIR6" = 1 ] && { echo "-m conntrack --ctdir REPLY"; return; }; [ "$ADDR6" = 1 ] && { echo "-m addrtype ! --src-type LOCAL"; return; }; fi
    echo ""
}
guard_dst() {  # PREROUTING: let proxied replies (addressed to us via lo) through
    if [ "$1" = 4 ]; then [ "$CTDIR4" = 1 ] && { echo "-m conntrack --ctdir REPLY"; return; }; [ "$ADDR4" = 1 ] && { echo "-m addrtype --dst-type LOCAL"; return; }
    else [ "$CTDIR6" = 1 ] && { echo "-m conntrack --ctdir REPLY"; return; }; [ "$ADDR6" = 1 ] && { echo "-m addrtype --dst-type LOCAL"; return; }; fi
    echo ""
}

# ───────────────────────── teardown (idempotent) ─────────────────────────
teardown() {
    # drop every ip rule pointing at our table (any mark)
    while $IP rule del table $TABLE 2>/dev/null; do :; done
    while $IP -6 rule del table $TABLE 2>/dev/null; do :; done
    $IP route flush table $TABLE 2>/dev/null
    $IP -6 route flush table $TABLE 2>/dev/null
    for pair in "$IPT" "$IP6"; do
        $pair -t mangle -D OUTPUT -j $OUT 2>/dev/null
        $pair -t mangle -D PREROUTING -j $PRE 2>/dev/null
        $pair -t mangle -F $OUT 2>/dev/null; $pair -t mangle -X $OUT 2>/dev/null
        $pair -t mangle -F $PRE 2>/dev/null; $pair -t mangle -X $PRE 2>/dev/null
    done
    $IP6 -D OUTPUT -j $V6REJ 2>/dev/null
    $IP6 -F $V6REJ 2>/dev/null; $IP6 -X $V6REJ 2>/dev/null
}

# GROUPS is the arg list "mark,port,uid,uid,..." — helpers iterate it.
# field N (1=mark 2=port 3+=uids) of a group string
gf() { echo "$1" | cut -d, -f"$2"; }

# ───────────────────────── OUTPUT marking ─────────────────────────
build_output() {
    cmd=$1; g=$2; shift 2   # remaining = groups
    $cmd -t mangle -N $OUT 2>/dev/null; $cmd -t mangle -F $OUT
    [ -n "$g" ] && $cmd -t mangle -A $OUT $g -j RETURN          # anti-loop: hev's spoofed replies
    $cmd -t mangle -A $OUT -o lo -j RETURN
    # DNS DIRECT (app resolves locally via netd; only the CONNECT is proxied → works with a TCP-only
    # upstream SOCKS5). Must precede the owner MARKs. (Metadata leak, not an IP leak.)
    $cmd -t mangle -A $OUT -p udp --dport 53 -j RETURN
    $cmd -t mangle -A $OUT -p tcp --dport 53 -j RETURN
    if [ "$cmd" = "$IP6" ]; then for c in $CIDR6; do $cmd -t mangle -A $OUT -d $c -j RETURN; done
    else for c in $CIDR4; do $cmd -t mangle -A $OUT -d $c -j RETURN; done; fi
    # mark each app's uid with ITS proxy's mark (fail-closed: no uid → nothing marked, never catch-all)
    for grp in "$@"; do
        m=$(gf "$grp" 1); i=3
        while :; do u=$(gf "$grp" $i); [ -n "$u" ] || break; $cmd -t mangle -A $OUT -m owner --uid-owner "$u" -j MARK --set-xmark "$m"; i=$((i+1)); done
    done
    $cmd -t mangle -D OUTPUT -j $OUT 2>/dev/null || true
    $cmd -t mangle -A OUTPUT -j $OUT
}

# ───────────────────────── PREROUTING TPROXY hand-off ─────────────────────────
build_prerouting() {
    cmd=$1; g=$2; shift 2   # remaining = groups
    if [ "$cmd" = "$IP6" ]; then loop=::1/128; addr=$ADDR6; ipc="$IP -6"; else loop=127.0.0.0/8; addr=$ADDR4; ipc="$IP -4"; fi
    $cmd -t mangle -N $PRE 2>/dev/null; $cmd -t mangle -F $PRE
    $cmd -t mangle -A $PRE -d $loop -j RETURN                   # MUST be first (genuine lo traffic)
    [ -n "$g" ] && $cmd -t mangle -A $PRE $g -j RETURN          # let proxied replies through
    if [ "$addr" = 1 ]; then $cmd -t mangle -A $PRE -m addrtype --dst-type LOCAL -j RETURN
    else for A in $($ipc -o addr show 2>/dev/null | awk '{print $4}' | cut -d/ -f1); do $cmd -t mangle -A $PRE -d "$A" -j RETURN 2>/dev/null; done
         [ "$cmd" = "$IPT" ] && $cmd -t mangle -A $PRE -d 255.255.255.255 -j RETURN; fi
    if [ "$cmd" = "$IP6" ]; then for c in $CIDR6; do $cmd -t mangle -A $PRE -d $c -j RETURN; done
    else for c in $CIDR4; do $cmd -t mangle -A $PRE -d $c -j RETURN; done; fi
    # per proxy: looped-back (-i lo) marked traffic → that proxy's hev port. Keyed on the fwmark
    # (verified to survive the local-route loop) so each app reaches its own proxy.
    for grp in "$@"; do
        m=$(gf "$grp" 1); p=$(gf "$grp" 2)
        $cmd -t mangle -A $PRE -i lo -m mark --mark "$m" -p tcp -j TPROXY --on-port "$p" --tproxy-mark "$m"
        $cmd -t mangle -A $PRE -i lo -m mark --mark "$m" -p udp -j TPROXY --on-port "$p" --tproxy-mark "$m"
    done
    $cmd -t mangle -D PREROUTING -j $PRE 2>/dev/null || true
    $cmd -t mangle -A PREROUTING -j $PRE
}

# ───────────────────────── setup ─────────────────────────
setup() {
    [ $# -ge 1 ] || { echo "setup: no proxy groups"; return 1; }
    eval "$(probe)"
    CTDIR4=$CAP_CTDIR4; CTDIR6=$CAP_CTDIR6; ADDR4=$CAP_ADDRTYPE4; ADDR6=$CAP_ADDRTYPE6
    [ "$CAP_TPROXY4" = 1 ] || { echo "kernel lacks TPROXY (v4) — cannot proxy"; return 1; }
    gs4=$(guard_src 4); gd4=$(guard_dst 4)
    [ -n "$gs4" ] && [ -n "$gd4" ] || { echo "no safe reply guard (need conntrack --ctdir OR xt_addrtype) — refusing"; return 1; }

    teardown

    echo 1 > /proc/sys/net/ipv4/conf/all/route_localnet 2>/dev/null || true
    echo 1 > /proc/sys/net/ipv4/conf/lo/route_localnet 2>/dev/null || true
    $IP route replace local default dev lo table $TABLE
    # FAIL-CLOSED backstop: if the local-loop route above is ever purged (e.g. a netd/connectivity
    # event), a marked packet would otherwise fall through fwmark→table→main and egress DIRECT (real
    # IP leak). A higher-metric blackhole makes that fall-through a DROP instead — a tagged app stays
    # locked to its proxy and never goes direct, even when the proxy/daemon is down. The local route
    # (metric 0) always wins while present, so normal proxying is unaffected. Mirror on v6 so tagged
    # v6 (also REJECTed below) can never leak via a stray table lookup. Swept by teardown's flush.
    $IP    route replace blackhole default metric 1024 table $TABLE 2>/dev/null || true
    $IP -6 route replace blackhole default metric 1024 table $TABLE 2>/dev/null || true

    # one ip rule per distinct mark → our local-loop table (priority 1000, before netd).
    for grp in "$@"; do m=$(gf "$grp" 1); $IP rule add fwmark "$m" table $TABLE priority $PRIORITY 2>/dev/null; done

    build_output "$IPT" "$gs4" "$@"
    build_prerouting "$IPT" "$gd4" "$@"

    # v6: upstream proxies are IPv4 → REJECT tagged uids' native v6 (fall back to v4-through-proxy).
    $IP6 -N $V6REJ 2>/dev/null; $IP6 -F $V6REJ
    for c in $CIDR6; do $IP6 -A $V6REJ -d $c -j RETURN; done
    for grp in "$@"; do i=3; while :; do u=$(gf "$grp" $i); [ -n "$u" ] || break; $IP6 -A $V6REJ -m owner --uid-owner "$u" -j REJECT --reject-with icmp6-adm-prohibited; i=$((i+1)); done; done
    $IP6 -D OUTPUT -j $V6REJ 2>/dev/null || true
    $IP6 -A OUTPUT -j $V6REJ

    echo "COPG proxy routing up: $# proxy group(s): $*"
}

case "${1:-}" in
    probe)    probe ;;
    setup)    shift; setup "$@" ;;
    teardown) teardown; echo "COPG proxy routing down" ;;
    *) echo "usage: $0 probe | setup <mark,port,uid[,uid...]>... | teardown"; exit 2 ;;
esac
