package com.lavarapido.backend_vehicular.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserProfileUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String firstName;

    @Size(max = 50, message = "El apellido no puede superar los 50 caracteres")
    private String lastName;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 10, message = "El teléfono no puede superar los 10 caracteres")
    private String phoneNumber;

    @Size(max = 20, message = "La foto de perfil no puede superar los 20 caracteres")
    private String profilePicture;
    public UserProfileUpdateDTO() { } public UserProfileUpdateDTO(String firstName,String lastName,String phoneNumber,String profilePicture){this.firstName=firstName;this.lastName=lastName;this.phoneNumber=phoneNumber;this.profilePicture=profilePicture;}
    public String getFirstName(){return firstName;} public void setFirstName(String v){firstName=v;} public String getLastName(){return lastName;} public void setLastName(String v){lastName=v;} public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;} public String getProfilePicture(){return profilePicture;} public void setProfilePicture(String v){profilePicture=v;}
}
