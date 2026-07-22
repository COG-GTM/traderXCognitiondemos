export class HttpError extends Error {
  constructor(public status: number, public statusText: string, public body?: unknown) {
    super(`HTTP ${status} ${statusText}`);
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  params?: Record<string, string>;
  retries?: number;
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, params, retries = 0 } = options;
  const fullUrl = params ? `${url}?${new URLSearchParams(params)}` : url;

  for (let attempt = 0; ; attempt++) {
    try {
      const response = await fetch(fullUrl, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: body !== undefined ? JSON.stringify(body) : undefined
      });
      if (!response.ok) {
        let errorBody: unknown;
        try {
          errorBody = await response.json();
        } catch {
          errorBody = undefined;
        }
        throw new HttpError(response.status, response.statusText, errorBody);
      }
      const text = await response.text();
      return (text ? JSON.parse(text) : undefined) as T;
    } catch (error) {
      if (attempt < retries) {
        continue;
      }
      console.error(error);
      throw error;
    }
  }
}
