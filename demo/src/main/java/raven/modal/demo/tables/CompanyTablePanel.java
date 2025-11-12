package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.utils.Constants;
import raven.modal.demo.dao.CompanyDao;
import raven.modal.demo.dao.UtilsDao;
import raven.modal.demo.forms.FormCompany;
import raven.modal.demo.model.CompanyModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

@SystemForm(name = "Companies", description = "Manage company records", tags = {"company", "table"})
public class CompanyTablePanel extends Form implements TableActions {

    private JTable table;
    private DefaultTableModel model;
    private CompanyDao companyDao;
    private JPagination pagination;
    private JLabel lbTotal;
    private int limit = 10;
    private JButton btnCreate;

    public CompanyTablePanel() {
        companyDao = new CompanyDao();
        initUI();
        loadCompanies(1);
    }

    private void initUI() {
        // Layout: Header Row, Control Row, Table Row, Pagination Row
        setLayout(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][][fill,grow][]"));

        JLabel title = new JLabel("Company List");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        add(title, "gapx 20");

        // --- Control Panel (Includes Create Button) ---
        JPanel controlPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill]20[right]", ""));
        controlPanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        btnCreate = new JButton("Create Company");
        btnCreate.putClientProperty(FlatClientProperties.STYLE, "font:bold; background:$Component.accentColor; foreground:white");
        btnCreate.addActionListener(e -> openCompanyFormModal(0)); // Open modal in ADD mode

        controlPanel.add(new JPanel(), "growx"); // Spacer to push button right
        controlPanel.add(btnCreate, "align right, w 150!, gapleft 10, gapright 10, gaptop 5, gapbottom 5");
        add(controlPanel, "gapx 20, gaptop 10");

        model = new DefaultTableModel(Constants.companyColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3;
            }
        };

        // Table setup
        table = new JTable(model);

        // Table Header Alignment
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column == 3) { // Action column
                    return SwingConstants.CENTER;
                }
                return SwingConstants.LEADING;
            }
        });

        // Set renderer & editor for Action column
        int actionColumnIndex = (table.getColumnCount() - 1);
        ActionItem[] actions = tableActions();

        table.getColumnModel().getColumn(actionColumnIndex).setCellRenderer(new TableActionCellRenderer(actions));
        table.getColumnModel().getColumn(actionColumnIndex).setCellEditor(new TableActionCellEditor(table, actions));

        // Column Width Settings
        table.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Status
        table.getColumnModel().getColumn(actionColumnIndex).setPreferredWidth(150); // Action

        // Standard table styling
        JScrollPane scroll = new JScrollPane(table);
        // ... (table styling properties remain the same) ...
        scroll.setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:30; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE, "rowHeight:30; showHorizontalLines:true; intercellSpacing:0,1; cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; selectionForeground:$Table.foreground;");


        add(scroll);

        // Pagination Panel
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> loadCompanies(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        pagePanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
    }

    // --- MODAL DIALOG METHOD ---
    private void openCompanyFormModal(int companyId) {
        // Reuse FormCompany, passing 0 for ADD or the ID for EDIT
        FormCompany formPanel = new FormCompany(companyId);

        // Create the Modal Dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                companyId > 0 ? "Edit Company" : "Create New Company",
                Dialog.ModalityType.APPLICATION_MODAL);

        dialog.setContentPane(formPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        // Refresh table after the dialog is closed (i.e., after save/update)
        formRefresh();
    }

    private void loadCompanies(int page) {
        model.setRowCount(0);
        int offset = (page - 1) * limit;

        List<CompanyModel> companies = companyDao.getAllCompanies(offset, limit);
        int totalCompanies = UtilsDao.getCount("TBLCompanies");

        for (CompanyModel companyModel : companies) {
            model.addRow(new Object[]{
                    companyModel.getCompanyId(),
                    companyModel.getCompanyName(),
                    companyModel.isActive() ? "Active" : "Inactive"
            });
        }

        lbTotal.setText(DecimalFormat.getInstance().format(totalCompanies));
        int totalPages = (int) Math.ceil((double) totalCompanies / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    @Override
    public void formInit() {
        loadCompanies(1);
    }

    @Override
    public void formRefresh() {
        loadCompanies(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        // Define the dynamic actions for the Company table: Edit and Delete
        return new ActionItem[]{
                new ActionItem("Edit", (table1, row) -> {
                    int companyId = (int) table1.getValueAt(row, 0);
                    openCompanyFormModal(companyId); // Open modal in EDIT mode
                }),
                new ActionItem("Delete", (table1, row) -> {
                    int companyId = (int) table1.getValueAt(row, 0);
                    String companyName = table1.getValueAt(row, 1).toString();
                    int confirm = JOptionPane.showConfirmDialog(table1,
                            "Are you sure you want to delete company: " + companyName + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        companyDao.deleteCompany(companyId);
                        formRefresh(); // Refresh table after deletion
                    }
                })
        };
    }
}
