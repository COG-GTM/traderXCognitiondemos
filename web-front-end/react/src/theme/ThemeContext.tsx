import React, { createContext, useCallback, useMemo, useState } from 'react';
import { SaltProvider } from '@salt-ds/core';

export type ThemeMode = 'light' | 'dark';

export interface ThemeContextValue {
	mode: ThemeMode;
	toggleTheme: () => void;
}

/**
 * React port of the Angular `ThemeService`, which toggled the document between
 * `professional-light` and `professional-dark` (defaulting to dark). Here we
 * express the same light/dark switching through Salt Design System's
 * `SaltProvider`, whose `mode` prop drives the professional theme variant.
 */
export const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export interface ThemeProviderProps {
	children: React.ReactNode;
	/** Initial mode. Matches the Angular default of dark. */
	defaultMode?: ThemeMode;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children, defaultMode = 'dark' }) => {
	const [mode, setMode] = useState<ThemeMode>(defaultMode);

	const toggleTheme = useCallback(() => {
		setMode((prev) => (prev === 'dark' ? 'light' : 'dark'));
	}, []);

	const value = useMemo<ThemeContextValue>(() => ({ mode, toggleTheme }), [mode, toggleTheme]);

	return (
		<ThemeContext.Provider value={value}>
			<SaltProvider mode={mode}>{children}</SaltProvider>
		</ThemeContext.Provider>
	);
};
