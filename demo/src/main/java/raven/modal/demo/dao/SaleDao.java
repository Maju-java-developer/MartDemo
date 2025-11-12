package raven.modal.demo.dao;

import raven.modal.demo.model.SaleDetailModel;
import raven.modal.demo.model.SaleModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SaleDao {

    /**
     * Saves a new sale transaction.
     * This is a complex transaction involving TBLSale, TBLSaleDetail, TBLStockLedger, and TBLCustomers.
     */
    public boolean saveSale(SaleModel saleModel) {
        Connection conn = null;

        // 1. Calculate the net change to the Customer's balance
        // A Sale DECREASES the customer's balance (they owe less)
        // Net change = Total Owed (TotalAmount) - Total Paid (ReceivedAmount)
        double netReceivableChange = saleModel.getTotalAmount() - saleModel.getReceivedAmount();

        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // --- A. INSERT SALE HEADER (TBLSale) ---
            String sqlHeader = "INSERT INTO TBLSale (CustomerID, SaleDate, InvoiceNo, TotalAmount, ReceivedAmount, Remarks) VALUES (?, ?, ?, ?, ?, ?)";
            int saleId;
            try (PreparedStatement ps = conn.prepareStatement(sqlHeader, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, saleModel.getCustomerID());
                ps.setTimestamp(2, Timestamp.valueOf(saleModel.getSaleDate()));
                ps.setString(3, saleModel.getInvoiceNo());
                ps.setDouble(4, saleModel.getTotalAmount());
                ps.setDouble(5, saleModel.getReceivedAmount());
                ps.setString(6, saleModel.getRemarks());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        saleModel.setSaleID(saleId);
                    } else {
                        throw new SQLException("Failed to retrieve Sale ID.");
                    }
                }
            }

            // --- B. INSERT SALE DETAILS AND UPDATE STOCK LEDGER ---
            String sqlDetail = "INSERT INTO TBLSaleDetail (SaleID, ProductID, Quantity, Rate, Total) VALUES (?, ?, ?, ?, ?)";
            String sqlStock = "INSERT INTO TBLStockLedger (ProductID, RefType, RefID, RefDetailID, QtyOut, Rate) VALUES (?, 'SALE', ?, ?, ?, ?)";

            try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psStock = conn.prepareStatement(sqlStock)) {

                for (SaleDetailModel detail : saleModel.getDetails()) {
                    // i. Insert Sale Detail
                    psDetail.setInt(1, saleId);
                    psDetail.setInt(2, detail.getProductID());
                    psDetail.setDouble(3, detail.getQuantity());
                    psDetail.setDouble(4, detail.getRate());
                    psDetail.setDouble(5, detail.getTotal());
                    psDetail.executeUpdate();

                    int saleDetailId;
                    try (ResultSet rs = psDetail.getGeneratedKeys()) {
                        if (rs.next()) {
                            saleDetailId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve Sale Detail ID.");
                        }
                    }

                    // ii. Insert Stock Ledger Entry (QtyOut)
                    psStock.setInt(1, detail.getProductID());
                    psStock.setInt(2, saleId);
                    psStock.setInt(3, saleDetailId);
                    psStock.setDouble(4, detail.getQuantity()); // QtyOut
                    psStock.setDouble(5, detail.getRate());
                    psStock.executeUpdate();

                    // TODO: CRITICAL STEP: Update TBLProducts.CurrentStock - Omitted for now, but necessary
                }
            }

            // --- C. UPDATE CUSTOMER LEDGER (TBLCustomers) ---
            // A Sale INCREASES the customer's OutstandingBalance (they owe more)
            updateCustomerBalanceInTransaction(conn, saleModel.getCustomerID(), netReceivableChange);

            // --- D. COMMIT TRANSACTION ---
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Sale Save Transaction failed. Rolling back: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) { ex.printStackTrace(); }
            JOptionPane.showMessageDialog(null, "Sale record failed due to a database error. Details: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // --- Helper Methods ---

    /**
     * Helper method to update the customer balance using an existing connection/transaction.
     */
    private void updateCustomerBalanceInTransaction(Connection conn, int customerId, double netChange) throws SQLException {
        // TBLCustomers.OutstandingBalance is increased by the net receivable change (Total - Received)
        String sqlBalance = "UPDATE TBLCustomers SET OpeningBalance = OpeningBalance + ? WHERE CustomerID = ?";
        try (PreparedStatement psBalance = conn.prepareStatement(sqlBalance)) {
            psBalance.setDouble(1, netChange);
            psBalance.setInt(2, customerId);

            if (psBalance.executeUpdate() == 0) {
                throw new SQLException("Failed to update customer outstanding balance (ID: " + customerId + ")");
            }
        }
    }

    /**
     * Checks the current stock level for a product.
     * In a real system, this would often be complex (FIFO, LIFO, etc.), but here
     * we will calculate it by summing QtyIn and subtracting QtyOut from TBLStockLedger.
     */
    public double getAvailableStock(int productId) {
        // NOTE: This query calculates stock based on the TBLStockLedger history.
        // If TBLProducts holds the current stock, use that field instead for performance.
        String sql = "SELECT SUM(QtyIn) - SUM(QtyOut) AS CurrentStock FROM TBLStockLedger WHERE ProductID = ?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("CurrentStock");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching available stock: " + e.getMessage());
        }
        return 0.0;
    }
    /**
     * Fetches a paginated list of sales history.
     */
    public List<SaleModel> getSales(int offset, int limit) {
        String sql = "SELECT s.*, c.CustomerName " +
                "FROM TBLSale s " +
                "JOIN TBLCustomers c ON s.CustomerID = c.CustomerID " +
                "ORDER BY s.SaleDate DESC LIMIT ? OFFSET ?";

        List<SaleModel> sales = new ArrayList<>();

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double total = rs.getDouble("TotalAmount");
                    double received = rs.getDouble("ReceivedAmount");

                    sales.add(SaleModel.builder()
                            .saleID(rs.getInt("SaleID"))
                            .customerID(rs.getInt("CustomerID"))
                            .customerName(rs.getString("CustomerName"))
                            .saleDate(rs.getTimestamp("SaleDate").toLocalDateTime())
                            .invoiceNo(rs.getString("InvoiceNo"))
                            .totalAmount(total)
                            .receivedAmount(received)
                            .remarks(rs.getString("Remarks"))
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching sales: " + e.getMessage());
        }
        return sales;
    }
    // TODO: Implement getSales(), getSaleCount(), getSaleForEdit(), updateSale(), deleteSale()
