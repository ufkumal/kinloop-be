# T1–T12 Test Sonuçları — Ufkum'un main'i (`d29dd9c`)

Tarih: 17 Ağustos 2026
Kod: `d29dd9c` (PR #21, *"new activities inserted"*), **değiştirilmeden**
Ortam: lokal Postgres 16 + `java -jar`, veritabanı sıfırdan kuruldu
Yöntem: gerçek HTTP çağrıları (kayıt → doğrulama → login → çocuk → anket → `/daily-plan/today`)

Kapsam: yalnız onboarding sonrası **ilk günlük plan**. Oylama, geri bildirim işleme,
basamak yükselmesi ve tazelik cezası kodda yok, test edilmedi.

---

## 1. Migration durumu

Ufkum V17 ve V18'i main'e ekledi. Kontrol edildi:

| Dosya | Durum |
|---|---|
| `V18__kidloop_test_activities_calm_pool.sql` | Gönderdiğimle **birebir aynı** |
| `V17__relocate_base_activity_ids_129_135.sql` | **Değiştirilmiş** — aşağıda |

**V17'deki değişiklik bir iyileştirme.** Benim sürümüm hedef id'ler doluysa sessizce
hiçbir şey yapmıyordu. Ufkum bunu yerine bir `DO $$ ... $$` bloğu koymuş: yalnız iki
tutarlı durumu kabul ediyor — yedi kaynak dolu / yedi hedef boş (taşı), ya da yedi
kaynak boş / yedi hedef dolu (zaten taşınmış, no-op). Kısmi veya karışık durumda
`RAISE EXCEPTION` ile duruyor:

```sql
RAISE EXCEPTION
    'Activity ID relocation aborted: expected source/target counts 7/0 or 0/7, found %/%',
    source_count, target_count;
```

Yarım kalmış bir veritabanında sessizce yanlış sonuç üretmek yerine gürültülü
başarısızlık veriyor. Doğru karar.

18 migration temiz uygulandı. Sonuç:

| Toplam | Temel (1-128) | Sakin havuz (129-162) |
|---|---|---|
| **162** | 128 | 34 |

---

## 2. Asıl sorun: kapandı

**12 testin 12'sinde de plan üretildi. Hiçbiri boş dönmedi.**
C4 + kaygı 5 profilinde havuz 2'den 7'ye çıktı.

Planların kaynağı, sakin havuzun tam olarak hedeflenen yerde çalıştığını gösteriyor:

| Test | Profil | Plandaki etkinlik | Sakin havuzdan |
|---|---|---|---|
| T1 | C1 | 3 | 0 |
| T2 | C4 · kaygı 5 | 3 | 3 |
| T3 | C4 · kaygı 2 | 3 | 3 |
| T4 | C3 · kaygı 5 | 3 | 1 |
| T5 | C2 | 3 | 0 |
| T6 | MIXED | 3 | 0 |
| T7 | C4 · 8 ay | 3 | 3 |
| T8 | C4 · 18 ay | 3 | 3 |
| T9 | C4 · 54 ay | 3 | 3 |
| T10 | C4 · 66 ay | 3 | 3 |
| T11 | C4 · 20 dk | 3 | 3 |
| T12 | C4 · 10 dk | 2 | 1 |

Sakin çocuklar (C1/C2/MIXED) eski havuzdan besleniyor, korunmacı çocukların planları
neredeyse tamamen yeni etkinliklerden oluşuyor.

---

## 3. Eleme (havuz): 12/12 TAM İSABET

| Test | Beklenen | Ölçülen | |
|---|---|---|---|
| T1 · C1, 30 ay | 30 | 30 | ✅ |
| T2 · C4 + kaygı 5, 30 ay | 7 | 7 | ✅ |
| T3 · C4 + kaygı 2, 30 ay | 8 | 8 | ✅ |
| T4 · C3 + kaygı 5, 30 ay | 28 | 28 | ✅ |
| T5 · C2, 30 ay | 30 | 30 | ✅ |
| T6 · MIXED, 30 ay | 30 | 30 | ✅ |
| T7 · C4 + kaygı 5, 8 ay | 14 | 14 | ✅ |
| T8 · C4 + kaygı 5, 18 ay | 8 | 8 | ✅ |
| T9 · C4 + kaygı 5, 54 ay | 5 | 5 | ✅ |
| T10 · C4 + kaygı 5, 66 ay | 6 | 6 | ✅ |
| T11 · C4 + kaygı 5, 30 ay | 7 | 7 | ✅ |
| T12 · C4 + kaygı 5, 30 ay | 7 | 7 | ✅ |

Profil türetme de 12/12: beklenen quadrant ve kaygı değeri snapshot'a doğru yazıldı.

---

## 4. YENİ VE EN ÖNEMLİ BULGU: sonuç her koşuda değişiyor

Bu koşu ile 16 Ağustos'taki koşu **aynı kod ve aynı veriyle farklı sonuçlar verdi.**
T2, T3, T4, T7, T9 ve T12'de seçilen etkinlikler ya da yuvaları değişti.

Bunu ölçmek için aynı T2 profilini (C4, kaygı 5, 30 ay, 30 dk) beş ayrı çocukla
tekrarladım:

| Koşu | GELİŞİM | GÜÇLENDİRME | KEŞİF |
|---|---|---|---|
| 1 | 141 | **146** | **143** |
| 2 | 141 | **145** | **144** |
| 3 | 141 | **145** | **143** |
| 4 | 141 | **146** | **144** |
| 5 | 141 | **145** | **143** |

**Beş özdeş çocuk, dört farklı plan.**

GELİŞİM yuvası kararlı (141), ama GÜÇLENDİRME ve KEŞİF her seferinde değişiyor.
Sebep `DailyPortfolioBuilder`: aday etkinlikler beraberlikte eşit skor aldığında
(`145` ve `146` ikisi de 138.0, `143` ve `144` ikisi de 138.0) seçimi
`random.nextInt(tied.size())` yapıyor.

**Ürün açısından anlamı:** aynı profildeki iki çocuk farklı plan alıyor; aynı çocuk
farklı bir günde farklı plan alabiliyor. Ve bu, test dokümanının beklenen çıktılarının
**hiçbir koşuda güvenilir biçimde tutmayacağı** anlamına geliyor — sabit beklenen
değerlerle test etmek mümkün değil.

---

## 5. Seçilen etkinlikler: 7/11 (bu koşuda)

| Test | Beklenen küme | Bu koşuda seçilen | |
|---|---|---|---|
| T1 | 8, 67, 80 | 8, 67, 80 | ✅ |
| T2 | 141, 144, 145 | 141, **143**, 145 | ❌ |
| T3 | 141, 144, 145 | 141, 144, **146** | ❌ |
| T4 | 141, 8, 67 | 141, 8, 67 | ✅ |
| T5 | 67, 66, 8 | **80**, 66, 8 | ❌ |
| T6 | 8, 67, 80 | 8, 67, 80 | ✅ |
| T7 | 130, 134, 129 | 130, 134, 129 | ✅ |
| T8 | 135, 138, 139 | 135, 138, 139 | ✅ |
| T9 | 153, 154, 156 | 153, 154, 156 | ✅ |
| T10 | 159, 158, 160 | 159, 158, 160 | ✅ |
| T11 | *(15 dk çalıştırılamadı)* | — | — |
| T12 | 142, 143 | 142, **146** | ❌ |

Önceki koşuda bu oran 8/11'di; T12 o zaman tutuyordu, şimdi tutmuyor. Oranın kendisi
kararsız — madde 4'ün doğrudan sonucu.

Dört sapmanın dördü de **beraberlik** kaynaklı, hesap hatası değil.

---

## 6. Yuva etiketleri: 12/12 sapma

Etkinlikler doğru seçildiği testlerde bile etiketler modelin kuralına uymuyor.

T1 (bu koşu):

| Yuva | Doküman | Ölçülen |
|---|---|---|
| GELİŞİM | 8 — 127.0 | 67 — 124.0 |
| GÜÇLENDİRME | 67 — 124.0 | 80 — 120.0 |
| KEŞİF | 80 — 120.0 | **8 — 127.0** |

T9'da da aynı: 153 ve 154 ikisi de 155.25; hangisinin GELİŞİM olacağını kura seçiyor.

Model "GELİŞİM = dönem görevi domain'inden **en yüksek** skor" diyor. id 8 (LANGUAGE,
127.0) GELİŞİM olmalıydı, KEŞİF'e düştü.

Sebep: `DailyPortfolioBuilder.build()` yuva başına en iyiyi seçmiyor; üçlünün
**toplam** skorunu maksimize ediyor. Toplam permütasyona duyarsız olduğu için
bütün dağılımlar berabere kalıyor ve etiketi kura belirliyor.

Ek olarak aday listeleri ilk gün birbirine yakınsıyor:
`StrengthenCandidateSelector` bütün Gardner puanları eşitken filtre uygulamadan
global ilk 5'i döndürüyor; `explore` de bütün alanlar 0 örneklenmiş olduğu için
global ilk 3'ü alıyor. Aynı etkinlik üç listede birden bulunabiliyor.

---

## 7. Skorlar: C4 dışında tam isabet

| Test | Profil | Beklenen | Ölçülen | |
|---|---|---|---|---|
| T1 | C1 | 127.0 / 124.0 / 120.0 | 127.0 / 124.0 / 120.0 | ✅ |
| T4 | C3 | 141.45 / 136.85 / 129.95 | 141.45 / 136.85 / 129.95 | ✅ |
| T5 | C2 | 111.0 / 108.0 / 102.0 | 111.0 / 108.0 / 102.0 | ✅ |
| T6 | MIXED | 127.0 / 124.0 / 120.0 | 127.0 / 124.0 / 120.0 | ✅ |

C3'ün çift katsayısı (10/10/6), C2'nin hareket ağırlığı (3/3/6), C1 ve MIXED'in
5/5/3'ü — hepsi doğru uygulanıyor.

### C4'te sistematik fark: motor D = 0 alıyor

| Test | id | Doküman | Ölçülen |
|---|---|---|---|
| T2 | 141 | 146.05 = (100−**8**+15+20)×1.15 | 155.25 = (100−**0**+15+20)×1.15 |
| T3 | 141 | 127.0 = (100−**8**+15+20)×1.0 | 135.0 = (100−**0**+15+20)×1.0 |
| T9 | 153 | 146.05 = (100−**8**+15+20)×1.15 | 155.25 = (100−**0**+15+20)×1.15 |
| T10 | 158 | 94.3 = (100−**8**+15−25)×1.15 | 103.5 = (100−**0**+15−25)×1.15 |
| T12 | 142 | 128.8 = (100−**3**+15+0)×1.15 | 132.25 = (100−**0**+15+0)×1.15 |

P, Z ve B her satırda birebir tutuyor. Fark yalnız D.

`dunn_profiles` tablosunda C4'ün toleransı dolu (1/2/2) ama üç katsayısı da `NULL`.
Motor çarpacak sayı bulamıyor. Model metni de bunu söylüyor: *"Ç4: katsayı yok, ceza
hesabı yapılmaz, eleme uygulanır."* Test dokümanı ise C1'in katsayılarıyla (5/5/3)
hesaplamış — üstelik kendi profil tablosunda "hesaplanmaz" yazarken.

**Bu, madde 4'teki kararsızlığın da bir sebebi.** D=0 olunca C4 havuzunda skorlar
yığılıyor. T2 havuzu iki senaryoda:

| id | Etkinlik | Yük | D=0 (kod) | D=5/5/3 (doküman) |
|---|---|---|---|---|
| 141 | Fısıltı kuklası sohbeti | 2/2/1 | 155.25 | 146.05 |
| 143 | Yavaş nefes balonu sessiz | 1/1/1 | **138.00** | 128.80 |
| 145 | Duygu yüzleri aynada | 1/2/1 | **138.00** | 134.55 |
| 146 | Sessiz ritim: parmak ucuyla | 2/1/2 | **138.00** | 126.50 |
| 144 | İkişer eşleştirme sessiz | 1/2/1 | **138.00** | 134.55 |
| 142 | Bu ne oyunu sade kartlarla | 1/2/1 | 132.25 | 128.80 |
| 71 | Bitki sulama görevi | 2/2/2 | 115.00 | 109.25 |

D=0 iken **dört etkinlik 138.00'de eşitleniyor** — beş koşuda dört farklı plan tam
olarak buradan çıkıyor. D=5/5/3 iken ayrışıyorlar ve ilk üç dokümanın beklediği
141, 144, 145 oluyor.

Yani C4 katsayısı yalnız puanı değil, **seçimin kararlı olup olmadığını** belirliyor.

---

## 8. Koşulamayan ve düzeltilmesi gerekenler

- **T11 koşulamadı.** Doküman 15 dk bütçe istiyor; şema yalnız 10/20/30 kabul ediyor
  (`chk_parent_profiles_time_budget`; Q7 seçenekleri A=10, B=20, C=30). Bilgi amaçlı
  20 dk ile koşuldu, havuz 7 çıktı; karşılaştırma yapılmadı.
- **T3 kendiyle çelişiyor.** Girdi *"Çoğunlukla rahat"* (kaygı 2), beklenen profil
  tablosu *"kaygı 1"* diyor. Skoru etkilemiyor, ikisi de eşiğin altında.
- **Skor 100'de kesilmiyor.** `score_display_max = 100` parametresi var ve açıklaması
  *"parent-facing clamped score"* diyor. `ActivityScorer:31` clamp'li değeri hesaplıyor
  ama `ActivityMatchingService:85-86` ham skoru kaydedip döndürüyor. API'den 155.25
  gibi değerler çıkıyor.
- **`RecommendationScenarioTest` gerçeği ölçmüyor.** Çalıştırıldı, 11/11 geçiyor —
  çünkü veritabanına bakmıyor, yalnız `db/migration/V10__kidloop_128_home_activities.sql`
  dosyasını metin olarak okuyor. Test dünyasında 128 etkinlik ve C4'te boş plan var;
  gerçek veritabanında 162 etkinlik ve dolu plan var. Test ayrıca kurayı sabitliyor
  (`new Random(){ nextInt → 0 }`), uygulama sabitlemiyor — madde 4 ve 6 bu yüzden
  testte hiç görünmüyor.

---

## 9. Özet

| Aşama | Durum |
|---|---|
| Migration'lar (V17 + V18) | ✅ temiz uygulandı, 162 etkinlik |
| Boş plan sorunu | ✅ **çözüldü**, 12/12 plan üretildi |
| Profil türetme | ✅ 12/12 |
| Eleme / havuz büyüklüğü | ✅ 12/12 |
| D — C1, C2, C3, MIXED | ✅ tam isabet |
| P, Z, B terimleri | ✅ tam isabet |
| D — C4 | ⚠️ D=0; karar gerekli |
| Etkinlik seçimi | ⚠️ 7/11 bu koşuda — **oran kararsız** |
| Sonuç kararlılığı | ❌ aynı profil 5 koşuda 4 farklı plan |
| Yuva etiketleri | ❌ 12/12 sapma |

## 10. Karar bekleyen üç konu

1. **C4 duyusal cezası.** `dunn_profiles`'a C4 katsayıları girilsin mi (doküman 5/5/3
   kullanmış), yoksa dokümanın C4 beklenen skorları D=0 ile mi yeniden yazılsın?
   Katsayı girmek aynı zamanda madde 4'teki beraberlik yığılmasını büyük ölçüde çözer.
2. **Yuva ataması ve kura.** `DailyPortfolioBuilder.best()` toplam skor yerine yuva
   önceliğine göre sıralasın ve `Random` kaldırılsın mı? Bu, madde 4 ve 6'yı birlikte
   kapatır ve sonucu deterministik yapar.
3. **Testin veri kaynağı.** `RecommendationScenarioTest` sakin havuzu da okusun mu?
   Okumazsa gerçek davranışı ölçmemeye devam eder.
