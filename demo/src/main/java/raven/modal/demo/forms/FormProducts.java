package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.BrandDao;
import raven.modal.demo.dao.CategoryDao;
import raven.modal.demo.dao.CompanyDao;
import raven.modal.demo.dao.PackingTypeDao;
import raven.modal.demo.dao.ProductDao;
import raven.modal.demo.model.BrandModel;
import raven.modal.demo.model.CategoryModel;
import raven.modal.demo.model.CompanyModel;
import raven.modal.demo.model.PackingTypeModel;
import raven.modal.demo.model.ProductModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.ComboBoxUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.Collections;
import java.util.List;

@SystemForm(name = "Product Form", description = "Add or edit product information", tags = {"product", "form"})
public class FormProducts extends Form {

    private JTextField txtProductCode, txtProductName;
    private JComboBox<CompanyModel> cmbCompany;
    private JComboBox<CategoryModel> cmbCategory;
    private JComboBox<PackingTypeModel> cmbPeckingType;
    private JComboBox<BrandModel> cmbBrand; // Dependent dropdown

    private JCheckBox chkIsActive;
    private JButton btnSave, btnClear;

    private int productId = 0; // 0 for ADD mode
    private JLabel titleLabel;

    private final CompanyDao companyDao = new CompanyDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final PackingTypeDao peekingTypeDao = new PackingTypeDao();
    private final BrandDao brandDao = new BrandDao();
    private final ProductDao productDao = new ProductDao();

    public FormProducts(int productId) {
        this.productId = productId;
        init();
        loadInitialData(); // Load non-dependent data
        if (this.productId > 0) {
            loadProductData(this.productId);
        }
    }

    public FormProducts() {
        this(0);
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx", "[fill]"));
        titleLabel = createHeader();
        add(titleLabel);
        add(createFormPanel(), "gapy 10");

        // Add listener for dependent dropdown (Brand based on Company selection)
        cmbCompany.addActionListener(e -> updateBrandDropdown());
    }

