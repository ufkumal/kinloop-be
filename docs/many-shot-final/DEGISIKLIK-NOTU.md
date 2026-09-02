# Many-shot örnek seti · nihai hâl ve gerekçe

Bu klasör Ufkum'a doğrudan verilecek üç şeyi içeriyor:

```
feedback-classification-many-shot.md   → src/main/resources/prompts/ altına, mevcut dosyanın yerine
DEGISIKLIK-NOTU.md                     → bu dosya, ne değişti ve neden
```

Ayrıca kodda **iki küçük değişiklik** gerekiyor (bölüm 4).

---

## 1 · Özet

| | Önce | Sonra |
|---|---|---|
| Örnek sayısı | 13 | **20** |
| Düzeltilen | — | 1 (eski Ö13) |
| Eklenen | — | 7 |
| Korunan | 12 | 12 (aynen) |

**Değişmeyen:** sistem talimatı (59 satır), çıktı şeması, dosya biçimi, kod akışı.
Yalnız örnek seti değişti.

---

## 2 · Neden 13 yetmiyordu

Kapsam ölçümü yaptım: 13 örneğin ürettiği çıktı alanlarını tek tek saydım.

| Alan | 13'te | Sorun |
|---|---|---|
| `situation_hint` | 1/1 | — |
| `involvement_hint` | 2/2 | — |
| `sensory_hint` | 2/4 | VISUAL ve MOVEMENT hiç yok |
| **`difficulty_hint`** | **0/2** | **Tek örnek yok** |
| **`duration_hint`** | **0/2** | **Tek örnek yok** |
| **ZORLANDI butonu** | **0 örnek** | Üç butondan biri hiç görünmüyor |

`difficulty_hint`'in hiç örneklenmemesi en ciddisiydi, çünkü bu alan **gerçek etki
üretiyor:**

```
llm_difficulty_hint_harder_delta =  0.20   → child_domain_levels.streak
llm_difficulty_hint_easier_delta = -0.20
```

Sayaç ±3.0 / −1.0 eşiklerini geçince çocuğun basamağı değişiyor, basamak da `Z` terimi
üzerinden skorun **45 puanlık** bandını belirliyor. Yani hiç örneklenmemiş bir alan,
öneri motorunun en oynak terimini besliyordu.

### Ve daha kötüsü: `HARDER` / `EASIER` prompt'ta hiç tanımlı değil

Sistem talimatının 8. kuralı bu alanı yalnız **sayıyor** (*“Duyusal, katılım, zorluk,
durum ve süre ipuçları Gardner alanlarından BAĞIMSIZDIR”*). Çıktı şeması da yalnız
değerleri listeliyor. **Hiçbir yerde ne anlama geldikleri yazmıyor.**

Kodda anlamı şu:

```
HARDER = "child needs harder activities"   → streak +0.20 → basamak YÜKSELİR
EASIER = "child needs easier activities"   → streak −0.20 → basamak DÜŞER
```

Etiket, **çocuğun bundan sonra neye ihtiyacı olduğunu** söylüyor; o gün ne yaşandığını
değil. Bu bir tuzak: “ZORLANDI → zor geldi → HARDER” sezgisi **tam tersi**. Ne örnek ne
açıklama varken modelin bunu doğru bilmesi için hiçbir sebep yoktu.

Aynı durum `duration_hint` için de geçerli (LONG/SHORT tanımsız), ama o alan yalnız
kayda gidiyor, puana etkisi yok — o yüzden daha hafif.

---

## 3 · Değişiklikler tek tek

### 3.1 · Düzeltilen: eski Ö13 → yeni Örnek 20

**Sorun:** Eski Ö13 ile Ö8 yapısal olarak aynıydı ama farklı güven alıyordu — hem de
tam karar eşiğinin iki yanında.

