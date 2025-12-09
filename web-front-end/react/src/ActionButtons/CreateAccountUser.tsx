import { Box, Button, Modal, } from "@mui/material"
import { FormEvent, SetStateAction, useCallback, useState } from "react";
import { RJSFSchema, } from '@rjsf/utils';
import validator from '@rjsf/validator-ajv8';
import Form, { IChangeEvent } from '@rjsf/core';
import { style } from "../style";
import { ActionButtonsProps, PeopleData } from "./types";
import { MatchingPeople } from "../AccountsDropdown";
import { Environment } from '../env';

/**
 * CreateAccountUser Component
 * 
 * A React functional component that provides a modal dialog for assigning users to trading accounts.
 * This component was migrated from the Angular AssignUserToAccountComponent.
 * 
 * @description
 * Renders a button that opens a modal containing a form for creating or updating account users.
 * The form includes a typeahead search for users that queries the People Service API.
 * On form change, it fetches matching people from the API to provide autocomplete suggestions.
 * On submission, it sends a POST request to the account service API to assign the user.
 * 
 * @migration
 * - Angular Source: AssignUserToAccountComponent in accounts/user/assign-user.component.ts
 * - Angular @Input() account replaced with accountId prop
 * - Angular @Output() update EventEmitter replaced with internal state management
 * - RxJS Observable pipe with switchMap replaced with useCallback and async/await
 * - Angular HttpClient replaced with fetch API
 * - ngOnInit Observable setup replaced with useCallback onChange handler
 * 
 * @param {ActionButtonsProps} props - Component props
 * @param {number} props.accountId - The ID of the account to assign users to
 * 
 * @example
 * ```tsx
 * <CreateAccountUser accountId={12345} />
 * ```
 * 
 * @returns {JSX.Element} A button that opens a modal with a user assignment form
 */
export const CreateAccountUser = (
	{accountId}:ActionButtonsProps
	) => {
	const [matchingPeople, setMatchingPeople] = useState<MatchingPeople[]>([]);
	const schema: RJSFSchema = {
		title: 'Create/Update Account Users',
		type: 'object',
		required: ['username, fullName'],
		properties: {
			fullName: { type: 'string', title: 'Full Name' },
			username: { type: 'string', title: 'Username' },
		},
	};
	const uiSchema = {
		"type": "VerticalLayout",
		"elements": [
			{
				"type": "Control",
				"scope": "#/properties/''",
				"options": {
					"ui:widget": "button",
					"autocomplete": true
				}
			}
		],
	}
	const [open, setOpen] = useState<boolean>(false);
  const handleClose = () => setOpen(false);
	const handleOpen = () => setOpen(true);
	const log = (type:string) => console.log.bind(console, type);

	const onSubmit = async (data: IChangeEvent<any>, _event: FormEvent<any>) => {
		const accountDetails = data.formData;
		try {
				await fetch(`${Environment.account_service_url}/accountuser/`, {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({
						id: accountId,
						displayName: accountDetails.displayName
					}),
				});
				setOpen(false);
				console.log('success');
		} catch (error) {
			return error
		}
	}

	const onChange = useCallback(async (data: IChangeEvent<any>) => {
		let json:PeopleData[];
		console.log(data.formData)
			try {
					const response = await fetch(`${Environment.people_service_url}/People/GetMatchingPeople?SearchText=${data.formData.fullName}`);
					json = await response.json();
					setMatchingPeople([]);
					json.forEach((data:any) => {
						setMatchingPeople((prevData:MatchingPeople[]) => [...prevData, data.fullName])
					})
			} catch (error) {
				return error;
			}
		}, [])
	return (
		<div className="button-modal-container">
			<Button onClick={handleOpen} variant="contained">Create Account User</Button>
				<Modal
					open={open}
					onClose={handleClose}
					aria-labelledby="modal-modal-title"
					aria-describedby="modal-modal-description"
				>
				<Box sx={style}>
					<Form
						schema={schema}
						uiSchema={uiSchema}
						validator={validator}
						onChange={onChange}
						onSubmit={onSubmit}
						onError={log('errors')}
					>
					</Form>
				</Box>
				</Modal>
		</div>
	)
}
