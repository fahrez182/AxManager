import os
import shutil

# لیست فایل‌های مشکوک و نام‌های جایگزین (Stealth Map)
STEALTH_MAP = {
    "libaxreignite.so": "libhwui_ext.so",
    "libaxshboot.so": "libvendor_audio.so",
    "libcodmmenu.so": "libart_helper.so",
    "libpayload.so": "libnative_utils.so"
}

def run_auto_stealth():
    print("--- 🛡️ AxManager Stealth Automator Started ---")
    found_count = 0
    
    # پیمایش تمام پوشه‌های پروژه
    for root, dirs, files in os.walk("."):
        for file in files:
            if file in STEALTH_MAP:
                old_path = os.path.join(root, file)
                new_name = STEALTH_MAP[file]
                new_path = os.path.join(root, new_name)
                
                try:
                    os.rename(old_path, new_path)
                    print(f"[✅] RENAMED: {file}  ==>  {new_name}")
                    print(f"     Location: {root}")
                    found_count += 1
                except Exception as e:
                    print(f"[❌] ERROR renaming {file}: {e}")

    if found_count == 0:
        print("\n[!] No suspicious files found. Did you Build the project first?")
    else:
        print(f"\n--- ✅ Success! {found_count} files were masked. ---")
        print("Next Step: Update your AxBladeInjector.kt with the new names and REBUILD.")

if __name__ == "__main__":
    run_auto_stealth()
