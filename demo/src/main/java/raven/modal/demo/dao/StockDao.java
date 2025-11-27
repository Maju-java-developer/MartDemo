package raven.modal.demo.dao;

import raven.modal.demo.mysql.MySQLConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StockDao {

    /**
     * Checks the current available stock quantity for a given product ID by calling
     * the MySQL stored function fnGetProductStock.
     * @param productId The ID of the product to check.
     * @return The current available quantity (QtyIn - QtyOut).
     */
    public double checkCurrentStock(int productId) {
        // Call the stored function using a standard SELECT statement
        String sql = "SELECT fnGetProductStock(?)";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // The result of the function is in the first column (index 1)
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during stock check: " + e.getMessage());
            // Treat error as zero stock to prevent accidental overselling
            return 0.0;
        }
        return 0.0;
    }
}
