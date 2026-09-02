

### Örnek 1 — Teyit (yeni bilgi yok)
**User:**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Kukla ile uzun uzun konuştu, hikâyeyi baştan sona kendi kelimeleriyle anlattı
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.9}
```
> Not: Metin, etkinliğin zaten hedeflediği alanı doğruluyor. Buton işini yaptı; ek sinyal yok.

### Örnek 2 — Ekleme (hedef yaşandı + üçüncü alan)
**User:**
```
Etkinlik: Yaprak dokunma tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yaprakları eline aldı ama asıl uzun uzun konuştu, her yaprağa bir isim taktı, hikâye uydurdu
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": "VERBAL_LINGUISTIC", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Not: "eline aldı" hedefi doğruluyor; "ama asıl" üçüncü bir alan ekliyor. `target_correction` null kalır.

### Örnek 3 — Yalanlama (hedef yaşanmadı)
**User:**
```
Etkinlik: Duygu hikâyeleri kahraman ne hissetti
Hedef zekâ: INTERPERSONAL
İkincil zekâ: VERBAL_LINGUISTIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Duygu hikâyesine hiç katılmadı, kenarda durdu, tek başına oturup kendi kendine düşündü
```
**Assistant:**
```json
{"target_correction": "INTERPERSONAL", "secondary_hint": "INTRAPERSONAL", "sensory_hint": null, "involvement_hint": "ALONE", "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.85}
```
> Not: "hiç katılmadı" hedefi yalanlıyor → `target_correction` **yaşanmayan** alanı taşır; yaşanan alan `secondary_hint`'e gider.

### Örnek 4 — Karşıtlık çifti · A (aynı etkinlik, yalanlama)
**User:**
```
Etkinlik: Yaprak dokunma tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yaprakları eline bile almadı, sadece şarkı söyledi, tekerleme uydurdu
```
**Assistant:**
```json
{"target_correction": "NATURALISTIC", "secondary_hint": "MUSICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.85}
```
> Not: Örnek 2 ile 4 yan yana okunmalı. Tek fark: hedefin yaşanıp yaşanmadığı. Burada doğa yaşanmadı → kredi yok, `conflict = true`.

### Örnek 5 — Çoklu ipucu (duyusal + katılım, aynı anda)
**User:**
```
Etkinlik: Duygu hikâyeleri kahraman ne hissetti
Hedef zekâ: INTERPERSONAL
İkincil zekâ: VERBAL_LINGUISTIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Hikâyeyi sevdi ama kalabalıktı, hep kucağıma geldi
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": "CROWDING", "involvement_hint": "TOGETHER", "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.8}
```
> Not: Gardner sinyali yok (metin hedefi doğruluyor). İki bağımsız ipucu: kalabalık → `CROWDING`, kucağa gelme → `TOGETHER`.

### Örnek 6 — Duyusal ipucu · gürültü
**User:**
```
Etkinlik: Yumuşak çıngırak ritmi
Hedef zekâ: MUSICAL
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Ortam çok gürültülüydü, kulaklarını kapattı ve köşeye çekildi
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": "NOISE", "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Not: İlgi değil, ortam sinyali. Gardner'a dokunulmaz.

### Örnek 7 — Duyusal ipucu · görsel
**User:**
```
Etkinlik: Renkli ışık şişesi takibi
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Işık çok parlaktı, gözlerini kıstı, yüzünü çevirdi, bakmak istemedi
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": "VISUAL", "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Not: Parlaklık, göz kısma, yüz çevirme → görsel eksen. Çocuk görsel alandan hoşlanmıyor demek değil; o ortam o gün fazla geldi.

