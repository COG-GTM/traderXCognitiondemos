import React from 'react';
import { NavLink } from 'react-router-dom';
import './Header.css';

export const Header: React.FC = () => {
  return (
    <div>
      <nav className="header-navbar">
        <div className="header-container">
          <div className="header-title">FINOS | TraderX Sample Application</div>
        </div>
      </nav>
      <ul className="nav-tabs">
        <li className="nav-item">
          <NavLink
            to="/trade"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            Trade
          </NavLink>
        </li>
        <li className="nav-item">
          <NavLink
            to="/account"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            Account
          </NavLink>
        </li>
      </ul>
    </div>
  );
};
