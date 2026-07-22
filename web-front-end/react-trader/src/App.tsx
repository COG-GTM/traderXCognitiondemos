import { Navigate, Route, Routes } from 'react-router-dom';

function Placeholder({ name }: { name: string }) {
  return <div className="container mt-4">{name} (migration in progress)</div>;
}

function App() {
  return (
    <Routes>
      <Route path="/trade" element={<Placeholder name="Trade" />} />
      <Route path="/account" element={<Placeholder name="Accounts" />} />
      <Route path="/" element={<Navigate to="/trade" replace />} />
      <Route path="*" element={<Placeholder name="Page not found" />} />
    </Routes>
  );
}

export default App;
