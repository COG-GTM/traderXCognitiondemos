import React, { useEffect, useRef, useState } from 'react';

// SECTION 2 — Reusable dropdown (replaces Angular `app-ngx-dropdown`).
// Consumed by the Trade page (account picker) and Assign User form.
// Do not change this prop contract without coordinating with those sections.
export interface DropdownProps<T> {
  items: T[];
  itemKey: keyof T & string;
  selectedItem?: T;
  placeholder?: string;
  selectionComparator?: (a: T, b: T) => boolean;
  onSelect: (item: T) => void;
}

const defaultComparator = <T,>(a: T, b: T): boolean => a === b;

export function Dropdown<T>(props: DropdownProps<T>) {
  const {
    items,
    itemKey,
    selectedItem,
    placeholder = 'Please select an item',
    selectionComparator = defaultComparator,
    onSelect,
  } = props;

  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  const isSelected = (item: T): boolean =>
    selectedItem != null && selectionComparator(selectedItem, item);

  const handleItemClick = (item: T) => {
    if (!isSelected(item)) {
      onSelect(item);
    }
    setOpen(false);
  };

  const toggleLabel =
    selectedItem != null ? String(selectedItem[itemKey]) : placeholder;

  return (
    <div className={`btn-group${open ? ' open show' : ''}`} ref={containerRef}>
      <button
        type="button"
        className="btn btn-sm btn-primary dropdown-toggle"
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
      >
        {toggleLabel} <span className="caret"></span>
      </button>
      <ul
        className={`dropdown-menu${open ? ' show' : ''}`}
        role="menu"
      >
        {items.map((item, index) => (
          <li key={index} role="menuitem" onClick={() => handleItemClick(item)}>
            <a className={`dropdown-item${isSelected(item) ? ' active' : ''}`}>
              {String(item[itemKey])}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
