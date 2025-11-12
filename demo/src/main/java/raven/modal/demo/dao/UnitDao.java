package raven.modal.demo.dao;

import raven.modal.demo.model.UnitModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnitDao {

    public boolean addUnit(UnitModel unit) {
        String sql = "INSERT INTO TBLUnits (UnitName) VALUES (?)";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, unit.getUnitName());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(null,
                        "Unit has been " + unit.getUnitName() + " added successfully!",
                        "Update Success",
                        JOptionPane.INFORMATION_MESSAGE);
                System.out.println("DAO: Unit added successfully: " + unit.getUnitName());
                return true;
            } else {
                JOptionPane.showMessageDialog(null,
                        "Unit added failed. Unit ID " + unit.getUnitID() + " not found or no changes made.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error while adding unit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public List<UnitModel> getAllUnits(int offset, int limit) {
        List<UnitModel> list = new ArrayList<>();
        String sql = "SELECT * FROM TBLUnits ORDER BY UnitID LIMIT ? OFFSET ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UnitModel unit = new UnitModel();
                    unit.setUnitID(rs.getInt("UnitID"));
                    unit.setUnitName(rs.getString("UnitName"));
                    list.add(unit);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    public Map<String, Integer> getAllUnits() {
        Map<String, Integer> unitMap = new HashMap<>();
        String sql = "SELECT * FROM TBLUnits ORDER BY UnitID";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    unitMap.put(rs.getString("UnitName"), rs.getInt("UnitID"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return unitMap;
    }


    /**
     * Retrieves a single unit by its ID, used to populate the edit form.
     * @param unitId The ID of the unit to fetch.
     * @return A UnitModel object, or null if not found or an error occurs.
     */
    public UnitModel getUnitById(int unitId) {
        String sql = "SELECT UnitID, UnitName FROM TBLUnits WHERE UnitID = ?";
        UnitModel unit = null;

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, unitId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    unit = UnitModel.builder()
                            .unitID(rs.getInt("UnitID"))
                            .unitName(rs.getString("UnitName"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error while fetching Unit ID " + unitId + ": " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            System.err.println("Database error during getUnitById: " + e.getMessage());
            // Return null on failure
        }
        return unit;
    }

    /**
     * Updates an existing unit in the TBLUnits table.
     * @param unit The UnitModel object containing the updated data and the UnitID.
     */
    public void updateUnit(UnitModel unit) {
        String sql = "UPDATE TBLUnits SET UnitName = ? WHERE UnitID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, unit.getUnitName());
            ps.setInt(2, unit.getUnitID());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null,
                        "Unit ID " + unit.getUnitID() + " updated successfully!",
                        "Update Success",
                        JOptionPane.INFORMATION_MESSAGE);
                System.out.println("DAO: Unit updated successfully: " + unit.getUnitName());
            } else {
                JOptionPane.showMessageDialog(null,
                        "Unit update failed. Unit ID " + unit.getUnitID() + " not found or no changes made.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            String errorMessage = e.getMessage();

            // Check for unique constraint violation on UnitName (if defined in DB)
            if (errorMessage != null && errorMessage.contains("Duplicate entry")) {
                JOptionPane.showMessageDialog(null,
                        "Error: The Unit Name '" + unit.getUnitName() + "' is already in use.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                // General database error
                JOptionPane.showMessageDialog(null,
                        "Database error while updating unit: " + errorMessage,
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            System.err.println("Database error during updateUnit: " + errorMessage);
        }
    }

    /**
     * Deletes a unit by its ID, if it is not referenced in other tables.
     * @param unitId The ID of the unit to delete.
     * @return true if the unit was deleted successfully, false otherwise.
     */
    public boolean deleteUnitById(int unitId) {
        String checkSql = "SELECT COUNT(*) FROM TBLProducts WHERE UnitID = ?"; // Example referencing table
        String deleteSql = "DELETE FROM TBLUnits WHERE UnitID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection()) {

            // Step 1: Check if UnitID is used in another table
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, unitId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(null,
                                "Cannot delete Unit ID " + unitId +
                                        " because it is being used in another table.",
                                "Delete Failed",
                                JOptionPane.WARNING_MESSAGE);
                        return false;
                    }
                }
            }

            // Step 2: Delete from TBLUnits
            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                deletePs.setInt(1, unitId);
                int rowsAffected = deletePs.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null,
                            "Unit ID " + unitId + " deleted successfully.",
                            "Delete Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    return true;
                } else {
                    JOptionPane.showMessageDialog(null,
                            "No unit found with ID " + unitId + ".",
                            "Delete Failed",
                            JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }

        } catch (SQLException e) {
            // If it’s a foreign key violation (MySQL error code 1451)
            if (e.getErrorCode() == 1451) {
                JOptionPane.showMessageDialog(null,
                        "Cannot delete Unit ID " + unitId + " because it is referenced in another table.",
                        "Foreign Key Constraint",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Database error while deleting Unit ID " + unitId + ": " + e.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            System.err.println("Database error during deleteUnit: " + e.getMessage());
            return false;
        }
    }

}

