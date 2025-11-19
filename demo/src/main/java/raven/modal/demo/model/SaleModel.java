package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleModel extends AbstractModel{

    private int saleID;
    private int customerID;
    private String customerName; // For display/joining
    private LocalDateTime saleDate;
    private String invoiceNo;

    private double actualAmount;     // Net amount after line discounts, before header discount/GST
    private String discountType;
    private double discountValue;    // Header discount value (fixed or percentage)
    private double totalAmount;      // Final amount (after header discount + GST)
    private double receivedAmount;   // Amount paid by customer
    private double gstPer;
    private double gstAmount;
    private String remarks;

    private List<SaleDetailModel> details; // Line items
}
