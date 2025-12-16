package ui;

import db.PlayerDao;
import db.PlayerDao.PlayerSeasonRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class PlayersPanel extends JPanel {

    private final JTextField searchField = new JTextField(20);
    private final JButton searchButton = new JButton("Search");
    private final JButton profileButton = new JButton("View Profile");

    private final DefaultTableModel tableModel;
    private final JTable table;

    public PlayersPanel() {
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.add(new JLabel("Nickname contains:"));
        top.add(searchField);
        top.add(searchButton);
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

        table.getColumnModel()
                .getColumn(7)
                .setCellRenderer(new TimeCellRenderer());

        add(new JScrollPane(table), BorderLayout.CENTER);

        profileButton.setEnabled(false);

        searchButton.addActionListener(e -> doSearch());
        profileButton.addActionListener(e -> openProfile());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                profileButton.setEnabled(table.getSelectedRow() >= 0);
            }
        });
    }

    private void doSearch() {
        tableModel.setRowCount(0);
        String term = searchField.getText().trim();
        if (term.isEmpty()) return;

        try {
            List<PlayerSeasonRow> rows = new PlayerDao().findPlayerByNickname(term);
            for (PlayerSeasonRow r : rows) {
                tableModel.addRow(new Object[] {
                        r.uuid,
                        r.nickname,
                        r.country,
                        r.eloRate,
                        r.eloRank,
                        r.wins,
                        r.loses,
                        r.bestTime
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error querying database:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openProfile() {
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