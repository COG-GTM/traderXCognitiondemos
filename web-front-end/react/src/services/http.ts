const JSON_HEADERS = { 'Content-Type': 'application/json' };

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status} ${res.statusText}`);
    console.error(err);
    throw err;
  }
  // Some POST endpoints may return an empty body.
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export async function getJson<T>(url: string): Promise<T> {
  const res = await fetch(url, { headers: JSON_HEADERS });
  return handle<T>(res);
}

export async function postJson<T>(url: string, body: unknown): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  });
  return handle<T>(res);
}