### Örnek 8 — Duyusal ipucu · hareket
**User:**
```
Etkinlik: Yastık dağı parkuru
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Zıplama tırmanma çok geldi, birkaç adım sonra durdu, oturmak istedi
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": "MOVEMENT", "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Not: Hareket yükü fazla geldi → `MOVEMENT`. Bu bir zorluk ipucu **değil**: çocuk beceremediği için değil, bedensel yük fazla geldiği için durdu.

### Örnek 9 — Durumsal sebep (puan güncellemesi yok)
**User:**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Uykusuzdu bugün, çok huysuzdu, o yüzden hiçbir şey yapamadık
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": "TRANSIENT", "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Not: İlgi hakkında bilgi taşımıyor. Kod hiçbir puan güncellemesi yapmaz.

### Örnek 10 — Zorluk ipucu · daha zoru gerekiyor
**User:**
```
Etkinlik: Büyük parça yapboz
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Bir dakikada bitirdi, çok kolay geldi, daha zoru var mı diye sordu
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": "HARDER", "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.9}
```
> Not: Etiket **çocuğun sonra neye ihtiyacı olduğunu** söyler, o gün ne yaşandığını değil. Çocuk kolay buldu → bir sonraki öneri daha zor olmalı → `HARDER`. Metin hedefi doğruluyor, Gardner alanları null kalır.

### Örnek 11 — Zorluk ipucu · daha kolayı gerekiyor
**User:**
```
Etkinlik: Kurallı kutu oyunu
Hedef zekâ: INTERPERSONAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: ZORLANDI
Ebeveynin yazdığı: Kuralları bir türlü aklında tutamadı, üç kere baştan anlattım, sonunda bıraktı
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": "EASIER", "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.9}
```
> Not: Örnek 10 ile 11 yan yana okunmalı. Çocuk zorlandı → bir sonraki öneri daha kolay olmalı → `EASIER`. Etiket ihtiyacı gösterir, yaşananı değil.

### Örnek 12 — Süre ipucu
**User:**
```
Etkinlik: Birlikte kukla gösterisi hazırlama
Hedef zekâ: INTERPERSONAL
İkincil zekâ: VERBAL_LINGUISTIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Güzeldi ama çok uzun sürdü, sonuna doğru ikimiz de yorulduk
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": "LONG", "conflict": false, "confidence": 0.85}
```
> Not: Süre yorumu ilgi ya da zorluk sinyali değildir. "Uzun sürdü" → `LONG`, "çok kısaydı, devam etmek istedi" → `SHORT`.

### Örnek 13 — Değinmeme (güven ≤ 0.65)
**User:**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Legolarla evi tek başına kurdu, hangi parçanın nereye gideceğini uzun uzun düşündü
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": "LOGICAL_MATHEMATICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.65}
```
> Not: Metin kukladan hiç bahsetmiyor. Hedefin yaşanıp yaşanmadığı bilinmiyor; belirsizlik güven tavanıyla bildiriliyor (eşiğin altı → uygulanmaz).

### Örnek 14 — Belirsizlik ifadesi (güven ≤ 0.60)
**User:**
```
Etkinlik: Sayarak hareket dizisi
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Sanırım hoşuna gitti, galiba sayıları da söylüyordu ama emin değilim
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.55}
```
> Not: "sanırım", "galiba", "emin değilim" → ebeveyn kendi gözleminden emin değil. Belirsiz gözlemden alan çıkarılmaz; güven 0.60'ı geçemez.

### Örnek 15 — İkinci elden aktarım (güven ≤ 0.60)
**User:**
```
Etkinlik: Yastık dağı parkuru
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Babası yaptırmış, çok beğenmiş diyor, ben görmedim
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.5}
```
> Not: Fren 6 — ikinci elden aktarım güveni 0.60'ı geçemez.

### Örnek 16 — Çelişki (buton ↔ metin)
**User:**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Aslında hiç ilgilenmedi, ben uğraştım durdum, bir daha yapmayız herhalde
```
**Assistant:**
```json
{"target_correction": "VERBAL_LINGUISTIC", "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.8}
```
> Not: Buton "sevdi" ama metin tersini söylüyor. `target_correction` yaşanmayan alanı taşır; kredi/ceza yok, buton geçerli kalır.

### Örnek 17 — Aynı cümle, "olmadı" butonuyla (çelişki yok)
**User:**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Hiç ilgilenmedi, eline bile almadı
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.9}
```
> Not: Örnek 16 ile 17 yan yana okunmalı. Aynı cümle, farklı buton. "Olmadı" oyunda metin butonu **doğruluyor** — çelişki yok, yeni bilgi yok, `target_correction` null kalır.

### Örnek 18 — Olumsuz duygusal tepki ("söyledi" tuzağı)
**User:**
```
Etkinlik: Yaprak dokunma tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Vakit geçirdi ama yaprakları parçaladı, sinirlendi, nefret dolu şeyler söyledi
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.55}
```
> Not: Öfke bir Gardner sinyali değildir. "Söyledi" fiiline takılıp `VERBAL_LINGUISTIC` yazmak ciddi hata olur.

### Örnek 19 — Sağlık iması (düşük güven, sinyal yok)
**User:**
```
Etkinlik: İsmini söyleme oyunu
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Hiç konuşmuyor zaten, doktor da gelişim geriliği olabilir dedi
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.3}
```
> Not: Sağlık/gelişim kaygısı içeren metinden Gardner sinyali çıkarılmaz; bütün alanlar null, güven 0.40'ın altında.

### Örnek 20 — Coşkulu ifade (delta tavanı)
**User:**
```
Etkinlik: Pencereden kuş gözlemi
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Kuşları saatlerce izledi, hayatının en güzel günüydü, sonra oturup hepsinin resmini yaptı
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": "VISUAL_SPATIAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Not: "Kuşları izledi" hedefi doğruluyor, "resmini yaptı" üçüncü alanı ekliyor → Örnek 2 ile aynı yapı. Coşkulu dil deltayı büyütmez (Fren 1); kod yine sabit `+0.15` uygular.
