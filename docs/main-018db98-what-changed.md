# Yeni main `018db98` · Ne değişti, hangi bulgular kapandı

Tarih: 25 Ağustos 2026
Önceki taban: `1468948` (V29–V33) → yeni: **`018db98`** (PR #38, V34–V38)
Yöntem: diff okuma + sıfırdan veritabanı (38 migration) + gerçek API üzerinden senaryo koşumu

Şimdiye kadarki beş raporum `1468948`'e göreydi. Ufkum'un gönderdiği yeni main'i kurup
hepsini yeniden ölçtüm.

---

## 0 · Özet

**Bildirdiğim engelleyici bulguların dördü kapanmış.** Aynı gün yeniden plan üretimi
yazılmış, seçim 500'ü düzelmiş, `dailyPlanItemId` yanıta eklenmiş, duyusal ayarlama
tablosuna yazan kod gelmiş.

**Üç bulgu açık kalıyor:** `mvn test` hâlâ kırmızı, API yuva sırası hâlâ ters,
saat dilimi hâlâ ayarlanmamış.

**Ada'nın gözlenen 2. gün planı bu sürümde de çıkmıyor** — ama artık daha iyi bir
açıklama var: aynı gün çok tur üretiliyor ve gözlenen üç etkinlik **üç ayrı tura**
dağılmış durumda. Ayrıntı bölüm 4'te.

---

## 1 · Kapanan bulgular

### 1.1 · Aynı gün yeniden plan üretimi — **yazılmış**

Bu, `plan-not-regenerated-root-cause.md`'nin kök nedeniydi. İki parça hâlinde çözülmüş:

**`V37__allow_multiple_daily_plan_rounds.sql`** şemadaki engeli kaldırıyor:

```sql
ALTER TABLE daily_plans DROP CONSTRAINT uq_daily_plans_child_date;
DROP INDEX idx_daily_plans_child;
CREATE INDEX idx_daily_plans_child ON daily_plans (child_id, plan_date DESC, id DESC);
```

**`ActivityMatchingService.today()`** artık turu kontrol ediyor:

```java
Optional<DailyPlan> currentRound = planRepository
        .findFirstByChildIdAndPlanDateOrderByIdDesc(child.getId(), today);
if (currentRound.isPresent() && !currentRound.get().isRoundCompleted()) {
    return response(currentRound.get(), child);
}
// ... aksi hâlde YENİ tur kurulur
```

`DailyPlan.isRoundCompleted()` (satır 93-95): üç item da `completed_at` almışsa tur bitmiş
sayılıyor. Yani **üç geri bildirim tamamlanınca sonraki çağrı yeni bir tur üretiyor.**

Canlı ölçüm — Ada profiliyle aynı gün içinde beş tur:

| Tur | Plan | Toplam | Aynı gün plan sayısı |
|---|---|---|---|
| 1 | 201 · 206 · 199 *(gözlenene sabitlendi)* | 30 dk | 1 |
| 2 | 232/20 · 107/20 · 155/10 | 50 dk (taahhüt 40) | 2 |
| 3 | 153/10 · 113/20 · 108/15 | 45 dk | 3 |
| 4 | 14/15 · 154/5 · 110/10 | 30 dk | 4 |
| 5 | 208/10 · 157/10 · 198/10 | 30 dk | 5 |

Beş tur, beş ayrı `daily_plans` satırı, hepsi bugün tarihli. **Mekanizma çalışıyor.**

### 1.2 · İkinci etkinliği seçerken HTTP 500 — **düzelmiş**

`plan-not-regenerated-root-cause.md` · B1. İki parça:

**`V35__allow_new_selection_after_activity_completion.sql`** kısmi tekil indeksi
gevşetiyor — artık yalnız **bitmemiş** seçim tekil:

```sql
CREATE UNIQUE INDEX uq_daily_plan_items_selected_per_plan
    ON daily_plan_items (daily_plan_id)
    WHERE selected_at IS NOT NULL AND completed_at IS NULL;
```

**Flush sırası** da ayrıştırılmış — `DailyPlan.prepareSelection()` eski seçimi temizliyor,
`ActivityMatchingService.selectActivity()` arada `saveAndFlush` çağırıyor:

```java
plan.prepareSelection(activityId);
planRepository.saveAndFlush(plan);   // eski satır once temizlenir
plan.select(activityId);
```

Kodun kendi yorumu bunu açıkça söylüyor: *"Keeping this as a separate step lets
persistence flush the old active row before setting the new one, which is required by
the partial unique index."* Bildirdiğim mekanizmanın aynısı.

### 1.3 · Plan yanıtında `dailyPlanItemId` yok — **düzelmiş**

`plan-not-regenerated-root-cause.md` · B2. `DailyActivityResponse`'un ilk alanı artık
`Long dailyPlanItemId`. Canlı doğruladım:

```
STRENGTHEN  id=157  dailyPlanItemId=17
DEVELOP     id=208  dailyPlanItemId=16
EXPLORE     id=198  dailyPlanItemId=18
```

Artık plan ekranından üç etkinliğe doğrudan oy verilebiliyor; önce seçim yapma
zorunluluğu kalkmış.

Yanıt ayrıca epey zenginleşmiş: yaş aralığı, hedef/ikincil zekâ, alan, zorluk, katılım
tipi, üç yük, adımlar, malzemeler, kazanımlar, güvenlik ve toparlama notları,
`completedAt` / `selectedAt`.

### 1.4 · Duyusal ayarlama tablosuna yazan kod — **gelmiş, ama tek koldan**

`resolved-reason-mechanism.md`'de "döngü kapalı" demiştim: `child_sensory_adjustments`
okunuyor ama hiçbir yerde yazılmıyordu. Artık yazılıyor:

```java
@Transactional
public void applySensoryHint(Long childId, SensoryHint hint) {
    ChildSensoryAdjustment adjustment = requiredSensoryAdjustment(childId);
    short step = matchingParameters.load().get("llm_sensory_tolerance_step").shortValueExact();
    adjustment.applySensoryAdjustment(hint, step);
    sensoryAdjustmentRepository.save(adjustment);
}

@Transactional
public void applyInvolvementHint(Long childId, InvolvementHint hint) { ... }
```

**Ama şunu net söylemem gerekiyor:** bu iki metodun tek çağıranı
`applyIndependentHints(childId, activity, parsed)` — yani **LLM'in serbest metinden
çıkardığı** ipuçları. `resolveReason()` hâlâ satır 332'de duruyor, hâlâ satır 90'da
çağrılıyor, ve ürettiği `SENSORY` / `INVOLVEMENT` değerleri **hâlâ hiçbir yerde
okunmuyor**.

Yani döngü kapandı, ama **annenin serbest metin yazması şartıyla.** Metin yazmazsa
`resolveReason` yine yalnız `feedback` satırına not düşüyor.

> `INVOLVEMENT`'in ulaşılamaz olduğu bulgum da bununla ilgili: artık
> `applyInvolvementHint` ile `involvement_filter = RELAXED` yazılabildiği için
> kaygılı çocuğun havuzuna `BAGIMSIZ` etkinlik girebiliyor. Yani dal teorik olarak
> canlanabiliyor — ama yalnız LLM o ipucunu üretirse.

---

## 2 · Açık kalan bulgular

| Bulgu | Durum | Kaynak rapor |
|---|---|---|
| `mvn test` BUILD FAILURE | ❌ **aynen duruyor** | `v6-code-review-1468948.md` |
| API yuva sırası ters | ❌ **aynen duruyor** | `plan-not-regenerated...` · B3 |
| Saat dilimi ayarlanmamış | ❌ aynen duruyor | `plan-not-regenerated...` · B4 |
| Teşhis kapısı yalnız C3/C4'te | ❌ aynen duruyor | `resolved-reason-mechanism.md` |
| 36-48 ay içerik açığı | ❌ aynen duruyor | `explore-slot-and-budget-floor.md` |

### 2.1 · `mvn test` hâlâ kırmızı

```
Tests run: 153   Failures: 0   Errors: 1   Skipped: 26
BUILD FAILURE
```

Test sayısı 100'den 153'e çıkmış — iyi. Ama `KinloopBackendApplicationTests.contextLoads`
aynı sebeple düşüyor:

```
No qualifying bean of type 'com.kinloop.backend.repository.ConsentDocumentRepository'
```

25 repository var, test 12'sini elle sağlıyor, **13'ü eksik** — liste
`1468948`'dekiyle birebir aynı.

**Bu sürümde durum kötüleşti:** `ConsentDocumentRepository` ve
`FeedbackLlmClassificationRepository` artık üretim kodunda **aktif olarak kullanılıyor**
(`ConsentService`, `FeedbackClassificationService`), yani eksik bean listesi büyümeye
devam edecek. Elle liste tutma yaklaşımı her yeni özellikte bu testi kıracak.

### 2.2 · Yuva sırası

`PlanSlotType {STRENGTHEN, DEVELOP, EXPLORE}` ve `response()` hâlâ `ordinal()`'a göre
sıralıyor. Canlı yanıt:

```
STRENGTHEN → DEVELOP → EXPLORE
```

v6 §4 ve §6.1 Gelişim'i başa koyuyor. Tek satırlık iş, hâlâ açık.

---

## 3 · Değişen davranışlar — bilinmesi gerekenler

Bunlar hata değil, ama önceki raporlarımın bazı cümlelerini geçersiz kılıyor.

### 3.1 · Bütçe artık ayrı bir onboarding adımı

`CreateChildRequest`'ten `dailyTimeBudgetOptionCode` **kaldırılmış**. Çocuk oluşturulurken
bütçe sorulmuyor; ayrı bir uç var:

```
PUT /api/children/{childId}/onboarding/daily-time-budget   { "optionCode": "C" }
→ { "answeredOptionCode": "C", "minMinutes": 35, "maxMinutes": 45 }
```

`V36` `children` tablosuna `daily_time_budget_answered_at` ekliyor. Kolonun yorumu
mantığı açıklıyor: *"Presence means the parent explicitly submitted the child budget
screen; defaults alone are not progress."* Yani varsayılan 25-35 ile ekranı geçmiş
olmak arasındaki fark artık kayıtlı.

**Uyarı:** bu ucu çağırmadan plan istenirse çocuk varsayılan 25-35 ile kalıyor.
Ölçtüm — ilk denemede bütçe C gönderdiğimi sanırken çocuk 25-35 çıktı, çünkü artık
o alan çocuk oluşturma isteğinde yok sayılıyor. Önyüzün bu adımı atlamadığından emin
olunmalı.

### 3.2 · Tazelik penceresi artık bugünü de kapsıyor

```diff
- WHERE child_id = :childId AND plan_date < :beforeDate
- ORDER BY plan_date DESC
+ WHERE child_id = :childId AND plan_date <= :throughDate
+ ORDER BY plan_date DESC, id DESC
```

Aynı gün içinde çok tur üretildiği için zorunlu bir değişiklik: 2. tur, 1. turun
etkinliklerini görmeli. `id DESC` ikincil sıralaması aynı günün turlarını doğru
sıralıyor.

Bu benim `high-score-not-selected.md`'deki S2 cevabımı günceller: pencere artık
"son N plan, **bugün dahil**".

### 3.3 · `today()` artık onay istiyor

```java
consentService.requireAllRequiredConsents(userId);
```

Zorunlu onaylar verilmemişse **HTTP 403** dönüyor:

```json
{"error":"All active required consents must be granted before generating activities","status":403}
```

Beş onay belgesi var: TERMS, PRIVACY, KVKK, MARKETING, DATA_PROCESSING.
Uçlar: `GET /api/consents`, `PUT /api/consents/{consentId}`.

### 3.4 · LLM serbest metin sınıflandırması yazılmış

`resolved-reason-mechanism.md`'de "LLM tarafı hiç çalışmıyor, çakışma yok" demiştim.
**Artık çalışıyor.** Yeni paket `service/llm/`:

```
AnthropicMessagesClient        FeedbackClassificationParser
FeedbackClassificationPrompt   FeedbackClassificationService
LlmProperties                  ParsedClassification
SecondhandReportDetector       FeedbackClassificationOutcome
```

506 satırlık few-shot prompt dosyası:
`src/main/resources/prompts/feedback-classification-few-shot.md`.
`SynchronousFeedbackSubmissionService` geri bildirimi sınıflandırmayla birlikte işliyor.
`V38` serbest metni 500 karakterle sınırlıyor.

Sınıflandırmadan çıkan ipuçları üç yere gidiyor: zorluk ipucu → basamak sayacı,
duyusal ipucu → tolerans ayarı, katılım ipucu → katılım filtresi.

---

## 4 · Ada'nın gözlenen planı bu sürümde de çıkmıyor

Senaryoyu yeni main'de baştan kurdum: onaylar verildi, bütçe ekranı C ile geçildi,
profil C3/kaygı 3/odak MEDIUM, 1. tur gözlenen hâle sabitlendi (201/206/199), üç oy
verildi.

**2. tur: 232 / 107 / 155** — `1468948`'dekiyle aynı. Gözlenen 153/154/155 değil.

Ama artık daha iyi bir açıklama var. Gözlenen üç etkinlik **ayrı turlarda** çıkıyor:

| Etkinlik | Hangi turda çıktı | Skor |
|---|---|---|
| **155** Hikâyeyi birlikte tamamlama | tur 2 · EXPLORE | 108.0 |
| **153** Duygu kartları sessiz eşleştirme | tur 3 · DEVELOP | 108.0 |
| **154** Teşekkür turu | tur 4 · STRENGTHEN | 88.0 |

Üçü de gerçekten öneriliyor, sadece aynı planda değil. Aynı gün çok tur üretildiği için
testi yapan kişinin **farklı turları tek plan sanmış olması** mümkün — ekranda hangi
turun gösterildiği önyüz tarafında ayırt edilebiliyor mu, bilmiyorum.

**Bunu kesinleştirmek için canlıdan tek bir sorgu yeter:**

```sql
SELECT p.id AS tur, p.plan_date, p.created_at,
       i.slot_type, i.activity_id, i.score, i.selected_at, i.completed_at
FROM daily_plans p
JOIN daily_plan_items i ON i.daily_plan_id = p.id
WHERE p.child_id = <Ada'nın id'si>
ORDER BY p.plan_date, p.id, i.slot_type;
```

153, 154 ve 155 **aynı `tur` değerinde** görünüyorsa gerçek bir sapma var ve
`high-score-not-selected.md` bölüm 06'daki üç şey aranmalı. **Farklı turlarda**
görünüyorsa sapma yok, gözlem turların karışmasından geliyor.

---

## 5 · Sırada ne var

| # | İş | Kim |
|---|---|---|
| 1 | Yukarıdaki SQL canlıda koşulsun — sapma gerçek mi, tur karışması mı | Ufkum |
| 2 | `KinloopBackendApplicationTests` düzeltilsin, `mvn test` yeşile dönsün | Ufkum |
| 3 | Yuva sırası Gelişim → Güçlendirme → Keşif yapılsın | Ufkum |
| 4 | Önyüz bütçe ekranını atlamıyor mu doğrulansın (3.1) | ürün |
| 5 | 36-48 ay · LANGUAGE · ≥15 dk · d=2 içerik açığı | içerik |
| 6 | Teşhis kapısının C3/C4 kısıtı kaldırılsın mı — karar | ürün |

Önceki beş raporum `1468948`'e göre yazılmıştı; yukarıdaki 1.1-1.4 maddeleri onların
kapanan kısımlarını gösteriyor. Kalanlar hâlâ geçerli.
