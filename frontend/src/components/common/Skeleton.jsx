// src/components/common/Skeleton.jsx
import React from 'react';

export function SkeletonPulse({ width = '100%', height = 20, borderRadius = '4px', style = {} }) {
  return (
    <div
      style={{
        width,
        height,
        borderRadius,
        background: 'linear-gradient(90deg, var(--bg-elevated) 25%, var(--border) 50%, var(--bg-elevated) 75%)',
        backgroundSize: '200% 100%',
        animation: 'skeleton-shimmer 1.5s infinite',
        ...style,
      }}
    />
  );
}

export function TableSkeleton({ rows = 5, columns = 5 }) {
  return (
    <div style={{ width: '100%', overflowX: 'auto' }}>
      <table className="table" style={{ width: '100%' }}>
        <thead>
          <tr>
            {Array.from({ length: columns }).map((_, i) => (
              <th key={i}>
                <SkeletonPulse width="80%" height={14} />
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: rows }).map((_, rIdx) => (
            <tr key={rIdx}>
              {Array.from({ length: columns }).map((_, cIdx) => (
                <td key={cIdx}>
                  <SkeletonPulse width={cIdx === 0 ? '40%' : '75%'} height={16} />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function CardSkeleton({ lines = 4 }) {
  return (
    <div className="card" style={{ padding: 24 }}>
      <SkeletonPulse width="50%" height={24} style={{ marginBottom: 16 }} />
      {Array.from({ length: lines }).map((_, i) => (
        <SkeletonPulse
          key={i}
          width={i === lines - 1 ? '70%' : '100%'}
          height={16}
          style={{ marginBottom: 12 }}
        />
      ))}
    </div>
  );
}

export function PageLoader() {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '60vh',
      flexDirection: 'column',
      gap: 16,
    }}>
      <div style={{
        width: 40,
        height: 40,
        border: '3px solid var(--border)',
        borderTopColor: 'var(--primary)',
        borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
      }} />
      <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', fontWeight: 500 }}>
        Loading view...
      </span>
    </div>
  );
}
