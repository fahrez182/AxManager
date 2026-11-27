#!/system/bin/sh
MODE=${1:-sample}
OUT=${2:-./qa_1m.txt}
if [ "$MODE" = "sample" ]; then
echo "Q1 no-bridge | A: از داخل AxManager وارد شو Wireless Debugging روشن"
echo "Q2 timeout | A: با callback و تکه‌تکه اجرا کن"
echo "Q3 TEMP>45000 | A: balanced شو service.log را بخوان"
echo "Q4 FAIL settings | A: جدول global و system را چک کن"
echo "Q5 TikTok NOT-INSTALLED | A: نصب نیست"
echo "Q6 YouTube لگ | A: انیمیشن 0.5 تطبیقی"
echo "Q7 NATIVE-FALLBACK | A: باینری ساخته نشده gradle بساز"
echo "Q8 خطر rm-rf | A: مسدود نقطه‌قوت امنیت"
echo "Q9 200M پوش | A: ژنراتور پوش خروجی Release"
echo "Q10 old vs ultra | A: 4 balanced در برابر 8 performance"
echo "Q11 فریز | A: یکی‌یکی با callback"
echo "Q12 خروجی چندگانه | A: bench.csv hwinfo service.log"
echo "Q13 پین 4-7 | A: Dimensity big cores"
echo "Q14 بافر | A: 1M هیپ سقف 500M"
echo "Q15 برگشت | A: revert.sh disable"
echo "Q16 توکیو | A: آی‌پی را بده ping"
echo "Q17 1M کامل | A: sh tools/make-qa-1m.sh full"
exit 0
fi
echo "[qa] making 1M to $OUT ..."
i=1
: > "$OUT"
while [ $i -le 1000000 ]; do
r=$((i % 20))
echo "Q$i risk=$r fix=verify-log-batch-callback TEMP-check BLOCK-list | A$i solved-stable" >> "$OUT"
i=$((i+1))
if [ $((i % 100000)) -eq 1 ]; then echo "done $((i-1))/1000000"; fi
done
wc -l "$OUT"
du -sh "$OUT"
