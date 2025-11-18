package com.agrolink.api.dto;

/**
 * DTO básico para representar al usuario en las respuestas de AuthController.
 * Ajusta campos/nombres según lo que realmente uses en el controlador.
 */
public class UserDto {

    private String id;
    private String email;
    private String fullName;
    private String phone;

    public UserDto() {
    }

    public UserDto(String id, String email, String fullName, String phone) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public UserDto setId(String id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public UserDto setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public UserDto setPhone(String phone) {
        this.phone = phone;
        return this;
    }
}
