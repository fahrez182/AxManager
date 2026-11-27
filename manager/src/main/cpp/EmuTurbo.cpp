#include <jni.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <cstdio>

static void getProp(const char* k, char* v) {
    __system_property_get(k, v);
}

extern "C" JNIEXPORT jstring JNICALL
Java_frb_axeron_manager_util_EmuTurbo_get(JNIEnv* env, jclass) {
    char qemu[PROP_VALUE_MAX] = {0};
    char model[PROP_VALUE_MAX] = {0};
    char brand[PROP_VALUE_MAX] = {0};
    getProp("ro.kernel.qemu", qemu);
    getProp("ro.product.model", model);
    getProp("ro.product.brand", brand);
    int cores = (int)sysconf(_SC_NPROCESSORS_ONLN);
    char out[512];
    int isEmu = (qemu[0]=='1');
    snprintf(out, sizeof(out), "emu=%d model=%s brand=%s cores=%d",
        isEmu, model, brand, cores);
    return env->NewStringUTF(out);
}
