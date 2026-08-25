# Yüksek skorlu etkinlikler neden seçilmiyor · Kök neden raporu

Tarih: 25 Ağustos 2026
İncelenen kod: `1468948` (main, V29–V33)
Yöntem: senaryonun gerçek API üzerinden birebir kurulması + kod okuma + varyant simülasyonu

Görevin talimatına uydum: v6 ile çelişki aramadım, yalnız kodun ne yaptığını ölçtüm.

---

## 1 · Kök neden

**Tek cümleyle: `1468948`'de böyle bir sapma yok — 232 ve 107 seçiliyor.**

Senaryoyu birebir kurdum. Ada'nın 1. gününü EK'teki hâle sabitledim (201 / 206 / 199),
üç oyu verdim, oy sonrası durumun rapordakiyle **aynı** olduğunu doğruladım, sonra
2. günü istedim. Kodun ürettiği:

| Yuva | id | Etkinlik | Süre | `d` | Skor |
|---|---|---|---|---|---|
| DEVELOP | **232** | Birlikte kukla gösterisi hazırlama | 20 dk | 3 | **123.0** |
| STRENGTHEN | **107** | Kurallı kutu oyunu | 20 dk | 3 | **109.0** |
| EXPLORE | 155 | Hikâyeyi birlikte tamamlama | 10 dk | 3 | 108.0 |

```
taahhüt 40 dk · toplam 50 dk · kademe 0 · Keşif within_budget = FALSE
```

**Bu, referans motorun beklediği planın birebir aynısı.** 232 ve 107 seçilmiş durumda.

Başlangıç durumunun aynı olduğunu ayrıca doğruladım, çünkü çıktı farkını tartışabilmek
için girdinin tartışmasız olması gerekiyor:

| | Raporun bildirdiği | Ölçtüğüm |
|---|---|---|
| `g[INTERPERSONAL]` | 3.60 | **3.60** ✅ |
| `g[VERBAL_LINGUISTIC]` | 3.15 | **3.15** ✅ |
| `g[LOGICAL_MATHEMATICAL]` | 3.15 | **3.15** ✅ |
| diğer beş alan | 3.00 | **3.00** ✅ |
| `L` (yedi alan) | 2 | **2** ✅ |
| `streak[SOCIAL_EMOTIONAL]` | 2.0 | **2.0** ✅ |
| `streak[GROSS_MOTOR]` | −0.5 | **−0.5** ✅ |

Girdi birebir aynı, çıktı farklı. Yani **canlıda koşan sürüm bu commit değil.**

Bölüm 6'da, gözlenen planı **birebir üreten bir kod varyantı** buldum. Ufkum'un
deploy edilmiş dalda arayacağı somut şey orada.

---

## 2 · Adım adım izleme · S7

```
Havuz                = 39 etkinlik      (yaş 48-62 kesişimi, süre ≤ 45, C3'te duyusal eleme yok)
Tazelik penceresi    = maks(2, tavan(39/6)) = 7 PLAN
Elenen               = 199, 201, 206    (yalnız dünkü plan; başka plan yok)
Kalan havuz          = 36 etkinlik

Başlangıç kalan      = 45
Rezerv               = havuzun en kısası = 5 dk        (tek yuvalık)
Gelişim tavanı       = 45 − 5 = 40
Gelişim seçildi      = id 232, 20 dk, skor 123.0       kalan = 25
Güçlendirme tavanı   = 25                              (en yüksek Gardner alanı INTERPERSONAL = 3.60)
Güçlendirme seçildi  = id 107, 20 dk, skor 109.0       kalan = 5
Keşif tavanı         = 5                               (en az örneklenme = 0)
Keşif: 5 dakikaya sığan aday yok
       → en kısa adaylar arasından birinci: id 155, 10 dk, skor 108.0
       → within_budget = FALSE
```

Toplam 50 dk, taahhüt 40 dk. Alt sınır 35'in üstünde.

---

## 3 · Gelişim yuvası aday listesi · S4

Sıralandıktan sonra, ilk beş (dönem görevi SOCIAL_EMOTIONAL, tavan 40 dk):

