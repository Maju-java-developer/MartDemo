package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
@Builder
public class SupplierPaymentModel {
    private int paymentID;
    private int supplierID;
    private LocalDateTime paymentDate;
    private double amount;
    private String remarks;

    public SupplierPaymentModel(int supplierID, double amount, String remarks) {
        this.supplierID = supplierID;
        this.amount = amount;
        this.remarks = remarks;
        this.paymentDate = LocalDateTime.now();
    }

}
