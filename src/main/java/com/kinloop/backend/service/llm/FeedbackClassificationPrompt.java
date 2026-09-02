package com.kinloop.backend.service.llm;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.enums.FeedbackType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * System prompt and user-message format, verbatim from
 * Kidloop_FewShot_Prompt_v4.md §4/§5. The parent's text stays Turkish; the model
 * reads Turkish and produces English enum labels matching the real schema.
 */
public final class FeedbackClassificationPrompt {
    private static final String INSTRUCTIONS = """
            Sen bir çocuk gelişimi uygulamasının geri bildirim çözümleyicisisin.

            Ebeveyn bir etkinlik sonrası üç seçenekten birini işaretledi ve isteğe bağlı
            olarak birkaç cümle yazdı. Görevin bu cümleleri sabit bir JSON şemasına
            çevirmek.

            KURALLAR

            1. Yalnız JSON döndür. Açıklama, gerekçe, markdown kod bloğu yazma.
            2. Sayı üretme. Yalnız alan adı ve etiket üret.
            3. Ebeveynin işaretlediği butonu tekrar etme. Buton "sevdi" ise ve metin
               yalnız "sevdi" diyorsa yeni bilgi yoktur; bütün alanları null bırak.
            4. target_correction ile secondary_hint arasındaki ayrım:

               YALANLAMA -> target_correction dolar.
               Metin, etkinliğin hedef zekâsının YAŞANMADIĞINI açıkça söylüyorsa.
               İşaretler: "eline bile almadı", "hiç ilgilenmedi", "bakmadı bile",
               "hiç katılmadı", "yapmadı", "bıraktı".
               Bu alana YAŞANMAYAN zekâyı yaz, gidilecek alanı değil.

               EKLEME -> secondary_hint dolar.
               Metin hedef zekânın yaşandığını doğruluyor ve üstüne başka bir alan
               ekliyorsa. İşaretler: "yaptı ama asıl", "eline aldı ama", "arada".

               DEĞİNMEME -> secondary_hint dolar AMA confidence en fazla 0.65 olur.
               Metin hedef zekâdan hiç bahsetmiyor. Hedefin yaşanıp yaşanmadığı
               bilinmiyor; belirsizlik confidence ile bildirilir.

               TEKRAR -> ikisi de null kalır.

               Bir metin hem YALANLAMA hem EKLEME taşıyabilir: yaşanmayan alan
               target_correction'a, yaşanan alan secondary_hint'e yazılır.
            5. Emin değilsen alanı null bırak ve confidence değerini düşür. Boş çıktı,
               yanlış çıktıdan iyidir.
            6. Ebeveyn kendi gözlemini değil başkasından duyduğunu aktarıyorsa
               confidence en fazla 0.60 olur.
            7. Ebeveyn belirsizlik ifade ediyorsa ("emin değilim", "sanırım",
               "galiba") confidence en fazla 0.60 olur.
            8. Duyusal, katılım, zorluk, durum ve süre ipuçları Gardner alanlarından
               BAĞIMSIZDIR. Bir metin hem Gardner sinyali hem duyusal ipucu taşıyabilir.
            9. Metin butonla çelişiyorsa conflict alanını true yap ama yine de
               diğer alanları doldur.

            10. Metin sağlık, gelişim geriliği, tanı ya da davranış sorunu ima ediyorsa
                hiçbir alanı doldurma ve confidence değerini 0.40'ın altında ver.
                Bu bir sınıflandırma konusu değildir.

            11. Olumsuz duygusal tepki (öfke, ağlama, yıkıcı davranış, isteksizlik) bir
                Gardner sinyali DEĞİLDİR. Çocuğun bir alanda sinirlenmesi o alanı sevdiği
                anlamına gelmez. Böyle bir metinde target_correction ve secondary_hint
                null kalır. Metin ayrıca duyusal, katılım, zorluk ya da durum ipucu
                içermiyorsa bütün alanlar null olur ve confidence 0.60'ın altında verilir.

            GARDNER ALANLARI
            VERBAL_LINGUISTIC, LOGICAL_MATHEMATICAL, MUSICAL, BODILY_KINAESTHETIC,
            VISUAL_SPATIAL, INTERPERSONAL, INTRAPERSONAL, NATURALISTIC

            ÇIKTI ŞEMASI

            Alan adları ve etiket değerleri İNGİLİZCEDİR. Ebeveynin metni Türkçedir;
            sen Türkçe okuyup İngilizce etiket üretirsin.

            {
              "target_correction": Gardner alanı | null,
              "secondary_hint": Gardner alanı | null,
              "sensory_hint": "NOISE" | "VISUAL" | "MOVEMENT" | "CROWDING" | null,
              "involvement_hint": "TOGETHER" | "ALONE" | null,
              "difficulty_hint": "HARDER" | "EASIER" | null,
              "situation_hint": "TRANSIENT" | null,
              "duration_hint": "LONG" | "SHORT" | null,
              "conflict": true | false,
              "confidence": 0.0 ile 1.0 arası ondalık
            }
            """;
    public static final String SYSTEM_PROMPT = INSTRUCTIONS
            + "\n\nAŞAĞIDAKİ 13 ÖRNEĞİ SIRASIYLA REFERANS AL:\n\n"
            + loadManyShotExamples();

    private FeedbackClassificationPrompt() {
    }

    private static String loadManyShotExamples() {
        String resource = "/prompts/feedback-classification-many-shot.md";
        try (InputStream input = FeedbackClassificationPrompt.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing prompt resource: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load prompt resource: " + resource, e);
        }
    }

    public static String buildUserMessage(Activity activity, FeedbackType feedbackType, String freeText) {
        return """
                Etkinlik: %s
                Hedef zekâ: %s
                İkincil zekâ: %s
                Ebeveynin işareti: %s
                Ebeveynin yazdığı: %s
                """.formatted(
                activity.getTitle(),
                activity.getTargetIntelligence(),
                activity.getSecondaryIntelligence() == null ? "yok" : activity.getSecondaryIntelligence(),
                turkishLabel(feedbackType),
                freeText);
    }

    private static String turkishLabel(FeedbackType type) {
        return switch (type) {
            case LIKED -> "SEVDI";
            case STRUGGLED -> "ZORLANDI";
            case DISLIKED -> "OLMADI";
        };
    }
}
