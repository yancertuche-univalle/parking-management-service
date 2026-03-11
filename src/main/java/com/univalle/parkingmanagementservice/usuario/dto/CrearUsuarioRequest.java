package com.univalle.parkingmanagementservice.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 50, message = "El usuario no puede superar los 50 caracteres")
        String nombreUsuario,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String contrasena,

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        String confirmacionContrasena,

        @NotBlank(message = "El rol es obligatorio")
        String rol
) {
}
