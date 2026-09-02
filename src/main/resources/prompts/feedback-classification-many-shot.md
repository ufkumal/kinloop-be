

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
> Not: Örnek 4 ile 5 yan yana okunmalı. Tek fark: hedefin yaşanıp yaşanmadığı. Burada doğa yaşanmadı → kredi yok, `conflict = true`.

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

### Örnek 6 — Duyusal ipucu (gürültü)
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

### Örnek 7 — Durumsal sebep (puan güncellemesi yok)
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

### Örnek 8 — Değinmeme (güven ≤ 0.65)
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

### Örnek 9 — İkinci elden aktarım (güven ≤ 0.60)
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

### Örnek 10 — Çelişki (buton ↔ metin)
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

### Örnek 11 — Olumsuz duygusal tepki ("söyledi" tuzağı)
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

### Örnek 12 — Sağlık iması (düşük güven, sinyal yok)
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
> Not: Sağlık/gelişim kaygısı içeren metinden Gardner sinyali çıkarılmaz; bütün alanlar null, güven ≤ 0.35.

### Örnek 13 — Coşkulu ifade (delta tavanı)
**User:**
```
Etkinlik: Pencereden kuş gözlemi
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Hayatının en güzel günüydü, resim yapmayı o kadar çok sevdi ki bir daha hiç bırakmak istemiyor
```
**Assistant:**
```json
{"target_correction": null, "secondary_hint": "VISUAL_SPATIAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Not: Coşku deltayı büyütmez (Fren 1). Kod yine sabit `+0.15` uygular; güven rubriğin açık-ifade bandında kalır.

