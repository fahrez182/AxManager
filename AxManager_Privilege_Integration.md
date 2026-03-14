# Panduan Integrasi Privilege Manager AxManager

Dokumen ini menjelaskan cara mengintegrasikan aplikasi Android Anda dengan AxManager agar dapat memanfaatkan hak akses (Privilege) layaknya aplikasi dengan akses *root* atau *ADB*, menggunakan Inter-Process Communication (IPC) melalui API yang disediakan oleh AxManager.

AxManager memungkinkan aplikasi klien (pihak ketiga) untuk mendapatkan akses ke layanannya, mengeksekusi perintah shell tingkat tinggi, dan mengakses layanan *system*, tanpa memerlukan *root* pada perangkat itu sendiri, selama AxManager berjalan dalam mode ADB atau Root. Mekanisme perizinannya dikontrol penuh oleh pengguna melalui antarmuka **Privilege Manager** yang terdapat di aplikasi AxManager.

## 1. Persiapan Awal

Untuk mengintegrasikan aplikasi Anda, Anda memerlukan dependensi dari API Axeron (AxManager). Jika Anda memiliki akses ke *library* `frb.axeron.api` (yang berisi antarmuka `Axeron.java` dan file AIDL yang dibutuhkan), masukkan ke dalam `build.gradle` atau integrasikan *module* API tersebut ke *project* aplikasi Android Anda.

API AxManager dirancang mirip dengan Shizuku API, jadi konsep dasarnya adalah aplikasi Anda akan mengikat diri (*bind*) ke *service* AxManager.

## 2. Mendeteksi dan Menghubungkan ke AxManager

AxManager menyiarkan layanan IPC (*Binder*) yang dapat ditangkap oleh aplikasi klien menggunakan *listener*. Kelas utama yang akan Anda gunakan adalah `frb.axeron.api.Axeron`.

Tambahkan *listener* untuk memantau kapan layanan AxManager tersedia:

```java
import frb.axeron.api.Axeron;

// Di dalam onCreate() dari Application atau Activity pertama Anda
Axeron.addBinderReceivedListenerSticky(new Axeron.OnBinderReceivedListener() {
    @Override
    public void onBinderReceived() {
        // AxManager Binder berhasil diterima!
        // Di sini Anda bisa mulai memeriksa status Izin Privilege.
        checkAxeronPrivilege();
    }
});

// Jangan lupa menghapus listener ketika komponen dihancurkan (onDestroy)
Axeron.removeBinderReceivedListener(this);
```

Untuk mendeteksi apakah AxManager sudah tidak terhubung (misal server mati):
```java
Axeron.addBinderDeadListener(new Axeron.OnBinderDeadListener() {
    @Override
    public void onBinderDead() {
        // AxManager terputus, disable fitur khusus
    }
});
```

## 3. Meminta Izin (Privilege) dari Pengguna

Jika layanan AxManager terdeteksi, langkah berikutnya adalah memverifikasi apakah aplikasi Anda sudah mendapatkan izin (Privilege) dari pengguna.
AxManager menangani permintaan izin mirip dengan cara `Shizuku` bekerja, yaitu mengecek *flag* dan jika belum disetujui, AxManager akan meminta pengguna.

### Mengecek Status Izin

Anda dapat menggunakan fungsi utilitas seperti ini untuk memeriksa status izin saat ini:

```java
public boolean isPrivilegeGranted() {
    if (Axeron.pingBinder()) {
        try {
            // Periksa izin ke server
            int checkResult = Axeron.checkRemotePermission("moe.shizuku.manager.permission.API_V23");
            return checkResult == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    return false;
}
```

*Catatan: Parameter pengecekan izin mungkin bervariasi bergantung pada konstanta izin yang didefinisikan oleh implementasi server AxManager. Biasanya server memanfaatkan infrastruktur izin serupa Shizuku.*

### Meminta Izin (Request Permission)

Jika izin belum diberikan, Anda perlu meminta kepada pengguna. Pengguna nanti akan dapat memberikan izin secara manual melalui aplikasi AxManager di menu **Privilege Manager**.
Untuk memicu dialog permintaan izin:

