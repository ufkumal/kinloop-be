
Her örnek bir kullanıcı mesajı ve beklenen JSON çıktısıdır. Prompt'a bu sırayla konur.

Kısaltma için çıktılarda `null` alanlar gösterilmiştir; modelin **her zaman dokuz alanın hepsini** döndürmesi beklenir.

### Grup A · Sekiz Gardner alanı, temiz olumlu sinyal

Bu örneklerde etkinliğin hedef zekâsı **farklı**; metin başka bir alana işaret ediyor. `target_correction` bu yüzden dolu.

**Ö1**
```
Etkinlik: Yaprak tepsisi
Hedef zekâ: NATURALISTIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Yaprakları eline aldı ama asıl uzun uzun konuştu, her yaprağa
bir isim taktı, hikâye uydurdu
```
```json
{"target_correction":"VERBAL_LINGUISTIC","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```

**Ö2**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Legolarla evi tek başına kurdu, hangi parçanın nereye gideceğini
uzun uzun düşündü
```
```json
{"target_correction":"LOGICAL_MATHEMATICAL","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```

**Ö3**
```
Etkinlik: Renk gruplama tepsisi
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Müzik açınca bıraktı her şeyi, şarkıya eşlik etti, ritim tuttu
```
```json
{"target_correction":"MUSICAL","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```

**Ö4**
```
Etkinlik: Sessiz taş sepeti
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Sallanan atın üzerinde saatlerce zıplayabilirdi, hiç durmak istemedi
```
```json
{"target_correction":"BODILY_KINAESTHETIC","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.80}
```

**Ö5**
```
Etkinlik: Blok kopyalama
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Ablasıyla sırayla oynamayı öğrendi, kendi sırasını beklerken
gülümsüyordu
```
```json
{"target_correction":"INTERPERSONAL","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```

**Ö6**
```
Etkinlik: Yastık adası atlama
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Bahçedeki böcek gözlemini hiç bırakmak istemedi, taşları
kaldırıp altına baktı
```
```json
{"target_correction":"NATURALISTIC","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```

**Ö7**
```
Etkinlik: Sıra sende sesler
Hedef zekâ: INTERPERSONAL
İkincil zekâ: VERBAL_LINGUISTIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Grup oyunlarında hep kenarda kaldı, tek başına yapılan kısımda
mutluydu
```
```json
{"target_correction":"INTRAPERSONAL","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":"ALONE","difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.75}
```

**Ö8**
```
Etkinlik: Kapak eşleştirme
Hedef zekâ: LOGICAL_MATHEMATICAL
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Renkleri dizerken çok dikkatliydi, hangisinin nereye gideceğini
gözüyle buluyordu
```
```json
{"target_correction":"VISUAL_SPATIAL","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.80}
```

### Grup B · Metin butonu tekrar ediyor, yeni bilgi yok

**Ö9**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Kukla ile uzun uzun konuştu, hikâyeyi baştan sona kendi
kelimeleriyle anlattı
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.90}
```
> Metin etkinliğin zaten hedeflediği alanı doğruluyor. Buton `+0.30`'u uyguladı; tekrar uygulanmaz.

**Ö10**
```
Etkinlik: Sessiz el ritmi
Hedef zekâ: MUSICAL
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Ritim tutmayı çok sevdi, bırakmak istemedi
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.90}
```

### Grup C · İki alana işaret

**Ö11**
```
Etkinlik: Sessiz taş sepeti
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Müzik başlayınca hem şarkı söyledi hem dans etti, ikisini bir
arada yapmaktan çok keyif aldı
```
```json
{"target_correction":"MUSICAL","secondary_hint":"BODILY_KINAESTHETIC","sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.75}
```
> Baskın alan `target_correction`, ikincisi `secondary_hint`. Kod birine `+0.30`, diğerine `+0.15` uygular.

**Ö12**
```
Etkinlik: Fotoğraf albümü turu
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Seramiği çok sevdi ama kalabalıktı, hep kucağıma geldi
```
```json
{"target_correction":"VISUAL_SPATIAL","secondary_hint":null,"sensory_hint":"CROWDING",
 "involvement_hint":"TOGETHER","difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.80}
```
> Gardner sinyali ve iki ayrı ipucu aynı metinde. Üçü de birbirinden bağımsız.

### Grup D · Duyusal ipucu, Gardner sinyali yok

**Ö13**
```
Etkinlik: Sessiz orkestra şefi
Hedef zekâ: MUSICAL
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Ortam çok gürültülüydü, kulaklarını kapattı ve köşeye çekildi
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":"NOISE",
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```
> Bu bir ilgi sinyali değil, ortam sinyali. Gardner'a dokunulmaz.

**Ö14**
```
Etkinlik: Sessiz gölge oyunu
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Işık gözüne geldi galiba, rahatsız oldu ve bakmak istemedi
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":"VISUAL",
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.75}
```

### Grup E · Katılım ipucu

**Ö15**
```
Etkinlik: Bitki sulama görevi
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Mutfağa gittim bir dakika, hemen ağlamaya başladı, oyunu bıraktı
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":"TOGETHER","difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```

**Ö16**
```
Etkinlik: Birlikte kolaj yapma
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Hep yalnız kalmak istedi, yanına gelmemi istemedi, kendi başına
daha rahattı
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":"ALONE","difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.75}
```

### Grup F · Zorluk ipucu

**Ö17**
```
Etkinlik: Sessiz örüntü tamamlama
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: ZORLANDI
Ebeveynin yazdığı: Çok karışık geldi, ne yapacağını anlamadı, ben gösterdim yine olmadı
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":"HARDER","situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```

**Ö18**
```
Etkinlik: İki nesne arasında seçim
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Çok kolay geldi ona, hemen bitirdi ve başka bir şey istedi
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":"EASIER","situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```

### Grup G · Durumsal sebep, puan güncellemesi yok

**Ö19**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Uykusuzdu bugün, çok huysuzdu, o yüzden hiçbir şey yapamadık
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":"TRANSIENT","duration_hint":null,
 "conflict":false,"confidence":0.85}
```
> Bu, ilgi hakkında bilgi taşımıyor. Kod hiçbir puan güncellemesi yapmaz.

