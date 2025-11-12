package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerModel {
    private Integer customerId;
    private String customerName;
    private String contactNo;
    private String address;
    private String email;
    private Double openingBalance;
    private String createdDate;
    private Double taxPer;
}
