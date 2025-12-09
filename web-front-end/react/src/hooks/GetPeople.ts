import { SetStateAction, useEffect, useState } from "react";
import { Environment } from '../env';

/**
 * GetPeople Custom Hook
 * 
 * A React custom hook that fetches and returns the list of people/users.
 * This hook was migrated from the Angular UserService.getUsers() method.
 * 
 * @description
 * Fetches people data from the people service API on component mount.
 * Returns an array of JSON objects representing available users in the system.
 * The fetch is performed once when the component mounts.
 * 
 * @migration
 * - Angular Source: UserService.getUsers() in service/user.service.ts
 * - Angular @Injectable service replaced with custom hook pattern
 * - RxJS Observable with map, tap, and catchError replaced with async/await and try/catch
 * - Angular HttpClient replaced with fetch API
 * - Angular dependency injection replaced with direct hook usage
 * 
 * @example
 * ```tsx
 * const people = GetPeople();
 * // people is JSON[]
 * ```
 * 
 * @returns {JSON[]} Array of people/user objects
 */
export const GetPeople = () => {
	const [people, setPeople] = useState<JSON[]>([]);
	type data = () => Promise<unknown>;
  useEffect(() => {
		let json:SetStateAction<JSON[]>;
    const loadPeople:data = async () => {
			try {
				const response = await fetch(`${Environment.people_service_url}/People/`);
				if (response.ok) {
					json = await response.json();
					setPeople(json);
				}
			} catch (error) {
				return error;
			}
    }
    loadPeople();
  }, []);
	return people;
}
