# "Ertesi gün yeni plan üretilmiyor" · Kök neden raporu

Tarih: 25 Ağustos 2026
İncelenen kod: `1468948` (main, V29–V33)
Yöntem: kod okuma + yerelde senaryonun birebir yeniden kurulması (gerçek HTTP çağrıları)

**Canlı veritabanına erişimim yok** (Neon `neondb` ve `kidloop-fe.vercel.app` çıkış
vekilinde engelli). Onun yerine aynı 33 migration'la yerel bir veritabanı kurup Mavi
senaryosunu baştan sona tekrar koştum. Yerel kopya canlıyla **birebir aynı sonucu**
üretti (aşağıda kanıt), o yüzden kod tarafındaki teşhis geçerli. Yalnız canlı satırlara
bakmayı gerektiren iki soruyu işaretledim.

---

## 0 · Önce bir düzeltme: v6'da §4.8 yok

Görev tanımı "v6 §4.8" diye bir maddeye atıf yapıyor. Bende olan iki v6 nüshasında da
(16 Ağustos ve **21 Ağustos 11:58 revizesi**) **4. bölüm 4.7'de bitiyor.** §4.8 yok.

`Kidloop_Oneri_Prensibi_v6.md` §4.7'nin tam metni:

> ### 4.7 Plan kaydı
> Günlük plan **bir kez üretilir** ve `daily_plans` tablosuna kaydedilir. Anne aynı gün
> ekranı yenilediğinde plan yeniden hesaplanmaz, kayıtlı plan döner (EK7).

"Üç geri bildirim tamamlandı → algoritma yeniden çalışır" kuralı bende olan v6
metninde **hiç geçmiyor**; unit test dokümanında da geçmiyor.

Yani kod ile v6 arasında bir çelişki yok — **kod §4.7'yi harfiyen uyguluyor.**
Çelişki, v6 ile görev tanımındaki §4.8 arasında. İki ihtimal var, hangisi olduğuna karar
vermek bana düşmez:

* Ya bende olmayan daha yeni bir v6 nüshası var ve §4.8 orada,
* Ya da §4.8 henüz karara bağlanmış ama v6'ya yazılmamış bir madde.

**Bu ayrım kritik**, çünkü Ufkum'a "kodda hata var, düzelt" demekle "yeni bir gereksinim
var, yaz" demek aynı şey değil. Aşağıdaki 1. ve 4. bölümler bunu ayırıyor.

---

## 1 · Kök neden

**Tek cümleyle:** Aynı gün için ikinci bir plan üretilmesi kodda değil, **veritabanı
şemasında** engelli — `daily_plans` tablosunda `UNIQUE (child_id, plan_date)` kısıtı var
ve plan üreten tek metot (`ActivityMatchingService.today()`) o günün satırını bulduğu an
yenisini hesaplamadan geri dönüyor.

`ActivityMatchingService.java:49-55`:

```java
Child child = childRepository.findLockedById(requestedChild.getId()).orElseThrow();
LocalDate today = LocalDate.now();
Optional<DailyPlan> existing = planRepository.findByChildIdAndPlanDate(child.getId(), today);
if (existing.isPresent()) return response(existing.get(), child);       // <-- kapı burada

ChildProfileSnapshot profile = profileRepository.findByChildIdAndCurrentTrue(child.getId())
        .orElseThrow(() -> new IllegalStateException("Child onboarding profile is missing"));
```

**Satır sırası burada kanıt niteliğinde.** Duyusal tipi ve kaygı puanını taşıyan
`ChildProfileSnapshot` satır 54'te, yani **erken dönüşten sonra** okunuyor. Kapı yalnız
iki şeye bakıyor: çocuk kimliği ve bugünün tarihi. Profili görmesi fiziken mümkün değil.

`daily_plans` kısıtı (canlı ve yerelde doğrulandı):

```
uq_daily_plans_child_date -> UNIQUE (child_id, plan_date)
```

