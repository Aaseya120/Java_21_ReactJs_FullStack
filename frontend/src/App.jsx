// src/App.jsx
import React, { Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider, useToast } from './context/ToastContext';
import Layout from './components/layout/Layout';
import ErrorBoundary from './components/common/ErrorBoundary';
import { PageLoader } from './components/common/Skeleton';
import './styles/globals.css';

const LoginPage = React.lazy(() => import('./pages/Login/LoginPage'));
const DashboardPage = React.lazy(() => import('./pages/Dashboard/DashboardPage'));
const HealthPage = React.lazy(() => import('./pages/Health/HealthPage'));
const ProductsPage = React.lazy(() => import('./pages/Products/ProductsPage'));
const OrdersPage = React.lazy(() => import('./pages/Orders/OrdersPage'));
const UsersPage = React.lazy(() => import('./pages/Users/UsersPage'));
const PaymentsPage = React.lazy(() => import('./pages/Payments/PaymentsPage'));
const AggregatorPage = React.lazy(() => import('./pages/Aggregator/AggregatorPage'));
const NotificationsPage = React.lazy(() => import('./pages/Notifications/NotificationsPage'));
const SettingsPage = React.lazy(() => import('./pages/Settings/SettingsPage'));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
});

function NetworkListener() {
  const toast = useToast();
  useEffect(() => {
    const handleOffline = () => toast.error('You are offline. Check your network connection.');
    const handleOnline = () => toast.success('Network connection restored.');
    window.addEventListener('offline', handleOffline);
    window.addEventListener('online', handleOnline);
    return () => {
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('online', handleOnline);
    };
  }, [toast]);
  return null;
}

function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace/>;
}

function AppRoutes() {
  const { isAuthenticated } = useAuth();

  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login" element={
          isAuthenticated ? <Navigate to="/" replace/> : <LoginPage/>
        }/>
        <Route path="/" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><DashboardPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="/health" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><HealthPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="/products" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><ProductsPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="/orders" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><OrdersPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="/users" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><UsersPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="/payments" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><PaymentsPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="/aggregator" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><AggregatorPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="/notifications" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><NotificationsPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="/settings" element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout><SettingsPage/></Layout>
            </ErrorBoundary>
          </ProtectedRoute>
        }/>
        <Route path="*" element={<Navigate to="/" replace/>}/>
      </Routes>
    </Suspense>
  );
}

const initTheme = () => {
  const theme = localStorage.getItem('theme') || 'dark';
  document.documentElement.setAttribute('data-theme', theme);
};
initTheme();

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <NetworkListener />
            <AppRoutes/>
          </ToastProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
