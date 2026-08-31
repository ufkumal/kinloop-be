# Kidloop · Kod tabanı durum raporu

Ölçüm tarihi: **31 Ağustos 2026**
Ölçülen: `ufkumal/kinloop-be` · main · **`0a2ef84`**
Yöntem: kod okuma + git geçmişi + sıfırdan veritabanı (39 migration) + `mvn test`

**Bu belge yalnız kodda ölçüleni içerir.** Canlı ortama (Vercel, Neon) erişimim yok;
oraya bağlı her madde bölüm 9'da.

---

## 0 · Tek paragraf özet

Backend, v6 öneri motorunu ve v4 LLM sınıflandırma katmanını uygulamış durumda; her iki
şartnamenin de sayısal ve yapısal maddeleri kodda birebir doğrulanıyor. Öneri motoru
bağımsız referans motorla aynı planları üretiyor, LLM prompt'u v4 şartnamesinin 41
örneğiyle bayt bayt aynı, ve v4'ün üç temel semantik kararı (`target_correction` kredi
tutma, etkinliğin ikincilinin korunması, `secondary_hint`'in üçüncü alana toplanması)
kodda doğru yazılmış. Buna karşılık **`mvn test` kırmızı** (158 testte 1 hata + 1
başarısızlık), LLM çağrısı şartnamenin istediği gibi asenkron değil **senkron**,
onboarding soru metinlerinin yarısında Türkçe karakter yok, izin belgelerinin beşi de
İngilizce, ve marka adı kodda `Kinloop`/`Kidloop` diye ikiye bölünmüş durumda. Canlı
ortamın hangi commit'i koştuğu ve veritabanında ne olduğu bu ölçümün dışında kaldı.

---

## 1 · Sürüm ve altyapı

### 1.1-1.2 · Sürüm

| | |
|---|---|
| main HEAD | **`0a2ef8479d08e91a082fc84d068e91e428c1a1d8`** (`0a2ef84`) |
| Tarih | 26 Ağustos 2026, 11:35:54 +0300 |
| Başlık | Merge pull request #39 from ufkumal/ufkum_v4 |
| `018db98`'den beri | **2 commit** (1 merge + 1 içerik) |
| İçerik commit'i | `6406e78` · “few_shot_v4 implemented” · 26 Ağu 11:02 |
| Konu | v4 LLM semantiği + `V39` + prompt dosyası + testler · 11 dosya, +549/−274 |

**26 Ağustos'tan bu yana kod değişmemiş** (5 gün).

Uzak dallar: `main`, `ufkum_v4`, `feature/ufkum-init-1`, `codex/main-with-claude-kinloop`,
`claude/kinloop-migration-model-check-yjkw3w`, `claude/kinloop-be-v5-analysis-xz18nn`,
`copilot/kinloop-backend-initial-setup`.

### 1.3 · Migration'lar (V34'ten itibaren)

| # | Dosya | Tek satır |
|---|---|---|
| V34 | `remove_other_gender_identity_option` | Q8'den `OTHER` şıkkı silinir, `UNDISCLOSED` sırası 3 olur |
| V35 | `allow_new_selection_after_activity_completion` | Seçim tekil indeksi `completed_at IS NULL` ile daraltılır; tamamlanan seçim sonraki etkinliği engellemez |
| V36 | `persist_onboarding_resume_marker` | `children.daily_time_budget_answered_at` eklenir; bütçe ekranının geçildiği kaydedilir |
| V37 | `allow_multiple_daily_plan_rounds` | `uq_daily_plans_child_date` düşürülür; aynı gün çok tur mümkün olur, indeks `(child_id, plan_date DESC, id DESC)` olur |
| V38 | `limit_feedback_free_text_length` | `FB_COMMENT` max 500; `feedback.free_text` için `NOT VALID` CHECK |
| V39 | `set_llm_max_affected_domains` | `scoring_parameters`'a `llm_max_affected_domains = 3` (upsert) |

Toplam **39 migration**, hepsi temiz uygulanıyor (sıfırdan kurulum doğrulandı).

### 1.4 · `mvn test`

```
Tests run: 158   Failures: 1   Errors: 1   Skipped: 26
BUILD FAILURE
```

| Durum | Test | Sebep |
|---|---|---|
| **ERROR** | `KinloopBackendApplicationTests.contextLoads` | `No qualifying bean of type 'ConsentDocumentRepository'`. Test 25 repository'nin 12'sini elle sağlıyor, **13'ü eksik** |
| **FAILURE** | `FeedbackClassificationPromptTest.containsV4SafetyRules` | Test `"Kâğıtları fırlattı, ağladı, çok öfkelendi"` arıyor; v4 §6 Ö39 ve prompt dosyası **`"Parçaları fırlattı"`** diyor. **Testin kendisinde yazım hatası**, prompt doğru |
| Skipped 26 | `RecommendationScenarioTest` (7) · `PostgreSqlMigrationIntegrationTest` (19) | `@Testcontainers(disabledWithoutDocker = true)`, ölçüm ortamında Docker yok |

Eksik 13 repository: `Activity`, `ChildDomainLevel`, `ChildIntelligenceScore`,
`ChildSensoryAdjustment`, `ConsentDocument`, `DevelopmentalPeriodTask`, `DunnProfile`,
`FeedbackEffect`, `FeedbackLlmClassification`, `Feedback`, `Recommendation`,
`ScoringParameter`, `UserConsent`.

Depoda `.github/workflows` **yok** — otomatik CI kapısı bulunmuyor.

### 1.5 · Canlı ortam

**Doğrulanamadı.** Bölüm 9.

### 1.6 · LLM ayarları (`application.yml`)

