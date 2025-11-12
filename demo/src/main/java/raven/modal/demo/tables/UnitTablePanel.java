package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.UnitDao;
import raven.modal.demo.dao.UtilsDao;
import raven.modal.demo.forms.FormUnits;
import raven.modal.demo.model.UnitModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

@SystemForm(name = "Units", description = "Manage measurement unit records", tags = {"unit", "table"})
public class UnitTablePanel extends Form implements TableActions {

    private JTable table;
    private DefaultTableModel model;
    private UnitDao unitDao; // Placeholder
    private JPagination pagination;
    private JLabel lbTotal;
    private int limit = 10;

    public UnitTablePanel() {
        unitDao = new UnitDao();
        initUI();
        loadUnits(1);
    }

    private void initUI() {
        setLayout(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][fill,grow][]"));

        JLabel title = new JLabel("Unit List");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        add(title, "gapx 20");

        // Table model: UnitID, UnitName, Action
        Object[] columns = {"ID", "Unit Name", "Action"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Only the Action column is editable
                return col == 2;
            }
        };

        // Table setup
        table = new JTable(model);

        // Alignment table header
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column == 2) { // Action column (index 2)
                    return SwingConstants.CENTER;
                }
                return SwingConstants.LEADING;
            }
        });

        // Set renderer & editor for Action column (the last column)
        int actionColumnIndex = (table.getColumnCount() - 1);

        // Pass the dynamically defined actions to the renderer and editor
        ActionItem[] actions = tableActions();
        table.getColumnModel().getColumn(actionColumnIndex).setCellRenderer(new TableActionCellRenderer(actions));
        table.getColumnModel().getColumn(actionColumnIndex).setCellEditor(new TableActionCellEditor(table, actions));

        // Optional: set fixed width for Action column
        table.getColumnModel().getColumn(actionColumnIndex).setPreferredWidth(150);

        // Set fixed width for ID column
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(50);


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
        pagination.addChangeListener(e -> loadUnits(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        pagePanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
    }

    private void loadUnits(int page) {
        model.setRowCount(0);
        int offset = (page - 1) * limit;

        // Fetch data using the DAO placeholder
        List<UnitModel> allUnits = unitDao.getAllUnits(offset, limit);
        int totalUnits = UtilsDao.getCount("tblunits");

        for (UnitModel unitModel : allUnits) {
            model.addRow(new Object[]{
                    unitModel.getUnitID(),
                    unitModel.getUnitName()
            });
        }

        // Update pagination and total count
        lbTotal.setText(DecimalFormat.getInstance().format(totalUnits));
        int totalPages = (int) Math.ceil((double) totalUnits / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    @Override
    public void formInit() {
        loadUnits(1);
    }

    @Override
    public void formRefresh() {
        loadUnits(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        // Define the dynamic actions for the Unit table
        return new ActionItem[]{
                new ActionItem("Edit", (table1, row) -> {
                    int unitId = (int) table1.getValueAt(row, 0); // Get UnitID from the first column

                    // 1. Instantiate the Unit Form with the ID
                    FormUnits formPanel = new FormUnits(unitId);

                    // 2. Create the Modal Dialog
                    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Unit", Dialog.ModalityType.APPLICATION_MODAL);

                    dialog.setContentPane(formPanel);
                    dialog.pack();
                    dialog.setLocationRelativeTo(null);
                    dialog.setVisible(true); // Blocks until form is closed

                    // 3. Refresh the table after closing the dialog
                    formRefresh();
                }),
                new ActionItem("Delete", (table1, row) -> {
                    int unitId = Integer.parseInt(table1.getValueAt(row, 0).toString());
                    int confirm = JOptionPane.showConfirmDialog(table1, "Are you sure you want to delete unit: " + unitId + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        // Implement deletion logic here
                        unitDao.deleteUnitById(unitId);
                        formRefresh(); // Refresh table after deletion
                    }
                })
        };
    }
}