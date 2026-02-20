import './App.css';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Datatable } from './Datatable/Datatable';
import { AccountManagement } from './AccountManagement';
import { Header } from './Header';
import React from 'react';

function App() {
  return (
    <BrowserRouter>
      <div className="App">
        <Header />
        <Routes>
          <Route path="/trade" element={<Datatable />} />
          <Route path="/account" element={<AccountManagement />} />
          <Route path="/" element={<Navigate to="/trade" replace />} />
          <Route path="*" element={<div style={{ padding: 20 }}>Page not found</div>} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;
