import { Navigate, Route, Routes } from 'react-router-dom';
import Header from './components/Header';
import PageNotFound from './components/PageNotFound';
import AccountPage from './pages/accounts/AccountPage';
import TradePage from './pages/trade/TradePage';
import { switchTheme } from './services/theme.service';

function App() {
  return (
    <>
      <Header onSwitchTheme={switchTheme} />
      <Routes>
        <Route path="/trade" element={<TradePage />} />
        <Route path="/account" element={<AccountPage />} />
        <Route path="/" element={<Navigate to="/trade" replace />} />
        <Route path="*" element={<PageNotFound />} />
      </Routes>
    </>
  );
}

export default App;
