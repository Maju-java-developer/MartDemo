package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
public class SupplierModel extends AbstractModel{

    // --- Getters and Setters ---
    private int supplierID;
    private String supplierName;
    private String contactNo;
    private String address;
    private String email;
    private Double openingBalance;
    private LocalDateTime createdDate;

    public SupplierModel() {}

    public SupplierModel(int supplierID, String supplierName) {
        this.supplierID = supplierID;
        this.supplierName = supplierName;
    }
}
