#!/system/bin/sh
MODPATH=${0%/*}

echo "rtw88: Loading modules..." > /dev/kmsg

if lsmod | grep -q mac80211; then
    rmmod mac80211 2>/dev/null
fi

load() {
    if [ -f "$MODPATH/lkm/$1.ko" ]; then
        if AXMANAGERd insmod "$MODPATH/lkm/$1.ko" 2>/dev/null; then
            echo "rtw88: loaded $1" > /dev/kmsg
        else
            echo "rtw88: FAILED to load $1" > /dev/kmsg
        fi
    fi
}

load mac80211

load rtw88_core
load rtw88_usb

load rtw88_88xxa
load rtw88_8723x

for mod in rtw88_8812a rtw88_8812au rtw88_8814a rtw88_8814au \
           rtw88_8821a rtw88_8821au rtw88_8821c rtw88_8821cu \
           rtw88_8822b rtw88_8822bu rtw88_8723d rtw88_8723du; do
    load "$mod"
done

load r8188eu

echo "rtw88 modules loaded" > /dev/kmsg
lsmod | grep -E "rtw|mac80211|cfg80211" > /dev/kmsg 2>&1
