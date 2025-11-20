package raven.modal.demo.dao;

import raven.modal.demo.model.SaleDetailModel;
import raven.modal.demo.model.SaleModel;
import raven.modal.demo.mysql.MySQLConnection;
import raven.modal.demo.utils.Constants;

import javax.swing.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaleDao {

    /**
     * Serializes a list of SaleDetailModel objects into a JSON array string.
     */
    private String serializeDetailsToJson(List<SaleDetailModel> details) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        for (int i = 0; i < details.size(); i++) {
            SaleDetailModel detail = details.get(i);

            jsonBuilder.append("{");
            jsonBuilder.append("\"productID\":").append(detail.getProductID()).append(",");
            jsonBuilder.append("\"quantity\":").append(detail.getQuantity()).append(",");
            jsonBuilder.append("\"rate\":").append(detail.getRate()).append(",");
            jsonBuilder.append("\"productDiscount\":").append(detail.getProductDiscount()).append(",");
            jsonBuilder.append("\"total\":").append(detail.getTotal());
            jsonBuilder.append("}");

            if (i < details.size() - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    public boolean handleSaleCRUD(SaleModel saleModel, String status) {
        String sql = "{CALL SP_IUD_Sale(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        Connection conn = null;

        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            CallableStatement cs = conn.prepareCall(sql);

            boolean isSave = status.equalsIgnoreCase("Save");
            boolean isDelete = status.equalsIgnoreCase("Delete");

            // Convert details → JSON (Only Save/Update)
            String detailsJson = (!isDelete) ? serializeDetailsToJson(saleModel.getDetails()) : null;

            // 1. SaleID (INOUT)
            if (isSave) {
                cs.setNull(1, Types.INTEGER);
            } else {
                cs.setInt(1, saleModel.getSaleID());
            }
            cs.registerOutParameter(1, Types.INTEGER);

            // 2. CustomerID
            cs.setInt(2, saleModel.getCustomerID());

            // 3. SaleDate
            cs.setTimestamp(3, Timestamp.valueOf(saleModel.getSaleDate()));

            // 4. InvoiceNo (INOUT)
            if (isSave) {
                cs.setNull(4, Types.VARCHAR);
            } else {
                cs.setString(4, saleModel.getInvoiceNo());
            }
            cs.registerOutParameter(4, Types.VARCHAR);

            // 5-12 Header Fields
            cs.setDouble(5, saleModel.getActualAmount());
            cs.setDouble(6, saleModel.getGstPer());
            cs.setDouble(7, saleModel.getGstAmount());
            cs.setString(8, saleModel.getDiscountType());
            cs.setDouble(9, saleModel.getDiscountValue());
            cs.setDouble(10, saleModel.getTotalAmount());
            cs.setDouble(11, saleModel.getReceivedAmount());
            cs.setString(12, saleModel.getRemarks());

            // 13. DateTime
            cs.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));

            // 14. UserID
            cs.setInt(14, Constants.getCurrentUserId());

            // 15. Status
            cs.setString(15, status);

            // 16. Details JSON
            if (!isDelete) {
                cs.setString(16, detailsJson);
            } else {
                cs.setNull(16, Types.VARCHAR);
            }

            // 17. Return Code (OUT)
            cs.registerOutParameter(17, Types.INTEGER);

            // Execute
            cs.executeUpdate();

            // Read OUT parameters
            int saleID = cs.getInt(1);
            String invoiceNo = cs.getString(4);
            int result = cs.getInt(17);

            if (result <= 0) {
                conn.rollback();
                return false;
            }

            conn.commit();

            // Success Messages
            switch (result) {
                case -1:
                    JOptionPane.showMessageDialog(null, "Sale Updated Successfully!", "Updated",
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
                case -2:
                    JOptionPane.showMessageDialog(null, "Sale Deleted Successfully!", "Deleted",
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "Sale Saved!\nID: " + saleID + "\nInvoice: " + invoiceNo,
                            "Saved", JOptionPane.INFORMATION_MESSAGE);
            }

            return true;

        } catch (Exception e) {
            try {
                if (conn != null)
                    conn.rollback();
            } catch (Exception ignore) {
            }
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (Exception ignore) {
            }
        }
    }

    public boolean saveSale(SaleModel saleModel) {
        return handleSaleCRUD(saleModel, "Save");
    }

    public boolean updateSale(SaleModel saleModel) {
        return handleSaleCRUD(saleModel, "Update");
    }

    public boolean deleteSale(int saleId) {
        // For delete, we need to fetch the sale first to pass required fields (like
        // CustomerID for reversal)
        // However, the SP handles reversal by looking up the ID.
        // But we need to pass a SaleModel to handleSaleCRUD.
        // We can create a dummy model with just the ID, but the SP might expect other
        // fields not to be null if we were strict.
        // In our SP, for DELETE, we only use SaleID to look up the record for reversal.
        // But we pass CustomerID etc. as parameters.
        // Let's fetch the sale first to be safe and pass valid data.
        SaleModel sale = getSaleForEdit(saleId);
        if (sale == null) {
            JOptionPane.showMessageDialog(null, "Sale not found for deletion.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return handleSaleCRUD(sale, "Delete");
    }

    /**
     * Checks the current stock level for a product.
     */
    public double getAvailableStock(int productId) {
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
                        sale = SaleModel.builder()
                                .saleID(rs.getInt("SaleID"))
                                .customerID(rs.getInt("CustomerID"))
                                .customerName(rs.getString("CustomerName"))
                                .saleDate(rs.getTimestamp("SaleDate").toLocalDateTime())
                                .invoiceNo(rs.getString("InvoiceNo"))
                                .totalAmount(rs.getDouble("TotalAmount"))
                                .receivedAmount(rs.getDouble("ReceivedAmount"))
                                .actualAmount(rs.getDouble("ActualAmount"))
                                .gstPer(rs.getDouble("GSTPercentage"))
                                .gstAmount(rs.getDouble("GSTAmount"))
                                .discountType(rs.getString("DiscountType"))
                                .discountValue(rs.getDouble("Discount"))
                                .remarks(rs.getString("Remarks"))
                                .details(new ArrayList<>())
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
                                .productDiscount(rs.getDouble("ProductDiscount"))
                                .total(rs.getDouble("Total"))
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
}
