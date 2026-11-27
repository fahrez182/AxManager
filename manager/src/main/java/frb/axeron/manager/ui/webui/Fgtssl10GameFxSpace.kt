package frb.axeron.manager.ui.webui.fgtssl10

object Fgtssl10GameFxSpace {
 fun hudJs(mode: String = "old-amlod2x-photo-adobe"): String {
  return "(function(){if(document.getElementById('fgt10-hud')){document.getElementById('fgt10-hud').setAttribute('data-mode','" + mode + "');return;}var h=document.createElement('div');h.id='fgt10-hud';h.setAttribute('data-mode','" + mode + "');h.style.cssText='position:fixed;top:8px;right:8px;z-index:999999;font-size:10px;color:#fff;pointer-events:none;font-family:sans-serif;';function b(l,v,c){return '<div style=\"background:rgba(10,5,40,.78);border:1px solid '+c+';box-shadow:0 0 16px '+c+';border-radius:12px;padding:4px 8px;margin-bottom:6px;min-width:178px;\">'+l+' '+v+'%<div style=\"height:5px;background:#111;border-radius:4px;margin-top:3px;\"><div style=\"height:100%;width:'+v+'%;background:'+c+';border-radius:4px;\"></div></div></div>';}h.innerHTML=b('vCPU SPACE 540',92,'#00f0ff')+b('vGPU SPACE 240',97,'#ff2bd6')+b('vRAM 400x 256M',78,'#7CFF00')+b('SWAP 263x 4K240','100','#B388FF');document.documentElement.appendChild(h);})();"
 }
}
