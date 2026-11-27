#pragma once
static inline void rgb_breathing(int t,int* r,int* g,int* b){
int p=t%360;
*r=(p<120)?255:(p<240)?120:0;
*g=(p<120)?180:255;
*b=(p<180)?255:60;
}
static inline unsigned int color32(int r,int g,int b){return (0xFFu<<24)|((r&255)<<16)|((g&255)<<8)|(b&255);}
