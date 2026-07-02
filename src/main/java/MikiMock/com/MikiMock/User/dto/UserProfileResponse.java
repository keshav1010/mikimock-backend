package MikiMock.com.MikiMock.User.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Integer rank;

    private Integer score;

    private String fullName;

    private String headline;

    private String bio;

    private String skills;

    private String currentOrganization;

    private Double experienceYears;

    private String currentLocation;

    private String preferredLocation;

    private String timezone;

    private String country;

}
