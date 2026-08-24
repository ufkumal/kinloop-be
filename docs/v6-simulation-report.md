# v6 Simülasyon Raporu — Ufkum'a vermeden önce

Tarih: 21 Ağustos 2026
Yöntem: v6 kurallarını birebir uygulayan **bağımsız referans motor** (Python) yazıldı ve
**gerçek veritabanı** üzerinde koşuldu. Ufkum'un Java kodu kullanılmadı — v6 henüz yazılmadı.
Havuz: 243 etkinlik (V17 + V18 + V20 uygulandı, doğrulandı).

---

## Özet

| Soru | Cevap |
|---|---|
| **1 · v6 ölçtüğümüz sorunları kapatıyor mu?** | **Evet, dördünü de.** Ölçüldü. |
| **2 · Unit testler tutarlı mı?** | 51 plan senaryosunun **33'ü birebir tuttu**. Kalan 18'in tamamı **tek bir kök nedene** iniyor. |

Ufkum'a vermeden önce düzeltilmesi gereken **5 madde** var (bölüm 4). Hiçbiri v6'nın
mantığıyla ilgili değil; hepsi belirsizlik ya da yazım hatası.

---

## 1 · İçerik yüklendi

| Aralık | Adet | Kaynak |
|---|---|---|
| 1-128 | 128 | temel seed |
| 129-162 | 34 | sakin havuz (V18) |
| 163-222 | 60 | keşif havuzu (yeni) |
| 223-243 | 21 | uzun etkinlikler (yeni) |
| **Toplam** | **243** | v6 §7 ile birebir |

Gelen dosya `V18__kidloop_activity_pool_163_243.sql` adıyla geldi; V18 ve V19 dolu olduğu
için **V20** olarak uygulandı. İçeriğine dokunulmadı. Ön koşulların ikisi de zaten
sağlanıyordu (`target_domain` 7 değerli, `difficulty` 1-4).

---

## 2 · Soru 1 — v6 sorunları kapatıyor mu?

Dört sorunu da ölçtük, dördü de kapanıyor.

### 2.1 Boş plan · ✅ kapandı

v5'te korunmacı + kaygılı çocukta plan hiç kurulamıyordu.

| Profil | Havuz | Plan | Kademe |
|---|---|---|---|
| 30 ay C4 kaygı 5 | 21 | **3** | 0 |
| 8 ay C4 kaygı 5 | 23 | **3** | 0 |
| 18 ay C4 kaygı 5 | 17 | **3** | 0 |
| 54 ay C4 kaygı 5 | 24 | **3** | 0 |
| 66 ay C4 kaygı 5 | 26 | **3** | 0 |
| 72 ay C4 kaygı 5 | 26 | **3** | 0 |

Hepsi kademe 0 — geri çekilmeye bile gerek kalmıyor. 72 aylık çocuk da artık plan alıyor
(EK1, bant üst sınırı 73).

### 2.2 Üç yuva · ✅ kapandı

v5'te 20 dk bütçede plan 2 etkinliğe düşüyor, KEŞİF sessizce kayboluyordu.

| Bütçe | Plan | committed | total | Bütçe dışı |
|---|---|---|---|---|
| A · 15-25 | **3** | 25 | 30 | Keşif ("vaktiniz varsa") |
| B · 25-35 | **3** | 35 | 35 | — |
| C · 35-45 | **3** | 40 | 40 | — |

Üç yuva her zaman doluyor. A bütçesinde Keşif taşıyor ama **kayboluyor değil, işaretleniyor** —
tasarlanan davranış bu.

### 2.3 Kararlılık · ✅ kapandı (bir çekinceyle)

v5'te aynı profildeki 5 çocuk 4 farklı plan alıyordu.

| Test | v5 | v6 |
|---|---|---|
| Aynı çocuk, 5 koşu | 4 farklı plan | **1 plan** |
| 5 ayrı çocuk, aynı profil | 4 farklı plan | 5 farklı plan |

İkisi birden isteniyordu ve ikisi de sağlanıyor: aynı çocuk hep aynı planı alıyor, farklı
çocuklar farklı plan alıyor (kura yansız kalıyor).

