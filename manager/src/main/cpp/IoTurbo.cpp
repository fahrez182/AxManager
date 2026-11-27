#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/statvfs.h>

extern "C" JNIEXPORT jint JNICALL
Java_com_axeron_tools_IoTurbo_copy(JNIEnv* env, jclass, jstring s_, jstring d_) {
    const char* s = env->GetStringUTFChars(s_,0);
    const char* d = env->GetStringUTFChars(d_,0);
    int in = open(s, O_RDONLY);
    int out = open(d, O_WRONLY|O_CREAT|O_TRUNC, 0644);
    int ret = -1;
    if (in>=0 && out>=0) {
        char buf[65536]; // بافر 64K = بهترین تعادل سرعت و رم
        ssize_t n; ret=0;
        while ((n=read(in,buf,sizeof(buf)))>0) {
            if (write(out,buf,n)!=n){ ret=-2; break; }
        }
    }
    if (in>=0) close(in);
    if (out>=0) close(out);
    env->ReleaseStringUTFChars(s_,s);
    env->ReleaseStringUTFChars(d_,d);
    return ret;
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_axeron_tools_IoTurbo_free(JNIEnv* env, jclass, jstring p_) {
    const char* p = env->GetStringUTFChars(p_,0);
    struct statvfs st; long long freeB=0;
    if (statvfs(p,&st)==0) freeB=(long long)st.f_bavail*st.f_frsize;
    env->ReleaseStringUTFChars(p_,p);
    return freeB;
}
