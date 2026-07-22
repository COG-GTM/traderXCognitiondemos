import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

describe('App', () => {
  it('should create the app', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>
    );
    expect(container).toBeTruthy();
  });

  it('redirects the root route to the trade view', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByRole('link', { name: 'Trade' })).toHaveClass('active');
  });

  it('renders the page-not-found view for unknown routes', () => {
    render(
      <MemoryRouter initialEntries={['/does-not-exist']}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
  });
});
