import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Header } from './components/Header';
import { TradePage } from './components/trade/TradePage';
import { AccountsPage } from './components/accounts/AccountsPage';
import { PageNotFound } from './components/shared';

function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/trade" element={<TradePage />} />
        <Route path="/account" element={<AccountsPage />} />
        <Route path="/" element={<Navigate to="/trade" replace />} />
        <Route path="*" element={<PageNotFound />} />
      </Routes>
    </>
  );
}

export default App;
