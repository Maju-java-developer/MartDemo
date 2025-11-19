package raven.modal.demo.dao;

import raven.modal.demo.model.CategoryModel;
import raven.modal.demo.mysql.MySQLConnection;
import raven.modal.demo.utils.Constants;

import javax.swing.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {
    public int addCategory(CategoryModel category) {
        String sql = "{ CALL SP_IUD_Category(?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setNull(1, java.sql.Types.INTEGER);
            cs.setString(2, category.getCategoryName());
            cs.setBoolean(3, category.isActive());
            cs.setInt(4, Constants.getCurrentUserId()); // updated user id with currentId
            cs.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(6, "Save");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error adding category: " + e.getMessage());
        }
        return 0;
    }
    public int updateCategory(CategoryModel category) {
        String sql = "{ CALL SP_IUD_Category(?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, category.getCategoryId());
            cs.setString(2, category.getCategoryName());
            cs.setBoolean(3, category.isActive());
            cs.setInt(4, Constants.getCurrentUserId());
            cs.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(6, "Update");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error updating category: " + e.getMessage());
        }
        return 0;
    }
    public int deleteCategory(int categoryId) {
        String sql = "{ CALL SP_IUD_Category(?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, categoryId);
            cs.setNull(2, java.sql.Types.VARCHAR);
            cs.setNull(3, java.sql.Types.BOOLEAN);
            cs.setInt(4, Constants.getCurrentUserId());
            cs.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(6, "Delete");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error deleting category: " + e.getMessage());
        }
        return 0;
    }

    // --- READ/FETCH Single Record Method (For Edit Form) ---
    public CategoryModel getCategoryById(int categoryId) {
        String sql = "SELECT CategoryID, CategoryName, IsActive FROM TBLCategories WHERE CategoryID = ?";
        CategoryModel category = null;
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    category = CategoryModel.builder()
                            .categoryId(rs.getInt("CategoryID"))
                            .categoryName(rs.getString("CategoryName"))
                            .isActive(rs.getBoolean("IsActive"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching category: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return category;
    }

    public List<CategoryModel> getAllCategories(int offset, int limit) {
        List<CategoryModel> categories = new ArrayList<>();

        // 🔴 CHANGE 1: Use the CALL syntax for the unified stored procedure
        String sql = "{CALL SP_GetList(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             // 🔴 CHANGE 2: Use CallableStatement
             CallableStatement cs = conn.prepareCall(sql)) {

            // --- Map SP Parameters ---
            cs.setInt(1, 0);                 // p_Id (Unused for list)
            cs.setInt(2, limit);             // p_DisplayLength (Your limit)
            cs.setInt(3, offset);            // p_DisplayStart (Your offset)
            cs.setNull(4, java.sql.Types.VARCHAR); // p_Search (NULL since no search logic in Java method)
            cs.setString(5, "CategoryList"); // p_ListBy (REQUIRED)
            cs.setInt(6, 0);                 // p_UserID (Unused)
            cs.setNull(7, java.sql.Types.TIMESTAMP); // p_DateTime (Unused)

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    categories.add(CategoryModel.builder()
                            // The columns remain the same
                            .categoryId(rs.getInt("CategoryID"))
                            .categoryName(rs.getString("CategoryName"))
                            .isActive(rs.getBoolean("IsActive"))
                            .build());
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
        return categories;
    }

    // --- READ/FETCH Count (For Pagination) ---
    public int getCategoryCount() {
        String sql = "SELECT COUNT(*) FROM TBLCategories where IsActive = true";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Fetches only active Category IDs and Names to populate a JComboBox.
     * Includes a placeholder item.
     * @return List of CategoryModel containing only ID and Name.
     */
    public List<CategoryModel> getActiveCategoriesForDropdown() {
        String sql = "SELECT CategoryID, CategoryName FROM TBLCategories WHERE IsActive = TRUE ORDER BY CategoryName";
        List<CategoryModel> categories = new ArrayList<>();

        // Add a placeholder/default item
        categories.add(CategoryModel.builder().categoryId(0).categoryName("--- Select Category ---").build());

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(CategoryModel.builder()
                        .categoryId(rs.getInt("CategoryID"))
                        .categoryName(rs.getString("CategoryName"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching active categories: " + e.getMessage());
        }
        return categories;
    }
}