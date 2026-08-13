package MikiMock.com.MikiMock.Auth.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank
    @NonNull
    @Email
    private String email;
}