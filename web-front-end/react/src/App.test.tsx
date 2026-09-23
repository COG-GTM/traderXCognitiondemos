import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('./Datatable/Datatable', () => ({ Datatable: () => <div>blotter placeholder</div> }));

test('renders the blotter and portfolio report tabs', () => {
  render(<App />);
  expect(screen.getByRole('tab', { name: 'Blotter' })).toBeInTheDocument();
  expect(screen.getByRole('tab', { name: 'Portfolio report' })).toBeInTheDocument();
});
