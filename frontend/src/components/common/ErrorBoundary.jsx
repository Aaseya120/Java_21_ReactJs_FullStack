// src/components/common/ErrorBoundary.jsx
import React from 'react';

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an unhandled error:', error, errorInfo);
  }

  handleReload = () => {
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'var(--bg-base)',
          color: 'var(--text-primary)',
          padding: 24,
        }}>
          <div className="card card--glass animate-fade" style={{ maxWidth: 480, width: '100%', textAlign: 'center', padding: '36px 28px' }}>
            <div style={{ fontSize: '3rem', marginBottom: 16 }}>⚠️</div>
            <h2 style={{ marginBottom: 12, fontSize: '1.5rem', fontWeight: 700 }}>Something went wrong</h2>
            <p style={{ color: 'var(--text-secondary)', marginBottom: 24, fontSize: '0.9375rem', lineHeight: 1.6 }}>
              An unexpected error occurred while rendering this view. Please try reloading the application.
            </p>
            {this.state.error && (
              <div style={{
                background: 'var(--bg-elevated)',
                padding: '12px 16px',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.8125rem',
                color: 'var(--danger)',
                fontFamily: 'monospace',
                marginBottom: 24,
                overflowX: 'auto',
                textAlign: 'left',
                border: '1px solid rgba(239, 68, 68, 0.2)',
              }}>
                {this.state.error.toString()}
              </div>
            )}
            <button
              className="btn btn--primary"
              style={{ width: '100%' }}
              onClick={this.handleReload}
            >
              ↻ Reload Application
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
