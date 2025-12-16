// ui/PercentCellRenderer.java
package ui;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JTable;
import java.awt.Component;

public class PercentCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        if (value instanceof Number n) {
            value = String.format("%.1f%%", n.doubleValue() * 100.0);
        }
        return super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
    }
}
