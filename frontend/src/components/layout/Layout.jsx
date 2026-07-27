// src/components/layout/Layout.jsx
import Sidebar from './Sidebar';
import '../../styles/globals.css';

export default function Layout({ children }) {
  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar />
      <main style={{
        marginLeft: 'var(--sidebar-w)',
        flex: 1,
        padding: '32px',
        minWidth: 0,
        overflowX: 'hidden',
      }}>
        {children}
      </main>
    </div>
  );
}
