# Eksperimen Penjadwalan CloudSim Plus

Aplikasi ini membandingkan penjadwalan cloudlet dengan **Baseline**,
**Simulated Annealing (SA)**, dan **SA + OPT-2** menggunakan dataset GoCJ dan
NASA Ames. Aplikasi berjalan melalui wizard terminal dan menjalankan hasil
penjadwalan di CloudSim Plus.

## Prasyarat

- JDK 25 (Temurin/OpenJDK); JRE saja tidak cukup untuk kompilasi.
- Apache Maven 3.10 atau versi kompatibel yang lebih baru.
- Koneksi internet saat build pertama untuk mengunduh dependensi Maven,
  termasuk CloudSim Plus 8.0.0.

### Instalasi pada Windows

1. Pasang JDK 25 dari [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=25)
   dengan installer Windows x64.
2. Unduh distribusi binary ZIP Maven dari
   [Apache Maven](https://maven.apache.org/download.cgi), lalu ekstrak, misalnya
   ke `C:\apache-maven-3.10.0`.
3. Buka **Environment Variables** pada Windows dan atur:
   - `JAVA_HOME` ke folder JDK, misalnya
     `C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot`.
   - Tambahkan `%JAVA_HOME%\bin` dan
     `C:\apache-maven-3.10.0\bin` ke `Path`.
4. Buka terminal baru (atau restart VS Code), lalu verifikasi:

   ```powershell
   java -version
   mvn -version
   ```

   Pastikan bagian `Java version` pada keluaran `mvn -version` menunjukkan JDK
   yang diinginkan. Maven menggunakan `JAVA_HOME`, jadi memeriksa `java -version`
   saja belum memastikan runtime Maven sudah benar.

CloudSim Plus tidak perlu dipasang secara terpisah. Dependensinya sudah
dideklarasikan di `pom.xml` dan akan diunduh oleh Maven.
Kode dikompilasi dengan target Java 17 agar kompatibel dengan Java 17 ke atas;
JDK 25 dapat dipakai sebagai runtime Maven.

## Build, test, dan menjalankan wizard

Jalankan perintah dari folder `demo`:

```powershell
cd C:\projects\SA_OPT2\demo
mvn clean test
mvn clean compile
mvn org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=com.example.App"
```

Perintah terakhir membuka wizard interaktif. Pilih nomor skenario, algoritma,
dan seed sesuai prompt. Wizard mengulang prompt jika input di luar pilihan yang
valid. Gunakan terminal interaktif, bukan terminal yang menutup input standar.

## Pilihan skenario

| Pilihan | Skenario | Task/job | VM | Dataset |
| ---: | --- | ---: | ---: | --- |
| 1 | GoCJ 500 Task x 50 VM | 500 | 50 | GoCJ 500 |
| 2 | GoCJ 1000 Task x 50 VM | 1.000 | 50 | GoCJ 1000 |
| 3 | GoCJ 750 Task x 10 VM | 750 | 10 | GoCJ 750 |
| 4 | NASA Ames 750 Job x 50 VM | 750 | 50 | 750 job valid pertama dari NASA SWF |

Pilih algoritma:

1. **Baseline** — menyerahkan pemetaan cloudlet ke broker CloudSim Plus.
2. **Simulated Annealing** — mencari pemetaan cloudlet ke VM menggunakan SA.
3. **Simulated Annealing + OPT-2** — menjalankan SA, lalu menyempurnakan
   pemetaannya dengan local search pertukaran pasangan.

Seed yang diminta adalah bilangan **1–10**. Seed yang sama menghasilkan
urutan acak SA yang sama. Baseline bersifat deterministik sehingga seed tidak
mengubah hasilnya.

## Cara kerja SA + OPT-2

### Representasi solusi dan fungsi objektif

Solusi berupa satu assignment untuk setiap cloudlet: setiap cloudlet dipetakan
ke indeks salah satu VM. Panjang cloudlet dinyatakan dalam MI. SA dan OPT-2
menggunakan estimasi makespan berikut:

```text
estimasi VM v = max(
    total MI pada v / (MIPS v x jumlah PE v),
    cloudlet terpanjang pada v / MIPS v
)
fitness = nilai estimasi VM terbesar
```

Ini adalah fungsi estimasi untuk mencari pemetaan. Setelah pemetaan diterapkan,
CloudSim Plus menjalankan simulasi dan menghitung makespan aktualnya.

### Tahap 1 — Simulated Annealing

1. SA membuat assignment awal secara acak.
2. Pada setiap langkah, SA memilih satu cloudlet dan mengusulkan VM secara
   acak sebagai tetangganya.
3. Assignment yang menurunkan fitness diterima. Assignment yang lebih buruk
   masih dapat diterima dengan probabilitas `exp(-delta / suhu)`, agar pencarian
   tidak langsung terjebak pada optimum lokal.
4. Suhu diturunkan setiap iterasi luar. Parameter saat ini: suhu awal `100`,
   suhu minimum `0.001`, laju pendinginan `0.95`, dan `100` percobaan per suhu.
5. SA mengembalikan assignment terbaik yang ditemukan. Generator acaknya memakai
   seed dari wizard.

### Tahap 2 — local search OPT-2

OPT-2 memulai dari solusi terbaik SA. Untuk setiap pasangan cloudlet yang
berada pada VM berbeda, algoritma menukar assignment VM keduanya dan
menghitung fitness hasil pertukaran. Pada setiap putaran, algoritma memilih
pertukaran terbaik yang memperbaiki fitness, lalu mengulang pencarian.
Proses berhenti ketika tidak ada pertukaran pasangan yang dapat memperbaiki
solusi.

Pada aplikasi penjadwalan ini, OPT-2 berarti **pertukaran assignment dua
cloudlet**; bukan pembalikan segmen rute 2-opt klasik. Pemetaan hasil akhir
SA + OPT-2 lalu diberikan kepada broker untuk simulasi CloudSim Plus.

## Infrastruktur simulasi

- VM dibuat berulang dalam urutan tipe berikut:

  | Tipe | MIPS per core | PE |
  | --- | ---: | ---: |
  | V1 | 500 | 1 |
  | V2 | 800 | 1 |
  | V3 | 1.000 | 2 |
  | V4 | 1.300 | 2 |
  | V5 | 1.600 | 3 |

- Datacenter memiliki 20 host, masing-masing 6 PE; 10 host memakai 2.000 MIPS
  per PE dan 10 host memakai 2.500 MIPS per PE.
- Cloudlet memakai satu PE dan scheduler cloudlet VM memakai
  `CloudletSchedulerSpaceShared`.

## Dataset dan hasil

Nilai GoCJ dibaca sebagai panjang cloudlet dalam MI. Untuk NASA SWF, runtime
dan jumlah prosesor diubah menjadi cloudlet satu-PE dengan panjang:

```text
max(runtime dalam detik, 1)
  x max(prosesor dialokasikan, prosesor diminta, 1)
  x 1000 MI
```

Sumber, atribusi, dan detail dataset tersedia di
[data/SOURCES.md](src/main/resources/data/SOURCES.md).

Setelah simulasi selesai, aplikasi menampilkan tabel CloudSim Plus per cloudlet,
lalu ringkasan skenario, algoritma, seed, jumlah cloudlet yang selesai, dan
makespan aktual. Skenario dengan 1.000 task akan menghasilkan tabel yang
panjang di terminal.
