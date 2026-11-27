#!/sbin/sh
MODDIR="${0%/*}"
source "$MODDIR/../functions.sh"
GAME_LIST="com.tencent.tmgp.sgame:王者荣耀|com.tencent.tmgp.pubgmhd:和平精英|com.tencent.ig:PUBGM|com.tencent.tmgp.cod:使命召唤|com.tencent.tmgp.speedmobile:QQ飞车|com.miHoYo.Yuanshen:原神|com.miHoYo.GenshinImpact:原神国际|com.hoyoverse.hyperion:崩坏3|com.netease.dwrg:第五人格|com.netease.onmyoji:阴阳师|com.activision.callofduty.shooter:CODM|com.garena.game.codm:CODM东南亚|com.dts.freefireth:FreeFire|com.supercell.clashofclans:部落冲突|com.supercell.clashroyale:皇室战争|com.kiloo.subwaysurf:地铁跑酷|com.tencent.tmgp.cf:穿越火线|com.tencent.tmgp.dnf:DNF手游|com.tencent.tmgp.qqyx:QQ游戏|com.tencent.tmgp.wzry:王者荣耀备用|com.tencent.letsgo:金铲铲|com.tencent.tmgp.hyxw:火影忍者|com.tencent.tmgp.pubgm:和平精英备用|com.tencent.tmgp.sgamece:王者荣耀体验|com.tencent.tmgp.ylm:英雄联盟手游|com.miHoYo.bh3:崩坏3备用|com.miHoYo.hkrpg:崩坏星穹铁道|com.miHoYo.HoYoverse:星穹铁道国际|com.zenlesszonezero:绝区零|com.miHoYo.Nap:绝区零备用|com.netease.game.xyq:梦幻西游|com.netease.mrzh:明日之后|com.netease.mc:我的世界网易|com.mojang.minecraftpe:我的世界|com.roblox.client:Roblox|com.ea.gp.fifamobile:FIFA|com.ea.game.nfs14_row:极品飞车|com.riotgames.league.teamfighttactics:云顶之弈|com.riotgames.league.wildrift:英雄联盟WR|com.blizzard.wtcg.hearthstone:炉石传说|com.bhvr.deadbydaylight:黎明杀机|com.kurogame.wutheringwaves:鸣潮|com.hypergryph.arknights:明日方舟|com.yostarjp.bluearchive:蔚蓝档案|com.nexon.bluearchive:蔚蓝档案国际|com.yostar.en.bluearchive:蔚蓝档案英"
get_foreground_app() {
    dumpsys activity activities 2>/dev/null | grep -i "mFocusedWindow\|mResumedActivity\|mCurrentFocus" | head -3
}
detect_window_mode() {
    local window_info=$(dumpsys window 2>/dev/null | grep -i "WindowState\|mFocusedApp")
    if echo "$window_info" | grep -qi "pip\|freeform\|split\|popup\|floating"; then
        echo "window"
    elif echo "$window_info" | grep -qi "fullscreen"; then
        echo "fullscreen"
    else
        echo "normal"
    fi
}
detect_game_running() {
    local running=$(dumpsys activity activities 2>/dev/null | grep -oE "([a-zA-Z0-9_]+\.)+[a-zA-Z0-9_]+" | sort -u)
    echo "$GAME_LIST" | tr '|' '\n' | while IFS=':' read pkg name; do
        if echo "$running" | grep -q "^$pkg$"; then
            echo "$pkg:$name"
            return 0
        fi
    done
    return 1
}
game_detector_main() {
    while true; do
        local game=$(detect_game_running)
        local window=$(detect_window_mode)
        local current_mode=$(ai_get_mode)
        if [ -n "$game" ]; then
            local pkg=$(echo "$game" | cut -d':' -f1)
            local gname=$(echo "$game" | cut -d':' -f2)
            if [ "$window" = "window" ]; then
                if [ "$current_mode" != "game_window" ]; then
                    ai_set_mode "game_window" "检测到游戏[$gname]处于小窗模式"
                    cpu_governor_performance
                    frame_lock_high
                    touch_optimize
                    network_optimize
                    ax_info "游戏小窗模式：保持性能，优化系统"
                fi
            else
                if [ "$current_mode" != "gaming" ]; then
                    ai_set_mode "gaming" "检测到游戏[$gname]全屏运行"
                    cpu_governor_performance
                    frame_lock_high
                    touch_optimize
                    network_optimize
                    audio_enhance
                    thermal_disable
                    aggressive_scheduler
                    freeze_background
                    ax_info "游戏全屏模式：全性能释放"
                fi
            fi
            ai_write_state "game_detected" "true"
            ai_write_state "window_state" "$window"
        else
            if [ "$current_mode" != "daily" ] && [ "$current_mode" != "game_window" ]; then
                ai_set_mode "daily" "游戏已退出，切换日常模式"
                frame_unlock
                cpu_governor_balanced
                battery_protect
                ax_info "日常模式：均衡性能"
            fi
            ai_write_state "game_detected" "false"
            ai_write_state "window_state" "normal"
        fi
        sleep 3
    done
}
game_detector_main &
ax_info "游戏窗口动态检测模块已启动 [PID: $!]"
