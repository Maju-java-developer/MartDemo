package raven.modal.demo.utils.combox;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;

public class JComponentUtils {
    public static void setNumberOnly(JTextField textField) {
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string.matches("\\d+")) { // only digits allowed
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    public static void resetTextField(JTextField field, String value) {
        AbstractDocument doc = (AbstractDocument) field.getDocument();
        DocumentFilter oldFilter = doc.getDocumentFilter();
        doc.setDocumentFilter(null);
        field.setText(value);
        doc.setDocumentFilter(oldFilter);
    }

    /**
     * Displays a Form component inside a modal JDialog, centered on its parent window.
     * * @param parent The parent Window (usually obtained via SwingUtilities.getWindowAncestor(this)).
     * @param form The Form instance to display.
     * @param title The title for the modal dialog.
     */
    public static void showModal(Window parent, JComponent form, String title) {
        // Find the JFrame or JDialog that is the top-level parent
        Window owner = (parent instanceof Frame || parent instanceof Dialog) ? parent : null;

        JDialog dialog;
        if (owner instanceof Frame) {
            dialog = new JDialog((Frame) owner, title, true);
        } else if (owner instanceof Dialog) {
            dialog = new JDialog((Dialog) owner, title, true);
        } else {
            // Fallback for when parent is null or not a top-level window
            dialog = new JDialog((Frame) null, title, true);
        }

        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(owner); // Center relative to the owner
        dialog.setVisible(true);
    }
}
