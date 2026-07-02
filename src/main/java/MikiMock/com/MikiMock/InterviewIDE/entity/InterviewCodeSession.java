package MikiMock.com.MikiMock.InterviewIDE.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class InterviewCodeSession {

    @Id
    private UUID id;

    private String roomCode;

    @Lob
    private String code;

    private String language;

    private LocalDateTime updatedAt;
}
