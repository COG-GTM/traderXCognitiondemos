import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

export type Theme = 'professional-light' | 'professional-dark';

interface ThemeContextValue {
  theme: Theme;
  switchTheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export const ThemeProvider = ({ children }: { children: React.ReactNode }) => {
  const [theme, setTheme] = useState<Theme>('professional-dark');

  const switchTheme = useCallback(() => {
    setTheme((current) => {
      const next: Theme =
        current === 'professional-dark' ? 'professional-light' : 'professional-dark';
      document.documentElement.className = next;
      document.documentElement.setAttribute(
        'data-bs-theme',
        next === 'professional-dark' ? 'dark' : 'light'
      );
      return next;
    });
  }, []);

  const value = useMemo(() => ({ theme, switchTheme }), [theme, switchTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = (): ThemeContextValue => {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return ctx;
};
