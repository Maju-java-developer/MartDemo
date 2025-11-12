package raven.modal.demo;

public class Constants {
    public final static String[] supplierColumns = {"#", "Vendor Name", "Contact No", "Email", "Address", "Remaining Balance", "Actions"};
    public final static String[] peckingTypeColumns = {"#", "PeckingTitle", "Quarter Qty", "Status", "Actions"};
    public final static String[] companyColumns = {"#", "Company Name", "Status", "Actions"};
    public final static String[] productColumns = {"#", "ProductName", "Code", "Company","Brand","Category","Pecking Type", "Actions"};
    public final static String[] customerColumns = {"#", "Customer Name", "Contact No", "Email", "Address", "Remaining Balance", "Tax", "Actions"};
    public final static String[] purchaseColumns = {"#", "Vendor", "Date", "Total Amount", "Paid Amount", "Remaining Balance", "Actions"};
    public final static String[] categoryColumns = {"#", "Category Name", "Status", "Action"};
    public final static String[] brandsColumns = {"#", "Brand Title", "Company Name", "Status", "Action"};

    public static final String DISCOUNT_TYPE_PERCENTAGE = "Percentage (%)";
    public static final String DISCOUNT_TYPE_FIXED = "Fixed Amount";

    public static final String[] DISCOUNT_TYPES = {
            DISCOUNT_TYPE_PERCENTAGE,
            DISCOUNT_TYPE_FIXED
    };
    public static final double DEFAULT_GST_RATE = 0.0;

}
