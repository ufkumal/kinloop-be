Her örnek bir kullanıcı mesajı ve beklenen JSON çıktısıdır. Prompt'a bu sırayla konur.

**v4.0'da yeniden yazıldı.** Eski 32 örnek saptırma semantiğiyle etiketlenmişti (`target_correction` = gidilecek alan); yeni semantikte `target_correction` = yaşanmayan alan. Ayrıca **Grup A3 (değinmeme)** eklendi ve örnek sayısı 41'e çıktı.

Bütün etkinlikler **gerçek havuzdan** alınmıştır; `id`, başlık, hedef ve ikincil zekâ değerleri veritabanı kayıtlarıyla birebir aynıdır (kaynak: `V9`, `V10`, `V18`).


Her örnek bir kullanıcı mesajı ve beklenen JSON çıktısıdır. Prompt'a bu sırayla konur.

Bütün etkinlikler **gerçek havuzdan** alınmıştır; `id`, başlık, hedef ve ikincil zekâ değerleri veritabanı kayıtlarıyla birebir aynıdır.

### Grup A1 · YALANLAMA · hedef zekâ yaşanmadı

Metin, etkinliğin hedef zekâsının **yaşanmadığını açıkça söylüyor.** `target_correction` o alanı taşır; kod oraya kredi vermez, ceza da vermez. Buton geçerli kalır, `conflict = true` işaretlenir.

**Ö1**

```
Etkinlik: Duygu hikâyeleri kahraman ne hissetti
Hedef zekâ: INTERPERSONAL
İkincil zekâ: VERBAL_LINGUISTIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Duygu hikâyesine hiç katılmadı, kenarda durdu, tek başına oturup kendi kendine düşündü
```
```json
{"target_correction": "INTERPERSONAL", "secondary_hint": "INTRAPERSONAL", "sensory_hint": null, "involvement_hint": "ALONE", "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.85}
```
> "hiç katılmadı" hedefi yalanlıyor → `target_correction` **yaşanmayan** alanı taşır. Yaşanan alan `secondary_hint`'e gider.

**Ö2**

```
Etkinlik: Renk eşleştirme kartları
Hedef zekâ: LOGICAL_MATHEMATICAL
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Kartlara hiç bakmadı, müzik açılınca her şeyi bıraktı, şarkıya eşlik etti
```
```json
{"target_correction": "VISUAL_SPATIAL", "secondary_hint": "MUSICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.85}
```

**Ö3**

```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Kuklayı eline bile almadı, blokları dizdi, hangi parça nereye gidecek uzun uzun düşündü
```
```json
{"target_correction": "VERBAL_LINGUISTIC", "secondary_hint": "LOGICAL_MATHEMATICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.85}
```

**Ö4**

```
Etkinlik: Yastık dağı parkuru
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yastıklara hiç tırmanmadı, pencereden kuşları izledi, hangisi geldi gitti anlattı
```
```json
{"target_correction": "BODILY_KINAESTHETIC", "secondary_hint": "NATURALISTIC", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.85}
```

### Grup A2 · EKLEME · hedef yaşandı, üstüne başka alan

Metin hedefin yaşandığını **doğruluyor** ve üstüne başka bir alan ekliyor. `target_correction` **null kalır**. Buton hedefe `+0.30` ve etkinliğin ikincilinde `+0.15` uygular; `secondary_hint` üçüncü alana `+0.15` ekler.

**Ö5**

