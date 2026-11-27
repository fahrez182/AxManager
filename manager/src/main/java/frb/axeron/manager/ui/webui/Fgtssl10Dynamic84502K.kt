package frb.axeron.manager.ui.webui.fgtssl10

object Fgtssl10Dynamic84502K {
 fun expandJs() = "(function(){try{window.__FGT10_Table={};for(var i=0;i<40500;i++){window.__FGT10_Table['k'+i]='v'+(i*997%65521);}window.__FGT10_84502K=true;window.__FGT10_SPACE=true;window.__vRam84502K=737280;window.__DYN_RATIO=84502000;document.dispatchEvent(new Event('fgt10-84502k-ready'));}catch(e){}})();"
 fun genKotlin(n: Int, seed: Int, fn: (Int) -> String): String { val sb = StringBuilder(n * 24); var x = seed; for (i in 0 until n) { x = (x * 1103515245 + 12345) and 0x7fffffff; sb.append(fn(x)) }; return sb.toString() }
}
