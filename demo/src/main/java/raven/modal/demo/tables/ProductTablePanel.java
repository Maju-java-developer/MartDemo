package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.utils.Constants;
import raven.modal.demo.dao.ProductDao;
import raven.modal.demo.dao.UtilsDao;
import raven.modal.demo.forms.FormProducts;
import raven.modal.demo.model.ProductModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.JComponentUtils;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.List;

@SystemForm(name = "Products", description = "View and manage product records", tags = { "product", "table" })
public class ProductTablePanel extends Form implements TableActions {

    private JTable table;
    private DefaultTableModel model;
    private ProductDao productDao = new ProductDao();
    private JPagination pagination;
    private JLabel lbTotal;

    public ProductTablePanel() {
        initUI();
    }

    @Override
    public void formOpen() {
        loadProducts(1);
    }

    private void initUI() {
        setLayout(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][fill,grow][]"));

        JLabel title = new JLabel("Product List");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        add(title, "gapx 20");

        // Table model: Only show required columns
        model = new DefaultTableModel(Constants.productColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Only the Action column is editable
                return col == (Constants.productColumns.length - 1);
            }
        };

        // Table setup
        table = new JTable(model);

        // Alignment table header
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column == (Constants.productColumns.length - 1)) { // Action column (index 4)
                    return SwingConstants.CENTER;
                }
                return SwingConstants.LEADING;
            }
        });

        // Set renderer & editor for Action column (the last column)
        int actionColumnIndex = (table.getColumnCount() - 1);
        ActionItem[] actions = tableActions();

        table.getColumnModel().getColumn(actionColumnIndex).setCellRenderer(new TableActionCellRenderer(actions));
        table.getColumnModel().getColumn(actionColumnIndex).setCellEditor(new TableActionCellEditor(table, actions));

        // Column Width Settings
        table.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        table.getColumnModel().getColumn(2).setPreferredWidth(120); // Code
        table.getColumnModel().getColumn(3).setPreferredWidth(80); // Unit
        table.getColumnModel().getColumn(actionColumnIndex).setPreferredWidth(150); // Action

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:30;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

        add(scroll);

        // Pagination Panel
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> loadProducts(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        pagePanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
    }

    private void loadProducts(int page) {
        model.setRowCount(0);
        // Items per page
        int limit = Constants.LIMIT_PER_PAGE;
        int offset = (page - 1) * limit;

        // Fetch data using the DAO
        List<ProductModel> products = productDao.getAllProducts(offset, limit);
        int totalProducts = UtilsDao.getCount("tblproducts");

        for (ProductModel productModel : products) {
            model.addRow(new Object[] {
                    productModel.getProductId(),
                    productModel.getProductName(),
                    productModel.getProductCode(),
                    productModel.getCompanyName(),
                    productModel.getBrandName(),
                    productModel.getCategoryName(),
                    productModel.getPeckingTypeName(),
            });
        }

        // Update pagination and total count
        lbTotal.setText(DecimalFormat.getInstance().format(totalProducts));
        int totalPages = (int) Math.ceil((double) totalProducts / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    @Override
    public void formRefresh() {
        loadProducts(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        // Define the dynamic actions for the Product table: Edit and Delete
        return new ActionItem[] {
                new ActionItem("Edit", (table1, row) -> {
                    // Assuming column 0 holds the Product ID
                    int productId = (int) table1.getValueAt(row, 0);
                    openProductFormModal(productId);
                }),
                new ActionItem("Delete", (table1, row) -> {
                    int productId = (int) table1.getValueAt(row, 0);
                    String productName = table1.getValueAt(row, 1).toString();
                    int confirm = JOptionPane.showConfirmDialog(table1,
                            "Are you sure you want to delete product: " + productName + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        deleteButtonAction(productId);
                        formRefresh(); // Refresh table after deletion
                    }
                })
        };
    }

    private void openProductFormModal(int typeId) {
        JComponentUtils.showModal(
                SwingUtilities.getWindowAncestor(this),
                new FormProducts(typeId),
                typeId > 0 ? "Edit Product Type" : "Create New Product Type");
        formRefresh();
    }

    public void deleteButtonAction(int productId) {
        // 1. Create a minimal product model just for the ID
        ProductModel productModel = new ProductModel();
        productModel.setProductId(productId);

        int returnCode = productDao.handleProductCRUD(productModel, "Delete");

        if (returnCode == -2) { // Delete Success
            JOptionPane.showMessageDialog(null, "Product ID " + productId + " deleted successfully!",
                    "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
        } else if (returnCode == -4) { // Not Found
            JOptionPane.showMessageDialog(null, "Product ID " + productId + " not found. No record was deleted.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
        } else if (returnCode == 0) {
            // Error already displayed by DAO (e.g., Integrity Constraint)
        }
    }
}