package raven.modal.demo.utils;

import raven.modal.demo.model.UserModel;

import javax.swing.*;

/**
 * Application-wide constants and utility methods.
 * <p>
 * This class contains global constants used throughout the application,
 * including
 * table column definitions, discount types, default settings, and the current
 * user session.
 * </p>
 */
public class Constants {

    /**
     * The currently logged-in user.
     * <p>
     * This field holds the reference to the {@link UserModel} representing the user
     * currently authenticated in the application. It is null if no user is logged
     * in.
     * </p>
     */
    public static UserModel currentUser = null;

    /**
     * Retrieves the ID of the currently logged-in user.
     * <p>
     * If no user is logged in (i.e., {@link #currentUser} is null), this method
     * displays an error message dialog and throws a NullPointerException
     * (implicitly).
     * </p>
     *
     * @return the ID of the current user.
     */
    public static int getCurrentUserId() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(null, "User not logged in", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return currentUser.getUserId();
    }

    /**
     * Column names for the Supplier table.
     */
    public final static String[] supplierColumns = { "#", "Vendor Name", "Contact No", "Email", "Address",
            "Remaining Balance", "Actions" };

    /**
     * Column names for the Pecking Type table.
     */
    public final static String[] peckingTypeColumns = { "#", "PeckingTitle", "Quarter Qty", "Status", "Actions" };

    /**
     * Column names for the Company table.
     */
    public final static String[] companyColumns = { "#", "Company Name", "Status", "Actions" };

    /**
     * Column names for the Product table.
     */
    public final static String[] productColumns = { "#", "ProductName", "Code", "Company", "Brand", "Category",
            "Pecking Type", "Actions" };

    /**
     * Column names for the Customer table.
     */
    public final static String[] customerColumns = { "#", "Customer Name", "Contact No", "Email", "Address",
            "Remaining Balance", "Tax", "Actions" };

    /**
     * Column names for the Purchase table.
     */
    public final static String[] purchaseColumns = { "#", "Vendor", "Date", "Total Amount", "Paid Amount",
            "Remaining Balance", "Actions" };

    /**
     * Column names for the Category table.
     */
    public final static String[] categoryColumns = { "#", "Category Name", "Status", "Action" };

    /**
     * Column names for the Brands table.
     */
    public final static String[] brandsColumns = { "#", "Brand Title", "Company Name", "Status", "Action" };

    /**
     * Constant for Percentage discount type.
     */
    public static final String DISCOUNT_TYPE_PERCENTAGE = "Percentage (%)";

    /**
     * Constant for Fixed Amount discount type.
     */
    public static final String DISCOUNT_TYPE_FIXED = "Fixed Amount";

    /**
     * Array of available discount types.
     */
    public static final String[] DISCOUNT_TYPES = {
            DISCOUNT_TYPE_PERCENTAGE,
            DISCOUNT_TYPE_FIXED
    };

    /**
     * Default Goods and Services Tax (GST) rate.
     */
    public static final double DEFAULT_GST_RATE = 4.0;

    /**
     * Default limit for the number of items per page in pagination.
     */
    public static final int LIMIT_PER_PAGE = 10;

}
