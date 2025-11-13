package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.PurchaseDao;
import raven.modal.demo.model.PurchaseDetailModel;
import raven.modal.demo.model.PurchaseModel;
import raven.modal.demo.reports.InvoiceGenerator;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.table.TableHeaderAlignment;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class FormViewPurchase extends Form {

    private final PurchaseDao purchaseDao = new PurchaseDao();
    private final int purchaseId;

    // Header Components (Read-only)
    private JTextField txtInvoiceNo, txtSupplier, txtPurchaseDate;

    // Detail Table
    private JTable detailTable;
    private DefaultTableModel detailModel;

    // Footer Components (Read-only)
    private JTextArea txtComment;
    private JTextField txtTotalAmount, txtPaidAmount, txtBalance;

    // Action Buttons
    private JButton btnGenerateInvoice, btnClose;

    public FormViewPurchase(int purchaseId) {
        this.purchaseId = purchaseId;
        init();
        loadPurchaseData();
    }

    private void init() {
        setLayout(new MigLayout("wrap, fill, insets 20", "[fill]"));

        JLabel title = new JLabel("View Purchase Record");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(title, "gapy 0 10");

        add(createHeaderPanel(), "gapy 0");
        add(createDetailTablePanel(), "gapy 10, grow, push");
        add(createFooterPanel(), "gapy 10");
        add(createButtonPanel(), "align right, gapy 10 0");

        setupDetailTableModel();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 6, fillx, insets 0", "[right][grow][right][grow][right][grow]", ""));

        txtInvoiceNo = new JTextField();
        txtSupplier = new JTextField();
        txtPurchaseDate = new JTextField();

        // Set all header fields to read-only
        txtInvoiceNo.setEditable(false);
        txtSupplier.setEditable(false);
        txtPurchaseDate.setEditable(false);

        panel.add(new JLabel("Invoice No:"));
        panel.add(txtInvoiceNo, "h 30!, w 150");
        panel.add(new JLabel("Supplier:"));
        panel.add(txtSupplier, "h 30!, growx");
        panel.add(new JLabel("Date:"));
        panel.add(txtPurchaseDate, "h 30!, w 150");

        return panel;
    }

    private JPanel createDetailTablePanel() {
        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 0", "[fill]"));

        detailTable = new JTable();
        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(new JLabel("Purchased Items"), "gapy 5 5");
        panel.add(scroll, "h 200, grow, push");

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2, fillx, insets 0", "[grow, fill]15[300, fill]")); // Left column wider

        // Left Column (Comments)
        JPanel leftPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 0", "[fill]"));
        txtComment = new JTextArea(3, 20);
        txtComment.setLineWrap(true);
        txtComment.setWrapStyleWord(true);
        txtComment.setEditable(false);
        JScrollPane scrollComment = new JScrollPane(txtComment);

        leftPanel.add(new JLabel("Remarks"));
        leftPanel.add(scrollComment, "h 60!");

        // Right Column (Summary)
        JPanel rightPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 0", "[right][150]", ""));
        txtTotalAmount = new JTextField();
        txtPaidAmount = new JTextField();
        txtBalance = new JTextField();

        // Set summary fields to read-only
        txtTotalAmount.setEditable(false);
        txtPaidAmount.setEditable(false);
        txtBalance.setEditable(false);

        rightPanel.add(new JLabel("Total Amount:"));
        rightPanel.add(txtTotalAmount, "h 30!, w 100");
        rightPanel.add(new JLabel("Paid Amount:"));
        rightPanel.add(txtPaidAmount, "h 30!, w 100");
        rightPanel.add(new JLabel("Balance Due:"));
        rightPanel.add(txtBalance, "h 30!, w 100");

        panel.add(leftPanel);
        panel.add(rightPanel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new MigLayout("right, insets 0", "[][][]"));
        btnClose = new JButton("Close");
        btnGenerateInvoice = new JButton("Generate Invoice (Future)");

        btnClose.addActionListener(e -> SwingUtilities.getWindowAncestor(this).dispose());

        // Placeholder action for future invoice generation
        btnGenerateInvoice.addActionListener(e -> generateInvoiceAction());

        panel.add(btnGenerateInvoice);
        panel.add(btnClose);
        return panel;
    }

    // --- Data Loading and Table Setup ---

    private void setupDetailTableModel() {
        String[] columns = {"Sr#", "Product", "Qty", "Unit Price", "Total"};

        detailModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // All cells are read-only
            }
        };

        detailTable.setModel(detailModel);

        // Styling and Alignment (Reuse the TableHeaderAlignment logic)
        detailTable.getColumnModel().getColumn(0).setMaxWidth(30);
        detailTable.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(detailTable) {
            @Override
            protected int getAlignment(int column) {
                if (column == 0 || column == 4) return SwingConstants.CENTER; // Qty, Price, Total
                return SwingConstants.LEADING;
            }
        });
        detailTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;");
        detailTable.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:30;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

    }

    private void loadPurchaseData() {
        PurchaseModel purchase = purchaseDao.getPurchaseForEdit(purchaseId);

        if (purchase == null) {
            JOptionPane.showMessageDialog(this, "Purchase record not found.", "Error", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.getWindowAncestor(this).dispose();
            return;
        }

        // Load Header Data
        txtInvoiceNo.setText(purchase.getInvoiceNo());
        txtSupplier.setText(purchase.getSupplierName());
        txtPurchaseDate.setText(purchase.getPurchaseDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

        // Load Detail Data
        int srNo = 1;
        for (PurchaseDetailModel detail : purchase.getDetails()) {
            detailModel.addRow(new Object[]{
                    srNo++,
                    detail.getProductName(),
                    detail.getQuantity(),
                    String.format("%.2f", detail.getRate()),
                    String.format("%.2f", detail.getTotal())
            });
        }

        // Load Footer Data
        double balance = purchase.getTotalAmount() - purchase.getPaidAmount();
        txtTotalAmount.setText(String.format("%.2f", purchase.getTotalAmount()));
        txtPaidAmount.setText(String.format("%.2f", purchase.getPaidAmount()));
        txtBalance.setText(String.format("%.2f", balance));
        txtComment.setText(purchase.getRemarks());
    }
// Inside raven.modal.demo.forms.FormViewPurchase.java

    private void generateInvoiceAction() {
        PurchaseModel purchase = purchaseDao.getPurchaseForEdit(purchaseId);
        if (purchase == null) {
            JOptionPane.showMessageDialog(this, "Cannot retrieve purchase data for invoice.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- STEP 1: Define Temporary File Path ---
        // Use the system's temporary directory for viewing the file,
        // which gives the user the "not saved" feeling.
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = "Purchase_Invoice_" + purchase.getInvoiceNo() + "_" + System.currentTimeMillis() + ".pdf";
        java.io.File tempFile = new java.io.File(tempDir, fileName);

        String filePath = tempFile.getAbsolutePath();

        try {
            // --- STEP 2: Generate and Save the PDF ---
            InvoiceGenerator generator = new InvoiceGenerator();
            generator.generatePurchaseInvoice(purchase, filePath);

            // --- STEP 3: Open the File in the Default Viewer ---
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(tempFile);

                // Optional: Notify the user where they can find the temp file if they want to save it permanently
                JOptionPane.showMessageDialog(this,
                        "Invoice generated and opened in your default PDF viewer. You can save it from there.",
                        "Invoice Generated",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Invoice generated to:\n" + filePath + "\nPlease open it manually.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (com.lowagie.text.DocumentException | java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to generate or open invoice. Details: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
//    private void generateInvoiceAction() {
//        PurchaseModel purchase = purchaseDao.getPurchaseForEdit(purchaseId);
//        if (purchase == null) {
//            JOptionPane.showMessageDialog(this, "Cannot retrieve purchase data for invoice.", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        // Use JFileChooser to let the user select the save location
//        JFileChooser fileChooser = new JFileChooser();
//        fileChooser.setDialogTitle("Save Purchase Invoice PDF");
//        fileChooser.setSelectedFile(new java.io.File("Purchase_Invoice_" + purchase.getInvoiceNo() + ".pdf"));
//
//        int userSelection = fileChooser.showSaveDialog(this);
//
//        if (userSelection == JFileChooser.APPROVE_OPTION) {
//            java.io.File fileToSave = fileChooser.getSelectedFile();
//
//            // Ensure the file has the .pdf extension
//            String filePath = fileToSave.getAbsolutePath();
//            if (!filePath.toLowerCase().endsWith(".pdf")) {
//                filePath += ".pdf";
//            }
//
//            try {
//                InvoiceGenerator generator = new InvoiceGenerator();
//                generator.generatePurchaseInvoice(purchase, filePath);
//
//                JOptionPane.showMessageDialog(this,
//                        "Invoice generated successfully and saved to:\n" + filePath,
//                        "Success",
//                        JOptionPane.INFORMATION_MESSAGE);
//            } catch (DocumentException | IOException ex) {
//                ex.printStackTrace();
//                JOptionPane.showMessageDialog(this,
//                        "Failed to generate invoice. Details: " + ex.getMessage(),
//                        "PDF Generation Error",
//                        JOptionPane.ERROR_MESSAGE);
//            }
//        }
//    }

}