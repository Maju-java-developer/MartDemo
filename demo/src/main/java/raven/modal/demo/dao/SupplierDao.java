package raven.modal.demo.dao;

import raven.modal.demo.model.SupplierModel;
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

public class SupplierDao {

    /**
     * Fetches only those suppliers who have a remaining OutstandingBalance > 0.
     * This is used for the Payment dialog.
     * @return List of SupplierModel with outstanding balances.
     */
    public List<SupplierModel> getRemainingBalanceSuppliers() {
        // ASSUMPTION: The TBLSuppliers table has columns: SupplierID, Name, OutstandingBalance
        String sql = "SELECT SupplierID, SupplierName, OpeningBalance FROM TBLSuppliers WHERE OpeningBalance > 0 ORDER BY SupplierName ASC";
        List<SupplierModel> suppliers = new ArrayList<>();

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Fetch only the necessary fields for the payment form
                SupplierModel supplier = new SupplierModel();
                supplier.setSupplierID(rs.getInt("SupplierID"));
                supplier.setSupplierName(rs.getString("SupplierName"));
                suppliers.add(supplier);
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching outstanding suppliers: " + e.getMessage());
        }
        return suppliers;
    }
    /**
     * Updates the OutstandingBalance for a supplier based on a new transaction.
     * OutstandingBalance is INCREASED by the Purchase Total and DECREASED by the Paid Amount.
     *
     * @param supplierId The ID of the supplier.
     * @param totalAmount The final Total Amount of the purchase (debt added).
     * @param paidAmount The amount paid at the time of purchase (debt reduced).
     * @return true if the update was successful.
     */
    public boolean updateSupplierBalance(int supplierId, double totalAmount, double paidAmount) {
        // The purchase increases the supplier's balance (liability/debt).
        // The paid amount decreases the supplier's balance.
        double netChange = totalAmount - paidAmount;

        // SQL: Update the balance by adding the net change (Total - Paid).
        String sql = "UPDATE TBLSuppliers SET OpeningBalance = OpeningBalance + ? WHERE SupplierID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, netChange);
            ps.setInt(2, supplierId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Database error updating supplier balance: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Error updating supplier ledger balance.", "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ✅ Get suppliers with pagination
    public List<SupplierModel> getActiveSuppliersForDropdown() {
        String sql = "SELECT SupplierID, SupplierName FROM TBLSuppliers ORDER BY SupplierID";
        List<SupplierModel> suppliers = new ArrayList<>();

        // Add a placeholder/default item
        suppliers.add(SupplierModel.builder().supplierID(0).supplierName("--- Select Supplier ---").build());

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                suppliers.add(SupplierModel.builder()
                        .supplierID(rs.getInt("SupplierID"))
                        .supplierName(rs.getString("SupplierName"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching active Supplier: " + e.getMessage());
        }
        return suppliers;
    }

    public List<SupplierModel> getSuppliers(int offset, int limit) {
        List<SupplierModel> list = new ArrayList<>();
        String sql = "SELECT * FROM TBLSuppliers ORDER BY SupplierID LIMIT ? OFFSET ?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SupplierModel model = new SupplierModel();
                    model.setSupplierID(rs.getInt("SupplierID"));
                    model.setSupplierName(rs.getString("SupplierName"));
                    model.setContactNo(rs.getString("ContactNo"));
                    model.setAddress(rs.getString("Address"));
                    model.setEmail(rs.getString("Email"));
                    model.setOpeningBalance(rs.getDouble("OpeningBalance"));
                    model.setCreatedDate(rs.getTimestamp("CreatedDate").toLocalDateTime());
                    list.add(model);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    public boolean updateSupplier(SupplierModel supplier) {
        String sql = "UPDATE TBLSuppliers SET SupplierName=?, ContactNo=?, Email=?, Address=?, OpeningBalance=? WHERE SupplierID=?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getContactNo());
            ps.setString(3, supplier.getEmail());
            ps.setString(4, supplier.getAddress());
            ps.setDouble(5, supplier.getOpeningBalance());
            ps.setInt(6, supplier.getSupplierID());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            handleSqlError(e, "updating vendor");
            return false;
        }
    }

    // --- DELETE Method ---
    public boolean deleteSupplier(int supplierId) {
        String sql = "DELETE FROM TBLSuppliers WHERE SupplierID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, supplierId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleSqlError(e, "deleting vendor");
            return false;
        }
    }

    // --- Existing ADD Method (Modified to return boolean) ---
    public boolean addSupplier(SupplierModel supplier) {
        // NOTE: SQL statement must match your TBLSuppliers schema
        String sql = "INSERT INTO TBLSuppliers (SupplierName, ContactNo, Email, Address, OpeningBalance, CreatedDate) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getContactNo());
            ps.setString(3, supplier.getEmail());
            ps.setString(4, supplier.getAddress());
            ps.setDouble(5, supplier.getOpeningBalance());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now())); // Use current time

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            handleSqlError(e, "saving vendor");
            return false;
        }
    }


    // --- Centralized Error Handler ---
    private void handleSqlError(SQLException e, String action) {
        String errorMessage = e.getMessage();
        System.err.println("Database error during " + action + ": " + errorMessage);

        if (errorMessage != null && errorMessage.contains("Cannot delete or update a parent row")) {
            JOptionPane.showMessageDialog(null,
                    "Operation Failed: This vendor has linked transactions and cannot be deleted.",
                    "Integrity Constraint Error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null,
                    "Database error while " + action + ": " + errorMessage,
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    public SupplierModel getSupplierById(int supplierId) {
        String sql = "SELECT SupplierID, SupplierName, ContactNo, Email, Address, OpeningBalance, CreatedDate FROM TBLSuppliers WHERE SupplierID = ?";
        SupplierModel supplier = null;
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    supplier = SupplierModel.builder()
                            .supplierID(rs.getInt("SupplierID"))
                            .supplierName(rs.getString("SupplierName"))
                            .contactNo(rs.getString("ContactNo"))
                            .email(rs.getString("Email"))
                            .address(rs.getString("Address"))
                            .openingBalance(rs.getDouble("OpeningBalance"))
                            .createdDate(rs.getTimestamp("CreatedDate").toLocalDateTime())
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching vendor: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return supplier;
    }
    /**
     * Fetches the current OutstandingBalance for a specific supplier.
     */
    public double getSupplierBalance(int supplierId) {
        String sql = "SELECT OpeningBalance FROM TBLSuppliers WHERE SupplierID = ?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("OpeningBalance");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching supplier balance: " + e.getMessage());
        }
        return 0.0;
    }
}