| Sıra | id | Süre | `d` | Skor | Başlık |
|---|---|---|---|---|---|
| **1** | **232** | 20 dk | 3 | **123.0** | Birlikte kukla gösterisi hazırlama |
| 2 | 107 | 20 dk | 3 | 109.0 | Kurallı kutu oyunu |
| **3** | **153** | 10 dk | 2 | **103.0** | Duygu kartları sessiz eşleştirme |
| 4 | 14 | 15 dk | 2 | 99.0 | Duygu günlüğü çizimi |
| 5 | 113 | 20 dk | 3 | 99.0 | Takım görevi birlikte çadır kurma |

**232 birinci, 153 üçüncü.** Sıralama ham skora göre yapılıyor ve doğru sıralıyor.

Güçlendirme yuvası (232 alındıktan sonra, INTERPERSONAL bandı):

| Sıra | id | Süre | Skor |
|---|---|---|---|
| **1** | **107** | 20 dk | **109.0** |
| 2 | 153 | 10 dk | 103.0 |
| 3 | 113 | 20 dk | 99.0 |
| 4 | 154 | 5 dk | 83.0 |

---

## 4 · `Z` terimi · S6

**Doğru hesaplanıyor.**

| id | Alan | `L` | `d` | `D` | `P` | **`Z`** | Ham skor |
|---|---|---|---|---|---|---|---|
| 232 | SOCIAL_EMOTIONAL | 2 | 3 | 12 | 15 | **+20** | 123.0 |
| 153 | SOCIAL_EMOTIONAL | 2 | 2 | 12 | 15 | **0** | 103.0 |
| 107 | SOCIAL_EMOTIONAL | 2 | 3 | 26 | 15 | **+20** | 109.0 |
| 154 | SOCIAL_EMOTIONAL | 2 | 2 | 32 | 15 | **0** | 83.0 |
| 155 | LANGUAGE | 2 | 3 | 12 | 0 | **+20** | 108.0 |

Beş değerin beşi de raporun kendi aritmetiğiyle aynı. `d = 3` tatlı nokta (+20),
`d = 2` nötr (0). Kod tatlı noktayı **atlamıyor**, tam tersine seçiyor.

**`L` nereden okunuyor:** `ActivityScorer.difficultyBonus()` satır **112**:

```java
int level = levels.get(activity.getTargetDomain()).getLevel();
```

`levels` haritası `ActivityMatchingService` satır 66'da kuruluyor:

```java
Map<DevelopmentDomain, ChildDomainLevel> levels = domainRepository.findByChildId(child.getId())
        .stream().collect(Collectors.toMap(ChildDomainLevel::getDomain, Function.identity()));
```

Yani **güncel `child_domain_levels` satırından**, `today()` çağrısının başında taze
okunuyor. Plan başında alınmış eski bir kopya değil, snapshot değil.

---

## 5 · Skor normalleştirmesi · S5

**Skor süreye bölünmüyor. Kodda hiçbir yerde bölme yok.**

`ActivityScorer.score()` satır 40-46:

```java
BigDecimal raw = parameters.get("score_base")
        .subtract(sensoryPenalty)      // D
        .add(interestBonus)            // G
        .add(periodBonus)              // P
        .add(difficultyBonus)          // Z
        .multiply(attachmentMultiplier); // × B
```

Tam olarak `(100 − D + G + P + Z) × B`, iki ondalığa yuvarlanmış.
`ScoredActivity` iki skor taşıyor: `rawScore` ve `displayScore` — dakika başına puan
diye bir alan yok.

Doğrulama:

* `grep -rn "divide" src/main/java` → **hiç sonuç yok.** Tüm ana kaynakta tek bir
  `BigDecimal.divide` çağrısı bulunmuyor.
* `getDurationMinutes()` on üç yerde geçiyor; hepsi karşılaştırma (`<=`, `==`),
  toplama (`sum`), en küçüğü bulma (`min`) ya da yanıta yazma. **Hiçbiri bölen değil.**

Kısa etkinlikleri sistematik avantajlı kılan bir normalleştirme **yok**.

### Ama bir tuzak var: `displayScore` 100'e kırpılıyor

`score_display_max = 100.0000`. `ActivityScorer` satır 47-48 `displayScore`'u
`[0, 100]` aralığına sıkıştırıyor.

Ada'nın 2. gün havuzunda **ham skoru 100'ün üstünde olan altı etkinlik var:**

```
232 (123)   236 (114)   107 (109)   155 (108)   108 (104)   153 (103)
```

