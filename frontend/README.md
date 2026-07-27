# Microservices Full-Stack Frontend

This is the React frontend application for the Java 21 Spring Boot Microservices project. It is built to be fast, scalable, and resilient, consuming the backend APIs securely via the Spring Cloud Gateway.

---

## 🛠️ Technologies Used

| Category | Technology | Purpose |
|----------|------------|---------|
| **Core** | **React 18** + **Vite 5** | Lightning-fast HMR, optimized production builds, concurrent rendering. |
| **Routing** | **React Router v6** | Client-side routing with protected routes and dynamic parameter extraction. |
| **State & Caching** | **TanStack Query v5** | Server-state management, automatic caching, background fetching, and deduplication of API requests. |
| **Forms** | **React Hook Form** | Performant, flexible, and extensible form validation without excessive re-renders. |
| **HTTP Client** | **Axios** | API communication with robust interceptors for JWT token injection and error handling. |

---

## 🏗️ Architectural Patterns & Solutions

To ensure a seamless user experience and robust security, we implemented several key frontend patterns:

| Pattern / Concept | How it was achieved in this frontend |
|-------------------|--------------------------------------|
| **Stateless Auth** | `AuthContext` manages the JWT token locally (in `localStorage` or memory). All routes check this context to determine access. |
| **Secure API Calls** | An **Axios Interceptor** automatically intercepts outgoing requests and injects the `Authorization: Bearer <token>` header. |
| **Global Error Handling** | Interceptors catch `401 Unauthorized` responses to instantly log the user out, and catch `500`/`503`/`504` errors to redirect to graceful failure/fallback UI pages. |
| **Smart Caching** | **TanStack Query** caches API responses (like the Product Catalog). If a user navigates away and back, the data loads instantly from cache while re-fetching silently in the background. |
| **Protected Routes** | Custom `ProtectedRoute` wrapper components evaluate the user's authentication state and roles before rendering the requested page, redirecting to `/login` if unauthorized. |

---

## 📦 Project Structure

```
frontend/
├── package.json
├── vite.config.js
├── src/
│   ├── api/            ← Axios instances, interceptors, and API call definitions
│   ├── assets/         ← Static images, SVGs, global CSS
│   ├── components/     ← Reusable UI components (Buttons, Modals, Cards)
│   ├── context/        ← React Context providers (e.g., AuthContext)
│   ├── pages/          ← Top-level route components (Login, Dashboard, Products)
│   ├── utils/          ← Helper functions and formatters
│   ├── App.jsx         ← Root component, Route definitions, QueryClientProvider
│   └── main.jsx        ← DOM mounting and React StrictMode
```

---

## 🚀 Getting Started

### Prerequisites
- **Node.js** (v18 or higher recommended)
- **npm** (v9+)

### Installation & Running Locally

1. **Navigate to the frontend directory:**
   ```bash
   cd frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start the Vite development server:**
   ```bash
   npm run dev
   ```

The application will be running locally at `http://localhost:3000`.

### ⚠️ Backend Dependency Warning
Because this frontend heavily relies on the microservices backend for authentication and data:
- Ensure the **API Gateway** is running on `localhost:8080`.
- Ensure the required microservices (`user-service`, `product-service`, etc.) are up and healthy.
- If the backend is down, the frontend will automatically detect the failure and trigger global error states or redirect you to the login screen.
