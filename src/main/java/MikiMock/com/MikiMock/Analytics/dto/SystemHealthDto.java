package MikiMock.com.MikiMock.Analytics.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthDto {

    private Boolean database;

    private Boolean redis;

    private Boolean kafka;

    private Boolean mail;

    private Boolean application;

}
