package db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerDao {
    private static final int MIN_VALID_TIME_MS = 5 * 60 * 1000 + 45 * 1000;
    private static final int MIN_FASTEST_TIME_MS = 5 * 60 * 1000 + 30 * 1000;

    public static class FastestCompletionRow {
        public String uuid;
        public String nickname;
        public String country;
        public Integer bestTime;
        public Integer eloRate;
        public Integer eloRank;
    }

    public static class FastestAverageRow {
        public String uuid;
        public String nickname;
        public String country;
        public Integer matches;
        public Double avgTime;
        public Integer eloRate;
        public Integer eloRank;
    }

    public static class BestWinRateRow {
        public String uuid;
        public String nickname;
        public String country;
        public Integer matches;
        public Integer wins;
        public Double winRate;
        public Integer eloRate;
        public Integer eloRank;
    }

    public static class SeasonResultRow {
        public int season;
        public int rank;
        public Integer endElo;
    }

    public static class PlayerSeasonRow {
        public String uuid;
        public String nickname;
        public int eloRate;
        public int eloRank;
        public String country;
        public Integer season;
        public Integer wins;
        public Integer loses;
        public Integer bestTime;
    }

    public static class AvgByKey {
        public String key;
        public Double avgTime;
        public int matches;
        public int wins;
        public Double winRate;
    }

    public static class PlayerProfileStats {
        public int totalMatches;
        public int wins;
        public Double avgCompletionTime;
        public Double winRate;
        public Integer overallBestTime;

        public List<AvgByKey> overworldAverages = new ArrayList<>();
        public List<AvgByKey> netherAverages = new ArrayList<>();

        public List<SeasonResultRow> seasonResults = new ArrayList<>();
    }

    public List<PlayerSeasonRow> findPlayerByNickname(String nicknameLike) throws SQLException {
        String sql = """
            SELECT u.uuid, u.nickname, u.eloRate, u.eloRank, u.country,
                   s.season, s.wins_ranked, s.loses_ranked, s.bestTime_ranked
            FROM users u
            LEFT JOIN user_season_ranked_stats s ON u.uuid = s.uuid
            WHERE u.nickname LIKE ?
              AND (s.season IS NULL OR s.season = 9)
            ORDER BY u.eloRate DESC, u.nickname ASC
            """;

        List<PlayerSeasonRow> rows = new ArrayList<>();
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nicknameLike + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerSeasonRow r = new PlayerSeasonRow();
                    r.uuid = rs.getString("uuid");
                    r.nickname = rs.getString("nickname");
                    r.eloRate = rs.getInt("eloRate");
                    r.eloRank = rs.getInt("eloRank");
                    r.country = rs.getString("country");
                    r.season = (Integer) rs.getObject("season");
                    r.wins = (Integer) rs.getObject("wins_ranked");
                    r.loses = (Integer) rs.getObject("loses_ranked");
                    r.bestTime = (Integer) rs.getObject("bestTime_ranked");
                    rows.add(r);
                }
            }
        }
        return rows;
    }

    public PlayerSeasonRow getPlayerSummaryByUuid(String uuid) throws SQLException {
        String sql = """
        SELECT u.uuid, u.nickname, u.eloRate, u.eloRank, u.country,
               s.season, s.wins_ranked, s.loses_ranked, s.bestTime_ranked
        FROM users u
        LEFT JOIN user_season_ranked_stats s
               ON u.uuid = s.uuid AND s.season = 9
        WHERE u.uuid = ?
        """;

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                PlayerSeasonRow r = new PlayerSeasonRow();
                r.uuid = rs.getString("uuid");
                r.nickname = rs.getString("nickname");
                r.eloRate = rs.getInt("eloRate");
                r.eloRank = rs.getInt("eloRank");
                r.country = rs.getString("country");
                r.season = (Integer) rs.getObject("season");
                r.wins = (Integer) rs.getObject("wins_ranked");
                r.loses = (Integer) rs.getObject("loses_ranked");
                r.bestTime = (Integer) rs.getObject("bestTime_ranked");
                return r;
            }
        }
    }

    public List<PlayerSeasonRow> searchPlayersForCompare(String nicknameLike) throws SQLException {
        String sql = """
            SELECT u.uuid, u.nickname, u.eloRate, u.eloRank, u.country
            FROM users u
            WHERE u.nickname LIKE ?
            ORDER BY u.nickname ASC
            """;

        List<PlayerSeasonRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nicknameLike + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerSeasonRow r = new PlayerSeasonRow();
                    r.uuid = rs.getString("uuid");
                    r.nickname = rs.getString("nickname");
                    r.eloRate = rs.getInt("eloRate");
                    r.eloRank = rs.getInt("eloRank");
                    r.country = rs.getString("country");
                    rows.add(r);
                }
            }
        }
        return rows;
    }

    public Integer getSeasonEloRank(String uuid, Integer seasonFilter) throws SQLException {
        try (Connection conn = DbManager.getConnection()) {

            if (seasonFilter == null) {
                String sql = "SELECT eloRank FROM users WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int v = rs.getInt("eloRank");
                            return rs.wasNull() ? null : v;
                        }
                    }
                }
                return null;
            }

            String sub = """
                SELECT
                    MAX(CASE WHEN d.data_index = 0 THEN d.data_value END) AS season_str,
                    MAX(CASE WHEN d.data_index = 1 THEN d.data_value END) AS rank_str
                FROM user_achievements a
                JOIN user_achievement_data d
                  ON d.uuid = a.uuid
                 AND d.achievement_id = a.achievement_id
                 AND d.achievement_date = a.achievement_date
                WHERE a.uuid = ?
                  AND a.achievement_id = 'seasonResult'
                GROUP BY a.achievement_date
                """;

            String sql = "SELECT season_str, rank_str FROM (" + sub + ") t " +
                    "WHERE season_str = ? LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setString(2, Integer.toString(seasonFilter));

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String rankStr = rs.getString("rank_str");
                        if (rankStr != null && !rankStr.isEmpty()) {
                            try {
                                return Integer.parseInt(rankStr);
                            } catch (NumberFormatException ignore) {
                            }
                        }
                    }
                }
            }

            String fallback = "SELECT eloRank FROM users WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(fallback)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int v = rs.getInt("eloRank");
                        return rs.wasNull() ? null : v;
                    }
                }
            }
        }

        return null;
    }

    public List<Integer> getSeasonsForPlayer(String uuid) throws SQLException {
        String sql = """
            SELECT DISTINCT m.season
            FROM matches m
            JOIN match_players mp ON mp.match_id = m.id AND mp.uuid = ?
            WHERE m.season IS NOT NULL
              AND m.type = 2
            ORDER BY m.season
            """;

        List<Integer> seasons = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seasons.add(rs.getInt("season"));
                }
            }
        }
        return seasons;
    }

    public List<FastestCompletionRow> getFastestCompletionLeaderboard(int limit) throws SQLException {
        String sql = """
        
                SELECT u.uuid, u.nickname, u.country,
               u.total_bestTime_ranked AS best_time,
               u.eloRate, u.eloRank
        FROM users u
        WHERE u.total_bestTime_ranked IS NOT NULL
          AND u.total_bestTime_ranked >= ?
        ORDER BY u.total_bestTime_ranked ASC
        LIMIT ?
        """;

        List<FastestCompletionRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setInt(idx++, MIN_FASTEST_TIME_MS);
            ps.setInt(idx++, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FastestCompletionRow r = new FastestCompletionRow();
                    r.uuid = rs.getString("uuid");
                    r.nickname = rs.getString("nickname");
                    r.country = rs.getString("country");
                    int bt = rs.getInt("best_time");
                    r.bestTime = rs.wasNull() ? null : bt;
                    r.eloRate = (Integer) rs.getObject("eloRate");
                    r.eloRank = (Integer) rs.getObject("eloRank");
                    rows.add(r);
                }
            }
        }

        return rows;
    }

    public List<FastestAverageRow> getFastestAverageLeaderboard(Integer seasonFilter, int limit) throws SQLException {

        String where = "WHERE m.type = 2";
        if (seasonFilter != null) {
            where += " AND m.season = ?";
        }

        String sql = "SELECT u.uuid, u.nickname, u.country, u.eloRate, u.eloRank, " +
                "       COUNT(*) AS matches, " +
                "       AVG(CASE WHEN m.result_time IS NOT NULL " +
                "                 AND m.result_time >= ? " +
                "            THEN m.result_time END) AS avg_time " +
                "FROM users u " +
                "JOIN match_players mp ON mp.uuid = u.uuid " +
                "JOIN matches m ON m.id = mp.match_id " +
                where + " " +
                "GROUP BY u.uuid, u.nickname, u.country, u.eloRate, u.eloRank " +
                "HAVING avg_time IS NOT NULL " +
                "   AND matches >= 100 " +
                "ORDER BY avg_time ASC " +
                "LIMIT ?";

        List<FastestAverageRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setInt(idx++, MIN_VALID_TIME_MS);
            if (seasonFilter != null) {
                ps.setInt(idx++, seasonFilter);
            }
            ps.setInt(idx++, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FastestAverageRow r = new FastestAverageRow();
                    r.uuid = rs.getString("uuid");
                    r.nickname = rs.getString("nickname");
                    r.country = rs.getString("country");
                    r.matches = rs.getInt("matches");
                    double avg = rs.getDouble("avg_time");
                    r.avgTime = rs.wasNull() ? null : avg;
                    r.eloRate = (Integer) rs.getObject("eloRate");
                    r.eloRank = (Integer) rs.getObject("eloRank");
                    rows.add(r);
                }
            }
        }

        return rows;
    }

    public List<BestWinRateRow> getBestWinRateLeaderboard(Integer seasonFilter, int limit) throws SQLException {

        String where = "WHERE m.type = 2";
        if (seasonFilter != null) {
            where += " AND m.season = ?";
        }

        String sql = "SELECT u.uuid, u.nickname, u.country, u.eloRate, u.eloRank, " +
                "       COUNT(*) AS matches, " +
                "       SUM(CASE WHEN m.result_uuid = u.uuid THEN 1 ELSE 0 END) AS wins, " +
                "       (SUM(CASE WHEN m.result_uuid = u.uuid THEN 1 ELSE 0 END) * 1.0 / COUNT(*)) AS win_rate " +
                "FROM users u " +
                "JOIN match_players mp ON mp.uuid = u.uuid " +
                "JOIN matches m ON m.id = mp.match_id " +
                where + " " +
                "GROUP BY u.uuid, u.nickname, u.country, u.eloRate, u.eloRank " +
                "HAVING matches >= 100 " +
                "ORDER BY win_rate DESC, matches DESC " +
                "LIMIT ?";

        List<BestWinRateRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            if (seasonFilter != null) {
                ps.setInt(idx++, seasonFilter);
            }
            ps.setInt(idx++, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BestWinRateRow r = new BestWinRateRow();
                    r.uuid = rs.getString("uuid");
                    r.nickname = rs.getString("nickname");
                    r.country = rs.getString("country");
                    r.matches = rs.getInt("matches");
                    r.wins = rs.getInt("wins");
                    double wr = rs.getDouble("win_rate");
                    r.winRate = rs.wasNull() ? null : wr;
                    r.eloRate = (Integer) rs.getObject("eloRate");
                    r.eloRank = (Integer) rs.getObject("eloRank");
                    rows.add(r);
                }
            }
        }

        return rows;
    }

    public PlayerProfileStats getPlayerProfileStats(String uuid, Integer seasonFilter) throws SQLException {
        PlayerProfileStats stats = new PlayerProfileStats();

        try (Connection conn = DbManager.getConnection()) {
            fillOverallStats(conn, uuid, seasonFilter, stats);
            fillOverallBestTime(conn, uuid, stats);
            fillOverworldStats(conn, uuid, seasonFilter, stats);
            fillNetherStats(conn, uuid, seasonFilter, stats);
            fillSeasonResults(conn, uuid, stats);
        }

        return stats;
    }

    private void fillOverallStats(Connection conn, String uuid, Integer seasonFilter,
                                  PlayerProfileStats stats) throws SQLException {

        String where = "WHERE mp.uuid = ? AND m.type = 2";
        if (seasonFilter != null) {
            where += " AND m.season = ?";
        }

        String sql = "SELECT " +
                "COUNT(*) AS total_matches, " +
                "SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS wins, " +
                "AVG(CASE WHEN m.result_time IS NOT NULL " +
                "          AND m.result_time >= ? " +
                "     THEN m.result_time END) AS avg_time " +
                "FROM matches m " +
                "JOIN match_players mp ON mp.match_id = m.id " +
                where;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, uuid);
            ps.setInt(idx++, MIN_VALID_TIME_MS);
            ps.setString(idx++, uuid);
            if (seasonFilter != null) {
                ps.setInt(idx++, seasonFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.totalMatches = rs.getInt("total_matches");
                    stats.wins = rs.getInt("wins");

                    double avg = rs.getDouble("avg_time");
                    stats.avgCompletionTime = rs.wasNull() ? null : avg;

                    if (stats.totalMatches > 0) {
                        stats.winRate = stats.wins / (double) stats.totalMatches;
                    } else {
                        stats.winRate = null;
                    }
                }
            }
        }
    }

    private void fillOverallBestTime(Connection conn, String uuid,
                                     PlayerProfileStats stats) throws SQLException {

        String sql = "SELECT total_bestTime_ranked " +
                "FROM users " +
                "WHERE uuid = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int v = rs.getInt("total_bestTime_ranked");
                    stats.overallBestTime = rs.wasNull() ? null : v;
                }
            }
        }
    }

    private void fillOverworldStats(Connection conn, String uuid, Integer seasonFilter,
                                    PlayerProfileStats stats) throws SQLException {

        String sql = "SELECT s.seed_overworld AS ow_key, " +
                "       COUNT(*) AS matches, " +
                "       SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS wins, " +
                "       AVG(CASE WHEN m.result_time IS NOT NULL " +
                "                 AND m.result_time >= ? " +
                "            THEN m.result_time END) AS avg_time " +
                "FROM matches m " +
                "JOIN match_players mp ON mp.match_id = m.id " +
                "JOIN seeds s ON s.seed_id = m.seed_id " +
                "WHERE mp.uuid = ? " +
                "  AND m.type = 2 ";

        if (seasonFilter != null) {
            sql += "AND m.season = ? ";
        }

        sql += "GROUP BY s.seed_overworld";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, uuid);
            ps.setInt(idx++, MIN_VALID_TIME_MS);
            ps.setString(idx++, uuid);
            if (seasonFilter != null) {
                ps.setInt(idx++, seasonFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AvgByKey a = new AvgByKey();
                    a.key = rs.getString("ow_key");
                    a.matches = rs.getInt("matches");
                    a.wins = rs.getInt("wins");

                    double avg = rs.getDouble("avg_time");
                    a.avgTime = rs.wasNull() ? null : avg;

                    a.winRate = a.matches > 0 ? a.wins / (double) a.matches : null;

                    stats.overworldAverages.add(a);
                }
            }
        }
    }

    private void fillNetherStats(Connection conn, String uuid, Integer seasonFilter,
                                 PlayerProfileStats stats) throws SQLException {

        String sql = "SELECT s.seed_nether AS n_key, " +
                "       COUNT(*) AS matches, " +
                "       SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS wins, " +
                "       AVG(CASE WHEN m.result_time IS NOT NULL " +
                "                 AND m.result_time >= ? " +
                "            THEN m.result_time END) AS avg_time " +
                "FROM matches m " +
                "JOIN match_players mp ON mp.match_id = m.id " +
                "JOIN seeds s ON s.seed_id = m.seed_id " +
                "WHERE mp.uuid = ? " +
                "  AND m.type = 2 ";

        if (seasonFilter != null) {
            sql += "AND m.season = ? ";
        }

        sql += "GROUP BY s.seed_nether";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, uuid);
            ps.setInt(idx++, MIN_VALID_TIME_MS);
            ps.setString(idx++, uuid);
            if (seasonFilter != null) {
                ps.setInt(idx++, seasonFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AvgByKey a = new AvgByKey();
                    a.key = rs.getString("n_key");
                    a.matches = rs.getInt("matches");
                    a.wins = rs.getInt("wins");

                    double avg = rs.getDouble("avg_time");
                    a.avgTime = rs.wasNull() ? null : avg;

                    a.winRate = a.matches > 0 ? a.wins / (double) a.matches : null;

                    stats.netherAverages.add(a);
                }
            }
        }
    }

    private void fillSeasonResults(Connection conn, String uuid,
                                   PlayerProfileStats stats) throws SQLException {

        String achievementSql = """
            SELECT a.achievement_date,
                   MAX(CASE WHEN d.data_index = 0 THEN d.data_value END) AS season_str,
                   MAX(CASE WHEN d.data_index = 1 THEN d.data_value END) AS rank_str
            FROM user_achievements a
            JOIN user_achievement_data d
                ON d.uuid = a.uuid
               AND d.achievement_id = a.achievement_id
               AND d.achievement_date = a.achievement_date
            WHERE a.uuid = ?
              AND a.achievement_id = 'seasonResult'
            GROUP BY a.achievement_date
            """;

        List<SeasonResultRow> rows = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(achievementSql)) {
            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String seasonStr = rs.getString("season_str");
                    String rankStr   = rs.getString("rank_str");

                    if (seasonStr == null || rankStr == null) {
                        continue;
                    }

                    try {
                        SeasonResultRow r = new SeasonResultRow();
                        r.season = Integer.parseInt(seasonStr);
                        r.rank   = Integer.parseInt(rankStr);
                        r.endElo = null;
                        rows.add(r);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }

        if (rows.isEmpty()) {
            stats.seasonResults = rows;
            return;
        }

        String lastMatchSql = """
            SELECT
                m.date,
                (mp.eloRate_before + COALESCE(mp.elo_change, 0)) AS end_elo
            FROM matches m
            JOIN match_players mp ON mp.match_id = m.id
            WHERE m.type = 2
              AND m.season = ?
              AND mp.uuid = ?
            ORDER BY m.date DESC
            LIMIT 1
            """;

        try (PreparedStatement ps = conn.prepareStatement(lastMatchSql)) {
            for (SeasonResultRow r : rows) {
                ps.setInt(1, r.season);
                ps.setString(2, uuid);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int endElo = rs.getInt("end_elo");
                        r.endElo = rs.wasNull() ? null : endElo;
                    } else {
                        r.endElo = null;
                    }
                }
            }
        }

        stats.seasonResults = rows;
    }
}