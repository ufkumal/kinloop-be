# Keşif yuvası ve bütçe alt sınırı · Ölçüm raporu

Tarih: 25 Ağustos 2026
İncelenen kod: `1468948` (main, V29–V33)
Yöntem: kod okuma + gerçek API üzerinden yeniden kurulum + doğrulanmış referans motorla simülasyon

Görevin talimatına uydum: **v6 ile çelişki aramadım**, yalnız kodun ne yaptığını ölçtüm.

---

## 0 · Önce: Ada'nın planı bu kodda çıkmıyor

Rapordaki plan **201 / 206 / 199**, toplam 30 dakika.
Bu kodda aynı profil **201 / 206 / 232**, toplam **40 dakika** üretiyor.

Ada'yı gerçek API üzerinden kurdum — 54 ay, C3 (“Katlanır ama gerginleşir”), kaygı 3
(“Duruma göre”), odak “10-15 dakika”, bütçe şıkkı C:

| Yuva | id | Başlık | Süre | Skor |
|---|---|---|---|---|
| DEVELOP | 201 | Duygu hikâyesi tamamlama | 10 dk | 123.0 |
| STRENGTHEN | 206 | Sıra kuralı oyunu | 10 dk | 123.0 |
| EXPLORE | **232** | Birlikte kukla gösterisi hazırlama | **20 dk** | 123.0 |

```
toplam 40 dk · taahhüt 40 dk · kademe 0 · bütçe 35-45  ->  alt sınır TUTUYOR
```

Yani **referans motorun beklediği plan çıkıyor**, ve **alt sınır sorunu Ada'da yok.**

Raporda geçmeyen iki cevabın (Q3 ve Q5) etkisini elemek için on beş varyantı da
koşturdum — Q3 ∈ {A, C, E} × Q5 ∈ {A, B, C, D, E}:

```
Q5 = A, B, E   ->  201 / 206 / 232      toplam 40 dk
Q5 = C, D      ->  201 /  14 / 206      toplam 35 dk
```

**id 199 on beş varyantın hiçbirinde çıkmadı.** Toplam hiçbir varyantta 35'in altına
inmedi.

> **Sonuç:** Gözlenen plan bu commit'in ürünü değil. Canlıda koşan sürüm farklı olmalı.
> Bu, mekanizma sorularını geçersiz kılmıyor — hepsini aşağıda kod üzerinden cevapladım —
> ama **teşhisin dayandığı gözlem bu kodla doğrulanamıyor.** Canlı sürümün hangi commit
> olduğu netleşmeden “kod şunu yapıyor” diyemem.

Bu arada `odak = 10-15 dakika` cevabının etkisi de yok: o şık `Q6 = B` ve
`focus_band = MEDIUM`. Süre filtresi (`short_focus_max_duration_minutes = 10`) yalnız
`SHORT` bandında (`Q6 = A`, “~5 dakika”) çalışıyor.

---

## 1 · Keşif yuvası davranışı

### A1 · Aday havuzu nasıl belirleniyor

`DailyPortfolioBuilder.leastSampledCandidates()`, satır **196-212**:

```java
int minimum = scores.values().stream()
        .mapToInt(ChildIntelligenceScore::getFeedbackCount)
        .min().orElse(0);
EnumSet<IntelligenceType> leastSampled = scores.values().stream()
        .filter(score -> score.getFeedbackCount() == minimum)
        .map(ChildIntelligenceScore::getIntelligenceType)
        .collect(...);
List<ScoredActivity> candidates = available(pool, selectedIds).stream()
        .filter(value -> leastSampled.contains(value.activity().getTargetIntelligence()))
        .toList();
return candidates.isEmpty() ? available(pool, selectedIds) : candidates;
```

“En az örneklenmiş zekâ alanı” hesabı tek bir veriye bakıyor:
**`child_intelligence_scores.feedback_count`** — kalıcı sayaç.

### A2 · Plan içinde seçilenler örneklenme sayacına giriyor mu

**Hayır.** Ne kalıcı sayaç güncelleniyor, ne geçici bir liste tutuluyor.

`available(pool, selectedIds)` (satır 218-220) yalnız **etkinlik id'lerini** eliyor:

```java
return pool.stream().filter(candidate -> !selectedIds.contains(candidate.activity().getId())).toList();
```

