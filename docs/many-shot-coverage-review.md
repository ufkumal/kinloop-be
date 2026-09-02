# Many-shot örnek setinin yeterlilik incelemesi

Tarih: 31 Ağustos 2026
İncelenen: main **`3e78d8a`** · commit `4da6670` “many shot model”
Dosyalar: `FeedbackClassificationPrompt.java` · `resources/prompts/feedback-classification-many-shot.md`

Örnek sayısı **41 → 13**. Sistem talimatı (§4, 59 satır) değişmemiş; yalnız örnek seti
değişti ve dosya adı `few-shot` → `many-shot` oldu.

---

## 0 · Sonuç

**Sayı sorun değil, dağılım sorun.** 13 iyi seçilmiş örnek 41 gevşek örnekten iyi
olabilir. Ama bu 13 örnek şemanın bir ucunda toplanmış: Gardner sinyalleri iyi
örneklenmiş, **ipucu alanlarının üçte biri hiç örneklenmemiş**.

Üç somut sorun:

| | Sorun | Ağırlık |
|---|---|---|
| 1 | `difficulty_hint` ve `duration_hint` **hiç örneklenmemiş**; ZORLANDI butonu **hiç yok** | Kapsam |
| 2 | Ö8 ve Ö13 yapısal olarak aynı, **farklı güven değeri alıyor** — hem de 0.70 eşiğinin iki yanında | Tutarlılık |
| 3 | `mvn test` kırmızı: eski test 41 örnek bekliyor, **0 buluyor** | Mekanik |

---

## 1 · Kapsam matrisi

13 örneğin ürettiği çıktılar:

| # | Konu | Buton | target_corr | secondary_hint | sensory | involv | diff | situation | duration | conflict | conf |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Teyit | SEVDI | – | – | – | – | – | – | – | false | 0.90 |
| 2 | Ekleme | SEVDI | – | VERBAL_LING | – | – | – | – | – | false | 0.85 |
| 3 | Yalanlama | SEVDI | INTERPERSONAL | INTRAPERSONAL | – | ALONE | – | – | – | **true** | 0.85 |
| 4 | Yalanlama çifti | SEVDI | NATURALISTIC | MUSICAL | – | – | – | – | – | **true** | 0.85 |
| 5 | Çoklu ipucu | SEVDI | – | – | CROWDING | TOGETHER | – | – | – | false | 0.80 |
| 6 | Duyusal gürültü | OLMADI | – | – | NOISE | – | – | – | – | false | 0.85 |
| 7 | Durumsal | OLMADI | – | – | – | – | – | TRANSIENT | – | false | 0.85 |
| 8 | Değinmeme | SEVDI | – | LOGICAL_MATH | – | – | – | – | – | false | **0.65** |
| 9 | İkinci el | SEVDI | – | – | – | – | – | – | – | false | 0.50 |
| 10 | Çelişki | SEVDI | VERBAL_LING | – | – | – | – | – | – | **true** | 0.80 |
| 11 | Olumsuz duygu | OLMADI | – | – | – | – | – | – | – | false | 0.55 |
| 12 | Sağlık iması | OLMADI | – | – | – | – | – | – | – | false | 0.30 |
| 13 | Coşku | SEVDI | – | VISUAL_SPATIAL | – | – | – | – | – | false | **0.85** |

### Alan bazında kapsam

| Alan | Kapsam | Eksik |
|---|---|---|
| `situation_hint` | **1/1** | — |
| `involvement_hint` | **2/2** | — |
| `sensory_hint` | 2/4 | **VISUAL, MOVEMENT** |
| `difficulty_hint` | **0/2** | **HARDER, EASIER** |
| `duration_hint` | **0/2** | **LONG, SHORT** |
| `target_correction` | 3/8 | LOGICAL_MATHEMATICAL, MUSICAL, BODILY_KINAESTHETIC, VISUAL_SPATIAL, INTRAPERSONAL |
| `secondary_hint` | 5/8 | BODILY_KINAESTHETIC, INTERPERSONAL, NATURALISTIC |

### Buton kapsamı

| Buton | Örnek |
|---|---|
| SEVDI | 9 |
| OLMADI | 4 |
| **ZORLANDI** | **0** |

---

## 2 · Kapsam boşlukları · ayrıntı

### 2.1 · `difficulty_hint` hiç örneklenmemiş — en ciddi boşluk

