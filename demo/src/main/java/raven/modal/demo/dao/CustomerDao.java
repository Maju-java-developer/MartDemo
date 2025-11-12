package raven.modal.demo.dao;

import raven.modal.demo.model.CustomerModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {

    public List<CustomerModel> getActiveCustomersForDropdown() {
        String sql = "SELECT CustomerID, CustomerName FROM tblcustomers ORDER BY CustomerID";
        List<CustomerModel> customers = new ArrayList<>();

        // Add a placeholder/default item
        customers.add(CustomerModel.builder().customerId(0).customerName("--- Select Customer ---").build());

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                customers.add(CustomerModel.builder()
                        .customerId(rs.getInt("CustomerID"))
                        .customerName(rs.getString("CustomerName"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching active Customer: " + e.getMessage());
        }
        return customers;
    }

    public boolean addCustomer(CustomerModel customer) {
        String sql = "INSERT INTO tblcustomers (CustomerName, ContactNo, Address, Email, OpeningBalance, TaxPer, CreatedDate) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getContactNo());
            ps.setString(3, customer.getAddress());
            ps.setString(4, customer.getEmail());
            ps.setDouble(5, customer.getOpeningBalance());
            ps.setDouble(6, customer.getTaxPer());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<CustomerModel> getAllCustomers(int offset, int limit) {
        List<CustomerModel> list = new ArrayList<>();
        String sql = "SELECT * FROM tblcustomers ORDER BY CustomerId LIMIT ? OFFSET ?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CustomerModel customer = new CustomerModel();
                customer.setCustomerId(rs.getInt("CustomerId"));
                customer.setCustomerName(rs.getString("CustomerName"));
                customer.setContactNo(rs.getString("ContactNo"));
                customer.setAddress(rs.getString("address"));
                customer.setEmail(rs.getString("email"));
                customer.setOpeningBalance(rs.getDouble("OpeningBalance"));
                customer.setTaxPer(rs.getDouble("TaxPer"));
                list.add(customer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // --- READ/FETCH Single Record Method (For Edit Form) ---
    public CustomerModel getCustomerById(int customerId) {
        // NOTE: Adjust column names if your table schema differs
        String sql = "SELECT CustomerID, CustomerName, ContactNo, Email, Address, OpeningBalance, TaxPer FROM TBLCustomers WHERE CustomerID = ?";
        CustomerModel customer = null;
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Assuming CustomerModel uses Lombok Builder
                    customer = CustomerModel.builder()
                            .customerId(rs.getInt("CustomerID"))
                            .customerName(rs.getString("CustomerName"))
                            .contactNo(rs.getString("ContactNo"))
                            .email(rs.getString("Email"))
                            .address(rs.getString("Address"))
                            .openingBalance(rs.getDouble("OpeningBalance"))
                            .taxPer(rs.getDouble("TaxPer"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching customer: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return customer;
    }

    // --- UPDATE Method ---
    public boolean updateCustomer(CustomerModel customer) {
        String sql = "UPDATE TBLCustomers SET CustomerName=?, ContactNo=?, Email=?, Address=?, OpeningBalance=?, TaxPer=? WHERE CustomerID=?";
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getContactNo());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setDouble(5, customer.getOpeningBalance());
            ps.setDouble(6, customer.getTaxPer());
            ps.setInt(7, customer.getCustomerId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            handleSqlError(e, "updating customer");
            return false;
        }
    }

    // --- DELETE Method ---
    public boolean deleteCustomer(int customerId) {
        String sql = "DELETE FROM TBLCustomers WHERE CustomerID = ?";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleSqlError(e, "deleting customer");
            return false;
        }
    }

    // --- Helper for handling SQL errors (Ensure this is available in your DAO) ---
    private void handleSqlError(SQLException e, String action) {
        String errorMessage = e.getMessage();
        System.err.println("Database error during " + action + ": " + errorMessage);

        if (errorMessage != null && errorMessage.contains("Cannot delete or update a parent row")) {
            JOptionPane.showMessageDialog(null,
                    "Deletion Failed: This customer has linked transactions and cannot be deleted.",
                    "Integrity Constraint Error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null,
                    "Database error while " + action + ": " + errorMessage,
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

