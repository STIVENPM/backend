package com.lavarapido.backend_vehicular.users.entity;

import com.lavarapido.backend_vehicular.shared.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 10, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 10, nullable = false)
    private DocumentType documentType;

    @Column(name = "document_number", length = 12, nullable = false)
    private String documentNumber;

    @Column(name = "password", length = 60, nullable = false)
    private String password;
    @Column(
        name = "profile_picture",
        length = 20,
        nullable = false
    )
    private String profilePicture = "avatar_1";

    @Column(nullable = false)
    private Boolean status = true;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