`displayScore` ile **altısı da 100.00** olur, yani ayırt edilemez hâle gelir. Bugün
sıralama `rawScore` kullandığı için sorun yok — ama sıralama bir gün `displayScore`'a
kaydırılırsa 100 üstü her etkinlik eşitlenir ve karar zincirin 4. kademesine, yani
**süreye (azdan çoka)** düşer. O durumda kısa etkinlikler sistematik olarak kazanır.

Bunu ölçtüm ve tam olarak gözlenen davranışın bir kısmını üretiyor: `displayScore`
sıralamasında Gelişim yuvası **153** oluyor (232 ve 153 eşit skorda, eşit duyusal yükte;
süre kademesi 10 dakikalığı öne alıyor). Güçlendirme yine 232'yi alıyor, o yüzden
gözlenen planın tamamını açıklamıyor — ama **bu kırpma gerçek bir risk** ve
belgelenmeli.

---

## 6 · Gözlenen planı üreten varyant

Gözlenen planın nereden gelebileceğini aramak için sıralama ölçütünü ve yuva doldurma
sırasını değiştirip dört varyant koşturdum. Sonuçlar:

| Sıralama | Yuva sırası | 2. gün sonucu | |
|---|---|---|---|
| ham skor | Gelişim → Güçlendirme → Keşif | 232/20 · 107/20 · 155/10 = 50 dk | **mevcut kod** |
| ham skor | Güçlendirme → Gelişim → Keşif | 107/20 · 232/20 · 155/10 = 50 dk | |
| **skor ÷ süre** | Gelişim → Güçlendirme → Keşif | 154/5 · 153/10 · 155/10 = 25 dk | set tuttu, etiket tutmadı |
| **skor ÷ süre** | **Güçlendirme → Gelişim → Keşif** | **153/10 · 154/5 · 155/10 = 25 dk** | **birebir tuttu** |

```
gözlenen (canlı)  :  Gelişim 153/10dk · Güçlendirme 154/5dk · Keşif 155/10dk = 25 dk
son varyant       :  Gelişim 153/10dk · Güçlendirme 154/5dk · Keşif 155/10dk = 25 dk
```

**Skor ÷ süre sıralaması, iki varyantta da aynı üç etkinliği ve aynı toplamı (25 dk)
üretiyor.** Yuva sırası Güçlendirme'yi öne aldığında etiketler de birebir oturuyor.

Dakika başına puan hesabı gözlenen tercihi tam açıklıyor:

| id | Ham skor | Süre | Puan/dk |
|---|---|---|---|
| 232 | 123.0 | 20 dk | 6.15 |
| 107 | 109.0 | 20 dk | 5.45 |
| 153 | 103.0 | 10 dk | **10.30** |
| 155 | 108.0 | 10 dk | **10.80** |
| 154 | 83.0 | 5 dk | **16.60** |

En düşük ham skorlu etkinlik (154, 83 puan) dakika başına en yüksek olan.

**Sınır:** Bu varyant Ada'nın **1. gününü üretmiyor** (verdiği 201/154/206 = 25 dk,
gözlenen 201/206/199 = 30 dk). Yani tek başına deploy edilmiş sürümün tamamını
açıklamıyor. Kesin konuşmuyorum: **elimde deploy edilmiş kod yok, bu bir hipotez.**

Ama Ufkum'a somut bir arama hedefi veriyor:

```
deploy edilen dalda ara:
  · sıralamada duration_minutes ile bölme (divide, / , doubleValue()/dk)
  · CandidateOrdering yerine baska bir karsilastirici
  · DailyPortfolioBuilder.fill() icinde yuva doldurma sirasi
```

---

## 7 · Bu bir hata mı, tasarım mı

**`1468948` için soru geçersiz: kod yüksek skorluyu seçiyor, davranış mevcut değil.**

Mevcut kodda bunun bilinçli bir tercih olduğuna dair iz aradım:

| Kaynak | Bulgu |
|---|---|
| Yorum | `CandidateOrdering`'de süre kademesini açıklayan yorum **yok** |
| Test | `CandidateOrderingTest` var; süre kademesini kapsıyor, ama skor farkı olan bir vakada süreye düşülmediğini sınayan test **yok** |
| Commit | Süreye bölme ekleyen ya da kaldıran bir commit **bulunamadı** |

Deploy edilmiş sürüm için aynı soruyu cevaplayamam; o kodu görmedim.

---

## 8 · Düzeltme yolu

**`1468948` üzerinde düzeltilecek bir şey yok.** Yapılacak iki iş var, ikisi de
başka nitelikte:

