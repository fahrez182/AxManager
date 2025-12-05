# ============================================
#  AXERON Plugin Manager
#  - Menjalankan plugin
#  - Menangani update/remove
#  - Menjalankan post-fs-data, system.prop, service
#  - Menautkan binary eksternal milik plugin
# ============================================

PLUGINS_DIR="$AXERONDIR/plugins"
PLUGINS_UPDATE_DIR="$AXERONDIR/plugins_update"
DEBUG="${1:-false}"

# --------------------------------------------
# executor()
# Menjalankan command sebagai service (background)
# - Menggunakan busybox + setsid
# - DEBUG=true → log output ke "log -t <tag>"
# - DEBUG=false → silent
# --------------------------------------------
executor() {
    local tag="$1"
    shift

    if [ "$DEBUG" = "true" ]; then
        exec busybox setsid "$@" 2>&1 | log -t "$tag"
    else
        exec busybox setsid "$@" >/dev/null 2>&1
    fi
}

# --------------------------------------------
# starter()
# Menjalankan command yang butuh Sync (blocking)
# Tidak memakai setsid, digunakan untuk:
# - post-fs-data
# - system.prop reset
# --------------------------------------------
starter() {
    local tag="$1"
    shift

    if [ "$DEBUG" = "true" ]; then
        "$@" 2>&1 | log -t "$tag"
    else
        "$@" >/dev/null 2>&1
    fi
}

