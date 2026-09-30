
function formatMemory(bytes) {
  return `${(bytes / 1024 / 1024).toFixed(0)} MB`
}

export default function ProcessRanking({ rows }) {
  if (!rows.length) {
    return (
      <div className="empty-state">
        Process rankings will appear after high-drain events are detected.
      </div>
    )
  }

  return (
    <div className="ranking-list">
      {rows.map((row, index) => (
        <article className="ranking-row" key={row.processName}>
          <span className="ranking-row__index">{index + 1}</span>
          <div className="ranking-row__main">
            <strong>{row.processName}</strong>
            <span>
              {row.highDrainIntervalCount} event{row.highDrainIntervalCount === 1 ? '' : 's'} ·{' '}
              {row.averageCpuUsage.toFixed(1)}% avg CPU · {formatMemory(row.averageMemoryBytes)}
            </span>
          </div>
          <strong className="ranking-row__score">{row.averageScore.toFixed(0)}</strong>
        </article>
      ))}
    </div>
  )
}
