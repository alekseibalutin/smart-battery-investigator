
package com.alexbalutin.smartbattery.repository;

import com.alexbalutin.smartbattery.model.ProcessDrainScore;
import com.alexbalutin.smartbattery.model.ProcessRankingAggregate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ProcessDrainScoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessDrainScoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replaceForInterval(long intervalId, List<ProcessDrainScore> scores) {
        jdbcTemplate.update("DELETE FROM process_drain_score WHERE interval_id = ?", intervalId);

        if (scores.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO process_drain_score (
                    interval_id, process_name, score, average_cpu_usage, baseline_cpu_usage,
                    cpu_increase_above_baseline, average_memory_bytes, presence_ratio, sample_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, scores, 50, (statement, score) -> {
            statement.setLong(1, intervalId);
            statement.setString(2, score.processName());
            statement.setDouble(3, score.score());
            statement.setDouble(4, score.averageCpuUsage());
            statement.setDouble(5, score.baselineCpuUsage());
            statement.setDouble(6, score.cpuIncreaseAboveBaseline());
            statement.setDouble(7, score.averageMemoryBytes());
            statement.setDouble(8, score.presenceRatio());
            statement.setInt(9, score.sampleCount());
        });
    }

    public List<ProcessDrainScore> findByIntervalId(long intervalId) {
        return jdbcTemplate.query("""
                        SELECT * FROM process_drain_score
                        WHERE interval_id = ?
                        ORDER BY score DESC
                        """,
                (rs, rowNum) -> new ProcessDrainScore(
                        rs.getLong("id"),
                        rs.getLong("interval_id"),
                        rs.getString("process_name"),
                        rs.getDouble("score"),
                        rs.getDouble("average_cpu_usage"),
                        rs.getDouble("baseline_cpu_usage"),
                        rs.getDouble("cpu_increase_above_baseline"),
                        rs.getDouble("average_memory_bytes"),
                        rs.getDouble("presence_ratio"),
                        rs.getInt("sample_count")),
                intervalId);
    }

    public List<ProcessRankingAggregate> findRanking(Instant since, int limit) {
        return jdbcTemplate.query("""
                        SELECT
                            pds.process_name,
                            AVG(pds.score) AS average_score,
                            COUNT(DISTINCT pds.interval_id) AS interval_count,
                            AVG(pds.average_cpu_usage) AS average_cpu_usage,
                            AVG(pds.average_memory_bytes) AS average_memory_bytes,
                            SUM(pds.sample_count) AS sample_count
                        FROM process_drain_score pds
                        JOIN drain_interval di ON di.id = pds.interval_id
                        WHERE di.end_time >= ?
                        GROUP BY pds.process_name
                        ORDER BY interval_count DESC, average_score DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new ProcessRankingAggregate(
                        rs.getString("process_name"),
                        rs.getDouble("average_score"),
                        rs.getLong("interval_count"),
                        rs.getDouble("average_cpu_usage"),
                        rs.getDouble("average_memory_bytes"),
                        rs.getLong("sample_count")),
                since.toEpochMilli(),
                limit);
    }
}
