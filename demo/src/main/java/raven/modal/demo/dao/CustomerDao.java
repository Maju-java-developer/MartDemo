package raven.modal.demo.dao;

import raven.modal.demo.model.CustomerModel;
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

    public int addCustomer(CustomerModel c) {
        String sql = "{ CALL SP_IUD_Customer(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setNull(1, java.sql.Types.INTEGER);
            cs.setString(2, c.getCustomerName());
            cs.setString(3, c.getContactNo());
            cs.setString(4, c.getEmail());
            cs.setString(5, c.getAddress());
            cs.setString(6, c.getCity());
            cs.setBoolean(7, true);
            cs.setInt(8, 1); // UserID todo UserId here later
            cs.setTimestamp(9, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(10, "Save");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error adding customer: " + e.getMessage());
        }

        return 0;
    }
    public int updateCustomer(CustomerModel c) {
        String sql = "{ CALL SP_IUD_Customer(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, c.getCustomerId());
            cs.setString(2, c.getCustomerName());
            cs.setString(3, c.getContactNo());
            cs.setString(4, c.getEmail());
            cs.setString(5, c.getAddress());
            cs.setString(6, c.getCity());
            cs.setBoolean(7, c.getIsActive()); // default customer is true
            cs.setInt(8, 1);
            cs.setTimestamp(9, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(10, "Update");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating customer: " + e.getMessage());
        }

        return 0;
    }
    public int deleteCustomer(int customerId) {
        String sql = "{ CALL SP_IUD_Customer(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, customerId);
            cs.setNull(2, java.sql.Types.VARCHAR);
            cs.setNull(3, java.sql.Types.VARCHAR);
            cs.setNull(4, java.sql.Types.VARCHAR);
            cs.setNull(5, java.sql.Types.VARCHAR);
            cs.setNull(6, java.sql.Types.VARCHAR);
            cs.setNull(7, java.sql.Types.BOOLEAN);
            cs.setInt(8, 1);
            cs.setTimestamp(9, new java.sql.Timestamp(System.currentTimeMillis()));
            cs.setString(10, "Delete");

            ResultSet rs = cs.executeQuery();
            if (rs.next()) return rs.getInt("Result");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error deleting customer: " + e.getMessage());
        }

        return 0;
    }
    public List<CustomerModel> getAllCustomers(int offset, int limit) {
        List<CustomerModel> customers = new ArrayList<>();

        // 🔴 CHANGE 1: Use the CALL syntax for the unified stored procedure
        String sql = "{CALL SP_GetList(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             // 🔴 CHANGE 2: Use CallableStatement
             CallableStatement cs = conn.prepareCall(sql)) {

            // --- Map SP Parameters ---
            cs.setInt(1, 0);                  // p_Id
            cs.setInt(2, limit);              // p_DisplayLength (Your limit)
            cs.setInt(3, offset);             // p_DisplayStart (Your offset)
            cs.setNull(4, java.sql.Types.VARCHAR); // p_Search (NULL)
            cs.setString(5, "CustomerList");  // p_ListBy (REQUIRED)
            cs.setInt(6, 0);                  // p_UserID
            cs.setNull(7, java.sql.Types.TIMESTAMP); // p_DateTime

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    customers.add(CustomerModel.builder()
                            .customerId(rs.getInt("CustomerID"))
                            .customerName(rs.getString("CustomerName"))
                            .contactNo(rs.getString("ContactNo"))
                            .address(rs.getString("Address"))
                            .email(rs.getString("Email"))
                            .openingBalance(rs.getDouble("OpeningBalance"))
                            .build());
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
        return customers;
    }

    public CustomerModel getCustomerById(int customerId) {
        String sql = "SELECT CustomerID, CustomerName, ContactNo, Email, Address, OpeningBalance, TaxPer, City FROM TBLCustomers WHERE CustomerID = ?";
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
                            .city(rs.getString("City"))
                            .build();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching customer: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return customer;
    }

}

