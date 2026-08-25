# `resolved_reason` teşhis mekanizması · Kök neden raporu

Tarih: 25 Ağustos 2026
İncelenen kod: `1468948` (main, V29–V33)
Yöntem: kod okuma + git geçmişi + gerçek API üzerinden tam havuz taraması (6 profil, 237 oy)

**Canlı veritabanına erişimim yok.** Ölçümleri yerelde, aynı 33 migration'la kurulmuş
veritabanında, gerçek HTTP çağrılarıyla yaptım — `resolveReason()` taklit edilmedi,
kodun kendisi koştu.

---

## 0 · Önce: raporun dayandığı v6 metni bende yok

Görev tanımı v6 §3.3'ten iki cümle alıntılıyor:

> "sebep sorulmuyor (A8 teslimat sonrası) ve serbest metin çözümlemesi henüz yok"
> "Bu sürümde 'olmadı' oyu **tek dal olarak** işlenir"

Ayrıca tablodaki "(bu sürümde uygulanmaz)" parantezlerini.

**Bu üç şey de bende olan iki v6 nüshasında yok** — ne 16 Ağustos'ta, ne
21 Ağustos 11:58 revizesinde. Elimdeki §3.3'ün tamamı şu:

| Oy | Gardner | Basamak sayacı | Filtre |
|---|---|---|---|
| Sevdi | +0.30 / +0.15 | Zorluk konumuna göre kredi | — |
| Zorlandı | **dokunulmaz** | Zorluk konumuna göre eksi | Sonraki öneride zorluk düşer |
| Olmadı (ilgisizlik) | −0.15 | **dokunulmaz** | — |
| Olmadı (duyusal) | dokunulmaz | dokunulmaz | Yük eşiği sıkılır |
| Olmadı (ayrılık) | dokunulmaz | dokunulmaz | Katılım filtresi sıkılır |

Tabloda "bu sürümde uygulanmaz" kaydı yok, "tek dal" cümlesi yok.

**Yani bende olan v6'ya göre çelişki yok.** §3.3 üç ayrı "Olmadı" dalını, üçünün ayrı
puan etkisiyle tanımlıyor; kod da tam olarak üç dal üretiyor. Kod §3.3'ün tablosunu
uyguluyor.

> **İkinci kez oluyor.** Bir önceki görev "v6 §4.8"e dayanıyordu, o da bende yok
> (bölüm 4 dördüncü maddede bitiyor). Şimdi §3.3'ün iki cümlesi ve üç parantezi.
> Büyük ihtimalle bana hiç verilmemiş daha yeni bir v6 nüshası dolaşımda.
> **O nüshayı almadan "kod v6'yla çelişiyor" diyemem.** Aşağıdaki değerlendirmenin
> tamamı elimdeki 21 Ağustos revizesine göre.

---

## 1 · Mekanizma

**Konum:** `FeedbackLearningService.resolveReason()`, satır **230-253**.
`submit()` içinden satır 67'de, feedback satırı yazılmadan hemen önce çağrılıyor.

### Karar ağacı

```
oy ≠ DISLIKED
    └─▶ null                                          (SEVDİ ve ZORLANDI sebep almaz)

oy = DISLIKED
    │
    ├─ tip C3 veya C4 mı?                             [satır 238]
    │      HAYIR ──────────────────────────┐
    │      EVET ─┐                         │
    │            │                         │
    │            ├─ etkinlik.ses     > tip.ses_toleransı      ┐
    │            ├─ VEYA görsel      > tip.görsel_toleransı   ├─ EVET ─▶ SENSORY
    │            └─ VEYA hareket     > tip.hareket_toleransı  ┘  [241-245]
    │                   │
    │                   HAYIR ─────────────┤
    │                                      │
    ├──────────────────────────────────────┘
    │
    ├─ kaygı ≥ 4  VE  etkinlik.katılım = BAGIMSIZ  ──▶ INVOLVEMENT   [247-251]
    │
    └─ aksi hâlde ─────────────────────────────────▶ INTEREST        [252]
```

### Girdiler

