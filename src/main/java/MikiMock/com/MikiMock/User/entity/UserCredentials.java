package MikiMock.com.MikiMock.User.entity;

import io.github.resilience4j.core.lang.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "user_credentials")

@Builder
@Getter
@Setter

@AllArgsConstructor
@NoArgsConstructor
public class UserCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(name = "password_hash")
    @Nullable
    private String password;

    @Column(name = "last_password_change_at")
    private Timestamp lastPasswordChangeAT;

    @Column(name ="user_email")
    @Email
    @NonNull
    private String email;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    @Column(name = "locked_until")
    private Timestamp lockedUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;
}
