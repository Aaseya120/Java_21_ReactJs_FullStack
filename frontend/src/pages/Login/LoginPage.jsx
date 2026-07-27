// src/pages/Login/LoginPage.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { authApi } from '../../api/auth.api';
import '../../styles/globals.css';

const loginSchema = z.object({
  email: z.string().trim().min(1, 'Email Address or User ID is required'),
  password: z.string().min(1, 'Password is required'),
});

const registerSchema = z.object({
  fullName: z.string().trim().min(2, 'Must be at least 2 characters'),
  email: z.string().trim().email('Please enter a valid email address'),
  mobileNumber: z.string().trim().regex(/^[0-9+-\s()]{7,20}$/, 'Enter a valid mobile number (7-20 digits)').optional().or(z.literal('')),
  password: z.string().min(8, 'Must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

export default function LoginPage() {
  const [tab, setTab] = useState('login');
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [showRegPassword, setShowRegPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState({ score: 0, label: '', color: '' });
  
  // New state for Registration Success confirmation screen (no auto-login)
  const [registeredUser, setRegisteredUser] = useState(null);

  // New state for User ID Recovery Modal ("Forgot User ID?")
  const [showRecoveryModal, setShowRecoveryModal] = useState(false);
  const [recoveryContact, setRecoveryContact] = useState('');
  const [recoveredUser, setRecoveredUser] = useState(null);
  const [recovering, setRecovering] = useState(false);
  
  const { login, registerWithoutLogin } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const loginForm = useForm({
    resolver: zodResolver(loginSchema),
  });
  const registerForm = useForm({
    resolver: zodResolver(registerSchema),
  });

  const calculateStrength = (pwd) => {
    if (!pwd) return { score: 0, label: '', color: 'transparent' };
    let score = 0;
    if (pwd.length >= 8) score++;
    if (pwd.length >= 12) score++;
    if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;

    if (score <= 2) return { score: 33, label: 'Weak', color: 'var(--danger)' };
    if (score <= 3) return { score: 66, label: 'Good', color: 'var(--warning)' };
    return { score: 100, label: 'Strong', color: 'var(--success)' };
  };

  const onLogin = async (data) => {
    try {
      await login(data);
      toast.success('Welcome back!');
      navigate('/');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed. Check your Email or User ID and Password.');
    }
  };

  const onRegister = async (data) => {
    try {
      const userResult = await registerWithoutLogin({
        fullName: data.fullName,
        email: data.email,
        mobileNumber: data.mobileNumber || '',
        password: data.password,
      });
      setRegisteredUser(userResult);
      toast.success('Registration successful! Please save your User ID.');
      registerForm.reset();
    } catch (err) {
      const serverMsg = err.response?.data?.errorDesc || err.response?.data?.message;
      const httpStatus = err.response?.status;
      const networkErr = !err.response ? `Network error — is the gateway running? (${err.message})` : null;
      toast.error(networkErr || serverMsg || `Registration failed (HTTP ${httpStatus}).`);
      console.error('[Register error]', err);
    }
  };

  const handleRecoverId = async (e) => {
    e.preventDefault();
    if (!recoveryContact.trim()) return;
    setRecovering(true);
    setRecoveredUser(null);
    try {
      const { data } = await authApi.recoverId(recoveryContact.trim());
      setRecoveredUser(data.data);
      toast.success('User ID recovered successfully!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'No account found matching that Email or Mobile Number.');
    } finally {
      setRecovering(false);
    }
  };

  return (
    <div style={{
      height: '100vh',
      display: 'flex',
      background: 'var(--bg-base)',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Animated background blobs */}
      <div style={{
        position: 'absolute', width: 600, height: 600,
        background: 'radial-gradient(circle, rgba(99,102,241,0.15) 0%, transparent 70%)',
        top: '-200px', left: '-200px', borderRadius: '50%',
        animation: 'pulse 4s ease-in-out infinite',
      }}/>
      <div style={{
        position: 'absolute', width: 400, height: 400,
        background: 'radial-gradient(circle, rgba(16,185,129,0.1) 0%, transparent 70%)',
        bottom: '-100px', right: '-100px', borderRadius: '50%',
        animation: 'pulse 6s ease-in-out infinite',
      }}/>

      {/* Left Panel — Branding */}
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
        padding: '24px', background: 'linear-gradient(135deg, rgba(99,102,241,0.08) 0%, rgba(16,185,129,0.05) 100%)',
        borderRight: '1px solid var(--border)',
      }} className="animate-fade login-brand-panel" id="login-brand">
        <div style={{ width: '100%', maxWidth: 440, height: 480, padding: '24px 32px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div style={{ fontSize: '1.75rem', marginBottom: '4px' }}>☕</div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800, lineHeight: 1.1, marginBottom: '8px' }}>
            Java 21<br/>Microservices<br/>
            <span style={{ color: 'var(--primary)' }}>Dashboard</span>
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', lineHeight: 1.4, marginBottom: 0 }}>
            Production-ready monitoring and management for all your microservices.
            Register, authenticate, manage products, orders, and real-time notifications.
          </p>
          {/* Dynamic Architecture Diagram */}
          <div className="login-diagram-container" style={{ marginTop: 12, position: 'relative', width: '100%' }}>
          <style>{`
            .arch-node {
              background: var(--bg-elevated);
              border: 1px solid var(--border);
              border-radius: var(--radius-md);
              padding: 6px 10px;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              position: relative;
              z-index: 2;
              box-shadow: var(--shadow-sm);
              transition: all 0.3s ease;
            }
            .arch-node:hover {
              border-color: var(--primary);
              box-shadow: 0 0 15px rgba(99,102,241,0.3);
              transform: translateY(-2px);
            }
            .arch-title { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); text-align: center; }
            .arch-sub { font-size: 0.65rem; color: var(--primary); font-family: monospace; margin-top: 2px; }
            
            .arch-line-v {
              width: 2px;
              background: var(--border);
              margin: 0 auto;
              position: relative;
            }
            .arch-line-h {
              height: 2px;
              background: var(--border);
              width: 80%;
              margin: 0 auto;
              position: relative;
            }

            @keyframes data-flow-down {
              0% { top: 0; opacity: 0; }
              10% { opacity: 1; }
              90% { opacity: 1; }
              100% { top: 100%; opacity: 0; }
            }
            
            @keyframes data-flow-up {
              0% { bottom: 0; opacity: 0; }
              10% { opacity: 1; }
              90% { opacity: 1; }
              100% { bottom: 100%; opacity: 0; }
            }

            @keyframes data-flow-right {
              0% { left: 0; opacity: 0; }
              10% { opacity: 1; }
              90% { opacity: 1; }
              100% { left: 100%; opacity: 0; }
            }

            .packet-down {
              position: absolute; width: 6px; height: 6px; border-radius: 50%;
              background: var(--primary); box-shadow: 0 0 8px var(--primary);
              left: -2px; animation: data-flow-down 1.5s linear infinite;
            }
            .packet-up {
              position: absolute; width: 6px; height: 6px; border-radius: 50%;
              background: var(--success); box-shadow: 0 0 8px var(--success);
              left: -2px; animation: data-flow-up 1.5s linear infinite 0.75s;
            }
            .packet-right {
              position: absolute; width: 6px; height: 6px; border-radius: 50%;
              background: var(--primary); box-shadow: 0 0 8px var(--primary);
              top: -2px; animation: data-flow-right 2s linear infinite;
            }
          `}</style>

          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            {/* Client */}
            <div className="arch-node" style={{ width: 140 }}>
              <div className="arch-title">⚛️ React Frontend</div>
              <div className="arch-sub">Port 3000</div>
            </div>

            {/* Connection to Gateway */}
            <div className="arch-line-v" style={{ height: 16 }}>
              <div className="packet-down" />
              <div className="packet-up" />
            </div>

            {/* API Gateway */}
            <div className="arch-node" style={{ width: 180, borderColor: 'var(--primary)' }}>
              <div className="arch-title">🚪 API Gateway</div>
              <div className="arch-sub">Spring Cloud • Port 8080</div>
            </div>

            {/* Connection to Microservices */}
            <div className="arch-line-v" style={{ height: 12 }}>
              <div className="packet-down" style={{ animationDuration: '2s' }}/>
            </div>
            
            {/* Horizontal Bus */}
            <div className="arch-line-h">
              <div className="packet-right" />
              <div className="packet-right" style={{ animationDelay: '1s', background: 'var(--success)', boxShadow: '0 0 8px var(--success)' }} />
            </div>
            
            {/* Vertical drops to services */}
            <div style={{ display: 'flex', justifyContent: 'space-between', width: '85%', margin: '0 auto' }}>
              <div className="arch-line-v" style={{ height: 12 }}><div className="packet-down" style={{ animationDelay: '0.2s' }}/></div>
              <div className="arch-line-v" style={{ height: 12 }}><div className="packet-down" style={{ animationDelay: '0.4s' }}/></div>
              <div className="arch-line-v" style={{ height: 12 }}><div className="packet-down" style={{ animationDelay: '0.6s' }}/></div>
              <div className="arch-line-v" style={{ height: 12 }}><div className="packet-down" style={{ animationDelay: '0.8s' }}/></div>
            </div>

            {/* Microservices */}
            <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', gap: 6 }}>
              <div className="arch-node" style={{ flex: 1, padding: '10px 2px' }}>
                <div className="arch-title" style={{ fontSize: '0.65rem' }}>👤 User SVC</div>
                <div className="arch-sub" style={{ fontSize: '0.55rem' }}>8081 • JWT</div>
              </div>
              <div className="arch-node" style={{ flex: 1, padding: '10px 2px' }}>
                <div className="arch-title" style={{ fontSize: '0.65rem' }}>📦 Prod SVC</div>
                <div className="arch-sub" style={{ fontSize: '0.55rem' }}>8082 • SQL</div>
              </div>
              <div className="arch-node" style={{ flex: 1, padding: '10px 2px' }}>
                <div className="arch-title" style={{ fontSize: '0.65rem' }}>🛒 Order SVC</div>
                <div className="arch-sub" style={{ fontSize: '0.55rem' }}>8083 • SQL</div>
              </div>
              <div className="arch-node" style={{ flex: 1, padding: '10px 2px' }}>
                <div className="arch-title" style={{ fontSize: '0.65rem' }}>🔔 Notif SVC</div>
                <div className="arch-sub" style={{ fontSize: '0.55rem' }}>8084 • Kafka</div>
              </div>
            </div>

            {/* Connection to Infra */}
            <div style={{ display: 'flex', justifyContent: 'space-between', width: '85%', margin: '0 auto' }}>
              <div className="arch-line-v" style={{ height: 6 }} />
              <div className="arch-line-v" style={{ height: 6 }} />
              <div className="arch-line-v" style={{ height: 6 }} />
              <div className="arch-line-v" style={{ height: 6 }} />
            </div>

            {/* Infrastructure Base Layer */}
            <div style={{
              width: '100%',
              background: 'rgba(255,255,255,0.02)',
              border: '1px dashed var(--border)',
              borderRadius: 'var(--radius-md)',
              padding: '8px 4px',
              display: 'flex',
              justifyContent: 'space-between',
              gap: 6
            }}>
              <div style={{ flex: 1, textAlign: 'center', fontSize: '0.65rem', color: 'var(--text-muted)', fontWeight: 600 }}>🐘 Postgres</div>
              <div style={{ flex: 1, textAlign: 'center', fontSize: '0.65rem', color: 'var(--text-muted)', fontWeight: 600 }}>🔴 Redis</div>
              <div style={{ flex: 1, textAlign: 'center', fontSize: '0.65rem', color: 'var(--text-muted)', fontWeight: 600 }}>⚡ Kafka</div>
              <div style={{ flex: 1, textAlign: 'center', fontSize: '0.65rem', color: 'var(--text-muted)', fontWeight: 600 }}>🔐 Keycloak</div>
            </div>
          </div>
        </div>

        <div style={{ marginTop: 16, fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          🔒 Secured with 256-bit JWT & Spring Security 6 Stateless Authentication
        </div>
        </div>
      </div>

      {/* Right Panel — Form */}
      <div className="login-form-wrapper" style={{
        flex: 1, display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
        padding: '12px',
      }}>
        <div className="card animate-up login-form-card" style={{ width: '100%', maxWidth: 440, height: 480, display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: '24px 32px', boxShadow: '0 25px 50px -12px rgba(0,0,0,0.25)' }}>
          {/* Logo small */}
          <div style={{ textAlign: 'center', marginBottom: 12 }}>
            <div style={{
              width: 36, height: 36, borderRadius: 10,
              background: 'linear-gradient(135deg, #6366f1, #818cf8)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '1rem', margin: '0 auto 6px',
            }}>☕</div>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 800 }}>
              {registeredUser ? '🎉 Registration Successful!' : tab === 'login' ? 'Welcome Back' : 'Create Your Account'}
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: 2 }}>
              {registeredUser
                ? 'Your account has been created securely. Please note down your User ID below.'
                : tab === 'login'
                ? 'Sign in with your Email Address or User ID'
                : 'Get started with Java 21 Microservices in seconds'}
            </p>
          </div>

          {!registeredUser && (
            /* Tabs */
            <div style={{
              display: 'flex', background: 'var(--bg-elevated)',
              borderRadius: 8, padding: 4, marginBottom: 12,
            }}>
              {['login', 'register'].map(t => (
                <button key={t} type="button" onClick={() => { setTab(t); setRegisteredUser(null); }} style={{
                  flex: 1, padding: '9px', border: 'none',
                  borderRadius: 8, fontWeight: 600, fontSize: '0.875rem',
                  transition: 'all 0.2s',
                  background: tab === t ? 'var(--primary)' : 'transparent',
                  color: tab === t ? '#fff' : 'var(--text-secondary)',
                  cursor: 'pointer',
                }}>
                  {t === 'login' ? '🔑 Sign In' : '✨ Register'}
                </button>
              ))}
            </div>
          )}

          {registeredUser ? (
            /* Registration Success Confirmation Screen (No auto-login) */
            <div className="animate-up" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div style={{
                background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(99, 102, 241, 0.15))',
                border: '1px solid rgba(16, 185, 129, 0.4)',
                borderRadius: 'var(--radius-lg)',
                padding: '24px 20px',
                textAlign: 'center',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 12
              }}>
                <div style={{
                  fontSize: '0.75rem',
                  color: 'var(--text-muted)',
                  textTransform: 'uppercase',
                  fontWeight: 700,
                  letterSpacing: '0.08em'
                }}>
                  YOUR ASSIGNED USER ID
                </div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  background: 'var(--bg-base)',
                  padding: '10px 18px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border)'
                }}>
                  <strong style={{ fontSize: '1.75rem', color: 'var(--primary)', fontFamily: 'monospace' }}>
                    #{registeredUser.id}
                  </strong>
                  <button
                    type="button"
                    className="btn btn--secondary btn--sm"
                    onClick={() => {
                      navigator.clipboard.writeText(String(registeredUser.id));
                      toast.success('User ID copied to clipboard!');
                    }}
                    title="Copy User ID"
                  >
                    📋 Copy
                  </button>
                </div>

                <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: 4 }}>
                  <div><strong>Email:</strong> {registeredUser.email}</div>
                  {registeredUser.mobileNumber && (
                    <div><strong>Mobile:</strong> {registeredUser.mobileNumber}</div>
                  )}
                </div>
              </div>

              <div style={{
                background: 'var(--bg-elevated)',
                padding: 16,
                borderRadius: 'var(--radius-md)',
                fontSize: '0.85rem',
                color: 'var(--text-secondary)',
                lineHeight: 1.6,
                borderLeft: '4px solid var(--primary)'
              }}>
                ℹ️ <strong>Please Note:</strong> Your account has been created successfully! Please save your User ID (<strong>#{registeredUser.id}</strong>) in a secure place. You can log in using either your assigned <strong>User ID</strong> or your <strong>Email Address</strong>.
                <br /><br />
                <em>Forgot your User ID?</em> You can easily recover it on the login page using your registered Email Address or Mobile Number.
              </div>

              <button
                type="button"
                className="btn btn--primary btn--lg w-full"
                onClick={() => {
                  setRegisteredUser(null);
                  setTab('login');
                }}
              >
                Go to Login Page →
              </button>

              <div style={{ textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary)', marginTop: 4 }}>
                Ready to sign into your account?{' '}
                <a
                  href="#login"
                  onClick={(e) => {
                    e.preventDefault();
                    setRegisteredUser(null);
                    setTab('login');
                  }}
                  style={{ color: 'var(--primary)', fontWeight: 600, textDecoration: 'underline', cursor: 'pointer' }}
                >
                  Login here
                </a>
              </div>
            </div>
          ) : tab === 'login' ? (
            <form onSubmit={loginForm.handleSubmit(onLogin)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="input-group">
                <label style={{ fontWeight: 600 }}>Email Address or User ID</label>
                <input className={`input ${loginForm.formState.errors.email ? 'input--error' : ''}`}
                  type="text" placeholder="john.doe@example.com or User ID (e.g. 1)"
                  {...loginForm.register('email', { required: 'Email Address or User ID is required' })}/>
                {loginForm.formState.errors.email && <span className="field-error">{loginForm.formState.errors.email.message}</span>}
              </div>
              
              <div className="input-group">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <label style={{ fontWeight: 600 }}>Password</label>
                  <button
                    type="button"
                    onClick={() => { setShowRecoveryModal(true); setRecoveredUser(null); setRecoveryContact(''); }}
                    style={{ background: 'none', border: 'none', color: 'var(--primary)', fontSize: '0.75rem', fontWeight: 600, cursor: 'pointer' }}
                  >
                    Forgot User ID?
                  </button>
                </div>
                <div style={{ position: 'relative', display: 'flex' }}>
                  <input className={`input ${loginForm.formState.errors.password ? 'input--error' : ''}`}
                    type={showLoginPassword ? 'text' : 'password'} placeholder="••••••••"
                    style={{ width: '100%', paddingRight: '40px' }}
                    {...loginForm.register('password', { required: 'Password is required' })}/>
                  <button type="button" onClick={() => setShowLoginPassword(!showLoginPassword)} style={{
                    position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
                    background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.1rem',
                    color: 'var(--text-muted)',
                  }}>
                    {showLoginPassword ? '👁️' : '👁️‍🗨️'}
                  </button>
                </div>
                {loginForm.formState.errors.password && <span className="field-error">{loginForm.formState.errors.password.message}</span>}
              </div>

              <button type="submit" className="btn btn--primary btn--lg w-full" style={{ marginTop: 8 }}
                disabled={loginForm.formState.isSubmitting}>
                {loginForm.formState.isSubmitting ? <><span className="spinner"/> Signing In...</> : '🔑 Sign In'}
              </button>

              <div style={{ textAlign: 'center', marginTop: 4 }}>
                <button
                  type="button"
                  onClick={() => { setShowRecoveryModal(true); setRecoveredUser(null); setRecoveryContact(''); }}
                  className="btn btn--secondary btn--sm w-full"
                >
                  🔍 Recover Forgotten User ID
                </button>
              </div>
            </form>
          ) : (
            <form onSubmit={registerForm.handleSubmit(onRegister)} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div className="input-group">
                <label style={{ fontWeight: 600 }}>Full Name</label>
                <input className={`input ${registerForm.formState.errors.fullName ? 'input--error' : ''}`}
                  type="text" placeholder="John Doe"
                  {...registerForm.register('fullName', {
                    required: 'Full name is required',
                    minLength: { value: 2, message: 'Must be at least 2 characters' }
                  })}/>
                {registerForm.formState.errors.fullName && <span className="field-error">{registerForm.formState.errors.fullName.message}</span>}
              </div>

              <div className="input-group">
                <label style={{ fontWeight: 600 }}>Email Address</label>
                <input className={`input ${registerForm.formState.errors.email ? 'input--error' : ''}`}
                  type="email" placeholder="john.doe@example.com"
                  {...registerForm.register('email', {
                    required: 'Email is required',
                    pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Please enter a valid email address' }
                  })}/>
                {registerForm.formState.errors.email && <span className="field-error">{registerForm.formState.errors.email.message}</span>}
              </div>

              <div className="input-group">
                <label style={{ fontWeight: 600 }}>Mobile Number (optional - for User ID recovery)</label>
                <input className={`input ${registerForm.formState.errors.mobileNumber ? 'input--error' : ''}`}
                  type="tel" placeholder="+1 (555) 012-3456 or digits"
                  {...registerForm.register('mobileNumber', {
                    pattern: { value: /^[0-9+-\s()]{7,20}$/, message: 'Enter a valid mobile number (7-20 digits)' }
                  })}/>
                {registerForm.formState.errors.mobileNumber && <span className="field-error">{registerForm.formState.errors.mobileNumber.message}</span>}
              </div>

              <div className="input-group">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <label style={{ fontWeight: 600 }}>Password</label>
                  {passwordStrength.label && (
                    <span style={{ fontSize: '0.75rem', fontWeight: 700, color: passwordStrength.color }}>
                      {passwordStrength.label}
                    </span>
                  )}
                </div>
                <div style={{ position: 'relative', display: 'flex' }}>
                  <input className={`input ${registerForm.formState.errors.password ? 'input--error' : ''}`}
                    type={showRegPassword ? 'text' : 'password'} placeholder="Min 8 characters (letters & digits)"
                    style={{ width: '100%', paddingRight: '40px' }}
                    {...registerForm.register('password', {
                      required: 'Password is required',
                      minLength: { value: 8, message: 'Must be at least 8 characters' },
                      onChange: (e) => setPasswordStrength(calculateStrength(e.target.value))
                    })}/>
                  <button type="button" onClick={() => setShowRegPassword(!showRegPassword)} style={{
                    position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
                    background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.1rem',
                    color: 'var(--text-muted)',
                  }}>
                    {showRegPassword ? '👁️' : '👁️‍🗨️'}
                  </button>
                </div>
                {/* Password Strength Meter Bar */}
                {passwordStrength.score > 0 && (
                  <div style={{ height: 4, background: 'var(--border)', borderRadius: 2, marginTop: 6, overflow: 'hidden' }}>
                    <div style={{
                      height: '100%', width: `${passwordStrength.score}%`,
                      background: passwordStrength.color, transition: 'width 0.3s ease, background 0.3s ease',
                    }}/>
                  </div>
                )}
                {registerForm.formState.errors.password && <span className="field-error">{registerForm.formState.errors.password.message}</span>}
              </div>

              <div className="input-group">
                <label style={{ fontWeight: 600 }}>Confirm Password</label>
                <div style={{ position: 'relative', display: 'flex' }}>
                  <input className={`input ${registerForm.formState.errors.confirmPassword ? 'input--error' : ''}`}
                    type={showConfirmPassword ? 'text' : 'password'} placeholder="Re-enter your password"
                    style={{ width: '100%', paddingRight: '40px' }}
                    {...registerForm.register('confirmPassword', {
                      required: 'Please confirm your password',
                      validate: (val) => val === registerForm.watch('password') || 'Passwords do not match'
                    })}/>
                  <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} style={{
                    position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
                    background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.1rem',
                    color: 'var(--text-muted)',
                  }}>
                    {showConfirmPassword ? '👁️' : '👁️‍🗨️'}
                  </button>
                </div>
                {registerForm.formState.errors.confirmPassword && <span className="field-error">{registerForm.formState.errors.confirmPassword.message}</span>}
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 4 }}>
                <input type="checkbox" id="terms" {...registerForm.register('terms', {
                  required: 'You must agree to the Terms & Privacy Policy'
                })} style={{ width: 16, height: 16, cursor: 'pointer', accentColor: 'var(--primary)' }}/>
                <label htmlFor="terms" style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                  I agree to the <strong style={{ color: 'var(--text-primary)' }}>Terms of Service</strong> & <strong style={{ color: 'var(--text-primary)' }}>Privacy Policy</strong>
                </label>
              </div>
              {registerForm.formState.errors.terms && <span className="field-error">{registerForm.formState.errors.terms.message}</span>}

              <button type="submit" className="btn btn--primary btn--lg w-full" style={{ marginTop: 8 }}
                disabled={registerForm.formState.isSubmitting}>
                {registerForm.formState.isSubmitting ? <><span className="spinner"/> Creating Account...</> : '✨ Create Account'}
              </button>
            </form>
          )}

          <div style={{ textAlign: 'center', marginTop: 20, fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {registeredUser ? (
              <>
                🔒 Your User ID is permanently assigned to your profile
              </>
            ) : tab === 'login' ? (
              <>
                Default demo login: <code 
                  onClick={() => { loginForm.setValue('email', 'john.doe@example.com'); loginForm.setFocus('password'); }}
                  style={{ color: 'var(--primary)', fontWeight: 600, cursor: 'pointer', textDecoration: 'underline' }}
                  title="Click to auto-fill"
                >john.doe@example.com</code> or ID <code 
                  onClick={() => { loginForm.setValue('email', '1'); loginForm.setFocus('password'); }}
                  style={{ color: 'var(--primary)', fontWeight: 600, cursor: 'pointer', textDecoration: 'underline' }}
                  title="Click to auto-fill"
                >1</code>
              </>
            ) : (
              <>
                ✨ Your account will be instantly active with JWT stateless auth
              </>
            )}
          </div>
        </div>
      </div>

      {/* Forgot User ID Recovery Modal */}
      {showRecoveryModal && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setShowRecoveryModal(false)}>
          <div className="modal animate-up" style={{ maxWidth: 440 }}>
            <div className="modal-header">
              <h2>🔍 Recover Your User ID</h2>
              <button className="modal-close" onClick={() => setShowRecoveryModal(false)}>✕</button>
            </div>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: 16 }}>
              Enter the Email Address or Mobile Number you used during registration. We will look up your assigned User ID.
            </p>

            <form onSubmit={handleRecoverId} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div className="input-group">
                <label style={{ fontWeight: 600 }}>Email Address or Mobile Number</label>
                <input
                  className="input"
                  type="text"
                  placeholder="e.g. john.doe@example.com or mobile number"
                  value={recoveryContact}
                  onChange={e => setRecoveryContact(e.target.value)}
                  required
                />
              </div>

              {recoveredUser && (
                <div className="animate-up" style={{
                  background: 'rgba(16, 185, 129, 0.12)',
                  border: '1px solid rgba(16, 185, 129, 0.4)',
                  borderRadius: 'var(--radius-md)',
                  padding: 16,
                  textAlign: 'center',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8
                }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                    YOUR RECOVERED USER ID
                  </div>
                  <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#10b981', fontFamily: 'monospace' }}>
                    #{recoveredUser.id}
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                    <strong>Name:</strong> {recoveredUser.fullName}<br />
                    <strong>Email:</strong> {recoveredUser.email}
                  </div>
                </div>
              )}

              <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
                <button
                  type="button"
                  className="btn btn--secondary"
                  onClick={() => setShowRecoveryModal(false)}
                  style={{ flex: 1 }}
                >
                  Close
                </button>
                <button
                  type="submit"
                  className="btn btn--primary"
                  disabled={recovering || !recoveryContact.trim()}
                  style={{ flex: 1 }}
                >
                  {recovering ? <span className="spinner" /> : 'Recover ID'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
