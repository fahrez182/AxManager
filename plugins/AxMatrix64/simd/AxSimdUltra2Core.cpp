#include "AxSimdUltra2.h"
#include <algorithm>
#include <cmath>
#if defined(__aarch64__)
#include <arm_neon.h>
#endif
namespace axm64 {
// ------------------------------------------------------------
// 0 و 1: uint8 یعنی 8 بیت 00000000 تا 11111111 معادل 0 تا 255
// float32 یعنی 1 بیت علامت + 8 بیت نما + 23 بیت مانتیس
// مثال 0.5 = 0 01111110 00000000000000000000000
// رجیستر q0 یعنی 128 بیت یعنی 4 float هم‌زمان
// اسمبلی: LDP q0,q1,[x0] یعنی 32 بایت بخوان
// ------------------------------------------------------------
static inline uint8_t clamp_u8_fast(float v) {
  // CMP + CSEL در سطح اسمبلی، بدون برنچ
  int iv = (int)(v + 0.5f);
  if (iv < 0) iv = 0;
  if (iv > 255) iv = 255;
  return (uint8_t)iv;
}
// ------------------------------------------------------------
// UpscaleBilinearNEON
// فرمول: Top=A+(B-A)*fx , Bot=C+(D-C)*fx , Out=Top+(Bot-Top)*fy
// نگاشت: UCVTF q->q ، FSUB ، FMLA ، FCVT
// چرا vdupq_n_f32 برای fy؟ چون هر 4 کانال یک fy دارند
// چرا prefetch؟ سطر بعد را به L1 می‌آورد: PRFM PLDL1KEEP
// پهنای باند 540p->1080p: خواندن 2MB + نوشتن 8MB = 10MB هر فریم
// در 60 فریم = 600MB/s که برای LPDDR5 راحت است
// ------------------------------------------------------------
void UpscaleBilinearNEON(const uint8_t* src,int sw,int sh,uint8_t* dst,int dw,int dh) {
  if(!src||!dst) return;
  if(sw<2||sh<2||dw<2||dh<2) return;
  float xRatio=(float)(sw-1)/(float)(dw-1);
  float yRatio=(float)(sh-1)/(float)(dh-1);
#if defined(__aarch64__)
  float32x4_t vHalf=vdupq_n_f32(0.5f);
  for(int y=0;y<dh;++y) {
    float sy=y*yRatio;
    int y0=(int)sy;
    int y1=y0+1<sh?y0+1:sh-1;
    float fy=sy-(float)y0;
    float32x4_t vfy=vdupq_n_f32(fy);
    const uint8_t* row0=src+(size_t)y0*(size_t)sw*4;
    const uint8_t* row1=src+(size_t)y1*(size_t)sw*4;
    uint8_t* drow=dst+(size_t)y*(size_t)dw*4;
    if(y+2<dh) {
      int yp=(int)((y+2)*yRatio);
      if(yp<sh) __builtin_prefetch(src+(size_t)yp*(size_t)sw*4,0,3);
    }
    for(int x=0;x<dw;++x) {
      float sx=x*xRatio;
      int x0=(int)sx;
      int x1=x0+1<sw?x0+1:sw-1;
      float fx=sx-(float)x0;
      uint8_t a0=row0[x0*4+0]; uint8_t a1=row0[x0*4+1];
      uint8_t a2=row0[x0*4+2]; uint8_t a3=row0[x0*4+3];
      uint8_t b0=row0[x1*4+0]; uint8_t b1=row0[x1*4+1];
      uint8_t b2=row0[x1*4+2]; uint8_t b3=row0[x1*4+3];
      uint8_t c0=row1[x0*4+0]; uint8_t c1=row1[x0*4+1];
      uint8_t c2=row1[x0*4+2]; uint8_t c3=row1[x0*4+3];
      uint8_t d0=row1[x1*4+0]; uint8_t d1=row1[x1*4+1];
      uint8_t d2=row1[x1*4+2]; uint8_t d3=row1[x1*4+3];
      uint32x4_t ia={a0,a1,a2,a3};
      uint32x4_t ib={b0,b1,b2,b3};
      uint32x4_t ic={c0,c1,c2,c3};
      uint32x4_t id={d0,d1,d2,d3};
      float32x4_t va=vcvtq_f32_u32(ia);
      float32x4_t vb=vcvtq_f32_u32(ib);
      float32x4_t vc=vcvtq_f32_u32(ic);
      float32x4_t vd=vcvtq_f32_u32(id);
      float32x4_t vfx=vdupq_n_f32(fx);
      float32x4_t vTop=vmlaq_f32(va,vsubq_f32(vb,va),vfx);
      float32x4_t vBot=vmlaq_f32(vc,vsubq_f32(vd,vc),vfx);
      float32x4_t vOut=vmlaq_f32(vTop,vsubq_f32(vBot,vTop),vfy);
      uint32x4_t vi=vcvtq_u32_f32(vaddq_f32(vOut,vHalf));
      drow[x*4+0]=(uint8_t)std::min<uint32_t>(vgetq_lane_u32(vi,0),255);
      drow[x*4+1]=(uint8_t)std::min<uint32_t>(vgetq_lane_u32(vi,1),255);
      drow[x*4+2]=(uint8_t)std::min<uint32_t>(vgetq_lane_u32(vi,2),255);
      drow[x*4+3]=(uint8_t)std::min<uint32_t>(vgetq_lane_u32(vi,3),255);
    }
  }
#else
  for(int y=0;y<dh;++y) {
    float sy=y*yRatio; int y0=(int)sy; int y1=std::min(y0+1,sh-1); float fy=sy-y0;
    for(int x=0;x<dw;++x) {
      float sx=x*xRatio; int x0=(int)sx; int x1=std::min(x0+1,sw-1); float fx=sx-x0;
      for(int c=0;c<4;++c) {
        float a=src[(y0*sw+x0)*4+c]; float b=src[(y0*sw+x1)*4+c];
        float cc=src[(y1*sw+x0)*4+c]; float d=src[(y1*sw+x1)*4+c];
        float v=a*(1-fx)*(1-fy)+b*fx*(1-fy)+cc*(1-fx)*fy+d*fx*fy;
        dst[(y*dw+x)*4+c]=clamp_u8_fast(v);
      }
    }
  }
#endif
}
// ------------------------------------------------------------
// Upscale2XFastNEON - مسیر تند separable
// مرحله افقی: t[2x]=a , t[2x+1]=(a+b+1)>>1 با VRHADD
// مرحله عمودی: d[2y]=t[y] , d[2y+1]=(t[y]+t[y+1]+1)>>1
// اسمبلی: LD4 {v0.8b-v3.8b} ، VRHADD v0.8b,v0.8b,v1.8b ، ST4
// چرا 8 بایت؟ چون vrhadd_u8 روی 64 بیت کار می‌کند
// چرا tmpRow؟ تا سطر میانی در L1 بماند و به DRAM نرود
// ------------------------------------------------------------
void Upscale2XFastNEON(const uint8_t* src,int sw,int sh,uint8_t* dst,uint8_t* tmpRow) {
  if(!src||!dst||!tmpRow) return;
  if(sw<2||sh<2) return;
  int dw=sw*2; int tmpW=sw*2;
#if defined(__aarch64__)
  for(int y=0;y<sh;++y) {
    const uint8_t* s=src+(size_t)y*(size_t)sw*4;
    uint8_t* t=tmpRow;
    int x=0;
    for(;x+8<=sw;x+=8) {
      uint8x8x4_t p=vld4_u8(s+(size_t)x*4);
      uint8_t nxtBuf[8*4];
      for(int k=0;k<7;++k) {
        nxtBuf[k*4+0]=s[(x+k+1)*4+0]; nxtBuf[k*4+1]=s[(x+k+1)*4+1];
        nxtBuf[k*4+2]=s[(x+k+1)*4+2]; nxtBuf[k*4+3]=s[(x+k+1)*4+3];
      }
      int lastX=(x+8<sw)?(x+8):(sw-1);
      nxtBuf[7*4+0]=s[lastX*4+0]; nxtBuf[7*4+1]=s[lastX*4+1];
      nxtBuf[7*4+2]=s[lastX*4+2]; nxtBuf[7*4+3]=s[lastX*4+3];
      uint8x8x4_t q=vld4_u8(nxtBuf);
      uint8x8_t m0=vrhadd_u8(p.val[0],q.val[0]);
      uint8x8_t m1=vrhadd_u8(p.val[1],q.val[1]);
      uint8x8_t m2=vrhadd_u8(p.val[2],q.val[2]);
      uint8_t p0[8],p1[8],p2[8],p3[8];
      vst1_u8(p0,p.val[0]); vst1_u8(p1,p.val[1]);
      vst1_u8(p2,p.val[2]); vst1_u8(p3,p.val[3]);
      uint8_t m0b[8],m1b[8],m2b[8];
      vst1_u8(m0b,m0); vst1_u8(m1b,m1); vst1_u8(m2b,m2);
      uint8_t out[16*4];
      for(int k=0;k<8;++k) {
        out[(k*2)*4+0]=p0[k]; out[(k*2)*4+1]=p1[k];
        out[(k*2)*4+2]=p2[k]; out[(k*2)*4+3]=p3[k];
        out[(k*2+1)*4+0]=m0b[k]; out[(k*2+1)*4+1]=m1b[k];
        out[(k*2+1)*4+2]=m2b[k]; out[(k*2+1)*4+3]=p3[k];
      }
      for(int k=0;k<16*4;++k) t[(x*2)*4+k]=out[k];
    }
    for(;x<sw;++x) {
      int x1=x+1<sw?x+1:x;
      for(int c=0;c<4;++c) {
        uint8_t a=s[x*4+c]; uint8_t b=s[x1*4+c];
        t[(x*2)*4+c]=a;
        t[(x*2+1)*4+c]=(uint8_t)((a+b+1)>>1);
      }
    }
    uint8_t* d0=dst+(size_t)(y*2)*(size_t)dw*4;
    uint8_t* d1=dst+(size_t)(y*2+1)*(size_t)dw*4;
    int n=tmpW*4; int i=0;
    for(;i+16<=n;i+=16) { uint8x16_t v=vld1q_u8(t+i); vst1q_u8(d0+i,v); }
    for(;i<n;++i) d0[i]=t[i];
    for(i=0;i<n;++i) d1[i]=t[i];
  }
  for(int y=0;y<sh;++y) {
    int y1=y+1<sh?y+1:y;
    uint8_t* r0=dst+(size_t)(y*2)*(size_t)dw*4;
    uint8_t* r1=dst+(size_t)(y1*2)*(size_t)dw*4;
    uint8_t* rd=dst+(size_t)(y*2+1)*(size_t)dw*4;
    int n=dw*4; int i=0;
    for(;i+8<=n;i+=8) {
      uint8x8_t a=vld1_u8(r0+i); uint8x8_t b=vld1_u8(r1+i);
      vst1_u8(rd+i,vrhadd_u8(a,b));
    }
    for(;i<n;++i) rd[i]=(uint8_t)((r0[i]+r1[i]+1)>>1);
  }
#else
  UpscaleBilinearNEON(src,sw,sh,dst,sw*2,sh*2);
#endif
}
// ------------------------------------------------------------
// SharpenNEON - Out=C+k*(C-Avg4)
// Avg4=(L+R+T+B+2)>>2 با int16 تا منفی گم نشود
// چرا کپی tmp؟ چون درجا می‌نویسیم و همسایه خراب می‌شود
// برای 1080p حافظه موقت 8MB است، بعد delete می‌شود
// اسمبلی: VMOVL.U8->S16 ، VADD ، VSHR ، VSUB ، VMLA ، VQMOVUN
// ------------------------------------------------------------
void SharpenNEON(uint8_t* img,int w,int h,float amount) {
  if(!img||w<3||h<3) return;
  if(amount<=0.01f) return;
  if(amount>1.0f) amount=1.0f;
#if defined(__aarch64__)
  size_t total=(size_t)w*(size_t)h*4;
  uint8_t* tmp=new (std::nothrow) uint8_t[total];
  if(!tmp) return;
  for(size_t i=0;i<total;++i) tmp[i]=img[i];
  float32x4_t vAmt=vdupq_n_f32(amount*1.25f);
  float32x4_t vHalf=vdupq_n_f32(0.5f);
  int32x4_t vZero=vdupq_n_s32(0); int32x4_t vMax=vdupq_n_s32(255);
  for(int y=1;y<h-1;++y) {
    uint8_t* dstRow=img+(size_t)y*(size_t)w*4;
    const uint8_t* cRow=tmp+(size_t)y*(size_t)w*4;
    const uint8_t* tRow=tmp+(size_t)(y-1)*(size_t)w*4;
    const uint8_t* bRow=tmp+(size_t)(y+1)*(size_t)w*4;
    int x=1;
    for(;x+8<=w-1;x+=8) {
      uint8x8x4_t c=vld4_u8(cRow+(size_t)x*4);
      uint8x8x4_t l=vld4_u8(cRow+(size_t)(x-1)*4);
      uint8x8x4_t r=vld4_u8(cRow+(size_t)(x+1)*4);
      uint8x8x4_t t=vld4_u8(tRow+(size_t)x*4);
      uint8x8x4_t b=vld4_u8(bRow+(size_t)x*4);
      for(int ch=0;ch<3;++ch) {
        uint8x8_t cv=ch==0?c.val[0]:(ch==1?c.val[1]:c.val[2]);
        uint8x8_t lv=ch==0?l.val[0]:(ch==1?l.val[1]:l.val[2]);
        uint8x8_t rv=ch==0?r.val[0]:(ch==1?r.val[1]:r.val[2]);
        uint8x8_t tv=ch==0?t.val[0]:(ch==1?t.val[1]:t.val[2]);
        uint8x8_t bv=ch==0?b.val[0]:(ch==1?b.val[1]:b.val[2]);
        int16x8_t ci=vreinterpretq_s16_u16(vmovl_u8(cv));
        int16x8_t li=vreinterpretq_s16_u16(vmovl_u8(lv));
        int16x8_t ri=vreinterpretq_s16_u16(vmovl_u8(rv));
        int16x8_t ti=vreinterpretq_s16_u16(vmovl_u8(tv));
        int16x8_t bi=vreinterpretq_s16_u16(vmovl_u8(bv));
        int16x8_t sum=vaddq_s16(vaddq_s16(li,ri),vaddq_s16(ti,bi));
        int16x8_t avg=vshrq_n_s16(vaddq_s16(sum,vdupq_n_s16(2)),2);
        int16x8_t diff=vsubq_s16(ci,avg);
        int16x4_t dLo=vget_low_s16(diff); int16x4_t dHi=vget_high_s16(diff);
        int16x4_t cLo=vget_low_s16(ci); int16x4_t cHi=vget_high_s16(ci);
        float32x4_t fLo=vcvtq_f32_s32(vmovl_s16(dLo));
        float32x4_t fHi=vcvtq_f32_s32(vmovl_s16(dHi));
        float32x4_t cLoF=vcvtq_f32_s32(vmovl_s16(cLo));
        float32x4_t cHiF=vcvtq_f32_s32(vmovl_s16(cHi));
        float32x4_t oLoF=vmlaq_f32(cLoF,fLo,vAmt);
        float32x4_t oHiF=vmlaq_f32(cHiF,fHi,vAmt);
        int32x4_t oLo=vcvtq_s32_f32(vaddq_f32(oLoF,vHalf));
        int32x4_t oHi=vcvtq_s32_f32(vaddq_f32(oHiF,vHalf));
        oLo=vmaxq_s32(vminq_s32(oLo,vMax),vZero);
        oHi=vmaxq_s32(vminq_s32(oHi,vMax),vZero);
        uint8x8_t out=vqmovun_s16(vcombine_s16(vqmovn_s32(oLo),vqmovn_s32(oHi)));
        if(ch==0) c.val[0]=out; else if(ch==1) c.val[1]=out; else c.val[2]=out;
      }
      vst4_u8(dstRow+(size_t)x*4,c);
    }
    for(;x<w-1;++x) for(int k=0;k<3;++k) {
      int idx=x*4+k;
      float center=(float)cRow[idx];
      float avg=((float)cRow[idx-4]+(float)cRow[idx+4]+(float)tRow[idx]+(float)bRow[idx])*0.25f;
      float v=center+(center-avg)*amount*1.25f;
      if(v<0) v=0; if(v>255) v=255;
      dstRow[idx]=(uint8_t)(v+0.5f);
    }
  }
  delete[] tmp;
#else
  size_t total=(size_t)w*(size_t)h*4;
  uint8_t* tmp=new (std::nothrow) uint8_t[total];
  if(!tmp) return;
  for(size_t i=0;i<total;++i) tmp[i]=img[i];
  for(int y=1;y<h-1;++y) for(int x=1;x<w-1;++x) for(int k=0;k<3;++k) {
    int i=(y*w+x)*4+k;
    float center=tmp[i]/255.0f;
    float avg=(tmp[i-4]+tmp[i+4]+tmp[i-w*4]+tmp[i+w*4])/(4.0f*255.0f);
    float v=center+(center-avg)*amount*1.25f;
    if(v<0) v=0; if(v>1) v=1;
    img[i]=(uint8_t)(v*255.0f+0.5f);
  }
  delete[] tmp;
#endif
}
// ------------------------------------------------------------
// SharpenAdaptiveNEON - فقط لبه‌ها شارپ می‌خورند
// edge = |L-R|+|T-B| ، اگر از thresh بیشتر بود k کامل وگرنه نصف
// این خلاقیت باعث می‌شود آسمان نویزی نشود و متن تیز بماند
// ------------------------------------------------------------
void SharpenAdaptiveNEON(uint8_t* img,int w,int h,float amount,float edgeThresh) {
  if(!img||w<3||h<3) return;
  if(amount<=0.01f) return;
  if(edgeThresh<4.0f) edgeThresh=4.0f;
  if(edgeThresh>40.0f) edgeThresh=40.0f;
  size_t total=(size_t)w*(size_t)h*4;
  uint8_t* tmp=new (std::nothrow) uint8_t[total];
  if(!tmp) return;
  for(size_t i=0;i<total;++i) tmp[i]=img[i];
  for(int y=1;y<h-1;++y) {
    for(int x=1;x<w-1;++x) {
      float lumC=(tmp[(y*w+x)*4+0]+tmp[(y*w+x)*4+1]+tmp[(y*w+x)*4+2])/3.0f;
      float lumL=(tmp[(y*w+x-1)*4+0]+tmp[(y*w+x-1)*4+1]+tmp[(y*w+x-1)*4+2])/3.0f;
      float lumR=(tmp[(y*w+x+1)*4+0]+tmp[(y*w+x+1)*4+1]+tmp[(y*w+x+1)*4+2])/3.0f;
      float lumT=(tmp[((y-1)*w+x)*4+0]+tmp[((y-1)*w+x)*4+1]+tmp[((y-1)*w+x)*4+2])/3.0f;
      float lumB=(tmp[((y+1)*w+x)*4+0]+tmp[((y+1)*w+x)*4+1]+tmp[((y+1)*w+x)*4+2])/3.0f;
      float edge=(lumL>lumR?lumL-lumR:lumR-lumL)+(lumT>lumB?lumT-lumB:lumB-lumT);
      float k=amount*1.25f*(edge>edgeThresh?1.0f:0.35f);
      for(int c=0;c<3;++c) {
        int i=(y*w+x)*4+c;
        float center=(float)tmp[i];
        float avg=((float)tmp[i-4]+(float)tmp[i+4]+(float)tmp[i-w*4]+(float)tmp[i+w*4])*0.25f;
        float v=center+(center-avg)*k;
        if(v<0) v=0; if(v>255) v=255;
        img[i]=(uint8_t)(v+0.5f);
      }
    }
  }
  delete[] tmp;
}
// ------------------------------------------------------------
// ProcessFrameAuto - اگر دقیقا 2x بود تند وگرنه عمومی
// scratch باید dw*4 بایت باشد
// ------------------------------------------------------------
SimdFrameInfo ProcessFrameAuto(const uint8_t* low,int lw,int lh,uint8_t* high,int hw,int hh,float sharp,uint8_t* scratch) {
  SimdFrameInfo s; s.srcW=lw; s.srcH=lh; s.dstW=hw; s.dstH=hh;
  s.sharpAmount=sharp; s.upscaleRatio=(lw>0)?(float)hw/(float)lw:1.0f;
  s.usedFast2X=false; s.edgeBoost=1.0f;
  if(!low||!high) return s;
  if(hw==lw*2&&hh==lh*2&&scratch) {
    Upscale2XFastNEON(low,lw,lh,high,scratch);
    s.usedFast2X=true;
  } else {
    UpscaleBilinearNEON(low,lw,lh,high,hw,hh);
  }
  if(sharp>0.6f) SharpenAdaptiveNEON(high,hw,hh,sharp,12.0f);
  else SharpenNEON(high,hw,hh,sharp);
  return s;
}
} // namespace axm64
