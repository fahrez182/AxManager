package frb.axeron.manager.ui.webui.fgtssl10

object Fgtssl10VirtualGpu {
 fun turboJs() = "(function(){try{var c=document.createElement('canvas');var gl=c.getContext('webgl2',{antialias:true,powerPreference:'high-performance',desynchronized:true})||c.getContext('webgl');window.__vGPU=!!gl;window.__vGPU240=!!gl;window.__SPACE_GPU=!!gl;window.__FGT10_GPU=!!gl;if(gl){try{var e=gl.getExtension('WEBGL_debug_renderer_info');if(e)window.__gpu=gl.getParameter(e.UNMASKED_RENDERER_WEBGL);}catch(_){}}document.dispatchEvent(new Event('fgt10-gpu'));}catch(e){window.__vGPU=false;}})();"
}
