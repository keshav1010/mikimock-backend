package MikiMock.com.MikiMock.Notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContactRequest {

    @Email
    private String email;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;
}
