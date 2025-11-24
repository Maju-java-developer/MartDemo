package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.SupplierDao;
import raven.modal.demo.dao.UtilsDao;
import raven.modal.demo.forms.FormSupplier;
import raven.modal.demo.model.SupplierModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.Constants;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.JComponentUtils;
import raven.modal.demo.utils.listeners.InputListenerConfiguration;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.List;

@SystemForm(name = "Vendors", description = "Manage vendor records", tags = { "vendor", "table" })
public class SupplierTablePanel extends Form implements TableActions, InputListenerConfiguration {

    private JTable table;
    private DefaultTableModel model;
    private final SupplierDao supplierDao = new SupplierDao();
    private JPagination pagination;
    private JLabel lbTotal;
    private JTextField txtSearch;

    public SupplierTablePanel() {
        initUI();
    }

    @Override
    public void formOpen() {
        loadSuppliers(1);
    }

    private JPanel addControlPanel() {
        JPanel controlPanel = new JPanel(new MigLayout(
                "fillx, insets 0",
                "[grow][fill][fill]",
                ""
        ));

        txtSearch = new JTextField();
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search Vendor...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new com.formdev.flatlaf.extras.FlatSVGIcon("raven/modal/demo/icons/search.svg", 0.4f));

        JButton btnCreate = new JButton("Create Vendor");
        btnCreate.putClientProperty(FlatClientProperties.STYLE,
                "font:bold; background:$Component.accentColor; foreground:white");
        btnCreate.addActionListener(e -> openSupplierFormModal(0)); // Open modal in ADD mode

        // Now items appear starting from LEFT
        controlPanel.add(txtSearch, "w 200!");
        controlPanel.add(btnCreate, "w 150!, gapleft 10");
        return controlPanel;
    }

    private void initUI() {
        // Updated main insets for better spacing
        setLayout(new MigLayout("fillx,wrap,insets 15 20 10 20", "[fill]", "[][][fill,grow][]"));

        JLabel title = new JLabel("Vendor List");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        add(title, "gaptop 5");

        add(addControlPanel(), "gaptop 10, gapbottom 5, wrap");

        model = new DefaultTableModel(Constants.supplierColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Set editable only for the Action column
                return col == (Constants.supplierColumns.length - 1);
            }
        };

        // Table
        table = new JTable(model);

        // alignment table header
        // ... (Header alignment logic remains the same) ...
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column == 6) {
                    return SwingConstants.CENTER;
                }
                return SwingConstants.LEADING;
            }
        });

        // Set renderer & editor for Action column
        int getLastOneColumn = (table.getColumnCount() - 1);
        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(getLastOneColumn).setCellRenderer(new TableActionCellRenderer(tableActions()));
        table.getColumnModel().getColumn(getLastOneColumn)
                .setCellEditor(new TableActionCellEditor(table, tableActions()));

        // Optional: set fixed width for Action column
        table.getColumnModel().getColumn(getLastOneColumn).setPreferredWidth(50);

        JScrollPane scroll = new JScrollPane(table);
        // ... (Table styling remains the same) ...
        scroll.setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:30;hoverBackground:null;pressedBackground:null;separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:30;showHorizontalLines:true;intercellSpacing:0,1;cellFocusColor:$TableHeader.hoverBackground;selectionBackground:$TableHeader.hoverBackground;selectionForeground:$Table.foreground;");

        add(scroll, "grow, push"); // Table takes remaining space

        // Pagination
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> loadSuppliers(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        pagePanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
        setupInputListener();
    }

    
    // --- Modal Handler ---
    private void openSupplierFormModal(int id) {
        JComponentUtils.showModal(
                SwingUtilities.getWindowAncestor(this),
                new FormSupplier(id),
                id > 0 ? "Edit Vendor" : "Create New Vendor");
        formRefresh();
    }

    private void loadSuppliers(int page) {
        model.setRowCount(0);
        int limit = Constants.LIMIT_PER_PAGE;
        int offset = (page - 1) * limit;

        String searchText = txtSearch.getText().trim();
        List<SupplierModel> suppliers = supplierDao.getSuppliers(offset, limit, searchText);
        int totalSuppliers = UtilsDao.getCount("TBLSuppliers");

        // The logic for count++ or offset+1 is usually redundant if you display the ID
        // The table should display the SupplierID (index 0)
        for (SupplierModel supplierModel : suppliers) {
            model.addRow(new Object[] {
                    supplierModel.getSupplierID(),
                    supplierModel.getSupplierName(),
                    supplierModel.getContactNo(),
                    supplierModel.getEmail(),
                    supplierModel.getAddress(),
                    supplierModel.getOpeningBalance()
                    // Action column is handled by the renderer/editor
            });
        }

        lbTotal.setText(DecimalFormat.getInstance().format(totalSuppliers));
        int totalPages = (int) Math.ceil((double) totalSuppliers / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    @Override
    public void formRefresh() {
        loadSuppliers(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        return new ActionItem[] {
                new ActionItem(new com.formdev.flatlaf.extras.FlatSVGIcon("raven/modal/demo/icons/edit.svg", 1.5f),
                        (table1, row) -> {
                            int supplierId = (int) table1.getValueAt(row, 0); // Get ID from first column
                            openSupplierFormModal(supplierId);
                        }),
                new ActionItem(new com.formdev.flatlaf.extras.FlatSVGIcon("raven/modal/demo/icons/delete.svg", 1.5f),
                        (table1, row) -> {
                            int supplierId = (int) table1.getValueAt(row, 0);
                            String supplierName = table1.getValueAt(row, 1).toString();

                            int confirm = JOptionPane.showConfirmDialog(table1,
                                    "Are you sure you want to delete vendor: " + supplierName + "?",
                                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

                            if (confirm == JOptionPane.YES_OPTION) {
                                if (supplierDao.deleteSupplier(supplierId) == -2) { // Call DAO delete
                                    JOptionPane.showMessageDialog(table1, "Vendor deleted successfully!", "Success",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    formRefresh();
                                }
                            }
                        })
        };
    }

    @Override
    public void setupInputListener() {
        // Listener for Detail Row Total Calculation
        KeyAdapter detailKeyAdapter = new KeyAdapter() {
            /**
             * Invoked when a key has been released.
             *
             * @param e
             */
            @Override
            public void keyReleased(KeyEvent e) {
                loadSuppliers(1);
            }
        };
        txtSearch.addKeyListener(detailKeyAdapter); // Search on Enter
    }
}