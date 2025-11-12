package raven.modal.demo.dao;

import lombok.Getter;
import raven.modal.demo.model.ModelUser;
import raven.modal.demo.model.User;
import raven.modal.demo.mysql.MySQLConnection;
import raven.modal.demo.utils.HashUtils;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
public class UserDAO {

//    /**
//     * Authenticates a user against the 'user' table.
//     * @param userName The username provided by the user.
//     * @param password The password provided by the user.
//     * @return UserModel object if credentials are valid, otherwise null.
//     */
//    public static UserModel authenticateUser(String userName, String password) {
//        UserModel user = null;
//
//        // SQL query to select a user where both UserName and Password match
//        String query = "SELECT UserID, UserName, Password FROM user WHERE UserName = ? AND Password = ?";
//
//        // Get the single connection instance
//        try (Connection conn = MySQLConnection.getInstance().getConnection()) {
//
//            // Validate the connection before using it
//            if (conn == null || conn.isClosed() || !conn.isValid(2)) {  // 2 seconds timeout for validation
//                JOptionPane.showMessageDialog(null, "Connection is closed or invalid.");
//                throw new SQLException("Connection is closed or invalid.");
//            }
//
//            // 1. Create a PreparedStatement for safety
//            try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
//
//                // 2. Set the parameters from the user input
//                preparedStatement.setString(1, userName);
//                preparedStatement.setString(2, password);
//
//                // 3. Execute the query
//                try (ResultSet rs = preparedStatement.executeQuery()) {
//
//                    // 4. Check if a row was returned (i.e., authentication succeeded)
//                    if (rs.next()) {
//                        // Create and return the ModelUser object
//                        int userID = rs.getInt("UserID");
//                        String retrievedUserName = rs.getString("UserName");
//                        String retrievedPassword = rs.getString("Password");
//
//                        user = new UserModel(userID, retrievedUserName, retrievedPassword);
//                        System.out.println("Authentication Success for: " + userName);
//                    } else {
//                        // No matching user found
//                        System.out.println("Authentication Failed: Invalid username or password.");
//                    }
//                }
//
//            }
//
//        } catch (SQLException e) {
//            JOptionPane.showMessageDialog(null, "Database error during authentication: " + e.getMessage());
//            System.err.println("Database error during authentication: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return user;
//    }

    /**
     * Authenticates a user against the 'TBLUserLogin' table.
     * @param userName The username provided by the user.
     * @param password The password provided by the user.
     * @return UserModel object if credentials are valid, otherwise null.
     */
    public static ModelUser authenticateUser(String userName, String password) {
        ModelUser user = null;

        // SQL query to select a user where both Username and PasswordHash match
        String query = "SELECT ul.UserID, ul.Username, ul.PasswordHash, u.FullName, u.Email, u.ContactNo, ul.Role " +
                "FROM TBLUserLogin ul " +
                "INNER JOIN TBLUsers u ON ul.UserID = u.UserID " +
                "WHERE ul.Username = ? AND ul.IsBlocked = FALSE";  // Ensure user is not blocked

        // Get the single connection instance
        try (Connection conn = MySQLConnection.getInstance().getConnection()) {

            // Validate the connection before using it
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {  // 2 seconds timeout for validation
                JOptionPane.showMessageDialog(null, "Connection is closed or invalid.");
                throw new SQLException("Connection is closed or invalid.");
            }

            // 1. Create a PreparedStatement for safety
            try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {

                // 2. Set the parameters from the user input
                preparedStatement.setString(1, userName);

                // 3. Execute the query
                try (ResultSet rs = preparedStatement.executeQuery()) {

                    // 4. Check if a row was returned (i.e., authentication succeeded)
                    if (rs.next()) {
                        // Get the hashed password and compare with the entered password
                        String storedPasswordHash = rs.getString("PasswordHash");

                        // Hash the input password (you can change this to use SHA2 or bcrypt, etc.)
                        String inputPasswordHash = HashUtils.hashPassword(password); // Assuming you have a method to hash the password

                        // Compare the stored hash with the entered hash
                        if (storedPasswordHash.equals(inputPasswordHash)) {
                            // Create the UserModel object with user details
                            int userID = rs.getInt("UserID");
                            String retrievedUserName = rs.getString("Username");
                            String fullName = rs.getString("FullName");
                            String email = rs.getString("Email");
                            String contactNo = rs.getString("ContactNo");

                            String roleString = rs.getString("Role");
                            ModelUser.Role role = ModelUser.Role.fromString(roleString);

                            user = ModelUser.builder()
                                    .userId(userID)
                                    .userName(retrievedUserName)
                                    .fullName(fullName)
                                    .email(email)
                                    .contactNo(contactNo)
                                    .role(role)
                                    .build();

                            System.out.println("Authentication Success for: " + userName);
                        } else {
                            // Passwords don't match
                            System.out.println("Authentication Failed: Invalid username or password.");
                        }
                    } else {
                        // No matching user found
                        System.out.println("Authentication Failed: Invalid username or password.");
                    }
                }

            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error during authentication: " + e.getMessage());
            System.err.println("Database error during authentication: " + e.getMessage());
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
