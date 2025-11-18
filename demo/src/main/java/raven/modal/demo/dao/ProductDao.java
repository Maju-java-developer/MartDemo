package raven.modal.demo.dao;

import raven.modal.demo.model.ProductModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    // Inside ProductDao.java

    /**
     * Searches for active products by name or code, limited to 10 results.
     * Used for the auto-suggest feature in the purchase form.
     * @param query The search string.
     * @return List of ProductModel (ID, Code, Name, PeckingTypeId, UnitID).
     */
    public List<ProductModel> searchActiveProducts(String query) {
        // NOTE: Need to fetch PeckingTypeId and UnitID for quantity calculation
        String sql = "SELECT p.ProductID, p.ProductCode, p.ProductName, p.PackingTypeId, pt.cartonQty as UnitPerCarton " +
                "FROM TBLProducts p " +
                "JOIN TBLPackingType pt ON p.PackingTypeId = pt.PackingTypeId " + // Join to get units per carton
                "WHERE p.IsActive = TRUE AND (p.ProductName LIKE ? OR p.ProductCode LIKE ?) LIMIT 10";

        List<ProductModel> products = new ArrayList<>();
        String searchPattern = "%" + query + "%";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(ProductModel.builder()
                            .productId(rs.getInt("ProductID"))
                            .productCode(rs.getString("ProductCode"))
                            .productName(rs.getString("ProductName"))
                            .packingTypeId(rs.getInt("PeckingTypeId"))
                            .unitsPerCarton(rs.getInt("UnitPerCarton")) // Assumed field in ProductModel/PeckingType
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during product search: " + e.getMessage());
        }
        return products;
    }


    /**
     * Fetches ALL active products (used for initial load of the searchable JComboBox).
     * @return List of all active ProductModel objects (ID, Code, Name, UnitsPerCarton).
     */
    public List<ProductModel> getAllActiveProducts() {
        String sql = "SELECT p.ProductID, p.ProductCode, p.ProductName, p.PackingTypeId, pt.cartonQty as UnitPerCarton " +
                "FROM TBLProducts p " +
                "JOIN TBLPackingType pt ON p.PackingTypeId = pt.PackingTypeId " +
                "WHERE p.IsActive = TRUE ORDER BY p.ProductName ASC";

        List<ProductModel> products = new ArrayList<>();

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(ProductModel.builder()
                        .productId(rs.getInt("ProductID"))
                        .productCode(rs.getString("ProductCode"))
                        .productName(rs.getString("ProductName"))
                        .packingTypeId(rs.getInt("PackingTypeId"))
                        .unitsPerCarton(rs.getInt("UnitPerCarton"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching all products: " + e.getMessage());
        }
        return products;
    }
    /**
     * Retrieves a paginated list of products with related company, brand, category, and peeking type info.
     * @param offset The starting row index.
     * @param limit  The maximum number of rows to return.
     * @return A List of ProductModel objects.
     */
    public List<ProductModel> getAllProducts(int offset, int limit) {
        List<ProductModel> products = new ArrayList<>();

        // 🔴 CHANGE 1: Use the CALL syntax for the unified stored procedure
        String sql = "{CALL SP_GetList(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             // 🔴 CHANGE 2: Use CallableStatement
             CallableStatement cs = conn.prepareCall(sql)) {

            // --- Map SP Parameters ---
            cs.setInt(1, 0);                 // p_Id (Unused for list)
            cs.setInt(2, limit);             // p_DisplayLength (Your limit)
            cs.setInt(3, offset);            // p_DisplayStart (Your offset)
            cs.setNull(4, java.sql.Types.VARCHAR); // p_Search (NULL)
            cs.setString(5, "ProductList");  // p_ListBy (REQUIRED)
            cs.setInt(6, 0);                 // p_UserID (Unused)
            cs.setNull(7, java.sql.Types.TIMESTAMP); // p_DateTime (Unused)


            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    ProductModel product = ProductModel.builder()
                            .productId(rs.getInt("ProductID"))
                            .productCode(rs.getString("ProductCode"))
                            .productName(rs.getString("ProductName"))
                            // All these columns are returned by the SP's 'ProductList' branch
                            .companyName(rs.getString("CompanyName"))
                            .brandName(rs.getString("BrandTitle")) // Note: BrandTitle is returned, not BrandName
                            .categoryName(rs.getString("CategoryName"))
                            .peckingTypeName(rs.getString("PackingTypeName"))
                            .build();
                    products.add(product);
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
        return products;
    }

    /**
     * Inserts a new product record into TBLProducts.
     * @param product The ProductModel to save.
     */
    public void addProduct(ProductModel product) {
        // SQL must match the 7 fields being inserted (excluding ProductID)
        String sql = "INSERT INTO TBLProducts (ProductCode, ProductName, IsActive, BrandId, CategoryId, PackingTypeId, CompanyId) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1. Set the parameters from the ProductModel (7 parameters)
            ps.setString(1, product.getProductCode());
            ps.setString(2, product.getProductName());
            ps.setBoolean(3, product.isActive());
            ps.setInt(4, product.getBrandId());
            ps.setInt(5, product.getCategoryId());
            ps.setInt(6, product.getPackingTypeId());
            ps.setInt(7, product.getCompanyId());

            // 2. Execute the update
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                // Success feedback
                JOptionPane.showMessageDialog(null,
                        "Product '" + product.getProductName() + "' saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Product not saved. No rows were affected.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            handleSqlError(e, "saving product");
        }
    }

    // --- UPDATE Method ---
    /**
     * Updates an existing product record in TBLProducts.
     * @param product The ProductModel with updated data and existing ProductID.
     */
    public void updateProduct(ProductModel product) {
        // SQL must update 7 fields, filtering by ProductID (8 parameters)
        String sql = "UPDATE TBLProducts SET ProductCode=?, ProductName=?, IsActive=?, BrandId=?, CategoryId=?, PackingTypeId=?, CompanyId=? "
                + "WHERE ProductID=?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1. Set the update parameters
            ps.setString(1, product.getProductCode());
            ps.setString(2, product.getProductName());
            ps.setBoolean(3, product.isActive());
            ps.setInt(4, product.getBrandId());
            ps.setInt(5, product.getCategoryId());
            ps.setInt(6, product.getPackingTypeId());
            ps.setInt(7, product.getCompanyId());

            // 2. Set the WHERE clause ID
            ps.setInt(8, product.getProductId());

            // 3. Execute the update
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null,
                        "Product '" + product.getProductName() + "' updated successfully!",
                        "Update Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Product ID " + product.getProductId() + " not found. No record was updated.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            handleSqlError(e, "updating product");
        }
    }

    // --- READ/FETCH Single Record Method ---
    /**
     * Fetches a single product record by ID.
     * @param productId The ID of the product to fetch.
     * @return The ProductModel or null if not found.
     */
    public ProductModel getProductById(int productId) {
        String sql = "SELECT ProductID, ProductCode, ProductName, IsActive, BrandId, CategoryId, PackingTypeId, CompanyId "
                + "FROM TBLProducts WHERE ProductID = ?";
        ProductModel product = null;

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    product = ProductModel.builder()
                            .productId(rs.getInt("ProductID"))
                            .productCode(rs.getString("ProductCode"))
                            .productName(rs.getString("ProductName"))
                            .isActive(rs.getBoolean("IsActive"))
                            .brandId(rs.getInt("BrandId"))
                            .categoryId(rs.getInt("CategoryId"))
                            .packingTypeId(rs.getInt("PackingTypeId"))
                            .companyId(rs.getInt("CompanyId"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching product: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return product;
    }

    // --- Centralized Error Handler ---
    private void handleSqlError(SQLException e, String action) {
        String errorMessage = e.getMessage();
        System.err.println("Database error during " + action + ": " + errorMessage);

        // Check for specific unique constraint violation error (ProductCode is UNIQUE)
        if (errorMessage != null && errorMessage.contains("Duplicate entry") && errorMessage.contains("ProductCode")) {
            JOptionPane.showMessageDialog(null,
                    "Error: The Product Code provided is already in use.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        // Check for Foreign Key constraint violation (e.g., trying to save with a non-existent BrandId)
        else if (errorMessage != null && errorMessage.contains("Cannot add or update a child row")) {
            JOptionPane.showMessageDialog(null,
                    "Error: One or more selected linked items (Company, Brand, Category, Peeking Type) is invalid or non-existent.",
                    "Data Integrity Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        else {
            // General database error
            JOptionPane.showMessageDialog(null,
                    "Database error while " + action + ": " + errorMessage,
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteProductById(int productId) {
        String sql = "DELETE FROM tblproducts WHERE ProductID = ? ";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null,
                        "Product ID " + productId + " deleted successfully!",
                        "Deletion Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Product ID " + productId + " not found. No record was deleted.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            String errorMessage = e.getMessage();

            if (errorMessage != null && errorMessage.contains("Cannot delete or update a parent row")) {
                JOptionPane.showMessageDialog(null,
                        "Deletion Failed: This Product is linked to existing products or records and cannot be deleted.",
                        "Integrity Constraint Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Database error while deleting brand: " + errorMessage,
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}