| | Eski Ö8 (yeni 13) | Eski Ö13 |
|---|---|---|
| Hedef | VERBAL_LINGUISTIC · Kukla ile sohbet | NATURALISTIC · Kuş gözlemi |
| Metin | Lego'dan bahsediyor | Resimden bahsediyor |
| Hedeften bahsediyor mu | Hayır | **Hayır** |
| Sınıf (kural 4) | DEĞİNMEME | **DEĞİNMEME** |
| Güven | 0.65 | **0.85** ← kural “en fazla 0.65” diyor |

`llm_feedback_confidence_threshold = 0.70` olduğu için biri uygulanmıyor, öteki
uygulanıyor. Model, en sonuç doğuran eşik hakkında birbirini çürüten iki örnek
görüyordu. 13 örneklik sette bu **setin %15'i**.

**Düzeltme:** metni hedefi doğrulayacak hâle getirdim, tek cümle:

```
önce : Hayatının en güzel günüydü, resim yapmayı o kadar çok sevdi ki bir daha
       hiç bırakmak istemiyor
sonra: Kuşları saatlerce izledi, hayatının en güzel günüydü, sonra oturup
       hepsinin resmini yaptı
```

Artık EKLEME (Örnek 2 ile aynı yapı): hedef doğrulanıyor, üçüncü alan ekleniyor,
0.85 tutarlı. **Coşku dersi de korunuyor** — “coşkulu dil deltayı büyütmez”.

### 3.2 · Eklenen 7 örnek

| Yeni # | Konu | Kapattığı boşluk |
|---|---|---|
| **7** | Duyusal · görsel | `sensory_hint = VISUAL` (etkisi var: `visualAdjustment -= 1`) |
| **8** | Duyusal · hareket | `sensory_hint = MOVEMENT` (etkisi var: `movementAdjustment -= 1`) |
| **10** | Zorluk · daha zoru gerekiyor | `HARDER` + etiketin anlamı |
| **11** | Zorluk · daha kolayı gerekiyor | `EASIER` + **ZORLANDI butonu** |
| **12** | Süre ipucu | `duration_hint = LONG` |
| **14** | Belirsizlik ifadesi | Kural 7 (“sanırım/galiba/emin değilim” → ≤ 0.60) |
| **17** | Aynı cümle, “olmadı” butonuyla | v4 §3.5 kenar durumu |

**Örnek 10 ve 11 kasten yan yana.** Biri SEVDI + kolay buldu → `HARDER`, öteki
ZORLANDI + zorlandı → `EASIER`. Karşıtlık çifti, etiketin yönünü tek bakışta
öğretiyor. Notlarda da açıkça yazılı: *“Etiket ihtiyacı gösterir, yaşananı değil.”*

**Örnek 16 ve 17 de kasten yan yana.** Neredeyse aynı cümle, farklı buton:

```
16  SEVDI  + "Aslında hiç ilgilenmedi"  → target_correction dolu, conflict = true
17  OLMADI + "Hiç ilgilenmedi"          → hepsi null,             conflict = false
```