### 8.1 · Önce deploy edilmiş sürüm netleşmeli

Bu, dördüncü görevde aynı sonuca varıyoruz: gözlenen davranış main'de üretilemiyor.
Tek soru: **`kidloop-fe.vercel.app`'in bağlandığı backend hangi commit'te?**
Cevap gelmeden "kod şunu yapıyor" diyemem, yalnız "main şunu yapıyor" diyebilirim.

Netleştikten sonra o commit'te bölüm 6'daki üç şey aranmalı.

### 8.2 · `displayScore` kırpması belgelenmeli

Mevcut kodda gerçek bir tuzak: `score_display_max = 100` yüzünden ham skoru 100 üstü
olan her etkinlik aynı `displayScore`'a sahip. Ada'nın 2. gün havuzunda bu altı
etkinlik demek.

Sıralama bugün `rawScore` kullanıyor ve doğru; ama bu bir tesadüf değil, korunması
gereken bir sözleşme. **Öneri:**

| Dosya | Ne |
|---|---|
| `CandidateOrdering.comparator()` | Birinci kademenin neden `rawScore` olduğunu, `displayScore` kullanılırsa 100 üstü her etkinliğin eşitleneceğini anlatan bir yorum |
| `CandidateOrderingTest` | Yeni test: ham skorları 123 ve 103 olan iki aday, kısa olan düşük skorlu — sıralamanın yüksek skorluyu döndürdüğünü sına. Bugün bu vakayı kapsayan test yok |

Kod yazmadım, tarif ettim.

---

## 9 · S1–S7 özet

| # | Soru | Cevap |
|---|---|---|
| **S1** | 232 ve 107 havuzda mı | ✅ **ikisi de.** Yedi filtrenin hepsinden geçiyorlar — aşağıda |
| **S2** | Tazelik ne eliyor | `N = maks(2, tavan(39/6)) = 7` **plan**, gün değil. Elenen: 199, 201, 206. Kalan 36 |
| **S3** | Gelişim tavanı | `45 − 5 = 40` dk. Rezerv = havuzun en kısası, **tek yuvalık** |
| **S4** | Sıralama | Ham skora göre. **232 birinci**, 153 üçüncü |
| **S5** | Skor normalleştirmesi | **Yok.** Kodda tek bir `divide` çağrısı bile bulunmuyor |
| **S6** | `Z` doğru mu | ✅ Beş etkinliğin beşinde de doğru. `L` güncel tablodan taze okunuyor |
| **S7** | Bütçe akışı | Bölüm 2 |

### S1 ayrıntı · filtre filtre

| Filtre | id 232 | id 107 |
|---|---|---|
| `status = PUBLISHED`, `scope = HOME` | geçti | geçti |
| Yaş aralığı (çocuk 54 ay) | 48-62 → geçti | 48-60 → geçti |
| Süre ≤ bütçe üst sınırı (45) | 20 dk → geçti | 20 dk → geçti |
| C4 duyusal elemesi | tip C3, kural uygulanmaz | tip C3, kural uygulanmaz |
| Kaygı elemesi (BAGIMSIZ) | kaygı 3 < 4, kural uygulanmaz (BIRLIKTE zaten) | aynı |
| Odak filtresi (SHORT) | odak MEDIUM, kural uygulanmaz | aynı |
| Tazelik | dün oynanmadı → geçti | dün oynanmadı → geçti |
| **Sonuç** | **havuzda** | **havuzda** |

### S2 ayrıntı · `findActivityIdsInRecentPlans`

`DailyPlanRepository` satır 18-30, yerel SQL:

```sql
SELECT DISTINCT dpi.activity_id
FROM daily_plan_items dpi
JOIN (
    SELECT id FROM daily_plans
    WHERE child_id = :childId AND plan_date < :beforeDate
    ORDER BY plan_date DESC
    LIMIT :planLimit
) recent_plans ON recent_plans.id = dpi.daily_plan_id
```

Pencere **son N plan**, son N gün değil. Aradaki fark: anne üç gün uygulamayı
açmazsa o günler plan üretilmediği için pencere geriye doğru genişlemiyor — üç plan
neyse o. `plan_date < :beforeDate` sayesinde bugünün kendi planı pencereye girmiyor.

Ada'da tek geçmiş plan var, o yüzden `LIMIT 7` etkisiz kalıyor ve yalnız dünkü üç
etkinlik eleniyor.
