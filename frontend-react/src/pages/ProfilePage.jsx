import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Grid,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import LockResetIcon from '@mui/icons-material/LockReset';
import { useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { memberApi, toMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ProfilePage() {
  const [searchParams] = useSearchParams();
  const forcePassword = searchParams.get('forcePassword') === '1';
  const { setMember } = useAuth();

  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const [profile, setProfile] = useState({ name: '', phone: '', email: '', dept: '', position: '' });
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '', newPasswordConfirm: '' });

  const notify = (message, severity = 'success') => setToast({ message, severity });

  useEffect(() => {
    (async () => {
      try {
        const data = await memberApi.me();
        setMe(data);
        setProfile({
          name: data.name ?? '',
          phone: data.phone ?? '',
          email: data.email ?? '',
          dept: data.dept ?? '',
          position: data.position ?? '',
        });
      } catch (e) {
        notify(toMessage(e), 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const handleSaveProfile = async () => {
    try {
      const updated = await memberApi.updateMe(profile);
      setMe(updated);
      setMember(updated);
      notify(
        updated.emailVerified
          ? '회원정보를 저장했습니다.'
          : '회원정보를 저장했습니다. 이메일이 변경되어 인증이 필요합니다. 새 주소로 인증 메일을 보냈습니다.'
      );
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const pwMismatch =
    pw.newPasswordConfirm.length > 0 && pw.newPassword !== pw.newPasswordConfirm;

  const handleChangePassword = async () => {
    try {
      const updated = await memberApi.changePassword(pw);
      setMe(updated);
      setMember(updated);
      setPw({ currentPassword: '', newPassword: '', newPasswordConfirm: '' });
      notify('비밀번호를 변경했습니다.');
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  if (loading) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', padding: 6 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 3 }}>
          👤 내 정보
        </Typography>

        {forcePassword && (
          <Alert severity="warning" sx={{ marginBottom: 2 }}>
            임시 비밀번호로 로그인했습니다. 계속 사용하려면 비밀번호를 변경해 주세요.
          </Alert>
        )}
        {me && !me.emailVerified && (
          <Alert severity="warning" sx={{ marginBottom: 2 }}>
            이메일 인증이 완료되지 않았습니다. 메일함의 인증 링크를 확인해 주세요.
          </Alert>
        )}

        <Card sx={{ padding: 3, marginBottom: 3 }}>
          <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', marginBottom: 2, flexWrap: 'wrap', gap: 1 }}
          >
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              {me?.loginId}
            </Typography>
            <Chip label={me?.role} size="small" color="primary" />
            <Chip label={me?.status} size="small" />
            <Chip
              label={me?.emailVerified ? '이메일 인증 완료' : '이메일 미인증'}
              size="small"
              color={me?.emailVerified ? 'success' : 'warning'}
            />
          </Stack>

          <Typography variant="subtitle2" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
            회원정보 수정
          </Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                label="아이디"
                value={me?.loginId ?? ''}
                disabled
                helperText="아이디는 변경할 수 없습니다."
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                required
                label="이름"
                value={profile.name}
                onChange={(e) => setProfile((p) => ({ ...p, name: e.target.value }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                required
                type="email"
                label="이메일"
                value={profile.email}
                onChange={(e) => setProfile((p) => ({ ...p, email: e.target.value }))}
                helperText="이메일을 바꾸면 인증을 다시 받아야 합니다."
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                required
                label="전화번호"
                value={profile.phone}
                onChange={(e) => setProfile((p) => ({ ...p, phone: e.target.value }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                label="부서"
                value={profile.dept}
                onChange={(e) => setProfile((p) => ({ ...p, dept: e.target.value }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                label="직급"
                value={profile.position}
                onChange={(e) => setProfile((p) => ({ ...p, position: e.target.value }))}
              />
            </Grid>
          </Grid>

          <Stack direction="row" sx={{ justifyContent: 'flex-end', marginTop: 2 }}>
            <Button
              variant="contained"
              startIcon={<SaveIcon />}
              disabled={!profile.name || !profile.email || !profile.phone}
              onClick={handleSaveProfile}
            >
              저장
            </Button>
          </Stack>
        </Card>

        <Card sx={{ padding: 3 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
            비밀번호 변경
          </Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                type="password"
                label="현재 비밀번호"
                value={pw.currentPassword}
                onChange={(e) => setPw((p) => ({ ...p, currentPassword: e.target.value }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                type="password"
                label="새 비밀번호"
                value={pw.newPassword}
                onChange={(e) => setPw((p) => ({ ...p, newPassword: e.target.value }))}
                helperText="영문과 숫자를 포함해 8자 이상"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                type="password"
                label="새 비밀번호 확인"
                value={pw.newPasswordConfirm}
                onChange={(e) => setPw((p) => ({ ...p, newPasswordConfirm: e.target.value }))}
                error={pwMismatch}
                helperText={pwMismatch ? '비밀번호가 일치하지 않습니다.' : ' '}
              />
            </Grid>
          </Grid>

          <Stack direction="row" sx={{ justifyContent: 'flex-end', marginTop: 2 }}>
            <Button
              variant="contained"
              startIcon={<LockResetIcon />}
              disabled={
                !pw.currentPassword || !pw.newPassword || !pw.newPasswordConfirm || pwMismatch
              }
              onClick={handleChangePassword}
            >
              비밀번호 변경
            </Button>
          </Stack>
        </Card>

        <Snackbar
          open={Boolean(toast)}
          autoHideDuration={6000}
          onClose={() => setToast(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity={toast?.severity} onClose={() => setToast(null)}>
            {toast?.message}
          </Alert>
        </Snackbar>
      </Box>
    </Layout>
  );
}
