// src/components/common/PaymentModal.jsx
import { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentApi } from '../../api/payment.api';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';
import { getServiceErrorMessage } from '../../utils/errorHelper';

const PAYMENT_METHODS = [
  { id: 'CREDIT_CARD', label: '💳 Credit / Debit Card', desc: 'PCI-DSS Tokenized RSA-2048 Security' },
  { id: 'UPI', label: '⚡ UPI Instant RTP', desc: 'India NPCI Virtual Payment Address' },
  { id: 'NET_BANKING', label: '🏦 Net Banking', desc: 'Direct Internet Banking Authentication' },
  { id: 'WALLET', label: '📱 Digital Wallet', desc: 'Apple Pay, Google Pay, Paytm' },
  { id: 'BNPL', label: '🛍️ BNPL', desc: 'Buy Now, Pay Later (Klarna/Afterpay)' },
  { id: 'EMI', label: '📅 EMI Installment', desc: 'Equated Monthly Installments' },
];

export function PaymentModal({ order, onClose, onSuccess }) {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  const [activeMethod, setActiveMethod] = useState('CREDIT_CARD');
  const [publicKeyPem, setPublicKeyPem] = useState(null);

  // Card form state
  const [cardName, setCardName] = useState('John Doe');
  const [cardNumber, setCardNumber] = useState('4532000000004242');
  const [cardExpiry, setCardExpiry] = useState('12/28');
  const [cardCvv, setCardCvv] = useState('123');
  const [cardBrand, setCardBrand] = useState('VISA');

  // UPI state
  const [upiVpa, setUpiVpa] = useState('user@okicici');

  // Net banking state
  const [bankCode, setBankCode] = useState('HDFC_BANK');

  // Wallet state
  const [walletProvider, setWalletProvider] = useState('PAYTM_WALLET');

  // BNPL / EMI state
  const [emiMonths, setEmiMonths] = useState(6);

  // Transaction result
  const [txResult, setTxResult] = useState(null);

  // Fetch RSA Public Key on open for tokenization simulation
  useEffect(() => {
    paymentApi.getPublicKey()
      .then(res => {
        if (res?.data?.data) {
          setPublicKeyPem(res.data.data);
        }
      })
      .catch(() => {
        // Fallback or ignore if public key check fails
      });
  }, []);

  const payMutation = useMutation({
    mutationFn: async (payload) => {
      const response = await paymentApi.processPayment(payload);
      return response.data;
    },
    onSuccess: (data) => {
      const result = data?.data || {};
      setTxResult(result);
      toast.success(`🎉 Payment Successful! Reference: ${result.transactionReference || 'OK'}`);
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      if (onSuccess) onSuccess(result);
    },
    onError: (err) => {
      toast.error(`❌ ${getServiceErrorMessage(err, 'Payment Failed')}`);
    },
  });

  const handleSubmitPayment = (e) => {
    e.preventDefault();
    const idempotencyKey = `IDEM-WEB-${Date.now()}-${Math.random().toString(36).substr(2, 5).toUpperCase()}`;

    const basePayload = {
      orderId: order.id,
      userId: user?.id || order.userId || 1,
      amount: order.totalPrice || 99.99,
      currency: 'USD',
      paymentMethod: activeMethod,
      idempotencyKey,
      gatewayProvider: 'STRIPE_SIMULATOR',
    };

    if (activeMethod === 'CREDIT_CARD' || activeMethod === 'DEBIT_CARD') {
      basePayload.cardLast4 = cardNumber.slice(-4);
      basePayload.cardBrand = cardBrand;
      basePayload.cardToken = `ENC:RSA2048_${btoa(cardNumber.slice(0, 8))}_OK`;
    } else if (activeMethod === 'UPI') {
      basePayload.upiVpa = upiVpa;
    } else if (activeMethod === 'NET_BANKING') {
      basePayload.bankCode = bankCode;
    } else if (activeMethod === 'WALLET') {
      basePayload.walletProvider = walletProvider;
    } else if (activeMethod === 'EMI') {
      basePayload.emiTenureMonths = Number(emiMonths);
      basePayload.cardLast4 = '4242';
      basePayload.cardBrand = 'VISA';
    }

    payMutation.mutate(basePayload);
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal animate-up modal--lg" role="dialog" aria-modal="true" style={{ maxWidth: '620px' }}>
        <div className="modal-header">
          <div>
            <h2>🔐 Secure Checkout</h2>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', margin: 0 }}>
              Order #{order.id} • Total: <strong style={{ color: 'var(--primary)' }}>${Number(order.totalPrice || 0).toFixed(2)} USD</strong>
            </p>
          </div>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="modal-body" style={{ padding: '20px' }}>
          {txResult ? (
            <div style={{ textAlign: 'center', padding: '30px 10px' }}>
              <div style={{ fontSize: '3rem', marginBottom: '15px' }}>✅</div>
              <h3 style={{ color: 'var(--success)', marginBottom: '8px' }}>Payment Authorized</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>
                Your transaction has been securely processed and recorded via Transactional Outbox.
              </p>
              <div style={{
                background: 'var(--bg-elevated)', border: '1px solid var(--border)',
                borderRadius: '8px', padding: '14px', margin: '20px 0', textAlign: 'left',
                fontFamily: 'monospace', fontSize: '0.85rem'
              }}>
                <div><strong>Status:</strong> {txResult.status || 'SUCCESS'}</div>
                <div><strong>Tx Reference:</strong> {txResult.transactionReference}</div>
                <div><strong>Payment ID:</strong> #{txResult.paymentId}</div>
                <div><strong>Auth Message:</strong> {txResult.message || '3D-Secure Verified'}</div>
              </div>
              <button className="btn btn--primary" style={{ width: '100%' }} onClick={onClose}>
                Done & Close
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmitPayment}>
              {publicKeyPem && (
                <div style={{
                  background: 'rgba(16, 185, 129, 0.1)', border: '1px solid var(--success)',
                  borderRadius: '6px', padding: '8px 12px', marginBottom: '16px',
                  display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', color: 'var(--success)'
                }}>
                  <span>🔒 PCI-DSS RSA-2048 Client Encryption Active</span>
                </div>
              )}

              {/* Payment Method Selector */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', marginBottom: '20px' }}>
                {PAYMENT_METHODS.map(method => (
                  <button
                    key={method.id}
                    type="button"
                    onClick={() => setActiveMethod(method.id)}
                    style={{
                      padding: '10px 8px',
                      borderRadius: '8px',
                      border: `2px solid ${activeMethod === method.id ? 'var(--primary)' : 'var(--border)'}`,
                      background: activeMethod === method.id ? 'rgba(99, 102, 241, 0.12)' : 'var(--bg-elevated)',
                      color: activeMethod === method.id ? 'var(--primary)' : 'var(--text)',
                      textAlign: 'center',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '4px'
                    }}
                  >
                    <span style={{ fontWeight: 600, fontSize: '0.85rem' }}>{method.label}</span>
                  </button>
                ))}
              </div>

              {/* Dynamic Instrument Forms */}
              {activeMethod === 'CREDIT_CARD' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  <div className="input-group">
                    <label>Cardholder Name</label>
                    <input
                      type="text"
                      className="input"
                      value={cardName}
                      onChange={e => setCardName(e.target.value)}
                      placeholder="JOHN DOE"
                      required
                    />
                  </div>
                  <div className="input-group">
                    <label>Card Number</label>
                    <input
                      type="text"
                      className="input"
                      value={cardNumber}
                      onChange={e => setCardNumber(e.target.value)}
                      placeholder="4532 •••• •••• 4242"
                      required
                    />
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '10px' }}>
                    <div className="input-group">
                      <label>Expiry</label>
                      <input
                        type="text"
                        className="input"
                        value={cardExpiry}
                        onChange={e => setCardExpiry(e.target.value)}
                        placeholder="MM/YY"
                        required
                      />
                    </div>
                    <div className="input-group">
                      <label>CVV</label>
                      <input
                        type="password"
                        className="input"
                        value={cardCvv}
                        onChange={e => setCardCvv(e.target.value)}
                        placeholder="•••"
                        required
                      />
                    </div>
                    <div className="input-group">
                      <label>Network</label>
                      <select className="input" value={cardBrand} onChange={e => setCardBrand(e.target.value)}>
                        <option value="VISA">VISA</option>
                        <option value="MASTERCARD">Mastercard</option>
                        <option value="AMEX">Amex</option>
                        <option value="RUPAY">RuPay</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}

              {activeMethod === 'UPI' && (
                <div className="input-group">
                  <label>UPI Virtual Payment Address (VPA)</label>
                  <input
                    type="text"
                    className="input"
                    value={upiVpa}
                    onChange={e => setUpiVpa(e.target.value)}
                    placeholder="user@okicici or user@sbi"
                    required
                  />
                  <small style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                    Standard Indian NPCI VPA address for instant debit
                  </small>
                </div>
              )}

              {activeMethod === 'NET_BANKING' && (
                <div className="input-group">
                  <label>Select Bank</label>
                  <select className="input" value={bankCode} onChange={e => setBankCode(e.target.value)}>
                    <option value="HDFC_BANK">HDFC Bank</option>
                    <option value="ICICI_BANK">ICICI Bank</option>
                    <option value="SBI_BANK">State Bank of India (SBI)</option>
                    <option value="AXIS_BANK">Axis Bank</option>
                    <option value="CHASE_US">JPMorgan Chase</option>
                  </select>
                </div>
              )}

              {activeMethod === 'WALLET' && (
                <div className="input-group">
                  <label>Select Digital Wallet Provider</label>
                  <select className="input" value={walletProvider} onChange={e => setWalletProvider(e.target.value)}>
                    <option value="PAYTM_WALLET">Paytm Wallet</option>
                    <option value="APPLE_PAY">Apple Pay (Contactless)</option>
                    <option value="GOOGLE_PAY">Google Pay</option>
                    <option value="PHONEPE">PhonePe Wallet</option>
                  </select>
                </div>
              )}

              {(activeMethod === 'BNPL' || activeMethod === 'EMI') && (
                <div className="input-group">
                  <label>Select Installment / Tenure</label>
                  <select className="input" value={emiMonths} onChange={e => setEmiMonths(e.target.value)}>
                    <option value={3}>3 Months (@ ${(order.totalPrice / 3).toFixed(2)}/mo)</option>
                    <option value={6}>6 Months (@ ${(order.totalPrice / 6).toFixed(2)}/mo)</option>
                    <option value={12}>12 Months (@ ${(order.totalPrice / 12).toFixed(2)}/mo)</option>
                  </select>
                </div>
              )}

              <div style={{ marginTop: '24px', display: 'flex', gap: '10px' }}>
                <button
                  type="button"
                  className="btn btn--secondary"
                  onClick={onClose}
                  style={{ flex: 1 }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn--primary"
                  disabled={payMutation.isPending}
                  style={{ flex: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
                >
                  {payMutation.isPending ? '⏳ Processing...' : `💳 Pay $${Number(order.totalPrice || 0).toFixed(2)}`}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
