package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerModel extends AbstractModel{
    private Integer customerId;
    private String customerName;
    private String contactNo;
    private String address;
    private String email;
    private Double openingBalance;
    private Double taxPer;
    private String city;
    private Boolean isActive;

    public CustomerModel(Integer customerId, String customerName) {
        this.customerId = customerId;
        this.customerName = customerName;
    }
}
