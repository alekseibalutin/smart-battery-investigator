
function memoryMiB(bytes) {
  return (bytes / 1024 / 1024).toFixed(0)
}

export default function CurrentProcesses({ rows }) {
  if (!rows.length) {
    return <div className="empty-state">No significant process samples in the latest snapshot.</div>
  }

  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Process</th>
            <th>PID</th>
            <th>CPU</th>
            <th>Memory</th>
          </tr>
        </thead>
        <tbody>
          {rows.slice(0, 12).map((row) => (
            <tr key={`${row.pid}-${row.processName}`}>
              <td>{row.processName}</td>
              <td>{row.pid}</td>
              <td>{row.cpuUsagePercent.toFixed(1)}%</td>
              <td>{memoryMiB(row.memoryBytes)} MB</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
