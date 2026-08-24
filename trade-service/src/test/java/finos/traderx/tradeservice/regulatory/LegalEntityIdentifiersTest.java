package finos.traderx.tradeservice.regulatory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class LegalEntityIdentifiersTest {

    @ParameterizedTest
    @ValueSource(strings = { "549300TRADERX0ACC128", "7LTWFZYICNSX8D621K86", "213800QILIUD4ROSUO03" })
    void acceptsLeisWithValidStructureAndCheckDigits(String lei) {
        assertTrue(LegalEntityIdentifiers.hasValidStructure(lei));
        assertTrue(LegalEntityIdentifiers.hasValidChecksum(lei));
    }

    @ParameterizedTest
    @CsvSource({
            "549300TRADERX0ACC129, wrong check digits",
            "549300TRADERX0ACC1AB, check digits are not numeric",
            "549300traderx0acc128, lower case is not permitted",
            "549300TRADERX0ACC12, too short",
            "549300TRADERX0ACC1288, too long" })
    void rejectsInvalidLeis(String lei, String reason) {
        assertFalse(LegalEntityIdentifiers.hasValidChecksum(lei), reason);
    }

    @ParameterizedTest
    @NullSource
    void rejectsNull(String lei) {
        assertFalse(LegalEntityIdentifiers.hasValidLength(lei));
        assertFalse(LegalEntityIdentifiers.hasValidStructure(lei));
        assertFalse(LegalEntityIdentifiers.hasValidChecksum(lei));
    }
}
