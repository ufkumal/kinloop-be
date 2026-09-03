# Bitirme Tezi Sunumu — Değerlendirme

Kaynak: `Bitirme_Tezi_Sunumu_Plan_.pptx` (21 slayt)
Kod referansı: `ufkumal/kinloop-be` @ `46d3e5b`
Tarih: 3 Eylül 2026

Bu belgedeki her sayısal iddia bugün kod üzerinde ölçüldü. Ölçülemeyen hiçbir şey
yazılmadı; bulunamayan yerlere "bulunamadı" yazıldı.

---

## 1. Kapsam: sunum şu an ne kadar kapsıyor

Mevcut 21 slaytın omurgası sağlam ve akış tutarlı. Kidloop'un **ürün ve
mühendislik** tarafı neredeyse eksiksiz anlatılıyor:

| Alan | Durum |
|---|---|
| Problem tanımı | Var (s.2) |
| Çözüm önerisi | Var (s.3) |
| Pedagojik temel (5 kuram) | Var, güçlü (s.4–6) |
| Onboarding → motor girdisi eşlemesi | Var (s.7) |
| Algoritma boru hattı | Var (s.8–10) |
| Çalışan örnek (statik) | Var (s.11) |
| Geri bildirim öğrenmesi | Var (s.12–13) |
| LLM katmanı ve frenler | Var (s.14–15) |
| Mimari ve dağıtım | Var (s.16–17) |
| Doğrulama | Var (s.18) |
| v5 → v6 kanıta dayalı iterasyon | Var, **en iyi slayt** (s.19) |
| Sınırlar ve yol haritası | Var (s.20) |
| Kapanış sayıları | Var (s.21) |

Slayt 19 sunumun en değerli slaydı: simülasyonun bulduğu dört hatayı ve dört
düzeltmeyi yan yana koyuyor. Bir jüri için "kural yazdık" ile "kuralı ölçtük,
yanlış çıktı, değiştirdik" arasındaki fark budur. Sunumun geri kalanı bu slayda
doğru inşa edilmeli.

---

## 2. Bir tez savunmasında bulunması gerekip **eksik** olanlar

Aşağıdakiler kişisel tercih değil; MSIT bitirme jürisinin standart olarak
sorduğu başlıklar. Önem sırasına göre:

### A. Literatür ve ilgili çalışmalar — **en büyük eksik**
Sunumda tek bir literatür slaydı yok. Slayt 2'de yalnızca bir cümle var
("Türkiye'deki araçlar ya anaokulu yönetimi ya da genel içerik kütüphanesi").
Kuramlar (Ayres, Dunn, Gardner, Piaget, Vygotsky, Bowlby) alıntılanıyor ama
**öneri sistemleri literatürü** hiç yok. Jüri kesinlikle soracak:
"Kural tabanlı öneri sistemleri hakkında literatür ne diyor, sizin boşluğunuz ne?"

Gereken: bir karşılaştırma tablosu — Kidloop vs. Khan Academy Kids / ABCmouse /
Lingokids / Türkiye'deki muadiller; eksenler: bireysel profil, duyusal uyarlama,
açıklanabilirlik, geri bildirim döngüsü, kuramsal temel.

### B. Araştırma sorusu ve kapsam sınırı
Açık bir "bu tezin araştırma sorusu şudur" cümlesi yok. Kapsam-dışı da yok
(ör. terapi/ tanı aracı değildir; klinik iddia taşımaz). Bir cümlelik kapsam
reddi hem akademik hem etik olarak gerekli.

### C. Yöntem (metodoloji) slaydı
Slayt 18–19 yöntemi ima ediyor ama adlandırmıyor. Nasıl çalıştınız: tasarım
bilimi mi, iteratif prototipleme mi? Karar matrisi (38 kayıt) yöntemin
çıktısıdır — bunu bir yöntem artefaktı olarak sunmak sunumun akademik
ağırlığını belirgin biçimde artırır.

### D. "Neden makine öğrenmesi değil de kural tabanlı?"
Bu, jürinin **kesin olarak soracağı** sorudur ve sunumda cevabı yok. Oysa
cevabınız güçlü:
- sıfır kullanıcıyla soğuk başlangıç, eğitilecek etkileşim verisi yok;
- ~243 öğelik havuz — matris çarpanlarına ayırma için çok küçük;
- ebeveyne ve pedagojik sorumluluğa karşı **açıklanabilirlik zorunlu**
  (her terim tek tek gerekçelendirilebiliyor);
- çocuk verisinde veri minimizasyonu (KVKK) — model eğitmek veri biriktirmeyi
  gerektirir, kural tabanlı sistem gerektirmez.

