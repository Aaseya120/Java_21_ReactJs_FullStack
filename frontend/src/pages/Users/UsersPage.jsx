// src/pages/Users/UsersPage.jsx
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../../api/users.api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { decodeToken } from '../../utils/token';
import { getAccessToken } from '../../utils/token';

export default function UsersPage() {
  const { user, logout } = useAuth();
  const toast = useToast();
  const qc = useQueryClient();
  const [lookupId, setLookupId] = useState('');
  const [editName, setEditName] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  const { data: profileData, isLoading: profileLoading } = useQuery({
    queryKey: ['user', user?.id],
    queryFn: () => usersApi.getById(user.id),
    enabled: !!user?.id,
  });

  const { data: lookupData, isLoading: lookupLoading, error: lookupError, isError: isLookupError, refetch: doLookup } = useQuery({
    queryKey: ['user-lookup', lookupId],
    queryFn: () => usersApi.getById(lookupId),
    enabled: false,
    retry: false,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, name }) => usersApi.updateName(id, name),
    onSuccess: () => {
      toast.success('Name updated successfully!');
      qc.invalidateQueries(['user', user?.id]);
      setIsEditing(false);
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Update failed'),
  });

  const profile = (profileData?.data?.data || profileData?.data) || profileData;
  const token = getAccessToken();
  const decoded = token ? decodeToken(token) : null;

  const initials = (name) => name
    ? name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase()
    : 'U';

  const ProfileField = ({ label, value, mono }) => (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      padding: '12px 0', borderBottom: '1px solid var(--border)',
    }}>
      <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
      <span style={{
        fontSize: '0.875rem', fontWeight: 500, maxWidth: '60%', textAlign: 'right',
        wordBreak: 'break-all',
        fontFamily: mono ? "'Courier New', monospace" : 'inherit',
        color: mono ? 'var(--primary)' : 'var(--text-primary)',
      }}>{value || '—'}</span>
    </div>
  );

  return (
    <div className="animate-fade">
      <div className="page-header">
        <div>
          <h1>Users</h1>
          <p>View and manage user profiles. Update your display name and look up other users.</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: 20 }}>

        {/* My Profile Card */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 20 }}>
            <div style={{
              width: 64, height: 64, borderRadius: '50%',
              background: 'linear-gradient(135deg, var(--primary), var(--primary-hover))',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '1.5rem', fontWeight: 700, color: '#fff', flexShrink: 0,
            }}>
              {initials(profile?.fullName || user?.fullName)}
            </div>
            <div>
              <h2 style={{ fontSize: '1.125rem', fontWeight: 700 }}>My Profile</h2>
              <span className="badge badge--primary" style={{ marginTop: 4 }}>
                {profile?.role || 'USER'}
              </span>
            </div>
          </div>

          {profileLoading ? (
            <>
              {Array(4).fill(0).map((_, i) => <div key={i} className="skeleton" style={{ height: 44, marginBottom: 8 }}/>)}
            </>
          ) : (
            <>
              <ProfileField label="Full Name" value={profile?.fullName}/>
              <ProfileField label="Email" value={profile?.email}/>
              <ProfileField label="User ID" value={profile?.id} mono/>
              <ProfileField label="Created" value={profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '—'}/>
            </>
          )}

          {/* Edit Name */}
          <div style={{ marginTop: 20 }}>
            {isEditing ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div className="input-group">
                  <label>New Full Name</label>
                  <input className="input" value={editName} onChange={e => setEditName(e.target.value)}
                    placeholder="Enter new name..."/>
                </div>
                <div style={{ display: 'flex', gap: 10 }}>
                  <button className="btn btn--ghost btn--sm flex-1" onClick={() => setIsEditing(false)}>Cancel</button>
                  <button className="btn btn--primary btn--sm flex-1"
                    disabled={!editName.trim() || updateMutation.isPending}
                    onClick={() => updateMutation.mutate({ id: user?.id, name: editName })}>
                    {updateMutation.isPending ? <span className="spinner"/> : 'Save Name'}
                  </button>
                </div>
              </div>
            ) : (
              <button className="btn btn--secondary w-full"
                onClick={() => { setEditName(profile?.fullName || ''); setIsEditing(true); }}>
                ✏️ Edit Name
              </button>
            )}
          </div>

          <hr className="divider"/>
          <button className="btn btn--danger btn--sm" onClick={logout}>⏻ Sign Out</button>
        </div>

        {/* JWT Token Info */}
        <div className="card">
          <h2 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: 20 }}>🔑 Token Info</h2>
          {decoded ? (
            <>
              <ProfileField label="Subject (email)" value={decoded.sub}/>
              <ProfileField label="Role" value={decoded.role}/>
              <ProfileField label="Issued At" value={decoded.iat ? new Date(decoded.iat * 1000).toLocaleString() : '—'}/>
              <ProfileField label="Expires" value={decoded.exp ? new Date(decoded.exp * 1000).toLocaleString() : '—'}/>
              <ProfileField label="Status" value={decoded.exp * 1000 > Date.now() ? '✅ Valid' : '❌ Expired'}/>
              <div style={{ marginTop: 16 }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 8 }}>RAW CLAIMS</div>
                <pre className="code-block" style={{ maxHeight: 140 }}>
                  {JSON.stringify(decoded, null, 2)}
                </pre>
              </div>
            </>
          ) : (
            <div className="empty-state" style={{ padding: '24px 0' }}>
              <div className="empty-state__icon">🔑</div>
              <div className="empty-state__title">No token available</div>
            </div>
          )}
        </div>

        {/* User Lookup */}
        <div className="card">
          <h2 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: 20 }}>🔍 Look Up User</h2>
          <div style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
            <input className="input" placeholder="Enter User UUID..."
              value={lookupId} onChange={e => setLookupId(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && lookupId.trim() && doLookup()}
              style={{ flex: 1 }}/>
            <button className="btn btn--primary" disabled={!lookupId.trim() || lookupLoading}
              onClick={() => doLookup()}>
              {lookupLoading ? <span className="spinner"/> : 'Look Up'}
            </button>
          </div>
          {user?.id && (
            <button className="btn btn--ghost btn--sm"
              onClick={() => { setLookupId(user.id); setTimeout(() => doLookup(), 100); }}
              style={{ marginBottom: 16 }}>
              Use My ID
            </button>
          )}
          {isLookupError && (
            <div style={{
              padding: '12px 16px', borderRadius: 8, marginBottom: 16,
              background: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)',
              color: 'var(--danger)', fontSize: '0.875rem'
            }}>
              ⚠️ {lookupError?.response?.data?.errorDesc || lookupError?.response?.data?.message || lookupError?.message || 'User not found or lookup failed.'}
            </div>
          )}
          {!isLookupError && ((lookupData?.data?.data || lookupData?.data) || lookupData) ? (() => {
            const u = (lookupData?.data?.data || lookupData?.data) || lookupData;
            return (
              <div style={{ animation: 'fadeIn 0.3s ease' }}>
                <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 16 }}>
                  <div style={{
                    width: 48, height: 48, borderRadius: '50%',
                    background: 'linear-gradient(135deg, var(--info), #60a5fa)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '1.1rem', fontWeight: 700, color: '#fff',
                  }}>{initials(u.fullName)}</div>
                  <div>
                    <div style={{ fontWeight: 700 }}>{u.fullName}</div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>{u.email}</div>
                  </div>
                </div>
                <ProfileField label="User ID" value={u.id} mono/>
                <ProfileField label="Role" value={u.role}/>
              </div>
            );
          })() : null}
        </div>

      </div>
    </div>
  );
}
