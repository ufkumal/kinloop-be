# Ufkum'un v6 Implementasyonu — İnceleme

Tarih: 23 Ağustos 2026
İncelenen: `4eed979` (PR #26) · 107 dosya, 6457 satır
Yöntem: kod okuma + sıfırdan veritabanı + canlı API + bağımsız referans motorla karşılaştırma

---

## Sonuç

**v6 doğru uygulanmış.** Öneri motoru, bağımsız yazdığım referans motorla 12 profilde
**birebir aynı** planı üretiyor — tohumlu kura dahil.

**Bir gerçek bug var:** annenin bütçe cevabı plana hiç ulaşmıyor. Ayrıntı bölüm 4'te.

---

## 1 · Şema: tamam

28 migration temiz uygulandı. v6 §1'in her maddesi yerinde:

| v6 maddesi | Durum |
|---|---|
| `is_scaffolded` düşürülmesi | ✅ kolon yok |
| `activities` CHECK'leri (7 domain, d 1-4) | ✅ |
| `dunn_profiles` C4 ağırlıkları 5/5/3 | ✅ |
| Dönem görevi son bant 73 | ✅ |
| `children.daily_time_budget_min/max` | ✅ kolonlar var |
| `child_domain_levels.streak` → NUMERIC | ✅ `numeric`, eski `0..2` kısıtı düşürülmüş |
| `daily_plans` yeni kolonları | ✅ |
| `scoring_parameters` yeni satırları | ✅ tohum parametreleri dahil |
| İçerik havuzu 243 | ✅ id 1-243, boşluksuz |

Migration numaralandırması bendekinden farklı — onunki `V19` = etkinlik havuzu,
`V20`/`V21` = v6 hizalaması. Onunki geçerli, benim V19/V20'm gereksiz kaldı.

**Küçük artık:** `scoring_parameters`'ta `freshness_penalty` satırı duruyor. v6 §1.7
kaldırılmasını söylüyor. Kodda kullanılmıyor, zararsız ama temizlenmeli.

---

## 2 · Motor: bağımsız doğrulama, 12/12

v6 kurallarını Ufkum'un kodundan bağımsız yazdığım referans motorla, aynı
veritabanında, aynı profillerde karşılaştırdım:

| Profil | API planı | Referans motor | |
|---|---|---|---|
| 30ay C1 kaygı2 · B | 74 / 59 / 146 | 74 / 59 / 146 | ✅ |
| 30ay C4 kaygı5 · B | 141 / 223 / 144 | 141 / 223 / 144 | ✅ |
| 30ay C3 kaygı5 · B | 141 / 223 / 144 | 141 / 223 / 144 | ✅ |
| 30ay C2 kaygı2 · B | 74 / 65 / 146 | 74 / 65 / 146 | ✅ |
| 30ay MIXED kaygı2 · B | 74 / 59 / 146 | 74 / 59 / 146 | ✅ |
| 8ay C4 kaygı5 · A | 130 / 165 / 132 | 130 / 165 / 132 | ✅ |
| 18ay C4 kaygı5 · A | 135 / 171 / 139 | 135 / 171 / 139 | ✅ |
| 54ay C4 kaygı5 · C | 201 / 206 / 157 | 201 / 206 / 157 | ✅ |
| 66ay C1 kaygı2 · C | 97 / 215 / 240 | 97 / 215 / 240 | ✅ |
| 72ay C1 kaygı2 · B | 97 / 215 / 210 | 97 / 215 / 210 | ✅ |
| 30ay C1 kaygı2 · A | 74 / 8 / 146 | 74 / 8 / 146 | ✅ |
| 60ay C3 kaygı5 · C | 215 / 240 / 97 | 215 / 240 / 97 | ✅ |

Sıra: Gelişim / Güçlendirme / Keşif.

**Bu güçlü bir kanıt.** İki bağımsız implementasyon, beş duyusal tipte, altı yaş
bandında, üç bütçe aralığında aynı planı üretiyor. Tohumlu kura da uyuşuyor — yani
`(childId × 1000003 + gün × 10007 + activityId) mod 2147483647` formülü birebir
uygulanmış.

### Davranış kontrolleri

| Kontrol | Sonuç |
|---|---|
| Üç yuva her zaman dolu | ✅ beş profilde de 3 etkinlik, kademe 0 |
| Boş plan | ✅ hiç oluşmadı |
| `within_budget` işaretlemesi | ✅ A bütçesinde Keşif taşınca FALSE geliyor |
| **EK7 · plan bir kez üretilir** | ✅ iki çağrı aynı `planId` (25/25) döndü |
| Yanıt alanları | ✅ `budgetMin/Max`, `committedDurationMinutes`, `totalDurationMinutes`, `fallbackLevel`, `withinBudget`, `repeatNotice` |

Tazelik politikası da doğru: dinamik pencere `maks(2, tavan(havuz/6))` ve geri çekilme
kademe 1 için elenmemiş havuz saklanıyor.

---

## 3 · Testler

```
Tests run: 96   Failures: 0   Errors: 1   Skipped: 13
```

* **1 hata — ortamsal, kod değil.** `KinloopBackendApplicationTests.contextLoads`
  `JWT_SECRET` ortam değişkeni tanımlı olmadığı için düşüyor
  (`Could not resolve placeholder 'JWT_SECRET'`). Ortam değişkeniyle koşulursa geçer.
* **13 atlanan — Docker gerekiyor.** `PostgreSqlMigrationIntegrationTest`
  `@Testcontainers(disabledWithoutDocker = true)`. Bende Docker yok; CI'da koşacaklar.
  575 satırlık gerçek migration testi yazılmış, iyi.

Yeni test dosyaları: `ActivityFreshnessPolicyTest`, `CandidateOrderingTest`,
`MatchingStateInitializerTest`, `ChildDomainLevelTest`, `DailyPlanControllerTest`,
`FeedbackLearningService*Test` ve genişletilmiş `DailyPortfolioBuilderTest`.

---

## 4 · Bulunan bug: annenin bütçe cevabı plana ulaşmıyor

**`Child.java` satır 54 ve 57:**

```java
private short dailyTimeBudgetMin = 25;
private short dailyTimeBudgetMax = 35;
```

Bu iki alan sabit değerle başlıyor ve `ChildService.create()` bunları **hiçbir zaman
annenin cevabına göre ayarlamıyor.** Kolonlarda DEFAULT da yok; değer yalnız bu iki
satırdan geliyor.

Canlı doğruladım — üç çocuk, üç farklı bütçe şıkkı:

| Gönderilen şık | `daily_time_budget_min` | `daily_time_budget_max` |
|---|---|---|
| B | 25 | 35 |
| A | 25 | 35 |
| C | 25 | 35 |

Üçü de aynı. Ebeveyn tarafında değer doğru kaydediliyor
(`parent_profiles.daily_time_budget_minutes = 30` oldu) ama **çocuğa geçmiyor.**
`ActivityMatchingService` ise `children.daily_time_budget_min/max`'i okuyor.

**Etkisi:** "Kısa ve öz olsun" diyen anne 35 dakikalık plan alıyor; "Rahatça vakit
ayırabiliriz" diyen anne 10 dakikalık içerik kaybediyor. Bütçe sorusu şu an hiçbir
şeye yaramıyor.

### Bağlantılı eksik: v6 §5.1'deki yeni bütçe sorusu yazılmamış

Q7 hâlâ v5 hâlinde:

| Şık | Mevcut etiket | Mevcut değer | v6 §5.1 |
|---|---|---|---|
| A | "5-10 dk" | 10 | "Kısa ve öz olsun" · 15-25 |
| B | "15-20 dk" | 20 | "Yarım saatim var" · 25-35 |
| C | "30+ dk" | 30 | "Rahatça vakit ayırabiliriz" · 35-45 |

Ayrıca §5.1'in "0-24 ayda C şıkkı sunulmaz" kuralı da yok.

`V20` migration'ı mevcut çocukları eski değerden aralığa çeviriyor (backfill doğru),
ama **yeni çocuk için bu dönüşüm hiçbir yerde yapılmıyor.**

**Yapılması gereken:** Q7 seçeneklerine `daily_time_budget_min/max` eklenmeli,
`ChildService.create()` seçilen şıkkın aralığını `Child`'a yazmalı, `Child.java`'daki
sabit başlangıç değerleri kaldırılmalı.

Bunu doğrulamak için bütçeyi doğrudan veritabanına yazıp testleri öyle koştum;
motorun kendisi doğru çalışıyor, sorun yalnız değerin oraya ulaşmaması.

---

## 5 · v6 kapsamında henüz görünmeyenler

Bunları doğrulayamadım, kapsam dışı olabilir:

* **§5.3 bilgilendirme ekranı** — onboarding'in son adımı; arayüz işi, backend'de izi yok
* **§4.5 kademe uyarıları** — geri çekilme kademelerinde log uyarısı üretilmesi; test
  ettiğim profillerin hepsi kademe 0'da kaldığı için tetiklenmedi
* **§7.1 yayın doğrulaması** — `PUBLISHED` öncesi zorunlu alan kontrolü; kodda ayrı bir
  doğrulayıcı görmedim

Ayrıca önceki raporumda bildirdiğim **EK8-01 sorunu duruyor**: id 3, 18, 36 (temel seed)
ikişer kazanıma sahip, kural en az 3 istiyor.

---

## 6 · Özet

| Alan | Durum |
|---|---|
| Şema (v6 §1) | ✅ tamam, bir artık parametre |
| Skor formülü (§2) | ✅ referans motorla birebir |
| Havuz filtresi (§3.4) | ✅ iki eksenli C4, dinamik tazelik |
| Yuva doldurma (§4.2) | ✅ sıralı, rezervli |
| Eşitlik bozma (§4.3) | ✅ beş kademe, tohum formülü birebir |
| Gözetimli garantisi (§4.4) | ✅ |
| Kademeli geri çekilme (§4.5) | ✅ altyapı var, uyarı üretimi doğrulanamadı |
| Plan kaydı (§4.7 / EK7) | ✅ |
| **Bütçe sorusu (§5.1)** | ❌ **yazılmamış, bütçe çocuğa ulaşmıyor** |
| Bilgilendirme ekranı (§5.3) | — arayüz |
| Yayın doğrulaması (§7.1) | — görülmedi |

Tek engelleyici madde bütçe. Onun dışında implementasyon spec'e sadık.