| Girdi | Kaynak | Rolü |
|---|---|---|
| `feedback_type` | annenin butonu | DISLIKED değilse sebep hiç üretilmez |
| `dunn_quadrant` | `child_profile_snapshots` | **kapı** — yalnız C3/C4 duyusal dala girebilir |
| tip toleransları | `dunn_profiles` tablosu | eşik değerleri, koda gömülü değil |
| `noise_load`, `visual_load`, `physical_intensity` | `activities` | karşılaştırılan yükler |
| `separation_anxiety` | `child_profile_snapshots` | ayrılık dalının kapısı |
| `involvement_type` | `activities` | ayrılık dalının ikinci koşulu |
| `attachment_anxiety_threshold` | `scoring_parameters` (4.00) | kaygı eşiği |

**Girdi olmayanlar:** serbest metin, `D` cezası, etkinliğin alanı/zekâsı, çocuğun
basamağı, `child_sensory_adjustments`.

### Tolerans tablosu (`dunn_profiles`)

| Tip | ses | görsel | hareket |
|---|---|---|---|
| C1 | 3 | 3 | 3 |
| C2 | 4 | 4 | 5 |
| C3 | 2 | 2 | 3 |
| C4 | 1 | 2 | 2 |
| MIXED | 3 | 3 | 3 |

Karşılaştırma **kesin büyüktür** (`>`), eşitlik aşma sayılmıyor. **Tek eksen yeterli**
(`VEYA`), üç eksenin birden aşması gerekmiyor.

---

## 2 · Puan etkileri

| `resolved_reason` | Gardner hedef | Gardner ikincil | Basamak sayacı | Başka etki |
|---|---|---|---|---|
| `INTEREST` | **−0.15** | dokunulmaz | dokunulmaz | yok |
| `SENSORY` | dokunulmaz | dokunulmaz | dokunulmaz | **yok — olması gerekiyordu** |
| `INVOLVEMENT` | dokunulmaz | dokunulmaz | dokunulmaz | **yok — olması gerekiyordu** |
| `null` (SEVDİ) | +0.30 | +0.15 | zorluk konumuna göre kredi | yok |
| `null` (ZORLANDI) | dokunulmaz | dokunulmaz | zorluk konumuna göre eksi | yok |

Kod referansı: `applyGardnerLearning()` satır 138-146 — yalnız
`type == DISLIKED && reason == INTEREST` dalında ceza uygulanıyor.
`applyDomainLearning()` satır 168-174 — `domainDelta()` yalnız LIKED ve STRUGGLED'a
bakıyor, DISLIKED her sebeple 0 döndürüyor.

**Örnekleme sayacı üç sebepte de artıyor** (`recordFeedbackSample()`, satır 135) —
sebep ne olursa olsun. Bu sayaç eşitlik bozma zincirinin 2. kademesine ve Keşif
yuvasının "en az örneklenmiş" kuralına giriyor.

### `SENSORY` çözümlendiğinde `child_sensory_adjustments`'a satır yazılıyor mu?

**Hayır.** Tüm `src/main` içinde `new ChildSensoryAdjustment(` ya da
`sensoryAdjustmentRepository.save(` geçen tek satır yok. Tablo `V26` ile kurulmuş,
`ActivityEligibilityPolicy` ve `ActivityScorer` tarafından **okunuyor**, ama
**hiçbir yerde yazılmıyor.**

Sonuç: `SENSORY` ve `INVOLVEMENT` sebepleri hesaplanıp `feedback` satırına yazılıyor ve
orada kalıyor. v6 §3.3'ün "Yük eşiği sıkılır" ve "Katılım filtresi sıkılır" sütunları
kodda karşılıksız. **Döngü kapalı.** (Bunu 24 Ağustos'taki kod incelemesinde de
bildirmiştim.)

---

## 3 · Hipotezlerin sınanması

