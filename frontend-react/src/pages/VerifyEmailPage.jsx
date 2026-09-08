import React, { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CircularProgress,
  Container,
  TextField,
  Typography,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlined';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi, toMessage } from '../api/client';

/**
 * 메일의 인증 링크가 도착하는 화면. 마운트되자마자 토큰을 서버로 보낸다.
 */
export default function VerifyEmailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [state, setState] = useState({ status: 'loading', message: '' });
  const [resendEmail, setResendEmail] = useState('');
  const [resendMessage, setResendMessage] = useState('');
  // StrictMode 는 개발 중 effect 를 두 번 실행한다. 토큰은 일회용이라 두 번째가 '이미 사용됨'으로 실패한다.
  const requested = useRef(false);

  useEffect(() => {
    if (!token) {
      setState({ status: 'error', message: '인증 토큰이 없는 주소입니다.' });
      return;
    }
    if (requested.current) return;
    requested.current = true;

    authApi
      .verifyEmail(token)
      .then((member) =>
        setState({ status: 'success', message: `${member.name}님, 이메일 인증이 완료되었습니다.` })
      )
      .catch((e) => setState({ status: 'error', message: toMessage(e) }));
  }, [token]);

  const handleResend = async () => {
    setResendMessage('');
    try {
      const res = await authApi.resendVerification(resendEmail);
      setResendMessage(res.message ?? '인증 메일을 다시 보냈습니다.');
    } catch (e) {
      setResendMessage(toMessage(e));
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        backgroundColor: '#f5f5f5',
        paddingY: 4,
      }}
    >
      <Container maxWidth="sm">
        <Card sx={{ padding: 4, textAlign: 'center' }}>
          <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 3 }}>
            이메일 인증
          </Typography>

          {state.status === 'loading' && (
            <>
              <CircularProgress />
              <Typography variant="body2" color="textSecondary" sx={{ marginTop: 2 }}>
                인증을 확인하는 중입니다…
              </Typography>
            </>
          )}

          {state.status === 'success' && (
            <>
              <CheckCircleIcon sx={{ fontSize: 56, color: '#2f9e44' }} />
              <Alert severity="success" sx={{ marginTop: 2, textAlign: 'left' }}>
                {state.message}
              </Alert>
              <Button variant="contained" fullWidth sx={{ marginTop: 3 }} onClick={() => navigate('/login')}>
                로그인하러 가기
              </Button>
            </>
          )}

          {state.status === 'error' && (
            <>
              <ErrorOutlineIcon sx={{ fontSize: 56, color: '#e03131' }} />
              <Alert severity="error" sx={{ marginTop: 2, textAlign: 'left' }}>
                {state.message}
              </Alert>

              <Box sx={{ marginTop: 3, textAlign: 'left' }}>
                <Typography variant="body2" sx={{ marginBottom: 1 }}>
                  링크가 만료되었다면 인증 메일을 다시 받으세요.
                </Typography>
                <TextField
                  fullWidth
                  size="small"
                  type="email"
                  label="가입한 이메일"
                  value={resendEmail}
                  onChange={(e) => setResendEmail(e.target.value)}
                />
                <Button
                  variant="outlined"
                  fullWidth
                  sx={{ marginTop: 1 }}
                  disabled={!resendEmail}
                  onClick={handleResend}
                >
                  인증 메일 재발송
                </Button>
                {resendMessage && (
                  <Alert severity="info" sx={{ marginTop: 2 }}>
                    {resendMessage}
                  </Alert>
                )}
              </Box>

              <Button fullWidth sx={{ marginTop: 2 }} onClick={() => navigate('/login')}>
                로그인 화면으로
              </Button>
            </>
          )}
        </Card>
      </Container>
    </Box>
  );
}
