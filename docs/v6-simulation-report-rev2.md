# v6 Simülasyon Raporu · 2. tur (düzenlemeler sonrası)

Tarih: 21 Ağustos 2026
Girdi: revize `Kidloop_Oneri_Prensibi_v6.md` + revize `Kidloop_Unit_Test_v6.md` + etkinlik paketi
Yöntem: v6 kurallarını uygulayan bağımsız referans motor, gerçek 243 etkinliklik veritabanında

---

## Sonuç: **51 / 51 — tam isabet**

Önceki turda 33/51 tutuyordu. Beş düzeltmenin tamamı çalıştı.

```
Koşulan plan senaryosu    51
Birebir tutan             51
Farklı                     0
```

Havuz büyüklüğü, `committed`, `total`, `fallback_level`, üç yuvanın id'si ve skorlar —
hepsi 51 senaryoda da beklenenle aynı. Hiçbir eşleme ya da düzeltme uygulanmadan.

---

## 1 · Önceki turdaki beş maddenin durumu

| # | Madde | Durum |
|---|---|---|
| 4.1 | Hash tanımsızdı | ✅ **Tanımlandı** — tam sayı aritmetiği, platform bağımsız |
| 4.1b | Süre kriteri önerisi | ✅ **Eklendi** — zincirin 4. kademesi |
| 4.2 | id 133 yanlıştı | ✅ **Düzeltildi** — her iki dokümanda da 97 |
| 4.3 | SQL kolon adları | ⚠️ **Kısmen** — 4 ad hâlâ yanlış (bölüm 3) |
| 4.4 | NOT NULL DEFAULT'suz | ✅ **Düzeltildi** — DEFAULT → UPDATE → DROP DEFAULT |
| 4.5 | `d > L+1` tanımsızdı | ✅ **Kapatıldı** — hem SEVDİ hem ZORLANDI için |

Tohum tanımı artık şöyle ve doğrulandı:

```
gun  = 20260821
seed = (childId × 1000003 + gun × 10007 + activityId) mod 2147483647
```

Aynı tanımı uyguladığımda dokümanın bütün beklenen değerleri çıktı. **Tanım yeterince
kesin — Ufkum bundan kod yazıp aynı sonuçları üretebilir.**

---

## 2 · Dört sorun kapandı (yeniden ölçüldü)

| Sorun | Ölçüm |
|---|---|
| Boş plan | 6 yaş bandının hepsinde C4+kaygı 5 → **3 etkinlik**, kademe 0 |
| Üç yuva | A: 3 (25/30 dk) · B: 3 (35/35) · C: 3 (40/40) — **hepsinde üç yuva** |
| Kararlılık | Aynı çocuk 5 koşu → **1 plan** |
| Yuva etiketi | **4/4** profilde GELİŞİM dönem görevinin en iyi kartını aldı |

Kural tabloları da doğrulandı:

* **B3-01** sayaç kredisi: 7 satırın 7'si tuttu — revize `d ≥ L+1 → +1.0` kuralı dahil
* **EK5-01** id bütünlüğü: 243 etkinlik, min 1, max 243, **boşluk yok** ✅

---

## 3 · Kalan dört ad hatası

Revizyonda üç ad düzeltilmiş (`noise_weight`, `quadrant`, `streak`) ama dördü hâlâ
gerçek şemayla uyuşmuyor. Bu haliyle migration'lar **çalışmaz**:

| v6 | Gerçek şema | Nerede |
|---|---|---|
| `chk_target_domain` | **`chk_activities_target_domain`** | §1.1 |
| `chk_difficulty` | **`chk_activities_difficulty`** | §1.1 |
| `physical_weight` | **`movement_weight`** | §1.2 |
| `chk_streak_range` | **`child_domain_levels_streak_check`** | §1.5 |

Doğrulama sorgusu:

```sql
SELECT conname FROM pg_constraint
 WHERE conrelid IN ('activities'::regclass,'child_domain_levels'::regclass) AND contype='c';

SELECT column_name FROM information_schema.columns
 WHERE table_name='dunn_profiles' AND column_name LIKE '%weight%';
```

**Not:** `chk_child_domain_levels_level` zaten `CHECK (level >= 1 AND level <= 4)` —
v6'nın `L ∈ [1,4]` kuralı için ek migration gerekmiyor.

---

## 4 · EK8-01 bu haliyle BAŞARISIZ olur

v6 §7.1: *"243 etkinliğin hepsi bu kontrolleri geçiyor."*

