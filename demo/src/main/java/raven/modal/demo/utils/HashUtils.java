package raven.modal.demo.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    public static String hashPassword(String password) {
        try {
            // Get SHA-256 MessageDigest instance
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Hash the password and convert it to a hexadecimal string
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString(); // Return the hashed password as a hex string
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}

