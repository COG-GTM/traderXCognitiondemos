import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Header from './Header';

describe('Header', () => {
  beforeEach(() => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    );
  });

  it('should create', () => {
    expect(screen.getByText('FINOS | TraderX Sample Application')).toBeInTheDocument();
  });

  it('renders the Trade and Account navigation tabs', () => {
    expect(screen.getByRole('link', { name: 'Trade' })).toHaveAttribute('href', '/trade');
    expect(screen.getByRole('link', { name: 'Account' })).toHaveAttribute('href', '/account');
  });
});
