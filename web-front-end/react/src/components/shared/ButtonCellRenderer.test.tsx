import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ButtonCellRenderer, ButtonCellRendererProps } from './ButtonCellRenderer';

describe('ButtonCellRenderer', () => {
  it('renders an "Update" button and calls clicked with the row data', () => {
    const data = { id: 42, name: 'Acme' };
    const clicked = jest.fn();

    render(
      <ButtonCellRenderer
        {...({ data, clicked } as unknown as ButtonCellRendererProps)}
      />
    );

    const button = screen.getByRole('button', { name: 'Update' });
    expect(button).toHaveClass('btn', 'btn-sm', 'btn-info');

    fireEvent.click(button);

    expect(clicked).toHaveBeenCalledTimes(1);
    expect(clicked).toHaveBeenCalledWith(data);
  });
});
