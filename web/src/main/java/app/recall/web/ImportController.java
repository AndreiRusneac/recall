package app.recall.web;

import app.recall.core.AppointmentCsv;
import app.recall.persistence.AppointmentImporter;
import app.recall.persistence.BusinessEntity;
import app.recall.persistence.BusinessRepository;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** Upload a CSV file of appointments. The parsing rules live in core, the saving in persistence. */
@Controller
public class ImportController {

    private static final Charset WINDOWS_1250 = Charset.forName("windows-1250");

    private final BusinessRepository businesses;
    private final AppointmentImporter importer;

    public ImportController(BusinessRepository businesses, AppointmentImporter importer) {
        this.businesses = businesses;
        this.importer = importer;
    }

    @GetMapping("/import")
    public String form() {
        return "import";
    }

    @PostMapping("/import")
    public String upload(
            @AuthenticationPrincipal RecallUser user,
            @RequestParam("file") MultipartFile file,
            Model model) throws IOException {
        BusinessEntity business = businesses.findById(user.getBusinessId()).orElseThrow();

        AppointmentCsv.Result parsed = AppointmentCsv.parse(decode(file.getBytes()), ZoneId.of(business.getTimezone()));
        AppointmentImporter.Summary summary = importer.importRows(business, parsed.rows());

        List<ErrorLine> errors = parsed.errors().stream()
                .map(error -> new ErrorLine(error.line(), describe(error)))
                .toList();
        model.addAttribute("summary", summary);
        model.addAttribute("errors", errors);
        return "import";
    }

    /**
     * Excel in Romania often saves CSV as Windows-1250 instead of UTF-8. We try UTF-8 strictly first,
     * so a good UTF-8 file is never misread, and fall back to Windows-1250 so diacritics survive.
     */
    static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, WINDOWS_1250);
        }
    }

    /** The text the owner reads for each kind of problem in the file. */
    static String describe(AppointmentCsv.RowError error) {
        return switch (error.problem()) {
            case MISSING_COLUMN -> "Lipsește coloana „" + error.detail() + "” din primul rând al fișierului.";
            case MISSING_NAME -> "Numele lipsește.";
            case BAD_PHONE -> "Număr de telefon invalid: „" + error.detail() + "”.";
            case BAD_DATE -> "Dată invalidă: „" + error.detail() + "”. Folosiți 24.11.2026 10:00 sau 2026-11-24 10:00.";
            case BAD_CONSENT -> "Acord neclar: „" + error.detail() + "”. Folosiți da, nu sau lăsați gol.";
            case TOO_MANY_ROWS -> "Fișierul are prea multe rânduri. Maximum " + error.detail() + " pe încărcare.";
        };
    }

    /** One problem row, ready to show. */
    public record ErrorLine(int line, String message) {
    }
}
