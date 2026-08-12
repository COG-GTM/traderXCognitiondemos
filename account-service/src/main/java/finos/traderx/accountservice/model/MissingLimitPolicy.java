package finos.traderx.accountservice.model;

/**
 * What a caller should do when an account has no risk limit on file.
 *
 * UNLIMITED - the account is not limit-controlled; the order may proceed.
 * REJECT    - absence of a limit is itself a reason to reject (fail-closed).
 */
public enum MissingLimitPolicy {
	UNLIMITED,
	REJECT
}
