# Skorlama Modeli ↔ Migration Şeması Boşluk Analizi

Kapsam: `src/main/resources/db/migration/V1..V8` ile aktivite skorlama modelinin
(filtre → skor → slot → oy → metin → kalkan → yeni gün) karşılaştırması.
V8 (`5b3bd69`) sonrası ikinci geçiş.

Formül: `Skor = clamp₀..₁₀₀[ (100 − D + G + P + Z) × B − T ]`

Bulgular önem sırasına göre gruplandı. Her başlıkta: **ne var / ne olmalı / etkisi**.

---

## 0-A. V8 sonrası durum (ikinci geçişin farkı)

`git diff df9f1a6..5b3bd69` → **tek dosya, 25 satır**: `V8__extend_activity_guidance.sql`.
V1–V7 bayt bayt aynı, Java tarafı da değişmemiş.

**V8'in kapattığı:** yalnızca **C7** — `activity_instructions`'a `easier_variation`
ve `harder_variation` geldi; artık kolaylaştırma metninin gideceği bir kolon var.
Yanında `purpose`, `why_it_matters`, `observation_tip` ve `activity_outcomes`
tablosu da eklenmiş (bunlar anne-yüzü içerik, skorlamaya girmiyor).

**Duran bulgular:** önceki 37'nin **36'sı aynen geçerli.** Kritiklerin hiçbirine
dokunulmamış: `IntelligenceType` enum uyuşmazlığı, Ç1'in 4/4 seed'i, tolerans
profilinin çift tasarımı, Dunn ağırlıklarının yokluğu, Piaget eşlemesinin
yokluğu, `feedback` silme kilidi — altısı da yerinde.

**V8'in yarattığı yeni bulgular:** 5 adet, bölüm **N**'de. Özeti: `is_scaffolded`
ile `easier_variation` arasında hiçbir bağ kurulmamış, dolayısıyla Z=+20 kararının
dayanağı artık **iki ayrı kaynakta** ve ikisi çelişebilir.

Güncel sayım: **6 kritik**, **9 model-şema çelişkisi**, **10 eksik alan/tablo**
(C7 düştü), **6 ölü kolon** (`harder_variation` eklendi), **5 V8 kaynaklı**,
**6 hijyen**.

---

## 0-B. ÇALIŞAN SİSTEM ÜZERİNDE DOĞRULAMA (V16, lokal)

Uygulama lokalde ayağa kaldırıldı (Postgres 16 + `java -jar`, 16 migration, 128
aktivite seed). `GET /api/children/5/daily-plan/today` **200 dönüyor.**
Test çocuğu Mavi ile birebir kuruldu: 30 ay, Q2=A (Ç1), Q3=C, Q4=2, Q5=B, Q7=B.

**Önceki analizde önerilenlerin çoğu uygulanmış:** `dunn_profiles`,
`developmental_period_tasks`, `scoring_parameters` (47 parametre) tabloları
eklendi; Ç1 artık **3/3/3 + w(5,5,3)** (A2 kapandı); V12 tolerans kolonlarını
snapshot'tan kaldırıp quadrant'tan türetmeye geçti (A3 kapandı); V14 zorluk
skalasını hizaladı (B1 kapandı); C4 filtresi `max(üç yük) < 3` olarak
netleşti (B6 kapandı); `focus_band` artık kullanılıyor (D bölümünden düştü).

**Motor matematiği doğru.** İki kartın skorunu elle yeniden hesapladım, birebir
tutuyor:
* Kukla (yük 3/2/2): D = 5·0 + 5·1 + 3·1 = 8 → (100−8+0+15+20)×1−0 = **127** ✓
* Fısıltı (yük 3/2/1): D = 5·0 + 5·1 + 3·2 = 11 → (100−11+0+15+20) = **124** ✓

> Not: model dokümanı Kukla için 132 diyor çünkü görsel yükü 3 varsayıyor;
> seed'de `visual_load = 2`. Fark **dokümanda**, motorda değil — doküman
> düzeltilmeli.

### R1. `clamp₀..₁₀₀` anne-yüzü yanıtta uygulanmıyor (HATA)
API `score: 127.0` ve `124.0` dönüyor. `scoring_parameters`'ta
`score_display_max = 100` var ve açıklaması **"Maximum parent-facing clamped
score"** diyor. `ActivityScorer:31` clamp'lenmiş `display` değerini hesaplıyor,
`breakdown`'a `displayScore` olarak yazıyor — ama
`ActivityMatchingService:85-86` hem `recommendations.score`'a hem
`daily_plan_items.score`'a **`rawScore`** yazıyor ve yanıt onu taşıyor.
Yani clamp hesaplanıp **atılıyor**. Modelin `clamp₀..₁₀₀` kuralı anne
ekranında geçersiz.

### R2. KEŞİF slotu 10 ve 20 dk bütçelerde sistematik olarak düşüyor
`DailyPortfolioBuilder.build()` önce 3'lü kombinasyonları deniyor, bütçeye
sığan yoksa 2'liye, sonra 1'liye düşüyor. Mavi'de (20 dk) sonuç **2 slot**:
`STRENGTHEN` + `DEVELOP`, `EXPLORE` yok.
Aynı çocuk 30 dk bütçeyle test edildiğinde **3 slot da geldi**.
Q7 seçenekleri 10/20/30 dk; uygun havuzun çoğu 10-15 dk. Yani pratikte
**yalnız 30 dk diyen ebeveyn tam portföyü görüyor.** "Sistemin soru sorma yolu"
olan keşif mekanizması, kullanıcıların çoğunda hiç çalışmıyor.
Ek olarak `slot_candidate_limit = 5`: 3'lü arama yalnız slot başına ilk 5 aday
üzerinde yapılıyor, dolayısıyla sığabilecek bir 5+5+10 kombinasyonu alt
sıralardaysa bulunamıyor.

