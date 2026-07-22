import React from 'react';

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

// Placeholder: replace with a Bootstrap dropdown listing `items[itemKey]`,
// highlighting the selected item and emitting `onSelect` on click.
export function Dropdown<T>(_props: DropdownProps<T>) {
  return <div data-testid="dropdown-placeholder" />;
}
