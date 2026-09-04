package club.escobar.util;

public final class CsvUtil {

    private CsvUtil() {
    }

    // Quotes a field and escapes embedded quotes for correct CSV encoding. Also guards against
    // formula injection: a cell value starting with =/+/-/@ is executed as a formula by Excel/Sheets
    // regardless of CSV quoting, so a leading apostrophe is inserted to force it to be read as text -
    // relevant wherever a field can be user-supplied (creator/campaign/business names, notes).
    public static String csvField(String value) {
        if (value == null) {
            return "\"\"";
        }
        String safe = value.matches("^[=+\\-@\\t\\r].*") ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
