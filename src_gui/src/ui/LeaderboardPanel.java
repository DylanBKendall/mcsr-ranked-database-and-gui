package ui;

import db.MatchDao;
import db.MatchDao.LeaderboardRow;
import db.PlayerDao;
import db.PlayerDao.FastestCompletionRow;
import db.PlayerDao.FastestAverageRow;
import db.PlayerDao.BestWinRateRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LeaderboardPanel extends JPanel {

    private DefaultTableModel eloModel;

    private DefaultTableModel fastestCompletionModel;

    private JTextField avgSeasonField;
    private DefaultTableModel fastestAverageModel;

    private JTextField winSeasonField;
    private DefaultTableModel bestWinRateModel;

    public LeaderboardPanel() {
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Elo", createEloTab());
        tabs.addTab("Fastest Time", createFastestCompletionTab());
        tabs.addTab("Fastest Average", createFastestAverageTab());
        tabs.addTab("Best Win%", createBestWinRateTab());

        add(tabs, BorderLayout.CENTER);

        loadEloLeaderboard();
        loadFastestCompletion();
        avgSeasonField.setText("9");
        loadFastestAverage();
        winSeasonField.setText("9");
        loadBestWinRate();
    }

    private JPanel createEloTab() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] cols = { "Rank", "Nickname", "UUID", "Country", "Elo Rate" };
        eloModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 4 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        JTable table = new JTable(eloModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(eloModel));

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadEloLeaderboard() {
        eloModel.setRowCount(0);

        try {
            List<LeaderboardRow> rows = new MatchDao().getLeaderboard();
            int displayedRank = 1;
            for (LeaderboardRow r : rows) {
                eloModel.addRow(new Object[] {
                        displayedRank++,
                        r.nickname,
                        r.uuid,
                        r.country,
                        r.eloRate
                });
            }
        } catch (SQLException ex) {
            showError("Error loading Elo leaderboard", ex);
        }
    }

    private JPanel createFastestCompletionTab() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] cols = { "Rank", "Nickname", "UUID", "Country", "Best Time", "Elo Rate", "Elo Rank" };
        fastestCompletionModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 5, 6 -> Integer.class;
                    case 4 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        JTable table = new JTable(fastestCompletionModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(fastestCompletionModel));
        table.getColumnModel().getColumn(4).setCellRenderer(new TimeCellRenderer());

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadFastestCompletion() {
        fastestCompletionModel.setRowCount(0);

        try {
            List<FastestCompletionRow> rows = new PlayerDao().getFastestCompletionLeaderboard(100);
            int rank = 1;
            for (FastestCompletionRow r : rows) {
                fastestCompletionModel.addRow(new Object[] {
                        rank++,
                        r.nickname,
                        r.uuid,
                        r.country,
                        r.bestTime,
                        r.eloRate,
                        r.eloRank
                });
            }
        } catch (SQLException ex) {
            showError("Error loading fastest completion leaderboard", ex);
        }
    }

    private JPanel createFastestAverageTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel();
        top.add(new JLabel("Season (blank = all):"));
        avgSeasonField = new JTextField(4);
        top.add(avgSeasonField);
        JButton loadButton = new JButton("Load");
        top.add(loadButton);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = { "Rank", "Nickname", "UUID", "Country", "Avg Time", "# Matches", "Elo Rate", "Elo Rank" };
        fastestAverageModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 5, 6, 7 -> Integer.class;
                    case 4 -> Double.class;
                    default -> String.class;
                };
            }
        };

        JTable table = new JTable(fastestAverageModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(fastestAverageModel));
        table.getColumnModel().getColumn(4).setCellRenderer(new TimeCellRenderer());

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        loadButton.addActionListener(e -> loadFastestAverage());

        return panel;
    }

    private void loadFastestAverage() {
        fastestAverageModel.setRowCount(0);

        String text = avgSeasonField.getText().trim();
        Integer season = null;
        if (!text.isEmpty()) {
            try {
                season = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Season must be an integer or blank.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            List<FastestAverageRow> rows = new PlayerDao().getFastestAverageLeaderboard(season, 100);
            int rank = 1;
            for (FastestAverageRow r : rows) {
                fastestAverageModel.addRow(new Object[] {
                        rank++,
                        r.nickname,
                        r.uuid,
                        r.country,
                        r.avgTime,
                        r.matches,
                        r.eloRate,
                        r.eloRank
                });
            }
        } catch (SQLException ex) {
            showError("Error loading fastest average leaderboard", ex);
        }
    }

    private JPanel createBestWinRateTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel();
        top.add(new JLabel("Season (blank = all):"));
        winSeasonField = new JTextField(4);
        top.add(winSeasonField);
        JButton loadButton = new JButton("Load");
        top.add(loadButton);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = { "Rank", "Nickname", "UUID", "Country", "Win %", "# Matches", "Wins", "Elo Rate", "Elo Rank" };
        bestWinRateModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 5, 6, 7, 8 -> Integer.class;
                    case 4 -> Double.class;
                    default -> String.class;
                };
            }
        };

        JTable table = new JTable(bestWinRateModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(bestWinRateModel));
        table.getColumnModel().getColumn(4).setCellRenderer(new PercentCellRenderer());

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        loadButton.addActionListener(e -> loadBestWinRate());

        return panel;
    }

    private void loadBestWinRate() {
        bestWinRateModel.setRowCount(0);

        String text = winSeasonField.getText().trim();
        Integer season = null;
        if (!text.isEmpty()) {
            try {
                season = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Season must be an integer or blank.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            List<BestWinRateRow> rows = new PlayerDao().getBestWinRateLeaderboard(season, 100);
            int rank = 1;
            for (BestWinRateRow r : rows) {
                bestWinRateModel.addRow(new Object[] {
                        rank++,
                        r.nickname,
                        r.uuid,
                        r.country,
                        r.winRate,
                        r.matches,
                        r.wins,
                        r.eloRate,
                        r.eloRank
                });
            }
        } catch (SQLException ex) {
            showError("Error loading best win% leaderboard", ex);
        }
    }

    private void showError(String msg, Exception ex) {
        JOptionPane.showMessageDialog(this,
                msg + ":\n" + ex.getMessage(),
                "DB Error", JOptionPane.ERROR_MESSAGE);
    }
}