### R3. Slot etiketi modelin seçim kuralını yansıtmıyor (kura ile dağılıyor)
`build()` slot başına ayrı ayrı "en iyi"yi seçmiyor; **kombinasyonun toplam
skorunu** maksimize edip eşitlikte kura çekiyor (`best()`, `random.nextInt`).
Sonuç, 30 dk testinde:

| Slot | Kart | Skor |
|---|---|---|
| STRENGTHEN | Balon volesi yavaş tempo | 120 |
| DEVELOP | Fısıltı şarkı oyunu | 124 |
| **EXPLORE** | **Kukla ile sohbet** | **127** |

Model açıkça diyor ki GELİŞİM slotu = dönem görevi domain'inden **en yüksek
skor**. Kukla LANGUAGE ve 127 ile en yüksek → GELİŞİM olmalıydı; KEŞİF'e düştü.
Toplam skor her iki dağılımda da aynı olduğu için kura karar veriyor.
Anne ekranındaki "çam / kayısı / leylak" üç yuvanın anlamı bu durumda kayboluyor.

### R4. Portföy çeşitliliği kısıtı yok
20 dk testinde iki slotun ikisi de `LANGUAGE` domain + `VERBAL_LINGUISTIC`
hedef aldı (Kukla ve Fısıltı). `uq_dpi_plan_activity` aynı aktiviteyi iki kez
engelliyor ama **aynı domain/zekâ tipini** engelleyen hiçbir kısıt yok.
Üç slot üç ayrı soruya cevap versin diye kurulmuş bir portföy, aynı soruyu iki
kez sorabiliyor.

### R5. A6 büyüdü: 7 domain, hâlâ 3 dönem görevi
V13 domainleri 7'ye çıkardı (`SENSORY`, `SELF_REGULATION` eklendi) ama
`developmental_period_tasks` hâlâ yalnız `GROSS_MOTOR` / `LANGUAGE` /
`SOCIAL_EMOTIONAL` eşliyor. 30 aylık Mavi'nin uygun havuzunda (24 kart):

| Domain | Kart | P=+15 alabilir mi? |
|---|---|---|
| COGNITIVE | 8 | ❌ |
| LANGUAGE | 5 | ✅ |
| SENSORY | 3 | ❌ |
| GROSS_MOTOR | 3 | ❌ (bu yaşta dönem görevi değil) |
| FINE_MOTOR | 2 | ❌ |
| SELF_REGULATION | 2 | ❌ |
| SOCIAL_EMOTIONAL | 1 | ❌ (bu yaşta değil) |

Havuzun **%79'u** (19/24) hiçbir koşulda dönem bonusu alamıyor ve GELİŞİM
slotuna giremiyor. Önceki analizdeki A6, domain sayısı arttıkça kötüleşti.

---

## 0. Bir cümlelik özet

Şema, skorlamanın **girdi tarafını** (aktivite pedagoji kolonları, çocuk profili,
zekâ puanları, domain seviyeleri) büyük ölçüde doğru kurmuş. Eksik olan taraf
**katsayı/eşik tarafı** (D'nin Dunn ağırlıkları, P'nin dönem görevi eşlemesi, tüm
sabitler) ve **geri besleme denetim tarafı** (LLM sinyalleri, kalkanlar, çifte
sayım, günlük tavan). Ayrıca modelin "tolerans profili Ç tipinden gelir" varsayımı
ile şemanın "tolerans her soru ekseninden ayrı gelir" tasarımı **birbirini
tutmuyor** — bu, D teriminin bugün yanlış hesaplanmasına yol açacak tek yapısal
çelişki.

Sayısal olarak: **6 kritik**, **9 model-şema çelişkisi**, **11 eksik alan/tablo**,
**5 ölü kolon**, **6 hijyen bulgusu**.

---

## A. KRİTİK — bugünkü haliyle hata üretir

### A1. `IntelligenceType` Java enum'u DB CHECK'leri ile uyuşmuyor
* **DB** (`V1:352-356`, `V1:563-564`, `V1:678-679`):
  `VERBAL_LINGUISTIC`, `VISUAL_SPATIAL`
* **Java** (`entity/enums/IntelligenceType.java`):
  `LINGUISTIC`, `SPATIAL`
* Diğer 6 değer aynı.
* **Etki:** `child_intelligence_scores` / `feedback_effects` yazımı `LINGUISTIC`
  ile denendiği anda `CHECK` ihlali. Ayrıca `GardnerPrior` kaydı
  `child_profile_snapshots.gardner_priors` JSONB'sinden Jackson ile okunuyor —
  seed'e bir gün `VERBAL_LINGUISTIC` prior'ı eklenirse **snapshot okuması patlar**.
  Bugün patlamamasının tek sebebi, V2 seed'inin yalnızca ortak isimli 3 değeri
  (`BODILY_KINAESTHETIC`, `INTERPERSONAL`, `INTRAPERSONAL`) kullanıyor olması.
* **Karar gerekir:** ya enum `VERBAL_LINGUISTIC`/`VISUAL_SPATIAL` olur, ya DB
  CHECK'leri `LINGUISTIC`/`SPATIAL`'a çekilir. Tek doğru var, iki isim olamaz.

### A2. Ç1 tolerans değerleri DB'de modelden farklı
* **Model:** Ç1 = ses 3 / görsel 3 / hareket 3.
* **DB** (`V2:280`, Q2 seçenek A → `C1`): `noise_sensitivity = 4`,
  `visual_sensitivity = 4`. Aynı hata `V2:268`'de Q2b seçenek A için de var.
* Ç2 (4/4), Ç3 (2/2), Ç4 (1/2) modelle birebir. **Sadece Ç1 sapmış** ve Ç1, Ç2
  ile aynı değerleri almış — yani "sakin gözlemci" ile "enerjik kaşif" duyusal
  olarak ayırt edilemiyor.
