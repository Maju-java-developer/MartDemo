package raven.modal.demo.tables;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class TableActionCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JPanel panel;
    private final ActionItem[] actions;
    private int selectedRow;
    private JTable table;

    // Pass the dynamic actions array to the constructor
    public TableActionCellEditor(JTable table, ActionItem[] actions) {
        this.table = table;
        this.actions = actions;
        this.panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        // --- Dynamically Create and Configure Buttons ---
        for (ActionItem actionItem : actions) {
            JButton button = new JButton(actionItem.getText(), actionItem.getIcon());
            button.putClientProperty("JButton.buttonType", "roundRect");

            // Add the action listener
            button.addActionListener(e -> {
                // Stop editing first to ensure the cell update is complete
                fireEditingStopped();

                // Execute the action logic with the table and the row index
                actionItem.getAction().accept(this.table, selectedRow);
            });

            panel.add(button);
        }
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        this.selectedRow = row; // Store the current row index
        return panel;
    }
}