package MikiMock.com.MikiMock.User.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")

@Getter
@Setter
@Builder

@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // RELATION WITH USER TABLE
    // =========================

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    // =========================
    // PROFILE FIELDS
    // =========================

    private String achievements;

    private Integer score;

    @Column(name = "current_organization")
    private String currentOrganization;

    @Column(name = "current_location")
    private String currentLocation;

//    @NotBlank
    @Size(max = 100)
    @Column(name = "full_name")
    private String fullName;

    @Size(max = 120)
    private String headline;

    @Size(max = 500)
    private String bio;

    @Column(name = "experience_years")
    private Double experienceYears;


    @Size(max = 50)
    private String timezone;

    @Size(max = 100)
    private String country;


    // =========================
    // AUDIT FIELDS
    // =========================

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
