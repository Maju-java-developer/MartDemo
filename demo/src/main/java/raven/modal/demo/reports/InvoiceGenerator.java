package raven.modal.demo.reports;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import raven.modal.demo.model.PurchaseDetailModel;
import raven.modal.demo.model.PurchaseModel;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class InvoiceGenerator {

    /**
     * Generates and saves a Purchase Invoice PDF to the specified file path.
     * @param purchase The complete PurchaseModel data.
     * @param filePath The full path where the PDF should be saved.
     * @throws DocumentException, IOException If there's an issue generating or saving the PDF.
     */
    public void generatePurchaseInvoice(PurchaseModel purchase, String filePath) throws DocumentException, IOException {

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // --- 1. TITLE and HEADER ---
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Paragraph title = new Paragraph("PURCHASE INVOICE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" ")); // Spacer

        // --- 2. COMPANY/SUPPLIER DETAILS ---
        document.add(createSupplierInfo(purchase));
        document.add(new Paragraph(" ")); // Spacer

        // --- 3. DETAIL TABLE ---
        document.add(createDetailTable(purchase));
        document.add(new Paragraph(" ")); // Spacer

        // --- 4. FOOTER SUMMARY ---
        document.add(createSummary(purchase));

        document.close();
    }

    private PdfPTable createSupplierInfo(PurchaseModel purchase) {
        // FIX: Switched from com.lowagie.text.Table to com.lowagie.text.pdf.PdfPTable
        PdfPTable infoTable = new PdfPTable(2);

        // Set column widths to distribute space (e.g., 50% / 50%)
        try {
            infoTable.setWidths(new float[]{1, 1});
        } catch (DocumentException e) {
            // Should not happen, but required by method signature
            e.printStackTrace();
        }

        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10f); // Add a little space after the title

        // Now, these calls will correctly resolve the method 'addCell(PdfPCell)'
        infoTable.addCell(createCell("Invoice No: " + purchase.getInvoiceNo(), Element.ALIGN_LEFT));
        infoTable.addCell(createCell("Supplier: " + purchase.getSupplierName(), Element.ALIGN_LEFT));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        infoTable.addCell(createCell("Date: " + purchase.getPurchaseDate().format(formatter), Element.ALIGN_LEFT));
        infoTable.addCell(createCell("Remarks: " + purchase.getRemarks(), Element.ALIGN_LEFT));

        return infoTable;
    }
    private PdfPTable createDetailTable(PurchaseModel purchase) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);

        // Headers
        Stream.of("No.", "Product Name", "Qty", "Rate", "Total")
                .forEach(header -> {
                    PdfPCell hCell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                    hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    hCell.setBackgroundColor(new Color(220, 220, 220)); // Light Gray background
                    table.addCell(hCell);
                });

        // Data Rows
        int srNo = 1;
        for (PurchaseDetailModel detail : purchase.getDetails()) {
            table.addCell(String.valueOf(srNo++));
            table.addCell(detail.getProductName());

            // Align Quantity, Rate, and Total to the right
            table.addCell(createRightAlignedCell(String.format("%.2f", detail.getQuantity())));
            table.addCell(createRightAlignedCell(String.format("%.2f", detail.getRate())));
            table.addCell(createRightAlignedCell(String.format("%.2f", detail.getTotal())));
        }
        return table;
    }

    private PdfPTable createSummary(PurchaseModel purchase) {
        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(40); // Make summary table narrower
        summary.setHorizontalAlignment(Element.ALIGN_RIGHT);

        double balanceDue = purchase.getTotalAmount() - purchase.getPaidAmount();

        summary.addCell(createCell("TOTAL AMOUNT:", Element.ALIGN_RIGHT));
        summary.addCell(createRightAlignedCell(String.format("₹ %.2f", purchase.getTotalAmount())));

        summary.addCell(createCell("PAID AMOUNT:", Element.ALIGN_RIGHT));
        summary.addCell(createRightAlignedCell(String.format("₹ %.2f", purchase.getPaidAmount())));

        summary.addCell(createCell("BALANCE DUE:", Element.ALIGN_RIGHT));
        summary.addCell(createRightAlignedCell(String.format("₹ %.2f", balanceDue)));

        return summary;
    }

    // Utility for creating standard cells
    private PdfPCell createCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(0);
        return cell;
    }

    // Utility for creating right-aligned data cells
    private PdfPCell createRightAlignedCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}