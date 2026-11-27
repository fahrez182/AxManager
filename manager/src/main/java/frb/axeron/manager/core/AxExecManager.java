package com.axdubai.manager.core;
import java.util.concurrent.*;
public class AxExecManager {
private final ExecutorService pool = Executors.newFixedThreadPool(4);
public Future<Long> scanAsync(final String path, final int th){
return pool.submit(new Callable<Long>(){
public Long call(){
try { return AxOmniBridge.scanNative(path, th); }
catch (UnsatisfiedLinkError e) { return -99L; }
}
});
}
public void shutdown(){ pool.shutdown(); }
}
