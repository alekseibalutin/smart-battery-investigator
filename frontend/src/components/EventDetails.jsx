
function formatMemory(bytes) {
  if (bytes == null) return '—'
  return `${(bytes / 1024 / 1024).toFixed(0)} MB`
}

export default function EventDetails({ detail }) {
  if (!detail) {
    return (
      <div className="empty-state">
        Select a high-drain event to inspect its associated processes.
      </div>
    )
  }

  const { interval, associatedProcesses } = detail

  return (
    <div className="event-detail">
      <div className="event-detail__summary">
        <div>
          <span>Battery drop</span>
          <strong>{interval.batteryPercentLost.toFixed(1)}%</strong>
        </div>
        <div>
          <span>Drain rate</span>
          <strong>{interval.calculatedDrainRatePercentPerHour.toFixed(1)}%/h</strong>
        </div>
        <div>
          <span>Baseline</span>
          <strong>
            {interval.baselineMean == null ? 'insufficient history' : `${interval.baselineMean.toFixed(1)}%/h`}
          </strong>
        </div>
      </div>

      <h3>Associated Processes</h3>
      <p className="section-note">
        Scores represent correlation with this high-drain interval, not proof of causation.
      </p>

      {!associatedProcesses.length ? (
        <div className="empty-state">No process samples were available for this interval.</div>
      ) : (
        <div className="process-score-list">
          {associatedProcesses.map((process, index) => (
            <article className="process-score" key={process.processName}>
              <div className="process-score__rank">{index + 1}</div>
              <div className="process-score__body">
                <div className="process-score__headline">
                  <strong>{process.processName}</strong>
                  <span>{process.score.toFixed(0)} / 100</span>
                </div>
                <div className="score-bar">
                  <span style={{ width: `${Math.min(100, process.score)}%` }} />
                </div>
                <div className="process-score__meta">
                  <span>CPU {process.averageCpuUsage.toFixed(1)}%</span>
                  <span>Baseline {process.baselineCpuUsage.toFixed(1)}%</span>
                  <span>Memory {formatMemory(process.averageMemoryBytes)}</span>
                  <span>Presence {(process.presenceRatio * 100).toFixed(0)}%</span>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
