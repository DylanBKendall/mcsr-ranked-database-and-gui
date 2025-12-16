package ui;

import db.PlayerDao;
import db.PlayerDao.PlayerSeasonRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class FavoritesPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;

    public FavoritesPanel() {
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        JButton refreshButton = new JButton("Refresh");
        JButton profileButton = new JButton("Open Profile");
        profileButton.setEnabled(false);

        top.add(refreshButton);
        top.add(profileButton);
        add(top, BorderLayout.NORTH);

        String[] cols = {
                "UUID", "Nickname", "Country",
                "Elo Rate", "Elo Rank",
                "Wins (S9)", "Loses (S9)", "Best Time S9 (ms)"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 3, 4, 5, 6, 7 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(tableModel));
        table.setRowHeight(24);
        table.getColumnModel().getColumn(7).setCellRenderer(new TimeCellRenderer());

        add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                profileButton.setEnabled(table.getSelectedRow() >= 0);
            }
        });

        refreshButton.addActionListener(e -> loadFavorites());
        profileButton.addActionListener(e -> openSelectedProfile());

        loadFavorites();
    }

    private void loadFavorites() {
        tableModel.setRowCount(0);

        List<String> favUuids = FavoritesManager.getFavorites();
        if (favUuids.isEmpty()) return;

        PlayerDao dao = new PlayerDao();

        for (String uuid : favUuids) {
            try {
                PlayerSeasonRow r = dao.getPlayerSummaryByUuid(uuid);
                if (r == null) continue;

                tableModel.addRow(new Object[]{
                        r.uuid,
                        r.nickname,
                        r.country,
                        r.eloRate,
                        r.eloRank,
                        r.wins,
                        r.loses,
                        r.bestTime
                });
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error loading favorite " + uuid + ":\n" + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openSelectedProfile() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = table.convertRowIndexToModel(viewRow);

        String uuid = (String) tableModel.getValueAt(modelRow, 0);
        String nickname = (String) tableModel.getValueAt(modelRow, 1);
        String country = (String) tableModel.getValueAt(modelRow, 2);

        Window owner = SwingUtilities.getWindowAncestor(this);
        PlayerProfileDialog dlg = new PlayerProfileDialog(owner, uuid, nickname, country);
        dlg.setVisible(true);
    }
}