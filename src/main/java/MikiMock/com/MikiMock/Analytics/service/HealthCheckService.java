package MikiMock.com.MikiMock.Analytics.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final JdbcTemplate jdbcTemplate;

    private final StringRedisTemplate redisTemplate;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final JavaMailSender mailSender;

    public boolean isDatabaseUp() {

        try {

            Integer result =
                    jdbcTemplate.queryForObject(
                            "SELECT 1",
                            Integer.class
                    );

            return result != null && result == 1;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean isRedisUp() {

        try {

            redisTemplate.opsForValue().get("health");

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean isKafkaUp() {

        try {

            kafkaTemplate.metrics();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean isMailUp() {

        try {

            mailSender.createMimeMessage();
            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean isApplicationUp() {

        return true;
    }
}