Yani "üç geri bildirim tamamlandı, yeni etkinlik öner" akışı **hiç yazılmamış.** Eksik
olan bir koşul ya da bozuk bir bayrak değil; mekanizma yok. Geri bildirim kaydeden kodun
(`FeedbackLearningService.submit()`, satır 54-84) plan üretimini çağıran tek bir satırı
bile yok — ne doğrudan, ne olay, ne zamanlanmış iş.

---

## 2 · Mevcut tetikleme mantığı

```
GET /api/children/{id}/daily-plan/today
        │
        ├─ daily_plans'ta (child_id, BUGÜN) satırı var mı?
        │      EVET ──> kayıtlı planı döndür.  ALGORİTMA ÇALIŞMAZ.        [satır 50]
        │      HAYIR ─┐
        │             ├─ havuz filtresi  ─ skorla ─ yuvaları doldur
        │             ├─ daily_plans'a YAZ  (plan_date = BUGÜN)
        │             └─ planı döndür
        │
POST /api/children/{id}/daily-plan/items/{itemId}/feedback
        │
        ├─ feedback satırı yaz
        ├─ Gardner puanlarını güncelle
        ├─ basamak sayacını güncelle
        ├─ item.complete()          <-- completed_at damgalanır
        └─ DÖNER.  PLAN ÜRETİMİNİ ÇAĞIRAN HİÇBİR ŞEY YOK.       [satır 74-84]
```

Algoritmayı çalıştıran tek koşul: **o çocuk için bugün tarihli satır olmaması.**
Pratikte bu, gece yarısı (sunucu saat diliminde) demek.

---

## 3 · §4.8 ile farkı

| | Görev tanımındaki §4.8 | Kodun yaptığı | v6 §4.7 |
|---|---|---|---|
| Ertesi gün | yeni plan | ✅ yeni plan | ✅ |
| Üç geri bildirim tamamlanınca | **yeni etkinlik önerilir** | ❌ hiçbir şey olmaz | metinde yok |
| Aynı gün ekran yenileme | — | kayıtlı plan döner | ✅ "yeniden hesaplanmaz" |

Kod §4.7'yi tam uyguluyor. §4.8 diye bir davranış kodda **yok** ve v6'da da yok.

---

## 4 · Ufkum'un yapması gereken

**Not:** Bu bir hata düzeltmesi değil, **yeni bir yetenek.** Ölçüsü de küçük değil —
şema kısıtı değişiyor.

### 4.1 · Önce ürün kararı (kod öncesi)

"Üç geri bildirim sonrası üretilen şey" nedir?

* **(a) Yeni bir tam plan mı?** O zaman `uq_daily_plans_child_date` kalkmalı ve
  `daily_plans` bir "üretim sırası" kolonu almalı (`generation_index` gibi). Tazelik
  penceresi `plan_date < bugün` yerine "önceki N üretim"e bakmalı
  (`DailyPlanRepository.findActivityIdsInRecentPlans`, satır 18-30 — şu an
  `plan_date < :beforeDate` diyor, aynı günün önceki üretimini göremez).
* **(b) Var olan plana eklenen tek etkinlik mi?** O zaman `daily_plans` dokunulmaz,
  `DailyPlan.add()`'deki "bu yuva zaten dolu" ve "bu etkinlik zaten var" kontrolleri
  (satır 62-67) gevşetilmeli, `daily_plan_items`'a bir "tur" kolonu gelmeli.

v6 §4.7 ve EK7 (a)'yı yasaklıyor. Bu yüzden karar önce ürün tarafında verilmeli.

### 4.2 · Kod tarafında dokunulacak yerler

