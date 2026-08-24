# Kod İncelemesi — `1468948` (V29–V33)

Tarih: 24 Ağustos 2026
İncelenen: `1468948` (PR #29 sonrası main) · 33 migration
Yöntem: kod okuma + sıfırdan veritabanı + gerçek onboarding akışı + bağımsız referans motor + `mvn test`

Karşılaştırma tabanı: v6 spec'in son hâli (`Kidloop_Oneri_Prensibi_v6.md`, 21 Ağustos 11:58).

---

## Sonuç

Önceki incelemede bildirdiğim **iki engelleyici madde de kapanmış**: annenin bütçe
cevabı artık çocuğa ulaşıyor, EK8 yayın kapısı kurulmuş.

Bu turda **iki bulgu** var:

1. `mvn test` derlemesi **kırmızı** — `contextLoads` düşüyor (bölüm 4).
2. Duyusal/ayrılık geri bildiriminin **öğrenme döngüsü kapalı** — sebep sınıflandırılıyor
   ve saklanıyor ama hiçbir yere yazılmıyor (bölüm 5).

Öneri motorunun kendisinde spec'ten sapma bulamadım.

---

## 1 · Kapanan maddeler

### Bütçe (§5.1) — düzelmiş

`V31` `question_options`'a `daily_time_budget_min/max` ekliyor, Q7'yi v6 metnine
çeviriyor ve `CHILD_BUDGET` kapsamına alıyor. `ChildService.createChild()` artık
`onboardingService.resolveDailyTimeBudget()` sonucunu `Child`'a yazıyor;
`Child.java`'daki sabit `25/35` başlangıç değerleri kalkmış.

Gerçek API üzerinden üç çocuk oluşturdum:

| Gönderilen şık | `daily_time_budget_min` | `daily_time_budget_max` |
|---|---|---|
| A · Kısa ve öz olsun | 15 | 25 |
| B · Yarım saatim var | 25 | 35 |
| C · Rahatça vakit ayırabiliriz | 35 | 45 |

**Yaş kuralı da yazılmış.** `OnboardingService.availableForAge()`:
`ageMonths >= 24 || !"C".equals(option.getCode())` — 24 aydan küçükte C şıkkı
reddediliyor (§5.1 tablosu).

### Bilgilendirme ekranı (§5.3) — yazılmış

`V31` `children` tablosuna `onboarding_closing_message_responded_at`,
`onboarding_closing_reminder_requested`, `onboarding_closing_reminder_plan_baseline`
ekliyor. "Daha sonra hatırlat" seçilirse `shouldShowPlanReminder()` sonraki üç planda
bandı gösteriyor — spec "ilk üç planın üstünde" diyor, birebir.

### Yayın doğrulaması (§7.1 · EK8) — yazılmış

`V32` `trg_activities_validate_published` tetikleyicisini kuruyor. Spec'teki sekiz
kontrolün **hepsi** var: `easier_variation`, `harder_variation`, ≥4 adım, ≥3 kazanım,
7 değerli `target_domain`, 8 değerli ve birbirinden farklı hedef/ikincil zekâ,
`difficulty` 1-4, `duration_minutes`.

Kurulum öncesi `DO $$` bloğu mevcut yayınlanmış içeriği tarıyor ve bir tanesi bile
geçersizse migration'ı durduruyor. Önceki raporumdaki **EK8-01 kalıntısı temizlenmiş**:
ikincil zekâsı boş 14 kart dolduruldu, iki kazanımlı 3 kart (id 3, 18, 36) üçe
tamamlandı. Şu an 0 geçersiz yayınlanmış etkinlik var.

> **Operasyonel not, hata değil.** Tetikleyici yalnız `activities` tablosunda.
> Yayınlanmış bir etkinliğin adımı/kazanımı sonradan silinirse ya da
> `activity_instructions.easier_variation` boşaltılırsa kapı bunu görmez. Ayrıca
> bundan sonraki her içerik migration'ı kartı önce `DRAFT` yazıp adım/kazanım/
> yönergeleri yükledikten sonra `PUBLISHED`'a çekmeli — `V32`'nin başlığındaki not
> bunu zaten söylüyor, ama `V10`/`V18` tarzı "önce activity, sonra adımlar" sırası
> artık çalışmaz.

### v5 kalıntıları (§1.7) — temizlenmiş

`V33` `freshness_penalty`, `attachment_multiplier`, `slot_candidate_limit`,
`domain_initial_level` ve 8 eski anahtarı siliyor; `parent_profiles` ve
`question_options`'taki tekil `daily_time_budget_minutes` kolonları düşüyor;
`HOUSEHOLD` kapsamı kaldırılıyor (önce boş olduğunu doğrulayan bir kontrolle).

---

## 2 · Motor: satır satır spec karşılaştırması

Bu turda kodun kendisini spec metnine karşı okudum. Sapma bulamadım.

| Spec | Kod | Durum |
|---|---|---|
| §4.2 Gelişim: aday = dönem görevi alanı, tavan = kalan − havuzdaki en kısa süre | `DailyPortfolioBuilder.fill()` satır 77-84 | ✅ |
| §4.2 Güçlendirme: en yüksek Gardner alanı, yoksa 2., sonra 3.; hepsi eşitse havuzun tamamı | `strongestFitting()` — üç farklı puan bandı, tek bant varsa tüm havuz | ✅ |
| §4.2 Keşif: en az örneklenmiş alan, yoksa havuzun tamamı | `leastSampledCandidates()` | ✅ |
| §4.2 Keşif sığmazsa: en kısa adaylar arasından birinci, `within_budget = FALSE` | `shortest()` + `explorationWithinBudget = false` | ✅ |
| §4.3 beş kademeli sıralama | `CandidateOrdering.comparator()` | ✅ |
| §4.3 `seed = (childId×1000003 + gün×10007 + activityId) mod 2147483647` | `CandidateOrdering.seed()`, `BigInteger` | ✅ |
| §4.4 Gözetimli garantisi, yalnız Keşif değişir, "en az örneklenmiş" aranmaz | `applySupervisedGuarantee()`, `protectedIds` Gelişim+Güçlendirme'yi koruyor | ✅ |
| §4.5 beş kademeli geri çekilme | `build()` satır 24-42 | ✅ |
| §4.5 her kademede çocuk kimliği + profil + yuva + kademe log'lanır | `ActivityMatchingService` satır 99-101 | ✅ |
| §4.6 Gelişim tavanı rezervli, Güçlendirme/Keşif tavanı = kalan | ✅ | ✅ |
| §3.2 basamak sayacı (zorluk duyarlı) | `FeedbackLearningService.domainDelta()` | ✅ |
| §3.2 tavan davranışı, `ceiling_counter_cap` | `ChildDomainLevel.applyFeedback()` | ✅ |
| §3.3 SEVDİ +0.30/+0.15, ZORLANDI dokunmaz, OLMADI(ilgi) −0.15 | `applyGardnerLearning()` | ✅ |
| §3.4 dinamik tazelik `maks(2, tavan(havuz/6))` | `ActivityFreshnessPolicy.windowSize()` | ✅ |
| §3.4 iki eksenli C4 elemesi | `maks(ses, görsel) ≥ eşik` | ✅ |

**Parametre değerleri** de spec'le birebir: `level_credit_stretch` 1.00,
`level_credit_at_level` 0.50, `level_credit_below` 0.00,
`level_penalty_struggle_stretch` −0.50, `level_penalty_struggle_at_level` −1.00,
`level_up_threshold` 3.00, `level_down_threshold` −1.00, `ceiling_counter_cap` 1.00,
`liked_target_delta` 0.30, `liked_secondary_delta` 0.15,
`disliked_interest_delta` −0.15, `attachment_anxiety_threshold` 4.00,
`attachment_multiplier_together` 1.15, `freshness_window_divisor` 6,
`freshness_window_min` 2, `tiebreak_seed_a/b/mod` 1000003 / 10007 / 2147483647.

---

## 3 · Davranış doğrulaması

Bağımsız yazdığım referans motorla, aynı veritabanında, **gerçek onboarding akışı
üzerinden** (SQL ile bütçe yazmadan) 12 profil karşılaştırdım: **12/12 aynı plan.**

Beş duyusal tip, altı yaş bandı, üç bütçe aralığı. Tohumlu kura dahil uyuşuyor.

Ayrıca:

| Kontrol | Sonuç |
|---|---|
| Üç yuva her zaman dolu | ✅ kademe 0 |
| Boş plan | ✅ hiç oluşmadı |
| `within_budget` | ✅ A bütçesinde Keşif taşınca FALSE |
| Plan bir kez üretilir (EK7) | ✅ iki çağrı aynı `planId` |
| Yanıt alanları | ✅ hepsi mevcut |

**Doğrulayamadığım:** geri çekilme kademeleri 1-3 canlı ortamda tetiklenmedi (test
ettiğim 12 profilin hepsi kademe 0'da kaldı). Beş kademenin hepsi
`DailyPortfolioBuilderTest`'te birim testle kapsanmış; canlı uçtan uca koşmadım.

---

## 4 · Bulgu 1 — `mvn test` kırmızı

```
Tests run: 100   Failures: 0   Errors: 1   Skipped: 23
BUILD FAILURE
```

`KinloopBackendApplicationTests.contextLoads` düşüyor:

```
No qualifying bean of type 'com.kinloop.backend.repository.ConsentDocumentRepository'
```

**Sebep.** Bu test veritabanı otomatik yapılandırmasını kapatıyor ve gereken repository
bean'lerini `SecurityTestConfig` içinde **elle** sağlıyor. Projede 25 repository var,
test 12 tanesini sağlıyor. Eksik 13'ü:

```
ActivityRepository                    FeedbackEffectRepository
ChildDomainLevelRepository            FeedbackLlmClassificationRepository
ChildIntelligenceScoreRepository      FeedbackRepository
ChildSensoryAdjustmentRepository      RecommendationRepository
ConsentDocumentRepository             ScoringParameterRepository
DevelopmentalPeriodTaskRepository     UserConsentRepository
DunnProfileRepository
```

Elle liste tutulduğu için **her yeni repository bu testi kırıyor.** Kırılma
kalıcı: her `mvn test` BUILD FAILURE veriyor.

**İkinci sorun, aynı test.** `application.yml:26` `secret: ${JWT_SECRET}` — varsayılan
yok ve `src/test/resources` altında bir override yok. `JWT_SECRET` ortam değişkeni
tanımlı değilse test `Could not resolve placeholder 'JWT_SECRET'` ile düşüyor.
Yukarıdaki hata ancak `JWT_SECRET` verilerek koşulduğunda ortaya çıkıyor.

**Öneri.** İkisi de tek hamleyle çözülür: bu testi `@SpringBootTest` +
`@MockitoBean` yerine `@DataJpaTest`/dilimli teste çevirmek yerine, en ucuzu
`src/test/resources/application.yml` içine sabit bir test `JWT_SECRET` koymak ve
repository bean'lerini elle saymak yerine
`@MockitoBean(types = {...})` ya da `Repositories` taraması ile üretmek. Elle liste
tutuldukça bu test her sprintte tekrar kırılacak.

**Not:** `.github/workflows` yok — bu depoda otomatik CI kapısı bulunmuyor, yani
kırmızı derleme kimseyi uyarmıyor.

### Atlanan 23 test

`RecommendationScenarioTest` (7) ve `PostgreSqlMigrationIntegrationTest` (16)
`@Testcontainers(disabledWithoutDocker = true)` ile işaretli. Benim ortamımda Docker
yok, o yüzden atlanıyorlar — **kod hatası değil.** Ama Docker'lı bir ortamda
koşulduğunu doğrulayamadım; CI olmadığı için bu 23 testin yeşil olduğuna dair bir
kanıt da yok.

`RecommendationScenarioTest`'in v6 §8'deki dönüşümü yapılmış: artık migration
dosyasını metin olarak okumuyor, Testcontainers ile gerçek veritabanına bakıyor.

---

## 5 · Bulgu 2 — duyusal/ayrılık geri bildirimi öğrenmeye dönmüyor

Spec §3.3, "Olmadı" oyunun üç sebebini ayırıyor:

| Oy | Gardner | Basamak sayacı | Filtre |
|---|---|---|---|
| Olmadı (ilgisizlik) | −0.15 | dokunulmaz | — |
| Olmadı (duyusal) | dokunulmaz | dokunulmaz | **Yük eşiği sıkılır** |
| Olmadı (ayrılık) | dokunulmaz | dokunulmaz | **Katılım filtresi sıkılır** |

Kodda **sınıflandırma var, sonucu yok.**

`FeedbackLearningService.resolveReason()` sebebi doğru hesaplıyor (C3/C4'te tolerans
aşımı → `SENSORY`; kaygı ≥4 ve etkinlik `BAGIMSIZ` → `INVOLVEMENT`; aksi hâlde
`INTEREST`) ve `feedback` satırına yazıyor. Ama:

* `FeedbackReason.SENSORY` ve `FeedbackReason.INVOLVEMENT` sabitleri **kodun başka
  hiçbir yerinde okunmuyor** — yalnız `resolveReason` içinde üretiliyorlar.
* Okuma tarafı hazır: `V26` `child_sensory_adjustments` tablosunu kuruyor
  (`noise_adjustment`, `visual_adjustment`, `movement_adjustment`,
  `involvement_filter`), `ActivityEligibilityPolicy` ve `ActivityScorer` bu tabloyu
  okuyor. **Yazan taraf yok.** Tüm `src/main` içinde `new ChildSensoryAdjustment(`
  ya da `sensoryAdjustmentRepository.save(` geçen tek bir satır bulunmuyor.

**Sonuç:** her çocukta `adjustment == null` kalıyor. Duyusal sebeple "olmadı" diyen
anne aynı yükteki etkinlikleri almaya devam ediyor; ayrılık sebebiyle "olmadı" diyen
annenin katılım filtresi sıkılmıyor. Döngü kapalı.

Bu, v6 §10'daki "bu sürümde kodlanmayacak" listesinde **yok** — yani kapsam içinde
görünüyor. Öte yandan geri bildirim akışının tamamı henüz ürün tarafında
kullanılmıyor; öncelik kararı sizin.

---

## 6 · Özet

| Alan | Durum |
|---|---|
| Şema (§1) | ✅ 33 migration temiz, v5 kalıntıları silinmiş |
| Skor formülü (§2) | ✅ referans motorla birebir |
| Basamak sayacı (§3.2) | ✅ parametreler dahil birebir |
| Gardner öğrenmesi (§3.3) | ⚠️ ilgi kolu çalışıyor, **duyusal ve ayrılık kolları yazmıyor** |
| Havuz filtresi (§3.4) | ✅ |
| Yuva doldurma (§4.2) | ✅ |
| Eşitlik bozma (§4.3) | ✅ tohum formülü birebir |
| Gözetimli garantisi (§4.4) | ✅ |
| Kademeli geri çekilme (§4.5) | ✅ birim testli, canlı tetiklenmedi |
| Plan kaydı (§4.7 / EK7) | ✅ |
| **Bütçe sorusu (§5.1)** | ✅ **düzeltilmiş**, yaş kuralı dahil |
| Bilgilendirme ekranı (§5.3) | ✅ backend tarafı hazır |
| Yayın doğrulaması (§7.1) | ✅ sekiz kontrolün hepsi |
| **Test altyapısı (§8)** | ❌ **`mvn test` BUILD FAILURE**, CI yok |