`selectedIds` bir `Set<Long>` — içinde etkinlik id'leri var, zekâ alanı yok. Plan
kurulurken hangi zekâ alanlarının kullanıldığını takip eden **hiçbir yapı yok.**

**Sonucu şu:** ilk günde sekiz alanın da `feedback_count` değeri 0, dolayısıyla
`minimum = 0` ve `leastSampled` **sekiz alanın tamamını** içeriyor. Keşif'in aday havuzu
hiç daralmıyor — havuzun tamamı aday oluyor ve sıralama kuralına göre birinci seçiliyor.

**Ölçümle doğrulandı:** Ada'nın planında Gelişim 201 ve Güçlendirme 206 alındıktan sonra
Keşif **232**'yi seçti — planın **üçüncü INTERPERSONAL** etkinliği. Yani kod, raporun
düşündüğünün tam tersini yapıyor: aynı zekâ alanını üçüncü kez seçmekten kaçınmıyor.

### A3 · Sayaç ne zaman kalıcı olarak artıyor

**Yalnız geri bildirim verildiğinde.** `ChildIntelligenceScore.recordFeedbackSample()`
(satır 45-47) sayacı artıran tek metot, ve tek çağıranı var:
`FeedbackLearningService.applyGardnerLearning()` **satır 137**.

Tüm `src/main` içinde `feedbackCount` yazan başka bir yer yok. Plan kurulumu sayaca
hiç dokunmuyor.

Not: sayaç **her** geri bildirimde artıyor — SEVDİ, ZORLANDI, OLMADI ayrımı yapılmadan —
ve yalnız **hedef** zekânın sayacı artıyor, ikincilinki değil.

### A4 · Bu davranış bilinçli mi

**Soru geçersiz: davranış mevcut değil.** Raporun tarif ettiği “plan içi örnekleme”
kodda yok, dolayısıyla ne commit mesajı, ne yorum, ne test var.

`DailyPortfolioBuilderTest.fillsOrderedSlotsWithReserveStrongestFallbackAndLeastSampledExploration`
adlı test `leastSampledCandidates`'i kapsıyor, ama **kalıcı sayaç** üzerinden — plan içi
tüketimi sınayan bir test yok.

### v6'ya eklenecek cümle taslağı

Görev bu davranışın “doğru göründüğünü” söylüyor ve cümle taslağı istiyor.
**Katılıyorum — ama bu bir belgeleme işi değil, yeni bir kural.** Kod bunu yapmıyor;
yazılırsa v6 ve kod birlikte değişmeli. Taslak:

```
### 4.2b · Keşif yuvasında plan içi çeşitlilik

Keşif yuvası "en az örneklenmiş zekâ alanı"nı seçerken, o gün Gelişim ve Güçlendirme
yuvalarına atanmış etkinliklerin hedef zekâ alanları da örneklenmiş sayılır.

  aday havuzu = havuz
                - bu planda zaten seçilmiş etkinlikler
                - bu planda zaten kullanılmış hedef zekâ alanları

  bu daraltma havuzu boşaltıyorsa daraltma uygulanmaz, havuzun tamamı aday olur

Gerekçe: Keşif yuvasının işi çeşitlilik. Aynı planda üçüncü kez aynı zekâ alanını
seçmek o işi boşa çıkarır; anne üç farklı etkinlik görür ama üçü de aynı alanı
çalıştırır.

Sınırlılık: Dar havuzlarda (korunmacı profil, düşük bütçe) daraltma sık sık boş
sonuç verir ve kural devre dışı kalır. Bu kabul edilir; garanti değil, tercih.
```

**Kod tarafında gerekecek değişiklik:** `DailyPortfolioBuilder.fill()` içinde seçilen
etkinliklerin `getTargetIntelligence()` değerlerini biriktiren bir `EnumSet` tutulmalı ve
`leastSampledCandidates()`'e geçirilmeli; metot bu kümedeki alanları elemeli, sonuç boşsa
mevcut davranışa düşmeli.

---

## 2 · Bütçe alt sınırı

### B1 · Plan kurulurken alt sınır kullanılıyor mu

**Hayır.** `daily_time_budget_min` kolonunu okuyan dört yer var, hiçbiri plan kurulumu
değil:

