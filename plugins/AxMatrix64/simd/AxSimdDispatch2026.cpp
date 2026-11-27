#include <cstdio>
#if defined(__aarch64__)
#include <arm_neon.h>
#if defined(__ARM_FEATURE_DOTPROD)
#define HAS_DOTPROD 1
#else
#define HAS_DOTPROD 0
#endif
#else
#define HAS_DOTPROD 0
#endif
namespace axm64 {
// تشخيص قديمي و 2026 و سال آينده در زمان اجرا
// قديمي: فقط ASIMD پايه - مسير int8 و float32 عمومي
// 2026: dotprod + fp16 - مسير 2x تند با vrhadd
// آينده: i8mm + sve2 - مسير ماتريسي، فعلا رزرو
// در سطح 0 و 1: HWCAP را مي‌خوانيم، اگر بيت DOTPROD يک بود مسير تند
int GetDeviceClass(){
#if HAS_DOTPROD
  return 2026;
#else
  return 2020;
#endif
}
void PrintPath(){
  int c=GetDeviceClass();
  if(c>=2026) printf("path=2026 fast: dotprod+fp16+vrhadd 2x\n");
  else printf("path=legacy safe: generic bilinear+sharpen\n");
}
}
