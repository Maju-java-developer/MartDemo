package raven.modal.demo.dao;

import raven.modal.demo.model.CategoryModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    // --- CREATE/ADD Method ---
    public void addCategory(CategoryModel category) {
        String sql = "INSERT INTO TBLCategories (CategoryName, IsActive) VALUES (?, ?)";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category.getCategoryName());
            ps.setBoolean(2, category.isActive());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Category '" + category.getCategoryName() + "' saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error saving category: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
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

    // --- READ/FETCH All Records (For Table Panel) ---
    public List<CategoryModel> getAllCategories(int offset, int limit) {
        List<CategoryModel> categories = new ArrayList<>();
        String sql = "SELECT CategoryID, CategoryName, IsActive FROM TBLCategories where IsActive = true LIMIT ? OFFSET ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(CategoryModel.builder()
                            .categoryId(rs.getInt("CategoryID"))
                            .categoryName(rs.getString("CategoryName"))
                            .isActive(rs.getBoolean("IsActive"))
                            .build());
                }
            }
        } catch (SQLException e) {
            // ... error handling ...
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
            // ... error handling ...
        }
        return 0;
    }

    // --- UPDATE Method ---
    public void updateCategory(CategoryModel category) {
        String sql = "UPDATE TBLCategories SET CategoryName=?, IsActive=? WHERE CategoryID=?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category.getCategoryName());
            ps.setBoolean(2, category.isActive());
            ps.setInt(3, category.getCategoryId());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Category ID " + category.getCategoryId() + " updated successfully!", "Update Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error updating category: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- DELETE Method ---
    public void deleteCategory(int categoryId) {
        String sql = "DELETE FROM TBLCategories WHERE CategoryID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Category ID " + categoryId + " deleted successfully!", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Category ID " + categoryId + " not found. No record was deleted.", "Warning", JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Cannot delete or update a parent row")) {
                JOptionPane.showMessageDialog(null,
                        "Deletion Failed: This category is linked to existing products or records and cannot be deleted.",
                        "Integrity Constraint Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Database error while deleting category: " + errorMessage, "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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