Ölçtüm — geçmiyor. Üç etkinliğin **ikişer kazanımı var**, kural en az 3 diyor:

| id | Etkinlik | Kazanım | Havuz |
|---|---|---|---|
| 3 | Kontrast kart izleme | 2 | temel seed |
| 18 | Minder üstünde destekli oturma oyunu | 2 | temel seed |
| 36 | Sessiz oda mobil izleme | 2 | temel seed |

Üçü de **temel seed'den** (1-128). Yeni havuzların (129-243) hepsi kontrolleri geçiyor —
sizin ürettiğiniz içerik temiz, sorun eski seed'de.

Diğer kontroller 243/243 geçiyor: `easier_variation` dolu, `harder_variation` dolu,
en az 4 adım, zekâ alanları farklı, süre tanımlı.

Ya bu üç etkinliğe birer kazanım eklenmeli, ya EK8-01 "yeni içerik için geçerli" diye
daraltılmalı.

---

## 5 · Süre kriterinin yan etkisi — benim önerimden çıktı

Süre kriterini ben önermiştim ve işe yaradı: bütçe taşmaları bitti, 18 fark kapandı.
Ama **beklemediğim bir sonucu var** ve dürüstçe bildirmem gerekiyor.

v6 §4.3 tohumlu kuranın gerekçesini şöyle açıklıyor:

> *"küçük id sistematik yanlılık üretir, aynı profildeki her çocuk aynı planı alır ve
> yüksek id'li etkinlikler hiç gösterilmez. Tohumlu kura hem deterministik hem yansız."*

Süre 4. kademeye eklenince beraberliklerin neredeyse tamamı tohuma ulaşmadan çözülüyor.
Ölçtüm — aynı profildeki **20 ayrı çocuk**:

| Profil | Farklı plan sayısı |
|---|---|
| 30 ay C1 kaygı 2 | **1** |
| 30 ay C4 kaygı 5 | **1** |
| 60 ay C1 kaygı 2 | **1** |
| 18 ay C4 kaygı 5 | **1** |
| 72 ay C3 kaygı 2 | **1** |

Seçimlerin yalnız **2 / 18**'i tohum kademesine iniyor.

Yani tohum, dokümanın ona yüklediği işi artık büyük ölçüde yapmıyor: aynı profildeki her
çocuk **aynı planı** alıyor.

**Bu bir hata değil, bir denge.** Üç seçenek var:

1. **Kabul et.** İlk gün aynı, ikinci günden itibaren tazelik penceresi ve geri bildirim
   çocukları ayrıştırıyor. Pratik etkisi ilk günle sınırlı. Bu durumda §4.3'teki gerekçe
   cümlesi düzeltilmeli — tohum artık o işi yapmıyor.
2. **Süreyi koşullu yap.** Süre kriteri yalnız *uzun olan bütçeyi taşıracaksa* devreye
   girsin. Hem bütçe korunur hem tohum işlevini sürdürür.
3. **Süreyi tohumdan sonraya al.** Bütçe sorunu geri gelir — önermem.

Ürün kararı sizin. Ölçümü verdim.

---

## 6 · İki küçük not

**§1.6'da kod bloğu açılışı eksik.** `daily_plans` ALTER'ları ``` ile başlamıyor, yalnız
kapanışı var. Markdown'da bozuk görünüyor; içerik doğru.

**§7 migration numarası.** Doküman "V17 (id düzeltmesi), V18 (163-243)" diyor ama V18
sakin havuz olarak zaten kullanıldı. Yeni paketi bende **V20** olarak uyguladım
(V19 = C4 ağırlıkları). Ufkum'un numaralandırmayı repodaki son sürüme göre alması gerekiyor.

---

## 7 · Özet

v6 artık **uygulanabilir durumda**. Spec'ten bağımsız bir motor yazdım ve dokümanın 51 plan
senaryosunun tamamını birebir ürettim — bu, spec'in hem tutarlı hem yeterince kesin
olduğunun kanıtı.

Ufkum'a vermeden önce üç küçük iş:

1. Dört ad düzeltmesi (bölüm 3) — yoksa migration çalışmaz
2. EK8-01 kararı (bölüm 4) — üç etkinliğe kazanım eklemek ya da senaryoyu daraltmak
3. §4.3'teki tohum gerekçesi (bölüm 5) — kabul edilecekse cümle düzeltilmeli

Bunlar dışında spec'te mantık hatası, çelişki ya da boşluk bulamadım.
