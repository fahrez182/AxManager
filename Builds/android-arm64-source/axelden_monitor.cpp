#include <jni.h>
#include <android/log.h>
#include <sys/sysinfo.h>
#include <time.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>
#include <string>
#include <mutex>
#include <algorithm>

namespace {
std::mutex g_lock;
unsigned long long g_prev_total = 0, g_prev_idle = 0;

bool read_cpu(unsigned long long &total, unsigned long long &idle) {
    FILE *f = fopen("/proc/stat", "r");
    if (!f) return false;
    unsigned long long u=0,n=0,s=0,i=0,iw=0,irq=0,soft=0,steal=0;
    const int got = fscanf(f, "cpu %llu %llu %llu %llu %llu %llu %llu %llu", &u,&n,&s,&i,&iw,&irq,&soft,&steal);
    fclose(f);
    if (got < 4) return false;
    idle = i + iw;
    total = u+n+s+i+iw+irq+soft+steal;
    return true;
}

long read_mem_kb(const char *key) {
    FILE *f = fopen("/proc/meminfo", "r");
    if (!f) return -1;
    char line[256], name[64]; long value = -1;
    while (fgets(line, sizeof(line), f)) {
        if (sscanf(line, "%63s %ld", name, &value) == 2) {
            if (strncmp(name, key, strlen(key)) == 0) { fclose(f); return value; }
        }
    }
    fclose(f); return -1;
}

long read_thermal_milli_c() {
    // Read-only best effort. Use the maximum plausible zone value because
    // zone ordering and sensor labels vary across Android vendors.
    long maximum = -1;
    for (int i=0; i<64; ++i) {
        char path[96];
        snprintf(path, sizeof(path), "/sys/class/thermal/thermal_zone%d/temp", i);
        FILE *f = fopen(path, "r");
        if (!f) continue;
        long value = -1;
        const int ok = fscanf(f, "%ld", &value);
        fclose(f);
        if (ok == 1 && value >= 1000 && value <= 200000)
            maximum = std::max(maximum, value);
    }
    return maximum;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample(JNIEnv *env, jobject) {
    struct sysinfo si{};
    const bool sys_ok = (sysinfo(&si) == 0);
    const long mem_total = read_mem_kb("MemTotal:");
    const long mem_avail = read_mem_kb("MemAvailable:");
    const long thermal = read_thermal_milli_c();
    double cpu = -1.0;
    {
        std::lock_guard<std::mutex> guard(g_lock);
        unsigned long long total=0, idle=0;
        if (read_cpu(total, idle)) {
            if (g_prev_total && total > g_prev_total && idle >= g_prev_idle) {
                const auto dt = total - g_prev_total;
                const auto di = idle - g_prev_idle;
                cpu = std::clamp(100.0 * (double)(dt-di) / (double)dt, 0.0, 100.0);
            }
            g_prev_total = total; g_prev_idle = idle;
        }
    }
    char out[512];
    snprintf(out, sizeof(out),
      "{\"mem_total_kb\":%ld,\"mem_available_kb\":%ld,\"cpu_busy_percent\":%.2f,\"thermal_milli_c\":%ld,\"uptime_seconds\":%ld,\"native_ok\":%s}",
      mem_total, mem_avail, cpu, thermal, sys_ok ? (long)si.uptime : -1L, sys_ok ? "true" : "false");
    return env->NewStringUTF(out);
}
