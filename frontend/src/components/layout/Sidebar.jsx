import { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import '../../styles/toast.css';

const NAV = [
  { section: 'Overview', items: [
    { to: '/',            icon: '⊞', label: 'Dashboard' },
    { to: '/health',      icon: '♥', label: 'Health Monitor' },
  ]},
  { section: 'Services', items: [
    { to: '/products',    icon: '📦', label: 'Products' },
    { to: '/orders',      icon: '📋', label: 'Orders' },
    { to: '/users',       icon: '👤', label: 'Users' },
    { to: '/payments',    icon: '💳', label: 'Payments' },
    { to: '/aggregator',  icon: '🔗', label: 'Aggregator', adminOnly: true },
    { to: '/notifications',icon:'🔔', label: 'Notifications' },
  ]},
  { section: 'System', items: [
    { to: '/settings',    icon: '⚙', label: 'Settings' },
  ]},
];

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [theme, setTheme] = useState(
    () => localStorage.getItem('theme') || 'dark'
  );

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = (e) => {
    e.stopPropagation();
    setTheme(t => t === 'dark' ? 'light' : 'dark');
  };

  const initials = user?.fullName
    ? user.fullName.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase()
    : 'U';

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="sidebar-logo__icon">☕</div>
        <div>
          <div className="sidebar-logo__text">MS Dashboard</div>
          <div className="sidebar-logo__sub">Java 21 Microservices</div>
        </div>
      </div>

      <nav className="sidebar-nav">
        {NAV.map(group => (
          <div key={group.section}>
            <div className="sidebar-section">{group.section}</div>
            {group.items.map(item => {
              if (item.adminOnly && user?.role !== 'ADMIN') return null;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                >
                  <span className="nav-item__icon">{item.icon}</span>
                  {item.label}
                </NavLink>
              );
            })}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-card" onClick={() => navigate('/users')}>
          <div className="user-avatar">{initials}</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="user-card__name truncate">{user?.fullName || 'User'}</div>
            <div className="user-card__role">{user?.email || ''}</div>
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              onClick={toggleTheme}
              style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '1rem' }}
              title="Toggle Theme"
            >
              {theme === 'dark' ? '☀️' : '🌙'}
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); logout(); }}
              style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '1rem' }}
              title="Logout"
            >⏻</button>
          </div>
        </div>
      </div>
    </aside>
  );
}
