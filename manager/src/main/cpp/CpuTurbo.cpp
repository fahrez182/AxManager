#include <jni.h>
#include <unistd.h>
#include <cstdio>

extern "C" JNIEXPORT jint JNICALL
Java_frb_axeron_manager_util_CpuTurbo_cores(JNIEnv*, jclass) {
    return (jint)sysconf(_SC_NPROCESSORS_ONLN);
}

extern "C" JNIEXPORT jint JNICALL
Java_frb_axeron_manager_util_CpuTurbo_freq(JNIEnv*, jclass, jint cpu) {
    char path[128];
    snprintf(path, sizeof(path), "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq", cpu);
    FILE* f = fopen(path, "r");
    if (!f) return -1;
    int freq = -1;
    fscanf(f, "%d", &freq);
    fclose(f);
    return freq / 1000;
}
