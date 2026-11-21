#!/system/bin/sh
MODDIR=${0%/*}
until [ "$(getprop sys.boot_completed)" = "1" ]; do
  sleep 2
done
sleep 5
chmod 0644 "$MODDIR/COPG.json" 2>/dev/null
for g in copg_fs_bootguard copg_vpn_bootguard copg_lang_bootguard copg_wallpaper_bootguard copg_account_bootguard copg_ssaid_bootguard copg_playsrc_bootguard copg_tensorfeat_bootguard copg_corepatch_bootguard copg_hidedev_bootguard; do
  echo 0 > "/data/system/$g" 2>/dev/null
done
rm -f "$MODDIR/.lic_cache" 2>/dev/null
if [ -x "$MODDIR/proxy/copg_proxy.sh" ]; then
  nohup sh "$MODDIR/proxy/copg_proxy.sh" watch >/dev/null 2>&1 &
fi
if [ -x "$MODDIR/copg_lic" ]; then
  ( while true; do "$MODDIR/copg_lic" tick >/dev/null 2>&1; sleep 21600; done ) &
fi
if [ -x "$MODDIR/controller" ]; then
  exec "$MODDIR/controller" >/dev/null 2>&1 &
fi
