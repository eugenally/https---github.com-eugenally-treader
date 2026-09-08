import React, { useState } from 'react';
import {
  Box,
  Card,
  TextField,
  Button,
  Typography,
  Checkbox,
  FormControlLabel,
  Alert,
  Container,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';

export default function LoginPage() {
  const navigate = useNavigate();
  const [loginId, setLoginId] = useState('sales_user');
  const [password, setPassword] = useState('1234');
  const [rememberMe, setRememberMe] = useState(true);
  const [error, setError] = useState('');

  const handleLogin = (e) => {
    e.preventDefault();

    if (!loginId || !password) {
      setError('아이디와 비밀번호를 입력해주세요.');
      return;
    }

    if (loginId === 'sales_user' && password === '1234') {
      sessionStorage.setItem('isLoggedIn', 'true');
      sessionStorage.setItem('userName', '김영업');
      navigate('/dashboard');
    } else {
      setError('아이디 또는 비밀번호가 잘못되었습니다.');
      setPassword('');
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f5f5f5',
      }}
    >
      <Container maxWidth="xs">
        <Card sx={{ padding: 4, boxShadow: 3 }}>
          <Box sx={{ textAlign: 'center', marginBottom: 3 }}>
            <Typography variant="h4" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
              🚢 TreaderAPP
            </Typography>
            <Typography variant="body2" color="textSecondary">
              수출 오더 관리 시스템
            </Typography>
          </Box>

          {error && (
            <Alert severity="error" sx={{ marginBottom: 2 }}>
              {error}
            </Alert>
          )}

          <form onSubmit={handleLogin}>
            <TextField
              fullWidth
              label="아이디"
              variant="outlined"
              margin="normal"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              autoFocus
            />
            <TextField
              fullWidth
              label="비밀번호"
              type="password"
              variant="outlined"
              margin="normal"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
              }
              label="아이디 저장"
              sx={{ marginTop: 1, marginBottom: 2 }}
            />
            <Button
              fullWidth
              variant="contained"
              color="primary"
              size="large"
              type="submit"
              sx={{ marginBottom: 2 }}
            >
              로그인
            </Button>
          </form>

          <Box sx={{ borderTop: '1px solid #ddd', paddingTop: 2 }}>
            <Typography
              variant="caption"
              color="textSecondary"
              sx={{ display: 'block', marginBottom: 1 }}
            >
              데모 계정
            </Typography>
            <Typography variant="caption" sx={{ display: 'block' }}>
              아이디: <strong>sales_user</strong>
            </Typography>
            <Typography variant="caption" sx={{ display: 'block' }}>
              비밀번호: <strong>1234</strong>
            </Typography>
          </Box>
        </Card>
      </Container>
    </Box>
  );
}
