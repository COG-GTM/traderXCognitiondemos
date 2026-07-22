// Ported from web-front-end/angular/main/app/service/user.service.ts
// People service (:18089). Maps the `{ people }` response to User[].
import { ServiceUrls, httpJson } from './config';
import { User } from './types';

export async function getUsers(searchText: string): Promise<User[]> {
	const params = new URLSearchParams({ SearchText: searchText, Take: '10' });
	const response = await httpJson<{ people: User[] }>(
		`${ServiceUrls.peopleService}/People/GetMatchingPeople?${params.toString()}`,
	);
	return response?.people ?? [];
}
