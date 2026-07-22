import { useId, useState } from 'react';

interface DropdownProps<T> {
  items: T[];
  itemKey?: string;
  selectedItem?: T;
  selectionComparator?: (src: T | undefined, target: T) => boolean;
  onSelectedItemChange?: (item: T) => void;
  placeholder?: string;
}

const defaultComparator = (src: unknown, target: unknown) => src === target;

function Dropdown<T extends Record<string, any>>({
  items,
  itemKey = 'label',
  selectedItem,
  selectionComparator = defaultComparator,
  onSelectedItemChange,
  placeholder = 'Please select an item'
}: DropdownProps<T>) {
  const uid = useId();
  const drpId = `drp${uid}`;
  const drpBtnId = `drpbtn${uid}`;
  const [open, setOpen] = useState(false);

  const onItemClick = (item: T) => {
    setOpen(false);
    if (!selectionComparator(selectedItem, item)) {
      onSelectedItemChange?.(item);
    }
  };

  return (
    <div className="btn-group">
      <button
        id={drpBtnId}
        type="button"
        className="btn btn-sm btn-primary dropdown-toggle"
        aria-controls={drpId}
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
      >
        {selectedItem?.[itemKey] || placeholder} <span className="caret"></span>
      </button>
      <ul
        id={drpId}
        className={`dropdown-menu${open ? ' show' : ''}`}
        role="menu"
        aria-labelledby={drpBtnId}
      >
        {items.map((item, index) => (
          <li role="menuitem" key={index} onClick={() => onItemClick(item)}>
            <a className={`dropdown-item${selectionComparator(selectedItem, item) ? ' active' : ''}`}>
              {item[itemKey]}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default Dropdown;
