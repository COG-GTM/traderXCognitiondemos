import { environment } from '../config/environment';
import { User } from '../models';
import { getJson } from './http';

const baseUrl = environment.peopleUrl;

export const userService = {
  async getUsers(searchText: string): Promise<User[]> {
    const params = new URLSearchParams({ SearchText: searchText, Take: '10' });
    const response = await getJson<{ people: User[] }>(
      `${baseUrl}/People/GetMatchingPeople?${params.toString()}`
    );
    return response.people || [];
  },
};