```
Etkinlik: Yaprak dokunma tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yaprakları eline aldı ama asıl uzun uzun konuştu, her yaprağa bir isim taktı, hikâye uydurdu
```
```json
{"target_correction": null, "secondary_hint": "VERBAL_LINGUISTIC", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> "eline aldı" hedefi doğruluyor, "ama asıl" üçüncü bir alan ekliyor. NATURALISTIC `+0.30` (buton), etkinliğin ikincili `+0.15`, VERBAL_LINGUISTIC `+0.15` (LLM). Üç alan.

**Ö6**

```
Etkinlik: Renk eşleştirme kartları
Hedef zekâ: LOGICAL_MATHEMATICAL
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Eşleştirmeyi yaptı ama asıl renkleri dizerken keyif aldı, hangisi nereye gidecek gözüyle buluyordu
```
```json
{"target_correction": null, "secondary_hint": "VISUAL_SPATIAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.8}
```

**Ö7**

```
Etkinlik: Büyük parça yapboz
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yapbozu tamamladı, arada ablasıyla sırayla oynamayı da öğrendi, sırasını beklerken gülümsüyordu
```
```json
{"target_correction": null, "secondary_hint": "INTERPERSONAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.8}
```

**Ö8**

```
Etkinlik: Pencereden kuş gözlemi
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Kuşlara baktı ama asıl sallanmaktan hoşlandı, koltukta saatlerce zıplayabilirdi
```
```json
{"target_correction": null, "secondary_hint": "BODILY_KINAESTHETIC", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.8}
```

**Ö9**

```
Etkinlik: Hayvan yürüyüşleri parkuru
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: NATURALISTIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Parkuru yaptı, sonra kendi kendine konuşup düşündü, çok sakin ve dalgındı
```
```json
{"target_correction": null, "secondary_hint": "INTRAPERSONAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.75}
```

### Grup A3 · DEĞİNMEME · hedeften hiç bahsetmiyor

Metin hedef zekâdan hiç söz etmiyor, tamamen başka bir alandan bahsediyor. Hedefin yaşanıp yaşanmadığı belirsiz. `secondary_hint` doldurulur ama **`confidence` en fazla 0.65** olur; yani güven eşiğinin altında kalır ve uygulanmaz.

**Ö10**

```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Legolarla evi tek başına kurdu, hangi parçanın nereye gideceğini uzun uzun düşündü
```
```json
{"target_correction": null, "secondary_hint": "LOGICAL_MATHEMATICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.65}
```
> Metin kukladan hiç bahsetmiyor. Hedefin yaşanıp yaşanmadığı **bilinmiyor**; belirsizlik `confidence` ile bildiriliyor. Eşiğin altında olduğu için uygulanmaz.

**Ö11**

```
Etkinlik: Yastık dağı parkuru
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Pencereden kuşları izledi, hangisi geldi gitti anlattı, çok konuştu
```
```json
{"target_correction": null, "secondary_hint": "VERBAL_LINGUISTIC", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.65}
```

### Grup A4 · KARŞITLIK · aynı etkinlik, iki farklı cümle

Bu ikisi yan yana okunmalı. Aynı etkinlik, aynı buton, aynı ek alan; tek fark hedefin yaşanıp yaşanmadığı.

**Ö12**

```
Etkinlik: Yaprak dokunma tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yaprakları eline bile almadı, sadece şarkı söyledi, tekerleme uydurdu
```
```json
{"target_correction": "NATURALISTIC", "secondary_hint": "MUSICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.85}
```
> **Yalanlama.** Doğa yaşanmadı → kredi verilmez. Müzik yaşandı → `+0.15`.

**Ö13**

```
Etkinlik: Yaprak dokunma tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yaprakları eline aldı ama asıl şarkı söyledi, her yaprağa bir tekerleme uydurdu
```
```json
{"target_correction": null, "secondary_hint": "MUSICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.8}
```
> **Ekleme.** İkisi de yaşandı. Doğa `+0.30`, etkinliğin ikincili `+0.15`, müzik `+0.15`.

### Grup B · TEKRAR · metin butonu doğruluyor

**Ö14**

```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Kukla ile uzun uzun konuştu, hikâyeyi baştan sona kendi kelimeleriyle anlattı
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.9}
```
> Metin etkinliğin zaten hedeflediği alanı doğruluyor. Buton işini yaptı; yeni bilgi yok.

**Ö15**

```
Etkinlik: El çırpma şarkısı fısıltı modu
Hedef zekâ: MUSICAL
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Ritim tutmayı çok sevdi, bırakmak istemedi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.9}
```

### Grup C · ÇOKLU SİNYAL · Gardner + ipucu

**Ö16**

```
Etkinlik: Duygu hikâyeleri kahraman ne hissetti
Hedef zekâ: INTERPERSONAL
İkincil zekâ: VERBAL_LINGUISTIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Hikâyeyi sevdi ama kalabalıktı, hep kucağıma geldi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": "CROWDING", "involvement_hint": "TOGETHER", "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.8}
```
> Gardner sinyali yok (metin hedefi doğruluyor), iki ayrı ipucu var. Üçü de birbirinden bağımsız.

**Ö17**

```
Etkinlik: Odamın haritasını çiziyorum
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Haritayı çizdi, bir yandan da şarkı mırıldandı, ritim tutuyordu
```
```json
{"target_correction": null, "secondary_hint": "MUSICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.75}
```

### Grup D · DUYUSAL İPUCU

**Ö18**

```
Etkinlik: Yumuşak çıngırak ritmi
Hedef zekâ: MUSICAL
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Ortam çok gürültülüydü, kulaklarını kapattı ve köşeye çekildi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": "NOISE", "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Bu bir ilgi sinyali değil, ortam sinyali. Gardner'a dokunulmaz.

**Ö19**

```
Etkinlik: Sessiz oda mobil izleme
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Işık gözüne geldi galiba, rahatsız oldu ve bakmak istemedi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": "VISUAL", "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.75}
```

**Ö20**

```
Etkinlik: Hayvan yürüyüşleri parkuru
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: NATURALISTIC
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Çok hareketliydi, koşturmaca ona fazla geldi, yoruldu ve oturdu
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": "MOVEMENT", "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.75}
```

### Grup E · KATILIM İPUCU

**Ö21**

```
Etkinlik: Bitki sulama görevi
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Mutfağa gittim bir dakika, hemen ağlamaya başladı, oyunu bıraktı
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": "TOGETHER", "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```

**Ö22**

```
Etkinlik: Duygu günlüğü çizimi
Hedef zekâ: INTRAPERSONAL
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Hep yalnız kalmak istedi, yanına gelmemi istemedi, kendi başına daha rahattı
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": "ALONE", "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.75}
```

### Grup F · ZORLUK İPUCU

**Ö23**

```
Etkinlik: Üç taş strateji oyunu
Hedef zekâ: LOGICAL_MATHEMATICAL
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: ZORLANDI
Ebeveynin yazdığı: Çok karışık geldi, ne yapacağını anlamadı, ben gösterdim yine olmadı
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": "HARDER", "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```

**Ö24**

```
Etkinlik: İsmini söyleme oyunu
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Çok kolay geldi ona, hemen bitirdi ve başka bir şey istedi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": "EASIER", "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```

### Grup G · DURUMSAL SEBEP

**Ö25**

```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Uykusuzdu bugün, çok huysuzdu, o yüzden hiçbir şey yapamadık
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": "TRANSIENT", "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Bu, ilgi hakkında bilgi taşımıyor. Kod **hiçbir puan güncellemesi yapmaz.**

**Ö26**

```
Etkinlik: Su deneyi yüzer mi batar mı
Hedef zekâ: LOGICAL_MATHEMATICAL
İkincil zekâ: NATURALISTIC
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Hastaydı, ateşi vardı, hiç oyun oynayacak halde değildi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": "TRANSIENT", "duration_hint": null, "conflict": false, "confidence": 0.9}
```

**Ö27**

```
Etkinlik: Yaprak dokunma tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Yaprak bulamadık, mevsim değil, malzeme olmadığı için yapamadık
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": "TRANSIENT", "duration_hint": null, "conflict": false, "confidence": 0.85}
```

### Grup H · SÜRE İPUCU

**Ö28**

```
Etkinlik: Odamın haritasını çiziyorum
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: ZORLANDI
Ebeveynin yazdığı: Çok uzun sürdü, ortasında sıkıldı ve kalktı gitti
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": "LONG", "conflict": false, "confidence": 0.8}
```

**Ö29**

```
Etkinlik: Ritmik alkış taklidi
Hedef zekâ: MUSICAL
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Çok çabuk bitti, daha uzun olsaydı iyiydi, devamını istedi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": "SHORT", "conflict": false, "confidence": 0.8}
```

### Grup I · DÜŞÜK GÜVEN

**Ö30**

```
Etkinlik: Büyük parça yapboz
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yapbozu yaparken çok dikkatliydi ama ilk kez denedik, henüz sevdi mi emin değilim
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.55}
```
> Ebeveyn belirsizliğini kendisi söylüyor. Güven eşiğin altında, hiçbir alan uygulanmaz.

**Ö31**

```
Etkinlik: Yastık dağı parkuru
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Babası yaptırmış, çok beğenmiş diyor, ben görmedim
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.5}
```
> İkinci elden aktarım. Fren 6 gereği güven 0.60'ı geçemez.

**Ö32**

```
Etkinlik: Renk eşleştirme kartları
Hedef zekâ: LOGICAL_MATHEMATICAL
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Fena değildi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.4}
```

### Grup J · KISA VE ÖZENSİZ GİRDİ

**Ö33**

```
Etkinlik: Pencereden kuş gözlemi
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: iyiydi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.5}
```

**Ö34**

```
Etkinlik: Hayvan yürüyüşleri parkuru
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: NATURALISTIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: cok sevdi kosturdu durdu
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.75}
```

**Ö35**

```
Etkinlik: Resimli kitapla bu ne oyunu
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: yapamadik
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.35}
```

**Ö36**

```
Etkinlik: Sakin battaniye kucak dinlenmesi
Hedef zekâ: INTRAPERSONAL
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: sarkiyi cok sevdi mirildandi durdu
```
```json
{"target_correction": null, "secondary_hint": "MUSICAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.7}
```

### Grup K · ÇELİŞKİ

**Ö37**

```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Aslında hiç ilgilenmedi, ben uğraştım durdum, bir daha yapmayız herhalde
```
```json
{"target_correction": "VERBAL_LINGUISTIC", "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": true, "confidence": 0.8}
```
> Buton "sevdi" ama metin tersini söylüyor. `target_correction` yaşanmayan alanı taşır; kredi verilmez, ceza da verilmez. Buton geçerli kalır.

### Grup L · OLUMSUZ DUYGUSAL TEPKİ

**Ö38**

```
Etkinlik: Yaprak dokunma tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Vakit geçirdi ama yaprakları parçaladı, sinirlendi, nefret dolu şeyler söyledi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.55}
```
> Öfke bir Gardner sinyali değildir. "Söyledi" fiiline takılıp `VERBAL_LINGUISTIC` yazmak ciddi bir hata olur.

**Ö39**

```
Etkinlik: Büyük parça yapboz
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: ZORLANDI
Ebeveynin yazdığı: Parçaları fırlattı, ağladı, çok öfkelendi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.55}
```

### Grup M · SAĞLIK İMA EDEN METİN

**Ö40**

```
Etkinlik: İsmini söyleme oyunu
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Hiç konuşmuyor zaten, doktor da gelişim geriliği olabilir dedi
```
```json
{"target_correction": null, "secondary_hint": null, "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.3}
```

### Grup N · COŞKULU İFADE · delta tavanı

**Ö41**

```
Etkinlik: Pencereden kuş gözlemi
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Hayatının en güzel günüydü, resim yapmayı o kadar çok sevdi ki bir daha hiç bırakmak istemiyor
```
```json
{"target_correction": null, "secondary_hint": "VISUAL_SPATIAL", "sensory_hint": null, "involvement_hint": null, "difficulty_hint": null, "situation_hint": null, "duration_hint": null, "conflict": false, "confidence": 0.85}
```
> Coşku deltayı büyütmez. Fren 1 gereği kod yine `+0.15` uygular.
