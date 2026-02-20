import './App.css';
import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Datatable } from './Datatable/Datatable';
import { AccountManagement } from './AccountManagement';
import { Header } from './Header';

function App() {
  return (
    <div className="App">
      <Header />
      <Routes>
        <Route path="/trade" element={<Datatable />} />
        <Route path="/account" element={<AccountManagement />} />
        <Route path="/" element={<Navigate to="/trade" replace />} />
      </Routes>
    </div>
  );
}

export default App;
