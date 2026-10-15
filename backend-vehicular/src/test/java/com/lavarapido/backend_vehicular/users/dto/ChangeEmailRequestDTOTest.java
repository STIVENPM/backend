package com.lavarapido.backend_vehicular.users.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeEmailRequestDTOTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidEmailWithoutWhitespace() {
        ChangeEmailRequestDTO dto = new ChangeEmailRequestDTO("CurrentPass1", "new@example.test");

        assertEquals("new@example.test", dto.newEmail());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void trimsAndLowercasesEmailBeforeValidation() {
        ChangeEmailRequestDTO dto = new ChangeEmailRequestDTO(
                "CurrentPass1", "  Usuario@Ejemplo.com  ");

        assertEquals("usuario@ejemplo.com", dto.newEmail());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void rejectsInvalidEmail() {
        assertFalse(validator.validate(new ChangeEmailRequestDTO(
                "CurrentPass1", "not-an-email")).isEmpty());
    }

    @Test
    void rejectsBlankOrNullEmail() {
        assertFalse(validator.validate(new ChangeEmailRequestDTO(
                "CurrentPass1", "   ")).isEmpty());
        assertFalse(validator.validate(new ChangeEmailRequestDTO(
                "CurrentPass1", null)).isEmpty());
    }

    @Test
    void rejectsEmailLongerThanLimitAfterNormalization() {
        String tooLong = "a".repeat(90) + "@example.test";

        assertFalse(validator.validate(new ChangeEmailRequestDTO(
                "CurrentPass1", tooLong)).isEmpty());
    }

    @Test
    void rejectsBlankCurrentPassword() {
        assertFalse(validator.validate(new ChangeEmailRequestDTO(
                " ", "new@example.test")).isEmpty());
    }
}
