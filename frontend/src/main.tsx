import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

// Initialize dark mode from store before render
const stored = localStorage.getItem('ragllm-theme');
if (stored) {
  try {
    const parsed = JSON.parse(stored);
    if (parsed?.state?.isDark) {
      document.documentElement.classList.add('dark');
    }
  } catch {
    // ignore
  }
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
