import React, { useCallback, useState } from 'react';
import { Alert, Box, Card, Snackbar, Tab, Tabs, Typography } from '@mui/material';
import { useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import CustomerTab from './master/CustomerTab';
import ProductTab from './master/ProductTab';
import PriceTab from './master/PriceTab';
import StockTab from './master/StockTab';

const TABS = [
  { value: 'customer', label: '거래처', Component: CustomerTab },
  { value: 'product', label: '제품', Component: ProductTab },
  { value: 'price', label: '단가', Component: PriceTab },
  { value: 'stock', label: '재고', Component: StockTab },
];

/**
 * 마스터 데이터 화면. 거래처·제품·단가·재고를 탭으로 묶었다.
 * 넷은 서로를 참조하므로 화면을 나누면 오가는 비용이 크다.
 */
export default function MasterPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [toast, setToast] = useState(null);

  const tab = TABS.find((t) => t.value === searchParams.get('tab')) ?? TABS[0];
  const Active = tab.Component;

  // 탭 컴포넌트가 매 렌더마다 새 함수를 받으면 useEffect 가 계속 다시 돈다
  const notify = useCallback(
    (message, severity = 'success') => setToast({ message, severity }),
    []
  );

  return (
    <Layout>
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
          🗂️ 마스터 관리
        </Typography>

        <Card sx={{ marginBottom: 2 }}>
          <Tabs
            value={tab.value}
            onChange={(e, v) => setSearchParams({ tab: v })}
            variant="scrollable"
            scrollButtons="auto"
          >
            {TABS.map((t) => (
              <Tab key={t.value} value={t.value} label={t.label} />
            ))}
          </Tabs>
        </Card>

        <Active notify={notify} />

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
