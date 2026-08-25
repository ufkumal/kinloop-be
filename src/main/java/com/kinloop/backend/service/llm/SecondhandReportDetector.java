package com.kinloop.backend.service.llm;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Code-side enforcement for v2 Fren 6; the model's self-reported confidence is not trusted. */
@Component
public class SecondhandReportDetector {
    private static final String SOURCE =
            "(?:babası|babasi|annesi|öğretmeni|ogretmeni|öğretmen|ogretmen|"
                    + "bakıcısı|bakicisi|dedesi|ninesi|ablası|ablasi|abisi)";
    private static final String REPORT_VERB =
            "(?:dedi|demiş|demis|söyledi|soyledi|anlattı|anlatti|aktardı|aktardi|diyor)";
    private static final Pattern THIRD_PARTY_REPORT = Pattern.compile(
            "(?iuU)" + SOURCE + ".{0,80}" + REPORT_VERB);
    private static final Pattern NOT_OBSERVED = Pattern.compile(
            "(?iuU)(?:ben\\s+)?(?:görmedim|gormedim|yanında\\s+değildim|yaninda\\s+degildim)");

    public boolean isSecondhand(String text) {
        if (text == null || text.isBlank()) return false;
        return THIRD_PARTY_REPORT.matcher(text).find() || NOT_OBSERVED.matcher(text).find();
    }
}