| Anahtar | Değer | v4 §11.1 |
|---|---|---|
| `llm.enabled` | `${LLM_ENABLED:true}` | — |
| `llm.api-key` | `${ANTHROPIC_API_KEY:}` | — |
| `llm.model` | `${LLM_MODEL:claude-haiku-4-5}` | ✅ `claude-haiku-4-5` |
| `llm.temperature` | `0` | ✅ `0` |
| `llm.max-tokens` | `200` | ✅ `200` |
| İstek zaman aşımı | 30 sn (`AnthropicMessagesClient:51`) | §11'de belirtilmemiş |

Uç nokta `https://api.anthropic.com/v1/messages`, JDK `HttpClient` ile elle yazılmış;
projede Anthropic SDK bağımlılığı **yok**.

---

## 2 · v6 kararlarının uygulanma durumu

`Kidloop_Oneri_Prensibi_v6.md` §0 elimdeki nüshada **19 madde** içeriyor (görev 21 diyor;
fark bölüm 9'da).

| # | Karar | Kodda | Nerede | Canlı |
|---|---|---|---|---|
| 1 | Skor `(100−D+G+P+Z)×B`, T yok | ✅ | `ActivityScorer:40-46` · `freshness_penalty` parametresi silinmiş (V33) | ? |
| 2 | Tazelik eleme, dinamik pencere | ✅ | `ActivityFreshnessPolicy:13-20` · `freshness_window_divisor=6`, `_min=2` | ? |
| 3 | C4 eleme `maks(ses,görsel) ≥ 3` | ✅ | `ActivityEligibilityPolicy:20-22` — hareket ekseni yok | ? |
| 4 | C4 ağırlıkları `5/5/3` | ✅ | `dunn_profiles`: tolerans `1/2/2`, ağırlık `5.00/5.00/3.00` | ? |
| 5 | Son yaş bandı 48-**73** | ✅ | `developmental_period_tasks`: `48-73 SOCIAL_EMOTIONAL` | ? |
| 6 | `is_scaffolded` → `easier_variation` | ✅ | Kolon yok; `ActivityScorer:115` `hasEasierVariation` kullanıyor | ? |
| 7 | Sıralı doldurma (toplam skor değil) | ✅ | `DailyPortfolioBuilder.fill():65-121` — Gelişim → Güçlendirme → Keşif | ? |
| 8 | Deterministik eşitlik bozma | ✅ | `CandidateOrdering:21-25` · tohum `1000003 / 10007 / 2147483647` | ? |
| 9 | 5 kademeli geri çekilme | ✅ | `DailyPortfolioBuilder.build():21-43` · KADEME 0-4 | kademe 0 dışı tetiklenmedi |
| 10 | Plan bir kez üretilip kaydedilir | ⚠️ **değişti** | `V37` bu kısıtı kaldırdı; artık **tur bazlı** — aşağı bak | ? |
| 11 | Bütçe aralığı, üç şık | ✅ | Q7: A `Kısa ve öz olsun` 15-25 · B `Yarım saatim var` 25-35 · C `Rahatça vakit ayırabiliriz` 35-45 | ? |
| 12 | Sıralı bütçe tüketimi, rezervli | ✅ | `DailyPortfolioBuilder:77-84` · rezerv = havuzun en kısası, tek yuvalık | ? |
| 13 | Birlikte ×1.15 + gözetimli garantisi | ✅ | `ActivityScorer:132-141` · `applySupervisedGuarantee():123-165` | ? |
| 14 | `L ∈ [1,4]` + tavan kuralı | ✅ | `chk_child_domain_levels_level CHECK (level >= 1 AND level <= 4)` · `ceiling_counter_cap = 1.0` | ? |
| 15 | Basamak başlangıcı yaşa göre | ✅ | `under_48m=1`, `48_to_60m=2`, `60_to_73m=3` · `MatchingStateInitializer:38-44` | ? |
| 16 | Zorluk duyarlı sayaç | ✅ | `FeedbackLearningService.domainDelta()` · `stretch=1.0`, `at_level=0.5`, `below=0.0`, `struggle_stretch=−0.5`, `struggle_at_level=−1.0` | ? |
| 17 | Gelişim alanı 7 değer | ✅ | `activities`'te 7 farklı `target_domain` | ? |
| 18 | Yayın doğrulaması (EK8) | ✅ | `trg_activities_validate_published` tetikleyicisi kurulu (V32) | ? |
| 19 | Test veritabanını okusun (EK3) | ✅ | `RecommendationScenarioTest:47-50` — “The database, rather than a migration file parser, is the source” | Docker yok, koşmadı |

### Görevin özel olarak sorduğu maddeler

| Soru | Cevap |
|---|---|
| B1 · 0-24 ayda C şıkkı gizleniyor mu | ✅ `OnboardingService.availableForAge():157-159` → `ageMonths >= 24 \|\| !"C".equals(code)` |
| B3 · `d=L+1 → +1.0`, `d=L → +0.5` | ✅ parametrelerle doğrulandı (madde 16) |
| EK4 · tohum formülü v6 §4.3 ile aynı mı | ✅ `(childId×1000003 + YYYYMMDD×10007 + activityId) mod 2147483647`, `BigInteger` |
| EK7 · plan bir kez üretilip kaydediliyor mu | ⚠️ artık **tur** bazlı, aşağı bak |
| §4.8 · üç oy tamamlanınca yeni plan | ✅ `ActivityMatchingService.today():52-57` + `DailyPlan.isRoundCompleted():93-95` |

**EK7 ↔ §4.8 gerilimi.** v6 §4.7/EK7 “plan bir kez üretilir, aynı gün yeniden
hesaplanmaz” diyor. `V37` + `isRoundCompleted()` bunu **tur bazlı**ya çevirdi: tur
bitmemişse kayıtlı plan döner (EK7 korunur), tur bittiyse yeni tur üretilir (§4.8).
Ölçtüm: aynı gün içinde **5 tur** üretilebiliyor. Bu, §4.8'i uygulamak için EK7'nin
gevşetilmesi anlamına geliyor; şartname metninde bu ikisinin nasıl bağdaştığı yazılı
değil.

---

## 3 · LLM katmanı

### 3.1 · Kod yolu

Şartname (v4 §11.2) **asenkron, `submit()` sonrası** istiyor. Kod **senkron ve
`submit()` öncesi**.

| Adım | Sınıf · satır |
|---|---|
| 1 · Uç nokta | `DailyPlanController.submitFeedback()` → `POST /api/children/{childId}/daily-plan/items/{dailyPlanItemId}/feedback` |
| 2 · Sarmalayıcı | `SynchronousFeedbackSubmissionService.submit():25-49` |
| 3 · Ön kontrol | Aynı item'a ikinci oy engeli, etkinlik okunur (`:37-45`) |
| 4 · Model çağrısı | `FeedbackClassificationService.classify():32-73` → `AnthropicMessagesClient.complete()` |
| 5 · Ayrıştırma | `FeedbackClassificationParser.parse()` → `ParsedClassification` |
| 6 · Yazma | `FeedbackLearningService.submit(child, itemId, request, outcome):73-137` |
| 7 · Tabloya kayıt | `persistClassification()` → `feedback_llm_classifications` |

**Event yok, `@Async` yok, `@TransactionalEventListener` yok** — tüm kaynakta bu üç
anotasyondan hiçbiri geçmiyor. Sınıfın kendi yorumu bunu açıkça söylüyor:
*“Keeping the provider call synchronous while ensuring it runs outside the DB write
transaction.”*

Sonuç fonksiyonel olarak denk (buton kredisi henüz yazılmadığı için “geri alma”
gerekmiyor, “hiç uygulama” yeterli), ama **anne LLM çağrısını bekliyor** — v4 §11.2'nin
birinci gerekçesi buydu.

### 3.2 · Canlı satır sayısı

**Doğrulanamadı.** Bölüm 9.

### 3.3 · Prompt sürümü

**v4.0.** Ölçtüm:

| Kontrol | Sonuç |
|---|---|
| Örnek sayısı | **41 / 41** — v4 §12 ile aynı |
| Örnek içeriği | v4 §6 ile **bayt bayt aynı** (41 örneğin girdisi ve JSON çıktısı tek tek karşılaştırıldı, fark yok) |
| Grup yapısı | A1 YALANLAMA · A2 EKLEME · A3 DEĞİNMEME · A4 KARŞITLIK · B–N — v4 §6 ile aynı |
| Sistem talimatı (§4) | **59 / 59 satır aynı**, eksik ya da fazla satır yok |
| Kullanıcı mesajı (§5) | Aynı beş alan, aynı sıra; `FeedbackType` Türkçeleştiriliyor |

Konum: sistem talimatı `FeedbackClassificationPrompt.INSTRUCTIONS` (gömülü metin
bloğu), örnekler `src/main/resources/prompts/feedback-classification-few-shot.md`
(599 satır), ikisi `SYSTEM_PROMPT` içinde birleştiriliyor (`:90-92`).

### 3.4 · v4'ün üç temel kararı

Üçü de **uygulanmış**. `FeedbackLearningService.applyGardnerLearning():168-200`:

```java
IntelligenceType targetType    = activity.getTargetIntelligence();
IntelligenceType secondaryType = activity.getSecondaryIntelligence();
if (type == FeedbackType.LIKED) {
    if (targetCorrection != targetType) {                    // 1 · kredi TUTULUR
        applyDelta(target, liked_target_delta);               //     (+0.30 verilmez)
    }
    if (secondaryType != null && secondaryType != targetType) {
        applyDelta(secondary, liked_secondary_delta);         // 2 · etkinliğin ikincili KORUNUR
    }
    if (secondaryHint != null
            && secondaryHint != targetType
            && secondaryHint != secondaryType) {
        applyDelta(secondaryHint, liked_secondary_delta);     // 3 · ÜÇÜNCÜ alana toplamsal
    }
}
```

| v4 kararı | Durum | Not |
|---|---|---|
| `target_correction` → hedefin kredisi verilmez | ✅ | `revertTargetCredit` adlı ayrı metot **yok**; senkron akışta kredi hiç yazılmadığı için “geri alma” gerekmiyor, koşul doğrudan atlıyor |
| Etkinliğin kendi ikincili her zaman `+0.15` | ✅ | `targetCorrection`'dan etkilenmiyor |
| `secondary_hint` üçüncü alana toplamsal `+0.15` | ✅ | `applySecondaryHint` adlı ayrı metot yok, aynı blokta |

**§10'un dört yeni doğrulaması** `sanitizeClassification():206-249`'da:

| §10 kontrolü | Kod |
|---|---|
| `DISLIKED` + `target_correction` → `null`, `conflict=false` | ✅ `:214-216` |
| `secondary_hint == etkinlik.hedefZeka` → `null` | ✅ `:224` |
| `secondary_hint == etkinlik.ikincilZeka` → `null` | ✅ `:225` |
| Etkilenen alan > `llm_max_affected_domains` → `secondary_hint` `null` | ✅ `:232-248` |

Ek: `target_correction` etkinliğin hedefinden farklıysa `null` sayılıyor (`:217-219`);
`LIKED` + dolu `target_correction` → `conflict = true` (`:220-222`).

### 3.5 · `llm_max_affected_domains`

**3** — `V39__set_llm_max_affected_domains.sql`, veritabanında doğrulandı.

### 3.6 · Kancalar

| Metot | Var mı | Çağrılıyor mu |
|---|---|---|
| `applySensoryHint(childId, hint)` | ✅ `FeedbackLearningService:235-241` | ✅ `applyIndependentHints():256-262` |
| `applyInvolvementHint(childId, hint)` | ✅ `:243-248` | ✅ aynı yer |
| `applyDifficultyHint(childId, domain, hint)` | ✅ `:222-232` | ✅ aynı yer |
| `duration_hint` | — | Yalnız tabloya yazılıyor, puana etkisi yok (v4 §7 de böyle diyor) |

Filtre ipuçları **her oyla** uygulanıyor (oy türü kapısı yok) — v4 §7.1 ile uyumlu.

### 3.7 · `child_sensory_adjustments`

**Yazan kod var.** `ChildSensoryAdjustment.applySensoryAdjustment():56-66`:

```java
case NOISE    -> noiseAdjustment    -= step;
case VISUAL   -> visualAdjustment   -= step;
case MOVEMENT -> movementAdjustment -= step;
case CROWDING -> { noiseAdjustment -= step; visualAdjustment -= step; }
```

Yön doğru — tolerans **düşüyor** (v4 §7.1: “sıkılır = tolerans düşer”). `CROWDING` iki
eksene birden dokunuyor ✅. Adım `llm_sensory_tolerance_step = 1.0`.

**Tek giriş yolu LLM.** `resolveReason()`'ın ürettiği `SENSORY` / `INVOLVEMENT`
değerleri bu tabloya **yazmıyor**; anne serbest metin yazmazsa tolerans hiç değişmiyor.

Canlıda satır var mı: **doğrulanamadı**.

### 3.8 · TRANSIENT

`reverseEffects()` metodu var (`:139-157`) ama TRANSIENT yolunda **çağrılmıyor**. Bunun
yerine `submit():107-131` hiçbir şey uygulamıyor:

```java
boolean transientSituation = classificationApplicable
        && parsed.situationHint() == SituationHint.TRANSIENT;
if (!transientSituation) {
    applyGardnerLearning(...);
    applyDomainLearning(...);
}
if (classificationApplicable && !transientSituation) {
    applyIndependentHints(...);
}
```

Gardner, basamak sayacı ve filtre ipuçlarının üçü de atlanıyor — v4 §2.1 “Hiçbir puan
güncellemesi yapılmaz” ile sonuç aynı. Senkron mimarinin doğal karşılığı.

### 3.9 · KVKK

**Uygulanmış.** `FeedbackClassificationService:41-43`:

```java
if (!hasDataProcessingConsent(childId)) {
    return FeedbackClassificationOutcome.notAttempted();
}
```

`hasDataProcessingConsent()` (`:76-83`) çocuk → ebeveyn profili → kullanıcı zincirini
izleyip `ConsentService.hasGrantedConsent(userId, ConsentType.DATA_PROCESSING)` çağırıyor.
Onay yoksa metin modele **gitmiyor**.

> **Tarihçe notu:** Bu kontrol `018db98`'de yorum satırındaydı; `6406e78` (v4 commit'i)
> ile açılmış.

