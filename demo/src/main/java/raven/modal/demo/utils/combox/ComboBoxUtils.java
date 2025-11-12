package raven.modal.demo.utils.combox;

import javax.swing.*;
import java.awt.*;

public class ComboBoxUtils {
    public static <T> void setupComboBoxRenderer(JComboBox<T> comboBox, java.util.function.Function<Object, String> nameExtractor) {
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value != null) {
                    value = nameExtractor.apply(value);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    // Helper method to set selection by ID for Model objects
    public static <T> T setComboBoxSelection(JComboBox<T> cmb, int targetId, java.util.function.Function<T, Integer> idExtractor) {
        ComboBoxModel<T> model = cmb.getModel();
        T selectedItem = null;
        for (int i = 0; i < model.getSize(); i++) {
            T item = model.getElementAt(i);
            if (idExtractor.apply(item).equals(targetId)) {
                cmb.setSelectedItem(item);
                selectedItem = item;
                break;
            }
        }
        return selectedItem;
    }

}
