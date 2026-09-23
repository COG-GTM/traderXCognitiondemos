/** Decimal-safe money helpers: amounts are strings, arithmetic is BigInt in minor units. No floats. */

const ZERO_DECIMAL_CURRENCIES = new Set(['JPY', 'KRW']);

export const scaleFor = (currency: string): number => (ZERO_DECIMAL_CURRENCIES.has(currency) ? 0 : 2);

const AMOUNT_PATTERN = /^(-)?(\d+)(?:\.(\d+))?$/;

/** "1234.5" in USD -> 123450n. Rejects malformed input and more decimals than the currency allows. */
export const toMinorUnits = (amount: string, currency: string): bigint => {
	const match = AMOUNT_PATTERN.exec(amount.trim());
	if (!match) {
		throw new Error(`Invalid decimal amount "${amount}"`);
	}
	const scale = scaleFor(currency);
	const [, sign, whole, fraction = ''] = match;
	if (fraction.length > scale) {
		throw new Error(`Amount "${amount}" has more than ${scale} decimals for ${currency}`);
	}
	const minor = BigInt(whole + fraction.padEnd(scale, '0'));
	return sign ? -minor : minor;
};

/** 123450n in USD -> "1234.50"; 8460000n in JPY -> "8460000". */
export const fromMinorUnits = (minor: bigint, currency: string): string => {
	const scale = scaleFor(currency);
	const negative = minor < BigInt(0);
	const digits = (negative ? -minor : minor).toString().padStart(scale + 1, '0');
	const whole = digits.slice(0, digits.length - scale);
	const fraction = digits.slice(digits.length - scale);
	return `${negative ? '-' : ''}${whole}${scale > 0 ? `.${fraction}` : ''}`;
};

/** Sums amounts of ONE currency. Throws if a mixed-currency sum is attempted. */
export const sumAmounts = (amounts: ReadonlyArray<{ amount: string; currency: string }>, currency: string): string => {
	let total = BigInt(0);
	for (const m of amounts) {
		if (m.currency !== currency) {
			throw new Error(`Refusing to sum ${m.currency} into a ${currency} total`);
		}
		total += toMinorUnits(m.amount, currency);
	}
	return fromMinorUnits(total, currency);
};

/** Display with thousands separators, keeping the exact decimal string (no float round-trip). */
export const formatAmount = (amount: string): string => {
	const [whole, fraction] = amount.split('.');
	const sign = whole.startsWith('-') ? '-' : '';
	const grouped = whole.replace('-', '').replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	return `${sign}${grouped}${fraction !== undefined ? `.${fraction}` : ''}`;
};
