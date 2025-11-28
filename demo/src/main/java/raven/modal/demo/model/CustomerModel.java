package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import raven.modal.demo.annotations.DropdownField;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerModel extends AbstractModel{
    @DropdownField(isId = true)
    private Integer customerId;
    @DropdownField(isText = true)
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
