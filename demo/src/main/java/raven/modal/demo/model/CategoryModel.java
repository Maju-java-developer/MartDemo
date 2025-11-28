package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import raven.modal.demo.annotations.DropdownField;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryModel extends AbstractModel{
    @DropdownField(isId = true)
    private int categoryId;
    @DropdownField(isText = true)
    private String categoryName;
    private boolean isActive;
}
