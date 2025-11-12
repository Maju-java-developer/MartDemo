package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
@Builder
public class UserModel {
    private Integer userId;
    private String userName;
    private String fullName;
    private String email;
    private String contactNo;
    private String role;
    private String password;

}
