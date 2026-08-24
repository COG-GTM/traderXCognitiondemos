package finos.traderx.tradeservice.regulatory;

import java.util.List;

import finos.traderx.tradeservice.model.TradeOrder;

/**
 * The EMIR REFIT rule table applied to every trade order before it is published downstream.
 *
 * Rules are declarative rows so that a regime rewrite is a change to this table rather than a
 * change to control flow spread across the services. Each rule is narrow: a rule stays compliant
 * when its precondition is absent, so a missing value raises exactly one rejection code.
 */
public final class RegulatoryRuleSet {

    public static final String EMIR_REFIT = "EMIR_REFIT";

    private static final List<RegulatoryRule> RULES = List.of(
            new RegulatoryRule(RejectionCode.ACCOUNT_MISSING, "accountId",
                    "Reporting counterparty account is mandatory on a reportable trade",
                    order -> order.getAccountId() != null),
            new RegulatoryRule(RejectionCode.SECURITY_MISSING, "security",
                    "Instrument identifier is mandatory on a reportable trade",
                    order -> hasText(order.getSecurity())),
            new RegulatoryRule(RejectionCode.SIDE_MISSING, "side",
                    "Trade side is mandatory on a reportable trade",
                    order -> order.getSide() != null),
            new RegulatoryRule(RejectionCode.QUANTITY_INVALID, "quantity",
                    "Quantity is mandatory and must be greater than zero",
                    order -> order.getQuantity() != null && order.getQuantity() > 0),
            new RegulatoryRule(RejectionCode.UTI_MISSING, "uti",
                    "A Unique Transaction Identifier must be present on every reportable trade",
                    order -> hasText(order.getUti())),
            new RegulatoryRule(RejectionCode.UTI_LENGTH_INVALID, "uti",
                    "UTI must be exactly " + UniqueTransactionIdentifiers.LENGTH + " characters",
                    order -> !hasText(order.getUti())
                            || UniqueTransactionIdentifiers.hasValidLength(order.getUti())),
            new RegulatoryRule(RejectionCode.UTI_FORMAT_INVALID, "uti",
                    "UTI must contain upper case alphanumeric characters only",
                    order -> !UniqueTransactionIdentifiers.hasValidLength(order.getUti())
                            || UniqueTransactionIdentifiers.hasValidStructure(order.getUti())),
            new RegulatoryRule(RejectionCode.UTI_PREFIX_MISMATCH, "uti",
                    "UTI must be prefixed with the LEI of the generating entity",
                    order -> !UniqueTransactionIdentifiers.hasValidStructure(order.getUti())
                            || !hasText(order.getReportingCounterpartyLei())
                            || order.getReportingCounterpartyLei()
                                    .equals(UniqueTransactionIdentifiers.prefixOf(order.getUti()))),
            new RegulatoryRule(RejectionCode.LEI_MISSING, "reportingCounterpartyLei",
                    "An LEI must be present for the reporting counterparty",
                    order -> hasText(order.getReportingCounterpartyLei())),
            new RegulatoryRule(RejectionCode.LEI_LENGTH_INVALID, "reportingCounterpartyLei",
                    "LEI must be exactly " + LegalEntityIdentifiers.LENGTH + " characters",
                    order -> !hasText(order.getReportingCounterpartyLei())
                            || LegalEntityIdentifiers.hasValidLength(order.getReportingCounterpartyLei())),
            new RegulatoryRule(RejectionCode.LEI_FORMAT_INVALID, "reportingCounterpartyLei",
                    "LEI must be 18 upper case alphanumeric characters followed by 2 check digits",
                    order -> !LegalEntityIdentifiers.hasValidLength(order.getReportingCounterpartyLei())
                            || LegalEntityIdentifiers.hasValidStructure(order.getReportingCounterpartyLei())),
            new RegulatoryRule(RejectionCode.LEI_CHECKSUM_INVALID, "reportingCounterpartyLei",
                    "LEI check digits must satisfy the ISO 7064 MOD 97-10 checksum",
                    order -> !LegalEntityIdentifiers.hasValidStructure(order.getReportingCounterpartyLei())
                            || LegalEntityIdentifiers.hasValidChecksum(order.getReportingCounterpartyLei())),
            new RegulatoryRule(RejectionCode.REGIME_MISSING, "reportingRegime",
                    "The reporting regime must be stamped on every reportable trade",
                    order -> hasText(order.getReportingRegime())),
            new RegulatoryRule(RejectionCode.REGIME_UNSUPPORTED, "reportingRegime",
                    "Only " + EMIR_REFIT + " reporting is supported by this service",
                    order -> !hasText(order.getReportingRegime())
                            || EMIR_REFIT.equals(order.getReportingRegime())));

    private RegulatoryRuleSet() {
    }

    public static List<RegulatoryRule> rules() {
        return RULES;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