### 3.10 · Model

`claude-haiku-4-5` (varsayılan, `LLM_MODEL` ile değiştirilebilir). Sağlayıcı Anthropic.

---

## 4 · Açık bulguların durumu

| Bulgu | Konu | 26 Ağu | **31 Ağu** | Kanıt |
|---|---|---|---|---|
| B1 | Bütçe şık metinleri | Açık | ✅ **kapandı** | Q7: `Kısa ve öz olsun` 15-25 · `Yarım saatim var` 25-35 · `Rahatça vakit ayırabiliriz` 35-45 |
| B2 | Yuva sırası API'de ters | Açık | ❌ **açık** | `PlanSlotType {STRENGTHEN, DEVELOP, EXPLORE}` + `ActivityMatchingService.response()` `slotType.ordinal()` ile sıralıyor → API `STRENGTHEN → DEVELOP → EXPLORE` |
| B3 | “Pekiştirme” → “Güçlendirme” | Açık | — **kapsam dışı** | Backend'de Türkçe yuva adı hiç geçmiyor; ad yalnız önyüzde |
| B6 | `resolved_reason` kapısı yalnız C3/C4 | Açık | ❌ **açık** | `FeedbackLearningService:395` `quadrant == C3 \|\| quadrant == C4` |
| **B7** | **Yüksek skorlu etkinlik seçilmiyor** | Açık, en kritik | ❌ **main'de üretilemiyor** | Aşağı bak |
| B8 | Bütçe alt sınırı tutmuyor | Açık | ❌ **açık** | `DailyPortfolioBuilder.Request` yalnız `budgetMax` taşıyor; `daily_time_budget_min` plan kurulumuna hiç girmiyor |
| B9 | Türkçe karakterler eksik | Açık | ❌ **açık** | 14 sorunun **8'inde** Türkçe karakter yok. Örnek Q2: `Kalabalik, muzikli bir ortama girince cocugunuz:` |
| B10 | Marka adı | Açık | ❌ **açık** | `src/main` içinde `Kinloop` 6 dosyada, `Kidloop` 8 dosyada |
| B11 | İzin metinleri İngilizce | Açık | ❌ **açık** | Beş belgenin beşi de İngilizce: `Terms of Use`, `Privacy Notice`, `KVKK Aydinlatma Metni`, `Marketing Communications`, `Data Processing Consent` |
| B12 | A1 uyarısı yok | Açık | ❌ **açık** | Hiçbir izin belgesinde `tanı/teşhis/terapi/diagnos/therap` geçmiyor |
| B13 | Bilgilendirme ekranı (C1) | Açık | ⚠️ **kısmen** | Backend durumu tutuyor (`V31` üç kolon + `OnboardingService.getClosingMessage/respondToClosingMessage`), ama yanıt yalnız üç bayrak döndürüyor (`shouldDisplay`, `planReminderEnabled`, `reminderPlansRemaining`) — **metin backend'de yok** |
| — | Saat dilimi | Açık | ❌ **açık** | `application.yml`'de ve uygulama sınıfında `TZ` / `time-zone` / `ZoneId` yok; `LocalDate.now()` JVM varsayılanını kullanıyor |
| — | `mvn test` kırmızı | Açık | ❌ **açık, kötüleşti** | 1 hata + **1 yeni başarısızlık** (bölüm 1.4) |

