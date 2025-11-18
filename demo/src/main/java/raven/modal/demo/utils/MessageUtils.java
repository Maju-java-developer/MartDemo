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
    public static void showBrandMessage(int result) {
        switch (result) {
            case -1 -> JOptionPane.showMessageDialog(null, "Brand updated successfully!");
            case -2 -> JOptionPane.showMessageDialog(null, "Brand deleted successfully!");
            case -3 -> JOptionPane.showMessageDialog(null, "Brand already exists!", "Duplicate", JOptionPane.WARNING_MESSAGE);
            case -4 -> JOptionPane.showMessageDialog(null, "Brand not found!", "Error", JOptionPane.ERROR_MESSAGE);
            default -> {
                if (result > 0)
                    JOptionPane.showMessageDialog(null, "Brand added successfully!");
                else
                    JOptionPane.showMessageDialog(null, "Operation failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void showCategoryMessage(int result) {
        switch (result) {
            case -1 -> JOptionPane.showMessageDialog(null, "Category updated successfully!");
            case -2 -> JOptionPane.showMessageDialog(null, "Category deleted successfully!");
            case -3 -> JOptionPane.showMessageDialog(null, "Category already exists!", "Duplicate", JOptionPane.WARNING_MESSAGE);
            case -4 -> JOptionPane.showMessageDialog(null, "Category not found!", "Error", JOptionPane.ERROR_MESSAGE);
            default -> {
                if (result > 0)
                    JOptionPane.showMessageDialog(null, "Category added successfully!");
                else
                    JOptionPane.showMessageDialog(null, "Operation failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public static void showPackingTypeMessage(int result) {
        switch (result) {
            case -1 -> JOptionPane.showMessageDialog(null, "Packing Type updated successfully!");
            case -2 -> JOptionPane.showMessageDialog(null, "Packing Type deleted successfully!");
            case -3 -> JOptionPane.showMessageDialog(null, "Packing Type already exists!", "Duplicate", JOptionPane.WARNING_MESSAGE);
            case -4 -> JOptionPane.showMessageDialog(null, "Packing Type not found!", "Error", JOptionPane.ERROR_MESSAGE);
            default -> {
                if (result > 0)
                    JOptionPane.showMessageDialog(null, "Packing Type added successfully!");
                else
                    JOptionPane.showMessageDialog(null, "Operation failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
