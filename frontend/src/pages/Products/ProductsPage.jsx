// src/pages/Products/ProductsPage.jsx
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { productsApi } from '../../api/products.api';
import { useToast } from '../../context/ToastContext';
import { getServiceErrorMessage } from '../../utils/errorHelper';
import axios from 'axios';

const CATEGORIES = ['Electronics', 'Clothing', 'Books', 'Home', 'Sports', 'Food', 'Accessories', 'Other', 'Testing'];

function ProductModal({ product, onClose, onSave }) {
  const [imageFile, setImageFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const toast = useToast();

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    defaultValues: product || { stockQty: 1, price: 0 },
  });
  const isEdit = !!product?.id;

  const handleFormSubmit = async (data) => {
    let finalImageUrl = data.imageUrl;
    
    if (imageFile) {
      setUploading(true);
      try {
        const ext = '.' + imageFile.name.split('.').pop();
        const res = await productsApi.getUploadUrl(ext);
        const { uploadUrl, finalUrl } = res.data.data;

        await axios.put(uploadUrl, imageFile, {
          headers: { 'Content-Type': imageFile.type },
        });
        
        finalImageUrl = finalUrl;
      } catch {
        toast.error('Failed to upload image. Please try again.');
        setUploading(false);
        return;
      }
      setUploading(false);
    }

    onSave({ ...data, imageUrl: finalImageUrl });
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal animate-up">
        <div className="modal-header">
          <h2>{isEdit ? '✏️ Edit Product' : '📦 New Product'}</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit(handleFormSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
            <div className="input-group" style={{ gridColumn: '1/-1' }}>
              <label>Product Name *</label>
              <input className={`input ${errors.name ? 'input--error' : ''}`} placeholder="MacBook Pro 16"
                {...register('name', { required: 'Name is required' })}/>
              {errors.name && <span className="field-error">{errors.name.message}</span>}
            </div>
            <div className="input-group">
              <label>Price ($) *</label>
              <input className={`input ${errors.price ? 'input--error' : ''}`} type="number" step="0.01" placeholder="0.00"
                {...register('price', { required: 'Price is required', min: { value: 0, message: 'Must be ≥ 0' } })}/>
              {errors.price && <span className="field-error">{errors.price.message}</span>}
            </div>
            <div className="input-group">
              <label>Stock Qty *</label>
              <input className={`input ${errors.stockQty ? 'input--error' : ''}`} type="number" placeholder="0"
                {...register('stockQty', { required: 'Stock is required', min: { value: 0, message: 'Must be ≥ 0' } })}/>
              {errors.stockQty && <span className="field-error">{errors.stockQty.message}</span>}
            </div>
            <div className="input-group">
              <label>SKU *</label>
              <input className={`input ${errors.sku ? 'input--error' : ''}`} placeholder="MBP-16-M3"
                {...register('sku', { required: 'SKU is required' })}/>
              {errors.sku && <span className="field-error">{errors.sku.message}</span>}
            </div>
            <div className="input-group">
              <label>Category *</label>
              <select className="input" {...register('category', { required: true })}>
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div className="input-group" style={{ gridColumn: '1/-1' }}>
              <label>Description</label>
              <input className="input" placeholder="Product description..."
                {...register('description')}/>
            </div>
            <div className="input-group" style={{ gridColumn: '1/-1' }}>
              <label>Product Image</label>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                {product?.imageUrl && !imageFile && (
                  <img src={product.imageUrl} alt="preview" style={{ width: 40, height: 40, borderRadius: 6, objectFit: 'cover' }} />
                )}
                <input className="input" type="file" accept="image/*"
                  onChange={e => setImageFile(e.target.files[0])}
                  style={{ flex: 1 }}/>
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn--ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn--primary" disabled={isSubmitting || uploading}>
              {(isSubmitting || uploading) ? <span className="spinner"/> : isEdit ? 'Save Changes' : 'Create Product'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function StockModal({ product, onClose }) {
  const [qty, setQty] = useState(1);
  const [mode, setMode] = useState('add');
  const toast = useToast();
  const qc = useQueryClient();
  const [loading, setLoading] = useState(false);

  const handleStock = async () => {
    if (!qty || qty < 1) return;
    setLoading(true);
    try {
      if (mode === 'add') await productsApi.addStock(product.id, qty);
      else await productsApi.deductStock(product.id, qty);
      toast.success(`Stock ${mode === 'add' ? 'added' : 'deducted'} successfully`);
      qc.invalidateQueries(['products']);
      onClose();
    } catch (err) {
      toast.error(getServiceErrorMessage(err, 'Stock operation failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal animate-up" style={{ maxWidth: 400 }}>
        <div className="modal-header">
          <h2>📊 Manage Stock</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <p style={{ color: 'var(--text-secondary)', marginBottom: 20 }}>
          Product: <strong>{product.name}</strong><br/>
          Current Stock: <strong style={{ color: 'var(--primary)' }}>{product.stockQty}</strong>
        </p>
        <div style={{ display: 'flex', background: 'var(--bg-elevated)', borderRadius: 10, padding: 4, marginBottom: 20 }}>
          {['add', 'deduct'].map(m => (
            <button key={m} onClick={() => setMode(m)} style={{
              flex: 1, padding: 8, border: 'none', borderRadius: 8, fontWeight: 600, fontSize: '0.875rem',
              cursor: 'pointer', transition: 'all 0.2s',
              background: mode === m ? (m === 'add' ? 'var(--success)' : 'var(--danger)') : 'transparent',
              color: mode === m ? '#fff' : 'var(--text-secondary)',
            }}>
              {m === 'add' ? '+ Add Stock' : '- Deduct Stock'}
            </button>
          ))}
        </div>
        <div className="input-group" style={{ marginBottom: 20 }}>
          <label>Quantity</label>
          <input className="input" type="number" min="1" value={qty} onChange={e => setQty(Number(e.target.value))}/>
        </div>
        <div className="modal-footer">
          <button className="btn btn--ghost" onClick={onClose}>Cancel</button>
          <button className={`btn btn--${mode === 'add' ? 'success' : 'danger'}`} onClick={handleStock} disabled={loading}>
            {loading ? <span className="spinner"/> : mode === 'add' ? 'Add Stock' : 'Deduct Stock'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function ProductsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [modal, setModal] = useState(null); // null | {type:'create'|'edit'|'stock', data?}
  const [deleteId, setDeleteId] = useState(null);
  const toast = useToast();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['products', page, categoryFilter],
    queryFn: () => categoryFilter
      ? productsApi.getByCategory(categoryFilter).then(r => ({ data: { data: { content: r.data.data, totalPages: 1 } } }))
      : productsApi.getAll({ page, size: 10, sort: 'createdAt,desc' }),
    keepPreviousData: true,
  });

  const products = data?.data?.data?.content || data?.data?.data || [];
  const totalPages = data?.data?.data?.totalPages || 1;

  const createMutation = useMutation({
    mutationFn: productsApi.create,
    onSuccess: (_, variables) => { 
      toast.success('Product created!'); 
      setCategoryFilter(variables.category);
      setPage(0);
      qc.invalidateQueries(['products']); 
      setModal(null); 
    },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Create failed')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => productsApi.update(id, data),
    onSuccess: (_, variables) => { 
      toast.success('Product updated!'); 
      setCategoryFilter(variables.data.category);
      setPage(0);
      qc.invalidateQueries(['products']); 
      setModal(null); 
    },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Update failed')),
  });

  const deleteMutation = useMutation({
    mutationFn: productsApi.delete,
    onSuccess: () => { toast.success('Product deleted'); qc.invalidateQueries(['products']); setDeleteId(null); },
    onError: (err) => toast.error(getServiceErrorMessage(err, 'Delete failed')),
  });

  const handleSave = async (formData) => {
    const payload = { ...formData, price: Number(formData.price), stockQty: Number(formData.stockQty) };
    if (modal?.data?.id) await updateMutation.mutateAsync({ id: modal.data.id, data: payload });
    else await createMutation.mutateAsync(payload);
  };

  const filtered = products.filter(p =>
    !search || p.name?.toLowerCase().includes(search.toLowerCase()) || p.sku?.toLowerCase().includes(search.toLowerCase())
  );

  const getAvailabilityBadge = (qty) => {
    if (qty <= 0) return <span className="badge badge--danger">Out of Stock</span>;
    if (qty < 10) return <span className="badge badge--warning">Low Stock</span>;
    return <span className="badge badge--success">In Stock</span>;
  };

  return (
    <div className="animate-fade">
      <div className="page-header">
        <div>
          <h1>Products</h1>
          <p>Manage your product catalog — create, update, and control stock.</p>
        </div>
        <button className="btn btn--primary" onClick={() => setModal({ type: 'create' })}>
          + New Product
        </button>
      </div>

      {/* Toolbar */}
      <div className="toolbar">
        <div className="search-box" style={{ flex: 1, minWidth: 200 }}>
          <span className="search-box__icon">🔍</span>
          <input className="input" placeholder="Search by name or SKU..."
            value={search} onChange={e => {
              const val = e.target.value;
              setSearch(val);
              if (val) {
                const matches = products.filter(p => 
                  p.name?.toLowerCase().includes(val.toLowerCase()) || p.sku?.toLowerCase().includes(val.toLowerCase())
                );
                if (matches.length > 0) {
                  const firstCat = matches[0].category;
                  if (matches.every(p => p.category === firstCat) && categoryFilter !== firstCat) {
                    setCategoryFilter(firstCat);
                    setPage(0);
                  }
                }
              } else {
                if (categoryFilter !== '') {
                  setCategoryFilter('');
                  setPage(0);
                }
              }
            }}/>
        </div>
        <select className="input" style={{ width: 180 }} value={categoryFilter} onChange={e => { setCategoryFilter(e.target.value); setPage(0); }}>
          <option value="">All Categories</option>
          {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
      </div>

      {/* Table */}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Product</th>
              <th>SKU</th>
              <th>Category</th>
              <th>Price</th>
              <th>Stock</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading
              ? Array(5).fill(0).map((_, i) => (
                  <tr key={i}><td colSpan={7}><div className="skeleton skeleton-row"/></td></tr>
                ))
              : filtered.length === 0
              ? <tr><td colSpan={7}>
                  <div className="empty-state">
                    <div className="empty-state__icon">📦</div>
                    <div className="empty-state__title">No products found</div>
                    <div className="empty-state__desc">Create your first product to get started.</div>
                    <button className="btn btn--primary btn--sm" onClick={() => setModal({ type: 'create' })}>+ Create Product</button>
                  </div>
                </td></tr>
              : filtered.map(p => (
                <tr key={p.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{p.name}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 2, maxWidth: 200 }} className="truncate">
                      {p.description || '—'}
                    </div>
                  </td>
                  <td><code style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>{p.sku}</code></td>
                  <td><span className="badge badge--primary">{p.category}</span></td>
                  <td style={{ fontWeight: 600 }}>${Number(p.price).toFixed(2)}</td>
                  <td style={{ fontWeight: 700, color: p.stockQty < 10 ? 'var(--warning)' : 'var(--text-primary)' }}>
                    {p.stockQty}
                  </td>
                  <td>{getAvailabilityBadge(p.stockQty)}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button className="btn btn--ghost btn--sm" onClick={() => setModal({ type: 'stock', data: p })} title="Manage stock">📊</button>
                      <button className="btn btn--secondary btn--sm" onClick={() => setModal({ type: 'edit', data: p })} title="Edit">✏️</button>
                      <button className="btn btn--danger btn--sm" onClick={() => setDeleteId(p.id)} title="Delete">🗑</button>
                    </div>
                  </td>
                </tr>
              ))
            }
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>‹ Prev</button>
          {Array.from({ length: totalPages }, (_, i) => (
            <button key={i} onClick={() => setPage(i)} className={page === i ? 'active' : ''}>{i + 1}</button>
          ))}
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page === totalPages - 1}>Next ›</button>
        </div>
      )}

      {/* Modals */}
      {(modal?.type === 'create' || modal?.type === 'edit') && (
        <ProductModal product={modal.data} onClose={() => setModal(null)} onSave={handleSave}/>
      )}
      {modal?.type === 'stock' && (
        <StockModal product={modal.data} onClose={() => setModal(null)}/>
      )}

      {/* Delete Confirm */}
      {deleteId && (
        <div className="modal-overlay" onClick={() => setDeleteId(null)}>
          <div className="modal animate-up" style={{ maxWidth: 380 }}>
            <div style={{ textAlign: 'center', padding: '8px 0 16px' }}>
              <div style={{ fontSize: '2.5rem', marginBottom: 12 }}>⚠️</div>
              <h2 style={{ marginBottom: 8 }}>Delete Product?</h2>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>This action cannot be undone.</p>
            </div>
            <div className="modal-footer">
              <button className="btn btn--ghost" onClick={() => setDeleteId(null)}>Cancel</button>
              <button className="btn btn--danger" onClick={() => deleteMutation.mutate(deleteId)} disabled={deleteMutation.isPending}>
                {deleteMutation.isPending ? <span className="spinner"/> : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
