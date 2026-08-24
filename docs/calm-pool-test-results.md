# Sakin Havuz Yüklemesi ve T1–T12 Test Sonuçları

Tarih: 16 Ağustos 2026 · Ortam: lokal (Postgres 16 + `java -jar`, Flyway v18)
Kapsam: yalnız onboarding sonrası **ilk günlük plan**. Oylama, geri bildirim
işleme, basamak yükselmesi ve tazelik cezası **kodda yok**, test edilmedi.

---

## 1. Yükleme

| Adım | Sonuç |
|---|---|
| Yüklenen dosya | `V10__kidloop_test_activities_calm_pool.sql`, 34 etkinlik (id 129-162) |
| xlsx ile tutarlılık | 34 satır, aynı id ve başlıklar — uyumlu |
| Uygulanan sürüm | **V18** (V10 repoda dolu) |
| Ek migration | **V17** — id çakışmasını çözer (aşağıda) |
| Son durum | **162 etkinlik**: temel 1-128 (boşluksuz) + sakin havuz 129-162 |

### 1.1 Yüklemeden önce çözülmesi gereken çakışma

Dosya olduğu gibi uygulandığında sonuç **162 değil 155** çıkıyor.

Sebep: temel seed (`V10__kidloop_128_home_activities.sql`) 128 etkinlik
içeriyor ama id aralığı **1..135**, çünkü şu 7 id boş bırakılmış:
`73, 76, 84, 90, 97, 120, 121`. Bu yüzden temel seed 129-135'i de kullanıyor.

Sakin havuz 129-162 istiyor → 7 id çakışıyor. Dosyadaki
`ON CONFLICT (id) DO UPDATE` bu 7 temel etkinliği **sessizce üzerine yazıyor**:

| Üzerine yazılan temel etkinlik | Eski id |
|---|---|
| Kendi hikâye kitabını yap 4 sayfa | 129 |
| Sessiz sudoku renkli 4x4 | 130 |
| Duygu termometresi bugünü ölç | 131 |
| Geri dönüşüm ayrıştırma sakin mod | 132 |
| Arkadaşına oyun öğret | 133 |
| Denge tahtası rotası | 134 |
| Şarkı sözü tamamlama yarışması | 135 |

`activity_steps` / `materials` / `outcomes` tablolarında `ON CONFLICT`
olmadığı için bu 7 etkinliğin adımları da karışıyor: eski adımlar silinmeden
yeni adımlar ekleniyor.

**Yapılan:** `V17__relocate_base_activity_ids_129_135.sql` bu 7 etkinliği boş
id'lere taşıyor (129→73, 130→76, 131→84, 132→90, 133→97, 134→120, 135→121).
Etkinlik silinmiyor, yalnız id değişiyor; alt tablolar da taşınıyor.
Sonuç: temel 1..128 boşluksuz, sakin havuz 129..162, toplam 162 — test
dokümanının beklediği tablo.