| Dosya | Metot / satır | Yapılacak |
|---|---|---|
| `ActivityMatchingService.java` | `today()`, satır 49-51 | Erken dönüş koşulu değişmeli: "bugün satır var mı" yerine "yeni üretim tetiklendi mi" |
| `FeedbackLearningService.java` | `submit()`, satır 74-84 | `item.complete()` sonrası "planın üç item'ı da tamamlandı mı" kontrolü; tamamlandıysa üretim çağrısı |
| `DailyPlanRepository.java` | `findActivityIdsInRecentPlans`, satır 18-30 | `plan_date < :beforeDate` aynı günün önceki üretimini eliyor; pencere tanımı yeniden yazılmalı |
| `DailyPlan.java` | `add()`, satır 61-68 | (b) seçilirse yuva/etkinlik tekilliği kısıtları gevşemeli |
| yeni migration | `daily_plans` | (a) seçilirse `uq_daily_plans_child_date` kaldırılmalı |
| `HomeService.java` | `getStatus()`, satır 62-67 | `shouldGenerateDailyPlan` şu an "son seçilen item tamamlandı mı"ya bakıyor; yeni kurala göre yeniden tanımlanmalı |

**Kod yazmadım, tarif ettim** — görevin kısıtı gereği.

---

## 5 · S1–S7 ve K1–K5

### S1 · Plan üretimi hangi koşulda tetikleniyor

**Var olan planı döndürüyor.** Karar kriteri tam olarak `plan_date = LocalDate.now()`
(`ActivityMatchingService.java:49-50`). Başka hiçbir kriter yok — geri bildirim sayısı,
tamamlanma durumu, seçim durumu hiçbiri bakılmıyor.

`daily_plans`'ta `child_id = 57` için kaç satır olduğunu **söyleyemem** — canlıya
erişimim yok. Kod mantığına göre gün başına en fazla bir satır olabilir; şema da
(`uq_daily_plans_child_date`) bunu garanti ediyor. Doğrulamak için:

```sql
SELECT id, plan_date, committed_duration_minutes, total_duration_minutes, fallback_level
FROM daily_plans WHERE child_id = 57 ORDER BY plan_date;
```

### S2 · Üç geri bildirim tamamlanınca ne oluyor

**Plan üretimini tetikleyen kod yok.** `FeedbackLearningService.submit()` (satır 54-84)
tam olarak şunları yapıyor: feedback satırını yazar, Gardner puanlarını günceller,
basamak sayacını günceller, `item.complete()` çağırır, yanıtı döndürür. "Üç item da
geri bildirim aldı mı" kontrolü **hiçbir yerde yok** — tüm `src/main` içinde böyle bir
sayım yapılmıyor.

**`shouldGenerateDailyPlan` nerede hesaplanıyor:** `HomeService.getStatus()`, satır
40-67. Mantık şu:

```
onboarding tamamlanmamış               -> "new-user",         alan hiç dönmez (null)
hiç seçilmiş etkinlik yok              -> "returning-user",   shouldGenerate = TRUE
son SEÇİLEN item tamamlanmamış         -> "feedback-required", shouldGenerate = FALSE
son SEÇİLEN item tamamlanmış           -> "returning-user",   shouldGenerate = TRUE
```

Dikkat: bu bayrak **"yeni plan üretilecek" demek değil.** Yalnız önyüze "plan
endpoint'ini çağırabilirsin" diyor. Endpoint çağrıldığında yine kayıtlı plan dönüyor.
Yerelde ölçtüm: bayrak `true` oldu, endpoint çağrıldı, **aynı `planId` döndü.**

### S3 · "state" alanı

Durum makinesi yok; her istekte yukarıdaki dört daldan biri seçiliyor. Tanımlı
durumlar `HomeStatusResponse.java`'da sabit: `new-user`, `returning-user`,
`feedback-required`.

**`feedback-required`'dan çıkış koşulu:** son *seçilen* `daily_plan_items` satırının
`completed_at` alanının dolması. Onu dolduran tek yer `DailyPlanItem.complete()`, onu
çağıran tek yer `FeedbackLearningService.submit()`.

**Canlıda neden `feedback-required` göründüğünü söyleyemem.** Rapora göre üç geri
bildirim de kaydedilmiş; o durumda üç item da `completed_at` almış olmalı ve kod
`returning-user` döndürmeliydi — yerelde tam olarak bunu ölçtüm. Canlıda tersi
görüldüyse iki ihtimal var: canlıdaki sürüm `item.complete()` çağrısını içermeyen daha
eski bir commit, ya da "son seçilen" item o üç item'dan biri değil. Ayırt etmek için:

```sql
SELECT i.id, i.activity_id, i.selected_at, i.completed_at,
       (SELECT count(*) FROM feedback f WHERE f.daily_plan_item_id = i.id) AS fb
FROM daily_plan_items i
JOIN daily_plans p ON p.id = i.daily_plan_id
WHERE p.child_id = 57
ORDER BY i.selected_at DESC NULLS LAST;
```

`selected_at` en yeni olan satırın `completed_at`'ı boşsa kod doğru davranıyor demektir.

### S4 · Tarih karşılaştırması

* `plan_date` tipi: **`date`** (timestamp değil). Doğruladım.
* Saat dilimi: **hiçbir yerde ayarlanmamış.** `application.yml`'de, uygulama sınıfında,
  Docker/deploy tarafında bir `TZ` ya da `spring.jackson.time-zone` yok.
  `LocalDate.now()` JVM'in varsayılan saat dilimini kullanıyor — konteynerlerde bu
  neredeyse her zaman **UTC**.

  **Sonucu:** Türkiye'deki anne için gün, yerel saatle **03:00'te** dönüyor. 00:00-03:00
  arasında "ertesi gün" hâlâ dünkü plan sayılıyor. Bu ayrı bir kusur, ama bildirilen
  hatanın sebebi değil.

* Görev tanımındaki ayrım doğru: senaryo **aynı gün** içinde. Kodun davranışı
  **§4.7 açısından doğru.** §4.8 bende olan v6'da olmadığı için "yanlış" diyemem —
  bölüm 0'a bakın.

### S5 · 24 saat kilidi

**24 saatlik bir kilit kodda yok.** `ActivityMatchingService`, `HomeService`,
`FeedbackLearningService`, `DailyPlan`, `DailyPlanItem` sınıflarında saat/süre
karşılaştırması yapan tek satır yok.

**Ama gözlenen "diğer ikisi kilitleniyor" davranışının gerçek bir sebebi var — ve o bir
hata:**

`V15__add_daily_plan_item_selection.sql`:

```sql
CREATE UNIQUE INDEX uq_daily_plan_items_selected_per_plan
    ON daily_plan_items (daily_plan_id)
    WHERE selected_at IS NOT NULL;
```

Bir planda aynı anda yalnız bir etkinlik seçili olabiliyor. `DailyPlan.select()`
(satır 89-99) önce diğerlerinin `selected_at`'ini temizleyip sonra yenisini işaretliyor.
Ama Hibernate flush sırası bu iki UPDATE'i ters çeviriyor: yeni satır işaretlenirken eski
satır hâlâ işaretli oluyor ve **kısıt patlıyor.**

Yerelde ölçtüm — birinci etkinlik seçildikten sonra ikincisini seçmek:

```
POST /api/children/17/daily-plan/today/selection  {"activityId": 8}
-> HTTP 500
ERROR: duplicate key value violates unique constraint "uq_daily_plan_items_selected_per_plan"
   at ActivityMatchingService.selectActivity
   at DailyPlanController.selectActivity (DailyPlanController.java:44)
```

**Bu, üç geri bildirimin tamamlanmasını engelleyebilir** — S5'in sorduğu şey buydu.
Anne birinci etkinliği seçip oy verdikten sonra ikinciye geçemiyor; ekran 500 alıyor.

Geri bildirim endpoint'inin kendisi seçim şartı aramıyor, o yüzden önyüz item id'lerini
başka yerden biliyorsa üçüne de oy verilebiliyor (canlıda öyle olmuş görünüyor). Ama
"seç → oy ver → sıradakine geç" akışı bozuk.