**Çekince:** bu kararlılık *tek bir implementasyon içinde* geçerli. İki farklı geliştirici
v6'yı doğru yazsa bile farklı planlar üretir — sebebi bölüm 4.1.

### 2.4 Yuva etiketleri · ✅ kapandı

v5'te GELİŞİM yuvasına dönem görevinin en iyi kartı gelmiyordu (Mavi'de 132'lik kart
dışarıda kalıp 124'lük kart GELİŞİM olmuştu).

| Profil | GELİŞİM'e gelen | Havuzun en iyi dönem kartı | |
|---|---|---|---|
| 30 ay C1 | 74 (132.0) | 74 (132.0) | ✅ |
| 30 ay C4 | 141 (127.0) | 141 (127.0) | ✅ |
| 60 ay C1 | 97 (135.0) | 97 (135.0) | ✅ |
| 72 ay C1 | 97 (135.0) | 97 (135.0) | ✅ |

Dördünde de GELİŞİM yuvası dönem görevi alanının en yüksek kartını alıyor.

---

## 3 · Soru 2 — Unit testlerin durumu

74 senaryonun 51'i plan üretiyor ve otomatik koşulabildi. 23'ü kural tablosu, bütünlük
kontrolü ya da plan üretmeyen doğrulama.

```
Koşulan plan senaryosu    51
Birebir tutan             33
Farklı                    18   → tamamı tek kök nedene iniyor
```

### 3.1 Tutan 33 senaryo

`A2-01, A2-03, A2-04, EK6-01, EK6-02, EK6-04, EK6-05, EK1-04, B1-01..B1-04, B1-06,
B7-01, B7-02, B7-04, B7-05, A11-01..A11-06, A11-08, EK4-01..EK4-04, B8-01..B8-04,
EK2-03` ve diğerleri.

Bunlarda havuz büyüklüğü, `committed`, `total`, `fallback_level`, üç yuvanın id'si ve
skorlar **tamamen** tuttu. Örnek — A2-01:

| Yuva | id | Süre | D | G | P | Z | B | Skor |
|---|---|---|---|---|---|---|---|---|
| Gelişim | 223 | 15 dk | 8 | 0 | 15 | 20 | 1.0 | 127.0 |
| Güçlendirme | 141 | 10 dk | 8 | 0 | 15 | 20 | 1.0 | 127.0 |
| Keşif | 178 | 10 dk | 0 | 0 | 0 | 20 | 1.0 | 120.0 |

Havuz 22, committed 35, total 35, fallback 0 — hepsi beklenenle aynı.

**Doküman gerçek veriye dayanıyor.** Referans motorunuz düzgün çalışmış; 39 id→başlık
eşleşmesinin 38'i veritabanıyla birebir.

### 3.2 Farklı çıkan 18 senaryonun tamamı aynı sebepten

Her farkı tek tek inceledim: benim seçtiğim kart ile dokümanınkinin **skoru, örneklenme
sayısı ve yük toplamı aynı**. Yani eşitlik zincirinin ilk üç kademesi ayıramıyor, iş
dördüncü kademeye — **tohumlu kuraya** düşüyor.

| Senaryo | Beraberlik |
|---|---|
| A2-02 | 149 vs 190 (117.0), 196 vs 152 (117.0) |
| A2-05 | 141 vs 223 (123.0) |
| A2-06 | 87 vs 86 (102.0) |
| EK6-03 | 212 vs 218 (99.0) |
| EK6-06 | 147 vs 100 (119.0) |
| EK1-01 | 240 vs 215 (119.0) |
| EK1-02 | 218 vs 214 (99.0) |
| EK1-03 | 229 vs 191 (119.0) |
| B1-05 | 210 vs 214 (117.0) |
| B7-03 | 223 vs 141 (146.05), 177 vs 181 (117.0) |
| B7-06 | 141 vs 223 (127.0) |
| B7-07 | 210 vs 214 (117.0) |
| B7-08 | 141 vs 223 (141.45), 177 vs 181 (98.0) |
| A11-07 | 240 vs 215 (146.05) |
| B8-05 | 240 vs 215 (119.0) |
| EK2-01 | 29 vs 26 (84.0), 26 vs 36 (84.0) |
| EK2-02 | 201 vs 206 (119.0) |
| EK2-04 | 19 vs 164 (79.0) |

