package com.lavarapido.backend_vehicular.users.dto;

import com.lavarapido.backend_vehicular.users.enums.DocumentType;

public class UserRegistrationDTO {

    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private DocumentType documentType;
    private String documentNumber;
    private String password;
    public UserRegistrationDTO() { } public UserRegistrationDTO(String email,String firstName,String lastName,String phoneNumber,DocumentType documentType,String documentNumber,String password){this.email=email;this.firstName=firstName;this.lastName=lastName;this.phoneNumber=phoneNumber;this.documentType=documentType;this.documentNumber=documentNumber;this.password=password;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getFirstName(){return firstName;} public void setFirstName(String v){firstName=v;} public String getLastName(){return lastName;} public void setLastName(String v){lastName=v;} public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;} public DocumentType getDocumentType(){return documentType;} public void setDocumentType(DocumentType v){documentType=v;} public String getDocumentNumber(){return documentNumber;} public void setDocumentNumber(String v){documentNumber=v;} public String getPassword(){return password;} public void setPassword(String v){password=v;}
}
