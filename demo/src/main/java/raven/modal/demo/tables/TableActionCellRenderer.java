package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class TableActionCellRenderer extends JPanel implements TableCellRenderer {

    // Pass the dynamic actions array to the constructor
    public TableActionCellRenderer(ActionItem[] actions) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
        setBackground(null);
        setOpaque(true);

        // --- Dynamically Create Buttons ---
        for (ActionItem action : actions) {
            JButton button = new JButton(action.getText(), action.getIcon());
            if (action.getIcon() == null) {
                button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
            } else {
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setContentAreaFilled(false);
                button.setFocusPainted(false);
                button.setBorderPainted(false);
                button.setOpaque(false);
            }

            button.setFocusable(false);
            button.setOpaque(false); // Button stays flat
            add(button);
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        if (isSelected) {
            setBackground(table.getSelectionBackground());
        } else {
            setBackground(table.getBackground());
        }
        return this;
    }
}