**Ö20**
```
Etkinlik: Yaprak boyut sıralaması
Hedef zekâ: NATURALISTIC
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Hastaydı, ateşi vardı, hiç oyun oynayacak halde değildi
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":"TRANSIENT","duration_hint":null,
 "conflict":false,"confidence":0.90}
```

### Grup H · Süre ipucu

**Ö21**
```
Etkinlik: Doğa masası kurma
Hedef zekâ: NATURALISTIC
İkincil zekâ: VERBAL_LINGUISTIC
Ebeveynin işareti: ZORLANDI
Ebeveynin yazdığı: Çok uzun sürdü, ortasında sıkıldı ve kalktı gitti
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":"LONG",
 "conflict":false,"confidence":0.80}
```

**Ö22**
```
Etkinlik: Sessiz ritim
Hedef zekâ: MUSICAL
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Çok çabuk bitti, daha uzun olsaydı iyiydi, devamını istedi
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":"SHORT",
 "conflict":false,"confidence":0.80}
```

### Grup I · Düşük güven

**Ö23**
```
Etkinlik: Renk gruplama tepsisi
Hedef zekâ: VISUAL_SPATIAL
İkincil zekâ: LOGICAL_MATHEMATICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Resim yaparken çok dikkatliydi ama ilk kez denedik, henüz sevdi
mi emin değilim
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.55}
```
> Ebeveyn belirsizliğini kendisi söylüyor. Güven eşiğin altında, hiçbir alan uygulanmaz.

**Ö24**
```
Etkinlik: Yastık adası atlama
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Babası yaptırmış, çok beğenmiş diyor, ben görmedim
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.50}
```
> İkinci elden aktarım. Fren 6 gereği güven 0.60'ı geçemez.

**Ö25**
```
Etkinlik: Kapak eşleştirme
Hedef zekâ: LOGICAL_MATHEMATICAL
İkincil zekâ: BODILY_KINAESTHETIC
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Fena değildi
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.40}
```

### Grup J · Kısa ve özensiz girdi

**Ö26**
```
Etkinlik: Sessiz taş sepeti
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: iyiydi
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.50}
```

**Ö27**
```
Etkinlik: Çizgi üstünde yürüme
Hedef zekâ: BODILY_KINAESTHETIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: cok sevdi kosturdu durdu 😊
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.75}
```
> Kısa ve özensiz ama metin butonu doğruluyor, yeni alan yok.

**Ö28**
```
Etkinlik: Fotoğraf albümü turu
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: VISUAL_SPATIAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: yapamadık
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.35}
```

**Ö29**
```
Etkinlik: Yavaş sallanma ritmi
Hedef zekâ: INTRAPERSONAL
İkincil zekâ: MUSICAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: sarkiyi cok sevdi mirildandi durdu
```
```json
{"target_correction":"MUSICAL","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.70}
```

### Grup K · Çelişki

**Ö30**
```
Etkinlik: Kukla ile sohbet
Hedef zekâ: VERBAL_LINGUISTIC
İkincil zekâ: INTERPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Aslında hiç ilgilenmedi, ben uğraştım durdum, bir daha
yapmayız herhalde
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":true,"confidence":0.80}
```
> Buton "sevdi" ama metin tersini söylüyor. Fren 2 gereği buton geçerli; çelişki log'a düşer.

### Grup L · Malzeme ve uygulanabilirlik

**Ö31**
```
Etkinlik: Koku kavanozları
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: OLMADI
Ebeveynin yazdığı: Evde tarçın yoktu, malzeme bulamadık yapamadık
```
```json
{"target_correction":null,"secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":"TRANSIENT","duration_hint":null,
 "conflict":false,"confidence":0.85}
```
> Etkinlikle ya da çocukla ilgili değil, uygulanabilirlik sorunu. İlgi puanına dokunulmaz.

### Grup M · Coşkulu ifade, delta tavanı

**Ö32**
```
Etkinlik: Sessiz taş sepeti
Hedef zekâ: NATURALISTIC
İkincil zekâ: INTRAPERSONAL
Ebeveynin işareti: SEVDI
Ebeveynin yazdığı: Hayatının en güzel günüydü, resim yapmayı o kadar çok sevdi ki
bir daha hiç bırakmak istemiyor
```
```json
{"target_correction":"VISUAL_SPATIAL","secondary_hint":null,"sensory_hint":null,
 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,"duration_hint":null,
 "conflict":false,"confidence":0.85}
```
> Coşku deltayı büyütmez. Fren 1 gereği kod yine `+0.30` uygular.

---

