
# Analysis Algorithm

## 1. Battery drain rate

A single percentage transition is too noisy because many laptop batteries update their reported
percentage in discrete steps. The application therefore evaluates a rolling window.

For a valid discharging window:

```text
percentageLost = startingPercent - endingPercent
durationHours = windowDurationMilliseconds / 3_600_000
drainRatePercentPerHour = percentageLost / durationHours
```

A window is excluded when:

- fewer than two battery samples exist;
- battery percentage is unavailable;
- any sample says the battery is charging;
- any sample does not positively identify the battery as discharging;
- the reported battery percentage rises;
- the sample timestamps do not span a meaningful portion of the configured rolling window.

The default rolling window is five minutes.

## 2. High-drain detection

The current rolling drain rate is first compared with an absolute floor:

```text
currentRate >= minimumHighDrainPercentPerHour
```

When enough historical discharging windows are available, the current rate must also exceed:

```text
historicalMean + standardDeviationMultiplier * historicalStandardDeviation
```

With insufficient historical data, the absolute threshold is used as a cold-start fallback.
This allows the application to operate immediately without inventing a baseline.

Default values:

- minimum high drain: `10 %/hour`
- standard-deviation multiplier: `1.5`
- minimum historical windows: `3`
- historical baseline horizon: `24 hours`

Severity is descriptive:

- `MODERATE`: at least the minimum threshold
- `HIGH`: at least 1.75× the minimum threshold
- `CRITICAL`: at least 2.5× the minimum threshold

Consecutive overlapping detections are merged into one persisted interval.

## 3. Process association score

The application cannot measure per-process electrical power directly with ordinary OSHI process
metrics. It therefore produces an association score rather than a causation claim.

For each process during a high-drain interval:

- average CPU usage is calculated;
- a pre-interval CPU baseline is calculated;
- CPU increase above baseline is calculated;
- average memory usage is calculated;
- presence ratio is the fraction of interval timestamps where the process was retained.

The normalized score is:

```text
score =
    0.50 * normalizedAverageCpu
  + 0.25 * normalizedCpuIncrease
  + 0.10 * normalizedMemory
  + 0.15 * presenceRatio
```

Normalization caps are intentionally simple and explainable:

- average CPU reaches full contribution at 40%;
- CPU increase reaches full contribution at +25 percentage points;
- memory reaches full contribution at 2 GiB;
- presence ratio is already in the 0–1 range.

The final value is multiplied by 100 and clamped naturally by the normalized inputs.

This score should be interpreted as: **how strongly the process telemetry is associated with the
high-drain interval**, not how much electrical power the process definitively consumed.

## 4. Process filtering

The Windows provider samples all visible processes but persists only processes that satisfy at least
one configured threshold:

- CPU usage >= `0.5%`, or
- resident memory >= `75 MiB`.

The retained list is sorted by CPU and memory and capped at 80 processes per sample by default.
This avoids filling SQLite with hundreds of insignificant rows while preserving likely contributors.
