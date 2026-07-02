package MikiMock.com.MikiMock.User.dto;


import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    private String fullName;

    private String headline;

    private String bio;

    private String skills;

    private String currentOrganization;

    private Double experienceYears;

    private String currentLocation;

    private String timezone;

    private String country;

}