| # | Hipotez | Sonuç |
|---|---|---|
| **H1** | `dunn_quadrant` doğrudan kullanılıyor | **Kısmen doğru** — satır 238 tam olarak `quadrant == C3 \|\| quadrant == C4` kontrolü yapıyor. Ama tek başına karar vermiyor, yalnız duyusal dalın **kapısı**. |
| **H2** | Yük ile tolerans karşılaştırılıyor | **Doğru** — satır 241-243, kapıdan geçenlerde. Eşik `>`, tek eksen yeterli. |
| **H3** | `D` cezası bir eşiği geçiyor | **Yanlış** — `resolveReason()` içinde `D`, ağırlık ya da skor geçen tek satır yok. Görev tanımının hesabı da bunu zaten çürütmüştü. |
| **H4** | `separation_anxiety` karara giriyor | **Doğru ama ölü** — satır 247-251 var, ancak hiçbir zaman çalışamıyor. Aşağıda. |
| **H5** | Rastgele ya da varsayılan | **Yanlış** — kolonun `DEFAULT`'u yok, `CHECK (IN ('SENSORY','INVOLVEMENT','INTEREST'))` var, DISLIKED dışında `NULL` kalıyor. Deterministik. |

### H4 ayrıntı — `INVOLVEMENT` üretilemez

İki koşul birbirini dışlıyor:

```
ActivityEligibilityPolicy satır 22-27:
    kaygı ≥ 4  ⇒  BAGIMSIZ etkinlikler HAVUZDAN ÇIKARILIR

FeedbackLearningService satır 249:
    kaygı ≥ 4  VE  etkinlik BAGIMSIZ  ⇒  INVOLVEMENT
```

Bir etkinliğe oy verilebilmesi için önce plana girmesi, plana girmesi için havuzda
olması gerekiyor. Kaygılı çocuğun havuzunda BAGIMSIZ etkinlik yok; kaygısız çocukta
ise ilk koşul sağlanmıyor. **`FeedbackReason.INVOLVEMENT` hiçbir çocukta üretilemez.**

Ölçümle de doğrulandı: altı profilde 237 DISLIKED oyu, **0 adet INVOLVEMENT**.

Kilidi açacak tek şey `child_sensory_adjustments.involvement_filter = 'RELAXED'` —
o da hiçbir yerde yazılmıyor. 30 aylık havuzda 2 adet BAGIMSIZ etkinlik var, yani
eleme gerçek, boşa çalışmıyor.

---

## 4 · Kritik ölçüm: C4 çocuklarda ilgi öğrenimi mümkün mü

Görevin en önemli sorusu buydu: *"C4 çocuklarda DISLIKED oyları **her zaman**
SENSORY mi çözümleniyor?"*

**Cevap: hayır.**

Altı profil kurdum ve her birinin **havuzundaki her etkinliğe** gerçek API üzerinden
`DISLIKED` oyu verdim. Toplam 237 oy.

| Profil | Kaygı | Havuz | SENSORY | INVOLVEMENT | INTEREST |
|---|---|---|---|---|---|
| **C4** | 5 | 21 | **7 (33%)** | 0 | **14 (67%)** |
| **C3** | 5 | 42 | **21 (50%)** | 0 | **21 (50%)** |
| C3 | 2 | 44 | 22 (50%) | 0 | 22 (50%) |
| C1 | 2 | 44 | 0 (0%) | 0 | 44 (100%) |
| C2 | 2 | 44 | 0 (0%) | 0 | 44 (100%) |
| MIXED | 5 | 42 | 0 (0%) | 0 | 42 (100%) |

**C4'te ilgi puanı düşebiliyor — havuzun üçte ikisinde.** Korunmacı çocuklarda
öğrenmenin tamamen kilitlenmesi riski **gerçekleşmiyor.**

Sebebi şu: C4 havuz filtresi zaten `maks(ses, görsel) ≥ 3` olanları eliyor, yani
havuzda kalan etkinliklerin yükleri düşük. C4 toleransı 1/2/2 olduğundan yalnız
**ses yükü 2 olanlar** eşiği aşıyor.

```
C4 havuzunda SENSORY çıkanların yükleri : 2/1/1  2/1/2  2/2/1  2/2/2   (ses 2 > 1)
C4 havuzunda INTEREST çıkanların yükleri: 1/1/1  1/1/2  1/2/1  1/2/2   (ses 1 ≤ 1)
```

Kural tutarlı ve okunabilir: **C4'te ses yükü 2 ise duyusal, 1 ise ilgi.**