> **Yan bulgu:** `daily-plan/today` yanıtındaki `DailyActivityResponse`
> `dailyPlanItemId` alanını **hiç içermiyor** (`DailyActivityResponse.java`, 16 alan).
> Oysa geri bildirim endpoint'i `/items/{dailyPlanItemId}/feedback`. Item id'yi veren tek
> yer `/api/home/status` ve orası **tek bir item** gösteriyor (son seçilen). Yani plan
> ekranından doğrudan üç etkinliğe oy verilemiyor; her biri için önce seçim yapmak
> gerekiyor — ve o seçim yukarıdaki 500'ü veriyor.

### S5b · İki vakanın ortak noktası — **aynı kök neden, ölçüldü**

Deniz profilini de (30 ay, **C4**, kaygı **5**, bütçe A) yerelde baştan kurdum.

**1. gün planı — canlıdaki Deniz planıyla birebir aynı:**

| Yuva | id | Başlık | Süre | Katılım | Skor |
|---|---|---|---|---|---|
| DEVELOP | 141 | Fısıltı kuklası sohbeti | 10 dk | BIRLIKTE | 146.05 |
| STRENGTHEN | 223 | Uzun kukla hikâyesi | 15 dk | BIRLIKTE | 146.05 |
| EXPLORE | 71 | Bitki sulama görevi | 5 dk | **GOZETIMLI** | 95.0 |

Gözetimli garantisi Keşif yuvasında devreye girmiş, `within_budget = FALSE`.
Üç geri bildirim sonrası durum da birebir aynı çıktı:

```
VERBAL_LINGUISTIC  3.30      LANGUAGE  L1  streak 0.5
INTERPERSONAL      3.15      resolved_reason (DISLIKED) = SENSORY
diğer altı alan    3.00
```

**Sonra tam olarak Mavi'deki davranış:** `shouldGenerateDailyPlan = true` oldu, endpoint
çağrıldı, **aynı `planId` döndü**, `daily_plans` tek satırda kaldı.

Planı bir gün geriye alınca yeni plan sorunsuz üretildi — 142 / 183 / 144, kademe 0,
taahhüt 25, üçü de bütçe içinde, Keşif yine gözetimli. Yani **C4'te de motor doğru**.

**İki çocuğun `daily_plans` kayıtları:**

| Çocuk | Tip | Kaygı | Plan | Tarih | Bütçe | Taahhüt | Toplam | Kademe |
|---|---|---|---|---|---|---|---|---|
| 17 · Mavi | C1 | 2 | 14 | 24 Ağu | 15-25 | 25 | 30 | 0 |
| 17 · Mavi | C1 | 2 | 15 | 25 Ağu | 15-25 | 25 | 30 | 0 |
| 18 · Deniz | C4 | 5 | 16 | 24 Ağu | 15-25 | 25 | 30 | 0 |
| 18 · Deniz | C4 | 5 | 17 | 25 Ağu | 15-25 | 25 | 25 | 0 |

Yapı aynı: gün başına tek satır, kademe 0. Tek fark Deniz'in 2. gününde toplamın 25
olması — Keşif bütçeye sığdığı için.

**Aynı kod yolu mu çalışıyor:** evet, ve bu bir gözlem değil, yapısal bir zorunluluk.
Erken dönüş kapısı (satır 52) profil okunmadan önce çalışıyor; `HomeService.getStatus()`
ise `ChildProfileSnapshot`'a hiç dokunmuyor. Tetikleme yolunda **profile bağlı tek bir
dal yok**. Duyusal tip ve kaygı yalnız üretim adımında (havuz filtresi, gözetimli
garantisi) rol oynuyor, o da ancak kapı açıldıktan sonra çalışıyor.

Hatanın C1 ve C4'te tekrarlaması bu yüzden şaşırtıcı değil — **her profilde tekrarlar.**

### S6 · Referans planı doğrula — **tuttu**

Yerelde senaryoyu baştan kurdum: 30 aylık, C1, kaygı 2, bütçe şıkkı A (15-25).

**1. gün planı — canlıyla birebir aynı:**

