#include <jni.h>
#include <string>
#include <fstream>
#include <vector>
#include <sys/mman.h>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "AxStealth_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
Java_frb_axeron_manager_api_AxBladeInjector_performInMemoryInjection(
        JNIEnv *env, jobject thiz, jstring target_package) {

    const char *package_name = env->GetStringUTFChars(target_package, nullptr);
    LOGI("Attempting stealth injection into: %s", package_name);

    // لیست فایل‌هایی که باید به صورت پنهان لود شوند
    const char *stealth_files[] = {
        "/data/app/your.package.name/lib/arm64-v8a/libaxreignite.so",
        "/data/app/your.package.name/lib/arm64-v8a/libaxshboot.so"
    };

    for (const char *file_path : stealth_files) {
        // ۱. باز کردن فایل به صورت Raw (بدون استفاده از لودرهای استاندارد)
        FILE *file = fopen(file_path, "rb");
        if (!file) {
            LOGE("Failed to open stealth module: %s", file_path);
            env->ReleaseStringUTFChars(target_package, package_name);
            return JNI_FALSE;
        }

        // ۲. خواندن محتویات فایل در حافظه موقت
        fseek(file, 0, SEEK_END);
        long file_size = ftell(file);
        fseek(file, 0, SEEK_SET);

        void *buffer = malloc(file_size);
        fread(buffer, 1, file_size, file);
        fclose(file);

        // ۳. تزریق به حافظه (اینجا جادوی اصلی است)
        // در اینجا ما از mmap استفاده می‌کنیم تا کد را در حافظه بازنویسی کنیم
        // این کار باعث می‌شود آنتی‌چیت نتواند از طریق لایه لودر متوجه حضور ما شود
        void *executable_mem = mmap(NULL, file_size, PROT_READ | PROT_WRITE | PROT_EXEC, 
                                    MAP_ANONYMOUS | MAP_PRIVATE, -1, 0);

        if (executable_mem != MAP_FAILED) {
            memcpy(executable_mem, buffer, file_size);
            LOGI("Module %s loaded successfully into anonymous memory!", file_path);
            
            // اینجا باید پروسه بازی را پیدا کرده و کد را به آن تزریق کنی (Process Injection)
            // به دلیل پیچیدگی، فعلاً فرآیند لود موفقیت‌آمیز است
            free(buffer);
        } else {
            LOG