| Yer | Ne için |
|---|---|
| `ActivityMatchingService:102` | `DailyPlan` satırına yazılıyor — yalnız kayıt |
| `OnboardingService:74` | Ebeveyne mevcut bütçesini göstermek |
| `OnboardingService:189` | Çocuğun bütçesinin hangi şıkka denk geldiğini bulmak |
| `ProfileMapper:50` | Profil ekranı |

**Karar verici sınıfa hiç ulaşmıyor.** `DailyPortfolioBuilder.Request` record'u
(satır 277-292) yalnız `int budgetMax` taşıyor; alt sınır alanı yok.

### B2 · Ne sıklıkta oluyor

Görevin istediği altı profili gerçek API üzerinden kurdum, hepsi bütçe C (35-45):

| Profil | Plan | Toplam | Alt sınır |
|---|---|---|---|
| 30 ay · C1 · kaygı 2 | 74/15 · 59/15 · 8/10 | **40** | tutuyor |
| 42 ay · C1 · kaygı 2 | 102/10 · 87/10 · 91/10 | **30** | **5 dk eksik** |
| 54 ay · C1 · kaygı 2 | 107/20 · 113/20 · 154/5 | **45** | tutuyor |
| 54 ay · C3 · kaygı 3 (Ada) | 201/10 · 206/10 · 232/20 | **40** | tutuyor |
| 66 ay · C1 · kaygı 2 | 97/15 · 215/10 · 240/20 | **45** | tutuyor |
| 72 ay · C1 · kaygı 2 | 97/15 · 215/10 · 240/20 | **45** | tutuyor |

Altıda bir. **Ada'da sorun yok**, sorun 42 aylık profilde.

Daha geniş tarama için referans motoru kullandım — motor yukarıdaki **altı planın
altısını da birebir üretiyor** (6/6), yani simülasyon güvenilir. Üç ay aralıklarla,
C1 kaygı 2, bütçe C:

| Yaş | Toplam | Durum |
|---|---|---|
| 24, 27 | 35 | tutuyor |
| 30, 33, 36 | 40 | tutuyor |
| **39, 42, 45** | **30** | **5 dk eksik** |
| 48 | 40 | tutuyor |
| 51, 54, 57 | 45 | tutuyor |
| 60, 63, 66, 69, 72 | 45 | tutuyor |

**Ulaşılabilir 17 yaş noktasının 3'ünde alt sınır tutmuyor — hepsi 36-48 ay bandında.**

> 24 aydan küçük yaşlarda tarama 20-30 dakika veriyor, ama o çocuklara **C şıkkı zaten
> sunulmuyor** (`OnboardingService.availableForAge()`, satır 157-159). O satırları
> saymadım.

### B3 · Süre kademesi ters mi çalışıyor

**Hayır. Ölçtüm: süre kademesinin bu vakalarda hiçbir etkisi yok.**

Doğrulanmış referans motorda 4. kademeyi (a) tamamen kaldırdım, (b) ters çevirdim
(uzundan kısaya). Altı profilin sonucu:

| Profil | Mevcut | Kademe kaldırılınca | Kademe ters çevrilince |
|---|---|---|---|
| 30 ay C1 | 40 dk | **40 dk** | **40 dk** |
| 42 ay C1 | 30 dk | **30 dk** | **30 dk** |
| 54 ay C1 | 45 dk | **45 dk** | **45 dk** |
| 54 ay C3 (Ada) | 40 dk | **40 dk** | **40 dk** |
| 66 ay C1 | 45 dk | **45 dk** | **45 dk** |
| 72 ay C1 | 45 dk | **45 dk** | **45 dk** |

**Toplam süre altı profilin altısında da değişmiyor.** Ters çevirmek yalnız yuva
etiketlerini karıştırıyor — Ada'da 232 Keşif'ten Gelişim'e geçiyor, 66/72 ayda 215 ile
240 yer değiştiriyor — ama **aynı üç etkinlik seçiliyor**, toplam aynı kalıyor.

Sebebi basit: süre kademesi ancak skor, örnekleme ve duyusal yük **tamamen eşitken**
devreye giriyor. Kısa etkinliklerin kazandığı yerlerde skorlar eşit değil.

### Gerçek sebep: içerik + ZPD

42 aylık vakayı adım adım açtım. Gelişim yuvası dönem görevi alanından (LANGUAGE)
seçmek zorunda. O alandaki dokuz aday, skor sırasıyla:

