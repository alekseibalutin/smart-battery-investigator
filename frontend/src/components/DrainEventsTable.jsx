
function durationLabel(seconds) {
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.round(seconds / 60)
  return `${minutes} min`
}

export default function DrainEventsTable({ events, selectedId, onSelect }) {
  if (!events.length) {
    return (
      <div className="empty-state">
        No high-drain intervals have been detected in this period.
      </div>
    )
  }

  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Start</th>
            <th>Duration</th>
            <th>Lost</th>
            <th>Drain rate</th>
            <th>Severity</th>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr
              key={event.id}
              className={selectedId === event.id ? 'is-selected' : ''}
              onClick={() => onSelect(event.id)}
            >
              <td>{new Date(event.startTime).toLocaleString()}</td>
              <td>{durationLabel(event.durationSeconds)}</td>
              <td>{event.batteryPercentLost.toFixed(1)}%</td>
              <td>{event.calculatedDrainRatePercentPerHour.toFixed(1)}%/h</td>
              <td>
                <span className={`severity severity--${event.severity.toLowerCase()}`}>
                  {event.severity}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