### B7 · ayrıntı

`docs/high-score-not-selected.md` (31 Ağu itibarıyla yalnız
`claude/kinloop-migration-model-check-yjkw3w` dalında) kök nedeni **bulamadı, çünkü
sapma main'de üretilemedi.** Ölçüm:

* Ada'nın 1. günü rapordaki hâle sabitlendi, üç oy verildi, oy sonrası yedi değerin
  yedisi de raporunkiyle **aynı** çıktı — girdi tartışmasız.
* 2. tur: **232 (123.0) / 107 (109.0) / 155 (108.0)**. Yani yüksek skorlular seçiliyor.
* 232 ve 107 yedi havuz filtresinin hepsinden geçiyor, tazelik elemiyor, Gelişim
  sıralamasında **1. ve 2.** sıradalar, Gelişim tavanı `45 − 5 = 40` ve 20 dk sığıyor.
* Kodda skorun süreye bölünmesi **yok** (`grep divide src/main/java` → 0 sonuç).
* **Gözlenen planı üreten varyant bulundu:** sıralama `skor ÷ süre` + Güçlendirme
  yuvasının önce doldurulması → `153/10 · 154/5 · 155/10 = 25 dk`, gözlenenle birebir.
  Bu bir **hipotez**; deploy edilmiş kodu görmedim.