---

## 5 · Asıl kusur: aynanın diğer yüzü

Risk C4'te değil, **diğer üç tipte.**

`dunn_quadrant` kapısı yüzünden C1, C2 ve MIXED çocuklarda `SENSORY` **hiçbir zaman**
çözümlenemiyor — etkinliğin yükü o çocuğun kendi toleransını açıkça aşsa bile.

30 aylık havuzda, her tipin **kendi** toleransına göre:

| Tip | Tolerans | Havuz | Kendi toleransını aşan | Kod ne yapıyor |
|---|---|---|---|---|
| C1 | 3/3/3 | 44 | **7 etkinlik** | yine de `INTEREST` — ilgi puanı düşüyor |
| C2 | 4/4/5 | 44 | **1 etkinlik** | yine de `INTEREST` |
| MIXED | 3/3/3 | 44 | **7 etkinlik** | yine de `INTEREST` |
| C3 | 2/2/3 | 44 | 22 etkinlik | `SENSORY` ✓ |
| C4 | 1/2/2 | 22 | 7 etkinlik | `SENSORY` ✓ |

Somut örnek: havuzda yükleri **5/3/3** olan bir etkinlik var. C1 çocuğun ses toleransı
3. Anne "olmadı" derse kod bunu `INTEREST` sayıyor ve hedef zekânın puanını 0.15
düşürüyor — oysa çocuk büyük ihtimalle **sesten** kaçtı, o zekâ alanından değil.

**Bu, görevin korktuğu hatanın tam tersi ama aynı büyüklükte.** C4'te ilgi öğrenimi
kısıtlanmıyor; C1/C2/MIXED'te duyusal teşhis hiç çalışmıyor ve ilgi puanı haksız yere
düşüyor.

`MIXED` özellikle önemli: `resolveReason` satır 237, `dunn_quadrant` **null** ise
`MIXED` varsayıyor. Yani tipi belirlenemeyen her çocuk duyusal teşhisin tamamen
dışında kalıyor.

### İkincil tutarsızlık

`resolveReason()` **temel** Dunn toleranslarını kullanıyor (`dunnRepository.findById`,
satır 239). Oysa `ActivityScorer` aynı işi yaparken çocuğun **ayarlanmış**
toleranslarını (`ChildSensoryAdjustment`) kullanıyor. İki yer aynı eşiği iki farklı
kaynaktan okuyor.

Bugün fark üretmiyor çünkü ayarlama tablosuna hiç satır yazılmıyor. Ama yazılmaya
başlandığı gün skor bir eşiği, teşhis başka bir eşiği kullanacak.

---

## 6 · Mekanizma ne zaman eklendi

| Ne | Commit | Tarih | Yazan |
|---|---|---|---|
| `resolved_reason` kolonu + 3 değerli CHECK | `27caaa4` (`V1__init_schema.sql`) | 23 Temmuz 2026 | UfkumDeniz |
| `FeedbackReason` enum'u | `0b450f0` | 22 Ağustos 2026 | UfkumDeniz |
| `resolveReason()` karar ağacı | `0b450f0` "feedback algorithm" | 22 Ağustos 2026 | UfkumDeniz |

Yani **kolon v5 şemasından geliyor** (Temmuz), **karar ağacı v6 çalışmasıyla geldi**
(22 Ağustos).

