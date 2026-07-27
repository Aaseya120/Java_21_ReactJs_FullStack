// src/pages/Notifications/NotificationsPage.jsx
import { useState, useEffect, useRef, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { getConfig } from '../../utils/config';
import { getAccessToken } from '../../utils/token';

const EVENT_COLORS = {
  ORDER_CREATED:    'var(--success)',
  ORDER_UPDATED:    'var(--warning)',
  ORDER_CANCELLED:  'var(--danger)',
  PRODUCT_CREATED:  'var(--info)',
  PRODUCT_UPDATED:  'var(--primary)',
  PRODUCT_DELETED:  'var(--danger)',
  USER_REGISTERED:  'var(--success)',
  default:          'var(--text-muted)',
};

function EventItem({ event, idx }) {
  const color = EVENT_COLORS[event.type] || EVENT_COLORS.default;
  return (
    <div style={{
      display: 'flex', gap: 14, padding: '14px 0',
      borderBottom: '1px solid var(--border)',
      animation: 'slideUp 0.3s ease',
    }}>
      <div style={{ position: 'relative', marginTop: 4 }}>
        <div style={{
          width: 10, height: 10, borderRadius: '50%',
          background: color, boxShadow: `0 0 8px ${color}`,
          marginTop: 4,
        }}/>
        {idx > 0 && (
          <div style={{
            position: 'absolute', top: 14, left: '50%', transform: 'translateX(-50%)',
            width: 2, height: '100%', background: 'var(--border)',
          }}/>
        )}
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6, flexWrap: 'wrap' }}>
          <span style={{
            background: `${color}22`, color, border: `1px solid ${color}44`,
            borderRadius: 99, fontSize: '0.7rem', fontWeight: 700,
            padding: '2px 10px', letterSpacing: '0.04em',
          }}>{event.type || 'EVENT'}</span>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            {new Date(event.timestamp || Date.now()).toLocaleTimeString()}
          </span>
        </div>
        {event.data && (
          <pre style={{
            background: 'var(--bg-base)', borderRadius: 6, padding: '8px 12px',
            fontSize: '0.75rem', color: 'var(--text-secondary)', overflow: 'auto', maxHeight: 120,
          }}>
            {typeof event.data === 'string' ? event.data : JSON.stringify(event.data, null, 2)}
          </pre>
        )}
      </div>
    </div>
  );
}

