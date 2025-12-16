package db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatchDao {

    public static class MatchRow {
        public int id;
        public Integer type;
        public String category;
        public String gameMode;
        public Integer season;
        public Integer date;
        public String resultUuid;
        public String winnerNickname;
        public Integer resultTime;
        public String seedType;
        public String bastionType;
    }

    public static class HeadToHeadOverall {
        public int matches;
        public int winsA;
        public int winsB;
        public Double winRateA;
        public Double winRateB;
    }

    public static class HeadToHeadByKeyRow {
        public String key;
        public int matches;
        public int winsA;
        public int winsB;
        public Double winRateA;
        public Double winRateB;
    }

    public static class HeadToHeadSummary {
        public int total;
        public int winsA;
        public int winsB;
    }

    public static class LeaderboardRow {
        public String uuid;
        public String nickname;
        public String country;
        public int eloRate;
        public int eloRank;
    }

    public List<MatchRow> listRecentMatches(Integer seasonFilter) throws SQLException {
        String sql = "SELECT m.id, m.type, m.category, m.gameMode, m.season, m.date, " +
                "       m.result_uuid, u.nickname AS winner_nick, " +
                "       m.result_time, s.seedType, s.bastionType " +
                "FROM matches m " +
                "LEFT JOIN users u ON m.result_uuid = u.uuid " +
                "LEFT JOIN seeds s ON m.seed_id = s.seed_id ";

        if (seasonFilter != null) {
            sql += "WHERE m.season = ? ";
        }

        sql += "ORDER BY m.date DESC LIMIT 200";

        List<MatchRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (seasonFilter != null) {
                ps.setInt(1, seasonFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatchRow r = new MatchRow();
                    r.id = rs.getInt("id");
                    r.type = (Integer) rs.getObject("type");
                    r.category = rs.getString("category");
                    r.gameMode = rs.getString("gameMode");
                    r.season = (Integer) rs.getObject("season");
                    r.date = (Integer) rs.getObject("date");
                    r.resultUuid = rs.getString("result_uuid");
                    r.winnerNickname = rs.getString("winner_nick");
                    r.resultTime = (Integer) rs.getObject("result_time");
                    r.seedType = rs.getString("seedType");
                    r.bastionType = rs.getString("bastionType");
                    rows.add(r);
                }
            }
        }

        return rows;
    }

    public HeadToHeadOverall getHeadToHeadOverall(String uuidA, String uuidB, Integer seasonFilter) throws SQLException {
        String where = "WHERE m.type = 2";
        if (seasonFilter != null) {
            where += " AND m.season = ?";
        }

        String sql = "SELECT " +
                "COUNT(*) AS matches, " +
                "SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS winsA, " +
                "SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS winsB " +
                "FROM matches m " +
                "JOIN match_players mpA ON mpA.match_id = m.id AND mpA.uuid = ? " +
                "JOIN match_players mpB ON mpB.match_id = m.id AND mpB.uuid = ? " +
                where;

        HeadToHeadOverall out = new HeadToHeadOverall();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, uuidA);
            ps.setString(idx++, uuidB);
            ps.setString(idx++, uuidA);
            ps.setString(idx++, uuidB);
            if (seasonFilter != null) {
                ps.setInt(idx++, seasonFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.matches = rs.getInt("matches");
                    out.winsA = rs.getInt("winsA");
                    out.winsB = rs.getInt("winsB");
                    if (out.matches > 0) {
                        out.winRateA = out.winsA / (double) out.matches;
                        out.winRateB = out.winsB / (double) out.matches;
                    } else {
                        out.winRateA = null;
                        out.winRateB = null;
                    }
                }
            }
        }

        return out;
    }

    public List<HeadToHeadByKeyRow> getHeadToHeadByOverworld(String uuidA, String uuidB, Integer seasonFilter) throws SQLException {
        String where = "WHERE m.type = 2";
        if (seasonFilter != null) {
            where += " AND m.season = ?";
        }

        String sql = "SELECT s.seed_overworld AS k, " +
                "       COUNT(*) AS matches, " +
                "       SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS winsA, " +
                "       SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS winsB " +
                "FROM matches m " +
                "JOIN match_players mpA ON mpA.match_id = m.id AND mpA.uuid = ? " +
                "JOIN match_players mpB ON mpB.match_id = m.id AND mpB.uuid = ? " +
                "JOIN seeds s ON s.seed_id = m.seed_id " +
                where + " " +
                "GROUP BY s.seed_overworld";

        List<HeadToHeadByKeyRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, uuidA);
            ps.setString(idx++, uuidB);
            ps.setString(idx++, uuidA);
            ps.setString(idx++, uuidB);
            if (seasonFilter != null) {
                ps.setInt(idx++, seasonFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HeadToHeadByKeyRow r = new HeadToHeadByKeyRow();
                    r.key = rs.getString("k");
                    r.matches = rs.getInt("matches");
                    r.winsA = rs.getInt("winsA");
                    r.winsB = rs.getInt("winsB");
                    if (r.matches > 0) {
                        r.winRateA = r.winsA / (double) r.matches;
                        r.winRateB = r.winsB / (double) r.matches;
                    } else {
                        r.winRateA = null;
                        r.winRateB = null;
                    }
                    rows.add(r);
                }
            }
        }

        return rows;
    }

    public List<HeadToHeadByKeyRow> getHeadToHeadByBastion(String uuidA, String uuidB, Integer seasonFilter) throws SQLException {
        String where = "WHERE m.type = 2";
        if (seasonFilter != null) {
            where += " AND m.season = ?";
        }

        String sql = "SELECT s.bastionType AS k, " +
                "       COUNT(*) AS matches, " +
                "       SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS winsA, " +
                "       SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS winsB " +
                "FROM matches m " +
                "JOIN match_players mpA ON mpA.match_id = m.id AND mpA.uuid = ? " +
                "JOIN match_players mpB ON mpB.match_id = m.id AND mpB.uuid = ? " +
                "JOIN seeds s ON s.seed_id = m.seed_id " +
                where + " " +
                "GROUP BY s.bastionType";

        List<HeadToHeadByKeyRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, uuidA);
            ps.setString(idx++, uuidB);
            ps.setString(idx++, uuidA);
            ps.setString(idx++, uuidB);
            if (seasonFilter != null) {
                ps.setInt(idx++, seasonFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HeadToHeadByKeyRow r = new HeadToHeadByKeyRow();
                    r.key = rs.getString("k");
                    r.matches = rs.getInt("matches");
                    r.winsA = rs.getInt("winsA");
                    r.winsB = rs.getInt("winsB");
                    if (r.matches > 0) {
                        r.winRateA = r.winsA / (double) r.matches;
                        r.winRateB = r.winsB / (double) r.matches;
                    } else {
                        r.winRateA = null;
                        r.winRateB = null;
                    }
                    rows.add(r);
                }
            }
        }

        return rows;
    }

    public HeadToHeadSummary getHeadToHeadSummary(String uuidA, String uuidB) throws SQLException {
        String sql = "SELECT " +
                "  SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS winsA, " +
                "  SUM(CASE WHEN m.result_uuid = ? THEN 1 ELSE 0 END) AS winsB, " +
                "  COUNT(*) AS total " +
                "FROM matches m " +
                "JOIN match_players a ON a.match_id = m.id AND a.uuid = ? " +
                "JOIN match_players b ON b.match_id = m.id AND b.uuid = ?";

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, uuidA);
            ps.setString(idx++, uuidB);
            ps.setString(idx++, uuidA);
            ps.setString(idx++, uuidB);

            try (ResultSet rs = ps.executeQuery()) {
                HeadToHeadSummary summary = new HeadToHeadSummary();
                if (rs.next()) {
                    summary.winsA = rs.getInt("winsA");
                    summary.winsB = rs.getInt("winsB");
                    summary.total = rs.getInt("total");
                }
                return summary;
            }
        }
    }

    public List<MatchRow> listHeadToHeadMatches(String uuidA, String uuidB) throws SQLException {
        String sql = "SELECT m.id, m.type, m.category, m.gameMode, m.season, m.date, " +
                "       m.result_uuid, w.nickname AS winner_nick, " +
                "       m.result_time, s.seedType, s.bastionType " +
                "FROM matches m " +
                "JOIN match_players a ON a.match_id = m.id AND a.uuid = ? " +
                "JOIN match_players b ON b.match_id = m.id AND b.uuid = ? " +
                "LEFT JOIN users w ON m.result_uuid = w.uuid " +
                "LEFT JOIN seeds s ON m.seed_id = s.seed_id " +
                "ORDER BY m.date DESC";

        List<MatchRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuidA);
            ps.setString(2, uuidB);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatchRow r = new MatchRow();
                    r.id = rs.getInt("id");
                    r.type = (Integer) rs.getObject("type");
                    r.category = rs.getString("category");
                    r.gameMode = rs.getString("gameMode");
                    r.season = (Integer) rs.getObject("season");
                    r.date = (Integer) rs.getObject("date");
                    r.resultUuid = rs.getString("result_uuid");
                    r.winnerNickname = rs.getString("winner_nick");
                    r.resultTime = (Integer) rs.getObject("result_time");
                    r.seedType = rs.getString("seedType");
                    r.bastionType = rs.getString("bastionType");
                    rows.add(r);
                }
            }
        }

        return rows;
    }

    public List<LeaderboardRow> getLeaderboard() throws SQLException {
        String sql = "SELECT uuid, nickname, country, eloRate, eloRank " +
                "FROM users " +
                "WHERE eloRate IS NOT NULL " +
                "ORDER BY eloRate DESC " +
                "LIMIT 50";

        List<LeaderboardRow> rows = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                LeaderboardRow r = new LeaderboardRow();
                r.uuid = rs.getString("uuid");
                r.nickname = rs.getString("nickname");
                r.country = rs.getString("country");
                r.eloRate = rs.getInt("eloRate");
                r.eloRank = rs.getInt("eloRank");
                rows.add(r);
            }
        }

        return rows;
    }
}