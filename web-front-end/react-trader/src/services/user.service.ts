import { environment } from '../environments/environment';
import type { User } from '../models/user.model';
import { request } from './http';

const baseUrl = environment.peopleUrl;

export async function getUsers(searchText: string): Promise<User[]> {
  const response = await request<{ people: User[] }>(`${baseUrl}/People/GetMatchingPeople`, {
    params: { SearchText: searchText, Take: '10' }
  });
  return response.people || [];
}

export const userService = { getUsers };
export type UserService = typeof userService;
