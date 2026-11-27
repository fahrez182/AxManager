#!/system/bin/sh
ROOT=$(pwd)
echo "[omni60] root=$ROOT"
mkdir -p modules/axelden/system/bin modules/axelden/webroot manager/src/main/cpp manager/src/main/java/com/axdubai/manager/core tools
mv -f *Service*.sh modules/axelden/service.sh 2>/dev/null || true
mv -f *WebUI*.html modules/axelden/webroot/index.html 2>/dev/null || true
mv -f *Engine*.cpp manager/src/main/cpp/axelden_engine.cpp 2>/dev/null || true
chmod +x modules/axelden/service.sh modules/axelden/system/bin/* tools/*.sh 2>/dev/null || true
ls -R modules/axelden 2>/dev/null || true
cat modules/axelden/module.prop 2>/dev/null || echo "missing module.prop"
