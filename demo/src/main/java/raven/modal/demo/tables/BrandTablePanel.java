package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.utils.Constants;
import raven.modal.demo.dao.BrandDao;
import raven.modal.demo.forms.FormBrand;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

@SystemForm(name = "Brands", description = "Manage product brands linked to companies", tags = {"brand", "table"})
public class BrandTablePanel extends Form implements TableActions {

    private JTable table;
    private DefaultTableModel model;
    private BrandDao brandDao;
    private JPagination pagination;
    private JLabel lbTotal;
    private int limit = 10;
    private JButton btnCreate;

    public BrandTablePanel() {
        brandDao = new BrandDao();
        initUI();
        loadBrands(1);
    }

    private void initUI() {
        // Layout: Header Row, Control Row, Table Row, Pagination Row
        setLayout(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][][fill,grow][]"));

        JLabel title = new JLabel("Brand List");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        add(title, "gapx 20");

        // --- Control Panel (Includes Create Button) ---
        JPanel controlPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill]20[right]", ""));

        btnCreate = new JButton("Create Brand");
        btnCreate.putClientProperty(FlatClientProperties.STYLE, "font:bold; background:$Component.accentColor; foreground:white");
        btnCreate.addActionListener(e -> openBrandFormModal(0)); // Open modal in ADD mode

        controlPanel.add(new JPanel(), "growx");
        controlPanel.add(btnCreate, "align right, w 150!, gapleft 10, gapright 10, gaptop 5, gapbottom 5");

//        add(controlPanel, "gapx 20, gaptop 10");
        add(controlPanel, "gaptop 10, gapbottom 5");

        model = new DefaultTableModel(Constants.brandsColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 4; // Action column is editable
            }
        };

        table = new JTable(model);

        // Header Alignment
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                // Center align Status and Action
                if (column == 3 || column == 4) return SwingConstants.CENTER;
                return SwingConstants.LEADING;
            }
        });

        // Action Column Setup
        int actionColumnIndex = (table.getColumnCount() - 1);
        ActionItem[] actions = tableActions();
        table.getColumnModel().getColumn(actionColumnIndex).setCellRenderer(new TableActionCellRenderer(actions));
        table.getColumnModel().getColumn(actionColumnIndex).setCellEditor(new TableActionCellEditor(table, actions));

        // Column Width Settings
        table.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Status
        table.getColumnModel().getColumn(actionColumnIndex).setPreferredWidth(150); // Action

        JScrollPane scroll = new JScrollPane(table);
        // Standard table styling
        scroll.setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:30; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE, "rowHeight:30; showHorizontalLines:true; intercellSpacing:0,1; cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; selectionForeground:$Table.foreground;");
        add(scroll, "grow, push");

        // Pagination Panel
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> loadBrands(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
    }

    // --- MODAL DIALOG METHOD ---
    private void openBrandFormModal(int brandId) {
        FormBrand formPanel = new FormBrand(brandId);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                brandId > 0 ? "Edit Brand" : "Create New Brand",
                Dialog.ModalityType.APPLICATION_MODAL);

        dialog.setContentPane(formPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        // Refresh table after the dialog is closed (i.e., after save/update)
        formRefresh();
    }

    private void loadBrands(int page) {
        model.setRowCount(0);
        int offset = (page - 1) * limit;

        // NOTE: The DAO method here needs to fetch the CompanyName using a JOIN.
        // Assuming BrandDao has a method: getBrandsWithCompanyName(offset, limit)
        List<Object[]> brandData = brandDao.getBrandsWithCompanyName(offset, limit);
        int totalBrands = brandDao.getBrandCount(); // Assuming this is implemented in BrandDao

        for (Object[] row : brandData) {
            // Row structure assumed from DAO: {BrandId, BrandTitle, CompanyName, IsActive}
            model.addRow(new Object[]{
                    row[0], // ID
                    row[1], // Brand Title
                    row[2], // Company Name
                    (boolean) row[3] ? "Active" : "Inactive" // Status
            });
        }

        lbTotal.setText(DecimalFormat.getInstance().format(totalBrands));
        int totalPages = (int) Math.ceil((double) totalBrands / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    // --- DAO Helper (Assuming this is implemented in BrandDao) ---
    // If you need the DAO implementation for getBrandsWithCompanyName, let me know!

    @Override
    public void formInit() {
        loadBrands(1);
    }

    @Override
    public void formRefresh() {
        loadBrands(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        return new ActionItem[]{
                new ActionItem("Edit", (table1, row) -> {
                    int brandId = (int) table1.getValueAt(row, 0);
                    openBrandFormModal(brandId); // Open modal in EDIT mode
                }),
                new ActionItem("Delete", (table1, row) -> {
                    int brandId = (int) table1.getValueAt(row, 0);
                    String brandTitle = table1.getValueAt(row, 1).toString();
                    int confirm = JOptionPane.showConfirmDialog(table1,
                            "Are you sure you want to delete brand: " + brandTitle + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        // Implement delete
                        brandDao.deleteBrand(brandId); // Assuming BrandDao has this method
                        formRefresh(); // Refresh table after deletion
                    }
                })
        };
    }
}