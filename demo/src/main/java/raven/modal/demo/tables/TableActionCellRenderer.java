package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class TableActionCellRenderer extends JPanel implements TableCellRenderer {

    private final ActionItem[] actions;

    // Pass the dynamic actions array to the constructor
    public TableActionCellRenderer(ActionItem[] actions) {
        this.actions = actions;
        setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
        setBackground(null);

        // --- Dynamically Create Buttons ---
        for (ActionItem action : actions) {
            JButton button = new JButton(action.getText(), action.getIcon());
            button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
            button.setFocusable(false);
            add(button);
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        // You can add logic here to change background/foreground if isSelected is true
        // but for a JPanel with transparent background, this is usually sufficient.
        return this;
    }
}