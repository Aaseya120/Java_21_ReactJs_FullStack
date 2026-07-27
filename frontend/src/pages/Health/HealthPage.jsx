// src/pages/Health/HealthPage.jsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { healthApi } from '../../api/health.api';
import { getConfig } from '../../utils/config';

const SERVICE_META = {
  gateway:      { label: 'API Gateway',           icon: '🌐', desc: 'Routes all requests, JWT auth, rate limiting' },
  user:         { label: 'User Service',           icon: '👤', desc: 'Auth, registration, user profiles' },
  order:        { label: 'Order Service',          icon: '📋', desc: 'Order lifecycle, state machine, Saga' },
  product:      { label: 'Product Service',        icon: '📦', desc: 'Product catalog, stock management, CQRS' },
  notification: { label: 'Notification Service',   icon: '🔔', desc: 'SSE streams, Kafka consumer' },
};

function HealthCard({ id, meta, data, url }) {
  const isUp = data?.status === 'UP';
  return (
    <div className="card" style={{
      borderLeft: `3px solid ${isUp ? 'var(--success)' : 'var(--danger)'}`,
      transition: 'all 0.2s',
    }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 16 }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <div style={{
            width: 44, height: 44, borderRadius: 12,
            background: isUp ? 'var(--success-light)' : 'var(--danger-light)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.25rem',
          }}>{meta.icon}</div>
          <div>
            <div style={{ fontWeight: 700, fontSize: '1rem' }}>{meta.label}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 2 }}>{meta.desc}</div>
          </div>
        </div>
        <span className={`badge badge--${isUp ? 'success' : 'danger'}`} style={{ flexShrink: 0 }}>
          {isUp ? '● UP' : '● DOWN'}
        </span>
      </div>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
        <code style={{
          background: 'var(--bg-base)', border: '1px solid var(--border)',
          borderRadius: 6, padding: '3px 10px', fontSize: '0.75rem', color: 'var(--text-secondary)',
        }}>{url}</code>
      </div>
      {data?.details && (
        <details style={{ marginTop: 12 }}>
          <summary style={{ cursor: 'pointer', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>View details</summary>
          <pre className="code-block" style={{ marginTop: 8, fontSize: '0.75rem' }}>
            {JSON.stringify(data.details, null, 2)}
          </pre>
        </details>
      )}
    </div>
  );
}

export default function HealthPage() {
  const config = getConfig();
  const { data: health, isLoading, refetch, dataUpdatedAt } = useQuery({
    queryKey: ['health-all'],
    queryFn: healthApi.checkAll,
    refetchInterval: 30000,
  });

  const upCount = health ? Object.values(health).filter(v => v.status === 'UP').length : 0;
  const total = Object.keys(SERVICE_META).length;

  const urlMap = {
    gateway: config.gatewayUrl,
    user: config.userServiceUrl,
    order: config.orderServiceUrl,
    product: config.productServiceUrl,
    notification: config.notificationServiceUrl,
  };

  return (
    <div className="animate-fade">
      <div className="page-header">
        <div>
          <h1>Health Monitor</h1>
          <p>Real-time health status for all microservices. Auto-refreshes every 30 seconds.</p>
        </div>
        <button className="btn btn--primary" onClick={() => refetch()}>
          ↻ Refresh Now
        </button>
      </div>

      {/* Summary */}
      <div className="stats-grid" style={{ marginBottom: 28 }}>
        <div className={`stat-card stat-card--${upCount === total ? 'success' : 'warning'}`}>
          <div className="stat-card__label">Services UP</div>
          <div className="stat-card__value">{isLoading ? '...' : `${upCount}/${total}`}</div>
          <div className="stat-card__sub">{upCount === total ? 'All operational' : `${total - upCount} offline`}</div>
        </div>
        <div className="stat-card">
          <div className="stat-card__label">Last Updated</div>
          <div className="stat-card__value" style={{ fontSize: '1rem', color: 'var(--info)' }}>
            {dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString() : '—'}
          </div>
          <div className="stat-card__sub">auto-refresh: 30s</div>
        </div>
        <div className="stat-card stat-card--primary">
          <div className="stat-card__label">Gateway URL</div>
          <div className="stat-card__value" style={{ fontSize: '0.875rem', wordBreak: 'break-all' }}>
            {config.gatewayUrl}
          </div>
          <div className="stat-card__sub">configurable in Settings</div>
        </div>
      </div>

      {/* Service Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: 16 }}>
        {isLoading
          ? Array(5).fill(0).map((_, i) => (
              <div key={i} className="card">
                <div className="skeleton" style={{ height: 44, borderRadius: 12, marginBottom: 16 }}/>
                <div className="skeleton" style={{ height: 16 }}/>
              </div>
            ))
          : Object.entries(SERVICE_META).map(([id, meta]) => (
              <HealthCard key={id} id={id} meta={meta} data={health?.[id]} url={urlMap[id]}/>
            ))
        }
      </div>
    </div>
  );
}
