import React from 'react';
import { render, screen } from '@testing-library/react';
import { PageNotFound } from './PageNotFound';

describe('PageNotFound', () => {
  it('renders the "Page not found" heading', () => {
    render(<PageNotFound />);
    expect(
      screen.getByRole('heading', { level: 2, name: 'Page not found' })
    ).toBeInTheDocument();
  });
});