# --------------------------------------------
# add_external_bin()
# Menautkan folder system/bin milik plugin → AXERONXBIN
# --------------------------------------------
add_external_bin() {
    local bin="$1"

    [ ! -d "$AXERONXBIN" ] && mkdir -p "$AXERONXBIN"

    [ -d "$bin" ] && {
        echo "- Linking external binaries from $bin"

        for binary in "$bin"/*; do
            xpath="${AXERONXBIN}/$(basename "$binary")"

            [ -f "$binary" ] && {
                busybox ln -sf "$binary" "$xpath"
                echo "[+] Linked: $xpath"
            }
        done
    }
}

# --------------------------------------------
# remove_external_bin()
# Membersihkan bin plugin yang sudah pernah dilink
# --------------------------------------------
remove_external_bin() {
    local bin="$1"

    [ ! -d "$AXERONXBIN" ] && mkdir -p "$AXERONXBIN"

    [ -d "$bin" ] && {
        echo "- Removing external binaries from $bin"

        for binary in "$bin"/*; do
            xpath="${AXERONXBIN}/$(basename "$binary")"

            if [ "$binary" = "$(busybox readlink "$xpath")" ]; then
                rm -f "$xpath"
                echo "[-] Removed: $xpath"
            fi
        done
    }
}

# --------------------------------------------
# uninstall()
# Menjalankan uninstall.sh milik plugin
# --------------------------------------------
uninstall() {
    local name="$1"
    local script="$2"
    local bin="$3"

    if grep -q '^ASH_STANDALONE=1$' "$script" 2>/dev/null; then
        executor "$name:uninstall" busybox sh -o standalone "$script" &
    else
        executor "$name:uninstall" busybox sh "$script" &
    fi

    remove_external_bin "$bin"
    rm -rf "$PLUGINS_DIR/$name"
}

# --------------------------------------------
# service()
# Menjalankan service.sh → background
# PID disimpan via setprop
# --------------------------------------------
service() {
    local name="$1"
    local script="$2"

    local dir
    dir=$(dirname "$script")

    # Path wrapper
    local wrapper="${dir}/._service_${name}.sh"

    # Generate wrapper (overwrite every time)
    cat > "$wrapper" <<EOF
#!/system/bin/sh
script="$SERVICE"
content=\$(busybox cat "\$script")
if grep -q '^ASH_STANDALONE=1$' "$script" 2>/dev/null; then
    exec busybox sh -o standalone -c "\$content"
else
    exec busybox sh -c "\$content"
fi
EOF

    chmod 755 "$wrapper"

    # Jalankan wrapper sebagai service
    if [ "$DEBUG" = "true" ]; then
        executor "$name" busybox sh "$wrapper" &
    else
        executor "$name" busybox sh "$wrapper" >/dev/null 2>&1 &
    fi

    # PID wrapper (ini yang akan di-track)
    local wrapper_pid=$!
    setprop "log.tag.service.$name" "$wrapper_pid"
}


# --------------------------------------------
# system_prop()
# Eksekusi system.prop file → via resetprop
# --------------------------------------------
system_prop() {
    starter "$1:props" resetprop -f "$2"
}

# --------------------------------------------
# post_fs_data()
# Script yang dijalankan saat post-fs-data
# Sync, tidak bisa background
# --------------------------------------------
post_fs_data() {
    local name="$1"
    local script="$2"

    if grep -q '^ASH_STANDALONE=1$' "$script" 2>/dev/null; then
        starter "$name:fsdata" busybox sh -o standalone "$script"
    else
        starter "$name:fsdata" busybox sh "$script"
    fi
}

# --------------------------------------------
# start_plugin()
# Menjalankan lifecycle plugin:
# 1. Link bin
# 2. post-fs-data (jika belum run)
# 3. system.prop (jika belum run)
# 4. service (background)
# --------------------------------------------
start_plugin() {
    (
        local name="$1"
        local fsdata="$2"
        local sprop="$3"
        local servicef="$4"
        local bin="$5"

        add_external_bin "$bin"

        # post-fs-data (once)
        if [ "$(getprop "log.tag.fs.$name")" != "1" ]; then
            [ -f "$fsdata" ] && post_fs_data "$name" "$fsdata" && setprop "log.tag.fs.$name" 1
        fi

        # system.prop (once)
        if [ "$(getprop "log.tag.props.$name")" != "1" ]; then
            [ -f "$sprop" ] && system_prop "$name" "$sprop" && setprop "log.tag.props.$name" 1
        fi

        # service selalu dijalankan (apache style)
        [ -f "$servicef" ] && service "$name" "$servicef"
    ) &
}

# --------------------------------------------
# stop_plugin()
# Mematikan service plugin, membersihkan prop, unlink bin
# --------------------------------------------
stop_plugin() {
    local name="$1"
    local servicef="$2"
    local bin="$3"

    setprop "log.tag.fs.$name" 0
    setprop "log.tag.props.$name" 0

    # Kill wrapper PID
    local wrapper_pid
    wrapper_pid=$(getprop "log.tag.service.$name")
    if [ -n "$wrapper_pid" ] && [ -d "/proc/$wrapper_pid" ]; then
        kill -TERM "$wrapper_pid"
    fi

    pkill -f "$name" 2>/dev/null

    # Reset props
    setprop "log.tag.service.$name" "-1"

    [ -n "$bin" ] && remove_external_bin "$bin"
}


# ============================================
# Bersihkan folder update
# ============================================
for plugin_update in "$PLUGINS_UPDATE_DIR"/*; do
    [ -d "$plugin_update" ] && rm -rf "$plugin_update"
done

# ============================================
# Loop utama untuk setiap plugin
# ============================================
for plugin in "$PLUGINS_DIR"/*; do
    [ -d "$plugin" ] || continue

    NAME=$(basename "$plugin")
    BIN="$plugin/system/bin"
    FSDATA="$plugin/post-fs-data.sh"
    SPROP="$plugin/system.prop"
    SERVICE="$plugin/service.sh"
    UNINSTALL="$plugin/uninstall.sh"
    UPDATE="$plugin/update"
    DISABLE="$plugin/disable"
    REMOVE="$plugin/remove"

    # --- uninstall ---
    if [ -f "$REMOVE" ]; then
        echo "- Disable and uninstalling $NAME"
        stop_plugin "$NAME" "$SERVICE"
        uninstall "$NAME" "$UNINSTALL" "$BIN"
        continue
    fi

    # --- disable only ---
    if [ -f "$DISABLE" ]; then
        echo "- Disable $NAME"
        stop_plugin "$NAME" "$SERVICE" "$BIN"
        continue
    fi

    # --- handle update ---
    if [ -f "$UPDATE" ]; then
        echo "- Updating $NAME"
        stop_plugin "$NAME" "$SERVICE"
        rm -rf "$UPDATE"
    else
        # Cek apakah service masih running
        pid=$(getprop "log.tag.service.$NAME")
        pid=${pid:-"-1"}

        if [ "$pid" != "-1" ] || [ -d "/proc/$pid" ] || pgrep -f "$NAME" >/dev/null 2>&1; then
            echo "- $NAME:$pid is already running, skip."
            continue
        fi
    fi

    # --- Start plugin ---
    echo "- Starting $NAME"
    start_plugin "$NAME" "$FSDATA" "$SPROP" "$SERVICE" "$BIN"
done

echo "- Completed"
exit 0
