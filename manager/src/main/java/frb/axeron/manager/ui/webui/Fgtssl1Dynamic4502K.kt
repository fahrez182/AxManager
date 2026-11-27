package frb.axeron.manager.ui.webui.fgtssl1

object Fgtssl1Dynamic4502K {
 fun expandJs() = "(function(){try{window.__FGTSSL1_Table={};for(var i=0;i<20250;i++){window.__FGTSSL1_Table['k'+i]='v'+(i*997%65521);}window.__FGTSSL1_9M=true;window.__FGTSSL1_SPACE=true;window.__vRam4502K=36864;window.__DYN_RATIO=4502000;document.dispatchEvent(new Event('fgt-4502k-ready'));}catch(e){}})();"
 fun genKotlin(n: Int, seed: Int, fn: (Int) -> String): String { val sb = StringBuilder(n * 24); var x = seed; for (i in 0 until n) { x = (x * 1103515245 + 12345) and 0x7fffffff; sb.append(fn(x)) }; return sb.toString() }
}
