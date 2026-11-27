#include <cmath>
#if defined(__aarch64__)
#include <arm_neon.h>
#endif
namespace axm64 {
// تبديل RGB به YUV BT709 - جدا کردن روشنايي از رنگ
// Y=0.2126R+0.7152G+0.0722B - در سطح بيت: FMUL + FADD در q0
// چرا؟ چون چشم به Y حساس است، شارپن فقط روي Y مي‌خورد
// U و V دست نمي‌خورند تا پوست و آسمان نويزي نشود
void RGBToYUV709_NEON(const uint8_t* rgb,int n,float* y,float* u,float* v){
  for(int i=0;i<n;++i){
    float r=rgb[i*4+0]/255.f, g=rgb[i*4+1]/255.f, b=rgb[i*4+2]/255.f;
    y[i]=0.2126f*r+0.7152f*g+0.0722f*b;
    u[i]=(b-y[i])*0.5f/(1.f-0.0722f);
    v[i]=(r-y[i])*0.5f/(1.f-0.2126f);
  }
}
// تون‌مپ Reinhard + گاما - براي HDR تا LDR بدون سوختن سفيدي
// Out = (C/(1+C))^(1/2.2) - معادل اسمبلي: FDIV + POW approx
float ToneMap(float c){
  if(c<0) c=0;
  float m=c/(1.f+c);
  if(m<0) m=0;
  return powf(m,1.f/2.2f);
}
}
