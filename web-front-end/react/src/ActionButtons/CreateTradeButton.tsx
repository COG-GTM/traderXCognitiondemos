import { Box, Button,  MenuItem,  Modal, TextField, ToggleButton, ToggleButtonGroup } from "@mui/material"
import { ChangeEvent, MouseEvent, useCallback, useState } from "react";
import { style } from "../style";
import { ActionButtonsProps, Side } from "./types";
import { Environment } from '../env';

/**
 * CreateTradeButton Component
 * 
 * A React functional component that provides a modal dialog for creating new trades.
 * This component was migrated from the Angular TradeTicketComponent.
 * 
 * @description
 * Renders a button that opens a modal containing a trade creation form with:
 * - Security dropdown (populated from reference data service)
 * - Quantity input field
 * - Buy/Sell toggle buttons
 * - Submit button with success feedback
 * 
 * On modal open, fetches available securities from the reference data service.
 * On submission, sends a POST request to the trade service API to create the trade.
 * Displays a success message briefly before closing the modal.
 * 
 * @migration
 * - Angular Source: TradeTicketComponent in trade/trade-ticket/trade-ticket.component.ts
 * - Angular @Input() stocks replaced with internal state populated on modal open
 * - Angular @Input() account replaced with accountId prop
 * - Angular @Output() create EventEmitter replaced with direct API call
 * - Angular @Output() cancel EventEmitter replaced with modal close handler
 * - ngOnInit ticket initialization replaced with useState hooks
 * - Angular TypeaheadMatch replaced with MUI Select component
 * - Angular form binding replaced with controlled components and useCallback handlers
 * 
 * @param {ActionButtonsProps} props - Component props
 * @param {number} props.accountId - The ID of the account to create the trade for
 * 
 * @example
 * ```tsx
 * <CreateTradeButton accountId={12345} />
 * ```
 * 
 * @returns {JSX.Element} A button that opens a modal with a trade creation form
 */
export const CreateTradeButton = ({accountId}:ActionButtonsProps) => {
	const [refData, setRefData] = useState<any>([]);
	const tradeId = Math.floor(Math.random() * 1000000);

	const delay = (ms:number) => new Promise(
		resolve => setTimeout(resolve, ms)
	);
	
	const handleSubmit = async () => {
		try {
			const response = await fetch(`${Environment.trade_service_url}/trade/`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					id: `TRADE-${tradeId}`,
					security: security,
					quantity: quantity,
					accountId: accountId,
					side: side,
				}),
			});
			if (response.ok) {
				setTradeSuccess(true);
				await delay(2000);
				setTradeSuccess(false);
				setOpen(false);
				console.log('success');
				return;
			}
		} catch (error) {
			console.log(error);
			setError(error);
			return error;
		}
	}
	const [open, setOpen] = useState<boolean>(false);
	const [error, setError] = useState<any>('');
  const handleClose = () => setOpen(false);
	const handleOpen = async () => {
		setOpen(true);
		try {
			const response = await fetch(`${Environment.reference_data_url}/stocks`);
			const data = await response.json();
			setRefData(data)
		} catch (error) {
			return error
		}
	}

	const tickerItem = refData.map((option:any) => (
		<MenuItem key={option.ticker} value={option.ticker}>
			{option.ticker}
		</MenuItem>
	))

	const [side, setSide] = useState<Side>();
	const [security, setSecurity] = useState<string>('');
	const [quantity, setQuantity] = useState<number>(0);
	const [tradeSuccess, setTradeSuccess] = useState<boolean>(false);
	
  const handleToggleChange = useCallback((
    _event: MouseEvent<HTMLElement>,
		newSide: Side,
  ) => {
    setSide(newSide);
  }, []);

	const handleSecurityChange = useCallback(
		(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
			setSecurity(event.target.value);
	}, [])

	const handleQuantityChange = useCallback(
		(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
			setQuantity(parseInt(event.target.value));
	}, [])

	return (
		<div className="modal-container">
			<Button onClick={handleOpen} variant="contained">Create New Trade</Button>
				<Modal
					open={open}
					onClose={handleClose}
					aria-labelledby="modal-modal-title"
					aria-describedby="modal-modal-description"
				>
				<Box className="modal-components" sx={style}>
					<div className="form-container">
						<TextField
							select
							label="Security"
							variant="outlined"
							style={{width: "8em"}}
							onChange={handleSecurityChange}
						>
							{tickerItem}
						</TextField>
						<TextField
						type="number"
						style={{width: "6em"}}
						label="Quantity"
						onChange={handleQuantityChange}
						>

						</TextField>
						<ToggleButtonGroup
							color="primary"
							size="medium"
							style={{height: "3.5em"}}
							value={side}
							exclusive
							onChange={handleToggleChange}
							aria-label="tradeSide"
						>
							<ToggleButton value="Buy">Buy</ToggleButton>
							<ToggleButton value="Sell">Sell</ToggleButton>
						</ToggleButtonGroup>
						{!tradeSuccess && <div style={{float: 'left'}} className="submit-button-container">
							<Button variant="contained" color="success" onClick={handleSubmit}>Submit</Button>
						</div>}
						{tradeSuccess && <div style={{backgroundColor: "greenyellow", width: "5em"}}> Trade Created!</div>}
						<span style={{color: "red", width: "5em"}}>{error}</span>
					</div>
				</Box>
				</Modal>
		</div>
	)
}