Bu bir kısıt değil, savunulabilir bir tasarım kararıdır. Kendi slaydını hak ediyor.

### E. Geçerlilik: sayılar neyi kanıtlıyor, neyi kanıtlamıyor
Slayt 18 "161 test, 18/18 referans, 4 canlı profil" diyor. Bunların hepsi
**iç doğruluk** kanıtıdır: motor, spesifikasyonun söylediğini yapıyor.
Hiçbiri **pedagojik etkililik** kanıtı değildir — hiçbir gerçek ebeveyn
sistemi kullanmadı. Bu slayt 20'de "Not done" altında geçiyor ama gömülü.
Ayrı bir "Geçerlilik" slaydı gerekli: iç doğruluk (kanıtlandı) / dış geçerlilik
(kanıtlanmadı) ayrımı + bunu ölçecek çalışma tasarımı (30–50 ebeveynlik kapalı
beta: ne ölçülecek, hangi süreyle, hangi ölçekle). Bu ayrımı **siz** yaparsanız
güç kazanırsınız; jüri yaparsa savunmaya düşersiniz.

### F. Etik, KVKK ve çocuk güvenliği
Slayt 14'te tek satır var. Çocuk gelişim verisi işleyen ve ebeveyn serbest
metnini yurt dışındaki bir LLM sağlayıcısına gönderen bir sistem için bu az.
Üstelik **kodda doğrulanmış çok güçlü bir argümanınız var ve sunumda yok**:

> LLM'e giden yükte çocuğa dair hiçbir kimlik verisi yok.
> `FeedbackClassificationPrompt.buildUserMessage()` yalnızca şunu gönderiyor:
> etkinlik başlığı, hedef zekâ, ikincil zekâ, basılan buton, ebeveynin yazdığı metin.
> Çocuk adı, yaşı, ID'si, profili gönderilmiyor.

Buna ek olarak kodda doğruladığım iki nokta daha:
- `DATA_PROCESSING` onayı yoksa çağrı hiç yapılmıyor (`hasDataProcessingConsent`);
- LLM çağrısı hata verirse sistem **fail-open** çalışıyor: sıradan buton
  öğrenmesi devam ediyor, sağlayıcı hata detayı API yanıtına sızmıyor.

Bu üçü bir arada tam bir "Etik ve veri koruma" slaydı yapar ve mimari bir
güvenlik tasarımı olarak sunulabilir.

### G. Ürünün kendisi — **sunumda tek bir ekran görüntüsü yok**
21 slayt tamamen kavramsal. Jüri çalışan ürünü görmek ister. En az 3–4 ekran
görüntüsü (onboarding sorusu, günlük plan, geri bildirim + serbest metin, plan
açıklaması) ya da 60 saniyelik kayıtlı demo. Canlı demo riskli — kayıt tercih edin.

### H. Bireysel katkı ayrımı
İki kişilik proje, MSIT jürileri genelde bireysel not verir. Slayt 16'nın
altında tek satır var. Kişi başına katkıların teslimatlarla eşleştiği açık bir
bölüm gerekli.

### I. Kaynakça slaydı
Kuramlar slayt içinde alıntılanıyor ama biçimsel bir kaynakça slaydı yok.

### J. Ek (appendix) slaytları
Danışmanlık sunumlarının değişmez parçası ve tez savunmasında da işe yarar:
Q&A'da açılacak yedek slaytlar. Öneri: tam `scoring_parameters` tablosu, 38
kayıtlık karar matrisi, 5 kademeli geri çekilme merdiveni, tam bir LLM istem +
yanıt örneği, veritabanı ERD'si, tie-break tohumunun türetimi.

---

## 3. Düzeltilmesi gereken **olgusal** noktalar

Bugün `46d3e5b` üzerinde ölçüldü. Bunlar savunmada açık verebilecek yerler.