* **Etki:** Mavi örneğindeki `D = 3` hesabı bugünkü veriyle **çıkmaz**. Kukla
  kartı (yük 3/3/2) için gerçek sonuç: fark (1,1,x) → `D = 5+5+…` yani en az 10.
  Dokümandaki tüm örnek çıktılar Ç1 için yanlış olur.

### A3. Tolerans profili iki farklı tasarımla modelleniyor (yapısal çelişki)
* **Model:** tolerans üçlüsü **Ç tipinin sabit çıktısı**
  (Ç1→3/3/3, Ç2→4/4/5, Ç3→2/2/3, Ç4→1/2/2). Hareket toleransı da tipten gelir.
* **DB:** üç eksen **birbirinden bağımsız** dolar —
  `noise_sensitivity`/`visual_sensitivity` Q2'den (`V2:265-286`),
  `mobility_preference` Q3'ten (`V2:290-299`). Q2 hiç hareket yazmıyor,
  Q3 hiç quadrant yazmıyor.
* **Etki 1 — çelişkili profil:** Q2=A (Ç1, modele göre hareket 3) + Q3=A
  ("sürekli hareket halinde" → 5) cevaplayan çocukta `mobility_preference = 5`.
  Model bu çocuğu Ç1/hareket 3 sayar; DB 5 der. D terimi iki farklı sayı üretir.
* **Etki 2 — 0-12 bandında NULL:** V3'e göre 0-12 seti `Q2b, Q4b, Q7`. Q3 yok,
  Q2b hareket yazmıyor → `mobility_preference` **NULL kalır**. `D` formülünün
  hareket ekseni hesaplanamaz, DEFAULT da yok.
* **Karar gerekir:** ya tolerans üçlüsü quadrant'tan türetilir (bir
  `dunn_profiles` tablosu), ya model "hareket toleransı Q3'ten gelir" diye
  güncellenir. Şu an ikisi de yazılı, ikisi farklı.

### A4. `feedback` satırı, aktivite silinince CHECK ihlali verip silmeyi bloke eder
* `V1:643`: `fk_feedback_activity … ON DELETE SET NULL`
* `V1:664-665`: `chk_feedback_target CHECK (daily_plan_item_id IS NOT NULL OR activity_id IS NOT NULL)`
* Atölye yorumu kanalında `daily_plan_item_id` NULL'dur. O aktivite hard-delete
  edilince FK `activity_id`'yi NULL'a çeker, CHECK anında ihlal edilir →
  **`DELETE FROM activities` hata verir**. Soft-delete (`deleted_at`) kullanıldığı
  için bugün tetiklenmiyor; ilk gerçek idari silmede çıkar.
* Çözüm: FK'yi `ON DELETE CASCADE` yapmak ya da CHECK'i kanal bazlı
  (`rating IS NOT NULL` → aktivite zorunlu) yeniden yazmak.

### A5. Dunn ceza katsayıları (`w`) hiçbir yerde saklanmıyor
* Model: Ç1 → (5,5,3), Ç2 → (3,3,6), Ç3 → (10,10,6), Ç4 → eleme, MIXED → **tanımsız**.
* Şemada ne tablo, ne kolon, ne seed var. `D` teriminin yarısı DB'de yok.
* **Etki:** katsayılar koda gömülmek zorunda. Oysa aynı doküman
  `question_options` için "Gülçin yeniden ayarlar, deploy gerekmez" diyor
  (`V2:62-64`). Skorlamanın en hassas parametresi için tam tersi yapılmış.
* Ayrıca `MIXED` quadrant'ı seed'de var (`V2:284`) ama modelde ne toleransı ne
  katsayısı tanımlı → **MIXED çocuk için D hesaplanamaz**.

### A6. Piaget dönem görevi eşlemesi (P terimi) hiçbir yerde yok
* Model: 0-24 ay → `GROSS_MOTOR`, 24-48 ay → `LANGUAGE`, 48-72 ay →
  `SOCIAL_EMOTIONAL`; eşleşirse +15.
* Şemada yaş bandı → domain eşlemesi yok. `AgeBand` enum'u
  (`BAND_0_12/12_24/24_48/48_72`) var ama domain taşımıyor.
* **Ek bulgu:** `target_domain` 5 değer alıyor (`V1:358`) ama dönem görevi olarak
  yalnız 3'ü kullanılıyor. **`FINE_MOTOR` ve `COGNITIVE` hiçbir yaşta +15 alamaz
  ve GELİŞİM slotuna asla giremez.** Renk eşleştirme kartı (COGNITIVE) örneğinin
  hiç GELİŞİM slotuna düşememesinin sebebi bu — bilinçli mi, boşluk mu, netleşmeli.

---

## B. MODEL İLE ŞEMANIN ÇELİŞTİĞİ NOKTALAR

### B1. Zorluk skalası: DB 1-5, model 1-4
`V1:359`: `difficulty SMALLINT NOT NULL CHECK (difficulty BETWEEN 1 AND 5)`
Model d=1 taklit, d=2 çok adımlı, d=3 kural, d=4 soyut üretim. **d=5 tanımsız.**
İçerik ekibi 5 girerse Z terimi tanımsız bir basamakla çalışır. CHECK 1-4'e
çekilmeli ya da d=5 tanımlanmalı.

### B2. `level` 1-5 ama `difficulty` 4 ile sınırlıysa üst seviyede büyüme durur
`child_domain_levels.level` 1-5 (`V1:582`). Z'nin tatlı noktası `d = L+1`.
L=4 ise d=5 gerekir; d 4'te bitiyorsa **L=4 ve L=5'teki çocuk hiçbir zaman +20
alamaz**, sadece 0 veya −5 alır. İki skala arasında tavan uyumsuzluğu var.

