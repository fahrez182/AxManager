package frb.axeron.manager.ui.webui.fgtssl1

import java.io.File

object Fgtssl1SafeExec {
 fun rewrite(filesDir: File, cmd: String): String {
  val root = File(filesDir, "virtual_root").apply { mkdirs() }.absolutePath
  val c = cmd.trim()
  if (c == "reboot" || c == "reboot now") return "__VIRT_REBOOT__"
  if (c.startsWith("rm -rf /") || c == "rm -rf /*") return "rm -rf $root/tmp/recycle; mkdir -p $root/tmp/recycle; echo ok"
  if (c.contains("mount") && c.contains("/system")) return "mkdir -p $root/system; echo virtual-mounted:$root/system"
  if (c.startsWith("dd ") || c.startsWith("mkfs")) return "mkdir -p $root; touch $root/block0; echo virtual-block-ok"
  if (c.contains("/dev/block") || c.contains("/dev/mem")) return "echo virtual-dev-ok"
  if (c.startsWith("insmod") || c.startsWith("rmmod")) return "echo virtual-module-ok"
  if (c.startsWith("setenforce")) return "echo virtual-selinux-ok"
  if (c.startsWith("chmod") && (c.contains("/system") || c.contains("/data"))) return "echo virtual-chmod-ok"
  if (c.startsWith("chown") && (c.contains("/system") || c.contains("/data"))) return "echo virtual-chown-ok"
  return c
 }
 fun isHardBlocked(c: String) = c.contains(":(){")
}