`service/matching/` paketi `1468948`'den bu yana **hiç değişmedi** (`git diff` boş), yani
bu ölçüm `0a2ef84` için de geçerli.

---

## 5 · Test durumu

| # | Soru | Cevap |
|---|---|---|
| 5.1 | 74 senaryodan kaçı otomatik | `RecommendationScenarioTest` **8 test metodu** içeriyor (7'si senaryo, Docker'sız atlanıyor). 74'ün tamamının karşılığı yok |
| 5.2 | `RecommendationScenarioTest` kaynağı | ✅ **veritabanı**. Testcontainers + `Flyway.locations("classpath:db/migration")`; sınıf yorumu: *“The database, rather than a migration file parser, is the source of the pool”* |
| 5.3 | `Kidloop_LLM_Sinama_Senaryolari.md` T1-T5 | **Doğrulanamadı** — belge bende yok, depoda da yok |
| 5.4 | Referans motor karşılaştırması | Yapıldı. `1468948` üzerinde **12/12** profil ve ayrıca 6 bütçe-C profilinde **6/6** birebir. Betikler: `docs/v6-reference-engine.py`, `docs/v6-scenario-runner.py`, `docs/v6-api-vs-reference.py` (yalnız claude dalında) |
| 5.5 | Canlı sayılar | **Doğrulanamadı** |

Test envanteri: **37 test sınıfı**, **161 `@Test`/`@ParameterizedTest`**, koşulan 158,
atlanan 26, 5285 satır test kodu.

---

## 6 · Proposal taahhütleri

Term Project Proposal belgesi bende **yok**; aşağıdaki satırlar yalnız görev tanımındaki
başlıklara karşılık kodda ne bulunduğunu söyler. “Karşılandı” yargısı proposal metni
görülmeden kesinleştirilemez.

| # | Taahhüt | Kodda bulunan | Değerlendirme |
|---|---|---|---|
| 6.1 | Multi-criteria activity matching | 5 terimli skor (D, G, P, Z, B) + 3 yuva + 5 kademeli eşitlik bozma + 5 kademeli geri çekilme | Karşılandı |
| 6.2 | Cold-start (pedagojik kurallar) | Onboarding → Dunn tipi, kaygı, odak; Gardner önsel değerleri; yaşa göre başlangıç basamağı; dönem görevi | Karşılandı |
| 6.3 | Unstructured feedback (LLM) | v4 prompt + Anthropic çağrısı + 9 alanlı şema + 6 güvenlik freni + `feedback_llm_classifications` | Kodda tam; **canlı çalıştığı doğrulanamadı** |
| 6.4 | Spring Boot REST API | Spring Boot **3.3.2**, 10 controller, **22 endpoint**, 929 satır `openapi.yaml` | Karşılandı |
| 6.5 | PostgreSQL | PostgreSQL sürücüsü + Flyway, **39 tablo**, **39 migration** | Karşılandı |
| 6.6 | Responsive React (web + mobil) | **Bu depoda yok** — ayrı önyüz deposu | Doğrulanamadı |
| 6.7 | Cloud deployment | `PORT`, `DB_URL`, `CORS_ALLOWED_ORIGINS` ortam değişkenleri var | Platform doğrulanamadı |
| 6.8 | JUnit testleri | **161 test**, `mvn test` **kırmızı** | Kısmen |
| 6.9 | Sentetik veri: 50 provider, 500 aile, ~3000 etkileşim | Böyle bir seed **yok**. Migration'larda 243 etkinlik + referans tabloları var; üretilmiş aile/etkileşim verisi yok | Karşılanmadı |
| 6.10 | İki taraflı eşleştirme (provider tarafı) | `activities.scope` yalnız `HOME` sorgulanıyor (`ActivityRepository:10`). `workshop_profiles`, `activity_workshop_details`, `activity_sessions` tabloları **var ama** eşleştirme motoru bunları kullanmıyor | Karşılanmadı |
| 6.11 | OpenAI API | **Anthropic'e geçilmiş** — `api.anthropic.com/v1/messages`, `claude-haiku-4-5`. Gerekçe kodda yazılı değil | Değişmiş, gerekçe doğrulanamadı |
| 6.12 | DB şeması, API dokümantasyonu, mimari karar kayıtları | Şema: 39 migration. API: `openapi.yaml` (929 satır). **ADR / mimari karar kaydı dizini yok** | Kısmen |

