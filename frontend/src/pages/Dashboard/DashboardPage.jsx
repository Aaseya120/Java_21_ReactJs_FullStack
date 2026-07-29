// src/pages/Dashboard/DashboardPage.jsx
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { healthApi } from '../../api/health.api';
import { useAuth } from '../../context/AuthContext';

const SERVICE_META = {
  gateway:      { label: 'API Gateway',          port: '8080', icon: '🌐', color: 'var(--primary)' },
  user:         { label: 'User Service',          port: '8081', icon: '👤', color: 'var(--info)' },
  order:        { label: 'Order Service',         port: '8082', icon: '📋', color: 'var(--warning)' },
  product:      { label: 'Product Service',       port: '8083', icon: '📦', color: 'var(--success)' },
  payment:      { label: 'Payment Service',       port: '8085', icon: '💳', color: '#f43f5e' },
  notification: { label: 'Notification Service',  port: '8084', icon: '🔔', color: '#a78bfa' },
};

function StatusDot({ status }) {
  const color = status === 'UP' ? 'var(--success)' : status === 'CHECKING' ? 'var(--warning)' : 'var(--danger)';
  return (
    <span style={{
      display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
      background: color, boxShadow: `0 0 8px ${color}`,
      animation: status === 'UP' ? 'none' : 'pulse 1.5s infinite',
    }}/>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: health, isLoading, refetch } = useQuery({
    queryKey: ['health-all'],
    queryFn: healthApi.checkAll,
    refetchInterval: 30000,
    staleTime: 15000,
  });

  const services = health ? Object.entries(health) : [];
  const upCount = services.filter(([, v]) => v.status === 'UP').length;
  const allUp = upCount === services.length;

  const quickActions = [
    { label: 'Create Product', icon: '📦', path: '/products', color: 'var(--success)' },
    { label: 'Place Order',    icon: '📋', path: '/orders',   color: 'var(--warning)' },
    { label: 'View Users',     icon: '👤', path: '/users',    color: 'var(--info)' },
    { label: 'Aggregator',     icon: '🔗', path: '/aggregator', color: 'var(--primary)' },
  ];

  const greeting = () => {
    const hr = new Date().getHours();
    if (hr < 12) return 'Good morning';
    if (hr < 18) return 'Good afternoon';
    return 'Good evening';
  };

  return (
    <div className="animate-fade">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1>{greeting()}, {user?.fullName || 'Developer'}!</h1>
          <p>Full-Stack Java 21 + React 19 Microservices Architecture Control Panel</p>
        </div>
        <button className="btn btn--secondary" onClick={() => refetch()}>
          ↻ Refresh
        </button>
      </div>

      {/* Overall status banner */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12,
        padding: '14px 20px', borderRadius: 'var(--radius-lg)',
        background: allUp ? 'var(--success-light)' : upCount === 0 ? 'rgba(239, 68, 68, 0.15)' : 'var(--warning-light)',
        border: `1px solid ${allUp ? 'rgba(16,185,129,0.3)' : upCount === 0 ? 'rgba(239, 68, 68, 0.4)' : 'rgba(245,158,11,0.3)'}`,
        marginBottom: 28,
      }}>
        <span style={{ fontSize: '1.25rem' }}>{allUp ? '✅' : upCount === 0 ? '🚨' : '⚠️'}</span>
        <div>
          <strong style={{ color: allUp ? 'var(--success)' : upCount === 0 ? 'var(--danger)' : 'var(--warning)' }}>
            {isLoading ? 'Checking services...' : allUp ? 'All Systems Operational' : upCount === 0 ? 'All Microservices Offline — Run start-all.ps1 to start backend services' : `${upCount}/${services.length} Services Online`}
          </strong>
          <span style={{ marginLeft: 12, fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
            Last checked: {new Date().toLocaleTimeString()}
          </span>
        </div>
        <button className="btn btn--ghost btn--sm" style={{ marginLeft: 'auto' }} onClick={() => navigate('/health')}>
          View Details →
        </button>
      </div>

      {/* Stats */}
      <div className="stats-grid">
        <div className="stat-card stat-card--primary">
          <div className="stat-card__label">Services Online</div>
          <div className="stat-card__value">{isLoading ? '—' : `${upCount}/${services.length}`}</div>
          <div className="stat-card__sub">microservices running</div>
        </div>
        <div className="stat-card stat-card--success">
          <div className="stat-card__label">Gateway</div>
          <div className="stat-card__value" style={{ fontSize: '1.25rem' }}>
            {isLoading ? '—' : health?.gateway?.status === 'UP' ? '✅ Online' : '❌ Offline'}
          </div>
          <div className="stat-card__sub">port 8080</div>
        </div>
        <div className="stat-card">
          <div className="stat-card__label">Auto-Refresh</div>
          <div className="stat-card__value" style={{ fontSize: '1.5rem', color: 'var(--info)' }}>30s</div>
          <div className="stat-card__sub">health check interval</div>
        </div>
        <div className="stat-card">
          <div className="stat-card__label">Architecture</div>
          <div className="stat-card__value" style={{ fontSize: '1.1rem', color: 'var(--primary)' }}>Java 21</div>
          <div className="stat-card__sub">Spring Boot 3 + Virtual Threads</div>
        </div>
      </div>

      {/* Service Health Cards */}
      <h2 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: 16, color: 'var(--text-secondary)' }}>
        SERVICE STATUS
      </h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 14, marginBottom: 32 }}>
        {isLoading
          ? Array(5).fill(0).map((_, i) => (
              <div key={i} className="card" style={{ padding: 20 }}>
                <div className="skeleton" style={{ height: 16, width: '60%', marginBottom: 12 }}/>
                <div className="skeleton" style={{ height: 24, width: '40%' }}/>
              </div>
            ))
          : Object.entries(SERVICE_META).map(([key, meta]) => {
              const svc = health?.[key];
              const isUp = svc?.status === 'UP';
              return (
                <div key={key} className="card" style={{
                  padding: 20,
                  borderLeft: `3px solid ${isUp ? 'var(--success)' : 'var(--danger)'}`,
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
                onClick={() => navigate('/health')}
                onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-2px)'}
                onMouseLeave={e => e.currentTarget.style.transform = 'translateY(0)'}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                    <span style={{ fontSize: '1.25rem' }}>{meta.icon}</span>
                    <StatusDot status={svc?.status || 'DOWN'}/>
                  </div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', marginBottom: 4 }}>{meta.label}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Port {meta.port}</div>
                  <div style={{ marginTop: 10 }}>
                    <span className={`badge badge--${isUp ? 'success' : 'danger'}`}>
                      {isUp ? '● UP' : '● DOWN'}
                    </span>
                  </div>
                </div>
              );
            })
        }
      </div>

      {/* Quick Actions */}
      <h2 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: 16, color: 'var(--text-secondary)' }}>
        QUICK ACTIONS
      </h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 12 }}>
        {quickActions.map(a => (
          <button key={a.label} className="card" onClick={() => navigate(a.path)}
            style={{
              border: 'none', cursor: 'pointer', textAlign: 'left',
              display: 'flex', flexDirection: 'column', gap: 10, padding: 20,
              transition: 'all 0.2s', background: 'var(--bg-surface)', color: 'var(--text-primary)'
            }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = a.color; e.currentTarget.style.transform = 'translateY(-2px)'; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border)'; e.currentTarget.style.transform = 'translateY(0)'; }}
          >
            <span style={{ fontSize: '1.5rem' }}>{a.icon}</span>
            <span style={{ fontWeight: 600, fontSize: '0.9375rem' }}>{a.label}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
