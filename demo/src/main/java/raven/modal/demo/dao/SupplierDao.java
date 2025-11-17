package raven.modal.demo.dao;

import raven.modal.demo.model.SupplierModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.CallableStatement;
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
        suppliers.add(SupplierModel.builder().supplierID(0).supplierName("--- Select Vendor ---").build());

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

    public int updateSupplier(SupplierModel supplier) {
        String sql = "{ CALL SP_IUD_Vendor(?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, supplier.getSupplierID());
            cs.setString(2, supplier.getSupplierName());
            cs.setString(3, supplier.getContactNo());
            cs.setString(4, supplier.getAddress());
            cs.setString(5, supplier.getEmail());
            cs.setDouble(6, supplier.getOpeningBalance());
            cs.setInt(7, 1);
            cs.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            cs.setString(9, "Update");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                return rs.getInt("Result");
            }

        } catch (SQLException e) {
            handleSqlError(e, "updating vendor");
        }
        return 0;
    }

    public int deleteSupplier(int supplierId) {
        String sql = "{ CALL SP_IUD_Vendor(?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, supplierId);
            cs.setNull(2, java.sql.Types.VARCHAR);
            cs.setNull(3, java.sql.Types.VARCHAR);
            cs.setNull(4, java.sql.Types.VARCHAR);
            cs.setNull(5, java.sql.Types.VARCHAR);
            cs.setNull(6, java.sql.Types.DOUBLE);
            cs.setInt(7, 1);
            cs.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            cs.setString(9, "Delete");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                return rs.getInt("Result");
            }

        } catch (SQLException e) {
            handleSqlError(e, "deleting vendor");
        }
        return 0;
    }

    public int addSupplier(SupplierModel supplier) {
        String sql = "{ CALL SP_IUD_Vendor(?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setNull(1, java.sql.Types.INTEGER); // VendorID for save
            cs.setString(2, supplier.getSupplierName());
            cs.setString(3, supplier.getContactNo());
            cs.setString(4, supplier.getAddress());
            cs.setString(5, supplier.getEmail());
            cs.setDouble(6, supplier.getOpeningBalance());
            cs.setInt(7, 1);                     // UserID
            cs.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            cs.setString(9, "Save");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                return rs.getInt("Result");  // returns new ID or -3 (duplicate)
            }

        } catch (SQLException e) {
            handleSqlError(e, "adding vendor");
        }
        return 0;
    }

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