### B3. `streak` CHECK'i 0-2, model 3'e kadar sayıyor
`V1:583`: `streak SMALLINT NOT NULL DEFAULT 0 CHECK (streak BETWEEN 0 AND 2)`
Model: "aynı alanda 3 kez başarı → basamak yükselir". 3 hiç yazılamayacağı için
uygulama `2 → (3. başarı) → level++ ve streak=0` geçişini **atomik** yapmak
zorunda. Yapılabilir ama sözleşme kodda gizli. En azından kolon yorumu şart;
tercihen CHECK 0-3.

### B4. Günlük vakit bütçesi çocukta değil ebeveynde
`V2:241-244`: `parent_profiles.daily_time_budget_minutes`.
Model ADIM 0'da bunu **Mavi'nin** alanı olarak listeliyor ("Günlük vakit 20 dk").
İki çocuklu ebeveynde tek bütçe paylaşılır; bir çocuk için 10 dk, diğeri için
30 dk denemez. Q7'nin `HOUSEHOLD` olması bilinçli bir karar (Matris 1.8) ama
**model bunu çocuk alanı sanıyor** — ikisinden biri güncellenmeli.

### B5. Süre filtresi tek aktiviteye bakıyor, günlük toplama bakmıyor
ADIM 1 filtre 3: `duration ≤ 20 dk`. Ama plan **3 aktivite** içeriyor →
en kötü 60 dk. "Günlük vakit 20 dk" diyen anneye 60 dakikalık gün öneriliyor.
Şema buna engel değil (`uq_dpi_plan_slot` sadece 3 slot garanti eder), ama
`daily_plans` üzerinde toplam süre alanı/kısıtı yok. Ürün kararı gerekiyor:
bütçe slot başına mı, gün toplamına mı?

### B6. `sensory_load` tek bir alan değil, üç ayrı kolon
Model Ç4 hard filtresi: "`sensory_load ≥ 3` kartlar elenir".
DB'de tek bir `sensory_load` yok; `noise_load`, `visual_load`,
`physical_intensity` var (`V1:365-367`). Eleme kuralı `max(üçü) ≥ 3` mi,
`noise_load ≥ 3` mü, ortalama mı — **tanımsız**. Üç yorum üç farklı havuz üretir.
(Örnek: Sessiz kutu 1/2/1 → hepsinde geçer; Kukla 3/3/2 → `max` yorumunda elenir.)

### B7. `GOZETIMLI` katılım tipi modelde hiç geçmiyor
`V1:361-362`: `involvement_type IN ('BIRLIKTE','GOZETIMLI','BAGIMSIZ')`.
Model yalnız BİRLİKTE ve BAĞIMSIZ'ı tanımlıyor. Kaygı ≥ 4 olan çocukta:
`BAGIMSIZ` elenir, `BIRLIKTE` ×1.15 alır, **`GOZETIMLI` ne olur belirsiz**.
Havuzun üçte biri kural dışı.

### B8. `v_scorable_activities` iki scope'u da veriyor, model scope'u hiç kullanmıyor
`V1:718-738` view'i `HOME` ve `WORKSHOP` aktivitelerini birlikte döndürüyor
("motor kendi ağırlıklandırır" notuyla). Ama model 6 terimin hiçbirinde scope'a
bakmıyor. Sonuç: **ücretli, şehir bağımlı, tarihli bir atölye etkinliği ev
planının GELİŞİM slotuna düşebilir.** Ayrıca view, atölye aktivitesinin
gelecekte oturumu (`activity_sessions`) olup olmadığını göstermiyor — tarihi
geçmiş bir atölye önerilebilir.

### B9. `gardner_priors` JSONB'de anahtar adı `domain` ama içeriği zekâ tipi
`V2:78`: `[{"domain":"INTRAPERSONAL","delta":0.5}]`. Şemada `domain` zaten
`GROSS_MOTOR/…/COGNITIVE` demek (`V1:358`, `V1:581`). Aynı kelime iki farklı
kavram. JSONB olduğu için CHECK de yok — yanlış değer sessizce yazılır ve
okuma anında enum hatası verir. Anahtar `intelligenceType` olmalı ve
`chk_qo_gardner_priors` benzeri bir JSONB doğrulaması eklenmeli.

---

## C. EKSİK — skorlamaya hizmet edecek veri/tablo yok

| # | Eksik | Model referansı | Şu an nerede? |
|---|-------|-----------------|---------------|
| C1 | Dunn ağırlıkları `w(quadrant)` | D terimi | **Yok** (A5) |
| C2 | Yaş bandı → dönem görevi domain | P terimi | **Yok** (A6) |
| C3 | Skorlama sabitleri: taban 100, P=+15, Z=+20/−25/−5, G=+10/+15/−15, T=−30, B=1.15, eşikler 4.0/2.5/1.5 | Tüm formül | **Yok** |
| C4 | Oy delta sabitleri: +0.30 / +0.15 / −0.15 | ADIM 4 | **Yok** |
| C5 | Kalkan parametreleri: ±0.3 sınırı, güven eşiği 0.6, günlük tavan +0.9 | ADIM 5 / 4 Fren | **Yok** |
| C6 | LLM ham çıktısı + güven skoru + uygulanmadı kaydı | ADIM 5, Fren 2 ("sadece loglanır") | **Yok** |
| ~~C7~~ | ~~"Kolaylaştırma" metni~~ | Z terimi | ✅ **V8 kapattı** — `easier_variation`. Ama bkz. N1/N2 |
| C8 | Etkinin kaynağı (EMOJI / TEXT / PRIOR) | Fren 3 çifte sayım | **Yok** — `feedback_effects`'te kolon yok |
| C9 | Zekâ alanı başına **örneklenme** sayacı | KEŞİF slotu "en az örneklenen alan" | **Yok** — `feedback_count` geri bildirim sayar, öneri sayısını değil |
| C10 | STRUGGLED sonrası "bir kolay öner" durumu | ADIM 4, 😐 satırı | **Yok** — hiçbir yerde saklanmıyor |
| C11 | Konfor/gelişim sorusunun sorulma kaydı | ADIM 6 trigger | **Yok** — `preference_mode` var, `asked_at` yok |

