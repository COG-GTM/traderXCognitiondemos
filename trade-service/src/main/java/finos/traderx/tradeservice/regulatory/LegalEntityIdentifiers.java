package finos.traderx.tradeservice.regulatory;

import java.util.regex.Pattern;

/**
 * Structural checks for ISO 17442 Legal Entity Identifiers.
 */
public final class LegalEntityIdentifiers {

    public static final int LENGTH = 20;

    private static final Pattern STRUCTURE = Pattern.compile("^[A-Z0-9]{18}[0-9]{2}$");

    private LegalEntityIdentifiers() {
    }

    public static boolean hasValidLength(String lei) {
        return lei != null && lei.length() == LENGTH;
    }

    public static boolean hasValidStructure(String lei) {
        return lei != null && STRUCTURE.matcher(lei).matches();
    }

    /**
     * ISO 7064 MOD 97-10 check digit verification, as mandated for LEIs.
     */
    public static boolean hasValidChecksum(String lei) {
        if (!hasValidStructure(lei)) {
            return false;
        }
        int remainder = 0;
        for (int i = 0; i < lei.length(); i++) {
            char c = lei.charAt(i);
            int value = Character.isDigit(c) ? c - '0' : c - 'A' + 10;
            remainder = (value > 9) ? (remainder * 100 + value) % 97 : (remainder * 10 + value) % 97;
        }
        return remainder == 1;
    }
}
