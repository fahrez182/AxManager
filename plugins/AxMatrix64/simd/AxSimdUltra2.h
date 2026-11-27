#pragma once
// ============================================================
// AxMatrix64 v2 - AxSimdUltra2.h
// arch: arm64-v8a - AArch64 - NEON - C++17 - فقط 64 بیت
// هدف: آپ‌اسکیل + شارپن فضایی - خروجی .so زیر 2 مگابایت
// فرمول bilinear:
//   Out = A*(1-fx)*(1-fy)+B*fx*(1-fy)+C*(1-fx)*fy+D*fx*fy
// فرمول شارپن:
//   Out = C + k*(C-Avg4) , k = amount*1.25
// فرمول ریسک گاورنر:
//   R = 0.5*Temp + 0.3*CPU + 0.2*CrashCount*15
// ============================================================
// پرسش 1: چرا فقط arm64-v8a؟ چون v0-v31 صدوبیست‌وهشت بیتی
//   و dotprod فقط روی AArch64 تضمینی است.
// پرسش 2: چرا float32x4؟ چون 4 کانال RGBA در یک FMLA.
// پرسش 3: چرا vrhadd برای 2x؟ چون صحیح است و واحد float خواب می‌ماند.
// پرسش 4: چرا int16 برای شارپن؟ چون تفاضل منفی می‌شود و uint8 سرریز می‌کند.
// پرسش 5: چرا prefetch؟ چون خط 64 بایتی است و خواندن سطری میس کم می‌کند.
// پرسش 6: چرا separable؟ چون 2x جداپذیر است و نصف دستور می‌برد.
// پرسش 7: چرا k حداکثر 1.25؟ بیشتر هاله و نویز می‌دهد.
// پرسش 8: ورودی چیست؟ RGBA8888 خطی بدون premult.
// پرسش 9: آلفا چه می‌شود؟ کپی بدون شارپن.
// پرسش 10: چرا حد 2 مگ؟ تا در حافظه حس نشود.
// پرسش 11: آیا عصبی است؟ نه، فضایی است. عصبی جدا با TFLite است.
// پرسش 12: آیا به بازی دیگران تزریق می‌شود؟ نه، فقط بازی خودت.
// پرسش 13: چرا O3 و LTO؟ اینلاین و حذف کد مرده.
// پرسش 14: حرارت بالا؟ گاورنر رزولوشن را کم می‌کند، ترمال خاموش نمی‌شود.
// پرسش 15: چرا scratch جدا؟ تا کش L1 داغ بماند.
// پرسش 16: UCVTF چیست؟ تبدیل uint به float در NEON.
// پرسش 17: FMLA چیست؟ a+b*c در یک راندینگ.
// پرسش 18: VQMOVUN چیست؟ saturate سخت‌افزاری به 0..255.
// پرسش 19: LD4 و ST4 چیست؟ جدا و بسته‌بندی R,G,B,A.
// پرسش 20: چرا آستانه لبه؟ تا آسمان شارپ نخورد و فقط لبه تیز شود.
// ============================================================
#include <cstdint>
#include <cstddef>
namespace axm64 {
struct SimdFrameInfo {
  int srcW = 0; int srcH = 0;
  int dstW = 0; int dstH = 0;
  float sharpAmount = 0.35f;
  bool usedFast2X = false;
  float upscaleRatio = 1.0f;
  float edgeBoost = 1.0f;
};
void UpscaleBilinearNEON(const uint8_t* src,int sw,int sh,uint8_t* dst,int dw,int dh);
void Upscale2XFastNEON(const uint8_t* src,int sw,int sh,uint8_t* dst,uint8_t* tmpRow);
void SharpenNEON(uint8_t* img,int w,int h,float amount);
void SharpenAdaptiveNEON(uint8_t* img,int w,int h,float amount,float edgeThresh);
SimdFrameInfo ProcessFrameAuto(const uint8_t* low,int lw,int lh,uint8_t* high,int hw,int hh,float sharp,uint8_t* scratch);
inline size_t FrameBytes(int lw,int lh,int hw,int hh) {
  return (size_t)lw*(size_t)lh*4 + (size_t)hw*(size_t)hh*4;
}
}
