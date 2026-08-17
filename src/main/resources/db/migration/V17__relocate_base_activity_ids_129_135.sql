-- =====================================================================
-- V17__relocate_base_activity_ids_129_135.sql
--
-- NEDEN
--   V10 temel seed'i 128 etkinlik iceriyor ama id araligi 1..135; cunku
--   1..135 arasinda 7 bos id var: 73, 76, 84, 90, 97, 120, 121.
--   Bu yuzden temel seed 129, 130, 131, 132, 133, 134, 135 id'lerini de
--   kullaniyor.
--
--   Sakin havuz test verisi (V18) id 129-162 araligini istiyor ve test
--   dokumani beklenen ciktilarda bu id'leri isim isim referans veriyor
--   (129, 130, 134, 135, 138, 139, 141, 142, 143, 144, 145, 153, 154,
--   156, 158, 159, 160). Onlem alinmazsa V18'in ON CONFLICT (id) DO UPDATE
--   ifadesi temel seed'in 7 etkinligini SESSIZCE UZERINE YAZAR: toplam
--   162 yerine 155 olur ve 7 etkinlik kaybolur.
--
-- NE YAPIYOR
--   Temel seed'in 129-135 etkinliklerini bos id'lere tasir:
--     129->73  130->76  131->84  132->90  133->97  134->120  135->121
--   Boylece temel seed 1..128 araligini bosluksuz doldurur, 129-162
--   araligi sakin havuza kalir ve toplam 162 olur.
--
--   Etkinlikler SILINMEZ, yalnizca id'leri degisir. Alt tablolardaki
--   satirlar (instructions, steps, materials, outcomes, ...) yeni id'ye
--   tasinir. activities(id) uzerindeki yabanci anahtarlarda ON UPDATE
--   CASCADE olmadigi icin sira: once yeni satiri ekle, sonra cocuklari
--   tasi, en son eskiyi sil.
--
-- IDEMPOTENT: tum satirlar daha once tasinmissa hicbir sey yapmaz.
-- GUVENLIK: kaynak/hedef durumu kismi veya karisiksa islemi durdurur.
-- =====================================================================

BEGIN;

CREATE TEMP TABLE tmp_activity_id_map (old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL) ON COMMIT DROP;

INSERT INTO tmp_activity_id_map (old_id, new_id) VALUES
    (129, 73), (130, 76), (131, 84), (132, 90), (133, 97), (134, 120), (135, 121);

-- Guvenlik: yalnizca iki tutarli durum kabul edilir:
--   1) yedi kaynak dolu, yedi hedef bos (tasima yapilir),
--   2) yedi kaynak bos, yedi hedef dolu (daha once tasinmis; no-op).
-- Kismi veya karisik durumda V18'in veri ezmesini engellemek icin durur.
DO $$
DECLARE
    source_count INTEGER;
    target_count INTEGER;
    mapping_count INTEGER;
BEGIN
    SELECT count(*) INTO mapping_count FROM tmp_activity_id_map;
    SELECT count(*) INTO source_count
      FROM tmp_activity_id_map m
      JOIN activities a ON a.id = m.old_id;
    SELECT count(*) INTO target_count
      FROM tmp_activity_id_map m
      JOIN activities a ON a.id = m.new_id;

    IF source_count = mapping_count AND target_count = 0 THEN
        NULL;
    ELSIF source_count = 0 AND target_count = mapping_count THEN
        DELETE FROM tmp_activity_id_map;
    ELSE
        RAISE EXCEPTION
            'Activity ID relocation aborted: expected source/target counts 7/0 or 0/7, found %/%',
            source_count, target_count;
    END IF;
END $$;

-- 1) Etkinlik satirlarini yeni id ile kopyala.
INSERT INTO activities (
    id, scope, workshop_id, title, description, min_age_months, max_age_months,
    status, target_intelligence, secondary_intelligence, target_domain,
    difficulty, duration_minutes, involvement_type,
    noise_load, visual_load, physical_intensity, is_scaffolded,
    created_at, updated_at, deleted_at)
SELECT m.new_id, a.scope, a.workshop_id, a.title, a.description, a.min_age_months, a.max_age_months,
       a.status, a.target_intelligence, a.secondary_intelligence, a.target_domain,
       a.difficulty, a.duration_minutes, a.involvement_type,
       a.noise_load, a.visual_load, a.physical_intensity, a.is_scaffolded,
       a.created_at, a.updated_at, a.deleted_at
FROM tmp_activity_id_map m
JOIN activities a ON a.id = m.old_id;

-- 2) Alt tablolari yeni id'ye tasi.
UPDATE activity_instructions      c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE activity_steps             c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE activity_materials         c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE activity_outcomes          c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE activity_media             c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE activity_attributes        c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE activity_sessions          c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE activity_workshop_details  c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE daily_plan_items           c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE recommendations            c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;
UPDATE feedback                   c SET activity_id = m.new_id FROM tmp_activity_id_map m WHERE c.activity_id = m.old_id;

-- 3) Eski satirlari sil (alt tablolar artik yeni id'yi gosteriyor).
DELETE FROM activities a USING tmp_activity_id_map m WHERE a.id = m.old_id;

COMMIT;
