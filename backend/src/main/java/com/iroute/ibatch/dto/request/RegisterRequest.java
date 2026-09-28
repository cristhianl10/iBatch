package com.iroute.ibatch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 3, max = 100, message = "El usuario debe tener entre 3 y 100 caracteres")
        String username,
        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
        String password) {
}
