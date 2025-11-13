package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.BrandDao;
import raven.modal.demo.dao.CompanyDao;
import raven.modal.demo.model.BrandModel;
import raven.modal.demo.model.CompanyModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.ComboBoxUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a form to add or edit product brand information.
 * Allows users to specify brand details, link them to companies, and set active status.
 */
@SystemForm(name = "Brand Form", description = "Add or edit product brand information", tags = {"brand", "form"})
public class FormBrand extends Form {

    private JTextField txtBrandTitle;
    private JComboBox<CompanyModel> cmbCompany;
    private JComboBox<String> cmbIsActive;
    private JButton btnSave, btnClear;

    private int brandId = 0;
    private JLabel titleLabel;
    private BrandDao brandDao = new BrandDao();
    private CompanyDao companyDao = new CompanyDao();

    private Map<String, Integer> companyMap = new HashMap<>();

    /**
     * Creates a new FormBrand to edit a specific brand.
     * @param brandId ID of the brand to edit. If 0, creates new brand form.
     */
    public FormBrand(int brandId) {
        this.brandId = brandId;
        init();
        if (this.brandId > 0) {
            loadBrandData(this.brandId);
        }
    }

    /**
     * Creates a new FormBrand for adding a new brand.
     */
    public FormBrand() {
        this(0);
    }

    /**
     * Initializes the layout and form components.
     */
    private void init() {
        setLayout(new MigLayout("wrap, fillx", "[fill]"));
        add(createHeader());
        add(createFormPanel(), "gapy 10");
    }

    /**
     * Creates the header section for the form.
     * @return Component representing the header panel
     */
    private Component createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));

        titleLabel = new JLabel(brandId > 0 ? "Edit Brand" : "Add New Brand");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        JTextPane text = new JTextPane();
        text.setText("Enter brand details and link to an active company.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());

        panel.add(titleLabel);
        panel.add(text, "width 500");
        return panel;
    }

    /**
     * Creates the main form panel containing fields for brand details.
     * @return JPanel containing form fields
     */
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", ""));
        panel.setBorder(new TitledBorder("Brand Details"));

        txtBrandTitle = new JTextField();
        cmbCompany = new JComboBox<>();

        cmbIsActive = new JComboBox<>(new String[]{"Active", "Inactive"});
        cmbIsActive.setSelectedItem("Active");

        // Populate company dropdown
        List<CompanyModel> companies = companyDao.getActiveCompaniesForDropdown();
        cmbCompany.setModel(new DefaultComboBoxModel<>(companies.toArray(new CompanyModel[0])));

        // Set renderer to show company name
        ComboBoxUtils.setupComboBoxRenderer(cmbCompany, m -> ((CompanyModel)m).getCompanyName());

        txtBrandTitle.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter brand title (e.g., Apple, Samsung)");

        panel.add(new JLabel("Brand Title *:"));
        panel.add(txtBrandTitle);

        panel.add(new JLabel("Company *:"));
        panel.add(cmbCompany);

        panel.add(new JLabel("Status:"));
        panel.add(cmbIsActive);

        panel.add(createButtonPanel(), "span 2, align center, gaptop 15");

        return panel;
    }

    /**
     * Creates the button panel for Save and Clear actions.
     * @return JPanel containing action buttons
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][][]", "10[]10"));

        btnSave = new JButton(brandId > 0 ? "Update" : "Save");
        btnClear = new JButton("Clear");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear, "gapright 10");

        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveBrand());

        return buttonPanel;
    }

    /**
     * Loads brand data to populate the form for editing.
     * @param id ID of the brand to load
     */
    private void loadBrandData(int id) {
        BrandModel brand = brandDao.getBrandById(id);

        if (brand != null) {
            txtBrandTitle.setText(brand.getBrandTitle());
            cmbIsActive.setSelectedItem(brand.isActive() ? "Active" : "Inactive");

            // Select company in combo box
            ComboBoxUtils.setComboBoxSelection(cmbCompany, brand.getCompanyId(), CompanyModel::getCompanyId);

            titleLabel.setText("Edit Brand: " + brand.getBrandTitle());
        } else {
            JOptionPane.showMessageDialog(this, "Brand ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            this.brandId = 0;
        }
    }

    /**
     * Clears all form fields and resets to default.
     */
    private void clearForm() {
        txtBrandTitle.setText("");
        cmbIsActive.setSelectedItem("Active");
        cmbCompany.setSelectedIndex(0);
    }

    /**
     * Validates form data and saves the brand.
     * If editing, updates the existing brand, otherwise inserts new brand.
     */
    private void saveBrand() {
        String title = txtBrandTitle.getText().trim();
        CompanyModel selectedCompany = (CompanyModel) cmbCompany.getSelectedItem();

        // --- Validation Logic ---
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Brand Title is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedCompany == null || selectedCompany.getCompanyId() == 0) {
            JOptionPane.showMessageDialog(this, "A company must be selected.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // --- End Validation ---

        boolean isActive = Objects.equals(cmbIsActive.getSelectedItem(), "Active");

        BrandModel brandModel = BrandModel.builder()
                .brandTitle(title)
                .companyId(selectedCompany.getCompanyId())
                .isActive(isActive)
                .build();

        if (brandId > 0) {
            brandModel.setBrandId(brandId);
            brandDao.updateBrand(brandModel);
        } else {
            brandDao.addBrand(brandModel);
        }

        SwingUtilities.getWindowAncestor(this).dispose();
    }
}