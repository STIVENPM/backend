package com.lavarapido.backend_vehicular.users.entity;

import com.lavarapido.backend_vehicular.users.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
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
    public User() { }
    public User(UUID userId, String email, String firstName, String lastName, String phoneNumber, DocumentType documentType, String documentNumber, String password, String profilePicture, Boolean status, LocalDateTime createdAt, LocalDateTime updatedAt) { this.userId=userId; this.email=email; this.firstName=firstName; this.lastName=lastName; this.phoneNumber=phoneNumber; this.documentType=documentType; this.documentNumber=documentNumber; this.password=password; this.profilePicture=profilePicture; this.status=status; this.createdAt=createdAt; this.updatedAt=updatedAt; }
    public UUID getUserId(){return userId;} public void setUserId(UUID v){userId=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getFirstName(){return firstName;} public void setFirstName(String v){firstName=v;} public String getLastName(){return lastName;} public void setLastName(String v){lastName=v;} public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;} public DocumentType getDocumentType(){return documentType;} public void setDocumentType(DocumentType v){documentType=v;} public String getDocumentNumber(){return documentNumber;} public void setDocumentNumber(String v){documentNumber=v;} public String getPassword(){return password;} public void setPassword(String v){password=v;} public String getProfilePicture(){return profilePicture;} public void setProfilePicture(String v){profilePicture=v;} public Boolean getStatus(){return status;} public void setStatus(Boolean v){status=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
