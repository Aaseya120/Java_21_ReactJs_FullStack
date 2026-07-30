// src/pages/Orders/OrdersPage.jsx
import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { ordersApi } from '../../api/orders.api';
import { productsApi } from '../../api/products.api';
import { paymentApi } from '../../api/payment.api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { getServiceErrorMessage } from '../../utils/errorHelper';
import { TableSkeleton } from '../../components/common/Skeleton';
import { PaymentModal } from '../../components/common/PaymentModal';

const refundOrderSchema = z.object({
  reason: z.string().trim().min(5, 'Reason must be at least 5 characters'),
  refundDestination: z.enum(['ORIGINAL_PAYMENT_METHOD', 'STORE_CREDIT']),
  refundAmount: z.union([z.string(), z.number()]).optional().nullable().transform(val => {
    if (val === '' || val === null || val === undefined) return null;
    const num = Number(val);
    return isNaN(num) ? null : num;
  }),
});

function RefundOrderModal({ order, onClose, onSubmit, isAdmin, actionType = 'REQUEST' }) {
  const { register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(refundOrderSchema),
    defaultValues: { reason: '', refundDestination: 'ORIGINAL_PAYMENT_METHOD' },
  });

  useEffect(() => {
    if (!isAdmin) {
      setValue('refundAmount', order.totalPrice);
    }
  }, [isAdmin, order.totalPrice, setValue]);

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal animate-up" role="dialog">
        <div className="modal-header">
          <h2>{actionType === 'REJECT' ? '❌ Reject Refund' : '💸 Issue Refund'}</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit(d => onSubmit(d))} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="input-group">
            <label>Order Number</label>
            <input className="input" value={order.orderNumber || order.id} disabled />
          </div>
          <div className="input-group">
            <label>{actionType === 'REJECT' ? 'Reason for Rejection *' : 'Reason for Refund *'}</label>
            <textarea className={`input ${errors.reason ? 'input--error' : ''}`} rows="3"
              placeholder={actionType === 'REJECT' ? "Reason why refund is denied..." : "e.g. Damaged in transit, customer requested..."}
              {...register('reason')} />
            {errors.reason && <span className="field-error">{errors.reason.message}</span>}
          </div>
          {actionType !== 'REJECT' && (
            <>
              <div className="input-group">
                <label>Refund Destination *</label>
                <select className="input" {...register('refundDestination')}>
                  <option value="ORIGINAL_PAYMENT_METHOD">Original Payment Method</option>
                  <option value="STORE_CREDIT">Store Credit / Wallet</option>
                </select>
              </div>
              <div className="input-group">
                <label>Partial Refund Amount (Optional)</label>
                <input type="number" step="0.01" className="input" placeholder="Leave empty for full refund"
                  {...register('refundAmount')} 
                  readOnly={!isAdmin} 
                  style={!isAdmin ? { background: 'var(--bg-elevated)', cursor: 'not-allowed' } : {}}
                  />
                {!isAdmin && <span className="field-error" style={{color: 'var(--primary)', marginTop: 4}}>Refund amount is locked to full purchase amount for customer requests.</span>}
              </div>
            </>
          )}
          <div className="modal-footer">
            <button type="button" className="btn btn--ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn--danger" disabled={isSubmitting}>
              {isSubmitting ? <span className="spinner"/> : (actionType === 'REJECT' ? 'Submit Rejection' : 'Submit Refund')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

const createOrderSchema = z.object({
  userId: z.coerce.string().trim().min(1, 'User ID is required'),
  productId: z.string().trim().min(1, 'Please select a product'),
  quantity: z.coerce.number().min(1, 'Quantity must be at least 1').max(9999, 'Quantity too large'),
  notes: z.string().optional(),
});

const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUND_REQUESTED', 'REFUND_REJECTED', 'REFUNDED'];
const STATUS_FLOW = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'];

const STATUS_COLOR = {
  PENDING:    'warning',
  CONFIRMED:  'info',
  PROCESSING: 'primary',
  SHIPPED:    'info',
  DELIVERED:  'success',
  CANCELLED:  'danger',
  REFUND_REQUESTED: 'warning',
  REFUND_REJECTED: 'danger',
  REFUNDED:   'muted',
};

function StatusStepper({ currentStatus }) {
  const currentIdx = STATUS_FLOW.indexOf(currentStatus);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 0, margin: '12px 0' }}>
      {STATUS_FLOW.map((s, i) => {
        const done = i <= currentIdx;
        const active = i === currentIdx;
        return (
          <div key={s} style={{ display: 'flex', alignItems: 'center', flex: i < STATUS_FLOW.length - 1 ? 1 : 'none' }}>
            <div style={{
              width: 28, height: 28, borderRadius: '50%', flexShrink: 0,
              background: done ? 'var(--primary)' : 'var(--bg-elevated)',
              border: `2px solid ${done ? 'var(--primary)' : 'var(--border)'}`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '0.65rem', fontWeight: 700,
              color: done ? '#fff' : 'var(--text-muted)',
              boxShadow: active ? '0 0 12px rgba(99,102,241,0.5)' : 'none',
              transition: 'all 0.3s',
            }}>
              {done && i < currentIdx ? '✓' : i + 1}
            </div>
            {i < STATUS_FLOW.length - 1 && (
              <div style={{
                flex: 1, height: 2,
                background: i < currentIdx ? 'var(--primary)' : 'var(--border)',
                transition: 'all 0.3s',
              }}/>
            )}
          </div>
        );
      })}
    </div>
  );
}