---

## 7 · Mimari özet

### 7.1 · Katmanlar

```
controller (10)  →  service (17 + matching 8 + llm 8)  →  repository (25)  →  entity (30)
                          ↑                                                      ↑
                     dto (45)                                          entity/enums (26)
```

Ek paketler: `mapper` (2), `security` (4), `exception` (21), `config` (1).

### 7.2 · Öneri motoru

| Sınıf | Görev |
|---|---|
| `ActivityMatchingService` | Akışı yönetir; tur kontrolü, onay kontrolü, kayıt |
| `MatchingParameters` | `scoring_parameters` yükler |
| `MatchingStateInitializer` | İlk planda 8 zekâ + 7 alan satırını kurar |
| `ActivityEligibilityPolicy` | Havuz filtresi 4-6 (C4 elemesi, kaygı, odak) |
| `ActivityFreshnessPolicy` | Dinamik tazelik penceresi |
| `ActivityScorer` | `(100 − D + G + P + Z) × B` |
| `CandidateOrdering` | 5 kademeli eşitlik bozma + tohumlu kura |
| `DailyPortfolioBuilder` | Yuva doldurma, rezerv, gözetimli garantisi, geri çekilme |
| `ScoredActivity` | `(activity, rawScore, displayScore, breakdown)` |

### 7.3 · Geri bildirim işleme

`SynchronousFeedbackSubmissionService` (sarmalayıcı) → `FeedbackLearningService`
(Gardner, basamak, teşhis, ipuçları) → `FeedbackEffect` kayıtları.
`FeedbackQuestionService` geri bildirim sorularını döndürür.

### 7.4 · LLM entegrasyonu

`FeedbackClassificationService` (orkestrasyon, KVKK, Fren 6) ·
`AnthropicMessagesClient` (HTTP) · `FeedbackClassificationPrompt` (sistem + 41 örnek) ·
`FeedbackClassificationParser` (§10 doğrulaması) · `ParsedClassification` ·
`FeedbackClassificationOutcome` · `SecondhandReportDetector` (regex) · `LlmProperties`.

### 7.5 · Toplam

| | |
|---|---|
| Java sınıfı (main) | **206** |
| Test sınıfı | 37 (39 dosya) |
| REST endpoint | **22** |
| Tablo | **39** |
| Migration | **39** |
| Satır (main java) | ~7 074 |
| Satır (test java) | ~5 285 |
| Satır (migration SQL) | ~7 584 |
| `openapi.yaml` | 929 satır |

### 7.6 · Dış bağımlılıklar

| | |
|---|---|
| Java | **21** |
| Spring Boot | **3.3.2** |
| Starter'lar | web, data-jpa, security, validation, test |
| Flyway | `flyway-core`, `flyway-database-postgresql` |
| JWT | `jjwt` 0.12.6 (api / impl / jackson) |
| Veritabanı | `postgresql` sürücüsü |
| Diğer | Lombok, JUnit Jupiter, spring-security-test, Testcontainers |
| **Anthropic SDK** | **Yok** — JDK `HttpClient` ile elle yazılmış |

### 7.7 · Uçtan uca izler

**Akış A · Plan üretimi**

```
GET /api/children/{childId}/daily-plan/today
  → DailyPlanController.today()
  → ChildService.getOwnedChild()
  → ActivityMatchingService.today()
      1  childRepository.findLockedById()                      satır kilidi
      2  planRepository.findFirstByChildIdAndPlanDateOrderByIdDesc()
         tur bitmemişse  → response(kayıtlı plan)               ÇIKIŞ
      3  consentService.requireAllRequiredConsents()            403 olabilir
      4  profileRepository.findByChildIdAndCurrentTrue()        child_profile_snapshots
      5  stateInitializer.initialize()                          child_intelligence_scores + child_domain_levels
      6  dunnRepository / periodRepository                      dunn_profiles + developmental_period_tasks
      7  activityRepository.findEligibleBasePool(age, budgetMax)
         + eligibilityPolicy.allows()                           C4 → kaygı → odak
      8  freshnessPolicy.windowSize() + eliminate()
      9  scorer.score()  her aday için
     10  candidateOrdering.comparator() ile sırala
     11  portfolioBuilder.build()                               yuvalar + geri çekilme
     12  recommendationRepository.save()  ×3                    recommendations
     13  plan.add() ×3 + planRepository.save()                  daily_plans + daily_plan_items
  → DailyPlanResponse
```

**Akış B · Geri bildirim**

```
POST /api/children/{childId}/daily-plan/items/{itemId}/feedback
  → DailyPlanController.submitFeedback()
  → SynchronousFeedbackSubmissionService.submit()
      1  serbest metin normalleştirilir
      2  varsa: FeedbackClassificationService.classify()        SENKRON, işlem dışında
             KVKK onayı → AnthropicMessagesClient.complete()
             → parser.parse() → Fren 6 tavanı
  → FeedbackLearningService.submit(child, itemId, request, outcome)   @Transactional
      3  resolveReason()                                        SENSORY / INVOLVEMENT / INTEREST
      4  feedbackRepository.save()                              feedback
      5  sanitizeClassification()                               §10 doğrulamaları
      6  TRANSIENT değilse:
             applyGardnerLearning()                             child_intelligence_scores + feedback_effects
             applyDomainLearning()                              child_domain_levels
      7  applyIndependentHints()                                child_sensory_adjustments
      8  persistClassification()                                feedback_llm_classifications
      9  item.complete()                                        daily_plan_items.completed_at
  → ActivityFeedbackResponse
```