Detaylar:

### C7. ✅ V8 ile kapandı (kısmen)
`easier_variation` ve `harder_variation` eklendi (`V8:5-6`). Metnin gideceği yer
artık var. **Ama bağlanmadı** — Z=+20'nin dayanağı olan `is_scaffolded` ile bu
kolon arasında hiçbir ilişki kurulmamış. Devamı N1 ve N2'de.

### C8. `feedback_effects` çifte sayımı ayırt edemiyor
`V1:673-683`: `feedback_id`, `intelligence_type`, `delta`, `reversed_at`.
Eksikler:
* `source` yok → emoji kaynaklı +0.30 ile metin kaynaklı +0.15 ayrılamaz →
  **Fren 3 denetlenemez** (birleştirme yapıldı mı, sonradan doğrulanamaz).
* `confidence` yok → Fren 2 kararı arşivlenmiyor.
* `child_id` yok → günlük +0.9 tavanı (Fren 4) için
  `feedback_effects → feedback → child` çift join gerekiyor ve bunu destekleyen
  indeks yok. Mevcut indeksler yalnız `feedback_id` üzerinde (`V1:685-686`).
* `delta` üzerinde **hiç CHECK yok** (`NUMERIC(3,2) NOT NULL`) → Fren 1'in
  ±0.3 sınırı DB tarafında hiç korunmuyor.
* Yalnız `intelligence_type` var → domain seviyesi/streak değişimleri ve
  SENSORY/INVOLVEMENT teşhis sonuçları **geri alınabilir şekilde loglanmıyor**;
  M4'ün "her skor değişimi denetlenebilir ve geri alınabilir" iddiası yarım.

### C9. KEŞİF slotu için "örneklenme" verisi yok
Model: "en az örneklenen alandan seç". `child_intelligence_scores.feedback_count`
(`V1:567`) **geri bildirim** sayar. Bir alan 5 kez önerilip hiç oylanmadıysa
sayaç 0 kalır ve alan "hiç denenmemiş" gibi görünür → keşif sonsuza kadar aynı
alanı seçer. Öneri sayısı `daily_plan_items ⋈ activities` ile türetilebilir ama
bunu destekleyen indeks yok (`idx_dpi_activity` var, zekâ tipine göre yok).

---

## N. V8 İLE GELEN YENİ BULGULAR

V8 doğru yöne bir adım ama Z terimini **yarım bağladı**. Beş sonuç:

### N1. `is_scaffolded` ile `easier_variation` arasında hiçbir bağ yok — iki kaynak
* `activities.is_scaffolded BOOLEAN NOT NULL DEFAULT FALSE` (`V1:368`)
* `activity_instructions.easier_variation TEXT` (`V8:5`) — **ayrı tabloda, nullable**
* Aralarında ne FK mantığı, ne CHECK, ne trigger var.

Dört kombinasyon mümkün ve ikisi bozuk:

| `is_scaffolded` | `easier_variation` | Sonuç |
|---|---|---|
| TRUE | dolu | ✅ doğru |
| FALSE | NULL | ✅ doğru |
| **TRUE** | **NULL** | ⚠️ Z=+20 verilir, anneye gösterilecek yardım **yok** |
| **FALSE** | **dolu** | ⚠️ Yardım yazılmış ama Z=+20 **verilmez**, içerik boşa gider |

**Etki:** "Kolaylaştırma var mı?" sorusunun DB'de iki cevabı var ve
uyuşmaları garanti değil. Motor hangisini okuyacak, kararlaştırılmalı. En temiz
çözüm: `is_scaffolded`'ı türetilmiş hale getirmek
(`easier_variation IS NOT NULL` = scaffolded) ve kolonu düşürmek — böylece
"metin varsa yardım var" tek kuralı kalır ve içerik ekibi tek yer doldurur.

### N2. `activity_instructions` satırı zorunlu değil — `is_scaffolded=TRUE` metinsiz kalabilir
`activity_instructions.activity_id BIGINT PRIMARY KEY` (`V1:424`) — 1:1 ama
**opsiyonel**; V1 yorumu bunu açıkça söylüyor ("Optional (a workshop listing may
not need it)"). V8 bu opsiyonelliği değiştirmedi.
Yani `is_scaffolded = TRUE` olan bir aktivitenin `activity_instructions` satırı
hiç olmayabilir. N1'in en kötü kutusu bu yüzden sadece teorik değil, **varsayılan
durum**: içerik ekibi bayrağı işaretler, talimat satırını hiç açmaz.
Ev aktiviteleri için `activity_instructions` satırının varlığını zorunlu kılan
bir kısıt (ya da `status='PUBLISHED'` öncesi bir doğrulama) yok.

### N3. `harder_variation` formülde karşılıksız
Model'in Z terimi `d < L` durumunda düz **−5 (sıkılma)** yazıyor ve
"kolaylaştırma sorusuna gerek yok; bu durumda bakılmaz" diyor. Yani
zorlaştırmanın skora hiçbir etkisi tanımlı değil.
`harder_variation` (`V8:6`) bugün **ölü kolon**. İki seçenek:
* İçerik zenginliği olarak kalsın (anne isterse zorlaştırsın) — o zaman modele
  "skoru etkilemez" diye yazılmalı, yoksa ileride biri +bonus sanır.
