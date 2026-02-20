import React from 'react';
import { NavLink } from 'react-router-dom';
import './Header.css';

export const Header: React.FC = () => {
  return (
    <>
      <nav className="header-navbar">
        <div className="header-container">
          <div className="header-title">FINOS | TraderX Sample Application</div>
        </div>
      </nav>
      <ul className="header-tabs">
        <li className="header-tab-item">
          <NavLink
            to="/trade"
            className={({ isActive }) => `header-tab-link${isActive ? ' active' : ''}`}
          >
            Trade
          </NavLink>
        </li>
        <li className="header-tab-item">
          <NavLink
            to="/account"
            className={({ isActive }) => `header-tab-link${isActive ? ' active' : ''}`}
          >
            Account
          </NavLink>
        </li>
      </ul>
    </>
  );
};
