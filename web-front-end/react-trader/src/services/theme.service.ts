export enum Themes {
  ProfessionalLight = 'professional-light',
  ProfessionalDark = 'professional-dark'
}

let currentTheme = Themes.ProfessionalDark;

export function getCurrentTheme(): Themes {
  return currentTheme;
}

export function switchTheme(): Themes {
  currentTheme = currentTheme === Themes.ProfessionalDark ? Themes.ProfessionalLight : Themes.ProfessionalDark;
  document.documentElement.className = currentTheme;
  const themeTag = document.querySelector<HTMLLinkElement>('#theme-tag');
  if (themeTag) {
    themeTag.href = `${currentTheme}.css`;
  }
  return currentTheme;
}

export const themeService = { getCurrentTheme, switchTheme };
export type ThemeService = typeof themeService;
