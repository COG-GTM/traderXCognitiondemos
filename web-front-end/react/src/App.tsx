import './App.css';
import { Datatable } from './Datatable/Datatable';
import Loader from './Loader';
import React from 'react';

function App() {
  return (
    <div className="App">
      <Loader />
      <Datatable />
    </div>
  );
}

export default App;
