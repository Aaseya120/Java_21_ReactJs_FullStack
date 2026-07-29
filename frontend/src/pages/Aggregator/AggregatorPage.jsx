// src/pages/Aggregator/AggregatorPage.jsx
import { useState, useRef } from 'react';
import { aggregatorApi } from '../../api/aggregator.api';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';
import { getServiceErrorMessage } from '../../utils/errorHelper';
import { CardSkeleton } from '../../components/common/Skeleton';

function InfoRow({ label, value, mono, highlight }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
      padding: '10px 0', borderBottom: '1px solid var(--border)', gap: 16,
    }}>
      <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 500, flexShrink: 0 }}>{label}</span>
      <span style={{
        fontSize: '0.875rem', fontWeight: highlight ? 700 : 500,
        textAlign: 'right', wordBreak: 'break-all', flex: 1,
        fontFamily: mono ? "'Courier New', monospace" : 'inherit',
        color: highlight ? 'var(--primary)' : mono ? 'var(--text-secondary)' : 'var(--text-primary)',
      }}>{value ?? '—'}</span>
    </div>
  );
}

export default function AggregatorPage() {
  const { user } = useAuth();
  const [orderId, setOrderId] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [responseTime, setResponseTime] = useState(null);
  const [error, setError] = useState(null);
  const toast = useToast();
  const startRef = useRef(null);

  const handleFetch = async () => {
    if (!orderId.trim()) {
      toast.error('Please enter an Order ID');
      return;
    }
    setLoading(true);
    setResult(null);
    setError(null);
    startRef.current = Date.now();
    try {
      const { data } = await aggregatorApi.getOrderDetails(orderId.trim());
      setResult(data.data || data);
      setResponseTime(Date.now() - startRef.current);
      toast.success('Aggregated data fetched!');
    } catch (err) {
      const msg = getServiceErrorMessage(err, 'Order not found or failed to fetch aggregated data');
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const order = (result?.order?.data || result?.order) || (result?.data || result);
  const product = result?.product?.data || result?.product;

  return (
    <div className="animate-fade">
      <div className="page-header">
        <div>
          <h1>Aggregator</h1>
          <p>Fetch combined order + product data in a single call using the CompletableFuture aggregation pattern.</p>
        </div>
        {responseTime && (
          <span className="badge badge--success" style={{ fontSize: '0.8rem' }}>
            ⚡ {responseTime}ms
          </span>
        )}
      </div>

      {/* Architecture Diagram */}
      <div className="card card--glass" style={{ marginBottom: 24, padding: 20 }}>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          How it works
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', fontSize: '0.875rem' }}>
          <span className="badge badge--primary">Client</span>
          <span style={{ color: 'var(--text-muted)' }}>→</span>
          <span className="badge badge--info">API Gateway :8080</span>
          <span style={{ color: 'var(--text-muted)' }}>→</span>
          <span className="badge badge--warning">Order Service :8082</span>
          <span style={{ color: 'var(--text-muted)' }}>→ CompletableFuture →</span>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span className="badge badge--success">Order Service :8082</span>
            <span className="badge badge--success">Product Service :8083</span>
          </div>
          <span style={{ color: 'var(--text-muted)' }}>→</span>
          <span className="badge badge--primary">Merged Response</span>
        </div>
      </div>

      {/* Search Input */}
      <div className="card" style={{ marginBottom: 24 }}>
        <div style={{ fontWeight: 700, marginBottom: 16, fontSize: '1rem' }}>🔗 Fetch Aggregated Order Details</div>
        {user?.role !== 'ADMIN' && (
          <div style={{
            background: 'var(--info-light)', border: '1px solid rgba(59,130,246,0.3)',
            borderRadius: 'var(--radius-md)', padding: '10px 14px', marginBottom: 16,
            fontSize: '0.8125rem', color: 'var(--info)', display: 'flex', alignItems: 'center', gap: 8,
          }}>
            <span>🔒</span>
            <span><strong>IDOR Security Enabled:</strong> Standard users can only search orders belonging to their own User ID ({user?.id}). Admins can search any order ID.</span>
          </div>
        )}
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <input
            className="input"
            style={{ flex: 1, minWidth: 200 }}
            placeholder="Enter Order UUID..."
            value={orderId}
            onChange={e => setOrderId(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleFetch()}
          />
          <button className="btn btn--primary" onClick={handleFetch} disabled={loading}>
            {loading ? <><span className="spinner"/> Fetching...</> : '🔗 Fetch'}
          </button>
        </div>
        <p style={{ marginTop: 12, fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
          Endpoint: <code style={{ color: 'var(--primary)' }}>GET /api/v1/aggregator/order-details/&#123;orderId&#125;</code>
        </p>
      </div>

      {/* Error */}
      {error && (
        <div style={{
          background: 'var(--danger-light)', border: '1px solid rgba(239,68,68,0.3)',
          borderRadius: 'var(--radius-lg)', padding: '16px 20px', marginBottom: 20,
          color: 'var(--danger)', display: 'flex', gap: 10, alignItems: 'center',
        }}>
          <span style={{ fontSize: '1.25rem' }}>⚠️</span>
          <div>
            <strong>Request Failed</strong>
            <div style={{ fontSize: '0.875rem', marginTop: 4, opacity: 0.9 }}>{error}</div>
          </div>
        </div>
      )}

      {/* Loading Skeleton */}
      {loading && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: 20 }}>
          <CardSkeleton lines={6} />
          <CardSkeleton lines={6} />
          <CardSkeleton lines={4} />
        </div>
      )}

      {/* Result */}
      {result && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: 20, animation: 'fadeIn 0.4s ease' }}>
          {/* Order Details */}
          <div className="card" style={{ borderTop: '3px solid var(--warning)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
              <div style={{
                width: 36, height: 36, borderRadius: 10,
                background: 'var(--warning-light)', display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>📋</div>
              <h2 style={{ fontSize: '1rem', fontWeight: 700 }}>Order Details</h2>
              <span className="badge badge--warning" style={{ marginLeft: 'auto' }}>Order Service</span>
            </div>
            {order && <>
              <InfoRow label="Order ID" value={order.id} mono/>
              <InfoRow label="User ID" value={order.userId} mono/>
              <InfoRow label="Product ID" value={order.productId} mono/>
              <InfoRow label="Quantity" value={order.quantity} highlight/>
              <InfoRow label="Total Price" value={`$${Number(order.totalPrice || 0).toFixed(2)}`} highlight/>
              <InfoRow label="Status" value={order.status}/>
              <InfoRow label="Status Desc." value={order.statusDescription}/>
              <InfoRow label="Notes" value={order.notes}/>
              <InfoRow label="Created" value={order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'}/>
            </>}
          </div>

          {/* Product Details */}
          <div className="card" style={{ borderTop: '3px solid var(--success)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
              <div style={{
                width: 36, height: 36, borderRadius: 10,
                background: 'var(--success-light)', display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>📦</div>
              <h2 style={{ fontSize: '1rem', fontWeight: 700 }}>Product Details</h2>
              <span className="badge badge--success" style={{ marginLeft: 'auto' }}>Product Service</span>
            </div>
            {product ? <>
              <InfoRow label="Product ID" value={product.id} mono/>
              <InfoRow label="Name" value={product.name} highlight/>
              <InfoRow label="SKU" value={product.sku} mono/>
              <InfoRow label="Category" value={product.category}/>
              <InfoRow label="Price" value={`$${Number(product.price || 0).toFixed(2)}`} highlight/>
              <InfoRow label="Stock Qty" value={product.stockQty}/>
              <InfoRow label="Availability" value={product.availabilityStatus || (product.stockQty > 0 ? 'IN_STOCK' : 'OUT_OF_STOCK')}/>
              <InfoRow label="Description" value={product.description}/>
            </> : (
              <div className="empty-state" style={{ padding: '20px 0' }}>
                <div className="empty-state__icon">📦</div>
                <div className="empty-state__title">No product data</div>
                <div className="empty-state__desc">Product details not included in response</div>
              </div>
            )}
          </div>

          {/* Raw JSON */}
          <div className="card" style={{ gridColumn: '1 / -1' }}>
            <details>
              <summary style={{ cursor: 'pointer', fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                📄 Raw Response JSON
              </summary>
              <pre className="code-block" style={{ marginTop: 12, maxHeight: 320, overflow: 'auto' }}>
                {JSON.stringify(result, null, 2)}
              </pre>
            </details>
          </div>
        </div>
      )}

      {!result && !error && !loading && (
        <div className="empty-state card" style={{ padding: 60 }}>
          <div style={{ fontSize: '3rem' }}>🔗</div>
          <div className="empty-state__title">Enter an Order ID above</div>
          <div className="empty-state__desc">
            The aggregator will fetch order and product details concurrently from two microservices and combine them into a single response.
          </div>
        </div>
      )}
    </div>
  );
}
