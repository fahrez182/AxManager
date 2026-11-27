#pragma once
#ifndef AXMANAGER_CORE_AX_ORCHESTRATOR_HPP
#define AXMANAGER_CORE_AX_ORCHESTRATOR_HPP
#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <cstdio>
#include <functional>
#include <future>
#include <map>
#include <mutex>
#include <queue>
#include <deque>
#include <array>
#include <string>
#include <thread>
#include <vector>
#include <chrono>
#include <stdexcept>
#include <sstream>
#include <memory>
#include <unordered_map>
#include <unistd.h>
#ifdef linux
#include <sched.h>
#include <pthread.h>
#endif
#if __has_include("axmanager/memory/core/nexus/AxCoreNexus.hpp")
#include "axmanager/memory/core/nexus/AxCoreNexus.hpp"
#define AXM_HAS_NEXUS 1
#endif
#if __has_include("axmanager/memory/core/AntiCheatShield.h")
#include "axmanager/memory/core/AntiCheatShield.h"
#define AXM_HAS_SHIELD 1
#endif
namespace axmanager::core {
static inline uint64_t axm_now_ms(){return (uint64_t)std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now().time_since_epoch()).count();}
static inline long axm_ncpu(){long n=::sysconf(_SC_NPROCESSORS_ONLN);return n<=0?4:n;}
static inline void axm_tname(const std::string& n){
#if defined(linux)&&!defined(APPLE)
::pthread_setname_np(::pthread_self(),n.substr(0,15).c_str());
#elif defined(APPLE)
::pthread_setname_np(n.substr(0,63).c_str());
#else
(void)n;
#endif
}
static inline void axm_pin(int c){#ifdef linux
if(c<0)return;long nc=axm_ncpu();if(c>=nc)return;cpu_set_t s;CPU_ZERO(&s);CPU_SET((unsigned)c,&s);::sched_setaffinity(0,sizeof(s),&s);
#else
(void)c;
#endif
}
static inline void axm_unpin(){#ifdef linux
long nc=axm_ncpu();cpu_set_t s;CPU_ZERO(&s);for(long i=0;i<nc&&i<128;++i)CPU_SET((unsigned)i,&s);::sched_setaffinity(0,sizeof(s),&s);
#endif
}
enum class OrchestratorState:int{UNCONFIGURED=0,CONFIGURED=1,RUNNING=2,PAUSED=3,STOPPING=4,STOPPED=5};
enum class ShutdownMode:int{Drain=0,Discard=1};
enum class QoS:int{Background=0,Utility=1,Initiated=2,Interactive=3};
enum class Backpressure:int{DropNewest=0,DropOldest=1,BlockCaller=2};
struct TaskOptions{QoS qos=QoS::Utility;uint64_t weight=100;int affinity_core=-1;uint32_t timeout_ms=0;uint32_t max_retries=0;uint32_t backoff_ms=50;uint64_t group=0;uint32_t deadline_ms=0;};
struct OrchestratorConfig{int workers=0;bool enable_nexus=true;bool enable_shield=false;size_t max_queue=2048;Backpressure backpressure=Backpressure::DropNewest;uint32_t starvation_ms=1500;uint32_t stuck_threshold_ms=5000;bool pin_workers=true;std::string worker_prefix="AxWrk";bool enable_burst=true;int thermal=0;};
struct OrchestratorMetrics{uint64_t accepted=0,completed=0,failed=0,cancelled=0,timeouts=0,retried=0,stuck=0,pending=0,running=0,late=0,dedup=0,dag=0,burst=0,brk=0,ratew=0;uint64_t avg_us=0,max_us=0;double tps=0;int thermal=0;uint64_t h[7]={0,0,0,0,0,0,0};
std::string to_json() const{std::ostringstream o;o<<"{\"a\":"<<accepted<<",\"c\":"<<completed<<",\"f\":"<<failed<<",\"x\":"<<cancelled<<",\"t\":"<<timeouts<<",\"r\":"<<retried<<",\"s\":"<<stuck<<",\"p\":"<<pending<<",\"n\":"<<running<<",\"late\":"<<late<<",\"dd\":"<<dedup<<",\"dag\":"<<dag<<",\"burst\":"<<burst<<",\"brk\":"<<brk<<",\"avg\":"<<avg_us<<",\"max\":"<<max_us<<",\"tps\":"<<tps<<",\"th\":"<<thermal<<",\"h\":["<<h[0]<<","<<h[1]<<","<<h[2]<<","<<h[3]<<","<<h[4]<<","<<h[5]<<","<<h[6]<<"]}";return o.str();}};
struct CancelToken{std::atomic<bool> c{false};void cancel(){c.store(true);}bool cancelled()const{return c.load();}};
struct CancellableHandle{std::future<void*> fut;std::shared_ptr<CancelToken> tok;void cancel(){if(tok)tok->cancel();}};
struct Observer{std::function<void(uint64_t,QoS)> on_start;std::function<void(uint64_t,uint64_t)> on_finish;std::function<void(uint64_t,const std::string&)> on_error;std::function<void(int)> on_stuck;};
struct DagNode{uint64_t id=0;std::function<void*()> fn;std::vector<uint64_t> after;TaskOptions opt;};
class AxLatch{public:explicit AxLatch(int n=0):c(n){}void reset(int n){std::lock_guard<std::mutex>l(m);c=n;}void down(){std::lock_guard<std::mutex>l(m);if(c>0&&--c==0)cv.notify_all();}void wait(){std::unique_lock<std::mutex>l(m);cv.wait(l,[this]{return c<=0;});}private:int c;std::mutex m;std::condition_variable cv;};
class AxOrchestrator final{
private:
struct Task{uint64_t id=0,seq=0,weight=100;TaskOptions opt;std::chrono::steady_clock::time_point enq{};std::chrono::steady_clock::time_point dl{};bool has_dl=false;std::function<void*()> fn;std::shared_ptr<std::promise<void*>> pr;std::shared_ptr<CancelToken> tk;};
struct DEnt{std::chrono::steady_clock::time_point w{};Task t;};
struct Per{std::atomic<bool> x{false};std::function<void*()> fn;uint32_t ms=0;uint32_t jit=0;TaskOptions opt;};
struct Bucket{double tk=0;uint64_t last=0;double qps=0,cap=0;};
struct Brk{int f=0;uint64_t ws=0,open=0;int th=5;uint32_t cool=2000;};
struct DD{std::shared_future<void*> f;uint64_t exp=0;};
static bool higher(const Task&a,const Task&b){if(a.has_dl!=b.has_dl)return a.has_dl>b.has_dl;if(a.has_dl&&b.has_dl&&a.dl!=b.dl)return a.dl<b.dl;if(a.weight!=b.weight)return a.weight>b.weight;return a.seq<b.seq;}
public:
static AxOrchestrator& get_instance(){static AxOrchestrator i;return i;}
static AxOrchestrator& get(){return get_instance();}
AxOrchestrator(const AxOrchestrator&)=delete;AxOrchestrator& operator=(const AxOrchestrator&)=delete;
static const char* version(){return "6.0.0-core-hyper";}
bool configure(const OrchestratorConfig& c){std::lock_guard<std::mutex>l(m_cfgx);auto s=m_st.load();if(s==OrchestratorState::RUNNING||s==OrchestratorState::PAUSED||s==OrchestratorState::STOPPING)return false;m_cfg=c;if(m_cfg.workers<=0){long hc=axm_ncpu();if(hc<=0)hc=4;m_cfg.workers=(int)hc;}if(m_cfg.workers<2)m_cfg.workers=2;if(m_cfg.workers>8)m_cfg.workers=8;if(m_cfg.max_queue<64)m_cfg.max_queue=64;if(m_cfg.max_queue>65536)m_cfg.max_queue=65536;if(m_cfg.thermal<0)m_cfg.thermal=0;if(m_cfg.thermal>3)m_cfg.thermal=3;m_th.store(m_cfg.thermal);m_st.store(OrchestratorState::CONFIGURED);return true;}
void boot(){(void)start();}
bool start(){if(m_st.load()==OrchestratorState::RUNNING)return true;if(m_st.load()==OrchestratorState::UNCONFIGURED){OrchestratorConfig d;configure(d);}m_stop.store(false);m_disc.store(false);m_pause.store(false);m_thr.store(false);m_watch_stop.store(false);
m_acc.store(0);m_done.store(0);m_fail.store(0);m_cx.store(0);m_to.store(0);m_re.store(0);m_stk.store(0);m_run.store(0);m_late.store(0);m_dd.store(0);m_dag.store(0);m_brk.store(0);m_ratew.store(0);m_burst_n.store(0);m_tot.store(0);m_max.store(0);for(auto&h:m_hist)h.store(0);
{std::lock_guard<std::mutex>l(m_mx);for(auto&q:m_lanes){std::deque<Task>e;q.swap(e);}m_del.clear();}
{std::lock_guard<std::mutex>l(m_pmx);for(auto&kv:m_per)kv.second->x.store(true);m_per.clear();}
#ifdef AXM_HAS_SHIELD
if(m_cfg.enable_shield){static ::AxManager::AntiCheatShield s;s.initialize();s.maskSystemIntegrity();s.hideMemoryHooks();}
#endif
#ifdef AXM_HAS_NEXUS
if(m_cfg.enable_nexus){std::string e;(void)::axmanager::nexus::AxCoreNexus::get_instance().boot_engine(e);std::lock_guard<std::mutex>l(m_cfgx);m_nerr=e;}
#endif
size_t n=(size_t)m_cfg.workers;m_nw=n;m_hb.reset(new std::atomic<uint64_t>[n]);m_ts.reset(new std::atomic<uint64_t>[n]);m_sr.reset(new std::atomic<uint64_t>[n]);for(size_t i=0;i<n;++i){m_hb[i].store(axm_now_ms());m_ts[i].store(0);m_sr[i].store(0);}
m_t0=std::chrono::steady_clock::now();m_workers.clear();m_workers.reserve(n);for(size_t i=0;i<n;++i)m_workers.emplace_back([this,i]{wloop((int)i);});m_watch=std::thread([this]{watch();});m_st.store(OrchestratorState::RUNNING);ev("start w="+std::to_string(n));return true;}
void shutdown(ShutdownMode mode=ShutdownMode::Drain){auto s=m_st.load();if(s==OrchestratorState::UNCONFIGURED||s==OrchestratorState::STOPPED)return;if(s==OrchestratorState::STOPPING){if(mode==ShutdownMode::Discard){m_disc.store(true);m_cv.notify_all();}return;}m_st.store(OrchestratorState::STOPPING);m_stop.store(true);m_disc.store(mode==ShutdownMode::Discard);m_pause.store(false);
{std::lock_guard<std::mutex>l(m_mx);if(mode==ShutdownMode::Drain){for(auto&kv:m_del){m_lanes[(size_t)kv.second.t.opt.qos].push_back(std::move(kv.second.t));}m_del.clear();}else{for(auto&q:m_lanes)while(!q.empty()){Task t=std::move(q.front());q.pop_front();try{t.pr->set_exception(std::make_exception_ptr(std::runtime_error("shutdown")));}catch(...){}m_cx.fetch_add(1);}for(auto&kv:m_del){try{kv.second.t.pr->set_exception(std::make_exception_ptr(std::runtime_error("shutdown")));}catch(...){}m_cx.fetch_add(1);}m_del.clear();}}
{std::lock_guard<std::mutex>l(m_pmx);for(auto&kv:m_per)kv.second->x.store(true);m_per.clear();}
m_cv.notify_all();for(auto&t:m_workers)if(t.joinable()&&t.get_id()!=std::this_thread::get_id())t.join();m_workers.clear();m_watch_stop.store(true);if(m_watch.joinable()&&m_watch.get_id()!=std::this_thread::get_id())m_watch.join();
{std::lock_guard<std::mutex>l(m_mx);for(auto&q:m_lanes)while(!q.empty()){Task t=std::move(q.front());q.pop_front();try{t.pr->set_exception(std::make_exception_ptr(std::runtime_error("shutdown")));}catch(...){}}for(auto&kv:m_del){try{kv.second.t.pr->set_exception(std::make_exception_ptr(std::runtime_error("shutdown")));}catch(...){}}m_del.clear();}
#ifdef AXM_HAS_NEXUS
if(m_cfg.enable_nexus)::axmanager::nexus::AxCoreNexus::get_instance().shutdown_engine();
#endif
m_st.store(OrchestratorState::STOPPED);ev("stop");}
bool is_running()const{auto s=m_st.load();return s==OrchestratorState::RUNNING||s==OrchestratorState::PAUSED;}
OrchestratorState state()const{return m_st.load();}
void pause(){if(m_st.load()==OrchestratorState::RUNNING){m_pause.store(true);m_st.store(OrchestratorState::PAUSED);}}
void resume(){if(m_st.load()==OrchestratorState::PAUSED){m_pause.store(false);m_st.store(OrchestratorState::RUNNING);m_cv.notify_all();}}
void set_throttled(bool v){m_thr.store(v);}
void set_thermal(int v){if(v<0)v=0;if(v>3)v=3;m_th.store(v);{std::lock_guard<std::mutex>l(m_cfgx);m_cfg.thermal=v;}}
std::future<void*> submit(std::function<void*()> fn,uint64_t w=100){TaskOptions o;o.weight=w;return submit_ex(o,std::move(fn));}
std::future<void*> submit_simple(std::function<void()> fn,TaskOptions o=TaskOptions{}){return submit_ex(o,f=std::move(fn)->void*{f();return nullptr;});}
std::future<void*> submit_ex(TaskOptions op,std::function<void*()> fn){auto pr=std::make_shared<std::promise<void*>>();auto fu=pr->get_future();if(!fn){pr->set_exception(std::make_exception_ptr(std::runtime_error("empty")));return fu;}if(!is_running()){pr->set_exception(std::make_exception_ptr(std::runtime_error("not running")));return fu;}if((int)op.qos<0||(int)op.qos>3)op.qos=QoS::Utility;
if(op.group&&brk_open(op.group)){pr->set_exception(std::make_exception_ptr(std::runtime_error("circuit open")));m_cx.fetch_add(1);return fu;}
Task t;t.id=m_id.fetch_add(1)+1;t.seq=m_seq.fetch_add(1);t.opt=op;t.enq=std::chrono::steady_clock::now();if(op.deadline_ms){t.has_dl=true;t.dl=t.enq+std::chrono::milliseconds(op.deadline_ms);}t.fn=std::move(fn);t.pr=pr;if(!enq_task(std::move(t))){}return fu;}
CancellableHandle submit_cancellable(TaskOptions op,std::function<void*(CancelToken&)> fn){CancellableHandle h;h.tok=std::make_shared<CancelToken>();auto pr=std::make_shared<std::promise<void*>>();h.fut=pr->get_future();if(!fn){pr->set_exception(std::make_exception_ptr(std::runtime_error("empty")));return h;}if(!is_running()){pr->set_exception(std::make_exception_ptr(std::runtime_error("not running")));return h;}auto tk=h.tok;std::function<void*()> w=fn=std::move(fn),tk->void*{return fn(*tk);};Task t;t.id=m_id.fetch_add(1)+1;t.seq=m_seq.fetch_add(1);t.opt=op;t.enq=std::chrono::steady_clock::now();if(op.deadline_ms){t.has_dl=true;t.dl=t.enq+std::chrono::milliseconds(op.deadline_ms);}t.fn=std::move(w);t.pr=pr;t.tk=tk;enq_task(std::move(t));return h;}
std::vector<std::future<void*>> submit_bulk(std::vector<std::function<void*()>> fns,TaskOptions o=TaskOptions{}){std::vector<std::future<void*>> r;r.reserve(fns.size());for(auto&f:fns)r.push_back(submit_ex(o,std::move(f)));return r;}
std::shared_future<void*> submit_unique(const std::string& key,std::function<void*()> fn,TaskOptions o=TaskOptions{},uint32_t ttl_ms=2000){if(key.empty()){auto f=submit_ex(o,std::move(fn));return f.share();}uint64_t now=axm_now_ms();{std::lock_guard<std::mutex>l(m_ex);for(auto it=m_ddm.begin();it!=m_ddm.end();){if(it->second.exp<=now)it=m_ddm.erase(it);else ++it;}auto it=m_ddm.find(key);if(it!=m_ddm.end()&&it->second.f.valid()){m_dd.fetch_add(1);return it->second.f;}}auto f=submit_ex(o,std::move(fn));auto sf=f.share();{std::lock_guard<std::mutex>l(m_ex);m_ddm[key]=DD{sf,now+ttl_ms};}return sf;}
std::future<void*> then(std::future<void*> prev,std::function<void*(void*)> nx,TaskOptions o=TaskOptions{}){auto pr=std::make_shared<std::promise<void*>>();auto fu=pr->get_future();std::thread(p=std::move(prev),nx=std::move(nx),o,pr,thismutable{try{void*r=p.get();auto f2=this->submit_ex(o,nx,r->void*{return nx(r);});try{void*r2=f2.get();pr->set_value(r2);}catch(...){pr->set_exception(std::current_exception());}}catch(...){try{pr->set_exception(std::current_exception());}catch(...){}}}).detach();return fu;}
std::future<void> submit_dag(std::vector<DagNode> nodes,TaskOptions cm=TaskOptions{}){auto pr=std::make_shared<std::promise<void>>();auto fu=pr->get_future();if(nodes.empty()){pr->set_value();return fu;}if(!is_running()){pr->set_exception(std::make_exception_ptr(std::runtime_error("not running")));return fu;}
struct DS{std::mutex m;std::unordered_map<uint64_t,DagNode> nd;std::unordered_map<uint64_t,std::vector<uint64_t>> ch;std::unordered_map<uint64_t,int> ind;std::atomic<size_t> left{0};std::atomic<int> bad{0};std::string err;std::shared_ptr<std::promise<void>> pr;};
auto st=std::make_shared<DS>();st->pr=pr;st->left.store(nodes.size());for(auto&n:nodes){st->nd[n.id]=n;if(!st->ind.count(n.id))st->ind[n.id]=0;}for(auto&n:nodes)for(auto d:n.after){st->ch[d].push_back(n.id);st->ind[n.id]++;if(!st->ind.count(d)&&!st->nd.count(d)){st->ind[d]=0;}}
std::function<void(uint64_t)> go;go=this,st,cm{auto it=st->nd.find(id);if(it==st->nd.end()){if(st->left.fetch_sub(1)==1){if(st->bad.load())st->pr->set_exception(std::make_exception_ptr(std::runtime_error(st->err.empty()?"dag fail":st->err)));else st->pr->set_value();}return;}DagNode nd=it->second;TaskOptions o=nd.opt;if(o.weight==100)o=cm;if((int)o.qos<0||(int)o.qos>3)o.qos=QoS::Utility;
this->submit_ex(o,st,id,go,this->void*{auto nit=st->nd.find(id);void* r=nullptr;try{if(nit!=st->nd.end()&&nit->second.fn)r=nit->second.fn();}catch(...){if(!st->bad.exchange(1)){std::lock_guard<std::mutex>l(st->m);st->err="dag node fail";}}auto cit=st->ch.find(id);std::vector<uint64_t> rdy;{std::lock_guard<std::mutex>l(st->m);if(cit!=st->ch.end())for(auto c:cit->second){auto k=st->ind.find(c);if(k!=st->ind.end()&&--(k->second)==0)rdy.push_back(c);}}for(auto c:rdy)go(c);if(st->left.fetch_sub(1)==1){if(st->bad.load()){try{st->pr->set_exception(std::make_exception_ptr(std::runtime_error(st->err)));}catch(...){}}else{try{st->pr->set_value();}catch(...){}}}return r;});};
std::vector<uint64_t> z;for(auto&kv:st->ind)if(kv.second==0&&st->nd.count(kv.first))z.push_back(kv.first);if(z.empty()&&!nodes.empty()){pr->set_exception(std::make_exception_ptr(std::runtime_error("dag cycle")));return fu;}m_dag.fetch_add(1);for(auto id:z)go(id);return fu;}
void dispatch_workload(uint64_t w,void*(*f)(void*),void* c){if(!f)return;TaskOptions o;o.weight=w;(void)submit_ex(o,f,c->void*{return f(c);});}
std::future<void*> dispatch_workload_future(uint64_t w,void*(*f)(void*),void* c){if(!f){std::promise<void*>p;p.set_exception(std::make_exception_ptr(std::runtime_error("null")));return p.get_future();}TaskOptions o;o.weight=w;return submit_ex(o,f,c->void*{return f(c);});}
uint64_t schedule_after(uint32_t ms,std::function<void*()> fn,TaskOptions o=TaskOptions{}){if(!fn||!is_running())return 0;Task t;t.id=m_id.fetch_add(1)+1;t.seq=m_seq.fetch_add(1);t.opt=o;t.enq=std::chrono::steady_clock::now();if(o.deadline_ms){t.has_dl=true;t.dl=t.enq+std::chrono::milliseconds(o.deadline_ms);}auto pr=std::make_shared<std::promise<void*>>();(void)pr->get_future();t.fn=std::move(fn);t.pr=pr;auto w=std::chrono::steady_clock::now()+std::chrono::milliseconds(ms);uint64_t id=t.id;{std::lock_guard<std::mutex>l(m_mx);m_del.emplace(id,DEnt{w,std::move(t)});m_acc.fetch_add(1);}m_cv.notify_all();return id;}
std::future<void*> schedule_after_future(uint32_t ms,std::function<void*()> fn,TaskOptions o=TaskOptions{}){std::promise<void*>p;if(!fn||!is_running()){p.set_exception(std::make_exception_ptr(std::runtime_error("bad")));return p.get_future();}auto pr=std::make_shared<std::promise<void*>>();auto fu=pr->get_future();Task t;t.id=m_id.fetch_add(1)+1;t.seq=m_seq.fetch_add(1);t.opt=o;t.enq=std::chrono::steady_clock::now();if(o.deadline_ms){t.has_dl=true;t.dl=t.enq+std::chrono::milliseconds(o.deadline_ms);}t.fn=std::move(fn);t.pr=pr;auto w=std::chrono::steady_clock::now()+std::chrono::milliseconds(ms);uint64_t id=t.id;{std::lock_guard<std::mutex>l(m_mx);m_del.emplace(id,DEnt{w,std::move(t)});m_acc.fetch_add(1);}m_cv.notify_all();return fu;}
uint64_t schedule_every(uint32_t iv,std::function<void*()> fn,TaskOptions o=TaskOptions{},uint32_t jit=0){if(!fn||!iv||!is_running())return 0;uint64_t id=m_id.fetch_add(1)+1;auto pe=std::make_shared<Per>();pe->fn=std::move(fn);pe->ms=iv;pe->jit=jit;pe->opt=o;{std::lock_guard<std::mutex>l(m_pmx);m_per.emplace(id,pe);}std::thread([this,id,pe]{uint64_t n=0;while(!pe->x.load()&&!m_stop.load()&&is_running()){uint32_t ex=pe->ms;if(pe->jit)ex+= (uint32_t)((axm_now_ms()+n*97+id)%(pe->jit+1));std::this_thread::sleep_for(std::chrono::milliseconds(ex));++n;if(pe->x.load()||m_stop.load()||!is_running())break;try{(void)submit_ex(pe->opt,pe->fn);}catch(...){}}}).detach();return id;}
uint64_t schedule_at(std::chrono::steady_clock::time_point tp,std::function<void*()> fn,TaskOptions o=TaskOptions{}){auto now=std::chrono::steady_clock::now();uint32_t ms=tp<=now?0:(uint32_t)std::chrono::duration_cast<std::chrono::milliseconds>(tp-now).count();return schedule_after(ms,std::move(fn),o);}
bool cancel(uint64_t id){std::vector<std::shared_ptr<std::promise<void*>>> F;{std::lock_guard<std::mutex>l(m_mx);auto it=m_del.find(id);if(it!=m_del.end()){F.push_back(it->second.t.pr);m_del.erase(it);m_cx.fetch_add(1);}else for(size_t q=0;q<4;++q){std::deque<Task> nq;bool f=false;while(!m_lanes[q].empty()){Task tt=std::move(m_lanes[q].front());m_lanes[q].pop_front();if(!f&&tt.id==id){F.push_back(tt.pr);f=true;continue;}nq.push_back(std::move(tt));}m_lanes[q].swap(nq);if(f){m_cx.fetch_add(1);break;}}}{std::lock_guard<std::mutex>l(m_pmx);auto it=m_per.find(id);if(it!=m_per.end()){it->second->x.store(true);m_per.erase(it);return true;}}for(auto&p:F)try{p->set_exception(std::make_exception_ptr(std::runtime_error("cancelled")));}catch(...){}return !F.empty();}
size_t cancel_group(uint64_t g){if(!g)return 0;size_t n=0;std::vector<std::shared_ptr<std::promise<void*>>> F;{std::lock_guard<std::mutex>l(m_mx);for(size_t q=0;q<4;++q){std::deque<Task> nq;while(!m_lanes[q].empty()){Task tt=std::move(m_lanes[q].front());m_lanes[q].pop_front();if(tt.opt.group==g){F.push_back(tt.pr);++n;continue;}nq.push_back(std::move(tt));}m_lanes[q].swap(nq);}for(auto it=m_del.begin();it!=m_del.end();){if(it->second.t.opt.group==g){F.push_back(it->second.t.pr);it=m_del.erase(it);++n;}else ++it;}m_cx.fetch_add(n);}for(auto&p:F)try{p->set_exception(std::make_exception_ptr(std::runtime_error("group cancelled")));}catch(...){}return n;}
void cancel_all(){std::vector<std::shared_ptr<std::promise<void*>>> F;{std::lock_guard<std::mutex>l(m_mx);for(auto&q:m_lanes)while(!q.empty()){F.push_back(q.front().pr);q.pop_front();}for(auto&kv:m_del)F.push_back(kv.second.t.pr);m_del.clear();m_cx.fetch_add(F.size());}{std::lock_guard<std::mutex>l(m_pmx);for(auto&kv:m_per)kv.second->x.store(true);m_per.clear();}for(auto&p:F)try{p->set_exception(std::make_exception_ptr(std::runtime_error("cancelled")));}catch(...){}}
void cancel_periodic(uint64_t id){(void)cancel(id);}
bool wait_for_empty(uint32_t to_ms){auto dl=std::chrono::steady_clock::now()+std::chrono::milliseconds(to_ms);while(std::chrono::steady_clock::now()<dl){{std::lock_guard<std::mutex>l(m_mx);bool e=true;for(auto&q:m_lanes)if(!q.empty()){e=false;break;}if(e&&m_del.empty()&&m_run.load()==0)return true;}std::this_thread::sleep_for(std::chrono::milliseconds(5));}{std::lock_guard<std::mutex>l(m_mx);bool e=true;for(auto&q:m_lanes)if(!q.empty())e=false;return e&&m_del.empty()&&m_run.load()==0;}}
static bool wait_for_all(std::vector<std::future<void*>>& v,uint32_t to_ms){auto dl=std::chrono::steady_clock::now()+std::chrono::milliseconds(to_ms);for(auto&f:v){auto now=std::chrono::steady_clock::now();if(now>=dl)return false;auto rem=std::chrono::duration_cast<std::chrono::milliseconds>(dl-now);if(f.wait_for(rem)==std::future_status::timeout)return false;}return true;}
std::vector<std::future<void*>> parallel_for(size_t n,std::function<void*(size_t)> fn,QoS q=QoS::Utility){std::vector<std::future<void*>> o;if(!n||!fn)return o;size_t ch=(size_t)m_cfg.workers;if(!ch)ch=4;if(ch>n)ch=n;size_t per=(n+ch-1)/ch;for(size_t c=0;c<ch;++c){size_t s=c*per,e=s+per;if(e>n)e=n;if(s>=e)break;TaskOptions op;op.qos=q;o.push_back(submit_ex(op,fn,s,e->void*{for(size_t i=s;i<e;++i)(void)fn(i);return nullptr;}));}return o;}
void set_rate_limit(uint64_t g,double qps,double burst=10){if(!g||qps<=0){std::lock_guard<std::mutex>l(m_ex);m_rates.erase(g);return;}std::lock_guard<std::mutex>l(m_ex);Bucket b;b.qps=qps;b.cap=burst>0?burst:qps;b.tk=b.cap;b.last=axm_now_ms();m_rates[g]=b;}
void set_breaker(uint64_t g,int th=5,uint32_t cool=2000){std::lock_guard<std::mutex>l(m_ex);if(th<=0){m_brks.erase(g);return;}Brk b;b.th=th;b.cool=cool;b.ws=axm_now_ms();m_brks[g]=b;}
void set_observer(Observer o){std::lock_guard<std::mutex>l(m_obx);m_obs=std::move(o);}
OrchestratorMetrics metrics()const{OrchestratorMetrics m;m.accepted=m_acc.load();m.completed=m_done.load();m.failed=m_fail.load();m.cancelled=m_cx.load();m.timeouts=m_to.load();m.retried=m_re.load();m.stuck=m_stk.load();m.running=m_run.load();m.late=m_late.load();m.dedup=m_dd.load();m.dag=m_dag.load();m.burst=m_burst_n.load();m.brk=m_brk.load();m.ratew=m_ratew.load();m.max_us=m_max.load();uint64_t c=m.completed;m.avg_us=c?(m_tot.load()/c):0;m.thermal=m_th.load();for(int i=0;i<7;++i)m.h[i]=m_hist[i].load();{std::lock_guard<std::mutex>l(m_mx);size_t p=0;for(auto&q:m_lanes)p+=q.size();p+=m_del.size();m.pending=p;}double up=std::chrono::duration<double>(std::chrono::steady_clock::now()-m_t0).count();m.tps=(up>0.01&&c)?((double)c/up):0;return m;}
std::string status_string()const{auto m=metrics();const char*st="IDLE";switch(m_st.load()){case OrchestratorState::RUNNING:st="ACTIVE";break;case OrchestratorState::PAUSED:st="PAUSED";break;case OrchestratorState::STOPPING:st="STOPPING";break;case OrchestratorState::CONFIGURED:st="READY";break;case OrchestratorState::STOPPED:st="STOPPED";break;default:break;}char b[512];snprintf(b,sizeof(b),"[ORCH] %s w=%d pend=%llu run=%llu done=%llu fail=%llu cx=%llu to=%llu re=%llu stk=%llu late=%llu brk=%llu avg=%lluus max=%lluus %.1f/s th=%d",st,m_cfg.workers,(unsigned long long)m.pending,(unsigned long long)m.running,(unsigned long long)m.completed,(unsigned long long)m.failed,(unsigned long long)m.cancelled,(unsigned long long)m.timeouts,(unsigned long long)m.retried,(unsigned long long)m.stuck,(unsigned long long)m.late,(unsigned long long)m.brk,(unsigned long long)m.avg_us,(unsigned long long)m.max_us,m.tps,m.thermal);return std::string(b);}
std::string status_json()const{return metrics().to_json();}
std::vector<std::string> dump_events()const{std::lock_guard<std::mutex>l(m_evx);std::vector<std::string> r;for(size_t i=0;i<m_evs.size();++i){size_t k=(m_evi+i)%m_evs.size();if(!m_evs[k].empty())r.push_back(m_evs[k]);}return r;}
std::string last_nexus_error()const{std::lock_guard<std::mutex>l(m_cfgx);return m_nerr;}
private:
AxOrchestrator():m_st(OrchestratorState::UNCONFIGURED){for(auto&h:m_hist)h.store(0);m_evs.resize(64);m_t0=std::chrono::steady_clock::now();}
~AxOrchestrator(){shutdown(ShutdownMode::Discard);}
bool allempty()const{for(auto&q:m_lanes)if(!q.empty())return false;return true;}
size_t total()const{size_t t=0;for(auto&q:m_lanes)t+=q.size();t+=m_del.size();return t;}
void promote(const std::chrono::steady_clock::time_point& n){for(auto it=m_del.begin();it!=m_del.end();){if(it->second.w<=n){m_lanes[(size_t)it->second.t.opt.qos].push_back(std::move(it->second.t));it=m_del.erase(it);}else ++it;}}
int picklane(const std::chrono::steady_clock::time_point& n){int th=m_th.load();auto allowed=th{if(th>=3)return q==3;if(th==2)return q>=2;return true;};uint32_t sv=m_cfg.starvation_ms;for(int q=0;q<=1;++q){if(!allowed(q))continue;if(!m_lanes[(size_t)q].empty()&&sv){auto w=std::chrono::duration_cast<std::chrono::milliseconds>(n-m_lanes[(size_t)q].front().enq).count();if(w>(long long)sv)return q;}}
int best=-1;uint64_t emax=0;bool hased=false;for(int q=0;q<4;++q){if(!allowed(q)||m_lanes[(size_t)q].empty())continue;for(auto&tt:m_lanes[(size_t)q]){if(tt.has_dl){uint64_t left=tt.dl>n? (uint64_t)std::chrono::duration_cast<std::chrono::milliseconds>(tt.dl-n).count():0;if(left<80){if(!hased||left<emax){hased=true;emax=left;best=q;}}}}}if(hased)return best;
for(int q=3;q>=0;--q)if(allowed(q)&&!m_lanes[(size_t)q].empty())return q;return 3;}
bool enq_task(Task&& t){size_t mx=m_cfg.max_queue;auto bp=m_cfg.backpressure;if(bp==Backpressure::BlockCaller){auto dl=std::chrono::steady_clock::now()+std::chrono::milliseconds(200);while(std::chrono::steady_clock::now()<dl){{std::lock_guard<std::mutex>l(m_mx);if(total()<mx)break;}if(m_stop.load())break;std::this_thread::sleep_for(std::chrono::milliseconds(2));}}{std::lock_guard<std::mutex>l(m_mx);if(total()>=mx){if(bp==Backpressure::DropOldest){for(size_t q=0;q<4;++q)if(!m_lanes[q].empty()){Task o=std::move(m_lanes[q].front());m_lanes[q].pop_front();try{o.pr->set_exception(std::make_exception_ptr(std::runtime_error("dropped oldest")));}catch(...){}m_cx.fetch_add(1);break;}}else{try{t.pr->set_exception(std::make_exception_ptr(std::runtime_error("queue full")));}catch(...){}m_cx.fetch_add(1);return false;}}m_lanes[(size_t)t.opt.qos].push_back(std::move(t));m_acc.fetch_add(1);}m_cv.notify_one();return true;}
bool trypop(Task& o){std::lock_guard<std::mutex>l(m_mx);auto n=std::chrono::steady_clock::now();promote(n);if(allempty())return false;int ln=picklane(n);if(m_lanes[(size_t)ln].empty()){for(int q=3;q>=0;--q)if(!m_lanes[(size_t)q].empty()){ln=q;break;}}auto&dq=m_lanes[(size_t)ln];size_t bi=0;for(size_t i=1;i<dq.size();++i)if(higher(dq[i],dq[bi]))bi=i;o=std::move(dq[bi]);dq.erase(dq.begin()+(long)bi);return true;}
bool brk_open(uint64_t g){if(!g)return false;std::lock_guard<std::mutex>l(m_ex);auto it=m_brks.find(g);if(it==m_brks.end())return false;uint64_t n=axm_now_ms();if(it->second.open&&n<it->second.open)return true;if(it->second.open&&n>=it->second.open){it->second.open=0;it->second.f=0;it->second.ws=n;}return false;}
bool rate_ok(uint64_t g){if(!g)return true;std::lock_guard<std::mutex>l(m_ex);auto it=m_rates.find(g);if(it==m_rates.end()||it->second.qps<=0)return true;uint64_t n=axm_now_ms();Bucket&b=it->second;double dt=(double)(n>=b.last?(n-b.last):0);b.tk+=dt*b.qps/1000.0;if(b.tk>b.cap)b.tk=b.cap;b.last=n;if(b.tk>=1.0){b.tk-=1.0;return true;}m_ratew.fetch_add(1);return false;}
void brk_note(uint64_t g,bool ok){if(!g)return;std::lock_guard<std::mutex>l(m_ex);auto it=m_brks.find(g);if(it==m_brks.end())return;uint64_t n=axm_now_ms();if(ok){it->second.f=0;it->second.ws=n;return;}if(n-it->second.ws>10000){it->second.f=0;it->second.ws=n;}if(++it->second.f>=it->second.th){it->second.open=n+it->second.cool;it->second.f=0;m_brk.fetch_add(1);ev("breaker open g="+std::to_string(g));}}
void ev(const std::string& s){std::lock_guard<std::mutex>l(m_evx);char b[160];snprintf(b,sizeof(b),"[%llu] %s",(unsigned long long)axm_now_ms(),s.c_str());m_evs[m_evi%m_evs.size()]=b;++m_evi;}
void hist(uint64_t us){int i=us<100?0:us<1000?1:us<5000?2:us<10000?3:us<50000?4:us<100000?5:6;m_hist[i].fetch_add(1);m_tot.fetch_add(us);uint64_t c=m_max.load();while(us>c&&!m_max.compare_exchange_weak(c,us)){}}
Observer obs(){std::lock_guard<std::mutex>l(m_obx);return m_obs;}
int auto_core(QoS q){long nc=axm_ncpu();switch(q){case QoS::Interactive:return (int)(nc-1);case QoS::Initiated:return nc>2?(int)(nc-2):0;default:return 0;}}
void runone(Task t,int idx){uint64_t tid=t.id;Observer ob=obs();if(t.tk&&t.tk->cancelled()){try{t.pr->set_exception(std::make_exception_ptr(std::runtime_error("cancelled")));}catch(...){}m_cx.fetch_add(1);m_run.fetch_sub(1);if(m_nw&&(size_t)idx<m_nw)m_ts[(size_t)idx].store(0);return;}
if(t.has_dl&&std::chrono::steady_clock::now()>t.dl){try{t.pr->set_exception(std::make_exception_ptr(std::runtime_error("deadline")));}catch(...){}m_to.fetch_add(1);m_late.fetch_add(1);m_run.fetch_sub(1);if(m_nw&&(size_t)idx<m_nw)m_ts[(size_t)idx].store(0);return;}
if(t.opt.timeout_ms){auto w=std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now()-t.enq).count();if(w>(long long)t.opt.timeout_ms){try{t.pr->set_exception(std::make_exception_ptr(std::runtime_error("queue timeout")));}catch(...){}m_to.fetch_add(1);m_run.fetch_sub(1);if(m_nw&&(size_t)idx<m_nw)m_ts[(size_t)idx].store(0);return;}}
if(t.opt.group&&brk_open(t.opt.group)){try{t.pr->set_exception(std::make_exception_ptr(std::runtime_error("circuit open")));}catch(...){}m_cx.fetch_add(1);m_run.fetch_sub(1);if(m_nw&&(size_t)idx<m_nw)m_ts[(size_t)idx].store(0);return;}
int tries=0;while(t.opt.group&&!rate_ok(t.opt.group)&&tries<3){std::this_thread::sleep_for(std::chrono::milliseconds(5));++tries;}if(t.opt.group&&!rate_ok(t.opt.group)){enq_task(std::move(t));m_run.fetch_sub(1);if(m_nw&&(size_t)idx<m_nw)m_ts[(size_t)idx].store(0);return;}
if(ob.on_start)try{ob.on_start(tid,t.opt.qos);}catch(...){}
int th=m_th.load();if(th>=1&&t.opt.qos==QoS::Background)std::this_thread::sleep_for(std::chrono::milliseconds(5));else if(m_thr.load()&&t.opt.qos<=QoS::Utility)std::this_thread::sleep_for(std::chrono::milliseconds(2));
if(t.opt.affinity_core==-2)axm_pin(auto_core(t.opt.qos));else if(t.opt.affinity_core>=0)axm_pin(t.opt.affinity_core);
auto ts=std::chrono::steady_clock::now();uint32_t at=0;for(;;){if(t.tk&&t.tk->cancelled()){try{t.pr->set_exception(std::make_exception_ptr(std::runtime_error("cancelled")));}catch(...){}m_cx.fetch_add(1);break;}try{void*r=t.fn?t.fn():nullptr;auto te=std::chrono::steady_clock::now();uint64_t lat=(uint64_t)std::chrono::duration_cast<std::chrono::microseconds>(te-ts).count();hist(lat);try{t.pr->set_value(r);}catch(...){}m_done.fetch_add(1);brk_note(t.opt.group,true);if(ob.on_finish)try{ob.on_finish(tid,lat);}catch(...){}break;}catch(const std::exception&e){std::string w=e.what();if(at<t.opt.max_retries&&!m_stop.load()&&!(t.tk&&t.tk->cancelled())){++at;m_re.fetch_add(1);uint32_t bk=t.opt.backoff_ms*(1u<<(at>4?4:at))+(uint32_t)(tid%7);std::this_thread::sleep_for(std::chrono::milliseconds(bk));continue;}try{t.pr->set_exception(std::current_exception());}catch(...){}m_fail.fetch_add(1);brk_note(t.opt.group,false);if(ob.on_error)try{ob.on_error(tid,w);}catch(...){}break;}catch(...){if(at<t.opt.max_retries&&!m_stop.load()){++at;m_re.fetch_add(1);std::this_thread::sleep_for(std::chrono::milliseconds(t.opt.backoff_ms));continue;}try{t.pr->set_exception(std::current_exception());}catch(...){}m_fail.fetch_add(1);brk_note(t.opt.group,false);if(ob.on_error)try{ob.on_error(tid,"unknown");}catch(...){}break;}}
if(t.opt.affinity_core>=0||t.opt.affinity_core==-2){if(m_cfg.pin_workers&&m_nw)axm_pin(idx%(int)axm_ncpu());else axm_unpin();}m_run.fetch_sub(1);if(m_nw&&(size_t)idx<m_nw){m_ts[(size_t)idx].store(0);m_hb[(size_t)idx].store(axm_now_ms());}m_cv.notify_all();}
void wloop(int ix){std::string nm=m_cfg.worker_prefix+"-"+std::to_string(ix);axm_tname(nm);if(m_cfg.pin_workers)axm_pin(ix%(int)axm_ncpu());if(m_nw)m_hb[(size_t)ix].store(axm_now_ms());for(;;){Task c;bool h=false;{std::unique_lock<std::mutex>l(m_mx);m_cv.wait_for(l,std::chrono::milliseconds(5),[this]{return m_stop.load()||!allempty()||!m_del.empty();});if(m_nw)m_hb[(size_t)ix].store(axm_now_ms());if(m_stop.load()&&m_disc.load())return;if(m_pause.load()&&!m_stop.load())continue;auto n=std::chrono::steady_clock::now();promote(n);if(!allempty()){int ln=picklane(n);if(m_lanes[(size_t)ln].empty()){for(int q=3;q>=0;--q)if(!m_lanes[(size_t)q].empty()){ln=q;break;}}auto&dq=m_lanes[(size_t)ln];size_t bi=0;for(size_t i=1;i<dq.size();++i)if(higher(dq[i],dq[bi]))bi=i;c=std::move(dq[bi]);dq.erase(dq.begin()+(long)bi);h=true;m_run.fetch_add(1);if(m_nw)m_ts[(size_t)ix].store(axm_now_ms());}else{if(m_stop.load())return;continue;}}if(h)runone(std::move(c),ix);}}
void burst_one(){Task c;if(!trypop(c))return;m_run.fetch_add(1);runone(std::move(c),9999);}
void burst_drain(){int k=0;while(!m_stop.load()&&k<64){Task c;if(!trypop(c))break;m_run.fetch_add(1);runone(std::move(c),9999);++k;}}
void watch(){axm_tname("AxWatchdog");uint64_t lastb=0;while(!m_watch_stop.load()){std::this_thread::sleep_for(std::chrono::milliseconds(250));if(!is_running()||!m_nw)continue;uint32_t th=m_cfg.stuck_threshold_ms;uint64_t now=axm_now_ms();if(th)for(size_t i=0;i<m_nw;++i){uint64_t s=m_ts[i].load();if(!s||!m_run.load())continue;if(now>s&&now-s>th){uint64_t lr=m_sr[i].load();if(now-lr>th){m_sr[i].store(now);m_stk.fetch_add(1);Observer o=obs();if(o.on_stuck)try{o.on_stuck((int)i);}catch(...){}ev("stuck w="+std::to_string(i));if(m_cfg.enable_burst&&now-lastb>2000){lastb=now;m_burst_n.fetch_add(1);std::thread([this]{burst_drain();}).detach();}}}}
if(m_cfg.enable_burst){size_t p=0;{std::lock_guard<std::mutex>l(m_mx);for(auto&q:m_lanes)p+=q.size();}if(p>m_nw*64&&now-lastb>1500){lastb=now;m_burst_n.fetch_add(1);ev("burst p="+std::to_string(p));for(int b=0;b<2;++b)std::thread([this]{burst_drain();}).detach();}}}}
void scheduler_loop(){}
OrchestratorConfig m_cfg;mutable std::mutex m_cfgx;std::string m_nerr;std::atomic<OrchestratorState> m_st;std::atomic<bool> m_stop{false},m_disc{false},m_pause{false},m_thr{false};std::atomic<int> m_th{0};
std::atomic<uint64_t> m_seq{0},m_id{0},m_acc{0},m_done{0},m_fail{0},m_cx{0},m_to{0},m_re{0},m_stk{0},m_run{0},m_late{0},m_dd{0},m_dag{0},m_burst_n{0},m_brk{0},m_ratew{0},m_tot{0},m_max{0};
std::chrono::steady_clock::time_point m_t0{std::chrono::steady_clock::now()};
mutable std::mutex m_mx;std::condition_variable m_cv;std::array<std::deque<Task>,4> m_lanes;std::map<uint64_t,DEnt> m_del;std::vector<std::thread> m_workers;size_t m_nw=0;
std::unique_ptr<std::atomic<uint64_t>[]> m_hb,m_ts,m_sr;std::thread m_watch;std::atomic<bool> m_watch_stop{false};
Observer m_obs;std::mutex m_obx;std::map<uint64_t,std::shared_ptr<Per>> m_per;std::mutex m_pmx;
std::unordered_map<uint64_t,Bucket> m_rates;std::unordered_map<uint64_t,Brk> m_brks;std::unordered_map<std::string,DD> m_ddm;std::mutex m_ex;
std::array<std::atomic<uint64_t>,7> m_hist;std::vector<std::string> m_evs;size_t m_evi=0;mutable std::mutex m_evx;std::atomic<bool> m_burst_on{false};
};
} // namespace
extern "C"{
inline void* axcore_get(){return (void*)&axmanager::core::AxOrchestrator::get_instance();}
inline int axcore_start(){return axmanager::core::AxOrchestrator::get_instance().start()?0:-1;}
inline void axcore_shutdown(){axmanager::core::AxOrchestrator::get_instance().shutdown(axmanager::core::ShutdownMode::Drain);}
inline void axcore_pause(){axmanager::core::AxOrchestrator::get_instance().pause();}
inline void axcore_resume(){axmanager::core::AxOrchestrator::get_instance().resume();}
inline void axcore_thermal(int v){axmanager::core::AxOrchestrator::get_instance().set_thermal(v);}
}
#endif