**16'sı sadece farklı kart seçiyor. 2'si (EK1-01, B8-05) bütçeyi de değiştiriyor** — çünkü
beraber kalan iki karttan biri 10 dk, öteki 20 dk:

> EK1-01 · 215 ve 240 ikisi de 119.0, ikisi de SOCIAL_EMOTIONAL/INTERPERSONAL, ikisinin de
> yük toplamı 5. Ama 215 = **10 dk**, 240 = **20 dk**.
> 215 seçilirse: 15+10+10 = **35 dk**, hepsi bütçe içi.
> 240 seçilirse: 15+20 = 35, Keşif'e yer kalmıyor → **total 45 dk**, taşma.

Yani kuranın hangi kartı seçtiği **planın bütçeye sığıp sığmadığını** belirliyor.

---

## 4 · Ufkum'a vermeden düzeltilmesi gereken 5 madde

### 4.1 · Hash fonksiyonu tanımsız — EN KRİTİK

§4.3'ün 4. kademesi: `Tohumlu kura: hash(childId | planDate | activityId)`

Tanımlı olmayanlar: **hangi hash** (SHA-256? MD5? Java `String.hashCode()`?), **hangi
kodlama**, **ayraç karakteri**, **sıralama yönü**, kaç bit alınacağı.

Sonuç: v6'yı doğru okuyan iki geliştirici farklı plan üretir. Ben SHA-256 kullandım,
sizin referans motorunuz başka bir şey kullanmış. **Dokümandaki 18 senaryonun beklenen
değerleri, o hash yazılmadan Ufkum tarafından üretilemez.**

Öneri — spec'e tam formülü yazın, örneğin:

```
seed = SHA-256(UTF-8("<childId>|<yyyy-MM-dd>|<activityId>"))
anahtar = seed'in ilk 8 hex hanesi, işaretsiz tamsayı, ARTAN sırada
```

**İkinci öneri (daha değerli):** zincire hash'ten önce **süre** kriteri ekleyin.

```
1. Skor (yüksekten düşüğe)
2. Örneklenme sayısı (azdan çoka)
3. Duyusal yük toplamı (azdan çoka)
4. SÜRE (kısadan uzuna)      ← yeni
5. Tohumlu kura
```

Bu tek satır, 18 farkın büyük kısmını deterministik olarak çözer **ve** EK1-01 gibi
durumlarda planı bütçe içinde tutar (10 dk'lık 215, 20 dk'lık 240'ın önüne geçer).
Süre skora girmiyor — kısıt olarak kalıyor — ama beraberlik bozmada kullanılması
"üç yuva bütçeye sığsın" hedefiyle aynı yöne çalışıyor.

### 4.2 · Doküman bir etkinliği eski id'siyle anıyor

Doküman **id 133** = "Arkadaşına oyun öğret" diyor. Veritabanında:

| | id | Başlık |
|---|---|---|
| Doküman | 133 | Arkadaşına oyun öğret |
| Gerçek | **97** | Arkadaşına oyun öğret |
| Gerçek | 133 | Kalp sesi dinleme (sakin havuz) |

Sebep: `V17` bu etkinliği 133'ten **97'ye taşıdı** (satır 38: `(133, 97)`). Doküman V17
öncesi id'yi kullanmış. Diğer bütün nitelikler (SOCIAL_EMOTIONAL, INTERPERSONAL, d=4,
15 dk, GOZETIMLI, yük 3/3/3, skor 135.0) birebir doğru — yalnız id yanlış.

Etkilenen senaryolar: **B1-04, B8-02, EK1-01, EK1-02, EK6-03, EK6-05, B8-05**

### 4.3 · v6'daki SQL'ler gerçek şemayla uyuşmuyor

Bu haliyle migration'lar **çalışmaz**:

| v6 dokümanı §1.2 / §1.5 / §1.1 | Gerçek şema |
|---|---|
| `dunn_profiles.weight_noise / weight_visual / weight_physical` | `noise_weight / visual_weight / movement_weight` |
| `WHERE dunn_quadrant = 'C4'` | kolon adı **`quadrant`** |
| `child_domain_levels.success_counter` | kolon adı **`streak`** (smallint, `CHECK 0..2`) |
| `DROP CONSTRAINT activities_target_domain_check` | `chk_activities_target_domain` |
| `DROP CONSTRAINT activities_difficulty_check` | `chk_activities_difficulty` |

Ayrıca `streak` üzerindeki `CHECK (streak BETWEEN 0 AND 2)` kısıtı da düşürülmeli — v6
sayacı −1.0 ile +3.0 arasında geziyor, ondalık **ve negatif** olabiliyor.

Bir de: `dunn_profiles`'ta `chk_dunn_profile_weight_mode` kısıtı **"C4 ise ağırlıklar NULL
olmak zorunda"** diyor. v6 C4'e ağırlık veriyor; bu kısıt önce düşürülmeli. (Bunu V19'da
zaten yaptım, quadrant'tan bağımsız hepsi-ya-hiçbiri kuralıyla değiştirdim.)

### 4.4 · NOT NULL kolonlar DEFAULT'suz eklenemez

```sql
ALTER TABLE children ADD COLUMN daily_time_budget_min INT NOT NULL;   -- calismaz
ALTER TABLE daily_plans ADD COLUMN budget_min INT NOT NULL;           -- calismaz
```

`children` tablosunda satır var. DEFAULT'suz NOT NULL kolon eklenemez. Ya `DEFAULT`
verilmeli, ya üç adımda yapılmalı (nullable ekle → doldur → NOT NULL yap).

Aynı sorun `daily_plans`'ın beş yeni kolonunda da var.

### 4.5 · §3.2'de bir durum tanımsız

Sayaç kuralı SEVDİ için üç durum sayıyor: `d = L+1`, `d = L`, `d < L`.
**`d > L+1` tanımlı değil** — ama mümkün (L=1 çocuğa d=3 kart gelebilir, Z=−25 alır ama
yine de plana girebilir).

Unit test B3-01 satır 4 bu boşluğu `+0.0` diye dolduruyor. Aynısı ZORLANDI için de
geçerli: `d = L+1` ve `d ≤ L` tanımlı, `d > L+1` değil.

Spec'e iki satır eklenmeli.

---

## 5 · Koşulamayan 23 senaryo

Bunlar plan üretmiyor; kural tablosu, bütünlük ya da içerik kontrolü:

```
B3-01..B3-04   basamak sayacı kredi tablosu
B8-06, B8-07   tavan davranışı
A4-01, A5-01, A5-02, A6-01, A12-01   liste ve adlandırma kontrolleri
A7-01, A10-01, C1-01, C1-02, C4-01   sinyal, tazelik, kapsam
EK5-01, EK5-02, EK6-07, EK7-01, EK8-01, A2-07, EK1-05
```

Bunlardan ikisini doğrudan doğruladım:

* **EK5-01** (id 1-243 boşluksuz, çakışma yok): veritabanı **243 etkinlik, min 1, max 243,
  boşluk yok** ✅
* **EK8-01** (243 etkinliğin tamamı yayın doğrulamasından geçer): ayrı kontrol gerekiyor,
  koşulmadı.

---

## 6 · Sonuç

**v6 tasarım olarak doğru ve ölçtüğümüz dört sorunu da kapatıyor.** Referans motoru
yazarken spec'te mantık hatası çıkmadı; kurallar tutarlı ve uygulanabilir.

Unit test dokümanı da sağlam: 51 senaryonun 33'ü birebir tuttu ve 39 id→başlık
eşleşmesinin 38'i doğru. Kalan farkların **hiçbiri v6'nın mantığından kaynaklanmıyor** —
tamamı tek bir belirsizlikten (hash) ve bir yazım hatasından (id 133) geliyor.

Bölüm 4'teki beş madde düzeltilirse Ufkum spec'ten kod yazıp testleri doğrudan
geçirebilir. Özellikle 4.1'deki **süre kriteri** eklenirse hem belirsizlik büyük ölçüde
kalkar hem plan daha sık bütçe içinde kalır.
