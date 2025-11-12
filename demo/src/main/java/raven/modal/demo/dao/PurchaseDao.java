package raven.modal.demo.dao;

import raven.modal.demo.model.PurchaseDetailModel;
import raven.modal.demo.model.PurchaseModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDao {

    /**
     * Deletes a purchase transaction, performing a ledger reversal and removing all details.
     * @param purchaseId The ID of the purchase to delete.
     * @return true if the deletion transaction was successful.
     */
    public boolean deletePurchase(int purchaseId) {
        Connection conn = null;

        // 1. Fetch OLD totals for reversal (we reuse the existing helper method)
        PurchaseModel oldPurchase = getOldPurchaseTotals(purchaseId);
        if (oldPurchase == null) {
            JOptionPane.showMessageDialog(null, "Cannot find original purchase to delete.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // --- A. LEDGER REVERSAL ---
            double oldNetChange = oldPurchase.getTotalAmount() - oldPurchase.getPaidAmount();

            // Reversal: The original transaction ADDED 'oldNetChange' to the balance.
            // To reverse it, we SUBTRACT 'oldNetChange'.
            // We reuse the updateSupplierBalanceInTransaction helper by passing -oldNetChange.
            updateSupplierBalanceInTransaction(conn, oldPurchase.getSupplierID(), -oldNetChange);

            // --- B. DELETE OLD DETAILS ---
            String sqlDeleteDetails = "DELETE FROM TBLPurchaseDetail WHERE PurchaseID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteDetails)) {
                ps.setInt(1, purchaseId);
                ps.executeUpdate();
                // We don't check row count here; it's okay if it deletes 0 details if somehow the record was already cleaned.
            }

            // --- C. DELETE PURCHASE HEADER (TBLPurchase) ---
            String sqlDeleteHeader = "DELETE FROM TBLPurchase WHERE PurchaseID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteHeader)) {
                ps.setInt(1, purchaseId);

                if (ps.executeUpdate() == 0) {
                    // If the header wasn't deleted, something is wrong.
                    throw new SQLException("Failed to delete purchase header. Purchase ID not found.");
                }
            }

            // --- D. COMMIT ---
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Purchase Deletion Transaction failed. Rolling back: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            JOptionPane.showMessageDialog(null, "Purchase deletion failed due to a database error.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

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
            String sqlDetails = "SELECT pd.*, p.ProductName, pt.quarterQty as UnitPerCarton " +
                    "FROM TBLPurchaseDetail pd " +
                    "JOIN TBLProducts p ON pd.ProductID = p.ProductID " +
                    "JOIN TBLPeckingType pt ON p.PeckingTypeId = pt.PeekingTypeId " +
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
// Assuming the necessary helper method exists in PurchaseDao:
    // private void updateProductStock(Connection conn, int productId, double quantityChange) throws SQLException

    public boolean savePurchase(PurchaseModel purchaseModel) {
        String sqlPurchase = "INSERT INTO TBLPurchase (SupplierID, PurchaseDate, InvoiceNo, TotalAmount, PaidAmount, Remarks, CreatedDate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlDetail = "INSERT INTO TBLPurchaseDetail (PurchaseID, ProductID, Quantity, Rate, Total) VALUES (?, ?, ?, ?, ?)";
        String sqlStock = "INSERT INTO TBLStockLedger (ProductID, RefType, RefID, RefDetailID, QtyIn, Rate) VALUES (?, 'PURCHASE', ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert Purchase Header
            PreparedStatement psPurchase = conn.prepareStatement(sqlPurchase, Statement.RETURN_GENERATED_KEYS);
            Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());

            psPurchase.setInt(1, purchaseModel.getSupplierID());
            psPurchase.setTimestamp(2, Timestamp.valueOf(purchaseModel.getPurchaseDate()));
            psPurchase.setString(3, purchaseModel.getInvoiceNo());
            psPurchase.setDouble(4, purchaseModel.getTotalAmount());
            psPurchase.setDouble(5, purchaseModel.getPaidAmount());
            psPurchase.setString(6, purchaseModel.getRemarks());
            psPurchase.setTimestamp(7, currentTimestamp);

            psPurchase.executeUpdate();

            // Get generated PurchaseID
            int purchaseId = 0;
            ResultSet rs = psPurchase.getGeneratedKeys();
            if (rs.next()) {
                purchaseId = rs.getInt(1);
            } else {
                conn.rollback();
                JOptionPane.showMessageDialog(null, "Failed to retrieve Purchase ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // 2. Insert Purchase Details and Update Stock
            PreparedStatement psDetail = conn.prepareStatement(sqlDetail, Statement.RETURN_GENERATED_KEYS);
            PreparedStatement psStock = conn.prepareStatement(sqlStock);

            for (PurchaseDetailModel detail : purchaseModel.getDetails()) {
                // A. Insert Purchase Detail
                psDetail.setInt(1, purchaseId);
                psDetail.setInt(2, detail.getProductID());
                psDetail.setDouble(3, detail.getQuantity());
                psDetail.setDouble(4, detail.getRate());
                psDetail.setDouble(5, detail.getTotal());
                psDetail.executeUpdate(); // Execute immediately to get the ID

                int purchaseDetailsId;
                try (ResultSet rs1 = psDetail.getGeneratedKeys()) {
                    if (rs1.next()) {
                        purchaseDetailsId = rs1.getInt(1);
                    } else {
                        // Crucial: Throwing exception forces rollback
                        throw new SQLException("Failed to retrieve Purchase Detail ID.");
                    }
                }

                // B. Insert Stock Ledger Entry (QtyIn)
                psStock.setInt(1, detail.getProductID());
                psStock.setInt(2, purchaseId);
                psStock.setInt(3, purchaseDetailsId);
                psStock.setDouble(4, detail.getQuantity()); // QtyIn
                psStock.setDouble(5, detail.getRate());
                // FIX: Execute immediately after setting parameters, don't use executeBatch here
                if(psStock.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert stock ledger entry for Product ID: " + detail.getProductID());
                }

                // C. Update TBLProducts.CurrentStock (Adds quantity)
//                updateProductStock(conn, detail.getProductID(), detail.getQuantity());
            }

            // 3. Update Supplier Balance
            double totalAmount = purchaseModel.getTotalAmount();
            double paidAmount = purchaseModel.getPaidAmount();
            int supplierId = purchaseModel.getSupplierID();

            double netChange = totalAmount - paidAmount;

            String sqlBalance = "UPDATE TBLSuppliers SET OpeningBalance = OpeningBalance + ? WHERE SupplierID = ?";
            try (PreparedStatement psBalance = conn.prepareStatement(sqlBalance)) {
                psBalance.setDouble(1, netChange);
                psBalance.setInt(2, supplierId);

                if (psBalance.executeUpdate() == 0) {
                    throw new SQLException("Failed to update supplier outstanding balance.");
                }
            }

            conn.commit(); // Commit transaction
            JOptionPane.showMessageDialog(null, "Purchase transaction saved successfully! ID: " + purchaseId, "Success", JOptionPane.INFORMATION_MESSAGE);
            return true;

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback(); // Rollback on error
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            System.err.println("Database error during savePurchase: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true); // Restore auto-commit
            } catch (SQLException closeEx) {
                // Ignore
            }
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