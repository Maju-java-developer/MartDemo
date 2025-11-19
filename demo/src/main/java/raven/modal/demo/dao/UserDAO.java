package raven.modal.demo.dao;

import lombok.Getter;
import raven.modal.demo.model.UserModel;
import raven.modal.demo.model.User;
import raven.modal.demo.mysql.MySQLConnection;
import raven.modal.demo.utils.HashUtils;

import javax.swing.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
public class UserDAO {

    /**
     * Authenticates a user by calling the MySQL Stored Procedure SP_Auth_LoginUser.
     * Hashing is performed in Java before calling the procedure.
     * * @param userName The username provided by the user.
     * @param password The raw password provided by the user.
     * @return ModelUser object if credentials are valid, otherwise null.
     */
    public static UserModel authenticateUser(String userName, String password) {
        UserModel user = null;

        // 1. Hash the input password in Java (This hash is passed to the SP for matching)
        // NOTE: Ensure HashUtils.hashPassword(password) is available and uses the same algorithm as used during user registration.
        String inputPasswordHash = HashUtils.hashPassword(password);

        // 2. SQL to call the stored procedure
        String storedProcCall = "{CALL SP_Auth_LoginUser(?, ?, ?)}";

        // Get the current date/time for the LastLogin update in the database
        Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());

        try (Connection conn = MySQLConnection.getInstance().getConnection()) {

            // Validate the connection
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                JOptionPane.showMessageDialog(null, "Connection is closed or invalid.");
                throw new SQLException("Connection is closed or invalid.");
            }

            // 3. Use CallableStatement to execute the SP
            try (CallableStatement cs = conn.prepareCall(storedProcCall)) {

                // Set IN parameters for the SP
                cs.setString(1, userName);
                cs.setString(2, inputPasswordHash);
                cs.setTimestamp(3, currentTimestamp);

                // Execute the procedure (it returns a ResultSet with user data on success)
                try (ResultSet rs = cs.executeQuery()) {

                    // 4. Check if the SP returned a user record
                    if (rs.next()) {
                        // Create the UserModel object from the retrieved data
                        int userID = rs.getInt("UserID");
                        String retrievedUserName = rs.getString("Username");
                        String fullName = rs.getString("FullName");
                        String email = rs.getString("Email");
                        String contactNo = rs.getString("ContactNo");
                        String roleString = rs.getString("Role");
                        UserModel.Role role = UserModel.Role.fromString(roleString);
                        Boolean isActive = rs.getBoolean("isActive");
                        Boolean isBlocked = rs.getBoolean("isBlocked");

                        user = UserModel.builder()
                                .userId(userID)
                                .userName(retrievedUserName)
                                .fullName(fullName)
                                .email(email)
                                .contactNo(contactNo)
                                .role(role)
                                .isActive(isActive)
                                .isBlocked(isBlocked)
                                .build();

                        System.out.println("Authentication Success for: " + userName);
                    } else {
                        // SP returned no rows (user not found or passwords did not match)
                        System.out.println("Authentication Failed: Invalid username or password.");
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error during authentication: " + e.getMessage());
            System.err.println("Database error during authentication: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Catch exceptions from HashUtils or unexpected errors
            System.err.println("Unexpected error during authentication: " + e.getMessage());
            e.printStackTrace();
        }

        return user;
    }

    /**
     * Adds a new user to the TBLUsers table.
     * @param user The UserModel object to save.
     */
    public void addUser(User user) {
        String sql = "INSERT INTO TBLUsers (FullName, Email, ContactNo, Address, CNIC, IsActive) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = MySQLConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getContactNo());
            ps.setString(4, user.getAddress());
            ps.setString(5, user.getCnic());
            ps.setBoolean(6, user.isActive());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null,
                        "User '" + user.getFullName() + "' saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                System.out.println("DAO: User saved successfully: " + user.getFullName());
            } else {
                JOptionPane.showMessageDialog(null,
                        "User not saved. No rows were affected.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            JOptionPane.showMessageDialog(null,
                    "Database error while saving user: " + errorMessage,
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            System.err.println("Database error during addUser: " + errorMessage);
            e.printStackTrace();
        }
    }
}