export default function NotificationsPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [userId, setUserId] = useState(() => user?.id || '');
  const [connected, setConnected] = useState(false);
  const [events, setEvents] = useState([]);
  const [connectionError, setConnectionError] = useState(null);
  const [statusCheck, setStatusCheck] = useState(null);
  const esRef = useRef(null);
  const eventsEndRef = useRef(null);

  const connect = useCallback(() => {
    if (!String(userId).trim()) { toast.error('Enter a User ID to connect'); return; }
    if (esRef.current) esRef.current.close();

    const config = getConfig();
    const token = getAccessToken();
    const url = `${config.gatewayUrl}/api/v1/notifications/stream/${userId}?token=${token}`;
    setConnectionError(null);

    try {
      const es = new EventSource(url);
      esRef.current = es;

      es.onopen = () => {
        setConnected(true);
        setConnectionError(null);
        toast.success('Connected to notification stream!');
        setEvents(prev => [{
          type: 'SYSTEM',
          timestamp: Date.now(),
          data: `Connected to stream for user: ${userId}`,
        }, ...prev]);
      };

      const handleMessage = (e) => {
        try {
          if (e.data === 'heartbeat') return;
          const parsed = JSON.parse(e.data);
          setEvents(prev => [{ ...parsed, timestamp: Date.now() }, ...prev.slice(0, 99)]);
          eventsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
        } catch {
          setEvents(prev => [{ type: 'RAW', data: e.data, timestamp: Date.now() }, ...prev.slice(0, 99)]);
        }
      };

      es.onmessage = handleMessage;
      es.addEventListener('notification', handleMessage);
      es.addEventListener('ping', (e) => { /* ignore heartbeat */ });

      es.onerror = () => {
        setConnected(false);
        setConnectionError('Stream connection lost. The service may be unavailable or the connection timed out.');
      };
    } catch (err) {
      setConnectionError('Failed to create SSE connection: ' + err.message);
    }
  }, [userId, toast]);

  const disconnect = useCallback(() => {
    if (esRef.current) { esRef.current.close(); esRef.current = null; }
    setConnected(false);
    toast.info('Disconnected from stream');
  }, [toast]);

  useEffect(() => () => { if (esRef.current) esRef.current.close(); }, []);

  // Check notification service status
  const checkStatus = async () => {
    const config = getConfig();
    try {
      const r = await fetch(`${config.notificationServiceUrl}/actuator/health`);
      const data = await r.json();
      setStatusCheck({ ok: data.status === 'UP', data });
    } catch {
      setStatusCheck({ ok: false, data: 'Service unreachable' });
    }
  };

  return (
    <div className="animate-fade">
      <div className="page-header">
        <div>
          <h1>Notifications</h1>
          <p>Real-time event stream via Server-Sent Events (SSE) from the notification service.</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn--secondary btn--sm" onClick={checkStatus}>Check Status</button>
          <span className={`badge badge--${connected ? 'success' : 'muted'}`}>
            ● {connected ? 'LIVE' : 'DISCONNECTED'}
          </span>
        </div>
      </div>

      {/* Status Result */}
      {statusCheck && (
        <div style={{
          background: statusCheck.ok ? 'var(--success-light)' : 'var(--danger-light)',
          border: `1px solid ${statusCheck.ok ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}`,
          borderRadius: 'var(--radius-md)', padding: '12px 16px',
          color: statusCheck.ok ? 'var(--success)' : 'var(--danger)',
          fontSize: '0.875rem', marginBottom: 20,
        }}>
          {statusCheck.ok ? '✅' : '❌'} Notification Service: {JSON.stringify(statusCheck.data)}
        </div>
      )}

      {/* Architecture Note */}
      <div className="card card--glass" style={{ marginBottom: 20, padding: 16 }}>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 8, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>SSE Architecture</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', fontSize: '0.875rem' }}>
          <span className="badge badge--warning">Kafka Topics</span>
          <span style={{ color: 'var(--text-muted)' }}>→</span>
          <span className="badge badge--danger">Notification Consumer</span>
          <span style={{ color: 'var(--text-muted)' }}>→</span>
          <span className="badge badge--info">Reactive SSE Stream</span>
          <span style={{ color: 'var(--text-muted)' }}>→</span>
          <span className="badge badge--success">Browser</span>
        </div>
      </div>

      {/* Connect Panel */}
      <div className="card" style={{ marginBottom: 20 }}>
        <div style={{ fontWeight: 700, marginBottom: 16 }}>🔌 Stream Connection</div>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <input className="input" style={{ flex: 1, minWidth: 200 }}
            placeholder="User UUID to subscribe..."
            value={userId} onChange={e => setUserId(e.target.value)}
            disabled={connected}/>
          {!connected ? (
            <button className="btn btn--success" onClick={connect} disabled={!String(userId).trim()}>
              ▶ Connect
            </button>
          ) : (
            <button className="btn btn--danger" onClick={disconnect}>
              ■ Disconnect
            </button>
          )}
          {events.length > 0 && (
            <button className="btn btn--ghost btn--sm" onClick={() => setEvents([])}>
              Clear
            </button>
          )}
        </div>
        {connectionError && (
          <div style={{ marginTop: 12, color: 'var(--danger)', fontSize: '0.8125rem', background: 'var(--danger-light)', borderRadius: 'var(--radius-sm)', padding: '8px 12px' }}>
            ⚠️ {connectionError}
          </div>
        )}
        {connected && (
          <div style={{ marginTop: 12, fontSize: '0.8125rem', color: 'var(--success)' }}>
            ✅ Listening on <code style={{ color: 'var(--primary)' }}>/api/v1/notifications/stream/{userId}</code>
          </div>
        )}
      </div>

      {/* Event Log */}
      <div className="card" style={{ padding: 0 }}>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '16px 20px', borderBottom: '1px solid var(--border)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontWeight: 700 }}>Event Log</span>
            <span className="badge badge--primary">{events.length}</span>
          </div>
          {connected && (
            <span style={{
              display: 'flex', alignItems: 'center', gap: 6,
              fontSize: '0.75rem', color: 'var(--success)',
            }}>
              <span style={{
                width: 7, height: 7, borderRadius: '50%', background: 'var(--success)',
                animation: 'pulse 1.5s infinite',
              }}/>
              LIVE
            </span>
          )}
        </div>
        <div style={{ padding: '4px 20px 20px', maxHeight: 500, overflowY: 'auto' }}>
          {events.length === 0 ? (
            <div className="empty-state" style={{ padding: '40px 0' }}>
              <div style={{ fontSize: '2rem' }}>{connected ? '👂' : '📭'}</div>
              <div className="empty-state__title">
                {connected ? 'Listening for events...' : 'No events yet'}
              </div>
              <div className="empty-state__desc">
                {connected
                  ? 'Events will appear here in real-time as they occur in the system.'
                  : 'Connect to a user stream to see real-time notifications.'}
              </div>
            </div>
          ) : (
            events.map((e, i) => <EventItem key={`${e.timestamp}-${i}`} event={e} idx={i}/>)
          )}
          <div ref={eventsEndRef}/>
        </div>
      </div>
    </div>
  );
}
