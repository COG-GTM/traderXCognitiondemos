import React from 'react';
import { NavLink } from 'react-router-dom';
import './Header.css';

export interface HeaderProps {
  /**
   * Optional callback invoked when the "Switch theme" button is clicked.
   * The theme toggle logic lives in a separate piece; this component only
   * surfaces the button and delegates to the provided handler.
   */
  onSwitchTheme?: () => void;
}

const traderxLogo = process.env.PUBLIC_URL + '/traderx-apple-touch-icon.png';
const finosLogo = process.env.PUBLIC_URL + '/FINOS_Icon_White.png';

export const Header = ({ onSwitchTheme }: HeaderProps) => {
  return (
    <header className="traderx-header">
      <nav className="traderx-navbar" data-bs-theme="dark">
        <div className="traderx-navbar-container">
          <img
            className="traderx-navbar-logo"
            width={50}
            src={traderxLogo}
            alt="TraderX"
          />
          <div className="traderx-navbar-title">
            FINOS | TraderX Sample Application
          </div>
          <img
            className="traderx-navbar-finos"
            width={30}
            src={finosLogo}
            alt="FINOS"
          />
        </div>
        {onSwitchTheme && (
          <button
            type="button"
            className="traderx-switch-theme"
            onClick={onSwitchTheme}
          >
            Switch theme
          </button>
        )}
      </nav>

      <ul className="traderx-nav-tabs">
        <li className="traderx-nav-item">
          <NavLink
            to="/trade"
            className={({ isActive }) =>
              'traderx-nav-link' + (isActive ? ' active' : '')
            }
          >
            Trade
          </NavLink>
        </li>
        <li className="traderx-nav-item">
          <NavLink
            to="/account"
            className={({ isActive }) =>
              'traderx-nav-link' + (isActive ? ' active' : '')
            }
          >
            Account
          </NavLink>
        </li>
      </ul>
    </header>
  );
};

export default Header;
