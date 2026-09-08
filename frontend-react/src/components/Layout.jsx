import React from 'react';
import {
  Box,
  AppBar,
  Toolbar,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemIcon,
  Typography,
  Button,
  Container,
} from '@mui/material';
import { useNavigate, useLocation } from 'react-router-dom';
import DashboardIcon from '@mui/icons-material/Dashboard';
import DescriptionIcon from '@mui/icons-material/Description';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import LogoutIcon from '@mui/icons-material/Logout';
import LoginIcon from '@mui/icons-material/Login';
import PersonIcon from '@mui/icons-material/Person';
import ForumIcon from '@mui/icons-material/Forum';
import FolderIcon from '@mui/icons-material/Folder';
import { useAuth } from '../auth/AuthContext';

const DRAWER_WIDTH = 250;

/** {@code memberOnly} 인 메뉴는 비로그인 상태에서 감춘다 */
const menuItems = [
  { label: '대시보드', path: '/dashboard', icon: DashboardIcon, memberOnly: true },
  { label: '마스터', path: '/master', icon: FolderIcon, memberOnly: true },
  { label: '견적', path: '/quotation', icon: DescriptionIcon, memberOnly: true },
  { label: '수주', path: '/sales-order', icon: ShoppingCartIcon, memberOnly: true },
  { label: '출하', path: '/shipment', icon: LocalShippingIcon, memberOnly: true },
  { label: '인보이스', path: '/invoice', icon: ReceiptLongIcon, memberOnly: true },
  { label: '게시판', path: '/board', icon: ForumIcon, memberOnly: false },
];

export default function Layout({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { member, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <Box sx={{ display: 'flex' }}>
      {/* Header */}
      <AppBar
        position="fixed"
        sx={{
          zIndex: (theme) => theme.zIndex.drawer + 1,
          backgroundColor: '#1976d2',
        }}
      >
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            🚢 TreaderAPP - 수출오더관리시스템
          </Typography>
          {member ? (
            <>
              <Button
                color="inherit"
                size="small"
                startIcon={<PersonIcon />}
                onClick={() => navigate('/profile')}
                sx={{ marginRight: 1 }}
              >
                {member.name} ({member.role})
              </Button>
              <Button color="inherit" size="small" onClick={handleLogout} startIcon={<LogoutIcon />}>
                로그아웃
              </Button>
            </>
          ) : (
            // 비회원도 자유게시판을 볼 수 있으므로 로그인 버튼을 띄운다
            <Button color="inherit" size="small" startIcon={<LoginIcon />} onClick={() => navigate('/login')}>
              로그인
            </Button>
          )}
        </Toolbar>
      </AppBar>

      {/* Sidebar */}
      <Drawer
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            marginTop: '64px',
          },
        }}
        variant="permanent"
        anchor="left"
      >
        <List>
          {menuItems
            .filter((item) => member || !item.memberOnly)
            .map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname.startsWith(item.path);
            return (
              <ListItem key={item.path} disablePadding>
                <ListItemButton
                  onClick={() => navigate(item.path)}
                  sx={{
                    backgroundColor: isActive ? '#e3f2fd' : 'transparent',
                    borderLeft: isActive ? '4px solid #1976d2' : '4px solid transparent',
                    '&:hover': { backgroundColor: '#f5f5f5' },
                  }}
                >
                  <ListItemIcon sx={{ color: isActive ? '#1976d2' : '#666' }}>
                    <Icon />
                  </ListItemIcon>
                  <ListItemText
                    primary={item.label}
                    sx={{ color: isActive ? '#1976d2' : '#333' }}
                  />
                </ListItemButton>
              </ListItem>
            );
          })}
        </List>
      </Drawer>

      {/* Main Content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          padding: 3,
          marginTop: '64px',
          backgroundColor: '#fafafa',
          minHeight: 'calc(100vh - 64px)',
        }}
      >
        <Container maxWidth="lg">{children}</Container>
      </Box>
    </Box>
  );
}
