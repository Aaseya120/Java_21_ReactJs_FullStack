// src/pages/Payments/PaymentsPage.jsx
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentApi } from '../../api/payment.api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { getServiceErrorMessage } from '../../utils/errorHelper';
import { TableSkeleton } from '../../components/common/Skeleton';

const STATUS_COLOR = {
  COMPLETED: 'success',
  PENDING: 'warning',
  FAILED: 'danger',
  REFUNDED: 'muted',
};

function RefundModal({ payment, onClose, onRefund }) {
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason) return;
    setLoading(true);
    try {
      await onRefund(payment.id, reason);
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal animate-up">
        <div className="modal-header">
          <h2>💸 Issue Refund</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ background: 'var(--bg-elevated)', padding: 12, borderRadius: 8 }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Payment ID</div>
            <div style={{ fontWeight: 600 }}>#{payment.id} (Order: {payment.orderId})</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 8 }}>Amount</div>
            <div style={{ fontWeight: 600, color: 'var(--primary)' }}>${Number(payment.amount).toFixed(2)}</div>
          </div>
          <div className="input-group">
            <label>Reason for Refund *</label>
            <input 
              className="input" 
              placeholder="e.g. Customer requested cancellation" 
              value={reason} 
              onChange={e => setReason(e.target.value)} 
              required
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn--ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn--secondary" disabled={!reason || loading}>
              {loading ? <span className="spinner"/> : 'Submit Refund'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function PaymentsPage() {
  const { user } = useAuth();
  const [searchType, setSearchType] = useState('userId');
  const [searchValue, setSearchValue] = useState(user?.role !== 'ADMIN' ? user?.id || '' : '');
  const [activeQuery, setActiveQuery] = useState({ type: 'userId', value: user?.id || '' });
  const [refundingPayment, setRefundingPayment] = useState(null);
  
  const toast = useToast();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['payments', activeQuery.type, activeQuery.value],
    queryFn: async () => {
      if (!activeQuery.value) return { data: { data: [] } };
      
      try {
        if (activeQuery.type === 'orderId') {
          return await paymentApi.getByOrderId(activeQuery.value);
        } else if (activeQuery.type === 'paymentId') {
          const res = await paymentApi.getById(activeQuery.value);
          // Wrap single payment in array for table
          return { data: { data: res.data?.data ? [res.data.data] : [] } };
        } else {
          return await paymentApi.getByUserId(activeQuery.value);
        }
      } catch (err) {
        if (err.response?.status === 404) return { data: { data: [] } };
        throw err;
      }
    },
    enabled: !!activeQuery.value,
  });

  const rawData = data?.data?.data;
  const payments = Array.isArray(rawData) ? rawData : [];

  const handleSearch = (e) => {
    e.preventDefault();
    if (!searchValue) return;
    setActiveQuery({ type: searchType, value: searchValue });
  };

  const refundMutation = useMutation({
    mutationFn: ({ id, reason }) => paymentApi.refundPayment(id, { reason }),
    onSuccess: () => { 
      toast.success('Refund processed successfully!'); 
      qc.invalidateQueries(['payments']); 
    },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Failed to process refund')),
  });

  return (
    <div className="animate-fade">
      <div className="page-header">
        <div>
          <h1>💳 Payments</h1>
          <p>
            {user?.role === 'ADMIN'
              ? 'Search and manage transactions, issue refunds, and view payment history.'
              : 'View your payment history.'}
          </p>
        </div>
      </div>

      <div className="toolbar">
        {user?.role === 'ADMIN' ? (
          <form className="search-box" style={{ flex: 1, maxWidth: 600, display: 'flex', padding: 0, border: 'none' }} onSubmit={handleSearch}>
            <select 
              className="input" 
              style={{ width: 140, borderTopRightRadius: 0, borderBottomRightRadius: 0, borderRight: 'none', background: 'var(--bg-elevated)' }}
              value={searchType}
              onChange={e => { setSearchType(e.target.value); setSearchValue(''); }}
            >
              <option value="userId">User ID</option>
              <option value="orderId">Order ID</option>
              <option value="paymentId">Payment ID</option>
            </select>
            <div style={{ position: 'relative', flex: 1 }}>
              <span className="search-box__icon">🔍</span>
              <input 
                className="input" 
                style={{ borderTopLeftRadius: 0, borderBottomLeftRadius: 0, paddingLeft: 40 }}
                placeholder={`Search by ${searchType}...`}
                value={searchValue} 
                onChange={e => setSearchValue(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn--primary" style={{ marginLeft: 12 }}>Search</button>
          </form>
        ) : (
          <div style={{ fontWeight: 600, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 8 }}>
            <span>💳 Showing your payment history</span>
            <span className="badge badge--primary" style={{ fontSize: '0.75rem' }}>User ID: {user?.id}</span>
          </div>
        )}
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Payment ID</th>
              <th>Order ID</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Transaction Ref</th>
              <th>Date</th>
              {user?.role === 'ADMIN' && <th>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={user?.role === 'ADMIN' ? 7 : 6} style={{ padding: 20 }}><TableSkeleton rows={3} columns={user?.role === 'ADMIN' ? 7 : 6} /></td></tr>
            ) : payments.length === 0 ? (
              <tr>
                <td colSpan={user?.role === 'ADMIN' ? 7 : 6} style={{ textAlign: 'center', padding: 48 }}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
                    <div style={{ fontSize: '2.5rem' }}>💸</div>
                    <strong style={{ fontSize: '1.1rem', color: 'var(--text-primary)' }}>No payments found</strong>
                    <p style={{ color: 'var(--text-secondary)', maxWidth: 360, fontSize: '0.875rem', margin: 0 }}>
                      {user?.role === 'ADMIN' 
                        ? 'Try searching with a different ID.'
                        : "You haven't made any payments yet."}
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              payments.map(p => (
                <tr key={p.id}>
                  <td><strong>#{p.id}</strong></td>
                  <td><code style={{ color: 'var(--primary)' }}>{p.orderId}</code></td>
                  <td><strong>${Number(p.amount).toFixed(2)}</strong></td>
                  <td>
                    <span className={`badge badge--${STATUS_COLOR[p.status] || 'muted'}`}>
                      {p.status}
                    </span>
                  </td>
                  <td><code style={{ fontSize: '0.7rem' }}>{p.transactionReference || '—'}</code></td>
                  <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {p.createdAt ? new Date(p.createdAt).toLocaleString() : '—'}
                  </td>
                  {user?.role === 'ADMIN' && (
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        {p.status === 'SUCCESS' && (
                          <button 
                            className="btn btn--secondary btn--sm" 
                            title="Refund Payment"
                            onClick={() => setRefundingPayment(p)}
                          >
                            💸 Refund
                          </button>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {refundingPayment && (
        <RefundModal
          payment={refundingPayment}
          onClose={() => setRefundingPayment(null)}
          onRefund={(id, reason) => refundMutation.mutateAsync({ id, reason })}
        />
      )}
    </div>
  );
}
