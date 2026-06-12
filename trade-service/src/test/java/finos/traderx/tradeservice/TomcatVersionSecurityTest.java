package finos.traderx.tradeservice;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

/**
 * Verifies the embedded Apache Tomcat runtime is patched against CVE-2026-43512
 * (digest authentication bypass, fixed in 10.1.55). The vulnerable range is
 * 10.1.0-M1 through 10.1.54, so the bundled server number must be at least
 * 10.1.55.
 */
class TomcatVersionSecurityTest {

    private static final int[] MIN_PATCHED = {10, 1, 55};

    @Test
    void embeddedTomcatIsAtLeastPatchedVersion() {
        String serverNumber = ServerInfo.getServerNumber();
        assertTrue(
                isAtLeast(serverNumber, MIN_PATCHED),
                "Embedded Tomcat " + serverNumber
                        + " is affected by CVE-2026-43512; expected >= 10.1.55");
    }

    private static boolean isAtLeast(String version, int[] minimum) {
        String[] rawParts = version.split("\\.");
        for (int i = 0; i < minimum.length; i++) {
            int part = i < rawParts.length ? parseLeadingInt(rawParts[i]) : 0;
            if (part != minimum[i]) {
                return part > minimum[i];
            }
        }
        return true;
    }

    private static int parseLeadingInt(String token) {
        int end = 0;
        while (end < token.length() && Character.isDigit(token.charAt(end))) {
            end++;
        }
        return end == 0 ? 0 : Integer.parseInt(token.substring(0, end));
    }
}