Çünkü “olmadı” oyunda metin butonu **doğruluyor** — çelişki yok, yeni bilgi yok. Kod bu
durumu `sanitizeClassification():214-216`'da zaten temizliyor; örnek, modelin en baştan
doğru üretmesini sağlıyor (boşa token ve kirli `conflict` log'u önlüyor).

**Örnek 6, 7, 8 üçlüsü** duyusal ekseni tamamlıyor. Örnek 8'in notu bir tuzağı da
kapatıyor: hareket yükü fazla gelmesi bir **zorluk** ipucu değil — çocuk beceremediği
için değil, bedensel yük fazla geldiği için durdu.

---

## 4 · Kodda gereken iki değişiklik

### 4.1 · Test düzeltilmeli — şu an build kırmızı

`FeedbackClassificationPromptTest.containsAllFortyOneV4FewShotExamples` hâlâ 41 örnek
bekliyor. Üstelik **13 de bulmuyor, 0 buluyor**: eski `**Ö1**` düzenini arıyor, yeni
dosya `### Örnek 1 —` kullanıyor.

```
expected: <41> but was: <0>
```

Yeni hâli:

```java
@Test
void containsAllManyShotExamples() {
    var matcher = Pattern.compile("### Örnek \\d+")
            .matcher(FeedbackClassificationPrompt.SYSTEM_PROMPT);
    int count = 0;
    while (matcher.find()) count++;
    assertEquals(20, count);
}
```

Metot adı da değişmeli — artık ne 41 ne few-shot.

### 4.2 · Sistem promptundaki sayı

`FeedbackClassificationPrompt.java:91`:

```java
+ "\n\nAŞAĞIDAKİ 20 ÖRNEĞİ SIRASIYLA REFERANS AL:\n\n"
```

(Şu an 13 yazıyor.)

### 4.3 · Öneri: 8. kurala iki satır

Örnekler tek başına yeterli olabilir, ama `HARDER`/`EASIER` ve `LONG`/`SHORT`'un
anlamını **talimatta da** yazmak daha sağlam. 8. kuralın altına:

```
   difficulty_hint çocuğun BUNDAN SONRA neye ihtiyacı olduğunu söyler:
   kolay buldu, çabuk bitirdi -> HARDER      zorlandı, yapamadı -> EASIER
   duration_hint etkinliğin süresine dairdir:
   uzun sürdü, yoruldu -> LONG               kısa geldi, devam istedi -> SHORT
```

Bu, tanımı örneklerden çıkarma yüküyle modeli baş başa bırakmıyor.

---

## 5 · Nihai kapsam

| Alan | Kapsam | Not |
|---|---|---|
| `sensory_hint` | **4/4** | NOISE · VISUAL · MOVEMENT · CROWDING |
| `involvement_hint` | **2/2** | TOGETHER · ALONE |
| `difficulty_hint` | **2/2** | HARDER · EASIER, karşıtlık çiftiyle |
| `situation_hint` | **1/1** | TRANSIENT |
| `duration_hint` | 1/2 | LONG örnekli; SHORT örnek notunda tarif edildi |
| `target_correction` | 3/8 Gardner | — |
| `secondary_hint` | 5/8 Gardner | — |
| Buton | **3/3** | SEVDI 12 · OLMADI 7 · ZORLANDI 1 |
| Güven bandı | eşiğin altı 5, üstü 15 | 0.30 – 0.90 arası dağılmış |

### Bilerek kapatılmayan iki boşluk

**`duration_hint = SHORT`** için ayrı örnek yok. Bu alan yalnız
`feedback_llm_classifications`'a yazılıyor, **puana hiç etki etmiyor** (v4 §7 de böyle
diyor). Örnek 12'nin notu iki yönü de tarif ediyor. Bir örnek daha eklemek 20'yi 21
yapardı, kazancı düşük.

**`BODILY_KINAESTHETIC`** hiçbir çıktıda etiket olarak geçmiyor. Dört örneğin
*bağlamında* var (etkinliğin hedefi ya da ikincili olarak), yalnız model çıktısı olarak
yok. Sekiz Gardner alanı sistem talimatında zaten listeli; sıfır örnekli alanlarla
kıyaslanınca risk düşük. Set büyütülecekse ilk aday bu.

---

## 6 · Kontrol listesi

```
[ ] feedback-classification-many-shot.md  →  src/main/resources/prompts/ (üzerine yaz)
[ ] FeedbackClassificationPrompt.java:91  →  "13 ÖRNEĞİ" yerine "20 ÖRNEĞİ"
[ ] FeedbackClassificationPromptTest      →  regex "### Örnek \d+", assertEquals(20)
[ ] (öneri) sistem talimatı 8. kural      →  HARDER/EASIER ve LONG/SHORT tanımı
[ ] mvn test                              →  yeşil olmalı
```

`contextLoads` bu push'ta zaten düzelmiş (25/25 repository). Yukarıdaki test
düzeltildiğinde **build tamamen yeşile döner** — şu an tek kırmızı o.
