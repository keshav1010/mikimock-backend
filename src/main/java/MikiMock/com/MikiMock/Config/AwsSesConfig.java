package MikiMock.com.MikiMock.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;


@Configuration
public class AwsSesConfig {


    @Bean
    public SesClient sesClient(
            @Value("${spring.aws.region}") String region,
            @Value("${spring.aws.access-key}") String accessKey,
            @Value("${spring.aws.secret-key}") String secretKey
    ) {

        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        accessKey,
                        secretKey
                );


        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();
    }
}
