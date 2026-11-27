#include <jni.h>
#include <fcntl.h>
#include <unistd.h>

static long readMem(const char* key) {
    int fd = open("/proc/meminfo", O_RDONLY);
    if (fd < 0) return -1;
    char buf[2048];
    int n = read(fd, buf, sizeof(buf)-1);
    close(fd);
    if (n <= 0) return -1;
    buf[n] = 0;
    // جست‌وجوی سریع بدون sscanf در حلقه
    const char* p = buf;
    while (*p) {
        if (__builtin_strncmp(p, key, 8) == 0) {
            while (*p && (*p<'0' || *p>'9')) p++;
            long v = 0;
            while (*p>='0' && *p<='9') { v = v*10 + (*p-'0'); p++; }
            return v / 1024; // KB به MB
        }
        while (*p && *p!='\n') p++;
        if (*p) p++;
    }
    return -1;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_axeron_tools_RamTurbo_avail(JNIEnv*, jclass) {
    return (jint)readMem("MemAvaila");
}
extern "C" JNIEXPORT jint JNICALL
Java_com_axeron_tools_RamTurbo_total(JNIEnv*, jclass) {
    // MemTotal: 8 حرف اولش با MemAvaila فرق دارد ولی همین تابع جواب می‌دهد
    int fd = open("/proc/meminfo", O_RDONLY);
    char buf[256]; int n = read(fd, buf, 200); close(fd);
    buf[n]=0; long v=0; const char* p=buf;
    while(*p && (*p<'0'||*p>'9')) p++;
    while(*p>='0'&&*p<='9'){ v=v*10+(*p-'0'); p++; }
    return (jint)(v/1024);
}
