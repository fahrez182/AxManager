#include <string>
#include <vector>
#include <unordered_set>
#include <thread>
#include <cstdio>
#include <pthread.h>
#include <sched.h>
#include <sys/stat.h>
#include <jni.h>
void pin_big(int id){cpu_set_t s;CPU_ZERO(&s);
#ifndef __x86_64__
CPU_SET((4+id)%8,&s);
#else
CPU_SET(id%8,&s);
#endif
pthread_setaffinity_np(pthread_self(),sizeof(s),&s);}
static long scan_range(const char* p,long st,long en,std::unordered_set<std::string>& pat){
FILE* f=fopen(p,"rb");if(!f) return -1;fseek(f,st,SEEK_SET);
if(st!=0){int c;while((c=fgetc(f))!=EOF&&c!='\n');}
std::vector<char> b(1048576);std::string c,ch;long h=0;long pos=ftell(f);
while(pos<en&&!feof(f)){size_t w=(size_t)((en-pos)>1048576?1048576:(en-pos));size_t r=fread(b.data(),1,w,f);if(r==0)break;pos+=(long)r;ch.assign(c);ch.append(b.data(),r);size_t a=0,q=0;while((q=ch.find('\n',a))!=std::string::npos){if(pat.find(ch.substr(a,q-a))!=pat.end())h++;a=q+1;}c.assign(ch.substr(a));if(c.size()>8192)c.clear();}
fclose(f);return h;}
long scan_mt(const char* p,std::unordered_set<std::string>& pat,int n,long* o){
struct stat st;if(stat(p,&st)!=0)return -1;if(st.st_size>(off_t)500*1024*1024)return -2;
if(n<1)n=4;if(n>8)n=8;long t=st.st_size,part=t/n;
std::vector<std::thread> ts;std::vector<long> rs(n,0);
for(int i=0;i<n;i++){long s=(long)i*part,e=(i==n-1)?t:(long)(i+1)*part;ts.emplace_back([=,&rs,&pat](){pin_big(i);rs[i]=scan_range(p,s,e,pat);});}
for(auto& x:ts)x.join();long s=0;for(auto v:rs)if(v>0)s+=v;if(o)*o=s;return 0;}
int main(int a,char** v){if(a<2)return 2;int th=a>=3?atoi(v[2]):8;std::unordered_set<std::string> pt;pt.insert("boot_completed");long o=0;long rc=scan_mt(v[1],pt,th,&o);printf("threads=%d rc=%ld hits=%ld\n",th,rc,o);return 0;}
extern "C" JNIEXPORT jlong JNICALL Java_com_axdubai_manager_core_AxOmniBridge_scanNative(JNIEnv* e,jobject t,jstring p,jint n){const char* s=e->GetStringUTFChars(p,0);std::unordered_set<std::string> pt;pt.insert("boot_completed");long o=0;long rc=scan_mt(s,pt,n,&o);e->ReleaseStringUTFChars(p,s);return rc!=0?(jlong)rc:(jlong)o;}
