
import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from './services/api.js'
import MetricCard from './components/MetricCard.jsx'
import BatteryHistoryChart from './components/BatteryHistoryChart.jsx'
import DrainEventsTable from './components/DrainEventsTable.jsx'
import EventDetails from './components/EventDetails.jsx'
import ProcessRanking from './components/ProcessRanking.jsx'
import CurrentProcesses from './components/CurrentProcesses.jsx'

function batteryState(status) {
  const battery = status?.currentBattery
  if (!battery) return 'Waiting for telemetry'
  if (battery.charging) return 'Charging'
  if (battery.discharging) return 'Discharging'
  return 'State unavailable'
}

function rateLabel(value) {
  return value == null ? '—' : `${value.toFixed(1)}%/h`
}

export default function App() {
  const [status, setStatus] = useState(null)
  const [history, setHistory] = useState([])
  const [events, setEvents] = useState([])
  const [ranking, setRanking] = useState([])
  const [summary, setSummary] = useState(null)
  const [currentProcesses, setCurrentProcesses] = useState([])
  const [selectedEventId, setSelectedEventId] = useState(null)
  const [selectedEvent, setSelectedEvent] = useState(null)
  const [error, setError] = useState('')
  const [lastRefresh, setLastRefresh] = useState(null)

  const refresh = useCallback(async () => {
    try {
      const [
        nextStatus,
        nextHistory,
        nextEvents,
        nextRanking,
        nextSummary,
        nextProcesses
      ] = await Promise.all([
        api.status(),
        api.batteryHistory(24),
        api.drainIntervals(24),
        api.processRanking(24),
        api.analyticsSummary(24),
        api.currentProcesses()
      ])

      setStatus(nextStatus)
      setHistory(nextHistory)
      setEvents(nextEvents)
      setRanking(nextRanking)
      setSummary(nextSummary)
      setCurrentProcesses(nextProcesses)
      setError('')
      setLastRefresh(new Date())

      setSelectedEventId((current) => {
        if (current && nextEvents.some((event) => event.id === current)) return current
        return nextEvents[0]?.id ?? null
      })
    } catch (err) {
      setError(err.message || 'Unable to load telemetry')
    }
  }, [])

  useEffect(() => {
    refresh()
    const handle = window.setInterval(refresh, 5000)
    return () => window.clearInterval(handle)
  }, [refresh])

  useEffect(() => {
    if (!selectedEventId) {
      setSelectedEvent(null)
      return
    }

    let cancelled = false
    api.drainInterval(selectedEventId)
      .then((detail) => {
        if (!cancelled) setSelectedEvent(detail)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Unable to load event details')
      })

    return () => {
      cancelled = true
    }
  }, [selectedEventId, events])

  const batteryPercent = status?.currentBattery?.batteryPercent
  const batteryTone = batteryPercent != null && batteryPercent < 20 ? 'warning' : 'default'
  const telemetryDetail = status
    ? `${status.telemetryMode} mode · every ${(status.samplingIntervalMs / 1000).toFixed(0)}s`
    : 'Connecting to backend'

  const headerState = useMemo(() => {
    if (!status) return 'Connecting'
    if (status.databaseStatus !== 'UP') return 'Database unavailable'
    if (status.lastCollectionError) return 'Collector retrying'
    if (status.telemetryCollectionActive) return 'Collecting telemetry'
    return 'Starting collector'
  }, [status])

  return (
    <main className="app-shell">
      <header className="hero">
        <div>
          <div className="eyebrow">Windows battery telemetry + process correlation</div>
          <h1>Smart Battery Investigator</h1>
          <p>
            Detect unusually high battery drain and inspect the processes most strongly
            associated with those intervals.
          </p>
        </div>
        <div className="hero-status">
          <span className="live-dot" />
          <div>
            <strong>{headerState}</strong>
            <span>
              {lastRefresh ? `Updated ${lastRefresh.toLocaleTimeString()}` : 'Loading latest sample'}
            </span>
          </div>
        </div>
      </header>

      {error && (
        <div className="error-banner">
          <strong>Telemetry dashboard warning:</strong> {error}
        </div>
      )}

      <section className="metric-grid" aria-label="Current status">
        <MetricCard
          label="Battery"
          value={batteryPercent == null ? '—' : `${batteryPercent.toFixed(1)}%`}
          detail={batteryState(status)}
          tone={batteryTone}
        />
        <MetricCard
          label="Estimated drain"
          value={rateLabel(status?.currentEstimatedDrainRatePercentPerHour)}
          detail="Rolling battery-percentage window"
        />
        <MetricCard
          label="High-drain events"
          value={summary?.highDrainEventCount ?? '—'}
          detail="Detected in the last 24 hours"
        />
        <MetricCard
          label="Telemetry"
          value={status?.telemetryCollectionActive ? 'Active' : 'Starting'}
          detail={telemetryDetail}
          tone={status?.lastCollectionError ? 'warning' : 'success'}
        />
      </section>

      <section className="panel panel--wide">
        <div className="section-heading">
          <div>
            <span className="section-kicker">Battery History</span>
            <h2>Discharge trend</h2>
          </div>
          <div className="summary-inline">
            <span>
              Avg drain <strong>{rateLabel(summary?.averageDrainRatePercentPerHour)}</strong>
            </span>
            <span>
              Max drain <strong>{rateLabel(summary?.maximumDrainRatePercentPerHour)}</strong>
            </span>
          </div>
        </div>
        <BatteryHistoryChart samples={history} intervals={events} />
        <p className="section-note">
          Shaded regions mark intervals classified as unusually high drain. Charging and
          ambiguous power-state windows are excluded from drain-rate analysis.
        </p>
      </section>

      <div className="two-column-grid">
        <section className="panel">
          <div className="section-heading">
            <div>
              <span className="section-kicker">High Drain Events</span>
              <h2>Detected intervals</h2>
            </div>
          </div>
          <DrainEventsTable
            events={events}
            selectedId={selectedEventId}
            onSelect={setSelectedEventId}
          />
        </section>

        <section className="panel">
          <div className="section-heading">
            <div>
              <span className="section-kicker">Event Details</span>
              <h2>Process association</h2>
            </div>
          </div>
          <EventDetails detail={selectedEvent} />
        </section>
      </div>

      <div className="two-column-grid">
        <section className="panel">
          <div className="section-heading">
            <div>
              <span className="section-kicker">Process Analysis</span>
              <h2>Repeated high-drain associations</h2>
            </div>
          </div>
          <ProcessRanking rows={ranking} />
        </section>

        <section className="panel">
          <div className="section-heading">
            <div>
              <span className="section-kicker">Current Snapshot</span>
              <h2>Significant running processes</h2>
            </div>
          </div>
          <CurrentProcesses rows={currentProcesses} />
        </section>
      </div>

      <footer>
        Smart Battery Investigator treats process rankings as correlation evidence, not proof of
        direct power causation.
      </footer>
    </main>
  )
}
