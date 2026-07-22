import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Header } from './Header';
import { ThemeProvider } from '../../context/ThemeContext';

const renderHeader = () =>
  render(
    <ThemeProvider>
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    </ThemeProvider>
  );

describe('Header', () => {
  it('renders the title and both nav links', () => {
    renderHeader();

    expect(
      screen.getByText('FINOS | TraderX Sample Application')
    ).toBeInTheDocument();

    const trade = screen.getByRole('link', { name: 'Trade' });
    const account = screen.getByRole('link', { name: 'Account' });
    expect(trade).toHaveAttribute('href', '/trade');
    expect(account).toHaveAttribute('href', '/account');
  });

  it('invokes switchTheme when the theme control is clicked', () => {
    const switchTheme = jest.fn();
    const spy = jest
      .spyOn(require('../../context/ThemeContext'), 'useTheme')
      .mockReturnValue({ theme: 'professional-dark', switchTheme });

    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: 'Switch theme' }));
    expect(switchTheme).toHaveBeenCalledTimes(1);

    spy.mockRestore();
  });

  it('applies the active class to the current route link', () => {
    render(
      <ThemeProvider>
        <MemoryRouter initialEntries={['/trade']}>
          <Header />
        </MemoryRouter>
      </ThemeProvider>
    );

    expect(screen.getByRole('link', { name: 'Trade' })).toHaveClass('active');
    expect(screen.getByRole('link', { name: 'Account' })).not.toHaveClass(
      'active'
    );
  });
});
