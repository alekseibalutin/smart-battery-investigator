
CREATE TABLE IF NOT EXISTS battery_sample (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sampled_at INTEGER NOT NULL,
    battery_percent REAL,
    remaining_capacity INTEGER,
    capacity_units TEXT,
    voltage_volts REAL,
    discharge_rate_watts REAL,
    charging INTEGER,
    discharging INTEGER,
    power_online INTEGER,
    source_name TEXT,
    telemetry_mode TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_battery_sample_sampled_at
    ON battery_sample(sampled_at);

CREATE TABLE IF NOT EXISTS process_sample (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    battery_sample_id INTEGER NOT NULL,
    sampled_at INTEGER NOT NULL,
    pid INTEGER NOT NULL,
    process_name TEXT NOT NULL,
    cpu_usage_percent REAL NOT NULL,
    memory_bytes INTEGER NOT NULL,
    executable_path TEXT,
    process_start_time INTEGER,
    cumulative_cpu_ms INTEGER NOT NULL,
    FOREIGN KEY (battery_sample_id) REFERENCES battery_sample(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_process_sample_sampled_at
    ON process_sample(sampled_at);

CREATE INDEX IF NOT EXISTS idx_process_sample_battery_sample_id
    ON process_sample(battery_sample_id);

CREATE INDEX IF NOT EXISTS idx_process_sample_process_name
    ON process_sample(process_name);

CREATE TABLE IF NOT EXISTS drain_interval (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    start_time INTEGER NOT NULL,
    end_time INTEGER NOT NULL,
    starting_battery_percent REAL NOT NULL,
    ending_battery_percent REAL NOT NULL,
    calculated_drain_rate REAL NOT NULL,
    severity TEXT NOT NULL,
    baseline_mean REAL,
    baseline_stddev REAL
);

CREATE INDEX IF NOT EXISTS idx_drain_interval_end_time
    ON drain_interval(end_time);

CREATE TABLE IF NOT EXISTS process_drain_score (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    interval_id INTEGER NOT NULL,
    process_name TEXT NOT NULL,
    score REAL NOT NULL,
    average_cpu_usage REAL NOT NULL,
    baseline_cpu_usage REAL NOT NULL,
    cpu_increase_above_baseline REAL NOT NULL,
    average_memory_bytes REAL NOT NULL,
    presence_ratio REAL NOT NULL,
    sample_count INTEGER NOT NULL,
    FOREIGN KEY (interval_id) REFERENCES drain_interval(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_process_drain_score_interval_process
    ON process_drain_score(interval_id, process_name);

CREATE INDEX IF NOT EXISTS idx_process_drain_score_interval_id
    ON process_drain_score(interval_id);
