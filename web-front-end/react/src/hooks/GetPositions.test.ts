import { fetchAllPositions, V2_POSITIONS_MAX_LIMIT } from './GetPositions';
import { Position, PositionPage } from '../Datatable/types';
import { loadFixture, ReportExpectations, V2Page } from '../report/fixtures';

/**
 * Adapter tests for the v2 cursor walk. A simulated v2 server pages the approved fixture rows at ANY
 * limit so the adapter's result can be checked to be identical for every limit in 1..100.
 */
const expectations = loadFixture<ReportExpectations>('report-expectations.json');
const recordedPages = loadFixture<V2Page[]>('v2-pages-77007.json');

const allRows: Position[] = recordedPages.flatMap((p) => p.response.items);

const parseUrl = (input: RequestInfo | URL) => new URL(String(input));

const mockFetch = (handler: (url: URL) => PositionPage | undefined) => {
	global.fetch = jest.fn(async (input: RequestInfo | URL) => {
		const body = handler(parseUrl(input));
		if (body === undefined) {
			return { ok: false, status: 400, json: async () => ({ error: 'bad request' }) } as Response;
		}
		return { ok: true, status: 200, json: async () => body } as Response;
	}) as unknown as typeof fetch;
	return global.fetch as jest.Mock;
};

/** Opaque cursor: the server encodes the offset, the client must never interpret it. */
const encodeCursor = (offset: number) => `opaque:${Buffer.from(String(offset)).toString('base64')}/+=`;
const decodeCursor = (cursor: string) => Number(Buffer.from(cursor.replace(/^opaque:/, '').replace(/\/\+=$/, ''), 'base64').toString());

/** Simulated v2 server over the approved rows: pages at the requested limit, null cursor only after the last row. */
const simulatedServer = (rows: Position[], defaultLimit = 10) => (url: URL): PositionPage | undefined => {
	if (url.pathname !== '/v2/positions') return undefined;
	if (Number(url.searchParams.get('accountId')) !== expectations.accountId) return { items: [], nextCursor: null };
	const limitParam = url.searchParams.get('limit');
	const limit = limitParam === null ? defaultLimit : Number(limitParam);
	if (!Number.isInteger(limit) || limit < 1 || limit > V2_POSITIONS_MAX_LIMIT) return undefined;
	const cursorParam = url.searchParams.get('cursor');
	const offset = cursorParam === null ? 0 : decodeCursor(cursorParam);
	if (Number.isNaN(offset)) return undefined;
	const items = rows.slice(offset, offset + limit);
	const next = offset + limit;
	return { items, nextCursor: next < rows.length ? encodeCursor(next) : null };
};

const recordedServer = (url: URL): PositionPage | undefined => {
	if (url.pathname !== '/v2/positions') return undefined;
	const cursor = url.searchParams.get('cursor');
	const page = recordedPages.find((p) => p.request.cursor === cursor);
	return page?.response;
};