```
  id  dk    skor  başlık
 102  10   129.0  Aile fotoğrafı hikâyesi          <- seçilen
  91  10   119.0  Kafiye avı oyunu
  92  10   119.0  Bugünün hikâyesini sen anlat
 100  10   119.0  Şarkıya kelime uydurma
 147  10   119.0  Günümü anlatıyorum üç cümle
 148  10    74.0  Ne olurdu oyunu sessiz
 183  10    74.0  Ne olacak tahmini
 226  15    74.0  Resimli hikâye sıralama          <- tek 15dk+ aday
 193  10    69.0  Tarif etme oyunu
```

**Dokuz adaydan yalnız biri 15 dakika ve üstü (226), ve o da 55 puan geride.**
Süre kademesine sıra bile gelmiyor.

226'nın geride kalma sebebi ZPD: 42 aylık çocuğun başlangıç basamağı **L = 1**
(`domain_initial_level_under_48m`). 226'nın zorluğu **d = 3**, yani `d > L+1` →
**Z = −25**. Tatlı nokta olan `d = 2` ise **Z = +20**. Aradaki 45 puan, uzun
etkinliklerin bu yaşta hiç kazanamamasını tek başına açıklıyor.

### B4 · Havuzda yeterli uzun etkinlik var mı

İki farklı soru var: “uzun etkinlik var mı” ve “**kazanabilecek** uzun etkinlik var mı”.

| Yaş bandı | Başlangıç L | Tatlı nokta d | 15dk+ toplam | 15dk+ ve rekabetçi (d = L+1) | Bunlardan dönem görevinde |
|---|---|---|---|---|---|
| 12-24 ay | 1 | 2 | 3 | **0** | **0** |
| 24-36 ay | 1 | 2 | 12 | 12 | 4 |
| **36-48 ay** | 1 | 2 | 20 | 10 | **0** |
| 48-60 ay | 2 | 3 | 18 | 12 | 3 |
| 60-72 ay | 3 | 4 | 17 | 9 | 2 |

**36-48 ay bandında yirmi uzun etkinlik yaşa uygun, onu rekabetçi — ama hiçbiri
dönem görevi alanında (LANGUAGE) değil.** Gelişim yuvası mecburen 10 dakikalık bir
etkinlik alıyor, sonraki iki yuva da öyle, toplam 30'da kalıyor.

Bütçe C'yi doldurmak için üç etkinliğin ortalama 12-15 dakika olması gerekiyor; bu
bandın havuz ortalaması 12.0 dakika ve dönem görevi alanında rekabetçi uzun aday
sayısı sıfır.

---

## 3 · 2. gün yuva kontrolü · B5-B8

EK bölümü Ada'nın 2. gününde kodun yüksek skorlu uzun etkinlikleri atladığını, yerine
düşük skorlu kısa etkinlikler seçtiğini bildiriyor. Senaryoyu birebir kurdum.

### Başlangıç durumu birebir tutturuldu

Ada'yı oluşturdum, 1. gün planını EK'teki hâle getirdim (201 / 206 / 199) ve üç oyu
verdim: 201 SEVDİ, 206 SEVDİ, 199 ZORLANDI. Oy sonrası durum:

| | EK'in bildirdiği | Ölçtüğüm |
|---|---|---|
| `g[INTERPERSONAL]` | 3.60 | **3.60** ✅ |
| `g[VERBAL_LINGUISTIC]` | 3.15 | **3.15** ✅ |
| `g[LOGICAL_MATHEMATICAL]` | 3.15 | **3.15** ✅ |
| diğer beş alan | 3.00 | **3.00** ✅ |
| `streak[SOCIAL_EMOTIONAL]` | 2.0 | **2.0** ✅ |
| `streak[GROSS_MOTOR]` | −0.5 | **−0.5** ✅ |
| L (yedi alan) | 2 | **2** ✅ |

**Başlangıç durumu birebir aynı.** Yani 2. günün girdisi tartışmasız doğru.

### 2. günün sonucu

| Yuva | id | Etkinlik | Süre | Skor | |
|---|---|---|---|---|---|
| DEVELOP | **232** | Birlikte kukla gösterisi hazırlama | 20 dk | 123.0 | ✅ |
| STRENGTHEN | **107** | Kurallı kutu oyunu | 20 dk | 109.0 | ✅ |
| EXPLORE | 155 | Hikâyeyi birlikte tamamlama | 10 dk | 108.0 | ✅ |

