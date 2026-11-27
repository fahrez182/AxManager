#include <jni.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>
#include <sys/time.h>

extern "C" JNIEXPORT jint JNICALL
Java_frb_axeron_manager_util_NetTurbo_ping(JNIEnv*, jclass) {
    struct timespec t1,t2;
    clock_gettime(CLOCK_MONOTONIC,&t1);
    int s = socket(AF_INET,SOCK_STREAM,0);
    if (s<0) return -1;
    struct sockaddr_in a; a.sin_family=AF_INET; a.sin_port=htons(53);
    a.sin_addr.s_addr=inet_addr("8.8.8.8");
    struct timeval tv; tv.tv_sec=2; tv.tv_usec=0;
    setsockopt(s,SOL_SOCKET,SO_SNDTIMEO,&tv,sizeof(tv));
    int r = connect(s,(sockaddr*)&a,sizeof(a));
    close(s);
    clock_gettime(CLOCK_MONOTONIC,&t2);
    if (r!=0) return 9999;
    long ms=(t2.tv_sec-t1.tv_sec)*1000+(t2.tv_nsec-t1.tv_nsec)/1000000;
    return (jint)ms;
}