* Ya da modele girsin: `d < L` **ve** `harder_variation` doluysa ceza −5 yerine
  0 olsun (sıkılma zorlaştırmayla telafi edilir). Pedagojik olarak tutarlı olan
  bu ama **ürün kararı**.

### N4. `observation_tip` geri besleme akışına bağlanmamış
`observation_tip` (`V8:7`) tam olarak akşam oyunun kalitesini yükseltecek veri —
anneye "şuna dikkat et" der, oy da o gözleme dayanır. Ama `feedback` tarafında
ne bu ipucuna referans var, ne "gözlem yapıldı mı" alanı. Şu an sadece
gösterilen bir metin; ADIM 4'ün teşhis zinciriyle (SENSORY / INVOLVEMENT /
INTEREST) ilişkilendirilmemiş. Kaçırılmış bağ.

### N5. `activity_outcomes` pedagoji alanlarıyla çapraz denetlenmiyor
`activity_outcomes.outcome TEXT NOT NULL` (`V8:14`) serbest metin.
Bir aktivitenin `target_domain = LANGUAGE` olup outcome'larının tamamen motor
kazanımlar yazması mümkün. Skorlamayı bozmaz (motor bu tabloyu okumuyor) ama
anneye gösterilen vaat ile puanı üreten sınıflandırma **ayrışabilir** — değer
önerisi "en doğru etkinlik" olan bir üründe bu bir güven riski.
Ayrıca `uq_activity_outcomes_activity_outcome UNIQUE (activity_id, outcome)`
TEXT üzerinde tekillik kuruyor: uzun metinlerde indeks boyutu sorun olabilir ve
"aynı kazanım, farklı noktalama" ikilemesini engellemez.

---

## D. ŞEMADA VAR, MODELDE KARŞILIĞI YOK (ölü veri)

Bunlar hata değil ama ya model eksik ya kolon gereksiz — netleşmeli:

| Kolon | Nerede | Durum |
|---|---|---|
| `focus_band` (SHORT/MEDIUM/LONG) | `child_profile_snapshots` (`V2:186`), Q6 | **Formülün hiçbir teriminde kullanılmıyor.** Süre filtresi yalnız `daily_time_budget`'a bakıyor. Q6 48-72 ay için sorulup hiç okunmuyor. |
| `social_orientation` | `child_profile_snapshots` (`V2:185`), Q5 | Skorda yok. Yalnız `gardner_priors` üzerinden dolaylı etki ediyor; kolonun kendisi ölü. |
| `preference_mode` (BALANCED/KEYIF/GELISIM) | `children` (`V1:172`) | ADIM 6'da tetiklenmesinden söz ediliyor ama **formülü nasıl değiştirdiği tanımsız**. Üç mod da aynı skoru üretiyor. |
| `mobility_preference` | snapshot | D'de kullanılıyor ama kaynağı modelle çelişiyor (A3). |
| `activity_attributes` / `attribute_definitions` EAV | `V1:522-550` | Skorlama hiç okumuyor. Sorun değil, ama "11 skorlama alanı gerçek kolon" notu ile birlikte kapsam dışı olduğu yazılmalı. |
| `harder_variation` | `activity_instructions` (`V8:6`) | **YENİ.** Modelde `d < L` düz −5; zorlaştırmanın skora etkisi tanımsız. Bkz. N3. |
| `observation_tip` | `activity_instructions` (`V8:7`) | **YENİ.** Geri besleme akışına bağlanmamış. Bkz. N4. |

---

## E. TUTARLILIK / HİJYEN

### E1. `updated_at` trigger'ı 3 tabloda eksik
`set_updated_at()` trigger'ı 11 tabloya bağlanmış; **bağlanmamış olanlar:**
* `child_intelligence_scores` (`V1:568`) — algoritmanın en sık yazdığı tablo
* `child_domain_levels` (`V1:584`)
* `user_consents` (`V7:34`)

Üçünde de `updated_at` kolonu var ve `DEFAULT now()` alıyor, ama UPDATE'te
kendiliğinden tazelenmiyor. Uygulama unutursa **skor değişti ama updated_at
ilk yazma anını gösteriyor** olur — "sistem öğreniyor" sinyalinin zaman ekseni
bozulur.

### E2. `daily_plan_items` ile `recommendations` arasında bağ yok
Model ADIM 2: "Ham skor DB'de `score_breakdown` içinde saklanır, açıklanabilirlik
için." `score_breakdown` yalnız `recommendations`'ta (`V1:703`).
`daily_plan_items`'ta `score NUMERIC(5,2)` var ama `score_breakdown` da yok,
`recommendation_id` FK'si de yok (`V1:610-625`). Yani **plana giren kartın skor
dökümüne ulaşmanın yolu yok**; ancak `(child_id, activity_id, tarih)` ile
tahmini eşleştirilebilir. `recommendations`'ta `slot_type` ve
`daily_plan_id` de yok, gün başına tekillik kısıtı da yok.

### E3. `recommendations` şişme riski
Her gün ~18 skorlanan kart × çocuk sayısı, tek indeksi
`(child_id, created_at DESC)` (`V1:708`). Retention/partition politikası yok.
Arşiv amaçlı bir tablo için TTL kararı verilmeli.

### E4. `child_intelligence_scores`'ta `created_at` yok
Sadece `updated_at` var (`V1:568`). Profilin ne zaman ilk kurulduğu (8 satırın
insert anı) kaydedilmiyor; "ilk gün 3.0'dan başladı" iddiası veri ile
gösterilemiyor.

### E5. Sensory indeksi eksik eksenli
`idx_activities_sensory ON activities (noise_load, physical_intensity)`
(`V1:388`) — `visual_load` indekste yok. Ç4 elemesi üç eksene de bakacaksa
indeks kapsamı yetmez.

