import { useMemo } from 'react'
import { useSelector } from 'react-redux'

import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  Legend,
} from 'recharts'

const STATUS_COLORS = {
  SUCCESS: '#16a34a',
  FAILED: '#dc2626',
  CREATED: '#f59e0b',
  PROCESSING: '#3b82f6',
  REFUNDED: '#8b5cf6',
}

function AnalyticsCharts() {
  const {
    paymentTrends,
    paymentStatus,
    gatewayPerformance,
    loading,
    error,
  } = useSelector((state) => state.analytics)

  // Prepare payment trend data.
  // Fill dates without transactions with zero.
  const trendData = useMemo(() => {
    const countsByDate = new Map(
      paymentTrends.map((item) => [
        item.date,
        item.transactionCount,
      ]),
    )

    const today = new Date()
    const result = []

    for (let i = 29; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(today.getDate() - i)

      const dateKey = [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0'),
      ].join('-')

      result.push({
        date: dateKey,
        transactionCount: countsByDate.get(dateKey) ?? 0,
      })
    }

    return result
  }, [paymentTrends])

  // Prepare payment status data.
  const statusData = useMemo(
    () =>
      paymentStatus
        .filter((item) => item.count > 0)
        .map((item) => ({
          name: item.status,
          value: item.count,
          fill: STATUS_COLORS[item.status] || '#64748b',
        })),
    [paymentStatus],
  )

  // Prepare gateway performance data.
  const gatewayData = useMemo(
    () =>
      gatewayPerformance.map((gateway) => ({
        gatewayName: gateway.gatewayName,
        totalTransactions: gateway.totalTransactions,
        successfulTransactions: gateway.successfulTransactions,
        failedTransactions: gateway.failedTransactions,
        successRate: gateway.successRate,
      })),
    [gatewayPerformance],
  )

  if (loading) {
    return (
      <section className="analytics-loading panel">
        Loading analytics charts...
      </section>
    )
  }

  if (error) {
    return (
      <section className="analytics-error panel">
        <h3>Unable to load analytics</h3>
        <p>{error}</p>
      </section>
    )
  }

  return (
    <section className="analytics-charts">

      {/* PAYMENT TRENDS */}

      <div className="panel analytics-panel">
        <div className="panel-header">
          <div>
            <h3>Payment Trends</h3>
            <p>Daily transaction activity · Last 30 days</p>
          </div>
        </div>

        {trendData.length === 0 ? (
          <p className="chart-empty">No payment trend data available.</p>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart
              data={trendData}
              margin={{ top: 10, right: 20, left: 0, bottom: 0 }}
            >
              <defs>
                <linearGradient
                  id="paymentTrendGradient"
                  x1="0"
                  y1="0"
                  x2="0"
                  y2="1"
                >
                  <stop
                    offset="0%"
                    stopColor="#7653e8"
                    stopOpacity={0.35}
                  />
                  <stop
                    offset="100%"
                    stopColor="#7653e8"
                    stopOpacity={0.02}
                  />
                </linearGradient>
              </defs>

              <CartesianGrid
                strokeDasharray="3 3"
                vertical={false}
              />

              <XAxis
                dataKey="date"
                tickFormatter={(value) => value.slice(5)}
                tick={{ fontSize: 11 }}
              />

              <YAxis
                allowDecimals={false}
                tick={{ fontSize: 11 }}
              />

              <Tooltip />

              <Area
                type="monotone"
                dataKey="transactionCount"
                name="Transactions"
                stroke="#7653e8"
                strokeWidth={2}
                fill="url(#paymentTrendGradient)"
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* PAYMENT STATUS DISTRIBUTION */}

      <div className="panel analytics-panel">
        <div className="panel-header">
          <div>
            <h3>Payment Status Distribution</h3>
            <p>Transaction breakdown by status</p>
          </div>
        </div>

        {statusData.length === 0 ? (
          <p className="chart-empty">No payment status data available.</p>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={statusData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                innerRadius={65}
                outerRadius={105}
                paddingAngle={3}
                label={({ name, percent }) =>
                  `${name} ${(percent * 100).toFixed(1)}%`
                }
              >
                {statusData.map((entry) => (
                  <Cell
                    key={entry.name}
                    fill={entry.fill}
                  />
                ))}
              </Pie>

              <Tooltip />

              <Legend />
            </PieChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* GATEWAY PERFORMANCE */}

      <div className="panel analytics-panel gateway-chart-panel">
        <div className="panel-header">
          <div>
            <h3>Gateway Performance</h3>
            <p>Successful and failed payment transactions</p>
          </div>
        </div>

        {gatewayData.length === 0 ? (
          <p className="chart-empty">
            No gateway performance data available.
          </p>
        ) : (
          <ResponsiveContainer width="100%" height={320}>
            <BarChart
              data={gatewayData}
              margin={{ top: 20, right: 20, left: 0, bottom: 5 }}
            >
              <CartesianGrid
                strokeDasharray="3 3"
                vertical={false}
              />

              <XAxis
                dataKey="gatewayName"
                tick={{ fontSize: 12 }}
              />

              <YAxis
                allowDecimals={false}
              />

              <Tooltip />

              <Legend />

              <Bar
                dataKey="successfulTransactions"
                name="Successful"
                fill="#16a34a"
                radius={[4, 4, 0, 0]}
              />

              <Bar
                dataKey="failedTransactions"
                name="Failed"
                fill="#dc2626"
                radius={[4, 4, 0, 0]}
              />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

    </section>
  )
}

export default AnalyticsCharts