import React, { useState, useEffect } from 'react';
import {
  IconButton,
  Badge,
  Popover,
  List,
  ListItemButton,
  Divider,
  Box,
  Typography,
  Chip,
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import ClearAllIcon from '@mui/icons-material/ClearAll';
import client from '../api/client';

const notificationTypeLabel = {
  DELIVERY_DUE: '납기 임박',
  PAYMENT_OVERDUE: '결제 연체',
  QNA_ANSWERED: 'Q&A 답변',
};

export default function NotificationPanel() {
  const [anchorEl, setAnchorEl] = useState(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchUnreadCount = async () => {
    try {
      const response = await client.get('/notifications/unread-count');
      setUnreadCount(response.data);
    } catch (error) {
      console.error('미읽음 알림 개수 조회 실패:', error);
    }
  };

  const fetchNotifications = async () => {
    if (loading) return;
    setLoading(true);
    try {
      const response = await client.get('/notifications/unread', {
        params: { page: 0, size: 10 },
      });
      setNotifications(response.data.content || []);
    } catch (error) {
      console.error('알림 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleOpen = (event) => {
    setAnchorEl(event.currentTarget);
    fetchNotifications();
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleMarkAsRead = async (notifId) => {
    try {
      await client.post(`/notifications/${notifId}/read`);
      setNotifications(notifications.filter((n) => n.id !== notifId));
      setUnreadCount(Math.max(0, unreadCount - 1));
    } catch (error) {
      console.error('알림 읽음 처리 실패:', error);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await client.post('/notifications/mark-all-as-read');
      setNotifications([]);
      setUnreadCount(0);
    } catch (error) {
      console.error('모든 알림 읽음 처리 실패:', error);
    }
  };

  const open = Boolean(anchorEl);

  return (
    <>
      <IconButton
        color="inherit"
        onClick={handleOpen}
        sx={{ position: 'relative', marginRight: 1 }}
      >
        <Badge badgeContent={unreadCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>

      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Box sx={{ width: 400, maxHeight: 500, display: 'flex', flexDirection: 'column' }}>
          <Box
            sx={{
              p: 2,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              borderBottom: '1px solid #eee',
            }}
          >
            <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
              알림 ({unreadCount})
            </Typography>
            {unreadCount > 0 && (
              <IconButton
                size="small"
                onClick={handleMarkAllAsRead}
                title="모두 읽음 처리"
              >
                <ClearAllIcon fontSize="small" />
              </IconButton>
            )}
          </Box>

          <List sx={{ flex: 1, overflowY: 'auto' }}>
            {notifications.length === 0 ? (
              <Box sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="body2" color="textSecondary">
                  새로운 알림이 없습니다
                </Typography>
              </Box>
            ) : (
              notifications.map((notif, idx) => (
                <React.Fragment key={notif.id}>
                  <ListItemButton
                    onClick={() => handleMarkAsRead(notif.id)}
                    sx={{
                      backgroundColor: '#f9f9f9',
                      '&:hover': { backgroundColor: '#f0f0f0' },
                      p: 1.5,
                    }}
                  >
                    <Box sx={{ flex: 1 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                          {notif.title}
                        </Typography>
                        <Chip
                          label={notificationTypeLabel[notif.notifType] || notif.notifType}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                      </Box>
                      <Typography variant="caption" color="textSecondary">
                        {notif.message}
                      </Typography>
                      <Typography variant="caption" display="block" color="textSecondary" sx={{ mt: 0.5 }}>
                        {new Date(notif.createdAt).toLocaleString('ko-KR')}
                      </Typography>
                    </Box>
                  </ListItemButton>
                  {idx < notifications.length - 1 && <Divider />}
                </React.Fragment>
              ))
            )}
          </List>
        </Box>
      </Popover>
    </>
  );
}
