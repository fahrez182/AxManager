// AxDarkGov2Engine.cpp - dual-arch professional engine
// aarch64: clang++ -O3 -march=armv8-a -flto -pthread -shared -o libaxdark2.so AxDarkGov2Engine.cpp
// amd64: clang++ -O3 -march=x86-64-v3 -flto -pthread -shared -o libaxdark2_amd.so AxDarkGov2Engine.cpp
#include <string>
#include <fstream>
#include <sstream>
#include <unordered_set>
#include <pthread.h>
#include <sched.h>
#include <fcntl.h>
#include <unistd.h>
void pin_big(int id){
  cpu_set_t s; CPU_ZERO(&s);
#ifdef __x86_64__
  CPU_SET(id % 8, &s);
#else
  CPU_SET((4 + id) % 8, &s);
#endif
  pthread_setaffinity_np(pthread_self(), sizeof(s), &s);
}
std::string hw_info(){
  std::ifstream m("/proc/meminfo");
  std::string k, v; long t = 0, a = 0;
  while(m >> k >> v){ if(k=="MemTotal:") t=atol(v.c_str()); if(k=="MemAvailable:") a=atol(v.c_str()); if(t&&a) break; }
  std::ostringstream o; o << "MemTotalKB=" << t << " MemAvailKB=" << a;
  return o.str();
}
long turbo_scan(const char* path, std::unordered_set<std::string>& pat){
  int fd = open(path, O_RDONLY); if(fd<0) return -1;
  char buf[4194304]; ssize_t r; long hits = 0; std::string carry;
  while((r=read(fd,buf,sizeof(buf)))>0){
    std::string c = carry + std::string(buf,(size_t)r);
    size_t p=0,q=0;
    while((q=c.find('\n',p))!=std::string::npos){
      if(pat.find(c.substr(p,q-p))!=pat.end()) hits++;
      p=q+1;
    }
    carry = c.substr(p);
  }
  close(fd); return hits;
}
