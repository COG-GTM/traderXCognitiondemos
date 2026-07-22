import React from 'react';
import { NavLink } from 'react-router-dom';
import { useTheme } from '../../context/ThemeContext';

// SECTION 1 — App shell header & navigation.
// Ports the Angular `header/` component and the `app.component` theme-switch
// wiring: a dark Bootstrap navbar with FINOS/TraderX branding, Trade/Account
// nav-tabs (react-router NavLink with active styling) and a theme-switch
// button wired to `useTheme().switchTheme`.
export const Header = () => {
  const { switchTheme } = useTheme();

  return (
    <>
      <nav
        className="navbar navbar-expand-lg bg-body-tertiary text-bg-primary"
        data-bs-theme="dark"
      >
        <div className="container-fluid">
          <img
            width="50px"
            src="assets/img/traderx-apple-touch-icon.png"
            alt="TraderX"
          />
          <div style={{ fontSize: '1.5em' }}>FINOS | TraderX Sample Application</div>
          <div className="d-flex align-items-center gap-3">
            <button
              type="button"
              className="btn btn-outline-light btn-sm"
              onClick={switchTheme}
              aria-label="Switch theme"
              title="Switch theme"
            >
              Switch theme
            </button>
            <img
              width="30px"
              src="assets/img/FINOS_Icon_White.png"
              alt="FINOS"
            />
          </div>
        </div>
      </nav>

      <ul className="nav nav-tabs mt-3">
        <li className="nav-item">
          <NavLink
            className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
            to="/trade"
          >
            Trade
          </NavLink>
        </li>
        <li className="nav-item">
          <NavLink
            className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
            to="/account"
          >
            Account
          </NavLink>
        </li>
      </ul>
    </>
  );
};
