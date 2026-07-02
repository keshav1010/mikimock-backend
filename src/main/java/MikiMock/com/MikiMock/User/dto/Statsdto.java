package MikiMock.com.MikiMock.User.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Statsdto {

    private Map<String , Long>  topicStats;

    private Map<String , Long>  levelStats;

}
