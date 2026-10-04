package MikiMock.com.MikiMock.Auth.dto;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String name;

//    @NotBlank
//    @Size(min = 6)
//    private String password;
}