**Bir karar maddesine dayanıyor mu:** Elimdeki v6'nın §9 uygulama sırasında ve §10
kapsam dışı listesinde bu mekanizmadan söz edilmiyor. §3.3'ün üç dallı tablosu
dolaylı dayanak sayılabilir — tablo üç ayrı "Olmadı" dalını farklı puan etkileriyle
tanımlıyor ve kod tam olarak onu üretiyor. A8 ("teşhis zincirine dördüncü halka:
yorgunluk, açlık") §10'da kapsam dışı; ama o **dördüncü** halkayı erteliyor, ilk üçünü
değil.

**Açık kalan:** karar matrisinde bu mekanizmayı onaylayan bir madde var mı,
göremiyorum. Karar matrisine erişimim yok.

---

## 7 · S1–S7

### S1 · Nerede belirleniyor
`FeedbackLearningService.resolveReason()` satır 230-253. Karar ağacı ve girdiler
bölüm 1'de.

### S2 · Olası değerler
Enum: `FeedbackReason { SENSORY, INVOLVEMENT, INTEREST }`. Veritabanında
`VARCHAR(15)` + `CHECK (resolved_reason IN ('SENSORY','INVOLVEMENT','INTEREST'))`,
`V1__init_schema.sql:654-655`. `DEFAULT` yok. DISLIKED dışındaki oylarda `NULL`.

Pratikte **iki** değer üretilebiliyor — `INVOLVEMENT` ulaşılamaz (bölüm 3).

### S3 · Puan etkileri
Bölüm 2'deki tablo. `child_sensory_adjustments`'a yazan kod yok.

### S4 · Ne zaman eklendi
Bölüm 6.

### S5 · Serbest metin ve LLM

**Serbest metin `resolved_reason`'ı hiç etkilemiyor.** `submit()` içinde
`normalizeFreeText()` (satır 104-108) metni yalnız kırpıp `Feedback` satırına yazıyor;
`resolveReason()`'a parametre olarak bile geçmiyor.

**LLM çakışması yok, çünkü LLM tarafı hiç çalışmıyor:**

* `FeedbackLlmClassification` entity'si ve repository'si var, ama
  `src/main` içinde **tek bir okuma ya da yazma çağrısı yok.**
* `SensoryHint { NOISE, VISUAL, MOVEMENT, CROWDING }` enum'u tanımlı, hiçbir yerde
  kullanılmıyor.
* `applyDifficultyHint()` (satır 177-188) **public**, ama üretim kodunda çağıranı yok —
  yalnız `FeedbackLearningServiceDifficultyHintTest` çağırıyor.

Öncelik sorusunun cevabı: **tek mekanizma var**, o da `resolveReason()`.

### S6 · Deterministik mi

**Evet.** Fonksiyon saf: girdileri `feedback_type`, `dunn_quadrant`, üç yük değeri,
`separation_anxiety`, `involvement_type` ve iki tablo sabiti. Rastgelelik, zaman,
sayaç ya da geçmiş oy yok. Aynı çocuk + aynı etkinlik + aynı oy → her zaman aynı sonuç.

Doğrulama: 237 oyun hepsi yük üçlüsüne göre tam olarak öngörülebilir şekilde
sınıflandı; tek istisna yok.

### S7 · Dağılım

Canlı veritabanına erişemiyorum. Yerel ölçümüm bölüm 4'teki tabloda — 237 oy,
altı profil, tam havuz taraması. Canlıda koşulacak sorgu:

```sql
SELECT s.dunn_quadrant AS tip, f.resolved_reason, count(*)
FROM feedback f
JOIN child_profile_snapshots s
  ON s.child_id = f.child_id AND s.is_current
WHERE f.feedback_type = 'DISLIKED'
GROUP BY 1, 2
ORDER BY 1, 2;
```

Beklenen: C1/C2/MIXED satırlarında **yalnız** `INTEREST`, C3/C4'te karışık,
`INVOLVEMENT` hiç yok. Farklı çıkarsa canlıdaki sürüm bu commit değildir.

---

## 8 · Pedagojik değerlendirme

**Mekanizmanın fikri savunulabilir.** "Anne olmadı dedi; etkinlik bu çocuğun duyusal
eşiğini aşıyorsa bunu ilgisizlik sanma" doğru bir ayrım. Sebebi anneye sormadan,
elimizdeki iki veriden (çocuğun tipi, etkinliğin yükü) çıkarmak makul bir
yaklaşım — özellikle onboarding'i uzatmamak istiyorsak.

**Uygulaması yarım.** İki somut kusur:

1. **Kapı yanlış yerde.** Tolerans karşılaştırması doğru araç, ama yalnız C3/C4'e
   uygulanıyor. C1 çocuğun toleransı da var (3/3/3) ve havuzda onu aşan 7 etkinlik
   var. O çocukta duyusal tepki hiç tanınmıyor, ilgi puanı haksız düşüyor.

2. **Teşhisin sonucu yok.** `SENSORY` çözümlendiğinde v6 §3.3 "yük eşiği sıkılır"
   diyor; kodda karşılığı yok. Yani mekanizma bir şey **öğrenmiyor**, yalnız bir
   cezayı **atlıyor**. Yarısı yazılmış.

**Öğrenilemeyen ne:** C3/C4 çocukta, yükü toleransı aşan bir etkinlik gerçekten
sıkıcı olduğu için "olmadı" aldıysa, o zekâ alanı hakkında bilgi kaybediliyor.
Bu kabul edilebilir bir takas — yanlış cezalandırmaktansa öğrenmemek daha güvenli.
Ama takas **yalnız C3/C4'te** yapılıyor, diğerlerinde tersi yapılıyor.

---

## 9 · Öneri: **(a)**, tek düzeltmeyle

Görevin (a) şıkkı iki koşula bağlanmıştı: *"kural tolerans karşılaştırmasına
dayanıyorsa (H2) **ve** C4 çocuklarda INTEREST de çıkabiliyorsa."*

**İki koşul da sağlanıyor** — H2 doğru (bölüm 3), C4'te %67 INTEREST çıkıyor
(bölüm 4). Veri (b)'yi desteklemiyor: kaldırmayı gerektiren "C4'te ilgi hiç
öğrenilemiyor" durumu **yok.**

(c)'nin önerdiği eşik — "iki eksen birden aşılsın" — **yanlış yönde bir daraltma
olur.** C4 havuzunda tek bir etkinlik bile iki ekseni birden aşmıyor (hepsi
`maks(ses,görsel) < 3` filtresinden geçmiş), yani o kural duyusal dalı C4'te tamamen
kapatırdı.

### Önerilen: mekanizma kalsın, v6'ya yazılsın, **kapı kaldırılsın**

Tek satırlık değişiklik: `resolveReason()` satır 238'deki
`if (quadrant == C3 || quadrant == C4)` koşulu kalksın, tolerans karşılaştırması
**her tipe** uygulansın.

Gerekçe:

* Kural zaten tipe duyarlı — eşikler `dunn_profiles`'tan geliyor. C2'nin toleransı
  4/4/5 olduğu için havuzda onu aşan yalnız 1 etkinlik var; kapı kaldırılsa bile
  C2'de neredeyse hiçbir şey değişmez. Kapı gereksiz.
* C1 ve MIXED'te 7'şer etkinlik doğru sınıflanmaya başlar.
* Kural tek cümleye iner ve v6'ya yazılabilir hâle gelir:
  *"Yük, çocuğun tipinin toleransını herhangi bir eksende aşıyorsa duyusal sayılır."*

**Sırayla ikinci iş** (bu düzeltmeden bağımsız, ama onsuz mekanizma yarım kalır):
`SENSORY` çözümlendiğinde `child_sensory_adjustments`'a yazan kod eklenmeli.
Aksi hâlde teşhis bir dosyaya not düşmekten ibaret.

**Karar sizin olmak üzere bir not:** Yukarıdaki öneri elimdeki v6'ya göre. Görev
tanımının alıntıladığı "bu sürümde tek dal" cümlesi gerçekten yeni bir v6 nüshasında
varsa, o zaman durum tersine döner ve **(b)** doğru olur — mekanizma erken yazılmış
demektir. **Önce o nüshanın var olup olmadığı netleşmeli.**

---

## 10 · Belgeleme taslakları

### 10.1 · v6'ya eklenecek metin

```
### 3.3b · "Olmadı" oyunun teşhisi

Anne "olmadı" dediğinde sistem sebebi sormaz. Bunun yerine, etkinliğin duyusal
yükünü çocuğun tipinin toleransıyla karşılaştırarak üç daldan birine atar.

  SENSORY      -> Gardner'a dokunulmaz, çocuğun yük eşiği bir kademe sıkılır
  INVOLVEMENT  -> Gardner'a dokunulmaz, katılım filtresi STRICT'e çekilir
  INTEREST     -> g[hedef] -= 0.15

Karar ağacı:

  ses > tip.ses_toleransı
  VEYA görsel > tip.görsel_toleransı
  VEYA hareket > tip.hareket_toleransı
      -> SENSORY

  aksi hâlde, kaygı >= 4 VE etkinlik BAGIMSIZ
      -> INVOLVEMENT

  aksi hâlde
      -> INTEREST

Karşılaştırma kesin büyüktür; eşitlik aşma sayılmaz. Tek eksenin aşması yeterlidir.
Toleranslar dunn_profiles tablosundan gelir, koda gömülü değildir.

Gerekçe: Sebebi anneye sormak onboarding'i ve her geri bildirimi uzatır. Elimizde
zaten iki veri var: çocuğun duyusal tipi ve etkinliğin ölçülmüş yükü. Yük eşiği
aşıyorsa "olmadı" büyük ihtimalle duyusal bir tepkidir; o zekâ alanını
cezalandırmak yanlış olur.

Sınırlılık: Yükü yüksek bir etkinlik gerçekten sıkıcı olduğu için reddedildiyse,
o zekâ alanı hakkında bilgi kaybedilir. Bu bilinçli bir takastır: yanlış
cezalandırmaktansa öğrenmemek tercih edilir.

Kapsam dışı: Yorgunluk ve açlık gibi dördüncü halka bu sürümde yok (A8).
```

### 10.2 · Kod tarafı

**Adlandırma.** `resolveReason` ne yaptığını söylemiyor; "hangi sebebi çözümlüyor"
belirsiz. Öneri: metot `diagnoseDislikeReason`, ya da mantık ayrı bir sınıfa çıkarılıp
`DislikeReasonPolicy.diagnose(...)`. Diğer eşleştirme kuralları zaten
`service.matching` altında ayrı politika sınıflarında (`ActivityEligibilityPolicy`,
`ActivityFreshnessPolicy`); bu da onlarla aynı hizaya gelir ve tek başına test
edilebilir olur.

**Sabitler nerede.** Toleranslar zaten `dunn_profiles` tablosundan geliyor, koda
gömülü değil — bu iyi. `attachment_anxiety_threshold` da
`scoring_parameters`'tan geliyor. Koda gömülü kalan **iki karar var:**

| Gömülü karar | Nerede | Öneri |
|---|---|---|
| Kapı: yalnız C3/C4 | satır 238 | Kaldırılmalı (bölüm 9) |
| `>` mü `>=` mi, `VEYA` mı `VE` mi | satır 241-243 | v6'ya yazılırsa gömülü kalabilir; parametreleştirmeye değmez |

Yeni bir `scoring_parameters` anahtarı gerekmiyor.

---

## 11 · Özet

| Bulgu | Durum |
|---|---|
| Mekanizma nerede | `FeedbackLearningService.resolveReason()` 230-253 |
| Kural | Tip kapısı (C3/C4) + tolerans karşılaştırması, tek eksen, kesin büyüktür |
| Deterministik mi | ✅ evet, saf fonksiyon |
| **C4'te ilgi öğrenimi kilitleniyor mu** | ✅ **hayır** — %67 INTEREST çıkıyor |
| **C1/C2/MIXED'te duyusal teşhis** | ❌ **hiç çalışmıyor** — C1'de 7 etkinlik yanlış sınıflanıyor |
| **`INVOLVEMENT` dalı** | ❌ **ulaşılamaz** — 237 oyda 0 kez üretildi |
| `SENSORY`'nin filtre etkisi | ❌ yazılmamış — `child_sensory_adjustments` boş kalıyor |
| Serbest metin / LLM | — hiç çalışmıyor, çakışma yok |
| Tolerans kaynağı tutarlılığı | ⚠️ teşhis temel, skor ayarlanmış tolerans kullanıyor |
| v6 ile çelişki | **Elimdeki v6'da yok.** Görevin alıntıladığı metin bende mevcut değil |

**Öneri: (a)** — mekanizma kalsın, v6'ya yazılsın, tip kapısı kaldırılsın.
Şartı: görevin alıntıladığı "tek dal" metninin gerçekten yeni bir v6 nüshasında olup
olmadığı netleşsin. Varsa öneri **(b)**'ye döner.
