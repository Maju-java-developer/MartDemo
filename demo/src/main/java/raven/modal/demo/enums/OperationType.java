package raven.modal.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum OperationType {
    SAVE("Save"),
    UPDATE("Update"),
    DELETE("Delete"),
    DROPDOWN("DropDown"),
    LIST("List"),
    SINGLE("Single");

    private final String value;
}

