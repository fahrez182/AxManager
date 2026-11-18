#include <jni.h>
#include <string>
#include <fstream>
#include <sstream>
#include <dirent.h>
#include <cstdlib>
#include <algorithm>
#include <cctype>
#include <sys/sysinfo.h>
#include <time.h>

static std::string read_file(const std::string& path) {
    std::ifstream f(path); if (!f) return {}; std::ostringstream s; s << f.rdbuf(); return s.str();
}
static long long number(const std::string& s, long long fallback=-1) {
    try { size_t n=0; long long v=std::stoll(s,&n); return n? v:fallback; } catch (...) { return fallback; }
}
static long long battery() {
    DIR* d=opendir("/sys/class/power_supply"); if(!d) return -1;
    long long result=-1; dirent* e;
    while((e=readdir(d))) { std::string n=e->d_name; if(n=="."||n=="..") continue;
        auto type=read_file("/sys/class/power_supply/"+n+"/type");
        if(type.find("Battery")!=std::string::npos) { result=number(read_file("/sys/class/power_supply/"+n+"/capacity")); if(result>=0) break; }
    } closedir(d); return result;
}
static long long max_thermal_mC() {
    DIR* d=opendir("/sys/class/thermal"); if(!d) return -1;
    long long best=-1; dirent* e;
    while((e=readdir(d))) { std::string n=e->d_name; if(n.rfind("thermal_zone",0)!=0) continue;
        long long v=number(read_file("/sys/class/thermal/"+n+"/temp"));
        // Android thermal sysfs is commonly millidegrees C; reject implausible values.
        if(v>=1000 && v<=200000) best=std::max(best,v);
    } closedir(d); return best;
}
static std::string snapshot() {
    struct sysinfo si{}; sysinfo(&si);
    long long total=0, avail=0;
    std::istringstream m(read_file("/proc/meminfo")); std::string key,unit; long long val;
    while(m>>key>>val>>unit) { if(key=="MemTotal:") total=val; else if(key=="MemAvailable:") avail=val; }
    std::string load=read_file("/proc/loadavg"); if(!load.empty()) { auto x=load.find(' '); if(x!=std::string::npos) load.resize(x); }
    long long b=battery(), t=max_thermal_mC();
    std::ostringstream o; o << "{\"uptime_sec\":" << si.uptime
      << ",\"ram_total_kb\":" << total << ",\"ram_available_kb\":" << avail
      << ",\"battery_percent\":" << b << ",\"thermal_max_mC\":" << t
      << ",\"load1\":\"" << load << "\"}";
    return o.str();
}
extern "C" JNIEXPORT jstring JNICALL
Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot(JNIEnv* env, jclass) {
    std::string s=snapshot(); return env->NewStringUTF(s.c_str());
}
extern "C" JNIEXPORT jlong JNICALL
Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis(JNIEnv*, jclass) {
    struct timespec ts{}; clock_gettime(CLOCK_MONOTONIC,&ts); return (jlong)ts.tv_sec*1000+ts.tv_nsec/1000000;
}