**Event yayınlanmıyor.** Asenkron parça yok.

### 7.8 · Endpoint tablosu

| Method | Path | Ne yapar | Kimlik |
|---|---|---|---|
| POST | `/api/auth/register` | Ebeveyn kaydı | açık |
| GET | `/api/auth/verify` | E-posta doğrulama | açık (token) |
| POST | `/api/auth/login` | JWT üretir | açık |
| GET | `/api/consents` | Aktif izin belgeleri + kullanıcının kararı | JWT |
| PUT | `/api/consents/{consentId}` | İzni verir/geri alır | JWT |
| GET | `/api/profile` | Ebeveyn + çocuk profili | JWT |
| GET | `/api/home/status` | Ana ekran durumu (`new-user` / `returning-user` / `feedback-required`) | JWT |
| GET | `/api/home/feedback/questions` | Geri bildirim soruları | JWT |
| GET | `/api/onboarding/identity-questions` | Çocuk oluşturma soruları | JWT |
| POST | `/api/children` | Çocuk oluşturur | JWT |
| PUT | `/api/children/{childId}` | Çocuk günceller | JWT |
| GET | `/api/children/{childId}/onboarding/daily-time-budget` | Bütçe şıkları (yaşa göre) | JWT |
| PUT | `/api/children/{childId}/onboarding/daily-time-budget` | Bütçe şıkkını kaydeder | JWT |
| GET | `/api/children/{childId}/onboarding/closing-message` | Bilgilendirme ekranı bayrakları | JWT |
| PUT | `/api/children/{childId}/onboarding/closing-message` | “Başlayalım” / “Sonra hatırlat” | JWT |
| GET | `/api/children/{childId}/questionnaire/current` | Sıradaki soru | JWT |
| PUT | `/api/children/{childId}/questionnaire/answers/{questionCode}` | Cevap kaydeder | JWT |
| POST | `/api/children/{childId}/questionnaire/complete` | Onboarding'i kapatır, profil anlık görüntüsü üretir | JWT |
| GET | `/api/children/{childId}/daily-plan/today` | Günün planı / yeni tur | JWT |
| POST | `/api/children/{childId}/daily-plan/today/selection` | Etkinlik seçer | JWT |
| POST | `/api/children/{childId}/daily-plan/items/{dailyPlanItemId}/feedback` | Oy + serbest metin | JWT |
| GET | `/api/children/{childId}/activity-history` | Geçmiş etkinlikler | JWT |

### 7.9 · Veri modeli omurgası

