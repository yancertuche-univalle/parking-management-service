package com.univalle.parkingmanagementservice.usuario.service;

import com.univalle.parkingmanagementservice.usuario.dto.CrearUsuarioRequest;
import com.univalle.parkingmanagementservice.usuario.dto.EditarUsuarioRequest;
import com.univalle.parkingmanagementservice.usuario.dto.UsuarioListItemResponse;
import java.util.List;

public interface UsuarioService {
    List<UsuarioListItemResponse> listarUsuarios();
    UsuarioListItemResponse crearUsuario(CrearUsuarioRequest request);
    UsuarioListItemResponse editarUsuario(Long idUsuario, EditarUsuarioRequest request);
    void eliminarUsuario(Long idUsuario);
}
