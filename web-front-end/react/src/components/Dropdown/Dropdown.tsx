import { MouseEvent, useState } from 'react';
import Button from '@mui/material/Button';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import { DropdownProps } from './types';

/**
 * Generic single-select dropdown, a React port of the Angular
 * `app-ngx-dropdown` component (`dropdown.component.ts` / `.html`).
 *
 * Mirrors the Angular API: `items`, `itemKey`, `selectedItem`, `placeholder`,
 * `onSelectedItemChange` (was `selectedItemChange`), and an optional
 * `selectionComparator`. Only emits a change when the selection differs from
 * the current one, matching the Angular `onItemClick` behavior.
 */
export function Dropdown<T>({
	items,
	itemKey,
	selectedItem,
	placeholder = 'Please select an item',
	onSelectedItemChange,
	selectionComparator,
}: DropdownProps<T>) {
	const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
	const open = Boolean(anchorEl);

	const comparator =
		selectionComparator ?? ((src: T | undefined, target: T) => src === target);

	const handleOpen = (event: MouseEvent<HTMLButtonElement>) => {
		setAnchorEl(event.currentTarget);
	};

	const handleClose = () => setAnchorEl(null);

	const handleItemClick = (item: T) => {
		if (!comparator(selectedItem, item)) {
			onSelectedItemChange?.(item);
		}
		handleClose();
	};

	const label = selectedItem ? String(selectedItem[itemKey]) : placeholder;

	return (
		<>
			<Button
				variant="contained"
				size="small"
				onClick={handleOpen}
				aria-haspopup="true"
				aria-expanded={open ? 'true' : undefined}
			>
				{label} <span className="caret" style={{ marginLeft: 4 }}>▾</span>
			</Button>
			<Menu anchorEl={anchorEl} open={open} onClose={handleClose} role="menu">
				{items.map((item, index) => (
					<MenuItem
						key={index}
						role="menuitem"
						selected={comparator(selectedItem, item)}
						onClick={() => handleItemClick(item)}
					>
						{String(item[itemKey])}
					</MenuItem>
				))}
			</Menu>
		</>
	);
}