describe('GetPositions adapter: v2 cursor walk', () => {
	afterEach(() => jest.restoreAllMocks());

	test('calls only /v2/positions with accountId and never the deprecated v1 path', async () => {
		const fetchMock = mockFetch(recordedServer);
		await fetchAllPositions(expectations.accountId);
		const urls = fetchMock.mock.calls.map(([u]) => parseUrl(u));
		expect(urls.length).toBeGreaterThan(0);
		urls.forEach((u) => {
			expect(u.pathname).toBe('/v2/positions');
			expect(u.searchParams.get('accountId')).toBe(String(expectations.accountId));
		});
		expect(urls.some((u) => u.pathname.startsWith('/positions/'))).toBe(false);
	});

	test('walks the recorded pages (more than one) and follows each nextCursor string verbatim', async () => {
		const fetchMock = mockFetch(recordedServer);
		const positions = await fetchAllPositions(expectations.accountId);
		expect(recordedPages.length).toBeGreaterThan(1);
		expect(fetchMock).toHaveBeenCalledTimes(recordedPages.length);
		const sentCursors = fetchMock.mock.calls.map(([u]) => parseUrl(u).searchParams.get('cursor'));
		expect(sentCursors).toEqual(recordedPages.map((p) => p.request.cursor));
		expect(sentCursors.slice(1).every((c) => typeof c === 'string' && c.length > 0)).toBe(true);
		expect(positions.map((p) => p.security)).toEqual(expectations.rows.map((r) => r.security));
	});

	test('returns every expected position exactly once, unchanged, for every limit in 1..100', async () => {
		for (let limit = 1; limit <= V2_POSITIONS_MAX_LIMIT; limit++) {
			const fetchMock = mockFetch(simulatedServer(allRows));
			const positions = await fetchAllPositions(expectations.accountId, limit);
			expect(fetchMock).toHaveBeenCalledTimes(Math.ceil(allRows.length / limit));
			expect(positions).toEqual(allRows);
			expect(new Set(positions.map((p) => p.security)).size).toBe(expectations.rows.length);
		}
	});

	test('URL-encodes the opaque cursor and echoes it back exactly', async () => {
		const fetchMock = mockFetch(simulatedServer(allRows));
		await fetchAllPositions(expectations.accountId, 7);
		const raw = String(fetchMock.mock.calls[1][0]);
		expect(raw).toContain('cursor=opaque%3A');
		expect(raw).toContain('%2F%2B%3D');
		expect(parseUrl(raw).searchParams.get('cursor')).toBe(encodeCursor(7));
	});

	test('a short or empty page is not terminal; only a null nextCursor stops the walk', async () => {
		const pages: Record<string, PositionPage> = {
			first: { items: allRows.slice(0, 2), nextCursor: 'c1' },
			c1: { items: [], nextCursor: 'c2' },
			c2: { items: allRows.slice(2, 3), nextCursor: 'c3' },
			c3: { items: allRows.slice(3, 13), nextCursor: null }, // full-size final page
		};
		const fetchMock = mockFetch((url) => pages[url.searchParams.get('cursor') ?? 'first']);
		const positions = await fetchAllPositions(expectations.accountId, 10);
		expect(fetchMock).toHaveBeenCalledTimes(4);
		expect(positions).toEqual(allRows.slice(0, 13));
	});

	test('preserves field meaning: ISO-8601 UTC asOf, string amounts, null unknowns, currency on every row', async () => {
		mockFetch(recordedServer);
		const positions = await fetchAllPositions(expectations.accountId);
		positions.forEach((p, i) => {
			const r = expectations.rows.find((x) => x.security === p.security)!;
			expect(p.asOf).toBe(r.asOf);
			expect(p.asOf).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/);
			expect(p.currency).toBe(r.currency);
			if (r.marketValue === null) {
				expect(p.marketValue).toBeNull();
			} else {
				expect(typeof p.marketValue!.amount).toBe('string');
				expect(p.marketValue!.amount).toBe(r.marketValue);
				expect(p.marketValue!.currency).toBe(p.currency);
			}
			expect(typeof i).toBe('number');
		});
		expect(positions.filter((p) => p.marketValue === null).map((p) => p.security).sort()).toEqual(
			[...expectations.unknownMarketValueSecurities].sort()
		);
	});

	test('rejects an out-of-range limit before calling the service and surfaces HTTP errors', async () => {
		const fetchMock = mockFetch(simulatedServer(allRows));
		await expect(fetchAllPositions(expectations.accountId, 0)).rejects.toThrow(/limit/);
		await expect(fetchAllPositions(expectations.accountId, 101)).rejects.toThrow(/limit/);
		expect(fetchMock).not.toHaveBeenCalled();
		mockFetch(() => undefined);
		await expect(fetchAllPositions(expectations.accountId)).rejects.toThrow(/400/);
	});
});
