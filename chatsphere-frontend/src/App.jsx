import React from 'react';
import Navbar from './components/common/Navbar';
import HealthCheckPage from './pages/HealthCheckPage';

export default function App() {
  return (
    <div className="min-vh-100 d-flex flex-column">
      <Navbar />
      <main className="flex-grow-1">
        <HealthCheckPage />
      </main>
    </div>
  );
}
