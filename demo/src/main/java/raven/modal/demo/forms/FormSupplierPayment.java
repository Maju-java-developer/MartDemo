package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.SupplierDao;
import raven.modal.demo.dao.SupplierPaymentDao;
import raven.modal.demo.model.SupplierModel;
import raven.modal.demo.model.SupplierPaymentModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.combox.ComboBoxUtils;
import raven.modal.demo.utils.combox.JComponentUtils;

import javax.swing.*;
import java.util.List;

public class FormSupplierPayment extends Form {

    private final SupplierDao supplierDao = new SupplierDao();
    private final SupplierPaymentDao paymentDao = new SupplierPaymentDao();

    private JComboBox<SupplierModel> cbSupplier;
    private JTextField txtRemainingBalance, txtPaymentAmount;
    private JTextArea txtRemarks;
    private JButton btnPay, btnCancel;

    public FormSupplierPayment() {
        init();
    }

    @Override
    public void formOpen() {
        loadSuppliers();
    }

    private void init() {
        setLayout(new MigLayout("wrap, fill, insets 20", "[fill]"));

        JLabel title = new JLabel("Record Supplier Payment");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(title, "gapy 0 10");

        // --- Input Panel ---
        JPanel inputPanel = new JPanel(new MigLayout("wrap 4, fillx, insets 0", "[right][grow][right][grow]"));

        txtRemainingBalance = new JTextField();
        txtPaymentAmount = new JTextField();
        txtRemainingBalance.setEditable(false);

        cbSupplier = new JComboBox<>();
        cbSupplier.addActionListener(e -> updateRemainingBalance());

        inputPanel.add(new JLabel("Supplier:"));
        inputPanel.add(cbSupplier, "span 3, h 30!");

        inputPanel.add(new JLabel("Remaining Balance:"));
        inputPanel.add(txtRemainingBalance, "h 30!");

        inputPanel.add(new JLabel("Payment Amount:"));
        inputPanel.add(txtPaymentAmount, "h 30!");
        JComponentUtils.setNumberOnly(txtPaymentAmount); // Use the number filter utility

        txtRemarks = new JTextArea(3, 20);
        txtRemarks.setLineWrap(true);
        txtRemarks.setWrapStyleWord(true);
        JScrollPane scrollRemarks = new JScrollPane(txtRemarks);

        inputPanel.add(new JLabel("Remarks:"), "newline");
        inputPanel.add(scrollRemarks, "span 3, h 60!, growx");

        add(inputPanel);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new MigLayout("right, insets 0", "[][]"));
        btnCancel = new JButton("Cancel");
        btnPay = new JButton("Record Payment");

        btnCancel.addActionListener(e -> SwingUtilities.getWindowAncestor(this).dispose());
        btnPay.addActionListener(e -> recordPayment());

        buttonPanel.add(btnPay);
        buttonPanel.add(btnCancel);

        add(buttonPanel, "align right, gapy 10 0");
    }

    private void loadSuppliers() {
        List<SupplierModel> suppliers = supplierDao.getRemainingBalanceSuppliers();
        cbSupplier.setModel(new DefaultComboBoxModel<>(suppliers.toArray(new SupplierModel[0])));
        // Utility to render model objects by name
        ComboBoxUtils.setupComboBoxRenderer(cbSupplier, model -> ((SupplierModel)model).getSupplierName());
        if (!suppliers.isEmpty()) {
            updateRemainingBalance();
        } else {
            txtRemainingBalance.setText("0.00");
        }
    }

    private void updateRemainingBalance() {
        SupplierModel selectedSupplier = (SupplierModel) cbSupplier.getSelectedItem();
        if (selectedSupplier != null) {
            // Note: Assuming TBLSuppliers has a column named OutstandingBalance
            double balance = supplierDao.getSupplierBalance(selectedSupplier.getSupplierID());
            txtRemainingBalance.setText(String.format("%.2f", balance));
            txtPaymentAmount.setText(String.format("%.2f", balance)); // Suggest paying full balance
        } else {
            txtRemainingBalance.setText("0.00");
        }
    }

    private void recordPayment() {
        SupplierModel selectedSupplier = (SupplierModel) cbSupplier.getSelectedItem();
        if (selectedSupplier == null) {
            JOptionPane.showMessageDialog(this, "Please select a supplier.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double paymentAmount;
        try {
            paymentAmount = Double.parseDouble(txtPaymentAmount.getText());
            if (paymentAmount <= 0) {
                JOptionPane.showMessageDialog(this, "Payment amount must be greater than zero.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid payment amount.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Basic validation (optional, but good practice)
        double outstanding = Double.parseDouble(txtRemainingBalance.getText());
        if (paymentAmount > outstanding) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Payment amount (₹" + paymentAmount + ") exceeds the outstanding balance (₹" + outstanding + "). Proceed?",
                    "Payment Warning", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        SupplierPaymentModel payment = new SupplierPaymentModel(
                selectedSupplier.getSupplierID(),
                paymentAmount,
                txtRemarks.getText()
        );

        if (paymentDao.saveSupplierPayment(payment)) {
            JOptionPane.showMessageDialog(this, "Payment of " + String.format("%.2f", paymentAmount) + " recorded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            //            SwingUtilities.getWindowAncestor(this).dispose(); // Close dialog
            // TODO: Refresh the parent panel's table data
        }
    }
    /**
     * Resets all input fields and reloads the supplier data to reflect the new balances.
     */
    public void clearForm() {
        // 1. Reset text fields
        txtPaymentAmount.setText("");
        txtRemarks.setText("");

        // 2. Reload the supplier list and update the displayed balance.
        loadSuppliers();

        // 3. Ensure the pay button is enabled if loadSuppliers found outstanding balances
        btnPay.setEnabled(true);
    }

    @Override
    public void formRefresh() {
        clearForm();
    }
}