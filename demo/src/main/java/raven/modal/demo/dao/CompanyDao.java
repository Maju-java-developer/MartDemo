package raven.modal.demo.dao;

import raven.modal.demo.model.CompanyModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CompanyDao {

    // --- CREATE/ADD Method (Handles only required fields from simplified form) ---
    public void addCompany(CompanyModel company) {
        // Only insert the two fields the simplified form provides.
        // Other columns (ContactNo, Email, Address) must be nullable in the DB.
        String sql = "INSERT INTO TBLCompanies (CompanyName, IsActive) VALUES (?, ?)";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, company.getCompanyName());
            ps.setBoolean(2, company.isActive());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Company '" + company.getCompanyName() + "' saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error saving company: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
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

    // --- READ/FETCH All Records (For Table Panel) ---
    public List<CompanyModel> getAllCompanies(int offset, int limit) {
        List<CompanyModel> companies = new ArrayList<>();
        // Select only the columns needed for the simplified table view (ID, Name, Status)
        String sql = "SELECT c.CompanyID, c.CompanyName, c.IsActive FROM TBLCompanies" +
                " c where c.IsActive = true LIMIT ? OFFSET ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    companies.add(CompanyModel.builder()
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

    // --- UPDATE Method (Only updates the two fields the simplified form handles) ---
    public void updateCompany(CompanyModel company) {
        String sql = "UPDATE TBLCompanies SET CompanyName=?, IsActive=? WHERE CompanyID=?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, company.getCompanyName());
            ps.setBoolean(2, company.isActive());
            ps.setInt(3, company.getCompanyId());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Company ID " + company.getCompanyId() + " updated successfully!", "Update Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error updating company: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Deletes a company record from the TBLCompanies table by ID.
     * @param companyId The ID of the company to delete.
     */
    public void deleteCompany(int companyId) {
        String sql = "DELETE FROM TBLCompanies WHERE CompanyID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, companyId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                // Success feedback
                JOptionPane.showMessageDialog(null,
                        "Company ID " + companyId + " deleted successfully!",
                        "Deletion Success",
                        JOptionPane.INFORMATION_MESSAGE);
                System.out.println("DAO: Company ID " + companyId + " deleted successfully.");
            } else {
                // Warning if no row was found (e.g., already deleted)
                JOptionPane.showMessageDialog(null,
                        "Company ID " + companyId + " not found. No record was deleted.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            String errorMessage = e.getMessage();

            // Check for Foreign Key Constraint violation (most common delete error)
            if (errorMessage != null && errorMessage.contains("Cannot delete or update a parent row")) {
                JOptionPane.showMessageDialog(null,
                        "Deletion Failed: This company is linked to existing transactions or records (e.g., invoices, products, or users) and cannot be deleted.",
                        "Integrity Constraint Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                // General database error
                JOptionPane.showMessageDialog(null,
                        "Database error while deleting company: " + errorMessage,
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            System.err.println("Database error during deleteCompany: " + errorMessage);
        }
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
