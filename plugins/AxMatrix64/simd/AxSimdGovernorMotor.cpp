#include <cstdio>
namespace axm64 {
// موتور 2000 - يعني 2000 تصميم در ثانيه = هر 0.5ms يک راي
// ورودي: دما، درصد CPU، افت فريم، کرش
// خروجي: 0 سبز آزاد، 1 زرد محدود، 2 قرمز توقف و رول‌بک
// فرمول: R = 0.5*(Temp-36)*8 + 0.3*CPU + 15*Crash
// مثال: Temp=41 CPU=70 Crash=0 => R=20+21=41 سبز متمايل به زرد
// هيچ دسترسي به خاموش کردن thermal-engine ندارد، فقط پيشنهاد پروفايل
int Decide(float temp,float cpu,int crash,float fpsDrop){
  float r=0;
  if(temp>36) r+=(temp-36)*8.f*0.5f;
  r+=cpu*0.3f;
  r+=crash*15.f;
  r+=fpsDrop*2.f;
  if(r>=80) return 2;
  if(r>=50) return 1;
  return 0;
}
void ApplyDecision(int d){
  if(d==2) printf("action=cool+rollback to stable\n");
  else if(d==1) printf("action=throttle canary to 10%% cpu\n");
  else printf("action=allow full\n");
}
}
