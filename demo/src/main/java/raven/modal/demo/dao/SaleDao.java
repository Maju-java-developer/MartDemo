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

    public List<SaleModel> getSales(int offset, int limit) {
        List<SaleModel> sales = new ArrayList<>();
        String sql = "{CALL SP_Get(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
                CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, offset); // p_Id (Used as Offset for SaleList)
            cs.setInt(2, limit); // p_OtherId (Used as Limit for SaleList)
            cs.setNull(3, Types.VARCHAR); // p_Search
            cs.setNull(4, Types.VARCHAR); // p_OtherData
            cs.setString(5, "SaleList"); // p_SearchType
            cs.setInt(6, Constants.getCurrentUserId()); // p_UserID
            cs.setNull(7, Types.TIMESTAMP); // p_DateTime

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    sales.add(SaleModel.builder()
                            .saleID(rs.getInt("SaleID"))
                            .invoiceNo(rs.getString("InvoiceNo"))
                            .customerName(rs.getString("CustomerName"))
                            .saleDate(rs.getTimestamp("SaleDate").toLocalDateTime())
                            .totalAmount(rs.getDouble("TotalAmount"))
                            .receivedAmount(rs.getDouble("ReceivedAmount"))
                            .build());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

    public SaleModel getSaleById(int saleId) {
        SaleModel sale = null;
        String sql = "{CALL SP_Get(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
                CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, saleId);
            cs.setNull(2, Types.INTEGER);
            cs.setNull(3, Types.VARCHAR);
            cs.setNull(4, Types.VARCHAR);
            cs.setString(5, "Sale");
            cs.setInt(6, Constants.getCurrentUserId());
            cs.setNull(7, Types.TIMESTAMP);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    sale = SaleModel.builder()
                            .saleID(rs.getInt("SaleID"))
                            .customerID(rs.getInt("CustomerID"))
                            .customerName(rs.getString("CustomerName"))
                            .saleDate(rs.getTimestamp("SaleDate").toLocalDateTime())
                            .actualAmount(rs.getDouble("ActualAmount"))
                            .gstPer(rs.getDouble("GSTPercentage"))
                            .gstAmount(rs.getDouble("GSTAmount"))
                            .discountType(rs.getString("DiscountType"))
                            .discountValue(rs.getDouble("Discount"))
                            .totalAmount(rs.getDouble("TotalAmount"))
                            .receivedAmount(rs.getDouble("ReceivedAmount"))
                            .remarks(rs.getString("Remarks"))
                            .build();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sale;
    }

    public List<SaleDetailModel> getSaleDetails(int saleId) {
        List<SaleDetailModel> details = new ArrayList<>();
        String sql = "{CALL SP_Get(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
                CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, saleId);
            cs.setNull(2, Types.INTEGER);
            cs.setNull(3, Types.VARCHAR);
            cs.setNull(4, Types.VARCHAR);
            cs.setString(5, "SaleLine");
            cs.setInt(6, Constants.getCurrentUserId());
            cs.setNull(7, Types.TIMESTAMP);

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    details.add(SaleDetailModel.builder()
                            .saleDetailID(rs.getInt("SaleDetailID"))
                            .saleID(rs.getInt("SaleID"))
                            .productID(rs.getInt("ProductID"))
                            .productName(rs.getString("ProductName"))
                            .quantity(rs.getDouble("Quantity"))
                            .rate(rs.getDouble("Rate"))
                            .productDiscount(rs.getDouble("ProductDiscount"))
                            .total(rs.getDouble("Total"))
                            .build());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return details;
    }
}