| Yuva | id | Başlık | Süre |
|---|---|---|---|
| DEVELOP | 74 | Market rol oyunu mini | 15 dk |
| STRENGTHEN | 8 | Kukla ile sohbet | 10 dk |
| EXPLORE | 146 | Sessiz ritim: parmak ucuyla vuruş | 5 dk |

taahhüt 25, toplam 30, kademe 0, Keşif `within_budget = FALSE`.

**Üç geri bildirim sonrası durum — canlıyla birebir aynı:**

```
INTERPERSONAL      3.30      LANGUAGE  L1  streak 0.5
VERBAL_LINGUISTIC  3.15      diğer altı alan L1 streak 0.0
MUSICAL            2.85
diğer beş alan     3.00      resolved_reason (DISLIKED) = INTEREST
```

**2. gün planı — planı bir gün geriye alıp endpoint'i tekrar çağırdım:**

| Yuva | id | Başlık | Süre | Skor | Beklenen |
|---|---|---|---|---|---|
| DEVELOP | **59** | Resimli kitapla hikâye tamamlama | 15 dk | **129.0** | 59 · 129.0 ✅ |
| STRENGTHEN | **182** | Yardım et oyunu | 10 dk | **104.0** | 182 · 104.0 ✅ |
| EXPLORE | **143** | Yavaş nefes balonu sessiz | 5 dk | **94.0** | 143 · 94.0 ✅ |

```
taahhüt 25 dk        toplam 30 dk        kademe 0
Keşif within_budget = FALSE      repeat_notice = FALSE (üçünde de)
```

**Üç etkinlik de, üç skor da, üç yuva da referansla aynı.** Sapan terim yok.

Havuz ve tazelik de doğrulandı:

```
havuz  = 44   (HOME, PUBLISHED, 30 ay, süre <= 25 dk — SQL ile saydım)
N      = maks(2, tavan(44/6)) = maks(2, 8) = 8 plan
elenen = 74, 8, 146   ->   44 - 3 = 41
```

Referans dokümandaki "44 -> 41" ile aynı.

**id 182'nin seçilmesi de doğrulandı.** INTERPERSONAL 3.30'a çıktığı için Güçlendirme
yuvası o alandan seçti. Yani **geri bildirimin ertesi güne etkisi çalışıyor.** Motorda
sorun yok; sorun motorun ne zaman çalıştırıldığında.

### S7 · Tohum formülü — **tam**

`CandidateOrdering.comparator()` beş kademenin hepsini uyguluyor:

```java
Comparator.comparing(ScoredActivity::rawScore, Comparator.reverseOrder())   // 1 skor
    .thenComparingInt(candidate -> sampleCount(candidate, intelligenceScores)) // 2 örnekleme
    .thenComparingInt(this::totalSensoryLoad)                                  // 3 duyusal yük
    .thenComparingInt(candidate -> candidate.activity().getDurationMinutes())  // 4 SÜRE  ✅
    .thenComparing(candidate -> seed(childId, planDate, ...));                 // 5 TOHUM ✅
```

Tohum `BigInteger` ile, `gün = YYYYMMDD`, çarpanlar veritabanından
(`tiebreak_seed_a` 1000003, `tiebreak_seed_b` 10007, `tiebreak_seed_mod` 2147483647).
4. ve 5. kademe **var.**

---

### K1–K5

| # | Kontrol | Sonuç |
|---|---|---|
| K1 | `daily_plan_items.within_budget` | ✅ var, doluyor. 2. gün planında Keşif `false`, diğer ikisi `true` |
| K2 | `daily_plans.committed_duration_minutes` / `total_duration_minutes` | ✅ var, 25 / 30 — doğru |
| K3 | `daily_plans.fallback_level` | ✅ var, `0` yazıyor |
| K4 | Yuva sırası | ❌ **Önyüze yanlış sırayla gidiyor** — aşağıda |
| K5 | `resolved_reason = INTEREST` | **Çıkarım.** Anneye sorulmuyor |

**K4 ayrıntı — gerçek bir kusur.**

`PlanSlotType` enum'unun tanım sırası:

