package raven.modal.demo.utils;

import javax.swing.*;

public class MessageUtils{
    public static void showCompanyMessageResult(int result) {
        switch (result) {
            case -1 -> JOptionPane.showMessageDialog(null,
                    "Company updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            case -2 -> JOptionPane.showMessageDialog(null,
                    "Company deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            case -3 -> JOptionPane.showMessageDialog(null,
                    "Company name already exists!", "Duplicate", JOptionPane.WARNING_MESSAGE);

            case -4 -> JOptionPane.showMessageDialog(null,
                    "Company not found!", "Error", JOptionPane.ERROR_MESSAGE);
            default -> {
                if (result > 0) {
                    JOptionPane.showMessageDialog(null,
                            "Company added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Operation failed!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

}
