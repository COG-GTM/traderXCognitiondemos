import {
	Alert,
	Box,
	Button,
	MenuItem,
	Modal,
	TextField,
	ToggleButton,
	ToggleButtonGroup,
} from '@mui/material';
import {
	ChangeEvent,
	MouseEvent,
	useCallback,
	useEffect,
	useState,
} from 'react';
import { createTrade, fetchStocks } from './api';
import { Side, Stock, TradeTicket as TradeTicketModel } from './types';

const modalStyle = {
	position: 'absolute' as const,
	top: '50%',
	left: '50%',
	transform: 'translate(-50%, -50%)',
	width: 420,
	bgcolor: 'background.paper',
	border: '2px solid #000',
	boxShadow: 24,
	p: 4,
};

export interface TradeTicketProps {
	/** Account the trade will be booked against. */
	accountId: number;
	/** Optional read-only display name for the account. */
	accountName?: string;
	/**
	 * Controls modal visibility. When omitted the ticket renders inline
	 * (no Modal wrapper) — useful for embedding directly in a page.
	 */
	open?: boolean;
	/** Called when the user closes/cancels the ticket. */
	onClose?: () => void;
	/** Called after a trade is successfully created. */
	onCreated?: (ticket: TradeTicketModel) => void;
}

export const TradeTicket = ({
	accountId,
	accountName,
	open,
	onClose,
	onCreated,
}: TradeTicketProps) => {
	const isModal = open !== undefined;
	const visible = !isModal || open;

	const [stocks, setStocks] = useState<Stock[]>([]);
	const [security, setSecurity] = useState<string>('');
	const [quantity, setQuantity] = useState<number>(0);
	const [side, setSide] = useState<Side>('Buy');
	const [success, setSuccess] = useState<boolean>(false);
	const [error, setError] = useState<string>('');

	useEffect(() => {
		if (!visible) return;
		let active = true;
		fetchStocks()
			.then((data) => {
				if (active) setStocks(data);
			})
			.catch((e) => {
				if (active) setError(e instanceof Error ? e.message : String(e));
			});
		return () => {
			active = false;
		};
	}, [visible]);

	const handleSecurityChange = useCallback(
		(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
			setSecurity(event.target.value);
		},
		[]
	);

	const handleQuantityChange = useCallback(
		(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
			const value = parseInt(event.target.value, 10);
			setQuantity(Number.isNaN(value) ? 0 : value);
		},
		[]
	);

	const handleSideChange = useCallback(
		(_event: MouseEvent<HTMLElement>, newSide: Side | null) => {
			if (newSide) setSide(newSide);
		},
		[]
	);

	const handleCreate = useCallback(async () => {
		setError('');
		if (!security || !quantity) {
			setError('Either security is not selected or quantity is not set!');
			return;
		}
		const ticket: TradeTicketModel = { side, quantity, security, accountId };
		try {
			await createTrade(ticket);
			setSuccess(true);
			onCreated?.(ticket);
		} catch (e) {
			setError(e instanceof Error ? e.message : String(e));
		}
	}, [security, quantity, side, accountId, onCreated]);

	const handleClose = useCallback(() => {
		setSuccess(false);
		setError('');
		onClose?.();
	}, [onClose]);

	const form = (
		<Box sx={isModal ? modalStyle : { p: 2, maxWidth: 420 }}>
			<h3 style={{ marginTop: 0 }}>New Trade</h3>
			{accountName !== undefined && (
				<TextField
					label="Account"
					value={accountName}
					InputProps={{ readOnly: true }}
					disabled
					fullWidth
					margin="dense"
				/>
			)}
			<TextField
				select
				label="Security"
				value={security}
				onChange={handleSecurityChange}
				fullWidth
				margin="dense"
			>
				{stocks.map((stock) => (
					<MenuItem key={stock.ticker} value={stock.ticker}>
						{stock.companyName} ({stock.ticker})
					</MenuItem>
				))}
			</TextField>
			<TextField
				type="number"
				label="Quantity"
				value={quantity}
				onChange={handleQuantityChange}
				fullWidth
				margin="dense"
			/>
			<Box sx={{ my: 1 }}>
				<ToggleButtonGroup
					color="primary"
					size="medium"
					value={side}
					exclusive
					onChange={handleSideChange}
					aria-label="tradeSide"
				>
					<ToggleButton value="Buy">Buy</ToggleButton>
					<ToggleButton value="Sell">Sell</ToggleButton>
				</ToggleButtonGroup>
			</Box>
			<Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
				<Button
					variant="contained"
					color="primary"
					id="createButton"
					onClick={handleCreate}
				>
					Create
				</Button>
				<Button
					variant="outlined"
					color="secondary"
					id="cancelButton"
					onClick={handleClose}
				>
					Close
				</Button>
			</Box>
			{success && (
				<Alert severity="success" sx={{ mt: 2 }}>
					Trade created!
				</Alert>
			)}
			{error && (
				<Alert severity="error" sx={{ mt: 2 }}>
					{error}
				</Alert>
			)}
		</Box>
	);

	if (!isModal) {
		return form;
	}

	return (
		<Modal
			open={!!open}
			onClose={handleClose}
			aria-labelledby="trade-ticket-title"
		>
			{form}
		</Modal>
	);
};

export default TradeTicket;
