package ui;

import db.MatchDao;
import db.MatchDao.HeadToHeadOverall;
import db.MatchDao.HeadToHeadByKeyRow;
import db.PlayerDao;
import db.PlayerDao.PlayerProfileStats;
import db.PlayerDao.PlayerSeasonRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ComparePanel extends JPanel {

    private final JTextField searchAField = new JTextField(12);
    private final JTextField searchBField = new JTextField(12);
    private final JButton searchAButton = new JButton("Search A");
    private final JButton searchBButton = new JButton("Search B");

    private final JComboBox<PlayerSeasonRow> comboA = new JComboBox<>();
    private final JComboBox<PlayerSeasonRow> comboB = new JComboBox<>();

    private final JTextField seasonField = new JTextField(4);
    private final JButton compareButton = new JButton("Compare");

    private final JLabel summaryALabel = new JLabel("Player A summary");
    private final JLabel summaryBLabel = new JLabel("Player B summary");

    private final JLabel h2hSummaryLabel = new JLabel("Head-to-head summary");

    private final DefaultTableModel overworldModel;
    private final DefaultTableModel bastionModel;

    public ComparePanel() {
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row;
        top.add(new JLabel("Player A nick:"), gbc);
        gbc.gridx = 1;
        top.add(searchAField, gbc);
        gbc.gridx = 2;
        top.add(searchAButton, gbc);

        gbc.gridx = 3;
        top.add(new JLabel("Player B nick:"), gbc);
        gbc.gridx = 4;
        top.add(searchBField, gbc);
        gbc.gridx = 5;
        top.add(searchBButton, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        top.add(new JLabel("Select A:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        top.add(comboA, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 3;
        top.add(new JLabel("Select B:"), gbc);
        gbc.gridx = 4; gbc.gridwidth = 2;
        top.add(comboB, gbc);
        gbc.gridwidth = 1;

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        top.add(new JLabel("Season (blank = all, default 9):"), gbc);
        gbc.gridx = 1;
        top.add(seasonField, gbc);
        gbc.gridx = 2;
        top.add(compareButton, gbc);

        add(top, BorderLayout.NORTH);

        seasonField.setText("9");

        DefaultListCellRenderer comboRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                if (value instanceof PlayerSeasonRow r) {
                    String shortUuid = r.uuid != null && r.uuid.length() >= 8
                            ? r.uuid.substring(0, 8)
                            : r.uuid;
                    value = r.nickname + " [" + (r.country == null ? "??" : r.country) + "] (" + shortUuid + ")";
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        };
        comboA.setRenderer(comboRenderer);
        comboB.setRenderer(comboRenderer);

        JPanel summaries = new JPanel(new GridLayout(1, 2, 8, 8));
        summaryALabel.setVerticalAlignment(SwingConstants.TOP);
        summaryBLabel.setVerticalAlignment(SwingConstants.TOP);
        summaries.add(wrapInTitled(summaryALabel, "Player A"));
        summaries.add(wrapInTitled(summaryBLabel, "Player B"));

        overworldModel = new DefaultTableModel(
                new Object[]{"Overworld Type", "Matches", "A Wins", "B Wins", "A Win %", "B Win %"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 1, 2, 3 -> Integer.class;
                    case 4, 5 -> Double.class;
                    default -> String.class;
                };
            }
        };
        JTable owTable = new JTable(overworldModel);
        owTable.setRowHeight(24);
        owTable.setAutoCreateRowSorter(true);
        owTable.setRowSorter(new TableRowSorter<>(overworldModel));
        owTable.getColumnModel().getColumn(4).setCellRenderer(new PercentCellRenderer());
        owTable.getColumnModel().getColumn(5).setCellRenderer(new PercentCellRenderer());

        bastionModel = new DefaultTableModel(
                new Object[]{"Bastion Type", "Matches", "A Wins", "B Wins", "A Win %", "B Win %"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 1, 2, 3 -> Integer.class;
                    case 4, 5 -> Double.class;
                    default -> String.class;
                };
            }
        };
        JTable bastionTable = new JTable(bastionModel);
        bastionTable.setRowHeight(24);
        bastionTable.setAutoCreateRowSorter(true);
        bastionTable.setRowSorter(new TableRowSorter<>(bastionModel));
        bastionTable.getColumnModel().getColumn(4).setCellRenderer(new PercentCellRenderer());
        bastionTable.getColumnModel().getColumn(5).setCellRenderer(new PercentCellRenderer());

        JSplitPane h2hTables = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(owTable),
                new JScrollPane(bastionTable)
        );
        h2hTables.setResizeWeight(0.5);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(summaries, BorderLayout.NORTH);
        center.add(wrapInTitled(h2hTables, "Head-to-head by Overworld / Bastion"), BorderLayout.CENTER);
        center.add(h2hSummaryLabel, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        searchAButton.addActionListener(e -> doSearch(true));
        searchBButton.addActionListener(e -> doSearch(false));
        compareButton.addActionListener(e -> doCompare());
    }

    private JPanel wrapInTitled(JComponent comp, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void doSearch(boolean isA) {
        String term = (isA ? searchAField : searchBField).getText().trim();
        if (term.isEmpty()) return;

        try {
            PlayerDao dao = new PlayerDao();
            List<PlayerSeasonRow> results = dao.searchPlayersForCompare(term);

            JComboBox<PlayerSeasonRow> combo = isA ? comboA : comboB;
            combo.removeAllItems();
            for (PlayerSeasonRow r : results) {
                combo.addItem(r);
            }

            if (combo.getItemCount() > 0) {
                combo.setSelectedIndex(0);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No players found for '" + term + "'.",
                        "No Results", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error searching players:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doCompare() {
        PlayerSeasonRow selA = (PlayerSeasonRow) comboA.getSelectedItem();
        PlayerSeasonRow selB = (PlayerSeasonRow) comboB.getSelectedItem();

        if (selA == null || selB == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select both Player A and Player B.",
                    "Missing Player", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selA.uuid.equals(selB.uuid)) {
            JOptionPane.showMessageDialog(this,
                    "Please select two different players.",
                    "Invalid Selection", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String seasonText = seasonField.getText().trim();
        Integer seasonFilter = null;
        if (!seasonText.isEmpty()) {
            try {
                seasonFilter = Integer.parseInt(seasonText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Season must be an integer or blank.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            PlayerDao pdao = new PlayerDao();
            MatchDao mdao = new MatchDao();

            PlayerProfileStats statsA = pdao.getPlayerProfileStats(selA.uuid, seasonFilter);
            PlayerProfileStats statsB = pdao.getPlayerProfileStats(selB.uuid, seasonFilter);

            Integer eloRankA = pdao.getSeasonEloRank(selA.uuid, seasonFilter);
            Integer eloRankB = pdao.getSeasonEloRank(selB.uuid, seasonFilter);

            HeadToHeadOverall h2h = mdao.getHeadToHeadOverall(selA.uuid, selB.uuid, seasonFilter);
            List<HeadToHeadByKeyRow> h2hOW = mdao.getHeadToHeadByOverworld(selA.uuid, selB.uuid, seasonFilter);
            List<HeadToHeadByKeyRow> h2hBast = mdao.getHeadToHeadByBastion(selA.uuid, selB.uuid, seasonFilter);

            summaryALabel.setText(buildPlayerSummary(selA, statsA, eloRankA));
            summaryBLabel.setText(buildPlayerSummary(selB, statsB, eloRankB));

            String label = String.format(
                    "<html>Head-to-head matches: %d &nbsp;&nbsp; " +
                            "%s wins: %d (%.1f%%) &nbsp;&nbsp; " +
                            "%s wins: %d (%.1f%%)</html>",
                    h2h.matches,
                    selA.nickname, h2h.winsA, (h2h.winRateA == null ? 0.0 : h2h.winRateA * 100.0),
                    selB.nickname, h2h.winsB, (h2h.winRateB == null ? 0.0 : h2h.winRateB * 100.0)
            );
            h2hSummaryLabel.setText(label);

            overworldModel.setRowCount(0);
            for (HeadToHeadByKeyRow r : h2hOW) {
                overworldModel.addRow(new Object[]{
                        r.key,
                        r.matches,
                        r.winsA,
                        r.winsB,
                        r.winRateA,
                        r.winRateB
                });
            }

            bastionModel.setRowCount(0);
            for (HeadToHeadByKeyRow r : h2hBast) {
                bastionModel.addRow(new Object[]{
                        r.key,
                        r.matches,
                        r.winsA,
                        r.winsB,
                        r.winRateA,
                        r.winRateB
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error computing comparison:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildPlayerSummary(PlayerSeasonRow basic, PlayerProfileStats stats, Integer seasonEloRank) {
        String avgStr = (stats.avgCompletionTime == null)
                ? "N/A"
                : UiUtils.formatMillis(stats.avgCompletionTime);

        String bestStr = (stats.overallBestTime == null)
                ? "N/A"
                : UiUtils.formatMillis(stats.overallBestTime);

        String winStr = (stats.winRate == null)
                ? "N/A"
                : String.format("%.1f%%", stats.winRate * 100.0);

        String eloRankStr = (seasonEloRank == null) ? "N/A" : seasonEloRank.toString();
        String eloRateStr = (basic.eloRate == 0 && basic.eloRank == 0 && basic.country == null)
                ? "N/A"
                : String.valueOf(basic.eloRate);

        return String.format(
                "<html>%s (%s)<br>" +
                        "Matches (type 2): %d &nbsp;&nbsp; Wins: %d &nbsp;&nbsp; Win%%: %s<br>" +
                        "Avg time (≥ 5 min): %s &nbsp;&nbsp; Best time: %s<br>" +
                        "Season EloRank: %s &nbsp;&nbsp; EloRate: %s</html>",
                basic.nickname,
                basic.uuid,
                stats.totalMatches,
                stats.wins,
                winStr,
                avgStr,
                bestStr,
                eloRankStr,
                eloRateStr
        );
    }
}