| # | Slayt | Sunumda yazan | Kodda ölçülen | Ne yapmalı |
|---|---|---|---|---|
| 1 | 15, 21 | "18 many-shot examples" | **20** (`grep -c '^### Örnek'` = 20; istem başlığı "AŞAĞIDAKİ 20 ÖRNEĞİ") | 18 → **20** |
| 2 | 18, 21 | "161 automated tests in 37 classes" | Kaynakta **162 `@Test`, 39 test sınıfı**; son `mvn test`: **159 çalıştı, 27 atlandı (Docker'a bağlı), 0 hata** | "159 çalıştırıldı · 27'si Docker gerektirdiği için atlandı" yazın. Jüri "hepsini çalıştırdınız mı?" diye sorarsa "161" cevabı savunulamaz |
| 3 | 10 | "SLOT 1 · DEVELOP / SLOT 2 · STRENGTHEN" | `enum PlanSlotType {STRENGTHEN, DEVELOP, EXPLORE}` — API sırası Strengthen, Develop, Explore | Ya enum sırası savunmadan önce düzeltilsin ya da slaytta sıra iddia edilmesin (demo aksini gösterir) |
| 4 | 12 | Teşhis zinciri evrensel gibi sunuluyor | `resolveReason()` satır 395: duyusal dal **yalnızca C3/C4** için çalışıyor. C1/C2 çocukta tolerans aşılsa bile SENSORY üretilmiyor | Slayt 20 bunu "open issue" olarak listeliyor — iki slayt çelişiyor. Slayt 12'ye "şu an C3/C4" notu düşün |
| 5 | 12 | Adım 2: "Independent + anxious → involvement" | Pratikte **erişilemez**: kaygılı çocukta bağımsız etkinlikler zaten havuzdan eleniyor, dolayısıyla oylanabilecek böyle bir öğe oluşmuyor. 237 gerçek oyluk taramada sıfır INVOLVEMENT kaydı | "Tasarlandı, mevcut eleme kuralları nedeniyle henüz tetiklenmiyor" deyin ya da çıkarın |
| 6 | 20 | Açık konular listesi | İki ölçülmüş açık konu eksik: (a) **sunucuda saat dilimi tanımlı değil** — 10+ yerde `LocalDate.now()`, hiçbir `ZoneId` yok, Render UTC çalışıyor → TRT 00:00–03:00 arasında ebeveyn bir önceki günün planını görüyor; (b) **36–48 ay LANGUAGE içerik boşluğu** (bütçe tabanı belirtisinin gerçek nedeni) | İkisini de listeye ekleyin — kendiniz söylerseniz olgunluk, jüri bulursa açık |

Doğrulanan ve **değişmemesi gereken** sayılar: 243 etkinlik, 65 motor
parametresi, 39 Flyway migrasyonu, 22 API uç noktası, 5 kuram, 8 Gardner
zekâsı, 9 alanlı LLM şeması, 6 fren.

---

## 4. "Örnekleri artırmalı mıyız?"

Sorunun iki ayrı anlamı var; cevapları farklı.

### (a) LLM istemindeki many-shot örnekleri: **hayır, artırmayın**
20 örnek yeterli ve bugün makineyle doğrulandı: 20 örneğin tamamı geçerli JSON,
dokuz alanın hepsi her örnekte mevcut, geçersiz enum yok, buton dağılımı
SEVDI 12 · OLMADI 7 · ZORLANDI 1, güven aralığı 0.30–0.90 (5 örnek eşiğin
altında — eşik davranışını öğretiyor), kural ihlali sıfır.

İki bilinçli boşluk kaldı: `duration_hint = SHORT` için örnek yok ve
`BODILY_KINAESTHETIC` hiç çıktı etiketi olarak geçmiyor. Savunmadan önce örnek
eklemeyin — örnek eklemek model davranışını değiştirir ve yeniden doğrulamaya
vaktiniz yok. Bunun yerine bu iki boşluğu **ek slaytta bilinen kapsam sınırı
olarak yazın**; bu titizlik olarak okunur.

### (b) Sunumdaki çalışılmış örnekler: **evet, tam bir tane ekleyin — ve başarısızlık örneği olsun**
Şu an iki başarı senaryosu var (s.11 statik C4, s.13 Ada 3 gün). Üçüncü bir
başarı senaryosu eklemeyin — 20 dakikalık savunmada üç anlatı çok, argüman az
demektir.

Eklenecek tek örnek: **5 kademeli geri çekilme merdiveninin gerçekten
tetiklendiği vaka.** Havuzu tükenen bir çocukta planın strict → yakın geçmişi
geri al → slot/alan kuralını gevşet → kısmi plan → açıklama ekranı biçiminde
nasıl bozulduğunu gösterin. Bu, slayt 10'daki bir madde işaretini kanıta
çevirir. Jüri "peki sistem cevap veremezse ne oluyor?" diye sorduğunda hazır
slaytla cevap vermek, sunumun en güçlü anı olur.

---

## 5. Süre, yoğunluk ve anlatım sırası

**Süre.** 21 slayt / 20 dakika ≈ slayt başına 57 saniye. Slayt 5 (52 şekil),
7 (100 şekil), 13 (52 şekil), 14 (37 şekil) bu sürede anlatılamaz.
Slayt 7 — tam onboarding soru matrisi — salonun arkasından okunamaz.

Öneri: slayt 7'yi eke taşıyın, yerine 4 soruluk bir özet koyun. Tek başına
~90 saniye kazandırır. Hedef: **18–20 ana slayt + 8–10 ek slayt**.

**Sıra.** Mevcut akış problem → kuram → algoritma → geri bildirim → mimari →
doğrulama → sınırlar. Bu bir **inşa** sırası, **argüman** sırası değil.
Danışmanlık sunumu cevapla başlar. Slayt 1'den hemen sonra bir
**yönetici özeti** slaydı ekleyin: iddia, üç destekleyici sütun, kanıt, çekince —
tek sayfada. Jüri 19 slaytlık desteği dinlemeden önce sonucu bilmelidir. İstenen
KPMG/McKinsey karakterini veren en büyük tek yapısal değişiklik budur.

**Başlıklar.** Danışmanlık sunumlarında her slaytın başlığı **konu** değil
**bulgu**dur. Şu an başlıklar konu: "Elimination Layer", "Three Vote Types",
"System Architecture". Bunları iddiaya çevirmek sunumu anında o dile taşır:

| Şimdi | Önerilen |
|---|---|
| Elimination Layer | Tekrarı ceza değil, eleme durdurur |
| Three Vote Types | Zorlanmak ilgi hakkında bir şey söylemez — puanlar değişmez |
| Free-Text Interpretation with an LLM | Model yön verir, büyüklüğü asla model belirlemez |
| System Architecture | İki kişilik ekip için doğru takas: maliyet yerine sıfır operasyon |
| Three-Layer Verification | Motorun doğruluğu kanıtlandı; pedagojik etkililik henüz kanıtlanmadı |

Slayt 19 zaten örtük olarak bunu yapıyor ve sunumun en iyi slaydı olmasının
nedeni de bu.

---

## 6. Önerilen nihai slayt planı (24 ana + 9 ek)

```
 1  Kapak
 2  Yönetici özeti — tek sayfada iddia, üç sütun, kanıt, çekince      [YENİ]
 3  Problem
 4  Araştırma sorusu ve kapsam sınırı                                 [YENİ]
 5  İlgili çalışmalar ve boşluk — karşılaştırma tablosu               [YENİ]
 6  Neden kural tabanlı, neden makine öğrenmesi değil                 [YENİ]
 7  Çözüm — döngü
 8  Yöntem: iteratif tasarım + karar matrisi                          [YENİ]
 9  Beş kuram, beş terim
10  Duyusal profiller (Dunn)
11  Zorluk uyumu ve basamaklar
12  Onboarding — 4 soruluk özet          (tam matris eke taşındı)
13  Motor boru hattı
14  Eleme katmanı
15  Plan kurulumu ve eşitlik bozma
16  Çalışılmış örnek 1 — 30 aylık C4 çocuk
17  Çalışılmış örnek 2 — Ada, 1.–3. gün
18  Çalışılmış örnek 3 — geri çekilme merdiveni tetiklendiğinde       [YENİ]
19  Üç oy tipi ve teşhis zinciri        (C3/C4 sınırı belirtilerek)
20  LLM ile serbest metin yorumlama ve altı fren
21  Geçici durumlar ve çelişkiler
22  Etik, KVKK, veri minimizasyonu                                    [YENİ]
23  Mimari + veritabanı yapılandırma olarak
24  Ürün — ekran görüntüleri / 60 sn demo                             [YENİ]
25  Üç katmanlı doğrulama
26  Geçerlilik: neyi kanıtladık, neyi kanıtlamadık                    [YENİ]
27  v5 → v6: simülasyonun bulduğu
28  Sınırlar, açık konular, yol haritası  (6 madde güncellenerek)
29  Bireysel katkılar                                                 [YENİ]
30  Sayılarla Kidloop + teşekkür
31  Kaynakça                                                          [YENİ]
--- EK ---
A1  Tam onboarding soru matrisi
A2  65 satırlık scoring_parameters
A3  38 kayıtlık karar matrisi
A4  5 kademeli geri çekilme merdiveni, tam
A5  Tam LLM istemi + örnek yanıt
A6  20 many-shot örneğin kapsam haritası + iki bilinen boşluk
A7  Veritabanı ERD
A8  Tie-break tohumunun türetimi
A9  Test envanteri: 159 çalıştı / 27 atlandı, sınıf sınıf
```

Ana gövde 20 dakikada anlatılamaz görünüyorsa: 2, 5, 6, 18, 22, 26 kalır;
16 ve 17'den biri eke iner.
