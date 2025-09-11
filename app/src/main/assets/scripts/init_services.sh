PLUGINS_DIR="/data/local/tmp/AxManager/plugins"
PLUGINS_UPDATE_DIR="/data/local/tmp/AxManager/plugins_update"
DEBUG="${1:-false}"

executor() {
    local tag="$1"
    shift
    if [ "$DEBUG" = "true" ]; then
        exec setsid "$@" 2>&1 | log -t "$tag"
    else
        exec setsid "$@" >/dev/null 2>&1
    fi
}

starter() {
    local tag="$1"
    shift
    if [ "$DEBUG" = "true" ]; then
        "$@" 2>&1 | log -t "$tag"
    else
        "$@" >/dev/null 2>&1
    fi
}

uninstall() {
    [ -d "$3" ] && [ -n "$(ls -A "$3" 2>/dev/null)" ] && export PATH=$PATH:"$3"
    if grep -q '^ASH_STANDALONE=1$' "$2" 2>/dev/null; then
        executor "$1:uninstall" busybox sh -o standalone "$2" &
    else
        executor "$1:uninstall" busybox sh "$2" &
    fi
    rm -rf "$PLUGINS_DIR/$1"
}

service() {
    [ -d "$3" ] && [ -n "$(ls -A "$3" 2>/dev/null)" ] && export PATH=$PATH:"$3"
    if grep -q '^ASH_STANDALONE=1$' "$2" 2>/dev/null; then
        executor "$1" busybox sh -o standalone "$2" &
    else
        executor "$1" busybox sh "$2" &
    fi
    local pid=$!
    setprop "log.tag.service.$1" "$pid"
}

#sync_with_service
system_prop() {
    # only prop with debug prefix
    grep ^debug "$2" 2>/dev/null | while IFS='=' read -r key val; do
        [ -n "$key" ] && starter "$1:props" setprop "$key" "$val"
    done
}


#Sync function, wait until execute finish
post_fs_data() {
    [ -d "$3" ] && [ -n "$(ls -A "$3" 2>/dev/null)" ] && export PATH=$PATH:"$3"
    if grep -q '^ASH_STANDALONE=1$' "$2" 2>/dev/null; then
        starter "$1:fsdata" busybox sh -o standalone "$2"
    else
        starter "$1:fsdata" busybox sh "$2"
    fi
}

start_plugin() {
    (
        local name="$1" fsdata="$2" sprop="$3" servicef="$4" bin="$5"
        if [ "$(getprop "log.tag.fs.$name" 2>/dev/null)" != "1" ]; then
            [ -f "$fsdata" ] && post_fs_data "$name" "$fsdata" "$bin" && setprop "log.tag.fs.$name" 1
        fi
        if [ "$(getprop "log.tag.props.$name" 2>/dev/null)" != "1" ]; then
            [ -f "$sprop" ] && system_prop "$name" "$sprop" && setprop "log.tag.props.$name" 1
        fi
        service "$name" "$servicef" "$bin"
    ) &
}

stop_plugin() {
    local name="$1" servicef="$2"
    setprop "log.tag.fs.$name" 0
    setprop "log.tag.props.$name" 0
    pid=$(getprop "log.tag.service.$name")
    if [ -n "$pid" ] && [ -d "/proc/$pid" ]; then
        kill -TERM -"$pid"
    fi
    if pgrep -f "$name" >/dev/null 2>&1; then
        pkill -f "$name"
    fi
}

for plugin_update in "$PLUGINS_UPDATE_DIR"/*; do
  [ -d "$plugin_update" ] || continue
  rm -rf "$plugin_update"
done

for plugin in "$PLUGINS_DIR"/*; do
    [ -d "$plugin" ] || continue

    NAME=$(basename "$plugin")
    BIN="$plugin/system/bin"
    FSDATA="$plugin/post-fs-data.sh"
    SPROP="$plugin/system.prop"
    SERVICE="$plugin/service.sh"
    UNINSTALL="$plugin/uninstall.sh"
    DISABLE="$plugin/disable"
    REMOVE="$plugin/remove"

    if [ -f "$REMOVE" ]; then
        echo "- Disable and uninstalling $NAME"
        stop_plugin "$NAME" "$SERVICE"
        uninstall "$NAME" "$UNINSTALL" "$BIN"
        continue
    fi

    if [ -f "$DISABLE" ]; then
        echo "- Disable $NAME"
        stop_plugin "$NAME" "$SERVICE"
        continue
    fi

    [ ! -f "$SERVICE" ] && {
        echo "- Service not available for $NAME, skip."
        continue
    }

    pid=$(getprop "log.tag.service.$NAME")
    # shellcheck disable=SC2235
    if ( [ -n "$pid" ] && [ -d "/proc/$pid" ] ) || pgrep -f "$NAME" >/dev/null 2>&1; then
        echo "- $NAME:$pid is already running, skip."
        continue
    fi

    echo "- Starting $NAME"
    start_plugin "$NAME" "$FSDATA" "$SPROP" "$SERVICE" "$BIN"
done

echo "- Completed"
exit 0