```java
import moe.shizuku.api.Shizuku;

public void requestPrivilege() {
    if (Axeron.pingBinder() && !isPrivilegeGranted()) {
        // Karena AxManager juga mengadopsi protokol Shizuku,
        // API meminta izin bisa dialihkan via Shizuku wrapper atau Axeron API.
        Shizuku.requestPermission(REQUEST_CODE);
    }
}
```
Ketika pengguna menyetujui di aplikasi AxManager, hasil persetujuan akan diterima melalui *listener*. Anda harus mengimplementasikan dan mendaftarkan `OnRequestPermissionResultListener`.

```java
private final Shizuku.OnRequestPermissionResultListener permissionListener =
    (requestCode, grantResult) -> {
        if (requestCode == REQUEST_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
            // Izin berhasil didapatkan! Lanjutkan inisialisasi fitur Privilege.
        }
    };

// Daftar dan Hapus listener sesuai siklus hidup Activity Anda
Shizuku.addRequestPermissionResultListener(permissionListener);
Shizuku.removeRequestPermissionResultListener(permissionListener);
```

### Penjelasan "Privilege Manager" Internal

Di belakang layar (di dalam server AxManager `AxeronService.kt`), AxManager menyimpan status izin (*flag*) setiap aplikasi (*uid*).
- Di **Privilege Manager** UI, pengguna bisa mengaktifkan atau menonaktifkan sakelar (Toggle).
- Ketika diaktifkan, manajer memanggil `Axeron.updateFlagsForUid(...)` yang mengatur `FLAG_ALLOWED` pada server, memungkinkan layanan Anda menjalankan perintah yang membutuhkan *privilege*.
- Jika pengguna menonaktifkan sakelar, `FLAG_DENIED` akan diatur, aplikasi Anda akan dihentikan paksa (*force-stop*), dan sesi IPC Anda akan dicabut untuk memastikan keamanan.

## 4. Menggunakan API Privilege

Setelah izin berhasil didapatkan (`isPrivilegeGranted() == true`), Anda bebas menggunakan metode-metode *Privilege* yang ditawarkan di dalam `Axeron.java`.

### Mengeksekusi Shell Command dengan Hak Akses AxManager
```java
// Melakukan eksekusi perintah shell menggunakan hak akses AxManager (Root / ADB)
AxeronNewProcess process = Axeron.newProcess("ls -la /data/local/tmp");
process.waitFor();
// Anda bisa membaca InputStream dari process ini
```

### Mengeksekusi Remote Binder/System Services
Aplikasi Anda dapat berinteraksi dengan layanan *system* (ActivityManager, PackageManager, dll.) secara langsung tanpa *reflection* *bypass* menggunakan *wrapper* Binder dari Axeron:
```java
// Contoh jika didukung oleh API Axeron
IBinder amBinder = AxeronBinderWrapper.getSystemService(Context.ACTIVITY_SERVICE);
// Anda kemudian dapat melakukan transaksi IPC langsung dengan amBinder
// dengan hak istimewa (privilege) AxManager.
```

## 5. Ringkasan Siklus Hidup Integrasi

1. Tambahkan dependensi `frb.axeron.api` (dan `moe.shizuku.api` jika diperlukan) ke *project* aplikasi Anda.
2. Saat aplikasi dimulai, daftarkan `Axeron.addBinderReceivedListenerSticky`.
3. Saat *Binder* diterima, cek izin dengan `isPrivilegeGranted()`.
4. Jika belum ada izin, panggil fungsi *request permission* dan minta pengguna membuka **Privilege Manager** di AxManager untuk mengizinkan aplikasi.
5. Setelah diizinkan, gunakan fungsi `Axeron.newProcess()` atau metode *service* sistem lainnya dengan hak istimewa tinggi secara aman melalui jalur IPC (*Binder*).

Jika Anda ingin menghindari *crash*, selalu periksa apakah `Axeron.pingBinder()` mengembalikan `true` sebelum memanggil fungsi API Axeron apapun.

---
**Catatan Penting:**
Mengingat aplikasi klien dan server (AxManager) berkomunikasi melalui IPC asinkron (AIDL/Binder), selalu tempatkan panggilan sinkron jangka panjang (*long-running synchronous calls*) di luar UI Thread, baik menggunakan `AsyncTask`, `Coroutines` (jika menggunakan Kotlin), atau utilitas asinkron lainnya.