### E6. `gardner_priors` snapshot'ta birikiyor, clamp yok
`ProfileSnapshotService.rebuild()` tüm tamamlanmış oturumların prior'larını
`priors.addAll(...)` ile üst üste ekliyor. Yaş bandı değiştikçe aynı prior
tekrar tekrar listeye giriyor. V2'nin kendi yorumu (`V2:316`) "servis sonucu
[2.5, 3.5] aralığına clamp'ler" diyor — **kodda clamp yok**, `child_intelligence_scores`
tarafında da prior'ın uygulanıp uygulanmadığını gösteren bir işaret yok →
bant geçişinde çifte uygulama riski.

---

## F. DOĞRU KURULMUŞ OLANLAR (regresyon olmasın diye kayda geçiyorum)

* `activities`: `target_intelligence`, `secondary_intelligence`, `target_domain`,
  `difficulty`, `duration_minutes`, `involvement_type`, üç duyusal yük ve
  `is_scaffolded` — **formülün ihtiyaç duyduğu 11 alanın hepsi NOT NULL ya da
  nötr DEFAULT 3 ile mevcut.** Motorun null'a dallanmaması bilinçli ve doğru.
* `chk_activities_distinct_intelligence` — ikincil = hedef olamaz; köprü
  mekaniğinin (G=+15) anlamlı kalmasını garantiliyor. İyi kısıt.
* `child_intelligence_scores.score CHECK BETWEEN 0.00 AND 5.00` — "makas kuralı"
  DB'de zorlanıyor, sadece kodda değil. Doğru yer.
* `DEFAULT 3.00` ve `level DEFAULT 1` — "ilk gün herkes ortadan başlar"
  varsayımı şema seviyesinde.
* `uq_dpi_plan_slot (daily_plan_id, slot_type)` + `uq_dpi_plan_activity` —
  günde tam 3 slot, aynı aktivite iki slota giremez. Portföy kuralı korunuyor.
* `slot_type IN ('STRENGTHEN','DEVELOP','EXPLORE')` — GÜÇLENDİRME / GELİŞİM /
  KEŞİF ile birebir.
* `uq_daily_plans_child_date` — T teriminin "dün plandaydı mı" sorgusu tek satıra
  düşüyor, `idx_daily_plans_child (child_id, plan_date DESC)` de bunu destekliyor.
* `feedback.resolved_reason IN ('SENSORY','INVOLVEMENT','INTEREST')` — 🙁
  teşhis zincirinin üç çıkışı da modellenmiş. "Yanlış öğrenmeme sigortası" veri
  modelinde var.
* `uq_feedback_plan_item UNIQUE (daily_plan_item_id)` — M3 (plan başına tek
  geri bildirim) doğru; Postgres çoklu NULL'a izin verdiği için atölye yorumu
  kanalı engellenmiyor.
* `feedback.accepted` / `bulk_flag` — M9 / M6 karşılıkları yerinde.
* `child_profile_snapshots` + `uq_cps_current_per_child` — versiyonlu profil ve
  tek güncel satır garantisi; "gelişim sinyali" iddiasının altyapısı sağlam.

---

## G. ÖNERİLEN V9 İSKELETİ (uygulanmadı, tartışmaya açık taslak)

> V8 yalnız C7'yi kapattı; aşağıdaki taslak V8 sonrası kalan işi gösterir.
> `scaffold_notes` önerisi V8'in `easier_variation`'ı ile karşılandığı için
> çıkarıldı, yerine N1'in bağlama kısıtı eklendi.

