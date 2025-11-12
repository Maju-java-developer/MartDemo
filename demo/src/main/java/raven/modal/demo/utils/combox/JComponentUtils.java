package raven.modal.demo.utils.combox;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

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
                if (text.matches("\\d*")) { // allow replace with empty string too
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

}
