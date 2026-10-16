# Shared by every Test Function script. Set MODDIR, then: . "$MODDIR/common/lib.sh"
# Each stage writes what it saw to $MODDIR/reports/<stage>.txt as well as to its output.

PLUGIN_ID=${MODDIR##*/}
REPORT_DIR="$MODDIR/reports"
RUN_DIR="$MODDIR/run"
REPORT=

report_begin() { # $1 = stage name
  mkdir -p "$REPORT_DIR" "$RUN_DIR"
  REPORT="$REPORT_DIR/$1.txt"
  : > "$REPORT"
  say "== $1 · $(date '+%Y-%m-%d %H:%M:%S') =="
}

say() {
  echo "$*"
  [ -n "$REPORT" ] && echo "$*" >> "$REPORT"
}

section() { say ""; say "-- $* --"; }

kv() { say "$(printf '%-18s %s' "$1" "${2:-<unset>}")"; }

# check "label" command [args...]: PASS when the command succeeds.
check() {
  label=$1
  shift
  if "$@" >/dev/null 2>&1; then say "[PASS] $label"; else say "[FAIL] $label"; fi
}

# info "label" value: a result that is worth seeing but has no right answer.
info() { say "[INFO] $1: ${2:-<empty>}"; }

dump_env() {
  section "environment"
  for v in AXERON AXERONVER AXERONDIR AXERONBIN AXERONXBIN AXERONLIB TMPDIR HOSTNAME AXERON_PLUGIN; do
    eval "kv $v \"\$$v\""
  done
  kv PATH "$PATH"
  kv LD_LIBRARY_PATH "$LD_LIBRARY_PATH"
  kv uid "$(id -u) ($(id -un 2>/dev/null))"
  kv selinux "$(tr -d '\0' < /proc/self/attr/current 2>/dev/null)"
  kv shell "$(readlink /proc/$$/exe)"
  kv stdin "$(readlink /proc/$$/fd/0)"
  kv stdout "$(readlink /proc/$$/fd/1)"
}

# Whether process $1 carries this plugin's marker in its environment. Other uids' environ is
# unreadable; 2>/dev/null must come before the < redirect, or the open error still prints.
has_marker() {
  [ -r "/proc/$1/environ" ] || return 1
  tr '\0' '\n' 2>/dev/null < "/proc/$1/environ" | grep -qx "AXERON_PLUGIN=$PLUGIN_ID"
}

# A process's command line on one line (sh -c scripts contain newlines). One escape per tr:
# Android's tr misreads '\0\n' and replaces every letter n instead of the newline.
cmdline_of() { tr '\0' ' ' 2>/dev/null < "/proc/$1/cmdline" | tr '\n' ' '; }

# Every live process that carries this plugin's marker, as "pid cmdline".
marked_processes() {
  for d in /proc/[0-9]*; do
    p=${d#/proc/}
    [ "$p" = "$$" ] && continue
    has_marker "$p" || continue
    echo "$p $(cmdline_of "$p")"
  done
}

# pid_state <name>: alive/dead for the pid stored in run/<name>.pid.
pid_state() {
  f="$RUN_DIR/$1.pid"
  [ -f "$f" ] || { echo "no pid file"; return; }
  p=$(cat "$f")
  if [ -d "/proc/$p" ]; then
    if has_marker "$p"; then echo "pid $p alive, marked"; else echo "pid $p alive, no marker"; fi
  else
    echo "pid $p gone"
  fi
}
