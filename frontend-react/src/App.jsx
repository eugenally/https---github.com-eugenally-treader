import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

import { AuthProvider, useAuth } from './auth/AuthContext';

import LoginPage from './pages/LoginPage';
import SignUpPage from './pages/SignUpPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import FindPasswordPage from './pages/FindPasswordPage';
import ProfilePage from './pages/ProfilePage';
import DashboardPage from './pages/DashboardPage';
import QuotationPage from './pages/QuotationPage';
import SalesOrderPage from './pages/SalesOrderPage';
import ShipmentPage from './pages/ShipmentPage';
import InvoicePage from './pages/InvoicePage';
import MasterPage from './pages/MasterPage';
import BoardListPage from './pages/BoardListPage';
import PostDetailPage from './pages/PostDetailPage';
import PostFormPage from './pages/PostFormPage';

const theme = createTheme({
  palette: {
    primary: { main: '#1976d2' },
    secondary: { main: '#f50057' },
    background: { default: '#fafafa' },
  },
  typography: {
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Malgun Gothic"',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
  },
});

/** 로그인하지 않았으면 로그인 화면으로 돌린다 */
function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

/** 이미 로그인했으면 로그인·가입 화면 대신 대시보드로 보낸다 */
function RedirectIfAuthenticated({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : children;
}

function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <RedirectIfAuthenticated>
            <LoginPage />
          </RedirectIfAuthenticated>
        }
      />
      <Route
        path="/signup"
        element={
          <RedirectIfAuthenticated>
            <SignUpPage />
          </RedirectIfAuthenticated>
        }
      />
      {/* 인증 링크는 로그인 여부와 무관하게 열려 있어야 한다 */}
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/find-password" element={<FindPasswordPage />} />

      {/*
        게시판은 로그인 없이도 들어올 수 있다. 자유게시판은 비회원 읽기·쓰기가 허용된 게시판이라
        여기서 막으면 그 설정이 무의미해진다. 실제 접근 제어는 게시판별로 서버가 판단한다.
      */}
      <Route path="/board" element={<BoardListPage />} />
      <Route path="/board/post/:postId" element={<PostDetailPage />} />
      <Route path="/board/write" element={<PostFormPage />} />

      <Route path="/dashboard" element={<RequireAuth><DashboardPage /></RequireAuth>} />
      <Route path="/master" element={<RequireAuth><MasterPage /></RequireAuth>} />
      <Route path="/quotation" element={<RequireAuth><QuotationPage /></RequireAuth>} />
      <Route path="/sales-order" element={<RequireAuth><SalesOrderPage /></RequireAuth>} />
      <Route path="/shipment" element={<RequireAuth><ShipmentPage /></RequireAuth>} />
      <Route path="/invoice" element={<RequireAuth><InvoicePage /></RequireAuth>} />
      <Route path="/profile" element={<RequireAuth><ProfilePage /></RequireAuth>} />

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <Router>
          <AppRoutes />
        </Router>
      </AuthProvider>
    </ThemeProvider>
  );
}
