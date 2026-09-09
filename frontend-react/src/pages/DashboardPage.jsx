import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Card,
  Chip,
  CircularProgress,
  Grid,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import BusinessIcon from '@mui/icons-material/Business';
import DescriptionIcon from '@mui/icons-material/Description';
import Layout from '../components/Layout';
import client, { toMessage } from '../api/client';

const COLORS = ['#1976d2', '#e64980', '#2f9e44', '#f08c00', '#7048e8', '#0c8599'];

const money = (v, digits = 2) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });

function SummaryCard({ icon: Icon, title, value, unit, color }) {
  return (
    <Card sx={{ padding: 2, backgroundColor: color, color: 'white', height: '100%' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="caption" sx={{ opacity: 0.85 }}>
            {title}
          </Typography>
          <Typography variant="h6" sx={{ fontWeight: 'bold', marginTop: 0.5 }} noWrap>
            {value}
            {unit && (
              <Typography component="span" variant="caption">
                {unit}
              </Typography>
            )}
          </Typography>
        </Box>
        <Icon sx={{ fontSize: 40, opacity: 0.35 }} />
      </Box>
    </Card>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        // M6 통계 API (PIVOT + 윈도우 함수)
        const summaryResponse = await client.get('/statistics/dashboard-summary');
        setData(summaryResponse.data);
      } catch (e) {
        setToast({ message: toMessage(e), severity: 'error' });
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const stats = useMemo(() => {
    if (!data) return null;

    // M6 통계 API 응답 구조
    const {
      totalSales,
      activeCustomers,
      activeProducts,
      totalSalesOrders,
      outstandingReceivables,
      monthlySales,
      topCustomers,
      topProducts,
      agingBuckets,
    } = data;

    // 월별 매출 데이터 변환
    const salesTrend = (monthlySales || []).map(sale => ({
      month: new Date(sale.month).toLocaleDateString('ko-KR', { year: '2-digit', month: '2-digit' }),
      sales: Math.round(Number(sale.krwSales || 0) / 1000000), // 백만원 단위
      growth: Number(sale.yoyGrowthRate || 0),
    }));

    // 거래처별 매출 (TOP 5)
    const customerSales = (topCustomers || []).map(c => ({
      name: c.customerName,
      sales: Math.round(Number(c.krwSales || 0) / 1000000),
    }));

    // 미수금 Aging (원형 그래프)
    const agingChart = (agingBuckets || []).map(b => ({
      name: b.agingBucket,
      value: Number(b.invoiceCount || 0),
    }));

    return {
      totalSales: Number(totalSales || 0),
      receivable: Number(outstandingReceivables || 0),
      activeCustomers: Number(activeCustomers || 0),
      activeProducts: Number(activeProducts || 0),
      orderCount: Number(totalSalesOrders || 0),
      topCustomer: customerSales[0],
      customerSales,
      agingChart,
      topProducts: (topProducts || []).slice(0, 6).map(p => ({
        name: p.productCode,
        qty: Math.round(Number(p.totalQty || 0)),
      })),
      salesTrend,
    };
  }, [data]);

  if (loading) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', padding: 6 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  if (!stats) {
    return (
      <Layout>
        <Alert severity="error">데이터를 불러오지 못했습니다. 백엔드가 실행 중인지 확인하세요.</Alert>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 3 }}>
          📊 대시보드
        </Typography>

        <Grid container spacing={2} sx={{ marginBottom: 3 }}>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={AttachMoneyIcon}
              title="총 매출 (KRW)"
              value={`₩${money(stats.totalSales / 1000000, 1)}`}
              unit="M"
              color="#1976d2"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={TrendingUpIcon}
              title="미수금 잔액 (KRW)"
              value={`₩${money(stats.receivable / 1000000, 1)}`}
              unit="M"
              color="#e64980"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={Inventory2Icon}
              title="활성 제품"
              value={stats.activeProducts}
              unit=" 종"
              color="#2f9e44"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={BusinessIcon}
              title="TOP 거래처"
              value={stats.topCustomer?.name ?? '-'}
              unit={stats.topCustomer ? ` ₩${money(stats.topCustomer.sales, 0)}M` : ''}
              color="#f08c00"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={DescriptionIcon}
              title="활성 거래처"
              value={stats.activeCustomers}
              unit=" 개"
              color="#7048e8"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={ShoppingCartIcon}
              title="총 수주"
              value={stats.orderCount}
              unit=" 건"
              color="#0c8599"
            />
          </Grid>
        </Grid>

        <Grid container spacing={3} sx={{ marginBottom: 3 }}>
          <Grid size={{ xs: 12, md: 7 }}>
            <Card sx={{ padding: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                📈 월별 매출 추이 (KRW)
              </Typography>
              {stats.salesTrend.length === 0 ? (
                <Alert severity="info">매출 데이터가 없습니다.</Alert>
              ) : (
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={stats.salesTrend}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="month" />
                    <YAxis />
                    <Tooltip formatter={(v) => `₩${money(v)}M`} />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey="sales"
                      stroke="#1976d2"
                      name="매출액"
                      strokeWidth={2}
                      dot={{ r: 4 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 5 }}>
            <Card sx={{ padding: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                💰 미수금 Aging
              </Typography>
              {stats.agingChart.length === 0 ? (
                <Alert severity="info">미수금 데이터가 없습니다.</Alert>
              ) : (
                <>
                  <ResponsiveContainer width="100%" height={240}>
                    <PieChart>
                      <Pie
                        data={stats.agingChart}
                        cx="50%"
                        cy="50%"
                        innerRadius={55}
                        outerRadius={95}
                        paddingAngle={4}
                        dataKey="value"
                      >
                        {stats.agingChart.map((entry, index) => (
                          <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(v) => `${v} 건`} />
                    </PieChart>
                  </ResponsiveContainer>
                  <Box sx={{ marginTop: 1 }}>
                    {stats.agingChart.map((item, idx) => (
                      <Box
                        key={item.name}
                        sx={{ display: 'flex', alignItems: 'center', gap: 1, marginBottom: 0.5 }}
                      >
                        <Box
                          sx={{
                            width: 12,
                            height: 12,
                            backgroundColor: COLORS[idx % COLORS.length],
                            borderRadius: '50%',
                          }}
                        />
                        <Typography variant="caption">
                          {item.name} — {item.value}건
                        </Typography>
                      </Box>
                    ))}
                  </Box>
                </>
              )}
            </Card>
          </Grid>
        </Grid>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ padding: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                🏭 제품별 매출 TOP 6
              </Typography>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={stats.topProducts}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 10 }} angle={-20} textAnchor="end" height={60} />
                  <YAxis />
                  <Tooltip formatter={(v) => money(v, 0)} />
                  <Legend />
                  <Bar dataKey="qty" fill="#2f9e44" name="판매 수량" />
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ padding: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                🏆 거래처별 매출 TOP 5
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                    <TableRow>
                      <TableCell>순위</TableCell>
                      <TableCell>거래처</TableCell>
                      <TableCell align="right">매출 (KRW)</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {stats.customerSales.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={3} align="center" sx={{ color: '#999', padding: 3 }}>
                          매출 데이터가 없습니다.
                        </TableCell>
                      </TableRow>
                    )}
                    {stats.customerSales.map((c, idx) => (
                      <TableRow key={idx}>
                        <TableCell sx={{ fontWeight: 'bold' }}>{idx + 1}</TableCell>
                        <TableCell>{c.name}</TableCell>
                        <TableCell align="right">
                          ₩{money(c.sales, 0)}M
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Card>
          </Grid>
        </Grid>

        <Snackbar
          open={Boolean(toast)}
          autoHideDuration={5000}
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