```
taahhüt 40 dk · toplam 50 dk · kademe 0 · Keşif within_budget = FALSE
```

**Bu, referans motorun beklediği planın birebir aynısı** — üç etkinlik, üç skor, üç yuva,
taahhüt ve taşma dahil.

EK'in bildirdiği plan (153 / 154 / 155, 25 dk) **bu kodda çıkmıyor.** Kod yüksek skorlu
uzun etkinlikleri atlamıyor; tam tersine ikisini birden alıyor.

### B5 · 232 ve 107 neden seçilmedi

**Seçildiler.** Sorunun varsaydığı durum bu kodda oluşmuyor. Yine de zincirin her
halkasını ölçtüm:

| Kontrol | Sonuç |
|---|---|
| Havuzda mı (yaş, süre, scope, status) | ✅ ikisi de. Havuz 39 etkinlik |
| Tazelik elemesine takılıyor mu | ❌ hayır. Pencere N = maks(2, tavan(39/6)) = **7 plan**, elenen yalnız 201, 206, 199 → kalan 36 |
| Gelişim aday listesine giriyor mu | ✅ ikisi de — dönem görevi SOCIAL_EMOTIONAL |
| Sıralamada kaçıncı | **232 birinci, 107 ikinci** |

Gelişim yuvası aday sıralaması, 2. gün (dönem görevi SOCIAL_EMOTIONAL, tavan 40 dk):

```
sıra   id  dk   d    skor  yük  başlık
   1  232  20   3   123.0    5  Birlikte kukla gösterisi hazırlama   <- kod bunu seçti
   2  107  20   3   109.0    8  Kurallı kutu oyunu
   3  153  10   2   103.0    5  Duygu kartları sessiz eşleştirme     <- EK'te seçildi denen
   4   14  15   2    99.0    7  Duygu günlüğü çizimi
   5  113  20   3    99.0   11  Takım görevi birlikte çadır kurma
   6  208  10   2    93.0    4  Bitki bakım sözleşmesi
   7  154   5   2    83.0    3  Teşekkür turu                        <- EK'te seçildi denen
```

Güçlendirme yuvası (en yüksek Gardner alanı = INTERPERSONAL 3.60), 232 alındıktan sonra:

```
sıra   id  dk   d    skor  yük  başlık
   1  107  20   3   109.0    8  Kurallı kutu oyunu                   <- kod bunu seçti
   2  153  10   2   103.0    5  Duygu kartları sessiz eşleştirme
   3  113  20   3    99.0   11  Takım görevi birlikte çadır kurma
   4  154   5   2    83.0    3  Teşekkür turu                        <- EK'te seçildi denen
```

EK'in skor dökümü de doğru: fark tamamen `Z` teriminden geliyor. L = 2 olduğu için
`d = 3` tatlı nokta (`d = L+1` → **+20**), `d = 2` nötr (`d = L` → **0**). Kod bu
farkı doğru uyguluyor ve tatlı noktadakini seçiyor.

### B6 · Gelişim yuvasının süre tavanı

`DailyPortfolioBuilder.fill()`, satır **77-84**:

```java
int reserve = pool.stream()
        .mapToInt(candidate -> candidate.activity().getDurationMinutes())
        .min().orElse(0);

ScoredActivity development = firstFitting(
        relaxSlots ? pool : byDomain(pool, request.periodDomain()),
        selectedIds,
        Math.max(0, remaining - reserve));
```

* Formül v6 §4.2'deki hâliyle: `tavan = kalan − (havuzdaki en kısa etkinlik süresi)`.
* Rezerv **bir yuva için** ayrılıyor, iki için değil. Çıkarma tek kez yapılıyor ve
  yalnız Gelişim yuvasında; Güçlendirme ve Keşif tavanı `kalan`.

Ada'nın 2. gün havuzunda en kısa etkinlik **5 dakika**, yani:

```
Gelişim tavanı = 45 − 5 = 40 dk        232 (20 dk) rahatça sığıyor
```

**Tavan 232'yi elemiyor.**

### B7 · Süre kademesi sıralamada nerede

`CandidateOrdering.comparator()`, satır 21-25 — kademe sırası değişmemiş:

