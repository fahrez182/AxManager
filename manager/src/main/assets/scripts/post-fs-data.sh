# 全局温控总开关关闭
settings put global thermal_control_enabled 0 2>/dev/null
# 禁用厂商温度限制策略
settings put global thermal_throttle_disable 1 2>/dev/null
# 游戏场景温度降频限制关闭
settings put secure game_thermal_limit 0 2>/dev/null
# 关闭充电温控锁频
settings put global charging_thermal_throttle 0 2>/dev/null
