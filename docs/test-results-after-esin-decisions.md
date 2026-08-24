# T1–T12 — Esin'in kararları uygulandıktan sonra

Tarih: 17 Ağustos 2026
Taban: `d29dd9c` (Ufkum'un main'i) + bu çalışmadaki A değişiklikleri
Ortam: lokal Postgres 16, veritabanı sıfırdan kuruldu, 19 migration

---

## 1. Uygulanan üç karar

| # | Karar | Nerede |
|---|---|---|
| 1 | C4 duyusal ceza katsayıları **5 / 5 / 3** | `V19` → `dunn_profiles` |
| 2 | C4 elemesi **maks(ses, görsel) ≥ 3** — hareket ekseni elemeden çıktı | `ActivityEligibilityPolicy` |
| 3 | GÖZETİMLİ için ayrı bağlanma çarpanı **1.07** | `V19` + `ActivityScorer` |

### Yol üstünde çıkan engel: şema eski kuralı kısıt olarak tutuyordu

`V11` şu kısıtı koymuş:

```sql
CONSTRAINT chk_dunn_profile_weight_mode CHECK (
    (quadrant = 'C4' AND noise_weight IS NULL AND visual_weight IS NULL AND movement_weight IS NULL)
    OR
    (quadrant <> 'C4' AND noise_weight IS NOT NULL AND ...)
)
```

Yani "C4'ün ağırlıkları NULL olmak **zorunda**" kuralı veritabanına gömülüydü.
İlk denemede migration bu kısıta takılıp uygulama açılmadı. `V19` kısıtı düşürüp
yerine quadrant'tan bağımsız *hepsi-ya-hiçbiri* kuralı koyuyor. Bu ayrıca
`ActivityScorer`'daki bir riski de kapatıyor: kod yalnız `noise_weight`'in null
olup olmadığına bakıp diğer ikisini doğrudan kullanıyor, kısmi ağırlık NPE üretirdi.

### Bir not: GÖZETİMLİ 1.07 kararını çıkarımla aldım

Aktarılan metinde A maddesi yalnız katsayı ve eleme kuralını sayıyordu; 1.07
"üç karar birlikte çalışıyor" cümlesinde geçiyordu. Uyguladım, çünkü doğrulama
ölçümüm ancak üçü birlikteyken aktarılan tabloyla birebir tuttu. Yanlışsa tek
satır geri alınır.

---

## 2. Aktarılan ölçüm doğrulandı

Beraberlik gruplarını bağımsız hesapladım, altı yaşın altısında da aynı çıktı:

| Yaş | Bugün | Esin 3 karar | *(yalnız katsayı)* |
|---|---|---|---|
| 8 ay | 11 | **4** | 4 |
| 18 ay | 3 | **1** | 1 |
| 30 ay | 4 | **2** | 2 |
| 42 ay | 3 | **1** | 2 |
| 54 ay | 2 | **2** | 2 |
| 66 ay | 4 | **2** | 3 |

Ayrıştırma: **işin büyük kısmını katsayı yapıyor.** İki eksenli eleme ve
GÖZETİMLİ çarpanı iki bantta ek katkı veriyor (42 ay 2→1, 66 ay 3→2).
Havuzlar da büyüyor: 8 ay 14→17, 18 ay 8→9, 54 ay 5→6.

---

## 3. En büyük kazanım: C4 skorları artık dokümanla birebir

Önceki koşuda **bütün** C4 skorları sapıyordu (D=0). Şimdi:

| Test | Beklenen | Ölçülen | |
|---|---|---|---|
| T3 · 30 ay | 127.0 / 117.0 / 117.0 | 127.0 / 117.0 / 117.0 | ✅ |
| T7 · 8 ay | 155.25 / 134.55 / 126.5 | 155.25 / 134.55 / 126.5 | ✅ |
| T10 · 66 ay | 146.05 / 94.3 / 82.8 | 146.05 / 94.3 / 82.8 | ✅ |
| T12 · 30 ay, 10 dk | 128.8 / 128.8 | 128.8 / 128.8 | ✅ |

Kısmen tutanlarda da tutan kartların skoru tam: T2'de 141 = 146.05 ✓ ve
145 = 134.55 ✓; T8'de 135 = 155.25 ✓ ve 138 = 138.0 ✓; T9'da 153 ve 154 = 146.05 ✓.

**D terimi artık beş quadrant'ta da doğru.** Bu madde kapandı.

**T12 tam isabet:** etkinlikler, skorlar ve yuva etiketleri dokümanla birebir
(GELİŞİM 142 = 128.8, GÜÇLENDİRME 143 = 128.8).

---

## 4. Değişen etkinlik seçimleri ve sebepleri

Üç testte beklenen kart plandan çıktı. Hepsinin sebebi belirlendi, hiçbiri hata değil:

