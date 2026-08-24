package finos.traderx.tradeservice.regulatory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import finos.traderx.tradeservice.model.TradeOrder;

/**
 * Regression harness for the EMIR REFIT rule table.
 *
 * Cases live in golden/emir-refit-cases.json and the rendered rejection report is pinned in
 * golden/emir-refit-rejection-report.txt. A regime change that alters a code, a field name or a
 * rule description shows up as a diff on the golden report rather than as a silent behaviour
 * change downstream. Run with -Dgolden.update=true to rewrite the report after an intended change.
 */
class RegulatoryRuleSetGoldenFileTest {

    private static final String CASES = "golden/emir-refit-cases.json";
    private static final String REPORT = "golden/emir-refit-rejection-report.txt";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private final RegulatoryValidator validator = new RegulatoryValidator();

    record ValidationCase(String name, TradeOrder order, List<String> expectedCodes) {
        @Override
        public String toString() {
            return this.name;
        }
    }

    static Stream<ValidationCase> cases() throws IOException {
        try (InputStream in = RegulatoryRuleSetGoldenFileTest.class.getClassLoader().getResourceAsStream(CASES)) {
            JsonNode root = MAPPER.readTree(in);
            List<ValidationCase> cases = new ArrayList<>();
            for (JsonNode node : root) {
                TradeOrder order = MAPPER.treeToValue(node.get("order"), TradeOrder.class);
                List<String> expected = new ArrayList<>();
                node.get("expectedCodes").forEach(code -> expected.add(code.asText()));
                cases.add(new ValidationCase(node.get("name").asText(), order, expected));
            }
            return cases.stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void rejectionCodesMatchTheRuleTable(ValidationCase testCase) {
        ValidationResult result = this.validator.validate(testCase.order());
        assertEquals(testCase.expectedCodes(), result.getRejectionCodes(),
                "Rejection codes changed for case " + testCase.name());
        assertEquals(testCase.expectedCodes().isEmpty(), result.isValid());
    }

    @Test
    void renderedRejectionReportMatchesGoldenFile() throws IOException {
        StringBuilder actual = new StringBuilder();
        cases().forEach(testCase -> {
            actual.append("case: ").append(testCase.name()).append('\n');
            List<RejectionReason> rejections = this.validator.validate(testCase.order()).getRejections();
            if (rejections.isEmpty()) {
                actual.append("  ACCEPTED\n");
            } else {
                rejections.forEach(rejection -> actual.append("  ").append(rejection).append('\n'));
            }
        });

        if (Boolean.getBoolean("golden.update")) {
            Files.writeString(Path.of("src/test/resources", REPORT), actual.toString(), StandardCharsets.UTF_8);
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(REPORT)) {
            String golden = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(golden, actual.toString(), "Rendered rejection report drifted from the golden file");
        }
    }
}