Görevdeki zincir **doğrulandı**, iki düzeltmeyle: `feedback` doğrudan `daily_plan_items`'a
bağlı (plan'a değil), ve `recommendations` ayrı bir dal.

```
users ─1:1─ parent_profiles ─1:n─ children
                                     ├─1:n─ child_profile_snapshots   (is_current tek satır)
                                     ├─1:8─ child_intelligence_scores
                                     ├─1:7─ child_domain_levels
                                     ├─0:1─ child_sensory_adjustments
                                     ├─1:n─ questionnaire_sessions ─1:n─ child_answers
                                     ├─1:n─ recommendations
                                     └─1:n─ daily_plans ─1:3─ daily_plan_items
                                                                └─0:1─ feedback
                                                                         ├─1:n─ feedback_effects
                                                                         └─0:1─ feedback_llm_classifications
activities ─┬─1:1─ activity_instructions
            ├─1:n─ activity_steps        (1 097 satır)
            ├─1:n─ activity_materials    (390)
            ├─1:n─ activity_outcomes     (733)
            ├─1:n─ activity_media / activity_attributes
            └─0:1─ activity_workshop_details ─1:n─ activity_sessions
referans: dunn_profiles (5) · developmental_period_tasks (4) · scoring_parameters (65)
          questions (14) · question_options (46) · consent_documents (5)
```

| Tablo | Tek satır işlevi |
|---|---|
| `users` | Kimlik ve parola |
| `parent_profiles` | Ebeveyn profili, kullanıcıya bağlı |
| `children` | Çocuk; yaş, bütçe aralığı, onboarding durumu |
| `child_profile_snapshots` | Onboarding çıktısı: Dunn tipi, kaygı, odak, Gardner önselleri |
| `child_intelligence_scores` | 8 Gardner alanı, puan + örnekleme sayacı |
| `child_domain_levels` | 7 gelişim alanı, basamak + ondalık sayaç |
| `child_sensory_adjustments` | LLM ipuçlarıyla sıkılan tolerans ve katılım filtresi |
| `questionnaire_sessions` / `child_answers` | Onboarding oturumu ve cevaplar |
| `daily_plans` | Bir tur; tarih, bütçe, taahhüt/toplam süre, geri çekilme kademesi |
| `daily_plan_items` | Turdaki üç etkinlik; yuva, skor, seçim ve tamamlanma damgası |
| `feedback` | Annenin oyu + çözümlenen sebep + serbest metin |
| `feedback_effects` | Uygulanan Gardner deltaları (geri alınabilir) |
| `feedback_llm_classifications` | Model çıktısı ve uygulanıp uygulanmadığı |
| `recommendations` | Skor dökümüyle öneri kaydı |
| `activities` + 6 yardımcı tablo | İçerik havuzu (243) |
| `dunn_profiles` | 5 duyusal tip: tolerans üçlüsü + ceza ağırlıkları |
| `developmental_period_tasks` | Yaş bandı → dönem görevi alanı |
| `scoring_parameters` | 65 sayısal eşik |
| `consent_documents` / `user_consents` | İzin metinleri ve kullanıcı kararları |
| `email_verification_tokens` · `user_devices` · `media_assets` · `attribute_definitions` · `child_allergies` · `child_medications` · `workshop_profiles` | Yardımcı / henüz kullanılmayan |

### 7.10 · Frontend

**Bu depoda yok.** Tek iz: `app.cors.allowed-origins` varsayılanı
`http://localhost:3000`. Framework, sayfa listesi, deploy dalı **doğrulanamadı**.

---

## 8 · Sayısal özet

| | |
|---|---|
| Etkinlik havuzu | **243** (1-128 temel · 129-162 sakin · 163-243 yeni) |
| Onboarding sorusu | **14** kayıtlı soru; çocuk başına yaşa göre alt küme |
| Gardner alanı | 8 |
| Gelişim alanı | 7 |
| Duyusal tip | 5 |
| Skor terimi | 5 (D, G, P, Z, B) |
| Yuva | 3 |
| Karar matrisi maddesi | **doğrulanamadı** (belge bende yok) |
| v6 §0 değişiklik maddesi | **19** (görev 21 diyor) |
| Unit test senaryosu (doküman) | **doğrulanamadı** (74 sayısı belgeden, belge bende yok) |
| Few-shot örneği | **41** |
| Java sınıfı (main) | **206** |
| Test sınıfı | 37 |
| REST endpoint | **22** |
| Tablo | **39** |
| Migration | **39** |
| JUnit test | **161** (koşan 158, atlanan 26, kırmızı) |
| Canlıda test edilen çocuk | **doğrulanamadı** |
| Etkinlik adımı / malzeme / kazanım | 1 097 / 390 / 733 |
| `scoring_parameters` satırı | 65 |

---

## 9 · Doğrulanamayanlar

Raporda “doğrulanamadı” yazmak, yanlış bir şey yazmaktan iyidir.

### 9.1 · Canlı ortama erişimim yok

`kidloop-fe.vercel.app` ve Neon `neondb` bu oturumun çıkış vekilinde engelli
(`403 CONNECT`). Aşağıdakilerin hiçbirini ölçemedim:

| Madde | Ne gerekiyor |
|---|---|
| 1.5 · Vercel URL, Neon branch, son deploy tarihi | Deploy panosu |
| 3.2 · `feedback_llm_classifications` satır sayısı | `SELECT count(*) FROM feedback_llm_classifications` |
| 3.7 · `child_sensory_adjustments` canlıda satır var mı | `SELECT count(*) FROM child_sensory_adjustments` |
| 5.5 · Canlı çocuk / plan / geri bildirim sayısı | Üç `count(*)` |
| Bölüm 2'nin tüm “Canlı” sütunu | Canlı veritabanı |

**Canlıda hangi commit koşuyor bilinmiyor.** Bu ölçümün tamamı `0a2ef84` içindir.
21-26 Ağustos'ta bildirilen dört davranış (Ada 1. gün, Ada 2. gün, Deniz, Mavi) main'de
üretilemedi; sebebi canlının farklı bir sürüm koşması olabilir.

### 9.2 · Bende olmayan belgeler

| Belge | Etkilediği bölüm |
|---|---|
| Term Project Proposal (Haziran 2026) | 6 · taahhütlerin metni görülmeden “karşılandı” denemez |
| `Kidloop_LLM_Sinama_Senaryolari.md` | 5.3 · T1-T5 koşulamadı |
| Karar matrisi (38 madde) | 8 · sayı doğrulanamadı |
| Unit test dokümanı (74 senaryo) | 5.1 · 74 sayısı doğrulanamadı |
| `Kidloop_Oneri_Prensibi_v6.md`'nin güncel nüshası | Bendeki nüsha 21 Ağustos 11:58; §0'da **19** madde var, görev 21 diyor. §4.8 ve §3.3'ün bazı cümleleri bendeki nüshada **yok** — 25 Ağustos'ta eklendiği bildirildi, bana verilmedi |

### 9.3 · Kod var, çalıştığı doğrulanmadı

| Madde | Neden |
|---|---|
| LLM çağrısının gerçekten yanıt alması | Gerçek API anahtarı ve dış ağ erişimi yok; kod yolu okundu, uçtan uca çağrı yapılmadı |
| Geri çekilme kademeleri 1-4 | Ölçtüğüm profillerin hepsi kademe 0'da kaldı; kademeler birim testle kapsanmış, canlı tetiklenmedi |
| `RecommendationScenarioTest` ve `PostgreSqlMigrationIntegrationTest` | Docker yok; 26 test atlandı, yeşil oldukları **doğrulanmadı** |
| Önyüzün bütçe ekranını çağırdığı | Ayrı depo; çağırmazsa çocuk varsayılan 25-35'te kalıyor |

### 9.4 · Belirsiz kalan tasarım soruları

* **EK7 ↔ §4.8**: “plan bir kez üretilir” ile “üç oy sonrası yeni plan” arasındaki
  ilişki şartnamede yazılı değil; kod tur bazlı çözmüş.
* **v4 §11.2 asenkron**: kod senkron. Fonksiyonel sonuç denk, ama anne bekliyor.
  Bilinçli bir tercih mi, yoksa henüz yapılmamış bir iş mi — kodda gerekçe yok.
* **`resolveReason` ↔ LLM**: iki ayrı teşhis mekanizması var; `resolveReason`'ın
  `SENSORY`/`INVOLVEMENT` çıktıları hiçbir yere yazılmıyor, yalnız LLM ipuçları yazıyor.
  İkisinin nasıl bağdaşacağı kararlaştırılmamış görünüyor.
