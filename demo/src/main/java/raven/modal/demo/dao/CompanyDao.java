package raven.modal.demo.dao;

import raven.modal.demo.model.CompanyModel;
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

public class CompanyDao {

    public int addCompany(CompanyModel company) {
        String sql = "{ CALL SP_IUD_Company(?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setNull(1, java.sql.Types.INTEGER); // CompanyID NULL for insert
            cs.setString(2, company.getCompanyName());
            cs.setBoolean(3, company.isActive());
            cs.setInt(4, Constants.getCurrentUserId());
            cs.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(6, "Save");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                return rs.getInt("Result"); // return SP result
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error while adding company: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        return 0;
    }

    // --- READ/FETCH Single Record Method (For Edit Form) ---
    public CompanyModel getCompanyById(int companyId) {
        // Select all fields, even if the form only displays two, to accurately represent the record.
        String sql = "SELECT CompanyID, CompanyName, IsActive FROM TBLCompanies WHERE CompanyID = ?";
        CompanyModel company = null;
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    company = CompanyModel.builder()
                            .companyId(rs.getInt("CompanyID"))
                            .companyName(rs.getString("CompanyName"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching company: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return company;
    }

    public List<CompanyModel> getAllCompanies(int offset, int limit) {
        List<CompanyModel> companies = new ArrayList<>();

        String sql = "{CALL SP_GetList(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, 0);                  // p_Id
            cs.setInt(2, limit);              // p_DisplayLength
            cs.setInt(3, offset);             // p_DisplayStart
            cs.setNull(4, java.sql.Types.VARCHAR); // p_Search
            cs.setString(5, "CompanyList");   // p_ListBy
            cs.setInt(6, 0);                  // p_UserID
            cs.setNull(7, java.sql.Types.TIMESTAMP); // p_DateTime

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    companies.add(CompanyModel.builder()
                            // Columns from the SP's 'CompanyList' branch
                            .companyId(rs.getInt("CompanyID"))
                            .companyName(rs.getString("CompanyName"))
                            .isActive(rs.getBoolean("IsActive"))
                            .build());
                }
            }
        } catch (SQLException e) {
            // ... error handling ...
        }
        return companies;
    }
    // --- READ/FETCH Count (For Pagination) ---
    public int getCompanyCount() {
        String sql = "SELECT COUNT(*) FROM TBLCompanies c where c.IsActive = true ";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            // ... error handling ...
        }
        return 0;
    }

    public int updateCompany(CompanyModel company) {
        String sql = "{ CALL SP_IUD_Company(?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, company.getCompanyId());
            cs.setString(2, company.getCompanyName());
            cs.setBoolean(3, company.isActive());
            cs.setInt(4, Constants.getCurrentUserId());
            cs.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(6, "Update");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                return rs.getInt("Result");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error while updating company: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        return 0;
    }


    public int deleteCompany(int companyId) {
        String sql = "{ CALL SP_IUD_Company(?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, companyId);
            cs.setNull(2, java.sql.Types.VARCHAR);
            cs.setNull(3, java.sql.Types.BOOLEAN);
            cs.setInt(4, Constants.getCurrentUserId());
            cs.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(6, "Delete");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                return rs.getInt("Result");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error while deleting company: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        return 0;
    }

    // --- HELPER METHOD: Get Active Companies for Dropdown ---
    /**
     * Fetches only active Company IDs and Names to populate a JComboBox.
     * @return List of CompanyModel containing only ID and Name.
     */
    public List<CompanyModel> getActiveCompaniesForDropdown() {
        // Assuming TBLCompanies has CompanyID, CompanyName, and IsActive columns
        String sql = "SELECT CompanyID, CompanyName FROM TBLCompanies WHERE IsActive = TRUE ORDER BY CompanyName";
        List<CompanyModel> companies = new ArrayList<>();

        // Add a placeholder/default item for the dropdown
        companies.add(CompanyModel.builder().companyId(0).companyName("--- Select Company ---").build());

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                companies.add(CompanyModel.builder()
                        .companyId(rs.getInt("CompanyID"))
                        .companyName(rs.getString("CompanyName"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching active companies: " + e.getMessage());
            // Optionally show a dialog, but often best to fail silently in helper methods
        }
        return companies;
    }

}
