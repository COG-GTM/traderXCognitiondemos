import './App.css';
import { Datatable } from './Datatable/Datatable';
import { PortfolioReport } from './report/PortfolioReport';
import React, { useState } from 'react';

type Tab = 'blotter' | 'report';

function App() {
  const [tab, setTab] = useState<Tab>('blotter');
  return (
    <div className="App">
      <nav className="app-tabs" role="tablist" aria-label="Views">
        <button role="tab" aria-selected={tab === 'blotter'} onClick={() => setTab('blotter')}>Blotter</button>
        <button role="tab" aria-selected={tab === 'report'} onClick={() => setTab('report')}>Portfolio report</button>
      </nav>
      {tab === 'blotter' ? <Datatable /> : <PortfolioReport />}
    </div>
  );
}

export default App;