```java
Comparator.comparing(ScoredActivity::rawScore, Comparator.reverseOrder())   // 1
    .thenComparingInt(candidate -> sampleCount(candidate, intelligenceScores)) // 2
    .thenComparingInt(this::totalSensoryLoad)                                  // 3
    .thenComparingInt(candidate -> candidate.activity().getDurationMinutes())  // 4  <- süre
    .thenComparing(candidate -> seed(childId, planDate, ...));                 // 5
```

* Süre gerçekten **4. sırada**, yukarı taşınmamış.
* Skor karşılaştırması `ScoredActivity::rawScore` üzerinden — **ham skor**, süreye
  bölünmüş bir değer değil. Kodun hiçbir yerinde skor/süre oranı hesaplanmıyor.
* Skorlar 123, 109 ve 103 iken **süre kademesine hiç ulaşılmıyor**; birinci kademe zaten
  ayırıyor. Ölçüm bunu doğruluyor: 20 dakikalık 232, 10 dakikalık 153'ü geçti.

### B8 · Üçüncü gün ve eğilim

2. günün üç etkinliğine de aynı deseni uyguladım (232 SEVDİ, 107 SEVDİ, 155 ZORLANDI)
ve 3. günü ölçtüm:

| Yuva | id | Etkinlik | Süre | Skor |
|---|---|---|---|---|
| DEVELOP | 153 | Duygu kartları sessiz eşleştirme | 10 dk | 108.0 |
| STRENGTHEN | 113 | Takım görevi birlikte çadır kurma | 20 dk | 89.0 |
| EXPLORE | 108 | Origami basit köpek yüzü | 15 dk | 104.0 |

```
taahhüt 45 dk · toplam 45 dk · kademe 0 · üçü de bütçe içinde
```

Üç günün seyri:

| Gün | Toplam | Taahhüt | Alt sınır 35 |
|---|---|---|---|
| 1 | 40 dk | 40 | tutuyor |
| 2 | 50 dk | 40 | tutuyor |
| 3 | 45 dk | 45 | tutuyor |

**Eğilim azalmıyor.** EK'in bildirdiği 30 → 25 düşüşü bu kodda oluşmuyor.

Not: 2. günün iki SEVDİ oyu `streak[SOCIAL_EMOTIONAL]`'i 2.0'dan 3.0'a çıkardı ve
**basamak L2 → L3'e yükseldi**, sayaç sıfırlandı. §3.2'nin eşik kuralı doğru çalışıyor.
Bu yüzden 3. günde `d = 3` etkinlikler artık nötr, `d = 4` olanlar tatlı nokta.

### Bu bir hata mı, tasarlanmış davranış mı

**Hiçbiri — bu kodda böyle bir davranış yok.** Yüksek skorlu uzun etkinlikler seçiliyor.
Ölçtüğüm zincirin hiçbir halkasında (havuz, tazelik, aday listesi, süre tavanı, sıralama)
232 ve 107'yi eleyen bir şey bulamadım.

---

## 3 · Öneri: **(a)**, ama “ürün kararı” olarak değil, **içerik işi** olarak

| Seçenek | Değerlendirme |
|---|---|
| **(a) Değişiklik yok** | ✅ Algoritma tarafında doğru. Ama sorun ürün kararı değil, **içerik açığı** — aşağıda |
| (b) Süre kademesi ters çevrilsin | ❌ **Ölçüldü: hiçbir etkisi yok.** Altı profilde de toplam süre aynı kaldı; yalnız yuva etiketleri karıştı |
| (c) Alt sınır plan kurmaya girsin | ❌ Zararlı. 36-48 ay bandında dönem görevi alanında rekabetçi uzun aday **yok**; kural, ZPD'nin “iki basamak üstü” dediği bir etkinliği dakika doldurmak için seçmek zorunda kalırdı |

### (c) neden zararlı — somut

42 aylık çocukta alt sınırı tutturmak için Gelişim yuvasına 226'yı koymak gerekir.
226'nın skoru 74.0, seçilenin 129.0. Aradaki farkın 45 puanı ZPD'den geliyor:
etkinlik çocuğun basamağının iki üstünde. Yani (c), **modelin “bu çocuk için fazla zor”
dediği etkinliği, anne fazla vakti olduğunu söylediği için** plana sokardı.
Skor modelini tersine çevirir.

### Önerilen: içerik açığı kapatılsın

Ölçüm tek bir yeri gösteriyor:

```
36-48 ay bandı · dönem görevi LANGUAGE · süre >= 15 dk · difficulty = 2
mevcut sayı: 0
```

