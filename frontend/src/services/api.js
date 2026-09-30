
async function getJson(path) {
  const response = await fetch(path)
  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`
    try {
      const body = await response.json()
      message = body.message || message
    } catch {
      // Keep the HTTP status text when the body is not JSON.
    }
    throw new Error(message)
  }
  return response.json()
}

export const api = {
  status: () => getJson('/api/status'),
  batteryHistory: (hours = 24) => getJson(`/api/battery/history?hours=${hours}`),
  currentProcesses: () => getJson('/api/processes/current'),
  drainIntervals: (hours = 24) => getJson(`/api/drain-intervals?hours=${hours}`),
  drainInterval: (id) => getJson(`/api/drain-intervals/${id}`),
  processRanking: (hours = 24) => getJson(`/api/processes/ranking?hours=${hours}`),
  analyticsSummary: (hours = 24) => getJson(`/api/analytics/summary?hours=${hours}`)
}
