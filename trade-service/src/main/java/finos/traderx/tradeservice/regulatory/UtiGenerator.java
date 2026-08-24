package finos.traderx.tradeservice.regulatory;

import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Generates ISO 23897 UTIs: the LEI of the generating entity followed by a 32 character
 * transaction value unique to that entity.
 */
@Service
public class UtiGenerator {

    public String generate(String generatingEntityLei) {
        String prefix = generatingEntityLei == null ? LeiRegistry.DEFAULT_LEI : generatingEntityLei;
        String transactionValue = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return prefix + transactionValue;
    }
}
