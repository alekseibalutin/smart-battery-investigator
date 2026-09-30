
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceArea,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts'

function compactTime(value) {
  return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export default function BatteryHistoryChart({ samples, intervals }) {
  const data = samples
    .filter((sample) => sample.batteryPercent != null)
    .map((sample) => ({
      ...sample,
      time: new Date(sample.timestamp).getTime()
    }))

  if (data.length < 2) {
    return <div className="empty-state">Waiting for enough battery history to draw the chart.</div>
  }

  return (
    <div className="chart-wrap">
      <ResponsiveContainer width="100%" height={320}>
        <LineChart data={data} margin={{ top: 12, right: 18, left: -14, bottom: 4 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis
            dataKey="time"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={compactTime}
            tick={{ fontSize: 12 }}
          />
          <YAxis
            domain={['auto', 'auto']}
            unit="%"
            tick={{ fontSize: 12 }}
          />
          <Tooltip
            labelFormatter={(value) => new Date(value).toLocaleString()}
            formatter={(value) => [`${Number(value).toFixed(1)}%`, 'Battery']}
          />
          {intervals.map((interval) => (
            <ReferenceArea
              key={interval.id}
              x1={new Date(interval.startTime).getTime()}
              x2={new Date(interval.endTime).getTime()}
              fillOpacity={0.12}
            />
          ))}
          <Line
            type="monotone"
            dataKey="batteryPercent"
            strokeWidth={2.4}
            dot={false}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
