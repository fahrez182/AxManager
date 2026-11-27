#include <jni.h>
#include <string>
#include <cstdio>
#include <algorithm>

static inline int clamp8(int v) {
    if (v < 0) return 0;
    if (v > 255) return 255;
    return v;
}

extern "C" JNIEXPORT jstring JNICALL
Java_frb_axeron_manager_util_GfxTurbo_info(JNIEnv* env, jclass) {
    return env->NewStringUTF("GfxTurbo amoled-adobeRGB 120fps-helper v1");
}

extern "C" JNIEXPORT jint JNICALL
Java_frb_axeron_manager_util_GfxTurbo_targetFps(JNIEnv*, jclass, jint want) {
    if (want >= 120) return 120;
    if (want >= 90) return 90;
    return 60;
}

// vivid شبیه AMOLED Photo: ساتوریشن و کنتراست کمی بالاتر
extern "C" JNIEXPORT void JNICALL
Java_frb_axeron_manager_util_GfxTurbo_applyAmoled(JNIEnv* env, jclass, jintArray px_, jint w, jint h) {
    if (!px_) return;
    jsize len = env->GetArrayLength(px_);
    if (len != w * h || len <= 0) return;
    jint* px = env->GetIntArrayElements(px_, 0);
    if (!px) return;
    for (int i = 0; i < len; i++) {
        int c = px[i];
        int a = (c >> 24) & 255;
        int r = (c >> 16) & 255;
        int g = (c >> 8) & 255;
        int b = c & 255;
        // خاکستری برای محاسبه ساتوریشن
        int gray = (r * 30 + g * 59 + b * 11) / 100;
        // 1.18 برابر vivid + کنتراست ملایم
        r = clamp8((int)((gray + (r - gray) * 1.18f) * 1.04f - 6));
        g = clamp8((int)((gray + (g - gray) * 1.18f) * 1.04f - 6));
        b = clamp8((int)((gray + (b - gray) * 1.15f) * 1.03f - 4));
        px[i] = (a << 24) | (r << 16) | (g << 8) | b;
    }
    env->ReleaseIntArrayElements(px_, px, 0);
}
