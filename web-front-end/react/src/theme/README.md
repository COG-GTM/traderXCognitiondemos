# Theme switching (Salt DS provider + toggle)

React port of the Angular `ThemeService`
(`web-front-end/angular/main/app/service/theme.service.ts`), which toggled the
document between `professional-light` and `professional-dark` (defaulting to
dark). Here the same light/dark switching is expressed through Salt Design
System's `SaltProvider`.

## Exports (`src/theme`)

- `ThemeProvider` — context provider that holds the current `mode`
  (`'light' | 'dark'`, default `'dark'`) and wraps children in `SaltProvider`
  with the matching `mode`.
- `useTheme()` — hook returning `{ mode, toggleTheme }`.
- `ThemeContext`, and types `ThemeMode`, `ThemeContextValue`, `ThemeProviderProps`.

## INTEGRATION NOTE

- `App.tsx` should wrap the application in `<ThemeProvider>` (near the root, so
  every component sits inside the `SaltProvider`):

  ```tsx
  import { ThemeProvider } from './theme';

  function App() {
    return (
      <ThemeProvider>
        {/* rest of the app */}
      </ThemeProvider>
    );
  }
  ```

- The Header's theme toggle button should call `toggleTheme()` from `useTheme()`:

  ```tsx
  import { useTheme } from '../theme';

  const { mode, toggleTheme } = useTheme();
  <Button onClick={toggleTheme}>{mode === 'dark' ? 'Light' : 'Dark'}</Button>
  ```

Default mode is `dark`, matching the Angular app.
