import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import { PositionBlotter } from './PositionBlotter';
import { positionService } from '../../../services/positionService';
import { tradeFeed } from '../../../services/tradeFeed';
import { Position } from '../../../models';

jest.mock('../../../services/positionService', () => ({
  positionService: { getPositions: jest.fn() },
}));

jest.mock('../../../services/tradeFeed', () => ({
  tradeFeed: { subscribe: jest.fn() },
}));

// Lightweight stand-in for AgGridReact: exposes a grid api via onGridReady
// that mimics getRowNode/applyTransaction on top of the rowData prop, and
// renders each row so we can assert on its contents.
jest.mock('ag-grid-react', () => {
  const ReactLib = require('react');
  const AgGridReact = ({ rowData, getRowId, onGridReady }: any) => {
    const [rows, setRows] = ReactLib.useState([]);
    const rowsRef = ReactLib.useRef([]);

    ReactLib.useEffect(() => {
      rowsRef.current = rowData || [];
      setRows(rowData || []);
    }, [rowData]);

    ReactLib.useEffect(() => {
      const api = {
        getRowNode: (id: string) => {
          const found = rowsRef.current.find(
            (r: any) => getRowId({ data: r }) === id
          );
          return found ? { data: found } : undefined;
        },
        applyTransaction: (t: any) => {
          let next = [...rowsRef.current];
          if (t.add) {
            next = [...t.add, ...next];
          }
          rowsRef.current = next;
          setRows([...next]);
        },
      };
      onGridReady && onGridReady({ api });
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return ReactLib.createElement(
      'div',
      { 'data-testid': 'grid' },
      rows.map((r: any) =>
        ReactLib.createElement(
          'div',
          { key: getRowId({ data: r }), 'data-testid': `row-${r.security}` },
          `${r.security}:${r.quantity}`
        )
      )
    );
  };
  return { AgGridReact };
});

const getPositionsMock = positionService.getPositions as jest.Mock;
const subscribeMock = tradeFeed.subscribe as jest.Mock;

const account = { id: 42, displayName: 'Test Account' };

const initialPositions: Position[] = [
  {
    accountid: 42,
    quantity: 100,
    security: 'AAPL',
    updated: new Date('2024-01-01'),
  },
];

describe('PositionBlotter', () => {
  let feedCb: (data: Position) => void;
  let unsubscribe: jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    unsubscribe = jest.fn();
    subscribeMock.mockImplementation((_topic: string, cb: any) => {
      feedCb = cb;
      return unsubscribe;
    });
    getPositionsMock.mockResolvedValue(initialPositions);
  });

  it('hydrates the grid from positionService.getPositions', async () => {
    render(<PositionBlotter account={account} />);

    expect(getPositionsMock).toHaveBeenCalledWith(42);
    expect(subscribeMock).toHaveBeenCalledWith(
      '/accounts/42/positions',
      expect.any(Function)
    );

    await waitFor(() =>
      expect(screen.getByTestId('row-AAPL')).toHaveTextContent('AAPL:100')
    );
  });

  it('updates an existing row and adds a new row from feed updates', async () => {
    render(<PositionBlotter account={account} />);

    await waitFor(() =>
      expect(screen.getByTestId('row-AAPL')).toHaveTextContent('AAPL:100')
    );

    // Existing security -> quantity updated in place.
    act(() => {
      feedCb({
        accountid: 42,
        quantity: 250,
        security: 'AAPL',
        updated: new Date('2024-01-02'),
      });
    });
    await waitFor(() =>
      expect(screen.getByTestId('row-AAPL')).toHaveTextContent('AAPL:250')
    );

    // New security -> row added.
    act(() => {
      feedCb({
        accountid: 42,
        quantity: 30,
        security: 'MSFT',
        updated: new Date('2024-01-03'),
      });
    });
    await waitFor(() =>
      expect(screen.getByTestId('row-MSFT')).toHaveTextContent('MSFT:30')
    );
  });

  it('unsubscribes from the feed on unmount', async () => {
    const { unmount } = render(<PositionBlotter account={account} />);
    await waitFor(() => expect(subscribeMock).toHaveBeenCalled());
    unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });
});
