import { render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import type { Position } from '../../models/trade.model';
import PositionBlotter from './PositionBlotter';

const positions: Position[] = [
  { accountid: 1, quantity: 100, security: 'AAPL', updated: new Date() },
  { accountid: 1, quantity: 250, security: 'MSFT', updated: new Date() }
];

vi.mock('../../services/position.service', () => ({
  getPositions: vi.fn(() => Promise.resolve(positions))
}));

vi.mock('../../services/trade-feed.service', () => ({
  subscribe: vi.fn(() => () => {})
}));

describe('PositionBlotter', () => {
  it('should create', () => {
    const { container } = render(<PositionBlotter />);
    expect(container).toBeInTheDocument();
    expect(screen.getByText('Positions')).toBeInTheDocument();
  });

  it('should show given positions in the grid', async () => {
    const { container } = render(<PositionBlotter account={{ id: 1, displayName: 'Test Account' }} />);

    await waitFor(() => {
      expect(container.querySelectorAll('.ag-center-cols-container .ag-row').length).toEqual(2);
    });

    const columns = container.querySelectorAll('.ag-header-cell');
    expect(columns.length).toEqual(2);

    const rows = container.querySelectorAll('.ag-center-cols-container .ag-row');
    const firstRow = rows[0];
    expect(firstRow.children[0].textContent).toEqual(positions[0].security);
    expect(firstRow.children[1].textContent).toEqual(positions[0].quantity.toString());
  });
});
