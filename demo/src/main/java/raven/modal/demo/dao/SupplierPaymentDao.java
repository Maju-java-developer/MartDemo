package raven.modal.demo.dao;

import raven.modal.demo.model.SupplierPaymentModel;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class SupplierPaymentDao {

    /**
     * Saves a new supplier payment, allocating the amount to the oldest outstanding
     * purchase invoices (FIFO) and updating the supplier's master balance.
     * * NOTE: TBLPurchase must have a 'BalanceDue' column for this to work.
     */
    public boolean saveSupplierPayment(SupplierPaymentModel paymentModel) {
        Connection conn = null;
        try {
            conn = MySQLConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // --- A. Record the Payment Header (TBLSupplierPayments) ---
            String sqlPayment = "INSERT INTO TBLSupplierPayments (SupplierID, PaymentDate, Amount, Remarks) " +
                    "VALUES (?, ?, ?, ?)";

            int paymentId = 0;
            try (PreparedStatement psPayment = conn.prepareStatement(sqlPayment, Statement.RETURN_GENERATED_KEYS)) {
                psPayment.setInt(1, paymentModel.getSupplierID());
                psPayment.setTimestamp(2, Timestamp.valueOf(paymentModel.getPaymentDate()));
                psPayment.setDouble(3, paymentModel.getAmount());
                psPayment.setString(4, paymentModel.getRemarks());
                psPayment.executeUpdate();

                try (ResultSet rs = psPayment.getGeneratedKeys()) {
                    if (rs.next()) {
                        paymentId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve Payment ID.");
                    }
                }
            }

            // --- B. Payment Allocation (FIFO) ---
            double paymentRemaining = paymentModel.getAmount();

            // 1. Get partially paid purchases (ORDERED BY PurchaseDate ASC)
            String sqlGetOutstanding = "SELECT PurchaseID, (TotalAmount - PaidAmount) AS BalanceDue " +
                    "FROM TBLPurchase " +
                    "WHERE SupplierID = ? AND (TotalAmount - PaidAmount) > 0 " +
                    "ORDER BY PurchaseDate ASC";

            try (PreparedStatement psOutstanding = conn.prepareStatement(sqlGetOutstanding)) {
                psOutstanding.setInt(1, paymentModel.getSupplierID());
                try (ResultSet rs = psOutstanding.executeQuery()) {

                    while (rs.next() && paymentRemaining > 0) {
                        int purchaseId = rs.getInt("PurchaseID");
                        double invoiceBalance = rs.getDouble("BalanceDue");

                        double amountToApply = Math.min(paymentRemaining, invoiceBalance);

                        // 2. Update the specific Purchase record
                        String sqlUpdatePurchase = "UPDATE TBLPurchase SET PaidAmount = PaidAmount + ? " +
                                "WHERE PurchaseID = ?";
                        try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdatePurchase)) {
                            psUpdate.setDouble(1, amountToApply);
                            psUpdate.setInt(2, purchaseId);
                            psUpdate.executeUpdate();
                        }

                        paymentRemaining -= amountToApply;
                    }
                }
            }

            // --- C. Update Supplier's Cumulative OutstandingBalance ---
            String sqlBalance = "UPDATE TBLSuppliers SET OpeningBalance = OpeningBalance - ? WHERE SupplierID = ?";
            try (PreparedStatement psBalance = conn.prepareStatement(sqlBalance)) {
                psBalance.setDouble(1, paymentModel.getAmount());
                psBalance.setInt(2, paymentModel.getSupplierID());

                if (psBalance.executeUpdate() == 0) {
                    throw new SQLException("Failed to update supplier outstanding balance.");
                }
            }

            conn.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            System.err.println("Database error during saveSupplierPayment. Rolling back: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            JOptionPane.showMessageDialog(null, "Payment failed due to a database error.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true); conn.close();
            } catch (SQLException closeEx) {
                // Ignore
            }
        }
    }
}
