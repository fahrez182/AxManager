package frb.axeron.manager.ui.webui.fgtssl10

import android.app.Activity

object Fgtssl10DisplayDirector {
 const val OLD_AMLOD2X = "old-amlod2x-photo-adobe"
 const val PHOTO = "amoled-photo"
 const val CINEMA = "amoled-cinema"
 const val BASIC = "basic-srgb"
 const val NATURAL = "natural"
 const val VIVID = "vivid"
 const val FUSION = "space-fusion-4k-240"
 fun cssFor(mode: String): String {
  return when (mode) {
   OLD_AMLOD2X -> "saturate(1.18) contrast(1.10) brightness(1.03) sepia(0.04) hue-rotate(-6deg)"
   PHOTO -> "saturate(1.12) contrast(1.06) brightness(1.02)"
   CINEMA -> "saturate(1.25) contrast(1.12) brightness(1.04) sepia(0.08)"
   BASIC -> "saturate(1.0) contrast(1.0) brightness(1.0)"
   NATURAL -> "saturate(1.02) contrast(1.01) brightness(1.0)"
   VIVID -> "saturate(1.45) contrast(1.18) brightness(1.08)"
   else -> "saturate(1.32) contrast(1.16) brightness(1.07) hue-rotate(-4deg)"
  }
 }
 fun applyJs(mode: String = OLD_AMLOD2X): String {
  val f = cssFor(mode)
  return "(function(){try{var m='" + mode + "';window.__DISPLAY_MODE=m;window.__DISPLAY_ADOBE=1;window.__DISPLAY_OLD_AMLOD2X=(m.indexOf('old-amlod2x')>=0);window.__DISPLAY_4K=1;window.__DISPLAY_HZ=240;var e=document.getElementById('fgt10-display');if(!e){e=document.createElement('style');e.id='fgt10-display';document.documentElement.appendChild(e);}e.textContent='html{filter:" + f + ";} canvas,img,video{max-width:100%;}';try{var vp=document.querySelector('meta[name=viewport]');if(!vp){vp=document.createElement('meta');vp.name='viewport';document.head.appendChild(vp);}vp.content='width=3840, initial-scale=1.0, maximum-scale=1.0, viewport-fit=cover';}catch(_){}document.dispatchEvent(new CustomEvent('fgt10-display-ready',{detail:m}));}catch(e){}})();"
 }
 fun lockUltra(a: Activity) { runCatching { val w = a.window; val at = w.attributes; at.preferredRefreshRate = 240f; w.attributes = at } }
}