Bu alan **gerçek etki üretiyor**. `FeedbackLearningService.applyDifficultyHint()` basamak
sayacını oynatıyor:

```
llm_difficulty_hint_harder_delta =  0.20
llm_difficulty_hint_easier_delta = -0.20
```

Sayaç ±3.0 / −1.0 eşiklerini geçtiğinde çocuğun basamağı değişiyor, basamak da `Z`
terimi üzerinden skorun **45 puanlık** bandını belirliyor. Yani hiç örneklenmemiş bir
alan, öneri motorunun en oynak terimini besliyor.

Sistem talimatının 8. kuralı bu alanı sayıyor (*“Duyusal, katılım, **zorluk**, durum ve
süre ipuçları Gardner alanlarından BAĞIMSIZDIR”*), ama tek bir örnek yok.

### 2.2 · ZORLANDI butonu hiç yok

Üç butondan biri modele hiç gösterilmiyor. İki sonucu var:

* v4 §7.1 ZORLANDI için ayrı davranış tanımlıyor: `secondary_hint` ve `target_correction`
  **kaydedilir ama uygulanmaz**. Model bu bağlamı hiç görmemiş.
* Zorluk ipucu doğal olarak ZORLANDI ile gelir (“çok zor geldi”, “kolay buldu”).
  **İki boşluk aynı yerde birleşiyor** — 2.1 ile 2.2 aslında tek bir eksik örnek ailesi.

### 2.3 · `duration_hint` hiç örneklenmemiş

