#include <iostream>
#include <vector>
#include <cstring>
#include <sys/syscall.h>
#include <unistd.h>

// مقادیر تعریف شده در Patch
#define KSU_UMOUNT_GETSIZE 107
#define KSU_UMOUNT_GETLIST 108

void fetch_mount_list() {
    size_t required_size = 0;

    // مرحله ۱: گرفتن اندازه مورد نیاز (Get Size)
    // ما از syscall استفاده می‌کنیم تا مستقیماً با هسته صحبت کنیم
    if (syscall(SYS_supercall, KSU_UMOUNT_GETSIZE, &required_size) < 0) {
        perror("Failed to get size");
        return;
    }

    std::cout << "Required buffer size: " << required_size << " bytes" << std::endl;

    if (required_size == 0) return;

    // مرحله ۲: تخصیص حافظه در سمت کاربر (User-space allocation)
    std::vector<char> buffer(required_size);

    // مرحله ۳: گرفتن لیست واقعی (Get List)
    if (syscall(SYS_supercall, KSU_UMOUNT_GETLIST, buffer.data()) < 0) {
        perror("Failed to get list");
        return;
    }

    // نمایش نتیجه
    std::cout << "--- Mount List Received ---" << std::endl;
    char* ptr = buffer.data();
    while (*ptr != '\0') {
        std::cout << "Found entry: " << ptr << std::endl;
        ptr += strlen(ptr) + 1; // رفتن به رشته بعدی
    }
}

int main() {
    fetch_mount_list();
    return 0;
}
