package MikiMock.com.MikiMock.CodeExecution.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "code.runner")
public class CodeRunnerProperties {

    private String url;

    private String secret;

    private Integer timeoutMs;
}