package com.univalle.parkingmanagementservice.usuario.dto;

public record CrearUsuarioResponse(
        String mensaje,
        UsuarioListItemResponse usuario
) {
}