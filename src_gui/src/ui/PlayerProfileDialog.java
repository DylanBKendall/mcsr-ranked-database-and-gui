package ui;

import db.PlayerDao;
import db.PlayerDao.PlayerProfileStats;
import db.PlayerDao.AvgByKey;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class PlayerProfileDialog extends JDialog {

    private final String uuid;
    private final String nickname;
    private final String country;

    private final JLabel headerLabel = new JLabel();
    private final JLabel overallLabel = new JLabel();

    private final JComboBox<Object> seasonBox = new JComboBox<>();

    private final DefaultTableModel overworldModel;
    private final DefaultTableModel netherModel;

    private final DefaultTableModel seasonModel;
    private final JTable seasonTable;
    private final JButton favoriteButton = new JButton();

    public PlayerProfileDialog(Window owner, String uuid, String nickname, String country) {
        super(owner, "Player Profile - " + nickname, ModalityType.APPLICATION_MODAL);
        this.uuid = uuid;
        this.nickname = nickname;
        this.country = country;

        setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        headerLabel.setText(buildHeaderText("All time"));
        top.add(headerLabel, BorderLayout.WEST);

        JPanel seasonPanel = new JPanel();
        seasonPanel.add(new JLabel("Season:"));
        seasonPanel.add(seasonBox);
        seasonPanel.add(favoriteButton);
        top.add(seasonPanel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        updateFavoriteButtonText();
        favoriteButton.addActionListener(e -> toggleFavorite());

        seasonModel = new DefaultTableModel(
                new Object[]{"Season", "Placement", "End Elo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return Integer.class;
            }
        };

        seasonTable = new JTable(seasonModel);
        seasonTable.setAutoCreateRowSorter(true);
        seasonTable.setRowSorter(new TableRowSorter<>(seasonModel));
        seasonTable.setRowHeight(24);
        seasonTable.setPreferredScrollableViewportSize(new Dimension(600, 140));

        overworldModel = new DefaultTableModel(
                new Object[]{"Overworld Type", "Avg Time", "# Matches", "Win %"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 1 -> Double.class;
                    case 2 -> Integer.class;
                    case 3 -> Double.class;
                    default -> String.class;
                };
            }
        };
        JTable owTable = new JTable(overworldModel);
        owTable.setAutoCreateRowSorter(true);
        owTable.setRowSorter(new TableRowSorter<>(overworldModel));
        owTable.setRowHeight(24);
        owTable.getColumnModel().getColumn(1).setCellRenderer(new TimeCellRenderer());
        owTable.getColumnModel().getColumn(3).setCellRenderer(new PercentCellRenderer());

        netherModel = new DefaultTableModel(
                new Object[]{"Nether Type", "Avg Time", "# Matches", "Win %"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 1 -> Double.class;
                    case 2 -> Integer.class;
                    case 3 -> Double.class;
                    default -> String.class;
                };
            }
        };
        JTable nTable = new JTable(netherModel);
        nTable.setAutoCreateRowSorter(true);
        nTable.setRowSorter(new TableRowSorter<>(netherModel));
        nTable.setRowHeight(24);
        nTable.getColumnModel().getColumn(1).setCellRenderer(new TimeCellRenderer());
        nTable.getColumnModel().getColumn(3).setCellRenderer(new PercentCellRenderer());

        JSplitPane horizSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(owTable),
                new JScrollPane(nTable)
        );
        horizSplit.setResizeWeight(0.5);

        JSplitPane vertSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                horizSplit,
                new JScrollPane(seasonTable)
        );
        vertSplit.setResizeWeight(0.7);

        add(vertSplit, BorderLayout.CENTER);

        overallLabel.setVerticalAlignment(SwingConstants.TOP);
        add(overallLabel, BorderLayout.SOUTH);

        seasonBox.addActionListener(e -> reloadStats());

        loadSeasons();
        reloadStats();

        pack();
        setSize(900, 600);
        setLocationRelativeTo(owner);
    }

    private void updateFavoriteButtonText() {
        boolean fav = FavoritesManager.isFavorite(uuid);
        favoriteButton.setText(fav ? "★ Unfavorite" : "☆ Favorite");
    }

    private void toggleFavorite() {
        if (FavoritesManager.isFavorite(uuid)) {
            FavoritesManager.removeFavorite(uuid);
        } else {
            FavoritesManager.addFavorite(uuid);
        }
        updateFavoriteButtonText();
    }

    private String buildHeaderText(String seasonLabel) {
        String countryPart = (country == null || country.isBlank()) ? "" : " [" + country + "]";
        return "Player: " + nickname + countryPart + " (" + uuid + ") — " + seasonLabel;
    }

    private void loadSeasons() {
        seasonBox.removeAllItems();
        seasonBox.addItem("All time");

        try {
            PlayerDao dao = new PlayerDao();
            List<Integer> seasons = dao.getSeasonsForPlayer(uuid);
            for (Integer s : seasons) {
                seasonBox.addItem(s);
            }

            boolean has9 = false;
            for (int i = 0; i < seasonBox.getItemCount(); i++) {
                Object item = seasonBox.getItemAt(i);
                if (item instanceof Integer && ((Integer) item) == 9) {
                    has9 = true;
                    break;
                }
            }
            if (has9) {
                seasonBox.setSelectedItem(9);
            } else {
                seasonBox.setSelectedIndex(0);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading seasons for player:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            seasonBox.setSelectedIndex(0);
        }
    }

    private void reloadStats() {
        Object sel = seasonBox.getSelectedItem();
        Integer seasonFilter = (sel instanceof Integer) ? (Integer) sel : null;

        String seasonLabel = (seasonFilter == null) ? "All time"
                : ("Season " + seasonFilter);
        headerLabel.setText(buildHeaderText(seasonLabel));

        PlayerProfileStats stats = null;

        try {
            PlayerDao dao = new PlayerDao();
            stats = dao.getPlayerProfileStats(uuid, seasonFilter);

            String avgTimeStr = (stats.avgCompletionTime == null)
                    ? "N/A"
                    : UiUtils.formatMillis(stats.avgCompletionTime);

            String bestTimeStr = (stats.overallBestTime == null)
                    ? "N/A"
                    : UiUtils.formatMillis(stats.overallBestTime);

            String winRateStr = (stats.winRate == null)
                    ? "N/A"
                    : String.format("%.1f%%", stats.winRate * 100.0);

            String overallHtml = "<html>" +
                    "Total matches: " + stats.totalMatches +
                    " &nbsp;&nbsp; Wins: " + stats.wins +
                    " &nbsp;&nbsp; Win rate: " + winRateStr +
                    "<br>Average completion time (≥ 5 min): " + avgTimeStr +
                    " &nbsp;&nbsp; Best time (overall): " + bestTimeStr +
                    "</html>";

            overallLabel.setText(overallHtml);

            overworldModel.setRowCount(0);
            for (AvgByKey a : stats.overworldAverages) {
                overworldModel.addRow(new Object[]{
                        a.key,
                        a.avgTime,
                        a.matches,
                        a.winRate
                });
            }

            netherModel.setRowCount(0);
            for (AvgByKey a : stats.netherAverages) {
                netherModel.addRow(new Object[]{
                        a.key,
                        a.avgTime,
                        a.matches,
                        a.winRate
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading stats for player:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        seasonModel.setRowCount(0);
        if (stats != null && stats.seasonResults != null) {
            for (PlayerDao.SeasonResultRow r : stats.seasonResults) {
                seasonModel.addRow(new Object[]{
                        r.season,
                        r.rank,
                        r.endElo
                });
            }
        }
    }
}