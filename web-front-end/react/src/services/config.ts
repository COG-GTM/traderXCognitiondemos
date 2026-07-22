// Backend service base URLs, computed from window.location.hostname (mirrors
// src/env.ts and web-front-end/angular/main/environments/environment.ts).
// Kept local so this service layer is self-contained.
//
// NOTE: the people service uses :18089 per the Angular environment (source of
// truth); the pre-existing src/env.ts lists :18095, which is incorrect.

const hostname = (): string =>
	typeof window !== 'undefined' && window.location ? window.location.hostname : 'localhost';

export const ServiceUrls = {
	get accountService(): string {
		return `http://${hostname()}:18088`;
	},
	get referenceData(): string {
		return `http://${hostname()}:18085`;
	},
	get tradeService(): string {
		return `http://${hostname()}:18092`;
	},
	get positionService(): string {
		return `http://${hostname()}:18090`;
	},
	get peopleService(): string {
		return `http://${hostname()}:18089`;
	},
	get tradeFeed(): string {
		return `http://${hostname()}:18086`;
	},
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// Small fetch wrapper: sets JSON headers, checks response.ok, logs + rethrows
// on error (matching the Angular services' console.error + throwError).
export async function httpJson<T>(url: string, init?: RequestInit): Promise<T> {
	try {
		const response = await fetch(url, {
			...init,
			headers: { ...JSON_HEADERS, ...(init?.headers ?? {}) },
		});
		if (!response.ok) {
			throw new Error(`Request to ${url} failed with status ${response.status}`);
		}
		const text = await response.text();
		return (text ? JSON.parse(text) : undefined) as T;
	} catch (error) {
		console.error(error);
		throw error;
	}
}
