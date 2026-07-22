import React from 'react';
import { render, screen, fireEvent, within } from '@testing-library/react';
import { Dropdown } from './Dropdown';

interface Item {
  id: number;
  label: string;
}

const items: Item[] = [
  { id: 1, label: 'Alpha' },
  { id: 2, label: 'Bravo' },
  { id: 3, label: 'Charlie' },
];

describe('Dropdown', () => {
  it('shows the placeholder when nothing is selected', () => {
    render(<Dropdown<Item> items={items} itemKey="label" onSelect={jest.fn()} />);
    expect(screen.getByRole('button')).toHaveTextContent('Please select an item');
  });

  it('shows a custom placeholder', () => {
    render(
      <Dropdown<Item>
        items={items}
        itemKey="label"
        placeholder="Pick one"
        onSelect={jest.fn()}
      />
    );
    expect(screen.getByRole('button')).toHaveTextContent('Pick one');
  });

  it('shows the selected item label on the toggle', () => {
    render(
      <Dropdown<Item>
        items={items}
        itemKey="label"
        selectedItem={items[1]}
        onSelect={jest.fn()}
      />
    );
    expect(screen.getByRole('button')).toHaveTextContent('Bravo');
  });

  it('opens the menu and lists every item label', () => {
    render(<Dropdown<Item> items={items} itemKey="label" onSelect={jest.fn()} />);
    fireEvent.click(screen.getByRole('button'));
    const menuItems = screen.getAllByRole('menuitem');
    expect(menuItems).toHaveLength(3);
    expect(menuItems.map((el) => el.textContent)).toEqual(['Alpha', 'Bravo', 'Charlie']);
  });

  it('calls onSelect with the clicked item', () => {
    const onSelect = jest.fn();
    render(<Dropdown<Item> items={items} itemKey="label" onSelect={onSelect} />);
    fireEvent.click(screen.getByRole('button'));
    fireEvent.click(screen.getByText('Charlie'));
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onSelect).toHaveBeenCalledWith(items[2]);
  });

  it('does not call onSelect when re-selecting the active item', () => {
    const onSelect = jest.fn();
    render(
      <Dropdown<Item>
        items={items}
        itemKey="label"
        selectedItem={items[0]}
        onSelect={onSelect}
      />
    );
    fireEvent.click(screen.getByRole('button'));
    fireEvent.click(within(screen.getByRole('menu')).getByText('Alpha'));
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('marks the selected menu item as active', () => {
    render(
      <Dropdown<Item>
        items={items}
        itemKey="label"
        selectedItem={items[1]}
        onSelect={jest.fn()}
      />
    );
    fireEvent.click(screen.getByRole('button'));
    const menu = screen.getByRole('menu');
    expect(within(menu).getByText('Bravo')).toHaveClass('active');
    expect(within(menu).getByText('Alpha')).not.toHaveClass('active');
  });

  it('uses a custom selectionComparator', () => {
    const onSelect = jest.fn();
    render(
      <Dropdown<Item>
        items={items}
        itemKey="label"
        selectedItem={{ id: 2, label: 'Bravo' }}
        selectionComparator={(a, b) => a.id === b.id}
        onSelect={onSelect}
      />
    );
    fireEvent.click(screen.getByRole('button'));
    const menu = screen.getByRole('menu');
    fireEvent.click(within(menu).getByText('Bravo'));
    expect(onSelect).not.toHaveBeenCalled();
    fireEvent.click(within(menu).getByText('Alpha'));
    expect(onSelect).toHaveBeenCalledWith(items[0]);
  });
});