Doğrulandı: repodaki testlerde 129-135 sabit id referansı **yok**
(`RecommendationScenarioTest`'teki 129/132 sayıları **skor**, id değil).

---

## 2. Asıl sorun kapandı

Kullanıcı raporu: *"gürültü sevmeyen, ayrılık anksiyetesi olan çocuklara
etkinlik önerisi çıkaramadım, hep liste boş geliyor."*

Bu davranış Ufkum'un kendi testinde de kayıtlı:
`RecommendationScenarioTest` senaryo 11 → *"protective profile content gap"*,
C4 profili, beklenen havuz **2**, beklenen plan **boş** (`Map.of()`).

Sakin havuz yüklendikten sonra **12 testin 12'sinde de plan üretildi. Hiçbiri
boş dönmedi.** C4 + kaygı 5 profilinde havuz 2'den 7'ye çıktı.

> **Not (doğrulandı):** `RecommendationScenarioTest` **kırılmıyor** — çalıştırıldı,
> 11/11 geçiyor. Sebep: bu test veritabanına bakmıyor, yalnızca
> `db/migration/V10__kidloop_128_home_activities.sql` dosyasını metin olarak
> okuyor (dosyanın 45. satırındaki `ACTIVITY_SEED` sabiti). Sakin havuz ayrı
> dosyada olduğu için test onu görmüyor.
>
> Sonuç: test yeşil kalıyor ama artık ürünün gerçek davranışını ölçmüyor.
> Test dünyasında 128 etkinlik ve C4'te boş plan var; gerçek DB'de 162 etkinlik
> ve dolu plan var. Ayrıca test kurayı sabitliyor
> (`new Random(){ nextInt → 0 }`), gerçek uygulama sabitlemiyor — bu yüzden
> bölüm 6'daki yuva karışması testte hiç görünmüyor.

---

## 3. Eleme (havuz) sonuçları: 12/12 TAM İSABET

| Test | Beklenen havuz | Ölçülen | Sonuç |
|---|---|---|---|
| T1 · C1, 30 ay | 30 | 30 | ✅ |
| T2 · C4 + kaygı 5, 30 ay | 7 | 7 | ✅ |
| T3 · C4 + düşük kaygı, 30 ay | 8 | 8 | ✅ |
| T4 · C3 + kaygı 5, 30 ay | 28 | 28 | ✅ |
| T5 · C2, 30 ay | 30 | 30 | ✅ |
| T6 · MIXED, 30 ay | 30 | 30 | ✅ |
| T7 · C4 + kaygı 5, 8 ay | 14 | 14 | ✅ |
| T8 · C4 + kaygı 5, 18 ay | 8 | 8 | ✅ |
| T9 · C4 + kaygı 5, 54 ay | 5 | 5 | ✅ |
| T10 · C4 + kaygı 5, 66 ay | 6 | 6 | ✅ |
| T11 · C4 + kaygı 5, 30 ay | 7 | 7 | ✅ |
| T12 · C4 + kaygı 5, 30 ay | 7 | 7 | ✅ |

Eleme mantığı (yaş aralığı, C4'te üç yükün maksimumu < 3, kaygı ≥ 4'te
BAGIMSIZ eleme) **tamamen doğru çalışıyor.** Profil türetme de doğru: 12
testin 12'sinde beklenen quadrant ve kaygı değeri snapshot'a doğru yazıldı.

---

## 4. Seçilen etkinlikler: 8/11 küme isabeti (T11 hariç)

| Test | Beklenen id kümesi | Seçilen id kümesi | Küme |
|---|---|---|---|
| T1 | 8, 67, 80 | 8, 67, 80 | ✅ |
| T2 | 141, 144, 145 | 141, 144, **143** | ❌ |
| T3 | 141, 144, 145 | 141, 144, **143** | ❌ |
| T4 | 141, 8, 67 | 141, 8, 67 | ✅ |
| T5 | 67, 66, 8 | **80**, 66, 8 | ❌ |
| T6 | 8, 67, 80 | 8, 67, 80 | ✅ |
| T7 | 130, 134, 129 | 130, 134, 129 | ✅ |
| T8 | 135, 138, 139 | 135, 138, 139 | ✅ |
| T9 | 153, 154, 156 | 153, 154, 156 | ✅ |
| T10 | 159, 158, 160 | 159, 158, 160 | ✅ |
| T11 | *(15 dk çalıştırılamadı)* | — | — |
| T12 | 142, 143 | 142, 143 | ✅ |

T5'teki sapma **beraberlik**: id 67 ve id 80 C2 profilinde ikisi de tam
**102.0** alıyor (67: D=33, 80: D=18 ama P farkı dengeliyor). Motor
beraberlikte kura çekiyor, 80'i seçti. Rastgele; her koşuda değişebilir.

T2/T3'teki sapma da beraberlik: 144 ve 143 aynı skoru alıyor.

---

## 5. Skorlar: C1/C2/C3/MIXED tam isabet, C4'te sistematik fark

### 5.1 Doğru çıkanlar

| Test | Profil | Beklenen skor | Ölçülen | Sonuç |
|---|---|---|---|---|
| T1 | C1 | 127.0 / 124.0 / 120.0 | 127.0 / 124.0 / 120.0 | ✅ |
| T4 | C3 | 141.45 / 136.85 / 129.95 | 141.45 / 136.85 / 129.95 | ✅ |
| T6 | MIXED | 127.0 / 124.0 / 120.0 | 127.0 / 124.0 / 120.0 | ✅ |
| T5 | C2 | 111.0 / 108.0 / 102.0 | 111.0 / 108.0 / 102.0 | ✅ |

C3'ün çift katsayısı (10/10/6), C2'nin hareket ağırlıklı katsayısı (3/3/6),
C1 ve MIXED'in 5/5/3'ü — **hepsi doğru uygulanıyor.** D terimi bu dört
profilde kusursuz.

### 5.2 C4 profilinde sistematik fark: D = 0

C4 olan **her** testte (T2, T3, T7, T8, T9, T10, T12) ölçülen skor
dokümandakinden yüksek çıktı. Tek sebep var: **motor C4 çocukta duyusal ceza
hesaplamıyor, D = 0 alıyor.**

| Test | id | Doküman | Ölçülen | Fark nereden |
|---|---|---|---|---|
| T2 | 141 | 146.05 = (100−**8**+15+20)×1.15 | 155.25 = (100−**0**+15+20)×1.15 | D |
| T3 | 141 | 127.0 = (100−**8**+15+20)×1.0 | 135.0 = (100−**0**+15+20)×1.0 | D |
| T7 | 134 | 134.55 = (100−**3**+0+20)×1.15 | 138.0 = (100−**0**+0+20)×1.15 | D |
| T8 | 139 | 134.55 = (100−**3**+0+20)×1.15 | 138.0 = (100−**0**+0+20)×1.15 | D |
| T9 | 153 | 146.05 = (100−**8**+15+20)×1.15 | 155.25 = (100−**0**+15+20)×1.15 | D |
| T10 | 158 | 94.3 = (100−**8**+15−25)×1.15 | 103.5 = (100−**0**+15−25)×1.15 | D |
| T12 | 142 | 128.8 = (100−**3**+15+0)×1.15 | 132.25 = (100−**0**+15+0)×1.15 | D |

P, Z ve B terimleri her satırda birebir tutuyor. Fark **yalnız D**.

**Bu bir kod hatası değil, iki belge arasındaki çelişki:**

* **Model metni** diyor ki: *"Ç4: katsayı yok. Ç4'te ceza hesabı yapılmaz;
  yükü 3 ve üstü etkinlik direkt listeden ÇIKARILIR (eleme)."*
  → `dunn_profiles` tablosunda C4 satırının üç ağırlığı da **NULL**.
  Motor bu tabloyu okuyor ve modele uyuyor.
* **Test dokümanı** her C4 testinde D'yi 5/5/3 katsayılarıyla hesaplamış
  (tam olarak C1'in katsayıları), ama profil tablosunda yine
  *"Ceza katsayısı: hesaplanmaz, eleme uygulanır"* yazıyor. Doküman kendi
  içinde de tutarsız.

**Karar gerekiyor** (kod veya doküman, ikisinden biri değişmeli):
1. C4'te de ceza hesaplansın → `dunn_profiles`'a C4 için ağırlık girilir.
   Hangi değerler? Dokümanın kullandığı 5/5/3 mü?
2. C4'te ceza hesaplanmasın (model metni böyle diyor) → test dokümanının
   C4 beklenen skorları D=0 ile yeniden hesaplanmalı.

---

## 6. Yuva (slot) etiketleri: 12/12 sapma

Etkinlik kümesi doğru seçildiği testlerde bile **yuva etiketleri karışıyor.**

T1 örneği:

| Yuva | Doküman | Ölçülen |
|---|---|---|
| GELISIM | 8 (127.0) | 67 (124.0) |
| GUCLENDIRME | 67 (124.0) | 80 (120.0) |
| KESIF | 80 (120.0) | **8 (127.0)** |

Aynı üç etkinlik, aynı üç skor, farklı üç etiket. Model der ki *"GELİŞİM
yuvası: dönem görevi domain'inden en yüksek skor"* → id 8 (LANGUAGE, 127)
GELİŞİM olmalıydı; KEŞİF'e düştü.

Sebep `DailyPortfolioBuilder.build()`: yuva başına ayrı ayrı en iyiyi
seçmiyor, **üçlü kombinasyonun toplam skorunu** maksimize edip beraberlikte
kura çekiyor. Toplam her dağılımda aynı olduğu için etiketi kura belirliyor.

Bu, aynı çocuk için **her istekte farklı etiket** üretebilir.

---

## 7. T11 çalıştırılamadı

Test dokümanı T11 için **15 dk** bütçe istiyor. Şemada bu değer yok:

```
chk_parent_profiles_time_budget CHECK (daily_time_budget_minutes IN (10, 20, 30))
```

Q7 seçenekleri: A=10, B=20, C=30. **15 girilemiyor.**
T11 bilgi amaçlı 20 dk ile çalıştırıldı; havuz 7 (beklenen 7 ✅), plan 3
etkinlik / 20 dk. Doküman ile karşılaştırma yapılmadı, çünkü girdi farklı.

Doküman ya 20 dk'ya çekilmeli ya da Q7'ye 15 dk seçeneği eklenmeli.

---

## 8. Dokümanda saptanan iki iç tutarsızlık

1. **T3 girdi ↔ beklenen profil çelişkisi.** Girdi *"Q4: Çoğunlukla rahat"*
   diyor, bu seçenek `separation_anxiety = 2`. Beklenen profil tablosu ise
   *"Ayrılık kaygısı 1"* yazıyor (bu *"Çok rahat"* seçeneği olurdu).
   Skoru etkilemiyor (ikisi de < 4 → B = ×1), ama düzeltilmeli.
2. **C4 ceza katsayısı çelişkisi.** Bölüm 5.2'de anlatıldı.

---

## 9. Toplu skor tablosu (ölçülen)

| Test | Profil | Bütçe | Toplam süre | Seçilen (yuva: id = skor) |
|---|---|---|---|---|
| T1 | C1 / 2 | 30 | 30 | DEV:67=124.0 · STR:80=120.0 · EXP:8=127.0 |
| T2 | C4 / 5 | 30 | 25 | DEV:141=155.25 · STR:144=138.0 · EXP:143=138.0 |
| T3 | C4 / 2 | 30 | 25 | DEV:141=135.0 · STR:144=120.0 · EXP:143=120.0 |
| T4 | C3 / 5 | 30 | 30 | DEV:67=129.95 · STR:8=136.85 · EXP:141=141.45 |
| T5 | C2 / 2 | 30 | 30 | DEV:8=108.0 · STR:80=102.0 · EXP:66=111.0 |
| T6 | MIXED / 2 | 30 | 30 | DEV:67=124.0 · STR:80=120.0 · EXP:8=127.0 |
| T7 | C4 / 5 | 30 | 15 | DEV:130=155.25 · STR:129=132.25 · EXP:134=138.0 |
| T8 | C4 / 5 | 30 | 25 | DEV:135=155.25 · STR:139=138.0 · EXP:138=138.0 |
| T9 | C4 / 5 | 30 | 25 | DEV:154=155.25 · STR:153=155.25 · EXP:156=138.0 |
| T10 | C4 / 5 | 30 | 30 | DEV:158=103.5 · STR:160=86.25 · EXP:159=155.25 |
| T11 | C4 / 5 | 20\* | 20 | DEV:141=155.25 · STR:146=138.0 · EXP:143=138.0 |
| T12 | C4 / 5 | 10 | 10 | DEV:142=132.25 · STR:143=138.0 |

\* 15 dk şemada yok, 20 dk ile çalıştırıldı.

T2↔T3 karşılaştırmalı kontrolü doğrulandı: aynı etkinlikler, T2 skorları
T3'ün tam **1.15 katı** (155.25/135.0 = 1.15, 138.0/120.0 = 1.15).
Bowlby çarpanı doğru çalışıyor.

T1↔T6 karşılaştırmalı kontrolü doğrulandı: MIXED, C1 ile birebir aynı sonucu
verdi.

---

## 10. Özet

| Aşama | Durum |
|---|---|
| Profil türetme (quadrant, kaygı, yaş bandı) | ✅ 12/12 |
| Eleme / havuz büyüklüğü | ✅ 12/12 |
| D terimi — C1, C2, C3, MIXED | ✅ tam isabet |
| D terimi — C4 | ⚠️ D=0; doküman D>0 bekliyor (karar gerekli) |
| P, Z, B terimleri | ✅ tam isabet |
| Etkinlik seçimi (küme) | ⚠️ 8/11 — 3 sapma, üçü de beraberlik kaynaklı |
| Yuva etiketleri | ❌ 12/12 sapma (kombinasyon + kura) |
| Boş plan sorunu | ✅ çözüldü |
