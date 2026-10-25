#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <ctime>
#include <cstring>
#include <libgen.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <cerrno>
#include <string>
#include <vector>
#include <termios.h>
#include "misc.h"
#include "selinux.h"
#include "cgroup.h"
#include "logging.h"

#ifdef DEBUG
#define JAVA_DEBUGGABLE
#endif

#define perrorf(...) fprintf(stderr, __VA_ARGS__)

#define EXIT_FATAL_SET_CLASSPATH 3
#define EXIT_FATAL_FORK 4
#define EXIT_FATAL_APP_PROCESS 5
#define EXIT_FATAL_UID 6
#define EXIT_FATAL_PM_PATH 7
#define EXIT_FATAL_KILL 9
#define EXIT_FATAL_BINDER_BLOCKED_BY_SELINUX 10

#define PACKAGE_NAME "frb.axeron.manager"
#define SERVER_NAME "axeron_server"
#define SERVER_CLASS_PATH "frb.axeron.server.AxeronService"

#if defined(__arm__)
#define ABI "arm"
#elif defined(__i386__)
#define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__aarch64__)
#define ABI "arm64"
#endif

// In AXERONDIR, written by the server at start. Must match START_ENV_FILE in AxeronService.kt.
#define START_ENV_FILE "server_env"

/** [path]'s whole content; empty when it can't be read. */
static std::string read_file(const char *path) {
    std::string data;
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd == -1) return data;
    char buf[4096];
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf))) > 0) data.append(buf, n);
    close(fd);
    return data;
}

/**
 * Run from inside AxManager (a relaunch, the manager's root shell, a plugin's script), the
 * starter has the env the server gives its children, the user's variables included: passed on,
 * it would become the new server's own and grow with every restart. Such an env carries AXERON
 * and AXERONDIR, where the server saved the env it was itself started with; that one replaces
 * it. A saved env from before the last boot is left alone (an APEX update can change
 * BOOTCLASSPATH), and so is one missing: the env is then passed on as it is.
 */
static void use_start_env() {
    if (getenv("AXERON") == nullptr) return;
    const char *dir = getenv("AXERONDIR");
    if (dir == nullptr || dir[0] == '\0') return;

    std::string data = read_file((std::string(dir) + "/" START_ENV_FILE).c_str());
    size_t header = data.find('\n');
    if (header == std::string::npos) {
        printf("warn: no saved start env in %s, passing this one on\n", dir);
        return;
    }
    std::string boot_id = read_file("/proc/sys/kernel/random/boot_id");
    while (!boot_id.empty() && isspace((unsigned char) boot_id.back())) boot_id.pop_back();
    if (boot_id.empty() || data.compare(0, header, boot_id) != 0) {
        printf("warn: saved start env is from another boot, passing this one on\n");
        return;
    }

    std::vector<std::string> env;
    size_t start = header + 1;
    while (start < data.size()) {
        size_t end = data.find('\0', start);
        if (end == std::string::npos) end = data.size();
        if (end > start) env.emplace_back(data, start, end - start);
        start = end + 1;
    }
    if (env.empty()) return;

    clearenv();
    for (const auto &entry : env) {
        size_t eq = entry.find('=');
        if (eq == 0 || eq == std::string::npos) continue;
        setenv(entry.substr(0, eq).c_str(), entry.c_str() + eq + 1, true);
    }
    printf("info: using the server's saved start env\n");
}

