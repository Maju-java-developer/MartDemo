package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.CompanyDao;
import raven.modal.demo.model.CompanyModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Objects;

@SystemForm(name = "Company Form", description = "Add or edit company information", tags = {"company", "form"})
public class FormCompany extends Form {

    private JTextField txtCompanyName;
    private JComboBox<String> cmbIsActive;
    private JButton btnSave, btnClear;

    private int companyId = 0; // 0 for ADD, > 0 for EDIT
    private JLabel titleLabel;
    private CompanyDao companyDao = new CompanyDao();

    // --- Dual Constructor for Add/Edit ---
    public FormCompany(int companyId) {
        this.companyId = companyId;
        init();
        if (this.companyId > 0) {
            loadCompanyData(this.companyId);
        }
    }

    public FormCompany() {
        this(0); // Default to ADD mode
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx", "[fill]"));
        add(createHeader());
        add(createFormPanel(), "gapy 10");
    }

    private Component createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));

        titleLabel = new JLabel(companyId > 0 ? "Edit Company" : "Add New Company");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        JTextPane text = new JTextPane();
        text.setText("Enter company name and set status. Company Name is mandatory.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());

        panel.add(titleLabel);
        panel.add(text, "width 500");
        return panel;
    }

    private JPanel createFormPanel() {
        // --- Simplified Form Panel ---
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", ""));
        panel.setBorder(new TitledBorder("Company Details"));

        txtCompanyName = new JTextField();
        // isActive default will be true, so 'Active' is default selection
        cmbIsActive = new JComboBox<>(new String[]{"Active", "Inactive"});
        cmbIsActive.setSelectedItem("Active");

        // Apply FlatLaf style to fields
        txtCompanyName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter company name");

        // Form Rows (Only two fields)
        panel.add(new JLabel("Company Name *:"));
        panel.add(txtCompanyName);

        panel.add(new JLabel("Status:"));
        panel.add(cmbIsActive);

        panel.add(createButtonPanel(), "span 2, align center, gaptop 15");

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][][]", "10[]10"));

        btnSave = new JButton(companyId > 0 ? "Update" : "Save");
        btnClear = new JButton("Clear");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear, "gapright 10");

        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveCompany());

        return buttonPanel;
    }

    private void loadCompanyData(int id) {
        CompanyModel company = companyDao.getCompanyById(id);

        if (company != null) {
            // Load only the two required fields
            txtCompanyName.setText(company.getCompanyName());
            cmbIsActive.setSelectedItem(company.isActive() ? "Active" : "Inactive");

            titleLabel.setText("Edit Company: " + company.getCompanyName());
        } else {
            JOptionPane.showMessageDialog(this, "Company ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            this.companyId = 0; // Revert to ADD mode if data is missing
        }
    }

    private void clearForm() {
        txtCompanyName.setText("");
        cmbIsActive.setSelectedItem("Active");
    }

    private void saveCompany() {
        String name = txtCompanyName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Company name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isActive = Objects.equals(cmbIsActive.getSelectedItem(), "Active");
        CompanyDao dao = new CompanyDao();

        // Build the model with only the required fields set
        CompanyModel companyModel = CompanyModel.builder()
                .companyName(txtCompanyName.getText())
                .isActive(isActive)
                // Other fields (ContactNo, Email, Address) will be null/default in the model,
                // but the DAO update/insert must handle this if those columns exist in the DB.
                .build();

        if (companyId > 0) {
            // EDIT Mode: Set the ID and call UPDATE
            companyModel.setCompanyId(companyId);
            dao.updateCompany(companyModel); // Assumes DAO update handles sparse data
        } else {
            // ADD Mode: Call ADD
            dao.addCompany(companyModel);
        }
        // Close the dialog after successful save/update
        SwingUtilities.getWindowAncestor(this).dispose();
    }
}