```java
public enum PlanSlotType {STRENGTHEN, DEVELOP, EXPLORE}
```

`ActivityMatchingService.response()` (satır 125) item'ları
`Comparator.comparingInt(x -> x.getSlotType().ordinal())` ile sıralıyor. Enum sırası
STRENGTHEN=0 olduğu için API **Güçlendirme → Gelişim → Keşif** döndürüyor.

Yerel çıktıdan, birebir:

```
STRENGTHEN  id=182   Yardım et oyunu
DEVELOP     id=59    Resimli kitapla hikâye tamamlama
EXPLORE     id=143   Yavaş nefes balonu sessiz
```

Veritabanında sıra doğru (item 43=DEVELOP, 44=STRENGTHEN, 45=EXPLORE), API bozuyor.

v6 §4: *"Plan her zaman üç etkinliktir: Gelişim, Güçlendirme, Keşif. Seçim ve gösterim
sırası aynıdır."* §6.1'in anne ekranı örneği de Gelişim'i başa koyuyor.

Düzeltme enum sırasını değiştirmek **değil** — `slot_type` veritabanında metin olarak
saklanıyor, enum sırası veriyi etkilemiyor ama `ordinal()`'a başka yerde güvenilmiş
olabilir. En güvenlisi `response()` içinde açık bir sıra tanımlamak.

**K5 ayrıntı.** `SubmitActivityFeedbackRequest` yalnız iki alan taşıyor:
`feedbackType` ve isteğe bağlı `freeText`. Sebep alanı yok. Sebebi
`FeedbackLearningService.resolveReason()` (satır 230-253) çıkarıyor:

```
DISLIKED değilse                                     -> null
C3/C4 ve etkinlik toleransı aşıyorsa                 -> SENSORY
kaygı >= 4 ve etkinlik BAGIMSIZ ise                  -> INVOLVEMENT
aksi hâlde                                           -> INTEREST      <-- varsayılan dal
```

Mavi C1 ve kaygı 2 olduğu için ilk iki dal hiç çalışmıyor; `INTEREST` **varsayılan
dalın çıktısı**, annenin beyanı değil. Geri bildirim soruları da (`scope = FEEDBACK`)
yalnız iki tane: `FB_ENJOYMENT` ("Çocuğun etkinlikten keyif aldı mı?") ve `FB_COMMENT`.
Sebep sorulmuyor.

> Buna bağlı, önceki raporumda da bildirdiğim açık: `SENSORY` ve `INVOLVEMENT` sabitleri
> hesaplanıp `feedback` satırına yazılıyor ama kodun başka hiçbir yerinde okunmuyor.
> `child_sensory_adjustments` tablosu var, okunuyor, ama **hiçbir yerde yazılmıyor.**

---

## 6 · Özet

| Bulgu | Ağırlık |
|---|---|
| Aynı gün ikinci plan üretimi yok; mekanizma hiç yazılmamış, şema da yasaklıyor | Bildirilen hatanın kök nedeni |
| İkinci etkinliği seçmek HTTP 500 veriyor (`uq_daily_plan_items_selected_per_plan`) | **Bağımsız, ciddi hata** |
| `daily-plan/today` yanıtı `dailyPlanItemId` içermiyor; oy vermek için tek yol seçim | Yukarıdakini kilitliyor |
| API yuva sırası Güçlendirme → Gelişim → Keşif; v6 Gelişim'i başa koyuyor | Kusur, kolay |
| Saat dilimi ayarlanmamış; gün Türkiye'de 03:00'te dönüyor | Kusur |
| Duyusal/ayrılık sebepleri hesaplanıyor ama hiçbir yere yazılmıyor | Kapalı döngü |
| Motorun kendisi: 59 / 182 / 143 · 129.0 / 104.0 / 94.0 | ✅ referansla birebir |

**Motor doğru çalışıyor.** Bildirilen hata motorun ne ürettiğiyle değil, ne zaman
çalıştırıldığıyla ilgili — ve o kural henüz yazılmamış.