static void run_server(const char *dex_path, const char *main_class, const char *process_name) {
    if (setenv("CLASSPATH", dex_path, true)) {
        LOGE("can't set CLASSPATH\n");
        exit(EXIT_FATAL_SET_CLASSPATH);
    }

    // A plugin's process marker. Started from a plugin's script (one that restarts AxManager),
    // the server would inherit it and pass it to every process it starts, so stopping that
    // plugin would take all of them down. The server never belongs to a plugin.
    unsetenv("AXERON_PLUGIN");

#define ARG(v) char **v = nullptr; \
    char buf_##v[PATH_MAX]; \
    size_t v_size = 0; \
    uintptr_t v_current = 0;
#define ARG_PUSH(v, arg) v_size += sizeof(char *); \
if (v == nullptr) { \
    v = (char **) malloc(v_size); \
} else { \
    v = (char **) realloc(v, v_size);\
} \
v_current = (uintptr_t) v + v_size - sizeof(char *); \
*((char **) v_current) = arg ? strdup(arg) : nullptr;

#define ARG_END(v) ARG_PUSH(v, nullptr)

#define ARG_PUSH_FMT(v, fmt, ...) snprintf(buf_##v, PATH_MAX, fmt, __VA_ARGS__); \
    ARG_PUSH(v, buf_##v)

#ifdef JAVA_DEBUGGABLE
#define ARG_PUSH_DEBUG_ONLY(v, arg) ARG_PUSH(v, arg)
#define ARG_PUSH_DEBUG_VM_PARAMS(v) \
    if (android_get_device_api_level() >= 30) { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-XjdwpProvider:adbconnection"); \
        ARG_PUSH(v, "-XjdwpOptions:suspend=n,server=y"); \
    } else if (android_get_device_api_level() >= 28) { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-XjdwpProvider:internal"); \
        ARG_PUSH(v, "-XjdwpOptions:transport=dt_android_adb,suspend=n,server=y"); \
    } else { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-agentlib:jdwp=transport=dt_android_adb,suspend=n,server=y"); \
    }
#else
#define ARG_PUSH_DEBUG_VM_PARAMS(v)
#define ARG_PUSH_DEBUG_ONLY(v, arg)
#endif

    char lib_path[PATH_MAX]{0};
    snprintf(lib_path, PATH_MAX, "%s/lib/%s", dirname(dex_path), ABI);

    ARG(argv)
    ARG_PUSH(argv, "/system/bin/app_process")
    ARG_PUSH_FMT(argv, "-Djava.class.path=%s", dex_path)
    ARG_PUSH_FMT(argv, "-Daxeron.library.path=%s", lib_path)
    ARG_PUSH_DEBUG_VM_PARAMS(argv)
    ARG_PUSH(argv, "/system/bin")
    ARG_PUSH_FMT(argv, "--nice-name=%s", process_name)
    ARG_PUSH(argv, main_class)
    ARG_PUSH_DEBUG_ONLY(argv, "--debug")
    ARG_END(argv)

    LOGD("exec app_process");

    if (execvp((const char *) argv[0], argv)) {
        exit(EXIT_FATAL_APP_PROCESS);
    }
}

static void start_server(const char *path, const char *main_class, const char *process_name) {
    pid_t pid = fork();
    switch (pid) {
        case -1: {
            perrorf("fatal: can't fork\n");
            exit(EXIT_FATAL_FORK);
        }
        case 0: {
            LOGD("child");
            setsid();
            chdir("/");
            int fd = open("/dev/null", O_RDWR);
            if (fd != -1) {
                dup2(fd, STDIN_FILENO);
                dup2(fd, STDOUT_FILENO);
                dup2(fd, STDERR_FILENO);
                if (fd > 2) close(fd);
            }
            run_server(path, main_class, process_name);
        }
        default: {
            printf("info: axeron_server pid is %d\n", pid);
            printf("info: axeron_starter exit with 0\n");
            exit(EXIT_SUCCESS);
        }
    }
}

static int check_selinux(const char *s, const char *t, const char *c, const char *p) {
    int res = se::selinux_check_access(s, t, c, p, nullptr);
#ifndef DEBUG
    if (res != 0) {
#endif
    printf("info: selinux_check_access %s %s %s %s: %d\n", s, t, c, p, res);
    fflush(stdout);
#ifndef DEBUG
    }
#endif
    return res;
}

static int switch_cgroup() {
    int pid = getpid();
    if (cgroup::switch_cgroup("/acct", pid)) {
        printf("info: switch cgroup succeeded, cgroup in /acct\n");
        return 0;
    }
    if (cgroup::switch_cgroup("/dev/cg2_bpf", pid)) {
        printf("info: switch cgroup succeeded, cgroup in /dev/cg2_bpf\n");
        return 0;
    }
    if (cgroup::switch_cgroup("/sys/fs/cgroup", pid)) {
        printf("info: switch cgroup succeeded, cgroup in /sys/fs/cgroup\n");
        return 0;
    }
    char buf[PROP_VALUE_MAX + 1];
    if (__system_property_get("ro.config.per_app_memcg", buf) > 0 &&
        strncmp(buf, "false", 5) != 0) {
        if (cgroup::switch_cgroup("/dev/memcg/apps", pid)) {
            printf("info: switch cgroup succeeded, cgroup in /dev/memcg/apps\n");
            return 0;
        }
    }
    printf("warn: can't switch cgroup\n");
    fflush(stdout);
    return -1;
}

int main(int argc, char *argv[]) {
    std::string apk_path;
    for (int i = 0; i < argc; ++i) {
        if (strncmp(argv[i], "--apk=", 6) == 0) {
            apk_path = argv[i] + 6;
        }
    }

    uid_t uid = getuid();
    if (uid != 0 && uid != 2000) {
        perrorf("fatal: run AxManager from non root nor adb user (uid=%d).\n", uid);
        exit(EXIT_FATAL_UID);
    }

    se::init();

    if (uid == 0) {
        switch_cgroup();

        if (android_get_device_api_level() >= 29) {
            printf("info: switching mount namespace to init...\n");
            switch_mnt_ns(1);
        }
    }

    if (uid == 0) {
        char *context = nullptr;
        if (se::getcon(&context) == 0) {
            int res = 0;

            res |= check_selinux("u:r:untrusted_app:s0", context, "binder", "call");
            res |= check_selinux("u:r:untrusted_app:s0", context, "binder", "transfer");

            if (res != 0) {
                perrorf("fatal: the su you are using does not allow app (u:r:untrusted_app:s0) to connect to su (%s) with binder.\n",
                        context);
                exit(EXIT_FATAL_BINDER_BLOCKED_BY_SELINUX);
            }
            se::freecon(context);
        }
    }

    printf("info: starter begin\n");
    fflush(stdout);

    // kill old server
    printf("info: killing old process...\n");
    fflush(stdout);

    foreach_proc([](pid_t pid) {
        if (pid == getpid()) return;

        char name[1024];
        if (get_proc_name(pid, name, 1024) != 0) return;

        if (strcmp(SERVER_NAME, name) != 0)
            return;

        if (kill(pid, SIGKILL) == 0)
            printf("info: killed %d (%s)\n", pid, name);
        else if (errno == EPERM) {
            perrorf("fatal: can't kill %d, please try to stop existing AxManager from app first.\n",
                    pid);
            exit(EXIT_FATAL_KILL);
        } else {
            printf("warn: failed to kill %d (%s)\n", pid, name);
        }
    });

    if (access(apk_path.c_str(), R_OK) == 0) {
        printf("info: use apk path from argv\n");
        fflush(stdout);
    }

    if (apk_path.empty()) {
        auto f = popen("pm path " PACKAGE_NAME, "r");
        if (f) {
            char line[PATH_MAX]{0};
            fgets(line, PATH_MAX, f);
            trim(line);
            if (strstr(line, "package:") == line) {
                apk_path = line + strlen("package:");
            }
            pclose(f);
        }
    }

    if (apk_path.empty()) {
        perrorf("fatal: can't get path of manager\n");
        exit(EXIT_FATAL_PM_PATH);
    }

    printf("info: apk path is %s\n", apk_path.c_str());
    if (access(apk_path.c_str(), R_OK) != 0) {
        perrorf("fatal: can't access manager %s\n", apk_path.c_str());
        exit(EXIT_FATAL_PM_PATH);
    }

    // Before the fork: the server's child has its output on /dev/null.
    use_start_env();

    printf("info: starting server...\n");
    fflush(stdout);
    LOGD("start_server");
    start_server(apk_path.c_str(), SERVER_CLASS_PATH, SERVER_NAME);
}
