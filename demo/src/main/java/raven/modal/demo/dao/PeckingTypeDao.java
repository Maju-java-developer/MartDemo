package raven.modal.demo.dao;

import raven.modal.demo.model.PeckingTypeModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PeckingTypeDao {

    // --- CREATE/ADD Method ---
    public void addPeckingType(PeckingTypeModel type) {
        String sql = "INSERT INTO TBLPeckingType (PeekingTypeName, quarterQty, IsActive) VALUES (?, ?, ?)";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type.getPeckingTypeName());
            ps.setInt(2, type.getQuarterQty());
            ps.setBoolean(3, type.isActive());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Pecking Type '" + type.getPeckingTypeName() + "' saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error saving Pecking Type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- READ/FETCH Single Record Method (For Edit Form) ---
    public PeckingTypeModel getPeckingTypeById(int typeId) {
        String sql = "SELECT PeekingTypeId, PeekingTypeName, quarterQty, IsActive FROM TBLPeckingType WHERE PeekingTypeId = ?";
        PeckingTypeModel type = null;
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, typeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    type = PeckingTypeModel.builder()
                            .peckingTypeId(rs.getInt("PeekingTypeId"))
                            .peckingTypeName(rs.getString("PeekingTypeName"))
                            .quarterQty(rs.getInt("quarterQty"))
                            .isActive(rs.getBoolean("IsActive"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching Pecking Type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return type;
    }

    public List<PeckingTypeModel> getAllPeckingTypes(int offset, int limit) {
        List<PeckingTypeModel> types = new ArrayList<>();
        String sql = "SELECT PeekingTypeId, PeekingTypeName, quarterQty, IsActive FROM TBLPeckingType WHERE IsActive = TRUE LIMIT ? OFFSET ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    types.add(PeckingTypeModel.builder()
                            .peckingTypeId(rs.getInt("PeekingTypeId"))
                            .peckingTypeName(rs.getString("PeekingTypeName"))
                            .quarterQty(rs.getInt("quarterQty"))
                            .isActive(rs.getBoolean("IsActive"))
                            .build());
                }
            }
        } catch (SQLException e) {
            // ... error handling ...
        }
        return types;
    }

    // --- READ/FETCH Count (For Pagination) ---
    public int getPeckingTypeCount() {
        String sql = "SELECT COUNT(*) FROM TBLPeckingType p where p.isActive = true";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error updating Pecking Type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return 0;
    }

    public void updatePeckingType(PeckingTypeModel type) {
        String sql = "UPDATE TBLPeckingType SET PeekingTypeName=?, quarterQty=?, IsActive=? WHERE PeekingTypeId=?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type.getPeckingTypeName());
            ps.setInt(2, type.getQuarterQty());
            ps.setBoolean(3, type.isActive());
            ps.setInt(4, type.getPeckingTypeId());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Pecking Type ID " + type.getPeckingTypeId() + " updated successfully!", "Update Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error updating Pecking Type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- DELETE Method ---
    public void deletePeckingType(int typeId) {
        String sql = "DELETE FROM TBLPeckingType WHERE PeekingTypeId = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, typeId);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Pecking Type ID " + typeId + " deleted successfully!", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Pecking Type ID " + typeId + " not found. No record was deleted.", "Warning", JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Cannot delete or update a parent row")) {
                JOptionPane.showMessageDialog(null,
                        "Deletion Failed: This Pecking Type is linked to other records and cannot be deleted.",
                        "Integrity Constraint Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Database error while deleting Pecking Type: " + errorMessage, "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    /**
     * Fetches only active PeekingType IDs and Names to populate a JComboBox.
     * Includes a placeholder item.
     * @return List of PeekingTypeModel containing only ID and Name.
     */
    public List<PeckingTypeModel> getActivePeekingTypesForDropdown() {
        String sql = "SELECT PeekingTypeId, PeekingTypeName FROM TBLPeckingType WHERE IsActive = TRUE ORDER BY PeekingTypeName";
        List<PeckingTypeModel> types = new ArrayList<>();

        // Add a placeholder/default item
        types.add(PeckingTypeModel.builder().peckingTypeId(0).peckingTypeName("--- Select Pecking Type ---").build());

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                types.add(PeckingTypeModel.builder()
                        .peckingTypeId(rs.getInt("PeekingTypeId"))
                        .peckingTypeName(rs.getString("PeekingTypeName"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching active peeking types: " + e.getMessage());
        }
        return types;
    }
}