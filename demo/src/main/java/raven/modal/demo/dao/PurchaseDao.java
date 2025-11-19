package raven.modal.demo.dao;

import raven.modal.demo.model.PurchaseDetailModel;
import raven.modal.demo.model.PurchaseModel;
import raven.modal.demo.mysql.MySQLConnection;
import raven.modal.demo.utils.Constants;

import javax.swing.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDao {

    /**
     * Fetches purchase header and all associated details for editing.
     */
    public PurchaseModel getPurchaseForEdit(int purchaseId) {
        // 1. Fetch Header Data
        String sqlHeader = "SELECT p.*, s.SupplierName " +
                "FROM TBLPurchase p " +
                "JOIN TBLSuppliers s ON p.SupplierID = s.SupplierID " +
                "WHERE p.PurchaseID = ?";

        PurchaseModel purchase = null;

        try (Connection conn = MySQLConnection.getInstance().getConnection()) {

            try (PreparedStatement psHeader = conn.prepareStatement(sqlHeader)) {
                psHeader.setInt(1, purchaseId);
                try (ResultSet rs = psHeader.executeQuery()) {
                    if (rs.next()) {
                        purchase = PurchaseModel.builder()
                                .purchaseID(rs.getInt("PurchaseID"))
                                .supplierID(rs.getInt("SupplierID"))
                                .supplierName(rs.getString("SupplierName"))
                                .purchaseDate(rs.getTimestamp("PurchaseDate").toLocalDateTime())
                                .invoiceNo(rs.getString("InvoiceNo"))
//                                .actualAmount(rs.getDouble("ActualAmount"))
//                                .discountType(rs.getString("DiscountType"))
//                                .discountValue(rs.getDouble("DiscountValue"))
                                .totalAmount(rs.getDouble("TotalAmount"))
                                .paidAmount(rs.getDouble("PaidAmount"))
                                .remarks(rs.getString("Remarks"))
                                .details(new ArrayList<>()) // Initialize list for details
                                .build();
                    } else {
                        return null;
                    }
                }
            }

            // 2. Fetch Detail Data (Line Items)
            String sqlDetails = "SELECT pd.*, p.ProductName, pt.cartonQty as UnitPerCarton " +
                    "FROM TBLPurchaseDetail pd " +
                    "JOIN TBLProducts p ON pd.ProductID = p.ProductID " +
                    "JOIN TBLPackingType pt ON p.packingTypeId = pt.packingTypeId " +
                    "WHERE pd.PurchaseID = ?";

            try (PreparedStatement psDetails = conn.prepareStatement(sqlDetails)) {
                psDetails.setInt(1, purchaseId);
                try (ResultSet rs = psDetails.executeQuery()) {
                    while (rs.next()) {
                        PurchaseDetailModel detail = PurchaseDetailModel.builder()
                                .purchaseDetailID(rs.getInt("PurchaseDetailID"))
                                .productID(rs.getInt("ProductID"))
                                .productName(rs.getString("ProductName")) // To display in table
                                .unitsPerCarton(rs.getInt("UnitPerCarton")) // Crucial for splitting
                                .quantity(rs.getDouble("Quantity"))
                                .rate(rs.getDouble("Rate"))
                                .total(rs.getDouble("Total"))
                                .build();
                        purchase.getDetails().add(detail);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error fetching purchase for edit: " + e.getMessage());
            return null;
        }
        return purchase;
    }

    /**
     * Serializes a list of PurchaseDetailModel objects into a JSON array string.
     * NOTE: For production, use a proper library like Jackson or Gson for safety.
     */
    private String serializeDetailsToJson(List<PurchaseDetailModel> details) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        for (int i = 0; i < details.size(); i++) {
            PurchaseDetailModel detail = details.get(i);

            jsonBuilder.append("{");
            // Important: Use keys exactly matching the JSON path in the SP
            jsonBuilder.append("\"productID\":").append(detail.getProductID()).append(",");
            jsonBuilder.append("\"quantity\":").append(detail.getQuantity()).append(",");
            jsonBuilder.append("\"rate\":").append(detail.getRate()).append(",");
            jsonBuilder.append("\"total\":").append(detail.getTotal());
            jsonBuilder.append("}");

            if (i < details.size() - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    public boolean handlePurchaseCRUD(PurchaseModel purchaseModel, String status) {

        String sql = "{CALL SP_IUD_Purchase(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        Connection conn = null;

        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            CallableStatement cs = conn.prepareCall(sql);

            boolean isSave = status.equalsIgnoreCase("Save");
            boolean isUpdate = status.equalsIgnoreCase("Update");
            boolean isDelete = status.equalsIgnoreCase("Delete");

            // Convert details → JSON (Only Save/Update)
            String detailsJson = (!isDelete) ? serializeDetailsToJson(purchaseModel.getDetails()) : null;


            // ------------------------------
            // 1. PurchaseID (INOUT)
            // ------------------------------
            if (isSave) {
                cs.setNull(1, Types.INTEGER);
            } else {
                cs.setInt(1, purchaseModel.getPurchaseID());
            }
            cs.registerOutParameter(1, Types.INTEGER);


            // ------------------------------
            // 2. SupplierID
            // ------------------------------
            cs.setInt(2, purchaseModel.getSupplierID());


            // ------------------------------
            // 3. PurchaseDate
            // ------------------------------
            cs.setTimestamp(3, Timestamp.valueOf(purchaseModel.getPurchaseDate()));


            // ------------------------------
            // 4. InvoiceNo (INOUT)
            // ------------------------------
            if (isSave) {
                cs.setNull(4, Types.VARCHAR);
            } else {
                cs.setString(4, purchaseModel.getInvoiceNo());
            }
            cs.registerOutParameter(4, Types.VARCHAR);


            // ------------------------------
            // 5–10 Header Fields
            // ------------------------------
            cs.setDouble(5, purchaseModel.getActualAmount());
            cs.setString(6, purchaseModel.getDiscountType());
            cs.setDouble(7, purchaseModel.getDiscountValue());
            cs.setDouble(8, purchaseModel.getTotalAmount());
            cs.setDouble(9, purchaseModel.getPaidAmount());
            cs.setString(10, purchaseModel.getRemarks());


            // 11: Current timestamp
            cs.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));

            // 12: User ID
            cs.setInt(12, Constants.getCurrentUserId());

            // 13: Status (Save/Update/Delete)
            cs.setString(13, status);

            // 14: JSON (NULL for Delete)
            if (!isDelete) {
                cs.setString(14, detailsJson);
            } else {
                cs.setNull(14, Types.VARCHAR);
            }

            // 15: OUT return code
            cs.registerOutParameter(15, Types.INTEGER);


            // 🟢 Execute
            cs.executeUpdate();


            // Read OUT parameters
            int purchaseID = cs.getInt(1);
            String invoiceNo = cs.getString(4);
            int result = cs.getInt(15);


            // ------------------------------
            // 🧩 Interpret result codes
            // ------------------------------

            if (result <= 0) {
                conn.rollback();
//                JOptionPane.showMessageDialog(null,
//                        "Failed! Code: " + result,
//                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            conn.commit();


            // SUCCESS MESSAGES
            switch (result) {

                case -1:
                    JOptionPane.showMessageDialog(null,
                            "Purchase Updated Successfully!",
                            "Updated", JOptionPane.INFORMATION_MESSAGE);
                    break;

                case -2:
                    JOptionPane.showMessageDialog(null,
                            "Purchase Deleted Successfully!",
                            "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    break;

                default:
                    JOptionPane.showMessageDialog(null,
                            "Purchase Saved!\nID: " + purchaseID + "\nInvoice: " + invoiceNo,
                            "Saved", JOptionPane.INFORMATION_MESSAGE);
            }

            return true;


        } catch (Exception e) {

            try { if (conn != null) conn.rollback(); } catch (Exception ignore) {}

            JOptionPane.showMessageDialog(null,
                    "Database Error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;

        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (Exception ignore) {}
        }
    }

    /**
     * Fetches a paginated list of Purchase header records, including the Supplier name.
     * @param offset Starting point of the records.
     * @param limit Maximum number of records to return.
     * @return List of PurchaseModel.
     */
    public List<PurchaseModel> getPurchases(int offset, int limit) {
        // SQL to join TBLPurchase with TBLSuppliers to get the SupplierName
        String sql = "SELECT p.PurchaseID, p.SupplierID, p.PurchaseDate, p.InvoiceNo, p.TotalAmount," +
//                " p.DiscountType, p.DiscountValue," +
                " p.TotalAmount, p.PaidAmount, s.SupplierName " +
                "FROM TBLPurchase p " +
                "JOIN TBLSuppliers s ON p.SupplierID = s.SupplierID " +
                "ORDER BY p.PurchaseDate DESC LIMIT ? OFFSET ?";

        List<PurchaseModel> purchases = new ArrayList<>();

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    purchases.add(PurchaseModel.builder()
                            .purchaseID(rs.getInt("PurchaseID"))
                            .supplierID(rs.getInt("SupplierID"))
                            .purchaseDate(rs.getTimestamp("PurchaseDate").toLocalDateTime())
                            .invoiceNo(rs.getString("InvoiceNo"))
//                            .discountType(rs.getString("DiscountType"))
//                            .discountValue(rs.getDouble("DiscountValue"))
                            .totalAmount(rs.getDouble("TotalAmount"))
                            .paidAmount(rs.getDouble("PaidAmount"))
                            .supplierName(rs.getString("SupplierName")) // Set the joined field
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching purchases: " + e.getMessage());
        }
        return purchases;
    }

    /**
     * Gets the total number of records in TBLPurchase for pagination.
     * @return The total count.
     */
    public int getPurchaseCount() {
        String sql = "SELECT COUNT(*) FROM TBLPurchase";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching purchase count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Fetches a single Purchase header and its details by ID.
     * @param purchaseId The ID of the purchase to fetch.
     * @return The PurchaseModel with details, or null if not found.
     */
    public PurchaseModel getPurchaseById(int purchaseId) {
        // 1. Fetch Header Details
        String sqlHeader = "SELECT PurchaseID, SupplierID, PurchaseDate, InvoiceNo, TotalAmount, PaidAmount, Remarks, CreatedDate " +
                "FROM TBLPurchase WHERE PurchaseID = ?";

        PurchaseModel purchase = null;

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement psHeader = conn.prepareStatement(sqlHeader)) {

            psHeader.setInt(1, purchaseId);
            try (ResultSet rsHeader = psHeader.executeQuery()) {
                if (rsHeader.next()) {
                    purchase = PurchaseModel.builder()
                            .purchaseID(rsHeader.getInt("PurchaseID"))
                            .supplierID(rsHeader.getInt("SupplierID"))
                            .purchaseDate(rsHeader.getTimestamp("PurchaseDate").toLocalDateTime())
                            .invoiceNo(rsHeader.getString("InvoiceNo"))
                            .totalAmount(rsHeader.getDouble("TotalAmount"))
                            .paidAmount(rsHeader.getDouble("PaidAmount"))
                            .remarks(rsHeader.getString("Remarks"))
                            .createdDate(rsHeader.getTimestamp("CreatedDate").toLocalDateTime())
                            // Fetch SupplierName here if needed, or in the form using SupplierDao
                            .build();

                    // 2. Fetch Purchase Detail Lines
                    List<PurchaseDetailModel> details = getPurchaseDetails(conn, purchaseId);
                    purchase.setDetails(details);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching purchase by ID: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Error fetching purchase details.", "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return purchase;
    }

    /**
     * Helper method to fetch purchase detail lines for a given Purchase ID.
     * @param conn The active database connection.
     * @param purchaseId The ID of the purchase header.
     * @return List of PurchaseDetailModel.
     */
    private List<PurchaseDetailModel> getPurchaseDetails(Connection conn, int purchaseId) throws SQLException {
        // NOTE: Assuming TBLProducts has ProductName.
        String sqlDetail = "SELECT pd.PurchaseDetailID, pd.ProductID, pd.Quantity, pd.Rate, pd.Total, p.ProductName " +
                "FROM TBLPurchaseDetail pd " +
                "JOIN TBLProducts p ON pd.ProductID = p.ProductID " +
                "WHERE pd.PurchaseID = ?";

        List<PurchaseDetailModel> details = new ArrayList<>();

        try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail)) {
            psDetail.setInt(1, purchaseId);

            try (ResultSet rsDetail = psDetail.executeQuery()) {
                while (rsDetail.next()) {
                    details.add(PurchaseDetailModel.builder()
                            .purchaseDetailID(rsDetail.getInt("PurchaseDetailID"))
                            .productID(rsDetail.getInt("ProductID"))
                            .quantity(rsDetail.getDouble("Quantity"))
                            .rate(rsDetail.getDouble("Rate"))
                            .total(rsDetail.getDouble("Total"))
                            .productName(rsDetail.getString("ProductName"))
                            .purchaseID(purchaseId) // Set parent ID
                            .build());
                }
            }
        }
        return details;
    }

    // Inside PurchaseDao.java

    /**
     * Records a payment against a specific purchase and updates the supplier's balance.
     * This operation is executed as a transaction to ensure data consistency across TBLPurchase and TBLSuppliers.
     * * @param purchaseId The ID of the purchase receiving the payment.
     * @param paymentAmount The amount paid.
     * @return true if the transaction was successful.
     */
    public boolean updateSupplierPayment(int purchaseId, double paymentAmount) {
        Connection conn = null;

        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // --- 1. Update TBLPurchase: Increase PaidAmount ---
            String sqlPurchase = "UPDATE TBLPurchase SET PaidAmount = PaidAmount + ? WHERE PurchaseID = ?";
            try (PreparedStatement psPurchase = conn.prepareStatement(sqlPurchase)) {
                psPurchase.setDouble(1, paymentAmount);
                psPurchase.setInt(2, purchaseId);

                if (psPurchase.executeUpdate() == 0) {
                    // This means the PurchaseID was not found
                    throw new SQLException("Failed to update purchase paid amount. Purchase ID not found.");
                }
            }

            // --- 2. Look up SupplierID from TBLPurchase ---
            String sqlLookup = "SELECT SupplierID FROM TBLPurchase WHERE PurchaseID = ?";
            int supplierId;
            try (PreparedStatement psLookup = conn.prepareStatement(sqlLookup)) {
                psLookup.setInt(1, purchaseId);
                ResultSet rs = psLookup.executeQuery();
                if (!rs.next()) {
                    // Should not happen if step 1 succeeded, but good practice to check
                    throw new SQLException("Supplier ID not found for the given Purchase ID.");
                }
                supplierId = rs.getInt("SupplierID");
            }

            // --- 3. Update Supplier OutstandingBalance: Payment REDUCES Liability ---
            // Note: The payment REDUCES the outstanding balance/debt.
            String sqlBalance = "UPDATE TBLSuppliers SET OpeningBalance = OpeningBalance - ? WHERE SupplierID = ?";
            try (PreparedStatement psBalance = conn.prepareStatement(sqlBalance)) {
                psBalance.setDouble(1, paymentAmount);
                psBalance.setInt(2, supplierId);

                if (psBalance.executeUpdate() == 0) {
                    // This means the SupplierID was not found
                    throw new SQLException("Failed to update supplier ledger balance. Supplier ID not found.");
                }
            }

            // --- 4. Commit Transaction ---
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Payment transaction failed. Rolling back: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Rollback failed: " + ex.getMessage());
            }
            JOptionPane.showMessageDialog(null, "Payment record failed due to a database error. Details: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Restore default
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    // Inside PurchaseDao.java

    /**
     * Updates an existing purchase transaction, managing ledger reversal and detail replacement.
     * @param purchaseModel The PurchaseModel containing the updated header and new details.
     * @return true if the update transaction was successful.
     */
    public boolean updatePurchase(PurchaseModel purchaseModel) {
        Connection conn = null;
        int purchaseId = purchaseModel.getPurchaseID();

        // 1. Fetch OLD totals for reversal
        PurchaseModel oldPurchase = getOldPurchaseTotals(purchaseId);
        if (oldPurchase == null) {
            JOptionPane.showMessageDialog(null, "Cannot find original purchase to update.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // --- A. LEDGER REVERSAL ---
            double oldNetChange = oldPurchase.getTotalAmount() - oldPurchase.getPaidAmount();
            // Reversal: Subtracting a debt (negative netChange) increases the balance, so we add the negative.
            updateSupplierBalanceInTransaction(conn, oldPurchase.getSupplierID(), -oldNetChange);

            // --- B. DELETE OLD DETAILS ---
            String sqlDeleteDetails = "DELETE FROM TBLPurchaseDetail WHERE PurchaseID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteDetails)) {
                ps.setInt(1, purchaseId);
                ps.executeUpdate();
            }

            // --- C. UPDATE PURCHASE HEADER (TBLPurchase) ---
            String sqlUpdateHeader = "UPDATE TBLPurchase SET SupplierID=?, PurchaseDate=?, InvoiceNo=?, TotalAmount=?, PaidAmount=?, Remarks=? WHERE PurchaseID=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateHeader)) {
                ps.setInt(1, purchaseModel.getSupplierID());
                ps.setTimestamp(2, Timestamp.valueOf(purchaseModel.getPurchaseDate()));
                ps.setString(3, purchaseModel.getInvoiceNo());
//                ps.setDouble(4, purchaseModel.getActualAmount());
//                ps.setString(5, purchaseModel.getDiscountType());
//                ps.setDouble(6, purchaseModel.getDiscountValue());
                ps.setDouble(4, purchaseModel.getTotalAmount());
                ps.setDouble(5, purchaseModel.getPaidAmount());
                ps.setString(6, purchaseModel.getRemarks());
                ps.setInt(7, purchaseId);

                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Failed to update purchase header.");
                }
            }

            // --- D. INSERT NEW DETAILS ---
            String sqlInsertDetails = "INSERT INTO TBLPurchaseDetail (PurchaseID, ProductID, Quantity, Rate, Total) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement psDetail = conn.prepareStatement(sqlInsertDetails)) {
                for (PurchaseDetailModel detail : purchaseModel.getDetails()) {
                    psDetail.setInt(1, purchaseId);
                    psDetail.setInt(2, detail.getProductID());
                    psDetail.setDouble(3, detail.getQuantity());
                    psDetail.setDouble(4, detail.getRate());
                    psDetail.setDouble(5, detail.getTotal());
                    psDetail.addBatch();
                }
                psDetail.executeBatch();
            }

            // --- E. APPLY NEW LEDGER CHANGE ---
            double newNetChange = purchaseModel.getTotalAmount() - purchaseModel.getPaidAmount();
            updateSupplierBalanceInTransaction(conn, purchaseModel.getSupplierID(), newNetChange);

            // --- F. COMMIT ---
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Purchase Update Transaction failed. Rolling back: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            JOptionPane.showMessageDialog(null, "Purchase update failed due to a database error.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    // Inside PurchaseDao.java

    /**
     * Fetches only the necessary header fields of an existing purchase for ledger reversal.
     */
    private PurchaseModel getOldPurchaseTotals(int purchaseId) {
        String sql = "SELECT TotalAmount, PaidAmount, SupplierID FROM TBLPurchase WHERE PurchaseID = ?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PurchaseModel.builder()
                            .totalAmount(rs.getDouble("TotalAmount"))
                            .paidAmount(rs.getDouble("PaidAmount"))
                            .supplierID(rs.getInt("SupplierID"))
                            .build();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching old purchase totals: " + e.getMessage());
        }
        return null;
    }

    /**
     * Helper method to update the supplier balance using an existing connection/transaction.
     */
    private void updateSupplierBalanceInTransaction(Connection conn, int supplierId, double netChange) throws SQLException {
        String sqlBalance = "UPDATE TBLSuppliers SET OpeningBalance = OpeningBalance + ? WHERE SupplierID = ?";
        try (PreparedStatement psBalance = conn.prepareStatement(sqlBalance)) {
            psBalance.setDouble(1, netChange);
            psBalance.setInt(2, supplierId);

            if (psBalance.executeUpdate() == 0) {
                throw new SQLException("Failed to update supplier outstanding balance (ID: " + supplierId + ")");
            }
        }
    }
}