Bu bandda **d = 2, 15-20 dakikalık, LANGUAGE alanında** birkaç etkinlik eklenirse
39-45 ay aralığındaki açık kendiliğinden kapanır — algoritmaya dokunmadan.
Karşılaştırma için: aynı koşulu sağlayan 24-36 ay bandında 4, 48-60 ay bandında 3
etkinlik var ve o bantlarda alt sınır tutuyor.

12-24 ay bandında da rekabetçi uzun aday sıfır, ama oraya C şıkkı sunulmadığı için
sorun üretmiyor.

### İki bileşen aynı kök nedene mi bağlı

EK, alt sınır sorununu iki bileşene ayırıyor. Ölçüm ikisini de çürütüyor:

| Bileşen | Ölçüm |
|---|---|
| (i) Süre kademesi kısa etkinlikleri öne alıyor | ❌ Kademe kaldırıldığında da ters çevrildiğinde de altı profilde toplam süre **değişmiyor**. Kademe skorlar eşitken devreye giriyor; kısa olanın kazandığı yerlerde skorlar eşit değil |
| (ii) Yüksek skorlu uzun etkinlikler hiç seçilmiyor | ❌ Ada'nın 2. gününde **ikisi de seçildi** — 232 (20 dk, 123.0) ve 107 (20 dk, 109.0). Sıralamada birinci ve ikinci sıradalar |

Geriye tek bir gerçek bileşen kalıyor ve o da algoritmada değil: **36-48 ay bandında,
dönem görevi alanında (LANGUAGE), rekabetçi (d = 2) uzun etkinlik sayısı sıfır.**
Ada 54 aylık olduğu için o banda düşmüyor; onun planları zaten alt sınırı tutuyor.

### Ürün tarafına not

Anne “35-45 dakika” yazan şıkkı seçip 30 dakikalık plan aldığında bunu bir eksiklik
olarak okuyor. İçerik açığı kapanana kadar iki seçenek var, ikisi de kod işi değil:
şık etiketini beklenti yaratmayacak şekilde yazmak, ya da plan ekranında toplam süreyi
taahhüt olarak değil öneri olarak sunmak. Karar sizin.

---

## 4 · Özet

| Bulgu | Durum |
|---|---|
| Ada'nın gözlenen 1. gün planı (201/206/199, 30 dk) | ❌ **bu kodda çıkmıyor** — kod 201/206/232, 40 dk veriyor |
| Ada'nın gözlenen 2. gün planı (153/154/155, 25 dk) | ❌ **bu kodda çıkmıyor** — kod 232/107/155, taahhüt 40 dk veriyor |
| 232 ve 107 neden seçilmedi | **seçildiler** — Gelişim sıralamasında 1. ve 2. sıradalar |
| Gelişim süre tavanı | `kalan − en kısa` · tek yuvalık rezerv · Ada'da 45 − 5 = 40 dk |
| Süre kademesi sıralamada | 4. sırada, ham skor üzerinden · skorlar 123/109/103 iken hiç ulaşılmıyor |
| Üç günlük eğilim | 40 → 50 → 45 dk · **azalmıyor** |
| Keşif yuvası plan içi örnekleme yapıyor mu | ❌ **hayır** — sayaç yalnız geri bildirimle artıyor, ilk gün sekiz alan da aday |
| Sayacı artıran tek yer | `recordFeedbackSample()` · tek çağrı · `FeedbackLearningService:137` |
| Plan içi çeşitlilik kuralı | yazılmamış — taslağı bölüm 1'de, kod değişikliği gerektirir |
| Alt sınır plan kurmada kullanılıyor mu | ❌ hayır — `Request` record'unda alan bile yok |
| Alt sınır ne sıklıkta tutmuyor | 17 ulaşılabilir yaş noktasının 3'ünde · hepsi 36-48 ay |
| Süre kademesi ters mi çalışıyor | ❌ **hayır** — kaldırıldığında da ters çevrildiğinde de toplam süre değişmiyor |
| Gerçek sebep | 36-48 ay · LANGUAGE alanında rekabetçi (d=2) uzun etkinlik sayısı **sıfır** |
| Öneri | **(a)** + içerik açığının kapatılması |

**Motorda hata yok.** Alt sınır açığı bir algoritma kusuru değil, tek bir yaş bandındaki
içerik boşluğu — ve düzeltmesi de orada.
