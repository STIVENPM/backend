package com.lavarapido.backend_vehicular.users.dto;

import com.lavarapido.backend_vehicular.users.enums.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRegistrationDTO {

    @NotBlank @Email @Size(max = 100)
    private String email;
    @NotBlank @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lastName;
    @NotBlank @Pattern(regexp = "^3[0-9]{9}$")
    private String phoneNumber;
    @NotNull
    private DocumentType documentType;
    @NotBlank @Size(max = 12)
    private String documentNumber;
    @NotBlank @Size(min = 8, max = 72)
    private String password;
    public UserRegistrationDTO() { } public UserRegistrationDTO(String email,String firstName,String lastName,String phoneNumber,DocumentType documentType,String documentNumber,String password){this.email=email;this.firstName=firstName;this.lastName=lastName;this.phoneNumber=phoneNumber;this.documentType=documentType;this.documentNumber=documentNumber;this.password=password;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getFirstName(){return firstName;} public void setFirstName(String v){firstName=v;} public String getLastName(){return lastName;} public void setLastName(String v){lastName=v;} public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;} public DocumentType getDocumentType(){return documentType;} public void setDocumentType(DocumentType v){documentType=v;} public String getDocumentNumber(){return documentNumber;} public void setDocumentNumber(String v){documentNumber=v;} public String getPassword(){return password;} public void setPassword(String v){password=v;}
}
