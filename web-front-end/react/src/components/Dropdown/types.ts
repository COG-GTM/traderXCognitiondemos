// Local, self-contained types for the generic Dropdown component.
// (Duplicated intentionally across pieces to keep this piece independent.)

export interface DropdownProps<T> {
	/** Items to render in the dropdown menu. */
	items: T[];
	/** Property of each item used as its display label. */
	itemKey: keyof T & string;
	/** Currently selected item (uncontrolled selection is also supported). */
	selectedItem?: T;
	/** Text shown when nothing is selected. */
	placeholder?: string;
	/** Emitted when the user selects a (different) item. */
	onSelectedItemChange?: (item: T) => void;
	/**
	 * Optional equality comparator mirroring the Angular `selectionComparator`.
	 * Defaults to strict reference/value equality.
	 */
	selectionComparator?: (src: T | undefined, target: T) => boolean;
}
