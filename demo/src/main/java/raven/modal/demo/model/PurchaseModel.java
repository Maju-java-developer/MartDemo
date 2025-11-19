package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseModel extends AbstractModel{

    private int purchaseID;
    private int supplierID;
    private LocalDateTime purchaseDate;
    private String invoiceNo;

    // Fields matching the design
    private double actualAmount; // Corresponds to Actual Amount (Total before final discount)
    private String discountType; // "Percentage (%)" or "Fixed Amount"
    private double discountValue;  // The actual percentage or fixed amount
    private double totalAmount; // Corresponds to Total Amount (Final total after discount)
    private double paidAmount; // Corresponds to Paying Amount
    private String remarks; // Corresponds to Comment

    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();

    // List to hold the detail items when fetching or saving a complete purchase
    private List<PurchaseDetailModel> details;

    // Helper fields for display/form (optional but useful)
    private String supplierName;
}