import { useContext } from 'react';
import { ThemeContext, ThemeContextValue } from './ThemeContext';

/**
 * Returns the current theme `mode` and a `toggleTheme()` function.
 * Must be used within a `ThemeProvider`.
 */
export const useTheme = (): ThemeContextValue => {
	const ctx = useContext(ThemeContext);
	if (!ctx) {
		throw new Error('useTheme must be used within a ThemeProvider');
	}
	return ctx;
};
