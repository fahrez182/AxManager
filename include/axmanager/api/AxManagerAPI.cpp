#include "axmanager/api/AxManagerAPI.hpp"
#if __has_include("axmanager/memory/core/nexus/AxCoreNexus.hpp")
#include "axmanager/memory/core/nexus/AxCoreNexus.hpp"
#define HAS_NEXUS 1
#endif
#if __has_include("axmanager/memory/core/AntiCheatShield.h")
#include "axmanager/memory/core/AntiCheatShield.h"
#define HAS_SHIELD 1
#endif
#if __has_include("axmanager/memory/core/hardware/NeuralHardwareBridge.h")
#include "axmanager/memory/core/hardware/NeuralHardwareBridge.h"
#define HAS_BRIDGE 1
#endif
#include <mutex>
#include <queue>
#include <thread>
#include <chrono>
#include <fstream>
#include <sstream>
#include <map>
#include <cmath>
#include <sched.h>
#include <unistd.h>
#ifdef __ANDROID__
#include <android/log.h>
#define L(...) __android_log_print(ANDROID_LOG_INFO,"AxAPI",__VA_ARGS__)
#else
#define L(...) do{}while(0)
#endif
namespace axmanager::api {
static int64_t memFld(const char*k){std::ifstream f("/proc/meminfo");std::string a,b;int64_t v;while(f>>a>>v>>b)if(a==k)return v*1024LL;return 0;}
struct CT{uint64_t i=0,t=0;};
static std::vector<CT> cpuT(){std::vector<CT> o;std::ifstream f("/proc/stat");std::string ln;while(std::getline(f,ln)){if(ln.rfind("cpu",0)!=0)break;if(ln.size()>3&&ln[3]!=' '&&(ln[3]<'0'||ln[3]>'9'))continue;std::istringstream s(ln);std::string n;s>>n;uint64_t u,nn,ss,id,io,ir,si,st;s>>u>>nn>>ss>>id>>io>>ir>>si>>st;CT c;c.i=id+io;c.t=u+nn+ss+id+io+ir+si+st;o.push_back(c);}return o;}
struct Item{TaskPriority pr;uint64_t sq;std::function<void()> fn;std::shared_ptr<std::promise<void>> prm;bool operator<(const Item&o)const{if((int)pr!=(int)o.pr)return(int)pr<(int)o.pr;return sq>o.sq;}};
struct AxManager::Impl{std::mutex m;std::string err;AxError code=AxError::OK;ApiConfig cfg;std::atomic<bool> inited{false},run{false};
std::mutex qm;std::condition_variable cv;std::priority_queue<Item> q;std::vector<std::thread> ws;std::atomic<bool> stop{false};std::atomic<uint64_t> sq{0};
std::mutex pm;std::map<uint64_t,std::pair<std::function<void()>,bool>> per;std::atomic<uint64_t> ps{1};
std::shared_ptr<IInferenceBackend> be;std::string loaded;std::vector<CT> prev;
void loop(){while(!stop.load()){Item it;{std::unique_lock<std::mutex> l(qm);cv.wait(l,[&]{return stop.load()||!q.empty();});if(stop.load())return;it=q.top();q.pop();}try{if(it.fn)it.fn();if(it.prm)it.prm->set_value();}catch(...){try{if(it.prm)it.prm->set_exception(std::current_exception());}catch(...){}}}}};
AxManager::AxManager():p_(new Impl()){}
AxManager::~AxManager(){shutdown();}
AxManager& AxManager::get(){static AxManager i;return i;}
bool AxManager::is_running() const noexcept{return p_->run.load();}
std::string AxManager::last_error() const{std::lock_guard<std::mutex> l(p_->m);return p_->err;}
AxError AxManager::last_code() const{std::lock_guard<std::mutex> l(p_->m);return p_->code;}
bool AxManager::init(const ApiConfig&c){std::lock_guard<std::mutex> l(p_->m);p_->cfg=c;if(p_->cfg.workers<1)p_->cfg.workers=1;if(p_->cfg.workers>8)p_->cfg.workers=8;p_->prev=cpuT();p_->inited=true;p_->err.clear();p_->code=AxError::OK;return true;}
bool AxManager::start(){if(!p_->inited.load()){ApiConfig d;init(d);}if(p_->run.load())return true;
#ifdef HAS_SHIELD
if(p_->cfg.shield){static ::AxManager::AntiCheatShield s;s.initialize();s.maskSystemIntegrity();s.hideMemoryHooks();if(!p_->cfg.mask.empty())s.cloakProcess();}
#endif
#ifdef HAS_NEXUS
{std::string e;if(!::axmanager::nexus::AxCoreNexus::get_instance().boot_engine(e)){std::lock_guard<std::mutex> l(p_->m);p_->err=e.empty()?"nexus boot fail":e;p_->code=AxError::BOOT_FAIL;return false;}}
#endif
p_->stop=false;for(int i=0;i<p_->cfg.workers;i++)p_->ws.emplace_back([this]{p_->loop();});p_->prev=cpuT();p_->run=true;L("AxManager started");return true;}
void AxManager::shutdown() noexcept{if(!p_)return;p_->run=false;{std::lock_guard<std::mutex> l(p_->pm);p_->per.clear();}p_->stop=true;p_->cv.notify_all();for(auto&t:p_->ws)if(t.joinable())t.join();p_->ws.clear();while(!p_->q.empty())p_->q.pop();
#ifdef HAS_NEXUS
::axmanager::nexus::AxCoreNexus::get_instance().shutdown_engine();
#endif
}
static void pin(int c){if(c<0)return;long n=sysconf(_SC_NPROCESSORS_ONLN);if(n<=0||c>=n)return;cpu_set_t s;CPU_ZERO(&s);CPU_SET((unsigned)c,&s);sched_setaffinity(0,sizeof(s),&s);}
std::future<void> AxManager::run_task(std::function<void()>t){return run_task_ex(std::move(t),TaskOptions{});}
std::future<void> AxManager::run_task_ex(std::function<void()>t,TaskOptions o){auto pr=std::make_shared<std::promise<void>>();auto f=pr->get_future();
if(!t){pr->set_exception(std::make_exception_ptr(std::runtime_error("empty")));return f;}
if(!p_->run.load()){pr->set_exception(std::make_exception_ptr(std::runtime_error("not running")));return f;}
Item it;it.pr=o.prio;it.sq=p_->sq++;it.prm=pr;it.fn=[fn=std::move(t),c=o.core]{pin(c);fn();};
{std::lock_guard<std::mutex> l(p_->qm);if(p_->q.size()>512){pr->set_exception(std::make_exception_ptr(std::runtime_error("queue full")));return f;}p_->q.push(std::move(it));}p_->cv.notify_one();return f;}
uint64_t AxManager::schedule(uint32_t ms,std::function<void()>t){if(!t||!ms)return 0;uint64_t id=p_->ps++;{std::lock_guard<std::mutex> l(p_->pm);p_->per[id]={t,true};}
std::thread([this,id,ms]{while(p_->run.load()){std::this_thread::sleep_for(std::chrono::milliseconds(ms));std::function<void()> fn;{std::lock_guard<std::mutex> l(p_->pm);auto it=p_->per.find(id);if(it==p_->per.end()||!it->second.second)return;fn=it->second.first;}try{fn();}catch(...){}}}).detach();return id;}
void AxManager::cancel_periodic(uint64_t id){std::lock_guard<std::mutex> l(p_->pm);p_->per.erase(id);}
void AxManager::cancel_all(){std::lock_guard<std::mutex> l(p_->qm);while(!p_->q.empty())p_->q.pop();std::lock_guard<std::mutex> l2(p_->pm);p_->per.clear();}
SystemStatus AxManager::status() const{SystemStatus s;s.running=p_->run.load();s.integrity=s.running?"OPTIMAL":"IDLE";s.totalRam=memFld("MemTotal:");int64_t av=memFld("MemAvailable:");s.freeRam=av>0?av:memFld("MemFree:");{std::ifstream f("/sys/class/thermal/thermal_zone0/temp");int v=0;if(f)f>>v;s.temp=v;}{std::ifstream f("/proc/uptime");double u=0;if(f)f>>u;s.uptime=(uint64_t)u;}auto cur=cpuT();auto&pr=const_cast<std::vector<CT>&>(p_->prev);if(!pr.empty()&&cur.size()==pr.size()&&cur.size()>=1){for(size_t i=1;i<cur.size();i++){uint64_t dt=cur[i].t>pr[i].t?cur[i].t-pr[i].t:1;uint64_t di=cur[i].i>pr[i].i?cur[i].i-pr[i].i:0;float x=(1.0f-(float)di/(float)dt)*100;if(x<0)x=0;if(x>100)x=100;s.perCore.push_back(x);}uint64_t dt=cur[0].t>pr[0].t?cur[0].t-pr[0].t:1;uint64_t di=cur[0].i>pr[0].i?cur[0].i-pr[0].i:0;s.cpu=(1.0f-(float)di/(float)dt)*100;}const_cast<std::vector<CT>&>(p_->prev)=cur;return s;}
std::string AxManager::status_string() const{return status().to_string();}
std::string SystemStatus::to_string() const{char b[512];snprintf(b,sizeof(b),"[%s] CPU %.1f%% RAM %lld/%lldMB T %dC Up %llus",running?"RUN":"STOP",cpu,(long long)(freeRam/1048576),(long long)(totalRam/1048576),temp/1000,(unsigned long long)uptime);return b;}
std::string SystemStatus::to_json() const{std::ostringstream o;o<<"{\"run\":"<<(running?"true":"false")<<",\"cpu\":"<<cpu<<",\"free\":"<<freeRam<<",\"total\":"<<totalRam<<",\"temp\":"<<temp<<",\"up\":"<<uptime<<"}";return o.str();}
void AxManager::set_backend(std::shared_ptr<IInferenceBackend>b){std::lock_guard<std::mutex> l(p_->m);p_->be=std::move(b);}
InferenceResult AxManager::infer(const std::string&m,const float in[8]){InferenceResult r;if(!p_->run.load()){r.err="not running";return r;}if(!in){r.err="null";return r;}if(m.empty()||m.size()>64){r.err="bad name";return r;}for(char c:m)if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_'||c=='-')){r.err="charset";return r;}
std::shared_ptr<IInferenceBackend> be;{std::lock_guard<std::mutex> l(p_->m);be=p_->be;}float o;
if(be){std::string e;if(p_->loaded!=m){if(!be->load(m,e)){r.err=e;return r;}p_->loaded=m;}o=be->predict(in);}else{float s=0;for(int i=0;i<8;i++)s+=in[i]*(0.12f*(i+1));o=1.0f/(1.0f+expf(-s/8.0f));}
if(!std::isfinite(o)){r.err="nan";return r;}r.ok=true;r.conf=o;r.label=o>0.7f?"HIGH_LOAD":(o>0.4f?"BALANCED":"IDLE");return r;}
bool AxManager::infer_simple(const std::string&m,std::string&e){float d[8]={0};SystemStatus s=status();d[0]=s.cpu/100.0f;d[1]=s.totalRam?(1.0f-(float)s.freeRam/(float)s.totalRam):0.5f;d[2]=(float)s.temp/80000.0f;for(int i=3;i<8;i++)d[i]=0.1f*i;auto r=infer(m,d);e=r.err;return r.ok;}
void* axm_get(){return &AxManager::get();}
int axm_start(){return AxManager::get().start()?0:-1;}
void axm_shutdown(){AxManager::get().shutdown();}
static std::string gJ;const char* axm_status_json(){gJ=AxManager::get().status().to_json();return gJ.c_str();}
int axm_run_task(void(*fn)(void*),void*arg){if(!fn)return -1;try{AxManager::get().run_task([fn,arg]{fn(arg);});return 0;}catch(...){return -1;}}
}
