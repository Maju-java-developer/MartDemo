package raven.modal.demo.dao;

import raven.modal.demo.model.BrandModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BrandDao {

    // --- CREATE/ADD Method ---
    public int addBrand(BrandModel brand) {
        String sql = "{ CALL SP_IUD_Brand(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setNull(1, java.sql.Types.INTEGER);
            cs.setString(2, brand.getBrandTitle());
            cs.setInt(3, brand.getCompanyId());
            cs.setBoolean(4, brand.isActive());
            cs.setInt(5, 1);
            cs.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(7, "Save");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error adding brand: " + e.getMessage());
        }

        return 0;
    }


    // --- READ/FETCH Single Record Method (For Edit Form) ---
    public BrandModel getBrandById(int brandId) {
        String sql = "SELECT BrandId, BrandTitle, b.CompanyId, c.CompanyName, b.IsActive FROM TBLBrands b join tblcompanies c WHERE BrandId = ?";
        BrandModel brand = null;
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, brandId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    brand = BrandModel.builder()
                            .brandId(rs.getInt("BrandId"))
                            .brandTitle(rs.getString("BrandTitle"))
                            .companyId(rs.getInt("CompanyId"))
                            .isActive(rs.getBoolean("IsActive"))
                            .companyName(rs.getString("CompanyName"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching brand: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return brand;
    }

    public int updateBrand(BrandModel brand) {
        String sql = "{ CALL SP_IUD_Brand(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, brand.getBrandId());
            cs.setString(2, brand.getBrandTitle());
            cs.setInt(3, brand.getCompanyId());
            cs.setBoolean(4, brand.isActive());
            cs.setInt(5, 1);
            cs.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(7, "Update");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating brand: " + e.getMessage());
        }

        return 0;
    }
    /**
     * Fetches paginated brand data along with the associated Company Name.
     * @param offset The starting index for pagination.
     * @param limit The maximum number of records to return.
     * @return List of Object[] where each array is {BrandId, BrandTitle, CompanyName, IsActive}.
     */
    public List<BrandModel> getBrandsWithCompanyName(int offset, int limit) {
        List<BrandModel> brandData = new ArrayList<>();

        // SQL JOIN query to link Brand data with the corresponding Company's name
        String sql = "SELECT b.BrandId, b.BrandTitle, c.CompanyName, b.IsActive " +
                "FROM TBLBrands b " +
                "JOIN TBLCompanies c ON b.CompanyId = c.CompanyID " +
                "ORDER BY b.BrandTitle " +
                "LIMIT ? OFFSET ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Map results to an Object array for the JTable model
                    brandData.add(new BrandModel(
                            rs.getInt("BrandId"),
                            rs.getString("BrandTitle"),
                            rs.getString("CompanyName"),
                            rs.getBoolean("IsActive"))
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching brands with company name: " + e.getMessage());
            // In a real application, proper logging would be used here.
        }
        return brandData;
    }

    /**
     * Counts the total number of brand records for pagination.
     * @return The total count of brands.
     */
    public int getBrandCount() {
        String sql = "SELECT COUNT(*) FROM TBLBrands";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Database error counting brands: " + e.getMessage());
        }
        return 0;
    }

    // --- DELETE Method (Completing the CRUD requirement) ---
    /**
     * Deletes a brand record from the TBLBrands table by ID.
     * @param brandId The ID of the brand to delete.
     */
    public int deleteBrand(int brandId) {
        String sql = "{ CALL SP_IUD_Brand(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, brandId);
            cs.setNull(2, java.sql.Types.VARCHAR);
            cs.setNull(3, java.sql.Types.INTEGER);
            cs.setNull(4, java.sql.Types.BOOLEAN);
            cs.setInt(5, 1);
            cs.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(7, "Delete");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error deleting brand: " + e.getMessage());
        }

        return 0;
    }
    /**
     * Fetches Brand IDs and Titles that belong to a specific Company ID.
     * Includes a placeholder item if no specific CompanyId is provided (id=0).
     * @param companyId The ID of the company to filter by.
     * @return List of BrandModel containing ID and Title.
     */
    public List<BrandModel> getBrandsByCompanyId(int companyId) {
        List<BrandModel> brands = new ArrayList<>();

        // Always add a placeholder/default item first
        brands.add(BrandModel.builder().brandId(0).brandTitle("--- Select Brand ---").build());

        if (companyId <= 0) {
            // Return only the placeholder if no valid company is selected/provided
            return brands;
        }

        // Fetch brands where IsActive is TRUE AND matches the CompanyId
        String sql = "SELECT BrandId, BrandTitle FROM TBLBrands WHERE IsActive = TRUE AND CompanyId = ? ORDER BY BrandTitle";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    brands.add(BrandModel.builder()
                            .brandId(rs.getInt("BrandId"))
                            .brandTitle(rs.getString("BrandTitle"))
                            .companyId(companyId) // Set the companyId just for consistency
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching brands by company ID: " + e.getMessage());
        }
        return brands;
    }
}