LONG/SHORT hiç yok. Bu alanın puana etkisi **yok** (yalnız
`feedback_llm_classifications`'a yazılıyor, v4 §7 de böyle diyor), o yüzden 2.1'den
hafif. Ama şemada duruyor ve model onu hiç dolu görmemiş.

### 2.4 · `sensory_hint` yarım

NOISE (Ö6) ve CROWDING (Ö5) var. **VISUAL ve MOVEMENT yok.**

`applySensoryHint()` bu değerleri doğrudan eksene çeviriyor:

```java
case VISUAL   -> visualAdjustment   -= step;
case MOVEMENT -> movementAdjustment -= step;
```

Yani örneklenmemiş iki değer de gerçek etki üretiyor — çocuğun görsel ve hareket
toleransını sıkıyor.

### 2.5 · DISLIKED + `target_correction` kenar durumu yok

v4 §3.5 ve §10 bu duruma özel bir kural koymuş: **OLMADI oyunda `target_correction`
null sayılır, `conflict` false olur** (metin butonu doğruluyor, çelişki yok). Kod bunu
`sanitizeClassification():214-216`'da uyguluyor.

Model bu durumu hiç görmemiş. Kod zaten temizlediği için **zararsız** — ama model
gereksiz yere dolduruyorsa her çağrıda boşa token harcanıyor ve `conflict` log'u
kirleniyor.

### 2.6 · Kural 7 örneklenmemiş

| Kural | Örnek |
|---|---|
| 6 · İkinci elden aktarım → conf ≤ 0.60 | ✅ Ö9 |
| **7 · Belirsizlik ifadesi (“emin değilim”, “sanırım”, “galiba”) → conf ≤ 0.60** | ❌ **yok** |

İki kural aynı eşiği paylaşıyor ama yalnız biri örneklenmiş.

### 2.7 · Gardner alanı dağılımı

`BODILY_KINAESTHETIC` hiçbir çıktıda geçmiyor — ne `target_correction`, ne
`secondary_hint`. v4'ün Grup A'sı *“Sekiz Gardner alanı, temiz olumlu sinyal”*
başlığıyla sekizini de tek tek örnekliyordu; o yapı kaybolmuş.

Bu tek başına kritik değil (etiketler sistem talimatında listeleniyor), ama
`target_correction` yalnız 3 alanda görülmüş ve o alan **kredi tutma** kararını
veriyor — yanlış alan yazılırsa çocuk hak ettiği +0.30'u kaybediyor.

---

## 3 · İç tutarsızlık · Ö8 ↔ Ö13

**Bu, kapsam boşluklarından daha ciddi.** İki örnek yapısal olarak aynı, farklı etiket
alıyor — hem de tam karar eşiğinin iki yanında.

| | Ö8 | Ö13 |
|---|---|---|
| Etkinlik | Kukla ile sohbet | Pencereden kuş gözlemi |
| Hedef zekâ | VERBAL_LINGUISTIC | NATURALISTIC |
| Metin | “Legolarla evi tek başına kurdu, hangi parçanın nereye gideceğini uzun uzun düşündü” | “Hayatının en güzel günüydü, resim yapmayı o kadar çok sevdi ki bir daha hiç bırakmak istemiyor” |
| Metin hedeften bahsediyor mu | **Hayır** — kukla geçmiyor | **Hayır** — kuş gözlemi geçmiyor |
| Başka alan ekliyor mu | Evet → LOGICAL_MATHEMATICAL | Evet → VISUAL_SPATIAL |
| **Sınıf (kural 4)** | **DEĞİNMEME** | **DEĞİNMEME** |
| Verilen confidence | **0.65** | **0.85** |

Sistem talimatı, kural 4:

> **DEĞİNMEME -> secondary_hint dolar AMA confidence en fazla 0.65 olur.**
> Metin hedef zekâdan hiç bahsetmiyor. Hedefin yaşanıp yaşanmadığı bilinmiyor;
> belirsizlik confidence ile bildirilir.

Ö13 bu kuralı çiğniyor. Ve rastgele bir yerde değil:

```
llm_feedback_confidence_threshold = 0.70

Ö8  → 0.65  → eşiğin ALTINDA → secondary_hint UYGULANMAZ
Ö13 → 0.85  → eşiğin ÜSTÜNDE → secondary_hint UYGULANIR (+0.15)
```

**Aynı yapıdaki iki girdi, biri puanı değiştiriyor biri değiştirmiyor.** Model 13
örneğin ikisinden bu eşik hakkında birbirini çürüten sinyal alıyor. 41 örnekli sette
bu tür bir çelişki başka örneklerle seyrelirdi; 13 örnekte ikisi **setin %15'i**.

Ö13'ün amacı v4 Grup N'in “coşku deltayı büyütmez” dersini vermek. Ders doğru, ama
seçilen metin yanlışlıkla DEĞİNMEME kategorisine de giriyor. **İki ders tek örneğe
sıkıştırılmış ve biri diğerini bozuyor.**

### Düzeltme seçenekleri

| Seçenek | Nasıl |
|---|---|
| **(a)** Metni hedefi doğrulayacak şekilde değiştir | “Kuşları saatlerce izledi, hayatının en güzel günüydü, sonra resmini yaptı” → EKLEME olur, `secondary_hint = VISUAL_SPATIAL`, conf 0.85 tutarlı kalır |
| (b) Confidence'ı 0.65'e indir | Kural korunur ama örnek artık “coşku” dersini veremez, çünkü çıktı uygulanmıyor |

**(a) önerilir** — tek cümlelik değişiklik, hem coşku dersini korur hem kuralı çiğnemez.

---

## 4 · Sarkan referanslar

İki notta, sistem talimatında karşılığı olmayan ifade var:

| Örnek | Notta yazan | Talimatta olan |
|---|---|---|
| Ö13 | *“güven **rubriğin açık-ifade bandında** kalır”* | Talimatta **rubrik ya da güven bandı tanımı yok** — kurallar yalnız tavan koyuyor (0.65 / 0.60 / 0.40) |
| Ö12 | *“güven ≤ **0.35**”* | Kural 10: *“confidence değerini **0.40'ın altında** ver”* |

Ö12'nin JSON'u 0.30 — ikisine de uyuyor, sorun yalnız öğretici metinde. Ama bu notlar
prompt'a **aynen giriyor**; model olmayan bir rubriğe atıf okuyor.

Muhtemelen bende olmayan daha yeni bir şartname nüshasında bir güven rubriği tanımlı.
Öyleyse **talimata da eklenmeli**; değilse notlar düzeltilmeli.

---

## 5 · Mekanik · `mvn test` kırmızı

```
Tests run: 159   Failures: 1   Errors: 0   Skipped: 27
BUILD FAILURE
```

**İyi haber:** `contextLoads` **düzelmiş** — 25 repository'nin 25'i sağlanıyor, eksik 0.
Hata sayısı 1'den 0'a indi. Bu, uzun süredir açık olan bir bulguydu.

**Kalan tek başarısızlık:**

```
FeedbackClassificationPromptTest.containsAllFortyOneV4FewShotExamples
expected: <41> but was: <0>
```

Test şu düzeni arıyor:

```java
Pattern.compile("\\*\\*Ö\\d+\\*\\*")     // **Ö1**, **Ö2**, …
```

Yeni dosya ise `### Örnek 1 — Teyit` biçiminde. Yani test **13 de bulmuyor, 0
buluyor** — sayı beklentisi güncellense bile regex tutmaz. İkisi birden düzeltilmeli:

```java
Pattern.compile("### Örnek \\d+")   // ve assertEquals(13, count)
```

Diğer test (`containsV4SafetyRules`) geçiyor; “Kâğıtları fırlattı” yazım hatalı iddiası
kaldırılmış, `"doktor da gelişim geriliği olabilir dedi"` iddiası Ö12'de karşılığını
buluyor.

---

## 6 · Biçim notu

Yeni dosya `**User:**` / `**Assistant:**` etiketleri kullanıyor. Bu etiketler gerçek
konuşma turu değil — hepsi **tek bir system mesajının içine** gömülü metin
(`SYSTEM_PROMPT = INSTRUCTIONS + "\n\nAŞAĞIDAKİ 13 ÖRNEĞİ…" + loadManyShotExamples()`).

Bu v4'te de böyleydi, yeni bir sorun değil. Ama etiketler artık gerçek turlar varmış
gibi göründüğü için not düşüyorum: Anthropic Messages API'de örnekler **gerçek
alternatif `user`/`assistant` turları** olarak verildiğinde genelde daha iyi tutunur.
Ölçmedim; bu bir gözlem, bulgu değil.

---

## 7 · Öneri

Set 13'ten **~18'e** çıkarılsın. Hâlâ 41'in yarısından az.

| # | Eklenecek örnek | Kapattığı boşluk |
|---|---|---|
| 1 | **ZORLANDI** + `difficulty_hint = HARDER` (“çok zor geldi, yapamadı”) | 2.1 + 2.2 birlikte |
| 2 | SEVDI + `difficulty_hint = EASIER` (“çok kolaydı, sıkıldı”) | 2.1 |
| 3 | OLMADI + `sensory_hint = VISUAL` ya da `MOVEMENT` | 2.4 |
| 4 | **OLMADI + `target_correction` dolu gelirse null'a çekilir** | 2.5 kenar durumu |
| 5 | Herhangi bir buton + `duration_hint` | 2.3 |

Ve **Ö13 düzeltilsin** (bölüm 3, seçenek a) — bu bir ekleme değil, tek cümlelik
düzeltme, ama listenin en önemli maddesi.

İsteğe bağlı: bir örnekte `BODILY_KINAESTHETIC` kullanılsın; bir örnekte kural 7
(“sanırım/galiba”) örneklensin.

### Testin düzeltilmesi

```java
@Test
void containsAllManyShotExamples() {
    var matcher = Pattern.compile("### Örnek \\d+")
            .matcher(FeedbackClassificationPrompt.SYSTEM_PROMPT);
    int count = 0;
    while (matcher.find()) count++;
    assertEquals(18, count);      // eklemeden sonraki sayı
}
```

Metot adı da `FortyOneV4FewShot`'tan kurtulmalı; artık ne 41 ne few-shot.

---

## 8 · Özet

| Konu | Durum |
|---|---|
| Örnek sayısı 41 → 13 | Sayı başlı başına sorun değil |
| Sistem talimatı (§4, 59 satır) | Değişmemiş, v4 ile birebir |
| `situation_hint` · `involvement_hint` | ✅ tam kapsanmış |
| `sensory_hint` | ⚠️ 2/4 |
| `difficulty_hint` · `duration_hint` | ❌ **0 örnek** |
| ZORLANDI butonu | ❌ **0 örnek** |
| Gardner alanı çeşitliliği | ⚠️ target_correction 3/8, BODILY_KINAESTHETIC hiç yok |
| Ö8 ↔ Ö13 tutarlılığı | ❌ **aynı yapı, 0.70 eşiğinin iki yanı** |
| Sarkan referans (rubrik, 0.35) | ⚠️ iki notta |
| `contextLoads` | ✅ **düzelmiş**, 25/25 repository |
| `mvn test` | ❌ kırmızı — tek sebep eski 41 iddiası |
