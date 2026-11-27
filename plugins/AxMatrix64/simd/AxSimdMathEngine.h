#pragma once
// AxMatrix64 MathEngine - مغز ریاضی - arm64-v8a
// مبنا: 0 و 1 - uint8 = 00000000 تا 11111111
// float32 = 1 بيت علامت + 8 بيت نما + 23 بيت مانتیس
// مثال: 0.5 = 0 01111110 00000000000000000000000
// NEON: v0-v31 هر کدام 128 بيت = 4xfloat32 در يک سيکل
// فرمول 1 Bilinear: Out=A+(B-A)*fx+(C-A+(D-C-(B-A))*fx)*fy
// فرمول 2 CAS Sharpen: Out=C+k*(C-Avg4) , k=amount*1.25
// فرمول 3 Luma BT709: Y=0.2126R+0.7152G+0.0722B
// فرمول 4 Luma BT601: Y=0.299R+0.587G+0.114B
// فرمول 5 Reinhard: Out=C/(1+C)
// فرمول 6 Gamma: Out=pow(C,1/2.2)
// فرمول 7 Bicubic w: w(t)=a+2|t|^3-3|t|^2 براي |t|<1
// فرمول 8 Lanczos: L(x)=sinc(x)*sinc(x/a)
// فرمول 9 Edge: E=|L-R|+|T-B|
// فرمول 10 Risk: R=0.5*Temp+0.3*CPU+0.2*Crash*15
#include <cstdint>
namespace axm64 {
float luma709(float r,float g,float b);
float luma601(float r,float g,float b);
float reinhard(float c);
float gamma22(float c);
float bicubicW(float t);
float lanczos(float x,int a);
float edgeMag(float l,float r,float t,float b);
float calcRisk(float temp,float cpu,int crash);
float clamp01(float v);
}