| Test | Çıkan | Sebep | Giren |
|---|---|---|---|
| T2 | 144 · İkişer eşleştirme sessiz | **GÖZETİMLİ** → 1.07 çarpanı, 134.55'ten **125.19**'a düştü, 6. sıraya indi | 142 |
| T8 | 139 · Kaba içine koyma çıkarma | **GÖZETİMLİ** → aynı sebep | 57 · Merdiven tırmanışı (yük 2/2/**4**) |
| T9 | 156 · Bugünkü havam çizimi | eşit skorlu rakip öne geçti | 117 · Yavaş yoga hayvanları (yük 1/2/**3**) |

57 ve 117'nin ortak özelliği: **hareket yükü 3+, ses ve görsel yükü düşük.**
Eski üç eksenli elemede düşüyorlardı, iki eksenli elemede havuza giriyorlar.
Yani karar 2 tam da amaçlandığı gibi çalışıyor — sakin ama hareketli etkinlikler
korunmacı çocuğa açıldı.

Test dokümanının beklenen kartları bu üç kararı bilmiyor; **yeniden hesaplanmaları
gerekiyor.** Bu beklenen bir sonuç, sapma değil.

---

## 5. Kararlılık: iyileşti ama ÇÖZÜLMEDİ

Aynı T2 profili, beş ayrı çocuk:

| Koşu | Öncesi | Sonrası |
|---|---|---|
| 1 | DEV:141 STR:146 EXP:143 | DEV:141 STR:143 EXP:145 |
| 2 | DEV:141 STR:145 EXP:144 | DEV:141 STR:143 EXP:145 |
| 3 | DEV:141 STR:145 EXP:143 | DEV:141 STR:142 EXP:145 |
| 4 | DEV:141 STR:146 EXP:144 | DEV:142 STR:145 EXP:141 |
| 5 | DEV:141 STR:145 EXP:143 | DEV:142 STR:145 EXP:141 |
| **Farklı sonuç** | **4 / 5** | **3 / 5** |

Kalan kararsızlığın iki kaynağı var ve **ikisi de A kapsamı dışında**:

1. **Gerçek skor beraberliği.** T2 havuzunda 142 ve 143 ikisi de tam 128.80.
   Katsayı bunu ayıramaz; iki farklı kart aynı puanı hak ediyor.
2. **Yuva permütasyonu.** `DailyPortfolioBuilder` üçlünün **toplamını**
   maksimize ediyor; toplam permütasyona duyarsız olduğu için 141/142/145'i
   hangi yuvaya koyduğu fark etmiyor ve kura karar veriyor.
   Koşu 4-5'te en yüksek kart (141, 146.05) GELİŞİM yerine KEŞİF'e düştü.

Yani **A tek başına yeterli değil. B ve C hâlâ gerekli.**

---

## 6. Etkinlik seçimi: 7/11

| Test | Küme | Skorlar | Yuvalar |
|---|---|---|---|
| T1 · C1 | ✅ | ✅ | ❌ |
| T2 · C4 | ❌ (144→142) | kısmi ✅ | ❌ |
| T3 · C4 | ✅ | ✅ | ❌ |
| T4 · C3 | ✅ | ✅ | ❌ |
| T5 · C2 | ❌ (67→80, berabere) | ✅ | ❌ |
| T6 · MIXED | ✅ | ✅ | ❌ |
| T7 · C4 8 ay | ✅ | ✅ | ❌ |
| T8 · C4 18 ay | ❌ (139→57) | kısmi ✅ | ❌ |
| T9 · C4 54 ay | ❌ (156→117) | kısmi ✅ | ❌ |
| T10 · C4 66 ay | ✅ | ✅ | ❌ |
| T11 | *(15 dk şemada yok)* | — | — |
| T12 · C4 10 dk | ✅ | ✅ | ✅ |

Oran önceki koşuyla aynı (7/11) ama **niteliği tamamen değişti**: önce sapmalar
hesap farkındandı, şimdi üçü karar sonucu (beklenen kart artık doğru olarak
plandan çıkıyor), biri beraberlik kurası.

Havuz büyüklükleri ve profil türetme yine 12/12 doğru.

---

## 7. Durum

| Madde | Durum |
|---|---|
| **A** · C4 katsayısı + eleme + GÖZETİMLİ | ✅ **uygulandı, çalışıyor** |
| D terimi tüm quadrant'larda | ✅ artık doğru |
| Boş plan sorunu | ✅ çözülü kaldı, 12/12 plan üretildi |
| Beraberlik yoğunluğu | ✅ belirgin azaldı (11→4, 3→1, 4→2) |
| **Sonuç kararlılığı** | ⚠️ 4/5 → 3/5, **hâlâ kararsız** |
| **B** · yuva önceliği | ⛔ yapılmadı |
| **C** · tohumlu kura | ⛔ yapılmadı |
| **D** · doküman beklenen değerleri | ⛔ yeniden hesaplanmalı |
| **E** · `RecommendationScenarioTest` | ⛔ hâlâ V10 dosyasını okuyor |

## 8. Sıradaki adım

B ve C birlikte uygulanırsa kalan kararsızlık tamamen kapanır:

* **B** — `DailyPortfolioBuilder.best()` toplam skor yerine yuva önceliğine göre
  sıralasın (önce GELİŞİM skoru, sonra GÜÇLENDİRME, sonra KEŞİF). Bu, modelin
  yazılı kuralı; 141 gibi en yüksek kartın KEŞİF'e düşmesini engeller.
* **C** — kura kalsın ama `(childId, planDate)`'den tohumlansın. Aynı çocuk aynı
  gün her zaman aynı planı alır; 142/143 gibi gerçek beraberlikler yine adil
  şekilde dağılır; testler sabitlenebilir.

Sonrasında **D** yapılabilir: beklenen değerler artık tekrarlanabilir olacağı için
doküman sabitlenebilir.
