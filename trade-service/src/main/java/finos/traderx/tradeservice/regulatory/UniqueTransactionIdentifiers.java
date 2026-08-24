package finos.traderx.tradeservice.regulatory;

import java.util.regex.Pattern;

/**
 * Structural checks for the ISO 23897 Unique Transaction Identifier (UTI) as required by EMIR REFIT:
 * a 20 character LEI prefix of the generating entity followed by an upper case alphanumeric
 * transaction value, 52 characters in total.
 */
public final class UniqueTransactionIdentifiers {

    public static final int LENGTH = 52;

    private static final Pattern STRUCTURE = Pattern.compile("^[A-Z0-9]{52}$");

    private UniqueTransactionIdentifiers() {
    }

    public static boolean hasValidLength(String uti) {
        return uti != null && uti.length() == LENGTH;
    }

    public static boolean hasValidStructure(String uti) {
        return uti != null && STRUCTURE.matcher(uti).matches();
    }

    public static String prefixOf(String uti) {
        if (uti == null || uti.length() < LegalEntityIdentifiers.LENGTH) {
            return null;
        }
        return uti.substring(0, LegalEntityIdentifiers.LENGTH);
    }
}
