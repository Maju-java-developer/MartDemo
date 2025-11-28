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
public class CompanyModel extends AbstractModel{
    @DropdownField(isId = true)
    private int companyId;
    @DropdownField(isText = true)
    private String companyName;
    private boolean isActive;
}