    private JLabel createHeader() {
        // Refactored to return the JLabel for easy text update in EDIT mode
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));
        JLabel title = new JLabel(productId > 0 ? "Edit Product" : "Add Product");
        JTextPane text = new JTextPane();
        text.setText("Enter product details. All dropdowns are required.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        panel.add(title);
        panel.add(text, "width 500");

        // Add the container to the form itself (or just return the title if we wrap the header logic)
        return title;
    }

    private void loadInitialData() {
        // Load Company (triggers Brand update via listener)
        List<CompanyModel> companies = companyDao.getActiveCompaniesForDropdown();
        cmbCompany.setModel(new DefaultComboBoxModel<>(companies.toArray(new CompanyModel[0])));

        // Load Category
        List<CategoryModel> categories = categoryDao.getActiveCategoriesForDropdown();
        cmbCategory.setModel(new DefaultComboBoxModel<>(categories.toArray(new CategoryModel[0])));

        // Load PeekingType (adjust method name as per your DAO)
        List<PackingTypeModel> types = peekingTypeDao.getActivePeekingTypesForDropdown();
        cmbPeckingType.setModel(new DefaultComboBoxModel<>(types.toArray(new PackingTypeModel[0])));

        // Load Brand (initially loaded empty or with placeholder, updated by cmbCompany listener)
        updateBrandDropdown(0); // Load default placeholder initially

        // Set up renderers for clarity (similarly to FormBrand)
        ComboBoxUtils.setupComboBoxRenderer(cmbCompany, m -> ((CompanyModel)m).getCompanyName());
        ComboBoxUtils.setupComboBoxRenderer(cmbCategory, m -> ((CategoryModel)m).getCategoryName());
        ComboBoxUtils.setupComboBoxRenderer(cmbPeckingType, m -> ((PackingTypeModel) m).getPackingTypeName());
        ComboBoxUtils.setupComboBoxRenderer(cmbBrand, m -> ((BrandModel)m).getBrandTitle());
    }

    private void updateBrandDropdown() {
        CompanyModel selectedCompany = (CompanyModel) cmbCompany.getSelectedItem();
        int companyId = (selectedCompany != null) ? selectedCompany.getCompanyId() : 0;
        updateBrandDropdown(companyId);
    }

    private void updateBrandDropdown(int companyIdToLoad) {
        List<BrandModel> brands;
        if (companyIdToLoad > 0) {
            brands = brandDao.getBrandsByCompanyId(companyIdToLoad);
        } else {
            // Load only a placeholder if no company is selected
            brands = Collections.singletonList(BrandModel.builder().brandId(0).brandTitle("--- Select Brand ---").build());
        }
        cmbBrand.setModel(new DefaultComboBoxModel<>(brands.toArray(new BrandModel[0])));
    }


    private JPanel createFormPanel() {
        // Two columns, aligned to the left
        JPanel panel = new JPanel(new MigLayout("wrap 4, fillx", "[left]10[grow,fill]15[left]10[grow,fill]", ""));
        panel.setBorder(new TitledBorder("Product Configuration"));

        txtProductCode = new JTextField();
        txtProductName = new JTextField();
        cmbCompany = new JComboBox<>();
        cmbCategory = new JComboBox<>();
        cmbPeckingType = new JComboBox<>();
        cmbBrand = new JComboBox<>();
        chkIsActive = new JCheckBox("Product is Active", true);

        txtProductCode.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter unique product code");
        txtProductName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter product name");

        // --- Form Rows (Two Columns per Row) ---
        // Row 1: Product Name / Product Code
        panel.add(new JLabel("Product Name *:"));
        panel.add(txtProductName);
        panel.add(new JLabel("Product Code:"));
        panel.add(txtProductCode);

        // Row 2: Company / Category
        panel.add(new JLabel("Company *:"));
        panel.add(cmbCompany);
        panel.add(new JLabel("Brand *:"));
        panel.add(cmbBrand);

        // Row 3: Peeking Type / Brand (Brand depends on Company)
        panel.add(new JLabel("Category *:"));
        panel.add(cmbCategory);

        panel.add(new JLabel("Pecking Type *:"));
        panel.add(cmbPeckingType);

        // Row 4: Status (Span two columns)
        panel.add(new JLabel("Status:"));
        panel.add(chkIsActive, "span 3"); // Checkbox spans the remaining row space

        // --- Buttons ---
        panel.add(createButtonPanel(), "span 4, align center, gaptop 15"); // Buttons span all 4 columns

        return panel;
    }

    private JPanel createButtonPanel() {
        // ... (Button panel creation remains the same) ...
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][][]", "10[]10"));

        btnSave = new JButton(productId > 0 ? "Update Product" : "Save Product");
        btnClear = new JButton("Clear");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear, "gapright 10");

        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveProduct());

        return buttonPanel;
    }

    private void loadProductData(int id) {
        ProductModel product = productDao.getProductById(id);

        if (product != null) {
            txtProductName.setText(product.getProductName());
            txtProductCode.setText(product.getProductCode());
            chkIsActive.setSelected(product.isActive());
            titleLabel.setText("Edit Product: " + product.getProductName());

            // Set Category/PeekingType
            ComboBoxUtils.setComboBoxSelection(cmbCategory, product.getCategoryId(), CategoryModel::getCategoryId);
            ComboBoxUtils.setComboBoxSelection(cmbPeckingType, product.getPeckingTypeId(), PackingTypeModel::getPackingTypeId);

            // Set Company (this must happen first to load the correct Brands)
            CompanyModel selectedCompany = ComboBoxUtils.setComboBoxSelection(cmbCompany, product.getCompanyId(), CompanyModel::getCompanyId);

            // Manually trigger Brand load based on the loaded CompanyId
            if (selectedCompany != null) {
                updateBrandDropdown(selectedCompany.getCompanyId());
            }

            // Set Brand
            ComboBoxUtils.setComboBoxSelection(cmbBrand, product.getBrandId(), BrandModel::getBrandId);

        } else {
            JOptionPane.showMessageDialog(this, "Product ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            this.productId = 0;
            titleLabel.setText("Add Product");
        }
    }

    public void clearForm() {
        txtProductCode.setText("");
        txtProductName.setText("");
        cmbCompany.setSelectedIndex(0);
        cmbCategory.setSelectedIndex(0);
        cmbPeckingType.setSelectedIndex(0);
        // Note: cmbBrand will be updated by cmbCompany's listener
        chkIsActive.setSelected(true);
    }

    private void saveProduct() {
        String name = txtProductName.getText().trim();
        CompanyModel company = (CompanyModel) cmbCompany.getSelectedItem();
        CategoryModel category = (CategoryModel) cmbCategory.getSelectedItem();
        BrandModel brand = (BrandModel) cmbBrand.getSelectedItem();
        PackingTypeModel peckingType = (PackingTypeModel) cmbPeckingType.getSelectedItem();

        // --- Validation Logic ---
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Product Name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate all required foreign key selections (assuming ID 0 is the placeholder)
        if (company == null || company.getCompanyId() == 0 ||
                category == null || category.getCategoryId() == 0 ||
                brand == null || brand.getBrandId() == 0 ||
                peckingType == null || peckingType.getPackingTypeId() == 0) {

            JOptionPane.showMessageDialog(this, "All four dropdown fields (Company, Category, Brand, Peeking Type) must be selected.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // --- End Validation ---

        try {
            String code = txtProductCode.getText().trim();

            ProductModel productModel = ProductModel.builder()
                    .productName(name)
                    .productCode(code.isEmpty() ? null : code)
                    .companyId(company.getCompanyId())
                    .categoryId(category.getCategoryId())
                    .brandId(brand.getBrandId())
                    .peckingTypeId(peckingType.getPackingTypeId())
                    .isActive(chkIsActive.isSelected())
                    .build();

            if (productId > 0) {
                productModel.setProductId(productId);
                productDao.updateProduct(productModel);
                SwingUtilities.getWindowAncestor(this).dispose();
            } else {
                productDao.addProduct(productModel);
                clearForm();
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while saving the product: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void formRefresh() {
        loadInitialData();
    }
}