// Inside SaleDao.java

    /**
     * Returns the total number of sale records.
     */
    public int getSaleCount() {
        String sql = "SELECT COUNT(*) FROM TBLSale";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching sale count: " + e.getMessage());
        }
        return 0;
    }
    // Inside SaleDao.java

    /**
     * Fetches sale header and all associated details for editing.
     */
    public SaleModel getSaleForEdit(int saleId) {
        SaleModel sale = null;
        try (Connection conn = MySQLConnection.getInstance().getConnection()) {

            // 1. Fetch Header Data
            String sqlHeader = "SELECT s.*, c.CustomerName " +
                    "FROM TBLSale s " +
                    "JOIN TBLCustomers c ON s.CustomerID = c.CustomerID " +
                    "WHERE s.SaleID = ?";

            try (PreparedStatement psHeader = conn.prepareStatement(sqlHeader)) {
                psHeader.setInt(1, saleId);
                try (ResultSet rs = psHeader.executeQuery()) {
                    if (rs.next()) {
                        // NOTE: Header discount fields are not in TBLSale yet,
                        // but we include placeholders for full implementation.
                        sale = SaleModel.builder()
                                .saleID(rs.getInt("SaleID"))
                                .customerID(rs.getInt("CustomerID"))
                                .customerName(rs.getString("CustomerName"))
                                .saleDate(rs.getTimestamp("SaleDate").toLocalDateTime())
                                .invoiceNo(rs.getString("InvoiceNo"))
                                // Assuming TotalAmount includes GST, ActualAmount is net/taxable base
                                .totalAmount(rs.getDouble("TotalAmount"))
                                .receivedAmount(rs.getDouble("ReceivedAmount"))
                                .remarks(rs.getString("Remarks"))
                                .details(new ArrayList<>())
                                // TODO: Load discountType, discountValue from TBLSale if added later
                                .build();
                    } else {
                        return null;
                    }
                }
            }

            // 2. Fetch Detail Data (Line Items)
            String sqlDetails = "SELECT sd.*, p.ProductName, pt.quarterQty as UnitPerCarton " +
                    "FROM TBLSaleDetail sd " +
                    "JOIN TBLProducts p ON sd.ProductID = p.ProductID " +
                    "JOIN TBLPeckingType pt ON p.PeckingTypeId = pt.PeekingTypeId " +
                    "WHERE sd.SaleID = ?";

            try (PreparedStatement psDetails = conn.prepareStatement(sqlDetails)) {
                psDetails.setInt(1, saleId);
                try (ResultSet rs = psDetails.executeQuery()) {
                    while (rs.next()) {
                        SaleDetailModel detail = SaleDetailModel.builder()
                                .saleDetailID(rs.getInt("SaleDetailID"))
                                .productID(rs.getInt("ProductID"))
                                .productName(rs.getString("ProductName"))
                                .unitsPerCarton(rs.getInt("UnitPerCarton"))
                                .quantity(rs.getDouble("Quantity"))
                                .rate(rs.getDouble("Rate"))
                                // .lineDiscount(rs.getDouble("LineDiscount"))
                                .total(rs.getDouble("Total")) // This is the net value of the row
                                .build();
                        sale.getDetails().add(detail);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error fetching sale for edit: " + e.getMessage());
            return null;
        }
        return sale;
    }
    // Inside SaleDao.java

    /**
     * Updates an existing sale transaction. Reverses old stock/ledger and applies new.
     */
    public boolean updateSale(SaleModel saleModel) {
        Connection conn = null;
        int saleId = saleModel.getSaleID();

        // 1. Fetch OLD totals for reversal
        SaleModel oldSale = getOldSaleTotals(saleId); // Need to implement this helper
        if (oldSale == null) {
            JOptionPane.showMessageDialog(null, "Cannot find original sale to update.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // New Net Receivable change
        double newNetReceivableChange = saleModel.getTotalAmount() - saleModel.getReceivedAmount();
        // Old Net Receivable change
        double oldNetReceivableChange = oldSale.getTotalAmount() - oldSale.getReceivedAmount();

        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // --- A. LEDGER REVERSAL ---
            // Reversal: The original sale ADDED oldNetReceivableChange. Subtract it back.
            updateCustomerBalanceInTransaction(conn, oldSale.getCustomerID(), -oldNetReceivableChange);

            // --- B. STOCK REVERSAL & DETAIL DELETION ---
            // Reversing stock for a sale means inserting a QtyIn entry (a 'Sale Reversal')
            reverseStockAndDetails(conn, saleId, oldSale.getDetails()); // Need to implement this helper

            // --- C. UPDATE HEADER (TBLSale) ---
            // NOTE: SQL needs to match the fields in TBLSale (InvoiceNo, TotalAmount, ReceivedAmount, Remarks, etc.)
            String sqlUpdateHeader = "UPDATE TBLSale SET CustomerID=?, SaleDate=?, InvoiceNo=?, TotalAmount=?, ReceivedAmount=?, Remarks=? WHERE SaleID=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateHeader)) {
                ps.setInt(1, saleModel.getCustomerID());
                ps.setTimestamp(2, Timestamp.valueOf(saleModel.getSaleDate()));
                ps.setString(3, saleModel.getInvoiceNo());
                ps.setDouble(4, saleModel.getTotalAmount());
                ps.setDouble(5, saleModel.getReceivedAmount());
                ps.setString(6, saleModel.getRemarks());
                ps.setInt(7, saleId);

                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Failed to update sale header.");
                }
            }

            // --- D. INSERT NEW DETAILS AND UPDATE STOCK LEDGER ---
            insertNewDetailsAndStock(conn, saleId, saleModel.getDetails()); // Need to implement this helper

            // --- E. APPLY NEW LEDGER CHANGE ---
            // Apply: The new sale ADDS the newNetReceivableChange.
            updateCustomerBalanceInTransaction(conn, saleModel.getCustomerID(), newNetReceivableChange);

            // --- F. COMMIT ---
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Sale Update Transaction failed. Rolling back: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            JOptionPane.showMessageDialog(null, "Sale update failed due to a database error.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    // Inside SaleDao.java

    /**
     * Deletes a sale transaction, performing ledger and stock reversal.
     */
    public boolean deleteSale(int saleId) {
        Connection conn = null;

        // 1. Fetch OLD totals and details for reversal
        SaleModel oldSale = getOldSaleForReversal(saleId); // Need a helper that gets both header and details
        if (oldSale == null) {
            JOptionPane.showMessageDialog(null, "Cannot find original sale to delete.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Old Net Receivable change
        double oldNetReceivableChange = oldSale.getTotalAmount() - oldSale.getReceivedAmount();

        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // --- A. LEDGER REVERSAL ---
            // Reversal: Subtract the change that the original sale added.
            updateCustomerBalanceInTransaction(conn, oldSale.getCustomerID(), -oldNetReceivableChange);

            // --- B. STOCK REVERSAL & DETAIL DELETION ---
            reverseStockAndDetails(conn, saleId, oldSale.getDetails());

            // --- C. DELETE SALE HEADER (TBLSale) ---
            String sqlDeleteHeader = "DELETE FROM TBLSale WHERE SaleID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteHeader)) {
                ps.setInt(1, saleId);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Failed to delete sale header.");
                }
            }

            // --- D. COMMIT ---
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Sale Deletion Transaction failed. Rolling back: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            JOptionPane.showMessageDialog(null, "Sale deletion failed due to a database error.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    // Inside SaleDao.java

    /**
     * Inserts QtyIn entries to TBLStockLedger to reverse the sale's stock impact,
     * then deletes TBLSaleDetail and TBLStockLedger entries linked to the SaleID.
     */
    private void reverseStockAndDetails(Connection conn, int saleId, List<SaleDetailModel> oldDetails) throws SQLException {

        // 1. Insert QtyIn to reverse the original stock outflow
        String sqlStockIn = "INSERT INTO TBLStockLedger (ProductID, RefType, RefID, QtyIn, Rate) VALUES (?, 'SALE_REVERSAL', ?, ?, ?)";
        try (PreparedStatement psStockIn = conn.prepareStatement(sqlStockIn)) {
            for (SaleDetailModel detail : oldDetails) {
                psStockIn.setInt(1, detail.getProductID());
                psStockIn.setInt(2, saleId);
                psStockIn.setDouble(3, detail.getQuantity()); // QtyIn
                psStockIn.setDouble(4, detail.getRate());
                psStockIn.addBatch();

                // TODO: CRITICAL STEP: Update TBLProducts.CurrentStock - Add back the quantity
            }
            psStockIn.executeBatch();
        }

        // 2. Delete old TBLSaleDetail entries
        String sqlDeleteDetails = "DELETE FROM TBLSaleDetail WHERE SaleID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlDeleteDetails)) {
            ps.setInt(1, saleId);
            ps.executeUpdate();
        }

        // 3. Delete old TBLStockLedger entries (The original QtyOut records)
        // NOTE: Deleting the original QtyOut records while keeping the QtyIn reversal
        // simplifies history, but might be debated. For now, we will delete the original
        // TBLStockLedger records linked to the SaleID as well, leaving only the reversal entry.
        String sqlDeleteStock = "DELETE FROM TBLStockLedger WHERE RefID = ? AND RefType = 'SALE'";
        try (PreparedStatement ps = conn.prepareStatement(sqlDeleteStock)) {
            ps.setInt(1, saleId);
            ps.executeUpdate();
        }
    }
    // Helper method
    /**
     * Fetches only the necessary header fields of an existing sale for ledger reversal.
     * Used to calculate the old net receivable change (TotalAmount - ReceivedAmount).
     */
    private SaleModel getOldSaleTotals(int saleId) {
        String sql = "SELECT CustomerID, TotalAmount, ReceivedAmount FROM TBLSale WHERE SaleID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return SaleModel.builder()
                            .saleID(saleId)
                            .customerID(rs.getInt("CustomerID"))
                            .totalAmount(rs.getDouble("TotalAmount"))
                            .receivedAmount(rs.getDouble("ReceivedAmount"))
                            .build();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching old sale totals: " + e.getMessage());
        }
        return null;
    }
    /**
     * Fetches header and details for stock/ledger reversal during update/delete.
     * Fetches required fields to reverse stock (QtyOut) and ledger.
     */
    private SaleModel getOldSaleForReversal(int saleId) {
        SaleModel sale = getOldSaleTotals(saleId); // Reuse the header fetching logic
        if (sale == null) {
            return null;
        }

        // Fetch only the details required for reversal (ProductID, Quantity, Rate)
        String sqlDetails = "SELECT SaleDetailID, ProductID, Quantity, Rate FROM TBLSaleDetail WHERE SaleID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement psDetails = conn.prepareStatement(sqlDetails)) {

            psDetails.setInt(1, saleId);

            List<SaleDetailModel> details = new ArrayList<>();
            try (ResultSet rs = psDetails.executeQuery()) {
                while (rs.next()) {
                    details.add(SaleDetailModel.builder()
                            .saleDetailID(rs.getInt("SaleDetailID"))
                            .productID(rs.getInt("ProductID"))
                            .quantity(rs.getDouble("Quantity")) // Qty to reverse
                            .rate(rs.getDouble("Rate"))
                            .build());
                }
            }
            sale.setDetails(details);

        } catch (SQLException e) {
            System.err.println("Error fetching old sale details for reversal: " + e.getMessage());
            return null;
        }
        return sale;
    }
    /**
     * Inserts new sale details and corresponding QtyOut stock ledger entries.
     * Executes within an active transaction (uses provided Connection conn).
     */
    private void insertNewDetailsAndStock(Connection conn, int saleId, List<SaleDetailModel> newDetails) throws SQLException {

        // SQL for TBLSaleDetail insertion (Total amount is the Net price after line discount)
        String sqlDetail = "INSERT INTO TBLSaleDetail (SaleID, ProductID, Quantity, Rate, Total) VALUES (?, ?, ?, ?, ?)";

        // SQL for TBLStockLedger insertion (This is the QtyOut entry)
        String sqlStock = "INSERT INTO TBLStockLedger (ProductID, RefType, RefID, RefDetailID, QtyOut, Rate) VALUES (?, 'SALE', ?, ?, ?, ?)";

        try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psStock = conn.prepareStatement(sqlStock)) {

            for (SaleDetailModel detail : newDetails) {

                // i. Insert Sale Detail
                psDetail.setInt(1, saleId);
                psDetail.setInt(2, detail.getProductID());
                psDetail.setDouble(3, detail.getQuantity());
                psDetail.setDouble(4, detail.getRate());
                psDetail.setDouble(5, detail.getTotal());
                psDetail.executeUpdate();

                int saleDetailId;
                try (ResultSet rs = psDetail.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleDetailId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve new Sale Detail ID during update insertion.");
                    }
                }

                // ii. Insert Stock Ledger Entry (QtyOut)
                psStock.setInt(1, detail.getProductID());
                psStock.setInt(2, saleId);
                psStock.setInt(3, saleDetailId);
                psStock.setDouble(4, detail.getQuantity()); // QtyOut
                psStock.setDouble(5, detail.getRate());
                psStock.executeUpdate();

                // TODO: CRITICAL STEP: Update TBLProducts.CurrentStock - Subtract the quantity
            }
        }
    }

}
