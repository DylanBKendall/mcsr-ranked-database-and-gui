package ui;

import db.MatchDao;
import db.MatchDao.MatchRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class MatchesPanel extends JPanel {

    private final JTextField seasonField = new JTextField(4);
    private final JButton loadButton = new JButton("Load Matches");
    private final DefaultTableModel tableModel;

    public MatchesPanel() {
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.add(new JLabel("Season (blank = all):"));
        top.add(seasonField);
        top.add(loadButton);
        add(top, BorderLayout.NORTH);

        String[] cols = {
                "ID", "Type", "Category", "Game Mode", "Season",
                "Date", "Winner UUID", "Winner Nickname",
                "Result Time", "Seed Type", "Bastion Type"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 1, 4, 5, 8 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        table.getColumnModel()
                .getColumn(8)
                .setCellRenderer(new TimeCellRenderer());

        add(new JScrollPane(table), BorderLayout.CENTER);

        loadButton.addActionListener(e -> loadMatches());
    }

    private void loadMatches() {
        tableModel.setRowCount(0);

        String seasonText = seasonField.getText().trim();
        Integer season = null;
        if (!seasonText.isEmpty()) {
            try {
                season = Integer.parseInt(seasonText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Season must be an integer or blank.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            List<MatchRow> rows = new MatchDao().listRecentMatches(season);
            for (MatchRow r : rows) {
                tableModel.addRow(new Object[] {
                        r.id,
                        r.type,
                        r.category,
                        r.gameMode,
                        r.season,
                        r.date,
                        r.resultUuid,
                        r.winnerNickname,
                        r.resultTime,
                        r.seedType,
                        r.bastionType
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading matches:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}