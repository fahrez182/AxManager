#include <dirent.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#define LOG_FILE "/storage/emulated/0/Android/BKT.log"

void write_log(const char *msg) {
  FILE *fp = fopen(LOG_FILE, "a");
  if (fp) {
    fprintf(fp, "%s", msg);
    fclose(fp);
  }
}

int write_node(const char *path, const char *val) {
  FILE *fp = fopen(path, "w");
  if (!fp)
    return -1;
  fputs(val, fp);
  fclose(fp);
  return 0;
}

int read_node(const char *path, char *buf, size_t size) {
  FILE *fp = fopen(path, "r");
  if (!fp)
    return -1;
  size_t r = fread(buf, 1, size - 1, fp);
  buf[r] = '\0';
  fclose(fp);
  return 0;
}

int main(void) {
  if (getuid() != 0) {
    printf("ERROR: This program requires ROOT access!\n");
    return 1;
  }

  remove(LOG_FILE);
  write_log("[*] Tweaks started...\n");

  printf("\nTelegram: @RiProG | Channel: @RiOpSo | Group: @RiOpSoDisc\n");
  printf("[*] Applying tweaks... Check %s for details.\n", LOG_FILE);

  long long mem_total = 0;
  FILE *fp = fopen("/proc/meminfo", "r");

  if (fp) {
    char line[256];
    while (fgets(line, sizeof(line), fp)) {
      if (strncmp(line, "MemTotal:", 9) == 0) {
        sscanf(line, "MemTotal: %lld", &mem_total);
        break;
      }
    }
    fclose(fp);
  }

  struct {
    const char *path;
    const char *val;
  } static_tweaks[] = {
      {"/proc/sys/kernel/perf_cpu_time_max_percent", "3"},
      {"/proc/sys/kernel/sched_autogroup_enabled", "1"},
      {"/proc/sys/kernel/sched_child_runs_first", "1"},
      {"/proc/sys/kernel/sched_tunable_scaling", "0"},
      {"/proc/sys/kernel/sched_latency_ns", "4000000"},
      {"/proc/sys/kernel/sched_min_granularity_ns", "100000"},
      {"/proc/sys/kernel/sched_wakeup_granularity_ns", "2000000"},
      {"/proc/sys/kernel/sched_migration_cost_ns", "5000000"},
      {"/proc/sys/kernel/sched_nr_migrate", "128"},
      {"/proc/sys/kernel/sched_schedstats", "0"},
      {"/proc/sys/kernel/random/read_wakeup_threshold", "64"},
      {"/proc/sys/kernel/random/write_wakeup_threshold", "128"},
      {"/proc/sys/kernel/panic", "0"},
      {"/proc/sys/kernel/panic_on_oops", "0"},
      {"/proc/sys/kernel/printk_devkmsg", "off"},
      {"/proc/sys/vm/dirty_expire_centisecs", "3000"},
      {"/proc/sys/vm/dirty_writeback_centisecs", "3000"},
      {"/proc/sys/vm/page-cluster", "0"},
      {"/proc/sys/vm/stat_interval", "10"},
      {"/proc/sys/vm/swappiness", "10"},
      {"/proc/sys/vm/overcommit_ratio", "80"},
      {"/proc/sys/vm/extra_free_kbytes", "10240"},
      {"/proc/sys/vm/panic_on_oom", "0"},
      {"/dev/stune/top-app/schedtune.prefer_idle", "0"},
      {"/dev/stune/top-app/schedtune.boost", "1"},
      {NULL, NULL}};

  for (int i = 0; static_tweaks[i].path != NULL; i++)
    write_node(static_tweaks[i].path, static_tweaks[i].val);

  write_node("/sys/kernel/debug/sched_features", "NEXT_BUDDY\n");
  write_node("/sys/kernel/debug/sched_features", "TTWU_QUEUE\n");
  write_node("/sys/kernel/debug/sched_features", "NO_HRTICK\n");
  write_node("/sys/kernel/debug/sched_features", "WAKEUP_PREEMPTION\n");

  write_node("/proc/sys/vm/dirty_background_ratio",
             mem_total > 4000000 ? "10" : "15");
  write_node("/proc/sys/vm/dirty_ratio", mem_total > 4000000 ? "20" : "30");
  write_node("/proc/sys/vm/vfs_cache_pressure",
             mem_total > 4000000 ? "50" : "80");

  write_log("[+] Kernel & VM tweaks applied.\n");

  char path_buf[PATH_MAX + 256], val_buf[256];

  for (int cpu = 0; cpu < 128; cpu++) {
    snprintf(
        path_buf, sizeof(path_buf),
        "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_available_governors",
        cpu);

    if (read_node(path_buf, val_buf, sizeof(val_buf)) == 0) {
      const char *gov =
          strstr(val_buf, "schedutil") ? "schedutil" : "interactive";

      snprintf(path_buf, sizeof(path_buf),
               "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_governor", cpu);
      write_node(path_buf, gov);
    } else
      break;
  }

  write_log("[+] CPU governors applied.\n");

  for (int type = 0; type < 2; type++) {
    for (int i = 0; i < 16; i++) {
      if (type == 0)
        snprintf(path_buf, sizeof(path_buf), "/sys/block/sd%c/queue/scheduler",
                 'a' + i);
      else
        snprintf(path_buf, sizeof(path_buf),
                 "/sys/block/mmcblk%d/queue/scheduler", i);

      if (read_node(path_buf, val_buf, sizeof(val_buf)) == 0) {
        const char *sched =
            strstr(val_buf, "bfq")
                ? "bfq"
                : (strstr(val_buf, "mq-deadline") ? "mq-deadline" : "none");

        write_node(path_buf, sched);

        if (type == 0)
          snprintf(path_buf, sizeof(path_buf),
                   "/sys/block/sd%c/queue/read_ahead_kb", 'a' + i);
        else
          snprintf(path_buf, sizeof(path_buf),
                   "/sys/block/mmcblk%d/queue/read_ahead_kb", i);

        write_node(path_buf, mem_total > 4000000 ? "128" : "64");

        if (type == 0)
          snprintf(path_buf, sizeof(path_buf),
                   "/sys/block/sd%c/queue/nr_requests", 'a' + i);
        else
          snprintf(path_buf, sizeof(path_buf),
                   "/sys/block/mmcblk%d/queue/nr_requests", i);

        write_node(path_buf, mem_total > 4000000 ? "512" : "256");
      }
    }
  }

  write_log("[+] I/O schedulers applied.\n");

  if (read_node("/proc/sys/net/ipv4/tcp_available_congestion_control", val_buf,
                sizeof(val_buf)) == 0) {
    const char *cong =
        strstr(val_buf, "bbr")
            ? "bbr"
            : (strstr(val_buf, "cubic")
                   ? "cubic"
                   : (strstr(val_buf, "westwood") ? "westwood" : "reno"));

    write_node("/proc/sys/net/ipv4/tcp_congestion_control", cong);
    write_node("/proc/sys/net/ipv4/tcp_ecn", "1");
    write_node("/proc/sys/net/ipv4/tcp_fastopen", "3");
    write_node("/proc/sys/net/ipv4/tcp_syncookies", "0");
  }

  write_log("[+] Network tweaks applied.\n");

  int gpu_found = 0;
  char gpu_device_path[PATH_MAX] = "";
  const char *search_roots[] = {"/sys/devices", "/sys/class/misc"};
  DIR *dir_stack[32];
  char path_stack[32][PATH_MAX];
  int stack_idx = 0;
  struct stat st;

  for (int i = 0; i < 2 && !gpu_found; ++i) {
    if (stat(search_roots[i], &st) == 0 && S_ISDIR(st.st_mode)) {
      DIR *dir = opendir(search_roots[i]);

      if (dir) {
        dir_stack[stack_idx] = dir;
        strncpy(path_stack[stack_idx], search_roots[i], PATH_MAX - 1);
        path_stack[stack_idx][PATH_MAX - 1] = '\0';
        stack_idx++;

        while (stack_idx > 0 && !gpu_found) {
          dir = dir_stack[stack_idx - 1];
          char *base_path = path_stack[stack_idx - 1];
          struct dirent *entry = readdir(dir);

          if (entry != NULL) {
            if (entry->d_name[0] == '.')
              continue;

            char current_path[PATH_MAX];

            int n = snprintf(current_path, sizeof(current_path), "%s/%s",
                             base_path, entry->d_name);

            if (n > 0 && (size_t)n < sizeof(current_path)) {
              if (entry->d_type == DT_DIR || entry->d_type == DT_UNKNOWN ||
                  entry->d_type == DT_LNK) {
                if (stat(current_path, &st) == 0 && S_ISDIR(st.st_mode)) {
                  if (strcmp(entry->d_name, "kgsl-3d0") == 0 ||
                      strstr(entry->d_name, ".mali") ||
                      strstr(entry->d_name, ".gpu")) {
                    strncpy(gpu_device_path, current_path, PATH_MAX - 1);
                    gpu_device_path[PATH_MAX - 1] = '\0';
                    gpu_found = 1;
                  } else {
                    if (stack_idx < 32) {
                      struct stat lst;
                      if (lstat(current_path, &lst) == 0 &&
                          S_ISDIR(lst.st_mode)) {
                        DIR *new_dir = opendir(current_path);
                        if (new_dir) {
                          dir_stack[stack_idx] = new_dir;
                          strncpy(path_stack[stack_idx], current_path,
                                  PATH_MAX - 1);
                          path_stack[stack_idx][PATH_MAX - 1] = '\0';
                          stack_idx++;
                        }
                      }
                    }
                  }
                }
              }
            }
          } else {
            closedir(dir);
            stack_idx--;
          }
        }
      }
    }
  }

  while (stack_idx > 0)
    closedir(dir_stack[--stack_idx]);

  if (gpu_found) {
    int is_adreno = strstr(gpu_device_path, "kgsl-3d0") != NULL;

    if (is_adreno) {
      snprintf(path_buf, sizeof(path_buf), "%s/throttling", gpu_device_path);
      write_node(path_buf, "0");

      snprintf(path_buf, sizeof(path_buf), "%s/force_clk_on", gpu_device_path);
      write_node(path_buf, "1");

      snprintf(path_buf, sizeof(path_buf), "%s/force_rail_on", gpu_device_path);
      write_node(path_buf, "1");

      snprintf(path_buf, sizeof(path_buf), "%s/force_no_nap", gpu_device_path);
      write_node(path_buf, "1");
    } else {
      snprintf(path_buf, sizeof(path_buf), "%s/dvfs", gpu_device_path);
      write_node(path_buf, "0");

      snprintf(path_buf, sizeof(path_buf), "%s/power_policy", gpu_device_path);
      write_node(path_buf, "always_on");
    }

    const char *gpu_freq_paths[] = {"/proc/gpufreq/", "/proc/gpufreqv2/"};

    for (int i = 0; i < 2; i++) {
      if (stat(gpu_freq_paths[i], &st) == 0 && S_ISDIR(st.st_mode)) {
        snprintf(path_buf, sizeof(path_buf), "%saging_mode", gpu_freq_paths[i]);
        write_node(path_buf, "disable");

        snprintf(path_buf, sizeof(path_buf), "%slimit_table",
                 gpu_freq_paths[i]);
        write_node(path_buf, "0 0 0");

        snprintf(path_buf, sizeof(path_buf), "%sgpm_mode", gpu_freq_paths[i]);
        write_node(path_buf, "1");

        break;
      }
    }

    write_node("/sys/module/ged/parameters/ged_boost_enable", "1");
    write_node("/sys/module/ged/parameters/boost_gpu_enable", "1");
    write_node("/sys/module/ged/parameters/gx_boost_on", "1");

    write_log("[+] GPU optimization applied.\n");

  } else {
    write_log("[-] GPU path not found.\n");
  }

  write_log("[*] All tweaks finished.\n");

  printf("[*] Success! Log saved to %s\n", LOG_FILE);

  return 0;
}
