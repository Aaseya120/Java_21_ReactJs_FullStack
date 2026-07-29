// src/pages/Settings/SettingsPage.jsx
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { getConfig, saveConfig, resetConfig } from '../../utils/config';
import { healthApi } from '../../api/health.api';
import { useToast } from '../../context/ToastContext';
import { getAccessToken, decodeToken } from '../../utils/token';

const SERVICE_FIELDS = [
  { key: 'gatewayUrl',            label: 'API Gateway',           icon: '🌐', port: '8080' },
  { key: 'userServiceUrl',        label: 'User Service',          icon: '👤', port: '8081' },
  { key: 'orderServiceUrl',       label: 'Order Service',         icon: '📋', port: '8082' },
  { key: 'productServiceUrl',     label: 'Product Service',       icon: '📦', port: '8083' },
  { key: 'paymentServiceUrl',     label: 'Payment Service',       icon: '💳', port: '8085' },
  { key: 'notificationServiceUrl',label: 'Notification Service',  icon: '🔔', port: '8084' },
];

export default function SettingsPage() {
  const currentConfig = getConfig();
  const toast = useToast();
  const [testResults, setTestResults] = useState({});
  const [testing, setTesting] = useState({});

  const { register, handleSubmit, reset, formState: { isDirty, isSubmitting } } = useForm({
    defaultValues: currentConfig,
  });

  const onSave = (data) => {
    saveConfig(data);
    toast.success('Configuration saved! Changes take effect immediately.');
    reset(data);
  };

  const onReset = () => {
    const defaults = resetConfig();
    reset(defaults);
    toast.info('Reset to default localhost settings');
  };

  const testService = async (key, url) => {
    setTesting(prev => ({ ...prev, [key]: true }));
    const result = await healthApi.checkService(url);
    setTestResults(prev => ({ ...prev, [key]: result }));
    setTesting(prev => ({ ...prev, [key]: false }));
  };

  const testAll = async () => {
    setTesting(Object.fromEntries(SERVICE_FIELDS.map(f => [f.key, true])));
    const results = await healthApi.checkAll();
    const mapped = Object.fromEntries(
      SERVICE_FIELDS.map((f, i) => {
        const serviceKey = ['gateway', 'user', 'order', 'product', 'payment', 'notification'][i];
        return [f.key, results[serviceKey]];
      })
    );
    setTestResults(mapped);
    setTesting({});
    const upCount = Object.values(results).filter(v => v.status === 'UP').length;
    toast[upCount === 6 ? 'success' : 'warning'](`${upCount}/6 services reachable`);
  };

  const token = getAccessToken();
  const decoded = token ? decodeToken(token) : null;

  return (
    <div className="animate-fade">
      <div className="page-header">
        <div>
          <h1>Settings</h1>
          <p>Configure backend service URLs. Settings are saved to your browser and work across any machine.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSave)}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(500px, 1fr))', gap: 20 }}>

          {/* Backend Configuration */}
          <div className="card" style={{ gridColumn: '1 / -1' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
              <h2 style={{ fontSize: '1.125rem', fontWeight: 700 }}>⚙️ Backend Configuration</h2>
              <div style={{ display: 'flex', gap: 10 }}>
                <button type="button" className="btn btn--ghost btn--sm" onClick={onReset}>
                  ↺ Reset Defaults
                </button>
                <button type="button" className="btn btn--secondary btn--sm"
                  onClick={handleSubmit(testAll)}>
                  🔌 Test All
                </button>
              </div>
            </div>

            <div style={{ display: 'grid', gap: 14 }}>
              {SERVICE_FIELDS.map(field => {
                const testResult = testResults[field.key];
                const isTesting = testing[field.key];
                const isUp = testResult?.status === 'UP';
                return (
                  <div key={field.key} style={{
                    display: 'grid', gridTemplateColumns: '200px 1fr auto',
                    gap: 12, alignItems: 'center',
                    padding: '14px 16px', background: 'var(--bg-elevated)',
                    borderRadius: 'var(--radius-md)',
                    border: `1px solid ${testResult ? (isUp ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)') : 'var(--border)'}`,
                    transition: 'border-color 0.3s',
                  }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 600, fontSize: '0.875rem' }}>
                        <span>{field.icon}</span> {field.label}
                      </div>
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>
                        default port: {field.port}
                      </div>
                    </div>
                    <input className="input" {...register(field.key)}
                      placeholder={`http://your-host:${field.port}`}
                      style={{ fontSize: '0.875rem' }}/>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      {testResult && (
                        <span className={`badge badge--${isUp ? 'success' : 'danger'}`}>
                          {isUp ? '● UP' : '● DOWN'}
                        </span>
                      )}
                      <button type="button" className="btn btn--ghost btn--sm"
                        disabled={isTesting}
                        onClick={handleSubmit(data => testService(field.key, data[field.key]))}>
                        {isTesting ? <span className="spinner spinner--primary"/> : 'Test'}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            <div style={{ display: 'flex', gap: 12, marginTop: 20, justifyContent: 'flex-end' }}>
              <button type="submit" className="btn btn--primary" disabled={!isDirty || isSubmitting}>
                {isSubmitting ? <span className="spinner"/> : '💾 Save Configuration'}
              </button>
            </div>
          </div>

          {/* Token Info */}
          <div className="card">
            <h2 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: 20 }}>🔑 Session Tokens</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 6, fontWeight: 600 }}>ACCESS TOKEN</div>
                <div className="code-block" style={{ wordBreak: 'break-all', fontSize: '0.7rem', maxHeight: 80, overflow: 'auto' }}>
                  {token || 'No token stored'}
                </div>
              </div>
              {decoded && (
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 6, fontWeight: 600 }}>DECODED CLAIMS</div>
                  <pre className="code-block" style={{ fontSize: '0.75rem' }}>
                    {JSON.stringify(decoded, null, 2)}
                  </pre>
                </div>
              )}
              <div style={{
                padding: '10px 14px', borderRadius: 'var(--radius-md)',
                background: decoded?.exp * 1000 > Date.now() ? 'var(--success-light)' : 'var(--danger-light)',
                color: decoded?.exp * 1000 > Date.now() ? 'var(--success)' : 'var(--danger)',
                fontSize: '0.8125rem',
              }}>
                {decoded
                  ? `Token ${decoded.exp * 1000 > Date.now() ? 'valid' : 'EXPIRED'} — expires ${new Date(decoded.exp * 1000).toLocaleString()}`
                  : 'Not logged in'}
              </div>
            </div>
          </div>

          {/* About */}
          <div className="card">
            <h2 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: 20 }}>ℹ️ About</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {[
                ['Frontend', 'React 18 + Vite 5'],
                ['Routing', 'React Router v6'],
                ['State', 'TanStack Query v5'],
                ['Forms', 'React Hook Form'],
                ['HTTP', 'Axios + JWT Interceptors'],
                ['Backend', 'Java 21 + Spring Boot 3'],
                ['Gateway', 'Spring Cloud Gateway'],
                ['Messaging', 'Apache Kafka'],
                ['Database', 'PostgreSQL 16'],
                ['Cache', 'Redis 7'],
              ].map(([k, v]) => (
                <div key={k} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', padding: '6px 0', borderBottom: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-secondary)' }}>{k}</span>
                  <span style={{ fontWeight: 500 }}>{v}</span>
                </div>
              ))}
            </div>
          </div>

        </div>
      </form>
    </div>
  );
}