function CreateOrderModal({ onClose, onSave }) {
  const { user } = useAuth();
  const { data: productsData } = useQuery({
    queryKey: ['products', 0, ''],
    queryFn: () => productsApi.getAll({ page: 0, size: 50 }),
  });
  const products = productsData?.data?.data?.content || [];

  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(createOrderSchema),
    defaultValues: { userId: user?.id || '', quantity: 1, notes: '', productId: '' },
  });

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const selectedProductId = watch('productId');
  const selectedProduct = products.find(p => String(p.id) === String(selectedProductId));
  const quantity = Number(watch('quantity') || 1);
  const totalPrice = selectedProduct ? (selectedProduct.price * quantity).toFixed(2) : '0.00';

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal animate-up modal--lg" role="dialog" aria-modal="true" aria-labelledby="create-order-title">
        <div className="modal-header">
          <h2 id="create-order-title">📋 New Order</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit(d => onSave({ ...d, totalPrice: Number(totalPrice), quantity: Number(d.quantity) }))}
          style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

          <div className="input-group">
            <label>User ID *</label>
            <input className={`input ${errors.userId ? 'input--error' : ''}`}
              placeholder="User ID placing the order"
              readOnly={user?.role !== 'ADMIN'}
              style={user?.role !== 'ADMIN' ? { background: 'var(--bg-elevated)', cursor: 'not-allowed' } : {}}
              {...register('userId')}/>
            {errors.userId && <span className="field-error">{errors.userId.message}</span>}
            {user?.role !== 'ADMIN' ? (
              <span style={{ fontSize: '0.75rem', color: 'var(--primary)', fontWeight: 600 }}>
                🔒 Ordering as logged-in user ({user?.email || user?.id})
              </span>
            ) : (
              user?.id && (
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Your ID: <code style={{ color: 'var(--primary)' }}>{user.id}</code>
                </span>
              )
            )}
          </div>

          <div className="input-group">
            <label>Select Product *</label>
            <select className={`input ${errors.productId ? 'input--error' : ''}`}
              {...register('productId', { required: 'Product is required' })}>
              <option value="">— Choose a product —</option>
              {products.map(p => (
                <option key={p.id} value={p.id}>
                  {p.name} — ${Number(p.price).toFixed(2)} (Stock: {p.stockQty})
                </option>
              ))}
            </select>
            {errors.productId && <span className="field-error">{errors.productId.message}</span>}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <div className="input-group">
              <label>Quantity *</label>
              <input className="input" type="number" min="1"
                {...register('quantity', { required: true, min: 1 })}/>
            </div>
            <div className="input-group">
              <label>Total Price</label>
              <input className="input" value={`$${totalPrice}`} readOnly
                style={{ opacity: 0.7, cursor: 'not-allowed' }}/>
            </div>
          </div>

          {selectedProduct && (
            <div style={{
              background: 'var(--bg-elevated)', border: '1px solid var(--border)',
              borderRadius: 'var(--radius-md)', padding: '12px 16px',
              fontSize: '0.875rem', color: 'var(--text-secondary)',
            }}>
              📦 <strong style={{ color: 'var(--text-primary)' }}>{selectedProduct.name}</strong>
              &nbsp;· SKU: {selectedProduct.sku} · Category: {selectedProduct.category}
            </div>
          )}

          <div className="input-group">
            <label>Notes</label>
            <input className="input" placeholder="Delivery instructions, special requests..."
              {...register('notes')}/>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn--ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn--primary" disabled={isSubmitting}>
              {isSubmitting ? <span className="spinner"/> : '📋 Place Order'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function OrderDetailModal({ order, onClose, onStatusChange, onRefundSubmit, onApproveRefundSubmit, onRejectRefundSubmit }) {
  const { user } = useAuth();
  const [nextStatus, setNextStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [showRefundModal, setShowRefundModal] = useState(false);
  const [refundActionType, setRefundActionType] = useState('REQUEST');

  const { data: paymentsData } = useQuery({
    queryKey: ['order-payments', order.id],
    queryFn: () => paymentApi.getByOrderId(order.id),
    enabled: !!order,
  });
  const payments = paymentsData?.data?.data || [];

  const handleStatusChange = async (targetStatus) => {
    const statusToApply = targetStatus || nextStatus;
    if (!statusToApply) return;
    setLoading(true);
    try {
      await onStatusChange(order.id, statusToApply);
      onClose();
    } finally {
      setLoading(false);
    }
  };

  const currentIdx = STATUS_FLOW.indexOf(order.status);
  const nextFlowStatus = currentIdx >= 0 && currentIdx < STATUS_FLOW.length - 1 ? STATUS_FLOW[currentIdx + 1] : null;
  const canChange = !['DELIVERED', 'CANCELLED', 'REFUNDED'].includes(order.status);
  const isAdmin = user?.role === 'ADMIN';

  // Customer Grace Period (7 days from updatedAt or createdAt)
  const isCustomer = user?.role !== 'ADMIN';
  const deliveredDate = new Date(order.updatedAt || order.createdAt);
  const gracePeriodActive = (Date.now() - deliveredDate.getTime()) <= 7 * 24 * 60 * 60 * 1000;
  const showCustomerRefund = isCustomer && (order.status === 'DELIVERED' || order.status === 'CANCELLED') && gracePeriodActive;

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      {showRefundModal ? (
        <RefundOrderModal 
          order={order} 
          isAdmin={isAdmin}
          actionType={refundActionType}
          onClose={() => setShowRefundModal(false)}
          onSubmit={async (data) => {
            if (refundActionType === 'REJECT') {
              await onRejectRefundSubmit(order.id, data);
            } else if (isAdmin && order.status === 'REFUND_REQUESTED') {
              await onApproveRefundSubmit(order.id, data);
            } else {
              await onRefundSubmit(order.id, data);
            }
            setShowRefundModal(false);
            onClose();
          }}
        />
      ) : (
      <div className="modal animate-up modal--lg">
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <h2>📋 Order Details & Lifecycle</h2>
            {isAdmin && (
              <span className="badge badge--warning" style={{ fontSize: '0.7rem', fontWeight: 700 }}>
                🛡️ ADMIN CENTER
              </span>
            )}
          </div>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <div style={{ display: 'grid', gap: 16 }}>
          {/* Order Info */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            {[
              ['Order Number', order.orderNumber || order.id],
              ['User ID', order.userId],
              ['Product ID', order.productId],
              ['Quantity', order.quantity],
              ['Total Price', `$${Number(order.totalPrice).toFixed(2)}`],
              ['Created', order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'],
            ].map(([label, value]) => (
              <div key={label} style={{
                background: 'var(--bg-elevated)', borderRadius: 'var(--radius-md)', padding: '12px 14px',
              }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</div>
                <div style={{ fontSize: '0.875rem', wordBreak: 'break-all', fontWeight: 500 }}>{value}</div>
              </div>
            ))}
          </div>

          {/* Refund Progress Stepper */}
          {['REFUND_REQUESTED', 'REFUND_REJECTED', 'REFUNDED'].includes(order.status) ? (
            <div className="card" style={{ padding: 20 }}>
              <div style={{ marginBottom: 12, fontWeight: 600 }}>Refund Progress</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {/* 1. Initiated */}
                <div style={{ display: 'flex', gap: 12 }}>
                  <div style={{ width: 24, height: 24, borderRadius: '50%', background: 'var(--success)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: '0.7rem', marginTop: 2, flexShrink: 0 }}>✓</div>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>Refund Initiated</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Customer requested a refund.</div>
                  </div>
                </div>
                
                {/* 2. Admin Approval */}
                <div style={{ display: 'flex', gap: 12 }}>
                  <div style={{ width: 24, height: 24, borderRadius: '50%', background: order.status === 'REFUND_REQUESTED' ? 'var(--warning)' : (order.status === 'REFUND_REJECTED' ? 'var(--danger)' : 'var(--success)'), display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: '0.7rem', marginTop: 2, flexShrink: 0 }}>
                    {order.status === 'REFUND_REQUESTED' ? '⏳' : (order.status === 'REFUND_REJECTED' ? '✕' : '✓')}
                  </div>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>
                      {order.status === 'REFUND_REQUESTED' ? 'Pending Approval' : (order.status === 'REFUND_REJECTED' ? 'Refund Rejected' : 'Refund Approved')}
                    </div>
                    {order.status === 'REFUND_REJECTED' && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--danger)', marginTop: 4, padding: 8, background: 'rgba(239,68,68,0.1)', borderRadius: 4, borderLeft: '3px solid var(--danger)' }}>
                        <strong>Reason: </strong>{order.notes?.split('| Refund Rejected:').pop() || 'Denied by Admin'}
                      </div>
                    )}
                  </div>
                </div>

                {/* 3. Refund Success */}
                {(order.status === 'REFUND_REQUESTED' || order.status === 'REFUNDED') && (
                  <div style={{ display: 'flex', gap: 12, opacity: order.status === 'REFUNDED' ? 1 : 0.4 }}>
                    <div style={{ width: 24, height: 24, borderRadius: '50%', background: order.status === 'REFUNDED' ? 'var(--success)' : 'var(--bg-elevated)', border: order.status !== 'REFUNDED' ? '2px solid var(--border)' : 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: '0.7rem', marginTop: 2, flexShrink: 0 }}>
                      {order.status === 'REFUNDED' ? '✓' : ''}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>Refund Processed</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Payment gateway processing complete.</div>
                      {order.status === 'REFUNDED' && payments.length > 0 && payments.filter(p => p.status === 'REFUNDED').map(p => (
                         <div key={p.id} style={{ fontSize: '0.75rem', color: 'var(--success)', marginTop: 4 }}>
                           Amount: ${Number(p.amount).toFixed(2)} ({p.paymentMethod})
                         </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : (
            payments.length > 0 && (
              <div className="card" style={{ padding: 20 }}>
                <div style={{ marginBottom: 12, fontWeight: 600 }}>💳 Payment Details</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {payments.map(p => (
                    <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 14px', background: 'var(--bg-elevated)', borderRadius: 'var(--radius-md)' }}>
                      <div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: 2 }}>METHOD</div>
                        <strong style={{ fontSize: '0.85rem' }}>{p.paymentMethod}</strong>
                      </div>
                      <div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: 2 }}>STATUS</div>
                        <span className={`badge badge--${p.status === 'SUCCESS' ? 'success' : p.status === 'REFUNDED' ? 'warning' : 'primary'}`} style={{ fontSize: '0.75rem' }}>
                          {p.status}
                        </span>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: 2 }}>AMOUNT</div>
                        <strong style={{ fontSize: '0.85rem' }}>${Number(p.amount).toFixed(2)}</strong>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )
          )}

          {/* Status Stepper */}
          <div className="card" style={{ padding: 20 }}>
            <div style={{ marginBottom: 12, fontWeight: 600 }}>Order Progress</div>
            <StatusStepper currentStatus={order.status}/>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
              {STATUS_FLOW.map(s => (
                <div key={s} style={{ fontSize: '0.6rem', color: 'var(--text-muted)', textAlign: 'center', flex: 1 }}>
                  {s}
                </div>
              ))}
            </div>
          </div>

          {/* Notes */}
          {order.notes && (
            <div style={{ background: 'var(--bg-elevated)', borderRadius: 'var(--radius-md)', padding: '12px 14px' }}>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: 4 }}>NOTES</div>
              <div style={{ fontSize: '0.875rem' }}>{order.notes}</div>
            </div>
          )}

          {/* Admin Operations Box */}
          {isAdmin ? (
            <div style={{
              background: 'rgba(99, 102, 241, 0.08)',
              border: '1px solid rgba(99, 102, 241, 0.3)',
              borderRadius: 'var(--radius-md)',
              padding: 16,
              display: 'flex',
              flexDirection: 'column',
              gap: 12,
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ fontWeight: 700, color: 'var(--primary)', fontSize: '0.9rem' }}>
                  ⚡ Admin Order Actions & State Override
                </span>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                  All actions logged to LOG_REST audit table
                </span>
              </div>

              {/* One-Click Quick Actions */}
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                {nextFlowStatus && (
                  <button type="button" className="btn btn--primary btn--sm"
                    onClick={() => handleStatusChange(nextFlowStatus)} disabled={loading}>
                    ✓ Advance to {nextFlowStatus}
                  </button>
                )}
                {order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
                  <button type="button" className="btn btn--danger btn--sm"
                    onClick={() => handleStatusChange('CANCELLED')} disabled={loading}>
                    ❌ Reject / Cancel Order
                  </button>
                )}
                {order.status === 'DELIVERED' && (
                  <button type="button" className="btn btn--secondary btn--sm"
                    onClick={() => { setRefundActionType('REQUEST'); setShowRefundModal(true); }} disabled={loading}>
                    💸 Request Refund on Behalf
                  </button>
                )}
                {order.status === 'REFUND_REQUESTED' && (
                  <>
                    <button type="button" className="btn btn--success btn--sm"
                      onClick={() => { setRefundActionType('REQUEST'); setShowRefundModal(true); }} disabled={loading}>
                      ✅ Approve Refund
                    </button>
                    <button type="button" className="btn btn--danger btn--sm"
                      onClick={() => { setRefundActionType('REJECT'); setShowRefundModal(true); }} disabled={loading}>
                      ❌ Reject Refund
                    </button>
                  </>
                )}
              </div>

              {/* Admin Status Override Dropdown */}
              <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginTop: 4 }}>
                <select className="input" value={nextStatus} onChange={e => setNextStatus(e.target.value)}
                  style={{ flex: 1 }}>
                  <option value="">— Force Custom Status Override —</option>
                  {ORDER_STATUSES.map(s => (
                    <option key={s} value={s} disabled={s === order.status}>{s}</option>
                  ))}
                </select>
                <button type="button" className="btn btn--secondary"
                  onClick={() => handleStatusChange(nextStatus)} disabled={!nextStatus || loading}>
                  {loading ? <span className="spinner"/> : 'Override Status'}
                </button>
              </div>
            </div>
          ) : (
            showCustomerRefund && (
              <div className="card" style={{ padding: 20, borderLeft: '4px solid var(--primary)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <strong style={{ display: 'block', color: 'var(--text-primary)' }}>Need to return this item?</strong>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                      You are within the 7-day grace period to request a return or refund.
                    </span>
                  </div>
                  <button className="btn btn--secondary btn--sm" onClick={() => { setRefundActionType('REQUEST'); setShowRefundModal(true); }}>
                    Request Return/Refund
                  </button>
                </div>
              </div>
            )
          )}
        </div>
      </div>
      )}
    </div>
  );
}

export default function OrdersPage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [filterUserId, setFilterUserId] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [payingOrder, setPayingOrder] = useState(null);
  const toast = useToast();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['orders', page, filterUserId, user?.id, user?.role],
    queryFn: () => {
      const targetUserId = filterUserId || (user?.role !== 'ADMIN' ? user?.id : '');
      return targetUserId
        ? ordersApi.getByUserId(targetUserId)
        : ordersApi.getAll({ page, size: 10, sort: 'createdAt' });
    },
    keepPreviousData: true,
    enabled: !!user,
  });

  const rawData = data?.data?.data;
  const orders = Array.isArray(rawData) ? rawData : rawData?.content || [];
  const totalPages = rawData?.totalPages || 1;

  const createMutation = useMutation({
    mutationFn: ordersApi.create,
    onMutate: async (newOrderData) => {
      await qc.cancelQueries(['orders']);
      const previousOrders = qc.getQueryData(['orders', page, filterUserId, user?.id, user?.role]);

      qc.setQueryData(['orders', page, filterUserId, user?.id, user?.role], (old) => {
        const optimisticOrder = {
          id: 'optimistic-' + Date.now(),
          orderNumber: 'ORD-PENDING',
          userId: newOrderData.userId,
          productId: newOrderData.productId,
          quantity: newOrderData.quantity,
          status: 'PENDING',
          totalPrice: '0.00',
          createdAt: new Date().toISOString(),
          isOptimistic: true,
        };
        if (!old || !old.data || !old.data.data) return old;
        const raw = old.data.data;
        if (Array.isArray(raw)) {
          return { ...old, data: { ...old.data, data: [optimisticOrder, ...raw] } };
        } else if (raw.content) {
          return { ...old, data: { ...old.data, data: { ...raw, content: [optimisticOrder, ...raw.content] } } };
        }
        return old;
      });

      setShowCreate(false);
      return { previousOrders };
    },
    onError: (err, newOrderData, context) => {
      if (context?.previousOrders) {
        qc.setQueryData(['orders', page, filterUserId, user?.id, user?.role], context.previousOrders);
      }
      toast.error(getServiceErrorMessage(err, 'Failed to place order. Optimistic update rolled back.'));
    },
    onSuccess: () => {
      toast.success('Order placed successfully!');
    },
    onSettled: () => {
      qc.invalidateQueries(['orders']);
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }) => ordersApi.updateStatus(id, status),
    onSuccess: () => { toast.success('Order status updated!'); qc.invalidateQueries(['orders']); },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Status update failed')),
  });

  const cancelMutation = useMutation({
    mutationFn: ordersApi.cancel,
    onSuccess: () => { toast.success('Order cancelled'); qc.invalidateQueries(['orders']); },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Cancel failed')),
  });

  const refundMutation = useMutation({
    mutationFn: ({ id, data }) => ordersApi.refund(id, data),
    onSuccess: () => { toast.success('Refund requested successfully!'); qc.invalidateQueries(['orders']); },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Refund request failed')),
  });

  const approveRefundMutation = useMutation({
    mutationFn: ({ id, data }) => ordersApi.approveRefund(id, data),
    onSuccess: () => { toast.success('Refund approved successfully!'); qc.invalidateQueries(['orders']); },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Refund approval failed')),
  });

  const rejectRefundMutation = useMutation({
    mutationFn: ({ id, data }) => ordersApi.rejectRefund(id, data),
    onSuccess: () => { toast.success('Refund rejected successfully!'); qc.invalidateQueries(['orders']); },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Refund rejection failed')),
  });

  return (
    <div className="animate-fade">
      <div className="page-header">
        <div>
          <h1>Orders</h1>
          <p>
            {user?.role === 'ADMIN'
              ? 'Manage orders across all customers and track their lifecycle.'
              : 'View and manage your personal orders.'}
          </p>
        </div>
        <button className="btn btn--primary" onClick={() => setShowCreate(true)}>+ New Order</button>
      </div>

      {/* Admin Dashboard Banner & Metrics Center */}
      {user?.role === 'ADMIN' && (
        <div style={{ marginBottom: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{
            background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15) 0%, rgba(168, 85, 247, 0.15) 100%)',
            border: '1px solid rgba(99, 102, 241, 0.35)',
            borderRadius: 'var(--radius-lg)',
            padding: '16px 20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 12
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{
                width: 42, height: 42, borderRadius: '50%',
                background: 'var(--primary)', color: '#fff',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '1.25rem', flexShrink: 0
              }}>
                🛡️
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-primary)' }}>
                  Admin Order Management Center
                </h3>
                <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                  Full authorization active: Modify, advance state, cancel, or refund any customer order across the system.
                </p>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <span className="badge badge--primary" style={{ padding: '6px 12px', fontSize: '0.75rem' }}>
                All Actions Audited (LOG_REST)
              </span>
            </div>
          </div>

          {/* 4 Admin Metric Cards */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: 16
          }}>
            <div className="card" style={{ padding: '16px 20px', borderLeft: '4px solid var(--primary)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                Total Visible Orders
              </div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: 4, color: 'var(--text-primary)' }}>
                {orders.length}
              </div>
            </div>
            <div className="card" style={{ padding: '16px 20px', borderLeft: '4px solid #f59e0b' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                Pending Confirmation
              </div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: 4, color: '#f59e0b' }}>
                {orders.filter(o => o.status === 'PENDING').length}
              </div>
            </div>
            <div className="card" style={{ padding: '16px 20px', borderLeft: '4px solid #3b82f6' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                In Processing / Shipped
              </div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: 4, color: '#3b82f6' }}>
                {orders.filter(o => ['CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(o.status)).length}
              </div>
            </div>
            <div className="card" style={{ padding: '16px 20px', borderLeft: '4px solid #10b981' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                Gross Order Value
              </div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: 4, color: '#10b981' }}>
                ${orders.reduce((sum, o) => sum + (Number(o.totalPrice) || 0), 0).toFixed(2)}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Filter Bar */}
      <div className="toolbar">
        {user?.role === 'ADMIN' ? (
          <>
            <div className="search-box" style={{ flex: 1, maxWidth: 420 }}>
              <span className="search-box__icon">🔍</span>
              <input className="input" placeholder="Filter by User ID (Admin only)..."
                value={filterUserId} onChange={e => { setFilterUserId(e.target.value); setPage(0); }}/>
            </div>
            {user?.id && (
              <button className="btn btn--secondary btn--sm"
                onClick={() => setFilterUserId(uid => uid === user.id ? '' : user.id)}>
                {filterUserId === user.id ? 'Show All Orders' : 'My Orders'}
              </button>
            )}
          </>
        ) : (
          <div style={{ fontWeight: 600, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 8 }}>
            <span>📋 Showing your personal orders</span>
            <span className="badge badge--primary" style={{ fontSize: '0.75rem' }}>User ID: {user?.id}</span>
          </div>
        )}
      </div>

      {/* Table */}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Order ID</th>
              <th>User ID</th>
              <th>Product ID</th>
              <th>Quantity</th>
              <th>Total Price</th>
              <th>Status</th>
              <th>Created At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={8} style={{ padding: 20 }}><TableSkeleton rows={5} columns={8} /></td></tr>
            ) : orders.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: 48 }}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
                    <div style={{ fontSize: '2.5rem' }}>📦</div>
                    <strong style={{ fontSize: '1.1rem', color: 'var(--text-primary)' }}>No orders found</strong>
                    <p style={{ color: 'var(--text-secondary)', maxWidth: 360, fontSize: '0.875rem', margin: 0 }}>
                      There are currently no orders displayed. Ready to create your first order?
                    </p>
                    <button
                      className="btn btn--primary"
                      style={{ marginTop: 8 }}
                      onClick={() => setShowCreate(true)}
                    >
                      + Place New Order
                    </button>
                  </div>
                </td>
              </tr>
            ) : (
              orders.map(o => (
                <tr key={o.id} style={{ cursor: 'pointer' }} onClick={() => setSelectedOrder(o)}>
                  <td><strong style={{ color: 'var(--primary)' }}>{o.orderNumber || '#' + o.id}</strong></td>
                  <td><code>{o.userId}</code></td>
                  <td><code>#{o.productId}</code></td>
                  <td>{o.quantity}</td>
                  <td><strong>${Number(o.totalPrice).toFixed(2)}</strong></td>
                  <td>
                    <span className={`badge badge--${STATUS_COLOR[o.status] || 'muted'}`}>
                      {o.status}
                    </span>
                  </td>
                  <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {o.createdAt ? new Date(o.createdAt).toLocaleDateString() : '—'}
                  </td>
                  <td onClick={e => e.stopPropagation()}>
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                      <button className="btn btn--secondary btn--sm" onClick={() => setSelectedOrder(o)}>
                        {user?.role === 'ADMIN' ? '⚙️ Manage' : 'View'}
                      </button>
                      {user?.role !== 'ADMIN' && o.status === 'PENDING' && (
                        <button
                          type="button"
                          className="btn btn--sm"
                          style={{
                            background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                            color: '#fff',
                            border: 'none',
                            boxShadow: '0 0 10px rgba(16, 185, 129, 0.4)',
                            fontWeight: 700,
                          }}
                          title="Secure PCI-DSS RSA-2048 Checkout"
                          onClick={() => setPayingOrder(o)}
                        >
                          💳 Pay Now
                        </button>
                      )}
                      {user?.role === 'ADMIN' && o.status === 'PENDING' && (
                        <button className="btn btn--primary btn--sm"
                          title="Quick Confirm"
                          onClick={() => statusMutation.mutate({ id: o.id, status: 'CONFIRMED' })}>
                          ⚡ Confirm
                        </button>
                      )}
                      {user?.role === 'ADMIN' && o.status === 'CONFIRMED' && (
                        <button className="btn btn--primary btn--sm"
                          title="Quick Process"
                          onClick={() => statusMutation.mutate({ id: o.id, status: 'PROCESSING' })}>
                          ⚡ Process
                        </button>
                      )}
                      {
                        // Cancel logic:
                        // ADMIN can cancel anything that isn't DELIVERED, CANCELLED, or REFUNDED.
                        // Standard USER can only cancel early stages: PENDING, CONFIRMED, PROCESSING.
                        (
                          (user?.role === 'ADMIN' && !['DELIVERED', 'CANCELLED', 'REFUNDED'].includes(o.status)) ||
                          (user?.role !== 'ADMIN' && ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(o.status))
                        ) && (
                          <button className="btn btn--danger btn--sm"
                            title="Cancel Order"
                            onClick={() => { if (window.confirm('Cancel this order?')) cancelMutation.mutate(o.id); }}>
                            ✕
                          </button>
                        )
                      }
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>‹ Prev</button>
          {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => (
            <button key={i} onClick={() => setPage(i)} className={page === i ? 'active' : ''}>{i + 1}</button>
          ))}
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>Next ›</button>
        </div>
      )}

      {showCreate && (
        <CreateOrderModal
          onClose={() => setShowCreate(false)}
          onSave={(data) => createMutation.mutateAsync(data)}
        />
      )}
      {selectedOrder && (
        <OrderDetailModal
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
          onStatusChange={(id, status) => statusMutation.mutateAsync({ id, status })}
          onRefundSubmit={(id, data) => refundMutation.mutateAsync({ id, data })}
          onApproveRefundSubmit={(id, data) => approveRefundMutation.mutateAsync({ id, data })}
          onRejectRefundSubmit={(id, data) => rejectRefundMutation.mutateAsync({ id, data })}
        />
      )}
      {payingOrder && (
        <PaymentModal
          order={payingOrder}
          onClose={() => setPayingOrder(null)}
          onSuccess={() => {
            setPayingOrder(null);
            qc.invalidateQueries(['orders']);
          }}
        />
      )}
    </div>
  );
}
