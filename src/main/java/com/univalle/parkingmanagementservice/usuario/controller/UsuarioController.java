package com.univalle.parkingmanagementservice.usuario.controller;

import java.util.List;

import com.univalle.parkingmanagementservice.usuario.dto.*;
import com.univalle.parkingmanagementservice.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Operaciones relacionadas con usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Retorna el listado de usuarios registrados")
    @PreAuthorize("hasRole('ROLE_ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioListItemResponse>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea nuevos usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_ADMINISTRADOR')")
    public CrearUsuarioResponse crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioListItemResponse usuario = usuarioService.crearUsuario(request);
        return new CrearUsuarioResponse("Usuario creado correctamente", usuario);
    }

    @PutMapping("/{idUsuario}")
    @Operation(summary = "Actualizar usuario", description = "Actualizar losa datos de un usuarios")
    @PreAuthorize("hasRole('ROLE_ADMINISTRADOR')")
    public UsuarioListItemResponse editarUsuario(
            @PathVariable Long idUsuario,
            @Valid @RequestBody EditarUsuarioRequest request
    ) {
        return usuarioService.editarUsuario(idUsuario, request);
    }

    @DeleteMapping("/{idUsuario}")
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema por su id")
    @PreAuthorize("hasRole('ROLE_ADMINISTRADOR')")
    public ResponseEntity<MensajeResponse> eliminarUsuario(@PathVariable Long idUsuario) {
        usuarioService.eliminarUsuario(idUsuario);
        return ResponseEntity.ok(new MensajeResponse("Usuario eliminado correctamente"));
    }
}