```sql
-- C1: Dunn ağırlıkları ve tolerans profilleri, tek yerde, deploysuz ayarlanabilir
CREATE TABLE dunn_profiles (
    quadrant        VARCHAR(10) PRIMARY KEY
        CHECK (quadrant IN ('C1','C2','C3','C4','MIXED')),
    noise_tolerance    SMALLINT CHECK (noise_tolerance    BETWEEN 1 AND 5),
    visual_tolerance   SMALLINT CHECK (visual_tolerance   BETWEEN 1 AND 5),
    mobility_tolerance SMALLINT CHECK (mobility_tolerance BETWEEN 1 AND 5),
    noise_weight       SMALLINT CHECK (noise_weight    >= 0),
    visual_weight      SMALLINT CHECK (visual_weight   >= 0),
    mobility_weight    SMALLINT CHECK (mobility_weight >= 0),
    hard_filter_load   SMALLINT,  -- C4 icin 3; digerleri NULL (eleme yok)
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- C1=3/3/3 w(5,5,3) | C2=4/4/5 w(3,3,6) | C3=2/2/3 w(10,10,6)
-- C4=1/2/2 hard_filter_load=3 | MIXED=? (KARAR GEREKIYOR)

-- C2: Piaget donem gorevi
CREATE TABLE developmental_period_tasks (
    age_band      VARCHAR(16) PRIMARY KEY
        CHECK (age_band IN ('BAND_0_12','BAND_12_24','BAND_24_48','BAND_48_72')),
    target_domain VARCHAR(30) NOT NULL
        CHECK (target_domain IN ('GROSS_MOTOR','FINE_MOTOR','LANGUAGE','SOCIAL_EMOTIONAL','COGNITIVE'))
);

-- C3/C4/C5: tum sabitler tek tabloda, versiyonlu
CREATE TABLE scoring_parameters (
    key         VARCHAR(60) PRIMARY KEY,   -- 'P_BONUS', 'Z_SWEET_SPOT', 'T_PENALTY',
    value       NUMERIC(6,3) NOT NULL,     -- 'G_COMFORT_THRESHOLD', 'LLM_MIN_CONFIDENCE' ...
    description TEXT,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- C6: LLM sinyal defteri (uygulanan VE elenen sinyaller)
CREATE TABLE feedback_text_signals (
    id                BIGSERIAL PRIMARY KEY,
    feedback_id       BIGINT NOT NULL REFERENCES feedback (id) ON DELETE CASCADE,
    intelligence_type VARCHAR(30) NOT NULL,
    suggested_delta   NUMERIC(3,2) NOT NULL,
    confidence        NUMERIC(3,2) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    applied           BOOLEAN NOT NULL,
    skip_reason       VARCHAR(30)   -- LOW_CONFIDENCE | DAILY_CAP | DELTA_CLAMP | DEDUPED
        CHECK (skip_reason IS NULL OR skip_reason IN
              ('LOW_CONFIDENCE','DAILY_CAP','DELTA_CLAMP','DEDUPED')),
    raw_model_output  JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- N1: is_scaffolded'i easier_variation'dan turet, iki kaynagi tekile indir.
-- Secenek A (onerilen): bayragi dusur, motor metnin varligina baksin.
--   ALTER TABLE activities DROP COLUMN is_scaffolded;
--   CREATE OR REPLACE VIEW v_scorable_activities AS SELECT ...,
--     (ai.easier_variation IS NOT NULL) AS is_scaffolded
--     FROM activities a LEFT JOIN activity_instructions ai ON ai.activity_id = a.id ...
-- Secenek B: bayragi birak, tutarliligi kisitla zorla (trigger gerekir,
--   CHECK tablolar arasi calismaz).

-- N2: PUBLISHED bir ev aktivitesi talimatsiz yayina cikamasin
--   (CHECK tablolar arasi calismadigi icin BEFORE INSERT/UPDATE trigger ile)

-- C8: etkinin kaynagi ve denetlenebilirligi
ALTER TABLE feedback_effects
    ADD COLUMN child_id   BIGINT REFERENCES children (id) ON DELETE CASCADE,
    ADD COLUMN source     VARCHAR(10) NOT NULL DEFAULT 'EMOJI'
        CHECK (source IN ('EMOJI','TEXT','PRIOR','MERGED')),
    ADD COLUMN confidence NUMERIC(3,2),
    ADD CONSTRAINT chk_fe_delta_bound CHECK (delta BETWEEN -0.30 AND 0.30);
CREATE INDEX idx_fe_child_day ON feedback_effects (child_id, created_at DESC);

-- C9: kesif slotu icin ornekleme sayaci
ALTER TABLE child_intelligence_scores
    ADD COLUMN exposure_count INTEGER NOT NULL DEFAULT 0 CHECK (exposure_count >= 0),
    ADD COLUMN created_at     TIMESTAMPTZ NOT NULL DEFAULT now();

-- C10: STRUGGLED sonrasi gecici zorluk indirimi
ALTER TABLE child_domain_levels
    ADD COLUMN difficulty_offset SMALLINT NOT NULL DEFAULT 0
        CHECK (difficulty_offset BETWEEN -1 AND 0);

-- C11: konfor/gelisim sorusu tetik kaydi
ALTER TABLE children
    ADD COLUMN preference_mode_asked_at TIMESTAMPTZ,
    ADD COLUMN preference_mode_set_at   TIMESTAMPTZ;

-- E1: eksik updated_at trigger'lari
CREATE TRIGGER trg_cis_updated_at BEFORE UPDATE ON child_intelligence_scores
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_cdl_updated_at BEFORE UPDATE ON child_domain_levels
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_user_consents_updated_at BEFORE UPDATE ON user_consents
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- E2: plan kalemi -> skor dokumu bagi
ALTER TABLE daily_plan_items
    ADD COLUMN recommendation_id BIGINT REFERENCES recommendations (id) ON DELETE SET NULL,
    ADD COLUMN score_breakdown   JSONB;

-- A2: Ç1 tolerans duzeltmesi (model dogruysa)
UPDATE question_options SET noise_sensitivity = 3, visual_sensitivity = 3
 WHERE dunn_quadrant = 'C1';

-- A4: feedback silme kilidi
ALTER TABLE feedback DROP CONSTRAINT chk_feedback_target;
ALTER TABLE feedback ADD CONSTRAINT chk_feedback_target
    CHECK (daily_plan_item_id IS NOT NULL OR rating IS NOT NULL OR comment IS NOT NULL);

-- B1: zorluk skalasi
ALTER TABLE activities DROP CONSTRAINT activities_difficulty_check;
ALTER TABLE activities ADD CONSTRAINT chk_activities_difficulty
    CHECK (difficulty BETWEEN 1 AND 4);
```

---

## H. KARAR BEKLEYEN 6 SORU

Bunlar teknik değil ürün/pedagoji kararı; cevap gelmeden V8 yazılmamalı:

1. **Tolerans nereden gelir?** Ç tipinden sabit mi (model), yoksa Q2+Q3'ten
   bağımsız mı (şema)? (A3)
2. **Ç1 = 3/3/3 mü, 4/4 mü?** Model ile seed çelişiyor. (A2)
3. **MIXED quadrant'ın tolerans ve ağırlıkları ne?** Şu an tanımsız. (A5)
4. **`sensory_load ≥ 3` hangi eksene bakar?** max / noise / ortalama? (B6)
5. **`GOZETIMLI` kaygılı çocukta elenir mi, çarpan alır mı?** (B7)
6. **Günlük 20 dk bütçesi slot başına mı, gün toplamına mı?** (B5)

7. **`is_scaffolded` mi `easier_variation` mı otorite?** V8 ikisini yan yana
   bıraktı; Z=+20 hangisine bakacak? (N1)
8. **`harder_variation` skoru etkileyecek mi?** `d < L` cezasını −5'ten 0'a
   çekmeli mi, yoksa salt içerik mi kalsın? (N3)

Ek olarak: `FINE_MOTOR` ve `COGNITIVE` domainlerinin hiçbir yaşta GELİŞİM
slotuna girememesi bilinçli mi? (A6)
