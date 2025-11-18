package raven.modal.demo.dao;

import raven.modal.demo.model.PackingTypeModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PackingTypeDao {

    public int addPackingType(PackingTypeModel type) {
        String sql = "{ CALL SP_IUD_PackingType(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setNull(1, java.sql.Types.INTEGER);
            cs.setString(2, type.getPackingTypeName());      // title
            cs.setInt(3, type.getCartonQty());                // cartonQty
            cs.setBoolean(4, type.isActive());
            cs.setInt(5, 1);
            cs.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(7, "Save");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error saving packing type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        return 0;
    }
    public int updatePackingType(PackingTypeModel type) {
        String sql = "{ CALL SP_IUD_PackingType(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, type.getPackingTypeId());
            cs.setString(2, type.getPackingTypeName());
            cs.setInt(3, type.getCartonQty());
            cs.setBoolean(4, type.isActive());
            cs.setInt(5, 1);
            cs.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(7, "Update");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error updating packing type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        return 0;
    }
    public int deletePackingType(int id) {
        String sql = "{ CALL SP_IUD_PackingType(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, id);
            cs.setNull(2, java.sql.Types.VARCHAR);
            cs.setNull(3, java.sql.Types.INTEGER);
            cs.setNull(4, java.sql.Types.BOOLEAN);
            cs.setInt(5, 1);
            cs.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(7, "Delete");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error deleting packing type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        return 0;
    }
    public PackingTypeModel getPackingTypeById(int typeId) {
        String sql = "SELECT PackingTypeId, PackingTypeName, cartonQty, IsActive FROM TBLPackingType WHERE PackingTypeId = ?";
        PackingTypeModel type = null;
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, typeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    type = PackingTypeModel.builder()
                            .packingTypeId(rs.getInt("PackingTypeId"))
                            .packingTypeName(rs.getString("PackingTypeName"))
                            .cartonQty(rs.getInt("cartonQty"))
                            .isActive(rs.getBoolean("IsActive"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching Packing Type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return type;
    }

    public List<PackingTypeModel> getAllPackingTypes(int offset, int limit) {
        List<PackingTypeModel> types = new ArrayList<>();

        // 🔴 CHANGE 1: Use the CALL syntax for the unified stored procedure
        String sql = "{CALL SP_GetList(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             // 🔴 CHANGE 2: Use CallableStatement
             CallableStatement cs = conn.prepareCall(sql)) {

            // --- Map SP Parameters ---
            cs.setInt(1, 0);                  // p_Id
            cs.setInt(2, limit);              // p_DisplayLength
            cs.setInt(3, offset);             // p_DisplayStart
            cs.setNull(4, java.sql.Types.VARCHAR); // p_Search
            cs.setString(5, "PackingTypeList");// p_ListBy
            cs.setInt(6, 0);                  // p_UserID
            cs.setNull(7, java.sql.Types.TIMESTAMP); // p_DateTime

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    types.add(PackingTypeModel.builder()
                            .packingTypeId(rs.getInt("PackingTypeId"))
                            .packingTypeName(rs.getString("PackingTypeName"))
                            .cartonQty(rs.getInt("cartonQty"))
                            .isActive(rs.getBoolean("IsActive"))
                            .build());
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
        return types;
    }

    public int getPackingTypeCount() {
        String sql = "SELECT COUNT(*) FROM TBLPackingType p where p.isActive = true";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error updating Packing Type: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return 0;
    }

    /**
     * Fetches only active PeekingType IDs and Names to populate a JComboBox.
     * Includes a placeholder item.
     * @return List of PeekingTypeModel containing only ID and Name.
     */
    public List<PackingTypeModel> getActivePackingTypesForDropdown() {
        String sql = "SELECT PackingTypeId, PackingTypeName FROM TBLPackingType WHERE IsActive = TRUE ORDER BY PackingTypeName";
        List<PackingTypeModel> types = new ArrayList<>();

        // Add a placeholder/default item
        types.add(PackingTypeModel.builder().packingTypeId(0).packingTypeName("--- Select Packing Type ---").build());

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                types.add(PackingTypeModel.builder()
                        .packingTypeId(rs.getInt("PackingTypeId"))
                        .packingTypeName(rs.getString("PackingTypeName"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching active peeking types: " + e.getMessage());
        }
        return types;
    }
}